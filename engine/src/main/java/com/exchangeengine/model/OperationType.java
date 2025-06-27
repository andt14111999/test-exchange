package com.exchangeengine.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enum for operation types
 */
public enum OperationType {
  COIN_DEPOSIT_CREATE("coin_deposit_create"),

  COIN_WITHDRAWAL_CREATE("coin_withdrawal_create"),
  COIN_WITHDRAWAL_RELEASING("coin_withdrawal_releasing"),
  COIN_WITHDRAWAL_FAILED("coin_withdrawal_failed"),
  COIN_WITHDRAWAL_CANCELLED("coin_withdrawal_cancelled"),

  COIN_ACCOUNT_CREATE("coin_account_create"),

  BALANCE_QUERY("balance_query"),
  BALANCE_RESET("balance_reset"),

  AMM_POOL_CREATE("amm_pool_create"),
  AMM_POOL_UPDATE("amm_pool_update"),

  MERCHANT_ESCROW_MINT("merchant_escrow_mint"),
  MERCHANT_ESCROW_BURN("merchant_escrow_burn"),

  AMM_POSITION_CREATE("amm_position_create"),
  AMM_POSITION_COLLECT_FEE("amm_position_collect_fee"),
  AMM_POSITION_CLOSE("amm_position_close"),

  AMM_ORDER_SWAP("amm_order_swap"),

  // Trade operations
  TRADE_CREATE("trade_create"),
  TRADE_COMPLETE("trade_complete"),
  TRADE_CANCEL("trade_cancel"),

  // Offer operations
  OFFER_CREATE("offer_create"),
  OFFER_UPDATE("offer_update"),
  OFFER_DISABLE("offer_disable"),
  OFFER_ENABLE("offer_enable"),
  OFFER_DELETE("offer_delete"),

  // Tick operations
  TICK_QUERY("tick_query"),
  
  // Balance lock operations
  BALANCES_LOCK_CREATE("balances_lock_create"),
  BALANCES_LOCK_RELEASE("balances_lock_release");

  private final String value;

  OperationType(String value) {
    this.value = value;
  }

  public static final List<OperationType> COIN_DEPOSIT_OPERATIONS = Arrays.asList(COIN_DEPOSIT_CREATE);

  public static final List<OperationType> COIN_WITHDRAWAL_OPERATIONS = Arrays.asList(
      COIN_WITHDRAWAL_CREATE,
      COIN_WITHDRAWAL_RELEASING,
      COIN_WITHDRAWAL_FAILED,
      COIN_WITHDRAWAL_CANCELLED);

  public static final List<OperationType> COIN_ACCOUNT_OPERATIONS = Arrays.asList(
      COIN_ACCOUNT_CREATE,
      BALANCE_QUERY,
      BALANCE_RESET);

  public static final List<OperationType> AMM_POOL_OPERATIONS = Arrays.asList(
      AMM_POOL_CREATE,
      AMM_POOL_UPDATE);

  public static final List<OperationType> MERCHANT_ESCROW_OPERATIONS = Arrays.asList(
      MERCHANT_ESCROW_MINT,
      MERCHANT_ESCROW_BURN);

  public static final List<OperationType> AMM_POSITION_OPERATIONS = Arrays.asList(
      AMM_POSITION_CREATE,
      AMM_POSITION_COLLECT_FEE,
      AMM_POSITION_CLOSE);

  public static final List<OperationType> TRADE_OPERATIONS = Arrays.asList(
      TRADE_CREATE,
      TRADE_COMPLETE,
      TRADE_CANCEL);

  public static final List<OperationType> OFFER_OPERATIONS = Arrays.asList(
      OFFER_CREATE,
      OFFER_UPDATE,
      OFFER_DISABLE,
      OFFER_ENABLE,
      OFFER_DELETE);
      
  public static final List<OperationType> BALANCES_LOCK_OPERATIONS = Arrays.asList(
      BALANCES_LOCK_CREATE,
      BALANCES_LOCK_RELEASE);

  public String getValue() {
    return value;
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
   * Get OperationType from string value
   *
   * @param value String value
   * @return OperationType or null if not found
   */
  public static OperationType fromValue(String value) {
    for (OperationType type : OperationType.values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    return null;
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
    return Arrays.stream(OperationType.values())
        .map(OperationType::getValue)
        .collect(Collectors.joining(", "));
  }

  public static String getSupportedAccountValues() {
    return COIN_ACCOUNT_OPERATIONS.stream()
        .map(OperationType::getValue)
        .collect(Collectors.joining(", "));
  }

  public static String getSupportedAmmPoolValues() {
    return AMM_POOL_OPERATIONS.stream()
        .map(OperationType::getValue)
        .collect(Collectors.joining(", "));
  }

  public static String getSupportedMerchantEscrowValues() {
    return MERCHANT_ESCROW_OPERATIONS.stream()
        .map(OperationType::getValue)
        .collect(Collectors.joining(", "));
  }

  public static String getSupportedAmmPositionValues() {
    return AMM_POSITION_OPERATIONS.stream()
        .map(OperationType::getValue)
        .collect(Collectors.joining(", "));
  }

  public static String getSupportedTradeValues() {
    return TRADE_OPERATIONS.stream()
        .map(OperationType::getValue)
        .collect(Collectors.joining(", "));
  }

  public static String getSupportedOfferValues() {
    return OFFER_OPERATIONS.stream()
        .map(OperationType::getValue)
        .collect(Collectors.joining(", "));
  }
  
  public static String getSupportedBalancesLockValues() {
    return BALANCES_LOCK_OPERATIONS.stream()
        .map(OperationType::getValue)
        .collect(Collectors.joining(", "));
  }
}
