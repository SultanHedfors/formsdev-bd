## Use the official Maven image to build the application
#FROM maven:3.9.4-eclipse-temurin-17 AS build
#
## Set the working directory inside the container
#WORKDIR /app
#
## Copy the pom.xml and the source code
#COPY pom.xml .
#COPY src ./src
#
## Build the Spring Boot application
#RUN mvn clean package -DskipTests
#
## Use a lighter JRE base image for the final image
#FROM eclipse-temurin:17-jre-alpine
#
## Set the working directory inside the container
#WORKDIR /app
#
## Copy the built application jar from the build image
#COPY --from=build /app/target/*.jar app.jar
#
## Expose the port on which the application will run
#EXPOSE 8080
#
## Run the Spring Boot application
#ENTRYPOINT ["java", "-jar", "app.jar"]
