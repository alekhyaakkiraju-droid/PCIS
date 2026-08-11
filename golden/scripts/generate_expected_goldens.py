#!/usr/bin/env python3
"""Generate committed expected JSON goldens under golden/outputs/ (WO-176).

These artifacts represent post-run mock captures after applying the known
scenario seed + mock batch writer effects. They are byte-stable and match
format-spec.md v1.0.0. Live IBM i captures replace them via capture tooling.
"""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "outputs"
REF = "2024-06-15"
FMT = "1.0.0"


def dump(program: str, scenario: str, payload: dict) -> None:
    dest = OUT / program.lower() / f"{scenario}.golden.json"
    dest.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(payload, indent=2, sort_keys=True) + "\n"
    dest.write_text(text, encoding="utf-8")
    print(f"wrote {dest.relative_to(ROOT)}")


def run_log(program: str, status: str, rows: int) -> dict:
    return {
        "programName": program,
        "rowsProcessed": rows,
        "runEnded": "NORMALIZED_TS",
        "runStarted": "NORMALIZED_TS",
        "status": status,
    }


def base(program: str, scenario: str, status: str, display: str, rows: int, tables: list) -> dict:
    return {
        "completionStatus": status,
        "displayOutput": display,
        "formatVersion": FMT,
        "program": program,
        "referenceDate": REF,
        "runLog": run_log(program, status, rows),
        "scenario": scenario,
        "tables": tables,
    }


def col(name: str, typ: str) -> dict:
    return {"name": name, "type": typ}


# --- BIL003B -----------------------------------------------------------------
def bil_installments(policy: str, amounts: list[str]) -> dict:
    rows = []
    for i, amt in enumerate(amounts, start=1):
        rows.append(
            {
                "AMOUNT": amt,
                "INSTALLMENT_ID": f"SEQ_{i:03d}",
                "INSTALLMENT_NO": i,
                "POLICY_ID": policy,
            }
        )
    return {
        "businessKeys": ["POLICY_ID", "INSTALLMENT_NO"],
        "columns": [
            col("INSTALLMENT_ID", "SURROGATE"),
            col("POLICY_ID", "STRING"),
            col("INSTALLMENT_NO", "INTEGER"),
            col("AMOUNT", "NUMERIC(11,2)"),
        ],
        "rows": rows,
        "tableName": "BILLING_INSTALLMENT_T",
    }


def bil_policy(policy: str, freq: str | None, premium: str, status: str = "A") -> dict:
    return {
        "businessKeys": ["POLICY_ID"],
        "columns": [
            col("POLICY_ID", "STRING"),
            col("BILLING_FREQ", "STATUS"),
            col("ANNUAL_PREMIUM", "NUMERIC(11,2)"),
            col("STATUS", "STATUS"),
        ],
        "rows": [
            {
                "ANNUAL_PREMIUM": premium,
                "BILLING_FREQ": freq,
                "POLICY_ID": policy,
                "STATUS": status,
            }
        ],
        "tableName": "POLICY_T",
    }


def bil_runlog_legacy(program: str = "BIL003B", status: str = "COMPLETED", rows: int = 0) -> dict:
    return {
        "businessKeys": ["PROGRAM_NAME", "STATUS"],
        "columns": [
            col("RUN_ID", "SURROGATE"),
            col("PROGRAM_NAME", "STRING"),
            col("STATUS", "STATUS"),
            col("ROWS_PROCESSED", "INTEGER"),
            col("RUN_STARTED", "TIMESTAMP"),
            col("RUN_ENDED", "TIMESTAMP"),
        ],
        "rows": [
            {
                "PROGRAM_NAME": program,
                "ROWS_PROCESSED": rows,
                "RUN_ENDED": "NORMALIZED_TS",
                "RUN_ID": "SEQ_001",
                "RUN_STARTED": "NORMALIZED_TS",
                "STATUS": status,
            }
        ],
        "tableName": "RPT_RUN_LOG_T",
    }


