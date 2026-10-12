-- ═══════════════════════════════════════════════════════════
--  V1 — افزونه‌ها و توابع کمکی پایه
--  فاز ۱ — زیرساخت
-- ═══════════════════════════════════════════════════════════

-- pgcrypto برای توابع کمکی رمزنگاری و gen_random_bytes
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- pg_trgm برای جست‌وجوی متنی فارسی (ILIKE سریع)
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ───────────────────────────────────────────────────────────
--  تریگر به‌روزرسانی خودکار updated_at
--
--  ⚠️ توجه: این تریگر عمداً ستون version را دست نمی‌زند.
--  ستون version توسط @Version هایبرنیت مدیریت می‌شود و اگر تریگر
--  هم آن را افزایش دهد، دو بار increment می‌شود و Optimistic Lock
--  به‌صورت کاذب شکست می‌خورد.
--
--  این تریگر فقط برای تغییراتی است که بیرون از اپلیکیشن رخ می‌دهند
--  (SQL دستی، Migration، ابزارهای دیتابیس).
-- ───────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
