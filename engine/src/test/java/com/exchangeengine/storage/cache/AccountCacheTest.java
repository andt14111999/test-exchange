package com.exchangeengine.storage.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exchangeengine.model.Account;
import com.exchangeengine.storage.rocksdb.AccountRocksDB;
import com.exchangeengine.factory.AccountFactory;

@ExtendWith(MockitoExtension.class)
class AccountCacheTest {

  @Mock
  private AccountRocksDB mockAccountRocksDB;

  private AccountCache accountCache;
  private MockedStatic<AccountRocksDB> mockedAccountRocksDBStatic;

  // Test data
  private Account testAccount;

  @BeforeEach
  void setUp() throws Exception {
    // Tạo dữ liệu test sử dụng AccountFactory
    testAccount = AccountFactory.createWithBalances("btc:test",
        new BigDecimal("100.0"),
        new BigDecimal("50.0"));

    // Cấu hình mock
    mockedAccountRocksDBStatic = Mockito.mockStatic(AccountRocksDB.class);
    mockedAccountRocksDBStatic.when(AccountRocksDB::getInstance).thenReturn(mockAccountRocksDB);

    // Reset AccountCache instance
    resetSingleton(AccountCache.class, "instance");

    // Khởi tạo AccountCache
    accountCache = AccountCache.getInstance();
    accountCache.updateAccount(testAccount);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (mockedAccountRocksDBStatic != null) {
      mockedAccountRocksDBStatic.close();
    }
    resetSingleton(AccountCache.class, "instance");
  }

  private void resetSingleton(Class<?> clazz, String fieldName) throws Exception {
    java.lang.reflect.Field instance = clazz.getDeclaredField(fieldName);
    instance.setAccessible(true);
    instance.set(null, null);
  }

  @Test
  @DisplayName("getInstance should return the same instance")
  void getInstance_ShouldReturnSameInstance() {
    AccountCache instance1 = AccountCache.getInstance();
    AccountCache instance2 = AccountCache.getInstance();

    assertSame(instance1, instance2, "getInstance should always return the same instance");
  }

  @Test
  @DisplayName("getAccount should return empty Optional when account not found")
  void getAccount_ShouldReturnEmptyOptional_WhenAccountNotFound() {
    // Arrange
    String accountKey = "nonexistent_account";

    // Act
    Optional<Account> result = accountCache.getAccount(accountKey);

    // Assert
    assertTrue(result.isEmpty(), "Should return empty Optional when account not found");
  }

  @Test
  @DisplayName("getAccount should return account when found")
  void getAccount_ShouldReturnAccount_WhenFound() {
    // Act
    Optional<Account> result = accountCache.getAccount(testAccount.getKey());

    // Assert
    assertTrue(result.isPresent(), "Should return Optional containing account when found");
    assertEquals(testAccount.getKey(), result.get().getKey(), "Account key should match");
    assertEquals(0, new BigDecimal("100.0").compareTo(result.get().getAvailableBalance()),
        "Account balance should match");
  }

  @Test
  @DisplayName("getOrInitAccount should create new account when not found")
  void getOrInitAccount_ShouldCreateNewAccount_WhenNotFound() {
    // Arrange
    String accountKey = "btc:newuser";

    // Act
    Account result = accountCache.getOrInitAccount(accountKey);

    // Assert
    assertNotNull(result, "Should return a non-null account");
    assertEquals(accountKey, result.getKey(), "Account key should match");
    assertEquals(0, BigDecimal.ZERO.compareTo(result.getAvailableBalance()),
        "Available balance should be zero");
    assertEquals(0, BigDecimal.ZERO.compareTo(result.getFrozenBalance()),
        "Frozen balance should be zero");
  }

  @Test
  @DisplayName("getOrInitAccount should return existing account when found")
  void getOrInitAccount_ShouldReturnExistingAccount_WhenFound() {
    // Act
    Account result = accountCache.getOrInitAccount(testAccount.getKey());

    // Assert
    assertNotNull(result, "Should return a non-null account");
    assertEquals(testAccount.getKey(), result.getKey(), "Account key should match");
    assertEquals(0, testAccount.getAvailableBalance().compareTo(result.getAvailableBalance()),
        "Available balance should match existing account");
    assertEquals(0, testAccount.getFrozenBalance().compareTo(result.getFrozenBalance()),
        "Frozen balance should match existing account");
  }

