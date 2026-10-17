-- V11: Seed initial Super Admin account for testing and system bootstrap

DO $$
DECLARE
    admin_user_id UUID := 'a0000000-0000-0000-0000-000000000001';
    admin_wallet_id UUID := 'a0000000-0000-0000-0000-000000000002';
    role_admin_id BIGINT;
    role_user_id BIGINT;
BEGIN
    -- 1. Thêm Super Admin nếu chưa có
    INSERT INTO users (id, full_name, email, phone_number, dob, status, password, created_at, updated_at)
    VALUES (
        admin_user_id,
        'Super Administrator',
        'admin@digitalwallet.com',
        '0909999999',
        '1990-01-01',
        'ACTIVE',
        '$argon2id$v=19$m=16384,t=2,p=1$JbG5+9dhUob+DqvUOL1s7g$mBSR4ifrExNP/FD7o2U/Y3DEpiWifAcBmpVCmJpGF9Q', -- Mật khẩu: Admin@123456
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
    ON CONFLICT (email) DO NOTHING;

    -- 2. Tạo ví mặc định cho Super Admin
    INSERT INTO wallets (id, user_id, balance, currency, status, created_at, updated_at)
    VALUES (
        admin_wallet_id,
        admin_user_id,
        0.00,
        'VND',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
    ON CONFLICT (id) DO NOTHING;

    -- 3. Gán quyền ROLE_ADMIN và ROLE_USER
    SELECT id INTO role_admin_id FROM roles WHERE name = 'ROLE_ADMIN';
    SELECT id INTO role_user_id FROM roles WHERE name = 'ROLE_USER';

    IF role_admin_id IS NOT NULL THEN
        INSERT INTO user_roles (user_id, role_id)
        VALUES (admin_user_id, role_admin_id)
        ON CONFLICT DO NOTHING;
    END IF;

    IF role_user_id IS NOT NULL THEN
        INSERT INTO user_roles (user_id, role_id)
        VALUES (admin_user_id, role_user_id)
        ON CONFLICT DO NOTHING;
    END IF;
END $$;
