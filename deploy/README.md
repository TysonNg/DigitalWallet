# 🚀 Deployment Infrastructure

Thư mục này chứa cấu hình Docker Compose để khởi chạy các dịch vụ hạ tầng phụ trợ (Database, Cache) cho Digital Wallet.

## 📦 Các dịch vụ bao gồm:
1. **PostgreSQL 16**: Port `5432`, Database: `digital_wallet`, User: `admin`, Password: `admin123`
2. **Redis 7**: Port `6379`, không yêu cầu password ở môi trường development.

---

## 🛠 Hướng dẫn khởi chạy:

### 1. Khởi động PostgreSQL và Redis
Từ thư mục gốc của dự án hoặc thư mục `deploy/`, chạy lệnh:
```bash
docker compose -f deploy/docker-compose.yml up -d
```
*(Nếu đang đứng ngay trong thư mục `deploy/`, chỉ cần gõ `docker compose up -d`)*

### 2. Kiểm tra trạng thái containers
```bash
docker compose -f deploy/docker-compose.yml ps
```

### 3. Dừng các dịch vụ (vẫn giữ nguyên dữ liệu)
```bash
docker compose -f deploy/docker-compose.yml stop
```

### 4. Xóa containers (giữ nguyên dữ liệu trong Docker Volumes)
```bash
docker compose -f deploy/docker-compose.yml down
```

### 5. Xóa sạch dữ liệu (Reset hoàn toàn Database & Redis)
```bash
docker compose -f deploy/docker-compose.yml down -v
```
