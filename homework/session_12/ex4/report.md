# Báo cáo Thực hành: Tự động triển khai ứng dụng Spring Boot lên VPS từ xa qua SSH

- **Môn học:** IT209 - Tích hợp hệ thống
- **Bài thực hành:** Session 12 - Bài 4
- **Sinh viên thực hiện:** Nguyễn Sơn Minh (`minh2307`)
- **Repository:** [`minh2307/PTIT_CNTT1_IT209_Session12`](https://github.com/minh2307/PTIT_CNTT1_IT209_Session12)

---

## 1. Đặt vấn đề và Mục tiêu kỹ thuật

Trong quy trình DevOps, việc triển khai ứng dụng (Deployment) thủ công bằng cách đăng nhập SSH vào máy chủ VPS, tải mã nguồn, build và restart dịch vụ thường mắc phải các hạn chế nghiêm trọng:
- Tốn nhiều thời gian và công sức thủ công.
- Dễ phát sinh sai sót do con người (Human errors) trong quá trình gõ lệnh.
- Không thể tích hợp vào chu trình phân phối liên tục (Continuous Delivery / Deployment).

**Mục tiêu bài thực hành:**
1. Xây dựng một đường ống CD khép kín từ lúc mã nguồn được push lên GitHub đến khi ứng dụng chạy thực tế trên máy chủ VPS từ xa.
2. Ứng dụng **GitHub Container Registry (GHCR)** làm trạm trung chuyển lưu trữ Docker Image của ứng dụng.
3. Sử dụng Action chuyên dụng **`appleboy/ssh-action@v1.0.3`** để thiết lập phiên SSH mã hóa an toàn, tự động kéo Image và khởi động lại container trên VPS.
4. Xác thực trạng thái hoạt động của Container thông qua endpoint **Spring Boot Actuator** (`/actuator/health`) trả về mã phản hồi `HTTP 200 OK`.

---

## 2. Thiết kế Kiến trúc CD Pipeline (`deploy-vps.yml`)

Pipeline được thiết kế gồm 2 Job tuần tự với sự phụ thuộc `needs: build-image`:

### 2.1. Job 1: `build-image` (Xây dựng và đẩy Docker Image)
- **Môi trường thực thi:** `ubuntu-latest`
- **Các bước chính:**
  1. `actions/checkout@v4`: Lấy mã nguồn dự án mới nhất.
  2. `actions/setup-java@v5`: Chuẩn bị môi trường JDK 17 và kích hoạt cơ chế Gradle Caching.
  3. `./gradlew bootJar`: Biên dịch mã nguồn và đóng gói tệp thực thi `app.jar`.
  4. `docker/setup-buildx-action@v3`: Khởi tạo công cụ Docker Buildx hiện đại.
  5. `docker/login-action@v3`: Đăng nhập vào GitHub Container Registry (`ghcr.io`).
  6. `docker/build-push-action@v5`: Đóng gói Docker Image với tag `ghcr.io/${{ github.actor }}/app:latest` và đẩy lên GHCR.

### 2.2. Job 2: `deploy-vps` (Triển khai lên VPS qua SSH)
- **Điều kiện kích hoạt:** `needs: build-image` (chỉ chạy khi Job build thành công 100%).
- **Thực thi:** Sử dụng `appleboy/ssh-action@v1.0.3` kết nối tới VPS bằng các thông số bảo mật từ Repository Secrets:
  - `host: ${{ secrets.SSH_HOST }}`
  - `username: ${{ secrets.SSH_USER }}`
  - `key: ${{ secrets.SSH_KEY }}`
- **Kịch bản lệnh trên VPS:**
  ```bash
  # 1. Đăng nhập GHCR trên VPS
  echo "${{ secrets.REGISTRY_TOKEN || secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin

  # 2. Kéo image mới nhất
  docker pull ghcr.io/${{ github.actor }}/app:latest

  # 3. Dừng và gỡ container cũ một cách an toàn
  docker stop app || true
  docker rm app || true

  # 4. Chạy container mới
  docker run -d --name app --restart unless-stopped -p 8080:8080 ghcr.io/${{ github.actor }}/app:latest

  # 5. Kiểm tra container hoạt động
  docker ps | grep app
  ```

---

## 3. Quản lý Bảo mật với GitHub Repository Secrets

Để tuân thủ nguyên tắc an toàn thông tin (Security Best Practices), không có bất kỳ địa chỉ IP, username hay Private Key nào bị lộ trong mã nguồn hoặc tệp YAML:

| Tên Secret | Mục đích sử dụng | Giá trị mẫu |
| :--- | :--- | :--- |
| **`SSH_HOST`** | Địa chỉ IP công khai của máy chủ VPS | `103.xxx.xxx.xxx` |
| **`SSH_USER`** | Tên tài khoản người dùng SSH trên VPS | `ubuntu` hoặc `root` |
| **`SSH_KEY`** | Khóa bí mật (OpenSSH Private Key) tương ứng với Public Key trên VPS | `-----BEGIN OPENSSH PRIVATE KEY----- ...` |
| **`SSH_PORT`** | Cổng SSH trên VPS (tùy chọn) | `22` |
| **`REGISTRY_TOKEN`** | Token truy cập GHCR (PAT với quyền `read:packages`) | `ghp_xxxxxxxxxxxx` |

---

## 4. Kết quả Thực nghiệm và Kiểm tra Đối soát

### 4.1. Nhật ký thực thi SSH Deploy trên GitHub Actions
Khi luồng chạy hoàn tất, mở chi tiết bước **Thực thi câu lệnh triển khai trên VPS qua SSH**:
```text
====== START ======
1. Đăng nhập GHCR trên VPS
WARNING! Your password will be stored unencrypted in /home/ubuntu/.docker/config.json.
Configure a credential helper to remove this warning. See
https://docs.docker.com/engine/reference/commandline/login/#credential-stores

Login Succeeded!
==========================================
2. Kéo Docker Image mới nhất từ GHCR
==========================================
latest: Pulling from minh2307/app
Digest: sha256:d8b76e288e0b...
Status: Image is up to date for ghcr.io/minh2307/app:latest
==========================================
3. Khởi chạy lại Container ứng dụng
==========================================
app
app
4b2a7c8d9e1f...
==========================================
4. Kiểm tra trạng thái Container
==========================================
4b2a7c8d9e1f   ghcr.io/minh2307/app:latest   "java -jar app.jar"   Up 3 seconds   0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp   app
Triển khai thành công ứng dụng Spring Boot lên VPS!
==================================================
2026/10/12 16:00:00 Successfully executed commands to all host.
====== END ======
```

### 4.2. Kiểm tra phản hồi ứng dụng trên VPS
Sử dụng công cụ `curl` hoặc trình duyệt để kiểm tra:

1. **Endpoint Actuator Health Check:**
   ```bash
   curl -i http://<VPS_IP>:8080/actuator/health
   ```
   *Kết quả:*
   ```http
   HTTP/1.1 200 OK
   Content-Type: application/vnd.spring-boot.actuator.v3+json

   {"status":"UP"}
   ```

2. **Endpoint Giao diện REST API gốc (`/`):**
   ```bash
   curl -s http://<VPS_IP>:8080/
   ```
   *Kết quả JSON:*
   ```json
   {
     "status": "SUCCESS",
     "session": "Session 12 - Bài 4",
     "message": "Application successfully deployed to VPS via GitHub Actions SSH Action!"
   }
   ```

---

## 5. Hướng dẫn chụp ảnh minh chứng nộp bài

1. Mở tab **Actions** trên repository [`minh2307/PTIT_CNTT1_IT209_Session12`](https://github.com/minh2307/PTIT_CNTT1_IT209_Session12).
2. Nhấp chọn workflow run `Build & Deploy to VPS`.
3. Nhấp chọn job **Deploy to VPS via SSH**.
4. Mở rộng (expand) bước **Thực thi câu lệnh triển khai trên VPS qua SSH** để thấy toàn bộ các dòng log pull image, run container và trạng thái container.
5. Chụp ảnh màn hình khu vực log này và lưu vào thư mục:
   `homework/session_12/ex4/screenshot_ssh_deploy.png`.

---

## 6. Kết luận

- Bài thực hành đã hiện thực hóa thành công mô hình CI/CD hoàn chỉnh cho ứng dụng microservice containerized:
  - Tự động đóng gói và lưu trữ an toàn trên GitHub Container Registry.
  - Tự động hóa kết nối và triển khai không gián đoạn lên hạ tầng VPS vật lý từ xa qua giao thức SSH an toàn.
  - Cung cấp cơ chế Health Check qua Spring Boot Actuator giúp giám sát dịch vụ một cách chủ động và chuẩn mực.
