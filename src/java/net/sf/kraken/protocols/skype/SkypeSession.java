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

import java.util.ArrayList;

import net.sf.kraken.BaseTransport;
import net.sf.kraken.registration.Registration;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.ChatStateType;
import net.sf.kraken.type.ConnectionFailureReason;
import net.sf.kraken.type.PresenceType;
import net.sf.kraken.type.TransportLoginStatus;

import org.apache.log4j.Logger;
import org.xmpp.packet.JID;

import com.skype.Chat;
import com.skype.ContactList;
import com.skype.Friend;
import com.skype.Profile;
import com.skype.Skype;
import com.skype.SkypeException;
import com.skype.User.Status;

public class SkypeSession extends TransportSession<SkypeBuddy> {

	static Logger Log = Logger.getLogger(SkypeSession.class);

	public SkypeSession(Registration registration, JID jid,
			BaseTransport<SkypeBuddy> transport, Integer priority) {
		super(registration, jid, transport, priority);
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#updateStatus(net.sf.kraken.type.PresenceType, java.lang.String)
	 */
	@Override
	public void updateStatus(PresenceType presenceType, String verboseStatus) {
		// xmpp status
		setPresenceAndStatus(presenceType, verboseStatus);
	
		try {
			Profile.Status status = convertXMPPStatusToSkypeStatus(presenceType);
			Skype.getProfile().setStatus(status);
		} catch (SkypeException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#addContact(org.xmpp.packet.JID, java.lang.String, java.util.ArrayList)
	 */
	@Override
	public void addContact(JID jid, String nickname, ArrayList<String> groups) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#acceptAddContact(org.xmpp.packet.JID)
	 */
	@Override
	public void acceptAddContact(JID jid) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#sendMessage(org.xmpp.packet.JID, java.lang.String)
	 */
	@Override
	public void sendMessage(JID jid, String message) {
		try {
			Chat chat = Skype.chat(getTransport().convertJIDToID(jid));
			chat.send(message);
		} catch (SkypeException e) {
			e.printStackTrace();
		}
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
			try {
				// startup skype
				Profile profile = Skype.getProfile();

				// status
				profile.setStatus(convertXMPPStatusToSkypeStatus(presenceType));

				// add listeners to handle chat and exceptions
				Skype.addChatMessageListener(new SkypeChatMessageListener(this));

				// mark logged in (prior to syncing rosters!
				setLoginStatus(TransportLoginStatus.LOGGED_IN);
				
				// buddies to roster
				syncFriendsToRosters();
			} catch (Exception e) {
				Log.error("Skype: Tried to start up duplicate session for: "
						+ jid);
				setFailureStatus(ConnectionFailureReason.UNKNOWN);
				sessionDisconnected("Duplicate session.");
			}
        }
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#logOut()
	 */
	@Override
	public void logOut() {
		try {
			Profile profile = Skype.getProfile();
			profile.setStatus(Profile.Status.NA);
		} catch (SkypeException e) {
			e.printStackTrace();
		}
        cleanUp();
        sessionDisconnectedNoReconnect(null);
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#cleanUp()
	 */
	@Override
	public void cleanUp() {
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#removeContact(net.sf.kraken.roster.TransportBuddy)
	 */
	@Override
	public void removeContact(SkypeBuddy contact) {
		throw new UnsupportedOperationException();
	}
	

	/* (non-Javadoc)
	 * @see net.sf.kraken.session.TransportSession#updateContact(net.sf.kraken.roster.TransportBuddy)
	 */
	@Override
	public void updateContact(SkypeBuddy contact) {
		throw new UnsupportedOperationException();
	}
	
	// populate rosters
    private void syncFriendsToRosters() {
		try {
			final ContactList contactList = Skype.getContactList();
			contactList.addPropertyChangeListener(new SkypeContactListListener(
					this, contactList));

			final Friend[] friends = contactList.getAllFriends();
			for (Friend friend : friends) {

				friend.addPropertyChangeListener(new SkypeUserListener(this,
						friend));

				// use full name if display name is not set
				String displayName = friend.getDisplayName();
				if (displayName == null || "".equals(displayName)) {
					displayName = friend.getFullName();
				}
				SkypeBuddy buddy = new SkypeBuddy(getBuddyManager(),
						friend.getId(), displayName, "Skype");

				// Status
				Status status = friend.getStatus();
				buddy.setPresence(convertSkypeStatusToXMPP(status));

				getBuddyManager().storeBuddy(buddy);
			}
			getTransport().syncLegacyRoster(getJID(),
					getBuddyManager().getBuddies());
			getBuddyManager().activate();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
}
