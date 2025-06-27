package com.exchangeengine.storage.rocksdb;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
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
import com.exchangeengine.model.Tick;

@ExtendWith({ MockitoExtension.class, SingletonResetExtension.class })
public class TickRocksDBTest {

  @Mock
  private RocksDBService rocksDBService;

  @Mock
  private ColumnFamilyHandle tickCF;

  private TickRocksDB tickRocksDB;

  @BeforeEach
  void setUp() {
    // Reset TickRocksDB singleton
    TickRocksDB.resetInstance();

    // Setup RocksDBService mock with lenient mode
    lenient().when(rocksDBService.getTickCF()).thenReturn(tickCF);
    RocksDBService.setTestInstance(rocksDBService);

    // Create a new TickRocksDB instance
    tickRocksDB = TickRocksDB.getInstance();
  }

  @Test
  @DisplayName("Lấy instance nên trả về cùng một instance")
  void getInstance_ShouldReturnSameInstance() {
    // Given & When
    TickRocksDB instance1 = TickRocksDB.getInstance();
    TickRocksDB instance2 = TickRocksDB.getInstance();

    // Then
    assertSame(instance1, instance2);
  }

  @Test
  @DisplayName("saveTick nên lưu tick vào RocksDB")
  void saveTick_ShouldSaveTickToRocksDB() {
    // Given
    Tick tick = new Tick("BTC-USDT", 100);
    doNothing().when(rocksDBService).saveObject(any(Tick.class), any(ColumnFamilyHandle.class), any(KeyExtractor.class),
        anyString());

    // When
    tickRocksDB.saveTick(tick);

    // Then
    verify(rocksDBService).saveObject(eq(tick), eq(tickCF), any(KeyExtractor.class), eq("tick"));
  }

  @Test
  @DisplayName("getTick nên lấy tick từ RocksDB")
  void getTick_ShouldRetrieveTickFromRocksDB() {
    // Given
    String tickKey = "BTC-USDT-100";
    Tick expectedTick = new Tick("BTC-USDT", 100);
    when(rocksDBService.getObject(eq(tickKey), eq(tickCF), eq(Tick.class), eq("tick")))
        .thenReturn(Optional.of(expectedTick));

    // When
    Optional<Tick> result = tickRocksDB.getTick(tickKey);

    // Then
    assertTrue(result.isPresent());
    assertEquals(expectedTick, result.get());
    verify(rocksDBService).getObject(eq(tickKey), eq(tickCF), eq(Tick.class), eq("tick"));
  }

  @Test
  @DisplayName("getAllTicks nên lấy tất cả ticks từ RocksDB")
  void getAllTicks_ShouldRetrieveAllTicksFromRocksDB() {
    // Given
    Tick tick1 = new Tick("BTC-USDT", 100);
    Tick tick2 = new Tick("ETH-USDT", 200);
    List<Tick> expectedTicks = Arrays.asList(tick1, tick2);

    when(rocksDBService.getAllObjects(eq(tickCF), eq(Tick.class), eq("ticks")))
        .thenReturn(expectedTicks);

    // When
    List<Tick> result = tickRocksDB.getAllTicks();

    // Then
    assertEquals(expectedTicks.size(), result.size());
    assertEquals(expectedTicks, result);
    verify(rocksDBService).getAllObjects(eq(tickCF), eq(Tick.class), eq("ticks"));
  }

  @Test
  @DisplayName("saveTickBatch nên lưu nhiều tick vào RocksDB")
  void saveTickBatch_ShouldSaveMultipleTicksToRocksDB() {
    // Given
    Tick tick1 = new Tick("BTC-USDT", 100);
    Tick tick2 = new Tick("ETH-USDT", 200);

    Map<String, Tick> ticks = new HashMap<>();
    ticks.put(tick1.getTickKey(), tick1);
    ticks.put(tick2.getTickKey(), tick2);

    doNothing().when(rocksDBService).saveBatch(anyMap(), any(ColumnFamilyHandle.class), any(KeyExtractor.class),
        anyString());

    // When
    tickRocksDB.saveTickBatch(ticks);

    // Then
    verify(rocksDBService).saveBatch(eq(ticks), eq(tickCF), any(KeyExtractor.class), eq("ticks"));
  }

  @Test
  @DisplayName("resetInstance nên xóa instance hiện tại")
  void resetInstance_ShouldClearCurrentInstance() {
    // Given
    TickRocksDB instance1 = TickRocksDB.getInstance();

    // When
    TickRocksDB.resetInstance();
    TickRocksDB instance2 = TickRocksDB.getInstance();

    // Then
    assertNotSame(instance1, instance2);
  }

  @Test
  @DisplayName("setTestInstance nên thiết lập instance cung cấp làm singleton")
  void setTestInstance_ShouldSetProvidedInstanceAsSingleton() {
    // Given
    TickRocksDB testInstance = mock(TickRocksDB.class);

    // When
    TickRocksDB.setTestInstance(testInstance);
    TickRocksDB result = TickRocksDB.getInstance();

    // Then
    assertSame(testInstance, result);
  }
}
