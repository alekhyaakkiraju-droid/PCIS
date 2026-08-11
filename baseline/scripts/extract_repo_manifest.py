#!/usr/bin/env python3
"""
Walk Property_Casualty_Insurance_System and categorize repository files by type.

WO-240 — Validate Production Library Against Repository Manifest.

Usage:
    python3 baseline/scripts/extract_repo_manifest.py [--source-dir PATH] [--output PATH]
"""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

EXTENSION_MAP: dict[str, str] = {
    ".cbl": "cobol",
    ".cob": "cobol",
    ".sqlcbl": "cobol",
    ".dspf": "display-file",
    ".prtf": "printer-file",
    ".pf": "physical-file",
    ".lf": "logical-file",
    ".dds": "dds",
    ".clle": "clle",
    ".clp": "cl",
    ".cmd": "command",
    ".rpgle": "rpg",
    ".sql": "sql",
    ".md": "markdown",
    ".txt": "text",
    ".yaml": "yaml",
    ".yml": "yaml",
    ".json": "json",
}


def categorize(path: Path) -> str:
    ext = path.suffix.lower()
    return EXTENSION_MAP.get(ext, "other")


def object_stem(path: Path) -> str | None:
    """Object/member name candidate from filename stem.

    COBOL/DDS members are typically <=10 chars (IBM i). Longer stems (e.g. CLLE
    tool members like JOBSCHD_NEW_DRIVERS) are kept intact for inventory.
    """
    stem = path.stem.upper()
    if path.suffix.lower() in {".cbl", ".cob", ".dspf", ".prtf", ".clle", ".clp", ".pf", ".lf", ".dds"}:
        return stem
    return None


def walk_repo(source_dir: Path) -> dict[str, Any]:
    files: list[dict[str, Any]] = []
    by_type: dict[str, list[str]] = {}
    objects: dict[str, dict[str, Any]] = {}

    for path in sorted(source_dir.rglob("*")):
        if not path.is_file():
            continue
        if path.name.startswith("."):
            continue
        rel = path.relative_to(source_dir).as_posix()
        ftype = categorize(path)
        data = path.read_bytes()
        sha = hashlib.sha256(data).hexdigest()
        try:
            text = data.decode("utf-8")
            line_count = text.count("\n") + (0 if text.endswith("\n") or not text else 1)
        except UnicodeDecodeError:
            line_count = None

        entry = {
            "path": rel,
            "type": ftype,
            "line_count": line_count,
            "sha256": sha,
            "object_name": object_stem(path),
        }
        files.append(entry)
        by_type.setdefault(ftype, []).append(rel)

        name = object_stem(path)
        if name:
            objects[name] = {
                "name": name,
                "type": ftype,
                "path": rel,
            }

    return {
        "source_dir": str(source_dir),
        "generated_at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "total_file_count": len(files),
        "counts_by_type": {k: len(v) for k, v in sorted(by_type.items())},
        "files": files,
        "objects": objects,
        "by_type": {k: v for k, v in sorted(by_type.items())},
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    root = Path(__file__).resolve().parents[2]
    parser.add_argument(
        "--source-dir",
        type=Path,
        default=root / "Property_Casualty_Insurance_System",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=root / "baseline" / "reports" / "repo_manifest.json",
    )
    args = parser.parse_args(argv)

    if not args.source_dir.is_dir():
        print(f"error: source dir not found: {args.source_dir}", file=sys.stderr)
        return 1

    manifest = walk_repo(args.source_dir)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(
        f"Wrote {args.output} ({manifest['total_file_count']} files; "
        f"types={manifest['counts_by_type']})"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
