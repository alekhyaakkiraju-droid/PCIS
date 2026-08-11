# PCIS Grafana Dashboards-as-Code (WO-142)

Version-controlled Grafana dashboard JSON models and provisioning configuration for PCIS operations.

## Dashboards

| File | UID | Purpose |
|------|-----|---------|
| `dashboards/service-health.json` | `pcis-service-health` | Eight microservices: request/error rates, latency percentiles, HikariCP, pod CPU/memory |
| `dashboards/batch-jobs.json` | `pcis-batch-jobs` | Six Spring Batch CronJobs: status, duration trends, throughput, schedule |
| `dashboards/audit-health.json` | `pcis-audit-health` | Outbox pending/lag, relay throughput, archive job metrics |
| `dashboards/reconciliation.json` | `pcis-reconciliation` | Parallel-run diff counts, clean-day counter, gate status |

All dashboards declare `__inputs` for the Prometheus datasource (`${DS_PROMETHEUS}`) so they import cleanly across environments without hardcoded UIDs.

## Metric Sources (WO-141)

Panel queries reference WO-141 recording rules where available:

- `pcis:error_rate:5m`
- `pcis:api_request_duration:p95` / `pcis:api_request_duration:p99`
- `pcis:batch_job_duration:p95`

And standard Micrometer / Kubernetes metrics:

- `http_server_requests_seconds_*`
- `hikaricp_connections_active`
- `spring_batch_*`
- `pcis_audit_outbox_*`
- `pcis_reconciliation_*`
- `kube_*`

SLO threshold lines are rendered on latency and outbox lag panels (500ms read, 1000ms write, 30s audit lag, 1% error rate).

## Provisioning

Mount into Grafana:

- `provisioning/dashboards.yaml` — file provider (60s refresh)
- `provisioning/datasources.yaml` — Prometheus (`${PROMETHEUS_URL}`) and Loki (`${LOKI_URL}`)

Example Kubernetes ConfigMap layout:

```text
/etc/grafana/provisioning/dashboards/pcis/*.json
/etc/grafana/provisioning/dashboards/dashboards.yaml
/etc/grafana/provisioning/datasources/datasources.yaml
```

## Validation

```bash
./observability/grafana/lint-dashboards.sh
```

The lint script validates JSON syntax with `jq`, checks for `__inputs`, rejects hardcoded datasource UIDs, and optionally runs `grafana-dashboard-linter` when installed.

Reformat dashboards for consistent diffs:

```bash
jq --sort-keys . observability/grafana/dashboards/*.json
```

## Dependencies

- **WO-141** — Prometheus recording rules must be deployed for aggregated SLI series.
- **WO-130** — Micrometer instrumentation from `pcis-observability-starter`.

When metrics are absent (new service, pre-first-run batch job, outside parallel-run window), panels show **No data** rather than erroring.
