package com.exchangeengine.storage.rocksdb;

import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.util.JsonSerializer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service cơ bản để tương tác với RocksDB
 */
public class RocksDBService implements AutoCloseable {
  // Singleton instance
  private static volatile RocksDBService instance;

  private static final Logger logger = LoggerFactory.getLogger(RocksDBService.class);

  private final String dbPath;
  private final int maxOpenFiles;
  private final long writeBufferSize;
  private final int maxWriteBufferNumber;
  private final long targetFileSizeBase;

  private RocksDB db;
  private ColumnFamilyHandle defaultCF;
  private ColumnFamilyHandle accountCF;
  private ColumnFamilyHandle depositCF;
  private ColumnFamilyHandle withdrawalCF;
  private ColumnFamilyHandle accountHistoryCF;
  private ColumnFamilyHandle ammPoolCF;
  private ColumnFamilyHandle merchantEscrowCF;
  private ColumnFamilyHandle tickCF;
  private ColumnFamilyHandle tickBitmapCF;
  private ColumnFamilyHandle ammPositionCF;
  private ColumnFamilyHandle ammOrdersCF;
  private ColumnFamilyHandle offerCF;
  private ColumnFamilyHandle tradeCF;
  private ColumnFamilyHandle balanceLockCF;
  private ColumnFamilyHandle kafkaGroupStateCF;
  private ColumnFamilyHandle settingsCF;

  private List<ColumnFamilyHandle> columnFamilyHandles;
  private WriteOptions writeOptions;

  static {
    RocksDB.loadLibrary();
  }

  /**
   * Lấy instance của RocksDBService với cấu hình từ RocksDBConfig.
   *
   * @return Instance của RocksDBService
   */
  public static synchronized RocksDBService getInstance() {
    if (instance == null) {
      instance = new RocksDBService();
      instance.initialize();

      logger.info("Initialized RocksDB with configuration from environment");
    }
    return instance;
  }

  /**
   * Constructor với các tham số cấu hình.
   * Private để đảm bảo Singleton pattern.
   */
  private RocksDBService() {
    this.dbPath = RocksDBConfig.getDbPath();
    this.maxOpenFiles = RocksDBConfig.getMaxOpenFiles();
    this.writeBufferSize = RocksDBConfig.getWriteBufferSize() * 1024 * 1024;
    this.maxWriteBufferNumber = RocksDBConfig.getMaxWriteBufferNumber();
    this.targetFileSizeBase = RocksDBConfig.getTargetFileSizeBase() * 1024 * 1024;
  }

