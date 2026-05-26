# Stage 1: Build the application
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/CloudStorage-0.0.1-SNAPSHOT.jar /app/cloudstorage.jar
ENTRYPOINT ["java", "-jar", "/app/cloudstorage.jar"]
