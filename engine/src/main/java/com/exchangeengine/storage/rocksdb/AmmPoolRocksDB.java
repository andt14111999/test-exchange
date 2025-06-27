package com.exchangeengine.storage.rocksdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.AmmPool;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lớp xử lý các thao tác với AmmPool trong RocksDB
 */
public class AmmPoolRocksDB {
  // Singleton instance
  private static volatile AmmPoolRocksDB instance;

  private static final Logger logger = LoggerFactory.getLogger(AmmPoolRocksDB.class);
  private final RocksDBService rocksDBService;

  /**
   * Lấy instance của AmmPoolRocksDB
   *
   * @return instance của AmmPoolRocksDB
   */
  public static synchronized AmmPoolRocksDB getInstance() {
    if (instance == null) {
      instance = new AmmPoolRocksDB();
    }
    return instance;
  }

  /**
   * Reset instance của AmmPoolRocksDB (chỉ dùng cho test)
   */
  public static void resetInstance() {
    instance = null;
  }

  /**
   * Constructor riêng tư để đảm bảo Singleton pattern
   */
  private AmmPoolRocksDB() {
    this.rocksDBService = RocksDBService.getInstance();
  }

  /**
   * Lưu AmmPool vào RocksDB
   *
   * @param ammPool AmmPool cần lưu
   */
  public void saveAmmPool(AmmPool ammPool) {
    rocksDBService.saveObject(ammPool, rocksDBService.getAmmPoolCF(), AmmPool::getPair, "amm_pool");
  }

  /**
   * Lấy AmmPool từ RocksDB theo pair
   *
   * @param pair Pair của pool
   * @return Optional chứa AmmPool nếu tồn tại
   */
  public Optional<AmmPool> getAmmPool(String pair) {
    return rocksDBService.getObject(pair, rocksDBService.getAmmPoolCF(), AmmPool.class, "amm_pool");
  }

  /**
   * Lấy tất cả AmmPool từ RocksDB
   *
   * @return Danh sách các AmmPool
   */
  public List<AmmPool> getAllAmmPools() {
    return rocksDBService.getAllObjects(rocksDBService.getAmmPoolCF(), AmmPool.class, "amm_pools");
  }

  /**
   * Lưu nhiều AmmPool vào RocksDB
   *
   * @param ammPools Map chứa các AmmPool cần lưu
   */
  public void saveAmmPoolBatch(Map<String, AmmPool> ammPools) {
    rocksDBService.saveBatch(ammPools, rocksDBService.getAmmPoolCF(), AmmPool::getPair, "amm_pools");
  }
}
