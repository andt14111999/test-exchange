package com.exchangeengine.extension;

import com.exchangeengine.util.EnvManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension JUnit để tự động xóa RocksDB trước các test
 *
 * Cách sử dụng:
 * 1. Cho từng test class: @ExtendWith(RocksDBTestExtension.class)
 * 2. Globally: Đã được bật trong file junit-platform.properties
 */
public class RocksDBTestExtension implements BeforeAllCallback, BeforeEachCallback {
  private static final Logger logger = LoggerFactory.getLogger(RocksDBTestExtension.class);

  private final boolean clearBeforeEachTest;

  /**
   * Constructor mặc định - clear RocksDB trước mỗi test case
   */
  public RocksDBTestExtension() {
    this(true);
  }

  /**
   * Constructor với tùy chọn clear trước mỗi test
   *
   * @param clearBeforeEachTest true nếu muốn clear trước mỗi test, false nếu chỉ
   *                            clear trước khi bắt đầu class
   */
  public RocksDBTestExtension(boolean clearBeforeEachTest) {
    this.clearBeforeEachTest = clearBeforeEachTest;
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    if (!clearBeforeEachTest) {
      logger.info("Clearing RocksDB before all tests in {}", context.getDisplayName());
      clearTestDatabase();
    }
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    if (clearBeforeEachTest) {
      logger.info("Clearing RocksDB before test: {}", context.getDisplayName());
      clearTestDatabase();
    }
  }

  /**
   * Thiết lập môi trường test và xóa thư mục dữ liệu test
   */
  public static void clearTestDatabase() {
    EnvManager.setTestEnvironment();
    String testDataDir = EnvManager.getInstance().get("ROCKSDB_DATA_DIR", "./data/rocksdb/test");
    clearDirectory(testDataDir);
  }

  /**
   * Xóa toàn bộ nội dung của một thư mục
   *
   * @param directoryPath Đường dẫn đến thư mục cần xóa
   */
  public static void clearDirectory(String directoryPath) {
    Path directory = Paths.get(directoryPath);
    try {
      // Tạo thư mục nếu chưa tồn tại
      if (!Files.exists(directory)) {
        Files.createDirectories(directory);
        logger.info("Đã tạo thư mục: {}", directoryPath);
        return;
      }

      // Xóa tất cả các file và thư mục trong ./data/rocksdb/test/*
      String osName = System.getProperty("os.name").toLowerCase();
      Process process;

      if (osName.contains("windows")) {
        // Windows
        process = Runtime.getRuntime().exec("cmd /c del /s /q \"" + directoryPath + "\\*\"");
      } else {
        // Unix/Linux/Mac
        process = Runtime.getRuntime().exec(new String[] { "sh", "-c", "rm -rf \"" + directoryPath + "\"/*" });
      }

      int exitCode = process.waitFor();
      if (exitCode == 0) {
        logger.info("Đã xóa nội dung thư mục: {}", directoryPath);
      } else {
        logger.warn("Không thể xóa nội dung thư mục (mã lỗi: {}): {}", exitCode, directoryPath);
      }
    } catch (Exception e) {
      logger.error("Lỗi khi xóa nội dung thư mục {}: {}", directoryPath, e.getMessage());
    }
  }
}
