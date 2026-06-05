CREATE TABLE IF NOT EXISTS family_groups (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(120) NOT NULL,
    monthly_spending_limit NUMERIC(20,4),
    currency VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS family_members (
    id BIGSERIAL PRIMARY KEY,
    family_group_id BIGINT NOT NULL REFERENCES family_groups(id),
    member_user_id BIGINT NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL,
    monthly_spending_limit NUMERIC(20,4),
    active BOOLEAN DEFAULT TRUE
);
