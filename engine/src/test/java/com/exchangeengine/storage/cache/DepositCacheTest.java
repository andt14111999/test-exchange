package com.exchangeengine.storage.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exchangeengine.extension.MockitoStaticCleanupExtension;
import com.exchangeengine.extension.SingletonResetExtension;
import com.exchangeengine.model.CoinDeposit;
import com.exchangeengine.factory.CoinDepositFactory;
import com.exchangeengine.storage.rocksdb.DepositRocksDB;

@ExtendWith({ MockitoExtension.class, MockitoStaticCleanupExtension.class, SingletonResetExtension.class })
class DepositCacheTest {

  @Mock
  private DepositRocksDB mockDepositRocksDB;

  private DepositCache depositCache;
  private MockedStatic<DepositRocksDB> mockedDepositRocksDBStatic;
  private AutoCloseable closeable;

  // Test data
  private CoinDeposit testDeposit;

  @BeforeEach
  void setUp() throws Exception {
    closeable = MockitoAnnotations.openMocks(this);

    // Tạo dữ liệu test
    testDeposit = CoinDepositFactory.createDefaultCoinDeposit();

    // Reset các instance để tránh ảnh hưởng từ các test khác
    resetSingleton(DepositCache.class, "instance");

    // Cấu hình mock cho DepositRocksDB
    mockedDepositRocksDBStatic = Mockito.mockStatic(DepositRocksDB.class);
    mockedDepositRocksDBStatic.when(DepositRocksDB::getInstance).thenReturn(mockDepositRocksDB);

    // Tạo instance mới cho DepositCache
    Constructor<DepositCache> constructor = DepositCache.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    depositCache = constructor.newInstance();

    // Đảm bảo các trường được khởi tạo đúng cách
    ensureFieldsInitialized(depositCache);

    // Tạo spy trên instance thực (không phải mock)
    depositCache = Mockito.spy(depositCache);

    // Thiết lập instance cho các test
    DepositCache.setTestInstance(depositCache);

    // Thêm test deposit vào cache
    depositCache.updateCoinDeposit(testDeposit);
  }

  /**
   * Đảm bảo các fields trong DepositCache được khởi tạo đúng cách
   */
  private void ensureFieldsInitialized(DepositCache cache) throws Exception {
    // Khởi tạo depositCache nếu null
    Field depositCacheField = DepositCache.class.getDeclaredField("depositCache");
    depositCacheField.setAccessible(true);
    if (depositCacheField.get(cache) == null) {
      depositCacheField.set(cache, new java.util.concurrent.ConcurrentHashMap<String, CoinDeposit>());
    }

    // Khởi tạo latestDeposits nếu null
    Field latestDepositsField = DepositCache.class.getDeclaredField("latestDeposits");
    latestDepositsField.setAccessible(true);
    if (latestDepositsField.get(cache) == null) {
      latestDepositsField.set(cache, new java.util.concurrent.ConcurrentHashMap<String, CoinDeposit>());
    }
  }

  @AfterEach
  void tearDown() throws Exception {
    if (mockedDepositRocksDBStatic != null) {
      mockedDepositRocksDBStatic.close();
    }

    if (closeable != null) {
      closeable.close();
    }

    resetSingleton(DepositCache.class, "instance");

    // Reset RocksDB instance
    Field rockDBField = DepositRocksDB.class.getDeclaredField("instance");
    rockDBField.setAccessible(true);
    rockDBField.set(null, null);
  }

  private void resetSingleton(Class<?> clazz, String fieldName) throws Exception {
    java.lang.reflect.Field instance = clazz.getDeclaredField(fieldName);
    instance.setAccessible(true);
    instance.set(null, null);
  }

  @Test
  @DisplayName("getInstance should return the same instance")
  void getInstance_ShouldReturnSameInstance() {
    DepositCache instance1 = DepositCache.getInstance();
    DepositCache instance2 = DepositCache.getInstance();

    assertSame(instance1, instance2, "getInstance should always return the same instance");
  }

  @Test
  @DisplayName("getDeposit should return empty Optional when deposit not found")
  void getDeposit_ShouldReturnEmptyOptional_WhenDepositNotFound() {
    // Arrange
    String depositId = "nonexistent_deposit";

    // Act
    Optional<CoinDeposit> result = depositCache.getDeposit(depositId);

    // Assert
    assertTrue(result.isEmpty(), "Should return empty Optional when deposit not found");
  }

  @Test
  @DisplayName("getDeposit should return deposit when found")
  void getDeposit_ShouldReturnDeposit_WhenFound() {
    // Act
    Optional<CoinDeposit> result = depositCache.getDeposit(testDeposit.getIdentifier());

    // Assert
    assertTrue(result.isPresent(), "Should return Optional containing deposit when found");
    assertEquals(testDeposit.getIdentifier(), result.get().getIdentifier(), "Deposit id should match");
    assertEquals(0, testDeposit.getAmount().compareTo(result.get().getAmount()),
        "Deposit amount should match");
    assertEquals(testDeposit.getCoin(), result.get().getCoin(), "Deposit coin should match");
  }

