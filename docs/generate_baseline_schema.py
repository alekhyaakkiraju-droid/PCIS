#!/usr/bin/env python3
"""
Generate Flyway V1 baseline schema from PCIS_Database_Design.md (WO-149).

Column names follow the design document (not COBOL resolution names).
"""

from __future__ import annotations

import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DESIGN_DOC = REPO_ROOT / "Property_Casualty_Insurance_System" / "PCIS_Database_Design.md"
RPT_DDL = REPO_ROOT / "baseline" / "ddl" / "RPT_RUN_LOG_T_reconciled.sql"
OUTPUT = REPO_ROOT / "shared-libs" / "pcis-schema" / "db" / "migration" / "V1__baseline_schema.sql"

AUDIT_COLS = ("CRT_USER", "CRT_TIMESTAMP", "UPD_USER", "UPD_TIMESTAMP")

PREMIUM_RESERVE_COLS = {
    "PREM_ANNUAL",
    "RESERVE_AMT",
    "APPROVED_AMT",
    "FINAL_PREMIUM",
    "QUOTE_PREMIUM",
    "LIMIT_AMT",
    "CESSION_THRESHOLD",
    "AUTHORITY_LIMIT",
    "PAID_TO_DATE",
    "OLD_AMT",
    "NEW_AMT",
    "THRESHOLD_AMT",
    "RECOVERY_AMT",
    "COV_PREMIUM",
    "COMPONENT_AMT",
    "CESSION_AMT",
}

RATE_COLS = {
    "BASE_RATE",
    "FACTOR_VALUE",
    "COMM_RATE",
    "TAX_PCT",
    "DISC_PCT",
    "SUR_PCT",
}

IDENTITY_PK = {
    "ADDRESS_ID",
    "CONTACT_ID",
    "LICENSE_ID",
    "AGT_COMM_ID",
    "COMMISSION_ID",
    "RATE_ID",
    "QUOTE_COV_ID",
    "UW_RULE_ID",
    "REFERRAL_ID",
    "DECISION_ID",
    "DEDUCT_ID",
    "HIST_ID",
    "VEHICLE_ID",
    "PROPERTY_ID",
    "ENDORSE_ID",
    "RATE_TABLE_ID",
    "RATE_FACTOR_ID",
    "CALC_ID",
    "DETAIL_ID",
    "DISC_RULE_ID",
    "SUR_RULE_ID",
    "TAX_ID",
    "RISK_FACTOR_ID",
    "BILL_PLAN_ID",
    "BILL_SCHED_ID",
    "NOTICE_ID",
    "INVOICE_ID",
    "PAYMENT_ID",
    "REFUND_ID",
    "RESERVE_HIST_ID",
    "RES_HIST_ID",
    "NOTE_ID",
    "DOC_ID",
    "APPROVAL_ID",
    "RECOVERY_ID",
    "CESSION_ID",
    "DOCUMENT_ID",
    "RUN_LOG_ID",
    "PARM_ID",
    "LOG_ID",
    "LEDGER_ID",
}
# RESERVE_HIST_ID is PK on CLAIM_RESERVE_T but FK on APPROVAL_T

BUSINESS_PK = {
    ("CUSTOMER_T", "CUST_ID"),
    ("AGENT_T", "AGT_ID"),
    ("QUOTE_T", "QUOTE_ID"),
    ("POLICY_T", "POL_NBR"),
    ("COVERAGE_T", "COVERAGE_ID"),
    ("COVERAGE_TYPE_T", "COV_TYPE"),
    ("CANCELLATION_REASON_T", "CANCEL_REASON"),
    ("CLAIM_T", "CLAIM_ID"),
    ("CLAIM_ADJUSTER_T", "ADJUSTER_ID"),
    ("REINSURANCE_TREATY_T", "TREATY_ID"),
    ("SEC_USER_T", "USER_ID"),
}

