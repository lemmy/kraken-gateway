/**
 * $Revision$
 * $Date$
 *
 * Copyright 2008 Daniel Henninger.  All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.protocols.sametime;

import net.sf.kraken.*;
import net.sf.kraken.registration.Registration;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.PresenceType;
import net.sf.kraken.type.TransportLoginStatus;

import org.jivesoftware.util.LocaleUtils;
import org.xmpp.packet.JID;

import com.lotus.sametime.core.types.STUserStatus;

/**
 * SameTime Transport Interface.
 *
 * This handles the bulk of the XMPP work via BaseTransport and provides
 * some gateway specific interactions.
 *
 * @author Daniel Henninger
 */
public class SameTimeTransport extends BaseTransport {

    /**
     * @see net.sf.kraken.BaseTransport#getTerminologyUsername()
     */
    public String getTerminologyUsername() {
        return LocaleUtils.getLocalizedString("gateway.sametime.username", "kraken");
    }

    /**
     * @see net.sf.kraken.BaseTransport#getTerminologyPassword()
     */
    public String getTerminologyPassword() {
        return LocaleUtils.getLocalizedString("gateway.sametime.password", "kraken");
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
        return LocaleUtils.getLocalizedString("gateway.sametime.registration", "kraken");
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
     * Handles creating a SameTime session and triggering a login.
     *
     * @param registration Registration information to be used to log in.
     * @param jid JID that is logged into the transport.
     * @param presenceType Type of presence.
     * @param verboseStatus Longer status description.
     */
    public TransportSession registrationLoggedIn(Registration registration, JID jid, PresenceType presenceType, String verboseStatus, Integer priority) {
        TransportSession session = new SameTimeSession(registration, jid, this, priority);
        session.setLoginStatus(TransportLoginStatus.LOGGING_IN);
        session.logIn(presenceType, verboseStatus);
        return session;
    }

    /**
     * Handles logging out of a SameTime session.
     *
     * @param session The session to be disconnected.
     */
    public void registrationLoggedOut(TransportSession session) {
        session.setLoginStatus(TransportLoginStatus.LOGGING_OUT);
        session.logOut();
    }

    /**
     * Converts a XMPP status to a SameTime status.
     *
     * @param jabStatus XMPP presence type.
     * @param hasVerbose We have a verbose status so give me the appropriate status type.
     * @return SameTime user status type.
     */
    public short convertXMPPStatusToSameTime(PresenceType jabStatus, Boolean hasVerbose) {
        if (jabStatus == PresenceType.available) {
            return STUserStatus.ST_USER_STATUS_ACTIVE;
        }
        else if (jabStatus == PresenceType.away) {
            return STUserStatus.ST_USER_STATUS_AWAY;
        }
        else if (jabStatus == PresenceType.xa) {
            return STUserStatus.ST_USER_STATUS_AWAY;
        }
        else if (jabStatus == PresenceType.dnd) {
            return STUserStatus.ST_USER_STATUS_DND;
        }
        else if (jabStatus == PresenceType.chat) {
            return STUserStatus.ST_USER_STATUS_ACTIVE;
        }
        else if (jabStatus == PresenceType.unavailable) {
            return STUserStatus.ST_USER_STATUS_OFFLINE;
        }
        else {
            return STUserStatus.ST_USER_STATUS_UNKNOWN;
        }
    }

    /**
     * Converts a SameTime status to an XMPP status.
     *
     * @param stUserStatus SameTime user status constant.
     * @return XMPP presence type.
     */
    public PresenceType convertSameTimeStatusToXMPP(short stUserStatus) {
        switch (stUserStatus) {
            case STUserStatus.ST_USER_STATUS_AWAY:
            case STUserStatus.ST_USER_STATUS_NOT_USING:
                return PresenceType.away;
                
            case STUserStatus.ST_USER_STATUS_DND:
                return PresenceType.dnd;

            case STUserStatus.ST_USER_STATUS_ACTIVE:
            case STUserStatus.ST_USER_STATUS_ACTIVE_MOBILE:
            case STUserStatus.ST_USER_STATUS_MOBILE:
                return PresenceType.available;
                
            case STUserStatus.ST_USER_STATUS_OFFLINE:
                return PresenceType.unavailable;
                
            case STUserStatus.ST_USER_STATUS_UNKNOWN:
            default:
                return PresenceType.unknown;
        }
     }

}
