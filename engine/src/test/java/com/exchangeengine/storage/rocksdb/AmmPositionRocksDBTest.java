package com.exchangeengine.storage.rocksdb;

import com.exchangeengine.model.AmmPosition;
import com.exchangeengine.factory.AmmPositionFactory;

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

class AmmPositionRocksDBTest {

  private AmmPositionRocksDB ammPositionRocksDB;

  @Mock
  private RocksDBService mockedRocksDBService;

  @Mock
  private ColumnFamilyHandle mockedAmmPositionCFHandle;

  private MockedStatic<RocksDBService> mockedRocksDBServiceStatic;
  private AutoCloseable closeable;

  @BeforeEach
  void setUp() {
    // Khởi tạo mocks
    closeable = MockitoAnnotations.openMocks(this);

    // Mock static getInstance method của RocksDBService
    mockedRocksDBServiceStatic = Mockito.mockStatic(RocksDBService.class);
    mockedRocksDBServiceStatic.when(RocksDBService::getInstance).thenReturn(mockedRocksDBService);

    // Mock getAmmPositionCF để trả về mock
    when(mockedRocksDBService.getAmmPositionCF()).thenReturn(mockedAmmPositionCFHandle);

    // Reset instance của AmmPositionRocksDB
    AmmPositionRocksDB.resetInstance();

    // Khởi tạo đối tượng test
    ammPositionRocksDB = AmmPositionRocksDB.getInstance();
  }

  @AfterEach
  void tearDown() throws Exception {
    // Reset instance của AmmPositionRocksDB
    AmmPositionRocksDB.resetInstance();

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
    AmmPositionRocksDB instance1 = AmmPositionRocksDB.getInstance();
    AmmPositionRocksDB instance2 = AmmPositionRocksDB.getInstance();

    // Then
    assertNotNull(instance1);
    assertSame(instance1, instance2, "getInstance phải trả về cùng một instance");
  }

  @Test
  void testSaveAmmPosition() {
    // Given
    String identifier = "00000000-0000-0000-0000-000000000001";
    AmmPosition ammPosition = AmmPositionFactory.createDefaultAmmPosition();
    ammPosition.setIdentifier(identifier);

    // When
    ammPositionRocksDB.saveAmmPosition(ammPosition);

    // Then
    verify(mockedRocksDBService).saveObject(
        eq(ammPosition),
        eq(mockedAmmPositionCFHandle),
        any(), // không thể so sánh function reference
        eq("amm_position"));
  }

  @Test
  void testGetAmmPosition() {
    // Given
    String identifier = "00000000-0000-0000-0000-000000000001";
    AmmPosition expectedAmmPosition = AmmPositionFactory.createDefaultAmmPosition();
    expectedAmmPosition.setIdentifier(identifier);

    when(mockedRocksDBService.getObject(
        eq(identifier),
        eq(mockedAmmPositionCFHandle),
        eq(AmmPosition.class),
        eq("amm_position"))).thenReturn(Optional.of(expectedAmmPosition));

    // When
    Optional<AmmPosition> resultAmmPosition = ammPositionRocksDB.getAmmPosition(identifier);

    // Then
    assertTrue(resultAmmPosition.isPresent());
    assertEquals(expectedAmmPosition, resultAmmPosition.get());
  }

  @Test
  void testGetAmmPositionNotFound() {
    // Given
    String identifier = "nonexistent";

    when(mockedRocksDBService.getObject(
        eq(identifier),
        eq(mockedAmmPositionCFHandle),
        eq(AmmPosition.class),
        eq("amm_position"))).thenReturn(Optional.empty());

    // When
    Optional<AmmPosition> resultAmmPosition = ammPositionRocksDB.getAmmPosition(identifier);

    // Then
    assertFalse(resultAmmPosition.isPresent());
  }

  @Test
  void testSaveAmmPositionBatch() {
    // Given
    Map<String, AmmPosition> ammPositions = new HashMap<>();

    AmmPosition position1 = AmmPositionFactory.createDefaultAmmPosition();
    position1.setIdentifier("00000000-0000-0000-0000-000000000001");
    position1.setPoolPair("BTC/USDT");
    position1.setLiquidity(new BigDecimal("10000"));

    AmmPosition position2 = AmmPositionFactory.createDefaultAmmPosition();
    position2.setIdentifier("00000000-0000-0000-0000-000000000002");
    position2.setPoolPair("ETH/USDT");
    position2.setLiquidity(new BigDecimal("20000"));

    ammPositions.put(position1.getIdentifier(), position1);
    ammPositions.put(position2.getIdentifier(), position2);

    // When
    ammPositionRocksDB.saveAmmPositionBatch(ammPositions);

    // Then
    verify(mockedRocksDBService).saveBatch(
        eq(ammPositions),
        eq(mockedAmmPositionCFHandle),
        any(), // không thể so sánh function reference
        eq("amm_positions"));
  }

  @Test
  void testGetAllAmmPositions() {
    // Given
    List<AmmPosition> expectedPositions = Arrays.asList(
        AmmPositionFactory.createCustomAmmPosition(Map.of(
            "poolPair", "BTC/USDT",
            "ownerAccountKey0", "account1",
            "ownerAccountKey1", "account1")),
        AmmPositionFactory.createCustomAmmPosition(Map.of(
            "poolPair", "ETH/USDT",
            "ownerAccountKey0", "account2",
            "ownerAccountKey1", "account2")));

    when(mockedRocksDBService.getAllObjects(
        eq(mockedAmmPositionCFHandle),
        eq(AmmPosition.class),
        eq("amm_positions"))).thenReturn(expectedPositions);

    // When
    List<AmmPosition> resultPositions = ammPositionRocksDB.getAllAmmPositions();

    // Then
    assertNotNull(resultPositions);
    assertEquals(expectedPositions.size(), resultPositions.size());
    assertEquals(expectedPositions, resultPositions);
    verify(mockedRocksDBService).getAllObjects(
        eq(mockedAmmPositionCFHandle),
        eq(AmmPosition.class),
        eq("amm_positions"));
  }

  @Test
  void testGetAmmPositionsByPool() {
    // Given
    String pool = "BTC/USDT";
    int limit = 10;
    String lastIdentifier = "00000000-0000-0000-0000-000000000001";

    List<AmmPosition> expectedPositions = Arrays.asList(
        AmmPositionFactory.createCustomAmmPosition(Map.of(
            "poolPair", "BTC/USDT",
            "ownerAccountKey0", "account1",
            "ownerAccountKey1", "account1")),
        AmmPositionFactory.createCustomAmmPosition(Map.of(
            "poolPair", "BTC/USDT",
            "ownerAccountKey0", "account2",
            "ownerAccountKey1", "account2")));

    when(mockedRocksDBService.getObjectsByPrefix(
        eq(pool + ":"),
        eq(limit),
        eq(lastIdentifier),
        eq(mockedAmmPositionCFHandle),
        eq(AmmPosition.class),
        eq("amm_positions_by_pool"))).thenReturn(expectedPositions);

    // When
    List<AmmPosition> resultPositions = ammPositionRocksDB.getAmmPositionsByPool(pool, limit, lastIdentifier);

    // Then
    assertNotNull(resultPositions);
    assertEquals(expectedPositions.size(), resultPositions.size());
    assertEquals(expectedPositions, resultPositions);
    verify(mockedRocksDBService).getObjectsByPrefix(
        eq(pool + ":"),
        eq(limit),
        eq(lastIdentifier),
        eq(mockedAmmPositionCFHandle),
        eq(AmmPosition.class),
        eq("amm_positions_by_pool"));
  }
}
