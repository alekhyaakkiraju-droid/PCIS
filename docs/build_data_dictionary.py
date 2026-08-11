#!/usr/bin/env python3
"""
PCIS Data Dictionary Builder (WO-128, WO-150)

Reconciles PCIS_Database_Design.md (55 tables) against COBOL host variables
from baseline/cobol-baseline.yaml (WOREF-002) or Property_Casualty_Insurance_System/*.cbl.

WO-150 extends entries with classification tiers, PII flags, drift notes (G-06),
and code-witnessed Flyway extras (COMMISSION_LEDGER_T, outbox_events).

Stdlib only — no PyYAML.
"""

from __future__ import annotations

import argparse
import re
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable, Optional

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_DDL = REPO_ROOT / "Property_Casualty_Insurance_System" / "PCIS_Database_Design.md"
DEFAULT_BASELINE = REPO_ROOT / "baseline" / "cobol-baseline.yaml"
DEFAULT_COBOL_DIR = REPO_ROOT / "Property_Casualty_Insurance_System"
DEFAULT_OUTPUT = REPO_ROOT / "docs" / "data-dictionary.yaml"
DEFAULT_FLYWAY = (
    REPO_ROOT / "shared-libs" / "pcis-schema" / "db" / "migration" / "V1__baseline_schema.sql"
)

from classification_registry import column_classification, table_tier  # noqa: E402
from flyway_schema_parser import parse_flyway_schema  # noqa: E402

# Table aliases: COBOL SQL table → DDL design table
TABLE_ALIASES = {
    "ADDRESS_T": "CUSTOMER_ADDRESS_T",
    "CONTACT_T": "CUSTOMER_CONTACT_T",
}

# G-06 / gap-analysis COBOL column witnesses for critical tables (not always
# present in the simplified shipped .cbl members, but documented in WO-128).
G06_COBOL_WITNESS: dict[str, list[dict[str, str]]] = {
    "CUSTOMER_T": [
        {"name": "CUST_DOB", "pic": "X(10)", "maps_to_ddl": "DOB"},
        {"name": "CUST_SSN_TAXID", "pic": "X(11)", "maps_to_ddl": "TAX_ID"},
        {"name": "CUST_EMAIL", "pic": "X(80)", "maps_to_ddl": "EMAIL"},
        {"name": "CUST_GENDER", "pic": "X(1)", "maps_to_ddl": ""},
        {"name": "CUST_MARITAL_ST", "pic": "X(1)", "maps_to_ddl": ""},
        {"name": "CUST_CREDIT_SCORE", "pic": "S9(3)", "maps_to_ddl": ""},
    ],
    "BILLING_SCHEDULE_T": [
        {"name": "DUE_AMT", "pic": "S9(9)V99 COMP-3", "maps_to_ddl": "AMT_DUE"},
        {"name": "PAID_AMT", "pic": "S9(9)V99 COMP-3", "maps_to_ddl": "AMT_PAID"},
        {"name": "BILL_STATUS", "pic": "X(1)", "maps_to_ddl": "SCHED_STATUS"},
        {"name": "COMM_CALC_FLAG", "pic": "X(1)", "maps_to_ddl": ""},
    ],
    "CLAIM_RESERVE_T": [
        {"name": "RESERVE_ID", "pic": "S9(9) COMP", "maps_to_ddl": "RESERVE_HIST_ID"},
        {"name": "APPROVED_AMT", "pic": "S9(11)V99 COMP-3", "maps_to_ddl": "RESERVE_AMT"},
        {"name": "PAID_TO_DATE", "pic": "S9(11)V99 COMP-3", "maps_to_ddl": ""},
        {"name": "RESERVE_STATUS", "pic": "X(2)", "maps_to_ddl": "RESERVE_STATUS"},
    ],
}

