# Use OpenJDK 21 as base image (matching the Java version in pom.xml)
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy the compiled JAR file
COPY target/transfer-system-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]