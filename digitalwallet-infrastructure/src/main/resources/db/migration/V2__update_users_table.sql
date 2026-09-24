-- 1. Thêm cột updated_at
    ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 2. Tăng độ dài phone_number và thêm UNIQUE
ALTER TABLE users ALTER COLUMN phone_number TYPE VARCHAR(20);
ALTER TABLE users ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD CONSTRAINT uq_users_phone_number UNIQUE (phone_number);

-- 3. Thêm CHECK constraint ngày sinh
ALTER TABLE users ADD CONSTRAINT chk_users_dob CHECK (dob <= CURRENT_DATE);

-- 4. Đánh Index
CREATE INDEX IF NOT EXISTS idx_users_phone_number ON users (phone_number);
CREATE INDEX IF NOT EXISTS idx_users_status ON users (status);
