# Multi-stage build for PCIS Audit Archive Batch (AUD002B)
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY shared-libs/pom.xml shared-libs/pom.xml
COPY shared-libs/pcis-observability-starter/pom.xml shared-libs/pcis-observability-starter/pom.xml
COPY shared-libs/pcis-batch-common/pom.xml shared-libs/pcis-batch-common/pom.xml
COPY shared-libs/pcis-batch-auth/pom.xml shared-libs/pcis-batch-auth/pom.xml
COPY shared-libs/pcis-schema/pom.xml shared-libs/pcis-schema/pom.xml
COPY services/pom.xml services/pom.xml
COPY services/audit-batch/pom.xml services/audit-batch/pom.xml

RUN mvn -q -pl services/audit-batch -am dependency:go-offline

COPY shared-libs shared-libs
COPY services/audit-batch services/audit-batch

RUN mvn -q -pl services/audit-batch -am package -DskipTests

ARG DISTROLESS_DIGEST=sha256:7e37784d94dccbf5ccb195c73b295f5ad00cd266512dfbac12eb9c3c28f8077d
FROM gcr.io/distroless/java21-debian12:nonroot@${DISTROLESS_DIGEST}
WORKDIR /app

COPY --from=build /workspace/services/audit-batch/target/audit-batch-*-exec.jar /app/batch.jar

USER 1000:1000
ENTRYPOINT ["java", "-jar", "/app/batch.jar"]
