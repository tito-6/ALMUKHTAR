CREATE TABLE IF NOT EXISTS branch_cash_inventory (
    id BIGSERIAL PRIMARY KEY,
    branch_id BIGINT NOT NULL REFERENCES branches(id),
    currency VARCHAR(10) NOT NULL,
    available_balance NUMERIC(20,4) NOT NULL DEFAULT 0,
    reserved_balance NUMERIC(20,4) NOT NULL DEFAULT 0,
    low_cash_threshold NUMERIC(20,4) NOT NULL DEFAULT 0,
    high_cash_threshold NUMERIC(20,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT uk_branch_cash_inventory_branch_currency UNIQUE(branch_id, currency),
    CONSTRAINT chk_branch_cash_available_nonnegative CHECK (available_balance >= 0),
    CONSTRAINT chk_branch_cash_reserved_nonnegative CHECK (reserved_balance >= 0)
);

CREATE TABLE IF NOT EXISTS branch_cash_reservations (
    id BIGSERIAL PRIMARY KEY,
    branch_id BIGINT NOT NULL REFERENCES branches(id),
    transaction_id BIGINT NOT NULL REFERENCES transactions(id),
    currency VARCHAR(10) NOT NULL,
    amount NUMERIC(20,4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT uk_branch_cash_reservation_transaction UNIQUE(transaction_id),
    CONSTRAINT chk_branch_cash_reservation_amount_positive CHECK (amount > 0)
);

CREATE TABLE IF NOT EXISTS branch_cash_movements (
    id BIGSERIAL PRIMARY KEY,
    branch_id BIGINT NOT NULL REFERENCES branches(id),
    transaction_id BIGINT REFERENCES transactions(id),
    cashier_id BIGINT REFERENCES users(id),
    currency VARCHAR(10) NOT NULL,
    movement_type VARCHAR(30) NOT NULL,
    amount NUMERIC(20,4) NOT NULL,
    available_after NUMERIC(20,4) NOT NULL,
    reserved_after NUMERIC(20,4) NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_branch_cash_inventory_branch ON branch_cash_inventory(branch_id);
CREATE INDEX IF NOT EXISTS idx_branch_cash_reservations_status ON branch_cash_reservations(status);
CREATE INDEX IF NOT EXISTS idx_branch_cash_movements_branch_created ON branch_cash_movements(branch_id, created_at DESC);
