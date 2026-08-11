# Audit Archive Batch (`auditArchiveJob`) — WO-213

**Legacy:** AUD002B | **Schedule:** ASSUMPTION `0 3 * * *`

## Exit codes

| Code | Meaning |
|------|---------|
| 0 | All partitions archived |
| 2 | **Archive verify mismatch** — partition detached, not dropped |
| 4 | Audit outbox write failure |
| 5 | Retention config below 365-day floor |

## Failure modes

- **Checksum mismatch (exit 2):** do not drop partition; compare export file vs detached table counts.
- **Retention floor violation:** fix tier config before re-run.
- **Legacy vacuous verify bug:** modern job uses per-partition checksums (WO-170).

## Restart

Restart resumes from last completed partition in job execution context.

## Escalation

Compliance + Platform on-call for exit 2; Security for retention misconfiguration.