def bil_runlog(
    program: str = "BIL003B",
    status: str = "COMPLETED",
    rows: int = 0,
    *,
    rec_selected: int | None = None,
    rec_updated: int | None = None,
    rec_errors: int = 0,
) -> dict:
    selected = rec_selected if rec_selected is not None else rows
    updated = rec_updated if rec_updated is not None else rows
    return {
        "businessKeys": ["PROGRAM_NAME", "STATUS"],
        "columns": [
            col("RUN_ID", "SURROGATE"),
            col("PROGRAM_NAME", "STRING"),
            col("STATUS", "STATUS"),
            col("REC_SELECTED", "INTEGER"),
            col("REC_UPDATED", "INTEGER"),
            col("REC_ERRORS", "INTEGER"),
            col("ROWS_PROCESSED", "INTEGER"),
            col("RUN_STARTED", "TIMESTAMP"),
            col("RUN_ENDED", "TIMESTAMP"),
        ],
        "rows": [
            {
                "PROGRAM_NAME": program,
                "REC_ERRORS": rec_errors,
                "REC_SELECTED": selected,
                "REC_UPDATED": updated,
                "ROWS_PROCESSED": rows,
                "RUN_ENDED": "NORMALIZED_TS",
                "RUN_ID": "SEQ_001",
                "RUN_STARTED": "NORMALIZED_TS",
                "STATUS": status,
            }
        ],
        "tableName": "RPT_RUN_LOG_T",
    }


def gen_bil_wo179() -> None:
    """WO-179 named billing installment golden scenarios."""
    scenarios = {
        "even-division": (
            "POLBILEVN",
            "M",
            "1200.00",
            ["100.00"] * 12,
            12,
            "",
        ),
        "remainder-loss": (
            "POLBILREM",
            "T",
            "1000.00",
            ["333.33", "333.33", "333.33"],
            3,
            "PENNY_REMAINDER=0.01 LOST (333.33x3=999.99)\n",
        ),
        "monthly-frequency": (
            "POLBILMON",
            "M",
            "600.00",
            ["50.00"] * 12,
            12,
            "",
        ),
        "quarterly-frequency": (
            "POLBILQTR",
            "Q",
            "1000.00",
            ["250.00"] * 4,
            4,
            "",
        ),
        "semiannual-frequency": (
            "POLBILSEM",
            "S",
            "2400.00",
            ["1200.00", "1200.00"],
            2,
            "",
        ),
        "annual-frequency": (
            "POLBILANN",
            "A",
            "3600.00",
            ["3600.00"],
            1,
            "",
        ),
        "lead-window-boundary": (
            "POLBILLED",
            "M",
            "1200.00",
            ["100.00"],
            1,
            "LEAD_WINDOW=15 DAYS_OUT=15\n",
        ),
        "zero-candidate": (
            "POLBILZER",
            None,
            "1800.00",
            [],
            0,
            "ZERO_CANDIDATES\n",
        ),
    }
    for scen, (pol, freq, prem, amts, rows, extra) in scenarios.items():
        status = "COMPLETED"
        display = (
            f"BIL003B START REF={REF}\n"
            f"POLICY {pol} FREQ={freq} PREMIUM={prem}\n"
            f"{extra}"
            f"INSTALLMENTS={len(amts)}\n"
            f"BIL003B END STATUS={status}\n"
        )
        dump(
            "bil003b",
            scen,
            base(
                "BIL003B",
                scen,
                status,
                display,
                rows,
                [
                    bil_policy(pol, freq, prem),
                    bil_installments(pol, amts),
                    bil_runlog(rows=rows),
                ],
            ),
        )


