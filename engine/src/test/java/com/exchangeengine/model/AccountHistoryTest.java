package com.exchangeengine.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountHistoryTest {

  private static final String ACCOUNT_KEY = "btc:user123";
  private static final String IDENTIFIER = "txn123";
  private static final String OPERATION_TYPE = OperationType.COIN_DEPOSIT_CREATE.getValue();

  @Test
  @DisplayName("No-arg constructor should initialize timestamp")
  void noArgConstructor_InitializesTimestamp() {
    AccountHistory history = new AccountHistory();

    assertNotNull(history);
    assertTrue(history.getTimestamp() > 0);
  }

  @Test
  @DisplayName("Constructor with required parameters should initialize fields correctly")
  void constructor_WithRequiredParameters_InitializesFieldsCorrectly() {
    AccountHistory history = new AccountHistory(ACCOUNT_KEY, IDENTIFIER, OPERATION_TYPE);

    assertEquals(ACCOUNT_KEY, history.getAccountKey());
    assertEquals(IDENTIFIER, history.getIdentifier());
    assertEquals(OPERATION_TYPE, history.getOperationType());
    assertTrue(history.getTimestamp() > 0);

    // Check key format
    String expectedKeyPrefix = AccountHistory.generateHashedPrefix(ACCOUNT_KEY) + "-" + ACCOUNT_KEY + "-" + IDENTIFIER;
    assertTrue(history.getKey().startsWith(expectedKeyPrefix));
  }

  @Test
  @DisplayName("generateHashedPrefix should generate 8-character hex prefix from accountKey")
  void generateHashedPrefix_GeneratesHexPrefixFromAccountKey() {
    String prefix = AccountHistory.generateHashedPrefix(ACCOUNT_KEY);

    assertNotNull(prefix);
    assertEquals(8, prefix.length());

    // Test consistency
    String prefix2 = AccountHistory.generateHashedPrefix(ACCOUNT_KEY);
    assertEquals(prefix, prefix2);

    // Different keys should have different prefixes (usually)
    String otherPrefix = AccountHistory.generateHashedPrefix("eth:user123");
    assertNotEquals(prefix, otherPrefix);
  }

  @Test
  @DisplayName("generateKey should form key with correct format")
  void generateKey_FormsKeyWithCorrectFormat() {
    long timestamp = 1711767742000L; // Thursday, March 28, 2024 4:35:42 PM GMT
    String key = AccountHistory.generateKey(ACCOUNT_KEY, IDENTIFIER, timestamp);

    String expectedFormat = AccountHistory.generateHashedPrefix(ACCOUNT_KEY) + "-" + ACCOUNT_KEY + "-" + IDENTIFIER
        + "-" + timestamp;
    assertEquals(expectedFormat, key);
  }

  @Test
  @DisplayName("generateAccountPrefix should return 8-character hash prefix")
  void generateAccountPrefix_ReturnsHashPrefix() {
    String prefix = AccountHistory.generateAccountPrefix(ACCOUNT_KEY);

    assertEquals(8, prefix.length());
    assertEquals(AccountHistory.generateHashedPrefix(ACCOUNT_KEY), prefix);
  }

  @Test
  @DisplayName("setBalanceValues should format balances correctly")
  void setBalanceValues_FormatsBalancesCorrectly() {
    AccountHistory history = new AccountHistory(ACCOUNT_KEY, IDENTIFIER, OPERATION_TYPE);

    BigDecimal prevAvailable = new BigDecimal("100.0");
    BigDecimal newAvailable = new BigDecimal("110.0");
    BigDecimal prevFrozen = new BigDecimal("50.0");
    BigDecimal newFrozen = new BigDecimal("40.0");

    history.setBalanceValues(prevAvailable, newAvailable, prevFrozen, newFrozen);

    String expectedAvailable = "100.00000000|110.00000000|10.00000000";
    String expectedFrozen = "50.00000000|40.00000000|-10.00000000";

    assertEquals(expectedAvailable, history.getAvailableBalance());
    assertEquals(expectedFrozen, history.getFrozenBalance());
  }

  @Test
  @DisplayName("setBalanceInitAccount should set balances to zero")
  void setBalanceInitAccount_SetsBalancesToZero() {
    AccountHistory history = new AccountHistory(ACCOUNT_KEY, IDENTIFIER, OPERATION_TYPE);

    history.setBalanceInitAccount();

    String expected = "0.00000000|0.00000000|0.00000000";
    assertEquals(expected, history.getAvailableBalance());
    assertEquals(expected, history.getFrozenBalance());
  }

  @Test
  @DisplayName("setAvailableBalanceValues should set available balance correctly")
  void setAvailableBalanceValues_SetsAvailableBalanceCorrectly() {
    AccountHistory history = new AccountHistory(ACCOUNT_KEY, IDENTIFIER, OPERATION_TYPE);

    BigDecimal prev = new BigDecimal("100.0");
    BigDecimal newVal = new BigDecimal("150.0");

    history.setAvailableBalanceValues(prev, newVal);

    String expected = "100.00000000|150.00000000|50.00000000";
    assertEquals(expected, history.getAvailableBalance());
  }

  @Test
  @DisplayName("setFrozenBalanceValues should set frozen balance correctly")
  void setFrozenBalanceValues_SetsFrozenBalanceCorrectly() {
    AccountHistory history = new AccountHistory(ACCOUNT_KEY, IDENTIFIER, OPERATION_TYPE);

    BigDecimal prev = new BigDecimal("100.0");
    BigDecimal newVal = new BigDecimal("75.0");

    history.setFrozenBalanceValues(prev, newVal);

    String expected = "100.00000000|75.00000000|-25.00000000";
    assertEquals(expected, history.getFrozenBalance());
  }

  @Test
  @DisplayName("formatBalanceChange should format string with correct pattern")
  void formatBalanceChange_FormatsStringWithCorrectPattern() {
    BigDecimal prev = new BigDecimal("123.45");
    BigDecimal newVal = new BigDecimal("246.90");

    String result = AccountHistory.formatBalanceChange(prev, newVal);

    String expected = "123.45000000|246.90000000|123.45000000";
    assertEquals(expected, result);

    // Negative change
    BigDecimal prev2 = new BigDecimal("100.00");
    BigDecimal newVal2 = new BigDecimal("50.00");

    String result2 = AccountHistory.formatBalanceChange(prev2, newVal2);

    String expected2 = "100.00000000|50.00000000|-50.00000000";
    assertEquals(expected2, result2);
  }

  @Test
  @DisplayName("Getters and setters should function correctly")
  void gettersAndSetters_FunctionCorrectly() {
    AccountHistory history = new AccountHistory();

    // Set values
    String key = "testkey123";
    String operationType = "test_operation";
    String availableBalance = "10.00|20.00|10.00";
    String frozenBalance = "5.00|0.00|-5.00";
    long timestamp = 1711767742000L;

    history.setKey(key);
    history.setAccountKey(ACCOUNT_KEY);
    history.setIdentifier(IDENTIFIER);
    history.setOperationType(operationType);
    history.setAvailableBalance(availableBalance);
    history.setFrozenBalance(frozenBalance);
    history.setTimestamp(timestamp);

    // Verify values
    assertEquals(key, history.getKey());
    assertEquals(ACCOUNT_KEY, history.getAccountKey());
    assertEquals(IDENTIFIER, history.getIdentifier());
    assertEquals(operationType, history.getOperationType());
    assertEquals(availableBalance, history.getAvailableBalance());
    assertEquals(frozenBalance, history.getFrozenBalance());
    assertEquals(timestamp, history.getTimestamp());
  }

  @Test
  @DisplayName("toString should return string with all fields")
  void toString_ReturnsStringWithAllFields() {
    AccountHistory history = new AccountHistory(ACCOUNT_KEY, IDENTIFIER, OPERATION_TYPE);
    history.setBalanceValues(
        new BigDecimal("100.0"),
        new BigDecimal("110.0"),
        new BigDecimal("50.0"),
        new BigDecimal("40.0"));

    String result = history.toString();

    // Verify all fields are included
    assertTrue(result.contains(history.getKey()));
    assertTrue(result.contains(ACCOUNT_KEY));
    assertTrue(result.contains(IDENTIFIER));
    assertTrue(result.contains(OPERATION_TYPE));
    assertTrue(result.contains(history.getAvailableBalance()));
    assertTrue(result.contains(history.getFrozenBalance()));
    assertTrue(result.contains(String.valueOf(history.getTimestamp())));
  }
}
