package com.exchangeengine.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for creating deep copies of objects.
 * Uses Jackson serialization/deserialization to create deep copies of objects.
 */
public class ObjectCloner {
  private static final Logger logger = LoggerFactory.getLogger(ObjectCloner.class);

  /**
   * Private constructor to prevent instantiation.
   */
  private ObjectCloner() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  /**
   * Creates a deep copy of the specified object using Jackson
   * serialization/deserialization.
   *
   * @param <T>    The type of the object to clone
   * @param object The object to clone
   * @param clazz  The class of the object
   * @return A deep copy of the object
   */
  public static <T> T duplicate(T object, Class<T> clazz) {
    if (object == null) {
      return null;
    }

    try {
      // Serialize the object to bytes
      byte[] serialized = JsonSerializer.serialize(object);

      // Deserialize back to create a deep copy
      return JsonSerializer.deserialize(serialized, clazz);
    } catch (Exception e) {
      logger.error("Error duplicating object: {}", e.getMessage(), e);
      throw new RuntimeException("Error creating duplicate: " + e.getMessage(), e);
    }
  }
}
