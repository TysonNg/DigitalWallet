CREATE TABLE transactions
(
    id                 UUID           NOT NULL,
    type               VARCHAR(30)    NOT NULL,
    amount             DECIMAL        NOT NULL,
    amount_before      DECIMAL        NOT NULL,
    amount_after       DECIMAL        NOT NULL,
    status             VARCHAR(30)    NOT NULL,
    description        VARCHAR(255),
    sender_wallet_id   UUID,
    receiver_wallet_id UUID,
    created_at         TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT fk_transactions_sender FOREIGN KEY (sender_wallet_id) REFERENCES wallets (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_receiver FOREIGN KEY (receiver_wallet_id) REFERENCES wallets (id) ON DELETE RESTRICT,
    CONSTRAINT chk_transactions_amount CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_transactions_sender_id ON transactions (sender_wallet_id);
CREATE INDEX IF NOT EXISTS idx_transactions_receiver_id ON transactions (receiver_wallet_id);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions (created_at);

CREATE TABLE idempotency_keys
(
    id              UUID         NOT NULL,
    user_id         UUID         NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    transaction_id  UUID,
    request_hash    VARCHAR(64)  NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT pk_idempotency_keys PRIMARY KEY (id),
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_idempotency_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE SET NULL,
    CONSTRAINT uq_user_idempotency_key UNIQUE (user_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_keys_expires_at ON idempotency_keys (expires_at);
