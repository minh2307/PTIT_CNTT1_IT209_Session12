# Bài 4: Tự động triển khai ứng dụng Spring Boot (Container) lên VPS từ xa qua SSH

- **Môn học:** IT209 - Tích hợp hệ thống
- **Bài thực hành:** Session 12 - Bài 4
- **Repository:** [`minh2307/PTIT_CNTT1_IT209_Session12`](https://github.com/minh2307/PTIT_CNTT1_IT209_Session12)

---

## 1. Mục tiêu bài thực hành

1. **Tự động hóa toàn trình (Full CI/CD Automation):** Xây dựng quy trình tự động đóng gói ứng dụng Spring Boot thành Docker Image, đẩy lên **GitHub Container Registry (GHCR)**, và kích hoạt triển khai lên máy chủ VPS từ xa qua giao thức **SSH**.
2. **Bảo mật thông tin xác thực:** Quản lý địa chỉ máy chủ, thông tin tài khoản và SSH Private Key thông qua **GitHub Repository Secrets** (`SSH_HOST`, `SSH_USER`, `SSH_KEY`).
3. **Triển khai ứng dụng dạng Container trên VPS:**
   - Sử dụng action `appleboy/ssh-action@v1.0.3` để tự động kết nối và chạy các lệnh shell trên VPS.
   - Kéo (pull) bản build mới nhất của Docker image từ GHCR.
   - Dừng và gỡ bỏ container cũ một cách an toàn (`docker stop || true && docker rm || true`).
   - Khởi chạy container mới ánh xạ cổng `8080:8080` với chính sách `--restart unless-stopped`.
4. **Kiểm tra và nghiệm thu:** Kiểm tra trạng thái sức khỏe của ứng dụng thông qua endpoint Spring Boot Actuator (`/actuator/health`) đạt mã phản hồi `HTTP 200 OK`.

---

## 2. Mô hình kiến trúc quy trình triển khai

```
+-----------------------------------------------------------------------------------------+
|                                    GitHub Repository                                    |
|   (Mã nguồn Spring Boot + Dockerfile + .github/workflows/deploy-vps.yml)                |
+-----------------------------------------------------------------------------------------+
                                             | (Push to main)
                                             v
+-----------------------------------------------------------------------------------------+
|                                 GitHub Actions Pipeline                                 |
|                                                                                         |
|   +------------------------------------+       +------------------------------------+   |
|   |         Job: build-image           |       |          Job: deploy-vps           |   |
|   |------------------------------------|       |------------------------------------|   |
|   | 1. Checkout mã nguồn               |       | 1. needs: build-image              |   |
|   | 2. Thiết lập Java JDK 17 & Gradle  |       | 2. Kết nối VPS từ xa qua SSH       |   |
|   | 3. Biên dịch: ./gradlew bootJar    | ====> |    (appleboy/ssh-action@v1.0.3)    |   |
|   | 4. Setup Docker Buildx             |       | 3. Chạy script triển khai trên VPS |   |
|   | 5. Đăng nhập GHCR (ghcr.io)        |       |    - docker login ghcr.io          |   |
|   | 6. Build & Push Docker Image       |       |    - docker pull image             |   |
|   +------------------------------------+       |    - docker stop & rm cũ           |   |
|                     |                          |    - docker run container mới      |   |
+---------------------|--------------------------+------------------------------------+---+
                      | (Đẩy Image)                                  | (Thực thi lệnh SSH)
                      v                                              v
      +-------------------------------+              +-------------------------------+
      | GitHub Container Registry     |              |       Máy chủ VPS từ xa       |
      | ghcr.io/<actor>/app:latest    | <==========  |  Docker Container: app (8080) |
      +-------------------------------+  (Kéo Image) +-------------------------------+
                                                                     |
                                                                     v
                                                            [ Client / Browser ]
                                                      http://<VPS_IP>:8080/actuator/health
                                                              (HTTP 200 OK)
```

---

## 3. Cấu hình kịch bản Workflow (`.github/workflows/deploy-vps.yml`)

```yaml
name: Build & Deploy to VPS

on:
  push:
    branches:
      - main
  workflow_dispatch:

permissions:
  contents: read
  packages: write

jobs:
  build-image:
    name: Build & Push Docker Image
    runs-on: ubuntu-latest
    steps:
      - name: Checkout mã nguồn
        uses: actions/checkout@v4

      - name: Thiết lập môi trường Java JDK 17
        uses: actions/setup-java@v5
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Cấp quyền thực thi cho gradlew
        run: chmod +x gradlew

      - name: Đóng gói ứng dụng Spring Boot (bootJar)
        run: ./gradlew bootJar --no-daemon

      - name: Thiết lập Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Đăng nhập vào GitHub Container Registry (GHCR)
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.REGISTRY_TOKEN || secrets.GITHUB_TOKEN }}

      - name: Xây dựng và Đẩy Docker Image lên GHCR
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ghcr.io/${{ github.actor }}/app:latest

  deploy-vps:
    name: Deploy to VPS via SSH
    needs: build-image
    runs-on: ubuntu-latest
    steps:
      - name: Thực thi câu lệnh triển khai trên VPS qua SSH
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.SSH_HOST }}
          username: ${{ secrets.SSH_USER }}
          key: ${{ secrets.SSH_KEY }}
          port: ${{ secrets.SSH_PORT || 22 }}
          script: |
            echo "=========================================="
            echo "1. Đăng nhập GHCR trên VPS"
            echo "=========================================="
            echo "${{ secrets.REGISTRY_TOKEN || secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin

            echo "=========================================="
            echo "2. Kéo Docker Image mới nhất từ GHCR"
            echo "=========================================="
            docker pull ghcr.io/${{ github.actor }}/app:latest

            echo "=========================================="
            echo "3. Khởi chạy lại Container ứng dụng"
            echo "=========================================="
            docker stop app || true
            docker rm app || true
            docker run -d --name app --restart unless-stopped -p 8080:8080 ghcr.io/${{ github.actor }}/app:latest

            echo "=========================================="
            echo "4. Kiểm tra trạng thái Container"
            echo "=========================================="
            docker ps | grep app
            echo "Triển khai thành công ứng dụng Spring Boot lên VPS!"
```

---

## 4. Các bước chuẩn bị và cấu hình

### Bước 1: Khai báo Repository Secrets trên GitHub
1. Mở repository: [`minh2307/PTIT_CNTT1_IT209_Session12`](https://github.com/minh2307/PTIT_CNTT1_IT209_Session12).
2. Vào **Settings** $\rightarrow$ **Secrets and variables** $\rightarrow$ **Actions**.
3. Bấm **New repository secret** và lần lượt thêm các biến sau:
   - **`SSH_HOST`**: Địa chỉ IP công khai hoặc tên miền của VPS (ví dụ: `103.x.x.x`).
   - **`SSH_USER`**: Tên người dùng SSH trên VPS (ví dụ: `ubuntu` hoặc `root`).
   - **`SSH_KEY`**: Toàn bộ nội dung tệp SSH Private Key kết nối vào VPS (bao gồm cả dòng header `-----BEGIN OPENSSH PRIVATE KEY-----` và footer `-----END OPENSSH PRIVATE KEY-----`).
   - **`SSH_PORT`** *(tùy chọn)*: Cổng SSH (mặc định là `22`).
   - **`REGISTRY_TOKEN`** *(tùy chọn)*: Personal Access Token (PAT) có quyền `read:packages` (đã tạo ở Session 11) nếu Package GHCR ở chế độ riêng tư.

### Bước 2: Chuẩn bị môi trường trên máy chủ VPS
Đảm bảo VPS đã cài đặt Docker và user có quyền chạy Docker không cần `sudo`:
```bash
# Cài đặt Docker (nếu chưa có)
sudo apt update && sudo apt install -y docker.io

# Thêm user hiện tại vào group docker
sudo usermod -aG docker $USER

# Khởi động Docker service
sudo systemctl enable --now docker

# Mở cổng 8080 trên tường lửa VPS
sudo ufw allow 8080/tcp
```

---

## 5. Kiểm tra kết quả và Bằng chứng thực thi

### 1. Kiểm tra log SSH Deploy trên GitHub Actions
1. Vào tab **Actions** $\rightarrow$ chọn luồng chạy `Build & Deploy to VPS`.
2. Chọn job **Deploy to VPS via SSH**.
3. Mở bước **Thực thi câu lệnh triển khai trên VPS qua SSH**, log sẽ hiển thị:
   ```text
   ====== START ======
   1. Đăng nhập GHCR trên VPS
   Login Succeeded!
   2. Kéo Docker Image mới nhất từ GHCR
   latest: Pulling from minh2307/app
   Digest: sha256:...
   Status: Image is up to date for ghcr.io/minh2307/app:latest
   3. Khởi chạy lại Container ứng dụng
   app
   <CONTAINER_ID>
   4. Kiểm tra trạng thái Container
   <CONTAINER_ID>   ghcr.io/minh2307/app:latest   Up 2 seconds   0.0.0.0:8080->8080/tcp   app
   Triển khai thành công ứng dụng Spring Boot lên VPS!
   ====== END ======
   ```

### 2. Kiểm tra trực tiếp trên trình duyệt / curl
1. Kiểm tra endpoint Health Check của Spring Boot Actuator:
   ```bash
   curl -i http://<VPS_IP>:8080/actuator/health
   ```
   **Kết quả phản hồi:**
   ```http
   HTTP/1.1 200 OK
   Content-Type: application/vnd.spring-boot.actuator.v3+json

   {"status":"UP"}
   ```
2. Kiểm tra giao diện API mặc định:
   ```bash
   curl -i http://<VPS_IP>:8080/
   ```
   **Kết quả phản hồi:**
   ```json
   {
     "status": "SUCCESS",
     "session": "Session 12 - Bài 4",
     "message": "Application successfully deployed to VPS via GitHub Actions SSH Action!"
   }
   ```

---

## 6. Sản phẩm nộp bài
1. Tệp kịch bản workflow: [`.github/workflows/deploy-vps.yml`](../../.github/workflows/deploy-vps.yml) (và bản sao [`deploy-vps.yml`](./deploy-vps.yml)).
2. Ảnh chụp màn hình log kết quả SSH Deploy: `screenshot_ssh_deploy.png`.
3. Báo cáo chi tiết: [report.md](report.md).
