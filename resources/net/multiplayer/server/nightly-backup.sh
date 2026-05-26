#!/usr/bin/env bash
set -euo pipefail

DB_PATH="${1:-multiplayer.db}"
OUT_DIR="${2:-backups}"
STAMP="$(date +%F)"
WEEKDAY="$(date +%u)" # 1=Mon

mkdir -p "${OUT_DIR}"

DAILY_FILE="${OUT_DIR}/daily-${STAMP}.sqlite.gz"
gzip -c "${DB_PATH}" > "${DAILY_FILE}"

ls -1t "${OUT_DIR}"/daily-*.sqlite.gz 2>/dev/null | tail -n +8 | xargs -r rm -f

if [[ "${WEEKDAY}" == "1" ]]; then
  WEEK_FILE="${OUT_DIR}/weekly-${STAMP}.sqlite.gz"
  cp "${DAILY_FILE}" "${WEEK_FILE}"
fi

ls -1t "${OUT_DIR}"/weekly-*.sqlite.gz 2>/dev/null | tail -n +5 | xargs -r rm -f
