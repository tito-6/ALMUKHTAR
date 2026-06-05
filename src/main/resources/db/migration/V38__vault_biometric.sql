CREATE TABLE IF NOT EXISTS vault_documents (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES users(id),
    file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    document_type VARCHAR(50),
    file_size_bytes BIGINT,
    encryption_key_ref VARCHAR(255),
    uploaded_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS webauthn_credentials (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    credential_id VARCHAR(512) NOT NULL UNIQUE,
    public_key_cose BYTEA,
    sign_count BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);
