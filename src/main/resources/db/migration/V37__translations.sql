CREATE TABLE IF NOT EXISTS translations (
    id BIGSERIAL PRIMARY KEY,
    locale VARCHAR(10) NOT NULL,
    message_key VARCHAR(200) NOT NULL,
    message_value TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(locale, message_key)
);

CREATE TABLE IF NOT EXISTS notification_templates (
    id BIGSERIAL PRIMARY KEY,
    template_key VARCHAR(120) NOT NULL,
    locale VARCHAR(10) NOT NULL,
    subject VARCHAR(255),
    body_template TEXT NOT NULL,
    channel VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);
