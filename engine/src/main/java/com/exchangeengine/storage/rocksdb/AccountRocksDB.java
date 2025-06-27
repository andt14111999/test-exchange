package com.exchangeengine.storage.rocksdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.Account;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lớp xử lý các thao tác với Account trong RocksDB
 */
public class AccountRocksDB {
  // Singleton instance
  private static volatile AccountRocksDB instance;

  private static final Logger logger = LoggerFactory.getLogger(AccountRocksDB.class);
  private final RocksDBService rocksDBService;

  /**
   * Lấy instance của AccountRocksDB
   *
   * @return instance của AccountRocksDB
   */
  public static synchronized AccountRocksDB getInstance() {
    if (instance == null) {
      instance = new AccountRocksDB();
    }
    return instance;
  }

  /**
   * Reset instance của AccountRocksDB (chỉ dùng cho test)
   */
  public static void resetInstance() {
    instance = null;
  }

  /**
   * Constructor riêng tư để đảm bảo Singleton pattern
   */
  private AccountRocksDB() {
    this.rocksDBService = RocksDBService.getInstance();
  }

  /**
   * Lưu Account vào RocksDB
   *
   * @param account Account cần lưu
   */
  public void saveAccount(Account account) {
    rocksDBService.saveObject(account, rocksDBService.getAccountCF(), Account::getKey, "accounts");
  }

  /**
   * Lấy Account từ RocksDB theo key
   *
   * @param accountKey Key của account
   * @return Optional chứa Account nếu tồn tại
   */
  public Optional<Account> getAccount(String accountKey) {
    return rocksDBService.getObject(accountKey, rocksDBService.getAccountCF(), Account.class, "accounts");
  }

  /**
   * Lấy tất cả Account từ RocksDB
   *
   * @return Danh sách các Account
   */
  public List<Account> getAllAccounts() {
    return rocksDBService.getAllObjects(rocksDBService.getAccountCF(), Account.class, "accounts");
  }

  /**
   * Lưu nhiều Account vào RocksDB
   *
   * @param accounts Map chứa các Account cần lưu
   */
  public void saveAccountBatch(Map<String, Account> accounts) {
    rocksDBService.saveBatch(accounts, rocksDBService.getAccountCF(), Account::getKey, "accounts");
  }
}
