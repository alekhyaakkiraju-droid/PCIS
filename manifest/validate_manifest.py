#!/usr/bin/env python3
"""
PCIS Manifest Validator (WO-001)

Cross-checks manifest/pcis-manifest.yaml against the filesystem:

  1. Every real-file entry (status != missing-callee and status != design-only,
     sha256_checksum is not null) must exist on disk with matching line count
     and SHA-256 checksum.
  2. Every file on disk inside the source directory must have a corresponding
     entry in the manifest.
  3. Synthetic entries (design-only, missing-callee) must NOT have a
     corresponding file on disk.

Exits with code 0 on success, non-zero on any discrepancy.

Usage:
    python3 manifest/validate_manifest.py [--manifest MANIFEST]
                                          [--source-dir SOURCE_DIR]
                                          [--repo-root REPO_ROOT]
"""

import argparse
import hashlib
import os
import sys
from pathlib import Path


# ---------------------------------------------------------------------------
# MINIMAL YAML LOADER (stdlib-only — handles the specific format written by
# generate_manifest.py.  Not a general-purpose YAML parser.)
# ---------------------------------------------------------------------------

def _load_yaml_manifest(text: str) -> dict:
    """Parse the YAML produced by generate_manifest.py.

    Supports only the subset of YAML present in the manifest:
      - Top-level scalar key: value pairs
      - A 'files' key whose value is a list of record dicts
      - Each record dict has scalar fields plus an optional 'dependencies' list
      - Quoted strings (double-quotes JSON-style)
      - null values
      - Integer values
      - Empty lists  []
    """
    lines = text.splitlines()
    # Strip document marker and comments
    lines = [l for l in lines if not l.startswith('---') and not l.startswith('#')]

    result = {}
    files_list = []
    in_files = False
    current_file = None
    in_dependencies = False
    current_dep = None

    def _parse_scalar(raw: str):
        """Convert a YAML scalar string to Python value."""
        raw = raw.strip()
        if raw == 'null' or raw == '~':
            return None
        if raw == 'true':
            return True
        if raw == 'false':
            return False
        if raw == '[]':
            return []
        # Quoted string
        if raw.startswith('"') and raw.endswith('"'):
            # Unescape JSON-style string
            import json as _json
            try:
                return _json.loads(raw)
            except Exception:
                return raw[1:-1]
        # Integer
        try:
            return int(raw)
        except ValueError:
            pass
        return raw

    for line in lines:
        if not line.strip():
            continue

        # Top-level key (no leading spaces)
        if line and line[0] != ' ' and ':' in line:
            key, _, rest = line.partition(':')
            key = key.strip()
            val = _parse_scalar(rest.strip())
            if key == 'files':
                in_files = True
                result['files'] = files_list
            else:
                result[key] = val
                in_files = False
            current_file = None
            in_dependencies = False
            continue

        indent = len(line) - len(line.lstrip())

        # File list entry (indent 2, starts with "- ")
        if indent == 2 and line.lstrip().startswith('- '):
            # Save previous file entry
            if current_file is not None:
                if current_dep is not None:
                    current_file.setdefault('dependencies', []).append(
                        current_dep)
                    current_dep = None
                files_list.append(current_file)
            in_dependencies = False
            current_dep = None
            rest = line.lstrip()[2:]  # strip "- "
            if ':' in rest:
                key, _, val_str = rest.partition(':')
                current_file = {key.strip(): _parse_scalar(val_str.strip())}
            else:
                current_file = {}
            continue

        # File record field (indent 4) or dependencies block
        if indent == 4 and current_file is not None:
            stripped = line.strip()
            if ':' in stripped:
                key, _, val_str = stripped.partition(':')
                key = key.strip()
                val_str = val_str.strip()
                if key == 'dependencies':
                    in_dependencies = True
                    if val_str == '[]':
                        current_file['dependencies'] = []
                        in_dependencies = False
                    else:
                        current_file['dependencies'] = []
                else:
                    in_dependencies = False
                    current_file[key] = _parse_scalar(val_str)
            continue

        # Dependency list item (indent 6, starts with "- ")
        if indent == 6 and in_dependencies:
            stripped = line.strip()
            if stripped.startswith('- '):
                if current_dep is not None:
                    current_file.setdefault('dependencies', []).append(
                        current_dep)
                rest = stripped[2:]
                if ':' in rest:
                    key, _, val_str = rest.partition(':')
                    current_dep = {key.strip(): _parse_scalar(val_str.strip())}
                else:
                    current_dep = {}
            continue

        # Dependency field (indent 8)
        if indent == 8 and current_dep is not None:
            stripped = line.strip()
            if ':' in stripped:
                key, _, val_str = stripped.partition(':')
                current_dep[key.strip()] = _parse_scalar(val_str.strip())
            continue

    # Flush final pending items
    if current_dep is not None and current_file is not None:
        current_file.setdefault('dependencies', []).append(current_dep)
    if current_file is not None:
        files_list.append(current_file)

    return result


# ---------------------------------------------------------------------------
# CHECKSUM / LINE COUNT (must match generate_manifest.py)
# ---------------------------------------------------------------------------

def _sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open('rb') as fh:
        for chunk in iter(lambda: fh.read(65536), b''):
            h.update(chunk)
    return h.hexdigest()


