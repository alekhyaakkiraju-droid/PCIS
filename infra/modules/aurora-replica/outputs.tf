output "reader_instance_id" {
  description = "Reporting read replica instance identifier."
  value       = aws_rds_cluster_instance.reader.id
}

output "reader_instance_arn" {
  description = "Reporting read replica instance ARN."
  value       = aws_rds_cluster_instance.reader.arn
}

output "reader_parameter_group_name" {
  description = "DB parameter group applied to the reporting reader."
  value       = aws_db_parameter_group.reporting.name
}

output "reporting_irsa_role_arn" {
  description = "IRSA role ARN for reporting-svc reader-only database access."
  value       = aws_iam_role.reporting_reader_irsa.arn
}

output "replica_lag_warning_alarm_name" {
  description = "CloudWatch alarm name for replica lag warning threshold."
  value       = aws_cloudwatch_metric_alarm.replica_lag_warning.alarm_name
}

output "replica_lag_critical_alarm_name" {
  description = "CloudWatch alarm name for replica lag critical threshold."
  value       = aws_cloudwatch_metric_alarm.replica_lag_critical.alarm_name
}
