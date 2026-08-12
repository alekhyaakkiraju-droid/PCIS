# GitHub Secrets for PCIS Code-to-Cloud CI/CD

Configure these secrets in **Settings → Secrets and variables → Actions** for the PCIS repository.

| Secret | Required | Description |
|--------|----------|-------------|
| `AWS_ACCESS_KEY_ID` | Yes | AWS credentials with ECR push and EKS access |
| `AWS_SECRET_ACCESS_KEY` | Yes | AWS secret key |
| `GH_PAT` | Yes | GitHub PAT with `repo` scope for manifest commits and ArgoCD repo access |
| `SLACK_WEBHOOK_URL` | No | Optional deployment notifications |

## Quick Setup Configuration

| Setting | Value |
|---------|-------|
| Tenant | `opsera` |
| Application | `pcis` |
| Cloud | AWS |
| Region | `us-west-2` |
| Hub cluster | `argocd-usw2` |
| Spoke cluster | `opsera-usw2-np` |
| Namespace | `pcis-dev` |
| ArgoCD server | `argocd-usw2.agent.opsera.dev` |

## Deployment Order

1. Run **00 - Bootstrap: PCIS Infrastructure** (workflow_dispatch)
2. Ensure `pcis-platform-dev` is healthy (PostgreSQL, Redis, Keycloak, Kafka)
3. Deploy foundation services: `authz-svc`, `config-svc`
4. Deploy domain services (parallel OK)
5. Deploy `api-gateway` last

Reference pipeline: [Ai-Tutor admin-config CI/CD](https://github.com/gayathri-opsera/Ai-Tutor-Agent/actions/runs/29876892786)

## Manual Trigger

Each service has its own workflow under `.github/workflows/cicd-pcis-{service}-dev.yaml`. Use **Run workflow** in GitHub Actions for on-demand deploys.
