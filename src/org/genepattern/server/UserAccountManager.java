package org.genepattern.server;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.auth.DefaultGenePatternAuthentication;
import org.genepattern.server.auth.DefaultGroupMembership;
import org.genepattern.server.auth.GroupMembershipWrapper;
import org.genepattern.server.auth.IAuthenticationPlugin;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.auth.NoAuthentication;
import org.genepattern.server.auth.XmlGroupMembership;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

/**
 * Common interface for managing user accounts and groups, used in the web application and soap server.
 * 
 * @author pcarr
 */
public class UserAccountManager {
    private static Logger log = Logger.getLogger(UserAccountManager.class);
    
    public static final String PROP_AUTHENTICATION_CLASS = "authentication.class";
    public static final String PROP_GROUP_MEMBERSHIP_CLASS = "group.membership.class";

    //force use of factory methods
    private UserAccountManager() {
    }
    
    private static UserAccountManager userAccountManager = null;
    public static UserAccountManager instance() {
        if (userAccountManager == null) {
            userAccountManager = new UserAccountManager();

            String prop = System.getProperty("require.password", "false").toLowerCase();
            userAccountManager.passwordRequired = 
                prop.equals("true") || prop.equals("y") || prop.equals("yes");

            prop = System.getProperty("create.account.allowed", "true").toLowerCase();
            userAccountManager.createAccountAllowed = 
                prop.equals("true") ||  prop.equals("y") || prop.equals("yes");
            
            prop = System.getProperty("show.registration.link", "true").toLowerCase();
            userAccountManager.showRegistrationLink = 
                prop.equals("true") ||  prop.equals("y") || prop.equals("yes");

            userAccountManager.refreshUsersAndGroups();
        }
        return userAccountManager;
    }
    
    private boolean passwordRequired = true;
    private boolean createAccountAllowed = true;
    private boolean showRegistrationLink = true;
    private IAuthenticationPlugin authentication = null;
    private IGroupMembershipPlugin groupMembership = null;

    /**
     * Flag indicating whether or not users can register new accounts via the web interface.
     * @return
     */
    public boolean isCreateAccountAllowed() {
        return createAccountAllowed;
    }
    
    public boolean isShowRegistrationLink() {
        return showRegistrationLink;
    }

    /**
     * Flag indicating whether or not passwords are required for the default genepattern authentication scheme.
     * @return
     */
    public boolean isPasswordRequired() {
        return passwordRequired;
    }

