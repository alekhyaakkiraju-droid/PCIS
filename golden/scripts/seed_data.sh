#!/usr/bin/env bash
# Restore scenario seed data into Db2 for i (or JDBC URL).
# Separates IBM i execution from normalization (CI-safe seeds are SQL files only).
set -euo pipefail

PROGRAM="${1:-}"
SCENARIO="${2:-}"
JDBC_URL="${PCIS_IBMI_JDBC_URL:-}"
DB_USER="${PCIS_IBMI_USER:-}"
DB_PASS="${PCIS_IBMI_PASSWORD:-}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SEED_DIR="${ROOT}/seeds/${PROGRAM}"

usage() {
  echo "Usage: $0 <PROGRAM> <SCENARIO>" >&2
  echo "  Requires PCIS_IBMI_JDBC_URL, PCIS_IBMI_USER, PCIS_IBMI_PASSWORD for live IBM i." >&2
  echo "  Dry-run (list scripts): PCIS_SEED_DRY_RUN=1 $0 <PROGRAM> <SCENARIO>" >&2
  exit 2
}

[[ -n "$PROGRAM" && -n "$SCENARIO" ]] || usage
[[ -d "$SEED_DIR" ]] || { echo "Missing seed dir: $SEED_DIR" >&2; exit 1; }

COMMON="${SEED_DIR}/_common.sql"
SCENARIO_SQL="${SEED_DIR}/${SCENARIO}.sql"
[[ -f "$SCENARIO_SQL" ]] || { echo "Missing seed SQL: $SCENARIO_SQL" >&2; exit 1; }

echo "==> Seeding ${PROGRAM}/${SCENARIO}"

if [[ "${PCIS_SEED_DRY_RUN:-0}" == "1" ]]; then
  [[ -f "$COMMON" ]] && echo "would-run: $COMMON"
  echo "would-run: $SCENARIO_SQL"
  exit 0
fi

if [[ -z "$JDBC_URL" || -z "$DB_USER" ]]; then
  echo "ERROR: IBM i connection not configured. Export PCIS_IBMI_JDBC_URL / PCIS_IBMI_USER / PCIS_IBMI_PASSWORD." >&2
  echo "Seed SQL is ready at: $SCENARIO_SQL" >&2
  exit 1
fi

# Prefer clj/jdbc tools if present; otherwise require ibm_db / jdbc runner.
RUNNER="${PCIS_SQL_RUNNER:-}"
if [[ -z "$RUNNER" ]]; then
  if command -v sqlcmd >/dev/null 2>&1; then
    RUNNER="sqlcmd"
  else
    echo "ERROR: Set PCIS_SQL_RUNNER to a JDBC/CLI that can execute Db2 for i SQL." >&2
    exit 1
  fi
fi

run_sql() {
  local file="$1"
  echo "exec: $file"
  # Placeholder invocation — operators wire their site JDBC wrapper here.
  "$RUNNER" --url "$JDBC_URL" --user "$DB_USER" --password "$DB_PASS" --file "$file"
}

[[ -f "$COMMON" ]] && run_sql "$COMMON"
run_sql "$SCENARIO_SQL"
echo "Seed complete: ${PROGRAM}/${SCENARIO}"
