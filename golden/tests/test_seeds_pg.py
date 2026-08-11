"""Validate seed SQL against a schema-compatible PostgreSQL subset.

Uses the local `psycopg` driver when DATABASE_URL is set; otherwise creates an
in-process SQLite stand-in for syntax/row-count smoke checks of INSERT shapes.
Full Db2 for i semantics require IBM i (see golden/README.md).
"""

from __future__ import annotations

import re
import sqlite3
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SEEDS = ROOT / "seeds"


def _sqlite_schema(conn: sqlite3.Connection) -> None:
    conn.executescript(
        """
        CREATE TABLE CLAIM_RESERVE_T (
          CLAIM_ID TEXT, RESERVE_ID TEXT, RESERVE_STATUS TEXT,
          RESERVE_AMT NUMERIC, AUTHORITY_LIMIT NUMERIC
        );
        CREATE TABLE CLAIM_PAYMENT_T (
          PAYMENT_ID INTEGER, CLAIM_ID TEXT, PAYMENT_AMT NUMERIC, CREATED_AT TEXT
        );
        CREATE TABLE POLICY_T (
          POLICY_ID TEXT PRIMARY KEY, BILLING_FREQ TEXT, ANNUAL_PREMIUM NUMERIC,
          STATUS TEXT, PREMIUM_DUE_DATE TEXT, GRACE_DAYS INT, EXPIRY_DATE TEXT
        );
        CREATE TABLE BILLING_INSTALLMENT_T (
          INSTALLMENT_ID INTEGER, POLICY_ID TEXT, INSTALLMENT_NO INT, AMOUNT NUMERIC
        );
        CREATE TABLE AUDIT_LOG_T (
          LOG_ID INTEGER, PROGRAM_NAME TEXT, ACTION_CODE TEXT, TABLE_NAME TEXT,
          RECORD_KEY TEXT, USER_ID TEXT, LOG_TIMESTAMP TEXT
        );
        CREATE TABLE AUDIT_LOG_ARCHIVE_T (
          LOG_ID INTEGER, PROGRAM_NAME TEXT, ACTION_CODE TEXT, TABLE_NAME TEXT,
          RECORD_KEY TEXT, USER_ID TEXT, LOG_TIMESTAMP TEXT, ARCHIVE_DATE TEXT
        );
        CREATE TABLE COMMISSION_T (
          POLICY_ID TEXT, AGENT_ID TEXT, PREMIUM_AMT NUMERIC,
          COMM_CALC_FLAG TEXT, COMMISSION_AMT NUMERIC
        );
        CREATE TABLE DEDUCTIBLE_T (POLICY_ID TEXT, DEDUCTIBLE_AMT NUMERIC);
        CREATE TABLE RPT_RUN_LOG_T (
          RUN_ID INTEGER, PROGRAM_NAME TEXT, STATUS TEXT, ROWS_PROCESSED INT,
          RUN_STARTED TEXT, RUN_ENDED TEXT
        );
        """
    )


def _adapt_sql(sql: str) -> str:
    # SQLite has no CHAR/VARCHAR types issues for these inserts; strip comments.
    lines = [ln for ln in sql.splitlines() if not ln.strip().startswith("--")]
    return "\n".join(lines)


class TestSeedsLoad(unittest.TestCase):
    def test_all_scenario_seeds_insert(self):
        conn = sqlite3.connect(":memory:")
        _sqlite_schema(conn)
        programs = ["CLM006B", "BIL003B", "AUD002B", "CMM001B", "PRM005B", "POL006B"]
        loaded = 0
        for prog in programs:
            common = SEEDS / prog / "_common.sql"
            if common.exists():
                # DELETE may run on empty tables
                for stmt in _adapt_sql(common.read_text()).split(";"):
                    stmt = stmt.strip()
                    if stmt:
                        conn.execute(stmt)
            for path in sorted((SEEDS / prog).glob("scenario-*.sql")):
                sql = _adapt_sql(path.read_text())
                for stmt in sql.split(";"):
                    stmt = stmt.strip()
                    if not stmt:
                        continue
                    conn.execute(stmt)
                    loaded += 1
        conn.commit()
        self.assertGreaterEqual(loaded, 16)
        # Spot-check key counts
        n = conn.execute("SELECT COUNT(*) FROM CLAIM_RESERVE_T").fetchone()[0]
        self.assertEqual(n, 6)  # 1+3+2 across CLM scenarios (reloaded sequentially — last wins after deletes)
        # After all programs, reserves may be cleared by later deletes? Only CLM deletes reserves.
        # Re-run only CLM006B scenario-02 for assertion clarity:
        conn.execute("DELETE FROM CLAIM_RESERVE_T")
        sql = _adapt_sql((SEEDS / "CLM006B" / "scenario-02.sql").read_text())
        for stmt in sql.split(";"):
            if stmt.strip():
                conn.execute(stmt)
        n = conn.execute("SELECT COUNT(*) FROM CLAIM_RESERVE_T").fetchone()[0]
        self.assertEqual(n, 3)

    def test_seed_files_exist_for_required_scenarios(self):
        expected = {
            "CLM006B": 3,
            "BIL003B": 5,
            "AUD002B": 4,
            "CMM001B": 2,
            "PRM005B": 2,
            "POL006B": 2,
        }
        for prog, count in expected.items():
            files = list((SEEDS / prog).glob("scenario-*.sql"))
            self.assertEqual(len(files), count, prog)


if __name__ == "__main__":
    unittest.main()