  @Test
  @DisplayName("updateAccount should update account in cache")
  void updateAccount_ShouldUpdateAccountInCache() {
    // Arrange
    Account account = AccountFactory.createWithBalances("btc:update_test",
        new BigDecimal("50.0"),
        BigDecimal.ZERO);

    // Act
    accountCache.updateAccount(account);

    // Assert
    Optional<Account> result = accountCache.getAccount("btc:update_test");
    assertTrue(result.isPresent(), "Account should be present in cache after update");
    assertEquals("btc:update_test", result.get().getKey(), "Account key should match");
    assertEquals(0, new BigDecimal("50.0").compareTo(result.get().getAvailableBalance()),
        "Account balance should match");
  }

  @Test
  @DisplayName("getOrCreateAccount should create and add new account when not found")
  void getOrCreateAccount_ShouldCreateAndAddNewAccount_WhenNotFound() {
    // Arrange
    String accountKey = "btc:newuser";

    // Act
    Account result = accountCache.getOrCreateAccount(accountKey);

    // Assert
    assertNotNull(result, "Should return a non-null account");
    assertEquals(accountKey, result.getKey(), "Account key should match");
    assertEquals(0, BigDecimal.ZERO.compareTo(result.getAvailableBalance()),
        "Available balance should be zero");

    // Verify account was added to cache
    Optional<Account> cachedAccount = accountCache.getAccount(accountKey);
    assertTrue(cachedAccount.isPresent(), "Account should be present in cache");
    assertEquals(accountKey, cachedAccount.get().getKey(), "Cached account key should match");
  }

