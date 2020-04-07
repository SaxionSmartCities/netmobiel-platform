# Configuration

## Configuration of the database
The Netmobiel platform uses a separate XA datasource for each service. To add the database to Wildfly, stop Wildfly and add the following XML snippet to the standalone.xml at `<subsystem xmlns="urn:jboss:domain:datasources:5.0">/<datasources>`:

```XML
<xa-datasource jndi-name="java:jboss/datasources/bankerDS" pool-name="bankerDS">
    <xa-datasource-property name="ServerName">
        localhost
    </xa-datasource-property>
    <xa-datasource-property name="PortNumber">
        5432
    </xa-datasource-property>
    <xa-datasource-property name="DatabaseName">
        banker_dev
    </xa-datasource-property>
    <driver>postgres</driver>
    <security>
        <user-name>aDatabaseUser</user-name>
        <password>thePassword</password>
    </security>
    <validation>
        <valid-connection-checker class-name="org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker"/>
        <exception-sorter class-name="org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter"/>
    </validation>
</xa-datasource>
```
In the snippet the database name is `banker_dev`. You are free to give the database any name you prefer. From an old hand I learned to distinguish explicitly between the databases used in different develop stages to prevent accidents, especially with the production database.    

## Configuration of the test database
The test database is used to run integration test with Maven. If you don't intend to run integration tests, then there is no need for a test database. 
Acceptance and production servers don't need a test database either.

To add a test database to Wildfly, stop Wildfly and add the following XML snippet to the standalone.xml at `<subsystem xmlns="urn:jboss:domain:datasources:5.0">/<datasources>`:

```XML
<datasource jndi-name="java:jboss/datasources/banker-testDS" pool-name="banker-testDS">
    <connection-url>jdbc:postgresql://localhost:5432/banker_test</connection-url>
    <driver>postgres</driver>
    <security>
        <user-name>aDatabaseUser</user-name>
        <password>thePassword</password>
    </security>
</datasource>
```


