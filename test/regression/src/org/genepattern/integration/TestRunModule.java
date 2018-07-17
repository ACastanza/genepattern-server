package org.genepattern.integration;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.genepattern.client.GPClient;
import org.genepattern.io.IOUtil;
import org.genepattern.io.ParseException;
import org.genepattern.io.odf.OdfObject;
import org.genepattern.matrix.Dataset;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;
import org.genepattern.webservice.WebServiceException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the ability to run GenePattern Modules.
 * @author jnedzel
 *
 */
public class TestRunModule extends TestCase {
    public static final String DATA_DIR=System.getProperty("data.dir", "resources/data");
	
	public static final String PREPROCESS_LSID = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00020";
	public static final String CONVERT_LSID = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002";
	public static final String SVM_LSID = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00025";

	public static final String SMALL_GCT = DATA_DIR+"/small.gct";
	public static final String SMALL_PREPROCESS_RESULT_GCT = "small.preprocessed.gct";	
	public static final String SMALL_CONVERT_RESULT_GCT = "small.cvt.gct";
	
	public static final String ALL_AML_RES_URL = "ftp://ftp.broadinstitute.org/pub/genepattern/all_aml/all_aml_train.res";
	public static final String ALL_AML_RES_PREPROCESS_RESULT = "all_aml_train.preprocessed.res";
	public static final String ALL_AML_RES_CONVERT_RESULT = "all_aml_train.cvt.res";	
	public static final String ALL_AML_RES_CONVERT_RERUN_RESULT = "all_aml_train.cvt.cvt.res";	
	
	public static final String TRAIN_DATA_FILENAME = DATA_DIR+"/all_aml_train.gct";
	public static final String TRAIN_CLS_FILENAME = DATA_DIR+"/all_aml_train.cls";
	public static final String TEST_DATA_FILENAME = DATA_DIR+"/all_aml_test.gct";
	public static final String TEST_CLS_FILENAME = DATA_DIR+"/all_aml_test.cls";
	public static final String SVM_ODF_RESULT = "all_aml_test.pred.odf";
	
	
	/**
	 * Submits a PreprocessDataset job to GenePattern.  Input file is uploaded from client.
	 * 
	 * Tests a Java module.
	 * 
	 * @throws WebServiceException 
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws ParseException 
	 */
	public void testPreprocessUploadedFile() throws WebServiceException, SecurityException, IOException, ParseException {
		File testFile = new File(SMALL_GCT);		
		Parameter[] params = new Parameter[1];
		Parameter fileParam = new Parameter("input.filename", testFile);
		params[0] = fileParam;
		File[] files = runModule(params, PREPROCESS_LSID, 1);
		validateResultFile(files, SMALL_PREPROCESS_RESULT_GCT, 2, 3);
	}

	/**
	 * Validates the result file.  Checks the result filename against the expected filename.
	 * Opens the dataset and compares the number of rows and columns against the expected values.
	 * @param files - downloaded GenePattern result files.
	 * @param expectedFilename Expected name of the result file containing microarray data.
	 * @param expectedNumRows expected number of rows in result file dataset.
	 * @param expectedNumColumns expected number of columns in result file dataset.
	 * @throws IOException
	 * @throws ParseException
	 */
	private void validateResultFile(File[] files, String expectedFilename, int expectedNumRows, int expectedNumColumns) 
	throws IOException, ParseException 
	{
		if (!expectedFilename.equals(files[0].getName())) {
			fail("found unexpected output file: " + files[0].getName());
		}
		
		Dataset dataset = IOUtil.readDataset(files[0].getAbsolutePath());
		assertNotNull(dataset);
		assertEquals(expectedNumRows, dataset.getRowCount());
		assertEquals(expectedNumColumns, dataset.getColumnCount());
	}

	/**
	 * Runs the module on GenePattern and downloads the result files.
	 * @param params Parameters for the run.
	 * @param lsid Lsid of module to run.
	 * @param expectedNumFiles expected number of result files
	 * @return Downloaded result files.
	 * @throws WebServiceException
	 * @throws IOException
	 */
	private File[] runModule(Parameter[] params, String lsid, int expectedNumFiles)
			throws WebServiceException, IOException {
		JobResult jobResult = runModuleHelper(params, lsid);
		File[] files = jobResult.downloadFiles(".");
		assertNotNull(files);
		assertEquals(expectedNumFiles, files.length);
		return files;
	}

