-- Module P1: Micro-Lending Engine

CREATE TABLE IF NOT EXISTS credit_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    credit_score INT NOT NULL DEFAULT 0,
    risk_tier VARCHAR(20) NOT NULL DEFAULT 'INELIGIBLE',
    max_loan_amount_usd DECIMAL(20,4) NOT NULL DEFAULT 0,
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_review_at TIMESTAMPTZ,
    score_components JSONB
);

CREATE INDEX IF NOT EXISTS idx_credit_profiles_user_id ON credit_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_credit_profiles_risk_tier ON credit_profiles(risk_tier);

CREATE TABLE IF NOT EXISTS loan_products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    min_amount DECIMAL(20,4) NOT NULL,
    max_amount DECIMAL(20,4) NOT NULL,
    min_term_days INT NOT NULL,
    max_term_days INT NOT NULL,
    apr_rate DECIMAL(8,4) NOT NULL,
    origination_fee_pct DECIMAL(5,2) NOT NULL DEFAULT 0,
    late_fee_amount DECIMAL(20,4) NOT NULL DEFAULT 0,
    risk_tier VARCHAR(10) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_loan_products_risk_tier ON loan_products(risk_tier);
CREATE INDEX IF NOT EXISTS idx_loan_products_is_active ON loan_products(is_active);

CREATE TABLE IF NOT EXISTS loan_applications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    product_id BIGINT NOT NULL REFERENCES loan_products(id),
    requested_amount DECIMAL(20,4) NOT NULL,
    requested_term_days INT NOT NULL,
    purpose VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT REFERENCES users(id),
    decision_reason VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_loan_applications_user_id ON loan_applications(user_id);
CREATE INDEX IF NOT EXISTS idx_loan_applications_status ON loan_applications(status);

CREATE TABLE IF NOT EXISTS loans (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES loan_applications(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    principal_amount DECIMAL(20,4) NOT NULL,
    currency VARCHAR(5) NOT NULL DEFAULT 'USD',
    disbursed_at TIMESTAMPTZ NOT NULL,
    term_days INT NOT NULL,
    apr_rate DECIMAL(8,4) NOT NULL,
    origination_fee DECIMAL(20,4) NOT NULL DEFAULT 0,
    monthly_payment DECIMAL(20,4) NOT NULL,
    outstanding_balance DECIMAL(20,4) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_loans_user_id ON loans(user_id);
CREATE INDEX IF NOT EXISTS idx_loans_status ON loans(status);

CREATE TABLE IF NOT EXISTS loan_repayment_schedule (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL REFERENCES loans(id) ON DELETE CASCADE,
    instalment_number INT NOT NULL,
    due_date DATE NOT NULL,
    principal_due DECIMAL(20,4) NOT NULL,
    interest_due DECIMAL(20,4) NOT NULL,
    total_due DECIMAL(20,4) NOT NULL,
    paid_amount DECIMAL(20,4) NOT NULL DEFAULT 0,
    paid_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(loan_id, instalment_number)
);

CREATE INDEX IF NOT EXISTS idx_loan_repayment_schedule_loan_id ON loan_repayment_schedule(loan_id);
CREATE INDEX IF NOT EXISTS idx_loan_repayment_schedule_due_date ON loan_repayment_schedule(due_date);
CREATE INDEX IF NOT EXISTS idx_loan_repayment_schedule_status ON loan_repayment_schedule(status);

CREATE TABLE IF NOT EXISTS loan_repayments (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL REFERENCES loans(id),
    schedule_id BIGINT NOT NULL REFERENCES loan_repayment_schedule(id),
    amount DECIMAL(20,4) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    wallet_transaction_id BIGINT REFERENCES wallet_transactions(id)
);

CREATE INDEX IF NOT EXISTS idx_loan_repayments_loan_id ON loan_repayments(loan_id);

CREATE TABLE IF NOT EXISTS loan_late_fees (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL REFERENCES loans(id),
    schedule_id BIGINT NOT NULL REFERENCES loan_repayment_schedule(id),
    fee_amount DECIMAL(20,4) NOT NULL,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    collected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_loan_late_fees_loan_id ON loan_late_fees(loan_id);
