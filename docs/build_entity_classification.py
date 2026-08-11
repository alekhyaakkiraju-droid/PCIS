#!/usr/bin/env python3
"""Generate docs/entity-classification.yaml from V1 tables and classification rules (WO-154)."""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "docs"))

from classification_registry import COLUMN_PII, TABLE_TIERS, table_tier  # noqa: E402
from flyway_schema_parser import parse_flyway_schema  # noqa: E402

V1_DDL = ROOT / "shared-libs/pcis-schema/db/migration/V1__baseline_schema.sql"
OUTPUT = ROOT / "docs/entity-classification.yaml"

# WO-154 explicit tier overrides (uppercase manifest enum).
TIER_OVERRIDES: dict[str, str] = {
    "CUSTOMER_T": "RESTRICTED",
    "CUSTOMER_ADDRESS_T": "RESTRICTED",
    "CUSTOMER_CONTACT_T": "RESTRICTED",
    "CLAIM_ADJUSTER_T": "RESTRICTED",
    "AUDIT_LOG_T": "RESTRICTED",
    "CLAIM_PAYMENT_T": "CONFIDENTIAL",
    "BILLING_SCHEDULE_T": "CONFIDENTIAL",
    "INVOICE_T": "CONFIDENTIAL",
    "PREMIUM_CALC_T": "CONFIDENTIAL",
    "COMMISSION_T": "CONFIDENTIAL",  # WO-154 COMMISSION_PAYMENT_T proxy
    "COVERAGE_TYPE_T": "PUBLIC",
    "CANCELLATION_REASON_T": "PUBLIC",
    "CODE_TABLE_T": "PUBLIC",
    "ROLE_MENU_T": "PUBLIC",
    "RATE_TABLE_T": "INTERNAL",
    "RATE_FACTOR_T": "INTERNAL",
}

RETENTION_BY_TIER = {
    "RESTRICTED": 365,
    "CONFIDENTIAL": 365,
    "INTERNAL": 180,
    "PUBLIC": 90,
}

DESCRIPTIONS: dict[str, str] = {
    "AGENT_COMMISSION_T": "Agent commission accruals by policy",
    "AGENT_LICENSE_T": "Agent licensing records",
    "AGENT_T": "Insurance agent master data",
    "APPROVAL_T": "Claim reserve approval workflow",
    "AUDIT_LOG_ARCHIVE_T": "Archived audit trail entries",
    "AUDIT_LOG_T": "Partitioned audit trail with before/after values",
    "BILLING_NOTICE_T": "Billing notice delivery tracking",
    "BILLING_PLAN_T": "Customer billing plan definitions",
    "BILLING_SCHEDULE_T": "Scheduled billing amounts and due dates",
    "CANCELLATION_REASON_T": "Reference codes for policy cancellations",
    "CLAIM_ADJUSTER_T": "Claim adjuster identity and authority limits",
    "CLAIM_DOCUMENT_T": "Claim-related document metadata",
    "CLAIM_NOTE_T": "Free-text claim notes",
    "CLAIM_PAYMENT_T": "Claim payment transactions and payee details",
    "CLAIM_RESERVE_HISTORY_T": "Historical claim reserve adjustments",
    "CLAIM_RESERVE_T": "Current claim reserve amounts",
    "CLAIM_T": "Claim header and status",
    "CODE_TABLE_T": "Generic reference code values",
    "COMMISSION_LEDGER_T": "Posted commission ledger entries",
    "COMMISSION_RATE_T": "Commission rate schedules",
    "COMMISSION_T": "Calculated agent commissions",
    "COVERAGE_T": "Policy coverage line items",
    "COVERAGE_TYPE_T": "Reference coverage type catalog",
    "CUSTOMER_ADDRESS_T": "Customer mailing and service addresses",
    "CUSTOMER_CONTACT_T": "Customer contact methods",
    "CUSTOMER_T": "Customer master with PII",
    "DEDUCTIBLE_T": "Coverage deductible definitions",
    "DISCOUNT_RULE_T": "Premium discount rules",
    "DOCUMENT_T": "Policy and claim document registry",
    "ENDORSEMENT_T": "Policy endorsement history",
    "INVOICE_T": "Customer billing invoices",
    "OUTBOX_EVENTS": "Transactional outbox for domain events",
    "PAYMENT_T": "Premium payment transactions",
    "POLICY_HISTORY_T": "Policy lifecycle event history",
    "POLICY_PROPERTY_T": "Insured property locations",
    "POLICY_T": "Policy contract header",
    "POLICY_VEHICLE_T": "Insured vehicle details",
    "PREMIUM_CALC_DETAIL_T": "Premium calculation line details",
    "PREMIUM_CALC_T": "Premium calculation summaries",
    "QUOTE_COVERAGE_T": "Quoted coverage line items",
    "QUOTE_T": "Insurance quote header",
    "RATE_FACTOR_T": "Rating factor reference values",
    "RATE_TABLE_T": "Base rate reference tables",
    "RECOVERY_T": "Subrogation recovery records",
    "REFUND_T": "Premium refund transactions",
    "REINSURANCE_CESSION_T": "Reinsurance cession allocations",
    "REINSURANCE_TREATY_T": "Reinsurance treaty definitions",
    "RISK_SCORE_FACTOR_T": "Underwriting risk score factors",
    "ROLE_MENU_T": "Security role menu mappings",
    "RPT_PARM_T": "Report parameter definitions",
    "RPT_RUN_LOG_T": "Batch report execution log",
    "SEC_USER_T": "Application user credentials",
    "SURCHARGE_RULE_T": "Premium surcharge rules",
    "TAX_TABLE_T": "Tax rate reference data",
    "UW_DECISION_T": "Underwriting decision records",
    "UW_REFERRAL_T": "Underwriting referral queue",
    "UW_RULE_T": "Underwriting rule expressions",
}


