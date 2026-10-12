-- ═══════════════════════════════════════════════════════════
--  V18 — تجاری: پلن، اشتراک، فاکتور (گام ۱ از فاز ۱۰)
--
--  features از قبل در V14 است. plan_features در گام‌های بعد.
--  Seed پلن‌های پیش‌فرض — همه داده، هیچ‌کدام hardcode در کد نیست.
-- ═══════════════════════════════════════════════════════════

-- ─────────────── plans ───────────────
CREATE TABLE plans (
    id              UUID          PRIMARY KEY,
    code            VARCHAR(40)   NOT NULL,
    name            VARCHAR(150)  NOT NULL,
    description     TEXT          NULL,
    price           NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency        VARCHAR(10)   NOT NULL DEFAULT 'IRT',
    billing_period  VARCHAR(20)   NOT NULL DEFAULT 'MONTHLY',
    trial_days      INTEGER       NULL,
    is_public       BOOLEAN       NOT NULL DEFAULT TRUE,
    is_default      BOOLEAN       NOT NULL DEFAULT FALSE,
    sort_order      INTEGER       NOT NULL DEFAULT 0,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,

    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version         BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT uk_plans_code UNIQUE (code),
    CONSTRAINT chk_plans_billing_period CHECK (billing_period IN (
        'MONTHLY', 'QUARTERLY', 'YEARLY', 'LIFETIME')),
    CONSTRAINT chk_plans_price CHECK (price >= 0),
    CONSTRAINT chk_plans_trial CHECK (trial_days IS NULL OR trial_days >= 0)
);

CREATE INDEX idx_plans_public ON plans (is_public, active, sort_order);

CREATE TRIGGER trg_plans_updated_at
    BEFORE UPDATE ON plans FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── plan_limits ───────────────
CREATE TABLE plan_limits (
    id           UUID         PRIMARY KEY,
    plan_id      UUID         NOT NULL,
    metric       VARCHAR(50)  NOT NULL,
    limit_value  BIGINT       NOT NULL,
    period       VARCHAR(20)  NOT NULL DEFAULT 'MONTHLY',

    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version      BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT fk_plan_limits_plan
        FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE CASCADE,
    CONSTRAINT uk_plan_limits UNIQUE (plan_id, metric, period),
    CONSTRAINT chk_plan_limits_period CHECK (period IN (
        'TOTAL', 'DAILY', 'MONTHLY', 'YEARLY'))
);

CREATE INDEX idx_plan_limits_plan ON plan_limits (plan_id);

CREATE TRIGGER trg_plan_limits_updated_at
    BEFORE UPDATE ON plan_limits FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── subscriptions ───────────────
