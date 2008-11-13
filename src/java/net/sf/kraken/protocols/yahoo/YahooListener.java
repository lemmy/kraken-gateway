/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2006-2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.protocols.yahoo;

import net.sf.kraken.type.TransportLoginStatus;

import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.NotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;
import org.openymsg.network.YahooUser;
import org.openymsg.network.event.*;
import org.openymsg.support.MessageDecoder;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.lang.ref.WeakReference;

/**
 * Handles incoming packets from Yahoo.
 *
 * This takes care of events we don't do anything with yet by logging them.
 *
 * @author Daniel Henninger
 * Heavily inspired by Noah Campbell's work.
 */
public class YahooListener extends SessionAdapter {

    static Logger Log = Logger.getLogger(YahooListener.class);

    /**
     * Handles converting messages between formats.
     */
    private MessageDecoder messageDecoder = new MessageDecoder();

    /**
     * Indicator for whether we've gotten through the initial email notification.
     */
    private Boolean emailInitialized = false;

    /**
     * Creates a Yahoo session listener affiliated with a session.
     *
     * @param session The YahooSession instance we are associatd with.
     */
    public YahooListener(YahooSession session) {
        this.yahooSessionRef = new WeakReference<YahooSession>(session);
    }

    /**
     * The transport session we are affiliated with.
     */
    WeakReference<YahooSession> yahooSessionRef;

