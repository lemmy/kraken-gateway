/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.sf.kraken.protocols.facebook;

import java.util.*;

import org.apache.log4j.Logger;
import org.json.*;

/**
 * Facebook buddy list that store the online buddies' information we got from
 * the server since we logged in. Some information of ourselves also included.
 * 
 * @author Dai Zhiwei
 * 
 */
public class FacebookBuddyList
{
    private static Logger logger = Logger.getLogger(FacebookBuddyList.class);
    /**
     * Some information of ourselves/myself.
     */
    private transient FacebookUser me;

    /**
     * Our (online) buddies' information cache
     */
    private transient Map<String, FacebookUser> buddyCache;

    /**
     * If is the list changed in the past a period of time.
     * NOT used currently
     */
    private Boolean listChanged;

    /**
     * Count of the available buddyies
     * NOT used currently
     */
    private Number availableCount;

    /**
     * The adapter of current account.
     */
    private FacebookAdapter adapter;

    /**
     * Init the cache and the parent adapter.
     * 
     * @param adapter
     */
    public FacebookBuddyList(FacebookAdapter adapter)
    {
    	buddyCache = new Hashtable<String, FacebookUser>();
        buddyCache.clear();
        this.adapter = adapter;
    }

    /**
     * Update the buddy list according to the given JSON object
     * 
     * @param buddyList the buddy list we just got from the server
     * @throws JSONException
     */
    public void updateBuddyList(JSONObject buddyList) throws JSONException
    {
        logger.info("Updating buddy list...");
        // JSONObject buddyList = (JSONObject) payload.get("buddy_list");
        listChanged = (Boolean) buddyList.get("listChanged");
        availableCount = (Number) buddyList.get("availableCount");

        logger.trace("listChanged: " + listChanged);
        logger.trace("availableCount: " + availableCount);

        //if listChanged, then we can get the buddies available via looking at the nowAvailableList
        //else. we can only get the buddies' info, and the nowAvailableList is empty.
        
        JSONObject userInfos = (JSONObject) buddyList.get("userInfos");
        if(userInfos == null)
        	return;
        
        //First we set all the buddies in the cache as offline,
        Iterator cacheIt=buddyCache.entrySet().iterator();
        while(cacheIt.hasNext()){
            Map.Entry entry=(Map.Entry)cacheIt.next();
            FacebookUser user=(FacebookUser)entry.getValue();
            user.isOnline = false;
        }
        //Then add the new buddies and set them as online(constructor)
        Iterator<String> it = userInfos.keys();
        while (it.hasNext())
        {
            String key = it.next();
            logger.debug("userID: " + key);
            JSONObject user = (JSONObject) userInfos.get(key);
            if(user == null)
            	continue;
            //default status is online and idle
            FacebookUser fu = new FacebookUser(key, user);
            if(fu == null)
            	continue;
            //get my own information
            if(key.equals(adapter.getUID())){
            	me = fu;
            	continue;
            }
            //not my information
            buddyCache.put(key, fu);
            printUserInfo(fu);
        }
        //The third, we set the idle status
        if(listChanged){
        	JSONObject nowAvailableList =
                (JSONObject) buddyList.get("nowAvailableList");

            if (nowAvailableList == null)
                return;// it's an/a exception/surprise.
            
            it = nowAvailableList.keys();
            while (it.hasNext())
            {
                String key = it.next();
                logger.debug("userID: " + key);
                JSONObject status = (JSONObject) nowAvailableList.get(key);
                if(status == null)
                	continue;
                buddyCache.get(key).isIdle = status.getBoolean("i");
            }
        }
        //At last, the best part: updating the contact list.
//        cacheIt=buddyCache.entrySet().iterator();
//        while(cacheIt.hasNext())
//        {
//            Map.Entry entry=(Map.Entry)cacheIt.next();
//            String uid = (String)entry.getKey();
//            FacebookUser user=(FacebookUser)entry.getValue();
//            if(user.isOnline && user.isIdle)
//            	operationSetPresence.setPresenceStatusForContact(uid, FacebookStatusEnum.IDLE);
//            else if(user.isOnline)
//            	operationSetPresence.setPresenceStatusForContact(uid, FacebookStatusEnum.ONLINE);
//            else
//            	operationSetPresence.setPresenceStatusForContact(uid, FacebookStatusEnum.OFFLINE);
//        }
    }
    /**
     * Get meta info of this account
     * 
     * @return meta info of this account
     */
    public FacebookUser getMyMetaInfo()
    {
        return me;
    }

    /**
     * For debuging
     * 
     * @param user
     */
    private static void printUserInfo(FacebookUser user)
    {
        logger.debug("name:\t" + user.name);
        logger.debug("firstName:\t" + user.firstName);
        logger.debug("thumbSrc:\t" + user.thumbSrc);
        logger.debug("status:\t" + user.status);
        logger.debug("statusTime:\t" + user.statusTime);
        logger.debug("statusTimeRel:\t" + user.statusTimeRel);
    }

    /**
     * Get our buddy who has the given id from the cache.
     * 
     * @param contactID the id we wanna look up
     * @return the buddy who has the given id
     */
    public FacebookUser getBuddyFromCacheByID(String contactID)
    {
        return buddyCache.get(contactID);
    }

    /**
     * Release the resource
     */
    public void clear()
    {
        me = null;
        buddyCache.clear();
    }
}
