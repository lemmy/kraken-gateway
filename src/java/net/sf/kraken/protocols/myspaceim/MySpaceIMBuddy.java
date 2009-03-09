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

import net.sf.kraken.roster.TransportBuddy;
import net.sf.kraken.roster.TransportBuddyManager;

import java.util.Arrays;

/**
 * @author Daniel Henninger
 */
public class MySpaceIMBuddy extends TransportBuddy {

    public MySpaceIMBuddy(TransportBuddyManager manager, Integer userid) {
        super(manager, String.valueOf(userid), String.valueOf(userid), Arrays.asList("IM Buddies"));
    }

}