def gen_bil() -> None:
    scenarios = {
        "scenario-01": ("POLBIL0001", "M", "1200.00", ["100.00"] * 12, 12),
        "scenario-02": (
            "POLBIL0002",
            "Q",
            "1000.01",
            ["250.01", "250.00", "250.00", "250.00"],
            4,
        ),
        "scenario-03": ("POLBIL0003", "S", "2400.00", ["1200.00", "1200.00"], 2),
        "scenario-04": ("POLBIL0004", "A", "3600.00", ["3600.00"], 1),
        "scenario-05": ("POLBIL0005", None, "1800.00", [], 0),
    }
    for scen, (pol, freq, prem, amts, rows) in scenarios.items():
        status = "COMPLETED" if amts else "COMPLETED"
        display = (
            f"BIL003B START REF={REF}\n"
            f"POLICY {pol} FREQ={freq} PREMIUM={prem}\n"
            f"INSTALLMENTS={len(amts)}\n"
            f"BIL003B END STATUS={status}\n"
        )
        dump(
            "bil003b",
            scen,
            base(
                "BIL003B",
                scen,
                status,
                display,
                rows,
                [
                    bil_policy(pol, freq, prem),
                    bil_installments(pol, amts),
                    bil_runlog_legacy(rows=rows),
                ],
            ),
        )


