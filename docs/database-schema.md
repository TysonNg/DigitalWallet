# 🗄️ Database Schema & Entity Relationship Diagram (ERD)

Hệ cơ sở dữ liệu: **PostgreSQL 16**  
Công cụ quản lý phiên bản: **Flyway Database Migration**

---

## 1. Sơ đồ Quan hệ Thực thể (ERD)

```mermaid
erDiagram
    USERS ||--|| WALLETS : "has exactly one (1:1)"
    WALLETS ||--o{ TRANSACTIONS : "records balance changes (1:N)"

    USERS {
        uuid id PK "Khóa chính"
        varchar full_name "Họ và tên (max 100)"
        varchar email UK "Email duy nhất (max 100)"
        varchar password "Mã băm mật khẩu Argon2id"
        varchar phone_number UK "Số điện thoại duy nhất (max 20)"
        date dob "Ngày sinh (CHECK <= CURRENT_DATE)"
        varchar status "Trạng thái: ACTIVE, SUSPENDED, CLOSED"
        timestamp created_at "Thời gian tạo"
        timestamp updated_at "Thời gian cập nhật gần nhất"
    }

    WALLETS {
        uuid id PK "Khóa chính ví"
        uuid user_id FK,UK "Khóa ngoại trỏ sang USERS (ON DELETE RESTRICT)"
        decimal balance "Số dư tài khoản (CHECK balance >= 0)"
        varchar currency "Loại tiền tệ (mặc định 'VND')"
        varchar status "Trạng thái: ACTIVE, FROZEN, CLOSED"
        timestamp created_at "Thời gian tạo ví"
        timestamp updated_at "Thời gian cập nhật ví"
    }

    TRANSACTIONS {
        uuid id PK "Mã giao dịch"
        uuid wallet_id FK "Khóa ngoại trỏ sang WALLETS"
        varchar type "DEPOSIT, WITHDRAW, TRANSFER"
        decimal amount "Số tiền giao dịch (> 0)"
        decimal balance_before "Số dư trước giao dịch"
        decimal balance_after "Số dư sau giao dịch"
        varchar status "PENDING, SUCCESS, FAILED"
        varchar idempotency_key UK "Khóa chống lặp giao dịch"
        timestamp created_at "Thời điểm thực hiện giao dịch"
    }
```

---

## 2. Các Ràng buộc An toàn Cấp Database (Integrity Constraints)

1. **Bảo vệ số dư không âm (`chk_wallets_balance`)**:
   - `CHECK (balance >= 0)`: Ngăn chặn triệt để tình trạng số dư bị âm ở tầng vật lý, bất chấp lỗi logic từ code.
2. **Ngăn chặn xóa người dùng có ví (`fk_wallets_user`)**:
   - `ON DELETE RESTRICT`: Tuyệt đối không cho phép xóa bản ghi `users` nếu ví của họ vẫn đang tồn tại nhằm bảo vệ dữ liệu tài chính.
3. **Mỗi người dùng sở hữu duy nhất 1 ví (`uq_wallets_user_id`)**:
   - Ràng buộc `UNIQUE(user_id)` trên bảng `wallets`.
4. **Độ tuổi hợp lệ (`chk_users_dob`)**:
   - `CHECK (dob <= CURRENT_DATE)`: Ngày sinh không thể nằm ở tương lai.
