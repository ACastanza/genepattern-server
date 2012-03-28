/*
 * Copyright © 2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 */

package org.genepattern.pipelines;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class ResponseJSON extends JSONObject {
    public static Logger log = Logger.getLogger(ResponseJSON.class);

    public void addChild(Object key, JSONObject object) {
        try {
            this.put(key.toString(), object);
        }
        catch (JSONException e) {
            log.error("Error attaching object to ResponseJSON: " + object);
        }
    }
    
    public void addChild(Object key, String value) {
        try {
            this.put(key.toString(), value);
        }
        catch (JSONException e) {
            log.error("Error attaching String to ResponseJSON: " + value);
        }
    }
    
    public void addMessage(String message) {
        try {
            this.put("MESSAGE", message);
        }
        catch (JSONException e) {
            log.error("Error attaching message to ResponseJSON: " + message);
        }
    }
    
    public void addError(String message) {
        try {
            this.put("ERROR", message);
        }
        catch (JSONException e) {
            log.error("Error attaching error to ResponseJSON: " + message);
        }
    }
}
