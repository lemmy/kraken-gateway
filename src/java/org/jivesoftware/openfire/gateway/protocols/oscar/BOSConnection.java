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

import net.kano.joscar.*;
import net.kano.joscar.flap.*;
import net.kano.joscar.flapcmd.*;
import net.kano.joscar.net.*;
import net.kano.joscar.snac.*;
import net.kano.joscar.snaccmd.*;
import net.kano.joscar.snaccmd.error.SnacError;
import net.kano.joscar.snaccmd.icq.OfflineMsgIcqCmd;
import net.kano.joscar.snaccmd.icq.OfflineMsgDoneCmd;
import net.kano.joscar.snaccmd.icq.OfflineMsgIcqAckCmd;
import net.kano.joscar.snaccmd.icq.MetaShortInfoCmd;
import net.kano.joscar.snaccmd.conn.*;
import net.kano.joscar.snaccmd.icbm.*;
import net.kano.joscar.snaccmd.loc.*;
import net.kano.joscar.snaccmd.ssi.*;
import net.kano.joscar.ssiitem.*;

import java.util.List;
import java.util.Date;

import org.jivesoftware.util.StringUtils;
import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.NotFoundException;
import org.jivesoftware.openfire.gateway.type.TransportLoginStatus;
import org.xmpp.packet.Presence;
import org.apache.log4j.Logger;

/**
 * Handles BOS related packets.
 *
 * @author Daniel Henninger
 * Heavily inspired by joscardemo from the joscar project.
 */
public class BOSConnection extends BasicFlapConnection {

    static Logger Log = Logger.getLogger(BOSConnection.class);

    protected SsiItemObjectFactory itemFactory = new DefaultSsiItemObjFactory();

    public BOSConnection(ConnDescriptor cd, OSCARSession mainSession, ByteBlock cookie) {
        super(cd, mainSession, cookie); // Hand off to BasicFlapConnection
    }

    protected void clientReady() {
        super.clientReady();
        startKeepAlive();
    }

    protected void handleStateChange(ClientConnEvent e) {
        Log.debug("OSCAR bos service state change from "+e.getOldState()+" to "+e.getNewState()+" Reason: "+e.getReason());
//        if (e.getNewState() == ClientFlapConn.STATE_NOT_CONNECTED && e.getOldState() == ClientFlapConn.STATE_CONNECTED && getMainSession().isLoggedIn()) {
//            getMainSession().sessionDisconnected(LocaleUtils.getLocalizedString("gateway.oscar.disconnected", "gateway"));
//        }
    }

    protected void handleFlapPacket(FlapPacketEvent e) {
//        Log.debug("OSCAR bos flap packet received: "+e);
        FlapCommand cmd = e.getFlapCommand();

        if (cmd instanceof CloseFlapCmd) {
            CloseFlapCmd cfc = (CloseFlapCmd)cmd;
            if (cfc.getCode() == CloseFlapCmd.CODE_LOGGED_IN_ELSEWHERE) {
                getMainSession().sessionDisconnectedNoReconnect(LocaleUtils.getLocalizedString("gateway.oscar.multilogin", "gateway"));
            }
            else {
                getMainSession().sessionDisconnected(LocaleUtils.getLocalizedString("gateway.oscar.disconnected", "gateway"));
            }
        }
        super.handleFlapPacket(e);
    }

