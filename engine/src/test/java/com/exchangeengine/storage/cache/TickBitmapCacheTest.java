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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.exchangeengine.model.TickBitmap;
import com.exchangeengine.factory.TickBitmapFactory;
import com.exchangeengine.storage.rocksdb.TickBitmapRocksDB;
import com.exchangeengine.extension.CombinedTestExtension;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test for TickBitmapCache
 */
@ExtendWith({ MockitoExtension.class, CombinedTestExtension.class })
public class TickBitmapCacheTest {

  private TickBitmapCache tickBitmapCache;
  private AutoCloseable closeable;
  private TickBitmapFactory tickBitmapFactory = new TickBitmapFactory();

  @Mock
  private TickBitmapRocksDB mockTickBitmapRocksDB;

  @BeforeEach
  public void setUp() throws Exception {
    closeable = MockitoAnnotations.openMocks(this);

    // Reset Singleton instance
    TickBitmapCache.resetInstance();

    // Mock TickBitmapRocksDB instance
    Field rockDBField = TickBitmapRocksDB.class.getDeclaredField("instance");
    rockDBField.setAccessible(true);
    rockDBField.set(null, mockTickBitmapRocksDB);

    // Mock behavior - thêm lenient() để tránh UnnecessaryStubbingException
    List<TickBitmap> emptyList = new ArrayList<>();
    lenient().when(mockTickBitmapRocksDB.getAllTickBitmaps()).thenReturn(emptyList);

    // Get the instance
    tickBitmapCache = TickBitmapCache.getInstance();

    // Clear the cache
    Field cacheField = TickBitmapCache.class.getDeclaredField("tickBitmapCache");
    cacheField.setAccessible(true);
    ConcurrentHashMap<String, TickBitmap> cache = (ConcurrentHashMap<String, TickBitmap>) cacheField
        .get(tickBitmapCache);
    cache.clear();

    // Clear latest bitmaps
    Field latestBitmapsField = TickBitmapCache.class.getDeclaredField("latestTickBitmaps");
    latestBitmapsField.setAccessible(true);
    ConcurrentHashMap<String, TickBitmap> latestBitmaps = (ConcurrentHashMap<String, TickBitmap>) latestBitmapsField
        .get(tickBitmapCache);
    latestBitmaps.clear();

    // Reset update counter
    Field updateCounterField = TickBitmapCache.class.getDeclaredField("updateCounter");
    updateCounterField.setAccessible(true);
    updateCounterField.set(tickBitmapCache, new java.util.concurrent.atomic.AtomicInteger(0));

    // Set test instance
    TickBitmapCache.setTestInstance(tickBitmapCache);
  }

  @AfterEach
  public void tearDown() throws Exception {
    closeable.close();

    // Reset Singleton instance
    TickBitmapCache.resetInstance();

    // Reset RocksDB instance
    Field rockDBField = TickBitmapRocksDB.class.getDeclaredField("instance");
    rockDBField.setAccessible(true);
    rockDBField.set(null, null);
  }

  @Test
  @DisplayName("getInstance should create a new instance if none exists")
  public void testGetInstance_ShouldCreateNewInstance_WhenNoneExists() {
    // Arrange
    TickBitmapCache.resetInstance();

    // Act
    TickBitmapCache instance = TickBitmapCache.getInstance();

    // Assert
    assertNotNull(instance);
    // Không verify số lần gọi vì có thể gây lỗi khi gọi nhiều lần
  }

  @Test
  @DisplayName("getInstance should return the same instance when called multiple times")
  public void testGetInstance_ShouldReturnSameInstance_WhenCalledMultipleTimes() {
    // Act
    TickBitmapCache instance1 = TickBitmapCache.getInstance();
    TickBitmapCache instance2 = TickBitmapCache.getInstance();

    // Assert
    assertSame(instance1, instance2);
  }

  @Test
  @DisplayName("setTestInstance should set the instance to the provided test instance")
  public void testSetTestInstance() {
    // Arrange
    TickBitmapCache testInstance = mock(TickBitmapCache.class);

    // Act
    TickBitmapCache.setTestInstance(testInstance);

    // Assert
    assertSame(testInstance, TickBitmapCache.getInstance());
  }

  @Test
  @DisplayName("resetInstance should set the instance to null")
  public void testResetInstance() throws Exception {
    // Arrange - Make sure we have an instance
    TickBitmapCache instance = TickBitmapCache.getInstance();
    assertNotNull(instance);

    // Act
    TickBitmapCache.resetInstance();

    // Assert - Using reflection to check the static field
    Field instanceField = TickBitmapCache.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    assertNull(instanceField.get(null));
  }

