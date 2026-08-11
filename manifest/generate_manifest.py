#!/usr/bin/env python3
"""
PCIS Repository Manifest Generator (WO-001)

Scans the Property_Casualty_Insurance_System directory and produces a
YAML manifest (manifest/pcis-manifest.yaml) classifying every file by
type, status, line count, SHA-256 checksum, and declared dependencies.

Usage:
    python3 manifest/generate_manifest.py [--repo-root REPO_ROOT]
                                          [--output OUTPUT]
                                          [--source-dir SOURCE_DIR]
"""

import argparse
import hashlib
import json
import os
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

# ---------------------------------------------------------------------------
# GENERATOR VERSION
# ---------------------------------------------------------------------------
GENERATOR_VERSION = "1.0.0"

# ---------------------------------------------------------------------------
# KNOWN SYNTHETIC ENTRIES (discovered from design docs and source analysis)
# ---------------------------------------------------------------------------

#: Programs that appear in design documents but have no COBOL source member.
DESIGN_ONLY_PROGRAMS = [
    {"name": "CLM001A", "dds": "CLMFNLD1",
     "description": "Claim FNOL Interactive Program"},
    {"name": "CLM002A", "dds": "CLMRSVD1",
     "description": "Claim Reserve Interactive Program"},
    {"name": "CLM003A", "dds": "CLMADJD1",
     "description": "Claim Adjuster Assignment Interactive Program"},
    {"name": "CLM004A", "dds": "CLMINQD1",
     "description": "Claim Inquiry Interactive Program"},
    {"name": "CLM005A", "dds": "CLMCLSD1",
     "description": "Claim Closure Interactive Program"},
    {"name": "CUS002A", "dds": "CUSINQD1",
     "description": "Customer Inquiry Interactive Program"},
    {"name": "CUS003A", "dds": "CUSLSTD1",
     "description": "Customer List Interactive Program"},
    {"name": "CUS004A", "dds": "CUSDELD1",
     "description": "Customer Delete Interactive Program"},
    {"name": "CUS005A", "dds": "CUSCNTD1",
     "description": "Customer Contacts Interactive Program"},
    {"name": "RPT001A", "dds": "RPTMNUD1",
     "description": "Report Menu Interactive Program"},
    {"name": "RPT006A", "dds": "COMRPTD1",
     "description": "Commission Report Interactive Program"},
]

#: Shared services referenced by CALL but absent from the repository.
MISSING_CALLEES = [
    {"name": "AUDLOG01",
     "description": "Audit Log Writer — shared service, no source member"},
    {"name": "SECCHK01",
     "description": "Security Authorization Checker — shared service, no source member"},
    {"name": "PRMCLC01",
     "description": "Premium Calculator — shared service, no source member"},
    {"name": "POLVAL01",
     "description": "Policy Validator — shared service, no source member"},
    {"name": "CUSVAL01",
     "description": "Customer Validator — shared service, no source member"},
    {"name": "CLMVAL01",
     "description": "Claim Validator — shared service, no source member"},
    {"name": "RESCLC01",
     "description": "Reserve Calculator — shared service, no source member"},
]

#: Printer files referenced in PCIS_CRTOBJ.clle but absent from the repository.
MISSING_PRINTER_FILES = [
    {"name": "POLPOLP1",
     "description": "Policy Output Printer File — missing from repository"},
    {"name": "CLMPAYP1",
     "description": "Claims Payment Printer File — missing from repository"},
    {"name": "RPT001P1",
     "description": "Report 001 Printer File — missing from repository"},
    {"name": "RPT006P1",
     "description": "Report 006 Printer File — missing from repository"},
]

#: Scheduler programs referenced in PCIS_CRTOBJ.clle but with no source member.
#: JOBSCHD4-7 are defined in JOBSCHD_NEW_DRIVERS.clle; JOBSCHD1-3 have no source.
RUNTIME_ONLY_SCHEDULER_REFS = [
    {"name": "JOBSCHD1",
     "description": "Job Scheduler 1 — runtime-only, no source in repository"},
    {"name": "JOBSCHD2",
     "description": "Job Scheduler 2 — runtime-only, no source in repository"},
    {"name": "JOBSCHD3",
     "description": "Job Scheduler 3 — runtime-only, no source in repository"},
]

