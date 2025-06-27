package com.exchangeengine.storage.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.exchangeengine.factory.CoinWithdrawalFactory;
import com.exchangeengine.model.CoinWithdrawal;
import com.exchangeengine.storage.rocksdb.WithdrawalRocksDB;

class WithdrawalCacheTest {
  private WithdrawalCache withdrawalCache;
  private AutoCloseable closeable;

  @Mock
  private WithdrawalRocksDB mockWithdrawalRocksDB;

  private MockedStatic<WithdrawalRocksDB> mockedWithdrawalRocksDBStatic;

  @BeforeEach
  void setUp() throws Exception {
    closeable = MockitoAnnotations.openMocks(this);

    // Cấu hình mock
    mockedWithdrawalRocksDBStatic = Mockito.mockStatic(WithdrawalRocksDB.class);
    mockedWithdrawalRocksDBStatic.when(WithdrawalRocksDB::getInstance).thenReturn(mockWithdrawalRocksDB);

    // Mock getAllWithdrawals để trả về danh sách rỗng
    when(mockWithdrawalRocksDB.getAllWithdrawals()).thenReturn(new ArrayList<>());

    // Reset WithdrawalCache instance
    resetSingleton(WithdrawalCache.class, "instance");

    // Khởi tạo WithdrawalCache
    withdrawalCache = WithdrawalCache.getInstance();
    withdrawalCache.initializeWithdrawalCache();

    // Reset số lần gọi mock
    clearInvocations(mockWithdrawalRocksDB);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (mockedWithdrawalRocksDBStatic != null) {
      mockedWithdrawalRocksDBStatic.close();
    }
    resetSingleton(WithdrawalCache.class, "instance");
    closeable.close();

    // Reset RocksDB instance
    Field rockDBField = WithdrawalRocksDB.class.getDeclaredField("instance");
    rockDBField.setAccessible(true);
    rockDBField.set(null, null);
  }

  private void resetSingleton(Class<?> clazz, String fieldName) throws Exception {
    Field instance = clazz.getDeclaredField(fieldName);
    instance.setAccessible(true);
    instance.set(null, null);
  }

  @Test
  void testGetInstance_ReturnsSameInstance() {
    WithdrawalCache instance1 = WithdrawalCache.getInstance();
    WithdrawalCache instance2 = WithdrawalCache.getInstance();
    assertSame(instance1, instance2, "getInstance should return the same instance");
  }

  @Test
  void testGetWithdrawal_NotExists() {
    String identifier = "non_existent_id";
    Optional<CoinWithdrawal> result = withdrawalCache.getWithdrawal(identifier);
    assertFalse(result.isPresent(), "Should return empty Optional for non-existent withdrawal");
  }

  @Test
  void testGetWithdrawal_Exists() {
    CoinWithdrawal withdrawal = CoinWithdrawalFactory.create("test_account", "test_id", new BigDecimal("1.0"));
    withdrawalCache.updateCoinWithdrawal(withdrawal);

    Optional<CoinWithdrawal> result = withdrawalCache.getWithdrawal(withdrawal.getIdentifier());
    assertTrue(result.isPresent(), "Should return withdrawal when it exists");
    assertEquals(withdrawal, result.get(), "Should return the correct withdrawal");
  }

  @Test
  void testUpdateCoinWithdrawal() {
    CoinWithdrawal withdrawal = CoinWithdrawalFactory.create("test_account", "test_id", new BigDecimal("1.0"));
    withdrawalCache.updateCoinWithdrawal(withdrawal);

    Optional<CoinWithdrawal> result = withdrawalCache.getWithdrawal(withdrawal.getIdentifier());
    assertTrue(result.isPresent(), "Should find the updated withdrawal");
    assertEquals(withdrawal, result.get(), "Should return the updated withdrawal");
  }

