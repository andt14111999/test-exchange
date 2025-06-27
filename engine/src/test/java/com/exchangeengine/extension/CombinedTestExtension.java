package com.exchangeengine.extension;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension JUnit kết hợp SingletonResetExtension và
 * MockitoStaticCleanupExtension.
 *
 * Mục đích:
 * - Đảm bảo thứ tự đúng khi reset singleton và cleanup static mocks
 * - Tránh vấn đề phụ thuộc thứ tự khi các extension được đăng ký riêng lẻ
 * - Đơn giản hóa cách sử dụng bằng cách chỉ cần đăng ký một extension
 *
 * Cách sử dụng:
 *
 * @ExtendWith({MockitoExtension.class, CombinedTestExtension.class})
 *                                      thay vì
 * @ExtendWith({MockitoExtension.class, SingletonResetExtension.class,
 *                                      MockitoStaticCleanupExtension.class})
 */
public class CombinedTestExtension implements BeforeEachCallback, AfterEachCallback {
  private static final Logger logger = LoggerFactory.getLogger(CombinedTestExtension.class);

  // Bước 1: Đồng bộ hóa chung cho tất cả hoạt động
  private static final Object GLOBAL_EXTENSION_MUTEX = new Object();

  // Các extension con
  private final SingletonResetExtension singletonResetExtension = new SingletonResetExtension();
  private final MockitoStaticCleanupExtension mockitoStaticCleanupExtension = new MockitoStaticCleanupExtension();

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    synchronized (GLOBAL_EXTENSION_MUTEX) {
      logger.debug("CombinedTestExtension.beforeEach - Start");

      // Bước 2: Đầu tiên cleanup static mocks
      mockitoStaticCleanupExtension.beforeEach(context);

      // Bước 3: Sau đó reset singleton instances
      singletonResetExtension.beforeEach(context);

      // Bước 4: Cuối cùng cleanup lại static mocks một lần nữa
      mockitoStaticCleanupExtension.beforeEach(context);

      logger.debug("CombinedTestExtension.beforeEach - Complete");
    }
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    synchronized (GLOBAL_EXTENSION_MUTEX) {
      logger.debug("CombinedTestExtension.afterEach - Start");

      // Cũng tương tự như beforeEach, nhưng theo thứ tự ngược lại

      // Bước 2: Đầu tiên cleanup static mocks
      mockitoStaticCleanupExtension.afterEach(context);

      // Bước 3: Sau đó reset singleton instances
      singletonResetExtension.afterEach(context);

      // Bước 4: Cuối cùng cleanup lại static mocks một lần nữa
      mockitoStaticCleanupExtension.afterEach(context);

      logger.debug("CombinedTestExtension.afterEach - Complete");
    }
  }

  /**
   * Reset tất cả các singleton và dọn dẹp static mocks
   * Phương thức tĩnh này có thể được gọi trực tiếp từ các test
   */
  public static void resetAllInOrder() {
    synchronized (GLOBAL_EXTENSION_MUTEX) {
      logger.debug("CombinedTestExtension.resetAllInOrder - Start");

      // Step 1: Đầu tiên cleanup static mocks
      MockitoStaticCleanupExtension.forceCleanupStaticMocks();

      // Step 2: Sau đó reset singleton instances
      SingletonResetExtension.resetAll();

      // Step 3: Cuối cùng cleanup lại static mocks một lần nữa
      MockitoStaticCleanupExtension.forceCleanupStaticMocks();

      logger.debug("CombinedTestExtension.resetAllInOrder - Complete");
    }
  }
}