# ---------------------------------------------------------------------------
# FILE CLASSIFICATION
# ---------------------------------------------------------------------------

#: Markdown filename patterns → (type, status) classification
_MARKDOWN_TYPE_PATTERNS = [
    # Gap analysis / edge case registers
    (re.compile(r'gap_analysis|edge_case|artifact_gap', re.IGNORECASE),
     ('analysis-doc', 'shipped')),
    # Architecture / database / enterprise docs
    (re.compile(r'enterprise_architecture|database_design|prd|intent|rtm|'
                r'requirements_traceability|user_stories|glossary|'
                r'operations_runbook', re.IGNORECASE),
     ('modernization-doc', 'shipped')),
    # Module design documents
    (re.compile(r'module_design|design_document', re.IGNORECASE),
     ('design-doc', 'shipped')),
]


def classify_file(path: Path) -> tuple:
    """Return (type_str, status_str) for a file path.

    type_str  : cobol | dds | cl | design-doc | modernization-doc |
                analysis-doc | other
    status_str: shipped | design-only | missing-callee | stub
    """
    suffix = path.suffix.lower()
    name_lower = path.name.lower()

    if suffix == '.cbl':
        return ('cobol', 'shipped')
    if suffix == '.dspf':
        return ('dds', 'shipped')
    if suffix == '.clle':
        return ('cl', 'shipped')
    if suffix == '.md':
        for pattern, result in _MARKDOWN_TYPE_PATTERNS:
            if pattern.search(name_lower):
                return result
        return ('design-doc', 'shipped')
    return ('other', 'shipped')


# ---------------------------------------------------------------------------
# CHECKSUM AND LINE COUNT
# ---------------------------------------------------------------------------

def compute_sha256(path: Path) -> str:
    """Return the hex-encoded SHA-256 digest of a file."""
    h = hashlib.sha256()
    with path.open('rb') as fh:
        for chunk in iter(lambda: fh.read(65536), b''):
            h.update(chunk)
    return h.hexdigest()


def count_lines(path: Path) -> int:
    """Return the number of lines in a text file."""
    count = 0
    with path.open('rb') as fh:
        for _ in fh:
            count += 1
    return count


# ---------------------------------------------------------------------------
# COBOL PROLOGUE PARSER
# ---------------------------------------------------------------------------

class CobolPrologueParser:
    """Extract CALLS, TABLES, and UI references from COBOL prologue comments.

    Expected prologue format (asterisk-delimited comment blocks):

        *================================================================*
        * PROGRAM:     MYPROG                                           *
        *----------------------------------------------------------------*
        * CALLS:       AUDLOG01                                         *
        *              SECCHK01                                         *
        * TABLES:      CUSTOMER_T                                       *
        *              POLICY_T                                         *
        * UI:          CUSMNTD1                                         *
        *================================================================*
    """

    # Match a labelled section start: "* CALLS:   VALUE1"
    _SECTION_RE = re.compile(
        r'^\s{6}\*\s+'
        r'(CALLS|TABLES|UI|COPY)\s*:\s*(.*)',
        re.IGNORECASE
    )
    # Match a continuation line: "*              VALUE2"
    _CONTINUATION_RE = re.compile(
        r'^\s{6}\*\s{2,}(\S.*)',
    )
    # Match the prologue delimiter line
    _DELIMITER_RE = re.compile(r'^\s{6}\*[=\-]{4,}')

    def parse(self, source: str) -> dict:
        """Parse ``source`` text and return a dict with keys:
        calls, tables, ui, copies — each a list of names.
        """
        result = {'calls': [], 'tables': [], 'ui': [], 'copies': []}
        in_prologue = False
        current_section = None

        for line in source.splitlines():
            stripped = line.rstrip()

            # Detect prologue start/end by delimiter lines
            if self._DELIMITER_RE.match(stripped):
                in_prologue = True
                continue

            if not in_prologue:
                # Stop trying once we hit IDENTIFICATION DIVISION
                if re.match(r'^\s*(IDENTIFICATION DIVISION|PROGRAM-ID)\s*\.?',
                            stripped, re.IGNORECASE):
                    break
                continue

            m = self._SECTION_RE.match(stripped)
            if m:
                label = m.group(1).upper()
                value = m.group(2).strip().rstrip('.')
                current_section = {
                    'CALLS': 'calls',
                    'TABLES': 'tables',
                    'UI': 'ui',
                    'COPY': 'copies',
                }.get(label)
                if current_section and value:
                    for item in re.split(r'[,\s]+', value):
                        item = item.strip().rstrip('*').strip()
                        if item and item != '*':
                            result[current_section].append(item)
                continue

            m = self._CONTINUATION_RE.match(stripped)
            if m and current_section:
                value = m.group(1).strip().rstrip('.').rstrip('*').strip()
                for item in re.split(r'[,\s]+', value):
                    item = item.strip().rstrip('*').strip()
                    if item and item != '*':
                        result[current_section].append(item)
                continue

            # A non-comment line ends the prologue context
            if stripped and not stripped.lstrip().startswith('*'):
                current_section = None

        return result


