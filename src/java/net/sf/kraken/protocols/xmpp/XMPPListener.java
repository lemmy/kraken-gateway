package net.sf.kraken.protocols.xmpp;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Date;

import net.sf.kraken.protocols.xmpp.packet.GoogleMailBoxPacket;
import net.sf.kraken.protocols.xmpp.packet.GoogleMailNotifyExtension;
import net.sf.kraken.protocols.xmpp.packet.GoogleMailSender;
import net.sf.kraken.protocols.xmpp.packet.GoogleMailThread;
import net.sf.kraken.protocols.xmpp.packet.GoogleNewMailExtension;
import net.sf.kraken.protocols.xmpp.packet.IQWithPacketExtension;
import net.sf.kraken.type.NameSpace;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.LocaleUtils;
import org.xmpp.packet.Message;

/**
 * Handles incoming events from XMPP server.
 *
 * @author Daniel Henninger
 * @author Mehmet Ecevit
 */
public class XMPPListener implements MessageListener, ConnectionListener, ChatManagerListener, PacketListener {

    static Logger Log = Logger.getLogger(XMPPListener.class);

    /**
     * Creates an XMPP listener instance and ties to session.
     *
     * @param session Session this listener is associated with.
     */
    public XMPPListener(XMPPSession session) {
        this.xmppSessionRef = new WeakReference<XMPPSession>(session);
    }
    
    /**
     * Session instance that the listener is associated with.
     */
    public WeakReference<XMPPSession> xmppSessionRef = null;

    /**
     * Last google mail thread id we saw.
     */
    public Long lastGMailThreadId = null;

    /**
     * Last google mail thread date we saw.
     */
    public Date lastGMailThreadDate = null;

    /**
     * Returns the XMPP session this listener is attached to.
     *
     * @return XMPP session we are attached to.
     */
    public XMPPSession getSession() {
        return xmppSessionRef.get();
    }

    /**
     * Handles incoming messages.
     *
     * @param chat Chat instance this message is associated with.
     * @param message Message received.
     */
    public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {
        Log.debug("Received XMPP/GTalk message: "+message.toXML());
        try {
            PacketExtension pe = message.getExtension("x", NameSpace.X_DELAY);
            if (pe != null && pe instanceof DelayInformation) {
                DelayInformation di = (DelayInformation)pe;
                getSession().getTransport().sendOfflineMessage(
                        getSession().getJID(),
                        getSession().getTransport().convertIDToJID(message.getFrom()),
                        message.getBody(),
                        di.getStamp(),
                        di.getReason()
                );
            }
            else {
                getSession().getTransport().sendMessage(
                        getSession().getJID(),
                        getSession().getTransport().convertIDToJID(message.getFrom()),
                        message.getBody()
                );
            }
//            if (message.getProperty("time") == null || message.getProperty("time").equals("")) {
//            }
//            else {
//                getSession().getTransport().sendOfflineMessage(
//                        getSession().getJID(),
//                        getSession().getTransport().convertIDToJID(message.getFrom()),
//                        message.getBody(),
//                        Message.Type.chat,
//                        message.getProperty("time").toString()
//                );
//            }
            
        }
        catch (Exception ex) {
            Log.debug("E001:"+ ex.getMessage(), ex);
        }
        
    }

    public void connectionClosed() {
        getSession().sessionDisconnectedNoReconnect(null);
    }

    public void connectionClosedOnError(Exception exception) {
        getSession().sessionDisconnected(LocaleUtils.getLocalizedString("gateway.xmpp.connectionclosed", "kraken"));
    }

    public void reconnectingIn(int i) {
        //Ignoring for now
    }

    public void reconnectionSuccessful() {
        //Ignoring for now
    }

    public void reconnectionFailed(Exception exception) {
        //Ignoring for now
    }

    public void chatCreated(Chat chat, boolean b) {
        chat.addMessageListener(this);
    }

    public void processPacket(Packet packet) {
        if (packet instanceof GoogleMailBoxPacket) {
            if (JiveGlobals.getBooleanProperty("plugin.gateway.gtalk.mailnotifications", true)) {
                GoogleMailBoxPacket mbp = (GoogleMailBoxPacket)packet;
                this.setLastGMailThreadDate(mbp.getResultTime());
                Integer newMailCount = 0;
                String mailList = "";
                for (GoogleMailThread mail : mbp.getMailThreads()) {
                    newMailCount++;
                    if (this.getLastGMailThreadId() == null || mail.getThreadId() > this.getLastGMailThreadId()) {
                        this.setLastGMailThreadId(mail.getThreadId());
                    }
                    String senderList = "";
                    for (GoogleMailSender sender : mail.getSenders()) {
                        if (!senderList.equals("")) {
                            senderList += ", ";
                        }
                        String name = sender.getName();
                        if (name != null) {
                            senderList += name + " <";
                        }
                        senderList += sender.getAddress();
                        if (name != null) {
                            senderList += ">";
                        }
                    }
                    mailList += "\n   "+senderList+" sent "+mail.getSubject();
                }
                if (newMailCount > 0) {
                    getSession().getTransport().sendMessage(
                            getSession().getJID(),
                            getSession().getTransport().getJID(),
                            LocaleUtils.getLocalizedString("gateway.gtalk.mail", "kraken", Arrays.asList(newMailCount))+mailList,
                            Message.Type.headline
                    );
                }
            }
        }
        else if (packet instanceof IQ) {
            Log.debug("XMPP: Got google mail notification");
            GoogleNewMailExtension gnme = (GoogleNewMailExtension)packet.getExtension(GoogleNewMailExtension.ELEMENT_NAME, GoogleNewMailExtension.NAMESPACE);
            if (gnme != null) {
                Log.debug("XMPP: Sending google mail request");
                getSession().conn.sendPacket(new IQWithPacketExtension(new GoogleMailNotifyExtension()));
            }
        }
    }

    public Long getLastGMailThreadId() {
        return lastGMailThreadId;
    }

    public void setLastGMailThreadId(Long lastGMailThreadId) {
        this.lastGMailThreadId = lastGMailThreadId;
    }

    public Date getLastGMailThreadDate() {
        return lastGMailThreadDate;
    }

    public void setLastGMailThreadDate(Date lastGMailThreadDate) {
        this.lastGMailThreadDate = lastGMailThreadDate;
    }

}
