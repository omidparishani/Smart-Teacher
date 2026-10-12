-- ═══════════════════════════════════════════════════════════
--  V20 — تخصیص Provider شش‌سطحی (فاز ۵ — ProviderResolver)
--
--  ⚠️ فقط اسکیمای تخصیص. Adapter واقعی HTTP اینجا نیست.
--  ⚠️ مدارس جداگانه جدول ندارند؛ مستأجر با type=SCHOOL کافی است.
-- ═══════════════════════════════════════════════════════════

CREATE TABLE ai_provider_assignments (
    id              UUID         PRIMARY KEY,
    scope_type      VARCHAR(20)  NOT NULL,
    scope_id        UUID         NULL,
    provider_id     UUID         NOT NULL,
    model_id        UUID         NULL,
    priority        INTEGER      NOT NULL DEFAULT 100,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    tenant_id       UUID         NULL,
    overrides       JSONB        NOT NULL DEFAULT '{}'::jsonb,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      UUID         NULL,
    updated_by      UUID         NULL,
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT fk_ai_pa_provider
        FOREIGN KEY (provider_id) REFERENCES ai_providers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_pa_model
        FOREIGN KEY (model_id) REFERENCES ai_models (id) ON DELETE SET NULL,
    CONSTRAINT fk_ai_pa_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE,
    CONSTRAINT chk_ai_pa_scope CHECK (
        scope_type IN ('USER', 'CLASS', 'TENANT', 'PLAN', 'FEATURE', 'GLOBAL')
    ),
    CONSTRAINT chk_ai_pa_global_scope CHECK (
        (scope_type = 'GLOBAL' AND scope_id IS NULL)
        OR (scope_type <> 'GLOBAL')
    ),
    CONSTRAINT chk_ai_pa_priority CHECK (priority >= 0)
);

CREATE INDEX idx_ai_pa_lookup
    ON ai_provider_assignments (scope_type, scope_id, enabled, priority)
    WHERE enabled = TRUE;

CREATE INDEX idx_ai_pa_tenant
    ON ai_provider_assignments (tenant_id)
    WHERE tenant_id IS NOT NULL;

CREATE TRIGGER trg_ai_provider_assignments_updated_at
    BEFORE UPDATE ON ai_provider_assignments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- هم‌راستاسازی موجودیت UsageCounter (AuditableEntity) با جدول V18
ALTER TABLE usage_counters ADD COLUMN IF NOT EXISTS created_by UUID NULL;
ALTER TABLE usage_counters ADD COLUMN IF NOT EXISTS updated_by UUID NULL;
