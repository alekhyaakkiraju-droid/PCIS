# Testing

**Selected Categories:** Functional test cases, Smoke test suite, Regression test suite, Performance test scenarios

**Total Test Cases:** 404


---

## Functional test cases (101)

### FUNCTIONAL-001 — Repository Member Manifest and Completeness Gate

- User Story: WO-001
- Objective: Validate functional behavior for "Repository Member Manifest and Completeness Gate" against acceptance criteria.
- Expected: Story "Repository Member Manifest and Completeness Gate" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A committed manifest file at manifest/pcis-manifest.yaml lists all 39 repository members with fields: path, member_type (cobol_program, dds_display_file, cl_member, design_document), module_code, implementation_status (implemented, design_only, empty, unverified), declared_calls, declared_tables and notes.
- Check acceptance criterion 2: A validator command (tools/manifest/validate_manifest.py or equivalent) walks the repository tree, compares it against the manifest, and exits with code 0 only when every file on disk is declared and every declared file exists; it exits non-zero with a per-member reason otherwise.
- Check acceptance criterion 3: The validator detects and reports zero-byte or whitespace-only source members (currently at least CLM006B.cbl, CMM001B.cbl and CUS_Module_Design_Document.md are suspected) as implementation_status=empty and fails if the manifest disagrees.

### FUNCTIONAL-002 — Extract Legacy Behavioural Baseline Specification Artifact

- User Story: WO-002
- Objective: Validate functional behavior for "Extract Legacy Behavioural Baseline Specification Artifact" against acceptance criteria.
- Expected: Story "Extract Legacy Behavioural Baseline Specification Artifact" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: baseline/legacy-baseline.yaml is committed and records, for each of the 8 COBOL programs: program name, called-by scheduler entry, commit scope statement, declared cursors with full SQL text, all WORKING-STORAGE numeric and identity literals with name, PIC clause and value, and all AUDLOG01 call sites with the nine parameter names and PIC widths.
- Check acceptance criterion 2: The baseline captures the six regulatory tunables (retention days 365, chunk size 5000, lead days 15, grace days 10, renewal window 60, reinsurance threshold 100000.00) and the six batch actor literals with the exact program and paragraph where each is defined.
- Check acceptance criterion 3: The baseline records every continue-after-failure error path, including the non-00 AUDLOG01 return handling in BIL003B, CMM001B, PRM005B, POL006B and CLM006B, and the archive verification mismatch halt in AUD002B, each with the source paragraph reference.

### FUNCTIONAL-003 — Scripted Dependency-Ordered Legacy COBOL Build

- User Story: WO-009
- Objective: Validate functional behavior for "Scripted Dependency-Ordered Legacy COBOL Build" against acceptance criteria.
- Expected: Story "Scripted Dependency-Ordered Legacy COBOL Build" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A single entrypoint (build/scripts/build_legacy.sh) accepts an environment name (dev, tst, prd) and builds all COBOL, DDS and CL members declared in manifest/pcis-manifest.yaml in dependency-correct order, with no manual step and no interactive prompt.
- Check acceptance criterion 2: Compile order is computed from declared dependencies (copybooks and service programs from prologue CALLS, DDS display files bound to interactive programs) rather than hard-coded, and the resolved order is printed and written to build/reports/compile-order.txt.
- Check acceptance criterion 3: Environment-specific library topology (INSDEV/INSDEVDTA, INSTST/INSTSTDTA, INSPRD/INSPRDDTA, shared INSCOM, tooling INSTOOLS) is externalized in build/build.yaml with no library name hard-coded in any script.

### FUNCTIONAL-004 — CI Pipeline Gating Manifest, Baseline and Build

- User Story: WO-024
- Objective: Validate functional behavior for "CI Pipeline Gating Manifest, Baseline and Build" against acceptance criteria.
- Expected: Story "CI Pipeline Gating Manifest, Baseline and Build" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A committed pipeline descriptor under ci/ defines source triggers for commit, pull request and tag, and stages for validate, scan, legacy-build and publish, using the Forge Shipping step catalog.
- Check acceptance criterion 2: The validate stage runs the WO-001 manifest validator and the WO-002 baseline drift detector and fails the pipeline with the offending member and reason on any non-zero exit.
- Check acceptance criterion 3: The scan stage runs a secret scan and a static-analysis scan across the repository and tooling code in parallel, and the pipeline blocks progression if any scan reports a blocking finding.

### FUNCTIONAL-005 — Coexistence Topology, Scheduler Map and Runbook

- User Story: WO-025
- Objective: Validate functional behavior for "Coexistence Topology, Scheduler Map and Runbook" against acceptance criteria.
- Expected: Story "Coexistence Topology, Scheduler Map and Runbook" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: ops/topology.yaml declares every coexistence environment (dev, tst, prd) with program library, data library, shared library INSCOM, tooling library INSTOOLS, resolved library list order and a data-sensitivity flag marking INSPRDDTA as the only library holding real customer data.
- Check acceptance criterion 2: ops/scheduler-map.yaml maps each scheduler entry (JOBSCHD1, JOBSCHD2, JOBSCHD3) to its programs, invocation cadence, declared commit scope from the WO-002 baseline, expected window, run-log table (RPT_RUN_LOG_T) evidence and an accountable owner role.
- Check acceptance criterion 3: A validator (tools/ops/validate_topology.py) cross-checks ops/scheduler-map.yaml against the CALLED BY lines in the COBOL prologues and against manifest/pcis-manifest.yaml, and exits non-zero on any program that is scheduled but undeclared, or declared but unscheduled.

### FUNCTIONAL-006 — Bootstrap Maven multi-module Java 21 platform skeleton

- User Story: WO-003
- Objective: Validate functional behavior for "Bootstrap Maven multi-module Java 21 platform skeleton" against acceptance criteria.
- Expected: Story "Bootstrap Maven multi-module Java 21 platform skeleton" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A root pom.xml exists declaring maven.compiler.release 21 and a reactor containing exactly 13 modules: pcis-bom, pcis-common, pcis-batch-common, pcis-contracts, pcis-migrations, claims-svc, customer-svc, policy-svc, premium-svc, billing-svc, reporting-svc, authz-svc, audit-svc.
- Check acceptance criterion 2: Running a clean full build from a fresh checkout succeeds with no network-pinned SNAPSHOT dependencies and completes in under 10 minutes on a standard runner; the command and prerequisites are documented in a BUILD.md runbook.
- Check acceptance criterion 3: pcis-bom manages versions for Spring Boot 3.5.x, Spring Batch 5.x, Spring Security 6, PostgreSQL JDBC, Flyway, jOOQ or JdbcClient, JUnit 5, AssertJ, Testcontainers, spring-batch-test, logstash-logback-encoder, Micrometer and OpenTelemetry; no child module declares an explicit version for any managed dependency.

### FUNCTIONAL-007 — Provision Terraform infrastructure for three PCIS environments

- User Story: WO-004
- Objective: Validate functional behavior for "Provision Terraform infrastructure for three PCIS environments" against acceptance criteria.
- Expected: Story "Provision Terraform infrastructure for three PCIS environments" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Terraform root configurations exist for three environments (dev, test, prod) composed from six reusable modules: network, kubernetes, database, secrets, object-storage and registry; environment differences are expressed only through tfvars, not divergent module code.
- Check acceptance criterion 2: terraform validate and terraform plan succeed for all three environments against a remote state backend with state locking, and plan output for prod shows zero unmanaged resources.
- Check acceptance criterion 3: The database module provisions PostgreSQL 17 with Multi-AZ enabled, automated backups retained for at least 35 days, point-in-time recovery enabled, encryption at rest with a customer-managed key, and storage-in-transit enforced via TLS-only parameter settings; documented RTO and RPO are expressed in hours as agreed.

### FUNCTIONAL-008 — Create reproducible distroless non-root service images

- User Story: WO-012
- Objective: Validate functional behavior for "Create reproducible distroless non-root service images" against acceptance criteria.
- Expected: Story "Create reproducible distroless non-root service images" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A shared Dockerfile template plus per-service build configuration produces an image for every service module and for the batch runtime, all based on a pinned distroless Java 21 runtime referenced by digest rather than by mutable tag.
- Check acceptance criterion 2: Every image runs as a non-root numeric UID with a read-only root filesystem, no shell and no package manager present, verified by an automated container-hardening test that inspects the built image.
- Check acceptance criterion 3: Building the same commit twice produces identical image digests (reproducible build), verified by a CI check that builds twice and compares digests, with build timestamps and file ordering normalized.

### FUNCTIONAL-009 — Implement Forge Shipping pipeline with security gates

- User Story: WO-029
- Objective: Validate functional behavior for "Implement Forge Shipping pipeline with security gates" against acceptance criteria.
- Expected: Story "Implement Forge Shipping pipeline with security gates" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A single declarative Forge Shipping pipeline definition exists in the repository covering, in order: build:maven, build:docker, four parallel scans, image scan with SBOM emission, push:ecr with signing, deploy to dev, functional test stage, deploy to staging, parity reconciliation gate, manual production approval, production deploy and post-deploy smoke.
- Check acceptance criterion 2: build:maven fails the pipeline when unit tests fail or when line coverage on monetary calculation packages falls below 90 percent; the failing package and actual coverage are reported in the pipeline output.
- Check acceptance criterion 3: The four scan steps run in parallel and every one is a hard gate: SonarQube allows zero new blocker issues, Snyk allows zero critical or high CVEs including transitive dependencies, Gitleaks allows zero detected secrets, and Semgrep allows zero high-severity findings plus a custom rule that fails when a mutating service or controller method lacks an authorization annotation.

### FUNCTIONAL-010 — Establish GitOps delivery with fifteen-minute rollback

- User Story: WO-046
- Objective: Validate functional behavior for "Establish GitOps delivery with fifteen-minute rollback" against acceptance criteria.
- Expected: Story "Establish GitOps delivery with fifteen-minute rollback" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Helm charts exist for all eight services and the batch runtime with value overlays for dev, test and prod, each chart setting a non-root securityContext, read-only root filesystem, dropped capabilities, resource requests and limits, and liveness, readiness and startup probes matching the WO-011 image contract.
- Check acceptance criterion 2: Argo CD Application manifests exist per service per environment with automated self-healing sync for dev and test, manual sync for prod, and revision history retaining at least the previous five releases so any of them can be rolled back to by digest.
- Check acceptance criterion 3: A default-deny NetworkPolicy is applied per namespace and each chart declares only the explicit ingress and egress it requires, verified by a policy test that fails when a chart requests unrestricted egress.

### FUNCTIONAL-011 — Define batch CronJob manifests and exit-code contract

- User Story: WO-063
- Objective: Validate functional behavior for "Define batch CronJob manifests and exit-code contract" against acceptance criteria.
- Expected: Story "Define batch CronJob manifests and exit-code contract" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Six Kubernetes manifests exist — prm005b-daily-premium, clm006b-claim-payment, aud002b-audit-archive, pol006b-renewal, bil003b-billing-generation and cmm001b-commission — each declaring a CronJob with a schedule, a concurrencyPolicy of Forbid, a backoffLimit, an activeDeadlineSeconds bounding its window, ttlSecondsAfterFinished and the WO-011 batch image referenced by digest.
- Check acceptance criterion 2: A documented exit-code contract file maps every legacy failure signal to a distinct non-zero exit status: at least accumulated item errors above threshold, archive verification count mismatch, cursor or query initialization failure, audit-write failure, and configuration-validation failure; exit zero is reserved for a completed run within the error threshold including runs that processed zero items.
- Check acceptance criterion 3: The pcis-batch-common JobExecutionListener implements the contract so a job that would have DISPLAYed an error and continued now terminates the process with the mapped non-zero status, and a corresponding unit test proves each mapped code.

### FUNCTIONAL-012 — Build shared observability starter with PII-masking structured logging

- User Story: WO-013
- Objective: Validate functional behavior for "Build shared observability starter with PII-masking structured logging" against acceptance criteria.
- Expected: Story "Build shared observability starter with PII-masking structured logging" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Adding the pcis-observability-starter dependency to a bare Spring Boot service produces JSON-encoded log output (logstash-logback-encoder) with the mandatory fields correlation_id, service, program, actor, resource, operation, and, for batch runtimes, job_id and run_id — verified by a test that parses emitted log lines as JSON and asserts field presence.
- Check acceptance criterion 2: A Logback masking converter redacts every configured restricted-tier field before emission: TAX_ID rendered as last four characters only, EMAIL rendered as domain only, PHONE, DOB, CUSTOMER_CONTACT_T contact values, address lines and claim payee name fully masked; a unit test feeds each pattern and asserts zero clear-text leakage including when the value appears inside an exception message or a serialized object.
- Check acceptance criterion 3: A Jackson serializer module masks the same annotated fields before any object is written to an audit event payload, proving masking happens at creation and not at display time.

### FUNCTIONAL-013 — Implement structured error library with reason-code registry

- User Story: WO-031
- Objective: Validate functional behavior for "Implement structured error library with reason-code registry" against acceptance criteria.
- Expected: Story "Implement structured error library with reason-code registry" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A shared reason-code registry exists as a versioned artifact seeded from the legacy <MOD>#### convention, including at minimum the evidenced codes for concurrent-update conflict, dependency-blocked delete, all-blank search, result-cap reached and generic system error, plus new codes for audit-write failure, cursor-open failure, archive-verification mismatch, skip-outside-lead-window, unrecognised billing frequency, no-active-commission-plan, commission arithmetic size error, sequence-allocation failure, rating-service non-success, reinsurance-referral write failure, authorization-denied-no-approval and authority-limit-exceeded; a unit test asserts uniqueness, non-reuse and presence of a client-safe title for every code.
- Check acceptance criterion 2: All REST error responses are RFC 9457 problem documents carrying type, title, status, detail, instance, a stable code, correlation_id and an errors array capped at 20 entries with per-entry code, detail and field pointer; HTTP status mapping is 400 for invalid input, 401 unauthenticated, 403 forbidden, 404 not found, 409 conflict, 422 business-rule rejection, 500 unexpected.
- Check acceptance criterion 3: A contract test asserts that no problem document field ever contains SQLSTATE, a native SQL error code, a stack frame, an internal class name or a restricted-tier value, while the same failure written to the structured log does carry the internal diagnostic detail for support.

### FUNCTIONAL-014 — Publish baseline metrics, SLO dashboards and alert rules

- User Story: WO-064
- Objective: Validate functional behavior for "Publish baseline metrics, SLO dashboards and alert rules" against acceptance criteria.
- Expected: Story "Publish baseline metrics, SLO dashboards and alert rules" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A committed baseline report records, per batch job (AUD002B, BIL003B, PRM005B, CMM001B, CLM006B, POL006B replacements), the measured run duration, row counts, per-run database query count and the declared scheduling window, plus measured p95 latency per API endpoint group; every threshold used later in this story references a value from this report or from an already-fixed requirement number.
- Check acceptance criterion 2: A versioned metric catalogue document and matching code registration exist for at least: batch items selected, processed, errors and skipped; job duration seconds; batch window utilisation ratio; audit write failures; archive chunk archived and deleted; archive verification mismatch; commission no-plan count and total amount; delinquent count; reinsurance flagged count and flag failures; authorization denials; database query count per run — all tagged with job, program, run_id and env.
- Check acceptance criterion 3: Grafana dashboards are committed as JSON-as-code with one dashboard per batch job and one per service, and a rendering test or provisioning dry-run proves each dashboard loads without unresolved datasource or variable references.

### FUNCTIONAL-015 — Author operational runbooks for batch, rollback, purge and incidents

- User Story: WO-080
- Objective: Validate functional behavior for "Author operational runbooks for batch, rollback, purge and incidents" against acceptance criteria.
- Expected: Story "Author operational runbooks for batch, rollback, purge and incidents" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Four runbooks are committed under a docs runbooks directory — batch restart and recovery, deployment rollback, purge and archive verification, incident response — each with a fixed section template covering trigger and alert reference, severity, first responder, prerequisites, diagnostic queries and log filters, step-by-step recovery, verification, rollback path, escalation and post-incident actions.
- Check acceptance criterion 2: An alert-to-runbook index maps every alert defined in the alerting story to exactly one runbook section by its runbook reference key, and an automated documentation test fails the build when an alert has no matching section or a section references a non-existent alert.
- Check acceptance criterion 3: The batch restart runbook documents, per job, the evidenced legacy commit boundary and the target chunk configuration — one item per commit for policy, installment, commission and claim-payment jobs and no more than one thousand rows per commit for the audit archive job — plus the restart command, the expected exit-code semantics and how to confirm restart resumed from the last committed chunk.

### FUNCTIONAL-016 — Build audit-svc core with versioned v1 audit event contract

- User Story: WO-010
- Objective: Validate functional behavior for "Build audit-svc core with versioned v1 audit event contract" against acceptance criteria.
- Expected: Story "Build audit-svc core with versioned v1 audit event contract" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: audit-svc starts as a Spring Boot 3.5.x service on Java 21, exposes an actuator health endpoint returning UP, and publishes an OpenAPI 3.1 document for POST /v1/audit-events.
- Check acceptance criterion 2: The v1 audit event contract accepts table name up to 30 chars, business key up to 40 chars, field name up to 30 chars, old and new values up to 100 chars each, actor up to 10 chars, and source program/service up to 64 chars, with no silent truncation on any field.
- Check acceptance criterion 3: Action code is an explicit enumeration; both legacy 3-character codes (ADD, UPD, PAY, REN) and legacy 1-character codes (A, C, D) map to canonical enum values via a documented mapping table, and an unknown code is rejected with HTTP 400 and an RFC 9457 problem detail.

### FUNCTIONAL-017 — Make audit writes atomic with mutations via transactional outbox

- User Story: WO-026
- Objective: Validate functional behavior for "Make audit writes atomic with mutations via transactional outbox" against acceptance criteria.
- Expected: Story "Make audit writes atomic with mutations via transactional outbox" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A shared audit-outbox library provides a single API that enlists an audit event in the caller's active transaction; calling it outside a transaction fails fast with a descriptive error rather than writing anything.
- Check acceptance criterion 2: Flyway migration adds an audit_outbox table (id, payload JSONB, idempotency_key UUID unique, status, attempt_count, next_attempt_at, created_at, last_error) with an index supporting the relay claim query.
- Check acceptance criterion 3: Fault-injection test: with audit persistence forced to fail, the paired financial mutation is rolled back and the database shows 0 mutated rows and 0 audit rows — proving the legacy PRM005B continue-after-failure behaviour is not reproduced.

### FUNCTIONAL-018 — Convert AUD002B archiving into restartable retention and purge job

- User Story: WO-042
- Objective: Validate functional behavior for "Convert AUD002B archiving into restartable retention and purge job" against acceptance criteria.
- Expected: Story "Convert AUD002B archiving into restartable retention and purge job" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A Spring Batch job replaces AUD002B with restart-from-last-committed-chunk behaviour; a fault-injection test kills the job mid-chunk, restarts it, and asserts zero duplicate archived rows and zero rows deleted from live without a verified archive copy.
- Check acceptance criterion 2: Chunk size, retention days per classification tier, and the archive verification tolerance are externalized via configuration properties with no compiled-in constants; the default commit chunk is 1000 rows or fewer (down from the legacy 5000).
- Check acceptance criterion 3: Retention is executed as a monthly partition detach plus cold-archive export rather than DELETE FROM audit_log; an integration test asserts no mass DELETE statement is issued against the live partitioned table.

### FUNCTIONAL-019 — Mask PII and classify data before audit persistence

- User Story: WO-051
- Objective: Validate functional behavior for "Mask PII and classify data before audit persistence" against acceptance criteria.
- Expected: Story "Mask PII and classify data before audit persistence" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A committed classification artifact assigns a tier (Public, Internal, Confidential, Restricted) to all 55 tables listed in PCIS_Database_Design.md, and to every field identified as restricted including CUSTOMER_T.TAX_ID, DOB, EMAIL, PHONE and CLAIM_PAYMENT_T payee.
- Check acceptance criterion 2: A build-time check fails the Maven build when any table in the data dictionary has no tier or when a field marked restricted has no masking strategy assigned.
- Check acceptance criterion 3: Annotation-driven masking is applied before the audit event is constructed: tax ID renders as last four characters, email as domain only, phone as last four digits, date of birth as year only — verified by unit tests for each strategy.

### FUNCTIONAL-020 — Expose audit inquiry API with gated unmask and observability

- User Story: WO-067
- Objective: Validate functional behavior for "Expose audit inquiry API with gated unmask and observability" against acceptance criteria.
- Expected: Story "Expose audit inquiry API with gated unmask and observability" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: GET /v1/audit-events supports filtering by tableName, businessKey, actor, action and an inclusive date range, with keyset pagination and a bounded maximum page size, returning masked values by default.
- Check acceptance criterion 2: Deny-by-default authorization: every endpoint carries an explicit method-level permission check; an unauthenticated request returns 401, a request without the audit-read permission returns 403, and a security test enumerates all endpoints to prove none is reachable without a grant.
- Check acceptance criterion 3: POST /v1/audit-events/{auditId}/unmask requires a dedicated unmask permission, returns the unmasked field value only for the requested field, and writes its own audit event naming the investigator, the audit id, the field revealed and a mandatory justification.

### FUNCTIONAL-021 — Build authz-svc policy decision service with deny-by-default

- User Story: WO-011
- Objective: Validate functional behavior for "Build authz-svc policy decision service with deny-by-default" against acceptance criteria.
- Expected: Story "Build authz-svc policy decision service with deny-by-default" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A POST /v1/authz/decisions endpoint returns 200 with body containing decision (PERMIT or DENY), reasonCode, and evaluatedPermissions for a valid bearer token; missing or expired token returns 401 and an unmapped resource/operation returns decision DENY with reasonCode NO_GRANT.
- Check acceptance criterion 2: Deny-by-default is proven: an integration test asserts that a principal with zero role assignments is denied for every seeded resource/operation pair, and that adding a single grant flips exactly one pair to PERMIT with no side effects on others.
- Check acceptance criterion 3: JWT validation uses local RS256 verification against a cached JWKS (cache TTL configurable, default 1 hour) with no per-request call to the identity provider; a test with a token signed by an untrusted key returns 401 with RFC 9457 problem detail and no stack trace or secret in the response body.

### FUNCTIONAL-022 — Enforce approval linkage and cumulative claim authority limits

- User Story: WO-030
- Objective: Validate functional behavior for "Enforce approval linkage and cumulative claim authority limits" against acceptance criteria.
- Expected: Story "Enforce approval linkage and cumulative claim authority limits" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A POST /v1/authz/claim-payments/decisions request for a claim with no APPROVED approval row linked to the payment request returns decision DENY with reasonCode APPROVAL_MISSING, and no approval row is created or mutated as a side effect.
- Check acceptance criterion 2: Given an APPROVED approval exists but CLAIM_ADJUSTER_T authority limit is less than paid-to-date plus requested amount, the response is DENY with reasonCode AUTHORITY_LIMIT_EXCEEDED — asserted by a test replicating BR-01: a 25000 limit, 20000 already paid and a further 10000 requested is denied even though 10000 alone is within limit.
- Check acceptance criterion 3: Given both checks pass, the response is PERMIT and includes approvalId, approverPrincipal, authorityLimitApplied and cumulativePaidToDate so the caller can record approver identity and the limit applied in the audit event.

### FUNCTIONAL-023 — Apply deny-by-default guards to financial mutation endpoints

- User Story: WO-048
- Objective: Validate functional behavior for "Apply deny-by-default guards to financial mutation endpoints" against acceptance criteria.
- Expected: Story "Apply deny-by-default guards to financial mutation endpoints" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A shared security starter module is published and consumed by at least one domain service; its default SecurityFilterChain denies every request that is not explicitly permitted, proven by a test asserting 401 or 403 on an unmapped path.
- Check acceptance criterion 2: Every mutating application-layer use case carries a method-level authorization guard referencing an explicit permission string; a committed inventory document or generated report lists each mutating endpoint with its required permission and HTTP method.
- Check acceptance criterion 3: The CI gate fails the build when a deliberately added unguarded mutating handler is present and passes once the guard is applied — demonstrated by a committed negative fixture that the gate detects and a green run after remediation.

### FUNCTIONAL-024 — Replace batch actor literals with authenticated service principals

- User Story: WO-049
- Objective: Validate functional behavior for "Replace batch actor literals with authenticated service principals" against acceptance criteria.
- Expected: Story "Replace batch actor literals with authenticated service principals" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A batch job started without valid workload credentials fails before processing the first item, exits with a non-zero status, emits a structured error with job name, resource and operation, and writes no business row.
- Check acceptance criterion 2: A batch job started with valid credentials resolves exactly one service principal for the whole execution; an integration test asserts the principal is available in every step, item reader, processor and writer via the job-scoped actor context.
- Check acceptance criterion 3: Every persisted row written by a migrated job populates crt_user and upd_user from the authenticated principal — proven by a test that asserts no row contains any of the strings BATCHAUD, BATCHBIL, BATCHCMM, BATCHPRM, BATCHCLM or BATCHREN.

### FUNCTIONAL-025 — Automate authorization regression and segregation-of-duties control evidence

- User Story: WO-065
- Objective: Validate functional behavior for "Automate authorization regression and segregation-of-duties control evidence" against acceptance criteria.
- Expected: Story "Automate authorization regression and segregation-of-duties control evidence" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: An authorization regression suite runs in CI and covers, for every financial mutation surface (claim payment, policy renewal, billing generation, commission posting, installment aging, audit purge), the cases permitted, denied for missing grant, denied for missing approval, denied for authority limit exceeded, and denied for unauthenticated caller — with the build failing on any gap.
- Check acceptance criterion 2: A disbursement-integrity check asserts that every claim payment row has exactly one linked CONSUMED approval and a non-null recorded authority limit; the test fails when a fixture inserts a payment without an approval, proving the control detects the violation.
- Check acceptance criterion 3: Consumer-driven contract tests (Spring Cloud Contract or Pact) freeze the authorization decision contract; a deliberately breaking change to a field name or reason-code value fails the build, and additive changes pass.

### FUNCTIONAL-026 — Machine-readable data classification registry for all PCIS entities

- User Story: WO-005
- Objective: Validate functional behavior for "Machine-readable data classification registry for all PCIS entities" against acceptance criteria.
- Expected: Story "Machine-readable data classification registry for all PCIS entities" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A machine-readable registry file (for example src/main/resources/classification/pcis-data-classification.yaml) exists and assigns exactly one tier from Public, Internal, Confidential, Restricted to every entity and to every column of every entity, with no column left unclassified.
- Check acceptance criterion 2: The registry contains an entity reconciliation section that explicitly lists entities present in code but absent from PCIS_Database_Design.md (AUDIT_LOG_ARCHIVE_T, COMMISSION_LEDGER_T) and documents the final authoritative entity count, proving it is not 55.
- Check acceptance criterion 3: AUDIT_LOG_T column names are canonicalized to a single set (audit_log_id, table_name, key_value, action_cd, field_name, old_value, new_value, chg_user, chg_timestamp, program_name) with a documented mapping from each of the three legacy spellings, and the key generation strategy (SEQUENCE versus IDENTITY) is decided and recorded.

### FUNCTIONAL-027 — Shared PII masking library with Jackson and Logback integration

- User Story: WO-017
- Objective: Validate functional behavior for "Shared PII masking library with Jackson and Logback integration" against acceptance criteria.
- Expected: Story "Shared PII masking library with Jackson and Logback integration" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A pcis-pii-masking module exposes an annotation (for example Classified with tier and mask attributes) and a MaskingService whose primary lookup is by entity name plus column name against the data_classification table or in-memory registry loaded in WO-050.
- Check acceptance criterion 2: Mask strategies produce exactly these canonical outputs: tax ID renders last four characters only, email renders domain only, phone renders last four digits only, date of birth renders year only, free-text renders a fixed redaction token, and no strategy ever emits any part of the original beyond what its rule allows.
- Check acceptance criterion 3: A Jackson BeanSerializerModifier masks annotated fields in every serialized payload, verified for API response DTOs and for the audit event payload object.

### FUNCTIONAL-028 — Permission-gated self-audited PII unmask action for investigators

- User Story: WO-035
- Objective: Validate functional behavior for "Permission-gated self-audited PII unmask action for investigators" against acceptance criteria.
- Expected: Story "Permission-gated self-audited PII unmask action for investigators" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A distinct pii:unmask permission exists, is deny-by-default, and is enforced server-side with method-level authorization on the unmask endpoint; no client-side or menu-level gating is relied upon.
- Check acceptance criterion 2: The unmask request requires a non-empty justification of a configured minimum length and a target reference (entity, column, subject id); a request missing any of these returns HTTP 400 with an RFC 9457 problem detail.
- Check acceptance criterion 3: A caller lacking pii:unmask receives HTTP 403 with an RFC 9457 problem detail containing no internal detail, no stack trace and no partial value, and an authorization_denied audit event is emitted with actor, resource and operation.

### FUNCTIONAL-029 — Tiered retention with partitioned audit table and restartable job

- User Story: WO-038
- Objective: Validate functional behavior for "Tiered retention with partitioned audit table and restartable job" against acceptance criteria.
- Expected: Story "Tiered retention with partitioned audit table and restartable job" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: AUDIT_LOG_T is monthly range-partitioned in PostgreSQL via Flyway migrations, with an automated partition pre-creation step so a run never fails for a missing future partition.
- Check acceptance criterion 2: Retention periods are read per classification tier from a versioned, change-audited configuration table (successor to RPT_PARM_T) with an effective-from date and change history, and the audit tier retention can never be configured below one year — an attempt is rejected with a validation error.
- Check acceptance criterion 3: The retention step detaches expired partitions as a metadata operation instead of executing DELETE FROM AUDIT_LOG_T, and a test asserts no row-level DELETE statement is issued against the live audit table during a retention run.

### FUNCTIONAL-030 — Mask PII at audit event creation before outbox persistence

- User Story: WO-043
- Objective: Validate functional behavior for "Mask PII at audit event creation before outbox persistence" against acceptance criteria.
- Expected: Story "Mask PII at audit event creation before outbox persistence" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: The audit service masks old_value and new_value using the WO-051 MaskingService keyed on canonical table_name plus field_name before constructing the persisted audit event and before writing the transactional outbox row.
- Check acceptance criterion 2: Tax ID renders last four characters only, email renders domain only, phone renders last four digits only, DOB renders year only, and free-text narrative fields render the fixed redaction token in every persisted audit event.
- Check acceptance criterion 3: The CUS005A-equivalent erasure path (cascading physical delete of CUSTOMER_CONTACT_T, CUSTOMER_ADDRESS_T and CUSTOMER_T with a full before-image audit set) produces audit rows containing zero cleartext Restricted values, verified by an explicit test.

### FUNCTIONAL-031 — CI gates for unclassified entities and unmasked PII leakage

- User Story: WO-047
- Objective: Validate functional behavior for "CI gates for unclassified entities and unmasked PII leakage" against acceptance criteria.
- Expected: Story "CI gates for unclassified entities and unmasked PII leakage" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A build step (Maven test plus Flyway-applied schema check) fails when any table or column in information_schema is missing from the classification registry, and passes when the registry is complete.
- Check acceptance criterion 2: A committed negative fixture adds an intentionally unclassified table via a test-only migration and a test asserts the classification gate fails for it.
- Check acceptance criterion 3: A custom Semgrep rule set (committed under a ci or semgrep directory) flags logger calls, string concatenations feeding loggers, and audit payload construction that pass a Restricted-tier field without the MaskingService, and the scan:semgrep pipeline step fails the build on any finding.

### FUNCTIONAL-032 — Automated purge with cryptographic erasure and immutable evidence

- User Story: WO-057
- Objective: Validate functional behavior for "Automated purge with cryptographic erasure and immutable evidence" against acceptance criteria.
- Expected: Story "Automated purge with cryptographic erasure and immutable evidence" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A purge job runs on a schedule such that 100 percent of records past their tier retention period are purged within 24 hours of expiry, proven by a time-advanced integration test asserting zero remaining expired records after one scheduled cycle.
- Check acceptance criterion 2: Purge uses physical deletion of detached partitions or per-subject cryptographic erasure via KMS key destruction; soft-delete alone is never used, and a test asserts purged data is unreadable after key destruction.
- Check acceptance criterion 3: Detached partitions are written to object storage with Object Lock in compliance mode plus a lifecycle expiry rule matching the tier retention period, provisioned as infrastructure-as-code, and a test or verification script asserts an overwrite or delete attempt before expiry is rejected.

### FUNCTIONAL-033 — Create versioned tunables and rules configuration schema

