/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;

/**
 * Factory method(s) for initializing JobPermissions flags for a given user for a given job.
 */
public class JobPermissionsFactory {

    /** @deprecated should pass in a DB session and GroupMembership */
    public static final JobPermissions createJobPermissionsFromDb(final boolean isAdmin, final String userId, final int jobNumber) {
        return createJobPermissionsFromDb(
                HibernateUtil.instance(),
                UserAccountManager.instance().getGroupMembership(),
                isAdmin, userId, jobNumber);
    }

    public static final JobPermissions createJobPermissionsFromDb(final HibernateSessionManager mgr, final IGroupMembershipPlugin groupInfo, final boolean isAdmin, final String userId, final int jobNumber) {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            PermissionsHelper perm = new PermissionsHelper(mgr, groupInfo, isAdmin, userId, jobNumber);
            return toJobPermissions(perm);
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
