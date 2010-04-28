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

package org.genepattern.server.webapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HsqlDbUtil;
import org.genepattern.server.executor.CommandManager;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.message.SystemAlertFactory;
import org.genepattern.server.process.JobPurger;
import org.genepattern.server.util.JobResultsFilenameFilter;
import org.genepattern.server.webapp.jsf.AboutBean;

/*
 * GenePattern startup servlet
 * 
 * This servlet performs periodic maintenance, based on definitions in the <omnigene.conf>/genepattern.properties file
 * @author Jim Lerner
 */
public class StartupServlet extends HttpServlet {
    private static Logger log = Logger.getLogger(StartupServlet.class);

    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static Vector<Thread> vThreads = new Vector<Thread>();

    public StartupServlet() {
        log.info("Creating StartupServlet");
    }
    
    public String getServletInfo() {
        return "GenePatternStartupServlet";
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        log.info("StartupServlet: user.dir=" + System.getProperty("user.dir"));

        ServletContext application = config.getServletContext();
        String genepatternProperties = config.getInitParameter("genepattern.properties");
        application.setAttribute("genepattern.properties", genepatternProperties);
        String customProperties = config.getInitParameter("custom.properties");
        if (customProperties == null) {
            customProperties = genepatternProperties;
        }
        application.setAttribute("custom.properties", customProperties);
        loadProperties(config);
        setServerURLs(config);

        String dbVendor = System.getProperty("database.vendor", "HSQL");
        if (dbVendor.equals("HSQL")) {
            try {
                HsqlDbUtil.startDatabase();
            }
            catch (Throwable t) {
                log.error("Unable to start HSQL Database!", t);
                return;
            }
        }

        //start the command executors before starting the internal job queue (AnalysisTask.startQueue) ...
        CommandManagerFactory.initializeCommandManager(System.getProperties());
        CommandManager cmdManager = CommandManagerFactory.getCommandManager();
        cmdManager.startAnalysisService();
        cmdManager.startCommandExecutors();

        startDaemons(System.getProperties(), application);
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        HttpsURLConnection.setDefaultHostnameVerifier(new SessionHostnameVerifier());

        //clear system alert messages
        try {
            SystemAlertFactory.getSystemAlert().deleteOnRestart();
        }
        catch (Exception e) {
            log.error("Error clearing system messages on restart: "+e.getLocalizedMessage(), e);
        }
        announceReady(System.getProperties());
    }

