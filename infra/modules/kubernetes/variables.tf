variable "environment_name" {
  description = "Environment identifier (dev, tst, or prd)."
  type        = string

  validation {
    condition     = contains(["dev", "tst", "prd"], var.environment_name)
    error_message = "environment_name must be one of: dev, tst, prd."
  }
}

variable "cluster_name" {
  description = "EKS cluster name. Defaults to pcis-<environment>."
  type        = string
  default     = null
}

variable "kubernetes_version" {
  description = "EKS Kubernetes version (1.28+)."
  type        = string
  default     = "1.29"

  validation {
    condition     = tonumber(split(".", var.kubernetes_version)[0]) >= 1 && tonumber(split(".", var.kubernetes_version)[1]) >= 28
    error_message = "kubernetes_version must be 1.28 or higher."
  }
}

variable "vpc_id" {
  description = "VPC ID from the network module (WO-129)."
  type        = string
}

variable "private_subnet_ids" {
  description = "Private application subnet IDs for the control plane ENIs and node groups."
  type        = list(string)

  validation {
    condition     = length(var.private_subnet_ids) >= 2
    error_message = "At least two private subnet IDs are required."
  }
}

variable "cluster_endpoint_public_access" {
  description = "When false (default), API is private-only. Set true only for break-glass with CIDR restrictions."
  type        = bool
  default     = false
}

variable "cluster_endpoint_public_access_cidrs" {
  description = "CIDRs allowed to reach a public API endpoint when public access is enabled."
  type        = list(string)
  default     = []
}

variable "application_instance_types" {
  description = "Instance types for the application managed node group."
  type        = list(string)
  default     = ["m5.xlarge"]
}

variable "application_scaling" {
  description = "Min/max/desired for the application node group."
  type = object({
    min_size     = number
    max_size     = number
    desired_size = number
  })
}

variable "batch_instance_types" {
  description = "Instance types for the batch managed node group (scales to zero)."
  type        = list(string)
  default     = ["m5.2xlarge"]
}

variable "batch_scaling" {
  description = "Min/max/desired for the batch node group. min_size must be 0."
  type = object({
    min_size     = number
    max_size     = number
    desired_size = number
  })

  validation {
    condition     = var.batch_scaling.min_size == 0
    error_message = "batch_scaling.min_size must be 0 so idle batch capacity can scale to zero."
  }
}

variable "cloudwatch_log_retention_days" {
  description = "CloudWatch retention for EKS control plane logs."
  type        = number
  default     = 30
}

variable "tags" {
  description = "Common tags applied to AWS resources."
  type        = map(string)
  default     = {}
}
