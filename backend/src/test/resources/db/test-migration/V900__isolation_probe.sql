-- ═══════════════════════════════════════════════════════════
--  V900 — جدول کاوشگر جداسازی مستأجر  ⚠️ فقط تست
--
--  این فایل در src/test/resources است، پس هرگز روی classpath
--  production نمی‌آید و هرگز در دیتابیس واقعی اجرا نمی‌شود.
--
--  ── چرا Migration و نه @Sql؟ ──
--  چون spring.jpa.hibernate.ddl-auto = validate است و اعتبارسنجی
--  هنگام بالا آمدن کانتکست اجرا می‌شود — یعنی پیش از آنکه @Sql
--  فرصت اجرا پیدا کند. اگر جدول از راه @Sql ساخته می‌شد، اعتبارسنجی
--  شکست می‌خورد و تست هرگز شروع نمی‌شد.
--
--  ── چرا این جدول لازم است؟ ──
--  برای آزمودن قید جداسازی به یک موجودیت واقعی با @TenantId نیاز
--  داریم. استفاده از جدول‌های production (مثل tenants) دو ایراد داشت:
--  آن‌ها یا tenant_id ندارند، یا حذف‌شان در تست به داده واقعی آسیب
--  می‌زد. یک جدول بی‌طرف، تست را از مدل دامنه جدا می‌کند.
--
--  ⚠️ عمداً هیچ Foreign Key روی tenant_id نیست: تست باید بتواند
--  مستأجرهای ساختگی بدون ساختن رکورد در users/tenants بسازد.
-- ═══════════════════════════════════════════════════════════

CREATE TABLE isolation_probe (
    id         UUID         PRIMARY KEY,
    tenant_id  UUID         NOT NULL,
    label      VARCHAR(100) NOT NULL,

    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by UUID         NULL,
    updated_by UUID         NULL,
    version    BIGINT       NOT NULL DEFAULT 0
);

-- ایندکس روی tenant_id تا کوئری‌های فیلترشده در تست به جدول‌پیمایی
-- کامل تبدیل نشوند (و همان الگویی که در جدول‌های واقعی داریم).
CREATE INDEX idx_isolation_probe_tenant ON isolation_probe (tenant_id);
