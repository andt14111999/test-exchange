package com.exchangeengine.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.AmmOrder;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.AmmOrderCache;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Model for AMM Order events
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmmOrderEvent extends BaseEvent {
  private String identifier;
  private String poolPair;
  private String ownerAccountKey0;
  private String ownerAccountKey1;
  private String status;
  private Boolean zeroForOne;
  private BigDecimal amountSpecified;
  private BigDecimal slippage;

  protected AmmOrderCache getAmmOrderCache() {
    return AmmOrderCache.getInstance();
  }

  @Override
  public String getProducerEventId() {
    return this.identifier;
  }

  @Override
  public String getEventHandler() {
    return EventHandlerAction.AMM_ORDER_EVENT;
  }

  public boolean checkAmmOrderExists() {
    return getAmmOrderCache().ammOrderExists(identifier);
  }

  public AmmOrder toAmmOrder(boolean raiseException) {
    if (raiseException && checkAmmOrderExists()) {
      throw new IllegalStateException("AmmOrder " + identifier + " already exists");
    }

    AmmOrder order = new AmmOrder(
        identifier,
        poolPair,
        ownerAccountKey0,
        ownerAccountKey1,
        zeroForOne,
        amountSpecified,
        slippage,
        status);

    return order;
  }

  public AmmOrderEvent parserData(JsonNode messageJson) {
    String eventId = messageJson.path("eventId").asText();
    ActionType tmpActionType = ActionType.fromValue(messageJson.path("actionType").asText());
    String tmpActionId = messageJson.path("actionId").asText();
    OperationType tmpOperationType = OperationType.fromValue(messageJson.path("operationType").asText());

    String tmpIdentifier = messageJson.path("identifier").asText();
    String tmpPoolPair = messageJson.path("poolPair").asText();
    String tmpOwnerAccountKey0 = messageJson.path("ownerAccountKey0").asText();
    String tmpOwnerAccountKey1 = messageJson.path("ownerAccountKey1").asText();
    String tmpStatus = messageJson.path("status").asText();
    Boolean tmpZeroForOne = messageJson.path("zeroForOne").asBoolean();
    BigDecimal tmpAmountSpecified = new BigDecimal(messageJson.path("amountSpecified").asText("0"));
    BigDecimal tmpSlippage = BigDecimal.valueOf(messageJson.path("slippage").asDouble(0.0));

    setEventId(eventId);
    setActionType(tmpActionType);
    setActionId(tmpActionId);
    setOperationType(tmpOperationType);
    setIdentifier(tmpIdentifier);
    setPoolPair(tmpPoolPair);
    setOwnerAccountKey0(tmpOwnerAccountKey0);
    setOwnerAccountKey1(tmpOwnerAccountKey1);
    setStatus(tmpStatus);
    setZeroForOne(tmpZeroForOne);
    setAmountSpecified(tmpAmountSpecified);
    setSlippage(tmpSlippage);

    return this;
  }

  public void validate() {
    List<String> objectErrors = super.validateRequiredFields();

    if (identifier == null || identifier.isEmpty()) {
      objectErrors.add("Order identifier is required");
    }

    if (objectErrors.size() == 0) {
      if (!ActionType.AMM_ORDER.isEqualTo(getActionType().getValue())) {
        objectErrors.add("Action type not matched: expected AmmOrder, got " + getActionType().getValue());
      }

      if (!OperationType.AMM_ORDER_SWAP.equals(getOperationType())) {
        objectErrors.add("Operation type is not supported for AMM Order: " + getOperationType().getValue());
      }

      if (checkAmmOrderExists()) {
        objectErrors.add("AmmOrder " + identifier + " already exists");
      }

      if (objectErrors.size() == 0) {
        AmmOrder order = toAmmOrder(false);
        objectErrors.addAll(order.validateRequiredFields());
        objectErrors.addAll(order.validateResourcesExist());
      }
    }

    if (objectErrors.size() > 0) {
      throw new IllegalArgumentException("validate AmmOrderEvent: " + String.join(", ", objectErrors));
    }
  }

  @Override
  public Map<String, Object> toOperationObjectMessageJson() {
    Map<String, Object> messageJson = super.toOperationObjectMessageJson();
    messageJson.put("object", toAmmOrder(true).toMessageJson());
    return messageJson;
  }
}