COMPOSITE_PK = {
    "ROLE_MENU_T": ("ROLE_CODE", "MENU_OPTION"),
    "CODE_TABLE_T": ("CODE_TYPE", "CODE_VALUE"),
}

NON_IDENTITY_COLS = {
    ("APPROVAL_T", "RESERVE_HIST_ID"),
}

EXTRA_COLUMNS: dict[str, list[tuple[str, str]]] = {
    "CUSTOMER_T": [
        ("CUST_GENDER", "CHAR(1)"),
        ("CUST_MARITAL_ST", "CHAR(1)"),
        ("CUST_CREDIT_SCORE", "INTEGER"),
    ],
    "BILLING_SCHEDULE_T": [
        ("COMM_CALC_FLAG", "CHAR(1)"),
    ],
    "CLAIM_RESERVE_T": [
        ("PAID_TO_DATE", "DECIMAL(11,2)"),
    ],
    "APPROVAL_T": [
        ("RESERVE_HIST_ID", "BIGINT"),
    ],
}

FOREIGN_KEYS: list[tuple[str, str, str, str]] = [
    ("CUSTOMER_T", "ASSIGNED_AGENT", "AGENT_T", "AGT_ID"),
    ("CUSTOMER_ADDRESS_T", "CUST_ID", "CUSTOMER_T", "CUST_ID"),
    ("CUSTOMER_CONTACT_T", "CUST_ID", "CUSTOMER_T", "CUST_ID"),
    ("AGENT_LICENSE_T", "AGT_ID", "AGENT_T", "AGT_ID"),
    ("AGENT_COMMISSION_T", "AGT_ID", "AGENT_T", "AGT_ID"),
    ("AGENT_COMMISSION_T", "POL_NBR", "POLICY_T", "POL_NBR"),
    ("COMMISSION_T", "AGT_ID", "AGENT_T", "AGT_ID"),
    ("COMMISSION_T", "POL_NBR", "POLICY_T", "POL_NBR"),
    ("QUOTE_T", "CUST_ID", "CUSTOMER_T", "CUST_ID"),
    ("QUOTE_COVERAGE_T", "QUOTE_ID", "QUOTE_T", "QUOTE_ID"),
    ("UW_REFERRAL_T", "POL_NBR", "POLICY_T", "POL_NBR"),
    ("UW_REFERRAL_T", "UW_RULE_ID", "UW_RULE_T", "UW_RULE_ID"),
    ("UW_DECISION_T", "REFERRAL_ID", "UW_REFERRAL_T", "REFERRAL_ID"),
    ("POLICY_T", "CUST_ID", "CUSTOMER_T", "CUST_ID"),
    ("POLICY_T", "AGT_ID", "AGENT_T", "AGT_ID"),
    ("COVERAGE_T", "POL_NBR", "POLICY_T", "POL_NBR"),
    ("DEDUCTIBLE_T", "COVERAGE_ID", "COVERAGE_T", "COVERAGE_ID"),
    ("POLICY_HISTORY_T", "POL_NBR", "POLICY_T", "POL_NBR"),
    ("POLICY_VEHICLE_T", "POL_NBR", "POLICY_T", "POL_NBR"),
    ("POLICY_PROPERTY_T", "POL_NBR", "POLICY_T", "POL_NBR"),
    ("ENDORSEMENT_T", "POL_NBR", "POLICY_T", "POL_NBR"),
    ("RATE_FACTOR_T", "RATE_TABLE_ID", "RATE_TABLE_T", "RATE_TABLE_ID"),
    ("PREMIUM_CALC_T", "POL_NBR", "POLICY_T", "POL_NBR"),
    ("PREMIUM_CALC_DETAIL_T", "CALC_ID", "PREMIUM_CALC_T", "CALC_ID"),
    ("BILLING_PLAN_T", "POL_NBR", "POLICY_T", "POL_NBR"),
    ("BILLING_SCHEDULE_T", "POL_NBR", "POLICY_T", "POL_NBR"),
    ("BILLING_SCHEDULE_T", "BILL_PLAN_ID", "BILLING_PLAN_T", "BILL_PLAN_ID"),
    ("BILLING_NOTICE_T", "BILL_SCHED_ID", "BILLING_SCHEDULE_T", "BILL_SCHED_ID"),
    ("BILLING_NOTICE_T", "CUST_ID", "CUSTOMER_T", "CUST_ID"),
    ("INVOICE_T", "BILL_SCHED_ID", "BILLING_SCHEDULE_T", "BILL_SCHED_ID"),
    ("PAYMENT_T", "INVOICE_ID", "INVOICE_T", "INVOICE_ID"),
    ("REFUND_T", "POL_NBR", "POLICY_T", "POL_NBR"),
    ("CLAIM_T", "POL_NBR", "POLICY_T", "POL_NBR"),
    ("CLAIM_T", "CUST_ID", "CUSTOMER_T", "CUST_ID"),
    ("CLAIM_RESERVE_T", "CLAIM_ID", "CLAIM_T", "CLAIM_ID"),
    ("CLAIM_RESERVE_HISTORY_T", "CLAIM_ID", "CLAIM_T", "CLAIM_ID"),
    ("CLAIM_PAYMENT_T", "CLAIM_ID", "CLAIM_T", "CLAIM_ID"),
    ("CLAIM_PAYMENT_T", "ADJUSTER_ID", "CLAIM_ADJUSTER_T", "ADJUSTER_ID"),
    ("CLAIM_NOTE_T", "CLAIM_ID", "CLAIM_T", "CLAIM_ID"),
    ("CLAIM_DOCUMENT_T", "CLAIM_ID", "CLAIM_T", "CLAIM_ID"),
    ("APPROVAL_T", "CLAIM_ID", "CLAIM_T", "CLAIM_ID"),
    ("APPROVAL_T", "RESERVE_HIST_ID", "CLAIM_RESERVE_T", "RESERVE_HIST_ID"),
    ("APPROVAL_T", "APPROVER_ID", "CLAIM_ADJUSTER_T", "ADJUSTER_ID"),
    ("RECOVERY_T", "CLAIM_ID", "CLAIM_T", "CLAIM_ID"),
    ("REINSURANCE_CESSION_T", "TREATY_ID", "REINSURANCE_TREATY_T", "TREATY_ID"),
    ("REINSURANCE_CESSION_T", "CLAIM_ID", "CLAIM_T", "CLAIM_ID"),
    ("COMMISSION_LEDGER_T", "AGT_ID", "AGENT_T", "AGT_ID"),
    ("COMMISSION_LEDGER_T", "BILL_SCHED_ID", "BILLING_SCHEDULE_T", "BILL_SCHED_ID"),
]

