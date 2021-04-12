FROM openjdk:16-buster

# See: https://spring.io/guides/gs/spring-boot-docker/

# This is a Debian system, update system packages (if needed)
RUN apt-get update && apt-get -y upgrade

RUN addgroup projectforge && adduser --ingroup projectforge projectforge
# ProjectForge's base dir: must be mounted on host file system:
RUN mkdir /ProjectForge
# Grant access for user projectforge:
RUN chown projectforge.projectforge /ProjectForge
VOLUME /ProjectForge
EXPOSE 8080

USER projectforge:projectforge

# Don't put fat jar files in docker images: https://phauer.com/2019/no-fat-jar-in-docker-image/
ARG DEPENDENCY=target/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app

#ARG JAVA_OPTS="-Xms1g -Xmx1g"
#ENV JAVA_OPTS_VAR=$JAVA_OPTS

# -Dloader.path=${HOME}/ProjectForge/resources/milton ${DEBUGOPTS}

# Variable expansion doesn't work for ENTRYPOINT definition as array, but array is required, because graceful shutdown of
# container isn't given if java is started via 'sh -c' as it will be done by ENTRYPOINT java .....
# Java options are modifiable by user through own ENTRYPOINT definition on docker run or in docker-compose.yml.
ENTRYPOINT ["java", "-Xms1g", "-Xmx1g", "-cp", "app:app/lib/*:/ProjectForge/plugins/*", "-Dprojectforge.setup=docker", "-Dprojectforge.plugins.dir=/ProjectForge/plugins", "org.projectforge.start.ProjectForgeApplication"]

MAINTAINER Micromata
