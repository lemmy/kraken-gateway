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
import net.sf.jmyspaceiml.packet.ActionMessage;
import net.sf.jmyspaceiml.packet.InstantMessage;
import net.sf.jmyspaceiml.packet.MediaMessage;
import net.sf.jmyspaceiml.packet.ProfileMessage;
import net.sf.jmyspaceiml.packet.StatusMessage;

import org.apache.log4j.Logger;
import org.jivesoftware.util.NotFoundException;

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

    public void processIncomingMessage(InstantMessage msgPacket) {
        Log.debug("MySpaceIM: Received instant message packet: "+msgPacket);
        getSession().getTransport().sendMessage(
                getSession().getJID(),
                getSession().getTransport().convertIDToJID(msgPacket.getFrom()),
                msgPacket.getBody()
        );
    }
    
    public void processIncomingMessage(ActionMessage msgPacket) {
        Log.debug("MySpaceIM: Received action message packet: "+msgPacket);
        if (msgPacket.getAction().equals(ActionMessage.ACTION_TYPING)) {
            getSession().getTransport().sendComposingNotification(
                    getSession().getJID(),
                    getSession().getTransport().convertIDToJID(msgPacket.getFrom())
            );
        }
        else if (msgPacket.getAction().equals(ActionMessage.ACTION_STOPTYPING)) {
            getSession().getTransport().sendComposingPausedNotification(
                    getSession().getJID(),
                    getSession().getTransport().convertIDToJID(msgPacket.getFrom())
            );
        }
    }
    
    public void processIncomingMessage(StatusMessage msgPacket) {
        Log.debug("MySpaceIM: Received status message packet: "+msgPacket);
        //Activate the buddy list if it's not already
        if (!getSession().getBuddyManager().isActivated()) {
            getSession().getBuddyManager().activate();
        }
        MySpaceIMBuddy buddy;
        try {
            buddy = (MySpaceIMBuddy)getSession().getBuddyManager().getBuddy(getSession().getTransport().convertIDToJID(msgPacket.getFrom()));
        }
        catch (NotFoundException e) {
            buddy = new MySpaceIMBuddy(getSession().getBuddyManager(), Integer.valueOf(msgPacket.getFrom()));
            getSession().getBuddyManager().storeBuddy(buddy); 
        }
        buddy.setPresenceAndStatus(
                ((MySpaceIMTransport)getSession().getTransport()).convertMySpaceIMStatusToXMPP(msgPacket.getStatusCode()),
                msgPacket.getStatusMessage()
        );
    }
    
    public void processIncomingMessage(MediaMessage msgPacket) {
        Log.debug("MySpaceIM: Received media message packet: "+msgPacket);
    }
    
    public void processIncomingMessage(ProfileMessage msgPacket) {
        Log.debug("MySpaceIM: Received profile message packet: "+msgPacket);
    }
    
}
