/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2006-2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.protocols.msn;

import org.jivesoftware.util.LocaleUtils;
import org.xmpp.packet.JID;
import net.sf.jml.MsnUserStatus;
import net.sf.kraken.*;
import net.sf.kraken.registration.Registration;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.PresenceType;
import net.sf.kraken.type.TransportLoginStatus;

/**
 * MSN Transport Interface.
 *
 * This handles the bulk of the XMPP work via BaseTransport and provides
 * some gateway specific interactions.
 *
 * @author Daniel Henninger
 */
public class MSNTransport extends BaseTransport {

    /**
     * @see net.sf.kraken.BaseTransport#getTerminologyUsername()
     */
    public String getTerminologyUsername() {
        return LocaleUtils.getLocalizedString("gateway.msn.username", "gateway");
    }

    /**
     * @see net.sf.kraken.BaseTransport#getTerminologyPassword()
     */
    public String getTerminologyPassword() {
        return LocaleUtils.getLocalizedString("gateway.msn.password", "gateway");
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
        return LocaleUtils.getLocalizedString("gateway.msn.registration", "gateway");
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
        return username.matches("[^ \\p{Cntrl}()@,;:\\\\\"\\[\\]]+@[^ \\p{Cntrl}()@,;:\\\\\"\\[\\]]+");
    }

    /**
     * Handles creating a MSN session and triggering a login.
     *
     * @param registration Registration information to be used to log in.
     * @param jid JID that is logged into the transport.
     * @param presenceType Type of presence.
     * @param verboseStatus Longer status description.
     */
    public TransportSession registrationLoggedIn(Registration registration, JID jid, PresenceType presenceType, String verboseStatus, Integer priority) {
        TransportSession session = new MSNSession(registration, jid, this, priority);
        session.setLoginStatus(TransportLoginStatus.LOGGING_IN);
        session.logIn(presenceType, verboseStatus);
        return session;
    }

    /**
     * Handles logging out of a MSN session.
     *
     * @param session The session to be disconnected.
     */
    public void registrationLoggedOut(TransportSession session) {
        session.setLoginStatus(TransportLoginStatus.LOGGING_OUT);
        session.logOut();
    }

    /**
     * Converts a jabber status to an MSN status.
     *
     * @param jabStatus Jabber presence type.
     * @return MSN user status id.
     */
    public MsnUserStatus convertXMPPStatusToMSN(PresenceType jabStatus) {
        if (jabStatus == PresenceType.available) {
            return MsnUserStatus.ONLINE;
        }
        else if (jabStatus == PresenceType.away) {
            return MsnUserStatus.AWAY;
        }
        else if (jabStatus == PresenceType.xa) {
            return MsnUserStatus.AWAY;
        }
        else if (jabStatus == PresenceType.dnd) {
            return MsnUserStatus.BUSY;
        }
        else if (jabStatus == PresenceType.chat) {
            return MsnUserStatus.ONLINE;
        }
        else if (jabStatus == PresenceType.unavailable) {
            return MsnUserStatus.OFFLINE;
        }
        else {
            return MsnUserStatus.ONLINE;
        }
    }

    /**
     * Converts an MSN status to an XMPP status.
     *
     * @param msnStatus MSN ContactStatus constant.
     * @return XMPP presence type.
     */
    public PresenceType convertMSNStatusToXMPP(MsnUserStatus msnStatus) {
        if (msnStatus.equals(MsnUserStatus.ONLINE)) {
            return PresenceType.available;
        }
        else if (msnStatus.equals(MsnUserStatus.AWAY)) {
            return PresenceType.away;
        }
        else if (msnStatus.equals(MsnUserStatus.BE_RIGHT_BACK)) {
            return PresenceType.away;
        }
        else if (msnStatus.equals(MsnUserStatus.BUSY)) {
            return PresenceType.dnd;
        }
        else if (msnStatus.equals(MsnUserStatus.IDLE)) {
            return PresenceType.away;
        }
        else if (msnStatus.equals(MsnUserStatus.OFFLINE)) {
            return PresenceType.unavailable;
        }
        else if (msnStatus.equals(MsnUserStatus.ON_THE_PHONE)) {
            return PresenceType.dnd;
        }
        else if (msnStatus.equals(MsnUserStatus.OUT_TO_LUNCH)) {
            return PresenceType.xa;
        }
        else {
            return PresenceType.unknown;
        }
    }

}
