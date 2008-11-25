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
import net.sf.jml.exception.*;
import net.sf.jml.event.MsnAdapter;
import net.sf.jml.message.*;
import net.sf.jml.message.p2p.DisplayPictureRetrieveWorker;
import net.sf.kraken.avatars.Avatar;
import net.sf.kraken.type.TransportLoginStatus;

import org.apache.log4j.Logger;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.NotFoundException;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;

/**
 * MSN Listener Interface.
 *
 * This handles real interaction with MSN, but mostly is a listener for
 * incoming events from MSN.
 *
 * @author Daniel Henninger
 */
public class MSNListener extends MsnAdapter {

    static Logger Log = Logger.getLogger(MSNListener.class);

    /**
     * Creates the MSN Listener instance.
     *
     * @param session Session this listener is associated with.
     */
    public MSNListener(MSNSession session) {
        this.msnSessionRef = new WeakReference<MSNSession>(session);
        sessionReaper = new SessionReaper();
        timer.schedule(sessionReaper, reaperInterval, reaperInterval);
    }

    /**
     * The session this listener is associated with.
     */
    public WeakReference<MSNSession> msnSessionRef = null;

    /**
     * Timer to check for stale typing notifications.
     */
    private Timer timer = new Timer();

    /**
     * Interval at which typing notifications are reaped.
     */
    private int reaperInterval = 5000; // 5 seconds

    /**
     * The actual reaper task.
     */
    private SessionReaper sessionReaper;

    /**
     * Record of active typing notifications.
     */
    private ConcurrentHashMap<String,Date> typingNotificationMap = new ConcurrentHashMap<String,Date>();

    /**
     * Returns the MSN session this listener is attached to.
     *
     * @return MSN session we are attached to.
     */
    public MSNSession getSession() {
        return msnSessionRef.get();
    }
    
    /**
     * Handles incoming messages from MSN users.
     */
    public void instantMessageReceived(MsnSwitchboard switchboard, MsnInstantMessage message, MsnContact friend) {
        getSession().getTransport().sendMessage(
                getSession().getJID(),
                getSession().getTransport().convertIDToJID(friend.getEmail().toString()),
                message.getContent()
        );
    }

    /**
     * Handles incoming system messages from MSN.
     *
     * @param switchboard Switchboard session the message is associated with.
     * @param message MSN message.
     */
    public void systemMessageReceived(MsnSwitchboard switchboard, MsnInstantMessage message) {
        getSession().getTransport().sendMessage(
                getSession().getJID(),
                getSession().getTransport().getJID(),
                message.getContent()
        );
    }

    /**
     * Handles incoming control messages from MSN.
     */
    public void controlMessageReceived(MsnSwitchboard switchboard, MsnControlMessage message, MsnContact friend) {
        if (message.getTypingUser() != null) {
            getSession().getTransport().sendComposingNotification(
                    getSession().getJID(),
                    getSession().getTransport().convertIDToJID(friend.getEmail().toString())
            );
            typingNotificationMap.put(friend.getEmail().toString(), new Date());
        }
        else {
            Log.debug("MSN: Received unknown control msg to " + switchboard + " from " + friend + ": " + message);
        }
    }

    /**
     * Handles incoming datacast messages from MSN.
     */
    public void datacastMessageReceived(MsnSwitchboard switchboard, MsnDatacastMessage message, MsnContact friend) {
        if (message.getId() == 1) {
            getSession().getTransport().sendMessage(
                    getSession().getJID(),
                    getSession().getTransport().convertIDToJID(friend.getEmail().toString()),
                    LocaleUtils.getLocalizedString("gateway.msn.nudge", "kraken"),
                    Message.Type.headline
            );
        }
        else if (message.getId() == 2) {
            getSession().getTransport().sendMessage(
                    getSession().getJID(),
                    getSession().getTransport().convertIDToJID(friend.getEmail().toString()),
                    LocaleUtils.getLocalizedString("gateway.msn.wink", "kraken"),
                    Message.Type.headline
            );
        }
        else {
            Log.debug("MSN: Received unknown datacast message to " + switchboard + " from " + friend + ": " + message);
        }
    }

    /**
     * Handles incoming unknown messages from MSN.
     */
    public void unknownMessageReceived(MsnSwitchboard switchboard, MsnUnknownMessage message, MsnContact friend) {
        Log.debug("MSN: Received unknown message to " + switchboard + " from " + friend + ": " + message);
    }

