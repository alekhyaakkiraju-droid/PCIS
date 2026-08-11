"""Unit tests for build_config.py (WO-005)."""

from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1] / "scripts"
sys.path.insert(0, str(SCRIPTS))

from build_config import load_build_yaml, select_environment  # noqa: E402

FIXTURES = Path(__file__).resolve().parents[1] / "test-fixtures"
REPO_BUILD_YAML = Path(__file__).resolve().parents[1] / "build.yaml"


class TestBuildYamlLoader(unittest.TestCase):
    def test_loads_fixture_environments(self):
        cfg = load_build_yaml(FIXTURES / "sample-build.yaml")
        self.assertIn("dev", cfg["environments"])
        self.assertEqual(cfg["environments"]["dev"]["pgm_lib"], "INSDEV")
        self.assertEqual(cfg["environments"]["prd"]["data_lib"], "INSPRDDTA")

    def test_compiler_config(self):
        cfg = load_build_yaml(FIXTURES / "sample-build.yaml")
        self.assertEqual(cfg["compiler"]["min_supported_release"], "7.1")
        self.assertIn("6.3", cfg["compiler"]["out_of_support_releases"])
        self.assertEqual(cfg["compiler"]["out_of_support_after"]["6.3"], "2025-09-30")

    def test_select_environment(self):
        cfg = load_build_yaml(FIXTURES / "sample-build.yaml")
        env = select_environment(cfg, "tst")
        self.assertEqual(env["pgm_lib"], "INSTST")

    def test_missing_environment_lists_available(self):
        cfg = load_build_yaml(FIXTURES / "sample-build.yaml")
        with self.assertRaises(KeyError) as ctx:
            select_environment(cfg, "qa")
        self.assertIn("dev", str(ctx.exception))
        self.assertIn("tst", str(ctx.exception))

    def test_invalid_yaml_no_environments(self):
        with tempfile.NamedTemporaryFile("w", suffix=".yaml", delete=False) as fh:
            fh.write("compiler:\n  min_supported_release: \"7.1\"\n")
            path = fh.name
        with self.assertRaises(ValueError):
            load_build_yaml(path)

    def test_repo_build_yaml_loads(self):
        cfg = load_build_yaml(REPO_BUILD_YAML)
        self.assertEqual(set(cfg["environments"]), {"dev", "tst", "prd"})
        self.assertIn("cobol", cfg["compile_options"])


if __name__ == "__main__":
    unittest.main()
