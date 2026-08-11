#!/usr/bin/env python3
"""Load and validate build/build.yaml (WO-005)."""

from __future__ import annotations

import re
from pathlib import Path
from typing import Any


def _unquote(raw: str) -> str:
    raw = raw.strip()
    if len(raw) >= 2 and raw[0] == raw[-1] and raw[0] in "\"'":
        return raw[1:-1]
    return raw


def load_build_yaml(path: Path | str) -> dict[str, Any]:
    text = Path(path).read_text(encoding="utf-8")
    # Strip comments
    cleaned = "\n".join(
        ln for ln in text.splitlines() if ln.strip() and not ln.lstrip().startswith("#")
    )

    result: dict[str, Any] = {
        "compiler": {
            "min_supported_release": None,
            "out_of_support_releases": [],
            "out_of_support_after": {},
        },
        "compile_options": {},
        "environments": {},
    }

    m = re.search(r"min_supported_release:\s*(\S+)", cleaned)
    if m:
        result["compiler"]["min_supported_release"] = _unquote(m.group(1))

    oos = re.search(
        r"out_of_support_releases:\n((?:\s+-\s+\S+\n)+)",
        cleaned,
    )
    if oos:
        result["compiler"]["out_of_support_releases"] = [
            _unquote(x) for x in re.findall(r"-\s+(\S+)", oos.group(1))
        ]

    after = re.search(r"out_of_support_after:\n((?:\s+\S+:\s+\S+\n)+)", cleaned)
    if after:
        for key, val in re.findall(r'[\'"]?([0-9.]+)[\'"]?:\s*[\'"]?([^\'"\n]+)[\'"]?', after.group(1)):
            result["compiler"]["out_of_support_after"][key] = _unquote(val)

    for opt_type in ("dds", "cobol", "cl"):
        block = re.search(
            rf"(?m)^  {opt_type}:\n((?:    .+\n)+)",
            cleaned,
        )
        if block:
            opts: dict[str, str] = {}
            for k, v in re.findall(r"^\s{4}([A-Za-z0-9_]+):\s*(\S+)\s*$", block.group(1), re.M):
                opts[k] = _unquote(v)
            result["compile_options"][opt_type] = opts

    env_section = re.search(r"(?ms)^environments:\n(.*)$", cleaned)
    if env_section:
        for name, body in re.findall(
            r"(?m)^  ([a-z0-9_]+):\n((?:    .+\n?)*)",
            env_section.group(1),
        ):
            env: dict[str, str] = {}
            for k, v in re.findall(r"^\s{4}([a-z0-9_]+):\s*(\S+)\s*$", body, re.M):
                env[k] = _unquote(v)
            if env:
                result["environments"][name] = env

    if not result["environments"]:
        raise ValueError(f"Invalid build.yaml — no environments found in {path}")
    return result


def select_environment(cfg: dict[str, Any], env_name: str) -> dict[str, Any]:
    envs = cfg.get("environments") or {}
    if env_name not in envs:
        available = ", ".join(sorted(envs)) or "(none)"
        raise KeyError(
            f"Environment '{env_name}' not defined in build.yaml. Available: {available}"
        )
    env = dict(envs[env_name])
    for req in ("pgm_lib", "data_lib", "shared_lib", "tools_lib"):
        if req not in env or not env[req]:
            raise KeyError(f"Environment '{env_name}' missing required key '{req}'")
    return env
