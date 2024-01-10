# The GLA e-Navigation Service Architecture - Message Broker Service

## Quick Reference
* Maintained by:<br/>
[GRAD](https://www.gla-rad.org/)
* Where to get help:<br/>
[Unix & Linux](https://unix.stackexchange.com/help/on-topic),
[Stack Overflow](https://stackoverflow.com/help/on-topic),
[GRAD Wiki](https://rnavlab.gla-rad.org/wiki/E-Navigation_Service_Architecture)
(for GRAD members only)

## What is e-Navigation
The maritime domain is facing a number for challenges, mainly due to the
increasing demand, that may increase the risk of an accident or loss of life.
These challenges require technological solutions and e-Navigation is one such
solution. The International Maritime Organization ([IMO](https://www.imo.org/))
adopted a ‘Strategy for the development and implementation of e‐Navigation’
(MSC85/26, Annexes 20 and 21), providing the following definition of
e‐Navigation:

<div style="padding: 4px;
    background:lightgreen;
    border:2px;
    border-style:solid;
    border-radius:20px;
    color:black">
E-Navigation, as defined by the IMO, is the harmonised collection, integration,
exchange, presentation and analysis of maritime information on-board and ashore
by electronic means to enhance berth-to-berth navigation and related services,
for safety and security at sea and protection of the marine environment.
</div>

In response, the International Association of Lighthouse Authorities 
([IALA](https://www.iala-aism.org/)) published a number of guidelines such as 
[G1113](https://www.iala-aism.org/product/g1113/) and
[G1114](https://www.iala-aism.org/product/g1114/), which establish the relevant
principles for the design and implementation of harmonised shore-based technical
system architectures and propose a set of best practices to be followed. In
these, the terms Common Shore‐Based System (CSS) and Common Shore‐based System
Architecture (CSSA) were introduced to describe the shore‐based technical system
of the IMO’s overarching architecture.

To ensure the secure communication between ship and CSSA, the International
Electrotechnical Commission (IEC), in coordination with IALA, compiled a set of
system architecture and operational requirements for e-Navigation into a
standard better known as [SECOM](https://webstore.iec.ch/publication/64543).
This provides mechanisms for secure data exchange, as well as a TS interface
design that is in accordance with the service guidelines and templates defined
by IALA. Although SECOM is just a conceptual standard, the Maritime Connectivity
Platform ([MCP](https://maritimeconnectivity.net/)) provides an actual
implementation of a decentralised framework that supports SECOM.

## What is the GRAD e-Navigation Service Architecture

The GLA follow the developments on e-Navigation closely, contributing through
their role as an IALA member whenever possible. As part of their efforts, a
prototype GLA e-Navigation Service Architecture is being developed by the GLA
Research and Development Directorate (GRAD), to be used as the basis for the
provision of the future GLA e-Navigation services.

As a concept, the CSSA is based on the Service Oriented Architecture (SOA). A
pure-SOA approach however was found to be a bit cumbersome for the GLA
operations, as it usually requires the entire IT landscape being compatible,
resulting in high investment costs. In the context of e-Navigation, this could
become a serious problem, since different components of the system are designed
by independent teams/manufacturers. Instead, a more flexible microservice
architecture was opted for. This is based on a break-down of the larger
functional blocks into small independent services, each responsible for
performing its own orchestration, maintaining its own data and communicating
through lightweight mechanisms such as HTTP/HTTPS. It should be pointed out that
SOA and the microservice architecture are not necessarily that different.
Sometimes, microservices are even considered as an extension or a more
fine-grained version of SOA.

## The e-Navigation Message Broker Service

IALA Guideline G1114 makes reference to a Maritime Messaging Service amongst its
value-added data processing services. In the current implementation, the
“Message Broker” assumes this role and facilitates a geospatially-aware
publish-subscribe communication pattern (through the use of the
[GeoMesa](https://www.geomesa.org) library), where the senders of messages
(publishers) do not program the messages to be send directly to a specific
receiver (consumers). Instead, publishers submit the messages to a specific
topic, so that all authorised services interested in that topic and the affected
geographical location will receive them. The topics can be defined as a
character string representation of the message type such as “aton” or
“aton/virtual”. The latter topic format can be used to provide a more
fine-grained topic categorisation.

## How to use this image

This image can be used in two ways (based on the use or not of the Spring Cloud
Config server). 
* Enabling the cloud config client and using the configurations located in an 
online repository.
* Disabling the cloud config client and using the configuration provided
locally.

### Cloud Config Configuration

In order to run the image in a **Cloud Config** configuration, you just need
to provide the environment variables that allow is to connect to the cloud
config server. This is assumed to be provided the GRAD e-Navigation Service
Architecture
[Eureka Service](https://hub.docker.com/repository/docker/glarad/enav-eureka/).

The available environment variables are:
    
    ENAV_CLOUD_CONFIG_URI=<The URL of the eureka cloud configuration server>
    ENAV_CLOUD_CONFIG_BRANCH=<The cloud configuration repository branch to be used>
    ENAV_CLOUD_CONFIG_USERNAME=<The cloud configration server username>
    ENAV_CLOUD_CONFIG_PASSWORD=<The cloud configration server password>
    
The parameters will be picked up and used to populate the default
**bootstrap.properties** of the service that look as follows:

    server.port=8763
    spring.application.name=msg-broker
    spring.application.version=<application.version>
    
    # The Spring Cloud Discovery Config
    spring.cloud.config.uri=${ENAV_CLOUD_CONFIG_URI}
    spring.cloud.config.username=${ENAV_CLOUD_CONFIG_USERNAME}
    spring.cloud.config.password=${ENAV_CLOUD_CONFIG_PASSWORD}
    spring.cloud.config.label=${ENAV_CLOUD_CONFIG_BRANCH}
    spring.cloud.config.fail-fast=false

As you can see, the service is called **msg-broker** and uses the **8763** port
when running.

To run the image, along with the aforementioned environment variables, you can
use the following command:

    docker run -t -i --rm \
        -p 8763:8763 \
        -e ENAV_CLOUD_CONFIG_URI='<cloud config server url>' \
        -e ENAV_CLOUD_CONFIG_BRANCH='<cloud config config repository branch>' \
        -e ENAV_CLOUD_CONFIG_USERNAME='<config config repository username>' \
        -e ENAV_CLOUD_CONFIG_PASSWORD='<config config repository passord>' \
        <image-id>

### Local Config Configuration

In order to run the image in a **Local Config** configuration, you just need
to mount a local configuration directory that contains the necessary 
**.properties** files (including bootstrap) into the **/conf** directory of the
image.

This can be done in the following way:

    docker run -t -i --rm \
        -p 8763:8763 \
        -v /path/to/config-directory/on/machine:/conf \
        <image-id>

Examples of the required properties files can be seen below.

For bootstrapping, we need to disable the cloud config client, and clear our the
environment variable inputs:
    
    server.port=8763
    spring.application.name=msg-broker
    spring.application.version=<application.version>
    
    # Disable the cloud config
    spring.cloud.config.enabled=false
    
    # Clear out the environment variables
    spring.cloud.config.uri=
    spring.cloud.config.username=
    spring.cloud.config.password=
    spring.cloud.config.label=

While the application properties need to provide the service with an OAuth2.0
server like keycloak, logging configuration, the eureka client connection etc.:

    # Configuration Variables
    service.variable.hostname=<service.hostname>
    service.variable.eureka.server.name=<eureka.server.name>
    service.variable.eureka.server.port=<eureka.server.port>
    service.variable.keycloak.server.name=<keycloak.server.name>
    service.variable.keycloak.server.port=<keycloak.server.port>
    service.variable.keycloak.server.realm=<keycloak.realm>
    service.variable.kafka.server.name=<kafka.server.name>
    service.variable.kafka.server.broker.port=<kafka.server.port>
    service.variable.kafka.server.zookeeper.port=<zookeeper.server.port>
    
    # Eureka Client Configuration
    eureka.client.service-url.defaultZone=http://${service.variable.eureka.server.name}:${service.variable.eureka.server.port}/eureka/
    eureka.client.registryFetchIntervalSeconds=5
    eureka.instance.preferIpAddress=true
    eureka.instance.leaseRenewalIntervalInSeconds=10
    eureka.instance.hostname=${service.variable.hostname}
    eureka.instance.metadata-map.startup=${random.int}
    
    # Spring-boot Admin Configuration
    spring.boot.admin.client.url=http://${service.variable.server.eureka.name}:${service.variable.server.eureka.port}/admin
    
    # Logging Configuration
    logging.file.name=/var/log/${spring.application.name}.log
    logging.logback.rollingpolicy.max-file-size=10MB
    logging.logback.rollingpolicy.file-name-pattern=${spring.application.name}-%d{yyyy-MM-dd}.%i.log
    
    # Management Endpoints
    management.endpoint.logfile.external-file=/var/log/${spring.application.name}.log
    management.endpoints.web.exposure.include=*
    management.endpoint.health.show-details=always
    management.endpoint.httpexchanges.enabled=true
    management.endpoint.health.probes.enabled: true
    
    # Springdoc configuration
    springdoc.swagger-ui.path=/swagger-ui.html
    springdoc.packagesToScan=org.grad.eNav.msgBroker.controllers
    
    # Keycloak Configuration
    spring.security.oauth2.client.registration.keycloak.client-id=msg-broker
    spring.security.oauth2.client.registration.keycloak.client-secret=<changeit>
    spring.security.oauth2.client.registration.keycloak.client-name=Keycloak
    spring.security.oauth2.client.registration.keycloak.provider=keycloak
    spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
    spring.security.oauth2.client.registration.keycloak.scope=openid
    spring.security.oauth2.client.registration.keycloak.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
    spring.security.oauth2.client.provider.keycloak.issuer-uri=http://${service.variable.keycloak.server.name}:${service.variable.keycloak.server.port}/realms/${service.variable.keycloak.server.realm}
    spring.security.oauth2.client.provider.keycloak.user-name-attribute=preferred_username
    spring.security.oauth2.resourceserver.jwt.issuer-uri=http://${service.variable.keycloak.server.name}:${service.variable.keycloak.server.port}/realms/${service.variable.keycloak.server.realm}
    
    # Kafka Configuration
    kafka.brokers=${service.variable.kafka.server.name}:${service.variable.kafka.server.broker.port}
    kafka.zookeepers=${service.variable.kafka.server.name}:${service.variable.kafka.server.zookeeper.port}
    kafka.consumer.count=1
    
    # Web Socket Configuration
    gla.rad.msg-broker.web-socket.name=msg-broker-websocket
    gla.rad.msg-broker.web-socket.prefix=topic
    
    # Front-end Information
    gla.rad.msgBroker.info.name=Message Broker
    gla.rad.msgBroker.info.version=${spring.application.version}
    gla.rad.msgBroker.info.operatorName=Research and Development Directorate of GLA of UK and Ireland
    gla.rad.msgBroker.info.operatorContact=Nikolaos.Vastardis@gla-rad.org
    gla.rad.msgBroker.info.operatorUrl=https://www.gla-rad.org/
    gla.rad.msgBroker.info.copyright=\u00A9 2023 GLA Research & Development

## Operation

The microservice is built as an HTTP REST overlay over a Kafka message broker,
while the GeoMesa library is used to provide a spatial semantics layer on top
of the published data. In other words, this means that the data (such as the
S-125 datasets produced by Niord) can be published in a topic and be tagged with
a specific location or area. This allows all interested entities that monitor
the Kafka message broker using the GeoMesa library, to only select the
publications that not only match the selected topic, but also their respective
areas of interest.

The service exposes two REST interfaces, one for allowing the publication of
new AtoN information and one for deleting existing AtoN entries respectively.
The publication interface, as documented by Swagger and shown in Figure 8,
accepts a request with the AtoN Unique Identifier (UID) and geometry (or 
bounding box) parameters, while the S-125 encoded description of the AtoN
information is passed in the body of the request. The deletion interface on the
other hand, only requires the AtoN UID information, in order to inform the 
subscribed listeners that the corresponding AtoN will no longer be available.

Both the creation and deletion endpoints result in a message publication on the
same Kafka topic, more specifically in this case the “S125” topic. Since the
“Message Broker” microservice is using the GeoMesa library, it makes use of a
specific data structure called a **Feature** to make the data available to the
subscribers. These “features” can contain any number of predefined of
attributes, which in this case are the AtoN UID, the AtoN geometry (or bounding
box), and of course the AtoN information content in S-125 encoding. When a
feature gets published, the Kafka broker will ensure its delivery to all
subscribed entities. These can then query and filter the received features based
on their attributes (e.g. geometry), to ensure that only the appropriate
information is received.

## Contributing
For contributing in this project, please have a look at the Github repository
[eNav-MessageBroker](https://github.com/gla-rad/eNav-MessageBroker). Pull
requests are welcome. For major changes, please open an issue first to discuss
what you would like to change.

Please make sure to update tests as appropriate.

## License
Distributed under the Apache License, Version 2.0.

## Contact
Nikolaos Vastardis - 
[Nikolaos.Vastardis@gla-rad.org](mailto:Nikolaos.Vastardis@gla-rad.org)