- User Story: WO-006
- Objective: Validate functional behavior for "Create versioned tunables and rules configuration schema" against acceptance criteria.
- Expected: Story "Create versioned tunables and rules configuration schema" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Flyway migration creates CONFIG_TUNABLE_T with primary key TUNABLE_KEY VARCHAR(60), plus DOMAIN_CD CHAR(3), VALUE_TYPE CHAR(1), VALUE_TEXT VARCHAR(200), NUMERIC_VALUE DECIMAL(11,2), MIN_VALUE, MAX_VALUE, UNIT_CD, DESCRIPTION, EFFECTIVE_FROM DATE NOT NULL, EFFECTIVE_TO DATE NULL, VERSION_NO INTEGER NOT NULL, and the four standard PCIS audit columns.
- Check acceptance criterion 2: Flyway migration creates append-only CONFIG_TUNABLE_HISTORY_T with BIGINT GENERATED ALWAYS AS IDENTITY surrogate key, TUNABLE_KEY, VERSION_NO, OLD_VALUE, NEW_VALUE, CHANGE_REASON, CHANGED_BY, CHANGED_TIMESTAMP, and a database rule or trigger that rejects UPDATE and DELETE on the table.
- Check acceptance criterion 3: Seed migration inserts exactly six rows with legacy-equivalent values: audit.retention.days=365, audit.archive.chunkSize=1000, billing.leadDays=15, premium.graceDays=10, policy.renewalWindowDays=60, claims.reinsurance.cessionThreshold=100000.00, each with MIN_VALUE and MAX_VALUE bounds and a non-null DESCRIPTION.

### FUNCTIONAL-034 — Typed tunable resolution service with cache and fail-fast validation

- User Story: WO-018
- Objective: Validate functional behavior for "Typed tunable resolution service with cache and fail-fast validation" against acceptance criteria.
- Expected: Story "Typed tunable resolution service with cache and fail-fast validation" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: TunableResolver exposes typed accessors returning int, long, BigDecimal, boolean and String, and throws a distinct TunableNotFoundException or TunableOutOfRangeException rather than returning a silent default when a key is absent or violates its bounds.
- Check acceptance criterion 2: PcisTunableProperties binds compile-time fallback defaults via @ConfigurationProperties with jakarta validation annotations, and the resolution order is documented and tested as database effective row, then property override, then fail-fast.
- Check acceptance criterion 3: Application startup fails with a non-zero exit and a structured log line naming the offending key when any tunable declared required in the TunableKey registry is missing, disabled or outside MIN_VALUE/MAX_VALUE.

### FUNCTIONAL-035 — Admin tunables REST API with RBAC and change evidence

- User Story: WO-036
- Objective: Validate functional behavior for "Admin tunables REST API with RBAC and change evidence" against acceptance criteria.
- Expected: Story "Admin tunables REST API with RBAC and change evidence" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: GET /v1/admin/tunables returns a paged list of tunables with key, domain, current value, unit, bounds, effective dates, version and description; GET /v1/admin/tunables/{key}/history returns the append-only change history newest first.
- Check acceptance criterion 2: PUT /v1/admin/tunables/{key} accepts new value, effective-from date, expected version and change reason, returns 200 with the new version number, and returns 409 with a problem detail when expected version does not match.
- Check acceptance criterion 3: All admin endpoints are deny-by-default: an unauthenticated request returns 401 and an authenticated principal without the configuration-admin authority returns 403, both as RFC 9457 problem details with no tunable values in the body.

### FUNCTIONAL-036 — Admin tunables web panel with versioned change history

- User Story: WO-037
- Objective: Validate functional behavior for "Admin tunables web panel with versioned change history" against acceptance criteria.
- Expected: Story "Admin tunables web panel with versioned change history" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: The admin tunables route renders a table of all tunables returned by GET /v1/admin/tunables showing key, domain, current value, unit, bounds, effective-from and a version badge, with loading, empty and error states implemented.
- Check acceptance criterion 2: The edit drawer performs client-side validation against the bounds and value type returned by the API, requires a non-empty change reason of at least the documented minimum length, and disables submit until the form is valid; server-side rejection reason codes from the API are surfaced as field-anchored plain-language messages.
- Check acceptance criterion 3: Successful submission calls PUT /v1/admin/tunables/{key} with the expected version number, optimistically shows the new version badge, refetches the list and history, and surfaces a 409 conflict as a clear reload-and-retry message rather than a silent failure.

### FUNCTIONAL-037 — Externalized code-table and business rules store service

- User Story: WO-055
- Objective: Validate functional behavior for "Externalized code-table and business rules store service" against acceptance criteria.
- Expected: Story "Externalized code-table and business rules store service" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Flyway migrations create CODE_TABLE_T (domain code, code value, description, sort order, active flag, effective dates, standard audit columns) with a unique constraint on domain plus code value plus effective-from.
- Check acceptance criterion 2: Seed migrations populate at minimum the billing frequency to interval mapping (M, Q, S and the default fallback), the billing schedule status domain including V for void, the reserve status domain including AP and PD, claim type codes and cancellation reason codes, each with a description.
- Check acceptance criterion 3: A CodeTableService exposes typed lookup, list-by-domain and validate-membership operations backed by the same Caffeine cache and refresh pattern established in WO-061, with no direct repository access from domain code.

### FUNCTIONAL-038 — Author Flyway Baseline PostgreSQL Schema Migrations

- User Story: WO-007
- Objective: Validate functional behavior for "Author Flyway Baseline PostgreSQL Schema Migrations" against acceptance criteria.
- Expected: Story "Author Flyway Baseline PostgreSQL Schema Migrations" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Flyway migrations under db/migration create every table in the canonical dictionary, split into six module-group batches, with one SQL file per table plus separate files for indexes, so each object retains independent change control equivalent to the QSQLSRC one-member-per-object convention.
- Check acceptance criterion 2: A Testcontainers PostgreSQL 17 integration test starts an empty database, runs Flyway migrate, and asserts a zero-error result plus an exact table count matching the dictionary; the test is wired into CI as the dev deployment gate.
- Check acceptance criterion 3: An automated assertion compares every column created in the container against the canonical dictionary for name, PostgreSQL type, numeric precision and scale, nullability and default, and fails with the specific column name on any mismatch.

### FUNCTIONAL-039 — Implement Sequence Objects, Block Allocator and Key Formatter

- User Story: WO-019
- Objective: Validate functional behavior for "Implement Sequence Objects, Block Allocator and Key Formatter" against acceptance criteria.
- Expected: Story "Implement Sequence Objects, Block Allocator and Key Formatter" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Flyway migrations create every required sequence: the fifteen declared in the architecture (customer, agent, quote, policy number, coverage, deductible, policy property, policy vehicle, billing schedule, claim number, claim payment, payment, refund, document, audit log) plus the three code-witnessed ones (invoice, commission ledger, and the claim payment sequence name variant), each with explicit INCREMENT, MINVALUE, MAXVALUE, CACHE and NO CYCLE.
- Check acceptance criterion 2: Sequence MAXVALUE and CACHE are set so that generated numeric values remain within the legacy S9(9) host-variable ceiling of 999999999 where a legacy consumer still reads the value, and a unit test asserts the configured maximum for each such sequence.
- Check acceptance criterion 3: A block allocator component fetches sequence values in externally configurable blocks (default 100) and hands them out in memory; an integration test issuing 10000 allocations asserts at most 100 database round trips, proving the at-least-90-percent reduction target.

### FUNCTIONAL-040 — Resolve Schema Discrepancies and Publish Corrected Data Dictionary

- User Story: WO-020
- Objective: Validate functional behavior for "Resolve Schema Discrepancies and Publish Corrected Data Dictionary" against acceptance criteria.
- Expected: Story "Resolve Schema Discrepancies and Publish Corrected Data Dictionary" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A canonical machine-readable data dictionary file exists in the repository (YAML or JSON) covering all 55 designed tables plus the code-witnessed extras COMMISSION_LEDGER_T and AUDIT_LOG_ARCHIVE_T and the premium-engine tables DISCOUNT_RULE_T, SURCHARGE_RULE_T, TAX_TABLE_T and RISK_SCORE_FACTOR_T, with table name, column name, PostgreSQL type, precision and scale, nullability, default, key role and data classification tier for every column.
- Check acceptance criterion 2: A discrepancy register documents at minimum the thirteen identified conflict classes, each with: source-of-truth citation (design document section and COBOL file plus paragraph), the two conflicting definitions, the chosen resolution, the rationale, the affected downstream programs, and a named decision owner.
- Check acceptance criterion 3: Every surrogate key in the dictionary is explicitly classified as either business-document-key-from-sequence (fixed-length VARCHAR or CHAR, prefix plus zero-pad) or detail-surrogate-identity (BIGINT GENERATED ALWAYS AS IDENTITY); BILL_SCHED_ID, INVOICE_ID, LEDGER_ID, RESERVE_ID, CLAIM_PAYMENT_T payment identifier, DEDUCT_ID, POL_PROP_ID and POL_VEH_ID each carry a recorded decision.

### FUNCTIONAL-041 — Automate Masked Anonymized Non-Production Data Refresh

- User Story: WO-021
- Objective: Validate functional behavior for "Automate Masked Anonymized Non-Production Data Refresh" against acceptance criteria.
- Expected: Story "Automate Masked Anonymized Non-Production Data Refresh" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: An anonymization pipeline populates a development-equivalent and a test-equivalent PostgreSQL database from the canonical dictionary and its classification tiers, driven by configuration with no hand-editing of SQL per run, replacing the manual masked-extract step described in the architecture library topology.
- Check acceptance criterion 2: Every restricted-tier field in the recorded inventory is masked or synthesised, including customer tax identifier, date of birth, email, phone and name fields, customer contact value, all customer address fields, agent name, email and phone, agent license number, vehicle identification number, property address fields, claim payment payee, claim note free text, underwriting decision reason, endorsement description, policy history event description, user credentials, and both audit log and audit log archive old and new value columns.
- Check acceptance criterion 3: Masked values remain valid against the documented validation rules: tax identifier retains nine numeric digits, email retains an at sign plus a domain-shaped suffix, phone retains ten digits, postal code retains five or nine digits, state code resolves in the code table state domain, and date of birth yields an age of at least sixteen — each asserted by test.

### FUNCTIONAL-042 — Build Polling Extraction and Idempotent PostgreSQL Loader

- User Story: WO-039
- Objective: Validate functional behavior for "Build Polling Extraction and Idempotent PostgreSQL Loader" against acceptance criteria.
- Expected: Story "Build Polling Extraction and Idempotent PostgreSQL Loader" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A per-domain extract definition set exists for Customer, Claims, Billing, Premium, Commission, Policy and Audit, each naming its source tables, its watermark column, its explicit projected column list (never SELECT star) and its target table mapping, and each is validated at startup against the canonical data dictionary.
- Check acceptance criterion 2: The loader is idempotent: running the same extract batch twice produces identical target row counts and identical column values, verified by an integration test that loads a fixture batch, re-loads it, and asserts zero duplicate keys and zero changed values.
- Check acceptance criterion 3: Commit chunk size is externally configurable and defaults to at most 1000 rows, satisfying the requirement to reduce commit blast radius from the legacy 5000; the configured value is asserted by test and logged at run start.

### FUNCTIONAL-043 — Build Nightly Cent-Level Parallel-Run Reconciliation Harness

- User Story: WO-058
- Objective: Validate functional behavior for "Build Nightly Cent-Level Parallel-Run Reconciliation Harness" against acceptance criteria.
- Expected: Story "Build Nightly Cent-Level Parallel-Run Reconciliation Harness" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A nightly reconciliation job compares source and target per domain for Billing, Premium and delinquency, Commission, Claims, Policy and renewal, and Audit, asserting row counts, cent-level amount equality and column checksums, and it reads the PostgreSQL streaming read replica rather than the OLTP primary.
- Check acceptance criterion 2: Per-domain amount and invariant assertions are implemented exactly as specified: billing compares due amount, paid amount, invoice amount and annual premium plus installment number, due date and status with the installment equal to annual premium divided by installment count at NUMERIC(9,2) using HALF_UP; premium compares base premium, final premium and total factor at NUMERIC(7,4) plus status transitions under a ten-day grace; commission compares commission amount, rate and paid amount plus the count of rows flagged as commissioned; claims compares approved amount, paid to date, payment amount and recovery amount plus the reserve status transition and the 100000.00 cession threshold; policy compares annual premium, limit amount and premium amount plus expiry status, exactly two history events and unchanged policy number format; audit compares row counts and the archive-then-delete invariant.
- Check acceptance criterion 3: Cross-cutting assertions are implemented: sequence high-water marks with zero key collisions, NUMERIC scale preservation per column, NULL-versus-blank fidelity, CHAR trailing-space semantics and UPD_TIMESTAMP microsecond fidelity so optimistic locking is not silently broken.

### FUNCTIONAL-044 — Deterministic Seed Data Harness for Batch Regression Fixtures

- User Story: WO-014
- Objective: Validate functional behavior for "Deterministic Seed Data Harness for Batch Regression Fixtures" against acceptance criteria.
- Expected: Story "Deterministic Seed Data Harness for Batch Regression Fixtures" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Running the same named scenario twice in a row produces byte-identical extracts of every seeded table, including all DECIMAL(9,2) and DECIMAL(11,2) columns, verified by a checksum assertion in the harness self-test.
- Check acceptance criterion 2: Scenario catalog covers at minimum: billing frequency M, Q, S and an out-of-domain value; days-out exactly 15, 14 and 16 relative to the fixed reference date; installments exactly 10, 9 and 11 days past due; reserves where APPROVED_AMT exceeds PAID_TO_DATE and reserves where it does not; payment amounts at 99999.99, 100000.00 and 100000.01 against the cession threshold; agents with an in-force commission plan and agents with none; audit rows immediately either side of a 365-day cutoff.
- Check acceptance criterion 3: All business document keys (CUST_ID, AGT_ID, POL_NBR, CLM_NBR) are allocated from harness-controlled SEQUENCE objects as fixed-length character values, never IDENTITY columns, and detail surrogate keys are reset to a known start value before every scenario load.

### FUNCTIONAL-045 — Capture COBOL Baseline Golden Outputs with Determinism Controls

- User Story: WO-032
- Objective: Validate functional behavior for "Capture COBOL Baseline Golden Outputs with Determinism Controls" against acceptance criteria.
- Expected: Story "Capture COBOL Baseline Golden Outputs with Determinism Controls" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A single documented command per program per scenario restores the seeded library, runs the COBOL program with an injected reference date, and writes a canonical golden artifact set to a deterministic path under the golden resources tree.
- Check acceptance criterion 2: Three consecutive capture runs of every program/scenario pair produce byte-identical artifacts; any pair that does not is automatically quarantined with a recorded reason rather than committed as a golden.
- Check acceptance criterion 3: Captured artifacts include full post-run row images for every mutated table, the RPT_RUN_LOG_T counter row, program DISPLAY output, and the final program completion status, with timestamps normalised and generated surrogate keys rewritten to stable ordinal placeholders.

### FUNCTIONAL-046 — Cent-Level Golden Output Comparison Engine and Diff Reporting

- User Story: WO-033
- Objective: Validate functional behavior for "Cent-Level Golden Output Comparison Engine and Diff Reporting" against acceptance criteria.
- Expected: Story "Cent-Level Golden Output Comparison Engine and Diff Reporting" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A single assertion entry point compares actual post-run state against a named golden and passes only when every table, row and column matches, with zero tolerance on all NUMERIC(9,2) and NUMERIC(11,2) columns.
- Check acceptance criterion 2: A one-cent difference in any monetary column, a missing row, an extra row, or a status-code difference each cause a distinct, clearly worded failure naming table, business key, column, expected value, actual value and delta.
- Check acceptance criterion 3: Normalisation is declarative and auditable: only fields explicitly listed as non-deterministic (capture timestamps, IDENTITY surrogate keys, job identifiers) may be masked, and an attempt to normalise a monetary or status column fails the comparator's own configuration validation.

### FUNCTIONAL-047 — Golden-Output Regression Suites for Six Batch Programs

- User Story: WO-053
- Objective: Validate functional behavior for "Golden-Output Regression Suites for Six Batch Programs" against acceptance criteria.
- Expected: Story "Golden-Output Regression Suites for Six Batch Programs" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: One regression suite exists per batch program (AUD002B, BIL003B, CMM001B, PRM005B, CLM006B, POL006B) with a test method per committed scenario, each asserting table-level cent equivalence, RPT_RUN_LOG_T counter parity and commit-boundary parity via the WO-082 comparator.
- Check acceptance criterion 2: Characterization suites exist for the two evidenced interactive transactions (customer creation per CUS001A and policy creation per POL001A), asserting the multi-row insert set, duplicate tax-ID rejection, and the created billing schedule row.
- Check acceptance criterion 3: A coverage guard test enumerates the golden artifact tree and fails the build if any program or scenario present in the goldens has no corresponding executing test, so coverage cannot silently regress to below 100 percent of captured behaviour.

### FUNCTIONAL-048 — Batch Fault Injection Proving Restart Without Duplicate Writes

- User Story: WO-054
- Objective: Validate functional behavior for "Batch Fault Injection Proving Restart Without Duplicate Writes" against acceptance criteria.
- Expected: Story "Batch Fault Injection Proving Restart Without Duplicate Writes" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A fault-injection API allows a test to fail a job at a configurable item index, at a configurable statement ordinal, or by aborting the process mid-chunk, and to then restart the job through the Spring Batch JobOperator restart path.
- Check acceptance criterion 2: For every one of the six batch jobs, a restart test asserts the final state after failure-plus-restart is identical to the clean-run golden via the WO-082 comparator, with zero duplicate and zero orphaned financial rows.
- Check acceptance criterion 3: Named invariants are asserted after every fault-injection run: no duplicate COMMISSION_LEDGER_T row for the same BILL_SCHED_ID; no CLAIM_PAYMENT_T row without a corresponding reserve status transition; no AUDIT_LOG_T row deleted without a verified AUDIT_LOG_ARCHIVE_T copy; no BILLING_SCHEDULE_T installment without its paired INVOICE_T row; no financial mutation persisted without its audit event.

### FUNCTIONAL-049 — CI Gate Wiring Coverage and Regression Evidence Artifacts

- User Story: WO-078
- Objective: Validate functional behavior for "CI Gate Wiring Coverage and Regression Evidence Artifacts" against acceptance criteria.
- Expected: Story "CI Gate Wiring Coverage and Regression Evidence Artifacts" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A pipeline definition runs, in order, the Maven build with unit tests, the Testcontainers-backed golden-output regression suite, and the fault-injection restart suite, and any failure in any of the three blocks the build before any image push or deployment step.
- Check acceptance criterion 2: JaCoCo coverage enforcement is scoped to the monetary calculation packages and fails the build below 90 percent line coverage, with the scoped package list committed as configuration rather than hardcoded in a script.
- Check acceptance criterion 3: On failure, the comparator JSON and text diff reports, the fault-injection invariant results and the coverage report are published as retained build artifacts and the build summary names the failing program and scenario without requiring a local rerun.

### FUNCTIONAL-050 — Create PostgreSQL customer schema with Flyway migrations

- User Story: WO-068
- Objective: Validate functional behavior for "Create PostgreSQL customer schema with Flyway migrations" against acceptance criteria.
- Expected: Story "Create PostgreSQL customer schema with Flyway migrations" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Flyway migration V1 creates customer schema objects: CUSTOMER_T, CUSTOMER_ADDRESS_T, CUSTOMER_CONTACT_T, and three sequences, and applies cleanly against an empty PostgreSQL 17 database.
- Check acceptance criterion 2: CUST_ID is VARCHAR(10) populated from SEQ_CUSTOMER_ID via a zero-padded formatting helper, never a SERIAL or IDENTITY column; ADDRESS_ID and CONTACT_ID are BIGINT GENERATED ALWAYS AS IDENTITY per the design document convention.
- Check acceptance criterion 3: Column types mirror the Db2 for i design: CUST_NAME VARCHAR(60), TAX_ID VARCHAR(11), DOB DATE, EMAIL VARCHAR(60), PHONE VARCHAR(15), CUST_STATUS CHAR(1), CREDIT_SCORE SMALLINT, plus CRT_USER, CRT_TIMESTAMP, UPD_USER, UPD_TIMESTAMP on every table.

### FUNCTIONAL-051 — Build customer-svc domain and persistence layers

- User Story: WO-083
- Objective: Validate functional behavior for "Build customer-svc domain and persistence layers" against acceptance criteria.
- Expected: Story "Build customer-svc domain and persistence layers" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A customer-svc Maven module exists with four packages (controller, application, domain, infrastructure) and an enforced dependency rule: the domain package imports no Spring, JPA or Jackson types, verified by an ArchUnit test.
- Check acceptance criterion 2: Domain layer contains Customer, CustomerAddress and CustomerContact with invariants ported from CUS001A and CUS_Module_Design_Document.md: mandatory name, customer type and tax ID; enumerated gender, marital status and customer status; credit score bounded 0 to 999; optional address and contact groups all-or-nothing.
- Check acceptance criterion 3: CustomerValidator replaces CUSVAL01 and returns a structured collection of field-anchored violations rather than terse coded messages, and all violations for one submission are returned together as the COBOL WS-MSG-ENTRY table did.

### FUNCTIONAL-052 — Expose versioned customer REST API with deny-by-default authorization

- User Story: WO-084
- Objective: Validate functional behavior for "Expose versioned customer REST API with deny-by-default authorization" against acceptance criteria.
- Expected: Story "Expose versioned customer REST API with deny-by-default authorization" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Endpoints exist and are documented in a generated OpenAPI 3.1 contract: POST /v1/customers, GET /v1/customers/{custId}, PATCH /v1/customers/{custId}, GET /v1/customers (paged search by name, tax-ID suffix and status).
- Check acceptance criterion 2: Spring Security 6 is configured as an OAuth2 resource server with local RS256 validation against a cached JWKS; requests with no or an invalid token return 401 and never reach the application layer.
- Check acceptance criterion 3: Deny-by-default is enforced: the security configuration denies any request not explicitly permitted, every controller method carries @PreAuthorize, and an automated test enumerates all mapped endpoints and fails if any lacks an authorization annotation.

### FUNCTIONAL-053 — Publish masked customer read projection for downstream consumers

- User Story: WO-085
- Objective: Validate functional behavior for "Publish masked customer read projection for downstream consumers" against acceptance criteria.
- Expected: Story "Publish masked customer read projection for downstream consumers" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: GET /v1/customers/{custId}/projection returns a documented, versioned read projection containing only the fields evidenced as needed by policy, claims and reporting consumers, with a committed field-to-consumer mapping document.
- Check acceptance criterion 2: All restricted-tier fields in the projection are masked before serialization: tax ID as last four characters, email as domain only, phone as last four digits, date of birth as year only; the unmasked variants are not present anywhere in the payload.
- Check acceptance criterion 3: The projection endpoint is read-only, requires an explicit service-to-service scope such as customer:project, and denies by default; no mutating verb is exposed on the projection path.

### FUNCTIONAL-054 — Establish customer slice parity harness and CI quality gates

- User Story: WO-091
- Objective: Validate functional behavior for "Establish customer slice parity harness and CI quality gates" against acceptance criteria.
- Expected: Story "Establish customer slice parity harness and CI quality gates" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A parity test module exists that loads recorded baseline fixtures representing CUS001A input/output pairs (customer with address and contact, customer without optional data, duplicate tax ID rejection, invalid field set, boundary credit score) and replays them through customer-svc.
- Check acceptance criterion 2: A reconciliation comparator asserts field-level equivalence between baseline expected records and customer-svc persisted records, reporting differences as a structured list of table, key, field, expected and actual rather than a single boolean.
- Check acceptance criterion 3: The suite runs offline in one command with Testcontainers PostgreSQL and a mock OIDC issuer, requiring no IBM i connection, no shared database and no external identity provider.

### FUNCTIONAL-055 — Provision Claims PostgreSQL Schema With Exact Decimal Precision

- User Story: WO-027
- Objective: Validate functional behavior for "Provision Claims PostgreSQL Schema With Exact Decimal Precision" against acceptance criteria.
- Expected: Story "Provision Claims PostgreSQL Schema With Exact Decimal Precision" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Flyway migration scripts create all Claims tables (CLAIM_T, CLAIM_RESERVE_T, CLAIM_RESERVE_HISTORY_T, CLAIM_PAYMENT_T, CLAIM_ADJUSTER_T, CLAIM_NOTE_T, APPROVAL_T, RECOVERY_T) and run cleanly from an empty PostgreSQL 17 database with zero manual steps.
- Check acceptance criterion 2: Every monetary column is NUMERIC(9,2) or NUMERIC(11,2) and a schema assertion test fails the build if any money column uses double precision, real or an unscaled numeric type.
- Check acceptance criterion 3: CLM_NBR, CLAIM_PAYMENT_ID business keys are allocated from PostgreSQL SEQUENCE objects producing fixed-length VARCHAR values of the documented widths; no business document key uses an IDENTITY column.

### FUNCTIONAL-056 — Model Claim Approval Lifecycle As First-Class Record

- User Story: WO-044
- Objective: Validate functional behavior for "Model Claim Approval Lifecycle As First-Class Record" against acceptance criteria.
- Expected: Story "Model Claim Approval Lifecycle As First-Class Record" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: An approval request can be created for a claim with requested amount, requesting adjuster identity, claim number and reserve reference, and is persisted in APPROVAL_T with status PENDING.
- Check acceptance criterion 2: A decision transition records decision code (approved or denied), rationale text, approver principal identity, approver authority limit at decision time and decision timestamp, and moves status to APPROVED or DENIED.
- Check acceptance criterion 3: An approval whose approver principal equals the requesting principal is rejected with a distinct segregation-of-duties reason code and never persisted in APPROVED state.

### FUNCTIONAL-057 — Enforce Dual Payment Authority Check In Claims Domain

- User Story: WO-060
- Objective: Validate functional behavior for "Enforce Dual Payment Authority Check In Claims Domain" against acceptance criteria.
- Expected: Story "Enforce Dual Payment Authority Check In Claims Domain" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A PaymentAuthorityService evaluates both checks in a single method and returns either a permit decision carrying approver identity and applied authority limit, or a denial carrying exactly one stable reason code.
- Check acceptance criterion 2: Given no qualifying approval, evaluation denies with reason code APPROVAL_REQUIRED, no CLAIM_PAYMENT_T row is written and the reserve remains at status AP.
- Check acceptance criterion 3: Given a qualifying approval but CLAIM_ADJUSTER_T.AUTHORITY_LIMIT less than PAID_TO_DATE plus payment amount, evaluation denies with the distinct reason code AUTHORITY_LIMIT_EXCEEDED.

### FUNCTIONAL-058 — Expose Versioned Claims REST API With Deny-By-Default

- User Story: WO-066
- Objective: Validate functional behavior for "Expose Versioned Claims REST API With Deny-By-Default" against acceptance criteria.
- Expected: Story "Expose Versioned Claims REST API With Deny-By-Default" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Endpoints exist and are documented in generated OpenAPI 3.1 for claim create and get, reserve set and adjust, approval request and decision, payment create, and claim payment list under the /v1/claims path family.
- Check acceptance criterion 2: Every mutating endpoint denies unauthenticated and unauthorized callers server-side: an integration test proves 401 without a token and 403 with a token lacking the required authority, and no endpoint relies on UI-level gating.
- Check acceptance criterion 3: POST payment returns 201 with the created payment on permit, and 403 with an RFC 9457 problem detail whose reason code distinguishes APPROVAL_REQUIRED from AUTHORITY_LIMIT_EXCEEDED on denial.

### FUNCTIONAL-059 — Instrument Claims Payment Observability And SoD Evidence

- User Story: WO-081
- Objective: Validate functional behavior for "Instrument Claims Payment Observability And SoD Evidence" against acceptance criteria.
- Expected: Story "Instrument Claims Payment Observability And SoD Evidence" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: All claims-svc and claims batch logs are emitted as structured JSON including actor, resource, operation, reason code, correlation id and job execution id, with no free-text-only error lines.
- Check acceptance criterion 2: A logging masking converter is active such that an automated log scan over the test suite output finds zero unmasked restricted-tier values (tax id, email, phone, payee name); the scan runs in CI and fails the build on any hit.
- Check acceptance criterion 3: OpenTelemetry traces link an incoming payment API request or batch step to the authority evaluation and the payment write, and the trace id appears in the corresponding log lines.

### FUNCTIONAL-060 — Convert CLM006B Claim Payment Batch To Spring Batch

- User Story: WO-090
- Objective: Validate functional behavior for "Convert CLM006B Claim Payment Batch To Spring Batch" against acceptance criteria.
- Expected: Story "Convert CLM006B Claim Payment Batch To Spring Batch" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A Spring Batch job reads payable reserves with a single set-based query (status AP, APPROVED_AMT greater than PAID_TO_DATE, joined to claim and adjuster) with no per-row round trips for derived values.
- Check acceptance criterion 2: Chunk size is one item per commit, configurable, so a single failing claim is skipped or recorded as an exception without blocking the remainder of the run, matching the legacy prologue commit scope.
- Check acceptance criterion 3: Every payment write passes through the shared PaymentAuthorityService: reserves without a qualifying approval or exceeding cumulative authority are not paid, produce an exception record with the distinct reason code, and leave the reserve at status AP.

### FUNCTIONAL-061 — Build Claim Payment Golden-Output Parity Harness

- User Story: WO-097
- Objective: Validate functional behavior for "Build Claim Payment Golden-Output Parity Harness" against acceptance criteria.
- Expected: Story "Build Claim Payment Golden-Output Parity Harness" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A deterministic seeded claims dataset is committed and loadable into both a Db2-shaped baseline fixture and PostgreSQL, covering payable reserves, missing approvals, overlimit adjusters, threshold breaches and zero-outstanding reserves.
- Check acceptance criterion 2: Golden output files capture expected CLAIM_PAYMENT_T rows, final CLAIM_RESERVE_T states, RECOVERY_T referrals and run-log counters derived from the CLM006B baseline semantics, and are committed under version control.
- Check acceptance criterion 3: A comparison engine asserts equality of every monetary field at scale 2 with zero tolerance and reports breaks as a structured list of claim number, table, field, expected and actual.

### FUNCTIONAL-062 — Migrate billing and commission schema with set-based readers

- User Story: WO-056
- Objective: Validate functional behavior for "Migrate billing and commission schema with set-based readers" against acceptance criteria.
- Expected: Story "Migrate billing and commission schema with set-based readers" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Flyway migrations create BILLING_PLAN_T, BILLING_SCHEDULE_T, INVOICE_T, AGENT_COMMISSION_T, COMMISSION_LEDGER_T and the batch run-log table on a clean PostgreSQL 17 container, with all monetary columns declared NUMERIC(9,2) or NUMERIC(11,2) and all business document keys as fixed-length VARCHAR/CHAR fed by SEQUENCE objects, never IDENTITY.
- Check acceptance criterion 2: A single billing candidate query returns pol_nbr, prem_annual, bill_freq, installment_cnt, last_installment_nbr, last_due_date, computed next_due_date and computed days_out, with zero additional per-row SQL statements verified by a query-count assertion in the test.
- Check acceptance criterion 3: A single delinquency candidate query returns bill_sched_id, pol_nbr, due_date, due_amt, paid_amt, bill_status and computed days_past_due for installments with status due or late and due_date on or before the run date.

### FUNCTIONAL-063 — Convert BIL003B billing generation to Spring Batch job

- User Story: WO-070
- Objective: Validate functional behavior for "Convert BIL003B billing generation to Spring Batch job" against acceptance criteria.
- Expected: Story "Convert BIL003B billing generation to Spring Batch job" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A Spring Batch job named billing-generation runs with the set-based candidate reader from WO-110, a pure-domain processor and a composite writer, committing exactly one policy per chunk so a single failure does not block the remaining population.
- Check acceptance criterion 2: Installment amount equals annual premium divided by installment count computed with BigDecimal divide at scale 2 and HALF_UP, and the generated rows match committed golden output for installment number, due date, amount and status for 100 percent of seeded rows.
- Check acceptance criterion 3: Next due date is computed as last due date plus 1 month for M, 3 months for Q, 6 months for S and 1 year for any other value, asserted for each frequency including the out-of-domain case.

### FUNCTIONAL-064 — Convert PRM005B delinquency aging to Spring Batch job

- User Story: WO-071
- Objective: Validate functional behavior for "Convert PRM005B delinquency aging to Spring Batch job" against acceptance criteria.
- Expected: Story "Convert PRM005B delinquency aging to Spring Batch job" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A Spring Batch job named delinquency-aging reads candidates through the WO-110 set-based query with days_past_due computed in SQL, and commits exactly one installment per chunk.
- Check acceptance criterion 2: Status transition logic matches the COBOL exactly: paid_amt greater than or equal to due_amt becomes paid; otherwise days_past_due greater than the configured grace period becomes late and increments the delinquency counter; otherwise the status remains due.
- Check acceptance criterion 3: An update is issued only when the newly computed status differs from the current status, and unchanged installments produce no write, no audit event and no run-log update count, matching legacy behaviour.

