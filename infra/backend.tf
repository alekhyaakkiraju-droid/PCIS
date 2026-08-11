# PCIS Terraform Remote Backend — Reference Configuration
#
# This file documents the S3 + DynamoDB backend pattern used by every PCIS
# environment.  The actual backend block lives in each environment composition
# directory (infra/environments/{dev,tst,prd}/backend.tf) so that state files
# are fully isolated per environment.
#
# Bootstrap Prerequisites (create once in the AWS account before first init):
#   S3 Bucket      : pcis-terraform-state-<ACCOUNT_ID>
#     - Versioning  : enabled
#     - Encryption  : AES-256
#     - Public access: blocked
#   DynamoDB Table : pcis-terraform-locks
#     - Partition key: LockID (String)
#     - Billing mode : PAY_PER_REQUEST
#
# Initialising an environment (run from infra/environments/{env}/):
#   terraform init \
#     -backend-config="bucket=pcis-terraform-state-<ACCOUNT_ID>" \
#     -backend-config="region=<AWS_REGION>"
#
# State key layout (prevents cross-environment blast radius):
#   dev : dev/network/terraform.tfstate
#   tst : tst/network/terraform.tfstate
#   prd : prd/network/terraform.tfstate
