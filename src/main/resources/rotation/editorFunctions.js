
var editor;


/**
 * Function to display example function in editor
 */
function showExample() {
    editor.setValue(
        "function myFunction(src){\n" +
        "\tif (src[0] == 1) {\n" +
        "\t\treturn 'm';\n" +
        "\t}\n" +
        "\telse if (src[1] == 1) {\n" +
        "\t	return 'w';\n" +
        "\t}\n" +
        "\telse if (src[2] == 1) {\n" +
        "\t\treturn 'div';\n" +
        "\t}\n" +
        "}");
}

/**
 * Function to evaluate current content of the editor, given Functionname and sample input
 */
function evaluateScript() {
    if (document.getElementById("Functionname").value === null || document.getElementById("Functioninput") === null) {
        return;
    }
    let func_string = eval(editor.getValue().valueOf() + ";" + document.getElementById("Functionname").value + "(" + document.getElementById("Functioninput").value + ");");
    document.getElementById("Functionoutput").value = func_string;
}
function setUpEditor() {
    editor = ace.edit("Editor", { maxLines: 60, minLines: 10 });
    editor.setTheme("ace/theme/monokai");
    editor.session.setMode("ace/mode/javascript");
    editor.setShowPrintMargin(false);
    editor.setValue("");
}

/**
 * Load current JSON from Server
 */
function loadJson() {
    var JsonRequest = new XMLHttpRequest();
    JsonRequest.addEventListener("load", function (evn) {
        let conceptMapJson = JSON.parse(evn.target.response);
        sessionStorage.setItem("ConceptMap", evn.target.response);

        var count = [];
        for (var curGroup in conceptMapJson.group) {
            let gr = conceptMapJson.group[curGroup];
            count[curGroup] = gr.element.length;

        }
        createDots(count);
        //document.getElementById("Output").appendChild(document.createTextNode(count));
    });
    JsonRequest.addEventListener("error", function (evn) {
        alert("Oops! Something went wrong.");
    });
    JsonRequest.open('GET', "./loadJson");
    JsonRequest.send();
}

/**
 * Function to create Dots for each Source-Target-Pair
 * @param {Array<Number>} dotNumber 
 */
function createDots(dotNumber) {
    var progress = document.querySelector('.dots');


    for (var i = 0; i < dotNumber.length; i++) {
        for (var j = 0; j < dotNumber[i]; j++) {
            let dot = document.createElement('button');
            let curGroup = i;
            let curElement = j;
            dot.addEventListener("click", function (evn) { switchTo(curGroup, curElement); });
            dot.classList.add('dot');
            dot.classList.add('btn','small', 'waves-effect',"waves-light")
            let symbole = document.createElement("i")
            symbole.classList.add("material-icons","center-align")
            symbole.innerHTML="edit" 
            dot.appendChild(symbole)
            dot.id = "Dot(" + i + "," + j + ")";
            progress.appendChild(dot);
        }
        space = document.createElement('div');
        space.classList.add('space');
        progress.appendChild(space);


    }
    progress.style.marginLeft = "20%";
    progress.style.marginRight = "20%";
    sessionStorage.setItem("CurGroup", 0);
    sessionStorage.setItem("CurElement", 0);
    //document.getElementById("Dot(0,0)").classList.add("activatedDot");
    switchTo(0, 0);

}
/**
 * Function to replace site-content with request Source-Target-Pair and the saved rule
 * @param {Number} group 
 * @param {Number} element 
 */
function switchTo(group, element) {
    let cm = JSON.parse(sessionStorage.getItem("ConceptMap"));
    let curGroup = parseInt(sessionStorage.getItem("CurGroup"));
    let curElem = parseInt(sessionStorage.getItem("CurElement"));
    let curDot = document.getElementById("Dot(" + curGroup + "," + curElem + ")");
    save();
    mdrElements = cm.group[group].element[element].target.length+1;
    curDot.classList.remove("activatedDot");
    let nextDot = document.getElementById("Dot(" + group + "," + element + ")");
    if (nextDot.classList.contains("saved")) {
        nextDot.classList.add("activatedDot");
        editor.setValue(sessionStorage.getItem("Mapping(" + group + "," + element + ")"));
    }
    else if (nextDot.classList.contains("dismissed")) {
        nextDot.classList.add("activatedDot");
        editor.setValue("");
    }
    else {
        nextDot.classList.add("activatedDot");
        editor.setValue("");
    }
    document.getElementById("Functionname").value = "";
    document.getElementById("Functioninput").value = "";
    document.getElementById("Functionoutput").value = "";
    sessionStorage.setItem("CurGroup", group);
    sessionStorage.setItem("CurElement", element);
    generateDecriptions(cm, group, element);
}
/**
 * Function to generate the desriptions for each Source-Target-Pair
 * @param {JSON} cm 
 * @param {Number} group 
 * @param {Number} element 
 */
