Use cases for, [OAuth 2.0](http://tools.ietf.org/html/rfc6749)
---------------------------------------------------------------------------------------------------------------------

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
```bash
$ export AUTH_DB_URL="jdbc:postgresql://127.0.0.1:5432/auth";
$ export AUTH_DB_USER="postgres";
$ export AUTH_DB_PASSWORD="";
$ export ISSUER="https://sso.rootservices.org"
$ export ALLOW_LOCAL_URLS="true"
$ export ALLOW_HTTP_URLS="true"
```

Environment Variable for publishing to a message queue
----------------------------------------------

See pelican for details.

```bash
$ export MESSAGE_QUEUE_HOST='localhost:9092'
```

Running migrations (replace values where necessary).
----------------------------------------------------
```
$ ./gradlew -x test clean build
$ ./gradlew flywayMigrate -Dflyway.user=postgres -Dflyway.password="" -Dflyway.url="jdbc:postgresql://127.0.0.1:5432/auth"  
```

Running the tests from the terminal.
------------------------------------
 - Install all dependencies.
 - Set environment variables.
 - Create the db specified in AUTH_DB_URL.
 - Run migrations against the test db (see, Running Migrations)
 - Use maven to run the tests, `mvn test`

Interact with postgres
-----------------------

```bash
$ docker exec -it postgres bash

root@8581e63f4474:/# psql -U postgres
postgres=# \c auth;
auth=# \dt
```