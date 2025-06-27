# Chiến lược Testing - Exchange Engine

## Tổng quan

Exchange Engine sử dụng nhiều loại test khác nhau để đảm bảo chất lượng code và độ tin cậy của hệ thống. Việc kiểm thử có vai trò quan trọng trong hệ thống tài chính như Exchange Engine, nơi độ chính xác và tính nhất quán của dữ liệu là yếu tố sống còn.

## Công nghệ sử dụng

### Thư viện Testing

- **JUnit 5**: Framework testing chính cho Java
- **Mockito**: Framework để tạo các mock objects
- **AssertJ**: Thư viện assertions với cú pháp fluent API

### Cấu trúc thư mục

Cấu trúc thư mục test phản ánh cấu trúc của codebase chính:

```
src/
├── main/java/com/exchangeengine/...
└── test/java/com/exchangeengine/...
```

## Phân loại Tests

### 1. Unit Tests

Unit tests kiểm tra chức năng của các thành phần riêng lẻ cấp thấp nhất như class và method. Trong Exchange Engine, unit tests được chia thành hai loại:

#### 1.1. Model Tests

Tests cho các model classes như `Account`, `CoinTransaction`, `CoinDeposit`, `CoinWithdrawal`...

Ví dụ từ `AccountTest`:

```java
@Test
@DisplayName("Tăng số dư khả dụng với số lượng hợp lệ")
void increaseAvailableBalance_WithValidAmount_IncreasesBalance() {
    // Setup
    BigDecimal initialBalance = new BigDecimal("10.0");
    BigDecimal increaseAmount = new BigDecimal("5.0");
    account.setAvailableBalance(initialBalance);
    long initialUpdatedAt = account.getUpdatedAt();

    // Execute
    account.increaseAvailableBalance(increaseAmount);

    // Verify
    assertEquals(new BigDecimal("15.0").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP),
                 account.getAvailableBalance());
    assertTrue(account.getUpdatedAt() > initialUpdatedAt);
}
```

Mục tiêu của model tests:

- Kiểm tra validation logic
- Đảm bảo các business rules được thực thi đúng
- Đảm bảo tính nhất quán của dữ liệu

#### 1.2. Service Tests

Tests cho các service classes như `DepositProcessor`, `WithdrawalProcessor`...

Trong service tests, chúng ta sử dụng mock objects để thay thế các dependencies:

```java
@Test
@DisplayName("Xử lý nạp tiền thành công")
void process_WithValidDeposit_SuccessfullyProcessesDeposit() {
    // Execute
    ProcessResult result = processor.process();

    // Verify
    assertTrue(result.isSuccess());
    assertEquals(account, result.getAccount());
    assertEquals(deposit, result.getDeposit());
    assertTrue(result.getAccountHistory().isPresent());

    // Verify account balance was updated correctly
    BigDecimal expectedBalance = new BigDecimal("12.5").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    assertEquals(expectedBalance, account.getAvailableBalance());

    // Verify StorageService interactions
    verify(storageService).getAccount(ACCOUNT_KEY);
    verify(storageService).updateAccount(account);
    verify(storageService).updateCoinDeposit(deposit);
}
```

Mục tiêu của service tests:

- Kiểm tra business logic phức tạp
- Đảm bảo các component tương tác đúng với nhau
- Kiểm tra các điều kiện biên và xử lý lỗi

### 2. Integration Tests

Integration tests kiểm tra các thành phần làm việc cùng nhau như một hệ thống. Trong Exchange Engine, chúng ta tập trung vào các integration tests sau:

#### 2.1. Service-Storage Integration

Tests mối quan hệ giữa các service và storage layer:

- Kiểm tra việc lưu trữ và truy xuất dữ liệu từ RocksDB
- Kiểm tra các transaction và consistency

#### 2.2. Kafka Integration

Tests cho các luồng xử lý thông qua Kafka:

- Gửi và nhận messages thông qua Kafka
- Xử lý các sự kiện từ Kafka và cập nhật state

### 3. End-to-End Tests

E2E tests kiểm tra toàn bộ luồng xử lý từ đầu đến cuối:

