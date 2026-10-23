-- Fail explicitly if existing holds require reconciliation. Never change balances or statuses here.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM withdrawal_requests
        WHERE status IN ('PENDING', 'PROCESSING', 'REVIEW_REQUIRED')
        GROUP BY wallet_id HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot enforce one held withdrawal per wallet: reconcile wallets with multiple active holds first';
    END IF;
END $$;

CREATE UNIQUE INDEX uq_withdrawal_one_hold_per_wallet
    ON withdrawal_requests(wallet_id)
    WHERE status IN ('PENDING', 'PROCESSING', 'REVIEW_REQUIRED');
