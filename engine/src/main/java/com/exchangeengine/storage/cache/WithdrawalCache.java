package com.exchangeengine.storage.cache;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.CoinWithdrawal;
import com.exchangeengine.storage.rocksdb.WithdrawalRocksDB;

/**
 * Cache service cho Withdrawal
 * Sử dụng Singleton pattern để đảm bảo chỉ có một instance duy nhất
 */
public class WithdrawalCache {
  private static final Logger logger = LoggerFactory.getLogger(WithdrawalCache.class);

  private static volatile WithdrawalCache instance;
  private final WithdrawalRocksDB withdrawalRocksDB = WithdrawalRocksDB.getInstance();

  private final ConcurrentHashMap<String, CoinWithdrawal> withdrawalCache = new ConcurrentHashMap<>();
  private final Map<String, CoinWithdrawal> latestWithdrawals = new ConcurrentHashMap<>();

  private static final int BACKUP_BATCH_SIZE = 10000;

  /**
   * Lấy instance của WithdrawalCache.
   *
   * @return Instance của WithdrawalCache
   */
  public static synchronized WithdrawalCache getInstance() {
    if (instance == null) {
      instance = new WithdrawalCache();
    }
    return instance;
  }

  /**
   * Thiết lập instance kiểm thử (chỉ sử dụng cho testing)
   *
   * @param testInstance Instance kiểm thử cần thiết lập
   */
  public static void setTestInstance(WithdrawalCache testInstance) {
    instance = testInstance;
  }

  /**
   * Constructor riêng tư để đảm bảo Singleton pattern
   */
  private WithdrawalCache() {
  }

  /**
   * Lấy CoinWithdrawal từ cache theo identifier
   *
   * @param identifier Identifier của withdrawal
   * @return CoinWithdrawal hoặc Optional.empty() nếu không tồn tại
   */
  public Optional<CoinWithdrawal> getWithdrawal(String identifier) {
    CoinWithdrawal cachedWithdrawal = withdrawalCache.get(identifier);
    return Optional.ofNullable(cachedWithdrawal);
  }

  /**
   * Cập nhật CoinWithdrawal trong cache
   *
   * @param withdrawal CoinWithdrawal mới
   */
  public void updateCoinWithdrawal(CoinWithdrawal withdrawal) {
    withdrawalCache.put(withdrawal.getIdentifier(), withdrawal);
  }

  /**
   * Kiểm tra xem có cần lưu dữ liệu vào RocksDB không.
   *
   * @return true nếu cần lưu
   */
  public boolean withdrawalCacheShouldFlush() {
    return latestWithdrawals.size() >= BACKUP_BATCH_SIZE;
  }

  /**
   * Khởi tạo cache cho CoinWithdrawal từ RocksDB
   */
  public void initializeWithdrawalCache() {
    try {
      List<CoinWithdrawal> dbWithdrawals = withdrawalRocksDB.getAllWithdrawals();
      int loadedCount = 0;

      for (CoinWithdrawal dbWithdrawal : dbWithdrawals) {
        String identifier = dbWithdrawal.getIdentifier();
        if (identifier != null && !identifier.isEmpty()) {
          withdrawalCache.put(identifier, dbWithdrawal);
          loadedCount++;
        }
      }

      logger.info("CoinWithdrawal cache đã được khởi tạo: {} bản ghi đã tải", loadedCount);
    } catch (Exception e) {
      logger.error("Không thể khởi tạo withdrawal cache: {}", e.getMessage(), e);
    }
  }

  /**
   * Thêm CoinWithdrawal vào batch để lưu vào RocksDB.
   *
   * @param withdrawal CoinWithdrawal cần lưu
   */
  public void addWithdrawalToBatch(CoinWithdrawal withdrawal) {
    String identifier = withdrawal.getIdentifier();
    if (identifier == null || identifier.isEmpty()) {
      return;
    }

    latestWithdrawals.compute(identifier, (key, existingWithdrawal) -> {
      if (existingWithdrawal == null || withdrawal.getUpdatedAt() > existingWithdrawal.getUpdatedAt()) {
        return withdrawal;
      }
      return existingWithdrawal;
    });
  }

  /**
   * Lưu CoinWithdrawal vào RocksDB.
   */
  public void flushWithdrawalToDisk() {
    if (latestWithdrawals.isEmpty()) {
      return;
    }

    withdrawalRocksDB.saveWithdrawalBatch(latestWithdrawals);
    latestWithdrawals.clear();
    logger.debug("Đã lưu withdrawal thành công");
  }
}
