package com.exchangeengine.storage.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.exchangeengine.extension.CombinedTestExtension;

import com.exchangeengine.model.AmmPosition;
import com.exchangeengine.factory.AmmPositionFactory;
import com.exchangeengine.storage.rocksdb.AmmPositionRocksDB;

/**
 * Test for AmmPositionCache
 */
@ExtendWith({ MockitoExtension.class, CombinedTestExtension.class })
public class AmmPositionCacheTest {

  private AmmPositionCache ammPositionCache;
  private AutoCloseable closeable;

  @Mock
  private AmmPositionRocksDB mockAmmPositionRocksDB;

  @BeforeEach
  public void setUp() throws Exception {
    closeable = MockitoAnnotations.openMocks(this);

    // Reset Singleton instance
    Field instanceField = AmmPositionCache.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);

    // Mock AmmPositionRocksDB instance
    Field rockDBField = AmmPositionRocksDB.class.getDeclaredField("instance");
    rockDBField.setAccessible(true);
    rockDBField.set(null, mockAmmPositionRocksDB);

    // Mock behavior - thêm lenient() để tránh UnnecessaryStubbingException
    lenient().when(mockAmmPositionRocksDB.getAllAmmPositions()).thenReturn(new ArrayList<>());

    // Get the instance
    ammPositionCache = AmmPositionCache.getInstance();

    // Clear the cache
    Field cacheField = AmmPositionCache.class.getDeclaredField("ammPositionCache");
    cacheField.setAccessible(true);
    ConcurrentHashMap<String, AmmPosition> cache = (ConcurrentHashMap<String, AmmPosition>) cacheField
        .get(ammPositionCache);
    cache.clear();

