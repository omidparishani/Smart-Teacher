-- ═══════════════════════════════════════════════════════════
--  V12 — فایل‌ها و کتابخانه (گام ۱ از فاز ۶)
--
--  ⚠️ شماره با نقشه راه docs/database.md بخش ۱۲ یکی نیست (آنجا
--  V10 را books می‌دانست). عدد واقعی = بزرگ‌ترین موجود + ۱.
--
--  ترتیب: files اول (چون books.cover_file_id و book_pages.file_id
--  به آن ارجاع می‌دهند)، سپس books، سپس chapters و pages.
-- ═══════════════════════════════════════════════════════════

-- ─────────────── files ───────────────
-- متادیتای ذخیره‌سازی. بایت‌ها روی دیسک محلی (LocalFileStorage)
-- یا بعداً MinIO/S3 با همان storage_key.
--
-- ⚠️ ERD می‌گوید tenant_id برای فایل پلتفرم nullable است. در گام ۱
-- همه فایل‌ها متعلق به مستأجرند (NOT NULL) — فایل پلتفرم وقتی لازم
-- شد با Migration جدا و مدل جدا اضافه می‌شود، نه با شل‌کردن این قید.
CREATE TABLE files (
    id                 UUID          PRIMARY KEY,
    tenant_id          UUID          NOT NULL,
    storage_provider   VARCHAR(20)   NOT NULL DEFAULT 'LOCAL',
    storage_key        VARCHAR(500)  NOT NULL,
    original_filename  VARCHAR(255)  NULL,
    content_type       VARCHAR(150)  NULL,
    size_bytes         BIGINT        NOT NULL DEFAULT 0,
    checksum_sha256    VARCHAR(64)   NULL,
    entity_type        VARCHAR(50)   NULL,
    entity_id          UUID          NULL,
    uploaded_by        UUID          NULL,
    scan_status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING',

    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by         UUID          NULL,
    updated_by         UUID          NULL,
    version            BIGINT        NOT NULL DEFAULT 0,
    deleted_at         TIMESTAMPTZ   NULL,

    CONSTRAINT fk_files_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_files_uploaded_by
        FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL,

    CONSTRAINT chk_files_storage_provider
        CHECK (storage_provider IN ('LOCAL', 'MINIO', 'S3')),
    CONSTRAINT chk_files_scan_status
        CHECK (scan_status IN ('PENDING', 'CLEAN', 'INFECTED', 'FAILED')),
    CONSTRAINT chk_files_size_bytes
        CHECK (size_bytes >= 0),

    -- هدف کلید خارجی ترکیبی books / book_pages
    CONSTRAINT uk_files_tenant_id UNIQUE (tenant_id, id)
);

