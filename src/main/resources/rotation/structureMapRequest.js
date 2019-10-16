/**
 * Function to generate a JSON which is used by the Server to generate the StructureMap
 */
function generateStructureMap(){
    var cm = JSON.parse(sessionStorage.getItem("ConceptMap"));
    var json = "{";

    for(var curGroup in cm.group){
        for(var curElem in cm.group[curGroup].element){
            let curCode = sessionStorage.getItem("Mapping("+curGroup+","+curElem+")");
            //current pair wasn't edited yet.
            if(curCode==null){
                switchTo(curGroup, curElem);
                if(confirm("Das Element " + curElem +" in Gruppe " + curGroup + " verwerfen?")){
                    dismiss();
                    curCode = sessionStorage.getItem("Mapping("+curGroup+","+curElem+")");
                }
                else{
                    return;
                }
            }
            //TODO Funktionen mit "" können auf diese Weise nicht übertragen werden.
            //curCode.replace('\"',"\'");
            json+='"Mapping(' + curGroup + "," + curElem + ')" : "' + curCode +'"';
            if(curGroup!=cm.group.length-1 || curElem!= cm.group[curGroup].element.length-1){
                json+=",\n";
            }


        }
    }
    json+="}"
    console.log(json);
    parseRequest = new XMLHttpRequest();
    parseRequest.addEventListener("load", function (event) {
        document.getElementById("EditorViewDiv").style.display = "none";
        document.getElementById("StructureMapViewDiv").style.display = "block";
        let structureMap = document.createElement("pre");
        sessionStorage.setItem("Bundle", event.target.response);
        sessionStorage.setItem("StructureMap", JSON.stringify(JSON.parse(event.target.response).entry[0].resource,null , 2));
        structureMap.innerHTML= sessionStorage.getItem("StructureMap")
        //structureMap.classList.add("flow-text");
        document.getElementById("StructureMapView").appendChild(structureMap);

    });
    parseRequest.addEventListener("error", function (event) {
        alert('Oops! Something went wrong.');
    });
    parseRequest.open('POST', "/parseToStructureMap", true);
    parseRequest.send(json);


}
/**
 * Function to go back to editor view
 */
function goBackToEditor(){
    sessionStorage.removeItem("StructureMap");
    sessionStorage.removeItem("Bundle");
    document.getElementById("StructureMapView").innerHTML="";
    document.getElementById("StructureMapViewDiv").style.display ="none";
    document.getElementById("EditorViewDiv").style.display = "block";
}
/**
 * Function to confirm the StructureMap, gives Server the request to upload the files to MongoDB and switch to save view
 */
function uploadToDb(){
    uploadRequest = new XMLHttpRequest();
    uploadRequest.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
          document.getElementById("StructureMapViewDiv").style.display="none";
          document.getElementById("SaveDiv").style.display="block";
        }
    };
    uploadRequest.open('GET', "/uploadToDb");
    uploadRequest.send();
}
var form2 = document.forms.namedItem("StructureMapUpload");

/**
 * Submit-Event to post the Bundle on a Server
 */
form2.addEventListener("submit", function (event) {
    event.preventDefault();
    let url = document.getElementById("PostURL").value;
    postRequest = new XMLHttpRequest();

    postRequest.onreadystatechange = function() {
        if (this.readyState == 4 && (this.status == 201 ||this.status == 200)) {
            document.getElementById("Response").style.color = "green";
            document.getElementById("Response").innerHTML = "Upload zu " + url + " erfolgreich!";
            console.log("Upload erfolgreich");
            console.log(event.target);
        }
        else if(this.readyState==4){
            document.getElementById("Response").style.color = "red";
            document.getElementById("Response").innerHTML = "Irgendwas ist schiefgelaufen! Upload zu " + url + " fehlgeschlagen, Fehlercode "+this.status+"!";
        }
    };
    postRequest.addEventListener("error", function (event) {
        alert("Irgendwas ging schief!");
    });

    postRequest.open('POST', url);
    postRequest.setRequestHeader("Content-Type","application/json")
    postRequest.send((sessionStorage.getItem("Bundle")));


        
    });
    