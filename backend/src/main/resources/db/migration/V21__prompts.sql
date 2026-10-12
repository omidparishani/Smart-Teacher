-- ═══════════════════════════════════════════════════════════
--  V21 — سیستم پرامپت (فاز ۵ — Template / Version / Assignment)
--
--  نام جداول مطابق درخواست پیاده‌سازی: prompts / prompt_versions /
--  prompt_assignments (معادل ERD: PROMPT_TEMPLATES / …).
-- ═══════════════════════════════════════════════════════════

CREATE TABLE prompts (
    id              UUID         PRIMARY KEY,
    code            VARCHAR(80)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    feature_code    VARCHAR(80)  NOT NULL,
    description     TEXT         NULL,
    is_system       BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      UUID         NULL,
    updated_by      UUID         NULL,
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_prompts_code UNIQUE (code)
);

CREATE INDEX idx_prompts_feature ON prompts (feature_code);

CREATE TABLE prompt_versions (
    id               UUID           PRIMARY KEY,
    prompt_id        UUID           NOT NULL,
    version_number   INTEGER        NOT NULL,
    system_prompt    TEXT           NOT NULL,
    user_prompt      TEXT           NOT NULL,
    variables        JSONB          NOT NULL DEFAULT '{}'::jsonb,
    response_schema  JSONB          NULL,
    temperature      NUMERIC(4, 2)  NULL,
    max_tokens       INTEGER        NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    published_by     UUID           NULL,
    published_at     TIMESTAMPTZ    NULL,
    change_note      TEXT           NULL,

    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by       UUID           NULL,
    updated_by       UUID           NULL,
    version          BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT fk_prompt_versions_prompt
        FOREIGN KEY (prompt_id) REFERENCES prompts (id) ON DELETE CASCADE,
    CONSTRAINT uk_prompt_versions_number UNIQUE (prompt_id, version_number),
    CONSTRAINT chk_prompt_versions_status CHECK (
        status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')
    ),
    CONSTRAINT chk_prompt_versions_number CHECK (version_number >= 1),
    CONSTRAINT chk_prompt_versions_temperature CHECK (
        temperature IS NULL OR (temperature >= 0 AND temperature <= 2)
    )
);

CREATE UNIQUE INDEX uk_prompt_versions_one_published
    ON prompt_versions (prompt_id)
    WHERE status = 'PUBLISHED';

CREATE INDEX idx_prompt_versions_prompt
    ON prompt_versions (prompt_id, version_number DESC);

CREATE TABLE prompt_assignments (
    id              UUID         PRIMARY KEY,
    scope_type      VARCHAR(20)  NOT NULL,
    scope_id        UUID         NULL,
    prompt_id       UUID         NOT NULL,
    version_id      UUID         NULL,
    priority        INTEGER      NOT NULL DEFAULT 100,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    tenant_id       UUID         NULL,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      UUID         NULL,
    updated_by      UUID         NULL,
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT fk_prompt_asg_prompt
        FOREIGN KEY (prompt_id) REFERENCES prompts (id) ON DELETE CASCADE,
    CONSTRAINT fk_prompt_asg_version
        FOREIGN KEY (version_id) REFERENCES prompt_versions (id) ON DELETE SET NULL,
    CONSTRAINT fk_prompt_asg_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE,
    CONSTRAINT chk_prompt_asg_scope CHECK (
        scope_type IN ('USER', 'CLASS', 'TENANT', 'PLAN', 'FEATURE', 'GLOBAL')
    ),
    CONSTRAINT chk_prompt_asg_global CHECK (
        (scope_type = 'GLOBAL' AND scope_id IS NULL)
        OR (scope_type <> 'GLOBAL')
    )
);

CREATE INDEX idx_prompt_asg_lookup
    ON prompt_assignments (scope_type, scope_id, enabled, priority)
    WHERE enabled = TRUE;

CREATE TRIGGER trg_prompts_updated_at
    BEFORE UPDATE ON prompts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_prompt_versions_updated_at
    BEFORE UPDATE ON prompt_versions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_prompt_assignments_updated_at
    BEFORE UPDATE ON prompt_assignments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- پرامپت سیستمی حداقلی برای دستیار معلم (GLOBAL)
INSERT INTO prompts (id, code, name, feature_code, description, is_system)
VALUES (
    'a1000000-0000-7000-8000-000000000001',
    'AI_CHAT_SYSTEM',
    'دستیار معلم',
    'TEACHER_CHAT',
    'پرامپت سیستمی پیش‌فرض گفت‌وگوی معلم',
    TRUE
);

INSERT INTO prompt_versions (
    id, prompt_id, version_number, system_prompt, user_prompt,
    variables, status, published_at, change_note
) VALUES (
    'a1000000-0000-7000-8000-000000000011',
    'a1000000-0000-7000-8000-000000000001',
    1,
    'شما دستیار آموزشی معلمان ایرانی هستید. پاسخ‌ها کوتاه، دقیق و فارسی باشند.',
    '{{message}}',
    '{"message":{"type":"UNTRUSTED_TEXT","required":true,"maxLength":8000}}'::jsonb,
    'PUBLISHED',
    now(),
    'نسخه اولیه seed'
);

INSERT INTO prompt_assignments (
    id, scope_type, scope_id, prompt_id, version_id, priority, enabled, tenant_id
) VALUES (
    'a1000000-0000-7000-8000-000000000021',
    'GLOBAL',
    NULL,
    'a1000000-0000-7000-8000-000000000001',
    NULL,
    100,
    TRUE,
    NULL
);
