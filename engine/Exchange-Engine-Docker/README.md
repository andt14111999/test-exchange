# Exchange Engine Docker

Hướng dẫn sử dụng Docker và RocksDB CLI cho Exchange Engine.

## Cấu trúc dự án

```
📦 Exchange-Engine-Docker
 ┣ 📜 docker-compose.yml           # Cấu hình Docker Compose chính
 ┣ 📜 docker-compose.production.yml # Cấu hình cho môi trường production
 ┣ 📜 Dockerfile                   # Docker build file
 ┣ 📜 README.md                    # Tài liệu hướng dẫn
 ┗ 📜 run.sh                       # Script hỗ trợ chạy ứng dụng

📦 src/main/java/com/exchangeengine/config
 ┣ 📜 .env                         # File cấu hình môi trường hiện tại
 ┣ 📜 .env.development            # Cấu hình môi trường development
 ┣ 📜 .env.production             # Cấu hình môi trường production
 ┗ 📜 .env.staging                # Cấu hình môi trường staging
```

## Cách sử dụng

### Chạy với Docker

1. Sử dụng script `run-dev.sh` để chạy ứng dụng:

   - Chạy trong môi trường development (mặc định):

   ```bash
   ./Exchange-Engine-Docker/run-dev.sh
   ```

   Script sẽ tự động:

   - Sao chép file cấu hình `.env.{environment}` từ thư mục `src/main/java/com/exchangeengine/config/`
   - Chạy Docker Compose với các cấu hình phù hợp

2. Nếu bạn muốn chạy Docker Compose trực tiếp:

   ```bash
   cd Exchange-Engine-Docker
   cp ../src/main/java/com/exchangeengine/config/.env.development .env
   docker compose up -d
   ```

3. Dừng ứng dụng:
   ```bash
   cd Exchange-Engine-Docker
   docker compose down
   ```

### Cài đặt và sử dụng RocksDB CLI

RocksDB CLI đã được cài đặt trong container Exchange Engine và có thể được sử dụng để tương tác với cơ sở dữ liệu RocksDB.

### Truy cập vào container

```bash
docker exec -it Exchange-engine bash
```

### Các lệnh RocksDB CLI hữu ích

```bash
# Xem help và các lệnh có sẵn
rocksdb_ldb --help

# Xem toàn bộ key-value (dùng lệnh scan)
rocksdb_ldb --db=/app/data/rocksdb/production --hex scan

# Xem key-value trong một khoảng nhất định
rocksdb_ldb --db=/app/data/rocksdb/production --hex scan --from="prefix" --to="prefiy"

# Giới hạn số lượng key trả về
rocksdb_ldb --db=/app/data/rocksdb/production --hex scan --max_keys=100

# Xem chi tiết về các file SST
rocksdb_ldb --db=/app/data/rocksdb/production dump_live_files

# Liệt kê các column family
rocksdb_ldb --db=/app/data/rocksdb/production list_column_families

# Xem dữ liệu trong một column family cụ thể
rocksdb_ldb --db=/app/data/rocksdb/production --column_family=users scan

# Đếm số lượng key (--count_only)
rocksdb_ldb --db=/app/data/rocksdb/production --column_family=users dump --count_only

# Kiểm tra tính nhất quán của database
rocksdb_ldb --db=/app/data/rocksdb/production checkconsistency

# Lấy giá trị của một key cụ thể trong column family
rocksdb_ldb --db=/app/data/rocksdb/production --column_family=accounts get <key>

# Ước tính kích thước của dữ liệu
rocksdb_ldb --db=/app/data/rocksdb/production approxsize

# Xem thống kê và metadata
rocksdb_ldb --db=/app/data/rocksdb/production dump --stats
```

## Xóa dữ liệu RocksDB trong môi trường development

### Cách 1: Xóa thư mục rocksdb_data từ host

```bash
# Dừng tất cả các container
docker compose down

# Xóa thư mục rocksdb_data từ máy host
rm -rf ../data/rocksdb/*

# Khởi động lại các container
docker compose up -d
```

### Cách 2: Xóa từ bên trong container

```bash
# Truy cập vào container
docker exec -it Exchange-engine bash

# Dừng ứng dụng (nếu cần)
kill $(pgrep -f "java -jar app.jar")

# Xóa dữ liệu
rm -rf /app/data/rocksdb/*

# Thoát container
exit

# Khởi động lại container
docker compose restart app
```

### Cách 3: Tạo container mới với volume trống

```bash
# Dừng tất cả các container
docker compose down

# Xóa volume
docker volume rm exchange-engine-docker_rocksdb_data

# Khởi động lại các container
docker compose up -d
```

## Lưu ý quan trọng

1. **KHÔNG** nên xóa dữ liệu trong môi trường production
2. Trong môi trường development, nên backup dữ liệu trước khi xóa
3. Khi xóa dữ liệu, tất cả thông tin user, account và event sẽ bị mất

## Về column family trong RocksDB

RocksDB sử dụng column family để tổ chức dữ liệu. Project Exchange Engine sử dụng các column family sau:

1. `users` - Lưu trữ thông tin người dùng
2. `accounts` - Lưu trữ thông tin tài khoản
3. `deposits` - Lưu trữ thông tin nạp tiền
4. `withdrawals` - Lưu trữ thông tin rút tiền
5. `events` - Lưu trữ event IDs (không còn được sử dụng vì events giờ chỉ lưu trong memory cache)

Không nên xóa các column family này vì chúng được tạo tự động khi khởi động ứng dụng. Nếu bạn muốn làm sạch dữ liệu, hãy sử dụng một trong các phương pháp xóa dữ liệu đã đề cập trước đó.
