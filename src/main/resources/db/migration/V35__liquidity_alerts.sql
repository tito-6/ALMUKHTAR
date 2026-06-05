CREATE TABLE IF NOT EXISTS liquidity_alerts (
    id BIGSERIAL PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    alert_type VARCHAR(30) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    current_balance NUMERIC(20,4),
    threshold_breached NUMERIC(20,4),
    message VARCHAR(500),
    resolved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);
