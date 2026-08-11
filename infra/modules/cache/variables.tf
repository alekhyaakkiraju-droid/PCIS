variable "environment_name" {
  description = "Environment identifier (dev, tst, or prd)."
  type        = string

  validation {
    condition     = contains(["dev", "tst", "prd"], var.environment_name)
    error_message = "environment_name must be one of: dev, tst, prd."
  }
}

variable "vpc_id" {
  description = "VPC ID for Redis security group."
  type        = string
}

variable "private_app_subnet_ids" {
  description = "Private application subnet IDs for Redis."
  type        = list(string)
}

variable "private_app_subnet_cidrs" {
  description = "Private application subnet CIDR blocks allowed on port 6379."
  type        = list(string)
}

variable "node_type" {
  description = "ElastiCache node type."
  type        = string
  default     = "cache.t3.medium"
}

variable "engine_version" {
  description = "Redis engine version."
  type        = string
  default     = "7.1"
}

variable "cluster_mode_enabled" {
  description = "Enable Redis cluster mode (required in prd)."
  type        = bool
  default     = false
}

variable "num_node_groups" {
  description = "Number of shards when cluster mode is enabled."
  type        = number
  default     = 2
}

variable "replicas_per_node_group" {
  description = "Replica count per shard in cluster mode."
  type        = number
  default     = 2
}

variable "auth_secret_name" {
  description = "Secrets Manager secret name for Redis AUTH token (value set out-of-band)."
  type        = string
  default     = null
}

variable "tags" {
  description = "Additional tags."
  type        = map(string)
  default     = {}
}
