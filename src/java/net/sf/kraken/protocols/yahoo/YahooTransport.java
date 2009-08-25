/**
 * $Revision$
 * $Date$
 *
 * Copyright 2008 Daniel Henninger.  All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.protocols.yahoo;

import net.sf.kraken.*;
import net.sf.kraken.registration.Registration;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.PresenceType;
import net.sf.kraken.type.TransportLoginStatus;

import org.apache.log4j.Logger;
import org.jivesoftware.util.LocaleUtils;
import org.xmpp.packet.JID;

import ymsg.network.StatusConstants;

/**
 * Yahoo Transport Interface.
 *
 * This handles the bulk of the XMPP work via BaseTransport and provides
 * some gateway specific interactions.
 *
 * @author Daniel Henninger
 */
public class YahooTransport extends BaseTransport {

    static Logger Log = Logger.getLogger(YahooTransport.class);

    /**
     * @see net.sf.kraken.BaseTransport#getTerminologyUsername()
     */
    public String getTerminologyUsername() {
        return LocaleUtils.getLocalizedString("gateway.yahoo.username", "kraken");
    }

    /**
     * @see net.sf.kraken.BaseTransport#getTerminologyPassword()
     */
    public String getTerminologyPassword() {
        return LocaleUtils.getLocalizedString("gateway.yahoo.password", "kraken");
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
        return LocaleUtils.getLocalizedString("gateway.yahoo.registration", "kraken");
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
        return username.matches("[^ \\p{Cntrl}()@,;:\\\\\"\\[\\]]+@?[^ \\p{Cntrl}()@,;:\\\\\"\\[\\]]+");
    }

    /**
     * Handles creating a Yahoo session and triggering a login.
     *
     * @param registration Registration information to be used to log in.
     * @param jid JID that is logged into the transport.
     * @param presenceType Type of presence.
     * @param verboseStatus Longer status description.
     */
    public TransportSession registrationLoggedIn(Registration registration, JID jid, PresenceType presenceType, String verboseStatus, Integer priority) {
        TransportSession session = new YahooSession(registration, jid, this, priority);
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
     * Converts an XMPP status to an Yahoo status.
     *
     * @param xmppStatus Jabber presence type.
     * @return Yahoo status identifier.
     */
    public long convertXMPPStatusToYahoo(PresenceType xmppStatus) {
        if (xmppStatus == PresenceType.available) {
            return StatusConstants.STATUS_AVAILABLE;
        }
        else if (xmppStatus == PresenceType.away) {
            return StatusConstants.STATUS_BRB;
        }
        else if (xmppStatus == PresenceType.xa) {
            return StatusConstants.STATUS_STEPPEDOUT;
        }
        else if (xmppStatus == PresenceType.dnd) {
            return StatusConstants.STATUS_BUSY;
        }
        else if (xmppStatus == PresenceType.chat) {
            return StatusConstants.STATUS_AVAILABLE;
        }
        else if (xmppStatus == PresenceType.unavailable) {
            return StatusConstants.STATUS_OFFLINE;
        }
        else {
            return StatusConstants.STATUS_AVAILABLE;
        }
    }

    /**
     * Converts a Yahoo status to an XMPP status.
     *
     * @param yahooStatus Yahoo StatusConstants constant.
     * @return XMPP presence type matching the Yahoo status.
     */
    public PresenceType convertYahooStatusToXMPP(long yahooStatus) {
        if (yahooStatus == StatusConstants.STATUS_AVAILABLE) {
            // We're good, leave the type as blank for available.
            return PresenceType.available;
        }
        else if (yahooStatus == StatusConstants.STATUS_BRB) {
            return PresenceType.away;
        }
        else if (yahooStatus == StatusConstants.STATUS_BUSY) {
            return PresenceType.dnd;
        }
        else if (yahooStatus == StatusConstants.STATUS_IDLE) {
            return PresenceType.away;
        }
        else if (yahooStatus == StatusConstants.STATUS_OFFLINE) {
            return PresenceType.unavailable;
        }
        else if (yahooStatus == StatusConstants.STATUS_NOTATDESK) {
            return PresenceType.away;
        }
        else if (yahooStatus == StatusConstants.STATUS_NOTINOFFICE) {
            return PresenceType.away;
        }
        else if (yahooStatus == StatusConstants.STATUS_ONPHONE) {
            return PresenceType.away;
        }
        else if (yahooStatus == StatusConstants.STATUS_ONVACATION) {
            return PresenceType.xa;
        }
        else if (yahooStatus == StatusConstants.STATUS_OUTTOLUNCH) {
            return PresenceType.xa;
        }
        else if (yahooStatus == StatusConstants.STATUS_STEPPEDOUT) {
            return PresenceType.away;
        }
        else if (yahooStatus == StatusConstants.STATUS_INVISIBLE) {
            return PresenceType.available;
        }
        else if (yahooStatus == StatusConstants.STATUS_CUSTOM) {
            return PresenceType.available;
        }
        else {
            // Not something we handle, we're going to ignore it.
            Log.warn("Yahoo: Unrecognized status "+yahooStatus+" received.");
            return PresenceType.unknown;
        }
    }

    static {
        KrakenPlugin.setLoggerProperty("log4j.additivity.org.openymsg", "false");
        KrakenPlugin.setLoggerProperty("log4j.logger.org.openymsg", "DEBUG, openfiredebug");
    }

}
