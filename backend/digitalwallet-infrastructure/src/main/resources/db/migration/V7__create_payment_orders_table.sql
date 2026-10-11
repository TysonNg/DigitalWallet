CREATE TABLE payment_orders
(
    id               UUID         NOT NULL,
    wallet_id        UUID         NOT NULL,
    user_id          UUID         NOT NULL,
    transaction_id   UUID,
    amount           DECIMAL      NOT NULL,
    gateway          VARCHAR(30)  NOT NULL,
    gateway_order_id VARCHAR(100),
    status           VARCHAR(30)  NOT NULL,
    checkout_url     TEXT,
    created_at       TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at     TIMESTAMP(6) WITHOUT TIME ZONE,

    CONSTRAINT pk_payment_orders PRIMARY KEY (id),
    CONSTRAINT fk_payment_orders_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id) ON DELETE RESTRICT,
    CONSTRAINT fk_payment_orders_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_orders_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE SET NULL,
    CONSTRAINT chk_payment_orders_amount CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_payment_orders_wallet_id ON payment_orders (wallet_id);
CREATE INDEX IF NOT EXISTS idx_payment_orders_user_id ON payment_orders (user_id);
CREATE INDEX IF NOT EXISTS idx_payment_orders_transaction_id ON payment_orders (transaction_id);
CREATE INDEX IF NOT EXISTS idx_payment_orders_gateway_order_id ON payment_orders (gateway_order_id);
CREATE INDEX IF NOT EXISTS idx_payment_orders_status ON payment_orders (status);
