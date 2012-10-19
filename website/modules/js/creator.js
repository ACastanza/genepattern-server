/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2012) by the
 Broad Institute. All rights are reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. The Broad Institute cannot be responsible for its
 use, misuse, or functionality.
*/

var mainLayout;
var run = false;
var dirty = false;
var saving = false;

var module_editor = {
    lsid: "",
    uploadedfiles: [],
    supportfileinputs: [],
    licensefile: "",
    filesToDelete: [],
    otherModAttrs: {}
};

var Request = {
 	parameter: function(name) {
 		return this.parameters()[name];
 	},

 	parameters: function() {
 		var result = {};
 		var url = window.location.href;
 		var parameters = url.slice(url.indexOf('?') + 1).split('&');

 		for(var i = 0;  i < parameters.length; i++) {
 			var parameter = parameters[i].split('=');
 			result[parameter[0]] = parameter[1];
 		}
 		return result;
 	}
};

//For those browsers that dont have it so at least they won't crash.
if (!window.console)
{
    window.console = { time:function(){}, timeEnd:function(){}, group:function(){}, groupEnd:function(){}, log:function(){} };
}

function trim(s)
{
	var l=0; var r=s.length -1;
	while(l < s.length && s[l] == ' ')
	{	l++; }
	while(r > l && s[r] == ' ')
	{	r-=1;	}
	return s.substring(l, r+1);
}

function setDirty(value)
{
    dirty = value;

    //if page is not already marked as dirty
    if(dirty)
    {
        $(window).bind('beforeunload', function()
        {
            return 'If you leave this page all module changes will be lost.';
        });
    }
    else
    {
        $(window).unbind('beforeunload');
    }
}

function isDirty()
{
    return dirty;
}

function bytesToSize(bytes, precision)
{
    var kilobyte = 1024;
    var megabyte = kilobyte * 1024;
    var gigabyte = megabyte * 1024;
    var terabyte = gigabyte * 1024;

    if ((bytes >= 0) && (bytes < kilobyte)) {
        return bytes + ' B';

    } else if ((bytes >= kilobyte) && (bytes < megabyte)) {
        return (bytes / kilobyte).toFixed(precision) + ' KB';

    } else if ((bytes >= megabyte) && (bytes < gigabyte)) {
        return (bytes / megabyte).toFixed(precision) + ' MB';

    } else if ((bytes >= gigabyte) && (bytes < terabyte)) {
        return (bytes / gigabyte).toFixed(precision) + ' GB';

    } else if (bytes >= terabyte) {
        return (bytes / terabyte).toFixed(precision) + ' TB';

    } else {
        return bytes + ' B';
    }
}


/*function bytesToSize(bytes, precision) {
    var sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    var posttxt = 0;
    if (bytes == 0)
    {
        return 'n/a';
    }

    if (bytes < 1024)
    {
        return Number(bytes) + " " + sizes[posttxt];
    }

    while( bytes >= 1024 ) {
        posttxt++;
        bytes = bytes / 1024;
    }
    return Number(bytes).toFixed(precision) + " " + sizes[posttxt];
} */

function abortSave(errorMessage)
{
    $("#savingDialog").dialog("destroy");
    $("#savedDialog").append(errorMessage);
    $("#savedDialog").dialog({
        resizable: false,
        width: 400,
        height:130,
        modal: true,
        title: "Module Save Error",
        buttons: {
            OK: function() {
                $( this ).dialog( "close" );
                throw(errorMessage);
            }
        }
    });
}

function updateModuleVersions(lsids)
{
    if(lsids == undefined || lsids == null)
    {
        return;
    }

    var currentVersion = $('select[name="modversion"]').val();
    $('select[name="modversion"]').children().remove();
    var modVersionLsidList = lsids;
    for(v =0;v<modVersionLsidList.length;v++)
    {
        var versionnum = modVersionLsidList[v];
        var index = versionnum.lastIndexOf(":");
        if(index == -1)
        {
            alert("An error occurred while loading module versions.\nInvalid lsid: " + moduleVersionLsidList[v]);
        }
        var version = versionnum.substring(index+1, versionnum.length);
        var modversion = "<option value='" + versionnum + "'>" + version + "</option>";
        $('select[name="modversion"]').append(modversion);
        $('select[name="modversion"]').multiselect("refresh");
    }

    $('select[name="modversion"]').change(function()
    {
        var editLocation = "creator.jsf?lsid=" + $(this).val();
        window.open(editLocation, '_self');
    });

    $('select[name="modversion"]').val(currentVersion);
    $('select[name="modversion"]').multiselect("refresh");
}

function runModule(lsid)
{
    window.open("/gp/pages/index.jsf?lsid=" + lsid, '_self');
}

