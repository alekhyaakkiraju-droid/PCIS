# PCIS Observability — SLI Metric Catalog and Prometheus Rules (WO-141)

Version-controlled Prometheus recording and alerting rules for PCIS SLO monitoring.
Consumes Micrometer metrics from Spring Boot services (`http_server_requests_seconds`,
`spring_batch_job_seconds`, `hikaricp_connections_active`) and custom `pcis_*` gauges.

## Layout

| Path | Purpose |
|------|---------|
| `prometheus/recording-rules.yaml` | SLI recording rules (`pcis:*` aggregates) |
| `prometheus/alerting-rules.yaml` | SLO breach alerts |
| `prometheus/promtool-check.sh` | CI validation via `promtool check rules` |
| `test-fixtures/sample-metrics.txt` | Prometheus exposition samples (normal + breach) |
| `test/test-rules.sh` | Integration test (optional Docker) |
| `docker-compose.test.yaml` | Prometheus + pushgateway test stack |

## SLI Recording Rules

| Recording rule | Source metric | Labels |
|----------------|---------------|--------|
| `pcis:api_request_duration:p50/p95/p99` | `http_server_requests_seconds_bucket` | service, method, uri, status |
| `error_rate_5m` / `pcis:error_rate:5m` | `http_server_requests_seconds_count` (5xx / total) | service |
| `pcis:batch_job_duration:p95` | `spring_batch_job_seconds_bucket` | job_name |
| `pcis:batch_job_exit_code` | `pcis_batch_job_exit_code` | job_name, namespace, pod |
| `pcis:audit_outbox_lag_seconds` | `pcis_audit_outbox_lag_seconds` | service, namespace, pod |
| `pcis:audit_outbox_pending_count` | `pcis_audit_outbox_pending_count` | service, namespace, pod |
| `pcis:hikaricp_connections_active` | `hikaricp_connections_active` | service, namespace, pod |
| `pcis:batch_window:utilization_ratio` | duration / `pcis_batch_window_seconds` | job_name |

**ASSUMPTION:** `pcis_batch_window_seconds` values are placeholders until Phase 0 batch-window
baseline measurement completes. Update gauge values without changing rule structure.

## Alert Catalog

| Alert | Severity | Threshold | For | Runbook | Team |
|-------|----------|-----------|-----|---------|------|
| ApiReadLatencyHigh | warning | p95 > 500ms (GET/HEAD) | 5m | `docs/runbooks/api-latency.md#api-read-latency-high` | platform |
| ApiWriteLatencyHigh | warning | p95 > 1000ms (POST/PUT/PATCH/DELETE) | 5m | `docs/runbooks/api-latency.md#api-write-latency-high` | platform |
| ErrorRateHigh | critical | 5xx rate > 1% | 5m | `docs/runbooks/error-rate-high.md` | platform |
| BatchJobFailed | critical | exit code ≠ 0 or `kube_job_status_failed` | 0m | `docs/runbooks/batch-job-failure.md` | platform |
| BatchWindowBreached | warning | duration > 75% of window | 5m | `docs/runbooks/batch-window-overrun.md` | platform |
| AuditOutboxLagHigh | critical | lag > 30s | 2m | `docs/runbooks/audit-outbox-backlog.md#lag-high` | platform |
| AuditOutboxBacklog | warning | pending > 100 | 5m | `docs/runbooks/audit-outbox-backlog.md#backlog` | platform |
| CertificateExpirySoon | warning | expiry < 14 days | 1h | `docs/runbooks/certificate-expiry.md` | platform |
| SecretRotationOverdue | info | last rotation > 90 days | 1h | `docs/runbooks/secret-rotation.md` | platform |

### Alert routing

- **critical** → PagerDuty (`BatchJobFailed`, `AuditOutboxLagHigh`, `ErrorRateHigh`)
- **warning** → Slack (`ApiReadLatencyHigh`, `BatchWindowBreached`, `CertificateExpirySoon`, …)
- **info** → Grafana annotation only (`SecretRotationOverdue`)

## Validation

```bash
# Primary CI gate (requires promtool)
make lint-prometheus

# Metadata + optional Docker integration test
bash observability/test/test-rules.sh

# Full integration stack
docker compose -f observability/docker-compose.test.yaml up --abort-on-container-exit
```

If Docker is unavailable, `test-rules.sh` skips integration and relies on `promtool` validation.

## Dependencies

- WO-130 (observability starter — `service` common tag)
- Phase 0 baseline (batch window gauge values — ASSUMPTION placeholders)
- WO-142 (Grafana dashboards consume these recording rules)
