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

import java.lang.ref.WeakReference;

import org.apache.log4j.Logger;

/**
 * @author Daniel Henninger
 */
public class FacebookListener {

    static Logger Log = Logger.getLogger(FacebookListener.class);
    
    FacebookListener(FacebookSession session) {
        this.facebookSessionRef = new WeakReference<FacebookSession>(session);
    }

    WeakReference<FacebookSession> facebookSessionRef;

    public FacebookSession getSession() {
        return facebookSessionRef.get();
    }
    
}
