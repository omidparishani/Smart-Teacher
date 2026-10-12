-- ═══════════════════════════════════════════════════════════
--  V3 — نقش‌ها و مجوزها (RBAC)
--
--  مدل مجوز «Resource:Action» است و در دیتابیس نگهداری می‌شود،
--  پس افزودن نقش سفارشی در آینده بدون تغییر کد ممکن است.
-- ═══════════════════════════════════════════════════════════

-- ─────────────── roles ───────────────
CREATE TABLE roles (
    id          UUID         PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    level       VARCHAR(20)  NOT NULL DEFAULT 'TENANT',
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_roles_code  UNIQUE (code),
    CONSTRAINT chk_roles_level CHECK (level IN ('PLATFORM','TENANT'))
);

-- ─────────────── permissions ───────────────
CREATE TABLE permissions (
    id          UUID         PRIMARY KEY,
    code        VARCHAR(100) NOT NULL,
    resource    VARCHAR(50)  NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    description VARCHAR(500) NULL,

    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_permissions_code UNIQUE (code)
);

CREATE INDEX idx_permissions_resource ON permissions (resource);

-- ─────────────── role_permissions ───────────────
CREATE TABLE role_permissions (
    id            UUID PRIMARY KEY,
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,

    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_role_permissions UNIQUE (role_id, permission_id),
    CONSTRAINT fk_rp_role       FOREIGN KEY (role_id)       REFERENCES roles(id)       ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_role ON role_permissions (role_id);

-- ─────────────── user_roles ───────────────
-- tenant_id = NULL یعنی نقش پلتفرم‌سطح (SUPER_ADMIN / ADMIN)
CREATE TABLE user_roles (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL,
    role_id    UUID        NOT NULL,
    tenant_id  UUID        NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by UUID        NULL,

    CONSTRAINT fk_ur_user   FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE,
    CONSTRAINT fk_ur_role   FOREIGN KEY (role_id)   REFERENCES roles(id)   ON DELETE CASCADE,
    CONSTRAINT fk_ur_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

-- در PostgreSQL مقادیر NULL در UNIQUE با هم «متمایز» شمرده می‌شوند،
-- پس برای پوشش هر دو حالت به دو ایندکس جزئی نیاز است.
CREATE UNIQUE INDEX uk_user_roles_scoped ON user_roles (user_id, role_id, tenant_id) WHERE tenant_id IS NOT NULL;
CREATE UNIQUE INDEX uk_user_roles_global ON user_roles (user_id, role_id)             WHERE tenant_id IS NULL;
CREATE INDEX idx_user_roles_user ON user_roles (user_id);

-- ─────────────── تریگرها ───────────────
CREATE TRIGGER trg_roles_updated_at
    BEFORE UPDATE ON roles FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_permissions_updated_at
    BEFORE UPDATE ON permissions FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ═══════════════════════════════════════════════════════════
--  Seed — نقش‌های سیستمی
--  شناسه‌های ثابت و خوانا تا در Migrationهای بعدی و تست‌ها
--  بتوان به آن‌ها ارجاع داد.
-- ═══════════════════════════════════════════════════════════
INSERT INTO roles (id, code, name, description, level, is_system) VALUES
('10000000-0000-7000-8000-000000000001', 'SUPER_ADMIN',  'مدیر ارشد سامانه', 'دسترسی کامل به همه بخش‌های پلتفرم',            'PLATFORM', TRUE),
('10000000-0000-7000-8000-000000000002', 'ADMIN',        'مدیر عملیاتی',     'عملیات روزمره، بدون تغییرات بحرانی',           'PLATFORM', TRUE),
('10000000-0000-7000-8000-000000000003', 'SCHOOL_ADMIN', 'مدیر مدرسه',       'مدیریت معلمان مدرسه و مشاهده آمار تجمیعی',    'TENANT',   TRUE),
('10000000-0000-7000-8000-000000000004', 'TEACHER',      'معلم',             'مدیریت کلاس‌ها، دانش‌آموزان، تکالیف و آزمون‌ها', 'TENANT',   TRUE),
('10000000-0000-7000-8000-000000000005', 'STUDENT',      'دانش‌آموز',        'مشاهده و ارسال تکالیف و شرکت در آزمون',        'TENANT',   TRUE);


-- ═══════════════════════════════════════════════════════════
--  Seed — مجوزها
-- ═══════════════════════════════════════════════════════════
INSERT INTO permissions (id, code, resource, action, description) VALUES
-- کلاس
('20000000-0000-7000-8000-000000000001', 'CLASS:CREATE',         'CLASS',        'CREATE',           'ایجاد کلاس'),
('20000000-0000-7000-8000-000000000002', 'CLASS:READ',           'CLASS',        'READ',             'مشاهده کلاس'),
('20000000-0000-7000-8000-000000000003', 'CLASS:UPDATE',         'CLASS',        'UPDATE',           'ویرایش کلاس'),
('20000000-0000-7000-8000-000000000004', 'CLASS:DELETE',         'CLASS',        'DELETE',           'حذف کلاس'),
-- دانش‌آموز
('20000000-0000-7000-8000-000000000005', 'STUDENT:CREATE',       'STUDENT',      'CREATE',           'افزودن دانش‌آموز'),
('20000000-0000-7000-8000-000000000006', 'STUDENT:READ',         'STUDENT',      'READ',             'مشاهده دانش‌آموز'),
('20000000-0000-7000-8000-000000000007', 'STUDENT:UPDATE',       'STUDENT',      'UPDATE',           'ویرایش دانش‌آموز'),
('20000000-0000-7000-8000-000000000008', 'STUDENT:DELETE',       'STUDENT',      'DELETE',           'حذف دانش‌آموز'),
-- تکلیف
('20000000-0000-7000-8000-000000000009', 'ASSIGNMENT:CREATE',    'ASSIGNMENT',   'CREATE',           'ایجاد تکلیف'),
('20000000-0000-7000-8000-000000000010', 'ASSIGNMENT:GRADE',     'ASSIGNMENT',   'GRADE',            'تصحیح و نمره‌دهی تکلیف'),
('20000000-0000-7000-8000-000000000011', 'ASSIGNMENT:SUBMIT',    'ASSIGNMENT',   'SUBMIT',           'ارسال پاسخ تکلیف'),
-- آزمون
('20000000-0000-7000-8000-000000000012', 'EXAM:CREATE',          'EXAM',         'CREATE',           'ایجاد آزمون'),
('20000000-0000-7000-8000-000000000013', 'EXAM:GRADE',           'EXAM',         'GRADE',            'تصحیح آزمون'),
('20000000-0000-7000-8000-000000000014', 'EXAM:TAKE',            'EXAM',         'TAKE',             'شرکت در آزمون'),
-- کتاب
('20000000-0000-7000-8000-000000000015', 'BOOK:UPLOAD',          'BOOK',         'UPLOAD',           'بارگذاری کتاب'),
('20000000-0000-7000-8000-000000000016', 'BOOK:ANALYZE',         'BOOK',         'ANALYZE',          'تحلیل کتاب با AI'),
-- هوش مصنوعی
('20000000-0000-7000-8000-000000000017', 'AI:USE',               'AI',           'USE',              'استفاده از دستیار هوشمند'),
('20000000-0000-7000-8000-000000000018', 'AI:CONFIGURE',         'AI',           'CONFIGURE',        'پیکربندی Provider و مدل'),
-- پرامپت
('20000000-0000-7000-8000-000000000019', 'PROMPT:CREATE',        'PROMPT',       'CREATE',           'ساخت و ویرایش پرامپت'),
('20000000-0000-7000-8000-000000000020', 'PROMPT:PUBLISH',       'PROMPT',       'PUBLISH',          'انتشار نسخه پرامپت'),
('20000000-0000-7000-8000-000000000021', 'PROMPT:ASSIGN',        'PROMPT',       'ASSIGN',           'تخصیص پرامپت به سطح'),
-- Provider
('20000000-0000-7000-8000-000000000022', 'PROVIDER:CREATE',      'PROVIDER',     'CREATE',           'افزودن Provider'),
('20000000-0000-7000-8000-000000000023', 'PROVIDER:UPDATE',      'PROVIDER',     'UPDATE',           'ویرایش Provider'),
('20000000-0000-7000-8000-000000000024', 'PROVIDER:VIEW_KEY',    'PROVIDER',     'VIEW_KEY',         'مشاهده کلید Provider — بحرانی'),
-- پلن و اشتراک
('20000000-0000-7000-8000-000000000025', 'PLAN:CREATE',          'PLAN',         'CREATE',           'ایجاد پلن'),
('20000000-0000-7000-8000-000000000026', 'PLAN:UPDATE',          'PLAN',         'UPDATE',           'ویرایش قیمت و امکانات پلن — بحرانی'),
('20000000-0000-7000-8000-000000000027', 'SUBSCRIPTION:MANAGE',  'SUBSCRIPTION', 'MANAGE',           'مدیریت اشتراک کاربران'),
-- پرداخت
('20000000-0000-7000-8000-000000000028', 'PAYMENT:VIEW',         'PAYMENT',      'VIEW',             'مشاهده پرداخت‌ها'),
('20000000-0000-7000-8000-000000000029', 'PAYMENT:REFUND',       'PAYMENT',      'REFUND',           'بازگرداندن وجه — بحرانی'),
-- پلتفرم
('20000000-0000-7000-8000-000000000030', 'USER:MANAGE',          'USER',         'MANAGE',           'مدیریت کاربران'),
('20000000-0000-7000-8000-000000000031', 'TENANT:MANAGE',        'TENANT',       'MANAGE',           'مدیریت مستأجرها و مدارس'),
('20000000-0000-7000-8000-000000000032', 'AUDIT:VIEW',           'AUDIT',        'VIEW',             'مشاهده لاگ حسابرسی'),
('20000000-0000-7000-8000-000000000033', 'SYSTEM:CONFIGURE',     'SYSTEM',       'CONFIGURE',        'تنظیمات سطح سامانه — بحرانی');


-- ═══════════════════════════════════════════════════════════
--  Seed — نگاشت نقش به مجوز
--
--  شناسه این جدول اتصالی تولید می‌شود (gen_random_uuid) چون
--  هیچ‌جا به آن ارجاع داده نمی‌شود و صرفاً یک ردیف نگاشت است.
-- ═══════════════════════════════════════════════════════════

-- SUPER_ADMIN — همه مجوزها
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN';

-- ADMIN — همه به‌جز موارد بحرانی (کلید، قیمت، تنظیمات سامانه)
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code NOT IN ('PROVIDER:VIEW_KEY', 'PLAN:UPDATE', 'SYSTEM:CONFIGURE');

-- SCHOOL_ADMIN — مدیریت معلمان مدرسه و آمار تجمیعی
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SCHOOL_ADMIN'
  AND p.code IN (
      'CLASS:READ', 'STUDENT:READ', 'STUDENT:CREATE',
      'USER:MANAGE', 'AUDIT:VIEW', 'TENANT:MANAGE'
  );

-- TEACHER — دامنه آموزشی خودش
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'TEACHER'
  AND p.code IN (
      'CLASS:CREATE', 'CLASS:READ', 'CLASS:UPDATE', 'CLASS:DELETE',
      'STUDENT:CREATE', 'STUDENT:READ', 'STUDENT:UPDATE', 'STUDENT:DELETE',
      'ASSIGNMENT:CREATE', 'ASSIGNMENT:GRADE',
      'EXAM:CREATE', 'EXAM:GRADE',
      'BOOK:UPLOAD', 'BOOK:ANALYZE',
      'AI:USE', 'PROMPT:CREATE',
      'PAYMENT:VIEW'
  );

-- STUDENT — فقط تکالیف و آزمون‌های خودش
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.code = 'STUDENT'
  AND p.code IN ('ASSIGNMENT:SUBMIT', 'EXAM:TAKE', 'CLASS:READ');
