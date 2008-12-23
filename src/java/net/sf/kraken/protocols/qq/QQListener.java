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
import net.sf.jqql.beans.FriendOnlineEntry;
import net.sf.jqql.beans.NormalIM;
import net.sf.jqql.beans.QQFriend;
import net.sf.jqql.events.IQQListener;
import net.sf.jqql.events.QQEvent;
import net.sf.jqql.packets.in.ChangeStatusReplyPacket;
import net.sf.jqql.packets.in.FriendChangeStatusPacket;
import net.sf.jqql.packets.in.GetFriendListReplyPacket;
import net.sf.jqql.packets.in.GetOnlineOpReplyPacket;
import net.sf.jqql.packets.in.ReceiveIMPacket;
import net.sf.kraken.type.TransportLoginStatus;

import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.NotFoundException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import java.util.*;
import java.lang.ref.WeakReference;

public class QQListener implements IQQListener {

    static Logger Log = Logger.getLogger(QQListener.class);

    private static String defaultGroupName = JiveGlobals.getProperty(
            "plugin.gateway.qq.defaultRosterName", "Friends");
//    public static final SimpleDateFormat sdf = new SimpleDateFormat(
//            "yyyy-MM-dd HH:mm:ss");
//    private List<String> groupNames = new ArrayList<String>();
    private Map<Integer, QQFriend> friends = new HashMap<Integer, QQFriend>();
    private Map<Integer, String> friendGroup = new HashMap<Integer, String>();
//    private Map<Integer,
//                ClusterInfo> clusters = new HashMap<Integer, ClusterInfo>();
//    private Map<Integer,
//                Map<Integer, String>> clusterMembers = new Hashtable<Integer,
//            Map<Integer, String>>(); //group members
    
    /**
     * Creates a QQ session listener affiliated with a session.
     *
     * @param session The QQSession instance we are associated with.
     */
    public QQListener(QQSession session) {
        this.qqSessionRef = new WeakReference<QQSession>(session);
    }

    /**
     * The transport session we are affiliated with.
     */
    WeakReference<QQSession> qqSessionRef;

    /**
     * Returns the QQ session this listener is attached to.
     *
     * @return QQ session we are attached to.
     */
    public QQSession getSession() {
        return qqSessionRef.get();
    }
    
    public void qqEvent(QQEvent e) {
        Log.debug("Received - " + e.getSource() + " Event ID: "+e.type);
        switch (e.type) {
            case QQEvent.QQ_LOGIN_SUCCESS:
                processSuccessfulLogin();
                break;
            case QQEvent.QQ_LOGIN_FAIL:
                getSession().sessionDisconnectedNoReconnect(null);
                break;
            case QQEvent.QQ_LOGIN_UNKNOWN_ERROR:
            case QQEvent.QQ_CONNECTION_BROKEN:
            case QQEvent.QQ_CONNECTION_LOST:
                getSession().sessionDisconnected(null);
                break;
            case QQEvent.QQ_CHANGE_STATUS_SUCCESS:
                processStatusChangeOK((ChangeStatusReplyPacket)e.getSource());
                break;
            case QQEvent.QQ_CHANGE_STATUS_FAIL:
                getSession().sessionDisconnected(null);
                break;
    //        case QQEvent.QQ_DOWNLOAD_GROUP_FRIEND_SUCCESS:
    //            processGroupFriend(e);
    //            break;
    //        case QQEvent.QQ_DOWNLOAD_GROUP_NAME_SUCCESS:
    //            processGroupNames(e);
    //            break;
    //        case QQEvent.QQ_GET_CLUSTER_INFO_SUCCESS:
    //            processClusterInfo(e);
    //            break;
    //        case QQEvent.QQ_GET_MEMBER_INFO_SUCCESS:
    //            processClusterMemberInfo(e);
    //            break;
    //        case QQEvent.QQ_RECEIVE_CLUSTER_IM:
    //            processClusterIM(e);
    //            break;
            case QQEvent.QQ_RECEIVE_NORMAL_IM:
                processNormalIM((ReceiveIMPacket)e.getSource());
                break;
            case QQEvent.QQ_NETWORK_ERROR:
            case QQEvent.QQ_RUNTIME_ERROR:
                getSession().sessionDisconnected(null);
                break;
            case QQEvent.QQ_GET_FRIEND_ONLINE_SUCCESS:
                processFriendOnline((GetOnlineOpReplyPacket)e.getSource());
                break;
            case QQEvent.QQ_FRIEND_CHANGE_STATUS:
                processFriendChangeStatus((FriendChangeStatusPacket)e.getSource());
                break;
            case QQEvent.QQ_GET_FRIEND_LIST_SUCCESS:
                processFriendList((GetFriendListReplyPacket)e.getSource());
                break;
            default:
                break;
    
        }
    }

