package org.genepattern.server.webapp.rest.api.v1;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.genepattern.junitutil.GroupAuth;
import org.genepattern.junitutil.Permission;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * jUnit tests for a utility class.
 * @author pcarr
 *
 */
public class TestGroupAuth {
    final static String adminUser="admin";
    final static String testUser="test";
    final static String devUser="dev";

    private GroupAuth groupAuth;
    
    @Test
    public void userGroups() {
        groupAuth=new GroupAuth.Builder()
            .group("administrators", adminUser, "admin@myemail.com")
            .group("developers", devUser, "dev_user_02")
        .build();
        
        assertEquals("getGroups(dev_user)", ImmutableSet.<String> of("*", "developers"), 
                groupAuth.getGroups(devUser));
        assertEquals("isMember, wildcard group", true, 
                groupAuth.isMember(devUser, "*"));
        assertEquals("isMember, developers group", true, 
                groupAuth.isMember(devUser, "developers"));
        assertEquals("isMember, administrators group", false, 
                groupAuth.isMember(devUser, "administrators"));
        assertEquals("isMember, null group", false, 
                groupAuth.isMember(devUser, null));
        assertEquals("isMember, empty group", false, 
                groupAuth.isMember(devUser, ""));
    }
    
    
    /** for a default install, all users have admin privileges */
    @Test
    public void groupAuth_withDefaults() {
        final Set<String> expectedGroups= ImmutableSet.<String> of("*", "administrators");
        
        groupAuth=new GroupAuth.Builder()
            .withDefaults()
        .build();
        
        assertEquals("getGroups(testUser)", expectedGroups, 
                groupAuth.getGroups(testUser)); 
        assertEquals("isMember(*)", true, 
                groupAuth.isMember(testUser, "*"));
        assertEquals("adminServer(testUser)", true, 
                groupAuth.adminServer(testUser));
    }
    
    /**
     * test GroupAuth.Builder().withAdminGroup, verify all permissions that can be set 
     * in the permissionMap.xml file. 
     */
    @Test
    public void groupAuth_withAdminGroup() {
        groupAuth=new GroupAuth.Builder()
            .withDefaultAdmin()
        .build();
        
        assertEquals("adminServer(testUser)",    false, groupAuth.adminServer(testUser));
        assertEquals("adminJobs(testUser)",      false, groupAuth.adminJobs(testUser));
        assertEquals("adminModules(testUser)",   false, groupAuth.adminModules(testUser));
        assertEquals("adminPipelines(testUser)", false, groupAuth.adminPipelines(testUser));
        assertEquals("adminSuites(testUser)",    false, groupAuth.adminSuites(testUser));
        assertEquals("createModule(testUser)",          false, groupAuth.createModule(testUser));
        assertEquals("createPipeline(testUser)",        true, groupAuth.createPipeline(testUser));
        assertEquals("createPrivatePipeline(testUser)", true, groupAuth.createPrivatePipeline(testUser));
        assertEquals("createPrivateSuite(testUser)",    true, groupAuth.createPrivateSuite(testUser));
        assertEquals("createPublicPipeline(testUser)",  true, groupAuth.createPublicPipeline(testUser));
        assertEquals("createPublicSuite(testUser)",     true, groupAuth.createPublicSuite(testUser));
        assertEquals("createSuite(testUser)",           true, groupAuth.createSuite(testUser));
        
        assertEquals("adminServer(adminUser)",   true, groupAuth.adminServer(adminUser));
        assertEquals("adminJobs(adminUser)",      true, groupAuth.adminJobs(adminUser));
        assertEquals("adminModules(adminUser)",   true, groupAuth.adminModules(adminUser));
        assertEquals("adminPipelines(adminUser)", true, groupAuth.adminPipelines(adminUser));
        assertEquals("adminSuites(adminUser)",    true, groupAuth.adminSuites(adminUser));
        assertEquals("createModule(adminUser)",          true, groupAuth.createModule(adminUser));
        assertEquals("createPipeline(adminUser)",        true, groupAuth.createPipeline(adminUser));
        assertEquals("createPrivatePipeline(adminUser)", true, groupAuth.createPrivatePipeline(adminUser));
        assertEquals("createPrivateSuite(adminUser)",    true, groupAuth.createPrivateSuite(adminUser));
        assertEquals("createPublicPipeline(adminUser)",  true, groupAuth.createPublicPipeline(adminUser));
        assertEquals("createPublicSuite(adminUser)",     true, groupAuth.createPublicSuite(adminUser));
        assertEquals("createSuite(adminUser)",           true, groupAuth.createSuite(adminUser));        
    }
    
