-- ═══════════════════════════════════════════════════════════
--  V8 — درس‌ها و کلاس‌ها (دامنه آموزش — گام ۱ از فاز ۳)
--
--  نخستین جدول‌های دامنه آموزش. پیش از این، تنها جدول‌های
--  زیرساخت (هویت، مستأجر، ایتا، حسابرسی) وجود داشت.
--
--  ⚠️ نام این فایل با فهرست پیشنهادی docs/database.md بخش ۱۲
--  یکی نیست. آن فهرست یک طرح اولیه بود: V4 را «features_and_plans»
--  می‌دانست در حالی که V4 واقعی «refresh_tokens» است. شماره‌گذاری
--  واقعی با آن طرح هم‌راستا نشد و مهم هم نیست — Flyway تنها ترتیب
--  را می‌خواند، نه نام را. عدد جدید همیشه «بزرگ‌ترین عدد موجود + ۱»
--  است، نه «شماره ردیف در طرح».
--
--  ⚠️ آنچه عمداً اینجا نیست: students و class_students. هرچند ERD
--  آن‌ها را در همین دامنه می‌آورد، موجودیت بدون تست متناظر همان
--  چیزی است که §۸۰ خواسته‌ها نهی کرده. گام بعدی فاز ۳ آن‌ها را با
--  V9 می‌سازد.
-- ═══════════════════════════════════════════════════════════

-- ─────────────── subjects ───────────────
-- درس، دارایی مستأجر است و بین کلاس‌های همان مستأجر مشترک.
-- برای همین جدول جدا است و نه یک ستون متنی روی classrooms:
-- «ریاضی» در یک مدرسه یک رکورد است، نه یک رشته تکراری در ده کلاس.
CREATE TABLE subjects (
    id          UUID         PRIMARY KEY,
    tenant_id   UUID         NOT NULL,
    name        VARCHAR(150) NOT NULL,
    code        VARCHAR(50)  NULL,
    grade_level VARCHAR(50)  NULL,

    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  UUID         NULL,
    updated_by  UUID         NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    deleted_at  TIMESTAMPTZ  NULL,

    CONSTRAINT fk_subjects_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,

    -- ⚠️ این قید «اضافی» نیست؛ هدفِ کلید خارجی ترکیبی classrooms است.
    -- پستگرس فقط می‌تواند به یک UNIQUE/PK ارجاع دهد و PK تنها id است
    -- که مستأجر را در خود ندارد. بدون این قید، ارجاع می‌توانست از
    -- مرز مستأجر عبور کند.
    CONSTRAINT uk_subjects_tenant_id UNIQUE (tenant_id, id)
);

