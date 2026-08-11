#!/usr/bin/env python3
"""
Validate Argo CD GitOps manifests against helm/charts/ (WO-139).

Runs without the argocd CLI — checks YAML structure, ApplicationSet matrix
coverage, Helm chart paths, value overlays, sync policies, and production
approval annotations.

Exits 0 when all checks pass, non-zero otherwise.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

try:
    import yaml
except ImportError:
    yaml = None  # type: ignore

REPO_ROOT = Path(__file__).resolve().parents[1]
ARGOCD_DIR = REPO_ROOT / "argocd"
HELM_CHARTS = REPO_ROOT / "helm" / "charts"

EXPECTED_SERVICES = frozenset(
    {
        "api-gateway",
        "authz-svc",
        "audit-svc",
        "customer-svc",
        "claims-svc",
        "policy-svc",
        "premium-svc",
        "billing-svc",
    }
)
EXPECTED_ENVS = ("dev", "tst", "prd")
ENV_VALUES = {
    "dev": "values-dev.yaml",
    "tst": "values-dev.yaml",  # tst reuses dev-tier overlays until values-tst.yaml lands
    "prd": "values-prd.yaml",
}
REQUIRED_FILES = (
    "argocd/applicationsets/pcis-services.yaml",
    "argocd/applicationsets/pcis-batch.yaml",
    "argocd/projects/pcis-project.yaml",
    "argocd/secrets/repo-credentials.yaml",
    "argocd/notifications/config.yaml",
    "argocd/docs/production-approval-gate.md",
    "ops/runbooks/rollback.md",
)


def load_yaml(path: Path) -> dict:
    if yaml is not None:
        with path.open(encoding="utf-8") as fh:
            data = yaml.safe_load(fh)
        if not isinstance(data, dict):
            raise ValueError(f"{path}: expected mapping at root")
        return data

    # Minimal fallback when PyYAML is unavailable
    text = path.read_text(encoding="utf-8")
    if "apiVersion:" not in text or "kind:" not in text:
        raise ValueError(f"{path}: missing apiVersion/kind")
    return {"_raw": text}


def extract_services_from_applicationset(data: dict) -> set[str]:
    services: set[str] = set()
    if "_raw" in data:
        text = data["_raw"]
        for match in re.finditer(r"^\s*-\s*service:\s*(\S+)\s*$", text, re.MULTILINE):
            services.add(match.group(1))
        return services

    generators = data.get("spec", {}).get("generators", [])
    for gen in generators:
        matrix = gen.get("matrix", {}).get("generators", [])
        for sub in matrix:
            elements = sub.get("list", {}).get("elements", [])
            for el in elements:
                if "service" in el:
                    services.add(el["service"])
    return services


def extract_env_automation(data: dict) -> dict[str, str]:
    """Return env -> automated flag from list generator elements."""
    mapping: dict[str, str] = {}
    if "_raw" in data:
        text = data["_raw"]
        current_env = None
        for line in text.splitlines():
            env_match = re.match(r"^\s*-\s*env:\s*(\S+)\s*$", line)
            if env_match:
                current_env = env_match.group(1)
                continue
            auto_match = re.match(r"^\s*automated:\s*\"?(true|false)\"?\s*$", line)
            if auto_match and current_env:
                mapping[current_env] = auto_match.group(1)
        return mapping

    def walk(generators: list) -> None:
        for gen in generators:
            if "list" in gen:
                for el in gen["list"].get("elements", []):
                    if "env" in el and "automated" in el:
                        mapping[el["env"]] = str(el["automated"]).lower()
            if "matrix" in gen:
                for sub in gen["matrix"].get("generators", []):
                    walk([sub])

    walk(data.get("spec", {}).get("generators", []))
    return mapping


def check_revision_history_limit(path: Path, data: dict, errors: list[str]) -> None:
    if "_raw" in data:
        if "revisionHistoryLimit: 5" not in data["_raw"]:
            errors.append(f"{path}: missing revisionHistoryLimit: 5")
        return
    template = data.get("spec", {}).get("template", {})
    spec = template.get("spec", {})
    limit = spec.get("revisionHistoryLimit")
    if limit != 5:
        errors.append(f"{path}: revisionHistoryLimit must be 5 (got {limit!r})")


def check_sync_policies(path: Path, data: dict, errors: list[str]) -> None:
    text = data.get("_raw") or path.read_text(encoding="utf-8")
    if 'automated: "true"' not in text and "automated: true" not in text:
        errors.append(f"{path}: dev automated sync not configured")
    if "selfHeal: true" not in text or "prune: true" not in text:
        errors.append(f"{path}: dev syncPolicy must enable selfHeal and prune")
    if "{{- if eq .automated \"true\" }}" not in text:
        errors.append(f"{path}: conditional automated sync template missing")


def check_prd_approval_annotations(path: Path, data: dict, errors: list[str]) -> None:
    text = data.get("_raw") or path.read_text(encoding="utf-8")
    required = (
        'pcis.governance/approval-required: "true"',
        "pcis.governance/approver-group: pcis-admins",
        'pcis.governance/change-ticket-required: "true"',
    )
    for ann in required:
        if ann not in text:
            errors.append(f"{path}: missing production annotation {ann}")


def check_helm_chart(service: str, env: str, errors: list[str]) -> None:
    chart_dir = HELM_CHARTS / service
    if not chart_dir.is_dir():
        errors.append(f"helm/charts/{service}: chart directory missing")
        return
    chart_yaml = chart_dir / "Chart.yaml"
    if not chart_yaml.is_file():
        errors.append(f"helm/charts/{service}/Chart.yaml: missing")
        return
    values_base = chart_dir / "values.yaml"
    values_env = chart_dir / ENV_VALUES[env]
    if not values_base.is_file():
        errors.append(f"helm/charts/{service}/values.yaml: missing")
    if not values_env.is_file():
        errors.append(
            f"helm/charts/{service}/{ENV_VALUES[env]}: missing (env={env})"
        )


def _field_from_raw(data: dict, path: Path, field: str) -> str | None:
    if "_raw" not in data:
        return None
    match = re.search(rf"^{field}:\s*(\S+)\s*$", data["_raw"], re.MULTILINE)
    return match.group(1) if match else None


def check_project(data: dict, path: Path, errors: list[str]) -> None:
    kind = data.get("kind") or _field_from_raw(data, path, "kind")
    if kind != "AppProject":
        errors.append(f"{path}: expected kind AppProject")
        return
    if "_raw" in data:
        text = data["_raw"]
        if re.search(r"^\s*name:\s*pcis\s*$", text, re.MULTILINE) is None:
            errors.append(f"{path}: AppProject name must be pcis")
        for ns in ("pcis-dev", "pcis-tst", "pcis-prd"):
            if f"namespace: {ns}" not in text:
                errors.append(f"{path}: missing destination namespace {ns}")
        return
    if data.get("metadata", {}).get("name") != "pcis":
        errors.append(f"{path}: AppProject name must be pcis")
    dests = data.get("spec", {}).get("destinations", [])
    namespaces = {d.get("namespace") for d in dests}
    for ns in ("pcis-dev", "pcis-tst", "pcis-prd"):
        if ns not in namespaces:
            errors.append(f"{path}: missing destination namespace {ns}")


def check_repo_credentials(data: dict, path: Path, errors: list[str]) -> None:
    kind = data.get("kind") or _field_from_raw(data, path, "kind")
    if kind != "ExternalSecret":
        errors.append(f"{path}: expected ExternalSecret placeholder")
        return
    if "_raw" in data:
        if "external-secrets.io/placeholder" not in data["_raw"]:
            errors.append(f"{path}: missing ESO placeholder annotation")
        return
    meta = data.get("metadata", {})
    if "external-secrets.io/placeholder" not in meta.get("annotations", {}):
        errors.append(f"{path}: missing ESO placeholder annotation")


def validate(repo_root: Path) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    notes: list[str] = []

    for rel in REQUIRED_FILES:
        if not (repo_root / rel).is_file():
            errors.append(f"required file missing: {rel}")

    services_path = repo_root / "argocd/applicationsets/pcis-services.yaml"
    batch_path = repo_root / "argocd/applicationsets/pcis-batch.yaml"
    project_path = repo_root / "argocd/projects/pcis-project.yaml"
    secrets_path = repo_root / "argocd/secrets/repo-credentials.yaml"

    services_data = load_yaml(services_path)
    batch_data = load_yaml(batch_path)
    project_data = load_yaml(project_path)
    secrets_data = load_yaml(secrets_path)

    found_services = extract_services_from_applicationset(services_data)
    if found_services != EXPECTED_SERVICES:
        missing = EXPECTED_SERVICES - found_services
        extra = found_services - EXPECTED_SERVICES
        if missing:
            errors.append(f"pcis-services.yaml: missing services {sorted(missing)}")
        if extra:
            errors.append(f"pcis-services.yaml: unexpected services {sorted(extra)}")

    for env in EXPECTED_ENVS:
        for service in EXPECTED_SERVICES:
            check_helm_chart(service, env, errors)

    check_helm_chart("pcis-batch", "dev", errors)
    check_helm_chart("pcis-batch", "tst", errors)
    check_helm_chart("pcis-batch", "prd", errors)

    svc_env_auto = extract_env_automation(services_data)
    batch_env_auto = extract_env_automation(batch_data)
    for env in EXPECTED_ENVS:
        for label, mapping in (("services", svc_env_auto), ("batch", batch_env_auto)):
            if env not in mapping:
                errors.append(f"pcis-{label}: missing env {env} in generator")
                continue
            expected = "true" if env == "dev" else "false"
            if mapping[env] != expected:
                errors.append(
                    f"pcis-{label}: env {env} automated={mapping[env]!r}, expected {expected!r}"
                )

    for path, data in (
        (services_path, services_data),
        (batch_path, batch_data),
    ):
        check_revision_history_limit(path, data, errors)
        check_sync_policies(path, data, errors)
        check_prd_approval_annotations(path, data, errors)

    check_project(project_data, project_path, errors)
    check_repo_credentials(secrets_data, secrets_path, errors)

    app_count = len(EXPECTED_SERVICES) * len(EXPECTED_ENVS)
    notes.append(f"ApplicationSet matrix: {len(EXPECTED_SERVICES)} services × {len(EXPECTED_ENVS)} envs = {app_count} apps")
    notes.append(f"Batch ApplicationSet: {len(EXPECTED_ENVS)} apps")
    notes.append("dev: automated selfHeal+prune; tst/prd: manual sync")
    notes.append("revisionHistoryLimit: 5 on all generated Applications")
    if (repo_root / "argocd/docs/production-approval-gate.md").is_file():
        notes.append("Production approval gate documented")

    return errors, notes


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate Argo CD manifests (WO-139)")
    parser.add_argument(
        "--repo-root",
        type=Path,
        default=REPO_ROOT,
        help="Repository root (default: parent of ops/)",
    )
    args = parser.parse_args()

    print("WO-139 Argo CD manifest validation")
    print(f"Repo: {args.repo_root.resolve()}")
    print()

    errors, notes = validate(args.repo_root)

    for note in notes:
        print(f"  OK  {note}")

    if errors:
        print()
        print(f"FAILED — {len(errors)} issue(s):")
        for err in errors:
            print(f"  ✗ {err}")
        return 1

    print()
    print("PASSED — all Argo CD manifest checks succeeded")
    return 0


if __name__ == "__main__":
    sys.exit(main())
