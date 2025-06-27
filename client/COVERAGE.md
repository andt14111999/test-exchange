# Code Coverage Guide

Hướng dẫn này mô tả cách đo lường và theo dõi code coverage trong dự án này, bao gồm cả client-side và server-side.

## Cách Hoạt Động

Dự án này sử dụng kết hợp hai công cụ để đo lường coverage toàn diện:

1. **Client-side Coverage**: Sử dụng Playwright và Chrome DevTools Protocol để thu thập JS coverage từ browser.
2. **Server-side Coverage**: Sử dụng c8 (dựa trên V8) để đo lường coverage trong Node.js.

## Các Lệnh Đơn Giản

| Script                    | Mô tả                                                                               |
| ------------------------- | ----------------------------------------------------------------------------------- |
| `pnpm test:coverage`      | Chạy test và tạo báo cáo coverage tự động kết hợp                                   |
| `pnpm test:coverage:all`  | Chạy tất cả test e2e, tạo báo cáo và tự động mở xem                                 |
| `pnpm test:coverage:open` | Chạy test, tạo báo cáo và tự động mở xem báo cáo                                    |
| `pnpm coverage:open`      | Mở báo cáo coverage đã tạo trước đó                                                 |
| `pnpm test:coverage:file` | Chạy test cho file cụ thể (vd: `pnpm test:coverage:file tests/e2e/profile.spec.ts`) |

## Xem Coverage Cho Một Trang Cụ Thể

```bash
# Chạy test cho một file cụ thể
pnpm test:coverage:file tests/e2e/profile.spec.ts

# Mở báo cáo coverage
pnpm coverage:open
```

## Cấu Trúc Thư Mục

- `.nyc_output/`: Chứa dữ liệu thô V8 coverage
- `coverage/`: Chứa dữ liệu client-side coverage đã được chuyển đổi
- `coverage-server/`: Chứa dữ liệu server-side coverage
- `coverage-combined/`: Chứa báo cáo kết hợp cả client và server

## Đọc Báo Cáo Coverage

Khi bạn mở báo cáo HTML, bạn có thể:

1. Xem tổng quan về tất cả các file trong dự án
2. Click vào một thư mục để xem các file trong thư mục đó
3. Click vào một file để xem chi tiết dòng-theo-dòng

Báo cáo HTML sẽ hiển thị:

- Lines covered (dòng code đã được thực thi)
- Statements covered (các câu lệnh đã được thực thi)
- Branches covered (các nhánh điều kiện đã được kiểm tra)
- Functions covered (các hàm đã được gọi)

## Cải Tiến Và Xử Lý Lỗi

Hệ thống coverage đã được cải tiến để:

1. Bỏ qua các React Server Components (RSC) mà không thể thu thập coverage
2. Giảm thiểu các thông báo lỗi không cần thiết
3. Tự động kết hợp coverage từ client và server
4. Hiển thị tóm tắt rõ ràng về số lượng entries được convert thành công

## Mẹo Tăng Hiệu Quả Coverage

1. **Tăng client-side coverage**:
   - Thêm các test tương tác với UI nhiều hơn
   - Kiểm tra các trường hợp xử lý lỗi và các trường hợp đặc biệt
2. **Tăng server-side coverage**:
   - Tạo API request đến tất cả các endpoint
   - Kiểm tra các trường hợp xử lý lỗi server

## Giải Quyết Sự Cố

Nếu bạn gặp vấn đề với coverage:

- **Không có client coverage**: Kiểm tra xem browser có hỗ trợ JS coverage không (chỉ Chromium hỗ trợ)
- **Báo cáo không hiển thị file đúng**: Kiểm tra source maps đã được bật chưa
- **Coverage 0%**: Kiểm tra xem code có thực sự được thực thi trong quá trình test không
