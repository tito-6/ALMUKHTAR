-- WhatsApp notification backbone: delivery audit trail, template metadata, preference flags.

CREATE TABLE IF NOT EXISTS notification_delivery_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    phone_masked VARCHAR(32) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id BIGINT,
    template_key VARCHAR(120) NOT NULL,
    language VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider_message_id VARCHAR(128),
    retry_count INTEGER NOT NULL DEFAULT 0,
    failure_reason VARCHAR(1024),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_delivery_provider_msg
    ON notification_delivery_logs (provider_message_id)
    WHERE provider_message_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ndl_user_created
    ON notification_delivery_logs (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ndl_entity
    ON notification_delivery_logs (entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_ndl_status_created
    ON notification_delivery_logs (status, created_at);

ALTER TABLE notification_templates
    ADD COLUMN IF NOT EXISTS message_category VARCHAR(20) NOT NULL DEFAULT 'OPERATIONAL';

ALTER TABLE notification_templates
    ADD COLUMN IF NOT EXISTS meta_template_name VARCHAR(255);

UPDATE notification_templates SET message_category = 'OPERATIONAL' WHERE message_category IS NULL;

ALTER TABLE user_notification_preferences
    ADD COLUMN IF NOT EXISTS marketing_enabled BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE user_notification_preferences
    ADD COLUMN IF NOT EXISTS transaction_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_notification_templates_key_locale_channel
    ON notification_templates (template_key, locale, channel);
