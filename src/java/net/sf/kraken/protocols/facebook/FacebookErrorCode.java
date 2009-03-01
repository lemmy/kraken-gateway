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

/**
 * Facebook Error Code
 * 
 * Primarily borrowed from SIP Communicator
 * 
 * @author Dai Zhiwei
 * @author Daniel Henninger
 * 
 */
public class FacebookErrorCode
{
    public static final int kError_Global_ValidationError = 1346001;

    public static final int kError_Login_GenericError = 1348009;

    public static final int kError_Platform_CallbackValidationFailure = 1349007;

    public static final int kError_Platform_ApplicationResponseInvalid = 1349008;

    public static final int kError_Chat_NotAvailable = 1356002;

    public static final int kError_Chat_SendOtherNotAvailable = 1356003;

    public static final int kError_Chat_TooManyMessages = 1356008;

    public static final int kError_Async_NotLoggedIn = 1357001;

    public static final int kError_Async_LoginChanged = 1357003;

    public static final int kError_Async_CSRFCheckFailed = 1357004;
    //Bad Parameter; There was an error understanding
    // the request.
    public static final int Error_Async_BadParameter = 1357005;

    public static final int Error_Global_NoError = 0;

    public static final int Error_Async_HttpConnectionFailed = 1001;

    public static final int Error_Async_UnexpectedNullResponse = 1002;

    public static final int Error_System_UIDNotFound = 1003;

    public static final int Error_System_ChannelNotFound = 1004;

    public static final int Error_System_PostFormIDNotFound = 1005;

    public static final int Error_Global_PostMethodError = 1006;

    public static final int Error_Global_GetMethodError = 1007;

    public static final int Error_Global_JSONError = 1008;
}