SEQUENCES = [
    "SEQ_CUSTOMER_ID",
    "SEQ_AGENT_ID",
    "SEQ_QUOTE_ID",
    "SEQ_POLICY_NBR",
    "SEQ_COVERAGE_ID",
    "SEQ_CLAIM_ID",
    "SEQ_ADJUSTER_ID",
    "SEQ_TREATY_ID",
    "SEQ_COMMISSION_ID",
    "SEQ_AUDIT_LOG_ID",
    "SEQ_INVOICE_ID",
    "SEQ_PAYMENT_ID",
    "SEQ_REFUND_ID",
    "SEQ_DOCUMENT_ID",
    "SEQ_LEDGER_ID",
]

CHECK_CONSTRAINTS = [
    ("BILLING_SCHEDULE_T", "chk_bill_sched_status", "SCHED_STATUS IN ('O','P','V','D','L')"),
    ("CLAIM_PAYMENT_T", "chk_claim_pmt_status", "PAYMENT_STATUS IN ('I','P','V')"),
    ("APPROVAL_T", "chk_approval_status", "APPROVAL_STATUS IN ('REQUESTED','APPROVED','DENIED','PENDING')"),
]


@dataclass
class Column:
    name: str
    data_type: str
    not_null: bool = False


@dataclass
class Table:
    name: str
    columns: list[Column] = field(default_factory=list)
    domain: str = ""


