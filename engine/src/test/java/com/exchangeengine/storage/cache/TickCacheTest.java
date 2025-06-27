package com.exchangeengine.storage.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exchangeengine.extension.SingletonResetExtension;
import com.exchangeengine.model.Tick;
import com.exchangeengine.storage.rocksdb.TickRocksDB;

@ExtendWith({ MockitoExtension.class, SingletonResetExtension.class })
public class TickCacheTest {

  @Mock
  private TickRocksDB tickRocksDB;

  private TickCache tickCache;

  @BeforeEach
  void setUp() {
    // Reset TickCache singleton
    TickCache.resetInstance();

    // Setup TickRocksDB mock
    TickRocksDB.setTestInstance(tickRocksDB);

    // Create a new TickCache instance
    tickCache = TickCache.getInstance();
  }

  @Test
  @DisplayName("Lấy instance nên trả về cùng một instance")
  void getInstance_ShouldReturnSameInstance() {
    // Given & When
    TickCache instance1 = TickCache.getInstance();
    TickCache instance2 = TickCache.getInstance();

    // Then
    assertSame(instance1, instance2);
  }

  @Test
  @DisplayName("Khởi tạo TickCache nên tải tất cả tick từ RocksDB")
  void initializeTickCache_ShouldLoadAllTicksFromRocksDB() {
    // Given
    Tick tick1 = new Tick("BTC-USDT", 1);
    Tick tick2 = new Tick("ETH-USDT", 2);
    List<Tick> mockTicks = Arrays.asList(tick1, tick2);

    when(tickRocksDB.getAllTicks()).thenReturn(mockTicks);

    // When
    tickCache.initializeTickCache();

    // Then
    Optional<Tick> resultTick1 = tickCache.getTick(tick1.getTickKey());
    Optional<Tick> resultTick2 = tickCache.getTick(tick2.getTickKey());

    assertTrue(resultTick1.isPresent());
    assertTrue(resultTick2.isPresent());
    assertEquals(tick1, resultTick1.get());
    assertEquals(tick2, resultTick2.get());
  }

  @Test
  @DisplayName("Khởi tạo TickCache nên xử lý ngoại lệ từ RocksDB")
  void initializeTickCache_ShouldHandleException() {
    // Given
    when(tickRocksDB.getAllTicks()).thenThrow(new RuntimeException("DB Error"));

    // When
    tickCache.initializeTickCache();

    // Then - verify that the cache remains functional
    Tick tick = new Tick("BTC-USDT", 100);
    tickCache.updateTick(tick);
    assertTrue(tickCache.getTick(tick.getTickKey()).isPresent());
  }