-- ⚠️ یکتایی فقط داخل یک مستأجر. دو مدرسه مختلف می‌توانند هر دو
-- کد «MATH-5» را داشته باشند؛ یکتایی سراسری آن‌ها را بی‌دلیل
-- به هم گره می‌زد.
--
-- ⚠️ نام درس یکتا نشد و عمدی است: «ریاضی» پایه پنجم و «ریاضی»
-- پایه ششم دو درس متفاوت‌اند. یکتایی روی (tenant_id, name) عملاً
-- ساخت درس پایه بالاتر را در همان مدرسه ممنوع می‌کرد.
CREATE UNIQUE INDEX uk_subjects_code
    ON subjects (tenant_id, lower(code))
    WHERE code IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_subjects_tenant      ON subjects (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_subjects_tenant_name ON subjects (tenant_id, name);

-- ─────────────── classrooms ───────────────
CREATE TABLE classrooms (
    id                  UUID         PRIMARY KEY,
    tenant_id           UUID         NOT NULL,
    name                VARCHAR(150) NOT NULL,
    grade_level         VARCHAR(50)  NULL,
    academic_year       VARCHAR(20)  NULL,
    subject_id          UUID         NULL,
    homeroom_teacher_id UUID         NULL,
    capacity            INTEGER      NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          UUID         NULL,
    updated_by          UUID         NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    deleted_at          TIMESTAMPTZ  NULL,

    CONSTRAINT chk_classrooms_status   CHECK (status IN ('ACTIVE','ARCHIVED')),
    CONSTRAINT chk_classrooms_capacity CHECK (capacity IS NULL OR capacity > 0),

    CONSTRAINT fk_classrooms_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,

    -- 🔴 کلید خارجی ترکیبی — نه یک FK ساده روی subject_id.
    --
    -- این قید در سطح دیتابیس تضمین می‌کند که کلاس فقط به درسِ
    -- «همان مستأجر» وصل شود. اگر فقط `subject_id REFERENCES
    -- subjects(id)` بود، یک باگ در لایه سرویس می‌توانست کلاس مدرسه
    -- الف را به درس مدرسه ب وصل کند و هیچ‌چیز جلویش را نمی‌گرفت.
    -- این همان «لایه دفاعی سوم» است: عمق بیشتر از دو لایه‌ای که
    -- docs/multi-tenancy.md الزام کرده، نه جایگزین آن‌ها.
    --
    -- ⚠️ ستون‌های رشته‌ای و ترتیب: tenant_id اول می‌آید تا با
    -- قاعده «tenant_id همیشه اولین ستون» (docs/database.md بخش ۱)
    -- هم‌راستا باشد.
    --
    -- ⚠️ ON DELETE SET NULL (subject_id) — پستگرس ۱۵ به بعد. بدون
    -- فهرست ستون، پستگرس هر دو ستون را NULL می‌کرد و چون tenant_id
    -- NOT NULL است، حذف درس شکست می‌خورد.
    CONSTRAINT fk_classrooms_subject
        FOREIGN KEY (tenant_id, subject_id) REFERENCES subjects (tenant_id, id)
        ON DELETE SET NULL (subject_id),

    -- معلمِ سرکلاس از سطح پلتفرم است (users)، پس این FK ترکیبی
    -- نمی‌تواند باشد: یک کاربر می‌تواند در چند مستأجر معلم باشد و
    -- هیچ ستون tenant_id در users وجود ندارد.
    CONSTRAINT fk_classrooms_homeroom_teacher
        FOREIGN KEY (homeroom_teacher_id) REFERENCES users(id) ON DELETE SET NULL
);

-- یکتایی نام کلاس در هر سال تحصیلی.
-- ⚠️ coalesce لازم است: در ایندکس یکتا، NULL با هیچ مقدار دیگری
-- «برابر» شمرده نمی‌شود؛ پس بدون آن، دو کلاس هم‌نام با سال تحصیلی
-- خالی هر دو ثبت می‌شدند.
CREATE UNIQUE INDEX uk_classrooms_name_year
    ON classrooms (tenant_id, lower(name), coalesce(academic_year, ''))
    WHERE deleted_at IS NULL;

-- قاعده docs/database.md بخش ۱: tenant_id همیشه اولین ستون ایندکس ترکیبی.
CREATE INDEX idx_classrooms_tenant         ON classrooms (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_classrooms_tenant_teacher ON classrooms (tenant_id, homeroom_teacher_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_classrooms_tenant_subject ON classrooms (tenant_id, subject_id) WHERE deleted_at IS NULL;

-- جست‌وجوی متنی فارسی روی نام کلاس (docs/api.md بخش ۷).
-- gin_trgm_ops از افزونه pg_trgm می‌آید که V1 ساخته است.
-- چرا GIN و نه B-tree: B-tree فقط پیشوند را می‌گیرد، ولی معلم
-- «الف» را می‌نویسد و انتظار «پنجم الف» را دارد.
CREATE INDEX idx_classrooms_name_trgm ON classrooms USING gin (name gin_trgm_ops);

-- ─────────────── تریگرهای updated_at ───────────────
-- همان الگوی V2. یادآوری V1: این تریگر فقط ستون updated_at را
-- دست می‌زند و هرگز version را — وگرنه قفل خوش‌بینانه کاذب می‌شکست.
CREATE TRIGGER trg_subjects_updated_at
    BEFORE UPDATE ON subjects FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_classrooms_updated_at
    BEFORE UPDATE ON classrooms FOR EACH ROW EXECUTE FUNCTION set_updated_at();
