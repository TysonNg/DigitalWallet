# Triển khai bản demo lên VPS

Hiện chưa có VPS/domain. Các file này chuẩn bị quy trình; không phải bằng chứng đã triển khai.
Phù hợp demo dữ liệu giả và thanh toán sandbox trên một VPS Linux AMD64. Không phải cấu hình vận hành tiền thật.

## Chuẩn bị

- VPS Ubuntu có Docker Engine và Compose plugin hỗ trợ `up --wait`, Git, curl, OpenSSL, Certbot và `flock`.
- Domain trỏ A record về IP VPS; chỉ thêm AAAA khi IPv6 thực sự hoạt động.
- Cho phép SSH từ địa chỉ quản trị, HTTP 80 và HTTPS 443. Các dịch vụ dữ liệu không publish cổng ra host trong Compose production.
- Stack gồm Java, Next.js, PostgreSQL/pgvector, Kafka và MinIO. Theo dõi RAM thực tế; tránh bắt đầu bằng VPS quá ít RAM. Monitoring là profile tùy chọn.
- CI xanh và cả bốn job GHCR thành công ở cùng một commit. Image hiện là `linux/amd64`.

```bash
git clone https://github.com/TysonNg/DigitalWallet.git
cd DigitalWallet
# Checkout đúng commit tương ứng với bộ image đã xuất bản.
git checkout <full-commit-sha>
cp environment/.env.prod.example environment/.env.prod
chmod 600 environment/.env.prod
```

Điền `.env.prod` bằng trình soạn thảo trên VPS. Tạo các secret khác nhau bằng `openssl rand -hex 32`; không gửi chúng lên chat hay commit Git. SESSION_SECRET tối thiểu 32 ký tự. Điền SMTP thật nếu cần OTP. Với portfolio chỉ dùng Stripe test mode/PayOS sandbox phù hợp; các tính năng không có credential chưa được coi là đã kiểm chứng.

Nếu package private, đăng nhập GHCR trên VPS bằng cơ chế chính thức với token chỉ đọc package và `--password-stdin`. Không lưu token trong repository.

## HTTPS lần đầu

Cổng 80 phải đang trống; chưa khởi động Nginx production ở bước này:

```bash
sudo certbot certonly --standalone -d wallet.example.com
mkdir -p environment/certbot/www
```

Thay domain ví dụ bằng domain của bạn. Nginx dùng `/etc/letsencrypt/live/<domain>/fullchain.pem` và `privkey.pem` từ host qua mount chỉ đọc. Không dùng chứng chỉ localhost cho demo công khai.

## Deploy

```bash
bash environment/scripts/deploy.sh sha-<full-40-character-commit-sha> wallet.example.com
```

Script đọc `.env.prod`, kiểm tra Compose, pull image, chờ health của dịch vụ, khóa tài khoản admin seed còn mật khẩu công khai, tạo lại Nginx để cập nhật DNS container, rồi kiểm tra HTTPS và trang `/vi`.
Các file image phải cùng commit SHA; file mount trên VPS cũng phải checkout cùng phiên bản. Không chạy chồng hai lần deploy.

Production dùng `ddl-auto=validate`; Flyway phải tạo đủ schema. Nếu health thất bại, xem log và sửa migration/schema; không bật tự sửa schema để che lỗi.

```bash
docker compose --env-file environment/.env.prod -f environment/docker-compose.prod.yml ps
docker compose --env-file environment/.env.prod -f environment/docker-compose.prod.yml logs --tail=100 backend frontend nginx
```

Không chia sẻ log chứa thông tin cá nhân hay credential. Không chạy `down -v` trên môi trường cần giữ dữ liệu.

## Tài khoản quản trị demo

Migration V11 có tài khoản admin seed với mật khẩu công khai. Script deploy khóa tài khoản đó nếu hash vẫn là hash gốc, trước khi mở Nginx. Không công bố tài khoản quản trị dùng chung.
Để có admin riêng: đăng ký một tài khoản mới qua UI, xác minh OTP, rồi người quản trị VPS cấp ROLE_ADMIN cho đúng email qua PostgreSQL. Dùng mật khẩu riêng và chỉ chia sẻ tài khoản user thử nghiệm có quyền hạn thấp nếu cần.

