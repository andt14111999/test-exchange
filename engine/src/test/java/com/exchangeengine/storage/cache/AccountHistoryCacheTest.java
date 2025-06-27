package com.exchangeengine.storage.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
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
import org.mockito.junit.jupiter.MockitoExtension;

import com.exchangeengine.model.AccountHistory;
import com.exchangeengine.storage.rocksdb.AccountHistoryRocksDB;
import com.exchangeengine.factory.AccountHistoryFactory;

@ExtendWith(MockitoExtension.class)
class AccountHistoryCacheTest {

  @Mock
  private AccountHistoryRocksDB mockAccountHistoryRocksDB;

  private AccountHistoryCache accountHistoryCache;
  private MockedStatic<AccountHistoryRocksDB> mockedAccountHistoryRocksDBStatic;

  // Test data
  private AccountHistory testHistory;
  private static final String TEST_ACCOUNT_KEY = "btc:user123";

  @BeforeEach
  void setUp() throws Exception {
    // Tạo dữ liệu test sử dụng factory
    testHistory = AccountHistoryFactory.createForDeposit(TEST_ACCOUNT_KEY, "history123");

    // Cấu hình mock static
    mockedAccountHistoryRocksDBStatic = Mockito.mockStatic(AccountHistoryRocksDB.class);
    mockedAccountHistoryRocksDBStatic.when(AccountHistoryRocksDB::getInstance).thenReturn(mockAccountHistoryRocksDB);

    // Reset AccountHistoryCache instance
    resetSingleton(AccountHistoryCache.class, "instance");

    // Khởi tạo AccountHistoryCache
    accountHistoryCache = AccountHistoryCache.getInstance();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (mockedAccountHistoryRocksDBStatic != null) {
      mockedAccountHistoryRocksDBStatic.close();
    }
    resetSingleton(AccountHistoryCache.class, "instance");
  }

  private void resetSingleton(Class<?> clazz, String fieldName) throws Exception {
    Field instance = clazz.getDeclaredField(fieldName);
    instance.setAccessible(true);
    instance.set(null, null);
  }

  @Test
  @DisplayName("getInstance should return the same instance")
  void getInstance_ShouldReturnSameInstance() {
    AccountHistoryCache instance1 = AccountHistoryCache.getInstance();
    AccountHistoryCache instance2 = AccountHistoryCache.getInstance();

    assertSame(instance1, instance2, "getInstance should always return the same instance");
  }

  @Test
  @DisplayName("getAccountHistory should return empty Optional when history not found")
  void getAccountHistory_ShouldReturnEmptyOptional_WhenHistoryNotFound() {
    // Arrange
    String historyKey = "nonexistent_history";
    lenient().when(mockAccountHistoryRocksDB.getAccountHistory(historyKey)).thenReturn(Optional.empty());

    // Act
    Optional<AccountHistory> result = accountHistoryCache.getAccountHistory(historyKey);

    // Assert
    assertTrue(result.isEmpty(), "Should return empty Optional when history not found");
    verify(mockAccountHistoryRocksDB, times(1)).getAccountHistory(historyKey);
  }

  @Test
  @DisplayName("getAccountHistory should return history when found")
  void getAccountHistory_ShouldReturnHistory_WhenFound() {
    // Arrange
    lenient().when(mockAccountHistoryRocksDB.getAccountHistory(testHistory.getKey()))
        .thenReturn(Optional.of(testHistory));

    // Act
    Optional<AccountHistory> result = accountHistoryCache.getAccountHistory(testHistory.getKey());

    // Assert
    assertTrue(result.isPresent(), "Should return Optional containing history when found");
    assertEquals(testHistory.getKey(), result.get().getKey(), "History key should match");
    assertEquals(testHistory.getAccountKey(), result.get().getAccountKey(), "Account key should match");
    assertEquals(testHistory.getOperationType(), result.get().getOperationType(), "Operation type should match");
    verify(mockAccountHistoryRocksDB, times(1)).getAccountHistory(testHistory.getKey());
  }

  @Test
  @DisplayName("updateAccountHistory should save history to RocksDB")
  void updateAccountHistory_ShouldSaveHistoryToRocksDB() {
    // Arrange
    AccountHistory history = AccountHistoryFactory.createForWithdrawal(TEST_ACCOUNT_KEY, "new_history");
    lenient().doNothing().when(mockAccountHistoryRocksDB).saveAccountHistory(any(AccountHistory.class));

    // Act
    accountHistoryCache.updateAccountHistory(history);

    // Assert
    verify(mockAccountHistoryRocksDB, times(1)).saveAccountHistory(history);
  }

  @Test
  @DisplayName("getAccountTransactionHistory should return account histories for specified account")
  void getAccountTransactionHistory_ShouldReturnAccountHistoriesForSpecifiedAccount() {
    // Arrange
    int limit = 10;
    String lastKey = null;

    // Create test histories using factory
    AccountHistory history1 = AccountHistoryFactory.createForDeposit(TEST_ACCOUNT_KEY, "history1");
    AccountHistory history2 = AccountHistoryFactory.createForWithdrawal(TEST_ACCOUNT_KEY, "history2");

    List<AccountHistory> expectedHistories = Arrays.asList(history1, history2);

    lenient().when(mockAccountHistoryRocksDB.getAccountHistoriesByAccountKey(TEST_ACCOUNT_KEY, limit, lastKey))
        .thenReturn(expectedHistories);

    // Act
    List<AccountHistory> result = accountHistoryCache.getAccountTransactionHistory(TEST_ACCOUNT_KEY, lastKey, limit);

    // Assert
    assertEquals(2, result.size(), "Should return 2 account histories");
    assertEquals(history1.getKey(), result.get(0).getKey(), "First history key should match");
    assertEquals(history2.getKey(), result.get(1).getKey(), "Second history key should match");
    verify(mockAccountHistoryRocksDB, times(1)).getAccountHistoriesByAccountKey(TEST_ACCOUNT_KEY, limit, lastKey);
  }

