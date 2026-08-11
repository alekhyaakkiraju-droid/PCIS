module "kubernetes" {
  source = "../../modules/kubernetes"

  environment_name   = var.environment_name
  vpc_id             = module.network.vpc_id
  private_subnet_ids = module.network.private_app_subnet_ids
  kubernetes_version  = var.kubernetes_version

  application_instance_types = var.application_instance_types
  application_scaling        = var.application_scaling
  batch_instance_types       = var.batch_instance_types
  batch_scaling              = var.batch_scaling

  tags = {
    Project   = "PCIS"
    ManagedBy = "Terraform"
  }
}

provider "kubernetes" {
  host                   = module.kubernetes.cluster_endpoint
  cluster_ca_certificate = base64decode(module.kubernetes.cluster_ca_certificate)

  exec {
    api_version = "client.authentication.k8s.io/v1beta1"
    command     = "aws"
    args = [
      "eks", "get-token",
      "--cluster-name", module.kubernetes.cluster_name,
      "--region", var.aws_region,
    ]
  }
}

provider "helm" {
  kubernetes {
    host                   = module.kubernetes.cluster_endpoint
    cluster_ca_certificate = base64decode(module.kubernetes.cluster_ca_certificate)

    exec {
      api_version = "client.authentication.k8s.io/v1beta1"
      command     = "aws"
      args = [
        "eks", "get-token",
        "--cluster-name", module.kubernetes.cluster_name,
        "--region", var.aws_region,
      ]
    }
  }
}

module "kubernetes_addons" {
  count  = var.enable_kubernetes_addons ? 1 : 0
  source = "../../modules/kubernetes_addons"

  environment_name      = var.environment_name
  istio_version         = var.istio_version
  argocd_version        = var.argocd_version
  argocd_automated_sync = var.argocd_automated_sync

  depends_on = [
    module.kubernetes,
  ]
}

variable "kubernetes_version" {
  type    = string
  default = "1.29"
}

variable "application_instance_types" {
  type    = list(string)
  default = ["m5.xlarge"]
}

variable "application_scaling" {
  type = object({
    min_size     = number
    max_size     = number
    desired_size = number
  })
}

variable "batch_instance_types" {
  type    = list(string)
  default = ["m5.2xlarge"]
}

variable "batch_scaling" {
  type = object({
    min_size     = number
    max_size     = number
    desired_size = number
  })
}

variable "enable_kubernetes_addons" {
  type    = bool
  default = true
}

variable "argocd_automated_sync" {
  type    = bool
  default = true
}

variable "istio_version" {
  type    = string
  default = "1.22.3"
}

variable "argocd_version" {
  type    = string
  default = "7.6.12"
}

output "eks_cluster_name" {
  value = module.kubernetes.cluster_name
}

output "eks_cluster_endpoint" {
  value = module.kubernetes.cluster_endpoint
}

output "eks_oidc_provider_arn" {
  value = module.kubernetes.oidc_provider_arn
}

output "argocd_server_url" {
  value = var.enable_kubernetes_addons ? "https://argocd-server.argocd.svc.cluster.local" : null
}
