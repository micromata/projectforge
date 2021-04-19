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

COPY --chown=borgbutler:borgbutler entrypoint.sh /app
RUN chmod 755 /app/entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]

MAINTAINER Micromata
