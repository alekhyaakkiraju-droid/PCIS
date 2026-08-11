#!/usr/bin/env python3
"""
OpenAPI contract diff gate (WO-221).

Compares committed snapshots under contracts/snapshots/ against generated OpenAPI
specs declared in contracts/snapshots/manifest.yaml. Exits non-zero when drift is
detected unless --dry-run is set.
"""

from __future__ import annotations

import argparse
import difflib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Any

try:
    import yaml
except ImportError:
    yaml = None  # type: ignore

REPO_ROOT = Path(__file__).resolve().parents[1]
MANIFEST_PATH = REPO_ROOT / "contracts" / "snapshots" / "manifest.yaml"


def load_manifest(path: Path) -> list[dict[str, Any]]:
    text = path.read_text(encoding="utf-8")
    if yaml is not None:
        data = yaml.safe_load(text)
        if not isinstance(data, dict):
            raise ValueError(f"{path}: expected mapping at root")
        apis = data.get("apis")
        if not isinstance(apis, list) or not apis:
            raise ValueError(f"{path}: expected non-empty apis list")
        return apis

    apis: list[dict[str, Any]] = []
    current: dict[str, Any] | None = None
    for raw in text.splitlines():
        line = raw.split("#", 1)[0].rstrip()
        if not line.strip():
            continue
        if re.match(r"^\s*-\s+id:\s*", line):
            if current:
                apis.append(current)
            current = {"id": line.split(":", 1)[1].strip()}
            continue
        if current is None:
            continue
        match = re.match(r"^\s+(snapshot|generated|generate_command|description):\s*(.*)$", line)
        if match:
            key, value = match.group(1), match.group(2).strip()
            if value.startswith('"') and value.endswith('"'):
                value = value[1:-1]
            current[key] = value
    if current:
        apis.append(current)
    if not apis:
        raise ValueError(f"{path}: no apis entries found")
    return apis


def canonicalize_yaml(path: Path) -> str:
    text = path.read_text(encoding="utf-8")
    if yaml is None:
        return "\n".join(line.rstrip() for line in text.splitlines()).strip() + "\n"

    data = yaml.safe_load(text)
    normalized = yaml.safe_dump(
        data,
        sort_keys=True,
        default_flow_style=False,
        allow_unicode=True,
        width=120,
    )
    return normalized.strip() + "\n"


def resolve_repo_path(relative: str) -> Path:
    path = (REPO_ROOT / relative).resolve()
    if not str(path).startswith(str(REPO_ROOT.resolve())):
        raise ValueError(f"Path escapes repository root: {relative}")
    return path


def materialize_generated(entry: dict[str, Any], work_dir: Path) -> Path:
    api_id = entry["id"]
    if "generate_command" in entry:
        command = entry["generate_command"]
        output = work_dir / f"{api_id}.yaml"
        env = {"GENERATED_PATH": str(output), "REPO_ROOT": str(REPO_ROOT)}
        subprocess.run(
            command,
            shell=True,
            check=True,
            cwd=REPO_ROOT,
            env={**os.environ, **env},
        )
        if not output.is_file():
            raise FileNotFoundError(
                f"{api_id}: generate_command did not produce {output}"
            )
        return output

    generated = entry.get("generated")
    if not generated:
        raise ValueError(f"{api_id}: manifest entry requires generated or generate_command")
    path = resolve_repo_path(generated)
    if not path.is_file():
        raise FileNotFoundError(f"{api_id}: generated spec missing at {path}")
    return path


def diff_text(left: str, right: str, left_label: str, right_label: str) -> str:
    return "".join(
        difflib.unified_diff(
            left.splitlines(keepends=True),
            right.splitlines(keepends=True),
            fromfile=left_label,
            tofile=right_label,
        )
    )


def relative_repo_path(path: Path) -> str:
    try:
        return str(path.resolve().relative_to(REPO_ROOT.resolve()))
    except ValueError:
        return str(path)


def check_api(
    entry: dict[str, Any],
    *,
    dry_run: bool,
    update_snapshots: bool,
    work_dir: Path,
) -> tuple[bool, str]:
    api_id = entry["id"]
    snapshot_rel = entry.get("snapshot")
    if not snapshot_rel:
        raise ValueError(f"{api_id}: snapshot path is required")

    snapshot_path = resolve_repo_path(snapshot_rel)
    generated_path = materialize_generated(entry, work_dir)

    if update_snapshots:
        snapshot_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(generated_path, snapshot_path)
        return True, f"{api_id}: snapshot updated at {snapshot_rel}"

    if not snapshot_path.is_file():
        return False, f"{api_id}: missing committed snapshot at {snapshot_rel}"

    snapshot_text = canonicalize_yaml(snapshot_path)
    generated_text = canonicalize_yaml(generated_path)
    if snapshot_text == generated_text:
        return True, f"{api_id}: snapshot matches generated spec"

    diff = diff_text(
        snapshot_text,
        generated_text,
        f"snapshot:{snapshot_rel}",
        f"generated:{relative_repo_path(generated_path)}",
    )
    message = (
        f"{api_id}: OpenAPI contract drift detected\n"
        f"  snapshot : {snapshot_rel}\n"
        f"  generated: {relative_repo_path(generated_path)}\n"
        f"\n{diff}\n"
        "If the contract change is intentional, refresh the snapshot:\n"
        "  bash scripts/openapi-contract-gate.sh --update-snapshots\n"
    )
    if dry_run:
        message = "[dry-run] would fail:\n" + message
        return True, message
    return False, message


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="OpenAPI contract diff gate (WO-221)")
    parser.add_argument(
        "--manifest",
        type=Path,
        default=MANIFEST_PATH,
        help="Path to contracts/snapshots/manifest.yaml",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Report drift without failing the process",
    )
    parser.add_argument(
        "--update-snapshots",
        action="store_true",
        help="Copy generated specs into committed snapshots",
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="Emit machine-readable results on stdout",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    manifest_path = args.manifest.resolve()
    if not manifest_path.is_file():
        print(f"ERROR: manifest not found: {manifest_path}", file=sys.stderr)
        return 2

    entries = load_manifest(manifest_path)
    results: list[dict[str, str | bool]] = []
    failed = False

    with tempfile.TemporaryDirectory(prefix="openapi-gate-") as tmp:
        work_dir = Path(tmp)
        for entry in entries:
            ok, message = check_api(
                entry,
                dry_run=args.dry_run,
                update_snapshots=args.update_snapshots,
                work_dir=work_dir,
            )
            results.append({"id": entry["id"], "ok": ok, "message": message})
            if not ok:
                failed = True

    if args.json:
        print(json.dumps({"results": results, "failed": failed}, indent=2))
    else:
        for result in results:
            prefix = "OK" if result["ok"] else "FAIL"
            print(f"==> {prefix}: {result['id']}")
            print(str(result["message"]).rstrip())
            print()

    if args.update_snapshots:
        return 0
    if failed and not args.dry_run:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