    protected void handleSnacPacket(SnacPacketEvent e) {
//        Log.debug("OSCAR bos snac packet received: "+e);
        super.handleSnacPacket(e);

        SnacCommand cmd = e.getSnacCommand();

        if (cmd instanceof ServerReadyCmd) {
            request(new ParamInfoRequest());
            request(new LocRightsRequest());
            request(new SsiRightsRequest());
            request(new SsiDataRequest());
        }
        else if (cmd instanceof BuddyAddedYouCmd) {
            BuddyAddedYouCmd bay = (BuddyAddedYouCmd)cmd;

            Presence p = new Presence();
            p.setType(Presence.Type.subscribe);
            p.setTo(getMainSession().getJID());
            p.setFrom(getMainSession().getTransport().convertIDToJID(bay.getUin()));
            getMainSession().getTransport().sendPacket(p);
        }
        else if (cmd instanceof BuddyAuthRequest) {
            BuddyAuthRequest bar = (BuddyAuthRequest)cmd;

            Presence p = new Presence();
            p.setType(Presence.Type.subscribe);
            p.setTo(getMainSession().getJID());
            p.setFrom(getMainSession().getTransport().convertIDToJID(bar.getScreenname()));
            getMainSession().getTransport().sendPacket(p);

            // Auto-accept auth request. (for now)
            // TODO: Evaluate handling this in a non-automated fashion.
            request(new AuthReplyCmd(bar.getScreenname(), null, true));
        }
        else if (cmd instanceof AuthReplyCmd) {
            AuthReplyCmd ar = (AuthReplyCmd)cmd;

            if (ar.isAccepted()) {
                Presence p = new Presence();
                p.setType(Presence.Type.subscribed);
                p.setTo(getMainSession().getJID());
                p.setFrom(getMainSession().getTransport().convertIDToJID(ar.getSender()));
                getMainSession().getTransport().sendPacket(p);
            }
            else {
                Presence p = new Presence();
                p.setType(Presence.Type.unsubscribed);
                p.setTo(getMainSession().getJID());
                p.setFrom(getMainSession().getTransport().convertIDToJID(ar.getSender()));
                getMainSession().getTransport().sendPacket(p);
            }
        }
        else if (cmd instanceof ModifyItemsCmd) {
            ModifyItemsCmd mic = (ModifyItemsCmd)cmd;

            List<SsiItem> items = mic.getItems();
            for (SsiItem item : items) {
                SsiItemObj obj = itemFactory.getItemObj(item);
                if (obj instanceof BuddyItem) {
                    BuddyItem bi = (BuddyItem)obj;
                    Log.debug("AIM got buddy item " + bi);
                    try {
                        OSCARBuddy oscarBuddy = (OSCARBuddy)getMainSession().getBuddyManager().getBuddy(getMainSession().getTransport().convertIDToJID(bi.getScreenname()));
                        oscarBuddy.tieBuddyItem(bi, false);
                    }
                    catch (NotFoundException ee) {
                        OSCARBuddy oscarBuddy = new OSCARBuddy(getMainSession().getBuddyManager(), bi);
                        getMainSession().getBuddyManager().storeBuddy(oscarBuddy);
                    }
                }
                else if (obj instanceof GroupItem) {
                    GroupItem gi = (GroupItem)obj;
                    Log.debug("AIM got group item " + gi);
                    getMainSession().gotGroup(gi);
                }
            }
        }
    }

