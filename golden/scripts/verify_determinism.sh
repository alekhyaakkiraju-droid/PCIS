#!/usr/bin/env bash
# Run capture three times; quarantine if normalized artifacts are not byte-identical.
set -euo pipefail

PROGRAM="${1:-}"
SCENARIO="${2:-}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPTS="${ROOT}/scripts"
QUAR="${ROOT}/quarantine/${PROGRAM}/${SCENARIO}"

[[ -n "$PROGRAM" && -n "$SCENARIO" ]] || {
  echo "Usage: $0 <PROGRAM> <SCENARIO>" >&2
  exit 2
}

WORKDIR="${PCIS_DETERMINISM_WORKDIR:-/tmp/pcis-golden-determinism}/${PROGRAM}/${SCENARIO}"
rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"

echo "==> Determinism check (3 runs): ${PROGRAM}/${SCENARIO}"

for i in 1 2 3; do
  export PCIS_RAW_CAPTURE_ROOT="${WORKDIR}/raw-run-${i}"
  # Capture writes into golden/<PROG>/<SCENARIO>; snapshot after each run.
  "${SCRIPTS}/capture.sh" "$PROGRAM" "$SCENARIO"
  mkdir -p "${WORKDIR}/norm-${i}"
  cp -R "${ROOT}/${PROGRAM}/${SCENARIO}/." "${WORKDIR}/norm-${i}/"
done

fail=0
for pair in "1 2" "2 3" "1 3"; do
  set -- $pair
  a=$1; b=$2
  if ! python3 "${SCRIPTS}/compare.py" "${WORKDIR}/norm-${a}" "${WORKDIR}/norm-${b}"; then
    fail=1
    mkdir -p "$QUAR"
    python3 "${SCRIPTS}/compare.py" "${WORKDIR}/norm-${a}" "${WORKDIR}/norm-${b}" \
      > "${QUAR}/diff-run${a}-vs-run${b}.txt" 2>&1 || true
    cp -R "${WORKDIR}/norm-${a}" "${QUAR}/run-${a}"
    cp -R "${WORKDIR}/norm-${b}" "${QUAR}/run-${b}"
    cat > "${QUAR}/REASON.txt" <<EOF
Non-deterministic golden for ${PROGRAM}/${SCENARIO}
Pair run-${a} vs run-${b} differed. Artifacts quarantined; do not commit as golden.
Captured at: $(date -u +%Y-%m-%dT%H:%M:%SZ)
EOF
  fi
done

if [[ "$fail" -ne 0 ]]; then
  echo "FAIL: quarantined under ${QUAR}" >&2
  exit 1
fi

echo "PASS: three runs byte-identical for ${PROGRAM}/${SCENARIO}"
