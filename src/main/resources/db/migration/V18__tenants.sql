-- Module P6: White-Label Tenancy

CREATE TABLE IF NOT EXISTS tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(50) NOT NULL UNIQUE,
    logo_url VARCHAR(512),
    primary_color VARCHAR(20),
    secondary_color VARCHAR(20),
    contact_email VARCHAR(255),
    monthly_saas_fee_usd DECIMAL(20,4) NOT NULL DEFAULT 0,
    per_transaction_royalty_bps INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    contract_start DATE,
    contract_end DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tenant_billing (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    period_month VARCHAR(7) NOT NULL,
    total_transactions INT NOT NULL DEFAULT 0,
    total_volume_usd DECIMAL(20,4) NOT NULL DEFAULT 0,
    saas_fee DECIMAL(20,4) NOT NULL DEFAULT 0,
    royalty_amount DECIMAL(20,4) NOT NULL DEFAULT 0,
    total_billed DECIMAL(20,4) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    invoice_url VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
