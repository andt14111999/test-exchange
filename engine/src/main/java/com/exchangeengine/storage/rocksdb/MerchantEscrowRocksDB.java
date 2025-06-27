package com.exchangeengine.storage.rocksdb;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.MerchantEscrow;

/**
 * RocksDB service for Merchant Escrow
 * Uses Singleton pattern to ensure only one instance
 */
public class MerchantEscrowRocksDB {
    private static final Logger logger = LoggerFactory.getLogger(MerchantEscrowRocksDB.class);
    private static volatile MerchantEscrowRocksDB instance;
    private final RocksDBService rocksDBService;
    
    /**
     * Get instance of MerchantEscrowRocksDB
     *
     * @return Instance of MerchantEscrowRocksDB
     */
    public static synchronized MerchantEscrowRocksDB getInstance() {
        if (instance == null) {
            instance = new MerchantEscrowRocksDB();
        }
        return instance;
    }

    /**
     * Set test instance (only for testing)
     *
     * @param testInstance Test instance to set
     */
    public static void setTestInstance(MerchantEscrowRocksDB testInstance) {
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
    private MerchantEscrowRocksDB() {
        this.rocksDBService = RocksDBService.getInstance();
    }
    
    /**
     * Constructor with pre-initialized RocksDBService (only for testing)
     *
     * @param rocksDBService RocksDBService instance
     */
    public MerchantEscrowRocksDB(RocksDBService rocksDBService) {
        this.rocksDBService = rocksDBService;
    }

    /**
     * Get MerchantEscrow from RocksDB
     *
     * @param identifier ID of merchant escrow
     * @return Optional containing merchant escrow if exists
     */
    public Optional<MerchantEscrow> getMerchantEscrow(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            logger.warn("Cannot get merchant escrow with null or empty ID");
            return Optional.empty();
        }

        return rocksDBService.getObject(
            identifier,
            rocksDBService.getMerchantEscrowCF(),
            MerchantEscrow.class,
            "merchant_escrow"
        );
    }

    /**
     * Get all MerchantEscrows from RocksDB
     *
     * @return List of merchant escrows
     */
    public List<MerchantEscrow> getAllMerchantEscrows() {
        return rocksDBService.getAllObjects(
            rocksDBService.getMerchantEscrowCF(),
            MerchantEscrow.class,
            "merchant_escrows"
        );
    }

    /**
     * Save MerchantEscrow to RocksDB
     *
     * @param merchantEscrow Merchant escrow to save
     */
    public void saveMerchantEscrow(MerchantEscrow merchantEscrow) {
        if (merchantEscrow == null) {
            logger.warn("Cannot save null merchant escrow");
            return;
        }

        rocksDBService.saveObject(
            merchantEscrow,
            rocksDBService.getMerchantEscrowCF(),
            MerchantEscrow::getIdentifier,
            "merchant_efscrow"
        );
    }

    /**
     * Save batch of MerchantEscrows to RocksDB
     *
     * @param merchantEscrows Map containing merchant escrows to save
     */
    public void saveMerchantEscrowBatch(Map<String, MerchantEscrow> merchantEscrows) {
        if (merchantEscrows == null || merchantEscrows.isEmpty()) {
            return;
        }

        rocksDBService.saveBatch(
            merchantEscrows,
            rocksDBService.getMerchantEscrowCF(),
            MerchantEscrow::getIdentifier,
            "merchant_escrows"
        );
    }
} 