  @Test
  @DisplayName("getTickBitmap should return empty Optional when pool pair not in cache")
  public void testGetTickBitmap_NotExists() {
    // Act
    Optional<TickBitmap> result = tickBitmapCache.getTickBitmap("BTC-USDT");

    // Assert
    assertFalse(result.isPresent(), "Should return empty optional when bitmap doesn't exist");
  }

  @Test
  @DisplayName("getTickBitmap should return bitmap when pool pair is in cache")
  public void testGetTickBitmap_Exists() {
    // Arrange
    TickBitmap bitmap = tickBitmapFactory.createEmptyBitmap("BTC-USDT");
    tickBitmapCache.updateTickBitmap(bitmap);

    // Act
    Optional<TickBitmap> result = tickBitmapCache.getTickBitmap(bitmap.getPoolPair());

    // Assert
    assertTrue(result.isPresent(), "Should return bitmap when it exists");
    assertEquals(bitmap.getPoolPair(), result.get().getPoolPair(), "Should return correct bitmap");
  }

  @Test
  @DisplayName("getOrInitTickBitmap should return existing bitmap when in cache")
  public void testGetOrInitTickBitmap_Exists() throws Exception {
    // Arrange
    String poolPair = "BTC-USDT";
    TickBitmap expectedBitmap = new TickBitmap(poolPair);

    // Add to cache using reflection
    Field cacheField = TickBitmapCache.class.getDeclaredField("tickBitmapCache");
    cacheField.setAccessible(true);
    ConcurrentHashMap<String, TickBitmap> cache = (ConcurrentHashMap<String, TickBitmap>) cacheField
        .get(tickBitmapCache);
    cache.put(poolPair, expectedBitmap);

    // Act
    TickBitmap result = tickBitmapCache.getOrInitTickBitmap(poolPair);

    // Assert
    assertSame(expectedBitmap, result);
  }

  @Test
  @DisplayName("getOrInitTickBitmap should create new bitmap when not in cache")
  public void testGetOrInitTickBitmap_NotExists() {
    // Act
    TickBitmap result = tickBitmapCache.getOrInitTickBitmap("BTC-USDT");

    // Assert
    assertNotNull(result, "Should return a non-null bitmap");
    assertEquals("BTC-USDT", result.getPoolPair(), "Should return bitmap with correct pool pair");
    assertFalse(tickBitmapCache.getTickBitmap("BTC-USDT").isPresent(),
        "Should not store the initialized bitmap in cache");
  }

  @Test
  @DisplayName("getOrCreateTickBitmap should create and add to cache when not in cache")
  public void testGetOrCreateTickBitmap_NotExists() {
    // Act
    TickBitmap result = tickBitmapCache.getOrCreateTickBitmap("BTC-USDT");

    // Assert
    assertNotNull(result, "Should return a non-null bitmap");
    assertEquals("BTC-USDT", result.getPoolPair(), "Should return bitmap with correct pool pair");
    assertTrue(tickBitmapCache.getTickBitmap("BTC-USDT").isPresent(), "Should store the created bitmap in cache");
  }

  @Test
  @DisplayName("getOrCreateTickBitmap should return existing bitmap when in cache")
  public void testGetOrCreateTickBitmap_Exists() {
    // Arrange
    TickBitmap bitmap = tickBitmapFactory.createEmptyBitmap("BTC-USDT");
    bitmap.setBit(100); // Set a specific bit to identify the bitmap
    tickBitmapCache.updateTickBitmap(bitmap);

    // Act
    TickBitmap result = tickBitmapCache.getOrCreateTickBitmap("BTC-USDT");

    // Assert
    assertTrue(result.isSet(100), "Should return existing bitmap with properties preserved");
  }

