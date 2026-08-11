#!/usr/bin/env bash
# Orchestrator: seed → set reference date → submit COBOL job → collect → normalize.
# IBM i execution is isolated from normalization so CI can test normalize/compare only.
set -euo pipefail

PROGRAM="${1:-}"
SCENARIO="${2:-}"
REF_DATE="${PCIS_REFERENCE_DATE:-2024-06-15}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPTS="${ROOT}/scripts"
RAW_ROOT="${PCIS_RAW_CAPTURE_ROOT:-/tmp/pcis-golden-raw}"
OUT_DIR="${ROOT}/${PROGRAM}/${SCENARIO}"

usage() {
  echo "Usage: $0 <PROGRAM> <SCENARIO>" >&2
  echo "  Env: PCIS_IBMI_*, PCIS_REFERENCE_DATE (default ${REF_DATE})" >&2
  echo "  Example: PCIS_REFERENCE_DATE=2024-06-15 $0 CLM006B scenario-01" >&2
  exit 2
}

[[ -n "$PROGRAM" && -n "$SCENARIO" ]] || usage

RAW_DIR="${RAW_ROOT}/${PROGRAM}/${SCENARIO}/$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$RAW_DIR" "$OUT_DIR/tables"

echo "============================================================"
echo " Golden capture: ${PROGRAM}/${SCENARIO}"
echo " Reference date: ${REF_DATE}"
echo " Raw dir:        ${RAW_DIR}"
echo " Golden dir:     ${OUT_DIR}"
echo "============================================================"

export PCIS_REFERENCE_DATE="$REF_DATE"

"${SCRIPTS}/seed_data.sh" "$PROGRAM" "$SCENARIO"

# Inject reference date (Db2 for i QAQQINI / job date). Site-specific.
if [[ "${PCIS_CAPTURE_DRY_RUN:-0}" != "1" ]]; then
  if command -v system >/dev/null 2>&1; then
    # CHGJOB DATE requires *MDY or ISO depending on DATFMT — operators adjust.
    system "SBMJOB CMD(CALL PGM(${PROGRAM})) JOB(${PROGRAM}) JOBQ(QBATCH)" || {
      echo "ERROR: SBMJOB failed for ${PROGRAM}" >&2
      exit 1
    }
    # Wait for job — operators may replace with more robust polling.
    sleep "${PCIS_JOB_WAIT_SECONDS:-5}"
  else
    echo "WARNING: IBM i 'system' CLI unavailable — skipping SBMJOB (dry collection only)." >&2
  fi
fi

"${SCRIPTS}/collect_outputs.sh" "$PROGRAM" "$SCENARIO" "$RAW_DIR"

# Normalize into the committed golden path (tables/, display.txt, …)
python3 "${SCRIPTS}/normalize.py" "$RAW_DIR" "$OUT_DIR"

echo "Capture complete. Normalized artifacts at ${OUT_DIR}"
echo "Next: ${SCRIPTS}/verify_determinism.sh ${PROGRAM} ${SCENARIO}"
