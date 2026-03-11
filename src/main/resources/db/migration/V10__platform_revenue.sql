CREATE TABLE IF NOT EXISTS platform_revenue_entries (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    amount NUMERIC(20,4) NOT NULL,
    currency VARCHAR(5) NOT NULL,
    source_entity_id BIGINT,
    source_entity_type VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_platform_revenue_entries_event ON platform_revenue_entries(event_type);
CREATE INDEX IF NOT EXISTS idx_platform_revenue_entries_created ON platform_revenue_entries(created_at);
