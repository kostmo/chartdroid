/*
 * Copyright (c) 2005 Aetrion LLC.
 */
package com.aetrion.flickr.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * String utility methods.
 *
 * @author Anthony Eden
 * @version $Id: StringUtilities.java,v 1.4 2007/09/09 17:13:39 x-mago Exp $
 */
public class StringUtilities {

    private StringUtilities() {

    }

    /**
     * Join the array of Strings using the specified delimiter.
     *
     * @param s The String array
     * @param delimiter The delimiter String
     * @return The joined String
     */
    public static String join(String[] s, String delimiter) {
        return join(s, delimiter, false);
    }
    
    public static String join(String[] s, String delimiter, boolean doQuote) {
        return join(Arrays.asList(s), delimiter, doQuote);
    }
    
    /**
     * Join the Collection of Strings using the specified delimiter and
     * optionally quoting each
     * @param s The String collection
     * @param delimiter the delimiter String
     * @param doQuote whether or not to quote the Strings
     * @return The joined String
     */
    public static String join( Collection s, String delimiter, boolean doQuote ) {
        StringBuffer buffer = new StringBuffer();
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            if( doQuote ) {
              buffer.append( "\"" + iter.next() + "\"" );
            }
            else {
                buffer.append(iter.next());
            }
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }

    /**
     * Join the Collection of Strings using the specified delimiter.
     *
     * @param s The String collection
     * @param delimiter The delimiter String
     * @return The joined String
     */
    public static String join(Collection s, String delimiter) {
      return join( s, delimiter, false );
    }

}
