# PCIS Object Storage Module (WO-133)

Provisions S3 buckets for **immutable audit archives** (Object Lock compliance mode), **VPC flow logs**, and **Terraform state**, encrypted with a dedicated CMK (`alias/pcis-s3-audit-{env}`).

## Buckets

| Bucket | Object Lock | Lifecycle |
|--------|-------------|-----------|
| `pcis-audit-archive-{env}-{account}` | COMPLIANCE (default retention configurable, usually 365d) | Glacier @ 90d → Deep Archive @ 365d; noncurrent expire @ 30d |
| `pcis-flow-logs-{env}-{account}` | none | Expire @ 90d |
| `pcis-terraform-state-{env}-{account}` | none | Noncurrent expire @ 90d |

## Access

- Public access blocked on all buckets.
- Audit `PutObject` denied unless principal is `audit_svc_role_arn`.
- Compliance Object Lock blocks delete/overwrite before retention expiry (even for bucket owner).

## Cryptographic erasure

After Object Lock retention expires, schedule deletion of `alias/pcis-s3-audit-{env}` (deletion window ≥ 7 days). Objects become unreadable once the key is destroyed — document the change ticket before scheduling.
