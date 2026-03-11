-- Module P5: Currency Forward Contracts

CREATE TABLE IF NOT EXISTS forward_contract_rates (
    id BIGSERIAL PRIMARY KEY,
    from_currency VARCHAR(5) NOT NULL,
    to_currency VARCHAR(5) NOT NULL,
    spot_rate DECIMAL(20,8) NOT NULL,
    forward_30d_rate DECIMAL(20,8),
    forward_60d_rate DECIMAL(20,8),
    forward_90d_rate DECIMAL(20,8),
    platform_spread_bps INT NOT NULL DEFAULT 150,
    captured_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS forward_contracts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    from_currency VARCHAR(5) NOT NULL,
    to_currency VARCHAR(5) NOT NULL,
    notional_amount DECIMAL(20,4) NOT NULL,
    locked_rate DECIMAL(20,8) NOT NULL,
    contract_fee DECIMAL(20,4) NOT NULL DEFAULT 0,
    execution_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    spread_profit DECIMAL(20,4),
    executed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_forward_contracts_user_id ON forward_contracts(user_id);
CREATE INDEX IF NOT EXISTS idx_forward_contracts_status ON forward_contracts(status);
