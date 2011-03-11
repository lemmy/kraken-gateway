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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.kraken.roster.TransportBuddy;
import net.sf.kraken.roster.TransportBuddyManager;

import com.skype.Friend;
import com.skype.Group;

public class SkypeBuddy extends TransportBuddy {
	
    private final Friend friend;
    private final Map<String, Group> skypeGroups;

    public SkypeBuddy(TransportBuddyManager<SkypeBuddy> manager, Friend aFriend, String nickname, Map<String, Group> aGroups) {
    	super(manager, aFriend.getId(), nickname, null);
    	friend = aFriend;
    	skypeGroups = aGroups;
    	
    	// populate roster groups
    	List<String> list = new ArrayList<String>();
    	for (String group : skypeGroups.keySet()) {
			list.add(group);
		}
    	this.setGroups(list);
	}

	/**
     * @return the friend
     */
    public Friend getFriend() {
        return friend;
    }

	/**
	 * @return the skypeGroups
	 */
	public Map<String, Group> getSkypeGroups() {
		return skypeGroups;
	}

	public Group addSkypeGroup(String displayName, Group group) {
		return skypeGroups.put(displayName, group);
	}

	public Group getSkypeGroup(String displayName) {
		return skypeGroups.get(displayName);
	}

	public Group removeSkypeGroup(String displayName) {
		return skypeGroups.remove(displayName);
	}
}
