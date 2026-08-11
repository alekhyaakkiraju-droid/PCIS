#!/usr/bin/env python3
"""Legacy IBM i build orchestrator (WO-005)."""

from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

# Allow running as script from repo root or build/scripts
SCRIPTS = Path(__file__).resolve().parent
if str(SCRIPTS) not in sys.path:
    sys.path.insert(0, str(SCRIPTS))

from build_config import load_build_yaml, select_environment
from cl_executor import RealExecutor, StubExecutor
from compiler_gate import run_compiler_gate
from dependency_resolver import (
    DependencyResolver,
    ManifestMember,
    parse_manifest_files,
    write_compile_order,
)


def library_list_for(env: dict[str, str]) -> list[str]:
    return [env["pgm_lib"], env["data_lib"], env["shared_lib"], env["tools_lib"]]


def object_name(member: ManifestMember) -> str:
    return member.name[:10]


def compile_command(member: ManifestMember, env: dict[str, str], options: dict[str, Any]) -> str:
    pgm_lib = env["pgm_lib"]
    obj = object_name(member)
    src = Path(member.path).name
    if member.type == "dds":
        opts = options.get("dds", {})
        dbg = opts.get("DBGVIEW", "*SOURCE")
        tgt = opts.get("TGTRLS", "*CURRENT")
        return (
            f"CRTDSPF FILE({pgm_lib}/{obj}) SRCFILE({pgm_lib}/QDDSSRC) "
            f"SRCMBR({Path(src).stem.upper()}) DBGVIEW({dbg}) TGTRLS({tgt})"
        )
    if member.type == "cobol":
        opts = options.get("cobol", {})
        dbg = opts.get("DBGVIEW", "*SOURCE")
        tgt = opts.get("TGTRLS", "*CURRENT")
        commit = opts.get("COMMIT", "*NONE")
        return (
            f"CRTSQLCBLI OBJ({pgm_lib}/{obj}) SRCFILE({pgm_lib}/QCBLLESRC) "
            f"SRCMBR({Path(src).stem.upper()}) OBJTYPE(*PGM) "
            f"DBGVIEW({dbg}) TGTRLS({tgt}) COMMIT({commit})"
        )
    if member.type == "cl":
        opts = options.get("cl", {})
        dbg = opts.get("DBGVIEW", "*SOURCE")
        tgt = opts.get("TGTRLS", "*CURRENT")
        return (
            f"CRTBNDCL PGM({pgm_lib}/{obj}) SRCFILE({pgm_lib}/QCLSRC) "
            f"SRCMBR({Path(src).stem.upper()}) DBGVIEW({dbg}) TGTRLS({tgt})"
        )
    raise ValueError(f"Unsupported member type for compile: {member.type}")


def object_type(member: ManifestMember) -> str:
    return {"dds": "*FILE", "cobol": "*PGM", "cl": "*PGM"}.get(member.type, "*PGM")


def run_build(
    *,
    env_name: str,
    repo_root: Path,
    executor: Any,
    build_yaml: Path | None = None,
    manifest_path: Path | None = None,
    reports_dir: Path | None = None,
) -> int:
    build_yaml = build_yaml or repo_root / "build" / "build.yaml"
    manifest_path = manifest_path or repo_root / "manifest" / "pcis-manifest.yaml"
    reports_dir = reports_dir or repo_root / "build" / "reports"
    reports_dir.mkdir(parents=True, exist_ok=True)

    cfg = load_build_yaml(build_yaml)
    try:
        env = select_environment(cfg, env_name)
    except KeyError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2

    gate = run_compiler_gate(executor, cfg.get("compiler", {}))
    print(gate.message)
    if not gate.ok:
        print(f"ERROR: compiler gate failed: {gate.message}", file=sys.stderr)
        return 3

    members = parse_manifest_files(manifest_path.read_text(encoding="utf-8"))
    resolver = DependencyResolver(members)
    try:
        ordered, warnings = resolver.resolve()
    except ValueError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 4

    for w in warnings:
        print(f"WARN: {w}")

    order_path = reports_dir / "compile-order.txt"
    write_compile_order(ordered, order_path)
    print(f"Compile order ({len(ordered)} members) written to {order_path}")
    for i, m in enumerate(ordered, 1):
        print(f"{i:04d}  {m.type:<8}  {m.path}")

    libl = library_list_for(env)
    options = cfg.get("compile_options", {})
    manifest_entries: list[dict[str, Any]] = []
    overall = "success"

    for m in ordered:
        cmd = compile_command(m, env, options)
        result = executor.run(cmd, library_list=libl)
        entry = {
            "path": m.path,
            "checksum": m.checksum,
            "object_name": object_name(m),
            "object_type": object_type(m),
            "compile_command": cmd,
            "result": "success" if result.exit_code == 0 else "failure",
            "message_id": result.message_id,
        }
        manifest_entries.append(entry)
        if result.exit_code != 0:
            overall = "failure"
            print(
                json.dumps(
                    {
                        "event": "compile_failure",
                        "member_path": m.path,
                        "cl_command": cmd,
                        "message_id": result.message_id or "UNKNOWN",
                        "stderr": result.stderr,
                    }
                ),
                file=sys.stderr,
            )
            _write_build_manifest(
                reports_dir / "build-manifest.json",
                env_name=env_name,
                compiler_release=gate.release,
                members=manifest_entries,
                overall_result=overall,
            )
            return 1

    _write_build_manifest(
        reports_dir / "build-manifest.json",
        env_name=env_name,
        compiler_release=gate.release,
        members=manifest_entries,
        overall_result=overall,
    )
    print(f"Build succeeded for environment '{env_name}' ({len(manifest_entries)} members)")
    return 0


def _write_build_manifest(
    path: Path,
    *,
    env_name: str,
    compiler_release: str | None,
    members: list[dict[str, Any]],
    overall_result: str,
) -> None:
    doc = {
        "timestamp": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "environment": env_name,
        "compiler_release": compiler_release,
        "members": members,
        "overall_result": overall_result,
    }
    path.write_text(json.dumps(doc, indent=2) + "\n", encoding="utf-8")


def main() -> int:
    ap = argparse.ArgumentParser(description="PCIS legacy build orchestrator")
    ap.add_argument("--env", required=True, help="Environment name: dev, tst, or prd")
    ap.add_argument("--repo-root", default=".")
    ap.add_argument("--manifest", default=None)
    ap.add_argument("--build-yaml", default=None)
    ap.add_argument("--reports-dir", default=None)
    ap.add_argument(
        "--executor",
        choices=("stub", "real"),
        default="stub",
        help="CL executor (default stub for local/CI without IBM i)",
    )
    ap.add_argument("--stub-compiler-release", default="Enterprise COBOL for i Version 7.5")
    args = ap.parse_args()

    repo_root = Path(args.repo_root).resolve()
    if args.executor == "real":
        executor: Any = RealExecutor()
    else:
        executor = StubExecutor(compiler_release_output=args.stub_compiler_release)

    return run_build(
        env_name=args.env,
        repo_root=repo_root,
        executor=executor,
        build_yaml=Path(args.build_yaml) if args.build_yaml else None,
        manifest_path=Path(args.manifest) if args.manifest else None,
        reports_dir=Path(args.reports_dir) if args.reports_dir else None,
    )


if __name__ == "__main__":
    raise SystemExit(main())