  @Test
  @DisplayName("resetAccount should reset account to initial state when account exists")
  void resetAccount_ShouldResetAccountToInitialState_WhenAccountExists() {
    // Arrange
    String accountKey = "btc:test";

    // Clear và cấu hình lại mock
    try {
      // Lấy accountCache vì nó sẽ được sử dụng để test
      java.lang.reflect.Field latestAccountsField = AccountCache.class.getDeclaredField("latestAccounts");
      latestAccountsField.setAccessible(true);
      @SuppressWarnings("unchecked")
      java.util.Map<String, Account> latestAccounts = (java.util.Map<String, Account>) latestAccountsField
          .get(accountCache);
      latestAccounts.clear();

      // Trong implementation, resetAccount sẽ gọi addAccountToBatch, không phải saveAccount 
      // Không cần phải verify saveAccount nữa
    } catch (Exception e) {
      fail("Failed to set up test: " + e.getMessage());
    }

    // Act
    accountCache.resetAccount(accountKey);

    // Assert
    Optional<Account> resetAccount = accountCache.getAccount(accountKey);
    assertTrue(resetAccount.isPresent(), "Account should still exist after reset");
    assertEquals(accountKey, resetAccount.get().getKey(), "Account key should remain the same");
    assertEquals(0, BigDecimal.ZERO.compareTo(resetAccount.get().getAvailableBalance()),
        "Available balance should be reset to zero");
    assertEquals(0, BigDecimal.ZERO.compareTo(resetAccount.get().getFrozenBalance()),
        "Frozen balance should be reset to zero");

    // Kiểm tra xem account đã được thêm vào latestAccounts chưa
    try {
      java.lang.reflect.Field latestAccountsField = AccountCache.class.getDeclaredField("latestAccounts");
      latestAccountsField.setAccessible(true);
      java.util.Map<String, Account> latestAccounts = (java.util.Map<String, Account>) latestAccountsField
          .get(accountCache);
      assertTrue(latestAccounts.containsKey(accountKey), "Account should be added to latestAccounts");
      
      Account batchedAccount = latestAccounts.get(accountKey);
      assertEquals(0, BigDecimal.ZERO.compareTo(batchedAccount.getAvailableBalance()),
          "Batched account should have zero balance");
      assertEquals(0, BigDecimal.ZERO.compareTo(batchedAccount.getFrozenBalance()),
          "Batched account should have zero frozen balance");
    } catch (Exception e) {
      fail("Failed to check latestAccounts: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("resetAccount should not do anything when account does not exist")
  void resetAccount_ShouldDoNothing_WhenAccountDoesNotExist() {
    // Arrange
    String nonExistentAccountKey = "btc:nonexistent";

    // Act
    accountCache.resetAccount(nonExistentAccountKey);

    // Assert
    Optional<Account> account = accountCache.getAccount(nonExistentAccountKey);
    assertFalse(account.isPresent(), "Account should not exist");

    // Verify rocksDBService.saveAccount was not called
    verify(mockAccountRocksDB, never()).saveAccount(any(Account.class));
  }

  @Test
  @DisplayName("addAccountToBatch should add account to latest accounts")
  void addAccountToBatch_ShouldAddAccountToLatestAccounts() throws Exception {
    // Arrange
    Account account = AccountFactory.createWithBalances("btc:new_batch_test",
        new BigDecimal("75.0"),
        BigDecimal.ZERO);
    lenient().doNothing().when(mockAccountRocksDB).saveAccountBatch(any());

    // Act
    accountCache.addAccountToBatch(account);

    // Need to verify indirectly by flushing and checking if saveAccountBatch was
    // called
    accountCache.flushAccountToDisk();

    // Assert
    verify(mockAccountRocksDB, times(1)).saveAccountBatch(any());
  }

  @Test
  @DisplayName("initializeAccountCache should update cache with accounts from DB")
  void initializeAccountCache_ShouldUpdateCacheWithAccountsFromDB() {
    // Arrange - create a new account that is not in cache initially
    Account dbAccount = AccountFactory.createWithBalances("btc:db_only",
        new BigDecimal("200.0"),
        BigDecimal.ZERO);
    lenient().when(mockAccountRocksDB.getAllAccounts()).thenReturn(List.of(testAccount, dbAccount));

    // Act
    accountCache.initializeAccountCache();

    // Assert
    Optional<Account> result = accountCache.getAccount("btc:db_only");
    assertTrue(result.isPresent(), "Account from DB should be loaded into cache");
    assertEquals("btc:db_only", result.get().getKey(), "Account key should match");
    assertEquals(0, new BigDecimal("200.0").compareTo(result.get().getAvailableBalance()),
        "Account balance should match");
  }

  @Test
  @DisplayName("accountCacheShouldFlush should return false when batch size is below threshold")
  void accountCacheShouldFlush_ShouldReturnFalse_WhenBatchSizeBelowThreshold() throws Exception {
    // Act & Assert
    assertFalse(accountCache.accountCacheShouldFlush(), "Should return false when batch size is below threshold");
  }

  @Test
  @DisplayName("accountCacheShouldFlush should return true when batch size reaches threshold")
  void accountCacheShouldFlush_ShouldReturnTrue_WhenBatchSizeReachesThreshold() throws Exception {
    // We need to access BACKUP_BATCH_SIZE through reflection to know threshold
    java.lang.reflect.Field batchSizeField = AccountCache.class.getDeclaredField("BACKUP_BATCH_SIZE");
    batchSizeField.setAccessible(true);
    int batchSizeThreshold = (int) batchSizeField.get(null);

    // Access latestAccounts to add many accounts
    java.lang.reflect.Field latestAccountsField = AccountCache.class.getDeclaredField("latestAccounts");
    latestAccountsField.setAccessible(true);
    java.util.Map<String, Account> latestAccounts = (java.util.Map<String, Account>) latestAccountsField
        .get(accountCache);

    // Add accounts until we reach threshold
    for (int i = 0; i < batchSizeThreshold; i++) {
      Account account = new Account("btc:batch_test_" + i);
      latestAccounts.put(account.getKey(), account);
    }

    // Act & Assert
    assertTrue(accountCache.accountCacheShouldFlush(), "Should return true when batch size reaches threshold");

    // Now remove some and check it returns false
    for (int i = 0; i < 5; i++) {
      latestAccounts.remove("btc:batch_test_" + i);
    }

    assertFalse(accountCache.accountCacheShouldFlush(), "Should return false when batch size is below threshold");
  }

  @Test
  @DisplayName("flushAccountToDisk should not call saveAccountBatch when no accounts to flush")
  void flushAccountToDisk_ShouldNotCallSaveAccountBatch_WhenNoAccountsToFlush() {
    // Arrange - clear latestAccounts
    // Prepare a clean state
    try {
      java.lang.reflect.Field latestAccountsField = AccountCache.class.getDeclaredField("latestAccounts");
      latestAccountsField.setAccessible(true);
      @SuppressWarnings("unchecked")
      java.util.Map<String, Account> latestAccounts = (java.util.Map<String, Account>) latestAccountsField
          .get(accountCache);
      
      // Clear latestAccounts
      latestAccounts.clear();
      
      // Verify it's empty before proceeding
      assertTrue(latestAccounts.isEmpty(), "latestAccounts should be empty before test");
    } catch (Exception e) {
      fail("Failed to clear latestAccounts: " + e.getMessage());
    }

    // Act
    accountCache.flushAccountToDisk();

    // Assert - since latestAccounts is empty, saveAccountBatch should not be called
    verify(mockAccountRocksDB, never()).saveAccountBatch(any());
  }

  @Test
  @DisplayName("addAccountToBatch should update account when new account has newer timestamp")
  void addAccountToBatch_ShouldUpdateAccount_WhenNewAccountHasNewerTimestamp() throws Exception {
    // Arrange
    Account oldAccount = AccountFactory.createWithBalances("btc:test",
        new BigDecimal("100.0"),
        BigDecimal.ZERO);
    oldAccount.setUpdatedAt(1000L);

    Account newAccount = AccountFactory.createWithBalances("btc:test",
        new BigDecimal("200.0"),
        BigDecimal.ZERO);
    newAccount.setUpdatedAt(2000L);

    // Access latestAccounts to clean and check 
    java.lang.reflect.Field latestAccountsField = AccountCache.class.getDeclaredField("latestAccounts");
    latestAccountsField.setAccessible(true);
    java.util.Map<String, Account> latestAccounts = (java.util.Map<String, Account>) latestAccountsField
        .get(accountCache);
    latestAccounts.clear();

    // Add old account first
    accountCache.addAccountToBatch(oldAccount);

    // Verify old account was added
    assertEquals(oldAccount, latestAccounts.get("btc:test"), 
        "old account should be in latestAccounts");

    // Act
    accountCache.addAccountToBatch(newAccount);

    // Assert
    Account storedAccount = latestAccounts.get("btc:test");
    assertNotNull(storedAccount, "Account should be present in latest accounts");
    assertEquals(newAccount, storedAccount, "New account should have replaced old account");
    assertEquals(0, new BigDecimal("200.0").compareTo(storedAccount.getAvailableBalance()),
        "Should have updated to new account's balance");
    assertEquals(2000L, storedAccount.getUpdatedAt(), "Should have updated timestamp");
  }

  @Test
  @DisplayName("addAccountToBatch should keep existing account when new account has older timestamp")
  void addAccountToBatch_ShouldKeepExistingAccount_WhenNewAccountHasOlderTimestamp() throws Exception {
    // Arrange
    Account newerAccount = AccountFactory.createWithBalances("btc:test",
        new BigDecimal("200.0"),
        BigDecimal.ZERO);
    newerAccount.setUpdatedAt(2000L);

    Account olderAccount = AccountFactory.createWithBalances("btc:test",
        new BigDecimal("100.0"),
        BigDecimal.ZERO);
    olderAccount.setUpdatedAt(1000L);

    // Access latestAccounts to clean and check
    java.lang.reflect.Field latestAccountsField = AccountCache.class.getDeclaredField("latestAccounts");
    latestAccountsField.setAccessible(true);
    @SuppressWarnings("unchecked")
    java.util.Map<String, Account> latestAccounts = (java.util.Map<String, Account>) latestAccountsField
        .get(accountCache);
    latestAccounts.clear();

    // Add newer account first
    accountCache.addAccountToBatch(newerAccount);
    
    // Verify newer account was added
    assertEquals(newerAccount, latestAccounts.get("btc:test"), 
        "newer account should be in latestAccounts");

    // Act
    accountCache.addAccountToBatch(olderAccount);

    // Assert
    Account storedAccount = latestAccounts.get("btc:test");
    assertNotNull(storedAccount, "Account should be present in latest accounts");
    assertEquals(newerAccount, storedAccount, "Newer account should still be present");
    assertEquals(0, new BigDecimal("200.0").compareTo(storedAccount.getAvailableBalance()),
        "Should have kept newer account's balance");
    assertEquals(2000L, storedAccount.getUpdatedAt(), "Should have kept newer timestamp");
  }

  @Test
  @DisplayName("initializeAccountCache should handle exception when RocksDB fails")
  void initializeAccountCache_ShouldHandleException_WhenRocksDBFails() {
    // Arrange
    lenient().when(mockAccountRocksDB.getAllAccounts()).thenThrow(new RuntimeException("DB Error"));

    // Act
    accountCache.initializeAccountCache();

    // Assert - verify that the cache still works after error
    Optional<Account> result = accountCache.getAccount(testAccount.getKey());
    assertTrue(result.isPresent(), "Existing account should still be in cache after error");
  }

  @Test
  @DisplayName("initializeAccountCache should skip accounts that are newer in cache")
  void initializeAccountCache_ShouldSkipAccounts_WhenCacheIsNewer() {
    // Arrange
    Account dbAccount = AccountFactory.create("btc:test");
    dbAccount.setUpdatedAt(1000L); // Older timestamp

    Account cacheAccount = AccountFactory.create("btc:test");
    cacheAccount.setUpdatedAt(2000L); // Newer timestamp
    accountCache.updateAccount(cacheAccount);

    lenient().when(mockAccountRocksDB.getAllAccounts()).thenReturn(List.of(dbAccount));

    // Act
    accountCache.initializeAccountCache();

    // Assert
    Optional<Account> result = accountCache.getAccount("btc:test");
    assertTrue(result.isPresent(), "Account should be in cache");
    assertEquals(2000L, result.get().getUpdatedAt(),
        "Should keep newer timestamp from cache");
  }

  @Test
  @DisplayName("initializeAccountCache should update accounts that are older in cache")
  void initializeAccountCache_ShouldUpdateAccounts_WhenDBIsNewer() {
    // Arrange
    Account dbAccount = AccountFactory.create("btc:test");
    dbAccount.setUpdatedAt(2000L); // Newer timestamp

    Account cacheAccount = AccountFactory.create("btc:test");
    cacheAccount.setUpdatedAt(1000L); // Older timestamp
    accountCache.updateAccount(cacheAccount);

    lenient().when(mockAccountRocksDB.getAllAccounts()).thenReturn(List.of(dbAccount));

    // Act
    accountCache.initializeAccountCache();

    // Assert
    Optional<Account> result = accountCache.getAccount("btc:test");
    assertTrue(result.isPresent(), "Account should be in cache");
    assertEquals(2000L, result.get().getUpdatedAt(),
        "Should update to newer timestamp from DB");
  }

  @Test
  @DisplayName("updateAccount should not update when account is null")
  void updateAccount_ShouldNotUpdate_WhenAccountIsNull() {
    // Act
    accountCache.updateAccount(null);

    // Assert - không có lỗi xảy ra và không thay đổi cache
    verify(mockAccountRocksDB, never()).saveAccount(any());
  }

  @Test
  @DisplayName("updateAccount should not update when account key is null")
  void updateAccount_ShouldNotUpdate_WhenAccountKeyIsNull() {
    // Arrange
    Account accountWithNullKey = AccountFactory.create("valid-key");
    accountWithNullKey.setKey(null);

    // Act
    accountCache.updateAccount(accountWithNullKey);

    // Assert - không có lỗi xảy ra và không thay đổi cache
    verify(mockAccountRocksDB, never()).saveAccount(any());
  }

  @Test
  @DisplayName("resetAccount should do nothing when accountKey is null")
  void resetAccount_ShouldDoNothing_WhenAccountKeyIsNull() {
    // Act
    accountCache.resetAccount(null);

    // Assert
    verify(mockAccountRocksDB, never()).saveAccount(any());
  }

  @Test
  @DisplayName("resetAccount should do nothing when accountKey is empty")
  void resetAccount_ShouldDoNothing_WhenAccountKeyIsEmpty() {
    // Act
    accountCache.resetAccount("");

    // Assert
    verify(mockAccountRocksDB, never()).saveAccount(any());
  }

  @Test
  @DisplayName("flushAccountToDisk should handle exceptions when RocksDB fails")
  void flushAccountToDisk_ShouldHandleExceptions_WhenRocksDBFails() throws Exception {
    // Arrange
    Account account = AccountFactory.createWithBalances("btc:exception_test",
        new BigDecimal("50.0"), 
        BigDecimal.ZERO);
    
    // Thêm account vào batch
    accountCache.addAccountToBatch(account);
    
    // Cấu hình mock để ném ngoại lệ
    doThrow(new RuntimeException("Database error")).when(mockAccountRocksDB).saveAccountBatch(any());

    // Act - không nên ném exception
    accountCache.flushAccountToDisk();

    // Assert
    verify(mockAccountRocksDB, times(1)).saveAccountBatch(any());
    
    // Kiểm tra latestAccounts đã được xóa
    java.lang.reflect.Field latestAccountsField = AccountCache.class.getDeclaredField("latestAccounts");
    latestAccountsField.setAccessible(true);
    @SuppressWarnings("unchecked")
    java.util.Map<String, Account> latestAccounts = (java.util.Map<String, Account>) latestAccountsField
        .get(accountCache);
    
    assertTrue(latestAccounts.isEmpty(), "latestAccounts should be cleared even when an exception occurs");
  }

  @Test
  @DisplayName("setTestInstance should set the instance for testing")
  void setTestInstance_ShouldSetInstanceForTesting() {
    // Arrange
    AccountCache testInstance = mock(AccountCache.class);
    
    // Act
    AccountCache.setTestInstance(testInstance);
    AccountCache result = AccountCache.getInstance();
    
    // Assert
    assertSame(testInstance, result, "getInstance should return the test instance");
    
    // Cleanup
    AccountCache.resetInstance();
  }
  
  @Test
  @DisplayName("initializeAccountCache should skip null accounts from DB")
  void initializeAccountCache_ShouldSkipNullAccounts() {
    // Arrange
    Account validAccount = AccountFactory.create("btc:valid");
    lenient().when(mockAccountRocksDB.getAllAccounts()).thenReturn(Arrays.asList(null, validAccount, null));
    
    // Act
    accountCache.initializeAccountCache();
    
    // Assert
    Optional<Account> result = accountCache.getAccount("btc:valid");
    assertTrue(result.isPresent(), "Valid account should be loaded");
    assertEquals("btc:valid", result.get().getKey(), "Account key should match");
  }
  
  @Test
  @DisplayName("initializeAccountCache should skip accounts with null key from DB")
  void initializeAccountCache_ShouldSkipAccountsWithNullKey() {
    // Arrange
    Account invalidAccount = AccountFactory.create("btc:invalid");
    invalidAccount.setKey(null);
    
    Account validAccount = AccountFactory.create("btc:valid");
    
    lenient().when(mockAccountRocksDB.getAllAccounts()).thenReturn(Arrays.asList(invalidAccount, validAccount));
    
    // Act
    accountCache.initializeAccountCache();
    
    // Assert
    Optional<Account> result = accountCache.getAccount("btc:valid");
    assertTrue(result.isPresent(), "Valid account should be loaded");
    assertEquals("btc:valid", result.get().getKey(), "Account key should match");
  }

  @Test
  @DisplayName("addAccountToBatch should not add account when account is null")
  void addAccountToBatch_ShouldNotAdd_WhenAccountIsNull() throws Exception {
    // Arrange
    java.lang.reflect.Field latestAccountsField = AccountCache.class.getDeclaredField("latestAccounts");
    latestAccountsField.setAccessible(true);
    java.util.Map<String, Account> latestAccounts = (java.util.Map<String, Account>) latestAccountsField
        .get(accountCache);
    latestAccounts.clear();
    
    // Act
    accountCache.addAccountToBatch(null);
    
    // Assert
    assertTrue(latestAccounts.isEmpty(), "latestAccounts should remain empty");
  }
  
  @Test
  @DisplayName("addAccountToBatch should not add account when account key is null")
  void addAccountToBatch_ShouldNotAdd_WhenAccountKeyIsNull() throws Exception {
    // Arrange
    Account accountWithNullKey = AccountFactory.create("valid-key");
    accountWithNullKey.setKey(null);
    
    java.lang.reflect.Field latestAccountsField = AccountCache.class.getDeclaredField("latestAccounts");
    latestAccountsField.setAccessible(true);
    java.util.Map<String, Account> latestAccounts = (java.util.Map<String, Account>) latestAccountsField
        .get(accountCache);
    latestAccounts.clear();
    
    // Act
    accountCache.addAccountToBatch(accountWithNullKey);
    
    // Assert
    assertTrue(latestAccounts.isEmpty(), "latestAccounts should remain empty");
  }
}
