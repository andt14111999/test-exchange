# Kafka Consumer Configuration và Data Recovery

## Cấu hình Offset Reset

### Các giá trị của AUTO_OFFSET_RESET_CONFIG

1. **earliest**

   - Đọc từ offset đầu tiên có sẵn của topic
   - Áp dụng khi không tìm thấy offset đã commit
   - Hữu ích khi cần đọc lại toàn bộ lịch sử

2. **latest**

   - Chỉ đọc các message mới nhất
   - Áp dụng khi không tìm thấy offset đã commit
   - Mặc định trong hệ thống

3. **none**
   - Ném ra exception nếu không tìm thấy offset đã commit
   - Sử dụng để đảm bảo không bỏ sót message

### Khi nào áp dụng?

- Chỉ áp dụng khi:
  - Consumer group mới được tạo
  - Không tìm thấy offset đã commit
  - Offset đã commit bị xóa

## Consumer Group Behavior

### Cách hoạt động

1. **Group ID cũ**

   - Đọc từ offset đã commit cuối cùng
   - Không bị ảnh hưởng bởi AUTO_OFFSET_RESET_CONFIG
   - Đảm bảo không miss message

2. **Group ID mới**
   - Áp dụng AUTO_OFFSET_RESET_CONFIG
   - Bắt đầu đọc từ vị trí được cấu hình
   - Có thể bỏ sót message cũ nếu dùng latest

### Xử lý message mới

- Consumer luôn đọc message mới
- Commit sau mỗi lần xử lý thành công
- Đảm bảo không bỏ sót message trong quá trình xử lý

## Khôi phục dữ liệu từ Kafka

### Khi nào cần khôi phục?

1. **Mất dữ liệu**

   - Dữ liệu bị xóa hoặc corrupt
   - Cần đọc lại toàn bộ lịch sử
   - Cần xử lý lại các message

2. **Deploy mới**
   - Cần đọc lại dữ liệu từ đầu
   - Cần đảm bảo xử lý tuần tự
   - Tránh xử lý trùng lặp

### Các bước thực hiện

1. **Tạo Group ID mới**

   ```bash
   # Thay đổi group ID trong biến môi trường
   KAFKA_CONSUMER_GROUP=new-recovery-group
   ```

2. **Cấu hình đọc từ đầu**

   ```bash
   # Set auto.offset.reset thành earliest
   KAFKA_AUTO_OFFSET_RESET=earliest
   ```

3. **Deploy ứng dụng**
   - Deploy với group ID mới
   - Đảm bảo xử lý tuần tự
   - Kiểm tra logs để theo dõi tiến trình

### Lưu ý quan trọng

1. **Trước khi khôi phục**

   - Backup dữ liệu hiện tại
   - Kiểm tra retention period của topic
   - Đảm bảo đủ dung lượng lưu trữ

2. **Trong quá trình khôi phục**

   - Theo dõi logs
   - Kiểm tra xử lý message
   - Đảm bảo không có lỗi

3. **Sau khi khôi phục**
   - Verify dữ liệu
   - Kiểm tra tính nhất quán
   - Có thể xóa group ID tạm thời

### Best Practices

1. **Quản lý Group ID**

   - Đặt tên có ý nghĩa
   - Ghi chú mục đích sử dụng
   - Tránh xung đột tên

2. **Monitoring**

   - Theo dõi lag
   - Kiểm tra xử lý message
   - Alert khi có vấn đề

3. **Testing**
   - Test quy trình khôi phục
   - Verify dữ liệu
   - Đảm bảo không ảnh hưởng hệ thống

## Thông tin bổ sung

### Offset là gì?

Offset trong Kafka là một số thứ tự duy nhất được gán cho mỗi message trong một partition của topic. Nó hoạt động như một con trỏ, cho biết consumer đã đọc đến message nào.

Ví dụ:

```
Partition 0: [Message 0, Offset 0] -> [Message 1, Offset 1] -> [Message 2, Offset 2]
Partition 1: [Message 0, Offset 0] -> [Message 1, Offset 1] -> [Message 2, Offset 2]
```

- Mỗi partition có offset riêng
- Offset tăng dần theo thứ tự
- Consumer lưu trữ offset đã đọc để biết vị trí tiếp theo

### Các cấu hình quan trọng trong KafkaConsumerConfig

1. **Cấu hình cơ bản**

   ```java
   props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
   props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
   props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
   props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
   ```

   - `BOOTSTRAP_SERVERS_CONFIG`: Địa chỉ Kafka brokers
   - `GROUP_ID_CONFIG`: Tên nhóm consumer
   - `KEY_DESERIALIZER_CLASS_CONFIG`: Cách giải mã key
   - `VALUE_DESERIALIZER_CLASS_CONFIG`: Cách giải mã value

2. **Cấu hình commit**

   ```java
   props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(autoCommit));
   props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
   ```

   - `ENABLE_AUTO_COMMIT_CONFIG`: Tự động commit offset
   - `AUTO_COMMIT_INTERVAL_MS_CONFIG`: Thời gian giữa các lần commit

3. **Cấu hình fetch**

   ```java
   props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "1024");
   props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, "500");
   ```

   - `FETCH_MIN_BYTES_CONFIG`: Số byte tối thiểu cần fetch
   - `FETCH_MAX_WAIT_MS_CONFIG`: Thời gian tối đa đợi đủ dữ liệu

