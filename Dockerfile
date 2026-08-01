FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-code/pom.xml common-code/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY wallet-service/pom.xml wallet-service/pom.xml
COPY transaction-service/pom.xml transaction-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
RUN mvn -q -B -DskipTests dependency:go-offline || true
COPY . .
ARG MODULE
RUN mvn -q -B -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
ARG MODULE
COPY --from=build /workspace/${MODULE}/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
