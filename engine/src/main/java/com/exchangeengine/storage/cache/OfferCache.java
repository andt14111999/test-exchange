package com.exchangeengine.storage.cache;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.Offer;
import com.exchangeengine.storage.rocksdb.OfferRocksDB;

/**
 * Cache service for Offer
 * Using Singleton pattern to ensure only one instance
 */
public class OfferCache {
    private static final Logger logger = LoggerFactory.getLogger(OfferCache.class);
    private static volatile OfferCache instance;
    
    private final OfferRocksDB offerRocksDB;
    private final ConcurrentHashMap<String, Offer> offerCache = new ConcurrentHashMap<>();
    private final Map<String, Offer> latestOffers = new ConcurrentHashMap<>();
    
    // Atomic counter for updates (thread-safe)
    private final AtomicInteger updateCounter = new AtomicInteger(0);
    private static final int UPDATE_THRESHOLD = 100;
    
    /**
     * Get instance of OfferCache
     *
     * @return Instance of OfferCache
     */
    public static synchronized OfferCache getInstance() {
        if (instance == null) {
            instance = new OfferCache();
            instance.initializeOfferCache();
        }
        return instance;
    }
    
    /**
     * Set test instance for testing purposes
     * ONLY USE IN UNIT TESTS
     *
     * @param testInstance Test instance to use
     */
    public static void setTestInstance(OfferCache testInstance) {
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
    private OfferCache() {
        this.offerRocksDB = OfferRocksDB.getInstance();
    }
    
    /**
     * Initialize cache from RocksDB if needed
     */
    public void initializeOfferCache() {
        try {
            List<Offer> allOffers = offerRocksDB.getAllOffers();
            logger.info("Read {} offers from RocksDB", allOffers.size());
            
            int loadedCount = 0;
            for (Offer offer : allOffers) {
                String identifier = offer.getIdentifier();
                if (identifier != null && !identifier.isEmpty()) {
                    // Check if offer already exists in cache and update if the record is newer
                    Offer existingOffer = offerCache.get(identifier);
                    if (existingOffer == null || offer.getUpdatedAt().isAfter(existingOffer.getUpdatedAt())) {
                        offerCache.put(identifier, offer);
                        loadedCount++;
                    }
                }
            }
            
            logger.info("Offer cache initialized: {} records loaded", loadedCount);
        } catch (Exception e) {
            logger.error("Cannot initialize offer cache: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get Offer from cache
     *
     * @param identifier Identifier of offer
     * @return Optional containing Offer if found
     */
    public Optional<Offer> getOffer(String identifier) {
        return Optional.ofNullable(offerCache.get(identifier));
    }
    
    /**
     * Get or initialize Offer from cache
     *
     * @param identifier Identifier of offer
     * @return Offer with default values
     */
    public Offer getOrInitOffer(String identifier) {
        return getOffer(identifier).orElseGet(() -> Offer.builder()
                .identifier(identifier)
                .build());
    }
    
    /**
     * Get Offer from cache, create new and save to cache if not exists
     *
     * @param identifier Identifier of offer
     * @return Offer
     */
    public Offer getOrCreateOffer(String identifier) {
        Offer offer = getOrInitOffer(identifier);
        offerCache.put(identifier, offer);
        return offer;
    }
    
    /**
     * Update offer in cache
     *
     * @param offer Offer to update
     */
    public void updateOffer(Offer offer) {
        if (offer == null) {
            logger.warn("Cannot update null offer");
            return;
        }
        
        updateCounter.incrementAndGet();
        String identifier = offer.getIdentifier();
        offerCache.put(identifier, offer);
        offerRocksDB.saveOffer(offer);
    }
    
    /**
     * Add offer to batch for later saving
     *
     * @param offer Offer to add to batch
     */
    public void addOfferToBatch(Offer offer) {
        if (offer == null || offer.getIdentifier() == null) {
            logger.warn("Cannot add null offer or with null ID to batch");
            return;
        }
        
        String identifier = offer.getIdentifier();
        offerCache.put(identifier, offer);
        
        // Use compute to only save the latest version based on timestamp
        latestOffers.compute(identifier, (key, existingOffer) -> {
            if (existingOffer == null || offer.getUpdatedAt().isAfter(existingOffer.getUpdatedAt())) {
                return offer;
            }
            return existingOffer;
        });
        
        // If batch is large enough, flush to RocksDB
        if (latestOffers.size() >= UPDATE_THRESHOLD) {
            flushOfferToDisk();
        }
    }
    
    /**
     * Check if batch should be flushed
     *
     * @return true if should flush
     */
    public boolean offerCacheShouldFlush() {
        // Return true when update count is divisible by UPDATE_THRESHOLD
        int count = updateCounter.get();
        return count > 0 && count % UPDATE_THRESHOLD == 0;
    }
    
    /**
     * Flush batch offer to RocksDB
     */
    public void flushOfferToDisk() {
        if (latestOffers.isEmpty()) {
            logger.debug("No offers in batch to flush");
            return;
        }
        
        logger.info("Saving {} offers to RocksDB", latestOffers.size());
        offerRocksDB.saveOfferBatch(latestOffers);
        latestOffers.clear();
        logger.info("Offer flush to RocksDB completed");
    }
    
    /**
     * Remove offer from cache
     *
     * @param identifier Identifier of offer to remove
     */
    public void removeOffer(String identifier) {
        offerCache.remove(identifier);
    }
    
    /**
     * Clear all offers from cache
     */
    public void clearAll() {
        offerCache.clear();
        latestOffers.clear();
    }
    
    /**
     * Get number of offers in cache
     *
     * @return Number of offers in cache
     */
    public long size() {
        return offerCache.size();
    }
}
