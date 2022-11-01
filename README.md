# netmobiel-platform

`Netmobiel Platform` is the back-end of the NetMobiel Mobility-As-A-Service. The back-end is written as a Java Enterprise application and packaged as an EAR. 

The Netmobiel Project (also named Netmobil) is a Dutch RAAK-Pro project managed by Saxion during 2017-2022. The purpose of the project was to create a Mobility-as-a-Services system for a rural area. The selected pilot region was the Achterhoek region, in the east of Gelderland, The Netherlands. The requirements, both for features and user interface were drawn up together with people from the selected communities. Prototypes were also tested in the same communities. The original intention was to reuse an existing MaaS platform and make adaptations. That appeared however too hard for multiple reasons. Enter this project, a first starter for a MaaS system. 
 
## Project setup
After checking out from GitHub the project is ready for use.

## Design
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

