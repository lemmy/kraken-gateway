/**
 * $Revision$
 * $Date$
 *
 * Copyright 2008 Daniel Henninger.  All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.protocols.msn;

import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import net.sf.jml.MsnContact;
import net.sf.jml.MsnGroup;
import net.sf.kraken.roster.TransportBuddy;
import net.sf.kraken.roster.TransportBuddyManager;

/**
 * @author Daniel Henninger
 */
public class MSNBuddy extends TransportBuddy {

    static Logger Log = Logger.getLogger(MSNBuddy.class);

    public MSNBuddy(TransportBuddyManager manager, MsnContact msnContact) {
        super(manager, msnContact.getEmail().toString(), msnContact.getFriendlyName(), null);
        ArrayList<String> groups = new ArrayList<String>();
        for (MsnGroup group : msnContact.getBelongGroups()) {
            groups.add(group.getGroupName());
        }
        this.setGroups(groups);
        this.msnContact = msnContact;
        this.setPresenceAndStatus(((MSNTransport)getManager().getSession().getTransport()).convertMSNStatusToXMPP(msnContact.getStatus()), msnContact.getPersonalMessage());
    }

    public MsnContact msnContact = null;

    public MsnContact getMsnContact(){
        return msnContact;
    }

    public void setMsnContact(MsnContact msnContact) {
        if (JiveGlobals.getBooleanProperty("plugin.gateway.msn.autonickname", false)) {
            if (!getNickname().equals(msnContact.getDisplayName())) {
                setNickname(msnContact.getDisplayName());
                try {
                    getManager().getSession().getTransport().addOrUpdateRosterItem(getManager().getSession().getJID(), getJID(), getNickname(), getGroups());
                }
                catch (UserNotFoundException e) {
                    // Can't update something that's not really in our list.
                }
            }
        }
        this.msnContact = msnContact;
    }

}