CREATE TABLE subscriptions (
    id                     UUID          PRIMARY KEY,
    tenant_id              UUID          NOT NULL,
    plan_id                UUID          NOT NULL,
    purchased_by_user_id   UUID          NULL,
    status                 VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    start_date             TIMESTAMPTZ   NULL,
    end_date               TIMESTAMPTZ   NULL,
    auto_renew             BOOLEAN       NOT NULL DEFAULT FALSE,
    price_paid             NUMERIC(19,4) NULL,
    discount_amount        NUMERIC(19,4) NULL,
    cancelled_at           TIMESTAMPTZ   NULL,
    cancellation_reason    TEXT          NULL,

    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by             UUID          NULL,
    updated_by             UUID          NULL,
    version                BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT fk_subscriptions_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_subscriptions_plan
        FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE RESTRICT,
    CONSTRAINT fk_subscriptions_purchaser
        FOREIGN KEY (purchased_by_user_id) REFERENCES users(id) ON DELETE SET NULL,

    CONSTRAINT chk_subscriptions_status CHECK (status IN (
        'PENDING', 'ACTIVE', 'EXPIRED', 'CANCELLED', 'SUSPENDED')),

    CONSTRAINT uk_subscriptions_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_subscriptions_tenant ON subscriptions (tenant_id);
CREATE INDEX idx_subscriptions_tenant_status ON subscriptions (tenant_id, status);
CREATE INDEX idx_subscriptions_plan ON subscriptions (plan_id);

CREATE TRIGGER trg_subscriptions_updated_at
    BEFORE UPDATE ON subscriptions FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── invoices ───────────────
CREATE TABLE invoices (
    id                UUID          PRIMARY KEY,
    tenant_id         UUID          NOT NULL,
    invoice_number    VARCHAR(40)   NOT NULL,
    subscription_id   UUID          NULL,
    subtotal          NUMERIC(19,4) NOT NULL DEFAULT 0,
    discount          NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax               NUMERIC(19,4) NOT NULL DEFAULT 0,
    total             NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency          VARCHAR(10)   NOT NULL DEFAULT 'IRT',
    status            VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    issued_at         TIMESTAMPTZ   NULL,
    due_at            TIMESTAMPTZ   NULL,
    line_items        JSONB         NULL,

    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        UUID          NULL,
    updated_by        UUID          NULL,
    version           BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT fk_invoices_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoices_subscription
        FOREIGN KEY (tenant_id, subscription_id)
        REFERENCES subscriptions (tenant_id, id) ON DELETE SET NULL (subscription_id),

    CONSTRAINT uk_invoices_number UNIQUE (invoice_number),
    CONSTRAINT chk_invoices_status CHECK (status IN (
        'DRAFT', 'ISSUED', 'PAID', 'VOID', 'REFUNDED')),

    CONSTRAINT uk_invoices_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_invoices_tenant ON invoices (tenant_id);
CREATE INDEX idx_invoices_subscription ON invoices (tenant_id, subscription_id);

CREATE TRIGGER trg_invoices_updated_at
    BEFORE UPDATE ON invoices FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── usage_counters (اختیاری گام ۱) ───────────────
CREATE TABLE usage_counters (
    id             UUID         PRIMARY KEY,
    tenant_id      UUID         NOT NULL,
    user_id        UUID         NULL,
    metric         VARCHAR(50)  NOT NULL,
    used_value     BIGINT       NOT NULL DEFAULT 0,
    limit_value    BIGINT       NOT NULL DEFAULT 0,
    period_start   TIMESTAMPTZ  NOT NULL,
    period_end     TIMESTAMPTZ  NOT NULL,
    reserved_value BIGINT       NOT NULL DEFAULT 0,

    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version        BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT fk_usage_counters_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_usage_counters_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_usage_counters_values CHECK (
        used_value >= 0 AND reserved_value >= 0),

    CONSTRAINT uk_usage_counters_period UNIQUE (
        tenant_id, user_id, metric, period_start)
);

CREATE INDEX idx_usage_counters_tenant ON usage_counters (tenant_id, metric);

CREATE TRIGGER trg_usage_counters_updated_at
    BEFORE UPDATE ON usage_counters FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ─────────────── Seed پلن‌های پیش‌فرض ───────────────
INSERT INTO plans (id, code, name, description, price, currency, billing_period,
                   trial_days, is_public, is_default, sort_order, active) VALUES
('70000000-0000-7000-8000-000000000001', 'FREE', 'رایگان',
 'پلن پایه بدون هزینه', 0, 'IRT', 'MONTHLY', NULL, TRUE, TRUE, 10, TRUE),
('70000000-0000-7000-8000-000000000002', 'BASIC', 'پایه',
 'پلن پایه با امکانات محدود', 99000, 'IRT', 'MONTHLY', 7, TRUE, FALSE, 20, TRUE),
('70000000-0000-7000-8000-000000000003', 'PRO', 'حرفه‌ای',
 'پلن حرفه‌ای معلم', 299000, 'IRT', 'MONTHLY', 7, TRUE, FALSE, 30, TRUE);

INSERT INTO plan_limits (id, plan_id, metric, limit_value, period) VALUES
('71000000-0000-7000-8000-000000000001',
 '70000000-0000-7000-8000-000000000001', 'maxAIRequests', 50, 'MONTHLY'),
('71000000-0000-7000-8000-000000000002',
 '70000000-0000-7000-8000-000000000002', 'maxAIRequests', 500, 'MONTHLY'),
('71000000-0000-7000-8000-000000000003',
 '70000000-0000-7000-8000-000000000003', 'maxAIRequests', 5000, 'MONTHLY');
