# Stage 1: Build the backend application
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy root pom and module poms for dependency caching
COPY pom.xml ./
COPY digitalwallet-domain/pom.xml ./digitalwallet-domain/
COPY digitalwallet-application/pom.xml ./digitalwallet-application/
COPY digitalwallet-infrastructure/pom.xml ./digitalwallet-infrastructure/
COPY digitalwallet-controller/pom.xml ./digitalwallet-controller/
COPY digitalwallet-start/pom.xml ./digitalwallet-start/

# Download dependencies (fail-safe for offline cache)
RUN mvn dependency:go-offline -B || true

# Copy all module sources
COPY digitalwallet-domain/src ./digitalwallet-domain/src
COPY digitalwallet-application/src ./digitalwallet-application/src
COPY digitalwallet-infrastructure/src ./digitalwallet-infrastructure/src
COPY digitalwallet-controller/src ./digitalwallet-controller/src
COPY digitalwallet-start/src ./digitalwallet-start/src

# Package application and prepare single executable jar
RUN mvn clean package -DskipTests -B && \
    cp digitalwallet-start/target/digitalwallet-start-*.jar app.jar

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && useradd --system --uid 10001 app

COPY --from=builder /build/app.jar app.jar

USER app

EXPOSE 3006
ENTRYPOINT ["java", "-jar", "app.jar"]
