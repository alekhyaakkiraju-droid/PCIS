# Aurora reporting replica lag (WO-233)

## AuroraReplicaLagWarning

Trigger: `pcis_reporting_replica_lag_seconds` or CloudWatch `AuroraReplicaLag` above **60 seconds** for 2 minutes.

Remediation: verify reader instance health, check long-running writer transactions, pause large extracts until lag drops below 30s. **Do not** repoint reporting-svc to the writer.

## AuroraReplicaLagCritical

Trigger: lag above **300 seconds** for 1 minute.

Remediation: execute warning steps, page database on-call, consider manual reader replacement via Terraform.

## Failover policy

`reporting-svc` uses secret `pcis/{env}/aurora-reader` only. No automatic fallback to the OLTP primary. Manual promotion is break-glass only; expected RTO 15–30 minutes.
