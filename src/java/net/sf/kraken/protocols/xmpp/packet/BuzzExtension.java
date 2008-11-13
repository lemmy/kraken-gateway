/**
 * $RCSfile$
 * $Revision: 3144 $
 * $Date: 2005-12-01 14:20:11 -0300 (Thu, 01 Dec 2005) $
 *
 * Copyright (C) 2004-2008 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution, or a commercial license
 * agreement with Jive.
 */

package net.sf.kraken.protocols.xmpp.packet;

import org.jivesoftware.smack.packet.PacketExtension;

/**
 * Borrowed directly from Spark.
 */
public class BuzzExtension implements PacketExtension {
    
    public String getElementName() {
        return "buzz";
    }

    public String getNamespace() {
        return "http://www.jivesoftware.com/spark";
    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append("\"/>");
        return buf.toString();
    }

}
