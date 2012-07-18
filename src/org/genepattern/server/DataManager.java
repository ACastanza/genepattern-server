package org.genepattern.server;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.userupload.UserUploadManager;
import org.genepattern.server.dm.userupload.dao.UserUpload;
import org.genepattern.server.dm.userupload.dao.UserUploadDao;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;

/**
 * Utility class for managing data files.
 * 
 * @author pcarr
 *
 */
public class DataManager {
    private static Logger log = Logger.getLogger(DataManager.class);
    public final static List<String> FILE_EXCLUDES = new ArrayList<String>();
    static {
        FILE_EXCLUDES.add(".DS_Store");
        FILE_EXCLUDES.add("Thumbs.db");
    }
    
    /**
     * Create a subdirectory in the user's upload directory and records the entry in the DB.
     * 
     *  TODO: refactor this into the UserUploadManager (or at least into the dm pacakge)
     * 
     * @param userContext, requires a valid userId
     * @param parentPath, the path to the parent directory, specified relative to the user's upload directory.
     *     It can be null. When null it means create a subdir in the user upload dir.
     * @param name, the filename
     * 
     * @return true if the directory was successfully created
     */
    public static boolean createSubdirectory(ServerConfiguration.Context userContext, String parentPath, String name) {
        File relativePath = null;
        //numerous tests for the parentPath ...
        if (parentPath == null || parentPath.length() == 0 || ".".equals(parentPath) || "./".equals(parentPath) ) {
            //mkdir in the user's upload dir
            relativePath = new File(name);
        }
        else {
            //mkdir in a path relative to the user's upload dir
            relativePath = new File(parentPath, name);
        }
        GpFilePath subdirRef = null;
        try {
            //another option ... subdirRef = GpFileObjFactory.getUserUploadFile(userContext, relativePath);
            boolean initMetaData = false;
            subdirRef = UserUploadManager.getUploadFileObj(userContext, relativePath, initMetaData);
        }
        catch (Throwable t) {
            log.error(t.getLocalizedMessage());
            return false;
        }
        File dir = subdirRef.getServerFile();
        boolean success = false;
        try {
            success = dir.mkdir();
        }
        catch (Throwable t) {
            log.error("system error creating directory: "+subdirRef.getRelativeUri()+": "+t.getLocalizedMessage());
            return false;
        } 
        if (success) {
            //update the DB
            try {
                UserUploadManager.createUploadFile(userContext, subdirRef, 1);
                UserUploadManager.updateUploadFile(userContext, subdirRef, 1, 1);
            }
            catch (Throwable t) {
                log.error(t);
                success = false;
            }
        }
        return success;
    }

    /**
     * Delete the user upload file from the server file system, checking permissions based on the given userId.
     * 
     * @param userId, the current user who is requesting to delete the file
     * @param uploadedFileObj, the record of the user upload file to delete
     * 
     * @return true if the file was deleted
     */
    public static boolean deleteUserUploadFile(String userId, GpFilePath uploadedFileObj) {
        File file = uploadedFileObj.getServerFile();
        
        //1) if it exists, delete the file from the file system
        boolean deleted = false;
        boolean canDelete = canDelete(userId, uploadedFileObj);
        if (!canDelete) {
            return false;
        }
        if (!file.exists()) {
            //indicate success even if the file doesn't exist
            deleted = true;
        }
            
        if (file.exists()) {
            deleted = file.delete(); 
            if (!deleted) {
                log.error("Error deleting file: "+file.getPath());
            }
        }
        //2) remove the record from the DB, even if it doesn't exist in the file system
        if (!file.exists()) {
            //if we are in a transaction, don't commit and close
            boolean inTransaction = HibernateUtil.isInTransaction();
            try {
                //this begins a new transaction
                UserUploadDao dao = new UserUploadDao();
                int numDeleted = dao.deleteUserUpload(userId, uploadedFileObj);
                if (numDeleted != 1) {
                    log.error("Error deleting user upload file record from db, '"+uploadedFileObj.getRelativeUri()+"'. numDeleted="+numDeleted);
                    if (numDeleted > 1) {
                        deleted = false;
                        //rollback if more than one row was deleted
                        HibernateUtil.rollbackTransaction();
                    }
                }
                if (!inTransaction) {
                    HibernateUtil.commitTransaction();
                }
            }
            catch  (Throwable t) {
                deleted = false;
                //possible error updating the DB
                log.error("Error deleting user upload file record from db, '"+uploadedFileObj.getRelativeUri()+"'", t);
                HibernateUtil.rollbackTransaction();
            }
        } 
        return deleted;
    }

