# PCIS Batch Auth (`pcis-batch-auth`)

Spring Boot auto-configuration library that gives PCIS batch jobs a real OAuth2
**client-credentials** identity instead of compiled-in literals such as `BATCHBIL`.

## Features

- **`BatchAuthenticationService`** — obtains and caches access tokens with a thread-safe
  refresh lock and a default **30s** expiration buffer (`pcis.batch.oauth2.expiration-buffer-seconds`).
- **`BatchAuthRestTemplateInterceptor`** — adds `Authorization: Bearer …` to outbound REST calls.
- **`BatchSecurityContextInitializer`** — Spring Batch `JobExecutionListener` that sets
  `SecurityContext` and MDC `actor` from the JWT `sub` claim at job start.
- **`BatchConfigurationException`** — fail-closed auth/config errors mapped to **exit code 5**
  (WO-137 configuration-validation failure).
- **`MockBatchAuthenticationService`** — published in the Maven **test-jar** (`testFixtures`) for
  downstream batch module tests.

Auto-configuration activates when `pcis.batch.oauth2.token-uri` is set.

## Configuration

```yaml
pcis.batch.oauth2:
  token-uri: https://idp.example.com/realms/pcis/protocol/openid-connect/token
  client-id: batch-billing
  client-secret-ref: arn:aws:secretsmanager:us-east-1:123456789012:secret:pcis/batch/billing
  scope: batch:billing
  expiration-buffer-seconds: 30   # default
```

Never commit plaintext client secrets in yaml. Resolve the secret at runtime via environment
variable `PCIS_BATCH_OAUTH2_CLIENT_SECRET` (or override with `pcis.batch.oauth2.client-secret-env`).
For local/tests only, `pcis.batch.oauth2.client-secret` may be set directly.

## Usage

### Maven dependency

```xml
<dependency>
  <groupId>com.pcis</groupId>
  <artifactId>pcis-batch-auth</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Register the job listener

```java
@Bean
Job auditArchiveJob(JobRepository jobRepository, BatchSecurityContextInitializer authListener) {
  return new JobBuilder("auditArchiveJob", jobRepository)
      .listener(authListener)
      // ...
      .build();
}
```

Outbound `RestTemplate` beans are automatically customized when auto-configuration is active.

### Downstream test fixture

```xml
<dependency>
  <groupId>com.pcis</groupId>
  <artifactId>pcis-batch-auth</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <type>test-jar</type>
  <scope>test</scope>
</dependency>
```

```java
MockBatchAuthenticationService auth =
    new MockBatchAuthenticationService().withAccessToken(TestJwtFactory.tokenWithSubject("batch-test"));
```

## Keycloak clients (WO-147)

Six dedicated batch clients are registered in `infra/keycloak/realm-export.json`:

| Client ID         | Workload        |
|-------------------|-----------------|
| `batch-audit`     | audit archive   |
| `batch-billing`   | billing         |
| `batch-claims`    | claims payment  |
| `batch-commission`| commission calc |
| `batch-premium`   | premium         |
| `batch-renewal`   | policy renewal  |

Each client uses the **client-credentials** grant with a service account granted the
`BATCH_SVC` realm role.

## Build & test

```bash
mvn -q -pl shared-libs/pcis-batch-auth -am test
```

Integration tests use **WireMock** for the token endpoint (no Docker/Keycloak required).
JaCoCo enforces **≥90%** line coverage on `com.pcis.batch.auth*`.

## Exit codes

| Code | Meaning                                      |
|------|----------------------------------------------|
| 5    | Configuration / OAuth2 token acquisition failure (`BatchConfigurationException`) |

See `helm/charts/pcis-batch/templates/prometheusrule.yaml` for the full WO-137 exit-code contract.
