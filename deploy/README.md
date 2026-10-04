# 🚀 Digital Wallet - Full-stack Deployment Infrastructure

Thư mục này chứa cấu hình Docker Compose để khởi chạy toàn bộ hệ thống Digital Wallet (Database, Cache, pgAdmin, Backend Spring Boot và Frontend Next.js BFF) chỉ với **một câu lệnh duy nhất**.

---

## 📦 Danh sách dịch vụ & Cổng truy cập:

| Dịch vụ | Công nghệ | Container Name | Cổng Host (Mapped) | Cổng Nội bộ Docker | URL truy cập |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Frontend (BFF)** | Next.js 15 (Node 22 Alpine) | `digital_wallet_frontend` | `3000` | `3000` | [http://localhost:3000](http://localhost:3000) |
| **Backend API** | Spring Boot (Java 21) | `digital_wallet_backend` | `8080` | `3006` | [http://localhost:8080/api/v1](http://localhost:8080/api/v1) |
| **pgAdmin 4** | Web GUI cho PostgreSQL | `digital_wallet_pgadmin` | `5050` | `80` | [http://localhost:5050](http://localhost:5050) |
| **PostgreSQL 16** | Database chính | `digital_wallet_postgres` | `5432` | `5432` | `localhost:5432` |
| **Redis 7** | Cache, Token Blacklist & OTP | `digital_wallet_redis` | `6379` | `6379` | `localhost:6379` |

---

## 🛠 Hướng dẫn khởi chạy

### 1. Chuẩn bị biến môi trường
Kiểm tra file `deploy/.env` (được copy từ `deploy/.env.example` nếu chưa có). Cập nhật thông tin SMTP Gmail nếu cần gửi email mã OTP đăng ký.

### 2. Khởi động toàn bộ hệ thống (1 câu lệnh)
Từ thư mục gốc của dự án:
```bash
docker compose -f deploy/docker-compose.yml up -d --build
```
*(Nếu đang đứng ngay trong thư mục `deploy/`, bạn chỉ cần chạy `docker compose up -d --build`)*

Hệ thống sẽ tự động:
1. Build image Spring Boot Backend từ mã nguồn `backend/`.
2. Build image Next.js Frontend Standalone từ mã nguồn `frontend/`.
3. Khởi tạo Database PostgreSQL và Cache Redis cùng các cơ chế healthcheck.
4. Chờ DB & Redis `healthy`, khởi động Backend.
5. Khởi động Frontend BFF kết nối tới Backend thông qua DNS mạng Docker `http://backend:3006/api/v1`.

### 3. Kiểm tra trạng thái các containers
```bash
docker compose -f deploy/docker-compose.yml ps
```

### 4. Xem log các dịch vụ
- Xem log toàn bộ:
  ```bash
  docker compose -f deploy/docker-compose.yml logs -f
  ```
- Xem log riêng Frontend:
  ```bash
  docker compose -f deploy/docker-compose.yml logs -f frontend
  ```
- Xem log riêng Backend:
  ```bash
  docker compose -f deploy/docker-compose.yml logs -f backend
  ```

### 5. Dừng các dịch vụ (vẫn lưu trữ dữ liệu DB & Redis)
```bash
docker compose -f deploy/docker-compose.yml stop
```

### 6. Xóa containers (giữ nguyên dữ liệu trong Docker Volumes)
```bash
docker compose -f deploy/docker-compose.yml down
```

### 7. Xóa sạch dữ liệu (Reset hoàn toàn Database, Redis, pgAdmin)
```bash
docker compose -f deploy/docker-compose.yml down -v
```

