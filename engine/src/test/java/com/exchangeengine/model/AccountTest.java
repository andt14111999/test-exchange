package com.exchangeengine.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

  private Account account;
  private static final int DEFAULT_SCALE = 16;
  private static final String TEST_ACCOUNT_KEY = "user123:BTC";

  @BeforeEach
  void setUp() {
    account = new Account(TEST_ACCOUNT_KEY);
  }

  @Test
  @DisplayName("Khởi tạo tài khoản với key hợp lệ")
  void constructor_WithValidKey_InitializesAccount() {
    // Verify
    assertEquals(TEST_ACCOUNT_KEY, account.getKey());
    assertEquals(BigDecimal.ZERO.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getAvailableBalance());
    assertEquals(BigDecimal.ZERO.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getFrozenBalance());
    assertTrue(account.getCreatedAt() > 0);
    assertEquals(account.getCreatedAt(), account.getUpdatedAt());
  }

  @Test
  @DisplayName("Lấy tổng số dư (available + frozen)")
  void getTotalBalance_ReturnsCorrectSum() {
    // Setup
    BigDecimal available = new BigDecimal("10.5");
    BigDecimal frozen = new BigDecimal("5.5");
    account.setAvailableBalance(available);
    account.setFrozenBalance(frozen);

    // Execute
    BigDecimal total = account.getTotalBalance();

    // Verify
    assertEquals(new BigDecimal("16.0").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), total);
  }

  @Test
  @DisplayName("Tăng số dư khả dụng với số lượng hợp lệ")
  void increaseAvailableBalance_WithValidAmount_IncreasesBalance() {
    // Setup
    BigDecimal initialBalance = new BigDecimal("10.0");
    BigDecimal increaseAmount = new BigDecimal("5.0");
    account.setAvailableBalance(initialBalance);
    long initialUpdatedAt = account.getUpdatedAt();

    // Wait to ensure timestamp changes
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Execute
    account.increaseAvailableBalance(increaseAmount);

    // Verify
    assertEquals(new BigDecimal("15.0").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getAvailableBalance());
    assertTrue(account.getUpdatedAt() > initialUpdatedAt);
  }

  @Test
  @DisplayName("Tăng số dư khả dụng với số lượng null gây ra lỗi")
  void increaseAvailableBalance_WithNullAmount_ThrowsException() {
    // Verify
    assertThrows(IllegalArgumentException.class, () -> account.increaseAvailableBalance(null));
  }

  @Test
  @DisplayName("Tăng số dư khả dụng với số lượng âm gây ra lỗi")
  void increaseAvailableBalance_WithNegativeAmount_ThrowsException() {
    // Verify
    assertThrows(IllegalArgumentException.class,
        () -> account.increaseAvailableBalance(new BigDecimal("-1.0")));
  }

  @Test
  @DisplayName("Giảm số dư khả dụng với số lượng hợp lệ")
  void decreaseAvailableBalance_WithValidAmount_DecreasesBalance() {
    // Setup
    BigDecimal initialBalance = new BigDecimal("10.0");
    BigDecimal decreaseAmount = new BigDecimal("5.0");
    account.setAvailableBalance(initialBalance);
    long initialUpdatedAt = account.getUpdatedAt();

    // Wait to ensure timestamp changes
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Execute
    account.decreaseAvailableBalance(decreaseAmount);

    // Verify
    assertEquals(new BigDecimal("5.0").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getAvailableBalance());
    assertTrue(account.getUpdatedAt() > initialUpdatedAt);
  }

  @Test
  @DisplayName("Giảm số dư khả dụng với số lượng lớn hơn số dư hiện tại gây ra lỗi")
  void decreaseAvailableBalance_WithExcessiveAmount_ThrowsException() {
    // Setup
    account.setAvailableBalance(new BigDecimal("5.0"));

    // Verify
    assertThrows(IllegalArgumentException.class,
        () -> account.decreaseAvailableBalance(new BigDecimal("10.0")));
  }

  @Test
  @DisplayName("Chuyển từ số dư khả dụng sang số dư đóng băng")
  void increaseFrozenBalance_WithIdentifier_TransfersFromAvailableToFrozen() {
    // Setup
    BigDecimal initialAvailable = new BigDecimal("10.0");
    BigDecimal initialFrozen = new BigDecimal("5.0");
    BigDecimal amount = new BigDecimal("3.0");
    account.setAvailableBalance(initialAvailable);
    account.setFrozenBalance(initialFrozen);

    // Execute
    account.increaseFrozenBalance(amount, "txn123");

    // Verify
    assertEquals(new BigDecimal("7.0").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getAvailableBalance());
    assertEquals(new BigDecimal("8.0").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getFrozenBalance());
  }

  @Test
  @DisplayName("Chuyển từ số dư đóng băng sang số dư khả dụng")
  void decreaseFrozenIncreaseAvailableBalance_TransfersFromFrozenToAvailable() {
    // Setup
    BigDecimal initialAvailable = new BigDecimal("7.0");
    BigDecimal initialFrozen = new BigDecimal("8.0");
    BigDecimal amount = new BigDecimal("3.0");
    account.setAvailableBalance(initialAvailable);
    account.setFrozenBalance(initialFrozen);

    // Execute
    account.decreaseFrozenIncreaseAvailableBalance(amount);

    // Verify
    assertEquals(new BigDecimal("10.0").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getAvailableBalance());
    assertEquals(new BigDecimal("5.0").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getFrozenBalance());
  }

  @Test
  @DisplayName("Chuyển đổi tài khoản sang định dạng JSON cho message")
  void toMessageJson_ReturnsCorrectMap() {
    // Setup
    BigDecimal available = new BigDecimal("10.0");
    BigDecimal frozen = new BigDecimal("5.0");
    account.setAvailableBalance(available);
    account.setFrozenBalance(frozen);

    // Execute
    Map<String, Object> json = account.toMessageJson();

    // Verify
    assertNotNull(json);
    assertEquals(TEST_ACCOUNT_KEY, json.get("key"));
    assertEquals(available.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP).toString(),
        json.get("availableBalance").toString());
    assertEquals(frozen.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP).toString(), json.get("frozenBalance").toString());
    assertEquals(account.getTotalBalance().toString(), json.get("totalBalance").toString());
  }

  @Test
  @DisplayName("Kiểm tra tính hợp lệ của tài khoản")
  void validateRequiredFields_WithValidAccount_ReturnsNoErrors() {
    // Execute
    List<String> errors = account.validateRequiredFields();

    // Verify
    assertTrue(errors.isEmpty());
  }

  @Test
  @DisplayName("Kiểm tra tính hợp lệ của tài khoản khi key null")
  void validateRequiredFields_WithNullKey_ReturnsErrors() {
    // Setup
    account.setKey(null);

    // Execute
    List<String> errors = account.validateRequiredFields();

    // Verify
    assertFalse(errors.isEmpty());
    assertTrue(errors.contains("Account Key cannot be null or empty"));
  }

  @Test
  @DisplayName("Tăng số dư đóng băng với số lượng hợp lệ")
  void increaseFrozenBalance_WithValidAmount_IncreasesBalance() {
    // Setup
    BigDecimal initialBalance = new BigDecimal("5.0");
    BigDecimal increaseAmount = new BigDecimal("3.0");
    account.setFrozenBalance(initialBalance);
    long initialUpdatedAt = account.getUpdatedAt();

    // Wait to ensure timestamp changes
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Execute
    account.increaseFrozenBalance(increaseAmount);

    // Verify
    assertEquals(new BigDecimal("8.0").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getFrozenBalance());
    assertTrue(account.getUpdatedAt() > initialUpdatedAt);
  }

  @Test
  @DisplayName("Tăng số dư đóng băng với số lượng null gây ra lỗi")
  void increaseFrozenBalance_WithNullAmount_ThrowsException() {
    // Verify
    assertThrows(IllegalArgumentException.class, () -> account.increaseFrozenBalance(null));
  }

  @Test
  @DisplayName("Tăng số dư đóng băng với số lượng âm gây ra lỗi")
  void increaseFrozenBalance_WithNegativeAmount_ThrowsException() {
    // Verify
    assertThrows(IllegalArgumentException.class,
        () -> account.increaseFrozenBalance(new BigDecimal("-1.0")));
  }

  @Test
  @DisplayName("Chuyển từ available sang frozen với identifier null gây ra lỗi")
  void increaseFrozenBalance_WithNullIdentifier_ThrowsException() {
    // Setup
    BigDecimal amount = new BigDecimal("5.0");
    account.setAvailableBalance(new BigDecimal("10.0"));

    // Verify
    assertThrows(IllegalArgumentException.class, () -> account.increaseFrozenBalance(amount, null));
  }

  @Test
  @DisplayName("Chuyển từ available sang frozen với identifier rỗng gây ra lỗi")
  void increaseFrozenBalance_WithEmptyIdentifier_ThrowsException() {
    // Setup
    BigDecimal amount = new BigDecimal("5.0");
    account.setAvailableBalance(new BigDecimal("10.0"));

    // Verify
    assertThrows(IllegalArgumentException.class, () -> account.increaseFrozenBalance(amount, "  "));
  }

  @Test
  @DisplayName("Chuyển từ available sang frozen với số lượng lớn hơn số dư khả dụng gây ra lỗi")
  void increaseFrozenBalance_WithInsufficientAvailableBalance_ThrowsException() {
    // Setup
    BigDecimal available = new BigDecimal("5.0");
    BigDecimal amount = new BigDecimal("10.0");
    account.setAvailableBalance(available);

    // Verify
    assertThrows(IllegalArgumentException.class, () -> account.increaseFrozenBalance(amount, "txn123"));
  }

  @Test
  @DisplayName("Giảm số dư đóng băng với số lượng hợp lệ")
  void decreaseFrozenBalance_WithValidAmount_DecreasesBalance() {
    // Setup
    BigDecimal initialBalance = new BigDecimal("10.0");
    BigDecimal decreaseAmount = new BigDecimal("5.0");
    account.setFrozenBalance(initialBalance);
    long initialUpdatedAt = account.getUpdatedAt();

    // Wait to ensure timestamp changes
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Execute
    account.decreaseFrozenBalance(decreaseAmount);

    // Verify
    assertEquals(new BigDecimal("5.0").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getFrozenBalance());
    assertTrue(account.getUpdatedAt() > initialUpdatedAt);
  }

  @Test
  @DisplayName("Giảm số dư đóng băng với số lượng null gây ra lỗi")
  void decreaseFrozenBalance_WithNullAmount_ThrowsException() {
    // Verify
    assertThrows(IllegalArgumentException.class, () -> account.decreaseFrozenBalance(null));
  }

  @Test
  @DisplayName("Giảm số dư đóng băng với số lượng âm gây ra lỗi")
  void decreaseFrozenBalance_WithNegativeAmount_ThrowsException() {
    // Verify
    assertThrows(IllegalArgumentException.class,
        () -> account.decreaseFrozenBalance(new BigDecimal("-1.0")));
  }

  @Test
  @DisplayName("Giảm số dư đóng băng với số lượng lớn hơn số dư hiện tại gây ra lỗi")
  void decreaseFrozenBalance_WithExcessiveAmount_ThrowsException() {
    // Setup
    account.setFrozenBalance(new BigDecimal("5.0"));

    // Verify
    assertThrows(IllegalArgumentException.class,
        () -> account.decreaseFrozenBalance(new BigDecimal("10.0")));
  }

  @Test
  @DisplayName("Kiểm tra cài đặt số dư khả dụng với giá trị null")
  void setAvailableBalance_WithNullValue_ThrowsException() {
    // Verify
    assertThrows(IllegalArgumentException.class, () -> account.setAvailableBalance(null));
  }

  @Test
  @DisplayName("Kiểm tra cài đặt số dư khả dụng với giá trị âm")
  void setAvailableBalance_WithNegativeValue_ThrowsException() {
    // Verify
    assertThrows(IllegalArgumentException.class, () -> account.setAvailableBalance(new BigDecimal("-1.0")));
  }

  @Test
  @DisplayName("Kiểm tra cài đặt số dư đóng băng với giá trị null")
  void setFrozenBalance_WithNullValue_ThrowsException() {
    // Verify
    assertThrows(IllegalArgumentException.class, () -> account.setFrozenBalance(null));
  }

  @Test
  @DisplayName("Kiểm tra cài đặt số dư đóng băng với giá trị âm")
  void setFrozenBalance_WithNegativeValue_ThrowsException() {
    // Verify
    assertThrows(IllegalArgumentException.class, () -> account.setFrozenBalance(new BigDecimal("-1.0")));
  }

  @Test
  @DisplayName("Kiểm tra phương thức toString")
  void toString_ReturnsCorrectFormat() {
    // Setup
    BigDecimal available = new BigDecimal("10.0");
    BigDecimal frozen = new BigDecimal("5.0");
    account.setAvailableBalance(available);
    account.setFrozenBalance(frozen);

    // Execute
    String result = account.toString();

    // Verify
    assertTrue(result.contains("key='" + TEST_ACCOUNT_KEY + "'"));
    assertTrue(result.contains("availableBalance=" + available.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP)));
    assertTrue(result.contains("frozenBalance=" + frozen.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP)));
    assertTrue(result.contains("createdAt=" + account.getCreatedAt()));
    assertTrue(result.contains("updatedAt=" + account.getUpdatedAt()));
  }

  @Test
  @DisplayName("Kiểm tra phương thức decreaseFrozenIncreaseAvailableBalance")
  void decreaseFrozenIncreaseAvailableBalance_DecreasesAndIncreases() {
    // Setup
    BigDecimal initialAvailable = new BigDecimal("5.0");
    BigDecimal initialFrozen = new BigDecimal("10.0");
    BigDecimal amount = new BigDecimal("7.0");
    account.setAvailableBalance(initialAvailable);
    account.setFrozenBalance(initialFrozen);

    // Execute
    account.decreaseFrozenIncreaseAvailableBalance(amount);

    // Verify
    assertEquals(new BigDecimal("12.0").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getAvailableBalance());
    assertEquals(new BigDecimal("3.0").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getFrozenBalance());
  }

  @Test
  @DisplayName("Kiểm tra phương thức setKey cập nhật updatedAt")
  void setKey_UpdatesUpdatedAt() {
    // Setup
    long initialUpdatedAt = account.getUpdatedAt();

    // Wait to ensure timestamp changes
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Execute
    account.setKey("newKey");

    // Verify
    assertEquals("newKey", account.getKey());
    assertTrue(account.getUpdatedAt() > initialUpdatedAt);
  }

  @Test
  @DisplayName("Kiểm tra phương thức setCreatedAt")
  void setCreatedAt_SetsCreatedAt() {
    // Setup
    long newCreatedAt = Instant.now().toEpochMilli() + 1000;

    // Execute
    account.setCreatedAt(newCreatedAt);

    // Verify
    assertEquals(newCreatedAt, account.getCreatedAt());
  }

  @Test
  @DisplayName("Kiểm tra phương thức setUpdatedAt")
  void setUpdatedAt_SetsUpdatedAt() {
    // Setup
    long newUpdatedAt = Instant.now().toEpochMilli() + 1000;

    // Execute
    account.setUpdatedAt(newUpdatedAt);

    // Verify
    assertEquals(newUpdatedAt, account.getUpdatedAt());
  }

  @Test
  @DisplayName("Kiểm tra tính hợp lệ của tài khoản khi key rỗng")
  void validateRequiredFields_WithEmptyKey_ReturnsErrors() {
    // Setup
    account.setKey("");

    // Execute
    List<String> errors = account.validateRequiredFields();

    // Verify
    assertFalse(errors.isEmpty());
    assertTrue(errors.contains("Account Key cannot be null or empty"));
  }

  @Test
  @DisplayName("Kiểm tra tính hợp lệ của tài khoản khi key chỉ chứa khoảng trắng")
  void validateRequiredFields_WithWhitespaceKey_ReturnsErrors() {
    // Setup
    account.setKey("   ");

    // Execute
    List<String> errors = account.validateRequiredFields();

    // Verify
    assertFalse(errors.isEmpty());
    assertTrue(errors.contains("Account Key cannot be null or empty"));
  }

  @Test
  @DisplayName("Giảm số dư khả dụng với số lượng bằng 0")
  void decreaseAvailableBalance_WithZeroAmount_DecreasesNothing() {
    // Setup
    BigDecimal initialBalance = new BigDecimal("10.0");
    BigDecimal decreaseAmount = BigDecimal.ZERO;
    account.setAvailableBalance(initialBalance);
    long initialUpdatedAt = account.getUpdatedAt();

    // Wait to ensure timestamp changes
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Execute
    account.decreaseAvailableBalance(decreaseAmount);

    // Verify
    assertEquals(initialBalance.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getAvailableBalance());
    assertTrue(account.getUpdatedAt() > initialUpdatedAt);
  }

  @Test
  @DisplayName("Giảm số dư khả dụng với số lượng bằng chính số dư hiện tại")
  void decreaseAvailableBalance_WithExactAmount_ZerosBalance() {
    // Setup
    BigDecimal initialBalance = new BigDecimal("10.0");
    account.setAvailableBalance(initialBalance);

    // Execute
    account.decreaseAvailableBalance(initialBalance);

    // Verify
    assertEquals(BigDecimal.ZERO.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getAvailableBalance());
  }

  @Test
  @DisplayName("Chuyển từ available sang frozen với số lượng 0")
  void increaseFrozenBalance_WithZeroAmount_NoChange() {
    // Setup
    BigDecimal initialAvailable = new BigDecimal("10.0");
    BigDecimal initialFrozen = new BigDecimal("5.0");
    BigDecimal amount = BigDecimal.ZERO;
    account.setAvailableBalance(initialAvailable);
    account.setFrozenBalance(initialFrozen);

    // Execute
    account.increaseFrozenBalance(amount, "txn123");

    // Verify
    assertEquals(initialAvailable.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getAvailableBalance());
    assertEquals(initialFrozen.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getFrozenBalance());
  }

  @Test
  @DisplayName("Chuyển từ available sang frozen với số lượng bằng đúng số dư khả dụng")
  void increaseFrozenBalance_WithExactAvailableAmount_ZerosAvailableBalance() {
    // Setup
    BigDecimal initialAvailable = new BigDecimal("10.0");
    BigDecimal initialFrozen = new BigDecimal("5.0");
    account.setAvailableBalance(initialAvailable);
    account.setFrozenBalance(initialFrozen);

    // Execute
    account.increaseFrozenBalance(initialAvailable, "txn123");

    // Verify
    assertEquals(BigDecimal.ZERO.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getAvailableBalance());
    assertEquals(new BigDecimal("15.0").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP), account.getFrozenBalance());
  }

  @Test
  @DisplayName("Giảm số dư khả dụng với số lượng null - Kiểm tra rõ ràng lỗi exception được ném")
  void decreaseAvailableBalance_WithNullAmount_ThrowsExceptionExplicit() {
    try {
      account.decreaseAvailableBalance(null);
      fail("Phải ném ra IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals("Amount cannot be null", e.getMessage());
    }
  }

  @Test
  @DisplayName("Giảm số dư khả dụng với số lượng âm - Kiểm tra rõ ràng lỗi exception được ném")
  void decreaseAvailableBalance_WithNegativeAmount_ThrowsExceptionExplicit() {
    try {
      account.decreaseAvailableBalance(new BigDecimal("-1.0"));
      fail("Phải ném ra IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals("Amount must be greater than or equal to zero", e.getMessage());
    }
  }

  @Test
  @DisplayName("Chuyển từ available sang frozen với số lượng null - Kiểm tra rõ ràng lỗi exception được ném")
  void increaseFrozenBalance_WithIdentifierAndNullAmount_ThrowsExceptionExplicit() {
    try {
      account.increaseFrozenBalance(null, "txn123");
      fail("Phải ném ra IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals("Amount cannot be null", e.getMessage());
    }
  }

  @Test
  @DisplayName("Chuyển từ available sang frozen với số lượng âm - Kiểm tra rõ ràng lỗi exception được ném")
  void increaseFrozenBalance_WithIdentifierAndNegativeAmount_ThrowsExceptionExplicit() {
    try {
      account.increaseFrozenBalance(new BigDecimal("-1.0"), "txn123");
      fail("Phải ném ra IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals("Amount must be greater than or equal to zero", e.getMessage());
    }
  }
}
