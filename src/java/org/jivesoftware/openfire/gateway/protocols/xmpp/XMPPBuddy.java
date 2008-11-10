/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2006-2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.openfire.gateway.protocols.xmpp;

import org.jivesoftware.openfire.gateway.roster.TransportBuddy;
import org.jivesoftware.openfire.gateway.roster.TransportBuddyManager;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterEntry;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Henninger
 */
public class XMPPBuddy extends TransportBuddy {

    public XMPPBuddy(TransportBuddyManager manager, String username, String nickname, Collection<RosterGroup> groups, RosterEntry entry) {
        super(manager, username, nickname, null);
        ArrayList<String> groupList = new ArrayList<String>();
        for (RosterGroup group : groups) {
            groupList.add(group.getName());
        }
        this.setGroups(groupList);
        this.rosterEntry = entry;
    }

    public RosterEntry rosterEntry = null;

}
