-- ═══════════════════════════════════════════════════════════
--  V10 — دانش‌آموزان و ثبت‌نام در کلاس (دامنه آموزش — گام ۲ از فاز ۳)
--
--  ⚠️ گام ۱ (V8/V9) عمداً درس‌ها و کلاس‌ها را تنها گذاشت. §۸۰ می‌گوید
--  چند Feature بزرگ هم‌زمان و بدون تست پیاده نشوند؛ دانش‌آموز و
--  ثبت‌نام واحد بعدی است و دروازه خودش را دارد.
-- ═══════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════
--  گام نخست: پیش‌نیازی که V8 جا گذاشت
-- ═══════════════════════════════════════════════════════════

-- 🔴 پستگرس فقط می‌تواند به یک UNIQUE یا PRIMARY KEY ارجاع دهد.
--  V8 روی `subjects` قید `uk_subjects_tenant_id UNIQUE (tenant_id, id)`
--  را گذاشت تا `classrooms` بتواند کلید خارجی ترکیبی بسازد — ولی خودِ
--  `classrooms` چنین قیدی نگرفت، چون در آن زمان هیچ‌کس به آن ارجاع
--  نمی‌داد. الان `class_students` می‌خواهد، پس باید اضافه شود.
--
--  ⚠️ این «تکرار» PK نیست: PK فقط `id` است و `tenant_id` را در خود
--  ندارد. بدون این قید، ارجاع `class_students` می‌توانست از مرز
--  مستأجر عبور کند.
ALTER TABLE classrooms
    ADD CONSTRAINT uk_classrooms_tenant_id UNIQUE (tenant_id, id);

-- ═══════════════════════════════════════════════════════════
--  students
-- ═══════════════════════════════════════════════════════════

CREATE TABLE students (
    id              UUID         PRIMARY KEY,
    tenant_id       UUID         NOT NULL,
    full_name       VARCHAR(150) NOT NULL,

    -- ⚠️ کد ملی در ایران یکتاست، ولی اینجا «یکتا در مستأجر» است:
    --  یکتایی سراسری یعنی دو مدرسه مختلف نمی‌توانند دانش‌آموز مشترکی
    --  داشته باشند، که با مدل چندمستأجری در تضاد است. (توجه: کد ملی
    --  ایرانی وابسته به یک رقم کنترلی است، ولی اعتبارسنجی‌اش کار لایه
    --  سرویس است، نه CHECK دیتابیس — چون فرمت‌های قدیمی هم وجود دارد.)
    national_code   VARCHAR(20)  NULL,

    guardian_name   VARCHAR(150) NULL,
    guardian_mobile VARCHAR(20)  NULL,
    birth_date      DATE         NULL,
    gender          VARCHAR(10)  NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',

    -- ⚠️ JSONB برای داده‌هایی که هنوز شکل مشخصی ندارند. عمداً هیچ
    --  فیلد پرکاربردی اینجا نیست: چیزی که در کوئری و ایندکس لازم است
    --  باید ستون نوع‌دار باشد، وگرنه جست‌وجو رویش پیمایش کامل می‌شود.
    metadata        JSONB        NULL,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      UUID         NULL,
    updated_by      UUID         NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMPTZ  NULL,

    CONSTRAINT fk_students_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,

    CONSTRAINT chk_students_status   CHECK (status IN ('ACTIVE','INACTIVE','GRADUATED')),
    CONSTRAINT chk_students_gender   CHECK (gender IS NULL OR gender IN ('MALE','FEMALE')),

    -- هدف کلید خارجی ترکیبی class_students — همان الگوی subjects.
    CONSTRAINT uk_students_tenant_id UNIQUE (tenant_id, id)
);

