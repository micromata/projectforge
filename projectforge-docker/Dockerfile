FROM openjdk:8-alpine

MAINTAINER Micromata

# add projectforge user and create home directory
RUN adduser -D pf

# Add the projectforge jar
COPY ./target/extra-resources/projectforge-application-6.19.0-SNAPSHOT.jar /home/pf/projectforge-application.jar

# Add the properties
COPY ./target/extra-resources/application.properties /home/pf/

# run PF as user projectforge instead of root
USER pf

WORKDIR /home/pf

EXPOSE 8080

CMD ["java", "-jar", "projectforge-application.jar"]