# --- CLM006B -----------------------------------------------------------------
def gen_clm() -> None:
    # scenario-01: pay AP reserve 1500.00, status → PD
    dump(
        "clm006b",
        "scenario-01",
        base(
            "CLM006B",
            "scenario-01",
            "COMPLETED",
            f"CLM006B START REF={REF}\nPAYMENT CLM0001001 1500.00\nCLM006B END\n",
            1,
            [
                {
                    "businessKeys": ["CLAIM_ID", "RESERVE_ID"],
                    "columns": [
                        col("CLAIM_ID", "STRING"),
                        col("RESERVE_ID", "STRING"),
                        col("RESERVE_STATUS", "STATUS"),
                        col("RESERVE_AMT", "NUMERIC(11,2)"),
                        col("AUTHORITY_LIMIT", "NUMERIC(11,2)"),
                    ],
                    "rows": [
                        {
                            "AUTHORITY_LIMIT": "5000.00",
                            "CLAIM_ID": "CLM0001001",
                            "RESERVE_AMT": "1500.00",
                            "RESERVE_ID": "RSV001",
                            "RESERVE_STATUS": "PD",
                        }
                    ],
                    "tableName": "CLAIM_RESERVE_T",
                },
                {
                    "businessKeys": ["CLAIM_ID", "PAYMENT_AMT"],
                    "columns": [
                        col("PAYMENT_ID", "SURROGATE"),
                        col("CLAIM_ID", "STRING"),
                        col("PAYMENT_AMT", "NUMERIC(11,2)"),
                        col("CREATED_AT", "TIMESTAMP"),
                    ],
                    "rows": [
                        {
                            "CLAIM_ID": "CLM0001001",
                            "CREATED_AT": "NORMALIZED_TS",
                            "PAYMENT_AMT": "1500.00",
                            "PAYMENT_ID": "SEQ_001",
                        }
                    ],
                    "tableName": "CLAIM_PAYMENT_T",
                },
                bil_runlog_legacy("CLM006B", "COMPLETED", 1),
            ],
        ),
    )
    # scenario-02: three AP reserves — payments for all (no SECCHK01 gate)
    dump(
        "clm006b",
        "scenario-02",
        base(
            "CLM006B",
            "scenario-02",
            "COMPLETED",
            f"CLM006B START REF={REF}\nPAYMENTS=3\nCLM006B END\n",
            3,
            [
                {
                    "businessKeys": ["CLAIM_ID", "RESERVE_ID"],
                    "columns": [
                        col("CLAIM_ID", "STRING"),
                        col("RESERVE_ID", "STRING"),
                        col("RESERVE_STATUS", "STATUS"),
                        col("RESERVE_AMT", "NUMERIC(11,2)"),
                        col("AUTHORITY_LIMIT", "NUMERIC(11,2)"),
                    ],
                    "rows": [
                        {
                            "AUTHORITY_LIMIT": "5000.00",
                            "CLAIM_ID": "CLM0002001",
                            "RESERVE_AMT": "2500.00",
                            "RESERVE_ID": "RSV001",
                            "RESERVE_STATUS": "PD",
                        },
                        {
                            "AUTHORITY_LIMIT": "10000.00",
                            "CLAIM_ID": "CLM0002002",
                            "RESERVE_AMT": "75000.00",
                            "RESERVE_ID": "RSV002",
                            "RESERVE_STATUS": "PD",
                        },
                        {
                            "AUTHORITY_LIMIT": "5000.00",
                            "CLAIM_ID": "CLM0002003",
                            "RESERVE_AMT": "500.00",
                            "RESERVE_ID": "RSV003",
                            "RESERVE_STATUS": "PD",
                        },
                    ],
                    "tableName": "CLAIM_RESERVE_T",
                },
                {
                    "businessKeys": ["CLAIM_ID", "PAYMENT_AMT"],
                    "columns": [
                        col("PAYMENT_ID", "SURROGATE"),
                        col("CLAIM_ID", "STRING"),
                        col("PAYMENT_AMT", "NUMERIC(11,2)"),
                        col("CREATED_AT", "TIMESTAMP"),
                    ],
                    "rows": [
                        {
                            "CLAIM_ID": "CLM0002001",
                            "CREATED_AT": "NORMALIZED_TS",
                            "PAYMENT_AMT": "2500.00",
                            "PAYMENT_ID": "SEQ_001",
                        },
                        {
                            "CLAIM_ID": "CLM0002003",
                            "CREATED_AT": "NORMALIZED_TS",
                            "PAYMENT_AMT": "500.00",
                            "PAYMENT_ID": "SEQ_002",
                        },
                        {
                            "CLAIM_ID": "CLM0002002",
                            "CREATED_AT": "NORMALIZED_TS",
                            "PAYMENT_AMT": "75000.00",
                            "PAYMENT_ID": "SEQ_003",
                        },
                    ],
                    "tableName": "CLAIM_PAYMENT_T",
                },
                bil_runlog_legacy("CLM006B", "COMPLETED", 3),
            ],
        ),
    )
    # scenario-03: zero payable
    dump(
        "clm006b",
        "scenario-03",
        base(
            "CLM006B",
            "scenario-03",
            "COMPLETED",
            f"CLM006B START REF={REF}\nPAYMENTS=0\nCLM006B END\n",
            0,
            [
                {
                    "businessKeys": ["CLAIM_ID", "RESERVE_ID"],
                    "columns": [
                        col("CLAIM_ID", "STRING"),
                        col("RESERVE_ID", "STRING"),
                        col("RESERVE_STATUS", "STATUS"),
                        col("RESERVE_AMT", "NUMERIC(11,2)"),
                        col("AUTHORITY_LIMIT", "NUMERIC(11,2)"),
                    ],
                    "rows": [
                        {
                            "AUTHORITY_LIMIT": "5000.00",
                            "CLAIM_ID": "CLM0003001",
                            "RESERVE_AMT": "1000.00",
                            "RESERVE_ID": "RSV001",
                            "RESERVE_STATUS": "PD",
                        },
                        {
                            "AUTHORITY_LIMIT": "5000.00",
                            "CLAIM_ID": "CLM0003002",
                            "RESERVE_AMT": "2000.00",
                            "RESERVE_ID": "RSV002",
                            "RESERVE_STATUS": "OP",
                        },
                    ],
                    "tableName": "CLAIM_RESERVE_T",
                },
                {
                    "businessKeys": ["CLAIM_ID", "PAYMENT_AMT"],
                    "columns": [
                        col("PAYMENT_ID", "SURROGATE"),
                        col("CLAIM_ID", "STRING"),
                        col("PAYMENT_AMT", "NUMERIC(11,2)"),
                        col("CREATED_AT", "TIMESTAMP"),
                    ],
                    "rows": [],
                    "tableName": "CLAIM_PAYMENT_T",
                },
                bil_runlog_legacy("CLM006B", "COMPLETED", 0),
            ],
        ),
    )


