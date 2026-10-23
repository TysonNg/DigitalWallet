-- Stripe Connect sandbox only. Available wallet balance excludes these holds.
CREATE TABLE stripe_withdrawals (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    user_id UUID NOT NULL REFERENCES users(id),
    amount NUMERIC(19,4) NOT NULL CHECK (amount >= 25000 AND amount <= 100000000 AND MOD(amount,250)=0),
    balance_before NUMERIC(19,4) NOT NULL,
    test_bank VARCHAR(10) NOT NULL CHECK (test_bank IN ('SUCCESS', 'FAILURE')),
    idempotency_key VARCHAR(100) NOT NULL,
    usd_cents BIGINT NOT NULL CHECK (usd_cents > 0 AND usd_cents * 250 = amount),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'REVIEW_REQUIRED')),
    stripe_account_id VARCHAR(100),
    transfer_id VARCHAR(100),
    payout_id VARCHAR(100) UNIQUE,
    transaction_id UUID REFERENCES transactions(id),
    error VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (wallet_id, idempotency_key)
);
CREATE INDEX idx_stripe_withdrawals_pending ON stripe_withdrawals(status, updated_at);
CREATE INDEX idx_stripe_withdrawals_wallet ON stripe_withdrawals(wallet_id, created_at);
