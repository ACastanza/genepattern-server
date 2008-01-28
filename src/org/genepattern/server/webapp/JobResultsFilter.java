/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2008) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 *
 *******************************************************************************/

package org.genepattern.server.webapp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;

/**
 *
 * Security filter on job results pages which are now accessible via normal http
 * without using the retrieveResults.jsp page
 *
 * In 3.0, you are allowed access to the directory of your own jobs only but not
 * the parent dir of your jobs or anyone elses jobs
 *
 * This is determined by looking at the URL, taking the job # as whatever
 * follows the "jobResults" part in the url
 *
 *
 * reqURL: http://gp21e-789.broad.mit.edu:8080/gp/jobResults/92/foo.out
 * contextPath: /gp reqURI: /gp/jobResults/92/foo.out ServletPath:
 * /jobResults/92/foo.out
 *
 * and in genepattern.properties we typically have jobs=./webapps/gp/jobResults
 * which defines the directory the files are in
 *
 * In this first implementation, we'll assume the dir name 'jobResults' cannot
 * be changed in any GP install
 *
 * @author Ted Liefeld
 * @author Joshua Gould
 *
 */
public class JobResultsFilter implements Filter {

    private String jobsDirectory;

    private static Logger log = Logger.getLogger(JobResultsFilter.class);

    private void loadProperties(Properties props, File propFile) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propFile);
            props.load(fis);

        } catch (IOException e) {
            log.error(e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {

                }
            }
        }

    }

    public void init(FilterConfig filterconfig) throws ServletException {
        String dir = filterconfig.getInitParameter("genepattern.properties");
        File propFile = new File(dir, "genepattern.properties");
        File customPropFile = new File(dir, "custom.properties");
        Properties props = new Properties();

        if (propFile.exists()) {
            loadProperties(props, propFile);
        }

        if (customPropFile.exists()) {
            loadProperties(props, customPropFile);
        }
        jobsDirectory = props.getProperty("jobs");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        boolean allowed = false;

        String userid = (String) request.getAttribute(GPConstants.USERID);

        // since this is a dir name with a path, we want to get the path in the
        // application context
        // ie "gp" so...
        // given http://aserver:aport/gp/jobResults/92/foo.txt
        // we want to get the "92" as the job #

        String servletPath = ((HttpServletRequest) request).getServletPath();
        int idx = servletPath.indexOf("jobResults");
        if (idx == -1) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
        String resultsPath = servletPath.substring(idx + 1 + ("jobResults".length()));
        try {
            resultsPath = URLDecoder.decode(resultsPath, "UTF-8");
        } catch (UnsupportedEncodingException x) {
        }

        StringTokenizer strtok = new StringTokenizer(resultsPath, "/");
        String job = null;
        String file = null;
        if (strtok.hasMoreTokens()) {
            job = strtok.nextToken();
        }
        if (strtok.hasMoreTokens()) {
            file = strtok.nextToken();
        }

        if (job == null || file == null) {
            // if file is null, request is for a directory-prohibit directory
            // listings
            // should admin be allowed here?
            allowed = false;
        } else if (isJobOwner(userid, job)
                || AuthorizationHelper.adminJobs(userid)) {
            allowed = true;
        }

        if (allowed) {
            File fileObj = new File(jobsDirectory + File.separator + job + File.separator + file);
            String lcFileName = fileObj.getName().toLowerCase();
            
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            httpServletResponse.setHeader("Content-disposition", "inline; filename=\"" + fileObj.getName() + "\"");
            httpServletResponse.setHeader("Cache-Control", "no-store");
            httpServletResponse.setHeader("Pragma", "no-cache");
            httpServletResponse.setDateHeader("Expires", 0);
            httpServletResponse.setDateHeader("Last-Modified", fileObj.lastModified());
            httpServletResponse.setHeader("Content-Length", "" + fileObj.length());

            if (lcFileName.endsWith(".html") || lcFileName.endsWith(".htm")){
            	httpServletResponse.setHeader("Content-Type", "text/html"); 
            }
            
            BufferedInputStream is = null;
            try {
                OutputStream os = response.getOutputStream();
                is = new BufferedInputStream(new FileInputStream(fileObj));
                byte[] b = new byte[10000];
                int bytesRead;
                while ((bytesRead = is.read(b)) != -1) {
                    os.write(b, 0, bytesRead);
                }
            } catch (FileNotFoundException e) {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException x) {
                    }
                }
            }
        } else {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    private boolean isJobOwner(String user, String jobId) {
        try {
            if (user == null) {
                return false;
            }
            int jobID = Integer.parseInt(jobId);
            AnalysisDAO ds = new AnalysisDAO();
            JobInfo jobInfo = ds.getJobInfo(jobID);
            return user.equals(jobInfo.getUserId());
        } catch (NumberFormatException nfe) {
            return false;
        }

    }

    public void destroy() {

    }

}
