-- ═══════════════════════════════════════════════════════════
--  V25 — کانال ایتا، قواعد خودکارسازی، اعتبار پیام (فاز ۹ گام ۲)
--
--  ❌ هیچ جدول inbound/webhook نیست (docs/eitaa.md بخش ۶).
--  V20–V23 توسط عوامل موازی رزرو شده‌اند؛ این Migration فقط V25 است.
-- ═══════════════════════════════════════════════════════════

-- ─────────────── eitaa_channels ───────────────
CREATE TABLE eitaa_channels (
    id                 UUID         PRIMARY KEY,
    tenant_id          UUID         NOT NULL,
    account_id         UUID         NULL,
    eitaa_chat_id      VARCHAR(100) NOT NULL,
    title              VARCHAR(250) NULL,
    username           VARCHAR(100) NULL,
    subscriber_count   INTEGER      NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    can_post           BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by         UUID         NULL,
    updated_by         UUID         NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    deleted_at         TIMESTAMPTZ  NULL,

    CONSTRAINT fk_eitaa_channels_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,

    CONSTRAINT uk_eitaa_channels_chat_id UNIQUE (eitaa_chat_id),
    CONSTRAINT chk_eitaa_channels_status CHECK (status IN ('ACTIVE', 'DISABLED', 'ERROR')),

    CONSTRAINT uk_eitaa_channels_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_eitaa_channels_tenant ON eitaa_channels (tenant_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_eitaa_channels_updated_at
    BEFORE UPDATE ON eitaa_channels FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── automation_rules ───────────────
CREATE TABLE automation_rules (
    id                 UUID         PRIMARY KEY,
    tenant_id          UUID         NOT NULL,
    name               VARCHAR(200) NOT NULL,
    trigger_type       VARCHAR(40)  NOT NULL,
    trigger_config     JSONB        NULL,
    action_type        VARCHAR(40)  NOT NULL,
    action_config      JSONB        NULL,
    enabled            BOOLEAN      NOT NULL DEFAULT TRUE,
    execution_count    INTEGER      NOT NULL DEFAULT 0,
    last_executed_at   TIMESTAMPTZ  NULL,

    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by         UUID         NULL,
    updated_by         UUID         NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    deleted_at         TIMESTAMPTZ  NULL,

    CONSTRAINT fk_automation_rules_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,

    CONSTRAINT chk_automation_rules_trigger CHECK (trigger_type IN (
        'ASSIGNMENT_PUBLISHED',
        'SUBMISSION_EVALUATED',
        'EXAM_COMPLETED',
        'SCORE_BELOW_THRESHOLD',
        'SUBSCRIPTION_EXPIRING',
        'SCHEDULE')),
    CONSTRAINT chk_automation_rules_action CHECK (action_type IN (
        'AI_EVALUATE',
        'NOTIFY_TEACHER',
        'NOTIFY_STUDENT',
        'SEND_MESSAGE',
        'GENERATE_REPORT')),
    CONSTRAINT chk_automation_rules_executions CHECK (execution_count >= 0),

    CONSTRAINT uk_automation_rules_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_automation_rules_tenant ON automation_rules (tenant_id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_automation_rules_trigger ON automation_rules (tenant_id, trigger_type)
    WHERE deleted_at IS NULL AND enabled = TRUE;

CREATE TRIGGER trg_automation_rules_updated_at
    BEFORE UPDATE ON automation_rules FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── message_credits (اختیاری — اعتبار پیام APP) ───────────────
CREATE TABLE message_credits (
    id                 UUID         PRIMARY KEY,
    tenant_id          UUID         NOT NULL,
    balance            INTEGER      NOT NULL DEFAULT 50000,
    free_quota         INTEGER      NOT NULL DEFAULT 50000,
    last_reset_at      TIMESTAMPTZ  NULL,

    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by         UUID         NULL,
    updated_by         UUID         NULL,
    version            BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT fk_message_credits_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT chk_message_credits_balance CHECK (balance >= 0),
    CONSTRAINT chk_message_credits_quota CHECK (free_quota >= 0),
    CONSTRAINT uk_message_credits_tenant UNIQUE (tenant_id),
    CONSTRAINT uk_message_credits_tenant_id UNIQUE (tenant_id, id)
);

CREATE TRIGGER trg_message_credits_updated_at
    BEFORE UPDATE ON message_credits FOR EACH ROW EXECUTE FUNCTION set_updated_at();
