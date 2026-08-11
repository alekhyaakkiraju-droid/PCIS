"""
Data classification tiers and PII masking rules for PCIS (WO-150).

Assigns Public / Internal / Confidential / Restricted to tables and columns
per BR-16 and the masking module in pcis-observability-starter.
"""

from __future__ import annotations

from typing import Optional

VALID_TIERS = frozenset({"Public", "Internal", "Confidential", "Restricted"})
VALID_MASK_STRATEGIES = frozenset(
    {
        "NONE",
        "LAST_FOUR",
        "EMAIL_DOMAIN_ONLY",
        "PHONE_LAST_FOUR",
        "DATE_YEAR_ONLY",
        "FULL_REDACT",
    }
)

# Default tier when no explicit table rule matches.
DEFAULT_TABLE_TIER = "Internal"

TABLE_TIERS: dict[str, str] = {
    "CODE_TABLE_T": "Public",
    "ROLE_MENU_T": "Public",
    "CANCELLATION_REASON_T": "Public",
    "COVERAGE_TYPE_T": "Public",
    "CUSTOMER_T": "Restricted",
    "CUSTOMER_ADDRESS_T": "Restricted",
    "CUSTOMER_CONTACT_T": "Restricted",
    "SEC_USER_T": "Restricted",
    "AUDIT_LOG_T": "Confidential",
    "AUDIT_LOG_ARCHIVE_T": "Confidential",
    "CLAIM_NOTE_T": "Restricted",
    "UW_DECISION_T": "Restricted",
    "POLICY_HISTORY_T": "Confidential",
    "ENDORSEMENT_T": "Confidential",
    "UW_RULE_T": "Confidential",
    "COMMISSION_LEDGER_T": "Internal",
    "OUTBOX_EVENTS": "Internal",
}

DOMAIN_DEFAULT_TIERS: dict[str, str] = {
    "CUS": "Restricted",
    "SEC": "Restricted",
    "AUD": "Confidential",
    "CLM": "Internal",
    "POL": "Internal",
    "BIL": "Internal",
    "PAY": "Internal",
    "PRM": "Internal",
    "QTE": "Internal",
    "UND": "Internal",
    "AGT": "Internal",
    "REI": "Internal",
    "DOC": "Internal",
    "RPT": "Internal",
}

