"""
Unit tests for baseline/extract_baseline.py (WO-002)

Run with:  python3 -m pytest baseline/tests/ -v
       or: python3 baseline/tests/test_extractor.py
"""

from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from extract_baseline import CobolSourceParser, generate_baseline
from validate_baseline import validate

FIXTURES = Path(__file__).parent.parent / "test-fixtures"


class TestSqlExtraction(unittest.TestCase):
    def setUp(self):
        self.result = CobolSourceParser(FIXTURES / "sample-sql-block.cbl").parse()

    def test_extracts_select_cursor(self):
        types = {s["statement_type"] for s in self.result["sql_statements"]}
        self.assertIn("SELECT", types)

    def test_extracts_insert_with_host_vars(self):
        inserts = [s for s in self.result["sql_statements"] if s["statement_type"] == "INSERT"]
        self.assertTrue(inserts)
        self.assertEqual(inserts[0]["target_table"], "PAYMENT_T")
        self.assertIn("HV-CLAIM-ID", inserts[0]["host_variables"])

    def test_fetch_strategy_chunk(self):
        selects = [
            s
            for s in self.result["sql_statements"]
            if s["statement_type"] == "SELECT" and s.get("cursor_name")
        ]
        self.assertTrue(any("chunk" in (s.get("fetch_strategy") or "") for s in selects))

    def test_negative_no_delete(self):
        deletes = [s for s in self.result["sql_statements"] if s["statement_type"] == "DELETE"]
        self.assertEqual(deletes, [])


class TestCallExtraction(unittest.TestCase):
    def test_batch_audit_call_and_pics(self):
        result = CobolSourceParser(FIXTURES / "sample-audit-call-batch.cbl").parse()
        callees = [c["callee"] for c in result["calls"]]
        self.assertIn("AUDLOG01", callees)
        audit = result["audit_call_parameters"]
        self.assertTrue(audit["called"])
        self.assertEqual(audit["action_code_pic"], "X(3)")
        self.assertEqual(audit["old_value_pic"], "X(30)")

    def test_interactive_audit_shape(self):
        result = CobolSourceParser(FIXTURES / "sample-audit-call-interactive.cbl").parse()
        audit = result["audit_call_parameters"]
        self.assertTrue(audit["called"])
        self.assertEqual(audit["action_code_pic"], "X(1)")
        self.assertEqual(audit["old_value_pic"], "X(100)")
        self.assertEqual(audit["key_field_pic"], "X(40)")

    def test_negative_missing_secchk_contradiction(self):
        result = CobolSourceParser(FIXTURES / "sample-audit-call-batch.cbl").parse()
        claims = [c["prologue_claim"] for c in result["prologue_contradictions"]]
        self.assertTrue(any("SECCHK01" in c for c in claims))

    def test_negative_no_call_when_absent(self):
        result = CobolSourceParser(FIXTURES / "sample-working-storage.cbl").parse()
        self.assertEqual(result["calls"], [])
        self.assertFalse(result["audit_call_parameters"]["called"])


class TestWorkingStorageExtraction(unittest.TestCase):
    def setUp(self):
        self.result = CobolSourceParser(FIXTURES / "sample-working-storage.cbl").parse()
        self.by_name = {t["name"]: t for t in self.result["working_storage_tunables"]}

    def test_retention_days(self):
        self.assertIn("WS-RETENTION-DAYS", self.by_name)
        self.assertEqual(self.by_name["WS-RETENTION-DAYS"]["value"], "365")

    def test_chunk_size(self):
        self.assertEqual(self.by_name["WS-CHUNK-SIZE"]["value"], "5000")

    def test_rei_cession_threshold(self):
        self.assertIn("WS-REI-CESSION-THRESHOLD", self.by_name)
        self.assertIn("100000", self.by_name["WS-REI-CESSION-THRESHOLD"]["value"])

    def test_batch_actor_literal(self):
        self.assertIn("WS-RUN-USER", self.by_name)
        self.assertIn("BATCHAUD", self.by_name["WS-RUN-USER"]["value"])

    def test_negative_no_host_without_value(self):
        # HV-* without VALUE should not flood tunables from this fixture
        hv = [t for t in self.result["working_storage_tunables"] if t["name"].startswith("HV-")]
        self.assertEqual(hv, [])


class TestErrorPathExtraction(unittest.TestCase):
    def test_rollback_path(self):
        result = CobolSourceParser(FIXTURES / "sample-error-rollback.cbl").parse()
        outcomes = [e["outcome"] for e in result["error_handling_paths"]]
        self.assertIn("ROLLBACK", outcomes)
        self.assertTrue(result["commit_scope"]["rollback_issued"])

    def test_display_only_audit_failure(self):
        result = CobolSourceParser(FIXTURES / "sample-audit-call-batch.cbl").parse()
        outcomes = [e["outcome"] for e in result["error_handling_paths"]]
        self.assertIn("DISPLAY-only", outcomes)


class TestGenerateAndValidate(unittest.TestCase):
    def test_integration_against_repo_sources(self):
        repo = Path(__file__).resolve().parents[2]
        source = repo / "Property_Casualty_Insurance_System"
        if not source.is_dir() or not list(source.glob("*.cbl")):
            self.skipTest("COBOL sources not present")
        with tempfile.TemporaryDirectory() as td:
            out = Path(td) / "cobol-baseline.yaml"
            doc = generate_baseline(source, out)
            self.assertEqual(doc["program_count"], 8)
            errors = validate(out, repo / "manifest" / "pcis-manifest.yaml")
            self.assertEqual(errors, [], msg="\n".join(errors))


if __name__ == "__main__":
    unittest.main()
