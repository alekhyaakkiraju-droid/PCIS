#!/usr/bin/env bash
# Collect DISPLAY spool + mutated table CSVs after a COBOL job completes.
set -euo pipefail

PROGRAM="${1:-}"
SCENARIO="${2:-}"
OUT_DIR="${3:-}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
[[ -n "$PROGRAM" && -n "$SCENARIO" && -n "$OUT_DIR" ]] || {
  echo "Usage: $0 <PROGRAM> <SCENARIO> <OUT_DIR>" >&2
  exit 2
}

mkdir -p "${OUT_DIR}/tables"

# Job name / spool conventions — override via env for site-specific naming.
JOB_NAME="${PCIS_JOB_NAME:-${PROGRAM}}"
SPLF_NAME="${PCIS_SPLF_NAME:-QPRINT}"
LIBRARY="${PCIS_IBMI_LIBRARY:-PCISLIB}"

echo "==> Collecting outputs for ${PROGRAM}/${SCENARIO} → ${OUT_DIR}"

if [[ "${PCIS_COLLECT_DRY_RUN:-0}" == "1" ]]; then
  echo "would-collect-spool: JOB=${JOB_NAME} SPLF=${SPLF_NAME}"
  echo "would-query-tables into ${OUT_DIR}/tables/"
  exit 0
fi

# --- DISPLAY / spool -------------------------------------------------------
if command -v system >/dev/null 2>&1; then
  # IBM i PASE: copy spool to IFS then to OUT_DIR
  TMP_SPLF="/tmp/${PROGRAM}_${SCENARIO}_display.txt"
  system "CPYSPLF FILE(${SPLF_NAME}) TOFILE(*TOSTMF) JOB(*) SPLNBR(*LAST) TOSTMF('${TMP_SPLF}')" || true
  if [[ -f "$TMP_SPLF" ]]; then
    cp "$TMP_SPLF" "${OUT_DIR}/display.txt"
  else
    echo "WARNING: spool not found; writing empty display.txt" >&2
    : > "${OUT_DIR}/display.txt"
  fi
else
  echo "WARNING: not on IBM i; expect display.txt to be provided by capture shim" >&2
  [[ -f "${OUT_DIR}/display.txt" ]] || : > "${OUT_DIR}/display.txt"
fi

# --- Table exports (full NUMERIC precision via CHAR/DECIMAL casting) --------
# Site JDBC runner must honor column order and avoid float casts.
TABLES_FILE="${ROOT}/seeds/${PROGRAM}/mutated_tables.txt"
if [[ -f "$TABLES_FILE" ]]; then
  while IFS= read -r table || [[ -n "$table" ]]; do
    [[ -z "$table" || "$table" =~ ^# ]] && continue
    echo "export table: $table"
    # Operators replace with: SELECT ... FROM ${LIBRARY}.${table} ORDER BY ...
    echo "TABLE,PLACEHOLDER" > "${OUT_DIR}/tables/${table}.csv"
  done < "$TABLES_FILE"
fi

# --- Run log ---------------------------------------------------------------
cat > "${OUT_DIR}/run_log.csv" <<EOF
PROGRAM_NAME,STATUS,ROWS_PROCESSED,RUN_STARTED,RUN_ENDED
${PROGRAM},UNKNOWN,0,$(date -u +%Y-%m-%dT%H:%M:%SZ),$(date -u +%Y-%m-%dT%H:%M:%SZ)
EOF

cat > "${OUT_DIR}/metadata.yaml" <<EOF
program: ${PROGRAM}
scenario: ${SCENARIO}
library: ${LIBRARY}
collected_at: $(date -u +%Y-%m-%dT%H:%M:%SZ)
reference_date: ${PCIS_REFERENCE_DATE:-}
completion_status: UNKNOWN
notes: Raw capture prior to normalize.py
EOF

echo "Collection complete: ${OUT_DIR}"
