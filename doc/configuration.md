# Configuration

## Configuration of Keycloak
Netmobiel uses the [Keycloak Open Source Identity and Access Management Server](https://www.keycloak.org/) as its single sign-on solution/. At the time of writing we used version 15.0. To use Keycloak for Netmobiel, the global roles need to be configured as well as the clients.

First install Keycloak, following the procedures in the Keycloak documentation. Then create a realm in Keycloak for Netmobiel. The realm is the container for all other configuration steps. Configure the realm according the keycloak instructions.

### Global roles
Add the following roles:
* admin -  A role with unrestricted great power in Netmobiel.
* treasurer - Responsible for the credit management in NetMobiel. The treasurer is allowed to run a payment batch to execute withdrawal requests. 
* delegate - Users with this role may act on behalf of other users in Netmobiel (for informal care, the caregiver).

The roles can only be assigned from the Keycloak console.

### Client applications
Each back-end service with a REST Api has a client application defined in Keycloak. Add the following clients with access type `bearer-only`.
* banker-service
* communicator-service
* geo-service
* planner-service
* profile-service
* rideshare-service 

The frontend application needs also a representation, so add another client:
* netmobiel-frontend (with access type `public`). Enable standard flow. Add valid redirection urls and web origins.


If you want to use [Postman](https://www.postman.com/) for testing, it is useful to add another client that can be used to fetch a token from, e.g. netmobiel-postman. Configure accoirding requirements of Postman.

## Configuration of Wildfly
Netmobiel uses the Wildfly application server (version 17 at the time of writing). Download and install Wildfly according instructions. 

Install the client adapter of Keycloak for Wildfly, follow the instructions.

Because of the distributed databases, extended datasources need to be defined. This is explained in the configuration section of each Netmobiel service.

Wildfly 17.0 kept endlessly complaining about attempts to roll-back failed transactions. In production usefull, but during development it is of no use. Disable the feature with the following fragment in `standalone.xml`.

```XML
<system-properties>
  <property name="com.arjuna.ats.jta.orphanSafetyInterval" value="20000"/>
  <property name="com.arjuna.ats.jta.xaAssumeRecoveryComplete" value="true"/>
</system-properties>
```

## Configuration of Postgres
Netmobiel uses Postgres 10 as RDBMS. Download and install according instructions.

For the extended datasource, this version of Postgres required a chnage of setting in `postgresql.conf`:

```
max_prepared_transactions = 100		# zero disables the feature
```

Details about each database are given in the configuration section of each service.

Also install the PostGiS extension. 
