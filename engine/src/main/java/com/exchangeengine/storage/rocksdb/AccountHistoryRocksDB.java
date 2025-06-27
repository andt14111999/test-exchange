package com.exchangeengine.storage.rocksdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.AccountHistory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lớp xử lý các thao tác với AccountHistory trong RocksDB
 */
public class AccountHistoryRocksDB {
  // Singleton instance
  private static volatile AccountHistoryRocksDB instance;

  private static final Logger logger = LoggerFactory.getLogger(AccountHistoryRocksDB.class);
  private final RocksDBService rocksDBService;

  /**
   * Lấy instance của AccountHistoryRocksDB
   *
   * @return instance của AccountHistoryRocksDB
   */
  public static synchronized AccountHistoryRocksDB getInstance() {
    if (instance == null) {
      instance = new AccountHistoryRocksDB();
    }
    return instance;
  }

  /**
   * Reset instance của AccountHistoryRocksDB (chỉ dùng cho test)
   */
  public static void resetInstance() {
    instance = null;
  }

  /**
   * Constructor riêng tư để đảm bảo Singleton pattern
   */
  private AccountHistoryRocksDB() {
    this.rocksDBService = RocksDBService.getInstance();
  }

  /**
   * Lưu AccountHistory vào RocksDB
   *
   * @param history AccountHistory cần lưu
   */
  public void saveAccountHistory(AccountHistory history) {
    rocksDBService.saveObject(history, rocksDBService.getAccountHistoryCF(),
        AccountHistory::getKey, "account_history");
  }

  /**
   * Lấy AccountHistory từ RocksDB theo key
   *
   * @param key Key của history
   * @return Optional chứa AccountHistory nếu tồn tại
   */
  public Optional<AccountHistory> getAccountHistory(String key) {
    return rocksDBService.getObject(key, rocksDBService.getAccountHistoryCF(),
        AccountHistory.class, "account_history");
  }

  /**
   * Lưu nhiều AccountHistory vào RocksDB
   *
   * @param histories Map chứa các AccountHistory cần lưu
   */
  public void saveAccountHistoryBatch(Map<String, AccountHistory> histories) {
    rocksDBService.saveBatch(histories, rocksDBService.getAccountHistoryCF(),
        AccountHistory::getKey, "account_histories");
  }

  /**
   * Lấy danh sách AccountHistory bằng prefix seek và lastKey
   */
  public List<AccountHistory> getAccountHistoriesByPrefix(String prefix, int limit, String lastKey) {
    return rocksDBService.getObjectsByPrefix(prefix, limit, lastKey,
        rocksDBService.getAccountHistoryCF(),
        AccountHistory.class, "account_history");
  }

  /**
   * Lấy lịch sử giao dịch theo accountKey
   */
  public List<AccountHistory> getAccountHistoriesByAccountKey(String accountKey, int limit, String lastKey) {
    String prefix = AccountHistory.generateAccountPrefix(accountKey);
    return rocksDBService.getObjectsByPrefix(prefix, limit, lastKey, rocksDBService.getAccountHistoryCF(),
        AccountHistory.class, "account_history");
  }

  /**
   * Lấy tất cả AccountHistory từ RocksDB
   *
   * @return Danh sách các AccountHistory
   */
  public List<AccountHistory> getAllAccountHistories() {
    return rocksDBService.getAllObjects(rocksDBService.getAccountHistoryCF(),
        AccountHistory.class, "account_histories");
  }
}