### FUNCTIONAL-065 — Convert CMM001B commission calculation to Spring Batch job

- User Story: WO-072
- Objective: Validate functional behavior for "Convert CMM001B commission calculation to Spring Batch job" against acceptance criteria.
- Expected: Story "Convert CMM001B commission calculation to Spring Batch job" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A Spring Batch job named commission-calculation reads paid installments with comm_calc_flag null joined to the policy agent and in-force plan through the WO-110 query, committing exactly one installment per chunk.
- Check acceptance criterion 2: Commission amount equals paid amount multiplied by rate divided by 100 computed with BigDecimal at scale 2 and HALF_UP, matching committed golden output to the cent for every seeded rate including four-decimal rates such as 12.3456.
- Check acceptance criterion 3: The commission ledger insert, the installment comm_calc_flag update to Y and the audit event commit as one transaction; an injected audit failure leaves no ledger row and no flag update.

### FUNCTIONAL-066 — Gate billing batch cutover with parallel-run reconciliation

- User Story: WO-088
- Objective: Validate functional behavior for "Gate billing batch cutover with parallel-run reconciliation" against acceptance criteria.
- Expected: Story "Gate billing batch cutover with parallel-run reconciliation" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A reconciliation job compares the PostgreSQL billing schedule, invoice and commission ledger state against the Db2 for i baseline extract for a given run date, matching on business key and asserting installment number, due date, amount, status, commission rate and commission amount to the cent at scale 2.
- Check acceptance criterion 2: Reconciliation output is a persisted, queryable break report with one row per break carrying break type, business key, expected value, actual value and a severity, and the job exits non-zero when unexplained breaks exceed the configured threshold of zero.
- Check acceptance criterion 3: The CI pipeline runs golden-output regression for billing generation, delinquency aging and commission calculation on every commit and fails the build on any mismatch in amount, count, status or commit boundary.

### FUNCTIONAL-067 — Expose billing-svc REST API and batch operations endpoints

- User Story: WO-089
- Objective: Validate functional behavior for "Expose billing-svc REST API and batch operations endpoints" against acceptance criteria.
- Expected: Story "Expose billing-svc REST API and batch operations endpoints" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A versioned OpenAPI 3.1 document is generated from code and published, covering GET billing schedules by policy, GET invoices by policy, GET aging summary, GET commission ledger by agent, GET batch runs, GET batch run exceptions and POST trigger endpoints for the three jobs.
- Check acceptance criterion 2: All endpoints require a validated bearer JWT; unauthenticated requests return 401 and requests lacking the required authority return 403 with a distinct machine-readable reason code, with deny-by-default enforced by method-level authorization on every handler.
- Check acceptance criterion 3: Every error response is an RFC 9457 problem detail containing type, title, status, detail and a correlation identifier, and no response body or log line contains a stack trace, SQL text or unmasked restricted-tier value.

### FUNCTIONAL-068 — Freeze versioned v1 premium rating OpenAPI contract

- User Story: WO-028
- Objective: Validate functional behavior for "Freeze versioned v1 premium rating OpenAPI contract" against acceptance criteria.
- Expected: Story "Freeze versioned v1 premium rating OpenAPI contract" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A file premium-rating-v1.yaml exists in the api contracts directory, is a valid OpenAPI 3.1 document, and passes an OpenAPI linter with zero errors in the build.
- Check acceptance criterion 2: Every input field from both evidenced PRMCLC01 parameter lists (policy type, coverage type, territory, limit, state, old premium) and every output field (premium, base rate, rating factor, return code, underwriting decision) has a corresponding contract field documented in the data dictionary with source COBOL PIC clause, target JSON type, decimal scale and rounding mode.
- Check acceptance criterion 3: The response schema exposes the full breakdown required by the policy issuance UI: composite risk score, risk tier, base rate, ordered factor list, discount list, surcharge list, tax list, final premium, and calculation snapshot identifier.

### FUNCTIONAL-069 — Scaffold premium-svc service and rating data access layer

- User Story: WO-045
- Objective: Validate functional behavior for "Scaffold premium-svc service and rating data access layer" against acceptance criteria.
- Expected: Story "Scaffold premium-svc service and rating data access layer" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A premium-svc Maven module builds from a clean checkout with a single command and produces a runnable Spring Boot 3.5.x application on Java 21 with no manual environment setup steps.
- Check acceptance criterion 2: Flyway migrations create all rating read tables and write tables with monetary columns as NUMERIC(9,2) or NUMERIC(11,2), business document keys as fixed-length VARCHAR or CHAR fed by SEQUENCE objects, child surrogate keys as BIGINT GENERATED ALWAYS AS IDENTITY, and the standard CRT_USER, CRT_TIMESTAMP, UPD_USER, UPD_TIMESTAMP columns on every table.
- Check acceptance criterion 3: All contract paths from WO-120 are wired as controllers that return an RFC 9457 problem detail with reason code PRM_NOT_IMPLEMENTED until WO-122 lands; no endpoint returns a stack trace.

### FUNCTIONAL-070 — Implement rating engine with exact decimal arithmetic

- User Story: WO-062
- Objective: Validate functional behavior for "Implement rating engine with exact decimal arithmetic" against acceptance criteria.
- Expected: Story "Implement rating engine with exact decimal arithmetic" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: The rating use case executes the documented PRMCLC01 sequence in order: input validation, composite risk score and tier, base rate and factor lookup with base premium equal to base rate multiplied by rating factor, discounts, surcharges, taxes, final premium — and the order is asserted by a test.
- Check acceptance criterion 2: All monetary arithmetic uses BigDecimal with RoundingMode.HALF_UP and rounds to two decimal places at each stage documented in PRM_Premium_Calculation_Engine_Design.md; no double, float or unscaled division appears anywhere in the domain package, enforced by an automated check.
- Check acceptance criterion 3: The domain package contains no framework imports and is unit testable without a database or HTTP layer, enforced by an architecture test.

### FUNCTIONAL-071 — Build golden-output parity harness for premium rating

- User Story: WO-069
- Objective: Validate functional behavior for "Build golden-output parity harness for premium rating" against acceptance criteria.
- Expected: Story "Build golden-output parity harness for premium rating" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A scenario matrix is defined and committed covering at minimum: homeowners and commercial policy types, each documented billing and rating path, both evidenced PRMCLC01 caller shapes, discount-only, surcharge-only, tax-inclusive, zero-discount, maximum-factor, half-cent rounding boundary, NUMERIC ceiling overflow, underwriting decline, underwriting referral, and missing reference data.
- Check acceptance criterion 2: Golden output files are committed in a stable machine-readable format (one file per scenario with all breakdown fields as exact decimal strings) and are traceable to the algorithm sections of PRM_Premium_Calculation_Engine_Design.md that define them.
- Check acceptance criterion 3: Database seeding is deterministic and versioned: running the harness twice from a clean state produces byte-identical outputs, and the seed is expressed as versioned migration or fixture scripts rather than ad hoc inserts.

### FUNCTIONAL-072 — Add underwriting rule evaluation and referral outcome tracking

- User Story: WO-076
- Objective: Validate functional behavior for "Add underwriting rule evaluation and referral outcome tracking" against acceptance criteria.
- Expected: Story "Add underwriting rule evaluation and referral outcome tracking" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Underwriting rules are evaluated from UW_RULE_T after risk scoring and before base rate lookup, matching the documented PRMCLC01 step order, and the ordering is asserted by a test.
- Check acceptance criterion 2: A DECLINE outcome returns HTTP 200 with return code 02, underwriting decision DECLINE, the matched rule identifier and reason text, no premium value, and exactly zero rows written to PREMIUM_CALC_T and PREMIUM_CALC_DETAIL_T.
- Check acceptance criterion 3: A DECLINE outcome writes exactly one audit record capturing actor, resource, operation, matched rule and decision, committed in the same transaction as the decision handling.

### FUNCTIONAL-073 — Enforce rating contract with consumer-driven tests and CI gate

- User Story: WO-077
- Objective: Validate functional behavior for "Enforce rating contract with consumer-driven tests and CI gate" against acceptance criteria.
- Expected: Story "Enforce rating contract with consumer-driven tests and CI gate" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Consumer-driven contract definitions exist for at least four consumers of the rating contract (policy issuance, policy endorsement or renewal batch, quote, billing) covering approve, refer, decline and caller-input-error interactions.
- Check acceptance criterion 2: Provider verification runs against premium-svc in the build and fails if any consumer expectation is unsatisfied, with the failing consumer and interaction named in the output.
- Check acceptance criterion 3: An OpenAPI diff gate compares the pull request contract against the last released contract and fails the build on any breaking change (field removal, type narrowing, required constraint added, enum value removed, path removal), naming the offending element.

### FUNCTIONAL-074 — Migrate Policy Domain Schema to PostgreSQL with Flyway

- User Story: WO-086
- Objective: Validate functional behavior for "Migrate Policy Domain Schema to PostgreSQL with Flyway" against acceptance criteria.
- Expected: Story "Migrate Policy Domain Schema to PostgreSQL with Flyway" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Flyway migrations under the policy-svc module create POLICY_T, COVERAGE_T, DEDUCTIBLE_T, POLICY_HISTORY_T, POLICY_PROPERTY_T, POLICY_VEHICLE_T, ENDORSEMENT_T and BILLING_PLAN_T references with all four standard audit columns (CRT_USER, CRT_TIMESTAMP, UPD_USER, UPD_TIMESTAMP) present on every table.
- Check acceptance criterion 2: POL_NBR is CHAR/VARCHAR(12) populated from a PostgreSQL SEQUENCE via a documented formatter, never IDENTITY; DEDUCT_ID, POL_PROP_ID and POL_VEH_ID are BIGINT GENERATED ALWAYS AS IDENTITY, and the resolution of the design-document conflict is recorded as a comment in the migration and in the module README.
- Check acceptance criterion 3: All monetary columns are NUMERIC(9,2) or NUMERIC(11,2) with a test asserting a round-trip of the boundary values 999999999.99 and 99999999999.99 through JDBC BigDecimal loses no precision and applies no implicit rounding.

### FUNCTIONAL-075 — Build policy-svc REST API with Versioned Read Contract

- User Story: WO-092
- Objective: Validate functional behavior for "Build policy-svc REST API with Versioned Read Contract" against acceptance criteria.
- Expected: Story "Build policy-svc REST API with Versioned Read Contract" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: GET /v1/policies/{polNbr} returns the published policy projection including POL_TYPE, CUST_ID, AGT_ID, EFF_DATE, EXP_DATE, POL_STATUS, PREM_ANNUAL as a string-serialized exact decimal, and coverage lines; 404 with an RFC 9457 problem detail is returned for an unknown policy number.
- Check acceptance criterion 2: Every mutating endpoint is annotated with a method-level authorization check and an integration test proves an authenticated principal without the required authority receives 403 with a distinct problem-detail reason code, while an unauthenticated request receives 401.
- Check acceptance criterion 3: Policy creation and endorsement persist the entity change and an audit outbox record in one transaction; an injected outbox-write failure rolls back the policy mutation and the endpoint returns 500 with no partial row, proving the legacy continue-after-audit-failure behaviour is not reproduced.

### FUNCTIONAL-076 — Convert POL006B Renewal Batch to Restartable Spring Batch Job

- User Story: WO-093
- Objective: Validate functional behavior for "Convert POL006B Renewal Batch to Restartable Spring Batch Job" against acceptance criteria.
- Expected: Story "Convert POL006B Renewal Batch to Restartable Spring Batch Job" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A Spring Batch job named policyRenewalJob reads renewal candidates in a single set-based query that computes days-to-expiry and eligibility server-side, with zero per-row date-arithmetic round trips verified by a query-count assertion in the integration test.
- Check acceptance criterion 2: The renewal window (default 60 days), rating call timeout, error-count threshold and chunk size are supplied via externalized configuration properties with validation, and changing the renewal window in configuration alters candidate selection with no code change or redeploy of the image.
- Check acceptance criterion 3: Chunk size is one item per commit by default; a failure on one policy rolls back only that policy, increments a skip/error counter, records a structured exception with actor, resource and operation, and the job continues processing the remaining population.

### FUNCTIONAL-077 — Golden-Output Parity Harness for Renewal Batch Reconciliation

- User Story: WO-094
- Objective: Validate functional behavior for "Golden-Output Parity Harness for Renewal Batch Reconciliation" against acceptance criteria.
- Expected: Story "Golden-Output Parity Harness for Renewal Batch Reconciliation" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A committed golden dataset and expected-output snapshot exist for the renewal job covering policies inside, on and outside the renewal window, all policy types, rating-failure cases and zero-candidate runs.
- Check acceptance criterion 2: The comparator asserts equality of new term effective/expiration dates, POL_STATUS transitions, PREM_ANNUAL to the cent at NUMERIC(9,2), coverage and deductible carry-forward row counts and values, POLICY_HISTORY_T event types and ordering, and RPT_RUN_LOG_T selected/updated/error counts; any single-field mismatch fails the build with a readable diff.
- Check acceptance criterion 3: The harness runs in CI on every commit against a Testcontainers PostgreSQL instance in under the agreed pipeline budget and publishes a coverage report showing at least 90 percent line coverage on renewal monetary calculation packages.

### FUNCTIONAL-078 — Renewal Exception Reporting, Metrics and Alerting Runbook

- User Story: WO-098
- Objective: Validate functional behavior for "Renewal Exception Reporting, Metrics and Alerting Runbook" against acceptance criteria.
- Expected: Story "Renewal Exception Reporting, Metrics and Alerting Runbook" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Every renewal failure or intentional skip writes a persisted exception record with policy number, reason code, batch job execution id, correlation id, actor identity, resource and operation, and no free-text-only console output is relied upon.
- Check acceptance criterion 2: A read-only endpoint exposes renewal exceptions and run-log summaries with paging and filtering by run date, reason code and policy number, secured deny-by-default and returning RFC 9457 problem details on error.
- Check acceptance criterion 3: Micrometer metrics are published for job duration, items read, written, skipped and errored, exit code, and premium-svc rating call latency and failure rate; a Grafana dashboard definition and Prometheus alert rules are committed as code.

### FUNCTIONAL-079 — Originate Renewal Billing Schedule via Policy Domain Events

- User Story: WO-100
- Objective: Validate functional behavior for "Originate Renewal Billing Schedule via Policy Domain Events" against acceptance criteria.
- Expected: Story "Originate Renewal Billing Schedule via Policy Domain Events" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A PolicyRenewed event carrying policy number, new term effective and expiration dates, annual premium as an exact decimal, billing frequency and a correlation identifier is written to the outbox in the same transaction as the renewal writes, and published by the relay after commit.
- Check acceptance criterion 2: billing-svc consumes the event idempotently: replaying the same event produces exactly one billing plan and one first installment for the new term, verified by a duplicate-delivery integration test.
- Check acceptance criterion 3: After a renewal run with the correction enabled, every renewed policy is selectable by the billing generation candidate query, proven by an integration test that runs renewal then billing generation and asserts a non-zero installment count for each renewed term.

### FUNCTIONAL-080 — Provision PostgreSQL Read Replica With Lag SLO

- User Story: WO-040
- Objective: Validate functional behavior for "Provision PostgreSQL Read Replica With Lag SLO" against acceptance criteria.
- Expected: Story "Provision PostgreSQL Read Replica With Lag SLO" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A Terraform module creates a PostgreSQL 17 streaming read replica per environment with parameters hot_standby_feedback enabled, statement_timeout and idle_in_transaction_session_timeout set for reporting workloads, and terraform plan is clean and idempotent on a second run.
- Check acceptance criterion 2: Replica endpoint and read-only credentials are read from the managed secret store at runtime; no connection string, host or password literal appears in any committed file or Helm values file (verified by the gitleaks scan step in the pipeline).
- Check acceptance criterion 3: A dedicated read-only datasource bean is registered and any INSERT, UPDATE or DELETE attempted through it is rejected, producing a structured log line with actor, resource and operation context and never being retried against the primary.

### FUNCTIONAL-081 — Build reporting-svc Replica-Backed Report APIs

- User Story: WO-059
- Objective: Validate functional behavior for "Build reporting-svc Replica-Backed Report APIs" against acceptance criteria.
- Expected: Story "Build reporting-svc Replica-Backed Report APIs" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Versioned endpoints exist under /v1/reports for regulatory extracts, management metrics, claims loss triangle, renewal and retention, billing aging and batch exceptions, plus /v1/batch-runs projecting RPT_RUN_LOG_T, all documented in a generated OpenAPI 3.1 contract committed to the repository.
- Check acceptance criterion 2: Every reporting endpoint is deny-by-default: an unauthenticated request returns 401, a request without the reporting or compliance authority returns 403, and both outcomes emit a structured authorization-denied event with actor, resource and operation.
- Check acceptance criterion 3: All reporting queries execute against the read replica datasource; an integration test asserts zero connections are opened against the primary datasource during a full sweep of every endpoint.

### FUNCTIONAL-082 — Document Storage And Notification Integration Layer

- User Story: WO-074
- Objective: Validate functional behavior for "Document Storage And Notification Integration Layer" against acceptance criteria.
- Expected: Story "Document Storage And Notification Integration Layer" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A document upload endpoint accepts a file with declared content type and size, rejects types outside a committed allow-list and sizes above the configured maximum with an RFC 9457 problem detail, and stores accepted bytes in object storage with server-side encryption.
- Check acceptance criterion 2: Document metadata and the corresponding outbox event are written inside one database transaction; a forced failure of the object-storage put leaves no metadata row and no outbox event, proven by an integration test.
- Check acceptance criterion 3: Retrieval is exclusively via short-lived presigned URLs; no endpoint streams stored bytes through the service and no permanent public URL is ever generated, verified by test and by absence of any public-read bucket policy in Terraform.

### FUNCTIONAL-083 — Automated Legacy Decommission Readiness Gate

- User Story: WO-075
- Objective: Validate functional behavior for "Automated Legacy Decommission Readiness Gate" against acceptance criteria.
- Expected: Story "Automated Legacy Decommission Readiness Gate" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A committed decommission manifest enumerates every legacy member — the 8 COBOL programs, 22 DDS display files and 2 CL members — with fields for replacement artifact, owning service, parity evidence reference, reconciliation status and decommission state.
- Check acceptance criterion 2: An inventory parser regenerates the legacy member list directly from the repository tree and fails the build if the manifest is missing an entry, contains an entry with no corresponding source member, or leaves any replacement field empty.
- Check acceptance criterion 3: A drain checker queries the batch-run projection and fails when any legacy program name (AUD002B, BIL003B, CMM001B, PRM005B, POL006B, CLM006B) has a run row inside the configured quiet-period window, with the window value externalized as configuration.

### FUNCTIONAL-084 — Execute Legacy IBM i Decommission And Archive

- User Story: WO-101
- Objective: Validate functional behavior for "Execute Legacy IBM i Decommission And Archive" against acceptance criteria.
- Expected: Story "Execute Legacy IBM i Decommission And Archive" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: All scheduler entries submitting legacy batch work (JOBSCHD1 nightly, JOBSCHD2 nightly renewal, JOBSCHD3 monthly billing and commission) are held or removed, and an attempted submission is proven to fail; evidence is captured in the decommission evidence pack.
- Check acceptance criterion 2: Db2 for i is placed in a documented read-only posture for the legacy libraries and INSPRDDTA write authority is revoked from all application user profiles, with before-and-after authority listings attached as evidence.
- Check acceptance criterion 3: A final save of INSPRD, INSPRDDTA, INSCOM and INSTOOLS is written to object storage with Object Lock enabled and a lifecycle policy matching the agreed regulatory retention (minimum six years for insurance policy records and one year minimum for audit), and the archive integrity is verified by checksum.

### FUNCTIONAL-085 — Accessible SPA Shell, Routing and API Client Foundation

- User Story: WO-015
- Objective: Validate functional behavior for "Accessible SPA Shell, Routing and API Client Foundation" against acceptance criteria.
- Expected: Story "Accessible SPA Shell, Routing and API Client Foundation" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A new web/ workspace exists with React 19, TypeScript in strict mode, Vite build, ESLint and Prettier, and npm scripts for dev, build, lint, test and typecheck; build completes with zero TypeScript errors and no use of the any type.
- Check acceptance criterion 2: AppShell renders semantic landmarks (header, nav, main, footer), a working skip-to-main-content link as the first focusable element, an accessible sidebar with module groups (Customer, Policy, Premium, Billing, Claims, Reporting, Admin), TopBar with breadcrumbs and a theme toggle persisted to localStorage.
- Check acceptance criterion 3: Client-side routing is configured with a route registry, lazy-loaded route modules, a 404 route, an error boundary that renders a recoverable error panel, and route changes announce the new page title to assistive technology via a live region.

### FUNCTIONAL-086 — Customer Workspace Replacing CUS Green-Screen Panels

- User Story: WO-061
- Objective: Validate functional behavior for "Customer Workspace Replacing CUS Green-Screen Panels" against acceptance criteria.
- Expected: Story "Customer Workspace Replacing CUS Green-Screen Panels" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A customer search screen replaces CUSLSTD1 with accessible filters (name, tax ID last four, customer id, status), paginated results in the shared DataTable, keyboard row activation opening the customer view, and an accessible no-results state.
- Check acceptance criterion 2: A single customer view replaces CUSINQD1 and aggregates identity, addresses, contacts, policies, billing summary and claim history in tabbed regions, each loading independently with its own skeleton and error state so one failing panel does not blank the page.
- Check acceptance criterion 3: Create and maintain forms replace CUSMNTD1 with all evidenced fields (customer type, name, date of birth, tax ID, gender, marital status, status, credit score, primary address, primary contact) and enforce the evidenced mandatory-field rules with field-anchored plain-language messages instead of coded messages.

### FUNCTIONAL-087 — Billing Invoice, Payment and Delinquency Screens

- User Story: WO-079
- Objective: Validate functional behavior for "Billing Invoice, Payment and Delinquency Screens" against acceptance criteria.
- Expected: Story "Billing Invoice, Payment and Delinquency Screens" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A billing schedule and invoice view replaces BILINVD1 and BILINQD1, listing installments with installment number, due date, amount, status and paid amount, plus per-installment status history, using decimal-safe money display that matches DECIMAL(9,2) values exactly.
- Check acceptance criterion 2: An aging and delinquency worklist lists installments that are due or late with days past due, grace-period remaining, delinquency counter and policy context, filterable and sortable, with accessible empty and large-result states.
- Check acceptance criterion 3: A payment application screen replaces BILPMTD1, allowing a payment to be applied to one or more installments with a running unapplied balance, blocking over-application with a field-anchored message, and handling only gateway tokens and last-four card values — never raw cardholder data.

### FUNCTIONAL-088 — Policy Issuance Screen With Premium Rating Breakdown

- User Story: WO-082
- Objective: Validate functional behavior for "Policy Issuance Screen With Premium Rating Breakdown" against acceptance criteria.
- Expected: Story "Policy Issuance Screen With Premium Rating Breakdown" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A mode-aware policy workspace supports CREATE, ENDORSE and INQUIRY, applying the field-level protected versus input-capable behaviour documented in POL_Module_Design_Document.md section 5.1, with mode clearly announced to assistive technology.
- Check acceptance criterion 2: Customer and agent lookup components validate existence and active status through the API and render inactive or missing entities as blocking field-anchored errors rather than generic messages.
- Check acceptance criterion 3: The coverage editor replaces the POLMNTD1 subfile with an accessible table supporting add, change and remove in ENDORSE mode only, limit and deductible entry with decimal-safe money fields, and mandatory coverages rendered locked with an accessible explanation that they cannot be removed.

### FUNCTIONAL-089 — OIDC Sign-In and Role-Based UI Gating

- User Story: WO-087
- Objective: Validate functional behavior for "OIDC Sign-In and Role-Based UI Gating" against acceptance criteria.
- Expected: Story "OIDC Sign-In and Role-Based UI Gating" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: An unauthenticated user visiting any protected route is redirected to the identity provider using Authorization Code with PKCE, and after successful sign-in is returned to the originally requested route.
- Check acceptance criterion 2: The session is carried in an httpOnly, Secure, SameSite=Strict cookie plus a bearer token attached by the API client; no access or refresh token is ever written to localStorage or sessionStorage, verified by an automated test asserting storage is empty after sign-in.
- Check acceptance criterion 3: Access token expiry triggers a silent refresh; a failed refresh clears client state, redirects to sign-in and preserves the intended route. Concurrent 401 responses trigger exactly one refresh attempt (single-flight), not one per request.

### FUNCTIONAL-090 — WCAG 2.1 AA Conformance Test Harness and CI Gate

- User Story: WO-095
- Objective: Validate functional behavior for "WCAG 2.1 AA Conformance Test Harness and CI Gate" against acceptance criteria.
- Expected: Story "WCAG 2.1 AA Conformance Test Harness and CI Gate" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Automated axe-core accessibility assertions run against every registered SPA route and every component gallery state, with zero serious or critical violations; the run fails the build on any new violation and publishes a machine-readable JSON report artifact per build.
- Check acceptance criterion 2: Keyboard-only traversal tests verify for each primary workflow (customer create, policy issuance, claim FNOL, payment request, approval decision, payment application) that every interactive control is reachable by Tab, that no focus trap exists outside modals, that Escape closes dialogs and returns focus to the invoker, and that a visible focus indicator is present on every focusable element.
- Check acceptance criterion 3: A contrast audit covers both design tokens and rendered pages, asserting 4.5:1 for normal text, 3:1 for large text and non-text UI components, and fails with the offending selector and computed ratio.

### FUNCTIONAL-091 — Claims Workspace With Approval and Payment Controls

- User Story: WO-096
- Objective: Validate functional behavior for "Claims Workspace With Approval and Payment Controls" against acceptance criteria.
- Expected: Story "Claims Workspace With Approval and Payment Controls" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: An FNOL intake screen replaces CLMFNLD1 with policy lookup and coverage validation, loss date and description, late-reporting indication, and multi-file document and photo attachment with progress, retry, type and size validation and accessible status announcements.
- Check acceptance criterion 2: A claim workspace replaces CLMUPDD1 and CLMINQD1 by combining claim header, append-only reserve history, payment list, notes and documents in one keyboard-navigable view where reserve history rows are visibly non-editable.
- Check acceptance criterion 3: The payment request screen shows, before submission, the remaining reserve (approved amount minus paid to date) and the requesting adjuster's remaining authority headroom computed on cumulative claim payout, so an over-limit request is visible in advance rather than after submission.

### FUNCTIONAL-092 — Accessible Component Library Replacing DDS Subfiles

- User Story: WO-099
- Objective: Validate functional behavior for "Accessible Component Library Replacing DDS Subfiles" against acceptance criteria.
- Expected: Story "Accessible Component Library Replacing DDS Subfiles" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: DataTable renders native table semantics with caption, column headers with scope, aria-sort on sortable columns, sticky header, dense mode, row-level action menu reachable by keyboard, full keyboard row navigation, and an accessible empty state and loading state.
- Check acceptance criterion 2: Form controls (TextField, Select, Checkbox, Radio, Toggle, DateField, MoneyField) each associate a visible label, optional description and error message via aria-describedby and aria-invalid; validation errors render adjacent to the offending field and are announced once via a live region, replacing the DDS bottom message line pattern.
- Check acceptance criterion 3: MoneyField and money display components format and parse decimal values without floating-point loss, preserving two-decimal scale so amounts round-trip exactly to the DECIMAL(9,2) and DECIMAL(11,2) backend types.

### FUNCTIONAL-093 — Deploy API Gateway With TLS, Routing and Rate Limiting

- User Story: WO-016
- Objective: Validate functional behavior for "Deploy API Gateway With TLS, Routing and Rate Limiting" against acceptance criteria.
- Expected: Story "Deploy API Gateway With TLS, Routing and Rate Limiting" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A gateway module (Spring Boot 3.5.x, Java 21, Spring Cloud Gateway) builds from the repository root Maven build and starts with an externalized route configuration listing /v1/customers, /v1/policies, /v1/claims, /v1/premium, /v1/billing, /v1/reports, /v1/authz and /v1/audit routes.
- Check acceptance criterion 2: TLS 1.3 is enforced at the edge; a connection attempt negotiating TLS 1.1 or 1.2 is rejected, and responses carry Strict-Transport-Security, Content-Security-Policy, X-Content-Type-Options nosniff, X-Frame-Options DENY and Referrer-Policy headers verified by an automated header assertion test.
- Check acceptance criterion 3: Rate limiting is enforced at 100 requests per minute per principal key (falling back to client IP when no principal is present) using a Redis-backed token bucket; the 101st request in a window returns HTTP 429 with an RFC 9457 problem detail containing a retry-after hint and no stack trace.

### FUNCTIONAL-094 — Enforce Zone Segmentation, mTLS and Egress Allow-Lists

- User Story: WO-034
- Objective: Validate functional behavior for "Enforce Zone Segmentation, mTLS and Egress Allow-Lists" against acceptance criteria.
- Expected: Story "Enforce Zone Segmentation, mTLS and Egress Allow-Lists" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A default-deny NetworkPolicy exists for every namespace covering both ingress and egress, and a committed policy test proves that a pod with no explicit allow rule cannot reach any other pod, the database, or the internet.
- Check acceptance criterion 2: Per-service NetworkPolicies allow only the documented conduits: gateway to the eight services, services to their own database and to the message broker, batch jobs to database and broker, reporting to the read replica only and never to the OLTP primary — each conduit asserted by an automated test.
- Check acceptance criterion 3: Mutual TLS is enforced for all internal pod-to-pod traffic (strict mode), verified by a test showing a plaintext connection attempt between two pods is refused and by inspecting the negotiated peer identity.

### FUNCTIONAL-095 — Implement OIDC Federation and JWT Validation Enforcement

- User Story: WO-050
- Objective: Validate functional behavior for "Implement OIDC Federation and JWT Validation Enforcement" against acceptance criteria.
- Expected: Story "Implement OIDC Federation and JWT Validation Enforcement" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Gateway is configured as an OAuth2 client performing Authorization Code with PKCE against the configured OIDC provider; login redirects, callback handling and RP-initiated logout all work against a containerised test identity provider.
- Check acceptance criterion 2: Successful login sets an httpOnly, Secure, SameSite=Strict session cookie; an automated test asserts no access or refresh token value ever appears in a response body, in a Set-Cookie without httpOnly, or in any log line.
- Check acceptance criterion 3: Access tokens are validated locally with RS256 against a JWKS cache with a 1 hour TTL and background refresh; validation adds no more than 5 ms at p99 and performs no network hop to the identity provider on the happy path, proven by a benchmark test and by asserting zero JWKS requests during a steady-state load run.

### FUNCTIONAL-096 — Federate Workload Identity For Batch and Service Calls

- User Story: WO-052
- Objective: Validate functional behavior for "Federate Workload Identity For Batch and Service Calls" against acceptance criteria.
- Expected: Story "Federate Workload Identity For Batch and Service Calls" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Each batch job and each domain service is registered as a distinct OAuth2 client-credentials principal with a least-privilege scope set, and a committed manifest maps workload name to client identifier, scopes and the operations it may perform.
- Check acceptance criterion 2: A MachineTokenProvider in the shared security starter obtains client-credentials tokens by exchanging the projected Kubernetes service account token (workload identity federation) so no long-lived client secret is stored in a container image or manifest; any required secret is referenced as a placeholder resolved from the managed secret store.
- Check acceptance criterion 3: Tokens are cached in memory and refreshed proactively before expiry with jittered retry; a test asserts that N sequential outbound calls within a token lifetime trigger exactly one token request, and that refresh failure causes the caller to fail closed rather than proceed unauthenticated.

### FUNCTIONAL-097 — Machine-readable open design item decision register with CI gate

- User Story: WO-008
- Objective: Validate functional behavior for "Machine-readable open design item decision register with CI gate" against acceptance criteria.
- Expected: Story "Machine-readable open design item decision register with CI gate" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: governance/open-design-items.yaml exists and contains exactly the twelve items enumerated in PCIS_Enterprise_Architecture.md section 7.4, each with id, title, evidence_file, evidence_section, owning_phase, decision_owner_role, status, decision_text and implemented_by fields.
- Check acceptance criterion 2: A JSON Schema (governance/schema/open-design-items.schema.json) validates the register; the validator exits non-zero with a field-level message when a required field is missing, an unknown status value is used, or an evidence_file path does not exist in the repository.
- Check acceptance criterion 3: Running the validator with a phase argument (for example --phase=CLAIMS) exits non-zero and lists offending item ids when any item whose owning_phase equals that phase has status OPEN; it exits zero when all such items are DECIDED or CONFIGURATION_DRIVEN.

### FUNCTIONAL-098 — Parallel-run reconciliation engine with per-domain cutover gate scoring

