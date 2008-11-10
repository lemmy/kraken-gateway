/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.openfire.gateway.protocols.gadugadu;

import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.openfire.gateway.*;
import org.jivesoftware.openfire.gateway.type.PresenceType;
import org.jivesoftware.openfire.gateway.type.TransportLoginStatus;
import org.jivesoftware.openfire.gateway.registration.Registration;
import org.jivesoftware.openfire.gateway.session.TransportSession;
import org.xmpp.packet.JID;
import pl.mn.communicator.StatusType;

/**
 * Gadu-Gadu Transport Interface.
 *
 * This handles the bulk of the XMPP work via BaseTransport and provides
 * some gateway specific interactions.
 *
 * @author Daniel Henninger
 */
public class GaduGaduTransport extends BaseTransport {

    /**
     * @see org.jivesoftware.openfire.gateway.BaseTransport#getTerminologyUsername()
     */
    public String getTerminologyUsername() {
        return LocaleUtils.getLocalizedString("gateway.gadugadu.username", "gateway");
    }

    /**
     * @see org.jivesoftware.openfire.gateway.BaseTransport#getTerminologyPassword()
     */
    public String getTerminologyPassword() {
        return LocaleUtils.getLocalizedString("gateway.gadugadu.password", "gateway");
    }

    /**
     * @see org.jivesoftware.openfire.gateway.BaseTransport#getTerminologyNickname()
     */
    public String getTerminologyNickname() {
        return null;
    }

    /**
     * @see org.jivesoftware.openfire.gateway.BaseTransport#getTerminologyRegistration()
     */
    public String getTerminologyRegistration() {
        return LocaleUtils.getLocalizedString("gateway.gadugadu.registration", "gateway");
    }

    /**
     * @see org.jivesoftware.openfire.gateway.BaseTransport#isPasswordRequired()
     */
    public Boolean isPasswordRequired() { return true; }

    /**
     * @see org.jivesoftware.openfire.gateway.BaseTransport#isNicknameRequired()
     */
    public Boolean isNicknameRequired() { return false; }

    /**
     * @see org.jivesoftware.openfire.gateway.BaseTransport#isUsernameValid(String)
     */
    public Boolean isUsernameValid(String username) {
        return username.matches("\\d+");
    }

    /**
     * Handles creating a Gadu-Gadu session and triggering a login.
     *
     * @param registration Registration information to be used to log in.
     * @param jid JID that is logged into the transport.
     * @param presenceType Type of presence.
     * @param verboseStatus Longer status description.
     */
    public TransportSession registrationLoggedIn(Registration registration, JID jid, PresenceType presenceType, String verboseStatus, Integer priority) {
        TransportSession session = new GaduGaduSession(registration, jid, this, priority);
        session.setLoginStatus(TransportLoginStatus.LOGGING_IN);
        session.logIn(presenceType, verboseStatus);
        return session;
    }

    /**
     * Handles logging out of a Gadu-Gadu session.
     *
     * @param session The session to be disconnected.
     */
    public void registrationLoggedOut(TransportSession session) {
        session.setLoginStatus(TransportLoginStatus.LOGGING_OUT);
        session.logOut();
    }

    /**
     * Converts a jabber status to a Gadu-Gadu status.
     *
     * @param jabStatus Jabber presence type.
     * @param hasVerbose We have a verbose status so give me the appropriate status type.
     * @return Gadu-Gadu status type.
     */
    public StatusType convertXMPPStatusToGaduGadu(PresenceType jabStatus, Boolean hasVerbose) {
        if (jabStatus == PresenceType.available) {
            if (hasVerbose) {
                return StatusType.ONLINE_WITH_DESCRIPTION;
            }
            else {
                return StatusType.ONLINE;
            }
        }
        else if (jabStatus == PresenceType.away) {
            if (hasVerbose) {
                return StatusType.BUSY_WITH_DESCRIPTION;
            }
            else {
                return StatusType.BUSY;
            }
        }
        else if (jabStatus == PresenceType.xa) {
            if (hasVerbose) {
                return StatusType.BUSY_WITH_DESCRIPTION;
            }
            else {
                return StatusType.BUSY;
            }
        }
        else if (jabStatus == PresenceType.dnd) {
            if (hasVerbose) {
                return StatusType.BUSY_WITH_DESCRIPTION;
            }
            else {
                return StatusType.BUSY;
            }
        }
        else if (jabStatus == PresenceType.chat) {
            if (hasVerbose) {
                return StatusType.ONLINE_WITH_DESCRIPTION;
            }
            else {
                return StatusType.ONLINE;
            }
        }
        else if (jabStatus == PresenceType.unavailable) {
            if (hasVerbose) {
                return StatusType.OFFLINE_WITH_DESCRIPTION;
            }
            else {
                return StatusType.OFFLINE;
            }
        }
        else {
            if (hasVerbose) {
                return StatusType.ONLINE_WITH_DESCRIPTION;
            }
            else {
                return StatusType.ONLINE;
            }
        }
    }

    /**
     * Converts a Gadu-Gadu status to an XMPP status.
     *
     * @param gadugaduStatus Gadu-Gadu StatusType constant.
     * @return XMPP presence type.
     */
    public PresenceType convertGaduGaduStatusToXMPP(StatusType gadugaduStatus) {
        if (gadugaduStatus.equals(StatusType.ONLINE) || gadugaduStatus.equals(StatusType.ONLINE_WITH_DESCRIPTION)) {
            return PresenceType.available;
        }
        else if (gadugaduStatus.equals(StatusType.BUSY) || gadugaduStatus.equals(StatusType.BUSY_WITH_DESCRIPTION)) {
            return PresenceType.away;
        }
        else if (gadugaduStatus.equals(StatusType.OFFLINE) || gadugaduStatus.equals(StatusType.OFFLINE_WITH_DESCRIPTION)) {
            return PresenceType.unavailable;
        }
        else {
            return PresenceType.unknown;
        }
    }

}
