package com.exchangeengine.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Model for Disruptor events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisruptorEvent {
  private boolean isSuccess = true;
  private String errorMessage;
  private long timestamp = Instant.now().toEpochMilli();

  private AccountEvent accountEvent;
  private CoinDepositEvent coinDepositEvent;
  private CoinWithdrawalEvent coinWithdrawalEvent;
  private AmmPoolEvent ammPoolEvent;
  private MerchantEscrowEvent merchantEscrowEvent;
  private AmmPositionEvent ammPositionEvent;
  private AmmOrderEvent ammOrderEvent;
  private TradeEvent tradeEvent;
  private OfferEvent offerEvent;
  private BalancesLockEvent balancesLockEvent;

  public void setErrorMessage(String errorMessage) {
    this.isSuccess = false;
    this.errorMessage = errorMessage;
  }

  public void successes() {
    this.isSuccess = true;
    this.errorMessage = null;
  }

  /**
   * Lấy event đầu tiên có giá trị không null
   *
   * @return BaseEvent đầu tiên có giá trị hoặc null nếu không có event nào
   */
  public BaseEvent getEvent() {
    if (accountEvent != null) {
      return accountEvent;
    } else if (coinDepositEvent != null) {
      return coinDepositEvent;
    } else if (coinWithdrawalEvent != null) {
      return coinWithdrawalEvent;
    } else if (ammPoolEvent != null) {
      return ammPoolEvent;
    } else if (merchantEscrowEvent != null) {
      return merchantEscrowEvent;
    } else if (ammPositionEvent != null) {
      return ammPositionEvent;
    } else if (ammOrderEvent != null) {
      return ammOrderEvent;
    } else if (tradeEvent != null) {
      return tradeEvent;
    } else if (offerEvent != null) {
      return offerEvent;
    } else if (balancesLockEvent != null) {
      return balancesLockEvent;
    }

    return new BaseEvent();
  }

  public String getEventId() {
    BaseEvent event = getEvent();
    return event != null ? event.getEventId() : null;
  }

  public String getProducerKey() {
    BaseEvent event = getEvent();
    if (event == null) {
      return UUID.randomUUID().toString();
    }

    return event.getProducerEventId();
  }

  public Map<String, Object> toOperationObjectMessageJson() {
    BaseEvent event = getEvent();
    Map<String, Object> objectMessageJson = new HashMap<>();
    if (event != null) {
      objectMessageJson = event.toOperationObjectMessageJson();
    }

    Map<String, Object> messageJson = new HashMap<>();
    messageJson.put("isSuccess", isSuccess);
    messageJson.put("errorMessage", errorMessage);
    messageJson.put("timestamp", timestamp);
    messageJson.put("inputEventId", getEventId());
    messageJson.put("object", objectMessageJson.get("object"));

    return messageJson;
  }

  public Map<String, Object> toDisruptorMessageJson() {
    Map<String, Object> messageJson = new HashMap<>();
    messageJson.put("eventId", getEventId());
    messageJson.put("isSuccess", isSuccess);
    messageJson.put("errorMessage", errorMessage);
    messageJson.put("timestamp", timestamp);

    for (Map.Entry<String, Object> entry : getEvent().toBaseMessageJson().entrySet()) {
      messageJson.put(entry.getKey(), entry.getValue());
    }

    return messageJson;
  }

  /**
   * Kiểm tra tính hợp lệ của event
   *
   * @throws IllegalArgumentException nếu không có event nào hoặc event không hợp
   *                                  lệ
   */
  public void validate() {
    BaseEvent event = getEvent();

    if (event == null) {
      throw new IllegalArgumentException("No event specified in DisruptorEvent");
    }

    // Check if we only have a default BaseEvent (not a specific event type)
    boolean isDefaultBaseEvent = (event.getClass() == BaseEvent.class);

    if (isDefaultBaseEvent) {
      throw new IllegalArgumentException("No event specified in DisruptorEvent");
    }

    if (event instanceof AccountEvent) {
      ((AccountEvent) event).validate();
    } else if (event instanceof CoinDepositEvent) {
      ((CoinDepositEvent) event).validate();
    } else if (event instanceof CoinWithdrawalEvent) {
      ((CoinWithdrawalEvent) event).validate();
    } else if (event instanceof AmmPoolEvent) {
      ((AmmPoolEvent) event).validate();
    } else if (event instanceof MerchantEscrowEvent) {
      ((MerchantEscrowEvent) event).validate();
    } else if (event instanceof AmmPositionEvent) {
      ((AmmPositionEvent) event).validate();
    } else if (event instanceof AmmOrderEvent) {
      ((AmmOrderEvent) event).validate();
    } else if (event instanceof TradeEvent) {
      ((TradeEvent) event).validate();
    } else if (event instanceof OfferEvent) {
      ((OfferEvent) event).validate();
    } else if (event instanceof BalancesLockEvent) {
      ((BalancesLockEvent) event).validate();
    } else {
      throw new IllegalArgumentException("Unknown event type: " + event.getClass().getSimpleName());
    }
  }

  public DisruptorEvent copyFrom(DisruptorEvent source) {
    if (source == null) {
      return this;
    }

    this.isSuccess = source.isSuccess();
    this.errorMessage = source.getErrorMessage();
    this.timestamp = source.getTimestamp();
    this.accountEvent = source.getAccountEvent();
    this.coinDepositEvent = source.getCoinDepositEvent();
    this.coinWithdrawalEvent = source.getCoinWithdrawalEvent();
    this.ammPoolEvent = source.getAmmPoolEvent();
    this.merchantEscrowEvent = source.getMerchantEscrowEvent();
    this.ammPositionEvent = source.getAmmPositionEvent();
    this.ammOrderEvent = source.getAmmOrderEvent();
    this.tradeEvent = source.getTradeEvent();
    this.offerEvent = source.getOfferEvent();
    this.balancesLockEvent = source.getBalancesLockEvent();

    return this;
  }
}
