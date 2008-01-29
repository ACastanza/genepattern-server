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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.util.GPConstants;

public class UIBeanHelper {
    private static Logger log = Logger.getLogger(UIBeanHelper.class);

    private UIBeanHelper() {
    }

    public static Map getSessionMap() {
	return FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
    }

    public static Map getRequestMap() {
	return FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
    }

    public static FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }

    public static ExternalContext getExternalContext() {
	return FacesContext.getCurrentInstance().getExternalContext();
    }

    public static HttpServletRequest getRequest() {
	FacesContext fc = FacesContext.getCurrentInstance();
	return fc != null ? (HttpServletRequest) getExternalContext().getRequest() : null;
    }

    public static HttpSession getSession() {
	return getRequest().getSession();
    }

    public static HttpSession getSession(boolean create) {
	return getRequest().getSession(create);
    }

    public static HttpServletResponse getResponse() {
	return (HttpServletResponse) getExternalContext().getResponse();
    }

    public static Object getManagedBean(String elExpression) {
	return getFacesContext().getApplication().createValueBinding(elExpression).getValue(getFacesContext());
    }

    public static void setInfoMessage(String summary) {
	getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null));
    }

    public static void setErrorMessage(String summary) {
	getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, null));
    }

    public static void setInfoMessage(UIComponent component, String summary) {
	getFacesContext().addMessage(component.getClientId(getFacesContext()),
		new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null));
    }

    public static void printAttributes() {
	System.out.println("Attributes:");
	Enumeration en = getRequest().getAttributeNames();
	while (en.hasMoreElements()) {
	    String name = (String) en.nextElement();
	    System.out.print(name + " -> ");
	    System.out.println(getRequest().getAttribute(name));

	}
    }

    public static void printParameters() {
	System.out.println("Parameters: ");
	Enumeration en = getRequest().getParameterNames();
	while (en.hasMoreElements()) {
	    String name = (String) en.nextElement();
	    System.out.print(name + " -> ");
	    for (String value : getRequest().getParameterValues(name)) {
		System.out.print(value + " ");
	    }
	    System.out.println();
	}
    }

    public static String getReferrer(HttpServletRequest request) {
	String referrer = (String) request.getSession().getAttribute("origin");
	request.getSession().removeAttribute("origin");
	if (referrer == null || referrer.length() == 0) {
	    referrer = request.getParameter("origin");
	}

	if (referrer == null || referrer.length() == 0) {
	    referrer = request.getContextPath() + "/pages/index.jsf";
	}
	return referrer;
    }

    public static String getUserId() {
	return (String) getRequest().getAttribute(GPConstants.USERID);
    }

    public static boolean isLoggedIn() {
	return getUserId() != null;
    }

    public static void logout(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        session.removeAttribute(GPConstants.USERID);
        session.invalidate();
    }

    public static void logout() {
        logout(getRequest(), getResponse(), getSession());
    }

    /**
     * 
     * @param username
     * @param sessionOnly
     *                whether the login cookie should be set for the session only
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public static void login(String username, boolean sessionOnly) throws UnsupportedEncodingException, IOException {
        UIBeanHelper.login(username, sessionOnly, true, UIBeanHelper.getRequest(), UIBeanHelper.getResponse());
    }

    /**
     * 
     * @param username
     * @param sessionOnly
     *                whether the login cookie should be set for the session only
     * @param redirect
     *                Whether to perform a redirect after login
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public static void login(String username, boolean sessionOnly, boolean redirect, HttpServletRequest request, HttpServletResponse response) 
    throws UnsupportedEncodingException, IOException 
    {
        User user = new UserDAO().findById(username);
        assert user != null;
        user.incrementLoginCount();
        user.setLastLoginDate(new Date());
        user.setLastLoginIP(request.getRemoteAddr());
        request.setAttribute(GPConstants.USERID, username);
        request.setAttribute("userID", username);
        request.getSession().setAttribute(GPConstants.USERID, user.getUserId());
        if (redirect) {
            String referrer = UIBeanHelper.getReferrer(request);
            response.sendRedirect(referrer);
            getFacesContext().responseComplete();
        }
    }

    public static String encode(String s) {
	if (s == null) {
	    return null;
	}
	try {
	    return URLEncoder.encode(s, "UTF-8");
	} catch (UnsupportedEncodingException e) {
	    log.error(e);
	    return s;
	}
    }

    public static String decode(String s) {
	if (s == null) {
	    return null;
	}
	try {
	    return URLDecoder.decode(s, "UTF-8");
	} catch (UnsupportedEncodingException e) {
	    log.error(e);
	    return s;
	}
    }

    /**
     * Gets the GenePatternURL or the server that the request came from. 
     * For example, http://localhost:8080/gp. 
     * Note if the GenePatternURL system property ends with a trailing '/', the slash is removed.
     * 
     * @return The server.
     */
    public static String getServer() {
        //Use GenePatternURL if it is set
        String server = System.getProperty("GenePatternURL", "");
        if (server != null && server.trim().length() > 0) {
            if (server.endsWith("/")) {
                server = server.substring(0, server.length() - 1);
            }
            return server;
        }
        //otherwise use the servlet request
        HttpServletRequest request = UIBeanHelper.getRequest();
        if (request != null) {
            String portStr = "";
            int port = request.getServerPort();
            if (port > 0) {
                portStr = ":"+port;
            }
            return request.getScheme() + "://" + request.getServerName() + portStr + request.getContextPath();
        }
        
        //TODO: handle this exception
        log.error("Invalid servername: GenePatternURL is null and UIBeanHelper.request is null!");
        return "http://localhost:8080/gp";
    }

}
