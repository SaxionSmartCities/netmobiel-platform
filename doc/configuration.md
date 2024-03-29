# Platform Configuration
The configuration of the backend concerns a number of topics:
1. The security setup for Keycloak. The application server requires a client adapter and a bit of client configuration is needed for each service.
2. The definition of api keys etc. in the application server.
3. Configuration of OpenTripPlanner and the periodic update of the map and GTFS data.
4. The database(s). Each service has its own database. 
5. The API Gateway, if included.
6. Other external services.

## Configuration of Keycloak
Netmobiel uses the [Keycloak Open Source Identity and Access Management Server](https://www.keycloak.org/) as its single sign-on solution. At the time of writing we used version 15.0. To use Keycloak for Netmobiel, the global roles need to be configured as well as the clients.

First install Keycloak, following the procedures in the Keycloak documentation. Then create a realm in Keycloak for Netmobiel, e.g. `netmobiel`. The realm is the container for all other configuration steps. Configure the realm according the keycloak instructions.

### Global roles
Add the following roles:
* admin -  The master of the Netmobiel universe.
* treasurer - Responsible for the credit management in NetMobiel. The treasurer is allowed to run a payment batch to execute withdrawal requests. 
* delegate - Users with this role may act on behalf of other users in Netmobiel (for informal care, the caregiver).

The roles can only be assigned from the Keycloak console.

### Client applications
Each back-end service with a REST API has a client application defined in Keycloak. Add the following clients with access type `bearer-only`.
* banker-service
* communicator-service
* geo-service
* planner-service
* profile-service
* rideshare-service 

The realm name and the client id are used in the `<REST Service>/src/main/webapp/WEB-INF/keycloak.json`. Update this file if necessary.

The frontend application also needs a representation, so add another client:
* netmobiel-frontend (with access type `public`). Enable standard flow. Add valid redirection urls and web origins.

If you want to use [Postman](https://www.postman.com/) for testing, it is useful to add another client that can be used to fetch a token from, e.g. netmobiel-postman. Configure according requirements of Postman.

### Modifying look and feel
For Netmobiel we adapted the standard look and feel of Keycloak. We modified the CSS a bit, and altered a few message texts. See our [keycloak project](https://github.com/SaxionSmartCities/netmobiel-keycloak).

## Configuration of Wildfly
Netmobiel uses the Wildfly application server (version 17 at the time of writing). Download and install Wildfly according instructions. 

Install the client adapter of Keycloak for Wildfly, follow the instructions.

Because of the distributed databases, extended datasources need to be defined. This is explained in the configuration section of each Netmobiel service.

Wildfly 17.0 kept endlessly complaining about attempts to roll-back failed transactions. In production useful, but during development it is of no use. Disable the feature with the following fragment in `standalone.xml`.

```XML
<system-properties>
  <property name="com.arjuna.ats.jta.orphanSafetyInterval" value="20000"/>
  <property name="com.arjuna.ats.jta.xaAssumeRecoveryComplete" value="true"/>
</system-properties>
```

The external services need to be defined in the `standalone.xml` configuration file of Wildfly. An template of this definition is:

```XML
<subsystem xmlns="urn:jboss:domain:naming:2.0">
    <bindings>
        <simple name="java:global/geocode/hereApiKey" value="" type="java.lang.String"/>
        <simple name="java:global/carRegistrar/RDW/AppToken" value="" type="java.lang.String"/>
        <simple name="java:global/carRegistrar/RDW/VoertuigenUrl" value="https://opendata.rdw.nl/resource/m9d7-ebf2.json" type="java.lang.String"/>
        <simple name="java:global/carRegistrar/RDW/BrandstofUrl" value="https://opendata.rdw.nl/resource/8ys7-d773.json" type="java.lang.String"/>
        <simple name="java:global/openTripPlanner/apiUrl" value="" type="java.lang.String"/>
        <simple name="java:global/rideshare/apiUrl" value="" type="java.lang.String"/>
        <simple name="java:global/firebase/credentialsPath" value="${jboss.server.config.dir}/firebase-service-account-file.json" type="java.lang.String"/>
        <simple name="java:global/paymentClient/emspay/apiKey" value="" type="java.lang.String"/>
        <simple name="java:global/planner/disputeEmailAddress" value="" type="java.lang.String"/>
        <simple name="java:global/planner/senderEmailAddress" value="" type="java.lang.String"/>
        <simple name="java:global/planner/scheduledUpdatePublicTransportData" value="false" type="java.lang.Boolean"/>
        <simple name="java:global/report/earliestStartAt" value="2020-01-01" type="java.lang.String"/>
        <simple name="java:global/report/timeZone" value="Europe/Amsterdam" type="java.lang.String"/>
        <simple name="java:global/report/lookbackMonths" value="12" type="java.lang.Integer"/>
        <simple name="java:global/report/recipientEmailAddress" value="" type="java.lang.String"/>
        <simple name="java:global/report/subjectPrefix" value="" type="java.lang.String"/>
        <simple name="java:global/application/stage" value="" type="java.lang.String"/>
        <simple name="java:global/messageBird/accessKey" value="" type="java.lang.String"/>
        <simple name="java:global/messageBird/live/accessKey" value="" type="java.lang.String"/>
        <simple name="java:global/messageBird/test/accessKey" value="" type="java.lang.String"/>
        <simple name="java:global/profileService/delegateActivationCodeTTL" value="120" type="java.lang.Integer"/>
        <simple name="java:global/imageService/imageFolder" value="" type="java.lang.String"/>
    </bindings>
    <remote-naming/>
</subsystem>
```

You need to enter valid values for each property that has no value yet.

### Image Server
Netmobiel uses a simple file base image server. Any authenticated user can access the images. You only have to define the root folder of the image service in the application server, keyed by `java:global/imageService/imageFolder`.

## Configuration of Postgres
Netmobiel uses Postgres 10 as RDBMS, but the latest stable works probably as well. Download and install according instructions.

For the extended datasource, this version of Postgres required a change of setting in `postgresql.conf`:

```
max_prepared_transactions = 100		# zero disables the feature
```

Details about each database are given in the configuration section of each service.

Also install the PostGiS extension, it is required by most services.

## Configuration of OpenTripPlanner
The [OpenTripPlanner](http://docs.opentripplanner.org/en/latest/) (OTP) is open source and can be downloaded from their website. We used version 1.4. The new version (2.1) is probably a better option. Our version needed a heavy-weight server for building the graph for The Netherlands, about 12 Gb memory was required. 

OTP needs timetables to search for public transport and map data for car routing. For The Netherlands:
* The timetables from [GTFS OVapi NL](http://gtfs.ovapi.nl/nl/gtfs-nl.zip). 
* Map data from [OpenStreetMap](http://download.openstreetmap.fr/extracts/europe/netherlands.osm.pbf). 

The graph needs a periodic rebuild. We used to do the rebuild once a week. The scripts can be found in the `/etc` folder in the project root.

## Configuration of the API Gateway
For netmobiel we used the [Gravitee](https://ww.gravitee.io) API Gateway, community edition. The gateway uses quite a lot of resources, at 4G memory is required. Elastic search engine is also installed. Setup was also quite a lot of work, so is updating. You might consider a ready-to-run cloud service instead of running your own.

## External Services
You need to obtain api keys for the external services:
* [Firebase Cloud Messaging](https://firebase.google.com/products/cloud-messaging): A file needs to be placed in a secure place, e.g., in the configuration section of Wildfly. This file can be downloaded from the [Google Cloud Console](https://console.cloud.google.com), once you have enabled the API. 
* [HERE Geocoding & Search](https://www.here.com/platform/geocoding): Create an account and get the API key. It is free up to some point.
* [Messagebird SMS](https://messagebird.com/): Optional (more or less), only for sending text messages to mobile devices (paid service), mainly for authentication purposes for the delegation function. Not free.
* [RDW License Plate registration](https://opendata.rdw.nl/browse?category=Voertuigen&provenance=official): The Dutch registrar for the license plates of cars in The Netherlands. Obtain an API for free.

Added the API keys to the application server in the template as explained before.