# Manual resolutions for critical tables (binding for Flyway / entity model).
CRITICAL_RESOLUTIONS: dict[tuple[str, str], dict[str, str]] = {
    ("CUSTOMER_T", "DOB"): {
        "cobol_host_variable": "CUST_DOB",
        "match_status": "mismatch",
        "resolution": "cust_dob",
        "resolution_rationale": (
            "G-06: DDL DOB vs COBOL CUST_DOB; canonical snake_case cust_dob for PostgreSQL"
        ),
        "drift_note": "G-06: design DOB vs COBOL CUST_DOB",
    },
    ("CUSTOMER_T", "TAX_ID"): {
        "cobol_host_variable": "CUST_SSN_TAXID",
        "match_status": "mismatch",
        "resolution": "tax_id",
        "resolution_rationale": (
            "G-06: DDL TAX_ID vs COBOL CUST_SSN_TAXID; keep tax_id (domain-neutral) as canonical"
        ),
    },
    ("CUSTOMER_T", "EMAIL"): {
        "cobol_host_variable": "CUST_EMAIL",
        "match_status": "mismatch",
        "resolution": "email",
        "resolution_rationale": (
            "G-06: DDL EMAIL vs COBOL CUST_EMAIL; strip CUST_ prefix for target email"
        ),
    },
    ("CUSTOMER_T", "FIRST_NAME"): {
        "cobol_host_variable": "",
        "match_status": "ddl-only",
        "resolution": "first_name",
        "resolution_rationale": "DDL-only in CUSTOMER_T; include in target for individual customers",
    },
    ("CUSTOMER_T", "LAST_NAME"): {
        "cobol_host_variable": "",
        "match_status": "ddl-only",
        "resolution": "last_name",
        "resolution_rationale": "DDL-only in CUSTOMER_T; include in target for name splitting",
    },
    ("CUSTOMER_T", "CUST_GENDER"): {
        "cobol_host_variable": "CUST_GENDER",
        "match_status": "cobol-only",
        "resolution": "cust_gender",
        "resolution_rationale": "COBOL-only column inserted by CUS path; add to target schema",
    },
    ("CUSTOMER_T", "CUST_MARITAL_ST"): {
        "cobol_host_variable": "CUST_MARITAL_ST",
        "match_status": "cobol-only",
        "resolution": "cust_marital_st",
        "resolution_rationale": "COBOL-only column; add to target schema",
    },
    ("CUSTOMER_T", "CUST_CREDIT_SCORE"): {
        "cobol_host_variable": "CUST_CREDIT_SCORE",
        "match_status": "cobol-only",
        "resolution": "cust_credit_score",
        "resolution_rationale": "COBOL-only column; add to target schema",
    },
    ("BILLING_SCHEDULE_T", "AMT_DUE"): {
        "cobol_host_variable": "DUE_AMT",
        "match_status": "mismatch",
        "resolution": "due_amt",
        "resolution_rationale": "Shipped code authoritative: BIL003B/PRM005B write DUE_AMT",
        "drift_note": "G-06: design AMT_DUE vs COBOL DUE_AMT (BIL003B)",
    },
    ("BILLING_SCHEDULE_T", "AMT_PAID"): {
        "cobol_host_variable": "PAID_AMT",
        "match_status": "mismatch",
        "resolution": "paid_amt",
        "resolution_rationale": "Shipped code authoritative: uses PAID_AMT not AMT_PAID",
        "drift_note": "G-06: design AMT_PAID vs COBOL PAID_AMT",
    },
    ("BILLING_SCHEDULE_T", "SCHED_STATUS"): {
        "cobol_host_variable": "BILL_STATUS",
        "match_status": "mismatch",
        "resolution": "bill_status",
        "resolution_rationale": "Code domain D/L/P via BILL_STATUS; design SCHED_STATUS O/P/V discarded",
        "drift_note": "G-06: design SCHED_STATUS vs COBOL BILL_STATUS domain D/L/P",
    },
    ("BILLING_SCHEDULE_T", "COMM_CALC_FLAG"): {
        "cobol_host_variable": "COMM_CALC_FLAG",
        "match_status": "cobol-only",
        "resolution": "comm_calc_flag",
        "resolution_rationale": "CMM001B idempotency flag absent from DDL; add to target",
    },
    ("CLAIM_RESERVE_T", "RESERVE_HIST_ID"): {
        "cobol_host_variable": "RESERVE_ID",
        "match_status": "mismatch",
        "resolution": "reserve_id",
        "resolution_rationale": "Shipped code uses RESERVE_ID; design RESERVE_HIST_ID renamed",
    },
    ("CLAIM_RESERVE_T", "RESERVE_AMT"): {
        "cobol_host_variable": "APPROVED_AMT",
        "match_status": "mismatch",
        "resolution": "approved_amt",
        "resolution_rationale": "Code APPROVED_AMT is the live reserve balance written by CLM006B",
    },
    ("CLAIM_RESERVE_T", "CHANGE_REASON"): {
        "cobol_host_variable": "",
        "match_status": "ddl-only",
        "resolution": "change_reason",
        "resolution_rationale": "Keep in history table path; optional on live reserve row",
    },
    ("CLAIM_RESERVE_T", "PAID_TO_DATE"): {
        "cobol_host_variable": "PAID_TO_DATE",
        "match_status": "cobol-only",
        "resolution": "paid_to_date",
        "resolution_rationale": "Code-witnessed on CLAIM_RESERVE_T; add to target",
    },
    ("CLAIM_PAYMENT_T", "PAYMENT_AMT"): {
        "cobol_host_variable": "HV-PMT-AMOUNT",
        "match_status": "mismatch",
        "resolution": "payment_amt",
        "resolution_rationale": "Host HV-PMT-AMOUNT maps to payment_amt NUMERIC(11,2)",
    },
    ("POLICY_T", "POL_NBR"): {
        "cobol_host_variable": "HV-POLICY-ID",
        "match_status": "mismatch",
        "resolution": "pol_nbr",
        "resolution_rationale": "DDL POL_NBR vs COBOL POLICY_ID/HV-POLICY-ID; keep pol_nbr business key",
    },
    ("POLICY_T", "PREM_ANNUAL"): {
        "cobol_host_variable": "HV-PREMIUM-AMT",
        "match_status": "mismatch",
        "resolution": "prem_annual",
        "resolution_rationale": "Host HV-PREMIUM-AMT (S9(9)V99) → prem_annual NUMERIC(9,2)",
    },
    ("AUDIT_LOG_T", "USER_ID"): {
        "cobol_host_variable": "HV-USER-ID",
        "match_status": "match",
        "resolution": "user_id",
        "resolution_rationale": "Canonicalize on USER_ID (CUS/AUD002B); CHG_USER is alias",
    },
    ("AUDIT_LOG_T", "LOG_TIMESTAMP"): {
        "cobol_host_variable": "HV-CUTOFF-DATE",
        "match_status": "mismatch",
        "resolution": "log_timestamp",
        "resolution_rationale": "AUD002B filters LOG_TIMESTAMP; prefer over CRT_TIMESTAMP synonym",
        "drift_note": "G-06: AUD002B cutoff host vs CRT_TIMESTAMP synonym",
    },
    ("RPT_RUN_LOG_T", "RUN_LOG_ID"): {
        "cobol_host_variable": "",
        "match_status": "ddl-only",
        "resolution": "run_log_id",
        "resolution_rationale": "WO-237 identity PK; no COBOL host (generated on INSERT)",
    },
    ("RPT_RUN_LOG_T", "PGM_NAME"): {
        "cobol_host_variable": "WS-RL-PGM-NAME",
        "match_status": "mismatch",
        "resolution": "pgm_name",
        "resolution_rationale": "WO-237: design PROGRAM_NAME vs COBOL WS-RL-PGM-NAME / PGM_NAME",
        "drift_note": "G-06: legacy design PROGRAM_NAME discarded for PGM_NAME",
    },
    ("RPT_RUN_LOG_T", "RUN_DATE"): {
        "cobol_host_variable": "WS-RL-RUN-DATE",
        "match_status": "mismatch",
        "resolution": "run_date",
        "resolution_rationale": "Batch run date from WS-RL-RUN-DATE working storage",
    },
    ("RPT_RUN_LOG_T", "REC_SELECTED"): {
        "cobol_host_variable": "WS-RL-SELECTED",
        "match_status": "mismatch",
        "resolution": "rec_selected",
        "resolution_rationale": "WO-237: design ROWS_PROCESSED replaced by REC_SELECTED counter",
        "drift_note": "G-06: ROWS_PROCESSED design column superseded by REC_SELECTED",
    },
    ("RPT_RUN_LOG_T", "REC_UPDATED"): {
        "cobol_host_variable": "WS-RL-UPDATED",
        "match_status": "mismatch",
        "resolution": "rec_updated",
        "resolution_rationale": "Batch programs write WS-RL-UPDATED into REC_UPDATED",
    },
    ("RPT_RUN_LOG_T", "REC_ERRORS"): {
        "cobol_host_variable": "WS-RL-ERRORS",
        "match_status": "mismatch",
        "resolution": "rec_errors",
        "resolution_rationale": "Batch programs write WS-RL-ERRORS into REC_ERRORS",
    },
    ("RPT_RUN_LOG_T", "REC_DELINQUENT"): {
        "cobol_host_variable": "WS-RL-DELINQUENT",
        "match_status": "mismatch",
        "resolution": "rec_delinquent",
        "resolution_rationale": "PRM005B-only delinquency counter; nullable in Flyway",
    },
    ("RPT_RUN_LOG_T", "START_TIMESTAMP"): {
        "cobol_host_variable": "WS-START-TIMESTAMP",
        "match_status": "mismatch",
        "resolution": "start_timestamp",
        "resolution_rationale": "WO-237 wall-clock timing captured at batch initialize",
    },
    ("RPT_RUN_LOG_T", "END_TIMESTAMP"): {
        "cobol_host_variable": "WS-END-TIMESTAMP",
        "match_status": "mismatch",
        "resolution": "end_timestamp",
        "resolution_rationale": "WO-237 wall-clock timing captured at 8000-WRITE-RUN-LOG",
    },
    ("RPT_RUN_LOG_T", "CRT_TIMESTAMP"): {
        "cobol_host_variable": "",
        "match_status": "ddl-only",
        "resolution": "crt_timestamp",
        "resolution_rationale": "Row create timestamp; default CURRENT_TIMESTAMP in Flyway",
    },
}


