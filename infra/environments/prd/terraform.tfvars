# ─── Prd Environment — Terraform variable values ────────────────────────────

aws_region = "us-east-1"

# CIDR scheme: prd = 10.2.0.0/16 (non-overlapping with dev=10.0.x and tst=10.1.x)
vpc_cidr = "10.2.0.0/16"

# Three AZs for full multi-AZ high availability in production.
az_count = 3

# HA NAT — one NAT Gateway per AZ ensures private subnet egress survives a
# single-AZ failure without cross-AZ traffic (which incurs additional cost
# and latency, and exposes a partial-outage blast radius).
enable_ha_nat = true

# Full 90-day retention for security audit and compliance.
flow_log_retention_days = 90