    /**
     * Runs the module on GenePattern and returns the jobResult.
     * @param params Parameters for the run.
     * @param lsid LSID of the module to run on GenePattern.
     * @return jobResult for the module
     * @throws WebServiceException
     */
    private JobResult runModuleHelper(Parameter[] params, String lsid) 
    throws WebServiceException {
        GenePatternConfig config = new GenePatternConfig();
        GPClient client = new GPClient(config.getGenePatternUrl(), config.getUsername(), config.getPassword());
        assertNotNull(client);
        JobResult jobResult = client.runAnalysis(lsid, params);
        assertNotNull(jobResult);
        String jobNum = "UNKNOWN";
        try {
            jobNum = ""+jobResult.getJobNumber();
        }
        catch (Throwable t) {
            //ignore
        }
        assertFalse("job #"+jobNum+" has stderr", jobResult.hasStandardError());
        return jobResult;
    }
	
	/**
	 * Submits a PreprocessDataset job to GenePattern.  Input file is an url to a public
	 * Broad url.
	 * 
	 * Tests a Java module.
	 * 
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws WebServiceException 
	 * @throws ParseException 
	 */
	public void testPreprocessExternalUrl() throws SecurityException, IOException, WebServiceException, ParseException {
		Parameter[] params = new Parameter[1];
		Parameter fileParam = new Parameter("input.filename", ALL_AML_RES_URL);
		params[0] = fileParam;
		File[] files = runModule(params, PREPROCESS_LSID, 1);
		validateResultFile(files, ALL_AML_RES_PREPROCESS_RESULT, 5864, 38);
	}
	
	/**
	 * Runs a PreprocessDataset job using an external URL, then takes result and
	 * runs Preprocess again, using the GenePattern url to the result from the 
	 * first run.
	 * @throws SecurityException
	 * @throws IOException
	 * @throws WebServiceException
	 */
	public void testPreprocessGpUrl() throws SecurityException, IOException, WebServiceException {
		File testFile = new File(SMALL_GCT);		
		Parameter[] params = new Parameter[1];
		Parameter fileParam = new Parameter("input.filename", testFile);
		params[0] = fileParam;
		
		JobResult jobResult = runModuleHelper(params, PREPROCESS_LSID);
		File[] files = jobResult.downloadFiles(".");
		assertNotNull(files);
		assertEquals(1, files.length);		
		
		URL inputUrl = jobResult.getURLForFileName(SMALL_PREPROCESS_RESULT_GCT);
		assertNotNull(inputUrl);
		
		fileParam = new Parameter("input.filename", inputUrl.toString());
		params[0] = fileParam;
		
		files = runModule(params, PREPROCESS_LSID, 1);
//		@TODO validate this 
	}

    public void testComparativeMarkerSelectionExternalUrl() {
        //final String lsid = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:7";
        final String lsid = "ComparativeMarkerSelection";
        Parameter[] params = new Parameter[2];
        params[0] = new Parameter("input.file", "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/protocols/all_aml_test.preprocessed.gct");
        params[1] = new Parameter("cls.file", "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.cls");
        
        JobResult jobResult = null;
        try {
            jobResult = runModuleHelper(params, lsid);
        }
        catch (WebServiceException e) {
            fail("WebServiceException thrown running module, lsid="+lsid+". Exception was: "+e.getLocalizedMessage());
        }
        File[] files = new File[0];
        try {
            files = jobResult.downloadFiles(".");
        }
        catch (IOException e) {
            fail("IOException thrown by jobResult.downloadFiles. Exception was: "+e.getLocalizedMessage());
        }
	    assertNotNull(files);
	    assertEquals("check # of result files", 1, files.length);
	}
    
    public void testHierarchicalClusteringExternalUrl() {
        //final String lsid="HierarchicalClustering";
        final String lsid = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00009:6";
        Parameter[] params = new Parameter[1];
        params[0] = new Parameter("input.filename", "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/protocols/all_aml_test.preprocessed.gct");
        
        JobResult jobResult = null;
        try {
            jobResult = runModuleHelper(params, lsid);
        }
        catch (WebServiceException e) {
            fail("WebServiceException thrown running module, lsid="+lsid+". Exception was: "+e.getLocalizedMessage());            
        }
        File[] files = new File[0];
        try {
            files = jobResult.downloadFiles(".");
        }
        catch (IOException e) {
            fail("IOException thrown by jobResult.downloadFiles. Exception was: "+e.getLocalizedMessage());
        }
        assertNotNull(files);
        assertEquals("check # of result files", 2, files.length);
    }
	
