resource "aws_db_parameter_group" "reporting" {
  name_prefix = "pcis-${var.environment_name}-aurora-reporting-"
  family      = "aurora-postgresql17"
  description = "PCIS Aurora reporting read replica — hot_standby_feedback, statement and idle-in-tx timeouts"

  parameter {
    name         = "hot_standby_feedback"
    value        = "1"
    apply_method = "immediate"
  }

  parameter {
    name         = "statement_timeout"
    value        = tostring(var.statement_timeout_ms)
    apply_method = "immediate"
  }

  parameter {
    name         = "idle_in_transaction_session_timeout"
    value        = tostring(var.idle_in_transaction_session_timeout_ms)
    apply_method = "immediate"
  }

  tags = merge(local.base_tags, {
    Name = "pcis-${var.environment_name}-aurora-reporting-pg"
  })

  lifecycle {
    create_before_destroy = true
  }
}
