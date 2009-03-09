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

package org.genepattern.server.webservice.server.dao;

import java.io.*;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.Suite;
import org.genepattern.server.domain.TaskMaster;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.webservice.*;
import org.hibernate.*;

import com.sun.rowset.CachedRowSetImpl;

public class BaseDAO {

    private static Logger log = Logger.getLogger(TaskIntegratorDAO.class);
    public static final int UNPROCESSABLE_TASKID = -1;
    public int PROCESSING_STATUS = 2;
    public static int JOB_WAITING_STATUS = 1;


    public BaseDAO() {
    	// Start a transaction if not begun already
    	HibernateUtil.beginTransaction();
    }
    
    protected Session getSession() {
        return HibernateUtil.getSession();
    }

    protected java.sql.Date now() {
        return new java.sql.Date(Calendar.getInstance().getTimeInMillis());
    }

    protected void cleanupJDBC(ResultSet rs, Statement st) {
        if (rs != null) {
            try {
                rs.close();
            }
            catch (SQLException x) {
            }
        }
        if (st != null) {
            try {
                st.close();
            }
            catch (SQLException x) {
            }
        }

    }

    protected void cleanupJDBC(ResultSet rs, Statement st, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            }
            catch (SQLException x) {
                log.error(x);
            }
        }
        if (st != null) {
            try {
                st.close();
            }
            catch (SQLException x) {
                log.error(x);
            }
        }
        if (conn != null)
            try {
                conn.close();
            }
            catch (SQLException x) {
                log.error(x);
            }

    }

  
    protected static TaskInfo taskInfoFromTaskMaster(TaskMaster tm) {
        return new TaskInfo(tm.getTaskId(), tm.getTaskName(), tm.getDescription(), tm.getParameterInfo(),
                TaskInfoAttributes.decode(tm.getTaskinfoattributes()), tm.getUserId(), tm.getAccessId());

    }

    protected SuiteInfo suiteInfoFromSuite(Suite suite) throws OmnigeneException {

        String lsid = suite.getLsid();
        int access_id = suite.getAccessId();
        String name = suite.getName();
        String description = suite.getDescription();
        String owner = suite.getUserId();
        String author = suite.getAuthor();

        ArrayList docs = new ArrayList();
        try {
            String suiteDirStr = DirectoryManager.getSuiteLibDir(name, lsid, owner);
            File suiteDir = new File(suiteDirStr);

            if (suiteDir.exists()) {
                File docFiles[] = suiteDir.listFiles();
                for (int i = 0; i < docFiles.length; i++) {
                    File f = docFiles[i];
                    docs.add(f.getName());
                }
            }
        }
        catch (Exception e) {
            // swallow & no docs
            log.error(e);
        }

        List mods = suite.getModules();

        SuiteInfo suiteInfo = new SuiteInfo(lsid, name, description, author, owner, mods, access_id, docs);
        suiteInfo.setContact(suite.getContact());

        return suiteInfo;

    }



    public JobInfo jobInfoFromAnalysisJob(org.genepattern.server.domain.AnalysisJob aJob) throws OmnigeneException {
        ParameterFormatConverter parameterFormatConverter = new ParameterFormatConverter();
    
        return new JobInfo(aJob.getJobNo().intValue(), aJob.getTaskId(), aJob.getJobStatus().getStatusName(), aJob
                .getSubmittedDate(), aJob.getCompletedDate(), parameterFormatConverter.getParameterInfoArray(aJob
                .getParameterInfo()), aJob.getUserId(), aJob.getTaskLsid(), aJob.getTaskName());
    
    }

    /**
     * execute arbitrary SQL on database, returning ResultSet
     * 
     * @param sql
     * @throws OmnigeneException
     * @throws RemoteException
     * @return ResultSet
     */
    public ResultSet executeSQL(String sql) throws OmnigeneException {
        return executeSQL(sql, true);
    }

    public ResultSet executeSQL(String sql, boolean logError) throws OmnigeneException {
    
        Statement stat = null;
        ResultSet resultSet = null;
    
        try {
            Connection conn = getSession().connection();
            stat = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            resultSet = stat.executeQuery(sql);
            CachedRowSetImpl crs = new CachedRowSetImpl();
            crs.populate(resultSet);
            return crs;
        }
        catch (Exception e) {
            if(logError) log.error("AnalysisHypersonicDAO: executeSQL for " + sql + " failed ", e);
            throw new OmnigeneException(e.getMessage());
        }
    
        finally {
            cleanupJDBC(resultSet, stat);
        }
    
    }

    /**
     * execute arbitrary SQL on database, returning int
     * 
     * @param sql
     * @throws OmnigeneException
     * @throws RemoteException
     * @return int number of rows returned
     */
    public int executeUpdate(String sql) {
        getSession().flush();
        getSession().clear();
    
        Statement updateStatement = null;
    
        try {
            updateStatement = getSession().connection().createStatement();
            return updateStatement.executeUpdate(sql);
        }
        catch (HibernateException e) {
            log.error(e);
            throw new OmnigeneException(e);
        }
        catch (SQLException e) {
            log.error(e);
            throw new OmnigeneException(e);
        }
        finally {
            cleanupJDBC(null, updateStatement);
        }
    }

}
