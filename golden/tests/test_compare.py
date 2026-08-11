"""Unit tests for golden artifact comparison."""

from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from compare import compare_dirs  # noqa: E402


def _write_tree(base: Path, files: dict[str, str]) -> None:
    for rel, body in files.items():
        path = base / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(body, encoding="utf-8")


class TestCompare(unittest.TestCase):
    def test_identical_pass(self):
        with tempfile.TemporaryDirectory() as tmp:
            left = Path(tmp) / "a"
            right = Path(tmp) / "b"
            body = {"tables/x.csv": "A,B\n1,2\n", "display.txt": "ok\n"}
            _write_tree(left, body)
            _write_tree(right, body)
            ok, details = compare_dirs(left, right)
            self.assertTrue(ok)
            self.assertEqual(details, [])

    def test_single_column_difference(self):
        with tempfile.TemporaryDirectory() as tmp:
            left = Path(tmp) / "a"
            right = Path(tmp) / "b"
            _write_tree(left, {"tables/x.csv": "A,B\n1,2\n"})
            _write_tree(right, {"tables/x.csv": "A,B\n1,3\n"})
            ok, details = compare_dirs(left, right)
            self.assertFalse(ok)
            self.assertTrue(any("column B" in d for d in details))

    def test_missing_row(self):
        with tempfile.TemporaryDirectory() as tmp:
            left = Path(tmp) / "a"
            right = Path(tmp) / "b"
            _write_tree(left, {"tables/x.csv": "A,B\n1,2\n3,4\n"})
            _write_tree(right, {"tables/x.csv": "A,B\n1,2\n"})
            ok, details = compare_dirs(left, right)
            self.assertFalse(ok)
            self.assertTrue(any("row count" in d for d in details))

    def test_extra_row(self):
        with tempfile.TemporaryDirectory() as tmp:
            left = Path(tmp) / "a"
            right = Path(tmp) / "b"
            _write_tree(left, {"tables/x.csv": "A,B\n1,2\n"})
            _write_tree(right, {"tables/x.csv": "A,B\n1,2\n3,4\n"})
            ok, details = compare_dirs(left, right)
            self.assertFalse(ok)
            self.assertTrue(any("extra row" in d for d in details))


if __name__ == "__main__":
    unittest.main()
