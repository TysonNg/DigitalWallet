# Frontend Code Style & Formatting Rules

Mọi mã nguồn trong thư mục `frontend/` (Next.js, React, TypeScript, TSX, CSS) BẮT BUỘC tuân thủ các quy chuẩn sau:

---

## 1. Thụt đầu dòng (Indentation / Tab Size = 4)
- **BẮT BUỘC dùng 4 spaces** cho mỗi mức thụt lề (tab size = 4).
- **TUYỆT ĐỐI KHÔNG dùng 2 spaces**.
- Áp dụng cho toàn bộ file `.ts`, `.tsx`, `.js`, `.jsx`, `.json`, `.css`.

---

## 2. Khoảng cách dòng thoáng & Ngắt dòng logic (Gap Line / Spacing)
- **Tuyệt đối không viết code dính chùm**: Các khối lệnh, logic khác nhau phải được phân cách bằng **1 dòng trắng (blank line)** rõ ràng để tăng tính dễ đọc.
- **Quy tắc cách dòng cụ thể**:
  1. **Sau cụm import**: Cách 1 dòng trắng trước khi khai báo kiểu dữ liệu hoặc component.
  2. **Giữa các interface / type definitions**: Cách 1 dòng trắng.
  3. **Bên trong Component**:
     - Cách 1 dòng trắng giữa phần gọi Hooks (e.g., `useState`, `useRouter`, `useForm`).
     - Cách 1 dòng trắng giữa Hooks và các hàm xử lý sự kiện (`handleSubmit`, `handleAction`,...).
     - Cách 1 dòng trắng giữa các hàm xử lý sự kiện.
     - Cách 1 dòng trắng trước lệnh `return (` trả về JSX.
  4. **Bên trong JSX**:
     - Các khối UI lớn (`<CardHeader>`, `<CardContent>`, `<CardFooter>`, `<form>`, `<div className="space-y-4">`) nên được ngắt dòng rõ ràng, không dồn nén trên cùng 1 dòng dài.
  5. **Giữa các hàm / methods**: Bắt buộc cách 1 dòng trắng.

---

## 3. Tooling & Enforcement
- Kiểm tra file cấu hình [.editorconfig](file:///d:/Projects/DigitalWallet/frontend/.editorconfig) và [.prettierrc](file:///d:/Projects/DigitalWallet/frontend/.prettierrc) (`tabWidth: 4`).
- Sau khi viết/sửa code, có thể chạy `pnpm format` để đảm bảo chuẩn 4 spaces toàn bộ.
