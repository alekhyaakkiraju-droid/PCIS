#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/frontend"

echo "==> WCAG CI gate: vitest route-level axe checks"
npm run test:a11y

echo "==> WCAG CI gate passed"