    /**
     * Returns the Yahoo session this listener is attached to.
     *
     * @return Yahoo session we are attached to.
     */
    public YahooSession getSession() {
        return yahooSessionRef.get();
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#messageReceived(org.openymsg.network.event.SessionEvent)
     */
    public void messageReceived(SessionEvent event) {
        getSession().getTransport().sendMessage(
                getSession().getJID(),
                getSession().getTransport().convertIDToJID(event.getFrom()),
                messageDecoder.decodeToText(event.getMessage())
        );
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#offlineMessageReceived(org.openymsg.network.event.SessionEvent)
     */
    public void offlineMessageReceived(SessionEvent event) {
        getSession().getTransport().sendMessage(
                getSession().getJID(),
                getSession().getTransport().convertIDToJID(event.getFrom()),
                messageDecoder.decodeToText(event.getMessage())
        );
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#newMailReceived(org.openymsg.network.event.SessionNewMailEvent)
     */
    public void newMailReceived(SessionNewMailEvent event) {
        if (JiveGlobals.getBooleanProperty("plugin.gateway.yahoo.mailnotifications", true) && (emailInitialized || event.getMailCount() > 0)) {
            if (!emailInitialized) {
                getSession().getTransport().sendMessage(
                        getSession().getJID(),
                        getSession().getTransport().getJID(),
                        LocaleUtils.getLocalizedString("gateway.yahoo.mail", "gateway", Arrays.asList(Integer.toString(event.getMailCount()))),
                        Message.Type.headline
                );
            }
            else {
                getSession().getTransport().sendMessage(
                        getSession().getJID(),
                        getSession().getTransport().getJID(),
                        LocaleUtils.getLocalizedString("gateway.yahoo.newmail", "gateway"),
                        Message.Type.headline
                );
            }
        }
        emailInitialized = true;
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#friendsUpdateReceived(org.openymsg.network.event.SessionFriendEvent)
     */
    public void friendsUpdateReceived(SessionFriendEvent event) {
        for (YahooUser user : event.getUsers()) {
            if (getSession().getBuddyManager().isActivated()) {
                try {
                    YahooBuddy yahooBuddy = (YahooBuddy)getSession().getBuddyManager().getBuddy(getSession().getTransport().convertIDToJID(user.getId()));
                    yahooBuddy.yahooUser = user;
                    yahooBuddy.setPresenceAndStatus(((YahooTransport)getSession().getTransport()).convertYahooStatusToXMPP(user.getStatus()), user.getCustomStatusMessage());

                }
                catch (NotFoundException e) {
                    // Not in our list.
                    Log.debug("Yahoo: Received presense notification for contact we don't care about: "+event.getFrom());
                }
            }
            else {
                getSession().getBuddyManager().storePendingStatus(getSession().getTransport().convertIDToJID(user.getId()), ((YahooTransport)getSession().getTransport()).convertYahooStatusToXMPP(user.getStatus()), user.getCustomStatusMessage());
            }
        }
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#friendAddedReceived(org.openymsg.network.event.SessionFriendEvent)
     */
    public void friendAddedReceived(SessionFriendEvent event) {
        // TODO: This means a friend -we- added is now added, do we want to use this
//        Presence p = new Presence(Presence.Type.subscribe);
//        p.setTo(getSession().getJID());
//        p.setFrom(getSession().getTransport().convertIDToJID(event.getFrom()));
//        getSession().getTransport().sendPacket(p);
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#friendRemovedReceived(org.openymsg.network.event.SessionFriendEvent)
     */
    public void friendRemovedReceived(SessionFriendEvent event) {
        // TODO: This means a friend -we- removed is now gone, do we want to use this
//        Presence p = new Presence(Presence.Type.unsubscribe);
//        p.setTo(getSession().getJID());
//        p.setFrom(getSession().getTransport().convertIDToJID(event.getFrom()));
//        getSession().getTransport().sendPacket(p);
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#groupRenameReceived(org.openymsg.network.event.SessionGroupEvent)
     */
    public void groupRenameReceived(SessionGroupEvent sessionGroupEvent) {
        // TODO: Handle this
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#chatJoinReceived(org.openymsg.network.event.SessionChatEvent)
     */
    public void chatJoinReceived(SessionChatEvent sessionChatEvent) {
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#chatExitReceived(org.openymsg.network.event.SessionChatEvent)
     */
    public void chatExitReceived(SessionChatEvent sessionChatEvent) {
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#connectionClosed(org.openymsg.network.event.SessionEvent)
     */
    public void connectionClosed(SessionEvent event) {
        Log.debug(event.toString());
        if (getSession().isLoggedIn()) {
            getSession().setLoginStatus(TransportLoginStatus.DISCONNECTED);
            getSession().sessionDisconnectedNoReconnect(null);
        }
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#fileTransferReceived(org.openymsg.network.event.SessionFileTransferEvent)
     */
    public void fileTransferReceived(SessionFileTransferEvent event) {
        Log.debug(event.toString());
    }


    /**
     * @see org.openymsg.network.event.SessionAdapter#listReceived(org.openymsg.network.event.SessionEvent)
     */
    public void listReceived(SessionEvent event) {
        // We just got the entire contact list.  Lets sync up.
        getSession().syncUsers();
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#buzzReceived(org.openymsg.network.event.SessionEvent)
     */
    public void buzzReceived(SessionEvent event) {
        getSession().getTransport().sendBuzzNotification(
                getSession().getJID(),
                getSession().getTransport().convertIDToJID(event.getFrom()),
                messageDecoder.decodeToText(event.getMessage())
        );
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#errorPacketReceived(org.openymsg.network.event.SessionErrorEvent)
     */
    public void errorPacketReceived(SessionErrorEvent event) {
        Log.debug("Error from yahoo: "+event.getMessage()+", Code:"+event.getCode());
        getSession().getTransport().sendMessage(
                getSession().getJID(),
                getSession().getTransport().getJID(),
                LocaleUtils.getLocalizedString("gateway.yahoo.error", "gateway")+" "+event.getMessage(),
                Message.Type.error
        );
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#inputExceptionThrown(org.openymsg.network.event.SessionExceptionEvent)
     */
    public void inputExceptionThrown(SessionExceptionEvent event) {
        Log.debug("Input error from yahoo: "+event.getMessage(), event.getException());
        // Lets keep this silent for now.  Not bother the end user with it.
//        getSession().getTransport().sendMessage(
//                getSession().getJID(),
//                getSession().getTransport().getJID(),
//                "Input error from yahoo: "+event.getMessage(),
//                Message.Type.error
//        );
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#notifyReceived(org.openymsg.network.event.SessionNotifyEvent)
     */
    public void notifyReceived(SessionNotifyEvent event) {
        Log.debug(event.toString());
        if (event.isTyping()) {
            if (event.isOn()) {
                getSession().getTransport().sendComposingNotification(
                        getSession().getJID(),
                        getSession().getTransport().convertIDToJID(event.getFrom())
                );
            }
            else {
                getSession().getTransport().sendComposingPausedNotification(
                        getSession().getJID(),
                        getSession().getTransport().convertIDToJID(event.getFrom())
                );
            }
        }
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#contactRequestReceived(org.openymsg.network.event.SessionEvent)
     */
    public void contactRequestReceived(SessionEvent event) {
        Presence p = new Presence(Presence.Type.subscribe);
        p.setTo(getSession().getJID());
        p.setFrom(getSession().getTransport().convertIDToJID(event.getFrom()));
        getSession().getTransport().sendPacket(p);
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#contactRejectionReceived(org.openymsg.network.event.SessionEvent)
     */
    public void contactRejectionReceived(SessionEvent event) {
        // TODO: Is this correct?  unsubscribed for a rejection?
        Log.debug(event.toString());
        Presence p = new Presence(Presence.Type.unsubscribed);
        p.setTo(getSession().getJID());
        p.setFrom(getSession().getTransport().convertIDToJID(event.getFrom()));
        getSession().getTransport().sendPacket(p);
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#conferenceInviteReceived(org.openymsg.network.event.SessionConferenceEvent)
     */
    public void conferenceInviteReceived(SessionConferenceEvent event) {
        Log.debug(event.toString());
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#conferenceInviteDeclinedReceived(org.openymsg.network.event.SessionConferenceEvent)
     */
    public void conferenceInviteDeclinedReceived(SessionConferenceEvent event) {
        Log.debug(event.toString());
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#conferenceLogonReceived(org.openymsg.network.event.SessionConferenceEvent)
     */
    public void conferenceLogonReceived(SessionConferenceEvent event) {
        Log.debug(event.toString());
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#conferenceLogoffReceived(org.openymsg.network.event.SessionConferenceEvent)
     */
    public void conferenceLogoffReceived(SessionConferenceEvent event) {
        Log.debug(event.toString());
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#conferenceMessageReceived(org.openymsg.network.event.SessionConferenceEvent)
     */
    public void conferenceMessageReceived(SessionConferenceEvent event) {
        Log.debug(event.toString());
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#chatMessageReceived(org.openymsg.network.event.SessionChatEvent)
     */
    public void chatMessageReceived(SessionChatEvent event) {
        Log.debug(event.toString());
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#chatUserUpdateReceived(org.openymsg.network.event.SessionChatEvent)
     */
    public void chatUserUpdateReceived(SessionChatEvent event) {
        Log.debug(event.toString());
    }

    /**
     * @see org.openymsg.network.event.SessionAdapter#chatConnectionClosed(org.openymsg.network.event.SessionEvent)
     */
    public void chatConnectionClosed(SessionEvent event) {
        Log.debug(event.toString());
    }

}
