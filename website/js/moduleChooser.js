function toggleVisibility(id) {
    var element = document.getElementById(id + "_panel");
    var panelState = "open";  // default   
    if(element.style.display=="none")
    {
        openCategory(id);
    }
    else
    {
        closeCategory(id);
    }

}

function closeCategory( id ) {
    var tableElement = document.getElementById(id + "_panel");
    var downArrow = document.getElementById(id + "_expanded_img");
    var rightArrow = document.getElementById(id + "_collapsed_img");	
        
    tableElement.style.display="none";
    downArrow.style.display ="none";
    rightArrow.style.display ="inline"	   
    
    updatePanelState(id, "closed"); 
}

function openCategory( id ) {
    var tableElement = document.getElementById(id + "_panel");
    var downArrow = document.getElementById(id + "_expanded_img");
    var rightArrow = document.getElementById(id + "_collapsed_img");	
        
    tableElement.style.display="block";
    downArrow.style.display ="inline";
    rightArrow.style.display ="none"
    
    updatePanelState(id, "open");
}
	
function openAll() {
   var parentPanel = getCurrentModePanel();
   var panels = parentPanel.getElementsByTagName("div");
   for(var i=0; i<panels.length; i++) {
     var catId = panels[i].getAttribute('categoryId');
     if(catId) {
        openCategory(catId);
     }
   }
}

	
function closeAll() {
   var parentPanel = getCurrentModePanel();
   var panels = parentPanel.getElementsByTagName("div");
   for(var i=0; i<panels.length; i++) {
     var catId = panels[i].getAttribute('categoryId');
     if(catId) {
        closeCategory(catId);
     }
   }
}

function getCurrentModePanel() {
   var radioPanel = document.getElementById("viewchoiceRadioPanel");
   var radioButtons = radioPanel.getElementsByTagName("input");
   for(var i=0; i<radioButtons.length; i++) {
     if(radioButtons[i].type == "radio") {
       var panelId  = "module_table_" + radioButtons[i].value;
       var panel = document.getElementById(panelId);
       if(radioButtons[i].checked && panel != null) {
         return panel;
       }
     }
   }
   return null;
}
     
function chooserModeChanged() {	  
   var radioPanel = document.getElementById("viewchoiceRadioPanel");
   var radioButtons = radioPanel.getElementsByTagName("input");
   for(var i=0; i<radioButtons.length; i++) {
     if(radioButtons[i].type == "radio") {
       var panelId  = "module_table_" + radioButtons[i].value;
       var panel = document.getElementById(panelId);
       if(radioButtons[i].checked && panel != null) {
         Element.show(panel);
         updateChooserMode(radioButtons[i].value);
       }
       else {
         Element.hide(panel);
       }
     }
   }
}


function updateChooserMode(mode) {            
  var opt = {
    method:    'get',
    parameters:  'el=moduleChooserState.updateChooserMode&mode=' + mode,
    onSuccess: receiveModuleResponse,
    onFailure: function(t) {
      alert('Error ' + t.status + ' -- ' + t.statusText);
    }
  } 
  new  Ajax.Request(contextRoot + '/anyThingAtAll.ajax',opt);
}

        
function updatePanelState(id, state) {            
  var opt = {
    method:    'get',
    parameters:  'el=moduleChooserState.updatePanelState&id=' + id + '&state=' + state,
    onSuccess: receiveModuleResponse,
    onFailure: function(t) {
      alert('Error ' + t.status + ' -- ' + t.statusText);
    }
  } 
  new  Ajax.Request(contextRoot + '/anyThingAtAll.ajax',opt);
}

// The callback function - receive response from server
function receiveModuleResponse( req ) {
  	// server response is empty
}

// Code for the combo box
var comboBoxSelect = 0;

function hideComboBox() {
	var choicesBox = document.getElementById("comboBoxChoices");
	setTimeout("document.getElementById('comboBoxChoices').style.display = 'none';", 500);
	
	// Clear selection
	choicesBox.childNodes[comboBoxSelect].style.backgroundColor = "#DFDFB9";
	comboBoxSelect = 0;
}

function showComboBox() {
	var choicesBox = document.getElementById("comboBoxChoices");
	choicesBox.style.display = "block";
	
	// Clear selection
	choicesBox.childNodes[comboBoxSelect].style.backgroundColor = "#DFDFB9";
	comboBoxSelect = 0;
}

function handleSelect(e) {
	var choicesBox = document.getElementById("comboBoxChoices");
	// keyCodes for important keys - UP: 38 - DOWN: 40 - ENTER: 13
	// Handle the enter key
	if (e.keyCode == 13) {
		if (e.preventDefault) e.preventDefault();
	    if (e.stopPropagation) e.stopPropagation();
		choicesBox.childNodes[comboBoxSelect].childNodes[0].onclick();
	}
	
	// Handle down
	if (e.keyCode == 40) {
		for (var i = comboBoxSelect + 1; i < choicesBox.childNodes.length; i++) {
			if (choicesBox.childNodes[i].tagName == "DIV" && choicesBox.childNodes[i].style.display == "block") {
				if (comboBoxSelect != 0) {
					choicesBox.childNodes[comboBoxSelect].style.backgroundColor = "#DFDFB9";
				}
				comboBoxSelect = i;
				choicesBox.childNodes[i].style.backgroundColor = "#FFFFFF";
				return true;
			}
		}
	}
	
	// Handle up
	if (e.keyCode == 38) {
		for (var i = comboBoxSelect - 1; i > 0; i--) {
			if (choicesBox.childNodes[i].tagName == "DIV" && choicesBox.childNodes[i].style.display == "block") {
				if (comboBoxSelect != 0) {
					choicesBox.childNodes[comboBoxSelect].style.backgroundColor = "#DFDFB9";
				}
				comboBoxSelect = i;
				choicesBox.childNodes[i].style.backgroundColor = "#FFFFFF";
				return true;
			}
		}
	}
}

function filterType(e) {
	var filterFor = document.getElementById("moduleComboBox").value.toLowerCase();
	var choicesBox = document.getElementById("comboBoxChoices");
	
	for (var i = 0; i < choicesBox.childNodes.length; i++) {
		if (choicesBox.childNodes[i].tagName == "DIV" && choicesBox.childNodes[i].id.toLowerCase().indexOf(filterFor) != -1) {
			choicesBox.childNodes[i].style.display = "block";
		}
		else if (choicesBox.childNodes[i].tagName == "DIV") {
			choicesBox.childNodes[i].style.display = "none";
		}
	}
}
