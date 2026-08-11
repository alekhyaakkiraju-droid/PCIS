## Legacy Baseline, Repository Manifest & Coexistence Build Automation

### [P0] Repository Member Manifest and Completeness Gate

WHAT & WHY: The PCIS repository holds 39 members (8 COBOL programs, 22 DDS display files, 2 CL members, 7 design documents) with no machine-readable inventory, and several members are incomplete or design-only (CLM001A-CLM005A are specifications only, CUS_Module_Design_Document.md is reported empty, and some .cbl bodies are unverifiable). Auditors, migration engineers and the future parallel-run harness cannot currently tell which described behaviour is actually implemented, which directly undermines confidence in documented controls such as segregation of duties. We need a versioned manifest that declares every member, its type, owning module, implementation status and declared dependencies, plus an automated gate that fails the build when a member is added, removed or left unclassified. IMPACT: Adds a new manifest artifact (manifest/pcis-manifest.yaml), a validator script under tools/manifest, and parsing logic that reads COBOL prologue CALLS/TABLES comment blocks from AUD002B.cbl, BIL003B.cbl, CMM001B.cbl, CLM006B.cbl, PRM005B.cbl, POL006B.cbl, CUS001A.cbl and POL001A.cbl. No COBOL source behaviour is modified. WHAT DONE LOOKS LIKE: Running the validator locally or in CI enumerates the repository tree, reconciles it against the manifest, and exits non-zero with an actionable per-member message on any drift, missing classification, empty file or undeclared member. WHAT DONE LOOKS LIKE also includes a human-readable completeness report listing implemented vs design-only vs empty members. SCOPE BOUNDARIES: This story does NOT create the legacy compile automation, does NOT wire the validator into the CI pipeline definition, does NOT change any COBOL or DDS source, and does NOT resolve the missing members themselves. DEPENDENCIES: None — this is the first story of the epic and produces the inventory consumed by WO-002, WO-003, WO-004 and WO-005.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | epic:EPIC-00, type:tooling, area:platform, legacy-baseline, complexity:medium |

**Acceptance Criteria**
- A committed manifest file at manifest/pcis-manifest.yaml lists all 39 repository members with fields: path, member_type (cobol_program, dds_display_file, cl_member, design_document), module_code, implementation_status (implemented, design_only, empty, unverified), declared_calls, declared_tables and notes.
- A validator command (tools/manifest/validate_manifest.py or equivalent) walks the repository tree, compares it against the manifest, and exits with code 0 only when every file on disk is declared and every declared file exists; it exits non-zero with a per-member reason otherwise.
- The validator detects and reports zero-byte or whitespace-only source members (currently at least CLM006B.cbl, CMM001B.cbl and CUS_Module_Design_Document.md are suspected) as implementation_status=empty and fails if the manifest disagrees.
- Declared_calls and declared_tables for each COBOL program are extracted from the program prologue comment block (CALLS and TABLES lines) and stored in the manifest; a mismatch between prologue and manifest fails validation.
- A generated human-readable report (build/reports/manifest-completeness.md) summarises counts per member_type and per implementation_status and is reproducible from a clean checkout.
- Unit tests written and passing for the manifest parser and validator covering: missing file, extra file, empty file, malformed prologue, and fully valid manifest.
- System integration test validating the validator against the real repository tree in a clean checkout, asserting exit code 0 for the committed manifest.
- Mock data/fixtures generated and committed under tools/manifest/testdata containing synthetic COBOL prologues and a synthetic tree for the negative cases above.

### [P0] Extract Legacy Behavioural Baseline Specification Artifact

WHAT & WHY: Every behaviour that the Java target must preserve to the cent currently lives only inside fixed-format COBOL WORKING-STORAGE literals, cursor declarations and inline SQLCODE branches — commit granularity (one policy/installment/payment per commit), the hard-coded tunables WS-RETENTION-DAYS 365, WS-CHUNK-SIZE 5000, WS-LEAD-DAYS 15, WS-GRACE-DAYS 10, WS-RENEWAL-WINDOW-DAYS 60 and the 100000.00 reinsurance threshold, the batch actor literals BATCHAUD/BATCHBIL/BATCHCMM/BATCHPRM/BATCHCLM/BATCHREN, and the AUDLOG01 parameter-width drift between batch callers (X(3) action, X(30) values) and interactive callers (X(1) action, X(100) values, X(40) key). Without a machine-readable baseline, the parallel-run reconciliation and golden-output suites have nothing authoritative to assert against and prologue claims such as CLM006B VERIFIES PAYMENT AUTHORITY stay unverified. IMPACT: Adds baseline/legacy-baseline.yaml plus an extractor and a drift detector under tools/baseline; reads AUD002B.cbl, BIL003B.cbl, CMM001B.cbl, CLM006B.cbl, PRM005B.cbl, POL006B.cbl, CUS001A.cbl and POL001A.cbl; consumes the manifest produced by WO-001. No COBOL logic is changed. WHAT DONE LOOKS LIKE: A single reviewable artifact enumerates, per program, the commit scope, cursor SQL text, every numeric and identity literal with its PIC clause, every audit call with its nine parameter widths, and every error path that continues after failure; the drift detector fails when source and baseline disagree or when a prologue claims a CALL the PROCEDURE DIVISION does not make. SCOPE BOUNDARIES: This story does NOT externalize any tunable, does NOT fix any error-handling defect, does NOT build the golden-output harness or seed data, and does NOT touch DDS panels. DEPENDENCIES: Depends on WO-001 for the member inventory and prologue parser it reuses.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:EPIC-00, type:analysis-tooling, area:platform, legacy-baseline, complexity:high |

**Acceptance Criteria**
- baseline/legacy-baseline.yaml is committed and records, for each of the 8 COBOL programs: program name, called-by scheduler entry, commit scope statement, declared cursors with full SQL text, all WORKING-STORAGE numeric and identity literals with name, PIC clause and value, and all AUDLOG01 call sites with the nine parameter names and PIC widths.
- The baseline captures the six regulatory tunables (retention days 365, chunk size 5000, lead days 15, grace days 10, renewal window 60, reinsurance threshold 100000.00) and the six batch actor literals with the exact program and paragraph where each is defined.
- The baseline records every continue-after-failure error path, including the non-00 AUDLOG01 return handling in BIL003B, CMM001B, PRM005B, POL006B and CLM006B, and the archive verification mismatch halt in AUD002B, each with the source paragraph reference.
- A drift detector (tools/baseline/check_baseline.py) re-extracts values from source and exits non-zero when any recorded literal, cursor text, commit scope or audit parameter width no longer matches the committed baseline.
- The drift detector additionally reports prologue-versus-code contradictions, specifically flagging any program whose prologue declares a CALL that does not appear in its PROCEDURE DIVISION and any prologue claim of authority verification with no SECCHK01 call.
- An audit-contract reconciliation section of the baseline documents the widest observed width per parameter across all callers and asserts that no historical value width exceeds the recorded maximum.
- Unit tests written and passing for the literal extractor, cursor extractor, audit-call extractor and prologue contradiction detector.
- System integration test running the extractor and drift detector against the real repository, asserting exit code 0 against the committed baseline.
- Mock data/fixtures generated and committed under tools/baseline/testdata with synthetic COBOL members exercising each extractor rule and each drift class.

**Depends on:** WO-001

### [P0] Scripted Dependency-Ordered Legacy COBOL Build

WHAT & WHY: The entire build capability of PCIS today is two hand-maintained CL members (PCIS_CRTOBJ.clle and JOBSCHD_NEW_DRIVERS.clle) with undocumented compile order and library setup, and promotion is a manual library copy along INSDEV to INSTST to INSPRD. Because parallel-run comparison requires a runnable legacy side throughout coexistence, the COBOL baseline must stay buildable and patchable from a clean checkout by an engineer who has never touched it. We need a scripted, dependency-correct, idempotent build that invokes CRTSQLCBLI and CRTPGM (and CRTDSPF for the 22 DDS members) with an explicit library list per environment, and that records the exact Enterprise COBOL for i compiler release used, since 6.3.x reached end of support on 2025-09-30 and the coexistence toolchain must be on a supported level. IMPACT: Adds build/ scripts and configuration (build/build.yaml, build/scripts/build_legacy.sh, build/scripts/*.clle templates), a compile-order graph derived from the WO-001 manifest, and a compiler-level assertion step. Consumes the manifest and baseline artifacts. WHAT DONE LOOKS LIKE: One command builds every declared member in dependency order against a configured environment, emits a structured build log and a build manifest recording compiler release, member checksums and object names, and exits non-zero on the first compile failure with the offending member and message id. SCOPE BOUNDARIES: This story does NOT provision the IBM i partition, does NOT perform library promotion or restore-based rollback (WO-005), does NOT define the CI pipeline that calls it (WO-004), and does NOT convert anything to Java. DEPENDENCIES: Depends on WO-001 for the member inventory and declared dependencies, and on WO-002 for the recorded commit-scope and cursor baseline used to sanity-check that no source drifted during build scripting.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:EPIC-00, type:build-automation, area:platform, coexistence, complexity:high |

**Acceptance Criteria**
- A single entrypoint (build/scripts/build_legacy.sh) accepts an environment name (dev, tst, prd) and builds all COBOL, DDS and CL members declared in manifest/pcis-manifest.yaml in dependency-correct order, with no manual step and no interactive prompt.
- Compile order is computed from declared dependencies (copybooks and service programs from prologue CALLS, DDS display files bound to interactive programs) rather than hard-coded, and the resolved order is printed and written to build/reports/compile-order.txt.
- Environment-specific library topology (INSDEV/INSDEVDTA, INSTST/INSTSTDTA, INSPRD/INSPRDDTA, shared INSCOM, tooling INSTOOLS) is externalized in build/build.yaml with no library name hard-coded in any script.
- The build records and asserts the Enterprise COBOL for i compiler release; the build fails with a clear message if the detected release is an out-of-support level (4.2, 5.1, 5.2 or 6.3.x past extended support) or if no release could be determined.
- A build manifest (build/reports/build-manifest.json) is emitted per run containing timestamp, environment, compiler release, per-member source checksum, target object name and object type, and overall result.
- The build is idempotent and re-runnable: running it twice against an unchanged checkout produces the same object set and a build manifest differing only in timestamp.
- A first compile failure aborts the run with a non-zero exit code and a structured log line containing member path, CL command, and the IBM i message id, with no silent continuation.
- Unit tests written and passing for the dependency-graph resolver, the build.yaml loader and the compiler-release gate, using fixtures rather than a live IBM i connection.
- System integration tests validating the build orchestrator end to end against a stubbed CL command executor that records invocation order and arguments, asserting correct CRTSQLCBLI/CRTPGM sequencing for all 8 COBOL programs.
- Mock data/fixtures generated and committed: a synthetic manifest, a synthetic member set, and recorded CL-invocation golden files under build/testdata.

**Depends on:** WO-001, WO-002

### [P0] CI Pipeline Gating Manifest, Baseline and Build

WHAT & WHY: There is no CI configuration, build manifest, container definition or IaC anywhere in the repository, so there is nowhere to enforce the manifest gate from WO-001, the baseline drift detector from WO-002, the legacy build from WO-003, or the supply-chain scanning required by policy. Until a pipeline exists, every control in this programme remains advisory. We need a declarative Forge Shipping pipeline (with an equivalent local invocation path) that runs on every commit, pull request and tag, executes the completeness and drift gates, runs secret and static-analysis scans, invokes the coexistence legacy build against the development environment, publishes the build manifest and an SBOM of the tooling dependencies, and blocks merge on any failed gate. IMPACT: Adds ci/forge-shipping.yaml (or the tenant-standard pipeline descriptor), a Makefile or task runner wrapping the same gates for local use, and a container definition for the tooling image so pipeline and laptop behaviour match. Consumes artifacts from WO-001, WO-002 and WO-003. WHAT DONE LOOKS LIKE: A pull request that adds an unclassified member, mutates a baselined literal, commits a secret, or breaks the legacy build fails with a named, actionable gate; a clean pull request passes all gates in under the agreed pipeline duration and publishes its reports as artifacts. SCOPE BOUNDARIES: This story does NOT deploy anything to an environment, does NOT create Java service pipelines, does NOT build container images for application services, and does NOT define production promotion approvals (WO-005). DEPENDENCIES: Depends on WO-001 (manifest validator), WO-002 (baseline drift detector) and WO-003 (legacy build entrypoint) as the executables it orchestrates.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | epic:EPIC-00, type:cicd, area:platform, coexistence, complexity:medium |

**Acceptance Criteria**
- A committed pipeline descriptor under ci/ defines source triggers for commit, pull request and tag, and stages for validate, scan, legacy-build and publish, using the Forge Shipping step catalog.
- The validate stage runs the WO-001 manifest validator and the WO-002 baseline drift detector and fails the pipeline with the offending member and reason on any non-zero exit.
- The scan stage runs a secret scan and a static-analysis scan across the repository and tooling code in parallel, and the pipeline blocks progression if any scan reports a blocking finding.
- The legacy-build stage invokes build/scripts/build_legacy.sh against the dev environment using credentials injected from a secret store, never from repository content, and fails the pipeline on any non-zero build exit code.
- The publish stage uploads build/reports/manifest-completeness.md, build/reports/compile-order.txt, build/reports/build-manifest.json, the structured build log and a CycloneDX SBOM of the tooling dependencies as retained pipeline artifacts.
- A local task runner target (make ci or equivalent) runs the identical validate and scan gates offline so a developer can reproduce a pipeline failure without pushing.
- A tooling container definition pins the Python and scanner versions used by the pipeline so pipeline and local runs use the same toolchain versions.
- Negative-path evidence is captured: four deliberately broken branches (undeclared member, mutated baseline literal, planted dummy secret, deliberately broken compile) each fail the expected named gate, with pipeline run links recorded in the pull request.
- Unit tests written and passing for any pipeline helper scripts added (report aggregation, exit-code mapping, artifact packaging).
- System integration test validating the full local gate chain (validate plus scan plus stubbed legacy build) executes end to end in a clean container and returns the expected aggregate exit code.
- Mock data/fixtures generated and committed under ci/testdata to drive the negative-path gate tests without touching real credentials or a live IBM i host.

**Depends on:** WO-001, WO-002, WO-003

### [P1] Coexistence Topology, Scheduler Map and Runbook

WHAT & WHY: Library and environment topology (INSDEV/INSDEVDTA, INSTST/INSTSTDTA, INSPRD/INSPRDDTA, shared INSCOM, tooling INSTOOLS) and the nightly and monthly batch scheduler topology (JOBSCHD1 running PRM005B, CLM006B and month-end AUD002B; JOBSCHD2 running POL006B; JOBSCHD3 running BIL003B and CMM001B) exist only inside CL members and COBOL prologue CALLED BY lines. Promotion is a manual library copy and the only rollback is restoring a saved library, none of it written down. During coexistence the legacy side must remain runnable, patchable and recoverable while Java domains migrate one at a time, so operators need a declarative topology plus a runbook covering promotion, batch schedule ownership, failure triage, batch-window monitoring and rollback with a stated recovery objective. IMPACT: Adds ops/topology.yaml describing environments and library lists, ops/scheduler-map.yaml mapping every scheduler entry to its programs, windows and owners, and docs/runbooks covering promotion, batch failure triage and rollback; cross-references the WO-003 build entrypoint and the WO-004 pipeline. WHAT DONE LOOKS LIKE: Any on-call engineer can determine from the repository which programs run in which window under which scheduler entry, promote a build from INSDEV to INSTST using a documented and validated procedure, triage a failed nightly run using the structured build and run-log evidence, and execute a rollback within the stated objective. SCOPE BOUNDARIES: This story does NOT change any COBOL, CL or DDS source, does NOT implement automated promotion or automated rollback execution, does NOT provision infrastructure, and does NOT define Java service deployment or Kubernetes manifests. DEPENDENCIES: Depends on WO-003 for the build entrypoint and environment configuration referenced by the promotion runbook, and on WO-004 for the pipeline artifacts referenced by the triage runbook.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | epic:EPIC-00, type:runbook, area:operations, coexistence, complexity:medium |

**Acceptance Criteria**
- ops/topology.yaml declares every coexistence environment (dev, tst, prd) with program library, data library, shared library INSCOM, tooling library INSTOOLS, resolved library list order and a data-sensitivity flag marking INSPRDDTA as the only library holding real customer data.
- ops/scheduler-map.yaml maps each scheduler entry (JOBSCHD1, JOBSCHD2, JOBSCHD3) to its programs, invocation cadence, declared commit scope from the WO-002 baseline, expected window, run-log table (RPT_RUN_LOG_T) evidence and an accountable owner role.
- A validator (tools/ops/validate_topology.py) cross-checks ops/scheduler-map.yaml against the CALLED BY lines in the COBOL prologues and against manifest/pcis-manifest.yaml, and exits non-zero on any program that is scheduled but undeclared, or declared but unscheduled.
- docs/runbooks/promotion.md documents the INSDEV to INSTST to INSPRD promotion procedure step by step, including the pre-promotion gate (green pipeline, clean manifest and baseline), the exact commands, the separation-of-duty requirement for production promotion and the post-promotion verification checks.
- docs/runbooks/batch-failure-triage.md documents, per scheduler entry, how to identify the failed program, where to read the structured build log and RPT_RUN_LOG_T counters, the known continue-after-failure paths recorded in the WO-002 baseline (including audit-write failure leaving a committed financial mutation), and the escalation path.
- docs/runbooks/rollback.md documents the save-and-restore library rollback procedure with a stated recovery time objective, the pre-conditions, the verification steps and the explicit statement that no schema or data change is included in a legacy-side rollback.
- A monitoring and alerting appendix defines the observable signals available today (batch job exit status, RPT_RUN_LOG_T error counts, window overrun) and the alert thresholds to configure for each scheduler entry.
- Unit tests written and passing for the topology and scheduler-map validator covering scheduled-but-undeclared, declared-but-unscheduled, unknown library reference and malformed YAML.
- System integration test running the topology validator against the real repository and asserting exit code 0 for the committed topology and scheduler map.
- Mock data/fixtures generated and committed under tools/ops/testdata providing synthetic topology, scheduler map and prologue inputs for each negative case.

**Depends on:** WO-003, WO-004

---

## Build, Container and Infrastructure-as-Code Platform

### [P0] Bootstrap Maven multi-module Java 21 platform skeleton

PCIS has no build manifest of any kind — the entire build capability is two IBM i CL members (PCIS_CRTOBJ.clle and JOBSCHD_NEW_DRIVERS.clle) whose compile order lives in tribal knowledge. Before any COBOL program can be re-expressed as a Spring service or Spring Batch job, a declarative, reproducible Java build must exist. This story creates the Maven 3.9 multi-module reactor for the eight deployable services identified in the target architecture (claims-svc, customer-svc, policy-svc, premium-svc, billing-svc, reporting-svc, authz-svc, audit-svc) plus five shared modules (pcis-bom, pcis-common, pcis-batch-common, pcis-contracts, pcis-migrations), pinned to Java 21 LTS and Spring Boot 3.5.x with a single dependency BOM so no service can drift onto an unmanaged transitive version. It also establishes the four-layer package convention (controller, application, domain, infrastructure) with an ArchUnit rule set that fails the build when domain packages import framework types, which is the mechanical guard against the JOBOL anti-pattern of transliterating procedural COBOL into Java. This is the root story of the epic: nothing else in EPIC-01 can start without a buildable artifact to containerize, scan, or deploy. Money handling conventions (BigDecimal with scale 2 and HALF_UP rounding, mirroring COMP-3 S9(9)V99 and S9(11)V99) are codified here as shared value types so every downstream domain story inherits them.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | epic:EPIC-01, type:platform, area:build, complexity:medium, modernization |

**Acceptance Criteria**
- A root pom.xml exists declaring maven.compiler.release 21 and a reactor containing exactly 13 modules: pcis-bom, pcis-common, pcis-batch-common, pcis-contracts, pcis-migrations, claims-svc, customer-svc, policy-svc, premium-svc, billing-svc, reporting-svc, authz-svc, audit-svc.
- Running a clean full build from a fresh checkout succeeds with no network-pinned SNAPSHOT dependencies and completes in under 10 minutes on a standard runner; the command and prerequisites are documented in a BUILD.md runbook.
- pcis-bom manages versions for Spring Boot 3.5.x, Spring Batch 5.x, Spring Security 6, PostgreSQL JDBC, Flyway, jOOQ or JdbcClient, JUnit 5, AssertJ, Testcontainers, spring-batch-test, logstash-logback-encoder, Micrometer and OpenTelemetry; no child module declares an explicit version for any managed dependency.
- JaCoCo is configured at the reactor level with an enforced 90 percent line-coverage rule scoped to monetary calculation packages (pcis-common money package and each service domain package), and the build fails when the threshold is not met.
- ArchUnit test suite in pcis-common fails the build if any class in a domain package imports org.springframework, jakarta.persistence, or jakarta.servlet types, and fails if an infrastructure class is referenced directly from a controller package.
- Unit tests: MoneyAmount value type tests prove scale-2 HALF_UP behaviour for division cases matching annual premium divided by installment count and for percentage commission multiplication, all passing.
- System integration tests: a smoke integration test per service module boots the Spring context with a Testcontainers PostgreSQL instance and asserts the actuator health endpoint returns UP.
- Mock data/fixtures: a shared test-fixtures source set in pcis-common provides synthetic policy, billing schedule, claim reserve and audit fixture builders with no real customer data, committed to the repository.

### [P0] Provision Terraform infrastructure for three PCIS environments

PCIS currently has no infrastructure definition at all; environments are hand-built IBM i libraries (INSDEV/INSDEVDTA, INSTST/INSTSTDTA, INSPRD/INSPRDDTA, INSCOM, INSTOOLS) promoted by manual library copy, which the assessment names as the root cause of dev/test/prod drift. This story writes Terraform 1.x modules that reproduce that topology as cloud infrastructure: a VPC with public, private and data subnets; a managed Kubernetes cluster with node groups or Karpenter provisioning that genuinely releases compute when batch pods scale to zero; a managed PostgreSQL 17 instance configured Multi-AZ for intra-region high availability with automated backups and point-in-time recovery; a managed secret store with rotation replacing the hard-coded batch actor literals and implicit OS trust; an object store bucket with Object Lock and lifecycle expiry for detached audit partitions; and a container registry. Critically, it also codifies the library-topology rule that INSPRDDTA is the only store of real customer data: the non-production databases are seeded exclusively by an automated masked-refresh pipeline so no unmasked tax ID, date of birth, email or phone ever lands in dev or test. Assumptions to state explicitly: AWS is the target cloud (inferred from the mandated push:ecr pipeline step), and Multi-AZ means intra-region HA only — no cross-region DR module is in scope.

| Field | Value |
|---|---|
| Story Points | 13 |
| Hours | 130h |
| Priority | P0 |
| Labels | epic:EPIC-01, type:infrastructure, area:iac, complexity:high, compliance:gdpr |

**Acceptance Criteria**
- Terraform root configurations exist for three environments (dev, test, prod) composed from six reusable modules: network, kubernetes, database, secrets, object-storage and registry; environment differences are expressed only through tfvars, not divergent module code.
- terraform validate and terraform plan succeed for all three environments against a remote state backend with state locking, and plan output for prod shows zero unmanaged resources.
- The database module provisions PostgreSQL 17 with Multi-AZ enabled, automated backups retained for at least 35 days, point-in-time recovery enabled, encryption at rest with a customer-managed key, and storage-in-transit enforced via TLS-only parameter settings; documented RTO and RPO are expressed in hours as agreed.
- The kubernetes module enforces a default-deny NetworkPolicy baseline per namespace and configures node provisioning such that a namespace with only completed Jobs releases its compute capacity, proven by a scale-to-zero verification test documented in the runbook.
- The object-storage module creates the audit archive bucket with Object Lock in compliance mode, versioning enabled and a lifecycle expiry rule, so detached audit partitions are write-once and expire on schedule.
- The secrets module provisions a managed secret store with automatic rotation and an IAM/IRSA binding per workload principal; no literal credential, token or password appears in any Terraform file, tfvars file or committed plan output — placeholders such as ${DATABASE_URL} are used in documentation.
- An automated masked-refresh pipeline definition exists that extracts from the production data source, applies field-level masking to restricted-tier columns (tax ID reduced to last four, email reduced to domain, phone and date of birth tokenized) and loads into test and dev databases; a verification job scans the refreshed non-production databases and fails if any unmasked restricted value is found.
- Unit tests: Terraform module unit tests (terraform test or Terratest) assert Multi-AZ, backup retention, encryption, Object Lock and default-deny NetworkPolicy attributes for each module, all passing.
- System integration tests: an ephemeral apply-and-destroy run in a sandbox account provisions network, cluster and database, runs a connectivity check from an in-cluster pod to the database over TLS, then destroys cleanly with no orphaned resources.
- Mock data/fixtures: synthetic seed datasets for dev are committed (policies, billing schedules, claim reserves, audit rows) containing no real customer data, and are used by the masked-refresh verification test.

### [P0] Create reproducible distroless non-root service images

No container definition exists anywhere in the PCIS repository, so there is currently no deployable unit for the target Java services and no way for the pipeline to reach a registry. This story defines the container build standard for all eight services and the batch runtime: a multi-stage build that compiles with the Maven reactor from WO-010, produces a layered Spring Boot image on a distroless Java 21 base, runs as an explicit non-root UID with a read-only root filesystem and no shell, and is byte-reproducible so the same commit always yields the same image digest. It also produces a CycloneDX software bill of materials per image and a health-probe contract (liveness, readiness, startup) so Kubernetes can make correct rollout decisions. Because batch jobs and long-running services have different runtime shapes, the story delivers two image profiles from one shared base: a service profile exposing an HTTP port with actuator probes, and a batch profile with no exposed port that exits with a meaningful process status code. Getting the image right here is what makes the security scan gates, image signing and rollback stories in this epic mechanically possible.

| Field | Value |
|---|---|
| Story Points | 3 |
| Hours | 30h |
| Priority | P0 |
| Labels | epic:EPIC-01, type:platform, area:containers, complexity:low, security |

**Acceptance Criteria**
- A shared Dockerfile template plus per-service build configuration produces an image for every service module and for the batch runtime, all based on a pinned distroless Java 21 runtime referenced by digest rather than by mutable tag.
- Every image runs as a non-root numeric UID with a read-only root filesystem, no shell and no package manager present, verified by an automated container-hardening test that inspects the built image.
- Building the same commit twice produces identical image digests (reproducible build), verified by a CI check that builds twice and compares digests, with build timestamps and file ordering normalized.
- Images use Spring Boot layered extraction so dependency layers are cached independently of application layers, and the resulting image size and layer count are recorded in the build output.
- A CycloneDX SBOM is emitted as a build artifact for every image and attached to the image metadata, listing all direct and transitive dependencies.
- The service profile exposes actuator liveness, readiness and startup probe endpoints that return correct states during slow startup and during database unavailability; the batch profile exposes no HTTP port and propagates the JVM exit code to the container exit status.
- Unit tests: build-configuration tests assert non-root UID, read-only root filesystem, absent shell, pinned base digest and probe endpoint paths for both profiles, all passing.
- System integration tests: each service image is started against a Testcontainers PostgreSQL instance and reaches readiness; the batch image is started with a failing job and its container exit status is asserted non-zero.
- Mock data/fixtures: a minimal synthetic schema and seed dataset used by the container integration test is committed so the test runs without external dependencies.

**Depends on:** WO-010

### [P0] Implement Forge Shipping pipeline with security gates

PCIS has no pipeline, which means there is nowhere to enforce the software-composition-analysis requirement, nowhere to run the golden-output regression suite that is the functional-parity gate, and nowhere to enforce separation of duty for production promotion. This story implements the mandated Forge Shipping pipeline as a single declarative definition covering build, scan, push, deploy, test and gate stages: build:maven with the 90 percent monetary coverage gate, build:docker producing the hardened reproducible image, four parallel scans (SonarQube quality gate, Snyk dependency CVEs, Gitleaks secrets, Semgrep SAST including a custom rule that fails the build when any mutating method lacks an authorization annotation), a container image scan emitting a CycloneDX SBOM, push:ecr with image signing and digest recording, deploy to dev with a Flyway-applies-cleanly-to-a-fresh-database check, functional and accessibility test stages, staging deployment with a parity reconciliation gate, a manual production approval gate where the approver must be distinct from the committer, and canary production rollout. Gates whose supporting artifacts are owned by other epics (golden-output regression harness, unclassified-table check, accessibility scan) must be wired as explicit fail-closed placeholders naming their owning epic, so the controls cannot be quietly deleted to unblock developers.

| Field | Value |
|---|---|
| Story Points | 13 |
| Hours | 130h |
| Priority | P0 |
| Labels | epic:EPIC-01, type:cicd, area:pipeline, complexity:high, security, compliance:sox |

**Acceptance Criteria**
- A single declarative Forge Shipping pipeline definition exists in the repository covering, in order: build:maven, build:docker, four parallel scans, image scan with SBOM emission, push:ecr with signing, deploy to dev, functional test stage, deploy to staging, parity reconciliation gate, manual production approval, production deploy and post-deploy smoke.
- build:maven fails the pipeline when unit tests fail or when line coverage on monetary calculation packages falls below 90 percent; the failing package and actual coverage are reported in the pipeline output.
- The four scan steps run in parallel and every one is a hard gate: SonarQube allows zero new blocker issues, Snyk allows zero critical or high CVEs including transitive dependencies, Gitleaks allows zero detected secrets, and Semgrep allows zero high-severity findings plus a custom rule that fails when a mutating service or controller method lacks an authorization annotation.
- The container image scan gate allows zero critical image CVEs and emits a CycloneDX SBOM artifact bound to the image digest; no registry push occurs unless all scan gates pass.
- push:ecr signs the image, records the immutable digest as a pipeline output, and downstream deploy stages reference the digest rather than a mutable tag.
- The dev deploy stage runs Flyway against a freshly created empty database and fails the pipeline if any migration does not apply cleanly or if migrations are not applied in a repeatable order.
- Cross-epic gates (golden-output batch regression comparison, the build-time unclassified-table check across all 55 tables, and the WCAG 2.1 AA accessibility scan) are present as fail-closed placeholder steps, each documenting the owning epic and the exact pass condition, so the pipeline fails rather than skips when the artifact is absent.
- The production approval gate rejects an approval submitted by the same identity that authored the release commit, enforcing separation of duty, and records approver identity, timestamp and release digest in an immutable audit record.
- Unit tests: pipeline definition lint and policy tests assert gate presence, ordering, parallel scan fan-out and the fail-closed behaviour of placeholder steps, all passing.
- System integration tests: an end-to-end pipeline run on a scratch branch proves each gate can both pass on a clean commit and fail on a deliberately seeded violation (uncovered monetary method, planted fake secret, unannotated mutating method, critical CVE dependency), with evidence captured per gate.
- Mock data/fixtures: seeded violation fixtures and a synthetic parity dataset used by the gate-failure tests are committed to the repository so the verification run needs no external data.

**Depends on:** WO-010, WO-011, WO-013

### [P1] Establish GitOps delivery with fifteen-minute rollback

Rollback in PCIS today means restoring a saved IBM i library — an unbounded, manual, undocumented recovery path with no tested time budget. This story establishes GitOps continuous delivery with Argo CD as the reconciler and Helm as the packaging format for all eight services plus the batch runtime, with a target rollback execution time of 15 minutes or less. Each service gets a Helm chart with environment value overlays for dev, test and prod, a securityContext matching the hardened non-root read-only image, resource requests and limits, probe wiring, and a default-deny NetworkPolicy. Argo CD applications are declared per environment with automated sync for dev and test and gated sync for prod, retaining at least the previous five release revisions so a rollback targets a known signed digest. Critically, the rollback procedure must cover both paths: the common case where coexistence migrations are additive-only (expand-then-contract) and a Helm rollback to the prior digest is sufficient, and the harder case where a Flyway migration ran and either a paired down-migration or a point-in-time restore is required. Both paths must be tested, timed and captured in a runbook rather than assumed.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P1 |
| Labels | epic:EPIC-01, type:cicd, area:gitops, complexity:medium, reliability |

**Acceptance Criteria**
- Helm charts exist for all eight services and the batch runtime with value overlays for dev, test and prod, each chart setting a non-root securityContext, read-only root filesystem, dropped capabilities, resource requests and limits, and liveness, readiness and startup probes matching the WO-011 image contract.
- Argo CD Application manifests exist per service per environment with automated self-healing sync for dev and test, manual sync for prod, and revision history retaining at least the previous five releases so any of them can be rolled back to by digest.
- A default-deny NetworkPolicy is applied per namespace and each chart declares only the explicit ingress and egress it requires, verified by a policy test that fails when a chart requests unrestricted egress.
- Rollback path A (no schema change) is executed as a timed drill: a bad release is deployed to a non-production environment and rolled back to the prior signed digest in 15 minutes or less, with the elapsed time recorded in the runbook.
- Rollback path B (schema change) is executed as a timed drill: a release including a Flyway migration is rolled back using either the paired down-migration or a point-in-time restore, and the runbook documents the decision criteria, the additive-only expand-then-contract policy that makes path A the normal case, and the measured recovery time.
- Chart values contain no secret material; all credentials are mounted from the managed secret store provisioned by WO-013 using external secret references, and a policy test fails the build if a literal password, token or connection-string credential appears in any values file.
- Unit tests: helm template and helm lint tests plus policy assertions on securityContext, probes, resource limits, NetworkPolicy defaults and absence of secret literals for every chart and every environment overlay, all passing.
- System integration tests: an end-to-end deploy of a service and the batch runtime through Argo CD into the dev environment reaches Healthy and Synced, and both rollback drills are executed as automated or scripted verification runs with captured evidence.
- Mock data/fixtures: a synthetic release pair (good digest and deliberately failing digest) plus a paired up and down migration fixture are committed so the rollback drills can be reproduced without production data.

**Depends on:** WO-011, WO-012, WO-013

### [P1] Define batch CronJob manifests and exit-code contract

All six PCIS batch programs share a hand-copied skeleton that ends in STOP RUN without ever setting a non-zero return code: 8000-WRITE-RUN-LOG inserts into RPT_RUN_LOG_T and commits unconditionally even when the error counter is greater than zero, and 9000-TERMINATE only closes the cursor and DISPLAYs counters. A failed nightly run therefore looks successful to any scheduler. This story delivers Kubernetes Job and CronJob manifests for the six jobs (prm005b-daily-premium, clm006b-claim-payment, aud002b-audit-archive, pol006b-renewal, bil003b-billing-generation, cmm001b-commission) together with a documented exit-code contract that maps legacy error signals — WS-CNT-ERRORS, the archive count-mismatch switch, cursor-open failure and audit-write failure — onto explicit non-zero process exit statuses, so a failure surfaces as a failed Job and an alert rather than a console line. Each job runs under its own workload principal (for example svc-claim-payment-job) replacing the compiled-in literals BATCHPRM, BATCHCLM, BATCHAUD, BATCHREN, BATCHBIL and BATCHCMM, carries externalized tunables from configuration rather than WORKING-STORAGE literals, and scales to zero between runs. Because the Spring Batch jobs themselves are owned by other epics, the manifests are authored against the job-name and exit-code contract, and the CronJob schedules ship as explicitly flagged assumptions because the actual batch-window clock times are unavailable in the repository.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | epic:EPIC-01, type:platform, area:batch, complexity:medium, reliability, observability |

**Acceptance Criteria**
- Six Kubernetes manifests exist — prm005b-daily-premium, clm006b-claim-payment, aud002b-audit-archive, pol006b-renewal, bil003b-billing-generation and cmm001b-commission — each declaring a CronJob with a schedule, a concurrencyPolicy of Forbid, a backoffLimit, an activeDeadlineSeconds bounding its window, ttlSecondsAfterFinished and the WO-011 batch image referenced by digest.
- A documented exit-code contract file maps every legacy failure signal to a distinct non-zero exit status: at least accumulated item errors above threshold, archive verification count mismatch, cursor or query initialization failure, audit-write failure, and configuration-validation failure; exit zero is reserved for a completed run within the error threshold including runs that processed zero items.
- The pcis-batch-common JobExecutionListener implements the contract so a job that would have DISPLAYed an error and continued now terminates the process with the mapped non-zero status, and a corresponding unit test proves each mapped code.
- Each job binds to its own ServiceAccount and workload principal (for example svc-claim-payment-job) with least-privilege database and secret access; no manifest references a literal actor name such as BATCHCLM and no credential value appears in any manifest.
- Externalized tunables are supplied via ConfigMap and secret references rather than compiled constants, covering at minimum retention days (365), archive chunk size (at most 1000, reduced from 5000), billing lead days (15), payment grace days (10), renewal window days (60) and the reinsurance referral threshold (100000.00); changing a value requires no image rebuild.
- Commit-granularity intent is recorded per job in the manifest annotations and the runbook: chunk size of one item for policy, installment and payment jobs preserving the legacy one-item-per-commit semantics, and chunk size of at most 1000 for the audit archive job.
- CronJob schedule values are explicitly annotated as unvalidated assumptions with a reference to the outstanding batch-window baseline measurement, and the runbook states that the 25 percent window-headroom criterion cannot be asserted until those baselines are measured.
- Scale-to-zero is verified: after all Jobs complete, no batch pods remain and the node capacity provisioned in WO-013 is released within the documented window.
- Unit tests: exit-code mapping tests for every failure signal plus manifest schema and policy tests asserting concurrencyPolicy Forbid, digest-pinned image, non-root securityContext, per-job ServiceAccount, activeDeadlineSeconds presence and absence of literal actors and secrets, all passing.
- System integration tests: each CronJob is triggered manually in the dev environment using a stub job honouring the exit-code contract, proving that a success run reports Job Succeeded and each seeded failure mode reports Job Failed with the expected exit status and an emitted alert.
- Mock data/fixtures: a stub batch runner plus synthetic seed data per job (paid installments, approved reserves, aged audit rows, expiring policies, billing candidates) are committed so the integration verification runs without external dependencies or real customer data.

**Depends on:** WO-011, WO-013, WO-014

---

## Observability, Structured Error Handling, SLOs and Runbooks

### [P0] Build shared observability starter with PII-masking structured logging

WHAT & WHY: PCIS today has exactly one telemetry mechanism — COBOL DISPLAY statements to the job log plus a single RPT_RUN_LOG_T summary row written by each of the six batch programs at the end of the run. There is no correlation identifier, no actor/resource/operation context, no metric, no trace, and raw restricted-tier PII (customer TAX_ID, DOB, EMAIL, PHONE, claim payee, address lines) flows unmasked into both job logs and AUDIT_LOG_T. This story creates the pcis-observability-starter Spring Boot auto-configuration module that every one of the eight target services and all six Spring Batch jobs will import, providing JSON structured logging with mandatory MDC context, Micrometer/OpenTelemetry metric and trace plumbing, and an emit-time PII masking converter. IMPACT: New shared Maven module pcis-observability-starter (logback configuration, MDC filters, masking converter, Micrometer configuration, OTel SDK auto-config, Spring Batch and @Async context propagation decorators); consumed by the Java replacements of AUD002B, BIL003B, PRM005B, CMM001B, CLM006B and POL006B, and by every REST service. Legacy COBOL sources are read-only reference for program names and counter semantics. WHAT DONE LOOKS LIKE: Any service or batch job that adds the starter dependency emits single-line JSON logs containing correlation_id, service, program, job_id, run_id, actor, resource and operation; no restricted-tier value appears in clear text in any emitted log line; Micrometer registry is present with pcis.* naming conventions and OTLP export configured; correlation context survives Spring Batch chunk threads, partitioned steps, @Async executors and Kafka consumers. SCOPE BOUNDARIES: Does NOT define the metric catalogue, dashboards or alert rules (WO-021); does NOT define reason codes, ProblemDetail responses or batch exit-code policy (WO-022); does NOT author runbooks (WO-023); does NOT implement the audit-svc transactional outbox or data classification tier assignment for the 55 tables. DEPENDENCIES: Requires only the Maven multi-module build skeleton and shared BOM from the foundation epic; all other EPIC-02 stories depend on the MDC keys and metric naming produced here.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | complexity:high, observability, logging, pii, platform, phase-0 |

**Acceptance Criteria**
- Adding the pcis-observability-starter dependency to a bare Spring Boot service produces JSON-encoded log output (logstash-logback-encoder) with the mandatory fields correlation_id, service, program, actor, resource, operation, and, for batch runtimes, job_id and run_id — verified by a test that parses emitted log lines as JSON and asserts field presence.
- A Logback masking converter redacts every configured restricted-tier field before emission: TAX_ID rendered as last four characters only, EMAIL rendered as domain only, PHONE, DOB, CUSTOMER_CONTACT_T contact values, address lines and claim payee name fully masked; a unit test feeds each pattern and asserts zero clear-text leakage including when the value appears inside an exception message or a serialized object.
- A Jackson serializer module masks the same annotated fields before any object is written to an audit event payload, proving masking happens at creation and not at display time.
- Correlation and MDC context propagate across a Spring Batch chunk-oriented step using a TaskExecutor, a partitioned step, an @Async method and a Kafka consumer — integration test asserts the same correlation_id appears in log lines from every hop.
- Micrometer MeterRegistry and the OpenTelemetry SDK are auto-configured with pcis.* metric naming, common tags for service, env and job, and OTLP endpoint configuration bound through @ConfigurationProperties with sane defaults and no hard-coded endpoints or credentials.
- An HTTP filter and a batch JobExecutionListener generate a correlation_id when absent and honour an inbound trace header, and both write actor identity from the authenticated principal (never from a hard-coded literal such as BATCHBIL or BATCHAUD).
- Unit tests written and passing for the masking converter, MDC filter, batch listener and context decorators with at least 90 percent line coverage on the starter module.
- System integration tests validate the starter inside a minimal Spring Boot service and a minimal Spring Batch job, asserting log shape, metric registration and context propagation end to end.
- Mock data and fixtures committed: sample customer records containing synthetic tax IDs, emails, phones and DOBs, plus expected masked-output golden files, so the suite runs with no external dependency.
- A build-time scanner test asserts that no log statement in the starter or its sample modules interpolates an unmasked restricted-tier field, and it fails the build when a violation is introduced.

**Depends on:** Maven multi-module build and shared BOM, Spring Boot 3.5.x baseline

### [P0] Implement structured error library with reason-code registry

WHAT & WHY: The legacy batch programs swallow errors by design — a non-'00' return from AUDLOG01 produces only a DISPLAY line and the financial mutation stays committed (PRM005B even documents this in a source comment), a failed cursor OPEN is converted into normal end-of-cursor so the job reports success with zero counts, CLM006B's RECOVERY_T insert failure above the 100,000.00 cession threshold is completely invisible, BIL003B silently drops candidates outside the lead window, POL006B checks no SQLCODE on its POLICY_HISTORY_T inserts, and no job ever exits non-zero. Organization policy forbids silently swallowing errors, forbids failing open, and forbids leaking stack traces. This story delivers the shared pcis-error module: an RFC 9457 ProblemDetail surface for all REST endpoints, a versioned reason-code registry harvested from the existing <MOD>#### message-file convention, a retryable-versus-terminal exception hierarchy, and Spring Batch listeners that persist structured exception records and drive non-zero job exit status on configured error thresholds. IMPACT: New shared module pcis-error (exception hierarchy, reason-code registry, ControllerAdvice, batch listeners, exception record entity and repository); consumed by every REST controller and by the Java replacements of AUD002B, BIL003B, PRM005B, CMM001B, CLM006B and POL006B. WHAT DONE LOOKS LIKE: Every API error returns an RFC 9457 problem document with a stable machine-readable code, correct HTTP status and an errors array modelled on the legacy 20-entry message table; no response ever carries SQLSTATE, SQLCODE, a stack trace or a raw business key belonging to another actor; every previously invisible batch failure produces a structured exception record plus a counter increment; and a job whose error count exceeds its configured threshold exits non-zero instead of reporting success. SCOPE BOUNDARIES: Does NOT build the dashboards or alert rules that consume the new counters (WO-021); does NOT author runbooks (WO-023); does NOT implement the JSON logging or masking plumbing (WO-020, assumed complete); does NOT implement the authorization service itself, only reserves and defines its denial reason codes; does NOT reproduce the legacy swallow-and-continue behaviour. DEPENDENCIES: Blocked by WO-020 for MDC keys (correlation_id, actor, resource, operation) and masking so that error payloads and exception records are context-rich and PII-safe.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | complexity:high, error-handling, api-contracts, batch, compliance, phase-0 |

**Acceptance Criteria**
- A shared reason-code registry exists as a versioned artifact seeded from the legacy <MOD>#### convention, including at minimum the evidenced codes for concurrent-update conflict, dependency-blocked delete, all-blank search, result-cap reached and generic system error, plus new codes for audit-write failure, cursor-open failure, archive-verification mismatch, skip-outside-lead-window, unrecognised billing frequency, no-active-commission-plan, commission arithmetic size error, sequence-allocation failure, rating-service non-success, reinsurance-referral write failure, authorization-denied-no-approval and authority-limit-exceeded; a unit test asserts uniqueness, non-reuse and presence of a client-safe title for every code.
- All REST error responses are RFC 9457 problem documents carrying type, title, status, detail, instance, a stable code, correlation_id and an errors array capped at 20 entries with per-entry code, detail and field pointer; HTTP status mapping is 400 for invalid input, 401 unauthenticated, 403 forbidden, 404 not found, 409 conflict, 422 business-rule rejection, 500 unexpected.
- A contract test asserts that no problem document field ever contains SQLSTATE, a native SQL error code, a stack frame, an internal class name or a restricted-tier value, while the same failure written to the structured log does carry the internal diagnostic detail for support.
- A two-tier exception hierarchy exists with an explicitly retryable branch and an explicitly terminal branch mirroring the legacy recoverable-versus-fatal switch semantics, and every custom exception carries a reason code, actor, resource and operation.
- Batch integration tests prove each previously invisible failure now surfaces: an audit-write failure rolls back the mutation and increments the audit-write-failure counter; a cursor-open failure fails the step with a distinct reason code and is distinguishable from a genuine zero-row run; a candidate skipped outside the lead window produces a skip record and increments a skipped counter; a reinsurance referral insert failure produces an error rather than silence; policy history insert failures are detected.
- A job whose error count exceeds its externally configured threshold completes with a failed BatchStatus and the Kubernetes Job container exits with a non-zero code; a job below threshold exits zero — both asserted by integration tests.
- A zero-row successful run still emits a complete run-log record and zero-valued metric series identical in shape to a non-empty run, so dashboards remain continuous, while a zero-row failed run is distinguishable by status and reason code.
- Unit tests written and passing for the registry, ControllerAdvice mapping, exception hierarchy, threshold evaluation and each batch listener, with at least 90 percent line coverage on the pcis-error module.
- System integration tests validate error handling across the REST boundary using MockMvc or WebTestClient and across the batch boundary using an in-memory JobLauncherTestUtils against a Testcontainers PostgreSQL instance.
- Mock data and fixtures committed: seeded rows reproducing each failure scenario (duplicate tax ID, unrecognised billing frequency, agent with no in-force commission plan, reserve below paid-to-date, archive verification mismatch) plus expected problem-document JSON golden files.

**Depends on:** pcis-observability-starter MDC keys and masking, Normalised audit contract widths and action-code domain

### [P1] Publish baseline metrics, SLO dashboards and alert rules

WHAT & WHY: PCIS has no measurable operational baseline. RPT_RUN_LOG_T records only a run date and an end timestamp with no run-start and no duration, so nightly and monthly batch-window durations are unknown, and there is no interactive latency measurement of any kind. Three programme guardrails — at least 25 percent batch-window headroom, API p95 no worse than the measured baseline, and a batch-window utilisation report — are currently uncomputable. This story first captures and publishes the baseline (instrumenting the legacy and migrated runs to record run-start, duration and per-job query counts), then defines the pcis.* metric catalogue, commits Grafana dashboards and Prometheus alert rules as code, and wires alerting to the reason codes produced by the error library. IMPACT: New observability configuration directory holding dashboards-as-code and Prometheus rule files; metric registration in the six batch job modules and all services via the observability starter; a baseline measurement report committed to the repository; CI validation of dashboard and rule syntax. WHAT DONE LOOKS LIKE: Every batch job and service emits the agreed pcis.* metrics with consistent tags; a committed baseline document records measured durations and latencies per job and per endpoint group; dashboards render per-job and per-service panels including zero-valued series on quiet days; alert rules fire on the first audit-write failure, on archive verification mismatch, on batch-window headroom below 25 percent, on error rate at or above 1 percent over a rolling 24-hour window, on authorization denials and on authorization-decision latency breaches; rule files are unit-tested with promtool-style assertions in CI. SCOPE BOUNDARIES: Does NOT provision Prometheus, Grafana or Alertmanager infrastructure (assumed available from the platform epic); does NOT author runbooks or escalation procedures (WO-023); does NOT implement the logging, masking or metric plumbing itself (WO-020); does NOT define reason codes (WO-022); does NOT set business SLA targets beyond the numbers already fixed in the requirements. DEPENDENCIES: Blocked by WO-020 for metric naming and tags and by WO-022 for reason codes and run status semantics.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | complexity:medium, observability, slo, alerting, devops, phase-0 |

**Acceptance Criteria**
- A committed baseline report records, per batch job (AUD002B, BIL003B, PRM005B, CMM001B, CLM006B, POL006B replacements), the measured run duration, row counts, per-run database query count and the declared scheduling window, plus measured p95 latency per API endpoint group; every threshold used later in this story references a value from this report or from an already-fixed requirement number.
- A versioned metric catalogue document and matching code registration exist for at least: batch items selected, processed, errors and skipped; job duration seconds; batch window utilisation ratio; audit write failures; archive chunk archived and deleted; archive verification mismatch; commission no-plan count and total amount; delinquent count; reinsurance flagged count and flag failures; authorization denials; database query count per run — all tagged with job, program, run_id and env.
- Grafana dashboards are committed as JSON-as-code with one dashboard per batch job and one per service, and a rendering test or provisioning dry-run proves each dashboard loads without unresolved datasource or variable references.
- Zero-row runs still render continuous series on dashboards (no gaps) while a failed run is visually and query-distinguishable from a quiet run by run status and reason code — asserted by a query test against seeded metric samples.
- Prometheus alert rules are committed and unit-tested with rule-evaluation fixtures for: audit-write failures greater than zero paging on first occurrence; archive verification mismatch triggering quarantine severity; batch-window headroom below 25 percent; production error rate at or above 1 percent of requests over a rolling 24-hour window; authorization denial spike; authorization decision latency p99 above 25 milliseconds; token validation above 5 milliseconds; API p95 regression against the committed baseline.
- Every alert rule carries labels for severity, service or job, and a runbook reference key, and an automated test asserts no alert exists without a severity and a runbook key.
- Unit tests written and passing for the metric registration helpers and for alert-rule expressions using recorded-sample fixtures.
- System integration tests scrape metrics from a running sample service and a completed sample batch job and assert the full expected metric set with correct tags is present.
- Mock data and fixtures committed: synthetic metric sample series covering a normal run, a zero-row run, a failed run, a window-overrun run and an audit-failure run, so alert-rule tests execute with no external dependency.
- A CI pipeline step validates dashboard JSON and Prometheus rule syntax and fails the build on any malformed or untested rule.

**Depends on:** Prometheus, Grafana and Alertmanager availability, pcis-observability-starter metric plumbing, pcis-error reason codes and run status

### [P1] Author operational runbooks for batch, rollback, purge and incidents

WHAT & WHY: Operating PCIS today depends entirely on tribal knowledge: the only automation members are an object-creation CL and a scheduler CL, promotion is a manual library copy from INSDEV to INSTST to INSPRD, and there is no documented restart, rollback, purge-verification or incident procedure. Once observability, structured errors and alerting exist, operators need executable procedures that turn each alert into a bounded recovery action with a measurable mean time to recovery. This story authors four versioned runbooks in the repository — batch restart, deployment rollback, purge and archive verification, and incident response — each cross-referenced to the alert catalogue by the runbook reference keys added in the alerting story, and each backed by an automated fault-injection or drill test where mechanisable. IMPACT: New docs/runbooks directory with four markdown runbooks plus an alert-to-runbook index; a fault-injection test suite proving the documented batch restart procedure actually recovers with zero duplicate and zero orphaned financial records; corrections to the audit archive documentation to record the defective legacy verification logic and specify the per-chunk identity verification successor. WHAT DONE LOOKS LIKE: Every alert in the catalogue resolves to exactly one runbook section; each runbook states trigger, severity, first responder, diagnostic queries, recovery steps, verification steps, rollback path and escalation; the batch-restart runbook names each job's commit boundary and target chunk size; the rollback runbook states the fifteen-minute execution target and the decision tree between a paired down-migration and a point-in-time restore; the purge runbook documents the legacy verification defect and the partition-detach successor with quarantine behaviour; and a documentation test fails the build if an alert exists with no matching runbook section. SCOPE BOUNDARIES: Does NOT implement the purge job, partitioning, object-lock storage or the rollback automation itself; does NOT create dashboards or alert rules (WO-021); does NOT define reason codes (WO-022); does NOT provision infrastructure or perform a production drill sign-off. DEPENDENCIES: Blocked by WO-020, WO-022 and WO-021 because runbook content references the MDC keys, reason codes, exit-code semantics, metric names and alert reference keys those stories produce.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | complexity:medium, documentation, runbook, reliability, compliance, phase-0 |

**Acceptance Criteria**
- Four runbooks are committed under a docs runbooks directory — batch restart and recovery, deployment rollback, purge and archive verification, incident response — each with a fixed section template covering trigger and alert reference, severity, first responder, prerequisites, diagnostic queries and log filters, step-by-step recovery, verification, rollback path, escalation and post-incident actions.
- An alert-to-runbook index maps every alert defined in the alerting story to exactly one runbook section by its runbook reference key, and an automated documentation test fails the build when an alert has no matching section or a section references a non-existent alert.
- The batch restart runbook documents, per job, the evidenced legacy commit boundary and the target chunk configuration — one item per commit for policy, installment, commission and claim-payment jobs and no more than one thousand rows per commit for the audit archive job — plus the restart command, the expected exit-code semantics and how to confirm restart resumed from the last committed chunk.
- A fault-injection integration test suite proves the documented restart procedure works: for each job, a simulated failure mid-run followed by a restart yields zero duplicate and zero orphaned financial records, and no audit row is deleted without a verified archive copy — assertions run against a Testcontainers PostgreSQL instance with committed seeded fixtures.
- The rollback runbook states the fifteen-minute execution target, the Helm or GitOps revision rollback command sequence, the decision tree between a paired Flyway down-migration and a point-in-time restore, and the note that coexistence migrations are additive-only so most rollbacks require no schema change.
- The purge and archive verification runbook explicitly records that the legacy archive verification compares a count of all historical archive rows below the cutoff against the current chunk insert count and therefore does not verify the chunk, specifies per-chunk key-set or checksum identity verification as the successor, and documents the quarantine-and-alert behaviour on verification mismatch instead of a silent halt.
- The incident response runbook includes an unrecorded-mutation reconciliation procedure that reconciles audit-write-failure alert counts against mutation rollback counts, plus alert dedup window, severity ladder and escalation contacts expressed as roles rather than named individuals.
- All runbooks include the environment and library topology context an operator needs — INSDEV with INSDEVDTA, INSTST with INSTSTDTA, INSPRD with INSPRDDTA identified as the only library holding real customer data, INSCOM shared service programs and message files, INSTOOLS scheduler and build objects — and note the interim legacy build job that must remain runnable during coexistence for parallel-run comparison.
- Unit tests written and passing for the documentation linter that validates runbook template completeness and the alert-to-runbook index consistency.
- System integration tests: the fault-injection restart suite executes in CI against seeded data for all six migrated jobs and passes.
- Mock data and fixtures committed: seeded audit, billing, commission, claim reserve and policy rows plus fault-injection configuration so the restart and verification drills run with no external dependency.
- No runbook contains a real secret, credential, endpoint or connection string; all such values appear only as named placeholders.

**Depends on:** Alert catalogue and runbook reference keys, Batch exit-code and reason-code semantics, Structured log MDC keys for diagnostic filters

---

## Shared Kernel — Audit Logging Service (replacing AUDLOG01)

### [P0] Build audit-svc core with versioned v1 audit event contract

WHAT & WHY: The AUDLOG01 audit writer is referenced by name in every mutating COBOL program (BIL003B, CMM001B, PRM005B, POL006B, CLM006B, CUS001A, POL001A) but has no source member anywhere in the repository, so the immutable audit trail the organization relies on for SOC 2 / SOX evidence is a convention rather than a component. Worse, the nine-parameter call drifts by caller: batch programs pass a 3-character action code with X(30) old/new values and an X(30) key, while interactive programs pass a 1-character action code with X(100) values and an X(40) key — meaning a X(100) before-image passed into an X(30) field silently loses 70 characters of evidence. This story stands up audit-svc as a first-class Spring Boot service with a single versioned v1 audit event contract that takes the widest of every legacy field, an explicit enumerated action domain, and a documented mapping from both legacy shapes so no historical value can be truncated. IMPACT: New Maven module audit-svc under the target Java platform (domain, application, infrastructure, controller layers per the four-layer service standard), a shared audit-contract artifact consumed by all six domain services and the Spring Batch jobs, Flyway migrations creating the PostgreSQL audit_log table partitioned monthly with a BIGINT identity surrogate key, and an OpenAPI 3.1 document published from code via springdoc. The legacy COBOL callers listed in CUS001A.cbl, POL001A.cbl, BIL003B.cbl, CMM001B.cbl, PRM005B.cbl and POL006B.cbl are the behavioural reference for field widths and action codes. WHAT DONE LOOKS LIKE: A running audit-svc exposes POST /v1/audit-events and accepts every legacy field combination without truncation; the enumerated action domain covers ADD, UPD, DEL, PAY, REN and their single-character legacy equivalents; a contract test proves the widest-field mapping preserves a 100-character before-image and a 40-character business key; every audit row carries actor, timestamp, resource (table plus key), operation, field, old value, new value, program/service name and a correlation id. SCOPE BOUNDARIES: No transactional outbox (WO-031), no PII masking or classification enforcement (WO-032), no retention/partition detach or purge (WO-033), no query/inquiry API or unmask action (WO-034), and no changes to the COBOL programs themselves. DEPENDENCIES: Requires the platform build pipeline and PostgreSQL 17 provisioning from the foundation epic; is a blocker for WO-031 through WO-034 and for every domain service that must record mutations.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | epic:shared-kernel, audit, backend, spring-boot, contract, complexity:medium |

**Acceptance Criteria**
- audit-svc starts as a Spring Boot 3.5.x service on Java 21, exposes an actuator health endpoint returning UP, and publishes an OpenAPI 3.1 document for POST /v1/audit-events.
- The v1 audit event contract accepts table name up to 30 chars, business key up to 40 chars, field name up to 30 chars, old and new values up to 100 chars each, actor up to 10 chars, and source program/service up to 64 chars, with no silent truncation on any field.
- Action code is an explicit enumeration; both legacy 3-character codes (ADD, UPD, PAY, REN) and legacy 1-character codes (A, C, D) map to canonical enum values via a documented mapping table, and an unknown code is rejected with HTTP 400 and an RFC 9457 problem detail.
- Flyway migration creates audit_log with a BIGINT GENERATED ALWAYS AS IDENTITY surrogate key, monthly range partitioning on the event timestamp, and no UPDATE or DELETE grant to the application role, verified by an integration test asserting an UPDATE attempt fails.
- Unit tests written and passing for contract validation, action-code mapping and width-preservation logic, with at least 90% line coverage on the mapping and validation classes.
- System integration tests using Testcontainers PostgreSQL validate the POST /v1/audit-events boundary end to end: request accepted, row persisted into the correct monthly partition, response returns the generated audit id.
- Mock data and fixtures committed: a JSON fixture set covering one sample event per legacy caller shape (batch X(3)/X(30) and interactive X(1)/X(100)) so the suite runs with no external dependency.

**Depends on:** Platform CI/CD pipeline and PostgreSQL 17 environment, Shared Maven BOM / parent POM

### [P0] Make audit writes atomic with mutations via transactional outbox

WHAT & WHY: In BIL003B, CMM001B, PRM005B, POL006B and CLM006B a non-'00' return from CALL 'AUDLOG01' produces only a DISPLAY line and the run continues — PRM005B even documents it in a comment stating the audit write failure does not roll back the already-determined status change. Every one of those paths can leave a committed financial mutation with no audit record, which is an unrecorded money movement and a reportable SOC 2 / SOX finding. This story makes the audit event and the business mutation commit or roll back together using a transactional outbox: the domain service writes the mutation and the outbox row in one local transaction, and a relay publishes to audit-svc with at-least-once delivery and idempotent consumption. An audit-write failure must fail the whole mutation rather than be logged and ignored. IMPACT: A shared audit-outbox library used by claims-svc, billing-svc, premium-svc, policy-svc, customer-svc and the Spring Batch jobs; a new audit_outbox table per owning service database with Flyway migration; an outbox relay component with retry, dead-letter and alerting; an idempotency key on audit-svc so replays do not duplicate rows; and a regression test explicitly asserting the legacy continue-after-audit-failure behaviour is NOT reproduced. WHAT DONE LOOKS LIKE: Injecting a failure in the audit path rolls back the paired financial mutation with zero orphaned rows; the outbox relay retries with backoff and, after the configured attempt ceiling, moves the record to a dead-letter state and raises an alert on the first failure; replaying the same outbox record twice yields exactly one audit row; a fault-injection test proves 0 committed mutations without a matching audit event. SCOPE BOUNDARIES: Does not implement masking or classification (WO-032), retention or purge (WO-033), or the audit query API (WO-034). Does not migrate the COBOL programs themselves and does not change business logic in any domain service beyond wiring the outbox at the existing transaction boundary. DEPENDENCIES: Requires the v1 audit contract and audit-svc endpoint from WO-030.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:shared-kernel, audit, reliability, transactional-outbox, backend, complexity:high |

**Acceptance Criteria**
- A shared audit-outbox library provides a single API that enlists an audit event in the caller's active transaction; calling it outside a transaction fails fast with a descriptive error rather than writing anything.
- Flyway migration adds an audit_outbox table (id, payload JSONB, idempotency_key UUID unique, status, attempt_count, next_attempt_at, created_at, last_error) with an index supporting the relay claim query.
- Fault-injection test: with audit persistence forced to fail, the paired financial mutation is rolled back and the database shows 0 mutated rows and 0 audit rows — proving the legacy PRM005B continue-after-failure behaviour is not reproduced.
- Outbox relay publishes to audit-svc with exponential backoff, honours an externally configured max attempt count, transitions exhausted records to DEAD_LETTER, and emits a structured ERROR log plus an alert-capable metric on the very first failure.
- Idempotency: replaying the same outbox record or resending the same idempotency key to audit-svc results in exactly one persisted audit row, verified by an integration test.
- Unit tests written and passing for outbox enlistment, retry/backoff scheduling, dead-letter transition and idempotency key generation, with at least 90% line coverage on the outbox library.
- System integration tests validate the service boundary end to end with Testcontainers PostgreSQL: mutation plus outbox commit, relay publish, audit-svc persistence, and rollback-on-failure across the whole chain.
- Mock data and fixtures committed: seeded mutation scenarios for a billing installment, a commission ledger entry and a claim payment so the fault-injection suite runs with no external dependency.

**Depends on:** audit-svc v1 contract and POST /v1/audit-events endpoint

### [P1] Convert AUD002B archiving into restartable retention and purge job

WHAT & WHY: AUD002B computes a cutoff from a compiled-in WS-RETENTION-DAYS of 365, copies up to WS-CHUNK-SIZE of 5000 rows into AUDIT_LOG_ARCHIVE_T, counts the archive to verify, deletes the same chunk from the live table, and halts the entire run on any verification mismatch or failure — leaving older records unarchived until the next month-end. There is no purge stage at all, so the archive grows without bound carrying unmasked personal data. This story converts that job into a restartable Spring Batch job with externalized tunables, retention expressed as a partition-detach metadata operation rather than a mass DELETE, a genuine purge stage performing physical deletion or cryptographic erasure past each tier's retention period, and quarantine-plus-alert on verification failure instead of a silent halt. IMPACT: A new Spring Batch job module in audit-svc replacing AUD002B, Flyway migrations adding partition lifecycle metadata and a purge evidence run log, a Kubernetes CronJob manifest scaling to zero between runs, externalized configuration for retention days per tier and chunk size, cold-archive export to object storage with Object Lock and lifecycle expiry, and observability covering archived, detached, purged and error counts. WHAT DONE LOOKS LIKE: The job archives and detaches expired monthly partitions rather than issuing DELETE FROM audit_log; chunk size is externally configured at 1000 or less; a simulated mid-chunk database failure restarts from the last committed chunk with zero rows deleted without a verified archive copy and zero rows archived twice; records past their tier retention are physically purged within 24 hours of expiry and the evidence is recorded in the run log; a verification mismatch quarantines the affected partition, raises an alert and exits non-zero rather than halting silently; audit retention never drops below the 1-year policy minimum. SCOPE BOUNDARIES: Does not implement masking (WO-032 provides it), does not implement the audit query API or unmask action (WO-034), does not migrate the other five COBOL batch programs, and does not build the cross-domain golden-output harness beyond the fixtures this job needs. DEPENDENCIES: Requires the partitioned audit_log schema from WO-030 and the classification/tier artifact from WO-032 to drive per-tier retention.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P1 |
| Labels | epic:shared-kernel, audit, spring-batch, retention, compliance, complexity:high |

**Acceptance Criteria**
- A Spring Batch job replaces AUD002B with restart-from-last-committed-chunk behaviour; a fault-injection test kills the job mid-chunk, restarts it, and asserts zero duplicate archived rows and zero rows deleted from live without a verified archive copy.
- Chunk size, retention days per classification tier, and the archive verification tolerance are externalized via configuration properties with no compiled-in constants; the default commit chunk is 1000 rows or fewer (down from the legacy 5000).
- Retention is executed as a monthly partition detach plus cold-archive export rather than DELETE FROM audit_log; an integration test asserts no mass DELETE statement is issued against the live partitioned table.
- A purge stage performs physical deletion or cryptographic erasure of records past their tier retention and writes evidence (tier, partition, row count, method, operator/job identity, timestamp) into a purge run log; a test asserts 100% of records past expiry are removed within the configured 24-hour window.
- Audit retention never falls below the 1-year policy minimum: a configuration value below 365 days is rejected at startup with a descriptive error.
- A verification mismatch quarantines the affected partition, emits a structured ERROR log plus an alert metric, and the job exits with a non-zero status instead of silently halting mid-run.
- Unit tests written and passing for cutoff computation, tier retention resolution, chunk sizing, quarantine decision logic and purge eligibility, with at least 90% line coverage on the retention packages.
- System integration tests with Testcontainers PostgreSQL validate archive, verify, detach, cold-export and purge as an end-to-end sequence, including the restart and quarantine paths.
- Mock data and fixtures committed: seeded audit rows spanning multiple monthly partitions and multiple classification tiers, including rows exactly at the retention boundary, so the suite runs with no external dependency.

**Depends on:** Partitioned audit_log schema, Classification tier artifact driving per-tier retention, Object storage bucket with Object Lock and lifecycle policy

### [P0] Mask PII and classify data before audit persistence

WHAT & WHY: CUSTOMER_T carries TAX_ID, DOB, EMAIL and PHONE, and the CUS module specifies Full audit level with one row per changed field including OLD_VALUE and NEW_VALUE — so raw personal data flows through the audit path into AUDIT_LOG_T and then into AUDIT_LOG_ARCHIVE_T, which has no purge stage. Masking today is a display convention only: the panel renders a tax ID as last-four while the stored and audited value stays in the clear. Organization policy (GDPR/CCPA, ISO 27001) requires PII masked in logs, per-entity data classification and encryption at rest, so masking must be applied at the point the audit event is constructed, not at render time. IMPACT: A classification and masking module in the audit-contract library (annotation-driven Jackson serializer), a Logback masking converter applied to all application log output, a committed classification artifact assigning every one of the 55 tables to Public / Internal / Confidential / Restricted, a build-time gate that fails if any table or restricted field is unclassified, and an automated scanner that fails CI when an unmasked restricted value appears in audit rows or log output. WHAT DONE LOOKS LIKE: A tax ID persisted into audit_log renders as last four characters only, an email as domain only, a phone as last four, and a date of birth as year only; no unmasked restricted-tier value can reach audit_log or any log line; the build fails when a new table is added to the data dictionary without a tier; the automated scanner reports zero unmasked restricted values across the audit store and log pipeline. SCOPE BOUNDARIES: Does not implement the permission-gated unmask action or audit inquiry API (WO-034), does not implement retention or purge (WO-033), does not change the outbox mechanics (WO-031), and does not encrypt columns at rest beyond using the managed database encryption already provisioned. DEPENDENCIES: Requires the v1 audit contract and audit_log schema from WO-030; masking must be in place before WO-033 archives or purges data and before any real data leaves the source platform.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | epic:shared-kernel, audit, security, pii, compliance, complexity:medium |

**Acceptance Criteria**
- A committed classification artifact assigns a tier (Public, Internal, Confidential, Restricted) to all 55 tables listed in PCIS_Database_Design.md, and to every field identified as restricted including CUSTOMER_T.TAX_ID, DOB, EMAIL, PHONE and CLAIM_PAYMENT_T payee.
- A build-time check fails the Maven build when any table in the data dictionary has no tier or when a field marked restricted has no masking strategy assigned.
- Annotation-driven masking is applied before the audit event is constructed: tax ID renders as last four characters, email as domain only, phone as last four digits, date of birth as year only — verified by unit tests for each strategy.
- A Logback masking converter is registered so no application log line emits an unmasked restricted value; an integration test logs a full tax ID through the normal logger and asserts the emitted line contains only the masked form.
- An automated scanner runs in CI against seeded audit rows and captured log output and fails the build on any unmasked restricted-tier pattern (SSN-shaped, full email, full phone).
- Unit tests written and passing for every masking strategy including boundary inputs, with at least 90% line coverage on the masking and classification packages.
- System integration tests validate the boundary end to end: a customer mutation carrying tax ID, email, phone and DOB produces an audit row in PostgreSQL containing only masked values.
- Mock data and fixtures committed: a synthetic PII fixture set (never real customer data) covering each restricted field type so the masking and scanner suites run with no external dependency.

**Depends on:** audit-svc v1 contract and audit_log schema, PCIS_Database_Design.md table inventory

### [P2] Expose audit inquiry API with gated unmask and observability

WHAT & WHY: AUD001A is described as an audit inquiry program searching AUDIT_LOG_T by table, key, user and date range, restricted to audit and compliance roles — but it exists only as a design reference with no implementation, and there is no API surface of any kind. Compliance and Internal Audit therefore cannot self-serve evidence, and there is no permission-gated path for an authorised investigator to see an unmasked restricted value. This story delivers a versioned read API over the audit store with deny-by-default RBAC, an explicit permission-gated and itself-audited unmask action, and the observability layer (SLIs, dashboards, alerts, runbook) that makes audit-svc operable: audit ingestion latency, outbox pending depth, dead-letter count, purge run success and unmask usage. IMPACT: New read endpoints in audit-svc, Spring Security 6 method-level authorization on every endpoint, an unmask use case that records its own audit event naming the investigator and the field revealed, Micrometer/OpenTelemetry instrumentation, Prometheus alert rules and Grafana dashboard definitions committed as code, and a runbook covering outbox backlog, dead-letter drain, purge failure and quarantined partition recovery. WHAT DONE LOOKS LIKE: An authorised compliance user can query audit events by table, business key, actor and date range with pagination and receives masked values by default; an investigator holding the dedicated unmask permission can reveal a specific field and that reveal itself lands in the audit log; every unauthenticated request returns 401, every unauthorised request returns 403, and no endpoint is reachable without an explicit grant; dashboards show audit ingestion latency and outbox depth, and alerts fire on the defined SLO breaches. SCOPE BOUNDARIES: No web UI screens (the admin retention and classification UI is separate), no changes to the outbox relay mechanics (WO-031), no changes to masking strategies (WO-032), no changes to the retention job logic (WO-033), and no identity provider configuration beyond validating tokens issued by it. DEPENDENCIES: Requires the audit_log schema and v1 contract (WO-030), masking (WO-032) so reads are masked by default, and the outbox and purge metrics surfaces (WO-031, WO-033) to instrument.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P2 |
| Labels | epic:shared-kernel, audit, api, security, observability, complexity:medium |

**Acceptance Criteria**
- GET /v1/audit-events supports filtering by tableName, businessKey, actor, action and an inclusive date range, with keyset pagination and a bounded maximum page size, returning masked values by default.
- Deny-by-default authorization: every endpoint carries an explicit method-level permission check; an unauthenticated request returns 401, a request without the audit-read permission returns 403, and a security test enumerates all endpoints to prove none is reachable without a grant.
- POST /v1/audit-events/{auditId}/unmask requires a dedicated unmask permission, returns the unmasked field value only for the requested field, and writes its own audit event naming the investigator, the audit id, the field revealed and a mandatory justification.
- Micrometer and OpenTelemetry instrumentation exposes audit ingestion latency, ingestion error rate, outbox pending depth, dead-letter count, purge run outcome and unmask invocation count through the actuator Prometheus endpoint.
- Prometheus alert rules and a Grafana dashboard definition are committed as code with defined SLOs (for example ingestion p99 latency threshold, outbox pending depth threshold, any dead-letter row, any failed purge run) and each alert links to a runbook section.
- A runbook is committed covering outbox backlog drain, dead-letter reprocessing, purge failure recovery and quarantined-partition recovery, each with concrete verification commands and rollback guidance.
- Unit tests written and passing for query filter construction, pagination boundaries, authorization annotations and unmask justification validation, with at least 90% line coverage on the query and authorization packages.
- System integration tests validate the API boundary end to end with Testcontainers PostgreSQL and a mocked JWT issuer: filtered query results, 401/403 behaviour, masked-by-default responses, and the self-auditing unmask flow.
- Mock data and fixtures committed: seeded audit events across multiple tables, actors, actions and dates plus JWT fixtures for read-only, unmask-capable and unauthorised principals so the suite runs with no external dependency.

**Depends on:** audit_log schema and v1 contract, Masking and classification registry, Outbox and purge metric surfaces, OIDC identity provider issuing RS256 JWTs

---

## Shared Kernel — Authorization Service and Deny-by-Default Access Control

### [P0] Build authz-svc policy decision service with deny-by-default

1. WHAT & WHY: PCIS has no authorization component with compilable source — SECCHK01 is referenced by name in design documents and prologue CALLS lists but no member exists, and menu-option gating in ROLE_MENU_T is presentation-layer authorization only. This story creates the shared-kernel authorization service (authz-svc): a Spring Boot 3.5.x / Java 21 service that validates OIDC-issued JWTs, resolves a principal to roles and permissions, evaluates a deny-by-default permission model, and emits a structured authorization decision event (permit or deny with an explicit reason code) for every evaluation. This is the foundation on which claim-payment authority checks, endpoint enforcement, and batch principal authorization are built. 2. IMPACT: Adds a new Maven module for the authorization service to the repository alongside the legacy COBOL members; introduces the JWT validation filter, JWKS caching, the permission/role/permission-assignment domain model, the PolicyDecisionService, the RFC 9457 problem-detail error mapping, the authorization decision event publisher, and the Flyway migrations for the SEC-tier tables (roles, permissions, role_permission, user_role) in PostgreSQL. No existing COBOL member is modified. 3. WHAT DONE LOOKS LIKE: A POST decision endpoint accepts actor, resource, operation and optional context attributes and returns PERMIT or DENY with a reason code; any request for a resource/operation pair with no explicit grant returns DENY with reason NO_GRANT; an unauthenticated caller receives 401 and an authenticated-but-unauthorized caller receives 403, both as RFC 9457 problem details with no stack trace; every decision produces one structured JSON log line and one authorization decision event carrying actor, resource, operation, decision, reason code and correlation id. 4. SCOPE BOUNDARIES: This story does NOT implement the claim cumulative-authority rule or the APPROVAL_T linkage check (WO-041), does NOT annotate or enforce authorization on domain service endpoints (WO-042), does NOT introduce batch service principals (WO-043), and does NOT build the cross-service regression/control suite (WO-044). It also does not stand up the identity provider itself or provision Kubernetes/Terraform resources — it consumes an existing OIDC issuer via configuration. 5. DEPENDENCIES: Depends on the shared build/CI plumbing and PostgreSQL schema baseline established in the foundation epics; consumes the audit event contract defined in the audit shared-kernel epic for denial events (a local interface is used with a stub implementation if the audit client is not yet available).

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | complexity:high, epic:shared-kernel, security, authorization, spring-boot, P0 |

**Acceptance Criteria**
- A POST /v1/authz/decisions endpoint returns 200 with body containing decision (PERMIT or DENY), reasonCode, and evaluatedPermissions for a valid bearer token; missing or expired token returns 401 and an unmapped resource/operation returns decision DENY with reasonCode NO_GRANT.
- Deny-by-default is proven: an integration test asserts that a principal with zero role assignments is denied for every seeded resource/operation pair, and that adding a single grant flips exactly one pair to PERMIT with no side effects on others.
- JWT validation uses local RS256 verification against a cached JWKS (cache TTL configurable, default 1 hour) with no per-request call to the identity provider; a test with a token signed by an untrusted key returns 401 with RFC 9457 problem detail and no stack trace or secret in the response body.
- Every decision emits exactly one structured JSON log line containing actor, resource, operation, decision, reasonCode and correlationId; an automated assertion confirms no token, tax ID, email or phone value appears in the emitted line.
- Unit tests: PolicyDecisionService, permission resolution, JWKS cache and reason-code mapping have unit tests with at least 90 percent line coverage on the decision package and all tests passing in CI.
- System integration tests: Testcontainers-based tests boot the service against PostgreSQL and a mock OIDC issuer (WireMock) and validate the decision endpoint, 401/403 paths, and event emission end to end.
- Mock data/fixtures: SQL seed fixtures for roles, permissions, role_permission and user_role covering adjuster, supervisor, CSR, batch and unassigned principals are committed under the service test resources and used by all integration tests without external dependencies.

**Depends on:** PostgreSQL schema baseline and Flyway migration tooling, OIDC identity provider issuer URL and JWKS endpoint (configuration input)

### [P0] Enforce approval linkage and cumulative claim authority limits

1. WHAT & WHY: CLM006B selects CLAIM_RESERVE_T rows with RESERVE_STATUS equal to AP, computes the payment as approved amount minus paid-to-date, and inserts CLAIM_PAYMENT_T with status I — with no SECCHK01 call and no reference to CLAIM_ADJUSTER_T.AUTHORITY_LIMIT anywhere in the program, despite its prologue claiming it verifies payment authority. Supervisory approval is recorded only as free text in CLAIM_NOTE_T, so there is no machine-readable link between an approval and the payment it authorises (PCIS_Enterprise_Architecture.md section 7.4 open item 8). This story creates the approval record as a first-class entity and implements the two mandatory checks — a qualifying approval exists, and the payer's authority limit covers cumulative payout — as authorization rules with distinct reason codes. 2. IMPACT: Adds an approval aggregate and its lifecycle (REQUESTED, APPROVED, DENIED, CONSUMED, EXPIRED) with Flyway migrations for approval and its link to a payment request; adds a ClaimPaymentAuthorizationService to the authorization shared kernel that evaluates BR-01 (cumulative payout, not single transaction) and BR-02 (approval and disbursement are distinct controls); extends the reason-code enum with APPROVAL_MISSING and AUTHORITY_LIMIT_EXCEEDED; documents the resolved open item in CLM_Module_Design_Document.md and PCIS_Enterprise_Architecture.md. 3. WHAT DONE LOOKS LIKE: A payment authorization request for a claim with no linked APPROVED approval returns DENY with reasonCode APPROVAL_MISSING; a request where the adjuster authority limit is less than paid-to-date plus the requested amount returns DENY with reasonCode AUTHORITY_LIMIT_EXCEEDED even when the single amount alone is within limit; a request passing both checks returns PERMIT carrying the approver identity, the approval identifier and the authority limit applied so the caller can persist them into the audit event; an approval can be consumed exactly once and a second attempt to reuse it is denied. 4. SCOPE BOUNDARIES: This story does NOT write CLAIM_PAYMENT_T or update CLAIM_RESERVE_T — the claims domain service owns that transaction and consumes this decision; it does NOT annotate endpoints or add gateway enforcement (WO-042); it does NOT create batch service principals (WO-043); it does NOT build the reinsurance referral outcome tracking or the payee/vendor master; and it does NOT implement the approval work-queue user interface. 5. DEPENDENCIES: Blocked by WO-040 for the decision engine, reason-code enum, JWT principal resolution and decision-event publisher.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | complexity:high, epic:shared-kernel, security, segregation-of-duties, claims, P0 |

**Acceptance Criteria**
- A POST /v1/authz/claim-payments/decisions request for a claim with no APPROVED approval row linked to the payment request returns decision DENY with reasonCode APPROVAL_MISSING, and no approval row is created or mutated as a side effect.
- Given an APPROVED approval exists but CLAIM_ADJUSTER_T authority limit is less than paid-to-date plus requested amount, the response is DENY with reasonCode AUTHORITY_LIMIT_EXCEEDED — asserted by a test replicating BR-01: a 25000 limit, 20000 already paid and a further 10000 requested is denied even though 10000 alone is within limit.
- Given both checks pass, the response is PERMIT and includes approvalId, approverPrincipal, authorityLimitApplied and cumulativePaidToDate so the caller can record approver identity and the limit applied in the audit event.
- Approval single-use is enforced: consuming an approval transitions it to CONSUMED within the caller's transaction boundary, and a second decision request against the same approval returns DENY with reasonCode APPROVAL_ALREADY_CONSUMED under a concurrent-request test using two parallel threads.
- Segregation of duties is enforced in data and code: a test asserts that an approval whose approver principal equals the requesting payer principal is rejected with reasonCode SELF_APPROVAL_FORBIDDEN, and that approval and disbursement require distinct permission strings.
- Unit tests: cumulative authority arithmetic uses BigDecimal at scale 2 with HALF_UP and is covered by boundary tests at exactly equal to limit, one cent under and one cent over; all monetary calculation classes reach at least 90 percent line coverage.
- System integration tests: Testcontainers PostgreSQL integration tests exercise the full decision flow across approval lifecycle states (REQUESTED, APPROVED, DENIED, CONSUMED, EXPIRED) and assert one authorization decision event per evaluation with the correct reason code.
- Mock data/fixtures: committed fixtures seed claims, reserves with status AP, adjusters with varied authority limits, and approvals in every lifecycle state, so the suite runs offline with no external dependency.

**Depends on:** Claims domain data model for CLAIM_RESERVE_T, CLAIM_PAYMENT_T and CLAIM_ADJUSTER_T, Audit event contract for recording approver identity

### [P0] Apply deny-by-default guards to financial mutation endpoints

1. WHAT & WHY: Organization policy and OWASP A01 require deny-by-default, server-side access control on every financial mutation, yet PCIS today gates access only at the CL driver and menu-option level — presentation-layer authorization the design documents themselves describe as the sole gate. Exploration confirmed the financial-mutation surface is wider than claims: POL006B renews policies and re-rates PREM_ANNUAL, BIL003B creates money-due rows in BILLING_SCHEDULE_T and INVOICE_T, CMM001B posts COMMISSION_LEDGER_T entries and PRM005B changes installment status — all with zero authority evaluation. This story installs enforcement: a method-level authorization guard on every mutating application-layer use case, a default-deny security filter chain, and a build-time check that fails the pipeline if any mutating endpoint or batch step lacks an authorization annotation. 2. IMPACT: Adds shared security auto-configuration (a reusable Spring Boot starter module) consumed by all domain services; adds the default-deny SecurityFilterChain, the method-security configuration, an authorization client that calls the WO-040 decision endpoint (or evaluates locally from token claims where latency demands it), the RFC 9457 error mapping for 401/403, and a static-analysis CI step that enumerates mutating handler methods and batch step beans and fails on any unguarded one. 3. WHAT DONE LOOKS LIKE: Any HTTP request to a mutating endpoint without a valid token receives 401, with a valid token but no grant receives 403, and both responses are problem details with a reason code and no stack trace; the default filter chain denies any request path not explicitly permitted; the CI gate fails a deliberately unguarded test endpoint and passes once the guard is added; a coverage report enumerates every mutating endpoint with its required permission. 4. SCOPE BOUNDARIES: This story does NOT implement the claim cumulative-authority or approval-linkage rules (WO-041), does NOT create batch service principals or replace hard-coded actor literals (WO-043), does NOT build the segregation-of-duties regression and control-sampling suite (WO-044), and does NOT provision the API gateway, WAF, rate limiting or network policies, which belong to the platform epic. 5. DEPENDENCIES: Blocked by WO-040 for the decision endpoint, permission model and reason codes.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | complexity:medium, epic:shared-kernel, security, owasp-a01, ci-gate, P0 |

**Acceptance Criteria**
- A shared security starter module is published and consumed by at least one domain service; its default SecurityFilterChain denies every request that is not explicitly permitted, proven by a test asserting 401 or 403 on an unmapped path.
- Every mutating application-layer use case carries a method-level authorization guard referencing an explicit permission string; a committed inventory document or generated report lists each mutating endpoint with its required permission and HTTP method.
- The CI gate fails the build when a deliberately added unguarded mutating handler is present and passes once the guard is applied — demonstrated by a committed negative fixture that the gate detects and a green run after remediation.
- Unauthenticated requests return 401 and authorized-but-forbidden requests return 403, both as application/problem+json with a pcisReasonCode extension, no stack trace and no SQL or token material in the body.
- Unit tests: the authorization client, guard expression resolution, and the static-analysis gate logic each have unit tests with all cases passing in CI.
- System integration tests: Spring Security test slices plus Testcontainers integration tests assert the full 401 / 403 / 200 matrix for a representative mutating endpoint per financial surface (claim payment, policy renewal, billing generation, commission posting, installment status change).
- Mock data/fixtures: committed JWT fixtures and permission seed data for adjuster, supervisor, batch and unassigned principals allow the entire enforcement suite to run offline with no identity provider dependency.

**Depends on:** Domain service module skeletons for claims, policy, billing and premium, CI pipeline capable of running a custom gate step

### [P0] Replace batch actor literals with authenticated service principals

1. WHAT & WHY: Batch identity in PCIS is a compiled-in literal. Exploration verified 01 HV-CURRENT-USER PIC X(10) VALUE 'BATCHREN' in POL006B, 'BATCHBIL' in BIL003B and 'BATCHCLM' in CLM006B, with 'BATCHAUD', 'BATCHCMM' and 'BATCHPRM' in the remaining jobs — and no runtime derivation, no parameter and no override, unlike interactive CUS001A which retrieves CURRENT USER. Because there is no principal, there is nothing to authorize: batch financial mutations cannot be subjected to the deny-by-default control. Worse, the literal is stamped at roughly twenty-plus write sites (POL006B alone writes it to POLICY_T.CRT_USER, COVERAGE_T.CRT_USER, POLICY_T.UPD_USER, two POLICY_HISTORY_T rows and the audit actor field), so audit attribution is a fiction. This story introduces workload identity for batch: each job runs as a distinct, credentialed service principal whose token is exchanged at job start, propagated through the Spring Batch execution context and used both for authorization decisions and for actor stamping at every write site. 2. IMPACT: Adds a BatchPrincipalProvider and a Spring Batch JobExecutionListener to the shared security starter; adds an AuditActorContext (a request/job-scoped holder) consumed by JPA auditing so crt_user and upd_user are populated from the authenticated principal rather than a literal; adds configuration properties per job for its service principal identifier with credentials sourced from the managed secret store; removes the literal actor values from the migrated job configuration and documents the mapping from legacy literal to new principal. 3. WHAT DONE LOOKS LIKE: Starting any migrated batch job without valid workload credentials fails the job with a non-zero exit code before the first chunk is read; a successfully started job carries a resolvable principal for the whole execution; every persisted row and every audit event records the authenticated service principal identity, not a literal; the authorization guard from WO-042 evaluates successfully inside batch steps; and a report maps each of the six legacy actor literals to its replacement principal and permission set. 4. SCOPE BOUNDARIES: This story does NOT rewrite the COBOL batch programs into Spring Batch jobs — that is the batch conversion epic; it only supplies the identity, propagation and stamping mechanism plus its wiring points. It does NOT create the secret store or the Kubernetes workload identity binding (platform epic), does NOT change commit semantics or chunk sizes, and does NOT implement the claim authority rules (WO-041). 5. DEPENDENCIES: Blocked by WO-040 for principal resolution and the decision endpoint, and by WO-042 for the guard that batch steps will now satisfy.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | complexity:medium, epic:shared-kernel, security, spring-batch, observability, P0 |

**Acceptance Criteria**
- A batch job started without valid workload credentials fails before processing the first item, exits with a non-zero status, emits a structured error with job name, resource and operation, and writes no business row.
- A batch job started with valid credentials resolves exactly one service principal for the whole execution; an integration test asserts the principal is available in every step, item reader, processor and writer via the job-scoped actor context.
- Every persisted row written by a migrated job populates crt_user and upd_user from the authenticated principal — proven by a test that asserts no row contains any of the strings BATCHAUD, BATCHBIL, BATCHCMM, BATCHPRM, BATCHCLM or BATCHREN.
- The WO-042 authorization guard evaluates successfully inside a batch step: a job whose principal lacks the required permission is denied and the job exits non-zero rather than continuing, and a job whose principal holds the permission proceeds.
- A committed mapping document lists each of the six legacy actor literals, its replacement service principal identifier, its granted permission set and the write sites it previously stamped, with credentials referenced only as environment placeholders and never as literal secret values.
- Unit tests: BatchPrincipalProvider, the job execution listener, credential-failure handling and the actor context propagation each have unit tests with all cases passing.
- System integration tests: a Spring Batch integration test on Testcontainers PostgreSQL runs a representative job end to end asserting principal propagation, actor stamping on all write sites, non-zero exit on credential failure and denial on missing permission.
- Mock data/fixtures: committed service-principal seed data, permission grants and a mock token issuer allow the batch identity suite to run with no external identity provider or secret store.

**Depends on:** Managed secret store configuration surface for workload credentials, Spring Batch job infrastructure in the batch conversion epic

### [P0] Automate authorization regression and segregation-of-duties control evidence

1. WHAT & WHY: The programme commits to measurable controls — 100 percent of claim-payment writes pass a server-side authority check, zero disbursements persisted without a matching approval record, deny-by-default enforced on 100 percent of financial mutation endpoints, and a quarterly control test sampling 100 disbursements. Today none of that is measurable: there are no test members, no harness and no CI gate, so segregation of duties is a documented intention. This story builds the automated authorization regression suite and the control-evidence reporting that turns those objectives into pipeline outcomes: an executable test suite spanning interactive and batch payment paths, consumer-driven contract tests freezing the authorization service interface, a disbursement-integrity reconciliation query, and a machine-readable control evidence pack published on every build. 2. IMPACT: Adds a dedicated authorization regression test module using JUnit 5, Testcontainers, WireMock and Spring Cloud Contract or Pact; adds the disbursement-integrity reconciliation check (every claim payment row must have a linked CONSUMED approval and a recorded authority limit) as both a test and a scheduled operational query; adds a CI stage publishing the coverage of guarded endpoints, the denial reason-code matrix and the control evidence pack as build artifacts; adds alerting thresholds so an authorization-denied spike or an integrity break is observable. 3. WHAT DONE LOOKS LIKE: The suite fails the build if any financial mutation path can be executed without an authorization decision, if any claim payment can be written without a linked consumed approval, if any denial reason code is unreachable or undocumented, or if the authorization contract changes in a breaking way; a control evidence pack listing every mutating operation, its permission, its denial reason codes and the sampled reconciliation result is published per build; a fault-injection test proves an unavailable authorization service results in denial, never a permitted write. 4. SCOPE BOUNDARIES: This story does NOT implement the authorization service, the claim authority rules, the endpoint guards or the batch principals — it verifies the outputs of WO-040 through WO-043. It does NOT build the golden-output batch parity harness for monetary arithmetic (batch regression epic), does NOT implement PII masking scanners (data protection epic), and does NOT create dashboards or SLO alert routing beyond emitting the metrics and thresholds. 5. DEPENDENCIES: Blocked by WO-040, WO-041, WO-042 and WO-043, whose services, rules, guards and principals are the subject under test.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | complexity:medium, epic:shared-kernel, testing, compliance, ci-gate, observability, P0 |

**Acceptance Criteria**
- An authorization regression suite runs in CI and covers, for every financial mutation surface (claim payment, policy renewal, billing generation, commission posting, installment aging, audit purge), the cases permitted, denied for missing grant, denied for missing approval, denied for authority limit exceeded, and denied for unauthenticated caller — with the build failing on any gap.
- A disbursement-integrity check asserts that every claim payment row has exactly one linked CONSUMED approval and a non-null recorded authority limit; the test fails when a fixture inserts a payment without an approval, proving the control detects the violation.
- Consumer-driven contract tests (Spring Cloud Contract or Pact) freeze the authorization decision contract; a deliberately breaking change to a field name or reason-code value fails the build, and additive changes pass.
- A fault-injection test stubs the authorization service as unavailable or timing out and asserts that no financial mutation is persisted in any path, interactive or batch, and that the failure is logged with actor, resource and operation context.
- A reason-code reachability test asserts every value in the authorization reason-code enum is produced by at least one test scenario and is documented in the published control evidence pack; an undocumented or unreachable code fails the build.
- A control evidence pack artifact is published on every CI run listing each mutating operation with its required permission, the observed denial reason codes, the guarded-endpoint coverage percentage and the disbursement-integrity reconciliation result, suitable for the quarterly control test and the annual compliance evidence pack.
- Unit tests: reconciliation query logic, reason-code reachability computation and evidence-pack generation each have unit tests passing in CI.
- System integration tests: end-to-end tests on Testcontainers PostgreSQL cover the interactive claim-payment path and the batch claim-payment job path with the same authorization expectations, proving parity between the two entry points.
- Mock data/fixtures: committed fixtures covering approvals in all lifecycle states, adjusters with varied authority limits, reserves at boundary amounts, and JWT tokens for adjuster, supervisor, batch and unassigned principals allow the whole suite to run offline.

**Depends on:** CI pipeline with artifact publication capability, Metrics pipeline for authorization denial counters

---

## Data Classification, PII Masking, Retention and Purge

### [P0] Machine-readable data classification registry for all PCIS entities

PCIS has no per-entity data classification, which blocks the GDPR/ISO 27001 classification policy and makes masking, retention and purge undecidable. Every downstream story in this epic resolves rules by table name plus field name, so the registry must be a machine-readable artifact (YAML plus a versioned data_classification table), not a document. The work must also reconcile the true entity inventory: PCIS_Database_Design.md documents 55 tables, but AUDIT_LOG_ARCHIVE_T (referenced only by AUD002B.cbl and README.md) and COMMISSION_LEDGER_T (written by CMM001B.cbl but absent from the inventory, which lists COMMISSION_PAYMENT_T instead) are missing, plus DDS PF legacy equivalents and SEQUENCE objects — the real count is roughly 57 to 60. It must additionally canonicalize the three conflicting AUDIT_LOG_T column specifications (architecture uses CHG_USER/CHG_TIMESTAMP, CUS design uses USER_ID/EVENT_TIMESTAMP, AUD002B filters and orders on CRT_TIMESTAMP) and the SEQUENCE-versus-IDENTITY conflict for AUDIT_LOG_ID, because retention metadata cannot be attached to an ambiguous schema. Free-text and polymorphic columns need explicit treatment: CLAIM_NOTE_T.NOTE_TEXT, UW_DECISION_T.DECISION_REASON, POLICY_HISTORY_T.EVENT_DESC and CUSTOMER_CONTACT_T.CONTACT_VALUE (email or phone, discriminated by CONTACT_TYPE) cannot be classified by column name alone. The registry also records that PCI-DSS is out of scope because payment capture is delegated to a tokenized third-party gateway storing only tokens and last four digits.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | epic:EPIC-05, domain:compliance, type:foundation, complexity:medium, gdpr, iso27001 |

**Acceptance Criteria**
- A machine-readable registry file (for example src/main/resources/classification/pcis-data-classification.yaml) exists and assigns exactly one tier from Public, Internal, Confidential, Restricted to every entity and to every column of every entity, with no column left unclassified.
- The registry contains an entity reconciliation section that explicitly lists entities present in code but absent from PCIS_Database_Design.md (AUDIT_LOG_ARCHIVE_T, COMMISSION_LEDGER_T) and documents the final authoritative entity count, proving it is not 55.
- AUDIT_LOG_T column names are canonicalized to a single set (audit_log_id, table_name, key_value, action_cd, field_name, old_value, new_value, chg_user, chg_timestamp, program_name) with a documented mapping from each of the three legacy spellings, and the key generation strategy (SEQUENCE versus IDENTITY) is decided and recorded.
- Every column classified Restricted or Confidential carries a mask strategy token (LAST_FOUR, EMAIL_DOMAIN_ONLY, PHONE_LAST_FOUR, DATE_YEAR_ONLY, FULL_REDACT, NONE) and, for polymorphic columns, a discriminator reference (CUSTOMER_CONTACT_T.CONTACT_VALUE resolves by CONTACT_TYPE).
- Free-text columns (CLAIM_NOTE_T.NOTE_TEXT, UW_DECISION_T.DECISION_REASON, POLICY_HISTORY_T.EVENT_DESC, ENDORSEMENT_T.ENDT_DESC, UW_RULE_T.RULE_EXPRESSION) are classified Restricted-by-default with FULL_REDACT for log and audit emission, and the decision is documented in the registry rationale field.
- A Flyway migration creates a data_classification table populated from the registry at startup, with a loader that fails fast on any registry/table drift, and per-tier handling rules (storage encryption, access, log emission, retention tier reference) are recorded.
- Unit tests validate registry schema, tier enum, mask-token enum, uniqueness of entity/column keys, and that the loader rejects a registry containing an unknown mask token or a missing tier.
- System integration test: a Spring Boot test with Testcontainers PostgreSQL boots the classification module, loads the registry into data_classification, and asserts row counts per tier match the registry file exactly.
- Mock data/fixtures: a fixture registry with a deliberately unclassified column and a fixture with an invalid mask token are committed under test resources and used by negative tests.
- The registry records the PCI-DSS out-of-scope decision with its justification (tokenized third-party gateway, tokens and last four only) and is reviewed and signed off by Compliance/Internal Audit; sign-off reference is recorded in the registry metadata header.

### [P0] Shared PII masking library with Jackson and Logback integration

Masking in PCIS today is a render-time convention on 5250 panels only, and it is self-inconsistent: CUSUPDD1 and CUSINQD1 show tax ID with leading digits hidden and a trailing double asterisk, CUSADDD1 shows tax ID fully unmasked, and CUSSRCD1 accepts tax ID, phone and email as exact search criteria. The stored and audited values are always cleartext. This story delivers a shared masking library consumed by all six domain services and both shared-kernel services so that no Restricted-tier value can be serialized into an API response, an audit payload or a log line. Masking must be metadata-driven rather than type-driven, because the legacy AUDLOG01 value slot is polymorphic across callers (CLM006B places a COMP-3 payment amount into the same X(30) slot where CUS001A places a tax ID and PRM005B places a status code), so the only safe key is table name plus field name resolved against the WO-050 registry. The library fixes the canonical mask format to last four characters for tax ID and domain only for email, superseding the panels' leading-mask convention. It ships an annotation for typed domain objects, a Jackson serializer modifier for response and audit payload serialization, and a Logback converter registered in the log pattern so masking is applied at emission time regardless of which code path logs the value.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | epic:EPIC-05, domain:security, type:library, complexity:medium, gdpr, ccpa |

**Acceptance Criteria**
- A pcis-pii-masking module exposes an annotation (for example Classified with tier and mask attributes) and a MaskingService whose primary lookup is by entity name plus column name against the data_classification table or in-memory registry loaded in WO-050.
- Mask strategies produce exactly these canonical outputs: tax ID renders last four characters only, email renders domain only, phone renders last four digits only, date of birth renders year only, free-text renders a fixed redaction token, and no strategy ever emits any part of the original beyond what its rule allows.
- A Jackson BeanSerializerModifier masks annotated fields in every serialized payload, verified for API response DTOs and for the audit event payload object.
- A Logback ClassicConverter registered in the logging pattern masks Restricted values in log messages, MDC values and exception messages, and is enabled by default in every service's logback-spring.xml.
- Polymorphic resolution works: CUSTOMER_CONTACT_T.CONTACT_VALUE masks as email when CONTACT_TYPE is EM and as phone when CONTACT_TYPE is PH or MB, driven by the registry discriminator, with a safe FULL_REDACT fallback for unknown discriminator values.
- Unit tests cover every mask strategy including boundary inputs (null, empty, shorter than four characters, email without an at sign, non-ASCII, value already masked) and assert masking is idempotent.
- System integration test: a Spring Boot test serializes a customer DTO and an audit event containing tax ID, DOB, email, phone and payee name and asserts zero cleartext Restricted values appear in the JSON output or in captured Logback output via ListAppender.
- Mock data/fixtures: a committed fixture set of synthetic customers, contacts, claim payments and audit events covering all mask strategies and all polymorphic discriminator values is used by the test suite with no external dependencies.
- Performance test shows masking adds no more than 1 millisecond p99 per serialized audit event at the target event rate, and the masking path never throws — an internal failure degrades to FULL_REDACT and emits a structured warning.

**Depends on:** WO-050

### [P1] Permission-gated self-audited PII unmask action for investigators

Masking is irreversible by design, but legitimate investigation, fraud review and regulatory response require occasional access to a cleartext value. Today there is no such control at all: masking is a render-time panel convention, CUSADDD1 displays tax ID unmasked, CUSSRCD1 accepts tax ID, phone and email as exact search criteria, and SECCHK01 — the named authorization service — has no source member anywhere, so the role-to-permission matrix cannot even be inspected. This story delivers an explicit, permission-gated, rate-limited and itself-audited unmask action so that revealing a Restricted value is a deliberate, attributable, reviewable event rather than an accident of screen design. The permission must be defined from scratch as deny-by-default, enforced server-side on the endpoint with method-level authorization, and required to carry a mandatory justification. Denials return an RFC 9457 problem detail with HTTP 403 and no internal detail or partial value leak, and emit an authorization_denied audit event. The unmask event itself is written through the audit service, which means the self-audit recursion must be designed deliberately: the unmask audit event records who, what field, which subject, the justification and the timestamp, but must never contain the revealed value. The action is surfaced on the audit inquiry successor and the customer inquiry view, replacing the ad hoc unmasked panel fields.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | epic:EPIC-05, domain:security, type:implementation, complexity:medium, owasp-a01, gdpr |

**Acceptance Criteria**
- A distinct pii:unmask permission exists, is deny-by-default, and is enforced server-side with method-level authorization on the unmask endpoint; no client-side or menu-level gating is relied upon.
- The unmask request requires a non-empty justification of a configured minimum length and a target reference (entity, column, subject id); a request missing any of these returns HTTP 400 with an RFC 9457 problem detail.
- A caller lacking pii:unmask receives HTTP 403 with an RFC 9457 problem detail containing no internal detail, no stack trace and no partial value, and an authorization_denied audit event is emitted with actor, resource and operation.
- Every successful unmask emits an audit event through the audit service recording actor principal, entity, column, subject id, justification, correlation id and timestamp — and explicitly not the revealed value, verified by a test asserting the revealed value is absent from the audit row and from all log output.
- The endpoint is rate-limited per principal with a configurable limit, and exceeding it returns HTTP 429 with an RFC 9457 problem detail and emits a metric plus a security alert on repeated breaches.
- The unmask capability is surfaced on the audit inquiry successor and the customer inquiry view, and no other view exposes a cleartext Restricted value; a test asserts the customer add and search paths never return cleartext tax ID, phone or email.
- Self-audit recursion is handled deliberately: the unmask audit event does not itself trigger an unmask, does not recurse, and its own field values are already masked or non-sensitive by construction, proven by a test.
- Unit tests cover permission enforcement, justification validation, rate limiting, problem-detail shape and the no-value-in-audit invariant; system integration tests with Testcontainers PostgreSQL exercise authorized unmask, unauthorized denial, missing justification and rate-limit breach end to end.
- Mock data/fixtures: committed synthetic customers with Restricted values, plus test principals with and without pii:unmask, drive the suite with no external identity provider dependency (mocked JWT issuer).

**Depends on:** WO-050, WO-051, WO-053

### [P0] Tiered retention with partitioned audit table and restartable job

AUD002B.cbl is the entire retention mechanism in PCIS and it is defective in three specific ways that must be fixed rather than preserved. Its verification paragraph counts all archive rows below the cutoff instead of the chunk just inserted, so after the first run the safety gate is trivially satisfied and vacuous. Its delete subquery uses FETCH FIRST without ORDER BY, so the delete set need not equal the insert set and archived-but-undeleted rows accumulate. Its termination paragraph emits four DISPLAY lines and no non-zero exit, so the job looks successful even after a halt. Retention is a WORKING-STORAGE literal of 365 days and chunk size a literal of 5000, and the declared HV-CURRENT-USER of BATCHAUD is never referenced by any statement, so the job has zero actor attribution. This story replaces it with a monthly range-partitioned audit table, per-tier retention periods read from a versioned change-audited configuration table so regulatory values change without a deploy, and a restartable Spring Batch job whose retention step is a partition-detach metadata operation rather than a row-by-row DELETE. Chunk blast radius drops to at most 1,000 rows, the job exits non-zero when the error threshold is exceeded, and every run writes an evidence row. A coexistence sub-task is mandatory because AUD002B copies with SELECT A.* positionally, so any additive column on the live audit table silently breaks the legacy job during parallel run.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:EPIC-05, domain:batch, type:implementation, complexity:high, gdpr, sox, soc2 |

**Acceptance Criteria**
- AUDIT_LOG_T is monthly range-partitioned in PostgreSQL via Flyway migrations, with an automated partition pre-creation step so a run never fails for a missing future partition.
- Retention periods are read per classification tier from a versioned, change-audited configuration table (successor to RPT_PARM_T) with an effective-from date and change history, and the audit tier retention can never be configured below one year — an attempt is rejected with a validation error.
- The retention step detaches expired partitions as a metadata operation instead of executing DELETE FROM AUDIT_LOG_T, and a test asserts no row-level DELETE statement is issued against the live audit table during a retention run.
- Archive-before-delete and the AUDIT_LOG_ID idempotency guard are preserved: a re-run archives zero duplicates and detaches nothing already detached, proven by running the job twice against the same data.
- The vacuous verification defect is fixed: verification asserts identity of the specific chunk or partition just archived (by id range or checksum), not a count of all archive rows below the cutoff, and a regression test reproduces the legacy false-pass scenario and asserts the new check fails it.
- All archive copy statements use explicit column lists — never SELECT star — and a coexistence sub-task either alters AUDIT_LOG_ARCHIVE_T in lockstep or documents the lockstep procedure, with a test proving an additive column on the live table does not break the archive copy.
- Commit chunk size is externally configured at 1,000 rows or fewer, the job restarts from the last committed chunk under fault injection with zero duplicate and zero orphaned audit rows, and it exits with a non-zero status and raises an alert when the error count exceeds its configured threshold.
- Actor attribution uses a workload principal (for example svc-audit-retention-job) on every audit and evidence record; no compiled-in literal such as BATCHAUD appears anywhere.
- Db2 labelled-duration arithmetic (CURRENT TIMESTAMP minus n DAYS) is rewritten as PostgreSQL INTERVAL arithmetic with a test asserting identical cutoff timestamps for a fixed reference date.
- Unit tests cover cutoff computation, tier-to-retention resolution, the one-year floor validation and chunk sizing; system integration tests with Testcontainers PostgreSQL cover partition detach, restart-from-last-chunk fault injection and non-zero exit on threshold breach; mock data/fixtures seed a multi-month audit population with rows on both sides of every tier cutoff and are committed.
- A golden-output parity test compares archived and detached record counts against the COBOL AUD002B baseline for the same seeded dataset and reference date, with any divergence explained by a documented defect fix.

**Depends on:** WO-050

### [P0] Mask PII at audit event creation before outbox persistence

Today raw before and after values flow through the nine-parameter AUDLOG01 call into AUDIT_LOG_T and then, via AUD002B, into a never-purged AUDIT_LOG_ARCHIVE_T. The worst case is the erasure path: CUS_Module_Design_Document.md specifies that CUS005A physically deletes CUSTOMER_CONTACT_T, CUSTOMER_ADDRESS_T and CUSTOMER_T in one committed unit and calls AUDLOG01 with a full before-image set of entries across all three tables, so exercising a GDPR erasure right currently increases the persistence of that subject's PII. This story moves masking to the point of audit event construction inside the audit shared-kernel service, so no unmasked Restricted value ever reaches the audit store, the transactional outbox row, the archive or any downstream SIEM. It must cover both legacy parameter-block variants (batch callers using X(3) action codes with X(30) values, interactive callers using X(1) action codes with X(100) values and X(40) keys) by resolving mask rules from table name plus field name rather than from the value's type — necessary because CLM006B places a COMP-3 payment amount, PRM005B a status code and CUS001A a tax ID into the same value slot. Masking must occur before the outbox row is written so that the mutation and its already-masked audit event commit atomically.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:EPIC-05, domain:audit, type:implementation, complexity:high, gdpr, sox, soc2 |

**Acceptance Criteria**
- The audit service masks old_value and new_value using the WO-051 MaskingService keyed on canonical table_name plus field_name before constructing the persisted audit event and before writing the transactional outbox row.
- Tax ID renders last four characters only, email renders domain only, phone renders last four digits only, DOB renders year only, and free-text narrative fields render the fixed redaction token in every persisted audit event.
- The CUS005A-equivalent erasure path (cascading physical delete of CUSTOMER_CONTACT_T, CUSTOMER_ADDRESS_T and CUSTOMER_T with a full before-image audit set) produces audit rows containing zero cleartext Restricted values, verified by an explicit test.
- Both legacy parameter-block variants are normalized to the canonical v1 audit contract taking the widest field of each (key up to 40 characters, values up to 100 characters, enumerated action domain including ADD, UPD, DEL, PAY, REN) with a migration mapping asserting no historical value is truncated further than it already is.
- Audit write failure fails the enclosing mutation: a forced audit persistence failure rolls back the business write, in contrast to the legacy DISPLAY-and-continue behavior documented in PRM005B.
- Actor attribution comes from the authenticated principal or workload identity, never from a hard-coded literal such as BATCHAUD, BATCHBIL or BATCHCLM, and is present on every audit event.
- Unit tests cover masking for every Restricted carrier field, both parameter-block variants, the enumerated action domain, and the not-found registry case (fails closed to FULL_REDACT).
- System integration test: with Testcontainers PostgreSQL, a customer update, a claim payment and a customer erasure each produce audit rows asserted to contain zero cleartext Restricted values, and a forced audit failure is asserted to roll back the business mutation.
- Mock data/fixtures: synthetic customers with tax ID, DOB, email, phone and multiple CUSTOMER_CONTACT_T rows of differing CONTACT_TYPE, plus a claim payment with a free-text payee name, are committed and drive the tests.

**Depends on:** WO-050, WO-051

### [P0] CI gates for unclassified entities and unmasked PII leakage

Classification and masking are only durable if the build refuses to ship violations. PCIS today has zero test members, zero CI configuration, no build manifest and no schema migrations, so the safety of DELETE FROM AUDIT_LOG_T and the absence of PII in logs are verified by human inspection only. This story adds three automated gates to the Forge Shipping pipeline. Gate one fails the build when any entity or column present in the schema is absent from the WO-050 classification registry, so new tables cannot silently arrive unclassified. Gate two is a custom Semgrep rule set that fails on source patterns which pass a Restricted-tier value into a logging call, a string concatenation destined for a log, or an audit payload constructor without going through the WO-051 MaskingService. Gate three is a runtime integration test that boots the services against Testcontainers PostgreSQL, exercises the customer, claim payment and audit paths with synthetic Restricted values, and asserts that zero cleartext Restricted values appear in captured Logback output or in serialized audit JSON. Each gate must have a committed negative test proving it actually fails when a violation is introduced, otherwise the gate is decorative.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | epic:EPIC-05, domain:devops, type:ci-gate, complexity:medium, gdpr |

**Acceptance Criteria**
- A build step (Maven test plus Flyway-applied schema check) fails when any table or column in information_schema is missing from the classification registry, and passes when the registry is complete.
- A committed negative fixture adds an intentionally unclassified table via a test-only migration and a test asserts the classification gate fails for it.
- A custom Semgrep rule set (committed under a ci or semgrep directory) flags logger calls, string concatenations feeding loggers, and audit payload construction that pass a Restricted-tier field without the MaskingService, and the scan:semgrep pipeline step fails the build on any finding.
- A committed negative source fixture containing a direct logger call on a tax ID field is detected by the Semgrep rule in a rule unit test, and a positive fixture using MaskingService is not flagged.
- A test:generic integration test using Testcontainers PostgreSQL and a Logback ListAppender exercises customer create/update, claim payment and audit write paths with synthetic Restricted values and asserts zero cleartext Restricted values in captured log output and in serialized audit JSON.
- The Forge Shipping pipeline definition wires all three gates so they run before any registry push, and the pipeline fails closed if any gate step errors rather than being skipped.
- Unit tests exist for the classification gate logic itself (registry-versus-schema diffing) including empty-schema and extra-registry-entry cases.
- Mock data/fixtures: synthetic Restricted values (never real INSPRDDTA values) for tax ID, DOB, email, phone and payee name are committed and shared by all three gates.
- Gate failures produce actionable output naming the offending entity, column, file and line, and a runbook section documents how to remediate each gate failure.

**Depends on:** WO-050, WO-051

### [P0] Automated purge with cryptographic erasure and immutable evidence

PCIS has no purge stage anywhere. AUD002B's own prologue states that the audit table is deliberately not purged by any other program, and AUDIT_LOG_ARCHIVE_T — undocumented, with no DDL in the 55-table inventory — grows without bound carrying before and after values including customer tax ID, phone and email. This story adds the genuine purge stage the data retention policy requires: physical deletion or per-subject cryptographic erasure via key destruction, never soft-delete alone. Cold archive moves detached partitions to object storage under Object Lock in compliance mode with lifecycle expiry, so the tamper-evidence requirement is met while retention still expires automatically. Because audit records must be retained at least one year under SOC 2/SOX while GDPR and CCPA demand erasure, and the legacy retention is exactly 365 days, erasure of a data subject inside the retention floor must be achieved by destroying that subject's encryption key rather than deleting the record — this reconciliation must be documented, not assumed. Evidence goes into a purpose-built immutable purge evidence table rather than RPT_RUN_LOG_T, whose six columns are already semantically overloaded by AUD002B (selected means archived, updated means deleted), which admits an undocumented optional column in PRM005B, and which is committed outside the work transaction. The evidence table must still be consumable by the reporting exception report. The measured target is that purge removes 100 percent of records past retention within 24 hours of expiry.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:EPIC-05, domain:compliance, type:implementation, complexity:high, gdpr, ccpa, sox |

**Acceptance Criteria**
- A purge job runs on a schedule such that 100 percent of records past their tier retention period are purged within 24 hours of expiry, proven by a time-advanced integration test asserting zero remaining expired records after one scheduled cycle.
- Purge uses physical deletion of detached partitions or per-subject cryptographic erasure via KMS key destruction; soft-delete alone is never used, and a test asserts purged data is unreadable after key destruction.
- Detached partitions are written to object storage with Object Lock in compliance mode plus a lifecycle expiry rule matching the tier retention period, provisioned as infrastructure-as-code, and a test or verification script asserts an overwrite or delete attempt before expiry is rejected.
- A purpose-built immutable purge_evidence table records run id, tier, partition or key range, cutoff timestamp, rows archived, rows purged, rows failed, workload principal actor, erasure method, KMS key id, verification hash and completion timestamp, with no update or delete path exposed by the application.
- The purge evidence remains consumable by the reporting exception report: a documented view or projection over purge_evidence is provided so RPT-equivalent reporting does not need to read RPT_RUN_LOG_T for purge outcomes.
- The audit tier retention floor of at least one year is honoured: no purge run may delete an audit record younger than one year, and an attempt is rejected with a structured error; the GDPR-erasure versus SOX-immutability reconciliation is documented in the retention schedule artifact.
- Purge failures never silently skip: every failed record or partition is written to an exceptions list and increments a metric, the job exits non-zero when the error threshold is breached, and an alert fires on purge failure or window overrun.
- Unit tests cover cutoff and tier resolution, the one-year floor rejection, erasure-method selection and evidence record construction; system integration tests with Testcontainers PostgreSQL and a local object-storage emulator cover cold archive write, Object Lock rejection, purge, cryptographic erasure and evidence emission.
- Mock data/fixtures: a committed multi-tier synthetic dataset with detached partitions, per-subject keys and records on both sides of each cutoff drives the tests without external dependencies.
- Terraform (or equivalent IaC) for the object-storage bucket with Object Lock and lifecycle rules and for the KMS keys enabling cryptographic erasure is committed and reviewed.

**Depends on:** WO-050, WO-054

---

## Externalized Configuration, Rules Store and Admin Tunables

### [P0] Create versioned tunables and rules configuration schema

WHAT & WHY: The six regulatory tunables that govern money movement and data retention are compiled into COBOL WORKING-STORAGE literals today (WS-RETENTION-DAYS +365 and WS-CHUNK-SIZE +5000 in AUD002B.cbl, WS-LEAD-DAYS +15 in BIL003B.cbl, WS-GRACE-DAYS +10 in PRM005B.cbl, WS-RENEWAL-WINDOW-DAYS +60 in POL006B.cbl, and the 100000.00 reinsurance cession threshold in CLM006B), so a regulatory change requires a recompile and a manual library promotion through INSDEV to INSTST to INSPRD. This story creates the PostgreSQL persistence foundation for an externalized, effective-dated, version-tracked tunables and rules store so that values can be changed operationally with full who/what/when evidence and zero deployment. IMPACT: New Flyway migration scripts and JPA entities in the shared configuration module of the Java platform; new tables CONFIG_TUNABLE_T, CONFIG_TUNABLE_HISTORY_T and CONFIG_RULE_SET_T following existing PCIS conventions (CRT_USER, CRT_TIMESTAMP, UPD_USER, UPD_TIMESTAMP on every table, DECIMAL(11,2) for monetary values, CHAR fixed-width codes); seed data migration carrying the exact legacy default values so parallel-run comparison is unaffected. WHAT DONE LOOKS LIKE: Running the Flyway migration against a clean PostgreSQL 17 database creates the three tables with constraints, indexes and a seeded row for each of the six tunables holding its legacy value, and every UPDATE against CONFIG_TUNABLE_T produces exactly one append-only history row with the prior and new value. SCOPE BOUNDARIES: No REST API, no resolution/caching service, no admin UI, no PII classification or masking tables, and no changes to any COBOL member — those are separate stories. DEPENDENCIES: Requires the Maven multi-module build scaffold, the PostgreSQL container and the Flyway migration baseline delivered by the platform foundation epic.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | complexity:medium, configuration, database, flyway, compliance |

**Acceptance Criteria**
- Flyway migration creates CONFIG_TUNABLE_T with primary key TUNABLE_KEY VARCHAR(60), plus DOMAIN_CD CHAR(3), VALUE_TYPE CHAR(1), VALUE_TEXT VARCHAR(200), NUMERIC_VALUE DECIMAL(11,2), MIN_VALUE, MAX_VALUE, UNIT_CD, DESCRIPTION, EFFECTIVE_FROM DATE NOT NULL, EFFECTIVE_TO DATE NULL, VERSION_NO INTEGER NOT NULL, and the four standard PCIS audit columns.
- Flyway migration creates append-only CONFIG_TUNABLE_HISTORY_T with BIGINT GENERATED ALWAYS AS IDENTITY surrogate key, TUNABLE_KEY, VERSION_NO, OLD_VALUE, NEW_VALUE, CHANGE_REASON, CHANGED_BY, CHANGED_TIMESTAMP, and a database rule or trigger that rejects UPDATE and DELETE on the table.
- Seed migration inserts exactly six rows with legacy-equivalent values: audit.retention.days=365, audit.archive.chunkSize=1000, billing.leadDays=15, premium.graceDays=10, policy.renewalWindowDays=60, claims.reinsurance.cessionThreshold=100000.00, each with MIN_VALUE and MAX_VALUE bounds and a non-null DESCRIPTION.
- CONFIG_RULE_SET_T exists to hold named rule payloads (rule set key, version, JSONB payload, effective dates, status) with a unique constraint on rule set key plus version.
- A CHECK constraint prevents overlapping effective-date ranges for the same TUNABLE_KEY, verified by an integration test that attempts an overlapping insert and expects rejection.
- Unit tests: JPA entity mapping tests assert column names, precision of DECIMAL(11,2) mapped to BigDecimal, and optimistic-locking behaviour on VERSION_NO; all passing.
- System integration tests: Testcontainers PostgreSQL 17 test applies all migrations from empty, asserts the six seeded rows, asserts history row creation on update, and asserts migration idempotency on a second run.
- Mock data/fixtures: a committed SQL fixture file provides an additional non-production tunable set plus one expired and one future-dated row for effective-dating tests, so the suite runs with no external database.

**Depends on:** Maven multi-module build scaffold and shared BOM, PostgreSQL 17 Testcontainers harness and Flyway baseline

### [P0] Typed tunable resolution service with cache and fail-fast validation

WHAT & WHY: Java services and Spring Batch jobs must consume the externalized tunables through one typed, observable, cache-backed component instead of scattering direct queries or duplicating literals, otherwise the same class of hard-coding that exists in COBOL simply reappears in Java. This story delivers the resolution layer: Spring Boot @ConfigurationProperties defaults, a TunableResolver that reads the effective row from CONFIG_TUNABLE_T, a bounded cache with explicit refresh, startup fail-fast validation that refuses to boot if a required tunable is missing or out of bounds, and Micrometer metrics so operators can see which value a job actually used. It also removes the hard-coded batch actor literals such as BATCHAUD and BATCHBIL in favour of an injected batch principal so audit attribution is real. IMPACT: Shared configuration module gains TunableResolver, TunableKey enum/registry, PcisTunableProperties, a Caffeine-backed cache, a health indicator and Micrometer gauges; batch job configurations and domain services obtain their windows, thresholds and chunk sizes through injection. WHAT DONE LOOKS LIKE: A job or service asks the resolver for billing.leadDays and receives a validated typed value from cache with a metric recording the resolved value and source; changing the row and calling refresh causes the next resolution to return the new value without a restart; booting with a missing or out-of-bounds required tunable fails startup with a structured, actionable error. SCOPE BOUNDARIES: No admin REST API, no admin UI, no code-table/rule-set evaluation, and no changes to COBOL members. DEPENDENCIES: Requires the configuration schema and seeded rows from WO-060 and the structured logging and Micrometer setup from the observability foundation.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | complexity:high, configuration, spring-boot, observability, batch |

**Acceptance Criteria**
- TunableResolver exposes typed accessors returning int, long, BigDecimal, boolean and String, and throws a distinct TunableNotFoundException or TunableOutOfRangeException rather than returning a silent default when a key is absent or violates its bounds.
- PcisTunableProperties binds compile-time fallback defaults via @ConfigurationProperties with jakarta validation annotations, and the resolution order is documented and tested as database effective row, then property override, then fail-fast.
- Application startup fails with a non-zero exit and a structured log line naming the offending key when any tunable declared required in the TunableKey registry is missing, disabled or outside MIN_VALUE/MAX_VALUE.
- Resolved values are cached with a configurable TTL and a maximum size, and an explicit refresh operation invalidates a single key or all keys; a test proves a database change is visible after refresh and not before TTL expiry.
- Every resolution emits a Micrometer gauge tagged by tunable key and a structured log entry containing actor, resource and operation context on refresh, with no secret or PII values in the payload.
- Batch actor identity is supplied by an injected BatchPrincipalProvider rather than a literal string, and a test asserts no hard-coded BATCHAUD, BATCHBIL, BATCHCMM, BATCHPRM, BATCHCLM or BATCHREN literal remains in Java sources.
- Unit tests: resolver precedence, type coercion, bounds validation, cache hit/miss, refresh semantics and failure modes covered with at least 90 percent line coverage on the resolution package; all passing.
- System integration tests: Testcontainers-backed Spring context test proves fail-fast on a corrupted tunable row, and proves a Spring Batch step reads its chunk size and lead-day window from the resolver at job start.
- Mock data/fixtures: an in-memory tunable fixture provider and committed SQL fixtures allow all resolver tests to run with no external service.

**Depends on:** Structured JSON logging and Micrometer/OpenTelemetry setup, Spring Batch job scaffolding

### [P0] Admin tunables REST API with RBAC and change evidence

WHAT & WHY: Compliance needs regulatory tunables changed within days rather than a release cycle, and every change must be attributable, reason-bearing, reviewable and reversible. This story exposes a versioned REST surface for reading and changing tunables and rule sets, protected by deny-by-default method security, validated against the bounds stored with each tunable, wrapped in a transaction that writes the append-only history row and the audit event together, and followed by a cache-invalidation broadcast so running services pick the change up. IMPACT: New admin controller, application service and DTOs in the shared configuration module; Spring Security 6 @PreAuthorize annotations; RFC 9457 problem-detail error mapping; integration with the audit service and the resolver cache invalidation hook from WO-061; OpenAPI 3.1 documentation generated by springdoc. WHAT DONE LOOKS LIKE: An authorised compliance administrator can list tunables with current values and bounds, view the full change history for a key, and submit a change with an effective date and business reason; the response carries the new version number; an unauthorised or unauthenticated caller receives 403 or 401 with a problem detail and no value disclosure; an out-of-bounds change is rejected with 400 and a distinct reason code; every accepted change produces exactly one history row and one audit event in the same transaction. SCOPE BOUNDARIES: No admin web UI (WO-064), no data classification or masking rule endpoints, no retention purge endpoints, and no changes to COBOL members. DEPENDENCIES: Requires WO-060 schema, WO-061 resolver and cache invalidation, plus the shared authorization and audit services from the shared-kernel epic.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | complexity:medium, api, configuration, security, audit |

**Acceptance Criteria**
- GET /v1/admin/tunables returns a paged list of tunables with key, domain, current value, unit, bounds, effective dates, version and description; GET /v1/admin/tunables/{key}/history returns the append-only change history newest first.
- PUT /v1/admin/tunables/{key} accepts new value, effective-from date, expected version and change reason, returns 200 with the new version number, and returns 409 with a problem detail when expected version does not match.
- All admin endpoints are deny-by-default: an unauthenticated request returns 401 and an authenticated principal without the configuration-admin authority returns 403, both as RFC 9457 problem details with no tunable values in the body.
- A value outside the stored MIN_VALUE/MAX_VALUE, of the wrong type, or with an effective-from date overlapping an existing range is rejected with 400 and a distinct machine-readable reason code per failure class.
- An accepted change writes the CONFIG_TUNABLE_T row, one CONFIG_TUNABLE_HISTORY_T row and one audit event inside a single transaction; injecting an audit-write failure rolls back the whole change and the API returns 500 with a problem detail.
- After a successful change the resolver cache invalidation is broadcast and a follow-up read through the resolver returns the new value; a test asserts this across two application contexts.
- OpenAPI 3.1 documentation is generated for all admin endpoints including error schemas, and a committed snapshot of the specification is diffed in the build so contract drift fails the pipeline.
- Unit tests: application service validation, version-conflict handling, authorization annotations and problem-detail mapping covered; all passing.
- System integration tests: MockMvc or WebTestClient tests with Testcontainers PostgreSQL cover the happy path, all rejection paths, the audit-failure rollback and the cache-refresh visibility flow.
- Mock data/fixtures: committed JSON request/response fixtures and SQL seed fixtures for tunables and history rows allow the API test suite to run with no external dependency.

**Depends on:** Shared authorization service and Spring Security 6 method security setup, Shared audit service with transactional outbox

### [P1] Admin tunables web panel with versioned change history

WHAT & WHY: Compliance and operations staff must be able to see and change the six regulatory tunables and the reference-data domains without a database client or a developer, and every change must be reviewable in place with who, what and when. Today the only interface to any PCIS configuration is a 5250 green-screen path that cannot meet WCAG 2.1 AA and does not exist for tunables at all, since they are compiled literals. This story delivers the Admin Tunables surface of the web application: a tunables panel listing each key with its current value, unit, bounds and effective dates, an edit drawer that validates client-side against the stored bounds and requires a business reason, and a change-history table showing version, actor, old value, new value, reason and timestamp. IMPACT: New React 19 plus TypeScript route and components (TunablesPanel, TunableRow with value field, effective-from and version badge, EditDrawer, ChangeHistoryTable, SaveBar) plus a typed API client generated from the OpenAPI specification produced in WO-062; role-gated navigation entry. WHAT DONE LOOKS LIKE: An authorised administrator loads the admin tunables route, sees all tunables with current values, opens the edit drawer for billing lead days, is prevented from submitting an out-of-bounds value or a blank reason, submits successfully, sees the new version badge and a new history row; an unauthorised user never sees the navigation entry and receives an access-denied state if they deep-link. SCOPE BOUNDARIES: Classification, masking-rule and retention/purge panels are out of scope and belong to the PII and retention epic; no server-side authorization logic is implemented here since the API enforces it; no code-table editing UI. DEPENDENCIES: Requires the admin REST API and OpenAPI contract from WO-062 and the web application shell, design system and authentication flow from the UI epic.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | complexity:medium, frontend, accessibility, configuration, admin |

**Acceptance Criteria**
- The admin tunables route renders a table of all tunables returned by GET /v1/admin/tunables showing key, domain, current value, unit, bounds, effective-from and a version badge, with loading, empty and error states implemented.
- The edit drawer performs client-side validation against the bounds and value type returned by the API, requires a non-empty change reason of at least the documented minimum length, and disables submit until the form is valid; server-side rejection reason codes from the API are surfaced as field-anchored plain-language messages.
- Successful submission calls PUT /v1/admin/tunables/{key} with the expected version number, optimistically shows the new version badge, refetches the list and history, and surfaces a 409 conflict as a clear reload-and-retry message rather than a silent failure.
- The change-history table shows version, changed-by, old value, new value, change reason and timestamp newest first for the selected tunable, with pagination for long histories.
- The route and all interactive components meet WCAG 2.1 AA: full keyboard operation of the table, drawer and save bar, visible focus, correct labels and aria attributes, screen-reader announced validation errors and contrast conformance, verified by an automated accessibility test with zero critical violations.
- Navigation entry and route are role-gated so a principal without configuration-admin authority does not see the entry and receives an access-denied view on deep link; client-side gating is presentation only and never the sole control.
- Unit tests: component tests for rendering, validation, conflict handling, error states and the API client mapping, all passing with committed test coverage output.
- System integration tests: an end-to-end test against a mocked or containerised API exercises list, edit, submit, history refresh and conflict flows; N/A for cross-service transactional assertions, which are covered by WO-062 API tests.
- Mock data/fixtures: committed MSW handlers and JSON fixtures derived from the WO-062 OpenAPI snapshot allow the UI test suite and local development to run with no backend running.

**Depends on:** Web application shell, design system tokens and component library, OIDC authentication flow and role-based navigation gating

### [P1] Externalized code-table and business rules store service

WHAT & WHY: Beyond simple numeric tunables, PCIS carries whole domains of behaviour as compiled-in conventions: cancellation reason codes, claim type codes, the BILLING_SCHEDULE_T status domain including the proposed V for void, the claim late-reporting threshold, the billing frequency to interval mapping in BIL003B (M adds one month, Q three months, S six months, anything else one year) and the reserve status domain. The enterprise architecture lists twelve open design items and explicitly directs that where a business decision cannot yet be made the behaviour must be configuration-driven rather than hard-coded. This story delivers a reference-data and rules store service so these domains live in CODE_TABLE_T-style rows and versioned rule sets, are cached, versioned, validated at build time and served to domain services through a typed port. IMPACT: New code-table schema and rule-set evaluation component in the shared configuration module; billing frequency interval mapping, delinquency status transitions and claim classification domains sourced from configuration; a build-time completeness check that fails when a referenced domain has no rows. WHAT DONE LOOKS LIKE: A domain service resolves the interval for billing frequency Q from the rules store and receives three months; an unknown frequency resolves to the documented default of one year and raises a visible exception record rather than a silent skip; adding a new cancellation reason requires only a data change; the build fails if a code domain referenced in code has no seeded rows. SCOPE BOUNDARIES: No admin UI for code tables, no data classification or masking rules, no rewriting of the batch jobs themselves beyond wiring the lookup port, and no changes to COBOL members. DEPENDENCIES: Requires WO-060 schema foundation and WO-061 caching/resolution patterns.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | complexity:medium, configuration, reference-data, business-rules |

**Acceptance Criteria**
- Flyway migrations create CODE_TABLE_T (domain code, code value, description, sort order, active flag, effective dates, standard audit columns) with a unique constraint on domain plus code value plus effective-from.
- Seed migrations populate at minimum the billing frequency to interval mapping (M, Q, S and the default fallback), the billing schedule status domain including V for void, the reserve status domain including AP and PD, claim type codes and cancellation reason codes, each with a description.
- A CodeTableService exposes typed lookup, list-by-domain and validate-membership operations backed by the same Caffeine cache and refresh pattern established in WO-061, with no direct repository access from domain code.
- A RuleSetEvaluator resolves a named, versioned rule set from CONFIG_RULE_SET_T and returns an immutable typed view; the billing frequency interval mapping and the delinquency status transition table are expressed as rule sets and consumed by the corresponding domain services.
- An unknown or inactive code value produces a distinct UnknownCodeValueException and a structured exception record, and for billing frequency preserves the legacy one-year default while making the fallback observable rather than silent.
- A build-time check enumerates every code domain referenced in Java sources and fails the build when a referenced domain has no seeded active rows, mirroring the classification completeness gate pattern.
- Unit tests: lookup, membership validation, effective dating, fallback behaviour, cache refresh and rule-set immutability covered with at least 90 percent line coverage on the rules package; all passing.
- System integration tests: Testcontainers PostgreSQL test proves a billing-generation style step derives the next due date interval from the rule set for M, Q, S and an out-of-domain value, matching the COBOL semantics exactly.
- Mock data/fixtures: committed SQL fixtures for every seeded domain plus an in-memory CodeTableService stub allow domain unit tests to run without a database.

**Depends on:** Set-based batch reader refactor for billing and delinquency jobs, Structured exception recording and observability foundation

---

## PostgreSQL Schema Conversion, Data Migration and Reconciliation

### [P0] Author Flyway Baseline PostgreSQL Schema Migrations

WHAT & WHY: PCIS has no schema-as-code. Object creation today is a hand-maintained IBM i Control Language member (PCIS_CRTOBJ.clle) with undocumented compile order, and promotion between INSDEV, INSTST and INSPRD is a manual library copy. The PRD requires 100 percent of schema changes to arrive through versioned migrations with zero manual library-copy steps, and the deployment pipeline gate for the dev environment is literally that Flyway migrations apply cleanly to a fresh database. This story converts the corrected data dictionary from WO-072 into versioned, additive-only Flyway migrations for the full PostgreSQL 17 target schema, mirroring the existing QSQLSRC one-member-per-object convention so each table, index and sequence remains independently changeable. IMPACT: Adds a db/migration tree with per-object SQL files grouped by module (CUS/AGT/SEC/Shared, QTE/UND, POL/PRM, BIL/PAY, CLM/REI, DOC/RPT/AUD), a Flyway configuration, a Testcontainers-backed migration test, and a CI stage that provisions an empty PostgreSQL container and applies every migration in order. Supersedes PCIS_CRTOBJ.clle for the migrated path while leaving the legacy CL member in place for coexistence. WHAT DONE LOOKS LIKE: A single command applies the entire schema to an empty PostgreSQL 17 database with zero errors; every monetary column lands at the exact NUMERIC precision and scale mandated by the dictionary; every index follows the legacy naming convention translated to PostgreSQL; AUDIT_LOG_T is created as a monthly range-partitioned table; and the CI pipeline blocks any commit whose migrations do not apply cleanly to a fresh database. SCOPE BOUNDARIES: Does not create SEQUENCE objects, block allocation or key formatting logic (WO-071); does not migrate any data (WO-073); does not implement retention purge jobs, masking serializers or reconciliation; does not port DDS physical-file equivalents, which the PRD explicitly excludes from the target. DEPENDENCIES: Blocked by WO-072 for the canonical dictionary, especially the resolved BILLING_SCHEDULE_T, INVOICE_T, RPT_RUN_LOG_T, BILLING_PLAN_T and AUDIT_LOG_T definitions and the NOT NULL constraint decisions.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:EPIC-07, domain:data-migration, type:schema, complexity:high, phase:0, tooling:flyway |

**Acceptance Criteria**
- Flyway migrations under db/migration create every table in the canonical dictionary, split into six module-group batches, with one SQL file per table plus separate files for indexes, so each object retains independent change control equivalent to the QSQLSRC one-member-per-object convention.
- A Testcontainers PostgreSQL 17 integration test starts an empty database, runs Flyway migrate, and asserts a zero-error result plus an exact table count matching the dictionary; the test is wired into CI as the dev deployment gate.
- An automated assertion compares every column created in the container against the canonical dictionary for name, PostgreSQL type, numeric precision and scale, nullability and default, and fails with the specific column name on any mismatch.
- Monetary precision is preserved exactly: NUMERIC(9,2) and NUMERIC(11,2) as the defaults plus the approved outliers NUMERIC(13,2) for property replacement value, NUMERIC(9,4) for base rate, NUMERIC(7,4) for factor multipliers and commission rate, and NUMERIC(7,2) for installment fee — each verified by test.
- Business document keys are created as fixed-length CHAR or VARCHAR of the mandated widths (CUST_ID 10, AGT_ID 8, QUOTE_ID 12, POL_NBR 12, COVERAGE_ID 14, PAYMENT_ID 14, DOCUMENT_ID 14, RISK_ID 12, VEHICLE_ID 12, PROP_ID 12, COV_TYPE_CD 5) and no business document key is created as an IDENTITY column — asserted by a test that scans column defaults.
- Detail surrogate keys declared in the dictionary as identity are created as BIGINT GENERATED ALWAYS AS IDENTITY, and a test asserts the two key strategies are mutually exclusive and match the dictionary classification.
- AUDIT_LOG_T is created as a monthly range-partitioned table with at least twelve initial partitions and a documented partition-creation procedure, so retention can later be implemented as partition detach rather than mass DELETE.
- Indexes follow the translated legacy naming convention (for example customer, policy, billing-schedule and invoice navigation indexes), cover every foreign key navigation path, include the composite status-plus-date indexes needed by the batch candidate queries, and include descending history indexes; an integration test asserts each expected index exists.
- All migrations are additive-only with no column drops or type narrowing, and a CI check rejects any migration file containing DROP COLUMN or a narrowing ALTER TYPE while coexistence is active.
- Unit tests cover the migration-naming and additive-only lint rules; system integration tests are the Testcontainers fresh-database apply plus schema-versus-dictionary comparison; mock data and fixtures are limited to reference-data seed migrations (code table domains) committed alongside the DDL so integration tests run with no external dependencies.

**Depends on:** WO-072

### [P0] Implement Sequence Objects, Block Allocator and Key Formatter

WHAT & WHY: PCIS business document keys are not raw integers. CUS001A holds both a numeric sequence host variable and a separate fixed-length X(10) customer identifier, and the claims design specifies CLM_NBR as a policy-type-derived prefix plus a zero-padded sequence, proving the legacy pattern is sequence value formatted into a fixed-width string. The architecture mandates that these keys remain SEQUENCE-generated fixed-length values and explicitly forbids IDENTITY columns for them, while also requiring sequence allocation in blocks of 100 to cut sequence round trips by at least 90 percent relative to the current per-item VALUES NEXT VALUE FOR pattern seen in BIL003B, CMM001B, CLM006B and POL006B. This story delivers the sequence DDL, a concurrency-safe block allocator and a per-key-type formatter, plus a high-water-mark seeding procedure so cutover cannot produce a duplicate key. IMPACT: Adds sequence migration files to db/migration, a new key-generation module in the shared Java library (sequence DDL, block allocator, formatter, configuration properties) used by every domain service and Spring Batch writer, and a cutover seeding tool that reads source-side high-water marks. WHAT DONE LOOKS LIKE: Requesting a new customer identifier returns a ten-character value with the correct prefix and zero padding; requesting ten thousand identifiers concurrently across multiple threads produces zero collisions and issues at most one hundred database round trips; every sequence honours a maximum value compatible with the legacy S9(9) host-variable ceiling and never cycles; and after seeding from source high-water marks, no newly generated key can collide with an existing legacy key. SCOPE BOUNDARIES: Does not create tables or indexes (WO-070); does not migrate or reconcile data (WO-073, WO-074); does not change any COBOL program; does not implement the audit or authorization services. DEPENDENCIES: Blocked by WO-072 for the per-key IDENTITY-versus-SEQUENCE classification and by WO-070 for the tables the keys populate.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | epic:EPIC-07, domain:data-migration, type:implementation, complexity:medium, phase:0, concern:key-generation |

**Acceptance Criteria**
- Flyway migrations create every required sequence: the fifteen declared in the architecture (customer, agent, quote, policy number, coverage, deductible, policy property, policy vehicle, billing schedule, claim number, claim payment, payment, refund, document, audit log) plus the three code-witnessed ones (invoice, commission ledger, and the claim payment sequence name variant), each with explicit INCREMENT, MINVALUE, MAXVALUE, CACHE and NO CYCLE.
- Sequence MAXVALUE and CACHE are set so that generated numeric values remain within the legacy S9(9) host-variable ceiling of 999999999 where a legacy consumer still reads the value, and a unit test asserts the configured maximum for each such sequence.
- A block allocator component fetches sequence values in externally configurable blocks (default 100) and hands them out in memory; an integration test issuing 10000 allocations asserts at most 100 database round trips, proving the at-least-90-percent reduction target.
- A key formatter produces fixed-width values per key type with the mandated widths and prefixes: customer 10, agent 8, quote 12, policy number 12, coverage 14, payment 14, document 14, risk 12, vehicle 12, property 12, coverage type code 5, and claim number as the two-character CL prefix plus zero-padded sequence; unit tests assert exact length and padding for the minimum, a mid-range and the maximum value.
- A concurrency test running the allocator from at least sixteen threads against a Testcontainers PostgreSQL 17 database produces zero duplicate keys across at least 50000 allocations and no gaps that violate monotonic ordering guarantees documented for each key type.
- A round-trip proof test on at least one representative table (customer) inserts a formatted key generated by the allocator, reads it back, and asserts byte-for-byte equality including padding, satisfying the PRD assumption that sequence and format behaviour is validated on the target database.
- A high-water-mark seeding tool accepts per-sequence starting values captured from the source platform and issues ALTER SEQUENCE RESTART so that the first generated value is strictly greater than the highest existing legacy key; an integration test seeds a table with legacy-shaped keys and asserts the next generated key does not collide.
- Block allocation is crash-safe by design: allocated-but-unused values are permitted to be lost (documented as acceptable gap behaviour) but a value must never be issued twice, asserted by a test that kills and restarts the allocator mid-block.
- Unit tests cover formatter width, padding, prefix, overflow rejection and allocator block arithmetic; system integration tests cover concurrent allocation, round-trip insert and high-water-mark seeding against Testcontainers; mock data and fixtures include committed legacy-shaped key fixtures per key type used by the collision tests.

**Depends on:** WO-072, WO-070

### [P0] Resolve Schema Discrepancies and Publish Corrected Data Dictionary

WHAT & WHY: The published design document PCIS_Database_Design.md and the shipped COBOL programs disagree about the shape of several core tables, and no authoritative schema exists that both sides agree on. Exploration identified thirteen classes of contradiction, including BILLING_SCHEDULE_T column names (AMT_DUE/AMT_PAID in the design versus DUE_AMT/PAID_AMT in BIL003B), its status column (SCHED_STATUS versus BILL_STATUS) and status domain (O/P/V versus D/P/L), an undocumented COMM_CALC_FLAG that drives commission idempotency in CMM001B, a NOT NULL BILL_PLAN_ID that BIL003B never populates, INVOICE_T.INVOICE_DUE_DATE NOT NULL that is likewise never populated, RPT_RUN_LOG_T having no column definition anywhere while six programs insert into it with two different column lists, an AGENT_ID numeric versus AGT_ID VARCHAR(8) key conflict, commission rate precision DECIMAL(5,2) versus S9(3)V9999, a BILLING_PLAN_T that is a global reference table in the design but per-policy in code, three-way AUDIT_LOG_T column drift, and two code-witnessed tables (COMMISSION_LEDGER_T, AUDIT_LOG_ARCHIVE_T) absent from the 55-table inventory. Every downstream migration artifact depends on these being decided, so this is the gating Phase 0 story. IMPACT: Produces new repository artifacts under a docs/migration and schema/ directory tree: a machine-readable canonical data dictionary, a discrepancy and decision register, and a validation script; updates PCIS_Database_Design.md with corrections and cross-references to code evidence in BIL003B.cbl, CMM001B.cbl, PRM005B.cbl, CLM006B.cbl, POL006B.cbl, CUS001A.cbl, POL001A.cbl and AUD002B.cbl. WHAT DONE LOOKS LIKE: A single canonical machine-readable dictionary enumerates every target table, column, type, precision, nullability, key strategy classification (business document key from sequence versus detail surrogate identity), and data classification tier; each of the thirteen-plus discrepancies has a recorded decision, rationale, code citation and owner; a validation script fails the build when the dictionary and the design document diverge or when any entity is unclassified. SCOPE BOUNDARIES: Does not author the Flyway DDL (WO-070), does not create sequences or key formatters (WO-071), does not extract or load data, and does not implement masking rules beyond assigning tiers. Business and Finance sign-off of the pro-rata and precision decisions is tracked as an external approval item, not delivered here. DEPENDENCIES: None inside this epic; it is the blocker for WO-070, WO-071, WO-073, WO-074 and WO-075.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:EPIC-07, domain:data-migration, type:specification, complexity:high, phase:0, compliance:data-classification |

**Acceptance Criteria**
- A canonical machine-readable data dictionary file exists in the repository (YAML or JSON) covering all 55 designed tables plus the code-witnessed extras COMMISSION_LEDGER_T and AUDIT_LOG_ARCHIVE_T and the premium-engine tables DISCOUNT_RULE_T, SURCHARGE_RULE_T, TAX_TABLE_T and RISK_SCORE_FACTOR_T, with table name, column name, PostgreSQL type, precision and scale, nullability, default, key role and data classification tier for every column.
- A discrepancy register documents at minimum the thirteen identified conflict classes, each with: source-of-truth citation (design document section and COBOL file plus paragraph), the two conflicting definitions, the chosen resolution, the rationale, the affected downstream programs, and a named decision owner.
- Every surrogate key in the dictionary is explicitly classified as either business-document-key-from-sequence (fixed-length VARCHAR or CHAR, prefix plus zero-pad) or detail-surrogate-identity (BIGINT GENERATED ALWAYS AS IDENTITY); BILL_SCHED_ID, INVOICE_ID, LEDGER_ID, RESERVE_ID, CLAIM_PAYMENT_T payment identifier, DEDUCT_ID, POL_PROP_ID and POL_VEH_ID each carry a recorded decision.
- The precision outlier list is captured verbatim in the dictionary: PROPERTY_T.REPLACEMENT_VALUE NUMERIC(13,2), commission rate NUMERIC(7,4) reconciled against the design NUMERIC(5,2), RATE_TABLE_T.BASE_RATE NUMERIC(9,4), RATE_FACTOR_T.FACTOR_MULT and PREMIUM_CALC_T.TOTAL_FACTOR NUMERIC(7,4), BILLING_PLAN_T.INSTALLMENT_FEE NUMERIC(7,2).
- RPT_RUN_LOG_T has a complete target definition that accommodates both the six-column insert used by five programs and the seventh column REC_DELINQUENT used by PRM005B, plus a run identifier, CRT_USER, start and end timestamps and an exit status.
- AUDIT_LOG_T column widths are set to the maximum of the batch and interactive AUDLOG01 parameter conventions (key value at least 40 characters, action code enumerated, old and new value at least 100 characters) with a recorded note that narrower widths would silently truncate evidence.
- A validation script runs in CI and fails the build if any table or column in the dictionary lacks a classification tier, if a table referenced by any COBOL EXEC SQL statement is absent from the dictionary, or if a dictionary entry has no resolution recorded for a listed discrepancy.
- Unit tests cover the validation script: positive case on the committed dictionary, and negative cases for unclassified entity, missing table, and unresolved discrepancy — all passing.
- System integration tests: N/A — this story produces specification and validation artifacts only, with no service or API boundary to exercise.
- Mock data and fixtures: deliberately malformed dictionary fixtures are committed under a test resources directory so the validation script negative cases run with no external dependencies.

### [P1] Automate Masked Anonymized Non-Production Data Refresh

WHAT & WHY: Today INSPRDDTA is the only library holding real customer data, INSDEVDTA is described as masked or synthetic and INSTSTDTA is refreshed periodically from a manual masked production extract — a hand-run process that gates QA data quality and, once data leaves the closed IBM i partition for cloud storage, log aggregators and lower environments, materially expands the personal-data footprint. Organization policy requires PII masked in logs, anonymized in non-production and encrypted in storage, plus a classification tier per entity. This story delivers an automated, repeatable pipeline that produces anonymized development and test datasets from the canonical classification manifest, preserving field validity and referential integrity so parallel-run tests, key-format assertions and the customer search behaviours continue to function. IMPACT: Adds an anonymization module with per-field masking strategies driven by the data classification tiers in the canonical dictionary, a refresh pipeline job that populates the development and test PostgreSQL databases, a verification scanner asserting zero real personal data and intact referential integrity, and a scheduled CI or operator-triggered pipeline replacing the manual masked extract. WHAT DONE LOOKS LIKE: Triggering a refresh repopulates the target lower-environment database with anonymized data in which every restricted-tier field is masked or synthesised, every masked value still satisfies its validation rules, every foreign key still resolves, every business key still matches its prefix and zero-pad format, and an automated scanner reports zero real personal-data values. SCOPE BOUNDARIES: Does not implement the production audit masking serializer or the log masking converter, which belong to the PII and audit epic; does not implement retention purge; does not perform production data migration or reconciliation; does not decide the retention period per tier or the retrospective treatment of the existing audit archive, which are recorded open items with named owners. DEPENDENCIES: Blocked by WO-072 for the classification manifest that names every restricted field and by WO-070 for the target schema; benefits from WO-073 for the extraction plumbing it reuses.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | epic:EPIC-07, domain:data-migration, type:implementation, complexity:medium, phase:0, compliance:pii |

**Acceptance Criteria**
- An anonymization pipeline populates a development-equivalent and a test-equivalent PostgreSQL database from the canonical dictionary and its classification tiers, driven by configuration with no hand-editing of SQL per run, replacing the manual masked-extract step described in the architecture library topology.
- Every restricted-tier field in the recorded inventory is masked or synthesised, including customer tax identifier, date of birth, email, phone and name fields, customer contact value, all customer address fields, agent name, email and phone, agent license number, vehicle identification number, property address fields, claim payment payee, claim note free text, underwriting decision reason, endorsement description, policy history event description, user credentials, and both audit log and audit log archive old and new value columns.
- Masked values remain valid against the documented validation rules: tax identifier retains nine numeric digits, email retains an at sign plus a domain-shaped suffix, phone retains ten digits, postal code retains five or nine digits, state code resolves in the code table state domain, and date of birth yields an age of at least sixteen — each asserted by test.
- Fixed masking rules are applied where specified: tax identifier renders as last four characters only and email renders as domain only, and a test asserts no full tax identifier or full local-part email appears anywhere in the output.
- Business key formats survive anonymization unchanged: every prefix plus zero-padded business document key retains its exact width and format so parity tests, key-format assertions and the customer search behaviours (name partial, tax identifier exact, phone, email, city and state, status) continue to work.
- Referential integrity is intact after anonymization: an automated check asserts every foreign key in the anonymized database resolves and that consistent masking is applied to the same logical value across every table in which it appears, so joins produce the same cardinality as the source.
- A verification scanner runs as the final pipeline stage and fails the refresh if any value matching a real personal-data pattern from the source is present in the anonymized output, if any restricted field is unmasked, or if any entity lacks a classification tier.
- No credential literal appears in any pipeline configuration; source and target connection values are supplied as placeholders and the secret scan reports zero findings.
- Unit tests cover each masking strategy for format preservation, determinism of consistent masking for the same input, and rejection of an unclassified field; system integration tests run the full pipeline against a Testcontainers PostgreSQL 17 target and assert masking coverage, validation-rule survival, referential integrity and scanner pass or fail behaviour; mock data and fixtures include a committed synthetic source dataset containing deliberately realistic-looking personal data so the suite runs with no access to production data.

**Depends on:** WO-072, WO-070

### [P0] Build Polling Extraction and Idempotent PostgreSQL Loader

WHAT & WHY: Db2 for i remains the system of record until each domain passes its parallel-run gate, so the target PostgreSQL database must be continuously populated from the source without disturbing any financial mutation. Change data capture is impractical on IBM i per the intent constraints, so this story builds a watermark-driven polling extractor and an idempotent upsert loader, generalising the two idempotency patterns that already exist in the legacy code: the NOT EXISTS guard in AUD002B paragraph 2100 and the COMM_CALC_FLAG once-only guard in CMM001B. It must also translate a specific catalogue of Db2 for i constructs into PostgreSQL equivalents and normalise timestamp, NULL-versus-SPACES and CHAR trailing-space semantics, because these are the exact differences that will otherwise show up as false reconciliation breaks. IMPACT: Adds a migration tooling module containing per-domain extract definitions, a staging schema, an idempotent loader with configurable chunk size, durable restart points, a watermark store, and structured error handling; adds Flyway migrations for the staging and watermark tables; adds a polling schedule specification with frequency and latency SLA. WHAT DONE LOOKS LIKE: Running the extractor for a domain pulls only rows changed since its watermark, loads them into PostgreSQL through explicit column lists with zero duplicates on re-run, commits in chunks of at most 1000 rows, restarts from the last committed chunk after an injected failure without duplicating or orphaning any row, and records a run log entry with counts and exit status. SCOPE BOUNDARIES: Does not perform reconciliation or produce the parity gate report (WO-074); does not anonymise data for lower environments (WO-075); does not create the target tables (WO-070) or sequences (WO-071); does not rewrite any COBOL program or convert batch business logic to Spring Batch, which belongs to the batch conversion epic. DEPENDENCIES: Blocked by WO-070 for the target schema, WO-071 for key generation and seeding, and WO-072 for the resolved column definitions and NOT NULL decisions that would otherwise fail the first load.

| Field | Value |
|---|---|
| Story Points | 13 |
| Hours | 130h |
| Priority | P0 |
| Labels | epic:EPIC-07, domain:data-migration, type:implementation, complexity:high, phase:0, concern:coexistence |

**Acceptance Criteria**
- A per-domain extract definition set exists for Customer, Claims, Billing, Premium, Commission, Policy and Audit, each naming its source tables, its watermark column, its explicit projected column list (never SELECT star) and its target table mapping, and each is validated at startup against the canonical data dictionary.
- The loader is idempotent: running the same extract batch twice produces identical target row counts and identical column values, verified by an integration test that loads a fixture batch, re-loads it, and asserts zero duplicate keys and zero changed values.
- Commit chunk size is externally configurable and defaults to at most 1000 rows, satisfying the requirement to reduce commit blast radius from the legacy 5000; the configured value is asserted by test and logged at run start.
- Restartability is proven by fault injection: a simulated database failure mid-run leaves the durable restart point at the last committed chunk, and a restart resumes from that point with zero duplicated rows and zero orphaned rows, replacing the legacy AUD002B behaviour where any error halts the entire run with no restart point.
- A documented and implemented conversion layer handles every catalogued Db2 for i construct: FETCH FIRST n ROWS ONLY to LIMIT, VALUES expression INTO host variable to a plain SELECT, labelled durations of 1 MONTH, 3 MONTHS, 6 MONTHS and 1 YEAR to INTERVAL, CURRENT TIMESTAMP minus n DAYS to interval arithmetic, DAYS(a) minus DAYS(b) to date subtraction, NEXT VALUE FOR to nextval, SQLERRD(3) to update counts, SQLCODE 100 and negative values to SQLSTATE and exception mapping, comma joins to explicit joins, SYSIBM.SYSDUMMY1 to a FROM-less select — each with a unit test.
- Timestamp and date fidelity rules are pinned and tested: source X(26) timestamps map to PostgreSQL TIMESTAMP with a documented UTC convention preserving microsecond precision so optimistic-lock comparisons on UPD_TIMESTAMP still work, and X(10) dates map to DATE.
- NULL-versus-SPACES and CHAR trailing-space normalisation rules are implemented and tested so that a COBOL SPACES value and a SQL NULL are distinguished deterministically and identically on every run, preventing false reconciliation breaks.
- The transaction-timing semantic difference is handled explicitly: PostgreSQL CURRENT_TIMESTAMP is transaction-start while Db2 is statement-level, so every chunked loop computes its cutoff once from an explicit run parameter rather than from the database clock, asserted by a test that runs a multi-chunk load across a clock boundary.
- All database access uses parameterized statements with no string interpolation of any input, verified by a static analysis rule in CI, and no credential literal appears in any configuration file.
- Unit tests cover watermark arithmetic, construct conversion, normalisation rules and chunk boundary logic; system integration tests cover full extract-and-load against a Testcontainers PostgreSQL 17 target using a simulated source, idempotent re-run, and fault-injection restart; mock data and fixtures include committed per-domain source fixture datasets exercising nulls, blanks, boundary precision and maximum-width keys so the suite runs with no live Db2 connection.

**Depends on:** WO-070, WO-071, WO-072

### [P0] Build Nightly Cent-Level Parallel-Run Reconciliation Harness

WHAT & WHY: The programme's primary success metric is that 100 percent of reconciled records match the COBOL baseline to the cent with zero unexplained breaks across a minimum thirty-day parallel run per domain, measured by automated nightly reconciliation of amounts, counts and checksums between Db2 for i and PostgreSQL. No such tooling exists; the only comparison primitive in the repository is the single COUNT check in AUD002B paragraph 2200 that skips a delete on mismatch. This story builds the reconciliation harness that is the gate for every domain cutover, including per-domain amount and invariant assertions, an exception store, alerting, defined rollback triggers and a published gate report. It also closes the recorded specification gap that reconciliation tooling, gate criteria and rollback procedure were undefined. IMPACT: Adds a reconciliation module with per-domain comparators, Flyway migrations for a reconciliation run and break-exception schema, a nightly scheduled job reading the PostgreSQL streaming read replica rather than the OLTP primary, a gate report generator, and a documented per-domain gate and rollback specification. WHAT DONE LOOKS LIKE: A nightly run compares source and target for a domain, asserts row counts, cent-level amount equality, checksums and domain invariants, records every break with enough context to triage, alerts on the first unexplained break, and emits a gate report stating whether the domain has accumulated the required consecutive clean days. SCOPE BOUNDARIES: Does not extract or load data (WO-073); does not fix defects it discovers; does not implement the golden-output batch regression suite, which belongs to the batch conversion epic; does not perform the cutover itself. DEPENDENCIES: Blocked by WO-073 for a populated target and by WO-072 for the canonical column definitions that determine what is compared.

| Field | Value |
|---|---|
| Story Points | 13 |
| Hours | 130h |
| Priority | P0 |
| Labels | epic:EPIC-07, domain:data-migration, type:validation, complexity:high, phase:1, gate:cutover |

**Acceptance Criteria**
- A nightly reconciliation job compares source and target per domain for Billing, Premium and delinquency, Commission, Claims, Policy and renewal, and Audit, asserting row counts, cent-level amount equality and column checksums, and it reads the PostgreSQL streaming read replica rather than the OLTP primary.
- Per-domain amount and invariant assertions are implemented exactly as specified: billing compares due amount, paid amount, invoice amount and annual premium plus installment number, due date and status with the installment equal to annual premium divided by installment count at NUMERIC(9,2) using HALF_UP; premium compares base premium, final premium and total factor at NUMERIC(7,4) plus status transitions under a ten-day grace; commission compares commission amount, rate and paid amount plus the count of rows flagged as commissioned; claims compares approved amount, paid to date, payment amount and recovery amount plus the reserve status transition and the 100000.00 cession threshold; policy compares annual premium, limit amount and premium amount plus expiry status, exactly two history events and unchanged policy number format; audit compares row counts and the archive-then-delete invariant.
- Cross-cutting assertions are implemented: sequence high-water marks with zero key collisions, NUMERIC scale preservation per column, NULL-versus-blank fidelity, CHAR trailing-space semantics and UPD_TIMESTAMP microsecond fidelity so optimistic locking is not silently broken.
- RPT_RUN_LOG_T counters are reconciled per program per run date, accommodating the seven-column variant used by the delinquency program alongside the six-column variant used by the other five.
- Every mismatch is persisted to a break exception store with domain, table, business key, column, source value, target value, break class and triage status, and a break is never merely logged and discarded.
- The first unexplained break raises an alert with structured context (actor, resource, operation, domain, run identifier) and the run exits non-zero; the harness never fails open or reports success with unresolved breaks.
- A gate report generator produces a per-domain report stating consecutive clean days achieved against the required window (thirty days for Customer and Claims, forty-five days spanning two month-ends for Billing, Premium, Commission and Policy) plus a pass or fail gate verdict and the rollback trigger conditions.
- A break-injection test suite deliberately introduces a one-cent amount difference, a missing row, an extra row, a rounding-direction difference, a trailing-space difference and a null-versus-blank difference, and asserts that each is detected, classified correctly and reported with the exact business key.
- Unit tests cover comparator arithmetic, checksum computation, tolerance rules (zero tolerance on money) and gate-day accumulation; system integration tests run a full domain reconciliation against a Testcontainers PostgreSQL 17 target plus a simulated source and assert clean-run and broken-run outcomes; mock data and fixtures include committed matched and deliberately mismatched per-domain datasets so the suite runs with no live Db2 connection.

**Depends on:** WO-073, WO-072

---

## Golden-Output Regression Harness and Batch Fault Injection

### [P0] Deterministic Seed Data Harness for Batch Regression Fixtures

WHAT & WHY: PCIS has zero automated tests and zero seeded test data, so installment arithmetic, commission calculation, reserve drawdown and the archive-verify-then-delete sequence are validated only by human code reading. Golden-output regression is impossible without a byte-for-byte reproducible starting database state, so the first deliverable of this epic is a deterministic seed data harness that can materialise an identical fixture population on both the legacy Db2 for i baseline and the target PostgreSQL 17 model. IMPACT: adds a new Maven test-harness module with Testcontainers-backed PostgreSQL provisioning, Flyway-managed regression schema, versioned SQL/CSV seed fixtures, a fluent scenario builder for policies, billing plans, installments, invoices, agents, commission plans, claims, reserves and audit rows, plus a Db2-for-i dialect emitter so the same logical fixture loads on the legacy side for baseline capture. WHAT DONE LOOKS LIKE: any engineer or CI job can request a named scenario (for example billing-frequency-matrix, delinquency-grace-boundary, reserve-drawdown-above-cession-threshold, audit-retention-cutoff) and receive a fully populated, sequence-reset, fixed-clock database whose row images are identical on every invocation across machines. SCOPE BOUNDARIES: does not implement any Spring Batch job, does not capture golden outputs, does not perform comparison, does not migrate production data, and does not define production Flyway migrations for the live PostgreSQL schema — the regression schema is test-scoped and derived from PCIS_Database_Design.md. DEPENDENCIES: consumes the table definitions documented in PCIS_Database_Design.md and the host-variable precision facts visible in BIL003B.cbl, CMM001B.cbl, PRM005B.cbl, CLM006B.cbl, POL006B.cbl and AUD002B.cbl; it is the blocker for all remaining stories in this epic.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:EPIC-08, testing, test-data, complexity:high, platform |

**Acceptance Criteria**
- Running the same named scenario twice in a row produces byte-identical extracts of every seeded table, including all DECIMAL(9,2) and DECIMAL(11,2) columns, verified by a checksum assertion in the harness self-test.
- Scenario catalog covers at minimum: billing frequency M, Q, S and an out-of-domain value; days-out exactly 15, 14 and 16 relative to the fixed reference date; installments exactly 10, 9 and 11 days past due; reserves where APPROVED_AMT exceeds PAID_TO_DATE and reserves where it does not; payment amounts at 99999.99, 100000.00 and 100000.01 against the cession threshold; agents with an in-force commission plan and agents with none; audit rows immediately either side of a 365-day cutoff.
- All business document keys (CUST_ID, AGT_ID, POL_NBR, CLM_NBR) are allocated from harness-controlled SEQUENCE objects as fixed-length character values, never IDENTITY columns, and detail surrogate keys are reset to a known start value before every scenario load.
- Unit tests: harness self-tests assert sequence reset, fixed-clock injection, scale preservation on money columns and idempotent reload of every scenario; all passing in CI.
- System integration tests: a Testcontainers-based integration test provisions PostgreSQL 17, applies the regression schema via Flyway, loads all scenarios and asserts referential integrity across POLICY_T, BILLING_PLAN_T, BILLING_SCHEDULE_T, INVOICE_T, CLAIM_T, CLAIM_RESERVE_T, AGENT_COMMISSION_T and AUDIT_LOG_T.
- Mock data/fixtures: all seed data is synthetic, contains no real customer values, uses non-production tax IDs, emails and phones, and is committed under the test resources tree with a README documenting each scenario's intent.
- A Db2-for-i dialect emitter produces loadable DML for the same logical fixture set, and its output is validated by a dialect unit test asserting FETCH FIRST, CURRENT DATE and DECIMAL declarations are emitted in Db2-for-i-compatible form.

### [P0] Capture COBOL Baseline Golden Outputs with Determinism Controls

WHAT & WHY: Functional parity is the gate for every domain cutover, and parity can only be asserted against a trusted baseline. The COBOL programs are the only authority for behaviours that no document settles — the exact rounding of COMPUTE ROUNDED commission, the reuse of HV-INSTALLMENT-NBR as a scratch days-out counter, the silent skip of out-of-window candidates, and the archive-verify-then-delete ordering. This story builds the tooling that runs each COBOL batch program against a seeded fixture and captures its complete post-run effect as a committed golden artifact. It must also neutralise a confirmed determinism hazard: none of the five batch cursors is declared WITH HOLD yet all five COMMIT inside their fetch loops, so run order is not guaranteed reproducible without explicit controls. IMPACT: adds baseline runner scripts (CL plus shell wrapper) for AUD002B, BIL003B, CMM001B, PRM005B, CLM006B and POL006B, a canonical extractor that serialises post-run table state, RPT_RUN_LOG_T counters and job-log lines into normalised text, and a committed golden artifact tree keyed by program and scenario. WHAT DONE LOOKS LIKE: an operator runs one command per program per scenario, three consecutive runs produce identical artifacts, and the resulting goldens are committed as the executable specification the Java rewrite must match to the cent. SCOPE BOUNDARIES: does not implement the comparison engine, does not write any Java batch job, does not modify COBOL business logic, and does not provision the IBM i environment itself. DEPENDENCIES: blocked by WO-080 for the seeded fixtures and the Db2-for-i dialect emitter; produces the goldens consumed by WO-082 and WO-083.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:EPIC-08, testing, golden-output, cobol, complexity:high |

**Acceptance Criteria**
- A single documented command per program per scenario restores the seeded library, runs the COBOL program with an injected reference date, and writes a canonical golden artifact set to a deterministic path under the golden resources tree.
- Three consecutive capture runs of every program/scenario pair produce byte-identical artifacts; any pair that does not is automatically quarantined with a recorded reason rather than committed as a golden.
- Captured artifacts include full post-run row images for every mutated table, the RPT_RUN_LOG_T counter row, program DISPLAY output, and the final program completion status, with timestamps normalised and generated surrogate keys rewritten to stable ordinal placeholders.
- The captured rounding behaviour of commission (COMPUTE HV-COMMISSION-AMT ROUNDED) and installment division (HV-PREM-ANNUAL / HV-INSTALLMENT-CNT) is recorded as an explicit, documented rounding-oracle artifact resolving the HALF_UP versus truncation ambiguity from evidence rather than assumption.
- The cursor determinism hazard is documented in a capture-runbook section, and the runner enforces the mitigations: exclusive single-job execution, no concurrent mutation of fixture tables, and a deterministic ORDER BY applied only in the extractor and never in the program under test.
- Unit tests: the canonical extractor and normalisation rules are unit tested against synthetic result sets, asserting timestamp masking, surrogate-key remapping, money scale preservation and stable row ordering; all passing.
- System integration tests: an end-to-end capture is exercised against a PostgreSQL replay harness standing in for the legacy runtime so the capture pipeline is validated in CI without IBM i access, with the IBM i path covered by the committed runbook.
- Mock data/fixtures: goldens for every scenario in the WO-080 catalog are committed for all six batch programs plus the two evidenced interactive transactions, with provenance metadata recording program, scenario, reference date, capture timestamp and capture tool version.

**Depends on:** WO-080

### [P0] Cent-Level Golden Output Comparison Engine and Diff Reporting

WHAT & WHY: Captured goldens are only useful if a comparison engine can assert equivalence with zero monetary tolerance and produce a diff that an engineer or a Finance analyst can act on within seconds. Cent-level reconciliation is the stated gate for every domain cutover, so the comparator must compare BigDecimal money at fixed scale, ignore only explicitly declared non-deterministic fields, and never mask a real difference behind loose normalisation. IMPACT: adds a reusable comparison library to the regression harness with a JUnit 5 assertion API, table-and-key-aware row matching, declarative normalisation rules, commit-boundary and counter assertions against the captured RPT_RUN_LOG_T row, and machine-readable plus human-readable diff reports published as build artifacts. WHAT DONE LOOKS LIKE: a single assertion call compares an actual post-run database state against a named golden and, on failure, emits a report naming table, business key, column, expected and actual values with a monetary delta, together with any counter or commit-boundary divergence. SCOPE BOUNDARIES: does not capture goldens, does not implement any batch job, does not build the CI gate wiring, and does not perform live Db2-to-PostgreSQL parallel-run reconciliation of production data. DEPENDENCIES: blocked by WO-080 for the fixture and extractor conventions and WO-081 for the committed golden artifact format; consumed by WO-083 and WO-084.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | epic:EPIC-08, testing, golden-output, complexity:medium |

**Acceptance Criteria**
- A single assertion entry point compares actual post-run state against a named golden and passes only when every table, row and column matches, with zero tolerance on all NUMERIC(9,2) and NUMERIC(11,2) columns.
- A one-cent difference in any monetary column, a missing row, an extra row, or a status-code difference each cause a distinct, clearly worded failure naming table, business key, column, expected value, actual value and delta.
- Normalisation is declarative and auditable: only fields explicitly listed as non-deterministic (capture timestamps, IDENTITY surrogate keys, job identifiers) may be masked, and an attempt to normalise a monetary or status column fails the comparator's own configuration validation.
- Counter assertions compare the actual RPT_RUN_LOG_T row against the golden for selected, updated, error and domain-specific counters, and commit-boundary assertions verify the observed number of committed units matches the golden's one-item-per-commit expectation.
- Diff output is emitted both as JSON for CI consumption and as a readable text report, written to a stable artifact path, and truncated safely with a total-difference count when a run produces thousands of divergences.
- Unit tests: comparator unit tests cover equal states, one-cent divergence, row missing, row extra, column type mismatch, scale mismatch, null versus empty string, ordering differences and oversized diffs; all passing.
- System integration tests: an integration test loads a WO-080 scenario, applies a deliberately mutated writer, and asserts the comparator fails with the exact expected diff payload, proving the engine detects real divergence end to end.
- Mock data/fixtures: paired golden and actual fixture sets representing each failure class are committed so comparator tests run with no external dependency.

**Depends on:** WO-080, WO-081

### [P0] Golden-Output Regression Suites for Six Batch Programs

WHAT & WHY: The programme objective is that 100 percent of the six batch programs and both evidenced interactive transactions are covered by golden-output tests, because money-moving logic has never had an executable specification. This story turns the captured goldens into an enforceable regression suite: one test class per program, one test method per scenario, each asserting cent-level equivalence, counter parity and commit-boundary parity. The suite doubles as the acceptance gate that every Spring Batch rewrite must pass, and as characterization coverage for behaviours the design documents do not settle — silent skipping of out-of-window billing candidates, the COMM_CALC_FLAG idempotency guard, the archive-verify-then-delete ordering, the full-outstanding-amount reserve drawdown, and the deliberate non-reproduction of the legacy continue-after-audit-failure behaviour. IMPACT: adds regression suites for AUD002B, BIL003B, CMM001B, PRM005B, CLM006B and POL006B plus characterization suites for the CUS001A and POL001A interactive transactions, all wired to the WO-080 fixtures, WO-081 goldens and WO-082 comparator. WHAT DONE LOOKS LIKE: running one Maven command executes every scenario for every program and reports pass or a precise cent-level diff; the suite fails if any program or scenario is uncovered. SCOPE BOUNDARIES: does not implement the Spring Batch jobs, the authorization service, the audit service or the domain services themselves — those live in other epics; this story only defines and enforces their expected behaviour. DEPENDENCIES: blocked by WO-082 (and transitively WO-080 and WO-081).

| Field | Value |
|---|---|
| Story Points | 13 |
| Hours | 130h |
| Priority | P0 |
| Labels | epic:EPIC-08, testing, golden-output, regression, complexity:high |

**Acceptance Criteria**
- One regression suite exists per batch program (AUD002B, BIL003B, CMM001B, PRM005B, CLM006B, POL006B) with a test method per committed scenario, each asserting table-level cent equivalence, RPT_RUN_LOG_T counter parity and commit-boundary parity via the WO-082 comparator.
- Characterization suites exist for the two evidenced interactive transactions (customer creation per CUS001A and policy creation per POL001A), asserting the multi-row insert set, duplicate tax-ID rejection, and the created billing schedule row.
- A coverage guard test enumerates the golden artifact tree and fails the build if any program or scenario present in the goldens has no corresponding executing test, so coverage cannot silently regress to below 100 percent of captured behaviour.
- Billing generation assertions pin: next due date as last due date plus 1, 3, 6 or 12 months by frequency M, Q, S and other; generation only when the computed due date is within the configured 15-day lead window; installment amount as annual premium divided by installment count at scale 2 using the rounding oracle captured in WO-081.
- Delinquency assertions pin: fully-paid becomes paid; unpaid beyond the configured 10-day grace becomes late and increments the delinquency counter; otherwise status remains due; and the old-to-new status transition appears in the audit event.
- Commission assertions pin: only installments with BILL_STATUS P and null COMM_CALC_FLAG are selected; commission equals paid amount times rate divided by 100 at the captured rounding; the COMM_CALC_FLAG stamp makes a second run produce zero additional ledger rows (idempotency); and an agent with no in-force plan yields no ledger row plus an actionable exception record rather than a silent counter bump.
- Claim payment assertions pin: payment amount equals approved amount minus paid-to-date; the reinsurance referral outcome is raised above the configured threshold; and a dedicated negative test asserts the legacy behaviours of paying without an authority check and of overwriting reserve history in place are NOT reproduced, expressed as explicit expected-to-fail-if-present assertions.
- Audit archive assertions pin: chunk-bounded archive then verify then delete ordering; no audit row is ever absent from the archive after being deleted from the live table; a verification mismatch quarantines and alerts instead of silently halting; and the commit chunk size is externally configured at 1000 or fewer rows.
- Unit tests: pure monetary calculation logic extracted for assertion (installment division, commission rate application, reserve drawdown, days-past-due, next-due-date arithmetic) has direct unit tests at BigDecimal scale 2 with boundary and adversarial inputs; all passing.
- System integration tests: every regression suite runs against a Testcontainers PostgreSQL instance with the WO-080 fixtures loaded, exercising the real persistence boundary rather than mocks.
- Mock data/fixtures: all fixtures and goldens required by the suites are committed, and the entire suite runs offline with no IBM i, network or external service dependency.

**Depends on:** WO-080, WO-081, WO-082

### [P1] Batch Fault Injection Proving Restart Without Duplicate Writes

WHAT & WHY: The COBOL batch skeleton has no restart capability — a late failure in the nightly window forces a full rerun, and the audit archive job halts the entire run on a single chunk failure, leaving older records unarchived until next month-end. The target requires that 100 percent of migrated jobs restart from the last committed chunk with zero duplicate and zero orphaned financial records under fault injection, and this is a mandatory gate condition for batch deployment. This story builds the fault-injection suite that proves it. IMPACT: adds a fault-injection framework to the regression harness (statement-level datasource failure interceptor, item-index failure writer, mid-chunk abort and restart driver via the Spring Batch JobRepository and JobOperator), invariant assertion library for financial integrity, and per-job restart tests for all six batch jobs. WHAT DONE LOOKS LIKE: for every job, injecting a failure at a configurable item index and restarting produces a final database state that satisfies every financial invariant and matches the clean-run golden, with a non-zero exit status and an alert emitted on the failed attempt. SCOPE BOUNDARIES: does not implement the Spring Batch jobs, chunk sizing configuration or the alerting backend itself; does not perform chaos testing of Kubernetes or the database platform; does not cover interactive transaction rollback. DEPENDENCIES: blocked by WO-083 for the regression suites and invariant definitions, and by WO-080 and WO-082 for fixtures and the comparator.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P1 |
| Labels | epic:EPIC-08, testing, fault-injection, reliability, complexity:high |

**Acceptance Criteria**
- A fault-injection API allows a test to fail a job at a configurable item index, at a configurable statement ordinal, or by aborting the process mid-chunk, and to then restart the job through the Spring Batch JobOperator restart path.
- For every one of the six batch jobs, a restart test asserts the final state after failure-plus-restart is identical to the clean-run golden via the WO-082 comparator, with zero duplicate and zero orphaned financial rows.
- Named invariants are asserted after every fault-injection run: no duplicate COMMISSION_LEDGER_T row for the same BILL_SCHED_ID; no CLAIM_PAYMENT_T row without a corresponding reserve status transition; no AUDIT_LOG_T row deleted without a verified AUDIT_LOG_ARCHIVE_T copy; no BILLING_SCHEDULE_T installment without its paired INVOICE_T row; no financial mutation persisted without its audit event.
- A failed attempt exits with a non-zero status, emits a structured error record with actor, resource and operation context, and raises an alert when the configured error threshold is exceeded — asserted rather than assumed.
- A configuration assertion verifies the archive job commit chunk size is externally configured and no greater than 1000 rows, and that each of the per-item jobs commits one item per chunk in line with the legacy commit-scope prologues.
- Restarting a job that already completed successfully is a no-op that produces zero additional financial rows, and restarting after a failure never reprocesses an already-committed item.
- Unit tests: the failure interceptor, item-index failure writer and invariant assertion library have unit tests covering trigger accuracy, off-by-one boundaries and invariant violation detection; all passing.
- System integration tests: fault-injection restart scenarios run end to end against Testcontainers PostgreSQL with a real Spring Batch JobRepository, exercising the actual restart metadata path rather than a simulated one.
- Mock data/fixtures: fault-injection scenarios reuse the committed WO-080 fixtures and WO-081 goldens; any additional fixtures needed for large-chunk archive testing are generated deterministically and committed.

**Depends on:** WO-080, WO-082, WO-083

### [P1] CI Gate Wiring Coverage and Regression Evidence Artifacts

WHAT & WHY: A regression harness that is not enforced by the pipeline is documentation, not a control. The programme requires the golden-output suite to execute on every commit against a seeded database, requires 90 percent or better line coverage on monetary calculation code, and makes a passing fault-injection test a gate condition for batch deployment. Today there is no build pipeline at all, so there is nowhere to enforce any of it. This story wires the harness into the Forge Shipping pipeline as a blocking gate and publishes machine-readable evidence artifacts for compliance and parallel-run sign-off. IMPACT: adds pipeline stage definitions invoking the Maven build, unit tests, Testcontainers-backed golden-output regression and fault-injection suites; adds JaCoCo coverage enforcement scoped to the monetary calculation packages; publishes coverage reports, comparator diff reports and fault-injection invariant results as retained build artifacts; and adds a nightly full-suite schedule with duration tracking. WHAT DONE LOOKS LIKE: a commit that breaks a cent of monetary behaviour, drops monetary coverage below 90 percent, or breaks batch restartability cannot be pushed to the registry or deployed, and every build leaves an evidence bundle an auditor can read. SCOPE BOUNDARIES: does not provision Kubernetes, the container registry, ArgoCD or the alerting backend; does not define the security scan stages, image signing or deployment manifests; does not implement the harness or suites themselves. DEPENDENCIES: blocked by WO-083 and WO-084.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | epic:EPIC-08, ci-cd, quality-gate, observability, complexity:medium |

**Acceptance Criteria**
- A pipeline definition runs, in order, the Maven build with unit tests, the Testcontainers-backed golden-output regression suite, and the fault-injection restart suite, and any failure in any of the three blocks the build before any image push or deployment step.
- JaCoCo coverage enforcement is scoped to the monetary calculation packages and fails the build below 90 percent line coverage, with the scoped package list committed as configuration rather than hardcoded in a script.
- On failure, the comparator JSON and text diff reports, the fault-injection invariant results and the coverage report are published as retained build artifacts and the build summary names the failing program and scenario without requiring a local rerun.
- A batch-specific gate condition is expressed in the pipeline so that batch job artifacts cannot be promoted unless the fault-injection suite passed for that revision, matching the documented deployment gate.
- A nightly scheduled run executes the full suite including long-running archive chunk scenarios, records total duration as a tracked metric, and alerts on both failure and duration regression beyond a configured threshold.
- Test flakiness is controlled: a quarantine mechanism exists for a test proven non-deterministic, quarantining requires a linked issue and an owner, and quarantined tests are reported in the build summary so quarantine cannot become permanent silently.
- Unit tests: pipeline helper scripts and the coverage-scope configuration loader have unit tests, and a deliberate coverage-below-threshold fixture proves the gate fails as designed; all passing.
- System integration tests: a dry-run pipeline execution on a branch demonstrates the full stage sequence, including a deliberately introduced one-cent regression that is caught and blocks the build with an actionable diff artifact.
- Mock data/fixtures: N/A — this story consumes the fixtures and goldens committed by WO-080 and WO-081 and adds no new test data of its own.

**Depends on:** WO-083, WO-084

---

## Customer Domain Service (Phase 1 Thin End-to-End Slice)

### [P0] Create PostgreSQL customer schema with Flyway migrations

WHAT & WHY: The Customer domain is the Phase 1 thin end-to-end slice, and nothing can be built until CUSTOMER_T, CUSTOMER_ADDRESS_T and CUSTOMER_CONTACT_T exist as versioned PostgreSQL 17 objects that preserve the Db2 for i semantics documented in PCIS_Database_Design.md and used by CUS001A.cbl. Business document keys must stay SEQUENCE-generated fixed-length character keys (CUST_ID VARCHAR(10)), never IDENTITY, because POL001A, BIL003B, CMM001B and CLM006B all carry CUST_ID/POL_NBR as PIC X host variables and downstream reconciliation compares those exact strings. IMPACT: introduces a new Flyway migration module under the customer-svc source tree, PostgreSQL DDL for three tables plus SEQ_CUSTOMER_ID, SEQ_CUSTOMER_ADDRESS_ID and SEQ_CUSTOMER_CONTACT_ID, and a machine-readable data-classification manifest that assigns a tier to every customer column. WHAT DONE LOOKS LIKE: a clean database can be brought to the current customer schema by running the migration; CUST_ID values are ten-character zero-padded strings allocated from a sequence; monetary and numeric columns use NUMERIC with the exact precision of the COMP-3 originals; every column is classified and a build-time check fails when a column is unclassified. SCOPE BOUNDARIES: no Java entities, no REST endpoints, no data extraction from Db2 for i, no audit or approval tables, and no policy/claims tables. DEPENDENCIES: assumes the shared build pipeline and PostgreSQL container fixtures from the platform foundation epic are available; all later Customer stories build on these migrations.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | complexity:medium, database, migration, customer-domain, phase-1, data-classification |

**Acceptance Criteria**
- Flyway migration V1 creates customer schema objects: CUSTOMER_T, CUSTOMER_ADDRESS_T, CUSTOMER_CONTACT_T, and three sequences, and applies cleanly against an empty PostgreSQL 17 database.
- CUST_ID is VARCHAR(10) populated from SEQ_CUSTOMER_ID via a zero-padded formatting helper, never a SERIAL or IDENTITY column; ADDRESS_ID and CONTACT_ID are BIGINT GENERATED ALWAYS AS IDENTITY per the design document convention.
- Column types mirror the Db2 for i design: CUST_NAME VARCHAR(60), TAX_ID VARCHAR(11), DOB DATE, EMAIL VARCHAR(60), PHONE VARCHAR(15), CUST_STATUS CHAR(1), CREDIT_SCORE SMALLINT, plus CRT_USER, CRT_TIMESTAMP, UPD_USER, UPD_TIMESTAMP on every table.
- A unique constraint or partial unique index enforces one active customer per TAX_ID, replacing the soft duplicate warning implemented by the SELECT COUNT(*) check in CUS001A.cbl.
- A classification manifest file assigns a tier (Public, Internal, Confidential, Restricted) to every column of all three tables; TAX_ID, DOB, EMAIL and PHONE are Restricted, and a build-time validation task fails the build if any column is missing a tier.
- Unit tests: migration checksum and DDL assertions run as automated tests, including a test proving sequence-based CUST_ID formatting produces exactly ten characters for values 1 and 9999999999.
- System integration tests: a Testcontainers PostgreSQL test starts an empty database, runs Flyway migrate, inserts one customer with an address and a contact, and asserts referential integrity and duplicate-tax-ID rejection.
- Mock data/fixtures: a seed SQL fixture with at least 20 synthetic customers (individual and business, active and inactive, with and without optional address/contact) is committed and loaded by the integration test; no real customer data is used.
- Rollback: every migration has a paired down migration or a documented expand-then-contract note, and applying then reverting leaves the database at the prior state.

**Depends on:** Shared Maven BOM and CI pipeline, PostgreSQL 17 managed instance / Testcontainers image

### [P0] Build customer-svc domain and persistence layers

WHAT & WHY: CUS001A.cbl couples presentation, validation, key generation, persistence and audit into one procedural program driven by the CUSMNTD1 display file, which makes the customer onboarding rules untestable and unreusable. The Phase 1 slice needs those rules re-expressed as a framework-free domain layer plus an infrastructure layer that re-hosts every EXEC SQL statement through JPA and JdbcClient, because PostgreSQL has no static-bind equivalent for the precompiled packages the COBOL relies on. IMPACT: creates the customer-svc Spring Boot 3.5 module with controller, application, domain and infrastructure packages; adds Customer, CustomerAddress and CustomerContact aggregates, a CustomerValidator replacing CUSVAL01, a CustomerIdAllocator replacing the sequence VALUES round trip, and repository implementations over the WO-090 schema. WHAT DONE LOOKS LIKE: a single application-layer use case creates a customer with optional address and contact inside one transaction, emits a transactional-outbox audit event, rejects duplicate tax IDs and invalid fields with typed domain errors, and is fully unit-testable without a database or HTTP layer. SCOPE BOUNDARIES: no HTTP endpoints or OpenAPI contract (WO-092), no masked read projection for downstream consumers (WO-094), no golden-output parity harness (WO-093), no agent, policy or claims logic, and no UI. DEPENDENCIES: requires the WO-090 schema and classification manifest; consumes the shared audit outbox and authorization client contracts from the shared kernel epic.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | complexity:high, backend, spring-boot, customer-domain, phase-1, cobol-migration |

**Acceptance Criteria**
- A customer-svc Maven module exists with four packages (controller, application, domain, infrastructure) and an enforced dependency rule: the domain package imports no Spring, JPA or Jackson types, verified by an ArchUnit test.
- Domain layer contains Customer, CustomerAddress and CustomerContact with invariants ported from CUS001A and CUS_Module_Design_Document.md: mandatory name, customer type and tax ID; enumerated gender, marital status and customer status; credit score bounded 0 to 999; optional address and contact groups all-or-nothing.
- CustomerValidator replaces CUSVAL01 and returns a structured collection of field-anchored violations rather than terse coded messages, and all violations for one submission are returned together as the COBOL WS-MSG-ENTRY table did.
- CustomerIdAllocator obtains CUST_ID from SEQ_CUSTOMER_ID and formats it to a ten-character key, allocating in blocks to avoid a per-record sequence round trip.
- CreateCustomerUseCase persists customer, optional address and optional contact plus one audit outbox record in a single transaction; a forced audit-outbox failure rolls back the customer insert, proving the legacy continue-after-audit-failure behaviour is not reproduced.
- Duplicate tax ID is detected by the database constraint and mapped to a distinct typed domain error, and a concurrent-insert test proves exactly one of two simultaneous submissions succeeds.
- Unit tests: domain invariants, validator rules, key formatting and use-case orchestration are unit tested with in-memory fakes and no infrastructure mocks; coverage on the domain package is at least 90 percent line coverage.
- System integration tests: Testcontainers PostgreSQL tests exercise create, read-by-id and update paths through the real repositories and assert persisted column values, audit outbox rows and transaction rollback behaviour.
- Mock data/fixtures: committed synthetic customer fixtures and object-mother builders are used by all tests so the suite runs offline with no external service or database dependency.

**Depends on:** WO-090 customer schema migrations, Shared audit outbox contract, Shared Maven BOM

### [P0] Expose versioned customer REST API with deny-by-default authorization

WHAT & WHY: PCIS today has zero API surface — all customer interaction happens through the CUSMNTD1, CUSINQD1, CUSLSTD1 and CUSDELD1 5250 panels, and authorization is only a menu-option gate in ROLE_MENU_T, which is exactly the presentation-layer access control OWASP A01 forbids relying on. The Phase 1 slice must publish a versioned /v1/customers REST contract with server-side deny-by-default authorization so the accessible web UI and downstream services have a real integration point. IMPACT: adds the customer-svc controller layer, springdoc OpenAPI 3.1 contract, Spring Security 6 resource-server configuration with JWT validation, method-level @PreAuthorize on every operation, RFC 9457 problem-detail error mapping, and request DTO validation. WHAT DONE LOOKS LIKE: POST, GET, PATCH and search operations under /v1/customers work end to end against the WO-091 use cases; an unauthenticated call returns 401, an authenticated call without the required scope returns 403 with no stack trace, validation failures return 400 with field-anchored problem details, and a duplicate tax ID returns 409 with a distinct reason code. SCOPE BOUNDARIES: no web UI implementation, no masked cross-domain read projection (WO-094), no identity-provider provisioning, no rate limiting or gateway configuration, and no policy/claims endpoints. DEPENDENCIES: requires WO-091 domain and application layers; assumes the OIDC issuer and JWKS endpoint from the shared kernel authorization epic are available.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | complexity:high, api, security, backend, customer-domain, phase-1 |

**Acceptance Criteria**
- Endpoints exist and are documented in a generated OpenAPI 3.1 contract: POST /v1/customers, GET /v1/customers/{custId}, PATCH /v1/customers/{custId}, GET /v1/customers (paged search by name, tax-ID suffix and status).
- Spring Security 6 is configured as an OAuth2 resource server with local RS256 validation against a cached JWKS; requests with no or an invalid token return 401 and never reach the application layer.
- Deny-by-default is enforced: the security configuration denies any request not explicitly permitted, every controller method carries @PreAuthorize, and an automated test enumerates all mapped endpoints and fails if any lacks an authorization annotation.
- A caller with a valid token but insufficient authority receives 403 with an RFC 9457 problem detail containing a reason code and no stack trace, secret or SQL fragment.
- Validation failures return 400 with an RFC 9457 problem detail listing every field-anchored violation from the WO-091 validator; duplicate tax ID returns 409 with a distinct reason code separate from generic validation.
- Restricted-tier fields in responses are serialized according to the classification manifest — tax ID renders as last four characters only unless the caller holds the explicit unmask permission, and every unmask is itself audited.
- Unit tests: controller slice tests (MockMvc with mocked application layer) cover success, 400, 401, 403, 404 and 409 paths and assert response body shape and content type application/problem+json.
- System integration tests: full-stack tests with Testcontainers PostgreSQL and a mock OIDC issuer create, read, update and search a customer over HTTP and assert persisted state and audit outbox rows.
- Mock data/fixtures: committed JWT fixtures for authorized, under-privileged and unmask-privileged principals plus request/response JSON fixtures allow the suite to run offline against no external identity provider.

**Depends on:** WO-091 customer domain and application layers, Shared authorization service contract, OIDC issuer JWKS endpoint

### [P1] Publish masked customer read projection for downstream consumers

WHAT & WHY: POL001A, CLM005A and the reporting programs read CUSTOMER_T directly today — database-as-integration-bus — which is why policy, claims and reporting cannot migrate independently and why unmasked tax IDs, dates of birth, emails and phone numbers spread across every module. The Customer slice must publish a narrow, versioned, masked read projection so downstream domains consume a contract instead of a table, and so no restricted-tier value leaves customer-svc in the clear. IMPACT: adds a read-only projection endpoint and DTO in customer-svc, a consumer-scoped field set derived from what POL001A, CLM and RPT actually read, contract tests published for those consumers, and masking rules applied before serialization. WHAT DONE LOOKS LIKE: a consumer calls GET /v1/customers/{custId}/projection and receives only the fields it needs with restricted values masked; direct table access by other services is documented as prohibited; consumer-driven contract tests fail the build if a field is removed or narrowed; and an automated log/response scan finds zero unmasked restricted values. SCOPE BOUNDARIES: no changes to POL001A, CLM or RPT COBOL programs, no policy-svc or claims-svc implementation, no event streaming of customer changes, no bulk extract or reporting replica work, and no unmask workflow beyond reusing the WO-092 permission gate. DEPENDENCIES: requires WO-092 for the security and masking foundation and WO-090 for the classification manifest.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | complexity:medium, api, contract-testing, pii-masking, customer-domain, phase-1 |

**Acceptance Criteria**
- GET /v1/customers/{custId}/projection returns a documented, versioned read projection containing only the fields evidenced as needed by policy, claims and reporting consumers, with a committed field-to-consumer mapping document.
- All restricted-tier fields in the projection are masked before serialization: tax ID as last four characters, email as domain only, phone as last four digits, date of birth as year only; the unmasked variants are not present anywhere in the payload.
- The projection endpoint is read-only, requires an explicit service-to-service scope such as customer:project, and denies by default; no mutating verb is exposed on the projection path.
- Consumer-driven contract tests exist for at least three consumer roles (policy, claims, reporting) and the build fails when a projection field is removed, renamed or type-narrowed, enforcing additive-only evolution.
- An automated scanner runs in CI over projection responses and service logs and fails if any unmasked restricted-tier pattern (tax ID, full email, full phone, full date of birth) is detected.
- A documented prohibition on direct CUSTOMER_T reads by other services is added to the interface documentation, together with the migration note that POL001A, CLM and RPT consumers move to this projection at their own cutover.
- Unit tests: masking strategies, projection mapping and field-set selection per consumer scope are unit tested including null and empty-value handling.
- System integration tests: Testcontainers PostgreSQL plus mock OIDC tests call the projection endpoint as each consumer role and assert field presence, masking and 403 for an unscoped caller.
- Mock data/fixtures: synthetic customers with edge-case restricted values (short tax ID, missing email, international phone, null date of birth) plus per-consumer JWT fixtures are committed so contract and scan tests run offline.

**Depends on:** WO-090 classification manifest, WO-092 security and masking foundation

### [P0] Establish customer slice parity harness and CI quality gates

WHAT & WHY: There is no automated test harness anywhere in PCIS, so customer onboarding behaviour is verified only by human inspection and there is no evidence base for the Phase 1 cutover gate. The Customer slice needs a golden-output parity harness that captures the COBOL CUS001A baseline behaviour as fixtures, replays the same inputs through customer-svc, and asserts field-level equivalence — plus the CI quality gates (coverage, layering, security scan, contract, masking) that make every later domain inherit the same rigour. IMPACT: adds a parity test module with recorded baseline fixtures, a reconciliation comparator, a Forge Shipping pipeline definition for customer-svc with build, scan, push, deploy and test stages, coverage and endpoint-authorization gates, and a runbook describing how to run and interpret the harness. WHAT DONE LOOKS LIKE: a single command runs the full customer parity suite offline and reports zero unexplained differences; the pipeline fails on coverage below threshold, an unauthorized endpoint, a contract break, an unmasked restricted value, or a parity mismatch; and an operator can follow the runbook to reproduce and triage any break. SCOPE BOUNDARIES: no parity harness for billing, claims, policy or batch programs; no IBM i build automation beyond consuming already-captured baseline fixtures; no production data extraction; no performance or load testing. DEPENDENCIES: requires WO-091, WO-092 and WO-094 so the full slice is exercisable end to end.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | complexity:high, testing, ci-cd, quality-gate, customer-domain, phase-1 |

**Acceptance Criteria**
- A parity test module exists that loads recorded baseline fixtures representing CUS001A input/output pairs (customer with address and contact, customer without optional data, duplicate tax ID rejection, invalid field set, boundary credit score) and replays them through customer-svc.
- A reconciliation comparator asserts field-level equivalence between baseline expected records and customer-svc persisted records, reporting differences as a structured list of table, key, field, expected and actual rather than a single boolean.
- The suite runs offline in one command with Testcontainers PostgreSQL and a mock OIDC issuer, requiring no IBM i connection, no shared database and no external identity provider.
- CI gates are enforced and each demonstrably fails on a deliberately broken build: line coverage below 90 percent on domain and monetary/precision-sensitive packages, any endpoint missing an authorization annotation, a non-additive OpenAPI or consumer contract change, an unmasked restricted-tier value found by the scanner, and any parity mismatch.
- A Forge Shipping pipeline definition for customer-svc runs build, unit test, parity/integration test, four parallel security scans, SBOM generation, image push and deploy stages, and no registry push occurs unless all scans and gates pass.
- Unit tests: the comparator itself is unit tested, including cases where a field differs, a record is missing, an extra record exists, and where a difference is on a null-versus-empty boundary.
- System integration tests: an end-to-end scenario creates a customer over HTTP, reads it back, reads the masked projection as a consumer role, and asserts the audit outbox contents, all inside the parity suite run.
- Mock data/fixtures: all baseline fixtures are synthetic (no real customer data), committed under version control with a provenance note per fixture, and the suite is deterministic across repeated runs.
- A runbook is committed describing how to run the harness locally, how to interpret a mismatch report, how to add a new baseline fixture, and which gate failures block a cutover versus which are advisory.

**Depends on:** WO-091 domain and persistence layers, WO-092 REST API and security, WO-094 masked read projection, Forge Shipping pipeline engine

---

## Claims Domain Service and Enforced Payment Authority (Phase 2)

### [P0] Provision Claims PostgreSQL Schema With Exact Decimal Precision

The Claims domain currently has no relational target: CLAIM_T, CLAIM_RESERVE_T, CLAIM_PAYMENT_T, CLAIM_ADJUSTER_T, CLAIM_NOTE_T and RECOVERY_T exist only as Db2 for i definitions in PCIS_Database_Design.md plus embedded SQL references inside CLM006B.cbl, and APPROVAL_T is documented as table 43 but referenced by no program. Phase 2 of the modernization requires a cloud PostgreSQL 17 schema for claims-svc, created through versioned Flyway migrations, that preserves the exact conventions the COBOL baseline depends on: business document keys are SEQUENCE-generated fixed-length VARCHAR values (never IDENTITY), detail keys are BIGINT GENERATED ALWAYS AS IDENTITY, and every monetary column is NUMERIC(9,2) or NUMERIC(11,2) matching COMP-3 S9(9)V99 and S9(11)V99. Without this schema no reserve drawdown, payment insert, authority lookup or approval linkage can be implemented, and the parallel-run reconciliation gate cannot compare cent-level values. This story also assigns a data classification tier to every claims table and column so the build-time unclassified-table gate and the audit masking layer have authoritative metadata. Reserve history must be modelled append-only so BR-03 (reserve history is append-only) is enforced by the database rather than by application convention.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:claims, database, migration, complexity:high, phase-2 |

**Acceptance Criteria**
- Flyway migration scripts create all Claims tables (CLAIM_T, CLAIM_RESERVE_T, CLAIM_RESERVE_HISTORY_T, CLAIM_PAYMENT_T, CLAIM_ADJUSTER_T, CLAIM_NOTE_T, APPROVAL_T, RECOVERY_T) and run cleanly from an empty PostgreSQL 17 database with zero manual steps.
- Every monetary column is NUMERIC(9,2) or NUMERIC(11,2) and a schema assertion test fails the build if any money column uses double precision, real or an unscaled numeric type.
- CLM_NBR, CLAIM_PAYMENT_ID business keys are allocated from PostgreSQL SEQUENCE objects producing fixed-length VARCHAR values of the documented widths; no business document key uses an IDENTITY column.
- CLAIM_ADJUSTER_T exposes AUTHORITY_LIMIT as NUMERIC(11,2) NOT NULL and CLAIM_RESERVE_T exposes APPROVED_AMT, PAID_TO_DATE and RESERVE_STATUS with a CHECK constraint restricting status to the evidenced domain including AP and PD.
- A database trigger or revoked-privilege configuration prevents UPDATE and DELETE on CLAIM_RESERVE_HISTORY_T, and an integration test proves an attempted update is rejected.
- Every claims table and PII-bearing column carries a classification tier recorded in a committed classification metadata file, and a build-time check fails if any claims table is unclassified.
- Unit tests: JPA entity mapping tests assert BigDecimal scale 2 round-trips without precision loss for boundary values.
- System integration tests: Testcontainers PostgreSQL test applies all migrations, inserts a full claim-reserve-payment graph and verifies referential integrity and sequence key formats.
- Mock data/fixtures: a seeded claims dataset (adjusters with varying authority limits, reserves in AP and PD status, prior payments) is committed under test resources and loaded by the integration test.

### [P0] Model Claim Approval Lifecycle As First-Class Record

Today a supervisory approval of a claim payment is recorded only as free text in CLAIM_NOTE_T, so there is no machine-readable link between the approval decision and the disbursement it authorises. CLM_Module_Design_Document.md section 6.3 records this approval-to-payment linkage as an unresolved open design item, and enterprise architecture open item 8 states it may require a new APPROVAL_T table. This story implements the approval aggregate in claims-svc: an approval request raised against a specific claim, payment request amount and requesting adjuster; a decision (approve or deny) recorded by a supervisor with a rationale, the approver identity and the authority limit applied; and an explicit lifecycle with terminal states so an approval can be consumed exactly once by a payment. The record is the single artefact the payment authority check queries, and it is what makes BR-02 (approval and disbursement performed by different controls) enforceable and auditable rather than reconstructable from notes. Segregation of duties is enforced in the model: the approver principal may never equal the requesting principal, and approvals expire after a configurable window so stale authorisations cannot be replayed months later.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | epic:claims, domain-model, segregation-of-duties, complexity:medium, phase-2 |

**Acceptance Criteria**
- An approval request can be created for a claim with requested amount, requesting adjuster identity, claim number and reserve reference, and is persisted in APPROVAL_T with status PENDING.
- A decision transition records decision code (approved or denied), rationale text, approver principal identity, approver authority limit at decision time and decision timestamp, and moves status to APPROVED or DENIED.
- An approval whose approver principal equals the requesting principal is rejected with a distinct segregation-of-duties reason code and never persisted in APPROVED state.
- An APPROVED approval can be consumed by exactly one payment: a second consumption attempt fails with a distinct already-consumed reason code, proven by a concurrency test using two parallel transactions.
- An APPROVED approval older than the configured validity window is reported as EXPIRED by the query used for authority evaluation and cannot be consumed.
- Approval state transitions are append-only in an approval history table; no path updates or deletes a historical transition row.
- Unit tests: state machine tests cover every legal and illegal transition including self-approval, double consumption and expiry boundaries.
- System integration tests: an end-to-end test creates a request, records a supervisor decision and consumes it from a payment flow against a Testcontainers PostgreSQL instance.
- Mock data/fixtures: committed fixtures provide pending, approved, denied, expired and already-consumed approvals used by both unit and integration tests.

**Depends on:** WO-100

### [P0] Enforce Dual Payment Authority Check In Claims Domain

The highest-value control gap in PCIS is that CLM006B.cbl computes a payment amount from CLAIM_RESERVE_T rows in status AP and inserts CLAIM_PAYMENT_T with no SECCHK01 call and no reference to CLAIM_ADJUSTER_T.AUTHORITY_LIMIT, despite its prologue claiming it verifies payment authority. This story implements the single reusable enforcement component that both the interactive REST path and the Spring Batch payment job must call before any CLAIM_PAYMENT_T write: check one, a qualifying, unexpired, unconsumed APPROVED approval linked to the payment request exists; check two, the payer authority limit is greater than or equal to PAID_TO_DATE plus the payment amount, evaluated on cumulative payout per BR-01 so splitting one large payment into several small ones cannot circumvent the limit. Both denials produce distinct reason codes, both are audited with actor, resource, operation, approver identity and the authority limit applied, and neither leaks a stack trace. Because the check runs inside the caller transaction, a denial leaves the reserve untouched at status AP and persists no payment row. This converts segregation of duties from documented intent into an executable, testable control measurable by the quarterly control test.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:claims, security, authorization, complexity:high, phase-2 |

**Acceptance Criteria**
- A PaymentAuthorityService evaluates both checks in a single method and returns either a permit decision carrying approver identity and applied authority limit, or a denial carrying exactly one stable reason code.
- Given no qualifying approval, evaluation denies with reason code APPROVAL_REQUIRED, no CLAIM_PAYMENT_T row is written and the reserve remains at status AP.
- Given a qualifying approval but CLAIM_ADJUSTER_T.AUTHORITY_LIMIT less than PAID_TO_DATE plus payment amount, evaluation denies with the distinct reason code AUTHORITY_LIMIT_EXCEEDED.
- Cumulative evaluation is proven by a test where an adjuster with limit 25000.00 has PAID_TO_DATE 20000.00 and requests 10000.00: the request is denied even though 10000.00 alone is within limit.
- Given both checks pass, the approval is consumed, the payment is written with status I, the reserve moves to PD with PAID_TO_DATE equal to APPROVED_AMT and an audit event recording approver identity and applied limit is produced in the same transaction.
- Every denial emits a structured authorization-denied event containing actor, resource, operation and reason code with no unmasked payee or PII values and no stack trace.
- Deny-by-default holds: an evaluation invoked without a resolvable principal (including batch execution) denies with reason code PRINCIPAL_UNRESOLVED rather than defaulting to a literal batch user.
- Unit tests: parameterized tests cover all reason codes and the cumulative boundary at exactly equal to limit (permit) and one cent over (deny).
- System integration tests: the same service is exercised from both the REST payment endpoint and the batch job step, proving one enforcement path serves both callers.
- Mock data/fixtures: committed adjuster, reserve and approval fixtures cover permit, both denial classes and the boundary cases, and the suite runs with no external dependencies.

**Depends on:** WO-100, WO-101

### [P0] Expose Versioned Claims REST API With Deny-By-Default

PCIS has zero API surface: claims capabilities are reachable only through 5250 DDS panels (CLMFNLD1, CLMUPDD1, CLMAPRD1, CLMPAYD1, CLMINQD1) bound to design-only programs CLM001A through CLM005A, and integration between modules happens through in-process CALL and shared tables. This story publishes the claims-svc v1 REST surface that the accessible web UI and other services consume: claim registration and inquiry, reserve set and adjust, approval request and decision, and the payment endpoint that must pass the dual authority check. Every mutating endpoint is deny-by-default with server-side method security, all errors use RFC 9457 problem details with stable reason codes, all input is validated and bound with parameterized persistence, and restricted-tier fields such as payee and customer contact data are masked in responses unless the caller holds an explicit, itself-audited unmask permission. The contract is published as OpenAPI 3.1 generated from code and frozen for v1 so consumers can write contract tests, closing the drift that already exists in the legacy positional CALL interfaces.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:claims, api, security, complexity:high, phase-2 |

**Acceptance Criteria**
- Endpoints exist and are documented in generated OpenAPI 3.1 for claim create and get, reserve set and adjust, approval request and decision, payment create, and claim payment list under the /v1/claims path family.
- Every mutating endpoint denies unauthenticated and unauthorized callers server-side: an integration test proves 401 without a token and 403 with a token lacking the required authority, and no endpoint relies on UI-level gating.
- POST payment returns 201 with the created payment on permit, and 403 with an RFC 9457 problem detail whose reason code distinguishes APPROVAL_REQUIRED from AUTHORITY_LIMIT_EXCEEDED on denial.
- Validation failures return 400 with field-anchored, plain-language messages; unknown claim returns 404; version conflicts return 409; no response body contains a stack trace or SQL text.
- Restricted-tier fields (payee name, customer tax id, phone, email) are masked in all responses by default, and unmasking requires a distinct permission and produces its own audit event.
- All persistence uses parameterized queries or JPA criteria; a static analysis rule fails the build on string-concatenated SQL in claims-svc.
- Approval decision endpoint rejects self-approval with 403 and the segregation-of-duties reason code, driven by the shared domain rule rather than controller logic.
- Unit tests: controller slice tests cover status codes, problem-detail shapes, masking and validation for every endpoint.
- System integration tests: an end-to-end scenario registers a claim, sets a reserve, requests and grants approval, then pays, asserting persisted state and emitted audit events against Testcontainers PostgreSQL.
- Mock data/fixtures: committed request and response JSON fixtures plus a seeded claims dataset let the API test suite run offline with no external identity provider (token issuance stubbed).

**Depends on:** WO-100, WO-101, WO-105

### [P1] Instrument Claims Payment Observability And SoD Evidence

The legacy claims payment path is operationally opaque: failures surface as COBOL DISPLAY lines in a job log, the only durable record is a three-counter RPT_RUN_LOG_T row, there is no metric, trace, alert or dashboard, and a failed audit write leaves an unrecorded money movement with nothing to page on. This story makes the modernized claims payment path observable and auditable to SRE and Compliance standards: structured JSON logs with actor, resource, operation, reason code and correlation id and a masking converter that prevents restricted-tier values from ever reaching the log pipeline; OpenTelemetry traces spanning the API request or batch step through the authority check to the payment write; Micrometer metrics for payments issued, authorization denials by reason code, reinsurance referrals, audit outbox lag and job duration; SLOs with alerting for authorization-denied spikes, audit outbox backlog, batch window overrun and non-zero job exit; and a control-evidence query plus dashboard proving that 100 percent of disbursements have a linked consumed approval. A runbook documents diagnosis and recovery for each alert.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | epic:claims, observability, compliance, complexity:medium, phase-2 |

**Acceptance Criteria**
- All claims-svc and claims batch logs are emitted as structured JSON including actor, resource, operation, reason code, correlation id and job execution id, with no free-text-only error lines.
- A logging masking converter is active such that an automated log scan over the test suite output finds zero unmasked restricted-tier values (tax id, email, phone, payee name); the scan runs in CI and fails the build on any hit.
- OpenTelemetry traces link an incoming payment API request or batch step to the authority evaluation and the payment write, and the trace id appears in the corresponding log lines.
- Micrometer counters and timers exist for payments issued, denials by reason code, reinsurance referrals created, audit outbox pending age and payment job duration, and are scraped successfully by Prometheus in a local compose or test harness.
- Alert rules are defined as code for: any audit outbox record older than the configured threshold, payment job non-zero exit, denial rate exceeding baseline, batch window overrun, and zero payments produced on a night when payable reserves existed.
- A committed control-evidence query returns any CLAIM_PAYMENT_T row lacking a linked consumed approval, and an automated test asserts the query returns zero rows against the seeded post-run dataset.
- A runbook is committed describing symptom, probable cause, diagnostic query and recovery action for each alert, including how to safely reprocess a failed payment item.
- Unit tests: masking converter and structured log field enrichment are unit tested including null and oversized values.
- System integration tests: an end-to-end test asserts that a permitted payment, a denied payment and an induced audit failure each produce the expected log fields, metric increments and trace spans.
- Mock data/fixtures: committed fixtures and an expected-log-fields assertion file allow the observability tests to run offline.

**Depends on:** WO-102, WO-103, WO-105

### [P0] Convert CLM006B Claim Payment Batch To Spring Batch

CLM006B.cbl is the nightly claim payment run: it opens a cursor over CLAIM_RESERVE_T joined to CLAIM_T selecting rows with RESERVE_STATUS equal to AP and APPROVED_AMT greater than PAID_TO_DATE FOR UPDATE OF PAID_TO_DATE, computes the outstanding amount, inserts CLAIM_PAYMENT_T with status I, updates the reserve to PD with PAID_TO_DATE set to APPROVED_AMT, raises an informational reinsurance flag above a hard-coded 100000.00 threshold, calls AUDLOG01 whose non-00 return is only DISPLAYed, commits one claim per cycle, and writes a three-counter RPT_RUN_LOG_T row before ending with no non-zero exit status. This story re-expresses that job as a restartable Spring Batch job with chunk size one to preserve the stated one-claim-per-commit semantics, routes every payment through the shared dual authority check so the batch path is no longer the authority bypass, makes the audit write transactional so an audit failure fails the payment, turns the reinsurance referral into a tracked RECOVERY_T outcome with an owner, externalizes the threshold and batch identity, and exits non-zero with an alert when the error threshold is breached.

| Field | Value |
|---|---|
| Story Points | 13 |
| Hours | 130h |
| Priority | P0 |
| Labels | epic:claims, batch, spring-batch, complexity:high, phase-2 |

**Acceptance Criteria**
- A Spring Batch job reads payable reserves with a single set-based query (status AP, APPROVED_AMT greater than PAID_TO_DATE, joined to claim and adjuster) with no per-row round trips for derived values.
- Chunk size is one item per commit, configurable, so a single failing claim is skipped or recorded as an exception without blocking the remainder of the run, matching the legacy prologue commit scope.
- Every payment write passes through the shared PaymentAuthorityService: reserves without a qualifying approval or exceeding cumulative authority are not paid, produce an exception record with the distinct reason code, and leave the reserve at status AP.
- On permit, the payment insert, reserve update to PD with PAID_TO_DATE equal to APPROVED_AMT, appended reserve history row, approval consumption and audit outbox event all commit in one transaction; a forced audit outbox failure rolls back the entire payment.
- Reinsurance referral above the configured threshold creates a tracked RECOVERY_T row with an assigned owner and status rather than only an informational counter, and the threshold is a configuration property with no compiled-in literal.
- Batch identity is an injected authenticated service principal; the hard-coded BATCHCLM literal is removed and an unresolved principal denies rather than defaults.
- The job writes a run-log row and exits with a non-zero status plus an alert when the error count exceeds its configured threshold, instead of ending silently.
- Restart from the last committed chunk under fault injection produces zero duplicate payments and zero orphaned financial records, proven by an automated fault-injection test.
- Unit tests: reader query, processor decision logic and writer transaction composition are unit tested with BigDecimal boundary values.
- System integration tests: the job runs end to end against Testcontainers PostgreSQL with seeded reserves covering permit, both denial classes, threshold breach and induced failure, asserting final table state and exit status.
- Mock data/fixtures: a committed seeded claims dataset and golden expectation files drive the job tests without external dependencies.

**Depends on:** WO-100, WO-101, WO-105

### [P1] Build Claim Payment Golden-Output Parity Harness

There is no automated test harness anywhere in PCIS, so reserve drawdown and payment arithmetic are verified only by human inspection, and the phased cutover plan requires cent-level reconciliation against the COBOL baseline before Claims goes live. This story delivers the claims parity harness: deterministic seeded fixtures shared by both sides, a capture step that records the COBOL CLM006B baseline outputs (CLAIM_PAYMENT_T rows, CLAIM_RESERVE_T final states, RECOVERY_T referrals and RPT_RUN_LOG_T counters) as committed golden files, a comparison engine that diffs the Spring Batch job outputs against those golden files at NUMERIC(11,2) precision, and a reconciliation report that lists any break with claim number, field, expected and actual values. The harness runs on every commit in CI as the parity gate for the Claims domain and is reused during the minimum 30-day parallel run to produce the nightly reconciliation evidence that the cutover gate requires. Coverage targets are enforced: 100 percent of the claim payment path and 90 percent line coverage on monetary calculation packages.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P1 |
| Labels | epic:claims, testing, parity, complexity:high, phase-2 |

**Acceptance Criteria**
- A deterministic seeded claims dataset is committed and loadable into both a Db2-shaped baseline fixture and PostgreSQL, covering payable reserves, missing approvals, overlimit adjusters, threshold breaches and zero-outstanding reserves.
- Golden output files capture expected CLAIM_PAYMENT_T rows, final CLAIM_RESERVE_T states, RECOVERY_T referrals and run-log counters derived from the CLM006B baseline semantics, and are committed under version control.
- A comparison engine asserts equality of every monetary field at scale 2 with zero tolerance and reports breaks as a structured list of claim number, table, field, expected and actual.
- The harness fails the CI build on any unexplained break and prints a readable reconciliation summary including counts matched, counts broken and break categories.
- Line coverage on the claims monetary calculation packages is at least 90 percent and the build fails below that threshold.
- The harness can run in a nightly parallel-run mode producing a dated reconciliation report artifact suitable as compliance evidence for the domain cutover gate.
- A deliberately introduced arithmetic defect (for example rounding HALF_DOWN instead of exact outstanding computation) is detected by the harness, proven by a mutation-style negative test.
- Unit tests: comparison engine and fixture loader have their own unit tests including empty-result and extra-row cases.
- System integration tests: the harness executes the real Spring Batch payment job against Testcontainers PostgreSQL and compares to golden files with no external dependencies.
- Mock data/fixtures: all fixtures and golden files are committed; the suite runs offline in CI with no access to the IBM i baseline at test time.

**Depends on:** WO-100, WO-103

---

## Billing, Delinquency Aging and Commission Batch Migration (Phase 3)

### [P0] Migrate billing and commission schema with set-based readers

WHAT & WHY: The billing, delinquency and commission batch programs (BIL003B, PRM005B, CMM001B) reach Db2 for i through embedded static SQL cursors plus two to three per-row VALUES round trips for date arithmetic and sequence allocation. Before any Spring Batch job can be written, the PostgreSQL 17 target schema for the billing and commission tables must exist under versioned Flyway migrations with exact decimal semantics, and the per-row round trips must be replaced by single set-based reader queries that emit fully decided candidate rows. This story also resolves the documented BILLING_SCHEDULE_T discrepancy between PCIS_Database_Design.md and the columns actually referenced by the shipped COBOL (BILL_SCHED_ID, POL_NBR, INSTALLMENT_NBR, DUE_DATE, DUE_AMT, PAID_AMT, BILL_STATUS, COMM_CALC_FLAG). IMPACT: New Flyway migration scripts for BILLING_PLAN_T, BILLING_SCHEDULE_T, INVOICE_T, AGENT_COMMISSION_T, COMMISSION_LEDGER_T, POLICY_T read projection and the batch run-log table; new infrastructure DAO classes in billing-svc using JdbcClient/jOOQ; a shared sequence-block allocator component; a data dictionary entry per column with classification tier. WHAT DONE LOOKS LIKE: Flyway migrates a clean PostgreSQL instance in a Testcontainers run; three reader queries return candidate rows with next_due_date, days_out, days_past_due and commission inputs computed entirely in SQL; sequence identifiers are allocated in blocks of 100 with zero per-item round trips; all monetary columns are NUMERIC(9,2)/NUMERIC(11,2) mapped to BigDecimal. SCOPE BOUNDARIES: No Spring Batch job wiring, no REST endpoints, no reconciliation tooling, no changes to claims or policy schemas beyond the read projection needed by these three jobs. DEPENDENCIES: Requires the Phase 0 platform baseline (Maven multi-module build, Testcontainers harness, audit contract v1 table shapes) to already exist; consumed by WO-111, WO-112, WO-113 and WO-114.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | complexity:high, migration, database, billing, phase-3 |

**Acceptance Criteria**
- Flyway migrations create BILLING_PLAN_T, BILLING_SCHEDULE_T, INVOICE_T, AGENT_COMMISSION_T, COMMISSION_LEDGER_T and the batch run-log table on a clean PostgreSQL 17 container, with all monetary columns declared NUMERIC(9,2) or NUMERIC(11,2) and all business document keys as fixed-length VARCHAR/CHAR fed by SEQUENCE objects, never IDENTITY.
- A single billing candidate query returns pol_nbr, prem_annual, bill_freq, installment_cnt, last_installment_nbr, last_due_date, computed next_due_date and computed days_out, with zero additional per-row SQL statements verified by a query-count assertion in the test.
- A single delinquency candidate query returns bill_sched_id, pol_nbr, due_date, due_amt, paid_amt, bill_status and computed days_past_due for installments with status due or late and due_date on or before the run date.
- A single commission candidate query joins paid installments where bill_status is P and comm_calc_flag is null to the policy agent and the in-force agent commission plan using effective/expiry date predicates and a single-row limit, returning a null plan marker rather than dropping the row.
- Sequence block allocator issues identifiers in configurable blocks (default 100), is thread safe, never reuses an allocated value after a restart, and reduces sequence round trips by at least 90 percent in a 1000-item benchmark test.
- Unit tests: DAO row-mapper and allocator logic covered with unit tests, all passing.
- System integration tests: Testcontainers PostgreSQL integration tests validate every reader query and the Flyway migration path end to end.
- Mock data/fixtures: seed SQL fixtures committed covering billing frequencies M, Q, S and an out-of-domain value, paid/unpaid/late installments, agents with and without an in-force commission plan.

**Depends on:** Phase 0 platform baseline: Maven build, Testcontainers harness, audit contract v1

### [P0] Convert BIL003B billing generation to Spring Batch job

WHAT & WHY: BIL003B generates installments and invoices for active policies whose next due date falls within a hard-coded 15-day lead window, commits one policy per cycle, silently skips candidates outside the window, and continues after an AUDLOG01 failure leaving a committed financial mutation with no audit record. Finance cannot sign off on cutover without a modernized job that produces byte-identical installments and invoices while closing the silent-skip and audit-failure gaps. IMPACT: New Spring Batch job configuration, reader/processor/writer components and domain service in billing-svc; externalized lead-day and actor configuration via ConfigurationProperties; transactional outbox writer for audit events; a batch exception table and run-log writer replacing 8000-WRITE-RUN-LOG; Kubernetes Job manifest values. WHAT DONE LOOKS LIKE: Running the job against seeded data produces BILLING_SCHEDULE_T and INVOICE_T rows matching the COBOL baseline on installment number, due date, amount and status for 100 percent of rows; installment amount equals annual premium divided by installment count at BigDecimal scale 2 HALF_UP; the lead window is configurable without redeployment; a skipped candidate produces a visible exception row and metric instead of nothing; an audit-write failure rolls back the installment. SCOPE BOUNDARIES: Does not include the delinquency aging job, the commission job, REST endpoints, parallel-run reconciliation tooling, or invoice document rendering and notification. DEPENDENCIES: Blocked by WO-110 for the schema and the set-based billing candidate reader; consumes the Phase 0 audit outbox contract and the golden-output harness conventions.

| Field | Value |
|---|---|
| Story Points | 13 |
| Hours | 130h |
| Priority | P0 |
| Labels | complexity:high, spring-batch, billing, parity, phase-3 |

**Acceptance Criteria**
- A Spring Batch job named billing-generation runs with the set-based candidate reader from WO-110, a pure-domain processor and a composite writer, committing exactly one policy per chunk so a single failure does not block the remaining population.
- Installment amount equals annual premium divided by installment count computed with BigDecimal divide at scale 2 and HALF_UP, and the generated rows match committed golden output for installment number, due date, amount and status for 100 percent of seeded rows.
- Next due date is computed as last due date plus 1 month for M, 3 months for Q, 6 months for S and 1 year for any other value, asserted for each frequency including the out-of-domain case.
- An installment is generated only when computed days_out is less than or equal to the configured lead window; the lead window default is 15 and is changeable through externalized configuration with no code change or redeploy.
- A candidate outside the lead window produces a batch exception row plus an incremented Micrometer counter and a structured log line with actor, resource and operation, replacing the current silent skip.
- The installment insert, the invoice insert and the audit event insert commit or roll back as one transaction; injecting an audit-write failure leaves zero installment and zero invoice rows and increments an error counter, and the job exits non-zero once the configured error threshold is exceeded.
- The job writes a run-log row containing selected, generated, skipped and error counts, and restarting after a mid-run failure resumes from the last committed chunk with zero duplicate and zero orphaned financial rows.
- Unit tests: installment arithmetic, frequency date arithmetic and lead-window decision logic covered by unit tests on the pure domain component with no infrastructure mocks, all passing.
- System integration tests: Testcontainers-backed end-to-end job run plus a fault-injection restart test validating chunk-level restartability and outbox atomicity.
- Mock data/fixtures: golden-output fixtures and seed data for all billing frequencies, exhausted installment counts and boundary lead-window values committed to the repository.

**Depends on:** Audit outbox contract v1, Externalized configuration service for regulatory tunables

### [P0] Convert PRM005B delinquency aging to Spring Batch job

WHAT & WHY: PRM005B ages due installments nightly: fully paid items become paid, unpaid items beyond a hard-coded 10-day grace period become late and increment a delinquency counter, and everything else stays due. It computes days past due with a per-row SQL round trip, commits per installment, and explicitly documents that an audit-write failure does not roll back the already-committed status change. Collections and cancellation depend on this aging being correct, so the job must be migrated with exact parity while closing the audit atomicity gap and externalizing the grace period. IMPACT: New Spring Batch job configuration, aging domain component and status-transition writer in billing-svc; externalized grace-day configuration; audit outbox participation; delinquency counter maintenance; run-log and exception writers; CronJob manifest and metrics. WHAT DONE LOOKS LIKE: The job reproduces the COBOL status transitions for every seeded scenario, only updates rows whose new status differs from the current status, writes the old and new status into the audit event, and rolls the status change back if the audit event cannot be written. SCOPE BOUNDARIES: Does not include collections workflow, cancellation triggering, payment application, invoice generation or the commission job. DEPENDENCIES: Blocked by WO-110 for the schema and the delinquency candidate reader; shares the audit outbox, configuration and run-log patterns established by WO-111.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | complexity:medium, spring-batch, billing, parity, phase-3 |

**Acceptance Criteria**
- A Spring Batch job named delinquency-aging reads candidates through the WO-110 set-based query with days_past_due computed in SQL, and commits exactly one installment per chunk.
- Status transition logic matches the COBOL exactly: paid_amt greater than or equal to due_amt becomes paid; otherwise days_past_due greater than the configured grace period becomes late and increments the delinquency counter; otherwise the status remains due.
- An update is issued only when the newly computed status differs from the current status, and unchanged installments produce no write, no audit event and no run-log update count, matching legacy behaviour.
- Grace period defaults to 10 and is changeable through externalized configuration without recompilation or redeployment, verified by a test profile overriding the value.
- The status update and its audit event carrying old and new status commit atomically; an injected audit-write failure leaves the original status unchanged and increments the error count, explicitly not reproducing the documented legacy continue-after-failure behaviour.
- The job writes a run-log row with selected, updated, delinquent and error counts and exposes the same values as Micrometer metrics; the job exits non-zero when the error threshold is exceeded.
- Restarting the job after a mid-run failure resumes from the last committed chunk with zero duplicate audit events and zero double-incremented delinquency counters.
- Unit tests: aging decision matrix covered by parameterized unit tests on the pure domain component, all passing.
- System integration tests: Testcontainers job run compared against committed golden output plus fault-injection restart and audit-failure tests.
- Mock data/fixtures: fixtures committed for fully paid, partially paid, unpaid within grace, unpaid exactly at grace, unpaid beyond grace and already-late installments.

**Depends on:** Audit outbox contract v1, Externalized configuration service for regulatory tunables

### [P0] Convert CMM001B commission calculation to Spring Batch job

WHAT & WHY: CMM001B pays agents by selecting paid installments where the commission flag is null, looking up the in-force commission plan, computing commission as paid amount times rate divided by 100 with COBOL ROUNDED semantics, inserting a commission ledger row and stamping the installment flag as Y — the flag being the only guard that makes commissioned-exactly-once true. Agents with no in-force plan merely increment a counter and print a line, and an audit failure leaves a committed ledger row unaudited. The job must migrate with exact monetary parity while making the no-plan case actionable and the audit atomic. IMPACT: New Spring Batch job configuration, commission domain calculator and ledger writer in billing-svc; externalized configuration and batch actor identity; audit outbox participation; exception table for missing commission plans; run-log writer, metrics and CronJob manifest. WHAT DONE LOOKS LIKE: Every paid uncommissioned installment produces exactly one ledger row with an amount matching the COBOL result to the cent, the installment flag flips to Y in the same transaction, agents with no in-force plan produce a visible exception row rather than a console line, and rerunning the job produces zero duplicate ledger entries. SCOPE BOUNDARIES: Does not include agent statement generation, commission payout or clawback, agent commission plan maintenance UI, or 1099 reporting. DEPENDENCIES: Blocked by WO-110 for the schema and commission candidate reader; reuses the outbox, run-log and configuration patterns from WO-111.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | complexity:medium, spring-batch, commission, parity, phase-3 |

**Acceptance Criteria**
- A Spring Batch job named commission-calculation reads paid installments with comm_calc_flag null joined to the policy agent and in-force plan through the WO-110 query, committing exactly one installment per chunk.
- Commission amount equals paid amount multiplied by rate divided by 100 computed with BigDecimal at scale 2 and HALF_UP, matching committed golden output to the cent for every seeded rate including four-decimal rates such as 12.3456.
- The commission ledger insert, the installment comm_calc_flag update to Y and the audit event commit as one transaction; an injected audit failure leaves no ledger row and no flag update.
- Re-running the job immediately after a successful run produces zero additional ledger rows, proving the flag-based idempotency guard is preserved verbatim.
- An installment whose agent has no in-force commission plan produces a batch exception row with agent id, policy number and reason code, an incremented no-plan counter and a structured log line, instead of only a console message.
- Arithmetic overflow that would previously trigger the COBOL ON SIZE ERROR path is detected explicitly and recorded as an item error with a distinct reason code rather than producing a truncated amount.
- The job writes a run-log row with selected, calculated, no-plan and error counts plus total commission, exposes the same values as Micrometer metrics, and exits non-zero when the error threshold is exceeded.
- Unit tests: commission arithmetic and no-plan handling covered by parameterized unit tests on the pure domain calculator, all passing.
- System integration tests: Testcontainers job run compared against golden output, plus rerun-idempotency, restart and audit-failure fault-injection tests.
- Mock data/fixtures: fixtures committed for multiple rates, agents with expired plans, agents with no plan, already-commissioned installments and boundary paid amounts.

**Depends on:** Audit outbox contract v1, Externalized configuration service for regulatory tunables

### [P0] Gate billing batch cutover with parallel-run reconciliation

WHAT & WHY: Functional parity for the billing domain is a contractual cutover gate: reconciled records must match the COBOL baseline to the cent with zero unexplained breaks across a minimum thirty-day parallel run, and every migrated job must restart from the last committed chunk with zero duplicate or orphaned financial records under fault injection. Today there is no harness, no comparison tooling and no operational instrumentation, so parity is asserted by inspection. This story delivers the automated reconciliation harness, the golden-output CI gate for the three billing jobs, the fault-injection suite, and the observability, SLOs, alerting and runbook needed to operate the jobs in production. IMPACT: New reconciliation module comparing Db2 for i extracts against PostgreSQL state; CI pipeline stages running golden-output and fault-injection suites as build gates; Micrometer/OpenTelemetry instrumentation and Grafana dashboard definitions; Prometheus alert rules and SLO definitions; Kubernetes CronJob manifests for the reconciliation job; an operations runbook covering rerun, restart, quarantine and rollback. WHAT DONE LOOKS LIKE: A nightly reconciliation job compares row counts, per-row amounts and checksums for billing schedules, invoices and commission ledger entries, writing a break report with actionable detail; the CI pipeline fails on any golden-output mismatch, any fault-injection duplicate or orphan, or any monetary-package coverage below ninety percent; dashboards show job duration, throughput, exit codes, skipped counts and no-plan rates against defined SLOs. SCOPE BOUNDARIES: Does not include the actual production cutover decision, the polling-based extraction pipeline from IBM i itself, claims or policy domain reconciliation, or the legacy COBOL build automation. DEPENDENCIES: Blocked by WO-111, WO-112 and WO-113 for the jobs under comparison; consumes the Phase 0 CI pipeline, the extraction landing tables and the golden-output fixture conventions.

| Field | Value |
|---|---|
| Story Points | 13 |
| Hours | 130h |
| Priority | P0 |
| Labels | complexity:high, observability, testing, reliability, phase-3 |

**Acceptance Criteria**
- A reconciliation job compares the PostgreSQL billing schedule, invoice and commission ledger state against the Db2 for i baseline extract for a given run date, matching on business key and asserting installment number, due date, amount, status, commission rate and commission amount to the cent at scale 2.
- Reconciliation output is a persisted, queryable break report with one row per break carrying break type, business key, expected value, actual value and a severity, and the job exits non-zero when unexplained breaks exceed the configured threshold of zero.
- The CI pipeline runs golden-output regression for billing generation, delinquency aging and commission calculation on every commit and fails the build on any mismatch in amount, count, status or commit boundary.
- A fault-injection suite aborts each job mid-chunk and restarts it, asserting zero duplicate and zero orphaned billing schedule, invoice, commission ledger and audit rows, and the suite is a required pipeline gate for the batch deployment stage.
- Line coverage on monetary calculation packages is measured and the build fails below ninety percent.
- Each job emits Micrometer metrics for duration, items read, items written, items skipped, error count, no-plan count and exit status; a Grafana dashboard definition and Prometheus alert rules for non-zero exit, error-threshold breach, batch-window overrun and reconciliation break are committed as code.
- SLOs are defined and documented for each job (completion within its batch window with at least twenty-five percent headroom, zero unexplained reconciliation breaks, restart success rate) with the corresponding SLIs wired to the emitted metrics.
- An operations runbook is committed covering how to rerun a failed job, restart from the last committed chunk, quarantine a reconciliation break, roll back a release with helm rollback plus paired migration handling, and escalate an audit-write failure.
- Unit tests: comparison, tolerance and checksum logic covered by unit tests, all passing.
- System integration tests: end-to-end reconciliation run over seeded divergent and identical datasets validating break detection and clean-run behaviour, executed in CI with Testcontainers.
- Mock data/fixtures: baseline extract fixtures, deliberately divergent fixtures and golden-output snapshots for all three jobs committed so the suite runs without access to IBM i.

**Depends on:** Phase 0 CI pipeline and golden-output harness conventions, Polling-based extraction landing tables from Db2 for i

### [P1] Expose billing-svc REST API and batch operations endpoints

WHAT & WHY: PCIS has no HTTP surface at all — billing state is only visible through 5250 panels and integration happens through shared tables. Once the three batch jobs run on the new platform, Finance, Collections, Batch Operations and the new web UI need versioned REST endpoints to read billing schedules, invoices, aging status and commission ledger entries, and to trigger, monitor and inspect batch runs and their exception rows. Every mutating or trigger endpoint must be deny-by-default so no financial operation is reachable without an explicit server-side authorization decision. IMPACT: New controller, application-service and DTO layers in billing-svc; springdoc OpenAPI v1 contract; Spring Security method-level authorization; RFC 9457 problem-detail error handling; PII masking serializers; API gateway route and rate-limit configuration. WHAT DONE LOOKS LIKE: A published OpenAPI 3.1 document describes GET endpoints for billing schedules, invoices, aging summary, commission ledger, batch runs and batch exceptions plus POST endpoints to trigger the three jobs; unauthenticated calls return 401, insufficiently privileged calls return 403 with a reason code, unknown resources return 404, and validation failures return 400 — all as RFC 9457 problem details with no stack traces. SCOPE BOUNDARIES: No payment capture, no invoice document rendering, no notification dispatch, no policy or claims endpoints, and no UI work. DEPENDENCIES: Blocked by WO-110 for the schema and by WO-111, WO-112 and WO-113 for the run and exception data the endpoints expose; consumes the shared authorization service and gateway from the platform epics.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P1 |
| Labels | complexity:high, api, security, billing, phase-3 |

**Acceptance Criteria**
- A versioned OpenAPI 3.1 document is generated from code and published, covering GET billing schedules by policy, GET invoices by policy, GET aging summary, GET commission ledger by agent, GET batch runs, GET batch run exceptions and POST trigger endpoints for the three jobs.
- All endpoints require a validated bearer JWT; unauthenticated requests return 401 and requests lacking the required authority return 403 with a distinct machine-readable reason code, with deny-by-default enforced by method-level authorization on every handler.
- Every error response is an RFC 9457 problem detail containing type, title, status, detail and a correlation identifier, and no response body or log line contains a stack trace, SQL text or unmasked restricted-tier value.
- Monetary fields serialize as exact decimal strings with scale 2 and never as floating point; pagination is applied to all collection endpoints with a bounded maximum page size.
- POST trigger endpoints are idempotent by request key: a duplicate trigger for an already-running job returns 409 with a problem detail rather than starting a second instance.
- Input validation rejects malformed policy numbers, agent identifiers and date ranges with 400 problem details, and all data access uses parameterized queries only.
- Unit tests: controller and application-service logic, authorization annotations and serializer masking covered by unit tests, all passing.
- System integration tests: Spring MVC and Testcontainers integration tests validate each endpoint including 401, 403, 404, 400 and 409 paths, plus an authorization scan asserting no mutating endpoint is unannotated.
- Mock data/fixtures: API fixture data and OpenAPI contract snapshot committed so contract tests run without external dependencies.

**Depends on:** Shared authorization service and JWT validation, API gateway routing and rate limiting

---

## Premium Rating Service and Versioned Contract Governance

### [P0] Freeze versioned v1 premium rating OpenAPI contract

WHAT & WHY: The premium rating engine is the highest fan-in shared component in PCIS — POL001A, POL002A, POL006B, the QTE module and the batch renewal driver all CALL PRMCLC01 — yet two visibly different positional parameter lists already exist in the codebase (POL001A passes policy type, coverage type, territory and limit and receives premium, base rate, factor and return code; POL006B passes policy type, state and old premium and receives premium, return code and underwriting decision). Position is the contract today, so a width change is a silent truncation rather than a compile error. This story produces a single frozen, additive-only OpenAPI 3.1 contract for the premium rating service that is the superset of both call shapes plus the full rating breakdown (risk score, tier, base rate, factors, discounts, surcharges, taxes, final premium, calculation snapshot id) required by the policy issuance UI and by Finance for audit. IMPACT: New api contract directory in the repository holding premium-rating-v1.yaml, a shared data dictionary mapping every COBOL PIC clause and DB2 column to its JSON type, scale and rounding rule, a generated DTO artifact consumed by all rating callers, and documentation cross-referencing PRM_Premium_Calculation_Engine_Design.md sections 1 and 11 and PCIS_Database_Design.md decimal conventions. WHAT DONE LOOKS LIKE: A committed, lint-clean OpenAPI 3.1 document with request and response schemas, enumerated return codes ('00' success, '02' underwriting decline, '99' caller input error), enumerated underwriting decisions, RFC 9457 problem detail error responses, explicit decimal scale annotations, and generated Java DTOs that compile; every field present in either legacy PRMCLC01 parameter list is mappable to a contract field with no loss of width or precision. SCOPE BOUNDARIES: No service implementation, no rating arithmetic, no database migrations, no consumer wiring, no contract test execution — those land in WO-121, WO-122, WO-123 and WO-124. Billing, claims and customer contracts are out of scope. DEPENDENCIES: Reads the evidenced PRMCLC01 call sites in POL001A.cbl and POL006B.cbl and the algorithm order in PRM_Premium_Calculation_Engine_Design.md; is the blocking input for WO-121 and WO-124.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | complexity:medium, premium, api-contract, governance, modernization |

**Acceptance Criteria**
- A file premium-rating-v1.yaml exists in the api contracts directory, is a valid OpenAPI 3.1 document, and passes an OpenAPI linter with zero errors in the build.
- Every input field from both evidenced PRMCLC01 parameter lists (policy type, coverage type, territory, limit, state, old premium) and every output field (premium, base rate, rating factor, return code, underwriting decision) has a corresponding contract field documented in the data dictionary with source COBOL PIC clause, target JSON type, decimal scale and rounding mode.
- The response schema exposes the full breakdown required by the policy issuance UI: composite risk score, risk tier, base rate, ordered factor list, discount list, surcharge list, tax list, final premium, and calculation snapshot identifier.
- Return codes are an explicit enumeration ('00', '02', '99') with documented meanings, and all error responses use RFC 9457 problem detail with a machine-readable reason code and no stack trace or internal identifier leakage.
- All monetary fields declare scale 2 and are documented as mapping to NUMERIC(9,2) or NUMERIC(11,2) and Java BigDecimal; no monetary field is typed as a floating point number.
- Java DTOs are generated from the contract during the Maven build and the generated sources compile with no manual edits.
- Unit tests written and passing: schema validation tests assert that representative valid payloads deserialize and that payloads violating enumerations, required fields or decimal scale are rejected.
- System integration tests: N/A — no runtime service exists at this story; contract wiring is validated in WO-124.
- Mock data and fixtures generated and committed: at least eight sample request and response JSON documents (homeowners, commercial, renewal re-rate, decline, referral, caller input error, maximum-value, minimum-value) are committed under test resources and referenced by the schema validation tests.

### [P0] Scaffold premium-svc service and rating data access layer

WHAT & WHY: There is no Java service, no build manifest, no container definition and no schema migration tooling anywhere in the repository — rating logic exists only as embedded static SQL inside COBOL and as a design document. Before any rating arithmetic can be written or tested, premium-svc needs an operable, observable, deny-by-default Spring Boot skeleton with versioned PostgreSQL migrations for the rating reference tables and a set-based data access layer that replaces per-row EXEC SQL round trips. This is prerequisite plumbing for WO-122, WO-123, WO-124 and WO-125. IMPACT: New Maven module premium-svc with controller, application, domain and infrastructure packages; Flyway migrations creating the rating read tables (RATE_TABLE_T, RATE_FACTOR_T, RISK_SCORE_FACTOR_T, UW_RULE_T, DISCOUNT_RULE_T, SURCHARGE_RULE_T, TAX_TABLE_T) and write tables (PREMIUM_CALC_T, PREMIUM_CALC_DETAIL_T, UW_REFERRAL_T); repositories using JdbcClient or jOOQ for set-based reads; externalized configuration properties for the rating tunables; Spring Security method-level authorization; OpenTelemetry, Micrometer and structured JSON logging with actor, resource and operation context; Dockerfile and Kubernetes manifests. WHAT DONE LOOKS LIKE: A clean checkout builds, starts against a Testcontainers PostgreSQL 17 instance, applies all migrations, serves the WO-120 contract paths as wired-but-unimplemented endpoints returning a documented not-implemented problem detail, exposes health, readiness and metrics endpoints, denies every request without a valid token, and emits one structured JSON log line per request carrying actor, resource and operation. SCOPE BOUNDARIES: No rating arithmetic, no underwriting rule evaluation, no contract tests, no golden-output parity harness, no data migration from DB2 for i, no UI. Reference data seeding beyond minimal fixtures is out of scope. DEPENDENCIES: Requires the frozen contract and generated DTOs from WO-120; blocks WO-122, WO-123, WO-124 and WO-125.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | complexity:medium, premium, scaffolding, platform, modernization |

**Acceptance Criteria**
- A premium-svc Maven module builds from a clean checkout with a single command and produces a runnable Spring Boot 3.5.x application on Java 21 with no manual environment setup steps.
- Flyway migrations create all rating read tables and write tables with monetary columns as NUMERIC(9,2) or NUMERIC(11,2), business document keys as fixed-length VARCHAR or CHAR fed by SEQUENCE objects, child surrogate keys as BIGINT GENERATED ALWAYS AS IDENTITY, and the standard CRT_USER, CRT_TIMESTAMP, UPD_USER, UPD_TIMESTAMP columns on every table.
- All contract paths from WO-120 are wired as controllers that return an RFC 9457 problem detail with reason code PRM_NOT_IMPLEMENTED until WO-122 lands; no endpoint returns a stack trace.
- Deny-by-default is enforced: every mutating and reading endpoint carries a method-level authorization annotation, and an unauthenticated request receives 401 while an authenticated request lacking the rating permission receives 403.
- Rating tunables (rate table cache TTL, referral threshold, decimal scale, maximum coverage lines per request, database statement timeout) are bound through typed configuration properties with validation and are overridable by environment variable without a rebuild.
- Repositories read rate tables, factors and rule tables in set-based queries using parameterized SQL only; a test asserts zero string interpolation of caller input into SQL and that repeated lookups within one rating call issue no per-line round trips.
- Health, readiness and Prometheus metrics endpoints respond, and every request produces one structured JSON log line containing actor, resource, operation and correlation id with no unmasked personal data.
- Unit tests written and passing for configuration property binding and validation, repository row mapping, and the security configuration matrix.
- System integration tests: Spring Boot test with Testcontainers PostgreSQL 17 starts the context, applies all migrations, calls each wired endpoint and asserts the documented status codes and problem detail bodies.
- Mock data and fixtures generated and committed: a seed SQL fixture supplying minimal rate table, factor, rule, discount, surcharge and tax rows so the integration test suite runs with no external dependency.

**Depends on:** WO-120

### [P0] Implement rating engine with exact decimal arithmetic

WHAT & WHY: Premium arithmetic is the executable specification for every quote, issuance, endorsement and renewal in PCIS, and today it exists only as PRMCLC01 design prose plus COMP-3 COMPUTE statements whose rounding behaviour is verified by human reading. The evidenced precedent in CMM001B (COMPUTE ... ROUNDED with ON SIZE ERROR) confirms that COBOL ROUNDED is nearest-away-from-zero, which maps to BigDecimal HALF_UP, and that size overflow must be handled explicitly rather than silently wrapping. This story implements the PRMCLC01 orchestration sequence in the domain layer as pure BigDecimal logic — validate inputs, compute composite risk score and tier, look up base rate and factors, apply discounts, apply surcharges, apply taxes, round to two decimal places at each documented stage — and persists an immutable calculation snapshot so Finance and Actuarial can reconstruct any premium. IMPACT: New domain classes in premium-svc for risk scoring, base rate and factor application, discounts, surcharges, taxes and the orchestrating rating use case; infrastructure writers for PREMIUM_CALC_T and PREMIUM_CALC_DETAIL_T; the WO-120 controller becomes fully implemented; audit event emission joins the same transaction as the snapshot write. WHAT DONE LOOKS LIKE: A POST to the v1 calculations endpoint returns the full breakdown with a persisted calculation id; every intermediate and final amount matches the documented rounding to the cent; an overflow condition produces an explicit, logged, mapped error instead of a wrapped value; the snapshot and its audit event commit or roll back together. SCOPE BOUNDARIES: Underwriting rule evaluation, decline stop behaviour and UW_REFERRAL_T writes are WO-123 and are consumed here through an interface with a permissive default. Consumer contract tests are WO-124 and golden-output parity against the COBOL baseline is WO-125. No UI, no batch job, no data migration. DEPENDENCIES: Requires the frozen contract from WO-120 and the module, migrations and repositories from WO-121.

| Field | Value |
|---|---|
| Story Points | 13 |
| Hours | 130h |
| Priority | P0 |
| Labels | complexity:high, premium, domain-logic, financial-parity, modernization |

**Acceptance Criteria**
- The rating use case executes the documented PRMCLC01 sequence in order: input validation, composite risk score and tier, base rate and factor lookup with base premium equal to base rate multiplied by rating factor, discounts, surcharges, taxes, final premium — and the order is asserted by a test.
- All monetary arithmetic uses BigDecimal with RoundingMode.HALF_UP and rounds to two decimal places at each stage documented in PRM_Premium_Calculation_Engine_Design.md; no double, float or unscaled division appears anywhere in the domain package, enforced by an automated check.
- The domain package contains no framework imports and is unit testable without a database or HTTP layer, enforced by an architecture test.
- An arithmetic overflow or scale violation equivalent to the COBOL ON SIZE ERROR branch raises an explicit domain exception that is logged with actor, resource and operation and mapped to a distinct RFC 9457 reason code; it is never silently truncated or defaulted.
- Each successful calculation persists exactly one PREMIUM_CALC_T header row plus one PREMIUM_CALC_DETAIL_T row per factor, discount, surcharge and tax line, and the response calculationId retrieves the identical breakdown through the snapshot GET endpoint.
- The snapshot write and its audit event are committed in the same transaction; an induced audit-write failure rolls back the calculation snapshot, and a test proves the legacy log-and-continue behaviour is not reproduced.
- Unit tests written and passing: per-stage tests for risk scoring, factor application, discount, surcharge and tax engines, plus rounding tests at half-cent boundaries and negative-value guards, with line coverage of the monetary calculation package at or above 90 percent.
- System integration tests: Testcontainers PostgreSQL 17 tests that POST a rating request over HTTP, assert the response breakdown to the cent, assert the persisted header and detail rows, and assert transactional rollback on induced audit failure.
- Mock data and fixtures generated and committed: seeded rate, factor, discount, surcharge and tax reference rows plus at least twelve request fixtures covering homeowners, commercial, renewal re-rate, zero-discount, maximum-surcharge, multi-coverage, boundary and overflow scenarios.

**Depends on:** WO-120, WO-121

### [P0] Build golden-output parity harness for premium rating

WHAT & WHY: There is no automated test of any kind in the repository, so installment arithmetic, commission rounding and rating factor application are verified by human reading only — meaning the Java rating engine cannot be proven equivalent to the COBOL baseline and no domain can pass its parallel-run cutover gate. Golden-output comparison is the connective tissue that validates the schema migration, the arithmetic rewrite and the service extraction simultaneously. This story builds a deterministic, seeded, CI-executed parity harness for premium rating: a scenario matrix derived from PRM_Premium_Calculation_Engine_Design.md and the evidenced PRMCLC01 call sites, committed golden output files captured from the COBOL baseline, and an automated comparator that asserts every amount matches to the cent at NUMERIC(9,2) and NUMERIC(11,2) precision. IMPACT: New test harness module with deterministic database seeding, a scenario matrix definition, golden output fixtures in a stable machine-readable format, a cent-level comparator that reports per-field deltas, coverage enforcement on the monetary calculation packages, and a Forge Shipping gate that blocks release on any unexplained break. WHAT DONE LOOKS LIKE: A single command runs the full parity suite offline against Testcontainers PostgreSQL 17, produces a per-scenario pass or fail report naming the exact field and delta on any mismatch, enforces at least 90 percent line coverage on monetary calculation code, and fails the pipeline on a single-cent divergence. SCOPE BOUNDARIES: This story covers premium rating parity only — billing generation, delinquency aging, commission calculation, claim payment and audit archive parity harnesses belong to their own epics. No IBM i build automation, no data extraction tooling, no production parallel-run orchestration, no reconciliation dashboard. DEPENDENCIES: Requires the rating engine from WO-122 and the underwriting outcomes from WO-123 for full scenario coverage, plus the module and seed fixtures from WO-121.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | complexity:high, premium, testing, golden-output, financial-parity |

**Acceptance Criteria**
- A scenario matrix is defined and committed covering at minimum: homeowners and commercial policy types, each documented billing and rating path, both evidenced PRMCLC01 caller shapes, discount-only, surcharge-only, tax-inclusive, zero-discount, maximum-factor, half-cent rounding boundary, NUMERIC ceiling overflow, underwriting decline, underwriting referral, and missing reference data.
- Golden output files are committed in a stable machine-readable format (one file per scenario with all breakdown fields as exact decimal strings) and are traceable to the algorithm sections of PRM_Premium_Calculation_Engine_Design.md that define them.
- Database seeding is deterministic and versioned: running the harness twice from a clean state produces byte-identical outputs, and the seed is expressed as versioned migration or fixture scripts rather than ad hoc inserts.
- The comparator asserts equality to the cent for every monetary field including scale, and on mismatch reports scenario name, field path, expected value, actual value and signed delta.
- A single-cent divergence in any scenario fails the suite and fails the pipeline; there is no tolerance band and no ability to mark a break as accepted without an explicit, reviewed exclusion entry.
- Line coverage on the monetary calculation packages is measured and enforced at 90 percent or above in the build, with the report published as a pipeline artifact.
- The suite runs offline in under the agreed build budget with no dependency on IBM i, an external database or network access.
- Unit tests written and passing for the comparator itself, including scale-sensitive comparison (10.0 must not equal 10.00 where scale is asserted), null handling, missing-field detection and extra-field detection.
- System integration tests: the harness drives the real HTTP rating endpoint against Testcontainers PostgreSQL 17 for every scenario, exercising serialization, validation, persistence and snapshot retrieval end to end.
- Mock data and fixtures generated and committed: full reference data seed (rate tables, factors, risk score factors, discount, surcharge, tax and underwriting rule rows) plus per-scenario request fixtures and golden outputs, all committed to the repository.

**Depends on:** WO-121, WO-122, WO-123

### [P1] Add underwriting rule evaluation and referral outcome tracking

WHAT & WHY: The PRMCLC01 orchestration places underwriting rule evaluation before base rate lookup and specifies that a DECLINE outcome returns code 02, writes an audit-only record and produces no PREMIUM_CALC_T row — an underwriting stop, not a priced policy. In the current system this behaviour is design prose only, the UW referral threshold is an unresolved open design item, and a referral is an informational note with no owner or queue. This story implements the PRMUWR01 equivalent as a rule engine over UW_RULE_T, replaces the permissive UnderwritingDecisionPort default introduced in WO-122, and makes REFER a tracked outcome by writing UW_REFERRAL_T rows with a configurable threshold rather than a compiled-in literal. IMPACT: New domain rule-evaluation classes and repository reads over UW_RULE_T; UW_REFERRAL_T writer; orchestrator behaviour change on DECLINE (early return, audit-only, no snapshot); externalized referral threshold configuration; new response fields already reserved in the v1 contract; observability counters per decision. WHAT DONE LOOKS LIKE: A rating request that trips a decline rule returns HTTP 200 with return code 02, an underwriting decision of DECLINE, the matched rule id and reason text, no PREMIUM_CALC_T row and exactly one audit record; a referral request returns an APPROVE-with-referral outcome, a persisted UW_REFERRAL_T row and a full premium breakdown; the threshold is changeable by configuration with no rebuild. SCOPE BOUNDARIES: No underwriter review UI, no referral work-queue service, no notification or email integration, no claims authority checking (that belongs to the Claims epic), no changes to the v1 contract shape beyond fields already specified in WO-120. DEPENDENCIES: Requires the rating pipeline and orchestrator from WO-122, the schema and configuration plumbing from WO-121 and the contract from WO-120.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P1 |
| Labels | complexity:medium, premium, underwriting, business-rules, modernization |

**Acceptance Criteria**
- Underwriting rules are evaluated from UW_RULE_T after risk scoring and before base rate lookup, matching the documented PRMCLC01 step order, and the ordering is asserted by a test.
- A DECLINE outcome returns HTTP 200 with return code 02, underwriting decision DECLINE, the matched rule identifier and reason text, no premium value, and exactly zero rows written to PREMIUM_CALC_T and PREMIUM_CALC_DETAIL_T.
- A DECLINE outcome writes exactly one audit record capturing actor, resource, operation, matched rule and decision, committed in the same transaction as the decision handling.
- A REFER outcome writes exactly one UW_REFERRAL_T row linked to the calculation or quote reference with rule id, threshold applied, reason text and open status, and still returns the full premium breakdown.
- The referral threshold and any rule-evaluation tunables are bound through validated configuration properties, overridable by environment variable with no rebuild and no recompile, and a test asserts a changed threshold changes the outcome.
- Rule evaluation is deterministic: when multiple rules match, the documented precedence (severity then rule sequence) is applied and asserted by a test with a deliberately ambiguous rule set.
- Micrometer counters exist for APPROVE, REFER and DECLINE outcomes and a structured log line with actor, resource, operation and matched rule is emitted for every non-approve decision.
- Unit tests written and passing for rule matching, precedence, threshold boundary behaviour at exactly the configured value, and the no-snapshot-on-decline invariant, with monetary and decision-path coverage at or above 90 percent.
- System integration tests: Testcontainers PostgreSQL 17 tests POST decline, refer and approve scenarios over HTTP and assert response bodies, PREMIUM_CALC_T row counts, UW_REFERRAL_T contents and audit record presence.
- Mock data and fixtures generated and committed: UW_RULE_T seed rows covering decline, refer, approve, overlapping and inactive rules plus matching request fixtures, so tests run with no external dependency.

**Depends on:** WO-120, WO-121, WO-122

### [P1] Enforce rating contract with consumer-driven tests and CI gate

WHAT & WHY: premium-svc is a hub — policy issuance, policy endorsement, batch renewal and quoting all depend on it — so an accidental breaking change to the v1 rating contract would ripple across four consumers with no compile-time warning, exactly the failure mode the current positional CALL convention already exhibits. This story turns contract governance from a documented intention into a build failure: consumer-driven contract tests published by each consumer and verified against the premium-svc provider, an automated breaking-change diff gate against the previously released contract, and a dependency drift check that reconciles each service's declared dependencies against its actual outbound calls, replacing the COBOL prologue CALLS comment convention that nothing verifies today. IMPACT: New contract test module with provider verification in premium-svc and consumer stub tests for the policy, renewal, quote and billing consumers; an OpenAPI diff gate step; a declared-versus-actual dependency drift check; Forge Shipping pipeline stages wiring all of it as blocking gates; contract governance documentation describing the additive-only rules and version bump procedure. WHAT DONE LOOKS LIKE: A pull request that removes a field, narrows a type, tightens a required constraint or changes an enum in the v1 contract fails the pipeline with an actionable message naming the offending field and the affected consumers; a consumer expectation that the provider no longer satisfies fails provider verification; a service that calls premium-svc without declaring the dependency fails the drift check. SCOPE BOUNDARIES: No new rating behaviour, no schema changes, no v2 contract, no production deployment changes, no consumer feature work beyond the minimum stub interactions needed to express expectations. DEPENDENCIES: Requires the frozen contract from WO-120, the running service from WO-121 and WO-122, and the underwriting response fields from WO-123 for full response coverage.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | complexity:medium, premium, contract-testing, ci-cd, governance |

**Acceptance Criteria**
- Consumer-driven contract definitions exist for at least four consumers of the rating contract (policy issuance, policy endorsement or renewal batch, quote, billing) covering approve, refer, decline and caller-input-error interactions.
- Provider verification runs against premium-svc in the build and fails if any consumer expectation is unsatisfied, with the failing consumer and interaction named in the output.
- An OpenAPI diff gate compares the pull request contract against the last released contract and fails the build on any breaking change (field removal, type narrowing, required constraint added, enum value removed, path removal), naming the offending element.
- A dependency drift check reconciles each service's declared outbound dependencies against its actual calls and fails the build when a call to premium-svc is not declared, replacing the unverified COBOL prologue CALLS comment convention.
- Forge Shipping pipeline stages are declared so contract tests, the diff gate and the drift check all run before any container image is pushed, and no push occurs when any of them fails.
- Contract governance documentation records the additive-only evolution rule, the version bump procedure, the deprecation window and the owner of the frozen v1 contract.
- A deliberately breaking contract change on a scratch branch is demonstrated to fail the pipeline, and the failure output is captured in the story evidence.
- Unit tests written and passing for the diff gate configuration and the drift check logic, including a case where a declared dependency exists with no actual call and the inverse.
- System integration tests: provider verification executes against a running premium-svc instance backed by Testcontainers PostgreSQL 17 and validates real HTTP responses rather than mocks of the provider.
- Mock data and fixtures generated and committed: consumer stub payloads and provider state setup fixtures for every contract interaction, committed so the suite runs offline with no external service dependency.

**Depends on:** WO-120, WO-121, WO-122, WO-123

---

## Policy Administration Service and Renewal Batch (Phase 4)

### [P0] Migrate Policy Domain Schema to PostgreSQL with Flyway

The Policy domain owns POLICY_T — the highest fan-in table in PCIS, read by billing (BIL003B), commission (CMM001B), claims (CLM006B) and reporting — plus COVERAGE_T, DEDUCTIBLE_T, POLICY_HISTORY_T, POLICY_PROPERTY_T, POLICY_VEHICLE_T and ENDORSEMENT_T. Today these exist only as Db2 for i definitions described in PCIS_Database_Design.md, with a documented key-strategy conflict: PCIS_Enterprise_Architecture.md section 3.5 lists SEQ_DEDUCT_ID, SEQ_POL_PROP_ID and SEQ_POL_VEH_ID as sequence objects while PCIS_Database_Design.md sections 2.21/2.25 define the same primary keys as BIGINT GENERATED ALWAYS AS IDENTITY. This story creates the versioned, forward-only PostgreSQL 17 schema for the Policy domain as Flyway migrations, resolving that conflict explicitly (business document keys such as POL_NBR remain SEQUENCE-generated fixed-length VARCHAR; pure child surrogate keys use IDENTITY), preserving exact decimal semantics (COMP-3 S9(9)V99 and S9(11)V99 map to NUMERIC(9,2) and NUMERIC(11,2)), and attaching a data-classification tier to every column so downstream masking and retention controls have metadata to act on. Without this schema no policy service, renewal batch or parallel-run reconciliation can be built, and the additive-only evolution guarantee that bounds policy-svc blast radius cannot be enforced.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | complexity:high, database, migration, policy, phase-4 |

**Acceptance Criteria**
- Flyway migrations under the policy-svc module create POLICY_T, COVERAGE_T, DEDUCTIBLE_T, POLICY_HISTORY_T, POLICY_PROPERTY_T, POLICY_VEHICLE_T, ENDORSEMENT_T and BILLING_PLAN_T references with all four standard audit columns (CRT_USER, CRT_TIMESTAMP, UPD_USER, UPD_TIMESTAMP) present on every table.
- POL_NBR is CHAR/VARCHAR(12) populated from a PostgreSQL SEQUENCE via a documented formatter, never IDENTITY; DEDUCT_ID, POL_PROP_ID and POL_VEH_ID are BIGINT GENERATED ALWAYS AS IDENTITY, and the resolution of the design-document conflict is recorded as a comment in the migration and in the module README.
- All monetary columns are NUMERIC(9,2) or NUMERIC(11,2) with a test asserting a round-trip of the boundary values 999999999.99 and 99999999999.99 through JDBC BigDecimal loses no precision and applies no implicit rounding.
- A machine-readable classification manifest (YAML or SQL COMMENT) assigns Public/Internal/Confidential/Restricted to every column of every Policy-domain table, and a build-time check fails the Maven verify phase if any column is unclassified.
- Unit tests: repository-level tests for each entity mapping pass against a Testcontainers PostgreSQL 17 instance, including sequence key allocation and optimistic-lock version column behaviour.
- System integration tests: a Testcontainers-backed test applies every migration from empty database to head, then applies them again to prove idempotency, and asserts the resulting information_schema matches a committed expected-schema snapshot.
- Mock data/fixtures: a seeded dataset of at least 200 policies spanning POL_TYPE HOM/CML/AUT, statuses A/E/C, all four billing frequencies M/Q/S/other, and expiry dates inside and outside the 60-day renewal window is committed as SQL fixtures and loaded by the test harness with no external dependency.

### [P0] Build policy-svc REST API with Versioned Read Contract

POLICY_T is read directly across module lines today: BIL003B joins it for PREM_ANNUAL, CMM001B joins it for AGENT_ID, and CLM006B joins CLAIM_T to it for POL_NBR — database-as-integration-bus with no contract. Because policy is the highest fan-in component and migrates last among transactional domains, its consumers must read a published, versioned projection rather than the physical table. This story stands up policy-svc as a Spring Boot 3.5 service on Java 21 exposing a frozen v1 REST surface: GET /v1/policies/{polNbr} and a paged search projection for consumers, plus POST /v1/policies and PATCH endpoints for policy issuance and endorsement, with deny-by-default authorization via Spring Security 6 method annotations, RFC 9457 problem-detail errors, and audit events written through a transactional outbox in the same transaction as the mutation so an audit-write failure fails the mutation rather than being logged and ignored as in the COBOL baseline. Springdoc-generated OpenAPI 3.1 becomes the contract artifact and consumer-driven contract tests from billing, claims and commission consumers turn any breaking change into a build failure.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | complexity:high, api, spring-boot, policy, security |

**Acceptance Criteria**
- GET /v1/policies/{polNbr} returns the published policy projection including POL_TYPE, CUST_ID, AGT_ID, EFF_DATE, EXP_DATE, POL_STATUS, PREM_ANNUAL as a string-serialized exact decimal, and coverage lines; 404 with an RFC 9457 problem detail is returned for an unknown policy number.
- Every mutating endpoint is annotated with a method-level authorization check and an integration test proves an authenticated principal without the required authority receives 403 with a distinct problem-detail reason code, while an unauthenticated request receives 401.
- Policy creation and endorsement persist the entity change and an audit outbox record in one transaction; an injected outbox-write failure rolls back the policy mutation and the endpoint returns 500 with no partial row, proving the legacy continue-after-audit-failure behaviour is not reproduced.
- OpenAPI 3.1 specification is generated at build time, committed as an artifact, and a CI step fails the build on any non-additive change to the v1 schema (removed field, narrowed type, changed required set).
- Unit tests: domain services covering premium projection, coverage aggregation and status transition rules pass with no infrastructure mocks, using constructor injection for repositories.
- System integration tests: Spring Boot integration tests against Testcontainers PostgreSQL exercise create, read, endorse and unauthorized paths end to end; consumer-driven contract tests for the billing, commission and claims read projections are executed in CI and fail on contract drift.
- Mock data/fixtures: committed JSON request/response fixtures and SQL seed data for at least 20 representative policies allow the full API test suite to run offline with no external service or database dependency.

**Depends on:** WO-130

### [P0] Convert POL006B Renewal Batch to Restartable Spring Batch Job

POL006B.cbl selects active policies expiring within a hard-coded WS-RENEWAL-WINDOW-DAYS of 60, calls PRMCLC01 to re-rate, creates a new term, carries coverages forward, expires the prior term, and writes expiry and renewal history events, committing one policy at a time. Its prologue cites the one-policy-per-commit rule as PCIS batch standard section 7.4 item 6 — but that section is an explicitly unresolved open design item, not a ratified standard, so the commit granularity decision must be made deliberately here rather than inherited. This story re-expresses the job as a restartable Spring Batch 5 job: a set-based reader that computes eligibility and days-to-expiry in SQL rather than per-row round trips, a pure BigDecimal processor calling the versioned premium-svc rating contract, a writer that persists the new term, coverages, deductibles and history rows plus the audit outbox record in one transaction with chunk size one, externalized tunables for the renewal window and rating timeout, and a non-zero exit code plus alert when the error threshold is exceeded instead of ending silently. The job deploys as a Kubernetes CronJob scaling to zero between runs, replacing the JOBSCHD2 SBMJOB driver.

| Field | Value |
|---|---|
| Story Points | 13 |
| Hours | 130h |
| Priority | P0 |
| Labels | complexity:high, spring-batch, policy, renewal, phase-4 |

**Acceptance Criteria**
- A Spring Batch job named policyRenewalJob reads renewal candidates in a single set-based query that computes days-to-expiry and eligibility server-side, with zero per-row date-arithmetic round trips verified by a query-count assertion in the integration test.
- The renewal window (default 60 days), rating call timeout, error-count threshold and chunk size are supplied via externalized configuration properties with validation, and changing the renewal window in configuration alters candidate selection with no code change or redeploy of the image.
- Chunk size is one item per commit by default; a failure on one policy rolls back only that policy, increments a skip/error counter, records a structured exception with actor, resource and operation, and the job continues processing the remaining population.
- Fault-injection test: killing the job mid-run and restarting it resumes from the last committed chunk with zero duplicate new terms, zero orphaned coverage or history rows, and zero policies expired without a successor term.
- A non-success response from the premium-svc rating contract aborts only that policy, is recorded as an actionable exception, and never produces a partially created term; the job exits with a non-zero status when the configured error threshold is exceeded.
- Unit tests: reader query mapping, processor re-rate and term-date arithmetic, and writer transaction composition are unit tested, with the premium rating client behind an interface so domain logic tests need no HTTP stack.
- System integration tests: an end-to-end run against Testcontainers PostgreSQL plus a WireMock premium-svc validates candidate selection, term creation, prior-term expiry, coverage carry-forward, history events, audit outbox rows and run-log counts.
- Mock data/fixtures: committed seed data covering policies inside the window, exactly on the boundary, outside the window, already renewed, cancelled, expired, and one that triggers a rating failure, all loadable offline.

**Depends on:** WO-130, WO-131

### [P0] Golden-Output Parity Harness for Renewal Batch Reconciliation

Renewal is a money-moving process — it re-rates premium and rolls a term forward — and today it has no automated test of any kind, so the Java rewrite cannot be signed off without an executable specification. This story builds the golden-output regression harness for the renewal path: a deterministic seeded dataset, a captured baseline of the COBOL POL006B outputs (new POLICY_T terms, carried-forward COVERAGE_T and DEDUCTIBLE_T rows, POLICY_HISTORY_T expiry and renewal events, RPT_RUN_LOG_T counts and exit status), and a comparator that asserts the Spring Batch job produces cent-identical results at NUMERIC(9,2) precision for every field, row count and status transition. The harness runs on every commit in CI and additionally supports parallel-run mode, reconciling the Db2 for i baseline against PostgreSQL nightly during coexistence and emitting an exception report with row counts, checksums and per-field break detail. This harness is the cutover gate for the Policy domain: zero unexplained breaks over the parallel-run window is the pass condition.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | complexity:high, testing, regression, parity, policy |

**Acceptance Criteria**
- A committed golden dataset and expected-output snapshot exist for the renewal job covering policies inside, on and outside the renewal window, all policy types, rating-failure cases and zero-candidate runs.
- The comparator asserts equality of new term effective/expiration dates, POL_STATUS transitions, PREM_ANNUAL to the cent at NUMERIC(9,2), coverage and deductible carry-forward row counts and values, POLICY_HISTORY_T event types and ordering, and RPT_RUN_LOG_T selected/updated/error counts; any single-field mismatch fails the build with a readable diff.
- The harness runs in CI on every commit against a Testcontainers PostgreSQL instance in under the agreed pipeline budget and publishes a coverage report showing at least 90 percent line coverage on renewal monetary calculation packages.
- Parallel-run mode compares a Db2 for i baseline extract against the PostgreSQL result set by row count, per-column checksum and per-key field diff, and writes a machine-readable exception report; a deliberately injected one-cent premium difference is detected and reported.
- Known parity exceptions (such as the feature-flagged billing origination correction) are declared in a versioned exception register consumed by the comparator, so intended deviations do not mask unintended ones.
- Unit tests: comparator field-mapping, decimal comparison and checksum logic are unit tested including negative, zero and maximum boundary amounts.
- System integration tests: a full harness run executes seed, job, capture, compare and report stages end to end and fails on injected mutations to premium, dates and row counts.
- Mock data/fixtures: all seed SQL, baseline snapshot files and expected exception-register entries are committed so the harness runs offline with no IBM i or production access.

**Depends on:** WO-132

### [P1] Renewal Exception Reporting, Metrics and Alerting Runbook

POL006B surfaces failures only as DISPLAY lines in the job log and aggregate counters in RPT_RUN_LOG_T, and PCIS_Enterprise_Architecture.md section 2.1 shows RPT006A Batch Exception Report is supposed to be consumed by the batch renewal process — a channel POL006B never feeds. Operationally this means a renewal that silently skips a population or aborts on a rating failure is invisible until someone reads a job log. This story makes renewal observable and recoverable: persist per-policy exception records with reason code, actor, resource and operation instead of console text; expose a queryable renewal-exception endpoint and run-log projection for the operations dashboard; emit Micrometer/OpenTelemetry metrics for duration, items read, written, skipped and error count with exit-code labels; define SLIs and alert rules for batch-window overrun, error-threshold breach, audit-outbox backlog and zero-candidate anomalies; and publish a runbook covering restart from the last committed chunk, safe re-run, exception triage and rollback. Structured JSON logs must carry correlation identifiers and must never emit Restricted-tier values.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | complexity:medium, observability, operations, runbook, policy |

**Acceptance Criteria**
- Every renewal failure or intentional skip writes a persisted exception record with policy number, reason code, batch job execution id, correlation id, actor identity, resource and operation, and no free-text-only console output is relied upon.
- A read-only endpoint exposes renewal exceptions and run-log summaries with paging and filtering by run date, reason code and policy number, secured deny-by-default and returning RFC 9457 problem details on error.
- Micrometer metrics are published for job duration, items read, written, skipped and errored, exit code, and premium-svc rating call latency and failure rate; a Grafana dashboard definition and Prometheus alert rules are committed as code.
- Alert rules fire on batch-window overrun beyond the configured budget, error count exceeding the configured threshold, outbox publication backlog exceeding a threshold, and a zero-candidate run when candidates were expected; each alert links to the runbook section that resolves it.
- A committed runbook documents restart-from-last-committed-chunk procedure, safe re-run and idempotency guarantees, exception triage steps by reason code, how to change the renewal window tunable, and the rollback path including Helm rollback and paired down-migration.
- A log-scanning test asserts that no Restricted-tier value (tax id, email, phone, payee) appears in any renewal log line or exception record, using a fixture containing such values.
- Unit tests: exception record construction, reason-code mapping and metric tag derivation are unit tested; N/A is not claimed for any of the three test categories.
- System integration tests: an integration test forces a rating failure, an outbox failure and an error-threshold breach and asserts the corresponding exception records, metric values, non-zero exit code and alert-rule expression evaluation.
- Mock data/fixtures: seeded failure scenarios and expected exception payload fixtures are committed so the observability suite runs offline.

**Depends on:** WO-132

### [P1] Originate Renewal Billing Schedule via Policy Domain Events

Exploration confirmed a real defect: PCIS_Enterprise_Architecture.md section 2.1 states BIL001A Generate Billing Schedule is called internally by POL001A and POL004A, and section 7.2 lists BILLING_SCHEDULE_T as originated by POL001A/POL004A/POL005A — yet POL006B creates a renewed term without originating any billing schedule row. Because BIL003B's candidate cursor inner-joins BILLING_SCHEDULE_T with HAVING MAX(INSTALLMENT_NBR) < INSTALLMENT_CNT, a renewed policy with zero schedule rows can never be selected, so renewed terms are billing-invisible and under-billed. This story makes renewal publish a PolicyRenewed domain event through the transactional outbox so billing-svc originates the first installment and billing plan for the new term asynchronously, decoupling renewal availability from billing availability while guaranteeing at-least-once delivery with idempotent consumption keyed on policy number and term. Because the legacy behaviour is a defect rather than intended business logic, the story includes an explicit parity exception record and a feature flag so parallel-run reconciliation can compare with the defect reproduced or corrected.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | complexity:medium, events, billing, policy, defect-fix |

**Acceptance Criteria**
- A PolicyRenewed event carrying policy number, new term effective and expiration dates, annual premium as an exact decimal, billing frequency and a correlation identifier is written to the outbox in the same transaction as the renewal writes, and published by the relay after commit.
- billing-svc consumes the event idempotently: replaying the same event produces exactly one billing plan and one first installment for the new term, verified by a duplicate-delivery integration test.
- After a renewal run with the correction enabled, every renewed policy is selectable by the billing generation candidate query, proven by an integration test that runs renewal then billing generation and asserts a non-zero installment count for each renewed term.
- A feature flag controls whether the corrected billing origination is active; with the flag off the legacy behaviour (no schedule origination) is reproduced exactly so the parallel-run comparison against POL006B shows zero unexplained breaks, and the deviation is recorded as a documented parity exception.
- Event publication failure after commit is retried with backoff and, on exhaustion, raises an alert and leaves the outbox row in a retryable state; no renewal is lost and no event is dropped silently.
- Unit tests: outbox record construction, event payload serialization with exact decimal fidelity, and idempotency key derivation are unit tested.
- System integration tests: an end-to-end test with Testcontainers PostgreSQL and an embedded broker validates renewal, outbox relay, billing consumption, duplicate delivery and consumer failure retry.
- Mock data/fixtures: committed event payload fixtures and seeded renewal candidates for each billing frequency (M, Q, S and an out-of-domain value) allow the suite to run without external infrastructure.

**Depends on:** WO-132

---

## Reporting, Read Replica and Legacy Decommission (Phase 5)

### [P1] Provision PostgreSQL Read Replica With Lag SLO

WHAT & WHY: Reporting today runs against the same Db2 for i tables that billing and claims are mutating — every batch program writes RPT_RUN_LOG_T in its 8000-WRITE-RUN-LOG paragraph on the same tables BIL003B and CLM006B are updating, and the enterprise architecture lists RPT001A through RPT006A as reporting directly against operational tables. That is a known lock-contention source inside the nightly and monthly batch windows. This story provisions a managed PostgreSQL 17 streaming read replica as infrastructure-as-code, wires a dedicated read-only datasource into the Spring configuration used by reporting workloads, and instruments replication lag as a first-class SLI with alerting. IMPACT: new Terraform module for the replica and its parameter group, monitoring, and secrets; shared Spring Boot datasource configuration used by the reporting service; Prometheus scrape and alert rules; Grafana dashboard; runbook entry for replica failover and lag breach. WHAT DONE LOOKS LIKE: a replica exists per environment, its endpoint and credentials are resolved from the managed secret store, read-only workloads connect to it exclusively, replication lag is visible as a metric with p99 under 30 seconds, and alerts fire at warning 60 seconds and critical 300 seconds. Any attempt to write through the replica datasource fails fast with a structured error rather than silently falling back to the primary. SCOPE BOUNDARIES: report query implementation, report endpoints, extract file generation and masking are out of scope and belong to the reporting service story; no change to OLTP primary schema, backup or PITR posture; no cross-region replica; no Db2 for i changes. DEPENDENCIES: depends on the existing Terraform baseline and PostgreSQL primary provisioned in earlier phases, the managed secret store, and the Prometheus/Grafana observability stack.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | epic:EPIC-14, phase:5, infrastructure, observability, database, complexity:medium |

**Acceptance Criteria**
- A Terraform module creates a PostgreSQL 17 streaming read replica per environment with parameters hot_standby_feedback enabled, statement_timeout and idle_in_transaction_session_timeout set for reporting workloads, and terraform plan is clean and idempotent on a second run.
- Replica endpoint and read-only credentials are read from the managed secret store at runtime; no connection string, host or password literal appears in any committed file or Helm values file (verified by the gitleaks scan step in the pipeline).
- A dedicated read-only datasource bean is registered and any INSERT, UPDATE or DELETE attempted through it is rejected, producing a structured log line with actor, resource and operation context and never being retried against the primary.
- Replication lag is exported as a Prometheus metric; a Grafana dashboard panel shows lag over time; alert rules fire at warning threshold 60 seconds and critical threshold 300 seconds and are documented in the runbook with remediation steps.
- Unit tests: datasource routing and read-only enforcement are covered by unit tests asserting a write attempt on the replica datasource throws and is logged; tests pass in CI.
- System integration tests: a Testcontainers-based test starts a primary and a streaming standby, writes on the primary, asserts the reporting datasource observes the row after replication, and asserts that a write via the reporting datasource fails.
- Mock data/fixtures: a seeded reporting fixture dataset (policies, billing schedule rows, claim reserves, RPT_RUN_LOG_T rows) is committed and loaded by the integration test without external dependencies.
- A runbook section documents replica failover behaviour, the deliberate no-fallback-to-primary policy, and manual promotion steps with expected RTO.

**Depends on:** PostgreSQL primary and Terraform baseline, Managed secret store, Prometheus and Grafana stack

### [P1] Build reporting-svc Replica-Backed Report APIs

WHAT & WHY: The RPT module (RPT001A regulatory statutory extracts, RPT002A management dashboard extracts, RPT003A claims loss triangle, RPT004A renewal and retention, RPT005A billing aging, RPT006A batch exception) exists today only as design-level specifications plus DDS panels RPTMENUD1, RPTPRMD1 and RPTCONFD1, and each batch program writes its own run row into RPT_RUN_LOG_T. There is no API, no masking, no access control and no way to obtain an extract off the terminal. This story implements reporting-svc as a Spring Boot service that serves versioned read-only REST endpoints for the six report families plus batch run visibility, executing set-based queries exclusively against the streaming read replica. WHY: it removes report load from the OLTP primary, closes the deny-by-default access-control gap on reporting data, and guarantees no unmasked restricted-tier value leaves the platform in an extract. IMPACT: new reporting-service module (controller, application, domain, infrastructure layers), jOOQ or JdbcClient query classes, Jackson masking serializer reuse, OpenAPI 3.1 contract, RBAC configuration, async extract job with object-storage handoff, Helm chart and pipeline definition. WHAT DONE LOOKS LIKE: an authenticated caller with the reporting role can request any of the six reports, receive JSON for interactive queries and a 202 with a poll-able job id plus presigned download for large extracts, always served from the replica; restricted fields render masked; unauthorized callers receive 403; every request is logged with actor, resource and operation. SCOPE BOUNDARIES: replica provisioning and datasource wiring are delivered by the read-replica story; the reporting web UI, dashboards and CSV rendering in the browser are the UI epic's concern; no changes to transactional write paths; no new report families beyond RPT001A through RPT006A and the batch run log view. DEPENDENCIES: the provisioned read replica and read-only datasource, the shared masking and classification configuration, the audit service contract, and the API gateway JWT validation.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P1 |
| Labels | epic:EPIC-14, phase:5, reporting, api, security, complexity:high |

**Acceptance Criteria**
- Versioned endpoints exist under /v1/reports for regulatory extracts, management metrics, claims loss triangle, renewal and retention, billing aging and batch exceptions, plus /v1/batch-runs projecting RPT_RUN_LOG_T, all documented in a generated OpenAPI 3.1 contract committed to the repository.
- Every reporting endpoint is deny-by-default: an unauthenticated request returns 401, a request without the reporting or compliance authority returns 403, and both outcomes emit a structured authorization-denied event with actor, resource and operation.
- All reporting queries execute against the read replica datasource; an integration test asserts zero connections are opened against the primary datasource during a full sweep of every endpoint.
- No unmasked restricted-tier field appears in any report payload or generated extract file: tax ID renders as last four characters only and email renders as domain only, verified by an automated scan test over generated fixtures output.
- Large extract requests return 202 with a job identifier, expose GET status transitions, and deliver the artifact via a time-bounded presigned object-storage URL; the synchronous path is bounded by a configured row limit and returns a 413-style problem detail beyond it.
- All error responses use RFC 9457 problem detail with a machine-readable reason code and never include a stack trace, SQL text or SQLCODE value.
- Unit tests: report domain aggregation logic (loss triangle bucketing, aging buckets, retention ratio) is covered by unit tests using BigDecimal assertions at two-decimal scale; coverage of monetary aggregation code is at or above 90 percent.
- System integration tests: Testcontainers-backed tests exercise every endpoint end to end through the security filter chain, asserting status codes, masking, replica-only access and problem-detail shapes.
- Mock data/fixtures: a committed seed dataset covering policies, billing schedules with due, late and paid statuses, claim reserves and payments, commission ledger rows and RPT_RUN_LOG_T rows for all six batch programs is loaded by the test suite with no external dependency.

**Depends on:** WO-140, Shared masking and data classification configuration, API gateway JWT validation

### [P1] Document Storage And Notification Integration Layer

WHAT & WHY: The DOCSVC01 document interface and the NOTIFY01 event and notification interface are drawn in the enterprise architecture but explicitly labelled future — no document storage integration and no event distribution exist. Claims adjusters therefore cannot attach photos or documents at first notice of loss, the CLAIM_DOCUMENT_T and DOCUMENT_T IFS storage contract is an open design item, and no customer or agent notification is generated by any workflow. This story delivers the integration layer: an object-storage-backed document adapter with metadata persistence and presigned retrieval, and an outbox-driven notification adapter that emits domain events to the notification and document vendors with idempotency, retry and dead-letter handling. IMPACT: new document and notification adapter modules, Flyway migrations for document metadata and outbox tables, Kafka topic and consumer configuration, object-storage Terraform, egress allow-list policy, virus-scan hook, Helm charts and pipeline steps. WHAT DONE LOOKS LIKE: a caller can upload a document against a claim or policy, the bytes land in encrypted object storage, metadata is written in the same transaction as the outbox event, retrieval is by short-lived presigned URL only, and notifications are delivered exactly once per business event with failures visible in a dead-letter queue and alerting rather than being silently dropped. SCOPE BOUNDARIES: the browser upload UI, thumbnailing and document viewer are the UI epic's concern; no changes to claim reserve, payment or billing business rules; no email template authoring or print-vendor contract negotiation; payment gateway integration is out of scope and remains tokenized elsewhere. DEPENDENCIES: the transactional outbox pattern and audit service from the shared-kernel epic, Kafka platform availability, and the data classification and masking configuration for attachment metadata.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P1 |
| Labels | epic:EPIC-14, phase:5, integration, documents, notifications, complexity:high |

**Acceptance Criteria**
- A document upload endpoint accepts a file with declared content type and size, rejects types outside a committed allow-list and sizes above the configured maximum with an RFC 9457 problem detail, and stores accepted bytes in object storage with server-side encryption.
- Document metadata and the corresponding outbox event are written inside one database transaction; a forced failure of the object-storage put leaves no metadata row and no outbox event, proven by an integration test.
- Retrieval is exclusively via short-lived presigned URLs; no endpoint streams stored bytes through the service and no permanent public URL is ever generated, verified by test and by absence of any public-read bucket policy in Terraform.
- Notification events are published from the outbox and consumed idempotently keyed on event id; replaying the same event twice results in exactly one delivery attempt recorded, and a permanently failing delivery lands in a dead-letter topic with an alert firing.
- Outbound calls to document and notification vendors are restricted by an egress allow-list; a call to a non-allow-listed host is refused and logged with actor, resource and operation, satisfying the SSRF control.
- Unit tests: adapter logic for content-type validation, size limits, idempotency keys, retry backoff and dead-letter classification is unit tested with a fake object store and fake publisher; tests pass in CI.
- System integration tests: Testcontainers tests with PostgreSQL, Kafka and a MinIO-compatible object store cover upload, transactional outbox publication, consumer idempotency, retry, dead-letter routing and presigned retrieval expiry.
- Mock data/fixtures: committed fixture files (a small image, a PDF and a disallowed executable) and seeded claim and policy rows are used by the tests without any external service or network access.
- Attachment metadata containing restricted-tier values, including payee names on claim documents, is masked before any log line or audit event is emitted, verified by the automated masking scan.

**Depends on:** Transactional outbox and audit service, Kafka platform, Data classification and masking configuration

### [P1] Automated Legacy Decommission Readiness Gate

WHAT & WHY: Decommissioning the IBM i side is currently unfalsifiable — the repository has no inventory, no dependency manifest and no way to prove that every one of the 39 members (8 COBOL programs, 22 DDS display files, 2 CL members and 7 design documents) has a migrated replacement whose parallel run has passed. Prologue CALLS and TABLES comment blocks are the only coupling record and nothing reconciles them against actual behaviour. This story builds an automated decommission readiness gate: a committed inventory manifest mapping every legacy member to its replacement Java artifact and parity evidence, a parser that regenerates the inventory from source so drift fails the build, a traffic-drain checker that proves no legacy program has executed within the configured quiet period, and a machine-readable plus human-readable readiness report published as a pipeline artifact. IMPACT: new tooling module for the inventory parser and gate, a committed decommission manifest, RPT_RUN_LOG_T and batch-run projection queries for drain verification, reconciliation summary aggregation, Forge Shipping pipeline gate step, and a decommission readiness report template. WHAT DONE LOOKS LIKE: running the gate locally or in CI produces a report listing every legacy member, its replacement, its parity and reconciliation status and its last observed execution; the gate exits non-zero if any member is unmapped, any parity gate is unmet, or any legacy program executed inside the quiet window. SCOPE BOUNDARIES: this story does not switch off any scheduler, delete any library, archive any data or obtain sign-offs — those manual production actions belong to the decommission execution story; it does not implement the parallel-run reconciliation engine itself, only consumes its published results. DEPENDENCIES: the reporting service batch-run projection for drain evidence, the parallel-run reconciliation outputs from earlier domain phases, and the document and notification integration layer so no legacy interface remains unreplaced.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | epic:EPIC-14, phase:5, decommission, ci-cd, governance, complexity:medium |

**Acceptance Criteria**
- A committed decommission manifest enumerates every legacy member — the 8 COBOL programs, 22 DDS display files and 2 CL members — with fields for replacement artifact, owning service, parity evidence reference, reconciliation status and decommission state.
- An inventory parser regenerates the legacy member list directly from the repository tree and fails the build if the manifest is missing an entry, contains an entry with no corresponding source member, or leaves any replacement field empty.
- A drain checker queries the batch-run projection and fails when any legacy program name (AUD002B, BIL003B, CMM001B, PRM005B, POL006B, CLM006B) has a run row inside the configured quiet-period window, with the window value externalized as configuration.
- The gate aggregates published parallel-run reconciliation results per domain and fails if any domain reports unexplained breaks or fewer than the configured minimum number of consecutive clean reconciliation days.
- Running the gate produces both a machine-readable JSON report and a human-readable Markdown report, published as a Forge Shipping pipeline artifact with a non-zero exit code on any failed check and a clearly itemized list of blocking reasons.
- Unit tests: manifest parsing, drift detection, quiet-window evaluation and reconciliation aggregation are covered by unit tests including a deliberately drifted manifest fixture that must fail the gate.
- System integration tests: an integration test runs the gate against a seeded database containing batch-run rows and reconciliation results, asserting pass on a clean fixture and fail with the correct reason codes on each of three dirty fixtures.
- Mock data/fixtures: committed fixtures for a clean manifest, a drifted manifest, recent legacy run rows and diverging reconciliation results are used by the tests without external dependencies.
- The gate step is wired into the pipeline as a blocking condition for the decommission release and is documented in a runbook section explaining each failure reason and its remediation.

**Depends on:** WO-141, WO-144, Parallel-run reconciliation outputs

### [P1] Execute Legacy IBM i Decommission And Archive

WHAT & WHY: Once every domain has cut over and the automated readiness gate passes, the IBM i side must be retired in a controlled, auditable, reversible-for-a-window manner. Today promotion and retirement are manual library copies along INSDEV to INSTST to INSPRD with no rollback mechanism other than restoring a saved library, and INSPRDDTA is the only library holding real customer data — so an uncontrolled decommission is both a compliance and a recoverability risk. This story executes the decommission: quiesce the JOBSCHD1, JOBSCHD2 and JOBSCHD3 scheduler entries, place Db2 for i into a read-only posture, take a final verified save of INSPRD, INSPRDDTA, INSCOM and INSTOOLS to WORM object storage under Object Lock with the regulatory retention period, revoke library authorities, tag the final legacy source state in Git, and obtain written sign-off from Compliance, Finance and Batch Operations. IMPACT: production IBM i scheduler and library objects, object-storage retention configuration, access-control revocations, the decommission runbook and evidence pack, and the committed decommission manifest states. WHAT DONE LOOKS LIKE: no legacy batch job can be submitted, no legacy program is reachable by any user profile, a verified immutable archive of all four libraries exists with a documented restore procedure and tested restore drill, the evidence pack is filed, and every manifest entry is marked decommissioned with the date and approver. SCOPE BOUNDARIES: no code changes to any Java service, no new tooling — the automated gate and inventory manifest are delivered by the readiness story; this story does not delete the archive, does not shorten the regulatory retention period, and does not decommission the PostgreSQL platform or any Kubernetes workload. DEPENDENCIES: a passing automated decommission readiness gate, completed reporting and document/notification replacements, and named approvers from Compliance, Finance and Batch Operations.

| Field | Value |
|---|---|
| Story Points | 3 |
| Hours | 30h |
| Priority | P1 |
| Labels | epic:EPIC-14, phase:5, decommission, runbook, compliance, manual, complexity:low |

**Acceptance Criteria**
- All scheduler entries submitting legacy batch work (JOBSCHD1 nightly, JOBSCHD2 nightly renewal, JOBSCHD3 monthly billing and commission) are held or removed, and an attempted submission is proven to fail; evidence is captured in the decommission evidence pack.
- Db2 for i is placed in a documented read-only posture for the legacy libraries and INSPRDDTA write authority is revoked from all application user profiles, with before-and-after authority listings attached as evidence.
- A final save of INSPRD, INSPRDDTA, INSCOM and INSTOOLS is written to object storage with Object Lock enabled and a lifecycle policy matching the agreed regulatory retention (minimum six years for insurance policy records and one year minimum for audit), and the archive integrity is verified by checksum.
- A restore drill is executed from the archive into a non-production environment and its outcome, duration and any deviations are recorded in the runbook, establishing the documented rollback window.
- Every entry in docs/decommission/legacy-inventory.yaml is updated to decommissioned with the date, the approver and a link to the evidence artifact, and the automated readiness gate is re-run to confirm the manifest is internally consistent.
- Written sign-off is recorded from Compliance, Finance and Batch Operations, referencing the readiness report and the archive verification evidence.
- Unit tests: N/A — this story performs manual production operations and documentation updates with no application code changes.
- System integration tests: N/A — verification is by executed runbook steps, negative submission proof and a restore drill rather than automated test suites.
- Mock data/fixtures: N/A — the story operates on real production libraries under change control; no fixtures are produced.

**Depends on:** WO-142, Named approvers from Compliance, Finance and Batch Operations

---

## Accessible Web UI Replacing 5250 DDS Panels

### [P0] Accessible SPA Shell, Routing and API Client Foundation

PCIS today has no web presentation layer at all — every user interaction goes through 22 fixed 24x80 DDS display files (SGNON001.dspf, MENUMD1.dspf, CUSMNTD1.dspf, POLMNTD1.dspf, CLMFNLD1.dspf and others) driven by CL menu dispatchers, which cannot meet the organization's WCAG 2.1 AA keyboard, screen-reader and contrast requirements. This story creates the React 19 + TypeScript single-page application foundation that all subsequent domain screens build on: a workspace under a new web/ directory, an AppShell with skip-links, landmark regions, sidebar module navigation mirroring the PCISMENU module groups, breadcrumbs, theme (light/dark, high-contrast) support, and design tokens for colour, typography, spacing, radius and shadow. It also creates the typed API client that talks to the versioned REST services through the API gateway, including RFC 9457 problem-detail parsing so backend errors surface as plain-language, field-anchored messages instead of terse coded messages like CUS0099. Without this shell every domain story would reinvent layout, error handling and fetch plumbing, and accessibility would be retrofitted rather than designed in. The shell is deliberately domain-agnostic: it renders navigation and route outlets only, with no business logic.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:web-ui, frontend, react, accessibility, foundation, complexity:high |

**Acceptance Criteria**
- A new web/ workspace exists with React 19, TypeScript in strict mode, Vite build, ESLint and Prettier, and npm scripts for dev, build, lint, test and typecheck; build completes with zero TypeScript errors and no use of the any type.
- AppShell renders semantic landmarks (header, nav, main, footer), a working skip-to-main-content link as the first focusable element, an accessible sidebar with module groups (Customer, Policy, Premium, Billing, Claims, Reporting, Admin), TopBar with breadcrumbs and a theme toggle persisted to localStorage.
- Client-side routing is configured with a route registry, lazy-loaded route modules, a 404 route, an error boundary that renders a recoverable error panel, and route changes announce the new page title to assistive technology via a live region.
- A typed API client wraps fetch with base URL from environment configuration, correlation-id header propagation, timeout, retry on idempotent GET only, and parses RFC 9457 problem+json into a typed ProblemDetail object exposing title, detail, status, reason code and per-field errors.
- Design tokens are defined once as CSS custom properties plus a TypeScript token module; all token colour pairs used for text meet a 4.5:1 contrast ratio (3:1 for large text) verified by an automated token contrast test.
- Unit tests: component tests cover AppShell landmarks, skip-link focus behaviour, theme toggle persistence, route error boundary and API client problem-detail parsing including network failure and timeout paths; all tests pass in CI.
- System integration tests: a smoke test runs the built SPA against a mock API server (MSW) verifying navigation across all registered module routes and rendering of an error state when the API returns a 500 problem-detail response.
- Mock data and fixtures: MSW handlers and JSON fixtures for the health endpoint and a sample problem-detail error response are committed under web/src/test/fixtures so the suite runs with no external dependency.

### [P0] Customer Workspace Replacing CUS Green-Screen Panels

Customer service representatives today work across four separate DDS panels — CUSMNTD1 (add/maintain), CUSINQD1 (inquiry), CUSLSTD1 (search list) and CUSDELD1 (delete) — navigating with function keys documented in CUS_Module_Design_Document.md section 7, where duplicate tax-ID detection is a soft warning that is easy to click past and billing, policy and claim history live behind separate panels. This story delivers a single accessible customer workspace: search and results list, a single customer view spanning identity, addresses, contacts, policies, billing and claim history, create and maintain forms with plain-language field-anchored validation, an explicit duplicate-resolution step that blocks silent creation of a second customer with the same tax ID, and a deactivate flow replacing CUSDELD1. Tax ID, date of birth, email and phone are rendered masked by default using the MaskedValue component, with unmask available only to principals holding the permission and always audited server-side. This is the thin end-to-end slice named in the risk register, so it also proves the shell, auth, component library and API client together before the higher-risk domains follow.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:web-ui, frontend, customer, pii, complexity:high |

**Acceptance Criteria**
- A customer search screen replaces CUSLSTD1 with accessible filters (name, tax ID last four, customer id, status), paginated results in the shared DataTable, keyboard row activation opening the customer view, and an accessible no-results state.
- A single customer view replaces CUSINQD1 and aggregates identity, addresses, contacts, policies, billing summary and claim history in tabbed regions, each loading independently with its own skeleton and error state so one failing panel does not blank the page.
- Create and maintain forms replace CUSMNTD1 with all evidenced fields (customer type, name, date of birth, tax ID, gender, marital status, status, credit score, primary address, primary contact) and enforce the evidenced mandatory-field rules with field-anchored plain-language messages instead of coded messages.
- Duplicate tax-ID detection is an explicit blocking resolution step: on a duplicate the user is shown the matching existing customer and must either open that record or explicitly record a justified override where permitted; the create submit cannot proceed by simply pressing Enter again.
- Tax ID, date of birth, email and phone display masked by default (tax ID last four only, email domain only, phone last four only); the unmask action appears only for principals with the unmask capability, calls the server unmask endpoint, and the UI states that the action is audited.
- Deactivate replaces CUSDELD1 with a confirmation dialog naming the customer and the consequence, gated by the corresponding capability, and rendering the server 403 reason code verbatim when denied.
- Unit tests: form validation rules, duplicate-resolution state machine, masking presentation, tab-panel loading and error isolation, and search filter serialisation are covered and passing.
- System integration tests: end-to-end flows against MSW-backed customer-svc endpoints validate search, view aggregation, create with duplicate block, create success, maintain with server-side validation errors, unmask permitted and denied paths, and deactivate.
- Mock data and fixtures: customer, address, contact, policy summary, billing summary and claim summary fixtures plus duplicate-detection and validation-error problem-detail payloads are committed so the suite runs offline.

**Depends on:** WO-150, WO-151, WO-154

### [P1] Billing Invoice, Payment and Delinquency Screens

Billing interaction today runs through BILINVD1 (invoice), BILPMTD1 (payment) and BILINQD1 (inquiry) panels, while the money logic lives in BIL003B.cbl (installment and invoice generation within a 15-day lead window) and PRM005B.cbl (delinquency aging against a 10-day grace period). Two problems are visible in the code: candidates outside the lead window are silently skipped with no exception row and no counter even though WS-CNT-ELIGIBLE counts them as eligible, and an installment status change commits even when the AUDLOG01 write fails. Finance, Collections and CSR users therefore have no screen that shows why an expected installment was not generated or which items are aging. This story delivers the accessible billing workspace: a policy billing schedule and invoice view with installment status history, an aging and delinquency worklist with grace-period context, an accessible payment application screen replacing BILPMTD1, and a skipped-candidate exception list that makes previously silent outcomes visible and actionable. Payment capture remains tokenized through the third-party gateway, so the UI only ever handles tokens and last-four values.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P1 |
| Labels | epic:web-ui, frontend, billing, finance, complexity:high |

**Acceptance Criteria**
- A billing schedule and invoice view replaces BILINVD1 and BILINQD1, listing installments with installment number, due date, amount, status and paid amount, plus per-installment status history, using decimal-safe money display that matches DECIMAL(9,2) values exactly.
- An aging and delinquency worklist lists installments that are due or late with days past due, grace-period remaining, delinquency counter and policy context, filterable and sortable, with accessible empty and large-result states.
- A payment application screen replaces BILPMTD1, allowing a payment to be applied to one or more installments with a running unapplied balance, blocking over-application with a field-anchored message, and handling only gateway tokens and last-four card values — never raw cardholder data.
- A skipped-candidate exception list shows policies that were eligible but not generated (for example due date outside the configured lead window) with the reason and the configured window value, turning the previously silent BIL003B skip into a visible, actionable item.
- Configured tunables that affect what the user sees (billing lead days, grace days) are displayed as read-only context from the configuration API so users understand why an item is or is not present, without the UI hard-coding 15 or 10.
- All screens are fully keyboard operable with logical focus order, and every status value renders both a colour badge and a text label so status is never conveyed by colour alone.
- Unit tests: money formatting and totals presentation, days-past-due and grace-remaining derivation from server data, over-application validation, and filter/sort serialisation are covered and passing.
- System integration tests: MSW-backed flows validate schedule view, aging worklist filtering, payment application success and over-application rejection, exception list rendering, and a 5xx failure on one panel not blanking the page.
- Mock data and fixtures: billing schedule, invoice, installment status history, aging worklist, payment, skipped-candidate exception and configuration tunable fixtures are committed so tests run offline.

**Depends on:** WO-150, WO-151, WO-154

### [P0] Policy Issuance Screen With Premium Rating Breakdown

Policy work today runs through POLMNTD1 in CREATE, ENDORSE and INQUIRY modes plus POLINQD1, POLSCHD1, POLRENDX/POLREND1 and POLCAND1, with a scrollable coverage subfile using a numeric Opt column and an annual premium field that is display-only because it is computed by PRMCLC01. POL_Module_Design_Document.md section 5.1 shows the panel, and PRM_Premium_Calculation_Engine_Design.md section 11 shows that PRMCLC01 already produces a risk score, risk tier, base rate, factors, discounts, surcharges, taxes and an underwriting decision that the green screen has no room to display — so underwriters cannot see why a premium is what it is, and a Decline outcome is not surfaced as a stop. This story delivers the accessible policy workspace: mode-aware header, customer and agent lookups, term dates, an accessible coverage editor replacing the subfile with mandatory-coverage locking, and a rating panel showing the full premium breakdown with an explicit underwriting outcome banner and a persisted calculation snapshot id for audit. Read-only inquiry, schedule view, renewal preview and cancellation flows reuse the same components in protected mode.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:web-ui, frontend, policy, premium, complexity:high |

**Acceptance Criteria**
- A mode-aware policy workspace supports CREATE, ENDORSE and INQUIRY, applying the field-level protected versus input-capable behaviour documented in POL_Module_Design_Document.md section 5.1, with mode clearly announced to assistive technology.
- Customer and agent lookup components validate existence and active status through the API and render inactive or missing entities as blocking field-anchored errors rather than generic messages.
- The coverage editor replaces the POLMNTD1 subfile with an accessible table supporting add, change and remove in ENDORSE mode only, limit and deductible entry with decimal-safe money fields, and mandatory coverages rendered locked with an accessible explanation that they cannot be removed.
- A rating panel displays the full premium breakdown returned by the rating contract — composite risk score, risk tier, base rate, ordered factors, discounts, surcharges, taxes and final annual premium — in a readable table with a persisted calculation snapshot id shown for audit.
- An underwriting outcome banner renders Approve, Refer and Decline distinctly; a Decline blocks the issue action entirely with the returned reason text, and a Refer records the referral outcome and shows the pending state instead of silently continuing.
- Annual premium is never directly editable in any mode; it is displayed only and recomputed when coverage, deductible or term inputs change, with a clear stale-rating indicator until re-rated.
- Inquiry, billing schedule view, renewal preview and cancellation screens reuse the same components in protected mode, with cancellation requiring a reason from the reference-data domain and a confirmation dialog.
- Unit tests: mode-based field protection matrix, mandatory-coverage locking, stale-rating detection, money precision handling and underwriting outcome gating are covered and passing.
- System integration tests: MSW-backed flows validate create-and-rate-and-issue, endorse with re-rate, Decline blocking issue, Refer pending state, rating service failure surfacing an actionable error, and inquiry read-only rendering.
- Mock data and fixtures: policy, coverage type, deductible, property, customer and agent fixtures plus rating responses for Approve, Refer, Decline and rating-service-error cases are committed for offline runs.

**Depends on:** WO-150, WO-151, WO-154

### [P0] OIDC Sign-In and Role-Based UI Gating

Authentication in PCIS today is the 5250 sign-on panel SGNON001.dspf establishing an IBM i job user profile, with interactive programs recovering identity via SET :HV-CURRENT-USER = CURRENT USER and falling back to the literal PCISBATCH when SQLCODE is non-zero. Authorization is menu-option filtering through ROLE_MENU_T at the CL driver layer — presentation-layer authorization that OWASP A01 forbids relying on, and the reason SECCHK01 is declared in PCIS_Enterprise_Architecture.md but never actually called by any of the eight shipped COBOL members. This story replaces the terminal sign-on with an OIDC Authorization Code plus PKCE flow against the central identity provider, stores the session in an httpOnly, Secure, SameSite=Strict cookie (never localStorage), handles silent refresh and rotation, and derives UI capability gating from the verified token claims. Critically, UI gating is treated as convenience only: every mutating action still calls a server endpoint that performs the authoritative deny-by-default check, and the UI must render the server's denial reason code rather than pre-empting it. This closes the gap where function-key visibility (POL003A F6, CLM005A F7/F8, CUS003A F6/F9) was the only control.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | epic:web-ui, frontend, security, authn, authz, complexity:medium |

**Acceptance Criteria**
- An unauthenticated user visiting any protected route is redirected to the identity provider using Authorization Code with PKCE, and after successful sign-in is returned to the originally requested route.
- The session is carried in an httpOnly, Secure, SameSite=Strict cookie plus a bearer token attached by the API client; no access or refresh token is ever written to localStorage or sessionStorage, verified by an automated test asserting storage is empty after sign-in.
- Access token expiry triggers a silent refresh; a failed refresh clears client state, redirects to sign-in and preserves the intended route. Concurrent 401 responses trigger exactly one refresh attempt (single-flight), not one per request.
- A useCapabilities hook exposes role and permission claims from the validated token; navigation items and action buttons the principal lacks permission for are hidden or rendered disabled with an accessible explanation, while the underlying route still returns the server's 403 problem detail if reached directly by URL.
- A 403 response from any mutating endpoint renders the server-supplied reason code and message (for example missing-approval versus authority-limit-exceeded) without leaking a stack trace, and the denial is visible to keyboard and screen-reader users via an alert region.
- Sign-out clears the cookie, revokes the refresh token at the identity provider and returns the user to the sign-in page with no cached protected data in memory or in the query cache.
- Unit tests: PKCE code challenge generation, callback handling with state and nonce validation, single-flight refresh, capability gating logic and sign-out cache purge are covered and passing.
- System integration tests: an end-to-end flow against a mock OIDC provider and mock API validates sign-in, protected route access, token refresh on 401, direct-URL access to an unauthorized route returning a rendered 403 panel, and sign-out.
- Mock data and fixtures: a mock OIDC discovery document, JWKS, signed test JWTs for adjuster, supervisor, CSR and admin roles, and MSW handlers for 401 and 403 responses are committed for offline test execution.

**Depends on:** WO-150

### [P1] WCAG 2.1 AA Conformance Test Harness and CI Gate

WCAG 2.1 AA keyboard, screen-reader and contrast conformance is an organization policy requirement that the 5250 green-screen panels can never satisfy, and it is the stated justification for replacing all 22 DDS display files. Without automated enforcement, accessibility regressions will creep back in exactly as the audit and authorization controls did in the COBOL baseline — declared in documents but unenforced in code. This story builds the accessibility conformance harness and makes it a blocking pipeline gate: axe-core assertions across every route and every component gallery state, automated keyboard-only traversal tests proving no focus trap and no unreachable control, contrast auditing over the design tokens and rendered pages, landmark and heading-order checks, and reduced-motion and forced-colors rendering checks. It also produces a machine-readable conformance report per build that Compliance can retain as evidence, plus a documented manual screen-reader verification checklist for the flows that automation cannot fully assert. The gate fails the build on any new violation rather than warning, so conformance is a measured fact rather than an intention.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | epic:web-ui, accessibility, testing, ci-cd, compliance, complexity:medium |

**Acceptance Criteria**
- Automated axe-core accessibility assertions run against every registered SPA route and every component gallery state, with zero serious or critical violations; the run fails the build on any new violation and publishes a machine-readable JSON report artifact per build.
- Keyboard-only traversal tests verify for each primary workflow (customer create, policy issuance, claim FNOL, payment request, approval decision, payment application) that every interactive control is reachable by Tab, that no focus trap exists outside modals, that Escape closes dialogs and returns focus to the invoker, and that a visible focus indicator is present on every focusable element.
- A contrast audit covers both design tokens and rendered pages, asserting 4.5:1 for normal text, 3:1 for large text and non-text UI components, and fails with the offending selector and computed ratio.
- Structural checks assert a single main landmark per page, a correct heading hierarchy with no skipped levels, an accessible name on every landmark region, a working skip link, and an accurate document title per route.
- Rendering checks verify usable output under prefers-reduced-motion, forced-colors/high-contrast mode, 200 percent browser zoom and a 320 CSS pixel viewport width without loss of content or functionality.
- Status, error and validation information is asserted to be conveyed by text in addition to colour or icon, and live-region announcements are verified for async operations (submit success, validation failure, upload progress, authorization denial).
- A conformance gate is added to the CI pipeline configuration that blocks merge on failure, and a documented manual screen-reader verification checklist (NVDA and VoiceOver) for the six primary workflows is committed alongside the automated suite.
- Unit tests: N/A — this story is itself a test harness; instead, harness self-tests assert that a deliberately introduced violation fixture is detected and fails the run.
- System integration tests: the full harness executes against the built SPA served with MSW-backed mock APIs in CI and completes within the agreed pipeline time budget, producing the conformance report artifact.
- Mock data and fixtures: deterministic MSW fixtures for every audited route (including empty, populated, error and denied states) plus a known-bad fixture page used to prove the gate fails are committed.

**Depends on:** WO-150, WO-151, WO-152, WO-153, WO-154, WO-155, WO-156

### [P0] Claims Workspace With Approval and Payment Controls

Claims is the highest-control-risk area and today spans five DDS panels — CLMFNLD1 (FNOL), CLMUPDD1 (adjuster update), CLMAPRD1 (approval), CLMPAYD1 (payment) and CLMINQD1 (inquiry) — with CLM001A through CLM005A existing only as design specifications. The evidenced control gaps are severe: approval is recorded as a free-text CLAIM_NOTE_T entry with no machine-readable link to the payment it authorises, authority rejection is only discovered after re-entering the whole payment, there is no approval work queue so approvals are found by asking, and adjusters have no way to attach loss photos or documents at intake. This story delivers the accessible claims workspace: FNOL intake with document and photo attachment, a claim workspace combining reserve history, payments, notes and documents in one view, a payment request screen that shows the adjuster's remaining authority headroom on cumulative payout before submission and renders the server's distinct denial reason codes for missing approval versus authority exceeded, and a supervisor approval queue where decisions create first-class linked approval records. Segregation of duties is preserved in the UI by never offering approve and disburse to the same principal in one action.

| Field | Value |
|---|---|
| Story Points | 13 |
| Hours | 130h |
| Priority | P0 |
| Labels | epic:web-ui, frontend, claims, security, segregation-of-duties, complexity:high |

**Acceptance Criteria**
- An FNOL intake screen replaces CLMFNLD1 with policy lookup and coverage validation, loss date and description, late-reporting indication, and multi-file document and photo attachment with progress, retry, type and size validation and accessible status announcements.
- A claim workspace replaces CLMUPDD1 and CLMINQD1 by combining claim header, append-only reserve history, payment list, notes and documents in one keyboard-navigable view where reserve history rows are visibly non-editable.
- The payment request screen shows, before submission, the remaining reserve (approved amount minus paid to date) and the requesting adjuster's remaining authority headroom computed on cumulative claim payout, so an over-limit request is visible in advance rather than after submission.
- Submitting a payment renders the server's distinct denial reason codes verbatim and differently: missing linked approval, authority limit exceeded on cumulative payout, and insufficient remaining reserve each produce a distinct, accessible message with the escalation path.
- A supervisor approval queue lists pending payment and reserve approval requests with claim context, and an approve or deny decision requires a written rationale and creates a first-class approval record linked to the specific request, replacing the free-text note pattern.
- The UI never offers an approve-and-pay combined action, and a principal who approved a request does not see a disburse action for that same request, preserving segregation of duties as a visible design property in addition to the server control.
- Payments above the configured reinsurance referral threshold surface a tracked referral outcome with an owner rather than an informational-only note.
- Unit tests: authority headroom and remaining reserve computation, denial reason code rendering, reserve history immutability presentation, attachment validation and approval decision form rules are covered and passing.
- System integration tests: MSW-backed flows validate FNOL creation with attachments, reserve view, payment request denied for missing approval, denied for authority exceeded, approval queue decision creating a linked approval, subsequent payment success, and reinsurance referral surfacing.
- Mock data and fixtures: claim, reserve history, payment, note, document, adjuster authority-limit and approval fixtures plus 403 problem-detail payloads for each distinct reason code are committed for offline test runs.

**Depends on:** WO-150, WO-151, WO-154

### [P0] Accessible Component Library Replacing DDS Subfiles

The 22 DDS panels rely on presentation idioms that have no accessible web equivalent out of the box: scrollable subfiles with an Opt column (POLMNTD1 coverage lines, CUSLSTD1 customer list), function-key command bars (F3 Exit, F5 Refresh, F6 Endorse, F9 History, F12 Cancel), a single message line at the bottom of the panel for all errors, and render-time masking such as the tax ID shown as asterisks beyond the last four digits in CUS_Module_Design_Document.md section 5.2. This story builds the shared, WCAG 2.1 AA component library that every domain screen composes: an accessible DataTable that replaces subfiles with proper table semantics, sortable headers, row selection and keyboard row actions; typed form controls with field-anchored validation messages replacing the terse coded single message line; a StickyActionBar that exposes former function keys as labelled buttons with documented keyboard shortcuts; Modal, Tabs, Accordion, Badge and AlertStack primitives with correct focus management and roles; and a MaskedValue component that renders masked PII by default with a permission-gated, itself-audited unmask action. Building these once prevents each domain screen from re-implementing accessibility and guarantees consistent behaviour for keyboard and screen-reader users.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | epic:web-ui, frontend, design-system, accessibility, complexity:medium |

**Acceptance Criteria**
- DataTable renders native table semantics with caption, column headers with scope, aria-sort on sortable columns, sticky header, dense mode, row-level action menu reachable by keyboard, full keyboard row navigation, and an accessible empty state and loading state.
- Form controls (TextField, Select, Checkbox, Radio, Toggle, DateField, MoneyField) each associate a visible label, optional description and error message via aria-describedby and aria-invalid; validation errors render adjacent to the offending field and are announced once via a live region, replacing the DDS bottom message line pattern.
- MoneyField and money display components format and parse decimal values without floating-point loss, preserving two-decimal scale so amounts round-trip exactly to the DECIMAL(9,2) and DECIMAL(11,2) backend types.
- StickyActionBar exposes primary, secondary, ghost and danger actions with loading and disabled states, documents keyboard shortcuts equivalent to the legacy function keys, and shortcuts are discoverable through an accessible keyboard-shortcut help dialog.
- Modal traps focus, restores focus to the invoking element on close, closes on Escape, has role dialog with aria-modal and an accessible name; Tabs implement the ARIA tabs pattern with arrow-key navigation; Accordion uses button headers with aria-expanded.
- MaskedValue renders restricted-tier values masked by default (tax ID as last four only, email as domain only, phone as last four only), shows an unmask action solely when the principal holds the unmask capability, and calls the server unmask endpoint rather than receiving clear values in the initial payload.
- Unit tests: every component has Testing Library tests covering keyboard interaction, ARIA attributes, error association, empty and loading states, and money formatting boundary values; all passing.
- System integration tests: a composed harness page uses DataTable, form controls, Modal and StickyActionBar together against MSW-backed endpoints, validating submit-with-errors, error focus management and successful submit flows.
- Mock data and fixtures: fixture datasets for table rows (including empty, single-row and 500-row cases), validation error problem-detail payloads and masked/unmasked field samples are committed under web/src/test/fixtures.

**Depends on:** WO-150, WO-151

---

## API Gateway, Identity Federation and Edge Security

### [P0] Deploy API Gateway With TLS, Routing and Rate Limiting

PCIS today has zero HTTP surface: all interaction crosses the 5250 protocol into ILE COBOL programs, and integration happens by in-process CALL or shared Db2 tables, so there is no single place where an untrusted caller becomes an authenticated principal. This story builds that place: a Spring Cloud Gateway edge service, deployed on Kubernetes via Helm and fronted by IaC-provisioned load balancer and TLS termination, that routes to the eight target services (claims, customer, policy, premium, billing, reporting, authz, audit) under versioned /v1 paths. The gateway is the operational chokepoint for TLS 1.3, security headers, per-principal and per-IP rate limiting, request size limits, correlation-ID minting and propagation, and RFC 9457 problem-detail error normalisation, so that no service has to reimplement edge concerns and every request is observable end to end. Business reason: deny-by-default access control, rate limiting and structured error handling are organization policy requirements that cannot be measured today because there is no boundary to measure at, and every subsequent domain migration depends on a stable, observable edge to route traffic through during phased cutover. Scope here is the transport, routing and reliability plane only; token validation and identity federation land in WO-161.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:api-gateway, platform, edge-security, infrastructure, complexity:high |

**Acceptance Criteria**
- A gateway module (Spring Boot 3.5.x, Java 21, Spring Cloud Gateway) builds from the repository root Maven build and starts with an externalized route configuration listing /v1/customers, /v1/policies, /v1/claims, /v1/premium, /v1/billing, /v1/reports, /v1/authz and /v1/audit routes.
- TLS 1.3 is enforced at the edge; a connection attempt negotiating TLS 1.1 or 1.2 is rejected, and responses carry Strict-Transport-Security, Content-Security-Policy, X-Content-Type-Options nosniff, X-Frame-Options DENY and Referrer-Policy headers verified by an automated header assertion test.
- Rate limiting is enforced at 100 requests per minute per principal key (falling back to client IP when no principal is present) using a Redis-backed token bucket; the 101st request in a window returns HTTP 429 with an RFC 9457 problem detail containing a retry-after hint and no stack trace.
- Every request receives or reuses an X-Correlation-Id, which is propagated to downstream services and emitted in structured JSON logs together with route id, principal placeholder, HTTP method, path, status and latency; no request or response body content is logged.
- Upstream failures are normalised: connection refused or timeout returns 503 with an RFC 9457 problem detail, 4xx from upstream is passed through unchanged, and circuit-breaker open state returns 503 rather than leaking upstream exception text.
- Unit tests cover route predicate matching, header injection, rate-limit key resolution and problem-detail mapping; all pass in CI.
- System integration tests using Testcontainers spin up the gateway plus a WireMock upstream and Redis, and assert routing, rate limiting, header hardening, correlation propagation and 503 normalisation across the real HTTP boundary.
- Mock data and fixtures are committed: WireMock stub mappings for each of the eight routes, a rate-limit load fixture script, and a TLS test keystore generated by a committed script with placeholder-only secrets (no real keys or credentials in the repository).
- Helm chart and Terraform module for the gateway are committed, including liveness and readiness probes, non-root distroless container, HPA on CPU and request rate, resource requests/limits, and PodDisruptionBudget; helm template and terraform validate run clean in CI.
- An operational runbook is committed covering certificate rotation, rate-limit tuning, route add/remove procedure, rollback to the previous Helm revision within 15 minutes, and the SLO definition (99.9% availability, p99 added latency under 25 ms) with alert rules.

**Depends on:** Kubernetes cluster and container registry available, Managed Redis instance or in-cluster Redis for rate-limit state

### [P0] Enforce Zone Segmentation, mTLS and Egress Allow-Lists

In the current architecture the security boundary is the IBM i partition itself: object authority and the library list INSPRD INSPRDDTA INSCOM QGPL are the only controls, INSCOM service programs are resolved at runtime through standard *LIBL program-call resolution, and a CALL cannot be refused. Because presentation, logic and data run in one process there is no network segmentation and no trust boundary between a program and the service programs it invokes. This story replaces that implicit trust with explicit, enforced conduits on Kubernetes: four zones (public, DMZ, internal, data), default-deny NetworkPolicies so a pod can only reach the peers its policy names, mutual TLS between all internal pods so identity is cryptographic rather than positional, a WAF in front of the gateway with managed rule groups plus PCIS-specific rules, and an egress allow-list restricting outbound traffic to the identity provider, payment gateway, document storage and notification endpoints only — closing the SSRF exposure that an unrestricted service could otherwise create. Everything is expressed as infrastructure-as-code and validated by automated policy tests so a missing or over-broad policy fails the pipeline instead of being discovered in production. Scope is network and transport-level segmentation; application-level authorization is WO-161 and business authority checks belong to the authorization service epic.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:api-gateway, security, network, infrastructure-as-code, complexity:high |

**Acceptance Criteria**
- A default-deny NetworkPolicy exists for every namespace covering both ingress and egress, and a committed policy test proves that a pod with no explicit allow rule cannot reach any other pod, the database, or the internet.
- Per-service NetworkPolicies allow only the documented conduits: gateway to the eight services, services to their own database and to the message broker, batch jobs to database and broker, reporting to the read replica only and never to the OLTP primary — each conduit asserted by an automated test.
- Mutual TLS is enforced for all internal pod-to-pod traffic (strict mode), verified by a test showing a plaintext connection attempt between two pods is refused and by inspecting the negotiated peer identity.
- Egress is restricted to an explicit allow-list of fully qualified destinations (identity provider, payment gateway, document storage, notification provider, container registry, telemetry endpoint); a test asserts an arbitrary outbound host resolves and connects only if allow-listed, and is refused otherwise with a structured log entry.
- A WAF is associated with the gateway load balancer with managed rule groups for common injection and bot patterns plus PCIS-specific rules (request size cap, path allow-list for /v1 and actuator health, blocked administrative paths); WAF logs are shipped to the central log pipeline.
- All zone, policy, mTLS and WAF configuration is expressed as Terraform and Kubernetes manifests in the repository, with terraform validate, tflint, kubeconform and an OPA or Conftest policy suite all passing in CI.
- A conftest/OPA rule fails the pipeline if any new workload manifest is added without a matching NetworkPolicy, or if a NetworkPolicy uses an empty podSelector combined with an allow-all rule.
- Unit-level policy tests are written and passing for the OPA rules themselves (positive and negative manifests); N/A for application unit tests — this story changes infrastructure configuration rather than application code paths.
- System integration tests run against an ephemeral cluster (kind or equivalent in CI) deploying representative pods and asserting allowed conduits succeed while every other combination is refused, including reporting-to-primary being blocked.
- Mock data and fixtures are committed: sample allowed and denied workload manifests for the OPA suite, a network probe container manifest used by the connectivity tests, and a fixture list of allow-listed and non-allow-listed egress hosts — no real endpoints or credentials, placeholders only.
- A runbook is committed covering how to add a conduit or egress destination through code review, blast-radius expectations per zone, how to diagnose a blocked call (which metric and log line to read), and how to temporarily and auditably widen a policy during an incident.

**Depends on:** Kubernetes cluster with a CNI that enforces NetworkPolicy, Service mesh or sidecar capability for mTLS enforcement, Cloud WAF available in the target account

### [P0] Implement OIDC Federation and JWT Validation Enforcement

Authorization in PCIS today is entirely presentation-layer: the 5250 sign-on establishes an IBM i job user profile, interactive programs recover it with SET :HV-CURRENT-USER = CURRENT USER and fall back to the literal PCISBATCH when SQLCODE is non-zero, and access is gated only by ROLE_MENU_T menu-option visibility and function-key text such as F6=Update (if authorized). That is the exact OWASP A01 pattern the organization policy forbids. This story federates human authentication to a central OIDC identity provider using Authorization Code with PKCE, terminates the session as an httpOnly, Secure, SameSite=Strict cookie so no token is stored in browser storage, and enforces local RS256 JWT validation against a cached JWKS at the gateway plus a shared resource-server starter used by every domain service. Every route is deny-by-default: a route with no declared required scope or role is refused at startup, not at runtime. The validated principal, its roles and the correlation identifier are propagated downstream as signed internal headers so services and batch steps can apply Spring Security method-level checks and so audit events can carry a real actor instead of a compiled-in literal. Scope is authentication and coarse-grained authorization enforcement at the edge and in the shared starter; the claim-payment authority-limit and approval-linkage decision logic belongs to the authorization domain service epic.

| Field | Value |
|---|---|
| Story Points | 13 |
| Hours | 130h |
| Priority | P0 |
| Labels | epic:api-gateway, security, identity, owasp-a01, complexity:high |

**Acceptance Criteria**
- Gateway is configured as an OAuth2 client performing Authorization Code with PKCE against the configured OIDC provider; login redirects, callback handling and RP-initiated logout all work against a containerised test identity provider.
- Successful login sets an httpOnly, Secure, SameSite=Strict session cookie; an automated test asserts no access or refresh token value ever appears in a response body, in a Set-Cookie without httpOnly, or in any log line.
- Access tokens are validated locally with RS256 against a JWKS cache with a 1 hour TTL and background refresh; validation adds no more than 5 ms at p99 and performs no network hop to the identity provider on the happy path, proven by a benchmark test and by asserting zero JWKS requests during a steady-state load run.
- Token validation rejects, with 401 and an RFC 9457 problem detail, each of: expired token, wrong issuer, wrong audience, unsupported algorithm (including alg none and HS256 substitution), unknown key id, and malformed or truncated token — one test per case.
- Deny-by-default is enforced structurally: gateway startup fails with a descriptive error if any configured route lacks a required-scope or required-role declaration, and any request to a route the principal does not satisfy returns 403 with a distinct problem-detail type from 401.
- A shared Maven starter (pcis-security-starter) configures each domain service as an OAuth2 resource server with the same issuer, audience and JWKS settings, registers a JwtAuthenticationConverter mapping OIDC claims to Spring Security authorities, and enables method security so @PreAuthorize is active by default.
- Validated principal identity, roles and correlation identifier are propagated to downstream services as internal headers that are stripped from inbound client requests and re-added only by the gateway, with an automated test proving an externally supplied principal header cannot be spoofed.
- A committed mapping artifact translates legacy ROLE_MENU_T-derived roles (claims adjuster, claims supervisor, customer service representative, policy administrator, agent, batch operations, compliance auditor) to OIDC scopes and Spring Security authorities, and is referenced by both the gateway route policy and the starter.
- Unit tests cover JWT decoding branches, claim-to-authority mapping, cookie attributes, route policy validation and header stripping; all pass in CI with coverage on the security package at or above 90 percent line coverage.
- System integration tests using Testcontainers run the gateway, the shared starter embedded in a sample service, and a test OIDC provider, and validate the full flow: unauthenticated request refused, authorization-code login, authenticated call reaching the service with correct authorities, insufficient-scope 403, expired-token 401, refresh rotation, and logout invalidating the session.
- Mock data and fixtures are committed: test identity-provider realm/client definitions, generated test RSA key pair and JWKS document produced by a committed script, and a fixture set of signed tokens for each rejection scenario — all using placeholder secrets only.
- A runbook section is committed covering identity-provider outage behaviour, JWKS key rotation, token TTL changes (10 minute access, 8 hour refresh rotated on use), session revocation procedure and the alert conditions for elevated 401/403 rates.

**Depends on:** Central OIDC identity provider with client registration for the gateway and per-service audiences, Managed secret store for the gateway client secret and cookie signing key

### [P0] Federate Workload Identity For Batch and Service Calls

Batch identity in PCIS is a literal: AUD002B runs as BATCHAUD, BIL003B as BATCHBIL, CMM001B as BATCHCMM, PRM005B as BATCHPRM, CLM006B as BATCHCLM and POL006B as BATCHREN, all compiled into WORKING-STORAGE, and CUS001A falls back to PCISBATCH when it cannot read CURRENT USER. Because there is no principal there is nothing to authorize, which is precisely why the claim payment batch can insert CLAIM_PAYMENT_T rows with no authority evaluation. This story gives every non-human caller a real, short-lived, revocable identity: Kubernetes service account tokens are exchanged with the identity provider for OAuth2 client-credentials access tokens scoped to exactly what that job or service needs, tokens are cached in memory with proactive refresh, and the resulting machine principal is carried into downstream REST calls and into audit event construction so the actor recorded is a verifiable workload identity rather than a compiled literal. Machine principals are explicitly separated from human roles so a batch token can never inherit an adjuster or supervisor authority, and every credential is stored in the managed secret store with automated rotation. Scope is issuance, caching, propagation and rotation of machine identity plus its integration with the shared security starter; the business authority-limit and approval-linkage checks that consume this principal belong to the authorization service epic.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | epic:api-gateway, security, identity, batch, complexity:high |

**Acceptance Criteria**
- Each batch job and each domain service is registered as a distinct OAuth2 client-credentials principal with a least-privilege scope set, and a committed manifest maps workload name to client identifier, scopes and the operations it may perform.
- A MachineTokenProvider in the shared security starter obtains client-credentials tokens by exchanging the projected Kubernetes service account token (workload identity federation) so no long-lived client secret is stored in a container image or manifest; any required secret is referenced as a placeholder resolved from the managed secret store.
- Tokens are cached in memory and refreshed proactively before expiry with jittered retry; a test asserts that N sequential outbound calls within a token lifetime trigger exactly one token request, and that refresh failure causes the caller to fail closed rather than proceed unauthenticated.
- Machine principals are structurally separated from human roles: an automated test asserts that a client-credentials token carrying batch scopes is refused (403 with a distinct problem-detail type) on any endpoint requiring a human role, and that human tokens are refused on batch-only endpoints.
- Every Spring Batch job step and every outbound service-to-service call resolves its actor from the authenticated machine principal, and the AuditActorProvider returns that principal so no audit event is constructed with a hard-coded literal; a test asserts no occurrence of BATCHAUD, BATCHBIL, BATCHCMM, BATCHPRM, BATCHCLM, BATCHREN or PCISBATCH as an actor value in produced audit events.
- Credential and key rotation is automated and documented: rotating the identity-provider client credential or the signing key does not require a redeploy, and a rotation drill in staging shows zero failed requests during the rotation window.
- Unit tests cover token acquisition, cache hit and miss, proactive refresh timing, jittered retry, fail-closed behaviour, scope-to-authority mapping and machine-versus-human separation; all pass in CI.
- System integration tests using Testcontainers run a test identity provider, a sample Spring Batch job and a sample resource service, and assert the job obtains a token, calls the service successfully with its scopes, is refused on a human-role endpoint, recovers after a simulated token endpoint outage, and records the machine principal as the audit actor.
- Mock data and fixtures are committed: test identity-provider client registrations for each workload, a projected service account token fixture, and signed client-credentials token fixtures for valid, expired, wrong-audience and insufficient-scope cases — placeholders only, no real secrets.
- Metrics and structured logs are emitted for machine_token.issued, machine_token.refresh_failure and authz.denied labelled by workload and reason, with alert rules for repeated refresh failure and for any batch run that starts without a valid token.
- A runbook is committed covering adding a new workload identity, scope change review, credential rotation procedure, identity-provider outage impact on the nightly batch window, and the manual break-glass procedure with its audit requirements.

**Depends on:** Identity provider supporting client-credentials grant and token exchange from Kubernetes projected service account tokens, Managed secret store with automated rotation

---

## Migration Governance, Behaviour Decisions and Cutover Control

### [P0] Machine-readable open design item decision register with CI gate

WHAT & WHY: PCIS_Enterprise_Architecture.md section 7.4 carries twelve explicitly unresolved open design items (renewal window configurability, pro-rata cancellation refund formula, CANCELLATION_REASON_T domain, POL002A UW-referral threshold, BILLING_SCHEDULE_T V=Void status, POL004A commit granularity, CLM adjuster auto-assignment, the CLM003A-to-CLM004A approval linkage, CLM late-reporting threshold, payee/vendor master, reinsurance cession informational-versus-mandatory-stop, CLAIM_DOCUMENT_T IFS storage contract). Today these live only as prose in a markdown document with no owner, no due phase and no enforcement, which is exactly how ambiguous legacy behaviour becomes arbitrary new-platform behaviour (risk R12). We need a machine-readable decision register plus a build-time gate so no migration phase can be declared complete while an item owned by that phase is still open. IMPACT: adds a new governance module directory (governance/) containing the register file, a JSON schema, a Java/Maven validator tool, and a Forge Shipping gate step definition; references existing evidence files PCIS_Enterprise_Architecture.md, CLM_Module_Design_Document.md, POL_Module_Design_Document.md, CLM006B.cbl, POL006B.cbl, BIL003B.cbl. WHAT DONE LOOKS LIKE: governance/open-design-items.yaml holds all twelve items with id, title, evidence citation (file plus section), owning phase, business decision-maker role, status (OPEN, DECIDED, CONFIGURATION_DRIVEN), decision text, decided-on date and a link to the tunable or story that implements it; a validator command fails the build when an item is malformed, unowned, or OPEN while its owning phase gate is being evaluated; the pipeline emits a human-readable register report as a build artifact. SCOPE BOUNDARIES: this story does NOT make the business decisions themselves, does NOT implement any tunable or schema change (e.g. it does not add the V=Void status or APPROVAL_T table), and does NOT build the phase evidence pack (WO-173). DEPENDENCIES: none blocking; consumed later by WO-173 evidence pack and WO-174 behaviour decision records, which cross-reference item ids.

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P0 |
| Labels | complexity:medium, governance, ci-cd, migration |

**Acceptance Criteria**
- governance/open-design-items.yaml exists and contains exactly the twelve items enumerated in PCIS_Enterprise_Architecture.md section 7.4, each with id, title, evidence_file, evidence_section, owning_phase, decision_owner_role, status, decision_text and implemented_by fields.
- A JSON Schema (governance/schema/open-design-items.schema.json) validates the register; the validator exits non-zero with a field-level message when a required field is missing, an unknown status value is used, or an evidence_file path does not exist in the repository.
- Running the validator with a phase argument (for example --phase=CLAIMS) exits non-zero and lists offending item ids when any item whose owning_phase equals that phase has status OPEN; it exits zero when all such items are DECIDED or CONFIGURATION_DRIVEN.
- Items with status CONFIGURATION_DRIVEN must reference a tunable key in implemented_by; the validator fails if the referenced key is absent from the tunables inventory file.
- A generated markdown report (target/governance/open-design-items-report.md) lists every item grouped by owning phase with status and owner, and is published as a pipeline artifact on every build.
- Unit tests written and passing: validator tests cover valid register, missing field, unknown status, dangling evidence path, phase-gate open item, and CONFIGURATION_DRIVEN without tunable key.
- System integration tests validating service/API boundaries: a pipeline-level test invokes the validator through the Forge Shipping gate step definition and asserts the build fails on a deliberately open item and passes after it is marked DECIDED.
- Mock data/fixtures generated and committed: fixture registers under governance/src/test/resources (valid.yaml, missing-owner.yaml, open-in-phase.yaml, bad-evidence.yaml) so the test suite runs with no external dependencies.

### [P0] Parallel-run reconciliation engine with per-domain cutover gate scoring

WHAT & WHY: Every domain cutover is gated on a minimum 30-day parallel run with cent-level reconciliation against the COBOL baseline and zero unexplained breaks, but no reconciliation tooling exists anywhere in the repository. Without an automated, repeatable comparison between Db2 for i (the system of record during coexistence) and the target PostgreSQL model, parity sign-off would depend on manual spot checks — the same inspection-only verification the programme is trying to eliminate. This story builds the reconciliation engine and the gate scorecard that decides whether a domain may cut over. IMPACT: new reconciliation module (recon/) with Spring Batch jobs, comparison rules per entity, break persistence tables, Prometheus metrics and a gate-scoring report; consumes legacy extract snapshots produced by the coexistence polling extract and the migrated domain schemas; touches money-path entities evidenced in BIL003B.cbl (BILLING_SCHEDULE_T, INVOICE_T), CMM001B.cbl (COMMISSION_LEDGER_T), CLM006B.cbl (CLAIM_PAYMENT_T, CLAIM_RESERVE_T, RECOVERY_T), PRM005B.cbl (BILLING_SCHEDULE_T status transitions), POL006B.cbl (POLICY_T, POLICY_HISTORY_T, COVERAGE_T) and AUD002B.cbl (AUDIT_LOG_T counts). WHAT DONE LOOKS LIKE: a nightly reconciliation job per domain reads the legacy snapshot and the target tables, compares row counts, business-key sets and every monetary and status column using BigDecimal exact comparison at scale 2, writes typed break records with a classification (MISSING_IN_TARGET, MISSING_IN_LEGACY, VALUE_MISMATCH, COUNT_MISMATCH, CHECKSUM_MISMATCH), exposes per-domain metrics, and produces a gate scorecard stating consecutive clean days, break totals by class and a PASS or FAIL gate verdict. SCOPE BOUNDARIES: this story does NOT build the legacy extraction/polling pipeline itself, does NOT implement the domain services or their schemas, does NOT perform the traffic switch (WO-172), and does NOT define golden-output batch unit tests (owned by the regression harness epic). DEPENDENCIES: assumes the coexistence extract lands legacy snapshots in a staging schema and that target domain tables exist; the register from WO-170 supplies decision ids referenced when a break is classified as an approved behaviour change.

| Field | Value |
|---|---|
| Story Points | 13 |
| Hours | 130h |
| Priority | P0 |
| Labels | complexity:high, migration, reconciliation, observability, data |

**Acceptance Criteria**
- A reconciliation job can be launched per domain (customer, policy, billing, premium, claims, reporting) with a business-date parameter, and completes with a persisted RECON_RUN row recording start, end, entity count, compared row count and break counts by classification.
- All monetary comparisons use BigDecimal with scale 2 and exact equality (no epsilon tolerance); a one-cent difference on any of PREM_ANNUAL, DUE_AMT, PAID_AMT, COMMISSION_AMT, APPROVED_AMT, PAID_TO_DATE or payment amount produces a VALUE_MISMATCH break naming entity, business key, column, legacy value and target value.
- Break records are persisted in RECON_BREAK with classification, entity, business key, column, legacy value, target value, first-seen and last-seen timestamps, and an optional approved_decision_id linking to a WO-170 register item so approved behaviour changes are excluded from the unexplained-break count.
- A gate scorecard endpoint and report expose consecutive clean days per domain and a PASS verdict only when unexplained breaks equal zero for at least the configured minimum window (default 30 days); the verdict is FAIL otherwise and the reason is stated.
- Reconciliation is idempotent and restartable: rerunning the same domain and business date does not duplicate break rows and resumes from the last committed chunk after an injected failure.
- Prometheus metrics recon_breaks_total by domain and classification, recon_rows_compared_total, recon_run_duration_seconds and recon_consecutive_clean_days are exposed, and an alert rule fires when unexplained breaks are greater than zero for a domain in parallel-run state.
- Unit tests written and passing: comparison rule tests cover equal rows, cent-level mismatch, missing key on each side, null versus zero, status-code mismatch and approved-decision suppression.
- System integration tests validating service/API boundaries: Testcontainers PostgreSQL integration test seeds a legacy snapshot schema and a target schema, runs the billing and claims reconciliation jobs end to end, and asserts break counts, RECON_RUN contents, metric values and the gate verdict.
- Mock data/fixtures generated and committed: deterministic seed scripts producing matched and deliberately mismatched populations (including frequencies M, Q, S and an out-of-domain value, and a claim where PAID_TO_DATE differs by one cent) so the suite runs without access to Db2 for i.

**Depends on:** WO-170

### [P1] Per-domain cutover control plane with audited rollback switches

WHAT & WHY: Cutover must be phased per domain with Db2 for i remaining the system of record until that domain passes its parallel-run gate, and every phase needs a rollback path executable in under fifteen minutes. Today the only rollback unit is restoring a saved IBM i library and promotion is a manual library copy along INSDEV to INSTST to INSPRD, which gives no per-domain granularity, no audit of who switched what, and no way to stop double-writing during coexistence. This story builds the cutover control plane: an explicit, audited state machine per domain that gateway routing, domain services and batch jobs all consult before reading or writing. IMPACT: new cutover module or shared-kernel component exposing domain cutover state, a write-guard interceptor used by every mutating service path and batch step, gateway routing configuration, Helm values and Argo CD application settings, plus an operational runbook document. WHAT DONE LOOKS LIKE: each domain has one of the states LEGACY_ONLY, SHADOW_WRITE, PARALLEL_RUN, TARGET_PRIMARY or TARGET_ONLY; the state is readable by services at low latency, changeable only by an authorised operator through an audited API, and enforced so that no financial mutation is written to a target domain still in LEGACY_ONLY and no duplicate authoritative write occurs in PARALLEL_RUN; a rollback command returns a domain to its prior state and is proven in a drill within the fifteen-minute target. SCOPE BOUNDARIES: this story does NOT implement the reconciliation comparison logic (WO-171), does NOT build the legacy extract pipeline, does NOT provision cloud infrastructure, and does NOT change any domain business logic. DEPENDENCIES: consumes the gate verdict published by WO-171 so a promotion to TARGET_PRIMARY is refused while the verdict is FAIL; uses the authorization service for deny-by-default enforcement on state changes.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P1 |
| Labels | complexity:high, migration, cutover, security, operability |

**Acceptance Criteria**
- A cutover state store holds one row per domain with state, previous_state, changed_by, changed_at, reason and linked gate verdict snapshot; states are constrained to LEGACY_ONLY, SHADOW_WRITE, PARALLEL_RUN, TARGET_PRIMARY, TARGET_ONLY.
- State changes are performed only through an authenticated, deny-by-default endpoint requiring a cutover-operator permission; every change writes an immutable audit event with actor, resource (domain), operation, old and new state and reason, and the audit write is in the same transaction as the state change.
- Promotion to TARGET_PRIMARY is rejected with a distinct reason code when the WO-171 gate verdict for that domain is FAIL or the minimum parallel-run window has not elapsed; the rejection is audited.
- A write-guard component rejects any target-side financial mutation for a domain in LEGACY_ONLY, records the attempt as a structured denial event, and permits target writes in SHADOW_WRITE only through the non-authoritative shadow path.
- Batch jobs and service endpoints read cutover state through a cached client (cache TTL configurable, default 30 seconds) and fail closed to the more restrictive state if the store is unreachable.
- A rollback operation returns a domain to previous_state in one call, is idempotent, is audited, and a documented drill demonstrates end-to-end rollback (state switch plus Argo CD revision rollback plus paired Flyway down-migration or PITR decision) completing within fifteen minutes.
- An operational runbook document is committed covering per-domain promotion, freeze windows, rollback steps, decision tree for schema rollback versus PITR, and the escalation contacts, referencing the INSDEV/INSTST/INSPRD topology during coexistence.
- Unit tests written and passing: state machine transition tests (all legal and illegal transitions), gate-verdict rejection, fail-closed cache behaviour, write-guard allow/deny matrix and rollback idempotency.
- System integration tests validating service/API boundaries: Testcontainers-backed test drives state changes through the API, asserts audit rows, asserts a claims payment write is denied in LEGACY_ONLY and permitted in TARGET_PRIMARY, and asserts promotion is refused on a FAIL gate verdict.
- Mock data/fixtures generated and committed: fixture gate verdicts (PASS, FAIL, insufficient window) and seed cutover states per domain so tests run with no external dependencies.

**Depends on:** WO-171

### [P1] Automated phase-gate evidence pack generation and governance dashboard API

WHAT & WHY: Each phase gate and each domain cutover must be signed off against objective evidence: golden-output coverage on monetary logic, zero unexplained reconciliation breaks over the parallel-run window, one hundred percent of claim payments passing a server-side authority check, all fifty-five tables classified, purge executed within twenty-four hours of expiry, six of six regulatory tunables changeable without deployment, and batch restartability under fault injection. Today that evidence would be assembled by hand from scattered reports, which is slow, error-prone and unauditable. This story automates collection of gate evidence into a signed, versioned pack and exposes a governance dashboard API so programme, compliance and audit stakeholders read the same numbers. IMPACT: new governance reporting component that pulls from the CI coverage report, the WO-170 decision register, the WO-171 gate scorecard, the WO-172 cutover state history, authorization test results, purge run logs and batch fault-injection results; adds a pipeline step producing the pack artifact and a read-only API consumed by the admin governance screens. WHAT DONE LOOKS LIKE: running the pack generator for a phase produces a JSON manifest plus a human-readable markdown report listing every gate criterion, its measured value, its threshold, PASS or FAIL and the source artifact reference; the pipeline fails the gate stage when any mandatory criterion is FAIL; a read-only API returns the current gate status per domain and per criterion. SCOPE BOUNDARIES: this story does NOT compute the underlying metrics (coverage, reconciliation, purge, authorization tests are produced elsewhere), does NOT build the admin UI screens, and does NOT perform stakeholder sign-off. DEPENDENCIES: consumes outputs of WO-170 (register report), WO-171 (gate scorecard and metrics) and WO-172 (cutover state history).

| Field | Value |
|---|---|
| Story Points | 5 |
| Hours | 50h |
| Priority | P1 |
| Labels | complexity:medium, governance, ci-cd, compliance, observability |

**Acceptance Criteria**
- A gate criteria definition file declares every mandatory and advisory criterion with id, description, source type, threshold, comparison operator and applicable phases, covering at minimum monetary-logic line coverage at or above ninety percent, zero unexplained reconciliation breaks over the configured window, one hundred percent of claim payment paths passing an authority check, fifty-five of fifty-five tables classified, purge completion within twenty-four hours, six of six tunables externalized, commit blast radius at or below one thousand rows for the archive job, and batch restart with zero duplicate or orphaned financial records.
- The generator collects each criterion value from its declared source (JaCoCo XML, reconciliation gate endpoint or exported JSON, decision register report, purge run-log export, authorization test result file, fault-injection test result file) and records the source artifact path or URL plus a content hash for traceability.
- The generator writes a JSON manifest and a markdown evidence pack under target/governance/gate-pack/{phase}/ containing every criterion with measured value, threshold, verdict and source reference, plus the overall phase verdict and generation timestamp.
- The pipeline gate step exits non-zero when any mandatory criterion is FAIL or when a mandatory criterion's source artifact is missing (missing evidence is treated as FAIL, never as PASS).
- A read-only API exposes GET /v1/governance/gates and GET /v1/governance/gates/{phase} returning the same criterion-level data as the manifest, protected by an authenticated read permission and returning RFC 9457 problem details on error.
- The evidence pack is reproducible: generating twice from identical inputs yields identical manifests apart from the timestamp field, and the manifest includes the register, reconciliation and cutover artifact hashes it was built from.
- Unit tests written and passing: criterion evaluation for each comparison operator, missing-source handling, mandatory versus advisory verdict aggregation and manifest reproducibility.
- System integration tests validating service/API boundaries: an integration test runs the generator against committed fixture artifacts, asserts the manifest and markdown content, asserts non-zero exit on a failing mandatory criterion, and asserts the governance API returns matching data with authorization enforced.
- Mock data/fixtures generated and committed: fixture JaCoCo XML, reconciliation gate JSON, purge run-log export, authorization test result and fault-injection result files under the test resources so generation runs with no external dependencies.

**Depends on:** WO-170, WO-171, WO-172

### [P0] Legacy behaviour decision records with preserve-versus-change parity matrix

WHAT & WHY: The COBOL baseline contains behaviours that must be deliberately preserved to the cent and behaviours that must deliberately change, and today nothing distinguishes them. Evidenced examples: BIL003B silently skips candidates outside the fifteen-day lead window while still counting them as eligible; BIL003B, CMM001B, PRM005B, POL006B and CLM006B continue after a non-zero AUDLOG01 return so a committed financial mutation can have no audit record (PRM005B even documents this in a comment); CLM006B always pays the full outstanding reserve amount and raises the reinsurance cession flag as informational only; BIL003B reuses HV-INSTALLMENT-NBR as a scratch days-out counter; CMM001B skips agents with no in-force commission plan by incrementing a counter; AUD002B halts the whole run on a verification mismatch. Without an explicit register, a developer cannot tell whether a divergence found in reconciliation is a defect or an approved improvement. IMPACT: adds a behaviour parity matrix file, a validator enforcing that every entry cites source evidence and links to a test, generated documentation, and a linkage from reconciliation break suppression (WO-171 approved_decision_id) to matrix entry ids. WHAT DONE LOOKS LIKE: every catalogued legacy behaviour has an id, evidence citation down to program and paragraph, a decision of PRESERVE or CHANGE, rationale, owning decision-maker role, and a reference to the test that proves the decision; CI fails when an entry lacks a test reference, when a CHANGE entry lacks an approval, or when a test referenced by the matrix does not exist. SCOPE BOUNDARIES: this story does NOT implement the behaviour changes themselves (transactional audit outbox, exception surfacing, set-based reads are owned by their domain epics), does NOT write the golden-output batch tests, and does NOT decide business policy. DEPENDENCIES: shares tooling and conventions with WO-170's register validator; supplies decision ids consumed by WO-171 break suppression.

| Field | Value |
|---|---|
| Story Points | 8 |
| Hours | 80h |
| Priority | P0 |
| Labels | complexity:high, governance, migration, testing, compliance |

**Acceptance Criteria**
- governance/behaviour-decisions.yaml catalogues at minimum the following evidenced behaviours, each with a unique id: BIL003B silent skip of candidates outside the lead window while counting them eligible; the audit-write-failure continue path in BIL003B, CMM001B, PRM005B, POL006B and CLM006B; CLM006B full-outstanding payment computation; CLM006B informational-only reinsurance cession flag; CLM006B absence of any authority check; BIL003B reuse of HV-INSTALLMENT-NBR as a days-out counter; CMM001B no-in-force-plan counter path; CMM001B COMM_CALC_FLAG idempotency guard; PRM005B grace-period status transitions; AUD002B halt-on-verification-mismatch and archive-verify-then-delete ordering; per-item commit granularity stated in each program prologue.
- Each entry carries evidence_program, evidence_paragraph, evidence_excerpt, decision (PRESERVE or CHANGE), rationale, decision_owner_role, approved_by, approved_on, linked_open_design_item (optional) and test_ref pointing at a test class and method.
- A validator command fails the build when any entry lacks evidence_program or test_ref, when a CHANGE entry lacks approved_by and approved_on, when evidence_program does not name a file present in the repository, or when the referenced test class or method cannot be located in the source tree.
- Every PRESERVE entry's referenced test asserts the legacy behaviour is reproduced exactly (including exact rounding and per-item commit boundaries) and every CHANGE entry's referenced test asserts the legacy behaviour is NOT reproduced — for example that a failed audit write rolls the financial mutation back rather than leaving it committed.
- Reconciliation break suppression accepts only approved_decision_id values that resolve to a CHANGE entry in the matrix; a suppression referencing a PRESERVE entry or an unknown id is rejected and reported.
- A generated markdown document (target/governance/behaviour-parity-matrix.md) lists every behaviour grouped by program with decision, rationale, owner and test reference, and is published as a pipeline artifact.
- Unit tests written and passing: validator tests for missing evidence, missing test_ref, unapproved CHANGE, dangling test reference, dangling program reference and suppression-id resolution.
- System integration tests validating service/API boundaries: a pipeline-level test runs the validator against the real matrix and asserts exit code 0, then against mutated copies asserting exit code 1 with the offending id; a second test asserts that the reconciliation suppression path rejects a PRESERVE id.
- Mock data/fixtures generated and committed: fixture matrices for each failure mode plus a stub test class used to prove test-reference resolution, so the suite runs with no external dependencies.

**Depends on:** WO-170, WO-171