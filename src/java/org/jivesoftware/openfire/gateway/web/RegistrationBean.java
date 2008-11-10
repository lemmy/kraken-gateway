/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.openfire.gateway.web;

import java.util.Date;

import org.jivesoftware.openfire.gateway.registration.Registration;

import redstone.xmlrpc.XmlRpcStruct;

public class RegistrationBean {
       private String jid;
       private Date lastLogin;
       private String nickname;
       private String password;
       private Date registrationDate;
       private long registrationID;
       private String transportType;
       private String userName;

       public RegistrationBean(Registration r) {
               jid = r.getJID().toString();
               lastLogin = r.getLastLogin();
               nickname = r.getNickname();
               password = r.getPassword();
               registrationDate = r.getRegistrationDate();
               registrationID = r.getRegistrationID();
               transportType = r.getTransportType().toString();
               userName = r.getUsername();

       }
       
       public RegistrationBean (XmlRpcStruct struct) {
               jid = struct.getString("jid");
               lastLogin = struct.getDate("lastLogin");
               nickname =struct.getString("nickname");
               password =struct.getString("password");
               registrationDate = struct.getDate("registrationDate");
               registrationID = struct.getInteger("registrationID");
               transportType = struct.getString("transportType");
               userName = struct.getString("userName");
       }

       /**
        * @return the jid
        */
       public String getJid() {
               return jid;
       }

       /**
        * @return the lastLogin
        */
       public Date getLastLogin() {
               return lastLogin;
       }

       /**
        * @return the nickname
        */
       public String getNickname() {
               return nickname;
       }

       /**
        * @return the password
        */
       public String getPassword() {
               return password;
       }

       /**
        * @return the registrationDate
        */
       public Date getRegistrationDate() {
               return registrationDate;
       }

       /**
        * @return the registrationID
        */
       public long getRegistrationID() {
               return registrationID;
       }

       /**
        * @return the transportType
        */
       public String getTransportType() {
               return transportType;
       }

       /**
        * @return the userName
        */
       public String getUserName() {
               return userName;
       }

}
