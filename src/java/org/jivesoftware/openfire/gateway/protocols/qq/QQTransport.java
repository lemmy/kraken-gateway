package org.jivesoftware.openfire.gateway.protocols.qq;

import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.openfire.gateway.*;
import org.jivesoftware.openfire.gateway.type.PresenceType;
import org.jivesoftware.openfire.gateway.type.TransportLoginStatus;
import org.jivesoftware.openfire.gateway.registration.Registration;
import org.jivesoftware.openfire.gateway.session.TransportSession;
import org.xmpp.packet.JID;
import org.xmpp.packet.Presence;
import edu.tsinghua.lumaqq.qq.QQ;


/**
 * QQ Transport Interface.
 *
 * This handles the bulk of the QQ work via BaseTransport and provides
 * some gateway specific interactions.
 *
 * @author lizongbo
 */
public class QQTransport extends BaseTransport {
    /**
     * @see org.jivesoftware.openfire.gateway.BaseTransport#getTerminologyUsername()
     */
    public String getTerminologyUsername() {
        return LocaleUtils.getLocalizedString("gateway.qq.username", "gateway");
    }

    /**
     * @see org.jivesoftware.openfire.gateway.BaseTransport#getTerminologyPassword()
     */
    public String getTerminologyPassword() {
        return LocaleUtils.getLocalizedString("gateway.qq.password", "gateway");
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
        return LocaleUtils.getLocalizedString("gateway.qq.registration",
                                              "gateway");
    }

    /**
     * @see org.jivesoftware.openfire.gateway.BaseTransport#isPasswordRequired()
     */
    public Boolean isPasswordRequired() {
        return true;
    }

    /**
     * @see org.jivesoftware.openfire.gateway.BaseTransport#isNicknameRequired()
     */
    public Boolean isNicknameRequired() {
        return false;
    }

    /**
     * @see org.jivesoftware.openfire.gateway.BaseTransport#isUsernameValid(String)
     */
    public Boolean isUsernameValid(String username) {
        try {
            Integer.parseInt(username);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Handles creating a QQ session and triggering a login.
     *
     * @param registration Registration information to be used to log in.
     * @param jid JID that is logged into the transport.
     * @param presenceType Type of presence.
     * @param verboseStatus Longer status description.
     */
    public TransportSession registrationLoggedIn(Registration registration,
                                                 JID jid,
                                                 PresenceType presenceType,
                                                 String verboseStatus,
                                                 Integer priority) {
        TransportSession session = new QQSession(registration, jid, this,
                                                 priority);
        session.setLoginStatus(TransportLoginStatus.LOGGING_IN);
        session.logIn(presenceType, verboseStatus);
        return session;
    }

    /**
     * Handles logging out of a QQ session.
     *
     * @param session The session to be disconnected.
     */
    public void registrationLoggedOut(TransportSession session) {
        session.setLoginStatus(TransportLoginStatus.LOGGING_OUT);
        session.logOut();
    }

    /**
     * Converts a jabber status to an QQ status.
     *
     * @param jabStatus Jabber presence type.
     * @return QQ user status id.
     */
    public byte convertJabStatusToQQ(PresenceType jabStatus) {
        if (jabStatus == PresenceType.available) {
            return QQ.QQ_STATUS_ONLINE;
        } else if (jabStatus == PresenceType.away) {
            return QQ.QQ_STATUS_AWAY;
        } else if (jabStatus == PresenceType.xa) {
            return QQ.QQ_STATUS_AWAY;
        } else if (jabStatus == PresenceType.dnd) {
            return QQ.QQ_STATUS_AWAY;
        } else if (jabStatus == PresenceType.chat) {
            return QQ.QQ_STATUS_ONLINE;
        } else if (jabStatus == PresenceType.unavailable) {
            return QQ.QQ_STATUS_OFFLINE;
        } else {
            return QQ.QQ_STATUS_ONLINE;
        }
    }

    /**
     * Sets up a presence packet according to QQ status.
     *
     * @param qqStatus QQ ContactStatus constant.
     * @param packet Presence packet to set up.
     */
    public void setUpPresencePacket(Presence packet, byte qqStatus) {
        switch (qqStatus) {
        case QQ.QQ_STATUS_AWAY:
            packet.setShow(Presence.Show.away);
            break;
        case QQ.QQ_STATUS_HIDDEN:
            packet.setShow(Presence.Show.xa);
            break;
        case QQ.QQ_STATUS_OFFLINE:
            packet.setType(Presence.Type.unavailable);
            break;
        case QQ.QQ_STATUS_ONLINE:
            packet.setShow(Presence.Show.chat);
            break;
        default:
            packet.setShow(Presence.Show.chat);
            break;
        }
    }

}
