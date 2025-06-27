# Exchange Engine Architecture

This directory contains architecture documentation and design specifications for the Exchange Engine project. It includes high-level architectural descriptions, diagrams, testing strategies, and other technical documentation.

## Contents

- `event-flow.md` - Overview of event flow through the system
- `event-examples.md` - Examples of various events in the system and their structures
- `sequence-diagram.md` - Sequence diagrams showing interaction between components
- `testing-strategy.md` - Comprehensive testing approach including unit, integration, and performance testing strategies

## Architecture Overview

Exchange Engine is designed with a high-performance event-driven architecture, built around the following key components:

1. **LMAX Disruptor** - High-performance inter-thread messaging system that forms the core of our event processing pipeline
2. **RocksDB** - Embedded key-value store providing persistence with high write throughput
3. **Processing Modules** - Specialized modules for handling different types of operations:
   - Account Management
   - Deposit Processing
   - Withdrawal Processing
   - Trading Engine

## Key Design Decisions

- **Event-Driven Architecture**: All operations are modeled as events flowing through a pipeline, enabling high throughput and clear separation of concerns.
- **Embedded Database**: Using RocksDB as an embedded database eliminates network overhead and provides predictable latency.
- **Lockless Concurrency**: Utilizing the Disruptor pattern for inter-thread communication avoids locks and contention.
- **Fast Recovery Path**: In case of system restart, state can be quickly reconstructed from the persisted data.

## Implementation Constraints

- Java 17 or higher required
- Designed to be deployed in containerized environments (Docker)
- Optimized for high-throughput, low-latency operations
- Uses direct memory access for reduced garbage collection pressure

## Available Diagrams

- Event Flow: Visualizes how events flow through the system
- Sequence Diagrams: Illustrates the interaction between components for key operations

For more detailed information, please refer to the specific documentation files within this directory.

## Sơ đồ có sẵn

1. **[Sơ đồ Luồng Sự kiện (Event Flow)](event-flow.md)** - Mô tả tổng quan luồng xử lý sự kiện từ Backend đến Exchange Engine và ngược lại.

2. **[Sơ đồ Sequence](sequence-diagram.md)** - Mô tả chi tiết tuần tự các bước xử lý sự kiện.

3. **[Chiến lược Testing](testing-strategy.md)** - Mô tả chi tiết chiến lược testing và cách triển khai tests cho Exchange Engine.

## Chi tiết kiến trúc

### Single Thread Processing (LMAX Disruptor)

LMAX Disruptor là trái tim của hệ thống Exchange Engine, được chọn vì lý do sau:

- **Xử lý tuần tự**: Tất cả các sự kiện được xử lý tuần tự bởi một thread duy nhất, đảm bảo tính nhất quán và tránh race condition
- **Hiệu suất cao**: Giảm thiểu context switching và lock contention
- **RingBuffer**: Sử dụng cấu trúc dữ liệu hiệu quả để quản lý các sự kiện trong bộ nhớ

Các thành phần xử lý single-thread:

- `DisruptorEventHandler`: Handler chính xử lý các sự kiện từ RingBuffer
- `CoinAccountProcessor`: Xử lý tạo tài khoản và truy vấn số dư
- `DepositProcessor`: Xử lý nạp tiền
- `WithdrawalProcessor`: Xử lý rút tiền

### Multi-Thread Processing

Exchange Engine sử dụng các thread pools riêng biệt để xử lý các hoạt động I/O song song với xử lý logic nghiệp vụ:

1. **Kafka Executor (3 Threads)**:

   - Xử lý gửi các sự kiện response đến Kafka
   - Không phụ thuộc vào xử lý chính, đảm bảo không bị blocking
   - Số lượng thread có thể được cấu hình qua biến môi trường

2. **Storage Executor (2 Threads)**:

   - Xử lý lưu trữ dữ liệu vào RocksDB
   - Ghi dữ liệu theo batch để tối ưu hiệu suất
   - Flushing policy có thể được cấu hình

