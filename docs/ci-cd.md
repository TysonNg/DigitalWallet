# CI và xuất bản Docker image lên GHCR

## Luồng hoạt động

- `ci.yml` chạy khi tạo/cập nhật PR, push vào `main`/nhánh `codex/**`, hoặc chạy thủ công.
- Backend dùng Temurin Java 21, Maven `verify` và PostgreSQL 16 tạm thời. Các test nhận `outbox.test.jdbc` và `withdrawal.test.jdbc` chạy trên database CI riêng.
- Frontend dùng Node 22, pnpm 10.28.2: cài theo lockfile, lint, test, build rồi kiểm tra TypeScript.
- Báo cáo Maven lưu trong artifact `backend-test-reports`, kể cả khi test thất bại.
- `cd.yml` chỉ xuất bản image sau khi CI của sự kiện **push vào main** thành công. CI của PR hay lần chạy thủ công không xuất bản image.
- CD checkout đúng commit SHA đã qua CI, build và push bốn image: backend, frontend, nginx, media. Hai service `minio` và `minio-init` dùng chung image media.

## Thiết lập GitHub

1. Push các workflow lên `main` hoặc merge PR chứa các file này.
2. Trong **Settings > Actions > General**, cho phép GitHub Actions và các action được sử dụng theo policy của repo/organization.
3. CD tự xin quyền `contents: read` và `packages: write`, dùng `GITHUB_TOKEN` có sẵn. Không cần tạo PAT hay thêm secret registry. Policy organization vẫn phải cho phép xuất bản package.
4. Trong ruleset/branch protection của `main`, yêu cầu PR và hai check `Backend checks`, `Frontend checks` trước khi merge. Chạy CI một lần để các check xuất hiện trong danh sách.
5. Xem kết quả ở **Actions**, image ở **Packages** của owner. Nếu package cùng tên đã tồn tại, cấp quyền Actions cho repo này trong package settings.

CI còn build thử bốn Docker image bằng Buildx (`push: false`). Để bảo vệ main, nên yêu cầu cả bốn check `Container check (...)` bên cạnh backend/frontend.

## Image xuất bản

Owner/repo được chuyển thành chữ thường để đúng định dạng Docker:

```text
ghcr.io/<owner>/<repo>-backend:sha-<full-commit-sha>
ghcr.io/<owner>/<repo>-frontend:sha-<full-commit-sha>
ghcr.io/<owner>/<repo>-nginx:sha-<full-commit-sha>
ghcr.io/<owner>/<repo>-media:sha-<full-commit-sha>
```

Image build cho `linux/amd64`. Server ARM64 cần bổ sung nền tảng tương ứng.
Không dùng tag `latest` để tránh nhầm phiên bản. Khi cần định danh bất biến, dùng digest trong job summary.
Chỉ xem bản phát hành sẵn sàng khi **cả bốn job publish** thành công; nếu một job lỗi, các job khác có thể đã push một phần image.

## Phạm vi kiểm tra

Kafka và MinIO chưa được cấp trong CI. Các integration test cần `kafka.test.bootstrap-servers`, `media.test.jdbc` và `media.test.endpoint` vẫn skip theo điều kiện hiện có; CI xanh không có nghĩa các test này đã chạy.
Dockerfile backend dùng `-DskipTests`, nhưng chỉ được xuất bản sau khi CI đã chạy Maven `verify` thành công.

## Triển khai sau này

Hiện thực hiện CI và continuous delivery đến registry; **chưa tự động deploy lên server**, theo phạm vi đã chọn.

Khi có server:

- Thêm Compose production dùng image GHCR của cùng một commit, thay vì build source trên server.
- Chuẩn bị các file mount: Nginx/TLS, media init, Prometheus, Grafana. Chỉ pull image chưa đủ.
- Giữ secrets ứng dụng trên server/secret manager, không commit `.env` hoặc private key; không đưa secrets runtime vào build arguments.
- Nếu GHCR package private, cấp thông tin đăng nhập chỉ đọc package cho server.
- Thêm job deploy phụ thuộc toàn bộ job publish, GitHub Environment, health check và phục hồi phiên bản trước khi lỗi.
- Kiểm tra tương thích Flyway: rollback image không tự rollback database.

## Tài liệu

- [Build và test](https://docs.github.com/en/actions/tutorials/build-and-test-code)
- [Xuất bản Docker image](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images)
- [Container registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)

## Kết quả kiểm tra local sau khi sửa lint

- YAML của hai workflow đọc hợp lệ; `actionlint` kiểm tra cả hai file thành công.
- `pnpm test`: 84 test đạt, không fail/skip.
- `pnpm exec tsc --noEmit --incremental false`: đạt với dependencies hiện có.
- ESLint với `--max-warnings 0`: đạt, 0 lỗi và 0 cảnh báo; không tắt rule.
- `pnpm build`: tạo production build thành công.
- Maven `verify` trên JDK 26 local: BUILD SUCCESS, 304 test được báo cáo, 24 skipped, 0 failures/errors. Chưa xác nhận Java 21/Linux trên runner.
- Docker Desktop local lỗi khởi tạo Inference manager; CI có matrix build thử bốn image không publish. Kết quả GitHub/GHCR phải xác nhận bằng run thực tế.
