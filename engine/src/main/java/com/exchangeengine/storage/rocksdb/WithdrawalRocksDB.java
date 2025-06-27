package com.exchangeengine.storage.rocksdb;

import com.exchangeengine.model.CoinWithdrawal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lớp xử lý các thao tác với CoinWithdrawal trong RocksDB
 */
public class WithdrawalRocksDB {
  // Singleton instance
  private static volatile WithdrawalRocksDB instance;

  private final RocksDBService rocksDBService;

  /**
   * Lấy instance của WithdrawalRocksDB
   *
   * @return instance của WithdrawalRocksDB
   */
  public static synchronized WithdrawalRocksDB getInstance() {
    if (instance == null) {
      instance = new WithdrawalRocksDB();
    }
    return instance;
  }

  /**
   * Reset instance của WithdrawalRocksDB (chỉ dùng cho test)
   */
  public static void resetInstance() {
    instance = null;
  }

  /**
   * Constructor riêng tư để đảm bảo Singleton pattern
   */
  private WithdrawalRocksDB() {
    this.rocksDBService = RocksDBService.getInstance();
  }

  /**
   * Lưu CoinWithdrawal vào RocksDB
   *
   * @param withdrawal CoinWithdrawal cần lưu
   */
  public void saveWithdrawal(CoinWithdrawal withdrawal) {
    rocksDBService.saveObject(withdrawal, rocksDBService.getWithdrawalCF(),
        CoinWithdrawal::getIdentifier, "withdrawal");
  }

  /**
   * Lấy CoinWithdrawal từ RocksDB theo identifier
   *
   * @param identifier Identifier của withdrawal
   * @return Optional chứa CoinWithdrawal nếu tồn tại
   */
  public Optional<CoinWithdrawal> getWithdrawal(String identifier) {
    return rocksDBService.getObject(identifier, rocksDBService.getWithdrawalCF(),
        CoinWithdrawal.class, "withdrawal");
  }

  /**
   * Lấy tất cả CoinWithdrawal từ RocksDB
   *
   * @return Danh sách các CoinWithdrawal
   */
  public List<CoinWithdrawal> getAllWithdrawals() {
    return rocksDBService.getAllObjects(rocksDBService.getWithdrawalCF(),
        CoinWithdrawal.class, "withdrawals");
  }

  /**
   * Lưu nhiều CoinWithdrawal vào RocksDB
   *
   * @param withdrawals Map chứa các CoinWithdrawal cần lưu
   */
  public void saveWithdrawalBatch(Map<String, CoinWithdrawal> withdrawals) {
    rocksDBService.saveBatch(withdrawals, rocksDBService.getWithdrawalCF(),
        CoinWithdrawal::getIdentifier, "withdrawals");
  }
}
