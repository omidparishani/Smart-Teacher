-- ═══════════════════════════════════════════════════════════
--  V26 — فاز ۱۰: plan_features، کوپن، کیف پول، شماره‌گذاری فاکتور،
--  رسید callback پرداخت، و seed امکانات پلن‌ها
--
--  usage_counters از قبل در V18 است؛ اینجا فقط قابل‌استفاده نگه
--  داشته می‌شود (بدون تغییر اسکیما).
-- ═══════════════════════════════════════════════════════════

-- ─────────────── plan_features ───────────────
CREATE TABLE plan_features (
    id            UUID         PRIMARY KEY,
    plan_id       UUID         NOT NULL,
    feature_code  VARCHAR(60)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    config        JSONB        NOT NULL DEFAULT '{}'::jsonb,

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version       BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT fk_plan_features_plan
        FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_plan_features_feature
        FOREIGN KEY (feature_code) REFERENCES features(code) ON DELETE CASCADE,
    CONSTRAINT uk_plan_features UNIQUE (plan_id, feature_code)
);

CREATE INDEX idx_plan_features_plan ON plan_features (plan_id);
CREATE INDEX idx_plan_features_code ON plan_features (feature_code);

CREATE TRIGGER trg_plan_features_updated_at
    BEFORE UPDATE ON plan_features FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── coupons ───────────────
CREATE TABLE coupons (
    id                      UUID          PRIMARY KEY,
    code                    VARCHAR(40)   NOT NULL,
    discount_type           VARCHAR(20)   NOT NULL,
    discount_value          NUMERIC(19,4) NOT NULL,
    max_discount_amount     NUMERIC(19,4) NULL,
    min_purchase_amount     NUMERIC(19,4) NOT NULL DEFAULT 0,
    usage_limit             INTEGER       NULL,
    usage_limit_per_tenant  INTEGER       NULL,
    used_count              INTEGER       NOT NULL DEFAULT 0,
    valid_from              TIMESTAMPTZ   NOT NULL,
    valid_until             TIMESTAMPTZ   NOT NULL,
    applicable_plan_id      UUID          NULL,
    active                  BOOLEAN       NOT NULL DEFAULT TRUE,

    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version                 BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT uk_coupons_code UNIQUE (code),
    CONSTRAINT fk_coupons_plan
        FOREIGN KEY (applicable_plan_id) REFERENCES plans(id) ON DELETE SET NULL,
    CONSTRAINT chk_coupons_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED')),
    CONSTRAINT chk_coupons_discount_value CHECK (discount_value >= 0),
    CONSTRAINT chk_coupons_used_count CHECK (used_count >= 0),
    CONSTRAINT chk_coupons_valid_range CHECK (valid_until > valid_from)
);

CREATE INDEX idx_coupons_active_window ON coupons (active, valid_from, valid_until);

CREATE TRIGGER trg_coupons_updated_at
    BEFORE UPDATE ON coupons FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── coupon_redemptions ───────────────
CREATE TABLE coupon_redemptions (
    id                UUID          PRIMARY KEY,
    coupon_id         UUID          NOT NULL,
    tenant_id         UUID          NOT NULL,
    user_id           UUID          NULL,
    subscription_id   UUID          NULL,
    invoice_id        UUID          NULL,
    discount_applied  NUMERIC(19,4) NOT NULL DEFAULT 0,
    redeemed_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),

    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version           BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT fk_coupon_redemptions_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE RESTRICT,
    CONSTRAINT fk_coupon_redemptions_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_coupon_redemptions_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_coupon_redemptions_coupon ON coupon_redemptions (coupon_id);
CREATE INDEX idx_coupon_redemptions_tenant ON coupon_redemptions (tenant_id, coupon_id);

CREATE TRIGGER trg_coupon_redemptions_updated_at
    BEFORE UPDATE ON coupon_redemptions FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── wallets ───────────────
CREATE TABLE wallets (
    id          UUID          PRIMARY KEY,
    tenant_id   UUID          NOT NULL,
    balance     NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency    VARCHAR(10)   NOT NULL DEFAULT 'IRT',

    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version     BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT uk_wallets_tenant UNIQUE (tenant_id),
    CONSTRAINT fk_wallets_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT chk_wallets_balance CHECK (balance >= 0)
);

