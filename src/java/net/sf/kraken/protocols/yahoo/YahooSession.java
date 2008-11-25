/**
 * $Revision$
 * $Date$
 *
 * Copyright 2008 Daniel Henninger.  All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.protocols.yahoo;

import net.sf.kraken.pseudoroster.PseudoRoster;
import net.sf.kraken.pseudoroster.PseudoRosterItem;
import net.sf.kraken.pseudoroster.PseudoRosterManager;
import net.sf.kraken.registration.Registration;
import net.sf.kraken.roster.TransportBuddy;
import net.sf.kraken.session.TransportSession;
import net.sf.kraken.type.ChatStateType;
import net.sf.kraken.type.PresenceType;
import net.sf.kraken.type.TransportLoginStatus;

import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.NotFoundException;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.openymsg.network.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * Represents a Yahoo session.
 * 
 * This is the interface with which the base transport functionality will
 * communicate with Yahoo.
 *
 * @author Daniel Henninger
 * Heavily inspired by Noah Campbell's work.
 */
public class YahooSession extends TransportSession {

    static Logger Log = Logger.getLogger(YahooSession.class);

    /**
     * Create a Yahoo Session instance.
     *
     * @param registration Registration informationed used for logging in.
     * @param jid JID associated with this session.
     * @param transport Transport instance associated with this session
     * @param priority Priority of this session
     */
    public YahooSession(Registration registration, JID jid, YahooTransport transport, Integer priority) {
        super(registration, jid, transport, priority);

        pseudoRoster = PseudoRosterManager.getInstance().getPseudoRoster(registration);
    }

    /**
     * Our pseudo roster.
     *
     * We only really use it for nickname tracking.
     */
    private PseudoRoster pseudoRoster;

    /**
     * Yahoo session
     */
    private Session yahooSession;

    /**
     * Yahoo session listsner.
     */
    private YahooListener yahooListener;

