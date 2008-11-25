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

import net.sf.kraken.muc.MUCTransportSession;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.PresenceType;

import org.xmpp.packet.Presence;
import org.xmpp.packet.Message;
import org.dom4j.Element;
import org.apache.log4j.Logger;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import f00f.net.irc.martyr.services.AutoJoin;
import f00f.net.irc.martyr.commands.*;

/**
 * @author Daniel Henninger
 */

public class IRCMUCSession extends MUCTransportSession {

    static Logger Log = Logger.getLogger(IRCMUCSession.class);

    public IRCMUCSession(TransportSession session, String roomname, String nickname, IRCMUCTransport transport) {
        super(session, roomname, nickname, transport);
        ircSessionRef = new WeakReference<IRCSession>((IRCSession)session);
    }

    /* IRC session for conveniences. */
    WeakReference<IRCSession> ircSessionRef = null;

    AutoJoin autoJoin;

    public IRCSession getSession() {
        return ircSessionRef.get();
    }

    /* List of known contacts in this room. */
    public ArrayList<String> contacts = new ArrayList<String>();

    /**
     * Retrieve list of contacts from this chat room.
     *
     * @return List of contacts in this room.
     */
    public ArrayList<String> getContacts() {
        return contacts;
    }

    public void enterRoom() {
        autoJoin = new AutoJoin(getSession().getConnection(), roomname);
    }

    public void leaveRoom() {
        try {
            getSession().getConnection().sendCommand(new PartCommand(roomname));
        }
        catch (Exception e) {
            Log.debug("IRC: Error while trying to part chat room:", e);
        }
        try {
            autoJoin.disable();
        }
        catch (Exception e) {
            // Ignore
        }
        autoJoin = null;
        for (String contact : getContacts()) {
            Presence p = new Presence();
            p.setType(Presence.Type.unavailable);
            p.setTo(session.getJID());
            p.setFrom(transport.convertIDToJID(roomname, contact));
            Element elem = p.addChildElement("x", "http://jabber.org/protocol/muc#user");
            Element item = elem.addElement("item");
            item.addAttribute("affiliation", "none");
            item.addAttribute("role", "none");
            transport.sendPacket(p);
        }
        Presence p = new Presence();
        p.setType(Presence.Type.unavailable);
        p.setTo(session.getJID());
        p.setFrom(transport.convertIDToJID(roomname, nickname));
        Element elem = p.addChildElement("x", "http://jabber.org/protocol/muc#user");
        Element item = elem.addElement("item");
        item.addAttribute("affiliation", "none");
        item.addAttribute("role", "none");
        Element status = elem.addElement("status");
        status.addAttribute("code", "110");
        transport.sendPacket(p);
    }

    public void sendMessage(String message) {
        getSession().getConnection().sendCommand(new MessageCommand(roomname, message));
        // Return the favor.
        transport.sendMessage(session.getJID(), transport.convertIDToJID(roomname, nickname), message, Message.Type.groupchat);
    }

    public void sendPrivateMessage(String nickname, String message) {
        getSession().sendMessage(getSession().getTransport().convertIDToJID(nickname), message);
    }

    public void updateStatus(PresenceType presenceType) {
        // This should be taken care of by the main transport.
    }

    public void updateTopic(String topic) {
        getSession().getConnection().sendCommand(new TopicCommand(roomname, topic));
    }

    public void kickUser(String nickname, String reason) {
        getSession().getConnection().sendCommand(new KickCommand(roomname, nickname, reason));
    }

    public void grantVoice(String nickname) {
        // TODO: Map into IRC
    }

    public void revokeVoice(String nickname) {
        // TODO: Map into IRC
    }

    public void banUser(String nickname, String reason) {
        // TODO: Map into IRC
    }

    public void grantMembership(String nickname) {
        // TODO: Does this map into IRC?
    }

    public void revokeMembership(String nickname) {
        // TODO: Does this map into IRC?
    }

    public void grantModerator(String nickname) {
        // TODO: Map into IRC
    }

    public void revokeModerator(String nickname) {
        // TODO: Map into IRC
    }
    
}
