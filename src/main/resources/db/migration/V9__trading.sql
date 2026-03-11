-- Module 9: Real-Time Trading Platform

CREATE TABLE IF NOT EXISTS trading_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    account_type VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    buying_power_usd DECIMAL(20,4) NOT NULL DEFAULT 0,
    total_portfolio_value DECIMAL(20,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_trading_accounts_user ON trading_accounts(user_id);

CREATE TABLE IF NOT EXISTS watchlists (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(120) NOT NULL,
    symbols TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_watchlists_user ON watchlists(user_id);

CREATE TABLE IF NOT EXISTS platform_trading_fees (
    id BIGSERIAL PRIMARY KEY,
    asset_class VARCHAR(20) NOT NULL,
    fee_rate_bps INTEGER NOT NULL,
    min_fee_usd DECIMAL(10,2) DEFAULT 0,
    max_fee_usd DECIMAL(10,2),
    effective_from TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_platform_trading_fees_asset ON platform_trading_fees(asset_class);

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    trading_account_id BIGINT NOT NULL REFERENCES trading_accounts(id),
    symbol VARCHAR(20) NOT NULL,
    order_type VARCHAR(10) NOT NULL,
    side VARCHAR(4) NOT NULL,
    quantity DECIMAL(20,6) NOT NULL,
    limit_price DECIMAL(20,4),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    filled_price DECIMAL(20,4),
    filled_at TIMESTAMPTZ,
    platform_fee DECIMAL(20,4),
    platform_fee_currency VARCHAR(5),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_orders_account ON orders(trading_account_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);

CREATE TABLE IF NOT EXISTS positions (
    id BIGSERIAL PRIMARY KEY,
    trading_account_id BIGINT NOT NULL REFERENCES trading_accounts(id),
    symbol VARCHAR(20) NOT NULL,
    quantity DECIMAL(20,6) NOT NULL,
    avg_entry_price DECIMAL(20,4) NOT NULL,
    current_price DECIMAL(20,4),
    unrealized_pnl DECIMAL(20,4),
    last_updated TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(trading_account_id, symbol)
);
CREATE INDEX IF NOT EXISTS idx_positions_account ON positions(trading_account_id);

CREATE TABLE IF NOT EXISTS price_snapshots (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    price DECIMAL(20,4) NOT NULL,
    change_pct DECIMAL(10,4),
    volume BIGINT,
    market_cap DECIMAL(30,2),
    currency VARCHAR(5),
    source VARCHAR(20) DEFAULT 'YAHOO',
    captured_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_price_snapshots_symbol ON price_snapshots(symbol);
CREATE INDEX IF NOT EXISTS idx_price_snapshots_captured ON price_snapshots(captured_at);
