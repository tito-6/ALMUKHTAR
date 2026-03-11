CREATE TABLE IF NOT EXISTS fee_zones (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(512),
    country VARCHAR(100),
    governorate VARCHAR(100),
    city VARCHAR(100),
    district VARCHAR(100),
    street VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS fee_zone_rules (
    id BIGSERIAL PRIMARY KEY,
    fee_zone_id BIGINT NOT NULL REFERENCES fee_zones(id) ON DELETE CASCADE,
    rule_type VARCHAR(30) NOT NULL,
    discount_value DECIMAL(10,4) NOT NULL,
    applies_to VARCHAR(30) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS fee_zone_schedules (
    id BIGSERIAL PRIMARY KEY,
    fee_zone_id BIGINT NOT NULL REFERENCES fee_zones(id) ON DELETE CASCADE,
    schedule_type VARCHAR(30) NOT NULL,
    start_date DATE,
    end_date DATE,
    recurring_month INTEGER,
    recurring_day INTEGER,
    islamic_event VARCHAR(50),
    day_of_week INTEGER,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS branch_fee_zone_assignments (
    id BIGSERIAL PRIMARY KEY,
    branch_id BIGINT NOT NULL REFERENCES branches(id),
    fee_zone_id BIGINT NOT NULL REFERENCES fee_zones(id),
    assigned_by BIGINT REFERENCES users(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(branch_id, fee_zone_id)
);