-- یکتایی مسیر ذخیره‌سازی داخل مستأجر — دو رکورد به یک کلید نرسند.
CREATE UNIQUE INDEX uk_files_storage_key
    ON files (tenant_id, storage_key)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_files_tenant ON files (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_files_entity ON files (tenant_id, entity_type, entity_id)
    WHERE deleted_at IS NULL AND entity_type IS NOT NULL;

CREATE TRIGGER trg_files_updated_at
    BEFORE UPDATE ON files FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── books ───────────────
CREATE TABLE books (
    id              UUID         PRIMARY KEY,
    tenant_id       UUID         NOT NULL,
    title           VARCHAR(250) NOT NULL,
    publisher       VARCHAR(150) NULL,
    grade_level     VARCHAR(50)  NULL,
    subject_id      UUID         NULL,
    total_pages     INTEGER      NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    cover_file_id   UUID         NULL,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      UUID         NULL,
    updated_by      UUID         NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMPTZ  NULL,

    CONSTRAINT fk_books_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,

    CONSTRAINT chk_books_status
        CHECK (status IN ('DRAFT', 'PROCESSING', 'READY', 'FAILED')),
    CONSTRAINT chk_books_total_pages
        CHECK (total_pages IS NULL OR total_pages >= 0),

    -- کلید خارجی ترکیبی به درس همان مستأجر
    CONSTRAINT fk_books_subject
        FOREIGN KEY (tenant_id, subject_id) REFERENCES subjects (tenant_id, id)
        ON DELETE SET NULL (subject_id),

    -- جلد — همان مستأجر
    CONSTRAINT fk_books_cover_file
        FOREIGN KEY (tenant_id, cover_file_id) REFERENCES files (tenant_id, id)
        ON DELETE SET NULL (cover_file_id),

    CONSTRAINT uk_books_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_books_tenant ON books (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_books_tenant_status ON books (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_books_tenant_subject ON books (tenant_id, subject_id) WHERE deleted_at IS NULL;

-- جست‌وجوی عنوان — همان درس V9/V10: ایندکس روی lower(...)
CREATE INDEX idx_books_title_trgm
    ON books USING gin (lower(title) gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_books_updated_at
    BEFORE UPDATE ON books FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── book_chapters ───────────────
-- ⚠️ حذف نرم ندارد — فصل داراییِ کتاب است؛ با حذف نرم کتاب از API
-- بیرون می‌رود و فصل‌ها هم دیده نمی‌شوند.
CREATE TABLE book_chapters (
    id           UUID         PRIMARY KEY,
    tenant_id    UUID         NOT NULL,
    book_id      UUID         NOT NULL,
    title        VARCHAR(250) NOT NULL,
    order_index  INTEGER      NOT NULL DEFAULT 0,
    start_page   INTEGER      NULL,
    end_page     INTEGER      NULL,

    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   UUID         NULL,
    updated_by   UUID         NULL,
    version      BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT fk_book_chapters_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,

    CONSTRAINT fk_book_chapters_book
        FOREIGN KEY (tenant_id, book_id) REFERENCES books (tenant_id, id)
        ON DELETE CASCADE,

    CONSTRAINT chk_book_chapters_page_range
        CHECK (
            start_page IS NULL
            OR end_page IS NULL
            OR start_page <= end_page
        ),

    CONSTRAINT uk_book_chapters_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_book_chapters_book
    ON book_chapters (tenant_id, book_id, order_index);

CREATE TRIGGER trg_book_chapters_updated_at
    BEFORE UPDATE ON book_chapters FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── book_pages ───────────────
CREATE TABLE book_pages (
    id               UUID          PRIMARY KEY,
    tenant_id        UUID          NOT NULL,
    book_id          UUID          NOT NULL,
    page_number      INTEGER       NOT NULL,
    file_id          UUID          NOT NULL,
    extracted_text   TEXT          NULL,
    ocr_status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    ocr_confidence   NUMERIC(5, 4) NULL,
    layout_metadata  JSONB         NULL,

    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by       UUID          NULL,
    updated_by       UUID          NULL,
    version          BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT fk_book_pages_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,

    CONSTRAINT fk_book_pages_book
        FOREIGN KEY (tenant_id, book_id) REFERENCES books (tenant_id, id)
        ON DELETE CASCADE,

    CONSTRAINT fk_book_pages_file
        FOREIGN KEY (tenant_id, file_id) REFERENCES files (tenant_id, id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_book_pages_page_number
        CHECK (page_number > 0),
    CONSTRAINT chk_book_pages_ocr_status
        CHECK (ocr_status IN ('PENDING', 'DONE', 'FAILED', 'SKIPPED')),

    CONSTRAINT uk_book_pages_tenant_id UNIQUE (tenant_id, id)
);

-- یک صفحه در هر کتاب فقط یک‌بار.
CREATE UNIQUE INDEX uk_book_pages_book_page
    ON book_pages (tenant_id, book_id, page_number);

CREATE INDEX idx_book_pages_book
    ON book_pages (tenant_id, book_id, page_number);

CREATE TRIGGER trg_book_pages_updated_at
    BEFORE UPDATE ON book_pages FOR EACH ROW EXECUTE FUNCTION set_updated_at();
