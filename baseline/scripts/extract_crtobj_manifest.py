#!/usr/bin/env python3
"""
Extract CRT* object creation commands from PCIS_CRTOBJ.clle into JSON.

WO-240 — Validate Production Library Against Repository Manifest.

Usage:
    python3 baseline/scripts/extract_crtobj_manifest.py [--input PATH] [--output PATH]
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

# INSTEST is a known typo for INSTST (test program library).
INSTEST_TYPO = "INSTEST"
INSTEST_CORRECTION = "INSTST"

# Map IBM i CRT* verbs to object type metadata.
CRT_SPECS: dict[str, dict[str, str]] = {
    "CRTSQLCBLI": {"param": "OBJ", "object_type": "*MODULE", "category": "cobol-module"},
    "CRTCBLI": {"param": "OBJ", "object_type": "*MODULE", "category": "cobol-module"},
    "CRTPGM": {"param": "PGM", "object_type": "*PGM", "category": "program"},
    "CRTSRVPGM": {"param": "SRVPGM", "object_type": "*SRVPGM", "category": "service-program"},
    "CRTDSPF": {"param": "FILE", "object_type": "*FILE", "category": "display-file"},
    "CRTPRTF": {"param": "FILE", "object_type": "*FILE", "category": "printer-file"},
    "CRTCLPGM": {"param": "PGM", "object_type": "*PGM", "category": "cl-program"},
    "CRTCMD": {"param": "CMD", "object_type": "*CMD", "category": "command"},
}

DEFAULT_LIBS = {
    "SRCLIB": "PCIS",
    "PGMLIB": "PCISPGM",
    "DSPLIB": "PCISPGM",
}


def _join_continuations(text: str) -> str:
    """Join CL lines continued with trailing '+'."""
    lines = text.splitlines()
    joined: list[str] = []
    buf = ""
    for raw in lines:
        line = raw.rstrip()
        if not line.strip() or line.lstrip().startswith("/*"):
            if buf:
                joined.append(buf)
                buf = ""
            joined.append(line)
            continue
        if buf:
            buf = buf.rstrip("+").rstrip() + " " + line.lstrip()
        else:
            buf = line
        if line.rstrip().endswith("+"):
            continue
        joined.append(buf)
        buf = ""
    if buf:
        joined.append(buf)
    return "\n".join(joined)


def _parse_dcl_defaults(text: str) -> dict[str, str]:
    libs = dict(DEFAULT_LIBS)
    for m in re.finditer(
        r"DCL\s+VAR\(&(\w+)\)\s+TYPE\(\*CHAR\)\s+LEN\(\d+\)\s+VALUE\('([^']+)'\)",
        text,
        re.IGNORECASE,
    ):
        libs[m.group(1).upper()] = m.group(2)
    return libs


def _split_lib_name(raw: str, default_lib: str) -> tuple[str, str]:
    raw = raw.strip().strip("'\"")
    if "/" in raw:
        lib, name = raw.split("/", 1)
        return lib.strip(), name.strip()
    return default_lib, raw


def _detect_instest(text: str, line_no: int | None = None) -> list[dict[str, Any]]:
    findings: list[dict[str, Any]] = []
    for m in re.finditer(r"\bINSTEST\b", text, re.IGNORECASE):
        findings.append(
            {
                "typo": INSTEST_TYPO,
                "expected": INSTEST_CORRECTION,
                "message": (
                    f"Detected library typo '{INSTEST_TYPO}' "
                    f"(should be '{INSTEST_CORRECTION}' — test program library)."
                ),
                "offset": m.start(),
                "line": line_no,
                "context": text[max(0, m.start() - 40) : m.end() + 40],
            }
        )
    return findings


def parse_crtobj(text: str, source_path: str = "PCIS_CRTOBJ.clle") -> dict[str, Any]:
    """Parse CLLE source and return a structured CRT* object manifest."""
    defaults = _parse_dcl_defaults(text)
    flat = _join_continuations(text)
    objects: list[dict[str, Any]] = []
    typos: list[dict[str, Any]] = []
    sbmjobs: list[dict[str, Any]] = []

    # Whole-file INSTEST scan (comments + code).
    typos.extend(_detect_instest(text))

    # CRT* commands
    crt_re = re.compile(
        r"\b(" + "|".join(CRT_SPECS.keys()) + r")\b\s+(.+?)(?=\n\s*(?:[A-Z]{3,}|\s*$))",
        re.IGNORECASE | re.DOTALL,
    )
    # Simpler line-oriented parse after continuation join
    for line_no, line in enumerate(flat.splitlines(), start=1):
        stripped = line.strip()
        if not stripped or stripped.startswith("/*"):
            continue

        typos.extend(_detect_instest(stripped, line_no))

        sbm = re.match(
            r"SBMJOB\s+CMD\(CALL\s+PGM\(([^)]+)\)\)\s+JOB\((\w+)\)",
            stripped,
            re.IGNORECASE,
        )
        if sbm:
            lib, name = _split_lib_name(sbm.group(1), defaults.get("PGMLIB", "PCISPGM"))
            sbmjobs.append(
                {
                    "job": sbm.group(2),
                    "program": name,
                    "library": lib,
                    "line": line_no,
                    "command": stripped,
                }
            )
            continue

        m = re.match(r"([A-Z0-9]+)\s+(.+)", stripped, re.IGNORECASE)
        if not m:
            continue
        verb = m.group(1).upper()
        if verb not in CRT_SPECS:
            continue
        rest = m.group(2)
        spec = CRT_SPECS[verb]
        param = spec["param"]
        pm = re.search(rf"\b{param}\(([^)]+)\)", rest, re.IGNORECASE)
        if not pm:
            continue
        default_lib = defaults.get("PGMLIB", "PCISPGM")
        if verb in ("CRTDSPF", "CRTPRTF"):
            default_lib = defaults.get("DSPLIB", default_lib)
        library, name = _split_lib_name(pm.group(1), default_lib)

        srcfile = None
        srcmbr = None
        sm = re.search(r"\bSRCFILE\(([^)]+)\)", rest, re.IGNORECASE)
        if sm:
            srcfile = sm.group(1).strip()
        mm = re.search(r"\bSRCMBR\(([^)]+)\)", rest, re.IGNORECASE)
        if mm:
            srcmbr = mm.group(1).strip()

        objects.append(
            {
                "name": name.upper(),
                "type": spec["object_type"],
                "category": spec["category"],
                "library": library.upper(),
                "command": verb,
                "source_file": srcfile,
                "source_member": srcmbr.upper() if srcmbr else None,
                "line": line_no,
                "raw": stripped,
            }
        )

    # Deduplicate INSTEST findings by offset
    seen: set[tuple[Any, ...]] = set()
    unique_typos: list[dict[str, Any]] = []
    for t in typos:
        key = (t.get("offset"), t.get("line"), t.get("context"))
        if key in seen:
            continue
        seen.add(key)
        unique_typos.append(t)

    return {
        "source": source_path,
        "generated_at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "default_libraries": defaults,
        "object_count": len(objects),
        "objects": objects,
        "submitted_jobs": sbmjobs,
        "instest_typos": unique_typos,
        "instest_typo_detected": bool(unique_typos),
    }


def extract_from_path(path: Path) -> dict[str, Any]:
    text = path.read_text(encoding="utf-8", errors="replace")
    return parse_crtobj(text, source_path=str(path))


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    root = Path(__file__).resolve().parents[2]
    parser.add_argument(
        "--input",
        type=Path,
        default=root / "Property_Casualty_Insurance_System" / "PCIS_CRTOBJ.clle",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=root / "baseline" / "reports" / "crtobj_manifest.json",
    )
    args = parser.parse_args(argv)

    if not args.input.is_file():
        print(f"error: input not found: {args.input}", file=sys.stderr)
        return 1

    manifest = extract_from_path(args.input)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(
        f"Wrote {args.output} ({manifest['object_count']} objects, "
        f"instest_typo_detected={manifest['instest_typo_detected']})"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