@dataclass
class DdlColumn:
    name: str
    data_type: str
    description: str = ""
    constraints: str = ""


@dataclass
class DdlTable:
    name: str
    columns: list[DdlColumn] = field(default_factory=list)
    domain: str = ""
    source: str = "markdown"


@dataclass
class CobolColumnRef:
    table: str
    host_variable: str
    sql_column: str = ""
    pic: str = ""
    program_id: str = ""


# ---------------------------------------------------------------------------
# YAML helpers (stdlib)
# ---------------------------------------------------------------------------

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
    }:
        return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'
    return s


def dump_yaml(data: Any, indent: int = 0) -> str:
    sp = "  " * indent
    if isinstance(data, dict):
        if not data:
            return f"{sp}{{}}" if indent else "{}"
        lines: list[str] = []
        for k, v in data.items():
            if isinstance(v, (dict, list)):
                if not v and isinstance(v, list):
                    lines.append(f"{sp}{k}: []")
                elif not v and isinstance(v, dict):
                    lines.append(f"{sp}{k}: {{}}")
                else:
                    lines.append(f"{sp}{k}:")
                    lines.append(dump_yaml(v, indent + 1))
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
                        lines.append(dump_yaml(v, indent + 2))
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


def load_simple_yaml(text: str) -> Any:
    """Minimal indented YAML loader for dictionary documents we emit.

    Stack entries are (content_indent_threshold, container): children must have
    indent >= threshold to belong to that container. List-item maps use the
    indent of the '-' marker + 2 as the threshold for continuation keys.
    """
    lines: list[str] = []
    for raw in text.splitlines():
        if not raw.strip() or raw.lstrip().startswith("#"):
            continue
        lines.append(raw.rstrip())

    def parse_scalar(s: str) -> Any:
        s = s.strip()
        if s in ("null", "~"):
            return None
        if s in ("true", "false"):
            return s == "true"
        if s == "[]":
            return []
        if s == "{}":
            return {}
        if (s.startswith('"') and s.endswith('"')) or (s.startswith("'") and s.endswith("'")):
            return s[1:-1].replace('\\"', '"').replace("\\\\", "\\")
        if re.fullmatch(r"-?\d+", s):
            return int(s)
        if re.fullmatch(r"-?\d+\.\d+", s):
            return float(s)
        return s

    root: dict[str, Any] = {}
    # (min_child_indent, container)
    stack: list[tuple[int, Any]] = [(0, root)]

    def container_for(indent: int) -> Any:
        while len(stack) > 1 and indent < stack[-1][0]:
            stack.pop()
        return stack[-1][1]

    i = 0
    while i < len(lines):
        line = lines[i]
        indent = len(line) - len(line.lstrip(" "))
        content = line.lstrip(" ")
        container = container_for(indent)

        if content.startswith("- "):
            item_body = content[2:]
            if not isinstance(container, list):
                raise ValueError(f"List item under non-list at line: {line}")
            if ":" in item_body:
                key, _, rest = item_body.partition(":")
                key = key.strip()
                rest = rest.strip()
                obj: dict[str, Any] = {}
                container.append(obj)
                # continuation keys for this list item align at indent+2
                stack.append((indent + 2, obj))
                if rest:
                    obj[key] = parse_scalar(rest)
                else:
                    if i + 1 < len(lines):
                        nxt = lines[i + 1]
                        nind = len(nxt) - len(nxt.lstrip(" "))
                        ncontent = nxt.lstrip(" ")
                        if nind >= indent + 2:
                            nested: Any = [] if ncontent.startswith("- ") else {}
                            obj[key] = nested
                            stack.append((nind if ncontent.startswith("- ") else nind, nested))
                        else:
                            obj[key] = None
                    else:
                        obj[key] = None
            else:
                container.append(parse_scalar(item_body))
            i += 1
            continue

        if ":" in content:
            key, _, rest = content.partition(":")
            key = key.strip()
            rest = rest.strip()
            if not isinstance(container, dict):
                raise ValueError(f"Key under non-dict at line: {line}")
            if rest == "":
                if i + 1 < len(lines):
                    nxt = lines[i + 1]
                    nind = len(nxt) - len(nxt.lstrip(" "))
                    ncontent = nxt.lstrip(" ")
                    if nind > indent:
                        nested = [] if ncontent.startswith("- ") else {}
                        container[key] = nested
                        # list/dict children live at nind
                        stack.append((nind, nested))
                    else:
                        container[key] = None
                else:
                    container[key] = None
            else:
                container[key] = parse_scalar(rest)
            i += 1
            continue

        i += 1

    return root


# ---------------------------------------------------------------------------
# DdlParser
# ---------------------------------------------------------------------------

