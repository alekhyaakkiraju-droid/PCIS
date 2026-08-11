# PCIS Operations — Batch Scheduler Map (WO-003, WO-137)

## Overview

`scheduler-map.yaml` is the single authoritative reconciliation of legacy IBM i batch
schedulers (JOBSCHD1–7) into six Kubernetes CronJob definitions.

## Kubernetes CronJobs

| CronJob | COBOL Program | Legacy Drivers | Placeholder Schedule |
|---------|----------------|----------------|----------------------|
| `audit-archive-job` | AUD002B | JOBSCHD1 (month-end), JOBSCHD6 | `0 3 * * *` |
| `billing-installment-job` | BIL003B | JOBSCHD3, JOBSCHD5 | `0 1 * * *` |
| `claim-payment-job` | CLM006B | JOBSCHD1, JOBSCHD4 | `30 2 * * *` |
| `commission-calc-job` | CMM001B | JOBSCHD3, JOBSCHD5 | `0 4 * * *` |
| `premium-processing-job` | PRM005B | JOBSCHD1, JOBSCHD4 | `0 2 * * *` |
| `policy-renewal-job` | POL006B | JOBSCHD2, JOBSCHD7 | `0 5 1 * *` |

**All schedules are ASSUMPTION placeholders** pending Phase 0 batch-window baseline
measurements. Do not treat them as production-ready until baselines are recorded.

## Exit Code Contract

Spring Batch jobs exit with structured codes consumed by Alertmanager rules in
`helm/charts/pcis-batch/templates/prometheusrule.yaml`:

| Code | Meaning |
|------|---------|
| 0 | Success |
| 1 | Item-error threshold breached |
| 2 | Archive verify mismatch |
| 3 | Cursor-open failure |
| 4 | Audit-write failure |
| 5 | Config failure |

Kubernetes must **not** retry failed pods (`backoffLimit=0`, `restartPolicy=Never`).
Spring Batch handles checkpoint restart internally.

## Helm Chart

CronJob manifests live in `helm/charts/pcis-batch/`. Deploy with:

```bash
helm dependency update helm/charts/pcis-batch
helm upgrade --install pcis-batch helm/charts/pcis-batch \
  -f helm/charts/pcis-batch/values.yaml \
  -f helm/charts/pcis-batch/values-dev.yaml \
  --namespace pcis-batch-dev --create-namespace
```

## Validation

```bash
python3 ops/validate-scheduler-map.py
./ops/validate-scheduler-map.sh
python3 helm/tests/test_pcis_batch.py
```

## Batch Node Group

CronJob pods tolerate `batch=true:NoSchedule` and select `node-role=batch` nodes
that scale from zero. `startingDeadlineSeconds=300` accommodates node provisioning
(up to ~120s per architecture constraint).
