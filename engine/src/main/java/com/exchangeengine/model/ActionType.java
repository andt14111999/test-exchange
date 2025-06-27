package com.exchangeengine.model;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Enum for action types
 */
public enum ActionType {
  COIN_TRANSACTION("CoinTransaction"),
  FIAT_TRANSACTION("FiatTransaction"),
  COIN_ACCOUNT("CoinAccount"),
  AMM_POOL("AmmPool"),
  AMM_POSITION("AmmPosition"),
  MERCHANT_ESCROW("MerchantEscrow"),
  AMM_ORDER("AmmOrder"),
  TRADE("Trade"),
  OFFER("Offer"),
  BALANCES_LOCK("BalancesLock");

  private final String value;

  ActionType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  /**
   * Get ActionType from string value
   *
   * @param value String value
   * @return ActionType or null if not found
   */
  public static ActionType fromValue(String value) {
    for (ActionType type : ActionType.values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    return null;
  }

  /**
   * Check if operation type is equal to string value
   *
   * @param value String value
   * @return true if operation type is equal to string value
   */
  public boolean isEqualTo(String value) {
    return this.value.equalsIgnoreCase(value);
  }

  /**
   * Check if operation type is supported
   *
   * @param value String value
   * @return true if operation type is supported
   */
  public static boolean isSupported(String value) {
    return fromValue(value) != null;
  }

  public static String getSupportedValues() {
    return Arrays.stream(ActionType.values())
        .map(ActionType::getValue)
        .collect(Collectors.joining(", "));
  }
}