# Explicit column-level PII: (TABLE, DDL_COLUMN) -> (pii, mask_strategy)
COLUMN_PII: dict[tuple[str, str], tuple[bool, str]] = {
    ("CUSTOMER_T", "TAX_ID"): (True, "LAST_FOUR"),
    ("CUSTOMER_T", "DOB"): (True, "DATE_YEAR_ONLY"),
    ("CUSTOMER_T", "EMAIL"): (True, "EMAIL_DOMAIN_ONLY"),
    ("CUSTOMER_T", "PHONE"): (True, "PHONE_LAST_FOUR"),
    ("CUSTOMER_T", "FIRST_NAME"): (True, "FULL_REDACT"),
    ("CUSTOMER_T", "LAST_NAME"): (True, "FULL_REDACT"),
    ("CUSTOMER_ADDRESS_T", "ADDR_LINE1"): (True, "FULL_REDACT"),
    ("CUSTOMER_ADDRESS_T", "ADDR_LINE2"): (True, "FULL_REDACT"),
    ("CUSTOMER_ADDRESS_T", "CITY"): (True, "FULL_REDACT"),
    ("CUSTOMER_ADDRESS_T", "STATE"): (False, "NONE"),
    ("CUSTOMER_ADDRESS_T", "POSTAL_CODE"): (True, "LAST_FOUR"),
    ("CUSTOMER_CONTACT_T", "CONTACT_PHONE"): (True, "PHONE_LAST_FOUR"),
    ("CUSTOMER_CONTACT_T", "CONTACT_EMAIL"): (True, "EMAIL_DOMAIN_ONLY"),
    ("CUSTOMER_CONTACT_T", "CONTACT_VALUE"): (True, "FULL_REDACT"),
    ("AGENT_T", "AGT_NAME"): (True, "FULL_REDACT"),
    ("AGENT_LICENSE_T", "LICENSE_NBR"): (True, "LAST_FOUR"),
    ("POLICY_VEHICLE_T", "VIN"): (True, "FULL_REDACT"),
    ("POLICY_PROPERTY_T", "PROP_ADDR1"): (True, "FULL_REDACT"),
    ("POLICY_PROPERTY_T", "PROP_ADDR2"): (True, "FULL_REDACT"),
    ("POLICY_PROPERTY_T", "PROP_CITY"): (True, "FULL_REDACT"),
    ("POLICY_PROPERTY_T", "PROP_POSTAL"): (True, "LAST_FOUR"),
    ("CLAIM_PAYMENT_T", "PAYEE_NAME"): (True, "FULL_REDACT"),
    ("BILLING_NOTICE_T", "DELIVERY_EMAIL"): (True, "EMAIL_DOMAIN_ONLY"),
    ("CLAIM_NOTE_T", "NOTE_TEXT"): (True, "FULL_REDACT"),
    ("UW_DECISION_T", "DECISION_REASON"): (True, "FULL_REDACT"),
    ("POLICY_HISTORY_T", "EVENT_DESC"): (True, "FULL_REDACT"),
    ("ENDORSEMENT_T", "ENDT_DESC"): (True, "FULL_REDACT"),
    ("UW_RULE_T", "RULE_EXPRESSION"): (True, "FULL_REDACT"),
    ("SEC_USER_T", "USER_NAME"): (True, "FULL_REDACT"),
    ("SEC_USER_T", "PASSWORD_HASH"): (True, "FULL_REDACT"),
    ("AUDIT_LOG_T", "OLD_VALUE"): (True, "FULL_REDACT"),
    ("AUDIT_LOG_T", "NEW_VALUE"): (True, "FULL_REDACT"),
    ("AUDIT_LOG_ARCHIVE_T", "OLD_VALUE"): (True, "FULL_REDACT"),
    ("AUDIT_LOG_ARCHIVE_T", "NEW_VALUE"): (True, "FULL_REDACT"),
}

# Columns whose resolution name differs from ddl_column_name (G-06 drift).
RESOLUTION_PII: dict[tuple[str, str], tuple[bool, str]] = {
    ("CUSTOMER_T", "cust_dob"): (True, "DATE_YEAR_ONLY"),
    ("CUSTOMER_T", "tax_id"): (True, "LAST_FOUR"),
    ("CUSTOMER_T", "email"): (True, "EMAIL_DOMAIN_ONLY"),
    ("BILLING_SCHEDULE_T", "due_amt"): (False, "NONE"),
    ("BILLING_SCHEDULE_T", "paid_amt"): (False, "NONE"),
    ("BILLING_SCHEDULE_T", "bill_status"): (False, "NONE"),
}


def table_tier(table_name: str, domain: str = "") -> str:
    up = table_name.upper()
    if up in TABLE_TIERS:
        return TABLE_TIERS[up]
    if domain and domain.upper() in DOMAIN_DEFAULT_TIERS:
        return DOMAIN_DEFAULT_TIERS[domain.upper()]
    return DEFAULT_TABLE_TIER


def column_classification(
    table_name: str,
    ddl_column: str,
    resolution: str,
    table_tier_value: str,
) -> dict[str, object]:
    key = (table_name.upper(), (ddl_column or "").upper())
    res_key = (table_name.upper(), (resolution or "").lower())
    if key in COLUMN_PII:
        pii, mask = COLUMN_PII[key]
    elif res_key in RESOLUTION_PII:
        pii, mask = RESOLUTION_PII[res_key]
    elif table_tier_value == "Restricted":
        pii, mask = True, "FULL_REDACT"
    else:
        pii, mask = False, "NONE"

    if table_tier_value == "Restricted" and pii and mask == "NONE":
        mask = "FULL_REDACT"

    return {"pii": pii, "mask_strategy": mask}


def validate_tier(tier: Optional[str]) -> bool:
    return tier in VALID_TIERS


def validate_mask_for_tier(tier: str, pii: bool, mask_strategy: str) -> bool:
    if mask_strategy not in VALID_MASK_STRATEGIES:
        return False
    if tier == "Restricted" and pii and mask_strategy == "NONE":
        return False
    return True
