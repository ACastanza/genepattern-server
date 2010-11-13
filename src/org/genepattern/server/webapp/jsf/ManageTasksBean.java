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

/**
 *
 */
package org.genepattern.server.webapp.jsf;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.faces.event.ActionEvent;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.util.LSIDVersionComparator;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;

public class ManageTasksBean {
    private static Logger log = Logger.getLogger(ManageTasksBean.class);

    private boolean showEveryonesModules = true;
    private boolean adminModules;
    private List<TaskGroup> sortedTasks;

    public ManageTasksBean() {
        adminModules = AuthorizationHelper.adminModules();
        this.showEveryonesModules = Boolean.valueOf(
                new UserDAO().getPropertyValue(UIBeanHelper.getUserId(), "showEveryonesModules", String.valueOf(this.showEveryonesModules)));
    }

    public void delete(ActionEvent event) {
        String[] taskLsids = UIBeanHelper.getRequest().getParameterValues("selectedVersions");
        deleteTasks(taskLsids);
    }

    public Collection<TaskGroup> getTasks() {
        if (sortedTasks == null) {
            updateModules();
        }
        return sortedTasks;
    }

    public boolean isAdminModules() {
        return adminModules;
    }

    public boolean isShowEveryonesModules() {
        return showEveryonesModules;
    }

    public void setShowEveryonesModules(boolean b) {
        showEveryonesModules = b;
        new UserDAO().setProperty(UIBeanHelper.getUserId(), "showEveryonesModules", String.valueOf(showEveryonesModules));
        updateModules();
    }

    private void deleteTasks(String[] taskLsids) {
        if (taskLsids != null) {
            LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(UIBeanHelper.getUserId());
            for (String lsid : taskLsids) {
                try {
                    taskIntegratorClient.deleteTask(lsid);
                } 
                catch (Exception e) {
                    e.printStackTrace();
                    log.error(e);
                    throw new RuntimeException(e);
                }
            }
            updateModules();
        }
    }

