bucket         = "pcis-terraform-state"
key            = "dev/network/terraform.tfstate"
region         = "us-east-1"
dynamodb_table = "pcis-terraform-locks"
encrypt        = true
