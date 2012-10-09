package org.genepattern.server.eula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.eula.dao.RecordEulaToDb;
import org.genepattern.webservice.TaskInfo;

/**
 * Methods for managing End-user license agreements (EULA) for GenePattern modules.
 * 
 * @author pcarr
 */
public class EulaManager {
    public static Logger log = Logger.getLogger(EulaManager.class);

    static public class Singleton {
        private static final EulaManager INSTANCE = new EulaManager();
        public static EulaManager instance() {
            return INSTANCE;
        }
    }
    static public EulaManager instance() {
        return Singleton.INSTANCE;
    }

    //factory method for getting the method for recording the EULA
    private static RecordEula getRecordEula(EulaInfo eulaInfo) {
        //for debugging, the RecordEulaStub can be used
        //return RecordEulaStub.instance(); 
        //TODO: if necessary, remote record to external web service
        return new RecordEulaToDb();
    }
    
    /**
     * Implement a run-time check, before starting a job, verify that there are
     * no EULA which the current user has not yet agreed to.
     * 
     * Returns true, if,
     *     1) the module requires no EULAs, or
     *     2) the current user has agreed to all EULAs for the module.
     * 
     * @param jobContext
     * @return true if there is no record of EULA for the current user.
     */
    public boolean requiresEULA(Context jobContext) {
        final List<EulaInfo> notYetAgreed = getEulaInfos(jobContext,false);
        if (notYetAgreed.size()>0) {
            return true;
        }
        return false;
    }
    
    /**
     * Get the list of all End-user license agreements for the given module or pipeline.
     * This list includes all EULA, even if the current user has already agreed to them.
     * 
     * @param taskContext, must have a valid taskInfo object
     * @return
     */
    public List<EulaInfo> getAllEULAForModule(final Context taskContext) {
        List<EulaInfo> eulaInfos = getEulaInfos(taskContext, true);
        return eulaInfos;
    }

    /**
     * Get the list of End-user license agreements for the given module or pipeline, for
     * which to prompt for agreement from the current user.
     * 
     * Use this list as the basis for prompting the current user for agreement before
     * going to the job submit form for the task.
     * 
     * @param taskContext
     * @return
     */
    public List<EulaInfo> getPendingEULAForModule(final Context taskContext) {
        List<EulaInfo> eulaInfos = getEulaInfos(taskContext, false);
        return eulaInfos;
    }
    
    /**
     * Record current user agreement to the End-user license agreement.
     * When the taskInfo is a pipeline, accept all agreements.
     * 
     * @param taskContext, must have a valid taskInfo and userId
     */
    public void recordLicenseAgreement(final Context taskContext) {
        if (taskContext == null) {
            throw new IllegalArgumentException("taskContext==null");
        }
        TaskInfo taskInfo = taskContext.getTaskInfo();
        if (taskInfo==null) {
            throw new IllegalArgumentException("taskInfo==null");
        }
        if (taskInfo.getLsid()==null || taskInfo.getLsid().length()==0) {
            throw new IllegalArgumentException("taskInfo not set");
        }
        if (taskContext.getUserId()==null) {
            throw new IllegalArgumentException("userId==null");
        }
        if (taskContext.getUserId().length()==0) {
            throw new IllegalArgumentException("userId not set");
        }

        log.debug("recording EULA for user="+taskContext.getUserId()+", lsid="+taskContext.getTaskInfo().getLsid());
        
        List<EulaInfo> eulaInfos = getEulaInfosFromTaskInfo(taskInfo);
        for(EulaInfo eulaInfo : eulaInfos) {
            try {
                RecordEula recordEula = getRecordEula(eulaInfo);
                recordEula.recordLicenseAgreement(taskContext.getUserId(), eulaInfo.getModuleLsid());
            }
            catch (Exception e) {
                //TODO: report back to end user
                log.error("Error recording EULA for userId="+taskContext.getUserId()+", module="+eulaInfo.getModuleName()+", lsid="+eulaInfo.getModuleLsid());
            }
        }
    }

    /**
     * Get the list of required End-user license agreements for the given module or pipeline.
     * 
     * @param taskContext
     * @param includeAll
     * @return
     */
    private List<EulaInfo> getEulaInfos(final Context taskContext, final boolean includeAll) {
        if (taskContext == null) {
            throw new IllegalArgumentException("taskContext==null");
        }
        TaskInfo taskInfo = taskContext.getTaskInfo();
        if (taskInfo==null) {
            throw new IllegalArgumentException("taskInfo==null");
        }
        if (taskInfo.getLsid()==null || taskInfo.getLsid().length()==0) {
            throw new IllegalArgumentException("taskInfo not set");
        }
        List<EulaInfo> eulaObjs = getEulaInfosFromTaskInfo(taskInfo);
        if (includeAll) {
            return eulaObjs;
        }
        
        //only return ones which require user agreement
        if (taskContext.getUserId()==null) {
            throw new IllegalArgumentException("userId==null");
        }
        if (taskContext.getUserId().length()==0) {
            throw new IllegalArgumentException("userId not set");
        }
        
        List<EulaInfo> notYetAgreed = new ArrayList<EulaInfo>();
        for(EulaInfo eulaObj : eulaObjs) {
            boolean hasAgreed = false;
            final String userId=taskContext.getUserId();
            final String lsid=eulaObj.getModuleLsid();
            try {
                RecordEula recordEula = getRecordEula(eulaObj);
                hasAgreed = recordEula.hasUserAgreed(taskContext.getUserId(), eulaObj);
            }
            catch (Throwable t) {
                //TODO: report error back to end-user
                log.error("Error recording eula, userId="+userId+", lsid="+lsid, t);
            }
            if (!hasAgreed) {
                notYetAgreed.add( eulaObj );
            }
        }
        return notYetAgreed;
    }
    
    /**
     * Get the list of EulaInfo for the given task, can be a module or pipeline.
     * When it is a pipeline, recursively get all required licenses.
     * 
     * @param taskInfo
     * @return
     */
    private List<EulaInfo> getEulaInfosFromTaskInfo(TaskInfo taskInfo) {
        if (taskInfo==null) {
            log.error("taskInfo==null");
            return Collections.emptyList();
        }
        
        GetEulaFromTaskRecursive getEulaFromTask = new GetEulaFromTaskRecursive();
        SortedSet<EulaInfo> eulaObjs = getEulaFromTask.getEulasFromTask(taskInfo);
        //TODO: is this the best way to return the sortedset as a list?
        List<EulaInfo> list = new ArrayList<EulaInfo>(eulaObjs);
        return list;
    }
    
}