    protected void handleSnacResponse(SnacResponseEvent e) {
        super.handleSnacResponse(e);

//        Log.debug("OSCAR bos snac response received: "+e);

        SnacCommand cmd = e.getSnacCommand();

        if (cmd instanceof LocRightsCmd) {
            request(new SetInfoCmd(new InfoData("oscargateway",
                    null, getMainSession().getCapabilities(), null)));
            request(new MyInfoRequest());
        }
        else if (cmd instanceof ParamInfoCmd) {
            ParamInfoCmd pic = (ParamInfoCmd) cmd;

            ParamInfo info = pic.getParamInfo();

            request(new SetParamInfoCmd(new ParamInfo(0,
                    info.getFlags() | ParamInfo.FLAG_TYPING_NOTIFICATION, 8000,
                    info.getMaxSenderWarning(), info.getMaxReceiverWarning(),
                    0)));
        }
        else if (cmd instanceof ServiceRedirect) {
            ServiceRedirect sr = (ServiceRedirect) cmd;

            getMainSession().connectToService(sr.getSnacFamily(), sr.getRedirectHost(),
                    sr.getCookie());

        }
        else if (cmd instanceof SsiDataCmd) {
            SsiDataCmd sdc = (SsiDataCmd) cmd;

            List<SsiItem> items = sdc.getItems();
            for (SsiItem item : items) {
                SsiItemObj obj = itemFactory.getItemObj(item);
                if (obj instanceof BuddyItem) {
                    BuddyItem bi = (BuddyItem)obj;
                    Log.debug("OSCAR: got buddy item " + bi);
                    try {
                        OSCARBuddy oscarBuddy = (OSCARBuddy)getMainSession().getBuddyManager().getBuddy(getMainSession().getTransport().convertIDToJID(bi.getScreenname()));
                        oscarBuddy.tieBuddyItem(bi, false);
                    }
                    catch (NotFoundException ee) {
                        OSCARBuddy oscarBuddy = new OSCARBuddy(getMainSession().getBuddyManager(), bi);
                        getMainSession().getBuddyManager().storeBuddy(oscarBuddy);
                    }
                }
                else if (obj instanceof GroupItem) {
                    GroupItem gi = (GroupItem)obj;
                    Log.debug("OSCAR: got group item " + gi);
                    getMainSession().gotGroup(gi);
                }
                else if (obj instanceof IconItem) {
                    IconItem ii = (IconItem)obj;
                    Log.debug("OSCAR: got icon item " + ii);
                    getMainSession().gotIconItem(ii);
                }
                else if (obj instanceof VisibilityItem) {
                    VisibilityItem vi = (VisibilityItem)obj;
                    Log.debug("OSCAR: got visibility item " + vi);
                    getMainSession().gotVisibilityItem(vi);
                }
                else {
                    Log.debug("OSCAR: got item we're not handling " + obj);
                }
            }

            if (sdc.getLastModDate() != 0) {
                request(new ActivateSsiCmd());
                clientReady();

                getMainSession().setLoginStatus(TransportLoginStatus.LOGGED_IN);
                getMainSession().gotCompleteSSI();
            }
        }
        else if (cmd instanceof OfflineMsgIcqCmd) {
            OfflineMsgIcqCmd omic = (OfflineMsgIcqCmd)cmd;

            String sn = String.valueOf(omic.getFromUIN());
            Date whenSent = omic.getDate();
            ByteBlock block = omic.getIcqData();
            final int len = LEBinaryTools.getUShort(block, 12) - 1;
            String msg = OscarTools.getString(block.subBlock(14, len), null);
            msg = StringUtils.unescapeFromXML(OscarTools.stripHtml(msg));

            // TODO: Translate offline message note
            getMainSession().getTransport().sendOfflineMessage(
                    getMainSession().getJID(),
                    getMainSession().getTransport().convertIDToJID(sn),
                    msg,
                    whenSent,
                    "Offline Message"
            );
        }
        else if (cmd instanceof OfflineMsgDoneCmd) {
            request(new OfflineMsgIcqAckCmd(getMainSession().getUIN(), (int)getMainSession().nextIcqId()));
        }
        else if (cmd instanceof MetaShortInfoCmd) {
//            MetaShortInfoCmd msic = (MetaShortInfoCmd)cmd;
//            Log.debug("RECEIVED META SHORT INFO: "+msic);
//            getMainSession().updateRosterNickname(String.valueOf(msic.getUIN()), msic.getNickname());
        }
        else if (cmd instanceof AuthReplyCmd) {
            AuthReplyCmd ar = (AuthReplyCmd)cmd;

            if (ar.isAccepted()) {
                Presence p = new Presence();
                p.setType(Presence.Type.subscribed);
                p.setTo(getMainSession().getJID());
                p.setFrom(getMainSession().getTransport().convertIDToJID(ar.getSender()));
                getMainSession().getTransport().sendPacket(p);
            }
            else {
                Presence p = new Presence();
                p.setType(Presence.Type.unsubscribed);
                p.setTo(getMainSession().getJID());
                p.setFrom(getMainSession().getTransport().convertIDToJID(ar.getSender()));
                getMainSession().getTransport().sendPacket(p);
            }
        }
        else if (cmd instanceof AuthFutureCmd) {
            AuthFutureCmd af = (AuthFutureCmd)cmd;

            Presence p = new Presence();
            p.setType(Presence.Type.subscribe);
            p.setTo(getMainSession().getJID());
            p.setFrom(getMainSession().getTransport().convertIDToJID(af.getUin()));
            getMainSession().getTransport().sendPacket(p);
        }
        else if (cmd instanceof SnacError) {
            SnacError se = (SnacError)cmd;
            if (se.getErrorCode() == SnacError.CODE_REFUSED_BY_CLIENT) {
                getMainSession().getTransport().sendMessage(
                        getMainSession().getJID(),
                        getMainSession().getTransport().getJID(),
                        LocaleUtils.getLocalizedString("gateway.aim.msgrefused","gateway")
                );
            }
            //TODO: Tons more errors that can be caught.  Gotta catch 'em all!  =)  (please don't sue me Nintendo)
        }
    }
}