def gen_aud() -> None:
    dump(
        "aud002b",
        "scenario-01",
        base(
            "AUD002B",
            "scenario-01",
            "COMPLETED",
            f"AUD002B START REF={REF}\nARCHIVED=2\nAUD002B END\n",
            2,
            [
                {
                    "businessKeys": ["PROGRAM_NAME", "ACTION_CODE", "RECORD_KEY"],
                    "columns": [
                        col("LOG_ID", "SURROGATE"),
                        col("PROGRAM_NAME", "STRING"),
                        col("ACTION_CODE", "STATUS"),
                        col("TABLE_NAME", "STRING"),
                        col("RECORD_KEY", "STRING"),
                        col("USER_ID", "STRING"),
                        col("LOG_TIMESTAMP", "TIMESTAMP"),
                    ],
                    "rows": [],
                    "tableName": "AUDIT_LOG_T",
                },
                {
                    "businessKeys": ["PROGRAM_NAME", "ACTION_CODE", "RECORD_KEY"],
                    "columns": [
                        col("LOG_ID", "SURROGATE"),
                        col("PROGRAM_NAME", "STRING"),
                        col("ACTION_CODE", "STATUS"),
                        col("TABLE_NAME", "STRING"),
                        col("RECORD_KEY", "STRING"),
                        col("USER_ID", "STRING"),
                        col("LOG_TIMESTAMP", "TIMESTAMP"),
                        col("ARCHIVE_DATE", "TIMESTAMP"),
                    ],
                    "rows": [
                        {
                            "ACTION_CODE": "ADD",
                            "ARCHIVE_DATE": "NORMALIZED_TS",
                            "LOG_ID": "SEQ_001",
                            "LOG_TIMESTAMP": "NORMALIZED_TS",
                            "PROGRAM_NAME": "BIL003B",
                            "RECORD_KEY": "POLBIL0001",
                            "TABLE_NAME": "BILLING_INSTALLMENT_T",
                            "USER_ID": "BATCHBIL",
                        },
                        {
                            "ACTION_CODE": "PAY",
                            "ARCHIVE_DATE": "NORMALIZED_TS",
                            "LOG_ID": "SEQ_002",
                            "LOG_TIMESTAMP": "NORMALIZED_TS",
                            "PROGRAM_NAME": "CLM006B",
                            "RECORD_KEY": "CLM0001001",
                            "TABLE_NAME": "CLAIM_PAYMENT_T",
                            "USER_ID": "BATCHCLM",
                        },
                    ],
                    "tableName": "AUDIT_LOG_ARCHIVE_T",
                },
                bil_runlog_legacy("AUD002B", "COMPLETED", 2),
            ],
        ),
    )
    for scen, rows, status, note in [
        ("scenario-02", 1, "COMPLETED", "boundary"),
        ("scenario-03", 0, "COMPLETED", "none-eligible"),
        ("scenario-04", 0, "HALTED", "verify-mismatch"),
    ]:
        dump(
            "aud002b",
            scen,
            base(
                "AUD002B",
                scen,
                status,
                f"AUD002B START REF={REF}\n{note}\nAUD002B END STATUS={status}\n",
                rows,
                [
                    {
                        "businessKeys": ["PROGRAM_NAME", "ACTION_CODE", "RECORD_KEY"],
                        "columns": [
                            col("LOG_ID", "SURROGATE"),
                            col("PROGRAM_NAME", "STRING"),
                            col("ACTION_CODE", "STATUS"),
                            col("TABLE_NAME", "STRING"),
                            col("RECORD_KEY", "STRING"),
                            col("USER_ID", "STRING"),
                            col("LOG_TIMESTAMP", "TIMESTAMP"),
                        ],
                        "rows": [],
                        "tableName": "AUDIT_LOG_T",
                    },
                    {
                        "businessKeys": ["PROGRAM_NAME", "ACTION_CODE", "RECORD_KEY"],
                        "columns": [
                            col("LOG_ID", "SURROGATE"),
                            col("PROGRAM_NAME", "STRING"),
                            col("ACTION_CODE", "STATUS"),
                            col("TABLE_NAME", "STRING"),
                            col("RECORD_KEY", "STRING"),
                            col("USER_ID", "STRING"),
                            col("LOG_TIMESTAMP", "TIMESTAMP"),
                            col("ARCHIVE_DATE", "TIMESTAMP"),
                        ],
                        "rows": [],
                        "tableName": "AUDIT_LOG_ARCHIVE_T",
                    },
                    bil_runlog_legacy("AUD002B", status, rows),
                ],
            ),
        )


