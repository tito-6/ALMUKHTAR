-- ALMUKHTAR Syria Hawala Operations Advantage Pack (entities + sync extensions)

CREATE TABLE IF NOT EXISTS branch_vault_balances (
    id                  BIGSERIAL PRIMARY KEY,
    branch_id           BIGINT NOT NULL REFERENCES branches(id),
    currency            VARCHAR(10) NOT NULL,
    vault_balance       NUMERIC(20,4) NOT NULL DEFAULT 0,
    last_reconciled_at  TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    CONSTRAINT uq_branch_vault_currency UNIQUE (branch_id, currency)
);

CREATE TABLE IF NOT EXISTS cashier_drawers (
    id           BIGSERIAL PRIMARY KEY,
    shift_id     BIGINT NOT NULL UNIQUE REFERENCES cashier_shifts(id),
    status       VARCHAR(20) NOT NULL,
    opened_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at    TIMESTAMP,
    notes        VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS cash_drawer_movements (
    id               BIGSERIAL PRIMARY KEY,
    drawer_id        BIGINT NOT NULL REFERENCES cashier_drawers(id),
    movement_type    VARCHAR(30) NOT NULL,
    currency         VARCHAR(10) NOT NULL,
    amount           NUMERIC(20,4) NOT NULL,
    transaction_id   BIGINT REFERENCES transactions(id),
    created_by       BIGINT REFERENCES users(id),
    approval_status  VARCHAR(20) NOT NULL,
    approved_by      BIGINT REFERENCES users(id),
    approved_at      TIMESTAMP,
    note             VARCHAR(500),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cash_transfer_orders (
    id             BIGSERIAL PRIMARY KEY,
    from_branch_id BIGINT NOT NULL REFERENCES branches(id),
    to_branch_id   BIGINT NOT NULL REFERENCES branches(id),
    currency       VARCHAR(10) NOT NULL,
    amount         NUMERIC(20,4) NOT NULL,
    status         VARCHAR(20) NOT NULL,
    requested_by   BIGINT REFERENCES users(id),
    approved_by    BIGINT REFERENCES users(id),
    approved_at    TIMESTAMP,
    dispatched_at  TIMESTAMP,
    received_at    TIMESTAMP,
    notes          VARCHAR(500),
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payout_reservations (
    id                   BIGSERIAL PRIMARY KEY,
    receiver_id          BIGINT NOT NULL REFERENCES users(id),
    branch_id            BIGINT NOT NULL REFERENCES branches(id),
    currency             VARCHAR(10) NOT NULL,
    amount               NUMERIC(20,4) NOT NULL,
    pickup_window_start  TIMESTAMP NOT NULL,
    pickup_window_end    TIMESTAMP NOT NULL,
    expires_at           TIMESTAMP NOT NULL,
    status               VARCHAR(20) NOT NULL,
    transaction_id       BIGINT REFERENCES transactions(id),
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP
);

CREATE TABLE IF NOT EXISTS account_freeze_cases (
    id                     BIGSERIAL PRIMARY KEY,
    public_case_id         VARCHAR(40) NOT NULL UNIQUE,
    user_id                BIGINT NOT NULL REFERENCES users(id),
    wallet_id              BIGINT,
    customer_category      VARCHAR(40) NOT NULL,
    internal_reason        VARCHAR(2000),
    customer_message       VARCHAR(2000),
    sla_due_at             TIMESTAMP,
    appeal_or_dispute_url  VARCHAR(1024),
    status                 VARCHAR(30) NOT NULL,
    assigned_auditor_id    BIGINT REFERENCES users(id),
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP
);

CREATE TABLE IF NOT EXISTS freeze_case_events (
    id                      BIGSERIAL PRIMARY KEY,
    freeze_case_id          BIGINT NOT NULL REFERENCES account_freeze_cases(id),
    event_type              VARCHAR(40) NOT NULL,
    actor_id                BIGINT REFERENCES users(id),
    internal_note           VARCHAR(4000),
    customer_safe_summary   VARCHAR(2000),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS system_incidents (
    id             BIGSERIAL PRIMARY KEY,
    component      VARCHAR(30) NOT NULL,
    severity       VARCHAR(20) NOT NULL,
    public_title   VARCHAR(200) NOT NULL,
    public_message VARCHAR(2000),
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    started_at     TIMESTAMP NOT NULL,
    ended_at       TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP
);

CREATE TABLE IF NOT EXISTS branch_incidents (
    id                     BIGSERIAL PRIMARY KEY,
    branch_id              BIGINT NOT NULL REFERENCES branches(id),
    cash_shortage_public   BOOLEAN NOT NULL DEFAULT FALSE,
    integration_failure    BOOLEAN NOT NULL DEFAULT FALSE,
    whatsapp_degraded      BOOLEAN NOT NULL DEFAULT FALSE,
    branch_outage          BOOLEAN NOT NULL DEFAULT FALSE,
    public_message         VARCHAR(2000),
    active                 BOOLEAN NOT NULL DEFAULT TRUE,
    started_at             TIMESTAMP NOT NULL,
    ended_at               TIMESTAMP,
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP
);

ALTER TABLE sync_queue ADD COLUMN IF NOT EXISTS cashier_device_id VARCHAR(128);
ALTER TABLE sync_queue ADD COLUMN IF NOT EXISTS device_sequence_number BIGINT;
ALTER TABLE sync_queue ADD COLUMN IF NOT EXISTS signed_payload_hash VARCHAR(64);
ALTER TABLE sync_queue ADD COLUMN IF NOT EXISTS device_signature VARCHAR(512);
ALTER TABLE sync_queue ADD COLUMN IF NOT EXISTS conflict_status VARCHAR(40);
