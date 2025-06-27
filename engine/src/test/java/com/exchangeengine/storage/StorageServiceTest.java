package com.exchangeengine.storage;

import static org.mockito.Mockito.*;

import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.DepositCache;
import com.exchangeengine.storage.cache.WithdrawalCache;
import com.exchangeengine.storage.cache.AccountHistoryCache;
import com.exchangeengine.storage.cache.EventCache;
import com.exchangeengine.storage.cache.AmmPoolCache;
import com.exchangeengine.storage.cache.AmmPositionCache;
import com.exchangeengine.storage.cache.MerchantEscrowCache;
import com.exchangeengine.storage.cache.TickCache;
import com.exchangeengine.storage.cache.TickBitmapCache;
import com.exchangeengine.storage.cache.AmmOrderCache;
import com.exchangeengine.extension.CombinedTestExtension;
import com.exchangeengine.storage.rocksdb.RocksDBService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({ MockitoExtension.class, CombinedTestExtension.class })
class StorageServiceTest {

  @Mock
  private AccountCache mockAccountCache;

  @Mock
  private DepositCache mockDepositCache;

  @Mock
  private WithdrawalCache mockWithdrawalCache;

  @Mock
  private AccountHistoryCache mockAccountHistoryCache;

  @Mock
  private EventCache mockEventCache;

  @Mock
  private AmmPoolCache mockAmmPoolCache;

  @Mock
  private TickCache mockTickCache;

  @Mock
  private TickBitmapCache mockTickBitmapCache;

  @Mock
  private AmmPositionCache mockAmmPositionCache;

  @Mock
  private MerchantEscrowCache mockMerchantEscrowCache;

  @Mock
  private AmmOrderCache mockAmmOrderCache;

  private StorageService storageService;

  @BeforeEach
  void setUp() throws Exception {
    // CombinedTestExtension đã tự động reset tất cả, không cần reset thủ công

    // Thiết lập mock và cấu hình
    setupMocks();

    // Tạo StorageService test instance
    createTestStorageService();
  }

  /**
   * Thiết lập các mock cần thiết cho test
   */
  private void setupMocks() {
    // Thiết lập các mock
    AccountCache.setTestInstance(mockAccountCache);
    DepositCache.setTestInstance(mockDepositCache);
    WithdrawalCache.setTestInstance(mockWithdrawalCache);
    AccountHistoryCache.setTestInstance(mockAccountHistoryCache);
    EventCache.setTestInstance(mockEventCache);
    AmmPoolCache.setTestInstance(mockAmmPoolCache);
    TickCache.setTestInstance(mockTickCache);
    TickBitmapCache.setTestInstance(mockTickBitmapCache);
    AmmPositionCache.setTestInstance(mockAmmPositionCache);
    MerchantEscrowCache.setTestInstance(mockMerchantEscrowCache);
    AmmOrderCache.setTestInstance(mockAmmOrderCache);

    // Cấu hình hành vi cơ bản
    lenient().when(mockAccountCache.accountCacheShouldFlush()).thenReturn(false);
    lenient().when(mockDepositCache.depositCacheShouldFlush()).thenReturn(false);
    lenient().when(mockWithdrawalCache.withdrawalCacheShouldFlush()).thenReturn(false);
    lenient().when(mockAccountHistoryCache.historiesCacheShouldFlush()).thenReturn(false);
    lenient().when(mockAmmPoolCache.ammPoolCacheShouldFlush()).thenReturn(false);
    lenient().when(mockTickCache.tickCacheShouldFlush()).thenReturn(false);
    lenient().when(mockTickBitmapCache.tickBitmapCacheShouldFlush()).thenReturn(false);
    lenient().when(mockAmmPositionCache.ammPositionCacheShouldFlush()).thenReturn(false);
    lenient().when(mockMerchantEscrowCache.merchantEscrowCacheShouldFlush()).thenReturn(false);
  }

