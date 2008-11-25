/**
 * $Revision$
 * $Date$
 *
 * Copyright 2008 Daniel Henninger.  All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.protocols.xmpp;

import net.sf.kraken.roster.TransportBuddy;
import net.sf.kraken.roster.TransportBuddyManager;

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
