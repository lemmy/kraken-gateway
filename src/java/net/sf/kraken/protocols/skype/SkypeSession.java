/*******************************************************************************
 * Copyright (c) 2011 Markus Alexander Kuppe.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Markus Alexander Kuppe (ecf-dev_eclipse.org <at> lemmster <dot> de) - initial API and implementation
 ******************************************************************************/
package net.sf.kraken.protocols.skype;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import net.sf.kraken.BaseTransport;
import net.sf.kraken.avatars.Avatar;
import net.sf.kraken.registration.Registration;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.ChatStateType;
import net.sf.kraken.type.PresenceType;
import net.sf.kraken.type.TransportLoginStatus;

import org.apache.log4j.Logger;
import org.xmpp.packet.JID;

import com.skype.Chat;
import com.skype.ChatMessage;
import com.skype.ContactList;
import com.skype.Group;
import com.skype.Profile;
import com.skype.Skype;
import com.skype.SkypeException;
import com.skype.User;
import com.skype.User.Status;
import com.skype.connector.ConnectorListener;
import com.skype.connector.ConnectorMessageEvent;
import com.skype.connector.ConnectorStatusEvent;

public class SkypeSession extends TransportSession<SkypeBuddy> implements ConnectorListener {

	static Logger Log = Logger.getLogger(SkypeSession.class);
	
	private final Skype skype;

