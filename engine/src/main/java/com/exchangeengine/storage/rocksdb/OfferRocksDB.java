package com.exchangeengine.storage.rocksdb;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.Offer;

/**
 * RocksDB service for Offer
 * Uses Singleton pattern to ensure only one instance
 */
public class OfferRocksDB {
    private static final Logger logger = LoggerFactory.getLogger(OfferRocksDB.class);
    private static volatile OfferRocksDB instance;
    private final RocksDBService rocksDBService;
    
    /**
     * Get instance of OfferRocksDB
     *
     * @return Instance of OfferRocksDB
     */
    public static synchronized OfferRocksDB getInstance() {
        if (instance == null) {
            instance = new OfferRocksDB();
        }
        return instance;
    }

    /**
     * Set test instance (only for testing)
     *
     * @param testInstance Test instance to set
     */
    public static void setTestInstance(OfferRocksDB testInstance) {
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
    private OfferRocksDB() {
        this.rocksDBService = RocksDBService.getInstance();
    }
    
    /**
     * Constructor with pre-initialized RocksDBService (only for testing)
     *
     * @param rocksDBService RocksDBService instance
     */
    public OfferRocksDB(RocksDBService rocksDBService) {
        this.rocksDBService = rocksDBService;
    }

    /**
     * Get Offer from RocksDB
     *
     * @param identifier ID of offer
     * @return Optional containing offer if exists
     */
    public Optional<Offer> getOffer(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            logger.warn("Cannot get offer with null or empty ID");
            return Optional.empty();
        }

        return rocksDBService.getObject(
            identifier,
            rocksDBService.getOfferCF(),
            Offer.class,
            "offer"
        );
    }

    /**
     * Get all Offers from RocksDB
     *
     * @return List of offers
     */
    public List<Offer> getAllOffers() {
        return rocksDBService.getAllObjects(
            rocksDBService.getOfferCF(),
            Offer.class,
            "offers"
        );
    }

    /**
     * Save Offer to RocksDB
     *
     * @param offer Offer to save
     */
    public void saveOffer(Offer offer) {
        if (offer == null) {
            logger.warn("Cannot save null offer");
            return;
        }

        rocksDBService.saveObject(
            offer,
            rocksDBService.getOfferCF(),
            Offer::getIdentifier,
            "offer"
        );
    }

    /**
     * Save batch of Offers to RocksDB
     *
     * @param offers Map containing offers to save
     */
    public void saveOfferBatch(Map<String, Offer> offers) {
        if (offers == null || offers.isEmpty()) {
            return;
        }

        rocksDBService.saveBatch(
            offers,
            rocksDBService.getOfferCF(),
            Offer::getIdentifier,
            "offers"
        );
    }
}
