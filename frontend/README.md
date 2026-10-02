# 💻 Digital Wallet - Frontend

Thư mục này dành cho ứng dụng Frontend (Next.js / React) phục vụ giao diện người dùng cho hệ thống Ví điện tử.

## 🎯 Kế hoạch công nghệ:
- **Framework**: Next.js 14+ (App Router) hoặc React (Vite)
- **UI & Styling**: Tailwind CSS / Vanilla CSS + Lucide Icons
- **State & Form**: React Hook Form + Zod
- **API Client**: Axios / Fetch với Interceptor tự động đính kèm `Bearer Token`

## 📱 Các màn hình dự kiến:
1. **Authentication**:
   - Đăng ký tài khoản (Họ tên, Email, Mật khẩu, SĐT, Ngày sinh).
   - Đăng nhập (Email, Mật khẩu) ➔ Lưu JWT vào cookie / localStorage.
2. **Dashboard chính**:
   - Hiển thị số dư ví điện tử hiện tại (VND).
   - Trạng thái ví (ACTIVE, FROZEN).
   - Danh sách biến động số dư gần đây (Lịch sử Nạp/Rút).
3. **Giao dịch**:
   - Form Nạp tiền (Deposit).
   - Form Rút tiền (Withdraw).
   - Form Chuyển tiền (P2P Transfer).
