FROM openjdk:17-slim
WORKDIR /app

RUN apt-get update && apt-get install -y \
    fontconfig \
    fonts-dejavu \
    libfreetype6 \
    && rm -rf /var/lib/apt/lists/*

# Kopiuj JAR
COPY target/be-app-0.0.1-SNAPSHOT.jar be-app.jar


EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=docker-local", "-jar", "be-app.jar"]