  /**
   * Tạo và cấu hình StorageService test instance
   */
  private void createTestStorageService() throws Exception {
    // Mock RocksDBService để tránh khởi tạo thật
    RocksDBService mockRocksDBService = mock(RocksDBService.class);
    RocksDBService.setTestInstance(mockRocksDBService);
    
    // Tạo instance thông qua reflection để tránh gọi getInstance()
    Constructor<StorageService> constructor = StorageService.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    storageService = constructor.newInstance();

    // Tạo spy để verify
    storageService = spy(storageService);

    // Cấu hình các phương thức getter trả về mock objects
    lenient().doReturn(mockAccountCache).when(storageService).getAccountCache();
    lenient().doReturn(mockDepositCache).when(storageService).getDepositCache();
    lenient().doReturn(mockWithdrawalCache).when(storageService).getWithdrawalCache();
    lenient().doReturn(mockAccountHistoryCache).when(storageService).getAccountHistoryCache();
    lenient().doReturn(mockEventCache).when(storageService).getEventCache();
    lenient().doReturn(mockAmmPoolCache).when(storageService).getAmmPoolCache();
    lenient().doReturn(mockTickCache).when(storageService).getTickCache();
    lenient().doReturn(mockTickBitmapCache).when(storageService).getTickBitmapCache();
    lenient().doReturn(mockAmmPositionCache).when(storageService).getAmmPositionCache();
    lenient().doReturn(mockMerchantEscrowCache).when(storageService).getMerchantEscrowCache();
    lenient().doReturn(mockAmmOrderCache).when(storageService).getAmmOrderCache();

    // Thiết lập instance để test
    StorageService.setTestInstance(storageService);
  }

  // Không cần afterEach vì CombinedTestExtension đã tự động reset

  @Test
  @DisplayName("getInstance should return the same instance")
  void getInstance_ShouldReturnSameInstance() {
    StorageService instance1 = StorageService.getInstance();
    StorageService instance2 = StorageService.getInstance();

    assertSame(instance1, instance2, "getInstance should always return the same instance");
  }

  @Test
  @DisplayName("initializeCache should call initialize on all cache instances")
  void initializeCache_ShouldCallInitializeOnAllCaches() {
    // Reset mock counts
    clearInvocations(mockAccountCache, mockDepositCache, mockWithdrawalCache, mockAmmPoolCache,
        mockTickCache, mockTickBitmapCache);
    clearInvocations(mockAccountCache, mockDepositCache, mockWithdrawalCache, mockAmmPoolCache,
        mockMerchantEscrowCache, mockAmmOrderCache);

    // Act
    storageService.initializeCache();

    // Assert
    verify(mockAccountCache).initializeAccountCache();
    verify(mockDepositCache).initializeDepositCache();
    verify(mockWithdrawalCache).initializeWithdrawalCache();
    verify(mockAmmPoolCache).initializeAmmPoolCache();
    verify(mockTickCache).initializeTickCache();
    verify(mockTickBitmapCache).initializeTickBitmapCache();
    verify(mockAmmPositionCache).initializeAmmPositionCache();
    verify(mockMerchantEscrowCache).initializeMerchantEscrowCache();
    verify(mockAmmOrderCache).initializeAmmOrderCache();
  }

  @Test
  @DisplayName("getAccountCache should return the account cache instance")
  void getAccountCache_ShouldReturnAccountCacheInstance() {
    // Act
    AccountCache result = storageService.getAccountCache();

    // Assert
    assertSame(mockAccountCache, result, "Should return the mock account cache");
  }

  @Test
  @DisplayName("getDepositCache should return the deposit cache instance")
  void getDepositCache_ShouldReturnDepositCacheInstance() {
    // Act
    DepositCache result = storageService.getDepositCache();

    // Assert
    assertSame(mockDepositCache, result, "Should return the mock deposit cache");
  }

  @Test
  @DisplayName("getWithdrawalCache should return the withdrawal cache instance")
  void getWithdrawalCache_ShouldReturnWithdrawalCacheInstance() {
    // Act
    WithdrawalCache result = storageService.getWithdrawalCache();

    // Assert
    assertSame(mockWithdrawalCache, result, "Should return the mock withdrawal cache");
  }

