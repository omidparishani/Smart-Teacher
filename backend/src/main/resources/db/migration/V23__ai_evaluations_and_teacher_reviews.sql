-- ═══════════════════════════════════════════════════════════
--  V23 — گسترش ai_evaluations + teacher_reviews (فاز ۷ گام ۲)
--
--  stub V15 فقط id/status داشت؛ ستون‌های Structured Output اینجا
--  اضافه می‌شوند. teacher_reviews لاگ Append-Only است — هیچ UPDATE
--  کسب‌وکاری روی ردیف‌های قبلی نیست.
-- ═══════════════════════════════════════════════════════════

-- ─────────────── گسترش ai_evaluations ───────────────
ALTER TABLE ai_evaluations
    ADD COLUMN IF NOT EXISTS ai_score           NUMERIC(8, 2)  NULL,
    ADD COLUMN IF NOT EXISTS is_correct         BOOLEAN        NULL,
    ADD COLUMN IF NOT EXISTS mistakes           JSONB          NULL,
    ADD COLUMN IF NOT EXISTS strengths          JSONB          NULL,
    ADD COLUMN IF NOT EXISTS weaknesses         JSONB          NULL,
    ADD COLUMN IF NOT EXISTS feedback           TEXT           NULL,
    ADD COLUMN IF NOT EXISTS teacher_note       TEXT           NULL,
    ADD COLUMN IF NOT EXISTS confidence         NUMERIC(5, 4)  NULL,
    ADD COLUMN IF NOT EXISTS ai_usage_id        UUID           NULL,
    ADD COLUMN IF NOT EXISTS prompt_version_id  UUID           NULL,
    ADD COLUMN IF NOT EXISTS raw_response       JSONB          NULL;

ALTER TABLE ai_evaluations
    DROP CONSTRAINT IF EXISTS chk_ai_evaluations_confidence;

ALTER TABLE ai_evaluations
    ADD CONSTRAINT chk_ai_evaluations_confidence
        CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1));

-- ─────────────── teacher_reviews (Append-Only) ───────────────
CREATE TABLE teacher_reviews (
    id                  UUID           PRIMARY KEY,
    tenant_id           UUID           NOT NULL,
    submission_id       UUID           NOT NULL,
    reviewer_user_id    UUID           NOT NULL,
    action              VARCHAR(30)    NOT NULL,
    previous_score      NUMERIC(8, 2)  NULL,
    new_score           NUMERIC(8, 2)  NULL,
    comment             TEXT           NULL,
    reviewed_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),

    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by          UUID           NULL,
    updated_by          UUID           NULL,
    version             BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT fk_teacher_reviews_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,

    CONSTRAINT fk_teacher_reviews_submission
        FOREIGN KEY (tenant_id, submission_id)
            REFERENCES assignment_submissions (tenant_id, id)
        ON DELETE CASCADE,

    CONSTRAINT fk_teacher_reviews_reviewer
        FOREIGN KEY (reviewer_user_id) REFERENCES users(id) ON DELETE RESTRICT,

    CONSTRAINT chk_teacher_reviews_action
        CHECK (action IN (
            'ACCEPT', 'EDIT_SCORE', 'EDIT_FEEDBACK', 'REJECT', 'RE_EVALUATE'
        )),

    CONSTRAINT uk_teacher_reviews_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_teacher_reviews_submission
    ON teacher_reviews (tenant_id, submission_id, reviewed_at);
CREATE INDEX idx_teacher_reviews_reviewer
    ON teacher_reviews (tenant_id, reviewer_user_id);

CREATE TRIGGER trg_teacher_reviews_updated_at
    BEFORE UPDATE ON teacher_reviews
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
