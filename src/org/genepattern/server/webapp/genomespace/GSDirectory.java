package org.genepattern.server.webapp.genomespace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.genepattern.server.webapp.jsf.KeyValuePair;
import org.genepattern.server.webapp.jsf.UIBeanHelper;

import org.genepattern.util.GPConstants;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.TaskInfo;
import org.genomespace.client.GsFile;

public class GSDirectory {
	private static final Comparator<KeyValuePair> COMPARATOR = new KeyValueComparator();

	public String name;
	public List<GSFileInfo> gsFiles;
	public boolean expanded=true;
	public int level=0;
	
	public GSDirectory(String nom){
		this.name = nom;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<GSFileInfo> getGsFiles() {
		if (gsFiles == null) gsFiles = new ArrayList<GSFileInfo>();
		return gsFiles;
	}

	public void setGsFiles(List<GSFileInfo> gsFiles) {
		this.gsFiles = gsFiles;
	
	}

	public void setGsFileList(List<GsFile> gsfiles, Map<String, Collection<TaskInfo>> kindToModules, GenomeSpaceBean genomeSpaceBean) {
		this.gsFiles = new ArrayList<GSFileInfo>();
		for (GsFile afile: gsfiles){
			GSFileInfo info = new GSFileInfo(afile);
			this.gsFiles.add(info);
			info.setUrl(genomeSpaceBean.getFileURL(name, afile.getFilename()));
			
			String kind = SemanticUtil.getKind(afile.getFile());
            Collection<TaskInfo> modules;
            List<KeyValuePair> moduleMenuItems = new ArrayList<KeyValuePair>();
            modules = kindToModules.get(kind);
           
            if (modules != null) {
				for (TaskInfo t : modules) {
				    KeyValuePair mi = new KeyValuePair(t.getShortName(), UIBeanHelper.encode(t.getLsid()));
				    moduleMenuItems.add(mi);
				}
				Collections.sort(moduleMenuItems, COMPARATOR);
			}
			info.setModuleMenuItems(moduleMenuItems);
			
			
		}
	
	}

	public boolean isExpanded() {
		return expanded;
	}

	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}
	

	private static class KeyValueComparator implements Comparator<KeyValuePair> {

		public int compare(KeyValuePair o1, KeyValuePair o2) {
		    return o1.getKey().compareToIgnoreCase(o2.getKey());
		}

	    }
}


