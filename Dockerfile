# Stage 1: Build the application jar
FROM eclipse-temurin:24-jdk-alpine AS builder
WORKDIR /build

# Copy maven wrapper and pom file to leverage Docker layer caching for dependencies
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./

# Pre-fetch dependencies (clean build container setup)
RUN ./mvnw dependency:go-offline -B

# Copy source code and build the package
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Create runtime container
FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /build/target/control-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