def parse_design(path: Path) -> dict[str, Table]:
    text = path.read_text(encoding="utf-8")
    tables: dict[str, Table] = {}
    current: Table | None = None
    in_col_table = False
    heading_re = re.compile(r"^###\s+([A-Z][A-Z0-9_]*_T)\s*$")
    domain_re = re.compile(r"\*\*Domain:\*\*\s*(\w+)")
    row_re = re.compile(r"^\|\s*([A-Z][A-Z0-9_]*)\s*\|\s*([^|]+?)\s*\|\s*(.*?)\s*\|\s*$")
    create_re = re.compile(
        r"CREATE\s+TABLE\s+([A-Z][A-Z0-9_]*_T)\s*\((.*?)\)\s*;",
        re.I | re.S,
    )
    col_def_re = re.compile(
        r"^\s*([A-Z][A-Z0-9_]*)\s+([A-Z]+(?:\s*\(\s*\d+(?:\s*,\s*\d+)?\s*\))?(?:\s+GENERATED\s+ALWAYS\s+AS\s+IDENTITY)?)",
        re.I,
    )

    for line in text.splitlines():
        hm = heading_re.match(line.strip())
        if hm:
            current = Table(name=hm.group(1))
            tables[current.name] = current
            in_col_table = False
            continue
        if current is None:
            continue
        dm = domain_re.search(line)
        if dm:
            current.domain = dm.group(1)
        if line.strip().startswith("| Column ") or line.strip().startswith("|Column "):
            in_col_table = True
            continue
        if in_col_table:
            if not line.strip().startswith("|"):
                in_col_table = False
                continue
            if re.match(r"^\|\s*-+", line.strip()):
                continue
            rm = row_re.match(line.strip())
            if rm and rm.group(1).upper() != "COLUMN":
                col_name = rm.group(1).upper()
                dtype = rm.group(2).strip()
                if not any(c.name == col_name for c in current.columns):
                    current.columns.append(Column(col_name, dtype))

    for m in create_re.finditer(text):
        tname = m.group(1).upper()
        body = m.group(2)
        table = tables.get(tname) or Table(name=tname)
        tables[tname] = table
        for chunk in _split_create_table_body(body):
            if not chunk or chunk.upper().startswith("PRIMARY KEY"):
                continue
            cm = col_def_re.match(chunk)
            if not cm:
                continue
            cname = cm.group(1).upper()
            ctype = re.sub(r"\s+", " ", cm.group(2).upper().strip())
            ctype = re.sub(r"\(\s*", "(", ctype)
            ctype = re.sub(r"\s*,\s*", ",", ctype)
            ctype = re.sub(r"\s*\)", ")", ctype)
            ctype = re.sub(r"\s+GENERATED.*", "", ctype, flags=re.I)
            not_null = "NOT NULL" in chunk.upper()
            existing = next((c for c in table.columns if c.name == cname), None)
            if existing:
                existing.data_type = ctype or existing.data_type
                existing.not_null = existing.not_null or not_null
            else:
                table.columns.append(Column(cname, ctype, not_null))

    for tname, extras in EXTRA_COLUMNS.items():
        table = tables.setdefault(tname, Table(name=tname))
        for col_name, dtype in extras:
            if not any(c.name == col_name for c in table.columns):
                table.columns.append(Column(col_name, dtype))

    for table in tables.values():
        for col in AUDIT_COLS:
            if not any(c.name == col for c in table.columns):
                table.columns.append(Column(col, "TIMESTAMP" if "TIMESTAMP" in col else "VARCHAR(10)"))

    return tables


