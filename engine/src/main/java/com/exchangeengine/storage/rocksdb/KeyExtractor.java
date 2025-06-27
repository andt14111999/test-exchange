package com.exchangeengine.storage.rocksdb;

/**
 * Interface để trích xuất key từ đối tượng
 */
@FunctionalInterface
public interface KeyExtractor<T> {
  String getKey(T item);
}
