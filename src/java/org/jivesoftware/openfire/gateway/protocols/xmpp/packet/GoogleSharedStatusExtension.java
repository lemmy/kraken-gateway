/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.openfire.gateway.protocols.xmpp.packet;

import org.jivesoftware.smack.packet.PacketExtension;

/**
 * See: http://code.google.com/apis/talk/jep_extensions/shared_status.html
 * 
 * @author Daniel Henninger
 */
public class GoogleSharedStatusExtension implements PacketExtension {

    public static String ELEMENT_NAME = "query";
    public static String NAMESPACE = "google:shared-status";

    public String getElementName() {
        return ELEMENT_NAME;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append("\"/>");
        return buf.toString();
    }

}
