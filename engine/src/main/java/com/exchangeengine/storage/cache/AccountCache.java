package com.exchangeengine.storage.cache;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.Account;
import com.exchangeengine.storage.rocksdb.AccountRocksDB;

/**
 * Cache service cho Account
 * Sử dụng Singleton pattern để đảm bảo chỉ có một instance duy nhất
 */
public class AccountCache {
  private static final Logger logger = LoggerFactory.getLogger(AccountCache.class);

  private static volatile AccountCache instance;
  private final AccountRocksDB accountRocksDB = AccountRocksDB.getInstance();

  private final ConcurrentHashMap<String, Account> accountCache = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Account> latestAccounts = new ConcurrentHashMap<>();
  private final ReadWriteLock flushLock = new ReentrantReadWriteLock();

  private static final int BACKUP_BATCH_SIZE = 10000;

  /**
   * Lấy instance của AccountCache.
   *
   * @return Instance của AccountCache
   */
  public static AccountCache getInstance() {
    if (instance == null) {
      synchronized (AccountCache.class) {
        if (instance == null) {
          instance = new AccountCache();
        }
      }
    }
    return instance;
  }

  /**
   * Constructor riêng tư để đảm bảo Singleton pattern
   */
  private AccountCache() {
  }

  /**
   * Thiết lập instance cho mục đích testing.
   * CHỈ SỬ DỤNG TRONG UNIT TEST.
   *
   * @param testInstance Instance để sử dụng cho testing
   */
  public static void setTestInstance(AccountCache testInstance) {
    synchronized (AccountCache.class) {
      instance = testInstance;
    }
  }

  /**
   * Reset instance - chỉ sử dụng cho mục đích testing.
   */
  public static void resetInstance() {
    synchronized (AccountCache.class) {
      instance = null;
    }
  }

  /**
   * Lấy Account từ cache, trả về Optional.empty() nếu không tồn tại.
   *
   * @param accountKey Key của account
   * @return Account hoặc null nếu không tồn tại
   */
  public Optional<Account> getAccount(String accountKey) {
    Account account = accountCache.get(accountKey);
    return Optional.ofNullable(account);
  }

  /**
   * Lấy Account từ cache, tạo mới nếu không tồn tại.
   *
   * @param accountKey Key của account
   * @return Account
   */
  public Account getOrInitAccount(String accountKey) {
    return getAccount(accountKey).orElseGet(() -> new Account(accountKey));
  }

  /**
   * Lấy Account từ cache, tạo mới nếu không tồn tại.
   *
   * @param accountKey Key của account
   * @return Account
   */
  public Account getOrCreateAccount(String accountKey) {
    return accountCache.computeIfAbsent(accountKey, key -> new Account(key));
  }

  /**
   * Cập nhật Account trong cache
   *
   * @param account Account mới
   */
  public void updateAccount(Account account) {
    if (account == null || account.getKey() == null) {
      logger.warn("Cannot update null account or account with null key");
      return;
    }
    accountCache.put(account.getKey(), account);
    addAccountToBatch(account);
  }

  /**
   * Reset account về trạng thái ban đầu
   */
  public void resetAccount(String accountKey) {
    if (accountKey == null || accountKey.isEmpty()) {
      logger.warn("Cannot reset account with null or empty key");
      return;
    }

    Optional<Account> currentAccount = getAccount(accountKey);

    if (!currentAccount.isPresent()) {
      logger.info("Account not found: accountKey={}", accountKey);
      return;
    }

    Account newAccount = new Account(accountKey);
    accountCache.put(newAccount.getKey(), newAccount);
    
    // Also update in batch to persist to disk
    addAccountToBatch(newAccount);
  }

  /**
   * Khởi tạo cache cho Account từ RocksDB
   */
  public void initializeAccountCache() {
    try {
      List<Account> dbAccounts = accountRocksDB.getAllAccounts();
      int loadedCount = 0;
      int skippedCount = 0;

      for (Account dbAccount : dbAccounts) {
        if (dbAccount == null || dbAccount.getKey() == null) {
          logger.warn("Skipping null account or account with null key during initialization");
          continue;
        }
        
        String accountKey = dbAccount.getKey();
        Account cacheAccount = accountCache.get(accountKey);

        if (cacheAccount == null || dbAccount.getUpdatedAt() > cacheAccount.getUpdatedAt()) {
          accountCache.put(accountKey, dbAccount);
          loadedCount++;
        } else {
          skippedCount++;
        }
      }

      logger.info("Account cache đã được khởi tạo: {} bản ghi đã tải, {} bản ghi bỏ qua (mới hơn trong cache)",
          loadedCount, skippedCount);
    } catch (Exception e) {
      logger.error("Không thể khởi tạo account cache: {}", e.getMessage(), e);
    }
  }

  /**
   * Kiểm tra xem có cần lưu dữ liệu vào RocksDB không.
   *
   * @return true nếu cần lưu
   */
  public boolean accountCacheShouldFlush() {
    return latestAccounts.size() >= BACKUP_BATCH_SIZE;
  }

  /**
   * Thêm Account vào batch để lưu vào database.
   *
   * @param account Account cần lưu
   */
  public void addAccountToBatch(Account account) {
    if (account == null || account.getKey() == null) {
      return;
    }
    
    String accountKey = account.getKey();
    latestAccounts.compute(accountKey, (key, existingAccount) -> {
      if (existingAccount == null || account.getUpdatedAt() > existingAccount.getUpdatedAt()) {
        return account;
      }
      return existingAccount;
    });
  }

  /**
   * Lưu Account vào RocksDB.
   */
  public void flushAccountToDisk() {
    if (latestAccounts.isEmpty()) {
      return;
    }

    flushLock.writeLock().lock();
    try {
      Map<String, Account> batchToSave = new ConcurrentHashMap<>(latestAccounts);
      latestAccounts.clear();
      
      if (!batchToSave.isEmpty()) {
        accountRocksDB.saveAccountBatch(batchToSave);
        logger.debug("Đã lưu {} accounts thành công", batchToSave.size());
      }
    } catch (Exception e) {
      logger.error("Error flushing accounts to disk: {}", e.getMessage(), e);
    } finally {
      flushLock.writeLock().unlock();
    }
  }
}
