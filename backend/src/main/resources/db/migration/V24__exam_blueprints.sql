-- ═══════════════════════════════════════════════════════════
--  V24 — طرح تولید آزمون (فاز ۸ گام ۲)
--
--  جدول exam_blueprints مطابق docs/database.md بخش ۶.
--  V20–V23 توسط عوامل موازی رزرو شده‌اند؛ این Migration فقط V24 است.
-- ═══════════════════════════════════════════════════════════

CREATE TABLE exam_blueprints (
    id                         UUID          PRIMARY KEY,
    tenant_id                  UUID          NOT NULL,
    exam_id                    UUID          NOT NULL,
    book_id                    UUID          NOT NULL,
    page_from                  INTEGER       NOT NULL,
    page_to                    INTEGER       NOT NULL,
    difficulty_distribution    JSONB         NULL,
    question_type_distribution JSONB         NULL,
    question_count             INTEGER       NOT NULL DEFAULT 0,
    ai_usage_id                UUID          NULL,

    created_at                 TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by                 UUID          NULL,
    updated_by                 UUID          NULL,
    version                    BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT fk_exam_blueprints_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_exam_blueprints_exam
        FOREIGN KEY (tenant_id, exam_id)
        REFERENCES exams (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_blueprints_book
        FOREIGN KEY (tenant_id, book_id)
        REFERENCES books (tenant_id, id) ON DELETE RESTRICT,

    CONSTRAINT chk_exam_blueprints_pages CHECK (page_to >= page_from AND page_from > 0),
    CONSTRAINT chk_exam_blueprints_count CHECK (question_count >= 0),

    CONSTRAINT uk_exam_blueprints_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_exam_blueprints_tenant ON exam_blueprints (tenant_id);
CREATE INDEX idx_exam_blueprints_exam ON exam_blueprints (tenant_id, exam_id);

CREATE TRIGGER trg_exam_blueprints_updated_at
    BEFORE UPDATE ON exam_blueprints FOR EACH ROW EXECUTE FUNCTION set_updated_at();
