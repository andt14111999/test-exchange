# Hướng dẫn sử dụng biến môi trường

## Giới thiệu

Dự án này sử dụng thư viện [dotenv-java](https://github.com/cdimascio/dotenv-java) để quản lý biến môi trường. Điều này cho phép cấu hình ứng dụng dễ dàng cho các môi trường khác nhau (development, staging, production) mà không cần thay đổi mã nguồn.

## Cấu trúc file

- `.env`: File cấu hình mặc định
- `.env.dev`: Cấu hình cho môi trường development
- `.env.staging`: Cấu hình cho môi trường staging
- `.env.prod`: Cấu hình cho môi trường production
- `.env.example`: File mẫu để tham khảo

## Các biến môi trường

### Cấu hình môi trường

- `APP_ENV`: Môi trường hiện tại (dev, staging, prod)

### Cấu hình Kafka

- `KAFKA_BOOTSTRAP_SERVERS`: Địa chỉ Kafka bootstrap servers
- `KAFKA_TOPIC_PARTITIONS`: Số lượng partitions cho Kafka topic
- `KAFKA_TOPIC_REPLICATION_FACTOR`: Hệ số replication cho Kafka topic

### Cấu hình RocksDB

- `ROCKSDB_DATA_DIR`: Đường dẫn đến thư mục dữ liệu RocksDB
- `ROCKSDB_MAX_OPEN_FILES`: Số lượng file tối đa có thể mở cùng lúc
- `ROCKSDB_WRITE_BUFFER_SIZE`: Kích thước buffer ghi (MB)
- `ROCKSDB_MAX_WRITE_BUFFER_NUMBER`: Số lượng buffer ghi tối đa
- `ROCKSDB_TARGET_FILE_SIZE_BASE`: Kích thước file đích cơ bản (MB)

### Cấu hình Disruptor

- `DISRUPTOR_BUFFER_SIZE`: Kích thước buffer của Disruptor
- `DISRUPTOR_WAIT_STRATEGY`: Chiến lược chờ của Disruptor (blocking, yielding, sleeping, busy_spin)

### Cấu hình ứng dụng

- `APP_LOG_LEVEL`: Mức độ log (DEBUG, INFO, WARN, ERROR)

## Cách sử dụng

### Chạy ứng dụng với file .env

1. Tạo file `.env` dựa trên `.env.example`
2. Chỉnh sửa các giá trị trong file `.env` theo nhu cầu
3. Chạy ứng dụng

### Chạy ứng dụng với biến môi trường hệ thống

Bạn có thể ghi đè các giá trị trong file `.env` bằng cách thiết lập biến môi trường hệ thống:

```bash
export APP_ENV=prod
export KAFKA_BOOTSTRAP_SERVERS=prod-kafka:9092
java -jar app.jar
```

### Chạy ứng dụng với Docker

Sử dụng Docker Compose để chạy ứng dụng với cấu hình môi trường cụ thể:

```bash
# Chạy môi trường development
docker-compose up balance-service-dev

# Chạy môi trường staging
docker-compose up balance-service-staging

# Chạy môi trường production
docker-compose up balance-service-prod
```

## Thứ tự ưu tiên

Thứ tự ưu tiên của các cấu hình (từ cao đến thấp):

1. Biến môi trường hệ thống (hoặc biến môi trường Docker)
2. File `.env.[environment]` tương ứng với môi trường hiện tại
3. File `.env` mặc định

## Lưu ý

- Không commit file `.env` chứa thông tin nhạy cảm vào repository
- Luôn cập nhật file `.env.example` khi thêm biến môi trường mới
- Trong môi trường production, nên sử dụng biến môi trường hệ thống hoặc biến môi trường Docker thay vì file `.env`
