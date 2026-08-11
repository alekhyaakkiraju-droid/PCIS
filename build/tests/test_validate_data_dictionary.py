"""Unit tests for build/scripts/validate-data-dictionary.py (WO-150)."""

from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DOCS = REPO_ROOT / "docs"
sys.path.insert(0, str(DOCS))

from build_data_dictionary import build_dictionary  # noqa: E402
from flyway_schema_parser import parse_flyway_schema  # noqa: E402


def _load_validator():
    path = REPO_ROOT / "build/scripts/validate-data-dictionary.py"
    spec = importlib.util.spec_from_file_location("validate_data_dictionary", path)
    mod = importlib.util.module_from_spec(spec)
    assert spec and spec.loader
    spec.loader.exec_module(mod)
    return mod


vdd = _load_validator()


class TestFlywayParser(unittest.TestCase):
    def test_parses_v1_baseline(self):
        path = REPO_ROOT / "shared-libs/pcis-schema/db/migration/V1__baseline_schema.sql"
        if not path.is_file():
            self.skipTest("V1 baseline missing")
        tables = parse_flyway_schema(path)
        self.assertIn("BILLING_SCHEDULE_T", tables)
        cols = {c.name for c in tables["BILLING_SCHEDULE_T"].columns}
        self.assertIn("AMT_DUE", cols)
        self.assertIn("COMM_CALC_FLAG", cols)
        self.assertGreaterEqual(len(tables), 55)


class TestValidateDataDictionary(unittest.TestCase):
    def test_generated_dictionary_passes_flyway_gate(self):
        ddl = REPO_ROOT / "Property_Casualty_Insurance_System/PCIS_Database_Design.md"
        baseline = REPO_ROOT / "baseline/cobol-baseline.yaml"
        cobol = REPO_ROOT / "Property_Casualty_Insurance_System"
        flyway = REPO_ROOT / "shared-libs/pcis-schema/db/migration/V1__baseline_schema.sql"
        if not ddl.is_file() or not flyway.is_file():
            self.skipTest("repo sources missing")
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / "data-dictionary.yaml"
            doc = build_dictionary(ddl, baseline, cobol, out, flyway)
            self.assertGreaterEqual(doc["table_count"], 55)
            errors, _ = vdd.validate(out, flyway)
            self.assertEqual(errors, [], errors)

    def test_missing_tier_fails(self):
        bad = {
            "table_count": 1,
            "tables": [
                {
                    "table_name": "CUSTOMER_T",
                    "domain": "CUS",
                    "columns": [
                        {
                            "ddl_column_name": "CUST_ID",
                            "cobol_host_variable": "",
                            "resolution": "cust_id",
                            "pii": False,
                            "mask_strategy": "NONE",
                        }
                    ],
                }
            ],
        }
        errors = vdd.validate_dictionary_structure(bad)
        self.assertTrue(any("classification_tier" in e for e in errors))

    def test_billing_due_amt_resolution_maps_to_flyway(self):
        ddl = REPO_ROOT / "Property_Casualty_Insurance_System/PCIS_Database_Design.md"
        baseline = REPO_ROOT / "baseline/cobol-baseline.yaml"
        cobol = REPO_ROOT / "Property_Casualty_Insurance_System"
        flyway = REPO_ROOT / "shared-libs/pcis-schema/db/migration/V1__baseline_schema.sql"
        if not ddl.is_file():
            self.skipTest("repo sources missing")
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / "data-dictionary.yaml"
            doc = build_dictionary(ddl, baseline, cobol, out, flyway)
            bil = next(t for t in doc["tables"] if t["table_name"] == "BILLING_SCHEDULE_T")
            due = next(c for c in bil["columns"] if c.get("ddl_column_name") == "AMT_DUE")
            self.assertEqual(due["cobol_host_variable"], "DUE_AMT")
            self.assertIn("G-06", due.get("drift_note", "") + due.get("resolution_rationale", ""))


if __name__ == "__main__":
    unittest.main()
