terraform {
  backend "s3" {
    # State key — unique per environment to guarantee isolation.
    key = "dev/network/terraform.tfstate"

    # Encryption at rest.
    encrypt = true

    # State locking via DynamoDB — prevents concurrent plan/apply conflicts.
    dynamodb_table = "pcis-terraform-locks"

    # bucket and region are injected at `terraform init` time:
    #   terraform init \
    #     -backend-config="bucket=pcis-terraform-state-<ACCOUNT_ID>" \
    #     -backend-config="region=<AWS_REGION>"
    #
    # This avoids hardcoding account IDs or region names in source control.
  }
}
