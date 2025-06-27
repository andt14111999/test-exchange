package com.exchangeengine.storage.rocksdb;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rocksdb.ColumnFamilyHandle;

import com.exchangeengine.extension.SingletonResetExtension;
import com.exchangeengine.factory.TickBitmapFactory;
import com.exchangeengine.model.TickBitmap;
import org.mockito.quality.Strictness;

@ExtendWith({ MockitoExtension.class, SingletonResetExtension.class })
public class TickBitmapRocksDBTest {

  @Mock
  private RocksDBService rocksDBService;

  @Mock
  private ColumnFamilyHandle tickBitmapCF;

  private TickBitmapRocksDB tickBitmapRocksDB;
  private TickBitmapFactory tickBitmapFactory;

  @BeforeEach
  void setUp() {
    // Reset TickBitmapRocksDB singleton
    TickBitmapRocksDB.resetInstance();

    // Setup RocksDBService mock with lenient mode for the methods that aren't used
    // in all tests
    lenient().when(rocksDBService.getTickBitmapCF()).thenReturn(tickBitmapCF);
    RocksDBService.setTestInstance(rocksDBService);

    // Create a new TickBitmapRocksDB instance
    tickBitmapRocksDB = TickBitmapRocksDB.getInstance();

    // Initialize the factory for test data
    tickBitmapFactory = new TickBitmapFactory();
  }

  @Test
  @DisplayName("getInstance nên trả về cùng một instance")
  void getInstance_ShouldReturnSameInstance() {
    // Given & When
    TickBitmapRocksDB instance1 = TickBitmapRocksDB.getInstance();
    TickBitmapRocksDB instance2 = TickBitmapRocksDB.getInstance();

    // Then
    assertSame(instance1, instance2);
  }

  @Test
  @DisplayName("saveTickBitmap nên lưu tickBitmap vào RocksDB")
  void saveTickBitmap_ShouldSaveTickBitmapToRocksDB() {
    // Given
    TickBitmap tickBitmap = tickBitmapFactory.createBitmapWithBits("BTC-USDT", new int[] { 100, 200, 300 });
    doNothing().when(rocksDBService).saveObject(any(TickBitmap.class), any(ColumnFamilyHandle.class),
        any(KeyExtractor.class), anyString());

    // When
    tickBitmapRocksDB.saveTickBitmap(tickBitmap);

    // Then
    verify(rocksDBService).saveObject(eq(tickBitmap), eq(tickBitmapCF), any(KeyExtractor.class), eq("tick_bitmap"));
  }

  @Test
  @DisplayName("getTickBitmap nên lấy tickBitmap từ RocksDB")
  void getTickBitmap_ShouldRetrieveTickBitmapFromRocksDB() {
    // Given
    String poolPair = "BTC-USDT";
    TickBitmap expectedTickBitmap = tickBitmapFactory.createBitmapWithBits(poolPair, new int[] { 100, 200, 300 });

    when(rocksDBService.getObject(eq(poolPair), eq(tickBitmapCF), eq(TickBitmap.class), eq("tick_bitmap")))
        .thenReturn(Optional.of(expectedTickBitmap));

    // When
    Optional<TickBitmap> result = tickBitmapRocksDB.getTickBitmap(poolPair);

    // Then
    assertTrue(result.isPresent());
    assertEquals(expectedTickBitmap, result.get());
    verify(rocksDBService).getObject(eq(poolPair), eq(tickBitmapCF), eq(TickBitmap.class), eq("tick_bitmap"));
  }

  @Test
  @DisplayName("getAllTickBitmaps nên lấy tất cả tickBitmaps từ RocksDB")
  void getAllTickBitmaps_ShouldRetrieveAllTickBitmapsFromRocksDB() {
    // Given
    TickBitmap tickBitmap1 = tickBitmapFactory.createBitmapWithBits("BTC-USDT", new int[] { 100, 200 });
    TickBitmap tickBitmap2 = tickBitmapFactory.createBitmapWithBits("ETH-USDT", new int[] { 150, 250 });
    List<TickBitmap> expectedTickBitmaps = Arrays.asList(tickBitmap1, tickBitmap2);

    when(rocksDBService.getAllObjects(eq(tickBitmapCF), eq(TickBitmap.class), eq("tick_bitmaps")))
        .thenReturn(expectedTickBitmaps);

    // When
    List<TickBitmap> result = tickBitmapRocksDB.getAllTickBitmaps();

    // Then
    assertEquals(expectedTickBitmaps.size(), result.size());
    assertEquals(expectedTickBitmaps, result);
    verify(rocksDBService).getAllObjects(eq(tickBitmapCF), eq(TickBitmap.class), eq("tick_bitmaps"));
  }

  @Test
  @DisplayName("saveTickBitmapBatch nên lưu nhiều tickBitmap vào RocksDB")
  void saveTickBitmapBatch_ShouldSaveMultipleTickBitmapsToRocksDB() {
    // Given
    TickBitmap tickBitmap1 = tickBitmapFactory.createBitmapWithBits("BTC-USDT", new int[] { 100, 200 });
    TickBitmap tickBitmap2 = tickBitmapFactory.createBitmapWithBits("ETH-USDT", new int[] { 150, 250 });

    Map<String, TickBitmap> tickBitmaps = new HashMap<>();
    tickBitmaps.put(tickBitmap1.getPoolPair(), tickBitmap1);
    tickBitmaps.put(tickBitmap2.getPoolPair(), tickBitmap2);

    doNothing().when(rocksDBService).saveBatch(anyMap(), any(ColumnFamilyHandle.class), any(KeyExtractor.class),
        anyString());

    // When
    tickBitmapRocksDB.saveTickBitmapBatch(tickBitmaps);

    // Then
    verify(rocksDBService).saveBatch(eq(tickBitmaps), eq(tickBitmapCF), any(KeyExtractor.class), eq("tick_bitmaps"));
  }

  @Test
  @DisplayName("resetInstance nên xóa instance hiện tại")
  void resetInstance_ShouldClearCurrentInstance() {
    // Given
    TickBitmapRocksDB instance1 = TickBitmapRocksDB.getInstance();

    // When
    TickBitmapRocksDB.resetInstance();
    TickBitmapRocksDB instance2 = TickBitmapRocksDB.getInstance();

    // Then
    assertNotSame(instance1, instance2);
  }

  @Test
  @DisplayName("setTestInstance nên thiết lập instance cung cấp làm singleton")
  void setTestInstance_ShouldSetProvidedInstanceAsSingleton() {
    // Given
    TickBitmapRocksDB testInstance = mock(TickBitmapRocksDB.class);

    // When
    TickBitmapRocksDB.setTestInstance(testInstance);
    TickBitmapRocksDB result = TickBitmapRocksDB.getInstance();

    // Then
    assertSame(testInstance, result);
  }
}