  @Test
  @DisplayName("getAccountHistoryCache should return the account history cache instance")
  void getAccountHistoryCache_ShouldReturnAccountHistoryCacheInstance() {
    // Act
    AccountHistoryCache result = storageService.getAccountHistoryCache();

    // Assert
    assertSame(mockAccountHistoryCache, result, "Should return the mock account history cache");
  }

  @Test
  @DisplayName("getEventCache should return the event cache instance")
  void getEventCache_ShouldReturnEventCacheInstance() {
    // Act
    EventCache result = storageService.getEventCache();

    // Assert
    assertSame(mockEventCache, result, "Should return the mock event cache");
  }

  @Test
  @DisplayName("getAmmPoolCache should return the AMM pool cache instance")
  void getAmmPoolCache_ShouldReturnAmmPoolCacheInstance() {
    // Act
    AmmPoolCache result = storageService.getAmmPoolCache();

    // Assert
    assertSame(mockAmmPoolCache, result, "Should return the mock AMM pool cache");
  }

  @Test
  @DisplayName("getTickCache should return the tick cache instance")
  void getTickCache_ShouldReturnTickCacheInstance() {
    // Act
    TickCache result = storageService.getTickCache();

    // Assert
    assertSame(mockTickCache, result, "Should return the mock tick cache");
  }

  @Test
  @DisplayName("getTickBitmapCache should return the tick bitmap cache instance")
  void getTickBitmapCache_ShouldReturnTickBitmapCacheInstance() {
    // Act
    TickBitmapCache result = storageService.getTickBitmapCache();

    // Assert
    assertSame(mockTickBitmapCache, result, "Should return the mock tick bitmap cache");
  }

  @Test
  @DisplayName("getAmmPositionCache should return the AMM position cache instance")
  void getAmmPositionCache_ShouldReturnAmmPositionCacheInstance() {
    // Act
    AmmPositionCache result = storageService.getAmmPositionCache();

    // Assert
    assertSame(mockAmmPositionCache, result, "Should return the mock AMM position cache");
  }

  @Test
  @DisplayName("getMerchantEscrowCache should return the merchant escrow cache instance")
  void getMerchantEscrowCache_ShouldReturnMerchantEscrowCacheInstance() {
    // Act
    MerchantEscrowCache result = storageService.getMerchantEscrowCache();

    // Assert
    assertSame(mockMerchantEscrowCache, result, "Should return the mock merchant escrow cache");
  }

  @Test
  @DisplayName("getAmmOrderCache should return the AMM order cache instance")
  void getAmmOrderCache_ShouldReturnAmmOrderCacheInstance() {
    // Act
    AmmOrderCache result = storageService.getAmmOrderCache();

    // Assert
    assertSame(mockAmmOrderCache, result, "Should return the mock AMM order cache");
  }

