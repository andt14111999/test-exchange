package com.exchangeengine.storage.rocksdb;

import com.exchangeengine.model.Account;
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

class AccountRocksDBTest {

  private AccountRocksDB accountRocksDB;

  @Mock
  private RocksDBService mockedRocksDBService;

  @Mock
  private ColumnFamilyHandle mockedAccountCFHandle;

  private MockedStatic<RocksDBService> mockedRocksDBServiceStatic;
  private AutoCloseable closeable;

  @BeforeEach
  void setUp() {
    // Khởi tạo mocks
    closeable = MockitoAnnotations.openMocks(this);

    // Mock static getInstance method của RocksDBService
    mockedRocksDBServiceStatic = Mockito.mockStatic(RocksDBService.class);
    mockedRocksDBServiceStatic.when(RocksDBService::getInstance).thenReturn(mockedRocksDBService);

    // Mock getAccountCF để trả về mock
    when(mockedRocksDBService.getAccountCF()).thenReturn(mockedAccountCFHandle);

    // Reset instance của AccountRocksDB
    AccountRocksDB.resetInstance();

    // Khởi tạo đối tượng test
    accountRocksDB = AccountRocksDB.getInstance();
  }

  @AfterEach
  void tearDown() throws Exception {
    // Reset instance của AccountRocksDB
    AccountRocksDB.resetInstance();

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
    AccountRocksDB instance1 = AccountRocksDB.getInstance();
    AccountRocksDB instance2 = AccountRocksDB.getInstance();

    // Then
    assertNotNull(instance1);
    assertSame(instance1, instance2, "getInstance phải trả về cùng một instance");
  }

  @Test
  void testSaveAccount() {
    // Given
    String accountKey = "btc:user1";
    Account account = new Account(accountKey);
    account.setAvailableBalance(BigDecimal.valueOf(100));

    // When
    accountRocksDB.saveAccount(account);

    // Then
    verify(mockedRocksDBService).saveObject(
        eq(account),
        eq(mockedAccountCFHandle),
        any(), // không thể so sánh function reference
        eq("accounts"));
  }

  @Test
  void testGetAccount() {
    // Given
    String accountKey = "btc:user1";
    Account expectedAccount = new Account(accountKey);
    expectedAccount.setAvailableBalance(BigDecimal.valueOf(100));

    when(mockedRocksDBService.getObject(
        eq(accountKey),
        eq(mockedAccountCFHandle),
        eq(Account.class),
        eq("accounts"))).thenReturn(Optional.of(expectedAccount));

    // When
    Optional<Account> resultAccount = accountRocksDB.getAccount(accountKey);

    // Then
    assertTrue(resultAccount.isPresent());
    assertEquals(expectedAccount, resultAccount.get());
  }

  @Test
  void testGetAccountNotFound() {
    // Given
    String accountKey = "btc:nonexistent";

    when(mockedRocksDBService.getObject(
        eq(accountKey),
        eq(mockedAccountCFHandle),
        eq(Account.class),
        eq("accounts"))).thenReturn(Optional.empty());

    // When
    Optional<Account> resultAccount = accountRocksDB.getAccount(accountKey);

    // Then
    assertFalse(resultAccount.isPresent());
  }

  @Test
  void testSaveAccountBatch() {
    // Given
    Map<String, Account> accounts = new HashMap<>();
    accounts.put("btc:user1", new Account("btc:user1"));
    accounts.put("eth:user1", new Account("eth:user1"));

    // When
    accountRocksDB.saveAccountBatch(accounts);

    // Then
    verify(mockedRocksDBService).saveBatch(
        eq(accounts),
        eq(mockedAccountCFHandle),
        any(), // không thể so sánh function reference
        eq("accounts"));
  }

  @Test
  void testGetAllAccounts() {
    // Given
    List<Account> expectedAccounts = Arrays.asList(
        new Account("btc:user1"),
        new Account("eth:user1"));

    when(mockedRocksDBService.getAllObjects(
        eq(mockedAccountCFHandle),
        eq(Account.class),
        eq("accounts"))).thenReturn(expectedAccounts);

    // When
    List<Account> resultAccounts = accountRocksDB.getAllAccounts();

    // Then
    assertNotNull(resultAccounts);
    assertEquals(expectedAccounts.size(), resultAccounts.size());
    assertEquals(expectedAccounts, resultAccounts);
    verify(mockedRocksDBService).getAllObjects(
        eq(mockedAccountCFHandle),
        eq(Account.class),
        eq("accounts"));
  }
}
