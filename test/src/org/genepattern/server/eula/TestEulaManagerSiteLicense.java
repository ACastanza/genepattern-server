package org.genepattern.server.eula;

import java.util.List;

import org.genepattern.junitutil.ConfigUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.webservice.TaskInfo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for licensed modules, which have server configured alternate license agreements.
 * 
 * Technically, we are testing that the EulaManager is disabled for a module (by name, lsid no version, or full lsid),
 * for a particular site (all users),  a particular group (all users in the group), or a particular user.
 * 
 * @author pcarr
 */
public class TestEulaManagerSiteLicense {
    private TaskInfo taskInfo=null;
    private Context stdContext=null;
    private Context altContext=null;
    
    @Before
    public void setUp() {
        //load custom userGroups file
        ConfigUtil.setUserGroups(this.getClass(), "userGroups.xml");

        //initialize taskInfo
        taskInfo = TaskUtil.getTaskInfoFromZip(this.getClass(), "testLicenseAgreement_v3.zip");         
        //prove that we loaded the expected module
        Assert.assertNotNull("taskInfo==null", taskInfo);
        Assert.assertEquals("taskName", "testLicenseAgreement", taskInfo.getName());
        Assert.assertEquals("taskLsid", "urn:lsid:9090.gpdev.gpint01:genepatternmodules:812:3", taskInfo.getLsid());
        
        stdContext=ServerConfiguration.Context.getContextForUser("gp_user");
        stdContext.setTaskInfo(taskInfo);
        altContext=ServerConfiguration.Context.getContextForUser("gp_test_user");
        altContext.setTaskInfo(taskInfo);
    }
    
    @After
    public void tearDown() {
        UserAccountManager.instance().setUserGroups(null);
        UserAccountManager.instance().refreshUsersAndGroups();

        //revert back to a 'default' config.file
        System.getProperties().remove("config.file");
        ServerConfiguration.instance().reloadConfiguration();
    } 

    /**
     * test-case for a module with a site-license, by module lsid.
     */
    @Test
    public void testGetEulaSiteLicense() {
        //load a 'config.yaml' file from the directory which contains this source file
        ConfigUtil.loadConfigFile(this.getClass(), "config_EulaManager.site-license.yaml");
        
        List<EulaInfo> eulas=EulaManager.instance(altContext).getAllEulaForModule(altContext);
        Assert.assertEquals("eula manager is disabled for this module", 0, eulas.size());
        Assert.assertEquals("eula manager is disabled for this module, should have 0 licenses", 0, EulaManager.instance(altContext).getEulas(taskInfo).size());
    }
    
    @Test
    public void testGetEulaGroupLicense() {
        //load a 'config.yaml' file from the directory which contains this source file
        ConfigUtil.loadConfigFile(this.getClass(), "config_EulaManager.group-license.yaml");
        
        List<EulaInfo> eulas=EulaManager.instance(altContext).getAllEulaForModule(altContext);
        Assert.assertEquals("eula manager is disabled for this module & group", 0, eulas.size());
        Assert.assertEquals("eula manager is disabled for this module & group, should have 0 licenses", 0, EulaManager.instance(altContext).getEulas(taskInfo).size());
    }
    
    @Test
    public void testGetEulaUserLicense() {
        //load a 'config.yaml' file from the directory which contains this source file
        ConfigUtil.loadConfigFile(this.getClass(), "config_EulaManager.user-license.yaml");
        
        List<EulaInfo> eulas=EulaManager.instance(altContext).getAllEulaForModule(altContext);
        Assert.assertEquals("eula manager is disabled for this module & user", 0, eulas.size());
        Assert.assertEquals("eula manager is disabled for this module & user, should have 0 licenses", 0, EulaManager.instance(altContext).getEulas(taskInfo).size());
    }

}
