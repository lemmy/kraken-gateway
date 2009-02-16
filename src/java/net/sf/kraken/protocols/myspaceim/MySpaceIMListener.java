/**
 * $Revision$
 * $Date$
 *
 * Copyright 2009 Daniel Henninger.  All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.protocols.myspaceim;

import java.lang.ref.WeakReference;

import net.sf.jmyspaceiml.MessageListener;
import net.sf.jmyspaceiml.packet.Message;

import org.apache.log4j.Logger;

/**
 * @author Daniel Henninger
 */
public class MySpaceIMListener implements MessageListener {

    static Logger Log = Logger.getLogger(MySpaceIMListener.class);
    
    MySpaceIMListener(MySpaceIMSession session) {
        this.myspaceimSessionRef = new WeakReference<MySpaceIMSession>(session);
    }

    WeakReference<MySpaceIMSession> myspaceimSessionRef;

    public MySpaceIMSession getSession() {
        return myspaceimSessionRef.get();
    }

    public void processMessage(Message msgPacket) {
        getSession().getTransport().sendMessage(
            getSession().getJID(),
            getSession().getTransport().convertIDToJID(msgPacket.getFrom()),
            msgPacket.getBody()
        );
    }
    
}
