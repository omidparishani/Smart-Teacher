-- ═══════════════════════════════════════════════════════════
--  V15 — تکالیف و ارسال پاسخ (گام ۱ از فاز ۷)
--
--  ⚠️ شماره با نقشه راه docs/database.md یکی نیست (آنجا V11).
--  عدد واقعی = بزرگ‌ترین موجود (V14) + ۱.
--
--  ⚠️ دریافت پاسخ از چت ایتا نیست — docs/eitaa.md بخش ۶.
--  دانش‌آموز پاسخ را در Mini App ثبت می‌کند.
--
--  ترتیب: assignments → assignment_submissions → ai_evaluations (stub)
-- ═══════════════════════════════════════════════════════════

-- ─────────────── assignments ───────────────
CREATE TABLE assignments (
    id                      UUID           PRIMARY KEY,
    tenant_id               UUID           NOT NULL,
    classroom_id            UUID           NOT NULL,
    book_id                 UUID           NULL,
    page_from               INTEGER        NULL,
    page_to                 INTEGER        NULL,
    title                   VARCHAR(250)   NOT NULL,
    description             TEXT           NULL,
    type                    VARCHAR(20)    NOT NULL DEFAULT 'HOMEWORK',
    max_score               NUMERIC(8, 2)  NULL,
    due_at                  TIMESTAMPTZ    NULL,
    status                  VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    ai_evaluation_enabled   BOOLEAN        NOT NULL DEFAULT FALSE,
    prompt_version_id       UUID           NULL,
    eitaa_chat_id           VARCHAR(100)   NULL,
    eitaa_message_id        VARCHAR(100)   NULL,
    eitaa_send_status       VARCHAR(20)    NULL,

    created_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by              UUID           NULL,
    updated_by              UUID           NULL,
    version                 BIGINT         NOT NULL DEFAULT 0,
    deleted_at              TIMESTAMPTZ    NULL,

    CONSTRAINT fk_assignments_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,

    CONSTRAINT fk_assignments_classroom
        FOREIGN KEY (tenant_id, classroom_id) REFERENCES classrooms (tenant_id, id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_assignments_book
        FOREIGN KEY (tenant_id, book_id) REFERENCES books (tenant_id, id)
        ON DELETE SET NULL (book_id),

    CONSTRAINT chk_assignments_type
        CHECK (type IN ('HOMEWORK', 'PROJECT', 'PRACTICE', 'QUIZ')),
    CONSTRAINT chk_assignments_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED', 'ARCHIVED')),
    CONSTRAINT chk_assignments_pages
        CHECK (
            (page_from IS NULL AND page_to IS NULL)
            OR (page_from IS NOT NULL AND page_to IS NOT NULL
                AND page_from > 0 AND page_to >= page_from)
        ),
    CONSTRAINT chk_assignments_max_score
        CHECK (max_score IS NULL OR max_score > 0),
    CONSTRAINT chk_assignments_eitaa_send_status
        CHECK (eitaa_send_status IS NULL
            OR eitaa_send_status IN ('SENT', 'FAILED', 'SKIPPED')),

    -- هدف کلید خارجی ترکیبی assignment_submissions
    CONSTRAINT uk_assignments_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_assignments_tenant
    ON assignments (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_assignments_classroom
    ON assignments (tenant_id, classroom_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_assignments_status
    ON assignments (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_assignments_title_trgm
    ON assignments USING gin (lower(title) gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_assignments_updated_at
    BEFORE UPDATE ON assignments FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── assignment_submissions ───────────────
-- ⚠️ حذف نرم ندارد (ERD بخش ۵): ارسال یک رخداد است؛ بازنویسی پاسخ
-- با به‌روزرسانی همان ردیف مدل می‌شود، نه حذف.
CREATE TABLE assignment_submissions (
    id                      UUID           PRIMARY KEY,
    tenant_id               UUID           NOT NULL,
    assignment_id           UUID           NOT NULL,
    student_id              UUID           NOT NULL,
    answer_text             TEXT           NULL,
    status                  VARCHAR(20)    NOT NULL DEFAULT 'SUBMITTED',
    submitted_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    submitted_by_user_id    UUID           NULL,
    final_score             NUMERIC(8, 2)  NULL,
    teacher_overrode_ai     BOOLEAN        NOT NULL DEFAULT FALSE,

    created_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by              UUID           NULL,
    updated_by              UUID           NULL,
    version                 BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT fk_assignment_submissions_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,

    CONSTRAINT fk_assignment_submissions_assignment
        FOREIGN KEY (tenant_id, assignment_id) REFERENCES assignments (tenant_id, id)
        ON DELETE CASCADE,

    CONSTRAINT fk_assignment_submissions_student
        FOREIGN KEY (tenant_id, student_id) REFERENCES students (tenant_id, id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_assignment_submissions_submitted_by
        FOREIGN KEY (submitted_by_user_id) REFERENCES users(id) ON DELETE SET NULL,

    CONSTRAINT chk_assignment_submissions_status
        CHECK (status IN (
            'PENDING', 'SUBMITTED', 'AI_EVALUATED', 'REVIEWED', 'GRADED', 'RETURNED'
        )),

    CONSTRAINT uk_assignment_submissions_tenant_id UNIQUE (tenant_id, id)
);

-- یک دانش‌آموز برای یک تکلیف فقط یک ارسال دارد.
CREATE UNIQUE INDEX uk_assignment_submissions_student
    ON assignment_submissions (tenant_id, assignment_id, student_id);

CREATE INDEX idx_assignment_submissions_assignment
    ON assignment_submissions (tenant_id, assignment_id);
CREATE INDEX idx_assignment_submissions_student
    ON assignment_submissions (tenant_id, student_id);
CREATE INDEX idx_assignment_submissions_by_user
    ON assignment_submissions (tenant_id, submitted_by_user_id)
    WHERE submitted_by_user_id IS NOT NULL;

CREATE TRIGGER trg_assignment_submissions_updated_at
    BEFORE UPDATE ON assignment_submissions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── ai_evaluations (stub گام ۱) ───────────────
-- ستون‌های کامل Structured Output در گام ۲ فاز ۷ می‌آیند.
-- اینجا فقط اسکلت FK تا Migration بعدی جدول را از نو نسازد.
CREATE TABLE ai_evaluations (
    id                      UUID           PRIMARY KEY,
    tenant_id               UUID           NOT NULL,
    submission_id           UUID           NOT NULL,
    status                  VARCHAR(20)    NOT NULL DEFAULT 'PENDING',

    created_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by              UUID           NULL,
    updated_by              UUID           NULL,
    version                 BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT fk_ai_evaluations_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,

    CONSTRAINT fk_ai_evaluations_submission
        FOREIGN KEY (tenant_id, submission_id)
            REFERENCES assignment_submissions (tenant_id, id)
        ON DELETE CASCADE,

    CONSTRAINT chk_ai_evaluations_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'PARTIAL', 'FAILED', 'LOW_CONFIDENCE')),

    CONSTRAINT uk_ai_evaluations_submission UNIQUE (tenant_id, submission_id)
);

CREATE INDEX idx_ai_evaluations_tenant ON ai_evaluations (tenant_id);

CREATE TRIGGER trg_ai_evaluations_updated_at
    BEFORE UPDATE ON ai_evaluations FOR EACH ROW EXECUTE FUNCTION set_updated_at();
