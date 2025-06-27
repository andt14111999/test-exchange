package com.exchangeengine.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

import com.exchangeengine.model.Account;
import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.AccountCache;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Model for Disruptor events
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountEvent extends BaseEvent {
  private String accountKey;

  protected AccountCache getAccountCache() {
    return AccountCache.getInstance();
  }

  @Override
  public String getProducerEventId() {
    return this.accountKey;
  }

  public Account toAccount() {
    Account account = getAccountCache().getOrCreateAccount(accountKey);
    return account;
  }

  @Override
  public String getEventHandler() {
    return EventHandlerAction.ACCOUNT_EVENT;
  }

  public AccountEvent parserData(JsonNode messageJson) {
    String eventId = messageJson.path("eventId").asText();
    String tmpAccountKey = messageJson.path("accountKey").asText();
    ActionType tmpActionType = ActionType.fromValue(messageJson.path("actionType").asText());
    String tmpActionId = messageJson.path("actionId").asText();
    OperationType tmpOperationType = OperationType.fromValue(messageJson.path("operationType").asText());

    setEventId(eventId);
    setActionType(tmpActionType);
    setActionId(tmpActionId);
    setOperationType(tmpOperationType);
    setAccountKey(tmpAccountKey);

    return this;
  }

  public void validate() {
    List<String> objectErrors = super.validateRequiredFields();
    if (!OperationType.COIN_ACCOUNT_OPERATIONS.contains(getOperationType())) {
      objectErrors
          .add("Operation type is not supported in list: [" + OperationType.getSupportedAccountValues() + "]");
    }

    if (!ActionType.COIN_ACCOUNT.isEqualTo(getActionType().getValue())) {
      objectErrors.add("Action type not matched: expected CoinAccount, got " + getActionType());
    }

    if (objectErrors.size() == 0) {
      objectErrors.addAll(toAccount().validateRequiredFields());
    }

    if (objectErrors.size() > 0) {
      throw new IllegalArgumentException("validate AccountEvent: " + String.join(", ", objectErrors));
    }
  }

  @Override
  public Map<String, Object> toOperationObjectMessageJson() {
    Map<String, Object> messageJson = super.toOperationObjectMessageJson();
    Map<String, Object> objectMessageJson = toAccount().toMessageJson();

    messageJson.put("object", objectMessageJson);

    return messageJson;
  }
}
