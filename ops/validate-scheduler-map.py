#!/usr/bin/env python3
"""
Validate ops/scheduler-map.yaml against manifest/pcis-manifest.yaml (WO-003).

Checks:
  1. Every expected batch program has a scheduler-map entry.
  2. Every scheduler-map batch_programs key references a valid shipped COBOL
     program in the repository manifest.
  3. Required fields exist per program (legacy_scheduler_entries, schedules,
     target_cronjob_name, naming_notes).
  4. Target schedules are annotated as ASSUMPTION placeholders.
  5. JOBSCHD1-3 entries are source_status referenced-only; JOBSCHD4-7 in-repo.
  6. PRM005B resolves naming drift to premiumProcessingJob.

Exits 0 on success, non-zero on any discrepancy.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

EXPECTED_BATCH_PROGRAMS = frozenset(
    {"AUD002B", "BIL003B", "CLM006B", "CMM001B", "POL006B", "PRM005B"}
)
REQUIRED_FIELDS = (
    "legacy_scheduler_entries",
    "legacy_schedule",
    "target_cronjob_name",
    "target_schedule",
    "naming_notes",
)


def _parse_scalar(raw: str):
    raw = raw.strip()
    if raw in ("null", "~", ""):
        return None
    if raw in ("true", "false"):
        return raw == "true"
    if raw == "[]":
        return []
    if (raw.startswith('"') and raw.endswith('"')) or (
        raw.startswith("'") and raw.endswith("'")
    ):
        return raw[1:-1]
    if raw.startswith("[") and raw.endswith("]"):
        inner = raw[1:-1].strip()
        if not inner:
            return []
        return [p.strip().strip("'\"") for p in inner.split(",")]
    try:
        return int(raw)
    except ValueError:
        return raw


def load_simple_yaml(text: str) -> dict:
    """Minimal indentation-based YAML loader for scheduler-map.yaml."""
    lines = []
    for line in text.splitlines():
        if line.strip().startswith("#") or line.strip() in ("", "---"):
            continue
        lines.append(line)

    root: dict = {}
    stack: list[tuple[int, object]] = [(-1, root)]

    def current_container():
        return stack[-1][1]

    i = 0
    while i < len(lines):
        line = lines[i]
        indent = len(line) - len(line.lstrip(" "))
        content = line.strip()

        while len(stack) > 1 and indent <= stack[-1][0]:
            stack.pop()

        parent = current_container()

        if content.startswith("- "):
            item_body = content[2:].strip()
            if not isinstance(parent, list):
                raise ValueError(f"List item without list parent: {line}")
            if ": " in item_body or item_body.endswith(":"):
                key, _, rest = item_body.partition(":")
                key = key.strip()
                rest = rest.strip()
                item: dict = {}
                parent.append(item)
                stack.append((indent, item))
                if rest:
                    if rest in (">", "|"):
                        # folded/literal block — collect subsequent indented lines
                        block_lines = []
                        j = i + 1
                        while j < len(lines):
                            nxt = lines[j]
                            nindent = len(nxt) - len(nxt.lstrip(" "))
                            if nindent <= indent:
                                break
                            block_lines.append(nxt.strip())
                            j += 1
                        item[key] = " ".join(block_lines).strip()
                        i = j - 1
                    else:
                        item[key] = _parse_scalar(rest)
                else:
                    # nested mapping under list item key with empty value → skip
                    pass
            else:
                parent.append(_parse_scalar(item_body))
            i += 1
            continue

        if ":" in content:
            key, _, rest = content.partition(":")
            key = key.strip()
            rest = rest.strip()
            if not isinstance(parent, dict):
                raise ValueError(f"Mapping entry without dict parent: {line}")

            if rest in (">", "|"):
                block_lines = []
                j = i + 1
                while j < len(lines):
                    nxt = lines[j]
                    nindent = len(nxt) - len(nxt.lstrip(" "))
                    if nindent <= indent:
                        break
                    block_lines.append(nxt.strip())
                    j += 1
                parent[key] = " ".join(block_lines).strip()
                i = j
                continue

            if rest == "":
                # peek next non-empty line to decide list vs dict
                peek = None
                for j in range(i + 1, len(lines)):
                    if lines[j].strip():
                        peek = lines[j]
                        break
                if peek is not None and peek.strip().startswith("- "):
                    child: list = []
                else:
                    child = {}
                parent[key] = child
                stack.append((indent, child))
            else:
                parent[key] = _parse_scalar(rest)
            i += 1
            continue

        raise ValueError(f"Unrecognized YAML line: {line}")

    return root


def load_manifest_programs(manifest_path: Path) -> set[str]:
    """Extract COBOL program object names from the WO-001 manifest."""
    text = manifest_path.read_text(encoding="utf-8")
    programs: set[str] = set()
    # Shipped COBOL files: .../NAME.cbl
    for m in re.finditer(
        r"path:\s*Property_Casualty_Insurance_System/([A-Z0-9]+)\.cbl", text
    ):
        programs.add(m.group(1))
    # Synthetic missing-callee entries: path: NAME (no slash)
    for m in re.finditer(r"(?m)^\s*-\s*path:\s*([A-Z0-9]+)\s*$", text):
        programs.add(m.group(1))
    return programs


def validate(map_path: Path, manifest_path: Path) -> list[str]:
    errors: list[str] = []
    data = load_simple_yaml(map_path.read_text(encoding="utf-8"))
    programs = data.get("batch_programs")
    if not isinstance(programs, dict) or not programs:
        return ["batch_programs missing or empty in scheduler map"]

    map_keys = set(programs.keys())
    missing_from_map = EXPECTED_BATCH_PROGRAMS - map_keys
    extra_in_map = map_keys - EXPECTED_BATCH_PROGRAMS
    if missing_from_map:
        errors.append(f"batch programs missing from scheduler map: {sorted(missing_from_map)}")
    if extra_in_map:
        errors.append(f"unexpected programs in scheduler map: {sorted(extra_in_map)}")

    manifest_programs = load_manifest_programs(manifest_path)
    for name in sorted(map_keys & EXPECTED_BATCH_PROGRAMS):
        if name not in manifest_programs:
            errors.append(f"{name}: not present in repository manifest")

        entry = programs[name]
        if not isinstance(entry, dict):
            errors.append(f"{name}: entry is not a mapping")
            continue
        for field in REQUIRED_FIELDS:
            if field not in entry or entry[field] in (None, "", []):
                errors.append(f"{name}: missing required field {field}")

        target_sched = str(entry.get("target_schedule") or "")
        if "ASSUMPTION" not in target_sched:
            errors.append(f"{name}: target_schedule must be annotated ASSUMPTION")

        entries = entry.get("legacy_scheduler_entries")
        if not isinstance(entries, list) or not entries:
            errors.append(f"{name}: legacy_scheduler_entries must be a non-empty list")
            continue
        for sch in entries:
            if not isinstance(sch, dict):
                errors.append(f"{name}: scheduler entry is not a mapping")
                continue
            sname = sch.get("name")
            status = sch.get("source_status")
            if sname in {"JOBSCHD1", "JOBSCHD2", "JOBSCHD3"} and status != "referenced-only":
                errors.append(
                    f"{name}/{sname}: expected source_status referenced-only, got {status}"
                )
            if sname in {"JOBSCHD4", "JOBSCHD5", "JOBSCHD6", "JOBSCHD7"} and status != "in-repo":
                errors.append(
                    f"{name}/{sname}: expected source_status in-repo, got {status}"
                )

    prm = programs.get("PRM005B") if isinstance(programs, dict) else None
    if isinstance(prm, dict):
        if prm.get("target_cronjob_name") != "premiumProcessingJob":
            errors.append(
                "PRM005B: target_cronjob_name must be premiumProcessingJob "
                f"(got {prm.get('target_cronjob_name')})"
            )
        notes = str(prm.get("naming_notes") or "")
        if "delinquencyAgingJob" not in notes and prm.get("architecture_name") != "delinquencyAgingJob":
            errors.append("PRM005B: must document architecture name delinquencyAgingJob")
        if "premiumProcessingJob" not in notes:
            errors.append("PRM005B: naming_notes must justify premiumProcessingJob")

    # Bidirectional: every EXPECTED program that appears as shipped COBOL in
    # manifest must be scheduled (already covered by EXPECTED_BATCH_PROGRAMS).
    shipped_batch = EXPECTED_BATCH_PROGRAMS & manifest_programs
    for name in sorted(shipped_batch):
        if name not in map_keys:
            errors.append(f"{name}: shipped in manifest but missing from scheduler map")

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--map",
        default=None,
        help="Path to scheduler-map.yaml",
    )
    parser.add_argument(
        "--manifest",
        default=None,
        help="Path to pcis-manifest.yaml",
    )
    args = parser.parse_args()
    repo_root = Path(__file__).resolve().parents[1]
    map_path = Path(args.map) if args.map else repo_root / "ops" / "scheduler-map.yaml"
    manifest_path = (
        Path(args.manifest)
        if args.manifest
        else repo_root / "manifest" / "pcis-manifest.yaml"
    )

    if not map_path.is_file():
        print(f"ERROR: scheduler map not found: {map_path}", file=sys.stderr)
        return 2
    if not manifest_path.is_file():
        print(f"ERROR: manifest not found: {manifest_path}", file=sys.stderr)
        return 2

    errors = validate(map_path, manifest_path)
    if errors:
        print("Scheduler map validation FAILED:")
        for err in errors:
            print(f"  - {err}")
        return 1

    print("Scheduler map validation OK")
    print(f"  map: {map_path}")
    print(f"  manifest: {manifest_path}")
    print(f"  batch programs: {', '.join(sorted(EXPECTED_BATCH_PROGRAMS))}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
