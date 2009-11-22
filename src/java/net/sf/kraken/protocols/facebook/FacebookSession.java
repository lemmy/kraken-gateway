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

import net.sf.jfacebookiml.FacebookAdapter;
import net.sf.jfacebookiml.FacebookHttpClient;
import net.sf.kraken.registration.Registration;
import net.sf.kraken.roster.TransportBuddy;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.ChatStateType;
import net.sf.kraken.type.PresenceType;
import net.sf.kraken.type.TransportLoginStatus;

import org.xmpp.packet.JID;
import org.apache.log4j.Logger;
import org.jivesoftware.util.JiveGlobals;
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

    /* Adapter */
    private FacebookAdapter adapter;
    
    /* Listener */
    private FacebookListener listener;

    /**
     * @see net.sf.kraken.session.TransportSession#logIn(net.sf.kraken.type.PresenceType, String)
     */
    public void logIn(PresenceType presenceType, String verboseStatus) {
        setPendingPresenceAndStatus(presenceType, verboseStatus);
        if (!isLoggedIn()) {
            adapter = new FacebookAdapter();
            listener = new FacebookListener(this);
            adapter.addFacebookListener(listener);
            adapter.initialize(getRegistration().getUsername(), getRegistration().getPassword());
            adapter.setVisibility(true);
            setLoginStatus(TransportLoginStatus.LOGGED_IN);
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#logOut()
     */
    public void logOut() {
        if (adapter != null) {
            adapter.setVisibility(false);
            adapter.pause();
        }
        cleanUp();
        sessionDisconnectedNoReconnect(null);
    }

    /**
     * @see net.sf.kraken.session.TransportSession#cleanUp()
     */
    public void cleanUp() {
        if (adapter != null)
            adapter.shutdown();
    	if (listener != null)
    		adapter.removeFacebookListener(listener);
    	listener = null;
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
     * @see net.sf.kraken.session.TransportSession#acceptAddContact(TransportBuddy) 
     */
    public void acceptAddContact(TransportBuddy contact) {
        // TODO: Currently unimplemented
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
        try {
            adapter.postBuddyPoke(getTransport().convertJIDToID(jid));
        }
        catch (Exception e) {
            Log.error("Facebook: Unable to send composing notification.", e);
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#updateLegacyAvatar(String, byte[])
     */
    public void updateLegacyAvatar(String type, byte[] data) {
        // we don't support this yet, may or may not implement this
    }

    /**
     * @see net.sf.kraken.session.TransportSession#updateStatus(net.sf.kraken.type.PresenceType, String)
     */
    public void updateStatus(PresenceType presenceType, String verboseStatus) {
        adapter.setVisibility(true);
        // setting status on facebook will literally publish a new status on facebook on behalf of the user.  this could
        // potentially be spammy.  therefore, we will only allow this if the admin decides to turn it on via the system property
        if (JiveGlobals.getBooleanProperty("plugin.gateway.facebook.updatestatus", false)) {
          try{
            if( verboseStatus != null) {
              Log.debug("Facebook: setting facebook status to " + verboseStatus);
              adapter.setStatusMessage(verboseStatus);
            }
          }
          catch (Exception e) {
            Log.error("Facebook: Unable to update status of user", e);
          }
        }
    }
    
    /**
     * Download the photo from the given address.
     * @param address the url
     * @return a flow of bytes contening the photo
     */
    public byte[] readUrlBytes(String address) {
    	FacebookHttpClient facebookHttpClient = new FacebookHttpClient(adapter);
    	
    	byte[] response = facebookHttpClient.getBytesMethod(address);
    	
    	if(response != null) {
    		return response;
    	} else {
    		return null;
    	}
    }

}