CREATE TRIGGER trg_wallets_updated_at
    BEFORE UPDATE ON wallets FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── wallet_transactions ───────────────
CREATE TABLE wallet_transactions (
    id              UUID          PRIMARY KEY,
    wallet_id       UUID          NOT NULL,
    tenant_id       UUID          NOT NULL,
    type            VARCHAR(20)   NOT NULL,
    amount          NUMERIC(19,4) NOT NULL,
    balance_after   NUMERIC(19,4) NOT NULL,
    description     TEXT          NULL,
    reference_type  VARCHAR(40)   NULL,
    reference_id    UUID          NULL,
    created_by      UUID          NULL,

    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version         BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT fk_wallet_tx_wallet
        FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE,
    CONSTRAINT fk_wallet_tx_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT chk_wallet_tx_type CHECK (type IN (
        'PURCHASED', 'BONUS', 'PROMOTIONAL', 'CREDIT', 'DEBIT', 'REFUND', 'ADJUSTMENT')),
    CONSTRAINT chk_wallet_tx_amount CHECK (amount <> 0)
);

CREATE INDEX idx_wallet_tx_wallet ON wallet_transactions (wallet_id, created_at DESC);
CREATE INDEX idx_wallet_tx_tenant ON wallet_transactions (tenant_id, created_at DESC);

CREATE TRIGGER trg_wallet_transactions_updated_at
    BEFORE UPDATE ON wallet_transactions FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── invoice_sequences — شماره‌گذاری اتمیک ───────────────
CREATE TABLE invoice_sequences (
    year_key     INTEGER      PRIMARY KEY,
    next_value   BIGINT       NOT NULL DEFAULT 1,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ─────────────── payment_callback_receipts — idempotency ───────────────
CREATE TABLE payment_callback_receipts (
    id                 UUID          PRIMARY KEY,
    authority          VARCHAR(120)  NOT NULL,
    gateway_code       VARCHAR(40)   NOT NULL DEFAULT 'MOCK',
    subscription_id    UUID          NOT NULL,
    tenant_id          UUID          NOT NULL,
    reference_code     VARCHAR(120)  NULL,
    processed_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),

    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version            BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT uk_payment_callback_authority UNIQUE (authority),
    CONSTRAINT fk_payment_callback_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX idx_payment_callback_subscription
    ON payment_callback_receipts (subscription_id);

CREATE TRIGGER trg_payment_callback_receipts_updated_at
    BEFORE UPDATE ON payment_callback_receipts FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── Seed plan_features برای پلن‌های پیش‌فرض ───────────────
-- FREE: فقط MULTIPLE_CLASSES
-- BASIC: AI_CHAT + MULTIPLE_CLASSES
-- PRO: اکثر امکانات AI + MULTIPLE_CLASSES + ADVANCED_REPORTS
INSERT INTO plan_features (id, plan_id, feature_code, enabled) VALUES
('72000000-0000-7000-8000-000000000001',
 '70000000-0000-7000-8000-000000000001', 'MULTIPLE_CLASSES', TRUE),
('72000000-0000-7000-8000-000000000002',
 '70000000-0000-7000-8000-000000000002', 'AI_CHAT', TRUE),
('72000000-0000-7000-8000-000000000003',
 '70000000-0000-7000-8000-000000000002', 'MULTIPLE_CLASSES', TRUE),
('72000000-0000-7000-8000-000000000004',
 '70000000-0000-7000-8000-000000000003', 'AI_CHAT', TRUE),
('72000000-0000-7000-8000-000000000005',
 '70000000-0000-7000-8000-000000000003', 'AI_BOOK_ANALYSIS', TRUE),
('72000000-0000-7000-8000-000000000006',
 '70000000-0000-7000-8000-000000000003', 'AI_HOMEWORK_CHECK', TRUE),
('72000000-0000-7000-8000-000000000007',
 '70000000-0000-7000-8000-000000000003', 'AI_EXAM_GENERATION', TRUE),
('72000000-0000-7000-8000-000000000008',
 '70000000-0000-7000-8000-000000000003', 'MULTIPLE_CLASSES', TRUE),
('72000000-0000-7000-8000-000000000009',
 '70000000-0000-7000-8000-000000000003', 'ADVANCED_REPORTS', TRUE);
