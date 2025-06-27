package com.exchangeengine.storage.rocksdb;

import com.exchangeengine.model.AccountHistory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.rocksdb.ColumnFamilyHandle;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountHistoryRocksDBTest {

  private AccountHistoryRocksDB accountHistoryRocksDB;

  @Mock
  private RocksDBService mockedRocksDBService;

  @Mock
  private ColumnFamilyHandle mockedAccountHistoryCFHandle;

  private MockedStatic<RocksDBService> mockedRocksDBServiceStatic;
  private AutoCloseable closeable;

  @BeforeEach
  void setUp() {
    // Khởi tạo mocks
    closeable = MockitoAnnotations.openMocks(this);

    // Mock static getInstance method của RocksDBService
    mockedRocksDBServiceStatic = Mockito.mockStatic(RocksDBService.class);
    mockedRocksDBServiceStatic.when(RocksDBService::getInstance).thenReturn(mockedRocksDBService);

    // Mock getAccountHistoryCF để trả về mock
    when(mockedRocksDBService.getAccountHistoryCF()).thenReturn(mockedAccountHistoryCFHandle);

    // Reset instance của AccountHistoryRocksDB
    AccountHistoryRocksDB.resetInstance();

    // Khởi tạo đối tượng test
    accountHistoryRocksDB = AccountHistoryRocksDB.getInstance();
  }

  @AfterEach
  void tearDown() throws Exception {
    // Reset instance của AccountHistoryRocksDB
    AccountHistoryRocksDB.resetInstance();

    // Đóng mocks
    if (mockedRocksDBServiceStatic != null) {
      mockedRocksDBServiceStatic.close();
    }
    if (closeable != null) {
      closeable.close();
    }
  }

  @Test
  void testGetInstance() {
    // Given
    AccountHistoryRocksDB instance1 = AccountHistoryRocksDB.getInstance();
    AccountHistoryRocksDB instance2 = AccountHistoryRocksDB.getInstance();

    // Then
    assertNotNull(instance1);
    assertSame(instance1, instance2, "getInstance phải trả về cùng một instance");
  }

  @Test
  void testSaveAccountHistory() {
    // Given
    AccountHistory history = new AccountHistory("btc:user1", "deposit123", "DEPOSIT");
    history.setBalanceValues(
        BigDecimal.valueOf(100),
        BigDecimal.valueOf(150),
        BigDecimal.ZERO,
        BigDecimal.ZERO);

    // When
    accountHistoryRocksDB.saveAccountHistory(history);

    // Then
    verify(mockedRocksDBService).saveObject(
        eq(history),
        eq(mockedAccountHistoryCFHandle),
        any(), // không thể so sánh function reference
        eq("account_history"));
  }

  @Test
  void testGetAccountHistory() {
    // Given
    String historyKey = "abcd1234-btc:user1-deposit123-1234567890";
    AccountHistory expectedHistory = new AccountHistory("btc:user1", "deposit123", "DEPOSIT");
    expectedHistory.setKey(historyKey);
    expectedHistory.setBalanceValues(
        BigDecimal.valueOf(100),
        BigDecimal.valueOf(150),
        BigDecimal.ZERO,
        BigDecimal.ZERO);

    when(mockedRocksDBService.getObject(
        eq(historyKey),
        eq(mockedAccountHistoryCFHandle),
        eq(AccountHistory.class),
        eq("account_history"))).thenReturn(Optional.of(expectedHistory));

    // When
    Optional<AccountHistory> resultHistory = accountHistoryRocksDB.getAccountHistory(historyKey);

    // Then
    assertTrue(resultHistory.isPresent());
    assertEquals(expectedHistory, resultHistory.get());
  }

  @Test
  void testGetAccountHistoryNotFound() {
    // Given
    String historyKey = "nonexistent";

    when(mockedRocksDBService.getObject(
        eq(historyKey),
        eq(mockedAccountHistoryCFHandle),
        eq(AccountHistory.class),
        eq("account_history"))).thenReturn(Optional.empty());

    // When
    Optional<AccountHistory> resultHistory = accountHistoryRocksDB.getAccountHistory(historyKey);

    // Then
    assertFalse(resultHistory.isPresent());
  }

  @Test
  void testGetAccountHistoriesByAccountKey() {
    // Given
    String accountKey = "btc:user1";
    String prefix = AccountHistory.generateAccountPrefix(accountKey);
    int limit = 100;
    String lastKey = null;

    List<AccountHistory> expectedHistories = Arrays.asList(
        createTestHistory(accountKey, "deposit1", "DEPOSIT"),
        createTestHistory(accountKey, "deposit2", "DEPOSIT"),
        createTestHistory(accountKey, "withdrawal1", "WITHDRAWAL"));

    when(mockedRocksDBService.getObjectsByPrefix(
        eq(prefix),
        eq(limit),
        eq(lastKey),
        eq(mockedAccountHistoryCFHandle),
        eq(AccountHistory.class),
        eq("account_history"))).thenReturn(expectedHistories);

    // When
    List<AccountHistory> resultHistories = accountHistoryRocksDB.getAccountHistoriesByAccountKey(accountKey, limit,
        lastKey);

    // Then
    assertEquals(expectedHistories.size(), resultHistories.size());
    assertEquals(expectedHistories, resultHistories);
  }

  private AccountHistory createTestHistory(String accountKey, String identifier, String operationType) {
    AccountHistory history = new AccountHistory(accountKey, identifier, operationType);
    history.setBalanceValues(
        BigDecimal.valueOf(100),
        BigDecimal.valueOf(150),
        BigDecimal.ZERO,
        BigDecimal.ZERO);
    return history;
  }

  @Test
  void testSaveAccountHistoryBatch() {
    // Given
    Map<String, AccountHistory> histories = new HashMap<>();

    AccountHistory history1 = createTestHistory("btc:user1", "deposit1", "DEPOSIT");
    AccountHistory history2 = createTestHistory("eth:user1", "deposit2", "DEPOSIT");

    histories.put(history1.getKey(), history1);
    histories.put(history2.getKey(), history2);

    // When
    accountHistoryRocksDB.saveAccountHistoryBatch(histories);

    // Then
    verify(mockedRocksDBService).saveBatch(
        eq(histories),
        eq(mockedAccountHistoryCFHandle),
        any(), // không thể so sánh function reference
        eq("account_histories"));
  }

  @Test
  void testGetAllAccountHistories() {
    // Given
    List<AccountHistory> expectedHistories = Arrays.asList(
        createTestHistory("btc:user1", "deposit1", "DEPOSIT"),
        createTestHistory("eth:user1", "deposit2", "DEPOSIT"),
        createTestHistory("btc:user2", "withdrawal1", "WITHDRAWAL"));

    when(mockedRocksDBService.getAllObjects(
        eq(mockedAccountHistoryCFHandle),
        eq(AccountHistory.class),
        eq("account_histories"))).thenReturn(expectedHistories);

    // When
    List<AccountHistory> resultHistories = accountHistoryRocksDB.getAllAccountHistories();

    // Then
    assertEquals(expectedHistories, resultHistories);
    verify(mockedRocksDBService).getAllObjects(
        eq(mockedAccountHistoryCFHandle),
        eq(AccountHistory.class),
        eq("account_histories"));
  }

  @Test
  void testGetAccountHistoriesByPrefix() {
    // Given
    String prefix = "btc:user1";
    int limit = 50;
    String lastKey = "last_key_123";

    List<AccountHistory> expectedHistories = Arrays.asList(
        createTestHistory("btc:user1", "deposit1", "DEPOSIT"),
        createTestHistory("btc:user1", "deposit2", "DEPOSIT"),
        createTestHistory("btc:user1", "withdrawal1", "WITHDRAWAL"));

    when(mockedRocksDBService.getObjectsByPrefix(
        eq(prefix),
        eq(limit),
        eq(lastKey),
        eq(mockedAccountHistoryCFHandle),
        eq(AccountHistory.class),
        eq("account_history"))).thenReturn(expectedHistories);

    // When
    List<AccountHistory> resultHistories = accountHistoryRocksDB.getAccountHistoriesByPrefix(prefix, limit, lastKey);

    // Then
    assertEquals(expectedHistories.size(), resultHistories.size());
    assertEquals(expectedHistories, resultHistories);
    verify(mockedRocksDBService).getObjectsByPrefix(
        eq(prefix),
        eq(limit),
        eq(lastKey),
        eq(mockedAccountHistoryCFHandle),
        eq(AccountHistory.class),
        eq("account_history"));
  }
}
