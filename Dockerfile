# Build multi-etapes (SCRUM-36) : l'etape "build" compile le jar avec
# Maven, l'image finale ne contient que le JRE + le jar -- pas de Maven
# ni de code source dans l'image qui tourne en production.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copie pom.xml seul d'abord : tant que les dependances ne changent pas,
# Docker reutilise cette couche meme si le code source change ensuite.
COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