function generateDecriptions(cm, group, element) {

    let source = "";
    let target = "";
    let codelist= ""
    console.log(group + " " + element);
    source += "SourceURL: " + cm.sourceUri + "<br><br>";
    source += "Code: " + cm.group[group].element[element].code + "<br>";
    source += "Name: " + cm.group[group].element[element].display + "<br>";
    target += "TargetURL: " + cm.targetUri + "<br>";
    document.getElementById("TargetInjector").innerHTML="";
    let targList = cm.group[group].element[element].target;

    var targetIndex = 0;
    for (var curtarget in targList) {
        target += "<br>";
        target += "Code: " + targList[curtarget].code + "<br>";
        target += "Name: " + targList[curtarget].display + "<br>";
        var curCode = document.createElement("div");
        curCode.innerHTML = targList[curtarget].code;
        curCode.id = "TargetInjector:"+targetIndex;
        document.getElementById("TargetInjector").appendChild(curCode);
        targetIndex+=1;

    }
    document.getElementById("Source").innerHTML = source;
    document.getElementById("Target").innerHTML = target;

    document.getElementById("SourceInjector").innerHTML = cm.group[group].element[element].code;
    elementsToFinish = targList.length;
    curElement = 0
    
    // for(var childs in document.getElementById("TargetInjector").childNodes)
    for(let i = 0; i < elementsToFinish; i++){
        window.addEventListener("requestFinished:"+i, function (e) {
            console.log("event occured, TargetInjector:"+i);
            search('TargetInjector:'+i, cm.targetUri + "/dataelements/%s", 'urn:\\w+' + ':dataelement:\\d+:\\d+', 'validation', 'datatype', 'description');
    
        });

    }
    search('SourceInjector', cm.sourceUri + "/dataelements/%s", 'urn:\\w+' + ':dataelement:\\d+:\\d+', 'validation', 'datatype', 'description');


    //search('SourceInjector',"http://mdr.ccp-it.dktk.dkfz.de/v3/api/mdr/dataelements/%s",'urn:dktk:dataelement:\\d+:\\d+','validation', 'datatype','description');
}

/**
 * Back-Button function
 */
function goToElementBefore() {
    let curGroup = parseInt(sessionStorage.getItem("CurGroup"));
    let curElem = parseInt(sessionStorage.getItem("CurElement"));
    let newGroup, newElem;
    if (curGroup == 0 && curElem == 0) {
        return;
    }
    else if (curElem == 0) {
        let cm = JSON.parse(sessionStorage.getItem("ConceptMap"));
        newElem = cm.group[curGroup - 1].element.length - 1;
        newGroup = curGroup - 1;
    }
    else {
        newElem = curElem - 1;
        newGroup = curGroup;
    }
    switchTo(newGroup, newElem);

}
/**
 * Next-button function
 */
function goToElementAfter() {
    let curGroup = parseInt(sessionStorage.getItem("CurGroup"));
    let curElem = parseInt(sessionStorage.getItem("CurElement"));
    let cm = JSON.parse(sessionStorage.getItem("ConceptMap"));
    let newGroup, newElem;
    maxGroup = cm.group.length - 1;
    maxElement = cm.group[curGroup].element.length - 1;
    if (curGroup == maxGroup && curElem == maxElement) {
        return;
    }
    else if (curElem == maxElement) {
        newGroup = curGroup + 1;
        newElem = 0;
    }
    else {
        newGroup = curGroup;
        newElem = curElem + 1;
    }
    switchTo(newGroup, newElem);

}
/**
 * Function to save current rule in session storage. Change button color.
 */
function save() {
    let curGroup = parseInt(sessionStorage.getItem("CurGroup"));
    let curElem = parseInt(sessionStorage.getItem("CurElement"));
    let curDot = document.getElementById("Dot(" + curGroup + "," + curElem + ")");
    if (editor.getValue() != "") {
        curDot.classList.remove("dismissed");
        curDot.classList.add("saved");
        curDot.firstChild.innerHTML="done"
        sessionStorage.setItem("Mapping(" + curGroup + "," + curElem + ")", editor.getValue().replace(/"/g,"'"));

    }
    else {
        curDot.classList.remove("saved");
        if (!curDot.classList.contains("dismissed")) {
            curDot.firstChild.innerHTML="edit"
            sessionStorage.removeItem("Mapping(" + curGroup + "," + curElem + ")");
        }


    }

}

/**
 * Function to dismiss the current pair and change the color of the corresponding button
 */
function dismiss() {
    if (editor.getValue() != "") {
        let confirmation = confirm("Code verwerfen?");
        if (!confirmation) return;
    }
    let curGroup = parseInt(sessionStorage.getItem("CurGroup"));
    let curElem = parseInt(sessionStorage.getItem("CurElement"));
    let curDot = document.getElementById("Dot(" + curGroup + "," + curElem + ")");
    curDot.classList.remove("saved")
    curDot.classList.add("dismissed");
    curDot.firstChild.innerHTML="clear"
    sessionStorage.setItem("Mapping(" + curGroup + "," + curElem + ")", "DISMISSED");
    editor.setValue("");
}

function suggest() {
    let curGroup = parseInt(sessionStorage.getItem("CurGroup"));
    let curElem = parseInt(sessionStorage.getItem("CurElement"));

    var request = new XMLHttpRequest();
    request.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
            if(this.responseText!="nosuggestion"){
                editor.setValue(this.responseText);
                save();
            }else{
                alert("Keine ähnliche Transformation gefunden!");
            } 
        }
    };
    request.open('GET', "/getSuggestion?group="+curGroup+"&elem="+curElem);
    request.send();

}