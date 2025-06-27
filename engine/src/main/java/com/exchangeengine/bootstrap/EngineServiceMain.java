package com.exchangeengine.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.messaging.consumer.KafkaConsumerQueryService;
import com.exchangeengine.messaging.consumer.KafkaConsumerService;
import com.exchangeengine.messaging.producer.KafkaProducerService;
import com.exchangeengine.service.engine.EngineHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service chính quản lý Engine Service.
 * Khởi tạo và quản lý các thành phần riêng của Engine Service.
 */
public class EngineServiceMain {
  private static final Logger logger = LoggerFactory.getLogger(EngineServiceMain.class);
  private static EngineHandler engineHandler;
  private static KafkaProducerService producerService;
  private static KafkaConsumerService consumerService;
  private static KafkaConsumerQueryService queryConsumerService;
  private static ExecutorService logicExecutorService;
  private static ExecutorService queryExecutorService;

  private static boolean isRunning = false;

  private EngineServiceMain() {
    // do nothing
  }

  /**
   * Khởi động EngineService.
   * Phương thức này được gọi từ Main.
   */
  public static synchronized void start() {
    if (isRunning) {
      logger.warn("EngineService already started");
      return;
    }

    logger.info("Starting EngineService...");

    try {
      // create required topics
      createTopics();

      // initialize EngineService
      initializeEngineService();

      isRunning = true;
      logger.info("EngineService started successfully");
    } catch (Exception e) {
      logger.error("Error starting EngineService: {}", e.getMessage(), e);
      throw new RuntimeException("Cannot start EngineService", e);
    }
  }

  /**
   * Create required topics for EngineService.
   */
  private static void createTopics() {
    logger.info("Creating topics for EngineService...");

    // create topics using ServiceInitializer
    ServiceInitializer.createTopics();

    logger.info("Finished creating topics for EngineService");
  }

  /**
   * Initialize EngineService.
   */
  private static void initializeEngineService() {
    logger.info("Initializing EngineService...");

    // initialize Engine Service using Singleton Pattern
    engineHandler = EngineHandler.getInstance();
    logger.info("Engine Handler initialized");

    // create Kafka Producer for EngineService, using shared producer
    producerService = KafkaProducerService.getInstance();
    logger.info("Kafka Producer for Engine Handler initialized");

    // Khởi tạo KafkaConsumerService cho logic events (luồng chính)
    consumerService = KafkaConsumerService.getInstance();
    logger.info("Kafka Consumer for logic events initialized");

    // Khởi tạo KafkaConsumerQueryService cho query (luồng riêng)
    queryConsumerService = KafkaConsumerQueryService.getInstance();
    int queryThreads = queryConsumerService.getQueryThreadsCount();
    logger.info("Kafka Consumer for queries initialized with {} threads", queryThreads);

    // Start logic consumer trong một thread riêng (Single Thread)
    logicExecutorService = Executors.newSingleThreadExecutor();
    logicExecutorService.submit(consumerService);
    logger.info("Logic Kafka Consumer started in a separate thread");

    // Start query consumer trong một thread riêng
    queryExecutorService = Executors.newSingleThreadExecutor();
    queryExecutorService.submit(queryConsumerService);
    logger.info("Query Kafka Consumer started in a separate thread with {} worker threads", queryThreads);
  }

  /**
   * Lấy instance của EngineHandler.
   */
  public static EngineHandler getEngineHandler() {
    return engineHandler;
  }

  /**
   * Lấy instance của KafkaProducerService.
   */
  public static KafkaProducerService getProducerService() {
    return producerService;
  }

  /**
   * Dừng EngineService.
   * Phương thức này được gọi từ Main.
   */
  public static synchronized void stop() {
    if (!isRunning) {
      logger.warn("EngineService is not running or already stopped");
      return;
    }

    logger.info("Stopping EngineService...");

    try {
      // Dừng các thành phần theo thứ tự ngược lại
      if (consumerService != null) {
        consumerService.shutdown();
        logger.info("Logic Kafka Consumer shutdown signal sent");
      }

      if (queryConsumerService != null) {
        queryConsumerService.shutdown();
        logger.info("Query Kafka Consumer shutdown signal sent");
      }

      if (logicExecutorService != null) {
        logicExecutorService.shutdown();
        if (logicExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
          logger.info("Logic ExecutorService stopped");
        } else {
          logger.warn("Logic ExecutorService force shutdown");
          logicExecutorService.shutdownNow();
        }
      }

      if (queryExecutorService != null) {
        queryExecutorService.shutdown();
        if (queryExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
          logger.info("Query ExecutorService stopped");
        } else {
          logger.warn("Query ExecutorService force shutdown");
          queryExecutorService.shutdownNow();
        }
      }

      if (producerService != null) {
        producerService.close();
        logger.info("Kafka Producer stopped");
      }

      logger.info("EngineService stopped successfully");
    } catch (Exception e) {
      logger.error("Error stopping EngineService: {}", e.getMessage(), e);
    } finally {
      isRunning = false;
    }
  }

  /**
   * Thiết lập instance test cho EngineServiceMain. Chỉ dùng trong môi trường
   * test.
   *
   * @param testInstance Instance test để thiết lập. Do EngineServiceMain không có
   *                     instance
   *                     (tất cả phương thức là static), tham số này chỉ đóng vai
   *                     trò như một
   *                     "mock" để viết test thống nhất.
   */
  public static void setTestInstance(EngineServiceMain testInstance) {
    // Không thực sự lưu trữ instance, vì EngineServiceMain là một Utility class
    // Nhưng giả vờ làm vậy để các test có thể làm việc với nó
  }
}
