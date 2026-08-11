"""Unit tests for baseline/scripts/extract_crtobj_manifest.py (WO-240)."""

from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1] / "scripts"
sys.path.insert(0, str(SCRIPTS))

from extract_crtobj_manifest import extract_from_path, parse_crtobj  # noqa: E402

FIXTURES = Path(__file__).resolve().parents[1] / "test-fixtures" / "crtobj_parser"
REPO_ROOT = Path(__file__).resolve().parents[2]


class TestCrtobjParserFixture(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.sample = FIXTURES / "sample_crtobj_fragment.clle"
        cls.expected = json.loads(
            (FIXTURES / "expected_manifest.json").read_text(encoding="utf-8")
        )
        cls.manifest = extract_from_path(cls.sample)

    def test_object_names(self):
        names = sorted({o["name"] for o in self.manifest["objects"]})
        self.assertEqual(names, sorted(self.expected["object_names"]))

    def test_categories(self):
        by_name: dict[str, set[str]] = {}
        for o in self.manifest["objects"]:
            by_name.setdefault(o["name"], set()).add(o["category"])
        for name, cats in self.expected["categories_by_name"].items():
            self.assertEqual(by_name[name], set(cats), msg=name)

    def test_libraries(self):
        by_name: dict[str, set[str]] = {}
        for o in self.manifest["objects"]:
            by_name.setdefault(o["name"], set()).add(o["library"])
        for name, libs in self.expected["libraries_by_name"].items():
            self.assertEqual(by_name[name], set(libs), msg=name)

    def test_instest_typo_detected(self):
        self.assertTrue(self.manifest["instest_typo_detected"])
        self.assertGreaterEqual(len(self.manifest["instest_typos"]), 1)
        self.assertEqual(self.manifest["instest_typos"][0]["expected"], "INSTST")

    def test_submitted_jobs(self):
        jobs = [j["program"] for j in self.manifest["submitted_jobs"]]
        self.assertEqual(jobs, self.expected["submitted_jobs"])

    def test_continuation_lines_merged(self):
        modules = [
            o
            for o in self.manifest["objects"]
            if o["command"] == "CRTSQLCBLI" and o["name"] == "CLM006B"
        ]
        self.assertEqual(len(modules), 1)
        self.assertIn("OPTION(*EVENTF)", modules[0]["raw"])


class TestCrtobjRealFile(unittest.TestCase):
    def test_real_crtobj_parses(self):
        path = REPO_ROOT / "Property_Casualty_Insurance_System" / "PCIS_CRTOBJ.clle"
        manifest = extract_from_path(path)
        self.assertGreaterEqual(manifest["object_count"], 30)
        names = {o["name"] for o in manifest["objects"]}
        self.assertIn("CLM006B", names)
        self.assertIn("CLMFNLD1", names)
        self.assertIn("POLPOLP1", names)
        jobs = [j["program"] for j in manifest["submitted_jobs"]]
        self.assertEqual(jobs, ["JOBSCHD1", "JOBSCHD2", "JOBSCHD3"])


class TestCrtobjNegative(unittest.TestCase):
    def test_no_false_instest_on_clean_source(self):
        text = "CRTPGM PGM(FOO) MODULE(FOO)\n"
        result = parse_crtobj(text)
        self.assertFalse(result["instest_typo_detected"])
        self.assertEqual(result["objects"][0]["name"], "FOO")


if __name__ == "__main__":
    unittest.main()
