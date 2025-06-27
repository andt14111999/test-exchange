package com.exchangeengine.storage.cache;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Cache service cho Event
 * Sử dụng Singleton pattern để đảm bảo chỉ có một instance duy nhất
 */
public class EventCache {
  private static volatile EventCache instance;

  private static final int CACHE_EXPIRATION_DAYS = 2; // 2 days

  private final Cache<String, Boolean> eventCache = CacheBuilder.newBuilder()
      .expireAfterWrite(CACHE_EXPIRATION_DAYS, TimeUnit.DAYS)
      .build();

  /**
   * Lấy instance của EventCache.
   *
   * @return Instance của EventCache
   */
  public static synchronized EventCache getInstance() {
    if (instance == null) {
      instance = new EventCache();
    }
    return instance;
  }

  /**
   * Constructor riêng tư để đảm bảo Singleton pattern
   */
  private EventCache() {
  }

  /**
   * Thiết lập instance cho mục đích testing.
   * CHỈ SỬ DỤNG TRONG UNIT TEST.
   *
   * @param testInstance Instance để sử dụng cho testing
   */
  public static void setTestInstance(EventCache testInstance) {
    instance = testInstance;
  }

  /**
   * Reset instance - chỉ sử dụng cho mục đích testing.
   */
  public static void resetInstance() {
    instance = null;
  }

  /**
   * Cập nhật Event trong cache
   *
   * @param eventId ID của event
   */
  public void updateEvent(String eventId) {
    eventCache.put(eventId, true);
  }

  /**
   * Kiểm tra xem event đã được xử lý chưa
   *
   * @param eventId ID của event
   * @return true nếu đã xử lý, false nếu chưa xử lý
   */
  public Boolean isEventProcessed(String eventId) {
    return eventCache.getIfPresent(eventId) != null;
  }
}
