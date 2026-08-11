#!/usr/bin/env python3
"""Lightweight fixture tests for WO-129 (no AWS required)."""

from __future__ import annotations

import ipaddress
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENVS = ROOT / "environments"


class TestEnvironmentFixtures(unittest.TestCase):
    def test_tfvars_exist_for_all_envs(self) -> None:
        for env in ("dev", "tst", "prd"):
            path = ENVS / env / "terraform.tfvars"
            self.assertTrue(path.is_file(), f"missing {path}")

    def test_cidrs_are_non_overlapping(self) -> None:
        expected = {
            "dev": "10.0.0.0/16",
            "tst": "10.1.0.0/16",
            "prd": "10.2.0.0/16",
        }
        networks = []
        for env, cidr in expected.items():
            text = (ENVS / env / "terraform.tfvars").read_text()
            self.assertIn(f'vpc_cidr                = "{cidr}"', text)
            networks.append(ipaddress.ip_network(cidr))
        for i, a in enumerate(networks):
            for b in networks[i + 1 :]:
                self.assertFalse(a.overlaps(b), f"{a} overlaps {b}")

    def test_prd_enables_ha_nat_dev_does_not(self) -> None:
        dev = (ENVS / "dev" / "terraform.tfvars").read_text()
        prd = (ENVS / "prd" / "terraform.tfvars").read_text()
        self.assertIn("enable_ha_nat           = false", dev)
        self.assertIn("enable_ha_nat           = true", prd)

    def test_backend_keys_are_per_environment(self) -> None:
        keys = set()
        for env in ("dev", "tst", "prd"):
            text = (ENVS / env / "backend.hcl").read_text()
            self.assertIn("dynamodb_table = \"pcis-terraform-locks\"", text)
            self.assertIn(f"key            = \"{env}/network/terraform.tfstate\"", text)
            for line in text.splitlines():
                if line.startswith("key"):
                    keys.add(line.split("=")[1].strip().strip('"'))
        self.assertEqual(len(keys), 3)

    def test_module_files_present(self) -> None:
        mod = ROOT / "modules" / "network"
        for name in ("main.tf", "variables.tf", "outputs.tf", "versions.tf", "README.md"):
            self.assertTrue((mod / name).is_file(), name)


if __name__ == "__main__":
    unittest.main()
