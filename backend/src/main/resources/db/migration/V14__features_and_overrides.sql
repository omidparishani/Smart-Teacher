-- ═══════════════════════════════════════════════════════════
--  V14 — کاتالوگ امکانات و بازنویسی‌ها (Feature Flags)
--
--  جداول features / feature_overrides / user_feature_overrides
--  در V5 نبودند (V5 فقط audit و settings). طرح از docs/billing.md.
--  V12 = files/books، V13 = AI — پس این Migration شماره ۱۴ است.
--  plan_features و پلن‌ها در فاز صورتحساب می‌آیند — اینجا فقط
--  حداقل لازم برای پنل ادمین (فهرست، پیش‌فرض، override).
-- ═══════════════════════════════════════════════════════════

-- ─────────────── features ───────────────
CREATE TABLE features (
    id              UUID         PRIMARY KEY,
    code            VARCHAR(60)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    category        VARCHAR(30)  NOT NULL,
    description     TEXT,
    default_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order      INT          NOT NULL DEFAULT 0,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_features_code UNIQUE (code),
    CONSTRAINT chk_features_category CHECK (category IN ('AI','EITAA','REPORT','MANAGEMENT'))
);

CREATE INDEX idx_features_category ON features (category);
CREATE INDEX idx_features_sort ON features (sort_order, code);

-- ─────────────── feature_overrides (سطح مستأجر) ───────────────
CREATE TABLE feature_overrides (
    id           UUID         PRIMARY KEY,
    tenant_id    UUID         NOT NULL,
    feature_code VARCHAR(60)  NOT NULL,
    enabled      BOOLEAN      NOT NULL,
    reason       TEXT,
    expires_at   TIMESTAMPTZ,
    granted_by   UUID,

    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version      BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_feature_overrides_tenant_code UNIQUE (tenant_id, feature_code),
    CONSTRAINT fk_fo_tenant  FOREIGN KEY (tenant_id)    REFERENCES tenants(id)  ON DELETE CASCADE,
    CONSTRAINT fk_fo_feature FOREIGN KEY (feature_code) REFERENCES features(code) ON DELETE CASCADE,
    CONSTRAINT fk_fo_granted FOREIGN KEY (granted_by)   REFERENCES users(id)    ON DELETE SET NULL
);

CREATE INDEX idx_feature_overrides_tenant ON feature_overrides (tenant_id);
CREATE INDEX idx_feature_overrides_code ON feature_overrides (feature_code);

-- ─────────────── user_feature_overrides (سطح کاربر) ───────────────
CREATE TABLE user_feature_overrides (
    id           UUID         PRIMARY KEY,
    user_id      UUID         NOT NULL,
    feature_code VARCHAR(60)  NOT NULL,
    enabled      BOOLEAN      NOT NULL,
    reason       TEXT,
    expires_at   TIMESTAMPTZ,
    granted_by   UUID,

    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version      BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_user_feature_overrides UNIQUE (user_id, feature_code),
    CONSTRAINT fk_ufo_user    FOREIGN KEY (user_id)      REFERENCES users(id)     ON DELETE CASCADE,
    CONSTRAINT fk_ufo_feature FOREIGN KEY (feature_code) REFERENCES features(code) ON DELETE CASCADE,
    CONSTRAINT fk_ufo_granted FOREIGN KEY (granted_by)   REFERENCES users(id)     ON DELETE SET NULL
);

CREATE INDEX idx_user_feature_overrides_user ON user_feature_overrides (user_id);
CREATE INDEX idx_user_feature_overrides_code ON user_feature_overrides (feature_code);

-- ─────────────── تریگرها ───────────────
CREATE TRIGGER trg_features_updated_at
    BEFORE UPDATE ON features FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_feature_overrides_updated_at
    BEFORE UPDATE ON feature_overrides FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_user_feature_overrides_updated_at
    BEFORE UPDATE ON user_feature_overrides FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── Seed — کاتالوگ پایه امکانات ───────────────
-- شناسه‌های ثابت تا تست‌ها و Migrationهای بعدی بتوانند ارجاع دهند.
INSERT INTO features (id, code, name, category, description, default_enabled, sort_order) VALUES
('60000000-0000-7000-8000-000000000001', 'AI_CHAT',                  'گفت‌وگو با دستیار هوشمند',     'AI',         'دستیار گفت‌وگویی معلم',                    FALSE, 10),
('60000000-0000-7000-8000-000000000002', 'AI_BOOK_ANALYSIS',         'تحلیل کتاب و صفحه',            'AI',         'تحلیل محتوای کتاب با AI',                  FALSE, 20),
('60000000-0000-7000-8000-000000000003', 'AI_HOMEWORK_CHECK',        'بررسی تکلیف با AI',            'AI',         'تصحیح و بازخورد تکلیف',                    FALSE, 30),
('60000000-0000-7000-8000-000000000004', 'AI_EXAM_GENERATION',       'تولید آزمون',                  'AI',         'تولید سؤال و آزمون',                       FALSE, 40),
('60000000-0000-7000-8000-000000000005', 'AI_FEEDBACK',              'نوشتن بازخورد',                'AI',         'بازخورد نوشتاری برای دانش‌آموز',           FALSE, 50),
('60000000-0000-7000-8000-000000000006', 'AI_STUDENT_ANALYSIS',      'تحلیل عملکرد دانش‌آموز',       'AI',         'گزارش تحلیلی پیشرفت',                      FALSE, 60),
('60000000-0000-7000-8000-000000000007', 'EITAA_GROUP_MANAGEMENT',   'مدیریت گروه‌های ایتا',         'EITAA',      'ساخت و مدیریت گروه ایتا',                  FALSE, 70),
('60000000-0000-7000-8000-000000000008', 'EITAA_CHANNEL_MANAGEMENT', 'مدیریت کانال‌های ایتا',        'EITAA',      'ساخت و مدیریت کانال ایتا',                 FALSE, 80),
('60000000-0000-7000-8000-000000000009', 'SCHEDULED_MESSAGES',       'پیام‌های زمان‌بندی‌شده',       'EITAA',      'ارسال زمان‌بندی‌شده پیام',                 FALSE, 90),
('60000000-0000-7000-8000-00000000000A', 'AUTOMATION_RULES',         'قواعد خودکارسازی',             'MANAGEMENT', 'قواعد خودکار بر اساس رویداد',              FALSE, 100),
('60000000-0000-7000-8000-00000000000B', 'ADVANCED_REPORTS',         'گزارش‌های پیشرفته',            'REPORT',     'گزارش‌های تجمیعی و صادرات',                FALSE, 110),
('60000000-0000-7000-8000-00000000000C', 'MULTIPLE_CLASSES',         'چند کلاس',                     'MANAGEMENT', 'ایجاد بیش از یک کلاس',                     TRUE,  120),
('60000000-0000-7000-8000-00000000000D', 'MULTIPLE_SCHOOLS',         'چند مدرسه',                    'MANAGEMENT', 'عضویت در چند مدرسه',                       FALSE, 130),
('60000000-0000-7000-8000-00000000000E', 'OCR',                      'تشخیص متن از تصویر',           'AI',         'OCR صفحه و تکلیف',                         FALSE, 140),
('60000000-0000-7000-8000-00000000000F', 'EXAM_ONLINE',              'آزمون آنلاین',                 'MANAGEMENT', 'برگزاری آزمون آنلاین',                     FALSE, 150);
