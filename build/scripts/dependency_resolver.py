#!/usr/bin/env python3
"""Dependency-ordered compile list from pcis-manifest.yaml (WO-005)."""

from __future__ import annotations

import argparse
import re
import sys
from collections import defaultdict, deque
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

# ILE build tiers — lower compiles first
TYPE_TIER = {
    "dds": 0,
    "module": 1,
    "srvpgm": 2,
    "cobol": 3,
    "pgm": 3,
    "cl": 4,
    "printer-file": 5,
}

BUILDABLE_TYPES = {"dds", "cobol", "cl"}


@dataclass
class ManifestMember:
    path: str
    type: str
    status: str
    checksum: str | None
    dependencies: list[dict[str, str]] = field(default_factory=list)

    @property
    def name(self) -> str:
        return Path(self.path).stem.upper() if "/" in self.path or "." in self.path else self.path.upper()

    @property
    def buildable(self) -> bool:
        return self.status == "shipped" and self.type in BUILDABLE_TYPES and self.path.endswith(
            (".cbl", ".dspf", ".clle", ".pf", ".lf", ".cl")
        )


def parse_manifest_files(text: str) -> list[ManifestMember]:
    members: list[ManifestMember] = []
    for block in re.split(r"(?m)^  - path:", text)[1:]:
        path_m = re.match(r"\s*(\S+)", block)
        if not path_m:
            continue
        path = path_m.group(1)
        type_m = re.search(r"(?m)^\s+type:\s*(\S+)\s*$", block)
        status_m = re.search(r"(?m)^\s+status:\s*(\S+)\s*$", block)
        checksum_m = re.search(r"(?m)^\s+sha256_checksum:\s*(\S+)\s*$", block)
        typ = type_m.group(1) if type_m else "unknown"
        status = status_m.group(1) if status_m else "unknown"
        checksum = checksum_m.group(1) if checksum_m else None
        if checksum == "null":
            checksum = None
        deps: list[dict[str, str]] = []
        for dm in re.finditer(
            r"(?m)^\s+- target_name:\s*[\"']?([^\"'\n]+)[\"']?\s*$\n\s+relationship_type:\s*(\S+)",
            block,
        ):
            deps.append(
                {"target_name": dm.group(1).strip(), "relationship_type": dm.group(2).strip()}
            )
        members.append(
            ManifestMember(
                path=path,
                type=typ,
                status=status,
                checksum=checksum,
                dependencies=deps,
            )
        )
    return members


class DependencyResolver:
    def __init__(self, members: list[ManifestMember]):
        self.members = members
        self.by_name = {m.name: m for m in members}
        # Also index by basename for path-style names
        for m in members:
            self.by_name.setdefault(Path(m.path).name.upper(), m)

    def buildable_members(self) -> list[ManifestMember]:
        return [m for m in self.members if m.buildable]

    def resolve(self) -> tuple[list[ManifestMember], list[str]]:
        """Return (ordered members, warnings). Raises ValueError on cycles."""
        buildable = self.buildable_members()
        nodes = {m.path: m for m in buildable}
        # Graph: edge A->B means A must come before B (B depends on A)
        preds: dict[str, set[str]] = {p: set() for p in nodes}
        succs: dict[str, set[str]] = defaultdict(set)
        warnings: list[str] = []

        name_to_path = {m.name: m.path for m in buildable}

        for m in buildable:
            for dep in m.dependencies:
                target = dep["target_name"].upper().strip()
                if not target or target in {"(NONE)", "NONE", "NOTE:", "-", "DOES", "NOT", "CALL"}:
                    continue
                # Skip noisy prologue parse artifacts
                if not re.match(r"^[A-Z0-9_]+$", target):
                    continue
                dep_member = self.by_name.get(target)
                if dep_member is None:
                    warnings.append(f"unresolvable dependency: {m.name} -> {target}")
                    continue
                if dep_member.status == "missing-callee":
                    warnings.append(f"missing-callee skipped: {m.name} -> {target}")
                    continue
                if not dep_member.buildable:
                    warnings.append(f"non-buildable dependency skipped: {m.name} -> {target}")
                    continue
                # dep must compile before m
                preds[m.path].add(dep_member.path)
                succs[dep_member.path].add(m.path)

        # Tier edges: ensure DDS before COBOL before CL even without explicit deps
        by_tier: dict[int, list[ManifestMember]] = defaultdict(list)
        for m in buildable:
            by_tier[TYPE_TIER.get(m.type, 9)].append(m)
        tiers = sorted(by_tier)
        for i in range(len(tiers) - 1):
            for earlier in by_tier[tiers[i]]:
                for later in by_tier[tiers[i + 1]]:
                    # soft ordering via edge earlier -> later
                    if later.path not in preds[earlier.path]:
                        preds[later.path].add(earlier.path)
                        succs[earlier.path].add(later.path)

        # Kahn topological sort with stable name order
        indeg = {p: len(preds[p]) for p in nodes}
        ready = deque(sorted([p for p, d in indeg.items() if d == 0], key=lambda p: nodes[p].name))
        ordered: list[ManifestMember] = []
        while ready:
            p = ready.popleft()
            ordered.append(nodes[p])
            for s in sorted(succs[p], key=lambda x: nodes[x].name):
                indeg[s] -= 1
                if indeg[s] == 0:
                    ready.append(s)
            ready = deque(sorted(ready, key=lambda x: nodes[x].name))

        if len(ordered) != len(nodes):
            remaining = [nodes[p].name for p, d in indeg.items() if d > 0]
            raise ValueError(
                f"Circular dependency detected among members: {', '.join(sorted(remaining))}"
            )
        return ordered, warnings


def write_compile_order(ordered: list[ManifestMember], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    lines = [f"{i+1:04d}  {m.type:<8}  {m.path}" for i, m in enumerate(ordered)]
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    ap = argparse.ArgumentParser(description="Resolve COBOL/DDS/CL compile order")
    ap.add_argument("--manifest", default="manifest/pcis-manifest.yaml")
    ap.add_argument("--output", default="build/reports/compile-order.txt")
    args = ap.parse_args()
    text = Path(args.manifest).read_text(encoding="utf-8")
    members = parse_manifest_files(text)
    resolver = DependencyResolver(members)
    ordered, warnings = resolver.resolve()
    for w in warnings:
        print(f"WARN: {w}", file=sys.stderr)
    write_compile_order(ordered, Path(args.output))
    for i, m in enumerate(ordered, 1):
        print(f"{i:04d}  {m.type:<8}  {m.path}")
    print(f"Resolved {len(ordered)} buildable members -> {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
