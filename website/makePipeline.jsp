<%@ page import="org.genepattern.server.webapp.*,
		 org.genepattern.data.pipeline.*,
		 java.io.File,
		 java.io.FilenameFilter,
		 java.io.FileInputStream,
		 java.io.FileOutputStream,
		 java.io.FileWriter,
		 java.io.InputStream,
		 java.io.IOException,
		 java.lang.reflect.Constructor,
		 java.net.URLEncoder,
		 java.util.ArrayList,
		 java.util.Collection,
		 java.util.Enumeration,
		 java.util.HashMap,
		 java.util.Hashtable,
		 java.util.Iterator,
		 java.util.List,
		 java.util.Map,
		 java.util.StringTokenizer,
		 java.util.TreeMap,
		 java.util.Vector,
		 org.genepattern.webservice.ParameterFormatConverter,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.server.util.AccessManager,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.server.webservice.server.local.*,
		 org.genepattern.server.genepattern.TaskInstallationException,
		 org.genepattern.server.webservice.server.local.LocalAdminClient,
		 org.genepattern.data.pipeline.*,
		 org.genepattern.util.GPConstants,
 		 org.genepattern.util.StringUtils,
		 org.genepattern.codegenerator.*,
		 com.jspsmart.upload.*,
		 java.io.StringWriter"

	session="false" contentType="text/html" language="Java" %><jsp:useBean id="mySmartUpload" scope="page" class="com.jspsmart.upload.SmartUpload" /><%

	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);
	com.jspsmart.upload.Request requestParameters = null;

	if (!request.getMethod().equalsIgnoreCase("post")) {
%>
			<html>
			<head>
			<link href="stylesheet.css" rel="stylesheet" type="text/css">
			<link href="favicon.ico" rel="shortcut icon">
			<title>Delete pipeline</title>
			</head>
			<body>
			<jsp:include page="navbar.jsp"></jsp:include>
		Error: must submit from <a href="pipelineDesigner.jsp">pipelineDesigner.jsp</a>
<jsp:include page="footer.jsp"></jsp:include>
<%
		return;
	}

String serverPort = System.getProperty("GENEPATTERN_PORT");
String userID = null;
boolean bRun = false;
boolean bClone = false;
boolean bDelete = false;

