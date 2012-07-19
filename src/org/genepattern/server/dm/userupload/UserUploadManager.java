package org.genepattern.server.dm.userupload;

import java.io.File;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.genepattern.server.FileUtil;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpDirectoryNode;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.userupload.dao.UserUpload;
import org.genepattern.server.dm.userupload.dao.UserUploadDao;

public class UserUploadManager {
    private static Logger log = Logger.getLogger(UserUploadManager.class);

    /**
     * Get the root upload directory for the given user.
     * @param userContext, requires a valid userId
     * @return
     * @throws Exception
     */
    static public GpFilePath getUserUploadDir(ServerConfiguration.Context userContext) throws Exception {
        return GpFileObjFactory.getUserUploadDir(userContext);
    }
    
    /**
     * Create an instance of a GpFilePath object for the user upload file. 
     * If there is already a record in the DB, initialize the file meta data.
     * 
     * @param userContext, must contain the valid userId of the user who is uploading the file.
     * @param relativePath, must be a relative file path, relative to the current user's upload directory.
     * @return
     * @throws Exception
     */
    static public GpFilePath getUploadFileObj(Context userContext, File relativePath, boolean initMetaData) throws Exception {
        GpFilePath uploadFilePath = GpFileObjFactory.getUserUploadFile(userContext, relativePath);
        
        //if there is a record in the DB ... 
        boolean isInTransaction = false;
        try {
            isInTransaction = HibernateUtil.isInTransaction();
        }
        catch (Throwable t) {
            String message = "DB connection error: "+t.getLocalizedMessage();
            log.error(message, t);
            throw new Exception(message);
        }
        
        try {
            UserUploadDao dao = new UserUploadDao();
            UserUpload fromDb = dao.selectUserUpload(userContext.getUserId(), uploadFilePath);
            if (initMetaData) {
                initMetadata(uploadFilePath, fromDb);
            }
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
            return uploadFilePath;
        }
        catch (Throwable t) {
            String message = "DB error getting file meta data: "+t.getLocalizedMessage();
            log.error(message, t);
            throw new Exception(message);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    /**
     * Set all file metadata for the given uploadFilePath from the settings in the DB record.
     * 
     * @param uploadFilePath
     * @param fromDb
     */
    static private void initMetadata(GpFilePath uploadFilePath, UserUpload fromDb) {
        if (fromDb == null) {
            log.error("Unexpected null arg");
            return;
        }
        uploadFilePath.setName(fromDb.getName());
        uploadFilePath.setLastModified(fromDb.getLastModified());
        uploadFilePath.setExtension(fromDb.getExtension());
        uploadFilePath.setFileLength(fromDb.getFileLength());
        uploadFilePath.setKind(fromDb.getKind());
        uploadFilePath.setNumParts(fromDb.getNumParts());
        uploadFilePath.setNumPartsRecd(fromDb.getNumPartsRecd()); 
    }

    /**
     * Add a record of the user upload file into the database. 
     * 
     * Iff the current thread is not already in a DB transaction, this method will commit the transaction. 
     * Otherwise, it is up to the calling method to commit or rollback the transaction.
     * 
     * @param userContext, requires a valid userId,
     * @param gpFilePath, a GpFilePath to the upload file
     * @param numParts, the number of parts this file is broken up into, based on the jumploader applet.
     * @throws Exception if a duplicate entry for the file is found in the database
     */
    static public UserUpload createUploadFile(Context userContext, GpFilePath gpFileObj, int numParts) throws Exception {
        UserUpload uu = UserUpload.initFromGpFileObj(userContext, gpFileObj);
        uu.setNumParts(numParts);
        
        boolean inTransaction = HibernateUtil.isInTransaction();
        
        UserUploadDao dao = new UserUploadDao();
        try {
            dao.save( uu );
            if (!inTransaction) {
                HibernateUtil.commitTransaction();
            }
            return uu;
        }
        catch (RuntimeException e) {
            HibernateUtil.rollbackTransaction();
            throw new Exception("Duplicate entry found in the database for file: " + gpFileObj.getRelativePath());
        }
    }

    /**
     * Update the record of the user upload file in the database.

     * Iff the current thread is not already in a DB transaction, this method will commit the transaction. 
     * Otherwise, it is up to the calling method to commit or rollback the transaction.
     * 
     * @param userContext
     * @param gpFilePath
     * @param partNum, part numbers start with 1, e.g. a single chunk upload would have partNum=1 and totalParts=1.
     * @param totalParts
     * @throws Exception, if a part is received out of order.
     */
    static public void updateUploadFile(Context userContext, GpFilePath gpFilePath, int partNum, int totalParts) throws Exception {
        boolean inTransaction = HibernateUtil.isInTransaction();
        try {
            UserUploadDao dao = new UserUploadDao();
            UserUpload uu = dao.selectUserUpload(userContext.getUserId(), gpFilePath);
            if (uu.getNumParts() != totalParts) {
                throw new Exception("Expecting numParts to be " + uu.getNumParts() + " but it was " + totalParts);
            }
            if (uu.getNumPartsRecd() != (partNum - 1)) {
                throw new Exception("Received partial upload out of order, partNum=" + partNum + ", expecting partNum to be " + (uu.getNumPartsRecd() + 1));
            }
            uu.setNumPartsRecd(partNum);
            
            uu.init(gpFilePath.getServerFile());
            dao.saveOrUpdate(uu);
            if (!inTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            throw new Exception("Error updating upload file record for file '" + gpFilePath.getRelativePath() + "': " + t.getLocalizedMessage(), t);
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    /**
     * Get the entire tree of user upload files, rooted at the upload directory for the given user.
     * The root element is the user's upload directory, which typically is not displayed.
     * 
     * @param userContext
     * @param inDir
     * @return
     */
    static public GpDirectoryNode getFileTree(final ServerConfiguration.Context userContext) throws Exception { 
        final GpFilePath userDir = GpFileObjFactory.getUserUploadDir(userContext);
        final GpDirectoryNode root = new GpDirectoryNode(userDir);

        //get the list from the DB
        final List<UserUpload> all = getAllFiles(userContext.getUserId());
        
        //initialize the list of GpFilePath objects
        final SortedMap<String,GpDirectoryNode> allDirs = new TreeMap<String,GpDirectoryNode>();
        final SortedMap<String,GpFilePath> allFiles = new TreeMap<String,GpFilePath>();
        for(UserUpload userUpload : all) {
            GpFilePath uploadFilePath = GpFileObjFactory.getUserUploadFile(userContext, new File(userUpload.getPath()));
            initMetadata(uploadFilePath, userUpload);
            if (UserUpload.isDirectory(userUpload)) {
                GpDirectoryNode gpDirectory = new GpDirectoryNode(uploadFilePath);
                allDirs.put(userUpload.getPath(), gpDirectory);
            }
            else {
                allFiles.put(userUpload.getPath(),uploadFilePath);
            }
        }
        
        //now build the tree
        for(GpDirectoryNode dir : allDirs.values()) {
            GpDirectoryNode parentDir = root;
            final String parentPath = getParentPath(dir.getValue());
            if (parentPath != null) {
                if (allDirs.containsKey(parentPath)) {
                    parentDir = allDirs.get(parentPath);
                }
            }
            parentDir.addChild(dir); 
        }
        for(GpFilePath file : allFiles.values()) {
            GpDirectoryNode parentDir = root;
            final String parentPath = getParentPath(file);
            if (parentPath != null) {
                if (allDirs.containsKey(parentPath)) {
                    parentDir = allDirs.get(parentPath);
                }
            }
            parentDir.addChild(file); 
        }
        
        //for debugging, walk the tree
        //List<String> allPaths = new ArrayList<String>();
        //walk(allPaths,root);
        return root;
    }

//    //just for debugging
//    static private void walk(List<String> allPaths, GpDirectory dir) {
//        String relPath = dir.getValue().getRelativePath();
//        log.debug(relPath);
//        allPaths.add(relPath);
//        for(Node<GpFilePath> child : dir.getChildren()) {
//            if (child instanceof GpDirectory) {
//                walk( allPaths, (GpDirectory) child );
//            }
//            else {
//                final String childPath = child.getValue().getRelativePath();
//                log.debug(childPath);
//                allPaths.add(childPath);
//            }
//        }
//    }

    static private String getParentPath(final GpFilePath file) {
        if (file == null) {
            log.error("Unexpected null arg");
            return null;
        }
        String path = file.getRelativePath();
        int idx = path.lastIndexOf("/");
        if (idx < 0) {
            //has no parent
            return null;
        }
        String parentPath = path.substring(0, idx);
        return parentPath;
    }
    
    /**
     * query all files from the DB, for the given user.
     * @param userId
     * @return
     */
    static private List<UserUpload> getAllFiles(String userId) {
        UserUploadDao dao = new UserUploadDao();
        return dao.selectAllUserUpload(userId);
    }
    
    /**
     * Converts an absolute file path to a path relative to the user's upload directory. 
     * Throws an exception if the absolute path not in that directory.
     * @param context
     * @param absolute The absolute path
     * @return
     * @throws Exception Thrown is the path provided is not in the user's upload dir
     */
    static public String absoluteToRelativePath(Context context, String absolute) throws Exception {
        File userUploadDir = ServerConfiguration.instance().getUserUploadDir(context);
        File absoluteFile = new File(absolute);
        
        // Handle special case of trying to get a relative path to the root upload directory
        if (userUploadDir.getCanonicalPath().equals(absoluteFile.getCanonicalPath())) {
            return "";
        }
        
        String relativePath = FileUtil.getRelativePath(userUploadDir, absoluteFile);
        if (relativePath == null) {
            throw new Exception("Absolute path provided is not in the user's upload directory");
        }
        
        return relativePath;
    }
}

