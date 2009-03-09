/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JobStatus {

    private Integer statusId;

    private String statusName;

    public JobStatus() {
    }

    public Integer getStatusId() {
        return this.statusId;
    }

    public void setStatusId(Integer value) {
        this.statusId = value;
    }

    public String getStatusName() {
        return this.statusName;
    }

    public void setStatusName(String value) {
        this.statusName = value;
    }

    /** Static members */
    public static int JOB_PENDING = 1;

    public static int JOB_PROCESSING = 2;

    public static int JOB_FINISHED = 3;

    public static int JOB_ERROR = 4;

    public static String PENDING = "Pending";

    public static String PROCESSING = "Processing";

    public static String FINISHED = "Finished";

    public static String ERROR = "Error";

    /**
     * an unmodifiable map that maps a string representation of the status to
     * the numberic representation
     */
    public static final Map<String, Integer> STATUS_MAP;

    static {
        Map<String, Integer> statusHash = new HashMap<String, Integer>();
        statusHash.put(PENDING, new Integer(JOB_PENDING));
        statusHash.put(PROCESSING, new Integer(JOB_PROCESSING));
        statusHash.put(FINISHED, new Integer(JOB_FINISHED));
        statusHash.put(ERROR, new Integer(JOB_ERROR));
        STATUS_MAP = Collections.unmodifiableMap(statusHash);
    }

}
