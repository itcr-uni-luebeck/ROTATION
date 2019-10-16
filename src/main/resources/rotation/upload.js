/**
 * Function to load at start
 */
window.addEventListener("load", function () {
    sessionStorage.clear();

    /**
     * Function to send uploaded/imported file to the server
     * @param {String|Blob|Document} s 
     */
    function sendToServer(s) {
        var XHR2 = new XMLHttpRequest();
        XHR2.addEventListener("load", function (event) {
            if(this.status==200){
                document.getElementById("UploadedFile").innerHTML = XHR2.response;
                document.getElementById("StartUploadDiv").style.display = "none";
                document.getElementById("UploadedFileDiv").style.display = "block";

            }
            else if (this.status==405) {
                document.getElementById("errorUpload").innerHTML=XHR2.response;
                
            }
            else{
                alert("Etwas ist schiefgelaufen!");
            }

        });
        XHR2.addEventListener("error", function (event) {
            alert('Oops! Something went wrong.');
        });
        XHR2.open('POST', "/parseFile", true);
        XHR2.send(s);
    }

    /**
     * Function to get JSON from external server
     */
    function getExternalData() {
        var XHR = new XMLHttpRequest();
        // Define what happens on successful data submission
        XHR.addEventListener("load", function (event) {
            //alert(JSON.stringify(XHR.response));
            let conceptMap = XHR.response;
            conceptMap.url = document.getElementById("RessourceUrl").value;
            sendToServer(JSON.stringify(conceptMap));
        });
        // Define what happens in case of error
        XHR.addEventListener("error", function (event) {
            alert('Oops! Something went wrong.');
        });
        XHR.open('GET', document.getElementById("RessourceUrl").value);
        XHR.responseType = 'json';
        XHR.send();

    }

    var form = document.getElementById("ConceptMapRest");

    form.addEventListener("submit", function (event) {
        event.preventDefault();
        getExternalData();
    });

    var form2 = document.forms.namedItem("ConceptMapFile");
    form2.addEventListener("submit", function (event) {
        event.preventDefault();
        var formdat = new FormData(form2);

        sendToServer(formdat);
    });
});
/**
 * Function to go back to the Upload-View
 */
function backToUpload() {
    document.getElementById("StartUploadDiv").style.display = "block";
    document.getElementById("UploadedFileDiv").style.display = "none";
}
/**
 * Function to load EditorDiv content after user confirmed the uploaded file
 */
function showEditor(){
    var EditorRequest = new XMLHttpRequest();
    EditorRequest.addEventListener("load",function(evn){
        document.getElementById("EditorViewDiv").innerHTML= evn.target.response;
        document.getElementById("EditorViewDiv").style.display="block";
        document.getElementById("UploadedFileDiv").style.display = "none";
        setUpEditor();
        /* var scriptarray = document.getElementById("EditorViewDiv").getElementsByTagName("script");
        for(var i = 0; i < scriptarray.length; i++){
            eval(scriptarray[i].innerHTML);
        } */
        loadJson();

    });
    EditorRequest.addEventListener("error", function(evn){
        alert("Oops! Something went wrong.");
    });
    EditorRequest.open('GET', "./page");
    EditorRequest.send();
}

function getCookie(cname) {
  var name = cname + "=";
  var ca = document.cookie.split(';');
  for(var i = 0; i < ca.length; i++) {
    var c = ca[i];
    while (c.charAt(0) == ' ') {
      c = c.substring(1);
    }
    if (c.indexOf(name) == 0) {
      return c.substring(name.length, c.length);
    }
  }
  return "";
}