  @Test
  void testAddWithdrawalToBatch() throws Exception {
    // Test case 1: Add withdrawal with null identifier - should be ignored
    CoinWithdrawal nullIdWithdrawal = CoinWithdrawalFactory.create("test_account", null, new BigDecimal("1.0"));
    withdrawalCache.addWithdrawalToBatch(nullIdWithdrawal);

    // Test case 2: Add withdrawal with empty identifier - should be ignored
    CoinWithdrawal emptyIdWithdrawal = CoinWithdrawalFactory.create("test_account", "", new BigDecimal("1.0"));
    withdrawalCache.addWithdrawalToBatch(emptyIdWithdrawal);

    // Test case 3: Add valid BTC withdrawal
    CoinWithdrawal btcWithdrawal = CoinWithdrawalFactory.createWithCoin(
        "test_account",
        "btc_withdrawal_1",
        new BigDecimal("1.0"),
        "btc");
    btcWithdrawal.setStatus("pending");
    long btcTime = System.currentTimeMillis();
    btcWithdrawal.setUpdatedAt(btcTime);
    withdrawalCache.addWithdrawalToBatch(btcWithdrawal);

    // Test case 4: Add valid ETH withdrawal
    CoinWithdrawal ethWithdrawal = CoinWithdrawalFactory.createWithCoin(
        "test_account",
        "eth_withdrawal_1",
        new BigDecimal("10.0"),
        "eth");
    ethWithdrawal.setStatus("pending");
    long ethTime = btcTime + 1000;
    ethWithdrawal.setUpdatedAt(ethTime);
    withdrawalCache.addWithdrawalToBatch(ethWithdrawal);

    // Test case 5: Add newer version of BTC withdrawal
    CoinWithdrawal newerBtcWithdrawal = CoinWithdrawalFactory.createWithCoin(
        "test_account",
        "btc_withdrawal_1",
        new BigDecimal("1.0"),
        "btc");
    newerBtcWithdrawal.setStatus("processing");
    newerBtcWithdrawal.setUpdatedAt(btcTime + 2000);
    withdrawalCache.addWithdrawalToBatch(newerBtcWithdrawal);

    // Test case 6: Add older version of ETH withdrawal (should not replace newer
    // version)
    CoinWithdrawal olderEthWithdrawal = CoinWithdrawalFactory.createWithCoin(
        "test_account",
        "eth_withdrawal_1",
        new BigDecimal("10.0"),
        "eth");
    olderEthWithdrawal.setStatus("completed");
    olderEthWithdrawal.setUpdatedAt(btcTime);
    withdrawalCache.addWithdrawalToBatch(olderEthWithdrawal);

    // Truy cập trực tiếp vào map latestWithdrawals qua reflection
    Field latestWithdrawalsField = WithdrawalCache.class.getDeclaredField("latestWithdrawals");
    latestWithdrawalsField.setAccessible(true);
    Map<String, CoinWithdrawal> latestWithdrawals = (Map<String, CoinWithdrawal>) latestWithdrawalsField
        .get(withdrawalCache);

    // Kiểm tra trạng thái cuối cùng của cache
    assertEquals(2, latestWithdrawals.size(), "Should have exactly 2 withdrawals");

    // Verify BTC withdrawal
    CoinWithdrawal savedBtcWithdrawal = latestWithdrawals.get("btc_withdrawal_1");
    assertNotNull(savedBtcWithdrawal, "BTC withdrawal should exist");
    assertEquals("processing", savedBtcWithdrawal.getStatus(), "Should have updated status");
    assertEquals("btc", savedBtcWithdrawal.getCoin().toLowerCase(), "Should be BTC withdrawal");
    assertEquals(btcTime + 2000, savedBtcWithdrawal.getUpdatedAt(), "Should keep newer BTC withdrawal");

    // Verify ETH withdrawal
    CoinWithdrawal savedEthWithdrawal = latestWithdrawals.get("eth_withdrawal_1");
    assertNotNull(savedEthWithdrawal, "ETH withdrawal should exist");
    assertEquals("pending", savedEthWithdrawal.getStatus(), "Should keep original status");
    assertEquals("eth", savedEthWithdrawal.getCoin().toLowerCase(), "Should be ETH withdrawal");
    assertEquals(ethTime, savedEthWithdrawal.getUpdatedAt(), "Should keep ETH withdrawal timestamp");
  }

