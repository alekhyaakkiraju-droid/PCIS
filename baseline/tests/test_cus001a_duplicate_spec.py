#!/usr/bin/env python3
"""Smoke checks that WO-239 artifacts match shipped CUS001A findings."""

from __future__ import annotations

import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CUS = ROOT / "Property_Casualty_Insurance_System" / "CUS001A.cbl"
SPEC = ROOT / "baseline" / "specs" / "cus001a_duplicate_taxid_behavior.md"
FIX = ROOT / "baseline" / "test-fixtures" / "cus001a_duplicate_scenarios.json"
EDGE = ROOT / "Property_Casualty_Insurance_System" / "PCIS_Modernization_Edge_Cases.md"


class TestCus001aDuplicateSpec(unittest.TestCase):
    def test_source_has_no_duplicate_paragraph(self) -> None:
        text = CUS.read_text(encoding="utf-8")
        self.assertNotIn("4000-CHECK-DUPLICATE-CUSTOMER", text)
        self.assertNotIn("WS-DUPLICATE-FOUND-SW", text)
        self.assertNotIn("CUST_SSN_TAXID", text)
        self.assertIn("5000-ADD-CUSTOMER", text)
        self.assertIn("TAX_ID", text)

    def test_spec_states_no_check_finding(self) -> None:
        text = SPEC.read_text(encoding="utf-8")
        self.assertIn("Neither hard block nor soft warning", text)
        self.assertIn("5000-ADD-CUSTOMER", text)
        self.assertIn("US-007", text)

    def test_fixture_scenarios(self) -> None:
        data = json.loads(FIX.read_text(encoding="utf-8"))
        self.assertEqual(data["verified_finding"], "no_duplicate_check")
        ids = {s["id"] for s in data["scenarios"]}
        self.assertIn("no_duplicate", ids)
        self.assertIn("duplicate_found_hard_block", ids)
        self.assertIn("duplicate_found_soft_warning", ids)

    def test_edge_case_a_p1_1_updated(self) -> None:
        text = EDGE.read_text(encoding="utf-8")
        self.assertIn("A-P1-1", text)
        self.assertIn("WO-239", text)
        self.assertIn("cus001a_duplicate_taxid_behavior.md", text)


if __name__ == "__main__":
    unittest.main()