# ---------------------------------------------------------------------------
# PROCEDURE DIVISION SCANNER
# ---------------------------------------------------------------------------

class ProcedureDivisionScanner:
    """Find CALL literal-name USING and COPY member statements in COBOL source."""

    _PROC_DIV_RE = re.compile(
        r'^\s*PROCEDURE\s+DIVISION', re.IGNORECASE)
    _CALL_RE = re.compile(
        r"CALL\s+'([A-Z0-9_#@$]+)'", re.IGNORECASE)
    _COPY_RE = re.compile(
        r'^\s+COPY\s+([A-Z0-9_#@$]+)', re.IGNORECASE)

    def scan(self, source: str) -> dict:
        """Return {'calls': [...], 'copies': [...]} from PROCEDURE DIVISION."""
        result = {'calls': [], 'copies': []}
        in_procedure = False

        for line in source.splitlines():
            if self._PROC_DIV_RE.match(line):
                in_procedure = True
                continue
            if not in_procedure:
                continue

            for m in self._CALL_RE.finditer(line):
                name = m.group(1).upper()
                if name not in result['calls']:
                    result['calls'].append(name)

            m = self._COPY_RE.match(line)
            if m:
                name = m.group(1).upper()
                if name not in result['copies']:
                    result['copies'].append(name)

        return result


# ---------------------------------------------------------------------------
# DDS REFERENCE EXTRACTOR
# ---------------------------------------------------------------------------

class DdsReferenceExtractor:
    """Extract PFILE and REF references from DDS display-file source."""

    _PFILE_RE = re.compile(r'\bPFILE\s*\(\s*([A-Z0-9_#@$]+)\s*\)',
                           re.IGNORECASE)
    _REF_RE = re.compile(r'\bREF\s*\(\s*([A-Z0-9_#@$]+)\s*\)',
                         re.IGNORECASE)

    def extract(self, source: str) -> dict:
        """Return {'pfile': [...], 'ref': [...]}."""
        result = {'pfile': [], 'ref': []}
        for line in source.splitlines():
            # Skip comment lines (column 7 is *)
            if len(line) >= 7 and line[6] == '*':
                continue
            for m in self._PFILE_RE.finditer(line):
                name = m.group(1).upper()
                if name not in result['pfile']:
                    result['pfile'].append(name)
            for m in self._REF_RE.finditer(line):
                name = m.group(1).upper()
                if name not in result['ref']:
                    result['ref'].append(name)
        return result


# ---------------------------------------------------------------------------
# CL MEMBER SCANNER
# ---------------------------------------------------------------------------

