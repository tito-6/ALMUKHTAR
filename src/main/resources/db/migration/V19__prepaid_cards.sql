-- Module P7: Prepaid Cards

CREATE TABLE IF NOT EXISTS prepaid_cards (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    wallet_id BIGINT NOT NULL REFERENCES wallets(id),
    card_type VARCHAR(20) NOT NULL,
    card_last4 VARCHAR(4),
    card_network VARCHAR(20),
    expiry_month INT,
    expiry_year INT,
    card_token VARCHAR(512),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    daily_limit DECIMAL(20,4),
    monthly_limit DECIMAL(20,4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS card_transactions (
    id BIGSERIAL PRIMARY KEY,
    card_id BIGINT NOT NULL REFERENCES prepaid_cards(id),
    merchant_name VARCHAR(200),
    merchant_category VARCHAR(50),
    amount DECIMAL(20,4) NOT NULL,
    currency VARCHAR(5) NOT NULL DEFAULT 'USD',
    transaction_type VARCHAR(20),
    status VARCHAR(20),
    interchange_earned DECIMAL(20,4),
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
