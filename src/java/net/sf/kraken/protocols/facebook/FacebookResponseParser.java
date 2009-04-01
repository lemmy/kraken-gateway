/**
 * $Revision$
 * $Date$
 *
 * Copyright 2009 Daniel Henninger.  All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */
package net.sf.kraken.protocols.facebook;

import org.apache.log4j.Logger;
import org.json.*;
import org.xmpp.packet.Message.Type;

/**
 * A toolkit to parse Facebook responses.
 * 
 * Primarily borrowed from SIP Communicator.
 * 
 * @author Dai Zhiwei
 * @author Daniel Henninger
 * 
 */
public class FacebookResponseParser
{
    private static Logger logger = Logger.getLogger(FacebookResponseParser.class);

    /**
     * Parse the buddylist, store it in to FacebookBuddyList object.
     * 
     * @param response
     * @throws JSONException
     */
    public static int buddylistParser(FacebookAdapter adapter, String response)
        throws JSONException
    {
        if (response == null)
            return -1;
        String prefix = "for (;;);";
        if (response.startsWith(prefix))
            response = response.substring(prefix.length());

        JSONObject respObjs = new JSONObject(response);
        if (respObjs == null)
            return FacebookErrorCode.Error_Global_JSONError;
        logger.info("Facebook: error: " + respObjs.getInt("error"));
        if (respObjs.get("error") != null)
        {
            /*
             * kError_Global_ValidationError = 1346001,
             * kError_Login_GenericError = 1348009,
             * kError_Chat_NotAvailable = 1356002, 
             * kError_Chat_SendOtherNotAvailable = 1356003,
             * kError_Async_NotLoggedIn = 1357001, 
             * kError_Async_LoginChanged = 1357003,
             * kError_Async_CSRFCheckFailed = 1357004,
             * kError_Chat_TooManyMessages = 1356008,
             * kError_Platform_CallbackValidationFailure = 1349007,
             * kError_Platform_ApplicationResponseInvalid = 1349008;
             */

        	int errorCode = respObjs.getInt("error");
            if (errorCode == 0)
            {
                // no error
                try
                {
                    JSONObject payload = (JSONObject) respObjs.get("payload");
                    if (payload != null)
                    {
                        JSONObject buddyList =
                            (JSONObject) payload.get("buddy_list");
                        if (buddyList != null)
                        {
                            //parse and update the buddy list
                            adapter.updateBuddyList(buddyList);
                        }
                    }
                }
                catch (ClassCastException cce)
                {
                    logger.error("Facebook: ", cce);
                    // for
                    // (;;);{"error":0,"errorSummary":"","errorDescription":"No
                    // error.","payload":[],"bootload":[{"name":"js\/common.js.pkg.php","type":"js","src":"http:\/\/static.ak.fbcdn.net\/rsrc.php\/pkg\/60\/106715\/js\/common.js.pkg.php"}]}
                    // "payload":[]
                    // not "{}"
                    // we do nothing
                }
            } /*
                 * else if((Number)respObjs.get("error") == 1346001){ ; } else
                 * if((Number)respObjs.get("error") == 1348009){ ; } else
                 * if((Number)respObjs.get("error") == 1356002){ ; } else
                 * if((Number)respObjs.get("error") == 1356003){ ; } else
                 * if((Number)respObjs.get("error") == 1357001){ ; } else
                 * if((Number)respObjs.get("error") == 1357003){ ; } else
                 * if((Number)respObjs.get("error") == 1357004){ ; } else
                 * if((Number)respObjs.get("error") == 1356008){ ; } else
                 * if((Number)respObjs.get("error") == 1349007){ ; } else
                 * if((Number)respObjs.get("error") == 1349008){ ; }
                 */
            else
            {
                //  handle the error
                logger.warn("Facebook: Error(" + errorCode
                    + "): " + (String) respObjs.get("errorSummary") + ";"
                    + (String) respObjs.get("errorDescription"));
            }
            return errorCode;
        }
        return FacebookErrorCode.Error_Global_JSONError;
    }

