package com.exchangeengine.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.messaging.common.KafkaConfig;
import com.exchangeengine.messaging.producer.KafkaProducerService;
import com.exchangeengine.service.engine.EngineDisruptorService;
import com.exchangeengine.service.engine.EngineHandler;
import com.exchangeengine.service.engine.OutputProcessor;
import com.exchangeengine.messaging.consumer.KafkaConsumerService;
import com.exchangeengine.storage.rocksdb.RocksDBService;
import com.exchangeengine.storage.StorageService;

/**
 * Lớp này quản lý việc khởi tạo các thành phần dùng chung trong hệ thống.
 * Các service khác có thể sử dụng các thành phần này thông qua các phương thức
 * static.
 */
public class ServiceInitializer {
  private static final Logger logger = LoggerFactory.getLogger(ServiceInitializer.class);
  private static boolean initialized = false;

  /**
   * Private constructor để ngăn chặn việc tạo instance.
   */
  private ServiceInitializer() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  /**
   * Initialize common components.
   */
  public static synchronized void initialize() {
    if (initialized) {
      logger.warn("ServiceInitializer already initialized");
      return;
    }

    logger.info("Initializing common components...");

    try {
      initializeStorage();
      initializeKafka();
      initializeAllSingletons();
      initializeDisruptor();
      initialized = true;

      logger.info("Initialized common components");
    } catch (Exception e) {
      initialized = false;
      logger.error("Error initializing common components: {}", e.getMessage(), e);
      throw new RuntimeException("Không thể khởi tạo các thành phần dùng chung", e);
    }
  }

  /**
   * Khởi tạo tất cả các instance Singleton.
   */
  private static void initializeAllSingletons() {
    logger.info("Initializing all Singleton instances...");

    // Khởi tạo KafkaProducerService
    KafkaProducerService.getInstance();
    logger.info("Initialized KafkaProducerService");

    // Khởi tạo OutputProcessor
    OutputProcessor.getInstance();
    logger.info("Initialized OutputProcessor");

    // Khởi tạo EngineHandler
    EngineHandler.getInstance();
    logger.info("Initialized EngineHandler");

    // Khởi tạo KafkaConsumerService
    KafkaConsumerService.getInstance();
    logger.info("Initialized KafkaConsumerService");

    logger.info("All Singleton instances initialized");
  }

  /**
   * Initialize RocksDB and StorageService.
   */
  private static void initializeStorage() {
    RocksDBService.getInstance();
    logger.info("Initialized RocksDB");

    StorageService.getInstance();
    logger.info("Initialized StorageService");
  }

  /**
   * Initialize Disruptor.
   */
  private static void initializeDisruptor() {
    logger.info("Initializing Disruptor...");

    try {
      EngineDisruptorService.getInstance();
      logger.info("Initialized EngineDisruptorService");
    } catch (Exception e) {
      logger.error("Error initializing Disruptor: {}", e.getMessage(), e);
      throw new RuntimeException("Cannot initialize Disruptor", e);
    }
  }

  /**
   * Khởi tạo Kafka.
   */
  private static void initializeKafka() {
    logger.info("Initializing Kafka...");

    // Khởi tạo KafkaConfig - logic khởi tạo đã được đặt trong constructor
    KafkaConfig.getInstance();
    logger.info("Initialized KafkaConfig");
  }

  /**
   * Create Kafka topic if it does not exist.
   */
  public static void createTopics() {
    KafkaConfig.getInstance().createTopics();
  }

  /**
   * Shutdown all components.
   */
  public static synchronized void shutdown() {
    if (!initialized) {
      logger.warn("ServiceInitializer is not initialized or already shutdown");
      return;
    }

    logger.info("Shutting down all components...");

    // Shutdown components in reverse order of initialization
    shutdownDisruptor();
    shutdownKafka();
    shutdownRocksDB();

    initialized = false;
    logger.info("All components shutdown successfully");
  }

  /**
   * Shutdown Kafka.
   */
  private static void shutdownKafka() {
    logger.info("Shutting down Kafka components...");

    try {
      // Shutdown KafkaConsumerService
      KafkaConsumerService.getInstance().shutdown();
      logger.info("Shutdown KafkaConsumerService");
    } catch (Exception e) {
      logger.error("Error shutting down KafkaConsumerService: {}", e.getMessage(), e);
    }

    try {
      // Shutdown KafkaProducerService
      KafkaProducerService.getInstance().close();
      logger.info("Shutdown KafkaProducerService");
    } catch (Exception e) {
      logger.error("Error shutting down KafkaProducerService: {}", e.getMessage(), e);
    }

    try {
      // Shutdown KafkaConfig
      KafkaConfig.getInstance().shutdown();
      logger.info("Shutdown KafkaConfig");
    } catch (Exception e) {
      logger.error("Error shutting down KafkaConfig: {}", e.getMessage(), e);
    }
  }

  /**
   * Shutdown Disruptor.
   */
  private static void shutdownDisruptor() {
    logger.info("Shutting down Disruptor...");

    try {
      // Shutdown EngineDisruptorService
      EngineDisruptorService.getInstance().shutdown();
      logger.info("Shutdown Disruptor");

      // Shutdown OutputProcessor
      OutputProcessor.getInstance().shutdown();
      logger.info("Shutdown OutputProcessor");
    } catch (Exception e) {
      logger.error("Error shutting down Disruptor components: {}", e.getMessage(), e);
    }
  }

  /**
   * Shutdown RocksDB and StorageService.
   */
  private static void shutdownRocksDB() {
    logger.info("Shutting down StorageService and RocksDB...");

    try {
      StorageService.getInstance().shutdown();
      logger.info("Shutdown StorageService");
    } catch (Exception e) {
      logger.error("Error shutting down StorageService: {}", e.getMessage(), e);
    }

    try {
      RocksDBService.getInstance().close();
      logger.info("Đã đóng RocksDB");
    } catch (Exception e) {
      logger.error("Error shutting down RocksDB: {}", e.getMessage(), e);
    }
  }

  /**
   * Thiết lập instance test cho ServiceInitializer. Chỉ dùng trong môi trường
   * test.
   *
   * @param testInstance Instance test để thiết lập. Do ServiceInitializer không
   *                     có instance
   *                     (tất cả phương thức là static), tham số này chỉ đóng vai
   *                     trò như một
   *                     "mock" để viết test thống nhất.
   */
  public static void setTestInstance(ServiceInitializer testInstance) {
    // Không thực sự lưu trữ instance, vì ServiceInitializer là một Utility class
    // Nhưng giả vờ làm vậy để các test có thể làm việc với nó
  }
}
