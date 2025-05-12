
FROM openjdk:17-slim
WORKDIR /app


# Instalacja zależności, w tym fontconfig, libfreetype6 i czcionek DejaVu
RUN apt-get update && apt-get install -y \
    libfreetype6 \
    fontconfig \
    fonts-dejavu \
    && rm -rf /var/lib/apt/lists/*


COPY target/be-app-0.0.1-SNAPSHOT.jar /app/be-app.jar

COPY src/main/resources/application-prod.properties /app/application-prod.properties

# Ustawienie zmiennych środowiskowych, które będą używane w aplikacji
ENV SPRING_DATASOURCE_URL=jdbc:firebirdsql://prod-db-host:3050/Path/to/prod-db.FDB
ENV SPRING_DATASOURCE_USERNAME=prod-user
ENV SPRING_DATASOURCE_PASSWORD=prod-password
ENV FRONTEND_URL=http://prod-frontend.com
ENV UPLOAD_DIRECTORY=/path/to/prod/uploads

# Eksponowanie portu 8080, na którym będzie działać aplikacja
EXPOSE 8080

# Uruchomienie aplikacji z profilem produkcyjnym
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "be-app.jar"]
