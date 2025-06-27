package com.exchangeengine.model.event;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EventHandlerAction {
  public static final String UNKNOWN_EVENT = "UnknownEventHandler";
  public static final String ACCOUNT_EVENT = "AccountEventHandler";
  public static final String DEPOSIT_EVENT = "DepositEventHandler";
  public static final String WITHDRAWAL_EVENT = "WithdrawalEventHandler";
  public static final String AMM_POOL_EVENT = "AmmPoolEventHandler";
  public static final String MERCHANT_ESCROW_EVENT = "MerchantEscrowEventHandler";
  public static final String AMM_POSITION_EVENT = "AmmPositionEventHandler";
  public static final String AMM_ORDER_EVENT = "AmmOrderEventHandler";
  public static final String TRADE_EVENT = "TradeEventHandler";
  public static final String OFFER_EVENT = "OfferEventHandler";
  public static final String BALANCES_LOCK_EVENT = "BalancesLockEventHandler";
}
