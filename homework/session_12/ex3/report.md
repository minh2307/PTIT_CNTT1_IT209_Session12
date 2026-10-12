# Báo cáo Thực hành: Tách biệt luồng CI/CD đa môi trường với GitHub Environments

- **Môn học:** IT209 - Tích hợp hệ thống
- **Bài thực hành:** Session 12 - Bài 3
- **Sinh viên thực hiện:** Nguyễn Sơn Minh (`minh2307`)
- **Repository:** [`minh2307/PTIT_CNTT1_IT209_Session12`](https://github.com/minh2307/PTIT_CNTT1_IT209_Session12)

---

## 1. Mục tiêu và Đặt vấn đề

Trong chu trình phát triển phần mềm hiện đại (DevOps / GitOps), việc triển khai mã nguồn trực tiếp lên môi trường Production mà không qua kiểm soát nghiêm ngặt tiềm ẩn rủi ro rất lớn:
- Mã nguồn chưa được kiểm thử toàn diện trên môi trường tiền sản xuất (**Staging**).
- Không có cơ chế phê duyệt (**Manual Approval Gate**), dẫn đến nguy cơ triển khai sai phiên bản hoặc phát sinh downtime cho hệ thống thực tế.

**Mục tiêu bài thực hành:**
1. Ứng dụng tính năng **GitHub Environments** để quản lý độc lập 2 môi trường: `staging` và `production`.
2. Áp dụng quy tắc bảo vệ **Environment Protection Rules** với tính năng **Required reviewers** trên môi trường `production`.
3. Xây dựng kịch bản CD Pipeline (`.github/workflows/cd-multi-env.yml`) tự động hóa phân luồng dựa trên nhánh git:
   - Nhánh `develop` $\rightarrow$ Triển khai tự động lên `staging`.
   - Nhánh `main` $\rightarrow$ Kích hoạt `deploy-prod`, chuyển sang trạng thái chờ duyệt (**Waiting for review**), chỉ thực thi sau khi người quản trị phê duyệt.

---

## 2. Bảng so sánh cấu hình hai môi trường

| Tiêu chí | Môi trường Staging | Môi trường Production |
| :--- | :--- | :--- |
| **Tên Environment** | `staging` | `production` |
| **Nhánh kích hoạt (Branch Trigger)** | `develop` | `main` |
| **Job tương ứng** | `deploy-staging` | `deploy-prod` |
| **Protection Rules** | Không (Triển khai tự động) | **Required reviewers** (Bắt buộc phê duyệt) |
| **Người phê duyệt (Reviewer)** | Không yêu cầu | Tài khoản cá nhân (`minh2307`) |
| **Trạng thái khi kích hoạt** | Chạy ngay lập tức ($\rightarrow$ In progress $\rightarrow$ Success) | Dừng chờ duyệt (**Waiting for review**) |
| **Hành vi sau khi phê duyệt** | N/A | Bắt đầu chạy steps $\rightarrow$ Tích xanh (Success) |
| **URL môi trường** | `https://api.staging.example.com` | `https://api.prod.example.com` |

---

## 3. Phân tích Kịch bản Workflow (`cd-multi-env.yml`)

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

### Các điểm kỹ thuật then chốt:
1. **Từ khóa `environment`**:
   - Khai báo `environment.name` giúp liên kết Job của Actions với cấu hình Environment trong GitHub Repository Settings.
   - Khi Job chứa `environment: production`, GitHub Runner sẽ tạm hoãn việc khởi chạy container cho đến khi mọi điều kiện trong Protection Rules được thỏa mãn.
2. **Biểu thức điều kiện `if`**:
   - `if: github.ref == 'refs/heads/develop'` và `if: github.ref == 'refs/heads/main'` phân lập rạch ròi phạm vi thực thi. Giúp workflow chung có thể dùng chung 1 file cấu hình duy nhất mà không bị lẫn lộn giữa các nhánh.

---

## 4. Hướng dẫn các bước thiết lập và chụp ảnh minh chứng

### Bước 1: Thiết lập Environments trên GitHub Settings
1. Truy cập: [GitHub Environments Settings](https://github.com/minh2307/PTIT_CNTT1_IT209_Session12/settings/environments).
2. Tạo môi trường `staging`:
   - Bấm **New environment** $\rightarrow$ đặt tên `staging` $\rightarrow$ Bấm **Configure environment**.
3. Tạo môi trường `production`:
   - Bấm **New environment** $\rightarrow$ đặt tên `production` $\rightarrow$ Bấm **Configure environment**.
   - Tại mục **Environment protection rules**, tích chọn **Required reviewers**.
   - Tìm kiếm và chọn tài khoản cá nhân: `minh2307`.
   - Bấm **Save protection rules**.

### Bước 2: Kích hoạt triển khai và Chụp ảnh cửa sổ phê duyệt (Deliverable)
1. Đẩy mã nguồn lên nhánh `main`:
   ```bash
   git push origin main
   ```
2. Truy cập tab **Actions** $\rightarrow$ chọn workflow run vừa kích hoạt.
3. Màn hình sẽ hiển thị cảnh báo màu vàng:
   > **Deploy to Production is waiting for review**
   > *"1 job waiting for review: Deploy to Production"*
4. Nhấn nút **Review deployments**.
5. Hộp thoại modal hiển thị:
   - Checkbox lựa chọn môi trường `production`.
   - Thông tin người yêu cầu duyệt và trạng thái chờ.
   - Nút **Approve and deploy** và **Reject**.
6. **CHỤP ẢNH MÀN HÌNH TẠI ĐÂY:**
   - Chụp lại toàn bộ cửa sổ popup phê duyệt này.
   - Đặt tên tệp ảnh là `screenshot_approval.png` và lưu vào thư mục `homework/session_12/ex3/screenshot_approval.png`.
7. Nhấn **Approve and deploy**:
   - Job `deploy-prod` được cấp phát máy ảo.
   - Luồng chạy in log thành công và chuyển sang màu xanh lá cây (**Passed**).

---

## 5. Kết luận

- Việc sử dụng **GitHub Environments** kết hợp **Environment Protection Rules** mang lại giải pháp quản trị vòng đời triển khai chuyên nghiệp và an toàn tuyệt đối.
- Giúp tổ chức phân quyền kiểm duyệt minh bạch, ngăn chặn rủi ro triển khai nhầm lẫn lên môi trường thực tế (Production), đồng thời đảm bảo luồng thử nghiệm tại Staging luôn diễn ra tự động và nhanh chóng.
