"""
Unit tests for docs/build_data_dictionary.py (WO-128, WO-150)

Run: python3 -m unittest discover -s docs/tests -v
"""

from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

DOCS = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(DOCS))

from build_data_dictionary import (  # noqa: E402
    BaselineColumnExtractor,
    DdlParser,
    ReconciliationEngine,
    build_dictionary,
    dump_yaml,
)

FIXTURES = DOCS / "test-fixtures"


class TestDdlParser(unittest.TestCase):
    def setUp(self):
        self.tables = DdlParser().parse(FIXTURES / "sample-ddl.md")

    def test_parses_table_name(self):
        self.assertIn("SAMPLE_CUSTOMER_T", self.tables)

    def test_parses_markdown_columns(self):
        cols = {c.name for c in self.tables["SAMPLE_CUSTOMER_T"].columns}
        self.assertEqual(
            cols,
            {"CUST_ID", "DOB", "TAX_ID", "EMAIL", "FIRST_NAME", "LAST_NAME"},
        )

    def test_parses_data_types(self):
        by_name = {c.name: c.data_type for c in self.tables["SAMPLE_CUSTOMER_T"].columns}
        self.assertIn("DATE", by_name["DOB"].upper())
        self.assertIn("VARCHAR", by_name["TAX_ID"].upper())

    def test_create_table_block_enriched(self):
        # CREATE TABLE present — parser should keep all six columns
        self.assertEqual(len(self.tables["SAMPLE_CUSTOMER_T"].columns), 6)

    def test_domain_extracted(self):
        self.assertEqual(self.tables["SAMPLE_CUSTOMER_T"].domain, "CUS")


class TestBaselineColumnExtractor(unittest.TestCase):
    def setUp(self):
        self.refs = BaselineColumnExtractor().extract(
            baseline_path=FIXTURES / "sample-baseline-entry.yaml",
            cobol_dir=None,
        )
        self.by_table = {}
        for r in self.refs:
            self.by_table.setdefault(r.table, []).append(r)

    def test_extracts_target_table(self):
        self.assertIn("SAMPLE_CUSTOMER_T", self.by_table)

    def test_extracts_matching_and_mismatched_hosts(self):
        hosts = {r.host_variable.upper() for r in self.by_table["SAMPLE_CUSTOMER_T"]}
        self.assertIn("HV-CUST-ID", hosts)
        self.assertIn("CUST_DOB", hosts)
        self.assertIn("CUST_SSN_TAXID", hosts)
        self.assertIn("CUST_GENDER", hosts)
        self.assertIn("CUST_EMAIL", hosts)

    def test_negative_no_unrelated_table(self):
        self.assertNotIn("POLICY_T", self.by_table)


