package org.genepattern.junitutil;

import java.util.Arrays;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.util.GPConstants;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * Basic implementation of group membership and authorization for use by junit tests.
 * May eventually serve as a replacement for most of the code in the 
 *     org.genepattern.server.webapp.jsf.AuthorizationHelper.class
 * 
 * @author pcarr
 */
public final class GroupAuth implements IGroupMembershipPlugin, IAuthorizationManager {
    private static final Logger log = Logger.getLogger(GroupAuth.class);

    /** All users are members of the wildcard group; default='*'. */
    public static final String ALL_USERS="*";

    /** the default name of the administrators group */
    public static final String ADMIN_GROUP_ID="administrators";

    /**
     * multimap of userId->Set of groupId, maps each userId to a set of zero or more groups.
     */
    private final ImmutableSetMultimap<String,String> usersToGroups;
    
    /**
     * multimap of permission->Set of groupId,
     * a proxy for what is loaded from the 'permissionsMap.xml' file 
     */
    private final ImmutableSetMultimap<Permission,String> permissionMap;

    /**
     * multimap of links (aka actions) to permissions,
     * a proxy for what is loaded from the 'actionPermissionMap.xml' file.
     */
    private final ImmutableSetMultimap<String,Permission> actionPermissionMap;
    
    /**
     * the set of groups that all users are a member of, by default ['*'];
     * this also includes any groups with wildcard '*' members.
     * Derived from the usersToGroups multimap. 
     * This is an optimization for the getGroups call.
     */
    private final ImmutableSet<String> commonGroups; 

    private GroupAuth(final Builder in) {
        this.usersToGroups=ImmutableSetMultimap.copyOf(in.usersToGroups);
        this.permissionMap=ImmutableSetMultimap.copyOf(in.permissionMap);
        this.actionPermissionMap=ImmutableSetMultimap.copyOf(in.actionPermissionMap);
        this.commonGroups = new ImmutableSet.Builder<String>()
            .add(ALL_USERS)
            .addAll(usersToGroups.get(ALL_USERS))
        .build(); 
    }

    @Override
    public Set<String> getGroups(final String userId) { 
        return Sets.union(commonGroups, usersToGroups.get(userId));
    }

    @Override
    public boolean isMember(final String userId, final String groupId) {
        if (ALL_USERS.equals(groupId)) {
            return true;
        }
        return usersToGroups.get(userId).contains(groupId);
    }
    
