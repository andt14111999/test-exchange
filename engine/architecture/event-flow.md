```mermaid
graph TD
    BE["Backend Rails Service"] --"1.Gửi event"--> Kafka["Kafka Message Broker"]

    Kafka --"2.Consume event"--> KConsumer["KafkaConsumerService<br>(Multi Thread)"]

    KConsumer --"3.Parse & Validate"--> Handler["EngineHandler"]

    Handler --"4.Publish event"--> Disruptor["LMAX Disruptor<br><b>Single Thread</b>"]

    Disruptor --"5.Xử lý"--> Processor["Business Logic<br>(CoinAccount/Deposit/Withdrawal)"]

    Processor --"6.Kết quả"--> Output["OutputProcessor"]

    Output --"7a.Kafka task"--> KafkaOut["Kafka Executor<br>(3 Threads)"]
    Output --"7b.Storage task"--> StorageOut["Storage Executor<br>(2 Threads)"]

    KafkaOut --"8.Send response"--> Kafka
    StorageOut --"9.Save data"--> RocksDB["RocksDB Storage"]

    Kafka --"10.Response event"--> BE

    style Disruptor fill:#f66,stroke:#333,stroke-width:2px
    style KConsumer fill:#99f,stroke:#333,stroke-width:2px
    style KafkaOut fill:#99f,stroke:#333,stroke-width:2px
    style StorageOut fill:#99f,stroke:#333,stroke-width:2px
    style RocksDB fill:#6b6,stroke:#333,stroke-width:2px
```

# Sơ đồ Luồng Xử lý Sự kiện trong Exchange Engine

Sơ đồ trên mô tả luồng xử lý sự kiện từ khi Backend gửi event đến Exchange Engine, qua quá trình xử lý bằng LMAX Disruptor, và trả kết quả về Backend.

## Đặc điểm chính

- **Single Thread Processing**: LMAX Disruptor xử lý tất cả các sự kiện bằng một thread duy nhất (màu đỏ)
- **Multi-Thread I/O**: Các hoạt động I/O được xử lý bởi nhiều thread riêng biệt (màu xanh)
- **Storage**: Dữ liệu được lưu trữ vào RocksDB (màu xanh lá)

## Màu sắc

- **Đỏ**: Xử lý single-thread
- **Xanh dương**: Xử lý multi-thread
- **Xanh lá**: Lưu trữ dữ liệu