  /**
   * Khởi tạo RocksDB với các cấu hình đã cho.
   */
  public void initialize() {
    try {
      logger.info("Initializing RocksDB at {}", dbPath);

      // Tạo thư mục nếu chưa tồn tại
      File dbDir = new File(dbPath);
      if (!dbDir.exists()) {
        Files.createDirectories(dbDir.toPath());
      }

      // Cấu hình RocksDB từ RocksDBConfig
      DBOptions options = RocksDBConfig.createDBOptions(maxOpenFiles);

      // Tạo standard column family options
      ColumnFamilyOptions cfOptions = RocksDBConfig.createStandardColumnFamilyOptions(
          writeBufferSize, maxWriteBufferNumber, targetFileSizeBase);

      // Tạo history column family options với cấu hình nâng cao
      ColumnFamilyOptions cfHistoryOptions = RocksDBConfig.createHistoryColumnFamilyOptions(
          writeBufferSize, maxWriteBufferNumber, targetFileSizeBase);

      // Tạo danh sách column family
      List<ColumnFamilyDescriptor> columnFamilyDescriptors = new ArrayList<>();
      columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfOptions));
      columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDBConfig.ACCOUNT_CF.getBytes(), cfOptions));
      columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDBConfig.DEPOSIT_CF.getBytes(), cfOptions));
      columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDBConfig.WITHDRAWAL_CF.getBytes(), cfOptions));
      columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDBConfig.ACCOUNT_HISTORIES_CF.getBytes(),
          cfHistoryOptions));
      columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDBConfig.AMM_POOL_CF.getBytes(), cfOptions));
      columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDBConfig.MERCHANT_ESCROW_CF.getBytes(), cfOptions));
      columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDBConfig.TICK_CF.getBytes(), cfOptions));
      columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDBConfig.TICK_BITMAP_CF.getBytes(), cfOptions));
      columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDBConfig.AMM_POSITION_CF.getBytes(), cfOptions));
      columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDBConfig.AMM_ORDERS_CF.getBytes(), cfOptions));
      columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDBConfig.OFFER_CF.getBytes(), cfOptions));
      columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDBConfig.TRADE_CF.getBytes(), cfOptions));
      columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDBConfig.BALANCE_LOCK_CF.getBytes(), cfOptions));
      columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDBConfig.KAFKA_GROUP_STATE_CF.getBytes(), cfOptions));
      columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDBConfig.SETTINGS_CF.getBytes(), cfOptions));

      // Mở RocksDB
      columnFamilyHandles = new ArrayList<>();
      db = RocksDB.open(options, dbPath, columnFamilyDescriptors, columnFamilyHandles);

      // Lấy column family handle
      defaultCF = columnFamilyHandles.get(RocksDBConfig.DEFAULT_CF_INDEX);
      accountCF = columnFamilyHandles.get(RocksDBConfig.ACCOUNT_CF_INDEX);
      depositCF = columnFamilyHandles.get(RocksDBConfig.DEPOSIT_CF_INDEX);
      withdrawalCF = columnFamilyHandles.get(RocksDBConfig.WITHDRAWAL_CF_INDEX);
      accountHistoryCF = columnFamilyHandles.get(RocksDBConfig.ACCOUNT_HISTORIES_CF_INDEX);
      ammPoolCF = columnFamilyHandles.get(RocksDBConfig.AMM_POOL_CF_INDEX);
      merchantEscrowCF = columnFamilyHandles.get(RocksDBConfig.MERCHANT_ESCROW_CF_INDEX);
      tickCF = columnFamilyHandles.get(RocksDBConfig.TICK_CF_INDEX);
      tickBitmapCF = columnFamilyHandles.get(RocksDBConfig.TICK_BITMAP_CF_INDEX);
      ammPositionCF = columnFamilyHandles.get(RocksDBConfig.AMM_POSITION_CF_INDEX);
      ammOrdersCF = columnFamilyHandles.get(RocksDBConfig.AMM_ORDERS_CF_INDEX);
      offerCF = columnFamilyHandles.get(RocksDBConfig.OFFER_CF_INDEX);
      tradeCF = columnFamilyHandles.get(RocksDBConfig.TRADE_CF_INDEX);
      balanceLockCF = columnFamilyHandles.get(RocksDBConfig.BALANCE_LOCK_CF_INDEX);
      kafkaGroupStateCF = columnFamilyHandles.get(RocksDBConfig.KAFKA_GROUP_STATE_CF_INDEX);
      settingsCF = columnFamilyHandles.get(RocksDBConfig.SETTINGS_CF_INDEX);

      // Cấu hình write options từ RocksDBConfig
      writeOptions = RocksDBConfig.createWriteOptions();

      logger.info("RocksDB initialized successfully");
    } catch (RocksDBException | IOException e) {
      logger.error("Failed to initialize RocksDB: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to initialize RocksDB", e);
    }
  }

  /**
   * Tạo snapshot của database.
   *
   * @return Snapshot
   */
  public Snapshot createSnapshot() {
    return db.getSnapshot();
  }

  /**
   * Giải phóng snapshot.
   *
   * @param snapshot Snapshot cần giải phóng
   */
  public void releaseSnapshot(Snapshot snapshot) {
    db.releaseSnapshot(snapshot);
  }

  /**
   * Đóng RocksDB và giải phóng tài nguyên.
   */
  @Override
  public void close() {
    logger.info("Bắt đầu đóng RocksDB...");

    try {
      if (writeOptions != null) {
        writeOptions.close();
      }

      if (columnFamilyHandles != null) {
        for (ColumnFamilyHandle handle : columnFamilyHandles) {
          handle.close();
        }
      }

      if (db != null) {
        db.close();
      }
    } catch (Exception e) {
      logger.warn("Lỗi khi đóng WriteOptions: {}", e.getMessage());
    }

    logger.info("Đã đóng RocksDB thành công");
  }

  // Getters cho các column family handle
  public ColumnFamilyHandle getDefaultCF() {
    return defaultCF;
  }

  public ColumnFamilyHandle getAccountCF() {
    return accountCF;
  }

  public ColumnFamilyHandle getDepositCF() {
    return depositCF;
  }

  public ColumnFamilyHandle getWithdrawalCF() {
    return withdrawalCF;
  }

  public ColumnFamilyHandle getAccountHistoryCF() {
    return accountHistoryCF;
  }

  public ColumnFamilyHandle getAmmPoolCF() {
    return ammPoolCF;
  }

  public ColumnFamilyHandle getMerchantEscrowCF() {
    return merchantEscrowCF;
  }

  public ColumnFamilyHandle getTickCF() {
    return tickCF;
  }

  public ColumnFamilyHandle getTickBitmapCF() {
    return tickBitmapCF;
  }

  public ColumnFamilyHandle getAmmPositionCF() {
    return ammPositionCF;
  }

  public ColumnFamilyHandle getAmmOrdersCF() {
    return ammOrdersCF;
  }

  public ColumnFamilyHandle getOfferCF() {
    return offerCF;
  }

  public ColumnFamilyHandle getTradeCF() {
    return tradeCF;
  }

  public ColumnFamilyHandle getBalanceLockCF() {
    return balanceLockCF;
  }

  public ColumnFamilyHandle getKafkaGroupStateCF() {
    return kafkaGroupStateCF;
  }

  public ColumnFamilyHandle getSettingsCF() {
    return settingsCF;
  }

  public WriteOptions getWriteOptions() {
    return writeOptions;
  }

  public RocksDB getDb() {
    return db;
  }

  public static void resetInstance() {
    instance = null;
  }

  /**
   * Thiết lập instance kiểm thử (chỉ sử dụng cho testing)
   *
   * @param testInstance Instance kiểm thử cần thiết lập
   */
  public static void setTestInstance(RocksDBService testInstance) {
    instance = testInstance;
  }

  // ==================== HELPER METHODS ====================

  /**
   * Phương thức chung để lưu một đối tượng vào RocksDB.
   *
   * @param <T>          Kiểu dữ liệu cần lưu
   * @param item         Đối tượng cần lưu
   * @param cf           Column family handle
   * @param keyExtractor Hàm để trích xuất key từ đối tượng
   * @param logPrefix    Tiền tố cho log
   */
  public <T> void saveObject(T item, ColumnFamilyHandle cf, KeyExtractor<T> keyExtractor, String logPrefix) {
    if (item == null) {
      return;
    }

    String key = keyExtractor.getKey(item);
    if (key == null || key.isEmpty()) {
      return;
    }

    try {
      byte[] keyBytes = key.getBytes();
      byte[] valueBytes = JsonSerializer.serialize(item);

      db.put(cf, writeOptions, keyBytes, valueBytes);
    } catch (RocksDBException e) {
      logger.error("Lỗi khi lưu {} {}: {}", logPrefix, key, e.getMessage());
    }
  }

  /**
   * Phương thức chung để lấy một đối tượng từ RocksDB.
   *
   * @param <T>        Kiểu dữ liệu cần lấy
   * @param key        Key của đối tượng
   * @param cf         Column family handle
   * @param valueClass Class của đối tượng cần deserialize
   * @param logPrefix  Tiền tố cho log
   * @return Optional chứa đối tượng nếu tồn tại
   */
  public <T> Optional<T> getObject(String key, ColumnFamilyHandle cf, Class<T> valueClass, String logPrefix) {
    if (key == null || key.isEmpty()) {
      logger.warn("Không thể lấy {} với key null hoặc rỗng", logPrefix);
      return Optional.empty();
    }

    try {
      byte[] keyBytes = key.getBytes();
      byte[] valueBytes = db.get(cf, keyBytes);

      if (valueBytes == null) {
        return Optional.empty();
      }

      T item = JsonSerializer.deserialize(valueBytes, valueClass);
      return Optional.of(item);
    } catch (RocksDBException e) {
      logger.error("Lỗi khi lấy {} {}: {}", logPrefix, key, e.getMessage());
      throw new RuntimeException("Lỗi khi lấy " + logPrefix, e);
    }
  }

  /**
   * Phương thức chung để lấy tất cả các đối tượng từ một column family.
   *
   * @param <T>        Kiểu dữ liệu cần lấy
   * @param cf         Column family handle
   * @param valueClass Class của đối tượng cần deserialize
   * @param logPrefix  Tiền tố cho log
   * @return Danh sách các đối tượng
   */
  public <T> List<T> getAllObjects(ColumnFamilyHandle cf, Class<T> valueClass, String logPrefix) {
    List<T> items = new ArrayList<>();
    int errorCount = 0;

    try (RocksIterator iterator = db.newIterator(cf)) {
      iterator.seekToFirst();
      while (iterator.isValid()) {
        byte[] value = iterator.value();
        try {
          T item = JsonSerializer.deserialize(value, valueClass);
          items.add(item);
        } catch (Exception e) {
          logger.warn("Lỗi khi deserialize {}: {}", logPrefix, e.getMessage());
          errorCount++;
        }
        iterator.next();
      }
    }

    if (errorCount > 0) {
      logger.warn("Bỏ qua {} bản ghi lỗi khi lấy {}", errorCount, logPrefix);
    }

    return items;
  }

  /**
   * Phương thức chung để lưu batch dữ liệu vào RocksDB.
   *
   * @param <T>          Kiểu dữ liệu cần lưu
   * @param data         Map chứa dữ liệu cần lưu
   * @param cf           Column family handle
   * @param keyExtractor Hàm để trích xuất key từ đối tượng
   * @param logPrefix    Tiền tố cho log
   */
  public <T> void saveBatch(Map<String, T> data, ColumnFamilyHandle cf, KeyExtractor<T> keyExtractor,
      String logPrefix) {
    if (data.isEmpty()) {
      return;
    }

    // Xác định cấu hình batch size dựa trên loại dữ liệu
    int maxRecordsPerBatch = RocksDBConfig.DEFAULT_MAX_RECORDS_PER_BATCH;
    long maxBatchSizeBytes = RocksDBConfig.DEFAULT_MAX_BATCH_SIZE_BYTES;

    // Tính ngưỡng kích thước batch (80% của max size)
    long batchSizeThreshold = (long) (maxBatchSizeBytes * RocksDBConfig.BATCH_SIZE_THRESHOLD_PERCENT);

    // Ước tính kích thước trung bình của một item
    long estimatedItemSize = estimateItemSize(data, keyExtractor);

    // Chuẩn bị dữ liệu song song và lưu trữ
    List<Pair<byte[], byte[]>> preparedData = data.values().parallelStream()
        .filter(item -> item != null && keyExtractor.getKey(item) != null && !keyExtractor.getKey(item).isEmpty())
        .map(item -> prepareDataPair(item, keyExtractor))
        .filter(pair -> pair != null)
        .collect(Collectors.toList());

    // Tính toán số lượng batch dựa trên kích thước ước tính
    int estimatedItemsPerBatch = (int) Math.min(maxRecordsPerBatch,
        batchSizeThreshold / Math.max(1, estimatedItemSize));

    // Chia thành các batch và lưu
    int totalItems = preparedData.size();
    int totalBatches = (totalItems + estimatedItemsPerBatch - 1) / estimatedItemsPerBatch; // Ceiling division

    for (int i = 0; i < totalBatches; i++) {
      int fromIndex = i * estimatedItemsPerBatch;
      int toIndex = Math.min(fromIndex + estimatedItemsPerBatch, totalItems);

      List<Pair<byte[], byte[]>> batch = preparedData.subList(fromIndex, toIndex);
      saveBatchInternal(batch, cf, logPrefix);
    }

    logger.debug("Đã lưu {} {} trong {} batches", preparedData.size(), logPrefix, totalBatches);
  }

  /**
   * Ước tính kích thước trung bình của một item
   *
   * @param <T>          Kiểu dữ liệu cần lưu
   * @param data         Map chứa dữ liệu cần lưu
   * @param keyExtractor Hàm để trích xuất key từ đối tượng
   * @return Kích thước ước tính
   */
  private <T> long estimateItemSize(Map<String, T> data, KeyExtractor<T> keyExtractor) {
    if (data.isEmpty()) {
      return 1024;
    }

    T sampleItem = data.values().iterator().next();
    try {
      String key = keyExtractor.getKey(sampleItem);
      byte[] keyBytes = key.getBytes();
      byte[] valueBytes = JsonSerializer.serialize(sampleItem);

      long sampleSize = keyBytes.length + valueBytes.length;

      return (long) (sampleSize * 1.3);
    } catch (Exception e) {
      logger.warn("Lỗi khi ước tính kích thước item: {}", e.getMessage());
      return 1024;
    }
  }

  /**
   * Lưu một batch dữ liệu vào RocksDB
   *
   * @param items     Danh sách các cặp key-value cần lưu
   * @param cf        Column family handle
   * @param logPrefix Tiền tố cho log
   * @return Số lượng bản ghi đã lưu
   */
  private int saveBatchInternal(List<Pair<byte[], byte[]>> items, ColumnFamilyHandle cf, String logPrefix) {
    if (items.isEmpty()) {
      return 0;
    }

    try (WriteBatch batch = new WriteBatch()) {
      for (Pair<byte[], byte[]> item : items) {
        batch.put(cf, item.getKey(), item.getValue());
      }

      db.write(writeOptions, batch);
      return items.size();
    } catch (Exception e) {
      logger.error("Lỗi khi lưu batch {}: {}", logPrefix, e.getMessage());
      return 0;
    }
  }

  /**
   * Hàm kiểm tra nhanh nếu keyBytes bắt đầu bằng prefixBytes
   */
  public boolean startsWith(byte[] keyBytes, byte[] prefixBytes) {
    if (keyBytes.length < prefixBytes.length) {
      return false;
    }
    for (int i = 0; i < prefixBytes.length; i++) {
      if (keyBytes[i] != prefixBytes[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Phương thức chung để lấy đối tượng từ RocksDB theo prefix.
   *
   * @param <T>        Kiểu dữ liệu cần lấy
   * @param prefix     Prefix dùng để tìm kiếm
   * @param limit      Số lượng bản ghi tối đa cần lấy
   * @param lastKey    Key cuối cùng của trang trước (dùng cho phân trang)
   * @param cf         Column family handle
   * @param valueClass Class của đối tượng cần deserialize
   * @param logPrefix  Tiền tố cho log
   * @return Danh sách các đối tượng
   */
  public <T> List<T> getObjectsByPrefix(String prefix, int limit, String lastKey,
      ColumnFamilyHandle cf, Class<T> valueClass, String logPrefix) {
    List<T> items = new ArrayList<>();

    // Tạo ReadOptions để tối ưu tìm kiếm theo prefix
    try (ReadOptions readOptions = new ReadOptions().setPrefixSameAsStart(true);
        RocksIterator iterator = db.newIterator(cf, readOptions)) {
      byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);

      if (lastKey != null && !lastKey.isEmpty()) {
        // Nếu có lastKey, tìm kiếm từ key tiếp theo của lastKey
        iterator.seek(lastKey.getBytes());
        iterator.next();
      } else {
        // Nếu không có lastKey, tìm kiếm từ prefix
        iterator.seek(prefixBytes);
      }

      int count = 0;
      while (iterator.isValid() && count < limit) {
        byte[] keyBytes = iterator.key();

        // Kiểm tra nếu key không bắt đầu bằng prefix thì dừng
        if (!startsWith(keyBytes, prefixBytes)) {
          break;
        }

        try {
          byte[] valueBytes = iterator.value();
          T item = JsonSerializer.deserialize(valueBytes, valueClass);
          items.add(item);
          count++;
        } catch (Exception e) {
          logger.warn("Lỗi khi deserialize {} với key {}: {}", logPrefix,
              new String(keyBytes, StandardCharsets.UTF_8), e.getMessage());
        }

        iterator.next();
      }
    }

    return items;
  }

  private <T> Pair<byte[], byte[]> prepareDataPair(T item, KeyExtractor<T> keyExtractor) {
    try {
      String key = keyExtractor.getKey(item);
      byte[] keyBytes = key.getBytes();
      byte[] valueBytes = JsonSerializer.serialize(item);
      return new Pair<>(keyBytes, valueBytes);
    } catch (Exception e) {
      logger.error("Lỗi khi chuẩn bị dữ liệu cho {}: {}", keyExtractor.getKey(item), e.getMessage());
      return null;
    }
  }
}
