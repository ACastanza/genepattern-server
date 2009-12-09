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

package org.genepattern.server.webservice.server;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

/**
 * Directory Manager - does the heavy lifting of creating and finding directories for suites, pipelines and tasks
 * 
 * @author Joshua Gould
 */
public class DirectoryManager {

    /**
     * location on server of taskLib directory where per-task support files are stored
     */
    private static String taskLibDir = null;

    /** mapping of LSIDs to taskLibDir directories */
    protected static Hashtable htTaskLibDir = new Hashtable();

    protected static Hashtable htSuiteLibDir = new Hashtable();

    private static Logger _cat = Logger.getLogger("org.genepattern.server.webservice.server.DirectoryManager");

    /**
     * Locates the directory where the a particular task's files are stored. It is one level below
     * $omnigene.conf/taskLib. TODO: involve userID in this, so that there is no conflict among same-named private
     * tasks. Creates the directory if it doesn't already exist.
     * 
     * @param taskName
     *                name of task to look up
     * @return directory name on server where taskName support files are stored
     * @throws Exception
     *                 if genepattern.properties System property not defined
     * @author Jim Lerner
     */

    public static String getLibDir(String lsid) throws Exception, MalformedURLException {
	LSID l = new LSID(lsid);
	if (l.getAuthority().equals("") || l.getIdentifier().equals("") || !l.hasVersion()) {
	    throw new MalformedURLException("invalid LSID");
	}

	if (LSIDUtil.isSuiteLSID(lsid)) {
	    return getSuiteLibDir(null, lsid, null);

	} else {
	    return getTaskLibDir(null, lsid, null);
	}
    }

    /**
     * Locates the directory where the a particular task's files are stored. It is one level below
     * $omnigene.conf/taskLib. TODO: involve userID in this, so that there is no conflict among same-named private
     * tasks. Creates the directory if it doesn't already exist.
     * 
     * Warning: this method creates new DB connections, it is up to the calling method to close 
     *     the db connection if necessary.
     * 
     * @param taskName, name of task to look up
     * @return directory name on server where taskName support files are stored
     * @author Jim Lerner
     * @throws MalformedURLException, If the lsid is not properly formed.
     * @throws IllegalArgumentException, If the task name or lsid is not found in the database.
     */
    public static String getTaskLibDir(String taskName, String sLSID, String username) throws MalformedURLException {
        String ret = null;
        if (sLSID != null) {
            ret = (String) htTaskLibDir.get(sLSID);
            if (ret != null) {
                return ret;
            }
        }

        File f = null;
        getLibDir();
        LSID lsid = null;
        TaskInfo taskInfo = null;

        if (sLSID != null && sLSID.length() > 0) {
            lsid = new LSID(sLSID);
            if (taskName == null || taskInfo == null) {
                // lookup task name for this LSID
                taskInfo = (new AdminDAO()).getTask(lsid.toString(), username);
                if (taskInfo == null) {
                    throw new IllegalArgumentException("can't get TaskInfo from " + lsid.toString());
                }
                if (taskInfo != null) {
                    taskName = taskInfo.getName();
                    if (username == null) {
                        username = taskInfo.getUserId();
                    }
                }
            }
        }

        if (lsid == null && taskName != null) {
            lsid = new LSID(taskName);
            // lookup task name for this LSID
            taskInfo = (new AdminDAO()).getTask(lsid.toString(), username);
            if (taskInfo == null) {
                throw new IllegalArgumentException("can't get TaskInfo from " + lsid.toString());
            }
            taskName = taskInfo.getName();
            if (username == null) {
                username = taskInfo.getUserId();
            }
        }

        String dirName = makeDirName(lsid, taskName, taskInfo);
        f = new File(taskLibDir, dirName);
        f.mkdirs();
        try {
            ret = f.getCanonicalPath();
        } 
        catch (IOException e) {
            ret = f.getPath();
        }
        if (lsid != null) {
            htTaskLibDir.put(lsid, ret);
        }
        return ret;
    }

    /**
     * Locates the directory where the a particular task's files are stored. It is one level below
     * $omnigene.conf/taskLib. TODO: involve userID in this, so that there is no conflict among same-named private
     * tasks. Creates the directory if it doesn't already exist.
     * 
     * @param taskName
     *                name of task to look up
     * @return directory name on server where taskName support files are stored
     * @throws Exception
     *                 if genepattern.properties System property not defined
     * @author Jim Lerner (Moved to DirManager from GenePatternAnalysisTask by Ted Liefeld)
     */
    public static String getTaskLibDir(TaskInfo taskInfo) throws Exception {
	File f = null;
	getLibDir();

	String taskName = taskInfo.getName();
	TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
	LSID lsid = null;
	try {
	    lsid = new LSID(tia.get(GPConstants.LSID));
	} catch (MalformedURLException mue) {
	    // ignore -- not an LSID
	} catch (Exception e2) {
	}

	String dirName = makeDirName(lsid, taskName, taskInfo);
	f = new File(taskLibDir, dirName);
	f.mkdirs();
	return f.getCanonicalPath();
    }