4. **Cấu hình heartbeat**
   ```java
   props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "3000");
   props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "10000");
   props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000");
   ```
   - `HEARTBEAT_INTERVAL_MS_CONFIG`: Tần suất gửi heartbeat
   - `SESSION_TIMEOUT_MS_CONFIG`: Thời gian chờ heartbeat
   - `MAX_POLL_INTERVAL_MS_CONFIG`: Thời gian tối đa giữa các lần poll

### Lưu ý khi sử dụng

1. **Auto Commit**

   - Nên tắt (`autoCommit = false`) khi cần đảm bảo xử lý message
   - Commit thủ công sau khi xử lý thành công
   - Tránh mất message khi xử lý thất bại

2. **Fetch Settings**

   - `FETCH_MIN_BYTES`: Tăng để giảm tải broker
   - `FETCH_MAX_WAIT_MS`: Giảm để phản hồi nhanh hơn
   - Cân bằng giữa latency và throughput

3. **Heartbeat Settings**
   - `HEARTBEAT_INTERVAL_MS` < `SESSION_TIMEOUT_MS`
   - `MAX_POLL_INTERVAL_MS` > `SESSION_TIMEOUT_MS`
   - Điều chỉnh theo thời gian xử lý message

## Cấu hình hiện tại

### Các setting mặc định

```java
// Cấu hình cơ bản
props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

// Cấu hình offset reset - Luôn đọc từ đầu khi không có offset
props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

// Tắt auto commit để đảm bảo xử lý message
props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

// Cấu hình fetch
props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "1024");  // 1KB
props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, "500"); // 500ms

// Cấu hình heartbeat
props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "3000");    // 3s
props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "10000");      // 10s
props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000");   // 5 phút
```

### Giải thích các setting

1. **Offset Reset Strategy**

   - Mặc định: `earliest`
   - Lý do: Đảm bảo không bỏ sót message khi tạo group mới
   - Không ảnh hưởng đến group đã tồn tại

2. **Auto Commit**

   - Tắt auto commit (`false`)
   - Commit thủ công sau mỗi lần xử lý thành công
   - Đảm bảo message được xử lý đúng

3. **Fetch Settings**

   - `FETCH_MIN_BYTES`: 1KB - Giảm tải broker
   - `FETCH_MAX_WAIT_MS`: 500ms - Cân bằng latency

4. **Heartbeat Settings**
   - `HEARTBEAT_INTERVAL_MS`: 3s - Tần suất gửi heartbeat
   - `SESSION_TIMEOUT_MS`: 10s - Thời gian chờ heartbeat
   - `MAX_POLL_INTERVAL_MS`: 5 phút - Thời gian tối đa giữa các lần poll

## Luồng xử lý message

### Sơ đồ luồng xử lý

```
Producer -> Kafka Topic -> Consumer Group -> Exchange Engine
   |            |              |                    |
   |            |              |                    |
   v            v              v                    v
Message     Partition      Consumer            Processing
   |            |              |                    |
   |            |              |                    |
   +------------+--------------+--------------------+
                 |
                 v
              Commit
```

### Quy trình xử lý

1. **Khi message mới đến**

   ```
   Producer -> Topic
   [Message 1] -> [Partition 0, Offset 0]
   [Message 2] -> [Partition 0, Offset 1]
   [Message 3] -> [Partition 0, Offset 2]
   ```

2. **Consumer xử lý**

   ```
   Consumer Group
   ├── Poll messages
   ├── Process message
   └── Commit offset
   ```

3. **Commit offset**
   ```
   After successful processing:
   [Message 1] -> Commit Offset 0
   [Message 2] -> Commit Offset 1
   [Message 3] -> Commit Offset 2
   ```

## Xử lý khi EE dừng/khởi động lại

### Khi EE dừng

```
Time 1: [Message 1,2,3] -> Processed & Committed
Time 2: [Message 4,5] -> Arrived but EE stopped
Time 3: [Message 6,7] -> Arrived but EE stopped
```

### Khi EE khởi động lại

```
1. Consumer kết nối lại
2. Lấy offset đã commit cuối cùng (Offset 3)
3. Tiếp tục xử lý từ Message 4
4. Process: [Message 4,5,6,7]
5. Commit sau mỗi message
```

## Khôi phục dữ liệu

### Cách đơn giản nhất

1. Tạo group ID mới
2. Deploy lại ứng dụng
3. Consumer sẽ tự động đọc từ đầu (do `earliest`)

### Ví dụ

```
Group ID cũ: engine-logic-service-group
Group ID mới: engine-logic-service-group-recovery

Result:
- Đọc lại toàn bộ message từ đầu
- Xử lý tuần tự
- Commit sau mỗi message
```

## Best Practices

1. **Monitoring**

   - Theo dõi consumer lag
   - Kiểm tra commit offset
   - Alert khi có vấn đề

2. **Error Handling**

   - Xử lý lỗi cho từng message
   - Không commit khi xử lý thất bại
   - Log đầy đủ thông tin lỗi

3. **Performance**

   - Điều chỉnh `FETCH_MIN_BYTES` theo tải
   - Tối ưu `MAX_POLL_INTERVAL_MS`
   - Cân bằng giữa latency và throughput

4. **Recovery**
   - Backup offset trước khi reset
   - Verify dữ liệu sau khi khôi phục
   - Có kế hoạch rollback
