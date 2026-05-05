FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -Dmaven.test.skip=true


FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY --from=build /build/target/*.jar app.jar
COPY entrypoint-init-db.sh /app/entrypoint-init-db.sh
RUN chmod +x /app/entrypoint-init-db.sh

EXPOSE 8080

ENTRYPOINT ["/app/entrypoint-init-db.sh"]