package com.exchangeengine.util;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp quản lý biến môi trường cho môi trường test.
 * Đọc cấu hình từ file .env.test trong thư mục
 * test/java/com/exchangeengine/config
 */
public class TestEnvManager {
  private static final Logger logger = LoggerFactory.getLogger(TestEnvManager.class);
  private static TestEnvManager instance;
  private final Dotenv dotenv;
  private final String environment;

  /**
   * Khởi tạo TestEnvManager và đọc biến môi trường.
   */
  private TestEnvManager() {
    // Tạo và cấu hình Dotenv cho môi trường test
    dotenv = Dotenv.configure()
        .directory("src/test/java/com/exchangeengine/config") // Thư mục chứa file .env.test
        .filename(".env.test")
        .ignoreIfMissing() // Không báo lỗi nếu không tìm thấy file .env.test
        .systemProperties() // Tự động nạp vào System properties
        .load();

    // Xác định môi trường
    environment = get("APP_ENV", "test");

    logger.info("Đã tải biến môi trường test. Môi trường hiện tại: {}", environment);
  }

  /**
   * Lấy instance của TestEnvManager (Singleton pattern).
   *
   * @return Instance của TestEnvManager
   */
  public static synchronized TestEnvManager getInstance() {
    if (instance == null) {
      instance = new TestEnvManager();
    }
    return instance;
  }

  /**
   * Lấy giá trị biến môi trường.
   *
   * @param key Tên biến môi trường
   * @return Giá trị biến môi trường hoặc null nếu không tìm thấy
   */
  public String get(String key) {
    return dotenv.get(key);
  }

  /**
   * Lấy giá trị biến môi trường với giá trị mặc định.
   *
   * @param key          Tên biến môi trường
   * @param defaultValue Giá trị mặc định nếu không tìm thấy biến
   * @return Giá trị biến môi trường hoặc giá trị mặc định
   */
  public String get(String key, String defaultValue) {
    return dotenv.get(key, defaultValue);
  }

  /**
   * Lấy giá trị biến môi trường dưới dạng int.
   *
   * @param key          Tên biến môi trường
   * @param defaultValue Giá trị mặc định nếu không tìm thấy biến hoặc không thể
   *                     chuyển đổi
   * @return Giá trị biến môi trường dưới dạng int hoặc giá trị mặc định
   */
  public int getInt(String key, int defaultValue) {
    String value = get(key);
    if (value == null) {
      return defaultValue;
    }

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      logger.warn("Không thể chuyển đổi biến {} thành số nguyên: {}", key, value);
      return defaultValue;
    }
  }

  /**
   * Lấy giá trị biến môi trường dưới dạng boolean.
   *
   * @param key          Tên biến môi trường
   * @param defaultValue Giá trị mặc định nếu không tìm thấy biến
   * @return Giá trị biến môi trường dưới dạng boolean hoặc giá trị mặc định
   */
  public boolean getBoolean(String key, boolean defaultValue) {
    String value = get(key);
    if (value == null) {
      return defaultValue;
    }

    return Boolean.parseBoolean(value);
  }

  /**
   * Lấy môi trường hiện tại.
   *
   * @return Môi trường hiện tại (luôn là "test")
   */
  public String getEnvironment() {
    return environment;
  }

  /**
   * Trả về true vì luôn đang chạy trong môi trường test.
   *
   * @return true
   */
  public boolean isTest() {
    return true;
  }
}
