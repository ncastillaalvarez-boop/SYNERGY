# =====================================================================
#  Synergy (Spring Boot) en un contenedor
#  Se usa desde docker-compose.yml:  docker compose up --build
# =====================================================================

# 1) Compilar con Maven (las dependencias se descargan una vez y quedan en caché)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# 2) Imagen final, solo con Java y el .jar
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home synergy
USER synergy
COPY --from=build /app/target/*.jar synergy.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "synergy.jar"]
