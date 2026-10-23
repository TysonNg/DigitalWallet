\set ON_ERROR_STOP on
BEGIN;
CREATE SCHEMA withdrawal_migration_test;
SET search_path TO withdrawal_migration_test;
CREATE TABLE wallets(id UUID PRIMARY KEY);
CREATE TABLE users(id UUID PRIMARY KEY);
CREATE TABLE transactions(id UUID PRIMARY KEY);
INSERT INTO wallets VALUES('00000000-0000-0000-0000-000000000001');
INSERT INTO users VALUES('00000000-0000-0000-0000-000000000002');
INSERT INTO transactions VALUES('00000000-0000-0000-0000-000000000003');
\ir ../../../main/resources/db/migration/V14__create_stripe_withdrawals.sql
INSERT INTO stripe_withdrawals(id,wallet_id,user_id,amount,balance_before,test_bank,idempotency_key,usd_cents,status,stripe_account_id,transfer_id,payout_id,transaction_id,created_at,updated_at)
VALUES
('00000000-0000-0000-0000-000000000010','00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002',200000,1000000,'SUCCESS','processing-key',800,'PROCESSING','acct_old','tr_old',NULL,NULL,now(),now()),
('00000000-0000-0000-0000-000000000011','00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002',200000,1000000,'FAILURE','returned-key',800,'FAILED','acct_return','tr_return','po_return','00000000-0000-0000-0000-000000000003',now(),now()),
('00000000-0000-0000-0000-000000000012','00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002',200000,1000000,'SUCCESS','pending-key',800,'PENDING',NULL,NULL,NULL,NULL,now(),now());
\ir ../../../main/resources/db/migration/V15__generalize_withdrawal_requests.sql
DO $$ BEGIN
    IF (SELECT count(*) FROM withdrawal_requests)<>3 OR (SELECT count(*) FROM payout_attempts)<>3 THEN RAISE EXCEPTION 'Migration lost records'; END IF;
    IF NOT EXISTS(SELECT 1 FROM payout_attempts WHERE id='00000000-0000-0000-0000-000000000010' AND withdrawal_id=id AND provider_metadata->>'transfer_reference'='tr_old' AND provider_metadata->>'payout_started'='true') THEN RAISE EXCEPTION 'Lost stable operation identity or recovery checkpoint'; END IF;
    IF NOT EXISTS(SELECT 1 FROM withdrawal_requests WHERE idempotency_key='returned-key' AND status='RETURNED') THEN RAISE EXCEPTION 'Lost returned status'; END IF;
    IF (SELECT sum(amount) FROM withdrawal_requests WHERE status IN ('PENDING','PROCESSING','REVIEW_REQUIRED'))<>400000 THEN RAISE EXCEPTION 'Hold amount changed'; END IF;
END $$;
INSERT INTO payout_notification_inbox(provider,event_id,withdrawal_id) VALUES('STRIPE','evt_duplicate','00000000-0000-0000-0000-000000000010') ON CONFLICT(provider,event_id) DO NOTHING;
INSERT INTO payout_notification_inbox(provider,event_id,withdrawal_id) VALUES('STRIPE','evt_duplicate','00000000-0000-0000-0000-000000000010') ON CONFLICT(provider,event_id) DO NOTHING;
DO $$ BEGIN
    IF (SELECT count(*) FROM payout_notification_inbox)<>1 THEN RAISE EXCEPTION 'Duplicate webhook was not deduplicated'; END IF;
END $$;
INSERT INTO withdrawal_requests(id,wallet_id,user_id,amount,balance_before,currency,destination,idempotency_key,status,created_at,updated_at)
VALUES(gen_random_uuid(),'00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002',100,1000000,'VND','bank-token','other-provider-key','PENDING',now(),now());
SELECT 'V14 to V15 migration and inbox checks passed' AS result;
ROLLBACK;
