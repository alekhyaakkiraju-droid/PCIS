# Sample service JAR fixture

`sample-app.jar` is a **tiny placeholder JAR** (ZIP archive with a `META-INF/MANIFEST.MF` declaring a `Main-Class`). It is **not** a Spring Boot fat JAR and will not start a real HTTP server or pass actuator health checks.

Use it to:

- Validate `docker/sample-svc/Dockerfile` `COPY` / `FROM pcis-base-java21` wiring
- Exercise image build + structure tests without a full Maven reactor

For full health-check / smoke verification (liveness, readiness, `/actuator/health`), replace this fixture with a real Spring Boot executable fat JAR built from a PCIS service module, then re-run `test-smoke.sh`.
