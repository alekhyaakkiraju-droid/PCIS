# ─── Dev Environment — Terraform variable values ────────────────────────────
# Replace aws_region with your target region before running terraform init.

aws_region = "us-east-1"

# CIDR scheme: dev = 10.0.0.0/16 (no overlap with tst=10.1.x or prd=10.2.x)
vpc_cidr = "10.0.0.0/16"

# Two AZs are sufficient for dev; saves on EIP and NAT Gateway costs.
az_count = 2

# Single NAT Gateway for dev — cost optimisation.
# All private subnets in both AZs route egress through this one gateway.
enable_ha_nat = false

# Shorter retention for dev to reduce S3 storage costs.
flow_log_retention_days = 30
