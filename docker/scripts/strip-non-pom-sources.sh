#!/usr/bin/env bash
# Keep only pom.xml files under shared-libs/ and services/ for Maven reactor caching.
set -euo pipefail
find shared-libs services -type f ! -name 'pom.xml' -delete
find shared-libs services -depth -type d -empty -exec rmdir {} + 2>/dev/null || true
