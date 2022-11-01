# netmobiel-profile-ejb

`Profile Service` is the implementation of the profile service for the NetMobiel Mobility-As-A-Service platform. The EJB jar contains the business logic.
 
## Project setup
After checking out from GitHub the project is ready for use.

## Design
Refer to the [design page](doc/design.md) for a global description of the planner service.
  
## Configuration
The profile service component requires the definition of the profilesvcDS datasource in WildFly. Refer to the [detailed instructions](doc/configuration.md) to setup the database and database connection.

The database schema must be manually created for development, acceptance and production stages. 

The test database is automatically populated by the integration tests. Use the test schema generated by Hibernate JPA as inspiration to setup and maintain the correct schema. 

### Compiles and builds the EJB 
```
mvn install
```
### Run your unit tests and integration tests
```
mvn -P arq-remote verify
```
