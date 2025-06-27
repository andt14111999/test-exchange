package com.exchangeengine.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DaemonThreadFactoryTest {

  @Test
  @DisplayName("DaemonThreadFactory nên tạo thread với tên và cấu hình chính xác - constructor cơ bản")
  void constructor_WithServiceName_ShouldCreateThreadWithCorrectNameAndConfiguration() {
    // Arrange
    String serviceName = "test-service";

    // Act
    DaemonThreadFactory factory = new DaemonThreadFactory(serviceName);

    // Assert
    assertEquals(serviceName + "-thread-", factory.getNamePrefix());
    assertEquals(0, factory.getThreadCount());

    // Test tạo thread
    Thread thread1 = factory.newThread(() -> {
    });
    Thread thread2 = factory.newThread(() -> {
    });

    // Kiểm tra thuộc tính thread
    assertNotNull(thread1);
    assertTrue(thread1.getName().startsWith(serviceName + "-thread-"));
    assertTrue(thread1.isDaemon());

    // Kiểm tra tăng bộ đếm
    assertEquals(1, Integer.parseInt(thread1.getName().substring((serviceName + "-thread-").length())));
    assertEquals(2, Integer.parseInt(thread2.getName().substring((serviceName + "-thread-").length())));
    assertEquals(2, factory.getThreadCount());
  }

  @Test
  @DisplayName("DaemonThreadFactory nên tạo thread với tên và cấu hình chính xác - constructor với threadType")
  void constructor_WithServiceNameAndThreadType_ShouldCreateThreadWithCorrectNameAndConfiguration() {
    // Arrange
    String serviceName = "test-service";
    String threadType = "worker";

    // Act
    DaemonThreadFactory factory = new DaemonThreadFactory(serviceName, threadType);

    // Assert
    assertEquals(serviceName + "-" + threadType + "-", factory.getNamePrefix());

    // Test tạo thread
    Thread thread = factory.newThread(() -> {
    });

    // Kiểm tra thuộc tính thread
    assertNotNull(thread);
    assertTrue(thread.getName().startsWith(serviceName + "-" + threadType + "-"));
    assertTrue(thread.isDaemon());
    assertEquals(1, factory.getThreadCount());
  }

  @Test
  @DisplayName("Thread được tạo phải chạy thành công Runnable được cung cấp")
  void newThread_ShouldExecuteProvidedRunnable() throws InterruptedException {
    // Arrange
    DaemonThreadFactory factory = new DaemonThreadFactory("test-service");
    final AtomicBoolean runnableExecuted = new AtomicBoolean(false);
    final CountDownLatch latch = new CountDownLatch(1);

    Runnable testRunnable = () -> {
      runnableExecuted.set(true);
      latch.countDown();
    };

    // Act
    Thread thread = factory.newThread(testRunnable);
    thread.start();

    // Assert
    assertTrue(latch.await(1, TimeUnit.SECONDS), "Latch should have counted down");
    assertTrue(runnableExecuted.get(), "Runnable should have been executed");
  }

  @Test
  @DisplayName("getDefault nên trả về instance singleton")
  void getDefault_ShouldReturnSingletonInstance() {
    // Act
    DaemonThreadFactory instance1 = DaemonThreadFactory.getDefault();
    DaemonThreadFactory instance2 = DaemonThreadFactory.getDefault();

    // Assert
    assertNotNull(instance1);
    assertSame(instance1, instance2);
    assertEquals("default-thread-", instance1.getNamePrefix());

    // Kiểm tra tạo thread
    Thread thread = instance1.newThread(() -> {
    });
    assertTrue(thread.getName().startsWith("default-thread-"));
    assertTrue(thread.isDaemon());
  }
}
