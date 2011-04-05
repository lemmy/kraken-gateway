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

public abstract class SkypeListener {
    static Logger Log = Logger.getLogger(SkypeListener.class);
    
	private WeakReference<SkypeSession> skypeSessionRef;

    public SkypeListener(SkypeSession session) {
        this.skypeSessionRef = new WeakReference<SkypeSession>(session);
    }

    public SkypeSession getSession() {
        return skypeSessionRef.get();
    }
}
