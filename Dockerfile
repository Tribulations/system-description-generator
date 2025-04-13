### Dockerfile for SDG (Java app)
FROM openjdk:21-jdk-slim

# Set environment variables to ensure proper encoding inside the container
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8

# Set working directory
WORKDIR /app

# Copy JAR file into container
COPY target/system-description-generator-1.0-SNAPSHOT.jar ./system-description-generator.jar

# Copy .env for runtime environment variables
COPY .env .

# Expose port 8080
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "system-description-generator.jar"]

