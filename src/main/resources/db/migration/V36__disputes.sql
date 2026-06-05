CREATE TABLE IF NOT EXISTS disputes (
    id BIGSERIAL PRIMARY KEY,
    reporter_user_id BIGINT NOT NULL REFERENCES users(id),
    transaction_id BIGINT,
    category VARCHAR(30) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    assigned_branch_id BIGINT,
    sla_met BOOLEAN,
    sla_deadline TIMESTAMP,
    resolution TEXT,
    resolved_by BIGINT,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);
