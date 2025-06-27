package com.exchangeengine.storage.rocksdb;

import com.exchangeengine.model.CoinWithdrawal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.rocksdb.ColumnFamilyHandle;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WithdrawalRocksDBTest {

  private WithdrawalRocksDB withdrawalRocksDB;

  @Mock
  private RocksDBService mockedRocksDBService;

  @Mock
  private ColumnFamilyHandle mockedWithdrawalCFHandle;

  private MockedStatic<RocksDBService> mockedRocksDBServiceStatic;
  private AutoCloseable closeable;

  @BeforeEach
  void setUp() {
    // Khởi tạo mocks
    closeable = MockitoAnnotations.openMocks(this);

    // Mock static getInstance method của RocksDBService
    mockedRocksDBServiceStatic = Mockito.mockStatic(RocksDBService.class);
    mockedRocksDBServiceStatic.when(RocksDBService::getInstance).thenReturn(mockedRocksDBService);

    // Mock getWithdrawalCF để trả về mock
    when(mockedRocksDBService.getWithdrawalCF()).thenReturn(mockedWithdrawalCFHandle);

    // Reset instance của WithdrawalRocksDB
    WithdrawalRocksDB.resetInstance();

    // Khởi tạo đối tượng test
    withdrawalRocksDB = WithdrawalRocksDB.getInstance();
  }

  @AfterEach
  void tearDown() throws Exception {
    // Reset instance của WithdrawalRocksDB
    WithdrawalRocksDB.resetInstance();

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
    WithdrawalRocksDB instance1 = WithdrawalRocksDB.getInstance();
    WithdrawalRocksDB instance2 = WithdrawalRocksDB.getInstance();

    // Then
    assertNotNull(instance1);
    assertSame(instance1, instance2, "getInstance phải trả về cùng một instance");
  }

  @Test
  void testSaveWithdrawal() {
    // Given
    String withdrawalId = "withdrawal123";
    CoinWithdrawal withdrawal = new CoinWithdrawal();
    withdrawal.setIdentifier(withdrawalId);
    withdrawal.setAmount(BigDecimal.valueOf(100));
    withdrawal.setAccountKey("btc:user1");

    // When
    withdrawalRocksDB.saveWithdrawal(withdrawal);

    // Then
    verify(mockedRocksDBService).saveObject(
        eq(withdrawal),
        eq(mockedWithdrawalCFHandle),
        any(), // không thể so sánh function reference
        eq("withdrawal"));
  }

  @Test
  void testGetWithdrawal() {
    // Given
    String withdrawalId = "withdrawal123";
    CoinWithdrawal expectedWithdrawal = new CoinWithdrawal();
    expectedWithdrawal.setIdentifier(withdrawalId);
    expectedWithdrawal.setAmount(BigDecimal.valueOf(100));
    expectedWithdrawal.setAccountKey("btc:user1");

    when(mockedRocksDBService.getObject(
        eq(withdrawalId),
        eq(mockedWithdrawalCFHandle),
        eq(CoinWithdrawal.class),
        eq("withdrawal"))).thenReturn(Optional.of(expectedWithdrawal));

    // When
    Optional<CoinWithdrawal> resultWithdrawal = withdrawalRocksDB.getWithdrawal(withdrawalId);

    // Then
    assertTrue(resultWithdrawal.isPresent());
    assertEquals(expectedWithdrawal, resultWithdrawal.get());
    verify(mockedRocksDBService).getObject(
        eq(withdrawalId),
        eq(mockedWithdrawalCFHandle),
        eq(CoinWithdrawal.class),
        eq("withdrawal"));
  }

  @Test
  void testGetWithdrawalNotFound() {
    // Given
    String withdrawalId = "nonexistent";

    when(mockedRocksDBService.getObject(
        eq(withdrawalId),
        eq(mockedWithdrawalCFHandle),
        eq(CoinWithdrawal.class),
        eq("withdrawal"))).thenReturn(Optional.empty());

    // When
    Optional<CoinWithdrawal> resultWithdrawal = withdrawalRocksDB.getWithdrawal(withdrawalId);

    // Then
    assertFalse(resultWithdrawal.isPresent());
  }

  @Test
  void testSaveWithdrawalBatch() {
    // Given
    Map<String, CoinWithdrawal> withdrawals = new HashMap<>();

    CoinWithdrawal withdrawal1 = new CoinWithdrawal();
    withdrawal1.setIdentifier("withdrawal1");
    withdrawal1.setAmount(BigDecimal.valueOf(50));
    withdrawal1.setAccountKey("btc:user1");

    CoinWithdrawal withdrawal2 = new CoinWithdrawal();
    withdrawal2.setIdentifier("withdrawal2");
    withdrawal2.setAmount(BigDecimal.valueOf(100));
    withdrawal2.setAccountKey("eth:user1");

    withdrawals.put(withdrawal1.getIdentifier(), withdrawal1);
    withdrawals.put(withdrawal2.getIdentifier(), withdrawal2);

    // When
    withdrawalRocksDB.saveWithdrawalBatch(withdrawals);

    // Then
    verify(mockedRocksDBService).saveBatch(
        eq(withdrawals),
        eq(mockedWithdrawalCFHandle),
        any(), // không thể so sánh function reference
        eq("withdrawals"));
  }

  @Test
  void testGetAllWithdrawals() {
    // Given
    CoinWithdrawal withdrawal1 = new CoinWithdrawal();
    withdrawal1.setIdentifier("withdrawal1");
    withdrawal1.setAmount(BigDecimal.valueOf(50));
    withdrawal1.setAccountKey("btc:user1");

    CoinWithdrawal withdrawal2 = new CoinWithdrawal();
    withdrawal2.setIdentifier("withdrawal2");
    withdrawal2.setAmount(BigDecimal.valueOf(100));
    withdrawal2.setAccountKey("eth:user1");

    List<CoinWithdrawal> expectedWithdrawals = List.of(withdrawal1, withdrawal2);

    when(mockedRocksDBService.getAllObjects(
        eq(mockedWithdrawalCFHandle),
        eq(CoinWithdrawal.class),
        eq("withdrawals"))).thenReturn(expectedWithdrawals);

    // When
    List<CoinWithdrawal> resultWithdrawals = withdrawalRocksDB.getAllWithdrawals();

    // Then
    assertEquals(expectedWithdrawals, resultWithdrawals);
    verify(mockedRocksDBService).getAllObjects(
        eq(mockedWithdrawalCFHandle),
        eq(CoinWithdrawal.class),
        eq("withdrawals"));
  }
}
