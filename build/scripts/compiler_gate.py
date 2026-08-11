#!/usr/bin/env python3
"""Enterprise COBOL for i compiler release gate (WO-005)."""

from __future__ import annotations

import re
from dataclasses import dataclass
from datetime import date
from typing import Any, Protocol


class Executor(Protocol):
    def run(self, command: str, *, library_list: list[str] | None = None) -> "ExecResult":
        ...


@dataclass
class ExecResult:
    exit_code: int
    stdout: str
    stderr: str
    message_id: str | None = None


@dataclass
class GateResult:
    ok: bool
    release: str | None
    message: str


RELEASE_RE = re.compile(
    r"(?:Version|Release|V)\s*([0-9]+(?:\.[0-9]+){1,2})",
    re.I,
)
SIMPLE_RE = re.compile(r"\b([0-9]+\.[0-9]+(?:\.[0-9]+)?)\b")


def parse_release(output: str) -> str | None:
    if not output or not output.strip():
        return None
    m = RELEASE_RE.search(output)
    if m:
        return m.group(1)
    m = SIMPLE_RE.search(output)
    return m.group(1) if m else None


def normalize_release(release: str) -> str:
    parts = release.split(".")
    return ".".join(parts[:2])  # major.minor for out-of-support list


def assert_compiler_release(
    detected: str | None,
    compiler_cfg: dict[str, Any],
    *,
    today: date | None = None,
) -> GateResult:
    if not detected:
        return GateResult(
            ok=False,
            release=None,
            message="Compiler release could not be determined — refusing to build",
        )

    today = today or date.today()
    base = normalize_release(detected)
    out_of_support = [normalize_release(str(x)) for x in compiler_cfg.get("out_of_support_releases", [])]
    after_map = compiler_cfg.get("out_of_support_after", {}) or {}

    if base in out_of_support:
        # Special case: 6.3 allowed only before extended support end
        end = after_map.get(base) or after_map.get(detected)
        if base == "6.3" and end:
            end_date = date.fromisoformat(str(end))
            if today <= end_date:
                return GateResult(
                    ok=True,
                    release=detected,
                    message=f"Compiler release {detected} accepted (6.3 within extended support until {end})",
                )
        return GateResult(
            ok=False,
            release=detected,
            message=(
                f"Compiler release {detected} is out of support "
                f"(blocked levels: {', '.join(out_of_support)})"
            ),
        )

    return GateResult(
        ok=True,
        release=detected,
        message=f"Compiler release {detected} accepted",
    )


def detect_compiler_release(executor: Executor) -> str | None:
    """Query IBM i for Enterprise COBOL release via pluggable executor."""
    result = executor.run("DSPSFWRSC OUTPUT(*PRINT)")
    if result.exit_code != 0:
        # Fallback probe used by stub / alternate environments
        result = executor.run("QSYS/DSPCOBOLRLS")
    if result.exit_code != 0:
        return None
    return parse_release(result.stdout + "\n" + result.stderr)


def run_compiler_gate(executor: Executor, compiler_cfg: dict[str, Any]) -> GateResult:
    detected = detect_compiler_release(executor)
    return assert_compiler_release(detected, compiler_cfg)
