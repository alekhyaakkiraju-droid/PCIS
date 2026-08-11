#!/usr/bin/env python3
"""Static validation for WO-145 Keycloak realm export and Terraform module layout."""

from __future__ import annotations

import json
import subprocess
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]  # infra/keycloak
INFRA = ROOT.parent
MODULE = INFRA / "modules" / "keycloak"
REALM_PATH = ROOT / "realm-export.json"

REQUIRED_ROLES = {
    "CLAIMS_ADJUSTER",
    "CLAIMS_SUPERVISOR",
    "CSR",
    "UNDERWRITER",
    "FINANCE",
    "COMPLIANCE",
    "BATCH_SVC",
}

REQUIRED_CLIENTS = {"pcis-spa", "pcis-gateway", "pcis-batch"}

REQUIRED_PERSONAS = {
    "Claims Adjuster",
    "Claims Supervisor",
    "CSR",
    "Underwriter",
    "Billing Clerk",
    "Batch Operations Operator",
    "Compliance Officer",
    "IT Admin",
    "Reporting Analyst",
}


class TestRealmExport(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.realm = json.loads(REALM_PATH.read_text())

    def test_realm_name_is_pcis(self) -> None:
        self.assertEqual(self.realm["realm"], "pcis")
        self.assertTrue(self.realm.get("enabled", False))

    def test_token_ttls(self) -> None:
        self.assertEqual(self.realm["accessTokenLifespan"], 900)
        self.assertEqual(self.realm["ssoSessionIdleTimeout"], 28800)
        self.assertEqual(self.realm["ssoSessionMaxLifespan"], 28800)
        self.assertEqual(self.realm["clientSessionIdleTimeout"], 3600)
        self.assertEqual(self.realm["clientSessionMaxLifespan"], 3600)

    def test_realm_roles(self) -> None:
        names = {r["name"] for r in self.realm["roles"]["realm"]}
        self.assertTrue(REQUIRED_ROLES.issubset(names), names)

    def test_clients(self) -> None:
        clients = {c["clientId"]: c for c in self.realm["clients"]}
        self.assertTrue(REQUIRED_CLIENTS.issubset(clients), set(clients))

        spa = clients["pcis-spa"]
        self.assertTrue(spa["publicClient"])
        self.assertTrue(spa["standardFlowEnabled"])
        self.assertFalse(spa["directAccessGrantsEnabled"])
        self.assertEqual(spa["attributes"].get("pkce.code.challenge.method"), "S256")

        gateway = clients["pcis-gateway"]
        self.assertFalse(gateway["publicClient"])
        self.assertTrue(gateway["serviceAccountsEnabled"])
        self.assertIn("CHANGE_ME", gateway.get("secret", ""))

        batch = clients["pcis-batch"]
        self.assertFalse(batch["publicClient"])
        self.assertTrue(batch["serviceAccountsEnabled"])
        self.assertEqual(batch["attributes"].get("access.token.lifespan"), "3600")
        self.assertIn("CHANGE_ME", batch.get("secret", ""))

    def test_authority_limit_mapper(self) -> None:
        scopes = {s["name"]: s for s in self.realm.get("clientScopes", [])}
        self.assertIn("authority_limit", scopes)
        mappers = scopes["authority_limit"]["protocolMappers"]
        self.assertEqual(len(mappers), 1)
        mapper = mappers[0]
        self.assertEqual(mapper["name"], "authority_limit")
        self.assertEqual(mapper["protocolMapper"], "oidc-usermodel-attribute-mapper")
        self.assertEqual(mapper["config"]["user.attribute"], "authority_limit")
        self.assertEqual(mapper["config"]["claim.name"], "authority_limit")
        self.assertEqual(mapper["config"]["access.token.claim"], "true")
        self.assertIn("authority_limit", self.realm.get("defaultDefaultClientScopes", []))

    def test_nine_seed_personas(self) -> None:
        personas = set()
        temporary_passwords = 0
        for user in self.realm.get("users", []):
            attrs = user.get("attributes") or {}
            persona_vals = attrs.get("persona") or []
            personas.update(persona_vals)
            for cred in user.get("credentials") or []:
                if cred.get("temporary") is True:
                    temporary_passwords += 1
                    self.assertTrue(
                        str(cred.get("value", "")).startswith("TempChangeMe!"),
                        f"non-placeholder password for {user.get('username')}",
                    )
        self.assertTrue(REQUIRED_PERSONAS.issubset(personas), personas)
        self.assertGreaterEqual(temporary_passwords, 9)

    def test_batch_service_account_has_batch_svc_role(self) -> None:
        sa = next(
            (
                u
                for u in self.realm["users"]
                if u.get("serviceAccountClientId") == "pcis-batch"
            ),
            None,
        )
        self.assertIsNotNone(sa)
        self.assertIn("BATCH_SVC", sa.get("realmRoles", []))

    def test_adjuster_has_authority_limit_attribute(self) -> None:
        adj = next(u for u in self.realm["users"] if u["username"] == "adj.alice")
        self.assertIn("CLAIMS_ADJUSTER", adj["realmRoles"])
        self.assertEqual(adj["attributes"]["authority_limit"], ["25000"])

    def test_no_real_looking_secrets(self) -> None:
        text = REALM_PATH.read_text()
        forbidden = ["AKIA", "-----BEGIN", "aws_secret", "password123"]
        for token in forbidden:
            self.assertNotIn(token, text)


class TestModuleLayout(unittest.TestCase):
    def test_module_files_present(self) -> None:
        for name in ("main.tf", "variables.tf", "outputs.tf", "versions.tf", "README.md"):
            self.assertTrue((MODULE / name).is_file(), name)

    def test_docker_compose_and_smoke_present(self) -> None:
        self.assertTrue((ROOT / "docker-compose.yml").is_file())
        self.assertTrue((ROOT / "tests" / "smoke_test.sh").is_file())
        self.assertTrue((INFRA.parent / "docs" / "runbooks" / "keycloak-operations.md").is_file())

    def test_env_tfvars_present(self) -> None:
        for name in ("dev.tfvars", "staging.tfvars", "prod.tfvars"):
            path = ROOT / "environments" / name
            self.assertTrue(path.is_file(), name)
            text = path.read_text()
            self.assertIn("replica_count", text)
            self.assertIn("certificate_arn", text)
            self.assertIn("db_host", text)

    def test_environment_wiring_present(self) -> None:
        for env in ("dev", "tst", "prd"):
            self.assertTrue((INFRA / "environments" / env / "keycloak.tf").is_file())
            tfvars = (INFRA / "environments" / env / "terraform.tfvars").read_text()
            self.assertIn("enable_keycloak", tfvars)

    def test_module_references_secrets_manager_not_literal_passwords(self) -> None:
        main = (MODULE / "main.tf").read_text()
        self.assertIn("aws_secretsmanager_secret", main)
        self.assertIn("REPLACE_FROM_SECRETS_MANAGER", main)
        self.assertIn("/health/ready", main)
        self.assertIn("bitnami", main)


class TestTerraformFmtValidate(unittest.TestCase):
    def test_terraform_fmt_check(self) -> None:
        if not self._terraform_available():
            self.skipTest("terraform not installed")
        result = subprocess.run(
            ["terraform", "fmt", "-check", "-recursive", str(MODULE)],
            capture_output=True,
            text=True,
            check=False,
        )
        self.assertEqual(
            result.returncode,
            0,
            f"terraform fmt -check failed:\n{result.stdout}\n{result.stderr}",
        )

    def test_terraform_validate_via_wrapper(self) -> None:
        script = ROOT / "tests" / "validate_terraform.sh"
        if not script.is_file():
            self.skipTest("validate_terraform.sh missing")
        if not self._terraform_available():
            self.skipTest("terraform not installed")
        result = subprocess.run(
            ["bash", str(script)],
            capture_output=True,
            text=True,
            check=False,
        )
        self.assertEqual(
            result.returncode,
            0,
            f"validate_terraform.sh failed:\n{result.stdout}\n{result.stderr}",
        )

    @staticmethod
    def _terraform_available() -> bool:
        try:
            subprocess.run(
                ["terraform", "version"],
                capture_output=True,
                check=True,
            )
            return True
        except (FileNotFoundError, subprocess.CalledProcessError):
            return False


if __name__ == "__main__":
    unittest.main()