function saveModule()
{
    var modname = $('#modtitle').val();
    if(modname == undefined || modname == null || modname.length < 1)
    {
        abortSave("A module name must be specified");
        throw("A module name must be specified");
    }

    var description = $('textarea[name="description"]').val();
    var author = $('input[name="author"]').val();
    var organization = $('input[name="organization"]').val();

    if(organization !== undefined && organization !== "")
    {
        author = author + ";" + organization;
    }

    var privacy = $('select[name="privacy"] option:selected').val();
    var quality = $('select[name="quality"] option:selected').val();
    var language = $('select[name="language"] option:selected').val();
    var lang_version = $('input[name="lang_version"]').val();
    var os = $('input[name=os]:checked').val();
    var tasktype = $("select[name='category'] option:selected").val();
    var cpu = $("select[name='cpu'] option:selected").val();
    var commandLine = $('textarea[name="cmdtext"]').val();
    var fileFormats = $('select[name="mod_fileformat"]').val();

    if(fileFormats == null)
    {
        fileFormats = [];
    }

    if(commandLine == undefined || commandLine == null || commandLine.length < 1)
    {
        abortSave("A command line must be specified");
        throw("A command line must be specified");
    }

    var licenseFile = $('input[name="licensefile"]').val();
    if(licenseFile == null || licenseFile == undefined)
    {
        licenseFile = "";
    }

    var lsid = module_editor.lsid;
    var supportFiles = module_editor.uploadedfiles;
    var version = $('input[name="comment"]').val();
    var filesToDelete = module_editor.filesToDelete;

    var json = {};
    json["module"] = {"name": modname, "description": description,
        "author": author, "privacy": privacy, "quality": quality,
        "language": language, "JVMLevel": lang_version, "cpuType": cpu, "taskType": tasktype, "version": version,
        "os": os, "commandLine": commandLine, "LSID": lsid, "supportFiles": supportFiles,
        "filesToDelete": filesToDelete, "fileFormat": fileFormats, "license":licenseFile};

    //add other remaining attributes
    var keys = Object.keys(module_editor.otherModAttrs);
    for(k =0; k < keys.length; k++)
    {
        var keyName = keys[k];
        console.log("\nsaving other module attributes: " + keys[k] + "=" + module_editor.otherModAttrs[keyName]);
        json.module[keyName] = module_editor.otherModAttrs[keyName];
    }

    json["parameters"] = getParametersJSON();

    $.ajax({
        type: "POST",
        url: "/gp/ModuleCreator/save",
        data: { "bundle" : JSON.stringify(json) },
        success: function(response) {
            $("#savingDialog").dialog("destroy");
            //remove wait cursor
            //$("html").removeClass('busy').unbind('click');
            var error = response["ERROR"];
            var newLsid = response["lsid"];
            //get updated module versions
            var versions = response["lsidVersions"];

            if (error !== undefined && error !== null) {
                $("#savedDialog").append(error);
                $("#savedDialog").dialog({
                    resizable: false,
                    width: 400,
                    height:130,
                    modal: true,
                    title: "Module Save Error",
                    buttons: {
                        OK: function() {
                            $( this ).dialog( "close" );
                            throw(error);
                        }
                    }
                });
            }

            setDirty(false);
            updateModuleVersions(versions);

            // Update the LSID upon successful save
            if (newLsid !== undefined && newLsid !== null)
            {
                $("#lsid").empty().append("LSID: " + newLsid);
                var vindex = newLsid.lastIndexOf(":");
                if(vindex != -1)
                {
                    var version = newLsid.substring(vindex+1, newLsid.length);
                    var modtitle = $("#modtitle").val();

                    $("#savedDialog").append(modtitle + " version " + version + " saved");
                    $("#savedDialog").dialog({
                        resizable: false,
                        width: 400,
                        height:130,
                        modal: true,
                        title: "Module Saved",
                        buttons: {
                            OK: function() {
                                $( this ).dialog( "close" );
                                module_editor.lsid = newLsid;
                                module_editor.uploadedfiles = [];

                                var unversioned = $(' select[name="modversion"] option[value="unversioned"]');
                                if(unversioned != undefined && unversioned != null)
                                {
                                    unversioned.remove();
                                }

                                $('select[name="modversion"]').val(newLsid);
                                if($('select[name="modversion"]').val() != newLsid)
                                {
                                    var modversion = "<option value='" + newLsid + "'>" + version + "</option>";
                                    $('select[name="modversion"]').append(modversion);
                                    $('select[name="modversion"]').val(version);
                                }

                                $('select[name="modversion"]').multiselect("refresh");

                                saving = false;
                                if(run)
                                {
                                    runModule(newLsid);
                                }
                                else
                                {
                                    //reload the editor page using the new LSID
                                    var editLocation = "creator.jsf?lsid=" + newLsid;
                                    window.open(editLocation, '_self');;
                                }
                            }
                        }
                    });
                }
            }
        },
        dataType: "json"
    });

    module_editor.filesToDelete = [];
}

