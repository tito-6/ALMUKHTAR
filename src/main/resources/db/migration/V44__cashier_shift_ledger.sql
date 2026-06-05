CREATE TABLE IF NOT EXISTS cashier_shifts (
    id BIGSERIAL PRIMARY KEY,
    branch_id BIGINT NOT NULL REFERENCES branches(id),
    cashier_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    opened_at TIMESTAMP NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMP,
    approved_by BIGINT REFERENCES users(id),
    approved_at TIMESTAMP,
    notes VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS cashier_shift_balances (
    id BIGSERIAL PRIMARY KEY,
    shift_id BIGINT NOT NULL REFERENCES cashier_shifts(id),
    currency VARCHAR(10) NOT NULL,
    opening_balance NUMERIC(20,4) NOT NULL DEFAULT 0,
    cash_in_total NUMERIC(20,4) NOT NULL DEFAULT 0,
    cash_out_total NUMERIC(20,4) NOT NULL DEFAULT 0,
    expected_closing_balance NUMERIC(20,4) NOT NULL DEFAULT 0,
    actual_closing_balance NUMERIC(20,4),
    variance NUMERIC(20,4),
    CONSTRAINT uk_cashier_shift_balance_currency UNIQUE(shift_id, currency)
);

CREATE TABLE IF NOT EXISTS cashier_shift_entries (
    id BIGSERIAL PRIMARY KEY,
    shift_id BIGINT NOT NULL REFERENCES cashier_shifts(id),
    currency VARCHAR(10) NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    amount NUMERIC(20,4) NOT NULL,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_cashier_shift_entry_amount_positive CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_cashier_shifts_cashier_status ON cashier_shifts(cashier_id, status);
CREATE INDEX IF NOT EXISTS idx_cashier_shifts_branch_status ON cashier_shifts(branch_id, status);
CREATE INDEX IF NOT EXISTS idx_cashier_shift_entries_shift_created ON cashier_shift_entries(shift_id, created_at);
