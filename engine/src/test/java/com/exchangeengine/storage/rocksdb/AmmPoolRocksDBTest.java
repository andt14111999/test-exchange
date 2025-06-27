package com.exchangeengine.storage.rocksdb;

import com.exchangeengine.model.AmmPool;
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

class AmmPoolRocksDBTest {

  private AmmPoolRocksDB ammPoolRocksDB;

  @Mock
  private RocksDBService mockedRocksDBService;

  @Mock
  private ColumnFamilyHandle mockedAmmPoolCFHandle;

  private MockedStatic<RocksDBService> mockedRocksDBServiceStatic;
  private AutoCloseable closeable;

  @BeforeEach
  void setUp() {
    // Khởi tạo mocks
    closeable = MockitoAnnotations.openMocks(this);

    // Mock static getInstance method của RocksDBService
    mockedRocksDBServiceStatic = Mockito.mockStatic(RocksDBService.class);
    mockedRocksDBServiceStatic.when(RocksDBService::getInstance).thenReturn(mockedRocksDBService);

    // Mock getAmmPoolCF để trả về mock
    when(mockedRocksDBService.getAmmPoolCF()).thenReturn(mockedAmmPoolCFHandle);

    // Reset instance của AmmPoolRocksDB
    AmmPoolRocksDB.resetInstance();

    // Khởi tạo đối tượng test
    ammPoolRocksDB = AmmPoolRocksDB.getInstance();
  }

  @AfterEach
  void tearDown() throws Exception {
    // Reset instance của AmmPoolRocksDB
    AmmPoolRocksDB.resetInstance();

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
    AmmPoolRocksDB instance1 = AmmPoolRocksDB.getInstance();
    AmmPoolRocksDB instance2 = AmmPoolRocksDB.getInstance();

    // Then
    assertNotNull(instance1);
    assertSame(instance1, instance2, "getInstance phải trả về cùng một instance");
  }

  @Test
  void testSaveAmmPool() {
    // Given
    String pair = "BTC/USDT";
    AmmPool ammPool = new AmmPool();
    ammPool.setPair(pair);
    ammPool.setTotalValueLockedToken0(BigDecimal.valueOf(10.0));
    ammPool.setTotalValueLockedToken1(BigDecimal.valueOf(500000.0));

    // When
    ammPoolRocksDB.saveAmmPool(ammPool);

    // Then
    verify(mockedRocksDBService).saveObject(
        eq(ammPool),
        eq(mockedAmmPoolCFHandle),
        any(), // không thể so sánh function reference
        eq("amm_pool"));
  }

  @Test
  void testGetAmmPool() {
    // Given
    String pair = "BTC/USDT";
    AmmPool expectedAmmPool = new AmmPool();
    expectedAmmPool.setPair(pair);
    expectedAmmPool.setTotalValueLockedToken0(BigDecimal.valueOf(10.0));
    expectedAmmPool.setTotalValueLockedToken1(BigDecimal.valueOf(500000.0));

    when(mockedRocksDBService.getObject(
        eq(pair),
        eq(mockedAmmPoolCFHandle),
        eq(AmmPool.class),
        eq("amm_pool"))).thenReturn(Optional.of(expectedAmmPool));

    // When
    Optional<AmmPool> resultAmmPool = ammPoolRocksDB.getAmmPool(pair);

    // Then
    assertTrue(resultAmmPool.isPresent());
    assertEquals(expectedAmmPool, resultAmmPool.get());
  }

  @Test
  void testGetAmmPoolNotFound() {
    // Given
    String pair = "nonexistent";

    when(mockedRocksDBService.getObject(
        eq(pair),
        eq(mockedAmmPoolCFHandle),
        eq(AmmPool.class),
        eq("amm_pool"))).thenReturn(Optional.empty());

    // When
    Optional<AmmPool> resultAmmPool = ammPoolRocksDB.getAmmPool(pair);

    // Then
    assertFalse(resultAmmPool.isPresent());
  }

  @Test
  void testSaveAmmPoolBatch() {
    // Given
    Map<String, AmmPool> ammPools = new HashMap<>();

    AmmPool pool1 = new AmmPool();
    pool1.setPair("BTC/USDT");
    pool1.setTotalValueLockedToken0(BigDecimal.valueOf(10.0));
    pool1.setTotalValueLockedToken1(BigDecimal.valueOf(500000.0));

    AmmPool pool2 = new AmmPool();
    pool2.setPair("ETH/USDT");
    pool2.setTotalValueLockedToken0(BigDecimal.valueOf(100.0));
    pool2.setTotalValueLockedToken1(BigDecimal.valueOf(250000.0));

    ammPools.put(pool1.getPair(), pool1);
    ammPools.put(pool2.getPair(), pool2);

    // When
    ammPoolRocksDB.saveAmmPoolBatch(ammPools);

    // Then
    verify(mockedRocksDBService).saveBatch(
        eq(ammPools),
        eq(mockedAmmPoolCFHandle),
        any(), // không thể so sánh function reference
        eq("amm_pools"));
  }

  @Test
  void testGetAllAmmPools() {
    // Given
    List<AmmPool> expectedPools = Arrays.asList(
        new AmmPool("BTC/USDT"),
        new AmmPool("ETH/USDT"));

    when(mockedRocksDBService.getAllObjects(
        eq(mockedAmmPoolCFHandle),
        eq(AmmPool.class),
        eq("amm_pools"))).thenReturn(expectedPools);

    // When
    List<AmmPool> resultPools = ammPoolRocksDB.getAllAmmPools();

    // Then
    assertNotNull(resultPools);
    assertEquals(expectedPools.size(), resultPools.size());
    assertEquals(expectedPools, resultPools);
    verify(mockedRocksDBService).getAllObjects(
        eq(mockedAmmPoolCFHandle),
        eq(AmmPool.class),
        eq("amm_pools"));
  }
}
