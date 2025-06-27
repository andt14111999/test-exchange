package com.exchangeengine.model;

import com.exchangeengine.factory.CoinTransactionFactory;
import com.exchangeengine.factory.CoinTransactionFactory.TestCoinTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CoinTransactionTest {

  private static final String ACCOUNT_KEY = "usdt:user123";
  private static final String TRANSACTION_ID = "txn123";
  private static final BigDecimal AMOUNT = new BigDecimal("2.5");
  private static final int COIN_DEFAULT_SCALE = 16;

  @Test
  @DisplayName("Constructor should initialize action type and timestamps")
  void constructor_ShouldInitializeActionTypeAndTimestamps() {
    TestCoinTransaction transaction = new TestCoinTransaction();
    assertEquals(ActionType.COIN_TRANSACTION.getValue(), transaction.getActionType());
    assertTrue(transaction.getCreatedAt() > 0);
    assertEquals(transaction.getCreatedAt(), transaction.getUpdatedAt());
  }

  @Test
  @DisplayName("setAmount should scale the amount to the default scale")
  void setAmount_ShouldScaleAmountToDefaultScale() {
    TestCoinTransaction transaction = new TestCoinTransaction();
    transaction.setAmount(new BigDecimal("2.5"));
    assertEquals(COIN_DEFAULT_SCALE, transaction.getAmount().scale());
  }

  @Test
  @DisplayName("setCoin should convert to lowercase and trim")
  void setCoin_ShouldConvertToLowercaseAndTrim() {
    TestCoinTransaction transaction = new TestCoinTransaction();
    transaction.setCoin("  USDT  ");
    assertEquals("usdt", transaction.getCoin());
  }

  @Test
  @DisplayName("validateRequiredFields should return errors when amount is null")
  void validateRequiredFields_ShouldReturnErrors_WhenAmountIsNull() {
    TestCoinTransaction transaction = CoinTransactionFactory.createWithNullAmount(ACCOUNT_KEY, TRANSACTION_ID);

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Amount is required"));
  }

  @Test
  @DisplayName("validateRequiredFields should return errors when amount is zero")
  void validateRequiredFields_ShouldReturnErrors_WhenAmountIsZero() {
    TestCoinTransaction transaction = CoinTransactionFactory.createWithNonPositiveAmount(
        ACCOUNT_KEY, TRANSACTION_ID, BigDecimal.ZERO);

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Amount must be greater than 0"));
  }

  @Test
  @DisplayName("validateRequiredFields should return errors when amount is negative")
  void validateRequiredFields_ShouldReturnErrors_WhenAmountIsNegative() {
    TestCoinTransaction transaction = CoinTransactionFactory.createWithNonPositiveAmount(
        ACCOUNT_KEY, TRANSACTION_ID, new BigDecimal("-1.0"));

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Amount must be greater than 0"));
  }

  @Test
  @DisplayName("validateRequiredFields should return errors when coin is null")
  void validateRequiredFields_ShouldReturnErrors_WhenCoinIsNull() {
    TestCoinTransaction transaction = CoinTransactionFactory.createWithNullCoin(
        ACCOUNT_KEY, TRANSACTION_ID, AMOUNT);

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Coin is required"));
  }

  @Test
  @DisplayName("validateRequiredFields should return errors when coin is empty")
  void validateRequiredFields_ShouldReturnErrors_WhenCoinIsEmpty() {
    TestCoinTransaction transaction = CoinTransactionFactory.createWithEmptyCoin(
        ACCOUNT_KEY, TRANSACTION_ID, AMOUNT);

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Coin is required"));
  }

  @Test
  @DisplayName("validateRequiredFields should return errors when coin is invalid")
  void validateRequiredFields_ShouldReturnErrors_WhenCoinIsInvalid() {
    TestCoinTransaction transaction = CoinTransactionFactory.createWithInvalidCoin(
        ACCOUNT_KEY, TRANSACTION_ID, AMOUNT);

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Unsupported coin: INVALID_COIN"));
  }

  @Test
  @DisplayName("validateRequiredFields should return errors when txHash is null")
  void validateRequiredFields_ShouldReturnErrors_WhenTxHashIsNull() {
    TestCoinTransaction transaction = CoinTransactionFactory.createWithNullTxHash(
        ACCOUNT_KEY, TRANSACTION_ID, AMOUNT);

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Transaction hash is required"));
  }

  @Test
  @DisplayName("validateRequiredFields should return errors when txHash is empty")
  void validateRequiredFields_ShouldReturnErrors_WhenTxHashIsEmpty() {
    TestCoinTransaction transaction = CoinTransactionFactory.createWithEmptyTxHash(
        ACCOUNT_KEY, TRANSACTION_ID, AMOUNT);

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Transaction hash is required"));
  }

  @Test
  @DisplayName("validateRequiredFields should return errors when layer is null")
  void validateRequiredFields_ShouldReturnErrors_WhenLayerIsNull() {
    TestCoinTransaction transaction = CoinTransactionFactory.createWithNullLayer(
        ACCOUNT_KEY, TRANSACTION_ID, AMOUNT);

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Layer is required"));
  }

  @Test
  @DisplayName("validateRequiredFields should return errors when layer is empty")
  void validateRequiredFields_ShouldReturnErrors_WhenLayerIsEmpty() {
    TestCoinTransaction transaction = CoinTransactionFactory.createWithEmptyLayer(
        ACCOUNT_KEY, TRANSACTION_ID, AMOUNT);

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Layer is required"));
  }

  @Test
  @DisplayName("validateRequiredFields should return no errors when all fields are valid")
  void validateRequiredFields_ShouldReturnNoErrors_WhenAllFieldsAreValid() {
    TestCoinTransaction transaction = CoinTransactionFactory.create(
        ACCOUNT_KEY, TRANSACTION_ID, AMOUNT);

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.isEmpty());
  }

  @Test
  @DisplayName("toString should include all relevant fields")
  void toString_ShouldIncludeAllRelevantFields() {
    TestCoinTransaction transaction = CoinTransactionFactory.create(
        ACCOUNT_KEY, TRANSACTION_ID, AMOUNT);

    String toString = transaction.toString();

    assertTrue(toString.contains("amount="), "Should contain amount field");
    assertTrue(toString.contains("coin="), "Should contain coin field");
    assertTrue(toString.contains("txHash="), "Should contain txHash field");
    assertTrue(toString.contains("layer="), "Should contain layer field");
  }
}