```bash
docker compose --env-file environment/.env.prod -f environment/docker-compose.prod.yml exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Trong psql, thay email bằng tài khoản đã xác minh; kiểm tra id/email trước khi chạy INSERT:

```sql
SELECT id, email, status FROM users WHERE email = 'your-private-admin@example.com';
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN roles r
WHERE u.email = 'your-private-admin@example.com' AND u.status = 'ACTIVE' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;
```

Đăng nhập lại để nhận claims mới.

## Gia hạn chứng chỉ

Sau deploy đầu tiên, chuyển renewal của chứng chỉ sang webroot để không phải dừng Nginx:

```bash
sudo certbot reconfigure --cert-name wallet.example.com --webroot --webroot-path /absolute/path/DigitalWallet/environment/certbot/www
sudo certbot renew --dry-run
```

Bật timer Certbot của hệ điều hành. Thêm deploy hook gọi script sau từ đường dẫn tuyệt đối (với đúng người dùng có quyền Docker):

```bash
#!/bin/sh
cd /absolute/path/DigitalWallet
docker compose --env-file environment/.env.prod -f environment/docker-compose.prod.yml exec -T nginx nginx -s reload
```

Kiểm tra hook với chứng chỉ thử và xác nhận timer hoạt động; renew thành công không tự chứng minh Nginx đã nạp cert mới.

## Backup và phục hồi

```bash
bash environment/scripts/backup-db.sh
```

Lệnh tạo PostgreSQL custom-format dump trong `environment/backups/` với quyền riêng tư. Lên lịch, mã hóa và sao chép ra ngoài VPS; bản backup cùng ổ đĩa không bảo vệ khỏi mất VPS.
MinIO chứa ảnh/QR và phải được backup riêng. Để có bản nhất quán cho demo: dừng ghi từ backend/frontend, dump DB và snapshot/copy volume media, rồi khởi động lại ứng dụng. Kafka/Redis không thay thế backup PostgreSQL/MinIO. Có thể dùng snapshot VPS với các ứng dụng đã dừng ghi.

Thử phục hồi vào môi trường/database riêng trước: dùng `pg_restore --list <dump>` để kiểm tra archive, phục hồi bằng `pg_restore --no-owner --no-privileges -d <empty-test-db> <dump>`, phục hồi media tương ứng, rồi kiểm tra số dư/lịch sử/QR. Không thử restore đè database đang chạy.

## Rollback

Sau deploy thành công, tag hiện tại/trước đó lưu ở `.deployed-image-tag` và `.previous-image-tag` trên VPS. Muốn quay lại, chạy deploy với tag cũ sau khi kiểm tra migration tương thích. Script không tự đảo migration Flyway và không tự rollback khi lỗi; lần deploy thất bại có thể để lại hệ thống cập nhật một phần. Cần kiểm tra trạng thái và log trước khi thao tác tiếp.

## Monitoring tùy chọn

```bash
docker compose --env-file environment/.env.prod -f environment/docker-compose.prod.yml --profile observability up -d prometheus grafana
ssh -L 3001:127.0.0.1:3001 -L 9090:127.0.0.1:9090 <user>@<vps>
```

Grafana/Prometheus chỉ publish vào loopback để truy cập bằng SSH tunnel. Actuator bị chặn ở Nginx public; health và metrics vẫn dùng trong mạng Docker.

## Nguồn tham khảo

- [Docker Compose production](https://docs.docker.com/compose/how-tos/production/)
- [Startup và health check](https://docs.docker.com/compose/how-tos/startup-order/)
- [Nginx image templates](https://hub.docker.com/_/nginx)
- [Certbot renewal](https://eff-certbot.readthedocs.io/en/stable/using.html)
