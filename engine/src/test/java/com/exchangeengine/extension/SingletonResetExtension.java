package com.exchangeengine.extension;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.messaging.common.KafkaConfig;
import com.exchangeengine.messaging.consumer.KafkaConsumerService;
import com.exchangeengine.messaging.producer.KafkaProducerService;
import com.exchangeengine.service.engine.EngineDisruptorService;
import com.exchangeengine.service.engine.EngineHandler;
import com.exchangeengine.service.engine.OutputProcessor;
import com.exchangeengine.storage.StorageService;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.AccountHistoryCache;
import com.exchangeengine.storage.cache.AmmOrderCache;
import com.exchangeengine.storage.cache.AmmPoolCache;
import com.exchangeengine.storage.cache.AmmPositionCache;
import com.exchangeengine.storage.cache.BalanceLockCache;
import com.exchangeengine.storage.cache.DepositCache;
import com.exchangeengine.storage.cache.EventCache;
import com.exchangeengine.storage.cache.MerchantEscrowCache;
import com.exchangeengine.storage.cache.OfferCache;
import com.exchangeengine.storage.cache.TickBitmapCache;
import com.exchangeengine.storage.cache.TickCache;
import com.exchangeengine.storage.cache.TradeCache;
import com.exchangeengine.storage.cache.WithdrawalCache;
import com.exchangeengine.storage.rocksdb.AccountHistoryRocksDB;
import com.exchangeengine.storage.rocksdb.AccountRocksDB;
import com.exchangeengine.storage.rocksdb.AmmOrderRocksDB;
import com.exchangeengine.storage.rocksdb.AmmPoolRocksDB;
import com.exchangeengine.storage.rocksdb.AmmPositionRocksDB;
import com.exchangeengine.storage.rocksdb.BalanceLockRocksDB;
import com.exchangeengine.storage.rocksdb.DepositRocksDB;
import com.exchangeengine.storage.rocksdb.MerchantEscrowRocksDB;
import com.exchangeengine.storage.rocksdb.OfferRocksDB;
import com.exchangeengine.storage.rocksdb.RocksDBService;
import com.exchangeengine.storage.rocksdb.TickBitmapRocksDB;
import com.exchangeengine.storage.rocksdb.TickRocksDB;
import com.exchangeengine.storage.rocksdb.TradeRocksDB;
import com.exchangeengine.storage.rocksdb.WithdrawalRocksDB;
import com.exchangeengine.util.EnvManager;
import com.exchangeengine.util.logging.RollbarManager;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * JUnit extension để reset singleton instances trước và sau mỗi test
 *
 * Điều này giúp đảm bảo rằng các test không bị ảnh hưởng lẫn nhau,
 * đặc biệt quan trọng khi chạy song song trong CI/CD.
 */
public class SingletonResetExtension implements BeforeEachCallback, AfterEachCallback {
  private static final Logger logger = LoggerFactory.getLogger(SingletonResetExtension.class);
  // Sử dụng mutex để đồng bộ hóa các hoạt động reset singleton
  private static final Object SINGLETON_RESET_MUTEX = new Object();

  // Danh sách các singleton class cần reset
  private static final Set<Class<?>> SINGLETON_CLASSES = new HashSet<>(Arrays.asList(
      // Storage services
      StorageService.class,
      RocksDBService.class,
      // Cache services
      AccountCache.class,
      DepositCache.class,
      WithdrawalCache.class,
      EventCache.class,
      AccountHistoryCache.class,
      AmmPoolCache.class,
      TickCache.class,
      TickBitmapCache.class,
      AmmPositionCache.class,
      AmmOrderCache.class,
      MerchantEscrowCache.class,
      TradeCache.class,
      OfferCache.class,
      BalanceLockCache.class,
      // RocksDB services
      AccountRocksDB.class,
      DepositRocksDB.class,
      WithdrawalRocksDB.class,
      AccountHistoryRocksDB.class,
      AmmPoolRocksDB.class,
      TickRocksDB.class,
      TickBitmapRocksDB.class,
      AmmPositionRocksDB.class,
      AmmOrderRocksDB.class,
      MerchantEscrowRocksDB.class,
      TradeRocksDB.class,
      OfferRocksDB.class,
      BalanceLockRocksDB.class,
      // Kafka services
      KafkaConfig.class,
      KafkaProducerService.class,
      KafkaConsumerService.class,
      // Engine services
      EngineDisruptorService.class,
      EngineHandler.class,
      OutputProcessor.class,
      // RollbarManager
      RollbarManager.class,
      // EnvManager
      EnvManager.class));

  @Override
  public void beforeEach(ExtensionContext context) {
    synchronized (SINGLETON_RESET_MUTEX) {
      resetAllSingletons();
      // Đảm bảo luôn dọn dẹp static mocks
      MockitoStaticCleanupExtension.forceCleanupStaticMocks();
    }
  }

  @Override
  public void afterEach(ExtensionContext context) {
    synchronized (SINGLETON_RESET_MUTEX) {
      resetAllSingletons();
      // Đảm bảo luôn dọn dẹp static mocks
      MockitoStaticCleanupExtension.forceCleanupStaticMocks();
    }
  }

