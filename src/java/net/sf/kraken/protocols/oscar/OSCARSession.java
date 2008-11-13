/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2006-2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.protocols.oscar;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.OscarTools;
import net.kano.joscar.SeqNum;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.net.ConnDescriptor;
import net.kano.joscar.snac.SnacRequest;
import net.kano.joscar.snac.SnacRequestListener;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joscar.snaccmd.ExtraInfoData;
import net.kano.joscar.snaccmd.InfoData;
import net.kano.joscar.snaccmd.conn.ServiceRequest;
import net.kano.joscar.snaccmd.conn.SetExtraInfoCmd;
import net.kano.joscar.snaccmd.icbm.SendImIcbm;
import net.kano.joscar.snaccmd.icbm.SendTypingNotification;
import net.kano.joscar.snaccmd.icq.MetaShortInfoRequest;
import net.kano.joscar.snaccmd.icq.OfflineMsgIcqRequest;
import net.kano.joscar.snaccmd.loc.SetInfoCmd;
import net.kano.joscar.snaccmd.mailcheck.MailCheckCmd;
import net.kano.joscar.snaccmd.ssi.*;
import net.kano.joscar.ssiitem.BuddyItem;
import net.kano.joscar.ssiitem.GroupItem;
import net.kano.joscar.ssiitem.IconItem;
import net.kano.joscar.ssiitem.VisibilityItem;
import net.sf.kraken.registration.Registration;
import net.sf.kraken.roster.TransportBuddy;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.ChatStateType;
import net.sf.kraken.type.PresenceType;
import net.sf.kraken.type.TransportType;

import org.apache.log4j.Logger;
import org.jivesoftware.openfire.roster.RosterItem;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.*;
import org.xmpp.packet.JID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Represents an OSCAR session.
 *
 * This is the interface with which the base transport functionality will
 * communicate with OSCAR (AIM/ICQ).
 *
 * Yeesh, this is the one I'm most familiar with and yet it's the ugliest.
 * This needs some housecleaning.
 * 
 * @author Daniel Henninger
 */
public class OSCARSession extends TransportSession {

    static Logger Log = Logger.getLogger(OSCARSession.class);

    /**
     * Initialize a new session object for OSCAR
     * 
     * @param registration The registration information to use during login.
     * @param jid The JID associated with this session.
     * @param transport The transport that created this session.
     * @param priority Priority of this session.
     */
    public OSCARSession(Registration registration, JID jid, OSCARTransport transport, Integer priority) {
        super(registration, jid, transport, priority);
        this.propertyPrefix = "plugin.gateway."+transport.getType().toString();
        OscarTools.setDefaultCharset(JiveGlobals.getProperty(this.propertyPrefix+".encoding", "ISO8859-1"));
        highestBuddyIdPerGroup.put(0, 0); // Main group highest id
        if (JiveGlobals.getBooleanProperty("plugin.gateway."+transport.getType()+".crosschat", true)) {
            MY_CAPS.add(CapabilityBlock.BLOCK_ICQCOMPATIBLE);
        }
    }

    private BOSConnection bosConn = null;
    private LoginConnection loginConn = null;
    private Set<ServiceConnection> services = new HashSet<ServiceConnection>();
    private String propertyPrefix;
    private static final String DEFAULT_AIM_GROUP = "   "; // We're using 3 spaces to indicated main group, invalid in real aim
    private static final String DEFAULT_ICQ_GROUP = "General";
    private SeqNum icqSeqNum = new SeqNum(0, Integer.MAX_VALUE);
    public IconItem storedIconInfo = null;
    public VisibilityItem storedVisibilityInfo = null;
    public byte[] pendingAvatar = null;

    private final List<CapabilityBlock> MY_CAPS = new ArrayList<CapabilityBlock>();

    public List<CapabilityBlock> getCapabilities() {
        return MY_CAPS;
    }

