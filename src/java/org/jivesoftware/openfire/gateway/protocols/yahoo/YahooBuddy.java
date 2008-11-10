/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2006-2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.openfire.gateway.protocols.yahoo;

import org.jivesoftware.openfire.gateway.roster.TransportBuddy;
import org.jivesoftware.openfire.gateway.roster.TransportBuddyManager;
import org.jivesoftware.openfire.gateway.pseudoroster.PseudoRosterItem;
import org.openymsg.network.YahooUser;

import java.util.Collection;

/**
 * @author Daniel Henninger
 */
public class YahooBuddy extends TransportBuddy {

    public YahooBuddy(TransportBuddyManager manager, YahooUser yahooUser, String nickname, Collection<String> groups, PseudoRosterItem rosterItem) {
        super(manager, yahooUser.getId(), nickname, groups);
        this.yahooUser = yahooUser;
        this.pseudoRosterItem = rosterItem;
        String custommsg = this.yahooUser.getCustomStatusMessage();
        if (custommsg != null) {
            this.verboseStatus = custommsg;
        }

        this.setPresenceAndStatus(((YahooTransport)getManager().getSession().getTransport()).convertYahooStatusToXMPP(yahooUser.getStatus()), yahooUser.getCustomStatusMessage());
    }

    public YahooUser yahooUser = null;

    public PseudoRosterItem pseudoRosterItem = null;
    
}