class ClMemberScanner:
    """Extract program/object references from CL source members."""

    _CALL_RE = re.compile(r'\bCALL\s+PGM\s*\(\s*([A-Z0-9_#@$]+)\s*\)',
                          re.IGNORECASE)
    _SBMJOB_CALL_RE = re.compile(
        r'\bSBMJOB\b.*\bCMD\s*\(\s*CALL\s+PGM\s*\(\s*([A-Z0-9_#@$]+)\s*\)',
        re.IGNORECASE)
    _CRTPGM_RE = re.compile(r'\bCRTPGM\s+PGM\s*\(\s*([A-Z0-9_#@$]+)\s*\)',
                             re.IGNORECASE)
    _CRTDSPF_RE = re.compile(r'\bCRTDSPF\s+FILE\s*\(\s*([A-Z0-9_#@$]+)\s*\)',
                              re.IGNORECASE)
    _CRTPRTF_RE = re.compile(r'\bCRTPRTF\s+FILE\s*\(\s*([A-Z0-9_#@$]+)\s*\)',
                              re.IGNORECASE)
    _CRTSQLCBLI_RE = re.compile(
        r'\bCRTSQLCBLI\s+OBJ\s*\(\s*([A-Z0-9_#@$]+)\s*\)', re.IGNORECASE)

    def scan(self, source: str) -> dict:
        """Return categorised references from a CL member."""
        result = {
            'calls': [],
            'sbmjob': [],
            'crtpgm': [],
            'crtdspf': [],
            'crtprtf': [],
            'crtsqlcbli': [],
        }
        for line in source.splitlines():
            stripped = line.strip()
            # Skip comment lines
            if stripped.startswith('/*') or stripped.startswith('!'):
                pass  # allow – extract refs from inline comments? no.

            for m in self._SBMJOB_CALL_RE.finditer(line):
                name = m.group(1).upper()
                if name not in result['sbmjob']:
                    result['sbmjob'].append(name)

            for m in self._CALL_RE.finditer(line):
                name = m.group(1).upper()
                if name not in result['calls']:
                    result['calls'].append(name)

            for m in self._CRTPGM_RE.finditer(line):
                name = m.group(1).upper()
                if name not in result['crtpgm']:
                    result['crtpgm'].append(name)

            for m in self._CRTDSPF_RE.finditer(line):
                name = m.group(1).upper()
                if name not in result['crtdspf']:
                    result['crtdspf'].append(name)

            for m in self._CRTPRTF_RE.finditer(line):
                name = m.group(1).upper()
                if name not in result['crtprtf']:
                    result['crtprtf'].append(name)

            for m in self._CRTSQLCBLI_RE.finditer(line):
                name = m.group(1).upper()
                if name not in result['crtsqlcbli']:
                    result['crtsqlcbli'].append(name)

        return result


# ---------------------------------------------------------------------------
# DEPENDENCY BUILDER
# ---------------------------------------------------------------------------

def build_cobol_dependencies(path: Path) -> list:
    """Extract dependencies from a COBOL source file."""
    source = path.read_text(encoding='utf-8', errors='replace')
    deps = []
    seen = set()

    def _add(target, rel):
        key = (target, rel)
        if key not in seen:
            seen.add(key)
            deps.append({'target_name': target, 'relationship_type': rel})

    # Prologue parser
    prologue = CobolPrologueParser().parse(source)
    for name in prologue['calls']:
        _add(name, 'calls')
    for name in prologue['tables']:
        _add(name, 'references')
    for name in prologue['ui']:
        _add(name, 'binds-dds')
    for name in prologue['copies']:
        _add(name, 'copies')

    # Procedure division scanner (may find additional CALL/COPY targets)
    proc = ProcedureDivisionScanner().scan(source)
    for name in proc['calls']:
        _add(name, 'calls')
    for name in proc['copies']:
        _add(name, 'copies')

    return deps


def build_dds_dependencies(path: Path) -> list:
    """Extract dependencies from a DDS display-file source."""
    source = path.read_text(encoding='utf-8', errors='replace')
    deps = []
    seen = set()

    def _add(target, rel):
        key = (target, rel)
        if key not in seen:
            seen.add(key)
            deps.append({'target_name': target, 'relationship_type': rel})

    refs = DdsReferenceExtractor().extract(source)
    for name in refs['pfile']:
        _add(name, 'references')
    for name in refs['ref']:
        _add(name, 'references')

    return deps


def build_cl_dependencies(path: Path) -> list:
    """Extract dependencies from a CL source member."""
    source = path.read_text(encoding='utf-8', errors='replace')
    deps = []
    seen = set()

    def _add(target, rel):
        key = (target, rel)
        if key not in seen:
            seen.add(key)
            deps.append({'target_name': target, 'relationship_type': rel})

    refs = ClMemberScanner().scan(source)
    for name in refs['calls']:
        _add(name, 'calls')
    for name in refs['sbmjob']:
        _add(name, 'calls')
    for name in refs['crtpgm']:
        _add(name, 'references')
    for name in refs['crtdspf']:
        _add(name, 'references')
    for name in refs['crtprtf']:
        _add(name, 'references')
    for name in refs['crtsqlcbli']:
        _add(name, 'references')

    return deps


# ---------------------------------------------------------------------------
# YAML SERIALIZER (stdlib-only, no PyYAML dependency)
# ---------------------------------------------------------------------------

