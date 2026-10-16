-- V9: Bổ sung mã băm PIN (Argon2id) cho bảng wallets
ALTER TABLE wallets ADD COLUMN IF NOT EXISTS pin_hash VARCHAR(255);
