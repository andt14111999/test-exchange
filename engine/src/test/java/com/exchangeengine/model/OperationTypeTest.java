package com.exchangeengine.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OperationTypeTest {

  @Test
  @DisplayName("getValue() should return the correct string value")
  void getValue_ReturnsCorrectValue() {
    assertEquals("coin_deposit_create", OperationType.COIN_DEPOSIT_CREATE.getValue());
    assertEquals("coin_withdrawal_create", OperationType.COIN_WITHDRAWAL_CREATE.getValue());
    assertEquals("coin_withdrawal_releasing", OperationType.COIN_WITHDRAWAL_RELEASING.getValue());
    assertEquals("coin_withdrawal_failed", OperationType.COIN_WITHDRAWAL_FAILED.getValue());
    assertEquals("coin_account_create", OperationType.COIN_ACCOUNT_CREATE.getValue());
    assertEquals("balance_query", OperationType.BALANCE_QUERY.getValue());
    assertEquals("balance_reset", OperationType.BALANCE_RESET.getValue());
    assertEquals("amm_pool_create", OperationType.AMM_POOL_CREATE.getValue());
    assertEquals("amm_pool_update", OperationType.AMM_POOL_UPDATE.getValue());
    assertEquals("amm_position_create", OperationType.AMM_POSITION_CREATE.getValue());
    assertEquals("amm_position_collect_fee", OperationType.AMM_POSITION_COLLECT_FEE.getValue());
    assertEquals("amm_position_close", OperationType.AMM_POSITION_CLOSE.getValue());
    assertEquals("tick_query", OperationType.TICK_QUERY.getValue());
    assertEquals("balances_lock_create", OperationType.BALANCES_LOCK_CREATE.getValue());
    assertEquals("balances_lock_release", OperationType.BALANCES_LOCK_RELEASE.getValue());
  }

  @Test
  @DisplayName("COIN_DEPOSIT_OPERATIONS should contain correct operations")
  void coinDepositOperations_ContainsCorrectOperations() {
    List<OperationType> expected = Arrays.asList(OperationType.COIN_DEPOSIT_CREATE);
    assertEquals(expected, OperationType.COIN_DEPOSIT_OPERATIONS);
  }

  @Test
  @DisplayName("COIN_WITHDRAWAL_OPERATIONS should contain correct operations")
  void coinWithdrawalOperations_ContainsCorrectOperations() {
    List<OperationType> expected = Arrays.asList(
        OperationType.COIN_WITHDRAWAL_CREATE,
        OperationType.COIN_WITHDRAWAL_RELEASING,
        OperationType.COIN_WITHDRAWAL_FAILED,
        OperationType.COIN_WITHDRAWAL_CANCELLED);

    assertEquals(expected, OperationType.COIN_WITHDRAWAL_OPERATIONS);
    assertEquals(4, OperationType.COIN_WITHDRAWAL_OPERATIONS.size());
  }

  @Test
  @DisplayName("COIN_ACCOUNT_OPERATIONS should contain correct operations")
  void coinAccountOperations_ContainsCorrectOperations() {
    List<OperationType> expected = Arrays.asList(
        OperationType.COIN_ACCOUNT_CREATE,
        OperationType.BALANCE_QUERY,
        OperationType.BALANCE_RESET);

    assertEquals(expected, OperationType.COIN_ACCOUNT_OPERATIONS);
    assertEquals(3, OperationType.COIN_ACCOUNT_OPERATIONS.size());
  }

  @Test
  @DisplayName("AMM_POOL_OPERATIONS should contain correct operations")
  void ammPoolOperations_ContainsCorrectOperations() {
    List<OperationType> expected = Arrays.asList(
        OperationType.AMM_POOL_CREATE,
        OperationType.AMM_POOL_UPDATE);

    assertEquals(expected, OperationType.AMM_POOL_OPERATIONS);
    assertEquals(2, OperationType.AMM_POOL_OPERATIONS.size());
  }

  @Test
  @DisplayName("AMM_POSITION_OPERATIONS should contain correct operations")
  void ammPositionOperations_ContainsCorrectOperations() {
    List<OperationType> expected = Arrays.asList(
        OperationType.AMM_POSITION_CREATE,
        OperationType.AMM_POSITION_COLLECT_FEE,
        OperationType.AMM_POSITION_CLOSE);

    assertEquals(expected, OperationType.AMM_POSITION_OPERATIONS);
    assertEquals(3, OperationType.AMM_POSITION_OPERATIONS.size());
  }

  @Test
  @DisplayName("BALANCES_LOCK_OPERATIONS should contain correct operations")
  void balancesLockOperations_ContainsCorrectOperations() {
    List<OperationType> expected = Arrays.asList(
        OperationType.BALANCES_LOCK_CREATE,
        OperationType.BALANCES_LOCK_RELEASE);

    assertEquals(expected, OperationType.BALANCES_LOCK_OPERATIONS);
    assertEquals(2, OperationType.BALANCES_LOCK_OPERATIONS.size());
  }

  @Test
  @DisplayName("isEqualTo() should correctly compare string value to enum (case insensitive)")
  void isEqualTo_ComparesCorrectly() {
    assertTrue(OperationType.COIN_DEPOSIT_CREATE.isEqualTo("coin_deposit_create"));
    assertTrue(OperationType.COIN_DEPOSIT_CREATE.isEqualTo("COIN_DEPOSIT_CREATE"));
    assertTrue(OperationType.BALANCE_QUERY.isEqualTo("balance_query"));
    assertTrue(OperationType.BALANCE_QUERY.isEqualTo("BALANCE_QUERY"));
    assertTrue(OperationType.AMM_POOL_CREATE.isEqualTo("amm_pool_create"));
    assertTrue(OperationType.AMM_POSITION_CREATE.isEqualTo("amm_position_create"));

    assertFalse(OperationType.COIN_DEPOSIT_CREATE.isEqualTo("coin_deposit"));
    assertFalse(OperationType.BALANCE_QUERY.isEqualTo("balance"));
    assertFalse(OperationType.AMM_POOL_CREATE.isEqualTo("amm_pool"));
    assertFalse(OperationType.AMM_POSITION_CREATE.isEqualTo("amm_position"));
  }

  @Test
  @DisplayName("fromValue() should return correct enum for valid string values (case insensitive)")
  void fromValue_WithValidValue_ReturnsCorrectEnum() {
    assertEquals(OperationType.COIN_DEPOSIT_CREATE, OperationType.fromValue("coin_deposit_create"));
    assertEquals(OperationType.COIN_DEPOSIT_CREATE, OperationType.fromValue("COIN_DEPOSIT_CREATE"));
    assertEquals(OperationType.COIN_WITHDRAWAL_CREATE, OperationType.fromValue("coin_withdrawal_create"));
    assertEquals(OperationType.COIN_WITHDRAWAL_RELEASING, OperationType.fromValue("coin_withdrawal_releasing"));
    assertEquals(OperationType.COIN_WITHDRAWAL_FAILED, OperationType.fromValue("coin_withdrawal_failed"));
    assertEquals(OperationType.COIN_WITHDRAWAL_CANCELLED, OperationType.fromValue("coin_withdrawal_cancelled"));
    assertEquals(OperationType.COIN_ACCOUNT_CREATE, OperationType.fromValue("coin_account_create"));
    assertEquals(OperationType.BALANCE_QUERY, OperationType.fromValue("balance_query"));
    assertEquals(OperationType.BALANCE_RESET, OperationType.fromValue("balance_reset"));
    assertEquals(OperationType.AMM_POOL_CREATE, OperationType.fromValue("amm_pool_create"));
    assertEquals(OperationType.AMM_POOL_UPDATE, OperationType.fromValue("amm_pool_update"));
    assertEquals(OperationType.AMM_POSITION_CREATE, OperationType.fromValue("amm_position_create"));
    assertEquals(OperationType.AMM_POSITION_COLLECT_FEE, OperationType.fromValue("amm_position_collect_fee"));
    assertEquals(OperationType.AMM_POSITION_CLOSE, OperationType.fromValue("amm_position_close"));
    assertEquals(OperationType.TICK_QUERY, OperationType.fromValue("tick_query"));
    assertEquals(OperationType.TICK_QUERY, OperationType.fromValue("TICK_QUERY"));
    assertEquals(OperationType.BALANCES_LOCK_CREATE, OperationType.fromValue("balances_lock_create"));
    assertEquals(OperationType.BALANCES_LOCK_RELEASE, OperationType.fromValue("balances_lock_release"));
  }

  @Test
  @DisplayName("fromValue() should return null for invalid values")
  void fromValue_WithInvalidValue_ReturnsNull() {
    assertNull(OperationType.fromValue(null));
    assertNull(OperationType.fromValue(""));
    assertNull(OperationType.fromValue("unknown"));
    assertNull(OperationType.fromValue("invalid"));
    assertNull(OperationType.fromValue("123"));
  }

  @Test
  @DisplayName("isSupported() should correctly identify supported values")
  void isSupported_CorrectlyIdentifiesSupportedValues() {
    assertTrue(OperationType.isSupported("coin_deposit_create"));
    assertTrue(OperationType.isSupported("COIN_DEPOSIT_CREATE"));
    assertTrue(OperationType.isSupported("coin_withdrawal_create"));
    assertTrue(OperationType.isSupported("balance_query"));
    assertTrue(OperationType.isSupported("amm_pool_create"));
    assertTrue(OperationType.isSupported("amm_position_create"));
    assertTrue(OperationType.isSupported("tick_query"));
    assertTrue(OperationType.isSupported("TICK_QUERY"));
    assertTrue(OperationType.isSupported("balances_lock_create"));
    assertTrue(OperationType.isSupported("balances_lock_release"));

    assertFalse(OperationType.isSupported("unknown"));
    assertFalse(OperationType.isSupported("invalid"));
    assertFalse(OperationType.isSupported("123"));
  }

  @Test
  @DisplayName("getSupportedValues() should return all values separated by comma")
  void getSupportedValues_ReturnsAllValuesSeparatedByComma() {
    String supportedValues = OperationType.getSupportedValues();

    // Verify the result contains all operation types
    assertTrue(supportedValues.contains("coin_deposit_create"));
    assertTrue(supportedValues.contains("coin_withdrawal_create"));
    assertTrue(supportedValues.contains("coin_withdrawal_releasing"));
    assertTrue(supportedValues.contains("coin_withdrawal_failed"));
    assertTrue(supportedValues.contains("coin_withdrawal_cancelled"));
    assertTrue(supportedValues.contains("coin_account_create"));
    assertTrue(supportedValues.contains("balance_query"));
    assertTrue(supportedValues.contains("balance_reset"));
    assertTrue(supportedValues.contains("amm_pool_create"));
    assertTrue(supportedValues.contains("amm_pool_update"));
    assertTrue(supportedValues.contains("amm_position_create"));
    assertTrue(supportedValues.contains("amm_position_collect_fee"));
    assertTrue(supportedValues.contains("amm_position_close"));
    assertTrue(supportedValues.contains("tick_query"));
    assertTrue(supportedValues.contains("balances_lock_create"));
    assertTrue(supportedValues.contains("balances_lock_release"));

    // Check format: values separated by comma and space
    String[] parts = supportedValues.split(", ");
    assertEquals(OperationType.values().length, parts.length);
  }

  @Test
  @DisplayName("getSupportedAccountValues() should return account-related values separated by comma")
  void getSupportedAccountValues_ReturnsAccountValuesSeparatedByComma() {
    String supportedValues = OperationType.getSupportedAccountValues();

    // Verify the result contains all account-related operation types
    assertTrue(supportedValues.contains("coin_account_create"));
    assertTrue(supportedValues.contains("balance_query"));
    assertTrue(supportedValues.contains("balance_reset"));

    // Check format: values separated by comma and space
    String[] parts = supportedValues.split(", ");
    assertEquals(OperationType.COIN_ACCOUNT_OPERATIONS.size(), parts.length);
  }

  @Test
  @DisplayName("getSupportedAmmPoolValues() should return AMM pool-related values separated by comma")
  void getSupportedAmmPoolValues_ReturnsAmmPoolValuesSeparatedByComma() {
    String supportedValues = OperationType.getSupportedAmmPoolValues();

    // Verify the result contains all AMM pool-related operation types
    assertTrue(supportedValues.contains("amm_pool_create"));
    assertTrue(supportedValues.contains("amm_pool_update"));

    // Check format: values separated by comma and space
    String[] parts = supportedValues.split(", ");
    assertEquals(OperationType.AMM_POOL_OPERATIONS.size(), parts.length);
  }

  @Test
  @DisplayName("getSupportedAmmPositionValues() should return AMM position-related values separated by comma")
  void getSupportedAmmPositionValues_ReturnsAmmPositionValuesSeparatedByComma() {
    String supportedValues = OperationType.getSupportedAmmPositionValues();

    // Verify the result contains all AMM position-related operation types
    assertTrue(supportedValues.contains("amm_position_create"));
    assertTrue(supportedValues.contains("amm_position_collect_fee"));
    assertTrue(supportedValues.contains("amm_position_close"));

    // Check format: values separated by comma and space
    String[] parts = supportedValues.split(", ");
    assertEquals(OperationType.AMM_POSITION_OPERATIONS.size(), parts.length);
  }

  @Test
  @DisplayName("getSupportedBalancesLockValues() should return BalancesLock-related values separated by comma")
  void getSupportedBalancesLockValues_ReturnsBalancesLockValuesSeparatedByComma() {
    String supportedValues = OperationType.getSupportedBalancesLockValues();

    // Verify the result contains all BalancesLock-related operation types
    assertTrue(supportedValues.contains("balances_lock_create"));
    assertTrue(supportedValues.contains("balances_lock_release"));

    // Check format: values separated by comma and space
    String[] parts = supportedValues.split(", ");
    assertEquals(OperationType.BALANCES_LOCK_OPERATIONS.size(), parts.length);
  }
}
