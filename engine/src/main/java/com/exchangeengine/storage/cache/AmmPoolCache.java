package com.exchangeengine.storage.cache;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.AmmPool;
import com.exchangeengine.storage.rocksdb.AmmPoolRocksDB;

/**
 * Cache service cho AmmPool
 * Sử dụng Singleton pattern để đảm bảo chỉ có một instance duy nhất
 */
public class AmmPoolCache {
  private static final Logger logger = LoggerFactory.getLogger(AmmPoolCache.class);

  private static volatile AmmPoolCache instance;
  private final AmmPoolRocksDB ammPoolRocksDB = AmmPoolRocksDB.getInstance();

  private final ConcurrentHashMap<String, AmmPool> ammPoolCache = new ConcurrentHashMap<>();
  private final Map<String, AmmPool> latestAmmPools = new ConcurrentHashMap<>();

  // Biến đếm số lần cập nhật (atomic để đảm bảo thread-safe)
  private final AtomicInteger updateCounter = new AtomicInteger(0);
  private static final int UPDATE_THRESHOLD = 100;

  /**
   * Lấy instance của AmmPoolCache.
   *
   * @return Instance của AmmPoolCache
   */
  public static synchronized AmmPoolCache getInstance() {
    if (instance == null) {
      instance = new AmmPoolCache();
      instance.initializeAmmPoolCache();
    }
    return instance;
  }

  /**
   * Constructor riêng tư để đảm bảo Singleton pattern
   */
  private AmmPoolCache() {
  }

  /**
   * Thiết lập instance cho mục đích testing.
   * CHỈ SỬ DỤNG TRONG UNIT TEST.
   *
   * @param testInstance Instance để sử dụng cho testing
   */
  public static void setTestInstance(AmmPoolCache testInstance) {
    instance = testInstance;
  }

  /**
   * Reset instance - chỉ sử dụng cho mục đích testing.
   */
  public static void resetInstance() {
    instance = null;
  }

  /**
   * Lấy AmmPool từ cache, trả về Optional.empty() nếu không tồn tại.
   *
   * @param pair Cặp giao dịch (key)
   * @return AmmPool hoặc Optional.empty() nếu không tồn tại
   */
  public Optional<AmmPool> getAmmPool(String pair) {
    AmmPool pool = ammPoolCache.get(pair);
    return Optional.ofNullable(pool);
  }

  /**
   * Lấy AmmPool từ cache, tạo mới nếu không tồn tại (không lưu vào cache).
   *
   * @param pair Cặp giao dịch (key)
   * @return AmmPool
   */
  public AmmPool getOrInitAmmPool(String pair) {
    return getAmmPool(pair).orElseGet(() -> new AmmPool(pair));
  }

  /**
   * Lấy AmmPool từ cache, tạo mới và lưu vào cache nếu không tồn tại.
   *
   * @param pair Cặp giao dịch (key)
   * @return AmmPool
   */
  public AmmPool getOrCreateAmmPool(String pair) {
    AmmPool ammPool = getOrInitAmmPool(pair);
    ammPoolCache.put(pair, ammPool);
    return ammPool;
  }

  /**
   * Cập nhật AmmPool trong cache
   *
   * @param ammPool AmmPool mới
   */
  public void updateAmmPool(AmmPool ammPool) {
    updateCounter.incrementAndGet();
    ammPoolCache.put(ammPool.getPair(), ammPool);
  }

  public boolean ammPoolCacheShouldFlush() {
    // Trả về true khi số lần cập nhật chia hết cho 100
    int count = updateCounter.get();
    return count > 0 && count % UPDATE_THRESHOLD == 0;
  }

  /**
   * Khởi tạo cache từ dữ liệu trong database.
   * Method này sẽ được triển khai đầy đủ khi có RocksDBService hỗ trợ AmmPool.
   */
  public void initializeAmmPoolCache() {
    try {
      List<AmmPool> dbAmmPools = ammPoolRocksDB.getAllAmmPools();
      int loadedCount = 0;

      for (AmmPool dbAmmPool : dbAmmPools) {
        String pair = dbAmmPool.getPair();
        if (pair != null && !pair.isEmpty()) {
          ammPoolCache.put(pair, dbAmmPool);
          loadedCount++;
        }
      }

      logger.info("AmmPool cache đã được khởi tạo: {} bản ghi đã tải", loadedCount);
    } catch (Exception e) {
      logger.error("Không thể khởi tạo ammPool cache: {}", e.getMessage(), e);
    }
  }

  /**
   * Thêm AmmPool vào batch để lưu vào database.
   *
   * @param ammPool AmmPool cần lưu
   */
  public void addAmmPoolToBatch(AmmPool ammPool) {
    if (ammPool.getPair() == null) {
      return;
    }
    String pair = ammPool.getPair();
    latestAmmPools.compute(pair, (key, existingPool) -> {
      if (existingPool == null || ammPool.getUpdatedAt() > existingPool.getUpdatedAt()) {
        return ammPool;
      }
      return existingPool;
    });
  }

  public void flushAmmPoolToDisk() {
    if (latestAmmPools.isEmpty()) {
      return;
    }
    ammPoolRocksDB.saveAmmPoolBatch(latestAmmPools);
    latestAmmPools.clear();
    logger.debug("Đã lưu ammPool thành công");
  }
}
