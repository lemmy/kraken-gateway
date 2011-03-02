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

import net.sf.kraken.BaseTransport;

import com.skype.ChatMessage;
import com.skype.ChatMessageListener;
import com.skype.SkypeException;
import com.skype.User;

public class SkypeChatMessageListener extends SkypeListener implements ChatMessageListener {

	public SkypeChatMessageListener(SkypeSession session) {
		super(session);
	}

	/* (non-Javadoc)
	 * @see com.skype.ChatMessageListener#chatMessageReceived(com.skype.ChatMessage)
	 */
	public void chatMessageReceived(ChatMessage receivedChatMessage)
			throws SkypeException {
		final String content = receivedChatMessage.getContent();
		final User sender = receivedChatMessage.getSender();
        
		Log.debug("Skype: Received IM text: "+content+" from " + sender.getId());

		final SkypeSession session = getSession();
		final BaseTransport<SkypeBuddy> transport = session.getTransport();
		transport.sendMessage(session.getJID(),
				transport.convertIDToJID(sender.getId()), content);
	}

	/* (non-Javadoc)
	 * @see com.skype.ChatMessageListener#chatMessageSent(com.skype.ChatMessage)
	 */
	public void chatMessageSent(ChatMessage sentChatMessage)
			throws SkypeException {
		final String content = sentChatMessage.getContent();
		final User sender = sentChatMessage.getSender();

		Log.debug("Skype: Send IM text: "+content+" from " + sender.getId());
	}
}