-- یکتایی کد ملی، فقط بین دانش‌آموزان فعال و فقط وقتی کد داده شده.
-- ⚠️ `deleted_at IS NULL` عمدی است: دانش‌آموزی که حذف نرم شده نباید
--  کد ملی‌اش را برای همیشه رزرو کند. وگرنه معلمی که اشتباهی دانش‌آموز
--  را حذف و دوباره اضافه می‌کند، به قید دیتابیس می‌خورد.
CREATE UNIQUE INDEX uk_students_national_code
    ON students (tenant_id, national_code)
    WHERE national_code IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_students_tenant      ON students (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_students_tenant_name ON students (tenant_id, full_name);

-- جست‌وجوی متنی فارسی روی نام.
-- ⚠️ درس V9: ایندکس روی `lower(full_name)` است، نه `full_name` خام.
--  کوئری سرویس `lower(s.full_name) like :pattern` است و پستگرس آن را
--  به ایندکس روی ستون خام نگاشت نمی‌کند. V8 این اشتباه را کرد و V9
--  اصلاحش کرد؛ اینجا از همان ابتدا درست ساخته می‌شود.
CREATE INDEX idx_students_name_trgm
    ON students USING gin (lower(full_name) gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_students_updated_at
    BEFORE UPDATE ON students FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ═══════════════════════════════════════════════════════════
--  class_students — جدول اتصالی ثبت‌نام
-- ═══════════════════════════════════════════════════════════

-- ⚠️ این جدول برخلاف بقیه، ستون `deleted_at` ندارد و عمدی است:
--  «ثبت‌نام» یک رابطه جاری است، نه یک دارایی. برداشتن دانش‌آموز از
--  کلاس یعنی خودِ رابطه تمام شده، نه اینکه حذف نرم شده باشد. تاریخچه
--  دوره‌های ثبت‌نام (اگر روزی لازم شد) یک جدول گزارش جدا می‌خواهد،
--  نه یک ستون روی این جدول که هر کوئری را وادار به فیلترش کند.
--
--  ⚠️ این تصمیم با ERD هم‌خوان است: در docs/database.md بخش ۳ هم
--  CLASS_STUDENTS ستون deleted_at ندارد، برخلاف STUDENTS و CLASSROOMS.
CREATE TABLE class_students (
    id           UUID        PRIMARY KEY,
    tenant_id    UUID        NOT NULL,
    classroom_id UUID        NOT NULL,
    student_id   UUID        NOT NULL,

    roll_number  VARCHAR(20) NULL,
    enrolled_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   UUID        NULL,
    updated_by   UUID        NULL,
    version      BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT fk_class_students_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,

    -- 🔴 کلیدهای خارجی ترکیبی — همان الگوی `classrooms.subject_id`.
    --  این دو قید تضمین می‌کنند یک ردیف ثبت‌نام نتواند کلاس یا
    --  دانش‌آموزِ مستأجر دیگری را به هم بچسباند، حتی اگر لایه سرویس
    --  باگ داشته باشد.
    CONSTRAINT fk_class_students_classroom
        FOREIGN KEY (tenant_id, classroom_id) REFERENCES classrooms (tenant_id, id)
        ON DELETE CASCADE,
    CONSTRAINT fk_class_students_student
        FOREIGN KEY (tenant_id, student_id) REFERENCES students (tenant_id, id)
        ON DELETE CASCADE,

    -- یک دانش‌آموز در یک کلاس دوبار ثبت‌نام نمی‌شود.
    --  ⚠️ `roll_number` عمداً اینجا نیست: شماره حضور می‌تواند در دو
    --  کلاس مختلف یکی باشد (شماره ۱ کلاس الف و شماره ۱ کلاس ب).
    CONSTRAINT uk_class_students UNIQUE (tenant_id, classroom_id, student_id)
);

-- ⚠️ اینجا برخلاف V8 هیچ ایندکس تری‌گرام ساخته نمی‌شود: روی این جدول
--  جست‌وجوی متنی وجود ندارد، فقط «چه کسانی در این کلاس‌اند».
CREATE INDEX idx_class_students_classroom ON class_students (tenant_id, classroom_id);
CREATE INDEX idx_class_students_student   ON class_students (tenant_id, student_id);

-- ⚠️ تریگر updated_at لازم نیست: تنها ستون قابل تغییر این جدول
--  `roll_number` است و رکورد به‌ندرت ویرایش می‌شود. اگر روزی ویرایش
--  پرتکرار شد، این یادداشت جای اضافه‌کردنش است. (بقیه جدول‌ها تریگر
--  دارند چون ویرایش در آن‌ها مسیر عادی است، نه استثنا.)
