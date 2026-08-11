"""Unit tests for dependency_resolver.py (WO-005)."""

from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1] / "scripts"
sys.path.insert(0, str(SCRIPTS))

from dependency_resolver import (  # noqa: E402
    DependencyResolver,
    ManifestMember,
    parse_manifest_files,
)

FIXTURES = Path(__file__).resolve().parents[1] / "test-fixtures"


class TestDependencyResolver(unittest.TestCase):
    def setUp(self):
        text = (FIXTURES / "sample-manifest.yaml").read_text(encoding="utf-8")
        self.members = parse_manifest_files(text)
        self.resolver = DependencyResolver(self.members)

    def test_topological_sort_order(self):
        ordered, _ = self.resolver.resolve()
        names = [m.name for m in ordered]
        self.assertEqual(names.index("SCREEN1") < names.index("PGM001A"), True)
        self.assertEqual(names.index("PGM001A") < names.index("PGM002B"), True)
        self.assertEqual(names.index("PGM002B") < names.index("JOB001"), True)

    def test_ile_order_dds_before_cobol_before_cl(self):
        ordered, _ = self.resolver.resolve()
        types = [m.type for m in ordered]
        last_dds = max(i for i, t in enumerate(types) if t == "dds")
        first_cobol = min(i for i, t in enumerate(types) if t == "cobol")
        last_cobol = max(i for i, t in enumerate(types) if t == "cobol")
        first_cl = min(i for i, t in enumerate(types) if t == "cl")
        self.assertLess(last_dds, first_cobol)
        self.assertLess(last_cobol, first_cl)

    def test_missing_callee_skipped(self):
        ordered, warnings = self.resolver.resolve()
        names = {m.name for m in ordered}
        self.assertNotIn("AUDLOG01", names)
        self.assertTrue(any("missing-callee" in w for w in warnings))

    def test_cycle_detection(self):
        a = ManifestMember("a.cbl", "cobol", "shipped", "x", [{"target_name": "B", "relationship_type": "calls"}])
        b = ManifestMember("b.cbl", "cobol", "shipped", "y", [{"target_name": "A", "relationship_type": "calls"}])
        with self.assertRaises(ValueError) as ctx:
            DependencyResolver([a, b]).resolve()
        self.assertIn("Circular", str(ctx.exception))

    def test_buildable_count(self):
        ordered, _ = self.resolver.resolve()
        self.assertEqual(len(ordered), 5)


if __name__ == "__main__":
    unittest.main()
