#!/usr/bin/env bash
# WO-238: Static syntax checks for baseline SQL measurement scripts.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SCRIPT_DIR="${ROOT}/baseline/scripts"
FAIL=0

check_balanced_parens() {
  local file="$1"
  local depth=0
  while IFS= read -r line || [[ -n "$line" ]]; do
    # strip single-quoted strings to avoid false positives
    local stripped
    stripped="$(sed "s/'[^']*'//g" <<<"$line")"
    local chars
    chars="$(grep -o '[()]' <<<"$stripped" 2>/dev/null || true)"
    for ch in $chars; do
      if [[ "$ch" == "(" ]]; then
        depth=$((depth + 1))
      else
        depth=$((depth - 1))
        if [[ "$depth" -lt 0 ]]; then
          echo "FAIL: ${file} — unbalanced parentheses (extra ')')"
          return 1
        fi
      fi
    done
  done < "$file"
  if [[ "$depth" -ne 0 ]]; then
    echo "FAIL: ${file} — unbalanced parentheses (depth=${depth})"
    return 1
  fi
  return 0
}

validate_sql_file() {
  local file="$1"
  local base
  base="$(basename "$file")"

  if [[ ! -s "$file" ]]; then
    echo "FAIL: ${base} is empty"
    return 1
  fi

  if ! grep -qiE '^\s*--' "$file"; then
    echo "FAIL: ${base} missing header comment"
    return 1
  fi

  if ! grep -qiE '\bSELECT\b' "$file"; then
    echo "FAIL: ${base} missing SELECT"
    return 1
  fi

  if ! grep -qiE '\bFROM\b' "$file"; then
    echo "FAIL: ${base} missing FROM"
    return 1
  fi

  if ! tail -n 5 "$file" | grep -q ';'; then
    echo "FAIL: ${base} must terminate with semicolon"
    return 1
  fi

  if grep -qiE '\bSELCT\b|\bFORM\b|\bWHER\b' "$file"; then
    echo "FAIL: ${base} contains likely SQL typo"
    return 1
  fi

  if ! check_balanced_parens "$file"; then
    return 1
  fi

  case "$base" in
    measure_table_volumes.sql)
      local union_count
      union_count="$(grep -c 'UNION ALL' "$file" || true)"
      if [[ "$union_count" -ne 54 ]]; then
        echo "FAIL: ${base} expected 54 UNION ALL (55 tables), got ${union_count}"
        return 1
      fi
      if ! grep -q 'INSPRDDTA\.' "$file"; then
        echo "FAIL: ${base} must reference INSPRDDTA schema"
        return 1
      fi
      if ! grep -q 'CRT_TIMESTAMP' "$file"; then
        echo "FAIL: ${base} must measure CRT_TIMESTAMP"
        return 1
      fi
      ;;
    measure_batch_windows.sql)
      if ! grep -q 'INSPRDDTA.RPT_RUN_LOG_T' "$file"; then
        echo "FAIL: ${base} must query INSPRDDTA.RPT_RUN_LOG_T"
        return 1
      fi
      for col in START_TIMESTAMP END_TIMESTAMP; do
        if ! grep -q "$col" "$file"; then
          echo "FAIL: ${base} missing ${col}"
          return 1
        fi
      done
      for stat in AVG MAX MIN P95; do
        if ! grep -qi "$stat" "$file"; then
          echo "FAIL: ${base} missing ${stat} duration statistic"
          return 1
        fi
      done
      if ! grep -q 'REC_ERRORS' "$file"; then
        echo "FAIL: ${base} must include REC_ERRORS"
        return 1
      fi
      ;;
    verify_run_log_timing.sql)
      if ! grep -q 'RPT_RUN_LOG_T' "$file"; then
        echo "FAIL: ${base} must reference RPT_RUN_LOG_T"
        return 1
      fi
      ;;
  esac

  echo "OK: ${base}"
  return 0
}

echo "==> WO-238 baseline SQL syntax validation"

shopt -s nullglob
sql_files=("${SCRIPT_DIR}"/*.sql)
if [[ ${#sql_files[@]} -eq 0 ]]; then
  echo "FAIL: no SQL files in ${SCRIPT_DIR}"
  exit 1
fi

for f in "${sql_files[@]}"; do
  if ! validate_sql_file "$f"; then
    FAIL=1
  fi
done

if [[ "$FAIL" -ne 0 ]]; then
  echo "FAIL: SQL syntax validation failed"
  exit 1
fi

echo "OK: all ${#sql_files[@]} baseline SQL scripts passed static checks"
exit 0
