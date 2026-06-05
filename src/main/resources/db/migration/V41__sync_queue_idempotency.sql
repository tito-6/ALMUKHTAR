ALTER TABLE sync_queue
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);

UPDATE sync_queue
SET idempotency_key = payload_checksum
WHERE idempotency_key IS NULL;

ALTER TABLE sync_queue
    ALTER COLUMN idempotency_key SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_sync_queue_device_idempotency
    ON sync_queue(device_id, idempotency_key);
