#!/usr/bin/env bash
# WO-287/288/289: Apply cross-service fixture seed via Flyway repeatable migration.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SQL="${ROOT}/shared-libs/pcis-schema/db/migration/R__cross_service_fixtures.sql"

usage() {
  cat <<'EOF'
Usage: scripts/seed-cross-service-fixtures.sh [--jdbc-url URL] [--user USER] [--password PASS]

Applies the idempotent cross-service fixture SQL (R__cross_service_fixtures.sql).
When JDBC settings are omitted, runs Flyway migrate against pcis-schema migrations.

Environment:
  PCIS_DB_URL       JDBC URL (default: jdbc:postgresql://localhost:5432/pcis)
  PCIS_DB_USER      Database user (default: pcis)
  PCIS_DB_PASSWORD  Database password (default: pcis)
EOF
}

JDBC_URL="${PCIS_DB_URL:-jdbc:postgresql://localhost:5432/pcis}"
DB_USER="${PCIS_DB_USER:-pcis}"
DB_PASSWORD="${PCIS_DB_PASSWORD:-pcis}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --jdbc-url)
      JDBC_URL="$2"
      shift 2
      ;;
    --user)
      DB_USER="$2"
      shift 2
      ;;
    --password)
      DB_PASSWORD="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "ERROR: unknown option: $1" >&2
      usage
      exit 2
      ;;
  esac
done

if ! command -v psql >/dev/null 2>&1; then
  echo "ERROR: psql is required to apply ${SQL}" >&2
  exit 1
fi

echo "==> Applying cross-service fixtures from ${SQL}"
PGPASSWORD="${DB_PASSWORD}" psql "${JDBC_URL#jdbc:}" -v ON_ERROR_STOP=1 -U "${DB_USER}" -f "${SQL}"
echo "==> Cross-service fixtures applied"
