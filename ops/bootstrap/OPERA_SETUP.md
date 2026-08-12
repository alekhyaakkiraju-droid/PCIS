# Opsera: create pcis-platform-dev-pipeline

After this commit is on `main`, create the bootstrap pipeline in the Opsera portal:

## Option A — Duplicate api-gateway pipeline (fastest)

1. Open **pcis-api-gateway-dev-pipeline** → **Duplicate**
2. Rename to **`pcis-platform-dev-pipeline`**
3. Replace workflow script with `ops/bootstrap/opsera-platform-pipeline.sh`
4. Remove **build** and **push** stages (keep **clone** + **deploy** only)
5. Update variables:

| Variable | Value |
|----------|-------|
| `REPO_URL` | `https://github.com/alekhyaakkiraju-droid/PCIS.git` |
| `BRANCH` | `main` |
| `APP_NAME` | `pcis-platform` |
| `AWS_REGION` | `us-west-2` |
| `AWS_ACCOUNT_ID` | `792373136340` |
| `EKS_CLUSTER` | `opsera-usw2-np` |
| `DEPLOY_NAMESPACE` | `pcis-dev` |
| `HELM_RELEASE` | `pcis-platform` |
| `CHART_PATH` | `helm/charts/pcis-platform` |

6. Reuse the same secrets as api-gateway: `GIT_PAT`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
7. **Save** → **Run once** before any microservice deploy

## Option B — Manual / Argo CD

```bash
./helm/bootstrap/install-dev-platform.sh
# or
kubectl apply -f argocd/applications/pcis-platform-dev.yaml
```

## Run order

1. **pcis-platform-dev-pipeline** (this doc) — sync-wave 0
2. **Argo CD** ApplicationSets — sync-wave 1–2
3. **pcis-api-gateway-dev-pipeline** — build/push only (or full deploy if not using Argo CD)
