-- Advanced trading: risk profiles, tradable assets, price alerts, order failure metadata

ALTER TABLE orders ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(40);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS failure_detail TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS reference_market_price DECIMAL(20,4);

CREATE TABLE IF NOT EXISTS trading_risk_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    archetype VARCHAR(20) NOT NULL DEFAULT 'CONSERVATIVE',
    max_single_order_usd DECIMAL(20,4) NOT NULL,
    daily_trading_cap_usd DECIMAL(20,4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS trading_risk_profile_asset_classes (
    profile_id BIGINT NOT NULL REFERENCES trading_risk_profiles(id) ON DELETE CASCADE,
    asset_class VARCHAR(20) NOT NULL,
    PRIMARY KEY (profile_id, asset_class)
);

CREATE TABLE IF NOT EXISTS tradable_assets (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    country_code VARCHAR(3),
    allowed_roles_csv VARCHAR(500),
    symbol VARCHAR(32) NOT NULL,
    asset_class VARCHAR(20) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    currency VARCHAR(5) NOT NULL DEFAULT 'USD',
    market VARCHAR(40) NOT NULL DEFAULT 'US',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    min_order_value DECIMAL(20,4),
    max_order_value DECIMAL(20,4),
    risk_level VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tradable_assets_symbol ON tradable_assets(symbol);
CREATE INDEX IF NOT EXISTS idx_tradable_assets_tenant ON tradable_assets(tenant_id);

CREATE TABLE IF NOT EXISTS price_alerts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    symbol VARCHAR(32) NOT NULL,
    kind VARCHAR(30) NOT NULL,
    threshold_price DECIMAL(20,6),
    threshold_percent DECIMAL(10,4),
    baseline_volume BIGINT,
    reference_price DECIMAL(20,6),
    notify_in_app BOOLEAN NOT NULL DEFAULT TRUE,
    notify_whatsapp BOOLEAN NOT NULL DEFAULT FALSE,
    triggered BOOLEAN NOT NULL DEFAULT FALSE,
    triggered_at TIMESTAMPTZ,
    whatsapp_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_price_alerts_user ON price_alerts(user_id);
CREATE INDEX IF NOT EXISTS idx_price_alerts_symbol ON price_alerts(symbol);

CREATE TABLE IF NOT EXISTS user_trading_notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    order_in_app BOOLEAN NOT NULL DEFAULT TRUE,
    order_whatsapp BOOLEAN NOT NULL DEFAULT FALSE,
    price_alert_in_app BOOLEAN NOT NULL DEFAULT TRUE,
    price_alert_whatsapp BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);
