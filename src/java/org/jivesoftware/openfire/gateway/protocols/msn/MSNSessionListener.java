/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2006-2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.openfire.gateway.protocols.msn;

import org.apache.log4j.Logger;
import org.jivesoftware.openfire.gateway.type.TransportLoginStatus;
import org.jivesoftware.util.LocaleUtils;

import java.lang.ref.WeakReference;

import net.sf.cindy.SessionAdapter;
import net.sf.cindy.Session;
import net.sf.cindy.Message;

/**
 * MSN Session Listener Interface.
 *
 * This handles listening to session activities.
 *
 * @author lionheart@clansk.org
 * @author Daniel Henninger
 */
public class MSNSessionListener extends SessionAdapter {

    static Logger Log = Logger.getLogger(MSNSessionListener.class);

    public MSNSessionListener(MSNSession msnSession) {
        this.msnSessionRef = new WeakReference<MSNSession>(msnSession);
    }

    /**
     * The session this listener is associated with.
     */
    public WeakReference<MSNSession> msnSessionRef = null;

    /**
     * Returns the MSN session this listener is attached to.
     *
     * @return MSN session we are attached to.
     */
    public MSNSession getSession() {
        return msnSessionRef.get();
    }

    public void exceptionCaught(Session arg0, Throwable t) throws Exception{
        Log.debug("MSN: Session exceptionCaught for "+getSession().getRegistration().getUsername()+" : "+t);
    }

    public void messageReceived(Session arg0, Message message) throws Exception {
        Log.debug("MSN: Session messageReceived for "+getSession().getRegistration().getUsername()+" : "+message);
        // TODO: Kinda hacky, would like to improve on this later.
        if (message.toString().startsWith("OUT OTH")) {
            // Forced disconnect because account logged in elsewhere
            getSession().setLoginStatus(TransportLoginStatus.DISCONNECTED);
            getSession().sessionDisconnectedNoReconnect(LocaleUtils.getLocalizedString("gateway.msn.otherloggedin", "gateway"));
        }
        else if (message.toString().startsWith("OUT SDH")) {
            // Forced disconnect from server for maintenance
            getSession().setLoginStatus(TransportLoginStatus.DISCONNECTED);
            getSession().sessionDisconnectedNoReconnect(LocaleUtils.getLocalizedString("gateway.msn.disconnect", "gateway"));
        }
    }

    public void messageSent(Session arg0, Message message) throws Exception {
        Log.debug("MSN: Session messageSent for "+getSession().getRegistration().getUsername()+" : "+message);
    }

    public void sessionIdle(Session session) throws Exception {
    }

    public void sessionEstablished(Session session) {
        Log.debug("MSN: Session established for "+getSession().getRegistration().getUsername());
    }

    public void sessionTimeout(Session session) {
        // This is used to handle regular pings to the MSN server.  No need to mention it.
    }

    public void sessionClosed(Session session) {
        Log.debug("MSN: Session closed for "+getSession().getRegistration().getUsername());
    }

}
