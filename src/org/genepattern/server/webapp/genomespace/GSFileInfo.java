package org.genepattern.server.webapp.genomespace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.genepattern.server.webapp.jsf.KeyValuePair;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.TaskInfo;
import org.genomespace.client.GsFile;

public class GSFileInfo {

	GsFile gsFile;
	String filename;
	String url;
	List<KeyValuePair> moduleInputParameters;
	List<KeyValuePair> moduleMenuItems = new ArrayList<KeyValuePair>();

	public GSFileInfo(GsFile aGsFile) {
		this.filename = aGsFile.getFilename();
		this.gsFile = aGsFile;
	}

	public String getFilename(){
		return filename;
	}
	
	public void setUrl(String u){
		url = u;
	}
	
	public String getUrl() {
	    return url;
	}

	public GsFile getGsFile() {
		return gsFile;
	}

	public void setGsFile(GsFile gsFile) {
		this.gsFile = gsFile;
	}

	public List<KeyValuePair> getModuleInputParameters() {
		return moduleInputParameters;
	}

	public void setModuleInputParameters(List<KeyValuePair> moduleInputParameters) {
		this.moduleInputParameters = moduleInputParameters;
	}

	public List<KeyValuePair> getModuleMenuItems() {
		return moduleMenuItems;
	}

	public void setModuleMenuItems(List<KeyValuePair> moduleMenuItems) {
		this.moduleMenuItems = moduleMenuItems;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public String getKind(){
	   int dotIndex = filename.lastIndexOf(".");
        String extension = null;
        if (dotIndex > 0) {
            extension = filename.substring(dotIndex + 1, filename.length());
        } else {
            return null;
        }
        return extension;
	}
		 
	
	
}
