# Use a base image with Java
FROM openjdk:11-jre-slim

# Set the working directory
WORKDIR /app

# Copy the jar file into the container
COPY target/my-app-1.0-SNAPSHOT.jar app.jar

# Expose the port your application runs on
EXPOSE 8080

# Command to run your application
CMD ["java", "-jar", "app.jar"]