def _split_create_table_body(body: str) -> list[str]:
    """Split CREATE TABLE column list on commas outside parentheses."""
    parts: list[str] = []
    current: list[str] = []
    depth = 0
    for ch in body:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth = max(depth - 1, 0)
        if ch == "," and depth == 0:
            parts.append("".join(current).strip())
            current = []
            continue
        current.append(ch)
    tail = "".join(current).strip()
    if tail:
        parts.append(tail)
    return parts


def pg_type(col_name: str, ddl_type: str) -> str:
    dt = (ddl_type or "").upper().replace(" ", "")
    if col_name in RATE_COLS:
        return "NUMERIC(7,4)"
    if col_name in PREMIUM_RESERVE_COLS:
        return "NUMERIC(13,2)"
    m = re.match(r"DECIMAL\((\d+),(\d+)\)", dt)
    if m:
        p, s = int(m.group(1)), int(m.group(2))
        if s == 4 or col_name in RATE_COLS:
            return "NUMERIC(7,4)"
        if p >= 11 and col_name in PREMIUM_RESERVE_COLS:
            return "NUMERIC(13,2)"
        if p <= 9 and s == 2:
            return "NUMERIC(11,2)"
        return f"NUMERIC({p},{s})"
    m = re.match(r"NUMERIC\((\d+),(\d+)\)", dt)
    if m:
        return f"NUMERIC({m.group(1)},{m.group(2)})"
    m = re.match(r"VARCHAR\((\d+)\)", dt)
    if m:
        return f"VARCHAR({m.group(1)})"
    m = re.match(r"CHAR\((\d+)\)", dt)
    if m:
        return f"CHAR({m.group(1)})"
    if dt == "DECIMAL":
        return "NUMERIC(11,2)"
    if dt in {"DATE", "TIMESTAMP", "INTEGER", "BIGINT"}:
        return dt
    if dt == "TIMESTAMP(6)":
        return "TIMESTAMP(6)"
    return ddl_type or "TEXT"


def table_pk_cols(table: Table) -> list[str]:
    if table.name in COMPOSITE_PK:
        return list(COMPOSITE_PK[table.name])
    for col in table.columns:
        if (table.name, col.name) in BUSINESS_PK:
            return [col.name]
    for col in table.columns:
        desc = (col.data_type or "").upper()
        if col.name.endswith("_ID") and (
            "BIGINT" in desc or col.name in IDENTITY_PK
        ):
            if (table.name, col.name) in NON_IDENTITY_COLS:
                continue
            return [col.name]
    return []


def is_identity(table: str, col: Column, pk_cols: list[str]) -> bool:
    if (table, col.name) in NON_IDENTITY_COLS:
        return False
    if (table, col.name) in BUSINESS_PK:
        return False
    if col.name not in pk_cols:
        return False
    if "BIGINT" in col.data_type.upper():
        return True
    return False


def render_column(table: str, col: Column, pk_cols: list[str]) -> str:
    typ = pg_type(col.name, col.data_type)
    parts = [f"    {col.name} {typ}"]
    if is_identity(table, col, pk_cols):
        parts.append("GENERATED ALWAYS AS IDENTITY")
    if col.not_null and col.name not in AUDIT_COLS:
        parts.append("NOT NULL")
    return " ".join(parts)


def render_rpt_run_log() -> list[str]:
    lines = [
        "-- RPT_RUN_LOG_T (reconciled WO-237 + audit columns)",
        "CREATE TABLE RPT_RUN_LOG_T (",
        "    RUN_LOG_ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,",
        "    PGM_NAME VARCHAR(10) NOT NULL,",
        "    RUN_DATE DATE NOT NULL,",
        "    REC_SELECTED INTEGER NOT NULL DEFAULT 0,",
        "    REC_UPDATED INTEGER NOT NULL DEFAULT 0,",
        "    REC_ERRORS INTEGER NOT NULL DEFAULT 0,",
        "    REC_DELINQUENT INTEGER,",
        "    START_TIMESTAMP TIMESTAMP(6) NOT NULL,",
        "    END_TIMESTAMP TIMESTAMP(6) NOT NULL,",
        "    CRT_USER VARCHAR(10),",
        "    CRT_TIMESTAMP TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,",
        "    UPD_USER VARCHAR(10),",
        "    UPD_TIMESTAMP TIMESTAMP",
        ");",
        "",
    ]
    return lines


