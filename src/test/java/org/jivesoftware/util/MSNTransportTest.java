/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2006-2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.util;

import net.sf.kraken.protocols.msn.MSNTransport;
import net.sf.kraken.registration.Registration;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.PresenceType;
import net.sf.kraken.type.TransportType;

import org.xmpp.packet.JID;

public class MSNTransportTest
{

	/**
	 * @param args Arguments passed to program.
	 */
	public static void main(String[] args)
	{
		if (args.length!=4)
		{
			System.out.println("Syntax: java MSNTransportTest user password nickname jid");
			System.exit(0);
		}
		Log.setDebugEnabled(true);
		JID jid = new JID(args[3]);
		Registration registration = new Registration(jid, TransportType.msn, args[0], args[1], args[2], true);
		MSNTransport transport = new MSNTransport();
		transport.jid = jid;
		transport.setup(TransportType.msn, "MSN");
		TransportSession session = transport.registrationLoggedIn(registration,jid, PresenceType.available,"online",new Integer(1));
        transport.registrationLoggedOut(session);
    }

}
