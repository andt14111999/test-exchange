package com.exchangeengine.storage.cache;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.AmmPosition;
import com.exchangeengine.storage.rocksdb.AmmPositionRocksDB;

/**
 * Cache service cho AmmPosition
 * Sử dụng Singleton pattern để đảm bảo chỉ có một instance duy nhất
 */
public class AmmPositionCache {
  private static final Logger logger = LoggerFactory.getLogger(AmmPositionCache.class);

  private static volatile AmmPositionCache instance;
  private final AmmPositionRocksDB ammPositionRocksDB = AmmPositionRocksDB.getInstance();

  private final ConcurrentHashMap<String, AmmPosition> ammPositionCache = new ConcurrentHashMap<>();
  private final Map<String, AmmPosition> latestAmmPositions = new ConcurrentHashMap<>();

  // Biến đếm số lần cập nhật (atomic để đảm bảo thread-safe)
  private final AtomicInteger updateCounter = new AtomicInteger(0);
  private static final int UPDATE_THRESHOLD = 100;

  /**
   * Lấy instance của AmmPositionCache.
   *
   * @return Instance của AmmPositionCache
   */
  public static synchronized AmmPositionCache getInstance() {
    if (instance == null) {
      instance = new AmmPositionCache();
    }
    return instance;
  }

  /**
   * Constructor riêng tư để đảm bảo Singleton pattern
   */
  private AmmPositionCache() {
  }

  /**
   * Thiết lập instance cho mục đích testing.
   * CHỈ SỬ DỤNG TRONG UNIT TEST.
   *
   * @param testInstance Instance để sử dụng cho testing
   */
  public static void setTestInstance(AmmPositionCache testInstance) {
    instance = testInstance;
  }

  /**
   * Lấy AmmPosition từ cache, trả về Optional.empty() nếu không tồn tại.
   *
   * @param identifier Position identifier
   * @return AmmPosition hoặc Optional.empty() nếu không tồn tại
   */
  public Optional<AmmPosition> getAmmPosition(String identifier) {
    AmmPosition position = ammPositionCache.get(identifier);
    return Optional.ofNullable(position);
  }

  /**
   * Lấy AmmPosition từ cache, tạo mới nếu không tồn tại (không lưu vào cache).
   *
   * @param identifier Position identifier
   * @param pool       Pool name
   * @return AmmPosition
   */
  public AmmPosition getOrInitAmmPosition(String identifier, String pool) {
    return getAmmPosition(identifier).orElseGet(() -> new AmmPosition(identifier, pool));
  }

  /**
   * Lấy AmmPosition từ cache, tạo mới và lưu vào cache nếu không tồn tại.
   *
   * @param identifier Position identifier
   * @param pool       Pool name
   * @return AmmPosition
   */
  public AmmPosition getOrCreateAmmPosition(String identifier, String pool) {
    AmmPosition ammPosition = getOrInitAmmPosition(identifier, pool);
    ammPositionCache.put(identifier, ammPosition);
    return ammPosition;
  }

  /**
   * Cập nhật AmmPosition trong cache
   *
   * @param ammPosition AmmPosition mới
   */
  public void updateAmmPosition(AmmPosition ammPosition) {
    updateCounter.incrementAndGet();
    ammPositionCache.put(ammPosition.getIdentifier(), ammPosition);
  }

  public boolean ammPositionCacheShouldFlush() {
    // Trả về true khi số lần cập nhật chia hết cho 100
    int count = updateCounter.get();
    return count > 0 && count % UPDATE_THRESHOLD == 0;
  }

  /**
   * Khởi tạo cache từ dữ liệu trong database.
   */
  public void initializeAmmPositionCache() {
    try {
      List<AmmPosition> dbAmmPositions = ammPositionRocksDB.getAllAmmPositions();
      int loadedCount = 0;

      for (AmmPosition dbAmmPosition : dbAmmPositions) {
        String identifier = dbAmmPosition.getIdentifier();
        if (identifier != null && !identifier.isEmpty()) {
          ammPositionCache.put(identifier, dbAmmPosition);
          loadedCount++;
        }
      }

      logger.info("AmmPosition cache đã được khởi tạo: {} bản ghi đã tải", loadedCount);
    } catch (Exception e) {
      logger.error("Không thể khởi tạo AmmPosition cache: {}", e.getMessage(), e);
    }
  }

  /**
   * Thêm AmmPosition vào batch để lưu vào database.
   *
   * @param ammPosition AmmPosition cần lưu
   */
  public void addAmmPositionToBatch(AmmPosition ammPosition) {
    if (ammPosition.getIdentifier() == null) {
      return;
    }
    String identifier = ammPosition.getIdentifier();
    latestAmmPositions.compute(identifier, (key, existingPosition) -> {
      if (existingPosition == null || ammPosition.getUpdatedAt() > existingPosition.getUpdatedAt()) {
        return ammPosition;
      }
      return existingPosition;
    });
  }

  public void flushAmmPositionToDisk() {
    if (latestAmmPositions.isEmpty()) {
      return;
    }
    ammPositionRocksDB.saveAmmPositionBatch(latestAmmPositions);
    latestAmmPositions.clear();
    logger.debug("Đã lưu AmmPosition thành công");
  }
}
