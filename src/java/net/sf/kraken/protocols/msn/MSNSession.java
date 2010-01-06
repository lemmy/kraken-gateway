/**
 * $Revision$
 * $Date$
 *
 * Copyright 2008 Daniel Henninger.  All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.protocols.msn;

import net.sf.jml.*;
import net.sf.jml.impl.BasicMessenger;
import net.sf.jml.impl.MsnMessengerFactory;
import net.sf.jml.message.MsnControlMessage;
import net.sf.kraken.registration.Registration;
import net.sf.kraken.roster.TransportBuddy;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.ChatStateType;
import net.sf.kraken.type.PresenceType;

import org.apache.log4j.Logger;
import org.jivesoftware.openfire.roster.Roster;
import org.jivesoftware.openfire.roster.RosterItem;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.Base64;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.NotFoundException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a MSN session.
 *
 * This is the interface with which the base transport functionality will
 * communicate with MSN.
 *
 * @author Daniel Henninger
 */
public class MSNSession extends TransportSession {

    static Logger Log = Logger.getLogger(MSNSession.class);

    /**
     * Create a MSN Session instance.
     *
     * @param registration Registration informationed used for logging in.
     * @param jid JID associated with this session.
     * @param transport Transport instance associated with this session.
     * @param priority Priority of this session.
     */
    public MSNSession(Registration registration, JID jid, MSNTransport transport, Integer priority) {
        super(registration, jid, transport, priority);

        if (Email.parseStr(registration.getUsername()) == null) {
            Message m = new Message();
            m.setType(Message.Type.error);
            m.setTo(getJID());
            m.setFrom(getTransport().getJID());
            m.setBody(LocaleUtils.getLocalizedString("gateway.msn.illegalaccount", "kraken")+" "+registration.getUsername());
            getTransport().sendPacket(m);
            // TODO: this should probably be generic and done within base transport for -all- transports
            // TODO: Also, ths Email.parseStr could be used in the "is this a valid username" check
        }
    }

    /**
     * MSN session
     */
    private MsnMessenger msnMessenger = null;

    /**
     * MSN listener
     */
    private MSNListener msnListener = null;

    /**
     * MSN session listener
     */
    private MSNSessionListener msnSessionListener = null;

    /**
     * MSN groups.
     */
    private ConcurrentHashMap<String,MsnGroup> msnGroups = new ConcurrentHashMap<String,MsnGroup>();

    /**
     * Pending MSN groups and contact to be added.
     */
    private ConcurrentHashMap<String,ArrayList<Email>> msnPendingGroups = new ConcurrentHashMap<String,ArrayList<Email>>();

