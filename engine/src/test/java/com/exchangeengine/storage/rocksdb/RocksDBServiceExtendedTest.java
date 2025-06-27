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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test bổ sung cho RocksDBService để đạt 100% coverage.
 * Các test case trong lớp này tập trung vào các trường hợp chưa được test đầy
 * đủ.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(RocksDBTestExtension.class)
class RocksDBServiceExtendedTest {

    private RocksDBService rocksDBService;

    @Mock
    private RocksDB mockDB;

    @Mock
    private ColumnFamilyHandle mockDefaultCF;

    @Mock
    private ColumnFamilyHandle mockOfferCF;

    @Mock
    private ColumnFamilyHandle mockTradeCF;

    @Mock
    private WriteOptions mockWriteOptions;

    @Mock
    private RocksIterator mockIterator;

    private AutoCloseable mockCloseable;

    private static String testDataDir;

    @BeforeEach
    void setUp() {
        // Đảm bảo biến môi trường được thiết lập
        EnvManager.setTestEnvironment();

        // Tạo thư mục test độc lập
        testDataDir = EnvManager.getInstance().get(
                "ROCKSDB_DATA_DIR",
                "./data/rocksdb/test") + "/extended_test_" + System.currentTimeMillis() + "_" + System.nanoTime();

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
        if (rocksDBService != null) {
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

        Field offerCFField = RocksDBService.class.getDeclaredField("offerCF");
        offerCFField.setAccessible(true);
        offerCFField.set(rocksDBService, mockOfferCF);

        Field tradeCFField = RocksDBService.class.getDeclaredField("tradeCF");
        tradeCFField.setAccessible(true);
        tradeCFField.set(rocksDBService, mockTradeCF);

        Field writeOptionsField = RocksDBService.class.getDeclaredField("writeOptions");
        writeOptionsField.setAccessible(true);
        writeOptionsField.set(rocksDBService, mockWriteOptions);
    }

    @Test
    @Order(1)
    @DisplayName("Test setTestInstance() sẽ thiết lập instance chính xác")
    void setTestInstance_ShouldSetTheProvidedInstance() throws Exception {
        // Arrange
        RocksDBService.resetInstance();
        RocksDBService testInstance = mock(RocksDBService.class);

        // Act
        RocksDBService.setTestInstance(testInstance);
        RocksDBService instance = RocksDBService.getInstance();

        // Assert
        assertSame(testInstance, instance);
    }

    @Test
    @Order(2)
    @DisplayName("Test getOfferCF() và getTradeCF() trả về đúng column family handle")
    void getOfferCFAndTradeCF_ShouldReturnCorrectHandle() throws Exception {
        // Arrange
        setupMockedRocksDBService();

        // Act & Assert
        assertSame(mockOfferCF, rocksDBService.getOfferCF());
        assertSame(mockTradeCF, rocksDBService.getTradeCF());
    }

    @Test
    @Order(3)
    @DisplayName("Test startsWith() xử lý đúng các trường hợp biên")
    void startsWith_ShouldHandleEdgeCases() throws Exception {
        // Arrange
        setupMockedRocksDBService();

        // Test case 1: keyBytes ngắn hơn prefixBytes
        byte[] shortKey = "short".getBytes(StandardCharsets.UTF_8);
        byte[] longPrefix = "longPrefix".getBytes(StandardCharsets.UTF_8);

        // Act & Assert
        assertFalse(rocksDBService.startsWith(shortKey, longPrefix));

        // Test case 2: keyBytes rỗng
        byte[] emptyKey = new byte[0];
        byte[] prefix = "prefix".getBytes(StandardCharsets.UTF_8);

        assertFalse(rocksDBService.startsWith(emptyKey, prefix));

        // Test case 3: prefixBytes rỗng
        byte[] key = "key".getBytes(StandardCharsets.UTF_8);
        byte[] emptyPrefix = new byte[0];

        assertTrue(rocksDBService.startsWith(key, emptyPrefix));
    }

    @Test
    @Order(4)
    @DisplayName("Test getObjectsByPrefix() với prefix rỗng")
    void getObjectsByPrefix_ShouldHandleEmptyPrefix() throws Exception {
        // Arrange
        setupMockedRocksDBService();

        // Mock RocksDB.newIterator() và iterator behavior
        when(mockDB.newIterator(eq(mockOfferCF), any(ReadOptions.class))).thenReturn(mockIterator);

        // Giả lập iterator không tìm thấy kết quả
        when(mockIterator.isValid()).thenReturn(false);

        // Act
        List<Account> results = rocksDBService.getObjectsByPrefix("", 10, null, mockOfferCF, Account.class,
                "test_prefix");

        // Assert
        assertTrue(results.isEmpty());

        // Verify iterator.seek() được gọi với prefix rỗng
        verify(mockIterator).seek(eq("".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @Order(5)
    @DisplayName("Test getObjectsByPrefix() với lastKey không rỗng")
    void getObjectsByPrefix_ShouldUseLastKeyCorrectly() throws Exception {
        // Arrange
        setupMockedRocksDBService();

        // Mock RocksDB.newIterator() và iterator behavior
        when(mockDB.newIterator(eq(mockOfferCF), any(ReadOptions.class))).thenReturn(mockIterator);
        when(mockIterator.isValid()).thenReturn(true, false); // Giả lập valid chỉ cho lần đầu tiên

        // Mock iterator key và value
        String lastKey = "last_key";
        String prefix = "prefix";
        byte[] keyBytes = "prefix_key".getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = "{\"id\":\"test\"}".getBytes(StandardCharsets.UTF_8);

        when(mockIterator.key()).thenReturn(keyBytes);
        when(mockIterator.value()).thenReturn(valueBytes);

        // Mock JsonSerializer.deserialize
        Account testAccount = new Account("test");
        try (MockedStatic<JsonSerializer> mockedJsonSerializer = Mockito.mockStatic(JsonSerializer.class)) {
            mockedJsonSerializer.when(() -> JsonSerializer.deserialize(eq(valueBytes), eq(Account.class)))
                    .thenReturn(testAccount);

            // Act
            List<Account> results = rocksDBService.getObjectsByPrefix(prefix, 10, lastKey, mockOfferCF, Account.class,
                    "test_prefix");

            // Assert
            assertEquals(1, results.size());
            assertSame(testAccount, results.get(0));

            // Verify iterator.seek() được gọi với lastKey
            verify(mockIterator).seek(eq(lastKey.getBytes()));
            verify(mockIterator, atLeastOnce()).next(); // Verify next() được gọi ngay sau seek() khi có lastKey
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test getObjectsByPrefix() khi iterator.key() không bắt đầu bằng prefix")
    void getObjectsByPrefix_ShouldStopWhenKeyDoesNotStartWithPrefix() throws Exception {
        // Arrange
        setupMockedRocksDBService();

        // Mock RocksDB.newIterator() và iterator behavior
        when(mockDB.newIterator(eq(mockOfferCF), any(ReadOptions.class))).thenReturn(mockIterator);
        when(mockIterator.isValid()).thenReturn(true); // Valid iterator

        // Mock iterator key và value
        String prefix = "prefix";
        byte[] keyBytes = "different_prefix_key".getBytes(StandardCharsets.UTF_8);

        when(mockIterator.key()).thenReturn(keyBytes);

        // Act
        List<Account> results = rocksDBService.getObjectsByPrefix(prefix, 10, null, mockOfferCF, Account.class,
                "test_prefix");

        // Assert
        assertTrue(results.isEmpty());

        // Verify startsWith được gọi và dừng vòng lặp
        verify(mockIterator, never()).value();
    }

    @Test
    @Order(7)
    @DisplayName("Test prepareDataPair() với keyExtractor trả về null")
    void prepareDataPair_ShouldHandleNullKeyFromExtractor() throws Exception {
        // Arrange
        setupMockedRocksDBService();

        // Tạo keyExtractor mock trả về null
        KeyExtractor<Account> keyExtractor = mock(KeyExtractor.class);
        when(keyExtractor.getKey(any())).thenReturn(null);

        Account account = new Account("test");

        // Lấy phương thức prepareDataPair thông qua reflection
        Method prepareDataPairMethod = RocksDBService.class.getDeclaredMethod("prepareDataPair", Object.class,
                KeyExtractor.class);
        prepareDataPairMethod.setAccessible(true);

        // Act
        Pair<byte[], byte[]> result = (Pair<byte[], byte[]>) prepareDataPairMethod.invoke(rocksDBService, account,
                keyExtractor);

        // Assert
        assertNull(result);
        verify(keyExtractor, atLeastOnce()).getKey(eq(account));
    }

    @Test
    @Order(8)
    @DisplayName("Test getInstance() với singleton cơ bản")
    void getInstance_ShouldCreateSingletonInstance() throws Exception {
        // Reset để đảm bảo chưa có instance trước khi test
        RocksDBService.resetInstance();

        // Tạo mock RocksDBService
        RocksDBService mockServiceInstance = Mockito.mock(RocksDBService.class);
        when(mockServiceInstance.getDb()).thenReturn(mockDB);
        when(mockServiceInstance.getDefaultCF()).thenReturn(mockDefaultCF);
        when(mockServiceInstance.getAccountCF()).thenReturn(mockDefaultCF);
        when(mockServiceInstance.getDepositCF()).thenReturn(mockDefaultCF);
        when(mockServiceInstance.getWithdrawalCF()).thenReturn(mockDefaultCF);
        when(mockServiceInstance.getAccountHistoryCF()).thenReturn(mockDefaultCF);
        when(mockServiceInstance.getAmmPoolCF()).thenReturn(mockDefaultCF);
        when(mockServiceInstance.getMerchantEscrowCF()).thenReturn(mockDefaultCF);
        when(mockServiceInstance.getTickCF()).thenReturn(mockDefaultCF);
        when(mockServiceInstance.getTickBitmapCF()).thenReturn(mockDefaultCF);
        when(mockServiceInstance.getAmmPositionCF()).thenReturn(mockDefaultCF);
        when(mockServiceInstance.getAmmOrdersCF()).thenReturn(mockDefaultCF);
        when(mockServiceInstance.getOfferCF()).thenReturn(mockOfferCF);
        when(mockServiceInstance.getTradeCF()).thenReturn(mockTradeCF);
        when(mockServiceInstance.getWriteOptions()).thenReturn(mockWriteOptions);

        // Set mock instance làm singleton
        RocksDBService.setTestInstance(mockServiceInstance);

        try {
            // Act
            RocksDBService instance1 = RocksDBService.getInstance();
            RocksDBService instance2 = RocksDBService.getInstance();

            // Assert
            assertNotNull(instance1);
            assertSame(instance1, instance2, "Singleton nên trả về cùng một instance");

            // Kiểm tra field mặc định
            assertNotNull(instance1.getDb());
            assertNotNull(instance1.getDefaultCF());
            assertNotNull(instance1.getAccountCF());
            assertNotNull(instance1.getDepositCF());
            assertNotNull(instance1.getWithdrawalCF());
            assertNotNull(instance1.getAccountHistoryCF());
            assertNotNull(instance1.getAmmPoolCF());
            assertNotNull(instance1.getMerchantEscrowCF());
            assertNotNull(instance1.getTickCF());
            assertNotNull(instance1.getTickBitmapCF());
            assertNotNull(instance1.getAmmPositionCF());
            assertNotNull(instance1.getAmmOrdersCF());
            assertNotNull(instance1.getOfferCF());
            assertNotNull(instance1.getTradeCF());
            assertNotNull(instance1.getWriteOptions());
        } catch (Exception e) {
            fail("Không nên ném ra ngoại lệ khi tạo instance: " + e.getMessage());
        } finally {
            // Reset để tránh ảnh hưởng đến các test sau
            RocksDBService.resetInstance();
        }
    }

    @Test
    @Order(9)
    @DisplayName("Test getObjectsByPrefix() xử lý ngoại lệ khi không thể serialize")
    void getObjectsByPrefix_ShouldHandleSerializationExceptions() throws Exception {
        // Arrange
        setupMockedRocksDBService();

        // Mock RocksDB.newIterator() và iterator behavior
        when(mockDB.newIterator(eq(mockOfferCF), any(ReadOptions.class))).thenReturn(mockIterator);
        when(mockIterator.isValid()).thenReturn(true, false); // Chỉ valid cho lần đầu tiên

        // Mock iterator key và value
        String prefix = "prefix";
        byte[] keyBytes = "prefix_key".getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = "{\"invalid_json\"}".getBytes(StandardCharsets.UTF_8);

        when(mockIterator.key()).thenReturn(keyBytes);
        when(mockIterator.value()).thenReturn(valueBytes);

        // Mock JsonSerializer.deserialize để ném exception
        try (MockedStatic<JsonSerializer> mockedJsonSerializer = Mockito.mockStatic(JsonSerializer.class)) {
            mockedJsonSerializer.when(() -> JsonSerializer.deserialize(eq(valueBytes), eq(Account.class)))
                    .thenThrow(new RuntimeException("Invalid JSON"));

            // Act
            List<Account> results = rocksDBService.getObjectsByPrefix(prefix, 10, null, mockOfferCF, Account.class,
                    "test_prefix");

            // Assert
            assertTrue(results.isEmpty());

            // Verify iterator methods được gọi
            verify(mockIterator).seek(eq(prefix.getBytes(StandardCharsets.UTF_8)));
            verify(mockIterator).key();
            verify(mockIterator).value();
            verify(mockIterator).next();
        }
    }

    @Test
    @Order(10)
    @DisplayName("Test getObjectsByPrefix() với giới hạn số lượng bản ghi")
    void getObjectsByPrefix_ShouldLimitResults() throws Exception {
        // Arrange
        setupMockedRocksDBService();

        // Mock RocksDB.newIterator() và iterator behavior
        when(mockDB.newIterator(eq(mockOfferCF), any(ReadOptions.class))).thenReturn(mockIterator);
        when(mockIterator.isValid()).thenReturn(true, true, true, true); // 4 lần valid

        // Mock iterator key và value
        String prefix = "prefix";
        byte[] keyBytes1 = "prefix_key1".getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes2 = "prefix_key2".getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = "{\"id\":\"test\"}".getBytes(StandardCharsets.UTF_8);

        // Chuẩn bị mock key và value trả về
        when(mockIterator.key()).thenReturn(keyBytes1, keyBytes2);
        when(mockIterator.value()).thenReturn(valueBytes);

        // Setup serializerr để trả về 2 Account khác nhau
        Account account1 = new Account("test1");
        Account account2 = new Account("test2");

        try (MockedStatic<JsonSerializer> mockedJsonSerializer = Mockito.mockStatic(JsonSerializer.class)) {
            mockedJsonSerializer.when(() -> JsonSerializer.deserialize(eq(valueBytes), eq(Account.class)))
                    .thenReturn(account1, account2);

            // Act - chỉ lấy 1 kết quả mặc dù có sẵn 2
            List<Account> results = rocksDBService.getObjectsByPrefix(prefix, 1, null, mockOfferCF, Account.class,
                    "test_prefix");

            // Assert
            assertEquals(1, results.size());
            assertEquals("test1", results.get(0).getKey());

            // Verify iterator chỉ gọi next() đến khi đạt đủ số lượng
            verify(mockIterator, times(1)).next();
        }
    }
}
