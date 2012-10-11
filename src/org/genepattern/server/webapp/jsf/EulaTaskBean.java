package org.genepattern.server.webapp.jsf;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.EulaInfo.EulaInitException;
import org.genepattern.server.eula.EulaManager;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;

/**
 * Request scope JSF bean for handling End-user license agreement(s) before running a particular module. 
 * 
 * The GUI must prompt a GP User before going to the job submit form.
 * 
 * @author pcarr
 */
public class EulaTaskBean {
    private static Logger log = Logger.getLogger(EulaTaskBean.class);
    
    /**
     * Use this class as an intermediary between the eula manager and the JSF gui.
     * 
     * @author pcarr
     */
    static public class EulaInfoBean {
        static EulaInfoBean from(EulaInfo eulaInfoObj) throws EulaInitException {
            EulaInfoBean eulaInfo = new EulaInfoBean();
            eulaInfo.setLsid(eulaInfoObj.getModuleLsid());
            eulaInfo.setLsidVersion(eulaInfoObj.getModuleLsidVersion());
            eulaInfo.setTaskName(eulaInfoObj.getModuleName());
            eulaInfo.setContent(eulaInfoObj.getContent());
            eulaInfo.setLink(eulaInfoObj.getLink());
            return eulaInfo;
        }

        private String lsid;
        private String lsidVersion;
        private String taskName;
        private String content;
        private String link;

        public String getLsid() {
            return lsid;
        }
        public void setLsid(final String lsid) {
            this.lsid = lsid;
        }
        
        public String getLsidVersion() {
            return lsidVersion;            
        }
        public void setLsidVersion(final String lsidVersion) {
            this.lsidVersion = lsidVersion;
        }

        public String getTaskName() {
            return taskName;
        }

        public void setTaskName(final String taskName) {
            this.taskName = taskName;
        }

        public String getContent() {
            return content;
        }

        public void setContent(final String content) {
            this.content = content;
        }

        public String getLink() {
            return link;
        }

        public void setLink(final String link) {
            this.link = link;
        }
    }
    
    //the current GP user
    private String currentUser=null;
    //the lsid of the job that the current user wants to run (can be a module or a pipeline)
    private String currentLsid=null;
    //
    private String currentLsidVersion=null;
     //the name of the job that the current user wants to run (can be a module or a pipeline)
    private String currentTaskName=null;
    //instantiated, when necessary, from the currentLsid
    private TaskInfo currentTaskInfo=null;
    //if true, it means we need to display the EULA agreement form, otherwise skip ahead to the job submit form
    private boolean prompt=false;
    //the list of pending EULA, those which the current user must agree to before running the module or pipeline 
    private List<EulaInfoBean> eulas=null;
    //internal helper variable (to deal with JSF lifecycle), sometimes prompt is true AND accepted is true
    //    which means, don't prompt again
    private boolean accepted=false;
    //helper method, needed to restore the 'reloadJob=<jobId>' request parameter,
    //    when we have to prompt for EULA before reloading a job
    private String reloadJobParam="";

    public EulaTaskBean() {
        this.currentUser = UIBeanHelper.getUserId();
    }
    
    //callback from ModuleChooserBean#setSelectedModule, JobBean#reload
    //  #{eulaTaskBean.initialQueryString}
    public void setCurrentLsid(final String currentLsid) {
        log.debug("currentLsid="+currentLsid);
        this.currentLsid=currentLsid;
        this.currentLsidVersion="";
        if (currentLsid != null && currentLsid.length() != 0) {
            //if necessary, init currentLsidVersion and currentTaskInfo
            if (currentTaskInfo == null || !currentLsid.equals( currentTaskInfo.getLsid() )) {
                log.debug("initializing currentLsidVersion, currentLsid="+currentLsid);
                try {
                    LSID tmpLsid = new LSID(currentLsid);
                    this.currentLsidVersion=tmpLsid.getVersion();
                }
                catch (MalformedURLException e) {
                    log.error("Unexpected error getting version from lsid string, currentLsid="+currentLsid,e);
                } 
                log.debug("initializing currentTaskInfo, currentLsid="+currentLsid);
                currentTaskInfo = initTaskInfo(currentUser, currentLsid);
            }
        }
        if (this.currentTaskInfo!=null) {
            this.prompt=initEulaInfo(currentTaskInfo);
            this.currentTaskName=currentTaskInfo.getName();
        }
        else {
            this.prompt=false;
        }
    }
    
    /**
     * Get the lsid for the module (or pipeline) that the current user wants to run.
     * This may or may not be the same lsid as that which requires EULA.
     * 
     * We use this to display the header information.
     * 
     * @return
     */
    public String getCurrentLsid() {
        return currentLsid;
    }
    
