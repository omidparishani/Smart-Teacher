-- ═══════════════════════════════════════════════════════════
--  V5 — حسابرسی، تنظیمات سامانه، جاب‌ها
-- ═══════════════════════════════════════════════════════════

-- ─────────────── audit_logs ───────────────
-- ⚠️ این جدول فقط-افزودنی است و این محدودیت در سطح دیتابیس
-- اعمال می‌شود (پایین‌تر: تریگر prevent_audit_mutation).
-- دلیل: لاگ حسابرسی‌ای که قابل ویرایش باشد، حسابرسی نیست.
CREATE TABLE audit_logs (
    id            UUID         PRIMARY KEY,
    tenant_id     UUID         NULL,
    actor_user_id UUID         NULL,
    actor_type    VARCHAR(20)  NOT NULL DEFAULT 'USER',
    action        VARCHAR(100) NOT NULL,
    entity_type   VARCHAR(100) NULL,
    entity_id     UUID         NULL,
    before_state  JSONB        NULL,
    after_state   JSONB        NULL,
    metadata      JSONB        NULL,
    ip_address    VARCHAR(45)  NULL,
    user_agent    VARCHAR(500) NULL,
    trace_id      VARCHAR(64)  NULL,
    severity      VARCHAR(20)  NOT NULL DEFAULT 'INFO',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_audit_severity   CHECK (severity   IN ('INFO','WARNING','CRITICAL')),
    CONSTRAINT chk_audit_actor_type CHECK (actor_type IN ('USER','SYSTEM','API')),
    -- RESTRICT عمدی: حذف مستأجر یا کاربر نباید لاگ حسابرسی را دست بزند
    CONSTRAINT fk_audit_tenant FOREIGN KEY (tenant_id)     REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_audit_actor  FOREIGN KEY (actor_user_id) REFERENCES users(id)   ON DELETE RESTRICT
);

CREATE INDEX idx_audit_logs_tenant_created ON audit_logs (tenant_id, created_at DESC);
CREATE INDEX idx_audit_logs_created        ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_action         ON audit_logs (action, created_at DESC);
CREATE INDEX idx_audit_logs_actor          ON audit_logs (actor_user_id, created_at DESC);
CREATE INDEX idx_audit_logs_entity         ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_trace          ON audit_logs (trace_id);
-- رویدادهای مهم کم‌تعدادند؛ ایندکس جزئی سبک‌تر و سریع‌تر است
CREATE INDEX idx_audit_logs_critical ON audit_logs (severity, created_at DESC)
    WHERE severity IN ('WARNING','CRITICAL');

-- Append-Only در سطح دیتابیس
CREATE OR REPLACE FUNCTION prevent_audit_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs فقط-افزودنی است؛ ویرایش و حذف مجاز نیست'
        USING ERRCODE = 'restrict_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_logs_append_only
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_mutation();

-- ─────────────── system_settings ───────────────
CREATE TABLE system_settings (
    id          UUID         PRIMARY KEY,
    key         VARCHAR(200) NOT NULL,
    value       TEXT         NULL,
    value_type  VARCHAR(20)  NOT NULL DEFAULT 'STRING',
    category    VARCHAR(50)  NULL,
    -- اگر true باشد، مقدار با AES-256-GCM رمزنگاری‌شده ذخیره می‌شود
    encrypted   BOOLEAN      NOT NULL DEFAULT FALSE,
    description VARCHAR(500) NULL,
    updated_by  UUID         NULL,

    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_system_settings_key UNIQUE (key),
    CONSTRAINT chk_settings_value_type CHECK (value_type IN ('STRING','INT','BOOLEAN','JSON')),
    CONSTRAINT fk_settings_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_system_settings_category ON system_settings (category);

-- ─────────────── scheduler_jobs ───────────────
CREATE TABLE scheduler_jobs (
    id                   UUID         PRIMARY KEY,
    job_name             VARCHAR(100) NOT NULL,
    job_group            VARCHAR(100) NULL,
    cron_expression      VARCHAR(100) NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'RUNNING',
    last_run_at          TIMESTAMPTZ  NULL,
    next_run_at          TIMESTAMPTZ  NULL,
    last_duration_ms     INTEGER      NULL,
    last_status          VARCHAR(20)  NULL,
    last_error           TEXT         NULL,
    consecutive_failures INTEGER      NOT NULL DEFAULT 0,

    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version              BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_scheduler_jobs_name   UNIQUE (job_name),
    CONSTRAINT chk_scheduler_status     CHECK (status IN ('RUNNING','PAUSED')),
    CONSTRAINT chk_scheduler_last_status CHECK (last_status IS NULL OR last_status IN ('SUCCESS','FAILED'))
);

CREATE INDEX idx_scheduler_jobs_status ON scheduler_jobs (status);

-- ─────────────── تریگرها ───────────────
CREATE TRIGGER trg_system_settings_updated_at
    BEFORE UPDATE ON system_settings FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_scheduler_jobs_updated_at
    BEFORE UPDATE ON scheduler_jobs FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── Seed — تنظیمات پیش‌فرض سامانه ───────────────
INSERT INTO system_settings (id, key, value, value_type, category, encrypted, description) VALUES
('50000000-0000-7000-8000-000000000001', 'platform.name',                  'دستیار هوشمند معلم', 'STRING',  'GENERAL',  FALSE, 'نام نمایشی پلتفرم'),
('50000000-0000-7000-8000-000000000002', 'platform.default_locale',        'fa',                 'STRING',  'GENERAL',  FALSE, 'زبان پیش‌فرض'),
('50000000-0000-7000-8000-000000000003', 'platform.default_timezone',      'Asia/Tehran',        'STRING',  'GENERAL',  FALSE, 'منطقه زمانی پیش‌فرض'),
('50000000-0000-7000-8000-000000000004', 'registration.auto_create_tenant','true',               'BOOLEAN', 'GENERAL',  FALSE, 'ایجاد خودکار مستأجر شخصی هنگام نخستین ورود'),
('50000000-0000-7000-8000-000000000005', 'security.eitaa_remote_verify',   'false',              'BOOLEAN', 'SECURITY', FALSE, 'اعتبارسنجی از راه دور initData ایتا — پیش‌فرض خاموش'),
('50000000-0000-7000-8000-000000000006', 'security.init_data_max_age_seconds', '300',            'INT',     'SECURITY', FALSE, 'حداکثر عمر مجاز initData (ضد Replay)');
