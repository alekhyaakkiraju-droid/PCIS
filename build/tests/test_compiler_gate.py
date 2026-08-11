"""Unit tests for compiler_gate.py (WO-005)."""

from __future__ import annotations

import sys
import unittest
from datetime import date
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1] / "scripts"
sys.path.insert(0, str(SCRIPTS))

from cl_executor import StubExecutor  # noqa: E402
from compiler_gate import (  # noqa: E402
    assert_compiler_release,
    parse_release,
    run_compiler_gate,
)

CFG = {
    "min_supported_release": "7.1",
    "out_of_support_releases": ["4.2", "5.1", "5.2", "6.3"],
    "out_of_support_after": {"6.3": "2025-09-30"},
}


class TestCompilerGate(unittest.TestCase):
    def test_parse_release(self):
        self.assertEqual(parse_release("Enterprise COBOL for i Version 7.5"), "7.5")
        self.assertEqual(parse_release("Release 7.4.0"), "7.4.0")

    def test_accept_supported(self):
        for rel in ("7.1", "7.2", "7.3", "7.4", "7.5"):
            result = assert_compiler_release(rel, CFG, today=date(2026, 1, 1))
            self.assertTrue(result.ok, rel)

    def test_reject_out_of_support(self):
        for rel in ("4.2", "5.1", "5.2", "6.3"):
            result = assert_compiler_release(rel, CFG, today=date(2026, 1, 1))
            self.assertFalse(result.ok, rel)

    def test_63_within_extended_support(self):
        result = assert_compiler_release("6.3", CFG, today=date(2025, 9, 30))
        self.assertTrue(result.ok)

    def test_unparseable(self):
        result = assert_compiler_release(None, CFG)
        self.assertFalse(result.ok)
        self.assertIn("could not be determined", result.message)

    def test_gate_via_stub_executor(self):
        ex = StubExecutor(compiler_release_output="Enterprise COBOL for i Version 7.5")
        result = run_compiler_gate(ex, CFG)
        self.assertTrue(result.ok)
        self.assertEqual(result.release, "7.5")

    def test_gate_fails_when_detection_fails(self):
        class Boom:
            def run(self, command, *, library_list=None):
                from compiler_gate import ExecResult

                return ExecResult(1, "", "connection refused")

        result = run_compiler_gate(Boom(), CFG)
        self.assertFalse(result.ok)


if __name__ == "__main__":
    unittest.main()
