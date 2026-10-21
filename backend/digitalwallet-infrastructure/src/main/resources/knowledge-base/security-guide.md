# 🛡️ HƯỚNG DẪN AN TOÀN BẢO MẬT & MÃ PIN GIAO DỊCH - DIGITAL WALLET

*Cẩm nang bảo vệ tài khoản và tài sản cá nhân trong hệ thống Ví điện tử Digital Wallet.*
*Cập nhật lần cuối: 2026-10-21.*

---

## 1. Mã PIN Giao dịch (Transaction PIN)
* **Quy cách:** Mã PIN là chuỗi gồm đúng **6 chữ số** (000000 - 999999) do người dùng tự thiết lập.
* **Mục đích sử dụng:**
  - Bắt buộc phải nhập để xác thực mỗi khi thực hiện **Chuyển tiền** hoặc **Rút tiền**.
  - Không ai, kể cả nhân viên hỗ trợ hay Quản trị viên hệ thống, được quyền yêu cầu bạn cung cấp mã PIN.
* **Lưu ý bảo mật:** Không đặt mã PIN trùng ngày sinh nhật hoặc số điện thoại dễ đoán.

---

## 2. Cơ chế Phòng thủ Chống Tấn công Dò mã PIN (Brute-Force Protection)
* Hệ thống tích hợp bộ đệm phân tán Redis để giám sát số lần thử mã PIN sai:
  - Nếu người dùng nhập sai mã PIN **3 lần liên tiếp**: Hệ thống hiển thị cảnh báo đỏ và đếm số lần còn lại.
  - Nếu người dùng nhập sai mã PIN **5 lần liên tiếp**: Ví sẽ bị **KHÓA TÍNH NĂNG GIAO DỊCH TẠM THỜI TRONG 15 PHÚT**.
  - Trong thời gian 15 phút bị khóa, mọi yêu cầu giao dịch đều bị từ chối ngay lập tức để bảo vệ số dư tài khoản.

---

## 3. Quy trình Đổi hoặc Cấp lại Mã PIN khi bị quên
* **Trường hợp nhớ mã PIN cũ:** Người dùng vào mục *Cài đặt bảo mật ➜ Đổi mã PIN*, nhập mã PIN cũ và tạo mã PIN 6 số mới.
* **Trường hợp quên mã PIN hoàn toàn:**
  1. Nhấn nút *"Quên mã PIN?"* trên giao diện.
  2. Hệ thống tạo mã xác thực OTP 6 số ngẫu nhiên có hiệu lực trong 5 phút và gửi về Email đăng ký.
  3. Nhập đúng mã OTP để mở khóa và thiết lập mã PIN 6 số mới.

---

## 4. Nguyên tắc An toàn khi dùng Trợ lý AI
* Trợ lý AI của Digital Wallet **TUYỆT ĐỐI KHÔNG BAO GIỜ** yêu cầu bạn nhập mật khẩu hoặc mã PIN trong khung chat.
* Mọi thao tác xác thực mã PIN chỉ được thực hiện trên **Cửa sổ bảo mật chuyên biệt (Security Dialog)** của hệ thống.
