FROM maven:3.8.1-jdk-11 AS build
RUN mkdir /app
COPY . /app
WORKDIR /app
# -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -B needed, otherwise log will be clipped (log limit reached)
RUN mvn clean install -DskipTests -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -B
RUN mkdir /dist
WORKDIR /dist
RUN unzip /app/projectforge-application/target/projectforge-application*.jar

FROM openjdk:11-buster

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
ARG DEPENDENCY=/dist
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

COPY --from=build --chown=projectforge:projectforge /app/docker/entrypoint.sh /app
RUN chmod 755 /app/entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]

MAINTAINER Micromata
