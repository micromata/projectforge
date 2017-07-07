FROM openjdk:8-alpine

MAINTAINER Micromata

# add projectforge user and create home directory
RUN adduser -D pf

# Add the projectforge jar
COPY ./target/extra-resources/projectforge-application-6.15.0-SNAPSHOT.jar /home/pf/projectforge-application.jar

# Add the properties
COPY ./configuration/default-application.properties /home/pf/application.properties

# run PF as user projectforge instead of root
USER pf

WORKDIR /home/pf

EXPOSE 8080

# Use /home/pf/config as projectforge base directory.
# You can use this as mount point for external configuration.
CMD ["java", "-Dprojectforge.base.dir=/home/pf/config", "-jar", "projectforge-application.jar", "--spring.config.location=file:/home/pf/config/"]
