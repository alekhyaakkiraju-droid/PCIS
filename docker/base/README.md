# PCIS Java 21 base image (`pcis-base-java21`)

Shared multi-stage Docker image for PCIS Java services:

- **Runtime**: `gcr.io/distroless/java21-debian12:nonroot` pinned by digest (`DISTROLESS_DIGEST`)
- **User**: `nonroot` (UID 65532)
- **Telemetry**: OpenTelemetry Java agent at `/opt/otel/opentelemetry-javaagent.jar` (SHA256-verified at build)
- **Entrypoint**: `java -javaagent:/opt/otel/opentelemetry-javaagent.jar -jar` — pass the service JAR as `CMD`

## Pins

| Pin | Current value |
|-----|----------------|
| Distroless digest | `sha256:7e37784d94dccbf5ccb195c73b295f5ad00cd266512dfbac12eb9c3c28f8077d` (`:nonroot` index) |
| OTel Java agent | `2.30.0` / SHA256 `9d6bc2ad8dd8fb7f730984988e57b8ac0a82d81c7b3b8ae795378718733a509d` |

Rotate pins via build-args `DISTROLESS_DIGEST`, `OTEL_JAVA_AGENT_VERSION`, and `OTEL_JAVA_AGENT_SHA256`.

## Build

```bash
./docker/base/build.sh
# IMAGE_TAG=dev SKIP_SCAN=1 ./docker/base/build.sh
```

Requires Docker BuildKit. Sets `SOURCE_DATE_EPOCH` from the latest git commit when unset. Optionally runs `trivy` or `grype` if installed.

## Structure tests

After building:

```bash
container-structure-test test --image pcis-base-java21:local --config docker/base/test-config.yaml
```

## Unit tests (no Docker daemon)

```bash
python3 -m unittest discover -s docker/base/tests -v
# or
./docker/base/tests/test_dockerfile.sh
```

## Consume from a service

```dockerfile
FROM pcis-base-java21:local
COPY app.jar /app/app.jar
CMD ["/app/app.jar"]
```

See `docker/sample-svc/` for a minimal example.
