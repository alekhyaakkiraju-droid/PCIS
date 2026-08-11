environment_name        = "dev"
vpc_cidr                = "10.0.0.0/16"
az_count                = 2
enable_ha_nat           = false
flow_log_retention_days = 90
aws_region              = "us-east-1"

kubernetes_version         = "1.29"
application_instance_types = ["m5.xlarge"]
application_scaling        = { min_size = 1, max_size = 3, desired_size = 1 }
batch_instance_types       = ["m5.2xlarge"]
batch_scaling              = { min_size = 0, max_size = 4, desired_size = 0 }
# Set true after the control plane exists so Helm/k8s providers can authenticate.
enable_kubernetes_addons = false
argocd_automated_sync    = true
