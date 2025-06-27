package com.exchangeengine.storage.cache;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.CoinDeposit;
import com.exchangeengine.storage.rocksdb.DepositRocksDB;

/**
 * Cache service cho Deposit
 * Sử dụng Singleton pattern để đảm bảo chỉ có một instance duy nhất
 */
public class DepositCache {
  private static final Logger logger = LoggerFactory.getLogger(DepositCache.class);

  private static volatile DepositCache instance;
  private final DepositRocksDB depositRocksDB = DepositRocksDB.getInstance();

  private final ConcurrentHashMap<String, CoinDeposit> depositCache = new ConcurrentHashMap<>();
  private final Map<String, CoinDeposit> latestDeposits = new ConcurrentHashMap<>();

  private static final int BACKUP_BATCH_SIZE = 10000;

  /**
   * Lấy instance của DepositCache.
   *
   * @return Instance của DepositCache
   */
  public static synchronized DepositCache getInstance() {
    if (instance == null) {
      instance = new DepositCache();
    }
    return instance;
  }

  /**
   * Thiết lập instance kiểm thử (chỉ sử dụng cho testing)
   *
   * @param testInstance Instance kiểm thử cần thiết lập
   */
  public static void setTestInstance(DepositCache testInstance) {
    instance = testInstance;
  }

  /**
   * Constructor riêng tư để đảm bảo Singleton pattern
   */
  private DepositCache() {
  }

  /**
   * Lấy CoinDeposit từ cache theo identifier
   *
   * @param identifier Identifier của deposit
   * @return CoinDeposit hoặc Optional.empty() nếu không tồn tại
   */
  public Optional<CoinDeposit> getDeposit(String identifier) {
    CoinDeposit cachedDeposit = depositCache.get(identifier);
    return Optional.ofNullable(cachedDeposit);
  }

  /**
   * Cập nhật CoinDeposit trong cache
   *
   * @param deposit CoinDeposit mới
   */
  public void updateCoinDeposit(CoinDeposit deposit) {
    depositCache.put(deposit.getIdentifier(), deposit);
  }

  /**
   * Kiểm tra xem có cần lưu dữ liệu vào RocksDB không.
   *
   * @return true nếu cần lưu
   */
  public boolean depositCacheShouldFlush() {
    return latestDeposits.size() >= BACKUP_BATCH_SIZE;
  }

  /**
   * Khởi tạo cache cho CoinDeposit từ RocksDB
   */
  public void initializeDepositCache() {
    try {
      List<CoinDeposit> dbDeposits = depositRocksDB.getAllDeposits();
      int loadedCount = 0;

      for (CoinDeposit dbDeposit : dbDeposits) {
        String identifier = dbDeposit.getIdentifier();
        if (identifier != null && !identifier.isEmpty()) {
          depositCache.put(identifier, dbDeposit);
          loadedCount++;
        }
      }

      logger.info("CoinDeposit cache đã được khởi tạo: {} bản ghi đã tải", loadedCount);
    } catch (Exception e) {
      logger.error("Không thể khởi tạo deposit cache: {}", e.getMessage(), e);
    }
  }

  /**
   * Thêm CoinDeposit vào batch để lưu vào RocksDB.
   *
   * @param deposit CoinDeposit cần lưu
   */
  public void addDepositToBatch(CoinDeposit deposit) {
    String identifier = deposit.getIdentifier();
    if (identifier == null || identifier.isEmpty()) {
      return;
    }
    latestDeposits.compute(identifier, (key, existingDeposit) -> {
      if (existingDeposit == null || deposit.getUpdatedAt() > existingDeposit.getUpdatedAt()) {
        return deposit;
      }
      return existingDeposit;
    });
  }

  /**
   * Lưu CoinDeposit vào RocksDB.
   */
  public void flushDepositToDisk() {
    if (latestDeposits.isEmpty()) {
      return;
    }

    depositRocksDB.saveDepositBatch(latestDeposits);
    latestDeposits.clear();
    logger.debug("Đã lưu deposit thành công");
  }
}
