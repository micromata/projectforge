FROM openjdk:8-alpine
ARG PF_VERSION

MAINTAINER Micromata

# add projectforge user and create home directory
RUN adduser -D pf

# Add the projectforge/plugins jar
COPY ./target/extra-resources/projectforge-application-$PF_VERSION.jar /home/pf/projectforge-application.jar

# Add the properties
COPY ./target/extra-resources/application.properties /home/pf/

# run PF as user projectforge instead of root
USER pf

WORKDIR /home/pf

EXPOSE 8080

# ENTRYPOINT ["tail", "-f", "/dev/null"]
ENTRYPOINT ["java", "-jar", "projectforge-application.jar"]