    /**
     * Validate the username before creating a new account.
     * Prohibit creating new user accounts whose names differ only by case.
     * 
     * @param username
     * @throws AuthenticationException
     */
    public void validateNewUsername(String username) throws AuthenticationException {
        //1) is it a valid username
        validateUsername(username);
        //2) is it a unique username
        User user = (new UserDAO()).findByIdIgnoreCase(username);
        if (user != null) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "User already registered: "+user.getUserId());
        }
    }

    /**
     * Is the username valid for a GenePattern account. This does not check for similar names in the database.
     * It just enforces any rules on what constitutes a valid name.
     * <ul>
     * <li>No space characters allowed at the beginning or end of the name.
     * <li>Must map to valid filename on the servers file system. E.g. for unix, no '/' characters allowed.
     * </ul>
     * 
     * @param username
     * @throws AuthenticationException if the username is not valid
     */
    public void validateUsername(String username) throws AuthenticationException {
        if (username == null) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME,
                    "Username is null");
        }
        if (username.startsWith(" ")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, 
                    "Invalid username: '"+username+"': Can't start with a space (' ') character.");
        }
        if (username.endsWith(" ")) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, 
                    "Invalid username: '"+username+"': Can't end with a space (' ') character.");
        }
        if (username.contains(File.separator)) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, 
                    "Invalid username: '"+username+"': Can't contain a file separator ('"+File.separator+"') character.");
        }
    }

    /**
     * Is there already a GenePattern user account with this username.
     * 
     * @param username
     * @return
     */
    public boolean userExists(String username) {
        User user = (new UserDAO()).findByIdIgnoreCase(username);
        return user != null;
    }

    /**
     * Create a new GenePattern user account.
     * 
     * @param username
     * @throws AuthenticationException - if the user is already registered.
     */
    public void createUser(String username) 
    throws AuthenticationException
    {
        String password = "";
        createUser(username, password);
    }

    /**
     * Create a new GenePattern user account.
     * 
     * @param username
     * @param password
     * @throws AuthenticationException - if the user is already registered.
     */
    public void createUser(String username, String password) 
    throws AuthenticationException
    {
        String email = null;
        createUser(username, password, email);
    }
    
    /**
     * Create a new GenePattern user account.
     * 
     * @param username
     * @param password
     * @param email
     * @throws AuthenticationException - if the user is already registered.
     */
    public void createUser(String username, String password, String email) 
    throws AuthenticationException {
        validateNewUsername(username);

        if (password == null) {
            password = "";
        }

        User newUser = new User();
        newUser.setUserId(username);
        newUser.setRegistrationDate(new Date());
        if (email != null) {
            newUser.setEmail(email);
        }
        try {
            newUser.setPassword(EncryptionUtil.encrypt(password));
        } 
        catch (NoSuchAlgorithmException e) {
            log.error(e);
        }
        (new UserDAO()).save(newUser);
    }
    
    /**
     * Authenticate using the username:password pair by looking up the credentials in the GenePattern user database.
     * 
     * @param username
     * @param password - the user's unencrypted password
     * @return
     * @throws AuthenticationException
     */
    public boolean authenticateUser(String username, byte[] password) throws AuthenticationException {
        if (username == null) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, "Missing required parmameter: username");
        }

        if (passwordRequired && password == null) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_CREDENTIALS, "Missing required parmameter: password");
        }
        
        User user = null;
        try {
            user = (new UserDAO()).findById(username);
        }
        catch (Error e) {
            throw new AuthenticationException(AuthenticationException.Type.SERVICE_NOT_AVAILABLE, e.getLocalizedMessage());
        }
        if (user == null) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, "User '"+username+"' is not registered.");
        }
        if (!passwordRequired) {
            return true;
        }
        byte[] encryptedPassword = null;
        try {
            encryptedPassword = EncryptionUtil.encrypt(new String(password));
        }
        catch (NoSuchAlgorithmException e) {
            throw new AuthenticationException(AuthenticationException.Type.SERVICE_NOT_AVAILABLE, e.getLocalizedMessage());
        }
        if (java.util.Arrays.equals(encryptedPassword, user.getPassword())) {
            return true;
        }
        return false;
    }

    /**
     * Get the IAuthenticationPlugin for this GenePattern Server.
     * @return
     */
    public IAuthenticationPlugin getAuthentication() {
        return authentication;
    }
    
    /**
     * Get the IGroupMembershipPlugin for this GenePattern Server.
     * @return
     */
    public IGroupMembershipPlugin getGroupMembership() {
        return groupMembership;
    }
    
    /**
     * If necessary reload user and groups information by reloading the IAuthenticationPlugin and IGroupMembershipPlugins.
     * This supports one specific use-case: when GP default group membership is used, and an admin edits the configuration file,
     * it cause the GP server to reload group membership information from the config file.
     */
    public void refreshUsersAndGroups() {
        this.authentication = null;
        this.groupMembership = null;
        String customAuthenticationClass = System.getProperty(PROP_AUTHENTICATION_CLASS);
        String customGroupMembershipClass = System.getProperty(PROP_GROUP_MEMBERSHIP_CLASS);
        loadAuthentication(customAuthenticationClass);

        //check for special case: 
        //    use the same instance for both Authentication and GroupMembership 
        //    if and only if both are set to the same class
        if (this.authentication instanceof IGroupMembershipPlugin &&
            customAuthenticationClass != null && 
            !"".equals(customAuthenticationClass) && 
            customAuthenticationClass.equals(customGroupMembershipClass))
        {
            this.groupMembership = (IGroupMembershipPlugin) this.authentication;
        }
        else {
            loadGroupMembership(customGroupMembershipClass);            
        }
        this.groupMembership = new GroupMembershipWrapper(this.groupMembership);
    }
    
    private void loadAuthentication(String customAuthenticationClass) {
        if (customAuthenticationClass == null) {
            userAccountManager.authentication = new DefaultGenePatternAuthentication();
        }
        else {
            try {
                userAccountManager.authentication = (IAuthenticationPlugin) Class.forName(customAuthenticationClass).newInstance();
            } 
            catch (final Exception e) {
                log.error("Failed to load custom authentication class: "+customAuthenticationClass, e);
                userAccountManager.authentication = new NoAuthentication(e);
            } 
        }
    }
    
    private void loadGroupMembership(String customGroupMembershipClass) {
        if (customGroupMembershipClass == null) {
            File userGroupMapFile = new File(System.getProperty("genepattern.properties"), "userGroups.xml");
            this.groupMembership = new XmlGroupMembership(userGroupMapFile);                
        }
        else {
            try {
                this.groupMembership = (IGroupMembershipPlugin) Class.forName(customGroupMembershipClass).newInstance();
            }
            catch (Exception e) {
                log.error("Failed to load custom group membership class: "+customGroupMembershipClass, e);
                this.groupMembership = new DefaultGroupMembership();
            }
        }
    }
    
}
