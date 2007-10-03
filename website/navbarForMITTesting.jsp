<% String username = (String) request.getAttribute("userid"); %>
<!-- top band with the logo -->
<div id="topband" class="topband">
  <a href="index.jsp"target="_top">
    <img src="<%=request.getContextPath()%>/images/GP-logo.gif" alt="GenePattern Portal" width="229" height="48" border="0" />
  </a>
</div>


<!-- horizontal navigation band -->
    <script language="JavaScript1.2" type="text/javascript">
    	var agt = navigator.userAgent.toLowerCase();
    	var isSafari = agt.indexOf("safari") != -1;
    	var x = isSafari ? -90 : 0;
    	var y =  isSafari ? 10 : 18;
    	mmLoadMenus();
    </script>
    <div id="navband1" class="navband1" style="cursor: pointer;">
        <nobr>
       <a name="link17" id="link6"
           href="<%=request.getContextPath()%>/pages/index.jsf"
           onclick="MM_showMenu(window.mm_menu_tasks,x,y,null,'link17')"
           onmouseover="MM_showMenu(window.mm_menu_tasks,x,y,null,'link17')"
           onmouseout="MM_startTimeout();">Modules &amp; Pipelines</a> &#160;&#160;&#160;&#160;&#160;&#160;
        <a name="link14" id="link9"
           href="<%=request.getContextPath()%>/pages/manageSuite.jsf"
           onclick="MM_showMenu(window.mm_menu_suites,x,y,null,'link14')"
           onmouseover="MM_showMenu(window.mm_menu_suites,x,y,null,'link14')"
           onmouseout="MM_startTimeout();">Suites</a> &#160;&#160;&#160;&#160;&#160;&#160;
        <a name="link15" id="link10"
           href="<%=request.getContextPath()%>/pages/jobResults.jsf"
           onclick="MM_showMenu(window.mm_menu_jobResults,x,y,null,'link15')"
           onmouseover="MM_showMenu(window.mm_menu_jobResults,x,y,null,'link15')"
           onmouseout="MM_startTimeout();">Job Results</a> &#160;&#160;&#160;&#160;&#160;&#160;
        <a name="link12" id="link3"
           href="<%=request.getContextPath()%>/pages/index.jsf?splash=resources"
           onclick="MM_showMenu(window.mm_menu_resources,x,y,null,'link12')"
           onmouseover="MM_showMenu(window.mm_menu_resources,x,y,null,'link12')"
           onmouseout="MM_startTimeout();">Resources</a> &#160;&#160;&#160;&#160;&#160;&#160;
        <a name="link2" id="link5"
           href="<%=request.getContextPath()%>/pages/index.jsf?splash=downloads"
           onclick="MM_showMenu(window.mm_menu_downloads,x,y,null,'link2')"
           onmouseover="MM_showMenu(window.mm_menu_downloads,x,y,null,'link2')"
           onmouseout="MM_startTimeout();">Downloads</a> &#160;&#160;&#160;&#160;&#160;&#160;
        <% if(org.genepattern.server.webapp.jsf.AuthorizationHelper.adminServer(username)) { %>
        	<a name="link13" id="link4"
        	    href="<%=request.getContextPath()%>/pages/serverSettings.jsf"
           		onclick="MM_showMenu(window.mm_menu_administration,x,y,null,'link13')"
           		onmouseover="MM_showMenu(window.mm_menu_administration,x,y,null,'link13')"
           		onmouseout="MM_startTimeout();">Administration</a>&#160;&#160;&#160;&#160;&#160;&#160;
		<% } %>
        <a name="link11" id="link1"
           href="<%=request.getContextPath()%>/pages/index.jsf"
           onclick="MM_showMenu(window.mm_menu_documentation,x,y,null,'link11')"
           onmouseover="MM_showMenu(window.mm_menu_documentation,x,y,null,'link11')"
           onmouseout="MM_startTimeout();">Help</a>
        </nobr>
   </div>

<!-- begin content area. this area contains three columns in an adjustable table, including tasks & pipeline, the center working space, and recent jobs. -->
<div id="content" class="content">
<table width="100%" border="0" cellspacing="0" cellpadding="0">
	<tr>

	</tr>

<!-- XXXXXXXXXXXXX ADDITIONS FOR MIT XXXXXXXXXXXXXX-->
<tr>
<td colspan="4">
<c:if test="#{requestScope.userID != null}">

<table><tr><td>

<table>
<tr><td><a name="link17a" id="link6a"
           href="<%=request.getContextPath()%>/pages/index.jsf"><b>Modules &amp; Pipelines</b></a>
</td></tr>
<tr><td><a href="<%=request.getContextPath()%>/pipelineDesigner.jsp">New Pipeline</a></td></tr>
<tr><td><a href="<%=request.getContextPath()%>/addTask.jsp">New Module</a></td></tr>
<tr><td><a href="<%=request.getContextPath()%>/pages/taskCatalog.jsf">Install from repos</a></td></tr>
<tr><td><a href="<%=request.getContextPath()%>/pages/importTask.jsf">Install from zip</a></td></tr>
<tr><td><a href="<%=request.getContextPath()%>/pages/manageTasks.jsf">Manage</a></td></tr>

