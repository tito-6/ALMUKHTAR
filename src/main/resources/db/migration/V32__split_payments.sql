CREATE TABLE IF NOT EXISTS split_requests (
    id BIGSERIAL PRIMARY KEY,
    initiator_user_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(200) NOT NULL,
    total_amount NUMERIC(20,4) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS split_participants (
    id BIGSERIAL PRIMARY KEY,
    split_request_id BIGINT NOT NULL REFERENCES split_requests(id),
    payee_user_id BIGINT NOT NULL REFERENCES users(id),
    share_amount NUMERIC(20,4) NOT NULL,
    paid_at TIMESTAMP,
    paid BOOLEAN DEFAULT FALSE
);
