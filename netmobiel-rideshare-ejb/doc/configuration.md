# Configuration

## Configuration of the database
The rideshare service uses Postgres (version 10.x) as relational database. The PostGis extensions is required.

For each database you need to create a login and the database itself.
### Add a Postgres user

Add a user (rideshare) with a password. Use the same values in the setup for WildFly.

```SQL
CREATE ROLE rideshare WITH
	LOGIN
	NOSUPERUSER
	NOCREATEDB
	NOCREATEROLE
	INHERIT
	NOREPLICATION
	CONNECTION LIMIT -1
	PASSWORD 'xxxxxx';
```
### Create the database
In the snippet the database name is `rideshare_dev`. You are free to give the database any name you prefer. Use whatever collation or character type you see fit. You might also use instead template0 or template 1.

```SQL
CREATE DATABASE rideshare_dev
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'English_Netherlands.1252'
    LC_CTYPE = 'English_Netherlands.1252'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;
```

COMMENT ON DATABASE banker_dev
    IS 'Carpool service database';
```

We are using the PostGIS extension. Use the following commands to add the extensions:

```SQL
create extension postgis;
create extension postgis_topology;
```

    
Repeat these steps for the integration test database (if required), with name `rideshare_test`. Because the test database is dropped and created by the application, the owner of the database must be set to `rideshare`. 

## Configuration of the datasource
The Netmobiel platform uses a separate XA datasource for each service. To add the database to Wildfly, stop Wildfly and add the following XML snippet to the standalone.xml at `<subsystem xmlns="urn:jboss:domain:datasources:5.0">/<datasources>`:

```XML
<xa-datasource jndi-name="java:jboss/datasources/rideshareDS" pool-name="rideshareDS">
    <xa-datasource-property name="ServerName">
        localhost
    </xa-datasource-property>
    <xa-datasource-property name="PortNumber">
        5432
    </xa-datasource-property>
    <xa-datasource-property name="DatabaseName">
        rideshare_dev
    </xa-datasource-property>
    <driver>postgres</driver>
    <security>
        <user-name>rideshare</user-name>
        <password>xxxxxx</password>
    </security>
    <validation>
        <valid-connection-checker class-name="org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker"/>
        <exception-sorter class-name="org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter"/>
    </validation>
</xa-datasource>
```

## Configuration of the test datasource
The test database is used to run integration test with Maven. If you don't intend to run integration tests, then there is no need for a test database. 
Acceptance and production servers don't need a test database either.

To add a test database to Wildfly, stop Wildfly and add the following XML snippet to the standalone.xml at `<subsystem xmlns="urn:jboss:domain:datasources:5.0">/<datasources>`:

```XML
<datasource jndi-name="java:jboss/datasources/rideshare-testDS" pool-name="rideshare-testDS">
    <connection-url>jdbc:postgresql://localhost:5432/rideshare_test</connection-url>
    <driver>postgres</driver>
    <security>
        <user-name>rideshare</user-name>
        <password>xxxxxx</password>
    </security>
</datasource>
```


## JNDI definitions
TBD

Rideshare uses the Dutch RDW for retrieving information about cars keyed by the license plate. Define the following JNDI entries:
* java:global/licensePlate/RDWAppToken: The access token issued by the RDW.