  @Test
  @DisplayName("shouldFlush should check cache flush conditions")
  void shouldFlush_ShouldCheckCacheFlushConditions() {
    // Arrange
    when(mockAccountCache.accountCacheShouldFlush()).thenReturn(true);
    when(mockDepositCache.depositCacheShouldFlush()).thenReturn(false);
    when(mockWithdrawalCache.withdrawalCacheShouldFlush()).thenReturn(false);
    when(mockAccountHistoryCache.historiesCacheShouldFlush()).thenReturn(false);
    when(mockAmmPoolCache.ammPoolCacheShouldFlush()).thenReturn(false);
    when(mockTickCache.tickCacheShouldFlush()).thenReturn(false);
    when(mockTickBitmapCache.tickBitmapCacheShouldFlush()).thenReturn(false);
    when(mockAmmPositionCache.ammPositionCacheShouldFlush()).thenReturn(false);
    when(mockMerchantEscrowCache.merchantEscrowCacheShouldFlush()).thenReturn(false);

    // Act
    boolean result = storageService.shouldFlush();

    // Assert
    assertTrue(result, "Should return true when any cache reports it should flush");

    // Test with all caches returning false
    Mockito.reset(mockAccountCache, mockDepositCache, mockWithdrawalCache, mockAccountHistoryCache,
        mockAmmPoolCache, mockTickCache, mockTickBitmapCache, mockAmmPositionCache, mockMerchantEscrowCache,
        mockAmmOrderCache);
    when(mockAccountCache.accountCacheShouldFlush()).thenReturn(false);
    when(mockDepositCache.depositCacheShouldFlush()).thenReturn(false);
    when(mockWithdrawalCache.withdrawalCacheShouldFlush()).thenReturn(false);
    when(mockAccountHistoryCache.historiesCacheShouldFlush()).thenReturn(false);
    when(mockAmmPoolCache.ammPoolCacheShouldFlush()).thenReturn(false);
    when(mockTickCache.tickCacheShouldFlush()).thenReturn(false);
    when(mockTickBitmapCache.tickBitmapCacheShouldFlush()).thenReturn(false);
    when(mockAmmPositionCache.ammPositionCacheShouldFlush()).thenReturn(false);
    when(mockMerchantEscrowCache.merchantEscrowCacheShouldFlush()).thenReturn(false);

    result = storageService.shouldFlush();
    assertFalse(result, "Should return false when no cache reports it should flush");
  }