    private void processFriendList(GetFriendListReplyPacket p) {
        try {
            for (QQFriend f : p.friends) {
                Log.debug("Found QQ friend: "+f);
                friends.put(f.qqNum, f);
                
                String groupName = friendGroup.get(f.qqNum);
                if (groupName == null || groupName.trim().length() < 1) {
                    groupName = defaultGroupName;
                }
                List<String> gl = new ArrayList<String>();
                gl.add(groupName);
                QQBuddy qqBuddy = new QQBuddy(getSession().getBuddyManager(), f, f.nick, gl);
                getSession().getBuddyManager().storeBuddy(qqBuddy);
            }
            if (p.position != 0xFFFF) {
                getSession().getQQClient().getFriendList(p.position);
            }
        } catch (Exception ex) {
            Log.error("Failed to process friend list: ", ex);
        }
        
        // Lets try the actual sync.
        try {
            getSession().getTransport().syncLegacyRoster(getSession().getJID(), getSession().getBuddyManager().getBuddies());
        }
        catch (UserNotFoundException ex) {
            Log.debug("Unable to sync QQ contact list for " + getSession().getJID());
        }
        
        getSession().getBuddyManager().activate();
    }

//    private void processGroupFriend(QQEvent e) {
//        try {
//            DownloadGroupFriendReplyPacket p =
//                    (DownloadGroupFriendReplyPacket) e.getSource();
//            for (DownloadFriendEntry entry : p.friends) {
////                if (entry.isCluster()) {
////                    getSession().getQQClient().getClusterInfo(entry.qqNum);
////                } else {
//                    if (groupNames != null && groupNames.size() > entry.group) {
//                        friendGroup.put(entry.qqNum, groupNames.get(entry.group));
//                    } else {
//                        friendGroup.put(entry.qqNum, defaultGroupName);
//                    }
////                }
//            }
////            if (p.beginFrom != 0) {
////                getSession().getQQClient().getClusterOnlineMember(p.beginFrom);
////            }
//        } catch (Exception ex) {
//            Log.error("Failed to process group friend: ", ex);
//        }
//    }

//    private void processGroupNames(QQEvent e) {
//        try {
//            groupNames.clear();
//            groupNames.add(defaultGroupName);
//            GroupDataOpReplyPacket p =
//                    (GroupDataOpReplyPacket) e.getSource();
//            groupNames.addAll(p.groupNames);
//        } catch (Exception ex) {
//            Log.error("Failed to process group names: ", ex);
//        }
//    }

//    private void processClusterInfo(QQEvent e) {
//        try {
//            ClusterCommandReplyPacket p = (ClusterCommandReplyPacket) e.
//                                          getSource();
//            ClusterInfo info = p.info;
//            if (QQ.QQ_CLUSTER_TYPE_PERMANENT == info.type) {
//                clusters.put(info.externalId, info);
//            }
//            List<String> gl = new ArrayList<String>();
//            gl.add(JiveGlobals.getProperty("plugin.gateway.qq.qqGroupName",
//                                           "QQ Group"));
//            TransportBuddy tb = new TransportBuddy(getSession().getBuddyManager(),
//                    String.valueOf(info.externalId), info.name, gl);
//            getSession().getBuddyManager().storeBuddy(tb);
//            Presence pp = new Presence();
//            pp.setFrom(getSession().getTransport().convertIDToJID(String.valueOf(info.externalId)));
//            pp.setTo(getSession().getJID());
//            pp.setShow(Presence.Show.chat);
//            getSession().getTransport().sendPacket(pp);
//            qqclient.getClusterMemberInfo(info.clusterId, p.members);
//        } catch (Exception ex) {
//            Log.error("Failed to process cluster info: ", ex);
//        }
//    }
//
//    private void processClusterMemberInfo(QQEvent e) {
//        try {
//            ClusterCommandReplyPacket p = (ClusterCommandReplyPacket) e.
//                                          getSource();
//            Map<Integer, String> cmm = new HashMap<Integer, String>();
//            for (Object obj : p.memberInfos) {
//                QQFriend m = (QQFriend) obj;
//                cmm.put(m.qqNum, m.nick);
//            }
//            int clusterId = 0;
//            for (ClusterInfo c : clusters.values()) {
//                if (c.clusterId == p.clusterId) {
//                    clusterId = c.externalId;
//                }
//            }
//            clusterMembers.put(clusterId, cmm);
//        } catch (Exception ex) {
//            Log.error("Failed to process cluster member info: ", ex);
//        }
//    }
    
    private void processSuccessfulLogin() {
        getSession().setLoginStatus(TransportLoginStatus.LOGGED_IN);
        getSession().getQQClient().getFriendList();
//        getSession().getQQClient().downloadGroup();
        getSession().getQQClient().getFriendOnline();
    }

