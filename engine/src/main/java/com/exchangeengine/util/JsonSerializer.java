package com.exchangeengine.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class JsonSerializer {
  private static final Logger logger = LoggerFactory.getLogger(JsonSerializer.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Private constructor để ngăn chặn việc tạo instance.
   */
  private JsonSerializer() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  static {
    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
    objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  public static byte[] serialize(Object object) {
    try {
      return objectMapper.writeValueAsBytes(object);
    } catch (IOException e) {
      logger.error("Error serializing object: {}", e.getMessage(), e);
      throw new RuntimeException("Serialization error", e);
    }
  }

  public static <T> T deserialize(byte[] bytes, Class<T> clazz) {
    try {
      return objectMapper.readValue(bytes, clazz);
    } catch (IOException e) {
      logger.error("Error deserializing object: {}", e.getMessage(), e);
      throw new RuntimeException("Deserialization error", e);
    }
  }

  /**
   * Convert object to Map<String, Object> using the configured visibility
   * settings
   * This will only include fields, not getter methods
   *
   * @param object The object to convert
   * @return Map representation of the object
   */
  public static Map<String, Object> toMap(Object object) {
    return objectMapper.convertValue(object, new TypeReference<Map<String, Object>>() {
    });
  }
}
