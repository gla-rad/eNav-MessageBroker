# e-Navigation Message Broker Service
The message broker repository contains the implementation of a service 
supporting the message exchange functionality required in the e-Navigation
architecture. In the context of e-Navigation this is usually under a
geographical location scope thus the 
[Geomesa](https://www.geomesa.org/documentation/stable/index.html) library is 
utilised. The core micro-service is built using the Springboot framework.

## Development Setup
To start developing just open ithe repository with the IDE of your choice. The
original code has been generated using
[Intellij IDEA](https://www.jetbrains.com/idea). Just open it by going to:

    File -> New -> Project From Version Control

Provide the URL of the current repository and the local directory you want.

You don't have to use it if you have another preference. Just make sure you
update the *.gitignore* file appropriately.

## Build Setup
The project is using the latest OpenJDK 8 to build, and only that should be 
used. The main issue is that the current Geomesa library only supports Java 8
at the moment. We can only upgrade after later JDK versions are also supported
by Geomesa.

To build the project you will need Maven, which usually comes along-side the
IDE. Nothing exotic about the goals, just clean and install should do:

    mvn clean package

## Configuration
The configuration of the eureka server is based on the properties files found
in the *main/resources* directory.

The *boostrap.properties* contains the necessary properties to start the service
while the *application.properties* everything else i.e. the security
configuration. 

Note that authentication is provided by Keycloak, so before you continue, you 
need to make sure that a keycloak server is up and running, and that a client
is registered. Once that is done, the service will required the following 
*application.properties* to be provided:

    keycloak.auth-server-url=<The keycloak address>
    keycloak.resource=<The client name>
    keycloak.credentials.secret=<The generated client sercet>

## Running the Service
To run the service, just like any other Springboot micro-service, all you need
to do is run the main class, i.e. MessageBroker. No further arguments are 
required. Everything should be picked up through the properties files.

## Description
The message broker receives generic inputs through a REST API. At the moment
it can support AtoN and Navigational Messages/Warnings. The following **POST**
endpoints are provided:
* /publish/atons
* /publish/messages (under development -- to be released soon)

Once a message has been posted onto one of these endpoints, it then gets passed
onto an Internal Spring channel and gets consumed by all services that are 
interested in propagating it. For example, for AtoNs these are:
* A service passing AtoN messages to the Geomesa Kafka Data Store
* A service passing AtoN messages to a web-socket for debugging purposes

## Contributing
Pull requests are welcome. For major changes, please open an issue first to 
discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
Distributed under the Apache License, Version 2.0. See [LICENSE](./LICENSE) for  
more information.

## Contact
Nikolaos Vastardis - Nikolaos.Vastardis@gla-rad.org



