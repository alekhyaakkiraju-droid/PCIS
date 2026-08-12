#!/usr/bin/env bash
# Package local Helm subchart dependencies (e.g. pcis-common) before install.
set -euo pipefail

CHART_PATH="${1:?Usage: prepare-chart.sh <path-to-chart>}"
helm dependency build "${CHART_PATH}"
