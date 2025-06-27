# Exchange Engine

![Java CI with Maven](https://github.com/snowfox-foundation/exchange-engine/actions/workflows/ci.yml/badge.svg)
![Test Coverage](https://img.shields.io/badge/coverage-96%25-green)

## Mục lục

- [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
- [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
- [Cài đặt](#cài-đặt)
  - [Cài đặt Docker](#cài-đặt-docker)
  - [Cài đặt Java](#cài-đặt-java)
- [Biến môi trường](#biến-môi-trường)
- [Chạy ứng dụng với Docker](#chạy-ứng-dụng-với-docker)
  - [Môi trường Development](#môi-trường-development)
  - [Môi trường Production](#môi-trường-production)
- [Testing](#testing)
  - [Báo cáo độ phủ code (JaCoCo)](#báo-cáo-độ-phủ-code-jacoco)
- [CI/CD](#cicd)
- [Kafka Channels và Event Model](#kafka-channels-và-event-model)

## Kiến trúc hệ thống

Exchange Engine sử dụng mô hình LMAX Disruptor để xử lý các sự kiện tài chính với hiệu suất cao và độ tin cậy cao.

- **[Chi tiết kiến trúc hệ thống và sơ đồ luồng xử lý](/architecture/README.md)**
- **[Sơ đồ luồng sự kiện](/architecture/event-flow.md)**
- **[Sơ đồ sequence](/architecture/sequence-diagram.md)**
- **[Ví dụ chi tiết các loại sự kiện](/architecture/event-examples.md)**
- **[Chiến lược testing](/architecture/testing-strategy.md)**
- **[Hướng dẫn Kafka Consumer và Khôi phục dữ liệu](/architecture/kafka-consumer-guide.md)**

**Đặc điểm chính:**

- **Single-Thread Processing**: Logic nghiệp vụ được xử lý tuần tự bởi một thread duy nhất (LMAX Disruptor)
- **Multi-Thread I/O**: Các hoạt động I/O (Kafka, RocksDB) được xử lý song song bởi các thread pools riêng biệt
- **RocksDB Storage**: Sử dụng RocksDB làm storage engine với hiệu suất cao, hỗ trợ WAL và snapshot

## Yêu cầu hệ thống

- Docker Desktop
- Java 23 (Temurin)

## Cài đặt

### Cài đặt Docker

1. Tải và cài đặt Docker Desktop từ [trang chủ Docker](https://www.docker.com/products/docker-desktop)
2. Khởi động Docker Desktop
3. Kiểm tra cài đặt:

```bash
docker --version
docker compose version
```

### Cài đặt Java

```bash
# Cài đặt Java bằng asdf
asdf plugin add java
asdf install java temurin-23.0.2
asdf global java temurin-23.0.2

# Kiểm tra phiên bản Java
java -version
# Kết quả mong đợi:
# openjdk version "23.0.2" 2025-01-21
# OpenJDK Runtime Environment Temurin-23.0.2+7 (build 23.0.2+7)
# OpenJDK 64-Bit Server VM Temurin-23.0.2+7 (build 23.0.2+7, mixed mode, sharing)
```

## Chạy ứng dụng với Docker

Chi tiết về cách sử dụng Docker và RocksDB CLI có thể xem tại [Docker README](/Exchange-Engine-Docker/README.md).

### Môi trường Development

1. Clone repository:

```bash
git clone <repository-url>
cd exchange-engine
```

2. Build và chạy các container:

```bash
cd Exchange-Engine-Docker
docker compose up -d
```

3. Kiểm tra các service:

- Kafka UI: http://127.0.0.1:8085

4. Xem logs:

```bash
# Xem logs của tất cả các service
docker compose logs -f

# Xem logs của service cụ thể
docker compose logs -f app    # Exchange Engine logs
docker compose logs -f kafka  # Kafka logs
```

5. Dừng các service:

```bash
docker compose down
```

### Môi trường Production

1. Tạo file `.env` cho production:

```bash
KAFKA_UI_USERNAME=your_secure_username
KAFKA_UI_PASSWORD=your_secure_password
```

2. Chạy với docker-compose production:

```bash
docker compose -f docker-compose.prod.yml up -d
```

3. Các port trong production:

- Kafka: Chỉ accessible trong internal network
- Kafka UI: Cần cấu hình qua reverse proxy với authentication
- Exchange Engine: Expose qua API Gateway

## Testing

Exchange Engine sử dụng các phương pháp và thư viện testing để đảm bảo chất lượng code và độ tin cậy của hệ thống. Chi tiết về chiến lược testing có thể xem tại [Chiến lược testing](/architecture/testing-strategy.md).

### Thư viện Testing

- **JUnit 5**: Framework testing chính cho Java
- **Mockito**: Framework để tạo các mock objects
- **AssertJ**: Thư viện assertions với cú pháp fluent API
- **JaCoCo**: Công cụ đo lường độ phủ code

### Chạy Tests

```bash
# Chạy tất cả tests
mvn test -Dnet.bytebuddy.experimental=true

# Chạy một test cụ thể
mvn test -Dtest=AccountTest -Dnet.bytebuddy.experimental=true

# Chạy tests cho một package cụ thể
mvn test -Dtest="com.exchangeengine.model.*" -Dnet.bytebuddy.experimental=true

# Lưu ý: Flag -Dnet.bytebuddy.experimental=true cần thiết khi chạy trên Java 23
# để đảm bảo tương thích với Mockito/ByteBuddy
```

### Báo cáo độ phủ code (JaCoCo)

Dự án sử dụng JaCoCo để đo lường độ phủ code của các bài kiểm thử. Để tạo báo cáo độ phủ code:

```bash
# Chạy tests và tạo báo cáo JaCoCo
mvn clean test jacoco:report

# Báo cáo sẽ được tạo tại
# target/site/jacoco/index.html
```

Hiện tại, dự án có độ phủ code là 77% các câu lệnh và 60% các nhánh điều kiện.

## CI/CD

Dự án sử dụng GitHub Actions để tự động hóa quá trình CI/CD. Mỗi khi có push hoặc pull request vào nhánh main/master, các bước sau sẽ được thực hiện:

1. Build project
2. Chạy tất cả các bài kiểm thử
3. Tạo báo cáo độ phủ code với JaCoCo
4. Kiểm tra độ phủ code tối thiểu (70% tổng thể, 80% cho các file đã thay đổi)
5. Tạo báo cáo kết quả kiểm thử

File cấu hình CI/CD được lưu tại [.github/workflows/ci.yml](/.github/workflows/ci.yml).

## Kafka Channels và Event Model

Exchange Engine sử dụng các kênh Kafka sau để giao tiếp:

### Input Topics (Từ Client đến Exchange Engine)

- `EE.I.coin_account`: Tạo tài khoản coin mới
- `EE.I.coin_deposit`: Xử lý nạp tiền
- `EE.I.coin_withdraw`: Xử lý rút tiền
- `EE.I.coin_account_query`: Truy vấn thông tin tài khoản
- `EE.I.coin_account_reset`: Reset tài khoản (chỉ dùng cho môi trường phát triển)

### Output Topics (Từ Exchange Engine đến Client)

- `EE.O.coin_account_update`: Cập nhật thông tin tài khoản (phản hồi cho các truy vấn balance và cập nhật balance)
- `EE.O.transaction_response`: Phản hồi kết quả giao dịch (phản hồi cho các giao dịch deposit, withdraw)

Để biết chi tiết về cách sử dụng các loại sự kiện cùng với các ví dụ JSON, xem [Tài liệu và ví dụ các loại sự kiện](/architecture/event-examples.md)
