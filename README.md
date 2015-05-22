Use cases for, [OAuth 2.0](http://tools.ietf.org/html/rfc6749)
---------------------------------------------------------------------------------------------------------------------

[![Build Status](https://travis-ci.org/RootServices/auth.svg?branch=development)](https://travis-ci.org/RootServices/auth)

Dependencies
------------
 - Postgres 9.3
 - Java 1.8
 - Maven 3.2.3

Contributing
------------
 - All code changes must have a story or bug written in Gherkin.
 - Follow the [setup](setup.md) instructions
 - All code must be written with the SOLID principles.
 - Unit and Integration tests are required.

Requesting Features and reporting bugs
-------------------------------------
 - Features are reported and tracked in [pivotal tracker](https://www.pivotaltracker.com/n/projects/1199316).
 - Reporting issues through github is acceptable. We will probably transfer them to PT.

Environment Variables for configuring db connection
---------------------------------------------------
```
export AUTH_DB_URL="jdbc:postgresql://127.0.0.1:5432/auth";
export AUTH_DB_USER="postgres";
export AUTH_DB_PASSWORD="";
export AUTH_DB_DRIVER="org.postgresql.Driver";
```

Running migrations (replace values where necessary).
----------------------------------------------------
```
mvn clean package -DskipTests
mvn flyway:migrate -Dflyway.user=postgres -Dflyway.password="" -Dflyway.url="jdbc:postgresql://127.0.0.1:5432/auth" -Dflyway.initOnMigrate=true
```

Running the tests from the terminal.
------------------------------------
 - Install all dependencies.
 - Set environment variables.
 - Create the db specified in AUTH_DB_URL.
 - Run migrations against the test db (see, Running Migrations)
 - Use maven to run the tests, `mvn test`

