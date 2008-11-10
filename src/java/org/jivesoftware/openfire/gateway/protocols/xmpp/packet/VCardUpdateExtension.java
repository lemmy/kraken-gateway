/**
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Lesser Public License (LGPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.openfire.gateway.protocols.xmpp.packet;

import org.jivesoftware.openfire.gateway.type.NameSpace;
import org.jivesoftware.smack.packet.PacketExtension;

/**
 * Borrowed directly from Spark.
 */
public class VCardUpdateExtension implements PacketExtension {

    private String photoHash;

    public void setPhotoHash(String hash) {
        photoHash = hash;
    }

    public String getElementName() {
        return "x";
    }

    public String getNamespace() {
        return NameSpace.VCARD_TEMP_X_UPDATE;
    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append("\">");
        buf.append("<photo>");
        buf.append(photoHash);
        buf.append("</photo>");
        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }
    
}
