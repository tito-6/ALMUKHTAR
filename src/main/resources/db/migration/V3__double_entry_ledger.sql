CREATE TABLE IF NOT EXISTS chart_of_accounts (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20) NOT NULL UNIQUE,
    name            VARCHAR(120) NOT NULL,
    account_type    VARCHAR(30) NOT NULL,
    parent_id       BIGINT REFERENCES chart_of_accounts(id),
    branch_id       BIGINT REFERENCES branches(id),
    is_system       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ledger_entries (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  BIGINT REFERENCES transactions(id),
    account_id      BIGINT NOT NULL REFERENCES chart_of_accounts(id),
    entry_type      VARCHAR(6) NOT NULL CHECK (entry_type IN ('DEBIT','CREDIT')),
    amount          NUMERIC(20,4) NOT NULL CHECK (amount > 0),
    currency_code   VARCHAR(5) NOT NULL DEFAULT 'USD',
    description     VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      BIGINT REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_ledger_transaction ON ledger_entries(transaction_id);
CREATE INDEX IF NOT EXISTS idx_ledger_account ON ledger_entries(account_id);

CREATE TABLE IF NOT EXISTS corporate_accounts (
    id              BIGSERIAL PRIMARY KEY,
    parent_user_id  BIGINT NOT NULL REFERENCES users(id),
    sub_user_id     BIGINT REFERENCES users(id),
    account_label   VARCHAR(120) NOT NULL,
    spending_limit  NUMERIC(20,4),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
