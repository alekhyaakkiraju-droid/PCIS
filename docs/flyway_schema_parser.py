"""
Parse PostgreSQL CREATE TABLE blocks from Flyway migration SQL (WO-150).

Used to validate docs/data-dictionary.yaml against shared-libs/pcis-schema V1.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path


@dataclass
class FlywayColumn:
    name: str
    data_type: str
    nullable: bool = True


@dataclass
class FlywayTable:
    name: str
    columns: list[FlywayColumn] = field(default_factory=list)


CREATE_RE = re.compile(
    r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?([A-Za-z][A-Za-z0-9_]*)\s*\((.*?)\)\s*;",
    re.I | re.S,
)

COL_LINE_RE = re.compile(
    r"^\s*([A-Z][A-Z0-9_]*)\s+(.+?)\s*(?:,\s*)?$",
    re.I | re.M,
)


def _normalize_type(raw: str) -> str:
    t = re.sub(r"\s+", "", raw.upper())
    t = re.sub(r"TIMESTAMP\(\d+\)", "TIMESTAMP", t)
    return t


def parse_flyway_schema(path: Path) -> dict[str, FlywayTable]:
    text = path.read_text(encoding="utf-8")
    tables: dict[str, FlywayTable] = {}
    for m in CREATE_RE.finditer(text):
        tname = m.group(1).upper()
        body = m.group(2)
        table = FlywayTable(name=tname)
        for raw_line in body.splitlines():
            line = raw_line.split("--", 1)[0].strip().rstrip(",")
            if not line:
                continue
            upper = line.upper()
            if upper.startswith(("PRIMARY KEY", "CONSTRAINT", "UNIQUE", "CHECK", "FOREIGN KEY")):
                continue
            if line.startswith("(") or line.startswith(")"):
                continue
            parts = line.split()
            if len(parts) < 2:
                continue
            col_name = parts[0].upper()
            if not re.fullmatch(r"[A-Z][A-Z0-9_]*", col_name):
                continue
            type_tokens: list[str] = []
            i = 1
            while i < len(parts):
                token = parts[i]
                if token.upper() in {"NOT", "NULL", "DEFAULT", "GENERATED", "PRIMARY", "REFERENCES"}:
                    break
                type_tokens.append(token)
                i += 1
            col_type = _normalize_type(" ".join(type_tokens))
            nullable = "NOT NULL" not in upper
            if "GENERATED" in upper and "IDENTITY" in upper:
                nullable = False
            table.columns.append(FlywayColumn(col_name, col_type, nullable))
        tables[tname] = table
    return tables


def resolution_column_name(entry: dict) -> str:
    """Canonical PostgreSQL column name for a dictionary column entry."""
    ddl = (entry.get("ddl_column_name") or "").strip()
    if ddl:
        return ddl.upper()
    resolution = (entry.get("resolution") or "").strip()
    return resolution.upper()


def flyway_match_name(entry: dict) -> str:
    """Column name as deployed in Flyway (prefer design DDL name over G-06 resolution)."""
    return resolution_column_name(entry)