- User Story: WO-022
- Objective: Validate functional behavior for "Parallel-run reconciliation engine with per-domain cutover gate scoring" against acceptance criteria.
- Expected: Story "Parallel-run reconciliation engine with per-domain cutover gate scoring" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A reconciliation job can be launched per domain (customer, policy, billing, premium, claims, reporting) with a business-date parameter, and completes with a persisted RECON_RUN row recording start, end, entity count, compared row count and break counts by classification.
- Check acceptance criterion 2: All monetary comparisons use BigDecimal with scale 2 and exact equality (no epsilon tolerance); a one-cent difference on any of PREM_ANNUAL, DUE_AMT, PAID_AMT, COMMISSION_AMT, APPROVED_AMT, PAID_TO_DATE or payment amount produces a VALUE_MISMATCH break naming entity, business key, column, legacy value and target value.
- Check acceptance criterion 3: Break records are persisted in RECON_BREAK with classification, entity, business key, column, legacy value, target value, first-seen and last-seen timestamps, and an optional approved_decision_id linking to a WO-170 register item so approved behaviour changes are excluded from the unexplained-break count.

### FUNCTIONAL-099 — Per-domain cutover control plane with audited rollback switches

- User Story: WO-023
- Objective: Validate functional behavior for "Per-domain cutover control plane with audited rollback switches" against acceptance criteria.
- Expected: Story "Per-domain cutover control plane with audited rollback switches" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A cutover state store holds one row per domain with state, previous_state, changed_by, changed_at, reason and linked gate verdict snapshot; states are constrained to LEGACY_ONLY, SHADOW_WRITE, PARALLEL_RUN, TARGET_PRIMARY, TARGET_ONLY.
- Check acceptance criterion 2: State changes are performed only through an authenticated, deny-by-default endpoint requiring a cutover-operator permission; every change writes an immutable audit event with actor, resource (domain), operation, old and new state and reason, and the audit write is in the same transaction as the state change.
- Check acceptance criterion 3: Promotion to TARGET_PRIMARY is rejected with a distinct reason code when the WO-171 gate verdict for that domain is FAIL or the minimum parallel-run window has not elapsed; the rejection is audited.

### FUNCTIONAL-100 — Automated phase-gate evidence pack generation and governance dashboard API

- User Story: WO-041
- Objective: Validate functional behavior for "Automated phase-gate evidence pack generation and governance dashboard API" against acceptance criteria.
- Expected: Story "Automated phase-gate evidence pack generation and governance dashboard API" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A gate criteria definition file declares every mandatory and advisory criterion with id, description, source type, threshold, comparison operator and applicable phases, covering at minimum monetary-logic line coverage at or above ninety percent, zero unexplained reconciliation breaks over the configured window, one hundred percent of claim payment paths passing an authority check, fifty-five of fifty-five tables classified, purge completion within twenty-four hours, six of six tunables externalized, commit blast radius at or below one thousand rows for the archive job, and batch restart with zero duplicate or orphaned financial records.
- Check acceptance criterion 2: The generator collects each criterion value from its declared source (JaCoCo XML, reconciliation gate endpoint or exported JSON, decision register report, purge run-log export, authorization test result file, fault-injection test result file) and records the source artifact path or URL plus a content hash for traceability.
- Check acceptance criterion 3: The generator writes a JSON manifest and a markdown evidence pack under target/governance/gate-pack/{phase}/ containing every criterion with measured value, threshold, verdict and source reference, plus the overall phase verdict and generation timestamp.

### FUNCTIONAL-101 — Legacy behaviour decision records with preserve-versus-change parity matrix

- User Story: WO-073
- Objective: Validate functional behavior for "Legacy behaviour decision records with preserve-versus-change parity matrix" against acceptance criteria.
- Expected: Story "Legacy behaviour decision records with preserve-versus-change parity matrix" satisfies expected functional validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: governance/behaviour-decisions.yaml catalogues at minimum the following evidenced behaviours, each with a unique id: BIL003B silent skip of candidates outside the lead window while counting them eligible; the audit-write-failure continue path in BIL003B, CMM001B, PRM005B, POL006B and CLM006B; CLM006B full-outstanding payment computation; CLM006B informational-only reinsurance cession flag; CLM006B absence of any authority check; BIL003B reuse of HV-INSTALLMENT-NBR as a days-out counter; CMM001B no-in-force-plan counter path; CMM001B COMM_CALC_FLAG idempotency guard; PRM005B grace-period status transitions; AUD002B halt-on-verification-mismatch and archive-verify-then-delete ordering; per-item commit granularity stated in each program prologue.
- Check acceptance criterion 2: Each entry carries evidence_program, evidence_paragraph, evidence_excerpt, decision (PRESERVE or CHANGE), rationale, decision_owner_role, approved_by, approved_on, linked_open_design_item (optional) and test_ref pointing at a test class and method.
- Check acceptance criterion 3: A validator command fails the build when any entry lacks evidence_program or test_ref, when a CHANGE entry lacks approved_by and approved_on, when evidence_program does not name a file present in the repository, or when the referenced test class or method cannot be located in the source tree.

---

## Smoke test suite (101)

### SMOKE-001 — Repository Member Manifest and Completeness Gate

- User Story: WO-001
- Objective: Run critical-path smoke verification for "Repository Member Manifest and Completeness Gate" after deployment.
- Expected: Story "Repository Member Manifest and Completeness Gate" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-001.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-002 — Extract Legacy Behavioural Baseline Specification Artifact

- User Story: WO-002
- Objective: Run critical-path smoke verification for "Extract Legacy Behavioural Baseline Specification Artifact" after deployment.
- Expected: Story "Extract Legacy Behavioural Baseline Specification Artifact" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-002.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-003 — Scripted Dependency-Ordered Legacy COBOL Build

- User Story: WO-009
- Objective: Run critical-path smoke verification for "Scripted Dependency-Ordered Legacy COBOL Build" after deployment.
- Expected: Story "Scripted Dependency-Ordered Legacy COBOL Build" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-009.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-004 — CI Pipeline Gating Manifest, Baseline and Build

- User Story: WO-024
- Objective: Run critical-path smoke verification for "CI Pipeline Gating Manifest, Baseline and Build" after deployment.
- Expected: Story "CI Pipeline Gating Manifest, Baseline and Build" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-024.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-005 — Coexistence Topology, Scheduler Map and Runbook

- User Story: WO-025
- Objective: Run critical-path smoke verification for "Coexistence Topology, Scheduler Map and Runbook" after deployment.
- Expected: Story "Coexistence Topology, Scheduler Map and Runbook" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-025.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-006 — Bootstrap Maven multi-module Java 21 platform skeleton

- User Story: WO-003
- Objective: Run critical-path smoke verification for "Bootstrap Maven multi-module Java 21 platform skeleton" after deployment.
- Expected: Story "Bootstrap Maven multi-module Java 21 platform skeleton" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-003.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-007 — Provision Terraform infrastructure for three PCIS environments

- User Story: WO-004
- Objective: Run critical-path smoke verification for "Provision Terraform infrastructure for three PCIS environments" after deployment.
- Expected: Story "Provision Terraform infrastructure for three PCIS environments" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-004.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-008 — Create reproducible distroless non-root service images

- User Story: WO-012
- Objective: Run critical-path smoke verification for "Create reproducible distroless non-root service images" after deployment.
- Expected: Story "Create reproducible distroless non-root service images" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-012.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-009 — Implement Forge Shipping pipeline with security gates

- User Story: WO-029
- Objective: Run critical-path smoke verification for "Implement Forge Shipping pipeline with security gates" after deployment.
- Expected: Story "Implement Forge Shipping pipeline with security gates" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-029.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-010 — Establish GitOps delivery with fifteen-minute rollback

- User Story: WO-046
- Objective: Run critical-path smoke verification for "Establish GitOps delivery with fifteen-minute rollback" after deployment.
- Expected: Story "Establish GitOps delivery with fifteen-minute rollback" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-046.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-011 — Define batch CronJob manifests and exit-code contract

- User Story: WO-063
- Objective: Run critical-path smoke verification for "Define batch CronJob manifests and exit-code contract" after deployment.
- Expected: Story "Define batch CronJob manifests and exit-code contract" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-063.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-012 — Build shared observability starter with PII-masking structured logging

- User Story: WO-013
- Objective: Run critical-path smoke verification for "Build shared observability starter with PII-masking structured logging" after deployment.
- Expected: Story "Build shared observability starter with PII-masking structured logging" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-013.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-013 — Implement structured error library with reason-code registry

- User Story: WO-031
- Objective: Run critical-path smoke verification for "Implement structured error library with reason-code registry" after deployment.
- Expected: Story "Implement structured error library with reason-code registry" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-031.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-014 — Publish baseline metrics, SLO dashboards and alert rules

- User Story: WO-064
- Objective: Run critical-path smoke verification for "Publish baseline metrics, SLO dashboards and alert rules" after deployment.
- Expected: Story "Publish baseline metrics, SLO dashboards and alert rules" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-064.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-015 — Author operational runbooks for batch, rollback, purge and incidents

- User Story: WO-080
- Objective: Run critical-path smoke verification for "Author operational runbooks for batch, rollback, purge and incidents" after deployment.
- Expected: Story "Author operational runbooks for batch, rollback, purge and incidents" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-080.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-016 — Build audit-svc core with versioned v1 audit event contract

- User Story: WO-010
- Objective: Run critical-path smoke verification for "Build audit-svc core with versioned v1 audit event contract" after deployment.
- Expected: Story "Build audit-svc core with versioned v1 audit event contract" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-010.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-017 — Make audit writes atomic with mutations via transactional outbox

- User Story: WO-026
- Objective: Run critical-path smoke verification for "Make audit writes atomic with mutations via transactional outbox" after deployment.
- Expected: Story "Make audit writes atomic with mutations via transactional outbox" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-026.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-018 — Convert AUD002B archiving into restartable retention and purge job

- User Story: WO-042
- Objective: Run critical-path smoke verification for "Convert AUD002B archiving into restartable retention and purge job" after deployment.
- Expected: Story "Convert AUD002B archiving into restartable retention and purge job" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-042.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-019 — Mask PII and classify data before audit persistence

- User Story: WO-051
- Objective: Run critical-path smoke verification for "Mask PII and classify data before audit persistence" after deployment.
- Expected: Story "Mask PII and classify data before audit persistence" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-051.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-020 — Expose audit inquiry API with gated unmask and observability

- User Story: WO-067
- Objective: Run critical-path smoke verification for "Expose audit inquiry API with gated unmask and observability" after deployment.
- Expected: Story "Expose audit inquiry API with gated unmask and observability" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-067.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-021 — Build authz-svc policy decision service with deny-by-default

- User Story: WO-011
- Objective: Run critical-path smoke verification for "Build authz-svc policy decision service with deny-by-default" after deployment.
- Expected: Story "Build authz-svc policy decision service with deny-by-default" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-011.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-022 — Enforce approval linkage and cumulative claim authority limits

- User Story: WO-030
- Objective: Run critical-path smoke verification for "Enforce approval linkage and cumulative claim authority limits" after deployment.
- Expected: Story "Enforce approval linkage and cumulative claim authority limits" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-030.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-023 — Apply deny-by-default guards to financial mutation endpoints

- User Story: WO-048
- Objective: Run critical-path smoke verification for "Apply deny-by-default guards to financial mutation endpoints" after deployment.
- Expected: Story "Apply deny-by-default guards to financial mutation endpoints" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-048.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-024 — Replace batch actor literals with authenticated service principals

- User Story: WO-049
- Objective: Run critical-path smoke verification for "Replace batch actor literals with authenticated service principals" after deployment.
- Expected: Story "Replace batch actor literals with authenticated service principals" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-049.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-025 — Automate authorization regression and segregation-of-duties control evidence

- User Story: WO-065
- Objective: Run critical-path smoke verification for "Automate authorization regression and segregation-of-duties control evidence" after deployment.
- Expected: Story "Automate authorization regression and segregation-of-duties control evidence" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-065.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-026 — Machine-readable data classification registry for all PCIS entities

- User Story: WO-005
- Objective: Run critical-path smoke verification for "Machine-readable data classification registry for all PCIS entities" after deployment.
- Expected: Story "Machine-readable data classification registry for all PCIS entities" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-005.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-027 — Shared PII masking library with Jackson and Logback integration

- User Story: WO-017
- Objective: Run critical-path smoke verification for "Shared PII masking library with Jackson and Logback integration" after deployment.
- Expected: Story "Shared PII masking library with Jackson and Logback integration" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-017.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-028 — Permission-gated self-audited PII unmask action for investigators

- User Story: WO-035
- Objective: Run critical-path smoke verification for "Permission-gated self-audited PII unmask action for investigators" after deployment.
- Expected: Story "Permission-gated self-audited PII unmask action for investigators" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-035.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-029 — Tiered retention with partitioned audit table and restartable job

- User Story: WO-038
- Objective: Run critical-path smoke verification for "Tiered retention with partitioned audit table and restartable job" after deployment.
- Expected: Story "Tiered retention with partitioned audit table and restartable job" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-038.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-030 — Mask PII at audit event creation before outbox persistence

- User Story: WO-043
- Objective: Run critical-path smoke verification for "Mask PII at audit event creation before outbox persistence" after deployment.
- Expected: Story "Mask PII at audit event creation before outbox persistence" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-043.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-031 — CI gates for unclassified entities and unmasked PII leakage

- User Story: WO-047
- Objective: Run critical-path smoke verification for "CI gates for unclassified entities and unmasked PII leakage" after deployment.
- Expected: Story "CI gates for unclassified entities and unmasked PII leakage" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-047.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-032 — Automated purge with cryptographic erasure and immutable evidence

- User Story: WO-057
- Objective: Run critical-path smoke verification for "Automated purge with cryptographic erasure and immutable evidence" after deployment.
- Expected: Story "Automated purge with cryptographic erasure and immutable evidence" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-057.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-033 — Create versioned tunables and rules configuration schema

- User Story: WO-006
- Objective: Run critical-path smoke verification for "Create versioned tunables and rules configuration schema" after deployment.
- Expected: Story "Create versioned tunables and rules configuration schema" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-006.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-034 — Typed tunable resolution service with cache and fail-fast validation

- User Story: WO-018
- Objective: Run critical-path smoke verification for "Typed tunable resolution service with cache and fail-fast validation" after deployment.
- Expected: Story "Typed tunable resolution service with cache and fail-fast validation" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-018.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-035 — Admin tunables REST API with RBAC and change evidence

- User Story: WO-036
- Objective: Run critical-path smoke verification for "Admin tunables REST API with RBAC and change evidence" after deployment.
- Expected: Story "Admin tunables REST API with RBAC and change evidence" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-036.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-036 — Admin tunables web panel with versioned change history

- User Story: WO-037
- Objective: Run critical-path smoke verification for "Admin tunables web panel with versioned change history" after deployment.
- Expected: Story "Admin tunables web panel with versioned change history" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-037.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-037 — Externalized code-table and business rules store service

- User Story: WO-055
- Objective: Run critical-path smoke verification for "Externalized code-table and business rules store service" after deployment.
- Expected: Story "Externalized code-table and business rules store service" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-055.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-038 — Author Flyway Baseline PostgreSQL Schema Migrations

- User Story: WO-007
- Objective: Run critical-path smoke verification for "Author Flyway Baseline PostgreSQL Schema Migrations" after deployment.
- Expected: Story "Author Flyway Baseline PostgreSQL Schema Migrations" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-007.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-039 — Implement Sequence Objects, Block Allocator and Key Formatter

- User Story: WO-019
- Objective: Run critical-path smoke verification for "Implement Sequence Objects, Block Allocator and Key Formatter" after deployment.
- Expected: Story "Implement Sequence Objects, Block Allocator and Key Formatter" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-019.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-040 — Resolve Schema Discrepancies and Publish Corrected Data Dictionary

- User Story: WO-020
- Objective: Run critical-path smoke verification for "Resolve Schema Discrepancies and Publish Corrected Data Dictionary" after deployment.
- Expected: Story "Resolve Schema Discrepancies and Publish Corrected Data Dictionary" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-020.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-041 — Automate Masked Anonymized Non-Production Data Refresh

- User Story: WO-021
- Objective: Run critical-path smoke verification for "Automate Masked Anonymized Non-Production Data Refresh" after deployment.
- Expected: Story "Automate Masked Anonymized Non-Production Data Refresh" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-021.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-042 — Build Polling Extraction and Idempotent PostgreSQL Loader

- User Story: WO-039
- Objective: Run critical-path smoke verification for "Build Polling Extraction and Idempotent PostgreSQL Loader" after deployment.
- Expected: Story "Build Polling Extraction and Idempotent PostgreSQL Loader" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-039.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-043 — Build Nightly Cent-Level Parallel-Run Reconciliation Harness

- User Story: WO-058
- Objective: Run critical-path smoke verification for "Build Nightly Cent-Level Parallel-Run Reconciliation Harness" after deployment.
- Expected: Story "Build Nightly Cent-Level Parallel-Run Reconciliation Harness" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-058.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-044 — Deterministic Seed Data Harness for Batch Regression Fixtures

- User Story: WO-014
- Objective: Run critical-path smoke verification for "Deterministic Seed Data Harness for Batch Regression Fixtures" after deployment.
- Expected: Story "Deterministic Seed Data Harness for Batch Regression Fixtures" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-014.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-045 — Capture COBOL Baseline Golden Outputs with Determinism Controls

- User Story: WO-032
- Objective: Run critical-path smoke verification for "Capture COBOL Baseline Golden Outputs with Determinism Controls" after deployment.
- Expected: Story "Capture COBOL Baseline Golden Outputs with Determinism Controls" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-032.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-046 — Cent-Level Golden Output Comparison Engine and Diff Reporting

- User Story: WO-033
- Objective: Run critical-path smoke verification for "Cent-Level Golden Output Comparison Engine and Diff Reporting" after deployment.
- Expected: Story "Cent-Level Golden Output Comparison Engine and Diff Reporting" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-033.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-047 — Golden-Output Regression Suites for Six Batch Programs

- User Story: WO-053
- Objective: Run critical-path smoke verification for "Golden-Output Regression Suites for Six Batch Programs" after deployment.
- Expected: Story "Golden-Output Regression Suites for Six Batch Programs" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-053.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-048 — Batch Fault Injection Proving Restart Without Duplicate Writes

- User Story: WO-054
- Objective: Run critical-path smoke verification for "Batch Fault Injection Proving Restart Without Duplicate Writes" after deployment.
- Expected: Story "Batch Fault Injection Proving Restart Without Duplicate Writes" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-054.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-049 — CI Gate Wiring Coverage and Regression Evidence Artifacts

- User Story: WO-078
- Objective: Run critical-path smoke verification for "CI Gate Wiring Coverage and Regression Evidence Artifacts" after deployment.
- Expected: Story "CI Gate Wiring Coverage and Regression Evidence Artifacts" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-078.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-050 — Create PostgreSQL customer schema with Flyway migrations

- User Story: WO-068
- Objective: Run critical-path smoke verification for "Create PostgreSQL customer schema with Flyway migrations" after deployment.
- Expected: Story "Create PostgreSQL customer schema with Flyway migrations" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-068.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-051 — Build customer-svc domain and persistence layers

- User Story: WO-083
- Objective: Run critical-path smoke verification for "Build customer-svc domain and persistence layers" after deployment.
- Expected: Story "Build customer-svc domain and persistence layers" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-083.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-052 — Expose versioned customer REST API with deny-by-default authorization

- User Story: WO-084
- Objective: Run critical-path smoke verification for "Expose versioned customer REST API with deny-by-default authorization" after deployment.
- Expected: Story "Expose versioned customer REST API with deny-by-default authorization" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-084.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-053 — Publish masked customer read projection for downstream consumers

- User Story: WO-085
- Objective: Run critical-path smoke verification for "Publish masked customer read projection for downstream consumers" after deployment.
- Expected: Story "Publish masked customer read projection for downstream consumers" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-085.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-054 — Establish customer slice parity harness and CI quality gates

- User Story: WO-091
- Objective: Run critical-path smoke verification for "Establish customer slice parity harness and CI quality gates" after deployment.
- Expected: Story "Establish customer slice parity harness and CI quality gates" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-091.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-055 — Provision Claims PostgreSQL Schema With Exact Decimal Precision

- User Story: WO-027
- Objective: Run critical-path smoke verification for "Provision Claims PostgreSQL Schema With Exact Decimal Precision" after deployment.
- Expected: Story "Provision Claims PostgreSQL Schema With Exact Decimal Precision" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-027.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-056 — Model Claim Approval Lifecycle As First-Class Record

- User Story: WO-044
- Objective: Run critical-path smoke verification for "Model Claim Approval Lifecycle As First-Class Record" after deployment.
- Expected: Story "Model Claim Approval Lifecycle As First-Class Record" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-044.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-057 — Enforce Dual Payment Authority Check In Claims Domain

- User Story: WO-060
- Objective: Run critical-path smoke verification for "Enforce Dual Payment Authority Check In Claims Domain" after deployment.
- Expected: Story "Enforce Dual Payment Authority Check In Claims Domain" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-060.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-058 — Expose Versioned Claims REST API With Deny-By-Default

- User Story: WO-066
- Objective: Run critical-path smoke verification for "Expose Versioned Claims REST API With Deny-By-Default" after deployment.
- Expected: Story "Expose Versioned Claims REST API With Deny-By-Default" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-066.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-059 — Instrument Claims Payment Observability And SoD Evidence

- User Story: WO-081
- Objective: Run critical-path smoke verification for "Instrument Claims Payment Observability And SoD Evidence" after deployment.
- Expected: Story "Instrument Claims Payment Observability And SoD Evidence" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-081.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-060 — Convert CLM006B Claim Payment Batch To Spring Batch

- User Story: WO-090
- Objective: Run critical-path smoke verification for "Convert CLM006B Claim Payment Batch To Spring Batch" after deployment.
- Expected: Story "Convert CLM006B Claim Payment Batch To Spring Batch" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-090.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-061 — Build Claim Payment Golden-Output Parity Harness

- User Story: WO-097
- Objective: Run critical-path smoke verification for "Build Claim Payment Golden-Output Parity Harness" after deployment.
- Expected: Story "Build Claim Payment Golden-Output Parity Harness" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-097.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-062 — Migrate billing and commission schema with set-based readers

- User Story: WO-056
- Objective: Run critical-path smoke verification for "Migrate billing and commission schema with set-based readers" after deployment.
- Expected: Story "Migrate billing and commission schema with set-based readers" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-056.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-063 — Convert BIL003B billing generation to Spring Batch job

- User Story: WO-070
- Objective: Run critical-path smoke verification for "Convert BIL003B billing generation to Spring Batch job" after deployment.
- Expected: Story "Convert BIL003B billing generation to Spring Batch job" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-070.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-064 — Convert PRM005B delinquency aging to Spring Batch job

- User Story: WO-071
- Objective: Run critical-path smoke verification for "Convert PRM005B delinquency aging to Spring Batch job" after deployment.
- Expected: Story "Convert PRM005B delinquency aging to Spring Batch job" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-071.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-065 — Convert CMM001B commission calculation to Spring Batch job

- User Story: WO-072
- Objective: Run critical-path smoke verification for "Convert CMM001B commission calculation to Spring Batch job" after deployment.
- Expected: Story "Convert CMM001B commission calculation to Spring Batch job" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-072.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-066 — Gate billing batch cutover with parallel-run reconciliation

- User Story: WO-088
- Objective: Run critical-path smoke verification for "Gate billing batch cutover with parallel-run reconciliation" after deployment.
- Expected: Story "Gate billing batch cutover with parallel-run reconciliation" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-088.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-067 — Expose billing-svc REST API and batch operations endpoints

- User Story: WO-089
- Objective: Run critical-path smoke verification for "Expose billing-svc REST API and batch operations endpoints" after deployment.
- Expected: Story "Expose billing-svc REST API and batch operations endpoints" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-089.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-068 — Freeze versioned v1 premium rating OpenAPI contract

- User Story: WO-028
- Objective: Run critical-path smoke verification for "Freeze versioned v1 premium rating OpenAPI contract" after deployment.
- Expected: Story "Freeze versioned v1 premium rating OpenAPI contract" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-028.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-069 — Scaffold premium-svc service and rating data access layer

- User Story: WO-045
- Objective: Run critical-path smoke verification for "Scaffold premium-svc service and rating data access layer" after deployment.
- Expected: Story "Scaffold premium-svc service and rating data access layer" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-045.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-070 — Implement rating engine with exact decimal arithmetic

- User Story: WO-062
- Objective: Run critical-path smoke verification for "Implement rating engine with exact decimal arithmetic" after deployment.
- Expected: Story "Implement rating engine with exact decimal arithmetic" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-062.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-071 — Build golden-output parity harness for premium rating

- User Story: WO-069
- Objective: Run critical-path smoke verification for "Build golden-output parity harness for premium rating" after deployment.
- Expected: Story "Build golden-output parity harness for premium rating" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-069.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-072 — Add underwriting rule evaluation and referral outcome tracking

- User Story: WO-076
- Objective: Run critical-path smoke verification for "Add underwriting rule evaluation and referral outcome tracking" after deployment.
- Expected: Story "Add underwriting rule evaluation and referral outcome tracking" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-076.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-073 — Enforce rating contract with consumer-driven tests and CI gate

- User Story: WO-077
- Objective: Run critical-path smoke verification for "Enforce rating contract with consumer-driven tests and CI gate" after deployment.
- Expected: Story "Enforce rating contract with consumer-driven tests and CI gate" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-077.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-074 — Migrate Policy Domain Schema to PostgreSQL with Flyway

- User Story: WO-086
- Objective: Run critical-path smoke verification for "Migrate Policy Domain Schema to PostgreSQL with Flyway" after deployment.
- Expected: Story "Migrate Policy Domain Schema to PostgreSQL with Flyway" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-086.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-075 — Build policy-svc REST API with Versioned Read Contract

- User Story: WO-092
- Objective: Run critical-path smoke verification for "Build policy-svc REST API with Versioned Read Contract" after deployment.
- Expected: Story "Build policy-svc REST API with Versioned Read Contract" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-092.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-076 — Convert POL006B Renewal Batch to Restartable Spring Batch Job

- User Story: WO-093
- Objective: Run critical-path smoke verification for "Convert POL006B Renewal Batch to Restartable Spring Batch Job" after deployment.
- Expected: Story "Convert POL006B Renewal Batch to Restartable Spring Batch Job" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-093.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-077 — Golden-Output Parity Harness for Renewal Batch Reconciliation

- User Story: WO-094
- Objective: Run critical-path smoke verification for "Golden-Output Parity Harness for Renewal Batch Reconciliation" after deployment.
- Expected: Story "Golden-Output Parity Harness for Renewal Batch Reconciliation" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-094.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-078 — Renewal Exception Reporting, Metrics and Alerting Runbook

- User Story: WO-098
- Objective: Run critical-path smoke verification for "Renewal Exception Reporting, Metrics and Alerting Runbook" after deployment.
- Expected: Story "Renewal Exception Reporting, Metrics and Alerting Runbook" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-098.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-079 — Originate Renewal Billing Schedule via Policy Domain Events

- User Story: WO-100
- Objective: Run critical-path smoke verification for "Originate Renewal Billing Schedule via Policy Domain Events" after deployment.
- Expected: Story "Originate Renewal Billing Schedule via Policy Domain Events" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-100.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-080 — Provision PostgreSQL Read Replica With Lag SLO

- User Story: WO-040
- Objective: Run critical-path smoke verification for "Provision PostgreSQL Read Replica With Lag SLO" after deployment.
- Expected: Story "Provision PostgreSQL Read Replica With Lag SLO" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-040.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-081 — Build reporting-svc Replica-Backed Report APIs

- User Story: WO-059
- Objective: Run critical-path smoke verification for "Build reporting-svc Replica-Backed Report APIs" after deployment.
- Expected: Story "Build reporting-svc Replica-Backed Report APIs" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-059.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-082 — Document Storage And Notification Integration Layer

- User Story: WO-074
- Objective: Run critical-path smoke verification for "Document Storage And Notification Integration Layer" after deployment.
- Expected: Story "Document Storage And Notification Integration Layer" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-074.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-083 — Automated Legacy Decommission Readiness Gate

- User Story: WO-075
- Objective: Run critical-path smoke verification for "Automated Legacy Decommission Readiness Gate" after deployment.
- Expected: Story "Automated Legacy Decommission Readiness Gate" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-075.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-084 — Execute Legacy IBM i Decommission And Archive

- User Story: WO-101
- Objective: Run critical-path smoke verification for "Execute Legacy IBM i Decommission And Archive" after deployment.
- Expected: Story "Execute Legacy IBM i Decommission And Archive" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-101.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-085 — Accessible SPA Shell, Routing and API Client Foundation

- User Story: WO-015
- Objective: Run critical-path smoke verification for "Accessible SPA Shell, Routing and API Client Foundation" after deployment.
- Expected: Story "Accessible SPA Shell, Routing and API Client Foundation" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-015.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-086 — Customer Workspace Replacing CUS Green-Screen Panels

- User Story: WO-061
- Objective: Run critical-path smoke verification for "Customer Workspace Replacing CUS Green-Screen Panels" after deployment.
- Expected: Story "Customer Workspace Replacing CUS Green-Screen Panels" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-061.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-087 — Billing Invoice, Payment and Delinquency Screens

- User Story: WO-079
- Objective: Run critical-path smoke verification for "Billing Invoice, Payment and Delinquency Screens" after deployment.
- Expected: Story "Billing Invoice, Payment and Delinquency Screens" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-079.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-088 — Policy Issuance Screen With Premium Rating Breakdown

- User Story: WO-082
- Objective: Run critical-path smoke verification for "Policy Issuance Screen With Premium Rating Breakdown" after deployment.
- Expected: Story "Policy Issuance Screen With Premium Rating Breakdown" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-082.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-089 — OIDC Sign-In and Role-Based UI Gating

- User Story: WO-087
- Objective: Run critical-path smoke verification for "OIDC Sign-In and Role-Based UI Gating" after deployment.
- Expected: Story "OIDC Sign-In and Role-Based UI Gating" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-087.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-090 — WCAG 2.1 AA Conformance Test Harness and CI Gate

- User Story: WO-095
- Objective: Run critical-path smoke verification for "WCAG 2.1 AA Conformance Test Harness and CI Gate" after deployment.
- Expected: Story "WCAG 2.1 AA Conformance Test Harness and CI Gate" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-095.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-091 — Claims Workspace With Approval and Payment Controls

- User Story: WO-096
- Objective: Run critical-path smoke verification for "Claims Workspace With Approval and Payment Controls" after deployment.
- Expected: Story "Claims Workspace With Approval and Payment Controls" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-096.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-092 — Accessible Component Library Replacing DDS Subfiles

- User Story: WO-099
- Objective: Run critical-path smoke verification for "Accessible Component Library Replacing DDS Subfiles" after deployment.
- Expected: Story "Accessible Component Library Replacing DDS Subfiles" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-099.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-093 — Deploy API Gateway With TLS, Routing and Rate Limiting

- User Story: WO-016
- Objective: Run critical-path smoke verification for "Deploy API Gateway With TLS, Routing and Rate Limiting" after deployment.
- Expected: Story "Deploy API Gateway With TLS, Routing and Rate Limiting" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-016.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-094 — Enforce Zone Segmentation, mTLS and Egress Allow-Lists

- User Story: WO-034
- Objective: Run critical-path smoke verification for "Enforce Zone Segmentation, mTLS and Egress Allow-Lists" after deployment.
- Expected: Story "Enforce Zone Segmentation, mTLS and Egress Allow-Lists" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-034.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-095 — Implement OIDC Federation and JWT Validation Enforcement

- User Story: WO-050
- Objective: Run critical-path smoke verification for "Implement OIDC Federation and JWT Validation Enforcement" after deployment.
- Expected: Story "Implement OIDC Federation and JWT Validation Enforcement" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-050.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-096 — Federate Workload Identity For Batch and Service Calls

- User Story: WO-052
- Objective: Run critical-path smoke verification for "Federate Workload Identity For Batch and Service Calls" after deployment.
- Expected: Story "Federate Workload Identity For Batch and Service Calls" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-052.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-097 — Machine-readable open design item decision register with CI gate

- User Story: WO-008
- Objective: Run critical-path smoke verification for "Machine-readable open design item decision register with CI gate" after deployment.
- Expected: Story "Machine-readable open design item decision register with CI gate" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-008.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-098 — Parallel-run reconciliation engine with per-domain cutover gate scoring

- User Story: WO-022
- Objective: Run critical-path smoke verification for "Parallel-run reconciliation engine with per-domain cutover gate scoring" after deployment.
- Expected: Story "Parallel-run reconciliation engine with per-domain cutover gate scoring" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-022.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-099 — Per-domain cutover control plane with audited rollback switches

- User Story: WO-023
- Objective: Run critical-path smoke verification for "Per-domain cutover control plane with audited rollback switches" after deployment.
- Expected: Story "Per-domain cutover control plane with audited rollback switches" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-023.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-100 — Automated phase-gate evidence pack generation and governance dashboard API

