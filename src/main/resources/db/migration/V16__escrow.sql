-- Module P4: Escrow & Conditional Transfers

CREATE TABLE IF NOT EXISTS escrow_contracts (
    id BIGSERIAL PRIMARY KEY,
    initiator_user_id BIGINT NOT NULL REFERENCES users(id),
    beneficiary_user_id BIGINT NOT NULL REFERENCES users(id),
    amount DECIMAL(20,4) NOT NULL,
    currency VARCHAR(5) NOT NULL DEFAULT 'USD',
    title VARCHAR(200),
    description VARCHAR(512),
    condition_type VARCHAR(30) NOT NULL,
    condition_details JSONB,
    release_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    daily_fee_rate DECIMAL(10,6) NOT NULL DEFAULT 0,
    total_fees_collected DECIMAL(20,4) NOT NULL DEFAULT 0,
    funded_at TIMESTAMPTZ,
    released_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_escrow_contracts_status ON escrow_contracts(status);

CREATE TABLE IF NOT EXISTS escrow_events (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES escrow_contracts(id),
    event_type VARCHAR(50) NOT NULL,
    performed_by BIGINT REFERENCES users(id),
    notes VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS escrow_fee_accruals (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES escrow_contracts(id),
    date DATE NOT NULL,
    fee_amount DECIMAL(20,4) NOT NULL,
    currency VARCHAR(5) NOT NULL DEFAULT 'USD',
    collected BOOLEAN NOT NULL DEFAULT FALSE,
    collected_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