try {
	// mySmartUpload is from http://www.jspsmart.com/
	// Initialization
	mySmartUpload.initialize(pageContext);
	mySmartUpload.upload();
	requestParameters = mySmartUpload.getRequest();
	userID = requestParameters.getParameter(GPConstants.USERID);
	String RUN = "run";
	String CLONE = "clone";

	boolean DEBUG = false; // (requestParameters.getParameter("debug") != null);

	if (DEBUG) {
		System.out.println("\n\nMAKEPIPELINE Request parameters:<br>");
		for (java.util.Enumeration eNames = requestParameters.getParameterNames(); eNames.hasMoreElements(); ) {
			String n = (String)eNames.nextElement();
                        if (!("code".equals(n)))
			System.out.println(n + "='" + StringUtils.htmlEncode(requestParameters.getParameter(n)) + "'");
		}
		out.println("<hr><br>");
	}

	bRun = requestParameters.getParameter("cmd").equals(RUN);
	bClone = requestParameters.getParameter("cmd").equals(CLONE);
	bDelete = requestParameters.getParameter("delete") != null;

	String pipelineName = requestParameters.getParameter("pipeline_name");
	if (bDelete) {
		try {
			TaskInfo taskInfo = GenePatternAnalysisTask.getTaskInfo(requestParameters.getParameter("changePipeline"), userID);
			String lsid = (String)taskInfo.getTaskInfoAttributes().get(GPConstants.LSID);
			String attachmentDir = GenePatternAnalysisTask.getTaskLibDir(requestParameters.getParameter("pipeline_name"), lsid, userID); // + "." + GPConstants.TASK_TYPE_PIPELINE);
			File dir = new File(attachmentDir);
			try {
				GenePatternAnalysisTask.deleteTask(lsid);
			} catch (Exception oe) {
				// ignore, probably already deleted
			}

			// clear out the directory
			File[] oldFiles = dir.listFiles();
			for (int i=0; oldFiles != null && i < oldFiles.length; i++) {
				oldFiles[i].delete();
			}
			dir.delete();
%>
			<html>
			<head>
			<link href="stylesheet.css" rel="stylesheet" type="text/css">
			<link href="favicon.ico" rel="shortcut icon">
			<title>Delete pipeline</title>
			</head>
			<body>
			<jsp:include page="navbar.jsp"></jsp:include>
			Stopped and deleted <%= taskInfo.getName() %> along with its support files.<br><br>
<%
		} catch (Throwable t) { 
			out.println(t + " while attempting to delete " + pipelineName);
		}
		return;
	}

	if (pipelineName.endsWith("." + GPConstants.TASK_TYPE_PIPELINE)) pipelineName = pipelineName.substring(0, pipelineName.lastIndexOf("."));

	if (bRun && (pipelineName == null || pipelineName.trim().length() == 0)) {
		pipelineName = "unnamed" + "." + GPConstants.TASK_TYPE_PIPELINE;
	}
	
	if (!bRun && (pipelineName == null || pipelineName.trim().length() == 0)) {
%>
		<html>
		<head>
		<link href="stylesheet.css" rel="stylesheet" type="text/css">
		<link href="favicon.ico" rel="shortcut icon">
		<title>Delete pipeline</title>
		</head>
		<body>
		<jsp:include page="navbar.jsp"></jsp:include>
		Error: pipeline must be named.  
		<a href="javascript:window.close()">back</a>
<%
		return;
	}
	
	Hashtable htFilenames = new Hashtable(); // map form field names to filenames for attached (fixed) files
	
	// tranform requestParameters into model data
	String taskName;
	String taskPrefix = null;
	String taskLSID;
	int numParameterInfos;
	String key = null;
	String value = null;
	TaskInfo pTaskInfo = new TaskInfo();
	TaskInfoAttributes pTia = null;
	Vector vProblems = new Vector();
	Map taskCatalog = new LocalAdminClient(requestParameters.getParameter(GPConstants.USERID)).getTaskCatalogByLSID();

	PipelineModel model = new PipelineModel();
	// NB: could use any language Fcode generator here
	String language = requestParameters.getParameter(GPConstants.LANGUAGE);
	if (language == null) language = "R";
	String version = requestParameters.getParameter(GPConstants.VERSION);
	
	JobSubmission jobSubmission = null;
	String paramName = null;
	String modelName = pipelineName;
	if (modelName == null || modelName.equals("")) {
		if (bRun) {
			modelName = "unnamed";
		} else {
			modelName = "p" + Double.toString(Math.random()).substring(2);
		}
	}
	model.setName(modelName);
	model.setDescription(requestParameters.getParameter("pipeline_description"));
	model.setAuthor(requestParameters.getParameter("pipeline_author"));
	String lsid = requestParameters.getParameter(GPConstants.LSID);
	boolean isTemp = modelName.startsWith("try.") || bRun;
	if (lsid == null) {
		lsid = "";
	}
	model.setLsid(lsid);
	model.setVersion(version);
	String display = requestParameters.getParameter("display");
	if (display == null || display.length() == 0) display = requestParameters.getParameter("custom");
	model.setUserID(requestParameters.getParameter(GPConstants.USERID));
	String privacy = requestParameters.getParameter(GPConstants.PRIVACY);
	model.setPrivacy(privacy != null && privacy.equals(GPConstants.PRIVATE));

	// save uploaded files as part of pipeline definition
	if (mySmartUpload.getFiles().getCount() > 0) {
		String attachmentDir = null;
		File dir = null;
		String attachmentName = null;

		com.jspsmart.upload.File attachedFile = null;
		for (int i=0;i<mySmartUpload.getFiles().getCount();i++){
			attachedFile = mySmartUpload.getFiles().getFile(i);
			if (attachedFile.isMissing()) continue;
			attachmentName = attachedFile.getFileName();
			if (attachmentName.trim().length() == 0) continue;
			String fieldName = attachedFile.getFieldName();
			String fullName = attachedFile.getFilePathName();
			if (DEBUG) System.out.println("makePipeline: " + fieldName + " -> " + fullName);
			if (fullName.startsWith("http:") || fullName.startsWith("ftp:") || fullName.startsWith("file:")) {
				// don't bother trying to save a file that is a URL, retrieve it at execution time instead
				htFilenames.put(fieldName, fullName); // map between form field name and filesystem name
				continue;
			}
			
			if (isTemp) {
				// leave the task name blank for getFile and put the file into the temp directory
				model.setLsid("");
				// it's for a temporary pipeline
				dir = new File(System.getProperty("java.io.tmpdir"));
			}
			htFilenames.put(fieldName, "<GenePatternURL>getFile.jsp?task=" + GPConstants.LEFT_DELIMITER + GPConstants.LSID + GPConstants.RIGHT_DELIMITER + "&file=" + URLEncoder.encode(attachmentName)); // map between form field name and filesystem name
		}
	}

	for (int taskNum = 0; ; taskNum++) {
		taskPrefix = "t" + taskNum;
		taskLSID = requestParameters.getParameter(taskPrefix + "_taskLSID");
		taskName = requestParameters.getParameter(taskPrefix + "_taskName");
		if (taskName == null) break;
//System.out.println("\ntask " + taskNum + ": " + taskName);
		TaskInfo mTaskInfo = null;
		if (taskLSID != null && taskLSID.length() > 0) {
			mTaskInfo = (TaskInfo)taskCatalog.get(taskLSID);
		}
		if (mTaskInfo == null) {
			mTaskInfo = (TaskInfo)taskCatalog.get(taskName);
		}
		if (mTaskInfo == null) {
			vProblems.add("makePipeline: couldn't find task number " + taskNum + " searching for name " + taskName + " or LSID " + taskLSID);
			continue;
		}
		TaskInfoAttributes mTia = mTaskInfo.giveTaskInfoAttributes();
		if (DEBUG) out.println("<br>" + mTaskInfo.getName() + "<br>");

		ParameterInfo[] params = new ParameterFormatConverter().getParameterInfoArray(mTaskInfo.getParameterInfo());
		ParameterInfo p = null;
		boolean[] runTimePrompt = (params != null ? new boolean[params.length] : null);
		String inheritedTaskNum = null;
		String inheritedFilename = null;
		String origValue = null;
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				p = params[i];
				paramName = p.getName();
				origValue = p.getValue();
//System.out.println("LOOK FOR: "+ taskPrefix + "_" + paramName);
				String val = requestParameters.getParameter(taskPrefix + "_" + paramName);
        
				if (val == null) {
					val = requestParameters.getParameter(taskName + (taskNum+1) + "." + 
						taskPrefix + "_" + paramName);
				}
				if (val != null) val = GenePatternAnalysisTask.replace(val, "\\", "\\\\");
                                
				p.setValue(val);

				runTimePrompt[i] = (requestParameters.getParameter(taskPrefix + "_prompt_" + i) != null);
                                
				String inheritFrom = requestParameters.getParameter(taskPrefix + "_i_" + i);
				boolean inherited = (inheritFrom != null && inheritFrom.length() > 0 && !inheritFrom.equals("NOT SET"));

				boolean isOptional = (((String)p.getAttributes().get(GPConstants.PARAM_INFO_OPTIONAL[0])).length() > 0);

//System.out.println(taskName + ": " + paramName + "=" + val + ", prompt= " + runTimePrompt[i] + ", optional=" + isOptional + ", inherited=" + inherited + " (" + requestParameters.getParameter(taskPrefix + "_i_" + i) + "), isInputFile=" + p.isInputFile());
				// inheritance has priority over run time prompt
				if (inherited) {
					runTimePrompt[i] = false;
					inheritedTaskNum = requestParameters.getParameter(taskPrefix + "_i_" + i);
					inheritedFilename = null;

					String[] values = requestParameters.getParameterValues(taskPrefix + "_if_" + i);
					for (int x=0; values!=null && x < values.length ; x++){
					 	if (x > 0) {
							inheritedFilename = inheritedFilename + GPConstants.PARAM_INFO_CHOICE_DELIMITER;
						} else {
							inheritedFilename = "";
						}
						inheritedFilename += values[x];
					}
				}

				if (runTimePrompt[i]) {
					p.getAttributes().put("runTimePrompt", "1");
					model.addInputParameter(taskName + (taskNum + 1) + "." + paramName, p);
				}

				if (inheritedTaskNum != null || inheritedFilename != null) {
					if (DEBUG) out.println(taskPrefix + "_i_" + i + "=" + requestParameters.getParameter(taskPrefix + "_i_" + i) + "<br>");
					if (DEBUG) out.println(taskPrefix + "_if_" + i + "=" + requestParameters.getParameter(taskPrefix + "_if_" + i) + "<br>");
				}
				if (DEBUG) out.println(paramName + " is " + (inherited ? "" : "not ") + " inherited and is " + (runTimePrompt[i] ? "" : "not ") + " runtime-prompted<br>");

				// inheritance and run time prompt both have priority over explicitly named input file
				if (inherited || runTimePrompt[i]) {
					p.setValue("");
				}
				if (p.isInputFile()) {
					if (inherited) {
						p.getAttributes().put(AbstractPipelineCodeGenerator.INHERIT_FILENAME, inheritedFilename);
						p.getAttributes().put(AbstractPipelineCodeGenerator.INHERIT_TASKNAME, inheritedTaskNum);
					} else {
						String shadowName = taskPrefix + "_shadow" + i;
						String shadow = requestParameters.getParameter(shadowName);
//System.out.println(shadowName + "=" + shadow);
						//if (shadow == null || shadow.length() == 0) shadow = (String)htFilenames.get(taskName + "1." + taskPrefix+ "_" + p.getName());
						if (shadow != null && (shadow.startsWith("http:") || shadow.startsWith("https:") || shadow.startsWith("ftp:") || shadow.startsWith("file:") || shadow.startsWith("<GenePatternURL>"))) {

							// if this is a URL that is in the taskLib, repoint it to the cloned taskLib
							if (bClone) {
								String taskFile = "<GenePatternURL>getFile.jsp?task=" + GPConstants.LEFT_DELIMITER + GPConstants.LSID + GPConstants.RIGHT_DELIMITER + "&file=";
								if (shadow.startsWith(taskFile)) {
									taskFile = shadow.substring(taskFile.length());
									// use clone's LSID, not the name
									shadow = "<GenePatternURL>getFile.jsp?task=" + GPConstants.LEFT_DELIMITER + GPConstants.LSID + GPConstants.RIGHT_DELIMITER + "&file=" + URLEncoder.encode(taskFile, "UTF-8");
								}
							}
							htFilenames.put(taskPrefix + "_" + paramName, shadow);
						}
                                                String filenameKey = taskPrefix + "_" + paramName;
                                                //System.out.println("shadow: isInputFile: " + htFilenames.get(filenameKey));

						p.setValue((String)htFilenames.get(filenameKey));
					}
				}

				// if runtime prompt, save choice list for display later
				if (runTimePrompt[i]) {
					p.setValue(origValue);
				}

				if (!inherited && !runTimePrompt[i] && (p.getValue() == null || p.getValue().equals("")) && !isOptional) {
					vProblems.add("Step " + (taskNum+1) + ", " + taskName + ", is missing required parameter " + p.getName());
				}
			}
		}
                
                
		boolean isVisualizer = ((String)mTia.get(GPConstants.TASK_TYPE)).equals(GPConstants.TASK_TYPE_VISUALIZER);
		jobSubmission = new JobSubmission(taskName, mTaskInfo.getDescription(), taskLSID, params, runTimePrompt, isVisualizer, mTaskInfo);

		model.addTask(jobSubmission);
	}

	if ((!bRun && !bClone) || vProblems.size() > 0) {
%>
		<html>
		<head>
		<link href="stylesheet.css" rel="stylesheet" type="text/css">
		<link href="favicon.ico" rel="shortcut icon">
		<title><%= pipelineName %> - saved</title>
		<script language="Javascript">window.focus();</script>
		</head>
		<body>
		<jsp:include page="navbar.jsp"></jsp:include>
<%
	}
   PipelineController controller = new PipelineController(model);

	//lsid = null;
	if (vProblems.size() == 0) {
		String oldLSID = lsid;
		if (bClone) {
			model.setName(requestParameters.getParameter("cloneName"));
			// TODO: change URLs that are task-relative to point to the new task
			String oldUser = model.getUserID();
			String requestUserID = (String)request.getAttribute("userID");
			if (oldUser.length() > 0 && !oldUser.equals(requestUserID)) {
				oldUser = " (" + oldUser + ")";
			} else {
				oldUser = "";
			}
			model.setUserID(requestUserID + oldUser);
			model.setLsid("");
		}

		
		if (!bRun) {
			// save the task to the database
			try {
				lsid = controller.generateTask();
				model.setLsid(lsid);
	 			if (bClone || !oldLSID.equals("")) {
	 				// TODO: change URLs that are task-relative to point to the new task
					// System.out.println("copying support files from " + oldLSID + " to " + lsid);
		 			copySupportFiles(modelName, model.getName(), oldLSID, lsid, userID);
	 			}
			} catch (TaskInstallationException tie) {
				vProblems.addAll(tie.getErrors());
			}
		}

		// save uploaded files as part of pipeline definition
		if (mySmartUpload.getFiles().getCount() > 0) {
			String attachmentDir = null;
			File dir = null;
			String attachmentName = null;

			if (!isTemp) {
				attachmentDir = GenePatternAnalysisTask.getTaskLibDir(modelName + "." + GPConstants.TASK_TYPE_PIPELINE, lsid, userID);
				dir = new File(attachmentDir);
				dir.mkdir();
			}
			com.jspsmart.upload.File attachedFile = null;
			for (int i=0;i<mySmartUpload.getFiles().getCount();i++){
				attachedFile = mySmartUpload.getFiles().getFile(i);
				if (attachedFile.isMissing()) continue;
				try {
					attachmentName = attachedFile.getFileName();
					if (attachmentName.trim().length() == 0) continue;
					String fieldName = attachedFile.getFieldName();
					String fullName = attachedFile.getFilePathName();
					if (DEBUG) System.out.println("makePipeline: " + fieldName + " -> " + fullName);
					if (fullName.startsWith("http:") || fullName.startsWith("ftp:") || fullName.startsWith("file:")) {
						// don't bother trying to save a file that is a URL, retrieve it at execution time instead
						htFilenames.put(fieldName, fullName); // map between form field name and filesystem name
						continue;
					}
					
					if (isTemp) {
						// leave the task name blank for getFile and put the file into the temp directory
						model.setLsid("");
						// it's for a temporary pipeline
						dir = new File(System.getProperty("java.io.tmpdir"));
					}
					htFilenames.put(fieldName, "<GenePatternURL>getFile.jsp?task=" + GPConstants.LEFT_DELIMITER + GPConstants.LSID + GPConstants.RIGHT_DELIMITER + "&file=" + URLEncoder.encode(attachmentName)); // map between form field name and filesystem name

					attachmentName = dir.getPath() + File.separator + attachmentName;
					File attachment = new File(attachmentName);
					if (attachment.exists()) {
						attachment.delete();
					}
						
					attachedFile.saveAs(attachmentName);

					if (DEBUG) System.out.println(fieldName + "=" + fullName + " (" + attachedFile.getSize() + " bytes) in " + htFilenames.get(fieldName) + "<br>");
				} catch (SmartUploadException sue) {
				    	throw new Exception("error saving " + attachmentName  + ": " + sue.getMessage());
				}
			}
		}


		// run immediately, without saving?
		if (bRun) {
			request.setAttribute("cmd", "run");
			request.setAttribute("name", pipelineName + (pipelineName.endsWith(GPConstants.TASK_TYPE_PIPELINE) ? "" : ("." + GPConstants.TASK_TYPE_PIPELINE)));
			
			request.setAttribute("saved", Boolean.FALSE);
         
			pTia = controller.giveTaskInfoAttributes();
                        pTaskInfo.setTaskInfoAttributes(pTia);
                        pTaskInfo.setParameterInfoArray(controller.giveParameterInfoArray());
			pTaskInfo.setName(pipelineName + "." + GPConstants.TASK_TYPE_PIPELINE);
			pTia.put(GPConstants.COMMAND_LINE, GPConstants.LEFT_DELIMITER + GPConstants.R + GPConstants.RIGHT_DELIMITER + " scriptNameNotUsed.r " + pipelineName + "." + GPConstants.TASK_TYPE_PIPELINE);
			
			pTia.put(GPConstants.CPU_TYPE, GPConstants.ANY);
			pTia.put(GPConstants.OS, GPConstants.ANY);
			pTia.put(GPConstants.LANGUAGE, language);
			pTia.put(GPConstants.SERIALIZED_MODEL, model.toXML());
			pTia.put(GPConstants.LSID, "" /*model.getLsid()*/);

			request.setAttribute("taskInfo", pTaskInfo);
			request.setAttribute("serverPort", serverPort);
			taskName = requestParameters.getParameter("taskName");
			request.setAttribute("taskName", taskName);
			if (taskName != null) {
				String singleTaskRun = taskName + "1.t0_";

				for (java.util.Enumeration eNames = requestParameters.getParameterNames(); eNames.hasMoreElements(); ) {
					String n = (String)eNames.nextElement();
					if (n.indexOf(singleTaskRun) != -1) {
						String n2 = taskName + "1." + n.substring(singleTaskRun.length());
						n2 = n2.substring(taskName.length() + ".".length() + 1);
						request.setAttribute(n2, requestParameters.getParameter(n));
					}
				}
				for (java.util.Enumeration eNames = htFilenames.keys(); eNames.hasMoreElements(); ) {
					key = (String)eNames.nextElement();
					int n = 0;
					if (key.startsWith(singleTaskRun)) {
						n = singleTaskRun.length();
					} else if (key.startsWith("t0_")) {
						n = "t0_".length();
					}
					String strippedName = key.substring(n);
					request.setAttribute(strippedName, (String)htFilenames.get(key));
				}
			}
			
			request.setAttribute("smartUpload", requestParameters);
			request.setAttribute("name", pipelineName + ".pipeline");
			request.getRequestDispatcher("runPipeline.jsp").forward(request, response);


			return;
		}
	}

	if (vProblems.size() > 0) {
%>
		There are some problems with the <%= model.getName() %> pipeline description that need to be fixed:<br>
		<ul>
<%	
    		for (Enumeration eProblems = vProblems.elements(); eProblems.hasMoreElements(); ) {
%>
			<li><%= StringUtils.htmlEncode((String)eProblems.nextElement()) %></li>
<%
		}
%>
		</ul>
		<a href="javascript:history.back()">back</a><br>
		<script language="javascript">
			window.resizeTo(600, 500);
			window.opener.focus();
			window.toolbar.visibility = false;
			window.personalbar.visibility = false;
			window.menubar.visibility = false;
			window.locationbar.visibility = false;
			window.focus();
		</script>
<%
		return;
	} else {
%>
		<script language="Javascript">
		addNavbarItem("<%= model.getName() + "." + GPConstants.TASK_TYPE_PIPELINE %>", "<%= model.getLsid() %>");
		</script>
<%
		// delete the legacy R file for the pipeline, if it exists
		pipelineName = model.getName() + "." + GPConstants.TASK_TYPE_PIPELINE;
		String dir = GenePatternAnalysisTask.getTaskLibDir(pipelineName, lsid, userID);
      out.println(model.getName() + " version " + new org.genepattern.util.LSID(model.getLsid()).getVersion()  + " has been saved.");
		new File(dir, model.getName() + ".r").delete();

		if (requestParameters.getParameter("cmd").equals(CLONE)) {
			response.sendRedirect("pipelineDesigner.jsp?" + GPConstants.NAME + "=" + lsid);
			return;
		}

		if (requestParameters.getParameter("autoSave").length() > 0) {
			out.println("<script language=\"Javascript\">window.close();</script>");
		}

		out.println("<form action=\"runTask.jsp\" method=\"post\">");
		out.println("<input type=\"hidden\" name=\"" + GPConstants.NAME + "\" value=\"" + lsid + "\">");
		out.println("<br><br><center><input type=\"submit\" value=\"run\" name=\"cmd\">&nbsp;&nbsp;");
		//out.println("<input type=\"submit\" value=\"edit pipeline code\" name=\"cmd\">");
		out.println("</center></form>");

		out.println("<a href=\"pipelineDesigner.jsp?" + GPConstants.NAME + "=" + lsid + "\">modify " + pipelineName + " design</a><br>");
		out.println("<a href=\"addTask.jsp?" + GPConstants.NAME + "=" + lsid + "\">edit task for " + pipelineName + "</a><br>");
	}
} catch (Exception e) {
%>
	makePipeline failed: <br>
	<%= e.getMessage() %><br>
	<pre>
	<% e.printStackTrace(); %>
	</pre><br>
	<a href="javascript:history.back()">back</a><br>
<%
} finally {
	if (!bClone) {
%>
		<jsp:include page="footer.jsp"></jsp:include>
		</body>
		</html>
<%
	}
} %>
<%! void copySupportFiles(String oldTaskName, String newTaskName, String oldLSID, String newLSID, String userID) throws Exception {
	String oldDir = GenePatternAnalysisTask.getTaskLibDir(oldTaskName + "." + GPConstants.TASK_TYPE_PIPELINE, oldLSID, userID);
	String newDir = GenePatternAnalysisTask.getTaskLibDir(newTaskName + "." + GPConstants.TASK_TYPE_PIPELINE, newLSID, userID);
	File[] oldFiles = new File(oldDir).listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return (!name.endsWith(".old") && !name.equals("version.txt"));
				} });
	byte[] buf = new byte[100000];
	int j;
	for (int i=0; oldFiles != null && i < oldFiles.length; i++) {
		FileInputStream is = new FileInputStream(oldFiles[i]);
		FileOutputStream os = new FileOutputStream(new File(newDir, oldFiles[i].getName()));
		while ((j = is.read(buf, 0, buf.length)) > 0) {
			os.write(buf, 0, j);
		}
		is.close();
		os.close();
	}
    }
%>