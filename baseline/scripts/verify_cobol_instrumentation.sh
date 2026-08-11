#!/usr/bin/env bash
# WO-237: Characterize COBOL run-log timing instrumentation in all six batch programs.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SRC="${ROOT}/Property_Casualty_Insurance_System"
PROGRAMS=(AUD002B BIL003B CLM006B CMM001B POL006B PRM005B)
FAIL=0

echo "Verifying RPT_RUN_LOG_T timing instrumentation in six batch programs..."

for pgm in "${PROGRAMS[@]}"; do
  file="${SRC}/${pgm}.cbl"
  if [[ ! -f "$file" ]]; then
    echo "FAIL: missing source $file"
    FAIL=1
    continue
  fi

  for pattern in WS-START-TIMESTAMP WS-END-TIMESTAMP; do
    if ! grep -q "$pattern" "$file"; then
      echo "FAIL: ${pgm} missing ${pattern}"
      FAIL=1
    fi
  done

  if ! grep -q "8000-WRITE-RUN-LOG" "$file"; then
    echo "FAIL: ${pgm} missing 8000-WRITE-RUN-LOG paragraph"
    FAIL=1
  fi

  if ! grep -q "INSERT INTO RPT_RUN_LOG_T" "$file"; then
    echo "FAIL: ${pgm} missing INSERT INTO RPT_RUN_LOG_T"
    FAIL=1
  fi

  # Timing columns must appear in the INSERT column list (not only in WORKING-STORAGE).
  insert_block="$(awk '/INSERT INTO RPT_RUN_LOG_T/,/END-EXEC/' "$file" || true)"
  if ! grep -q "START_TIMESTAMP" <<<"$insert_block"; then
    echo "FAIL: ${pgm} INSERT missing START_TIMESTAMP"
    FAIL=1
  fi
  if ! grep -q "END_TIMESTAMP" <<<"$insert_block"; then
    echo "FAIL: ${pgm} INSERT missing END_TIMESTAMP"
    FAIL=1
  fi

  if grep -q "PERFORM 8000-WRITE-RUN-LOG" "$file"; then
    :
  else
    echo "FAIL: ${pgm} does not PERFORM 8000-WRITE-RUN-LOG"
    FAIL=1
  fi
done

# PRM005B must include REC_DELINQUENT in its INSERT.
prm_insert="$(awk '/INSERT INTO RPT_RUN_LOG_T/,/END-EXEC/' "${SRC}/PRM005B.cbl" || true)"
if ! grep -q "REC_DELINQUENT" <<<"$prm_insert"; then
  echo "FAIL: PRM005B INSERT missing REC_DELINQUENT"
  FAIL=1
fi

if [[ "$FAIL" -ne 0 ]]; then
  echo "FAIL: COBOL instrumentation verification failed"
  exit 1
fi

echo "OK: all six programs instrumented with START_TIMESTAMP/END_TIMESTAMP run-log INSERT"
exit 0
