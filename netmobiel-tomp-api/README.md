# netmobiel-tomp-api

The TOMP API is the interface between a Mobility-As-A-Service provider and the Transport Operator NetMobiel  platform. The content of this project has been generated from the TOMP-API.yaml. 

This project generates three parts of the API:
* The model classes for use at client-side as well as server-side.
* The API with the JAXRS annotations for implementation by the server side.
* The generated RestEasy client for both the MaaS Provider and the Transport oparator Api.

## Project setup
After checking out from GitHub the project is ready for use.

Note: The setup of swagger is extremely tricky due to the tons of bugs. openapi-generator is an alternative, but does not yet support OpenAPI 3.0.0.

## Configuration
No further configuration is needed. 

### Compiles and builds the API code 
```
mvn install
```

