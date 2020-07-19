# Tokensmith
Tokensmith is a Java implementation of an [OAuth 2.0](http://tools.ietf.org/html/rfc6749) and [OIDC](https://openid.net/) Identity server.

This documentation is intended for a developer audience. More detailed documentation is [in progress](https://github.com/tokensmith/website).

- [Run the server](#run-the-server)
- [Seed the database](#seed-the-database)
- [Published messages](#published-messages)
- [Interaction](#interaction)
- [Request features and report bugs](#request-features-and-report-bugs)
- [Project layout](#project-layout)


## Run the server

### Locally

#### Start Postgres, Zookeeper, Kafka, and [Message-User](https://github.com/tokensmith/message-user) 
```bash
make run
```

#### Stop Postgres, Zookeeper, Kafka, and [Message-User](https://github.com/tokensmith/message-user) 
```bash
make stop
```

#### Compile the application
```bash
./gradlew clean install
```

#### Start the application
```bash
java -jar http/build/libs/http-0.0.1-SNAPSHOT.war
```

Or, from an IDE the main method is in [TokenSmithServer](http/src/main/java/net/tokensmith/authorization/http/server/TokenSmithServer.java)

#### Configuration

The server gets configured in a [properties file](http/src/main/resources/application-default.properties). 
The values can be overidden with command line arguments or environment variables. More information for how to overide 
the default values is available in [spring's docs](https://docs.spring.io/spring-boot/docs/1.3.0.M4/reference/html/boot-features-external-config.html). 

 - Arguments can passed in as, `-DallowLocalUrls=false`
 - Environment variables can be set as, `export allowLocalUrls=false`

## Seed the database

The database automatically gets seeded with a few [database migrations](https://github.com/tokensmith/tokensmith/tree/development/core/src/main/resources/db/migration). 

## Published messages
Tokensmith publishes messages to kafka when:
 - A user [registers](core/src/main/java/net/tokensmith/authorization/register/Register.java#L97) through interacting with `/register` 
 - A user requests to [reset their password](core/src/main/java/net/tokensmith/authorization/nonce/reset/ForgotPassword.java#L68) through interacting with `/forgot-password`.
 - A user [resets their password](core/src/main/java/net/tokensmith/authorization/nonce/reset/ForgotPassword.java#L107).

The [Message-User](https://github.com/tokensmith/message-user) worker will then template the messages and send the emails.

## Interaction

See the [HTTP](http/README.md) readme for documents on interacting with Tokensmith.

## Request features and report bugs
 - Use the [project's github issues](https://github.com/tokensmith/tokensmith/issues).
 - Include links to the RFCs that relate to the feature or bug.

## Project layout
This repo has multiple gradle projects.

### [http](http)
Everything related to accepting and responding to HTTP requests
### [core](core)
Use cases for supporting OAuth2 and OIDC.
### [repository](repository)
Entities and Repository interfaces
### [login](login)
An SDK to interact with Tokensmith (OIDC ID Server).