FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

# Copy the host-compiled jar directly to bypass slow in-container dependency compilation
COPY target/control-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
