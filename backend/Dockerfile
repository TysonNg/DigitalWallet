FROM eclipse-temurin:21-jre

WORKDIR /app

COPY digitalwallet-start/target/digitalwallet-start-*.jar app.jar

EXPOSE 3006
ENTRYPOINT ["java", "-jar", "app.jar"]