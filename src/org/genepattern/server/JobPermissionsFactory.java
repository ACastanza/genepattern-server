/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import org.apache.log4j.Logger;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;

/**
 * Factory method(s) for initializing JobPermissions flags for a given user for a given job.
 */
public class JobPermissionsFactory {
    private static final Logger log = Logger.getLogger(JobPermissionsFactory.class);

    /** @deprecated should pass in a DB session and GroupMembership */
    public static final JobPermissions createJobPermissionsFromDb(final boolean isAdmin, final String userId, final int jobNumber) 
    throws DbException
    {
        return createJobPermissionsFromDb(
                HibernateUtil.instance(),
                UserAccountManager.instance().getGroupMembership(),
                isAdmin, userId, jobNumber);
    }

    public static final JobPermissions createJobPermissionsFromDb(final HibernateSessionManager mgr, final IGroupMembershipPlugin groupInfo, final boolean isAdmin, final String userId, final int jobNumber) 
    throws DbException
    {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            PermissionsHelper perm = new PermissionsHelper(mgr, groupInfo, isAdmin, userId, jobNumber);
            return toJobPermissions(perm);
        }
        catch (Throwable t) {
            final String message="Error getting permission for job="+jobNumber+", userId="+userId;
            log.error(message, t);
            throw new DbException(message+": "+t.getLocalizedMessage()); 
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    /**
     * Convert PermissionsHelper instance into JobPermissions instance.
     * @param ph
     * @return
     */
    public static final JobPermissions toJobPermissions(final PermissionsHelper ph) {
        return new JobPermissions.Builder()
            .canRead(ph.canReadJob())
            .canWrite(ph.canWriteJob())
            .canSetPermissions(ph.canSetJobPermissions())
            .isPublic(ph.isPublic())
            .isShared(ph.isShared())
            .build();
    }
}
