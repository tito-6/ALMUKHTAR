CREATE TABLE IF NOT EXISTS recurring_transfers (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES users(id),
    destination_user_id BIGINT NOT NULL,
    amount NUMERIC(20,4) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    next_run_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0
);
