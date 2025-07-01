package com.exchangeengine.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.CoinDeposit;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.DepositCache;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Model for Coin Deposit events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoinDepositEvent extends BaseEvent {
  private String identifier;
  private String accountKey;
  private BigDecimal amount;
  private String coin;
  private String txHash;
  private String layer;
  private String depositAddress;
  private String status;
  private String statusExplanation;

  protected DepositCache getDepositCache() {
    return DepositCache.getInstance();
  }

  @Override
  public String getProducerEventId() {
    return this.identifier;
  }

  @Override
  public String getEventHandler() {
    return EventHandlerAction.DEPOSIT_EVENT;
  }

  public Optional<CoinDeposit> fetchCoinDeposit(boolean raiseException) {
    Optional<CoinDeposit> deposit = getDepositCache().getDeposit(identifier);
    if (raiseException && !deposit.isPresent()) {
      throw new IllegalStateException("CoinDeposit not found identifier: " + identifier);
    }
    return deposit;
  }

  public CoinDeposit toCoinDeposit(boolean raiseException) {
    CoinDeposit deposit = fetchCoinDeposit(raiseException).orElseGet(() -> new CoinDeposit(
        getActionType(),
        getActionId(),
        identifier,
        status,
        accountKey,
        amount,
        coin,
        txHash,
        layer,
        depositAddress));
    return deposit;
  }

  public CoinDepositEvent parserData(JsonNode messageJson) {
    String eventId = messageJson.path("eventId").asText();
    String tmpIdentifier = messageJson.path("identifier").asText();
    ActionType tmpActionType = ActionType.fromValue(messageJson.path("actionType").asText());
    String tmpActionId = messageJson.path("actionId").asText();
    OperationType tmpOperationType = OperationType.fromValue(messageJson.path("operationType").asText());
    String tmpAccountKey = messageJson.path("accountKey").asText();
    String tmpCoin = messageJson.path("coin").asText();
    BigDecimal tmpAmount = new BigDecimal(messageJson.path("amount").asText());
    String tmpTxHash = messageJson.path("txHash").asText();
    String tmpLayer = messageJson.path("layer").asText();
    String tmpDepositAddress = messageJson.path("depositAddress").asText();
    String tmpStatus = messageJson.path("status").asText();
    String tmpStatusExplanation = messageJson.path("statusExplanation").asText();

    setEventId(eventId);
    setActionType(tmpActionType);
    setActionId(tmpActionId);
    setOperationType(tmpOperationType);
    this.identifier = tmpIdentifier;
    this.accountKey = tmpAccountKey;
    this.coin = tmpCoin.toLowerCase().trim();
    this.amount = tmpAmount;
    this.txHash = tmpTxHash;
    this.layer = tmpLayer;
    this.depositAddress = tmpDepositAddress;
    this.status = tmpStatus;
    this.statusExplanation = tmpStatusExplanation;

    return this;
  }

  public void validate() {
    List<String> objectErrors = super.validateRequiredFields();

    if (objectErrors.size() == 0 &&
        OperationType.COIN_DEPOSIT_CREATE.isEqualTo(getOperationType().getValue())) {
      objectErrors.addAll(toCoinDeposit(false).coinDepositValidates(status));
    }

    if (objectErrors.size() > 0) {
      throw new IllegalArgumentException("validate CoinDepositEvent: " + String.join(", ", objectErrors));
    }
  }

  @Override
  public Map<String, Object> toOperationObjectMessageJson() {
    Map<String, Object> messageJson = super.toOperationObjectMessageJson();
    messageJson.put("object", fetchCoinDeposit(true).map(CoinDeposit::toMessageJson).orElse(null));

    return messageJson;
  }
}
