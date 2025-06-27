package com.exchangeengine.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ThreadFactory tạo daemon thread với tên có ý nghĩa.
 * Factory này có thể được sử dụng bởi nhiều service khác nhau cần tạo daemon
 * thread.
 */
public class DaemonThreadFactory implements ThreadFactory {
  private final String namePrefix;
  private final AtomicInteger threadCounter = new AtomicInteger(1);

  // Singleton pattern với lazy initialization
  private static volatile DaemonThreadFactory defaultInstance;

  /**
   * Khởi tạo ThreadFactory mặc định.
   *
   * @return ThreadFactory mặc định
   */
  public static synchronized DaemonThreadFactory getDefault() {
    if (defaultInstance == null) {
      defaultInstance = new DaemonThreadFactory("default");
    }
    return defaultInstance;
  }

  /**
   * Tạo một ThreadFactory mới với tiền tố tên được chỉ định.
   *
   * @param serviceName Tên dịch vụ sẽ được sử dụng làm tiền tố cho tên các thread
   */
  public DaemonThreadFactory(String serviceName) {
    this.namePrefix = serviceName + "-thread-";
  }

  /**
   * Tạo một ThreadFactory mới với tiền tố tên và loại được chỉ định.
   *
   * @param serviceName Tên dịch vụ sẽ được sử dụng làm tiền tố cho tên các thread
   * @param threadType  Loại thread (ví dụ: "worker", "disruptor", "event")
   */
  public DaemonThreadFactory(String serviceName, String threadType) {
    this.namePrefix = serviceName + "-" + threadType + "-";
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread thread = new Thread(r);
    thread.setName(namePrefix + threadCounter.getAndIncrement());
    thread.setDaemon(true);
    return thread;
  }

  /**
   * Lấy tiền tố tên của ThreadFactory này.
   *
   * @return tiền tố tên
   */
  public String getNamePrefix() {
    return namePrefix;
  }

  /**
   * Lấy số lượng thread đã được tạo bởi factory này.
   *
   * @return số lượng thread đã tạo
   */
  public int getThreadCount() {
    return threadCounter.get() - 1;
  }
}
