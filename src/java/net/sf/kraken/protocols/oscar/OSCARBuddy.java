/**
 * $Revision$
 * $Date$
 *
 * Copyright 2008 Daniel Henninger.  All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.protocols.oscar;

import net.kano.joscar.ssiitem.BuddyItem;
import net.sf.kraken.roster.TransportBuddy;
import net.sf.kraken.roster.TransportBuddyManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Daniel Henninger
 */
public class OSCARBuddy extends TransportBuddy {

    public OSCARBuddy(TransportBuddyManager manager, BuddyItem buddyItem) {
        super(manager, buddyItem.getScreenname(), buddyItem.getAlias(), null);
        buddyItems.put(buddyItem.getGroupId(), buddyItem);
        ((OSCARSession)getManager().getSession()).updateHighestId(buddyItem);
    }

    public ConcurrentHashMap<Integer,BuddyItem> buddyItems = new ConcurrentHashMap<Integer,BuddyItem>();

    /**
     * Runs through group id list, matching to finished collection of groups to get names.
     */
    public void populateGroupList() {
        ArrayList<String> groupList = new ArrayList<String>();
        for (BuddyItem buddyItem : buddyItems.values()) {
            try {
                groupList.add(((OSCARSession)getManager().getSession()).groups.get(buddyItem.getGroupId()).getGroupName());
            }
            catch (Exception e) {
                // Hrm, unknown group.  Don't include.
            }
        }
        setGroups(groupList);
    }

    /**
     * Ties another buddy item to this buddy.
     *
     * @param buddyItem Buddy item to attach.
     * @param autoPopulate Trigger an automatic regeneration of group list.
     */
    public synchronized void tieBuddyItem(BuddyItem buddyItem, Boolean autoPopulate) {
        buddyItems.put(buddyItem.getGroupId(), buddyItem);
        ((OSCARSession)getManager().getSession()).updateHighestId(buddyItem);
        if (autoPopulate) {
            populateGroupList();
        }
    }

    /**
     * Retrieves one of the buddy items, as tied to a group.
     *
     * @param groupId Group id of buddy item to retrieve.
     * @return Buddy item in said group.
     */
    public BuddyItem getBuddyItem(Integer groupId) {
        return buddyItems.get(groupId);
    }

    /**
     * Removes a buddy item from the list as specified by a group id.
     *
     * @param groupId Group id of buddy item to be removed.
     * @param autoPopulate Trigger an automatic regeneration of group list.
     */
    public synchronized void removeBuddyItem(Integer groupId, Boolean autoPopulate) {
        buddyItems.remove(groupId);
        if (autoPopulate) {
            populateGroupList();
        }
    }

    /**
     * Retrieves all of the buddy items associated with the buddy.
     *
     * @return List of buddy items associated with the buddy.
     */
    public Collection<BuddyItem> getBuddyItems() {
        return buddyItems.values();
    }

    /**
     * Updates the status of the buddy given an OSCAR FullUserInfo packet.
     *
     * @param info FullUserInfo packet to parse and set status based off of.
     */
//    public void parseFullUserInfo(FullUserInfo info) {
//        PresenceType pType = PresenceType.available;
//        String vStatus = "";
//        if (info.getAwayStatus()) {
//            pType = PresenceType.away;
//        }
//
//        if (getManager().getSession().getTransport().getType().equals(TransportType.icq) && info.getScreenname().matches("/^\\d+$/")) {
//            pType = ((OSCARTransport)getManager().getSession().getTransport()).convertICQStatusToXMPP(info.getIcqStatus());
//        }
//
//        List<ExtraInfoBlock> extraInfo = info.getExtraInfoBlocks();
//        if (extraInfo != null) {
//            for (ExtraInfoBlock i : extraInfo) {
//                ExtraInfoData data = i.getExtraData();
//
//                if (i.getType() == ExtraInfoBlock.TYPE_AVAILMSG) {
//                    ByteBlock msgBlock = data.getData();
//                    int len = BinaryTools.getUShort(msgBlock, 0);
//                    if (len >= 0) {
//                        byte[] msgBytes = msgBlock.subBlock(2, len).toByteArray();
//                        String msg;
//                        try {
//                            msg = new String(msgBytes, "UTF-8");
//                        }
//                        catch (UnsupportedEncodingException e1) {
//                            continue;
//                        }
//                        if (msg.length() > 0) {
//                            vStatus = msg;
//                        }
//                    }
//                }
//            }
//        }
//
//        setPresenceAndStatus(pType, vStatus);
//    }

}
