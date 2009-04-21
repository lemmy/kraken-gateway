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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

/**
 * @author Daniel Henninger
 * @author Patrick Li
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

    public byte[] readUrlBytes(String address) {
        byte[] byteReturn = null;
        URL theURL = null;
        InputStream inStream = null;
        try {
            theURL = new URL(address);
            inStream = theURL.openStream();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int c;
            while((c = inStream.read()) != -1) {
              os.write(c);
            }
            byteReturn = os.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inStream != null) {
                  inStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return byteReturn;
    }
    
}
