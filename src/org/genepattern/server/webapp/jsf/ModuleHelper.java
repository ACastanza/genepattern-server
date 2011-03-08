/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import static org.genepattern.server.webapp.jsf.UIBeanHelper.getUserId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.Suite;
import org.genepattern.server.domain.SuiteDAO;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserPropKey;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;

public class ModuleHelper {
    private static Logger log = Logger.getLogger(ModuleHelper.class);

    private TaskInfo[] tasks;

    public ModuleHelper() {
	this(false);
    }

    /**
     * 
     */
    public ModuleHelper(boolean allVersions) {
	if (allVersions) {
	    tasks = (new AdminDAO()).getAllTasksForUser(getUserId());
	} else {
	    tasks = (new AdminDAO()).getLatestTasks(getUserId());
	}
    }

    /**
     * @return
     */
    public ModuleCategory getRecentlyUsed() {
	AdminDAO dao = new AdminDAO();
	String userId = UIBeanHelper.getUserId();
	int recentJobsToShow = Integer.parseInt(new UserDAO().getPropertyValue(userId, UserPropKey.RECENT_JOBS_TO_SHOW,
		"4"));
	return new ModuleCategory("Recently Used", dao.getRecentlyRunTasksForUser(getUserId(), recentJobsToShow));
    }

    /**
     * @return
     */
    public ModuleCategory getTasks() {
	AdminDAO dao = new AdminDAO();
	return new ModuleCategory("All", tasks);
    }

    /**
     * @return
     */
    public List<ModuleCategory> getTasksByType() {

	List<ModuleCategory> categories = new ArrayList<ModuleCategory>();
	Map<String, List<TaskInfo>> taskMap = new HashMap<String, List<TaskInfo>>();

	for (int i = 0; i < tasks.length; i++) {
	    TaskInfo ti = tasks[i];
	    String taskType = ti.getTaskInfoAttributes().get("taskType");
	    if (taskType == null || taskType.length() == 0) {
		taskType = "Uncategorized";
	    }
	    List<TaskInfo> tasks = taskMap.get(taskType);
	    if (tasks == null) {
		tasks = new ArrayList<TaskInfo>();
		taskMap.put(taskType, tasks);
	    }
	    tasks.add(ti);
	}

	List<String> categoryNames = new ArrayList<String>(taskMap.keySet());
	Collections.sort(categoryNames);
	for (String categoryName : categoryNames) {
	    TaskInfo[] modules = new TaskInfo[taskMap.get(categoryName).size()];
	    modules = taskMap.get(categoryName).toArray(modules);
	    categories.add(new ModuleCategory(categoryName, modules));
	}
	return categories;
    }

    /**
     * Gets all module categories for the modules in this <tt>ModuleHelper</tt> instance.
     * 
     * @param selectedLsids
     *                The module LSIDs that belong to the suite of interest. Used to set the selected and
     *                selectedVersion properties of <tt>Module</tt> instances.
     * @return The module categories.
     */
    public List<ModuleCategory> getSelectedTasksByType(String[] selectedLsids) {
	List<ModuleCategory> categories = new ArrayList<ModuleCategory>();
	Map<String, List<TaskInfo>> taskMap = new HashMap<String, List<TaskInfo>>();

	for (int i = 0; i < tasks.length; i++) {
	    TaskInfo ti = tasks[i];
	    String taskType = ti.getTaskInfoAttributes().get("taskType");
	    if (taskType == null || taskType.length() == 0) {
		taskType = "Uncategorized";
	    }
	    List<TaskInfo> tasks = taskMap.get(taskType);
	    if (tasks == null) {
		tasks = new ArrayList<TaskInfo>();
		taskMap.put(taskType, tasks);
	    }
	    tasks.add(ti);
	}

	List<String> categoryNames = new ArrayList<String>(taskMap.keySet());
	Collections.sort(categoryNames);
	for (String categoryName : categoryNames) {
	    TaskInfo[] modules = taskMap.get(categoryName).toArray(new TaskInfo[0]);
	    ModuleCategory mc = new ModuleCategory(categoryName, modules);
	    mc.setSelected(Arrays.asList(selectedLsids));
	    categories.add(mc);
	}
	return categories;
    }