- User Story: WO-041
- Objective: Run critical-path smoke verification for "Automated phase-gate evidence pack generation and governance dashboard API" after deployment.
- Expected: Story "Automated phase-gate evidence pack generation and governance dashboard API" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-041.
- Verify no blocking errors in logs, APIs, or UI.

### SMOKE-101 — Legacy behaviour decision records with preserve-versus-change parity matrix

- User Story: WO-073
- Objective: Run critical-path smoke verification for "Legacy behaviour decision records with preserve-versus-change parity matrix" after deployment.
- Expected: Story "Legacy behaviour decision records with preserve-versus-change parity matrix" satisfies expected smoke validation outcomes without critical issues.

**Steps**
- Deploy the latest build to target environment.
- Execute primary user flow for WO-073.
- Verify no blocking errors in logs, APIs, or UI.

---

## Regression test suite (101)

### REGRESSION-001 — Repository Member Manifest and Completeness Gate

- User Story: WO-001
- Objective: Prevent regressions in existing behavior impacted by "Repository Member Manifest and Completeness Gate".
- Expected: Story "Repository Member Manifest and Completeness Gate" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A committed manifest file at manifest/pcis-manifest.yaml lists all 39 repository members with fields: path, member_type (cobol_program, dds_display_file, cl_member, design_document), module_code, implementation_status (implemented, design_only, empty, unverified), declared_calls, declared_tables and notes.
- Check acceptance criterion 2: A validator command (tools/manifest/validate_manifest.py or equivalent) walks the repository tree, compares it against the manifest, and exits with code 0 only when every file on disk is declared and every declared file exists; it exits non-zero with a per-member reason otherwise.
- Check acceptance criterion 3: The validator detects and reports zero-byte or whitespace-only source members (currently at least CLM006B.cbl, CMM001B.cbl and CUS_Module_Design_Document.md are suspected) as implementation_status=empty and fails if the manifest disagrees.

### REGRESSION-002 — Extract Legacy Behavioural Baseline Specification Artifact

- User Story: WO-002
- Objective: Prevent regressions in existing behavior impacted by "Extract Legacy Behavioural Baseline Specification Artifact".
- Expected: Story "Extract Legacy Behavioural Baseline Specification Artifact" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: baseline/legacy-baseline.yaml is committed and records, for each of the 8 COBOL programs: program name, called-by scheduler entry, commit scope statement, declared cursors with full SQL text, all WORKING-STORAGE numeric and identity literals with name, PIC clause and value, and all AUDLOG01 call sites with the nine parameter names and PIC widths.
- Check acceptance criterion 2: The baseline captures the six regulatory tunables (retention days 365, chunk size 5000, lead days 15, grace days 10, renewal window 60, reinsurance threshold 100000.00) and the six batch actor literals with the exact program and paragraph where each is defined.
- Check acceptance criterion 3: The baseline records every continue-after-failure error path, including the non-00 AUDLOG01 return handling in BIL003B, CMM001B, PRM005B, POL006B and CLM006B, and the archive verification mismatch halt in AUD002B, each with the source paragraph reference.

### REGRESSION-003 — Scripted Dependency-Ordered Legacy COBOL Build

- User Story: WO-009
- Objective: Prevent regressions in existing behavior impacted by "Scripted Dependency-Ordered Legacy COBOL Build".
- Expected: Story "Scripted Dependency-Ordered Legacy COBOL Build" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A single entrypoint (build/scripts/build_legacy.sh) accepts an environment name (dev, tst, prd) and builds all COBOL, DDS and CL members declared in manifest/pcis-manifest.yaml in dependency-correct order, with no manual step and no interactive prompt.
- Check acceptance criterion 2: Compile order is computed from declared dependencies (copybooks and service programs from prologue CALLS, DDS display files bound to interactive programs) rather than hard-coded, and the resolved order is printed and written to build/reports/compile-order.txt.
- Check acceptance criterion 3: Environment-specific library topology (INSDEV/INSDEVDTA, INSTST/INSTSTDTA, INSPRD/INSPRDDTA, shared INSCOM, tooling INSTOOLS) is externalized in build/build.yaml with no library name hard-coded in any script.

### REGRESSION-004 — CI Pipeline Gating Manifest, Baseline and Build

- User Story: WO-024
- Objective: Prevent regressions in existing behavior impacted by "CI Pipeline Gating Manifest, Baseline and Build".
- Expected: Story "CI Pipeline Gating Manifest, Baseline and Build" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A committed pipeline descriptor under ci/ defines source triggers for commit, pull request and tag, and stages for validate, scan, legacy-build and publish, using the Forge Shipping step catalog.
- Check acceptance criterion 2: The validate stage runs the WO-001 manifest validator and the WO-002 baseline drift detector and fails the pipeline with the offending member and reason on any non-zero exit.
- Check acceptance criterion 3: The scan stage runs a secret scan and a static-analysis scan across the repository and tooling code in parallel, and the pipeline blocks progression if any scan reports a blocking finding.

### REGRESSION-005 — Coexistence Topology, Scheduler Map and Runbook

- User Story: WO-025
- Objective: Prevent regressions in existing behavior impacted by "Coexistence Topology, Scheduler Map and Runbook".
- Expected: Story "Coexistence Topology, Scheduler Map and Runbook" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: ops/topology.yaml declares every coexistence environment (dev, tst, prd) with program library, data library, shared library INSCOM, tooling library INSTOOLS, resolved library list order and a data-sensitivity flag marking INSPRDDTA as the only library holding real customer data.
- Check acceptance criterion 2: ops/scheduler-map.yaml maps each scheduler entry (JOBSCHD1, JOBSCHD2, JOBSCHD3) to its programs, invocation cadence, declared commit scope from the WO-002 baseline, expected window, run-log table (RPT_RUN_LOG_T) evidence and an accountable owner role.
- Check acceptance criterion 3: A validator (tools/ops/validate_topology.py) cross-checks ops/scheduler-map.yaml against the CALLED BY lines in the COBOL prologues and against manifest/pcis-manifest.yaml, and exits non-zero on any program that is scheduled but undeclared, or declared but unscheduled.

### REGRESSION-006 — Bootstrap Maven multi-module Java 21 platform skeleton

- User Story: WO-003
- Objective: Prevent regressions in existing behavior impacted by "Bootstrap Maven multi-module Java 21 platform skeleton".
- Expected: Story "Bootstrap Maven multi-module Java 21 platform skeleton" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A root pom.xml exists declaring maven.compiler.release 21 and a reactor containing exactly 13 modules: pcis-bom, pcis-common, pcis-batch-common, pcis-contracts, pcis-migrations, claims-svc, customer-svc, policy-svc, premium-svc, billing-svc, reporting-svc, authz-svc, audit-svc.
- Check acceptance criterion 2: Running a clean full build from a fresh checkout succeeds with no network-pinned SNAPSHOT dependencies and completes in under 10 minutes on a standard runner; the command and prerequisites are documented in a BUILD.md runbook.
- Check acceptance criterion 3: pcis-bom manages versions for Spring Boot 3.5.x, Spring Batch 5.x, Spring Security 6, PostgreSQL JDBC, Flyway, jOOQ or JdbcClient, JUnit 5, AssertJ, Testcontainers, spring-batch-test, logstash-logback-encoder, Micrometer and OpenTelemetry; no child module declares an explicit version for any managed dependency.

### REGRESSION-007 — Provision Terraform infrastructure for three PCIS environments

- User Story: WO-004
- Objective: Prevent regressions in existing behavior impacted by "Provision Terraform infrastructure for three PCIS environments".
- Expected: Story "Provision Terraform infrastructure for three PCIS environments" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Terraform root configurations exist for three environments (dev, test, prod) composed from six reusable modules: network, kubernetes, database, secrets, object-storage and registry; environment differences are expressed only through tfvars, not divergent module code.
- Check acceptance criterion 2: terraform validate and terraform plan succeed for all three environments against a remote state backend with state locking, and plan output for prod shows zero unmanaged resources.
- Check acceptance criterion 3: The database module provisions PostgreSQL 17 with Multi-AZ enabled, automated backups retained for at least 35 days, point-in-time recovery enabled, encryption at rest with a customer-managed key, and storage-in-transit enforced via TLS-only parameter settings; documented RTO and RPO are expressed in hours as agreed.

### REGRESSION-008 — Create reproducible distroless non-root service images

- User Story: WO-012
- Objective: Prevent regressions in existing behavior impacted by "Create reproducible distroless non-root service images".
- Expected: Story "Create reproducible distroless non-root service images" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A shared Dockerfile template plus per-service build configuration produces an image for every service module and for the batch runtime, all based on a pinned distroless Java 21 runtime referenced by digest rather than by mutable tag.
- Check acceptance criterion 2: Every image runs as a non-root numeric UID with a read-only root filesystem, no shell and no package manager present, verified by an automated container-hardening test that inspects the built image.
- Check acceptance criterion 3: Building the same commit twice produces identical image digests (reproducible build), verified by a CI check that builds twice and compares digests, with build timestamps and file ordering normalized.

### REGRESSION-009 — Implement Forge Shipping pipeline with security gates

- User Story: WO-029
- Objective: Prevent regressions in existing behavior impacted by "Implement Forge Shipping pipeline with security gates".
- Expected: Story "Implement Forge Shipping pipeline with security gates" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A single declarative Forge Shipping pipeline definition exists in the repository covering, in order: build:maven, build:docker, four parallel scans, image scan with SBOM emission, push:ecr with signing, deploy to dev, functional test stage, deploy to staging, parity reconciliation gate, manual production approval, production deploy and post-deploy smoke.
- Check acceptance criterion 2: build:maven fails the pipeline when unit tests fail or when line coverage on monetary calculation packages falls below 90 percent; the failing package and actual coverage are reported in the pipeline output.
- Check acceptance criterion 3: The four scan steps run in parallel and every one is a hard gate: SonarQube allows zero new blocker issues, Snyk allows zero critical or high CVEs including transitive dependencies, Gitleaks allows zero detected secrets, and Semgrep allows zero high-severity findings plus a custom rule that fails when a mutating service or controller method lacks an authorization annotation.

### REGRESSION-010 — Establish GitOps delivery with fifteen-minute rollback

- User Story: WO-046
- Objective: Prevent regressions in existing behavior impacted by "Establish GitOps delivery with fifteen-minute rollback".
- Expected: Story "Establish GitOps delivery with fifteen-minute rollback" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Helm charts exist for all eight services and the batch runtime with value overlays for dev, test and prod, each chart setting a non-root securityContext, read-only root filesystem, dropped capabilities, resource requests and limits, and liveness, readiness and startup probes matching the WO-011 image contract.
- Check acceptance criterion 2: Argo CD Application manifests exist per service per environment with automated self-healing sync for dev and test, manual sync for prod, and revision history retaining at least the previous five releases so any of them can be rolled back to by digest.
- Check acceptance criterion 3: A default-deny NetworkPolicy is applied per namespace and each chart declares only the explicit ingress and egress it requires, verified by a policy test that fails when a chart requests unrestricted egress.

### REGRESSION-011 — Define batch CronJob manifests and exit-code contract

- User Story: WO-063
- Objective: Prevent regressions in existing behavior impacted by "Define batch CronJob manifests and exit-code contract".
- Expected: Story "Define batch CronJob manifests and exit-code contract" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Six Kubernetes manifests exist — prm005b-daily-premium, clm006b-claim-payment, aud002b-audit-archive, pol006b-renewal, bil003b-billing-generation and cmm001b-commission — each declaring a CronJob with a schedule, a concurrencyPolicy of Forbid, a backoffLimit, an activeDeadlineSeconds bounding its window, ttlSecondsAfterFinished and the WO-011 batch image referenced by digest.
- Check acceptance criterion 2: A documented exit-code contract file maps every legacy failure signal to a distinct non-zero exit status: at least accumulated item errors above threshold, archive verification count mismatch, cursor or query initialization failure, audit-write failure, and configuration-validation failure; exit zero is reserved for a completed run within the error threshold including runs that processed zero items.
- Check acceptance criterion 3: The pcis-batch-common JobExecutionListener implements the contract so a job that would have DISPLAYed an error and continued now terminates the process with the mapped non-zero status, and a corresponding unit test proves each mapped code.

### REGRESSION-012 — Build shared observability starter with PII-masking structured logging

- User Story: WO-013
- Objective: Prevent regressions in existing behavior impacted by "Build shared observability starter with PII-masking structured logging".
- Expected: Story "Build shared observability starter with PII-masking structured logging" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Adding the pcis-observability-starter dependency to a bare Spring Boot service produces JSON-encoded log output (logstash-logback-encoder) with the mandatory fields correlation_id, service, program, actor, resource, operation, and, for batch runtimes, job_id and run_id — verified by a test that parses emitted log lines as JSON and asserts field presence.
- Check acceptance criterion 2: A Logback masking converter redacts every configured restricted-tier field before emission: TAX_ID rendered as last four characters only, EMAIL rendered as domain only, PHONE, DOB, CUSTOMER_CONTACT_T contact values, address lines and claim payee name fully masked; a unit test feeds each pattern and asserts zero clear-text leakage including when the value appears inside an exception message or a serialized object.
- Check acceptance criterion 3: A Jackson serializer module masks the same annotated fields before any object is written to an audit event payload, proving masking happens at creation and not at display time.

### REGRESSION-013 — Implement structured error library with reason-code registry

- User Story: WO-031
- Objective: Prevent regressions in existing behavior impacted by "Implement structured error library with reason-code registry".
- Expected: Story "Implement structured error library with reason-code registry" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A shared reason-code registry exists as a versioned artifact seeded from the legacy <MOD>#### convention, including at minimum the evidenced codes for concurrent-update conflict, dependency-blocked delete, all-blank search, result-cap reached and generic system error, plus new codes for audit-write failure, cursor-open failure, archive-verification mismatch, skip-outside-lead-window, unrecognised billing frequency, no-active-commission-plan, commission arithmetic size error, sequence-allocation failure, rating-service non-success, reinsurance-referral write failure, authorization-denied-no-approval and authority-limit-exceeded; a unit test asserts uniqueness, non-reuse and presence of a client-safe title for every code.
- Check acceptance criterion 2: All REST error responses are RFC 9457 problem documents carrying type, title, status, detail, instance, a stable code, correlation_id and an errors array capped at 20 entries with per-entry code, detail and field pointer; HTTP status mapping is 400 for invalid input, 401 unauthenticated, 403 forbidden, 404 not found, 409 conflict, 422 business-rule rejection, 500 unexpected.
- Check acceptance criterion 3: A contract test asserts that no problem document field ever contains SQLSTATE, a native SQL error code, a stack frame, an internal class name or a restricted-tier value, while the same failure written to the structured log does carry the internal diagnostic detail for support.

### REGRESSION-014 — Publish baseline metrics, SLO dashboards and alert rules

- User Story: WO-064
- Objective: Prevent regressions in existing behavior impacted by "Publish baseline metrics, SLO dashboards and alert rules".
- Expected: Story "Publish baseline metrics, SLO dashboards and alert rules" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A committed baseline report records, per batch job (AUD002B, BIL003B, PRM005B, CMM001B, CLM006B, POL006B replacements), the measured run duration, row counts, per-run database query count and the declared scheduling window, plus measured p95 latency per API endpoint group; every threshold used later in this story references a value from this report or from an already-fixed requirement number.
- Check acceptance criterion 2: A versioned metric catalogue document and matching code registration exist for at least: batch items selected, processed, errors and skipped; job duration seconds; batch window utilisation ratio; audit write failures; archive chunk archived and deleted; archive verification mismatch; commission no-plan count and total amount; delinquent count; reinsurance flagged count and flag failures; authorization denials; database query count per run — all tagged with job, program, run_id and env.
- Check acceptance criterion 3: Grafana dashboards are committed as JSON-as-code with one dashboard per batch job and one per service, and a rendering test or provisioning dry-run proves each dashboard loads without unresolved datasource or variable references.

### REGRESSION-015 — Author operational runbooks for batch, rollback, purge and incidents

- User Story: WO-080
- Objective: Prevent regressions in existing behavior impacted by "Author operational runbooks for batch, rollback, purge and incidents".
- Expected: Story "Author operational runbooks for batch, rollback, purge and incidents" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Four runbooks are committed under a docs runbooks directory — batch restart and recovery, deployment rollback, purge and archive verification, incident response — each with a fixed section template covering trigger and alert reference, severity, first responder, prerequisites, diagnostic queries and log filters, step-by-step recovery, verification, rollback path, escalation and post-incident actions.
- Check acceptance criterion 2: An alert-to-runbook index maps every alert defined in the alerting story to exactly one runbook section by its runbook reference key, and an automated documentation test fails the build when an alert has no matching section or a section references a non-existent alert.
- Check acceptance criterion 3: The batch restart runbook documents, per job, the evidenced legacy commit boundary and the target chunk configuration — one item per commit for policy, installment, commission and claim-payment jobs and no more than one thousand rows per commit for the audit archive job — plus the restart command, the expected exit-code semantics and how to confirm restart resumed from the last committed chunk.

### REGRESSION-016 — Build audit-svc core with versioned v1 audit event contract

- User Story: WO-010
- Objective: Prevent regressions in existing behavior impacted by "Build audit-svc core with versioned v1 audit event contract".
- Expected: Story "Build audit-svc core with versioned v1 audit event contract" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: audit-svc starts as a Spring Boot 3.5.x service on Java 21, exposes an actuator health endpoint returning UP, and publishes an OpenAPI 3.1 document for POST /v1/audit-events.
- Check acceptance criterion 2: The v1 audit event contract accepts table name up to 30 chars, business key up to 40 chars, field name up to 30 chars, old and new values up to 100 chars each, actor up to 10 chars, and source program/service up to 64 chars, with no silent truncation on any field.
- Check acceptance criterion 3: Action code is an explicit enumeration; both legacy 3-character codes (ADD, UPD, PAY, REN) and legacy 1-character codes (A, C, D) map to canonical enum values via a documented mapping table, and an unknown code is rejected with HTTP 400 and an RFC 9457 problem detail.

### REGRESSION-017 — Make audit writes atomic with mutations via transactional outbox

- User Story: WO-026
- Objective: Prevent regressions in existing behavior impacted by "Make audit writes atomic with mutations via transactional outbox".
- Expected: Story "Make audit writes atomic with mutations via transactional outbox" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A shared audit-outbox library provides a single API that enlists an audit event in the caller's active transaction; calling it outside a transaction fails fast with a descriptive error rather than writing anything.
- Check acceptance criterion 2: Flyway migration adds an audit_outbox table (id, payload JSONB, idempotency_key UUID unique, status, attempt_count, next_attempt_at, created_at, last_error) with an index supporting the relay claim query.
- Check acceptance criterion 3: Fault-injection test: with audit persistence forced to fail, the paired financial mutation is rolled back and the database shows 0 mutated rows and 0 audit rows — proving the legacy PRM005B continue-after-failure behaviour is not reproduced.

### REGRESSION-018 — Convert AUD002B archiving into restartable retention and purge job

- User Story: WO-042
- Objective: Prevent regressions in existing behavior impacted by "Convert AUD002B archiving into restartable retention and purge job".
- Expected: Story "Convert AUD002B archiving into restartable retention and purge job" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A Spring Batch job replaces AUD002B with restart-from-last-committed-chunk behaviour; a fault-injection test kills the job mid-chunk, restarts it, and asserts zero duplicate archived rows and zero rows deleted from live without a verified archive copy.
- Check acceptance criterion 2: Chunk size, retention days per classification tier, and the archive verification tolerance are externalized via configuration properties with no compiled-in constants; the default commit chunk is 1000 rows or fewer (down from the legacy 5000).
- Check acceptance criterion 3: Retention is executed as a monthly partition detach plus cold-archive export rather than DELETE FROM audit_log; an integration test asserts no mass DELETE statement is issued against the live partitioned table.

### REGRESSION-019 — Mask PII and classify data before audit persistence

- User Story: WO-051
- Objective: Prevent regressions in existing behavior impacted by "Mask PII and classify data before audit persistence".
- Expected: Story "Mask PII and classify data before audit persistence" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A committed classification artifact assigns a tier (Public, Internal, Confidential, Restricted) to all 55 tables listed in PCIS_Database_Design.md, and to every field identified as restricted including CUSTOMER_T.TAX_ID, DOB, EMAIL, PHONE and CLAIM_PAYMENT_T payee.
- Check acceptance criterion 2: A build-time check fails the Maven build when any table in the data dictionary has no tier or when a field marked restricted has no masking strategy assigned.
- Check acceptance criterion 3: Annotation-driven masking is applied before the audit event is constructed: tax ID renders as last four characters, email as domain only, phone as last four digits, date of birth as year only — verified by unit tests for each strategy.

### REGRESSION-020 — Expose audit inquiry API with gated unmask and observability

- User Story: WO-067
- Objective: Prevent regressions in existing behavior impacted by "Expose audit inquiry API with gated unmask and observability".
- Expected: Story "Expose audit inquiry API with gated unmask and observability" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: GET /v1/audit-events supports filtering by tableName, businessKey, actor, action and an inclusive date range, with keyset pagination and a bounded maximum page size, returning masked values by default.
- Check acceptance criterion 2: Deny-by-default authorization: every endpoint carries an explicit method-level permission check; an unauthenticated request returns 401, a request without the audit-read permission returns 403, and a security test enumerates all endpoints to prove none is reachable without a grant.
- Check acceptance criterion 3: POST /v1/audit-events/{auditId}/unmask requires a dedicated unmask permission, returns the unmasked field value only for the requested field, and writes its own audit event naming the investigator, the audit id, the field revealed and a mandatory justification.

### REGRESSION-021 — Build authz-svc policy decision service with deny-by-default

- User Story: WO-011
- Objective: Prevent regressions in existing behavior impacted by "Build authz-svc policy decision service with deny-by-default".
- Expected: Story "Build authz-svc policy decision service with deny-by-default" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A POST /v1/authz/decisions endpoint returns 200 with body containing decision (PERMIT or DENY), reasonCode, and evaluatedPermissions for a valid bearer token; missing or expired token returns 401 and an unmapped resource/operation returns decision DENY with reasonCode NO_GRANT.
- Check acceptance criterion 2: Deny-by-default is proven: an integration test asserts that a principal with zero role assignments is denied for every seeded resource/operation pair, and that adding a single grant flips exactly one pair to PERMIT with no side effects on others.
- Check acceptance criterion 3: JWT validation uses local RS256 verification against a cached JWKS (cache TTL configurable, default 1 hour) with no per-request call to the identity provider; a test with a token signed by an untrusted key returns 401 with RFC 9457 problem detail and no stack trace or secret in the response body.

### REGRESSION-022 — Enforce approval linkage and cumulative claim authority limits

- User Story: WO-030
- Objective: Prevent regressions in existing behavior impacted by "Enforce approval linkage and cumulative claim authority limits".
- Expected: Story "Enforce approval linkage and cumulative claim authority limits" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A POST /v1/authz/claim-payments/decisions request for a claim with no APPROVED approval row linked to the payment request returns decision DENY with reasonCode APPROVAL_MISSING, and no approval row is created or mutated as a side effect.
- Check acceptance criterion 2: Given an APPROVED approval exists but CLAIM_ADJUSTER_T authority limit is less than paid-to-date plus requested amount, the response is DENY with reasonCode AUTHORITY_LIMIT_EXCEEDED — asserted by a test replicating BR-01: a 25000 limit, 20000 already paid and a further 10000 requested is denied even though 10000 alone is within limit.
- Check acceptance criterion 3: Given both checks pass, the response is PERMIT and includes approvalId, approverPrincipal, authorityLimitApplied and cumulativePaidToDate so the caller can record approver identity and the limit applied in the audit event.

### REGRESSION-023 — Apply deny-by-default guards to financial mutation endpoints

- User Story: WO-048
- Objective: Prevent regressions in existing behavior impacted by "Apply deny-by-default guards to financial mutation endpoints".
- Expected: Story "Apply deny-by-default guards to financial mutation endpoints" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A shared security starter module is published and consumed by at least one domain service; its default SecurityFilterChain denies every request that is not explicitly permitted, proven by a test asserting 401 or 403 on an unmapped path.
- Check acceptance criterion 2: Every mutating application-layer use case carries a method-level authorization guard referencing an explicit permission string; a committed inventory document or generated report lists each mutating endpoint with its required permission and HTTP method.
- Check acceptance criterion 3: The CI gate fails the build when a deliberately added unguarded mutating handler is present and passes once the guard is applied — demonstrated by a committed negative fixture that the gate detects and a green run after remediation.

### REGRESSION-024 — Replace batch actor literals with authenticated service principals

- User Story: WO-049
- Objective: Prevent regressions in existing behavior impacted by "Replace batch actor literals with authenticated service principals".
- Expected: Story "Replace batch actor literals with authenticated service principals" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A batch job started without valid workload credentials fails before processing the first item, exits with a non-zero status, emits a structured error with job name, resource and operation, and writes no business row.
- Check acceptance criterion 2: A batch job started with valid credentials resolves exactly one service principal for the whole execution; an integration test asserts the principal is available in every step, item reader, processor and writer via the job-scoped actor context.
- Check acceptance criterion 3: Every persisted row written by a migrated job populates crt_user and upd_user from the authenticated principal — proven by a test that asserts no row contains any of the strings BATCHAUD, BATCHBIL, BATCHCMM, BATCHPRM, BATCHCLM or BATCHREN.

### REGRESSION-025 — Automate authorization regression and segregation-of-duties control evidence

- User Story: WO-065
- Objective: Prevent regressions in existing behavior impacted by "Automate authorization regression and segregation-of-duties control evidence".
- Expected: Story "Automate authorization regression and segregation-of-duties control evidence" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: An authorization regression suite runs in CI and covers, for every financial mutation surface (claim payment, policy renewal, billing generation, commission posting, installment aging, audit purge), the cases permitted, denied for missing grant, denied for missing approval, denied for authority limit exceeded, and denied for unauthenticated caller — with the build failing on any gap.
- Check acceptance criterion 2: A disbursement-integrity check asserts that every claim payment row has exactly one linked CONSUMED approval and a non-null recorded authority limit; the test fails when a fixture inserts a payment without an approval, proving the control detects the violation.
- Check acceptance criterion 3: Consumer-driven contract tests (Spring Cloud Contract or Pact) freeze the authorization decision contract; a deliberately breaking change to a field name or reason-code value fails the build, and additive changes pass.

### REGRESSION-026 — Machine-readable data classification registry for all PCIS entities

- User Story: WO-005
- Objective: Prevent regressions in existing behavior impacted by "Machine-readable data classification registry for all PCIS entities".
- Expected: Story "Machine-readable data classification registry for all PCIS entities" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A machine-readable registry file (for example src/main/resources/classification/pcis-data-classification.yaml) exists and assigns exactly one tier from Public, Internal, Confidential, Restricted to every entity and to every column of every entity, with no column left unclassified.
- Check acceptance criterion 2: The registry contains an entity reconciliation section that explicitly lists entities present in code but absent from PCIS_Database_Design.md (AUDIT_LOG_ARCHIVE_T, COMMISSION_LEDGER_T) and documents the final authoritative entity count, proving it is not 55.
- Check acceptance criterion 3: AUDIT_LOG_T column names are canonicalized to a single set (audit_log_id, table_name, key_value, action_cd, field_name, old_value, new_value, chg_user, chg_timestamp, program_name) with a documented mapping from each of the three legacy spellings, and the key generation strategy (SEQUENCE versus IDENTITY) is decided and recorded.

### REGRESSION-027 — Shared PII masking library with Jackson and Logback integration

- User Story: WO-017
- Objective: Prevent regressions in existing behavior impacted by "Shared PII masking library with Jackson and Logback integration".
- Expected: Story "Shared PII masking library with Jackson and Logback integration" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A pcis-pii-masking module exposes an annotation (for example Classified with tier and mask attributes) and a MaskingService whose primary lookup is by entity name plus column name against the data_classification table or in-memory registry loaded in WO-050.
- Check acceptance criterion 2: Mask strategies produce exactly these canonical outputs: tax ID renders last four characters only, email renders domain only, phone renders last four digits only, date of birth renders year only, free-text renders a fixed redaction token, and no strategy ever emits any part of the original beyond what its rule allows.
- Check acceptance criterion 3: A Jackson BeanSerializerModifier masks annotated fields in every serialized payload, verified for API response DTOs and for the audit event payload object.

### REGRESSION-028 — Permission-gated self-audited PII unmask action for investigators

- User Story: WO-035
- Objective: Prevent regressions in existing behavior impacted by "Permission-gated self-audited PII unmask action for investigators".
- Expected: Story "Permission-gated self-audited PII unmask action for investigators" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A distinct pii:unmask permission exists, is deny-by-default, and is enforced server-side with method-level authorization on the unmask endpoint; no client-side or menu-level gating is relied upon.
- Check acceptance criterion 2: The unmask request requires a non-empty justification of a configured minimum length and a target reference (entity, column, subject id); a request missing any of these returns HTTP 400 with an RFC 9457 problem detail.
- Check acceptance criterion 3: A caller lacking pii:unmask receives HTTP 403 with an RFC 9457 problem detail containing no internal detail, no stack trace and no partial value, and an authorization_denied audit event is emitted with actor, resource and operation.

### REGRESSION-029 — Tiered retention with partitioned audit table and restartable job

- User Story: WO-038
- Objective: Prevent regressions in existing behavior impacted by "Tiered retention with partitioned audit table and restartable job".
- Expected: Story "Tiered retention with partitioned audit table and restartable job" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: AUDIT_LOG_T is monthly range-partitioned in PostgreSQL via Flyway migrations, with an automated partition pre-creation step so a run never fails for a missing future partition.
- Check acceptance criterion 2: Retention periods are read per classification tier from a versioned, change-audited configuration table (successor to RPT_PARM_T) with an effective-from date and change history, and the audit tier retention can never be configured below one year — an attempt is rejected with a validation error.
- Check acceptance criterion 3: The retention step detaches expired partitions as a metadata operation instead of executing DELETE FROM AUDIT_LOG_T, and a test asserts no row-level DELETE statement is issued against the live audit table during a retention run.

### REGRESSION-030 — Mask PII at audit event creation before outbox persistence

- User Story: WO-043
- Objective: Prevent regressions in existing behavior impacted by "Mask PII at audit event creation before outbox persistence".
- Expected: Story "Mask PII at audit event creation before outbox persistence" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: The audit service masks old_value and new_value using the WO-051 MaskingService keyed on canonical table_name plus field_name before constructing the persisted audit event and before writing the transactional outbox row.
- Check acceptance criterion 2: Tax ID renders last four characters only, email renders domain only, phone renders last four digits only, DOB renders year only, and free-text narrative fields render the fixed redaction token in every persisted audit event.
- Check acceptance criterion 3: The CUS005A-equivalent erasure path (cascading physical delete of CUSTOMER_CONTACT_T, CUSTOMER_ADDRESS_T and CUSTOMER_T with a full before-image audit set) produces audit rows containing zero cleartext Restricted values, verified by an explicit test.

### REGRESSION-031 — CI gates for unclassified entities and unmasked PII leakage

- User Story: WO-047
- Objective: Prevent regressions in existing behavior impacted by "CI gates for unclassified entities and unmasked PII leakage".
- Expected: Story "CI gates for unclassified entities and unmasked PII leakage" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A build step (Maven test plus Flyway-applied schema check) fails when any table or column in information_schema is missing from the classification registry, and passes when the registry is complete.
- Check acceptance criterion 2: A committed negative fixture adds an intentionally unclassified table via a test-only migration and a test asserts the classification gate fails for it.
- Check acceptance criterion 3: A custom Semgrep rule set (committed under a ci or semgrep directory) flags logger calls, string concatenations feeding loggers, and audit payload construction that pass a Restricted-tier field without the MaskingService, and the scan:semgrep pipeline step fails the build on any finding.

### REGRESSION-032 — Automated purge with cryptographic erasure and immutable evidence

- User Story: WO-057
- Objective: Prevent regressions in existing behavior impacted by "Automated purge with cryptographic erasure and immutable evidence".
- Expected: Story "Automated purge with cryptographic erasure and immutable evidence" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A purge job runs on a schedule such that 100 percent of records past their tier retention period are purged within 24 hours of expiry, proven by a time-advanced integration test asserting zero remaining expired records after one scheduled cycle.
- Check acceptance criterion 2: Purge uses physical deletion of detached partitions or per-subject cryptographic erasure via KMS key destruction; soft-delete alone is never used, and a test asserts purged data is unreadable after key destruction.
- Check acceptance criterion 3: Detached partitions are written to object storage with Object Lock in compliance mode plus a lifecycle expiry rule matching the tier retention period, provisioned as infrastructure-as-code, and a test or verification script asserts an overwrite or delete attempt before expiry is rejected.

