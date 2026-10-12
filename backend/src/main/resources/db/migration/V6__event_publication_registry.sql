-- ═══════════════════════════════════════════════════════════
--  V6 — رجیستری انتشار رویداد (Spring Modulith)
-- ═══════════════════════════════════════════════════════════
--
-- چرا این جدول در مهاجرت ما است و نه ساخته‌شده توسط خود کتابخانه:
-- چون Flyway تنها منبع حقیقتِ اسکیما در این پروژه است و
-- `ddl-auto: validate` بالا می‌آید. اگر جدولی را که Hibernate
-- انتظار دارد در مهاجرت‌ها نسازیم، کانتکست اصلاً بالا نمی‌آید
-- (خطای «Schema validation: missing table»).
--
-- ⚠️ تفاوت JPA و JDBC در Modulith:
-- کتابخانه دو پیاده‌سازی دارد و اسکیمای هر دو یکی نیست.
-- ما از نسخه‌ی JPA استفاده می‌کنیم (spring-modulith-starter-jpa)،
-- پس ستون‌ها از روی خودِ موجودیت‌های همان نسخه استخراج شده‌اند،
-- نه از مستندات نسخه‌ی JDBC. کلاس‌های مرجع در نسخه ۲.۱.۱:
--   JpaEventPublication            (MappedSuperclass — ستون‌ها)
--   ...jpa.updating.DefaultJpaEventPublication   (Entity)
--   ...jpa.archiving.ArchivedJpaEventPublication (Entity — جدول آرشیو)
--
-- ⚠️ چرا TEXT و نه VARCHAR(255):
-- Hibernate برای این ستون‌ها varchar(255) تولید می‌کند (چون در
-- موجودیت طولی اعلام نشده). اما این ستون‌ها JSON رویداد را نگه
-- می‌دارند و ۲۵۵ کاراکتر قطعاً کم است — یعنی اولین رویداد با
-- payload بزرگ با خطای «value too long» شکست می‌خورد. خود
-- Modulith هم در اسکیمای رسمی PostgreSQL برای همین ستون‌ها TEXT
-- اعلام کرده است. حدود ۲۵۵ چیزی نیست که بخواهیم به آن تکیه کنیم.
-- (اینکه اعتبارسنجی Hibernate با TEXT هم سبز می‌ماند، عملاً
-- آزمایش شده و در docs/verification-log.md ثبت است.)

-- ─────────────── event_publication ───────────────
-- رویدادهای دامنه پیش از انتشار کامل اینجا ثبت می‌شوند تا اگر
-- برنامه وسط کار از کار افتاد، پس از راه‌اندازی دوباره منتشر شوند.
-- یعنی «انتشار حداقلی-یک‌بار» بدون از دست دادن رویداد.
CREATE TABLE event_publication (
    id                     UUID        PRIMARY KEY,
    listener_id            TEXT        NOT NULL,
    event_type             TEXT        NOT NULL,
    serialized_event       TEXT        NOT NULL,
    publication_date       TIMESTAMPTZ NOT NULL,
    completion_date        TIMESTAMPTZ NULL,
    -- NULL بودن status عمدی و بخشی از قرارداد است: Modulith رویدادهای
    -- ناتمام را با شرط «status IS NULL AND completion_date IS NULL»
    -- پیدا می‌کند. پس این ستون نباید NOT NULL یا DEFAULT داشته باشد.
    status                 VARCHAR(255) NULL,
    completion_attempts    INTEGER     NOT NULL DEFAULT 0,
    last_resubmission_date TIMESTAMPTZ NULL,

    -- مقادیر دقیقاً همان enum در EventPublication.Status هستند.
    -- در SQL، CHECK روی NULL «نامعلوم» می‌شود و رد نمی‌شود — پس
    -- این قید با NULL بودن status تعارضی ندارد.
    CONSTRAINT chk_event_publication_status CHECK (
        status IN ('PUBLISHED','PROCESSING','COMPLETED','FAILED','RESUBMITTED')
    )
);

-- ایندکس‌ها از روی کوئری‌های واقعی خودِ JpaEventPublicationRepository
-- انتخاب شده‌اند (استخراج‌شده از بایت‌کد نسخه ۲.۱.۱)، نه حدسی:
--
-- ۱) «رویداد تکمیل‌نشده‌ی همین listener را پیدا کن» — داغ‌ترین کوئری،
--    در هر انتشار رویداد اجرا می‌شود:
--      where p.serializedEvent = ?1 and p.listenerId = ?2
--        and p.completionDate is null
--    ایندکس جزئی است چون این کوئری همیشه completion_date IS NULL دارد
--    و در عمل فقط به رویدادهای در جریان کار دارد — یعنی ایندکس کوچک
--    می‌ماند و با بزرگ‌شدن جدول باد نمی‌کند.
CREATE INDEX idx_event_publication_incomplete
    ON event_publication (serialized_event, listener_id)
    WHERE completion_date IS NULL;

-- ۲) «همه‌ی رویدادهای ناتمام، به ترتیب انتشار» — هنگام راه‌اندازی
--    دوباره و در جاب بازفرست:
--      where p.completionDate is null order by p.publicationDate asc
CREATE INDEX idx_event_publication_pending_order
    ON event_publication (publication_date)
    WHERE completion_date IS NULL;

-- ۳) پاک‌سازی رویدادهای تمام‌شده (کوئری‌های DELETE_COMPLETED و
--    DELETE_COMPLETED_BEFORE روی completion_date شرط می‌گذارند).
CREATE INDEX idx_event_publication_completion_date
    ON event_publication (completion_date);

-- ─────────────── جدول آرشیو — عمداً ساخته نشد ───────────────
-- Modulith حالت «archive» دارد که رویدادهای تمام‌شده را به جدول
-- event_publication_archive منتقل می‌کند. آن موجودیت فقط وقتی نگاشت
-- می‌شود که حالت تکمیل روی archive تنظیم شده باشد؛ چون پیش‌فرض
-- «update» است، Hibernate آن را نمی‌خواهد و ساختن جدول بی‌مصرف
-- می‌بود. اگر روزی spring.modulith.events.completion-mode=archive
-- شد، آن‌وقت باید مهاجرت جدول آرشیو هم اضافه شود — وگرنه همان خطای
-- «missing table» برمی‌گردد.