    @Test
    public void fromPermissionName() {
        assertEquals(Permission.CREATE_MODULE, Permission.fromPermissionName("createModule"));
    }

    @Test
    public void fromPermissionName_all_lower() {
        assertEquals(Permission.CREATE_PRIVATE_PIPELINE, Permission.fromPermissionName("createprivatepipeline"));
    }
    @Test
    public void fromPermissionName_ALL_CAPS() {
        assertEquals(Permission.ADMIN_SERVER, Permission.fromPermissionName("ADMINSERVER"));
    }

    @Test
    public void fromPermissionName_ENUM_name() {
        assertEquals(Permission.ADMIN_JOBS, Permission.fromPermissionName("ADMIN_JOBS"));
    }
    @Test(expected=IllegalArgumentException.class)
    public void fromPermissionName_nullArg() {
        Permission.fromPermissionName(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void fromPermissionName_emptyArg() {
        Permission.fromPermissionName("");
    }

    @Test(expected=IllegalArgumentException.class)
    public void fromPermissionName_invalidName() {
        Permission.fromPermissionName("bogus_permission_name");
    }

    @Test
    public void checkPermission_adminServer() {
        groupAuth=new GroupAuth.Builder()
            .withDefaultAdmin()
        .build();

        final String ADMIN_SERVER="adminServer";
        assertEquals("checkPermission(adminServer, adminUser)", true, groupAuth.checkPermission(ADMIN_SERVER, adminUser));
        assertEquals("checkPermission(adminServer, testUser)", false, groupAuth.checkPermission(ADMIN_SERVER, testUser));
    }
    
    @Test
    public void checkPermission_cornerCases() {
        groupAuth=new GroupAuth.Builder()
            .withDefaultAdmin()
        .build();

        assertEquals("null permissionName", false,
            groupAuth.checkPermission(null, testUser));
        
        assertEquals("empty permissionName", false, 
                groupAuth.checkPermission("", testUser));
        
        final Level before=Logger.getLogger(GroupAuth.class).getLevel();
        try {
            Logger.getLogger(GroupAuth.class).setLevel(Level.OFF);
            assertEquals("bogus permissionName", false, 
                    groupAuth.checkPermission("bogus_permission", testUser));
        }
        finally {
            Logger.getLogger(GroupAuth.class).setLevel(before);
        }
        
        assertEquals("null userName", false,
                groupAuth.checkPermission("adminJobs", null));

        assertEquals("empty userName", false,
                groupAuth.checkPermission("adminJobs", ""));

    }
    
    
    @Test
    public void actionPermissionMap_withAdminGroup() {
        groupAuth=new GroupAuth.Builder()
            .withDefaultAdmin()
        .build();

        assertEquals("isAllowed(sql.jsp, adminUser)", true, 
                groupAuth.isAllowed("sql.jsp", adminUser));
        assertEquals("isAllowed(createReport.jsp, adminUser)", true, 
                groupAuth.isAllowed("createReport.jsp", adminUser));
        assertEquals("isAllowed(requestReport.jsp, adminUser)", true, 
                groupAuth.isAllowed("requestReport.jsp", adminUser));
        assertEquals("isAllowed(installFrame.jsp, adminUser)", true, 
                groupAuth.isAllowed("installFrame.jsp", adminUser));
        assertEquals("isAllowed(installLog.jsp, adminUser)", true, 
                groupAuth.isAllowed("installLog.jsp", adminUser));
        assertEquals("isAllowed(anything else, adminUser)", true, 
                groupAuth.isAllowed("anything else", adminUser));

        assertEquals("isAllowed(sql.jsp, testUser)", false, 
                groupAuth.isAllowed("sql.jsp", testUser));
        assertEquals("isAllowed(createReport.jsp, testUser)", false, 
                groupAuth.isAllowed("createReport.jsp", testUser));
        assertEquals("isAllowed(requestReport.jsp, testUser)", false, 
                groupAuth.isAllowed("requestReport.jsp", testUser));
        assertEquals("isAllowed(installFrame.jsp, testUser)", false, 
                groupAuth.isAllowed("installFrame.jsp", testUser));
        assertEquals("isAllowed(installLog.jsp, testUser)", false, 
                groupAuth.isAllowed("installLog.jsp", testUser));
        assertEquals("isAllowed(anything else, testUser)", true, 
                groupAuth.isAllowed("anything else", testUser));
    }

}
