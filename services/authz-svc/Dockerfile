# Multi-stage build for PCIS Authz Svc
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY shared-libs shared-libs
COPY services services
COPY docker/scripts/strip-non-pom-sources.sh /tmp/strip-non-pom-sources.sh
RUN chmod +x /tmp/strip-non-pom-sources.sh && /tmp/strip-non-pom-sources.sh

RUN mvn -q -pl services/authz-svc -am dependency:go-offline

COPY shared-libs shared-libs
COPY services services
RUN mvn -q -pl services/authz-svc -am package -DskipTests

FROM gcr.io/distroless/java21-debian12:nonroot@sha256:7e37784d94dccbf5ccb195c73b295f5ad00cd266512dfbac12eb9c3c28f8077d
WORKDIR /app

COPY --from=build /workspace/services/authz-svc/target/authz-svc-*.jar /app/app.jar
COPY config/pcis-data-classification.yaml /app/config/pcis-data-classification.yaml

USER 1000:1000
EXPOSE 8083
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
