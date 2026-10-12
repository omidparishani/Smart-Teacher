-- ═══════════════════════════════════════════════════════════
--  V27 — فاز ۱۲: ستون‌های TOTP برای 2FA سوپرادمین
--
--  totp_secret رمزنگاری‌شده در لایهٔ اپ (EncryptedStringConverter)
--  ذخیره می‌شود؛ این ستون فقط ciphertext نگه می‌دارد.
--  partitioning برای ai_usage_records در docs/runbook.md مستند شده
--  (تبدیل بدون قطع تست‌ها در این گام انجام نمی‌شود).
-- ═══════════════════════════════════════════════════════════

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS totp_secret  TEXT    NULL,
    ADD COLUMN IF NOT EXISTS totp_enabled BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN users.totp_secret IS 'راز TOTP رمزنگاری‌شده (AES-GCM) — هرگز خام';
COMMENT ON COLUMN users.totp_enabled IS 'آیا 2FA برای این کاربر فعال است';
