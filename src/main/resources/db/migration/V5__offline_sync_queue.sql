CREATE TABLE IF NOT EXISTS sync_queue (
    id                  BIGSERIAL PRIMARY KEY,
    device_id           VARCHAR(128) NOT NULL,
    cashier_id          BIGINT NOT NULL REFERENCES users(id),
    payload_encrypted   TEXT NOT NULL,
    payload_checksum    VARCHAR(64) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    offline_timestamp   TIMESTAMPTZ NOT NULL,
    synced_at           TIMESTAMPTZ,
    conflict_reason     TEXT,
    retry_count         INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_sync_queue_status ON sync_queue(status, cashier_id);
