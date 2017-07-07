FROM openjdk:8

MAINTAINER Micromata

# Add the projectforge jar
COPY ./target/extra-resources/projectforge-application-6.15.0-SNAPSHOT.jar /usr/share/projectforge-application.jar

# Add the properties
COPY ./configuration/default-application.properties /usr/share/application.properties

EXPOSE 8080

# TODO: run as unprivileged user
WORKDIR /usr/share

ENTRYPOINT ["/usr/bin/java", "-jar", "/usr/share/projectforge-application.jar"]
