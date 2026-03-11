-- Add details column to audit_logs for extended audit information
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS details VARCHAR(512);

-- Add currency_code, idempotency_key, version to transactions
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS currency_code VARCHAR(5) DEFAULT 'USD';
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
CREATE UNIQUE INDEX IF NOT EXISTS idx_transactions_idempotency_key ON transactions(idempotency_key) WHERE idempotency_key IS NOT NULL;
