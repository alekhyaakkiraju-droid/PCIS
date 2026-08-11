#!/usr/bin/env bash
# Terraform fmt + validate for infra/modules/keycloak (WO-145).
# Uses a temporary root with stub providers so the reusable module stays provider-free
# for wiring from infra/environments/{dev,tst,prd}.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KEYCLOAK_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
MODULE_DIR="$(cd "${KEYCLOAK_DIR}/../modules/keycloak" && pwd)"

if ! command -v terraform >/dev/null 2>&1; then
  echo "SKIP: terraform not installed"
  exit 0
fi

echo "==> terraform fmt -check (module)"
terraform fmt -check -recursive "${MODULE_DIR}"

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/pcis-keycloak-tfvalidate.XXXXXX")"
cleanup() { rm -rf "${TMP_DIR}"; }
trap cleanup EXIT

cat > "${TMP_DIR}/versions.tf" <<'EOF'
terraform {
  required_version = ">= 1.5.0"
  required_providers {
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.13"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.29"
    }
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.0"
    }
  }
}

provider "helm" {}
provider "kubernetes" {}
provider "aws" {
  region                      = "us-east-1"
  skip_credentials_validation = true
  skip_requesting_account_id  = true
  skip_metadata_api_check     = true
  skip_region_validation      = true
  access_key                  = "mock"
  secret_key                  = "mock"
}
EOF

cat > "${TMP_DIR}/main.tf" <<EOF
module "keycloak" {
  source = "${MODULE_DIR}"

  environment_name       = "dev"
  ingress_host           = "auth.dev.pcis.example.com"
  certificate_arn        = "arn:aws:acm:us-east-1:000000000000:certificate/example"
  db_host                = "keycloak.db.example.internal"
  enable_realm_import    = true
  create_secret_shells   = true
  replica_count          = 1
}
EOF

echo "==> terraform init -backend=false (temp root wrapping module)"
terraform -chdir="${TMP_DIR}" init -backend=false -input=false >/dev/null

echo "==> terraform validate"
terraform -chdir="${TMP_DIR}" validate

# Also satisfy the documented module-path check when possible:
# init/validate directly in the module requires local provider stubs; we keep the
# module reusable and validate via the wrapper above.
echo "OK: Keycloak module fmt + validate passed"
