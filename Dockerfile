# Use an official Maven image as the base image
FROM maven:3-eclipse-temurin-21-jammy AS build
# Set the working directory in the container
WORKDIR /app
# Copy the pom.xml and the project files to the container
COPY pom.xml .
COPY .mvn .mvn
COPY src ./src
# Build the application using Maven
RUN mvn clean package -DskipTests
# Use an official OpenJDK image as the base image
FROM maven:3-eclipse-temurin-21-jammy
# Set the working directory in the container
WORKDIR /app
# Copy the built JAR file from the previous stage to the container
COPY --from=build /app/target/fileStorage-0.0.1-SNAPSHOT.jar .
# Set the command to run the application
EXPOSE 8082
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://db-1:5432/fileStorage
CMD ["java", "-jar", "fileStorage-0.0.1-SNAPSHOT.jar"]
