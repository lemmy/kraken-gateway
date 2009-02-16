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

import net.sf.kraken.*;
import net.sf.kraken.registration.Registration;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.PresenceType;
import net.sf.kraken.type.TransportLoginStatus;

import org.jivesoftware.util.LocaleUtils;
import org.xmpp.packet.JID;

/**
 * Facebook Transport Interface.
 *
 * This handles the bulk of the XMPP work via BaseTransport and provides
 * some gateway specific interactions.
 *
 * @author Daniel Henninger
 */
public class FacebookTransport extends BaseTransport {

    /**
     * @see net.sf.kraken.BaseTransport#getTerminologyUsername()
     */
    public String getTerminologyUsername() {
        return LocaleUtils.getLocalizedString("gateway.facebook.username", "kraken");
    }

    /**
     * @see net.sf.kraken.BaseTransport#getTerminologyPassword()
     */
    public String getTerminologyPassword() {
        return LocaleUtils.getLocalizedString("gateway.facebook.password", "kraken");
    }

    /**
     * @see net.sf.kraken.BaseTransport#getTerminologyNickname()
     */
    public String getTerminologyNickname() {
        return null;
    }

    /**
     * @see net.sf.kraken.BaseTransport#getTerminologyRegistration()
     */
    public String getTerminologyRegistration() {
        return LocaleUtils.getLocalizedString("gateway.facebook.registration", "kraken");
    }

    /**
     * @see net.sf.kraken.BaseTransport#isPasswordRequired()
     */
    public Boolean isPasswordRequired() { return true; }

    /**
     * @see net.sf.kraken.BaseTransport#isNicknameRequired()
     */
    public Boolean isNicknameRequired() { return false; }

    /**
     * @see net.sf.kraken.BaseTransport#isUsernameValid(String)
     */
    public Boolean isUsernameValid(String username) {
        return true;
    }

    /**
     * Handles creating a Facebook session and triggering a login.
     *
     * @param registration Registration information to be used to log in.
     * @param jid JID that is logged into the transport.
     * @param presenceType Type of presence.
     * @param verboseStatus Longer status description.
     */
    public TransportSession registrationLoggedIn(Registration registration, JID jid, PresenceType presenceType, String verboseStatus, Integer priority) {
        TransportSession session = new FacebookSession(registration, jid, this, priority);
        session.setLoginStatus(TransportLoginStatus.LOGGING_IN);
        session.logIn(presenceType, verboseStatus);
        return session;
    }

    /**
     * Handles logging out of a Facebook session.
     *
     * @param session The session to be disconnected.
     */
    public void registrationLoggedOut(TransportSession session) {
        session.setLoginStatus(TransportLoginStatus.LOGGING_OUT);
        session.logOut();
    }

    /**
     * Converts a XMPP status to a Facebook status.
     *
     * @param jabStatus XMPP presence type.
     * @param hasVerbose We have a verbose status so give me the appropriate status type.
     * @return Facebook user status type.
     */
    public short convertXMPPStatusToFacebook(PresenceType jabStatus, Boolean hasVerbose) {
        if (jabStatus == PresenceType.available) {
        }
        else if (jabStatus == PresenceType.away) {
        }
        else if (jabStatus == PresenceType.xa) {
        }
        else if (jabStatus == PresenceType.dnd) {
        }
        else if (jabStatus == PresenceType.chat) {
        }
        else if (jabStatus == PresenceType.unavailable) {
        }
        else {
        }
        return -1;
    }

    /**
     * Converts a Facebook status to an XMPP status.
     *
     * @param fbUserStatus Facebook user status constant.
     * @return XMPP presence type.
     */
    public PresenceType convertFacebookStatusToXMPP(short fbUserStatus) {
        switch (fbUserStatus) {
            default:
                return PresenceType.unknown;
        }
     }

}
