CREATE TABLE wallets
(
    id         UUID        NOT NULL,
    balance    DECIMAL     NOT NULL DEFAULT 0.0000,
    currency   VARCHAR(10) NOT NULL DEFAULT 'VND',
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id    UUID         NOT NULL ,

    -- 1. Khóa chính
    CONSTRAINT pk_wallets PRIMARY KEY (id),

    -- 2. Khóa ngoại trỏ sang users (chặn xóa user nếu ví vẫn còn tồn tại để bảo vệ dữ liệu tài chính)
    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,

    -- 3. Một user chỉ được có duy nhất 1 ví cho mỗi loại tiền tệ (1 ví VND, 1 ví USD)
    CONSTRAINT uq_wallets_user_currency UNIQUE (user_id, currency),

    -- 4. Tầng bảo vệ an toàn: Ngăn chặn số dư bị âm ngay ở tầng Database
    CONSTRAINT chk_wallets_balance CHECK (balance >= 0)
);

CREATE INDEX IF NOT EXISTS idx_wallets_user_id ON wallets (user_id);