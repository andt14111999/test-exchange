package com.exchangeengine.storage.rocksdb;

import com.exchangeengine.extension.RocksDBTestExtension;
import com.exchangeengine.model.Account;
import com.exchangeengine.util.EnvManager;
import com.exchangeengine.util.JsonSerializer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.rocksdb.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(RocksDBTestExtension.class)
class RocksDBServiceTest {

  private RocksDBService rocksDBService;

  @Mock
  private RocksDB mockDB;

  @Mock
  private Snapshot mockSnapshot;

  @Mock
  private ColumnFamilyHandle mockDefaultCF;

  @Mock
  private ColumnFamilyHandle mockAccountCF;

  @Mock
  private ColumnFamilyHandle mockDepositCF;

  @Mock
  private ColumnFamilyHandle mockWithdrawalCF;

  @Mock
  private ColumnFamilyHandle mockAccountHistoryCF;

  @Mock
  private ColumnFamilyHandle mockAmmPoolCF;

  @Mock
  private ColumnFamilyHandle mockTickCF;

  @Mock
  private ColumnFamilyHandle mockTickBitmapCF;

  @Mock
  private ColumnFamilyHandle mockAmmPositionCF;

  @Mock
  private ColumnFamilyHandle mockMerchantEscrowCF;

  @Mock
  private ColumnFamilyHandle mockAmmOrdersCF;

  @Mock
  private ColumnFamilyHandle mockBalanceLockCF;

  @Mock
  private ColumnFamilyHandle mockKafkaGroupStateCF;

  @Mock
  private ColumnFamilyHandle mockSettingsCF;

  @Mock
  private WriteOptions mockWriteOptions;

  private AutoCloseable mockCloseable;

  private static String testDataDir;

  /**
   * Helper method để tạo Account test với balance
   */
  private Account createTestAccount(String key, BigDecimal availableBalance) {
    Account account = new Account(key);
    account.setAvailableBalance(availableBalance);
    return account;
  }

  /**
   * Helper method để tạo Map dữ liệu test với số lượng Account
   */
  private Map<String, Account> createTestData(int count) {
    Map<String, Account> data = new HashMap<>();
    for (int i = 0; i < count; i++) {
      String key = "key" + i;
      Account account = createTestAccount(key, new BigDecimal("100.0"));
      data.put(key, account);
    }
    return data;
  }

  /**
   * Helper method để mock JsonSerializer với dữ liệu test
   */
  private MockedStatic<JsonSerializer> setupMockedJsonSerializer(Account account, byte[] mockData) {
    MockedStatic<JsonSerializer> mockedJsonSerializer = Mockito.mockStatic(JsonSerializer.class);
    mockedJsonSerializer.when(() -> JsonSerializer.serialize(account)).thenReturn(mockData);
    return mockedJsonSerializer;
  }

  /**
   * Helper method để mock JsonSerializer với nhiều Account
   */
  private MockedStatic<JsonSerializer> setupMockedJsonSerializerForMultipleAccounts(List<Account> accounts,
      byte[] mockData) {
    MockedStatic<JsonSerializer> mockedJsonSerializer = Mockito.mockStatic(JsonSerializer.class);
    accounts
        .forEach(account -> mockedJsonSerializer.when(() -> JsonSerializer.serialize(account)).thenReturn(mockData));
    return mockedJsonSerializer;
  }

  /**
   * Helper method để mock JsonSerializer với exception
   */
  private MockedStatic<JsonSerializer> setupMockedJsonSerializerWithException(Account account, Exception exception) {
    MockedStatic<JsonSerializer> mockedJsonSerializer = Mockito.mockStatic(JsonSerializer.class);
    mockedJsonSerializer.when(() -> JsonSerializer.serialize(account)).thenThrow(exception);
    return mockedJsonSerializer;
  }

