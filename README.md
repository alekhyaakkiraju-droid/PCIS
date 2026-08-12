# PCIS — Property & Casualty Insurance System

**Modernization of a single-partition IBM i (COBOL / DDS / Db2) policy administration
system into cloud-native microservices.** This repository contains both the original
legacy source (for reference and parity testing) and the modernized replacement system.

## Why this exists

PCIS is a hackathon project demonstrating a legacy-to-cloud modernization: taking a
1990s-style IBM i insurance application — ILE COBOL batch/interactive programs, DDS
5250 green-screen panels, embedded SQL against Db2 for i — and rebuilding it as a
set of Spring Boot microservices with a React SPA front end, without losing any of
the original business logic or breaking any downstream integration.

The legacy source lives in `Property_Casualty_Insurance_System/`, alongside the
module design documents (`*_Module_Design_Document.md`) that were used to reverse
engineer the COBOL into a modern domain model. Those documents are the source of
truth for "what the old system actually did," including its bugs and gaps — several
of which are called out below because the modernization deliberately fixes them
rather than silently carrying them forward.

## Architecture at a glance

| | Legacy | Modernized |
|---|---|---|
| Runtime | IBM i, single partition | Spring Boot 3.5 / Java 21 microservices, target: Kubernetes (EKS) |
| Interactive UI | DDS 5250 green-screen panels | React 19 + TypeScript SPA (Vite) |
| Batch | ILE COBOL batch programs, IBM i job scheduler (`JOBSCHD1–7`) | Spring Batch jobs, Kubernetes CronJobs |
| Database | Db2 for i | PostgreSQL 17, one database per domain service |
| Auth | 5250 sign-on / SECCHK01 | Keycloak (OIDC), gateway-enforced JWT + per-endpoint RBAC |
| Integration | Direct program calls, shared files | REST APIs behind an API gateway, outbox-pattern domain events (Kafka) |
| Audit | `AUDLOG01` shared routine (never had source — a documented gap) | `audit-svc`, a real service with a versioned event contract |

## Legacy program → modernized replacement

This is the core of the modernization: every legacy program either has a direct
1:1 replacement, or — where the legacy design was only ever a stub — a real
implementation for the first time.

| Legacy program | Legacy role | Status in legacy repo | Modern replacement |
|---|---|---|---|
| `CUS001A` | Customer maintenance (5250) | Shipped | `customer-svc` REST API + Customer 360 UI |
| `CUS002A`–`CUS005A` | Customer inquiry, list, delete, contacts | Design-only, never built | `customer-svc` + Customer 360 UI |
| `POL001A` | Policy issuance (5250) | Shipped | `policy-svc` (issuance, endorsement, cancellation) |
| `POL006B` | Policy renewal batch | Shipped | `policy-batch` → `policyRenewalJob` |
| `CLM001A`–`CLM005A` | FNOL, reserve, adjuster assignment, inquiry, closure | Design-only, never built | `claims-svc` (FNOL wizard, reserves, approvals, payments) |
| `CLM006B` | Claim payment batch | Shipped | `claims-batch` → `claimPaymentJob` |
| `BIL003B` | Billing installment generation batch | Shipped | `billing-svc` → `billingGenerationJob` |
| `CMM001B` | Agent commission calculation batch | Shipped | `billing-svc` → `commissionCalculationJob` |
| `PRM005B` | Premium delinquency aging batch | Shipped | `billing-svc` → `delinquencyAgingJob` |
| `AUD002B` | Audit log archive batch | Shipped | `audit-batch` → `auditArchiveJob` |
| `AUDLOG01` | Shared audit-write routine | **Missing callee** — called by 7 programs, no source ever existed | `audit-svc` (real service, unified event contract) |
| `RPT001A`, `RPT006A` | Report menu, commission report | Design-only, never built | `reporting-svc` |
| — | No legacy equivalent | — | `authz-svc` — deny-by-default RBAC, didn't exist on IBM i (auth was a 5250 sign-on + hardcoded checks) |
| — | No legacy equivalent | — | `auditPurgeJob`, `reconciliationJob`, parallel-run cutover scorecard — needed for a safe migration, had no legacy counterpart |
| — | Migration tooling | — | `sync-agent` — watermark-driven Db2-for-i → PostgreSQL sync, used during parallel run |

