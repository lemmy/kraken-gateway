/**
 * $Revision$
 * $Date$
 *
 * Copyright 2008 Daniel Henninger.  All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */
package net.sf.kraken.protocols.qq;

import net.sf.jqql.QQ;
import net.sf.jqql.QQClient;
import net.sf.jqql.beans.QQUser;
import net.sf.jqql.net.PortGateFactory;
import net.sf.kraken.registration.Registration;
import net.sf.kraken.roster.TransportBuddy;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.ChatStateType;
import net.sf.kraken.type.PresenceType;
import net.sf.kraken.type.TransportLoginStatus;

import org.xmpp.packet.JID;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import java.util.*;

public class QQSession extends TransportSession {

    static Logger Log = Logger.getLogger(QQSession.class);

    private List<String> tcpServerList = new ArrayList<String>();
    private List<String> udpServerList = new ArrayList<String>();
    private QQClient qqclient;
    private QQUser qquser;
//    private Map<Integer,
//                ClusterInfo> clusters = new HashMap<Integer, ClusterInfo>();
//    private Map<Integer,
//                Map<Integer, String>> clusterMembers = new Hashtable<Integer,
//            Map<Integer, String>>(); //group members
    
    /**
     * QQ session listener.
     */
    private QQListener qqListener;
    
    private void setupDefaultServerList() {
        // set up default tcp server list
        Collections.addAll(tcpServerList,
    		"tcpconn.tencent.com",
    		"tcpconn2.tencent.com",
    		"tcpconn3.tencent.com",
    		"tcpconn4.tencent.com",
    		"tcpconn5.tencent.com",
    		"tcpconn6.tencent.com"
		);
        Collections.shuffle(tcpServerList);
        // set up default udp server list
        Collections.addAll(udpServerList,
    		"sz.tencent.com",
    		"sz2.tencent.com",
    		"sz3.tencent.com",
    		"sz4.tencent.com",
    		"sz5.tencent.com",
    		"sz6.tencent.com",
    		"sz7.tencent.com",
    		"sz8.tencent.com",
    		"sz9.tencent.com"
		);
        Collections.shuffle(udpServerList);
        // TODO: Add preferred server to top of list, and add checkbox for udp vs tcp
    }
    
    public QQSession(Registration registration, JID jid,
                     QQTransport transport, Integer priority) {
        super(registration, jid, transport, priority);
        qquser = new QQUser(Integer.parseInt(registration.getUsername()),
                            registration.getPassword());
        qquser.setStatus(QQ.QQ_LOGIN_MODE_NORMAL);
        qquser.setUdp(true);
        qquser.setShowFakeCam(false);
        setupDefaultServerList();
    }

    public void updateStatus(PresenceType presenceType, String verboseStatus) {
        if (isLoggedIn()) {
        	if (presenceType.equals(PresenceType.away) ||
        			presenceType.equals(PresenceType.xa) ||
        			presenceType.equals(PresenceType.dnd)) {
        		qqclient.makeMeAway();
        	}
        	else {
        		qqclient.makeMeOnline();
        	}
//            try { 
//                qquser.setStatus(((QQTransport) getTransport()).
//                                 convertJabStatusToQQ(presenceType));
//            } catch (IllegalStateException e) {
//                // Nothing to do
//            	Log.debug("Failed to change QQ status: ", e);
//            }
        }

    }

    public void addContact(JID jid, String nickname, ArrayList<String> groups) {
    	//qqclient.addFriend(Integer.valueOf(getTransport().convertJIDToID(jid)));
    	qqclient.sendAddFriendAuth(Integer.valueOf(getTransport().convertJIDToID(jid)), "Please accept my friend request!");
    	qqclient.downloadFriend(Integer.valueOf(getTransport().convertJIDToID(jid)));
    }

    public void removeContact(TransportBuddy transportBuddy) {
    	qqclient.deleteFriend(Integer.valueOf(getTransport().convertJIDToID(transportBuddy.getJID())));
    }

    public void updateContact(TransportBuddy transportBuddy) {
    	// There's nothing to change here currently.
    }
    
    /**
     * @see net.sf.kraken.session.TransportSession#acceptAddContact(TransportBuddy) 
     */
    public void acceptAddContact(TransportBuddy contact) {
        // TODO: Currently unimplemented
    }

    public void sendMessage(JID jID, String message) {
        try {
            int qqNum = Integer.parseInt(getTransport().convertJIDToID(jID));
//            if (clusters.get(qqNum) != null) {
//                qqclient.sendClusterIM(clusters.get(qqNum).clusterId,
//                                        message);
//            } else {
                qqclient.sendIM(qqNum, message.getBytes());
//            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void sendChatState(JID jID, ChatStateType chatStateType) {
    	// either not supported by QQ, or not supported by the lumaqq library
    }

    public void sendBuzzNotification(JID jID, String string) {
    	// either not supported by QQ, or not supported by the lumaqq library
    }

    public void logIn(PresenceType presenceType, String string) {
        setPendingPresenceAndStatus(presenceType, verboseStatus);
    	if (udpServerList.isEmpty()) {
    		// Ran out of servers to try to log in to.  Dooh.
    		sessionDisconnectedNoReconnect("Unable to log into any QQ servers.");
    		return;
    	}
    	String qqserver = udpServerList.remove(0); // pull a server to connect to from the end of the list
        setLoginStatus(TransportLoginStatus.LOGGING_IN);
        qqclient = new QQClient();
        qqclient.setUser(qquser);
        qqclient.setConnectionPoolFactory(new PortGateFactory());
        //qqclient.setTcpLoginPort(8000);
        qqListener = new QQListener(this);
        qqclient.addQQListener(qqListener);
        qquser.setServerPort(8000);
        qqclient.setLoginServer(qqserver);
        try {
			qqclient.login();
		}
        catch (Exception e) {
			Log.debug("Login attempt at server "+qqserver+" failed, trying next.");
		}
    }

    public void logOut() {
        cleanUp();
        sessionDisconnectedNoReconnect(null);
    }

    public void cleanUp() {
    	if (qqclient != null) {
    		qqclient.logout();
    		qqclient.release();
    	}
        qqclient = null;
    }

    public void updateLegacyAvatar(String string, byte[] byteArray) {
    }

    public QQUser getQQUser() {
        return qquser;
    }
    
    public QQClient getQQClient() {
        return qqclient;
    }

}
