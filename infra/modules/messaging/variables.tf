variable "environment_name" {
  description = "Environment identifier (dev, tst, or prd)."
  type        = string

  validation {
    condition     = contains(["dev", "tst", "prd"], var.environment_name)
    error_message = "environment_name must be one of: dev, tst, prd."
  }
}

variable "vpc_id" {
  description = "VPC ID for MSK security group."
  type        = string
}

variable "private_app_subnet_ids" {
  description = "Private application subnet IDs for MSK broker ENIs."
  type        = list(string)
}

variable "private_app_subnet_cidrs" {
  description = "Private application subnet CIDR blocks allowed to connect on port 9096."
  type        = list(string)
}

variable "broker_count" {
  description = "Number of MSK brokers (2 dev/tst, 3 prd)."
  type        = number

  validation {
    condition     = var.broker_count >= 2
    error_message = "broker_count must be at least 2."
  }
}

variable "broker_instance_type" {
  description = "MSK broker instance type."
  type        = string
  default     = "kafka.m5.large"
}

variable "kafka_version" {
  description = "Apache Kafka version for MSK."
  type        = string
  default     = "3.6.0"
}

variable "default_replication_factor" {
  description = "MSK default.replication.factor."
  type        = number
  default     = 2
}

variable "min_insync_replicas" {
  description = "MSK min.insync.replicas."
  type        = number
  default     = 1
}

variable "scram_secret_name" {
  description = "Secrets Manager secret name for SASL/SCRAM credentials (value set out-of-band)."
  type        = string
  default     = null
}

variable "log_retention_days" {
  description = "CloudWatch log retention for MSK broker logs."
  type        = number
  default     = 30
}

variable "s3_log_retention_days" {
  description = "S3 lifecycle expiration for MSK broker logs."
  type        = number
  default     = 90
}

variable "tags" {
  description = "Additional tags."
  type        = map(string)
  default     = {}
}
