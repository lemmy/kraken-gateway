/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2006-2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 *
 * Heavily inspired by joscardemo of the Joust Project: http://joust.kano.net/
 */

package org.jivesoftware.openfire.gateway.protocols.oscar;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.flap.FlapCommand;
import net.kano.joscar.flap.FlapPacketEvent;
import net.kano.joscar.flapcmd.LoginFlapCmd;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.net.ConnDescriptor;
import net.kano.joscar.ratelim.RateLimitingQueueMgr;
import net.kano.joscar.snac.*;
import net.kano.joscar.snaccmd.*;
import net.kano.joscar.snaccmd.buddy.BuddyOfflineCmd;
import net.kano.joscar.snaccmd.buddy.BuddyStatusCmd;
import net.kano.joscar.snaccmd.conn.*;
import net.kano.joscar.snaccmd.error.SnacError;
import net.kano.joscar.snaccmd.icbm.InstantMessage;
import net.kano.joscar.snaccmd.icbm.OldIcbm;
import net.kano.joscar.snaccmd.icbm.RecvImIcbm;
import net.kano.joscar.snaccmd.icbm.TypingCmd;
import net.kano.joscar.snaccmd.icon.IconDataCmd;
import net.kano.joscar.snaccmd.icon.IconRequest;
import net.kano.joscar.snaccmd.icon.UploadIconAck;
import net.kano.joscar.snaccmd.icon.UploadIconCmd;
import org.apache.log4j.Logger;
import org.jivesoftware.openfire.gateway.avatars.Avatar;
import org.jivesoftware.openfire.gateway.type.PresenceType;
import org.jivesoftware.openfire.gateway.type.TransportType;
import org.jivesoftware.openfire.gateway.util.StringUtils;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.NotFoundException;
import org.xmpp.packet.Message;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Handles incoming FLAP packets.
 *
 * @author Daniel Henninger
 * Heavily inspired by joscardemo from the joscar project.
 */
public abstract class BasicFlapConnection extends AbstractFlapConnection {

    static Logger Log = Logger.getLogger(BasicFlapConnection.class);

    protected final ByteBlock cookie;
    protected boolean sentClientReady = false;

    protected int[] snacFamilies = null;
    protected Collection<SnacFamilyInfo> snacFamilyInfos;
    protected RateLimitingQueueMgr rateMgr = new RateLimitingQueueMgr();

    public BasicFlapConnection(ConnDescriptor cd, OSCARSession mainSession, ByteBlock cookie) {
        super(cd, mainSession);
        this.cookie = cookie;
        initBasicFlapConnection();
    }

    private void initBasicFlapConnection() {
        sp.setSnacQueueManager(rateMgr);
    }