  @Test
  @DisplayName("updateCoinDeposit should update deposit in cache")
  void updateCoinDeposit_ShouldUpdateDepositInCache() {
    // Arrange
    CoinDeposit deposit = new CoinDeposit();
    deposit.setIdentifier("new_deposit");
    deposit.setAmount(new BigDecimal("2.5"));
    deposit.setCoin("eth");

    // Act
    depositCache.updateCoinDeposit(deposit);

    // Assert
    Optional<CoinDeposit> result = depositCache.getDeposit("new_deposit");
    assertTrue(result.isPresent(), "Deposit should be present in cache after update");
    assertEquals("new_deposit", result.get().getIdentifier(), "Deposit id should match");
    assertEquals(0, new BigDecimal("2.5").compareTo(result.get().getAmount()),
        "Deposit amount should match");
    assertEquals("eth", result.get().getCoin(), "Deposit coin should match");
  }

  @Test
  @DisplayName("addDepositToBatch should handle all cases correctly")
  void testAddDepositToBatch() throws Exception {
    // Case 1: Add deposit with null identifier (should be ignored)
    CoinDeposit nullIdDeposit = CoinDepositFactory.createDefaultCoinDeposit();
    nullIdDeposit.setIdentifier(null);
    depositCache.addDepositToBatch(nullIdDeposit);

    // Case 2: Add deposit with empty identifier (should be ignored)
    CoinDeposit emptyIdDeposit = CoinDepositFactory.createDefaultCoinDeposit();
    emptyIdDeposit.setIdentifier("");
    depositCache.addDepositToBatch(emptyIdDeposit);

    // Case 3: Add first deposit for BTC/USDT
    String identifier = "btc_usdt_deposit";
    CoinDeposit firstDeposit = CoinDepositFactory.createDefaultCoinDeposit();
    firstDeposit.setIdentifier(identifier);
    firstDeposit.setCoin("BTC");
    firstDeposit.setUpdatedAt(1000L);
    depositCache.addDepositToBatch(firstDeposit);

    // Case 4: Add first deposit for ETH/USDT (different identifier)
    CoinDeposit ethDeposit = CoinDepositFactory.createDefaultCoinDeposit();
    ethDeposit.setIdentifier("eth_usdt_deposit");
    ethDeposit.setCoin("ETH");
    ethDeposit.setUpdatedAt(1000L);
    depositCache.addDepositToBatch(ethDeposit);

    // Case 5: Add newer deposit for BTC/USDT
    CoinDeposit newerDeposit = CoinDepositFactory.createDefaultCoinDeposit();
    newerDeposit.setIdentifier(identifier);
    newerDeposit.setCoin("BTC");
    newerDeposit.setUpdatedAt(2000L);
    depositCache.addDepositToBatch(newerDeposit);

    // Case 6: Add older deposit for BTC/USDT (should be ignored)
    CoinDeposit olderDeposit = CoinDepositFactory.createDefaultCoinDeposit();
    olderDeposit.setIdentifier(identifier);
    olderDeposit.setCoin("BTC");
    olderDeposit.setUpdatedAt(500L);
    depositCache.addDepositToBatch(olderDeposit);

    // Case 7: Add deposit with same timestamp (should keep existing)
    CoinDeposit sameTimeDeposit = CoinDepositFactory.createDefaultCoinDeposit();
    sameTimeDeposit.setIdentifier(identifier);
    sameTimeDeposit.setCoin("BTC");
    sameTimeDeposit.setUpdatedAt(2000L);
    depositCache.addDepositToBatch(sameTimeDeposit);

    // Assert final state
    Field latestDepositsField = DepositCache.class.getDeclaredField("latestDeposits");
    latestDepositsField.setAccessible(true);
    Map<String, CoinDeposit> latestDeposits = (Map<String, CoinDeposit>) latestDepositsField.get(depositCache);

    // Should have 2 deposits: BTC/USDT and ETH/USDT
    assertEquals(2, latestDeposits.size(), "Should have exactly 2 deposits");

    // Verify BTC/USDT deposit
    CoinDeposit savedBtcDeposit = latestDeposits.get(identifier);
    assertNotNull(savedBtcDeposit, "BTC deposit should exist");
    assertEquals(2000L, savedBtcDeposit.getUpdatedAt(), "Should keep newer BTC deposit");
    assertEquals("btc", savedBtcDeposit.getCoin(), "Should be BTC deposit");

    // Verify ETH/USDT deposit
    CoinDeposit savedEthDeposit = latestDeposits.get("eth_usdt_deposit");
    assertNotNull(savedEthDeposit, "ETH deposit should exist");
    assertEquals(1000L, savedEthDeposit.getUpdatedAt(), "Should keep ETH deposit timestamp");
    assertEquals("eth", savedEthDeposit.getCoin(), "Should be ETH deposit");
  }

