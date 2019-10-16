# ROTATION
The smart edito**R** f**O**r s**T**andardised d**A**ta **T**ransformat**ION** uses [HL7 FHIR](http://hl7.org/fhir/) ConceptMaps and StructureMaps to define transformation rules based on matched metadata.

The integration of heterogeneous healthcare data sources is a necessary process to enable the secondary use of valuable information in clinical research. Data integration is time-consuming for data stewards. Most tools define the transformation in a proprietary format within the application and therefore the reuse of the rules is hardly not supported. The transformation using predefined rules for data harmonization can reduce the time-consuming and error-prone work and ease the data integration at various sites. In our study, we examined various script(ing) languages to find the most suitable candidate for definition of transformation rules and implement a smart editor which supports the data stewards in selecting rules reusing them. Thereby, it also provides an automatic and seamless documentation to strengthen the reliability of the defined transformation rules.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites
What things you need to install the software and how to install them:
- **Docker** (see https://docker.com)
- **Auth Server** for user identification (e.g. [Keycloak](https://www.keycloak.org/) or [Samply.Auth](https://maven.mitro.dkfz.de/samply/auth/))

### Installing
A step by step series of examples that tell you how to get a development env running

1. Register this client at your auth server:
    - The redirect URL should be http://localhost:4567/validateLogin
2. Add an  **application.properties** file to the resource directory containing:
    - auth.hosturl = The URL of the auth server you want to use (begins with "https://")
    - auth.clientid = Your client id (request it from the auth-admin)
    - auth.clientsecret = Your client secret ("-")
    - auth.publickey = the public key of your auth server (get it via "\*hosturl\*/oauth2/certs")
    - mongo.url = Access-URL to your MongoDB (including user and password)
    - mongo.database = Name of your database


3. Run in terminal (from root directory of the project)
    `docker build -t itcrl_rotation .`
4. After building completes:
    `docker run -d -p 4567:4567 itcrl_rotation`
5. Go to http://localhost:4567/
6. Login and enjoy! 
