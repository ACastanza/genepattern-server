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

public class TestVariationFilter extends TestCallTasks {
    
    public TestVariationFilter(String name){
	super(name);
    }

    
    public void testSimpleCall() throws Exception{
	AnalysisService svc = (AnalysisService)serviceMap.get("VariationFilter");
	String outFileName = "vfout";

	// set up parameters
	ParameterInfo params[] = new ParameterInfo[8];
	params[0] = new ParameterInfo("low", "0","");
	params[1] = new ParameterInfo("high", "0","");
	params[2] = new ParameterInfo("min.fold", "3.0","");
	params[3] = new ParameterInfo("min.difference", "100.0","");
	params[4] = new ParameterInfo("ceiling", "None","");
	params[5] = new ParameterInfo("threshold", "None","");
	params[6] =  getInputFileParam("input.filename","ALL_vs_AML_train_set_38_sorted.res");
	params[7] = new ParameterInfo("output.name", outFileName,"");

	System.out.println("Calling");
	// call and wait for completion or error
	AnalysisJob job = submitJob(svc, params);
	JobInfo jobInfo = waitForErrorOrCompletion(job);
	
	// look for successful completion (not an error)
	assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	//assertNoStddErrFileGenerated(job);  // ?? change variation filter to not report warnings to stderr
	assertFileWithNameGenerated(job, outFileName);
    }
        


}

