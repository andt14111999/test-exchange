package com.exchangeengine.storage.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.cache.Cache;

@ExtendWith(MockitoExtension.class)
class EventCacheTest {

  private EventCache eventCache;

  @BeforeEach
  void setUp() throws Exception {
    // Reset EventCache instance
    resetSingleton(EventCache.class, "instance");

    // Khởi tạo EventCache
    eventCache = EventCache.getInstance();
  }

  @AfterEach
  void tearDown() throws Exception {
    resetSingleton(EventCache.class, "instance");
  }

  private void resetSingleton(Class<?> clazz, String fieldName) throws Exception {
    java.lang.reflect.Field instance = clazz.getDeclaredField(fieldName);
    instance.setAccessible(true);
    instance.set(null, null);
  }

  @Test
  @DisplayName("getInstance should return the same instance")
  void getInstance_ShouldReturnSameInstance() {
    EventCache instance1 = EventCache.getInstance();
    EventCache instance2 = EventCache.getInstance();

    assertSame(instance1, instance2, "getInstance should always return the same instance");
  }

  @Test
  @DisplayName("updateEvent should add event to cache")
  void updateEvent_ShouldAddEventToCache() throws Exception {
    // Arrange
    String eventId = "event123";

    // Access the eventCache field through reflection to verify
    java.lang.reflect.Field cacheField = EventCache.class.getDeclaredField("eventCache");
    cacheField.setAccessible(true);
    com.google.common.cache.Cache<String, Boolean> cache = (com.google.common.cache.Cache<String, Boolean>) cacheField
        .get(eventCache);

    // Act
    eventCache.updateEvent(eventId);

    // Assert
    assertNotNull(cache.getIfPresent(eventId), "Event should be present in cache");
    assertEquals(Boolean.TRUE, cache.getIfPresent(eventId), "Event value should be TRUE");
  }

  @Test
  @DisplayName("isEventProcessed should return true when event is in cache")
  void isEventProcessed_ShouldReturnTrue_WhenEventIsInCache() throws Exception {
    // Arrange
    String eventId = "event123";

    // Add event to cache first
    eventCache.updateEvent(eventId);

    // Act
    Boolean result = eventCache.isEventProcessed(eventId);

    // Assert
    assertTrue(result, "Should return true for processed event");
  }

  @Test
  @DisplayName("isEventProcessed should return false when event is not in cache")
  void isEventProcessed_ShouldReturnFalse_WhenEventIsNotInCache() {
    // Arrange
    String eventId = "nonexistent_event";

    // Act
    Boolean result = eventCache.isEventProcessed(eventId);

    // Assert
    assertFalse(result, "Should return false for non-processed event");
  }

  @Test
  @DisplayName("updateEvent with multiple events should add all events to cache")
  void updateEvent_WithMultipleEvents_ShouldAddAllToCache() throws Exception {
    // Arrange
    String eventId1 = "event1";
    String eventId2 = "event2";
    String eventId3 = "event3";

    // Access the eventCache field through reflection to verify
    Field cacheField = EventCache.class.getDeclaredField("eventCache");
    cacheField.setAccessible(true);
    Cache<String, Boolean> cache = (Cache<String, Boolean>) cacheField.get(eventCache);

    // Act
    eventCache.updateEvent(eventId1);
    eventCache.updateEvent(eventId2);
    eventCache.updateEvent(eventId3);

    // Assert
    assertNotNull(cache.getIfPresent(eventId1), "Event 1 should be present in cache");
    assertNotNull(cache.getIfPresent(eventId2), "Event 2 should be present in cache");
    assertNotNull(cache.getIfPresent(eventId3), "Event 3 should be present in cache");

    assertTrue(eventCache.isEventProcessed(eventId1), "Event 1 should be processed");
    assertTrue(eventCache.isEventProcessed(eventId2), "Event 2 should be processed");
    assertTrue(eventCache.isEventProcessed(eventId3), "Event 3 should be processed");
  }

  @Test
  @DisplayName("updateEvent with same eventId should update cache")
  void updateEvent_WithSameEventId_ShouldUpdateCache() throws Exception {
    // Arrange
    String eventId = "event123";

    // Access the eventCache field through reflection
    Field cacheField = EventCache.class.getDeclaredField("eventCache");
    cacheField.setAccessible(true);
    Cache<String, Boolean> cache = (Cache<String, Boolean>) cacheField.get(eventCache);

    // Act
    eventCache.updateEvent(eventId);

    // Manually change the value (for testing purposes)
    cache.put(eventId, false);
    assertEquals(Boolean.FALSE, cache.getIfPresent(eventId), "Event value should be FALSE after manual change");

    // Update again
    eventCache.updateEvent(eventId);

    // Assert
    assertEquals(Boolean.TRUE, cache.getIfPresent(eventId), "Event value should be TRUE after update");
  }

  @Test
  @DisplayName("updateEvent with empty eventId should work properly")
  void updateEvent_WithEmptyEventId_ShouldWorkProperly() {
    // Act
    eventCache.updateEvent("");

    // Assert
    assertTrue(eventCache.isEventProcessed(""), "Empty event should be processed");
  }
}
