package com.exchangeengine.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonSerializerTest {

  // Lớp test đơn giản để serialize/deserialize
  static class TestClass {
    private String name;
    private int age;
    private boolean active;

    // No-args constructor cần thiết cho Jackson
    public TestClass() {
    }

    public TestClass(String name, int age, boolean active) {
      this.name = name;
      this.age = age;
      this.active = active;
    }

    // Getters and setters
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }

    public boolean isActive() {
      return active;
    }

    public void setActive(boolean active) {
      this.active = active;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      TestClass testClass = (TestClass) o;
      return age == testClass.age &&
          active == testClass.active &&
          (name == null ? testClass.name == null : name.equals(testClass.name));
    }
  }

  @Test
  @DisplayName("JsonSerializer should have a private constructor to prevent instantiation")
  void constructor_ShouldBePrivate() throws Exception {
    Constructor<JsonSerializer> constructor = JsonSerializer.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");

    // Kiểm tra constructor ném exception khi cố tạo instance
    constructor.setAccessible(true);
    Exception exception = assertThrows(Exception.class, constructor::newInstance);
    assertTrue(exception.getCause() instanceof UnsupportedOperationException,
        "Constructor should throw UnsupportedOperationException");
    assertEquals("Utility class should not be instantiated", exception.getCause().getMessage());
  }

  @Test
  @DisplayName("serialize should convert object to byte array")
  void serialize_ShouldConvertObjectToByteArray() {
    // Arrange
    TestClass testObject = new TestClass("John", 25, true);

    // Act
    byte[] serialized = JsonSerializer.serialize(testObject);

    // Assert
    assertNotNull(serialized);
    assertTrue(serialized.length > 0);
  }

  @Test
  @DisplayName("serialize should throw RuntimeException when IOException occurs")
  void serialize_ShouldThrowRuntimeException_WhenIOExceptionOccurs() {
    // Tạo object sẽ gây ra IOException khi serialize
    SerializeFailObject failObject = new SerializeFailObject();

    // Kiểm tra rằng RuntimeException được ném với cause là IOException
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> JsonSerializer.serialize(failObject));

    assertEquals("Serialization error", exception.getMessage());
    assertTrue(exception.getCause() instanceof IOException);
  }

  // Inner class gây lỗi khi serialize
  static class SerializeFailObject {
    private Object nonSerializableField = new Object();

    // Phương thức tùy chỉnh sẽ gây ra lỗi khi Jackson serialize
    @com.fasterxml.jackson.annotation.JsonValue
    public Object getValue() throws IOException {
      throw new IOException("Test IOException from custom getter");
    }
  }

  @Test
  @DisplayName("private constructor should throw UnsupportedOperationException")
  void constructor_ShouldThrowUnsupportedOperationException() {
    try {
      Constructor<?> constructor = JsonSerializer.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      try {
        constructor.newInstance();
        fail("Expected UnsupportedOperationException but no exception was thrown");
      } catch (InvocationTargetException e) {
        assertTrue(e.getCause() instanceof UnsupportedOperationException);
        assertEquals("Utility class should not be instantiated", e.getCause().getMessage());
      }
    } catch (Exception e) {
      fail("Test failed with exception: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("deserialize should convert byte array back to object")
  void deserialize_ShouldConvertByteArrayBackToObject() {
    // Arrange
    TestClass original = new TestClass("John", 25, true);
    byte[] serialized = JsonSerializer.serialize(original);

    // Act
    TestClass deserialized = JsonSerializer.deserialize(serialized, TestClass.class);

    // Assert
    assertNotNull(deserialized);
    assertEquals(original.getName(), deserialized.getName());
    assertEquals(original.getAge(), deserialized.getAge());
    assertEquals(original.isActive(), deserialized.isActive());
    assertEquals(original, deserialized);
  }

  @Test
  @DisplayName("toMap should convert object to Map")
  void toMap_ShouldConvertObjectToMap() {
    // Arrange
    TestClass testObject = new TestClass("John", 25, true);

    // Act
    Map<String, Object> map = JsonSerializer.toMap(testObject);

    // Assert
    assertNotNull(map);
    assertEquals("John", map.get("name"));
    assertEquals(25, map.get("age"));
    assertEquals(true, map.get("active"));
  }

  @Test
  @DisplayName("serialize and deserialize should work with Map")
  void serializeAndDeserialize_ShouldWorkWithMap() {
    // Arrange
    Map<String, Object> original = new HashMap<>();
    original.put("string", "value");
    original.put("number", 123);
    original.put("boolean", true);

    // Act
    byte[] serialized = JsonSerializer.serialize(original);
    Map<?, ?> deserialized = JsonSerializer.deserialize(serialized, Map.class);

    // Assert
    assertNotNull(deserialized);
    assertEquals("value", deserialized.get("string"));
    assertEquals(123, ((Number) deserialized.get("number")).intValue());
    assertEquals(true, deserialized.get("boolean"));
  }

  @Test
  @DisplayName("deserialize should throw RuntimeException when invalid bytes provided")
  void deserialize_ShouldThrowRuntimeException_WhenInvalidBytesProvided() {
    // Arrange
    byte[] invalidBytes = "Not valid JSON".getBytes();

    // Act & Assert
    assertThrows(RuntimeException.class, () -> {
      JsonSerializer.deserialize(invalidBytes, TestClass.class);
    });
  }
}
