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
import net.sf.kraken.protocols.facebook.FacebookBuddy;
import net.sf.kraken.type.PresenceType;

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

    public void processMessage(Message msgPacket) {
        Log.debug("MySpaceIM: Received message packet: "+msgPacket);
        if (msgPacket.getType().equals(Message.Type.ACTION_MESSAGE)) {
            if (msgPacket.getBody().equals("%typing%")) {
                getSession().getTransport().sendComposingNotification(
                        getSession().getJID(),
                        getSession().getTransport().convertIDToJID(msgPacket.getFrom())
                );
            }
            else if (msgPacket.getBody().equals("%stoptyping%")) {
                getSession().getTransport().sendComposingPausedNotification(
                        getSession().getJID(),
                        getSession().getTransport().convertIDToJID(msgPacket.getFrom())
                );
            }
        }
        else if (msgPacket.getType().equals(Message.Type.INSTANT_MESSAGE)) {
            getSession().getTransport().sendMessage(
                    getSession().getJID(),
                    getSession().getTransport().convertIDToJID(msgPacket.getFrom()),
                    msgPacket.getBody()
            );
        }
        else if (msgPacket.getType().equals(Message.Type.STATUS_MESSAGE)) {
            String statusPcs[] = msgPacket.getBody().split("|");
            int statusType = Integer.valueOf(statusPcs[1]);
            String statusMsg = statusPcs[3];
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
                    ((MySpaceIMTransport)getSession().getTransport()).convertMySpaceIMStatusToXMPP(statusType),
                    statusMsg
            );
        }
        else if (msgPacket.getType().equals(Message.Type.PROFILE_MESSAGE)) {
            // TODO: Incorporate into the buddy list
        }
        else if (msgPacket.getType().equals(Message.Type.MEDIA_MESSAGE)) {
            // ignore for the time being
        }
    }
    
}
