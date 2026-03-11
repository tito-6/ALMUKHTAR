-- Module P9: Document Vault & WebAuthn

CREATE TABLE IF NOT EXISTS vault_documents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    filename VARCHAR(255) NOT NULL,
    doc_type VARCHAR(50),
    encrypted_path VARCHAR(512) NOT NULL,
    file_size_bytes BIGINT,
    upload_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,
    tags TEXT[],
    is_kyc_linked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS vault_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    tier VARCHAR(20) NOT NULL DEFAULT 'FREE',
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    next_billing_date DATE,
    amount_usd DECIMAL(20,4),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS webauthn_credentials (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    credential_id VARCHAR(512) NOT NULL,
    public_key VARCHAR(1024) NOT NULL,
    device_name VARCHAR(100),
    sign_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS biometric_challenges (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    challenge VARCHAR(512) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
