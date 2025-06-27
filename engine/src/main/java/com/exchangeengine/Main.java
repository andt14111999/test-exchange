package com.exchangeengine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.bootstrap.EngineServiceMain;
import com.exchangeengine.bootstrap.ServiceInitializer;
import com.exchangeengine.util.EnvManager;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;

/**
 * Main class of application.
 * Initialize common components and services.
 */
public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  // EnvManager to manage environment variables
  private static final EnvManager envManager = EnvManager.getInstance();

  // Lấy thông tin version từ Maven
  public static final String VERSION = Main.class.getPackage().getImplementationVersion();

  // Định dạng thời gian
  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  // Lấy thời gian hiện tại theo múi giờ UTC+7
  private static String getCurrentTimeUTC7() {
    ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC+7"));
    return now.format(formatter);
  }

  /**
   * Default constructor.
   */
  public Main() {
    // Empty constructor
  }

  public static void main(String[] args) {
    // Hiển thị thông tin phiên bản và thời gian khi khởi động
    String currentTime = getCurrentTimeUTC7();
    logger.info("======================= ENVIRONMENT: {} - VERSION: {} - TIME: {}=========================",
        envManager.getEnvironment(), VERSION, currentTime);

    try {
      // Initialize common components
      ServiceInitializer.initialize();
      logger.info("Initialized ServiceInitializer with all common components");

      // Start services
      startServices();
      logger.info("========================*.*========================");

      // Use CountDownLatch to keep application running
      CountDownLatch latch = new CountDownLatch(1);

      // Thêm shutdown hook
      setupShutdownHook(latch);

      logger.info("Hệ thống đã khởi động thành công vào lúc {}. Nhấn Ctrl+C để dừng.", currentTime);

      try {
        latch.await(); // wait until shutdown signal is received
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.warn("Main thread interrupted", e);
      }

    } catch (Exception e) {
      logger.error("Lỗi khi khởi động hệ thống: {}", e.getMessage(), e);
    }

    logger.info("Hệ thống đã dừng vào lúc {}", getCurrentTimeUTC7());
  }

  /**
   * Khởi động các service.
   */
  private static void startServices() {
    // start BalanceService
    EngineServiceMain.start();
    logger.info("EngineService started");

    // start other services in the future
    // PoolServiceMain.start();
  }

  /**
   * Thiết lập shutdown hook.
   */
  private static void setupShutdownHook(CountDownLatch latch) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      String stopTime = getCurrentTimeUTC7();
      logger.info("Đang dừng hệ thống Exchange Engine v{} vào lúc {}...", VERSION != null ? VERSION : "Dev", stopTime);

      try {
        // Dừng các service theo thứ tự ngược lại
        EngineServiceMain.stop();
        logger.info("Đã dừng EngineService");

        // Dừng các service khác trong tương lai
        // PoolServiceMain.stop();

        // Dừng ServiceInitializer (bao gồm RocksDB, Disruptor, Kafka)
        ServiceInitializer.shutdown();
        logger.info("Đã dừng ServiceInitializer");

      } catch (Exception e) {
        logger.error("Lỗi khi dừng hệ thống: {}", e.getMessage(), e);
      } finally {
        latch.countDown();
      }
    }));
  }
}
