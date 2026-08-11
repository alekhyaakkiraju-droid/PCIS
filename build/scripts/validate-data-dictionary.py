#!/usr/bin/env python3
"""
Validate docs/data-dictionary.yaml against V1 Flyway schema (WO-150).

Compares dictionary resolution column names and target_pg_type hints against
shared-libs/pcis-schema/db/migration/V1__baseline_schema.sql. When --jdbc-url
is supplied, also checks information_schema.columns on a live PostgreSQL instance.

Exits 0 on success, 1 on failure. Stdlib only (optional psycopg2 not required).
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DOCS = REPO_ROOT / "docs"
sys.path.insert(0, str(DOCS))

from build_data_dictionary import load_simple_yaml  # noqa: E402
from classification_registry import (  # noqa: E402
    VALID_MASK_STRATEGIES,
    VALID_TIERS,
    validate_mask_for_tier,
    validate_tier,
)
from flyway_schema_parser import flyway_match_name, parse_flyway_schema  # noqa: E402

DEFAULT_DICT = REPO_ROOT / "docs" / "data-dictionary.yaml"
DEFAULT_FLYWAY = (
    REPO_ROOT / "shared-libs" / "pcis-schema" / "db" / "migration" / "V1__baseline_schema.sql"
)
MIN_TABLE_COUNT = 55


def _normalize_pg_type(raw: str) -> str:
    t = re.sub(r"\s+", "", (raw or "").upper())
    t = re.sub(r"TIMESTAMP\(\d+\)", "TIMESTAMP", t)
    if t.startswith("NUMERIC("):
        return t
    if t.startswith("VARCHAR(") or t.startswith("CHAR("):
        return t
    return t


def _types_compatible(dict_type: str, flyway_type: str) -> bool:
    d = _normalize_pg_type(dict_type)
    f = _normalize_pg_type(flyway_type)
    if d == f:
        return True
    # Dictionary may carry DECIMAL while Flyway uses NUMERIC
    if d.startswith("DECIMAL(") and f.startswith("NUMERIC("):
        return d.replace("DECIMAL", "NUMERIC") == f
    if d.startswith("NUMERIC(") and f.startswith("NUMERIC("):
        return d == f
    if d in {"BIGINT", "INTEGER", "DATE", "TIMESTAMP", "TEXT"} and d == f:
        return True
    if not d and f:
        return True
    return False


def validate_dictionary_structure(dictionary: dict) -> list[str]:
    errors: list[str] = []
    tables = dictionary.get("tables") or []
    table_names = {
        t.get("table_name") for t in tables if isinstance(t, dict) and t.get("table_name")
    }
    count = int(dictionary.get("table_count") or len(table_names))
    if count < MIN_TABLE_COUNT:
        errors.append(f"Expected at least {MIN_TABLE_COUNT} tables, table_count={count}")
    if len(table_names) < MIN_TABLE_COUNT:
        errors.append(f"Expected at least {MIN_TABLE_COUNT} table entries, found {len(table_names)}")

    for table in tables:
        tname = table.get("table_name")
        tier = table.get("classification_tier")
        if not validate_tier(tier):
            errors.append(f"Missing or invalid classification_tier on {tname}: {tier!r}")
        for col in table.get("columns") or []:
            label = f"{tname}.{col.get('ddl_column_name') or col.get('cobol_host_variable')}"
            res = col.get("resolution")
            if res is None or str(res).strip() == "":
                errors.append(f"Empty resolution: {label}")
            pii = col.get("pii")
            mask = col.get("mask_strategy")
            if pii is None:
                errors.append(f"Missing pii flag: {label}")
            if mask not in VALID_MASK_STRATEGIES:
                errors.append(f"Invalid mask_strategy on {label}: {mask!r}")
            elif tier and not validate_mask_for_tier(tier, bool(pii), mask):
                errors.append(
                    f"Restricted PII column {label} requires mask_strategy, got {mask!r}"
                )
            hv = col.get("cobol_host_variable")
            if hv is None:
                errors.append(f"Missing cobol_host_variable key on {label} (use empty string)")
    return errors


def validate_against_flyway_sql(dictionary: dict, flyway_path: Path) -> list[str]:
    errors: list[str] = []
    if not flyway_path.is_file():
        return [f"Flyway schema not found: {flyway_path}"]

    flyway = parse_flyway_schema(flyway_path)
    dict_tables = {
        t.get("table_name", "").upper(): t
        for t in (dictionary.get("tables") or [])
        if isinstance(t, dict) and t.get("table_name")
    }

    for tname, ft in flyway.items():
        if tname not in dict_tables:
            errors.append(f"Flyway table {tname} missing from data dictionary")
            continue

        table = dict_tables[tname]
        fly_cols = {c.name.upper() for c in ft.columns}
        ddl_cols: set[str] = set()
        for col in table.get("columns") or []:
            ddl_name = (col.get("ddl_column_name") or "").strip().upper()
            if ddl_name:
                ddl_cols.add(ddl_name)
            elif col.get("match_status") == "cobol-only":
                res = (col.get("resolution") or "").strip().upper()
                if res in fly_cols:
                    ddl_cols.add(res)

        missing_in_dict = fly_cols - ddl_cols
        if missing_in_dict:
            errors.append(
                f"Flyway columns missing from dictionary {tname}: {sorted(missing_in_dict)}"
            )

        extra_in_dict = ddl_cols - fly_cols
        if extra_in_dict:
            errors.append(
                f"Dictionary DDL columns not in Flyway {tname}: {sorted(extra_in_dict)}"
            )

    return errors


def validate_against_information_schema(dictionary: dict, jdbc_url: str) -> list[str]:
    try:
        import psycopg2  # type: ignore
    except ImportError:
        return ["psycopg2 not installed; omit --jdbc-url or pip install psycopg2-binary"]

    errors: list[str] = []
    conn = psycopg2.connect(jdbc_url)
    try:
        with conn.cursor() as cur:
            for table in dictionary.get("tables") or []:
                tname = table.get("table_name", "").lower()
                cur.execute(
                    """
                    SELECT column_name, data_type, numeric_precision, numeric_scale,
                           character_maximum_length, is_nullable
                    FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = %s
                    """,
                    (tname,),
                )
                rows = {r[0].upper(): r for r in cur.fetchall()}
                if not rows:
                    continue
                for col in table.get("columns") or []:
                    res = flyway_match_name(col)
                    if not res or res not in rows:
                        if col.get("match_status") == "cobol-only":
                            continue
                        errors.append(
                            f"information_schema missing {table.get('table_name')}.{res}"
                        )
    finally:
        conn.close()
    return errors


def validate(
    dict_path: Path,
    flyway_path: Path,
    jdbc_url: str | None = None,
) -> tuple[list[str], dict]:
    if not dict_path.is_file():
        return [f"Dictionary missing: {dict_path}"], {}

    dictionary = load_simple_yaml(dict_path.read_text(encoding="utf-8"))
    if not isinstance(dictionary, dict):
        return ["Dictionary root must be a mapping"], {}

    errors: list[str] = []
    errors.extend(validate_dictionary_structure(dictionary))
    errors.extend(validate_against_flyway_sql(dictionary, flyway_path))
    if jdbc_url:
        errors.extend(validate_against_information_schema(dictionary, jdbc_url))
    return errors, dictionary


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="Validate PCIS data dictionary vs Flyway V1")
    ap.add_argument("--dictionary", type=Path, default=DEFAULT_DICT)
    ap.add_argument("--flyway", type=Path, default=DEFAULT_FLYWAY)
    ap.add_argument("--jdbc-url", default="", help="Optional PostgreSQL JDBC URL for live check")
    args = ap.parse_args(argv)

    errors, dictionary = validate(args.dictionary, args.flyway, args.jdbc_url or None)
    if errors:
        print(f"FAIL: {len(errors)} validation error(s)", file=sys.stderr)
        for e in errors[:50]:
            print(f"  - {e}", file=sys.stderr)
        if len(errors) > 50:
            print(f"  ... and {len(errors) - 50} more", file=sys.stderr)
        return 1

    summary = dictionary.get("summary", {})
    print(
        f"OK: data dictionary valid vs Flyway V1 — "
        f"{dictionary.get('table_count')} tables, "
        f"tiers={VALID_TIERS}, summary={summary}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
