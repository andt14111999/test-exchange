package com.exchangeengine.model;

import com.exchangeengine.util.JsonSerializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Account {
  private static final int DEFAULT_SCALE = 16;

  private String key;
  private BigDecimal availableBalance;
  private BigDecimal frozenBalance;
  private long createdAt;
  private long updatedAt;

  /**
   * Constructor không tham số cho Jackson deserialization
   */
  public Account() {
    this.availableBalance = BigDecimal.ZERO.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    this.frozenBalance = BigDecimal.ZERO.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    this.createdAt = Instant.now().toEpochMilli();
    this.updatedAt = this.createdAt;
  }

  /**
   * Constructor với key
   */
  public Account(String key) {
    this();
    this.key = key;
  }

  /**
   * Lấy tổng số dư (available + frozen)
   */
  public BigDecimal getTotalBalance() {
    return availableBalance.add(frozenBalance).setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
  }

  /**
   * Tăng số dư khả dụng
   */
  public void increaseAvailableBalance(BigDecimal amount) {
    if (amount == null) {
      throw new IllegalArgumentException("Amount cannot be null");
    }

    BigDecimal scaledAmount = amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);

    if (scaledAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Amount must be greater than or equal to zero");
    }

    this.availableBalance = this.availableBalance.add(scaledAmount).setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    this.updatedAt = Instant.now().toEpochMilli();
  }

  /**
   * Giảm số dư khả dụng
   */
  public void decreaseAvailableBalance(BigDecimal amount) {
    if (amount == null) {
      throw new IllegalArgumentException("Amount cannot be null");
    }

    BigDecimal scaledAmount = amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);

    if (scaledAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Amount must be greater than or equal to zero");
    }

    if (this.availableBalance.compareTo(scaledAmount) < 0) {
      throw new IllegalArgumentException("Available balance is not enough to decrease");
    }

    this.availableBalance = this.availableBalance.subtract(scaledAmount).setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    this.updatedAt = Instant.now().toEpochMilli();
  }

  /**
   * Tăng số dư đóng băng
   */
  public void increaseFrozenBalance(BigDecimal amount) {
    if (amount == null) {
      throw new IllegalArgumentException("Amount cannot be null");
    }

    BigDecimal scaledAmount = amount.setScale(DEFAULT_SCALE,
        RoundingMode.HALF_UP);

    if (scaledAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Amount must be greater than or equal to  zero");
    }

    this.frozenBalance = this.frozenBalance.add(scaledAmount).setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    this.updatedAt = Instant.now().toEpochMilli();
  }

  /**
   * Chuyển từ available sang frozen với identifier
   */
  public void increaseFrozenBalance(BigDecimal amount, String identifier) {
    if (amount == null) {
      throw new IllegalArgumentException("Amount cannot be null");
    }

    if (identifier == null || identifier.trim().isEmpty()) {
      throw new IllegalArgumentException("Identifier cannot be null or empty");
    }

    BigDecimal scaledAmount = amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);

    if (scaledAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Amount must be greater than or equal to zero");
    }

    if (this.availableBalance.compareTo(scaledAmount) < 0) {
      throw new IllegalArgumentException("Available balance is not enough to lock");
    }

    this.availableBalance = this.availableBalance.subtract(scaledAmount).setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    this.frozenBalance = this.frozenBalance.add(scaledAmount).setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    this.updatedAt = Instant.now().toEpochMilli();
  }

  /**
   * Giảm số dư đóng băng
   */
  public void decreaseFrozenBalance(BigDecimal amount) {
    if (amount == null) {
      throw new IllegalArgumentException("Amount cannot be null");
    }

    BigDecimal scaledAmount = amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);

    if (scaledAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Amount must be greater than or equal to zero");
    }

    if (this.frozenBalance.compareTo(scaledAmount) < 0) {
      throw new IllegalArgumentException("Frozen balance is not enough to decrease");
    }

    this.frozenBalance = this.frozenBalance.subtract(scaledAmount).setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    this.updatedAt = Instant.now().toEpochMilli();
  }

  /**
   * Chuyển từ frozen sang available với identifier
   */
  public void decreaseFrozenIncreaseAvailableBalance(BigDecimal amount) {
    decreaseFrozenBalance(amount);
    increaseAvailableBalance(amount);
  }

  // Getters and Setters
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
    this.updatedAt = Instant.now().toEpochMilli();
  }

  public BigDecimal getAvailableBalance() {
    return availableBalance;
  }

  public void setAvailableBalance(BigDecimal availableBalance) {
    if (availableBalance == null) {
      throw new IllegalArgumentException("Available balance cannot be null");
    }

    BigDecimal scaledBalance = availableBalance.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);

    if (scaledBalance.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Available balance cannot be negative");
    }

    this.availableBalance = scaledBalance;
    this.updatedAt = Instant.now().toEpochMilli();
  }

  public BigDecimal getFrozenBalance() {
    return frozenBalance;
  }

  public void setFrozenBalance(BigDecimal frozenBalance) {
    if (frozenBalance == null) {
      throw new IllegalArgumentException("Frozen balance cannot be null");
    }

    BigDecimal scaledBalance = frozenBalance.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);

    if (scaledBalance.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Frozen balance cannot be negative");
    }

    this.frozenBalance = scaledBalance;
    this.updatedAt = Instant.now().toEpochMilli();
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }

  /**
   * Convert to message json
   *
   * @return message json
   */
  public Map<String, Object> toMessageJson() {
    Map<String, Object> accountMap = JsonSerializer.toMap(this);
    accountMap.put("totalBalance", getTotalBalance());
    return accountMap;
  }

  /**
   * Validate account
   */
  public List<String> validateRequiredFields() {
    List<String> errors = new ArrayList<>();
    if (key == null || key.trim().isEmpty()) {
      errors.add("Account Key cannot be null or empty");
    }
    return errors;
  }

  // override toString
  @Override
  public String toString() {
    return "Account{" +
        "key='" + key + '\'' +
        ", availableBalance=" + availableBalance +
        ", frozenBalance=" + frozenBalance +
        ", createdAt=" + createdAt +
        ", updatedAt=" + updatedAt +
        '}';
  }
}
