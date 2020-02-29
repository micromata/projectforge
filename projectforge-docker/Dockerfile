FROM openjdk:8-jdk-alpine

# See: https://spring.io/guides/gs/spring-boot-docker/

RUN addgroup -S projectforge && adduser -D -S projectforge -G projectforge
USER projectforge:projectforge
# ProjectForge's base dir: must be mounted on host file system:
RUN mkdir /home/projectforge/ProjectForge
VOLUME /home/projectforge/ProjectForge

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
