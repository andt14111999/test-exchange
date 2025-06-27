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
import com.exchangeengine.factory.TradeFactory;
import com.exchangeengine.model.Trade;

@ExtendWith({ MockitoExtension.class, SingletonResetExtension.class })
public class TradeRocksDBTest {

  @Mock
  private RocksDBService rocksDBService;

  @Mock
  private ColumnFamilyHandle tradeCF;

  private TradeRocksDB tradeRocksDB;

  @BeforeEach
  void setUp() {
    // Reset TradeRocksDB singleton
    TradeRocksDB.resetInstance();

    // Setup RocksDBService mock with lenient mode
    lenient().when(rocksDBService.getTradeCF()).thenReturn(tradeCF);
    RocksDBService.setTestInstance(rocksDBService);

    // Create a new TradeRocksDB instance
    tradeRocksDB = TradeRocksDB.getInstance();
  }

  @Test
  @DisplayName("getInstance nên trả về cùng một instance")
  void getInstance_ShouldReturnSameInstance() {
    // Given & When
    TradeRocksDB instance1 = TradeRocksDB.getInstance();
    TradeRocksDB instance2 = TradeRocksDB.getInstance();

    // Then
    assertSame(instance1, instance2);
  }

  @Test
  @DisplayName("saveTrade nên lưu trade vào RocksDB")
  void saveTrade_ShouldSaveTradeToRocksDB() {
    // Given
    Trade trade = TradeFactory.create();
    doNothing().when(rocksDBService).saveObject(any(Trade.class), any(ColumnFamilyHandle.class), any(KeyExtractor.class),
        anyString());

    // When
    tradeRocksDB.saveTrade(trade);

    // Then
    verify(rocksDBService).saveObject(eq(trade), eq(tradeCF), any(KeyExtractor.class), eq("trade"));
  }

  @Test
  @DisplayName("saveTrade không thực hiện khi trade là null")
  void saveTrade_ShouldNotSaveWhenTradeIsNull() {
    // Given
    Trade trade = null;

    // When
    tradeRocksDB.saveTrade(trade);

    // Then
    verify(rocksDBService, never()).saveObject(any(Trade.class), any(ColumnFamilyHandle.class), any(KeyExtractor.class),
        anyString());
  }

  @Test
  @DisplayName("getTrade nên lấy trade từ RocksDB")
  void getTrade_ShouldRetrieveTradeFromRocksDB() {
    // Given
    String tradeId = "trade-123";
    Trade expectedTrade = TradeFactory.create();
    when(rocksDBService.getObject(eq(tradeId), eq(tradeCF), eq(Trade.class), eq("trade")))
        .thenReturn(Optional.of(expectedTrade));

    // When
    Optional<Trade> result = tradeRocksDB.getTrade(tradeId);

    // Then
    assertTrue(result.isPresent());
    assertEquals(expectedTrade, result.get());
    verify(rocksDBService).getObject(eq(tradeId), eq(tradeCF), eq(Trade.class), eq("trade"));
  }

  @Test
  @DisplayName("getTrade nên trả về Optional.empty khi identifier là null")
  void getTrade_ShouldReturnEmptyOptionalWhenIdentifierIsNull() {
    // Given
    String tradeId = null;

    // When
    Optional<Trade> result = tradeRocksDB.getTrade(tradeId);

    // Then
    assertFalse(result.isPresent());
    verify(rocksDBService, never()).getObject(any(), any(), any(), any());
  }

  @Test
  @DisplayName("getTrade nên trả về Optional.empty khi identifier là rỗng")
  void getTrade_ShouldReturnEmptyOptionalWhenIdentifierIsEmpty() {
    // Given
    String tradeId = "";

    // When
    Optional<Trade> result = tradeRocksDB.getTrade(tradeId);

    // Then
    assertFalse(result.isPresent());
    verify(rocksDBService, never()).getObject(any(), any(), any(), any());
  }

  @Test
  @DisplayName("getAllTrades nên lấy tất cả trades từ RocksDB")
  void getAllTrades_ShouldRetrieveAllTradesFromRocksDB() {
    // Given
    Trade trade1 = TradeFactory.create();
    Trade trade2 = TradeFactory.create();
    List<Trade> expectedTrades = Arrays.asList(trade1, trade2);

    when(rocksDBService.getAllObjects(eq(tradeCF), eq(Trade.class), eq("trades")))
        .thenReturn(expectedTrades);

    // When
    List<Trade> result = tradeRocksDB.getAllTrades();

    // Then
    assertEquals(expectedTrades.size(), result.size());
    assertEquals(expectedTrades, result);
    verify(rocksDBService).getAllObjects(eq(tradeCF), eq(Trade.class), eq("trades"));
  }

  @Test
  @DisplayName("saveTradeBatch nên lưu nhiều trade vào RocksDB")
  void saveTradeBatch_ShouldSaveMultipleTradesToRocksDB() {
    // Given
    Trade trade1 = TradeFactory.create();
    Trade trade2 = TradeFactory.create();

    Map<String, Trade> trades = new HashMap<>();
    trades.put(trade1.getIdentifier(), trade1);
    trades.put(trade2.getIdentifier(), trade2);

    doNothing().when(rocksDBService).saveBatch(anyMap(), any(ColumnFamilyHandle.class), any(KeyExtractor.class),
        anyString());

    // When
    tradeRocksDB.saveTradeBatch(trades);

    // Then
    verify(rocksDBService).saveBatch(eq(trades), eq(tradeCF), any(KeyExtractor.class), eq("trades"));
  }

  @Test
  @DisplayName("saveTradeBatch không thực hiện khi map là null")
  void saveTradeBatch_ShouldNotSaveWhenMapIsNull() {
    // Given
    Map<String, Trade> trades = null;

    // When
    tradeRocksDB.saveTradeBatch(trades);

    // Then
    verify(rocksDBService, never()).saveBatch(anyMap(), any(ColumnFamilyHandle.class), any(KeyExtractor.class),
        anyString());
  }

  @Test
  @DisplayName("saveTradeBatch không thực hiện khi map rỗng")
  void saveTradeBatch_ShouldNotSaveWhenMapIsEmpty() {
    // Given
    Map<String, Trade> trades = new HashMap<>();

    // When
    tradeRocksDB.saveTradeBatch(trades);

    // Then
    verify(rocksDBService, never()).saveBatch(anyMap(), any(ColumnFamilyHandle.class), any(KeyExtractor.class),
        anyString());
  }

  @Test
  @DisplayName("resetInstance nên xóa instance hiện tại")
  void resetInstance_ShouldClearCurrentInstance() {
    // Given
    TradeRocksDB instance1 = TradeRocksDB.getInstance();

    // When
    TradeRocksDB.resetInstance();
    TradeRocksDB instance2 = TradeRocksDB.getInstance();

    // Then
    assertNotSame(instance1, instance2);
  }

  @Test
  @DisplayName("setTestInstance nên thiết lập instance cung cấp làm singleton")
  void setTestInstance_ShouldSetProvidedInstanceAsSingleton() {
    // Given
    TradeRocksDB testInstance = mock(TradeRocksDB.class);

    // When
    TradeRocksDB.setTestInstance(testInstance);
    TradeRocksDB result = TradeRocksDB.getInstance();

    // Then
    assertSame(testInstance, result);
  }
}
