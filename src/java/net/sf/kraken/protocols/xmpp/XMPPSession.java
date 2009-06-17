/**
 * $Revision$
 * $Date$
 *
 * Copyright 2008 Daniel Henninger.  All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.protocols.xmpp;

import net.sf.kraken.avatars.Avatar;
import net.sf.kraken.protocols.xmpp.packet.*;
import net.sf.kraken.registration.Registration;
import net.sf.kraken.roster.TransportBuddy;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.*;

import org.apache.log4j.Logger;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.util.Base64;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.NotFoundException;
import org.xmpp.packet.JID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Handles XMPP transport session.
 *
 * @author Daniel Henninger
 * @author Mehmet Ecevit
 */
public class XMPPSession extends TransportSession {

    static Logger Log = Logger.getLogger(XMPPSession.class);
    
    /**
     * Create an XMPP Session instance.
     *
     * @param registration Registration information used for logging in.
     * @param jid JID associated with this session.
     * @param transport Transport instance associated with this session.
     * @param priority Priority of this session.
     */
    public XMPPSession(Registration registration, JID jid, XMPPTransport transport, Integer priority) {
        super(registration, jid, transport, priority);
        Log.debug("Creating "+getTransport().getType()+" session for " + registration.getUsername());
        String connecthost;
        Integer connectport;
        String domain;

        connecthost = JiveGlobals.getProperty("plugin.gateway."+getTransport().getType()+".connecthost", (getTransport().getType().equals(TransportType.gtalk) ? "talk.google.com" : "jabber.org"));
        connectport = JiveGlobals.getIntProperty("plugin.gateway."+getTransport().getType()+".connectport", 5222);

        if (getTransport().getType().equals(TransportType.gtalk)) {
            domain = "gmail.com";
        }
        else {
            domain = connecthost;
        }

        // For different domains other than 'gmail.com', which is given with Google Application services
        if (registration.getUsername().indexOf("@") >- 1) {
            domain = registration.getUsername().substring( registration.getUsername().indexOf("@")+1 );
        }

        // If administrator specified "*" for domain, allow user to connect to anything.
        if (connecthost.equals("*")) {
            connecthost = domain;
        }

        config = new ConnectionConfiguration(connecthost, connectport, domain);
        config.setCompressionEnabled(JiveGlobals.getBooleanProperty("plugin.gateway."+getTransport().getType()+".usecompression", false));

        if (getTransport().getType().equals(TransportType.gtalk) && JiveGlobals.getBooleanProperty("plugin.gateway.gtalk.mailnotifications", true)) {
            ProviderManager.getInstance().addIQProvider(GoogleMailBoxPacket.MAILBOX_ELEMENT, GoogleMailBoxPacket.MAILBOX_NAMESPACE, new GoogleMailBoxPacket.Provider());
            ProviderManager.getInstance().addExtensionProvider(GoogleNewMailExtension.ELEMENT_NAME, GoogleNewMailExtension.NAMESPACE, new GoogleNewMailExtension.Provider());
        }
    }
    
    /*
     * XMPP connection
     */
    public XMPPConnection conn = null;
    
    /**
     * XMPP listener
     */
    private XMPPListener listener = null;

    /*
     * XMPP connection configuration
     */
    private ConnectionConfiguration config = null;

    /**
     * Timer to check for online status.
     */
    public Timer timer = new Timer();

    /**
     * Interval at which status is checked.
     */
    private int timerInterval = 60000; // 1 minute

    /**
     * Mail checker
     */
    MailCheck mailCheck;

    /**
     * The session's current presence
     */
    org.jivesoftware.smack.packet.Presence presence;

    /**
     * Returns a full JID based off of a username passed in.
     *
     * If it already looks like a JID, returns what was passed in.
     *
     * @param username Username to turn into a JID.
     * @return Converted username.
     */
    public String generateFullJID(String username) {
        if (username.indexOf("@") > -1) {
            return username;
        }

        if (getTransport().getType().equals(TransportType.gtalk)) {
            return username+"@"+"gmail.com";
        }
        else {
            String connecthost = JiveGlobals.getProperty("plugin.gateway."+getTransport().getType()+".connecthost", (getTransport().getType().equals(TransportType.gtalk) ? "talk.google.com" : "jabber.org"));
            return username+"@"+connecthost;
        }
    }

