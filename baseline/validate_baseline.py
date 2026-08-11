#!/usr/bin/env python3
"""
Validate cobol-baseline.yaml against manifest cobol entries (WO-002).

Every shipped COBOL source entry in the manifest (type: cobol, path ends
with .cbl, status: shipped) must have a corresponding baseline program
entry with non-empty sql_statements and commit_scope.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


REQUIRED_PROGRAM_KEYS = {
    "program_id",
    "module",
    "purpose",
    "manifest_path",
    "sql_statements",
    "commit_scope",
    "audit_call_parameters",
    "error_handling_paths",
    "working_storage_tunables",
    "prologue_contradictions",
}


def parse_manifest_shipped_cobol(text: str) -> list[str]:
    """Return program ids for shipped type:cobol .cbl entries."""
    ids: list[str] = []
    # Split on list items under files:
    for block in re.split(r"(?m)^  - path:", text):
        if "type: cobol" not in block:
            continue
        if "status: shipped" not in block and "status: missing-callee" in block:
            continue
        path_m = re.match(r"\s*(\S+)", block)
        if not path_m:
            continue
        path = path_m.group(1)
        if not path.endswith(".cbl"):
            continue
        # Prefer shipped; also accept any .cbl with checksum present
        if "status: shipped" in block or "sha256_checksum:" in block and "null" not in block.split("sha256_checksum:", 1)[1][:80]:
            ids.append(Path(path).stem)
        elif "status: missing-callee" not in block and path.endswith(".cbl"):
            # missing file entries still listed as cobol with path — skip if status missing
            if "status:" in block and "shipped" not in block:
                continue
            ids.append(Path(path).stem)
    return sorted(set(ids))


def extract_programs_section(baseline: str) -> str:
    if "programs:" not in baseline:
        return ""
    body = baseline.split("programs:", 1)[1]
    if "\nsummary:" in body:
        body = body.split("\nsummary:", 1)[0]
    return body


def parse_baseline_ids(programs_body: str) -> list[str]:
    ordered: list[str] = []
    seen = set()
    for m in re.finditer(r"(?m)^\s*- program_id:\s*([A-Z0-9]+)\s*$", programs_body):
        pid = m.group(1)
        if pid not in seen:
            seen.add(pid)
            ordered.append(pid)
    return ordered


def validate(baseline_path: Path, manifest_path: Path) -> list[str]:
    errors: list[str] = []
    if not baseline_path.is_file():
        return [f"Missing baseline file: {baseline_path}"]
    if not manifest_path.is_file():
        return [f"Missing manifest file: {manifest_path}"]

    baseline = baseline_path.read_text(encoding="utf-8")
    manifest = manifest_path.read_text(encoding="utf-8")
    programs_body = extract_programs_section(baseline)
    ordered = parse_baseline_ids(programs_body)
    manifest_ids = parse_manifest_shipped_cobol(manifest)

    if not ordered:
        errors.append("Baseline contains no program_id entries under programs:")
    if not manifest_ids:
        errors.append("Manifest contains no shipped type: cobol .cbl entries")

    missing = sorted(set(manifest_ids) - set(ordered))
    extra = sorted(set(ordered) - set(manifest_ids))
    if missing:
        errors.append(f"Baseline missing programs present in manifest: {missing}")
    if extra:
        errors.append(f"Baseline has programs not in shipped manifest cobol entries: {extra}")

    if len(ordered) != 8:
        errors.append(f"Expected exactly 8 COBOL program baselines, found {len(ordered)}")

    blocks = re.split(r"(?m)^\s*- program_id:", programs_body)
    for block in blocks[1:]:
        header_m = re.match(r"\s*([A-Z0-9]+)", block)
        if not header_m:
            continue
        pid = header_m.group(1)
        for key in REQUIRED_PROGRAM_KEYS:
            if key == "program_id":
                continue
            if not re.search(rf"(?m)^\s+{key}:", block):
                errors.append(f"{pid}: missing required key '{key}'")
        # non-empty sql_statements and commit_scope
        if re.search(r"(?m)^\s+sql_statements:\s*\[\]\s*$", block):
            errors.append(f"{pid}: sql_statements is empty")
        if not re.search(r"(?m)^\s+commit_scope:", block):
            errors.append(f"{pid}: commit_scope missing")
        else:
            # commit_scope should have nested keys
            if "commit_granularity" not in block:
                errors.append(f"{pid}: commit_scope missing commit_granularity")

    for needle, label in [
        ("CLM006B", "SECCHK01"),
        ("PRM005B", "PRMCLC01"),
        ("AUD002B", "AUDLOG01"),
    ]:
        if needle in ordered and label not in baseline:
            errors.append(f"Expected contradiction evidence mentioning {label} for {needle}")

    return errors


def main() -> int:
    ap = argparse.ArgumentParser(description="Validate COBOL baseline vs manifest")
    ap.add_argument("--baseline", default="baseline/cobol-baseline.yaml")
    ap.add_argument("--manifest", default="manifest/pcis-manifest.yaml")
    args = ap.parse_args()
    errors = validate(Path(args.baseline), Path(args.manifest))
    if errors:
        print("VALIDATION FAILED:")
        for e in errors:
            print(f"  - {e}")
        return 1
    print("Baseline validation passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
