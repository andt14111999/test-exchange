package com.exchangeengine.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActionTypeTest {

  @Test
  @DisplayName("getValue() should return the correct string value")
  void getValue_ReturnsCorrectValue() {
    assertEquals("CoinTransaction", ActionType.COIN_TRANSACTION.getValue());
    assertEquals("FiatTransaction", ActionType.FIAT_TRANSACTION.getValue());
    assertEquals("CoinAccount", ActionType.COIN_ACCOUNT.getValue());
    assertEquals("AmmPool", ActionType.AMM_POOL.getValue());
    assertEquals("AmmPosition", ActionType.AMM_POSITION.getValue());
    assertEquals("MerchantEscrow", ActionType.MERCHANT_ESCROW.getValue());
  }

  @Test
  @DisplayName("fromValue() should return correct enum for valid string values (case insensitive)")
  void fromValue_WithValidValue_ReturnsCorrectEnum() {
    assertEquals(ActionType.COIN_TRANSACTION, ActionType.fromValue("CoinTransaction"));
    assertEquals(ActionType.COIN_TRANSACTION, ActionType.fromValue("COINTRANSACTION"));
    assertEquals(ActionType.COIN_TRANSACTION, ActionType.fromValue("cointransaction"));
    assertEquals(ActionType.FIAT_TRANSACTION, ActionType.fromValue("FiatTransaction"));
    assertEquals(ActionType.FIAT_TRANSACTION, ActionType.fromValue("fiattransaction"));
    assertEquals(ActionType.COIN_ACCOUNT, ActionType.fromValue("CoinAccount"));
    assertEquals(ActionType.COIN_ACCOUNT, ActionType.fromValue("coinaccount"));
    assertEquals(ActionType.AMM_POSITION, ActionType.fromValue("AmmPosition"));
    assertEquals(ActionType.AMM_POSITION, ActionType.fromValue("ammposition"));
  }

  @Test
  @DisplayName("fromValue() should return null for invalid values")
  void fromValue_WithInvalidValue_ReturnsNull() {
    assertNull(ActionType.fromValue(null));
    assertNull(ActionType.fromValue(""));
    assertNull(ActionType.fromValue("unknown"));
    assertNull(ActionType.fromValue("invalid"));
    assertNull(ActionType.fromValue("123"));
  }

  @Test
  @DisplayName("isEqualTo() should correctly compare string value to enum (case insensitive)")
  void isEqualTo_ComparesCorrectly() {
    assertTrue(ActionType.COIN_TRANSACTION.isEqualTo("CoinTransaction"));
    assertTrue(ActionType.COIN_TRANSACTION.isEqualTo("cointransaction"));
    assertTrue(ActionType.FIAT_TRANSACTION.isEqualTo("FiatTransaction"));
    assertTrue(ActionType.FIAT_TRANSACTION.isEqualTo("fiattransaction"));
    assertTrue(ActionType.COIN_ACCOUNT.isEqualTo("CoinAccount"));
    assertTrue(ActionType.COIN_ACCOUNT.isEqualTo("coinaccount"));
    assertTrue(ActionType.AMM_POSITION.isEqualTo("AmmPosition"));
    assertTrue(ActionType.AMM_POSITION.isEqualTo("ammposition"));

    assertFalse(ActionType.COIN_TRANSACTION.isEqualTo("unknown"));
    assertFalse(ActionType.COIN_TRANSACTION.isEqualTo("coin"));
  }

  @Test
  @DisplayName("isSupported() should correctly identify supported values")
  void isSupported_CorrectlyIdentifiesSupportedValues() {
    assertTrue(ActionType.isSupported("CoinTransaction"));
    assertTrue(ActionType.isSupported("cointransaction"));
    assertTrue(ActionType.isSupported("FiatTransaction"));
    assertTrue(ActionType.isSupported("fiattransaction"));
    assertTrue(ActionType.isSupported("CoinAccount"));
    assertTrue(ActionType.isSupported("coinaccount"));
    assertTrue(ActionType.isSupported("AmmPosition"));
    assertTrue(ActionType.isSupported("ammposition"));

    assertFalse(ActionType.isSupported("unknown"));
    assertFalse(ActionType.isSupported("invalid"));
    assertFalse(ActionType.isSupported("123"));
  }

  @Test
  @DisplayName("getSupportedValues() should return all values separated by comma")
  void getSupportedValues_ReturnsAllValuesSeparatedByComma() {
    String supportedValues = ActionType.getSupportedValues();
    assertTrue(supportedValues.contains("CoinTransaction"));
    assertTrue(supportedValues.contains("FiatTransaction"));
    assertTrue(supportedValues.contains("CoinAccount"));
    assertTrue(supportedValues.contains("AmmPosition"));

    // Check format: values separated by comma and space
    String[] parts = supportedValues.split(", ");
    assertEquals(ActionType.values().length, parts.length);
  }
}
