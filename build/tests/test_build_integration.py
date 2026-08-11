"""Integration tests for build orchestrator with StubExecutor (WO-005)."""

from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1] / "scripts"
ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(SCRIPTS))

from build_orchestrator import run_build  # noqa: E402
from cl_executor import StubExecutor  # noqa: E402
from dependency_resolver import DependencyResolver, parse_manifest_files  # noqa: E402

FIXTURES = Path(__file__).resolve().parents[1] / "test-fixtures"


class TestBuildIntegration(unittest.TestCase):
    def test_fixture_build_sequence(self):
        with tempfile.TemporaryDirectory() as td:
            reports = Path(td)
            ex = StubExecutor()
            rc = run_build(
                env_name="dev",
                repo_root=ROOT,
                executor=ex,
                build_yaml=FIXTURES / "sample-build.yaml",
                manifest_path=FIXTURES / "sample-manifest.yaml",
                reports_dir=reports,
            )
            self.assertEqual(rc, 0)
            cmds = [inv.command for inv in ex.invocations if inv.command.startswith("CRT")]
            self.assertTrue(any(c.startswith("CRTDSPF") for c in cmds))
            self.assertTrue(any(c.startswith("CRTSQLCBLI") for c in cmds))
            self.assertTrue(any(c.startswith("CRTBNDCL") for c in cmds))
            # DDS before COBOL before CL
            first_dds = next(i for i, c in enumerate(cmds) if c.startswith("CRTDSPF"))
            first_cbl = next(i for i, c in enumerate(cmds) if c.startswith("CRTSQLCBLI"))
            first_cl = next(i for i, c in enumerate(cmds) if c.startswith("CRTBNDCL"))
            self.assertLess(first_dds, first_cbl)
            self.assertLess(first_cbl, first_cl)
            # library list from build.yaml
            compile_inv = [i for i in ex.invocations if i.command.startswith("CRT")][0]
            self.assertEqual(compile_inv.library_list[0], "INSDEV")
            self.assertIn("INSCOM", compile_inv.library_list)

            manifest = json.loads((reports / "build-manifest.json").read_text())
            self.assertEqual(manifest["environment"], "dev")
            self.assertEqual(manifest["overall_result"], "success")
            self.assertEqual(len(manifest["members"]), 5)
            self.assertTrue((reports / "compile-order.txt").is_file())

    def test_abort_on_first_failure(self):
        with tempfile.TemporaryDirectory() as td:
            reports = Path(td)
            ex = StubExecutor(fail_on_command_substring="PGM002B")
            rc = run_build(
                env_name="dev",
                repo_root=ROOT,
                executor=ex,
                build_yaml=FIXTURES / "sample-build.yaml",
                manifest_path=FIXTURES / "sample-manifest.yaml",
                reports_dir=reports,
            )
            self.assertEqual(rc, 1)
            crt = [i.command for i in ex.invocations if i.command.startswith("CRT")]
            # No commands after the failed member
            failed_idx = next(i for i, c in enumerate(crt) if "PGM002B" in c)
            self.assertEqual(len(crt), failed_idx + 1)
            manifest = json.loads((reports / "build-manifest.json").read_text())
            self.assertEqual(manifest["overall_result"], "failure")
            self.assertTrue(any(m["result"] == "failure" for m in manifest["members"]))

    def test_full_repo_manifest_dds_before_cobol(self):
        text = (ROOT / "manifest" / "pcis-manifest.yaml").read_text(encoding="utf-8")
        ordered, _ = DependencyResolver(parse_manifest_files(text)).resolve()
        dds = [m for m in ordered if m.type == "dds"]
        cobol = [m for m in ordered if m.type == "cobol"]
        self.assertEqual(len(dds), 22)
        self.assertEqual(len(cobol), 8)
        last_dds = ordered.index(dds[-1])
        first_cobol = ordered.index(cobol[0])
        self.assertLess(last_dds, first_cobol)

    def test_invalid_environment(self):
        with tempfile.TemporaryDirectory() as td:
            rc = run_build(
                env_name="qa",
                repo_root=ROOT,
                executor=StubExecutor(),
                build_yaml=FIXTURES / "sample-build.yaml",
                manifest_path=FIXTURES / "sample-manifest.yaml",
                reports_dir=Path(td),
            )
            self.assertEqual(rc, 2)

    def test_shell_entrypoint_exists(self):
        script = ROOT / "build" / "scripts" / "build_legacy.sh"
        self.assertTrue(script.is_file())
        self.assertIn("build_orchestrator.py", script.read_text())


if __name__ == "__main__":
    unittest.main()
