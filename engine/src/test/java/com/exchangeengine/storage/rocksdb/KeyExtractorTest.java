package com.exchangeengine.storage.rocksdb;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KeyExtractorTest {

  private static class TestObject {
    private final String id;

    TestObject(String id) {
      this.id = id;
    }

    public String getId() {
      return id;
    }
  }

  @Test
  void testKeyExtractor() {
    // Arrange
    KeyExtractor<TestObject> keyExtractor = TestObject::getId;
    TestObject testObject = new TestObject("test-id");

    // Act
    String key = keyExtractor.getKey(testObject);

    // Assert
    assertEquals("test-id", key);
  }

  @Test
  void testKeyExtractorWithNullObject() {
    // Arrange
    KeyExtractor<TestObject> keyExtractor = obj -> obj != null ? obj.getId() : null;

    // Act & Assert
    assertNull(keyExtractor.getKey(null));
  }

  @Test
  void testKeyExtractorComposition() {
    // Arrange
    KeyExtractor<TestObject> prefixExtractor = obj -> "prefix:";
    KeyExtractor<TestObject> idExtractor = TestObject::getId;

    // Compose a new extractor that combines both
    KeyExtractor<TestObject> composedExtractor = obj -> prefixExtractor.getKey(obj) + idExtractor.getKey(obj);

    TestObject testObject = new TestObject("test-id");

    // Act
    String key = composedExtractor.getKey(testObject);

    // Assert
    assertEquals("prefix:test-id", key);
  }
}
