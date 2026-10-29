-- Tokens identify destinations only. Images remain owner-authenticated private media.
CREATE TABLE wallet_receive_codes (
    wallet_id UUID PRIMARY KEY REFERENCES wallets(id),
    token VARCHAR(36) NOT NULL UNIQUE CHECK (length(token) = 36),
    media_id UUID REFERENCES media_files(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);