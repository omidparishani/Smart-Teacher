-- ═══════════════════════════════════════════════════════════
--  V19 — ایندکس‌های تجمیع گزارش/نمودار (فاز ۱۱ گام ۱)
--
--  شمارش روزانه کاربران/مستأجرها/اشتراک‌ها و جمع درآمد/مصرف AI
--  روی created_at و status تکیه دارند. ایندکس‌های موجود روی
--  tenant_id بودند؛ گزارش پلتفرم‌سطح بدون این‌ها Sequential Scan
--  می‌شود. هیچ جدول تازه‌ای ساخته نمی‌شود.
-- ═══════════════════════════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_users_created_at
    ON users (created_at)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_tenants_created_at
    ON tenants (created_at)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_subscriptions_created_at
    ON subscriptions (created_at);

CREATE INDEX IF NOT EXISTS idx_subscriptions_status
    ON subscriptions (status);

CREATE INDEX IF NOT EXISTS idx_invoices_status_created
    ON invoices (status, created_at);

CREATE INDEX IF NOT EXISTS idx_ai_usage_created
    ON ai_usage_records (created_at);
