FROM openjdk:8

MAINTAINER Micromata

# Add the projectforge jar
ADD ./target/extra-resources/projectforge-application-6.14.0-SNAPSHOT.jar /usr/share/projectforge-application-6.14.0-SNAPSHOT.jar

# Add the properties
ADD ./configuration/default-application.properties /usr/share/application.properties

EXPOSE 8080

# TODO: run as unprivileged user
WORKDIR /usr/share

ENTRYPOINT ["/usr/bin/java", "-jar", "/usr/share/projectforge-application-6.14.0-SNAPSHOT.jar"]
