variable "environment_name" {
  description = "Environment identifier (dev, tst, or prd)."
  type        = string

  validation {
    condition     = contains(["dev", "tst", "prd"], var.environment_name)
    error_message = "environment_name must be one of: dev, tst, prd."
  }
}

variable "aws_region" {
  description = "AWS region for Secrets Manager secret shells and ALB annotations."
  type        = string
  default     = "us-east-1"
}

variable "namespace" {
  description = "Kubernetes namespace for the Keycloak release."
  type        = string
  default     = null
}

variable "release_name" {
  description = "Helm release name."
  type        = string
  default     = "keycloak"
}

variable "chart_version" {
  description = "Pinned bitnami/keycloak Helm chart version (ships Keycloak 26.x)."
  type        = string
  default     = "24.4.13"
}

variable "keycloak_image_tag" {
  description = "Keycloak container image tag (26.x)."
  type        = string
  default     = "26.0.2"

  validation {
    condition     = startswith(var.keycloak_image_tag, "26.")
    error_message = "keycloak_image_tag must be Keycloak 26.x."
  }
}

variable "replica_count" {
  description = "Number of Keycloak pods."
  type        = number
  default     = 1

  validation {
    condition     = var.replica_count >= 1 && var.replica_count <= 10
    error_message = "replica_count must be between 1 and 10."
  }
}

variable "resources" {
  description = "CPU/memory requests and limits for Keycloak pods."
  type = object({
    requests = object({
      cpu    = string
      memory = string
    })
    limits = object({
      cpu    = string
      memory = string
    })
  })
  default = {
    requests = {
      cpu    = "500m"
      memory = "1Gi"
    }
    limits = {
      cpu    = "2"
      memory = "2Gi"
    }
  }
}

variable "certificate_arn" {
  description = "ACM certificate ARN for HTTPS ingress (ALB annotation). Empty disables TLS annotation."
  type        = string
  default     = ""
}

variable "ingress_host" {
  description = "Hostname for the Keycloak ingress."
  type        = string
}

variable "ingress_class_name" {
  description = "Ingress class (e.g. alb, nginx)."
  type        = string
  default     = "alb"
}

variable "ingress_scheme" {
  description = "ALB scheme: internal or internet-facing."
  type        = string
  default     = "internal"

  validation {
    condition     = contains(["internal", "internet-facing"], var.ingress_scheme)
    error_message = "ingress_scheme must be internal or internet-facing."
  }
}

variable "db_host" {
  description = "PostgreSQL hostname for the Keycloak database (external; not embedded)."
  type        = string
}

variable "db_port" {
  description = "PostgreSQL port."
  type        = number
  default     = 5432
}

variable "db_name" {
  description = "Keycloak database name (separate from PCIS app DBs)."
  type        = string
  default     = "keycloak"
}

variable "db_username" {
  description = "Keycloak database username. Password comes from Secrets Manager (never in Terraform state values)."
  type        = string
  default     = "keycloak"
}

variable "admin_username" {
  description = "Keycloak admin username (non-secret). Password is sourced from Secrets Manager."
  type        = string
  default     = "pcis-kc-admin"
}

variable "admin_secret_name" {
  description = "Secrets Manager secret name for admin credentials (shell only; value set out-of-band)."
  type        = string
  default     = null
}

variable "db_secret_name" {
  description = "Secrets Manager secret name for DB password (shell only; value set out-of-band)."
  type        = string
  default     = null
}

variable "create_secret_shells" {
  description = "When true, create Secrets Manager secret shells (no values) for admin and DB credentials."
  type        = bool
  default     = true
}

variable "enable_realm_import" {
  description = "Mount realm-export.json ConfigMap and pass --import-realm on startup (dev/tst)."
  type        = bool
  default     = true
}

variable "realm_export_path" {
  description = "Path to realm-export.json relative to the Terraform root, or absolute. Defaults to the committed export next to this module."
  type        = string
  default     = null
}

variable "enable_ingress" {
  description = "Create an Ingress for Keycloak."
  type        = bool
  default     = true
}

variable "tags" {
  description = "Common tags applied to AWS resources."
  type        = map(string)
  default     = {}
}
