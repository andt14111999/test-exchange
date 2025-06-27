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
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.exchangeengine.model.AmmPool;
import com.exchangeengine.factory.AmmPoolFactory;
import com.exchangeengine.storage.rocksdb.AmmPoolRocksDB;

/**
 * Test for AmmPoolCache
 */
public class AmmPoolCacheTest {

  private AmmPoolCache ammPoolCache;
  private AutoCloseable closeable;

  @Mock
  private AmmPoolRocksDB mockAmmPoolRocksDB;

  @BeforeEach
  public void setUp() throws Exception {
    closeable = MockitoAnnotations.openMocks(this);

    // Reset Singleton instance
    Field instanceField = AmmPoolCache.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);

    // Mock AmmPoolRocksDB instance
    Field rockDBField = AmmPoolRocksDB.class.getDeclaredField("instance");
    rockDBField.setAccessible(true);
    rockDBField.set(null, mockAmmPoolRocksDB);

    // Mock behavior
    List<AmmPool> emptyList = new ArrayList<>();
    when(mockAmmPoolRocksDB.getAllAmmPools()).thenReturn(emptyList);

    // Get the instance
    ammPoolCache = AmmPoolCache.getInstance();

    // Clear the cache
    Field cacheField = AmmPoolCache.class.getDeclaredField("ammPoolCache");
    cacheField.setAccessible(true);
    ConcurrentHashMap<String, AmmPool> cache = (ConcurrentHashMap<String, AmmPool>) cacheField.get(ammPoolCache);
    cache.clear();

