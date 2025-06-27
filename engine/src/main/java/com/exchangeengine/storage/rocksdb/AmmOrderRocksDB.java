package com.exchangeengine.storage.rocksdb;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.exchangeengine.model.AmmOrder;

/**
 * Lớp quản lý lưu trữ AmmOrder trong RocksDB
 */
public class AmmOrderRocksDB {
  // Singleton instance
  private static volatile AmmOrderRocksDB instance;

  private final RocksDBService rocksDBService;

  /**
   * Lấy instance của AmmOrderRocksDB
   *
   * @return instance của AmmOrderRocksDB
   */
  public static synchronized AmmOrderRocksDB getInstance() {
    if (instance == null) {
      instance = new AmmOrderRocksDB();
    }
    return instance;
  }

  /**
   * Reset instance của AmmOrderRocksDB (chỉ dùng cho test)
   */
  public static void resetInstance() {
    instance = null;
  }

  /**
   * Constructor riêng tư để đảm bảo Singleton pattern
   */
  private AmmOrderRocksDB() {
    this.rocksDBService = RocksDBService.getInstance();
  }

  /**
   * Lưu AmmOrder vào RocksDB
   *
   * @param ammOrder AmmOrder cần lưu
   */
  public void saveAmmOrder(AmmOrder ammOrder) {
    rocksDBService.saveObject(ammOrder, rocksDBService.getAmmOrdersCF(), AmmOrder::getIdentifier,
        "amm_order");
  }

  /**
   * Lấy AmmOrder từ RocksDB theo identifier
   *
   * @param identifier Identifier của order
   * @return Optional chứa AmmOrder nếu tồn tại
   */
  public Optional<AmmOrder> getOrder(String identifier) {
    return rocksDBService.getObject(identifier, rocksDBService.getAmmOrdersCF(), AmmOrder.class, "amm_order");
  }

  /**
   * Lấy tất cả AmmOrder từ RocksDB
   *
   * @return Danh sách các AmmOrder
   */
  public List<AmmOrder> getAllOrders() {
    return rocksDBService.getAllObjects(rocksDBService.getAmmOrdersCF(), AmmOrder.class, "amm_orders");
  }

  /**
   * Lưu nhiều AmmOrder vào RocksDB
   *
   * @param ammOrders Map chứa các AmmOrder cần lưu
   */
  public void saveAmmOrderBatch(Map<String, AmmOrder> ammOrders) {
    rocksDBService.saveBatch(ammOrders, rocksDBService.getAmmOrdersCF(), AmmOrder::getIdentifier,
        "amm_orders");
  }
}
