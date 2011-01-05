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

package org.genepattern.server.database;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.server.webservice.server.dao.BaseDAO;
import org.hsqldb.Server;

public class HsqlDbUtil {
    private static Logger log = Logger.getLogger(HsqlDbUtil.class);

    /**
     * HSQL.args= -port 9001  -database.0 file:../resources/GenePatternDB -dbname.0 xdb
     */
    public static void startDatabase() throws Throwable {
        log.debug("Starting HSQL Database...");
        // TODO - get from properties file
        //  String port = System.getProperty("HSQL_port", "9001");
        //  String dbFile = System.getProperty("HSQL.dbfile", "../resources/GenePatternDB");
        //  String dbUrl = "file:" + dbFile;
        //  String dbName = System.getProperty("HSQL.dbName", "xdb");
        //  String[] args = new String[] { "-port", port, "-database.0", dbUrl, "-dbname.0", dbName };
        String args = System.getProperty("HSQL.args", " -port 9001  -database.0 file:../resources/GenePatternDB -dbname.0 xdb");

        StringTokenizer strTok = new StringTokenizer(args);
        List<String> argsList = new ArrayList<String>();
        //int i=0;
        while (strTok.hasMoreTokens()){
            String tok = strTok.nextToken();
            argsList.add(tok);
        }
        //prevent HSQLDB from calling System.exit when errors occur,
        //    which is the default behavior when starting the DB using Server.main
        if (!argsList.contains("-no_system_exit")) {
            argsList.add("-no_system_exit");
            argsList.add("true");
        }
        String[] argsArray = new String[argsList.size()];
        argsArray = argsList.toArray(argsArray);
        //TODO: instead of invoking main, start the server by constructing the server
        //    so that status can be checked and handled here
        Server.main(argsArray);
        
        try {
            // 1) ...
            HibernateUtil.beginTransaction();
            try {
                // 2) ...
                updateSchema();
                HibernateUtil.commitTransaction();
            }
            catch (Throwable t) {
                // ... 2) can't update the schema
                try {
                    HibernateUtil.rollbackTransaction();
                }
                catch (Throwable ex2) {
                    log.error("Error rolling back transaction: "+ex2.getLocalizedMessage(), ex2);
                }
                throw new Throwable("Error checking or updating db schema: "+t.getLocalizedMessage(), t);
            }
        }
        catch (Throwable t) {
            // ... 1) can't even begin a transaction
            throw new Throwable("Database connection error: "+t.getLocalizedMessage(), t);
        }
        finally {
            //in both cases, attempt to close the session
            try {
                HibernateUtil.closeCurrentSession();
            }
            catch (Exception e) {
                log.error("Exception thrown closing database connection: "+e.getLocalizedMessage(), e);
            }
        }
    }

