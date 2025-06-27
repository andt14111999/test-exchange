package com.exchangeengine.extension;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension JUnit để đảm bảo không có static mocking được đăng ký giữa các test
 *
 * Vấn đề: "static mocking is already registered in the current thread"
 * Giải pháp: Tự động dọn dẹp các static mock trước và sau mỗi test
 *
 * Cải tiến: Làm việc tốt hơn trong CI/CD nơi các test có thể chạy song song
 */
public class MockitoStaticCleanupExtension implements BeforeEachCallback, AfterEachCallback {
  private static final Logger logger = LoggerFactory.getLogger(MockitoStaticCleanupExtension.class);
  // Mutex giúp xử lý truy cập song song vào Mockito's static configuration
  private static final Object STATIC_MOCK_MUTEX = new Object();

  @Override
  public void beforeEach(ExtensionContext context) {
    synchronized (STATIC_MOCK_MUTEX) {
      cleanupStaticMocks();
    }
  }

  @Override
  public void afterEach(ExtensionContext context) {
    synchronized (STATIC_MOCK_MUTEX) {
      cleanupStaticMocks();
    }
  }

  /**
   * Dọn dẹp các static mock đã đăng ký
   */
  private void cleanupStaticMocks() {
    try {
      // Truy cập vào lớp MockingProgress để lấy dữ liệu về static mocks
      Class<?> threadLocalMockingProgressClass = Class
          .forName("org.mockito.internal.progress.ThreadLocalMockingProgress");
      Field instanceField = threadLocalMockingProgressClass.getDeclaredField("INSTANCE");
      instanceField.setAccessible(true);
      Object mockingProgress = instanceField.get(null);

      // Truy cập vào mockingProgress để lấy field staticMockRegistry
      Class<?> mockingProgressClass = Class.forName("org.mockito.internal.progress.MockingProgressImpl");
      Field staticMockRegistryField = mockingProgressClass.getDeclaredField("staticMockRegistry");
      staticMockRegistryField.setAccessible(true);
      Object staticMockRegistry = staticMockRegistryField.get(mockingProgress);

      // Truy cập vào staticMockRegistry để lấy field mocks
      Class<?> staticMockRegistryClass = Class
          .forName("org.mockito.internal.configuration.plugins.DefaultStaticMockRegistry");
      Field mocksField = staticMockRegistryClass.getDeclaredField("mocks");
      mocksField.setAccessible(true);
      Map<Class<?>, Object> mocks = (Map<Class<?>, Object>) mocksField.get(staticMockRegistry);

      // Lấy thread ID hiện tại để ghi log rõ ràng hơn trong môi trường CI
      long threadId = Thread.currentThread().getId();
      String threadName = Thread.currentThread().getName();

      if (!mocks.isEmpty()) {
        int size = mocks.size();
        logger.info("Thread {}-{}: Cleaning up {} static mocks", threadId, threadName, size);
        mocks.clear();

        // Đợi một chút để đảm bảo công tác dọn dẹp hoàn tất
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        // Kiểm tra lại việc dọn dẹp
        if (!mocks.isEmpty()) {
          logger.warn("Thread {}-{}: Failed to clean up static mocks, still have {} mocks",
              threadId, threadName, mocks.size());
        }
      }
    } catch (Exception e) {
      logger.warn("Could not clean up static mocks: {}", e.getMessage());
    }
  }

  /**
   * Phương thức tĩnh để dọn dẹp mock bên ngoài lifecycle của extension
   */
  public static void forceCleanupStaticMocks() {
    synchronized (STATIC_MOCK_MUTEX) {
      try {
        Class<?> threadLocalMockingProgressClass = Class
            .forName("org.mockito.internal.progress.ThreadLocalMockingProgress");
        Field instanceField = threadLocalMockingProgressClass.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        Object mockingProgress = instanceField.get(null);

        Class<?> mockingProgressClass = Class.forName("org.mockito.internal.progress.MockingProgressImpl");
        Field staticMockRegistryField = mockingProgressClass.getDeclaredField("staticMockRegistry");
        staticMockRegistryField.setAccessible(true);
        Object staticMockRegistry = staticMockRegistryField.get(mockingProgress);

        Class<?> staticMockRegistryClass = Class
            .forName("org.mockito.internal.configuration.plugins.DefaultStaticMockRegistry");
        Field mocksField = staticMockRegistryClass.getDeclaredField("mocks");
        mocksField.setAccessible(true);
        Map<Class<?>, Object> mocks = (Map<Class<?>, Object>) mocksField.get(staticMockRegistry);

        mocks.clear();

        logger.info("Force cleanup static mocks in thread {}", Thread.currentThread().getName());
      } catch (Exception e) {
        logger.warn("Could not force clean up static mocks: {}", e.getMessage());
      }
    }
  }
}
