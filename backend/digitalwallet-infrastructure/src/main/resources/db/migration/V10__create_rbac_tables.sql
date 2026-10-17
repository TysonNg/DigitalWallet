-- V10: Enterprise Role-Based Access Control (RBAC) 3-Tier Tables: Users <-> Roles <-> Authorities

-- 1. Bảng danh mục vai trò (roles)
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Bảng quan hệ nhiều - nhiều giữa users và roles (user_roles)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);

-- 3. Bảng danh mục quyền hạn hạt nhân (authorities / privileges)
CREATE TABLE IF NOT EXISTS authorities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 4. Bảng quan hệ nhiều - nhiều giữa roles và authorities (role_authorities)
CREATE TABLE IF NOT EXISTS role_authorities (
    role_id BIGINT NOT NULL,
    authority_id BIGINT NOT NULL,
    assigned_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (role_id, authority_id),
    CONSTRAINT fk_role_authorities_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_authorities_authority FOREIGN KEY (authority_id) REFERENCES authorities (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_role_authorities_role_id ON role_authorities(role_id);
CREATE INDEX IF NOT EXISTS idx_role_authorities_authority_id ON role_authorities(authority_id);

-- 5. Seed các vai trò mặc định
INSERT INTO roles (name, description) VALUES
    ('ROLE_USER', 'Standard customer role with personal wallet privileges'),
    ('ROLE_ADMIN', 'System administrator with full operational backoffice access')
ON CONFLICT (name) DO NOTHING;

-- 6. Seed các quyền hạn hạt nhân (authorities)
INSERT INTO authorities (name, description) VALUES
    ('USER_READ', 'Xem danh sách và chi tiết thông tin người dùng'),
    ('USER_SUSPEND', 'Khóa hoặc mở khóa tài khoản người dùng'),
    ('USER_MANAGE_ROLE', 'Cấp hoặc thu hồi vai trò quản trị của người dùng'),
    ('WALLET_READ', 'Xem thông tin ví và lịch sử giao dịch ví'),
    ('WALLET_FREEZE', 'Đóng băng hoặc mở đóng băng ví điện tử'),
    ('SYSTEM_STATS', 'Xem thống kê chỉ số và thanh khoản toàn hệ thống')
ON CONFLICT (name) DO NOTHING;

-- 7. Gán quyền cho ROLE_ADMIN (toàn quyền)
INSERT INTO role_authorities (role_id, authority_id)
SELECT r.id, a.id
FROM roles r, authorities a
WHERE r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

-- 8. Gán quyền cơ bản cho ROLE_USER (chỉ đọc thông tin của mình)
INSERT INTO role_authorities (role_id, authority_id)
SELECT r.id, a.id
FROM roles r, authorities a
WHERE r.name = 'ROLE_USER' AND a.name IN ('USER_READ', 'WALLET_READ')
ON CONFLICT DO NOTHING;

-- 9. Tự động gán ROLE_USER cho tất cả người dùng hiện có trong hệ thống
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE r.name = 'ROLE_USER'
ON CONFLICT DO NOTHING;
