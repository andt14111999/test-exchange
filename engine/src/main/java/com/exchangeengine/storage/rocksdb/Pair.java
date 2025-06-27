package com.exchangeengine.storage.rocksdb;

/**
 * Lớp tiện ích để lưu trữ cặp key-value
 */
public class Pair<K, V> {
  private final K key;
  private final V value;

  public Pair(K key, V value) {
    this.key = key;
    this.value = value;
  }

  public K getKey() {
    return key;
  }

  public V getValue() {
    return value;
  }
}
