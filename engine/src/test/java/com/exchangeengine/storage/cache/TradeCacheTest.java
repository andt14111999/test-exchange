package com.exchangeengine.storage.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exchangeengine.factory.TradeFactory;
import com.exchangeengine.model.Trade;
import com.exchangeengine.storage.rocksdb.TradeRocksDB;

@ExtendWith(MockitoExtension.class)
class TradeCacheTest {

    @Mock
    private TradeRocksDB mockTradeRocksDB;

    private TradeCache tradeCache;
    private MockedStatic<TradeRocksDB> mockedTradeRocksDBStatic;

    // Test data
    private Trade testTrade;

    @BeforeEach
    void setUp() throws Exception {
        // Tạo dữ liệu test sử dụng TradeFactory
        testTrade = TradeFactory.create();

        // Cấu hình mock
        mockedTradeRocksDBStatic = Mockito.mockStatic(TradeRocksDB.class);
        mockedTradeRocksDBStatic.when(TradeRocksDB::getInstance).thenReturn(mockTradeRocksDB);

        // Reset TradeCache instance
        resetSingleton(TradeCache.class, "instance");

        // Khởi tạo TradeCache
        tradeCache = TradeCache.getInstance();
        tradeCache.updateTrade(testTrade);
        
        // Reset lại mock sau khi updateTrade đã gọi saveTrade
        reset(mockTradeRocksDB);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockedTradeRocksDBStatic != null) {
            mockedTradeRocksDBStatic.close();
        }
        resetSingleton(TradeCache.class, "instance");
    }

    private void resetSingleton(Class<?> clazz, String fieldName) throws Exception {
        java.lang.reflect.Field instance = clazz.getDeclaredField(fieldName);
        instance.setAccessible(true);
        instance.set(null, null);
    }

    @Test
    @DisplayName("getInstance should return the same instance")
    void getInstance_ShouldReturnSameInstance() {
        TradeCache instance1 = TradeCache.getInstance();
        TradeCache instance2 = TradeCache.getInstance();

        assertSame(instance1, instance2, "getInstance should always return the same instance");
    }

    @Test
    @DisplayName("setTestInstance should set instance for testing")
    void setTestInstance_ShouldSetInstanceForTesting() {
        // Arrange
        TradeCache testInstance = mock(TradeCache.class);

        // Act
        TradeCache.setTestInstance(testInstance);
        TradeCache result = TradeCache.getInstance();

        // Assert
        assertSame(testInstance, result, "getInstance should return the test instance");
    }

    @Test
    @DisplayName("resetInstance should clear instance")
    void resetInstance_ShouldClearInstance() throws Exception {
        // Act
        TradeCache.resetInstance();
        
        // Verify instance is null
        java.lang.reflect.Field instance = TradeCache.class.getDeclaredField("instance");
        instance.setAccessible(true);
        assertNull(instance.get(null), "Instance should be null after reset");
    }

    @Test
    @DisplayName("getTrade should return empty Optional when trade not found")
    void getTrade_ShouldReturnEmptyOptional_WhenTradeNotFound() {
        // Arrange
        String tradeId = "nonexistent_trade";

        // Act
        Optional<Trade> result = tradeCache.getTrade(tradeId);

        // Assert
        assertTrue(result.isEmpty(), "Should return empty Optional when trade not found");
    }

    @Test
    @DisplayName("getTrade should return trade when found")
    void getTrade_ShouldReturnTrade_WhenFound() {
        // Act
        Optional<Trade> result = tradeCache.getTrade(testTrade.getIdentifier());

        // Assert
        assertTrue(result.isPresent(), "Should return Optional containing trade when found");
        assertEquals(testTrade.getIdentifier(), result.get().getIdentifier(), "Trade identifier should match");
    }

    @Test
    @DisplayName("getOrInitTrade should create new trade when not found")
    void getOrInitTrade_ShouldCreateNewTrade_WhenNotFound() {
        // Arrange
        String tradeId = "new_trade_id";

        // Act
        Trade result = tradeCache.getOrInitTrade(tradeId);

        // Assert
        assertNotNull(result, "Should return a non-null trade");
        assertEquals(tradeId, result.getIdentifier(), "Trade identifier should match");
        assertEquals(Trade.TradeStatus.UNPAID, result.getStatus(), "New trade should have UNPAID status");
    }

    @Test
    @DisplayName("getOrInitTrade should return existing trade when found")
    void getOrInitTrade_ShouldReturnExistingTrade_WhenFound() {
        // Act
        Trade result = tradeCache.getOrInitTrade(testTrade.getIdentifier());

        // Assert
        assertNotNull(result, "Should return a non-null trade");
        assertEquals(testTrade.getIdentifier(), result.getIdentifier(), "Trade identifier should match");
        assertEquals(testTrade.getPrice(), result.getPrice(), "Trade price should match existing trade");
    }

    @Test
    @DisplayName("getOrCreateTrade should create and add new trade when not found")
    void getOrCreateTrade_ShouldCreateAndAddNewTrade_WhenNotFound() {
        // Arrange
        String tradeId = "new_trade_id";

        // Act
        Trade result = tradeCache.getOrCreateTrade(tradeId);

        // Assert
        assertNotNull(result, "Should return a non-null trade");
        assertEquals(tradeId, result.getIdentifier(), "Trade identifier should match");

        // Verify trade was added to cache
        Optional<Trade> cachedTrade = tradeCache.getTrade(tradeId);
        assertTrue(cachedTrade.isPresent(), "Trade should be present in cache");
        assertEquals(tradeId, cachedTrade.get().getIdentifier(), "Cached trade identifier should match");
    }

    @Test
    @DisplayName("updateTrade should update trade in cache")
    void updateTrade_ShouldUpdateTradeInCache() {
        // Arrange
        Trade newTrade = TradeFactory.create();

        // Act
        tradeCache.updateTrade(newTrade);

        // Assert
        Optional<Trade> result = tradeCache.getTrade(newTrade.getIdentifier());
        assertTrue(result.isPresent(), "Trade should be present in cache after update");
        assertEquals(newTrade.getIdentifier(), result.get().getIdentifier(), "Trade identifier should match");
    }

    @Test
    @DisplayName("updateTrade should not update when trade is null")
    void updateTrade_ShouldNotUpdate_WhenTradeIsNull() {
        // Arrange
        reset(mockTradeRocksDB);  // Đảm bảo reset mock trước khi test
        int initialUpdateCount = 0;
        
        // Act
        tradeCache.updateTrade(null);
        
        // Assert
        java.lang.reflect.Field field;
        try {
            field = TradeCache.class.getDeclaredField("updateCounter");
            field.setAccessible(true);
            java.util.concurrent.atomic.AtomicInteger counter = 
                (java.util.concurrent.atomic.AtomicInteger) field.get(tradeCache);
            int currentCount = counter.get() - 1; // trừ đi 1 vì đã tăng khi setup
            assertEquals(initialUpdateCount, currentCount, 
                "Update counter should not increase when trade is null");
        } catch (Exception e) {
            fail("Failed to access updateCounter field: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("addTradeToBatch should add trade to batch")
    void addTradeToBatch_ShouldAddTradeToBatch() throws Exception {
        // Arrange
        Trade trade = TradeFactory.create();

        // Clear và cấu hình lại mock
        java.lang.reflect.Field latestTradesField = TradeCache.class.getDeclaredField("latestTrades");
        latestTradesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Trade> latestTrades = (Map<String, Trade>) latestTradesField.get(tradeCache);
        latestTrades.clear();

        // Act
        tradeCache.addTradeToBatch(trade);

        // Assert
        Map<String, Trade> updatedLatestTrades = (Map<String, Trade>) latestTradesField.get(tradeCache);
        assertTrue(updatedLatestTrades.containsKey(trade.getIdentifier()), "Trade should be added to batch");
    }

    @Test
    @DisplayName("addTradeToBatch should not add null trade")
    void addTradeToBatch_ShouldNotAdd_WhenTradeIsNull() {
        // Act
        tradeCache.addTradeToBatch(null);

        // Assert - verify that saveTradeBatch was not called
        verify(mockTradeRocksDB, never()).saveTradeBatch(any());
    }

    @Test
    @DisplayName("addTradeToBatch should not add trade with null identifier")
    void addTradeToBatch_ShouldNotAdd_WhenTradeIdentifierIsNull() {
        // Arrange
        Trade trade = TradeFactory.create();
        trade.setIdentifier(null);

        // Act
        tradeCache.addTradeToBatch(trade);

        // Assert - verify that saveTradeBatch was not called
        verify(mockTradeRocksDB, never()).saveTradeBatch(any());
    }

    @Test
    @DisplayName("addTradeToBatch should update trade when new trade has newer timestamp")
    void addTradeToBatch_ShouldUpdateTrade_WhenNewTradeHasNewerTimestamp() throws Exception {
        // Arrange
        Trade olderTrade = TradeFactory.create();
        olderTrade.setUpdatedAt(Instant.now().minusSeconds(100));
        
        Trade newerTrade = TradeFactory.create();
        newerTrade.setIdentifier(olderTrade.getIdentifier()); // Same ID
        newerTrade.setUpdatedAt(Instant.now()); // But newer timestamp
        
        // Clear map and add older trade
        java.lang.reflect.Field latestTradesField = TradeCache.class.getDeclaredField("latestTrades");
        latestTradesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Trade> latestTrades = (Map<String, Trade>) latestTradesField.get(tradeCache);
        latestTrades.clear();
        latestTrades.put(olderTrade.getIdentifier(), olderTrade);
        
        // Act
        tradeCache.addTradeToBatch(newerTrade);
        
        // Assert
        Map<String, Trade> updatedLatestTrades = (Map<String, Trade>) latestTradesField.get(tradeCache);
        assertEquals(newerTrade, updatedLatestTrades.get(newerTrade.getIdentifier()), 
                "Newer trade should replace older trade in batch");
    }

    @Test
    @DisplayName("addTradeToBatch should keep existing trade when new trade has older timestamp")
    void addTradeToBatch_ShouldKeepExistingTrade_WhenNewTradeHasOlderTimestamp() throws Exception {
        // Arrange
        Trade newerTrade = TradeFactory.create();
        newerTrade.setUpdatedAt(Instant.now());
        
        Trade olderTrade = TradeFactory.create();
        olderTrade.setIdentifier(newerTrade.getIdentifier()); // Same ID
        olderTrade.setUpdatedAt(Instant.now().minusSeconds(100)); // But older timestamp
        
        // Clear map and add newer trade
        java.lang.reflect.Field latestTradesField = TradeCache.class.getDeclaredField("latestTrades");
        latestTradesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Trade> latestTrades = (Map<String, Trade>) latestTradesField.get(tradeCache);
        latestTrades.clear();
        latestTrades.put(newerTrade.getIdentifier(), newerTrade);
        
        // Act
        tradeCache.addTradeToBatch(olderTrade);
        
        // Assert
        Map<String, Trade> updatedLatestTrades = (Map<String, Trade>) latestTradesField.get(tradeCache);
        assertEquals(newerTrade, updatedLatestTrades.get(newerTrade.getIdentifier()), 
                "Newer trade should be kept when older trade is added");
    }

    @Test
    @DisplayName("addTradeToBatch should flush when batch size reaches threshold")
    void addTradeToBatch_ShouldFlush_WhenBatchSizeReachesThreshold() throws Exception {
        // Arrange
        java.lang.reflect.Field thresholdField = TradeCache.class.getDeclaredField("UPDATE_THRESHOLD");
        thresholdField.setAccessible(true);
        int threshold = (int) thresholdField.get(null);
        
        // Clear latestTrades map
        java.lang.reflect.Field latestTradesField = TradeCache.class.getDeclaredField("latestTrades");
        latestTradesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Trade> latestTrades = (Map<String, Trade>) latestTradesField.get(tradeCache);
        latestTrades.clear();
        
        // Add trades until just below threshold
        for (int i = 0; i < threshold - 1; i++) {
            Trade trade = TradeFactory.create();
            tradeCache.addTradeToBatch(trade);
        }
        
        // Verify saveTradeBatch not called yet
        verify(mockTradeRocksDB, never()).saveTradeBatch(any());
        
        // Add one more trade to reach threshold
        Trade finalTrade = TradeFactory.create();
        tradeCache.addTradeToBatch(finalTrade);
        
        // Verify saveTradeBatch called
        verify(mockTradeRocksDB, times(1)).saveTradeBatch(any());
    }

    @Test
    @DisplayName("tradeCacheShouldFlush should return false when update count is zero")
    void tradeCacheShouldFlush_ShouldReturnFalse_WhenUpdateCountIsZero() throws Exception {
        // Arrange
        java.lang.reflect.Field counterField = TradeCache.class.getDeclaredField("updateCounter");
        counterField.setAccessible(true);
        java.util.concurrent.atomic.AtomicInteger counter = (java.util.concurrent.atomic.AtomicInteger) counterField.get(tradeCache);
        counter.set(0);
        
        // Act
        boolean result = tradeCache.tradeCacheShouldFlush();
        
        // Assert
        assertFalse(result, "Should return false when update count is zero");
    }

    @Test
    @DisplayName("tradeCacheShouldFlush should return true when update count is divisible by threshold")
    void tradeCacheShouldFlush_ShouldReturnTrue_WhenUpdateCountIsDivisibleByThreshold() throws Exception {
        // Arrange
        java.lang.reflect.Field thresholdField = TradeCache.class.getDeclaredField("UPDATE_THRESHOLD");
        thresholdField.setAccessible(true);
        int threshold = (int) thresholdField.get(null);
        
        java.lang.reflect.Field counterField = TradeCache.class.getDeclaredField("updateCounter");
        counterField.setAccessible(true);
        java.util.concurrent.atomic.AtomicInteger counter = (java.util.concurrent.atomic.AtomicInteger) counterField.get(tradeCache);
        counter.set(threshold);
        
        // Act
        boolean result = tradeCache.tradeCacheShouldFlush();
        
        // Assert
        assertTrue(result, "Should return true when update count is divisible by threshold");
    }

    @Test
    @DisplayName("flushTradeToDisk should not call saveTradeBatch when no trades to flush")
    void flushTradeToDisk_ShouldNotCallSaveTradeBatch_WhenNoTradesToFlush() throws Exception {
        // Arrange
        java.lang.reflect.Field latestTradesField = TradeCache.class.getDeclaredField("latestTrades");
        latestTradesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Trade> latestTrades = (Map<String, Trade>) latestTradesField.get(tradeCache);
        latestTrades.clear();
        
        // Act
        tradeCache.flushTradeToDisk();
        
        // Assert
        verify(mockTradeRocksDB, never()).saveTradeBatch(any());
    }

    @Test
    @DisplayName("flushTradeToDisk should call saveTradeBatch when trades exist")
    void flushTradeToDisk_ShouldCallSaveTradeBatch_WhenTradesExist() throws Exception {
        // Arrange
        java.lang.reflect.Field latestTradesField = TradeCache.class.getDeclaredField("latestTrades");
        latestTradesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Trade> latestTrades = (Map<String, Trade>) latestTradesField.get(tradeCache);
        latestTrades.clear();
        
        // Add trade to batch
        Trade trade = TradeFactory.create();
        latestTrades.put(trade.getIdentifier(), trade);
        
        // Act
        tradeCache.flushTradeToDisk();
        
        // Assert
        verify(mockTradeRocksDB, times(1)).saveTradeBatch(any());
        assertTrue(latestTrades.isEmpty(), "latestTrades should be cleared after flush");
    }

    @Test
    @DisplayName("removeTrade should remove trade from cache")
    void removeTrade_ShouldRemoveTradeFromCache() {
        // Arrange
        String identifier = testTrade.getIdentifier();
        assertTrue(tradeCache.getTrade(identifier).isPresent(), "Trade should be present before removal");
        
        // Act
        tradeCache.removeTrade(identifier);
        
        // Assert
        assertFalse(tradeCache.getTrade(identifier).isPresent(), "Trade should not be present after removal");
    }

    @Test
    @DisplayName("clearAll should clear all trades from cache")
    void clearAll_ShouldClearAllTradesFromCache() throws Exception {
        // Arrange
        // Ensure there's data in the cache
        java.lang.reflect.Field tradeCacheField = TradeCache.class.getDeclaredField("tradeCache");
        tradeCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Trade> tradeCacheMap = (ConcurrentHashMap<String, Trade>) tradeCacheField.get(tradeCache);
        assertTrue(tradeCacheMap.size() > 0, "Cache should have at least one trade before clearing");
        
        // Add trade to latestTrades
        java.lang.reflect.Field latestTradesField = TradeCache.class.getDeclaredField("latestTrades");
        latestTradesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Trade> latestTrades = (Map<String, Trade>) latestTradesField.get(tradeCache);
        Trade trade = TradeFactory.create();
        latestTrades.put(trade.getIdentifier(), trade);
        
        // Act
        tradeCache.clearAll();
        
        // Assert
        assertEquals(0, tradeCacheMap.size(), "Cache should be empty after clearing");
        assertTrue(latestTrades.isEmpty(), "latestTrades should be empty after clearing");
    }

    @Test
    @DisplayName("initializeTradeCache should update cache with trades from DB")
    void initializeTradeCache_ShouldUpdateCacheWithTradesFromDB() {
        // Arrange
        List<Trade> mockedTrades = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Trade trade = TradeFactory.create();
            trade.setUpdatedAt(Instant.now());
            mockedTrades.add(trade);
        }
        
        when(mockTradeRocksDB.getAllTrades()).thenReturn(mockedTrades);
        
        // Reset cache
        tradeCache.clearAll();
        
        // Act
        tradeCache.initializeTradeCache();
        
        // Assert - verify trades are added to cache
        java.lang.reflect.Field tradeCacheField;
        try {
            tradeCacheField = TradeCache.class.getDeclaredField("tradeCache");
            tradeCacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<String, Trade> tradeCacheMap = (ConcurrentHashMap<String, Trade>) tradeCacheField.get(tradeCache);
            
            assertEquals(5, tradeCacheMap.size(), "Cache should contain all trades from DB");
            
            // Đảm bảo tất cả các trades từ mock đều có trong cache
            for (Trade trade : mockedTrades) {
                Optional<Trade> cachedTrade = tradeCache.getTrade(trade.getIdentifier());
                assertTrue(cachedTrade.isPresent(), "Trade should be present in cache");
                assertEquals(trade.getIdentifier(), cachedTrade.get().getIdentifier(), 
                        "Cached trade identifier should match");
            }
        } catch (Exception e) {
            fail("Failed to access tradeCache field: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("initializeTradeCache should handle exception when RocksDB fails")
    void initializeTradeCache_ShouldHandleException_WhenRocksDBFails() {
        // Arrange
        when(mockTradeRocksDB.getAllTrades()).thenThrow(new RuntimeException("DB error"));
        
        // Reset cache
        tradeCache.clearAll();
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> tradeCache.initializeTradeCache());
    }

    @Test
    @DisplayName("initializeTradeCache should skip trades with null identifiers")
    void initializeTradeCache_ShouldSkipTrades_WithNullIdentifiers() {
        // Arrange
        List<Trade> mockedTrades = new ArrayList<>();
        Trade validTrade = TradeFactory.create();
        
        Trade invalidTrade = TradeFactory.create();
        invalidTrade.setIdentifier(null);
        
        mockedTrades.add(validTrade);
        mockedTrades.add(invalidTrade);
        
        when(mockTradeRocksDB.getAllTrades()).thenReturn(mockedTrades);
        
        // Reset cache
        tradeCache.clearAll();
        
        // Act
        tradeCache.initializeTradeCache();
        
        // Assert
        java.lang.reflect.Field tradeCacheField;
        try {
            tradeCacheField = TradeCache.class.getDeclaredField("tradeCache");
            tradeCacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<String, Trade> tradeCacheMap = (ConcurrentHashMap<String, Trade>) tradeCacheField.get(tradeCache);
            
            assertEquals(1, tradeCacheMap.size(), "Cache should only contain valid trades");
            assertTrue(tradeCache.getTrade(validTrade.getIdentifier()).isPresent(), "Valid trade should be in cache");
        } catch (Exception e) {
            fail("Failed to access tradeCache field: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("initializeTradeCache should skip trades with empty identifiers")
    void initializeTradeCache_ShouldSkipTrades_WithEmptyIdentifiers() {
        // Arrange
        List<Trade> mockedTrades = new ArrayList<>();
        Trade validTrade = TradeFactory.create();
        
        Trade invalidTrade = TradeFactory.create();
        invalidTrade.setIdentifier("");
        
        mockedTrades.add(validTrade);
        mockedTrades.add(invalidTrade);
        
        when(mockTradeRocksDB.getAllTrades()).thenReturn(mockedTrades);
        
        // Reset cache
        tradeCache.clearAll();
        
        // Act
        tradeCache.initializeTradeCache();
        
        // Assert
        java.lang.reflect.Field tradeCacheField;
        try {
            tradeCacheField = TradeCache.class.getDeclaredField("tradeCache");
            tradeCacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<String, Trade> tradeCacheMap = (ConcurrentHashMap<String, Trade>) tradeCacheField.get(tradeCache);
            
            assertEquals(1, tradeCacheMap.size(), "Cache should only contain valid trades");
            assertTrue(tradeCache.getTrade(validTrade.getIdentifier()).isPresent(), "Valid trade should be in cache");
        } catch (Exception e) {
            fail("Failed to access tradeCache field: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("initializeTradeCache should skip trades that are newer in cache")
    void initializeTradeCache_ShouldSkipTrades_WhenCacheIsNewer() {
        // Arrange
        Trade newerTrade = TradeFactory.create();
        newerTrade.setUpdatedAt(Instant.now());
        
        Trade olderTrade = TradeFactory.create();
        olderTrade.setIdentifier(newerTrade.getIdentifier()); // Same ID
        olderTrade.setUpdatedAt(Instant.now().minusSeconds(100)); // But older timestamp
        
        List<Trade> mockedTrades = new ArrayList<>();
        mockedTrades.add(olderTrade);
        
        when(mockTradeRocksDB.getAllTrades()).thenReturn(mockedTrades);
        
        // Reset and initialize cache with newer trade
        tradeCache.clearAll();
        tradeCache.updateTrade(newerTrade);
        reset(mockTradeRocksDB); // Reset mock to clear saveTrade call
        
        // Act
        tradeCache.initializeTradeCache();
        
        // Assert
        Optional<Trade> result = tradeCache.getTrade(newerTrade.getIdentifier());
        assertTrue(result.isPresent(), "Trade should be present in cache");
        assertEquals(newerTrade.getUpdatedAt(), result.get().getUpdatedAt(), 
                "Trade should not be updated with older version from DB");
    }
}