### REGRESSION-033 — Create versioned tunables and rules configuration schema

- User Story: WO-006
- Objective: Prevent regressions in existing behavior impacted by "Create versioned tunables and rules configuration schema".
- Expected: Story "Create versioned tunables and rules configuration schema" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Flyway migration creates CONFIG_TUNABLE_T with primary key TUNABLE_KEY VARCHAR(60), plus DOMAIN_CD CHAR(3), VALUE_TYPE CHAR(1), VALUE_TEXT VARCHAR(200), NUMERIC_VALUE DECIMAL(11,2), MIN_VALUE, MAX_VALUE, UNIT_CD, DESCRIPTION, EFFECTIVE_FROM DATE NOT NULL, EFFECTIVE_TO DATE NULL, VERSION_NO INTEGER NOT NULL, and the four standard PCIS audit columns.
- Check acceptance criterion 2: Flyway migration creates append-only CONFIG_TUNABLE_HISTORY_T with BIGINT GENERATED ALWAYS AS IDENTITY surrogate key, TUNABLE_KEY, VERSION_NO, OLD_VALUE, NEW_VALUE, CHANGE_REASON, CHANGED_BY, CHANGED_TIMESTAMP, and a database rule or trigger that rejects UPDATE and DELETE on the table.
- Check acceptance criterion 3: Seed migration inserts exactly six rows with legacy-equivalent values: audit.retention.days=365, audit.archive.chunkSize=1000, billing.leadDays=15, premium.graceDays=10, policy.renewalWindowDays=60, claims.reinsurance.cessionThreshold=100000.00, each with MIN_VALUE and MAX_VALUE bounds and a non-null DESCRIPTION.

### REGRESSION-034 — Typed tunable resolution service with cache and fail-fast validation

- User Story: WO-018
- Objective: Prevent regressions in existing behavior impacted by "Typed tunable resolution service with cache and fail-fast validation".
- Expected: Story "Typed tunable resolution service with cache and fail-fast validation" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: TunableResolver exposes typed accessors returning int, long, BigDecimal, boolean and String, and throws a distinct TunableNotFoundException or TunableOutOfRangeException rather than returning a silent default when a key is absent or violates its bounds.
- Check acceptance criterion 2: PcisTunableProperties binds compile-time fallback defaults via @ConfigurationProperties with jakarta validation annotations, and the resolution order is documented and tested as database effective row, then property override, then fail-fast.
- Check acceptance criterion 3: Application startup fails with a non-zero exit and a structured log line naming the offending key when any tunable declared required in the TunableKey registry is missing, disabled or outside MIN_VALUE/MAX_VALUE.

### REGRESSION-035 — Admin tunables REST API with RBAC and change evidence

- User Story: WO-036
- Objective: Prevent regressions in existing behavior impacted by "Admin tunables REST API with RBAC and change evidence".
- Expected: Story "Admin tunables REST API with RBAC and change evidence" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: GET /v1/admin/tunables returns a paged list of tunables with key, domain, current value, unit, bounds, effective dates, version and description; GET /v1/admin/tunables/{key}/history returns the append-only change history newest first.
- Check acceptance criterion 2: PUT /v1/admin/tunables/{key} accepts new value, effective-from date, expected version and change reason, returns 200 with the new version number, and returns 409 with a problem detail when expected version does not match.
- Check acceptance criterion 3: All admin endpoints are deny-by-default: an unauthenticated request returns 401 and an authenticated principal without the configuration-admin authority returns 403, both as RFC 9457 problem details with no tunable values in the body.

### REGRESSION-036 — Admin tunables web panel with versioned change history

- User Story: WO-037
- Objective: Prevent regressions in existing behavior impacted by "Admin tunables web panel with versioned change history".
- Expected: Story "Admin tunables web panel with versioned change history" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: The admin tunables route renders a table of all tunables returned by GET /v1/admin/tunables showing key, domain, current value, unit, bounds, effective-from and a version badge, with loading, empty and error states implemented.
- Check acceptance criterion 2: The edit drawer performs client-side validation against the bounds and value type returned by the API, requires a non-empty change reason of at least the documented minimum length, and disables submit until the form is valid; server-side rejection reason codes from the API are surfaced as field-anchored plain-language messages.
- Check acceptance criterion 3: Successful submission calls PUT /v1/admin/tunables/{key} with the expected version number, optimistically shows the new version badge, refetches the list and history, and surfaces a 409 conflict as a clear reload-and-retry message rather than a silent failure.

### REGRESSION-037 — Externalized code-table and business rules store service

- User Story: WO-055
- Objective: Prevent regressions in existing behavior impacted by "Externalized code-table and business rules store service".
- Expected: Story "Externalized code-table and business rules store service" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Flyway migrations create CODE_TABLE_T (domain code, code value, description, sort order, active flag, effective dates, standard audit columns) with a unique constraint on domain plus code value plus effective-from.
- Check acceptance criterion 2: Seed migrations populate at minimum the billing frequency to interval mapping (M, Q, S and the default fallback), the billing schedule status domain including V for void, the reserve status domain including AP and PD, claim type codes and cancellation reason codes, each with a description.
- Check acceptance criterion 3: A CodeTableService exposes typed lookup, list-by-domain and validate-membership operations backed by the same Caffeine cache and refresh pattern established in WO-061, with no direct repository access from domain code.

### REGRESSION-038 — Author Flyway Baseline PostgreSQL Schema Migrations

- User Story: WO-007
- Objective: Prevent regressions in existing behavior impacted by "Author Flyway Baseline PostgreSQL Schema Migrations".
- Expected: Story "Author Flyway Baseline PostgreSQL Schema Migrations" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Flyway migrations under db/migration create every table in the canonical dictionary, split into six module-group batches, with one SQL file per table plus separate files for indexes, so each object retains independent change control equivalent to the QSQLSRC one-member-per-object convention.
- Check acceptance criterion 2: A Testcontainers PostgreSQL 17 integration test starts an empty database, runs Flyway migrate, and asserts a zero-error result plus an exact table count matching the dictionary; the test is wired into CI as the dev deployment gate.
- Check acceptance criterion 3: An automated assertion compares every column created in the container against the canonical dictionary for name, PostgreSQL type, numeric precision and scale, nullability and default, and fails with the specific column name on any mismatch.

### REGRESSION-039 — Implement Sequence Objects, Block Allocator and Key Formatter

- User Story: WO-019
- Objective: Prevent regressions in existing behavior impacted by "Implement Sequence Objects, Block Allocator and Key Formatter".
- Expected: Story "Implement Sequence Objects, Block Allocator and Key Formatter" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Flyway migrations create every required sequence: the fifteen declared in the architecture (customer, agent, quote, policy number, coverage, deductible, policy property, policy vehicle, billing schedule, claim number, claim payment, payment, refund, document, audit log) plus the three code-witnessed ones (invoice, commission ledger, and the claim payment sequence name variant), each with explicit INCREMENT, MINVALUE, MAXVALUE, CACHE and NO CYCLE.
- Check acceptance criterion 2: Sequence MAXVALUE and CACHE are set so that generated numeric values remain within the legacy S9(9) host-variable ceiling of 999999999 where a legacy consumer still reads the value, and a unit test asserts the configured maximum for each such sequence.
- Check acceptance criterion 3: A block allocator component fetches sequence values in externally configurable blocks (default 100) and hands them out in memory; an integration test issuing 10000 allocations asserts at most 100 database round trips, proving the at-least-90-percent reduction target.

### REGRESSION-040 — Resolve Schema Discrepancies and Publish Corrected Data Dictionary

- User Story: WO-020
- Objective: Prevent regressions in existing behavior impacted by "Resolve Schema Discrepancies and Publish Corrected Data Dictionary".
- Expected: Story "Resolve Schema Discrepancies and Publish Corrected Data Dictionary" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A canonical machine-readable data dictionary file exists in the repository (YAML or JSON) covering all 55 designed tables plus the code-witnessed extras COMMISSION_LEDGER_T and AUDIT_LOG_ARCHIVE_T and the premium-engine tables DISCOUNT_RULE_T, SURCHARGE_RULE_T, TAX_TABLE_T and RISK_SCORE_FACTOR_T, with table name, column name, PostgreSQL type, precision and scale, nullability, default, key role and data classification tier for every column.
- Check acceptance criterion 2: A discrepancy register documents at minimum the thirteen identified conflict classes, each with: source-of-truth citation (design document section and COBOL file plus paragraph), the two conflicting definitions, the chosen resolution, the rationale, the affected downstream programs, and a named decision owner.
- Check acceptance criterion 3: Every surrogate key in the dictionary is explicitly classified as either business-document-key-from-sequence (fixed-length VARCHAR or CHAR, prefix plus zero-pad) or detail-surrogate-identity (BIGINT GENERATED ALWAYS AS IDENTITY); BILL_SCHED_ID, INVOICE_ID, LEDGER_ID, RESERVE_ID, CLAIM_PAYMENT_T payment identifier, DEDUCT_ID, POL_PROP_ID and POL_VEH_ID each carry a recorded decision.

### REGRESSION-041 — Automate Masked Anonymized Non-Production Data Refresh

- User Story: WO-021
- Objective: Prevent regressions in existing behavior impacted by "Automate Masked Anonymized Non-Production Data Refresh".
- Expected: Story "Automate Masked Anonymized Non-Production Data Refresh" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: An anonymization pipeline populates a development-equivalent and a test-equivalent PostgreSQL database from the canonical dictionary and its classification tiers, driven by configuration with no hand-editing of SQL per run, replacing the manual masked-extract step described in the architecture library topology.
- Check acceptance criterion 2: Every restricted-tier field in the recorded inventory is masked or synthesised, including customer tax identifier, date of birth, email, phone and name fields, customer contact value, all customer address fields, agent name, email and phone, agent license number, vehicle identification number, property address fields, claim payment payee, claim note free text, underwriting decision reason, endorsement description, policy history event description, user credentials, and both audit log and audit log archive old and new value columns.
- Check acceptance criterion 3: Masked values remain valid against the documented validation rules: tax identifier retains nine numeric digits, email retains an at sign plus a domain-shaped suffix, phone retains ten digits, postal code retains five or nine digits, state code resolves in the code table state domain, and date of birth yields an age of at least sixteen — each asserted by test.

### REGRESSION-042 — Build Polling Extraction and Idempotent PostgreSQL Loader

- User Story: WO-039
- Objective: Prevent regressions in existing behavior impacted by "Build Polling Extraction and Idempotent PostgreSQL Loader".
- Expected: Story "Build Polling Extraction and Idempotent PostgreSQL Loader" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A per-domain extract definition set exists for Customer, Claims, Billing, Premium, Commission, Policy and Audit, each naming its source tables, its watermark column, its explicit projected column list (never SELECT star) and its target table mapping, and each is validated at startup against the canonical data dictionary.
- Check acceptance criterion 2: The loader is idempotent: running the same extract batch twice produces identical target row counts and identical column values, verified by an integration test that loads a fixture batch, re-loads it, and asserts zero duplicate keys and zero changed values.
- Check acceptance criterion 3: Commit chunk size is externally configurable and defaults to at most 1000 rows, satisfying the requirement to reduce commit blast radius from the legacy 5000; the configured value is asserted by test and logged at run start.

### REGRESSION-043 — Build Nightly Cent-Level Parallel-Run Reconciliation Harness

- User Story: WO-058
- Objective: Prevent regressions in existing behavior impacted by "Build Nightly Cent-Level Parallel-Run Reconciliation Harness".
- Expected: Story "Build Nightly Cent-Level Parallel-Run Reconciliation Harness" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A nightly reconciliation job compares source and target per domain for Billing, Premium and delinquency, Commission, Claims, Policy and renewal, and Audit, asserting row counts, cent-level amount equality and column checksums, and it reads the PostgreSQL streaming read replica rather than the OLTP primary.
- Check acceptance criterion 2: Per-domain amount and invariant assertions are implemented exactly as specified: billing compares due amount, paid amount, invoice amount and annual premium plus installment number, due date and status with the installment equal to annual premium divided by installment count at NUMERIC(9,2) using HALF_UP; premium compares base premium, final premium and total factor at NUMERIC(7,4) plus status transitions under a ten-day grace; commission compares commission amount, rate and paid amount plus the count of rows flagged as commissioned; claims compares approved amount, paid to date, payment amount and recovery amount plus the reserve status transition and the 100000.00 cession threshold; policy compares annual premium, limit amount and premium amount plus expiry status, exactly two history events and unchanged policy number format; audit compares row counts and the archive-then-delete invariant.
- Check acceptance criterion 3: Cross-cutting assertions are implemented: sequence high-water marks with zero key collisions, NUMERIC scale preservation per column, NULL-versus-blank fidelity, CHAR trailing-space semantics and UPD_TIMESTAMP microsecond fidelity so optimistic locking is not silently broken.

### REGRESSION-044 — Deterministic Seed Data Harness for Batch Regression Fixtures

- User Story: WO-014
- Objective: Prevent regressions in existing behavior impacted by "Deterministic Seed Data Harness for Batch Regression Fixtures".
- Expected: Story "Deterministic Seed Data Harness for Batch Regression Fixtures" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Running the same named scenario twice in a row produces byte-identical extracts of every seeded table, including all DECIMAL(9,2) and DECIMAL(11,2) columns, verified by a checksum assertion in the harness self-test.
- Check acceptance criterion 2: Scenario catalog covers at minimum: billing frequency M, Q, S and an out-of-domain value; days-out exactly 15, 14 and 16 relative to the fixed reference date; installments exactly 10, 9 and 11 days past due; reserves where APPROVED_AMT exceeds PAID_TO_DATE and reserves where it does not; payment amounts at 99999.99, 100000.00 and 100000.01 against the cession threshold; agents with an in-force commission plan and agents with none; audit rows immediately either side of a 365-day cutoff.
- Check acceptance criterion 3: All business document keys (CUST_ID, AGT_ID, POL_NBR, CLM_NBR) are allocated from harness-controlled SEQUENCE objects as fixed-length character values, never IDENTITY columns, and detail surrogate keys are reset to a known start value before every scenario load.

### REGRESSION-045 — Capture COBOL Baseline Golden Outputs with Determinism Controls

- User Story: WO-032
- Objective: Prevent regressions in existing behavior impacted by "Capture COBOL Baseline Golden Outputs with Determinism Controls".
- Expected: Story "Capture COBOL Baseline Golden Outputs with Determinism Controls" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A single documented command per program per scenario restores the seeded library, runs the COBOL program with an injected reference date, and writes a canonical golden artifact set to a deterministic path under the golden resources tree.
- Check acceptance criterion 2: Three consecutive capture runs of every program/scenario pair produce byte-identical artifacts; any pair that does not is automatically quarantined with a recorded reason rather than committed as a golden.
- Check acceptance criterion 3: Captured artifacts include full post-run row images for every mutated table, the RPT_RUN_LOG_T counter row, program DISPLAY output, and the final program completion status, with timestamps normalised and generated surrogate keys rewritten to stable ordinal placeholders.

### REGRESSION-046 — Cent-Level Golden Output Comparison Engine and Diff Reporting

- User Story: WO-033
- Objective: Prevent regressions in existing behavior impacted by "Cent-Level Golden Output Comparison Engine and Diff Reporting".
- Expected: Story "Cent-Level Golden Output Comparison Engine and Diff Reporting" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A single assertion entry point compares actual post-run state against a named golden and passes only when every table, row and column matches, with zero tolerance on all NUMERIC(9,2) and NUMERIC(11,2) columns.
- Check acceptance criterion 2: A one-cent difference in any monetary column, a missing row, an extra row, or a status-code difference each cause a distinct, clearly worded failure naming table, business key, column, expected value, actual value and delta.
- Check acceptance criterion 3: Normalisation is declarative and auditable: only fields explicitly listed as non-deterministic (capture timestamps, IDENTITY surrogate keys, job identifiers) may be masked, and an attempt to normalise a monetary or status column fails the comparator's own configuration validation.

### REGRESSION-047 — Golden-Output Regression Suites for Six Batch Programs

- User Story: WO-053
- Objective: Prevent regressions in existing behavior impacted by "Golden-Output Regression Suites for Six Batch Programs".
- Expected: Story "Golden-Output Regression Suites for Six Batch Programs" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: One regression suite exists per batch program (AUD002B, BIL003B, CMM001B, PRM005B, CLM006B, POL006B) with a test method per committed scenario, each asserting table-level cent equivalence, RPT_RUN_LOG_T counter parity and commit-boundary parity via the WO-082 comparator.
- Check acceptance criterion 2: Characterization suites exist for the two evidenced interactive transactions (customer creation per CUS001A and policy creation per POL001A), asserting the multi-row insert set, duplicate tax-ID rejection, and the created billing schedule row.
- Check acceptance criterion 3: A coverage guard test enumerates the golden artifact tree and fails the build if any program or scenario present in the goldens has no corresponding executing test, so coverage cannot silently regress to below 100 percent of captured behaviour.

### REGRESSION-048 — Batch Fault Injection Proving Restart Without Duplicate Writes

- User Story: WO-054
- Objective: Prevent regressions in existing behavior impacted by "Batch Fault Injection Proving Restart Without Duplicate Writes".
- Expected: Story "Batch Fault Injection Proving Restart Without Duplicate Writes" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A fault-injection API allows a test to fail a job at a configurable item index, at a configurable statement ordinal, or by aborting the process mid-chunk, and to then restart the job through the Spring Batch JobOperator restart path.
- Check acceptance criterion 2: For every one of the six batch jobs, a restart test asserts the final state after failure-plus-restart is identical to the clean-run golden via the WO-082 comparator, with zero duplicate and zero orphaned financial rows.
- Check acceptance criterion 3: Named invariants are asserted after every fault-injection run: no duplicate COMMISSION_LEDGER_T row for the same BILL_SCHED_ID; no CLAIM_PAYMENT_T row without a corresponding reserve status transition; no AUDIT_LOG_T row deleted without a verified AUDIT_LOG_ARCHIVE_T copy; no BILLING_SCHEDULE_T installment without its paired INVOICE_T row; no financial mutation persisted without its audit event.

### REGRESSION-049 — CI Gate Wiring Coverage and Regression Evidence Artifacts

- User Story: WO-078
- Objective: Prevent regressions in existing behavior impacted by "CI Gate Wiring Coverage and Regression Evidence Artifacts".
- Expected: Story "CI Gate Wiring Coverage and Regression Evidence Artifacts" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A pipeline definition runs, in order, the Maven build with unit tests, the Testcontainers-backed golden-output regression suite, and the fault-injection restart suite, and any failure in any of the three blocks the build before any image push or deployment step.
- Check acceptance criterion 2: JaCoCo coverage enforcement is scoped to the monetary calculation packages and fails the build below 90 percent line coverage, with the scoped package list committed as configuration rather than hardcoded in a script.
- Check acceptance criterion 3: On failure, the comparator JSON and text diff reports, the fault-injection invariant results and the coverage report are published as retained build artifacts and the build summary names the failing program and scenario without requiring a local rerun.

### REGRESSION-050 — Create PostgreSQL customer schema with Flyway migrations

- User Story: WO-068
- Objective: Prevent regressions in existing behavior impacted by "Create PostgreSQL customer schema with Flyway migrations".
- Expected: Story "Create PostgreSQL customer schema with Flyway migrations" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Flyway migration V1 creates customer schema objects: CUSTOMER_T, CUSTOMER_ADDRESS_T, CUSTOMER_CONTACT_T, and three sequences, and applies cleanly against an empty PostgreSQL 17 database.
- Check acceptance criterion 2: CUST_ID is VARCHAR(10) populated from SEQ_CUSTOMER_ID via a zero-padded formatting helper, never a SERIAL or IDENTITY column; ADDRESS_ID and CONTACT_ID are BIGINT GENERATED ALWAYS AS IDENTITY per the design document convention.
- Check acceptance criterion 3: Column types mirror the Db2 for i design: CUST_NAME VARCHAR(60), TAX_ID VARCHAR(11), DOB DATE, EMAIL VARCHAR(60), PHONE VARCHAR(15), CUST_STATUS CHAR(1), CREDIT_SCORE SMALLINT, plus CRT_USER, CRT_TIMESTAMP, UPD_USER, UPD_TIMESTAMP on every table.

### REGRESSION-051 — Build customer-svc domain and persistence layers

- User Story: WO-083
- Objective: Prevent regressions in existing behavior impacted by "Build customer-svc domain and persistence layers".
- Expected: Story "Build customer-svc domain and persistence layers" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A customer-svc Maven module exists with four packages (controller, application, domain, infrastructure) and an enforced dependency rule: the domain package imports no Spring, JPA or Jackson types, verified by an ArchUnit test.
- Check acceptance criterion 2: Domain layer contains Customer, CustomerAddress and CustomerContact with invariants ported from CUS001A and CUS_Module_Design_Document.md: mandatory name, customer type and tax ID; enumerated gender, marital status and customer status; credit score bounded 0 to 999; optional address and contact groups all-or-nothing.
- Check acceptance criterion 3: CustomerValidator replaces CUSVAL01 and returns a structured collection of field-anchored violations rather than terse coded messages, and all violations for one submission are returned together as the COBOL WS-MSG-ENTRY table did.

### REGRESSION-052 — Expose versioned customer REST API with deny-by-default authorization

- User Story: WO-084
- Objective: Prevent regressions in existing behavior impacted by "Expose versioned customer REST API with deny-by-default authorization".
- Expected: Story "Expose versioned customer REST API with deny-by-default authorization" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Endpoints exist and are documented in a generated OpenAPI 3.1 contract: POST /v1/customers, GET /v1/customers/{custId}, PATCH /v1/customers/{custId}, GET /v1/customers (paged search by name, tax-ID suffix and status).
- Check acceptance criterion 2: Spring Security 6 is configured as an OAuth2 resource server with local RS256 validation against a cached JWKS; requests with no or an invalid token return 401 and never reach the application layer.
- Check acceptance criterion 3: Deny-by-default is enforced: the security configuration denies any request not explicitly permitted, every controller method carries @PreAuthorize, and an automated test enumerates all mapped endpoints and fails if any lacks an authorization annotation.

### REGRESSION-053 — Publish masked customer read projection for downstream consumers

- User Story: WO-085
- Objective: Prevent regressions in existing behavior impacted by "Publish masked customer read projection for downstream consumers".
- Expected: Story "Publish masked customer read projection for downstream consumers" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: GET /v1/customers/{custId}/projection returns a documented, versioned read projection containing only the fields evidenced as needed by policy, claims and reporting consumers, with a committed field-to-consumer mapping document.
- Check acceptance criterion 2: All restricted-tier fields in the projection are masked before serialization: tax ID as last four characters, email as domain only, phone as last four digits, date of birth as year only; the unmasked variants are not present anywhere in the payload.
- Check acceptance criterion 3: The projection endpoint is read-only, requires an explicit service-to-service scope such as customer:project, and denies by default; no mutating verb is exposed on the projection path.

### REGRESSION-054 — Establish customer slice parity harness and CI quality gates

- User Story: WO-091
- Objective: Prevent regressions in existing behavior impacted by "Establish customer slice parity harness and CI quality gates".
- Expected: Story "Establish customer slice parity harness and CI quality gates" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A parity test module exists that loads recorded baseline fixtures representing CUS001A input/output pairs (customer with address and contact, customer without optional data, duplicate tax ID rejection, invalid field set, boundary credit score) and replays them through customer-svc.
- Check acceptance criterion 2: A reconciliation comparator asserts field-level equivalence between baseline expected records and customer-svc persisted records, reporting differences as a structured list of table, key, field, expected and actual rather than a single boolean.
- Check acceptance criterion 3: The suite runs offline in one command with Testcontainers PostgreSQL and a mock OIDC issuer, requiring no IBM i connection, no shared database and no external identity provider.

### REGRESSION-055 — Provision Claims PostgreSQL Schema With Exact Decimal Precision

- User Story: WO-027
- Objective: Prevent regressions in existing behavior impacted by "Provision Claims PostgreSQL Schema With Exact Decimal Precision".
- Expected: Story "Provision Claims PostgreSQL Schema With Exact Decimal Precision" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Flyway migration scripts create all Claims tables (CLAIM_T, CLAIM_RESERVE_T, CLAIM_RESERVE_HISTORY_T, CLAIM_PAYMENT_T, CLAIM_ADJUSTER_T, CLAIM_NOTE_T, APPROVAL_T, RECOVERY_T) and run cleanly from an empty PostgreSQL 17 database with zero manual steps.
- Check acceptance criterion 2: Every monetary column is NUMERIC(9,2) or NUMERIC(11,2) and a schema assertion test fails the build if any money column uses double precision, real or an unscaled numeric type.
- Check acceptance criterion 3: CLM_NBR, CLAIM_PAYMENT_ID business keys are allocated from PostgreSQL SEQUENCE objects producing fixed-length VARCHAR values of the documented widths; no business document key uses an IDENTITY column.

### REGRESSION-056 — Model Claim Approval Lifecycle As First-Class Record

- User Story: WO-044
- Objective: Prevent regressions in existing behavior impacted by "Model Claim Approval Lifecycle As First-Class Record".
- Expected: Story "Model Claim Approval Lifecycle As First-Class Record" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: An approval request can be created for a claim with requested amount, requesting adjuster identity, claim number and reserve reference, and is persisted in APPROVAL_T with status PENDING.
- Check acceptance criterion 2: A decision transition records decision code (approved or denied), rationale text, approver principal identity, approver authority limit at decision time and decision timestamp, and moves status to APPROVED or DENIED.
- Check acceptance criterion 3: An approval whose approver principal equals the requesting principal is rejected with a distinct segregation-of-duties reason code and never persisted in APPROVED state.

### REGRESSION-057 — Enforce Dual Payment Authority Check In Claims Domain

- User Story: WO-060
- Objective: Prevent regressions in existing behavior impacted by "Enforce Dual Payment Authority Check In Claims Domain".
- Expected: Story "Enforce Dual Payment Authority Check In Claims Domain" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A PaymentAuthorityService evaluates both checks in a single method and returns either a permit decision carrying approver identity and applied authority limit, or a denial carrying exactly one stable reason code.
- Check acceptance criterion 2: Given no qualifying approval, evaluation denies with reason code APPROVAL_REQUIRED, no CLAIM_PAYMENT_T row is written and the reserve remains at status AP.
- Check acceptance criterion 3: Given a qualifying approval but CLAIM_ADJUSTER_T.AUTHORITY_LIMIT less than PAID_TO_DATE plus payment amount, evaluation denies with the distinct reason code AUTHORITY_LIMIT_EXCEEDED.

### REGRESSION-058 — Expose Versioned Claims REST API With Deny-By-Default

- User Story: WO-066
- Objective: Prevent regressions in existing behavior impacted by "Expose Versioned Claims REST API With Deny-By-Default".
- Expected: Story "Expose Versioned Claims REST API With Deny-By-Default" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Endpoints exist and are documented in generated OpenAPI 3.1 for claim create and get, reserve set and adjust, approval request and decision, payment create, and claim payment list under the /v1/claims path family.
- Check acceptance criterion 2: Every mutating endpoint denies unauthenticated and unauthorized callers server-side: an integration test proves 401 without a token and 403 with a token lacking the required authority, and no endpoint relies on UI-level gating.
- Check acceptance criterion 3: POST payment returns 201 with the created payment on permit, and 403 with an RFC 9457 problem detail whose reason code distinguishes APPROVAL_REQUIRED from AUTHORITY_LIMIT_EXCEEDED on denial.

### REGRESSION-059 — Instrument Claims Payment Observability And SoD Evidence

- User Story: WO-081
- Objective: Prevent regressions in existing behavior impacted by "Instrument Claims Payment Observability And SoD Evidence".
- Expected: Story "Instrument Claims Payment Observability And SoD Evidence" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: All claims-svc and claims batch logs are emitted as structured JSON including actor, resource, operation, reason code, correlation id and job execution id, with no free-text-only error lines.
- Check acceptance criterion 2: A logging masking converter is active such that an automated log scan over the test suite output finds zero unmasked restricted-tier values (tax id, email, phone, payee name); the scan runs in CI and fails the build on any hit.
- Check acceptance criterion 3: OpenTelemetry traces link an incoming payment API request or batch step to the authority evaluation and the payment write, and the trace id appears in the corresponding log lines.

### REGRESSION-060 — Convert CLM006B Claim Payment Batch To Spring Batch

- User Story: WO-090
- Objective: Prevent regressions in existing behavior impacted by "Convert CLM006B Claim Payment Batch To Spring Batch".
- Expected: Story "Convert CLM006B Claim Payment Batch To Spring Batch" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A Spring Batch job reads payable reserves with a single set-based query (status AP, APPROVED_AMT greater than PAID_TO_DATE, joined to claim and adjuster) with no per-row round trips for derived values.
- Check acceptance criterion 2: Chunk size is one item per commit, configurable, so a single failing claim is skipped or recorded as an exception without blocking the remainder of the run, matching the legacy prologue commit scope.
- Check acceptance criterion 3: Every payment write passes through the shared PaymentAuthorityService: reserves without a qualifying approval or exceeding cumulative authority are not paid, produce an exception record with the distinct reason code, and leave the reserve at status AP.

### REGRESSION-061 — Build Claim Payment Golden-Output Parity Harness

- User Story: WO-097
- Objective: Prevent regressions in existing behavior impacted by "Build Claim Payment Golden-Output Parity Harness".
- Expected: Story "Build Claim Payment Golden-Output Parity Harness" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A deterministic seeded claims dataset is committed and loadable into both a Db2-shaped baseline fixture and PostgreSQL, covering payable reserves, missing approvals, overlimit adjusters, threshold breaches and zero-outstanding reserves.
- Check acceptance criterion 2: Golden output files capture expected CLAIM_PAYMENT_T rows, final CLAIM_RESERVE_T states, RECOVERY_T referrals and run-log counters derived from the CLM006B baseline semantics, and are committed under version control.
- Check acceptance criterion 3: A comparison engine asserts equality of every monetary field at scale 2 with zero tolerance and reports breaks as a structured list of claim number, table, field, expected and actual.

### REGRESSION-062 — Migrate billing and commission schema with set-based readers

- User Story: WO-056
- Objective: Prevent regressions in existing behavior impacted by "Migrate billing and commission schema with set-based readers".
- Expected: Story "Migrate billing and commission schema with set-based readers" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Flyway migrations create BILLING_PLAN_T, BILLING_SCHEDULE_T, INVOICE_T, AGENT_COMMISSION_T, COMMISSION_LEDGER_T and the batch run-log table on a clean PostgreSQL 17 container, with all monetary columns declared NUMERIC(9,2) or NUMERIC(11,2) and all business document keys as fixed-length VARCHAR/CHAR fed by SEQUENCE objects, never IDENTITY.
- Check acceptance criterion 2: A single billing candidate query returns pol_nbr, prem_annual, bill_freq, installment_cnt, last_installment_nbr, last_due_date, computed next_due_date and computed days_out, with zero additional per-row SQL statements verified by a query-count assertion in the test.
- Check acceptance criterion 3: A single delinquency candidate query returns bill_sched_id, pol_nbr, due_date, due_amt, paid_amt, bill_status and computed days_past_due for installments with status due or late and due_date on or before the run date.

### REGRESSION-063 — Convert BIL003B billing generation to Spring Batch job

- User Story: WO-070
- Objective: Prevent regressions in existing behavior impacted by "Convert BIL003B billing generation to Spring Batch job".
- Expected: Story "Convert BIL003B billing generation to Spring Batch job" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A Spring Batch job named billing-generation runs with the set-based candidate reader from WO-110, a pure-domain processor and a composite writer, committing exactly one policy per chunk so a single failure does not block the remaining population.
- Check acceptance criterion 2: Installment amount equals annual premium divided by installment count computed with BigDecimal divide at scale 2 and HALF_UP, and the generated rows match committed golden output for installment number, due date, amount and status for 100 percent of seeded rows.
- Check acceptance criterion 3: Next due date is computed as last due date plus 1 month for M, 3 months for Q, 6 months for S and 1 year for any other value, asserted for each frequency including the out-of-domain case.

### REGRESSION-064 — Convert PRM005B delinquency aging to Spring Batch job

- User Story: WO-071
- Objective: Prevent regressions in existing behavior impacted by "Convert PRM005B delinquency aging to Spring Batch job".
- Expected: Story "Convert PRM005B delinquency aging to Spring Batch job" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A Spring Batch job named delinquency-aging reads candidates through the WO-110 set-based query with days_past_due computed in SQL, and commits exactly one installment per chunk.
- Check acceptance criterion 2: Status transition logic matches the COBOL exactly: paid_amt greater than or equal to due_amt becomes paid; otherwise days_past_due greater than the configured grace period becomes late and increments the delinquency counter; otherwise the status remains due.
- Check acceptance criterion 3: An update is issued only when the newly computed status differs from the current status, and unchanged installments produce no write, no audit event and no run-log update count, matching legacy behaviour.

