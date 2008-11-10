/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2006-2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.openfire.gateway.web;

import redstone.xmlrpc.XmlRpcServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.jivesoftware.admin.AuthCheckFilter;
import org.jivesoftware.util.PropertyEventListener;
import org.jivesoftware.util.PropertyEventDispatcher;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * @author Daniel Henninger
 */
public class GatewayXMLRPC extends XmlRpcServlet implements PropertyEventListener {

    static Logger Log = Logger.getLogger(GatewayXMLRPC.class);

    XMLRPCConduit conduit;

    public void init(ServletConfig servletConfig) {
        try {
            super.init(servletConfig);
            conduit = new XMLRPCConduit();
            PropertyEventDispatcher.addListener(this);

            this.getXmlRpcServer().addInvocationHandler("Manager", conduit);

            AuthCheckFilter.addExclude("gateway/xml-rpc");
        }
        catch (ServletException e) {
            Log.error("Error while loading XMLRPC servlet: ", e);
        }
    }

    public void destroy() {
        AuthCheckFilter.removeExclude("gateway/xml-rpc");
        PropertyEventDispatcher.removeListener(this);        
        conduit = null;
    }

    public void propertySet(String property, Map<String, Object> params) {
        if (property.equals("plugin.gateway.xmlrpc.password") && conduit != null) {
            conduit.authPassword = (String)params.get("value");
        }
    }

    public void propertyDeleted(String property, Map<String, Object> params) {
        if (property.equals("plugin.gateway.xmlrpc.password") && conduit != null) {
            conduit.authPassword = null;
        }
    }

    public void xmlPropertySet(String property, Map<String, Object> params) {
        propertySet(property, params);
    }

    public void xmlPropertyDeleted(String property, Map<String, Object> params) {
        propertyDeleted(property, params);
    }

}
