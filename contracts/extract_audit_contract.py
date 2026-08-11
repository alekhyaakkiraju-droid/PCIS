#!/usr/bin/env python3
"""
AUDLOG01 parameter contract extractor (WO-007).

Parses CALL 'AUDLOG01' USING sites in Property_Casualty_Insurance_System/*.cbl,
resolves PIC definitions from WORKING-STORAGE (including leaf expansion of
group items), and emits contracts/audlog01-v1-contract.yaml.

Stdlib only — no PyYAML.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

GENERATOR_VERSION = "1.0.0"
AUDLOG01 = "AUDLOG01"

EXPECTED_CALLERS = (
    "BIL003B",
    "CLM006B",
    "CMM001B",
    "POL006B",
    "PRM005B",
    "CUS001A",
    "POL001A",
)

BATCH_CALLERS = frozenset(
    {"BIL003B", "CLM006B", "CMM001B", "POL006B", "PRM005B"}
)
INTERACTIVE_CALLERS = frozenset({"CUS001A", "POL001A"})

# Documented modernization actor literals (replace with workload principals).
BATCH_ACTOR_LITERALS = {
    "AUD002B": "BATCHAUD",
    "BIL003B": "BATCHBIL",
    "CLM006B": "BATCHCLM",
    "CMM001B": "BATCHCMM",
    "POL006B": "BATCHREN",
    "PRM005B": "BATCHPRM",
}

SEMANTIC_HINTS = {
    "WS-AUD-PROGRAM": "calling program name stamped into the audit record",
    "WS-AUD-ACTION": "legacy action / operation code",
    "WS-AUD-TABLE": "target table / resource name",
    "WS-AUD-KEY": "business key for the audited row",
    "WS-AUD-USER": "change actor (batch literal or interactive user)",
    "WS-AUD-RESULT": "AUDLOG01 return / result code slot",
    "WS-AUD-OBJECT": "compact batch object / key payload (CLM006B shape)",
    "WS-AUD-OLD-VALUE": "before-image value (interactive G-04 shape)",
    "WS-AUD-NEW-VALUE": "after-image value (interactive G-04 shape)",
    "WS-AUDIT-PARMS": "group parameter block passed BY REFERENCE to AUDLOG01",
}

ACTION_TO_OPERATION = {
    "ADD": "CREATE",
    "A": "CREATE",
    "INSERT": "CREATE",
    "UPD": "UPDATE",
    "U": "UPDATE",
    "UPDATE": "UPDATE",
    "C": "UPDATE",
    "DEL": "DELETE",
    "D": "DELETE",
    "DELETE": "DELETE",
    "PAY": "PAY",
    "REN": "RENEW",
    "RENEW": "RENEW",
    "BILL": "BILL",
    "INIT": "INIT",
    "FINALIZE": "FINALIZE",
}


def strip_cobol_line(line: str) -> str:
    """Normalize a COBOL source line to code text (fixed or free form)."""
    raw = line.rstrip("\n")
    if not raw.strip():
        return ""
    if len(raw) >= 7 and raw[6] == "*":
        return "*" + raw[7:72].rstrip()
    if len(raw) >= 8 and raw[:6].strip() == "" and raw[6] == " ":
        return raw[7:72].rstrip()
    return raw.strip()


def format_scalar(v: Any) -> str:
    if v is None:
        return "null"
    if isinstance(v, bool):
        return "true" if v else "false"
    if isinstance(v, (int, float)):
        return str(v)
    s = str(v)
    if s == "":
        return '""'
    if any(c in s for c in [":", "#", "{", "}", "[", "]", ",", "\n", "'", '"']) or s.lower() in {
        "true",
        "false",
        "null",
        "yes",
        "no",
    }:
        return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'
    return s


def dump_yaml(data: Any, indent: int = 0) -> str:
    """Minimal YAML emitter (stdlib only)."""
    sp = "  " * indent
    if isinstance(data, dict):
        if not data:
            return f"{sp}{{}}"
        lines: list[str] = []
        for k, v in data.items():
            if isinstance(v, (dict, list)):
                if isinstance(v, list) and not v:
                    lines.append(f"{sp}{k}: []")
                elif isinstance(v, dict) and not v:
                    lines.append(f"{sp}{k}: {{}}")
                else:
                    lines.append(f"{sp}{k}:")
                    nested = dump_yaml(v, indent + 1)
                    if nested:
                        lines.append(nested)
            else:
                lines.append(f"{sp}{k}: {format_scalar(v)}")
        return "\n".join(lines)
    if isinstance(data, list):
        if not data:
            return f"{sp}[]"
        lines = []
        for item in data:
            if isinstance(item, dict):
                first = True
                for k, v in item.items():
                    prefix = "- " if first else "  "
                    first = False
                    if isinstance(v, (dict, list)) and v:
                        lines.append(f"{sp}{prefix}{k}:")
                        nested = dump_yaml(v, indent + 2)
                        if nested:
                            lines.append(nested)
                    elif isinstance(v, list) and not v:
                        lines.append(f"{sp}{prefix}{k}: []")
                    elif isinstance(v, dict) and not v:
                        lines.append(f"{sp}{prefix}{k}: {{}}")
                    else:
                        lines.append(f"{sp}{prefix}{k}: {format_scalar(v)}")
            else:
                lines.append(f"{sp}- {format_scalar(item)}")
        return "\n".join(lines)
    return f"{sp}{format_scalar(data)}"


class CobolAuditParser:
    """Parse one COBOL program for AUDLOG01 call sites and PIC maps."""

    LEVEL_RE = re.compile(
        r"^\s*(\d{2})\s+([A-Z0-9][A-Z0-9-]*)(?:\s+PIC\s+([^\s.]+(?:\([0-9]+\))?))?",
        re.I,
    )
    VALUE_RE = re.compile(
        r"\bVALUE\s+(IS\s+)?('([^']*)'|\"([^\"]*)\"|([^\s.]+))",
        re.I,
    )
    PARA_RE = re.compile(r"^([0-9A-Z][0-9A-Z-]*)\.\s*$", re.I)
    CALL_RE = re.compile(
        r"\bCALL\s+['\"]AUDLOG01['\"]\s*(?:USING\s+(.+))?",
        re.I,
    )
    MOVE_RE = re.compile(
        r"\bMOVE\s+('([^']*)'|\"([^\"]*)\"|([A-Z0-9][A-Z0-9-]*(?:\([^\)]*\))?))\s+TO\s+([A-Z0-9][A-Z0-9-]*)",
        re.I,
    )

    def __init__(self, path: Path):
        self.path = path
        self.program_id = path.stem.upper()
        self.raw_lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
        self.lines = [strip_cobol_line(l) for l in self.raw_lines]
        self.pic_map: dict[str, str] = {}
        self.value_map: dict[str, str] = {}
        self.children: dict[str, list[str]] = defaultdict(list)
        self.parent: dict[str, str | None] = {}
        self._scan_working_storage()

    def _scan_working_storage(self) -> None:
        in_ws = False
        stack: list[tuple[int, str]] = []
        last_name: str | None = None
        for line in self.lines:
            upper = line.upper()
            if "WORKING-STORAGE SECTION" in upper:
                in_ws = True
                continue
            if "LINKAGE SECTION" in upper or "PROCEDURE DIVISION" in upper:
                in_ws = False
            if not in_ws or line.startswith("*"):
                continue
            m = self.LEVEL_RE.match(line)
            if m:
                level = int(m.group(1))
                name = m.group(2).upper()
                pic = m.group(3)
                if pic:
                    self.pic_map[name] = re.sub(r"\s+", " ", pic).strip()
                while stack and stack[-1][0] >= level:
                    stack.pop()
                parent = stack[-1][1] if stack else None
                self.parent[name] = parent
                if parent:
                    self.children[parent].append(name)
                stack.append((level, name))
                last_name = name
                vm = self.VALUE_RE.search(line)
                if vm:
                    lit = vm.group(3) or vm.group(4) or vm.group(5) or ""
                    self.value_map[name] = lit.strip()
                continue
            if last_name:
                vm = self.VALUE_RE.search(line)
                if vm and last_name not in self.value_map:
                    lit = vm.group(3) or vm.group(4) or vm.group(5) or ""
                    self.value_map[last_name] = lit.strip()

    def leaf_fields(self, name: str) -> list[str]:
        """Resolve a data-name to ordered leaf field names (with PIC)."""
        name = name.upper()
        kids = self.children.get(name, [])
        if not kids:
            return [name]
        leaves: list[str] = []
        for kid in kids:
            leaves.extend(self.leaf_fields(kid))
        return leaves

    def resolve_pic(self, name: str) -> str | None:
        name = name.upper()
        if name in self.pic_map:
            return self.pic_map[name]
        # Group with no own PIC — report first leaf PIC summary for groups
        leaves = self.leaf_fields(name)
        if len(leaves) == 1 and leaves[0] != name:
            return self.pic_map.get(leaves[0])
        return None

    def _paragraph_at(self, line_idx: int) -> str:
        for i in range(line_idx, -1, -1):
            line = self.lines[i]
            if not line or line.startswith("*"):
                continue
            m = self.PARA_RE.match(line.strip())
            if m:
                return m.group(1).upper()
        return "UNKNOWN"

    _USING_STOP_WORDS = frozenset(
        {
            "USING",
            "BY",
            "REFERENCE",
            "CONTENT",
            "VALUE",
            "IF",
            "ELSE",
            "END-IF",
            "PERFORM",
            "EXEC",
            "MOVE",
            "DISPLAY",
            "CALL",
            "STOP",
            "WHEN",
            "EVALUATE",
            "END-EVALUATE",
            "CONTINUE",
            "GOBACK",
            "EXIT",
        }
    )

    def _is_data_name(self, tok: str) -> bool:
        """Accept only known WORKING-STORAGE names (or plausible WS-/HV- ids)."""
        tok = tok.upper()
        if tok in self._USING_STOP_WORDS:
            return False
        if tok in self.pic_map or tok in self.children or tok in self.parent:
            return True
        return bool(re.match(r"^(WS|HV|LK)-[A-Z0-9-]+$", tok))

    def _collect_using(self, start_idx: int, first_rest: str | None) -> list[str]:
        params: list[str] = []
        if first_rest:
            using_raw = first_rest.split(".")[0]
            for tok in re.findall(r"[A-Z0-9-]+", using_raw.upper()):
                if self._is_data_name(tok):
                    params.append(tok)
        ended = bool(first_rest and "." in first_rest)
        if not ended:
            for j in range(start_idx + 1, min(start_idx + 12, len(self.lines))):
                nxt = self.lines[j]
                if not nxt or nxt.startswith("*"):
                    continue
                upper = nxt.upper()
                if re.search(
                    r"\b(IF|ELSE|END-IF|PERFORM|EXEC|MOVE|DISPLAY|CALL|STOP|"
                    r"WHEN|EVALUATE|CONTINUE|GOBACK|EXIT)\b",
                    upper,
                ):
                    break
                for tok in re.findall(r"[A-Z0-9-]+", upper):
                    if self._is_data_name(tok):
                        params.append(tok)
                if "." in nxt:
                    break
        # de-dupe while preserving order
        seen = set()
        ordered = []
        for p in params:
            if p not in seen:
                seen.add(p)
                ordered.append(p)
        return ordered

    def _sample_moves_near(self, line_idx: int) -> dict[str, str]:
        samples: dict[str, str] = {}
        start = max(0, line_idx - 12)
        for i in range(start, line_idx):
            line = self.lines[i]
            if not line or line.startswith("*"):
                continue
            for m in self.MOVE_RE.finditer(line):
                target = m.group(5).upper()
                lit = m.group(2) or m.group(3)
                src = m.group(4)
                if lit is not None:
                    samples[target] = lit
                elif src:
                    samples[target] = src.upper().split("(")[0]
        return samples

    def extract_calls(self) -> list[dict[str, Any]]:
        calls: list[dict[str, Any]] = []
        in_proc = False
        for i, line in enumerate(self.lines):
            upper = line.upper()
            if "PROCEDURE DIVISION" in upper:
                in_proc = True
                continue
            if not in_proc or line.startswith("*"):
                continue
            m = self.CALL_RE.search(upper)
            if not m:
                continue
            using_names = self._collect_using(i, m.group(1))
            if not using_names:
                using_names = ["WS-AUDIT-PARMS"]
            samples = self._sample_moves_near(i)
            paragraph = self._paragraph_at(i)
            # Expand groups to leaf parameters for the contract inventory
            leaf_params: list[dict[str, Any]] = []
            position = 1
            for uname in using_names:
                leaves = self.leaf_fields(uname)
                for leaf in leaves:
                    pic = self.pic_map.get(leaf)
                    sample = samples.get(leaf) or self.value_map.get(leaf)
                    if sample is None and leaf == "WS-AUD-PROGRAM":
                        sample = self.program_id
                    leaf_params.append(
                        {
                            "position": position,
                            "working_storage_name": leaf,
                            "PIC": pic,
                            "sample_value": sample,
                            "semantic_meaning": SEMANTIC_HINTS.get(
                                leaf,
                                f"AUDLOG01 parameter from {uname}",
                            ),
                            "using_group": uname if leaf != uname else None,
                        }
                    )
                    position += 1
            calls.append(
                {
                    "program_id": self.program_id,
                    "call_location": {
                        "paragraph": paragraph,
                        "approx_line": i + 1,
                    },
                    "parameter_count": len(leaf_params),
                    "parameters": leaf_params,
                    "using_names": using_names,
                }
            )
        return calls


def classify_shape(call_entry: dict[str, Any]) -> str:
    """Classify a caller parameter shape for drift analysis."""
    pics = {p["working_storage_name"]: (p.get("PIC") or "") for p in call_entry["parameters"]}
    action = pics.get("WS-AUD-ACTION", "")
    old_v = pics.get("WS-AUD-OLD-VALUE", "")
    new_v = pics.get("WS-AUD-NEW-VALUE", "")
    key = pics.get("WS-AUD-KEY", "")
    obj = pics.get("WS-AUD-OBJECT", "")
    if "X(1)" in action and ("X(100)" in old_v or "X(100)" in new_v):
        return "interactive_g04"
    if "X(3)" in action and "X(30)" in obj:
        return "batch_compact_g04"
    if "WS-AUD-PROGRAM" in pics and "WS-AUD-TABLE" in pics:
        return "common_six_field"
    return "other"


def build_drift_analysis(callers: list[dict[str, Any]]) -> dict[str, Any]:
    by_prog: dict[str, str] = {}
    for c in callers:
        by_prog[c["program_id"]] = classify_shape(c)

    return {
        "gap_id": "G-04",
        "summary": (
            "AUDLOG01 parameter widths drift across callers. Gap analysis and "
            "modernization fixtures document batch X(3) action with X(30) value/object "
            "fields versus interactive X(1) action with X(100) old/new values and X(40) key. "
            "Shipped source also exposes a common six-field WS-AUDIT-PARMS group used by "
            "most batch and interactive programs; CLM006B retains the compact batch shape."
        ),
        "documented_canonical_shapes": {
            "batch": {
                "action": "X(3)",
                "old_value_or_object": "X(30)",
                "new_value": "X(30)",
                "key": "X(30)",
                "notes": "Canonical G-04 batch shape from gap analysis / fixtures",
            },
            "interactive": {
                "action": "X(1)",
                "old_value": "X(100)",
                "new_value": "X(100)",
                "key": "X(40)",
                "notes": "Canonical G-04 interactive shape from gap analysis / fixtures",
            },
        },
        "observed_source_shapes": {
            prog: {
                "shape_class": shape,
                "caller_class": (
                    "batch" if prog in BATCH_CALLERS else "interactive"
                ),
            }
            for prog, shape in by_prog.items()
        },
        "truncation_risk": (
            "Mapping an interactive X(100) before-image into a batch X(30) slot "
            "silently drops 70 characters of evidence. The unified v1 schema uses "
            "VARCHAR(100) for old/new and VARCHAR(40) for key to prevent truncation."
        ),
    }


def build_unified_schema() -> dict[str, Any]:
    return {
        "version": "v1",
        "description": (
            "Superset of legacy AUDLOG01 shapes plus modern observability fields. "
            "Widths are the maximum of batch and interactive G-04 PIC clauses."
        ),
        "fields": [
            {
                "name": "action",
                "type": "VARCHAR(3)",
                "nullable": False,
                "notes": "Holds batch X(3) codes; interactive X(1) left-justified",
            },
            {
                "name": "old_value",
                "type": "VARCHAR(100)",
                "nullable": True,
                "notes": "max(X(100) interactive, X(30) batch)",
            },
            {
                "name": "new_value",
                "type": "VARCHAR(100)",
                "nullable": True,
                "notes": "max(X(100) interactive, X(30) batch)",
            },
            {
                "name": "key",
                "type": "VARCHAR(40)",
                "nullable": True,
                "notes": "max(X(40) interactive, X(30) batch key/object)",
            },
            {
                "name": "correlation_id",
                "type": "UUID",
                "nullable": False,
                "notes": "Generated when absent; honour inbound trace header",
            },
            {
                "name": "service",
                "type": "VARCHAR(30)",
                "nullable": False,
                "notes": "Owning modern service (billing-svc, claims-svc, …)",
            },
            {
                "name": "program",
                "type": "VARCHAR(10)",
                "nullable": True,
                "notes": "Legacy program id (WS-AUD-PROGRAM)",
            },
            {
                "name": "actor",
                "type": "VARCHAR(10)",
                "nullable": False,
                "notes": "Authenticated principal; replaces BATCH* literals",
            },
            {
                "name": "resource",
                "type": "VARCHAR(50)",
                "nullable": False,
                "notes": "Table/resource name (WS-AUD-TABLE / object)",
            },
            {
                "name": "operation",
                "type": "VARCHAR(30)",
                "nullable": False,
                "notes": "Canonical operation from mapping_table",
            },
            {
                "name": "timestamp",
                "type": "TIMESTAMPTZ",
                "nullable": False,
                "notes": "Event time at emit",
            },
        ],
    }


def build_mapping_table() -> dict[str, Any]:
    legacy_to_v1 = [
        {
            "legacy_parameter": "WS-AUD-ACTION",
            "v1_field": "action",
            "transform": "Copy code; map via action_code_rules to operation",
        },
        {
            "legacy_parameter": "WS-AUD-OLD-VALUE",
            "v1_field": "old_value",
            "transform": "Direct copy; pad/truncate policy = reject >100",
        },
        {
            "legacy_parameter": "WS-AUD-NEW-VALUE",
            "v1_field": "new_value",
            "transform": "Direct copy; pad/truncate policy = reject >100",
        },
        {
            "legacy_parameter": "WS-AUD-OBJECT",
            "v1_field": "key",
            "transform": "Batch compact object maps to key (also informs resource)",
        },
        {
            "legacy_parameter": "WS-AUD-KEY",
            "v1_field": "key",
            "transform": "Direct copy into VARCHAR(40)",
        },
        {
            "legacy_parameter": "WS-AUD-TABLE",
            "v1_field": "resource",
            "transform": "Table name becomes resource",
        },
        {
            "legacy_parameter": "WS-AUD-PROGRAM",
            "v1_field": "program",
            "transform": "Direct copy; service derived from program module map",
        },
        {
            "legacy_parameter": "WS-AUD-USER",
            "v1_field": "actor",
            "transform": "Replace BATCH* literals with workload principal",
        },
        {
            "legacy_parameter": "WS-AUD-RESULT",
            "v1_field": None,
            "transform": "Legacy return slot — not persisted; HTTP status replaces it",
        },
    ]
    action_rules = [
        {"legacy_action": legacy, "v1_operation": op}
        for legacy, op in sorted(ACTION_TO_OPERATION.items())
    ]
    # Ensure required AC mappings are explicit first-class entries
    required = [
        {"legacy_action": "ADD", "v1_operation": "CREATE"},
        {"legacy_action": "UPD", "v1_operation": "UPDATE"},
        {"legacy_action": "DEL", "v1_operation": "DELETE"},
    ]
    return {
        "legacy_parameter_to_v1_field": legacy_to_v1,
        "action_code_rules": required
        + [r for r in action_rules if r["legacy_action"] not in {"ADD", "UPD", "DEL"}],
        "batch_actor_literals": [
            {
                "program_id": prog,
                "legacy_literal": lit,
                "v1_replacement": "authenticated workload principal (ServiceAccount)",
            }
            for prog, lit in sorted(BATCH_ACTOR_LITERALS.items())
        ],
        "notes": (
            "ADD→CREATE, UPD→UPDATE, DEL→DELETE are the required canonical mappings. "
            "Observed source codes (INIT, INSERT, BILL, RENEW, FINALIZE, PAY) are also listed. "
            "Batch actor literals BATCHAUD/BATCHBIL/BATCHCMM/BATCHPRM/BATCHCLM/BATCHREN "
            "must be replaced by authenticated workload principals in audit-svc."
        ),
    }


def consolidate_program(calls: list[dict[str, Any]]) -> dict[str, Any]:
    """Merge multiple call sites for one program into a contract entry."""
    if not calls:
        raise ValueError("no calls")
    program_id = calls[0]["program_id"]
    # Prefer a call site whose leaf params all resolve to a PIC (skip parse noise)
    def _quality(c: dict[str, Any]) -> tuple[int, int]:
        with_pic = sum(1 for p in c["parameters"] if p.get("PIC"))
        return (with_pic, c["parameter_count"])

    richest = max(calls, key=_quality)
    call_locations = [
        {
            "paragraph": c["call_location"]["paragraph"],
            "approx_line": c["call_location"]["approx_line"],
        }
        for c in calls
    ]
    # Prefer a single call_location field as AC requires, plus all sites
    primary = calls[0]["call_location"]
    # Collect sample values across sites (prefer non-SPACES literals)
    samples: dict[str, str] = {}
    for c in calls:
        for p in c["parameters"]:
            name = p["working_storage_name"]
            val = p.get("sample_value")
            if not val:
                continue
            if name not in samples or samples[name] in {"SPACES", "ZERO", "ZEROS"}:
                samples[name] = val
    parameters = []
    for p in richest["parameters"]:
        entry = {
            "position": p["position"],
            "working_storage_name": p["working_storage_name"],
            "PIC": p["PIC"],
            "sample_value": samples.get(p["working_storage_name"], p.get("sample_value")),
            "semantic_meaning": p["semantic_meaning"],
        }
        if p.get("using_group"):
            entry["using_group"] = p["using_group"]
        parameters.append(entry)
    return {
        "program_id": program_id,
        "caller_class": "batch" if program_id in BATCH_CALLERS else "interactive",
        "shape_class": classify_shape(richest),
        "call_location": {
            "paragraph": primary["paragraph"],
            "approx_line": primary["approx_line"],
        },
        "all_call_sites": call_locations,
        "call_site_count": len(calls),
        "parameter_count": len(parameters),
        "parameters": parameters,
        "batch_actor_literal": BATCH_ACTOR_LITERALS.get(program_id),
    }


def build_contract(source_dir: Path) -> dict[str, Any]:
    all_calls: dict[str, list[dict[str, Any]]] = {}
    for path in sorted(source_dir.glob("*.cbl")):
        parser = CobolAuditParser(path)
        calls = parser.extract_calls()
        if calls:
            all_calls[parser.program_id] = calls
        elif parser.program_id == "AUD002B":
            all_calls.setdefault("_non_callers", [])

    callers = []
    for prog in EXPECTED_CALLERS:
        if prog not in all_calls:
            raise SystemExit(f"Expected AUDLOG01 caller missing from source: {prog}")
        callers.append(consolidate_program(all_calls[prog]))

    # Confirm AUD002B has zero calls
    aud002 = CobolAuditParser(source_dir / "AUD002B.cbl")
    if aud002.extract_calls():
        raise SystemExit("AUD002B unexpectedly contains AUDLOG01 calls")

    return {
        "contract_id": "audlog01-v1",
        "woref": "WO-007",
        "gap_refs": ["G-03", "G-04"],
        "generation_timestamp": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "generator_version": GENERATOR_VERSION,
        "source_root": "Property_Casualty_Insurance_System",
        "baseline_ref": "baseline/cobol-baseline.yaml",
        "manifest_ref": "manifest/pcis-manifest.yaml",
        "description": (
            "Inferred AUDLOG01 parameter contract from seven COBOL callers. "
            "AUDLOG01 source is missing (G-03). Binding input for audit-svc v1."
        ),
        "callers": callers,
        "non_callers": [
            {
                "program_id": "AUD002B",
                "calls_audlog01": False,
                "severity": "BLOCK",
                "finding": (
                    "Audit archive program does not call AUDLOG01; the archiver "
                    "itself produces no audit trail for archive/delete operations "
                    "(self-audit gap)."
                ),
                "evidence": "PROCEDURE DIVISION never contains CALL 'AUDLOG01'",
                "batch_actor_literal": "BATCHAUD",
            }
        ],
        "drift_analysis": build_drift_analysis(callers),
        "unified_v1_schema": build_unified_schema(),
        "mapping_table": build_mapping_table(),
    }


def parse_cobol_text(text: str, program_id: str = "FIXTURE") -> CobolAuditParser:
    """Parse in-memory COBOL text (for unit tests / fixtures)."""
    path = Path(f"{program_id}.cbl")
    parser = CobolAuditParser.__new__(CobolAuditParser)
    parser.path = path
    parser.program_id = program_id.upper()
    parser.raw_lines = text.splitlines()
    parser.lines = [strip_cobol_line(l) for l in parser.raw_lines]
    parser.pic_map = {}
    parser.value_map = {}
    parser.children = defaultdict(list)
    parser.parent = {}
    parser._scan_working_storage()
    return parser


def main(argv: list[str] | None = None) -> int:
    repo = Path(__file__).resolve().parent.parent
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--source-dir",
        type=Path,
        default=repo / "Property_Casualty_Insurance_System",
        help="Directory containing COBOL sources",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=repo / "contracts" / "audlog01-v1-contract.yaml",
        help="Output contract YAML path",
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="Also print JSON summary to stdout",
    )
    args = parser.parse_args(argv)

    if not args.source_dir.is_dir():
        print(f"Source directory not found: {args.source_dir}", file=sys.stderr)
        return 1

    contract = build_contract(args.source_dir)
    header = (
        "# AUDLOG01 v1 Parameter Contract — generated by "
        "contracts/extract_audit_contract.py\n"
        "# Stdlib YAML emitter (no PyYAML). Regenerate after COBOL call-site changes.\n"
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(header + dump_yaml(contract) + "\n", encoding="utf-8")
    print(f"Wrote {args.output}")

    summary = {
        "callers": [
            {
                "program_id": c["program_id"],
                "parameter_count": c["parameter_count"],
                "call_site_count": c["call_site_count"],
                "shape_class": c["shape_class"],
            }
            for c in contract["callers"]
        ],
        "non_callers": [
            {"program_id": n["program_id"], "severity": n["severity"]}
            for n in contract["non_callers"]
        ],
    }
    if args.json:
        print(json.dumps(summary, indent=2))
    else:
        for c in summary["callers"]:
            print(
                f"  {c['program_id']}: params={c['parameter_count']} "
                f"sites={c['call_site_count']} shape={c['shape_class']}"
            )
        for n in summary["non_callers"]:
            print(f"  non_caller {n['program_id']}: severity={n['severity']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