    private Collection<TaskInfo> getModulesFromDatabase() {
        try {
            IAdminClient adminClient = new LocalAdminClient(UIBeanHelper.getUserId());
            if (!showEveryonesModules) {
                return Arrays.asList(adminClient.getTasksOwnedBy());
            }
            return adminModules ? Arrays.asList(adminClient.getAllTasksForModuleAdmin()) : adminClient.getTaskCatalog();
        } 
        catch (WebServiceException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    private void groupAndSortTasks(Collection<TaskInfo> tasks) {
        HashMap<String, TaskGroup> indexedTasks = new HashMap<String, TaskGroup>();
        for (Iterator<TaskInfo> itTasks = tasks.iterator(); itTasks.hasNext();) {
            TaskInfo ti = itTasks.next();
            String lsid = ti.getLsid();
            LSID lSID = null;
            try {
                lSID = new LSID(lsid);
            } catch (MalformedURLException mue) {
                log.error("Error creating LSID (Malformed URL): " + lsid, mue);
                throw new RuntimeException("Error creating LSID (Malformed URL): " + lsid);
            }

            String lsidNoVersion = lSID.toStringNoVersion();
            TaskGroup taskGroup = indexedTasks.get(lsidNoVersion);
            if (taskGroup == null) {
                taskGroup = new TaskGroup(ti, showEveryonesModules);
                indexedTasks.put(lsidNoVersion, taskGroup);
            }
            taskGroup.addVersionInfo(indexedTasks, ti);
        }

        sortedTasks = new ArrayList<TaskGroup>(indexedTasks.values());
        Collections.sort(sortedTasks, new Comparator<TaskGroup>() {
            public int compare(TaskGroup o1, TaskGroup o2) {
                String n1 = o1.getName();
                String n2 = o2.getName();
                return n1.compareToIgnoreCase(n2);
            }
        });
    }

    private void updateModules() {
        Collection<TaskInfo> tasks = getModulesFromDatabase();
        groupAndSortTasks(tasks);
    }

    public static class TaskGroup implements Serializable {
        private String lsidNoVersion = null;
        private String description = null;
        private TreeMap<String, VersionInfo> indexedVersions;
        private IAdminClient adminClient = null;
        private boolean pipeline;
        private boolean showEveryonesModules = false;

        public TaskGroup(TaskInfo ti, boolean showEveryonesModules) {
            pipeline = ti.isPipeline();
            adminClient = new LocalAdminClient(UIBeanHelper.getUserId());
            indexedVersions = new TreeMap<String, VersionInfo>(new Comparator<String>() {
                public int compare(String o1, String o2) {
                    try {
                        LSID lsid1 = new LSID(o1);
                        LSID lsid2 = new LSID(o2);
                        return LSIDVersionComparator.INSTANCE.compare(lsid2.getVersion(), lsid1.getVersion());
                    } 
                    catch (MalformedURLException e) {
                        log.error(e);
                        return 0;
                    }
                }
            });

            lsidNoVersion = getLSID(ti.getLsid()).toStringNoVersion();
            description = ti.getDescription();
        }

        /**
         * Add a specific specific versioned task.
         * 
         * @param indexedTasks
         * 
         * @param taskInfo
         */
        public void addVersionInfo(HashMap<String, TaskGroup> indexedTasks, TaskInfo taskInfo) {
            String lsid = taskInfo.getLsid();//+ "." + taskInfo.getID();
            String key = taskInfo.getLsid() + "." + taskInfo.getID();
            VersionInfo versionInfo = new VersionInfo(taskInfo);
            indexedVersions.put(key, versionInfo);// changed to include id jtl 12/11/07

            if (taskInfo.isPipeline() && showEveryonesModules) {
                TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
                String xml = (String) tia.get(GPConstants.SERIALIZED_MODEL);
                PipelineModel model = null;
                try {
                    model = PipelineModel.toPipelineModel(xml);
                } catch (Throwable t) {
                    log.error(" loading pipeline model " + taskInfo.getName() + " - " + lsid, t);
                    log.error(xml);
                    return;
                }
                Map<String, String> mDependencies = model.getLsidDependencies(); // LSID/Vector

                for (Iterator<String> itSubTasks = mDependencies.keySet().iterator(); itSubTasks.hasNext();) {
                    String keyLsid = itSubTasks.next();
                    LSID subtaskLsid = getLSID(keyLsid);

                    String lsidNoVersion = subtaskLsid.toStringNoVersion();

                    try {
                        TaskInfo subTask = adminClient.getTask(keyLsid);
                        if (subTask != null) {
                            TaskGroup taskGroup = (indexedTasks.containsKey(lsidNoVersion)) ? indexedTasks
                                    .get(lsidNoVersion) : new TaskGroup(subTask, showEveryonesModules);
                                    taskGroup.addVersionInfo(indexedTasks, subTask);
                                    String itKey = keyLsid + "." + subTask.getID();

                                    //taskGroup.setPipelineName(keyLsid, taskInfo);
                                    taskGroup.setPipelineName(itKey, taskInfo);
                                    indexedTasks.put(lsidNoVersion, taskGroup);
                        }
                    } catch (WebServiceException e) {
                        log.error(e);
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        public void deleteVersionInfo(String lsid) {
            indexedVersions.remove(lsid);
        }

        public String getDescription() {
            return description;
        }

        public Collection<VersionInfo> getIndexedVersions() {
            return indexedVersions.values();
        }

        public String getLsidNoVersion() {
            return lsidNoVersion;
        }

        public String getName() {
            return indexedVersions.isEmpty() ? "" : indexedVersions.get(indexedVersions.firstKey()).getName();
        }

        public boolean isAllowed() {
            return !isAllUsedBy() && isOneAllowed();
        }

        public boolean isAllUsedBy() {
            boolean allUsedBy = true;
            for (VersionInfo info : indexedVersions.values()) {
                if (!info.isUsedBy()) {
                    allUsedBy = false;
                    break;
                }
            }
            return allUsedBy;
        }

        public boolean isOneAllowed() {
            boolean oneAllowed = false;
            for (VersionInfo info : indexedVersions.values()) {
                if (info.isAllowed()) {
                    oneAllowed = true;
                    break;
                }
            }
            return oneAllowed;
        }

        public boolean isPipeline() {
            return pipeline;
        }

        private void setPipelineName(String lsid, TaskInfo ti) {
            VersionInfo vi = indexedVersions.get(lsid);
            if (vi != null)
                vi.addPipelineName(ti);
        }
    }

    public static class VersionInfo {
        private List<String> pipelineNames = new ArrayList<String>();
        private boolean isUsedBy = false;
        private TaskInfo ti;
        private boolean deleteAuthorized = false;
        private boolean editAuthorized = false;

        public VersionInfo() {
        }

        public VersionInfo(TaskInfo ti) {
            this.ti = ti;
            String userId = UIBeanHelper.getUserId();
            deleteAuthorized = ti.getUserId().equals(userId) || AuthorizationHelper.adminModules();
            editAuthorized = ti.getUserId().equals(userId) && LSIDUtil.getInstance().isAuthorityMine(ti.getLsid());
        }

        public void addPipelineName(TaskInfo pti) {
            pipelineNames.add(pti.getName() + " ver. " + getLSID(pti.getLsid()).getVersion());
            isUsedBy = true;
        }

        public String getLsid() {
            return (ti != null) ? ti.getLsid() : null;
        }

        public String getName() {
            return ti.getName();
        }

        public String getOwner() {
            return ti != null ? ti.getUserId() : null;
        }

        public List<String> getPipelineNames() {
            return pipelineNames;
        }

        public String getReason() {
            String reason = null;
            if (ti != null) {
                LSID lSID = getLSID(ti.getLsid());
                String authority = (lSID == null ? "" : lSID.getAuthority());

                TaskInfoAttributes tia = ti.giveTaskInfoAttributes();
                reason = tia.get(GPConstants.VERSION);
                if (reason.equals("1.0") && ("broadinstitute.org".equals(authority) || "broad.mit.edu".equals(authority))) {
                    reason = "";
                }
            }
            return reason;
        }

        public String getVersion() {
            return (ti != null) ? getLSID(ti.getLsid()).getVersion() : null;
        }

        public boolean isAllowed() {
            return (!isUsedBy && deleteAuthorized);
        }

        public boolean isDeleteAuthorized() {
            return deleteAuthorized;
        }

        public boolean isEditAuthorized() {
            return editAuthorized;
        }

        public boolean isUsedBy() {
            return isUsedBy;
        }
    }

    private static LSID getLSID(String lsid) {
        LSID lSID = null;
        try {
            lSID = new LSID(lsid);
        } catch (MalformedURLException mue) {
            return null;
        }
        return lSID;
    }
}
