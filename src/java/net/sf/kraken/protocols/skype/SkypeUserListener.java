/*******************************************************************************
 * Copyright (c) 2011 Markus Alexander Kuppe.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Markus Alexander Kuppe (ecf-dev_eclipse.org <at> lemmster <dot> de) - initial API and implementation
 ******************************************************************************/
package net.sf.kraken.protocols.skype;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import net.sf.kraken.BaseTransport;
import net.sf.kraken.roster.TransportBuddyManager;
import net.sf.kraken.type.PresenceType;

import org.jivesoftware.util.NotFoundException;

import com.skype.Friend;
import com.skype.SkypeException;
import com.skype.User;

public class SkypeUserListener extends SkypeListener implements PropertyChangeListener {

	private final Friend friend;

	public SkypeUserListener(SkypeSession skypeSession, Friend friend) {
		super(skypeSession);
		this.friend = friend;
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		final String propertyName = evt.getPropertyName();
		final Object newValue = evt.getNewValue();
		if(User.MOOD_TEXT_PROPERTY.equals(propertyName)) {
			
		} else if (User.STATUS_PROPERTY.equals(propertyName)) {
			handleUserStatusChange((User.Status) newValue);
		}
	}

	private void handleUserStatusChange(User.Status skypeStatus) {
		SkypeBuddy buddy = null;
		String moodMessage = "";
		
		final TransportBuddyManager<SkypeBuddy> buddyManager = getSession().getBuddyManager();
		try {
			final BaseTransport<SkypeBuddy> transport = getSession().getTransport();
			buddy = buddyManager.getBuddy(transport.convertIDToJID(friend.getId()));
			moodMessage = friend.getMoodMessage();
		} catch (NotFoundException e) {
			// Not in our contact list.  Ignore.
			Log.debug("Skype: Received presense notification for contact we don't care about: "+friend.getId());
			return;
		} catch (SkypeException e) {
			e.printStackTrace();
		}

		final PresenceType presenceType = ((SkypeSession)getSession()).convertSkypeStatusToXMPP(skypeStatus);
		if (buddyManager.isActivated()) {
			buddy.setPresenceAndStatus(presenceType, moodMessage);
		} else {
			buddyManager.storePendingStatus(buddy.getJID(), presenceType, moodMessage);
		}
	}
}