    /**
     * Checks whether the given user has permission to delete the server file at the given filePath reference.
     * 
     * Note: If for some reason the file no longer exists on the server, still return true.
     * TODO: should have better error handling/doc for when the file is still in the DB but not in the file system.
     * 
     * @param currentUser
     * @param uf
     * @return
     */
    private static boolean canDelete(String currentUserId, GpFilePath uf) {
        if (uf == null) {
            return false;
        }
        File toDel = uf.getServerFile();
        if (!toDel.exists()) {
            //Note: returning true to simplify code
            log.error("Attempt to delete a file which doesn't exist: "+toDel.getPath());
            return true;
        }
        
        if (toDel.isDirectory() && toDel.listFiles().length > 0) {
            log.error("Unable to delete non-empty directories: " + toDel.getPath());
            return false;
        }
        if (currentUserId == null) {
            //require a userid
            log.error("Require a valid userId to delete file: " + toDel.getPath());
            return false;
        }
        if (!toDel.canWrite()) {
            log.error("Server error, GP server doesn't have permission to delete file: "+toDel.getPath());
            return false;
        }

        //TODO: come up with an improved policy for ACL for admin users
        boolean isAdmin = false;
        isAdmin = AuthorizationHelper.adminJobs(currentUserId);
        if (isAdmin) {
            return true;
        }
        
        return currentUserId.equals(uf.getOwner());
    }

    /**
     * Checks whether a file is on the excluded files list.
     * Used when syncing the file system and database.
     * @param file
     * @return
     */
    private static boolean isExcludedFile(File file) {
        for (String i : FILE_EXCLUDES) {
            if (file.getName().equalsIgnoreCase(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Wipes all of a user's uploads from the database, then crawls the upload directory for a given user 
     * and adds database entries for all found files, except those whose filenames match a name on the 
     * exclude files list (used to ignore system files)
     * @param user
     */
    public static void syncUploadFiles(String userId) {
        log.debug("syncUploadFiles(userId='"+userId+"') ...");
        try {
            UserUploadDao dao = new UserUploadDao();
            
            File uploadDir = ServerConfiguration.instance().getUserUploadDir(Context.getContextForUser(userId));
            if (uploadDir == null) {
                log.error("Unable to get the user's upload directory in syncUploadFiles()");
                return;
            }
            
            // Remove all the old database entries
            log.debug("deleting old entries ...");
            int numDeleted = dao.deleteAllUserUpload(userId);
            log.debug("deleted "+numDeleted+" entries from DB");
            HibernateUtil.commitTransaction();

            // Add new entries to the database
            ServerConfiguration.Context userContext = ServerConfiguration.Context.getContextForUser(userId);
            String[] relPath = new String[0];
            Set<String> visitedDirs = new HashSet<String>();
            dao = new UserUploadDao();
            for (File file : uploadDir.listFiles()) {
                handleFileSync(dao, visitedDirs, relPath, file, userContext);
            }

            // Commit
            HibernateUtil.commitTransaction();
        }
        catch (Exception e) {
            log.error("Error committing upload file sync to database");
            HibernateUtil.rollbackTransaction();
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
        log.debug("syncUploadFiles(userId='"+userId+"') ... Done!");
    }
    
    /**
     * Updates the database  with a particular file found when syncing the file system and database.
     * 
     * @param dao
     * @param file
     * @param user
     * @throws Exception
     */
    private static void handleFileSync(UserUploadDao dao, Set<String> visitedDirs, String[] relPath, File file, ServerConfiguration.Context userContext) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("file==null");
        }

        // Exclude file on exclude list (ex: .DS_Store)
        if (isExcludedFile(file)) {
            return;
        }
        
        // avoid circular references, from symbolic links
        if (file.isDirectory()) {
            final String canonicalPath = file.getCanonicalPath();
            if (visitedDirs.contains( canonicalPath )) {
                log.debug("skipping dir, because it was already visited: "+canonicalPath);
                return;
            }
            else {
                visitedDirs.add( canonicalPath );
            }
        }
        
        if (!( (file.isFile() && file.exists()) || file.isDirectory() )) {
            //skip files which don't exist or aren't regular files or directories
            return;
        }

        //need a valid userId to proceed
        if (userContext == null) {
            throw new Exception("Missing required parameter, userContext is null");
        }
        if (userContext.getUserId() == null) {
            throw new Exception("Missing required parameter, userContext.userId is null");
        }
        
        //add this file to the DB
        final UserUpload uploadFile = new UserUpload();
        final String[] newRelPath = new String[relPath.length + 1];
        for(int idx = 0; idx<relPath.length; ++idx) {
            newRelPath[idx] = relPath[idx];
        }
        newRelPath[relPath.length] = file.getName();
        final String relativePath = join(newRelPath, "/");
        uploadFile.setPath(relativePath);
        uploadFile.setUserId(userContext.getUserId());
        uploadFile.setNumParts(1);
        uploadFile.setNumPartsRecd(1);
        uploadFile.init(file);
        dao.saveOrUpdate(uploadFile); 
        
        //if it's a directory, add children
        if (!file.isDirectory()) {
            return;
        }
        //if we're here, it means the file is a directory
        for(File child : file.listFiles()) {
            try {
                handleFileSync(dao, visitedDirs, newRelPath, child, userContext);
            }
            catch (Throwable t) {
                log.error("Error syncing user upload file, newRelPath="+newRelPath+", parent="+file+", child="+child);
            }
        }
    }

    private static String join(String[] arr, String sep) {
        String rval = "";
        boolean first = true;
        for(String s : arr) {
            if (first) {
                rval = s;
                first = false;
            }
            else {
                rval += (sep + s);
            }
        }
        return rval;
    }

}
