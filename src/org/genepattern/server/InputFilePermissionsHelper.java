package org.genepattern.server;

import java.io.File;

import org.genepattern.server.webapp.jsf.AuthorizationHelper;

public class InputFilePermissionsHelper {
	 private String currentUser = null;
	 private boolean isAdmin = false;
	 private boolean isOwner = false;
	 private boolean canRead = false;
	   
	
	public InputFilePermissionsHelper(String userId, String filename){
		this.currentUser = userId;
		this.isAdmin = AuthorizationHelper.adminJobs(currentUser);
	
		File in = new File(System.getProperty("java.io.tmpdir"), filename);
        int underscoreIndex = filename.indexOf("_");
    	String owningUser = filename.substring(0, underscoreIndex);
    	
    	isOwner = userId.equals(owningUser);
    	canRead = (isAdmin || isOwner) && (in.exists());   	
	}


	public boolean isAdmin() {
		return isAdmin;
	}


	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}


	public boolean isOwner() {
		return isOwner;
	}


	public void setOwner(boolean isOwner) {
		this.isOwner = isOwner;
	}


	public boolean isCanRead() {
		return canRead;
	}


	public void setCanRead(boolean canRead) {
		this.canRead = canRead;
	}
	
	
	
	
}
