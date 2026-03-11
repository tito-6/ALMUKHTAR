-- Module P3: Bulk Batch Transfer Engine

CREATE TABLE IF NOT EXISTS batch_jobs (
    id BIGSERIAL PRIMARY KEY,
    submitted_by BIGINT NOT NULL REFERENCES users(id),
    batch_type VARCHAR(20) NOT NULL DEFAULT 'CSV_UPLOAD',
    filename VARCHAR(255),
    total_rows INT NOT NULL DEFAULT 0,
    processed_rows INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    total_amount_usd DECIMAL(20,4) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'VALIDATING',
    approved_by BIGINT REFERENCES users(id),
    approved_at TIMESTAMPTZ,
    source_wallet_id BIGINT REFERENCES wallets(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_batch_jobs_submitted_by ON batch_jobs(submitted_by);
CREATE INDEX IF NOT EXISTS idx_batch_jobs_status ON batch_jobs(status);

CREATE TABLE IF NOT EXISTS batch_job_rows (
    id BIGSERIAL PRIMARY KEY,
    batch_job_id BIGINT NOT NULL REFERENCES batch_jobs(id) ON DELETE CASCADE,
    row_number INT NOT NULL,
    receiver_identifier VARCHAR(128) NOT NULL,
    amount DECIMAL(20,4) NOT NULL,
    currency VARCHAR(5) NOT NULL DEFAULT 'USD',
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message VARCHAR(512),
    transaction_id BIGINT REFERENCES wallet_transactions(id),
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_batch_job_rows_batch_job_id ON batch_job_rows(batch_job_id);

CREATE TABLE IF NOT EXISTS batch_templates (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    column_mapping JSONB,
    sample_rows JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_batch_templates_owner_user_id ON batch_templates(owner_user_id);