	/**
	 * Submits a ConvertLineEndings job to Genepattern.  Input file is an uploaded file.
	 * 
	 * Tests a Perl module.
	 * @throws SecurityException
	 * @throws IOException
	 * @throws WebServiceException
	 * @throws ParseException
	 */
	public void testConvertLineEndingsUploadedFile() throws SecurityException, IOException, WebServiceException, ParseException {
		File testFile = new File(SMALL_GCT);		
		Parameter[] params = new Parameter[1];
		Parameter fileParam = new Parameter("input.filename", testFile);
		params[0] = fileParam;
		File[] files = runModule(params, CONVERT_LSID, 1);
		validateResultFile(files, SMALL_CONVERT_RESULT_GCT, 2, 3);
	}
	
	
	/**
	 * Submits a ConvertLineEndings job to Genepattern.  Input file is an url to a public
	 * Broad url.  
	 * 
	 * Tests a Perl module.
	 * @throws SecurityException
	 * @throws IOException
	 * @throws WebServiceException
	 * @throws ParseException
	 */
	public void testConvertLineEndingsExternalUrl() throws SecurityException, IOException, WebServiceException, ParseException {
		Parameter[] params = new Parameter[1];
		Parameter fileParam = new Parameter("input.filename", ALL_AML_RES_URL);
		params[0] = fileParam;
		File[] files = runModule(params, CONVERT_LSID, 1);
		validateResultFile(files, ALL_AML_RES_CONVERT_RESULT, 7129, 38);
	}
	
	public void testConvertLineEndingsGpUrl() throws WebServiceException, IOException, ParseException {
		Parameter[] params = new Parameter[1];
		Parameter fileParam = new Parameter("input.filename", ALL_AML_RES_URL);
		params[0] = fileParam;
		JobResult jobResult = runModuleHelper(params, CONVERT_LSID);
		File[] files = jobResult.downloadFiles(".");
		assertNotNull(files);
		assertEquals(1, files.length);
		
		URL inputUrl = jobResult.getURLForFileName(ALL_AML_RES_CONVERT_RESULT);
		assertNotNull(inputUrl);
		
		fileParam = new Parameter("input.filename", inputUrl.toString());
		params[0] = fileParam;
		
		files = runModule(params, CONVERT_LSID, 1);
		validateResultFile(files, ALL_AML_RES_CONVERT_RERUN_RESULT, 7129, 38);
	}
	
	public void testSvmUploadedFiles() throws WebServiceException, IOException, ParseException {
		Parameter[] params = new Parameter[4];
		params[0] = new Parameter("train.data.filename", TRAIN_DATA_FILENAME);
		params[1] = new Parameter("train.cls.filename", TRAIN_CLS_FILENAME);
		params[2] = new Parameter("test.data.filename", TEST_DATA_FILENAME);		
		params[3] = new Parameter("test.cls.filename", TEST_CLS_FILENAME);

		JobResult jobResult = runModuleHelper(params, SVM_LSID);
		assertNotNull(jobResult);
		File[] files = jobResult.downloadFiles(".");
		assertNotNull(files);
		assertEquals(2, files.length);		
		
		
		File odfResult = jobResult.downloadFile(SVM_ODF_RESULT, ".");
		assertNotNull(odfResult);
		// TODO get revised gp-modules.jar from Josh and read in file using OdfObject
		OdfObject odf = new OdfObject(odfResult.getAbsolutePath());
	}
	
	public void testFlamePreprocess() throws WebServiceException {
	    String lsid = "FLAMEPreprocess";
	    String dataset = "ftp://ftp.broadinstitute.org/pub/genepattern/example_files/FLAME/SMALL_phospho.lymphgated.fcs.zip";
	    Parameter[] params = new Parameter[3];
	    params[0] = new Parameter("dataset", dataset);
	    params[1] = new Parameter("channels", "3,4,5,7");
	    params[2] = new Parameter("channel.names", "SLP76,ZAP70,CD4,CD45RA");
	    
	    JobResult jobResult = runModuleHelper(params, lsid);
	    assertNotNull(jobResult);
	    
	}

	public static Test suite() {
		  TestSuite suite= new TestSuite();
		  suite.addTestSuite(TestRunModule.class);
		  return suite;
	}
	
	

}
