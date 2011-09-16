package org.genepattern.server.user;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.genepattern.server.auth.GroupPermission;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.util.AuthorizationManagerFactory;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.server.webapp.jsf.UsersAndGroupsBean;

/**
 * Map each user to its groups.
 */
public class UserEntry {
    private static Logger log = Logger.getLogger(UserEntry.class);

    private User user;
    private SortedSet<String> groups;
    private boolean isAdmin = false;
    private IAuthorizationManager authManager;
    
    public UserEntry(User user) {
        authManager = AuthorizationManagerFactory.getAuthorizationManager();
        
        this.user = user;
        this.groups = new TreeSet<String>();
        this.isAdmin = authManager.checkPermission("adminServer", user.getUserId());
    }
    
    public void addGroup(String groupId) {
        if (UsersAndGroupsBean.ignorePublicGroups && GroupPermission.PUBLIC.equalsIgnoreCase(groupId)) {
            return;
        }
        groups.add(groupId);
    }
    
    public User getUser() {
        return user;
    }
    
    public List<String> getGroups() {
        return new ArrayList<String>(groups);
    }
    
    public boolean isAdmin() {
        return isAdmin;
    }
    
    public String getUserDir() {
        Context context = ServerConfiguration.Context.getContextForUser(user.getUserId());
        try {
            File userDir = ServerConfiguration.instance().getUserDir(context);
            return userDir.getPath();
        }
        catch (Throwable t) {
            log.error(t);
            return t.getLocalizedMessage();
        }
    }
}
 