class DdlParser:
    """Parse markdown tables and CREATE TABLE blocks from PCIS_Database_Design.md."""

    HEADING_RE = re.compile(r"^###\s+([A-Z][A-Z0-9_]*_T)\s*$")
    DOMAIN_RE = re.compile(r"\*\*Domain:\*\*\s*(\w+)")
    MD_ROW_RE = re.compile(
        r"^\|\s*([A-Z][A-Z0-9_]*)\s*\|\s*([^|]+?)\s*\|\s*(.*?)\s*\|\s*$"
    )
    CREATE_RE = re.compile(
        r"CREATE\s+TABLE\s+([A-Z][A-Z0-9_]*_T)\s*\((.*?)\)\s*;",
        re.I | re.S,
    )
    COL_DEF_RE = re.compile(
        r"^\s*([A-Z][A-Z0-9_]*)\s+([A-Z]+(?:\s*\(\s*\d+(?:\s*,\s*\d+)?\s*\))?)",
        re.I,
    )

    def parse(self, path: Path) -> dict[str, DdlTable]:
        text = path.read_text(encoding="utf-8")
        tables: dict[str, DdlTable] = {}

        # Markdown ### sections with column tables
        current: Optional[DdlTable] = None
        in_col_table = False
        for line in text.splitlines():
            hm = self.HEADING_RE.match(line.strip())
            if hm:
                current = DdlTable(name=hm.group(1), source="markdown")
                tables[current.name] = current
                in_col_table = False
                continue
            if current is None:
                continue
            dm = self.DOMAIN_RE.search(line)
            if dm:
                current.domain = dm.group(1)
            if line.strip().startswith("| Column ") or line.strip().startswith("|Column "):
                in_col_table = True
                continue
            if in_col_table:
                if not line.strip().startswith("|"):
                    in_col_table = False
                    continue
                if re.match(r"^\|\s*-+", line.strip()):
                    continue
                rm = self.MD_ROW_RE.match(line.strip())
                if rm:
                    col = rm.group(1).upper()
                    if col.upper() == "COLUMN":
                        continue
                    dtype = rm.group(2).strip()
                    desc = rm.group(3).strip()
                    if not any(c.name == col for c in current.columns):
                        current.columns.append(DdlColumn(col, dtype, desc))

        # CREATE TABLE blocks (may enrich / override types)
        for m in self.CREATE_RE.finditer(text):
            tname = m.group(1).upper()
            body = m.group(2)
            table = tables.get(tname) or DdlTable(name=tname, source="create_table")
            table.source = "create_table" if tname not in tables else "markdown+create_table"
            for raw_line in body.split(","):
                chunk = raw_line.strip()
                if not chunk or chunk.upper().startswith("PRIMARY KEY"):
                    continue
                cm = self.COL_DEF_RE.match(chunk)
                if not cm:
                    continue
                cname = cm.group(1).upper()
                ctype = re.sub(r"\s+", "", cm.group(2).upper())
                # normalize DECIMAL(9, 2) → DECIMAL(9,2)
                ctype = re.sub(r"\(\s*", "(", ctype)
                ctype = re.sub(r"\s*,\s*", ",", ctype)
                ctype = re.sub(r"\s*\)", ")", ctype)
                constraints = ""
                if "NOT NULL" in chunk.upper():
                    constraints = "NOT NULL"
                existing = next((c for c in table.columns if c.name == cname), None)
                if existing:
                    existing.data_type = ctype or existing.data_type
                    if constraints:
                        existing.constraints = constraints
                else:
                    table.columns.append(DdlColumn(cname, ctype, constraints=constraints))
            tables[tname] = table

        return tables


# ---------------------------------------------------------------------------
# BaselineColumnExtractor
# ---------------------------------------------------------------------------

