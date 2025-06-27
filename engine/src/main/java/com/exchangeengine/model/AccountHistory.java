package com.exchangeengine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.codec.digest.MurmurHash3;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountHistory {
  private String key; // Format: {hashedPrefix}_{accountKey}:{identifier}:{timestamp}
  private String accountKey;
  private String identifier;
  private String operationType;
  private String availableBalance; // Format: "previous|new|diff" (ví dụ: "100.0|110.0|10.0")
  private String frozenBalance; // Format: "previous|new|diff" (ví dụ: "50.0|40.0|-10.0")
  private long timestamp;

  /**
   * Constructor không tham số cho Jackson deserialization
   */
  public AccountHistory() {
    this.timestamp = Instant.now().toEpochMilli();
  }

  /**
   * Constructor với các tham số cần thiết
   */
  public AccountHistory(String accountKey, String identifier, String operationType) {
    this();
    this.accountKey = accountKey;
    this.identifier = identifier;
    this.operationType = operationType;
    this.key = generateKey(accountKey, identifier, this.timestamp);
  }

  /**
   * Tạo hash prefix từ accountKey (8 ký tự hex từ MurmurHash3)
   */
  public static String generateHashedPrefix(String accountKey) {
    // Sử dụng MurmurHash3 từ Apache Commons Codec
    long[] hash128 = MurmurHash3.hash128(accountKey.getBytes());
    String prefix = String.format("%016X", hash128[0]).substring(0, 8);
    return prefix.toLowerCase();
  }

  /**
   * Tạo key cho AccountHistory - Format:
   * {hashedPrefix}-{accountKey}-{identifier}-{timestamp}
   * Được tối ưu cho việc tìm kiếm theo hashedPrefix (8 ký tự đầu)
   */
  public static String generateKey(String accountKey, String identifier, long timestamp) {
    String hashedPrefix = generateHashedPrefix(accountKey);
    return String.format("%s-%s-%s-%d", hashedPrefix, accountKey, identifier, timestamp);
  }

  /**
   * Tạo prefix cho tìm kiếm theo accountKey với hashed prefix
   */
  public static String generateAccountPrefix(String accountKey) {
    String hashedPrefix = generateHashedPrefix(accountKey);
    // Chỉ trả về 8 ký tự hash làm prefix cho bloom filter
    return hashedPrefix;
  }

  /**
   * Thiết lập giá trị availableBalance và frozenBalance
   */
  public void setBalanceValues(
      BigDecimal prevAvailableBalance,
      BigDecimal newAvailableBalance,
      BigDecimal prevFrozenBalance,
      BigDecimal newFrozenBalance) {
    this.availableBalance = formatBalanceChange(prevAvailableBalance, newAvailableBalance);
    this.frozenBalance = formatBalanceChange(prevFrozenBalance, newFrozenBalance);
  }

  /**
   * Thiết lập giá trị availableBalance và frozenBalance ban đầu
   */
  public void setBalanceInitAccount() {
    setBalanceValues(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
  }

  /**
   * Thiết lập giá trị availableBalance
   */
  public void setAvailableBalanceValues(BigDecimal previous, BigDecimal newValue) {
    this.availableBalance = formatBalanceChange(previous, newValue);
  }

  /**
   * Thiết lập giá trị frozenBalance
   */
  public void setFrozenBalanceValues(BigDecimal previous, BigDecimal newValue) {
    this.frozenBalance = formatBalanceChange(previous, newValue);
  }

  /**
   * Tạo chuỗi balance change từ giá trị trước và sau
   */
  public static String formatBalanceChange(BigDecimal previous, BigDecimal newValue) {
    BigDecimal diff = newValue.subtract(previous);
    return String.format("%.8f|%.8f|%.8f", previous, newValue, diff);
  }

  // Getters and Setters
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getAccountKey() {
    return accountKey;
  }

  public void setAccountKey(String accountKey) {
    this.accountKey = accountKey;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getOperationType() {
    return operationType;
  }

  public void setOperationType(String operationType) {
    this.operationType = operationType;
  }

  public String getAvailableBalance() {
    return availableBalance;
  }

  public void setAvailableBalance(String availableBalance) {
    this.availableBalance = availableBalance;
  }

  public String getFrozenBalance() {
    return frozenBalance;
  }

  public void setFrozenBalance(String frozenBalance) {
    this.frozenBalance = frozenBalance;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public String toString() {
    return "AccountHistory{" +
        "key='" + key + '\'' +
        ", accountKey='" + accountKey + '\'' +
        ", identifier='" + identifier + '\'' +
        ", operationType='" + operationType + '\'' +
        ", availableBalance='" + availableBalance + '\'' +
        ", frozenBalance='" + frozenBalance + '\'' +
        ", timestamp=" + timestamp +
        '}';
  }
}