    /**
     * Return a list of module categories grouped by suite.
     * 
     * @return
     */
    public List<ModuleCategory> getTasksBySuite() {
	List<Suite> suites = (new SuiteDAO()).findAll();
	
	String userId = UIBeanHelper.getUserId();
	//user must be logged in ...
	if (userId == null || userId.trim().equals("")) {
	    return new ArrayList<ModuleCategory>();
	}
	
	List<ModuleCategory> categories = new ArrayList<ModuleCategory>(suites.size());
	for (Suite suite : suites) {
	    if (!userId.equals(suite.getUserId()) && suite.getAccessId().intValue() != GPConstants.ACCESS_PUBLIC) {
	        //don't include private suites unless owned by someone else
	        continue;
	    }
	    
	    List<String> lsids = suite.getModules();
	    List<TaskInfo> suiteTasks = new ArrayList<TaskInfo>();
	    for (String lsid : lsids) {
		TaskInfo ti = new AdminDAO().getTask(lsid, userId);
		if (ti != null) {
		    suiteTasks.add(ti);
		}
	    }
	    TaskInfo[] taskArray = suiteTasks.toArray(new TaskInfo[0]);
	    categories.add(new ModuleCategory(suite.getName(), taskArray, false, suite.getLsid()));
	}
	return categories;

    }

    /**
     * Used when viewing/editing a suite. If modules not in suite, selected version of <tt>Module</tt> set to latest,
     * otherwise selected version set to version in suite.
     * 
     * @param suite
     *                The suite.
     * @return The modules.
     */
    public List<ModuleCategory> getTasksByTypeForSuite(Suite suite) {
	List<ModuleCategory> categories = new ArrayList<ModuleCategory>();
	List<String> lsids = suite.getModules();
	Map<String, Map<String, TaskInfo>> taskMap = new HashMap<String, Map<String, TaskInfo>>();

	Map<String, TaskInfo> queriedTasks = (new AdminDAO()).getAllTasksForUserWithLsids(getUserId(), lsids);
	for (Map.Entry entry : queriedTasks.entrySet()) {
	    addTaskInfoToMap(entry, taskMap);
	}

	List<String> categoryNames = new ArrayList<String>(taskMap.keySet());
	Collections.sort(categoryNames);
	ModuleCategory mc;
	Map<String, TaskInfo> lsidToTaskInfoMap;
	for (String categoryName : categoryNames) {
	    TaskInfo[] modules = new TaskInfo[taskMap.get(categoryName).size()];
	    lsidToTaskInfoMap = taskMap.get(categoryName);

	    modules = taskMap.get(categoryName).values().toArray(modules);
	    mc = new ModuleCategory(categoryName, modules);
	    mc.setSelectedVersionOfModules(lsidToTaskInfoMap);

	    categories.add(mc);
	}
	return categories;
    }

    /**
     * @param ti
     * @param taskMap
     */
    private void addTaskInfoToMap(Map.Entry entry, Map<String, Map<String, TaskInfo>> taskMap) {
	TaskInfo ti = (TaskInfo) entry.getValue();
	String taskType = ti.getTaskInfoAttributes().get("taskType");
	if (taskType == null || taskType.length() == 0) {
	    taskType = "Uncategorized";
	}
	Map<String, TaskInfo> tasks = taskMap.get(taskType);
	if (tasks == null) {
	    tasks = new HashMap<String, TaskInfo>();
	    taskMap.put(taskType, tasks);
	}
	tasks.put((String) entry.getKey(), (TaskInfo) entry.getValue());
    }

}