    /**
     * @see net.sf.kraken.session.TransportSession#logIn(net.sf.kraken.type.PresenceType, String)
     */
    public void logIn(PresenceType presenceType, String verboseStatus) {
        if (!isLoggedIn()) {
            Log.debug("Creating MSN session for " + registration.getUsername());
            msnMessenger = MsnMessengerFactory.createMsnMessenger(registration.getUsername(), registration.getPassword());
            msnSessionListener = new MSNSessionListener(this);
            ((BasicMessenger)msnMessenger).addSessionListener(msnSessionListener);
            if (JiveGlobals.getBooleanProperty("plugin.gateway.msn.uselegacyprotocol", true)) {
            	msnMessenger.setSupportedProtocol(new MsnProtocol[] { MsnProtocol.MSNP11 });
            }
            else {
            	msnMessenger.setSupportedProtocol(new MsnProtocol[] { MsnProtocol.MSNP15 });
            }
            if (JiveGlobals.getBooleanProperty("plugin.gateway.msn.avatars", true) && getAvatar() != null) {
                try {
                    msnMessenger.getOwner().setInitDisplayPicture(MsnObject.getInstance(
                        msnMessenger.getOwner().getEmail().getEmailAddress(),
                        Base64.decode(getAvatar().getImageData())
                    ));
                    getAvatar().setLegacyIdentifier(msnMessenger.getOwner().getDisplayPicture().getSha1c());
                }
                catch (NotFoundException e) {
                    // Allllrighty then. no avatar for us.
                }
            }

            try {
                Log.debug("Logging in to MSN session for " + msnMessenger.getOwner().getEmail());
                msnMessenger.getOwner().setInitStatus(((MSNTransport)getTransport()).convertXMPPStatusToMSN(presenceType));
                msnMessenger.getOwner().setInitPersonalMessage(verboseStatus);
                msnMessenger.setLogIncoming(false);
                msnMessenger.setLogOutgoing(false);
                msnListener = new MSNListener(this);
                msnMessenger.addListener(msnListener);
                ((BasicMessenger)msnMessenger).login(
                        JiveGlobals.getProperty("plugin.gateway.msn.connecthost", "messenger.hotmail.com"),
                        JiveGlobals.getIntProperty("plugin.gateway.msn.connectport", 1863));
            }
            catch (Exception e) {
                Log.debug("MSN user is not able to log in: " + msnMessenger.getOwner().getEmail(), e);
            }
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#logOut()
     */
    public void logOut() {
        cleanUp();
        sessionDisconnectedNoReconnect(null);
    }

    /**
     * @see net.sf.kraken.session.TransportSession#cleanUp()
     */
    public void cleanUp() {
        if (msnMessenger != null) {
            if (msnSessionListener != null) {
                ((BasicMessenger)msnMessenger).removeSessionListener(msnSessionListener);
            }
            if (msnListener != null) {
                msnMessenger.removeListener(msnListener);
            }
            try {
                msnMessenger.getOwner().setPersonalMessage("");
            }
            catch (Exception e) {
                // Ignore then.
            }
            msnMessenger.logout();
        }
        if (msnListener != null) {
            try {
                msnListener.cleanup();
            }
            catch (Exception e) {
                // No problem, we tried our best to do it smoothly.
            }
            msnListener = null;
        }
        if (msnSessionListener != null) {
            msnSessionListener = null;
        }
    }

    /**
     * Retrieves the manager for this session.
     *
     * @return Messenger instance the session is associated with.
     */
    public MsnMessenger getManager() {
        return msnMessenger;
    }

    /**
     * Records information about a group on the user's contact list.
     *
     * @param msnGroup MSN group we are storing a copy of.
     */
    public void storeGroup(MsnGroup msnGroup) {
        msnGroups.put(msnGroup.getGroupName(), msnGroup);
    }

    /**
     * Removes information about a group from the user's contact list.
     *
     * @param msnGroup MSN group we are removing a copy of.
     */
    public void unstoreGroup(MsnGroup msnGroup) {
        msnGroups.remove(msnGroup.getGroupName());
    }

    /**
     * Records a member of a pending new group that will be added later.
     *
     * @param groupName Name of group to be stored.
     * @param member Email address of member to be added.
     */
    public void storePendingGroup(String groupName, Email member) {
        if (!msnPendingGroups.containsKey(groupName)) {
            ArrayList<Email> newList = new ArrayList<Email>();
            newList.add(member);
            msnPendingGroups.put(groupName, newList);
        }
        else {
            ArrayList<Email> list = msnPendingGroups.get(groupName);
            list.add(member);
            msnPendingGroups.put(groupName, list);
        }
    }

    /**
     * Completes the addition of groups to a new contact after the contact has been created.
     *
     * @param msnContact Contact that was added.
     */
    public void completedPendingContactAdd(MsnContact msnContact) {
        try {
            Roster roster = getTransport().getRosterManager().getRoster(getJID().getNode());
            Email contact = msnContact.getEmail();
            JID contactJID = getTransport().convertIDToJID(contact.toString());
            RosterItem item = roster.getRosterItem(contactJID);

            getBuddyManager().storeBuddy(new MSNBuddy(getBuddyManager(), msnContact));

            syncContactGroups(contact, item.getGroups());
        }
        catch (UserNotFoundException e) {
            Log.debug("MSN: Unable to find roster when adding pendingcontact for "+getJID());
        }
    }

    /**
     * Completes the addition of a contact to a new group after the group has been created.
     *
     * @param msnGroup Group that was added.
     */
    public void completedPendingGroupAdd(MsnGroup msnGroup) {
        if (!msnPendingGroups.containsKey(msnGroup.getGroupName())) {
            // Nothing to do, no pending.
            return;
        }
        try {
            Roster roster = getTransport().getRosterManager().getRoster(getJID().getNode());
            for (Email contact : msnPendingGroups.get(msnGroup.getGroupName())) {
                JID contactJID = getTransport().convertIDToJID(contact.toString());
                RosterItem item = roster.getRosterItem(contactJID);
                syncContactGroups(contact, item.getGroups());
            }
        }
        catch (UserNotFoundException e) {
            Log.debug("MSN: Unable to find roster when adding pending group contacts for "+getJID());
        }
    }

    /**
     * Syncs up the MSN roster with the jabber roster.
     */
    public void syncUsers() {
        try {
            getTransport().syncLegacyRoster(getJID(), buddyManager.getBuddies());
        }
        catch (UserNotFoundException e) {
            Log.debug("Unable to sync MSN contact list for " + getJID(), e);
        }

        buddyManager.activate();
    }

    /**
     * @see net.sf.kraken.session.TransportSession#addContact(org.xmpp.packet.JID, String, java.util.ArrayList)
     */
    public void addContact(JID jid, String nickname, ArrayList<String> groups) {
        Email contact = Email.parseStr(getTransport().convertJIDToID(jid));
        if (contact == null) {
            Log.debug("MSN: Unable to update illegal contact "+jid);
            return;
        }
        msnMessenger.addFriend(contact, nickname);
    }

    /**
     * @see net.sf.kraken.session.TransportSession#removeContact(net.sf.kraken.roster.TransportBuddy)
     */
    public void removeContact(TransportBuddy contact) {
        Email email = Email.parseStr(getTransport().convertJIDToID(contact.getJID()));
        if (email == null) {
            Log.debug("MSN: Unable to update illegal contact "+contact.getJID());
            return;
        }
        msnMessenger.removeFriend(email, false);
    }

    /**
     * @see net.sf.kraken.session.TransportSession#updateContact(net.sf.kraken.roster.TransportBuddy)
     */
    public void updateContact(TransportBuddy contact) {
        Email email = Email.parseStr(getTransport().convertJIDToID(contact.getJID()));
        if (email == null) {
            Log.debug("MSN: Unable to update illegal contact "+contact.getJID());
            return;
        }
        String nickname = getTransport().convertJIDToID(contact.getJID());
        if (contact.getNickname() != null && !contact.getNickname().equals("")) {
            nickname = contact.getNickname();
        }

        try {
            MSNBuddy msnBuddy = (MSNBuddy)getBuddyManager().getBuddy(contact.getJID());
            if (msnBuddy.msnContact == null) {
                MsnContact msnContact = msnMessenger.getContactList().getContactByEmail(email);
                if (msnContact == null) {
                    Log.debug("MSN: Contact updated but doesn't exist?  Adding.");
                    addContact(contact.getJID(), nickname, (ArrayList<String>)contact.getGroups());
                    return;
                }
                else {
                    msnBuddy.setMsnContact(msnContact);
                }
            }
            if (!msnBuddy.msnContact.getFriendlyName().equals(nickname)) {
                msnMessenger.renameFriend(email, nickname);
            }
            syncContactGroups(email, (List<String>)contact.getGroups());
        }
        catch (NotFoundException e) {
            Log.debug("MSN: Newly added buddy not found in buddy manager: "+email.getEmailAddress());
        }
    }
    
    /**
     * @see net.sf.kraken.session.TransportSession#acceptAddContact(TransportBuddy) 
     */
    public void acceptAddContact(TransportBuddy contact) {
        // TODO: Currently unimplemented
    }

    /**
     * Given a legacy contact and a list of groups, makes sure that the list is in sync with
     * the actual group list.
     *
     * @param contact Email address of contact.
     * @param groups List of groups contact should be in.
     */
    public void syncContactGroups(Email contact, List<String> groups) {
        MsnContact msnContact = null;
        try {
            MSNBuddy msnBuddy = (MSNBuddy)getBuddyManager().getBuddy(getTransport().convertIDToJID(contact.getEmailAddress()));
            msnContact = msnBuddy.getMsnContact();
        }
        catch (NotFoundException e) {
            Log.debug("MSN: Buddy not found in buddy manager: "+contact.getEmailAddress());
        }

        if (msnContact == null) {
            return;
        }

        if (groups != null && !groups.isEmpty()) {
            // Create groups that do not currently exist.
            for (String group : groups) {
                if (!msnGroups.containsKey(group)) {
                    Log.debug("MSN: Group "+group+" is a new group, creating.");
                    msnMessenger.addGroup(group);
                    // Ok, short circuit here, we need to wait for this group to be added.  We'll be back.
                    storePendingGroup(group, contact);
                    return;
                }
            }
            // Make sure contact belongs to groups that we want.
            for (String group : groups) {
                Log.debug("MSN: Found "+contact+" should belong to group "+group);
                MsnGroup msnGroup = msnGroups.get(group);
                if (msnGroup != null && !msnContact.belongGroup(msnGroup)) {
                    Log.debug("MSN: "+contact+" does not belong to "+group+", copying.");
                    msnMessenger.copyFriend(contact, msnGroup.getGroupId());
                }
            }
            // Now we will clean up groups that we should no longer belong to.
            for (MsnGroup msnGroup : msnContact.getBelongGroups()) {
                Log.debug("MSN: Found "+contact+" belongs to group "+msnGroup.getGroupName());
                if (!groups.contains(msnGroup.getGroupName())) {
                    Log.debug("MSN: "+contact+" should not belong to "+msnGroup.getGroupName()+", removing.");
                    msnMessenger.removeFriend(contact, msnGroup.getGroupId());
                }
            }
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#sendMessage(org.xmpp.packet.JID, String)
     */
    public void sendMessage(JID jid, String message) {
        msnMessenger.sendText(Email.parseStr(getTransport().convertJIDToID(jid)), message.replaceAll("\n", "\r\n"));
    }

    /**
     * @see net.sf.kraken.session.TransportSession#sendChatState(org.xmpp.packet.JID,net.sf.kraken.type.ChatStateType)
     */
    public void sendChatState(JID jid, ChatStateType chatState) {
        if (chatState.equals(ChatStateType.composing)) {
            Email jidEmail = Email.parseStr(getTransport().convertJIDToID(jid));
            MsnControlMessage mcm = new MsnControlMessage();
            mcm.setTypingUser(msnMessenger.getOwner().getEmail().getEmailAddress());
            for (MsnSwitchboard sb : msnMessenger.getActiveSwitchboards()) {
                if (sb.containContact(jidEmail)) {
                    sb.sendMessage(mcm, true);
                }
            }
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#sendBuzzNotification(org.xmpp.packet.JID, String)
     */
    public void sendBuzzNotification(JID jid, String message) {
    }

    /**
     * @see net.sf.kraken.session.TransportSession#updateLegacyAvatar(String, byte[])
     */
    public void updateLegacyAvatar(String type, byte[] data) {
        msnMessenger.getOwner().setDisplayPicture(MsnObject.getInstance(
            msnMessenger.getOwner().getEmail().getEmailAddress(),
            data
        ));
        getAvatar().setLegacyIdentifier(msnMessenger.getOwner().getDisplayPicture().getSha1c());
    }

    /**
     * @see net.sf.kraken.session.TransportSession#updateStatus(net.sf.kraken.type.PresenceType, String)
     */
    public void updateStatus(PresenceType presenceType, String verboseStatus) {
        if (isLoggedIn()) {
            try {
                msnMessenger.getOwner().setPersonalMessage(verboseStatus);
                msnMessenger.getOwner().setStatus(((MSNTransport)getTransport()).convertXMPPStatusToMSN(presenceType));
            }
            catch (IllegalStateException e) {
//                // Hrm, not logged in?  Lets fix that.
//                msnMessenger.getOwner().setInitStatus(((MSNTransport)getTransport()).convertJabStatusToMSN(presenceType));
//                msnMessenger.login();
            }
        }
        else {
//            // Hrm, not logged in?  Lets fix that.
//            msnMessenger.getOwner().setInitStatus(((MSNTransport)getTransport()).convertJabStatusToMSN(presenceType));
//            msnMessenger.login();
        }
    }

}
