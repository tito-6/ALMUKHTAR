-- Outbound WhatsApp destination + recipient polling index (PostgreSQL)
ALTER TABLE notification_outbox ADD COLUMN IF NOT EXISTS recipient_phone_e164 VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_recipient_created
    ON notification_outbox (recipient_user_id, created_at DESC);
