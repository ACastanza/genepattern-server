package org.genepattern.webservice;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.genepattern.server.TaskIDNotFoundException;
import org.genepattern.server.TaskLSIDNotFoundException;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.TaskMaster;
import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.EulaManager;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.util.GPConstants;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;

/**
 * Use this cache to fetch a TaskInfo for a given lsid.
 * @author pcarr
 */
public class TaskInfoCache {
    private static Logger log = Logger.getLogger(TaskInfoCache.class);
    
    public static TaskInfoCache instance() {
        return Singleton.taskInfoCache;
    }
    private static class Singleton {
        final static TaskInfoCache taskInfoCache = new TaskInfoCache();
        private Singleton() {
        }
    }
    private static TaskInfo taskInfoFromTaskMaster(TaskMaster tm, TaskInfoAttributes taskInfoAttributes) {
        return new TaskInfo(
                tm.getTaskId(), 
                tm.getTaskName(), 
                tm.getDescription(), 
                tm.getParameterInfo(),
                taskInfoAttributes, 
                tm.getUserId(), 
                tm.getAccessId());
    }
    
    private boolean enableCache = false;

    private final ConcurrentMap<Integer, TaskMaster> taskMasterCache = new ConcurrentHashMap<Integer, TaskMaster>();
    private final ConcurrentMap<Integer, TaskInfoAttributes> taskInfoAttributesCache = new ConcurrentHashMap<Integer, TaskInfoAttributes>();
    private final ConcurrentMap<Integer, List<String>> taskDocFilenameCache = new ConcurrentHashMap<Integer, List<String>>();
    
    private TaskInfoCache() {
        ServerConfiguration.Context serverContext = ServerConfiguration.Context.getServerContext();
        enableCache = ServerConfiguration.instance().getGPBooleanProperty(serverContext, "taskInfoCache.enable", false);
        boolean initializeCache = 
            enableCache && ServerConfiguration.instance().getGPBooleanProperty(serverContext, "taskInfoCache.initialize", true);
        if (initializeCache) {
            initializeCache();
        }
    }

    private void initializeCache() {
        boolean closeDbSession = true;
        List<TaskMaster> allTaskMasters = findAll(closeDbSession);
        for(TaskMaster taskMaster : allTaskMasters) {
            addToCache(taskMaster);
        }
    }
    
    private void addToCache(TaskMaster taskMaster) {
        Integer taskId = taskMaster.getTaskId();
        taskMasterCache.put(taskId, taskMaster);
        TaskInfoAttributes taskInfoAttributes = TaskInfoAttributes.decode(taskMaster.getTaskinfoattributes());
        taskInfoAttributesCache.put(taskId, taskInfoAttributes);
    }

    public void clearCache() {
        taskMasterCache.clear();
        taskInfoAttributesCache.clear();
        taskDocFilenameCache.clear();
    }

    public void removeFromCache(String lsid) {
        int taskId = findTaskId(lsid);
        DirectoryManager.removeTaskLibDirFromCache(lsid);
        removeFromCache(taskId);
    }

    public void removeFromCache(Integer taskId) {
        TaskMaster tm = taskMasterCache.get(taskId);
        if (tm != null && tm.getLsid() != null) {
            DirectoryManager.removeTaskLibDirFromCache(tm.getLsid());
        }
        taskMasterCache.remove(taskId);
        taskInfoAttributesCache.remove(taskId);
        taskDocFilenameCache.remove(taskId);
    }
    