class BaselineColumnExtractor:
    """Extract host-variable / table mappings from baseline YAML or COBOL sources."""

    SQL_BLOCK_RE = re.compile(r"EXEC\s+SQL(.*?)END-EXEC", re.I | re.S)
    PIC_RE = re.compile(
        r"^\s*0?5\s+(HV-[\w-]+)\s+PIC\s+([^\.]+)\.",
        re.I | re.M,
    )
    HV_RE = re.compile(r":([A-Z][A-Z0-9-]*)", re.I)
    TABLE_RE = re.compile(
        r"(?:INTO|FROM|UPDATE|JOIN|DELETE\s+FROM)\s+(?:[A-Z][A-Z0-9_]*\.)?([A-Z][A-Z0-9_]*_T)\b",
        re.I,
    )

    def extract(
        self,
        baseline_path: Optional[Path] = None,
        cobol_dir: Optional[Path] = None,
    ) -> list[CobolColumnRef]:
        refs: list[CobolColumnRef] = []
        pic_map: dict[str, str] = {}

        if cobol_dir and cobol_dir.is_dir():
            for path in sorted(cobol_dir.glob("*.cbl")):
                text = path.read_text(encoding="utf-8", errors="replace")
                for m in self.PIC_RE.finditer(text):
                    pic_map[m.group(1).upper()] = " ".join(m.group(2).split())
                refs.extend(self._from_cobol_text(path.stem, text, pic_map))

        if baseline_path and baseline_path.is_file():
            refs.extend(self._from_baseline(baseline_path, pic_map))

        # G-06 witnesses
        for table, items in G06_COBOL_WITNESS.items():
            for item in items:
                refs.append(
                    CobolColumnRef(
                        table=table,
                        host_variable=item["name"],
                        sql_column=item["name"],
                        pic=item.get("pic", ""),
                        program_id="G06-WITNESS",
                    )
                )

        return self._dedupe(refs)

    def _dedupe(self, refs: Iterable[CobolColumnRef]) -> list[CobolColumnRef]:
        seen: set[tuple[str, str, str]] = set()
        out: list[CobolColumnRef] = []
        for r in refs:
            key = (r.table, r.host_variable, r.sql_column)
            if key in seen:
                continue
            seen.add(key)
            out.append(r)
        return out

    def _canonicalize_table(self, name: str) -> str:
        up = name.upper().strip().strip('"')
        # reject schema/library qualifiers and non-table tokens
        if not re.fullmatch(r"[A-Z][A-Z0-9_]*_T", up):
            return ""
        # false positives from TIMESTAMP / LAST_UPDATE_TS fragments
        if up in {
            "CREATE_T",
            "CURRENT_T",
            "LAST_UPDATE_T",
            "UPDATE_T",
            "PAID_T",
            "CUST_T",
            "PLAN_T",
            "LOG_T",
            "ADDR_T",
            "TIME_T",
        }:
            return ""
        return TABLE_ALIASES.get(up, up)

    def _from_cobol_text(
        self, program_id: str, text: str, pic_map: dict[str, str]
    ) -> list[CobolColumnRef]:
        refs: list[CobolColumnRef] = []
        for m in self.SQL_BLOCK_RE.finditer(text):
            sql = re.sub(r"\s+", " ", m.group(1))
            tables = [
                t
                for t in (self._canonicalize_table(x) for x in self.TABLE_RE.findall(sql))
                if t and not t.startswith("SYS")
            ]
            hvs = [h.upper() for h in self.HV_RE.findall(sql)]
            sql_cols = self._sql_columns(sql)
            for t in tables:
                for hv in hvs:
                    # skip non-column scratch hosts that never map to table columns
                    if hv in {"WS-RETENTION-DAYS", "WS-CHUNK-SIZE"}:
                        continue
                    guessed = self._hv_to_column(hv)
                    col = ""
                    for sc in sql_cols:
                        if self._normalize(sc) == self._normalize(guessed) or sc in hv:
                            col = sc
                            break
                    refs.append(
                        CobolColumnRef(
                            table=t,
                            host_variable=hv,
                            sql_column=col or guessed,
                            pic=pic_map.get(hv, ""),
                            program_id=program_id,
                        )
                    )
                for sc in sql_cols:
                    refs.append(
                        CobolColumnRef(
                            table=t,
                            host_variable=sc,
                            sql_column=sc,
                            pic="",
                            program_id=program_id,
                        )
                    )
        return refs

    def _sql_columns(self, sql: str) -> list[str]:
        cols: list[str] = []
        for m in re.finditer(
            r"INSERT\s+INTO\s+(?:INSPRDDTA\.)?[A-Z][A-Z0-9_]*_T\s*\(([^)]+)\)",
            sql,
            re.I,
        ):
            cols.extend(c.strip().upper() for c in m.group(1).split(",") if c.strip())
        for m in re.finditer(
            r"UPDATE\s+(?:INSPRDDTA\.)?[A-Z][A-Z0-9_]*_T\s+SET\s+(.+?)(?:\s+WHERE|\s*$)",
            sql,
            re.I,
        ):
            cols.extend(
                cm.group(1).upper()
                for cm in re.finditer(r"([A-Z][A-Z0-9_]*)\s*=", m.group(1), re.I)
            )
        # SELECT list simple identifiers
        sm = re.search(r"SELECT\s+(.+?)\s+INTO\s+", sql, re.I)
        if sm:
            for part in sm.group(1).split(","):
                part = part.strip()
                # C.COL or COL
                cm = re.search(r"([A-Z][A-Z0-9_]*)$", part, re.I)
                if cm and cm.group(1).upper() not in {"SELECT", "FROM"}:
                    cols.append(cm.group(1).upper())
        return cols

    def _from_baseline(
        self, path: Path, pic_map: dict[str, str]
    ) -> list[CobolColumnRef]:
        """Regex extract from baseline YAML — avoids a full YAML dependency."""
        refs: list[CobolColumnRef] = []
        text = path.read_text(encoding="utf-8")
        current_program = ""
        current_table = ""
        pending_hvs: list[str] = []
        in_host_vars = False

        for raw in text.splitlines():
            line = raw.rstrip()
            m_prog = re.match(r"^\s+- program_id:\s*(\S+)", line)
            if m_prog:
                current_program = m_prog.group(1).strip().strip('"')
                in_host_vars = False
                continue
            m_table = re.match(r"^\s+target_table:\s*(\S+)", line)
            if m_table:
                current_table = m_table.group(1).strip().strip('"')
                if current_table.lower() in {"null", "none", "~"}:
                    current_table = ""
                in_host_vars = False
                pending_hvs = []
                continue
            if re.match(r"^\s+host_variables:\s*\[\]\s*$", line):
                in_host_vars = False
                pending_hvs = []
                continue
            if re.match(r"^\s+host_variables:\s*$", line):
                in_host_vars = True
                pending_hvs = []
                continue
            if in_host_vars:
                m_hv = re.match(r"^\s+- ([A-Za-z0-9_-]+)\s*$", line)
                if m_hv:
                    pending_hvs.append(m_hv.group(1).upper())
                    continue
                in_host_vars = False
            m_preview = re.match(r"^\s+sql_preview:\s*(.*)$", line)
            if m_preview:
                preview = m_preview.group(1).strip()
                if preview.startswith('"') and preview.endswith('"'):
                    preview = preview[1:-1].replace('\\"', '"')
                if current_table and not current_table.upper().startswith("SYS"):
                    ct = self._canonicalize_table(current_table)
                    tables = [ct] if ct else []
                else:
                    tables = [
                        t
                        for t in (
                            self._canonicalize_table(x)
                            for x in self.TABLE_RE.findall(preview)
                        )
                        if t and not t.startswith("SYS")
                    ]
                sql_cols = self._sql_columns(preview) if preview else []
                hvs = list(pending_hvs) or [
                    h.upper() for h in self.HV_RE.findall(preview)
                ]
                for t in tables:
                    for hv in hvs:
                        refs.append(
                            CobolColumnRef(
                                table=t,
                                host_variable=hv,
                                sql_column=self._hv_to_column(hv),
                                pic=pic_map.get(hv, ""),
                                program_id=current_program,
                            )
                        )
                    for sc in sql_cols:
                        refs.append(
                            CobolColumnRef(
                                table=t,
                                host_variable=sc,
                                sql_column=sc,
                                pic="",
                                program_id=current_program,
                            )
                        )
                pending_hvs = []
        return refs

    @staticmethod
    def _hv_to_column(hv: str) -> str:
        name = hv.upper()
        for prefix in ("HV-", "WS-", "HOST-"):
            if name.startswith(prefix):
                name = name[len(prefix) :]
                break
        return name.replace("-", "_")

    @staticmethod
    def _normalize(name: str) -> str:
        # Delegate to reconciliation synonyms so validators share one vocabulary
        return ReconciliationEngine._normalize_name(name)


# ---------------------------------------------------------------------------
# ReconciliationEngine
# ---------------------------------------------------------------------------

