/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */
package org.jivesoftware.openfire.gateway.protocols.xmpp.packet;

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;

/**
 * @author Daniel Henninger
 */
public class ProbePacket extends Packet {
    
    /**
     * Creates a new presence probe packet.
     *
     * @param from JID of presence requestor.
     * @param to JID to request presence of.
     */
    public ProbePacket(String from, String to) {
        setTo(to);
        setFrom(from);
    }

    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<presence");
        if (getTo() != null) {
            buf.append(" to=\"").append(StringUtils.escapeForXML(getTo())).append("\"");
        }
        if (getFrom() != null) {
            buf.append(" from=\"").append(StringUtils.escapeForXML(getFrom())).append("\"");
        }
        buf.append(" type=\"probe\"");
        buf.append("/>");
        
        return buf.toString();
    }

}
