# ─── Tst Environment — Terraform variable values ────────────────────────────

aws_region = "us-east-1"

# CIDR scheme: tst = 10.1.0.0/16 (non-overlapping with dev=10.0.x and prd=10.2.x)
vpc_cidr = "10.1.0.0/16"

# Two AZs match dev; mirrors production topology without the HA NAT cost.
az_count = 2

# Single NAT Gateway for tst — mirrors dev cost posture.
enable_ha_nat = false

# Moderate retention for compliance testing.
flow_log_retention_days = 60
