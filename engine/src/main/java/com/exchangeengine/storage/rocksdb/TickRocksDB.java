package com.exchangeengine.storage.rocksdb;

import com.exchangeengine.model.Tick;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TickRocksDB {
  private static volatile TickRocksDB instance;
  private final RocksDBService rocksDBService;

  public static synchronized TickRocksDB getInstance() {
    if (instance == null) {
      instance = new TickRocksDB();
    }
    return instance;
  }

  public static void resetInstance() {
    instance = null;
  }

  public static void setTestInstance(TickRocksDB testInstance) {
    instance = testInstance;
  }

  private TickRocksDB() {
    this.rocksDBService = RocksDBService.getInstance();
  }

  public void saveTick(Tick tick) {
    rocksDBService.saveObject(tick, rocksDBService.getTickCF(), Tick::getTickKey, "tick");
  }

  public Optional<Tick> getTick(String tickKey) {
    return rocksDBService.getObject(tickKey, rocksDBService.getTickCF(), Tick.class, "tick");
  }

  public List<Tick> getAllTicks() {
    return rocksDBService.getAllObjects(rocksDBService.getTickCF(), Tick.class, "ticks");
  }

  public void saveTickBatch(Map<String, Tick> ticks) {
    rocksDBService.saveBatch(ticks, rocksDBService.getTickCF(), Tick::getTickKey, "ticks");
  }
}