	public SkypeSession(Registration registration, JID jid,
			BaseTransport<SkypeBuddy> transport, Integer priority, PresenceType presenceType, String verboseStatus) {
		super(registration, jid, transport, priority);
		
		this.presence = presenceType;
		this.verboseStatus = verboseStatus == null ? "" : verboseStatus;
		
		final String username = registration.getUsername();
		final String password = registration.getPassword();

		this.skype = new Skype(username, password);
		System.out.println("Created new session with skype instance: " + skype);
		skype.addConnectorListener(this);
		skype.connect();
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#updateStatus(net.sf.kraken.type.PresenceType, java.lang.String)
	 */
	@Override
	public void updateStatus(PresenceType presenceType, String verboseStatus) {
		// xmpp status
		setPresenceAndStatus(presenceType, verboseStatus);
	
		if(isLoggedIn()) {
		    Profile.Status status = convertXMPPStatusToSkypeStatus(presenceType);
		    skype.getProfile().setStatus(status);
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#addContact(org.xmpp.packet.JID, java.lang.String, java.util.ArrayList)
	 */
	@Override
	public void addContact(JID jid, String nickname, ArrayList<String> groups) {
	    acceptAddContact(jid);
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#acceptAddContact(org.xmpp.packet.JID)
	 */
	@Override
	public void acceptAddContact(JID jid) {
	    //TODO sanity check skypeId validity (it's user input after all) 
	    final String skypeId = getTransport().convertJIDToID(jid);
	    skype.getContactList().addUser(skypeId, "Please allow me to see your online status");
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#sendMessage(org.xmpp.packet.JID, java.lang.String)
	 */
	@Override
	public void sendMessage(JID jid, String message) {
		Chat chat = skype.chat(getTransport().convertJIDToID(jid));
		chat.send(message);
	}


	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#sendChatState(org.xmpp.packet.JID, net.sf.kraken.type.ChatStateType)
	 */
	@Override
	public void sendChatState(JID jid, ChatStateType chatState) {
		throw new UnsupportedOperationException();
	}


	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#sendBuzzNotification(org.xmpp.packet.JID, java.lang.String)
	 */
	@Override
	public void sendBuzzNotification(JID jid, String message) {
		throw new UnsupportedOperationException();
	}


	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#updateLegacyAvatar(java.lang.String, byte[])
	 */
	@Override
	public void updateLegacyAvatar(String type, byte[] data) {
		throw new UnsupportedOperationException();
	}


	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#logIn(net.sf.kraken.type.PresenceType, java.lang.String)
	 */
	@Override
	public void logIn(PresenceType presenceType, String verboseStatus) {
        setPendingPresenceAndStatus(presenceType, verboseStatus);
        if (!isLoggedIn()) {  
			// startup skype
			Profile profile = skype.getProfile();

			// status
			profile.setStatus(convertXMPPStatusToSkypeStatus(presenceType));

			// add listeners to handle chat and exceptions
			skype.addChatMessageListener(new SkypeChatMessageListener(this));

			// mark logged in (prior to syncing rosters!
			setLoginStatus(TransportLoginStatus.LOGGED_IN);

			// buddies to roster
			syncFriendsToRosters();

			// old chats
			syncMissedChatMessages();
        }
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#logOut()
	 */
	@Override
	public void logOut() {
        sessionDisconnectedNoReconnect(null);
	}


	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#cleanUp()
	 */
	@Override
	public void cleanUp() {
		skype.removeConnectorListener(this);
		
		if(skype.getStatus() == com.skype.connector.Connector.Status.ATTACHED) {
			Profile profile = skype.getProfile();
			profile.setStatus(Profile.Status.NA);

			skype.clearChatHistory();
		}
	    
		// remove all listeners
        skype.removeAllListeners();
        
        skype.dispose();
	}


	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#removeContact(net.sf.kraken.roster.TransportBuddy)
	 */
	@Override
	public void removeContact(SkypeBuddy contact) {
        skype.getContactList().removeUser(contact.getUser());
	}
	
	

	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#updateContact(net.sf.kraken.roster.TransportBuddy)
	 */
	@Override
	public void updateContact(SkypeBuddy skypeBuddy) {
		final ContactList contactList = skype.getContactList();
		final Map<String, Group> skypeGroups = skypeBuddy.getSkypeGroups();
		final Collection<String> groups = skypeBuddy.getGroups() == null ? new ArrayList<String>()
				: skypeBuddy.getGroups();

		// remove from skype groups
		for (final Map.Entry<String, Group> skypeGroup : skypeGroups.entrySet()) {
			final String displayName = skypeGroup.getKey();
			if (!groups.contains(displayName)) {
				Group group = skypeGroup.getValue();
				group.removeUser(skypeBuddy.getUser());
				skypeBuddy.removeSkypeGroup(displayName);
			}
		}
		// add to new groups
		final Set<String> keySet = skypeGroups.keySet();
		for (final String groupName : groups) {
			if (!keySet.contains(groupName)) {
				Group group = contactList.getGroup(groupName);
				if (group == null) {
					group = contactList.addGroup(groupName);
				}
				group.addUser(skypeBuddy.getUser());
				skypeBuddy.addSkypeGroup(groupName, group);
			}
		}

		// nickname => displayname
		final String nickname = skypeBuddy.getNickname();
		final String displayName = skypeBuddy.getUser().getDisplayName();
		if (!displayName.equals(nickname)) {
			skypeBuddy.getUser().setDisplayName(nickname);
		}
	}
	
	// populate rosters
    private void syncFriendsToRosters() {
		try {
			final ContactList contactList = skype.getContactList();

			// first get all custom groups a user is associated with
			final Map<User, List<Group>> friendWithGroups = new HashMap<User, List<Group>>();
			final Group[] allGroups = contactList.getAllGroups();
			for (Group group : allGroups) {
				final User[] friends = group.getAllUsers();
				for (User friend : friends) {
					List<Group> list = friendWithGroups.get(friend);
					if(list == null) {
						list = new ArrayList<Group>();
						list.add(group);
						friendWithGroups.put(friend, list);
					} else {
						list.add(group);
					}
				}
			}

			// get add contacts independent of their group but reuse group list here
			final User[] friends = contactList.getAllUsers();
			for (User friend : friends) {
				
				// use full name if display name is not set
				String displayName = friend.getDisplayName();
				if (displayName == null || "".equals(displayName)) {
					displayName = friend.getFullName();
				}
				
				final Map<String, Group> map = new HashMap<String, Group>();
				List<Group> list = friendWithGroups.get(friend);
				if(list == null) {
					list = new ArrayList<Group>();
				}
				for (Group group : list) {
					map.put(group.getDisplayName(), group);
				}
				final SkypeBuddy buddy = new SkypeBuddy(getBuddyManager(),
						friend, displayName, map);
				
				// add listener to receive buddy state changes
				friend.addPropertyChangeListener(new SkypeUserListener(this,
						friend));
				
				// Status
				final Status status = friend.getStatus();
				buddy.setPresence(convertSkypeStatusToXMPP(status));
				
				// Avatar
				final BufferedImage bi = friend.getAvatar();
				if(bi != null) { // be defensive 
				    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
				    try {
				        ImageIO.write(bi, "jpg", bos);
				        final byte[] imageData = bos.toByteArray();
				        final Avatar avatar = new Avatar(buddy.getJID(), imageData);
				        buddy.setAvatar(avatar);
				    } catch(IOException e) {
				        e.printStackTrace();
				    }
				}
				
				// finally store the buddy
				getBuddyManager().storeBuddy(buddy);
			}
			getTransport().syncLegacyRoster(getJID(),
					getBuddyManager().getBuddies());
			getBuddyManager().activate();
			System.out.println("Synced friends to roster for skype isntance: " + skype);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    private void syncMissedChatMessages() throws SkypeException {
		final Chat[] chats = skype.getAllMissedChats();
		for (Chat chat : chats) {

			// chat messages are unordered, but we want to show 'em in chronological order 
			final SortedSet<ChatMessage> chatMessages = new TreeSet<ChatMessage>(
					new SkypeChatMessageComparator());
			chatMessages.addAll(Arrays.asList(chat.getAllChatMessages()));
			
			for (ChatMessage chatMessage : chatMessages) {
			    final User sender = chatMessage.getSender();
		        final JID from = getTransport().convertIDToJID(sender.getId());
			    final Date time = chatMessage.getTime();
			    final String msg = chatMessage.getContent();
			    getTransport().sendOfflineMessage(getJID(), from, msg, time, null);
		    }
		}
		//finally purge old chat messages
		skype.clearChatHistory();
	}

	public PresenceType convertSkypeStatusToXMPP(Status status) {
		PresenceType presenceType;
		switch (status) {
			case ONLINE:
				presenceType = PresenceType.available;
				break;
			case AWAY:
				presenceType = PresenceType.away;
				break;
			case DND:
				presenceType = PresenceType.dnd;
				break;
			case NA:
				presenceType = PresenceType.xa;
				break;
			case OFFLINE:
				presenceType = PresenceType.unavailable;
				break;
			case SKYPEME:
				presenceType = PresenceType.chat;
				break;
			case SKYPEOUT:
				presenceType = PresenceType.unavailable;
				break;
			case UNKNOWN:
				presenceType = PresenceType.unavailable;
				break;
			default:
				presenceType = PresenceType.unavailable;
				break;
		}
		return presenceType;
	}

	public com.skype.Profile.Status convertXMPPStatusToSkypeStatus(PresenceType presenceType) {
		switch (presenceType) {
			case away:
				return Profile.Status.AWAY;
			case dnd:
				return Profile.Status.DND;
			case chat:
				return Profile.Status.SKYPEME;
			case available:
				return Profile.Status.ONLINE;
			case unavailable:
				return Profile.Status.OFFLINE;
			case unknown:
				return Profile.Status.ONLINE;
			case xa:
				return Profile.Status.NA;
		}
		return Profile.Status.UNKNOWN;
	}
	

	/* (non-Javadoc)
	 * @see com.skype.connector.ConnectorListener#messageReceived(com.skype.connector.ConnectorMessageEvent)
	 */
	public void messageReceived(ConnectorMessageEvent event) {
	}

	/* (non-Javadoc)
	 * @see com.skype.connector.ConnectorListener#messageSent(com.skype.connector.ConnectorMessageEvent)
	 */
	public void messageSent(ConnectorMessageEvent event) {
	}

	/* (non-Javadoc)
	 * @see com.skype.connector.ConnectorListener#statusChanged(com.skype.connector.ConnectorStatusEvent)
	 */
	public synchronized void statusChanged(ConnectorStatusEvent event) {
		if(skype.isDisposed()) {
			return;
		}
		final com.skype.connector.Connector.Status status = event.getStatus();
		switch (status) {
			case NOT_RUNNING:
				if(isLoggedIn()) {
					System.out.println("Received NOT_RUNNING from skype instance: " + skype);
					setLoginStatus(TransportLoginStatus.LOGGING_OUT);
					sessionDisconnectedNoReconnect(null);
					setLoginStatus(TransportLoginStatus.LOGGED_OUT);
				} else {
					System.err.println("Received NOT_RUNNING from skype instance: " + skype + " while not logged in");
				}
				break;
			case ATTACHED:
				System.out.println("Received ATTACHED from skype instance: " + skype);
				logIn(presence, verboseStatus);
				break;
			default:
				System.out.println("Received " + status + " from skype instance: " + skype);
				break;
		}
	}
}
