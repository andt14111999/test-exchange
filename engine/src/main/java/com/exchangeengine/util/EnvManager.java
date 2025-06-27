package com.exchangeengine.util;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quản lý biến môi trường cho ứng dụng.
 * Lớp này sử dụng dotenv-java để đọc biến môi trường từ file .env
 * và cung cấp các phương thức để truy cập các giá trị.
 */
public class EnvManager {
  private static final Logger logger = LoggerFactory.getLogger(EnvManager.class);
  private static EnvManager instance;
  private final Dotenv dotenv;
  private final String environment;

  /**
   * Khởi tạo EnvManager và đọc biến môi trường.
   */
  private EnvManager() {
    // Kiểm tra xem có đang chạy trong môi trường test không
    boolean isTestEnv = System.getProperty("isTest") != null ||
        "test".equalsIgnoreCase(System.getProperty("APP_ENV"));

    if (isTestEnv) {
      // Tạo và cấu hình Dotenv cho môi trường test
      dotenv = Dotenv.configure()
          .directory("src/test/java/com/exchangeengine/config") // Thư mục chứa file .env.test
          .filename(".env.test")
          .ignoreIfMissing() // Không báo lỗi nếu không tìm thấy file .env.test
          .systemProperties() // Tự động nạp vào System properties
          .load();

      environment = "test";
      logger.info("Đã tải biến môi trường cho TEST. Môi trường hiện tại: {}", environment);
    } else {
      // Tạo và cấu hình Dotenv cho môi trường thông thường
      dotenv = Dotenv.configure()
          .directory("./config") // Thư mục chứa file .env
          .filename(".env")
          .ignoreIfMissing() // Không báo lỗi nếu không tìm thấy file .env
          .systemProperties() // Tự động nạp vào System properties
          .load();

      // Xác định môi trường
      environment = get("APP_ENV", "development");
      logger.info("Đã tải biến môi trường. Môi trường hiện tại: {}", environment);
    }
  }

  /**
   * Lấy instance của EnvManager (Singleton pattern).
   *
   * @return Instance của EnvManager
   */
  public static synchronized EnvManager getInstance() {
    if (instance == null) {
      instance = new EnvManager();
    }
    return instance;
  }

  /**
   * Thiết lập môi trường test để JUnit tests có thể sử dụng
   */
  public static void setTestEnvironment() {
    System.setProperty("isTest", "true");
    System.setProperty("APP_ENV", "test");

    // Reset instance nếu đã tồn tại
    if (instance != null) {
      instance = null;
    }
  }

  /**
   * Thiết lập instance test cho EnvManager. Chỉ dùng trong môi trường test.
   *
   * @param testInstance Instance test để thiết lập
   */
  public static void setTestInstance(EnvManager testInstance) {
    instance = testInstance;
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
   * @return Môi trường hiện tại (dev, staging, prod)
   */
  public String getEnvironment() {
    return environment;
  }

  /**
   * Kiểm tra xem có đang chạy trong môi trường development không.
   *
   * @return true nếu đang chạy trong môi trường development
   */
  public boolean isDevelopment() {
    return "development".equals(environment);
  }

  /**
   * Kiểm tra xem có đang chạy trong môi trường test không.
   *
   * @return true nếu đang chạy trong môi trường test
   */
  public boolean isTest() {
    return "test".equals(environment);
  }

  /**
   * Kiểm tra xem có đang chạy trong môi trường staging không.
   *
   * @return true nếu đang chạy trong môi trường staging
   */
  public boolean isStaging() {
    return "staging".equals(environment);
  }

  /**
   * Kiểm tra xem có đang chạy trong môi trường production không.
   *
   * @return true nếu đang chạy trong môi trường production
   */
  public boolean isProduction() {
    return "production".equals(environment);
  }
}
