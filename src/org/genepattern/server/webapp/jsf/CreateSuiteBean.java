/**
 * 
 */
package org.genepattern.server.webapp.jsf;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.io.FilenameUtils;
import org.apache.myfaces.custom.fileupload.HtmlInputFileUpload;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.Suite;
import org.genepattern.server.domain.SuiteHome;
import org.genepattern.server.genepattern.LSIDManager;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.TaskIntegrator;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.dao.TaskIntegratorDAO;
import org.genepattern.util.LSID;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

import static org.genepattern.server.webapp.jsf.UIBeanHelper.getUserId;

/**
 * @author jrobinso
 * 
 */
public class CreateSuiteBean implements java.io.Serializable {

    private static final long serialVersionUID = 352540582209631173l;
    private String name;
    private String description;
    private String author;
    private String accessId = "1"; // Public
    private UploadedFile supportFile1;
    private UploadedFile supportFile2;
    private UploadedFile supportFile3;
    private List<ModuleCategory> categories;
    private boolean success = false; // Default value

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getAccessId() {
        return accessId;
    }

    public void setAccessId(String accessId) {
        this.accessId = accessId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List getCategoryColumns() {

        List<List> cols = new ArrayList<List>();
        if(categories == null) {
          categories = (new ModuleHelper()).getTasksByType();
        }
        
        // Find the midpoint in the category list.
        int totalCount = 0;
        for (ModuleCategory cat : categories) {
            totalCount += cat.getModuleCount();
        }
        int midpoint = totalCount / 2;

        cols.add(new ArrayList());
        cols.add(new ArrayList());
        int cumulativeCount = 0;
        for (ModuleCategory cat : categories) {
            if (cumulativeCount < midpoint) {
                cols.get(0).add(cat);
            }
            else {
                cols.get(1).add(cat);
            }
            cumulativeCount += cat.getModuleCount();
        }
        return cols;
    }

    public UploadedFile getSupportFile1() {
        return supportFile1;
    }

    public void setSupportFile1(UploadedFile supportFile1) {
        this.supportFile1 = supportFile1;
    }

    public UploadedFile getSupportFile2() {
        return supportFile2;
    }

    public void setSupportFile2(UploadedFile supportFile2) {
        this.supportFile2 = supportFile2;
    }

    public UploadedFile getSupportFile3() {
        return supportFile3;
    }

    public void setSupportFile3(UploadedFile supportFile3) {
        this.supportFile3 = supportFile3;
    }

    public String save() {

        try {
 
            LSID lsidObj = LSIDManager.getInstance().createNewID(org.genepattern.util.IGPConstants.SUITE_NAMESPACE);          
            String lsid = lsidObj.toString();

           
            // Save database record
            Suite suite = new Suite();
            suite.setLsid(lsid);
            suite.setOwner(getUserId());
            suite.setName(name);
            suite.setDescription(description);
            suite.setAccessId(new Integer(accessId));
            suite.setAuthor(author);
           
            List<String> selectedLSIDs = new ArrayList<String>();
            for (ModuleCategory cat : categories) {
                for (Module mod : cat.getModules()) {
                    if (mod.isSelected()) {
                        selectedLSIDs.add(mod.getSelectedVersion());
                     }
                }
            }
            if(!selectedLSIDs.isEmpty()) {
                suite.setModules(selectedLSIDs);
            }
            HibernateUtil.getSession().save(suite);

            // Save uploaded files, if any
            String suiteDir = DirectoryManager.getSuiteLibDir(suite.getName(), lsid, suite.getOwner());
            if (supportFile1 != null) {
                saveUploadedFile(supportFile1, suiteDir);
            }
            if (supportFile2 != null) {
                saveUploadedFile(supportFile2, suiteDir);
            }
            if (supportFile3 != null) {
                saveUploadedFile(supportFile3, suiteDir);
            }
            
            RunTaskBean homePageBean = (RunTaskBean) UIBeanHelper.getManagedBean("#{runTaskBean}");
            homePageBean.setSplashMessage("Suite " + suite.getName() + " was successfully created.");
            
            return "home";
        }
        catch (Exception e) {
            HibernateUtil.rollbackTransaction(); // This shouldn't be
                                                    // neccessary, but just in
                                                    // case
            throw new RuntimeException(e); // @todo -- replace with appropriate
                                            // GP exception
        }

    }

    private void saveUploadedFile(UploadedFile uploadedFile, String suiteDir) throws FileNotFoundException, IOException {
        String fileName = uploadedFile.getName();
        if (fileName != null) {
            fileName = FilenameUtils.getName(fileName);

        }
        FileOutputStream out = new FileOutputStream(new File(suiteDir, fileName));
        InputStream in = uploadedFile.getInputStream();
        int c;
        while ((c = in.read()) != -1) {
            out.write(c);
        }
        in.close();
        out.close();
    }

    public String clear() {
        return null;
    }

    public List<ModuleCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<ModuleCategory> categories) {
        this.categories = categories;
    }

}
