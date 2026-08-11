#!/usr/bin/env python3
"""Byte-level comparison of two normalized golden artifact directories."""

from __future__ import annotations

import argparse
import csv
import io
import sys
from pathlib import Path


def _rel_files(root: Path) -> list[Path]:
    return sorted(p.relative_to(root) for p in root.rglob("*") if p.is_file())


def _column_diff(left: str, right: str, rel: Path) -> list[str]:
    details: list[str] = []
    if not rel.name.endswith(".csv"):
        if left != right:
            details.append(f"{rel}: content differs (non-CSV)")
        return details

    lreader = list(csv.DictReader(io.StringIO(left)))
    rreader = list(csv.DictReader(io.StringIO(right)))
    lfields = list(csv.DictReader(io.StringIO(left)).fieldnames or [])
    # Re-read fieldnames cleanly
    lfields = list(csv.reader(io.StringIO(left)))
    header = lfields[0] if lfields else []

    if len(lreader) != len(rreader):
        details.append(
            f"{rel}: row count {len(lreader)} != {len(rreader)} "
            f"(missing/extra rows)"
        )

    for idx, (lrow, rrow) in enumerate(zip(lreader, rreader)):
        for col in header:
            lv = lrow.get(col, "")
            rv = rrow.get(col, "")
            if lv != rv:
                details.append(f"{rel}: row {idx} column {col}: {lv!r} != {rv!r}")

    if len(lreader) > len(rreader):
        details.append(f"{rel}: {len(lreader) - len(rreader)} extra row(s) on left")
    elif len(rreader) > len(lreader):
        details.append(f"{rel}: {len(rreader) - len(lreader)} extra row(s) on right")

    return details


def compare_dirs(left: Path, right: Path) -> tuple[bool, list[str]]:
    left_files = set(_rel_files(left))
    right_files = set(_rel_files(right))
    details: list[str] = []

    for missing in sorted(left_files - right_files):
        details.append(f"missing on right: {missing}")
    for extra in sorted(right_files - left_files):
        details.append(f"extra on right: {extra}")

    for rel in sorted(left_files & right_files):
        ltext = (left / rel).read_text(encoding="utf-8")
        rtext = (right / rel).read_text(encoding="utf-8")
        if ltext == rtext:
            continue
        details.extend(_column_diff(ltext, rtext, rel))

    return (len(details) == 0, details)


def main() -> int:
    parser = argparse.ArgumentParser(description="Compare normalized golden artifacts")
    parser.add_argument("left", type=Path)
    parser.add_argument("right", type=Path)
    args = parser.parse_args()

    ok, details = compare_dirs(args.left, args.right)
    if ok:
        print(f"PASS: {args.left} == {args.right}")
        return 0

    print(f"FAIL: {args.left} != {args.right}", file=sys.stderr)
    for line in details:
        print(f"  - {line}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