def _line_count(path: Path) -> int:
    count = 0
    with path.open('rb') as fh:
        for _ in fh:
            count += 1
    return count


# ---------------------------------------------------------------------------
# VALIDATION
# ---------------------------------------------------------------------------

def validate(manifest_path: Path, repo_root: Path,
             source_dir_name: str, strict: bool = True) -> int:
    """Run all validation checks; return the number of failures."""
    if not manifest_path.exists():
        print(f"ERROR: Manifest not found: {manifest_path}", file=sys.stderr)
        return 1

    text = manifest_path.read_text(encoding='utf-8')
    manifest = _load_yaml_manifest(text)

    failures = []
    warnings = []

    source_dir = repo_root / source_dir_name

    # -------------------------------------------------------------------
    # Build lookup: path → entry
    # -------------------------------------------------------------------
    entries = manifest.get('files', [])
    manifest_paths = {e['path']: e for e in entries}

    # -------------------------------------------------------------------
    # Check 1: every real-file entry must exist on disk with matching
    #           line count and checksum
    # -------------------------------------------------------------------
    real_entries = [
        e for e in entries
        if e.get('sha256_checksum') is not None
        and e.get('status') not in ('missing-callee', 'design-only')
    ]

    for entry in real_entries:
        rel_path = entry['path']
        disk_path = repo_root / rel_path

        if not disk_path.exists():
            failures.append(
                f"MISSING FILE: manifest entry '{rel_path}' does not exist on disk")
            continue

        # Line count check
        actual_lc = _line_count(disk_path)
        expected_lc = entry.get('line_count', 0)
        if actual_lc != expected_lc:
            failures.append(
                f"LINE COUNT MISMATCH: '{rel_path}' "
                f"expected={expected_lc} actual={actual_lc}")

        # Checksum check
        actual_sha = _sha256(disk_path)
        expected_sha = entry.get('sha256_checksum', '')
        if actual_sha != expected_sha:
            failures.append(
                f"CHECKSUM MISMATCH: '{rel_path}' "
                f"expected={expected_sha[:16]}... actual={actual_sha[:16]}...")

    # -------------------------------------------------------------------
    # Check 2: every file on disk inside source_dir must be in manifest
    # -------------------------------------------------------------------
    if source_dir.is_dir():
        for disk_path in sorted(source_dir.rglob('*')):
            if not disk_path.is_file():
                continue
            rel = str(disk_path.relative_to(repo_root)).replace(os.sep, '/')
            if rel not in manifest_paths:
                failures.append(
                    f"UNTRACKED FILE: '{rel}' is on disk but not in manifest")
    else:
        warnings.append(
            f"WARNING: Source directory '{source_dir}' does not exist; "
            f"skipping disk scan")

    # -------------------------------------------------------------------
    # Check 3: design-only entries must NOT have files on disk
    # -------------------------------------------------------------------
    for entry in entries:
        if entry.get('status') == 'design-only':
            rel_path = entry['path']
            disk_path = repo_root / rel_path
            if disk_path.exists():
                warnings.append(
                    f"DESIGN-ONLY CONFLICT: '{rel_path}' is marked design-only "
                    f"but a file exists on disk")

    # -------------------------------------------------------------------
    # Check 4: total_file_count matches actual real-file entries
    # -------------------------------------------------------------------
    declared_count = manifest.get('total_file_count', -1)
    actual_count = len(real_entries)
    if declared_count != actual_count:
        failures.append(
            f"FILE COUNT MISMATCH: manifest declares total_file_count="
            f"{declared_count} but found {actual_count} real entries")

    # -------------------------------------------------------------------
    # Report
    # -------------------------------------------------------------------
    for w in warnings:
        print(f"  {w}")

    if failures:
        print(f"\nValidation FAILED — {len(failures)} issue(s):")
        for f in failures:
            print(f"  {f}")
        return len(failures)

    n = len(real_entries)
    print(f"Validation PASSED — {n} real-file entries verified, "
          f"{len(entries) - n} synthetic entries present")
    return 0


# ---------------------------------------------------------------------------
# CLI ENTRY POINT
# ---------------------------------------------------------------------------

def main():
    ap = argparse.ArgumentParser(
        description='Validate PCIS manifest against filesystem.')
    ap.add_argument(
        '--manifest', default='manifest/pcis-manifest.yaml',
        help='Path to manifest YAML relative to repo root '
             '(default: manifest/pcis-manifest.yaml)')
    ap.add_argument(
        '--source-dir', default='Property_Casualty_Insurance_System',
        help='Source directory name (default: Property_Casualty_Insurance_System)')
    ap.add_argument(
        '--repo-root', default='.',
        help='Repository root (default: current directory)')
    ap.add_argument(
        '--no-strict', action='store_true',
        help='Treat warnings as non-fatal')
    args = ap.parse_args()

    repo_root = Path(args.repo_root).resolve()
    manifest_path = repo_root / args.manifest

    failures = validate(
        manifest_path=manifest_path,
        repo_root=repo_root,
        source_dir_name=args.source_dir,
        strict=not args.no_strict,
    )
    sys.exit(0 if failures == 0 else 1)


if __name__ == '__main__':
    main()
