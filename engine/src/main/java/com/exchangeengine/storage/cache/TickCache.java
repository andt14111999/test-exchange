package com.exchangeengine.storage.cache;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.Tick;
import com.exchangeengine.storage.rocksdb.TickRocksDB;

public class TickCache {
  private static final Logger logger = LoggerFactory.getLogger(TickCache.class);

  private static volatile TickCache instance;
  private final TickRocksDB tickRocksDB = TickRocksDB.getInstance();

  private final ConcurrentHashMap<String, Tick> tickCache = new ConcurrentHashMap<>();
  private final Map<String, Tick> latestTicks = new ConcurrentHashMap<>();

  private final AtomicInteger updateCounter = new AtomicInteger(0);
  private static final int BACKUP_BATCH_SIZE = 1000;

  public static synchronized TickCache getInstance() {
    if (instance == null) {
      instance = new TickCache();
    }
    return instance;
  }

  private TickCache() {
  }

  public static void setTestInstance(TickCache testInstance) {
    instance = testInstance;
  }

  public static synchronized void resetInstance() {
    instance = null;
  }

  public Optional<Tick> getTick(String tickKey) {
    Tick tick = tickCache.get(tickKey);
    return Optional.ofNullable(tick);
  }

  public Tick getOrInitTick(String poolPair, int tickIndex) {
    String tickKey = poolPair + "-" + tickIndex;
    return getTick(tickKey).orElseGet(() -> {
      Tick tick = new Tick(poolPair, tickIndex);
      return tick;
    });
  }

  public Tick getOrCreateTick(String poolPair, int tickIndex) {
    Tick tick = getOrInitTick(poolPair, tickIndex);
    updateTick(tick);
    return tick;
  }

  public void updateTick(Tick tick) {
    updateCounter.incrementAndGet();
    tickCache.put(tick.getTickKey(), tick);
    addTickToBatch(tick);
  }

  public void addTickToBatch(Tick tick) {
    String tickKey = tick.getTickKey();
    latestTicks.compute(tickKey, (key, existingTick) -> {
      if (existingTick == null || tick.getUpdatedAt() > existingTick.getUpdatedAt()) {
        return tick;
      }
      return existingTick;
    });
  }

  public boolean tickCacheShouldFlush() {
    int count = updateCounter.get();
    return (count > 0 && count % BACKUP_BATCH_SIZE == 0);
  }

  public void flushTicksToDisk() {
    if (latestTicks.isEmpty()) {
      return;
    }

    tickRocksDB.saveTickBatch(latestTicks);
    logger.info("Flushed {} Ticks to RocksDB", latestTicks.size());
    latestTicks.clear();
  }

  public void initializeTickCache() {
    try {
      List<Tick> dbTicks = tickRocksDB.getAllTicks();
      int loadedCount = 0;

      for (Tick dbTick : dbTicks) {
        String tickKey = dbTick.getTickKey();
        if (tickKey != null && !tickKey.isEmpty()) {
          tickCache.put(tickKey, dbTick);
          loadedCount++;
        }
      }

      logger.info("Tick cache đã được khởi tạo: {} bản ghi đã tải", loadedCount);
    } catch (Exception e) {
      logger.error("Không thể khởi tạo tick cache: {}", e.getMessage(), e);
    }
  }
}
