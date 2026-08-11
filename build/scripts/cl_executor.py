#!/usr/bin/env python3
"""Pluggable IBM i CL command executors (WO-005)."""

from __future__ import annotations

import os
import re
import subprocess
from dataclasses import dataclass
from typing import Callable

from compiler_gate import ExecResult


@dataclass
class Invocation:
    command: str
    library_list: list[str]


class StubExecutor:
    """Records invocations for tests; optionally fails on a configured substring."""

    def __init__(
        self,
        *,
        compiler_release_output: str = "Enterprise COBOL for i Version 7.5",
        fail_on_command_substring: str | None = None,
        fail_message_id: str = "CPF9898",
        on_run: Callable[[str], None] | None = None,
    ):
        self.compiler_release_output = compiler_release_output
        self.fail_on_command_substring = fail_on_command_substring
        self.fail_message_id = fail_message_id
        self.on_run = on_run
        self.invocations: list[Invocation] = []

    def run(self, command: str, *, library_list: list[str] | None = None) -> ExecResult:
        libl = list(library_list or [])
        self.invocations.append(Invocation(command=command, library_list=libl))
        if self.on_run:
            self.on_run(command)
        upper = command.upper()
        if "DSPSFWRSC" in upper or "DSPCOBOLRLS" in upper:
            return ExecResult(0, self.compiler_release_output, "")
        if self.fail_on_command_substring and self.fail_on_command_substring in command:
            return ExecResult(
                1,
                "",
                f"Stubbed failure for {command}",
                message_id=self.fail_message_id,
            )
        return ExecResult(0, f"OK: {command}", "")


class RealExecutor:
    """Runs CL via SSH against an IBM i partition (PCIS_IBMI_SSH)."""

    def __init__(self, ssh_target: str | None = None):
        self.ssh_target = ssh_target or os.environ.get("PCIS_IBMI_SSH", "")

    def run(self, command: str, *, library_list: list[str] | None = None) -> ExecResult:
        if not self.ssh_target:
            return ExecResult(
                1,
                "",
                "PCIS_IBMI_SSH not configured — cannot run RealExecutor",
                message_id="PCIS0001",
            )
        libl = library_list or []
        libl_cmd = " ".join(f"CHGLIBL LIBL({lib})" for lib in libl)
        remote = f"{libl_cmd}; {command}" if libl_cmd else command
        try:
            proc = subprocess.run(
                ["ssh", self.ssh_target, remote],
                capture_output=True,
                text=True,
                check=False,
            )
        except OSError as exc:
            return ExecResult(1, "", str(exc), message_id="PCIS0002")
        msg_id = None
        for token in (proc.stderr or "").split():
            if re.match(r"^[A-Z]{3}[A-Z0-9]{4}$", token):
                msg_id = token
                break
        return ExecResult(proc.returncode, proc.stdout, proc.stderr, message_id=msg_id)