    // Clear latest positions
    Field latestPositionsField = AmmPositionCache.class.getDeclaredField("latestAmmPositions");
    latestPositionsField.setAccessible(true);
    ConcurrentHashMap<String, AmmPosition> latestPositions = (ConcurrentHashMap<String, AmmPosition>) latestPositionsField
        .get(ammPositionCache);
    latestPositions.clear();
  }

  @AfterEach
  public void tearDown() throws Exception {
    closeable.close();

    // Reset Singleton instance
    Field instanceField = AmmPositionCache.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);

    // Reset RocksDB instance
    Field rockDBField = AmmPositionRocksDB.class.getDeclaredField("instance");
    rockDBField.setAccessible(true);
    rockDBField.set(null, null);
  }

  @Test
  @DisplayName("getInstance_ReturnsSameInstance")
  public void testGetInstance_ReturnsSameInstance() {
    // Act
    AmmPositionCache instance1 = AmmPositionCache.getInstance();
    AmmPositionCache instance2 = AmmPositionCache.getInstance();

    // Assert
    assertSame(instance1, instance2, "getInstance() should always return the same instance");
  }

  @Test
  @DisplayName("getAmmPosition_NotExists")
  public void testGetAmmPosition_NotExists() {
    // Act
    Optional<AmmPosition> result = ammPositionCache.getAmmPosition("nonexistent");

    // Assert
    assertFalse(result.isPresent(), "Should return empty optional when position doesn't exist");
  }

  @Test
  @DisplayName("getAmmPosition_Exists")
  public void testGetAmmPosition_Exists() {
    // Arrange
    AmmPosition position = AmmPositionFactory.createDefaultAmmPosition();
    ammPositionCache.updateAmmPosition(position);

    // Act
    Optional<AmmPosition> result = ammPositionCache.getAmmPosition(position.getIdentifier());

    // Assert
    assertTrue(result.isPresent(), "Should return position when it exists");
    assertEquals(position.getIdentifier(), result.get().getIdentifier(), "Should return correct position");
  }

  @Test
  @DisplayName("getOrInitAmmPosition_NotExists")
  public void testGetOrInitAmmPosition_NotExists() {
    // Arrange
    String identifier = "00000000-0000-0000-0000-000000000001";
    String poolPair = "BTC/USDT";

    // Act
    AmmPosition result = ammPositionCache.getOrInitAmmPosition(identifier, poolPair);

    // Assert
    assertNotNull(result, "Should return a non-null position");
    assertEquals(identifier, result.getIdentifier(), "Should return position with correct identifier");
    assertEquals(poolPair, result.getPoolPair(), "Should return position with correct poolPair");
    assertFalse(ammPositionCache.getAmmPosition(identifier).isPresent(),
        "Should not store the initialized position in cache");
  }

  @Test
  @DisplayName("getOrCreateAmmPosition_NotExists")
  public void testGetOrCreateAmmPosition_NotExists() {
    // Arrange
    String identifier = "00000000-0000-0000-0000-000000000001";
    String poolPair = "BTC/USDT";

    // Act
    AmmPosition result = ammPositionCache.getOrCreateAmmPosition(identifier, poolPair);

    // Assert
    assertNotNull(result, "Should return a non-null position");
    assertEquals(identifier, result.getIdentifier(), "Should return position with correct identifier");
    assertEquals(poolPair, result.getPoolPair(), "Should return position with correct poolPair");
    assertTrue(ammPositionCache.getAmmPosition(identifier).isPresent(), "Should store the created position in cache");
  }

  @Test
  @DisplayName("getOrCreateAmmPosition_Exists")
  public void testGetOrCreateAmmPosition_Exists() {
    // Arrange
    AmmPosition position = AmmPositionFactory.createCustomAmmPosition(Map.of(
        "poolPair", "USDT-VND",
        "ownerAccountKey0", "account1",
        "ownerAccountKey1", "account1",
        "tickLowerIndex", -200,
        "tickUpperIndex", 200));
    ammPositionCache.updateAmmPosition(position);

    // Act
    AmmPosition result = ammPositionCache.getAmmPosition(position.getIdentifier()).get();

    // Assert
    assertEquals(-200, result.getTickLowerIndex(), "Should return existing position with properties preserved");
    assertEquals(200, result.getTickUpperIndex(), "Should return existing position with properties preserved");
  }

  @Test
  @DisplayName("updateAmmPosition_ShouldStoreUpdatedPositionInCache")
  public void testUpdateAmmPosition() {
    // Arrange
    AmmPosition position = AmmPositionFactory.createDefaultAmmPosition();

    // Act
    ammPositionCache.updateAmmPosition(position);

    // Assert
    assertTrue(ammPositionCache.getAmmPosition(position.getIdentifier()).isPresent(),
        "Should store the updated position in cache");
  }

  @Test
  @DisplayName("addAmmPositionToBatch_ShouldHandleAllCasesCorrectly")
  public void testAddAmmPositionToBatch() {
    // Case 1: Add position with null identifier (should be ignored)
    AmmPosition nullIdPosition = AmmPositionFactory.createDefaultAmmPosition();
    nullIdPosition.setIdentifier(null);
    ammPositionCache.addAmmPositionToBatch(nullIdPosition);

    // Case 2: Add first position for a specific identifier
    AmmPosition firstPosition = AmmPositionFactory.createDefaultAmmPosition();
    firstPosition.setIdentifier("00000000-0000-0000-0000-000000000001");
    firstPosition.setUpdatedAt(1000L);
    ammPositionCache.addAmmPositionToBatch(firstPosition);

    // Case 3: Add first position for another identifier
    AmmPosition secondPosition = AmmPositionFactory.createDefaultAmmPosition();
    secondPosition.setIdentifier("00000000-0000-0000-0000-000000000002");
    secondPosition.setUpdatedAt(1500L);
    ammPositionCache.addAmmPositionToBatch(secondPosition);

    // Case 4: Add newer position for first identifier
    AmmPosition newerPosition = AmmPositionFactory.createDefaultAmmPosition();
    newerPosition.setIdentifier("00000000-0000-0000-0000-000000000001");
    newerPosition.setUpdatedAt(2000L);
    ammPositionCache.addAmmPositionToBatch(newerPosition);

    // Case 5: Add older position for first identifier (should be ignored)
    AmmPosition olderPosition = AmmPositionFactory.createDefaultAmmPosition();
    olderPosition.setIdentifier("00000000-0000-0000-0000-000000000001");
    olderPosition.setUpdatedAt(1500L);
    ammPositionCache.addAmmPositionToBatch(olderPosition);

    // Case 6: Add position with same timestamp for first identifier (should keep
    // existing)
    AmmPosition sameTimePosition = AmmPositionFactory.createDefaultAmmPosition();
    sameTimePosition.setIdentifier("00000000-0000-0000-0000-000000000001");
    sameTimePosition.setUpdatedAt(2000L);
    ammPositionCache.addAmmPositionToBatch(sameTimePosition);

    // Verify final state
    try {
      Field latestPositionsField = AmmPositionCache.class.getDeclaredField("latestAmmPositions");
      latestPositionsField.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<String, AmmPosition> latestPositions = (Map<String, AmmPosition>) latestPositionsField.get(ammPositionCache);

      // Verify total number of positions
      assertEquals(2, latestPositions.size(), "Should have two positions with different identifiers");

      // Verify first position
      AmmPosition firstStoredPosition = latestPositions.get("00000000-0000-0000-0000-000000000001");
      assertNotNull(firstStoredPosition, "Should have first position");
      assertEquals(2000L, firstStoredPosition.getUpdatedAt(), "Should keep the newest first position");
      assertSame(newerPosition, firstStoredPosition, "Should be the same instance as newerPosition");

      // Verify second position
      AmmPosition secondStoredPosition = latestPositions.get("00000000-0000-0000-0000-000000000002");
      assertNotNull(secondStoredPosition, "Should have second position");
      assertEquals(1500L, secondStoredPosition.getUpdatedAt(), "Should keep second position timestamp");
      assertSame(secondPosition, secondStoredPosition, "Should be the same instance as secondPosition");
    } catch (Exception e) {
      fail("Failed to access latestAmmPositions field: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("initializeAmmPositionCache_WithException")
  public void testInitializeAmmPositionCache_WithException() {
    // Arrange
    when(mockAmmPositionRocksDB.getAllAmmPositions()).thenThrow(new RuntimeException("DB Error"));

    // Act
    ammPositionCache.initializeAmmPositionCache();

    // Assert - verify that the cache remains functional
    AmmPosition position = AmmPositionFactory.createDefaultAmmPosition();
    ammPositionCache.updateAmmPosition(position);
    assertTrue(ammPositionCache.getAmmPosition(position.getIdentifier()).isPresent(),
        "Cache should still be functional after initialization error");
  }

  @Test
  @DisplayName("initializeAmmPositionCache_ShouldUpdateCacheWithPositionsFromDB")
  public void testInitializeAmmPositionCache() {
    // Arrange
    List<AmmPosition> mockPositions = new ArrayList<>();
    AmmPosition validPosition = AmmPositionFactory.createDefaultAmmPosition();
    AmmPosition invalidPosition = AmmPositionFactory.createDefaultAmmPosition();
    invalidPosition.setIdentifier("");
    mockPositions.add(validPosition);
    mockPositions.add(invalidPosition);
    when(mockAmmPositionRocksDB.getAllAmmPositions()).thenReturn(mockPositions);

    // Act
    ammPositionCache.initializeAmmPositionCache();

    // Assert
    assertTrue(ammPositionCache.getAmmPosition(validPosition.getIdentifier()).isPresent(),
        "Valid position should be in cache");
    assertFalse(ammPositionCache.getAmmPosition("").isPresent(),
        "Invalid position should not be in cache");
  }

  @Test
  @DisplayName("flushAmmPositionToDisk_WithEmptyBatch")
  public void testFlushAmmPositionToDisk_WithEmptyBatch() {
    // Act
    ammPositionCache.flushAmmPositionToDisk();

    // Assert
    verify(mockAmmPositionRocksDB, never()).saveAmmPositionBatch(any());
  }

  @Test
  @DisplayName("flushAmmPositionToDisk_ShouldSavePositionsToRocksDB")
  public void testFlushAmmPositionToDisk() {
    // Arrange
    AmmPosition position = AmmPositionFactory.createDefaultAmmPosition();
    ammPositionCache.addAmmPositionToBatch(position);

    // Act
    ammPositionCache.flushAmmPositionToDisk();

    // Assert
    verify(mockAmmPositionRocksDB).saveAmmPositionBatch(any());
  }

  @Test
  @DisplayName("ammPositionCacheShouldFlush_ShouldHandleAllCasesCorrectly")
  public void testAmmPositionCacheShouldFlush() {
    // Trường hợp 1: Ban đầu khi chưa có bản ghi nào được cập nhật (counter = 0)
    assertFalse(ammPositionCache.ammPositionCacheShouldFlush(),
        "Không nên flush khi counter = 0");

    // Lấy tham chiếu đến counter để theo dõi giá trị
    try {
      Field updateCounterField = AmmPositionCache.class.getDeclaredField("updateCounter");
      updateCounterField.setAccessible(true);
      AtomicInteger counter = (AtomicInteger) updateCounterField.get(ammPositionCache);

      // Trường hợp 2: Tăng counter lên một giá trị nhỏ hơn threshold
      // Set counter = 50
      counter.set(50);
      assertFalse(ammPositionCache.ammPositionCacheShouldFlush(),
          "Không nên flush khi counter < threshold (50 < 100)");

      // Trường hợp 3: Tăng counter đến đúng ngưỡng threshold
      // Set counter = 100
      counter.set(100);
      assertTrue(ammPositionCache.ammPositionCacheShouldFlush(),
          "Nên flush khi counter = threshold (100 = 100)");

      // Trường hợp 4: Tăng counter sau ngưỡng threshold
      // Set counter = 101
      counter.set(101);
      assertFalse(ammPositionCache.ammPositionCacheShouldFlush(),
          "Không nên flush khi counter > threshold nhưng không chia hết (101 % 100 != 0)");

      // Trường hợp 5: Tăng counter đến bội số tiếp theo của threshold
      // Set counter = 200
      counter.set(200);
      assertTrue(ammPositionCache.ammPositionCacheShouldFlush(),
          "Nên flush khi counter là bội số của threshold (200 % 100 = 0)");

    } catch (Exception e) {
      fail("Không thể truy cập field updateCounter: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("initializeAmmPositionCache_WithException should skip amm positions with null or empty identifier")
  void testInitializeAmmPositionCache_WithException_ShouldSkipAmmPositions_WithNullOrEmptyIdentifier()
      throws Exception {
    // Arrange
    AmmPosition nullIdAmmPosition = new AmmPosition();
    nullIdAmmPosition.setIdentifier(null);

    AmmPosition emptyIdAmmPosition = new AmmPosition();
    emptyIdAmmPosition.setIdentifier("");

    // Quan trọng: Đảm bảo mock trả về đúng danh sách position
    when(mockAmmPositionRocksDB.getAllAmmPositions()).thenReturn(List.of(nullIdAmmPosition, emptyIdAmmPosition));

    // Thêm một position hợp lệ vào cache trước khi test
    AmmPosition validPosition = AmmPositionFactory.createDefaultAmmPosition();
    ammPositionCache.updateAmmPosition(validPosition);

    // Act: Clear cache trước để đảm bảo chỉ có dữ liệu từ mock
    Field cacheField = AmmPositionCache.class.getDeclaredField("ammPositionCache");
    cacheField.setAccessible(true);
    Map<String, AmmPosition> cache = (Map<String, AmmPosition>) cacheField.get(ammPositionCache);
    cache.clear();

    // Gọi phương thức cần test
    ammPositionCache.initializeAmmPositionCache();

    // Assert - kiểm tra kích thước cache
    assertEquals(0, cache.size(),
        "Cache nên trống vì cả hai position đều có identifier null hoặc rỗng");
  }
}
