environment_name        = "prd"
vpc_cidr                = "10.2.0.0/16"
az_count                = 2
enable_ha_nat           = true
flow_log_retention_days = 90
aws_region              = "us-east-1"

kubernetes_version         = "1.29"
application_instance_types = ["m5.xlarge"]
application_scaling        = { min_size = 2, max_size = 6, desired_size = 2 }
batch_instance_types       = ["m5.2xlarge"]
batch_scaling              = { min_size = 0, max_size = 4, desired_size = 0 }
enable_kubernetes_addons   = false
argocd_automated_sync      = false