    public void initialEmailNotificationReceived(MsnSwitchboard switchboard, MsnEmailInitMessage message, MsnContact contact) {
        Log.debug("MSN: Got init email notify "+message.getInboxUnread()+" unread message(s)");
        if (JiveGlobals.getBooleanProperty("plugin.gateway.msn.mailnotifications", true) && message.getInboxUnread() > 0) {
            getSession().getTransport().sendMessage(
                    getSession().getJID(),
                    getSession().getTransport().getJID(),
                    LocaleUtils.getLocalizedString("gateway.msn.initialmail", "kraken", Arrays.asList(message.getInboxUnread())),
                    Message.Type.headline
            );
        }
    }

    public void initialEmailDataReceived(MsnSwitchboard switchboard, MsnEmailInitEmailData message, MsnContact contact) {
        Log.debug("MSN: Got init email data "+message.getInboxUnread()+" unread message(s)");
        if (JiveGlobals.getBooleanProperty("plugin.gateway.msn.mailnotifications", true) && message.getInboxUnread() > 0) {
            getSession().getTransport().sendMessage(
                    getSession().getJID(),
                    getSession().getTransport().getJID(),
                    LocaleUtils.getLocalizedString("gateway.msn.initialmail", "kraken", Arrays.asList(message.getInboxUnread())),
                    Message.Type.headline
            );
        }
    }

    public void newEmailNotificationReceived(MsnSwitchboard switchboard, MsnEmailNotifyMessage message, MsnContact contact) {
        Log.debug("MSN: Got new email notification from "+message.getFrom()+" <"+message.getFromAddr()+">");
        if (JiveGlobals.getBooleanProperty("plugin.gateway.msn.mailnotifications", true)) {
            getSession().getTransport().sendMessage(
                    getSession().getJID(),
                    getSession().getTransport().getJID(),
                    LocaleUtils.getLocalizedString("gateway.msn.mail", "kraken", Arrays.asList(message.getFrom(), message.getFromAddr(), message.getSubject())),
                    Message.Type.headline
            );
        }
    }

    public void activityEmailNotificationReceived(MsnSwitchboard switchboard, MsnEmailActivityMessage message, MsnContact contact) {
        Log.debug("MSN: Got email activity notification "+message.getSrcFolder()+" to "+message.getDestFolder());
    }

    /**
     * The user's login has completed and was accepted.
     */
    public void loginCompleted(MsnMessenger messenger) {
        Log.debug("MSN: Login completed for "+messenger.getOwner().getEmail());
        getSession().setLoginStatus(TransportLoginStatus.LOGGED_IN);
    }

    /**
     * Contact list has been synced.
     */
    public void contactListSyncCompleted(MsnMessenger messenger) {
        Log.debug("MSN: Contact list sync for "+messenger.getOwner().getEmail());
    }
    
    /**
     * Contact list initialization has completed.
     */
    public void contactListInitCompleted(MsnMessenger messenger) {
        for (MsnGroup msnGroup : messenger.getContactList().getGroups()) {
            Log.debug("MSN: Got group "+msnGroup);
            getSession().storeGroup(msnGroup);
        }
        for (MsnContact msnContact : messenger.getContactList().getContacts()) {
            Log.debug("MSN: Got contact "+msnContact);
            if (msnContact.isInList(MsnList.FL) && msnContact.getEmail() != null) {
                final MSNBuddy buddy = new MSNBuddy(getSession().getBuddyManager(), msnContact);
                getSession().getBuddyManager().storeBuddy(buddy);
                if (JiveGlobals.getBooleanProperty("plugin.gateway.msn.avatars", true)) {
                    final MsnObject msnAvatar = msnContact.getAvatar();
                    if (msnAvatar != null && (buddy.getAvatar() == null || !buddy.getAvatar().getLegacyIdentifier().equals(msnAvatar.getSha1c()))) {
                        try {
                            messenger.retrieveDisplayPicture(msnAvatar,
                                    new DisplayPictureListener() {

                                        public void notifyMsnObjectRetrieval(
                                                MsnMessenger messenger,
                                                DisplayPictureRetrieveWorker worker,
                                                MsnObject msnObject,
                                                ResultStatus result,
                                                byte[] resultBytes,
                                                Object context) {

                                            Log.debug("MSN: Got avatar retrieval result: "+result);

                                            // Check for the value
                                            if (result == ResultStatus.GOOD) {

                                                try {
                                                    Log.debug("MSN: Found avatar of length "+resultBytes.length);
                                                    Avatar avatar = new Avatar(buddy.getJID(), msnAvatar.getSha1c(), resultBytes);
                                                    buddy.setAvatar(avatar);
                                                }
                                                catch (IllegalArgumentException e) {
                                                    Log.debug("MSN: Got null avatar, ignoring.");
                                                }
                                            }
                                        }
                                    });
                        }
                        catch (Exception e) {
                            Log.debug("MSN: Unable to retrieve MSN avatar: ", e);
                        }

                    }
                    else if (buddy.getAvatar() != null && msnAvatar == null) {
                        buddy.setAvatar(null);
                    }
                }
            }
        }
        getSession().syncUsers();
    }

