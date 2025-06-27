package com.exchangeengine.util.logging;

import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import com.exchangeengine.util.EnvManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;

/**
 * Quản lý Rollbar cho ứng dụng.
 * Cung cấp phương thức tiện ích để gửi thông báo đến Rollbar.
 */
public class RollbarManager {
  private static final Logger logger = LoggerFactory.getLogger(RollbarManager.class);
  private static final EnvManager envManager = EnvManager.getInstance();
  private static volatile RollbarManager instance;
  private final Rollbar rollbar;

  /**
   * Constructor riêng tư để đảm bảo Singleton.
   */
  private RollbarManager() {
    String accessToken = envManager.get("ROLLBAR_ACCESS_TOKEN", "");
    String environment = envManager.isProduction() ? "production" : "development";

    Config config = ConfigBuilder.withAccessToken(accessToken)
        .environment(environment)
        .codeVersion("1.0.0")
        .build();

    rollbar = Rollbar.init(config);
    logger.info("RollbarManager đã được khởi tạo với môi trường {}", environment);
  }

  /**
   * Lấy instance của RollbarManager (Singleton pattern).
   *
   * @return Instance của RollbarManager
   */
  public static synchronized RollbarManager getInstance() {
    if (instance == null) {
      instance = new RollbarManager();
    }
    return instance;
  }

  /**
   * Gửi thông báo lỗi đến Rollbar.
   *
   * @param throwable Lỗi cần gửi
   * @param message   Thông báo kèm theo
   */
  public void error(Throwable throwable, String message) {
    try {
      rollbar.error(throwable, createCustomData(), message);
    } catch (Exception e) {
      // Bắt exception từ Rollbar API để tránh retry
      System.err.println("Rollbar error failed: " + e.getMessage());
    }
  }

  /**
   * Gửi thông báo cảnh báo đến Rollbar.
   *
   * @param message Thông báo cảnh báo
   */
  public void warning(String message) {
    try {
      rollbar.warning(message, createCustomData());
    } catch (Exception e) {
      // Bắt exception từ Rollbar API để tránh retry
      System.err.println("Rollbar warning failed: " + e.getMessage());
    }
  }

  /**
   * Gửi thông báo critical đến Rollbar.
   *
   * @param throwable Lỗi nghiêm trọng cần gửi
   * @param message   Thông báo kèm theo
   */
  public void critical(Throwable throwable, String message) {
    try {
      rollbar.critical(throwable, createCustomData(), message);
    } catch (Exception e) {
      // Bắt exception từ Rollbar API để tránh retry
      System.err.println("Rollbar critical failed: " + e.getMessage());
    }
  }

  /**
   * Tạo dữ liệu tùy chỉnh cho các thông báo Rollbar.
   *
   * @return Map chứa dữ liệu tùy chỉnh
   */
  private Map<String, Object> createCustomData() {
    Map<String, Object> custom = new HashMap<>();
    custom.put("environment", envManager.getEnvironment());
    custom.put("is_production", envManager.isProduction());
    return custom;
  }

  /**
   * Đóng kết nối Rollbar.
   */
  public void close() {
    try {
      rollbar.close(true);
    } catch (Exception e) {
      // Không log error khi đóng vì app đang tắt
      System.err.println("Lỗi khi đóng kết nối Rollbar: " + e.getMessage());
    }
  }
}
