# Multi-stage build for PCIS Audit Service
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY shared-libs/pom.xml shared-libs/pom.xml
COPY shared-libs/pcis-observability-starter/pom.xml shared-libs/pcis-observability-starter/pom.xml
COPY shared-libs/pcis-test-support/pom.xml shared-libs/pcis-test-support/pom.xml
COPY shared-libs/pcis-batch-auth/pom.xml shared-libs/pcis-batch-auth/pom.xml
COPY shared-libs/pcis-schema/pom.xml shared-libs/pcis-schema/pom.xml
COPY shared-libs/pcis-audit-contract/pom.xml shared-libs/pcis-audit-contract/pom.xml
COPY services/pom.xml services/pom.xml
COPY services/audit-svc/pom.xml services/audit-svc/pom.xml

RUN mvn -q -pl services/audit-svc -am dependency:go-offline

COPY shared-libs shared-libs
COPY services/audit-svc services/audit-svc

RUN mvn -q -pl services/audit-svc -am package -DskipTests

# Pin: gcr.io/distroless/java21-debian12:nonroot index digest (2026-08).
ARG DISTROLESS_DIGEST=sha256:7e37784d94dccbf5ccb195c73b295f5ad00cd266512dfbac12eb9c3c28f8077d
FROM gcr.io/distroless/java21-debian12:nonroot@${DISTROLESS_DIGEST}
WORKDIR /app

COPY --from=build /workspace/services/audit-svc/target/audit-svc-*.jar /app/app.jar

USER 1000:1000
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
