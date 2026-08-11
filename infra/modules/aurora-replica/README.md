# PCIS Aurora Reporting Read Replica Module (WO-233)

Provisions the **Aurora PostgreSQL 17 reporting read replica** with reporting-tuned parameters, CloudWatch lag alarms, and a dedicated IRSA role for `reporting-svc`.

See `docs/runbooks/aurora-replica-lag.md` for lag breach remediation and deliberate no-fallback-to-primary policy.