    private void processStatusChangeOK(ChangeStatusReplyPacket p) {
//        if (!getSession().isLoggedIn()) {
//            getSession().setLoginStatus(TransportLoginStatus.LOGGED_IN);
//            getSession().getQQClient().getFriendList();
//            getSession().getQQClient().downloadGroup();
//            getSession().getQQClient().getFriendOnline();
//        }
    }

//    private void processClusterIM(QQEvent e) {
//        try {
//            ReceiveIMPacket p = (ReceiveIMPacket) e.getSource();
//            ClusterIM im = p.clusterIM;
//            if (clusters.get(im.externalId) == null) {
//                qqclient.downloadGroup();
//            }
//            String sDate = sdf.format(new Date(im.sendTime));
//            String clusterName = "";
//            try {
//                clusterName = clusters.get(im.externalId).name;
//            } catch (Exception ex) {
//                Log.debug("Failed to get cluster name: ", ex);
//            }
//            String senderName = " ";
//            try {
//                senderName = clusterMembers.get(im.externalId).get(im.sender);
//            } catch (Exception ex) {
//                Log.debug("Failed to get sender name: ", ex);
//            }
//            String msg = clusterName + "[" + im.externalId + "]"
//                         + senderName + "(" + im.sender + ") "
//                         + sDate + ":\n"
//                         + new String(im.messageBytes) + "\n";
//            Message m = new Message();
//            m.setType(Message.Type.chat);
//            m.setTo(getSession().getJID());
//            m.setFrom(getSession().getTransport().convertIDToJID(String.valueOf(im.externalId)));
//            String b = " ";
//            try {
//                b = new String(msg);
//            } catch (Exception ex) {
//                Log.debug("Failed to string-ify message: ", ex);
//            }
//            m.setBody(b);
//            getSession().getTransport().sendPacket(m);
//        } catch (Exception ex) {
//            Log.error("Failed to handle cluster IM: ", ex);
//        }
//    }

    /**
     * Handles a standard instant message being sent to us.
     * 
     * @param e Event of the message.
     */
    private void processNormalIM(ReceiveIMPacket p) {
        NormalIM im = p.normalIM;
        String msg = "";
        try {
            msg = new String(im.messageBytes);
        } catch (Exception ex) {
            Log.debug("Failed to string-ify message: ", ex);
        }
        getSession().getTransport().sendMessage(
                getSession().getJID(),
                getSession().getTransport().convertIDToJID(String.valueOf(p.normalHeader.sender)),
                msg
        );
    }

    /**
     * Handles an event when a friend has come online.
     * 
     * @param e Event to be handled.
     */
    private void processFriendOnline(GetOnlineOpReplyPacket p) {
        try {
            for (FriendOnlineEntry f : p.onlineFriends) {
                if (getSession().getBuddyManager().isActivated()) {
                    try {
                        QQBuddy qqBuddy = (QQBuddy)getSession().getBuddyManager().getBuddy(getSession().getTransport().convertIDToJID(String.valueOf(f.status.qqNum)));
                        qqBuddy.setPresenceAndStatus(((QQTransport)getSession().getTransport()).convertQQStatusToXMPP(f.status.status), null);
                    }
                    catch (NotFoundException ee) {
                        // Not in our list.
                        Log.debug("QQ: Received presense notification for contact we don't care about: "+String.valueOf(f.status.qqNum));
                    }
                }
                else {
                    getSession().getBuddyManager().storePendingStatus(getSession().getTransport().convertIDToJID(String.valueOf(f.status.qqNum)), ((QQTransport)getSession().getTransport()).convertQQStatusToXMPP(f.status.status), null);
                }
            }
//            if (!p.finished) {
//                qqclient.getUser().user_GetOnline(p.position);
//            }
        } catch (Exception ex) {
            Log.error("Failed to handle friend online event: ", ex);
        }
    }

    /**
     * Handles an event where a friend changes their status.
     * 
     * @param e Event representing change.
     */
    private void processFriendChangeStatus(FriendChangeStatusPacket p) {
        try {
            if (getSession().getBuddyManager().isActivated()) {
                try {
                    QQBuddy qqBuddy = (QQBuddy)getSession().getBuddyManager().getBuddy(getSession().getTransport().convertIDToJID(String.valueOf(p.friendQQ)));
                    qqBuddy.setPresenceAndStatus(((QQTransport)getSession().getTransport()).convertQQStatusToXMPP(p.status), null);
                }
                catch (NotFoundException ee) {
                    // Not in our list.
                    Log.debug("QQ: Received presense notification for contact we don't care about: "+String.valueOf(p.friendQQ));
                }
            }
            else {
                getSession().getBuddyManager().storePendingStatus(getSession().getTransport().convertIDToJID(String.valueOf(p.friendQQ)), ((QQTransport)getSession().getTransport()).convertQQStatusToXMPP(p.status), null);
            }
        } catch (Exception ex) {
            Log.error("Failed to handle friend status change event: ", ex);
        }
    }

}