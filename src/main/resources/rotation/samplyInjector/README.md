# MDR Injector #

The MDR Injector is a JavaScirpt library for resolving identifiers of a meta data repository.

### Install ###

You can use the libary by including the JavaScript file.  
This can be done by adding the following line between the ```<head>``` tag:
   
   
<head> 

```
<script src="injector.js" charset="utf-8"></script>
```

</head>

and then call the method:
   
```search(tagname or tagID,MDRURL,regular expression, interface-dependent attribute(key) ,interface-dependent attribute(subkey), popupID)```   
   
###Example###
   
```search('output',"http://mdr.ccp-it.dktk.dkfz.de/v3/api/mdr/dataelements/%s",'urn:dktk:dataelement:\\d+:\\d+','designations','designation', 'definition')```
   
####tagname/tagID####
The tagname or the tagID like:  
   
```<div id='output'>``` or ```<output>```
   
which should be searched for identifiers.     
   
####MDRURL####
The URL of the meta data repository: 
   
```http://mdr.ccp-it.dktk.dkfz.de/v3/api/mdr/dataelements/%s``` 
   
Add always a "*%s*" in the end of the URL to replace it by the identifier and to call the URL of the identifier:     
      
```http://mdr.ccp-it.dktk.dkfz.de/v3/api/mdr/dataelements/%s``` => ```http://mdr.ccp-it.dktk.dkfz.de/v3/api/mdr/dataelements/urn:dktk:dataelement:12:1```  
    
####Regular Expression###
A regular expression is necessary to search after identifiers.   
With the regular expression:   
```'urn:dktk:dataelement:\\d+:\\d+'```   
   
the key ```urn:dktk:dataelement:12:1``` can be found.
 
 
####Key, Subkey and PopupID####

If the JSON file is a twoDimensional array you need a key and a subkey to get access to the identifiers.   
Example for ```"designations"```(key), ```"designation"```(subkey) and ```"definition"``` (popupID):   
```
"designations":[
				{"language":"en",
				"designation":"smoking status",
				"definition":"An indication of a person's current tobacco and nicotine consumption as well as some indication of smoking history
				}]
```

With the popupID you can choose which text should appear in the popup when the mouse is hovering over a identifier.

####Release notes####

####1.1.0
- Changed popup style
- fixed issue where div id and tag name can be the same

####1.0.0

- first version