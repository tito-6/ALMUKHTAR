-- Idempotent financial notification deliveries (prod / Flyway)
CREATE TABLE IF NOT EXISTS notification_dispatch_log (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(512) NOT NULL UNIQUE,
    event_type VARCHAR(80) NOT NULL,
    recipient_user_id BIGINT,
    template_key VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_dispatch_event ON notification_dispatch_log(event_type);

CREATE TABLE IF NOT EXISTS cash_out_requests (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL REFERENCES wallets(id),
    branch_id BIGINT NOT NULL REFERENCES branches(id),
    amount DECIMAL(20,4) NOT NULL,
    currency VARCHAR(5) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    cashier_id BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_cash_out_branch_status ON cash_out_requests(branch_id, status);