    // Clear latest pools
    Field latestPoolsField = AmmPoolCache.class.getDeclaredField("latestAmmPools");
    latestPoolsField.setAccessible(true);
    ConcurrentHashMap<String, AmmPool> latestPools = (ConcurrentHashMap<String, AmmPool>) latestPoolsField
        .get(ammPoolCache);
    latestPools.clear();
  }

  @AfterEach
  public void tearDown() throws Exception {
    closeable.close();

    // Reset Singleton instance
    Field instanceField = AmmPoolCache.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);

    // Reset RocksDB instance
    Field rockDBField = AmmPoolRocksDB.class.getDeclaredField("instance");
    rockDBField.setAccessible(true);
    rockDBField.set(null, null);
  }

  @Test
  public void testGetInstance_ReturnsSameInstance() {
    // Act
    AmmPoolCache instance1 = AmmPoolCache.getInstance();
    AmmPoolCache instance2 = AmmPoolCache.getInstance();

    // Assert
    assertSame(instance1, instance2, "getInstance() should always return the same instance");
  }

  @Test
  public void testGetAmmPool_NotExists() {
    // Act
    Optional<AmmPool> result = ammPoolCache.getAmmPool("BTC/USDT");

    // Assert
    assertFalse(result.isPresent(), "Should return empty optional when pool doesn't exist");
  }

  @Test
  public void testGetAmmPool_Exists() {
    // Arrange
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    ammPoolCache.updateAmmPool(pool);

    // Act
    Optional<AmmPool> result = ammPoolCache.getAmmPool(pool.getPair());

    // Assert
    assertTrue(result.isPresent(), "Should return pool when it exists");
    assertEquals(pool.getPair(), result.get().getPair(), "Should return correct pool");
  }

  @Test
  public void testGetOrInitAmmPool() {
    // Act
    AmmPool result = ammPoolCache.getOrInitAmmPool("BTC/USDT");

    // Assert
    assertNotNull(result, "Should return a non-null pool");
    assertEquals("BTC/USDT", result.getPair(), "Should return pool with correct pair");
    assertFalse(ammPoolCache.getAmmPool("BTC/USDT").isPresent(), "Should not store the initialized pool in cache");
  }

  @Test
  public void testGetOrCreateAmmPool_NotExists() {
    // Act
    AmmPool result = ammPoolCache.getOrCreateAmmPool("BTC/USDT");

    // Assert
    assertNotNull(result, "Should return a non-null pool");
    assertEquals("BTC/USDT", result.getPair(), "Should return pool with correct pair");
    assertTrue(ammPoolCache.getAmmPool("BTC/USDT").isPresent(), "Should store the created pool in cache");
  }

  @Test
  public void testGetOrCreateAmmPool_Exists() {
    // Arrange
    AmmPool pool = AmmPoolFactory.createCustomAmmPool("BTC/USDT", "BTC", "USDT", 0.003);
    pool.setTickSpacing(10);
    ammPoolCache.updateAmmPool(pool);

    // Act
    AmmPool result = ammPoolCache.getOrCreateAmmPool("BTC/USDT");

    // Assert
    assertEquals(10, result.getTickSpacing(), "Should return existing pool with properties preserved");
  }

  @Test
  public void testUpdateAmmPool() {
    // Arrange
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();

    // Act
    ammPoolCache.updateAmmPool(pool);

    // Assert
    assertTrue(ammPoolCache.getAmmPool(pool.getPair()).isPresent(), "Should store the updated pool in cache");
  }

  @Test
  public void testAddAmmPoolToBatch() {
    // Case 1: Add pool with null pair (should be ignored)
    AmmPool nullPairPool = AmmPoolFactory.createCustomAmmPool("BTC/USDT", "BTC", "USDT", 0.003);
    nullPairPool.setPair(null);
    ammPoolCache.addAmmPoolToBatch(nullPairPool);

    // Case 2: Add first pool for BTC/USDT
    AmmPool firstPool = AmmPoolFactory.createCustomAmmPool("BTC/USDT", "BTC", "USDT", 0.003);
    firstPool.setUpdatedAt(1000L);
    ammPoolCache.addAmmPoolToBatch(firstPool);

    // Case 3: Add first pool for ETH/USDT (different pair)
    AmmPool ethPool = AmmPoolFactory.createCustomAmmPool("ETH/USDT", "ETH", "USDT", 0.003);
    ethPool.setUpdatedAt(1500L);
    ammPoolCache.addAmmPoolToBatch(ethPool);

    // Case 4: Add newer pool for BTC/USDT
    AmmPool newerPool = AmmPoolFactory.createCustomAmmPool("BTC/USDT", "BTC", "USDT", 0.003);
    newerPool.setUpdatedAt(2000L);
    ammPoolCache.addAmmPoolToBatch(newerPool);

    // Case 5: Add older pool for BTC/USDT (should be ignored)
    AmmPool olderPool = AmmPoolFactory.createCustomAmmPool("BTC/USDT", "BTC", "USDT", 0.003);
    olderPool.setUpdatedAt(1500L);
    ammPoolCache.addAmmPoolToBatch(olderPool);

    // Case 6: Add pool with same timestamp for BTC/USDT (should keep existing)
    AmmPool sameTimePool = AmmPoolFactory.createCustomAmmPool("BTC/USDT", "BTC", "USDT", 0.003);
    sameTimePool.setUpdatedAt(2000L);
    ammPoolCache.addAmmPoolToBatch(sameTimePool);

    // Verify final state
    try {
      Field latestPoolsField = AmmPoolCache.class.getDeclaredField("latestAmmPools");
      latestPoolsField.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<String, AmmPool> latestPools = (Map<String, AmmPool>) latestPoolsField.get(ammPoolCache);

      // Verify total number of pools
      assertEquals(2, latestPools.size(), "Should have pools for BTC/USDT and ETH/USDT");

      // Verify BTC/USDT pool
      AmmPool btcPool = latestPools.get("BTC/USDT");
      assertNotNull(btcPool, "Should have BTC/USDT pool");
      assertEquals(2000L, btcPool.getUpdatedAt(), "Should keep the newest BTC/USDT pool");
      assertSame(newerPool, btcPool, "Should be the same instance as newerPool");

      // Verify ETH/USDT pool
      AmmPool storedEthPool = latestPools.get("ETH/USDT");
      assertNotNull(storedEthPool, "Should have ETH/USDT pool");
      assertEquals(1500L, storedEthPool.getUpdatedAt(), "Should keep ETH/USDT pool timestamp");
      assertSame(ethPool, storedEthPool, "Should be the same instance as ethPool");
    } catch (Exception e) {
      fail("Failed to access latestAmmPools field: " + e.getMessage());
    }
  }

  @Test
  public void testInitializeAmmPoolCache_WithException() {
    // Arrange
    when(mockAmmPoolRocksDB.getAllAmmPools()).thenThrow(new RuntimeException("DB Error"));

    // Act
    ammPoolCache.initializeAmmPoolCache();

    // Assert - verify that the cache remains functional
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    ammPoolCache.updateAmmPool(pool);
    assertTrue(ammPoolCache.getAmmPool(pool.getPair()).isPresent(),
        "Cache should still be functional after initialization error");
  }

  @Test
  public void testInitializeAmmPoolCache() {
    // Arrange
    List<AmmPool> mockPools = new ArrayList<>();
    AmmPool validPool = AmmPoolFactory.createDefaultAmmPool();
    AmmPool invalidPool = AmmPoolFactory.createDefaultAmmPool();
    invalidPool.setPair("");
    mockPools.add(validPool);
    mockPools.add(invalidPool);
    when(mockAmmPoolRocksDB.getAllAmmPools()).thenReturn(mockPools);

    // Act
    ammPoolCache.initializeAmmPoolCache();

    // Assert
    assertTrue(ammPoolCache.getAmmPool(validPool.getPair()).isPresent(), "Should load valid pool");
    assertFalse(ammPoolCache.getAmmPool("").isPresent(), "Should not load pool with empty pair");

    // Verify cache size
    Field cacheField;
    try {
      cacheField = AmmPoolCache.class.getDeclaredField("ammPoolCache");
      cacheField.setAccessible(true);
      ConcurrentHashMap<String, AmmPool> cache = (ConcurrentHashMap<String, AmmPool>) cacheField.get(ammPoolCache);
      assertEquals(1, cache.size(), "Should only load valid pools");
    } catch (Exception e) {
      fail("Failed to access ammPoolCache field: " + e.getMessage());
    }
  }

  @Test
  public void testFlushAmmPoolToDisk_WithEmptyBatch() {
    // Act
    ammPoolCache.flushAmmPoolToDisk();

    // Assert
    verify(mockAmmPoolRocksDB, never()).saveAmmPoolBatch(any());
  }

  @Test
  public void testFlushAmmPoolToDisk() {
    // Arrange
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    ammPoolCache.addAmmPoolToBatch(pool);

    // Act
    ammPoolCache.flushAmmPoolToDisk();

    // Assert
    verify(mockAmmPoolRocksDB, times(1)).saveAmmPoolBatch(any());
  }

  @Test
  public void testAmmPoolCacheShouldFlush() {
    // Act & Assert
    assertFalse(ammPoolCache.ammPoolCacheShouldFlush(), "Should not flush when no updates");

    // Update pool many times to trigger flush threshold
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    for (int i = 0; i < 100; i++) {
      ammPoolCache.updateAmmPool(pool);
    }

    // Should now return true
    assertTrue(ammPoolCache.ammPoolCacheShouldFlush(), "Should flush after 100 updates");
  }

  @Test
  public void testInitializeAmmPoolCache_WithInvalidPairs() {
    // Arrange
    List<AmmPool> mockPools = new ArrayList<>();
    AmmPool validPool = AmmPoolFactory.createDefaultAmmPool();
    AmmPool nullPairPool = AmmPoolFactory.createDefaultAmmPool();
    nullPairPool.setPair(null);
    AmmPool emptyPairPool = AmmPoolFactory.createDefaultAmmPool();
    emptyPairPool.setPair("");

    mockPools.add(validPool);
    mockPools.add(nullPairPool);
    mockPools.add(emptyPairPool);
    when(mockAmmPoolRocksDB.getAllAmmPools()).thenReturn(mockPools);

    // Act
    ammPoolCache.initializeAmmPoolCache();

    // Assert
    assertTrue(ammPoolCache.getAmmPool(validPool.getPair()).isPresent(), "Should load valid pool");
    assertFalse(ammPoolCache.getAmmPool("").isPresent(), "Should not load pool with empty pair");

    // Verify loadedCount through cache size
    Field cacheField;
    try {
      cacheField = AmmPoolCache.class.getDeclaredField("ammPoolCache");
      cacheField.setAccessible(true);
      ConcurrentHashMap<String, AmmPool> cache = (ConcurrentHashMap<String, AmmPool>) cacheField.get(ammPoolCache);
      assertEquals(1, cache.size(), "Should only load valid pools");
    } catch (Exception e) {
      fail("Failed to access ammPoolCache field: " + e.getMessage());
    }
  }

  @Test
  void testInitializeAmmPoolCache_LoadedCountIncrement() throws Exception {
    // Given
    AmmPool validPool1 = AmmPoolFactory.createCustomAmmPool("BTC/USDT", "BTC", "USDT", 0.003);
    AmmPool validPool2 = AmmPoolFactory.createCustomAmmPool("ETH/USDT", "ETH", "USDT", 0.003);
    AmmPool invalidPool = AmmPoolFactory.createDefaultAmmPool();
    invalidPool.setPair("");

    when(mockAmmPoolRocksDB.getAllAmmPools()).thenReturn(Arrays.asList(validPool1, validPool2, invalidPool));

    // When
    ammPoolCache.initializeAmmPoolCache();

    // Then
    Field loadedCountField = AmmPoolCache.class.getDeclaredField("ammPoolCache");
    loadedCountField.setAccessible(true);
    Map<String, AmmPool> cache = (Map<String, AmmPool>) loadedCountField.get(ammPoolCache);

    assertEquals(2, cache.size(), "LoadedCount should be 2 for valid pools only");
  }

  @Test
  public void testAmmPoolCacheShouldFlush_EdgeCases() {
    // Test when count is 0
    assertFalse(ammPoolCache.ammPoolCacheShouldFlush(), "Should not flush when count is 0");

    // Test when count is not divisible by 100
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    for (int i = 0; i < 99; i++) {
      ammPoolCache.updateAmmPool(pool);
    }
    assertFalse(ammPoolCache.ammPoolCacheShouldFlush(), "Should not flush when count is not divisible by 100");

    // Test when count is divisible by 100
    ammPoolCache.updateAmmPool(pool);
    assertTrue(ammPoolCache.ammPoolCacheShouldFlush(), "Should flush when count is divisible by 100");
  }
}
