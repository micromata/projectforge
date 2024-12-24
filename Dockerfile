# docker buildx build --platform linux/arm64 --build-arg JAR_FILE=projectforge-application-8.0.jar -t projectforge:arm64 -t projectforge:8.0 -t projectforge:latest \--load .
# docker buildx build --platform linux/amd64 --build-arg JAR_FILE=projectforge-application-8.0.jar -t projectforge:amd64 -t projectforge:8.0 -t projectforge:latest \--load .

# Stage 1: Build stage
FROM openjdk:17-slim AS build

# Argument to accept JAR file name
ARG JAR_FILE

RUN mkdir -p /app
COPY ${JAR_FILE} /app
COPY docker/entrypoint.sh /app/docker/entrypoint.sh
WORKDIR /app

# Install Gradle wrapper and build project
# Optional: Uncomment if offline dependencies are required
# RUN ./gradlew dependencies --refresh-dependencies

# Install findutils (xargs needed by gradle)
#RUN apt-get update && apt-get install -y findutils && rm -rf /var/lib/apt/lists/*
RUN apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/*

#RUN chmod +x ./gradlew

# Build the project
#RUN ./gradlew build -x test

# Extract the built application
RUN mkdir /dist
RUN unzip /app/${JAR_FILE} -d /dist

# Stage 2: Runtime stage
FROM openjdk:17-slim

# Update system packages (optional, only if needed)
RUN apt-get update && apt-get -y upgrade

# Create a dedicated user and group for the application
RUN addgroup --system projectforge && adduser --system --ingroup projectforge projectforge

# Set up the application base directory
RUN mkdir /ProjectForge && chown projectforge:projectforge /ProjectForge
VOLUME /ProjectForge
EXPOSE 8080

# Switch to the non-root user
USER projectforge:projectforge

# Set application dependencies and configuration
ARG DEPENDENCY=/dist
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

# Add entrypoint script
COPY --from=build --chown=projectforge:projectforge /app/docker/entrypoint.sh /app
RUN chmod 755 /app/entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]

MAINTAINER Micromata
