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

import net.sf.kraken.avatars.Avatar;
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
        
        // We also want to initialize their avatar picture here
        Avatar avatar = new Avatar(getJID(), user.thumbSrc, readUrlBytes(user.thumbSrc));
        setAvatar(avatar);
    }
    
    public FacebookUser getUser() {
        return user;
    }

    /**
     * Download the photo from the given address.
     * @param address the url
     * @return a flow of bytes contening the photo
     */
    public byte[] readUrlBytes(String address) {
    	FacebookHttpClient facebookHttpClient = new FacebookHttpClient();
    	
    	String response = facebookHttpClient.getMethod(address);
    	
    	if(response != null) {
    		return response.getBytes();
    	} else {
    		return null;
    	}
    }
    
}
