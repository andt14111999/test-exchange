package com.exchangeengine.serializer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Base64;
import java.util.BitSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

@DisplayName("BitSetSerializer")
class BitSetSerializerTest {

  @Test
  @DisplayName("Mã hóa BitSet trống thành chuỗi Base64")
  void serializeEmptyBitSet() throws IOException {
    // Given
    BitSetSerializer serializer = new BitSetSerializer();
    BitSet emptyBitSet = new BitSet();
    JsonGenerator jsonGenerator = Mockito.mock(JsonGenerator.class);
    SerializerProvider provider = Mockito.mock(SerializerProvider.class);

    // When
    serializer.serialize(emptyBitSet, jsonGenerator, provider);

    // Then
    // Verify writeString was called with Base64 encoding of empty BitSet
    Mockito.verify(jsonGenerator).writeString(Mockito.anyString());
  }

  @Test
  @DisplayName("Mã hóa BitSet có các bit được thiết lập thành chuỗi Base64")
  void serializeBitSetWithSetBits() throws IOException {
    // Given
    BitSetSerializer serializer = new BitSetSerializer();
    BitSet bitSet = new BitSet();
    bitSet.set(0);
    bitSet.set(3);
    bitSet.set(8);

    JsonGenerator jsonGenerator = Mockito.mock(JsonGenerator.class);
    SerializerProvider provider = Mockito.mock(SerializerProvider.class);

    // When
    serializer.serialize(bitSet, jsonGenerator, provider);

    // Then
    // Khi BitSet được thiết lập [0, 3, 8], toByteArray() sẽ tạo ra [137] (binary:
    // 10001001)
    byte[] expected = bitSet.toByteArray();
    String expectedBase64 = Base64.getEncoder().encodeToString(expected);

    Mockito.verify(jsonGenerator).writeString(expectedBase64);
  }

  @Test
  @DisplayName("Mã hóa BitSet có các bit cao được thiết lập thành chuỗi Base64")
  void serializeBitSetWithHighBits() throws IOException {
    // Given
    BitSetSerializer serializer = new BitSetSerializer();
    BitSet bitSet = new BitSet();
    bitSet.set(100);
    bitSet.set(200);

    JsonGenerator jsonGenerator = Mockito.mock(JsonGenerator.class);
    SerializerProvider provider = Mockito.mock(SerializerProvider.class);

    // When
    serializer.serialize(bitSet, jsonGenerator, provider);

    // Then
    byte[] expected = bitSet.toByteArray();
    String expectedBase64 = Base64.getEncoder().encodeToString(expected);

    Mockito.verify(jsonGenerator).writeString(expectedBase64);
  }

  @Test
  @DisplayName("Xử lý BitSet null bằng cách ghi null")
  void serializeNullBitSet() throws IOException {
    // Given
    BitSetSerializer serializer = new BitSetSerializer();
    BitSet nullBitSet = null;
    JsonGenerator jsonGenerator = Mockito.mock(JsonGenerator.class);
    SerializerProvider provider = Mockito.mock(SerializerProvider.class);

    // When
    serializer.serialize(nullBitSet, jsonGenerator, provider);

    // Then
    Mockito.verify(jsonGenerator).writeNull();
    Mockito.verifyNoMoreInteractions(jsonGenerator);
  }
}
