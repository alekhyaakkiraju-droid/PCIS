terraform {
  backend "s3" {
    key            = "prd/network/terraform.tfstate"
    encrypt        = true
    dynamodb_table = "pcis-terraform-locks"
    # Provide bucket and region at init:
    #   terraform init \
    #     -backend-config="bucket=pcis-terraform-state-<ACCOUNT_ID>" \
    #     -backend-config="region=<AWS_REGION>"
  }
}