  @Test
  @DisplayName("shouldFlush should return true when any individual cache needs flush")
  void shouldFlush_ShouldReturnTrue_WhenAnyIndividualCacheNeedsFlush() {
    // Test all individual caches

    // Test 1: Only Account cache needs flush
    reset(mockAccountCache, mockDepositCache, mockWithdrawalCache, mockAccountHistoryCache,
        mockAmmPoolCache, mockTickCache, mockTickBitmapCache);
    when(mockAccountCache.accountCacheShouldFlush()).thenReturn(true);
    when(mockDepositCache.depositCacheShouldFlush()).thenReturn(false);
    when(mockWithdrawalCache.withdrawalCacheShouldFlush()).thenReturn(false);
    when(mockAccountHistoryCache.historiesCacheShouldFlush()).thenReturn(false);
    when(mockAmmPoolCache.ammPoolCacheShouldFlush()).thenReturn(false);
    when(mockTickCache.tickCacheShouldFlush()).thenReturn(false);
    when(mockTickBitmapCache.tickBitmapCacheShouldFlush()).thenReturn(false);
    when(mockAmmPositionCache.ammPositionCacheShouldFlush()).thenReturn(false);
    assertTrue(storageService.shouldFlush(), "Should flush when account cache needs flush");

    // Test 2: Only Deposit cache needs flush
    reset(mockAccountCache, mockDepositCache, mockWithdrawalCache, mockAccountHistoryCache,
        mockAmmPoolCache, mockTickCache, mockTickBitmapCache);
    when(mockAccountCache.accountCacheShouldFlush()).thenReturn(false);
    when(mockDepositCache.depositCacheShouldFlush()).thenReturn(true);
    when(mockWithdrawalCache.withdrawalCacheShouldFlush()).thenReturn(false);
    when(mockAccountHistoryCache.historiesCacheShouldFlush()).thenReturn(false);
    when(mockAmmPoolCache.ammPoolCacheShouldFlush()).thenReturn(false);
    when(mockTickCache.tickCacheShouldFlush()).thenReturn(false);
    when(mockTickBitmapCache.tickBitmapCacheShouldFlush()).thenReturn(false);
    when(mockAmmPositionCache.ammPositionCacheShouldFlush()).thenReturn(false);
    assertTrue(storageService.shouldFlush(), "Should flush when deposit cache needs flush");

    // Test 3: Only Withdrawal cache needs flush
    reset(mockAccountCache, mockDepositCache, mockWithdrawalCache, mockAccountHistoryCache,
        mockAmmPoolCache, mockTickCache, mockTickBitmapCache);
    when(mockAccountCache.accountCacheShouldFlush()).thenReturn(false);
    when(mockDepositCache.depositCacheShouldFlush()).thenReturn(false);
    when(mockWithdrawalCache.withdrawalCacheShouldFlush()).thenReturn(true);
    when(mockAccountHistoryCache.historiesCacheShouldFlush()).thenReturn(false);
    when(mockAmmPoolCache.ammPoolCacheShouldFlush()).thenReturn(false);
    when(mockTickCache.tickCacheShouldFlush()).thenReturn(false);
    when(mockTickBitmapCache.tickBitmapCacheShouldFlush()).thenReturn(false);
    when(mockAmmPositionCache.ammPositionCacheShouldFlush()).thenReturn(false);
    assertTrue(storageService.shouldFlush(), "Should flush when withdrawal cache needs flush");

    // Test 4: Only AccountHistory cache needs flush
    reset(mockAccountCache, mockDepositCache, mockWithdrawalCache, mockAccountHistoryCache,
        mockAmmPoolCache, mockTickCache, mockTickBitmapCache);
    when(mockAccountCache.accountCacheShouldFlush()).thenReturn(false);
    when(mockDepositCache.depositCacheShouldFlush()).thenReturn(false);
    when(mockWithdrawalCache.withdrawalCacheShouldFlush()).thenReturn(false);
    when(mockAccountHistoryCache.historiesCacheShouldFlush()).thenReturn(true);
    when(mockAmmPoolCache.ammPoolCacheShouldFlush()).thenReturn(false);
    when(mockTickCache.tickCacheShouldFlush()).thenReturn(false);
    when(mockTickBitmapCache.tickBitmapCacheShouldFlush()).thenReturn(false);
    when(mockAmmPositionCache.ammPositionCacheShouldFlush()).thenReturn(false);
    assertTrue(storageService.shouldFlush(), "Should flush when account history cache needs flush");

    // Test 5: Only AmmPool cache needs flush
    reset(mockAccountCache, mockDepositCache, mockWithdrawalCache, mockAccountHistoryCache,
        mockAmmPoolCache, mockTickCache, mockTickBitmapCache);
    when(mockAccountCache.accountCacheShouldFlush()).thenReturn(false);
    when(mockDepositCache.depositCacheShouldFlush()).thenReturn(false);
    when(mockWithdrawalCache.withdrawalCacheShouldFlush()).thenReturn(false);
    when(mockAccountHistoryCache.historiesCacheShouldFlush()).thenReturn(false);
    when(mockAmmPoolCache.ammPoolCacheShouldFlush()).thenReturn(true);
    when(mockTickCache.tickCacheShouldFlush()).thenReturn(false);
    when(mockTickBitmapCache.tickBitmapCacheShouldFlush()).thenReturn(false);
    when(mockAmmPositionCache.ammPositionCacheShouldFlush()).thenReturn(false);
    assertTrue(storageService.shouldFlush(), "Should flush when amm pool cache needs flush");

    // Test 6: Only Tick cache needs flush
    reset(mockAccountCache, mockDepositCache, mockWithdrawalCache, mockAccountHistoryCache,
        mockAmmPoolCache, mockTickCache, mockTickBitmapCache);
    when(mockAccountCache.accountCacheShouldFlush()).thenReturn(false);
    when(mockDepositCache.depositCacheShouldFlush()).thenReturn(false);
    when(mockWithdrawalCache.withdrawalCacheShouldFlush()).thenReturn(false);
    when(mockAccountHistoryCache.historiesCacheShouldFlush()).thenReturn(false);
    when(mockAmmPoolCache.ammPoolCacheShouldFlush()).thenReturn(false);
    when(mockTickCache.tickCacheShouldFlush()).thenReturn(true);
    when(mockTickBitmapCache.tickBitmapCacheShouldFlush()).thenReturn(false);
    when(mockAmmPositionCache.ammPositionCacheShouldFlush()).thenReturn(false);
    assertTrue(storageService.shouldFlush(), "Should flush when tick cache needs flush");

    // Test 7: Only TickBitmap cache needs flush
    reset(mockAccountCache, mockDepositCache, mockWithdrawalCache, mockAccountHistoryCache,
        mockAmmPoolCache, mockTickCache, mockTickBitmapCache);
    when(mockAccountCache.accountCacheShouldFlush()).thenReturn(false);
    when(mockDepositCache.depositCacheShouldFlush()).thenReturn(false);
    when(mockWithdrawalCache.withdrawalCacheShouldFlush()).thenReturn(false);
    when(mockAccountHistoryCache.historiesCacheShouldFlush()).thenReturn(false);
    when(mockAmmPoolCache.ammPoolCacheShouldFlush()).thenReturn(false);
    when(mockTickCache.tickCacheShouldFlush()).thenReturn(false);
    when(mockTickBitmapCache.tickBitmapCacheShouldFlush()).thenReturn(true);
    when(mockAmmPositionCache.ammPositionCacheShouldFlush()).thenReturn(false);
    assertTrue(storageService.shouldFlush(), "Should flush when tick bitmap cache needs flush");

    // Test 8: Only AmmPosition cache needs flush
    reset(mockAccountCache, mockDepositCache, mockWithdrawalCache, mockAccountHistoryCache,
        mockAmmPoolCache, mockTickCache, mockTickBitmapCache, mockAmmPositionCache);
    when(mockAccountCache.accountCacheShouldFlush()).thenReturn(false);
    when(mockDepositCache.depositCacheShouldFlush()).thenReturn(false);
    when(mockWithdrawalCache.withdrawalCacheShouldFlush()).thenReturn(false);
    when(mockAccountHistoryCache.historiesCacheShouldFlush()).thenReturn(false);
    when(mockAmmPoolCache.ammPoolCacheShouldFlush()).thenReturn(false);
    when(mockTickCache.tickCacheShouldFlush()).thenReturn(false);
    when(mockTickBitmapCache.tickBitmapCacheShouldFlush()).thenReturn(false);
    when(mockAmmPositionCache.ammPositionCacheShouldFlush()).thenReturn(true);
    assertTrue(storageService.shouldFlush(), "Should flush when amm position cache needs flush");
  }

