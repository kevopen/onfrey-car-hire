# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS builder
# Set the working directory in the container
WORKDIR /app
# Copy the pom.xml and any other necessary files
COPY pom.xml . 
COPY src ./src
# Build the application
RUN mvn package -Pproduction

# Stage 2: Create the final image
FROM eclipse-temurin:21-jre-jammy
# Set the working directory in the container
WORKDIR /app
# Copy the JAR file from the builder stage
COPY --from=builder /app/target/my-app-1.0-SNAPSHOT.jar my-app.jar
# Command to run the application
CMD ["java", "-jar", "my-app.jar"]
