package net.sf.kraken.protocols.qq;

import net.sf.jqql.QQ;
import net.sf.jqql.QQClient;
import net.sf.jqql.beans.ClusterIM;
import net.sf.jqql.beans.ClusterInfo;
import net.sf.jqql.beans.DownloadFriendEntry;
import net.sf.jqql.beans.FriendOnlineEntry;
import net.sf.jqql.beans.NormalIM;
import net.sf.jqql.beans.QQFriend;
import net.sf.jqql.beans.QQUser;
import net.sf.jqql.events.IQQListener;
import net.sf.jqql.events.QQEvent;
import net.sf.jqql.net.PortGateFactory;
import net.sf.jqql.packets.in.ClusterCommandReplyPacket;
import net.sf.jqql.packets.in.DownloadGroupFriendReplyPacket;
import net.sf.jqql.packets.in.FriendChangeStatusPacket;
import net.sf.jqql.packets.in.GetFriendListReplyPacket;
import net.sf.jqql.packets.in.GetOnlineOpReplyPacket;
import net.sf.jqql.packets.in.GroupDataOpReplyPacket;
import net.sf.jqql.packets.in.ReceiveIMPacket;
import net.sf.kraken.registration.Registration;
import net.sf.kraken.roster.TransportBuddy;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.ChatStateType;
import net.sf.kraken.type.PresenceType;
import net.sf.kraken.type.TransportLoginStatus;

import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.NotFoundException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import java.util.*;
import java.text.SimpleDateFormat;

public class QQSession extends TransportSession implements IQQListener {

    static Logger Log = Logger.getLogger(QQSession.class);

    private List<String> tcpServerList = new ArrayList<String>();
    private List<String> udpServerList = new ArrayList<String>();
    private static String defaultGroupName = JiveGlobals.getProperty(
            "plugin.gateway.qq.defaultRosterName", "Friends");
    public static final SimpleDateFormat sdf = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");
    private QQClient qqclient;
    private QQUser qquser;
    private List<String> groupNames = new ArrayList<String>();
    private Map<Integer, QQFriend> friends = new HashMap<Integer, QQFriend>();
    private Map<Integer, String> friendGroup = new HashMap<Integer, String>();
    private Map<Integer,
                ClusterInfo> clusters = new HashMap<Integer, ClusterInfo>();
    private Map<Integer,
                Map<Integer, String>> clusterMembers = new Hashtable<Integer,
            Map<Integer, String>>(); //group members
    
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

    public void updateStatus(PresenceType presenceType, String string) {
        if (isLoggedIn()) {
            try { 
                qquser.setStatus(((QQTransport) getTransport()).
                                 convertJabStatusToQQ(presenceType));
            } catch (IllegalStateException e) {
                // Nothing to do
            }
        }

    }

    public void addContact(JID jid, String nickname, ArrayList<String> groups) {
    	qqclient.addFriend(Integer.valueOf(getTransport().convertJIDToID(jid)));
    }

    public void removeContact(TransportBuddy transportBuddy) {
    	qqclient.deleteFriend(Integer.valueOf(getTransport().convertJIDToID(jid)));
    }

    public void updateContact(TransportBuddy transportBuddy) {
    	// There's nothing to change here currently.
    }

