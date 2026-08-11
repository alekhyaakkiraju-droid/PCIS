"""Validate docker/base/Dockerfile contains required security/runtime strings (no Docker daemon)."""
from __future__ import annotations

import unittest
from pathlib import Path

DOCKERFILE = Path(__file__).resolve().parents[1] / "Dockerfile"


class TestBaseDockerfileContent(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.text = DOCKERFILE.read_text(encoding="utf-8")

    def test_dockerfile_exists(self) -> None:
        self.assertTrue(DOCKERFILE.is_file(), f"missing {DOCKERFILE}")

    def test_distroless_java21(self) -> None:
        self.assertIn("distroless", self.text)
        self.assertIn("java21", self.text.lower())
        self.assertIn("gcr.io/distroless/java21-debian12", self.text)

    def test_nonroot_user(self) -> None:
        self.assertRegex(self.text, r"(?m)^\s*USER\s+nonroot\s*$")

    def test_javaagent_entrypoint(self) -> None:
        self.assertIn("javaagent", self.text)
        self.assertIn("-javaagent:/opt/otel/", self.text)
        self.assertIn("ENTRYPOINT", self.text)

    def test_sha256_verify(self) -> None:
        self.assertIn("SHA256", self.text.upper())
        self.assertIn("sha256sum", self.text)


if __name__ == "__main__":
    unittest.main()
