-- ═══════════════════════════════════════════════════════════
--  V16 — آزمون‌ها (گام ۱ از فاز ۸)
--
--  جداول exams / exam_questions / exam_attempts / exam_answers
--  مطابق docs/database.md بخش ۶ — حداقل ستون‌های لازم برای
--  CRUD معلم، شروع/ارسال دانش‌آموز، و تصحیح خودکار MCQ.
--
--  ⚠️ V12–V14 گرفته شده‌اند؛ V15 برای عامل موازی تکلیف رزرو است.
-- ═══════════════════════════════════════════════════════════

-- ─────────────── exams ───────────────
CREATE TABLE exams (
    id                       UUID          PRIMARY KEY,
    tenant_id                UUID          NOT NULL,
    classroom_id             UUID          NOT NULL,
    book_id                  UUID          NULL,
    title                    VARCHAR(250)  NOT NULL,
    description              TEXT          NULL,
    duration_minutes         INTEGER       NULL,
    total_score              NUMERIC(10,2) NULL,
    start_at                 TIMESTAMPTZ   NULL,
    end_at                   TIMESTAMPTZ   NULL,
    status                   VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    shuffle_questions        BOOLEAN       NOT NULL DEFAULT FALSE,
    show_results_immediately BOOLEAN       NOT NULL DEFAULT TRUE,

    created_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by               UUID          NULL,
    updated_by               UUID          NULL,
    version                  BIGINT        NOT NULL DEFAULT 0,
    deleted_at               TIMESTAMPTZ   NULL,

    CONSTRAINT fk_exams_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_exams_classroom
        FOREIGN KEY (tenant_id, classroom_id)
        REFERENCES classrooms (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_exams_book
        FOREIGN KEY (tenant_id, book_id)
        REFERENCES books (tenant_id, id) ON DELETE SET NULL (book_id),

    CONSTRAINT chk_exams_status CHECK (status IN (
        'DRAFT', 'SCHEDULED', 'ACTIVE', 'FINISHED', 'ARCHIVED')),
    CONSTRAINT chk_exams_duration CHECK (
        duration_minutes IS NULL OR duration_minutes > 0),
    CONSTRAINT chk_exams_window CHECK (
        end_at IS NULL OR start_at IS NULL OR end_at >= start_at),

    CONSTRAINT uk_exams_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_exams_tenant ON exams (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_exams_tenant_class ON exams (tenant_id, classroom_id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_exams_tenant_status ON exams (tenant_id, status)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_exams_updated_at
    BEFORE UPDATE ON exams FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── exam_questions ───────────────
CREATE TABLE exam_questions (
    id              UUID          PRIMARY KEY,
    tenant_id       UUID          NOT NULL,
    exam_id         UUID          NOT NULL,
    type            VARCHAR(20)   NOT NULL,
    question_text   TEXT          NOT NULL,
    options         JSONB         NULL,
    correct_answer  TEXT          NULL,
    score           NUMERIC(10,2) NOT NULL DEFAULT 1,
    difficulty      VARCHAR(10)   NULL,
    explanation     TEXT          NULL,
    order_index     INTEGER       NOT NULL DEFAULT 0,
    ai_generated    BOOLEAN       NOT NULL DEFAULT FALSE,

    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      UUID          NULL,
    updated_by      UUID          NULL,
    version         BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT fk_exam_questions_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_exam_questions_exam
        FOREIGN KEY (tenant_id, exam_id)
        REFERENCES exams (tenant_id, id) ON DELETE CASCADE,

    CONSTRAINT chk_exam_questions_type CHECK (type IN (
        'MCQ', 'TRUE_FALSE', 'SHORT_ANSWER', 'ESSAY', 'FILL_BLANK')),
    CONSTRAINT chk_exam_questions_difficulty CHECK (
        difficulty IS NULL OR difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT chk_exam_questions_score CHECK (score >= 0),

    CONSTRAINT uk_exam_questions_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_exam_questions_tenant ON exam_questions (tenant_id);
CREATE INDEX idx_exam_questions_exam ON exam_questions (tenant_id, exam_id, order_index);

CREATE TRIGGER trg_exam_questions_updated_at
    BEFORE UPDATE ON exam_questions FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── exam_attempts ───────────────
CREATE TABLE exam_attempts (
    id              UUID          PRIMARY KEY,
    tenant_id       UUID          NOT NULL,
    exam_id         UUID          NOT NULL,
    student_id      UUID          NOT NULL,
    started_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    submitted_at    TIMESTAMPTZ   NULL,
    score           NUMERIC(10,2) NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'IN_PROGRESS',
    attempt_number  INTEGER       NOT NULL DEFAULT 1,

    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      UUID          NULL,
    updated_by      UUID          NULL,
    version         BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT fk_exam_attempts_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_exam_attempts_exam
        FOREIGN KEY (tenant_id, exam_id)
        REFERENCES exams (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_exam_attempts_student
        FOREIGN KEY (tenant_id, student_id)
        REFERENCES students (tenant_id, id) ON DELETE RESTRICT,

    CONSTRAINT chk_exam_attempts_status CHECK (status IN (
        'IN_PROGRESS', 'SUBMITTED', 'GRADING', 'GRADED', 'ABSENT')),
    CONSTRAINT chk_exam_attempts_number CHECK (attempt_number >= 1),

    CONSTRAINT uk_exam_attempts_tenant_id UNIQUE (tenant_id, id)
);

-- یک تلاش در حال انجام برای هر دانش‌آموز در هر آزمون
CREATE UNIQUE INDEX uk_exam_attempts_in_progress
    ON exam_attempts (tenant_id, exam_id, student_id)
    WHERE status = 'IN_PROGRESS';

CREATE INDEX idx_exam_attempts_tenant ON exam_attempts (tenant_id);
CREATE INDEX idx_exam_attempts_exam ON exam_attempts (tenant_id, exam_id);
CREATE INDEX idx_exam_attempts_student ON exam_attempts (tenant_id, student_id);

CREATE TRIGGER trg_exam_attempts_updated_at
    BEFORE UPDATE ON exam_attempts FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── exam_answers ───────────────
CREATE TABLE exam_answers (
    id                UUID          PRIMARY KEY,
    tenant_id         UUID          NOT NULL,
    attempt_id        UUID          NOT NULL,
    question_id       UUID          NOT NULL,
    answer_text       TEXT          NULL,
    selected_options  JSONB         NULL,
    awarded_score     NUMERIC(10,2) NULL,
    is_correct        BOOLEAN       NULL,
    ai_graded         BOOLEAN       NOT NULL DEFAULT FALSE,
    ai_feedback       TEXT          NULL,

    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        UUID          NULL,
    updated_by        UUID          NULL,
    version           BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT fk_exam_answers_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_exam_answers_attempt
        FOREIGN KEY (tenant_id, attempt_id)
        REFERENCES exam_attempts (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_answers_question
        FOREIGN KEY (tenant_id, question_id)
        REFERENCES exam_questions (tenant_id, id) ON DELETE RESTRICT,

    CONSTRAINT uk_exam_answers_attempt_question UNIQUE (tenant_id, attempt_id, question_id),
    CONSTRAINT uk_exam_answers_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_exam_answers_tenant ON exam_answers (tenant_id);
CREATE INDEX idx_exam_answers_attempt ON exam_answers (tenant_id, attempt_id);

CREATE TRIGGER trg_exam_answers_updated_at
    BEFORE UPDATE ON exam_answers FOR EACH ROW EXECUTE FUNCTION set_updated_at();
