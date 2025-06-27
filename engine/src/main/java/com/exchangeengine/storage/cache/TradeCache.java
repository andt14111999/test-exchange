package com.exchangeengine.storage.cache;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.Trade;
import com.exchangeengine.model.Trade.TradeStatus;
import com.exchangeengine.storage.rocksdb.TradeRocksDB;

/**
 * Cache service for Trade
 * Using Singleton pattern to ensure only one instance
 */
public class TradeCache {
    private static final Logger logger = LoggerFactory.getLogger(TradeCache.class);
    private static volatile TradeCache instance;
    
    private final TradeRocksDB tradeRocksDB;
    private final ConcurrentHashMap<String, Trade> tradeCache = new ConcurrentHashMap<>();
    private final Map<String, Trade> latestTrades = new ConcurrentHashMap<>();
    
    // Atomic counter for updates (thread-safe)
    private final AtomicInteger updateCounter = new AtomicInteger(0);
    private static final int UPDATE_THRESHOLD = 100;
    
    /**
     * Get instance of TradeCache
     *
     * @return Instance of TradeCache
     */
    public static synchronized TradeCache getInstance() {
        if (instance == null) {
            instance = new TradeCache();
            instance.initializeTradeCache();
        }
        return instance;
    }
    
    /**
     * Set test instance for testing purposes
     * ONLY USE IN UNIT TESTS
     *
     * @param testInstance Test instance to use
     */
    public static void setTestInstance(TradeCache testInstance) {
        instance = testInstance;
    }
    
    /**
     * Reset instance to null (for testing only)
     */
    public static void resetInstance() {
        instance = null;
    }
    
    /**
     * Private constructor to ensure Singleton pattern
     */
    private TradeCache() {
        this.tradeRocksDB = TradeRocksDB.getInstance();
    }
    
    /**
     * Initialize cache from RocksDB if needed
     */
    public void initializeTradeCache() {
        try {
            List<Trade> allTrades = tradeRocksDB.getAllTrades();
            logger.info("Read {} trades from RocksDB", allTrades.size());
            
            int loadedCount = 0;
            for (Trade trade : allTrades) {
                String identifier = trade.getIdentifier();
                if (identifier != null && !identifier.isEmpty()) {
                    // Check if trade already exists in cache and update if the record is newer
                    Trade existingTrade = tradeCache.get(identifier);
                    if (existingTrade == null || trade.getUpdatedAt().isAfter(existingTrade.getUpdatedAt())) {
                        tradeCache.put(identifier, trade);
                        loadedCount++;
                    }
                }
            }
            
            logger.info("Trade cache initialized: {} records loaded", loadedCount);
        } catch (Exception e) {
            logger.error("Cannot initialize trade cache: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get Trade from cache
     *
     * @param identifier Identifier of trade
     * @return Optional containing Trade if found
     */
    public Optional<Trade> getTrade(String identifier) {
        return Optional.ofNullable(tradeCache.get(identifier));
    }
    
    /**
     * Get or initialize Trade from cache
     *
     * @param identifier Identifier of trade
     * @return Trade with default values
     */
    public Trade getOrInitTrade(String identifier) {
        return getTrade(identifier).orElseGet(() -> Trade.builder()
                .identifier(identifier)
                .status(TradeStatus.UNPAID)
                .build());
    }
    
    /**
     * Get Trade from cache, create new and save to cache if not exists
     *
     * @param identifier Identifier of trade
     * @return Trade
     */
    public Trade getOrCreateTrade(String identifier) {
        Trade trade = getOrInitTrade(identifier);
        tradeCache.put(identifier, trade);
        return trade;
    }
    
    /**
     * Update trade in cache
     *
     * @param trade Trade to update
     */
    public void updateTrade(Trade trade) {
        if (trade == null) {
            logger.warn("Cannot update null trade");
            return;
        }
        
        updateCounter.incrementAndGet();
        String identifier = trade.getIdentifier();
        tradeCache.put(identifier, trade);
        tradeRocksDB.saveTrade(trade);
    }
    
    /**
     * Add trade to batch for later saving
     *
     * @param trade Trade to add to batch
     */
    public void addTradeToBatch(Trade trade) {
        if (trade == null || trade.getIdentifier() == null) {
            logger.warn("Cannot add null trade or with null ID to batch");
            return;
        }
        
        String identifier = trade.getIdentifier();
        tradeCache.put(identifier, trade);
        
        // Use compute to only save the latest version based on timestamp
        latestTrades.compute(identifier, (key, existingTrade) -> {
            if (existingTrade == null || trade.getUpdatedAt().isAfter(existingTrade.getUpdatedAt())) {
                return trade;
            }
            return existingTrade;
        });
        
        // If batch is large enough, flush to RocksDB
        if (latestTrades.size() >= UPDATE_THRESHOLD) {
            flushTradeToDisk();
        }
    }
    
    /**
     * Check if batch should be flushed
     *
     * @return true if should flush
     */
    public boolean tradeCacheShouldFlush() {
        // Return true when update count is divisible by UPDATE_THRESHOLD
        int count = updateCounter.get();
        return count > 0 && count % UPDATE_THRESHOLD == 0;
    }
    
    /**
     * Flush batch trade to RocksDB
     */
    public void flushTradeToDisk() {
        if (latestTrades.isEmpty()) {
            logger.debug("No trades in batch to flush");
            return;
        }
        
        logger.info("Saving {} trades to RocksDB", latestTrades.size());
        tradeRocksDB.saveTradeBatch(latestTrades);
        latestTrades.clear();
        logger.info("Trade flush to RocksDB completed");
    }
    
    /**
     * Remove trade from cache
     *
     * @param identifier Identifier of trade to remove
     */
    public void removeTrade(String identifier) {
        tradeCache.remove(identifier);
    }
    
    /**
     * Clear all trades from cache
     */
    public void clearAll() {
        tradeCache.clear();
        latestTrades.clear();
    }
}
