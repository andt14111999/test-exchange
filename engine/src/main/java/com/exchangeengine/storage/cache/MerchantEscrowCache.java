package com.exchangeengine.storage.cache;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.MerchantEscrow;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.rocksdb.MerchantEscrowRocksDB;

/**
 * Cache service for MerchantEscrow
 * Using Singleton pattern to ensure only one instance
 */
public class MerchantEscrowCache {
    private static final Logger logger = LoggerFactory.getLogger(MerchantEscrowCache.class);
    private static volatile MerchantEscrowCache instance;
    
    private final MerchantEscrowRocksDB merchantEscrowRocksDB;
    private final ConcurrentHashMap<String, MerchantEscrow> merchantEscrowCache = new ConcurrentHashMap<>();
    private final Map<String, MerchantEscrow> latestMerchantEscrows = new ConcurrentHashMap<>();
    
    // Atomic counter for updates (thread-safe)
    private final AtomicInteger updateCounter = new AtomicInteger(0);
    private static final int UPDATE_THRESHOLD = 100;
    
    /**
     * Get instance of MerchantEscrowCache
     *
     * @return Instance of MerchantEscrowCache
     */
    public static synchronized MerchantEscrowCache getInstance() {
        if (instance == null) {
            instance = new MerchantEscrowCache();
            instance.initializeMerchantEscrowCache();
        }
        return instance;
    }
    
    /**
     * Set test instance for testing purposes
     * ONLY USE IN UNIT TESTS
     *
     * @param testInstance Test instance to use
     */
    public static void setTestInstance(MerchantEscrowCache testInstance) {
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
    private MerchantEscrowCache() {
        this.merchantEscrowRocksDB = MerchantEscrowRocksDB.getInstance();
    }
    
    /**
     * Initialize cache from RocksDB if needed
     */
    public void initializeMerchantEscrowCache() {
        try {
            List<MerchantEscrow> allMerchantEscrows = merchantEscrowRocksDB.getAllMerchantEscrows();
            logger.info("Read {} merchant escrows from RocksDB", allMerchantEscrows.size());
            
            int loadedCount = 0;
            for (MerchantEscrow merchantEscrow : allMerchantEscrows) {
                String identifier = merchantEscrow.getIdentifier();
                if (identifier != null && !identifier.isEmpty()) {
                    // Kiểm tra xem đã tồn tại trong cache chưa và cập nhật nếu bản ghi mới hơn
                    MerchantEscrow existingEscrow = merchantEscrowCache.get(identifier);
                    if (existingEscrow == null || merchantEscrow.getUpdatedAt() > existingEscrow.getUpdatedAt()) {
                        merchantEscrowCache.put(identifier, merchantEscrow);
                        loadedCount++;
                    }
                }
            }
            
            logger.info("Merchant escrow cache initialized: {} records loaded", loadedCount);
        } catch (Exception e) {
            logger.error("Cannot initialize merchant escrow cache: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get MerchantEscrow from cache
     *
     * @param identifier Identifier of merchant escrow
     * @return Optional containing MerchantEscrow if found
     */
    public Optional<MerchantEscrow> getMerchantEscrow(String identifier) {
        return Optional.ofNullable(merchantEscrowCache.get(identifier));
    }
    
    /**
     * Get or initialize MerchantEscrow from cache
     *
     * @param identifier Identifier of merchant escrow
     * @return MerchantEscrow
     */
    public MerchantEscrow getOrInitMerchantEscrow(String identifier) {
        return getMerchantEscrow(identifier).orElseGet(() -> {
            // Initialize a new MerchantEscrow with default values
            return new MerchantEscrow(
                identifier,
                null, // usdtAccountId
                null, // fiatAccountId
                OperationType.MERCHANT_ESCROW_MINT, // operationType
                BigDecimal.ZERO, // usdtAmount
                BigDecimal.ZERO, // fiatAmount
                "USD", // fiatCurrency
                null,  // userId
                "merchant-escrow-op-" + identifier // merchantEscrowOperationId
            );
        });
    }
    
    /**
     * Get MerchantEscrow from cache, create new and save to cache if not exists
     *
     * @param identifier Identifier of merchant escrow
     * @return MerchantEscrow
     */
    public MerchantEscrow getOrCreateMerchantEscrow(String identifier) {
        MerchantEscrow merchantEscrow = getOrInitMerchantEscrow(identifier);
        merchantEscrowCache.put(identifier, merchantEscrow);
        return merchantEscrow;
    }
    
    /**
     * Update merchant escrow in cache
     *
     * @param merchantEscrow Merchant escrow to update
     */
    public void updateMerchantEscrow(MerchantEscrow merchantEscrow) {
        if (merchantEscrow == null) {
            logger.warn("Cannot update null merchant escrow");
            return;
        }
        
        updateCounter.incrementAndGet();
        String identifier = merchantEscrow.getIdentifier();
        merchantEscrowCache.put(identifier, merchantEscrow);
        merchantEscrowRocksDB.saveMerchantEscrow(merchantEscrow);
    }
    
    /**
     * Add merchant escrow to batch for later saving
     *
     * @param merchantEscrow Merchant escrow to add to batch
     */
    public void addMerchantEscrowToBatch(MerchantEscrow merchantEscrow) {
        if (merchantEscrow == null || merchantEscrow.getIdentifier() == null) {
            logger.warn("Cannot add null merchant escrow or with null ID to batch");
            return;
        }
        
        String identifier = merchantEscrow.getIdentifier();
        merchantEscrowCache.put(identifier, merchantEscrow);
        
        // Use compute to only save the latest version based on timestamp
        latestMerchantEscrows.compute(identifier, (key, existingEscrow) -> {
            if (existingEscrow == null || merchantEscrow.getUpdatedAt() > existingEscrow.getUpdatedAt()) {
                return merchantEscrow;
            }
            return existingEscrow;
        });
        
        // If batch is large enough, flush to RocksDB
        if (latestMerchantEscrows.size() >= UPDATE_THRESHOLD) {
            flushMerchantEscrowToDisk();
        }
    }
    
    /**
     * Check if batch should be flushed
     *
     * @return true if should flush
     */
    public boolean merchantEscrowCacheShouldFlush() {
        // Return true when update count is divisible by UPDATE_THRESHOLD
        int count = updateCounter.get();
        return count > 0 && count % UPDATE_THRESHOLD == 0;
    }
    
    /**
     * Flush batch merchant escrow to RocksDB
     */
    public void flushMerchantEscrowToDisk() {
        if (latestMerchantEscrows.isEmpty()) {
            logger.debug("No merchant escrows in batch to flush");
            return;
        }
        
        logger.info("Saving {} merchant escrows to RocksDB", latestMerchantEscrows.size());
        merchantEscrowRocksDB.saveMerchantEscrowBatch(latestMerchantEscrows);
        latestMerchantEscrows.clear();
        logger.info("Merchant escrow flush to RocksDB completed");
    }
} 