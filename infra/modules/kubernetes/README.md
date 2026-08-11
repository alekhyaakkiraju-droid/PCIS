# PCIS Kubernetes Module (WO-130)

Provisions Amazon EKS (1.28+) with IRSA OIDC, dual managed node groups, and CloudWatch control-plane logging.

Mesh / GitOps / NetworkPolicy add-ons live in the sibling module `kubernetes_addons` (avoids provider cycles).

## Interface

| Input | Description |
|-------|-------------|
| `vpc_id` / `private_subnet_ids` | From network module (WO-129); private API by default |
| `application_scaling` | Always-on app nodes (prd min 2) |
| `batch_scaling` | Batch nodes with `min_size=0`, taint `batch=true:NoSchedule` |

| Output | Description |
|--------|-------------|
| `cluster_endpoint` / `cluster_ca_certificate` / `cluster_name` | kubectl + Helm provider wiring |
| `oidc_provider_arn` | IRSA for Aurora, S3, Secrets Manager, MSK |

## Scaling

- **Application** node group stays warm for APIs.
- **Batch** node group scales to zero when idle. Cluster Autoscaler labels are applied so a CronJob with `tolerations: batch=true:NoSchedule` can provision capacity within ~2 minutes.

## Two-phase apply

1. `enable_kubernetes_addons = false` — create EKS + node groups  
2. `enable_kubernetes_addons = true` — install Istio (strict mTLS), Argo CD, default-deny NetworkPolicies, PSS `restricted` on `pcis-*`

Istio chart version is pinned (`istio_version`) for CVE scanning. Argo CD automated sync is forced off for `prd`.
