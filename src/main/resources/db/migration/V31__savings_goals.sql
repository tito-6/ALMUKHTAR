CREATE TABLE IF NOT EXISTS savings_goals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(120) NOT NULL,
    target_amount NUMERIC(20,4) NOT NULL,
    saved_amount NUMERIC(20,4) NOT NULL DEFAULT 0,
    currency VARCHAR(10) NOT NULL,
    deadline DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    auto_sweep BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0
);
