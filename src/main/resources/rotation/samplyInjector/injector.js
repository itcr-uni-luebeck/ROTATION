var isIE = window.XDomainRequest ? true : false;
var url = '';
var resultText = '';
var invocation = createCrossDomainRequest();
var urnTmp;
var matches = new Array();
var number = 0;
var id = '';
var idOrName = 0;
var result = '';
var keyName = '';
var subKeyName = '';
var popUpID = '';
var jsonResultAll = new Array();
var jsonResultId = new Array();
var popUpString = '';
var popUpStringArray = new Array();
var popupActivated = new Array();
var customEvent = new CustomEvent("requestFinished");
var curElement = 0;
var elementsToFinish = 0;

function createCrossDomainRequest(url, handler) {
    var request;
    if (isIE) {
        request = new window.XDomainRequest();
    } else {
        request = new XMLHttpRequest();
    }
    return request;
}

function makeRequest(urn) {
    urnTmp = urn;
    var urlTmp = url.replace('%s', urn);
    if (invocation) {
        if (isIE) {
            invocation.onload = requestSucceeded;
            invocation.open("GET", urlTmp);
            invocation.timeout = 1000;
            invocation.setRequestHeader("Content-Type", "application/json");
            invocation.send();
        } else {
            invocation.open('GET', urlTmp);
            invocation.timeout = 1000;
            invocation.onreadystatechange = handler;
            invocation.send();
        }
    } else {
        resultText = "No Invocation TookPlace At All";
    }
}

function handler(evtXHR) {
    if (invocation.readyState == 4) {
        if (invocation.status == 0 || invocation.status == 404 || url == '') {
            outputResultError();
        }
        if (invocation.status == 200) {
            requestSucceeded();
        }
        else {
            resultText = "Invocation Errors Occured";
        }
    }
}

function requestSucceeded() {
    resultText = JSON.parse(invocation.responseText);
    outputResult();
}

function outputResultError() {
    var str = "Unbekannt" //urnTmp;
    var str = str.fontcolor("red");
    if (idOrName == 0) {
        document.getElementById(id).innerHTML = str //document.getElementById(id).innerHTML.replace(new RegExp(urnTmp, 'g'), str);
    } else {
        document.getElementsByTagName(id)[0].innerHTML = str //document.getElementsByTagName(id)[0].innerHTML.replace(new RegExp(urnTmp, 'g'), str);
    }
    if(curElement<elementsToFinish){
        
        dispatchEvent(new CustomEvent("requestFinished:"+curElement));
        console.log("dispatched"+curElement);
        curElement+=1;
    }
    //createRequest(number + 1);
}

function outputResult() {
    getNames(resultText, keyName, subKeyName, false);
    jsonResultAll.push(resultText);
    jsonResultId.push(urnTmp);
    if (idOrName == 0) {
        document.getElementById(id).innerHTML = document.getElementById(id).innerHTML.replace(new RegExp(urnTmp, 'g'), "<span onmouseover='popup(" + '"' + urnTmp + '"' + ",event)' onmouseout='deletePopup(" + '"' + urnTmp + '"' + ")'>" + result + "</span><div id='" + urnTmp + "' style='display: none; position:absolute; border-radius: 5px;border: 7px solid #336699; color: #000000; background-color: powderblue';>text</div>");
    } else {
        document.getElementsByTagName(id)[0].innerHTML = document.getElementsByTagName(id)[0].innerHTML.replace(new RegExp(urnTmp, 'g'), "<span onmouseover='popup(" + '"' + urnTmp + '"' + ",event)' onmouseout='deletePopup(" + '"' + urnTmp + '"' + ")'>" + result + "</span><div id='" + urnTmp + "' style='display: none; position:absolute; border-radius: 5px;border: 7px solid #336699; color: #000000; background-color: powderblue';>text</div>");
    }
    if(curElement<elementsToFinish){
        
        dispatchEvent(new CustomEvent("requestFinished:"+curElement));
        console.log("dispatched"+curElement);
        curElement+=1;
    }
    //createRequest(number + 1);
}

function popup(id, event) {
    var x = event.clientX;
    var y = event.clientY;
    var jsonID = jsonResultId.indexOf(id);
    var popupActivatedBoolean = popupActivated[jsonID];
    if (popupActivatedBoolean == false) {
        var jsonString = jsonResultAll[jsonID];
        getNames(jsonString, keyName, popUpID, true);
        popUpStringArray[jsonID] = popUpString;
        popupActivated[jsonID] = true;
    }
    document.getElementById(id).style.left = x + "px";
    document.getElementById(id).style.top = y + "px";
    document.getElementById(id).innerHTML = popUpID + ':<br>' + popUpStringArray[jsonID];
    document.getElementById(id).style.display = 'block';

}

function deletePopup(id) {
    document.getElementById(id).style.display = 'none';
}


function search(idTag, urlUrn, regex, keyNameTmp, subKeyNameTmp, popUpIDTmp) {
    url = urlUrn;
    id = idTag;
    keyName = keyNameTmp;
    subKeyName = subKeyNameTmp;
    popUpID = popUpIDTmp;
    searchByTagName(id, regex);
    searchByID(id, regex);
}

function searchByTagName(id, regex) {
    var myString = '';
    if (document.getElementsByTagName(id).length > 0) {
        myString = document.getElementsByTagName(id)[0].innerHTML;
        idOrName = 1;
        matches = myString.match(new RegExp(regex, "g"));
        filterMatches();
        createRequest(0);
    }
}


function searchByID(id, regex) {
    var myString = '';
    if (document.getElementById(id).innerHTML.length > 0) {
        myString = document.getElementById(id).innerHTML;
        idOrName = 0;
        matches = myString.match(new RegExp(regex, "g"));
        filterMatches();
        createRequest(0);
    }

}

function createRequest(count) {
    for (i = count; i < matches.length; i++) {
        number = i;
        makeRequest(matches[i]);
    }
}

function filterMatches() {
    var matchTmp = new Array();
    for (i = 0; i < matches.length; i++) {
        var tmpUrn = matches[i];
        if ((matchTmp.indexOf(tmpUrn)) == -1) {
            matchTmp.push(tmpUrn);
            popupActivated.push(false);
            popUpStringArray.push('');
        }
    }
    matches = matchTmp;
}

function getNames(obj, keyNameTmp, subKeyName, popup) {
    for (var key in obj) {
        if (obj.hasOwnProperty(key)) {
            if ("object" == typeof(obj[key])) {
                if (keyNameTmp == key || keyNameTmp == "") {
                    getNames(obj[key], "", subKeyName, popup);
                }
            } else if (key == subKeyName && popup == false) {
                result = obj[key];
            } else if (key == subKeyName && popup == true) {
                popUpString = obj[key];
            }
        }
    }
}