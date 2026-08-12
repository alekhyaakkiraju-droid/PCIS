#!/usr/bin/env bash
# Opsera C2C deploy stage — platform bootstrap only.
# Add as a pipeline step before microservice deploys, or run once manually.
set -euo pipefail
exec "$(dirname "$0")/../../helm/bootstrap/install-dev-platform.sh"