def _yaml_needs_quoting(s: str) -> bool:
    """Return True if string ``s`` must be quoted in YAML."""
    if not s:
        return True
    if s.lower() in ('true', 'false', 'null', 'yes', 'no', 'on', 'off',
                     '~', '.inf', '-.inf', '.nan'):
        return True
    # Starts with a character that is special in YAML
    if s[0] in '#!&*?|>{}[],\'"`:@%':
        return True
    # Contains colon-space (would break key: value parsing)
    if ': ' in s or s.endswith(':'):
        return True
    # ISO 8601 timestamps are YAML 1.1 timestamp literals — quote to keep as strings
    if re.match(r'^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}', s):
        return True
    # Contains newline or tab
    if '\n' in s or '\t' in s or '\r' in s:
        return True
    return False


def _yaml_scalar(s) -> str:
    """Serialise a scalar value to YAML representation."""
    if s is None:
        return 'null'
    if isinstance(s, bool):
        return 'true' if s else 'false'
    if isinstance(s, int):
        return str(s)
    s = str(s)
    if _yaml_needs_quoting(s):
        # Use JSON-style double-quoted string (valid YAML)
        import json as _json
        return _json.dumps(s)
    return s


def _yaml_dump_lines(obj, indent: int = 0) -> list:
    """Recursively serialise *obj* to a list of YAML text lines."""
    pad = '  ' * indent
    lines = []

    if isinstance(obj, dict):
        for key, value in obj.items():
            k = _yaml_scalar(key)
            if isinstance(value, list):
                if not value:
                    lines.append(f'{pad}{k}: []')
                else:
                    lines.append(f'{pad}{k}:')
                    lines.extend(_yaml_dump_lines(value, indent + 1))
            elif isinstance(value, dict):
                if not value:
                    lines.append(f'{pad}{k}: {{}}')
                else:
                    lines.append(f'{pad}{k}:')
                    lines.extend(_yaml_dump_lines(value, indent + 1))
            else:
                lines.append(f'{pad}{k}: {_yaml_scalar(value)}')

    elif isinstance(obj, list):
        for item in obj:
            if isinstance(item, dict):
                items = list(item.items())
                first_k, first_v = items[0]
                fk = _yaml_scalar(first_k)
                if isinstance(first_v, (list, dict)):
                    lines.append(f'{pad}- {fk}:')
                    lines.extend(_yaml_dump_lines(first_v, indent + 2))
                else:
                    lines.append(f'{pad}- {fk}: {_yaml_scalar(first_v)}')
                for k, v in items[1:]:
                    ks = _yaml_scalar(k)
                    if isinstance(v, list):
                        if not v:
                            lines.append(f'{pad}  {ks}: []')
                        else:
                            lines.append(f'{pad}  {ks}:')
                            lines.extend(_yaml_dump_lines(v, indent + 2))
                    elif isinstance(v, dict):
                        if not v:
                            lines.append(f'{pad}  {ks}: {{}}')
                        else:
                            lines.append(f'{pad}  {ks}:')
                            lines.extend(_yaml_dump_lines(v, indent + 2))
                    else:
                        lines.append(f'{pad}  {ks}: {_yaml_scalar(v)}')
            else:
                lines.append(f'{pad}- {_yaml_scalar(item)}')

    return lines


def dump_yaml(obj) -> str:
    """Serialise *obj* to a YAML string (no PyYAML dependency)."""
    lines = _yaml_dump_lines(obj)
    return '\n'.join(lines) + '\n'


# ---------------------------------------------------------------------------
# MANIFEST GENERATOR
# ---------------------------------------------------------------------------

def build_file_entry(path: Path, source_dir: Path) -> dict:
    """Build a single manifest entry for *path*."""
    rel = str(path.relative_to(source_dir.parent)).replace(os.sep, '/')
    file_type, status = classify_file(path)
    lc = count_lines(path)
    sha = compute_sha256(path)
    suffix = path.suffix.lower()

    if file_type == 'cobol':
        deps = build_cobol_dependencies(path)
    elif file_type == 'dds':
        deps = build_dds_dependencies(path)
    elif file_type == 'cl':
        deps = build_cl_dependencies(path)
    else:
        deps = []

    return {
        'path': rel,
        'type': file_type,
        'status': status,
        'line_count': lc,
        'sha256_checksum': sha,
        'dependencies': deps,
    }