    protected DateFormat dateFormat
            = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                    DateFormat.SHORT);

    protected void handleFlapPacket(FlapPacketEvent e) {
        Log.debug("OSCAR flap packet received: "+e);
        FlapCommand cmd = e.getFlapCommand();

        if (cmd instanceof LoginFlapCmd) {
            getFlapProcessor().sendFlap(new LoginFlapCmd(cookie));
        }
    }

    protected void handleSnacPacket(SnacPacketEvent e) {
        Log.debug("OSCAR snac packet received: "+e);
        SnacCommand cmd = e.getSnacCommand();
        if (cmd instanceof ServerReadyCmd) {
            ServerReadyCmd src = (ServerReadyCmd) cmd;
            setSnacFamilies(src.getSnacFamilies());

            Collection<SnacFamilyInfo> familyInfos = SnacFamilyInfoFactory.getDefaultFamilyInfos(src.getSnacFamilies());
            setSnacFamilyInfos(familyInfos);

            getMainSession().registerSnacFamilies(this);

            request(new ClientVersionsCmd(familyInfos));
            request(new RateInfoRequest());
        }
        else if (cmd instanceof RecvImIcbm) {
            RecvImIcbm icbm = (RecvImIcbm) cmd;

            String sn = icbm.getSenderInfo().getScreenname();
            InstantMessage message = icbm.getMessage();
            String msg = StringUtils.convertFromHtml(message.getMessage());

            getMainSession().getTransport().sendMessage(
                    getMainSession().getJID(),
                    getMainSession().getTransport().convertIDToJID(sn),
                    msg
            );
        }
        else if (cmd instanceof OldIcbm) {
            OldIcbm oicbm = (OldIcbm) cmd;
            if (oicbm.getMessageType() == OldIcbm.MTYPE_PLAIN) {
                String uin = String.valueOf(oicbm.getSender());
                String msg = StringUtils.convertFromHtml(oicbm.getReason());
                Log.debug("Got ICBM message "+uin+" with "+msg+"\n"+oicbm);
//                InstantMessage message = oicbm.getMessage();
//                Log.debug("Got ICBM message "+uin+" with "+message+"\n"+oicbm);
//                String msg = StringUtils.unescapeFromXML(OscarTools.stripHtml(message.getMessage()));

                getMainSession().getTransport().sendMessage(
                        getMainSession().getJID(),
                        getMainSession().getTransport().convertIDToJID(uin),
                        msg
                );
            }
        }
        else if (cmd instanceof WarningNotification) {
            WarningNotification wn = (WarningNotification) cmd;
            MiniUserInfo warner = wn.getWarner();
            if (warner == null) {
                getMainSession().getTransport().sendMessage(
                        getMainSession().getJID(),
                        getMainSession().getTransport().getJID(),
                        LocaleUtils.getLocalizedString("gateway.aim.warninganon", "gateway", Arrays.asList(wn.getNewLevel().toString())),
                        Message.Type.headline
                );
            }
            else {
                Log.debug("*** " + warner.getScreenname()
                        + " warned you up to " + wn.getNewLevel() + "%");
                getMainSession().getTransport().sendMessage(
                        getMainSession().getJID(),
                        getMainSession().getTransport().getJID(),
                        LocaleUtils.getLocalizedString("gateway.aim.warningdirect", "gateway", Arrays.asList(warner.getScreenname(), wn.getNewLevel().toString())),
                        Message.Type.headline
                );
            }
        }
        else if (cmd instanceof ExtraInfoAck) {
            ExtraInfoAck eia = (ExtraInfoAck)cmd;
            List<ExtraInfoBlock> extraInfo = eia.getExtraInfos();
            if (extraInfo != null) {
                for (ExtraInfoBlock i : extraInfo) {
                    ExtraInfoData data = i.getExtraData();

                    if (JiveGlobals.getBooleanProperty("plugin.gateway."+getMainSession().getTransport().getType()+".avatars", true) && (data.getFlags() & ExtraInfoData.FLAG_UPLOAD_ICON) != 0 && getMainSession().pendingAvatar != null) {
                        Log.debug("OSCAR: Server has indicated that it wants our icon.");
                        request(new UploadIconCmd(ByteBlock.wrap(getMainSession().pendingAvatar)), new SnacRequestAdapter() {
                            public void handleResponse(SnacResponseEvent e) {
                                SnacCommand cmd = e.getSnacCommand();
                                if (cmd instanceof UploadIconAck && getMainSession().pendingAvatar != null) {
                                    UploadIconAck iconAck = (UploadIconAck) cmd;
                                    if (iconAck.getCode() == UploadIconAck.CODE_DEFAULT || iconAck.getCode() == UploadIconAck.CODE_SUCCESS) {
                                        ExtraInfoBlock iconInfo = iconAck.getIconInfo();
                                        if (iconInfo == null) {
                                          Log.debug("OSCAR: Got icon ack with no iconInfo: " + iconAck);
                                        }
                                        Log.debug("OSCAR: Successfully set icon.");
                                        try {
                                            MessageDigest md = MessageDigest.getInstance("MD5");
                                            md.update(getMainSession().pendingAvatar);
                                            getMainSession().getAvatar().setLegacyIdentifier(org.jivesoftware.util.StringUtils.encodeHex(md.digest()));
                                        }
                                        catch (NoSuchAlgorithmException ee) {
                                            Log.error("No algorithm found for MD5!", ee);
                                        }
                                    }
                                    else if (iconAck.getCode() == UploadIconAck.CODE_BAD_FORMAT) {
                                        Log.debug("OSCAR: Uploaded icon was not in an unaccepted format.");
                                    }
                                    else if (iconAck.getCode() == UploadIconAck.CODE_TOO_LARGE) {
                                        Log.debug("OSCAR: Uploaded icon was too large to be accepted.");
                                    }
                                    else {
                                        Log.debug("OSCAR: Got unknown code from UploadIconAck: " + iconAck.getCode());
                                    }
                                    getMainSession().pendingAvatar = null;
                                }
                                else if (cmd instanceof SnacError) {
                                    Log.debug("Got SnacError while setting icon: " + cmd);
                                }
                            }
                        });
                    }
                }
            }
        }
        else if (cmd instanceof BuddyStatusCmd) {
            BuddyStatusCmd bsc = (BuddyStatusCmd)cmd;
            FullUserInfo info = bsc.getUserInfo();
            PresenceType pType = PresenceType.available;
            String vStatus = "";
            if (info.getAwayStatus()) {
                pType = PresenceType.away;
            }

            if (getMainSession().getTransport().getType().equals(TransportType.icq) && info.getScreenname().matches("/^\\d+$/")) {
                pType = ((OSCARTransport)getMainSession().getTransport()).convertICQStatusToXMPP(info.getIcqStatus());
            }

            List<ExtraInfoBlock> extraInfo = info.getExtraInfoBlocks();
            if (extraInfo != null) {
                for (ExtraInfoBlock i : extraInfo) {
                    ExtraInfoData data = i.getExtraData();

                    if (i.getType() == ExtraInfoBlock.TYPE_AVAILMSG) {
                        ByteBlock msgBlock = data.getData();
                        int len = BinaryTools.getUShort(msgBlock, 0);
                        if (len >= 0) {
                            byte[] msgBytes = msgBlock.subBlock(2, len).toByteArray();
                            String msg;
                            try {
                                msg = new String(msgBytes, "UTF-8");
                            }
                            catch (UnsupportedEncodingException e1) {
                                continue;
                            }
                            if (msg.length() > 0) {
                                vStatus = msg;
                            }
                        }
                    }
                    else if (i.getType() == ExtraInfoBlock.TYPE_ICONHASH && JiveGlobals.getBooleanProperty("plugin.gateway."+getMainSession().getTransport().getType()+".avatars", true)) {
                        try {
                            OSCARBuddy oscarBuddy = (OSCARBuddy)getMainSession().getBuddyManager().getBuddy(getMainSession().getTransport().convertIDToJID(info.getScreenname()));
                            Avatar curAvatar = oscarBuddy.getAvatar();
                            if (curAvatar == null || !curAvatar.getLegacyIdentifier().equals(org.jivesoftware.util.StringUtils.encodeHex(i.getExtraData().getData().toByteArray()))) {
                                IconRequest req = new IconRequest(info.getScreenname(), i.getExtraData());
                                request(req, new SnacRequestAdapter() {
                                    public void handleResponse(SnacResponseEvent e) {
                                        SnacCommand cmd = e.getSnacCommand();
                                        if (cmd instanceof IconDataCmd) {
                                            IconDataCmd idc = (IconDataCmd)cmd;
                                            if (idc.getIconData().getLength() > 0 && idc.getIconData().getLength() != 90) {
                                                Log.debug("Got icon data: "+idc);
                                                if (getMainSession().getBuddyManager().isActivated()) {
                                                    try {
                                                        OSCARBuddy oscarBuddy = (OSCARBuddy)getMainSession().getBuddyManager().getBuddy(getMainSession().getTransport().convertIDToJID(idc.getScreenname()));
                                                        oscarBuddy.setAvatar(new Avatar(getMainSession().getTransport().convertIDToJID(idc.getScreenname()), org.jivesoftware.util.StringUtils.encodeHex(idc.getIconInfo().getExtraData().getData().toByteArray()), idc.getIconData().toByteArray()));
                                                    }
                                                    catch (NotFoundException ee) {
                                                        // Apparently we don't care about this contact.
                                                    }
                                                    catch (IllegalArgumentException ee) {
                                                        Log.debug("OSCAR: Got null avatar, ignoring.");
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    public void handleTimeout(SnacRequestTimeoutEvent e) {
                                        Log.debug("Time out while waiting for icon data.");
                                    }
                                });
                            }
                        }
                        catch (NotFoundException ee) {
                            // Apparently we don't care about this contact.
                        }
                    }
                }
            }
            if (getMainSession().getBuddyManager().isActivated()) {
                try {
                    OSCARBuddy oscarBuddy = (OSCARBuddy)getMainSession().getBuddyManager().getBuddy(getMainSession().getTransport().convertIDToJID(info.getScreenname()));
                    oscarBuddy.setPresenceAndStatus(pType, vStatus);
                }
                catch (NotFoundException ee) {
                    // Apparently we don't care about this contact.
                    Log.debug("OSCAR: Received presense notification for contact we don't care about: "+info.getScreenname());
                }
            }
            else {
                getMainSession().getBuddyManager().storePendingStatus(getMainSession().getTransport().convertIDToJID(info.getScreenname()), pType, vStatus);
            }
        }
        else if (cmd instanceof BuddyOfflineCmd) {
            BuddyOfflineCmd boc = (BuddyOfflineCmd)cmd;
            if (getMainSession().getBuddyManager().isActivated()) {
                try {
                    OSCARBuddy oscarBuddy = (OSCARBuddy)getMainSession().getBuddyManager().getBuddy(getMainSession().getTransport().convertIDToJID(boc.getScreenname()));
                    oscarBuddy.setPresence(PresenceType.unavailable);
                }
                catch (NotFoundException ee) {
                    // Apparently we don't care about this contact.
                }
            }
            else {
                getMainSession().getBuddyManager().storePendingStatus(getMainSession().getTransport().convertIDToJID(boc.getScreenname()), PresenceType.unavailable, null);
            }
        }
        else if (cmd instanceof TypingCmd) {
            TypingCmd tc = (TypingCmd) cmd;
            String sn = tc.getScreenname();

            if (tc.getTypingState() == TypingCmd.STATE_TYPING) {
                getMainSession().getTransport().sendComposingNotification(
                        getMainSession().getJID(),
                        getMainSession().getTransport().convertIDToJID(sn)
                );
            }
            else if (tc.getTypingState() == TypingCmd.STATE_PAUSED) {
                getMainSession().getTransport().sendComposingPausedNotification(
                        getMainSession().getJID(),
                        getMainSession().getTransport().convertIDToJID(sn)
                );
            }
            else if (tc.getTypingState() == TypingCmd.STATE_NO_TEXT) {
                getMainSession().getTransport().sendChatInactiveNotification(
                        getMainSession().getJID(),
                        getMainSession().getTransport().convertIDToJID(sn)
                );
            }
        }
    }

    protected void handleSnacResponse(SnacResponseEvent e) {
        Log.debug("OSCAR snac packet response: "+e);
        SnacCommand cmd = e.getSnacCommand();

        if (cmd instanceof RateInfoCmd) {
            RateInfoCmd ric = (RateInfoCmd) cmd;
            List <RateClassInfo> rateClasses = ric.getRateClassInfos();

            int[] classes = new int[rateClasses.size()];
            for (int i = 0; i < rateClasses.size(); i++) {
                classes[i] = rateClasses.get(i).getRateClass();
            }

            request(new RateAck(classes));
        }
    }

    public int[] getSnacFamilies() { return snacFamilies; }

    protected void setSnacFamilies(int[] families) {
        this.snacFamilies = families.clone();
        Arrays.sort(snacFamilies);
    }

    protected void setSnacFamilyInfos(Collection<SnacFamilyInfo> infos) {
        snacFamilyInfos = infos;
    }

    protected boolean supportsFamily(int family) {
        return Arrays.binarySearch(snacFamilies, family) >= 0;
    }

    protected void clientReady() {
        if (!sentClientReady) {
            sentClientReady = true;
            request(new ClientReadyCmd(snacFamilyInfos));
        }
    }

    protected SnacRequest dispatchRequest(SnacCommand cmd) {
        return dispatchRequest(cmd, null);
    }

    protected SnacRequest dispatchRequest(SnacCommand cmd,
            SnacRequestListener listener) {
        SnacRequest req = new SnacRequest(cmd, listener);
        dispatchRequest(req);
        return req;
    }

    protected void dispatchRequest(SnacRequest req) {
        getMainSession().handleRequest(req);
    }

    protected SnacRequest request(SnacCommand cmd,
            SnacRequestListener listener) {
        SnacRequest req = new SnacRequest(cmd, listener);

        handleReq(req);

        return req;
    }

    private void handleReq(SnacRequest request) {
        int family = request.getCommand().getFamily();
        if (snacFamilies == null || supportsFamily(family)) {
            // this connection supports this snac, so we'll send it here
            sendRequest(request);
        }
        else {
            getMainSession().handleRequest(request);
        }
    }

}
