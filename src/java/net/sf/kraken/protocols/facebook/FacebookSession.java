/**
 * $Revision$
 * $Date$
 *
 * Copyright 2009 Daniel Henninger.  All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.protocols.facebook;

import net.sf.kraken.registration.Registration;
import net.sf.kraken.roster.TransportBuddy;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.ChatStateType;
import net.sf.kraken.type.PresenceType;
import net.sf.kraken.type.TransportLoginStatus;

import org.xmpp.packet.JID;
import org.apache.log4j.Logger;
import org.json.JSONException;

import java.util.ArrayList;

/**
 * Represents a Facebook session.
 *
 * This is the interface with which the base transport functionality will
 * communicate with Facebook.
 *
 * @author Daniel Henninger
 */
public class FacebookSession extends TransportSession {

    static Logger Log = Logger.getLogger(FacebookSession.class);

    /**
     * Create a Facebook Session instance.
     *
     * @param registration Registration information used for logging in.
     * @param jid JID associated with this session.
     * @param transport Transport instance associated with this session.
     * @param priority Priority of this session.
     */
    public FacebookSession(Registration registration, JID jid, FacebookTransport transport, Integer priority) {
        super(registration, jid, transport, priority);
    }

    /* Listener */
    //private FacebookListener listener;
    
    /* Adapter */
    private FacebookAdapter adapter;

    /**
     * @see net.sf.kraken.session.TransportSession#logIn(net.sf.kraken.type.PresenceType, String)
     */
    public void logIn(PresenceType presenceType, String verboseStatus) {
        setPendingPresenceAndStatus(presenceType, verboseStatus);
        if (!isLoggedIn()) {
            adapter = new FacebookAdapter(this);
            adapter.initialize(getRegistration().getUsername(), getRegistration().getPassword());
            setLoginStatus(TransportLoginStatus.LOGGED_IN);
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#logOut()
     */
    public void logOut() {
        adapter.setVisibility(false);
        adapter.shutdown();
        cleanUp();
        sessionDisconnectedNoReconnect(null);
    }

    /**
     * @see net.sf.kraken.session.TransportSession#cleanUp()
     */
    public void cleanUp() {
        adapter = null;
    }

    /**
     * @see net.sf.kraken.session.TransportSession#addContact(org.xmpp.packet.JID, String, java.util.ArrayList)
     */
    public void addContact(JID jid, String nickname, ArrayList<String> groups) {
    }

    /**
     * @see net.sf.kraken.session.TransportSession#removeContact(net.sf.kraken.roster.TransportBuddy)
     */
    public void removeContact(TransportBuddy contact) {
    }

    /**
     * @see net.sf.kraken.session.TransportSession#updateContact(net.sf.kraken.roster.TransportBuddy)
     */
    public void updateContact(TransportBuddy contact) {
    }

    /**
     * @see net.sf.kraken.session.TransportSession#sendMessage(org.xmpp.packet.JID, String)
     */
    public void sendMessage(JID jid, String message) {
        try {
            adapter.postFacebookChatMessage(message, getTransport().convertJIDToID(jid));
        }
        catch (JSONException e) {
            Log.error("Facebook: Unable to send message.", e);
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#sendChatState(org.xmpp.packet.JID,net.sf.kraken.type.ChatStateType)
     */
    public void sendChatState(JID jid, ChatStateType chatState) {
        try {
            adapter.postTypingNotification(getTransport().convertJIDToID(jid), chatState == ChatStateType.composing ? 1 : 0);
        }
        catch (Exception e) {
            Log.error("Facebook: Unable to send composing notification.", e);
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
    }

    /**
     * @see net.sf.kraken.session.TransportSession#updateStatus(net.sf.kraken.type.PresenceType, String)
     */
    public void updateStatus(PresenceType presenceType, String verboseStatus) {
        adapter.setVisibility(true);
    }

}
