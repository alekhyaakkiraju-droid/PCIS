# Multi-stage Dockerfile for PCIS Claims Payment Batch
FROM eclipse-temurin:21-jre AS builder
WORKDIR /workspace
COPY target/claims-batch-*-exec.jar app.jar

ARG DISTROLESS_DIGEST=sha256:7e37784d94dccbf5ccb195c73b295f5ad00cd266512dfbac12eb9c3c28f8077d
FROM gcr.io/distroless/java21-debian12:nonroot@${DISTROLESS_DIGEST}
WORKDIR /app
COPY --from=builder /workspace/app.jar /app/app.jar
USER 1000:1000
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
