# Forge Implementation Log

| Field | Value |
|-------|-------|
| Project | b7a5a841-d2ab-4c97-ba40-428eec119c54 |
| Branch | forge/property-casualty-insurance-sy-5304b66b-run8-6wo |
| Started | 2026-08-11T12:35:58Z |

---

## WO-242: User Story: WO-242 - CLM006B Claims Payment Reserve Drawdown Golden Test Fixtures
- **Status:** completed
- **Commit:** `c001333`
- **Files:** 0 (+0/-0)
- **Duration:** 137ss
- **Approach:** WO-242 is an orphan-commit scope review work order. All required golden test fixtures and oracle files for CLM006B were already present in the repository as committed code. The implementation verified that all 7 acceptance criteria are satisfied by the existing artifacts under golden/fixtures/clm006b/ and golden/outputs/clm006b/.

## WO-243: User Story: WO-243 - CLM006B Claims Payment Reserve Drawdown Golden Test Fixtures
- **Status:** completed
- **Commit:** `6a149b3`
- **Files:** 0 (+0/-0)
- **Duration:** 90ss
- **Approach:** WO-243 is an orphan-commit scope review work order. All required artifacts — seven fixture SQL files, seven golden JSON oracles, schema-drift-notes.md, normalization-rules.yaml RECOVERY_ID entry, _common.sql cleanup script, and ClaimsPaymentGoldenTest.java stub — were already present in the repository as committed code. All 9 acceptance criteria verified as PASS.

## WO-244: User Story: WO-244 - Measure Production Data Volumes and Batch Windows for Phase 0 Baseline (WO-238)
- **Status:** completed
- **Commit:** `40f884a`
- **Files:** 0 (+0/-0)
- **Duration:** 76ss
- **Approach:** WO-244 is an orphan-commit scope review work order. All required artifacts — measure_table_volumes.sql (55 tables, 54 UNION ALL), measure_batch_windows.sql (6 programs, avg/max/min/P95), ops/scheduler-map.yaml measurement fields, ops/scheduler-map-schema.yaml schema, validate-scheduler-map.py enforcement, phase0_baseline_report.md template, sample_baseline_report.md, and validate_sql_syntax.sh — were already present in the repository as committed code. All 8 acceptance criteria verified as PASS.

## WO-245: User Story: WO-245 - Phase 0 Production Baseline Measurement Scripts and Report Templates
- **Status:** completed
- **Commit:** `86a7ddd`
- **Files:** 0 (+0/-0)
- **Duration:** 48ss
- **Approach:** WO-245 is an orphan-commit scope review work order with near-identical scope to WO-244. All required artifacts were already present in the repository. The single difference from WO-244 is AC7's explicit requirement for semicolon-termination checking and AC9's requirement that all validation scripts exit 0 on committed artifacts — both already satisfied (validate_sql_syntax.sh line 63 checks for semicolon termination; running the script against committed files outputs all-OK and exits 0).

## WO-246: User Story: WO-246 - Implement OIDC PKCE Authentication and Role-Based Navigation Gating in React Frontend
- **Status:** completed
- **Commit:** `82f678e`
- **Files:** 1 (+4/-0)
- **Duration:** 197ss
- **Approach:** Almost all WO-246 artifacts were already committed (AuthProvider, ProtectedRoute, LoginCallback, role-menu-config, Sidebar role filtering, session fixtures, OIDC config, test suite). The single gap was AC5: ForbiddenPage was missing its return-to-dashboard link. Added `<Link to="/">Return to dashboard</Link>` via react-router Link, satisfying the accessible navigation requirement equivalent to MENUMD1-91 error handling.

## WO-224: User Story: WO-224 - OIDC PKCE Login and Role-Based Navigation
- **Status:** completed
- **Commit:** `6bf04b3`
- **Files:** 9 (+234/-0)
- **Duration:** 556ss
- **Approach:** The core OIDC PKCE authentication implementation (AuthContext/AuthProvider, ProtectedRoute, ForbiddenPage, LoginCallback, Sidebar, role-menu-config, oidc-config, session-api, errors, and unit tests) was already present in the repository from a prior implementation. The missing pieces were: (1) session fixtures for UNDERWRITER, FINANCE, and COMPLIANCE roles needed for AC 9; (2) .env.development and .env.example files for VITE_OIDC_* environment variable configuration required by the Technical Details section; (3) a Playwright E2E test required by AC 8. Added @playwright/test to devDependencies, created playwright.config.ts with Chromium project and optional webServer, and wrote e2e/auth.spec.ts covering unauthenticated PKCE redirect, role-based sidebar visibility (adjuster/CSR), 403 Forbidden page, and logout using Playwright route interception to mock BFF and Keycloak endpoints.