    /**
     * Parse the buddylist, get the notifications,
     *  store them in to FacebookNotifications object.
     *  
     * @fixme unused for now
     * @deprecated
     * @param response
     */
    public static void notificationParser(String response)
    {
        if (response == null)
            return;
        String prefix = "for (;;);";
        if (response.startsWith(prefix))
            response = response.substring(prefix.length());
    }

    /**
     * Parse the message posting response, and doing some corresponding things
     * e.g. if it succeeds, we do nothing; else if we get some error, we print
     * them.
     * 
     * @param response
     * @throws JSONException
     */
    public static void messagePostingResultParser(FacebookAdapter adapter, 
        String msg, String to, String response) throws JSONException
    {
        if (response == null)
            throw new NullPointerException("The parameter response is null");
        String prefix = "for (;;);";
        if (response.startsWith(prefix))
            response = response.substring(prefix.length());

        // for (;;);{"error":0,"errorSummary":"","errorDescription":"No
        // error.","payload":[],"bootload":[{"name":"js\/common.js.pkg.php","type":"js","src":"http:\/\/static.ak.fbcdn.net\/rsrc.php\/pkg\/60\/106715\/js\/common.js.pkg.php"}]}
        // for (;;);{"error":1356003,"errorSummary":"Send destination not
        // online","errorDescription":"This person is no longer
        // online.","payload":null,"bootload":[{"name":"js\/common.js.pkg.php","type":"js","src":"http:\/\/static.ak.fbcdn.net\/rsrc.php\/pkg\/60\/106715\/js\/common.js.pkg.php"}]}
        JSONObject respObjs = new JSONObject(response);
        if (respObjs == null)
            throw new NullPointerException(
                "Failed to parse JSONObject from the parameter response");
        logger.info("Facebook: error: " + respObjs.getInt("error"));
        if (respObjs.get("error") != null)
        {
            /*
             * kError_Global_ValidationError = 1346001,
             * kError_Login_GenericError = 1348009,
             * kError_Chat_NotAvailable = 1356002, 
             * kError_Chat_SendOtherNotAvailable = 1356003,
             * kError_Async_NotLoggedIn = 1357001, 
             * kError_Async_LoginChanged = 1357003,
             * kError_Async_CSRFCheckFailed = 1357004,
             * kError_Chat_TooManyMessages = 1356008,
             * kError_Platform_CallbackValidationFailure = 1349007,
             * kError_Platform_ApplicationResponseInvalid = 1349008;
             */

            int errorCode = respObjs.getInt("error");
            String errorString =
                "Error(" + errorCode + "): "
                    + (String) respObjs.get("errorSummary") + "; "
                    + (String) respObjs.get("errorDescription");

            logger.warn("Facebook: "+errorString);

            if (errorCode == FacebookErrorCode.Error_Global_NoError)
            {
                // "error":0, no error
                return;
            }
            else
            {
                // notify the posting error
                adapter.getSession().getTransport().sendMessage(adapter.getSession().getJID(),
                        adapter.getSession().getTransport().getJID(),
                        respObjs.get("errorSummary").toString(), Type.error);
                return;
//                MessageDeliveryFailedEvent mdfe =
//                    new MessageDeliveryFailedEvent(msg, to, errorCode,
//                        new Date(), (String) respObjs.get("errorSummary"));
//                return mdfe;
                
            }
        }
        throw new JSONException("The response has no \'error\' code field");
    }

