# 🚀 Digital Wallet - Full-stack Deployment Infrastructure

Thư mục này chứa cấu hình Docker Compose để khởi chạy toàn bộ hệ thống Digital Wallet (Database, Cache, pgAdmin, Backend Spring Boot và Frontend Next.js BFF) chỉ với **một câu lệnh duy nhất**.

---

## 📦 Danh sách dịch vụ & Cổng truy cập:

| Dịch vụ | Công nghệ | Container Name | Cổng Host (Mapped) | Cổng Nội bộ Docker | URL truy cập |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Nginx (Gateway, SSL & LB)** | Nginx 1.27 Alpine | `digital_wallet_nginx` | `80`, `443` | `80`, `443` | [https://localhost](https://localhost) (tự chuyển hướng từ HTTP) |
| **Frontend (BFF)** | Next.js 15 (Node 22 Alpine) | Tự động (scale-ready) | `3000` | `3000` | Qua Nginx: [https://localhost](https://localhost) hoặc direct [http://localhost:3000](http://localhost:3000) |
| **Backend API** | Spring Boot (Java 21) | Tự động (scale-ready) | `8080`, `3006` | `3006` | Qua Nginx: [https://localhost/api/v1](https://localhost/api/v1) hoặc direct [http://localhost:3006/api/v1](http://localhost:3006/api/v1) |
| **Grafana** | Observability & Dashboard | `digital_wallet_grafana` | `3001` | `3000` | [http://localhost:3001](http://localhost:3001) (admin/admin) |
| **Prometheus** | Metrics Monitoring | `digital-wallet-prometheus` | `9090` | `9090` | [http://localhost:9090](http://localhost:9090) |
| **pgAdmin 4** | Web GUI cho PostgreSQL | `digital_wallet_pgadmin` | `5050` | `80` | [http://localhost:5050](http://localhost:5050) |
| **PostgreSQL 16** | Database chính | `digital_wallet_postgres` | `5432` | `5432` | `localhost:5432` |
| **Redis 7** | Cache, Token Blacklist & OTP | `digital_wallet_redis` | `6379` | `6379` | `localhost:6379` |

---

## 🛠 Hướng dẫn khởi chạy

### 1. Chuẩn bị biến môi trường
Kiểm tra file `environment/.env` (được copy từ `environment/.env.example` nếu chưa có). Cập nhật thông tin SMTP Gmail nếu cần gửi email mã OTP đăng ký.

### 2. Khởi động toàn bộ hệ thống (1 câu lệnh)
Từ thư mục gốc của dự án:
```bash
docker compose -f environment/docker-compose.yml up -d --build
```
*(Nếu đang đứng ngay trong thư mục `environment/`, bạn chỉ cần chạy `docker compose up -d --build`)*

Hệ thống sẽ tự động:
1. Build image Spring Boot Backend từ mã nguồn `backend/`.
2. Build image Next.js Frontend Standalone từ mã nguồn `frontend/`.
3. Khởi tạo Database PostgreSQL và Cache Redis cùng các cơ chế healthcheck.
4. Chờ DB & Redis `healthy`, khởi động Backend.
5. Khởi động Prometheus thu thập metrics và Grafana tự nạp Dashboard sẵn sàng sử dụng.
6. Khởi động Frontend BFF kết nối tới Backend thông qua DNS mạng Docker `http://backend:3006/api/v1`.
7. Khởi động Nginx đóng vai trò SSL Termination, Reverse Proxy & Load Balancer tại cổng `80` và `443` điều hướng toàn bộ lưu lượng.

---

## 🔒 Nginx SSL/TLS Termination, Reverse Proxy & Load Balancer

### 1. Kiến trúc điều hướng & SSL Termination
```text
                           [ Khách hàng / Trình duyệt ]
                                        │
                         ┌──────────────┴──────────────┐
                         ▼ (Port 80 - HTTP)            ▼ (Port 443 - HTTPS)
                     [ Redirect 301 ]            [ TLS/SSL Termination ]
                         └──────────────►──────────────┘
                                        │ (Giải mã mã hóa)
                             [ NGINX REVERSE PROXY ]
                                        │ (HTTP thuần nội bộ Docker)
              ┌─────────────────────────┴─────────────────────────┐
              │ (Location /api/v1/*, /api/v1/ws)                  │ (Location /*, /_next/static/*)
              ▼                                                   ▼
   [ Upstream backend_cluster ]                        [ Upstream frontend_cluster ]
      (Thuật toán: least_conn)                            (Thuật toán: least_conn)
       ├── Backend Instance 1 (Port 3006)                  ├── Frontend Instance 1 (Port 3000)
       └── Backend Instance 2 (Port 3006, nếu scale)       └── Frontend Instance 2 (Port 3000, nếu scale)
```

- **HTTPS / SSL Termination**:
  - Lắng nghe cổng `443` (HTTPS/HTTP2) với chứng chỉ SSL/TLS bảo mật. Toàn bộ mã hóa/giải mã được Nginx đảm nhiệm (SSL Offloading), giảm tải CPU cho Backend và Frontend.
  - Cổng `80` (HTTP) tự động chuyển hướng 301 sang `https://...`.
  - Hỗ trợ đầy đủ **HSTS (HTTP Strict Transport Security)** và các chuẩn mã hóa hiện đại (TLSv1.2, TLSv1.3).
  - Tự động sinh chứng chỉ Self-signed SSL cho `localhost` ngay khi container Nginx khởi động lần đầu nếu chưa có chứng chỉ.
- **Reverse Proxy**:
  - `https://localhost/`: Điều hướng tới cụm **Frontend Next.js (BFF)**.
  - `https://localhost/api/v1/*`: Điều hướng tới cụm **Backend Spring Boot REST API**.
  - `https://localhost/api/v1/ws`: Điều hướng WebSocket STOMP realtime notifications qua **WSS (Secure WebSocket)**.
  - `https://localhost/_next/static/*`: Tối ưu cache trực tiếp cho các static asset của Next.js (365 ngày).
  - `http://localhost/health` hoặc `https://localhost/health`: Endpoint kiểm tra trạng thái Nginx.
- **Load Balancer**:
  - Sử dụng thuật toán `least_conn` (phân phối request tới container có ít kết nối đang xử lý nhất).
  - Tự động chuyển hướng dự phòng (`proxy_next_upstream error timeout http_502 http_503 http_504`) nếu một container backend gặp lỗi hoặc đang khởi động lại.
  - Tái sử dụng kết nối HTTP Keepalive (`keepalive 32`) nhằm tối ưu thông lượng và giảm latency.
  - Tích hợp log chi tiết `upstream_addr`, `upstream_status`, `upstream_response_time` để quan sát cân bằng tải trong thời gian thực.

### 2. Cách mở rộng quy mô (Scale Out) nhiều Instance Backend:
Để scale backend lên nhiều container nhằm chịu tải cao:
```bash
# Scale backend lên 2 instances
docker compose -f environment/docker-compose.yml up -d --scale backend=2

# Nạp lại cấu hình upstream Nginx (nếu cần re-resolve IP mới)
docker compose -f environment/docker-compose.yml exec nginx nginx -s reload

# Xem log phân phối tải giữa các địa chỉ IP upstream
docker compose -f environment/docker-compose.yml logs -f nginx
```
*(Lưu ý: Khi scale nhiều instance, hãy tắt mapping port cố định trên host `3006:3006` trong `docker-compose.yml` để tránh xung đột cổng host, lúc này toàn bộ truy cập đi qua cổng 80 của Nginx).*

---

### 3. Kiểm tra trạng thái các containers
```bash
docker compose -f environment/docker-compose.yml ps
```

### 4. Xem log các dịch vụ
- Xem log toàn bộ:
  ```bash
  docker compose -f environment/docker-compose.yml logs -f
  ```
- Xem log riêng Frontend:
  ```bash
  docker compose -f environment/docker-compose.yml logs -f frontend
  ```
- Xem log riêng Backend:
  ```bash
  docker compose -f environment/docker-compose.yml logs -f backend
  ```
- Xem log riêng Prometheus/Grafana:
  ```bash
  docker compose -f environment/docker-compose.yml logs -f grafana
  ```

### 5. Dừng các dịch vụ (vẫn lưu trữ dữ liệu DB & Redis & Grafana)
```bash
docker compose -f environment/docker-compose.yml stop
```

### 6. Xóa containers (giữ nguyên dữ liệu trong Docker Volumes)
```bash
docker compose -f environment/docker-compose.yml down
```

### 7. Xóa sạch dữ liệu (Reset hoàn toàn Database, Redis, pgAdmin, Grafana)
```bash
docker compose -f environment/docker-compose.yml down -v
```


## Private media storage

MinIO, its private bucket initializer and application-scoped credentials are configured in Compose. Existing `.env` files need the media variables from `.env.example` before starting Compose. See [media storage setup, API and backup instructions](../docs/architecture/media-storage.md).
