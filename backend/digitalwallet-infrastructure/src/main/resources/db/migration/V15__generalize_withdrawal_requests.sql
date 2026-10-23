-- Preserve V14 history and Stripe operation identities. Deploy with the old worker stopped.
ALTER TABLE stripe_withdrawals RENAME TO withdrawal_requests;
CREATE TABLE payout_attempts (
    id UUID PRIMARY KEY,
    withdrawal_id UUID NOT NULL REFERENCES withdrawal_requests(id),
    attempt_no INTEGER NOT NULL DEFAULT 1 CHECK (attempt_no > 0),
    provider VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK(status IN ('PENDING','PROCESSING','SUCCEEDED','FAILED','RETURNED','UNKNOWN','REVIEW_REQUIRED')),
    error VARCHAR(500),
    provider_reference VARCHAR(255),
    provider_account_reference VARCHAR(255),
    amount BIGINT NOT NULL CHECK (amount > 0),
    currency VARCHAR(10) NOT NULL,
    provider_metadata JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (withdrawal_id, attempt_no)
);
INSERT INTO payout_attempts(id,withdrawal_id,provider,status,error,provider_reference,provider_account_reference,amount,currency,provider_metadata,created_at)
SELECT id,id,'STRIPE',CASE WHEN status='FAILED' AND transaction_id IS NOT NULL THEN 'RETURNED' ELSE status END,error,payout_id,stripe_account_id,usd_cents,'USD',jsonb_strip_nulls(jsonb_build_object(
    'currency_exponent','2','account_reference',stripe_account_id,'transfer_reference',transfer_id,'payout_reference',payout_id,
    'account_started','true','transfer_started',CASE WHEN stripe_account_id IS NOT NULL THEN 'true' END,
    'payout_started',CASE WHEN transfer_id IS NOT NULL THEN 'true' END)),created_at
FROM withdrawal_requests;
CREATE UNIQUE INDEX uq_payout_provider_reference ON payout_attempts
    (provider,COALESCE(provider_account_reference,''),provider_reference)
    WHERE provider_reference IS NOT NULL;
CREATE UNIQUE INDEX uq_payout_active_attempt ON payout_attempts(withdrawal_id)
    WHERE status IN ('PENDING','PROCESSING','UNKNOWN','REVIEW_REQUIRED');
ALTER TABLE withdrawal_requests ADD COLUMN currency VARCHAR(10) NOT NULL DEFAULT 'VND';
ALTER TABLE withdrawal_requests ADD COLUMN destination VARCHAR(255);
UPDATE withdrawal_requests SET destination='test-bank:'||test_bank;
ALTER TABLE withdrawal_requests ALTER COLUMN destination SET NOT NULL;
-- V14 failures with a transaction were returns after an earlier successful payout.
ALTER TABLE withdrawal_requests DROP CONSTRAINT stripe_withdrawals_status_check;
UPDATE withdrawal_requests SET status='RETURNED' WHERE status='FAILED' AND transaction_id IS NOT NULL;
ALTER TABLE withdrawal_requests ADD CONSTRAINT withdrawal_requests_status_check
    CHECK(status IN ('PENDING','PROCESSING','SUCCEEDED','FAILED','RETURNED','REVIEW_REQUIRED'));
ALTER TABLE withdrawal_requests DROP CONSTRAINT stripe_withdrawals_amount_check;
ALTER TABLE withdrawal_requests ADD CONSTRAINT withdrawal_requests_amount_check CHECK(amount>0);
ALTER TABLE withdrawal_requests DROP COLUMN test_bank;
ALTER TABLE withdrawal_requests DROP COLUMN usd_cents;
ALTER TABLE withdrawal_requests DROP COLUMN stripe_account_id;
ALTER TABLE withdrawal_requests DROP COLUMN transfer_id;
ALTER TABLE withdrawal_requests DROP COLUMN payout_id;
CREATE TABLE payout_notification_inbox (
    provider VARCHAR(50) NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    withdrawal_id UUID NOT NULL REFERENCES withdrawal_requests(id),
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    PRIMARY KEY(provider,event_id)
);
CREATE INDEX idx_payout_notifications_pending ON payout_notification_inbox(received_at) WHERE processed_at IS NULL;
