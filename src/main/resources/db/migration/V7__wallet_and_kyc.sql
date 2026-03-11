-- Module 7: Digital Wallet and KYC

-- Wallet applications (KYC lifecycle)
CREATE TABLE IF NOT EXISTS wallet_applications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    submitted_at TIMESTAMPTZ,
    reviewed_by BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    rejection_reason VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_wallet_applications_user_id ON wallet_applications(user_id);
CREATE INDEX IF NOT EXISTS idx_wallet_applications_status ON wallet_applications(status);

-- KYC documents (uploaded docs with AI analysis)
CREATE TABLE IF NOT EXISTS kyc_documents (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES wallet_applications(id) ON DELETE CASCADE,
    doc_type VARCHAR(30) NOT NULL,
    file_reference VARCHAR(512),
    upload_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ai_confidence_score DECIMAL(5,4),
    ai_flags TEXT,
    verified_by_human BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kyc_documents_application_id ON kyc_documents(application_id);

-- Wallets (post-KYC approval)
CREATE TABLE IF NOT EXISTS wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    wallet_number VARCHAR(36) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    kyc_tier VARCHAR(20) NOT NULL DEFAULT 'BASIC',
    daily_limit DECIMAL(20,4) DEFAULT 10000,
    monthly_limit DECIMAL(20,4) DEFAULT 100000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_wallets_user_id ON wallets(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_wallets_wallet_number ON wallets(wallet_number);

-- Wallet balances (multi-currency)
CREATE TABLE IF NOT EXISTS wallet_balances (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
    currency_code VARCHAR(5) NOT NULL,
    available_balance DECIMAL(20,4) NOT NULL DEFAULT 0,
    locked_balance DECIMAL(20,4) NOT NULL DEFAULT 0,
    last_updated TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(wallet_id, currency_code)
);

CREATE INDEX IF NOT EXISTS idx_wallet_balances_wallet_id ON wallet_balances(wallet_id);

-- Wallet transactions (audit trail)
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL REFERENCES wallets(id),
    type VARCHAR(30) NOT NULL,
    amount DECIMAL(20,4) NOT NULL,
    currency VARCHAR(5) NOT NULL,
    reference_id VARCHAR(128),
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wallet_transactions_wallet_id ON wallet_transactions(wallet_id);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_created_at ON wallet_transactions(created_at);

-- Top-up requests (branch cash -> wallet)
CREATE TABLE IF NOT EXISTS topup_requests (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL REFERENCES wallets(id),
    branch_id BIGINT NOT NULL REFERENCES branches(id),
    amount DECIMAL(20,4) NOT NULL,
    currency VARCHAR(5) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    cashier_id BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_topup_requests_wallet_id ON topup_requests(wallet_id);
CREATE INDEX IF NOT EXISTS idx_topup_requests_branch_id ON topup_requests(branch_id);
CREATE INDEX IF NOT EXISTS idx_topup_requests_status ON topup_requests(status);

-- Extend ledger_entries for wallet (and other non-Transaction) reference
ALTER TABLE ledger_entries ADD COLUMN IF NOT EXISTS reference_type VARCHAR(50);
ALTER TABLE ledger_entries ADD COLUMN IF NOT EXISTS reference_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_ledger_reference ON ledger_entries(reference_type, reference_id);
