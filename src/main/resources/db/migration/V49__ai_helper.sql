CREATE TABLE ai_conversation_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    tenant_id BIGINT,
    agent_role VARCHAR(40) NOT NULL,
    external_conversation_id VARCHAR(128),
    client_session_key VARCHAR(64),
    pending_action_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_conv_sess_user ON ai_conversation_sessions (user_id);
CREATE INDEX idx_ai_conv_sess_external ON ai_conversation_sessions (external_conversation_id);
CREATE UNIQUE INDEX ux_ai_conv_sess_user_client ON ai_conversation_sessions (user_id, client_session_key)
    WHERE client_session_key IS NOT NULL;

CREATE TABLE ai_conversation_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES ai_conversation_sessions (id) ON DELETE CASCADE,
    direction VARCHAR(16) NOT NULL,
    channel VARCHAR(16) NOT NULL DEFAULT 'WEB',
    content_masked TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_conv_msg_session ON ai_conversation_messages (session_id);

CREATE TABLE ai_tool_call_audits (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES ai_conversation_sessions (id) ON DELETE SET NULL,
    user_id BIGINT NOT NULL REFERENCES users (id),
    tenant_id BIGINT,
    tool_name VARCHAR(64) NOT NULL,
    arguments_masked TEXT,
    result_summary TEXT,
    ok BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_tool_audit_session ON ai_tool_call_audits (session_id);
CREATE INDEX idx_ai_tool_audit_user ON ai_tool_call_audits (user_id);
