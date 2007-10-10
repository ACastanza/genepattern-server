<%--
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
--%>

<!--
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2007) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
-->

<%@ page
	import="org.genepattern.webservice.TaskInfo,
		org.genepattern.server.process.ZipTask,
		org.genepattern.server.process.ZipTaskWithDependents,
		org.genepattern.server.genepattern.GenePatternAnalysisTask,
		java.io.*"
	session="false" language="Java"%>
<%
            response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
            response.setHeader("Pragma", "no-cache"); // HTTP 1.0 cache control
            response.setDateHeader("Expires", 0);
            String name = request.getParameter("name");
            if (name == null || name.length() == 0) {
                out.println("Must specify module name as name parameter.");
                return;
            }
            File zipFile = null;
            BufferedInputStream is = null;
            OutputStream os = response.getOutputStream();
            try {
                String userID = (String) request.getAttribute("userID");
                TaskInfo ti = GenePatternAnalysisTask.getTaskInfo(name, userID);
                ZipTask zt;
                String inclDependents = request.getParameter("includeDependents");
                if (inclDependents != null) {
                    zt = new ZipTaskWithDependents();
                } else {
                    zt = new ZipTask();
                }
                zipFile = zt.packageTask(ti, userID);

                String contentType = "application/x-zip-compressed; name=\"" + ti.getName() + ".zip" + "\";";
                response.addHeader("Content-Disposition", "attachment; filename=\"" + ti.getName() + ".zip" + "\";");
                response.setContentType(contentType);
                is = new BufferedInputStream(new FileInputStream(zipFile));
                int bytesRead = 0;
                byte[] b = new byte[10000];
                while ((bytesRead = is.read(b)) != -1) {
                    os.write(b, 0, bytesRead);
                }
            } catch (Exception e) {
				out.println("An error occurred while making the zip file.");
            } finally {
                if(zipFile!=null) {
                	zipFile.delete();
                }
                if(is!=null) {
                	try {
                	    is.close();
                	} catch(IOException x){}
                }
                os.flush();
            }
            %>
