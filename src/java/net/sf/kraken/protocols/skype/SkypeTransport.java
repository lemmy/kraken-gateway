/*******************************************************************************
 * Copyright (c) 2011 Markus Alexander Kuppe.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Markus Alexander Kuppe (ecf-dev_eclipse.org <at> lemmster <dot> de) - initial API and implementation
 ******************************************************************************/
package net.sf.kraken.protocols.skype;

import net.sf.kraken.BaseTransport;
import net.sf.kraken.registration.Registration;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.PresenceType;
import net.sf.kraken.type.TransportLoginStatus;

import org.xmpp.packet.JID;

import com.skype.Profile.Status;

public class SkypeTransport extends BaseTransport<SkypeBuddy> {

	/* (non-Javadoc)
	 * @see net.sf.kraken.BaseTransport#registrationLoggedIn(net.sf.kraken.registration.Registration, org.xmpp.packet.JID, net.sf.kraken.type.PresenceType, java.lang.String, java.lang.Integer)
	 */
	@Override
	public TransportSession<SkypeBuddy> registrationLoggedIn(
			Registration registration, JID jid, PresenceType presenceType,
			String verboseStatus, Integer priority) {

		return new SkypeSession(registration, jid, this, priority, presenceType, verboseStatus);
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.BaseTransport#registrationLoggedOut(net.sf.kraken.session.TransportSession)
	 */
	@Override
	public void registrationLoggedOut(TransportSession<SkypeBuddy> session) {
        session.setLoginStatus(TransportLoginStatus.LOGGING_OUT);
        session.logOut();
        session.setLoginStatus(TransportLoginStatus.LOGGED_OUT);
	}
	
	/* (non-Javadoc)
	 * @see net.sf.kraken.BaseTransport#getTerminologyUsername()
	 */
	@Override
	public String getTerminologyUsername() {
		return "getTerminologyUsername";
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.BaseTransport#getTerminologyPassword()
	 */
	@Override
	public String getTerminologyPassword() {
		return "getTerminologyPassword";
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.BaseTransport#getTerminologyNickname()
	 */
	@Override
	public String getTerminologyNickname() {
		return "getTerminologyNickName";
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.BaseTransport#getTerminologyRegistration()
	 */
	@Override
	public String getTerminologyRegistration() {
		return "getTerminologyRegistration";
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.BaseTransport#isPasswordRequired()
	 */
	@Override
	public Boolean isPasswordRequired() {
		return true;
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.BaseTransport#isNicknameRequired()
	 */
	@Override
	public Boolean isNicknameRequired() {
		return false;
	}

	/* (non-Javadoc)
	 * @see net.sf.kraken.BaseTransport#isUsernameValid(java.lang.String)
	 */
	@Override
	public Boolean isUsernameValid(String username) {
		return true;
	}

	/**
     * Converts a jabber status to a Skype status.
     *
     * @param jabStatus Jabber presence type.
     * @return Skype user status id.
     */
    public Status convertJabStatusToSkype(PresenceType jabStatus) {
        if (jabStatus == PresenceType.available) {
            return Status.ONLINE;
        } else if (jabStatus == PresenceType.away) {
            return Status.AWAY;
        } else if (jabStatus == PresenceType.xa) {
            return Status.INVISIBLE;
        } else if (jabStatus == PresenceType.dnd) {
            return Status.DND;
        } else if (jabStatus == PresenceType.chat) {
            return Status.SKYPEME;
        } else if (jabStatus == PresenceType.unavailable) {
            return Status.OFFLINE;
        } else {
            return Status.UNKNOWN;
        }
    }

    /**
     * Converts a Skype status to an XMPP status.
     *
     * @param skypeStatus Skype status constant.
     * @return XMPP presence type matching the Skype status.
     */
    public PresenceType convertSkypeStatusToXMPP(Status skypeStatus) {
        switch (skypeStatus) {
	        case AWAY:
	            return PresenceType.away;
	        case INVISIBLE:
	            return PresenceType.xa;
	        case OFFLINE:
	            return PresenceType.unavailable;
	        case ONLINE:
	            return PresenceType.available;
	        case SKYPEME:
	        	return PresenceType.chat;
	        case DND:
	        	return PresenceType.dnd;
	        default:
	            return PresenceType.unknown;
        }
    }
}