class TestReconciliationEngine(unittest.TestCase):
    def setUp(self):
        ddl = DdlParser().parse(FIXTURES / "sample-ddl.md")
        refs = BaselineColumnExtractor().extract(
            baseline_path=FIXTURES / "sample-baseline-entry.yaml",
            cobol_dir=None,
        )
        # Fixture-scoped manual resolutions mirroring CUSTOMER_T G-06
        manual = {
            ("SAMPLE_CUSTOMER_T", "DOB"): {
                "cobol_host_variable": "CUST_DOB",
                "match_status": "mismatch",
                "resolution": "cust_dob",
                "resolution_rationale": "fixture: DOB vs CUST_DOB",
            },
            ("SAMPLE_CUSTOMER_T", "TAX_ID"): {
                "cobol_host_variable": "CUST_SSN_TAXID",
                "match_status": "mismatch",
                "resolution": "tax_id",
                "resolution_rationale": "fixture: TAX_ID vs CUST_SSN_TAXID",
            },
            ("SAMPLE_CUSTOMER_T", "EMAIL"): {
                "cobol_host_variable": "CUST_EMAIL",
                "match_status": "mismatch",
                "resolution": "email",
                "resolution_rationale": "fixture: EMAIL vs CUST_EMAIL",
            },
            ("SAMPLE_CUSTOMER_T", "FIRST_NAME"): {
                "cobol_host_variable": "",
                "match_status": "ddl-only",
                "resolution": "first_name",
                "resolution_rationale": "fixture ddl-only include",
            },
            ("SAMPLE_CUSTOMER_T", "LAST_NAME"): {
                "cobol_host_variable": "",
                "match_status": "ddl-only",
                "resolution": "last_name",
                "resolution_rationale": "fixture ddl-only include",
            },
            ("SAMPLE_CUSTOMER_T", "CUST_GENDER"): {
                "cobol_host_variable": "CUST_GENDER",
                "match_status": "cobol-only",
                "resolution": "cust_gender",
                "resolution_rationale": "fixture cobol-only add",
            },
        }
        self.doc = ReconciliationEngine(manual=manual).reconcile(ddl, refs)
        self.table = self.doc["tables"][0]
        self.by_ddl = {
            c["ddl_column_name"]: c
            for c in self.table["columns"]
            if c.get("ddl_column_name")
        }
        self.cobol_only = [
            c for c in self.table["columns"] if c["match_status"] == "cobol-only"
        ]

    def test_mismatch_dob(self):
        dob = self.by_ddl["DOB"]
        self.assertEqual(dob["match_status"], "mismatch")
        self.assertEqual(dob["cobol_host_variable"], "CUST_DOB")
        self.assertEqual(dob["resolution"], "cust_dob")

    def test_mismatch_tax_id(self):
        tax = self.by_ddl["TAX_ID"]
        self.assertEqual(tax["match_status"], "mismatch")
        self.assertEqual(tax["cobol_host_variable"], "CUST_SSN_TAXID")
        self.assertEqual(tax["resolution"], "tax_id")

    def test_ddl_only_names(self):
        self.assertEqual(self.by_ddl["FIRST_NAME"]["match_status"], "ddl-only")
        self.assertEqual(self.by_ddl["LAST_NAME"]["match_status"], "ddl-only")

    def test_cobol_only_gender(self):
        genders = [c for c in self.cobol_only if c["cobol_host_variable"] == "CUST_GENDER"]
        self.assertTrue(genders)
        self.assertEqual(genders[0]["resolution"], "cust_gender")

    def test_monetary_pg_type_mapping(self):
        engine = ReconciliationEngine(manual={})
        self.assertEqual(
            engine.target_pg_type("DECIMAL(9,2)", "S9(9)V99 COMP-3"),
            "NUMERIC(9,2)",
        )
        self.assertEqual(
            engine.target_pg_type("DECIMAL(11,2)", "S9(11)V99 COMP-3"),
            "NUMERIC(11,2)",
        )
        self.assertEqual(
            engine.target_pg_type("", "S9(9)V9(2) COMP-3"),
            "NUMERIC(9,2)",
        )

    def test_dump_yaml_roundtrip_keys(self):
        text = dump_yaml({"a": 1, "b": [{"x": "y"}]})
        self.assertIn("a: 1", text)
        self.assertIn("- x: y", text)


class TestFullBuildSmoke(unittest.TestCase):
    def test_build_against_repo_sources(self):
        ddl = DOCS.parent / "Property_Casualty_Insurance_System" / "PCIS_Database_Design.md"
        baseline = DOCS.parent / "baseline" / "cobol-baseline.yaml"
        cobol = DOCS.parent / "Property_Casualty_Insurance_System"
        if not ddl.is_file():
            self.skipTest("repo DDL missing")
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / "data-dictionary.yaml"
            doc = build_dictionary(ddl, baseline, cobol, out)
            self.assertGreaterEqual(doc["table_count"], 55)
            self.assertTrue(out.is_file())
            # WO-150: classification tiers and PII flags on every table/column
            for table in doc["tables"]:
                self.assertIn(
                    table.get("classification_tier"),
                    {"Public", "Internal", "Confidential", "Restricted"},
                )
                for col in table["columns"]:
                    self.assertIn("pii", col)
                    self.assertIn("mask_strategy", col)
                    self.assertIn("cobol_host_variable", col)
            # CUSTOMER_T critical resolutions present
            cust = next(t for t in doc["tables"] if t["table_name"] == "CUSTOMER_T")
            by_ddl = {c["ddl_column_name"]: c for c in cust["columns"] if c["ddl_column_name"]}
            self.assertEqual(by_ddl["DOB"]["resolution"], "cust_dob")
            self.assertEqual(by_ddl["TAX_ID"]["resolution"], "tax_id")
            self.assertEqual(by_ddl["EMAIL"]["resolution"], "email")
            self.assertEqual(by_ddl["FIRST_NAME"]["match_status"], "ddl-only")
            cobol_only = {
                c["cobol_host_variable"]
                for c in cust["columns"]
                if c["match_status"] == "cobol-only"
            }
            self.assertIn("CUST_GENDER", cobol_only)
            self.assertIn("CUST_MARITAL_ST", cobol_only)
            self.assertIn("CUST_CREDIT_SCORE", cobol_only)


if __name__ == "__main__":
    unittest.main()
