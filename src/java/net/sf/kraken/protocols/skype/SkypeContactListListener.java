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

import com.skype.ContactList;

public class SkypeContactListListener extends SkypeListener implements PropertyChangeListener {

	private final ContactList contactList;

	public SkypeContactListListener(SkypeSession skypeSession,
			ContactList contactList) {
		super(skypeSession);
		this.contactList = contactList;
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		final String propertyName = evt.getPropertyName();
		final Object newValue = evt.getNewValue();
		if (ContactList.STATUS_PROPERTY.equals(propertyName)) {
			handleContactListChange(newValue);
		}
	}

	private void handleContactListChange(Object newValue) {
        if (getSession().getBuddyManager().isActivated()) {
//                final SkypeBuddy buddy = getSession().getBuddyManager().getBuddy(getSession().getTransport().convertIDToJID(friend.getEmail().toString()));
        }
	}
}
