#!/usr/bin/env python3
"""Unit tests for the OpenAPI contract diff gate (WO-221)."""

from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
GATE_PATH = REPO_ROOT / "scripts" / "openapi_contract_gate.py"


def load_gate_module():
    spec = importlib.util.spec_from_file_location("openapi_contract_gate", GATE_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


gate = load_gate_module()


class OpenApiContractGateTest(unittest.TestCase):
    def test_manifest_loads_customer_api(self):
        entries = gate.load_manifest(REPO_ROOT / "contracts" / "snapshots" / "manifest.yaml")
        ids = {entry["id"] for entry in entries}
        self.assertIn("customer-api", ids)

    def test_customer_snapshot_matches_generated_spec(self):
        with tempfile.TemporaryDirectory() as tmp:
            work_dir = Path(tmp)
            entry = {
                "id": "customer-api",
                "snapshot": "contracts/snapshots/customer-api.yaml",
                "generated": "frontend/specs/customer-svc.yaml",
            }
            ok, message = gate.check_api(entry, dry_run=False, update_snapshots=False, work_dir=work_dir)
            self.assertTrue(ok, message)

    def test_drift_is_detected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            snapshot = root / "snapshot.yaml"
            generated = root / "generated.yaml"
            snapshot.write_text("openapi: 3.1.0\ninfo:\n  title: A\n  version: 1.0.0\n", encoding="utf-8")
            generated.write_text("openapi: 3.1.0\ninfo:\n  title: B\n  version: 1.0.0\n", encoding="utf-8")

            original_root = gate.REPO_ROOT
            gate.REPO_ROOT = root
            try:
                entry = {
                    "id": "demo-api",
                    "snapshot": "snapshot.yaml",
                    "generated": "generated.yaml",
                }
                ok, message = gate.check_api(
                    entry, dry_run=False, update_snapshots=False, work_dir=root / "work"
                )
            finally:
                gate.REPO_ROOT = original_root

            self.assertFalse(ok)
            self.assertIn("drift detected", message)

    def test_update_snapshots_refreshes_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            generated = root / "generated.yaml"
            snapshot = root / "contracts" / "snapshots" / "demo-api.yaml"
            generated.write_text("openapi: 3.1.0\ninfo:\n  title: Fresh\n  version: 2.0.0\n", encoding="utf-8")

            original_root = gate.REPO_ROOT
            gate.REPO_ROOT = root
            try:
                entry = {
                    "id": "demo-api",
                    "snapshot": "contracts/snapshots/demo-api.yaml",
                    "generated": "generated.yaml",
                }
                ok, _ = gate.check_api(
                    entry, dry_run=False, update_snapshots=True, work_dir=root / "work"
                )
            finally:
                gate.REPO_ROOT = original_root

            self.assertTrue(ok)
            self.assertTrue(snapshot.is_file())
            self.assertEqual(snapshot.read_text(encoding="utf-8"), generated.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
