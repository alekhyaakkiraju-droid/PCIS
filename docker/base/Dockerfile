# PCIS shared Java 21 base image: distroless + OpenTelemetry Java agent.
# Pin runtime by digest (see DISTROLESS_DIGEST). Rebuild when bumping pins.

# ---- Stage: download & verify OpenTelemetry Java agent ----
FROM debian:bookworm-slim AS otel-agent

ARG OTEL_JAVA_AGENT_VERSION=2.30.0
# sha256 of opentelemetry-javaagent.jar for v2.30.0 (verified at pin time)
ARG OTEL_JAVA_AGENT_SHA256=9d6bc2ad8dd8fb7f730984988e57b8ac0a82d81c7b3b8ae795378718733a509d

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && find /var/lib/apt/lists -mindepth 1 -delete \
    && mkdir -p /opt/otel \
    && curl -fsSL -o /opt/otel/opentelemetry-javaagent.jar \
      "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_JAVA_AGENT_VERSION}/opentelemetry-javaagent.jar" \
    && echo "${OTEL_JAVA_AGENT_SHA256}  /opt/otel/opentelemetry-javaagent.jar" | sha256sum -c - \
    && chmod 0444 /opt/otel/opentelemetry-javaagent.jar

# ---- Stage: runtime (distroless Java 21, nonroot) ----
# Pin: gcr.io/distroless/java21-debian12:nonroot index digest (2026-08).
# Override via --build-arg DISTROLESS_DIGEST=sha256:... when rotating the pin.
ARG DISTROLESS_DIGEST=sha256:7e37784d94dccbf5ccb195c73b295f5ad00cd266512dfbac12eb9c3c28f8077d
FROM gcr.io/distroless/java21-debian12:nonroot@${DISTROLESS_DIGEST}

LABEL org.opencontainers.image.title="pcis-base-java21" \
      org.opencontainers.image.description="PCIS distroless Java 21 base with OpenTelemetry Java agent" \
      org.opencontainers.image.source="https://github.com/alekhyaakkiraju-droid/PCIS"

COPY --from=otel-agent /opt/otel/opentelemetry-javaagent.jar /opt/otel/opentelemetry-javaagent.jar

# Distroless nonroot UID (65532)
USER nonroot

# Services append their JAR path as CMD / remaining args.
ENTRYPOINT ["java", "-javaagent:/opt/otel/opentelemetry-javaagent.jar", "-jar"]
