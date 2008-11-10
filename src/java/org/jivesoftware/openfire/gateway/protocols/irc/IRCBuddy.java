/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.openfire.gateway.protocols.irc;

import org.jivesoftware.openfire.gateway.roster.TransportBuddy;
import org.jivesoftware.openfire.gateway.roster.TransportBuddyManager;
import org.jivesoftware.openfire.gateway.pseudoroster.PseudoRosterItem;

/**
 * @author Daniel Henninger
 */
public class IRCBuddy extends TransportBuddy {

    public IRCBuddy(TransportBuddyManager manager, String username, PseudoRosterItem item) {
        super(manager, username, null, null);
        pseudoRosterItem = item;
        this.setNickname(item.getNickname());
        this.setGroups(item.getGroups());
    }

    public PseudoRosterItem pseudoRosterItem = null;
    
}