    /**
     * A friend for this user has changed status.
     */
    public void contactStatusChanged(MsnMessenger messenger, MsnContact friend) {
        if (!friend.isInList(MsnList.FL) || friend.getEmail() == null) {
            // Not in our buddy list, don't care, or null email address.  We need that.
            return;
        }
        if (getSession().getBuddyManager().isActivated()) {
            try {
                final MSNBuddy buddy = (MSNBuddy)getSession().getBuddyManager().getBuddy(getSession().getTransport().convertIDToJID(friend.getEmail().toString()));
                buddy.setPresenceAndStatus(((MSNTransport)getSession().getTransport()).convertMSNStatusToXMPP(friend.getStatus()), friend.getPersonalMessage());
                buddy.setMsnContact(friend);
                if (JiveGlobals.getBooleanProperty("plugin.gateway.msn.avatars", true)) {
                    final MsnObject msnAvatar = friend.getAvatar();
                    if (msnAvatar != null && (buddy.getAvatar() == null || !buddy.getAvatar().getLegacyIdentifier().equals(msnAvatar.getSha1c()))) {
                        try {
                            messenger.retrieveDisplayPicture(msnAvatar,
                                    new DisplayPictureListener() {

                                        public void notifyMsnObjectRetrieval(
                                                MsnMessenger messenger,
                                                DisplayPictureRetrieveWorker worker,
                                                MsnObject msnObject,
                                                ResultStatus result,
                                                byte[] resultBytes,
                                                Object context) {

                                            Log.debug("MSN: Got avatar retrieval result: "+result);

                                            // Check for the value
                                            if (result == ResultStatus.GOOD) {

                                                try {
                                                    Log.debug("MSN: Found avatar of length "+resultBytes.length);
                                                    Avatar avatar = new Avatar(buddy.getJID(), msnAvatar.getSha1c(), resultBytes);
                                                    buddy.setAvatar(avatar);
                                                }
                                                catch (IllegalArgumentException e) {
                                                    Log.debug("MSN: Got null avatar, ignoring.");
                                                }
                                            }
                                        }
                                    });
                        }
                        catch (Exception e) {
                            Log.debug("MSN: Unable to retrieve MSN avatar: ", e);
                        }

                    }
                    else if (buddy.getAvatar() != null && msnAvatar == null) {
                        buddy.setAvatar(null);
                    }
                }
            }
            catch (NotFoundException e) {
                // Not in our contact list.  Ignore.
                Log.debug("MSN: Received presense notification for contact we don't care about: "+friend.getEmail().toString());
            }
        }
        else {
            getSession().getBuddyManager().storePendingStatus(getSession().getTransport().convertIDToJID(friend.getEmail().toString()), ((MSNTransport)getSession().getTransport()).convertMSNStatusToXMPP(friend.getStatus()), friend.getPersonalMessage());
        }
    }

    /**
     * Someone added us to their contact list.
     */
    public void contactAddedMe(MsnMessenger messenger, MsnContact friend) {
        Presence p = new Presence();
        p.setType(Presence.Type.subscribe);
        p.setTo(getSession().getJID());
        p.setFrom(getSession().getTransport().convertIDToJID(friend.getEmail().toString()));
        getSession().getTransport().sendPacket(p);
    }

    /**
     * Someone removed us from their contact list.
     */
    public void contactRemovedMe(MsnMessenger messenger, MsnContact friend) {
        Presence p = new Presence();
        p.setType(Presence.Type.unsubscribe);
        p.setTo(getSession().getJID());
        p.setFrom(getSession().getTransport().convertIDToJID(friend.getEmail().toString()));
        getSession().getTransport().sendPacket(p);
    }

    /**
     * A contact we added has been added to the server.
     */
    public void contactAddCompleted(MsnMessenger messenger, MsnContact contact) {
        Log.debug("MSN: Contact add completed: "+contact);
//        Presence p = new Presence();
//        p.setType(Presence.Type.subscribed);
//        p.setTo(getSession().getJID());
//        p.setFrom(getSession().getTransport().convertIDToJID(contact.getEmail().toString()));
//        getSession().getTransport().sendPacket(p);
        getSession().completedPendingContactAdd(contact);
    }

