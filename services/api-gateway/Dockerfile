# Multi-stage build for PCIS API Gateway
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY shared-libs/pom.xml shared-libs/pom.xml
COPY shared-libs/pcis-observability-starter/pom.xml shared-libs/pcis-observability-starter/pom.xml
COPY shared-libs/pcis-test-support/pom.xml shared-libs/pcis-test-support/pom.xml
COPY services/pom.xml services/pom.xml
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml

RUN mvn -q -pl services/api-gateway -am dependency:go-offline

COPY shared-libs shared-libs
COPY services/api-gateway services/api-gateway

RUN mvn -q -pl services/api-gateway -am package -DskipTests

FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /app

COPY --from=build /workspace/services/api-gateway/target/api-gateway-*.jar /app/app.jar

USER 1000:1000
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