    /**
     * Get the version (from the lsid) from the module (or pipeline) that the current user wants to run.
     * @return
     */
    public String getCurrentLsidVersion() {
        return currentLsidVersion;
    }

    /**
     * Get the name of the module (or pipeline) that the current user wants to run.
     * This may or may not be the same lsid as that which requires EULA.
     *
     * We use this to display the header information.
     *
     * @return
     */
    public String getCurrentTaskName() {
        return currentTaskName;
    }

    /**
     * Check to see if we need to prompt the currentUser for an EULA for the current module.
     * Note: this covers the following cases:
     *     1) module has no EULA (return false)
     *     2) module has one or more EULA, but current user has already agreed to all of them (return false)
     *     3) module has one or more EULA which the current user has not yet agreed to (return true)
     * @return true, if the GUI should prompt the current user to accept one or more EULA.
     */
    public boolean isPrompt() {
        Object obj = UIBeanHelper.getRequest().getSession().getAttribute(GPConstants.LSID);
        if (obj instanceof String) {
            String taskNameOrLsid = (String) obj;
            if (taskNameOrLsid != currentLsid) {
                setCurrentLsid(taskNameOrLsid);
            }
        }

        if (prompt) {
            return !accepted;
        }
        return prompt;
    }

    /**
     * Get the list of pending End-user license agreements that the current user must agree to 
     * before they can run the current module.
     * 
     * @return
     */
    public List<EulaInfoBean> getEulas() {
        if (eulas==null) {
            return Collections.emptyList();
        }
        return eulas;
    }

    //helper method, when reloading a job from a URL, e.g.
    //    /gp/pages/index.jsf?lsid=urn:lsid:8080.gp-trunk-dev.120.0.0.1:genepatternmodules:303:2&reloadJob=7767
    public String getInitialQueryString() {
        HttpServletRequest request = UIBeanHelper.getRequest();
        String qs = request.getQueryString();
        if (qs == null) {
            qs="";
        }
        log.debug("queryString="+qs);
        return qs;
    }
    
    //helper method, when reloading a job from the job menu
    public void setReloadJobParam(final String str) {
        log.debug("reloadJobParam="+str);
        this.reloadJobParam=str;
    }
    
    public String getReloadJobParam() {
        return reloadJobParam;
    }

    private static TaskInfo initTaskInfo(final String currentUser, final String lsid) {
        //TODO: this code is duplicated in RunTaskBean, 
        //      should find a way to share the same instance of the TaskInfo per page request
        TaskInfo taskInfo = null;
        if (lsid != null && lsid.length()>0) {
            try {
                final LocalAdminClient lac = new LocalAdminClient(currentUser);
                taskInfo = lac.getTask(lsid);
            }
            catch (Throwable t) {
                log.error("Error initializing taskInfo for lsid=" + lsid, t);
            }
        } 
        return taskInfo;
    }

    /**
     * @see #prompt for documentation.
     */
    private boolean initEulaInfo(final TaskInfo taskInfo) {
        log.debug("initializing EULA info for userId="+currentUser+", lsid="+currentLsid);
        boolean requiresEULA = false; 
        if (taskInfo != null) {
            Context taskContext = Context.getContextForUser(currentUser);
            taskContext.setTaskInfo(taskInfo);
            List<EulaInfo> promptForEulas = EulaManager.instance().getPendingEulaForModule(taskContext);
            if (promptForEulas == null || promptForEulas.size()==0) {
                requiresEULA=false;
                this.eulas=Collections.emptyList();
            }
            else {
                requiresEULA=true;
                if (eulas==null) {
                    eulas=new ArrayList<EulaInfoBean>();
                }
                else {
                    eulas.clear();
                }
                for(EulaInfo eulaInfoObj : promptForEulas) {
                    try {
                        EulaInfoBean eulaInfoBean = EulaInfoBean.from(eulaInfoObj);
                        eulas.add(eulaInfoBean);
                    }
                    catch (EulaInitException e) {
                        String message="Error initializing EULA info";
                        if (eulaInfoObj != null) {
                            message += ", moduleName="+eulaInfoObj.getModuleName();
                            message += ", lsid="+eulaInfoObj.getModuleLsid();
                        }
                        //TODO: should propagate a message back to the end user
                        //   The message is: 'An error occurred getting the End-user license agreement for this module,
                        //                    You will not be able to run it until it has been resolved.
                        //                    <moduleName> (lsid)'
                        //Note: by swallowing the exception, the EULA is not displayed
                        //    this will cause a runtime error, because the user has not had a chance to accept the agreement
                        log.error(message, e);
                    }
                }
            }
        }
        
        log.debug("requiresEULA="+requiresEULA);
        return requiresEULA;
    }

}
