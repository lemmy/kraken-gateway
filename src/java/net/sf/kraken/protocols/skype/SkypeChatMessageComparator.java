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

import java.util.Comparator;
import java.util.Date;

import com.skype.ChatMessage;
import com.skype.SkypeException;

public class SkypeChatMessageComparator implements Comparator<ChatMessage> {

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(ChatMessage c1, ChatMessage c2) {
		try {
			final Date t1 = c1.getTime();
			final Date t2 = c2.getTime();
			return t1.compareTo(t2);
		} catch (SkypeException e) {
			e.printStackTrace();
			return 0;
		}
	}
}
