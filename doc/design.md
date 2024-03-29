# Design

Below is the context diagram of the Netmobiel Platform.

![Netmobiel Context Diagram](Netmobiel-Context-Diagram.png)

The context diagram shows a number of subsystems:
* [The Maas platform](#backend): The backend of the Mobility-as-a-Service system.
* [The Rideshare service](#rideshare): The carpool service developed in Netmobiel, it's own transport operator for sharing a ride by car. In the implementation the Maas platform and Rideshare service have been packaged together.
* [The Front-end application](#frontend): The front-end comprises of a large responsive webclient contained in very small app written developed with the [Flutter](https://flutter.dev/) framework to support both Android and iOS.
* [Transport Operator GTFS Ingres](#gtfs-ingres): The periodic loading of timetables of transport operators.

Netmobiel uses free as well as some paid external services, in alphabetical order:
* [Firebase Cloud Messaging](https://firebase.google.com/products/cloud-messaging): Text-based message exchange for mobile devices (Apple and Android).
* [Gravitee API Gateway](https://ww.gravitee.io): Centralized access for all client applications of the platform. This service is optional. You can use any API Gateway, or simply omit it.
* [HERE Geocoding & Search](https://www.here.com/platform/geocoding): For geolocating addresses and doing reverse lookups.
* [Keycloak security](https://www.keycloak.org): Identity Management service, used for authentication of all users.
* [Messagebird SMS](https://messagebird.com/): For sending text messages to mobile devices (paid service) for authentication purposes as part of the delegation setup process.
* [OpenTripPlanner](https://www.opentripplanner.org/): The trip planner for road (modality car) and public transport.
* [RDW License Plate registration](https://opendata.rdw.nl/browse?category=Voertuigen&provenance=official): The Dutch registrar for the license plates of cars in The Netherlands, registering the non-private details of each vehicle.

Two services/libraries have not been realized in Netmobiel:
* Mobility Tracker: A smart library for measuring the movements of the traveller, fitting the positions to a street map or public transport routes.
* Travel Diary: The register of the movements of the traveller, as reported by the Mobility Tracker, coupled to the trips in the Netmobiel database.

## Backend
The platform backend is comprised of multiple services and client libraries. 

![Netmobiel Backend](Netmobiel-Backend.png)

The services are in alphabetical order:
* [Banker](../netmobiel-banker-ejb/doc/design.md) - The credit and incentive management service, maintaining the financial position of each user.
* [Communicator](../netmobiel-communicator-ejb/doc/design.md) - The message service, using the internal chat feature, Google's Firebase Cloud Messaging and Messagebird SMS.
* [Profile Service](../netmobiel-profile-ejb/doc/design.md) - The profile management service, maintaining all user attributes.
* [Overseer](../netmobiel-overseer-ejb/doc/design.md) - The coordinator and orchestrator service in Netmobiel.
* [Planner](../netmobiel-planner-ejb/doc/design.md) - The planning an trip managing service for the planning of a trip, using a multi-modal planner.
* [Rideshare](../netmobiel-rideshare-ejb/doc/design.md) - The carpool service, Netmobiel's own mobility provider (transport operator) for offering shared rides.

The backend is packaged as a single EAR, but the design follows more or less a microarchitecture (with a few TODOs left although). The frontend communicates with the platform through a REST API. Each API has its own specification file for easier handling. Each service has the same overall setup, see the picture below. The `Overseer` service is the exception as it does not have its own persistence and has no production REST service.

![Netmobiel Backend Service](Netmobiel-Backend-Service.png) 

Each service consists of three subprojects: 
 * The EJB for the business logic (including database for the particular service).
 * The REST implementation, including the (generated) mapping between domain objects and REST interface objects.
 * The OpenAPI specification. From the specification a JAX-RS interface and data transfer objects are generated. The OpenAPI specification plays therefore an important and central role.
   For editing the OpenAPI specification we used [apicurio](https://www.apicur.io/studio/) and exported each specification to Github.
 
Each external service, e.g. HERE or Firebase, is encapsulated in a client library to limit the visibility of external interfaces as much as possible and to ease the migration to a different service provider, if and when necessary.

As a design principle the services do not depend on each other, with the exception of the `Overseer` which depends on all services because of its orchestration role. This principle is currently a bit violated by the `Planner` service because of the direct link with the `Rideshare` service. Once the [TOMP-API](#netmobiel-and-the-tomp-api) is fully implemented, this direct dependency is removed. The services use an transactional event-based publish-subscribe mechanism to communicate with the `Overseer`. The `Overseer` makes on its turn direct calls to the other services.

### Database setup
The Netmobiel project uses multiple PostgreSQL databases in a single DBMS. In PostgreSQL it is possible to create multiple schemes in a single database. However, in Netmobiel an early decision was made (perhaps unnecessary) to use a separate database for each service. Due to this separation the application server setup requires an XA datasource connection to each database to support distributed transactions. 

More details are provided in the [configuration](configuration.md) section.

### Identification of system objects
In the Netmobiel application the important system objects like a TripPlan or a Trip have a unique identifier. This identifier is the primary key and is generated by the database. Within the same database scheme these objects are related to each other using foreign keys and data integrity is protected by foreign key constraints. When it is necessary to refer to objects in another database scheme a different mechanism is used. Instead of foreign keys a URN scheme is used, with the format `urn:nb:<service>:<class>:<id>`. It is therefore important to understand that these references (also called context) are not protected against integrity errors. In Netmobiel it does (in general) not cause issues, because most records are only soft deleted, i.e., the records are never really deleted. Most REST services can handle both a single id and a urn. The advantage of the urn is that it is easier for a human to detect wrong usage of identifiers while at the same time a loose coupling between services is realized. 

## Rideshare
Netmobiel has developed its own Rideshare service. The original idea at the start of the project has been to have a generic API with the possibility to connect multiple third party rideshare services. That idea has not been implemented, we did not use any other rideshare services than our own. The better choice is to join the [TOMP](#netmobiel-and-the-tomp-api), although (in 2022) TOMP does not yet explicitly support rideshare.

## Frontend
The front-end written for Netmobiel is in the project [Netmobiel Vue Client](https://github.com/SaxionSmartCities/netmobiel-vue-client). It is (mainly) a reactive HTML5 application written in [Vue 2](https://v2.vuejs.org/). For integration with the Firebase messaging a real app was required. For ease of development we used a [Flutter application](https://github.com/SaxionSmartCities/netmobiel-flutter-client) to have a single source and generate from there an Android as well as an iOS app. On desktop the web application can be used directly in any modern browser.

![Netmobiel Frontend](Netmobiel-Frontend.png) 

## GTFS Ingres
GTFS is the abbreviation of General Transit Feed Specification. GTFS defines a common format for public transportation schedules and associated geographic information. GTFS *feeds* let public transit agencies publish their transit data and developers write applications that consume that data in an interoperable way. See [GTFS.org](https://gtfs.org/) for more information.

![GTFS-Ingres](GTFS-Ingres.png) 

For the pilot of Netmobiel the GTFS data of all the public transport operators in the Netherlands was periodically loaded by a script every Sunday morning very early. Netmobiel does not yet support GTFS-Flex or data provided by Bike rentals or Shared Car rentals.

## Netmobiel and the TOMP API
Parallel to the development of Netmobiel the [TOMP](https://github.com/TOMP-WG/TOMP-API) initiative was underway to standardize the interface between a MaaS Platform and the Transport Operators. In Netmobiel a proof of concept has been developed to make the (already finished) Rideshare service available too as a TOMP Transport Operator service. Only the planning and operator end-points are implemented.

The TOMP OpenApi specification is used in Netmobiel to create the Java interface together with all data transfer object types in the [netmobiel-tomp-api](../netmobiel-tomp-api) project. This interface is implemented by a separate [Rideshare Transport Operator](../netmobiel-rideshare-to) REST service. In the repository the service is currently not used. For more information about calling the api see the comments in the Planner in [Planner.java](../netmobiel-planner-ejb/src/main/java/eu/netmobiel/planner/service/Planner.java), method `searchRideshareOnly()`.