    @Override
    public boolean isAllowed(final String urlOrSoapMethod, String userID) {
        if (!actionPermissionMap.containsKey(urlOrSoapMethod)) {
            // anything not listed is allowed
            return true;
        }
        for(final Permission permission : actionPermissionMap.get(urlOrSoapMethod)) {
            final boolean allowed = checkPermission(permission, userID);
            if (allowed) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean checkPermission(final String permissionName, String userID) {
        if (Strings.isNullOrEmpty(permissionName)) {
            return false;
        }
        try {
            final Permission p=Permission.fromPermissionName(permissionName);
            return checkPermission(p, userID);
        }
        catch (Throwable t) {
            if (log.isEnabledFor(Level.ERROR)) {
                ///CLOVER:OFF
                log.error("Error getting permission from string="+permissionName, t);
                ///CLOVER:ON
            }
            return false;
        }
    }
    
    protected boolean checkPermission(final Permission permission, final String userID) {
        // check for all ('*')
        if (permissionMap.get(permission).contains(ALL_USERS)) {
            return true;
        }
        
        // null-check
        if (Strings.isNullOrEmpty(userID)) {
            // equivalent to anonymous user, gets same permissions as ALL
            return false;
        }
        
        for(final String groupId : getGroups(userID)) {
            if (permissionMap.get(permission).contains(groupId)) {
                return true;
            } 
        }
        return false;
    }
    
    public boolean adminJobs(final String userId) {
        return checkPermission(Permission.ADMIN_JOBS, userId);
    }

    public boolean adminModules(final String userId) {
        return checkPermission(Permission.ADMIN_MODULES, userId);
    }

    public boolean adminPipelines(final String userId) {
        return checkPermission(Permission.ADMIN_PIPELINES, userId);
    }

    public boolean adminServer(final String userId) {
        return checkPermission(Permission.ADMIN_SERVER, userId);
    }

    public boolean adminSuites(final String userId) {
        return checkPermission(Permission.ADMIN_SUITES, userId);
    }
    
    public int checkPipelineAccessId(final String userId, final int accessId) {
        if (accessId == GPConstants.ACCESS_PUBLIC) {
            if (!checkPermission(Permission.CREATE_PUBLIC_PIPELINE, userId)) {
                return GPConstants.ACCESS_PRIVATE;
            }
        }
        return accessId;
    }

    public int checkSuiteAccessId(final String userId, int accessId) {
        if (accessId == GPConstants.ACCESS_PUBLIC) {
            if (!checkPermission(Permission.CREATE_PUBLIC_SUITE, userId)) {
                return GPConstants.ACCESS_PRIVATE;
            }
        }
        return accessId;
    }

    public boolean createModule(final String userId) {
        return checkPermission(Permission.CREATE_MODULE, userId);
    }

    public boolean createPipeline(final String userId) {
        return checkPermission(Permission.CREATE_PUBLIC_PIPELINE, userId)
                || checkPermission(Permission.CREATE_PRIVATE_PIPELINE, userId);
    }

    public boolean createPrivatePipeline(final String userId) {
        return checkPermission(Permission.CREATE_PRIVATE_PIPELINE, userId);
    }

    public boolean createPrivateSuite(final String userId) {
        return checkPermission(Permission.CREATE_PRIVATE_SUITE, userId);
    }

    public boolean createPublicPipeline(final String userId) {
        return checkPermission(Permission.CREATE_PUBLIC_PIPELINE, userId);
    }

    public boolean createPublicSuite(final String userId) {
        return checkPermission(Permission.CREATE_PUBLIC_SUITE, userId);
    }

    public boolean createSuite(final String userId) {
        return checkPermission(Permission.CREATE_PUBLIC_SUITE, userId)
                || checkPermission(Permission.CREATE_PRIVATE_SUITE, userId);
    }

    /**
     * fluent builder for the GroupAuth class.
     * @author pcarr
     *
     */
    public static final class Builder {
        private final SetMultimap<String,String> usersToGroups=HashMultimap.create();
        private final SetMultimap<Permission,String> permissionMap=HashMultimap.create();
        private final SetMultimap<String,Permission> actionPermissionMap=HashMultimap.create();

        /**
         * create a new builder, initialized with the default actionPermissions.
         */
        public Builder() {
            withDefaultActionPermissions();
        }
        
        /**
         * initialize permission for a default (laptop) install of GP; everyone is an admin.
         * <pre>
           <group name="administrators">
               <user name="*" />
           </group>
         * </pre>
         */
        public Builder withDefaults() {
            // userGroups.xml
            group(ADMIN_GROUP_ID, ALL_USERS);
            // permissionMap.xml
            withDefaultPermissions();
            // actionPermissionMap.xml
            withDefaultActionPermissions();
            return this;
        }

        /**
         * initialize permissions for a default server install; the 'admin' user is 
         * in the administrators group.
         * @return
         */
        public Builder withAdminGroup() {
            return 
                withDefaultPermissions()
                .group(GroupAuth.ADMIN_GROUP_ID, "admin");
        }
        
        public Builder withDefaultPermissions() {
            // permissionMap.xml
            permission(Permission.ADMIN_JOBS, ADMIN_GROUP_ID);
            permission(Permission.ADMIN_MODULES, ADMIN_GROUP_ID);
            permission(Permission.ADMIN_SUITES, ADMIN_GROUP_ID);
            permission(Permission.ADMIN_PIPELINES, ADMIN_GROUP_ID);
            permission(Permission.ADMIN_SERVER, ADMIN_GROUP_ID);
            permission(Permission.CREATE_MODULE, ADMIN_GROUP_ID);
            permission(Permission.CREATE_PRIVATE_PIPELINE, ALL_USERS);
            permission(Permission.CREATE_PRIVATE_SUITE, ALL_USERS);
            permission(Permission.CREATE_PUBLIC_PIPELINE, ALL_USERS);
            permission(Permission.CREATE_PUBLIC_SUITE, ALL_USERS);
            return this;
        }
        
        public Builder withDefaultActionPermissions() {
            // actionPermissionMap.xml
            actionPermission("sql.jsp", Permission.ADMIN_SERVER);
            actionPermission("createReport.jsp", Permission.ADMIN_SERVER);
            actionPermission("requestReport.jsp", Permission.ADMIN_SERVER);
            actionPermission("installFrame.jsp", Permission.ADMIN_SERVER);
            actionPermission("installLog.jsp", Permission.ADMIN_SERVER);
            return this;
        }

        /**
         * Set group members, the equivalent of parsing an entry from the userGroups.xml file, e.g.
         * <pre>
               <group name="administrators">
                 <user name="admin"/>
                 <user name="gp_admin"/>
               </group>
         * </pre>
         * 
         * @param groupId, the name of the group
         * @param users, a list of one or more userId to be added to the group
         * @return
         */
        public Builder group(final String groupName, final String... userNames) {
            // first, clear the group
            usersToGroups.removeAll(groupName);
            for(final String user : userNames) {
                usersToGroups.put(user, groupName);
            }
            return this;
        }
        
        /**
         * Set group permissions, the equivalent of parsing an entry from the permissionMap.xml file, e.g.
         * <pre>
             <permission name="adminJobs">
               <group name="administrators"/>
             </permission>
         * </pre>
         * 
         * @return
         */
        public Builder permission(final Permission permission, final String... groupNames) {
            permissionMap.replaceValues(permission, Arrays.asList(groupNames));
            return this;
        }
        
        /**
         * Append an (action,permission) line from the actionPermissionMap.xml file. E.g.
         * <pre>
           <url link="sql.jsp" permission="adminServer"/>
         * </pre>
         * 
         * @param link
         * @param permission
         * @return
         */
        public Builder actionPermission(final String link, final Permission permission) {
            actionPermissionMap.put(link, permission);
            return this;
        }

        public GroupAuth build() {
            return new GroupAuth(this);
        }
    } 
}