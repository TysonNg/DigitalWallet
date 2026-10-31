# Checklist demo và nội dung CV

Chỉ đánh dấu khi có kết quả thực tế. Hiện chưa có VPS/domain và chưa kiểm tra end-to-end công khai.

## Bằng chứng cần lưu

- [ ] Link GitHub Actions CI thành công của commit phát hành.
- [ ] Cả bốn image GHCR cùng SHA và digest được ghi nhận.
- [ ] Domain HTTPS truy cập được, không dùng chứng chỉ self-signed.
- [ ] Cookie phiên có Secure/HttpOnly; dịch vụ dữ liệu và Actuator không truy cập công khai.
- [ ] Admin seed công khai không đăng nhập được; admin riêng hoạt động.

## Kiểm tra nghiệp vụ trên dữ liệu giả

- [ ] Đăng ký, nhận OTP, xác minh, đăng nhập/đăng xuất, refresh phiên.
- [ ] Tạo ví, thiết lập PIN và đổi PIN; nhập PIN sai không chuyển tiền.
- [ ] Chuyển tiền giữa hai tài khoản, kiểm tra số dư và lịch sử cả hai phía.
- [ ] Không chuyển quá số dư/cho chính mình; gửi lặp không ghi nợ hai lần.
- [ ] Thanh toán sandbox: xác minh webhook, trạng thái pending/success/failure và số dư. Không dùng tiền thật.
- [ ] User thường không truy cập API admin hay ví/ảnh của người khác.
- [ ] Ảnh/QR private hoạt động với đúng quyền; WebSocket thông báo và AI streaming hoạt động nếu được cấu hình.
- [ ] Restart container không làm mất dữ liệu; thử phục hồi DB/media ở môi trường riêng.

## Ảnh minh họa nên chụp sau khi chạy demo

Dashboard, xác nhận PIN, lịch sử giao dịch, thanh toán sandbox và trang Actions xanh. Dùng tên/email giả; ẩn token, khóa API, IP riêng và thông tin quản trị.
Đưa ảnh đã kiểm tra vào `docs/images/` rồi thêm vào README. Không tạo ảnh giả như bằng chứng ứng dụng thật.

## Mô tả CV

Có thể ghi sau khi kiểm tra code/test:

> Xây dựng ví điện tử bằng Spring Boot và Next.js theo kiến trúc DDD; triển khai xác thực JWT qua BFF, PostgreSQL, Redis, Kafka, sổ cái ghi kép và kiểm thử nghiệp vụ; đóng gói Docker và cấu hình GitHub Actions CI/GHCR.

Chỉ thêm “triển khai trên VPS với HTTPS” khi domain hoạt động. Chỉ thêm kết quả hiệu năng khi đã có kịch bản đo, cấu hình máy và số liệu tái hiện được.
