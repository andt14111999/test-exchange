package com.exchangeengine.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

import org.mockito.MockedStatic;

class ObjectClonerTest {

  // Simple test class with primitive fields
  static class SimpleTestObject {
    private String name;
    private int age;
    private boolean active;

    public SimpleTestObject() {
    }

    public SimpleTestObject(String name, int age, boolean active) {
      this.name = name;
      this.age = age;
      this.active = active;
    }

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
  }

  // Complex test class with nested objects and collections
  static class ComplexTestObject {
    private String id;
    private SimpleTestObject simpleObject;
    private List<String> stringList;
    private BigDecimal amount;

    public ComplexTestObject() {
      this.stringList = new ArrayList<>();
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public SimpleTestObject getSimpleObject() {
      return simpleObject;
    }

    public void setSimpleObject(SimpleTestObject simpleObject) {
      this.simpleObject = simpleObject;
    }

    public List<String> getStringList() {
      return stringList;
    }

    public void setStringList(List<String> stringList) {
      this.stringList = stringList;
    }

    public BigDecimal getAmount() {
      return amount;
    }

    public void setAmount(BigDecimal amount) {
      this.amount = amount;
    }
  }

  @Test
  @DisplayName("private constructor should throw UnsupportedOperationException")
  void constructor_ShouldThrowUnsupportedOperationException() {
    try {
      Constructor<?> constructor = ObjectCloner.class.getDeclaredConstructor();
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
  @DisplayName("duplicate should return null when source is null")
  void duplicate_ShouldReturnNull_WhenSourceIsNull() {
    // Act
    SimpleTestObject result = ObjectCloner.duplicate(null, SimpleTestObject.class);

    // Assert
    assertNull(result);
  }

  @Test
  @DisplayName("duplicate should create a deep copy of a simple object")
  void duplicate_ShouldCreateDeepCopy_OfSimpleObject() {
    // Arrange
    SimpleTestObject original = new SimpleTestObject("John", 30, true);

    // Act
    SimpleTestObject copy = ObjectCloner.duplicate(original, SimpleTestObject.class);

    // Assert
    assertNotNull(copy);
    assertNotSame(original, copy); // Different object instances
    assertEquals(original.getName(), copy.getName());
    assertEquals(original.getAge(), copy.getAge());
    assertEquals(original.isActive(), copy.isActive());

    // Modify the copy
    copy.setName("Jane");
    copy.setAge(25);
    copy.setActive(false);

    // Original should remain unchanged
    assertEquals("John", original.getName());
    assertEquals(30, original.getAge());
    assertEquals(true, original.isActive());
  }

  @Test
  @DisplayName("duplicate should create a deep copy of a complex object")
  void duplicate_ShouldCreateDeepCopy_OfComplexObject() {
    // Arrange
    ComplexTestObject original = new ComplexTestObject();
    original.setId("123");
    original.setSimpleObject(new SimpleTestObject("John", 30, true));
    original.getStringList().add("Item 1");
    original.getStringList().add("Item 2");
    original.setAmount(new BigDecimal("123.45"));

    // Act
    ComplexTestObject copy = ObjectCloner.duplicate(original, ComplexTestObject.class);

    // Assert
    assertNotNull(copy);
    assertNotSame(original, copy); // Different object instances

    // Check that the nested object is also a deep copy
    assertNotSame(original.getSimpleObject(), copy.getSimpleObject());
    assertEquals(original.getSimpleObject().getName(), copy.getSimpleObject().getName());

    // Check that the list is a deep copy
    assertNotSame(original.getStringList(), copy.getStringList());
    assertEquals(original.getStringList().size(), copy.getStringList().size());
    assertEquals(original.getStringList().get(0), copy.getStringList().get(0));

    // Check that BigDecimal is copied correctly
    assertEquals(0, original.getAmount().compareTo(copy.getAmount()));

    // Modify the copy
    copy.getSimpleObject().setName("Jane");
    copy.getStringList().add("Item 3");

    // Original should remain unchanged
    assertEquals("John", original.getSimpleObject().getName());
    assertEquals(2, original.getStringList().size());
  }

  @Test
  @DisplayName("duplicate should throw RuntimeException when serialization fails")
  void duplicate_ShouldThrowRuntimeException_WhenSerializationFails() {
    // Arrange
    SimpleTestObject testObject = new SimpleTestObject("Test", 25, true);

    try (MockedStatic<JsonSerializer> mockedSerializer = mockStatic(JsonSerializer.class)) {
      // Mock JsonSerializer.serialize to throw exception
      mockedSerializer.when(() -> JsonSerializer.serialize(any()))
          .thenThrow(new RuntimeException("Serialization test error"));

      // Act & Assert
      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> ObjectCloner.duplicate(testObject, SimpleTestObject.class));

      assertEquals("Error creating duplicate: Serialization test error", exception.getMessage());
      assertTrue(exception.getCause() instanceof RuntimeException);
      assertEquals("Serialization test error", exception.getCause().getMessage());
    }
  }
}
