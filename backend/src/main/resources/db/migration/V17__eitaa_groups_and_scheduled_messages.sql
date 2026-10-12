-- ═══════════════════════════════════════════════════════════
--  V17 — گروه‌های ایتا و پیام زمان‌بندی‌شده (گام ۱ از فاز ۹)
--
--  فقط خروجی: eitaa_groups + scheduled_messages.
--  ❌ هیچ جدول inbound / webhook نیست (docs/eitaa.md بخش ۶).
--
--  ⚠️ account_id در گام ۱ nullable است — توکن حساب در فاز بعد؛
--  ارسال از طریق MockEitaaAdapter با توکن ساختگی کار می‌کند.
-- ═══════════════════════════════════════════════════════════

-- ─────────────── eitaa_groups ───────────────
CREATE TABLE eitaa_groups (
    id                UUID         PRIMARY KEY,
    tenant_id         UUID         NOT NULL,
    account_id        UUID         NULL,
    -- یکتایی سراسری chat_id: یک گروه ایتا فقط به یک مستأجر
    eitaa_chat_id     VARCHAR(100) NOT NULL,
    title             VARCHAR(250) NULL,
    type              VARCHAR(20)  NOT NULL DEFAULT 'GROUP',
    member_count      INTEGER      NULL,
    classroom_id      UUID         NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    can_send_messages BOOLEAN      NOT NULL DEFAULT TRUE,
    last_synced_at    TIMESTAMPTZ  NULL,

    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by        UUID         NULL,
    updated_by        UUID         NULL,
    version           BIGINT       NOT NULL DEFAULT 0,
    deleted_at        TIMESTAMPTZ  NULL,

    CONSTRAINT fk_eitaa_groups_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_eitaa_groups_classroom
        FOREIGN KEY (tenant_id, classroom_id)
        REFERENCES classrooms (tenant_id, id) ON DELETE SET NULL (classroom_id),

    CONSTRAINT uk_eitaa_groups_chat_id UNIQUE (eitaa_chat_id),
    CONSTRAINT chk_eitaa_groups_type CHECK (type IN ('GROUP', 'CHANNEL')),
    CONSTRAINT chk_eitaa_groups_status CHECK (status IN ('ACTIVE', 'DISABLED', 'ERROR')),

    CONSTRAINT uk_eitaa_groups_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_eitaa_groups_tenant ON eitaa_groups (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_eitaa_groups_classroom ON eitaa_groups (tenant_id, classroom_id)
    WHERE deleted_at IS NULL AND classroom_id IS NOT NULL;

CREATE TRIGGER trg_eitaa_groups_updated_at
    BEFORE UPDATE ON eitaa_groups FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── scheduled_messages ───────────────
-- گام ۱: محتوا مستقیم ذخیره می‌شود (بدون message_template).
CREATE TABLE scheduled_messages (
    id              UUID         PRIMARY KEY,
    tenant_id       UUID         NOT NULL,
    target_type     VARCHAR(20)  NOT NULL,
    target_id       UUID         NOT NULL,
    content_text    TEXT         NOT NULL,
    scheduled_at    TIMESTAMPTZ  NOT NULL,
    recurrence      VARCHAR(20)  NOT NULL DEFAULT 'NONE',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts        INTEGER      NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ  NULL,
    eitaa_message_id VARCHAR(50) NULL,
    error_message   TEXT         NULL,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      UUID         NULL,
    updated_by      UUID         NULL,
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT fk_scheduled_messages_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,

    CONSTRAINT chk_scheduled_messages_target CHECK (target_type IN (
        'GROUP', 'CHANNEL', 'USER')),
    CONSTRAINT chk_scheduled_messages_recurrence CHECK (recurrence IN (
        'NONE', 'DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT chk_scheduled_messages_status CHECK (status IN (
        'PENDING', 'SENT', 'CANCELLED', 'FAILED')),
    CONSTRAINT chk_scheduled_messages_attempts CHECK (attempts >= 0),

    CONSTRAINT uk_scheduled_messages_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_scheduled_messages_tenant ON scheduled_messages (tenant_id);
CREATE INDEX idx_scheduled_messages_pending
    ON scheduled_messages (tenant_id, status, scheduled_at)
    WHERE status = 'PENDING';

CREATE TRIGGER trg_scheduled_messages_updated_at
    BEFORE UPDATE ON scheduled_messages FOR EACH ROW EXECUTE FUNCTION set_updated_at();
