# Build like this:
#     docker build -t <version> -f Dockerfile ..
# e.g.
#     docker build -t glarad/enav-msg-broker:latest -f Dockerfile ..
#
# Run like this:
#     sudo docker run -t -i --rm -p 8763:8763 -v /path/to/config-directory/on/machine:/conf <image-id>
#
# You can also push to docker-hub like:
#     docker push glarad/enav-msg-broker:tagname
#
# A customized conf file (application.yaml) must be available in the folder mounted to /conf.
# When using in non-local environment it is recommended to generate new trust and keystores and place them in
# the conf-folder and point to them in application.yaml.
#
FROM eclipse-temurin:17-jre

LABEL org.opencontainers.image.source="https://github.com/gla-rad/eNav-MessageBroker"

ENV LOADER_PATH /modules

RUN mkdir /conf

RUN mkdir $LOADER_PATH

ADD target/message-broker-0.0.3-SNAPSHOT.jar .

EXPOSE 8763

CMD ["java", "-Dspring.profiles.active=prod", "-jar", "message-broker-0.0.3-SNAPSHOT.jar"]
