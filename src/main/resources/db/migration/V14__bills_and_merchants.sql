-- Module P2: Bill Payments & Merchant Network

CREATE TABLE IF NOT EXISTS bill_providers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(30) NOT NULL,
    country VARCHAR(5),
    city VARCHAR(100),
    api_integration VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    api_endpoint VARCHAR(512),
    api_key_ref VARCHAR(128),
    logo_url VARCHAR(512),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    processing_fee_pct DECIMAL(5,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bill_providers_category ON bill_providers(category);
CREATE INDEX IF NOT EXISTS idx_bill_providers_is_active ON bill_providers(is_active);

CREATE TABLE IF NOT EXISTS bill_payment_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    provider_id BIGINT NOT NULL REFERENCES bill_providers(id),
    account_reference VARCHAR(128) NOT NULL,
    amount DECIMAL(20,4) NOT NULL,
    currency VARCHAR(5) NOT NULL DEFAULT 'USD',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_ref VARCHAR(128),
    platform_fee DECIMAL(20,4) NOT NULL DEFAULT 0,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bill_payment_requests_user_id ON bill_payment_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_bill_payment_requests_status ON bill_payment_requests(status);

CREATE TABLE IF NOT EXISTS merchants (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES users(id),
    business_name VARCHAR(200) NOT NULL,
    category VARCHAR(50),
    registration_number VARCHAR(100),
    address VARCHAR(512),
    city VARCHAR(100),
    country VARCHAR(5),
    branch_id BIGINT REFERENCES branches(id),
    qr_code_data VARCHAR(512),
    qr_code_image_path VARCHAR(512),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    kyc_approved BOOLEAN NOT NULL DEFAULT FALSE,
    processing_fee_pct DECIMAL(5,2) NOT NULL DEFAULT 1.5,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_merchants_owner_user_id ON merchants(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_merchants_status ON merchants(status);

CREATE TABLE IF NOT EXISTS merchant_transactions (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL REFERENCES merchants(id),
    payer_user_id BIGINT NOT NULL REFERENCES users(id),
    amount DECIMAL(20,4) NOT NULL,
    currency VARCHAR(5) NOT NULL DEFAULT 'USD',
    description VARCHAR(255),
    platform_fee DECIMAL(20,4) NOT NULL DEFAULT 0,
    merchant_net_amount DECIMAL(20,4) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    qr_scan_ref VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_merchant_transactions_merchant_id ON merchant_transactions(merchant_id);
CREATE INDEX IF NOT EXISTS idx_merchant_transactions_payer_user_id ON merchant_transactions(payer_user_id);

CREATE TABLE IF NOT EXISTS merchant_settlements (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL REFERENCES merchants(id),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    gross_amount DECIMAL(20,4) NOT NULL DEFAULT 0,
    total_fees DECIMAL(20,4) NOT NULL DEFAULT 0,
    net_amount DECIMAL(20,4) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    settlement_wallet_id BIGINT REFERENCES wallets(id),
    settled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_merchant_settlements_merchant_id ON merchant_settlements(merchant_id);
CREATE INDEX IF NOT EXISTS idx_merchant_settlements_status ON merchant_settlements(status);
