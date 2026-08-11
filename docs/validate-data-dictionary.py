#!/usr/bin/env python3
"""
Validate docs/data-dictionary.yaml against DDL design and COBOL baseline (WO-128).

Checks:
  1. Every table in PCIS_Database_Design.md has a dictionary entry
  2. Every baseline host variable that references a table column is mapped
  3. No resolution field is empty
  4. Dictionary reports 55 tables

Exits 0 on success, 1 on failure. Stdlib only.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(Path(__file__).resolve().parent))

from build_data_dictionary import (  # noqa: E402
    BaselineColumnExtractor,
    DdlParser,
    load_simple_yaml,
)

DEFAULT_DICT = REPO_ROOT / "docs" / "data-dictionary.yaml"
DEFAULT_DDL = REPO_ROOT / "Property_Casualty_Insurance_System" / "PCIS_Database_Design.md"
DEFAULT_BASELINE = REPO_ROOT / "baseline" / "cobol-baseline.yaml"
DEFAULT_COBOL_DIR = REPO_ROOT / "Property_Casualty_Insurance_System"

# Host variables that are scratch / control and need not map to a persisted column
SCRATCH_HOSTS = {
    "WS-RETENTION-DAYS",
    "WS-CHUNK-SIZE",
    "WS-LEAD-DAYS",
    "WS-GRACE-DAYS",
    "WS-RENEWAL-WINDOW-DAYS",
    "WS-REI-CESSION-THRESHOLD",
    "HV-CHUNK-SIZE",
    "HV-ROWS-INSERTED",
    "HV-ROWS-DELETED",
    "HV-CUTOFF-DATE",
    "HV-DAYS-OVERDUE",
}


def collect_mapped_hosts(dictionary: dict) -> set[str]:
    mapped: set[str] = set()
    for table in dictionary.get("tables") or []:
        for col in table.get("columns") or []:
            hv = (col.get("cobol_host_variable") or "").strip().upper()
            if hv:
                mapped.add(hv)
                # also accept HV- form of plain names
                mapped.add(hv.replace("_", "-"))
                if not hv.startswith("HV-") and not hv.startswith("HV_"):
                    mapped.add("HV-" + hv.replace("_", "-"))
                    mapped.add("HV_" + hv)
            ddl = (col.get("ddl_column_name") or "").strip().upper()
            if ddl:
                mapped.add(ddl)
            res = (col.get("resolution") or "").strip().upper()
            if res:
                mapped.add(res)
                mapped.add(res.upper())
    return mapped


def host_mapped(hv: str, mapped: set[str], table: str, dictionary: dict) -> bool:
    key = hv.upper()
    if key in SCRATCH_HOSTS:
        return True
    if key in mapped:
        return True
    plain = key
    for prefix in ("HV-", "HV_", "WS-", "WS_"):
        if plain.startswith(prefix):
            plain = plain[len(prefix) :]
            break
    plain = plain.replace("-", "_")
    if plain in mapped or plain.lower() in {m.lower() for m in mapped}:
        return True
    key_norm = BaselineColumnExtractor._normalize(key)
    # table-scoped: match host against cobol var, DDL name, or resolution via synonyms
    for t in dictionary.get("tables") or []:
        if t.get("table_name") != table:
            continue
        for col in t.get("columns") or []:
            candidates = [
                col.get("cobol_host_variable") or "",
                col.get("ddl_column_name") or "",
                col.get("resolution") or "",
            ]
            for cand in candidates:
                if not cand:
                    continue
                cu = cand.upper().replace("-", "_")
                if cu == key or cu == plain or cand.lower() == plain.lower():
                    return True
                if BaselineColumnExtractor._normalize(cand) == key_norm:
                    return True
    return False


def validate(
    dict_path: Path,
    ddl_path: Path,
    baseline_path: Path,
    cobol_dir: Path,
) -> list[str]:
    errors: list[str] = []
    if not dict_path.is_file():
        return [f"Dictionary missing: {dict_path}"]

    dictionary = load_simple_yaml(dict_path.read_text(encoding="utf-8"))
    if not isinstance(dictionary, dict):
        return ["Dictionary root must be a mapping"]

    ddl_tables = DdlParser().parse(ddl_path)
    dict_tables = {
        t.get("table_name")
        for t in (dictionary.get("tables") or [])
        if isinstance(t, dict) and t.get("table_name")
    }

    table_count = dictionary.get("table_count") or len(dict_tables)
    if int(table_count) != 55:
        errors.append(f"Expected 55 tables in dictionary, found table_count={table_count}")
    if len(dict_tables) != 55:
        errors.append(f"Expected 55 table entries, found {len(dict_tables)}")

    for name in sorted(ddl_tables):
        if name not in dict_tables:
            errors.append(f"DDL table missing from dictionary: {name}")

    # Empty resolutions
    for t in dictionary.get("tables") or []:
        tname = t.get("table_name")
        for col in t.get("columns") or []:
            res = col.get("resolution")
            if res is None or str(res).strip() == "":
                errors.append(
                    f"Empty resolution: {tname}."
                    f"{col.get('ddl_column_name') or col.get('cobol_host_variable')}"
                )

    # Baseline host variables mapped
    extractor = BaselineColumnExtractor()
    baseline = baseline_path if baseline_path.is_file() else None
    # Extract without G06 witnesses for the "every baseline host var" check —
    # re-read baseline/cobol only
    refs = []
    if baseline:
        pic_map: dict[str, str] = {}
        if cobol_dir.is_dir():
            for path in cobol_dir.glob("*.cbl"):
                text = path.read_text(encoding="utf-8", errors="replace")
                for m in extractor.PIC_RE.finditer(text):
                    pic_map[m.group(1).upper()] = " ".join(m.group(2).split())
        refs.extend(extractor._from_baseline(baseline, pic_map))
    if cobol_dir.is_dir():
        pic_map = {}
        for path in cobol_dir.glob("*.cbl"):
            text = path.read_text(encoding="utf-8", errors="replace")
            for m in extractor.PIC_RE.finditer(text):
                pic_map[m.group(1).upper()] = " ".join(m.group(2).split())
            refs.extend(extractor._from_cobol_text(path.stem, text, pic_map))

    mapped = collect_mapped_hosts(dictionary)
    checked: set[tuple[str, str]] = set()
    for r in refs:
        if r.table.upper().startswith("SYS"):
            continue
        key = (r.table, r.host_variable.upper())
        if key in checked:
            continue
        checked.add(key)
        if r.host_variable.upper() in SCRATCH_HOSTS:
            continue
        # SQL column names that are also emitted as host_variable by extractor
        # are OK if the table has any mapping covering that column
        if not host_mapped(r.host_variable, mapped, r.table, dictionary):
            # aliased tables
            from build_data_dictionary import TABLE_ALIASES

            alias_t = TABLE_ALIASES.get(r.table, r.table)
            if alias_t != r.table and host_mapped(
                r.host_variable, mapped, alias_t, dictionary
            ):
                continue
            errors.append(
                f"Unmapped COBOL host variable: {r.table}.{r.host_variable} "
                f"(program={r.program_id})"
            )

    return errors


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="Validate PCIS data dictionary")
    ap.add_argument("--dictionary", type=Path, default=DEFAULT_DICT)
    ap.add_argument("--ddl", type=Path, default=DEFAULT_DDL)
    ap.add_argument("--baseline", type=Path, default=DEFAULT_BASELINE)
    ap.add_argument("--cobol-dir", type=Path, default=DEFAULT_COBOL_DIR)
    args = ap.parse_args(argv)

    errors = validate(args.dictionary, args.ddl, args.baseline, args.cobol_dir)
    if errors:
        print(f"FAIL: {len(errors)} validation error(s)", file=sys.stderr)
        for e in errors[:50]:
            print(f"  - {e}", file=sys.stderr)
        if len(errors) > 50:
            print(f"  ... and {len(errors) - 50} more", file=sys.stderr)
        return 1

    dictionary = load_simple_yaml(args.dictionary.read_text(encoding="utf-8"))
    print(
        f"OK: data dictionary valid — "
        f"{dictionary.get('table_count')} tables, "
        f"summary={dictionary.get('summary')}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