class ReconciliationEngine:
    PREFIXES = ("CUST_", "HV_", "WS_", "PMT_", "SCHED_", "AGT_")

    def __init__(self, manual: Optional[dict[tuple[str, str], dict[str, str]]] = None):
        self.manual = manual or CRITICAL_RESOLUTIONS

    def reconcile(
        self,
        ddl_tables: dict[str, DdlTable],
        cobol_refs: list[CobolColumnRef],
    ) -> dict[str, Any]:
        cobol_by_table: dict[str, list[CobolColumnRef]] = defaultdict(list)
        for r in cobol_refs:
            cobol_by_table[r.table].append(r)

        tables_out: list[dict[str, Any]] = []
        counts = {"match": 0, "mismatch": 0, "ddl-only": 0, "cobol-only": 0}

        for tname in sorted(ddl_tables):
            ddl = ddl_tables[tname]
            refs = cobol_by_table.get(tname, [])
            used_cobol: set[str] = set()
            columns_out: list[dict[str, Any]] = []
            tier_value = table_tier(tname, ddl.domain)

            def emit(**kwargs: Any) -> dict[str, Any]:
                return self._column_entry(
                    table_name=tname,
                    table_tier_value=tier_value,
                    **kwargs,
                )

            # Apply critical cobol-only keys that use synthetic ddl names
            for (mt, key), res in self.manual.items():
                if mt != tname:
                    continue
                if res.get("match_status") == "cobol-only":
                    # ensure present even if not in DDL
                    if not any(c.name == key for c in ddl.columns):
                        entry = emit(
                            ddl_column_name="",
                            cobol_host_variable=res.get("cobol_host_variable") or key,
                            ddl_data_type="",
                            cobol_pic=self._pic_for(refs, res.get("cobol_host_variable") or key),
                            match_status="cobol-only",
                            resolution=res["resolution"],
                            resolution_rationale=res["resolution_rationale"],
                            drift_note=res.get("drift_note", ""),
                        )
                        columns_out.append(entry)
                        counts["cobol-only"] += 1
                        used_cobol.add((res.get("cobol_host_variable") or key).upper())

            for col in ddl.columns:
                manual = self.manual.get((tname, col.name))
                if manual and manual.get("match_status") == "cobol-only":
                    # Pure cobol-only rows without a DDL column were emitted above.
                    if not col.name:
                        continue

                cobol_match = self._find_cobol(col.name, refs, used_cobol)
                drift_note = ""
                if manual:
                    status = manual["match_status"]
                    hv = manual.get("cobol_host_variable") or (
                        cobol_match.host_variable if cobol_match else ""
                    )
                    resolution = manual["resolution"]
                    rationale = manual["resolution_rationale"]
                    drift_note = manual.get("drift_note", "")
                    if cobol_match:
                        used_cobol.add(cobol_match.host_variable.upper())
                        if cobol_match.sql_column:
                            used_cobol.add(cobol_match.sql_column.upper())
                    if hv:
                        used_cobol.add(hv.upper())
                    pic = self._pic_for(refs, hv) or (
                        cobol_match.pic if cobol_match else ""
                    )
                elif cobol_match is None:
                    status = "ddl-only"
                    hv = ""
                    resolution = col.name.lower()
                    rationale = "No COBOL host variable reference; include from DDL design"
                    pic = ""
                else:
                    used_cobol.add(cobol_match.host_variable.upper())
                    if cobol_match.sql_column:
                        used_cobol.add(cobol_match.sql_column.upper())
                    hv = cobol_match.host_variable
                    pic = cobol_match.pic
                    ddl_u = col.name.upper()
                    sql_u = (cobol_match.sql_column or "").upper()
                    hv_u = cobol_match.host_variable.upper()
                    exact_sql = sql_u == ddl_u
                    same_norm = self._normalize(col.name) == self._normalize(
                        cobol_match.sql_column or cobol_match.host_variable
                    )
                    if exact_sql or (
                        same_norm and ddl_u in {sql_u, hv_u, self._hv_plain(hv_u)}
                    ):
                        status = "match"
                        resolution = col.name.lower()
                        rationale = "DDL column name aligns with COBOL reference"
                    else:
                        status = "mismatch"
                        resolution = self._suggest_resolution(col.name, hv)
                        rationale = (
                            f"Name drift {col.name} ↔ {hv}"
                            + (f" / {cobol_match.sql_column}" if cobol_match.sql_column else "")
                            + f"; canonical {resolution}"
                        )
                        if status == "mismatch":
                            drift_note = f"G-06: {col.name} vs COBOL {hv}"

                target_pg = self.target_pg_type(col.data_type, pic)
                entry = emit(
                    ddl_column_name=col.name,
                    cobol_host_variable=hv,
                    ddl_data_type=col.data_type,
                    cobol_pic=pic,
                    match_status=status,
                    resolution=resolution,
                    resolution_rationale=rationale,
                    target_pg_type=target_pg,
                    drift_note=drift_note,
                )
                columns_out.append(entry)
                counts[status] = counts.get(status, 0) + 1

            # Remaining cobol-only refs
            for r in refs:
                tokens = {r.host_variable.upper(), (r.sql_column or "").upper()}
                if tokens & used_cobol:
                    continue
                if not r.host_variable:
                    continue
                # skip pure scratch / non-persistent hosts
                if r.host_variable.upper().startswith("WS-") and "HV-" not in r.host_variable.upper():
                    if r.host_variable.upper() in {
                        "WS-RETENTION-DAYS",
                        "WS-CHUNK-SIZE",
                        "WS-LEAD-DAYS",
                        "WS-GRACE-DAYS",
                    }:
                        continue
                key = (tname, r.host_variable.upper())
                # already emitted as cobol-only via manual under synthetic key?
                if any(
                    (c.get("cobol_host_variable") or "").upper() == r.host_variable.upper()
                    for c in columns_out
                ):
                    continue
                manual = self.manual.get((tname, r.host_variable.upper())) or self.manual.get(
                    (tname, r.sql_column.upper() if r.sql_column else "")
                )
                if manual and manual.get("match_status") == "cobol-only":
                    continue  # already handled
                pic = r.pic
                resolution = self._suggest_resolution("", r.host_variable)
                rationale = "COBOL reference with no DDL column; review for schema add/exclude"
                status = "cobol-only"
                entry = emit(
                    ddl_column_name="",
                    cobol_host_variable=r.host_variable,
                    ddl_data_type="",
                    cobol_pic=pic,
                    match_status=status,
                    resolution=resolution,
                    resolution_rationale=rationale,
                    target_pg_type=self.target_pg_type("", pic),
                )
                columns_out.append(entry)
                counts["cobol-only"] += 1
                used_cobol.add(r.host_variable.upper())

            tables_out.append(
                {
                    "table_name": tname,
                    "domain": ddl.domain,
                    "classification_tier": tier_value,
                    "column_count": len(columns_out),
                    "columns": columns_out,
                }
            )

        # COBOL tables not in DDL
        orphan_tables = sorted(set(cobol_by_table) - set(ddl_tables) - {"SYSDUMMY1"})
        return {
            "generation_timestamp": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
            "generator": "docs/build_data_dictionary.py",
            "woref": "WO-150",
            "source_ddl": str(DEFAULT_DDL.relative_to(REPO_ROOT)),
            "source_baseline": "baseline/cobol-baseline.yaml",
            "source_flyway": str(DEFAULT_FLYWAY.relative_to(REPO_ROOT)),
            "table_count": len(tables_out),
            "summary": {
                "tables": len(tables_out),
                "match": counts.get("match", 0),
                "mismatch": counts.get("mismatch", 0),
                "ddl_only": counts.get("ddl-only", 0),
                "cobol_only": counts.get("cobol-only", 0),
                "orphan_cobol_tables": orphan_tables,
            },
            "tables": tables_out,
        }

    def _find_cobol(
        self,
        ddl_col: str,
        refs: list[CobolColumnRef],
        used: set[str],
    ) -> Optional[CobolColumnRef]:
        ddl_n = self._normalize(ddl_col)
        # exact sql column
        for r in refs:
            if r.host_variable.upper() in used:
                continue
            if r.sql_column and r.sql_column.upper() == ddl_col.upper():
                return r
        for r in refs:
            if r.host_variable.upper() in used:
                continue
            if self._normalize(r.sql_column or r.host_variable) == ddl_n:
                return r
        # substring / token overlap
        best = None
        best_score = 0
        for r in refs:
            if r.host_variable.upper() in used:
                continue
            rn = self._normalize(r.host_variable)
            score = 0
            if rn == ddl_n:
                score = 100
            elif ddl_n in rn or rn in ddl_n:
                score = 50 + min(len(rn), len(ddl_n))
            if score > best_score:
                best_score = score
                best = r
        return best if best_score >= 50 else None

    def _pic_for(self, refs: list[CobolColumnRef], hv: str) -> str:
        if not hv:
            return ""
        for r in refs:
            if r.host_variable.upper() == hv.upper() and r.pic:
                return r.pic
            if self._normalize(r.host_variable) == self._normalize(hv) and r.pic:
                return r.pic
        # G06 witness pics
        return ""

    def _column_entry(
        self,
        ddl_column_name: str,
        cobol_host_variable: str,
        ddl_data_type: str,
        cobol_pic: str,
        match_status: str,
        resolution: str,
        resolution_rationale: str,
        target_pg_type: str = "",
        table_name: str = "",
        table_tier_value: str = "Internal",
        drift_note: str = "",
    ) -> dict[str, Any]:
        if not target_pg_type:
            target_pg_type = self.target_pg_type(ddl_data_type, cobol_pic)
        class_info = column_classification(
            table_name, ddl_column_name, resolution, table_tier_value
        )
        entry: dict[str, Any] = {
            "ddl_column_name": ddl_column_name,
            "cobol_host_variable": cobol_host_variable,
            "data_type": ddl_data_type,
            "ddl_data_type": ddl_data_type,
            "pic_definition": cobol_pic,
            "cobol_pic": cobol_pic,
            "match_status": match_status,
            "resolution": resolution,
            "target_pg_type": target_pg_type,
            "resolution_rationale": resolution_rationale,
            "pii": class_info["pii"],
            "mask_strategy": class_info["mask_strategy"],
        }
        if drift_note:
            entry["drift_note"] = drift_note
        return entry

    @staticmethod
    def _hv_plain(hv: str) -> str:
        n = hv.upper().replace("-", "_")
        for prefix in ("HV_", "WS_", "HOST_"):
            if n.startswith(prefix):
                n = n[len(prefix) :]
        return n

    def _suggest_resolution(self, ddl_col: str, cobol_hv: str) -> str:
        base = ddl_col or cobol_hv
        n = base.upper()
        for prefix in ("HV-", "HV_", "WS-", "WS_"):
            if n.startswith(prefix):
                n = n[len(prefix) :]
        n = n.replace("-", "_")
        # prefer shorter ddl-ish name without CUST_ when both sides agree semantically
        if ddl_col:
            return ddl_col.lower()
        for prefix in self.PREFIXES:
            if n.startswith(prefix) and len(n) > len(prefix) + 1:
                # keep CUST_ for gender/marital/credit which are cobol-only business fields
                if n in {"CUST_GENDER", "CUST_MARITAL_ST", "CUST_CREDIT_SCORE"}:
                    break
                if n.startswith("CUST_") and n[5:] in {
                    "DOB",
                    "EMAIL",
                    "PHONE",
                    "SSN_TAXID",
                    "NAME",
                    "TYPE",
                    "STATUS",
                }:
                    n = n[5:]
                    if n == "SSN_TAXID":
                        n = "TAX_ID"
                    break
        return n.lower()

    def _normalize(self, name: str) -> str:
        return self._normalize_name(name)

    @staticmethod
    def _normalize_name(name: str) -> str:
        n = (name or "").upper().replace("-", "_")
        for prefix in ("HV_", "WS_", "HOST_"):
            if n.startswith(prefix):
                n = n[len(prefix) :]
        synonyms = {
            "BIRTH_DATE": "DOB",
            "DOB": "DOB",
            "SSN_TAXID": "TAX_ID",
            "TAXID": "TAX_ID",
            "AMT_DUE": "DUE_AMT",
            "DUE_AMT": "DUE_AMT",
            "AMT_PAID": "PAID_AMT",
            "PAID_AMT": "PAID_AMT",
            "SCHED_STATUS": "BILL_STATUS",
            "BILL_STATUS": "BILL_STATUS",
            "RESERVE_HIST_ID": "RESERVE_ID",
            "RESERVE_ID": "RESERVE_ID",
            "RESERVE_AMT": "APPROVED_AMT",
            "APPROVED_AMT": "APPROVED_AMT",
            "POLICY_ID": "POL_NBR",
            "POL_NBR": "POL_NBR",
            "CUSTOMER_ID": "CUST_ID",
            "CUST_ID": "CUST_ID",
            "PREMIUM_AMT": "PREM_ANNUAL",
            "ANNUAL_PREMIUM": "PREM_ANNUAL",
            "PREM_ANNUAL": "PREM_ANNUAL",
            "PAYMENT_AMOUNT": "PAYMENT_AMT",
            "PAYMENT_AMT": "PAYMENT_AMT",
            "EMAIL_ADDRESS": "EMAIL",
            "PHONE_NUMBER": "PHONE",
        }
        if n in synonyms:
            return synonyms[n]
        for prefix in ("CUST_", "PMT_"):
            if n.startswith(prefix):
                rest = n[len(prefix) :]
                return synonyms.get(rest, rest)
        return synonyms.get(n, n)

    @staticmethod
    def target_pg_type(ddl_type: str, cobol_pic: str) -> str:
        pic = (cobol_pic or "").upper().replace(" ", "")
        # Monetary COMP-3 patterns
        if re.search(r"S9\(11\)V9{2}", pic) or re.search(r"S9\(11\)V99", pic):
            return "NUMERIC(11,2)"
        if re.search(r"S9\(9\)V9{2}", pic) or re.search(r"S9\(9\)V99", pic) or re.search(
            r"S9\(9\)V9\(2\)", pic
        ):
            return "NUMERIC(9,2)"
        if re.search(r"S9\(5\)V9\(4\)", pic) or re.search(r"S9\(3\)V9{4}", pic):
            return "NUMERIC(7,4)" if "S9(5)" in pic or "S9(5)" in (cobol_pic or "").upper() else "NUMERIC(7,4)"

        dt = (ddl_type or "").upper().replace(" ", "")
        m = re.match(r"DECIMAL\((\d+),(\d+)\)", dt)
        if m:
            return f"NUMERIC({m.group(1)},{m.group(2)})"
        m = re.match(r"NUMERIC\((\d+),(\d+)\)", dt)
        if m:
            return f"NUMERIC({m.group(1)},{m.group(2)})"
        m = re.match(r"VARCHAR\((\d+)\)", dt)
        if m:
            return f"VARCHAR({m.group(1)})"
        m = re.match(r"CHAR\((\d+)\)", dt)
        if m:
            return f"CHAR({m.group(1)})"
        if dt in {"DATE", "TIMESTAMP", "INTEGER", "BIGINT"}:
            return dt
        if "COMP-3" in pic and "V" in pic:
            # fallback monetary
            return "NUMERIC(9,2)"
        if dt:
            return dt
        if pic.startswith("X("):
            m = re.search(r"X\((\d+)\)", pic)
            if m:
                return f"VARCHAR({m.group(1)})"
        return "TEXT"