    public void sendMessage(JID jID, String message) {
        try {
            int qqNum = Integer.parseInt(getTransport().convertJIDToID(jID));
            if (clusters.get(qqNum) != null) {
                qqclient.sendClusterIM(clusters.get(qqNum).clusterId,
                                        message);
            } else {
                qqclient.sendIM(qqNum, message.getBytes());
            }

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
        qqclient.addQQListener(this);
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

    public QQUser getQquser() {
        return qquser;
    }

    public void qqEvent(QQEvent e) {
        Log.debug(" QQEvent: " + Integer.toHexString(e.type) +
                           " " + e.getSource());
        switch (e.type) {
        case QQEvent.QQ_LOGIN_FAIL:
            sessionDisconnectedNoReconnect(null);
            break;
        case QQEvent.QQ_LOGIN_UNKNOWN_ERROR:
        case QQEvent.QQ_CONNECTION_BROKEN:
        case QQEvent.QQ_CONNECTION_LOST:
            sessionDisconnected(null);
            break;
        case QQEvent.QQ_CHANGE_STATUS_SUCCESS:
            processStatusChangeOK(e);
            break;
//        case QQEvent.USER_STATUS_CHANGE_FAIL:
//            sessionDisconnected(null);
//            break;
//        case QQEvent.FRIEND_DOWNLOAD_GROUPS_OK:
//            processGroupFriend(e);
//            break;
//        case QQEvent.FRIEND_GET_GROUP_NAMES_OK:
//            processGroupNames(e);
//            break;
        case QQEvent.QQ_GET_CLUSTER_INFO_SUCCESS:
            processClusterInfo(e);
            break;
//        case QQEvent.QQ_GET_MEMBER_INFO_SUCCESS
//            processClusterMemberInfo(e);
//            break;
        case QQEvent.QQ_RECEIVE_CLUSTER_IM:
            processClusterIM(e);
            break;
        case QQEvent.QQ_RECEIVE_NORMAL_IM:
            processNormalIM(e);
            break;
        case QQEvent.QQ_NETWORK_ERROR:
        case QQEvent.QQ_RUNTIME_ERROR:
            sessionDisconnected(null);
            break;
//        case QQEvent.FRIEND_GET_ONLINE_OK:
//            processFriendOnline(e);
//            break;
        case QQEvent.QQ_FRIEND_CHANGE_STATUS:
            processFriendChangeStatus(e);
            break;
        case QQEvent.QQ_GET_FRIEND_LIST_SUCCESS:
            processFriendList(e);
            break;
        default:
            break;

        }

    }

    private void processFriendList(QQEvent e) {
        try {
            GetFriendListReplyPacket p =
                    (GetFriendListReplyPacket) e.getSource();
            for (QQFriend f : p.friends) {
                friends.put(f.qqNum, f);
            }
            if (p.position != 0xFFFF) {
                qqclient.getFriendList(p.position);
            } else {
                syncContactGroups();
            }
        } catch (Exception ex) {
        	Log.error("Failed to process friend list: ", ex);
        }
        
        // Lets try the actual sync.
        try {
            getTransport().syncLegacyRoster(getJID(), getBuddyManager().getBuddies());
        }
        catch (UserNotFoundException ex) {
            Log.debug("Unable to sync yahoo contact list for " + getJID());
        }
        
        getBuddyManager().activate();
    }

    public void syncContactGroups() {
        for (QQFriend f : friends.values()) {
            String groupName = friendGroup.get(f.qqNum);
            if (groupName == null || groupName.trim().length() < 1) {
                groupName = defaultGroupName;
            }
            List<String> gl = new ArrayList<String>();
            gl.add(groupName);
            TransportBuddy tb = new TransportBuddy(getBuddyManager(),
                    String.valueOf(f.qqNum), f.nick, gl);
            getBuddyManager().storeBuddy(tb);
        }

    }

    private void processGroupFriend(QQEvent e) {
        try {
            DownloadGroupFriendReplyPacket p =
                    (DownloadGroupFriendReplyPacket) e.getSource();
            for (DownloadFriendEntry entry : p.friends) {
                if (entry.isCluster()) {
                    qqclient.getClusterInfo(entry.qqNum);
                } else {
                    if (groupNames != null && groupNames.size() > entry.group) {
                        friendGroup.put(entry.qqNum, groupNames.get(entry.group));
                    } else {
                        friendGroup.put(entry.qqNum, defaultGroupName);
                    }
                }
            }
            if (p.beginFrom != 0) {
                qqclient.getClusterOnlineMember(p.beginFrom);
            }
        } catch (Exception ex) {
            Log.error("Failed to process group friend: ", ex);
        }
    }

    private void processGroupNames(QQEvent e) {
        try {
            groupNames.clear();
            groupNames.add(defaultGroupName);
            GroupDataOpReplyPacket p =
                    (GroupDataOpReplyPacket) e.getSource();
            groupNames.addAll(p.groupNames);
        } catch (Exception ex) {
            Log.error("Failed to process group names: ", ex);
        }
    }

    private void processClusterInfo(QQEvent e) {
        try {
            ClusterCommandReplyPacket p = (ClusterCommandReplyPacket) e.
                                          getSource();
            ClusterInfo info = p.info;
            if (QQ.QQ_CLUSTER_TYPE_PERMANENT == info.type) {
                clusters.put(info.externalId, info);
            }
            List<String> gl = new ArrayList<String>();
            gl.add(JiveGlobals.getProperty("plugin.gateway.qq.qqGroupName",
                                           "QQ Group"));
            TransportBuddy tb = new TransportBuddy(getBuddyManager(),
            		String.valueOf(info.externalId), info.name, gl);
            getBuddyManager().storeBuddy(tb);
            Presence pp = new Presence();
            pp.setFrom(getTransport().convertIDToJID(String.valueOf(info.externalId)));
            pp.setTo(getJID());
            pp.setShow(Presence.Show.chat);
            getTransport().sendPacket(pp);
            qqclient.getClusterMemberInfo(info.clusterId, p.members);
        } catch (Exception ex) {
            Log.error("Failed to process cluster info: ", ex);
        }
    }

    private void processClusterMemberInfo(QQEvent e) {
        try {
            ClusterCommandReplyPacket p = (ClusterCommandReplyPacket) e.
                                          getSource();
            Map<Integer, String> cmm = new HashMap<Integer, String>();
            for (Object obj : p.memberInfos) {
                QQFriend m = (QQFriend) obj;
                cmm.put(m.qqNum, m.nick);
            }
            int clusterId = 0;
            for (ClusterInfo c : clusters.values()) {
                if (c.clusterId == p.clusterId) {
                    clusterId = c.externalId;
                }
            }
            clusterMembers.put(clusterId, cmm);
        } catch (Exception ex) {
            Log.error("Failed to process cluster member info: ", ex);
        }
    }

    private void processStatusChangeOK(QQEvent e) {
        setLoginStatus(TransportLoginStatus.LOGGED_IN);
        Presence p = new Presence();
        p.setTo(getJID());
        p.setFrom(getTransport().getJID());
        p.setStatus("Chat");
        getTransport().sendPacket(p);
        qqclient.getFriendList();
        qqclient.downloadGroup();
//        qqclient.user_GetList();
//        qqclient.user_GetOnline();

    }

    private void processClusterIM(QQEvent e) {
        try {
            ReceiveIMPacket p = (ReceiveIMPacket) e.getSource();
            ClusterIM im = p.clusterIM;
            if (clusters.get(im.externalId) == null) {
                qqclient.downloadGroup();
            }
            String sDate = sdf.format(new Date(im.sendTime));
            String clusterName = "";
            try {
                clusterName = clusters.get(im.externalId).name;
            } catch (Exception ex) {
            	Log.debug("Failed to get cluster name: ", ex);
            }
            String senderName = " ";
            try {
                senderName = clusterMembers.get(im.externalId).get(im.sender);
            } catch (Exception ex) {
            	Log.debug("Failed to get sender name: ", ex);
            }
            String msg = clusterName + "[" + im.externalId + "]"
                         + senderName + "(" + im.sender + ") "
                         + sDate + ":\n"
                         + new String(im.messageBytes) + "\n";
            Message m = new Message();
            m.setType(Message.Type.chat);
            m.setTo(getJID());
            m.setFrom(getTransport().convertIDToJID(String.valueOf(im.externalId)));
            String b = " ";
            try {
                b = new String(msg);
            } catch (Exception ex) {
            	Log.debug("Failed to string-ify message: ", ex);
            }
            m.setBody(b);
            getTransport().sendPacket(m);
        } catch (Exception ex) {
        	Log.error("Failed to handle cluster IM: ", ex);
        }
    }

    private void processNormalIM(QQEvent e) {
        try {
            ReceiveIMPacket p = (ReceiveIMPacket) e.getSource();
            NormalIM im = p.normalIM;
            Message m = new Message();
            m.setType(Message.Type.chat);
            m.setTo(getJID());
            m.setFrom(getTransport().convertIDToJID(String.valueOf(p.normalHeader.sender)));
            String b = " ";
            try {
                b = new String(im.messageBytes);
            } catch (Exception ex) {
            	Log.debug("Failed to string-ify message: ", ex);
            }
            m.setBody(b);
            getTransport().sendPacket(m);
        } catch (Exception ex) {
            Log.error("Failed to handle normal IM: ", ex);
        }
    }

    private void processFriendOnline(QQEvent e) {
        try {
            GetOnlineOpReplyPacket p =
                    (GetOnlineOpReplyPacket) e.getSource();
            for (FriendOnlineEntry f : p.onlineFriends) {
                if (getBuddyManager().isActivated()) {
                    try {
                        TransportBuddy trBuddy = getBuddyManager().getBuddy(getTransport().convertIDToJID(String.valueOf(f.status.qqNum)));
                        trBuddy.setPresenceAndStatus(((QQTransport)getTransport()).convertQQStatusToXMPP(f.status.status), null);
                    }
                    catch (NotFoundException ee) {
                        // Not in our list.
                        Log.debug("QQ: Received presense notification for contact we don't care about: "+String.valueOf(f.status.qqNum));
                    }
                }
                else {
                	getBuddyManager().storePendingStatus(getTransport().convertIDToJID(String.valueOf(f.status.qqNum)), ((QQTransport)getTransport()).convertQQStatusToXMPP(f.status.status), null);
                }
            }
//            if (!p.finished) {
//                qqclient.getUser().user_GetOnline(p.position);
//            }
        } catch (Exception ex) {
            Log.error("Failed to handle friend online event: ", ex);
        }
    }

    private void processFriendChangeStatus(QQEvent e) {
        try {
            FriendChangeStatusPacket p =
                    (FriendChangeStatusPacket) e.getSource();
            if (getBuddyManager().isActivated()) {
                try {
                    TransportBuddy trBuddy = getBuddyManager().getBuddy(getTransport().convertIDToJID(String.valueOf(p.friendQQ)));
                    trBuddy.setPresenceAndStatus(((QQTransport)getTransport()).convertQQStatusToXMPP(p.status), null);
                }
                catch (NotFoundException ee) {
                    // Not in our list.
                    Log.debug("QQ: Received presense notification for contact we don't care about: "+String.valueOf(p.friendQQ));
                }
            }
            else {
            	getBuddyManager().storePendingStatus(getTransport().convertIDToJID(String.valueOf(p.friendQQ)), ((QQTransport)getTransport()).convertQQStatusToXMPP(p.status), null);
            }
        } catch (Exception ex) {
            Log.error("Failed to handle friend status change event: ", ex);
        }
    }

}