def render_audit_log() -> list[str]:
    lines = [
        "-- AUDIT_LOG_T (monthly range partitions on CRT_TIMESTAMP)",
        "CREATE TABLE AUDIT_LOG_T (",
        "    LOG_ID BIGINT GENERATED ALWAYS AS IDENTITY,",
        "    PROGRAM_NAME VARCHAR(10),",
        "    ACTION_CODE VARCHAR(10),",
        "    TABLE_NAME VARCHAR(30),",
        "    RECORD_KEY VARCHAR(40),",
        "    USER_ID VARCHAR(10),",
        "    OLD_VALUE VARCHAR(100),",
        "    NEW_VALUE VARCHAR(100),",
        "    LOG_TIMESTAMP TIMESTAMP,",
        "    CRT_USER VARCHAR(10),",
        "    CRT_TIMESTAMP TIMESTAMP NOT NULL,",
        "    UPD_USER VARCHAR(10),",
        "    UPD_TIMESTAMP TIMESTAMP,",
        "    PRIMARY KEY (LOG_ID, CRT_TIMESTAMP)",
        ") PARTITION BY RANGE (CRT_TIMESTAMP);",
        "",
    ]
    for month in range(1, 13):
        start = f"2026-{month:02d}-01"
        end_month = month + 1
        end_year = 2026
        if end_month > 12:
            end_month = 1
            end_year = 2027
        end = f"{end_year}-{end_month:02d}-01"
        part = f"AUDIT_LOG_T_y2026m{month:02d}"
        lines.append(
            f"CREATE TABLE {part} PARTITION OF AUDIT_LOG_T "
            f"FOR VALUES FROM ('{start}') TO ('{end}');"
        )
    lines.extend(
        [
            "CREATE TABLE AUDIT_LOG_T_default PARTITION OF AUDIT_LOG_T DEFAULT;",
            "",
        ]
    )
    return lines


def render_commission_ledger() -> list[str]:
    return [
        "-- COMMISSION_LEDGER_T (code-witnessed, not in 55-table inventory)",
        "CREATE TABLE COMMISSION_LEDGER_T (",
        "    LEDGER_ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,",
        "    AGT_ID VARCHAR(8) NOT NULL,",
        "    BILL_SCHED_ID BIGINT NOT NULL,",
        "    COMM_RATE NUMERIC(7,4),",
        "    COMMISSION_AMT NUMERIC(11,2) NOT NULL,",
        "    CALC_DATE DATE NOT NULL,",
        "    CRT_USER VARCHAR(10),",
        "    CRT_TIMESTAMP TIMESTAMP,",
        "    UPD_USER VARCHAR(10),",
        "    UPD_TIMESTAMP TIMESTAMP,",
        "    CONSTRAINT uq_comm_ledger_bill_sched UNIQUE (BILL_SCHED_ID)",
        ");",
        "",
    ]


def render_outbox_events() -> list[str]:
    return [
        "-- outbox_events (transactional outbox for domain event relay)",
        "CREATE TABLE outbox_events (",
        "    ID BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,",
        "    AGGREGATE_TYPE VARCHAR(100) NOT NULL,",
        "    AGGREGATE_ID VARCHAR(100) NOT NULL,",
        "    EVENT_TYPE VARCHAR(100) NOT NULL,",
        "    PAYLOAD JSONB NOT NULL,",
        "    IDEMPOTENCY_KEY UUID NOT NULL,",
        "    STATUS VARCHAR(20) NOT NULL DEFAULT 'PENDING',",
        "    ATTEMPT_COUNT INTEGER NOT NULL DEFAULT 0,",
        "    NEXT_ATTEMPT_AT TIMESTAMP,",
        "    LAST_ERROR VARCHAR(500),",
        "    CRT_USER VARCHAR(10),",
        "    CRT_TIMESTAMP TIMESTAMP,",
        "    UPD_USER VARCHAR(10),",
        "    UPD_TIMESTAMP TIMESTAMP,",
        "    CONSTRAINT uq_outbox_idempotency UNIQUE (IDEMPOTENCY_KEY)",
        ");",
        "CREATE INDEX idx_outbox_relay ON outbox_events (STATUS, NEXT_ATTEMPT_AT) WHERE STATUS = 'PENDING';",
        "",
    ]


