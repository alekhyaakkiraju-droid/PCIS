#!/usr/bin/env python3
"""Normalize COBOL golden-capture artifacts for byte-identical comparison.

Transforms:
  - Timestamp columns / DISPLAY timestamps → NORMALIZED_TS
  - Surrogate/identity keys → SEQ_001, SEQ_002, … (order of appearance)
  - Rows sorted by business-key columns (or all columns if none specified)
  - NUMERIC values preserved as decimal strings (no float conversion)
"""

from __future__ import annotations

import argparse
import csv
import io
import re
from pathlib import Path
from typing import Iterable

TIMESTAMP_RE = re.compile(
    r"""(?x)
    \d{4}-\d{2}-\d{2}                 # date
    (?:
        [ T]\d{2}:\d{2}:\d{2}        # ISO / SQL time
        (?:\.\d+)?
      | -\d{2}\.\d{2}\.\d{2}          # IBM i: 2024-06-15-14.22.33
        (?:\.\d+)?
    )?
    |
    \d{2}/\d{2}/\d{4}\s+\d{2}:\d{2}:\d{2}
    """
)

# Columns treated as generated surrogates (rewritten to ordinal placeholders).
DEFAULT_SURROGATE_COLUMNS = {
    "LOG_ID",
    "PAYMENT_ID",
    "INSTALLMENT_ID",
    "COMMISSION_ID",
    "RUN_ID",
    "ARCHIVE_ID",
    "SEQ_NO",
    "ID",
}

DEFAULT_BUSINESS_KEYS = {
    "CLAIM_RESERVE_T": ["CLAIM_ID", "RESERVE_ID"],
    "CLAIM_PAYMENT_T": ["CLAIM_ID", "PAYMENT_AMT", "SEQ_PLACEHOLDER"],
    "BILLING_INSTALLMENT_T": ["POLICY_ID", "INSTALLMENT_NO"],
    "AUDIT_LOG_T": ["PROGRAM_NAME", "ACTION_CODE", "RECORD_KEY"],
    "AUDIT_LOG_ARCHIVE_T": ["PROGRAM_NAME", "ACTION_CODE", "RECORD_KEY"],
    "COMMISSION_T": ["POLICY_ID", "AGENT_ID"],
    "POLICY_T": ["POLICY_ID"],
    "RPT_RUN_LOG_T": ["PROGRAM_NAME", "STATUS"],
}


def normalize_value(value: str, column: str, seq_counters: dict[str, int]) -> str:
    if value is None:
        return ""
    text = value.strip()
    if text.upper() in {"NULL", "\\N"}:
        return ""

    upper_col = column.upper()
    if upper_col in DEFAULT_SURROGATE_COLUMNS or upper_col.endswith("_SEQ"):
        # Preserve empty; otherwise assign stable ordinal in appearance order.
        if text == "":
            return ""
        key = upper_col
        seq_counters[key] = seq_counters.get(key, 0) + 1
        return f"SEQ_{seq_counters[key]:03d}"

    if TIMESTAMP_RE.fullmatch(text) or upper_col.endswith("_TS") or upper_col.endswith("_TIMESTAMP") or upper_col in {
        "LOG_TIMESTAMP",
        "ARCHIVE_DATE",
        "RUN_STARTED",
        "RUN_ENDED",
        "CREATED_AT",
        "UPDATED_AT",
    }:
        # Dates that look like plain ISO dates used as business keys stay as-is
        # only when column is clearly a business date (not timestamp).
        if upper_col.endswith("_DATE") and re.fullmatch(r"\d{4}-\d{2}-\d{2}", text):
            if upper_col in {"ARCHIVE_DATE", "CUTOFF_DATE"}:
                return "NORMALIZED_TS"
            return text
        return "NORMALIZED_TS"

    # Collapse embedded timestamps inside DISPLAY / free-text cells.
    return TIMESTAMP_RE.sub("NORMALIZED_TS", text)


def normalize_csv(
    raw: str,
    *,
    table_name: str = "",
    surrogate_columns: Iterable[str] | None = None,
    business_keys: list[str] | None = None,
) -> str:
    reader = csv.DictReader(io.StringIO(raw))
    if reader.fieldnames is None:
        return ""

    fieldnames = list(reader.fieldnames)
    surrogates = {c.upper() for c in (surrogate_columns or DEFAULT_SURROGATE_COLUMNS)}
    # Pass 1: normalize non-surrogate fields; keep raw surrogate values for sort stability.
    interim: list[dict[str, str]] = []
    raw_surrogates: list[dict[str, str]] = []

    for row in reader:
        out: dict[str, str] = {}
        raw_s: dict[str, str] = {}
        for col in fieldnames:
            val = row.get(col, "") or ""
            if col.upper() in surrogates:
                raw_s[col] = val.strip()
                out[col] = ""  # filled after sort
            else:
                out[col] = normalize_value(val, col, {})
        interim.append(out)
        raw_surrogates.append(raw_s)

    keys = business_keys or DEFAULT_BUSINESS_KEYS.get(table_name.upper())
    if keys:
        present = [k for k in keys if k in fieldnames and k.upper() not in surrogates]
    else:
        present = []

    order = list(range(len(interim)))
    if present:
        order.sort(key=lambda i: tuple(interim[i].get(k, "") for k in present))
    else:
        order.sort(key=lambda i: tuple(interim[i].get(c, "") for c in fieldnames))

    # Pass 2: assign SEQ_* in sorted appearance order.
    seq_counters: dict[str, int] = {}
    rows: list[dict[str, str]] = []
    for i in order:
        out = dict(interim[i])
        for col, raw in raw_surrogates[i].items():
            out[col] = normalize_value(raw, col, seq_counters)
        rows.append(out)

    buf = io.StringIO()
    writer = csv.DictWriter(buf, fieldnames=fieldnames, lineterminator="\n")
    writer.writeheader()
    writer.writerows(rows)
    return buf.getvalue()


def normalize_display(raw: str) -> str:
    lines = []
    for line in raw.splitlines():
        lines.append(TIMESTAMP_RE.sub("NORMALIZED_TS", line.rstrip()))
    # Stable trailing newline
    return "\n".join(lines) + ("\n" if raw.endswith("\n") or lines else "")


def normalize_artifact_dir(src: Path, dest: Path) -> None:
    dest.mkdir(parents=True, exist_ok=True)
    tables_src = src / "tables"
    tables_dest = dest / "tables"
    if tables_src.is_dir():
        tables_dest.mkdir(parents=True, exist_ok=True)
        for csv_path in sorted(tables_src.glob("*.csv")):
            raw = csv_path.read_text(encoding="utf-8")
            normalized = normalize_csv(raw, table_name=csv_path.stem)
            (tables_dest / csv_path.name).write_text(normalized, encoding="utf-8")

    for name in ("display.txt", "run_log.csv", "metadata.yaml"):
        path = src / name
        if not path.exists():
            continue
        raw = path.read_text(encoding="utf-8")
        if name.endswith(".csv"):
            out = normalize_csv(raw, table_name="RPT_RUN_LOG_T")
        elif name.endswith(".txt"):
            out = normalize_display(raw)
        else:
            # metadata.yaml: normalize embedded timestamps only
            out = TIMESTAMP_RE.sub("NORMALIZED_TS", raw)
        (dest / name).write_text(out, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Normalize golden COBOL artifacts")
    parser.add_argument("source", type=Path, help="Raw capture directory")
    parser.add_argument("dest", type=Path, help="Normalized output directory")
    args = parser.parse_args()
    normalize_artifact_dir(args.source, args.dest)
    print(f"Normalized {args.source} → {args.dest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
