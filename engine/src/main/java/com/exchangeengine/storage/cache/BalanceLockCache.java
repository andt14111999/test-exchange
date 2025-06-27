package com.exchangeengine.storage.cache;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.BalanceLock;
import com.exchangeengine.storage.rocksdb.BalanceLockRocksDB;

/**
 * Cache service cho BalanceLock
 */
public class BalanceLockCache {
    private static final Logger logger = LoggerFactory.getLogger(BalanceLockCache.class);
    private static volatile BalanceLockCache instance;

    private final Map<String, BalanceLock> locks = new ConcurrentHashMap<>();
    private final Map<String, BalanceLock> lockBatch = new ConcurrentHashMap<>();
    private final BalanceLockRocksDB balanceLockRocksDB = BalanceLockRocksDB.getInstance();

    /**
     * Lấy instance của BalanceLockCache
     *
     * @return Instance của BalanceLockCache
     */
    public static synchronized BalanceLockCache getInstance() {
        if (instance == null) {
            instance = new BalanceLockCache();
        }
        return instance;
    }

    /**
     * Thiết lập instance kiểm thử (chỉ sử dụng cho testing)
     *
     * @param testInstance Instance kiểm thử cần thiết lập
     */
    public static void setTestInstance(BalanceLockCache testInstance) {
        instance = testInstance;
    }

    /**
     * Reset instance (chỉ sử dụng cho testing)
     */
    public static void resetInstance() {
        instance = null;
    }

    /**
     * Private constructor để đảm bảo singleton pattern
     */
    private BalanceLockCache() {
        super();
    }

    /**
     * Lấy BalanceLock theo lockId
     *
     * @param lockId ID của khóa
     * @return Optional chứa BalanceLock hoặc empty nếu không tìm thấy
     */
    public Optional<BalanceLock> getBalanceLock(String lockId) {
        BalanceLock lock = locks.get(lockId);
        return Optional.ofNullable(lock);
    }

    /**
     * Lấy tất cả khóa của một tài khoản
     *
     * @param accountKey Khóa tài khoản
     * @return Set chứa tất cả lockId của tài khoản
     */
    public Set<String> getAccountLocks(String accountKey) {
        return locks.entrySet().stream()
            .filter(entry -> entry.getValue().getAccountKeys() != null)
            .filter(entry -> entry.getValue().getAccountKeys().contains(accountKey))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    /**
     * Lấy tất cả khóa đang hoạt động của một tài khoản
     *
     * @param accountKey Khóa tài khoản
     * @return Set chứa tất cả lockId đang hoạt động của tài khoản
     */
    public Set<String> getActiveAccountLocks(String accountKey) {
        return locks.entrySet().stream()
            .filter(entry -> entry.getValue().getAccountKeys() != null)
            .filter(entry -> entry.getValue().getAccountKeys().contains(accountKey))
            .filter(entry -> "LOCKED".equals(entry.getValue().getStatus()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    /**
     * Thêm BalanceLock vào cache
     *
     * @param lock BalanceLock cần thêm
     * @return BalanceLock đã thêm
     */
    public BalanceLock addBalanceLock(BalanceLock lock) {
        locks.put(lock.getLockId(), lock);
        addBalanceLockToBatch(lock);
        return lock;
    }

    /**
     * Thêm BalanceLock vào batch để lưu trữ
     *
     * @param lock BalanceLock cần thêm
     * @return BalanceLock đã thêm
     */
    public BalanceLock addBalanceLockToBatch(BalanceLock lock) {
        lockBatch.put(lock.getLockId(), lock);
        return lock;
    }

    /**
     * Load BalanceLocks từ RocksDB
     */
    public void loadBalanceLocksFromRocksDB() {
        try {
            // Use dedicated BalanceLockRocksDB service
            List<BalanceLock> allBalanceLocks = balanceLockRocksDB.getAllBalanceLocks();
            
            // Add all locks to cache
            for (BalanceLock lock : allBalanceLocks) {
                locks.put(lock.getLockId(), lock);
            }
            
            logger.info("Loaded {} balance locks from BalanceLockRocksDB", allBalanceLocks.size());
        } catch (Exception e) {
            logger.error("Error loading balance locks with BalanceLockRocksDB: {}", e.getMessage(), e);
        }
        
        logger.info("Loaded {} balance locks from RocksDB", locks.size());
    }

    /**
     * Lưu BalanceLock batch vào RocksDB
     */
    public void saveBalanceLockBatch() {
        try {
            // Use the dedicated BalanceLockRocksDB service
            balanceLockRocksDB.saveBalanceLockBatch(lockBatch);
        } catch (Exception e) {
            logger.error("Error saving balance locks batch with BalanceLockRocksDB: {}", e.getMessage(), e);
        }

        lockBatch.clear();
    }

    /**
     * Lấy tất cả khóa trong cache
     *
     * @return Map chứa tất cả khóa
     */
    public Map<String, BalanceLock> getBalanceLocks() {
        return locks;
    }

    /**
     * Xóa khóa khỏi cache
     *
     * @param lockId ID của khóa cần xóa
     */
    public void removeBalanceLock(String lockId) {
        locks.remove(lockId);
    }

    /**
     * Xóa tất cả khóa khỏi cache
     */
    public void clearBalanceLocks() {
        locks.clear();
    }
} 