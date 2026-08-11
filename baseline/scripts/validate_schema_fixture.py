#!/usr/bin/env python3
"""Validate baseline/test-fixtures/rpt_run_log_schema.json against reconciled DDL (WO-237)."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DDL_PATH = REPO_ROOT / "baseline" / "ddl" / "RPT_RUN_LOG_T_reconciled.sql"
FIXTURE_PATH = REPO_ROOT / "baseline" / "test-fixtures" / "rpt_run_log_schema.json"


def parse_ddl_columns(ddl_text: str) -> list[dict]:
    """Extract column definitions from CREATE TABLE RPT_RUN_LOG_T (...)."""
    match = re.search(
        r"CREATE\s+TABLE\s+RPT_RUN_LOG_T\s*\((.*)\)\s*;",
        ddl_text,
        flags=re.IGNORECASE | re.DOTALL,
    )
    if not match:
        raise ValueError("CREATE TABLE RPT_RUN_LOG_T not found in DDL")

    body = match.group(1)
    columns: list[dict] = []
    for raw_line in body.splitlines():
        line = raw_line.split("--", 1)[0].strip().rstrip(",")
        if not line:
            continue
        upper = line.upper()
        if upper.startswith("PRIMARY KEY") or upper.startswith("CONSTRAINT"):
            continue
        # Skip identity/option continuation lines such as "(START WITH 1 ...)"
        if line.startswith("(") or line.startswith(")"):
            continue
        # Column name is first token; must be a simple identifier
        parts = line.split()
        if not parts:
            continue
        name = parts[0].upper()
        if not re.match(r"^[A-Z][A-Z0-9_]*$", name):
            continue
        # Type may be TIMESTAMP(6) or VARCHAR(10) — join tokens until NOT/DEFAULT/GENERATED/PRIMARY
        type_tokens: list[str] = []
        i = 1
        while i < len(parts):
            token = parts[i]
            token_upper = token.upper()
            if token_upper in {"NOT", "DEFAULT", "GENERATED", "PRIMARY", "NULL"}:
                break
            type_tokens.append(token)
            i += 1
        col_type = re.sub(r"\s+", "", " ".join(type_tokens).upper())
        nullable = "NOT NULL" not in upper
        if "GENERATED" in upper and "IDENTITY" in upper:
            nullable = False
        columns.append(
            {
                "name": name,
                "type": col_type,
                "nullable": nullable,
                "identity": "GENERATED" in upper and "IDENTITY" in upper,
                "primary_key": "PRIMARY KEY" in upper or name == "RUN_LOG_ID",
            }
        )
    return columns


def normalize_type(type_str: str) -> str:
    return re.sub(r"\s+", "", type_str.upper())


def validate() -> int:
    ddl_text = DDL_PATH.read_text(encoding="utf-8")
    fixture = json.loads(FIXTURE_PATH.read_text(encoding="utf-8"))
    ddl_cols = parse_ddl_columns(ddl_text)
    fixture_cols = fixture.get("columns", [])

    errors: list[str] = []
    if len(ddl_cols) != len(fixture_cols):
        errors.append(
            f"column count mismatch: DDL={len(ddl_cols)} fixture={len(fixture_cols)}"
        )

    ddl_by_name = {c["name"]: c for c in ddl_cols}
    fix_by_name = {c["name"].upper(): c for c in fixture_cols}

    for name, fix_col in fix_by_name.items():
        if name not in ddl_by_name:
            errors.append(f"fixture column missing from DDL: {name}")
            continue
        ddl_col = ddl_by_name[name]
        if normalize_type(fix_col.get("type", "")) != normalize_type(ddl_col["type"]):
            errors.append(
                f"{name} type mismatch: fixture={fix_col.get('type')} ddl={ddl_col['type']}"
            )
        if bool(fix_col.get("nullable")) != bool(ddl_col["nullable"]):
            errors.append(
                f"{name} nullable mismatch: fixture={fix_col.get('nullable')} "
                f"ddl={ddl_col['nullable']}"
            )

    for name in ddl_by_name:
        if name not in fix_by_name:
            errors.append(f"DDL column missing from fixture: {name}")

    required = {
        "RUN_LOG_ID",
        "PGM_NAME",
        "RUN_DATE",
        "REC_SELECTED",
        "REC_UPDATED",
        "REC_ERRORS",
        "REC_DELINQUENT",
        "START_TIMESTAMP",
        "END_TIMESTAMP",
        "CRT_TIMESTAMP",
    }
    missing_required = required - set(ddl_by_name)
    if missing_required:
        errors.append(f"required columns missing from DDL: {sorted(missing_required)}")

    if "REC_DELINQUENT" in ddl_by_name and not ddl_by_name["REC_DELINQUENT"]["nullable"]:
        errors.append("REC_DELINQUENT must be nullable")

    if errors:
        print("FAIL: schema fixture validation errors:")
        for err in errors:
            print(f"  - {err}")
        return 1

    print(
        f"OK: fixture matches DDL ({len(ddl_cols)} columns) — "
        f"{DDL_PATH.relative_to(REPO_ROOT)} ↔ {FIXTURE_PATH.relative_to(REPO_ROOT)}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(validate())
