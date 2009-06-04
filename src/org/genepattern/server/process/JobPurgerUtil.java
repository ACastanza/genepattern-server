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

package org.genepattern.server.process;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;

public class JobPurgerUtil {
    private static Logger log = Logger.getLogger(JobPurgerUtil.class);


   /**
    * Helper method which gives the next time that the purger will run based on the current time.
    * 
    * @param now, the current time from which to compute the next run.
    * @param purgeTime, system setting giving the time of day (hour and minute) to run the purger.
    * 
    * @return
    */
    public static Date getNextPurgeTime(Date now, String purgeTime) {
        GregorianCalendar nextPurgeTime = new GregorianCalendar();
        nextPurgeTime.setTime(now);
        GregorianCalendar purgeTOD = new GregorianCalendar();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        try {
            purgeTOD.setTime(dateFormat.parse(purgeTime));
        } 
        catch (ParseException pe) {
            purgeTOD.set(GregorianCalendar.HOUR_OF_DAY, JobPurger.DEFAULT_PURGE_HOUR);
            purgeTOD.set(GregorianCalendar.MINUTE, JobPurger.DEFAULT_PURGE_MINUTE);
        }
        nextPurgeTime.set(GregorianCalendar.HOUR_OF_DAY, purgeTOD.get(GregorianCalendar.HOUR_OF_DAY));
        nextPurgeTime.set(GregorianCalendar.MINUTE, purgeTOD.get(GregorianCalendar.MINUTE));
        nextPurgeTime.set(GregorianCalendar.SECOND, 0);
        nextPurgeTime.set(GregorianCalendar.MILLISECOND, 0);
        
        if (!nextPurgeTime.getTime().after(now)) {
            // it's already on or after today's purge time, wait until tomorrow's
            nextPurgeTime.add(GregorianCalendar.DATE, 1);
        }

        log.debug("next purge will be at " + nextPurgeTime.getTime());
        return nextPurgeTime.getTime();
    }
   
    /**
     * Helper method which gives the date at which a given job will be purged,  
     * based on the job completion date.
     * 
     * @param jobCompletionDate
     * @return the purge date, or null if the date is unknown or if the purger is not configured to purge jobs.
     */
    public static Date getJobPurgeDate(Date jobCompletionDate) {
        if (jobCompletionDate == null) {
            return null;
        }
        
        int purgeInterval = lookupPurgeInterval();
        if (purgeInterval < 0) {
            return null;
        }
        String purgeTime = System.getProperty("purgeTime", "23:00");
        GregorianCalendar purgeTOD = new GregorianCalendar();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        try {
            purgeTOD.setTime(dateFormat.parse(purgeTime));
        } 
        catch (ParseException pe) {
            purgeTOD.set(GregorianCalendar.HOUR_OF_DAY, JobPurger.DEFAULT_PURGE_HOUR);
            purgeTOD.set(GregorianCalendar.MINUTE, JobPurger.DEFAULT_PURGE_MINUTE);
        }
        
        Calendar jobPurgeCal = new GregorianCalendar();
        jobPurgeCal.setTime(jobCompletionDate);
        jobPurgeCal.add(Calendar.DATE, purgeInterval);
        // if the purgeTime is less than the job completion time, add another day
        Date jobPurgeDateInit = jobPurgeCal.getTime();
        jobPurgeCal.set(GregorianCalendar.HOUR_OF_DAY, purgeTOD.get(GregorianCalendar.HOUR_OF_DAY));
        jobPurgeCal.set(GregorianCalendar.MINUTE, purgeTOD.get(GregorianCalendar.MINUTE));
        jobPurgeCal.set(GregorianCalendar.SECOND, 0);
        jobPurgeCal.set(GregorianCalendar.MILLISECOND, 0);
        Date jobPurgeDateAdjusted = jobPurgeCal.getTime();
        if (jobPurgeDateAdjusted.before(jobPurgeDateInit)) {
            jobPurgeCal.add(Calendar.DATE, 1);
        }
        
        Date purgeDate = jobPurgeCal.getTime();
        
        //if the purgeDate is in the past, return the next time the purger will run
        Date now = new Date();
        if (!purgeDate.after(now)) {
            return getNextPurgeTime(now, purgeTime);
        }
        return jobPurgeCal.getTime();
    }


    private static int lookupPurgeInterval() {
        String purgeJobsAfter = System.getProperty("purgeJobsAfter", "-1");
        return lookupPurgeInterval(purgeJobsAfter);
    }
    
    private static int lookupPurgeInterval(String purgeJobsAfter) {
        try {
            return Integer.parseInt(purgeJobsAfter);
        } 
        catch (NumberFormatException nfe) {
            return JobPurger.DEFAULT_PURGE_JOBS_INTERVAL;
        }
    }

}
