/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2008) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


package edu.mit.genome.gp;

import junit.framework.TestCase;
import edu.mit.wi.omnigene.framework.analysis.*;
import edu.mit.wi.omnigene.util.*;
import edu.mit.wi.omniview.analysis.*;
import edu.mit.wi.omnigene.framework.analysis.webservice.client.*;
import java.io.*;
import java.net.*;
import java.util.*;
import edu.mit.broad.util.*;

public class TestKSscore extends TestCallTasks {
    
    public TestKSscore(String name){
        super(name);
    }

    
    public void testSimpleCall() throws Exception{
    	//FIXME need input files
        AnalysisService svc = (AnalysisService)serviceMap.get("KSscore.lamb");
        ParameterInfo params[] = new ParameterInfo[3];
        params[0] = getInputFileParam("query.filename","XXX");
        params[1] = new ParameterInfo("output","out", "");
        params[2] = getInputFileParam("input.filename","XXX");
        // call and wait for completion or error
    /*    AnalysisJob job = submitJob(svc, params);
        JobInfo jobInfo = waitForErrorOrCompletion(job);
        // look for successful completion (not an error)        
        assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
		assertNoStddErrFileGenerated(job);  
		*/
		fail("Not implemented yet. Need approprate input files. ");
    }
    
   }
