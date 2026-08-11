locals {
  # DDL logging in prd; full statement logging in non-prod for diagnostics.
  log_statement = var.environment_name == "prd" ? "ddl" : "all"
}

resource "aws_rds_cluster_parameter_group" "aurora" {
  name_prefix = "pcis-aurora-${var.environment_name}-"
  family      = "aurora-postgresql17"
  description = "PCIS Aurora PostgreSQL 17 — TLS-only, UTC timezone"

  parameter {
    name         = "rds.force_ssl"
    value        = "1"
    apply_method = "immediate"
  }

  parameter {
    name         = "timezone"
    value        = "UTC"
    apply_method = "immediate"
  }

  parameter {
    name         = "log_statement"
    value        = local.log_statement
    apply_method = "immediate"
  }

  tags = merge(local.base_tags, {
    Name = "pcis-aurora-${var.environment_name}-cluster-pg"
  })

  lifecycle {
    create_before_destroy = true
  }
}