3. **KafkaConsumerService (Multi-Thread)**:
   - Xử lý các sự kiện đầu vào từ Kafka
   - Validate dữ liệu trước khi đưa vào Disruptor

### RocksDB Storage

RocksDB được chọn làm storage engine chính vì những lý do sau:

- **Hiệu suất cao**: Tối ưu cho workload ghi nhiều (write-heavy)
- **Độ bền**: Sử dụng WAL (Write-Ahead Log) và snapshot để đảm bảo dữ liệu không bị mất
- **Compaction**: Tự động nén và tối ưu hóa dữ liệu trong background
- **Column Families**: Phân tách dữ liệu thành các nhóm logic riêng biệt

Column Families trong RocksDB:

- `accounts`: Lưu trữ thông tin tài khoản
- `deposits`: Lưu trữ thông tin nạp tiền
- `withdrawals`: Lưu trữ thông tin rút tiền
- `account_histories`: Lưu trữ lịch sử tài khoản

### So sánh RocksDB và File-based Storage

| Tiêu chí                             | RocksDB                                        | GHI File                                                    |
| ------------------------------------ | ---------------------------------------------- | ----------------------------------------------------------- |
| Tốc độ ghi/đọc                       | 🚀 Cực nhanh (vài ms)                          | ⏱️ Chậm (giây hoặc phút)                                    |
| Tìm kiếm (lookup)                    | 🔍 O(1) (có indexing)                          | ⏳ O(n) (phải đọc toàn bộ file)                             |
| Dung lượng lưu trữ                   | 📊 Được nén, tiết kiệm ổ cứng                  | 📁 File lớn hơn                                             |
| Độ bền dữ liệu (durability)          | ✅ Có WAL & snapshot                           | ⚠️ Có thể mất dữ liệu khi crash                             |
| Checkpoint (Lưu trạng thái hệ thống) | ✅ Hỗ trợ checkpoint, có thể khôi phục dễ dàng | ❌ Không có checkpoint, nếu crash phải đọc lại toàn bộ file |
| Dễ đọc bằng tay                      | ❌ Không (dữ liệu nhị phân)                    | ✅ Có thể đọc trực tiếp                                     |
| Khả năng mở rộng                     | 📈 Hỗ trợ hàng tỷ records                      | 📉 Khó mở rộng khi file quá lớn                             |

### Mô hình luồng xử lý tổng thể

1. **Input (Kafka → Consumer)**:

   - Kafka Consumer nhận sự kiện từ Backend
   - Sự kiện được parse và validate
   - Multi-thread để tối ưu throughput

2. **Processing (Disruptor)**:

   - Single-thread xử lý logic nghiệp vụ
   - Các sự kiện được xử lý tuần tự theo thứ tự vào RingBuffer
   - Đảm bảo tính consistency

3. **Output (Executors → Kafka/RocksDB)**:
   - Multi-thread xử lý I/O
   - Kafka Executor gửi response về Backend
   - Storage Executor lưu dữ liệu vào RocksDB

## Ưu điểm của kiến trúc

1. **Hiệu suất cao**: LMAX Disruptor tối ưu hóa throughput bằng cách giảm thiểu locks và context switching
2. **Độ tin cậy**: Xử lý tuần tự đảm bảo tính nhất quán của dữ liệu
3. **Khả năng mở rộng**: Tách biệt xử lý logic và I/O cho phép scale riêng các thành phần
4. **Bảo trì dễ dàng**: Kiến trúc rõ ràng, mỗi thành phần có trách nhiệm riêng biệt

## Công nghệ sử dụng

- **LMAX Disruptor**: Thư viện xử lý sự kiện hiệu suất cao
- **Kafka**: Message broker để giao tiếp giữa các dịch vụ
- **RocksDB**: Storage engine dạng key-value có hiệu suất cao
- **Java Concurrency**: Thread pools và concurrent collections
