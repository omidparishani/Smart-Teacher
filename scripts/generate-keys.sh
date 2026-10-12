#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
#  تولید کلیدهای امنیتی پروژه
#  اجرا: bash scripts/generate-keys.sh
# ─────────────────────────────────────────────────────────────
set -euo pipefail

if ! command -v openssl >/dev/null 2>&1; then
  echo "خطا: openssl یافت نشد. در ویندوز از دستور PowerShell در docs/development.md بخش ۶ استفاده کنید." >&2
  exit 1
fi

echo "# این خطوط را در فایل .env قرار دهید"
echo "ENCRYPTION_MASTER_KEY=$(openssl rand -base64 32)"
echo "JWT_SECRET=$(openssl rand -base64 48)"
