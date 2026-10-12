# Bài 3: Tách biệt luồng CI/CD đa môi trường (Staging & Production) với GitHub Environments

- **Môn học:** IT209 - Tích hợp hệ thống
- **Bài thực hành:** Session 12 - Bài 3
- **Repository:** [`minh2307/PTIT_CNTT1_IT209_Session12`](https://github.com/minh2307/PTIT_CNTT1_IT209_Session12)

---

## 1. Mục tiêu bài thực hành

1. **Quản lý hạ tầng triển khai đa môi trường:** Thiết lập tách biệt hai môi trường **Staging** và **Production** bằng tính năng **GitHub Environments**.
2. **Thiết lập Environment Protection Rules:** Cấu hình quy tắc kiểm duyệt triển khai (**Required reviewers**), bắt buộc phải có sự phê duyệt thủ công từ người quản trị trước khi luồng CD được phép triển khai lên môi trường Production.
3. **Tách biệt nhánh triển khai (Branch-based CD trigger):**
   - Push vào nhánh `develop` $\rightarrow$ Tự động kích hoạt job `deploy-staging`.
   - Push vào nhánh `main` $\rightarrow$ Kích hoạt job `deploy-prod`, dừng lại chờ phê duyệt (**Waiting for review**), sau khi Approve mới tiếp tục chạy thành công.

---

## 2. Mô hình kiến trúc luồng CI/CD

```
+-------------------------------------------------------------------------------+
|                             GitHub Actions CD Pipeline                        |
+-------------------------------------------------------------------------------+
                                        |
                  +---------------------+---------------------+
                  |                                           |
         [ Push to develop ]                           [ Push to main ]
                  |                                           |
                  v                                           v
      +------------------------+                  +------------------------+
      |   Job: deploy-staging  |                  |    Job: deploy-prod    |
      +------------------------+                  +------------------------+
      | Environment: staging   |                  | Environment: production|
      | Protection: None       |                  | Protection: Reviewers  |
      +------------------------+                  +------------------------+
                  |                                           |
                  | (Chạy tự động)                            v
                  |                               +------------------------+
                  |                               |  🔴 Waiting for review |
                  |                               |   (Yêu cầu phê duyệt)  |
                  |                               +------------------------+
                  |                                           |
                  |                                   [ User clicks:       ]
                  |                                   [ Approve & Deploy   ]
                  |                                           |
                  v                                           v
      +------------------------+                  +------------------------+
      |  ✅ Staging Deployed   |                  |  ✅ Production Deployed|
      +------------------------+                  +------------------------+
```

---

## 3. Cấu hình kịch bản CD (`.github/workflows/cd-multi-env.yml`)

```yaml
name: CD Multi-Environment Pipeline

on:
  push:
    branches:
      - main
      - develop
  workflow_dispatch:

jobs:
  deploy-staging:
    name: Deploy to Staging
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'
    environment:
      name: staging
      url: https://api.staging.example.com
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Triển khai lên Staging
        run: |
          echo "=========================================="
          echo "🚀 Triển khai ứng dụng lên môi trường STAGING"
          echo "=========================================="
          echo "Branch: ${{ github.ref_name }}"
          echo "Commit SHA: ${{ github.sha }}"
          echo "Environment: staging"
          echo "Target URL: https://api.staging.example.com"
          echo "Trạng thái: Triển khai Staging thành công!"

  deploy-prod:
    name: Deploy to Production
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment:
      name: production
      url: https://api.prod.example.com
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Triển khai lên Production
        run: |
          echo "============================================="
          echo "🚀 Triển khai ứng dụng lên môi trường PRODUCTION"
          echo "============================================="
          echo "Branch: ${{ github.ref_name }}"
          echo "Commit SHA: ${{ github.sha }}"
          echo "Environment: production"
          echo "Target URL: https://api.prod.example.com"
          echo "Trạng thái: Phê duyệt hoàn tất! Triển khai Production thành công!"
```

### Giải thích các thành phần:
- `on.push.branches: [main, develop]`: Lắng nghe sự kiện đẩy mã nguồn lên cả 2 nhánh `main` và `develop`.
- `if: github.ref == 'refs/heads/develop'`: Đảm bảo job `deploy-staging` chỉ chạy khi sự kiện push xảy ra trên nhánh `develop`.
- `if: github.ref == 'refs/heads/main'`: Đảm bảo job `deploy-prod` chỉ chạy khi sự kiện push xảy ra trên nhánh `main`.
- `environment.name: staging`: Liên kết job với môi trường `staging` trên GitHub.
- `environment.name: production`: Liên kết job với môi trường `production` trên GitHub (kích hoạt Environment Protection Rules).
- `environment.url`: Cung cấp đường dẫn URL của môi trường triển khai sau khi hoàn tất.

---

## 4. Các bước cấu hình trên GitHub Repository

### Bước 1: Tạo môi trường `staging`
1. Truy cập Repository: [`minh2307/PTIT_CNTT1_IT209_Session12`](https://github.com/minh2307/PTIT_CNTT1_IT209_Session12).
2. Vào tab **Settings** $\rightarrow$ chọn **Environments** ở menu bên trái.
3. Bấm **New environment**.
4. Nhập tên: `staging` $\rightarrow$ bấm **Configure environment**.
5. Môi trường `staging` không cần bật Protection Rules (triển khai tự động).

### Bước 2: Tạo môi trường `production` và bật Required reviewers
1. Tại trang **Settings** $\rightarrow$ **Environments**, tiếp tục bấm **New environment**.
2. Nhập tên: `production` $\rightarrow$ bấm **Configure environment**.
3. Tại mục **Environment protection rules**:
   - Tích chọn ô checkbox **Required reviewers**.
   - Trong ô tìm kiếm bên dưới, nhập tài khoản của bạn (`minh2307`) và chọn tài khoản này.
4. Bấm **Save protection rules** để lưu lại cấu hình.

---

## 5. Quy trình kiểm tra và kết quả thực nghiệm

### Trường hợp 1: Kiểm thử triển khai Staging (Nhánh `develop`)
1. Tạo nhánh `develop` từ `main`:
   ```bash
   git checkout -b develop
   git push -u origin develop
   ```
2. **Kết quả:**
   - Workflow được kích hoạt.
   - Job `deploy-staging` thực thi ngay lập tức mà không yêu cầu phê duyệt.
   - Job `deploy-prod` bị bỏ qua (Skipped do điều kiện `github.ref`).

### Trường hợp 2: Kiểm thử phê duyệt Production (Nhánh `main`)
1. Chuyển về nhánh `main` và push commit mới:
   ```bash
   git checkout main
   git push origin main
   ```
2. **Trạng thái chờ phê duyệt (Waiting for review):**
   - Job `deploy-staging` bị bỏ qua (Skipped).
   - Job `deploy-prod` dừng lại ở trạng thái **Waiting for review** kèm thông báo:
     > *"1 job waiting for review: Deploy to Production"*
3. **Phê duyệt triển khai:**
   - Người duyệt (`minh2307`) bấm nút **Review deployments**.
   - Tích chọn môi trường **production** $\rightarrow$ bấm nút **Approve and deploy**.
4. **Kết quả sau khi phê duyệt:**
   - Job `deploy-prod` bắt đầu cấp phát runner và thực thi bước triển khai.
   - Luồng chạy hoàn tất với tích xanh (Green checkmark).

---

## 6. Sản phẩm nộp bài
1. Tệp kịch bản workflow: [`.github/workflows/cd-multi-env.yml`](../../.github/workflows/cd-multi-env.yml) (hoặc bản sao [`cd-multi-env.yml`](./cd-multi-env.yml)).
2. Ảnh chụp màn hình cửa sổ phê duyệt deployment môi trường Production: `screenshot_approval.png`.
3. Báo cáo chi tiết: [report.md](report.md).
