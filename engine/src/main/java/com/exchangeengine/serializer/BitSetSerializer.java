package com.exchangeengine.serializer;

import java.io.IOException;
import java.util.BitSet;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Custom serializer cho BitSet để Jackson có thể xử lý đúng cách
 */
public class BitSetSerializer extends JsonSerializer<BitSet> {
  @Override
  public void serialize(BitSet value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    if (value == null) {
      gen.writeNull();
      return;
    }

    // Chuyển BitSet thành mảng byte
    byte[] bytes = value.toByteArray();

    // Mã hóa Base64 để lưu trữ an toàn trong JSON
    String base64String = Base64.getEncoder().encodeToString(bytes);

    // Ghi chuỗi Base64 vào JSON
    gen.writeString(base64String);
  }
}
