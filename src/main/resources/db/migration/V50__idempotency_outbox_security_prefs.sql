-- API idempotency ledger for money-moving endpoints
CREATE TABLE IF NOT EXISTS idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),
    endpoint VARCHAR(256) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_hash VARCHAR(64),
    entity_type VARCHAR(80),
    entity_id BIGINT,
    status VARCHAR(20) NOT NULL,
    response_body TEXT,
    http_status INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_idempotency_user_endpoint_key
    ON idempotency_records (user_id, endpoint, idempotency_key);

CREATE INDEX IF NOT EXISTS idx_idempotency_expires ON idempotency_records (expires_at);

-- Outbox for async notification delivery (WhatsApp) decoupled from financial commits
CREATE TABLE IF NOT EXISTS notification_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id BIGINT,
    recipient_user_id BIGINT REFERENCES users(id),
    recipient_role VARCHAR(40),
    channel VARCHAR(20) NOT NULL,
    phone_masked VARCHAR(32),
    template_key VARCHAR(120) NOT NULL,
    language_code VARCHAR(10) NOT NULL DEFAULT 'ar',
    payload_json TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error VARCHAR(2000),
    correlation_id VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_poll
    ON notification_outbox (status, next_attempt_at);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_entity
    ON notification_outbox (entity_type, entity_id);

ALTER TABLE user_notification_preferences
    ADD COLUMN IF NOT EXISTS security_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE;
