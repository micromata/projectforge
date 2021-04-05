FROM openjdk:11

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

# This application.properties is needed
COPY src/application.properties /

ARG JAVA_OPTS="-Xms1g -Xmx1g"
ENV JAVA_OPTS_VAR=$JAVA_OPTS

# -Dloader.path=${HOME}/ProjectForge/resources/milton ${DEBUGOPTS}

# Variable expansion doesn't work for ENTRYPOINT definition as array.
#ENTRYPOINT ["java","-cp","app:app/lib/*","org.projectforge.start.ProjectForgeApplication"]
ENTRYPOINT java ${JAVA_OPTS_VAR} -cp app:app/lib/*:/ProjectForge/plugins/* -Dprojectforge.docker.mode=y -Dprojectforge.plugins.dir=/ProjectForge/plugins org.projectforge.start.ProjectForgeApplication

MAINTAINER Micromata
