package com.exchangeengine.storage.rocksdb;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.BalanceLock;

/**
 * RocksDB service for BalanceLock
 * Uses Singleton pattern to ensure only one instance
 */
public class BalanceLockRocksDB {
    private static final Logger logger = LoggerFactory.getLogger(BalanceLockRocksDB.class);
    private static volatile BalanceLockRocksDB instance;
    private final RocksDBService rocksDBService;
    
    /**
     * Get instance of BalanceLockRocksDB
     *
     * @return Instance of BalanceLockRocksDB
     */
    public static synchronized BalanceLockRocksDB getInstance() {
        if (instance == null) {
            instance = new BalanceLockRocksDB();
        }
        return instance;
    }

    /**
     * Set test instance (only for testing)
     *
     * @param testInstance Test instance to set
     */
    public static void setTestInstance(BalanceLockRocksDB testInstance) {
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
    private BalanceLockRocksDB() {
        this.rocksDBService = RocksDBService.getInstance();
    }

    /**
     * Get BalanceLock from RocksDB
     *
     * @param lockId ID of balance lock
     * @return Optional containing balance lock if exists
     */
    public Optional<BalanceLock> getBalanceLock(String lockId) {
        if (lockId == null || lockId.isEmpty()) {
            logger.warn("Cannot get balance lock with null or empty ID");
            return Optional.empty();
        }

        return rocksDBService.getObject(
            lockId,
            rocksDBService.getBalanceLockCF(),
            BalanceLock.class,
            "balance_lock"
        );
    }

    /**
     * Get all BalanceLocks from RocksDB
     *
     * @return List of balance locks
     */
    public List<BalanceLock> getAllBalanceLocks() {
        return rocksDBService.getAllObjects(
            rocksDBService.getBalanceLockCF(),
            BalanceLock.class,
            "balance_locks"
        );
    }

    /**
     * Save BalanceLock to RocksDB
     *
     * @param balanceLock BalanceLock to save
     */
    public void saveBalanceLock(BalanceLock balanceLock) {
        if (balanceLock == null) {
            logger.warn("Cannot save null balance lock");
            return;
        }

        rocksDBService.saveObject(
            balanceLock,
            rocksDBService.getBalanceLockCF(),
            BalanceLock::getLockId,
            "balance_lock"
        );
    }

    /**
     * Save batch of BalanceLocks to RocksDB
     *
     * @param balanceLocks Map containing balance locks to save
     */
    public void saveBalanceLockBatch(Map<String, BalanceLock> balanceLocks) {
        if (balanceLocks == null || balanceLocks.isEmpty()) {
            return;
        }

        rocksDBService.saveBatch(
            balanceLocks,
            rocksDBService.getBalanceLockCF(),
            BalanceLock::getLockId,
            "balance_locks"
        );
    }
} 