function editModule()
{
    var lsid = Request.parameter('lsid');

    if(lsid !== undefined && lsid !== null && lsid.length > 0)
    {
        lsid = lsid.replace(/#/g, "");
        loadModule(lsid);
    }
}

function addparameter()
{
    var paramDiv = $("<div class='parameter'>  \
        <table class='deloptions'>\
        <tr><td class='btntd'>\
        <button class='delparam'>x Delete</button></td><td>\
        <p>Name*: <br/>\
        <input type='text' name='p_name' size='28'/>\
        <input type='checkbox' name='p_optional' size='25'/>Optional</p><p>\
        Description:<br/>\
        <textarea cols='60' name='p_description' rows='2'></textarea></p>\
        </td><td class='lasttd'>\
        <table class='pmoptions'><tr><td>Default Value:</td><td><input type='text' name='p_defaultvalue' size='16'/><br/>\
        </td></tr>\
        <tr><td>Flag:</td><td><input type='text' name='p_flag' size='7'/>\
        <input type='checkbox' name='p_prefix' size='7' disabled='disabled'></input> prefix when specified \
        </td> \
        </tr>\
        <tr><td>Type*:</td><td><select name='p_type'>\
                       <option value='text'>Text</option> \
                       <option value='Input File'>Input File</option>\
                       <option value='Choice'>Choice</option> \
                       <option value='Integer'>Integer</option>  \
                       <option value='Floating Point'>Floating Point</option>  \
                       <option value='Directory'>Directory</option>\
                       <option value='Password'>Password</option> \
                   </select>\
        </td></tr></table>\
        </td></tr>\
        </table>\
    </div>");

    paramDiv.find("select[name='p_type']").multiselect({
        multiple: false,
        header: false,
        noneSelectedText: "",
        selectedList: 1,
        position: {
          my: 'left bottom',
          at: 'left top'
        }
    });

    $('#parameters').append(paramDiv);

    paramDiv.find(".delparam").button().click(function()
    {
        //first remove the parameter from the commandline
        var pelement = $(this).parent().parent().find("input[name='p_name']");

        if(!confirm("Are you sure you want to delete this parameter?"))
        {
            return;
        }

        var felement = $(this).parent().parent().find("input[name='p_flag']");
        pelement.val("");
        felement.val("");

        updateparameter($(this).parent().parent());

        $(this).parents("div:first").remove();
    });

    $("select[name='p_type']").live("change", function()
    {
        var tSelect = $(this);

        changeParameterType(tSelect);
    });

    return paramDiv;
}

function addtocommandline(flag, name, prevflag, prevname)
{
    var text = "";

    if (flag == "" && name == "" && prevflag ==undefined && prevname == undefined)
    {
        return;
    }

    //construct new parameter value
    if(name !== "")
    {
        text = "&lt;" + name + "&gt;";
    }

    if(flag !== "")
    {
        text = flag + text;
    }

    var item = $("<li class='commanditem'>" + text + "</li>");

    //construct prev parameter value
    var  prevtext = "";
    if(prevname !== "")
    {
        prevtext = "&lt;" + prevname + "&gt;";
    }

    if(prevflag !== "")
    {
        prevtext = prevflag + prevtext;
    }

    //if no change in value do nothing
    if(prevtext == text)
    {
        return;
    }

    //look for child with matching old parameter value and replace with new parameter value
    var found = false;

    var cmdline= $("#commandtextarea textarea").val();
    var decodedPrevText = $('<div/>').html(prevtext).text();

    var decodedText = $('<div/>').html(text).text();

    $('#commandlist').children().each(function()
    {
        //decode the prevtext string first and compare it
        if($(this).text() ==  decodedPrevText)
        {
            if(text !== "")
            {
                $(this).replaceWith(item);
            }
            else
            {
                $(this).remove();
            }
            found = true;
            cmdline = cmdline.replace(decodedPrevText, decodedText);
            $("#commandtextarea textarea").val(cmdline);

        }
    });

    // if old parameter value was not found then this must be a new parameter so
    // insert it into parameter list
    if(text !== "")
    {
        $('#commandlist').append(item);

        //if argument is already in command which will occur if this is
        // a module edit
        if(cmdline.indexOf(decodedText) == -1)
        {
            cmdline += " " + decodedText;
            $("#commandtextarea textarea").val(cmdline);
        }
    }
}

//update the specific parameter div
function updateparameter(parameter, updateCmdLine)
{
    if(typeof(updateCmdLine)==='undefined') updateCmdLine = true;
    var pelement = parameter.find("input[name='p_name']");
    var felement = parameter.find("input[name='p_flag']");

    var pelementval = pelement.val().replace(/ /g, ".");
    pelement.val(pelementval);

    var pname_newval = pelement.val();
    var pflag_newval = felement.val();
    if(parameter.find("input[name='p_prefix']").attr('checked'))
    {
        pflag_newval = "";
    }

    var pname_oldval = pelement.data('oldVal');
    var pflag_oldval = felement.data('oldVal');

    pelement.data('oldVal',  pname_newval );
    felement.data('oldVal',  pflag_newval );

    var paramExists = true;
    //check if parameter exists
    $("input[name='p_name']").each(function()
    {
        if($(this).val() == pname_newval)
        {
            paramExists = !paramExists;
        }
    });

    if(paramExists)
    {
        alert("The parameter: " + pname_newval + " already exists.");
        pelement.val("");
        return;
    }

    //do not update the command line
    if(updateCmdLine)
    {
        addtocommandline(pflag_newval, pname_newval, pflag_oldval, pname_oldval);
    }
    else
    {
        //add any new parameter to the command line argument listing
        if(pname_oldval == undefined && pflag_oldval == undefined)
        {
            var text = "&lt;" + pname_newval + "&gt;";
            text = pflag_newval + text;

            var decodedText = $('<div/>').html(text).text();
            var cmdline= $("#commandtextarea textarea").val();


            if(cmdline.indexOf(decodedText) != -1)
            {
                var item = $("<li class='commanditem'>" + text + "</li>");

                $('#commandlist').append(item);
            }
        }
    }
}

function changeParameterType(element)
{
    var value = element.val();

    if(element.data("editing") !== value)
    {
        if(!element.parent().next().children().is("input[name='p_prefix']"))
        {
            element.parent().next().remove();
        }

        if(value == "Choice")
        {
            //added as a workaround - check for duplicate calls to this function
            var editChoiceList = $("<td><input type='text' name='choicelist' size='40'/></td>");
            var editChoiceLink = $("<a href='#'>edit choice</a>");

            editChoiceLink.click(function()
            {
                var choices = $(this).prev().val();

                $( "#editchoicedialog" ).dialog({
                    autoOpen: true,
                    height: 500,
                    width: 360,
                    create: function()
                    {
                        var table = $("<table>\
                            <tr>  \
                                <th>Value<br/>(required)</th> \
                                <th>Display Value<br/>(optional)</th> \
                            </tr>  \
                        </table>");

                        $(this).append(table);

                        table.find("tbody").sortable();

                        var result = choices.split(';');
                        if(result == null || result == "" || result.length < 0)
                        {
                            //start with two rows of data
                            var trowdef = $("<tr><td> <input type='text' name='choicev' size='15'/> </td>" +
                                 "<td> <input type='text' name='choicen' size='15'/> </td>" +
                                 "<td> <button> X </button></td></tr>");

                            trowdef.find("button").button().click(function()
                            {
                                    $(this).parent().parent().remove();
                            });

                            var trowdef2 = $("<tr><td> <input type='text' name='choicev' size='15'/> </td>" +
                                 "<td> <input type='text' name='choicen' size='15'/> </td>" +
                                 "<td> <button> X </button></td></tr>");

                            trowdef2.find("button").button().click(function()
                            {
                                    $(this).parent().parent().remove();
                            });

                            $(this).find("table").append(trowdef);
                            $(this).find("table").append(trowdef2);
                            return;
                        }

                        for(i=0;i<result.length;i++)
                        {
                            var rowdata = result[i].split("=");

                            var displayValue = "";
                            var value = "";
                            if(rowdata.length > 1)
                            {
                                displayValue = rowdata[1];
                                value = rowdata[0];
                            }
                            else
                            {
                                value = rowdata[0];
                            }

                            var trow = $("<tr><td> <input type='text' name='choicev' size='15' value='"
                                        + value +"'/> </td>" +
                                    "<td> <input type='text' name='choicen' size='15'value='"
                                     + displayValue + "'/> </td>" +
                                    "<td> <button> X </button></td></tr>");

                            trow.find("button").button().click(function()
                            {
                                $(this).parent().parent().remove();
                            });

                            $(this).find("table").append(trow);
                        }
                    },
                    close: function()
                    {
                        $( this ).dialog( "destroy" );
                        $("#editchoicedialog").find( "table" ).remove();
                    },
                    buttons: {
                            "OK": function() {
                                var choicelist = "";
                                $(this).find("tr").each(function()
                                {
                                    var dvalue = $(this).find("td input[name='choicen']").val();
                                    var value = $(this).find("td input[name='choicev']").val();

                                    if((dvalue == undefined && value == undefined)
                                       || (dvalue == "" && value==""))
                                    {
                                        return;
                                    }

                                    if(choicelist !== "")
                                    {
                                        choicelist += ";";
                                    }

                                    if(dvalue == undefined || dvalue == null|| dvalue == "")
                                    {
                                        choicelist += value;
                                    }
                                    else
                                    {
                                        choicelist += value + "=" + dvalue;
                                    }
                                });

                                $("#editchoicedialog").dialog("destroy");
                                $("#editchoicedialog").find( "table" ).remove();

                                element.parent().parent().find("input[name='choicelist']").each(function()
                                {
                                    $(this).val(choicelist);
                                    $(this).data('prevVal', choicelist);
                                    element.data('editing', "Choice");

                                    //validate if default value is valid
                                    var parent = $(this).closest(".parameter");
                                    var defaultValueObj = parent.find("input[name='p_defaultvalue']");
                                    validateDefaultChoiceValue(defaultValueObj);
                                });
                            },
                            "Cancel": function() {
                                var choiceListElement = $("input[name='choicelist']");
                                choiceListElement.parent().next().data('editing', false);
                                $( this ).dialog( "destroy" );
                                $("#editchoicedialog").find( "table" ).remove();
                            }
                    },
                    resizable: true
                });
            });

            element.data("editing", "Choice");

            element.parent().after(editChoiceList);
            editChoiceList.append(editChoiceLink);

            $("input[name='choicelist']").change(function()
            {
               var prevVal = $(this).data("prevVal");
               var curVal = $(this).val();
               if(prevVal !== curVal)
               {
                   $(this).val(prevVal);
                   alert("The list of choices cannot be edited from here. Please use the edit choice link.");
               }
            });

        }
        else if(value == "Input File")
        {
            var fileFormatList = $('<select multiple="multiple" name="fileformat"></select>');
            var fileFormatButton = $('<button id="addinputfileformat">New</button>');

            fileFormatButton.button().click(function()
            {
                $( "#addfileformatdialog" ).dialog("open");
            });

            console.log("Found file formats: " + $('select[name="mod_fileformat"]').children("option").text());
            //copy option values from the modules output file format list that was generated earlier
            $('select[name="mod_fileformat"]').children("option").each(function()
            {
                fileFormatList.append("<option>" + $(this).val() + "</option>");
            });

            var fileFormatTD = $("<td></td>");
            element.parent().after(fileFormatTD);

            //hide TD so that default select appearance does not appear in UI
            fileFormatTD.hide();
            fileFormatTD.append(fileFormatList);
            fileFormatTD.append(fileFormatButton);
            fileFormatTD.show();
            fileFormatList.multiselect({
                header: false,
                noneSelectedText: "Specify input file formats",
                selectedList: 4, // 0-based index
                position:
                {
                    my: 'left bottom',
                    at: 'left top'
                }
            });

            element.data("editing", "Input File");
        }
        else
        {
            element.data('editing', "");
        }
    }
}

function updatemodulecategories()
{
    $.ajax({
        type: "POST",
        url: "/gp/ModuleCreator/categories",
        success: function(response) {
            var error = response["ERROR"];
            if (error !== undefined) {
                alert(error);
            }
            else {
                var categories = response["categories"];
                categories = categories.substring(1, categories.length-1);

                var result = categories.split(", ");
                var mcat = $("select[name='category']");

                for(i=0;i < result.length;i++)
                {
                    mcat.append($("<option>" + result[i] + "</option>"));
                }
                mcat.multiselect("refresh");
            }
        },
        dataType: "json"
    });
}

function updatefileformats()
{
    $.ajax({
        type: "POST",
        url: "/gp/ModuleCreator/fileformats",
        success: function(response) {
            var error = response["ERROR"];
            if (error !== undefined) {
                alert(error);
            }
            else {
                var fileformats = response["fileformats"];

                var unique_fileformatlist = [];

                for(m=0;m < fileformats.length;m++)
                {
                    var index = unique_fileformatlist.indexOf(fileformats[m]);

                    if(index == -1)
                    {
                        unique_fileformatlist.push(fileformats[m]);
                    }
                }

                var result = unique_fileformatlist;

                var mcat = $("select[name='mod_fileformat']");

                for(i=0;i < result.length;i++)
                {
                    mcat.append($("<option>" + result[i] + "</option>"));
                }

                if(mcat.data("values") != undefined && mcat.data("values") != null)
                {
                    mcat.val(mcat.data("values"));
                }
                mcat.multiselect('refresh');

                $("select[name='fileformat']").each(function()
                {
                    console.log("Adding loaded file formats to parameters");
                    var fileformat = $(this);
                    $('select[name="mod_fileformat"]').children("option").each(function()
                    {
                        fileformat.append("<option>" + $(this).val() + "</option>");
                    });

                    if(fileformat.data("fileformats") != null && fileformat.data("fileformats") != "")
                    {
                        fileformat.val(fileformat.data("fileformats"));
                    }

                    fileformat.multiselect("refresh");
                });
            }
        },
        dataType: "json"
    });
}

function addsectioncollapseimages()
{
    var imagecollapse = $("<img class='imgcollapse' src='styles/images/section_collapsearrow.png' alt='some_text' width='11' height='11'/>");
    var imageexpand = $("<img class='imgexpand' src='styles/images/section_expandarrow.png' alt='some_text' width='11' height='11'/>");

    $(".heading").prepend(imageexpand);
    $(".heading").prepend(imagecollapse);

    $(".heading").children(".imgcollapse").toggle();

    $(".heading").next(".hcontent").data("visible", true);
}


function htmlEncode(value)
{
  return $('<div/>').text(value).html();
}

function loadModuleInfo(module)
{
    module_editor.lsid = module["LSID"];
    $("#lsid").empty().append("LSID: " + module_editor.lsid);

    if(module["name"] !== undefined)
    {
        $('#modtitle').val(module["name"]);
    }

    if(module["lsidVersions"] !== undefined)
    {
        updateModuleVersions(module["lsidVersions"]);
        $('select[name="modversion"]').val(module["LSID"]);
        $('select[name="modversion"]').multiselect("refresh");
    }

    if(module["description"] !== undefined)
    {
        $('textarea[name="description"]').val(module["description"]);
    }

    if(module["author"] !== undefined)
    {
        var author = module["author"];
        if(author.indexOf(";") != -1)
        {
            var results = author.split(";");
            author = results[0];
            var organization = results[1];

            $('input[name="author"]').val(author);
            $('input[name="organization"]').val(organization);
        }
        else
        {
            $('input[name="author"]').val(module["author"]);
        }
    }

    if(module["privacy"] !== undefined)
    {
        $('select[name="privacy"]').val(module["privacy"]);
        $('select[name="privacy"]').multiselect("refresh");
    }

    if(module["quality"] !== undefined)
    {
        $('select[name="quality"]').val(module["quality"]);
        $('select[name="quality"]').multiselect("refresh");
    }

    if(module["version"] !== undefined)
    {
        $('input[name="comment"]').val(module["version"]);
    }

    if(module["language"] !== undefined)
    {
        $('select[name="language"]').val(module["language"]);
        $('select[name="language"]').multiselect("refresh");

        if($('select[name="language"]').val() == null)
        {
            $('select[name="language"]').val("any");
            $('select[name="language"]').multiselect("refresh");
        }

        if(module["language"] == "Java")
        {
            $("select[name='c_type']").val(module["language"]);
            $("select[name='c_type']").multiselect("refresh");
            $("#commandtextarea textarea").data("type", "<java>");
        }
        if(module["language"] == "Perl")
        {
            $("select[name='c_type']").val(module["language"]);
            $("select[name='c_type']").multiselect("refresh");
            $("#commandtextarea textarea").data("type", "<perl>");
        }
    }

    if(module["os"] !== undefined)
    {
        $('input[name=os]').val(module["os"]);
    }

    if(module["taskType"] !== undefined)
    {
        $("select[name='category']").val(module["taskType"]);
        $("select[name='category']").multiselect("refresh");
    }

    if(module["cpuType"] !== undefined)
    {
        $("select[name='cpu']").val(module["cpuType"]);
        $("select[name='cpu']").multiselect("refresh");
    }

    if(module["commandLine"] !== undefined)
    {
        $('textarea[name="cmdtext"]').val(module["commandLine"]);

        var cmdtype = $("select[name='c_type']").val();
        if(cmdtype === "Custom" || cmdtype == null)
        {
            if(module["commandLine"].indexOf("<java>") != -1 &&
                    module["commandLine"].indexOf("<java>") < 1)
            {
                $("select[name='c_type']").val("Java");
                $("select[name='c_type']").multiselect("refresh");
                $("#commandtextarea textarea").data("type", "<java>");
            }
            if(module["commandLine"].indexOf("<perl>") != -1 &&
                    module["commandLine"].indexOf("<perl>") < 1)
            {
                $("select[name='c_type']").val("Perl");
                $("select[name='c_type']").multiselect("refresh");
                $("#commandtextarea textarea").data("type", "<perl>");
            }
        }
    }

    if(module["fileFormat"] !== undefined)
    {
        var fileformats = module["fileFormat"];
        fileformats = fileformats.split(";");
        $("select[name='mod_fileformat']").data("values", fileformats);
        $("select[name='mod_fileformat']").val(fileformats);
        $("select[name='mod_fileformat']").multiselect("refresh");
    }

    if(module["license"] !== undefined && module["license"] !== "")
    {
        var license = module["license"];
        var licenseFileURL = "<a href=\"/gp/getFile.jsp?task=" + module_editor.lsid + "&file=" + encodeURI(license) + "\" target=\"new\">" + htmlEncode(license) + "</a> ";

        $("#licenseDiv").empty();
        $("#licenseDiv").append(licenseFileURL);

    }

    //store remaining task info attributes
    var keys = Object.keys(module);
    for(k =0; k < keys.length; k++)
    {
        console.log("\nkeys: " + keys[k]);
        var keyName = keys[k];
        if(keyName != "fileFormat" && keyName != "commandLine" && keyName != "description"
                && keyName != "os" && keyName != "name" && keyName != "author" && keyName != "JVMLevel"
                && keyName != "LSID" && keyName != "lsidVersions" && keyName != "cpuType"
                && keyName != "privacy" && keyName != "language" && keyName != "version"
                && keyName != "supportFiles" && keyName != "taskType"
                && keyName != "quality" && keyName != "license")
        {
            module_editor.otherModAttrs[keyName] = module[keyName];
        }
    }

    var supportFilesList = module["supportFiles"];
    if(supportFilesList !== undefined && supportFilesList != null &&  supportFilesList != "")
    {
        var currentFilesDiv = $("<div id=currentfiles><div>");
        currentFilesDiv.append("Current Files (Check to delete): ");

        $("#supportfilecontent").prepend(currentFilesDiv);

        // var currentFilesSelect = $("<select name='currentfiles' multiple='multiple'></select>");
        supportFilesList = supportFilesList.split(";");
        for(s=0;s<supportFilesList.length;s++)
        {
            //do not show the module manifest in the list of support files
            if(supportFilesList[s] == "manifest")
            {
                continue;
            }
            var checkbox = $('<input type="checkbox" name="currentfiles" value="' +
            supportFilesList[s] + '" />').click(function()
            {
                var selectedVal = $(this).val();

                if($(this).is(':checked'))
                {
                    module_editor.filesToDelete.push(selectedVal);
                }
                else
                {   //remove from delete file list
                    var index = jQuery.inArray(selectedVal, module_editor.filesToDelete);
                    module_editor.filesToDelete.splice(index,1);
                }
            });

            currentFilesDiv.append(checkbox);

            var currentFileURL = "<a href=\"/gp/getFile.jsp?task=" + module_editor.lsid + "&file=" + encodeURI(supportFilesList[s]) + "\" target=\"new\">" + htmlEncode(supportFilesList[s]) + "</a> ";
            currentFilesDiv.append(currentFileURL);
        }

        currentFilesDiv.append("<br>");

        currentFilesDiv.append("<br><br>");
    }
}

function loadParameterInfo(parameters)
{
    for(i=0; i < parameters.length;i++)
    {
        var newParameter = addparameter();
        newParameter.find("input[name='p_name']").val(parameters[i].name);
        newParameter.find("textarea[name='p_description']").val(parameters[i].description);
        newParameter.find("input[name='p_defaultvalue']").val(parameters[i].default_value);
        var optional = parameters[i].optional;
        var prefix = parameters[i].prefix;

        if(parameters[i].flag !== undefined && parameters[i].flag !== null)
        {
            newParameter.find("input[name='p_flag']").val(parameters[i].flag);
            if(newParameter.find("input[name='p_flag']").val() != "")
            {
                newParameter.find("input[name='p_prefix']").removeAttr("disabled");
            }
        }

        if(optional.length > 0)
        {
            newParameter.find("input[name='p_optional']").attr('checked', true);
        }

        if(prefix !== undefined && prefix !== null && prefix.length > 0)
        {
            newParameter.find("input[name='p_prefix']").attr('checked', true);
            newParameter.find("input[name='p_flag']").val(prefix);
        }

        var pfileformat = parameters[i].fileFormat;

        var type = parameters[i].type;

        if(type == "java.io.File")
        {
            newParameter.find("select[name='p_type']").val("Input File");
            newParameter.find("select[name='p_type']").multiselect("refresh");
            changeParameterType(newParameter.find("select[name='p_type']"));
        }

        if(type == "java.lang.Integer")
        {
            newParameter.find("select[name='p_type']").val("Integer");
            newParameter.find("select[name='p_type']").multiselect("refresh");
            changeParameterType(newParameter.find("select[name='p_type']"));
        }
        if(type == "java.lang.Float")
        {
            newParameter.find("select[name='p_type']").val("Floating Point");
            newParameter.find("select[name='p_type']").multiselect("refresh");
            changeParameterType(newParameter.find("select[name='p_type']"));
        }

        if(type == "PASSWORD")
        {
            newParameter.find("select[name='p_type']").val("Password");
            newParameter.find("select[name='p_type']").multiselect("refresh");
            changeParameterType(newParameter.find("select[name='p_type']"));
        }

        if(type == "DIRECTORY")
        {
            newParameter.find("select[name='p_type']").val("Directory");
            newParameter.find("select[name='p_type']").multiselect("refresh");
            changeParameterType(newParameter.find("select[name='p_type']"));
        }

        if(pfileformat !== undefined && pfileformat != null && pfileformat.length > 0)
        {
            newParameter.find("select[name='p_type']").val("Input File");
            newParameter.find("select[name='p_type']").multiselect("refresh");
            changeParameterType(newParameter.find("select[name='p_type']"));

            var pfileformatlist = pfileformat.split(";");
            newParameter.find("select[name='fileformat']").val(pfileformatlist);
            newParameter.find("select[name='fileformat']").data("fileformats", pfileformatlist);
            newParameter.find("select[name='fileformat']").multiselect('refresh');
        }

        var choices = parameters[i].value;
        if(choices !== undefined && choices !== null && choices.length > 0)
        {
            newParameter.find("select[name='p_type']").val("Choice");
            newParameter.find("select[name='p_type']").multiselect("refresh");
            newParameter.find("select[name='p_type']").trigger("change");

            newParameter.find('input[name="choicelist"]').val(choices);
            newParameter.find('input[name="choicelist"]').multiselect("refresh");
            newParameter.find('input[name="choicelist"]').data("prevVal", choices);
        }

        var otherAttrs = {};
        var keys = Object.keys(parameters[i]);
        for(k =0; k < keys.length; k++)
        {
            console.log("\nkeys: " + keys[k]);
            var keyName = keys[k];
            if(keyName != "name" && keyName != "description"
                    && keyName != "flag" && keyName != "fileFormat"
                    && keyName != "default_value" && keyName != "prefix" && keyName != "type"
                    && keyName != "TYPE" && keyName != "MODE" && keyName != "optional" && keyName != "value" && keyName != "prefix_when_specified")
            {
                otherAttrs[keyName] = parameters[i][keyName];
            }
        }

        newParameter.data("otherAttrs", otherAttrs);
        updateparameter(newParameter, false);
    }
}

function loadModule(taskId)
{
     $.ajax({
            type: "POST",
            url: "/gp/ModuleCreator/load",
            data: { "lsid" : taskId },
            success: function(response) {
                var message = response["MESSAGE"];
                var error = response["ERROR"];
                var module = response["module"];

                if (error !== undefined && error !== null)
                {
                    alert(error);

                    if(error.indexOf("not editable") != -1)
                    {
                        window.open("/gp/modules/creator.jsf", '_self');
                    }
                }
                if (message !== undefined && message !== null) {
                    alert(message);
                }
                loadModuleInfo(response["module"]);
                loadParameterInfo(response["parameters"]);
                setDirty(false);
            },
            dataType: "json"
        });
}

function getParametersJSON()
{
    var parameters = [];
    var pnum = 0;
    $(".parameter").each(function()
    {
        pnum = pnum +1;
        var pname = $(this).find("input[name='p_name']").val();
        var description = $(this).find("textarea[name='p_description']").val();
        var type = $(this).find("select[name='p_type'] option:selected").val();
        var default_val = $(this).find("input[name='p_defaultvalue']").val();
        var optional = $(this).find('input[name="p_optional"]').is(':checked') ? "on" : "";
        var fileformatlist = "";
        var value = "";
        var mode = "";
        var prefix = "";
        var flag = "";

        if($(this).find('select[name="fileformat"]').val() !== undefined
                && $(this).find('select[name="fileformat"]').val() !== null)
        {
            var fileformat = $(this).find('select[name="fileformat"]').val();
            for(f=0;f< fileformat.length;f++)
            {
                fileformatlist = fileformatlist + fileformat[f];
                if(f+1 < fileformat.length)
                {
                    fileformatlist = fileformatlist + ";";
                }
            }
        }

        if($(this).find("input[name='p_flag']").val() != undefined && $(this).find("input[name='p_flag']").val() !== null)
        {
            flag = $(this).find("input[name='p_flag']").val();
        }

        if(pname == undefined || pname == null || pname.length < 1)
        {
            abortSave("A parameter name must be specified for parameter number " + pnum);
            throw("A parameter name is missing");
        }
        //this is an input file type
        if(type === "Input File")
        {
            mode = "IN";
            type = "FILE";
        }
        else if(type === "Directory")
        {
            type = "DIRECTORY";
        }
        else if(type === "Password")
        {
            type = "PASSWORD";
        }
        else if(type === "Integer")
        {
            type = "Integer";
        }
        else if(type === "Floating Point")
        {
            type = "Floating Point";
        }
        else
        {
            type = "TEXT";
        }

        //this is a choice type
        var choices = "";

        if($(this).find('input[name="choicelist"]').length > 0)
        {
            choices = $(this).find('input[name="choicelist"]').val();
        }

        if($(this).find('input[name="p_prefix"]').is(":checked"))
        {
            prefix = $(this).find('input[name="p_flag"]').val();
        }

        var parameter = {
            "name": pname, "description": description, "TYPE": type,
            "default_value": default_val, "optional": optional,
            "fileFormat": fileformatlist, "MODE": mode, "value": choices, "prefix": prefix, "flag":flag
        };

        //add other remaining attributes
        var otherAttrs = $(this).data("otherAttrs");
        var keys = Object.keys(otherAttrs);
        for(k =0; k < keys.length; k++)
        {
            var keyName = keys[k];
            parameter[keyName] =  otherAttrs[keyName];
            console.log("\nsaving other parameter attributes: " + keyName + "=" + otherAttrs[keyName]);
        }

        parameters.push(parameter);
    });

    return(parameters);
}


function saveAndUpload(runModule)
{
    //$("html").addClass('busy').bind('click',function(){
    //    return false;
    //});

    if(saving)
    {
        return;
    }

    saving = true;

    $("#savingDialog").dialog({
        autoOpen: true,
        modal: true,
        height:130,
        width: 400,
        title: "Saving Module",
        beforeClose: function() { return false; }
    });
    $('#progressbar').progressbar({ value: 100 });

    run = runModule;
    //if no support files need to be upload then skip upload file step
    if(module_editor.supportfileinputs.length == 0 && module_editor.license == "")
    {
        saveModule();
    }
    else
    {
        uploadSupportFiles();
    }
}

function uploadSupportFiles()
{
    // all support files have been uploaded now save the module
    if (module_editor.supportfileinputs.length == 0
            && module_editor.licensefile == "")
    {
        saveModule();
    }

    if(module_editor.licensefile != "")
    {
        uploadFile(module_editor.licensefile);
        module_editor.licensefile = "";
    }

    if (module_editor.supportfileinputs.length)
    {
        var nextFile = module_editor.supportfileinputs.shift();

        uploadFile(nextFile);
    }
}

function validateDefaultChoiceValue(defaultValueObj)
{
    var defaultValue = defaultValueObj.val();

    if(defaultValue == undefined || defaultValue == null || defaultValue.length < 1)
    {
        return;
    }

    var choiceValues = [];
    //we are validating that the default value is valid if this parameter is a drop down
    var parent = defaultValueObj.closest(".parameter");

    var choiceParameter = parent.find("input[name='choicelist']");
    if(choiceParameter !== undefined && choiceParameter !== null)
    {
        var choicelist = choiceParameter.val();
        if(choicelist == undefined || choicelist == null)
        {
            return;
        }

        var choices = choicelist.split(';');
        for(i=0;i<choices.length;i++)
        {
            var rowdata = choices[i].split("=");
            if(rowdata != undefined && rowdata != null && rowdata.length > 0)
            {
                choiceValues.push(rowdata[0]);
                if(rowdata[0] == defaultValue)
                {
                    return;
                }
            }
        }
    }

    defaultValueObj.val("");
    alert("Default value \"" + defaultValue + "\" could not be found in the list of possible input values:\n" + choiceValues.join(', ') + "\n\nPlease enter a valid default value.");
}

jQuery(document).ready(function() {

    $("input[type='text']").val("");

    addsectioncollapseimages();
    updatemodulecategories();
    updatefileformats();

    //check if this is a request to edit an existing module
    editModule();

    $(".heading").click(function()
    {
        var visible = $(this).next(".hcontent").data("visible");
        //if first time then content is visible
        if(visible == undefined)
        {
            visible = true;
        }

        $(this).next(".hcontent").slideToggle(340);
        $(this).children(".imgcollapse:first").toggle();
        $(this).children(".imgexpand:first").toggle();

        //visibilty has changed to the opposite
        $(this).next(".hcontent").data("visible", !visible);
    });

    $(".hcontent").show();

    //$(".mheader").load("/gp/modules/header.xhtml");
    // this layout could be created with NO OPTIONS - but showing some here just as a sample...
    // myLayout = $('body').layout(); -- syntax with No Options

    mainLayout = $('body').layout({

    //	enable showOverflow on west-pane so CSS popups will overlap north pane
        west__showOverflowOnHover: false

    //	reference only - these options are NOT required because 'true' is the default
    ,	closable:				true
    ,	resizable:				true	// when open, pane can be resized
    ,	slidable:				true	// when closed, pane can 'slide' open over other panes - closes on mouse-out

    //	some resizing/toggling settings
    ,	north__slidable:		false	// OVERRIDE the pane-default of 'slidable=true'
    ,	north__spacing_open:	0		// no resizer-bar when open (zero height)
    ,	north__spacing_closed:	20		// big resizer-bar when open (zero height)
    ,	south__spacing_open:	0

    ,	south__slidable:		false	// OVERRIDE the pane-default of 'slidable=true'
        //some pane-size settings
    ,	north__minHeight:		80
    ,	north__height:		    80
    ,	south__minHeight:		40
    ,	west__size:			    360
    ,	east__size:				300
    ,	south__size:		    34
    ,	center__minWidth:		100
    ,	useStateCookie:			true
    });

    $( "#parameters" ).sortable();
    $( "#commandlist" ).sortable();

    $("#addone").button().click(function()
    {
        addparameter();
    });

    $("#addparamnum").val("1");
    $("#addmultiple").button().click(function()
    {
        var numparams = $("#addparamnum").val();
        for(i=0;i<numparams;i++)
        {
            addparameter();
        }
    });


    $("input[name='p_flag']").live("keyup", function()
    {
        var parameterParent = $(this).parents(".parameter");

        var p_prefix = parameterParent.find("input[name='p_prefix']");
        var p_flag = parameterParent.find("input[name='p_flag']");

        if(p_flag.val() !== undefined && p_flag.val() !== null)
        {
            if(p_flag.val() !== "")
            {
                p_prefix.removeAttr("disabled");
            }
            else
            {
                p_prefix.attr("disabled", true);
            }
        }
     });

    $("input[name='p_name'], input[name='p_flag'], input[name='p_prefix']").live("change", function()
    {
        var parameterParent = $(this).parents(".parameter");

        updateparameter(parameterParent);
    });


    $('#commandpreview').children().button().click(function()
    {
        var cmd_args = $('#commandlist').text();

        $("#commandtextarea textarea").attr("value", cmd_args);
        $("#commandtextarea").toggle();
    });

    $( "#choiceadd" )
        .button()
        .click(function() {
            var choicerow = $("<tr><td> <input type='text' name='choicev' size='15'/></td>" +
                            "<td> <input type='text' name='choicen' size='15'/></td>" +
                            "<td> <button> X </button></td></tr>");
            choicerow.find("button").button().click(function()
            {
                $(this).parent().parent().remove();
            });

            $(this).parent().next("table").append(choicerow);
    });

    //check for invalid chars ; and = in parameter choice list
    $("input[name='choicen'], input[name='choicev']").live("keyup", function()
    {
        if($(this).val().indexOf(";") != -1 || $(this).val().indexOf("=") != -1)
        {
            alert("The characters = and ; are not allowed");
            $(this).val("");
        }
    });

    $( "#addmodcategorydialog" ).dialog({
            autoOpen: false,
            height: 210,
            width: 330,
            buttons: {
                    "OK": function() {
                        var category = $("#newcategoryname").val();
                        var newcategory = $("<option>" +category + "</option>");
                        $("select[name='category']").append(newcategory);
                        $("select[name='category']").val(category);
                        $("select[name='category']").multiselect("refresh");
                        $( this ).dialog( "close" );
                    },
                    "Cancel": function() {
                        $( this ).dialog( "close" );
                    }
            },
            resizable: false
     });

    $("#addcategory").button().click(function()
    {
       $( "#addmodcategorydialog" ).dialog("open");
    });


    $( "#addfileformatdialog" ).dialog({
            autoOpen: false,
            height: 210,
            width: 330,
            buttons: {
                    "OK": function() {
                        var fileformat = $("#newfileformat").val();
                        fileformat = trim(fileformat);

                        $("#newfileformat").val("");
                        if(fileformat != "")
                        {
                            var newfileformat = $("<option value='" + fileformat + "'>" + fileformat + "</option>");

                            var exists = false;
                            //check if fileformat already exists
                            //append to parameter input file format
                            $("select[name='mod_fileformat']").children().each(function()
                            {
                                if($(this).val() == fileformat)
                                {
                                    exists = true;
                                }
                            });

                            if(exists)
                            {
                                alert("The file format " + fileformat + " already exists");
                                return;
                            }


                            $("select[name='fileformat']").append(newfileformat);
                            $("select[name='fileformat']").multiselect("refresh");

                            //append to module output file format
                            var modnewfileformat = $("<option value='" + fileformat + "'>" + fileformat + "</option>");
                            $("select[name='mod_fileformat']").append(modnewfileformat);
                            $("select[name='mod_fileformat']").multiselect("refresh");
                        }
                        $( this ).dialog( "close" );
                    },
                    "Cancel": function() {
                        $("#newfileformat").val("");
                        $( this ).dialog( "close" );
                    }
            },
            resizable: false
    });


    $("#addfileformat").button().click(function()
    {
       $( "#addfileformatdialog" ).dialog("open");
    });

    $("#viewparameter").button().click(function()
    {
        var listing = [];

        $("#commandlist").children("li").each(function()
        {
            listing.push($(this).text());
        });

        $("#commandlist").data("prevlisting", listing);

        $( "#clistdialog" ).dialog("open");
    });

    $( "#clistdialog" ).dialog({
        autoOpen: false,
        height: 440,
        width: 340,
        buttons: {
                "OK": function()
                {
                    var prev = $("#commandlist").data("prevlisting");

                    var cur = [];
                    $("#commandlist").children("li").each(function()
                    {
                        cur.push($(this).text());
                    });

                    //Reorder the parameters in the command line
                    if(prev !== cur)
                    {
                        var cmdline = $("#commandtextarea textarea").val();

                        for(p=0; p <prev.length; p++)
                        {
                            cmdline = cmdline.replace(prev[p], "+++" + p + "***");
                        }

                        for(p=0;p<prev.length;p++)
                        {
                            cmdline = cmdline.replace("+++" + p + "***", cur[p]);
                        }
                    }

                    $("#commandtextarea textarea").val(cmdline);
                    $( this ).dialog( "close" );
                }
        },
        resizable: true
    });

    $('#savebtn').button().click(function()
    {
        if(!isDirty())
        {
            alert("No changes to save");
        }
        else
        {
            saveAndUpload(false);
        }
    });

    $('#saveRunbtn').button().click(function()
    {
        //no changes detected so skip to run step
        if(!isDirty() && module_editor.lsid != "")
        {
            runModule(module_editor.lsid);
        }
        else
        {
            // save and then run the module
            saveAndUpload(true);
        }
    });


    $("select[name='c_type']").change(function()
    {
        var cmdlinetext = $("#commandtextarea textarea").val();
        var type = $(this).val();
        type = type.toLowerCase();

        var prev_cmd = $("#commandtextarea textarea").data("type");

        cmdlinetext = cmdlinetext.replace(prev_cmd, "");
        if(type == "java")
        {
            cmdlinetext = "<java>" + cmdlinetext;
        }
        else if(type == "perl")
        {
            cmdlinetext = "<perl>" + cmdlinetext;
        }


        $("#commandtextarea textarea").data("type", "<" + type +">");
        $("#commandtextarea textarea").val(cmdlinetext);
    });

    $(".licensefile").change(function()
    {

        if(this.files[0].type != "text/plain")
        {
            alert("ERROR: License file must be a text file");
            return;
        }

        var delbutton = $("<button>x</button>&nbsp;");
        delbutton.button().click(function()
        {
            //remove display of uploaded license file
            $("#licenseFileNameDiv").remove();

            //show the button to upload a new file
            $(".licensefile").parents("span").show();
        });

        var licenseFileNameDiv = $("<div id='licenseFileNameDiv' class='clear'>" + this.files[0].name
                + " (" + bytesToSize(this.files[0].size) + ")" +"</div>");
        licenseFileNameDiv.prepend(delbutton);

        //hide the button to upload a new file
        $(this).parents("span").hide();

        $("#licenseDiv").append(licenseFileNameDiv);

	    //add to list of support files
	    module_editor.licensefile = this.files[0];
    });

    $(".supportfile").live("change", function()
    {
        addToSupportFileList(this.files[0]);

        //add a new file input field
        $(this).attr('name', "name" + module_editor.supportfileinputs.length);
        var parent = $(this).parent();
        parent.append('<input type="file" class="supportfile">');
        $(this).detach();
    });

    $("select[name='mod_fileformat']").multiselect({
        header: false,
        noneSelectedText: "Specify output file formats",
        selectedList: 4 // 0-based index
    });

    $("select[name='category'], select[name='privacy'], select[name='quality'], " +
      "select[name='c_type'], select[name='cpu'], select[name='language'], select[name='modversion']").multiselect({
        multiple: false,
        header: false,
        selectedList: 1
    });

    $("#helpbtn").button().click(function()
    {
          window.open('createhelp.jsp#editingPropertiesHelp', '_blank');
    });

    $("#modtitle").change(function()
    {
        var modtitle = $("#modtitle").val();
        if(modtitle.indexOf(" ") != -1)
        {
            modtitle = modtitle.replace(/ /g, ".");
            $("#modtitle").val(modtitle);
        }
    });

    $("input[name='p_defaultvalue']").live("change", function()
    {
        validateDefaultChoiceValue($(this));
    });

    $("body").change(function()
    {
        setDirty(true);
    });

    //area for dropping support files
    var dropbox = document.getElementById("dropbox");

    // init event handlers
    dropbox.addEventListener("dragenter", dragEnter, true);
    dropbox.addEventListener("dragleave", dragLeave, true);
    dropbox.addEventListener("dragexit", dragExit, false);
    dropbox.addEventListener("dragover", dragOver, false);
    dropbox.addEventListener("drop", drop, false);

    //disable default browser behavior of opening files using drag and drop
    $(document).bind({
       dragenter: function (e) {
          e.stopPropagation();
          e.preventDefault();
          var dt = e.originalEvent.dataTransfer;
         dt.effectAllowed = dt.dropEffect = 'none';
       },
       dragover: function (e) {
          e.stopPropagation();
          e.preventDefault();
          var dt = e.originalEvent.dataTransfer;
          dt.effectAllowed = dt.dropEffect = 'none';
       }
    });
});

function dragEnter(evt)
{
    $("#dropbox").addClass("highlight");
    evt.stopPropagation();
    evt.preventDefault();
}

function dragLeave(evt)
{
    $("#dropbox").removeClass("highlight");
    evt.stopPropagation();
    evt.preventDefault();
}

function dragExit(evt)
{
    evt.stopPropagation();
    evt.preventDefault();
}

function dragOver(evt)
{
    evt.stopPropagation();
    evt.preventDefault();
}

function drop(evt)
{

    $("#dropbox").removeClass("highlight");
    evt.stopPropagation();
    evt.preventDefault();

    var files = evt.dataTransfer.files;
    var count = files.length;

    // Only call the handler if 1 or more files was dropped.
    if (count > 0)
        handleFiles(files);
}

function addToSupportFileList(file)
{
    if(file.name == "manifest")
    {
        alert("You are not allowed to upload files with file name 'manifest'. Please re-name your file and try again.");
        return;
    }

    setDirty(true);

    //check for duplicate files
    $("#supportfileslist").children().each(function()
    {
        var supportFileName = $(this).data("fname");

        if(file.name === supportFileName)
        {
            //trigger click of delete button if file already uploaded
            $(this).find("button").trigger("click");
        }
    });

    var sfilelist = $("<li>" + file.name + " (" + bytesToSize(file.size) + ")" + "</li>");
    sfilelist.data("fname", file.name);

    var delbutton = $("<button>x</button>&nbsp;");
    delbutton.button().click(function()
    {
        var index = -1;
        for(i=0;i<module_editor.supportfileinputs.length;i++)
        {
            var value1 = module_editor.supportfileinputs[i].name;
            var value2 = $(this).parent().data("fname");
            if(value1 === value2)
            {
                index = i;
            }
        }

        if(index == -1)
        {
            //do nothing, unable to find file in support listing
            alert("An error occurred while removing file: File not found");
            return;
        }

        module_editor.supportfileinputs.splice(index,1);
        $(this).parent().remove();
    });

    sfilelist.prepend(delbutton);


    $("#supportfileslist").append(sfilelist);

    module_editor.supportfileinputs.push(file);
}

function handleFiles(files)
{

    for(var i=0;i<files.length;i++)
    {
        addToSupportFileList(files[i]);
    }
}

var result = document.getElementById('result');

// upload file
function uploadFile(file)
{
    var destinationUrl = "/gp/ModuleCreator/upload";
    // prepare XMLHttpRequest
    var xhr = new XMLHttpRequest();
    xhr.open('POST', destinationUrl);
    xhr.onload = function() {
        console.log("on load response: " + this.responseText);

        var response = $.parseJSON(this.responseText);
        module_editor.uploadedfiles.push(response.location);

        uploadSupportFiles();
    };
    xhr.onerror = function() {
        result.textContent = this.responseText;
        console.log("response: " + this.responseText);
    };
    xhr.upload.onprogress = function(event) {
        console.log("upload progress");
        //handleProgress(event);
    }
    xhr.upload.onloadstart = function(event) {
        console.log("onload start support file upload");
    }

    // prepare FormData
    var formData = new FormData();
    formData.append('myfile', file);
    xhr.send(formData);
}

