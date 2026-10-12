# PTIT CNTT1 - IT209: Session 12

[![CD Multi-Environment Pipeline](https://github.com/minh2307/PTIT_CNTT1_IT209_Session12/actions/workflows/cd-multi-env.yml/badge.svg)](https://github.com/minh2307/PTIT_CNTT1_IT209_Session12/actions/workflows/cd-multi-env.yml)

Repository bài tập môn **Tích hợp hệ thống (IT209)** - Học viện Công nghệ Bưu chính Viễn thông (PTIT).

## Danh sách bài tập
- **[Bài 3: Tách biệt luồng CI/CD đa môi trường (Staging & Production) với GitHub Environments](homework/session_12/ex3/)**
  - Quản lý hạ tầng triển khai đa môi trường thông qua tính năng GitHub Environments.
  - Cấu hình Environment Protection Rules yêu cầu phê duyệt thủ công (**Required reviewers**) trước khi deploy lên Production.
  - Tách biệt nhánh: push vào `develop` kích hoạt `deploy-staging`, push vào `main` kích hoạt `deploy-prod` (chờ phê duyệt thủ công).
  - [Báo cáo chi tiết và hướng dẫn minh chứng (report.md)](homework/session_12/ex3/report.md)
