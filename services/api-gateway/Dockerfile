# Multi-stage build for PCIS API Gateway
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY shared-libs/pom.xml shared-libs/pom.xml
# Maven validates every module declared in shared-libs/pom.xml; copy sibling POMs only for layer caching.
COPY shared-libs/pcis-observability-starter/pom.xml shared-libs/pcis-observability-starter/pom.xml
COPY shared-libs/pcis-batch-common/pom.xml shared-libs/pcis-batch-common/pom.xml
COPY shared-libs/pcis-batch-auth/pom.xml shared-libs/pcis-batch-auth/pom.xml
COPY shared-libs/pcis-test-support/pom.xml shared-libs/pcis-test-support/pom.xml
COPY shared-libs/pcis-schema/pom.xml shared-libs/pcis-schema/pom.xml
COPY shared-libs/pcis-audit-contract/pom.xml shared-libs/pcis-audit-contract/pom.xml
COPY shared-libs/pcis-audit-outbox/pom.xml shared-libs/pcis-audit-outbox/pom.xml
COPY shared-libs/pcis-outbox/pom.xml shared-libs/pcis-outbox/pom.xml
COPY shared-libs/pcis-classification/pom.xml shared-libs/pcis-classification/pom.xml
COPY shared-libs/pcis-pii-masking/pom.xml shared-libs/pcis-pii-masking/pom.xml
COPY shared-libs/pcis-error/pom.xml shared-libs/pcis-error/pom.xml
COPY shared-libs/pcis-config/pom.xml shared-libs/pcis-config/pom.xml
COPY services/pom.xml services/pom.xml
# Maven validates every module declared in services/pom.xml; copy sibling POMs only for layer caching.
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml
COPY services/authz-svc/pom.xml services/authz-svc/pom.xml
COPY services/audit-svc/pom.xml services/audit-svc/pom.xml
COPY services/audit-batch/pom.xml services/audit-batch/pom.xml
COPY services/reconciliation-batch/pom.xml services/reconciliation-batch/pom.xml
COPY services/claims-batch/pom.xml services/claims-batch/pom.xml
COPY services/policy-batch/pom.xml services/policy-batch/pom.xml
COPY services/billing-svc/pom.xml services/billing-svc/pom.xml
COPY services/reporting-svc/pom.xml services/reporting-svc/pom.xml
COPY services/customer-svc/pom.xml services/customer-svc/pom.xml
COPY services/policy-svc/pom.xml services/policy-svc/pom.xml
COPY services/claims-svc/pom.xml services/claims-svc/pom.xml
COPY services/premium-svc/pom.xml services/premium-svc/pom.xml
COPY services/sync-agent/pom.xml services/sync-agent/pom.xml
COPY services/config-svc/pom.xml services/config-svc/pom.xml

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
