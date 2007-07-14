/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.process;

import java.io.File;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.indexer.Indexer;
import org.genepattern.webservice.JobInfo;

/**
 * Periodically purge jobs that completed some number of days ago.
 * 
 * @author Jim Lerner
 */
public class Purger extends TimerTask {

    private static Logger log = Logger.getLogger(Purger.class);

    /** number of days back to preserve completed jobs */
    int purgeInterval = -1;

    public Purger(int purgeInterval) {
        this.purgeInterval = purgeInterval;
    }

    public void run() {
        if (purgeInterval != -1) {
            try {
                HibernateUtil.beginTransaction();

                // find all purgeable jobs
                GregorianCalendar gcPurgeDate = new GregorianCalendar();
                gcPurgeDate.add(GregorianCalendar.DATE, -purgeInterval);
                log.info("Purger: purging jobs completed before " + gcPurgeDate.getTime());

                AnalysisDAO ds = new AnalysisDAO();
                JobInfo[] purgeableJobs = ds.getJobInfo(gcPurgeDate.getTime());

                // purge expired jobs
 
                for (int jobNum = 0; jobNum < purgeableJobs.length; jobNum++) {
                    try {

                        int jobID = purgeableJobs[jobNum].getJobNumber();
                        log.info("Purger: deleting jobID " + jobID);

                        // delete search indexes for job
                        try {
                            Indexer.deleteJob(jobID);
                        }
                        catch (IOException ioe) {
                            System.err.println(ioe + " while deleting search index while deleting job " + jobID);
                        }

                        // enumerate output files for this job and delete them
                        File jobDir = new File(GenePatternAnalysisTask.getJobDir(Integer.toString(jobID)));
                        File[] files = jobDir.listFiles();
                        if (files != null) {
                            for (int i = 0; i < files.length; i++) {
                                files[i].delete();
                            }
                        }

                        // delete the job directory
                        jobDir.delete();

                        // TODO: figure out which input files to purge. This is
                        // hard, because an input file could be shared among
                        // numerous jobs, some of which are not old enough to
                        // purge yet

                        ds.deleteJob(jobID);
                    }
                    catch (Exception e) {
                        log.error(" while purging jobs", e);
                    }
                }

                HibernateUtil.commitTransaction();

                try {
                    Indexer.optimize(Indexer.getIndexDir());
                }
                catch (IOException ioe) {
                    log.error(" while optimizing search index", ioe);
                }

                long dateCutoff = gcPurgeDate.getTime().getTime();
                purge(System.getProperty("jobs"), dateCutoff);
                purge(System.getProperty("java.io.tmpdir"), dateCutoff);

            }
            catch (Exception e) {
                HibernateUtil.rollbackTransaction();
                log.error("Error while purging jobs", e);
            }
            finally {
                HibernateUtil.closeCurrentSession();
            }
            log.info("Purger: done");
        }
    }

    protected void purge(String dirName, long dateCutoff) throws IOException {
        File[] moreFiles = new File(dirName).listFiles();
        // log.info("cutoff: " + new Date(dateCutoff).toString());
        if (moreFiles != null) {
            for (int i = 0; i < moreFiles.length; i++) {
                // log.info(moreFiles[i].getName() + ": " + new
                // Date(moreFiles[i].lastModified()).toString());
                if (moreFiles[i].getName().startsWith("Lucene") && moreFiles[i].getName().endsWith(".lock")) continue;
                if (/* moreFiles[i].getName().startsWith("pipe") && */
                moreFiles[i].lastModified() < dateCutoff) {
                    try {
                        if (moreFiles[i].isDirectory()) {
                            // log.info("Purger: deleting pipeline " +
                            // moreFiles[i]);
                            File[] files = moreFiles[i].listFiles();
                            if (files != null) {
                                for (int j = 0; j < files.length; j++) {
                                    try {
                                        files[j].delete();
                                    }
                                    catch (SecurityException se) {
                                        log.error("unable to delete " + files[j].getPath());
                                    }
                                }
                            }
                        }
                        try {
                            moreFiles[i].delete();
                        }
                        catch (SecurityException se) {
                            log.error("unable to delete " + moreFiles[i].getPath(), se);
                        }
                    }
                    catch (SecurityException se) {
                        log.error("unable to browse " + moreFiles[i].getPath(), se);
                    }
                }
            }
        }
    }

    public static void main(String args[]) {
        Purger purger = new Purger(7);
        purger.run();
    }
}