# 💻 Digital Wallet - Frontend (Next.js BFF Architecture)

Giao diện người dùng cho hệ thống Ví điện tử (Digital Wallet) được xây dựng bằng **Next.js (App Router)** theo mô hình **Backend-For-Frontend (BFF)** hiện đại, tuân thủ tiêu chuẩn an ninh tài chính **OWASP & Token-Handler Pattern**.

---

## 🛠️ Công nghệ cốt lõi

- **Framework**: Next.js 15+ (App Router, Turbopack, Server Actions)
- **Styling**: Tailwind CSS v4, phong cách **Clean Minimalist** (tối giản, sắc nét, không gradient, viền mỏng)
- **BFF Session**: `iron-session` (Mã hóa phiên AES-256-GCM qua HttpOnly Cookie, trình duyệt không lưu bất kỳ chuỗi JWT nào)
- **Validation**: React Hook Form + Zod
- **Client State**: Zustand
- **Icons**: Lucide Icons

---

## 🏗️ Cấu trúc thư mục (Feature-Based Architecture)

```text
frontend/src/
├── app/                        # Routing & Layouts
│   ├── (auth)/                 # Route Group cho xác thực
│   │   ├── login/page.tsx      # Màn hình Đăng nhập
│   │   └── register/page.tsx   # Màn hình Đăng ký 2 bước (OTP)
│   ├── (dashboard)/            # Route Group bảo vệ (yêu cầu đăng nhập)
│   │   ├── layout.tsx          # Dashboard Header, User info, Logout button
│   │   └── dashboard/page.tsx  # Trang Bảng điều khiển ví tiền
│   ├── layout.tsx              # Root Layout
│   ├── page.tsx                # Trang chủ / Điều hướng phiên
│   └── globals.css             # Tailwind v4 theme configuration
│
├── features/                   # Logic gom theo tính năng (Co-location)
│   └── auth/
│       ├── actions/            # Server Actions (BFF handlers)
│       │   └── auth.actions.ts # loginAction, registerAction, verifyOtpAction, logoutAction
│       ├── components/         # LoginForm, RegisterForm, VerifyOtpForm, AuthHeader
│       ├── schemas/            # auth.schema.ts (Zod validation)
│       ├── store/              # auth.store.ts (Zustand state)
│       └── types/              # auth.types.ts
│
├── server/                     # [TẦNG BFF - CHỈ CHẠY PHÍA SERVER]
│   ├── session.ts              # Quản lý mã hóa iron-session (AES-256-GCM)
│   └── backend-client.ts       # HTTP Client gọi sang Spring Boot kèm Bearer Token
│
├── components/ui/              # Minimalist UI Kit (Button, Input, Label, Card, Badge)
└── lib/
    ├── env.ts                  # Biến môi trường (SPRING_BOOT_URL, SESSION_SECRET)
    └── utils.ts                # Helper cn, formatVND
```

---

## 🔐 Cơ chế bảo mật BFF (Token-Handler Pattern)

1. **Trình duyệt không lưu JWT:** Trình duyệt chỉ nhận và gửi cookie phiên `wallet_session` đã được mã hóa bằng thuật toán AES-256. Không có Access Token hay Refresh Token nào lưu ở `localStorage` hoặc biến JS của trình duyệt $\rightarrow$ **Miễn nhiễm 100% với tấn công XSS**.
2. **BFF tự động đính kèm Token:** Khi gọi các API ví tiền, Server Next.js tự giải mã phiên, lấy Access Token và gắn vào Header:
   ```http
   Authorization: Bearer <accessToken>
   ```
   rồi chuyển tiếp sang Backend Spring Boot qua mạng nội bộ.
3. **Tự động làm mới Token (Transparent Refresh):** Nếu Access Token hết hạn (401), BFF tự động dùng Refresh Token để xin cấp token mới và thử lại request mà người dùng không bị gián đoạn.

---

## 🚀 Hướng dẫn khởi chạy

### 1. Cài đặt thư viện
```bash
pnpm install
```

### 2. Cấu hình biến môi trường
File `.env.local` đã được thiết lập sẵn:
```env
SPRING_BOOT_URL=http://localhost:3006/api/v1
SESSION_SECRET=digital_wallet_secret_session_key_32_characters_minimum_length_required
NODE_ENV=development
```

### 3. Chạy môi trường phát triển (Development)
```bash
pnpm dev
```
Truy cập: `http://localhost:3000`

### 4. Build sản phẩm (Production)
```bash
pnpm build
pnpm start
```
