"""Unit tests for baseline/scripts/extract_repo_manifest.py (WO-240)."""

from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1] / "scripts"
sys.path.insert(0, str(SCRIPTS))

from extract_repo_manifest import categorize, walk_repo  # noqa: E402
from generate_delta_report import (  # noqa: E402
    KNOWN_MISSING_SERVICE_PROGRAMS,
    build_delta,
    _parse_jobschd_drivers,
)

REPO_ROOT = Path(__file__).resolve().parents[2]
SOURCE = REPO_ROOT / "Property_Casualty_Insurance_System"


class TestCategorize(unittest.TestCase):
    def test_extensions(self):
        self.assertEqual(categorize(Path("X.cbl")), "cobol")
        self.assertEqual(categorize(Path("X.dspf")), "display-file")
        self.assertEqual(categorize(Path("X.clle")), "clle")
        self.assertEqual(categorize(Path("X.md")), "markdown")
        self.assertEqual(categorize(Path("X.unknown")), "other")


class TestWalkRepo(unittest.TestCase):
    def test_real_tree_counts(self):
        manifest = walk_repo(SOURCE)
        self.assertGreaterEqual(manifest["total_file_count"], 40)
        self.assertIn("cobol", manifest["counts_by_type"])
        self.assertIn("display-file", manifest["counts_by_type"])
        self.assertEqual(manifest["counts_by_type"]["cobol"], 8)
        self.assertEqual(manifest["counts_by_type"]["display-file"], 22)
        self.assertIn("CLM006B", manifest["objects"])
        self.assertEqual(manifest["objects"]["CLM006B"]["type"], "cobol")

    def test_temp_tree(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "AAA.cbl").write_text("IDENTIFICATION DIVISION.\n", encoding="utf-8")
            (root / "BBB.dspf").write_text("A  R RECORD\n", encoding="utf-8")
            (root / "notes.md").write_text("# hi\n", encoding="utf-8")
            manifest = walk_repo(root)
            self.assertEqual(manifest["total_file_count"], 3)
            self.assertEqual(manifest["counts_by_type"]["cobol"], 1)
            self.assertEqual(manifest["counts_by_type"]["display-file"], 1)
            self.assertEqual(manifest["counts_by_type"]["markdown"], 1)


class TestDeltaIntegration(unittest.TestCase):
    def test_known_missing_and_jobschd(self):
        from extract_crtobj_manifest import extract_from_path

        crtobj = extract_from_path(SOURCE / "PCIS_CRTOBJ.clle")
        repo = walk_repo(SOURCE)
        jobschd = _parse_jobschd_drivers(SOURCE / "JOBSCHD_NEW_DRIVERS.clle")
        delta = build_delta(crtobj, repo, jobschd)

        names = {e["name"] for e in delta["known_missing_service_programs"]}
        self.assertEqual(names, set(KNOWN_MISSING_SERVICE_PROGRAMS))
        self.assertTrue(delta["jobschd_reconciliation"]["new_drivers_complete"])
        self.assertEqual(
            delta["jobschd_reconciliation"]["runtime_only_no_source"],
            ["JOBSCHD1", "JOBSCHD2", "JOBSCHD3"],
        )
        printer_names = {e["name"] for e in delta["printer_file_gaps"]}
        self.assertTrue({"POLPOLP1", "CLMPAYP1", "RPT001P1", "RPT006P1"} <= printer_names)
        self.assertTrue(any(r["object"] == "CLM006B" for r in delta["dspobjd_checklist"]))


if __name__ == "__main__":
    unittest.main()