    /**
     * @see net.sf.kraken.session.TransportSession#logIn(net.sf.kraken.type.PresenceType, String)
     */
    public void logIn(PresenceType presenceType, String verboseStatus) {
        setPendingPresenceAndStatus(presenceType, verboseStatus);
        if (!isLoggedIn()) {
            yahooSession = new Session(new DirectConnectionHandler(
                    JiveGlobals.getProperty("plugin.gateway.yahoo.connecthost", "scs.msg.yahoo.com"),
                    JiveGlobals.getIntProperty("plugin.gateway.yahoo.connectport", 5050)
            ));
            yahooListener = new YahooListener(this);
            yahooSession.addSessionListener(yahooListener);

            new Thread() {
                public void run() {
                    try {
                        yahooSession.setStatus(Status.AVAILABLE);
                        yahooSession.login(registration.getUsername(), registration.getPassword());
                        setLoginStatus(TransportLoginStatus.LOGGED_IN);

                        yahooSession.setStatus(((YahooTransport)getTransport()).convertXMPPStatusToYahoo(getPresence()));

                        syncUsers();
                    }
                    catch (LoginRefusedException e) {
                        yahooSession.reset();
                        String reason = LocaleUtils.getLocalizedString("gateway.yahoo.loginrefused", "kraken");
                        AuthenticationState state = e.getStatus();
                        if (state.equals(AuthenticationState.BADUSERNAME)) {
                            reason = LocaleUtils.getLocalizedString("gateway.yahoo.unknownuser", "kraken");
                        }
                        else if (state.equals(AuthenticationState.BAD)) {
                            reason = LocaleUtils.getLocalizedString("gateway.yahoo.badpassword", "kraken");
                        }
                        else if (state.equals(AuthenticationState.LOCKED)) {
                            AccountLockedException e2 = (AccountLockedException)e;
                            if(e2.getWebPage() != null) {
                                reason = LocaleUtils.getLocalizedString("gateway.yahoo.accountlockedwithurl", "kraken", Arrays.asList(e2.getWebPage().toString()));
                            }
                            else {
                                reason = LocaleUtils.getLocalizedString("gateway.yahoo.accountlocked", "kraken");
                            }
                        }

                        Log.debug("Yahoo login failed for "+getJID()+": "+reason);

                        getTransport().sendMessage(
                                getJID(),
                                getTransport().getJID(),
                                reason,
                                Message.Type.error
                        );
                        setLoginStatus(TransportLoginStatus.LOGGED_OUT);
                    }
                    catch (IOException e) {
                        Log.debug("Yahoo login caused IO exception:", e);

                        getTransport().sendMessage(
                                getJID(),
                                getTransport().getJID(),
                                LocaleUtils.getLocalizedString("gateway.yahoo.unknownerror", "kraken"),
                                Message.Type.error
                        );
                        setLoginStatus(TransportLoginStatus.LOGGED_OUT);
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
        if (yahooSession != null) {
            if (yahooListener != null) {
                yahooSession.removeSessionListener(yahooListener);
                yahooListener = null;
            }
            try {
                yahooSession.logout();
            }
            catch (IOException e) {
                Log.debug("Failed to log out from Yahoo.");
            }
            catch (IllegalStateException e) {
                // Not logged in, well then no problem.
            }
            try {
                yahooSession.reset();
            }
            catch (Exception e) {
                // If this fails it's ok, move on
            }
            yahooSession = null;
        }
    }

    /**
     * Syncs up the yahoo roster with the jabber roster.
     */
    public void syncUsers() {
        // First we need to get a good mapping of users to what groups they are in.
        HashMap<String,ArrayList<String>> userToGroups = new HashMap<String,ArrayList<String>>();
        for (YahooGroup group : yahooSession.getGroups()) {
            for (YahooUser user : group.getUsers()) {
                ArrayList<String> groups;
                if (userToGroups.containsKey(user.getId())) {
                    groups = userToGroups.get(user.getId());
                }
                else {
                    groups = new ArrayList<String>();
                }
                if (!groups.contains(group.getName())) {
                    groups.add(group.getName());
                }
                userToGroups.put(user.getId(), groups);
            }
        }
        // Now we will run through the entire list of users and set up our sync group.
        for (Object userObj : yahooSession.getUsers().values()) {
            YahooUser user = (YahooUser)userObj;
            PseudoRosterItem rosterItem = pseudoRoster.getItem(user.getId());
            String nickname = null;
            if (rosterItem != null) {
                nickname = rosterItem.getNickname();
            }
            if (nickname == null) {
                nickname = user.getId();
            }
            if (userToGroups.containsKey(user.getId())) {
                getBuddyManager().storeBuddy(new YahooBuddy(this.getBuddyManager(), user, nickname, userToGroups.get(user.getId()), rosterItem));
            }
            else {
                getBuddyManager().storeBuddy(new YahooBuddy(this.getBuddyManager(), user, nickname, null, rosterItem));
            }
        }
        // Lets try the actual sync.
        try {
            getTransport().syncLegacyRoster(getJID(), getBuddyManager().getBuddies());
        }
        catch (UserNotFoundException e) {
            Log.debug("Unable to sync yahoo contact list for " + getJID());
        }

        getBuddyManager().activate();
    }

    /**
     * @see net.sf.kraken.session.TransportSession#addContact(org.xmpp.packet.JID, String, java.util.ArrayList)
     */
    public void addContact(JID jid, String nickname, ArrayList<String> groups) {
        // Syncing will take care of add.
        String contact = getTransport().convertJIDToID(jid);
        PseudoRosterItem rosterItem;
        if (pseudoRoster.hasItem(contact)) {
            rosterItem = pseudoRoster.getItem(contact);
            rosterItem.setNickname(nickname);
        }
        else {
            rosterItem = pseudoRoster.createItem(contact, nickname, null);
        }
        YahooUser yUser = new YahooUser(contact);
        YahooBuddy yBuddy = new YahooBuddy(getBuddyManager(), yUser, nickname, groups, rosterItem);
        getBuddyManager().storeBuddy(yBuddy);
        syncContactGroups(yBuddy);
    }

    /**
     * @see net.sf.kraken.session.TransportSession#removeContact(net.sf.kraken.roster.TransportBuddy)
     */
    public void removeContact(TransportBuddy contact) {
        String yahooContact = getTransport().convertJIDToID(contact.getJID());
        YahooUser yUser = ((YahooBuddy)contact).yahooUser;
        for (YahooGroup yahooGroup : yUser.getGroups()) {
            try {
                yahooSession.removeFriend(yahooContact, yahooGroup.getName());
            }
            catch (IOException e) {
                Log.debug("Failed to remove yahoo user "+yUser+" from group "+yahooGroup);
            }
        }
        pseudoRoster.removeItem(yahooContact);
    }

    /**
     * @see net.sf.kraken.session.TransportSession#updateContact(net.sf.kraken.roster.TransportBuddy)
     */
    public void updateContact(TransportBuddy contact) {
        String yahooContact = getTransport().convertJIDToID(contact.getJID());
        PseudoRosterItem rosterItem;
        if (pseudoRoster.hasItem(yahooContact)) {
            rosterItem = pseudoRoster.getItem(yahooContact);
            rosterItem.setNickname(contact.getNickname());
        }
        else {
            rosterItem = pseudoRoster.createItem(yahooContact, contact.getNickname(), null);
        }
        try {
            YahooBuddy yBuddy = (YahooBuddy)getBuddyManager().getBuddy(contact.getJID());
            yBuddy.pseudoRosterItem = rosterItem;
            syncContactGroups(yBuddy);
        }
        catch (NotFoundException e) {
            Log.debug("Yahoo: Updated buddy not found in buddy manager: "+yahooContact);
        }
    }

    /**
     * Given a legacy contact and a list of groups, makes sure that the list is in sync with
     * the actual group list.
     *
     * @param yBuddy Yahoo buddy instance of contact.
     */
    public void syncContactGroups(YahooBuddy yBuddy) {
        if (yBuddy.getGroups() == null || yBuddy.getGroups().size() == 0) {
            yBuddy.setGroups(Arrays.asList("Transport Buddies"));
        }
        HashMap<String,YahooGroup> yahooGroups = new HashMap<String,YahooGroup>();
        // Lets create a hash of these for easier reference.
        for (YahooGroup yahooGroup : yahooSession.getGroups()) {
            yahooGroups.put(yahooGroup.getName(), yahooGroup);
        }
        // Create groups(add user to them) that do not currently exist.
        for (String group : yBuddy.groups) {
            if (!yahooGroups.containsKey(group)) {
                try {
                    Log.debug("Yahoo: Adding contact "+yBuddy.getName()+" to non-existent group "+group);
                    yahooSession.addFriend(yBuddy.getName(), group);
                }
                catch (IOException e) {
                    Log.debug("Error while syncing Yahoo groups.");
                }
            }
        }
        // Now we handle adds, syncing the two lists.
        for (YahooGroup yahooGroup : yahooSession.getGroups()) {
            if (yBuddy.groups.contains(yahooGroup.getName())) {
                if (!yahooGroup.getUsers().contains(yBuddy.yahooUser)) {
                    try {
                        Log.debug("Yahoo: Adding contact "+yBuddy.getName()+" to existing group "+yahooGroup.getName());
                        yahooSession.addFriend(yBuddy.getName(), yahooGroup.getName());
                    }
                    catch (IOException e) {
                        Log.debug("Error while syncing Yahoo groups.");
                    }
                }
            }
        }
        // Now we handle removes, syncing the two lists. 
        for (YahooGroup yahooGroup : yahooSession.getGroups()) {
            if (!yBuddy.groups.contains(yahooGroup.getName())) {
                if (yahooGroup.getUsers().contains(yBuddy.yahooUser)) {
                    try {
                        Log.debug("Yahoo: Removing contact "+yBuddy.getName()+" from group "+yahooGroup.getName());
                        yahooSession.removeFriend(yBuddy.getName(), yahooGroup.getName());
                    }
                    catch (IOException e) {
                        Log.debug("Error while syncing Yahoo groups.");
                    }
                }
            }
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#sendMessage(org.xmpp.packet.JID, String)
     */
    public void sendMessage(JID jid, String message) {
        try {
            yahooSession.sendMessage(jid.getNode(), message);
        }
        catch (IOException e) {
            Log.debug("Failed to send message to yahoo user.");
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#sendChatState(org.xmpp.packet.JID,net.sf.kraken.type.ChatStateType)
     */
    public void sendChatState(JID jid, ChatStateType chatState) {
        try {
            if (chatState.equals(ChatStateType.composing)) {
                yahooSession.sendTypingNotification(getTransport().convertJIDToID(jid), true);
            }
            else {
                yahooSession.sendTypingNotification(getTransport().convertJIDToID(jid), false);
            }
        }
        catch (IOException e) {
            Log.debug("Failed to send typing notification to yahoo user.");
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#sendBuzzNotification(org.xmpp.packet.JID, String)
     */
    public void sendBuzzNotification(JID jid, String message) {
        try {
            yahooSession.sendBuzz(getTransport().convertJIDToID(jid));
        }
        catch (IOException e) {
            Log.debug("Failed to send buzz notification to yahoo user.");
        }
    }

    /**
     * @see net.sf.kraken.session.TransportSession#updateLegacyAvatar(String, byte[])
     */
    public void updateLegacyAvatar(String type, byte[] data) {
    }

    /**
     * @see net.sf.kraken.session.TransportSession#updateStatus(net.sf.kraken.type.PresenceType, String)
     */
    public void updateStatus(PresenceType presenceType, String verboseStatus) {
        try {
            if (isLoggedIn()) {
                yahooSession.setStatus(((YahooTransport)getTransport()).convertXMPPStatusToYahoo(presenceType));
                setPresenceAndStatus(presenceType, verboseStatus);
            }
            else {
                // TODO: Should we consider auto-logging back in?
            }
        }
        catch (Exception e) {
            Log.debug("Unable to set Yahoo Status:", e);
        }
    }

}
