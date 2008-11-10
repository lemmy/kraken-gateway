/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2006-2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.openfire.gateway.util;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A simple class to perform various string related functions.
 *
 * @author Daniel Henninger
 */
public class StringUtils {

    /**
     * Convenience routine to perform a string join for groups in the database.
     * @param array Array of strings to join together.
     * @param delim Delimiter to separate strings with.
     * @return Joined string
     */
    public static String join( List<String> array, String delim ) {
        StringBuffer sb = join(array, delim, new StringBuffer());
        return sb.toString();
    }

    /**
     * Helper function for primary use join function.
     * @param array Array of strings to join together.
     * @param delim Delimiter to separate strings with.
     * @param sb String buffer instance to work from.
     * @return String buffer instance.
     */
    static StringBuffer join( List<String> array, String delim, StringBuffer sb ) {
        Boolean first = true;
        for (String s : array) {
            if (!first) {
                sb.append(delim);
            }
            else {
                first = false;
            }
            sb.append(s);
        }
        return sb;
    }

    /**
     *  Regular Expressions
     */

    /* HTML looking tags */
    private static final Pattern htmlRE = Pattern.compile("<[^>]*>");

    /* Newlines */
    private static final Pattern newlineRE = Pattern.compile("<br/?>", Pattern.CASE_INSENSITIVE);

    /**
     * Strips HTML tags fairly loosely, trusting that html tags will look like
     * &lt;whatever&gt;.  Before stripping these tags, it tries to convert known tags
     * to text versions, such as newlines.
     *
     * @param str the string from which to strip HTML tags
     * @return the given string with HTML tags removed
     */
    public static String convertFromHtml(String str) {
        str = newlineRE.matcher(str).replaceAll("\\\n");
        str = htmlRE.matcher(str).replaceAll("");
        str = org.jivesoftware.util.StringUtils.unescapeFromXML(str);
        return str;
    }

}
