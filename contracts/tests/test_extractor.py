"""
Unit tests for contracts/extract_audit_contract.py (WO-007).

Run with:
  python3 -m unittest discover -s contracts/tests -v
"""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from extract_audit_contract import (  # noqa: E402
    CobolAuditParser,
    classify_shape,
    consolidate_program,
    parse_cobol_text,
)

FIXTURES = ROOT / "test-fixtures"
BATCH_FIXTURE = FIXTURES / "batch-audit-call.cbl"
INTERACTIVE_FIXTURE = FIXTURES / "interactive-audit-call.cbl"


class TestBatchFixtureExtractor(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        text = BATCH_FIXTURE.read_text(encoding="utf-8")
        cls.parser = parse_cobol_text(text, "FIXAUDB")
        cls.calls = cls.parser.extract_calls()

    def test_finds_two_call_sites(self):
        self.assertEqual(len(self.calls), 2)

    def test_batch_pic_widths(self):
        self.assertEqual(self.parser.pic_map.get("WS-AUD-ACTION"), "X(3)")
        self.assertEqual(self.parser.pic_map.get("WS-AUD-OBJECT"), "X(30)")

    def test_first_call_uses_leaf_parameters(self):
        first = self.calls[0]
        self.assertEqual(first["parameter_count"], 2)
        names = [p["working_storage_name"] for p in first["parameters"]]
        self.assertEqual(names, ["WS-AUD-ACTION", "WS-AUD-OBJECT"])
        self.assertEqual(first["parameters"][0]["PIC"], "X(3)")
        self.assertEqual(first["parameters"][1]["PIC"], "X(30)")

    def test_sample_value_from_move(self):
        first = self.calls[0]
        by_name = {p["working_storage_name"]: p for p in first["parameters"]}
        self.assertEqual(by_name["WS-AUD-ACTION"]["sample_value"], "ADD")
        self.assertEqual(by_name["WS-AUD-OBJECT"]["sample_value"], "CLAIM_T")

    def test_group_call_expands_to_leaves(self):
        second = self.calls[1]
        self.assertEqual(second["parameter_count"], 2)
        self.assertEqual(second["using_names"], ["WS-AUDIT-PARMS"])
        self.assertTrue(all(p.get("using_group") == "WS-AUDIT-PARMS" for p in second["parameters"]))

    def test_paragraph_and_line(self):
        first = self.calls[0]
        self.assertEqual(first["call_location"]["paragraph"], "0000-MAIN")
        self.assertIsInstance(first["call_location"]["approx_line"], int)
        self.assertGreater(first["call_location"]["approx_line"], 0)

    def test_semantic_meaning_present(self):
        for p in self.calls[0]["parameters"]:
            self.assertTrue(p["semantic_meaning"])

    def test_shape_class_batch_compact(self):
        self.assertEqual(classify_shape(self.calls[0]), "batch_compact_g04")


class TestInteractiveFixtureExtractor(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        text = INTERACTIVE_FIXTURE.read_text(encoding="utf-8")
        cls.parser = parse_cobol_text(text, "FIXAUDI")
        cls.calls = cls.parser.extract_calls()

    def test_finds_one_call_site(self):
        self.assertEqual(len(self.calls), 1)

    def test_interactive_pic_widths(self):
        self.assertEqual(self.parser.pic_map.get("WS-AUD-ACTION"), "X(1)")
        self.assertEqual(self.parser.pic_map.get("WS-AUD-OLD-VALUE"), "X(100)")
        self.assertEqual(self.parser.pic_map.get("WS-AUD-NEW-VALUE"), "X(100)")
        self.assertEqual(self.parser.pic_map.get("WS-AUD-KEY"), "X(40)")

    def test_parameter_count_and_order(self):
        call = self.calls[0]
        self.assertEqual(call["parameter_count"], 4)
        names = [p["working_storage_name"] for p in call["parameters"]]
        self.assertEqual(
            names,
            ["WS-AUD-ACTION", "WS-AUD-OLD-VALUE", "WS-AUD-NEW-VALUE", "WS-AUD-KEY"],
        )
        for idx, p in enumerate(call["parameters"], start=1):
            self.assertEqual(p["position"], idx)

    def test_documents_pic_width_not_content_width(self):
        """PIC X(100) must be recorded even when MOVE uses a short literal."""
        by_name = {p["working_storage_name"]: p for p in self.calls[0]["parameters"]}
        self.assertEqual(by_name["WS-AUD-OLD-VALUE"]["PIC"], "X(100)")
        self.assertEqual(by_name["WS-AUD-OLD-VALUE"]["sample_value"], "OLD-NAME-VALUE")

    def test_paragraph_name(self):
        self.assertEqual(
            self.calls[0]["call_location"]["paragraph"], "1000-UPDATE-CUSTOMER"
        )

    def test_shape_class_interactive(self):
        self.assertEqual(classify_shape(self.calls[0]), "interactive_g04")


class TestConsolidateProgram(unittest.TestCase):
    def test_multiple_sites_preserved(self):
        text = BATCH_FIXTURE.read_text(encoding="utf-8")
        parser = parse_cobol_text(text, "FIXAUDB")
        calls = parser.extract_calls()
        entry = consolidate_program(calls)
        self.assertEqual(entry["call_site_count"], 2)
        self.assertEqual(len(entry["all_call_sites"]), 2)
        self.assertIn("call_location", entry)
        self.assertEqual(entry["parameter_count"], len(entry["parameters"]))


class TestRealSourceSmoke(unittest.TestCase):
    """Light smoke against shipped COBOL when present."""

    def test_clm006b_compact_batch_shape(self):
        src = ROOT.parent / "Property_Casualty_Insurance_System" / "CLM006B.cbl"
        if not src.is_file():
            self.skipTest("CLM006B.cbl not present")
        parser = CobolAuditParser(src)
        calls = parser.extract_calls()
        self.assertGreaterEqual(len(calls), 1)
        self.assertEqual(parser.pic_map.get("WS-AUD-ACTION"), "X(3)")
        self.assertEqual(parser.pic_map.get("WS-AUD-OBJECT"), "X(30)")
        entry = consolidate_program(calls)
        self.assertEqual(entry["shape_class"], "batch_compact_g04")

    def test_aud002b_has_no_calls(self):
        src = ROOT.parent / "Property_Casualty_Insurance_System" / "AUD002B.cbl"
        if not src.is_file():
            self.skipTest("AUD002B.cbl not present")
        parser = CobolAuditParser(src)
        self.assertEqual(parser.extract_calls(), [])


if __name__ == "__main__":
    unittest.main()
