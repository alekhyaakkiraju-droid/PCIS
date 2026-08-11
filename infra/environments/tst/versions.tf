terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.13"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.29"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }

  # Local backend for day-to-day plan/validate.
  # Production remote state: copy infra/backend.tf pattern and
  # `terraform init -migrate-state -backend-config=backend.hcl`
  backend "local" {
    path = "terraform.tfstate"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "PCIS"
      ManagedBy   = "Terraform"
      Environment = "tst"
    }
  }
}

variable "aws_region" {
  description = "AWS region for the provider."
  type        = string
  default     = "us-east-1"
}
