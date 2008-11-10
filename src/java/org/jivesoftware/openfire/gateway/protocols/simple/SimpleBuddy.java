/*
 * Licensed to the ProTel Communications Ltd. (PT) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The PT licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.openfire.gateway.protocols.simple;

import org.jivesoftware.openfire.gateway.roster.TransportBuddy;
import org.jivesoftware.openfire.gateway.roster.TransportBuddyManager;
import org.jivesoftware.openfire.gateway.pseudoroster.PseudoRosterItem;

import javax.sip.Dialog;

/**
 * This class represents a roster item of SIP transport.
 * @author Patrick Siu
 * @author Daniel Henninger
 */
public class SimpleBuddy extends TransportBuddy {
	private SimplePresence presence;
	private Dialog         outgoingDialog;

    public PseudoRosterItem pseudoRosterItem = null;

    public SimpleBuddy(TransportBuddyManager manager, String username, PseudoRosterItem rosterItem) {
        super(manager, username, null, null);
        pseudoRosterItem = rosterItem;
        this.setNickname(rosterItem.getNickname());
        this.setGroups(rosterItem.getGroups());

		presence = new SimplePresence();
		presence.setTupleStatus(SimplePresence.TupleStatus.CLOSED);
		
		outgoingDialog = null;
	}

	public void updatePresence(String newPresence) throws Exception {
		presence.parse(newPresence);
	}
	
	public void setOutgoingDialog(Dialog outgoingDialog) {
		this.outgoingDialog = outgoingDialog;
	}
	
	public Dialog getOutgoingDialog() {
		return outgoingDialog;
	}
}
