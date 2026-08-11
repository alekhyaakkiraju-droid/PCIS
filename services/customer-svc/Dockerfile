# Multi-stage build for PCIS Customer Service
# Stage 1: Maven build using Eclipse Temurin 21
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dependency resolution layer separately from source
COPY pom.xml .
COPY shared-libs/pom.xml shared-libs/pom.xml
COPY shared-libs/pcis-observability-starter/pom.xml shared-libs/pcis-observability-starter/pom.xml
COPY shared-libs/pcis-batch-common/pom.xml shared-libs/pcis-batch-common/pom.xml
COPY shared-libs/pcis-batch-auth/pom.xml shared-libs/pcis-batch-auth/pom.xml
COPY shared-libs/pcis-schema/pom.xml shared-libs/pcis-schema/pom.xml
COPY shared-libs/pcis-audit-contract/pom.xml shared-libs/pcis-audit-contract/pom.xml
COPY shared-libs/pcis-outbox/pom.xml shared-libs/pcis-outbox/pom.xml
COPY shared-libs/pcis-classification/pom.xml shared-libs/pcis-classification/pom.xml
COPY shared-libs/pcis-test-support/pom.xml shared-libs/pcis-test-support/pom.xml
COPY services/pom.xml services/pom.xml
COPY services/customer-svc/pom.xml services/customer-svc/pom.xml

RUN mvn -q -pl services/customer-svc -am dependency:go-offline

COPY shared-libs shared-libs
COPY services/customer-svc services/customer-svc

RUN mvn -q -pl services/customer-svc -am package -DskipTests

# Stage 2: Distroless runtime — no shell, no package manager, non-root
# Pin: gcr.io/distroless/java21-debian12:nonroot index digest (2026-08).
ARG DISTROLESS_DIGEST=sha256:7e37784d94dccbf5ccb195c73b295f5ad00cd266512dfbac12eb9c3c28f8077d
FROM gcr.io/distroless/java21-debian12:nonroot@${DISTROLESS_DIGEST}
WORKDIR /app

COPY --from=build /workspace/services/customer-svc/target/customer-svc-*.jar /app/app.jar

# Non-root execution as UID 1000 (distroless nonroot image default)
USER 1000:1000

EXPOSE 8082

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "/app/app.jar"]
