CREATE TABLE IF NOT EXISTS qr_tokens (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  BIGINT NOT NULL REFERENCES transactions(id),
    token_hash      VARCHAR(256) NOT NULL UNIQUE,
    totp_secret     VARCHAR(128) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    scanned_at      TIMESTAMPTZ,
    scanned_by      BIGINT REFERENCES users(id),
    branch_id       BIGINT REFERENCES branches(id),
    is_used         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_qr_tokens_transaction ON qr_tokens(transaction_id);
CREATE INDEX IF NOT EXISTS idx_qr_tokens_expires ON qr_tokens(expires_at) WHERE is_used = FALSE;
