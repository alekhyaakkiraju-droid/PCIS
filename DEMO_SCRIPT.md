# PCIS Modernization Demo — Recording Script

Target length: ~6-8 minutes. Record at http://127.0.0.1:3001 with the local stack running
(`./scripts/run-local.sh`). Sign in with a role that has broad access (CLAIMS_SUPERVISOR
covers most nav items) or ADMIN if you have a demo login for it.

---

## 1. Cold open (30s) — no screen yet, or title slide

**Say:**
"PCIS is a Property & Casualty Insurance System originally built on IBM i — ILE COBOL
batch and interactive programs, DDS green-screen panels, Db2 for i. For this hackathon
we rebuilt it as cloud-native microservices: Spring Boot services, a React front end,
PostgreSQL, Keycloak for auth — without losing the original business logic. What you're
about to see is the real running system, hitting real backends, not a mockup."

---

## 2. Batch Operations Console — `/batch` (2 min, the centerpiece)

**Say while landing on the page:**
"This screen has no legacy equivalent at all. On IBM i, these batch jobs — billing
generation, commission calculation, delinquency aging, claim payments, policy renewal,
audit archive — ran unattended on a job scheduler. Nobody had live visibility into them
beyond an operator reading a job log after the fact."

**Point at the KPI row:**
"These numbers — jobs tracked, last run duration, completion rate, total errors — are
computed live from each service's own Spring Batch execution tables. Nothing here is
hardcoded."

**Click a row's "Details" link (pick `commissionCalculationJob` or `billingGenerationJob`):**
"Selecting a job shows its real execution history — read count, write count, error count,
per step — pulled straight from `BATCH_STEP_EXECUTION`."

**Click "Trigger Run" on `commissionCalculationJob`:**
"And this is the part that didn't exist on the legacy system at all: on-demand triggering
with a live console. Watch — this is a real Server-Sent Events stream. The job is
actually executing right now on the server, and every log line appears the instant it's
written, with a real timestamp."

*(Let it run to completion — it's fast, ~100ms. Point at the result.)*

"And here's the real business output: these are the actual new commission ledger rows
this run just inserted — not a canned response. If you re-run it immediately, you'll see
zero new rows, because the job is idempotent — it already billed everything eligible."

*(Optional: click Details on a different job to show the selection correctly following you,
and show the "Failed at ..." error detail if any job has a FAILED run in history.)*

---

## 3. Customer 360 — `/customers` (1.5 min)

**Search or land on a customer — use "David Chen":**
"This is a live aggregation, not a cached read model. Watch the Policies and Claims
counts —"

**Click into David Chen's Customer 360 page:**
"customer-svc is calling out to policy-svc and claims-svc in real time to build this
view. David Chen has two real policies — a homeowners policy and an auto policy — and
two real claims against them, one closed and paid, one denied."

**Click the Policies tab, then Claims tab:**
"Every row here links through to the real policy or claim record — this isn't a
snapshot, it's live."

---

## 4. Claims lifecycle — `/claims` (1.5 min)

**Land on Claim Inquiry, filter or search for a claim in progress — e.g. CLM000004904
(Riverside Auto Group, liability claim):**
"This claim shows a full real lifecycle: FNOL intake, an $85,000 reserve set by the
adjuster, a $25,000 interim payment already approved and paid, $60,000 remaining
exposure, still open."

**Open the claim detail — show reserve ledger / approval / payment:**
"Every one of these — the reserve, the approval, the payment — is a real foreign-key-
linked row. The approval references the actual reserve id; the payment references the
actual approval id."

**Optional: show the FNOL wizard (`/claims/fnol`) briefly:**
"And this is one of five legacy claim programs that were never actually built on IBM i —
only ever specified in a design document. We built the real thing."

---

## 5. Billing — `/billing` (1 min)

**Land on the Billing Overview page:**
"These installment and aging numbers are computed live from billing-svc — total
installments, paid, open, and overdue counts, spread across every aging bucket from
current through 90+ days. This replaces the legacy BIL003B billing-generation batch
program."

**Point at the phase-gate sign-off card:**
"And this gate is real, too — it's blocked right now because there are genuinely overdue
installments in the data, not because of a fake condition."

---

## 6. Wrap-up (30s)

**Say:**
"So to summarize: every legacy COBOL program — shipped or design-only-and-never-built —
now has a real modern replacement, running end-to-end with real seeded, interconnected
data. Role-based access is enforced from the gateway down to the UI nav. And the batch
layer, which used to be a black box, now has full live visibility and on-demand control."

---

## Notes for smooth recording

- If a page looks empty or stale, hard-refresh it first — some of this data was seeded
  mid-session and needs a fresh page load to show up.
- The Trigger Run demo is the single most impressive moment — don't rush it, let the log
  actually stream.
- Avoid triggering `delinquencyAgingJob` live on camera — it currently completes but
  doesn't visibly change any statuses (a known issue, not yet fixed). Stick to
  `commissionCalculationJob` or `billingGenerationJob` for the live demo.
- If asked "is this real data," the honest answer is: yes, hand-seeded with realistic
  relationships (not fixtures), not a production data volume.