    public static void shutdownDatabase() {
        try {
            HibernateUtil.beginTransaction();
            log.info("Checkpointing database");
            AnalysisDAO dao = new AnalysisDAO();
            dao.executeUpdate("CHECKPOINT");
            log.info("Checkpointed.");
            dao.executeUpdate("SHUTDOWN");
        }  
        catch (Throwable t) {
            t.printStackTrace();
            log.info("checkpoint database in StartupServlet.destroy", t);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }

    private static void updateSchema() 
    throws Exception 
    {
        log.debug("Updating schema...");
        if (!checkSchema()) {
            createSchema();
            if (!checkSchema()) {
                log.error("schema didn't have correct version after creating");
                //throw new IOException("Unable to successfully update database tables.");
            }
        }
        log.debug("Updating schema...Done!");
    }

    /**
     * 
     * @param resourceDir
     * @param props
     * @return
     */
    private static boolean checkSchema() throws Exception {
        boolean upToDate = false;
        String dbSchemaVersion = "";

        BaseDAO dao = new BaseDAO();
        String requiredSchemaVersion = System.getProperty("GenePatternVersion");
        // check schemaVersion
        try {
            ResultSet resultSet = dao.executeSQL("select value from props where key='schemaVersion'", false);
            if (resultSet.next()) {
                dbSchemaVersion = resultSet.getString(1);
                upToDate = (requiredSchemaVersion.compareTo(dbSchemaVersion) <= 0);
            }
            else {
                dbSchemaVersion = "";
                upToDate = false;
            }
        }
        catch (Exception e) {
            log.info("Database tables not found.  Create new database");
            dbSchemaVersion = "";
        }

        System.setProperty("dbSchemaVersion", dbSchemaVersion);
        log.info("schema up-to-date: " + upToDate + ": " + requiredSchemaVersion + " required, " + dbSchemaVersion + " current");
        return upToDate;
    }

    /**
     * @return the resources directory, or null if there are configuration errors
     */
    private static File getResourceDir() {
        String resourceDirProp = System.getProperty("resources");
        if (resourceDirProp == null || resourceDirProp.trim().length() == 0) {
            log.error("Missing required System property, 'resources': "+resourceDirProp);
            //TODO: throw exception
            return null;
        }
        File resourceDir = new File(resourceDirProp);
        if (!resourceDir.canRead()) {
            log.error("Configuration error: Can't read resources directory: "+resourceDir.getPath());
            //TODO: throw exception
            return null;
        }
        return resourceDir;
    }
    
    /**
     * 
     * @param resourceDir
     * @param props
     * @throws IOException
     */
    private static void createSchema() {
        File resourceDir = getResourceDir();
        final String schemaPrefix = System.getProperty("HSQL.schema", "analysis_hypersonic-");
        FilenameFilter schemaFilenameFilter = new FilenameFilter() {
            // INNER CLASS !!!
            public boolean accept(File dir, String name) {
                return name.endsWith(".sql") && name.startsWith(schemaPrefix);
            }
        };
        File[] schemaFiles = resourceDir.listFiles(schemaFilenameFilter);
        Arrays.sort(schemaFiles, new Comparator<File>() {
            public int compare(File f1, File f2) {
                String name1 = f1.getName();
                String version1 = name1.substring(schemaPrefix.length(), name1.length() - ".sql".length());
                String name2 = f2.getName();
                String version2 = name2.substring(schemaPrefix.length(), name2.length() - ".sql".length());
                return version1.compareToIgnoreCase(version2);
            }
        });
        String expectedSchemaVersion = System.getProperty("GenePatternVersion");
        String dbSchemaVersion = (String) System.getProperty("dbSchemaVersion");
        for (int f = 0; f < schemaFiles.length; f++) {
            File schemaFile = schemaFiles[f];
            String name = schemaFile.getName();
            String version = name.substring(schemaPrefix.length(), name.length() - ".sql".length());
            if (version.compareTo(expectedSchemaVersion) <= 0 && version.compareTo(dbSchemaVersion) > 0) {
                log.info("processing" + name + " (" + version + ")");
                processSchemaFile(schemaFile);
            }
            else {
                log.info("skipping " + name + " (" + version + ")");
            }
        }
        log.debug("createSchema ... Done!");
    }

    private static void processSchemaFile(File schemaFile) {
        log.info("updating database from schema " + schemaFile.getPath());
        String all = null;
        try {
            all = readFile(schemaFile);
        }
        catch (IOException e) {
            log.error("database not updated from schema, error reading schema " + schemaFile.getPath(), e);
            return;
        }
        while (!all.equals("")) {
            all = all.trim();
            int i = all.indexOf('\n');
            if (i == -1) i = all.length() - 1;
            if (all.startsWith("--")) {
                all = all.substring(i + 1);
                continue;
            }

            i = all.indexOf(';');
            String sql;
            if (i != -1) {
                sql = all.substring(0, i);
                all = all.substring(i + 1);
            }
            else {
                sql = all;
                all = "";
            }
            sql = sql.trim();
            log.info("apply SQL-> " + sql);
            BaseDAO dao = new BaseDAO();
            dao.executeUpdate(sql);
        }
        log.debug("updating database from schema ... Done!");
    }

    /**
     * @param file
     * @return the contents of the file as a String
     * @throws IOException from reader.read
     */
    private static String readFile(File file) throws IOException 
    {
        FileReader reader = null;
        StringBuffer b = new StringBuffer();
        try {
            reader = new FileReader(file);
            char buffer[] = new char[1024];
            while (true) {
                int i = reader.read(buffer, 0, buffer.length);
                if (i == -1) {
                    break;
                }
                b.append(buffer, 0, i);
            }
            return b.toString();
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException ioe) {
                }
            }
        }
    }

    public static void main(String args[]) {
        try {
            startDatabase();
            System.in.read();
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