  /**
   * Reset tất cả các singleton để đảm bảo test độc lập
   * Phương thức tĩnh này có thể được gọi trực tiếp từ các test
   */
  public static void resetAll() {
    synchronized (SINGLETON_RESET_MUTEX) {
      // Logging thread information
      Thread currentThread = Thread.currentThread();
      logger.debug("Executing resetAll in thread {}-{}", currentThread.getId(), currentThread.getName());

      // Thử reset với cơ chế retry
      int maxRetries = 3;
      boolean allReset = false;

      for (int attempt = 0; attempt < maxRetries && !allReset; attempt++) {
        // Reset tất cả các singleton
        for (Class<?> clazz : SINGLETON_CLASSES) {
          resetSingleton(clazz);
        }

        // Kiểm tra xem tất cả đã được reset thành công chưa
        allReset = verifySingletonsReset();

        if (!allReset && attempt < maxRetries - 1) {
          logger.warn("Not all singletons were reset. Retry attempt {} of {}", attempt + 1, maxRetries);
          // Thêm một chút delay trước khi thử lại
          try {
            Thread.sleep(10);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }

      // Dọn dẹp static mocks để đảm bảo không có xung đột
      MockitoStaticCleanupExtension.forceCleanupStaticMocks();
    }
  }

  /**
   * Kiểm tra xem tất cả singleton đã được reset thành công chưa
   */
  private static boolean verifySingletonsReset() {
    try {
      for (Class<?> clazz : SINGLETON_CLASSES) {
        Field instanceField = findInstanceField(clazz);
        if (instanceField != null) {
          instanceField.setAccessible(true);
          Object instance = instanceField.get(null);
          if (instance != null) {
            logger.warn("Failed to reset singleton: {} still has an instance", clazz.getSimpleName());
            return false;
          }
        }
      }
      return true;
    } catch (Exception e) {
      logger.warn("Error verifying singletons reset: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Reset tất cả các singleton để đảm bảo test độc lập
   */
  private void resetAllSingletons() {
    resetAll();
  }

  /**
   * Reset singleton instance cho một class
   */
  private static void resetSingleton(Class<?> clazz) {
    long threadId = Thread.currentThread().getId();
    String threadName = Thread.currentThread().getName();

    try {
      // Tìm field "instance" trong class
      Field instanceField = findInstanceField(clazz);
      if (instanceField != null) {
        // Đảm bảo field có thể truy cập
        instanceField.setAccessible(true);

        // Lấy giá trị hiện tại để kiểm tra
        Object currentValue = instanceField.get(null);

        // Nếu là RollbarManager, gọi close() trước khi reset
        if (clazz == RollbarManager.class && currentValue != null) {
          try {
            RollbarManager manager = (RollbarManager) currentValue;
            manager.close();
          } catch (Exception e) {
            logger.warn("Error closing RollbarManager: {}", e.getMessage());
          }
        }

        // Reset instance về null
        instanceField.set(null, null);

        // Kiểm tra lại sau khi reset
        Object newValue = instanceField.get(null);
        if (newValue == null) {
          logger.debug("Thread {}-{}: Reset singleton successful for {}: {} -> null",
              threadId, threadName, clazz.getSimpleName(), currentValue);
        } else {
          logger.warn("Thread {}-{}: Reset singleton failed for {}: value is still {}",
              threadId, threadName, clazz.getSimpleName(), newValue);

          // Thử dùng cách mạnh hơn với force reset
          forceResetSingleton(clazz, instanceField);
        }
      } else {
        logger.warn("Thread {}-{}: Could not find 'instance' field in {}",
            threadId, threadName, clazz.getSimpleName());
      }
    } catch (Exception e) {
      logger.warn("Thread {}-{}: Could not reset singleton for {}: {}",
          threadId, threadName, clazz.getSimpleName(), e.getMessage());
    }
  }

  /**
   * Thử force reset một singleton instance khi cách thông thường không thành công
   */
  private static void forceResetSingleton(Class<?> clazz, Field instanceField) {
    try {
      // Sử dụng hacky reflection approach để reset
      Object instance = instanceField.get(null);
      if (instance != null) {
        instanceField.set(null, null);
        System.gc(); // Thúc đẩy garbage collector

        // Kiểm tra lại
        Object newValue = instanceField.get(null);
        if (newValue != null) {
          logger.warn("Force reset singleton failed for {}: value is still {}",
              clazz.getSimpleName(), newValue);
        }
      }
    } catch (Exception e) {
      logger.warn("Could not force reset singleton for {}: {}", clazz.getSimpleName(), e.getMessage());
    }
  }

  /**
   * Tìm field "instance" trong class
   */
  private static Field findInstanceField(Class<?> clazz) {
    Field[] fields = clazz.getDeclaredFields();
    for (Field field : fields) {
      // Tìm field static có tên là "instance"
      if (Modifier.isStatic(field.getModifiers()) && field.getName().equals("instance")) {
        return field;
      }
    }
    return null;
  }
}
