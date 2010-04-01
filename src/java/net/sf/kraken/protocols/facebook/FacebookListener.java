/**
 * $Revision$
 * $Date$
 *
 * Copyright 2006-2010 Daniel Henninger.  All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */
package net.sf.kraken.protocols.facebook;

import java.lang.ref.WeakReference;

import net.sf.jfacebookiml.FacebookEventListener;
import net.sf.jfacebookiml.FacebookMessage;
import net.sf.jfacebookiml.FacebookUser;
import net.sf.kraken.avatars.Avatar;
import net.sf.kraken.type.PresenceType;

import org.apache.log4j.Logger;
import org.jivesoftware.util.NotFoundException;
import org.xmpp.packet.Message.Type;

public class FacebookListener implements FacebookEventListener {
	
    static Logger Log = Logger.getLogger(FacebookListener.class);
    
    FacebookListener(FacebookSession session) {
        this.facebookSessionRef = new WeakReference<FacebookSession>(session);
    }

    WeakReference<FacebookSession> facebookSessionRef;

    public FacebookSession getSession() {
        return facebookSessionRef.get();
    }

	public void contactChangedStatus(FacebookUser user) {
		// TODO Auto-generated method stub
        //Activate the buddy list if it's not already
        if (!getSession().getBuddyManager().isActivated()) {
            getSession().getBuddyManager().activate();
        }
        FacebookBuddy buddy;
        try {
            buddy = getSession().getBuddyManager().getBuddy(getSession().getTransport().convertIDToJID(user.uid));
        }
        catch (NotFoundException e) {
            buddy = new FacebookBuddy(getSession().getBuddyManager(), user);
            getSession().getBuddyManager().storeBuddy(buddy); 
        }
        if(user.isOnline && user.isIdle)
            buddy.setPresenceAndStatus(PresenceType.away, user.status);
        else if(user.isOnline)
            buddy.setPresenceAndStatus(PresenceType.available, user.status);
        else
            buddy.setPresenceAndStatus(PresenceType.unavailable, user.status);
        Avatar avatar = new Avatar(buddy.getJID(), user.thumbSrc, (getSession()).readUrlBytes(user.thumbSrc));
        buddy.setAvatar(avatar);
	}

	public void receivedErrorEvent(String summary, String description) {
        getSession().getTransport().sendMessage(getSession().getJID(),
                getSession().getTransport().getJID(),
                summary, Type.error);	
	}

	public void receivedMessageEvent(FacebookMessage message) {
		getSession().getTransport().sendMessage(getSession().getJID(), getSession().getTransport().convertIDToJID(message.from.toString()), message.text);
	}

	public void receivedTypingEvent(String from, Boolean isTyping) {
		if (isTyping) {
			getSession().getTransport().sendComposingNotification(getSession().getJID(), getSession().getTransport().convertIDToJID(from));
		}
		else {
            getSession().getTransport().sendComposingPausedNotification(getSession().getJID(), getSession().getTransport().convertIDToJID(from));
		}
	}

	public void sessionDisconnected(String reason) {
		getSession().sessionDisconnected(reason);
	}

}
