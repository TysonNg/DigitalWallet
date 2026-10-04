# 💳 Digital Wallet System

Hệ thống backend lõi cho **Ví điện tử (Digital Wallet)** được xây dựng theo kiến trúc **Domain-Driven Design (DDD)**, tuân thủ các tiêu chuẩn bảo mật tài chính khắt khe (**OWASP**), kiểm soát đồng thời (Concurrency Control) và đảm bảo tính toàn vẹn dữ liệu giao dịch.

---

## 🏗️ Cấu trúc dự án (Enterprise Monorepo)

```text
DigitalWallet/
├── backend/                      # Mã nguồn Backend Spring Boot đa module (DDD)
│   ├── digitalwallet-domain/          # Core Domain: Entities, Value Objects, Domain Ports
│   ├── digitalwallet-application/     # Application: Use Cases, Commands, DTOs
│   ├── digitalwallet-controller/      # Presentation: REST Controllers, Global Exception
│   ├── digitalwallet-infrastructure/  # Infrastructure: JPA, Flyway, Security, Redis
│   ├── digitalwallet-start/           # Bootstrapper: Main Application, Configs
│   └── pom.xml
│
├── frontend/                     # Mã nguồn Giao diện người dùng (Next.js / React)
│   └── README.md
│
├── deploy/                       # Cấu hình môi trường triển khai & hạ tầng
│   ├── docker-compose.yml             # Khởi chạy PostgreSQL 16 & Redis 7
│   └── README.md
│
├── docs/                         # Tài liệu thiết kế hệ thống & CSDL
│   ├── architecture.md                # Sơ đồ kiến trúc DDD, Luồng xác thực
│   └── database-schema.md             # Sơ đồ quan hệ thực thể (ERD) & Ràng buộc toàn vẹn
│
└── README.md                     # Tài liệu tổng quan dự án
```

---

## 🛠️ Công nghệ cốt lõi

- **Ngôn ngữ & Nền tảng**: Java 21+, Spring Boot 3.x / 4.x
- **Kiến trúc phần mềm**: Domain-Driven Design (DDD), Clean Architecture / Hexagonal
- **Cơ sở dữ liệu**: PostgreSQL 16 (Quan hệ 1-1, Check Constraints `balance >= 0`, Unique Email/Phone)
- **Database Migration**: Flyway (Quản lý phiên bản CSDL tự động)
- **Bộ nhớ đệm & Phiên**: Redis 7 (Quản lý Blacklist Token, Idempotency, Session Store)
- **Bảo mật**: Spring Security, Stateless Authentication (JWT), Băm mật khẩu chuẩn **Argon2id** (OWASP #1)
- **Giao diện & BFF**: Next.js 15 (App Router, React 19, TypeScript), Tailwind CSS, Iron Session (BFF Token Encryption)
- **Đóng gói & Hạ tầng**: Docker, Docker Compose (Multi-stage build)

---

## 🚀 Hướng dẫn khởi chạy nhanh (Quick Start)

### Cách 1: Khởi chạy toàn bộ hệ thống bằng Docker Compose (Khuyên dùng)
Chỉ cần chạy **1 câu lệnh duy nhất** tại thư mục gốc của dự án:
```bash
docker compose -f deploy/docker-compose.yml up -d --build
```
Lệnh trên sẽ tự động build và khởi động toàn bộ:
- **Frontend (Next.js BFF)**: [http://localhost:3000](http://localhost:3000)
- **Backend API**: [http://localhost:8080/api/v1](http://localhost:8080/api/v1)
- **pgAdmin 4**: [http://localhost:5050](http://localhost:5050)
- **PostgreSQL 16**: `localhost:5432` (User: `admin`, Password: `admin123`, DB: `digital_wallet`)
- **Redis 7**: `localhost:6379`

---

### Cách 2: Chạy từng phần cục bộ (Dành cho Development)

#### 1. Khởi chạy Database & Cache:
```bash
docker compose -f deploy/docker-compose.yml up -d postgres redis
```

#### 2. Khởi chạy Backend (Spring Boot):
```bash
cd backend
mvn clean spring-boot:run -pl digitalwallet-start
```
Backend API sẽ hoạt động tại `http://localhost:3006/api/v1`.

#### 3. Khởi chạy Frontend (Next.js):
```bash
cd frontend
pnpm dev
```
Frontend web sẽ hoạt động tại `http://localhost:3000`.

---

## 📡 Danh sách API chính

| Nhóm | Endpoint | Method | Mô tả |
| :--- | :--- | :--- | :--- |
| **Auth** | `/api/v1/auth/register` | `POST` | Đăng ký tài khoản (Băm mật khẩu bằng Argon2, kiểm tra tuổi $\ge 18$) |
| **Auth** | `/api/v1/auth/login` | `POST` | Đăng nhập hệ thống (Đối chiếu mật khẩu, cấp JWT Token) |
| **User** | `/api/v1/users/{id}` | `GET` | Xem thông tin chi tiết người dùng |
| **User** | `/api/v1/users/email` | `POST` | Thay đổi email người dùng |
| **Wallet** | `/api/v1/wallet` | `POST` | Tạo ví điện tử mới (Mỗi user chỉ có 1 ví duy nhất) |
| **Wallet** | `/api/v1/wallet/{id}` | `GET` | Lấy thông tin ví theo Wallet ID |
| **Wallet** | `/api/v1/wallet/user/{userId}` | `GET` | Lấy thông tin ví theo User ID |
| **Wallet** | `/api/v1/wallet/deposit` | `POST` | Nạp tiền vào ví điện tử |
| **Wallet** | `/api/v1/wallet/withdraw` | `POST` | Rút tiền khỏi ví (Kiểm tra số dư, trạng thái ví) |

---

## 📚 Tài liệu chi tiết
- Chi tiết kiến trúc phân tầng: [docs/architecture.md](docs/architecture.md)
- Thiết kế cơ sở dữ liệu & ERD: [docs/database-schema.md](docs/database-schema.md)
- Hướng dẫn hạ tầng Docker: [deploy/README.md](deploy/README.md)