    /**
     * SSI tracking variables.
     */
    public ConcurrentHashMap<Integer,GroupItem> groups = new ConcurrentHashMap<Integer,GroupItem>();
    public ConcurrentHashMap<Integer,Integer> highestBuddyIdPerGroup = new ConcurrentHashMap<Integer,Integer>();

    /**
     * @see net.sf.kraken.session.TransportSession#logIn(net.sf.kraken.type.PresenceType, String)
     */
    public void logIn(PresenceType presenceType, String verboseStatus) {
        setPendingPresenceAndStatus(presenceType, verboseStatus);
        if (!isLoggedIn()) {
            loginConn = new LoginConnection(new ConnDescriptor(
                    JiveGlobals.getProperty(propertyPrefix+".connecthost", "login.oscar.aol.com"),
                    JiveGlobals.getIntProperty(propertyPrefix+".connectport", 5190)),
                    this);
            loginConn.connect();
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#logOut()
     */
    public synchronized void logOut() {
        cleanUp();
        sessionDisconnectedNoReconnect(null);
    }

    /**
     * @see net.sf.kraken.session.TransportSession#cleanUp()
     */
    public synchronized void cleanUp() {
        if (loginConn != null) {
            try {
                loginConn.stopKeepAlive();
            }
            catch (Exception e) {
                // Ignore.
            }
            try {
                loginConn.disconnect();
            }
            catch (Exception e) {
                // Ignore.
            }
            loginConn = null;
        }
        if (bosConn != null) {
            try {
                bosConn.stopKeepAlive();
            }
            catch (Exception e) {
                // Ignore.
            }
            try {
                bosConn.disconnect();
            }
            catch (Exception e) {
                // Ignore.
            }
            bosConn = null;
        }
        for (ServiceConnection conn : getServiceConnections()) {
            try {
                conn.stopKeepAlive();
            }
            catch (Exception e) {
                // Ignore.
            }
            try {
                conn.disconnect();
            }
            catch (Exception e) {
                // Ignore.
            }
            try {
                services.remove(conn);
            }
            catch (Exception e) {
                // Ignore.
            }
            try {
                snacMgr.unregister(conn);
            }
            catch (Exception e) {
                // Ignore.
            }
        }
    }

    /**
     * Finds the id number of a group specified or creates a new one and returns that id.
     *
     * @param groupName Name of the group we are looking for.
     * @return Id number of the group.
     */
    public Integer getGroupIdOrCreateNew(String groupName) {
        if (groupName.matches("/^\\s*$/")) { return 0; } // Special master group handling
        for (GroupItem g : groups.values()) {
            if (groupName.equalsIgnoreCase(g.getGroupName())) {
                return g.getId();
            }
        }

        // Group doesn't exist, lets create a new one.
        Integer newGroupId = getNextBuddyId(0);
        GroupItem newGroup = new GroupItem(groupName, newGroupId);
        request(new CreateItemsCmd(newGroup.toSsiItem()));
        gotGroup(newGroup);

        return newGroupId;
    }

    /**
     * Determines if a contact is a member of a group.
     *
     * @param groupId ID of group to check
     * @param member Screen name of member
     * @return True or false if member is in group with id groupId
     */
    public boolean isMemberOfGroup(Integer groupId, String member) {
        for (TransportBuddy buddy : getBuddyManager().getBuddies()) {
            if (buddy.getName().equalsIgnoreCase(member) && ((OSCARBuddy)buddy).getBuddyItem(groupId) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the next highest buddy ID for a group and stores the new highest id.
     *
     * @param groupId ID of group to get highest of
     * @return ID number of buddy ID you should use.
     */
    public Integer getNextBuddyId(Integer groupId) {
        Integer id = highestBuddyIdPerGroup.get(groupId) + 1;
        highestBuddyIdPerGroup.put(groupId, id);
        return id;
    }

    /**
     * Synchronizes the list of groups a contact is a member of, updating nicknames in
     * the process.
     *
     * @param contact Screen name/UIN of the contact.
     * @param nickname Nickname of the contact (should not be null)
     * @param grouplist List of groups the contact should be a member of.
     */
    public void syncContactGroupsAndNickname(String contact, String nickname, List<String> grouplist) {
        if (grouplist == null) {
            grouplist = new ArrayList<String>();
        }
        if (grouplist.isEmpty()) {
            if (getTransport().getType().equals(TransportType.icq)) {
                grouplist.add(DEFAULT_ICQ_GROUP);
            }
            else {
                grouplist.add(DEFAULT_AIM_GROUP);
            }
        }
        Log.debug("contact = "+contact+", nickname = "+nickname+", grouplist = "+grouplist);
        OSCARBuddy oscarBuddy = null;
        try {
            oscarBuddy = (OSCARBuddy)getBuddyManager().getBuddy(getTransport().convertIDToJID(contact));
            Log.debug("found related oscarbuddy");
        }
        catch (NotFoundException e) {
//            Log.error("OSCAR: Serious problem (not found in list) while syncing buddy "+contact);
//            return;
        }

        //TODO: Should we do a premodcmd here and postmodcmd at the end and not have separate ones?

        // Stored 'removed' list of buddy items for later use
        ArrayList<BuddyItem> freeBuddyItems = new ArrayList<BuddyItem>();
        
        // First, lets clean up any groups this contact should no longer be a member of.
        // We'll keep the buddy items around for potential modification instead of deletion.
        if (oscarBuddy != null) {
            for (BuddyItem buddy : oscarBuddy.getBuddyItems()) {
//                if (buddy.getScreenname().equalsIgnoreCase(contact)) {
                    if (buddy.getGroupId() == 0 && !grouplist.contains(DEFAULT_AIM_GROUP)) {
                        // Ok this group is the "main group", but contact isn't in it.
                        Log.debug("Removing "+buddy+" from main group");
                        freeBuddyItems.add(buddy);
//                        request(new DeleteItemsCmd(buddy.toSsiItem()));
//                        oscarBuddy.removeBuddyItem(buddy.getGroupId(), true);
                    }
                    else if (!groups.containsKey(buddy.getGroupId())) {
                        // Well this is odd, a group we don't know about?  Nuke it.
                        Log.debug("Removing "+buddy+" because of unknown group");
                        freeBuddyItems.add(buddy);
//                        request(new DeleteItemsCmd(buddy.toSsiItem()));
//                        oscarBuddy.removeBuddyItem(buddy.getGroupId(), true);
                    }
                    else if (!grouplist.contains(groups.get(buddy.getGroupId()).getGroupName())) {
                        Log.debug("Removing "+buddy+" because not in list of groups");
                        freeBuddyItems.add(buddy);
//                        request(new DeleteItemsCmd(buddy.toSsiItem()));
//                        oscarBuddy.removeBuddyItem(buddy.getGroupId(), true);
                    }
                    else {
                        if (buddy.getAlias() == null || !buddy.getAlias().equals(nickname)) {
                            Log.debug("Updating alias for "+buddy);
                            buddy.setAlias(nickname);
                            request(new PreModCmd());
                            request(new ModifyItemsCmd(buddy.toSsiItem()));
                            request(new PostModCmd());
                            oscarBuddy.tieBuddyItem(buddy, true);
                        }
                    }
//                }
            }
        }
        // Now, lets take the known good list of groups and add whatever is missing on the server.
        for (String group : grouplist) {
            Integer groupId = getGroupIdOrCreateNew(group);
            if (isMemberOfGroup(groupId, contact)) {
                // Already a member, moving on
                continue;
            }

            Integer newBuddyId = 1;
            if (highestBuddyIdPerGroup.containsKey(groupId)) {
                newBuddyId = getNextBuddyId(groupId);
            }

            if (freeBuddyItems.size() > 0) {
                // Moving a freed buddy item
                // TODO: This isn't working.. why?  Returns RESULT_ID_TAKEN
//                BuddyItem buddy = freeBuddyItems.remove(0);
//                if (oscarBuddy != null) {
//                    oscarBuddy.removeBuddyItem(buddy.getGroupId(), false);
//                }
//                buddy.setGroupid(groupId);
//                buddy.setId(newBuddyId);
//                buddy.setAlias(nickname);
//                request(new ModifyItemsCmd(buddy.toSsiItem()));
//                if (oscarBuddy == null) {
//                    oscarBuddy = new OSCARBuddy(getBuddyManager(), buddy);
//                    // TODO: translate this
//                    request(new BuddyAuthRequest(contact, "Automated add request on behalf of user."));
//                }
//                else {
//                    oscarBuddy.tieBuddyItem(buddy, false);
//                }
                request(new PreModCmd());
                BuddyItem buddy = freeBuddyItems.remove(0);
                BuddyItem newBuddy = new BuddyItem(buddy);
                newBuddy.setGroupid(groupId);
                newBuddy.setId(newBuddyId);
                newBuddy.setAlias(nickname);
                request(new DeleteItemsCmd(buddy.toSsiItem()));
                if (oscarBuddy != null) {
                    oscarBuddy.removeBuddyItem(buddy.getGroupId(), false);
                }
                request(new CreateItemsCmd(newBuddy.toSsiItem()));
                if (oscarBuddy == null) {
                    oscarBuddy = new OSCARBuddy(getBuddyManager(), newBuddy);
                    // TODO: translate this
                    request(new BuddyAuthRequest(contact, "Automated add request on behalf of user."));
                }
                else {
                    oscarBuddy.tieBuddyItem(newBuddy, false);
                }
                request(new PostModCmd());
            }
            else {
                // Creating a new buddy item
                BuddyItem newBuddy = new BuddyItem(contact, groupId, newBuddyId);
                newBuddy.setAlias(nickname);
                //  TODO: Should we be doing this for AIM too?
                if (getTransport().getType().equals(TransportType.icq)) {
                    newBuddy.setAwaitingAuth(true);
                }
                request(new PreModCmd());
                request(new CreateItemsCmd(newBuddy.toSsiItem()));
                request(new PostModCmd());
                if (oscarBuddy == null) {
                    oscarBuddy = new OSCARBuddy(getBuddyManager(), newBuddy);
                    // TODO: translate this
                    request(new BuddyAuthRequest(contact, "Automated add request on behalf of user."));
                }
                else {
                    oscarBuddy.tieBuddyItem(newBuddy, true);
                }
            }
        }
        // Now, lets remove any leftover buddy items that we're no longer using.
        for (BuddyItem buddy : freeBuddyItems) {
            request(new DeleteItemsCmd(buddy.toSsiItem()));
            if (oscarBuddy != null) {
                oscarBuddy.removeBuddyItem(buddy.getGroupId(), false);
            }
        }
        // Lastly, lets store the final buddy item after we've modified it, making sure to update groups first.
        if (oscarBuddy != null) {
//            oscarBuddy.populateGroupList();
            getBuddyManager().storeBuddy(oscarBuddy);
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#addContact(org.xmpp.packet.JID, String, java.util.ArrayList)
     */
    public void addContact(JID jid, String nickname, ArrayList<String> groups) {
        String legacyId = getTransport().convertJIDToID(jid);
        if (nickname == null || nickname.equals("")) {
            nickname = legacyId;
        }

        if (getTransport().getType().equals(TransportType.icq)) {
            request(new AuthFutureCmd(legacyId, null));
        }

        // Syncing takes care of all the dirty work.
        syncContactGroupsAndNickname(legacyId, nickname, groups);
    }

    /**
     * @see net.sf.kraken.session.TransportSession#removeContact(net.sf.kraken.roster.TransportBuddy)
     */
    public void removeContact(TransportBuddy contact) {
        String legacyId = getTransport().convertJIDToID(contact.getJID());
        OSCARBuddy oscarBuddy = (OSCARBuddy)contact;
        for (BuddyItem i : oscarBuddy.getBuddyItems()) {
            if (i.getScreenname().equalsIgnoreCase(legacyId)) {
                request(new DeleteItemsCmd(i.toSsiItem()));
            }
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#updateContact(net.sf.kraken.roster.TransportBuddy)
     */
    public void updateContact(TransportBuddy contact) {
        String legacyId = getTransport().convertJIDToID(contact.getJID());
        String nickname = contact.getNickname();
        if (nickname == null || nickname.equals("")) {
            nickname = legacyId;
        }

        // Syncing takes care of all of the dirty work.
        syncContactGroupsAndNickname(legacyId, nickname, (List<String>)contact.getGroups());
    }

    /**
     * @see net.sf.kraken.session.TransportSession#sendMessage(org.xmpp.packet.JID, String)
     */
    public void sendMessage(JID jid, String message) {
        SendImIcbm icbm = new SendImIcbm(getTransport().convertJIDToID(jid), message);
        // TODO: Should we consider checking to see if they really are offline?
        if (getTransport().getType().equals(TransportType.icq)) {
            icbm.setOffline(true);
        }
        request(icbm);
    }

    /**
     * @see net.sf.kraken.session.TransportSession#sendChatState(org.xmpp.packet.JID,net.sf.kraken.type.ChatStateType)
     */
    public void sendChatState(JID jid, ChatStateType chatState) {
        if (chatState.equals(ChatStateType.composing)) {
            request(new SendTypingNotification(
                    getTransport().convertJIDToID(jid),
                    SendTypingNotification.STATE_TYPING
            ));
        }
        else if (chatState.equals(ChatStateType.paused)) {
            request(new SendTypingNotification(
                    getTransport().convertJIDToID(jid),
                    SendTypingNotification.STATE_PAUSED
            ));
        }
        else if (chatState.equals(ChatStateType.inactive)) {
            request(new SendTypingNotification(
                    getTransport().convertJIDToID(jid),
                    SendTypingNotification.STATE_NO_TEXT
            ));
        }
    }
    
    /**
     * @see net.sf.kraken.session.TransportSession#sendBuzzNotification(org.xmpp.packet.JID, String)
     */
    public void sendBuzzNotification(JID jid, String message) {
    }

    /**
     * @see net.sf.kraken.session.TransportSession#updateLegacyAvatar(String, byte[])
     */
    public void updateLegacyAvatar(String type, byte[] data) {
//        if (!type.equals("image/jpeg") && !type.equals("image/gif")) {
//            try {
//                BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
//                ByteArrayOutputStream tmpImg = new ByteArrayOutputStream();
//                ImageIO.write(image, "gif", tmpImg);
//                data = tmpImg.toByteArray();
//            }
//            catch (IOException e) {
//                // Hrm, no conversion.
//                Log.debug("OSCAR: Unable to convert image: ", e);
//            }
//        }
//        ImageInfo imageInfo = new ImageInfo();
//        imageInfo.setInput(new ByteArrayInputStream(data));
//        Log.debug("updateLegacyAvatar: Arg1: "+type+"   Detected: "+imageInfo.getMimeType());

        pendingAvatar = data;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);

            if (storedIconInfo != null) {
                IconItem newIconItem = new IconItem(storedIconInfo);
                newIconItem.setIconInfo(new ExtraInfoData(ExtraInfoData.FLAG_HASH_PRESENT, ByteBlock.wrap(md.digest())));
                storedIconInfo = newIconItem;
                request(new PreModCmd());
                request(new ModifyItemsCmd(storedIconInfo.toSsiItem()));
                request(new PostModCmd());
            }
            else {
                storedIconInfo = new IconItem(IconItem.NAME_DEFAULT, this.getNextBuddyId(0),
                        new ExtraInfoData(ExtraInfoData.FLAG_HASH_PRESENT, ByteBlock.wrap(md.digest()))
                );
                request(new PreModCmd());
                request(new CreateItemsCmd(storedIconInfo.toSsiItem()));
                request(new PostModCmd());
            }
        }
        catch (NoSuchAlgorithmException e) {
            Log.error("No algorithm found for MD5 checksum??");
        }
    }

    /**
     * Opens/creates a new BOS connection to a specific server and port, given a cookie.
     *
     * @param server Server to connect to.
     * @param port Port to connect to.
     * @param cookie Auth cookie.
     */
    void startBosConn(String server, int port, ByteBlock cookie) {
        bosConn = new BOSConnection(new ConnDescriptor(server, port), this, cookie);
        bosConn.connect();
    }

    /**
     * Registers the set of SNAC families that the given connection supports.
     *
     * @param conn FLAP connection to be registered.
     */
    void registerSnacFamilies(BasicFlapConnection conn) {
        snacMgr.register(conn);
    }

    protected SnacManager snacMgr = new SnacManager(new PendingSnacListener() {
        public void dequeueSnacs(List<SnacRequest> pending) {
            for (SnacRequest request : pending) {
                handleRequest(request);
            }
        }
    });

    synchronized void handleRequest(SnacRequest request) {
        Log.debug("Handling request "+request);
        int family = request.getCommand().getFamily();
        if (snacMgr.isPending(family)) {
            snacMgr.addRequest(request);
            return;
        }

        BasicFlapConnection conn = snacMgr.getConn(family);

        if (conn != null) {
            conn.sendRequest(request);
        }
        else {
            // it's time to request a service
            if (!(request.getCommand() instanceof ServiceRequest)) {
                snacMgr.setPending(family, true);
                snacMgr.addRequest(request);
                request(new ServiceRequest(family));
            } else {
                // TODO: Why does this occur a lot and yet not cause problems?
                Log.debug("eep! can't find a service redirector server.");
            }
        }
    }

    SnacRequest request(SnacCommand cmd) {
        Log.debug("Sending SNAC command: "+cmd);
        return request(cmd, null);
    }

    private SnacRequest request(SnacCommand cmd, SnacRequestListener listener) {
        Log.debug("Setting up SNAC request and listener: "+cmd+","+listener);
        SnacRequest req = new SnacRequest(cmd, listener);
        handleRequest(req);
        return req;
    }

    void connectToService(int snacFamily, String host, ByteBlock cookie) {
        Log.debug("Connection to service "+snacFamily+" on host "+host);
        ServiceConnection conn;
        if (snacFamily == MailCheckCmd.FAMILY_MAILCHECK) {
            conn = new EmailConnection(new ConnDescriptor(host,
                    JiveGlobals.getIntProperty(propertyPrefix+".connectport", 5190)),
                    this,
                    cookie,
                    snacFamily);
        }
        else {
            conn = new ServiceConnection(new ConnDescriptor(host,
                    JiveGlobals.getIntProperty(propertyPrefix+".connectport", 5190)),
                    this,
                    cookie,
                    snacFamily);
        }

        conn.connect();
    }

    void serviceFailed(ServiceConnection conn) {
        Log.debug("OSCAR service failed: "+conn.toString());
    }

    void serviceConnected(ServiceConnection conn) {
        Log.debug("OSCAR service connected: "+conn.toString());
        services.add(conn);
    }

    public boolean isServiceConnected(ServiceConnection conn) {
        return services.contains(conn);
    }

    public Set<ServiceConnection> getServiceConnections() {
        return services;
    }

    void serviceReady(ServiceConnection conn) {
        Log.debug("OSCAR service ready: "+conn.toString());
        snacMgr.dequeueSnacs(conn);
    }

    void serviceDied(ServiceConnection conn) {
        Log.debug("OSCAR service died: "+conn.toString());
        services.remove(conn);
        snacMgr.unregister(conn);
    }

    /**
     * We've been told about a group that exists on the buddy list.
     *
     * @param group The group we've been told about.
     */
    void gotGroup(GroupItem group) {
        Log.debug("Found group item: " + group.toString() + " at id " + group.getId());
        groups.put(group.getId(), group);
        if (!highestBuddyIdPerGroup.containsKey(0)) {
            highestBuddyIdPerGroup.put(0, 0);
        }
        if (group.getId() > highestBuddyIdPerGroup.get(0)) {
            highestBuddyIdPerGroup.put(0, group.getId());
        }
    }

    /**
     * We've been told about an icon that exists on the buddy list.
     *
     * @param iconitem The icon info we've been told about.
     */
    void gotIconItem(IconItem iconitem) {
//        if (iconitem.getName().equals("1")) {
            // We only care about the one with the name "1" ... don't ask.
            this.storedIconInfo = iconitem;
//        }
    }

    /**
     * We've been told about a visibility item that exists on the buddy list.
     *
     * @param visibilityitem The visibility info we've been told about.
     */
    void gotVisibilityItem(VisibilityItem visibilityitem) {
        this.storedVisibilityInfo = visibilityitem;
    }

    /**
     * Update highest buddy id in a group if new id is indeed higher.
     *
     * @param buddyItem Buddy item to compare.
     */
    void updateHighestId(BuddyItem buddyItem) {
        if (!highestBuddyIdPerGroup.containsKey(buddyItem.getGroupId())) {
            highestBuddyIdPerGroup.put(buddyItem.getGroupId(), 0);
        }
        if (buddyItem.getId() > highestBuddyIdPerGroup.get(buddyItem.getGroupId())) {
            highestBuddyIdPerGroup.put(buddyItem.getGroupId(), buddyItem.getId());
        }
    }

    /**
     * Apparantly we now have the entire list, lets sync.
     */
    void gotCompleteSSI() {
        ArrayList<Integer> nicknameRequests = new ArrayList<Integer>();

        for (TransportBuddy buddy : getBuddyManager().getBuddies()) {
            OSCARBuddy oscarBuddy = (OSCARBuddy)buddy;
            String nickname = buddy.getNickname();

            oscarBuddy.populateGroupList();

            for (BuddyItem buddyItem : oscarBuddy.getBuddyItems()) {
                if (buddyItem.isAwaitingAuth()) {
                    buddy.setAskType(RosterItem.ASK_SUBSCRIBE);
                    buddy.setSubType(RosterItem.SUB_NONE);
                }
                try {
                    if (nickname.equalsIgnoreCase(buddyItem.getScreenname())) {
                        Integer buddyUIN = Integer.parseInt(buddyItem.getScreenname());
                        Log.debug("REQUESTING SHORT INFO FOR "+buddyUIN);
                        nicknameRequests.add(buddyUIN);
                    }
                }
                catch (NumberFormatException e) {
                    // Not an ICQ number then  ;D
                }
            }
        }

        try {
            getTransport().syncLegacyRoster(getJID(), getBuddyManager().getBuddies());
        }
        catch (UserNotFoundException e) {
            Log.debug("Unable to sync oscar contact list for " + getJID(), e);
        }

        getBuddyManager().activate();

        request(new SetInfoCmd(InfoData.forCapabilities(getCapabilities())));

//        if (JiveGlobals.getBooleanProperty("plugin.gateway."+getTransport().getType()+".avatars", true) && getAvatar() != null) {
//            if (storedIconInfo == null || !StringUtils.encodeHex(storedIconInfo.getIconInfo().getData().toByteArray()).equals(getAvatar().getLegacyIdentifier())) {
//                try {
//                    updateLegacyAvatar(getAvatar().getMimeType(), Base64.decode(getAvatar().getImageData()));
//                }
//                catch (NotFoundException e) {
//                    // No avatar found, moving on
//                }
//            }
//        }

        updateStatus(getPresence(), getVerboseStatus());

        if (storedVisibilityInfo != null) {
            if ((storedVisibilityInfo.getVisFlags() & VisibilityItem.MASK_DISABLE_RECENT_BUDDIES) == 0) {
                storedVisibilityInfo.setVisFlags((storedVisibilityInfo.getVisFlags() | VisibilityItem.MASK_DISABLE_RECENT_BUDDIES));
                request(new ModifyItemsCmd(storedVisibilityInfo.toSsiItem()));
            }
        }
        else {
            storedVisibilityInfo = new VisibilityItem(getNextBuddyId(SsiItem.GROUP_ROOT), SsiItem.GROUP_ROOT);
            storedVisibilityInfo.setVisFlags(VisibilityItem.MASK_DISABLE_RECENT_BUDDIES);
            request(new CreateItemsCmd(storedVisibilityInfo.toSsiItem()));
        }

        if (getTransport().getType().equals(TransportType.icq)) {
            request(new OfflineMsgIcqRequest(getUIN(), (int)nextIcqId()));
        }

        if (JiveGlobals.getBooleanProperty("plugin.gateway."+getTransport().getType()+".mailnotifications", true)) {
            request(new ServiceRequest(MailCheckCmd.FAMILY_MAILCHECK));
        }


        for (Integer uin : nicknameRequests) {
            MetaShortInfoRequest req = new MetaShortInfoRequest(getUIN(), (int)nextIcqId(), uin);
            Log.debug("Doing a MetaShortInfoRequest for "+uin+" as "+req);
            request(req);
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#updateStatus(net.sf.kraken.type.PresenceType, String)
     */
    public void updateStatus(PresenceType presenceType, String verboseStatus) {
        if (getTransport().getType().equals(TransportType.icq)) {
            request(new SetExtraInfoCmd(((OSCARTransport)getTransport()).convertXMPPStatusToICQ(presenceType)));
        }
        if (presenceType != PresenceType.available && presenceType != PresenceType.chat) {
            String awayMsg = LocaleUtils.getLocalizedString("gateway.oscar.away", "kraken");
            if (verboseStatus != null && verboseStatus.length() > 0) {
                awayMsg = verboseStatus;
            }
            request(new SetInfoCmd(InfoData.forAwayMessage(awayMsg)));
            if (!getTransport().getType().equals(TransportType.icq)) {
                presenceType = PresenceType.away;
            }
        }
        else {
            request(new SetInfoCmd(InfoData.forAwayMessage(InfoData.NOT_AWAY)));
            request(new SetExtraInfoCmd(new ExtraInfoBlock(ExtraInfoBlock.TYPE_AVAILMSG, ExtraInfoData.getAvailableMessageBlock(verboseStatus == null ? "" : verboseStatus))));
        }
        setPresenceAndStatus(presenceType, verboseStatus);
    }

    /**
     * Retrieves the next ICQ id number and increments the counter.
     * @return The next ICQ id number.
     */
    public long nextIcqId() { return icqSeqNum.next(); }

    /**
     * Retrieves a UIN in integer format for the session.
     *
     * @return The UIN in integer format.
     */
    public int getUIN() {
        try {
            return Integer.parseInt(getRegistration().getUsername());
        }
        catch (Exception e) {
            return -1;
        }
    }

    /**
     * Updates roster nickname information about a contact.
     *
     * @param sn Screenname/UIN of contact
     * @param nickname New nickname
     */
    public void updateRosterNickname(String sn, String nickname) {
        try {
            TransportBuddy buddy = getBuddyManager().getBuddy(getTransport().convertIDToJID(sn));
            buddy.setNickname(nickname);
            try {
                getTransport().addOrUpdateRosterItem(getJID(), buddy.getName(), buddy.getNickname(), buddy.getGroups());
            }
            catch (UserNotFoundException e) {
                // Can't update something that's not really in our list.
            }
        }
        catch (NotFoundException e) {
            // Can't update something that's not really in our list.
        }
    }

}
