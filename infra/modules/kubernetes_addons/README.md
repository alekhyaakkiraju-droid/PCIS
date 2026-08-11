# PCIS Kubernetes Add-ons (WO-130)

Installs Istio (strict mTLS), Argo CD, default-deny NetworkPolicies, and Pod Security Standards (`restricted`) on `pcis-*` namespaces.

Consumed from environment roots after the EKS module is available. Helm chart versions are pinned via module variables for CVE scanning.
