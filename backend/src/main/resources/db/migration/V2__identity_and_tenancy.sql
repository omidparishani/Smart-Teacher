-- ═══════════════════════════════════════════════════════════
--  V2 — هویت و مستأجر (Identity & Tenancy)
--
--  users و tenants سطح پلتفرم هستند و tenant_id ندارند
--  (docs/multi-tenancy.md بخش ۹): یک کاربر می‌تواند عضو چند مستأجر
--  باشد، پس نمی‌تواند به یکی گره بخورد؛ و tenants خودِ ریشه است.
--
--  ⚠️ tenant_members ستون tenant_id دارد اما موجودیت JPA آن
--  @TenantId نمی‌گیرد — به‌عمد. این جدول در زمان ورود خوانده
--  می‌شود، یعنی پیش از آنکه مستأجری معلوم باشد؛ اگر Hibernate
--  خودکار فیلترش می‌کرد، کوئری «مستأجرهای این کاربر» هیچ نتیجه‌ای
--  نمی‌داد و ورود کاربر برای همیشه می‌شکست. اسکوپ این جدول در
--  لایه سرویس و با TenantGuard اعمال می‌شود، نه با فیلتر خودکار.
--  همین استدلال برای refresh_tokens هم برقرار است.
-- ═══════════════════════════════════════════════════════════

-- ─────────────── users ───────────────
CREATE TABLE users (
    id                    UUID        PRIMARY KEY,
    username              VARCHAR(100) NULL,
    password_hash         VARCHAR(255) NULL,
    full_name             VARCHAR(200) NOT NULL,
    email                 VARCHAR(255) NULL,
    mobile                VARCHAR(20)  NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    preferred_locale      VARCHAR(10)  NOT NULL DEFAULT 'fa',
    theme                 VARCHAR(10)  NOT NULL DEFAULT 'SYSTEM',
    failed_login_attempts INTEGER      NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ  NULL,
    last_login_at         TIMESTAMPTZ  NULL,

    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by            UUID         NULL,
    updated_by            UUID         NULL,
    version               BIGINT       NOT NULL DEFAULT 0,
    deleted_at            TIMESTAMPTZ  NULL,

    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE','DISABLED','SUSPENDED','PENDING')),
    CONSTRAINT chk_users_theme  CHECK (theme  IN ('LIGHT','DARK','SYSTEM'))
);

-- یکتایی فقط روی رکوردهای حذف‌نشده — تا نام کاربری آزادشده قابل استفاده مجدد باشد
CREATE UNIQUE INDEX uk_users_username ON users (lower(username)) WHERE username IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX uk_users_email    ON users (lower(email))    WHERE email    IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX uk_users_mobile   ON users (mobile)          WHERE mobile   IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_users_status ON users (status) WHERE deleted_at IS NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_users_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL;

-- ─────────────── tenants ───────────────
CREATE TABLE tenants (
    id               UUID         PRIMARY KEY,
    type             VARCHAR(20)  NOT NULL,
    name             VARCHAR(200) NOT NULL,
    slug             VARCHAR(100) NOT NULL,
    parent_tenant_id UUID         NULL,
    owner_user_id    UUID         NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    settings         JSONB        NOT NULL DEFAULT '{}'::jsonb,

    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       UUID         NULL,
    updated_by       UUID         NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    deleted_at       TIMESTAMPTZ  NULL,

    CONSTRAINT chk_tenants_type   CHECK (type   IN ('TEACHER','SCHOOL','ORGANIZATION')),
    CONSTRAINT chk_tenants_status CHECK (status IN ('ACTIVE','SUSPENDED','ARCHIVED')),
    CONSTRAINT fk_tenants_parent  FOREIGN KEY (parent_tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tenants_owner   FOREIGN KEY (owner_user_id)    REFERENCES users(id)   ON DELETE RESTRICT,
    -- یک مستأجر نمی‌تواند والد خودش باشد (محافظت سطح اول؛ چرخه‌های عمیق‌تر در سرویس بررسی می‌شوند)
    CONSTRAINT chk_tenants_not_self_parent CHECK (parent_tenant_id IS NULL OR parent_tenant_id <> id)
);

CREATE UNIQUE INDEX uk_tenants_slug ON tenants (slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_tenants_parent ON tenants (parent_tenant_id);
CREATE INDEX idx_tenants_owner  ON tenants (owner_user_id);
CREATE INDEX idx_tenants_type   ON tenants (type) WHERE deleted_at IS NULL;

-- ─────────────── tenant_members ───────────────
CREATE TABLE tenant_members (
    id         UUID        PRIMARY KEY,
    tenant_id  UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    role       VARCHAR(30) NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID        NULL,
    updated_by UUID        NULL,
    version    BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT uk_tenant_members      UNIQUE (tenant_id, user_id),
    CONSTRAINT chk_tm_role            CHECK (role   IN ('OWNER','SCHOOL_ADMIN','TEACHER','ASSISTANT','STUDENT')),
    CONSTRAINT chk_tm_status          CHECK (status IN ('ACTIVE','INVITED','SUSPENDED','REMOVED')),
    CONSTRAINT fk_tm_tenant           FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_tm_user             FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE
);

CREATE INDEX idx_tenant_members_user   ON tenant_members (user_id);
CREATE INDEX idx_tenant_members_tenant ON tenant_members (tenant_id);

-- ─────────────── eitaa_identities ───────────────
-- جدول جدا تا users آلوده به جزئیات ایتا نشود و افزودن
-- پیام‌رسان بعدی (فاز آینده) بدون تغییر users ممکن باشد.
--
-- ⚠️ eitaa_user_id از نوع BIGINT است: مستندات رسمی ایتا می‌گوید
-- user.id تا ۵۲ بیت اهمیت دارد (بیش از ظرفیت INT).
CREATE TABLE eitaa_identities (
    id             UUID        PRIMARY KEY,
    user_id        UUID        NOT NULL,
    eitaa_user_id  BIGINT      NOT NULL,
    eitaa_username VARCHAR(100) NULL,
    first_name     VARCHAR(200) NULL,
    last_name      VARCHAR(200) NULL,
    language_code  VARCHAR(10)  NULL,
    is_premium     BOOLEAN     NOT NULL DEFAULT FALSE,
    phone_number   VARCHAR(20)  NULL,
    phone_verified BOOLEAN     NOT NULL DEFAULT FALSE,
    linked_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at   TIMESTAMPTZ NULL,

    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    version        BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT uk_eitaa_identities_user_id UNIQUE (eitaa_user_id),
    CONSTRAINT fk_eitaa_identities_user    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_eitaa_identities_user ON eitaa_identities (user_id);

-- ─────────────── تریگرهای updated_at ───────────────
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_tenants_updated_at
    BEFORE UPDATE ON tenants FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_tenant_members_updated_at
    BEFORE UPDATE ON tenant_members FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_eitaa_identities_updated_at
    BEFORE UPDATE ON eitaa_identities FOR EACH ROW EXECUTE FUNCTION set_updated_at();