def gen_cmm() -> None:
    dump(
        "cmm001b",
        "scenario-01",
        base(
            "CMM001B",
            "scenario-01",
            "COMPLETED",
            f"CMM001B START REF={REF}\nCALC POLCMM0001 100.00\nCMM001B END\n",
            1,
            [
                {
                    "businessKeys": ["POLICY_ID", "AGENT_ID"],
                    "columns": [
                        col("POLICY_ID", "STRING"),
                        col("AGENT_ID", "STRING"),
                        col("PREMIUM_AMT", "NUMERIC(11,2)"),
                        col("COMM_CALC_FLAG", "STATUS"),
                        col("COMMISSION_AMT", "NUMERIC(9,2)"),
                    ],
                    "rows": [
                        {
                            "AGENT_ID": "AGT001",
                            "COMMISSION_AMT": "100.00",
                            "COMM_CALC_FLAG": "Y",
                            "POLICY_ID": "POLCMM0001",
                            "PREMIUM_AMT": "1000.00",
                        }
                    ],
                    "tableName": "COMMISSION_T",
                },
                bil_runlog_legacy("CMM001B", "COMPLETED", 1),
            ],
        ),
    )
    dump(
        "cmm001b",
        "scenario-02",
        base(
            "CMM001B",
            "scenario-02",
            "COMPLETED",
            f"CMM001B START REF={REF}\nSKIP POLCMM0002\nCMM001B END\n",
            0,
            [
                {
                    "businessKeys": ["POLICY_ID", "AGENT_ID"],
                    "columns": [
                        col("POLICY_ID", "STRING"),
                        col("AGENT_ID", "STRING"),
                        col("PREMIUM_AMT", "NUMERIC(11,2)"),
                        col("COMM_CALC_FLAG", "STATUS"),
                        col("COMMISSION_AMT", "NUMERIC(9,2)"),
                    ],
                    "rows": [
                        {
                            "AGENT_ID": "AGT002",
                            "COMMISSION_AMT": "200.00",
                            "COMM_CALC_FLAG": "Y",
                            "POLICY_ID": "POLCMM0002",
                            "PREMIUM_AMT": "2000.00",
                        }
                    ],
                    "tableName": "COMMISSION_T",
                },
                bil_runlog_legacy("CMM001B", "COMPLETED", 0),
            ],
        ),
    )


