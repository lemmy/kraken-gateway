/**
 * $Revision$
 * $Date$
 *
 * Copyright 2009 Daniel Henninger.  All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.protocols.facebook;

import net.sf.jfacebookiml.FacebookUser;
import net.sf.kraken.roster.TransportBuddy;
import net.sf.kraken.roster.TransportBuddyManager;

import java.util.Arrays;

/**
 * @author Daniel Henninger
 * @author Patrick Li
 * @author Maxime Chéramy
 */
public class FacebookBuddy extends TransportBuddy {
    
    FacebookUser user;

    public FacebookBuddy(TransportBuddyManager manager, FacebookUser user) {
        super(manager, user.uid, user.name, Arrays.asList("Facebook"));
        this.user = user;
    }
    
    public FacebookUser getUser() {
        return user;
    }
    
}