def manifest_tier(table_name: str) -> str:
    up = table_name.upper()
    if up in TIER_OVERRIDES:
        return TIER_OVERRIDES[up]
    registry = TABLE_TIERS.get(up)
    if registry:
        return registry.upper()
    return table_tier(up).upper()


def pii_columns(table_name: str) -> list[str]:
    up = table_name.upper()
    cols = sorted(
        col
        for (tbl, col), (is_pii, _mask) in COLUMN_PII.items()
        if tbl == up and is_pii
    )
    if up == "CLAIM_ADJUSTER_T" and "ADJ_NAME" not in cols:
        cols.append("ADJ_NAME")
    return cols


def _is_partition_name(name: str) -> bool:
    lower = name.lower()
    return "_y2026m" in lower or lower.endswith("_default")


def _yaml_quote(value: str) -> str:
    if not value:
        return '""'
    if any(ch in value for ch in ':#[]{},"\'&*!?|>\\@`'):
        escaped = value.replace("\\", "\\\\").replace('"', '\\"')
        return f'"{escaped}"'
    return value


def _dump_yaml(doc: dict) -> str:
    lines = [
        f"manifest_version: {doc['manifest_version']}",
        f"woref: {doc['woref']}",
        f"source_migration: {doc['source_migration']}",
        f"table_count: {doc['table_count']}",
        "entries:",
    ]
    for entry in doc["entries"]:
        lines.append(f"  - table_name: {entry['table_name']}")
        lines.append(f"    classification_tier: {entry['classification_tier']}")
        lines.append(f"    retention_days: {entry['retention_days']}")
        lines.append("    pii_columns:")
        for col in entry["pii_columns"]:
            lines.append(f"      - {col}")
        lines.append(f"    description: {_yaml_quote(entry['description'])}")
    return "\n".join(lines) + "\n"


def _normalize_table_name(name: str) -> str:
    return "OUTBOX_EVENTS" if name.upper() == "OUTBOX_EVENTS" else name.upper()


def main() -> None:
    tables = parse_flyway_schema(V1_DDL)
    table_names = sorted(
        _normalize_table_name(name)
        for name in tables
        if not _is_partition_name(name)
    )
    if len(table_names) != 57:
        raise SystemExit(f"expected 57 base tables, found {len(table_names)}: {table_names}")

    entries = []
    for name in table_names:
        tier = manifest_tier(name)
        entries.append(
            {
                "table_name": name,
                "classification_tier": tier,
                "retention_days": RETENTION_BY_TIER[tier],
                "pii_columns": pii_columns(name),
                "description": DESCRIPTIONS.get(name, f"{name} entity"),
            }
        )

    doc = {
        "manifest_version": 1,
        "woref": "WO-154",
        "source_migration": "shared-libs/pcis-schema/db/migration/V1__baseline_schema.sql",
        "table_count": len(entries),
        "entries": entries,
    }
    OUTPUT.write_text(_dump_yaml(doc), encoding="utf-8")
    print(f"Wrote {len(entries)} entries to {OUTPUT}")


if __name__ == "__main__":
    main()
