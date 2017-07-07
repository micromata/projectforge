FROM openjdk:8-alpine

MAINTAINER Micromata

# add projectforge user and create home directory
RUN adduser -D projectforge

# Add the projectforge jar
COPY ./target/extra-resources/projectforge-application-6.15.0-SNAPSHOT.jar /home/projectforge/projectforge-application.jar

# Add the properties
COPY ./configuration/default-application.properties /home/projectforge/application.properties

# run PF as user projectforge instead of root
USER projectforge

WORKDIR /home/projectforge

EXPOSE 8080

CMD ["/usr/bin/java", "-jar", "projectforge-application.jar"]
