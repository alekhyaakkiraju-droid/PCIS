#!/usr/bin/env bash
# Lightweight shell checks for Dockerfile required strings (no Docker daemon).
set -euo pipefail
DF="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/Dockerfile"
fail=0
require() {
  local pat="$1"
  if ! grep -Eq "$pat" "$DF"; then
    echo "FAIL: expected pattern /$pat/ in $DF"
    fail=1
  else
    echo "OK: /$pat/"
  fi
}
require 'distroless'
require 'java21'
require 'USER[[:space:]]+nonroot'
require 'javaagent'
require 'SHA256|sha256'
require 'gcr\.io/distroless/java21-debian12'
if [[ "$fail" -ne 0 ]]; then
  exit 1
fi
echo "All Dockerfile content checks passed."
