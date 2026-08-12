# pcis-common

Helm **library** chart providing shared Kubernetes templates for PCIS microservices.

## Features

| Concern | Behavior |
|--------|----------|
| Security context | `runAsNonRoot`, `readOnlyRootFilesystem`, `allowPrivilegeEscalation=false`, drop `ALL` capabilities |
| Probes | Spring Actuator: `/actuator/health/liveness`, `/readiness` (startup probe uses readiness; Spring Boot does not expose `/startup`) |
| PDB | `minAvailable` from values (`prd` typically `2`, `dev` typically `1`) |
| HPA | CPU (optional memory) autoscaling when enabled |
| NetworkPolicy | Default-deny ingress; allow from `api-gateway` and configured peers |
| Istio | `sidecar.istio.io/inject: "true"` when `istio.inject` is true |
| Batch workloads | Set `autoscaling.enableHpa: false` / `podDisruptionBudget.enablePdb: false` to skip HPA/PDB |

## Usage

Depend on this chart from a service chart:

```yaml
# Chart.yaml
dependencies:
  - name: pcis-common
    version: "0.1.0"
    repository: "file://../pcis-common"
```

Include library templates:

```yaml
# templates/deployment.yaml
{{- include "pcis-common.deployment" . }}
```

Repeat for `service`, `serviceaccount`, `pdb`, `hpa`, `networkpolicy`, and `configmap`.

## Key values

```yaml
appName: customer-svc
replicaCount: 2
image:
  repository: ghcr.io/pcis/customer-svc
  tag: "1.0.0"
service:
  port: 80
  targetPort: 8080
serviceAccount:
  create: true
istio:
  inject: true
autoscaling:
  enabled: true
  enableHpa: true   # set false for batch
  minReplicas: 2
  maxReplicas: 10
podDisruptionBudget:
  enabled: true
  enablePdb: true   # set false for batch
  minAvailable: 1
networkPolicy:
  enabled: true
  allowedPeers: []
configMap:
  enabled: false
  data: {}
resources:
  requests:
    cpu: 100m
    memory: 256Mi
  limits:
    cpu: 500m
    memory: 512Mi
```

## Environment overlays

Service charts ship `values-dev.yaml` (`minAvailable: 1`) and `values-prd.yaml` (`minAvailable: 2`).

```bash
helm dependency update ./helm/charts/customer-svc
helm template customer ./helm/charts/customer-svc -f ./helm/charts/customer-svc/values-dev.yaml
```

## Validation

```bash
./helm/scripts/validate.sh
```
