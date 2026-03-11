-- Module P11: Operational Intelligence

CREATE TABLE IF NOT EXISTS liquidity_alerts (
    id BIGSERIAL PRIMARY KEY,
    branch_id BIGINT NOT NULL REFERENCES branches(id),
    fund_id BIGINT REFERENCES funds(id),
    predicted_depletion_at TIMESTAMPTZ,
    current_balance DECIMAL(20,4),
    predicted_balance_48h DECIMAL(20,4),
    alert_severity VARCHAR(20) NOT NULL,
    acknowledged_by BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS disputes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    transaction_id BIGINT REFERENCES transactions(id),
    dispute_type VARCHAR(30) NOT NULL,
    description VARCHAR(512),
    evidence_url VARCHAR(512),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    assigned_branch_id BIGINT REFERENCES branches(id),
    assigned_to BIGINT REFERENCES users(id),
    sla_deadline TIMESTAMPTZ,
    resolution_notes VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS translations (
    id BIGSERIAL PRIMARY KEY,
    language_code VARCHAR(5) NOT NULL,
    message_key VARCHAR(200) NOT NULL,
    message_value TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users(id),
    UNIQUE(language_code, message_key)
);