  @Test
  @DisplayName("getAccountTransactionHistory should return empty list when no histories found")
  void getAccountTransactionHistory_ShouldReturnEmptyList_WhenNoHistoriesFound() {
    // Arrange
    int limit = 10;
    String lastKey = null;

    lenient().when(mockAccountHistoryRocksDB.getAccountHistoriesByAccountKey(TEST_ACCOUNT_KEY, limit, lastKey))
        .thenReturn(Collections.emptyList());

    // Act
    List<AccountHistory> result = accountHistoryCache.getAccountTransactionHistory(TEST_ACCOUNT_KEY, lastKey, limit);

    // Assert
    assertTrue(result.isEmpty(), "Should return empty list when no histories found");
    verify(mockAccountHistoryRocksDB, times(1)).getAccountHistoriesByAccountKey(TEST_ACCOUNT_KEY, limit, lastKey);
  }

  @Test
  @DisplayName("addHistoryToBatch should add history to latest histories")
  void addHistoryToBatch_ShouldAddHistoryToLatestHistories() throws Exception {
    // Arrange
    AccountHistory history = AccountHistoryFactory.createForDeposit(TEST_ACCOUNT_KEY, "batch_history");
    lenient().doNothing().when(mockAccountHistoryRocksDB).saveAccountHistoryBatch(any());

    // Act
    accountHistoryCache.addHistoryToBatch(history);

    // Verify through reflection that history was added to batch
    Field latestHistoriesField = AccountHistoryCache.class.getDeclaredField("latestHistories");
    latestHistoriesField.setAccessible(true);
    Map<String, AccountHistory> latestHistories = (Map<String, AccountHistory>) latestHistoriesField
        .get(accountHistoryCache);

    // Assert
    AccountHistory storedHistory = latestHistories.get(history.getKey());
    assertNotNull(storedHistory, "History should be present in latest histories");
    assertEquals(history.getKey(), storedHistory.getKey(), "History key should match");
    assertEquals(history.getAccountKey(), storedHistory.getAccountKey(), "Account key should match");
    assertEquals(history.getOperationType(), storedHistory.getOperationType(), "Operation type should match");
  }

  @Test
  @DisplayName("historiesCacheShouldFlush should return true when batch size reaches threshold")
  void historiesCacheShouldFlush_ShouldReturnTrue_WhenBatchSizeReachesThreshold() throws Exception {
    // We need to access BACKUP_BATCH_SIZE through reflection to know threshold
    java.lang.reflect.Field batchSizeField = AccountHistoryCache.class.getDeclaredField("BACKUP_BATCH_SIZE");
    batchSizeField.setAccessible(true);
    int batchSizeThreshold = (int) batchSizeField.get(null);

    // First test with size below threshold
    for (int i = 0; i < batchSizeThreshold - 1; i++) {
      AccountHistory history = AccountHistoryFactory.createForDeposit(TEST_ACCOUNT_KEY, "history_" + i);
      accountHistoryCache.addHistoryToBatch(history);
    }

    assertFalse(accountHistoryCache.historiesCacheShouldFlush(),
        "Should return false when batch size is below threshold");

    // Add one more to reach threshold
    AccountHistory history = AccountHistoryFactory.createForDeposit(TEST_ACCOUNT_KEY, "history_final");
    accountHistoryCache.addHistoryToBatch(history);

    assertTrue(accountHistoryCache.historiesCacheShouldFlush(),
        "Should return true when batch size reaches threshold");
  }

  @Test
  @DisplayName("flushHistoryToDisk should not call saveAccountHistoryBatch when no histories to flush")
  void flushHistoryToDisk_ShouldNotCallSaveAccountHistoryBatch_WhenNoHistoriesToFlush() {
    // Act
    accountHistoryCache.flushHistoryToDisk();

    // Assert
    verify(mockAccountHistoryRocksDB, never()).saveAccountHistoryBatch(any());
  }

  @Test
  @DisplayName("flushHistoryToDisk should clear latestHistories after saving to RocksDB")
  void flushHistoryToDisk_ShouldClearLatestHistories_AfterSavingToRocksDB() throws Exception {
    // Arrange
    AccountHistory history = AccountHistoryFactory.createForDeposit(TEST_ACCOUNT_KEY, "flush_test");
    lenient().doNothing().when(mockAccountHistoryRocksDB).saveAccountHistoryBatch(any());

    // Add history to batch
    accountHistoryCache.addHistoryToBatch(history);

    // Access latestHistories to verify it's not empty before flush
    Field latestHistoriesField = AccountHistoryCache.class.getDeclaredField("latestHistories");
    latestHistoriesField.setAccessible(true);
    Map<String, AccountHistory> latestHistories = (Map<String, AccountHistory>) latestHistoriesField
        .get(accountHistoryCache);
    assertFalse(latestHistories.isEmpty(), "Latest histories should not be empty before flush");

    // Act
    accountHistoryCache.flushHistoryToDisk();

    // Assert
    assertTrue(latestHistories.isEmpty(), "Latest histories should be empty after flush");
    verify(mockAccountHistoryRocksDB, times(1)).saveAccountHistoryBatch(any());
  }
}
