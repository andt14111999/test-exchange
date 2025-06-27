package com.exchangeengine.serializer;

import java.io.IOException;
import java.util.BitSet;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Custom deserializer cho BitSet để Jackson có thể xử lý đúng cách
 */
public class BitSetDeserializer extends JsonDeserializer<BitSet> {
  @Override
  public BitSet deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    String value = p.getValueAsString();
    if (value == null || value.isEmpty()) {
      return new BitSet();
    }

    try {
      // Giải mã chuỗi Base64 thành mảng byte
      byte[] bytes = Base64.getDecoder().decode(value);

      // Chuyển đổi mảng byte thành BitSet
      return BitSet.valueOf(bytes);
    } catch (IllegalArgumentException e) {
      // Xử lý lỗi nếu chuỗi không phải Base64 hợp lệ
      return new BitSet();
    }
  }
}
