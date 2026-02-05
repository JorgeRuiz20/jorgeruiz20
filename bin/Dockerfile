# Etapa 1: construir el proyecto con Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: ejecutar la aplicación compilada
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render usa la variable PORT automáticamente
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