</table> <!-- end of modules menu -->

<td><table><tr><td>
		<a name="link14a" id="link9a"
           href="<%=request.getContextPath()%>/pages/manageSuite.jsf"><b>Suites</b></a>
</td></tr>
<tr><td><a href="<%=request.getContextPath()%>/pages/createSuite.jsf">New Suite</a></td></tr>
<tr><td><a href="<%=request.getContextPath()%>/pages/suiteCatalog.jsf">Install from repos</a></td></tr>
<tr><td><a href="<%=request.getContextPath()%>/pages/importTask.jsf?suite=1">Install from zip</a></td></tr>
<tr><td><a href="<%=request.getContextPath()%>/pages/manageSuite.jsf">Manage</a></td></tr>

</table></td><!-- end of SUITE menu -->

<td><table><tr><td>
        <a name="link15a" id="link10a" href="<%=request.getContextPath()%>/pages/jobResults.jsf">
           <b>Job Results Menu</b></a>
</td></tr>
<tr><td>
        <a name="link15a" id="link10a" href="<%=request.getContextPath()%>/pages/jobResults.jsf">
           Results Summary</a>
</td></tr>

</table></td><!-- end of JOB menu -->


<td><table><tr><td>
        <a name="link12a" id="link3a"
           href="<%=request.getContextPath()%>/pages/index.jsf?splash=resources"><b>Resources Menu</b></a>
</td></tr>
<tr><td><a href="http://www.broad.mit.edu/cancer/software/genepattern/gp_mail.html">Mailing List</a></td></tr>
<tr><td><a href="<%=request.getContextPath()%>/pages/contactUs.jsf">Report Bugs</a></td></tr>
<tr><td><a href="http://www.broad.mit.edu/cancer/software/genepattern/forum/">User Forum</a></td></tr>



</table></td><!-- end of RESOURCES menu -->

<td><table><tr><td>

        	<a name="link13a" id="link4a"
        	    href="<%=request.getContextPath()%>/pages/index.jsf?splash=downloads"><b>Downloads Menu</b></a>
</td></tr>
<tr><td><a href="http://www.broad.mit.edu/cancer/software/genepattern/download/gpge/3.0/install.htm">Install desktop client</a></td></tr>
<tr><td><a href="<%=request.getContextPath()%>/pages/downloadProgrammingLibaries.jsf">Programming libraries</a></td></tr>
<tr><td><a href="http://www.broad.mit.edu/cancer/software/genepattern/datasets/">Public datasets</a></td></tr>


</table></td><!-- end of DOWNLOADS menu -->




        <c:if test="#{org.genepattern.server.webapp.jsf.AuthorizationHelper.adminServer(username)}">
<td><table><tr><td>

        	<a name="link13a" id="link4a"
        	    href="<%=request.getContextPath()%>/pages/serverSettings.jsf"><b>Administration Menu</b></a>
</td></tr>
<tr><td><a href="<%=request.getContextPath()%>/pages/serverSettings.jsf">Server settings</a></td></tr>


</table></td>


          </c:if>
<td><table><tr><td>
        <a name="link2a" id="link5a"
           href="<%=request.getContextPath()%>/pages/index.jsf?splash=downloads"><b>Help Menu</b></a>
</td></tr>
<tr><td><a href="http://www.broad.mit.edu/cancer/software/genepattern/tutorial/gp_tutorial.html">Tutorial</a></td></tr>
<tr><td><a href="http://www.broad.mit.edu/cancer/software/genepattern/tutorial/gp_java_client.html">Desktop Client Guide</a></td></tr>
<tr><td><a href="http://www.broad.mit.edu/cancer/software/genepattern/tutorial/gp_web_client.html">Web Client Guide</a></td></tr>
<tr><td><a href="http://www.broad.mit.edu/cancer/software/genepattern/tutorial/gp_programmer.html">Programmers Guide</a></td></tr>
<tr><td><a href="<%=request.getContextPath()%>/getTaskDoc.jsp">Module Documentation</a></td></tr>
<tr><td><a href="http://www.broad.mit.edu/cancer/software/genepattern/tutorial/gp_fileformats.html">File Formats</a></td></tr>
<tr><td><a href="http://www.broad.mit.edu/cancer/software/genepattern/doc/relnotes/current/">Release Notes</a></td></tr>
<tr><td><a href="http://www.broad.mit.edu/cancer/software/genepattern/doc/faq/">FAQ</a></td></tr>





</table></td>



</td></tr></table>

</c:if>

    </td>
</tr>

<!-- XXXXXXXXXXXXX END OF ADDITIONS FOR MIT XXXXXXXXXXXXXX-->




	<!-- main content area.  -->
	<tr>
		<td valign="top" class="maincontent" id="maincontent">
