package com.exchangeengine.storage.rocksdb;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PairTest {

  @Test
  void testPairWithStringValues() {
    // Arrange
    String key = "testKey";
    String value = "testValue";

    // Act
    Pair<String, String> pair = new Pair<>(key, value);

    // Assert
    assertEquals(key, pair.getKey());
    assertEquals(value, pair.getValue());
  }

  @Test
  void testPairWithDifferentTypes() {
    // Arrange
    String key = "accountId";
    BigDecimal value = new BigDecimal("100.50");

    // Act
    Pair<String, BigDecimal> pair = new Pair<>(key, value);

    // Assert
    assertEquals(key, pair.getKey());
    assertEquals(value, pair.getValue());
    assertEquals(new BigDecimal("100.50"), pair.getValue());
  }

  @Test
  void testPairWithNullValues() {
    // Arrange & Act
    Pair<String, Object> pair = new Pair<>(null, null);

    // Assert
    assertNull(pair.getKey());
    assertNull(pair.getValue());
  }

  @Test
  void testPairImmutability() {
    // Arrange
    StringBuilder key = new StringBuilder("key");
    StringBuilder value = new StringBuilder("value");

    Pair<StringBuilder, StringBuilder> pair = new Pair<>(key, value);

    // Assert initial state
    assertEquals("key", pair.getKey().toString());
    assertEquals("value", pair.getValue().toString());

    // Modify the original objects
    key.append("Modified");
    value.append("Modified");

    // The pair should reflect changes since it stores references
    assertEquals("keyModified", pair.getKey().toString());
    assertEquals("valueModified", pair.getValue().toString());
  }
}
