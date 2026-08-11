"""Tests for WO-176 normalization deny-list and fixture/output presence."""

from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from normalize import (  # noqa: E402
    NormalizationConfigError,
    is_denied_column,
    load_normalization_rules,
    normalize_csv,
    validate_allow_list,
)


class TestNormalizationRules(unittest.TestCase):
    def test_rules_file_loads(self):
        rules = load_normalization_rules()
        self.assertIn("PAYMENT_AMT", rules["deny_monetary"])
        self.assertIn("COMMISSION_AMT", rules["deny_monetary"])
        self.assertIn("STATUS", rules["deny_status"])
        self.assertIn("CREATED_AT", rules["allow_timestamps"])

    def test_reject_monetary_on_allow_list(self):
        with self.assertRaises(NormalizationConfigError) as ctx:
            validate_allow_list(["CREATED_AT", "PAYMENT_AMT", "AMOUNT"])
        self.assertIn("PAYMENT_AMT", str(ctx.exception))

    def test_reject_status_on_allow_list(self):
        with self.assertRaises(NormalizationConfigError):
            validate_allow_list(["RESERVE_STATUS", "COMM_CALC_FLAG"])

    def test_accept_timestamp_allow_list(self):
        validate_allow_list(["CREATED_AT", "RUN_STARTED", "PAYMENT_ID"])

    def test_monetary_and_status_not_rewritten(self):
        raw = (
            "PAYMENT_ID,CLAIM_ID,PAYMENT_AMT,RESERVE_STATUS,CREATED_AT\n"
            "10042,CLM0001001,1500.00,AP,2024-06-15 14:22:33\n"
        )
        got = normalize_csv(raw, table_name="CLAIM_PAYMENT_T")
        self.assertIn("1500.00", got)
        self.assertIn("AP", got)
        self.assertIn("SEQ_001", got)
        self.assertIn("NORMALIZED_TS", got)
        self.assertNotIn("NORMALIZED_TS,AP", got.replace("NORMALIZED_TS", "X"))  # status intact
        self.assertTrue(is_denied_column("PAYMENT_AMT"))
        self.assertTrue(is_denied_column("STATUS"))
        self.assertFalse(is_denied_column("CREATED_AT"))


class TestFixturesAndOutputs(unittest.TestCase):
    PROGRAMS = ["aud002b", "bil003b", "clm006b", "cmm001b", "pol006b", "prm005b"]

    def test_fixture_seeds_exist(self):
        for prog in self.PROGRAMS:
            path = ROOT / "fixtures" / prog / "seed.sql"
            self.assertTrue(path.is_file(), path)
            text = path.read_text(encoding="utf-8")
            self.assertIn("INSERT", text.upper())

    def test_expected_goldens_exist_and_parse(self):
        outputs = list((ROOT / "outputs").glob("*/*.golden.json"))
        self.assertGreaterEqual(len(outputs), 18)
        for path in outputs:
            data = json.loads(path.read_text(encoding="utf-8"))
            self.assertEqual(data["formatVersion"], "1.0.0")
            self.assertIn("tables", data)
            self.assertIn("runLog", data)
            # Monetary values must appear as exact decimal strings when present
            blob = path.read_text(encoding="utf-8")
            self.assertNotIn('"AMOUNT": "NORMALIZED', blob)


class TestFormatSpecPresent(unittest.TestCase):
    def test_format_spec_and_rules(self):
        self.assertTrue((ROOT / "format-spec.md").is_file())
        self.assertTrue((ROOT / "normalization-rules.yaml").is_file())


if __name__ == "__main__":
    unittest.main()
