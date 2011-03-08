/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2009) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 *
 *******************************************************************************/
package org.genepattern.server.webapp.jsf.jobinfo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.JobInfoWrapper;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserProp;
import org.genepattern.server.webapp.jsf.UIBeanHelper;

/**
 * Access job status for a single job result from a JSF page.
 * 
 * @author pcarr
 */
public class JobStatusBean {
    private static Logger log = Logger.getLogger(JobStatusBean.class);
    
    private JobInfoWrapper jobInfoWrapper = null;
    private List<JobInfoWrapper> allSteps = null;
    private String currentUserId = null;
    private String currentUserEmail = null;

    private boolean sendEmailNotification = false;
    private boolean showExecutionLogs = false;
    private boolean openVisualizers = false;

    //track the list of automatically opened visualizers
    private Map<Integer,String> visualizerStatus = new HashMap<Integer,String>();
    
    public JobStatusBean() {
      init();
    }
    
    public void init(){
        allSteps = null;

        //get the job number from the request parameter
        int jobNumber = -1;
        String jobNumberParameter = null;
        jobNumberParameter = UIBeanHelper.getRequest().getParameter("jobNumber");
        jobNumberParameter = UIBeanHelper.decode(jobNumberParameter);
        if (jobNumberParameter == null) {
            log.warn("init(): Missing jobNumber.");
            return;
        }
        try {
            jobNumber = Integer.parseInt(jobNumberParameter);
        }
        catch (NumberFormatException e) {
            log.error("init(): Invalid jobNumber="+jobNumberParameter+": "+e.getLocalizedMessage());
            return;
        }

        String openVisualizersParameter = UIBeanHelper.getRequest().getParameter("openVisualizers");
        setOpenVisualizers(openVisualizersParameter != null);

        currentUserId = UIBeanHelper.getUserId();
        UserDAO userDao = new UserDAO();
        User user = userDao.findById(currentUserId);
        if (user != null) {
            currentUserEmail = user.getEmail();
            showExecutionLogs = userDao.getPropertyShowExecutionLogs(currentUserId);
        }

        try {
            String key = UserProp.getEmailNotificationPropKey(jobNumber);
            if (key != null) {
                String propValue = userDao.getPropertyValue(currentUserId, key, String.valueOf(sendEmailNotification));
                sendEmailNotification = Boolean.valueOf(propValue);
            }
        }
        catch (Exception e) {
            String errorMessage = "Unable to initialize email notification for user: '"+currentUserId+"': "+e.getLocalizedMessage();
            UIBeanHelper.setErrorMessage(errorMessage);
        }

        HttpServletRequest request = UIBeanHelper.getRequest();
        String contextPath = request.getContextPath();
        String cookie = request.getHeader("Cookie");   
        JobInfoManager jobInfoManager = new JobInfoManager();
        this.jobInfoWrapper = jobInfoManager.getJobInfo(cookie, contextPath, currentUserId, jobNumber);
          
        if (jobInfoWrapper == null) {
            String errorMessage = "Job # "+jobNumber+" is deleted.";
            UIBeanHelper.setErrorMessage(errorMessage);
            try {
                HttpServletResponse response = UIBeanHelper.getResponse();
                response.sendError(HttpServletResponse.SC_NOT_FOUND, errorMessage);
            }
            catch (IOException e) {
                log.error("Error sending error: "+e.getMessage(), e);
            }
        }
        
        //special-case for visualizers
        if (jobInfoWrapper != null) {
            visualizerStatus = new HashMap<Integer,String>(); 
            for(JobInfoWrapper step : jobInfoWrapper.getAllSteps()) {
                if (step.isVisualizer()) {
                    visualizerStatus.put(step.getJobNumber(), "PING");
                }
            }
        }
    }
    
    public JobInfoWrapper getJobInfo() {
        return jobInfoWrapper;
    }

    public boolean getOpenVisualizers() {
        return openVisualizers;
    }

	public void setOpenVisualizers(boolean openVisualizers) {
		this.openVisualizers = openVisualizers;
	}

    /**
     * @return the top level job info, including all steps if it is a pipeline.
     */
    public List<JobInfoWrapper> getAllSteps() {
        if (allSteps == null) {
            allSteps = jobInfoWrapper.getAllSteps();
            allSteps.add(0, jobInfoWrapper);
        }
        return allSteps;
    }

    public boolean isFinished() {
        boolean  finished = 
            jobInfoWrapper != null && jobInfoWrapper.isFinished();
        return finished;
    }

    public boolean isShowExecutionLogs() {
        return this.showExecutionLogs;
    }
    
    public boolean isSendEmailNotification() {
        return sendEmailNotification;
    }

    /**
     * @return the userId of the logged in user,
     *         not necessarily the same as the owner of the job.
     */
    public String getCurrentUserId() {
        return currentUserId;
    }
    
    /**
     * @return the email address of the logged in user,
     *         not necessarily the same as the owner of the job.
     */
    public String getCurrentUserEmail() {
        return currentUserEmail;        
    }

    /**
     * Support for variable a4j polling based on how long the job has been running.
     * @return an interval, in milliseconds, between the previous response and the next request.
     */
    public int getInterval() {
        if (jobInfoWrapper == null) {
            return 2500;
        }
        long elapsedTime = jobInfoWrapper.getElapsedTimeMillis();
        
        if (elapsedTime <   60000) { //(1 min)
            return 2500; //(2.5 sec)
        }
        if (elapsedTime <  120000) { //(2 min)
            return 10000; //(10 sec)
        }
        if (elapsedTime <  300000) { //(5 min)
            return 20000; //(20 sec)
        }
        if (elapsedTime < 3600000) { //(1 hr)
            return 60000; //(1 min)
        } 
        return 300000; //(5 min)
    }

    //migrate actions from JobBean
    public void downloadZip(ActionEvent event) {
        if (jobInfoWrapper == null) {
            init();
        }
        if (jobInfoWrapper == null) {
            UIBeanHelper.setErrorMessage("Invalid job, can't download zip files.");
            return;
        }

        HttpServletResponse response = UIBeanHelper.getResponse();
        response.setHeader("Content-Disposition", "attachment; filename=" + jobInfoWrapper.getJobNumber() + ".zip" + ";");
        response.setHeader("Content-Type", "application/octet-stream");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        
        try {
            OutputStream os = response.getOutputStream();
            JobInfoManager.writeOutputFilesToZipStream(os, jobInfoWrapper);
            os.close();
        }
        catch (IOException e) {
            UIBeanHelper.setErrorMessage("Error downloading output files for job "+jobInfoWrapper.getJobNumber()+": "+e.getLocalizedMessage());
        }
        UIBeanHelper.getFacesContext().responseComplete();
    }

}