def build_design_only_entry(prog: dict, source_dir_name: str) -> dict:
    """Build a manifest entry for a design-only program (no source file)."""
    deps = []
    if prog.get('dds'):
        deps.append({'target_name': prog['dds'], 'relationship_type': 'binds-dds'})
    return {
        'path': f"{source_dir_name}/{prog['name']}.cbl",
        'type': 'cobol',
        'status': 'design-only',
        'line_count': 0,
        'sha256_checksum': None,
        'dependencies': deps,
    }


def build_missing_callee_entry(item: dict) -> dict:
    """Build a manifest entry for a missing callee (no source file)."""
    return {
        'path': item['name'],
        'type': 'cobol',
        'status': 'missing-callee',
        'line_count': 0,
        'sha256_checksum': None,
        'dependencies': [],
    }


def build_missing_printer_entry(item: dict) -> dict:
    """Build a manifest entry for a missing printer file."""
    return {
        'path': item['name'],
        'type': 'printer-file',
        'status': 'missing-callee',
        'line_count': 0,
        'sha256_checksum': None,
        'dependencies': [],
    }


def build_runtime_scheduler_entry(item: dict) -> dict:
    """Build a manifest entry for a runtime-only scheduler program."""
    return {
        'path': item['name'],
        'type': 'cobol',
        'status': 'missing-callee',
        'line_count': 0,
        'sha256_checksum': None,
        'dependencies': [],
    }


def generate_manifest(repo_root: Path, source_dir: Path,
                      output_path: Path) -> dict:
    """Scan *source_dir*, build the manifest, and write it to *output_path*.

    Returns the manifest dict.
    """
    if not source_dir.is_dir():
        print(f"ERROR: Source directory not found: {source_dir}", file=sys.stderr)
        sys.exit(1)

    # Collect and sort all files for deterministic output
    all_files = sorted(
        p for p in source_dir.rglob('*') if p.is_file()
    )

    file_entries = [build_file_entry(p, source_dir) for p in all_files]
    real_file_count = len(file_entries)

    # Append synthetic design-only entries
    source_dir_name = source_dir.name
    design_only_entries = [
        build_design_only_entry(p, source_dir_name)
        for p in DESIGN_ONLY_PROGRAMS
    ]

    # Append missing-callee entries
    missing_entries = (
        [build_missing_callee_entry(c) for c in MISSING_CALLEES]
        + [build_missing_printer_entry(p) for p in MISSING_PRINTER_FILES]
        + [build_runtime_scheduler_entry(s) for s in RUNTIME_ONLY_SCHEDULER_REFS]
    )

    all_entries = sorted(
        file_entries + design_only_entries + missing_entries,
        key=lambda e: e['path']
    )

    manifest = {
        'generation_timestamp': datetime.now(timezone.utc).strftime(
            '%Y-%m-%dT%H:%M:%SZ'),
        'generator_version': GENERATOR_VERSION,
        'repository_root': source_dir_name,
        'total_file_count': real_file_count,
        'files': all_entries,
    }

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(
        '---\n# PCIS Repository Manifest — generated by manifest/generate_manifest.py\n'
        '# Edit manifest/generate_manifest.py to regenerate after source changes.\n'
        + dump_yaml(manifest),
        encoding='utf-8'
    )
    print(f"Manifest written to {output_path} "
          f"({real_file_count} real files, {len(all_entries)} total entries)",
          file=sys.stderr)
    return manifest


# ---------------------------------------------------------------------------
# CLI ENTRY POINT
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description='Generate PCIS repository manifest YAML.')
    parser.add_argument(
        '--repo-root', default='.',
        help='Repository root directory (default: current directory)')
    parser.add_argument(
        '--source-dir',
        default='Property_Casualty_Insurance_System',
        help='Source directory name relative to repo root '
             '(default: Property_Casualty_Insurance_System)')
    parser.add_argument(
        '--output', default='manifest/pcis-manifest.yaml',
        help='Output manifest path relative to repo root '
             '(default: manifest/pcis-manifest.yaml)')
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    source_dir = repo_root / args.source_dir
    output_path = repo_root / args.output

    generate_manifest(repo_root, source_dir, output_path)


if __name__ == '__main__':
    main()
