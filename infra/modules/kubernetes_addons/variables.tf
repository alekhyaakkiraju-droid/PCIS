variable "environment_name" {
  type = string
}

variable "istio_version" {
  type    = string
  default = "1.22.3"
}

variable "argocd_version" {
  type    = string
  default = "7.6.12"
}

variable "argocd_automated_sync" {
  description = "Enable Argo CD automated sync indicators. Forced off when environment_name is prd."
  type        = bool
  default     = false
}