  @Test
  @DisplayName("initializeDepositCache should update cache with deposits from DB")
  void initializeDepositCache_ShouldUpdateCacheWithDepositsFromDB() {
    // Arrange - create a new deposit that is not in cache initially
    CoinDeposit dbDeposit = new CoinDeposit();
    dbDeposit.setIdentifier("db_deposit");
    dbDeposit.setAmount(new BigDecimal("5.0"));
    when(mockDepositRocksDB.getAllDeposits()).thenReturn(List.of(testDeposit, dbDeposit));

    // Act
    depositCache.initializeDepositCache();

    // Assert
    Optional<CoinDeposit> result = depositCache.getDeposit("db_deposit");
    assertTrue(result.isPresent(), "Deposit from DB should be loaded into cache");
    assertEquals("db_deposit", result.get().getIdentifier(), "Deposit id should match");
    assertEquals(0, new BigDecimal("5.0").compareTo(result.get().getAmount()),
        "Deposit amount should match");
  }

  @Test
  @DisplayName("initializeDepositCache should skip deposits with null or empty identifier")
  void initializeDepositCache_ShouldSkipDeposits_WithNullOrEmptyIdentifier() throws Exception {
    // Arrange
    CoinDeposit nullIdDeposit = new CoinDeposit();
    nullIdDeposit.setIdentifier(null);

    CoinDeposit emptyIdDeposit = new CoinDeposit();
    emptyIdDeposit.setIdentifier("");

    when(mockDepositRocksDB.getAllDeposits()).thenReturn(List.of(nullIdDeposit, emptyIdDeposit));

    // Act
    depositCache.initializeDepositCache();

    // Assert - we can check this indirectly by examining the cache size through
    // reflection
    java.lang.reflect.Field cacheField = DepositCache.class.getDeclaredField("depositCache");
    cacheField.setAccessible(true);
    java.util.Map<String, CoinDeposit> cache = (java.util.Map<String, CoinDeposit>) cacheField.get(depositCache);

    assertEquals(1, cache.size(),
        "Cache should only contain the test deposit set in setUp");
  }

  @Test
  public void testInitializeDepositCache_WithException() {
    // Arrange
    when(mockDepositRocksDB.getAllDeposits()).thenThrow(new RuntimeException("DB Error"));

    // Act
    depositCache.initializeDepositCache();

    // Assert - verify that the cache remains functional
    CoinDeposit deposit = CoinDepositFactory.createDefaultCoinDeposit();
    depositCache.updateCoinDeposit(deposit);
    assertTrue(depositCache.getDeposit(deposit.getIdentifier()).isPresent(),
        "Cache should still be functional after initialization error");
  }

  @Test
  public void testInitializeDepositCache() throws Exception {
    // Arrange
    List<CoinDeposit> deposits = new ArrayList<>();
    deposits.add(testDeposit);

    // Configure mock
    when(mockDepositRocksDB.getAllDeposits()).thenReturn(deposits);

    // Act
    depositCache.initializeDepositCache();

    // Assert
    Field cacheField = DepositCache.class.getDeclaredField("depositCache");
    cacheField.setAccessible(true);
    Map<String, CoinDeposit> cache = (Map<String, CoinDeposit>) cacheField.get(depositCache);
    assertEquals(1, cache.size(), "Should only load valid deposits");
  }

  @Test
  public void testFlushDepositToDisk_WithEmptyBatch() {
    // Act
    depositCache.flushDepositToDisk();

    // Assert
    verify(mockDepositRocksDB, never()).saveDepositBatch(any());
  }

  @Test
  public void testFlushDepositToDisk() {
    // Arrange
    CoinDeposit deposit = CoinDepositFactory.createDefaultCoinDeposit();
    depositCache.addDepositToBatch(deposit);

    // Act
    depositCache.flushDepositToDisk();

    // Assert
    verify(mockDepositRocksDB, times(1)).saveDepositBatch(any());
  }

  @Test
  public void testDepositCacheShouldFlush() {
    // Act & Assert
    assertFalse(depositCache.depositCacheShouldFlush(), "Should not flush when batch is empty");

    // Add deposits until reaching batch size
    for (int i = 0; i < 10000; i++) {
      CoinDeposit deposit = CoinDepositFactory.createDefaultCoinDeposit();
      deposit.setIdentifier("deposit" + i);
      depositCache.addDepositToBatch(deposit);
    }

    // Should now return true
    assertTrue(depositCache.depositCacheShouldFlush(), "Should flush when batch size reaches threshold");
  }
}
