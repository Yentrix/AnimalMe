FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -Dmaven.test.skip=true


FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

# Directorios para almacenamiento persistente de archivos
RUN mkdir -p /app/uploads/users /app/uploads/pets /app/uploads/publications

VOLUME ["/app/uploads"]

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]