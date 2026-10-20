-- V12__create_ledger_tables.sql
-- Hệ thống Sổ cái ghi kép (Double-Entry General Ledger)

-- 1. Bảng tài khoản kế toán (Accounts)
CREATE TABLE accounts
(
    id         UUID         NOT NULL,
    code       VARCHAR(50)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    currency   VARCHAR(10)  NOT NULL DEFAULT 'VND',
    wallet_id  UUID,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT uq_accounts_code UNIQUE (code),
    CONSTRAINT fk_accounts_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_accounts_wallet_id ON accounts (wallet_id);

-- 2. Bảng Journal Entries (Mỗi giao dịch kinh tế tương ứng 1 Journal Entry)
CREATE TABLE journal_entries
(
    id               UUID         NOT NULL,
    transaction_id   UUID,
    payment_order_id UUID,
    description      VARCHAR(255) NOT NULL,
    created_at       TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_journal_entries PRIMARY KEY (id),
    CONSTRAINT fk_journal_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE SET NULL,
    CONSTRAINT fk_journal_payment_order FOREIGN KEY (payment_order_id) REFERENCES payment_orders (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_journal_entries_transaction_id ON journal_entries (transaction_id);
CREATE INDEX IF NOT EXISTS idx_journal_entries_payment_order_id ON journal_entries (payment_order_id);
CREATE INDEX IF NOT EXISTS idx_journal_entries_created_at ON journal_entries (created_at);

-- 3. Bảng Ledger Entries (Từng dòng bút toán con Debit/Credit - Bất biến Append-only)
CREATE TABLE ledger_entries
(
    id               UUID           NOT NULL,
    journal_entry_id UUID           NOT NULL,
    account_id       UUID           NOT NULL,
    debit_amount     DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    credit_amount    DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    created_at       TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_ledger_entries PRIMARY KEY (id),
    CONSTRAINT fk_ledger_journal FOREIGN KEY (journal_entry_id) REFERENCES journal_entries (id) ON DELETE CASCADE,
    CONSTRAINT fk_ledger_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT chk_ledger_amounts_non_negative CHECK (debit_amount >= 0 AND credit_amount >= 0),
    CONSTRAINT chk_ledger_single_side CHECK (
        (debit_amount > 0 AND credit_amount = 0) OR
        (credit_amount > 0 AND debit_amount = 0)
    )
);

CREATE INDEX IF NOT EXISTS idx_ledger_entries_account_id ON ledger_entries (account_id);
CREATE INDEX IF NOT EXISTS idx_ledger_entries_journal_id ON ledger_entries (journal_entry_id);
CREATE INDEX IF NOT EXISTS idx_ledger_entries_created_at ON ledger_entries (created_at);

-- 4. Khởi tạo sẵn các tài khoản hệ thống cốt lõi
INSERT INTO accounts (id, code, name, type, currency) VALUES
('a0000000-0000-0000-0000-000000000001', 'SYS_STRIPE_CLEARING', 'Stripe Settlement Gateway', 'ASSET', 'VND'),
('a0000000-0000-0000-0000-000000000002', 'SYS_PAYOS_CLEARING', 'PayOS Settlement Gateway', 'ASSET', 'VND'),
('a0000000-0000-0000-0000-000000000003', 'SYS_BANK_VAULT', 'Company Master Bank Vault', 'ASSET', 'VND'),
('a0000000-0000-0000-0000-000000000004', 'SYS_FEE_REVENUE', 'Transaction Fee Revenue', 'REVENUE', 'VND'),
('a0000000-0000-0000-0000-000000000005', 'SYS_GATEWAY_EXPENSE', 'Payment Gateway Processing Expense', 'EXPENSE', 'VND')
ON CONFLICT (code) DO NOTHING;

-- 5. Tự động sinh tài khoản Sổ cái cho các Ví người dùng đã tồn tại trong database (nếu có)
INSERT INTO accounts (id, code, name, type, currency, wallet_id, created_at)
SELECT gen_random_uuid(), 'WALLET_' || id::text, 'Ví người dùng ' || id::text, 'LIABILITY', currency, id, created_at
FROM wallets
ON CONFLICT (code) DO NOTHING;
