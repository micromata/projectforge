FROM openjdk:21-slim

# Argument for JAR file name to use in working directory:
ARG JAR_FILE

# Set working directory
WORKDIR /app

RUN mkdir -p /app

# Copy the specified JAR file into the container
COPY ${JAR_FILE} /app/application.jar
COPY docker/entrypoint.sh /app/entrypoint.sh

# Set permissions for the entrypoint script
RUN chmod +x /app/entrypoint.sh

# Expose application port
EXPOSE 8080

# Use a non-root user
RUN addgroup --system projectforge && adduser --system --ingroup projectforge projectforge

# Install findutils (xargs needed by gradle)
#RUN apt-get update && apt-get install -y findutils && rm -rf /var/lib/apt/lists/*
# pgrep needed by the entrypoint.sh
RUN apt-get update && apt-get install -y procps && rm -rf /var/lib/apt/lists/*


# Expose the port and declare the volume
RUN mkdir -p /ProjectForge && chown -R projectforge:projectforge /ProjectForge
VOLUME /ProjectForge

COPY docker/environment.sh /ProjectForge

# Switch to the non-root user
USER projectforge:projectforge

# Run the Spring Boot application
ENTRYPOINT ["/app/entrypoint.sh"]
# ENTRYPOINT ["java", "-jar", "/app/application.jar"]

LABEL maintainer="Micromata"