    /**
     * Returns a username based off of a registered name (possible JID) passed in.
     *
     * If it already looks like a username, returns what was passed in.
     *
     * @param regName Registered name to turn into a username.
     * @return Converted registered name.
     */
    public String generateUsername(String regName) {
        if (regName.indexOf("@") > -1) {
            if (getTransport().getType().equals(TransportType.gtalk)) {
                return regName;
            }
            else {
                return regName.substring(0, regName.indexOf("@"));
            }
        }
        else {
            if (getTransport().getType().equals(TransportType.gtalk)) {
                return regName+"@gmail.com";
            }
            else {
                return regName;
            }
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#logIn(net.sf.kraken.type.PresenceType, String)
     */
    public void logIn(PresenceType presenceType, String verboseStatus) {
        presence = new org.jivesoftware.smack.packet.Presence(org.jivesoftware.smack.packet.Presence.Type.available);
        if (JiveGlobals.getBooleanProperty("plugin.gateway."+getTransport().getType()+".avatars", true) && getAvatar() != null) {
            Avatar avatar = getAvatar();
            // Same thing in this case, so lets go ahead and set them.
            avatar.setLegacyIdentifier(avatar.getXmppHash());
            VCardUpdateExtension ext = new VCardUpdateExtension();
            ext.setPhotoHash(avatar.getLegacyIdentifier());
            presence.addExtension(ext);
        }
        setPendingPresenceAndStatus(presenceType, verboseStatus);
        if (!this.isLoggedIn()) {
            listener = new XMPPListener(this);
            new Thread() {
                public void run() {
                    String userName = generateUsername(registration.getUsername());
                    conn = new XMPPConnection(config);
                    try {
                        conn.connect();
                        conn.addConnectionListener(listener);
                        try {
                            conn.login(userName, registration.getPassword(), StringUtils.randomString(10));
                            conn.getRoster().addRosterListener(listener);
                            conn.getChatManager().addChatListener(listener);
                            // Use this to filter out anything we don't care about
                            conn.addPacketListener(listener, new OrFilter(
                                    new PacketTypeFilter(GoogleMailBoxPacket.class),
                                    new PacketExtensionFilter(GoogleNewMailExtension.ELEMENT_NAME, GoogleNewMailExtension.NAMESPACE)
                            ));

                            if (JiveGlobals.getBooleanProperty("plugin.gateway."+getTransport().getType()+".avatars", true) && getAvatar() != null) {
                                new Thread() {
                                    public void run() {
                                        Avatar avatar = getAvatar();

                                        VCard vCard = new VCard();
                                        try {
                                            vCard.load(conn);
                                            vCard.setAvatar(Base64.decode(avatar.getImageData()), avatar.getMimeType());
                                            vCard.save(conn);
                                        }
                                        catch (XMPPException e) {
                                            Log.debug("XMPP: Error while updating vcard for avatar change.", e);
                                        }
                                        catch (NotFoundException e) {
                                            Log.debug("XMPP: Unable to find avatar while setting initial.", e);
                                        }
                                    }
                                }.start();
                            }

                            setLoginStatus(TransportLoginStatus.LOGGED_IN);
                            syncUsers();

                            if (getTransport().getType().equals(TransportType.gtalk) && JiveGlobals.getBooleanProperty("plugin.gateway.gtalk.mailnotifications", true)) {
                                conn.sendPacket(new IQWithPacketExtension(generateFullJID(getRegistration().getUsername()), new GoogleUserSettingExtension(false, true, false), IQ.Type.SET));
                                conn.sendPacket(new IQWithPacketExtension(generateFullJID(getRegistration().getUsername()), new GoogleRelayExtension()));
                                conn.sendPacket(new IQWithPacketExtension(generateFullJID(getRegistration().getUsername()), new GoogleMailNotifyExtension()));
                                conn.sendPacket(new IQWithPacketExtension(generateFullJID(getRegistration().getUsername()), new GoogleSharedStatusExtension()));
                                mailCheck = new MailCheck();
                                timer.schedule(mailCheck, timerInterval, timerInterval);
                            }
                        }
                        catch (XMPPException e) {
                            Log.debug(getTransport().getType()+" user's login/password does not appear to be correct: "+getRegistration().getUsername(), e);
                            sessionDisconnectedNoReconnect(LocaleUtils.getLocalizedString("gateway.xmpp.passwordincorrect", "kraken"));
                        }
                    }
                    catch (XMPPException e) {
                        Log.debug(getTransport().getType()+" user is not able to connect: "+getRegistration().getUsername(), e);
                        sessionDisconnected(LocaleUtils.getLocalizedString("gateway.xmpp.connectionfailed", "kraken"));
                    }
                }
            }.start();
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#logOut()
     */
    public void logOut() {
        cleanUp();
        sessionDisconnectedNoReconnect(null);
    }

    /**
     * @see net.sf.kraken.session.TransportSession#cleanUp()
     */
    public void cleanUp() {
        if (timer != null) {
            try {
                timer.cancel();
            }
            catch (Exception e) {
                // Ignore
            }
            timer = null;
        }
        if (mailCheck != null) {
            try {
                mailCheck.cancel();
            }
            catch (Exception e) {
                // Ignore
            }
            mailCheck = null;
        }
        if (conn != null) {
            try {
                conn.removeConnectionListener(listener);
            }
            catch (Exception e) {
                // Ignore
            }
            try {
                conn.removePacketListener(listener);
            }
            catch (Exception e) {
                // Ignore
            }
            try {
                conn.getRoster().removeRosterListener(listener);
            }
            catch (Exception e) {
                // Ignore
            }
            try {
                conn.getChatManager().removeChatListener(listener);
            }
            catch (Exception e) {
                // Ignore
            }
            try {
                conn.disconnect();
            }
            catch (Exception e) {
                // Ignore
            }
        }
        conn = null;
        listener = null;
    }

    /**
     * @see net.sf.kraken.session.TransportSession#updateStatus(net.sf.kraken.type.PresenceType, String)
     */
    public void updateStatus(PresenceType presenceType, String verboseStatus) {
        org.jivesoftware.smack.packet.Presence.Mode mode = ((XMPPTransport)getTransport()).convertGatewayStatusToXMPP(presenceType);
        if (mode != null && mode != presence.getMode()) {
            presence.setMode(mode);
        }
        if (presence.getType() != org.jivesoftware.smack.packet.Presence.Type.available) {
            presence.setType(org.jivesoftware.smack.packet.Presence.Type.available);
        }
        if (verboseStatus != null) {
            presence.setStatus(verboseStatus);
        }
        else {
            presence.setStatus("");
        }
        try {
            conn.sendPacket(presence);
            setPresenceAndStatus(presenceType, verboseStatus);
        }
        catch (IllegalStateException e) {
            Log.debug("XMPP: Not connected while trying to change status.");
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#addContact(org.xmpp.packet.JID, String, java.util.ArrayList)
     */
    public void addContact(JID jid, String nickname, ArrayList<String> groups) {
        String mail = getTransport().convertJIDToID(jid);
        try {
            conn.getRoster().createEntry(mail, nickname, groups.toArray(new String[groups.size()]));
            RosterEntry entry = conn.getRoster().getEntry(mail);

            getBuddyManager().storeBuddy(new XMPPBuddy(getBuddyManager(), mail, nickname, entry.getGroups(), entry));
        }
        catch (XMPPException ex) {
            Log.debug("XMPP: unable to add:"+ mail);
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#removeContact(net.sf.kraken.roster.TransportBuddy)
     */
    public void removeContact(TransportBuddy contact) {
        RosterEntry user2remove;
        String mail = getTransport().convertJIDToID(contact.getJID());
        user2remove =  conn.getRoster().getEntry(mail);
        try {
            conn.getRoster().removeEntry(user2remove);
        }
        catch (XMPPException ex) {
            Log.debug("XMPP: unable to remove:"+ mail);
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#updateContact(net.sf.kraken.roster.TransportBuddy)
     */
    public void updateContact(TransportBuddy contact) {
        RosterEntry user2Update;
        String mail = getTransport().convertJIDToID(contact.getJID());
        user2Update =  conn.getRoster().getEntry(mail);
        user2Update.setName(contact.getNickname());
        Collection<String> newgroups = contact.getGroups();
        if (newgroups == null) {
            newgroups = new ArrayList<String>();
        }
        for (RosterGroup group : conn.getRoster().getGroups()) {
            if (newgroups.contains(group.getName())) {
                if (!group.contains(user2Update)) {
                    try {
                        group.addEntry(user2Update);
                        newgroups.remove(group.getName());
                    }
                    catch (XMPPException e) {
                        Log.debug("XMPP: Unable to add roster item to group.");
                    }
                }
            }
            else {
                if (group.contains(user2Update)) {
                    try {
                        group.removeEntry(user2Update);
                    }
                    catch (XMPPException e) {
                        Log.debug("XMPP: Unable to delete roster item from group.");
                    }
                }
            }
        }
        for (String group : newgroups) {
            RosterGroup newgroup = conn.getRoster().createGroup(group);
            try {
                newgroup.addEntry(user2Update);
            }
            catch (XMPPException e) {
                Log.debug("XMPP: Unable to add roster item to new group.");
            }
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#sendMessage(org.xmpp.packet.JID, String)
     */
    public void sendMessage(JID jid, String message) {
        Chat chat = conn.getChatManager().createChat(getTransport().convertJIDToID(jid), listener);
        try {
            chat.sendMessage(message);
        }
        catch (XMPPException e) {
            // Ignore
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#sendChatState(org.xmpp.packet.JID, net.sf.kraken.type.ChatStateType)
     */
    public void sendChatState(JID jid, ChatStateType chatState) {
        Chat chat =
conn.getChatManager().createChat(getTransport().convertJIDToID(jid),
listener);
        try {
            ChatState state = ChatState.active;
            switch (chatState) {
                case active:    state = ChatState.active;    break;
                case composing: state = ChatState.composing; break;
                case paused:    state = ChatState.paused;    break;
                case inactive:  state = ChatState.inactive;  break;
                case gone:      state = ChatState.gone;      break;
            };

            Message message = new Message();
            message.addExtension(new ChatStateExtension(state));
            chat.sendMessage(message);
        }
        catch (XMPPException e) {
            // Ignore
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#sendBuzzNotification(org.xmpp.packet.JID, String)
     */
    public void sendBuzzNotification(JID jid, String message) {
        Chat chat = conn.getChatManager().createChat(getTransport().convertJIDToID(jid), listener);
        try {
            Message m = new Message();
            m.setTo(getTransport().convertJIDToID(jid));
            m.addExtension(new BuzzExtension());
            chat.sendMessage(m);
        }
        catch (XMPPException e) {
            // Ignore
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#updateLegacyAvatar(String, byte[])
     */
    public void updateLegacyAvatar(String type, byte[] data) {
        final byte[] tmpData = data;
        new Thread() {
            public void run() {
                Avatar avatar = getAvatar();

                VCard vCard = new VCard();
                try {
                    vCard.load(conn);
                    vCard.setAvatar(tmpData, avatar.getMimeType());
                    vCard.save(conn);

                    // Same thing in this case, so lets go ahead and set them.
                    avatar.setLegacyIdentifier(avatar.getXmppHash());
                    VCardUpdateExtension ext = (VCardUpdateExtension)presence.getExtension("x", NameSpace.VCARD_TEMP_X_UPDATE);
                    if (ext != null) {
                        ext.setPhotoHash(avatar.getLegacyIdentifier());
                    }
                    else {
                        ext = new VCardUpdateExtension();
                        ext.setPhotoHash(avatar.getLegacyIdentifier());
                        presence.addExtension(ext);
                    }
                    conn.sendPacket(presence);
                }
                catch (XMPPException e) {
                    Log.debug("XMPP: Error while updating vcard for avatar change.", e);
                }
            }
        }.start();
    }
    
    public void syncUsers() {
        for (RosterEntry entry : conn.getRoster().getEntries()) {
            getBuddyManager().storeBuddy(new XMPPBuddy(getBuddyManager(), entry.getUser(), entry.getName(), entry.getGroups(), entry));
            ProbePacket probe = new ProbePacket(getJID().toString(), entry.getUser());
            try {
                conn.sendPacket(probe);
            }
            catch (IllegalStateException e) {
                Log.debug("XMPP: Not connected while trying to send probe.");
            }
        }

        try {
            getTransport().syncLegacyRoster(getJID(), getBuddyManager().getBuddies());
        }
        catch (UserNotFoundException ex) {
            Log.error("XMPP: User not found while syncing legacy roster: ", ex);
        }

        getBuddyManager().activate();
    }

    private class MailCheck extends TimerTask {
        /**
         * Check GMail for new mail.
         */
        public void run() {
            if (getTransport().getType().equals(TransportType.gtalk) && JiveGlobals.getBooleanProperty("plugin.gateway.gtalk.mailnotifications", true)) {
                GoogleMailNotifyExtension gmne = new GoogleMailNotifyExtension();
                gmne.setNewerThanTime(listener.getLastGMailThreadDate());
                gmne.setNewerThanTid(listener.getLastGMailThreadId());
                conn.sendPacket(new IQWithPacketExtension(generateFullJID(getRegistration().getUsername()), gmne));
            }
        }
    }
    
}