  @Test
  @DisplayName("flushToDisk should call flush on all cache instances")
  void flushToDisk_ShouldCallFlushOnAllCaches() {
    // Act
    storageService.flushToDisk();

    // Assert
    verify(mockAccountCache).flushAccountToDisk();
    verify(mockDepositCache).flushDepositToDisk();
    verify(mockWithdrawalCache).flushWithdrawalToDisk();
    verify(mockAccountHistoryCache).flushHistoryToDisk();
    verify(mockAmmPoolCache).flushAmmPoolToDisk();
    verify(mockTickCache).flushTicksToDisk();
    verify(mockTickBitmapCache).flushTickBitmapsToDisk();
    verify(mockAmmPositionCache).flushAmmPositionToDisk();
    verify(mockMerchantEscrowCache).flushMerchantEscrowToDisk();
  }

  @Test
  @DisplayName("shutdown should call flushToDisk")
  void shutdown_ShouldCallFlushToDisk() {
    // Sử dụng doNothing() cho phương thức flushToDisk
    doNothing().when(storageService).flushToDisk();

    // Act
    storageService.shutdown();

    // Assert
    verify(storageService, times(1)).flushToDisk();
  }

  @Test
  @DisplayName("resetInstance should set instance to null")
  void resetInstance_ShouldSetInstanceToNull() throws Exception {
    // Ensure there's an instance first
    StorageService instance = StorageService.getInstance();
    assertNotNull(instance);

    // Act
    StorageService.resetInstance();

    // Assert: Use reflection to check if the instance is now null
    Field instanceField = StorageService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    assertNull(instanceField.get(null), "Instance should be null after resetInstance() is called");
  }
}