### REGRESSION-065 — Convert CMM001B commission calculation to Spring Batch job

- User Story: WO-072
- Objective: Prevent regressions in existing behavior impacted by "Convert CMM001B commission calculation to Spring Batch job".
- Expected: Story "Convert CMM001B commission calculation to Spring Batch job" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A Spring Batch job named commission-calculation reads paid installments with comm_calc_flag null joined to the policy agent and in-force plan through the WO-110 query, committing exactly one installment per chunk.
- Check acceptance criterion 2: Commission amount equals paid amount multiplied by rate divided by 100 computed with BigDecimal at scale 2 and HALF_UP, matching committed golden output to the cent for every seeded rate including four-decimal rates such as 12.3456.
- Check acceptance criterion 3: The commission ledger insert, the installment comm_calc_flag update to Y and the audit event commit as one transaction; an injected audit failure leaves no ledger row and no flag update.

### REGRESSION-066 — Gate billing batch cutover with parallel-run reconciliation

- User Story: WO-088
- Objective: Prevent regressions in existing behavior impacted by "Gate billing batch cutover with parallel-run reconciliation".
- Expected: Story "Gate billing batch cutover with parallel-run reconciliation" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A reconciliation job compares the PostgreSQL billing schedule, invoice and commission ledger state against the Db2 for i baseline extract for a given run date, matching on business key and asserting installment number, due date, amount, status, commission rate and commission amount to the cent at scale 2.
- Check acceptance criterion 2: Reconciliation output is a persisted, queryable break report with one row per break carrying break type, business key, expected value, actual value and a severity, and the job exits non-zero when unexplained breaks exceed the configured threshold of zero.
- Check acceptance criterion 3: The CI pipeline runs golden-output regression for billing generation, delinquency aging and commission calculation on every commit and fails the build on any mismatch in amount, count, status or commit boundary.

### REGRESSION-067 — Expose billing-svc REST API and batch operations endpoints

- User Story: WO-089
- Objective: Prevent regressions in existing behavior impacted by "Expose billing-svc REST API and batch operations endpoints".
- Expected: Story "Expose billing-svc REST API and batch operations endpoints" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A versioned OpenAPI 3.1 document is generated from code and published, covering GET billing schedules by policy, GET invoices by policy, GET aging summary, GET commission ledger by agent, GET batch runs, GET batch run exceptions and POST trigger endpoints for the three jobs.
- Check acceptance criterion 2: All endpoints require a validated bearer JWT; unauthenticated requests return 401 and requests lacking the required authority return 403 with a distinct machine-readable reason code, with deny-by-default enforced by method-level authorization on every handler.
- Check acceptance criterion 3: Every error response is an RFC 9457 problem detail containing type, title, status, detail and a correlation identifier, and no response body or log line contains a stack trace, SQL text or unmasked restricted-tier value.

### REGRESSION-068 — Freeze versioned v1 premium rating OpenAPI contract

- User Story: WO-028
- Objective: Prevent regressions in existing behavior impacted by "Freeze versioned v1 premium rating OpenAPI contract".
- Expected: Story "Freeze versioned v1 premium rating OpenAPI contract" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A file premium-rating-v1.yaml exists in the api contracts directory, is a valid OpenAPI 3.1 document, and passes an OpenAPI linter with zero errors in the build.
- Check acceptance criterion 2: Every input field from both evidenced PRMCLC01 parameter lists (policy type, coverage type, territory, limit, state, old premium) and every output field (premium, base rate, rating factor, return code, underwriting decision) has a corresponding contract field documented in the data dictionary with source COBOL PIC clause, target JSON type, decimal scale and rounding mode.
- Check acceptance criterion 3: The response schema exposes the full breakdown required by the policy issuance UI: composite risk score, risk tier, base rate, ordered factor list, discount list, surcharge list, tax list, final premium, and calculation snapshot identifier.

### REGRESSION-069 — Scaffold premium-svc service and rating data access layer

- User Story: WO-045
- Objective: Prevent regressions in existing behavior impacted by "Scaffold premium-svc service and rating data access layer".
- Expected: Story "Scaffold premium-svc service and rating data access layer" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A premium-svc Maven module builds from a clean checkout with a single command and produces a runnable Spring Boot 3.5.x application on Java 21 with no manual environment setup steps.
- Check acceptance criterion 2: Flyway migrations create all rating read tables and write tables with monetary columns as NUMERIC(9,2) or NUMERIC(11,2), business document keys as fixed-length VARCHAR or CHAR fed by SEQUENCE objects, child surrogate keys as BIGINT GENERATED ALWAYS AS IDENTITY, and the standard CRT_USER, CRT_TIMESTAMP, UPD_USER, UPD_TIMESTAMP columns on every table.
- Check acceptance criterion 3: All contract paths from WO-120 are wired as controllers that return an RFC 9457 problem detail with reason code PRM_NOT_IMPLEMENTED until WO-122 lands; no endpoint returns a stack trace.

### REGRESSION-070 — Implement rating engine with exact decimal arithmetic

- User Story: WO-062
- Objective: Prevent regressions in existing behavior impacted by "Implement rating engine with exact decimal arithmetic".
- Expected: Story "Implement rating engine with exact decimal arithmetic" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: The rating use case executes the documented PRMCLC01 sequence in order: input validation, composite risk score and tier, base rate and factor lookup with base premium equal to base rate multiplied by rating factor, discounts, surcharges, taxes, final premium — and the order is asserted by a test.
- Check acceptance criterion 2: All monetary arithmetic uses BigDecimal with RoundingMode.HALF_UP and rounds to two decimal places at each stage documented in PRM_Premium_Calculation_Engine_Design.md; no double, float or unscaled division appears anywhere in the domain package, enforced by an automated check.
- Check acceptance criterion 3: The domain package contains no framework imports and is unit testable without a database or HTTP layer, enforced by an architecture test.

### REGRESSION-071 — Build golden-output parity harness for premium rating

- User Story: WO-069
- Objective: Prevent regressions in existing behavior impacted by "Build golden-output parity harness for premium rating".
- Expected: Story "Build golden-output parity harness for premium rating" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A scenario matrix is defined and committed covering at minimum: homeowners and commercial policy types, each documented billing and rating path, both evidenced PRMCLC01 caller shapes, discount-only, surcharge-only, tax-inclusive, zero-discount, maximum-factor, half-cent rounding boundary, NUMERIC ceiling overflow, underwriting decline, underwriting referral, and missing reference data.
- Check acceptance criterion 2: Golden output files are committed in a stable machine-readable format (one file per scenario with all breakdown fields as exact decimal strings) and are traceable to the algorithm sections of PRM_Premium_Calculation_Engine_Design.md that define them.
- Check acceptance criterion 3: Database seeding is deterministic and versioned: running the harness twice from a clean state produces byte-identical outputs, and the seed is expressed as versioned migration or fixture scripts rather than ad hoc inserts.

### REGRESSION-072 — Add underwriting rule evaluation and referral outcome tracking

- User Story: WO-076
- Objective: Prevent regressions in existing behavior impacted by "Add underwriting rule evaluation and referral outcome tracking".
- Expected: Story "Add underwriting rule evaluation and referral outcome tracking" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Underwriting rules are evaluated from UW_RULE_T after risk scoring and before base rate lookup, matching the documented PRMCLC01 step order, and the ordering is asserted by a test.
- Check acceptance criterion 2: A DECLINE outcome returns HTTP 200 with return code 02, underwriting decision DECLINE, the matched rule identifier and reason text, no premium value, and exactly zero rows written to PREMIUM_CALC_T and PREMIUM_CALC_DETAIL_T.
- Check acceptance criterion 3: A DECLINE outcome writes exactly one audit record capturing actor, resource, operation, matched rule and decision, committed in the same transaction as the decision handling.

### REGRESSION-073 — Enforce rating contract with consumer-driven tests and CI gate

- User Story: WO-077
- Objective: Prevent regressions in existing behavior impacted by "Enforce rating contract with consumer-driven tests and CI gate".
- Expected: Story "Enforce rating contract with consumer-driven tests and CI gate" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Consumer-driven contract definitions exist for at least four consumers of the rating contract (policy issuance, policy endorsement or renewal batch, quote, billing) covering approve, refer, decline and caller-input-error interactions.
- Check acceptance criterion 2: Provider verification runs against premium-svc in the build and fails if any consumer expectation is unsatisfied, with the failing consumer and interaction named in the output.
- Check acceptance criterion 3: An OpenAPI diff gate compares the pull request contract against the last released contract and fails the build on any breaking change (field removal, type narrowing, required constraint added, enum value removed, path removal), naming the offending element.

### REGRESSION-074 — Migrate Policy Domain Schema to PostgreSQL with Flyway

- User Story: WO-086
- Objective: Prevent regressions in existing behavior impacted by "Migrate Policy Domain Schema to PostgreSQL with Flyway".
- Expected: Story "Migrate Policy Domain Schema to PostgreSQL with Flyway" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Flyway migrations under the policy-svc module create POLICY_T, COVERAGE_T, DEDUCTIBLE_T, POLICY_HISTORY_T, POLICY_PROPERTY_T, POLICY_VEHICLE_T, ENDORSEMENT_T and BILLING_PLAN_T references with all four standard audit columns (CRT_USER, CRT_TIMESTAMP, UPD_USER, UPD_TIMESTAMP) present on every table.
- Check acceptance criterion 2: POL_NBR is CHAR/VARCHAR(12) populated from a PostgreSQL SEQUENCE via a documented formatter, never IDENTITY; DEDUCT_ID, POL_PROP_ID and POL_VEH_ID are BIGINT GENERATED ALWAYS AS IDENTITY, and the resolution of the design-document conflict is recorded as a comment in the migration and in the module README.
- Check acceptance criterion 3: All monetary columns are NUMERIC(9,2) or NUMERIC(11,2) with a test asserting a round-trip of the boundary values 999999999.99 and 99999999999.99 through JDBC BigDecimal loses no precision and applies no implicit rounding.

### REGRESSION-075 — Build policy-svc REST API with Versioned Read Contract

- User Story: WO-092
- Objective: Prevent regressions in existing behavior impacted by "Build policy-svc REST API with Versioned Read Contract".
- Expected: Story "Build policy-svc REST API with Versioned Read Contract" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: GET /v1/policies/{polNbr} returns the published policy projection including POL_TYPE, CUST_ID, AGT_ID, EFF_DATE, EXP_DATE, POL_STATUS, PREM_ANNUAL as a string-serialized exact decimal, and coverage lines; 404 with an RFC 9457 problem detail is returned for an unknown policy number.
- Check acceptance criterion 2: Every mutating endpoint is annotated with a method-level authorization check and an integration test proves an authenticated principal without the required authority receives 403 with a distinct problem-detail reason code, while an unauthenticated request receives 401.
- Check acceptance criterion 3: Policy creation and endorsement persist the entity change and an audit outbox record in one transaction; an injected outbox-write failure rolls back the policy mutation and the endpoint returns 500 with no partial row, proving the legacy continue-after-audit-failure behaviour is not reproduced.

### REGRESSION-076 — Convert POL006B Renewal Batch to Restartable Spring Batch Job

- User Story: WO-093
- Objective: Prevent regressions in existing behavior impacted by "Convert POL006B Renewal Batch to Restartable Spring Batch Job".
- Expected: Story "Convert POL006B Renewal Batch to Restartable Spring Batch Job" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A Spring Batch job named policyRenewalJob reads renewal candidates in a single set-based query that computes days-to-expiry and eligibility server-side, with zero per-row date-arithmetic round trips verified by a query-count assertion in the integration test.
- Check acceptance criterion 2: The renewal window (default 60 days), rating call timeout, error-count threshold and chunk size are supplied via externalized configuration properties with validation, and changing the renewal window in configuration alters candidate selection with no code change or redeploy of the image.
- Check acceptance criterion 3: Chunk size is one item per commit by default; a failure on one policy rolls back only that policy, increments a skip/error counter, records a structured exception with actor, resource and operation, and the job continues processing the remaining population.

### REGRESSION-077 — Golden-Output Parity Harness for Renewal Batch Reconciliation

- User Story: WO-094
- Objective: Prevent regressions in existing behavior impacted by "Golden-Output Parity Harness for Renewal Batch Reconciliation".
- Expected: Story "Golden-Output Parity Harness for Renewal Batch Reconciliation" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A committed golden dataset and expected-output snapshot exist for the renewal job covering policies inside, on and outside the renewal window, all policy types, rating-failure cases and zero-candidate runs.
- Check acceptance criterion 2: The comparator asserts equality of new term effective/expiration dates, POL_STATUS transitions, PREM_ANNUAL to the cent at NUMERIC(9,2), coverage and deductible carry-forward row counts and values, POLICY_HISTORY_T event types and ordering, and RPT_RUN_LOG_T selected/updated/error counts; any single-field mismatch fails the build with a readable diff.
- Check acceptance criterion 3: The harness runs in CI on every commit against a Testcontainers PostgreSQL instance in under the agreed pipeline budget and publishes a coverage report showing at least 90 percent line coverage on renewal monetary calculation packages.

### REGRESSION-078 — Renewal Exception Reporting, Metrics and Alerting Runbook

- User Story: WO-098
- Objective: Prevent regressions in existing behavior impacted by "Renewal Exception Reporting, Metrics and Alerting Runbook".
- Expected: Story "Renewal Exception Reporting, Metrics and Alerting Runbook" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Every renewal failure or intentional skip writes a persisted exception record with policy number, reason code, batch job execution id, correlation id, actor identity, resource and operation, and no free-text-only console output is relied upon.
- Check acceptance criterion 2: A read-only endpoint exposes renewal exceptions and run-log summaries with paging and filtering by run date, reason code and policy number, secured deny-by-default and returning RFC 9457 problem details on error.
- Check acceptance criterion 3: Micrometer metrics are published for job duration, items read, written, skipped and errored, exit code, and premium-svc rating call latency and failure rate; a Grafana dashboard definition and Prometheus alert rules are committed as code.

### REGRESSION-079 — Originate Renewal Billing Schedule via Policy Domain Events

- User Story: WO-100
- Objective: Prevent regressions in existing behavior impacted by "Originate Renewal Billing Schedule via Policy Domain Events".
- Expected: Story "Originate Renewal Billing Schedule via Policy Domain Events" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A PolicyRenewed event carrying policy number, new term effective and expiration dates, annual premium as an exact decimal, billing frequency and a correlation identifier is written to the outbox in the same transaction as the renewal writes, and published by the relay after commit.
- Check acceptance criterion 2: billing-svc consumes the event idempotently: replaying the same event produces exactly one billing plan and one first installment for the new term, verified by a duplicate-delivery integration test.
- Check acceptance criterion 3: After a renewal run with the correction enabled, every renewed policy is selectable by the billing generation candidate query, proven by an integration test that runs renewal then billing generation and asserts a non-zero installment count for each renewed term.

### REGRESSION-080 — Provision PostgreSQL Read Replica With Lag SLO

- User Story: WO-040
- Objective: Prevent regressions in existing behavior impacted by "Provision PostgreSQL Read Replica With Lag SLO".
- Expected: Story "Provision PostgreSQL Read Replica With Lag SLO" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A Terraform module creates a PostgreSQL 17 streaming read replica per environment with parameters hot_standby_feedback enabled, statement_timeout and idle_in_transaction_session_timeout set for reporting workloads, and terraform plan is clean and idempotent on a second run.
- Check acceptance criterion 2: Replica endpoint and read-only credentials are read from the managed secret store at runtime; no connection string, host or password literal appears in any committed file or Helm values file (verified by the gitleaks scan step in the pipeline).
- Check acceptance criterion 3: A dedicated read-only datasource bean is registered and any INSERT, UPDATE or DELETE attempted through it is rejected, producing a structured log line with actor, resource and operation context and never being retried against the primary.

### REGRESSION-081 — Build reporting-svc Replica-Backed Report APIs

- User Story: WO-059
- Objective: Prevent regressions in existing behavior impacted by "Build reporting-svc Replica-Backed Report APIs".
- Expected: Story "Build reporting-svc Replica-Backed Report APIs" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Versioned endpoints exist under /v1/reports for regulatory extracts, management metrics, claims loss triangle, renewal and retention, billing aging and batch exceptions, plus /v1/batch-runs projecting RPT_RUN_LOG_T, all documented in a generated OpenAPI 3.1 contract committed to the repository.
- Check acceptance criterion 2: Every reporting endpoint is deny-by-default: an unauthenticated request returns 401, a request without the reporting or compliance authority returns 403, and both outcomes emit a structured authorization-denied event with actor, resource and operation.
- Check acceptance criterion 3: All reporting queries execute against the read replica datasource; an integration test asserts zero connections are opened against the primary datasource during a full sweep of every endpoint.

### REGRESSION-082 — Document Storage And Notification Integration Layer

- User Story: WO-074
- Objective: Prevent regressions in existing behavior impacted by "Document Storage And Notification Integration Layer".
- Expected: Story "Document Storage And Notification Integration Layer" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A document upload endpoint accepts a file with declared content type and size, rejects types outside a committed allow-list and sizes above the configured maximum with an RFC 9457 problem detail, and stores accepted bytes in object storage with server-side encryption.
- Check acceptance criterion 2: Document metadata and the corresponding outbox event are written inside one database transaction; a forced failure of the object-storage put leaves no metadata row and no outbox event, proven by an integration test.
- Check acceptance criterion 3: Retrieval is exclusively via short-lived presigned URLs; no endpoint streams stored bytes through the service and no permanent public URL is ever generated, verified by test and by absence of any public-read bucket policy in Terraform.

### REGRESSION-083 — Automated Legacy Decommission Readiness Gate

- User Story: WO-075
- Objective: Prevent regressions in existing behavior impacted by "Automated Legacy Decommission Readiness Gate".
- Expected: Story "Automated Legacy Decommission Readiness Gate" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A committed decommission manifest enumerates every legacy member — the 8 COBOL programs, 22 DDS display files and 2 CL members — with fields for replacement artifact, owning service, parity evidence reference, reconciliation status and decommission state.
- Check acceptance criterion 2: An inventory parser regenerates the legacy member list directly from the repository tree and fails the build if the manifest is missing an entry, contains an entry with no corresponding source member, or leaves any replacement field empty.
- Check acceptance criterion 3: A drain checker queries the batch-run projection and fails when any legacy program name (AUD002B, BIL003B, CMM001B, PRM005B, POL006B, CLM006B) has a run row inside the configured quiet-period window, with the window value externalized as configuration.

### REGRESSION-084 — Execute Legacy IBM i Decommission And Archive

- User Story: WO-101
- Objective: Prevent regressions in existing behavior impacted by "Execute Legacy IBM i Decommission And Archive".
- Expected: Story "Execute Legacy IBM i Decommission And Archive" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: All scheduler entries submitting legacy batch work (JOBSCHD1 nightly, JOBSCHD2 nightly renewal, JOBSCHD3 monthly billing and commission) are held or removed, and an attempted submission is proven to fail; evidence is captured in the decommission evidence pack.
- Check acceptance criterion 2: Db2 for i is placed in a documented read-only posture for the legacy libraries and INSPRDDTA write authority is revoked from all application user profiles, with before-and-after authority listings attached as evidence.
- Check acceptance criterion 3: A final save of INSPRD, INSPRDDTA, INSCOM and INSTOOLS is written to object storage with Object Lock enabled and a lifecycle policy matching the agreed regulatory retention (minimum six years for insurance policy records and one year minimum for audit), and the archive integrity is verified by checksum.

### REGRESSION-085 — Accessible SPA Shell, Routing and API Client Foundation

- User Story: WO-015
- Objective: Prevent regressions in existing behavior impacted by "Accessible SPA Shell, Routing and API Client Foundation".
- Expected: Story "Accessible SPA Shell, Routing and API Client Foundation" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A new web/ workspace exists with React 19, TypeScript in strict mode, Vite build, ESLint and Prettier, and npm scripts for dev, build, lint, test and typecheck; build completes with zero TypeScript errors and no use of the any type.
- Check acceptance criterion 2: AppShell renders semantic landmarks (header, nav, main, footer), a working skip-to-main-content link as the first focusable element, an accessible sidebar with module groups (Customer, Policy, Premium, Billing, Claims, Reporting, Admin), TopBar with breadcrumbs and a theme toggle persisted to localStorage.
- Check acceptance criterion 3: Client-side routing is configured with a route registry, lazy-loaded route modules, a 404 route, an error boundary that renders a recoverable error panel, and route changes announce the new page title to assistive technology via a live region.

### REGRESSION-086 — Customer Workspace Replacing CUS Green-Screen Panels

- User Story: WO-061
- Objective: Prevent regressions in existing behavior impacted by "Customer Workspace Replacing CUS Green-Screen Panels".
- Expected: Story "Customer Workspace Replacing CUS Green-Screen Panels" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A customer search screen replaces CUSLSTD1 with accessible filters (name, tax ID last four, customer id, status), paginated results in the shared DataTable, keyboard row activation opening the customer view, and an accessible no-results state.
- Check acceptance criterion 2: A single customer view replaces CUSINQD1 and aggregates identity, addresses, contacts, policies, billing summary and claim history in tabbed regions, each loading independently with its own skeleton and error state so one failing panel does not blank the page.
- Check acceptance criterion 3: Create and maintain forms replace CUSMNTD1 with all evidenced fields (customer type, name, date of birth, tax ID, gender, marital status, status, credit score, primary address, primary contact) and enforce the evidenced mandatory-field rules with field-anchored plain-language messages instead of coded messages.

### REGRESSION-087 — Billing Invoice, Payment and Delinquency Screens

- User Story: WO-079
- Objective: Prevent regressions in existing behavior impacted by "Billing Invoice, Payment and Delinquency Screens".
- Expected: Story "Billing Invoice, Payment and Delinquency Screens" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A billing schedule and invoice view replaces BILINVD1 and BILINQD1, listing installments with installment number, due date, amount, status and paid amount, plus per-installment status history, using decimal-safe money display that matches DECIMAL(9,2) values exactly.
- Check acceptance criterion 2: An aging and delinquency worklist lists installments that are due or late with days past due, grace-period remaining, delinquency counter and policy context, filterable and sortable, with accessible empty and large-result states.
- Check acceptance criterion 3: A payment application screen replaces BILPMTD1, allowing a payment to be applied to one or more installments with a running unapplied balance, blocking over-application with a field-anchored message, and handling only gateway tokens and last-four card values — never raw cardholder data.

### REGRESSION-088 — Policy Issuance Screen With Premium Rating Breakdown

- User Story: WO-082
- Objective: Prevent regressions in existing behavior impacted by "Policy Issuance Screen With Premium Rating Breakdown".
- Expected: Story "Policy Issuance Screen With Premium Rating Breakdown" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A mode-aware policy workspace supports CREATE, ENDORSE and INQUIRY, applying the field-level protected versus input-capable behaviour documented in POL_Module_Design_Document.md section 5.1, with mode clearly announced to assistive technology.
- Check acceptance criterion 2: Customer and agent lookup components validate existence and active status through the API and render inactive or missing entities as blocking field-anchored errors rather than generic messages.
- Check acceptance criterion 3: The coverage editor replaces the POLMNTD1 subfile with an accessible table supporting add, change and remove in ENDORSE mode only, limit and deductible entry with decimal-safe money fields, and mandatory coverages rendered locked with an accessible explanation that they cannot be removed.

### REGRESSION-089 — OIDC Sign-In and Role-Based UI Gating

- User Story: WO-087
- Objective: Prevent regressions in existing behavior impacted by "OIDC Sign-In and Role-Based UI Gating".
- Expected: Story "OIDC Sign-In and Role-Based UI Gating" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: An unauthenticated user visiting any protected route is redirected to the identity provider using Authorization Code with PKCE, and after successful sign-in is returned to the originally requested route.
- Check acceptance criterion 2: The session is carried in an httpOnly, Secure, SameSite=Strict cookie plus a bearer token attached by the API client; no access or refresh token is ever written to localStorage or sessionStorage, verified by an automated test asserting storage is empty after sign-in.
- Check acceptance criterion 3: Access token expiry triggers a silent refresh; a failed refresh clears client state, redirects to sign-in and preserves the intended route. Concurrent 401 responses trigger exactly one refresh attempt (single-flight), not one per request.

### REGRESSION-090 — WCAG 2.1 AA Conformance Test Harness and CI Gate

- User Story: WO-095
- Objective: Prevent regressions in existing behavior impacted by "WCAG 2.1 AA Conformance Test Harness and CI Gate".
- Expected: Story "WCAG 2.1 AA Conformance Test Harness and CI Gate" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Automated axe-core accessibility assertions run against every registered SPA route and every component gallery state, with zero serious or critical violations; the run fails the build on any new violation and publishes a machine-readable JSON report artifact per build.
- Check acceptance criterion 2: Keyboard-only traversal tests verify for each primary workflow (customer create, policy issuance, claim FNOL, payment request, approval decision, payment application) that every interactive control is reachable by Tab, that no focus trap exists outside modals, that Escape closes dialogs and returns focus to the invoker, and that a visible focus indicator is present on every focusable element.
- Check acceptance criterion 3: A contrast audit covers both design tokens and rendered pages, asserting 4.5:1 for normal text, 3:1 for large text and non-text UI components, and fails with the offending selector and computed ratio.

### REGRESSION-091 — Claims Workspace With Approval and Payment Controls

- User Story: WO-096
- Objective: Prevent regressions in existing behavior impacted by "Claims Workspace With Approval and Payment Controls".
- Expected: Story "Claims Workspace With Approval and Payment Controls" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: An FNOL intake screen replaces CLMFNLD1 with policy lookup and coverage validation, loss date and description, late-reporting indication, and multi-file document and photo attachment with progress, retry, type and size validation and accessible status announcements.
- Check acceptance criterion 2: A claim workspace replaces CLMUPDD1 and CLMINQD1 by combining claim header, append-only reserve history, payment list, notes and documents in one keyboard-navigable view where reserve history rows are visibly non-editable.
- Check acceptance criterion 3: The payment request screen shows, before submission, the remaining reserve (approved amount minus paid to date) and the requesting adjuster's remaining authority headroom computed on cumulative claim payout, so an over-limit request is visible in advance rather than after submission.

### REGRESSION-092 — Accessible Component Library Replacing DDS Subfiles

- User Story: WO-099
- Objective: Prevent regressions in existing behavior impacted by "Accessible Component Library Replacing DDS Subfiles".
- Expected: Story "Accessible Component Library Replacing DDS Subfiles" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: DataTable renders native table semantics with caption, column headers with scope, aria-sort on sortable columns, sticky header, dense mode, row-level action menu reachable by keyboard, full keyboard row navigation, and an accessible empty state and loading state.
- Check acceptance criterion 2: Form controls (TextField, Select, Checkbox, Radio, Toggle, DateField, MoneyField) each associate a visible label, optional description and error message via aria-describedby and aria-invalid; validation errors render adjacent to the offending field and are announced once via a live region, replacing the DDS bottom message line pattern.
- Check acceptance criterion 3: MoneyField and money display components format and parse decimal values without floating-point loss, preserving two-decimal scale so amounts round-trip exactly to the DECIMAL(9,2) and DECIMAL(11,2) backend types.

### REGRESSION-093 — Deploy API Gateway With TLS, Routing and Rate Limiting

- User Story: WO-016
- Objective: Prevent regressions in existing behavior impacted by "Deploy API Gateway With TLS, Routing and Rate Limiting".
- Expected: Story "Deploy API Gateway With TLS, Routing and Rate Limiting" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A gateway module (Spring Boot 3.5.x, Java 21, Spring Cloud Gateway) builds from the repository root Maven build and starts with an externalized route configuration listing /v1/customers, /v1/policies, /v1/claims, /v1/premium, /v1/billing, /v1/reports, /v1/authz and /v1/audit routes.
- Check acceptance criterion 2: TLS 1.3 is enforced at the edge; a connection attempt negotiating TLS 1.1 or 1.2 is rejected, and responses carry Strict-Transport-Security, Content-Security-Policy, X-Content-Type-Options nosniff, X-Frame-Options DENY and Referrer-Policy headers verified by an automated header assertion test.
- Check acceptance criterion 3: Rate limiting is enforced at 100 requests per minute per principal key (falling back to client IP when no principal is present) using a Redis-backed token bucket; the 101st request in a window returns HTTP 429 with an RFC 9457 problem detail containing a retry-after hint and no stack trace.

### REGRESSION-094 — Enforce Zone Segmentation, mTLS and Egress Allow-Lists

- User Story: WO-034
- Objective: Prevent regressions in existing behavior impacted by "Enforce Zone Segmentation, mTLS and Egress Allow-Lists".
- Expected: Story "Enforce Zone Segmentation, mTLS and Egress Allow-Lists" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A default-deny NetworkPolicy exists for every namespace covering both ingress and egress, and a committed policy test proves that a pod with no explicit allow rule cannot reach any other pod, the database, or the internet.
- Check acceptance criterion 2: Per-service NetworkPolicies allow only the documented conduits: gateway to the eight services, services to their own database and to the message broker, batch jobs to database and broker, reporting to the read replica only and never to the OLTP primary — each conduit asserted by an automated test.
- Check acceptance criterion 3: Mutual TLS is enforced for all internal pod-to-pod traffic (strict mode), verified by a test showing a plaintext connection attempt between two pods is refused and by inspecting the negotiated peer identity.

### REGRESSION-095 — Implement OIDC Federation and JWT Validation Enforcement

- User Story: WO-050
- Objective: Prevent regressions in existing behavior impacted by "Implement OIDC Federation and JWT Validation Enforcement".
- Expected: Story "Implement OIDC Federation and JWT Validation Enforcement" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Gateway is configured as an OAuth2 client performing Authorization Code with PKCE against the configured OIDC provider; login redirects, callback handling and RP-initiated logout all work against a containerised test identity provider.
- Check acceptance criterion 2: Successful login sets an httpOnly, Secure, SameSite=Strict session cookie; an automated test asserts no access or refresh token value ever appears in a response body, in a Set-Cookie without httpOnly, or in any log line.
- Check acceptance criterion 3: Access tokens are validated locally with RS256 against a JWKS cache with a 1 hour TTL and background refresh; validation adds no more than 5 ms at p99 and performs no network hop to the identity provider on the happy path, proven by a benchmark test and by asserting zero JWKS requests during a steady-state load run.

### REGRESSION-096 — Federate Workload Identity For Batch and Service Calls

- User Story: WO-052
- Objective: Prevent regressions in existing behavior impacted by "Federate Workload Identity For Batch and Service Calls".
- Expected: Story "Federate Workload Identity For Batch and Service Calls" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: Each batch job and each domain service is registered as a distinct OAuth2 client-credentials principal with a least-privilege scope set, and a committed manifest maps workload name to client identifier, scopes and the operations it may perform.
- Check acceptance criterion 2: A MachineTokenProvider in the shared security starter obtains client-credentials tokens by exchanging the projected Kubernetes service account token (workload identity federation) so no long-lived client secret is stored in a container image or manifest; any required secret is referenced as a placeholder resolved from the managed secret store.
- Check acceptance criterion 3: Tokens are cached in memory and refreshed proactively before expiry with jittered retry; a test asserts that N sequential outbound calls within a token lifetime trigger exactly one token request, and that refresh failure causes the caller to fail closed rather than proceed unauthenticated.

### REGRESSION-097 — Machine-readable open design item decision register with CI gate

- User Story: WO-008
- Objective: Prevent regressions in existing behavior impacted by "Machine-readable open design item decision register with CI gate".
- Expected: Story "Machine-readable open design item decision register with CI gate" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: governance/open-design-items.yaml exists and contains exactly the twelve items enumerated in PCIS_Enterprise_Architecture.md section 7.4, each with id, title, evidence_file, evidence_section, owning_phase, decision_owner_role, status, decision_text and implemented_by fields.
- Check acceptance criterion 2: A JSON Schema (governance/schema/open-design-items.schema.json) validates the register; the validator exits non-zero with a field-level message when a required field is missing, an unknown status value is used, or an evidence_file path does not exist in the repository.
- Check acceptance criterion 3: Running the validator with a phase argument (for example --phase=CLAIMS) exits non-zero and lists offending item ids when any item whose owning_phase equals that phase has status OPEN; it exits zero when all such items are DECIDED or CONFIGURATION_DRIVEN.

### REGRESSION-098 — Parallel-run reconciliation engine with per-domain cutover gate scoring

