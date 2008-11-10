/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2006-2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.openfire.gateway.protocols.oscar;

import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.openfire.gateway.*;
import org.jivesoftware.openfire.gateway.type.TransportLoginStatus;
import org.jivesoftware.openfire.gateway.type.TransportType;
import org.jivesoftware.openfire.gateway.type.PresenceType;
import org.jivesoftware.openfire.gateway.registration.Registration;
import org.jivesoftware.openfire.gateway.session.TransportSession;
import org.xmpp.packet.JID;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.conn.SetExtraInfoCmd;

/**
 * OSCAR Transport Interface.
 *
 * This handles the bulk of the XMPP work via BaseTransport and provides
 * some gateway specific interactions.
 *
 * @author Daniel Henninger
 */
public class OSCARTransport extends BaseTransport {

    /**
     * @see org.jivesoftware.openfire.gateway.BaseTransport#getTerminologyUsername()
     */
    public String getTerminologyUsername() {
        return LocaleUtils.getLocalizedString("gateway."+getType().toString()+".username", "gateway");
    }

    /**
     * @see org.jivesoftware.openfire.gateway.BaseTransport#getTerminologyPassword()
     */
    public String getTerminologyPassword() {
        return LocaleUtils.getLocalizedString("gateway."+getType().toString()+".password", "gateway");
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
        return LocaleUtils.getLocalizedString("gateway."+getType().toString()+".registration", "gateway");
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
        if (getType() == TransportType.icq) {
            return username.matches("\\d+");
        }
        else {
            return username.matches("\\w+") || username.matches("\\w+@[\\w\\.]+");
        }
    }

    /**
     * Handles creating an OSCAR session and triggering a login.
     *
     * @param registration Registration information to be used to log in.
     * @param jid JID that is logged into the transport.
     * @param presenceType Type of presence.
     * @param verboseStatus Longer status description.
     */
    public TransportSession registrationLoggedIn(Registration registration, JID jid, PresenceType presenceType, String verboseStatus, Integer priority) {
        TransportSession session = new OSCARSession(registration, jid, this, priority);
        session.setLoginStatus(TransportLoginStatus.LOGGING_IN);
        session.logIn(presenceType, verboseStatus);
        return session;
    }

    /**
     * Handles logging out of a Yahoo session.
     *
     * @param session The session to be disconnected.
     */
    public void registrationLoggedOut(TransportSession session) {
        session.setLoginStatus(TransportLoginStatus.LOGGING_OUT);
        session.logOut();
    }

    /**
     * Converts an XMPP status to an ICQ status.
     *
     * @param xmppStatus XMPP presence type.
     * @return ICQ user status.
     */
    public long convertXMPPStatusToICQ(PresenceType xmppStatus) {
        if (xmppStatus == PresenceType.available) {
            return FullUserInfo.ICQSTATUS_DEFAULT;
        }
        else if (xmppStatus == PresenceType.away) {
            return FullUserInfo.ICQSTATUS_AWAY;
        }
        else if (xmppStatus == PresenceType.xa) {
            return FullUserInfo.ICQSTATUS_NA;
        }
        else if (xmppStatus == PresenceType.dnd) {
            return FullUserInfo.ICQSTATUS_DND;
        }
        else if (xmppStatus == PresenceType.chat) {
            return FullUserInfo.ICQSTATUS_FFC;
        }
        else if (xmppStatus == PresenceType.unavailable) {
            return SetExtraInfoCmd.ICQSTATUS_NONE;
        }
        else {
            return FullUserInfo.ICQSTATUS_DEFAULT;
        }
    }

    /**
     * Converts an ICQ status to an XMPP status.
     *
     * @param icqStatus ICQ status constant.
     * @return XMPP status.
     */
    public PresenceType convertICQStatusToXMPP(long icqStatus) {
        if (icqStatus == FullUserInfo.ICQSTATUS_DEFAULT) {
            return PresenceType.available;
        }
        else if (icqStatus == FullUserInfo.ICQSTATUS_AWAY) {
            return PresenceType.away;
        }
        else if (icqStatus == FullUserInfo.ICQSTATUS_DND) {
            return PresenceType.dnd;
        }
        else if (icqStatus == FullUserInfo.ICQSTATUS_FFC) {
            return PresenceType.chat;
        }
        else if (icqStatus == FullUserInfo.ICQSTATUS_INVISIBLE) {
            return PresenceType.available;
        }
        else if (icqStatus == FullUserInfo.ICQSTATUS_NA) {
            return PresenceType.xa;
        }
        else if (icqStatus == FullUserInfo.ICQSTATUS_OCCUPIED) {
            return PresenceType.dnd;
        }
        else {
            return PresenceType.unknown;
        }
    }

}