def render_table(table: Table) -> list[str]:
    if table.name == "RPT_RUN_LOG_T":
        return render_rpt_run_log()
    if table.name == "AUDIT_LOG_T":
        return render_audit_log()

    lines = [f"-- {table.name} (domain: {table.domain or 'N/A'})", f"CREATE TABLE {table.name} ("]
    col_lines = []
    pk_cols = table_pk_cols(table)

    for col in table.columns:
        col_lines.append(render_column(table.name, col, pk_cols) + ",")

    if pk_cols:
        col_lines.append(f"    PRIMARY KEY ({', '.join(pk_cols)})")
    else:
        col_lines[-1] = col_lines[-1].rstrip(",")

    lines.extend(col_lines)
    lines.append(");")
    lines.append("")
    return lines


def render_foreign_keys() -> list[str]:
    lines = ["-- Foreign key constraints", ""]
    seen: set[str] = set()
    for child, col, parent, pcol in FOREIGN_KEYS:
        name = f"fk_{child.lower()}_{col.lower()}"
        if name in seen:
            continue
        seen.add(name)
        lines.append(
            f"ALTER TABLE {child} ADD CONSTRAINT {name} "
            f"FOREIGN KEY ({col}) REFERENCES {parent} ({pcol});"
        )
    lines.append("")
    return lines


def render_checks() -> list[str]:
    lines = ["-- Check constraints", ""]
    for table, name, expr in CHECK_CONSTRAINTS:
        lines.append(f"ALTER TABLE {table} ADD CONSTRAINT {name} CHECK ({expr});")
    lines.append("")
    return lines


def render_sequences() -> list[str]:
    lines = ["-- Sequence objects (business document keys)", ""]
    for seq in SEQUENCES:
        lines.append(f"CREATE SEQUENCE {seq} START WITH 100000 INCREMENT BY 1 MINVALUE 1 MAXVALUE 999999999 NO CYCLE CACHE 100;")
    lines.append("")
    return lines


def generate() -> str:
    tables = parse_design(DESIGN_DOC)
    if len(tables) != 55:
        print(f"WARNING: expected 55 tables, found {len(tables)}", file=sys.stderr)

    skip = {"RPT_RUN_LOG_T", "AUDIT_LOG_T"}
    ordered = sorted(t for t in tables if t not in skip)

    parts = [
        "-- PCIS V1 Baseline Schema (WO-149)",
        "-- Authority: Property_Casualty_Insurance_System/PCIS_Database_Design.md",
        "-- PostgreSQL 17 compatible DDL",
        "",
    ]
    parts.extend(render_sequences())

    for tname in ordered:
        parts.extend(render_table(tables[tname]))

    parts.extend(render_rpt_run_log())
    parts.extend(render_audit_log())
    parts.extend(render_commission_ledger())
    parts.extend(render_outbox_events())
    parts.extend(render_foreign_keys())
    parts.extend(render_checks())

    return "\n".join(parts) + "\n"


def main() -> int:
    sql = generate()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(sql, encoding="utf-8")
    table_count = sql.count("CREATE TABLE")
    seq_count = sql.count("CREATE SEQUENCE")
    print(f"Wrote {OUTPUT}")
    print(f"  tables={table_count} sequences={seq_count} bytes={len(sql)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
