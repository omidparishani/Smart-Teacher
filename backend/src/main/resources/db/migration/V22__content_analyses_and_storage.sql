-- ═══════════════════════════════════════════════════════════
--  V22 — تحلیل محتوا + سقف اختیاری storageBytes (فاز ۶ گام ۲)
--
--  content_analyses: چندریختی (target_type + target_id) مطابق ERD.
--  مصرف ذخیره‌سازی از sum(files.size_bytes) محاسبه می‌شود؛ جدول
--  جداگانه storage_usage لازم نیست. سقف اختیاری در plan_limits با
--  متریک storageBytes.
-- ═══════════════════════════════════════════════════════════

-- ─────────────── content_analyses ───────────────
CREATE TABLE content_analyses (
    id                  UUID          PRIMARY KEY,
    tenant_id           UUID          NOT NULL,
    target_type         VARCHAR(20)   NOT NULL,
    target_id           UUID          NOT NULL,
    analysis_type       VARCHAR(30)   NOT NULL,
    result_text         TEXT          NULL,
    result_json         JSONB         NULL,
    ai_usage_id         UUID          NULL,
    prompt_version_id   UUID          NULL,

    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          UUID          NULL,
    updated_by          UUID          NULL,
    version             BIGINT        NOT NULL DEFAULT 0,
    deleted_at          TIMESTAMPTZ   NULL,

    CONSTRAINT fk_content_analyses_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,

    CONSTRAINT chk_content_analyses_target_type
        CHECK (target_type IN ('BOOK', 'PAGE', 'CHAPTER')),
    CONSTRAINT chk_content_analyses_analysis_type
        CHECK (analysis_type IN (
            'SUMMARY', 'QUESTIONS', 'TEACHING_PLAN', 'DIFFICULTY'
        )),

    CONSTRAINT uk_content_analyses_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_content_analyses_tenant
    ON content_analyses (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_content_analyses_target
    ON content_analyses (tenant_id, target_type, target_id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_content_analyses_type
    ON content_analyses (tenant_id, analysis_type)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_content_analyses_updated_at
    BEFORE UPDATE ON content_analyses
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── سقف اختیاری storageBytes روی پلن‌ها ───────────────
-- دوره TOTAL: جمع کل فایل‌های فعال مستأجر (نه ماهانه).
INSERT INTO plan_limits (id, plan_id, metric, limit_value, period) VALUES
('71000000-0000-7000-8000-000000000011',
 '70000000-0000-7000-8000-000000000001', 'storageBytes', 104857600, 'TOTAL'),
('71000000-0000-7000-8000-000000000012',
 '70000000-0000-7000-8000-000000000002', 'storageBytes', 1073741824, 'TOTAL'),
('71000000-0000-7000-8000-000000000013',
 '70000000-0000-7000-8000-000000000003', 'storageBytes', 10737418240, 'TOTAL');