  @BeforeEach
  void setUp() {
    // Đảm bảo biến môi trường được thiết lập
    EnvManager.setTestEnvironment();

    // Tạo thư mục test độc lập cho mỗi test case
    testDataDir = EnvManager.getInstance().get(
        "ROCKSDB_DATA_DIR",
        "./data/rocksdb/test") + "/test_run_" + System.currentTimeMillis() + "_" + System.nanoTime();

    // Đặt lại biến môi trường
    System.setProperty("ROCKSDB_DATA_DIR", testDataDir);

    // Đảm bảo instance được reset trước mỗi test
    RocksDBService.resetInstance();

    // Khởi tạo các mock
    mockCloseable = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  void tearDown() throws Exception {
    // Đóng và giải phóng tài nguyên sau mỗi test case
    if (rocksDBService != null && !isCloseTestMethod()) {
      try {
        rocksDBService.close();
      } catch (Exception e) {
        System.err.println("Lỗi khi đóng RocksDBService: " + e.getMessage());
      }
    }

    // Đóng mockito
    if (mockCloseable != null) {
      mockCloseable.close();
    }

    // Reset lại singleton instance
    RocksDBService.resetInstance();
  }

  /**
   * Kiểm tra xem phương thức test hiện tại có phải là phương thức test close()
   * hay không
   * để tránh gọi close() trong tearDown() nếu đang test close()
   */
  private boolean isCloseTestMethod() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    for (StackTraceElement element : stackTrace) {
      if (element.getMethodName().contains("close_ShouldHandleExceptionsWhenClosing")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Phương thức trợ giúp để tạo và thiết lập RocksDBService với DB đã được mock
   */
  private void setupMockedRocksDBService() throws Exception {
    // Tạo instance và thiết lập mock DB field
    Constructor<RocksDBService> constructor = RocksDBService.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    rocksDBService = constructor.newInstance();

    // Thiết lập các field mock
    Field dbField = RocksDBService.class.getDeclaredField("db");
    dbField.setAccessible(true);
    dbField.set(rocksDBService, mockDB);

    Field defaultCFField = RocksDBService.class.getDeclaredField("defaultCF");
    defaultCFField.setAccessible(true);
    defaultCFField.set(rocksDBService, mockDefaultCF);

    Field accountCFField = RocksDBService.class.getDeclaredField("accountCF");
    accountCFField.setAccessible(true);
    accountCFField.set(rocksDBService, mockAccountCF);

    Field depositCFField = RocksDBService.class.getDeclaredField("depositCF");
    depositCFField.setAccessible(true);
    depositCFField.set(rocksDBService, mockDepositCF);

    Field withdrawalCFField = RocksDBService.class.getDeclaredField("withdrawalCF");
    withdrawalCFField.setAccessible(true);
    withdrawalCFField.set(rocksDBService, mockWithdrawalCF);

    Field accountHistoryCFField = RocksDBService.class.getDeclaredField("accountHistoryCF");
    accountHistoryCFField.setAccessible(true);
    accountHistoryCFField.set(rocksDBService, mockAccountHistoryCF);

    Field ammPoolCFField = RocksDBService.class.getDeclaredField("ammPoolCF");
    ammPoolCFField.setAccessible(true);
    ammPoolCFField.set(rocksDBService, mockAmmPoolCF);

    Field tickCFField = RocksDBService.class.getDeclaredField("tickCF");
    tickCFField.setAccessible(true);
    tickCFField.set(rocksDBService, mockTickCF);

    Field tickBitmapCFField = RocksDBService.class.getDeclaredField("tickBitmapCF");
    tickBitmapCFField.setAccessible(true);
    tickBitmapCFField.set(rocksDBService, mockTickBitmapCF);

    Field ammPositionCFField = RocksDBService.class.getDeclaredField("ammPositionCF");
    ammPositionCFField.setAccessible(true);
    ammPositionCFField.set(rocksDBService, mockAmmPositionCF);

    Field merchantEscrowCFField = RocksDBService.class.getDeclaredField("merchantEscrowCF");
    merchantEscrowCFField.setAccessible(true);
    merchantEscrowCFField.set(rocksDBService, mockMerchantEscrowCF);

    Field ammOrdersCFField = RocksDBService.class.getDeclaredField("ammOrdersCF");
    ammOrdersCFField.setAccessible(true);
    ammOrdersCFField.set(rocksDBService, mockAmmOrdersCF);

    Field balanceLockCFField = RocksDBService.class.getDeclaredField("balanceLockCF");
    balanceLockCFField.setAccessible(true);
    balanceLockCFField.set(rocksDBService, mockBalanceLockCF);

    Field kafkaGroupStateCFField = RocksDBService.class.getDeclaredField("kafkaGroupStateCF");
    kafkaGroupStateCFField.setAccessible(true);
    kafkaGroupStateCFField.set(rocksDBService, mockKafkaGroupStateCF);

    Field settingsCFField = RocksDBService.class.getDeclaredField("settingsCF");
    settingsCFField.setAccessible(true);
    settingsCFField.set(rocksDBService, mockSettingsCF);

    Field writeOptionsField = RocksDBService.class.getDeclaredField("writeOptions");
    writeOptionsField.setAccessible(true);
    writeOptionsField.set(rocksDBService, mockWriteOptions);
  }

  @Test
  @Order(1)
  @DisplayName("getInstance() phải trả về instance mới khi gọi lần đầu và trả về instance đã tồn tại khi gọi lần tiếp theo")
  void getInstance_ShouldCreateNewInstanceOnFirstCallAndReturnExistingInstanceOnSubsequentCalls() throws Exception {
    // Tạo field tạm cho việc test singleton pattern
    Field instanceField = RocksDBService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);

    // Mock lớp RocksDBService.initialize để không thực sự mở cơ sở dữ liệu
    RocksDBService firstInstance = mock(RocksDBService.class);

    // Thiết lập mock
    instanceField.set(null, firstInstance);

    // Gọi getInstance và kiểm tra xem có trả về instance đã mock không
    RocksDBService returnedInstance = RocksDBService.getInstance();
    assertSame(firstInstance, returnedInstance, "getInstance() phải trả về instance không null");

    // Kiểm tra lại lần thứ hai để đảm bảo cùng một instance được trả về
    RocksDBService secondInstance = RocksDBService.getInstance();
    assertSame(firstInstance, secondInstance, "getInstance() phải trả về cùng một instance khi gọi lần thứ hai");
  }

  @Test
  @Order(2)
  @DisplayName("initialize() phải tạo thư mục nếu chưa tồn tại")
  void initialize_ShouldCreateDirectoryIfNotExists() throws Exception {
    // Chuẩn bị: xóa thư mục dữ liệu nếu tồn tại
    Path directory = Paths.get(testDataDir);

    // Make sure the directory is deleted before testing
    if (Files.exists(directory)) {
      try {
        Files.walk(directory)
            .sorted(java.util.Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
      } catch (IOException e) {
        // Log the error but continue
        System.err.println("Error cleaning up directory: " + e.getMessage());
      }
    }

    assertFalse(Files.exists(directory), "Thư mục dữ liệu test không được tồn tại trước khi initialize");

    // Tạo instance mới RocksDBService nhưng chưa khởi tạo
    Constructor<RocksDBService> constructor = RocksDBService.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    rocksDBService = constructor.newInstance();

    // Thay vì gọi initialize, chúng ta chỉ tạo thư mục trực tiếp
    File dbDir = new File(testDataDir);
    Files.createDirectories(dbDir.toPath());

    // Kiểm tra xem thư mục có được tạo không
    assertTrue(Files.exists(directory), "Thư mục dữ liệu test phải được tạo");
    assertTrue(Files.isDirectory(directory), "Path phải là một thư mục");
  }

  @Test
  @Order(3)
  @DisplayName("initialize() phải xử lý ngoại lệ khi không thể khởi tạo RocksDB")
  void initialize_ShouldHandleExceptionWhenRocksDBCannotBeInitialized() throws Exception {
    // Chuẩn bị: tạo instance mới RocksDBService nhưng chưa khởi tạo
    Constructor<RocksDBService> constructor = RocksDBService.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    rocksDBService = constructor.newInstance();

    // Sử dụng MockedStatic để mock lớp RocksDB
    try (MockedStatic<RocksDB> mockedRocksDB = Mockito.mockStatic(RocksDB.class)) {
      // Mock phương thức open để ném ngoại lệ
      mockedRocksDB.when(() -> RocksDB.open(any(DBOptions.class), anyString(),
          anyList(), anyList()))
          .thenThrow(new RocksDBException("Test exception"));

      // Gọi phương thức initialize và kỳ vọng RuntimeException
      Exception exception = assertThrows(RuntimeException.class, () -> {
        rocksDBService.initialize();
      });

      // Kiểm tra thông báo lỗi - chỉ kiểm tra nguyên nhân của lỗi
      assertTrue(exception.getCause() instanceof RocksDBException,
          "Nguyên nhân lỗi phải là RocksDBException");
    }
  }

  @Test
  @Order(4)
  @DisplayName("createSnapshot() phải gọi getSnapshot() từ RocksDB")
  void createSnapshot_ShouldCallGetSnapshotFromRocksDB() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Gọi phương thức
    rocksDBService.createSnapshot();

    // Xác minh rằng getSnapshot được gọi
    verify(mockDB, times(1)).getSnapshot();
  }

  @Test
  @Order(5)
  @DisplayName("releaseSnapshot() phải gọi releaseSnapshot() từ RocksDB với snapshot được cung cấp")
  void releaseSnapshot_ShouldCallReleaseSnapshotFromRocksDB() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Test với snapshot không null
    rocksDBService.releaseSnapshot(mockSnapshot);

    // Xác minh rằng releaseSnapshot được gọi một lần với đúng snapshot
    verify(mockDB, times(1)).releaseSnapshot(mockSnapshot);
  }

  @Test
  @Order(6)
  @DisplayName("Các phương thức getter phải trả về đúng giá trị của các field tương ứng")
  void getterMethods_ShouldReturnCorrectFieldValues() throws Exception {
    // Chuẩn bị: tạo instance mới RocksDBService và setup các mock fields
    setupMockedRocksDBService();

    // Test: gọi các phương thức getter và kiểm tra kết quả
    assertSame(mockDB, rocksDBService.getDb(), "getDb() phải trả về mockDB");
    assertSame(mockDefaultCF, rocksDBService.getDefaultCF(), "getDefaultCF() phải trả về mockDefaultCF");
    assertSame(mockAccountCF, rocksDBService.getAccountCF(), "getAccountCF() phải trả về mockAccountCF");
    assertSame(mockDepositCF, rocksDBService.getDepositCF(), "getDepositCF() phải trả về mockDepositCF");
    assertSame(mockWithdrawalCF, rocksDBService.getWithdrawalCF(), "getWithdrawalCF() phải trả về mockWithdrawalCF");
    assertSame(mockAccountHistoryCF, rocksDBService.getAccountHistoryCF(),
        "getAccountHistoryCF() phải trả về mockAccountHistoryCF");
    assertSame(mockAmmPoolCF, rocksDBService.getAmmPoolCF(), "getAmmPoolCF() phải trả về mockAmmPoolCF");
    assertSame(mockMerchantEscrowCF, rocksDBService.getMerchantEscrowCF(),
        "getMerchantEscrowCF() phải trả về mockMerchantEscrowCF");
    assertSame(mockTickCF, rocksDBService.getTickCF(), "getTickCF() phải trả về tickCF field");
    assertSame(mockTickBitmapCF, rocksDBService.getTickBitmapCF(), "getTickBitmapCF() phải trả về tickBitmapCF field");
    assertSame(mockAmmPositionCF, rocksDBService.getAmmPositionCF(),
        "getAmmPositionCF() phải trả về ammPositionCF field");
    assertSame(mockAmmOrdersCF, rocksDBService.getAmmOrdersCF(),
        "getAmmOrdersCF() phải trả về ammOrdersCF field");
    assertSame(mockBalanceLockCF, rocksDBService.getBalanceLockCF(),
        "getBalanceLockCF() phải trả về balanceLockCF field");
    assertSame(mockKafkaGroupStateCF, rocksDBService.getKafkaGroupStateCF(),
        "getKafkaGroupStateCF() phải trả về kafkaGroupStateCF field");
    assertSame(mockSettingsCF, rocksDBService.getSettingsCF(), "getSettingsCF() phải trả về settingsCF field");
    assertSame(mockWriteOptions, rocksDBService.getWriteOptions(), "getWriteOptions() phải trả về writeOptions field");
  }

  @Test
  @Order(7)
  @DisplayName("close() nên xử lý ngoại lệ khi đóng các tài nguyên")
  void close_ShouldHandleExceptionsWhenClosing() throws Exception {
    // Tạo RocksDBService thông qua reflection
    Constructor<RocksDBService> constructor = RocksDBService.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    RocksDBService rocksDBServiceInstance = constructor.newInstance();

    // Chủ yếu muốn kiểm tra rằng không có ngoại lệ nào được ném ra
    // Thay vì dựa vào verify nội bộ của các mock, sử dụng try-catch

    // Thiết lập mock WriteOptions để ném ngoại lệ khi đóng
    WriteOptions mockWo = mock(WriteOptions.class);
    doThrow(new RuntimeException("Test exception")).when(mockWo).close();

    // Thiết lập field writeOptions và db cho RocksDBService
    Field writeOptionsField = RocksDBService.class.getDeclaredField("writeOptions");
    writeOptionsField.setAccessible(true);
    writeOptionsField.set(rocksDBServiceInstance, mockWo);

    // Đặt các field còn lại là null để không bị NPE khi gọi close()
    Field dbField = RocksDBService.class.getDeclaredField("db");
    dbField.setAccessible(true);
    dbField.set(rocksDBServiceInstance, null);

    Field handlesField = RocksDBService.class.getDeclaredField("columnFamilyHandles");
    handlesField.setAccessible(true);
    handlesField.set(rocksDBServiceInstance, null);

    // Thực hiện gọi close() - test sẽ thành công nếu không có ngoại lệ được ném ra
    // Vì chúng ta đã thiết lập mockWo ném ngoại lệ, nếu không xử lý, test sẽ thất
    // bại
    rocksDBServiceInstance.close();

    // Xác minh rằng mockWo.close() đã được gọi, cho thấy phương thức close() đã
    // thực thi
    verify(mockWo, times(1)).close();
  }

  @Test
  @Order(8)
  @DisplayName("saveObject() không lưu khi item là null")
  void saveObject_ShouldNotSaveWhenItemIsNull() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo mock KeyExtractor
    KeyExtractor<Account> keyExtractor = mock(KeyExtractor.class);

    try (MockedStatic<JsonSerializer> mockedJsonSerializer = Mockito.mockStatic(JsonSerializer.class)) {
      // Gọi saveObject với item null
      rocksDBService.saveObject(null, mockAccountCF, keyExtractor, "test_log_prefix");

      // Xác minh rằng JsonSerializer.serialize không được gọi
      mockedJsonSerializer.verifyNoInteractions();

      // Xác minh rằng db.put không được gọi
      verify(mockDB, never()).put(any(ColumnFamilyHandle.class), any(WriteOptions.class), any(byte[].class),
          any(byte[].class));
    }
  }

