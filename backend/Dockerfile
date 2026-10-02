FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build \
    /app/digitalwallet-start/target/*.jar \
    app.jar

EXPOSE 3006
ENTRYPOINT ["java", "-jar", "app.jar"]