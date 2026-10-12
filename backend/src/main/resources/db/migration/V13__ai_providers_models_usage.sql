-- ═══════════════════════════════════════════════════════════
--  V13 — Provider/مدل/مصرف AI (فاز ۵ گام ۱ — فقط مسیر Mock)
--
--  ⚠️ هیچ Adapter واقعی (Anthropic/OpenAI/…) اینجا نیست. این Migration
--  فقط جدول‌های داده را می‌سازد تا MockAiProvider و CRUD ادمین قابل
--  تست باشند. نام فیلدهای API واقعی Provider حدس زده نمی‌شود
--  (docs/verification-log.md بخش ۴ · docs/ai-providers.md).
--
--  ⚠️ شماره V13: V12 = files/books (عامل موازی)، V14 = features.
-- ═══════════════════════════════════════════════════════════

-- ─────────────── ai_providers ───────────────
-- پیکربندی سطح پلتفرم — بدون tenant_id. کلید API فقط رمزشده ذخیره می‌شود.
CREATE TABLE ai_providers (
    id                 UUID         PRIMARY KEY,
    code               VARCHAR(50)  NOT NULL,
    name               VARCHAR(150) NOT NULL,
    type               VARCHAR(40)  NOT NULL,
    base_url           VARCHAR(500) NULL,
    api_key_encrypted  TEXT         NULL,
    api_key_hint       VARCHAR(20)  NULL,
    enabled            BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by         UUID         NULL,
    updated_by         UUID         NULL,
    version            BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_ai_providers_code UNIQUE (code),
    CONSTRAINT chk_ai_providers_type CHECK (
        type IN ('MOCK', 'ANTHROPIC', 'OPENAI_COMPATIBLE', 'GOOGLE', 'CUSTOM')
    )
);

CREATE INDEX idx_ai_providers_enabled ON ai_providers (enabled) WHERE enabled = TRUE;

-- ─────────────── ai_models ───────────────
CREATE TABLE ai_models (
    id                              UUID           PRIMARY KEY,
    provider_id                     UUID           NOT NULL,
    code                            VARCHAR(100)   NOT NULL,
    display_name                    VARCHAR(150)   NOT NULL,
    enabled                         BOOLEAN        NOT NULL DEFAULT TRUE,
    input_cost_per_million          NUMERIC(18, 8) NULL,
    output_cost_per_million         NUMERIC(18, 8) NULL,
    cached_input_cost_per_million   NUMERIC(18, 8) NULL,
    currency                        VARCHAR(10)    NOT NULL DEFAULT 'USD',

    created_at                      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by                      UUID           NULL,
    updated_by                      UUID           NULL,
    version                         BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT fk_ai_models_provider
        FOREIGN KEY (provider_id) REFERENCES ai_providers (id) ON DELETE CASCADE,
    CONSTRAINT uk_ai_models_provider_code UNIQUE (provider_id, code)
);

CREATE INDEX idx_ai_models_provider ON ai_models (provider_id) WHERE enabled = TRUE;

-- ─────────────── ai_usage_records ───────────────
-- حداقل ستون‌های لازم برای ثبت مصرف در گام ۱ (docs گام ۸ فاز ۵ کامل‌تر می‌شود).
CREATE TABLE ai_usage_records (
    id                  UUID           PRIMARY KEY,
    tenant_id           UUID           NOT NULL,
    feature             VARCHAR(80)    NOT NULL,
    model               VARCHAR(100)   NOT NULL,
    prompt_tokens       BIGINT         NOT NULL DEFAULT 0,
    completion_tokens   BIGINT         NOT NULL DEFAULT 0,
    cost                NUMERIC(18, 8) NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by          UUID           NULL,
    updated_by          UUID           NULL,
    version             BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT fk_ai_usage_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE RESTRICT,
    CONSTRAINT chk_ai_usage_tokens CHECK (
        prompt_tokens >= 0 AND completion_tokens >= 0 AND cost >= 0
    )
);

CREATE INDEX idx_ai_usage_tenant_created
    ON ai_usage_records (tenant_id, created_at DESC);
CREATE INDEX idx_ai_usage_feature
    ON ai_usage_records (tenant_id, feature, created_at DESC);
