/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


/*
 * 
 *
 * Created on January 14, 2004, 6:54 AM
 */

package custom;



import com.zerog.ia.api.pub.*;

import java.net.*;
import java.rmi.server.UID;
import java.io.*;

/**

 * custom class for adding /resources to the value of $R25$
 * @author  Liefeld
 */

public class SetRVariablePath extends CustomCodeAction {
  

    /** Creates a new instance of RegisterGenePattern */

    public SetRVariablePath () {
    }
  
    public String getInstallStatusMessage() {
        return "SetRVariablePath ";
    }

    public String getUninstallStatusMessage() {
        return "";
    }

    

    public void install(com.zerog.ia.api.pub.InstallerProxy ip) throws com.zerog.ia.api.pub.InstallException {
	try {
		
		String R_new = ip.substitute("$R25$");

		String os = System.getProperty("os.name").toLowerCase();

		if (os.indexOf("mac") >= 0){
	 		ip.setVariable("R25bin",  R_new + "/Resources");
			ip.setVariable("R25base",  R_new);
		} else {
	 		ip.setVariable("R25bin",  R_new);
			ip.setVariable("R25base",  R_new);
		}

        
       } catch (Exception e){
       	e.printStackTrace();
       }
    }
    

	
	
	

	

    
    public void uninstall(com.zerog.ia.api.pub.UninstallerProxy uninstallerProxy) throws com.zerog.ia.api.pub.InstallException {
    // do nothing on uninstall
    
    }
    
}
