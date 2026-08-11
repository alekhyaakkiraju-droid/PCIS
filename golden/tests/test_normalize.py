"""Unit tests for golden artifact normalization (no IBM i required)."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from normalize import normalize_csv, normalize_display  # noqa: E402

FIXTURES = ROOT / "test-fixtures"


class TestNormalizeCsv(unittest.TestCase):
    def test_timestamp_and_surrogate_rewrite(self):
        raw = (FIXTURES / "raw_claim_payment.csv").read_text(encoding="utf-8")
        expected = (FIXTURES / "normalized_claim_payment.csv").read_text(encoding="utf-8")
        got = normalize_csv(raw, table_name="CLAIM_PAYMENT_T")
        self.assertEqual(got, expected)

    def test_null_handling_and_row_ordering(self):
        raw = (FIXTURES / "raw_audit_nulls.csv").read_text(encoding="utf-8")
        expected = (FIXTURES / "normalized_audit_nulls.csv").read_text(encoding="utf-8")
        got = normalize_csv(raw, table_name="AUDIT_LOG_T")
        self.assertEqual(got, expected)
        # NULL → empty string
        self.assertIn(",,BATCHBIL,", got)

    def test_numeric_precision_preserved(self):
        raw = "PAYMENT_ID,PAYMENT_AMT\n9,1000.01\n8,999.99\n"
        got = normalize_csv(raw, table_name="CLAIM_PAYMENT_T")
        self.assertIn("1000.01", got)
        self.assertIn("999.99", got)
        self.assertNotIn("1000.010", got)


class TestNormalizeDisplay(unittest.TestCase):
    def test_display_timestamps(self):
        raw = (FIXTURES / "raw_display.txt").read_text(encoding="utf-8")
        expected = (FIXTURES / "normalized_display.txt").read_text(encoding="utf-8")
        self.assertEqual(normalize_display(raw), expected)


if __name__ == "__main__":
    unittest.main()
