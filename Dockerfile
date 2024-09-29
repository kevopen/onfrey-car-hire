# Stage 1: Build the application
FROM maven:3.8.5-openjdk-17 AS builder

# Set the working directory in the container
WORKDIR /app

# Copy the pom.xml and any other necessary files
COPY pom.xml . 
COPY src ./src

# Build the application
RUN mvn package -Pproduction

# Stage 2: Create the final image
FROM openjdk:17-slim

# Set the working directory in the container
WORKDIR /app

# Copy the JAR file from the builder stage
COPY --from=builder /app/target/my-app-1.0-SNAPSHOT.jar my-app.jar

# Command to run the application
CMD ["java", "-jar", "my-app.jar"]