- User Story: WO-022
- Objective: Prevent regressions in existing behavior impacted by "Parallel-run reconciliation engine with per-domain cutover gate scoring".
- Expected: Story "Parallel-run reconciliation engine with per-domain cutover gate scoring" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A reconciliation job can be launched per domain (customer, policy, billing, premium, claims, reporting) with a business-date parameter, and completes with a persisted RECON_RUN row recording start, end, entity count, compared row count and break counts by classification.
- Check acceptance criterion 2: All monetary comparisons use BigDecimal with scale 2 and exact equality (no epsilon tolerance); a one-cent difference on any of PREM_ANNUAL, DUE_AMT, PAID_AMT, COMMISSION_AMT, APPROVED_AMT, PAID_TO_DATE or payment amount produces a VALUE_MISMATCH break naming entity, business key, column, legacy value and target value.
- Check acceptance criterion 3: Break records are persisted in RECON_BREAK with classification, entity, business key, column, legacy value, target value, first-seen and last-seen timestamps, and an optional approved_decision_id linking to a WO-170 register item so approved behaviour changes are excluded from the unexplained-break count.

### REGRESSION-099 — Per-domain cutover control plane with audited rollback switches

- User Story: WO-023
- Objective: Prevent regressions in existing behavior impacted by "Per-domain cutover control plane with audited rollback switches".
- Expected: Story "Per-domain cutover control plane with audited rollback switches" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A cutover state store holds one row per domain with state, previous_state, changed_by, changed_at, reason and linked gate verdict snapshot; states are constrained to LEGACY_ONLY, SHADOW_WRITE, PARALLEL_RUN, TARGET_PRIMARY, TARGET_ONLY.
- Check acceptance criterion 2: State changes are performed only through an authenticated, deny-by-default endpoint requiring a cutover-operator permission; every change writes an immutable audit event with actor, resource (domain), operation, old and new state and reason, and the audit write is in the same transaction as the state change.
- Check acceptance criterion 3: Promotion to TARGET_PRIMARY is rejected with a distinct reason code when the WO-171 gate verdict for that domain is FAIL or the minimum parallel-run window has not elapsed; the rejection is audited.

### REGRESSION-100 — Automated phase-gate evidence pack generation and governance dashboard API

- User Story: WO-041
- Objective: Prevent regressions in existing behavior impacted by "Automated phase-gate evidence pack generation and governance dashboard API".
- Expected: Story "Automated phase-gate evidence pack generation and governance dashboard API" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: A gate criteria definition file declares every mandatory and advisory criterion with id, description, source type, threshold, comparison operator and applicable phases, covering at minimum monetary-logic line coverage at or above ninety percent, zero unexplained reconciliation breaks over the configured window, one hundred percent of claim payment paths passing an authority check, fifty-five of fifty-five tables classified, purge completion within twenty-four hours, six of six tunables externalized, commit blast radius at or below one thousand rows for the archive job, and batch restart with zero duplicate or orphaned financial records.
- Check acceptance criterion 2: The generator collects each criterion value from its declared source (JaCoCo XML, reconciliation gate endpoint or exported JSON, decision register report, purge run-log export, authorization test result file, fault-injection test result file) and records the source artifact path or URL plus a content hash for traceability.
- Check acceptance criterion 3: The generator writes a JSON manifest and a markdown evidence pack under target/governance/gate-pack/{phase}/ containing every criterion with measured value, threshold, verdict and source reference, plus the overall phase verdict and generation timestamp.

### REGRESSION-101 — Legacy behaviour decision records with preserve-versus-change parity matrix

- User Story: WO-073
- Objective: Prevent regressions in existing behavior impacted by "Legacy behaviour decision records with preserve-versus-change parity matrix".
- Expected: Story "Legacy behaviour decision records with preserve-versus-change parity matrix" satisfies expected regression validation outcomes without critical issues.

**Steps**
- Check acceptance criterion 1: governance/behaviour-decisions.yaml catalogues at minimum the following evidenced behaviours, each with a unique id: BIL003B silent skip of candidates outside the lead window while counting them eligible; the audit-write-failure continue path in BIL003B, CMM001B, PRM005B, POL006B and CLM006B; CLM006B full-outstanding payment computation; CLM006B informational-only reinsurance cession flag; CLM006B absence of any authority check; BIL003B reuse of HV-INSTALLMENT-NBR as a days-out counter; CMM001B no-in-force-plan counter path; CMM001B COMM_CALC_FLAG idempotency guard; PRM005B grace-period status transitions; AUD002B halt-on-verification-mismatch and archive-verify-then-delete ordering; per-item commit granularity stated in each program prologue.
- Check acceptance criterion 2: Each entry carries evidence_program, evidence_paragraph, evidence_excerpt, decision (PRESERVE or CHANGE), rationale, decision_owner_role, approved_by, approved_on, linked_open_design_item (optional) and test_ref pointing at a test class and method.
- Check acceptance criterion 3: A validator command fails the build when any entry lacks evidence_program or test_ref, when a CHANGE entry lacks approved_by and approved_on, when evidence_program does not name a file present in the repository, or when the referenced test class or method cannot be located in the source tree.

---

## Performance test scenarios (101)

### PERFORMANCE-001 — Repository Member Manifest and Completeness Gate

- User Story: WO-001
- Objective: Assess performance and stability impact introduced by "Repository Member Manifest and Completeness Gate".
- Expected: Story "Repository Member Manifest and Completeness Gate" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-001.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-002 — Extract Legacy Behavioural Baseline Specification Artifact

- User Story: WO-002
- Objective: Assess performance and stability impact introduced by "Extract Legacy Behavioural Baseline Specification Artifact".
- Expected: Story "Extract Legacy Behavioural Baseline Specification Artifact" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-002.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-003 — Scripted Dependency-Ordered Legacy COBOL Build

- User Story: WO-009
- Objective: Assess performance and stability impact introduced by "Scripted Dependency-Ordered Legacy COBOL Build".
- Expected: Story "Scripted Dependency-Ordered Legacy COBOL Build" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-009.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-004 — CI Pipeline Gating Manifest, Baseline and Build

- User Story: WO-024
- Objective: Assess performance and stability impact introduced by "CI Pipeline Gating Manifest, Baseline and Build".
- Expected: Story "CI Pipeline Gating Manifest, Baseline and Build" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-024.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-005 — Coexistence Topology, Scheduler Map and Runbook

- User Story: WO-025
- Objective: Assess performance and stability impact introduced by "Coexistence Topology, Scheduler Map and Runbook".
- Expected: Story "Coexistence Topology, Scheduler Map and Runbook" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-025.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-006 — Bootstrap Maven multi-module Java 21 platform skeleton

- User Story: WO-003
- Objective: Assess performance and stability impact introduced by "Bootstrap Maven multi-module Java 21 platform skeleton".
- Expected: Story "Bootstrap Maven multi-module Java 21 platform skeleton" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-003.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-007 — Provision Terraform infrastructure for three PCIS environments

- User Story: WO-004
- Objective: Assess performance and stability impact introduced by "Provision Terraform infrastructure for three PCIS environments".
- Expected: Story "Provision Terraform infrastructure for three PCIS environments" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-004.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-008 — Create reproducible distroless non-root service images

- User Story: WO-012
- Objective: Assess performance and stability impact introduced by "Create reproducible distroless non-root service images".
- Expected: Story "Create reproducible distroless non-root service images" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-012.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-009 — Implement Forge Shipping pipeline with security gates

- User Story: WO-029
- Objective: Assess performance and stability impact introduced by "Implement Forge Shipping pipeline with security gates".
- Expected: Story "Implement Forge Shipping pipeline with security gates" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-029.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-010 — Establish GitOps delivery with fifteen-minute rollback

- User Story: WO-046
- Objective: Assess performance and stability impact introduced by "Establish GitOps delivery with fifteen-minute rollback".
- Expected: Story "Establish GitOps delivery with fifteen-minute rollback" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-046.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-011 — Define batch CronJob manifests and exit-code contract

- User Story: WO-063
- Objective: Assess performance and stability impact introduced by "Define batch CronJob manifests and exit-code contract".
- Expected: Story "Define batch CronJob manifests and exit-code contract" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-063.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-012 — Build shared observability starter with PII-masking structured logging

- User Story: WO-013
- Objective: Assess performance and stability impact introduced by "Build shared observability starter with PII-masking structured logging".
- Expected: Story "Build shared observability starter with PII-masking structured logging" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-013.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-013 — Implement structured error library with reason-code registry

- User Story: WO-031
- Objective: Assess performance and stability impact introduced by "Implement structured error library with reason-code registry".
- Expected: Story "Implement structured error library with reason-code registry" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-031.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-014 — Publish baseline metrics, SLO dashboards and alert rules

- User Story: WO-064
- Objective: Assess performance and stability impact introduced by "Publish baseline metrics, SLO dashboards and alert rules".
- Expected: Story "Publish baseline metrics, SLO dashboards and alert rules" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-064.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-015 — Author operational runbooks for batch, rollback, purge and incidents

- User Story: WO-080
- Objective: Assess performance and stability impact introduced by "Author operational runbooks for batch, rollback, purge and incidents".
- Expected: Story "Author operational runbooks for batch, rollback, purge and incidents" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-080.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-016 — Build audit-svc core with versioned v1 audit event contract

- User Story: WO-010
- Objective: Assess performance and stability impact introduced by "Build audit-svc core with versioned v1 audit event contract".
- Expected: Story "Build audit-svc core with versioned v1 audit event contract" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-010.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-017 — Make audit writes atomic with mutations via transactional outbox

- User Story: WO-026
- Objective: Assess performance and stability impact introduced by "Make audit writes atomic with mutations via transactional outbox".
- Expected: Story "Make audit writes atomic with mutations via transactional outbox" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-026.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-018 — Convert AUD002B archiving into restartable retention and purge job

- User Story: WO-042
- Objective: Assess performance and stability impact introduced by "Convert AUD002B archiving into restartable retention and purge job".
- Expected: Story "Convert AUD002B archiving into restartable retention and purge job" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-042.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-019 — Mask PII and classify data before audit persistence

- User Story: WO-051
- Objective: Assess performance and stability impact introduced by "Mask PII and classify data before audit persistence".
- Expected: Story "Mask PII and classify data before audit persistence" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-051.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-020 — Expose audit inquiry API with gated unmask and observability

- User Story: WO-067
- Objective: Assess performance and stability impact introduced by "Expose audit inquiry API with gated unmask and observability".
- Expected: Story "Expose audit inquiry API with gated unmask and observability" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-067.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-021 — Build authz-svc policy decision service with deny-by-default

- User Story: WO-011
- Objective: Assess performance and stability impact introduced by "Build authz-svc policy decision service with deny-by-default".
- Expected: Story "Build authz-svc policy decision service with deny-by-default" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-011.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-022 — Enforce approval linkage and cumulative claim authority limits

- User Story: WO-030
- Objective: Assess performance and stability impact introduced by "Enforce approval linkage and cumulative claim authority limits".
- Expected: Story "Enforce approval linkage and cumulative claim authority limits" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-030.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-023 — Apply deny-by-default guards to financial mutation endpoints

- User Story: WO-048
- Objective: Assess performance and stability impact introduced by "Apply deny-by-default guards to financial mutation endpoints".
- Expected: Story "Apply deny-by-default guards to financial mutation endpoints" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-048.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-024 — Replace batch actor literals with authenticated service principals

- User Story: WO-049
- Objective: Assess performance and stability impact introduced by "Replace batch actor literals with authenticated service principals".
- Expected: Story "Replace batch actor literals with authenticated service principals" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-049.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-025 — Automate authorization regression and segregation-of-duties control evidence

- User Story: WO-065
- Objective: Assess performance and stability impact introduced by "Automate authorization regression and segregation-of-duties control evidence".
- Expected: Story "Automate authorization regression and segregation-of-duties control evidence" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-065.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-026 — Machine-readable data classification registry for all PCIS entities

- User Story: WO-005
- Objective: Assess performance and stability impact introduced by "Machine-readable data classification registry for all PCIS entities".
- Expected: Story "Machine-readable data classification registry for all PCIS entities" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-005.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-027 — Shared PII masking library with Jackson and Logback integration

- User Story: WO-017
- Objective: Assess performance and stability impact introduced by "Shared PII masking library with Jackson and Logback integration".
- Expected: Story "Shared PII masking library with Jackson and Logback integration" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-017.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-028 — Permission-gated self-audited PII unmask action for investigators

- User Story: WO-035
- Objective: Assess performance and stability impact introduced by "Permission-gated self-audited PII unmask action for investigators".
- Expected: Story "Permission-gated self-audited PII unmask action for investigators" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-035.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-029 — Tiered retention with partitioned audit table and restartable job

- User Story: WO-038
- Objective: Assess performance and stability impact introduced by "Tiered retention with partitioned audit table and restartable job".
- Expected: Story "Tiered retention with partitioned audit table and restartable job" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-038.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-030 — Mask PII at audit event creation before outbox persistence

- User Story: WO-043
- Objective: Assess performance and stability impact introduced by "Mask PII at audit event creation before outbox persistence".
- Expected: Story "Mask PII at audit event creation before outbox persistence" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-043.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-031 — CI gates for unclassified entities and unmasked PII leakage

- User Story: WO-047
- Objective: Assess performance and stability impact introduced by "CI gates for unclassified entities and unmasked PII leakage".
- Expected: Story "CI gates for unclassified entities and unmasked PII leakage" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-047.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-032 — Automated purge with cryptographic erasure and immutable evidence

- User Story: WO-057
- Objective: Assess performance and stability impact introduced by "Automated purge with cryptographic erasure and immutable evidence".
- Expected: Story "Automated purge with cryptographic erasure and immutable evidence" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-057.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-033 — Create versioned tunables and rules configuration schema

- User Story: WO-006
- Objective: Assess performance and stability impact introduced by "Create versioned tunables and rules configuration schema".
- Expected: Story "Create versioned tunables and rules configuration schema" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-006.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-034 — Typed tunable resolution service with cache and fail-fast validation

- User Story: WO-018
- Objective: Assess performance and stability impact introduced by "Typed tunable resolution service with cache and fail-fast validation".
- Expected: Story "Typed tunable resolution service with cache and fail-fast validation" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-018.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-035 — Admin tunables REST API with RBAC and change evidence

- User Story: WO-036
- Objective: Assess performance and stability impact introduced by "Admin tunables REST API with RBAC and change evidence".
- Expected: Story "Admin tunables REST API with RBAC and change evidence" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-036.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-036 — Admin tunables web panel with versioned change history

- User Story: WO-037
- Objective: Assess performance and stability impact introduced by "Admin tunables web panel with versioned change history".
- Expected: Story "Admin tunables web panel with versioned change history" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-037.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-037 — Externalized code-table and business rules store service

- User Story: WO-055
- Objective: Assess performance and stability impact introduced by "Externalized code-table and business rules store service".
- Expected: Story "Externalized code-table and business rules store service" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-055.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-038 — Author Flyway Baseline PostgreSQL Schema Migrations

- User Story: WO-007
- Objective: Assess performance and stability impact introduced by "Author Flyway Baseline PostgreSQL Schema Migrations".
- Expected: Story "Author Flyway Baseline PostgreSQL Schema Migrations" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-007.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-039 — Implement Sequence Objects, Block Allocator and Key Formatter

- User Story: WO-019
- Objective: Assess performance and stability impact introduced by "Implement Sequence Objects, Block Allocator and Key Formatter".
- Expected: Story "Implement Sequence Objects, Block Allocator and Key Formatter" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-019.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-040 — Resolve Schema Discrepancies and Publish Corrected Data Dictionary

- User Story: WO-020
- Objective: Assess performance and stability impact introduced by "Resolve Schema Discrepancies and Publish Corrected Data Dictionary".
- Expected: Story "Resolve Schema Discrepancies and Publish Corrected Data Dictionary" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-020.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-041 — Automate Masked Anonymized Non-Production Data Refresh

- User Story: WO-021
- Objective: Assess performance and stability impact introduced by "Automate Masked Anonymized Non-Production Data Refresh".
- Expected: Story "Automate Masked Anonymized Non-Production Data Refresh" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-021.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-042 — Build Polling Extraction and Idempotent PostgreSQL Loader

- User Story: WO-039
- Objective: Assess performance and stability impact introduced by "Build Polling Extraction and Idempotent PostgreSQL Loader".
- Expected: Story "Build Polling Extraction and Idempotent PostgreSQL Loader" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-039.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-043 — Build Nightly Cent-Level Parallel-Run Reconciliation Harness

- User Story: WO-058
- Objective: Assess performance and stability impact introduced by "Build Nightly Cent-Level Parallel-Run Reconciliation Harness".
- Expected: Story "Build Nightly Cent-Level Parallel-Run Reconciliation Harness" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-058.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-044 — Deterministic Seed Data Harness for Batch Regression Fixtures

- User Story: WO-014
- Objective: Assess performance and stability impact introduced by "Deterministic Seed Data Harness for Batch Regression Fixtures".
- Expected: Story "Deterministic Seed Data Harness for Batch Regression Fixtures" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-014.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-045 — Capture COBOL Baseline Golden Outputs with Determinism Controls

- User Story: WO-032
- Objective: Assess performance and stability impact introduced by "Capture COBOL Baseline Golden Outputs with Determinism Controls".
- Expected: Story "Capture COBOL Baseline Golden Outputs with Determinism Controls" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-032.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-046 — Cent-Level Golden Output Comparison Engine and Diff Reporting

- User Story: WO-033
- Objective: Assess performance and stability impact introduced by "Cent-Level Golden Output Comparison Engine and Diff Reporting".
- Expected: Story "Cent-Level Golden Output Comparison Engine and Diff Reporting" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-033.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-047 — Golden-Output Regression Suites for Six Batch Programs

- User Story: WO-053
- Objective: Assess performance and stability impact introduced by "Golden-Output Regression Suites for Six Batch Programs".
- Expected: Story "Golden-Output Regression Suites for Six Batch Programs" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-053.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-048 — Batch Fault Injection Proving Restart Without Duplicate Writes

- User Story: WO-054
- Objective: Assess performance and stability impact introduced by "Batch Fault Injection Proving Restart Without Duplicate Writes".
- Expected: Story "Batch Fault Injection Proving Restart Without Duplicate Writes" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-054.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-049 — CI Gate Wiring Coverage and Regression Evidence Artifacts

- User Story: WO-078
- Objective: Assess performance and stability impact introduced by "CI Gate Wiring Coverage and Regression Evidence Artifacts".
- Expected: Story "CI Gate Wiring Coverage and Regression Evidence Artifacts" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-078.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-050 — Create PostgreSQL customer schema with Flyway migrations

- User Story: WO-068
- Objective: Assess performance and stability impact introduced by "Create PostgreSQL customer schema with Flyway migrations".
- Expected: Story "Create PostgreSQL customer schema with Flyway migrations" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-068.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-051 — Build customer-svc domain and persistence layers

- User Story: WO-083
- Objective: Assess performance and stability impact introduced by "Build customer-svc domain and persistence layers".
- Expected: Story "Build customer-svc domain and persistence layers" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-083.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-052 — Expose versioned customer REST API with deny-by-default authorization

- User Story: WO-084
- Objective: Assess performance and stability impact introduced by "Expose versioned customer REST API with deny-by-default authorization".
- Expected: Story "Expose versioned customer REST API with deny-by-default authorization" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-084.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-053 — Publish masked customer read projection for downstream consumers

- User Story: WO-085
- Objective: Assess performance and stability impact introduced by "Publish masked customer read projection for downstream consumers".
- Expected: Story "Publish masked customer read projection for downstream consumers" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-085.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-054 — Establish customer slice parity harness and CI quality gates

- User Story: WO-091
- Objective: Assess performance and stability impact introduced by "Establish customer slice parity harness and CI quality gates".
- Expected: Story "Establish customer slice parity harness and CI quality gates" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-091.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-055 — Provision Claims PostgreSQL Schema With Exact Decimal Precision

- User Story: WO-027
- Objective: Assess performance and stability impact introduced by "Provision Claims PostgreSQL Schema With Exact Decimal Precision".
- Expected: Story "Provision Claims PostgreSQL Schema With Exact Decimal Precision" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-027.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-056 — Model Claim Approval Lifecycle As First-Class Record

- User Story: WO-044
- Objective: Assess performance and stability impact introduced by "Model Claim Approval Lifecycle As First-Class Record".
- Expected: Story "Model Claim Approval Lifecycle As First-Class Record" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-044.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-057 — Enforce Dual Payment Authority Check In Claims Domain

- User Story: WO-060
- Objective: Assess performance and stability impact introduced by "Enforce Dual Payment Authority Check In Claims Domain".
- Expected: Story "Enforce Dual Payment Authority Check In Claims Domain" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-060.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-058 — Expose Versioned Claims REST API With Deny-By-Default

- User Story: WO-066
- Objective: Assess performance and stability impact introduced by "Expose Versioned Claims REST API With Deny-By-Default".
- Expected: Story "Expose Versioned Claims REST API With Deny-By-Default" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-066.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-059 — Instrument Claims Payment Observability And SoD Evidence

- User Story: WO-081
- Objective: Assess performance and stability impact introduced by "Instrument Claims Payment Observability And SoD Evidence".
- Expected: Story "Instrument Claims Payment Observability And SoD Evidence" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-081.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-060 — Convert CLM006B Claim Payment Batch To Spring Batch

- User Story: WO-090
- Objective: Assess performance and stability impact introduced by "Convert CLM006B Claim Payment Batch To Spring Batch".
- Expected: Story "Convert CLM006B Claim Payment Batch To Spring Batch" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-090.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-061 — Build Claim Payment Golden-Output Parity Harness

- User Story: WO-097
- Objective: Assess performance and stability impact introduced by "Build Claim Payment Golden-Output Parity Harness".
- Expected: Story "Build Claim Payment Golden-Output Parity Harness" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-097.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-062 — Migrate billing and commission schema with set-based readers

- User Story: WO-056
- Objective: Assess performance and stability impact introduced by "Migrate billing and commission schema with set-based readers".
- Expected: Story "Migrate billing and commission schema with set-based readers" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-056.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-063 — Convert BIL003B billing generation to Spring Batch job

- User Story: WO-070
- Objective: Assess performance and stability impact introduced by "Convert BIL003B billing generation to Spring Batch job".
- Expected: Story "Convert BIL003B billing generation to Spring Batch job" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-070.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-064 — Convert PRM005B delinquency aging to Spring Batch job

- User Story: WO-071
- Objective: Assess performance and stability impact introduced by "Convert PRM005B delinquency aging to Spring Batch job".
- Expected: Story "Convert PRM005B delinquency aging to Spring Batch job" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-071.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-065 — Convert CMM001B commission calculation to Spring Batch job

- User Story: WO-072
- Objective: Assess performance and stability impact introduced by "Convert CMM001B commission calculation to Spring Batch job".
- Expected: Story "Convert CMM001B commission calculation to Spring Batch job" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-072.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-066 — Gate billing batch cutover with parallel-run reconciliation

- User Story: WO-088
- Objective: Assess performance and stability impact introduced by "Gate billing batch cutover with parallel-run reconciliation".
- Expected: Story "Gate billing batch cutover with parallel-run reconciliation" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-088.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-067 — Expose billing-svc REST API and batch operations endpoints

- User Story: WO-089
- Objective: Assess performance and stability impact introduced by "Expose billing-svc REST API and batch operations endpoints".
- Expected: Story "Expose billing-svc REST API and batch operations endpoints" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-089.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-068 — Freeze versioned v1 premium rating OpenAPI contract

- User Story: WO-028
- Objective: Assess performance and stability impact introduced by "Freeze versioned v1 premium rating OpenAPI contract".
- Expected: Story "Freeze versioned v1 premium rating OpenAPI contract" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-028.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-069 — Scaffold premium-svc service and rating data access layer

- User Story: WO-045
- Objective: Assess performance and stability impact introduced by "Scaffold premium-svc service and rating data access layer".
- Expected: Story "Scaffold premium-svc service and rating data access layer" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-045.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-070 — Implement rating engine with exact decimal arithmetic

- User Story: WO-062
- Objective: Assess performance and stability impact introduced by "Implement rating engine with exact decimal arithmetic".
- Expected: Story "Implement rating engine with exact decimal arithmetic" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-062.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-071 — Build golden-output parity harness for premium rating

- User Story: WO-069
- Objective: Assess performance and stability impact introduced by "Build golden-output parity harness for premium rating".
- Expected: Story "Build golden-output parity harness for premium rating" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-069.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-072 — Add underwriting rule evaluation and referral outcome tracking

- User Story: WO-076
- Objective: Assess performance and stability impact introduced by "Add underwriting rule evaluation and referral outcome tracking".
- Expected: Story "Add underwriting rule evaluation and referral outcome tracking" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-076.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-073 — Enforce rating contract with consumer-driven tests and CI gate

- User Story: WO-077
- Objective: Assess performance and stability impact introduced by "Enforce rating contract with consumer-driven tests and CI gate".
- Expected: Story "Enforce rating contract with consumer-driven tests and CI gate" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-077.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-074 — Migrate Policy Domain Schema to PostgreSQL with Flyway

- User Story: WO-086
- Objective: Assess performance and stability impact introduced by "Migrate Policy Domain Schema to PostgreSQL with Flyway".
- Expected: Story "Migrate Policy Domain Schema to PostgreSQL with Flyway" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-086.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-075 — Build policy-svc REST API with Versioned Read Contract

- User Story: WO-092
- Objective: Assess performance and stability impact introduced by "Build policy-svc REST API with Versioned Read Contract".
- Expected: Story "Build policy-svc REST API with Versioned Read Contract" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-092.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-076 — Convert POL006B Renewal Batch to Restartable Spring Batch Job

- User Story: WO-093
- Objective: Assess performance and stability impact introduced by "Convert POL006B Renewal Batch to Restartable Spring Batch Job".
- Expected: Story "Convert POL006B Renewal Batch to Restartable Spring Batch Job" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-093.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-077 — Golden-Output Parity Harness for Renewal Batch Reconciliation

- User Story: WO-094
- Objective: Assess performance and stability impact introduced by "Golden-Output Parity Harness for Renewal Batch Reconciliation".
- Expected: Story "Golden-Output Parity Harness for Renewal Batch Reconciliation" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-094.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-078 — Renewal Exception Reporting, Metrics and Alerting Runbook

- User Story: WO-098
- Objective: Assess performance and stability impact introduced by "Renewal Exception Reporting, Metrics and Alerting Runbook".
- Expected: Story "Renewal Exception Reporting, Metrics and Alerting Runbook" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-098.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-079 — Originate Renewal Billing Schedule via Policy Domain Events

- User Story: WO-100
- Objective: Assess performance and stability impact introduced by "Originate Renewal Billing Schedule via Policy Domain Events".
- Expected: Story "Originate Renewal Billing Schedule via Policy Domain Events" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-100.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-080 — Provision PostgreSQL Read Replica With Lag SLO

- User Story: WO-040
- Objective: Assess performance and stability impact introduced by "Provision PostgreSQL Read Replica With Lag SLO".
- Expected: Story "Provision PostgreSQL Read Replica With Lag SLO" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-040.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-081 — Build reporting-svc Replica-Backed Report APIs

- User Story: WO-059
- Objective: Assess performance and stability impact introduced by "Build reporting-svc Replica-Backed Report APIs".
- Expected: Story "Build reporting-svc Replica-Backed Report APIs" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-059.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-082 — Document Storage And Notification Integration Layer

- User Story: WO-074
- Objective: Assess performance and stability impact introduced by "Document Storage And Notification Integration Layer".
- Expected: Story "Document Storage And Notification Integration Layer" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-074.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-083 — Automated Legacy Decommission Readiness Gate

- User Story: WO-075
- Objective: Assess performance and stability impact introduced by "Automated Legacy Decommission Readiness Gate".
- Expected: Story "Automated Legacy Decommission Readiness Gate" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-075.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-084 — Execute Legacy IBM i Decommission And Archive

- User Story: WO-101
- Objective: Assess performance and stability impact introduced by "Execute Legacy IBM i Decommission And Archive".
- Expected: Story "Execute Legacy IBM i Decommission And Archive" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-101.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-085 — Accessible SPA Shell, Routing and API Client Foundation

- User Story: WO-015
- Objective: Assess performance and stability impact introduced by "Accessible SPA Shell, Routing and API Client Foundation".
- Expected: Story "Accessible SPA Shell, Routing and API Client Foundation" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-015.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-086 — Customer Workspace Replacing CUS Green-Screen Panels

- User Story: WO-061
- Objective: Assess performance and stability impact introduced by "Customer Workspace Replacing CUS Green-Screen Panels".
- Expected: Story "Customer Workspace Replacing CUS Green-Screen Panels" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-061.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-087 — Billing Invoice, Payment and Delinquency Screens

- User Story: WO-079
- Objective: Assess performance and stability impact introduced by "Billing Invoice, Payment and Delinquency Screens".
- Expected: Story "Billing Invoice, Payment and Delinquency Screens" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-079.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-088 — Policy Issuance Screen With Premium Rating Breakdown

- User Story: WO-082
- Objective: Assess performance and stability impact introduced by "Policy Issuance Screen With Premium Rating Breakdown".
- Expected: Story "Policy Issuance Screen With Premium Rating Breakdown" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-082.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-089 — OIDC Sign-In and Role-Based UI Gating

- User Story: WO-087
- Objective: Assess performance and stability impact introduced by "OIDC Sign-In and Role-Based UI Gating".
- Expected: Story "OIDC Sign-In and Role-Based UI Gating" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-087.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-090 — WCAG 2.1 AA Conformance Test Harness and CI Gate

- User Story: WO-095
- Objective: Assess performance and stability impact introduced by "WCAG 2.1 AA Conformance Test Harness and CI Gate".
- Expected: Story "WCAG 2.1 AA Conformance Test Harness and CI Gate" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-095.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-091 — Claims Workspace With Approval and Payment Controls

- User Story: WO-096
- Objective: Assess performance and stability impact introduced by "Claims Workspace With Approval and Payment Controls".
- Expected: Story "Claims Workspace With Approval and Payment Controls" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-096.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-092 — Accessible Component Library Replacing DDS Subfiles

- User Story: WO-099
- Objective: Assess performance and stability impact introduced by "Accessible Component Library Replacing DDS Subfiles".
- Expected: Story "Accessible Component Library Replacing DDS Subfiles" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-099.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-093 — Deploy API Gateway With TLS, Routing and Rate Limiting

- User Story: WO-016
- Objective: Assess performance and stability impact introduced by "Deploy API Gateway With TLS, Routing and Rate Limiting".
- Expected: Story "Deploy API Gateway With TLS, Routing and Rate Limiting" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-016.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-094 — Enforce Zone Segmentation, mTLS and Egress Allow-Lists

- User Story: WO-034
- Objective: Assess performance and stability impact introduced by "Enforce Zone Segmentation, mTLS and Egress Allow-Lists".
- Expected: Story "Enforce Zone Segmentation, mTLS and Egress Allow-Lists" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-034.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-095 — Implement OIDC Federation and JWT Validation Enforcement

- User Story: WO-050
- Objective: Assess performance and stability impact introduced by "Implement OIDC Federation and JWT Validation Enforcement".
- Expected: Story "Implement OIDC Federation and JWT Validation Enforcement" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-050.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-096 — Federate Workload Identity For Batch and Service Calls

- User Story: WO-052
- Objective: Assess performance and stability impact introduced by "Federate Workload Identity For Batch and Service Calls".
- Expected: Story "Federate Workload Identity For Batch and Service Calls" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-052.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-097 — Machine-readable open design item decision register with CI gate

- User Story: WO-008
- Objective: Assess performance and stability impact introduced by "Machine-readable open design item decision register with CI gate".
- Expected: Story "Machine-readable open design item decision register with CI gate" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-008.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-098 — Parallel-run reconciliation engine with per-domain cutover gate scoring

- User Story: WO-022
- Objective: Assess performance and stability impact introduced by "Parallel-run reconciliation engine with per-domain cutover gate scoring".
- Expected: Story "Parallel-run reconciliation engine with per-domain cutover gate scoring" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-022.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-099 — Per-domain cutover control plane with audited rollback switches

- User Story: WO-023
- Objective: Assess performance and stability impact introduced by "Per-domain cutover control plane with audited rollback switches".
- Expected: Story "Per-domain cutover control plane with audited rollback switches" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-023.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-100 — Automated phase-gate evidence pack generation and governance dashboard API

- User Story: WO-041
- Objective: Assess performance and stability impact introduced by "Automated phase-gate evidence pack generation and governance dashboard API".
- Expected: Story "Automated phase-gate evidence pack generation and governance dashboard API" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-041.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.

### PERFORMANCE-101 — Legacy behaviour decision records with preserve-versus-change parity matrix

- User Story: WO-073
- Objective: Assess performance and stability impact introduced by "Legacy behaviour decision records with preserve-versus-change parity matrix".
- Expected: Story "Legacy behaviour decision records with preserve-versus-change parity matrix" satisfies expected performance validation outcomes without critical issues.

**Steps**
- Identify throughput/latency-sensitive path for WO-073.
- Run benchmark/load scenario with representative input.
- Compare key metrics against baseline and acceptance thresholds.