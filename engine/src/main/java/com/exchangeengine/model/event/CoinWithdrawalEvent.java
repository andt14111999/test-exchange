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
import com.exchangeengine.model.CoinWithdrawal;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.WithdrawalCache;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Model for Coin Withdrawal events
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoinWithdrawalEvent extends BaseEvent {
  private String identifier;
  private String accountKey;
  private BigDecimal amount;
  private String coin;
  private String txHash;
  private String layer;
  private String destinationAddress;
  private BigDecimal fee;
  private String status;
  private String statusExplanation;
  private String recipientAccountKey;

  protected WithdrawalCache getWithdrawalCache() {
    return WithdrawalCache.getInstance();
  }

  @Override
  public String getProducerEventId() {
    return this.identifier;
  }

  @Override
  public String getEventHandler() {
    return EventHandlerAction.WITHDRAWAL_EVENT;
  }

  public Optional<CoinWithdrawal> fetchCoinWithdrawal(boolean raiseException) {
    Optional<CoinWithdrawal> withdrawal = getWithdrawalCache().getWithdrawal(identifier);
    if (raiseException && !withdrawal.isPresent()) {
      throw new IllegalStateException("CoinWithdrawal not found identifier: " + identifier);
    }
    return withdrawal;
  }

  public CoinWithdrawal toCoinWithdrawal(boolean raiseException) {
    CoinWithdrawal withdrawal = fetchCoinWithdrawal(raiseException).orElseGet(() -> new CoinWithdrawal(
        getActionType(),
        getActionId(),
        identifier,
        status,
        accountKey,
        amount,
        coin,
        txHash,
        layer,
        destinationAddress,
        fee,
        recipientAccountKey));
    return withdrawal;
  }

  public CoinWithdrawalEvent parserData(JsonNode messageJson) {
    String eventId = messageJson.path("eventId").asText();
    String tmpIdentifier = messageJson.path("identifier").asText();
    ActionType tmpActionType = ActionType.fromValue(messageJson.path("actionType").asText());
    String tmpActionId = messageJson.path("actionId").asText();
    OperationType tmpOperationType = OperationType.fromValue(messageJson.path("operationType").asText());
    String tmpAccountKey = messageJson.path("accountKey").asText();
    String tmpCoin = messageJson.path("coin").asText();
    BigDecimal tmpAmount = new BigDecimal(messageJson.path("amount").asDouble());
    String tmpTxHash = messageJson.path("txHash").asText();
    String tmpLayer = messageJson.path("layer").asText();
    String tmpDestinationAddress = messageJson.path("destinationAddress").asText();
    BigDecimal tmpFee = new BigDecimal(messageJson.path("fee").asDouble());
    String tmpStatus = messageJson.path("status").asText();
    String tmpStatusExplanation = messageJson.path("statusExplanation").asText();
    String tmpRecipientAccountKey = messageJson.has("recipientAccountKey") ? 
                                     messageJson.path("recipientAccountKey").asText() : null;

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
    this.destinationAddress = tmpDestinationAddress;
    this.fee = tmpFee;
    this.status = tmpStatus;
    this.statusExplanation = tmpStatusExplanation;
    this.recipientAccountKey = tmpRecipientAccountKey;

    return this;
  }

  public void validate() {
    List<String> objectErrors = super.validateRequiredFields();

    if (objectErrors.size() == 0) {
      String operationTypeValue = getOperationType().getValue();

      if (OperationType.COIN_WITHDRAWAL_CREATE.isEqualTo(operationTypeValue)) {
        objectErrors.addAll(toCoinWithdrawal(false).coinWithdrawalValidates(status));
      } else if (OperationType.COIN_WITHDRAWAL_RELEASING.isEqualTo(operationTypeValue)) {
        objectErrors.addAll(toCoinWithdrawal(true).coinWithdrawalReleasingValidates(operationTypeValue));
      } else if (OperationType.COIN_WITHDRAWAL_FAILED.isEqualTo(operationTypeValue)) {
        objectErrors.addAll(toCoinWithdrawal(true).coinWithdrawalFailedValidates(operationTypeValue));
      } else if (OperationType.COIN_WITHDRAWAL_CANCELLED.isEqualTo(operationTypeValue)) {
        objectErrors.addAll(toCoinWithdrawal(true).coinWithdrawalCancelledValidates(operationTypeValue));
      } else {
        objectErrors.add("Unsupported operation type: " + operationTypeValue);
      }
    }

    if (objectErrors.size() > 0) {
      throw new IllegalArgumentException("validate CoinWithdrawalEvent: " + String.join(", ", objectErrors));
    }
  }

  @Override
  public Map<String, Object> toOperationObjectMessageJson() {
    Map<String, Object> messageJson = super.toOperationObjectMessageJson();
    messageJson.put("object", fetchCoinWithdrawal(true).map(CoinWithdrawal::toMessageJson).orElse(null));

    return messageJson;
  }
}
