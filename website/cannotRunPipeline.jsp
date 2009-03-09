<%--
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2009) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
--%>


<%@ page import="java.io.IOException,
		 java.util.StringTokenizer,
		 java.util.Enumeration,
		 java.util.HashMap,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.webservice.ParameterFormatConverter,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.webservice.OmnigeneException,  
		 org.genepattern.data.pipeline.PipelineModel"
	session="false" contentType="text/html" language="Java" %>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
	<link href="skin/favicon.ico" rel="shortcut icon">
	<title>Cannot Run Pipeline</title>
    <jsp:include page="navbarHead.jsp"/>	
</head>

<body>
	<jsp:include page="navbar.jsp"/>
<%
String taskName = (String)request.getAttribute("name");
%>
<p>
The <%=taskName%> cannot be run as pipelines that prompt for runtime parameters must be saved first.<p>	

<jsp:include page="footer.jsp"/>
</body>
</html>