    protected static String makeDirName(LSID lsid, String taskName, TaskInfo taskInfo) {
	String dirName;
	int MAX_DIR_LENGTH = 255; // Mac OS X directory name limit
	String version;

	String invariantPart = (taskInfo != null ? ("" + taskInfo.getID()) : Integer.toString(Math.abs(taskName
		.hashCode()), 36)); // [a-z,0-9];
	if (lsid != null) {
	    // invariantPart = lsid.getAuthority() + "-" + lsid.getNamespace() +
	    // "-" + lsid.getIdentifier();
	    version = lsid.getVersion();
	    if (version.equals("")) {
		// invariantPart = "" + Math.random() + "-" + Math.random() ;
		version = "tmp";
	    }
	    // String hashBase36 =
	    // Integer.toString(Math.abs(invariantPart.hashCode()), 36); //
	    // [a-z,0-9]
	} else {
	    // try { throw new Exception("no LSID given"); } catch (Exception e)
	    // { System.out.println(e.getMessage()); e.printStackTrace(); }
	    dirName = taskName;
	    version = "1";
	}
	dirName = "." + version + "." + invariantPart; // hashBase36;
	dirName = taskName.substring(0, Math.min(MAX_DIR_LENGTH - dirName.length(), taskName.length())) + dirName;

	return dirName;
    }

    protected static String makeDirName(LSID lsid, String taskName) {
	String dirName;
	int MAX_DIR_LENGTH = 255; // Mac OS X directory name limit
	String version;
	String invariantPart = Integer.toString(Math.abs(taskName.hashCode()), 36); // [a-z,0-9];
	if (lsid != null) {
	    // invariantPart = lsid.getAuthority() + "-" + lsid.getNamespace() +
	    // "-" + lsid.getIdentifier();
	    version = lsid.getVersion();
	    if (version.equals("")) {
		// invariantPart = "" + Math.random() + "-" + Math.random() ;
		version = "tmp";
	    }
	    // String hashBase36 =
	    // Integer.toString(Math.abs(invariantPart.hashCode()), 36); //
	    // [a-z,0-9]
	} else {
	    // try { throw new Exception("no LSID given"); } catch (Exception e)
	    // { System.out.println(e.getMessage()); e.printStackTrace(); }
	    dirName = taskName;
	    version = "1";
	}
	dirName = "." + version + "." + invariantPart; // hashBase36;
	dirName = taskName.substring(0, Math.min(MAX_DIR_LENGTH - dirName.length(), taskName.length())) + dirName;

	return dirName;
    }

    protected static String getLibDir() {
	if (taskLibDir == null) {
	    taskLibDir = System.getProperty("tasklib");
	    if (taskLibDir == null || !new File(taskLibDir).exists()) {
		taskLibDir = ".." + File.separator + "taskLib";
	    }
	    File f = new File(taskLibDir);
	    try {
		taskLibDir = f.getCanonicalPath();
	    } catch (IOException e) {
		e.printStackTrace();
		taskLibDir = f.getPath();
	    }
	}
	return taskLibDir;
    }

    /**
     * Locates the directory where the a particular task's files are stored. It is one level below
     * $omnigene.conf/taskLib. TODO: involve userID in this, so that there is no conflict among same-named private
     * tasks. Creates the directory if it doesn't already exist.
     * 
     * @param taskName
     *                name of task to look up
     * @return directory name on server where taskName support files are stored
     * @throws Exception
     *                 if genepattern.properties System property not defined
     * @author Jim Lerner
     */
    public static String getSuiteLibDir(String suiteName, String sLSID, String username) throws Exception {
	String ret = null;
	String name = suiteName;
	if (suiteName == null) {
	    IAdminClient adminClient = new LocalAdminClient(username);
	    SuiteInfo si = adminClient.getSuite(sLSID);
	    name = si.getName();
	}

	if (sLSID != null) {
	    ret = (String) htSuiteLibDir.get(sLSID);
	    if (ret != null)
		return ret;
	}

	try {
	    File f = null;
	    getLibDir();

	    LSID lsid = null;

	    String dirName = makeDirName(lsid, name);
	    f = new File(taskLibDir, dirName);
	    f.mkdirs();
	    ret = f.getCanonicalPath();
	    if (lsid != null) {
		htTaskLibDir.put(lsid, ret);
	    }
	    return ret;
	} catch (Exception e) {
	    // e.printStackTrace();
	    throw e;
	}
    }

}
