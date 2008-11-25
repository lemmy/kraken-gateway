/**
 * $Revision$
 * $Date$
 *
 * Copyright 2008 Daniel Henninger.  All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.protocols.irc;

import net.sf.kraken.*;
import net.sf.kraken.registration.Registration;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.PresenceType;
import net.sf.kraken.type.TransportLoginStatus;

import org.jivesoftware.util.LocaleUtils;
import org.xmpp.packet.JID;

/***
 * IRC Transport Interface.
 *
 * This handles the bulk of the XMPP work via BaseTransport and provides
 * some gateway specific interactions.
 *
 * @author Daniel Henninger
 */
public class IRCTransport extends BaseTransport {

    /**
     * @see net.sf.kraken.BaseTransport#getTerminologyUsername()
     */
    public String getTerminologyUsername() {
        return LocaleUtils.getLocalizedString("gateway.irc.username", "kraken");
    }

    /**
     * @see net.sf.kraken.BaseTransport#getTerminologyPassword()
     */
    public String getTerminologyPassword() {
        return LocaleUtils.getLocalizedString("gateway.irc.password", "kraken");
    }

    /**
     * @see net.sf.kraken.BaseTransport#getTerminologyNickname()
     */
    public String getTerminologyNickname() {
        return LocaleUtils.getLocalizedString("gateway.irc.nickname", "kraken");
    }

    /**
     * @see net.sf.kraken.BaseTransport#getTerminologyRegistration()
     */
    public String getTerminologyRegistration() {
        return LocaleUtils.getLocalizedString("gateway.irc.registration", "kraken");
    }

    /**
     * @see net.sf.kraken.BaseTransport#isPasswordRequired()
     */
    public Boolean isPasswordRequired() { return false; }

    /**
     * @see net.sf.kraken.BaseTransport#isNicknameRequired()
     */
    public Boolean isNicknameRequired() { return true; }

    /**
     * @see net.sf.kraken.BaseTransport#isUsernameValid(String)
     */
    public Boolean isUsernameValid(String username) {
        return username.matches("\\w+");
    }

    /**
     * Handles creating a IRC session and triggering a login.
     *
     * @param registration Registration information to be used to log in.
     * @param jid JID that is logged into the transport.
     * @param presenceType Type of presence.
     * @param verboseStatus Longer status description.
     */
    public TransportSession registrationLoggedIn(Registration registration, JID jid, PresenceType presenceType, String verboseStatus, Integer priority) {
        TransportSession session = new IRCSession(registration, jid, this, priority);
        session.setLoginStatus(TransportLoginStatus.LOGGING_IN);
        session.logIn(presenceType, verboseStatus);
        return session;
    }

    /**
     * Handles logging out of a IRC session.
     *
     * @param session The session to be disconnected.
     */
    public void registrationLoggedOut(TransportSession session) {
        session.setLoginStatus(TransportLoginStatus.LOGGING_OUT);
        session.logOut();
    }

    /**
     * Converts a jabber status to an IRC away message (or not).
     *
     * @param jabStatus Jabber presence type.
     * @param verboseStatus Verbose status information.
     * @return IRC status string.
     */
    public String convertJabStatusToIRC(PresenceType jabStatus, String verboseStatus) {
        if (jabStatus == PresenceType.available) {
            return null;
        }
        else if (jabStatus == PresenceType.away) {
            return (verboseStatus == null || verboseStatus.equals("")) ? LocaleUtils.getLocalizedString("gateway.irc.away", "kraken") : LocaleUtils.getLocalizedString("gateway.irc.away", "kraken")+": "+verboseStatus;
        }
        else if (jabStatus == PresenceType.xa) {
            return (verboseStatus == null || verboseStatus.equals("")) ? LocaleUtils.getLocalizedString("gateway.irc.extendedaway", "kraken") : LocaleUtils.getLocalizedString("gateway.irc.extendedaway", "kraken")+": "+verboseStatus;
        }
        else if (jabStatus == PresenceType.dnd) {
            return (verboseStatus == null || verboseStatus.equals("")) ? LocaleUtils.getLocalizedString("gateway.irc.donotdisturb", "kraken") : LocaleUtils.getLocalizedString("gateway.irc.donotdisturb", "kraken")+": "+verboseStatus;
        }
        else if (jabStatus == PresenceType.chat) {
            return null;
        }
        else if (jabStatus == PresenceType.unavailable) {
            // This should never show up.
            return null;
        }
        else {
            return null;
        }
    }

    public void start() {
        super.start();
        mucTransport = new IRCMUCTransport(this);
        try {
            componentManager.addComponent("conference.irc", mucTransport);
        }
        catch (Exception e) {
            componentManager.getLog().error(e);
        }
    }

    public void shutdown() {
        mucTransport.shutdown();
        try {
            componentManager.removeComponent("conference.irc");
        }
        catch (Exception e) {
            componentManager.getLog().error(e);
        }
        super.shutdown();
    }

    static {
        GatewayPlugin.setLoggerProperty("log4j.additivity.f00f.net.irc.martyr", "false");
        GatewayPlugin.setLoggerProperty("log4j.logger.f00f.net.irc.martyr", "DEBUG, openfiredebug");
    }

}
