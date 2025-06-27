package com.exchangeengine.storage.rocksdb;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.Trade;

/**
 * RocksDB service for Trade
 * Uses Singleton pattern to ensure only one instance
 */
public class TradeRocksDB {
    private static final Logger logger = LoggerFactory.getLogger(TradeRocksDB.class);
    private static volatile TradeRocksDB instance;
    private final RocksDBService rocksDBService;
    
    /**
     * Get instance of TradeRocksDB
     *
     * @return Instance of TradeRocksDB
     */
    public static synchronized TradeRocksDB getInstance() {
        if (instance == null) {
            instance = new TradeRocksDB();
        }
        return instance;
    }

    /**
     * Set test instance (only for testing)
     *
     * @param testInstance Test instance to set
     */
    public static void setTestInstance(TradeRocksDB testInstance) {
        instance = testInstance;
    }

    /**
     * Reset instance to null (only for testing)
     */
    public static void resetInstance() {
        instance = null;
    }

    /**
     * Private constructor to ensure Singleton pattern
     */
    private TradeRocksDB() {
        this.rocksDBService = RocksDBService.getInstance();
    }
    
    /**
     * Constructor with pre-initialized RocksDBService (only for testing)
     *
     * @param rocksDBService RocksDBService instance
     */
    public TradeRocksDB(RocksDBService rocksDBService) {
        this.rocksDBService = rocksDBService;
    }

    /**
     * Get Trade from RocksDB
     *
     * @param identifier ID of trade
     * @return Optional containing trade if exists
     */
    public Optional<Trade> getTrade(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            logger.warn("Cannot get trade with null or empty ID");
            return Optional.empty();
        }

        return rocksDBService.getObject(
            identifier,
            rocksDBService.getTradeCF(),
            Trade.class,
            "trade"
        );
    }

    /**
     * Get all Trades from RocksDB
     *
     * @return List of trades
     */
    public List<Trade> getAllTrades() {
        return rocksDBService.getAllObjects(
            rocksDBService.getTradeCF(),
            Trade.class,
            "trades"
        );
    }

    /**
     * Save Trade to RocksDB
     *
     * @param trade Trade to save
     */
    public void saveTrade(Trade trade) {
        if (trade == null) {
            logger.warn("Cannot save null trade");
            return;
        }

        rocksDBService.saveObject(
            trade,
            rocksDBService.getTradeCF(),
            Trade::getIdentifier,
            "trade"
        );
    }

    /**
     * Save batch of Trades to RocksDB
     *
     * @param trades Map containing trades to save
     */
    public void saveTradeBatch(Map<String, Trade> trades) {
        if (trades == null || trades.isEmpty()) {
            return;
        }

        rocksDBService.saveBatch(
            trades,
            rocksDBService.getTradeCF(),
            Trade::getIdentifier,
            "trades"
        );
    }
}