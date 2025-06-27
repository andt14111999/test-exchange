package com.exchangeengine.storage.rocksdb;

import org.rocksdb.DBOptions;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.WriteOptions;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.BloomFilter;
import org.rocksdb.LRUCache;
import com.exchangeengine.util.EnvManager;

/**
 * Lớp chứa các cấu hình và hằng số cho RocksDB
 */
public class RocksDBConfig {
  private static final EnvManager envManager = EnvManager.getInstance();

  // Đường dẫn mặc định đến thư mục dữ liệu
  public static final String DEFAULT_DB_PATH = "./data/rocksdb/development";

  // Tên các column family
  public static final String ACCOUNT_CF = "accounts";
  public static final String DEPOSIT_CF = "deposits";
  public static final String WITHDRAWAL_CF = "withdrawals";
  public static final String ACCOUNT_HISTORIES_CF = "account_histories";
  public static final String AMM_POOL_CF = "amm_pools";
  public static final String MERCHANT_ESCROW_CF = "merchant_escrows";
  public static final String TICK_CF = "ticks";
  public static final String TICK_BITMAP_CF = "tick_bitmaps";
  public static final String AMM_POSITION_CF = "amm_positions";
  public static final String AMM_ORDERS_CF = "amm_orders";
  public static final String OFFER_CF = "offers";
  public static final String TRADE_CF = "trades";
  public static final String BALANCE_LOCK_CF = "balance_locks";
  public static final String KAFKA_GROUP_STATE_CF = "kafka_group_state";
  public static final String SETTINGS_CF = "settings";

  public static final int DEFAULT_CF_INDEX = 0;
  public static final int ACCOUNT_CF_INDEX = 1;
  public static final int DEPOSIT_CF_INDEX = 2;
  public static final int WITHDRAWAL_CF_INDEX = 3;
  public static final int ACCOUNT_HISTORIES_CF_INDEX = 4;
  public static final int AMM_POOL_CF_INDEX = 5;
  public static final int MERCHANT_ESCROW_CF_INDEX = 6;
  public static final int TICK_CF_INDEX = 7;
  public static final int TICK_BITMAP_CF_INDEX = 8;
  public static final int AMM_POSITION_CF_INDEX = 9;
  public static final int AMM_ORDERS_CF_INDEX = 10;
  public static final int OFFER_CF_INDEX = 11;
  public static final int TRADE_CF_INDEX = 12;
  public static final int BALANCE_LOCK_CF_INDEX = 13;
  public static final int KAFKA_GROUP_STATE_CF_INDEX = 14;
  public static final int SETTINGS_CF_INDEX = 15;

  // Cấu hình batch size mặc định
  public static final int DEFAULT_MAX_RECORDS_PER_BATCH = 10000;
  public static final long DEFAULT_MAX_BATCH_SIZE_BYTES = 50 * 1024 * 1024; // 50MB
  public static final double BATCH_SIZE_THRESHOLD_PERCENT = 0.8; // 80% của max size

  // Cấu hình mặc định cho RocksDB
  public static final int DEFAULT_MAX_OPEN_FILES = 1000;
  public static final int DEFAULT_WRITE_BUFFER_SIZE = 64; // MB
  public static final int DEFAULT_MAX_WRITE_BUFFER_NUMBER = 3;
  public static final int DEFAULT_TARGET_FILE_SIZE_BASE = 64; // MB

  // Cấu hình cho history cache
  public static final int ACCOUNT_HISTORY_PREFIX_SIZE = 8;
  public static final int BLOOM_FILTER_BITS_PER_KEY = 8;
  public static final int BLOCK_CACHE_SIZE_MB = 32;

  public RocksDBConfig() {
  }

  // Lấy cấu hình từ môi trường
  public static String getDbPath() {
    return envManager.get("ROCKSDB_DATA_DIR", DEFAULT_DB_PATH);
  }

  public static int getMaxOpenFiles() {
    return envManager.getInt("ROCKSDB_MAX_OPEN_FILES", DEFAULT_MAX_OPEN_FILES);
  }

