package com.exchangeengine.storage.rocksdb;

import com.exchangeengine.model.CoinDeposit;
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

class DepositRocksDBTest {

  private DepositRocksDB depositRocksDB;

  @Mock
  private RocksDBService mockedRocksDBService;

  @Mock
  private ColumnFamilyHandle mockedDepositCFHandle;

  private MockedStatic<RocksDBService> mockedRocksDBServiceStatic;
  private AutoCloseable closeable;

  @BeforeEach
  void setUp() {
    // Khởi tạo mocks
    closeable = MockitoAnnotations.openMocks(this);

    // Mock static getInstance method của RocksDBService
    mockedRocksDBServiceStatic = Mockito.mockStatic(RocksDBService.class);
    mockedRocksDBServiceStatic.when(RocksDBService::getInstance).thenReturn(mockedRocksDBService);

    // Mock getDepositCF để trả về mock
    when(mockedRocksDBService.getDepositCF()).thenReturn(mockedDepositCFHandle);

    // Reset instance của DepositRocksDB
    DepositRocksDB.resetInstance();

    // Khởi tạo đối tượng test
    depositRocksDB = DepositRocksDB.getInstance();
  }

  @AfterEach
  void tearDown() throws Exception {
    // Reset instance của DepositRocksDB
    DepositRocksDB.resetInstance();

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
    DepositRocksDB instance1 = DepositRocksDB.getInstance();
    DepositRocksDB instance2 = DepositRocksDB.getInstance();

    // Then
    assertNotNull(instance1);
    assertSame(instance1, instance2, "getInstance phải trả về cùng một instance");
  }

  @Test
  void testSaveDeposit() {
    // Given
    String depositId = "deposit123";
    CoinDeposit deposit = new CoinDeposit();
    deposit.setIdentifier(depositId);
    deposit.setAmount(BigDecimal.valueOf(100));
    deposit.setAccountKey("btc:user1");

    // When
    depositRocksDB.saveDeposit(deposit);

    // Then
    verify(mockedRocksDBService).saveObject(
        eq(deposit),
        eq(mockedDepositCFHandle),
        any(), // không thể so sánh function reference
        eq("deposit"));
  }

  @Test
  void testGetDeposit() {
    // Given
    String depositId = "deposit123";
    CoinDeposit expectedDeposit = new CoinDeposit();
    expectedDeposit.setIdentifier(depositId);
    expectedDeposit.setAmount(BigDecimal.valueOf(100));
    expectedDeposit.setAccountKey("btc:user1");

    when(mockedRocksDBService.getObject(
        eq(depositId),
        eq(mockedDepositCFHandle),
        eq(CoinDeposit.class),
        eq("deposit"))).thenReturn(Optional.of(expectedDeposit));

    // When
    Optional<CoinDeposit> resultDeposit = depositRocksDB.getDeposit(depositId);

    // Then
    assertTrue(resultDeposit.isPresent());
    assertEquals(expectedDeposit, resultDeposit.get());
  }

  @Test
  void testGetDepositNotFound() {
    // Given
    String depositId = "nonexistent";

    when(mockedRocksDBService.getObject(
        eq(depositId),
        eq(mockedDepositCFHandle),
        eq(CoinDeposit.class),
        eq("deposit"))).thenReturn(Optional.empty());

    // When
    Optional<CoinDeposit> resultDeposit = depositRocksDB.getDeposit(depositId);

    // Then
    assertFalse(resultDeposit.isPresent());
  }

  @Test
  void testSaveDepositBatch() {
    // Given
    Map<String, CoinDeposit> deposits = new HashMap<>();

    CoinDeposit deposit1 = new CoinDeposit();
    deposit1.setIdentifier("deposit1");
    deposit1.setAmount(BigDecimal.valueOf(50));
    deposit1.setAccountKey("btc:user1");

    CoinDeposit deposit2 = new CoinDeposit();
    deposit2.setIdentifier("deposit2");
    deposit2.setAmount(BigDecimal.valueOf(100));
    deposit2.setAccountKey("eth:user1");

    deposits.put(deposit1.getIdentifier(), deposit1);
    deposits.put(deposit2.getIdentifier(), deposit2);

    // When
    depositRocksDB.saveDepositBatch(deposits);

    // Then
    verify(mockedRocksDBService).saveBatch(
        eq(deposits),
        eq(mockedDepositCFHandle),
        any(), // không thể so sánh function reference
        eq("deposits"));
  }

  @Test
  void testGetAllDeposits() {
    // Given
    CoinDeposit deposit1 = new CoinDeposit();
    deposit1.setIdentifier("deposit1");
    deposit1.setAmount(BigDecimal.valueOf(50));
    deposit1.setAccountKey("btc:user1");

    CoinDeposit deposit2 = new CoinDeposit();
    deposit2.setIdentifier("deposit2");
    deposit2.setAmount(BigDecimal.valueOf(100));
    deposit2.setAccountKey("eth:user1");

    List<CoinDeposit> expectedDeposits = List.of(deposit1, deposit2);

    when(mockedRocksDBService.getAllObjects(
        eq(mockedDepositCFHandle),
        eq(CoinDeposit.class),
        eq("deposits"))).thenReturn(expectedDeposits);

    // When
    List<CoinDeposit> resultDeposits = depositRocksDB.getAllDeposits();

    // Then
    assertEquals(expectedDeposits, resultDeposits);
    verify(mockedRocksDBService).getAllObjects(
        eq(mockedDepositCFHandle),
        eq(CoinDeposit.class),
        eq("deposits"));
  }
}
