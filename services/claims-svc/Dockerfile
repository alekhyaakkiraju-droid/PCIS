# Multi-stage Dockerfile for PCIS Claims Service
# Stage 1: Eclipse Temurin JRE — copies the pre-built fat JAR from the Maven target directory
FROM eclipse-temurin:21-jre AS builder
WORKDIR /workspace
# Fat JAR is produced by spring-boot-maven-plugin:repackage
COPY target/claims-svc-*.jar app.jar

# Stage 2: Distroless runtime — no shell, no package manager, non-root
# Pin: gcr.io/distroless/java21-debian12:nonroot index digest (2026-08).
ARG DISTROLESS_DIGEST=sha256:7e37784d94dccbf5ccb195c73b295f5ad00cd266512dfbac12eb9c3c28f8077d
FROM gcr.io/distroless/java21-debian12:nonroot@${DISTROLESS_DIGEST}
WORKDIR /app

COPY --from=builder /workspace/app.jar /app/app.jar

# Non-root execution as UID 1000 (distroless nonroot image default)
USER 1000:1000

# JAVA_TOOL_OPTIONS enables OpenTelemetry auto-instrumentation agent when injected via
# EKS Pod env: JAVA_TOOL_OPTIONS="-javaagent:/otel/opentelemetry-javaagent.jar"
# The agent JAR is mounted as an init container volume in the Kubernetes PodSpec.
ENV JAVA_TOOL_OPTIONS=""

EXPOSE 8086

ENTRYPOINT ["java", \
  "-XX:+UseZGC", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "/app/app.jar"]