A few known legacy defects were deliberately **not** carried forward — e.g. `AUD002B`
and four other batch programs don't roll back their business mutation when the audit
write fails (a documented gap in the design docs); the modernized batch jobs treat
a failed audit write as a hard failure of the whole unit of work.

## Services

| Service | Purpose |
|---|---|
| `api-gateway` | Spring Cloud Gateway — JWT validation, rate limiting, routes `/api/v1/**` to each domain service |
| `customer-svc` | Customer onboarding, duplicate resolution, Customer 360 aggregation |
| `policy-svc` | Policy issuance, endorsement, renewal, cancellation, coverage/billing-plan management |
| `claims-svc` | FNOL intake, reserve management, adjuster authority, payment approval, recovery |
| `billing-svc` | Installment schedules, invoices, commission ledger, and the 3 billing-domain batch jobs |
| `premium-svc` | Premium rating (base rates, rate factors, surcharges) |
| `audit-svc` | Immutable audit trail — the real implementation of what `AUDLOG01` never was |
| `authz-svc` | Deny-by-default authorization: roles, permissions, policy decisions |
| `config-svc` | RBAC-gated admin API for operational tunables, plus the cross-domain batch run history used by the Batch Operations Console |
| `reporting-svc` | Operational reporting off a read replica, domain-event notifications |
| `audit-batch`, `claims-batch`, `policy-batch` | Spring Batch replacements for the single-domain legacy batch programs (`AUD002B`, `CLM006B`, `POL006B`) |
| `reconciliation-batch` | Parallel-run reconciliation and cutover-gate scorecard — verifies the new system agrees with the old one before cutover |
| `sync-agent` | Db2-for-i → PostgreSQL data sync during the parallel-run window |

## Frontend

React 19 + TypeScript SPA (Vite, React Router 7, TanStack Query). Screens map to
the legacy interactive programs (Customer 360, Policies, Claims/FNOL, Billing,
Payments) plus one screen with no legacy equivalent at all: the **Batch Operations
Console**, which gives real visibility and on-demand control over the batch jobs
that used to run unattended on the IBM i job scheduler — including a live,
streamed console log when a job is triggered on demand.

RBAC roles enforced end-to-end (gateway → service → UI nav gating):
`CLAIMS_ADJUSTER`, `CLAIMS_SUPERVISOR`, `CSR`, `UNDERWRITER`, `FINANCE`, `COMPLIANCE`,
`BATCH_SVC`.

## Batch scheduling

The legacy IBM i job scheduler (`JOBSCHD1`–`JOBSCHD7`) is replaced by Kubernetes
CronJobs, mapped 1:1 in `ops/scheduler-map.yaml`:

| Job | Legacy program | Cadence |
|---|---|---|
| `audit-archive-job` | AUD002B | Daily |
| `billing-installment-job` | BIL003B | Daily |
| `claim-payment-job` | CLM006B | Daily |
| `commission-calc-job` | CMM001B | Daily |
| `premium-processing-job` | PRM005B | Daily |
| `policy-renewal-job` | POL006B | Monthly |

Every job also exposes structured exit codes (see `ops/README.md`) consumed by
alerting rules — the legacy programs had no equivalent signal beyond a job log
someone had to go read manually.

## Tech stack

- **Backend:** Java 21, Spring Boot 3.5, Spring Batch, Spring Cloud Gateway, PostgreSQL 17, Keycloak 26 (OIDC), Kafka (outbox relay), Redis
- **Frontend:** React 19, TypeScript, Vite, React Router 7, TanStack Query
- **Target deployment:** Kubernetes (Amazon EKS), Helm, ArgoCD

## Running locally

```bash
./scripts/run-local.sh
```

This brings up Postgres/Redis/Keycloak via Docker, builds and starts the domain
services, and starts the frontend dev server. Once up:

- UI: http://127.0.0.1:3001
- API Gateway: http://127.0.0.1:8081
- Keycloak: http://localhost:8180 (admin/admin)

Stop everything with `./scripts/stop-local.sh`.

## Legacy repository reference

`Property_Casualty_Insurance_System/` contains the original 8 COBOL programs, 22 DDS
panels, 2 CL job-control members, and the module design documents this modernization
was built from. `manifest/pcis-manifest.yaml` is the machine-readable inventory:

```bash
python3 manifest/generate_manifest.py   # regenerate
python3 manifest/validate_manifest.py   # validate
python3 manifest/tests/test_generator.py  # unit tests
```
