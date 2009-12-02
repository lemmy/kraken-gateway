/**
 * $Revision$
 * $Date$
 *
 * Copyright 2008 Daniel Henninger.  All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package net.sf.kraken.protocols.gadugadu;

import net.sf.kraken.roster.TransportBuddy;
import net.sf.kraken.roster.TransportBuddyManager;
import pl.mn.communicator.LocalUser;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Henninger
 */
public class GaduGaduBuddy extends TransportBuddy {

    public GaduGaduBuddy(TransportBuddyManager manager, LocalUser localUser) {
        super(manager, Integer.toString(localUser.getUin()), localUser.getDisplayName(), null);
        if (localUser.getGroup() != null) {
            this.groups = Arrays.asList(localUser.getGroup());
        }
        gaduFirstName = localUser.getFirstName();
        gaduLastName = localUser.getLastName();
        gaduNickName = localUser.getNickName();
        gaduTelephone = localUser.getTelephone();
        gaduEmailAddress = localUser.getEmailAddress();
    }

    public GaduGaduBuddy(TransportBuddyManager manager, String uin, String nickname, String group) {
        super(manager, uin, nickname, null);
        if (group != null) {
            this.groups = Arrays.asList(group);
        }
    }

    private String gaduFirstName = null;
    private String gaduLastName = null;
    private String gaduNickName = null;
    private String gaduTelephone = null;
    private String gaduEmailAddress = null;

    public String getGaduFirstName() {
        return gaduFirstName;
    }

    public void setGaduFirstName(String gaduFirstName) {
        this.gaduFirstName = gaduFirstName;
    }

    public String getGaduLastName() {
        return gaduLastName;
    }

    public void setGaduLastName(String gaduLastName) {
        this.gaduLastName = gaduLastName;
    }

    public String getGaduNickName() {
        return gaduNickName;
    }

    public void setGaduNickName(String gaduNickName) {
        this.gaduNickName = gaduNickName;
    }

    public String getGaduTelephone() {
        return gaduTelephone;
    }

    public void setGaduTelephone(String gaduTelephone) {
        this.gaduTelephone = gaduTelephone;
    }

    public LocalUser toLocalUser() {
        LocalUser localUser = new LocalUser();
        localUser.setUin(Integer.parseInt(this.getName()));
        localUser.setDisplayName(this.getNickname() != null ? this.getNickname() : this.getName());
        Collection<String> groups = this.getGroups();
        if (groups.size() > 0) {
            localUser.setGroup((String)groups.toArray()[0]);
        }
        localUser.setEmailAddress(this.getGaduEmailAddress());
        localUser.setFirstName(this.getGaduFirstName() != null ? this.getGaduFirstName() : this.getName());
        localUser.setLastName(this.getGaduLastName());
        localUser.setNickName(this.getNickname());
        localUser.setTelephone(this.getGaduTelephone());
        return localUser;
    }

    public String getGaduEmailAddress() {
        return gaduEmailAddress;
    }

    public void setGaduEmailAddress(String gaduEmailAddress) {
        this.gaduEmailAddress = gaduEmailAddress;
    }
    
    public void setBuddyGroups(List<String> groups) {
    	this.groups = groups;
    }
    
    public void setBuddyNickname(String nickname) {
    	this.nickname = nickname;
    }
    
}
