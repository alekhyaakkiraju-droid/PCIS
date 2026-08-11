#!/usr/bin/env python3
"""
Validate contracts/audlog01-v1-contract.yaml (WO-007).

Checks:
  1. Every shipped COBOL program that depends on AUDLOG01 (manifest) has a
     callers[] entry — except AUD002B, which must appear under non_callers.
  2. AUD002B is documented as a non-caller with severity BLOCK.
  3. Required contract sections exist: callers, non_callers, drift_analysis,
     unified_v1_schema, mapping_table.
  4. Each caller entry has required per-parameter fields.
  5. When baseline/cobol-baseline.yaml is present, every baseline program with
     audit_call_parameters.called=true appears in callers[].

Exits 0 on success, non-zero on any discrepancy.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REQUIRED_SECTIONS = (
    "callers",
    "non_callers",
    "drift_analysis",
    "unified_v1_schema",
    "mapping_table",
)

REQUIRED_CALLER_FIELDS = (
    "program_id",
    "call_location",
    "parameter_count",
    "parameters",
)

REQUIRED_PARAM_FIELDS = (
    "position",
    "working_storage_name",
    "PIC",
    "sample_value",
    "semantic_meaning",
)

EXPECTED_CALLERS = frozenset(
    {
        "BIL003B",
        "CLM006B",
        "CMM001B",
        "POL006B",
        "PRM005B",
        "CUS001A",
        "POL001A",
    }
)


def _parse_scalar(raw: str):
    raw = raw.strip()
    if raw in ("null", "~", ""):
        return None
    if raw in ("true", "false"):
        return raw == "true"
    if raw == "[]":
        return []
    if raw == "{}":
        return {}
    if (raw.startswith('"') and raw.endswith('"')) or (
        raw.startswith("'") and raw.endswith("'")
    ):
        return raw[1:-1]
    try:
        return int(raw)
    except ValueError:
        return raw


def load_simple_yaml(text: str) -> dict:
    """Minimal indentation-based YAML loader for contract/manifest files."""
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
                    item[key] = _parse_scalar(rest)
                else:
                    # peek next indent for empty dict vs nested
                    item[key] = None
                    if i + 1 < len(lines):
                        nxt = lines[i + 1]
                        nindent = len(nxt) - len(nxt.lstrip(" "))
                        if nindent > indent:
                            # decide list vs dict from next content
                            if nxt.strip().startswith("- "):
                                item[key] = []
                                stack.append((indent + 2, item[key]))
                            else:
                                item[key] = {}
                                stack.append((indent + 2, item[key]))
            else:
                parent.append(_parse_scalar(item_body))
            i += 1
            continue

        if ": " in content or content.endswith(":"):
            key, _, rest = content.partition(":")
            key = key.strip()
            rest = rest.strip()
            if not isinstance(parent, dict):
                raise ValueError(f"Key under non-dict parent: {line}")
            if rest:
                parent[key] = _parse_scalar(rest)
            else:
                # look ahead
                if i + 1 < len(lines):
                    nxt = lines[i + 1]
                    nindent = len(nxt) - len(nxt.lstrip(" "))
                    if nindent > indent:
                        if nxt.strip().startswith("- "):
                            parent[key] = []
                            stack.append((indent, parent[key]))
                        else:
                            parent[key] = {}
                            stack.append((indent, parent[key]))
                    else:
                        parent[key] = None
                else:
                    parent[key] = None
            i += 1
            continue

        raise ValueError(f"Unrecognized YAML line: {line}")
    return root


def manifest_audlog01_dependents(manifest: dict) -> tuple[set[str], set[str]]:
    """
    Return (callers, aud002b_flagged).

    Manifest prologue parsing incorrectly attaches AUDLOG01 to AUD002B from the
    NOTE comment; treat AUD002B separately. Only shipped *.cbl members count.
    """
    callers: set[str] = set()
    aud002b_flagged = False
    for entry in manifest.get("files", []):
        path = entry.get("path") or ""
        if not path.endswith(".cbl"):
            continue
        prog = Path(path).stem.upper()
        deps = entry.get("dependencies") or []
        has_aud = any(
            (d.get("target_name") or "").upper() == "AUDLOG01"
            and (d.get("relationship_type") or "") == "calls"
            for d in deps
            if isinstance(d, dict)
        )
        if not has_aud:
            continue
        if prog == "AUD002B":
            aud002b_flagged = True
            continue
        callers.add(prog)
    return callers, aud002b_flagged


def baseline_audlog01_callers(baseline: dict) -> set[str]:
    callers: set[str] = set()
    for prog in baseline.get("programs", []):
        if not isinstance(prog, dict):
            continue
        audit = prog.get("audit_call_parameters") or {}
        if audit.get("called") is True:
            callers.add((prog.get("program_id") or "").upper())
    return {c for c in callers if c}


def validate(
    contract: dict,
    manifest: dict | None,
    baseline: dict | None,
) -> list[str]:
    errors: list[str] = []

    for section in REQUIRED_SECTIONS:
        if section not in contract:
            errors.append(f"missing required section: {section}")

    callers = contract.get("callers") or []
    if not isinstance(callers, list):
        errors.append("callers must be a list")
        callers = []

    caller_ids = set()
    for entry in callers:
        if not isinstance(entry, dict):
            errors.append("caller entry is not a mapping")
            continue
        pid = (entry.get("program_id") or "").upper()
        caller_ids.add(pid)
        for field in REQUIRED_CALLER_FIELDS:
            if field not in entry:
                errors.append(f"{pid}: missing field {field}")
        loc = entry.get("call_location") or {}
        if not isinstance(loc, dict) or "paragraph" not in loc or "approx_line" not in loc:
            errors.append(f"{pid}: call_location must include paragraph and approx_line")
        params = entry.get("parameters") or []
        count = entry.get("parameter_count")
        if isinstance(params, list) and count is not None and count != len(params):
            errors.append(
                f"{pid}: parameter_count={count} does not match len(parameters)={len(params)}"
            )
        for p in params if isinstance(params, list) else []:
            if not isinstance(p, dict):
                errors.append(f"{pid}: parameter entry not a mapping")
                continue
            for field in REQUIRED_PARAM_FIELDS:
                if field not in p:
                    errors.append(
                        f"{pid}: parameter missing {field} "
                        f"(pos={p.get('position')})"
                    )

    missing = EXPECTED_CALLERS - caller_ids
    extra = caller_ids - EXPECTED_CALLERS
    if missing:
        errors.append(f"callers missing expected programs: {sorted(missing)}")
    if extra:
        errors.append(f"unexpected callers: {sorted(extra)}")

    non_callers = contract.get("non_callers") or []
    aud002b = None
    for nc in non_callers:
        if isinstance(nc, dict) and (nc.get("program_id") or "").upper() == "AUD002B":
            aud002b = nc
            break
    if aud002b is None:
        errors.append("AUD002B must appear under non_callers")
    else:
        severity = (aud002b.get("severity") or "").upper()
        if severity != "BLOCK":
            errors.append(f"AUD002B non_caller severity must be BLOCK, got {severity!r}")
        if aud002b.get("calls_audlog01") not in (False, None):
            # allow missing; if present must be false
            if aud002b.get("calls_audlog01") is not False:
                errors.append("AUD002B.calls_audlog01 must be false")

    drift = contract.get("drift_analysis") or {}
    if isinstance(drift, dict):
        text = " ".join(str(v) for v in drift.values())
        for token in ("X(3)", "X(30)", "X(1)", "X(100)", "X(40)"):
            if token not in text and token not in str(drift):
                # deeper check in nested dict
                pass
        blob = repr(drift)
        for token in ("X(3)", "X(30)", "X(1)", "X(100)", "X(40)"):
            if token not in blob:
                errors.append(f"drift_analysis must document {token}")
    else:
        errors.append("drift_analysis must be a mapping")

    schema = contract.get("unified_v1_schema") or {}
    field_names = set()
    for f in schema.get("fields") or []:
        if isinstance(f, dict) and f.get("name"):
            field_names.add(f["name"])
    for required in (
        "action",
        "old_value",
        "new_value",
        "key",
        "correlation_id",
        "service",
        "program",
        "actor",
        "resource",
        "operation",
        "timestamp",
    ):
        if required not in field_names:
            errors.append(f"unified_v1_schema missing field: {required}")

    mapping = contract.get("mapping_table") or {}
    rules_blob = repr(mapping)
    for legacy, op in (("ADD", "CREATE"), ("UPD", "UPDATE"), ("DEL", "DELETE")):
        if legacy not in rules_blob or op not in rules_blob:
            errors.append(f"mapping_table must document {legacy}→{op}")
    for lit in (
        "BATCHAUD",
        "BATCHBIL",
        "BATCHCMM",
        "BATCHPRM",
        "BATCHCLM",
        "BATCHREN",
    ):
        if lit not in rules_blob:
            errors.append(f"mapping_table must document batch actor literal {lit}")

    if manifest is not None:
        m_callers, aud002b_flagged = manifest_audlog01_dependents(manifest)
        for prog in sorted(m_callers):
            if prog not in caller_ids:
                errors.append(
                    f"manifest AUDLOG01 dependent {prog} missing from contract callers"
                )
        # AUD002B may be falsely flagged in manifest; must still be non_caller
        if aud002b_flagged and aud002b is None:
            errors.append(
                "manifest references AUDLOG01 for AUD002B but contract lacks non_callers entry"
            )

    if baseline is not None:
        b_callers = baseline_audlog01_callers(baseline)
        for prog in sorted(b_callers):
            if prog not in caller_ids:
                errors.append(
                    f"baseline AUDLOG01 caller {prog} missing from contract callers"
                )

    return errors


def main(argv: list[str] | None = None) -> int:
    repo = Path(__file__).resolve().parent.parent
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument(
        "--contract",
        type=Path,
        default=repo / "contracts" / "audlog01-v1-contract.yaml",
    )
    ap.add_argument(
        "--manifest",
        type=Path,
        default=repo / "manifest" / "pcis-manifest.yaml",
    )
    ap.add_argument(
        "--baseline",
        type=Path,
        default=repo / "baseline" / "cobol-baseline.yaml",
    )
    args = ap.parse_args(argv)

    if not args.contract.is_file():
        print(f"FAIL: contract not found: {args.contract}", file=sys.stderr)
        return 1

    contract = load_simple_yaml(args.contract.read_text(encoding="utf-8"))
    manifest = None
    if args.manifest.is_file():
        manifest = load_simple_yaml(args.manifest.read_text(encoding="utf-8"))
    else:
        print(f"WARN: manifest not found, skipping manifest cross-check: {args.manifest}")

    baseline = None
    if args.baseline.is_file():
        baseline = load_simple_yaml(args.baseline.read_text(encoding="utf-8"))
    else:
        print(f"WARN: baseline not found, skipping baseline cross-check: {args.baseline}")

    errors = validate(contract, manifest, baseline)
    if errors:
        print(f"FAIL: {len(errors)} validation error(s)")
        for e in errors:
            print(f"  - {e}")
        return 1

    caller_n = len(contract.get("callers") or [])
    print(
        f"PASS: contract valid — {caller_n} callers, "
        f"AUD002B non_caller severity=BLOCK"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