- Kiểm tra luồng deposit: từ khi nhận request đến khi cập nhật balance và gửi response
- Kiểm tra luồng withdraw: từ khi nhận request đến khi cập nhật balance và gửi response
- Kiểm tra các query và truy vấn thông tin

## TestContainers

Exchange Engine sử dụng [TestContainers](https://www.testcontainers.org/) để chạy các integration tests với môi trường thực tế:

```java
@Testcontainers
public class KafkaIntegrationTest {
    @Container
    private static final KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.0"));

    @Test
    public void testKafkaProducerConsumer() {
        // Test với Kafka container thực
    }
}
```

Các container hỗ trợ:

- Kafka containers cho testing messaging
- ZooKeeper containers cho Kafka
- Custom containers cho RocksDB nếu cần

## Testing Patterns

### 1. Given-When-Then

Exchange Engine tests sử dụng pattern Given-When-Then để cấu trúc các test cases:

- **Given**: Setup các điều kiện ban đầu
- **When**: Thực hiện hành động cần test
- **Then**: Kiểm tra kết quả

### 2. Test Naming Convention

Quy ước đặt tên tests:

```
methodName_scenario_expectedBehavior
```

Ví dụ: `increaseAvailableBalance_WithNegativeAmount_ThrowsException`

### 3. @DisplayName Annotation

Sử dụng `@DisplayName` để mô tả test case bằng tiếng Việt hoặc ngôn ngữ tự nhiên:

```java
@Test
@DisplayName("Tăng số dư khả dụng với số lượng âm gây ra lỗi")
void increaseAvailableBalance_WithNegativeAmount_ThrowsException() {
    // ...
}
```

## Chạy Tests

```bash
# Chạy tất cả tests
mvn test

# Chạy một test cụ thể
mvn test -Dtest=AccountTest

# Chạy tests cho một package cụ thể
mvn test -Dtest="com.exchangeengine.model.*"

# Khi chạy trên Java 23, cần thêm flag để tương thích với Mockito/ByteBuddy
mvn test -Dnet.bytebuddy.experimental=true
```

## Xử lý lỗi compatibility với Java 23

Khi sử dụng Java 23, có thể gặp lỗi với Mockito và ByteBuddy:

```
Java 23 (67) is not supported by the current version of Byte Buddy which officially supports Java 22 (66)
```

Có nhiều cách để xử lý vấn đề này:

### 1. Sử dụng flag ByteBuddy experimental

```bash
# Khi chạy test
mvn test -Dnet.bytebuddy.experimental=true

# Khi build toàn bộ dự án
mvn clean install -Dnet.bytebuddy.experimental=true
```

### 2. Cấu hình mặc định trong pom.xml

Thêm cấu hình sau vào phần `<properties>` trong file pom.xml:

```xml
<properties>
    <!-- Các properties khác -->
    <argLine>-Dnet.bytebuddy.experimental=true</argLine>
</properties>
```

### 3. Giải pháp tạm thời: Skip tests khi cần

Trong một số trường hợp, bạn có thể cần skip tests khi build:

```bash
# Bỏ qua việc chạy tests, nhưng vẫn biên dịch chúng
mvn clean install -DskipTests

# Bỏ qua hoàn toàn việc biên dịch và chạy tests
mvn clean install -Dmaven.test.skip=true
```

**Lưu ý**: Việc skip tests chỉ nên là giải pháp tạm thời. Trong môi trường phát triển chuẩn:

- Các tests luôn phải pass
- Nên sửa lỗi compatibility thay vì bỏ qua tests
- Chỉ nên skip tests khi đang phát triển tính năng và tests chưa hoàn thiện

### 4. Thêm cấu hình Surefire plugin

Để có nhiều tùy chọn hơn trong việc kiểm soát việc chạy tests, bạn có thể cấu hình Maven Surefire plugin:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
            <configuration>
                <argLine>-Dnet.bytebuddy.experimental=true</argLine>
                <!-- Các cấu hình khác -->
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Best Practices

1. **Isolate Test Dependencies**: Mỗi test không nên phụ thuộc vào test khác
2. **Reset State**: Reset state sau mỗi test với `@BeforeEach` và `@AfterEach`
3. **Mock External Dependencies**: Sử dụng mock objects cho các external dependencies
4. **Test Edge Cases**: Kiểm tra các điều kiện biên và exceptional cases
5. **Code Coverage**: Phấn đấu đạt code coverage cao, đặc biệt cho business logic
6. **Test Performance**: Tests nên chạy nhanh để việc phát triển được hiệu quả

## Test Doubles

Exchange Engine sử dụng các loại test doubles sau:

- **Mocks**: Đối tượng giả lập có thể kiểm tra tương tác
- **Stubs**: Cung cấp các phản hồi cố định
- **Spies**: Theo dõi các cuộc gọi đến đối tượng thật
- **Dummies**: Đối tượng chỉ truyền qua các functions
- **Fakes**: Triển khai đơn giản của một interface

## Continous Integration

Tests được tích hợp vào quy trình CI/CD để đảm bảo code chất lượng:

1. Chạy tests tự động khi có pull request mới
2. Chạy tests trước khi deploy lên môi trường production
3. Báo cáo kết quả tests và code coverage

## Sử dụng Factory Pattern cho Test Data

Để tạo ra test data một cách nhất quán và dễ dàng, Exchange Engine sử dụng Factory Pattern thông qua các lớp như:

- `AccountFactory`: Tạo đối tượng Account với các giá trị mặc định hoặc tùy chỉnh
- `CoinDepositFactory`: Tạo đối tượng CoinDeposit cho các test về nạp tiền
- `CoinWithdrawalFactory`: Tạo đối tượng CoinWithdrawal cho các test về rút tiền
- `DisruptorEventFactory`: Tạo các sự kiện cho hệ thống Disruptor

### Ưu điểm của Factory Pattern trong Testing

1. **Tăng tính nhất quán**: Tất cả các test đều sử dụng các đối tượng cơ bản giống nhau, làm cho việc test dễ dự đoán hơn
2. **Giảm mã lặp lại**: Không cần lặp lại code tạo đối tượng trong mỗi test class
3. **Tập trung hóa việc cấu hình đối tượng test**: Nếu model thay đổi, chỉ cần cập nhật factory class
4. **Cải thiện khả năng đọc của test**: Code test tập trung vào logic test thay vì việc thiết lập đối tượng

### Ví dụ sử dụng Factory trong Test

```java
@ExtendWith(MockitoExtension.class)
class DepositProcessorTest {
    @Mock
    private StorageService storageService;

    private DisruptorEvent event;
    private Account account;
    private CoinDeposit deposit;
    private DepositProcessor processor;

    private static final String ACCOUNT_KEY = "btc:user123";
    private static final String TRANSACTION_ID = "txn123";
    private static final BigDecimal DEPOSIT_AMOUNT = new BigDecimal("2.5");

    @BeforeEach
    void setUp() {
        // Sử dụng Factory để tạo đối tượng test
        account = AccountFactory.create(ACCOUNT_KEY);
        deposit = CoinDepositFactory.create(ACCOUNT_KEY, TRANSACTION_ID, DEPOSIT_AMOUNT);
        event = DisruptorEventFactory.createDepositEvent(ACCOUNT_KEY, TRANSACTION_ID, DEPOSIT_AMOUNT);

        // Tạo processor
        processor = new DepositProcessor(event);

        // Cấu hình mock
        when(storageService.getAccount(ACCOUNT_KEY)).thenReturn(Optional.of(account));
    }

    // Các phương thức test...
}
```

## Kết luận

Chiến lược testing của Exchange Engine được thiết kế để đảm bảo tính chính xác và độ tin cậy của hệ thống. Chúng tôi sử dụng nhiều loại test khác nhau từ unit test đến integration test, kết hợp với các pattern như Factory Pattern để tạo test data có cấu trúc và dễ bảo trì.

Với việc áp dụng các best practices như isolation, mocking và test doubles, Exchange Engine đảm bảo rằng mọi thay đổi trong codebase đều được kiểm tra kỹ lưỡng trước khi triển khai. Điều này giúp duy trì chất lượng code cao và giảm thiểu rủi ro trong môi trường production.

Trong tương lai, chúng tôi sẽ tiếp tục cải thiện chiến lược testing, tăng code coverage và tối ưu hóa performance của test suite để phục vụ tốt hơn cho quy trình phát triển liên tục.
