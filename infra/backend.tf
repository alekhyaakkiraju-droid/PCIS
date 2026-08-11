# Canonical remote backend contract for PCIS infrastructure (WO-129).
#
# S3 state storage + DynamoDB state locking, with separate state files per
# environment (see environments/*/backend.hcl).
#
# Environments currently use a local backend so `terraform validate` / `plan`
# work before the state bucket exists. To adopt remote state:
#
#   1. Ensure S3 bucket `pcis-terraform-state` (versioned, SSE) exists
#   2. Ensure DynamoDB table `pcis-terraform-locks` (LockID hash key) exists
#   3. Replace the local backend block in environments/<env>/versions.tf with:
#
#        backend "s3" {}
#
#   4. Run:
#        terraform init -migrate-state -backend-config=backend.hcl
#
# Per-environment keys:
#   dev/network/terraform.tfstate
#   tst/network/terraform.tfstate
#   prd/network/terraform.tfstate

terraform {
  backend "s3" {
    bucket         = "pcis-terraform-state"
    key            = "ENV/network/terraform.tfstate" # replace ENV via backend.hcl
    region         = "us-east-1"
    dynamodb_table = "pcis-terraform-locks"
    encrypt        = true
  }
}
