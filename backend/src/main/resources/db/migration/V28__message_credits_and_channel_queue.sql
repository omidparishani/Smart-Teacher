-- ═══════════════════════════════════════════════════════════
--  V28 — صف انتشار کانال (فاز ۹ گام ۷)
--
--  message_credits در V25 ساخته شده؛ این Migration فقط صف انتشار است.
-- ═══════════════════════════════════════════════════════════

CREATE TABLE channel_publish_queue (
    id                 UUID         PRIMARY KEY,
    tenant_id          UUID         NOT NULL,
    channel_id         UUID         NOT NULL,
    topic              VARCHAR(500) NOT NULL,
    content_text       TEXT         NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts           INTEGER      NOT NULL DEFAULT 0,
    last_attempt_at    TIMESTAMPTZ  NULL,
    error_message      TEXT         NULL,

    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by         UUID         NULL,
    updated_by         UUID         NULL,
    version            BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT fk_channel_publish_queue_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE RESTRICT,
    CONSTRAINT fk_channel_publish_queue_channel
        FOREIGN KEY (tenant_id, channel_id)
        REFERENCES eitaa_channels(tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT chk_channel_publish_queue_status
        CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT chk_channel_publish_queue_attempts CHECK (attempts >= 0),
    CONSTRAINT uk_channel_publish_queue_tenant_id UNIQUE (tenant_id, id)
);

CREATE INDEX idx_channel_publish_queue_pending
    ON channel_publish_queue (tenant_id, status)
    WHERE status = 'PENDING';

CREATE TRIGGER trg_channel_publish_queue_updated_at
    BEFORE UPDATE ON channel_publish_queue FOR EACH ROW EXECUTE FUNCTION set_updated_at();
