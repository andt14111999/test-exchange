package com.exchangeengine.storage.cache;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.AccountHistory;
import com.exchangeengine.storage.rocksdb.AccountHistoryRocksDB;

/**
 * Cache service cho AccountHistory
 * Sử dụng Singleton pattern để đảm bảo chỉ có một instance duy nhất
 */
public class AccountHistoryCache {
  private static final Logger logger = LoggerFactory.getLogger(AccountHistoryCache.class);

  private static volatile AccountHistoryCache instance;
  private final AccountHistoryRocksDB accountHistoryRocksDB = AccountHistoryRocksDB.getInstance();

  private final Map<String, AccountHistory> latestHistories = new ConcurrentHashMap<>();

  private static final int BACKUP_BATCH_SIZE = 10000;

  /**
   * Lấy instance của AccountHistoryCache.
   *
   * @return Instance của AccountHistoryCache
   */
  public static synchronized AccountHistoryCache getInstance() {
    if (instance == null) {
      instance = new AccountHistoryCache();
    }
    return instance;
  }

  /**
   * Thiết lập instance kiểm thử (chỉ sử dụng cho testing)
   *
   * @param testInstance Instance kiểm thử cần thiết lập
   */
  public static void setTestInstance(AccountHistoryCache testInstance) {
    instance = testInstance;
  }

  /**
   * Constructor riêng tư để đảm bảo Singleton pattern
   */
  private AccountHistoryCache() {
  }

  /**
   * Lấy AccountHistory từ cache
   */
  public Optional<AccountHistory> getAccountHistory(String key) {
    return accountHistoryRocksDB.getAccountHistory(key);
  }

  /**
   * Cập nhật AccountHistory trong cache
   */
  public void updateAccountHistory(AccountHistory history) {
    accountHistoryRocksDB.saveAccountHistory(history);
  }

  /**
   * Lấy lịch sử giao dịch của account
   */
  public List<AccountHistory> getAccountTransactionHistory(String accountKey, String lastKey, int limit) {
    return accountHistoryRocksDB.getAccountHistoriesByAccountKey(accountKey, limit, lastKey);
  }

  /**
   * Thêm AccountHistory vào batch để lưu vào RocksDB
   */
  public void addHistoryToBatch(AccountHistory history) {
    latestHistories.put(history.getKey(), history);
  }

  /**
   * Kiểm tra xem có cần lưu dữ liệu vào RocksDB không.
   *
   * @return true nếu cần lưu
   */
  public boolean historiesCacheShouldFlush() {
    return latestHistories.size() >= BACKUP_BATCH_SIZE;
  }

  public void flushHistoryToDisk() {
    if (latestHistories.isEmpty()) {
      return;
    }
    accountHistoryRocksDB.saveAccountHistoryBatch(latestHistories);
    latestHistories.clear();
    logger.debug("Đã lưu account histories thành công");
  }
}