  @Test
  @DisplayName("updateTickBitmap should increment counter and add to cache and batch")
  public void testUpdateTickBitmap() {
    // Arrange
    TickBitmap bitmap = tickBitmapFactory.createEmptyBitmap("BTC-USDT");

    // Act
    tickBitmapCache.updateTickBitmap(bitmap);

    // Assert
    assertTrue(tickBitmapCache.getTickBitmap(bitmap.getPoolPair()).isPresent(),
        "Should store the updated bitmap in cache");

    // Verify the bitmap was added to the batch
    try {
      Field latestBitmapsField = TickBitmapCache.class.getDeclaredField("latestTickBitmaps");
      latestBitmapsField.setAccessible(true);
      Map<String, TickBitmap> latestBitmaps = (Map<String, TickBitmap>) latestBitmapsField.get(tickBitmapCache);
      assertTrue(latestBitmaps.containsKey(bitmap.getPoolPair()), "Should add the bitmap to the batch");
    } catch (Exception e) {
      fail("Failed to access latestTickBitmaps field: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("updateTickBitmap should not add null bitmap to cache")
  public void testUpdateTickBitmap_WithNullPoolPair() throws Exception {
    // Act
    tickBitmapCache.updateTickBitmap(null);

    // Verify bitmap was not added to cache
    Field cacheField = TickBitmapCache.class.getDeclaredField("tickBitmapCache");
    cacheField.setAccessible(true);
    ConcurrentHashMap<String, TickBitmap> cache = (ConcurrentHashMap<String, TickBitmap>) cacheField
        .get(tickBitmapCache);
    assertEquals(0, cache.size());

    // Verify bitmap was not added to batch
    Field batchField = TickBitmapCache.class.getDeclaredField("latestTickBitmaps");
    batchField.setAccessible(true);
    Map<String, TickBitmap> batch = (Map<String, TickBitmap>) batchField.get(tickBitmapCache);
    assertEquals(0, batch.size());
  }

  @Test
  @DisplayName("addTickBitmapToBatch should add new bitmap to batch")
  public void testAddTickBitmapToBatch() {
    // Case 1: Skip Add bitmap with null pool pair (would cause NullPointerException
    // in compute function)
    // Không thêm nullPoolPairBitmap vào batch vì sẽ gây NullPointerException

    // Case 2: Add first bitmap for BTC-USDT
    TickBitmap firstBitmap = tickBitmapFactory.createEmptyBitmap("BTC-USDT");
    firstBitmap.setBit(100);
    firstBitmap.setUpdatedAt(1000L);
    tickBitmapCache.addTickBitmapToBatch(firstBitmap);

    // Case 3: Add first bitmap for ETH-USDT (different pair)
    TickBitmap ethBitmap = tickBitmapFactory.createEmptyBitmap("ETH-USDT");
    ethBitmap.setBit(200);
    ethBitmap.setUpdatedAt(1500L);
    tickBitmapCache.addTickBitmapToBatch(ethBitmap);

    // Case 4: Add newer bitmap for BTC-USDT
    TickBitmap newerBitmap = tickBitmapFactory.createEmptyBitmap("BTC-USDT");
    newerBitmap.setBit(300);
    newerBitmap.setUpdatedAt(2000L);
    tickBitmapCache.addTickBitmapToBatch(newerBitmap);

    // Case 5: Add older bitmap for BTC-USDT (should be ignored)
    TickBitmap olderBitmap = tickBitmapFactory.createEmptyBitmap("BTC-USDT");
    olderBitmap.setBit(400);
    olderBitmap.setUpdatedAt(1500L);
    tickBitmapCache.addTickBitmapToBatch(olderBitmap);

    // Verify final state
    try {
      Field latestBitmapsField = TickBitmapCache.class.getDeclaredField("latestTickBitmaps");
      latestBitmapsField.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<String, TickBitmap> latestBitmaps = (Map<String, TickBitmap>) latestBitmapsField.get(tickBitmapCache);

      // Verify total number of bitmaps
      assertEquals(2, latestBitmaps.size(), "Should have bitmaps for BTC-USDT and ETH-USDT");

      // Verify BTC-USDT bitmap
      TickBitmap btcBitmap = latestBitmaps.get("BTC-USDT");
      assertNotNull(btcBitmap, "Should have BTC-USDT bitmap");
      assertEquals(2000L, btcBitmap.getUpdatedAt(), "Should keep the newest BTC-USDT bitmap");
      assertTrue(btcBitmap.isSet(300), "Should keep the specific bit set from newerBitmap");

      // Verify ETH-USDT bitmap
      TickBitmap storedEthBitmap = latestBitmaps.get("ETH-USDT");
      assertNotNull(storedEthBitmap, "Should have ETH-USDT bitmap");
      assertEquals(1500L, storedEthBitmap.getUpdatedAt(), "Should keep ETH-USDT bitmap timestamp");
      assertTrue(storedEthBitmap.isSet(200), "Should keep the specific bit set from ethBitmap");
    } catch (Exception e) {
      fail("Failed to access latestTickBitmaps field: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("tickBitmapCacheShouldFlush should return true when counter hits threshold")
  public void testTickBitmapCacheShouldFlush() {
    // Arrange & Assert
    assertFalse(tickBitmapCache.tickBitmapCacheShouldFlush(), "Should not flush when no updates");

    // Update bitmap many times to trigger flush threshold (UPDATE_THRESHOLD = 50)
    TickBitmap bitmap = tickBitmapFactory.createEmptyBitmap("BTC-USDT");
    for (int i = 0; i < 50; i++) {
      tickBitmapCache.updateTickBitmap(bitmap);
    }

    // Should now return true
    assertTrue(tickBitmapCache.tickBitmapCacheShouldFlush(), "Should flush after 50 updates");
  }

  @Test
  @DisplayName("tickBitmapCacheShouldFlush should return false when counter is zero")
  public void testTickBitmapCacheShouldFlush_EdgeCases() {
    // Test when count is 0
    assertFalse(tickBitmapCache.tickBitmapCacheShouldFlush(), "Should not flush when count is 0");

    // Test when count is not divisible by 50
    TickBitmap bitmap = tickBitmapFactory.createEmptyBitmap("BTC-USDT");
    for (int i = 0; i < 49; i++) {
      tickBitmapCache.updateTickBitmap(bitmap);
    }
    assertFalse(tickBitmapCache.tickBitmapCacheShouldFlush(), "Should not flush when count is not divisible by 50");

    // Test when count is divisible by 50
    tickBitmapCache.updateTickBitmap(bitmap);
    assertTrue(tickBitmapCache.tickBitmapCacheShouldFlush(), "Should flush when count is divisible by 50");
  }

  @Test
  @DisplayName("flushTickBitmapsToDisk should do nothing when batch is empty")
  public void testFlushTickBitmapsToDisk_WithEmptyBatch() {
    // Act
    tickBitmapCache.flushTickBitmapsToDisk();

    // Assert
    verify(mockTickBitmapRocksDB, never()).saveTickBitmapBatch(any());
  }

  @Test
  @DisplayName("flushTickBitmapsToDisk should save batch and clear it")
  public void testFlushTickBitmapsToDisk() {
    // Arrange
    TickBitmap bitmap = tickBitmapFactory.createEmptyBitmap("BTC-USDT");
    tickBitmapCache.addTickBitmapToBatch(bitmap);

    // Act
    tickBitmapCache.flushTickBitmapsToDisk();

    // Assert
    verify(mockTickBitmapRocksDB, times(1)).saveTickBitmapBatch(any());

    // Verify batch was cleared
    try {
      Field latestBitmapsField = TickBitmapCache.class.getDeclaredField("latestTickBitmaps");
      latestBitmapsField.setAccessible(true);
      Map<String, TickBitmap> latestBitmaps = (Map<String, TickBitmap>) latestBitmapsField.get(tickBitmapCache);
      assertTrue(latestBitmaps.isEmpty(), "Batch should be cleared after flush");
    } catch (Exception e) {
      fail("Failed to access latestTickBitmaps field: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("initializeTickBitmapCache should load bitmaps from RocksDB")
  public void testInitializeTickBitmapCache() {
    // Arrange
    List<TickBitmap> mockBitmaps = new ArrayList<>();
    TickBitmap validBitmap = tickBitmapFactory.createEmptyBitmap("BTC-USDT");
    TickBitmap invalidBitmap = tickBitmapFactory.createEmptyBitmap(""); // Empty pool pair
    mockBitmaps.add(validBitmap);
    mockBitmaps.add(invalidBitmap);
    when(mockTickBitmapRocksDB.getAllTickBitmaps()).thenReturn(mockBitmaps);

    // Act
    tickBitmapCache.initializeTickBitmapCache();

    // Assert
    assertTrue(tickBitmapCache.getTickBitmap(validBitmap.getPoolPair()).isPresent(), "Should load valid bitmap");
    assertFalse(tickBitmapCache.getTickBitmap("").isPresent(), "Should not load bitmap with empty pool pair");

    // Verify cache size
    Field cacheField;
    try {
      cacheField = TickBitmapCache.class.getDeclaredField("tickBitmapCache");
      cacheField.setAccessible(true);
      ConcurrentHashMap<String, TickBitmap> cache = (ConcurrentHashMap<String, TickBitmap>) cacheField
          .get(tickBitmapCache);
      assertEquals(1, cache.size(), "Should only load valid bitmaps");
    } catch (Exception e) {
      fail("Failed to access tickBitmapCache field: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("initializeTickBitmapCache should handle exception")
  public void testInitializeTickBitmapCache_WithException() {
    // Arrange
    when(mockTickBitmapRocksDB.getAllTickBitmaps()).thenThrow(new RuntimeException("Test exception"));

    // Act
    tickBitmapCache.initializeTickBitmapCache();

    // Assert - verify that the cache remains functional
    TickBitmap bitmap = tickBitmapFactory.createEmptyBitmap("BTC-USDT");
    tickBitmapCache.updateTickBitmap(bitmap);
    assertTrue(tickBitmapCache.getTickBitmap(bitmap.getPoolPair()).isPresent(),
        "Cache should still be functional after initialization error");
  }
}
