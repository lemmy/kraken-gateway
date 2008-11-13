/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.muc;

import org.xmpp.packet.JID;

/**
 * @author Daniel Henninger
 */
public class MUCTransportRoomMember {

    public MUCTransportRoomMember(JID memberjid) {
        this.jid = memberjid;
    }

    /* JID of the room member */
    public JID jid;

    public JID getJid() {
        return jid;
    }

    public void setJid(JID jid) {
        this.jid = jid;
    }
    
}
