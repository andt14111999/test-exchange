package com.exchangeengine.storage.cache;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.AmmOrder;
import com.exchangeengine.storage.rocksdb.AmmOrderRocksDB;

/**
 * Cache service cho AmmOrder
 * Sử dụng Singleton pattern để đảm bảo chỉ có một instance duy nhất
 */
public class AmmOrderCache {
  private static final Logger logger = LoggerFactory.getLogger(AmmOrderCache.class);

  private static volatile AmmOrderCache instance;
  private final AmmOrderRocksDB ammOrderRocksDB = AmmOrderRocksDB.getInstance();

  private final ConcurrentHashMap<String, Boolean> ammOrderCache = new ConcurrentHashMap<>();

  /**
   * Lấy instance của AmmOrderCache.
   *
   * @return Instance của AmmOrderCache
   */
  public static synchronized AmmOrderCache getInstance() {
    if (instance == null) {
      instance = new AmmOrderCache();
    }
    return instance;
  }

  /**
   * Constructor riêng tư để đảm bảo Singleton pattern
   */
  private AmmOrderCache() {
  }

  /**
   * Thiết lập instance cho mục đích testing.
   * CHỈ SỬ DỤNG TRONG UNIT TEST.
   *
   * @param testInstance Instance để sử dụng cho testing
   */
  public static void setTestInstance(AmmOrderCache testInstance) {
    instance = testInstance;
  }

  /**
   * Reset instance - chỉ sử dụng cho mục đích testing.
   */
  public static void resetInstance() {
    instance = null;
  }

  /**
   * Lấy AmmOrder từ cache, trả về Optional.empty() nếu không tồn tại.
   *
   * @param identifier Order identifier
   * @return AmmOrder hoặc Optional.empty() nếu không tồn tại
   */
  public boolean ammOrderExists(String identifier) {
    return ammOrderCache.get(identifier) != null;
  }

  /**
   * Cập nhật AmmOrder trong cache và lưu vào RocksDB
   *
   * @param identifier Order identifier
   */
  public void updateAmmOrder(String identifier) {
    ammOrderCache.put(identifier, true);
  }

  /**
   * Khởi tạo cache từ dữ liệu trong database.
   */
  public void initializeAmmOrderCache() {
    try {
      List<AmmOrder> dbAmmOrders = ammOrderRocksDB.getAllOrders();
      int loadedCount = 0;

      for (AmmOrder dbAmmOrder : dbAmmOrders) {
        String identifier = dbAmmOrder.getIdentifier();
        if (identifier != null && !identifier.isEmpty()) {
          updateAmmOrder(identifier);
          loadedCount++;
        }
      }

      logger.info("AmmOrder cache đã được khởi tạo: {} bản ghi đã tải", loadedCount);
    } catch (Exception e) {
      logger.error("Không thể khởi tạo AmmOrder cache: {}", e.getMessage(), e);
    }
  }

  /**
   * Thêm AmmOrder vào RocksDB trực tiếp.
   *
   * @param ammOrder AmmOrder cần lưu
   */
  public void addAmmOrderToBatch(AmmOrder ammOrder) {
    if (ammOrder.getIdentifier() == null) {
      return;
    }
    ammOrderRocksDB.saveAmmOrder(ammOrder);
  }
}
