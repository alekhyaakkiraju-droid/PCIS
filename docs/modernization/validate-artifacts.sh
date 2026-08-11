#!/usr/bin/env bash
# Validate modernization artifact completeness (WO-006)
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FAIL=0

# filename:min_lines
THRESHOLDS=(
  "Intent_Profile.md:150"
  "Architecture_Options.md:1000"
  "PRD-Spec.md:500"
  "Requirements_Traceability.md:15"
  "User_Stories.md:500"
  "UI_Design.md:200"
  "Testing.md:1000"
  "README.md:30"
)

echo "Validating modernization artifacts in ${DIR}"

for entry in "${THRESHOLDS[@]}"; do
  file="${entry%%:*}"
  min="${entry##*:}"
  path="${DIR}/${file}"
  if [[ ! -f "${path}" ]]; then
    echo "FAIL: missing ${file}"
    FAIL=1
    continue
  fi
  if [[ ! -s "${path}" ]]; then
    echo "FAIL: ${file} is empty"
    FAIL=1
    continue
  fi
  lines=$(wc -l < "${path}" | tr -d ' ')
  if (( lines < min )); then
    echo "FAIL: ${file} has ${lines} lines (minimum ${min})"
    FAIL=1
    continue
  fi
  echo "OK: ${file} (${lines} lines, min ${min})"
done

# README must reference all seven artifacts
README="${DIR}/README.md"
for art in Intent_Profile.md Architecture_Options.md PRD-Spec.md Requirements_Traceability.md User_Stories.md UI_Design.md Testing.md; do
  if ! grep -q "${art}" "${README}"; then
    echo "FAIL: README.md does not reference ${art}"
    FAIL=1
  fi
done

if (( FAIL != 0 )); then
  echo "Artifact validation FAILED"
  exit 1
fi

echo "Artifact validation PASSED"
exit 0
