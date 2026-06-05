CREATE TABLE IF NOT EXISTS branch_hourly_stats (
    id BIGSERIAL PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    hour INT NOT NULL,
    transaction_count INT DEFAULT 0,
    total_volume NUMERIC(20,4) DEFAULT 0,
    avg_wait_minutes NUMERIC(10,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);
