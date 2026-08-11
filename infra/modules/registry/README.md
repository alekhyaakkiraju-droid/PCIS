# PCIS ECR Registry Module (WO-134)

Provisions Amazon ECR repositories for all eight PCIS microservices plus the shared
`pcis-base-java21` distroless base image.

## Features

- **Image scanning on push** — `scan_on_push = true` on every repository
- **EventBridge integration** — `ECR Image Scan` events routed to CloudWatch Logs
- **Tag immutability** — `image_tag_mutability = IMMUTABLE` (CI must use unique tags)
- **Lifecycle policies** — retain the last 30 tagged images; expire untagged after 7 days
- **Cross-account pull** — production account ID may pull from non-prod registries

## Repository naming

Repositories are named `pcis-{service}-{environment}`, e.g. `pcis-customer-svc-dev`.

## Cosign image signing

Cosign signing is **not** provisioned by Terraform. After CI pushes an image, run:

```bash
cosign sign --key awskms://alias/pcis-cosign "${ECR_URL}:sha-${GIT_SHA}"
```

ECR supports OCI artifact storage for signatures on the same repository.

## Usage

```hcl
module "registry" {
  source = "../../modules/registry"

  environment_name       = var.environment_name
  production_account_id  = var.production_account_id
  repository_names       = var.ecr_repository_names

  tags = {
    Project   = "PCIS"
    ManagedBy = "Terraform"
  }
}
```

## Outputs

| Output | Description |
|--------|-------------|
| `repository_urls` | Map of service name → ECR URL for Helm/Dockerfile |
| `repository_arns` | Map of service name → repository ARN |
| `scan_findings_event_rule_arn` | EventBridge rule for scan events |

## Edge cases

- **Name length** — service names are validated ≤ 200 chars before prefixing
- **Production rollback buffer** — 30-image retention provides rollback headroom
- **Critical CVEs** — pipeline (not Terraform) blocks deployment on scan failures