    /**
     * Parse the message request response, and doing some corresponding things<br>
     * e.g. get the message text, put it in the corresponding chatroom.
     * 
     * @param response
     * @throws JSONException
     */
    public static void messageRequestResultParser(FacebookAdapter adapter,
        String response) throws JSONException
    {
        if (response == null)
            return;
        String prefix = "for (;;);";
        if (response.startsWith(prefix))
            response = response.substring(prefix.length());

        JSONObject respObjs = new JSONObject(response);
        logger.info("t: " + (String) respObjs.get("t"));
        if (respObjs.get("t") != null)
        {
            if (((String) respObjs.get("t")).equals("msg"))
            {
                JSONArray ms = (JSONArray) respObjs.get("ms");
                logger.info("Facebook: NO of msges: " + ms.length());
                // Iterator<JSONObject> it = ms..iterator();
                int index = 0;
                while (index < ms.length())
                {
                    JSONObject msg = ms.getJSONObject(index);
                    index++;
                    if (msg.get("type").equals("typ"))
                    {
                        // got a typing notification
                        // for
                        // (;;);{"t":"msg","c":"p_1386786477","ms":[{"type":"typ","st":0,"from":1190346972,"to":1386786477}]}
                        int facebookTypingState = msg.getInt("st");
                        Long from = msg.getLong("from");
                        if (!from.toString().equals(adapter.getUID()))
                            adapter.promoteTypingNotification(from.toString(),
                                facebookTypingState);
                    }
                    else if (msg.get("type").equals("msg"))
                    {
                        // the message itself
                        JSONObject realmsg = (JSONObject) msg.get("msg");
                        /*
                         * {"text":"FINE", "time":1214614165139,
                         * "clientTime":1214614163774, "msgID":"1809311570"},
                         * "from":1190346972, "to":1386786477,
                         * "from_name":"David Willer", "to_name":"\u5341\u4e00",
                         * "from_first_name":"David", "to_first_name":"\u4e00"}
                         */
                        FacebookMessage fm = new FacebookMessage();
                        fm.text = (String) realmsg.get("text");
                        fm.time = (Number) realmsg.get("time");
                        fm.clientTime = (Number) realmsg.get("clientTime");
                        fm.msgID = (String) realmsg.get("msgID");

                        // the attributes of the message
                        fm.from = (Number) msg.get("from");
                        fm.to = (Number) msg.get("to");
                        fm.fromName = (String) msg.get("from_name");
                        fm.toName = (String) msg.get("to_name");
                        fm.fromFirstName = (String) msg.get("from_first_name");
                        fm.toFirstName = (String) msg.get("to_first_name");

                        if (adapter.isMessageHandledBefore(fm.msgID))
                        {
                            logger.trace("Omitting a already handled message: msgIDCollection.contains(msgID)");
                            continue;
                        }
                        adapter.addMessageToCollection(fm.msgID);

                        printMessage(fm);

                        if (!fm.from.toString().equals(adapter.getUID()))
                            adapter.promoteMessage(fm);
                    }
                }
            }
            //refresh means that the session or post_form_id is invalid
            else if (((String) respObjs.get("t")).equals("refresh"))
            {
                logger.trace("Facebook: Refresh");// do nothing
                if (((String) respObjs.get("seq")) != null)
                {
                    logger.trace("Facebook: refresh seq: "
                        + (String) respObjs.get("seq"));
                }
            }
            //continue means that the server wants us to remake the connection
            else if (((String) respObjs.get("t")).equals("continue"))
            {
                logger.trace("Facebook: Time out? reconnect...");// do nothing
            }
            else
            {
                logger.warn("Facebook: Unrecognized response type: "
                    + (String) respObjs.get("t"));
            }
        }
    }

    /**
     * For debuging
     * 
     * @param msg
     */
    public static void printMessage(FacebookMessage msg)
    {
        logger.trace("Facebook: text:\t" + msg.text);
        logger.trace("Facebook: time:\t" + msg.time);
        logger.trace("Facebook: clientTime:\t" + msg.clientTime);
        logger.trace("Facebook: msgID:\t" + msg.msgID);
        logger.trace("Facebook: from:\t" + msg.from);
        logger.trace("Facebook: to:\t" + msg.to);
        logger.trace("Facebook: from_name:\t" + msg.fromName);
        logger.trace("Facebook: to_name:\t" + msg.toName);
        logger.trace("Facebook: from_first_name:\t" + msg.fromFirstName);
        logger.trace("Facebook: to_first_name:\t" + msg.toFirstName);
    }
}