    /**
     * Set the GenePatternURL property dynamically using
     * the current canonical host name and servlet context path.
     * Dynamic lookup works for Tomcat but may not work on other containers... 
     * Define GP_Path (to be used as the 'servletContextPath') in the genepattern.properties file to avoid dynamic lookup.
     * 
     * @param config
     */
    private void setServerURLs(ServletConfig config) {
        //set the GP_Path property if it has not already been set
        String contextPath = System.getProperty("GP_Path");
        if (contextPath == null) {
            contextPath = "/gp";
        }
        else {
            if (!contextPath.startsWith("/")) {
                contextPath = "/" + contextPath;
            }
            if (contextPath.endsWith("/")) {
                contextPath = contextPath.substring(0, contextPath.length()-1);
            }
        }
        System.setProperty("GP_Path", "/gp");
        String genePatternURL = System.getProperty("GenePatternURL", "");
        if (genePatternURL == null || genePatternURL.trim().length() == 0) {
            try {
                InetAddress addr = InetAddress.getLocalHost();
                String host_address = addr.getCanonicalHostName();
                String portStr = System.getProperty("GENEPATTERN_PORT", "");
                portStr = portStr.trim();
                if (portStr.length()>0) {
                    portStr = ":"+portStr;
                }
                contextPath = System.getProperty("GP_Path", "/gp");
                String genePatternServerURL = "http://" + host_address + portStr + contextPath + "/";
                System.setProperty("GenePatternURL", genePatternServerURL);
            } 
            catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }

    protected void startDaemons(Properties props, ServletContext application) {
        startJobPurger(props);
        // allow a bit of runtime to the independent threads
        Thread.yield();
    }

    protected void startJobPurger(Properties props) {
        Thread tJobPurger = JobPurger.startJobPurger(props);
        addDaemonThread(tJobPurger);
    }

    public static void startJobPurger() {
        Thread tJobPurger = JobPurger.startJobPurger(System.getProperties());
        addDaemonThread(tJobPurger);
    }

    protected static void addDaemonThread(Thread t) {
        vThreads.add(t);
    }

    protected void announceReady(Properties props) {
        AboutBean about = new AboutBean();
        about.getFull();
        about.getBuildTag();
        about.getDate();
        String message = "GenePattern server version " + about.getFull() + 
            " build " + about.getBuildTag() + 
            " built " + about.getDate() + " is ready.";

        String stars = "******************************************************************************************************************************************"
            .substring(0, message.length());
        StringBuffer startupMessage = new StringBuffer();
        final String NL = System.getProperty("line.separator");
        startupMessage.append(NL + "Listening on " + System.getProperty("GenePatternURL") + NL);
        startupMessage.append(""+NL);
        startupMessage.append(stars + NL);
        startupMessage.append(message + NL);
        startupMessage.append("\tJava Version: "+System.getProperty("java.version") + NL);
        startupMessage.append(stars);
        
        log.info(startupMessage);
    }

    public void destroy() {
        log.info("StartupServlet: destroy called");
        
        //TODO: terminate the job queue properly
        //this method, as presently implemented, can cause deadlock
        //AnalysisTask.stopQueue();

        //stop the command executors ...
        CommandManagerFactory.getCommandManager().stopCommandExecutors();
        CommandManagerFactory.getCommandManager().shutdownAnalysisService();

        String dbVendor = System.getProperty("database.vendor", "HSQL");
        if (dbVendor.equals("HSQL")) {
            HsqlDbUtil.shutdownDatabase();
        }

        for (Enumeration<Thread> eThreads = vThreads.elements(); eThreads.hasMoreElements();) {
            Thread t = eThreads.nextElement();
            try {
                if (t.isAlive()) {
                    log.info("Interrupting " + t.getName());
                    t.interrupt();
                    t.setPriority(Thread.NORM_PRIORITY);
                    t.join();
                    log.info(t.getName() + " exited");
                }
            } 
            catch (Throwable e) {
                log.error(e);
            }
        }
        vThreads.removeAllElements();

        log.info("StartupServlet: destroy, calling dumpThreads...");
        dumpThreads();
        log.info("StartupServlet: destroy done");
    }

    /**
     * Set System properties to the union of all settings in:
     * servlet init parameters
     * resources/genepattern.properties
     * resources/build.properties
     * 
     * @param config
     * @throws ServletException
     */
    protected void loadProperties(ServletConfig config) throws ServletException {
        File propFile = null;
        File customPropFile = null;
        FileInputStream fis = null;
        FileInputStream customFis = null;
        try {
            for (Enumeration<String> eConfigProps = config.getInitParameterNames(); eConfigProps.hasMoreElements();) {
                String propName = eConfigProps.nextElement();
                String propValue = config.getInitParameter(propName);
                if (propValue.startsWith(".")) {
                    propValue = new File(propValue).getCanonicalPath();
                }
                System.setProperty(propName, propValue);
            }
            Properties sysProps = System.getProperties();
            String dir = sysProps.getProperty("genepattern.properties");
            propFile = new File(dir, "genepattern.properties");
            customPropFile = new File(dir, "custom.properties");
            Properties props = new Properties();
            fis = new FileInputStream(propFile);
            props.load(fis);
            log.info("loaded GP properties from " + propFile.getCanonicalPath());

            if (customPropFile.exists()) {
                customFis = new FileInputStream(customPropFile);
                props.load(customFis);
                log.info("loaded Custom GP properties from " + customPropFile.getCanonicalPath());
            }

            // copy all of the new properties to System properties
            for (Iterator<?> iter = props.keySet().iterator(); iter.hasNext();) {
                String key = (String) iter.next();
                String val = props.getProperty(key);
                if (val.startsWith(".")) {
                    //HACK: don't rewrite my value
                    if (! key.equals(JobResultsFilenameFilter.KEY)) {
                        // why? why  is this here? -- PJC
                        val = new File(val).getAbsolutePath();
                    }
                }
                sysProps.setProperty(key, val);
            }

            propFile = new File(dir, "build.properties");
            fis = new FileInputStream(propFile);
            props.load(fis);
            fis.close();
            fis = null;
            // copy all of the new properties to System properties
            for (Iterator<?> iter = props.keySet().iterator(); iter.hasNext();) {
                String key = (String) iter.next();
                String val = props.getProperty(key);
                if (val.startsWith(".")) {
                    //HACK: don't rewrite my value
                    if (! key.equals(JobResultsFilenameFilter.KEY)) {
                        // why? why  is this here? -- PJC
                        val = new File(val).getAbsolutePath();
                    }
                }
                sysProps.setProperty(key, val);
            }

            System.setProperty("serverInfo", config.getServletContext().getServerInfo());
	    
            TreeMap tmProps = new TreeMap(sysProps);
            for (Iterator<?> iProps = tmProps.keySet().iterator(); iProps.hasNext();) {
                String propName = (String) iProps.next();
                String propValue = (String) tmProps.get(propName);
                log.debug(propName + "=" + propValue);
            }
        } 
        catch (IOException ioe) {
            ioe.printStackTrace();
            String path = null;
            try {
                path = propFile.getCanonicalPath();
            } 
            catch (IOException ioe2) {
            }
            throw new ServletException(path + " cannot be loaded.  " + ioe.getMessage());
        } 
        finally {
            try {
                if (fis != null)
                    fis.close();
            } 
            catch (IOException ioe) {
            }
        }
    }

    protected void dumpThreads() {
        log.info("StartupServlet.dumpThreads: what's still running...");
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        while (tg.getParent() != null) {
            tg = tg.getParent();
        }
        int MAX_THREADS = 100;
        Thread threads[] = new Thread[MAX_THREADS];
        int numThreads = tg.enumerate(threads, true);
        Thread t = null;
        for (int i = 0; i < numThreads; i++) {
            t = threads[i];
            if (t == null) {
                continue;
            } 
            if (!t.isAlive()) {
                continue;
            }
            log.info(t.getName() + " is running at " + t.getPriority() + " priority.  " + (t.isDaemon() ? "Is" : "Is not")
                    + " daemon.  " + (t.isInterrupted() ? "Is" : "Is not") + " interrupted.  ");

            //for debugging            
            //if (t.getName().startsWith("Thread-")) {
            //    log.info("what is this thread?");
            //    t.dumpStack();
            //    
            //    for(StackTraceElement e : t.getStackTrace()) {
            //        String m = ""+e.getClassName()+"."+e.getMethodName();
            //        String f = ""+e.getFileName()+":"+ e.getLineNumber();
            //        log.info(""+m+", "+f);
            //    }
            //
            //    log.info("calling Thread.stop()...");
            //    t.stop();
            //}
            
        }
        if (numThreads == MAX_THREADS) {
            log.info("Possibly more than " + MAX_THREADS + " are running.");
        }
    }
}

class LaunchThread extends Thread {
    private static Logger log = Logger.getLogger(LaunchThread.class);

    Method mainMethod;
    String[] args;
    StartupServlet parent;

    public LaunchThread(String taskName, Method mainMethod, String[] args, StartupServlet parent) {
        super(taskName);
        this.mainMethod = mainMethod;
        this.args = args;
        this.parent = parent;
        this.setDaemon(true);
    }

    public void run() {
        try {
            log.debug("invoking " + mainMethod.getDeclaringClass().getName() + "." + mainMethod.getName());
            mainMethod.invoke(null, new Object[] { args });
            parent.log(getName() + " " + mainMethod.getDeclaringClass().getName() + "." + mainMethod.getName()
                    + " returned from execution");
        } 
        catch (IllegalAccessException iae) {
            log.error("Can't invoke main in " + getName(), iae);
        } 
        catch (IllegalArgumentException iae2) {
            log.error("Bad args for " + getName(), iae2);
        } 
        catch (InvocationTargetException ite) {
            log.error("InvocationTargetException for " + getName(), ite.getTargetException());
        } 
        catch (Exception e) {
            log.error("Exception for " + getName(), e);
        }
    }
}