## WO-155: User Story: WO-155 - Sequence Strategy for Business and Surrogate Keys
- **Status:** completed
- **Commit:** `2e8565f`
- **Files:** 8 (+507/-17)
- **Duration:** 548ss
- **Approach:** Updated all 15 V1 PostgreSQL SEQUENCE objects from START WITH 100000 to START WITH 10000000 (with MAXVALUE extended to 9999999999) to prevent collision with legacy Db2 for i sequence values during the Strangler Fig parallel-run period. Updated V1BaselineMigrationTest to assert the new start value. Created docs/key-generation-strategy.md with full SEQUENCE vs IDENTITY table inventory, JPA mapping patterns, allocationSize=1 rationale, and composite key exceptions. Added jakarta.persistence-api (provided scope) to pcis-schema pom.xml and created IdentityKeyEntity and SequenceKeyEntity @MappedSuperclass base classes. Created two tests: KeyGenerationStrategyTest (pure reflection, no Docker) validates base class annotations, and KeyGenerationIntegrationTest (Testcontainers PostgreSQL 17) verifies NEXTVAL on business sequences returns >= 10000000 and IDENTITY inserts produce low-range surrogates.

## WO-158: User Story: WO-158 - Db2-to-PostgreSQL SQL Construct Translation Reference
- **Status:** completed
- **Commit:** `7707453`
- **Files:** 2 (+808/-0)
- **Duration:** 372ss
- **Approach:** Created the authoritative Db2-for-i to PostgreSQL SQL construct translation reference document (docs/db2-to-postgresql-translation.md) and a companion Testcontainers integration test class (SqlTranslationValidationTest.java). The reference document covers all 10 translation categories from the WO with before/after SQL examples, affected COBOL program inventory, and appendix tables. The test class has 20 test methods running against PostgreSQL 17 via Testcontainers — no V1 migration required since all tests are self-contained (use temp tables or literal expressions). Placed in com.pcis.migration package under pcis-schema since that module already has the Testcontainers/JUnit infrastructure needed.

