package com.exchangeengine.storage.rocksdb;

import com.exchangeengine.model.AmmPosition;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lớp xử lý các thao tác với AmmPosition trong RocksDB
 */
public class AmmPositionRocksDB {
  // Singleton instance
  private static volatile AmmPositionRocksDB instance;

  private final RocksDBService rocksDBService;

  /**
   * Lấy instance của AmmPositionRocksDB
   *
   * @return instance của AmmPositionRocksDB
   */
  public static synchronized AmmPositionRocksDB getInstance() {
    if (instance == null) {
      instance = new AmmPositionRocksDB();
    }
    return instance;
  }

  /**
   * Reset instance của AmmPositionRocksDB (chỉ dùng cho test)
   */
  public static void resetInstance() {
    instance = null;
  }

  /**
   * Constructor riêng tư để đảm bảo Singleton pattern
   */
  private AmmPositionRocksDB() {
    this.rocksDBService = RocksDBService.getInstance();
  }

  /**
   * Lưu AmmPosition vào RocksDB
   *
   * @param ammPosition AmmPosition cần lưu
   */
  public void saveAmmPosition(AmmPosition ammPosition) {
    rocksDBService.saveObject(ammPosition, rocksDBService.getAmmPositionCF(), AmmPosition::getIdentifier,
        "amm_position");
  }

  /**
   * Lấy AmmPosition từ RocksDB theo identifier
   *
   * @param identifier Identifier của position
   * @return Optional chứa AmmPosition nếu tồn tại
   */
  public Optional<AmmPosition> getAmmPosition(String identifier) {
    return rocksDBService.getObject(identifier, rocksDBService.getAmmPositionCF(), AmmPosition.class, "amm_position");
  }

  /**
   * Lấy tất cả AmmPosition từ RocksDB
   *
   * @return Danh sách các AmmPosition
   */
  public List<AmmPosition> getAllAmmPositions() {
    return rocksDBService.getAllObjects(rocksDBService.getAmmPositionCF(), AmmPosition.class, "amm_positions");
  }

  /**
   * Lấy danh sách các AmmPosition theo pool
   *
   * @param pool           Pool cần tìm kiếm position
   * @param limit          Số lượng tối đa bản ghi cần lấy
   * @param lastIdentifier Identifier cuối cùng của trang trước (cho phân trang)
   * @return Danh sách các AmmPosition thuộc về pool
   */
  public List<AmmPosition> getAmmPositionsByPool(String pool, int limit, String lastIdentifier) {
    return rocksDBService.getObjectsByPrefix(pool + ":", limit, lastIdentifier,
        rocksDBService.getAmmPositionCF(), AmmPosition.class, "amm_positions_by_pool");
  }

  /**
   * Lưu nhiều AmmPosition vào RocksDB
   *
   * @param ammPositions Map chứa các AmmPosition cần lưu
   */
  public void saveAmmPositionBatch(Map<String, AmmPosition> ammPositions) {
    rocksDBService.saveBatch(ammPositions, rocksDBService.getAmmPositionCF(), AmmPosition::getIdentifier,
        "amm_positions");
  }
}