# ---------------------------------------------------------------------------
# Orchestration
# ---------------------------------------------------------------------------

# Flyway-only tables not in the 55-table design inventory (WO-149 / WO-150).
FLYWAY_EXTRA_TABLES = frozenset({"COMMISSION_LEDGER_T", "OUTBOX_EVENTS"})


def merge_flyway_tables(
    ddl_tables: dict[str, DdlTable],
    flyway_path: Path,
) -> dict[str, DdlTable]:
    if not flyway_path.is_file():
        return ddl_tables
    flyway = parse_flyway_schema(flyway_path)
    merged = dict(ddl_tables)
    for tname, ft in flyway.items():
        if tname in merged:
            existing = {c.name for c in merged[tname].columns}
            for fc in ft.columns:
                if fc.name not in existing:
                    merged[tname].columns.append(DdlColumn(fc.name, fc.data_type))
            continue
        if tname not in FLYWAY_EXTRA_TABLES:
            continue
        domain = {
            "COMMISSION_LEDGER_T": "AGT",
            "OUTBOX_EVENTS": "Shared",
        }.get(tname, "")
        table = DdlTable(name=tname, domain=domain, source="flyway")
        for fc in ft.columns:
            table.columns.append(DdlColumn(fc.name, fc.data_type))
        merged[tname] = table
    return merged


