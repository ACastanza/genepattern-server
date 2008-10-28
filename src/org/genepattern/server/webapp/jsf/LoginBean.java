/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.webapp.LoginManager;

/**
 * Backing bean for pages/login.
 * 
 * @author jrobinso
 * 
 */
public class LoginBean {
    private static Logger log = Logger.getLogger(LoginBean.class);

    private String username;
    private String password;
    private boolean unknownUser = false;
    private boolean invalidPassword = false;

    public LoginBean() {
    }

    public String getPassword() {
        return this.password;
    }

    public String getUsername() {
        return username;
    }

    public boolean isCreateAccountAllowed() {
        return UserAccountManager.instance().isCreateAccountAllowed();
    }

    public boolean isInvalidPassword() {
        return invalidPassword;
    }

    public boolean isPasswordRequired() {
        return UserAccountManager.instance().isPasswordRequired();
    }

    public boolean isUnknownUser() {
        return unknownUser;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Submit the user / password. For now this uses an action listener since we are redirecting to a page outside of
     * the JSF framework. This should be changed to an action to use jsf navigation in the future.
     * 
     * @param event --
     *                ignored
     */
    public void submitLogin(ActionEvent event) {
        if (username == null) {
            unknownUser = true;
            return;
        }

        try {
            HttpServletRequest request = UIBeanHelper.getRequest();
            HttpServletResponse response = UIBeanHelper.getResponse();
            LoginManager.instance().login(request, response, username, password, true);
        }
        catch (AuthenticationException e) {
            if (AuthenticationException.Type.INVALID_USERNAME.equals(e.getType())) {
                unknownUser = true;
            }
            if (AuthenticationException.Type.INVALID_CREDENTIALS.equals(e.getType())) {
                invalidPassword = true;
            }
            if (AuthenticationException.Type.SERVICE_NOT_AVAILABLE.equals(e.getType())) {
                log.error(e);
                throw new RuntimeException(e);
            }
        }
        catch (IOException e) {
            log.error(e);
            throw new RuntimeException(e); // @TODO -- wrap in gp system exception.
        }
    }

}
