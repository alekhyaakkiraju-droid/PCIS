"""Unit tests for RPT_RUN_LOG_T schema fixture validation (WO-237)."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1] / "scripts"
sys.path.insert(0, str(SCRIPTS))

from validate_schema_fixture import validate  # noqa: E402


class TestSchemaFixture(unittest.TestCase):
    def test_fixture_matches_ddl(self):
        self.assertEqual(validate(), 0)


if __name__ == "__main__":
    unittest.main()
