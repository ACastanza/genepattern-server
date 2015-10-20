/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genepattern;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.log4j.Logger;

/**
 * Internal representation of a field=value pair in a query string.
 * Prefixed with the 'Gp' so that we don't confuse it with the JAX-RS QueryParam annotation.
 * @author pcarr
 *
 */
public class GpQueryParam {
    private static final Logger log = Logger.getLogger(GpQueryParam.class);
    
    /**
     * Helper method for converting a string into x-www-form-urlencoded format,
     * using the 'UTF-8' format without throwing the UnsupportedEncodingException.
     * 
     * @see URLEncoder#encode(String, String)
     * 
     * @param str
     * @return
     */
    public static final String encodeUtf8(final String str) {
        try {
            final String encodedStr=URLEncoder.encode(str, "UTF-8");
            return encodedStr;
        }
        catch (final UnsupportedEncodingException e) {
            // shouldn't be here on standard Java VM
            ///CLOVER:OFF
            log.error("Unexpected 'UTF-8' encoding exception while encoding '"+str+"'", e);
            return str;
            ///CLOVER:ON
        }
    }


    private final String encodedStr;

    public GpQueryParam(final String field) {
        this(field, (String)null);
    }

    public GpQueryParam(final String field, final String value) {
        if (field==null) {
            log.warn("Unexpected null arg, field=null");
            encodedStr="";
        }
        else {
            final String encodedName=encodeUtf8(field);
            if (value!=null) {
                final String encodedValue=encodeUtf8(value);
                encodedStr=encodedName+"="+encodedValue;
            }
            else {
                encodedStr=encodedName;
            }
        }
    }
    
    /**
     * Gets the URLencoded value for the entire query parameter, 
     *     <encodeName>=<encodedValue>
     */
    public String toString() {
        return encodedStr;
    }
}
