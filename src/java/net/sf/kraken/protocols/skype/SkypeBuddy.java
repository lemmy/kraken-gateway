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

import java.util.Arrays;

import net.sf.kraken.roster.TransportBuddy;
import net.sf.kraken.roster.TransportBuddyManager;

public class SkypeBuddy extends TransportBuddy {
	
    public SkypeBuddy(TransportBuddyManager<SkypeBuddy> manager, String uin, String nickname, String group) {
        super(manager, uin, nickname, null);
        if (group != null) {
            this.setGroups(Arrays.asList(group));
        }
    }
}
