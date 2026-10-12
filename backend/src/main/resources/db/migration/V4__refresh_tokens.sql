-- ═══════════════════════════════════════════════════════════
--  V4 — توکن تازه‌سازی (Refresh Token)
--
--  طراحی بر پایه docs/security.md بخش ۳:
--   • توکن خام هرگز ذخیره نمی‌شود — فقط SHA-256 آن
--   • چرخش: هر استفاده، توکن قبلی را باطل می‌کند
--   • تشخیص استفاده مجدد: باطل‌سازی کل «خانواده» توکن
-- ═══════════════════════════════════════════════════════════

CREATE TABLE refresh_tokens (
    id               UUID         PRIMARY KEY,
    user_id          UUID         NOT NULL,
    tenant_id        UUID         NULL,
    -- SHA-256 به‌صورت hex — ۶۴ کاراکتر
    token_hash       VARCHAR(64)  NOT NULL,
    -- همه توکن‌های یک زنجیره چرخش یک family_id دارند
    family_id        UUID         NOT NULL,
    scope            VARCHAR(20)  NOT NULL DEFAULT 'MINIAPP',
    expires_at       TIMESTAMPTZ  NOT NULL,
    revoked_at       TIMESTAMPTZ  NULL,
    revoked_reason   VARCHAR(100) NULL,
    replaced_by_id   UUID         NULL,
    ip_address       VARCHAR(45)  NULL,
    user_agent       VARCHAR(500) NULL,

    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- قفل خوش‌بینانه: اگر دو درخواست هم‌زمان همین توکن را مصرف کنند،
    -- فقط یکی موفق می‌شود و دیگری با خطای نسخه رد می‌گردد. این دقیقاً
    -- همان حالتی است که «استفاده مجدد» باید تشخیص دهد، پس فعال‌بودنش
    -- یک مزیت امنیتی است، نه فقط سازگاری با BaseEntity.
    version          BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT chk_refresh_scope      CHECK (scope IN ('MINIAPP','ADMIN')),
    CONSTRAINT fk_refresh_user        FOREIGN KEY (user_id)        REFERENCES users(id)          ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tenant      FOREIGN KEY (tenant_id)      REFERENCES tenants(id)        ON DELETE CASCADE,
    CONSTRAINT fk_refresh_replaced_by FOREIGN KEY (replaced_by_id) REFERENCES refresh_tokens(id) ON DELETE SET NULL
);

CREATE INDEX idx_refresh_tokens_user   ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
-- برای جاروی دوره‌ای توکن‌های منقضی
CREATE INDEX idx_refresh_tokens_active ON refresh_tokens (expires_at) WHERE revoked_at IS NULL;

CREATE TRIGGER trg_refresh_tokens_updated_at
    BEFORE UPDATE ON refresh_tokens FOR EACH ROW EXECUTE FUNCTION set_updated_at();