## WO-178: User Story: WO-178 - Wire Golden-Output Regression into CI Pipeline
- **Status:** completed
- **Commit:** `f05521e`
- **Files:** 3 (+400/-0)
- **Duration:** 440ss
- **Approach:** Created forge-pipeline.yaml as the Forge Shipping declarative pipeline with a fail-closed golden-output gate in the build:maven stage. The gate runs all @Tag('GoldenOutput') tests via Maven Surefire with failIfNoTests=true (fail-closed: missing tests or infrastructure failure → BUILD FAIL), forkedProcessTimeoutInSeconds=300, and reportsDirectory pointing to golden/reports/. Artifact retention configuration publishes golden/reports/*.json and golden/reports/*.txt on failure. A nightly cron (0 2 * * *) runs the full suite with per-class and suite-total duration metadata. Updated the parent pom.xml with a golden-output profile (Surefire configuration) and a jacoco profile (JaCoCo 90% LINE coverage rule scoped to com.pcis/*/calc and com.pcis/*/payment packages via <includes> filter). Also added pluginManagement for surefire 3.5.3 and jacoco 0.8.13, and three configurable properties (golden.surefire.timeout=300, golden.test.timeout.per.class=120, golden.coverage.minimum=0.90). Created ops/golden-pipeline.md documenting fail-closed semantics, timeout/threshold configuration, retained artifacts, nightly schedule, new-test onboarding, and troubleshooting.

## WO-181: User Story: WO-181 - Scaffold customer-svc Spring Boot Service Module
- **Status:** completed
- **Commit:** `7b88758`
- **Files:** 23 (+1182/-0)
- **Duration:** 744ss
- **Approach:** Created the customer-svc Spring Boot 3.5.x microservice as a new Maven module under services/, following the established authz-svc/audit-svc scaffold pattern. Added customer-svc to services/pom.xml reactor. The pom.xml imports Spring Boot 3.5.x BOM, Spring Cloud 2025.0.0 BOM (for OpenFeign and Resilience4j), and Testcontainers BOM, with all required starters. SecurityConfig implements deny-by-default SecurityFilterChain with /actuator/health/** and /actuator/info permitted, OAuth2 resource server JWT validation, and RoleAndScopeConverter extracting authorities from 'roles' collection and 'scope' string claims. Structured JSON ProblemDetail responses on 401/403 via custom AuthenticationEntryPoint and AccessDeniedHandler. CorrelationIdFilter extends OncePerRequestFilter to propagate X-Correlation-ID from headers (or generate UUID) into MDC fields correlationId, service=customer-svc, and actor from JWT subject. AuthzServiceClient and AuditServiceClient are @FeignClient interfaces with fail-closed fallbacks: circuit open → AccessDeniedException (authz) or AuditWriteException (audit) preventing mutation without audit record. FeignClientConfig propagates X-Correlation-ID to outbound Feign calls. PiiMaskingConverter extends ClassicConverter with regex patterns for SSN (preserve last 4), phone (10+ digits), email, and tax ID masking. logback-spring.xml uses LogstashEncoder with MDC field inclusion for production and PII-masked plain-text for test profile. application.yaml configures HikariCP (max 20, min 5, 30s timeout), Flyway, actuator probes with liveness/readiness/startup groups, and Resilience4j circuit breaker defaults. Dockerfile uses distroless java21-debian12:nonroot with USER 1000, EXPOSE 8082, and JVM container-aware flags.

## WO-192: User Story: WO-192 - Bootstrap policy-svc Spring Boot Module Structure
- **Status:** completed
- **Commit:** `bf98d5b`
- **Files:** 27 (+1059/-0)
- **Duration:** 684ss
- **Approach:** Created the policy-svc Spring Boot 3.5.x module following the established customer-svc/audit-svc scaffold pattern with policy-domain specifics. Added policy-svc to services/pom.xml reactor. The pom.xml imports Spring Boot 3.5.x BOM and Testcontainers BOM, includes all 6 required starters (web, data-jpa, security, actuator, validation, oauth2-resource-server), Flyway, logstash-logback-encoder 8.1, and nimbus-jose-jwt 10.3 (test scope) for TestJwtFactory. SecurityConfig implements deny-by-default SecurityFilterChain permitting /actuator/health/**, /actuator/readiness, and /actuator/info; RealmAccessRolesConverter maps Keycloak realm_access.roles to ROLE_* Spring Security authorities, handling null/empty/malformed claims gracefully. CorrelationIdFilter propagates X-Correlation-ID into MDC. PiiMaskingConverter masks SSN (preserve last 4), phone (10+ digits), email, and tax ID. application.yaml configures HikariCP (max 10, min 2, 30s timeout), Flyway, actuator probe groups (liveness/readiness/startup), and PostgreSQL dialect. Three environment profiles (application-dev.yaml, application-prod.yaml, application-test.yaml) provide profile-specific datasource and OIDC configuration. Dockerfile uses eclipse-temurin:21-jre as builder (copies pre-built fat JAR), then distroless java21-debian12:nonroot with USER 1000, EXPOSE 8080, and -XX:+UseZGC JVM flag. Package sub-structure established via package-info.java stubs for controller, domain/{entity,repository,event}, service, dto, exception, and batch. TestJwtFactory uses Nimbus JOSE RSA-signed JWTs for downstream story tests.

## WO-196: User Story: WO-196 - Bootstrap claims-svc Spring Boot Service Skeleton
- **Status:** completed
- **Commit:** `cb6bc3e`
- **Files:** 33 (+1957/-0)
- **Duration:** 805ss
- **Approach:** Created the full claims-svc Spring Boot 3.5.x skeleton following the established policy-svc/customer-svc patterns. Added claims-svc to services/pom.xml reactor. The pom.xml mirrors policy-svc (Spring Boot 3.5.x BOM, Testcontainers BOM, no Spring Cloud/Feign since skeleton only). SecurityConfig implements a stricter deny-by-default than other services: /actuator/health and /actuator/readiness permitAll, /api/v1/claims/** authenticated, everything else .denyAll() (not .authenticated()). RealmAccessRolesConverter maps Keycloak realm_access.roles to ROLE_* authorities. AuditableEntity @MappedSuperclass uses Spring Data JPA @CreatedBy/@CreatedDate/@LastModifiedBy/@LastModifiedDate; JpaConfig enables @EnableJpaAuditing with a SecurityContext-aware AuditorAware<String> bean that returns the JWT subject. Eight JPA entities (ClaimEntity, ClaimReserveEntity, ClaimPaymentEntity, ClaimAdjusterEntity, ClaimNoteEntity, ApprovalEntity, RecoveryEntity, OutboxEvent) with @ManyToOne LAZY relationships and BigDecimal precision=11,scale=2 for all monetary fields. Flyway V1 migration uses IF NOT EXISTS, BIGINT GENERATED ALWAYS AS IDENTITY for surrogate PKs, FK constraints between claims tables (no cross-domain FKs for pol_nbr/cust_id), and a partial index on outbox_events WHERE published=FALSE for the relay poller. Dockerfile matches policy-svc pattern: eclipse-temurin:21-jre builder + distroless java21-debian12:nonroot@pinned-digest + USER 1000 + EXPOSE 8086. k8s/claims-svc/deployment.yaml includes liveness/readiness/startup probes and PodDisruptionBudget minAvailable=2 in the same YAML file.