def gen_pol() -> None:
    dump(
        "pol006b",
        "scenario-01",
        base(
            "POL006B",
            "scenario-01",
            "COMPLETED",
            f"POL006B START REF={REF}\nRENEW POLREN0001\nPOL006B END\n",
            1,
            [
                {
                    "businessKeys": ["POLICY_ID"],
                    "columns": [
                        col("POLICY_ID", "STRING"),
                        col("STATUS", "STATUS"),
                        col("EXPIRY_DATE", "DATE"),
                    ],
                    "rows": [
                        {
                            "EXPIRY_DATE": "2025-07-01",
                            "POLICY_ID": "POLREN0001",
                            "STATUS": "A",
                        }
                    ],
                    "tableName": "POLICY_T",
                },
                {
                    "businessKeys": ["POLICY_ID"],
                    "columns": [
                        col("POLICY_ID", "STRING"),
                        col("DEDUCTIBLE_AMT", "NUMERIC(11,2)"),
                    ],
                    # Gap P-P8: deductible NOT copied on renewal — original row remains.
                    "rows": [{"DEDUCTIBLE_AMT": "500.00", "POLICY_ID": "POLREN0001"}],
                    "tableName": "DEDUCTIBLE_T",
                },
                bil_runlog_legacy("POL006B", "COMPLETED", 1),
            ],
        ),
    )
    dump(
        "pol006b",
        "scenario-02",
        base(
            "POL006B",
            "scenario-02",
            "COMPLETED",
            f"POL006B START REF={REF}\nSKIP OUTSIDE WINDOW\nPOL006B END\n",
            0,
            [
                {
                    "businessKeys": ["POLICY_ID"],
                    "columns": [
                        col("POLICY_ID", "STRING"),
                        col("STATUS", "STATUS"),
                        col("EXPIRY_DATE", "DATE"),
                    ],
                    "rows": [
                        {
                            "EXPIRY_DATE": "2024-12-01",
                            "POLICY_ID": "POLREN0002",
                            "STATUS": "A",
                        }
                    ],
                    "tableName": "POLICY_T",
                },
                {
                    "businessKeys": ["POLICY_ID"],
                    "columns": [
                        col("POLICY_ID", "STRING"),
                        col("DEDUCTIBLE_AMT", "NUMERIC(11,2)"),
                    ],
                    "rows": [{"DEDUCTIBLE_AMT": "1000.00", "POLICY_ID": "POLREN0002"}],
                    "tableName": "DEDUCTIBLE_T",
                },
                bil_runlog_legacy("POL006B", "COMPLETED", 0),
            ],
        ),
    )


def gen_prm() -> None:
    dump(
        "prm005b",
        "scenario-01",
        base(
            "PRM005B",
            "scenario-01",
            "COMPLETED",
            f"PRM005B START REF={REF}\nIN-GRACE POLPRM0001\nPRM005B END\n",
            1,
            [
                {
                    "businessKeys": ["POLICY_ID"],
                    "columns": [
                        col("POLICY_ID", "STRING"),
                        col("STATUS", "STATUS"),
                        col("PREMIUM_DUE_DATE", "DATE"),
                        col("GRACE_DAYS", "INTEGER"),
                    ],
                    "rows": [
                        {
                            "GRACE_DAYS": 10,
                            "POLICY_ID": "POLPRM0001",
                            "PREMIUM_DUE_DATE": "2024-06-10",
                            "STATUS": "A",
                        }
                    ],
                    "tableName": "POLICY_T",
                },
                bil_runlog_legacy("PRM005B", "COMPLETED", 1),
            ],
        ),
    )
    dump(
        "prm005b",
        "scenario-02",
        base(
            "PRM005B",
            "scenario-02",
            "COMPLETED",
            f"PRM005B START REF={REF}\nBOUNDARY POLPRM0002\nPRM005B END\n",
            1,
            [
                {
                    "businessKeys": ["POLICY_ID"],
                    "columns": [
                        col("POLICY_ID", "STRING"),
                        col("STATUS", "STATUS"),
                        col("PREMIUM_DUE_DATE", "DATE"),
                        col("GRACE_DAYS", "INTEGER"),
                    ],
                    "rows": [
                        {
                            "GRACE_DAYS": 10,
                            "POLICY_ID": "POLPRM0002",
                            "PREMIUM_DUE_DATE": "2024-06-05",
                            "STATUS": "D",
                        }
                    ],
                    "tableName": "POLICY_T",
                },
                bil_runlog_legacy("PRM005B", "COMPLETED", 1),
            ],
        ),
    )


def main() -> int:
    gen_bil()
    gen_bil_wo179()
    gen_clm()
    gen_aud()
    gen_cmm()
    gen_pol()
    gen_prm()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
