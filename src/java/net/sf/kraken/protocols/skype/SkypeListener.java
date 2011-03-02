/*******************************************************************************
 * Copyright (c) 2011 Markus Alexander Kuppe.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Markus Alexander Kuppe (ecf-dev_eclipse.org <at> lemmster <dot> de) - initial API and implementation
 ******************************************************************************/
package net.sf.kraken.protocols.skype;

import java.lang.ref.WeakReference;

import org.apache.log4j.Logger;

import com.skype.SkypeExceptionHandler;

public abstract class SkypeListener implements SkypeExceptionHandler {
    static Logger Log = Logger.getLogger(SkypeListener.class);
    
	private WeakReference<SkypeSession> skypeSessionRef;

    public SkypeListener(SkypeSession session) {
        this.skypeSessionRef = new WeakReference<SkypeSession>(session);
    }

    public SkypeSession getSession() {
        return skypeSessionRef.get();
    }

	/* (non-Javadoc)
	 * @see com.skype.SkypeExceptionHandler#uncaughtExceptionHappened(java.lang.Throwable)
	 */
	public void uncaughtExceptionHappened(Throwable e) {
        Log.error("Skype exception caught: ", e);
	}
}