    /**
     * A contact we removed has been removed from the server.
     */
    public void contactRemoveCompleted(MsnMessenger messenger, MsnContact contact) {
        Log.debug("MSN: Contact remove completed: "+contact);
//        Presence p = new Presence();
//        p.setType(Presence.Type.unsubscribed);
//        p.setTo(getSession().getJID());
//        p.setFrom(getSession().getTransport().convertIDToJID(contact.getEmail().toString()));
//        getSession().getTransport().sendPacket(p);
    }

    /**
     * A group we added has been added to the server.
     */
    public void groupAddCompleted(MsnMessenger messenger, MsnGroup group) {
        Log.debug("MSN: Group add completed: "+group);
        getSession().storeGroup(group);
        getSession().completedPendingGroupAdd(group);
    }

    /**
     * A group we removed has been removed from the server.
     */
    public void groupRemoveCompleted(MsnMessenger messenger, MsnGroup group) {
        Log.debug("MSN: Group remove completed: "+group);
        getSession().unstoreGroup(group);
    }

    /**
     * Owner status has changed.
     */
    public void ownerStatusChanged(MsnMessenger messenger) {
        getSession().setPresenceAndStatus(((MSNTransport)getSession().getTransport()).convertMSNStatusToXMPP(messenger.getOwner().getStatus()), messenger.getOwner().getPersonalMessage());
    }

    /**
     * Catches MSN exceptions.
     */
    public void exceptionCaught(MsnMessenger messenger, Throwable throwable) {
        Log.debug("MSN: Exception occurred for "+messenger.getOwner().getEmail()+" : "+throwable);        
        if (throwable instanceof IncorrectPasswordException) {
            getSession().setLoginStatus(TransportLoginStatus.DISCONNECTED);
            getSession().sessionDisconnectedNoReconnect(LocaleUtils.getLocalizedString("gateway.msn.passwordincorrect", "kraken"));
        }
        else if (throwable instanceof MsnProtocolException) {
            Log.debug("MSN: Protocol exception: "+throwable.toString());
        }
        else if (throwable instanceof MsgNotSendException) {
            getSession().getTransport().sendMessage(
                    getSession().getJID(),
                    getSession().getTransport().getJID(),
                    LocaleUtils.getLocalizedString("gateway.msn.sendmsgfailed", "kraken")+" "+throwable.toString(),
                    Message.Type.error
            );
        }
        else if (throwable instanceof UnknownMessageException) {
            Log.debug("MSN: Unknown message: "+throwable.toString());
        }
        else if (throwable instanceof UnsupportedProtocolException) {
            Log.debug("MSN: Protocol error: "+throwable.toString());
        }
        else if (throwable instanceof IOException) {
            Log.debug("MSN: IO error: "+throwable.toString());
            getSession().setLoginStatus(TransportLoginStatus.DISCONNECTED);
            getSession().sessionDisconnected(LocaleUtils.getLocalizedString("gateway.msn.disconnect", "kraken"));
        }
        else {
            Log.debug("MSN: Unknown error: "+throwable.toString(), throwable);
        }
    }

    /**
     * Clean up any active typing notifications that are stale.
     */
    private class SessionReaper extends TimerTask {
        /**
         * Silence any typing notifications that are stale.
         */
        public void run() {
            cancelTypingNotifications();
        }
    }

    /**
     * Any typing notification that hasn't been heard in 10 seconds will be killed.
     */
    private void cancelTypingNotifications() {
        for (String source : typingNotificationMap.keySet()) {
            if (typingNotificationMap.get(source).getTime() < ((new Date().getTime()) - 10000)) {
                getSession().getTransport().sendChatInactiveNotification(
                        getSession().getJID(),
                        getSession().getTransport().convertIDToJID(source)
                );
                typingNotificationMap.remove(source);
            }
        }
    }

    /**
     * Cleans up any leftover entities.
     */
    public void cleanup() {
        try {
            timer.cancel();
        }
        catch (Exception e) {
            // Ignore
        }
        try {
            typingNotificationMap.clear();
        }
        catch (Exception e) {
            // Ignore
        }
        try {
            timer.purge();
        }
        catch (Exception e) {
            // Ignore
        }
        timer = null;
        try {
            sessionReaper.cancel();
        }
        catch (Exception e) {
            // Ignore
        }
        sessionReaper = null;
    }

    /**
     * Retrieves the session reaper.
     * @return Session reaper associated with session.
     */
    public SessionReaper getSessionReaper() {
        return sessionReaper;
    }

}
