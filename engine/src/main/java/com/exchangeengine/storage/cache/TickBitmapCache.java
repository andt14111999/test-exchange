package com.exchangeengine.storage.cache;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.TickBitmap;
import com.exchangeengine.storage.rocksdb.TickBitmapRocksDB;

public class TickBitmapCache {
  private static final Logger logger = LoggerFactory.getLogger(TickBitmapCache.class);

  private static volatile TickBitmapCache instance;
  private final TickBitmapRocksDB tickBitmapRocksDB = TickBitmapRocksDB.getInstance();

  private final ConcurrentHashMap<String, TickBitmap> tickBitmapCache = new ConcurrentHashMap<>();
  private final Map<String, TickBitmap> latestTickBitmaps = new ConcurrentHashMap<>();

  private final AtomicInteger updateCounter = new AtomicInteger(0);
  private static final int UPDATE_THRESHOLD = 50;

  public static synchronized TickBitmapCache getInstance() {
    if (instance == null) {
      instance = new TickBitmapCache();
    }
    return instance;
  }

  private TickBitmapCache() {
  }

  public static void setTestInstance(TickBitmapCache testInstance) {
    instance = testInstance;
  }

  public static synchronized void resetInstance() {
    instance = null;
  }

  public Optional<TickBitmap> getTickBitmap(String poolPair) {
    TickBitmap bitmap = tickBitmapCache.get(poolPair);
    return Optional.ofNullable(bitmap);
  }

  public TickBitmap getOrInitTickBitmap(String poolPair) {
    return getTickBitmap(poolPair).orElseGet(() -> new TickBitmap(poolPair));
  }

  public TickBitmap getOrCreateTickBitmap(String poolPair) {
    TickBitmap tickBitmap = getOrInitTickBitmap(poolPair);
    tickBitmapCache.put(poolPair, tickBitmap);
    return tickBitmap;
  }

  public void updateTickBitmap(TickBitmap tickBitmap) {
    updateCounter.incrementAndGet();

    // Kiểm tra null trước khi thêm vào cache và batch
    if (tickBitmap != null && tickBitmap.getPoolPair() != null) {
      tickBitmapCache.put(tickBitmap.getPoolPair(), tickBitmap);
      addTickBitmapToBatch(tickBitmap);
    }
  }

  public void addTickBitmapToBatch(TickBitmap tickBitmap) {
    latestTickBitmaps.compute(tickBitmap.getPoolPair(), (key, existingBitmap) -> {
      if (existingBitmap == null || tickBitmap.getUpdatedAt() > existingBitmap.getUpdatedAt()) {
        return tickBitmap;
      }
      return existingBitmap;
    });
  }

  public boolean tickBitmapCacheShouldFlush() {
    int count = updateCounter.get();
    return (count > 0 && count % UPDATE_THRESHOLD == 0);
  }

  public void flushTickBitmapsToDisk() {
    if (latestTickBitmaps.isEmpty()) {
      return;
    }

    tickBitmapRocksDB.saveTickBitmapBatch(latestTickBitmaps);
    latestTickBitmaps.clear();
    logger.debug("Đã lưu tickBitmap thành công");
  }

  public void initializeTickBitmapCache() {
    try {
      List<TickBitmap> dbTickBitmaps = tickBitmapRocksDB.getAllTickBitmaps();
      int loadedCount = 0;

      for (TickBitmap dbTickBitmap : dbTickBitmaps) {
        String poolPair = dbTickBitmap.getPoolPair();
        if (poolPair != null && !poolPair.isEmpty()) {
          tickBitmapCache.put(poolPair, dbTickBitmap);
          loadedCount++;
        }
      }

      logger.info("TickBitmap cache đã được khởi tạo: {} bản ghi đã tải", loadedCount);
    } catch (Exception e) {
      logger.error("Không thể khởi tạo tick bitmap cache: {}", e.getMessage(), e);
    }
  }
}
