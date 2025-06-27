package com.exchangeengine.storage.rocksdb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rocksdb.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RocksDBConfigTest {

  @Test
  void testConstants() {
    // Test constants
    assertEquals("accounts", RocksDBConfig.ACCOUNT_CF);
    assertEquals("deposits", RocksDBConfig.DEPOSIT_CF);
    assertEquals("withdrawals", RocksDBConfig.WITHDRAWAL_CF);
    assertEquals("account_histories", RocksDBConfig.ACCOUNT_HISTORIES_CF);
    assertEquals("amm_pools", RocksDBConfig.AMM_POOL_CF);
    assertEquals("ticks", RocksDBConfig.TICK_CF);
    assertEquals("tick_bitmaps", RocksDBConfig.TICK_BITMAP_CF);
    assertEquals("amm_positions", RocksDBConfig.AMM_POSITION_CF);
    assertEquals("amm_orders", RocksDBConfig.AMM_ORDERS_CF);
    assertEquals("balance_locks", RocksDBConfig.BALANCE_LOCK_CF);
    assertEquals("kafka_group_state", RocksDBConfig.KAFKA_GROUP_STATE_CF);
    assertEquals("settings", RocksDBConfig.SETTINGS_CF);
    // Test column family index constants
    assertEquals(0, RocksDBConfig.DEFAULT_CF_INDEX);
    assertEquals(1, RocksDBConfig.ACCOUNT_CF_INDEX);
    assertEquals(2, RocksDBConfig.DEPOSIT_CF_INDEX);
    assertEquals(3, RocksDBConfig.WITHDRAWAL_CF_INDEX);
    assertEquals(4, RocksDBConfig.ACCOUNT_HISTORIES_CF_INDEX);
    assertEquals(5, RocksDBConfig.AMM_POOL_CF_INDEX);
    assertEquals(6, RocksDBConfig.MERCHANT_ESCROW_CF_INDEX);
    assertEquals(7, RocksDBConfig.TICK_CF_INDEX);
    assertEquals(8, RocksDBConfig.TICK_BITMAP_CF_INDEX);
    assertEquals(9, RocksDBConfig.AMM_POSITION_CF_INDEX);
    assertEquals(10, RocksDBConfig.AMM_ORDERS_CF_INDEX);
    assertEquals(11, RocksDBConfig.OFFER_CF_INDEX);
    assertEquals(12, RocksDBConfig.TRADE_CF_INDEX);
    assertEquals(13, RocksDBConfig.BALANCE_LOCK_CF_INDEX);
    assertEquals(14, RocksDBConfig.KAFKA_GROUP_STATE_CF_INDEX);
    assertEquals(15, RocksDBConfig.SETTINGS_CF_INDEX);
    // Test batch size constants
    assertEquals(10000, RocksDBConfig.DEFAULT_MAX_RECORDS_PER_BATCH);
    assertEquals(50 * 1024 * 1024, RocksDBConfig.DEFAULT_MAX_BATCH_SIZE_BYTES); // 50MB
    assertEquals(0.8, RocksDBConfig.BATCH_SIZE_THRESHOLD_PERCENT);

    // Test default RocksDB configuration constants
    assertEquals(1000, RocksDBConfig.DEFAULT_MAX_OPEN_FILES);
    assertEquals(64, RocksDBConfig.DEFAULT_WRITE_BUFFER_SIZE);
    assertEquals(3, RocksDBConfig.DEFAULT_MAX_WRITE_BUFFER_NUMBER);
    assertEquals(64, RocksDBConfig.DEFAULT_TARGET_FILE_SIZE_BASE);

    // Test history cache configuration constants
    assertEquals(8, RocksDBConfig.ACCOUNT_HISTORY_PREFIX_SIZE);
    assertEquals(8, RocksDBConfig.BLOOM_FILTER_BITS_PER_KEY);
    assertEquals(32, RocksDBConfig.BLOCK_CACHE_SIZE_MB);
  }

  @Test
  void testCreateDBOptions() {
    // Arrange & Act
    DBOptions options = RocksDBConfig.createDBOptions(2000);

    // Assert
    assertNotNull(options);
    assertTrue(options.createIfMissing());
    assertTrue(options.createMissingColumnFamilies());
    assertEquals(2000, options.maxOpenFiles());

    // Clean up to avoid resource leak
    options.close();
  }

  @Test
  void testCreateStandardColumnFamilyOptions() {
    // Arrange
    long writeBufferSize = 128 * 1024 * 1024; // 128MB
    int maxWriteBufferNumber = 5;
    long targetFileSizeBase = 256 * 1024 * 1024; // 256MB

    // Act
    ColumnFamilyOptions options = RocksDBConfig.createStandardColumnFamilyOptions(
        writeBufferSize, maxWriteBufferNumber, targetFileSizeBase);

    // Assert
    assertNotNull(options);
    assertEquals(writeBufferSize, options.writeBufferSize());
    assertEquals(maxWriteBufferNumber, options.maxWriteBufferNumber());
    assertEquals(targetFileSizeBase, options.targetFileSizeBase());

    // Clean up to avoid resource leak
    options.close();
  }

  @Test
  void testCreateHistoryColumnFamilyOptions() {
    // Arrange
    long writeBufferSize = 128 * 1024 * 1024; // 128MB
    int maxWriteBufferNumber = 5;
    long targetFileSizeBase = 256 * 1024 * 1024; // 256MB

    // Act
    ColumnFamilyOptions options = RocksDBConfig.createHistoryColumnFamilyOptions(
        writeBufferSize, maxWriteBufferNumber, targetFileSizeBase);

    // Assert
    assertNotNull(options);
    assertEquals(writeBufferSize, options.writeBufferSize());
    assertEquals(maxWriteBufferNumber, options.maxWriteBufferNumber());
    assertEquals(targetFileSizeBase, options.targetFileSizeBase());
    assertTrue(options.optimizeFiltersForHits());

    // Clean up to avoid resource leak
    options.close();
  }

  @Test
  void testCreateWriteOptions() {
    // Act
    WriteOptions options = RocksDBConfig.createWriteOptions();

    // Assert
    assertNotNull(options);
    assertFalse(options.sync());
    assertFalse(options.disableWAL());

    // Clean up to avoid resource leak
    options.close();
  }

  @Test
  void testEmptyConstructor() {
    // Arrange & Act
    RocksDBConfig config = new RocksDBConfig();

    // Assert
    assertNotNull(config);
  }

  @Test
  void testGetConfigDefaultValues() {
    // Make sure not to override any env vars for this test
    // Just testing default implementations

    // Act & Assert
    assertNotNull(RocksDBConfig.getDbPath());
    assertTrue(RocksDBConfig.getMaxOpenFiles() > 0);
    assertTrue(RocksDBConfig.getWriteBufferSize() > 0);
    assertTrue(RocksDBConfig.getMaxWriteBufferNumber() > 0);
    assertTrue(RocksDBConfig.getTargetFileSizeBase() > 0);
  }
}
