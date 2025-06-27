package com.exchangeengine.serializer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Base64;
import java.util.BitSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

@DisplayName("BitSetDeserializer")
class BitSetDeserializerTest {

  @Test
  @DisplayName("Giải mã chuỗi Base64 trống thành BitSet")
  void deserializeEmptyString() throws IOException {
    // Given
    BitSetDeserializer deserializer = new BitSetDeserializer();
    JsonParser parser = Mockito.mock(JsonParser.class);
    DeserializationContext context = Mockito.mock(DeserializationContext.class);

    Mockito.when(parser.getValueAsString()).thenReturn("");

    // When
    BitSet result = deserializer.deserialize(parser, context);

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Giải mã chuỗi Base64 thành BitSet với các bit thiết lập")
  void deserializeBitSetWithSetBits() throws IOException {
    // Given
    BitSetDeserializer deserializer = new BitSetDeserializer();
    JsonParser parser = Mockito.mock(JsonParser.class);
    DeserializationContext context = Mockito.mock(DeserializationContext.class);

    // Tạo BitSet mẫu với các bit 0, 3, 8 thiết lập
    BitSet expectedBitSet = new BitSet();
    expectedBitSet.set(0);
    expectedBitSet.set(3);
    expectedBitSet.set(8);

    // Chuyển đổi thành chuỗi Base64
    String base64String = Base64.getEncoder().encodeToString(expectedBitSet.toByteArray());
    Mockito.when(parser.getValueAsString()).thenReturn(base64String);

    // When
    BitSet result = deserializer.deserialize(parser, context);

    // Then
    assertNotNull(result);
    assertEquals(expectedBitSet, result);
    assertTrue(result.get(0));
    assertTrue(result.get(3));
    assertTrue(result.get(8));
    assertFalse(result.get(1));
    assertFalse(result.get(2));
  }

  @Test
  @DisplayName("Giải mã chuỗi Base64 thành BitSet với các bit cao thiết lập")
  void deserializeBitSetWithHighBits() throws IOException {
    // Given
    BitSetDeserializer deserializer = new BitSetDeserializer();
    JsonParser parser = Mockito.mock(JsonParser.class);
    DeserializationContext context = Mockito.mock(DeserializationContext.class);

    // Tạo BitSet mẫu với các bit ở vị trí cao
    BitSet expectedBitSet = new BitSet();
    expectedBitSet.set(100);
    expectedBitSet.set(200);

    // Chuyển đổi thành chuỗi Base64
    String base64String = Base64.getEncoder().encodeToString(expectedBitSet.toByteArray());
    Mockito.when(parser.getValueAsString()).thenReturn(base64String);

    // When
    BitSet result = deserializer.deserialize(parser, context);

    // Then
    assertNotNull(result);
    assertEquals(expectedBitSet, result);
    assertTrue(result.get(100));
    assertTrue(result.get(200));
    assertFalse(result.get(150));
    assertFalse(result.get(0));
  }

  @Test
  @DisplayName("Xử lý chuỗi null bằng cách trả về BitSet trống")
  void deserializeNullString() throws IOException {
    // Given
    BitSetDeserializer deserializer = new BitSetDeserializer();
    JsonParser parser = Mockito.mock(JsonParser.class);
    DeserializationContext context = Mockito.mock(DeserializationContext.class);

    Mockito.when(parser.getValueAsString()).thenReturn(null);

    // When
    BitSet result = deserializer.deserialize(parser, context);

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Xử lý chuỗi Base64 không hợp lệ bằng cách trả về BitSet trống")
  void deserializeInvalidBase64String() throws IOException {
    // Given
    BitSetDeserializer deserializer = new BitSetDeserializer();
    JsonParser parser = Mockito.mock(JsonParser.class);
    DeserializationContext context = Mockito.mock(DeserializationContext.class);

    // Chuỗi Base64 không hợp lệ
    Mockito.when(parser.getValueAsString()).thenReturn("@#$%^&*");

    // When
    BitSet result = deserializer.deserialize(parser, context);

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }
}