  public static int getWriteBufferSize() {
    return envManager.getInt("ROCKSDB_WRITE_BUFFER_SIZE", DEFAULT_WRITE_BUFFER_SIZE);
  }

  public static int getMaxWriteBufferNumber() {
    return envManager.getInt("ROCKSDB_MAX_WRITE_BUFFER_NUMBER", DEFAULT_MAX_WRITE_BUFFER_NUMBER);
  }

  public static int getTargetFileSizeBase() {
    return envManager.getInt("ROCKSDB_TARGET_FILE_SIZE_BASE", DEFAULT_TARGET_FILE_SIZE_BASE);
  }

  /**
   * Tạo và trả về DBOptions được cấu hình
   *
   * @param maxOpenFiles Số lượng file tối đa có thể mở
   * @return DBOptions được cấu hình
   */
  public static DBOptions createDBOptions(int maxOpenFiles) {
    return new DBOptions()
        .setCreateIfMissing(true)
        .setCreateMissingColumnFamilies(true)
        .setMaxOpenFiles(maxOpenFiles)
        .setMaxBackgroundJobs(Runtime.getRuntime().availableProcessors());
  }

  /**
   * Tạo và trả về ColumnFamilyOptions tiêu chuẩn
   *
   * @param writeBufferSizeBytes    Kích thước buffer ghi (bytes)
   * @param maxWriteBufferNumber    Số lượng buffer ghi tối đa
   * @param targetFileSizeBaseBytes Kích thước file đích (bytes)
   * @return ColumnFamilyOptions được cấu hình
   */
  public static ColumnFamilyOptions createStandardColumnFamilyOptions(
      long writeBufferSizeBytes, int maxWriteBufferNumber, long targetFileSizeBaseBytes) {
    return new ColumnFamilyOptions()
        .setWriteBufferSize(writeBufferSizeBytes)
        .setMaxWriteBufferNumber(maxWriteBufferNumber)
        .setTargetFileSizeBase(targetFileSizeBaseBytes);
  }

  /**
   * Tạo và trả về ColumnFamilyOptions cho lịch sử giao dịch
   *
   * @param writeBufferSizeBytes    Kích thước buffer ghi (bytes)
   * @param maxWriteBufferNumber    Số lượng buffer ghi tối đa
   * @param targetFileSizeBaseBytes Kích thước file đích (bytes)
   * @return ColumnFamilyOptions được cấu hình cho lịch sử giao dịch
   */
  public static ColumnFamilyOptions createHistoryColumnFamilyOptions(
      long writeBufferSizeBytes, int maxWriteBufferNumber, long targetFileSizeBaseBytes) {
    return new ColumnFamilyOptions()
        .setWriteBufferSize(writeBufferSizeBytes)
        .setMaxWriteBufferNumber(maxWriteBufferNumber)
        .setTargetFileSizeBase(targetFileSizeBaseBytes)
        .setOptimizeFiltersForHits(true) // Tăng hiệu suất filter
        .useFixedLengthPrefixExtractor(ACCOUNT_HISTORY_PREFIX_SIZE) // Chia index theo prefix 8 byte (tìm kiếm nhanh)
        .setTableFormatConfig(new BlockBasedTableConfig()
            .setFilterPolicy(new BloomFilter(BLOOM_FILTER_BITS_PER_KEY, false)) // Giữ Bloom Filter nhưng dùng ít RAM
                                                                                // hơn
            .setCacheIndexAndFilterBlocks(true) // Để RocksDB cache index & filter trên disk thay vì RAM
            .setBlockCache(new LRUCache(BLOCK_CACHE_SIZE_MB * 1024 * 1024)) // Giới hạn cache
        );
  }

  /**
   * Tạo và trả về WriteOptions được cấu hình
   *
   * @return WriteOptions được cấu hình
   */
  public static WriteOptions createWriteOptions() {
    return new WriteOptions().setSync(false).setDisableWAL(false);
  }
}
