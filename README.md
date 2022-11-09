# netmobiel-platform

`Netmobiel Platform` is the back-end of the NetMobiel Mobility-As-A-Service. The back-end is written as a Java Enterprise application and packaged as an EAR. 

The Netmobiel Project (also named Netmobil) is a Dutch RAAK-Pro project managed by Saxion during 2017-2022. The purpose of the project was to create a Mobility-as-a-Services system for rural areas. The selected pilot region was the Achterhoek region, in the east of Gelderland, The Netherlands. The requirements, both for features and user interface were drawn up together with people from the selected communities in the targeted areas. Prototypes were also tested in the same communities. The original intention was to reuse an existing MaaS platform and make adaptations. That appeared however too hard for multiple reasons. Enter this project, a first starter for a MaaS system. 
 
## Project setup
After checking out from GitHub the project is ready for use.

## Design
Following below is the functional architecture of Netmobiel.

![Netmobiel Functional Architecture](doc/Netmobiel-Architecture.png)

Netmobiel comprises of four blocks:
* The front-end: User interface for travellers, rideshare drivers and a bit of management for the administrators of Netmobiel. 
* The MaaS platform: The functionality for planning and booking a trip.
* The Transport Operators: The parties with the wheels, including public transport. Rideshare is the carpool service developed in Netmobiel.
* The security: Security must be enforced in each to prevent unauthorized access. 

Netmobiel targets the people living in a rural area. The emphasis has been on sharing rides, and also on combining in a multi-legged trip the first or last leg 
by car with public transport for the other legs. Netmobiel defines its own  carpool service. As a consequence of the emphasis on car pooling, 
Netmobiel has combined the user interface of traveller (passenger) and car driver in a single front-end application for all users of Netmobiel.

Refer to the [design page](doc/design.md) for a global description of the Netmobiel backend. Each individual service is explained in more detail by the documentation in the service itself.

## Configuration
Each service has its own database. Refer to the documentation of each service for the details of ech setup.

### Compiles and builds the EAR 
```
mvn install
```
### Run all unit tests and integration tests
```
mvn -P arq-remote verify
```

