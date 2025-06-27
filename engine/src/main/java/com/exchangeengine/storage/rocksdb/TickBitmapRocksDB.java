package com.exchangeengine.storage.rocksdb;

import com.exchangeengine.model.TickBitmap;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TickBitmapRocksDB {
  private static volatile TickBitmapRocksDB instance;
  private final RocksDBService rocksDBService;

  public static synchronized TickBitmapRocksDB getInstance() {
    if (instance == null) {
      instance = new TickBitmapRocksDB();
    }
    return instance;
  }

  public static void resetInstance() {
    instance = null;
  }

  public static void setTestInstance(TickBitmapRocksDB testInstance) {
    instance = testInstance;
  }

  private TickBitmapRocksDB() {
    this.rocksDBService = RocksDBService.getInstance();
  }

  public void saveTickBitmap(TickBitmap tickBitmap) {
    rocksDBService.saveObject(tickBitmap, rocksDBService.getTickBitmapCF(), TickBitmap::getPoolPair, "tick_bitmap");
  }

  public Optional<TickBitmap> getTickBitmap(String poolPair) {
    return rocksDBService.getObject(poolPair, rocksDBService.getTickBitmapCF(), TickBitmap.class, "tick_bitmap");
  }

  public List<TickBitmap> getAllTickBitmaps() {
    return rocksDBService.getAllObjects(rocksDBService.getTickBitmapCF(), TickBitmap.class, "tick_bitmaps");
  }

  public void saveTickBitmapBatch(Map<String, TickBitmap> tickBitmaps) {
    rocksDBService.saveBatch(tickBitmaps, rocksDBService.getTickBitmapCF(), TickBitmap::getPoolPair, "tick_bitmaps");
  }
}