    private static class IsDocFilenameFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return GenePatternAnalysisTask.isDocFile(name) && !name.equals("version.txt");
        }
    }
    private static FilenameFilter isDocFilenameFilter = new IsDocFilenameFilter();
    private static class DocFilenameComparator implements Comparator { 
        public int compare(Object o1, Object o2) {
            if (((File) o1).getName().equals("version.txt")) {
                return 1;
            }
            return ((File) o1).getName().compareToIgnoreCase(((File) o2).getName());
        }
    }
    private static Comparator docFilenameComparator = new DocFilenameComparator();
    /**
     * 
     * @param lsid
     * @return null if the taskLibDir does not exist or can't be read.
     */
    private List<String> listDocFilenames(String lsid) {
        List<String> docFilenames = new ArrayList<String>();
        File taskLibDir = null;
        try {
            String libDir = DirectoryManager.getLibDir(lsid);
            taskLibDir = new File(libDir);
        } 
        catch (Exception e) {
            log.error(e);
            return docFilenames;
        }
        
        if (!taskLibDir.canRead()) {
            //this happens when loading a task which has not yet been extracted to the file system
            return null;
        }
        
        List<File> docFiles = new ArrayList<File>(Arrays.asList(taskLibDir.listFiles(isDocFilenameFilter)));
        List<File> approvedDocFiles = new ArrayList<File>();
        
        // Filter out the license files
        List<EulaInfo> infos = EulaManager.instance(new Context()).getEulas(TaskInfoCache.instance().getTask(lsid));
        if (infos.size() > 0) {
            for (File file : docFiles) {
                boolean approved = true;
                 for (EulaInfo info : infos) {
                     if (file.getName().equals(info.getLicense())) {
                         approved = false;
                     }
                 }
                 if (approved) {
                     approvedDocFiles.add(file);
                 }
            }
            docFiles = approvedDocFiles;
        }
        
        
        boolean hasDoc = docFiles != null && docFiles.size() > 0;
        if (hasDoc) {
            // put version.txt last, all others alphabetically
            Collections.sort(docFiles, docFilenameComparator);
        }
        if (docFiles == null) {
            return docFilenames;
        }
        for(File docFile : docFiles) {
            docFilenames.add(docFile.getName());
        }
        return docFilenames;
    }
   
    //helper DAO methods
    private List<TaskMaster> findAll(boolean closeTransaction) throws ExceptionInInitializerError {
        StatelessSession session = null;
        try {
            SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
            if (sessionFactory == null) {
                throw new ExceptionInInitializerError("Hibernate session factory is not initialized");
            }
            session = sessionFactory.openStatelessSession();
            session.beginTransaction();
            
            String hql = "from org.genepattern.server.domain.TaskMaster";
            Query query = session.createQuery(hql);
            List<TaskMaster> taskMasters = query.list();
            return taskMasters;
        }
        finally {
            if (closeTransaction) {
                if (session != null) {
                    session.close();
                }
            }
        }
    }
    
    public TaskInfo getTask(String lsid) throws TaskLSIDNotFoundException {
        int taskId = findTaskId(lsid);
        if (taskId >= 0) {
            return getTask(taskId);
        }
        else {
            throw new TaskLSIDNotFoundException(lsid);
        }
    }

    public TaskInfo getTask(Integer taskId) throws TaskIDNotFoundException {
        TaskInfo taskInfo = null;
        if (enableCache) {
            taskInfo = getTaskInfoFromCache(taskId);
        }
        if (taskInfo == null) {
            TaskMaster taskMaster = getTaskMasterFromDb(taskId, enableCache);
            taskInfo = getTaskInfoFromTaskMaster(taskMaster, enableCache);
        }
        return taskInfo;
    }
    
    private TaskInfo getTaskInfoFromCache(Integer taskId) throws TaskIDNotFoundException {
        TaskMaster taskMaster = taskMasterCache.get(taskId);
        if (taskMaster == null) {
            return null;
        }
        TaskInfoAttributes taskInfoAttributes = taskInfoAttributesCache.get(taskId);
        if (taskInfoAttributes == null) {
            log.error("taskInfoAttributes for task id "+taskId+" is not in the taskInfoAttributesCache");
            taskInfoAttributes = TaskInfoAttributes.decode(taskMaster.getTaskinfoattributes());
            taskInfoAttributesCache.put(taskId, taskInfoAttributes);
        }
        TaskInfo taskInfo = taskInfoFromTaskMaster(taskMaster, taskInfoAttributes);
        return taskInfo;
    }

    private TaskMaster getTaskMasterFromDb(final Integer taskId, final boolean addToCache) throws TaskIDNotFoundException {
        //fetch from DB, then [optionally] add to cache
        TaskMaster taskMaster = findById(taskId);
        if (taskMaster == null) {
            throw new TaskIDNotFoundException(taskId);
        }
        if (addToCache) {
            addToCache(taskMaster);
        }
        return taskMaster;
    }

    private TaskMaster findById(Integer taskId) {
        String hql = "from org.genepattern.server.domain.TaskMaster where taskId = :taskId";
        HibernateUtil.beginTransaction();
        Session session = HibernateUtil.getSession();
        Query query = session.createQuery(hql);
        query.setInteger("taskId", taskId);
        TaskMaster taskMaster = (TaskMaster) query.uniqueResult();
        return taskMaster;
    }

    private TaskInfo getTaskInfoFromTaskMaster(TaskMaster taskMaster, boolean enableCache) {
        TaskInfoAttributes taskInfoAttributes = null;
        if (enableCache) {
            taskInfoAttributes = taskInfoAttributesCache.get(taskMaster.getTaskId());
        }
        if (taskInfoAttributes == null) {
            taskInfoAttributes = TaskInfoAttributes.decode(taskMaster.getTaskinfoattributes());
        }
        TaskInfo taskInfo = taskInfoFromTaskMaster(taskMaster, taskInfoAttributes);
        return taskInfo;
    }

    public TaskInfo[] getAllTasks() {
        List<Integer> allTaskIds = findAllTaskIds();
        return getTasks(allTaskIds);
    }
    
    private List<Integer> findAllTaskIds() {
        String hql = "select taskId from org.genepattern.server.domain.TaskMaster";
        Session session = HibernateUtil.getSession();
        Query query = session.createQuery(hql);
        List<Integer> results = query.list();
        return results;
    }
    
    private Integer findTaskId(String lsid) {
        String hql = "select taskId from org.genepattern.server.domain.TaskMaster where lsid = :lsid";
        Session session = HibernateUtil.getSession();
        Query query = session.createQuery(hql);
        query.setString("lsid", lsid);
        List<Integer> results = query.list();
        if (results == null || results.size() == 0) {
            return -1;
        }
        else if (results.size() == 1) {
            return results.get(0);
        }
        else {
            return -1;
        }
    }
    
    public TaskInfo[] getTasks(List<Integer> taskIds) {
        List<TaskInfo> allTaskInfos = new ArrayList<TaskInfo>();
        for(Integer taskId : taskIds) {
            try {
                TaskInfo taskInfo = getTask(taskId);
                allTaskInfos.add(taskInfo);
            }
            catch (TaskIDNotFoundException e) {
                log.error("Missing task info: ", e);
            }
        }
        TaskInfo[] taskInfoArray = allTaskInfos.toArray(new TaskInfo[allTaskInfos.size()]);
        return taskInfoArray;        
    }
    
    /**
     * Get the list of declared documentation files for this module.
     * 
     * Special cases:
     *     1) if null is returned, it means there is no 'taskDoc=' field in the manifest.
     *     2) if an empty list is returned, it means there is a 'taskDoc=' field in the manifest, but it's value is an empty string
     *     
     * @param lsid
     * @return
     */
    private List<String> getDeclaredDoc(String lsid) {
        TaskInfo info = getTask(lsid);
        boolean hasDeclaredDoc=info.getTaskInfoAttributes().containsKey(GPConstants.TASK_DOC);
        if (!hasDeclaredDoc) {
            log.debug("no declared doc for task, lsid="+lsid);
            return null;
        }
        String taskDoc = info.getTaskInfoAttributes().get(GPConstants.TASK_DOC);
        if (taskDoc.trim().length()==0) {
            log.debug("taskDoc=<whitespace>");
            taskDoc="";
        }
        if (taskDoc.length()==0) {
            return Collections.emptyList();
        }        
        List<String> docList = new ArrayList<String>();
        docList.add(taskDoc);
        return docList;
    }
    
    /**
     * Get the documentation files for the given task.
     * The list of doc file names is cached, for modules with at least one doc file.
     * 
     * Note: this could be improved, by caching empty lists of doc file names, but it is rather tricky to do so
     *     because of the way module creation, editing, and cloning is implemented.
     * 
     * @param taskId
     * @param lsid
     * @return
     */
    public List<String> getDocFilenames(Integer taskId, String lsid) {
        List<String> docFilenames = null;
        if (enableCache) {
            docFilenames = taskDocFilenameCache.get(taskId);
        }
        if (docFilenames != null) { 
            // for debugging
            if (log.isTraceEnabled()) {
                if (docFilenames.size() == 0) {
                    log.trace("no doc found for task, taskId="+taskId+", lsid="+lsid);
                }
            }
            return Collections.unmodifiableList(docFilenames);
        }
        
        docFilenames = this.getDeclaredDoc(lsid);
        
        if (docFilenames == null) {
            docFilenames = this.listDocFilenames(lsid);
        }

        //don't cache empty lists, see javadoc for details
        if (enableCache && docFilenames != null && docFilenames.size() > 0) {
            taskDocFilenameCache.put(taskId, docFilenames);
        }
        
        if (docFilenames == null) {
            log.warn("unexpected null value in getDocFilenames for taskId="+taskId+", lsid="+lsid);
            docFilenames = Collections.emptyList();
        }
        
        return docFilenames;
    }

    public void deleteTask(Integer taskId) {
        if (enableCache) {
            removeFromCache(taskId);
        }
    }
    
    /**
     * Get the set of module categories <pre>taskInfoAttributes.get('taskType')</pre> for all installed modules in this GP server.
     * 
     */
    public SortedSet<String> getAllCategories() {
        SortedSet<String> categories = new TreeSet<String>(new Comparator<String>() {
            // sort categories alphabetically, ignoring case
            public int compare(String arg0, String arg1) {
                String arg0tl = arg0.toLowerCase();
                String arg1tl = arg1.toLowerCase();
                int rval = arg0tl.compareTo(arg1tl);
                if (rval == 0) {
                    rval = arg0.compareTo(arg1);
                }
                return rval;
            }
        });
        TaskInfo[] taskInfos = getAllTasks();
        for(TaskInfo ti : taskInfos) {
            String taskType = ti.getTaskInfoAttributes().get("taskType");
            if (taskType == null || taskType.trim().length() == 0) {
                //ignore null and blank
            }
            else {
                categories.add(taskType); 
            }
        }
        return Collections.unmodifiableSortedSet(categories);
    }
}