def build_dictionary(
    ddl_path: Path,
    baseline_path: Path,
    cobol_dir: Path,
    output_path: Path,
    flyway_path: Optional[Path] = None,
) -> dict[str, Any]:
    ddl_tables = DdlParser().parse(ddl_path)
    flyway = flyway_path or DEFAULT_FLYWAY
    ddl_tables = merge_flyway_tables(ddl_tables, flyway)
    if len(ddl_tables) < 55:
        print(
            f"WARNING: expected at least 55 DDL tables, found {len(ddl_tables)}",
            file=sys.stderr,
        )
    extractor = BaselineColumnExtractor()
    baseline = baseline_path if baseline_path.is_file() else None
    cobol_refs = extractor.extract(baseline_path=baseline, cobol_dir=cobol_dir)
    engine = ReconciliationEngine()
    doc = engine.reconcile(ddl_tables, cobol_refs)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(dump_yaml(doc) + "\n", encoding="utf-8")
    return doc


def main(argv: Optional[list[str]] = None) -> int:
    ap = argparse.ArgumentParser(description="Build PCIS data dictionary (WO-150)")
    ap.add_argument("--ddl", type=Path, default=DEFAULT_DDL)
    ap.add_argument("--baseline", type=Path, default=DEFAULT_BASELINE)
    ap.add_argument("--cobol-dir", type=Path, default=DEFAULT_COBOL_DIR)
    ap.add_argument("--flyway", type=Path, default=DEFAULT_FLYWAY)
    ap.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = ap.parse_args(argv)
    if not args.ddl.is_file():
        print(f"DDL design not found: {args.ddl}", file=sys.stderr)
        return 1
    doc = build_dictionary(
        args.ddl, args.baseline, args.cobol_dir, args.output, args.flyway
    )
    summary = doc["summary"]
    print(
        f"Wrote {args.output} — tables={doc['table_count']} "
        f"match={summary['match']} mismatch={summary['mismatch']} "
        f"ddl_only={summary['ddl_only']} cobol_only={summary['cobol_only']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
