/*
 * Licensed to the ProTel Communications Ltd. (PT) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The PT licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sf.kraken.protocols.simple;

import javax.sip.ListeningPoint;
import javax.sip.SipFactory;

import net.sf.kraken.BaseTransport;
import net.sf.kraken.registration.Registration;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.PresenceType;
import net.sf.kraken.type.TransportLoginStatus;

import org.jivesoftware.util.LocaleUtils;
import org.xmpp.packet.JID;

/**
 * A transport implementation for SIMPLE protocol.
 * @author Patrick Siu
 * @author Daniel Henninger
 */
public class SimpleTransport extends BaseTransport {
	SipFactory sipFactory = null;
	
	public SimpleTransport() {
		super();
		
		// Initialize the SipFactory
		sipFactory = SipFactory.getInstance();
		sipFactory.setPathName("gov.nist");
	}
	
	public TransportSession registrationLoggedIn(Registration registration, JID jid, PresenceType presenceType, String verboseStatus, Integer priority) {
		TransportSession session = new SimpleSession(registration, jid, this, priority); 
		
		// Possibly more work here!
		session.logIn(presenceType, verboseStatus);
		
		return session;
	}

	public void registrationLoggedOut(TransportSession session) {
		session.logOut();
		
		((SimpleSession) session).removeStack();
		session.sessionDone();
		
        // Just in case.
		session.setLoginStatus(TransportLoginStatus.LOGGED_OUT);
	}
	
	/**
	 */
	public String getTerminologyUsername() {
		return LocaleUtils.getLocalizedString("gateway.simple.username", "gateway");
	}

	public String getTerminologyPassword() {
		return LocaleUtils.getLocalizedString("gateway.simple.password", "gateway");
	}
	
	/**
     * @see net.sf.kraken.BaseTransport#getTerminologyNickname()
     */
	public String getTerminologyNickname() {
		// If this string is needed, then take it.  Just put a draft code to ensure integrity.
		
		return null;
	}
	
	/**
     * @see net.sf.kraken.BaseTransport#getTerminologyRegistration()
     */
	public String getTerminologyRegistration() {
		return LocaleUtils.getLocalizedString("gateway.simple.registration", "gateway");
	}
	
	/**
     * @see net.sf.kraken.BaseTransport#isPasswordRequired()
     */
	public Boolean isPasswordRequired() {
		// Just put a draft code to ensure integrity.
		
		return true;
	}
	
	/**
     * @see net.sf.kraken.BaseTransport#isNicknameRequired()
     */
	public Boolean isNicknameRequired() {
		// Just put a draft code to ensure integrity.
		
		return false;
	}
	
	/**
     * @see net.sf.kraken.BaseTransport#isUsernameValid(String)
     */
	public Boolean isUsernameValid(String username) {
		// Just put a draft code to ensure integrity.
//		Log.debug("SimpleTransport.isUsernameValid:  Checking '" + username + "'");
//		Boolean result = username.matches("\\w+");
//		return username.matches("[^ \\p{Cntrl}()@,;:\\\\\"\\[\\]]+@[^ \\p{Cntrl}()@,;:\\\\\"\\[\\]]+");
        return username.matches("\\w+");
    }
	
	
	// The following code are generic custom classes for SIP-XMPP conversion.
	public SimplePresence convertXMPPStatusToSIP(PresenceType xmppStatus) {
		SimplePresence simplePresence = new SimplePresence();
		
		switch (xmppStatus) {
			case available:
//				simplePresence.setRpid(SimplePresence.Rpid.UNKNOWN);
				simplePresence.setDmNote("Online");
				break;
			case away:
				simplePresence.setRpid(SimplePresence.Rpid.AWAY);
				break;
			case chat:
				simplePresence.setRpid(SimplePresence.Rpid.OTHER);
				simplePresence.setDmNote("Free to chat");
				break;
			case dnd:
				simplePresence.setRpid(SimplePresence.Rpid.BUSY);
				break;
			case unavailable:
				simplePresence.setTupleStatus(SimplePresence.TupleStatus.CLOSED);
				break;
			case unknown:
				simplePresence.setRpid(SimplePresence.Rpid.UNKNOWN);
				break;
			case xa:
				simplePresence.setRpid(SimplePresence.Rpid.AWAY);
				break;
			default:
				break;
		}
		
		return simplePresence;
	}
	
	public PresenceType convertSIPStatusToXMPP(SimplePresence simplePresence) {
		if (simplePresence.getTupleStatus().isOpen()) {
			switch (simplePresence.getRpid()) {
				case APPOINTMENT:
					return PresenceType.dnd;
				case AWAY:
					return PresenceType.away;
				case BREAKFAST:
					return PresenceType.xa;
				case BUSY:
					return PresenceType.dnd;
				case DINNER:
					return PresenceType.xa;
				case HOLIDAY:
					return PresenceType.xa;
				case IN_TRANSIT:
					return PresenceType.xa;
				case LOOKING_FOR_WORK:
					return PresenceType.dnd;
				case LUNCH:
				case MEAL:
					return PresenceType.xa;
				case MEETING:
					return PresenceType.dnd;
				case ON_THE_PHONE:
					return PresenceType.away;
				case OTHER:
                    return PresenceType.available;
                case PERFORMANCE:
					return PresenceType.dnd;
				case PERMANENT_ABSENCE:
					return PresenceType.unavailable;
				case PLAYING:
					return PresenceType.away;
				case PRESENTATION:
					return PresenceType.dnd;
				case SHOPPING:
					return PresenceType.xa;
				case SLEEPING:
					return PresenceType.xa;
				case SPECTATOR:
					return PresenceType.xa;
				case STEERING:
					return PresenceType.xa;
				case TRAVEL:
					return PresenceType.xa;
				case TV:
					return PresenceType.away;
				case UNKNOWN:
                    return PresenceType.unknown;
                case VACATION:
					return PresenceType.xa;
				case WORKING:
					return PresenceType.dnd;
				case WORSHIP:
					return PresenceType.dnd;
				default:
					return PresenceType.available;
			}
		}
		else {
			return PresenceType.unavailable;
		}
	}
	
	
	/**
	 * An improved method to do the trick.
	 */
	public String convertJIDToID(JID jid) {
		String node = jid.getNode();
		while (!JID.unescapeNode(node).equals(node)) {
			node = JID.unescapeNode(node);
		}
		return node;
	}
	
	
	int portOffset = 0;
	synchronized int generateListenerPort() {
		return (ListeningPoint.PORT_5060 + (++portOffset));
	}
}
