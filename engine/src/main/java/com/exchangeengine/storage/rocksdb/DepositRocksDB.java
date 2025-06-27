package com.exchangeengine.storage.rocksdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.CoinDeposit;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lớp xử lý các thao tác với CoinDeposit trong RocksDB
 */
public class DepositRocksDB {
  // Singleton instance
  private static volatile DepositRocksDB instance;

  private static final Logger logger = LoggerFactory.getLogger(DepositRocksDB.class);
  private final RocksDBService rocksDBService;

  /**
   * Lấy instance của DepositRocksDB
   *
   * @return instance của DepositRocksDB
   */
  public static synchronized DepositRocksDB getInstance() {
    if (instance == null) {
      instance = new DepositRocksDB();
    }
    return instance;
  }

  /**
   * Reset instance của DepositRocksDB (chỉ dùng cho test)
   */
  public static void resetInstance() {
    instance = null;
  }

  /**
   * Constructor riêng tư để đảm bảo Singleton pattern
   */
  private DepositRocksDB() {
    this.rocksDBService = RocksDBService.getInstance();
  }

  /**
   * Lưu CoinDeposit vào RocksDB
   *
   * @param deposit CoinDeposit cần lưu
   */
  public void saveDeposit(CoinDeposit deposit) {
    rocksDBService.saveObject(deposit, rocksDBService.getDepositCF(), CoinDeposit::getIdentifier, "deposit");
  }

  /**
   * Lấy CoinDeposit từ RocksDB theo identifier
   *
   * @param identifier Identifier của deposit
   * @return Optional chứa CoinDeposit nếu tồn tại
   */
  public Optional<CoinDeposit> getDeposit(String identifier) {
    return rocksDBService.getObject(identifier, rocksDBService.getDepositCF(), CoinDeposit.class, "deposit");
  }

  /**
   * Lấy tất cả CoinDeposit từ RocksDB
   *
   * @return Danh sách các CoinDeposit
   */
  public List<CoinDeposit> getAllDeposits() {
    return rocksDBService.getAllObjects(rocksDBService.getDepositCF(), CoinDeposit.class, "deposits");
  }

  /**
   * Lưu nhiều CoinDeposit vào RocksDB
   *
   * @param deposits Map chứa các CoinDeposit cần lưu
   */
  public void saveDepositBatch(Map<String, CoinDeposit> deposits) {
    rocksDBService.saveBatch(deposits, rocksDBService.getDepositCF(), CoinDeposit::getIdentifier, "deposits");
  }
}
