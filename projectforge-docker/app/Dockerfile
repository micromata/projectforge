FROM openjdk:11

# See: https://spring.io/guides/gs/spring-boot-docker/

# This is a Debian system, update system packages (if needed)
RUN apt-get update && apt-get -y upgrade

RUN addgroup projectforge && adduser --ingroup projectforge projectforge
USER projectforge:projectforge
# ProjectForge's base dir: must be mounted on host file system:
RUN mkdir $HOME/ProjectForge
VOLUME $HOME/ProjectForge

# Don't put fat jar files in docker images: https://phauer.com/2019/no-fat-jar-in-docker-image/
ARG DEPENDENCY=target/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app

# This application.properties is needed
COPY src/application.properties /


#ENTRYPOINT ["java","-cp","app:app/lib/*","org.projectforge.start.ProjectForgeApplication"]
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -cp app:app/lib/* org.projectforge.start.ProjectForgeApplication"]

MAINTAINER Micromata

# Add the projectforge/plugins jar
#COPY ./target/extra-resources/projectforge-application-$PF_VERSION.jar /home/pf/projectforge-application.jar
