/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2006-2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.protocols.irc;

import net.sf.kraken.muc.BaseMUCTransport;
import net.sf.kraken.muc.MUCTransportSession;
import net.sf.kraken.session.TransportSession;

/**
 * @author Daniel Henninger
 */
public class IRCMUCTransport extends BaseMUCTransport {

    /**
     * Handles creation of a new IRCMUCTransport.
     *
     * @param transport Transport we are attached to.
     */
    public IRCMUCTransport(IRCTransport transport) {
        super(transport);
    }

    /**
     * @see net.sf.kraken.muc.BaseMUCTransport#createRoom(net.sf.kraken.session.TransportSession, String, String)
     */
    public MUCTransportSession createRoom(TransportSession transportSession, String roomname, String nickname) {
        return new IRCMUCSession(transportSession, roomname, nickname, this);
    }
    
}