  @Test
  void testInitializeWithdrawalCache() throws Exception {
    // Tạo danh sách withdrawals để mock
    List<CoinWithdrawal> withdrawals = new ArrayList<>();

    // Thêm withdrawal hợp lệ
    CoinWithdrawal validWithdrawal1 = CoinWithdrawalFactory.create("test_account", "id1", new BigDecimal("1.0"));
    validWithdrawal1.setCoin("BTC");
    validWithdrawal1.setUpdatedAt(1000L);
    withdrawals.add(validWithdrawal1);

    // Thêm withdrawal hợp lệ thứ 2
    CoinWithdrawal validWithdrawal2 = CoinWithdrawalFactory.create("test_account", "id2", new BigDecimal("2.0"));
    validWithdrawal2.setCoin("ETH");
    validWithdrawal2.setUpdatedAt(2000L);
    withdrawals.add(validWithdrawal2);

    // Thêm withdrawal với identifier null (sẽ bị bỏ qua)
    CoinWithdrawal nullIdWithdrawal = CoinWithdrawalFactory.create("test_account", null, new BigDecimal("3.0"));
    withdrawals.add(nullIdWithdrawal);

    // Thêm withdrawal với identifier rỗng (sẽ bị bỏ qua)
    CoinWithdrawal emptyIdWithdrawal = CoinWithdrawalFactory.create("test_account", "", new BigDecimal("4.0"));
    withdrawals.add(emptyIdWithdrawal);

    // Configure mock
    when(mockWithdrawalRocksDB.getAllWithdrawals()).thenReturn(withdrawals);

    // Reset và khởi tạo cache
    resetSingleton(WithdrawalCache.class, "instance");
    withdrawalCache = WithdrawalCache.getInstance();

    // Thực hiện phương thức cần test
    withdrawalCache.initializeWithdrawalCache();

    // Kiểm tra
    // 1. Kiểm tra withdrawalCache
    Field withdrawalCacheField = WithdrawalCache.class.getDeclaredField("withdrawalCache");
    withdrawalCacheField.setAccessible(true);
    Map<String, CoinWithdrawal> cache = (Map<String, CoinWithdrawal>) withdrawalCacheField.get(withdrawalCache);

    // Chỉ nên có 2 withdrawals hợp lệ
    assertEquals(2, cache.size(), "Should only load valid withdrawals");
    assertTrue(cache.containsKey("id1"), "Should contain first valid withdrawal");
    assertTrue(cache.containsKey("id2"), "Should contain second valid withdrawal");

    // 2. Kiểm tra dữ liệu từng withdrawal
    CoinWithdrawal cached1 = cache.get("id1");
    assertEquals("btc", cached1.getCoin().toLowerCase(), "First withdrawal should have correct coin");
    assertEquals(0, cached1.getAmount().compareTo(new BigDecimal("1.0")),
        "First withdrawal should have correct amount");

    CoinWithdrawal cached2 = cache.get("id2");
    assertEquals("eth", cached2.getCoin().toLowerCase(), "Second withdrawal should have correct coin");
    assertEquals(0, cached2.getAmount().compareTo(new BigDecimal("2.0")),
        "Second withdrawal should have correct amount");

    // 3. Verify logger (nếu muốn)
    verify(mockWithdrawalRocksDB).getAllWithdrawals();
  }

  @Test
  void testInitializeWithdrawalCache_WithException() {
    when(mockWithdrawalRocksDB.getAllWithdrawals())
        .thenThrow(new RuntimeException("Test exception"));

    withdrawalCache.initializeWithdrawalCache();

    Optional<CoinWithdrawal> result = withdrawalCache.getWithdrawal("any_id");
    assertFalse(result.isPresent(), "Cache should be empty after initialization failure");
  }

  @Test
  void testFlushWithdrawalToDisk() throws Exception {
    // Add some withdrawals to batch
    CoinWithdrawal withdrawal1 = CoinWithdrawalFactory.create("test_account", "id1", new BigDecimal("1.0"));
    withdrawal1.setStatus("pending");
    long time = System.currentTimeMillis();
    withdrawal1.setUpdatedAt(time);

    CoinWithdrawal withdrawal2 = CoinWithdrawalFactory.create("test_account", "id2", new BigDecimal("2.0"));
    withdrawal2.setStatus("pending");
    withdrawal2.setUpdatedAt(time + 1000);

    withdrawalCache.addWithdrawalToBatch(withdrawal1);
    withdrawalCache.addWithdrawalToBatch(withdrawal2);

    // Truy cập trực tiếp vào map latestWithdrawals qua reflection
    Field latestWithdrawalsField = WithdrawalCache.class.getDeclaredField("latestWithdrawals");
    latestWithdrawalsField.setAccessible(true);
    Map<String, CoinWithdrawal> latestWithdrawals = (Map<String, CoinWithdrawal>) latestWithdrawalsField
        .get(withdrawalCache);

    // Kiểm tra trạng thái trước khi flush
    assertEquals(2, latestWithdrawals.size(), "Should have 2 withdrawals in batch");
    assertTrue(latestWithdrawals.containsKey("id1"), "Should contain withdrawal1");
    assertTrue(latestWithdrawals.containsKey("id2"), "Should contain withdrawal2");

    // Test flush with non-empty batch
    withdrawalCache.flushWithdrawalToDisk();
    verify(mockWithdrawalRocksDB).saveWithdrawalBatch(latestWithdrawals);

    // Kiểm tra map đã được clear sau khi flush
    assertEquals(0, latestWithdrawals.size(), "Map should be empty after flush");

    // Test flush with empty batch (should not call saveWithdrawalBatch again)
    withdrawalCache.flushWithdrawalToDisk();
    verifyNoMoreInteractions(mockWithdrawalRocksDB);
  }

  @Test
  void testWithdrawalCacheShouldFlush() throws Exception {
    // Act & Assert
    assertFalse(withdrawalCache.withdrawalCacheShouldFlush(), "Should not flush when batch is empty");

    // Thêm withdrawals đến khi đạt ngưỡng
    for (int i = 0; i < 10000; i++) {
      CoinWithdrawal withdrawal = CoinWithdrawalFactory.create("test_account", "id" + i, new BigDecimal("1.0"));
      withdrawalCache.addWithdrawalToBatch(withdrawal);
    }

    // Should now return true
    assertTrue(withdrawalCache.withdrawalCacheShouldFlush(), "Should flush when batch size reaches threshold");
  }
}