  @Test
  @DisplayName("Khởi tạo TickCache nên bỏ qua các tick có tickKey rỗng")
  void initializeTickCache_ShouldSkipInvalidTicks() {
    // Given
    Tick validTick = new Tick("BTC-USDT", 1);

    // Create an invalid tick with null tickKey
    Tick invalidTick = mock(Tick.class);
    when(invalidTick.getTickKey()).thenReturn(null);

    // Create another invalid tick with empty tickKey
    Tick emptyKeyTick = mock(Tick.class);
    when(emptyKeyTick.getTickKey()).thenReturn("");

    List<Tick> mockTicks = Arrays.asList(validTick, invalidTick, emptyKeyTick);
    when(tickRocksDB.getAllTicks()).thenReturn(mockTicks);

    // When
    tickCache.initializeTickCache();

    // Then
    // Verify only valid ticks were loaded
    Optional<Tick> resultTick1 = tickCache.getTick(validTick.getTickKey());
    assertTrue(resultTick1.isPresent());

    // Check total loaded count is 1
    try {
      java.lang.reflect.Field tickCacheField = TickCache.class.getDeclaredField("tickCache");
      tickCacheField.setAccessible(true);
      ConcurrentHashMap<String, Tick> cache = (ConcurrentHashMap<String, Tick>) tickCacheField.get(tickCache);
      assertEquals(1, cache.size(), "Should only load valid ticks");
    } catch (Exception e) {
      fail("Could not access tickCache field: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Không tìm thấy tick nên trả về Optional rỗng")
  void getTick_WhenNotFound_ShouldReturnEmptyOptional() {
    // Given
    String nonExistingTickKey = "NON-EXISTENT-1";

    // When
    Optional<Tick> result = tickCache.getTick(nonExistingTickKey);

    // Then
    assertFalse(result.isPresent());
  }

  @Test
  @DisplayName("getOrInitTick nên tạo mới tick nếu không tìm thấy")
  void getOrInitTick_WhenNotFound_ShouldCreateNewTick() {
    // Given
    String poolPair = "BTC-USDT";
    int tickIndex = 100;
    String tickKey = poolPair + "-" + tickIndex;

    // When
    Tick result = tickCache.getOrInitTick(poolPair, tickIndex);

    // Then
    assertNotNull(result);
    assertEquals(poolPair, result.getPoolPair());
    assertEquals(tickIndex, result.getTickIndex());
    assertEquals(tickKey, result.getTickKey());
  }

  @Test
  @DisplayName("getOrCreateTick nên tạo và cập nhật tick mới nếu không tìm thấy")
  void getOrCreateTick_WhenNotFound_ShouldCreateAndUpdateNewTick() {
    // Given
    String poolPair = "BTC-USDT";
    int tickIndex = 100;
    String tickKey = poolPair + "-" + tickIndex;

    // When
    Tick result = tickCache.getOrCreateTick(poolPair, tickIndex);

    // Then
    assertNotNull(result);
    assertEquals(poolPair, result.getPoolPair());
    assertEquals(tickIndex, result.getTickIndex());

    // Kiểm tra tick đã được thêm vào cache
    Optional<Tick> cachedTick = tickCache.getTick(tickKey);
    assertTrue(cachedTick.isPresent());
    assertEquals(result, cachedTick.get());
  }

  @Test
  @DisplayName("updateTick nên cập nhật tick trong cache và thêm vào batch")
  void updateTick_ShouldUpdateTickInCacheAndAddToBatch() {
    // Given
    Tick tick = new Tick("ETH-USDT", 200);

    // When
    tickCache.updateTick(tick);

    // Then
    Optional<Tick> cachedTick = tickCache.getTick(tick.getTickKey());
    assertTrue(cachedTick.isPresent());
    assertEquals(tick, cachedTick.get());

    // Simulate flushing to disk to verify tick was added to batch
    doNothing().when(tickRocksDB).saveTickBatch(anyMap());
    tickCache.flushTicksToDisk();

    // Verify saveTickBatch was called (indicates tick was in batch)
    verify(tickRocksDB).saveTickBatch(anyMap());
  }

  @Test
  @DisplayName("addTickToBatch nên thêm tick mới và thay thế tick cũ nếu có timestamp mới hơn")
  void addTickToBatch_ShouldAddAndReplaceCorrectly() {
    // Given
    // Create a spy on the map to track real interactions
    Map<String, Tick> latestTicksSpy = spy(new HashMap<>());

    // Use reflection to set the latestTicks field in tickCache
    try {
      java.lang.reflect.Field latestTicksField = TickCache.class.getDeclaredField("latestTicks");
      latestTicksField.setAccessible(true);
      latestTicksField.set(tickCache, latestTicksSpy);
    } catch (Exception e) {
      fail("Could not set latestTicks field: " + e.getMessage());
    }

    // Create older tick
    Tick olderTick = new Tick("BTC-USDT", 100);
    olderTick.setUpdatedAt(1000L);

    // Create newer tick with same key
    Tick newerTick = new Tick("BTC-USDT", 100);
    newerTick.setUpdatedAt(2000L);

    // When - add older tick first
    tickCache.addTickToBatch(olderTick);

    // Verify compute was called
    verify(latestTicksSpy, times(1)).compute(eq(olderTick.getTickKey()), any());

    // Then - add newer tick and verify it replaces older one
    tickCache.addTickToBatch(newerTick);

    // Check directly in the map
    try {
      java.lang.reflect.Field latestTicksField = TickCache.class.getDeclaredField("latestTicks");
      latestTicksField.setAccessible(true);
      Map<String, Tick> latestTicks = (Map<String, Tick>) latestTicksField.get(tickCache);

      // Should have only one entry
      assertEquals(1, latestTicks.size());

      // The entry should be the newer tick
      Tick storedTick = latestTicks.get(newerTick.getTickKey());
      assertEquals(2000L, storedTick.getUpdatedAt());
      assertSame(newerTick, storedTick);
    } catch (Exception e) {
      fail("Could not access latestTicks field: " + e.getMessage());
    }

    // Verify older tick is not replaced by an even older one
    Tick evenOlderTick = new Tick("BTC-USDT", 100);
    evenOlderTick.setUpdatedAt(500L);
    tickCache.addTickToBatch(evenOlderTick);

    try {
      java.lang.reflect.Field latestTicksField = TickCache.class.getDeclaredField("latestTicks");
      latestTicksField.setAccessible(true);
      Map<String, Tick> latestTicks = (Map<String, Tick>) latestTicksField.get(tickCache);

      // Still should have only one entry
      assertEquals(1, latestTicks.size());

      // The entry should still be the newer tick
      Tick storedTick = latestTicks.get(newerTick.getTickKey());
      assertEquals(2000L, storedTick.getUpdatedAt());
      assertSame(newerTick, storedTick);
    } catch (Exception e) {
      fail("Could not access latestTicks field: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("tickCacheShouldFlush nên trả về true khi đạt ngưỡng")
  void tickCacheShouldFlush_WhenReachingThreshold_ShouldReturnTrue() {
    // Given - assuming BACKUP_BATCH_SIZE = 1000
    Tick tick = new Tick("BTC-USDT", 1);

    // Simulate 1000 updates
    for (int i = 0; i < 1000; i++) {
      tickCache.updateTick(tick);
    }

    // When & Then
    assertTrue(tickCache.tickCacheShouldFlush());
  }

  @Test
  @DisplayName("flushTicksToDisk nên lưu tất cả tick vào RocksDB và xóa các tick đã lưu")
  void flushTicksToDisk_ShouldSaveAllTicksToRocksDBAndClearBatch() {
    // Given
    Tick tick1 = new Tick("BTC-USDT", 1);
    Tick tick2 = new Tick("ETH-USDT", 2);

    // Create a spy on the map to track real interactions
    Map<String, Tick> latestTicksSpy = spy(new HashMap<>());

    // Use reflection to set the latestTicks field in tickCache
    try {
      java.lang.reflect.Field latestTicksField = TickCache.class.getDeclaredField("latestTicks");
      latestTicksField.setAccessible(true);
      latestTicksField.set(tickCache, latestTicksSpy);
    } catch (Exception e) {
      fail("Could not set latestTicks field: " + e.getMessage());
    }

    tickCache.updateTick(tick1);
    tickCache.updateTick(tick2);

    // When
    tickCache.flushTicksToDisk();

    // Then
    // Verify compute was called for each tick (not put directly)
    verify(latestTicksSpy, times(1)).compute(eq(tick1.getTickKey()), any());
    verify(latestTicksSpy, times(1)).compute(eq(tick2.getTickKey()), any());

    // Verify saveTickBatch was called
    verify(tickRocksDB).saveTickBatch(any());

    // Verify the map was cleared
    verify(latestTicksSpy).clear();
  }

  @Test
  @DisplayName("flushTicksToDisk nên bỏ qua khi không có tick để lưu")
  void flushTicksToDisk_WithEmptyBatch_ShouldDoNothing() {
    // When
    tickCache.flushTicksToDisk();

    // Then
    verify(tickRocksDB, never()).saveTickBatch(any());
  }

  @Test
  @DisplayName("resetInstance nên xóa instance hiện tại")
  void resetInstance_ShouldClearCurrentInstance() {
    // Given
    TickCache instance1 = TickCache.getInstance();

    // When
    TickCache.resetInstance();
    TickCache instance2 = TickCache.getInstance();

    // Then
    assertNotSame(instance1, instance2);
  }

  @Test
  @DisplayName("setTestInstance nên thiết lập instance cung cấp làm singleton")
  void setTestInstance_ShouldSetProvidedInstanceAsSingleton() {
    // Given
    TickCache testInstance = mock(TickCache.class);

    // When
    TickCache.setTestInstance(testInstance);
    TickCache result = TickCache.getInstance();

    // Then
    assertSame(testInstance, result);
  }

  @Test
  @DisplayName("tickCacheShouldFlush nên xử lý các trường hợp biên")
  void tickCacheShouldFlush_EdgeCases() {
    // Set updateCounter directly
    try {
      java.lang.reflect.Field updateCounterField = TickCache.class.getDeclaredField("updateCounter");
      updateCounterField.setAccessible(true);

      // Case 1: Count = 0
      updateCounterField.set(tickCache, new AtomicInteger(0));
      assertFalse(tickCache.tickCacheShouldFlush(), "Should return false when count is 0");

      // Case 2: Count > 0 but not divisible by BACKUP_BATCH_SIZE (1000)
      updateCounterField.set(tickCache, new AtomicInteger(999));
      assertFalse(tickCache.tickCacheShouldFlush(),
          "Should return false when count is not divisible by BACKUP_BATCH_SIZE");

      // Case 3: Count divisible by BACKUP_BATCH_SIZE (1000)
      updateCounterField.set(tickCache, new AtomicInteger(1000));
      assertTrue(tickCache.tickCacheShouldFlush(), "Should return true when count is divisible by BACKUP_BATCH_SIZE");

      // Case 4: Count divisible by BACKUP_BATCH_SIZE (2000)
      updateCounterField.set(tickCache, new AtomicInteger(2000));
      assertTrue(tickCache.tickCacheShouldFlush(), "Should return true when count is divisible by BACKUP_BATCH_SIZE");

    } catch (Exception e) {
      fail("Could not access updateCounter field: " + e.getMessage());
    }
  }
}