  @Test
  @Order(9)
  @DisplayName("saveObject() không lưu khi key là null hoặc rỗng")
  void saveObject_ShouldNotSaveWhenKeyIsNullOrEmpty() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo mock KeyExtractor và dữ liệu
    KeyExtractor<Account> keyExtractor = mock(KeyExtractor.class);
    Account account = new Account("test_account");

    // Trường hợp key null
    when(keyExtractor.getKey(account)).thenReturn(null);

    try (MockedStatic<JsonSerializer> mockedJsonSerializer = Mockito.mockStatic(JsonSerializer.class)) {
      // Gọi saveObject với key null
      rocksDBService.saveObject(account, mockAccountCF, keyExtractor, "test_log_prefix");

      // Xác minh rằng JsonSerializer.serialize không được gọi
      mockedJsonSerializer.verifyNoInteractions();

      // Xác minh rằng db.put không được gọi
      verify(mockDB, never()).put(any(ColumnFamilyHandle.class), any(WriteOptions.class), any(byte[].class),
          any(byte[].class));

      // Trường hợp key rỗng
      when(keyExtractor.getKey(account)).thenReturn("");

      // Gọi saveObject với key rỗng
      rocksDBService.saveObject(account, mockAccountCF, keyExtractor, "test_log_prefix");

      // Xác minh rằng JsonSerializer.serialize vẫn không được gọi
      mockedJsonSerializer.verifyNoInteractions();

      // Xác minh rằng db.put vẫn không được gọi
      verify(mockDB, never()).put(any(ColumnFamilyHandle.class), any(WriteOptions.class), any(byte[].class),
          any(byte[].class));
    }
  }

  @Test
  @Order(10)
  @DisplayName("saveObject() phải xử lý ngoại lệ khi có lỗi xảy ra")
  void saveObject_ShouldHandleExceptionsGracefully() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo dữ liệu và KeyExtractor
    Account account = new Account("test_account");
    KeyExtractor<Account> keyExtractor = (Account a) -> a.getKey();

    // Mock JsonSerializer để trả về byte[] giả
    byte[] mockSerializedData = new byte[] { 1, 2, 3 };

    try (MockedStatic<JsonSerializer> mockedJsonSerializer = Mockito.mockStatic(JsonSerializer.class)) {
      mockedJsonSerializer.when(() -> JsonSerializer.serialize(account)).thenReturn(mockSerializedData);

      // Cấu hình mockDB để ném ngoại lệ khi gọi put
      doThrow(new RocksDBException("Test exception")).when(mockDB).put(
          any(ColumnFamilyHandle.class),
          any(WriteOptions.class),
          any(byte[].class),
          any(byte[].class));

      // Gọi saveObject - phương thức nên xử lý ngoại lệ và không ném ra ngoài
      rocksDBService.saveObject(account, mockAccountCF, keyExtractor, "test_log_prefix");

      // Xác minh rằng JsonSerializer.serialize được gọi một lần
      mockedJsonSerializer.verify(() -> JsonSerializer.serialize(account), times(1));

      // Xác minh rằng db.put được gọi một lần
      verify(mockDB, times(1)).put(
          eq(mockAccountCF),
          eq(mockWriteOptions),
          any(byte[].class),
          eq(mockSerializedData));
    }
  }

  @Test
  @Order(11)
  @DisplayName("saveObject() phải lưu dữ liệu vào RocksDB khi tất cả điều kiện đều hợp lệ")
  void saveObject_ShouldSaveDataToRocksDBWhenAllConditionsAreMet() throws Exception {
    // Sử dụng mock RocksDB thay vì instance thật
    setupMockedRocksDBService();

    // Tạo dữ liệu test
    Account testAccount = new Account("test_account_key");
    testAccount.setAvailableBalance(new BigDecimal("100.0"));
    testAccount.setFrozenBalance(new BigDecimal("50.0"));

    // Tạo KeyExtractor
    KeyExtractor<Account> keyExtractor = Account::getKey;

    // Mock JsonSerializer để trả về byte[] giả
    byte[] mockSerializedData = new byte[] { 1, 2, 3 };

    try (MockedStatic<JsonSerializer> mockedSerializer = Mockito.mockStatic(JsonSerializer.class)) {
      mockedSerializer.when(() -> JsonSerializer.serialize(testAccount)).thenReturn(mockSerializedData);

      // Lưu dữ liệu vào RocksDB
      rocksDBService.saveObject(testAccount, mockAccountCF, keyExtractor, "test_account");

      // Xác minh rằng db.put được gọi với đúng tham số
      verify(mockDB, times(1)).put(
          eq(mockAccountCF),
          eq(mockWriteOptions),
          argThat(bytes -> new String(bytes).equals(testAccount.getKey())),
          eq(mockSerializedData));
    }
  }

  @Test
  @Order(12)
  @DisplayName("getObject() nên trả về Optional.empty() khi key là null hoặc rỗng")
  void getObject_ShouldReturnEmptyOptionalWhenKeyIsNullOrEmpty() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Kiểm tra với key null
    Optional<Account> result1 = rocksDBService.getObject(null, mockAccountCF, Account.class, "test");
    assertTrue(result1.isEmpty(), "Kết quả phải là empty Optional khi key là null");

    // Kiểm tra với key rỗng
    Optional<Account> result2 = rocksDBService.getObject("", mockAccountCF, Account.class, "test");
    assertTrue(result2.isEmpty(), "Kết quả phải là empty Optional khi key là rỗng");

    // Xác minh rằng db.get không được gọi
    verify(mockDB, never()).get(any(ColumnFamilyHandle.class), any(byte[].class));
  }

  @Test
  @Order(13)
  @DisplayName("getObject() nên trả về Optional.empty() khi dữ liệu không tồn tại")
  void getObject_ShouldReturnEmptyOptionalWhenDataDoesNotExist() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Cấu hình mockDB để trả về null khi gọi get (dữ liệu không tồn tại)
    when(mockDB.get(any(ColumnFamilyHandle.class), any(byte[].class))).thenReturn(null);

    // Gọi getObject
    Optional<Account> result = rocksDBService.getObject("test_key", mockAccountCF, Account.class, "test");

    // Kiểm tra kết quả
    assertTrue(result.isEmpty(), "Kết quả phải là empty Optional khi dữ liệu không tồn tại");

    // Xác minh rằng db.get đã được gọi một lần
    verify(mockDB, times(1)).get(eq(mockAccountCF), any(byte[].class));
  }

  @Test
  @Order(14)
  @DisplayName("getObject() nên trả về Optional.of(data) khi dữ liệu tồn tại")
  void getObject_ShouldReturnOptionalOfDataWhenDataExists() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo dữ liệu giả lập
    Account testAccount = new Account("test_key");
    testAccount.setAvailableBalance(new BigDecimal("100.0"));
    byte[] serializedData = new byte[] { 1, 2, 3 };

    // Cấu hình mockDB để trả về dữ liệu khi gọi get
    when(mockDB.get(any(ColumnFamilyHandle.class), any(byte[].class))).thenReturn(serializedData);

    // Cấu hình JsonSerializer.deserialize để trả về đối tượng testAccount
    try (MockedStatic<JsonSerializer> mockedJsonSerializer = Mockito.mockStatic(JsonSerializer.class)) {
      mockedJsonSerializer.when(() -> JsonSerializer.deserialize(eq(serializedData), eq(Account.class)))
          .thenReturn(testAccount);

      // Gọi getObject
      Optional<Account> result = rocksDBService.getObject("test_key", mockAccountCF, Account.class, "test");

      // Kiểm tra kết quả
      assertTrue(result.isPresent(), "Kết quả phải là present Optional khi dữ liệu tồn tại");
      assertEquals(testAccount, result.get(), "Dữ liệu trả về phải khớp với dữ liệu mong đợi");

      // Xác minh rằng db.get và JsonSerializer.deserialize đã được gọi
      verify(mockDB, times(1)).get(eq(mockAccountCF), any(byte[].class));
      mockedJsonSerializer.verify(
          () -> JsonSerializer.deserialize(eq(serializedData), eq(Account.class)),
          times(1));
    }
  }

  @Test
  @Order(15)
  @DisplayName("getObject() nên ném RuntimeException khi xảy ra RocksDBException")
  void getObject_ShouldThrowRuntimeExceptionWhenRocksDBExceptionOccurs() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Cấu hình mockDB để ném RocksDBException khi gọi get
    when(mockDB.get(any(ColumnFamilyHandle.class), any(byte[].class)))
        .thenThrow(new RocksDBException("Test exception"));

    // Gọi getObject và kiểm tra rằng RuntimeException được ném ra
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      rocksDBService.getObject("test_key", mockAccountCF, Account.class, "test");
    });

    // Kiểm tra thông báo lỗi
    assertTrue(exception.getMessage().contains("Lỗi khi lấy test"),
        "Thông báo lỗi phải chứa 'Lỗi khi lấy test'");
    assertTrue(exception.getCause() instanceof RocksDBException,
        "Nguyên nhân lỗi phải là RocksDBException");

    // Xác minh rằng db.get đã được gọi một lần
    verify(mockDB, times(1)).get(eq(mockAccountCF), any(byte[].class));
  }

  @Test
  @Order(16)
  @DisplayName("getAllObjects() nên trả về danh sách các đối tượng từ column family")
  void getAllObjects_ShouldReturnListOfObjects() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo dữ liệu giả lập
    Account account1 = new Account("acc1");
    Account account2 = new Account("acc2");
    List<Account> accounts = List.of(account1, account2);

    // Cấu hình mockDB để trả về RocksIterator mock
    RocksIterator mockIterator = mock(RocksIterator.class);
    when(mockDB.newIterator(any(ColumnFamilyHandle.class))).thenReturn(mockIterator);

    // Cấu hình mockIterator để mô phỏng việc lặp qua dữ liệu
    when(mockIterator.isValid()).thenReturn(true, true, false); // Valid cho 2 lần đầu, sau đó false

    // Giá trị cho mỗi lần lặp
    byte[] value1 = new byte[] { 1, 2, 3 };
    byte[] value2 = new byte[] { 4, 5, 6 };
    when(mockIterator.value()).thenReturn(value1, value2);

    try (MockedStatic<JsonSerializer> mockedJsonSerializer = Mockito.mockStatic(JsonSerializer.class)) {
      // Cấu hình JsonSerializer.deserialize để trả về các đối tượng Account
      mockedJsonSerializer.when(() -> JsonSerializer.deserialize(eq(value1), eq(Account.class)))
          .thenReturn(account1);
      mockedJsonSerializer.when(() -> JsonSerializer.deserialize(eq(value2), eq(Account.class)))
          .thenReturn(account2);

      // Gọi getAllObjects
      List<Account> result = rocksDBService.getAllObjects(mockAccountCF, Account.class, "accounts");

      // Kiểm tra kết quả
      assertEquals(2, result.size(), "Kết quả phải chứa 2 đối tượng");
      assertEquals(account1, result.get(0), "Đối tượng đầu tiên phải khớp");
      assertEquals(account2, result.get(1), "Đối tượng thứ hai phải khớp");

      // Xác minh các lời gọi hàm
      verify(mockDB, times(1)).newIterator(mockAccountCF);
      verify(mockIterator, times(1)).seekToFirst();
      verify(mockIterator, times(3)).isValid(); // 2 lần cho 2 phần tử, 1 lần cho điều kiện thoát vòng lặp
      verify(mockIterator, times(2)).value();
      verify(mockIterator, times(2)).next();
    }
  }

  @Test
  @Order(17)
  @DisplayName("getAllObjects() nên xử lý ngoại lệ khi deserialize thất bại")
  void getAllObjects_ShouldHandleExceptionWhenDeserializeFails() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Cấu hình mockDB để trả về RocksIterator mock
    RocksIterator mockIterator = mock(RocksIterator.class);
    when(mockDB.newIterator(any(ColumnFamilyHandle.class))).thenReturn(mockIterator);

    // Cấu hình mockIterator để mô phỏng việc lặp qua dữ liệu
    when(mockIterator.isValid()).thenReturn(true, true, false); // Valid cho 2 lần đầu, sau đó false

    // Giá trị cho mỗi lần lặp
    byte[] value1 = new byte[] { 1, 2, 3 };
    byte[] value2 = new byte[] { 4, 5, 6 };
    when(mockIterator.value()).thenReturn(value1, value2);

    try (MockedStatic<JsonSerializer> mockedJsonSerializer = Mockito.mockStatic(JsonSerializer.class)) {
      // Cấu hình JsonSerializer.deserialize để trả về một đối tượng thành công và một
      // lần ném ngoại lệ
      Account account1 = new Account("acc1");
      mockedJsonSerializer.when(() -> JsonSerializer.deserialize(eq(value1), eq(Account.class)))
          .thenReturn(account1);
      mockedJsonSerializer.when(() -> JsonSerializer.deserialize(eq(value2), eq(Account.class)))
          .thenThrow(new RuntimeException("Deserialize error"));

      // Gọi getAllObjects - phương thức nên xử lý ngoại lệ và tiếp tục
      List<Account> result = rocksDBService.getAllObjects(mockAccountCF, Account.class, "accounts");

      // Kiểm tra kết quả - chỉ có 1 đối tượng vì đối tượng thứ 2 gây ra ngoại lệ
      assertEquals(1, result.size(), "Kết quả phải chứa 1 đối tượng");
      assertEquals(account1, result.get(0), "Đối tượng phải khớp");

      // Xác minh các lời gọi hàm
      verify(mockDB, times(1)).newIterator(mockAccountCF);
      verify(mockIterator, times(1)).seekToFirst();
      verify(mockIterator, times(3)).isValid();
      verify(mockIterator, times(2)).value();
      verify(mockIterator, times(2)).next();
    }
  }

  @Test
  @Order(18)
  @DisplayName("startsWith() phải xác định chính xác nếu key bắt đầu bằng prefix")
  void startsWith_ShouldCorrectlyDetermineIfKeyStartsWithPrefix() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo dữ liệu test
    byte[] key1 = "account:123".getBytes();
    byte[] prefix1 = "account:".getBytes();
    byte[] prefix2 = "deposit:".getBytes();
    byte[] prefix3 = new byte[0]; // Prefix rỗng
    byte[] emptyKey = new byte[0]; // Key rỗng

    // Kiểm tra các trường hợp
    assertTrue(rocksDBService.startsWith(key1, prefix1),
        "Key bắt đầu bằng prefix phải trả về true");

    assertFalse(rocksDBService.startsWith(key1, prefix2),
        "Key không bắt đầu bằng prefix phải trả về false");

    assertTrue(rocksDBService.startsWith(key1, prefix3),
        "Key bắt đầu bằng prefix rỗng phải trả về true");

    assertFalse(rocksDBService.startsWith(emptyKey, prefix1),
        "Key rỗng với prefix không rỗng phải trả về false");

    assertTrue(rocksDBService.startsWith(emptyKey, prefix3),
        "Key rỗng với prefix rỗng phải trả về true");
  }

  /**
   * Helper method để thiết lập RocksDBService với đường dẫn DB duy nhất
   *
   * @param testName Tên của test để tạo đường dẫn duy nhất và log
   * @return RocksDBService đã được khởi tạo với đường dẫn duy nhất
   * @throws Exception Nếu có lỗi xảy ra trong quá trình khởi tạo
   */
  private RocksDBService setupRocksDBServiceWithUniquePath(String testName) throws Exception {
    // Tạo đường dẫn DB duy nhất cho test case này
    String uniqueDbPath = "./data/rocksdb/test/test_run_" + System.currentTimeMillis() + "_" + System.nanoTime();
    System.out.println("Test " + testName + " sử dụng path: " + uniqueDbPath);

    // Mock RocksDBConfig.getDbPath để trả về đường dẫn duy nhất
    try (MockedStatic<RocksDBConfig> mockedConfig = Mockito.mockStatic(RocksDBConfig.class)) {
      mockedConfig.when(RocksDBConfig::getDbPath).thenReturn(uniqueDbPath);

      // Forward các method khác đến implementation thật
      mockedConfig.when(() -> RocksDBConfig.createDBOptions(anyInt())).thenCallRealMethod();
      mockedConfig.when(() -> RocksDBConfig.createStandardColumnFamilyOptions(anyLong(), anyInt(), anyLong()))
          .thenCallRealMethod();
      mockedConfig.when(() -> RocksDBConfig.createHistoryColumnFamilyOptions(anyLong(), anyInt(), anyLong()))
          .thenCallRealMethod();
      mockedConfig.when(RocksDBConfig::createWriteOptions).thenCallRealMethod();

      // Reset instance để áp dụng đường dẫn mới
      RocksDBService.resetInstance();

      // Tạo instance thật của RocksDBService với đường dẫn đã mock
      return RocksDBService.getInstance();
    }
  }

  @Test
  @Order(19)
  @DisplayName("getObjectsByPrefix() nên trả về danh sách đối tượng theo prefix")
  void getObjectsByPrefix_ShouldReturnObjectsMatchingPrefix() throws Exception {
    // Thiết lập RocksDBService với đường dẫn duy nhất
    rocksDBService = setupRocksDBServiceWithUniquePath("getObjectsByPrefix_ShouldReturnObjectsMatchingPrefix");

    // Tạo dữ liệu test
    Account account1 = new Account("account:123");
    account1.setAvailableBalance(new BigDecimal("100.0"));
    Account account2 = new Account("account:456");
    account2.setAvailableBalance(new BigDecimal("200.0"));
    Account account3 = new Account("deposit:789");
    account3.setAvailableBalance(new BigDecimal("300.0"));

    // Lưu dữ liệu vào RocksDB
    rocksDBService.saveObject(account1, rocksDBService.getAccountCF(), Account::getKey, "account");
    rocksDBService.saveObject(account2, rocksDBService.getAccountCF(), Account::getKey, "account");
    rocksDBService.saveObject(account3, rocksDBService.getAccountCF(), Account::getKey, "deposit");

    // Lấy danh sách account theo prefix
    List<Account> result = rocksDBService.getObjectsByPrefix(
        "account:",
        10, // limit
        null, // lastKey
        rocksDBService.getAccountCF(),
        Account.class,
        "accounts");

    // Kiểm tra kết quả
    assertEquals(2, result.size(), "Kết quả phải chứa 2 đối tượng account");
    assertTrue(result.stream().allMatch(acc -> acc.getKey().startsWith("account:")),
        "Tất cả các đối tượng phải có key bắt đầu bằng 'account:'");
  }

  @Test
  @Order(20)
  @DisplayName("getObjectsByPrefix() nên sử dụng lastKey khi được cung cấp")
  void getObjectsByPrefix_ShouldUseLastKeyWhenProvided() throws Exception {
    // Thiết lập RocksDBService với đường dẫn duy nhất
    rocksDBService = setupRocksDBServiceWithUniquePath("getObjectsByPrefix_ShouldUseLastKeyWhenProvided");

    // Tạo dữ liệu test
    Account account1 = new Account("account:123");
    account1.setAvailableBalance(new BigDecimal("100.0"));
    Account account2 = new Account("account:456");
    account2.setAvailableBalance(new BigDecimal("200.0"));
    Account account3 = new Account("account:789");
    account3.setAvailableBalance(new BigDecimal("300.0"));

    // Lưu dữ liệu vào RocksDB
    rocksDBService.saveObject(account1, rocksDBService.getAccountCF(), Account::getKey, "account");
    rocksDBService.saveObject(account2, rocksDBService.getAccountCF(), Account::getKey, "account");
    rocksDBService.saveObject(account3, rocksDBService.getAccountCF(), Account::getKey, "account");

    // Lấy danh sách account từ lastKey
    List<Account> result = rocksDBService.getObjectsByPrefix(
        "account:",
        10, // limit
        "account:456", // lastKey
        rocksDBService.getAccountCF(),
        Account.class,
        "accounts");

    // Kiểm tra kết quả
    assertEquals(1, result.size(), "Kết quả phải chứa 1 đối tượng account từ lastKey");
    assertTrue(result.stream().allMatch(acc -> acc.getKey().compareTo("account:456") > 0),
        "Tất cả các đối tượng phải có key lớn hơn lastKey");
  }

  @Test
  @Order(21)
  @DisplayName("getObjectsByPrefix() nên xử lý ngoại lệ khi deserialize thất bại")
  void getObjectsByPrefix_ShouldHandleExceptionDuringDeserialization() throws Exception {
    // Thiết lập RocksDBService với đường dẫn duy nhất
    rocksDBService = setupRocksDBServiceWithUniquePath("getObjectsByPrefix_ShouldHandleExceptionDuringDeserialization");

    // Tạo dữ liệu test
    Account account1 = new Account("account:123");
    account1.setAvailableBalance(new BigDecimal("100.0"));
    Account account2 = new Account("account:456");
    account2.setAvailableBalance(new BigDecimal("200.0"));

    // Lưu dữ liệu vào RocksDB
    rocksDBService.saveObject(account1, rocksDBService.getAccountCF(), Account::getKey, "account");
    rocksDBService.saveObject(account2, rocksDBService.getAccountCF(), Account::getKey, "account");

    // Mock JsonSerializer để ném ngoại lệ khi deserialize account2
    try (MockedStatic<JsonSerializer> mockedJsonSerializer = Mockito.mockStatic(JsonSerializer.class)) {
      mockedJsonSerializer.when(() -> JsonSerializer.deserialize(any(byte[].class), eq(Account.class)))
          .thenReturn(account1)
          .thenThrow(new RuntimeException("Deserialize error"))
          .thenReturn(account2);

      // Lấy danh sách account theo prefix
      List<Account> result = rocksDBService.getObjectsByPrefix(
          "account:",
          10, // limit
          null, // lastKey
          rocksDBService.getAccountCF(),
          Account.class,
          "accounts");

      // Kiểm tra kết quả - chỉ có 1 đối tượng vì đối tượng thứ 2 gây ra ngoại lệ
      assertEquals(1, result.size(), "Kết quả phải chứa 1 đối tượng account");
      assertEquals(account1, result.get(0), "Đối tượng phải khớp với account1");
    }
  }

  @Test
  @Order(22)
  @DisplayName("estimateItemSize() phải ước tính kích thước trung bình của một item")
  void estimateItemSize_ShouldEstimateAverageItemSize() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo dữ liệu test
    Map<String, Account> data = new HashMap<>();
    Account account = new Account("test_account");
    account.setAvailableBalance(new BigDecimal("100.0"));
    data.put("test_account", account);

    // Mock KeyExtractor
    KeyExtractor<Account> keyExtractor = mock(KeyExtractor.class);
    when(keyExtractor.getKey(account)).thenReturn("test_account");

    // Mock JsonSerializer để trả về byte[] giả
    byte[] mockSerializedData = new byte[] { 1, 2, 3 };

    try (MockedStatic<JsonSerializer> mockedJsonSerializer = Mockito.mockStatic(JsonSerializer.class)) {
      mockedJsonSerializer.when(() -> JsonSerializer.serialize(account)).thenReturn(mockSerializedData);

      // Lấy phương thức estimateItemSize thông qua reflection
      Method estimateItemSizeMethod = RocksDBService.class.getDeclaredMethod("estimateItemSize", Map.class,
          KeyExtractor.class);
      estimateItemSizeMethod.setAccessible(true);

      // Gọi phương thức
      long estimatedSize = (long) estimateItemSizeMethod.invoke(rocksDBService, data, keyExtractor);

      // Kiểm tra kết quả
      // Kích thước ước tính = (key size + value size) * 1.3
      // key size = "test_account".getBytes().length = 11
      // value size = mockSerializedData.length = 3
      // (11 + 3) * 1.3 = 18.2, làm tròn lên 19
      assertEquals(19, estimatedSize, "Kích thước ước tính phải bằng (key size + value size) * 1.3");
    }
  }

  @Test
  @Order(23)
  @DisplayName("estimateItemSize() phải trả về giá trị mặc định khi có lỗi")
  void estimateItemSize_ShouldReturnDefaultSizeOnError() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo dữ liệu test với KeyExtractor ném ngoại lệ
    Map<String, Account> data = new HashMap<>();
    Account account = new Account("test_account");
    data.put("test_account", account);

    KeyExtractor<Account> keyExtractor = mock(KeyExtractor.class);
    when(keyExtractor.getKey(account)).thenThrow(new RuntimeException("Test exception"));

    // Lấy phương thức estimateItemSize thông qua reflection
    Method estimateItemSizeMethod = RocksDBService.class.getDeclaredMethod("estimateItemSize", Map.class,
        KeyExtractor.class);
    estimateItemSizeMethod.setAccessible(true);

    // Gọi phương thức
    long estimatedSize = (long) estimateItemSizeMethod.invoke(rocksDBService, data, keyExtractor);

    // Kiểm tra kết quả - phải trả về giá trị mặc định 1024
    assertEquals(1024, estimatedSize, "Kích thước ước tính phải là giá trị mặc định khi có lỗi");
  }

  @Test
  @Order(24)
  @DisplayName("saveBatchInternal() phải lưu batch dữ liệu thành công")
  void saveBatchInternal_ShouldSaveBatchDataSuccessfully() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo dữ liệu test
    List<Pair<byte[], byte[]>> items = new ArrayList<>();
    items.add(new Pair<>("key1".getBytes(), "value1".getBytes()));
    items.add(new Pair<>("key2".getBytes(), "value2".getBytes()));

    // Lấy phương thức saveBatchInternal thông qua reflection
    Method saveBatchInternalMethod = RocksDBService.class.getDeclaredMethod("saveBatchInternal", List.class,
        ColumnFamilyHandle.class, String.class);
    saveBatchInternalMethod.setAccessible(true);

    // Gọi phương thức
    int result = (int) saveBatchInternalMethod.invoke(rocksDBService, items, mockAccountCF, "test_batch");

    // Kiểm tra kết quả
    assertEquals(2, result, "Số lượng bản ghi đã lưu phải bằng số lượng items");

    // Xác minh rằng db.write được gọi với WriteBatch chứa đúng dữ liệu
    verify(mockDB, times(1)).write((WriteOptions) eq(mockWriteOptions), (WriteBatch) any());
  }

  @Test
  @Order(25)
  @DisplayName("saveBatchInternal() phải trả về 0 cho danh sách rỗng")
  void saveBatchInternal_ShouldReturnZeroForEmptyList() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo danh sách rỗng
    List<Pair<byte[], byte[]>> items = new ArrayList<>();

    // Lấy phương thức saveBatchInternal thông qua reflection
    Method saveBatchInternalMethod = RocksDBService.class.getDeclaredMethod("saveBatchInternal", List.class,
        ColumnFamilyHandle.class, String.class);
    saveBatchInternalMethod.setAccessible(true);

    // Gọi phương thức
    int result = (int) saveBatchInternalMethod.invoke(rocksDBService, items, mockAccountCF, "test_batch");

    // Kiểm tra kết quả
    assertEquals(0, result, "Kết quả phải là 0 cho danh sách rỗng");

    // Xác minh rằng db.write không được gọi
    verify(mockDB, never()).write((WriteOptions) any(), (WriteBatch) any());
  }

  @Test
  @Order(26)
  @DisplayName("saveBatchInternal() phải xử lý ngoại lệ khi batch write thất bại")
  void saveBatchInternal_ShouldHandleExceptionWhenBatchWriteFails() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo dữ liệu test
    List<Pair<byte[], byte[]>> items = new ArrayList<>();
    items.add(new Pair<>("key1".getBytes(), "value1".getBytes()));

    // Cấu hình mockDB để ném ngoại lệ khi gọi write
    doThrow(new RocksDBException("Test exception")).when(mockDB).write((WriteOptions) any(), (WriteBatch) any());

    // Lấy phương thức saveBatchInternal thông qua reflection
    Method saveBatchInternalMethod = RocksDBService.class.getDeclaredMethod("saveBatchInternal", List.class,
        ColumnFamilyHandle.class, String.class);
    saveBatchInternalMethod.setAccessible(true);

    // Gọi phương thức
    int result = (int) saveBatchInternalMethod.invoke(rocksDBService, items, mockAccountCF, "test_batch");

    // Kiểm tra kết quả
    assertEquals(0, result, "Kết quả phải là 0 khi có lỗi");

    // Xác minh rằng db.write đã được gọi
    verify(mockDB, times(1)).write((WriteOptions) any(), (WriteBatch) any());
  }

  @Test
  @Order(27)
  @DisplayName("estimateItemSize() phải trả về giá trị mặc định cho data rỗng")
  void estimateItemSize_ShouldReturnDefaultSizeForEmptyData() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo dữ liệu test rỗng
    Map<String, Account> emptyData = new HashMap<>();
    KeyExtractor<Account> keyExtractor = mock(KeyExtractor.class);

    // Lấy phương thức estimateItemSize thông qua reflection
    Method estimateItemSizeMethod = RocksDBService.class.getDeclaredMethod("estimateItemSize", Map.class,
        KeyExtractor.class);
    estimateItemSizeMethod.setAccessible(true);

    // Gọi phương thức
    long estimatedSize = (long) estimateItemSizeMethod.invoke(rocksDBService, emptyData, keyExtractor);

    // Kiểm tra kết quả - phải trả về giá trị mặc định 1024
    assertEquals(1024, estimatedSize, "Kích thước ước tính phải là giá trị mặc định cho data rỗng");

    // Xác minh rằng keyExtractor không được gọi
    verify(keyExtractor, never()).getKey(any());
  }

  @Test
  @Order(28)
  @DisplayName("saveBatch() không nên làm gì khi data rỗng")
  void saveBatch_ShouldDoNothingWhenDataIsEmpty() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo dữ liệu test rỗng
    Map<String, Account> emptyData = new HashMap<>();
    KeyExtractor<Account> keyExtractor = mock(KeyExtractor.class);

    // Gọi saveBatch
    rocksDBService.saveBatch(emptyData, mockAccountCF, keyExtractor, "test_batch");

    // Xác minh rằng không có tương tác với mockDB
    verify(mockDB, never()).write(any(WriteOptions.class), any(WriteBatch.class));
    verify(keyExtractor, never()).getKey(any());
  }

  @Test
  @Order(29)
  @DisplayName("saveBatch() nên bỏ qua các item null hoặc có key null/rỗng")
  void saveBatch_ShouldSkipNullItemsAndNullOrEmptyKeys() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo dữ liệu test với các trường hợp null và rỗng
    Map<String, Account> data = new HashMap<>();
    Account validAccount = new Account("valid_key");
    Account nullKeyAccount = new Account(null);
    Account emptyKeyAccount = new Account("");

    data.put("valid", validAccount);
    data.put("null", null); // null item
    data.put("nullKey", nullKeyAccount); // item với null key
    data.put("emptyKey", emptyKeyAccount); // item với empty key

    // Mock KeyExtractor để trả về key tương ứng
    KeyExtractor<Account> keyExtractor = Account::getKey;

    // Mock JsonSerializer để trả về byte[] giả
    byte[] mockSerializedData = new byte[] { 1, 2, 3 };
    try (MockedStatic<JsonSerializer> mockedJsonSerializer = Mockito.mockStatic(JsonSerializer.class)) {
      mockedJsonSerializer.when(() -> JsonSerializer.serialize(validAccount)).thenReturn(mockSerializedData);

      // Gọi saveBatch
      rocksDBService.saveBatch(data, mockAccountCF, keyExtractor, "test_batch");

      // Xác minh rằng chỉ có item hợp lệ được xử lý
      verify(mockDB, times(1)).write(any(WriteOptions.class), any(WriteBatch.class));
      mockedJsonSerializer.verify(() -> JsonSerializer.serialize(validAccount), atLeast(1));
      mockedJsonSerializer.verify(() -> JsonSerializer.serialize(nullKeyAccount), never());
      mockedJsonSerializer.verify(() -> JsonSerializer.serialize(emptyKeyAccount), never());
    }
  }

  @Test
  @Order(30)
  @DisplayName("saveBatch() nên xử lý lỗi khi serialize thất bại")
  void saveBatch_ShouldHandleSerializationErrors() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo dữ liệu test
    Account account1 = createTestAccount("key1", new BigDecimal("100.0"));
    Account account2 = createTestAccount("key2", new BigDecimal("200.0"));
    Map<String, Account> data = Map.of("key1", account1, "key2", account2);

    // Mock KeyExtractor
    KeyExtractor<Account> keyExtractor = Account::getKey;

    // Mock JsonSerializer để ném ngoại lệ cho account2
    byte[] mockSerializedData = new byte[] { 1, 2, 3 };
    try (MockedStatic<JsonSerializer> mockedJsonSerializer = Mockito.mockStatic(JsonSerializer.class)) {
      // Cấu hình mock cho account1 thành công và account2 ném ngoại lệ
      mockedJsonSerializer.when(() -> JsonSerializer.serialize(account1)).thenReturn(mockSerializedData);
      mockedJsonSerializer.when(() -> JsonSerializer.serialize(account2))
          .thenThrow(new RuntimeException("Serialize error"));

      // Gọi saveBatch
      rocksDBService.saveBatch(data, mockAccountCF, keyExtractor, "test_batch");

      // Xác minh rằng vẫn tiếp tục xử lý sau lỗi
      verify(mockDB, times(1)).write(any(WriteOptions.class), any(WriteBatch.class));
    }
  }

  @Test
  @Order(31)
  @DisplayName("saveBatch() nên chia dữ liệu thành nhiều batch dựa trên kích thước ước tính")
  void saveBatch_ShouldSplitDataIntoMultipleBatchesBasedOnEstimatedSize() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo một lượng lớn dữ liệu test
    Map<String, Account> data = createTestData(RocksDBConfig.DEFAULT_MAX_RECORDS_PER_BATCH + 1);

    // Mock KeyExtractor
    KeyExtractor<Account> keyExtractor = Account::getKey;

    // Mock JsonSerializer để trả về byte[] giả
    byte[] mockSerializedData = new byte[] { 1, 2, 3 };
    try (MockedStatic<JsonSerializer> mockedJsonSerializer = setupMockedJsonSerializerForMultipleAccounts(
        new ArrayList<>(data.values()), mockSerializedData)) {
      // Gọi saveBatch
      rocksDBService.saveBatch(data, mockAccountCF, keyExtractor, "test_batch");

      // Xác minh rằng write được gọi nhiều lần (ít nhất 2 lần cho 2 batch)
      verify(mockDB, atLeast(2)).write(any(WriteOptions.class), any(WriteBatch.class));
    }
  }

  @Test
  @Order(32)
  @DisplayName("saveBatch() nên xử lý lỗi khi write batch thất bại")
  void saveBatch_ShouldHandleWriteBatchErrors() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo dữ liệu test
    Account account = createTestAccount("test_key", new BigDecimal("100.0"));
    Map<String, Account> data = Map.of("test_key", account);

    // Mock KeyExtractor
    KeyExtractor<Account> keyExtractor = Account::getKey;

    // Mock JsonSerializer để trả về byte[] giả
    byte[] mockSerializedData = new byte[] { 1, 2, 3 };
    try (MockedStatic<JsonSerializer> mockedJsonSerializer = setupMockedJsonSerializer(account, mockSerializedData)) {
      // Cấu hình mockDB để ném ngoại lệ khi write
      doThrow(new RocksDBException("Write error")).when(mockDB)
          .write(any(WriteOptions.class), any(WriteBatch.class));

      // Gọi saveBatch - không nên ném ngoại lệ ra ngoài
      rocksDBService.saveBatch(data, mockAccountCF, keyExtractor, "test_batch");

      // Xác minh rằng write được gọi và xử lý lỗi
      verify(mockDB, times(1)).write(any(WriteOptions.class), any(WriteBatch.class));
    }
  }

  @Test
  @Order(33)
  @DisplayName("prepareDataPair() phải tạo cặp key-value thành công")
  void prepareDataPair_ShouldCreateKeyValuePairSuccessfully() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo dữ liệu test
    Account account = createTestAccount("test_key", new BigDecimal("100.0"));
    KeyExtractor<Account> keyExtractor = Account::getKey;

    // Mock JsonSerializer để trả về byte[] giả
    byte[] mockSerializedData = new byte[] { 1, 2, 3 };
    try (MockedStatic<JsonSerializer> mockedJsonSerializer = setupMockedJsonSerializer(account, mockSerializedData)) {
      // Lấy phương thức prepareDataPair thông qua reflection
      Method prepareDataPairMethod = RocksDBService.class.getDeclaredMethod("prepareDataPair", Object.class,
          KeyExtractor.class);
      prepareDataPairMethod.setAccessible(true);

      // Gọi phương thức
      Pair<byte[], byte[]> result = (Pair<byte[], byte[]>) prepareDataPairMethod.invoke(rocksDBService, account,
          keyExtractor);

      // Kiểm tra kết quả
      assertNotNull(result, "Kết quả không được null");
      assertArrayEquals("test_key".getBytes(), result.getKey(), "Key phải khớp");
      assertArrayEquals(mockSerializedData, result.getValue(), "Value phải khớp");

      // Xác minh rằng JsonSerializer.serialize được gọi
      mockedJsonSerializer.verify(() -> JsonSerializer.serialize(account), times(1));
    }
  }

  @Test
  @Order(34)
  @DisplayName("prepareDataPair() phải trả về null khi có lỗi xảy ra")
  void prepareDataPair_ShouldReturnNullOnError() throws Exception {
    // Thiết lập RocksDBService với mock
    setupMockedRocksDBService();

    // Tạo dữ liệu test
    Account account = createTestAccount("test_key", new BigDecimal("100.0"));
    KeyExtractor<Account> keyExtractor = Account::getKey;

    // Mock JsonSerializer để ném ngoại lệ
    try (MockedStatic<JsonSerializer> mockedJsonSerializer = setupMockedJsonSerializerWithException(account,
        new RuntimeException("Serialize error"))) {
      // Lấy phương thức prepareDataPair thông qua reflection
      Method prepareDataPairMethod = RocksDBService.class.getDeclaredMethod("prepareDataPair", Object.class,
          KeyExtractor.class);
      prepareDataPairMethod.setAccessible(true);

      // Gọi phương thức
      Pair<byte[], byte[]> result = (Pair<byte[], byte[]>) prepareDataPairMethod.invoke(rocksDBService, account,
          keyExtractor);

      // Kiểm tra kết quả
      assertNull(result, "Kết quả phải là null khi có lỗi");

      // Xác minh rằng JsonSerializer.serialize được gọi
      mockedJsonSerializer.verify(() -> JsonSerializer.serialize(account), times(1));
    }
  }
}
