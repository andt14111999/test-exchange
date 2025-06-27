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
import com.exchangeengine.model.AmmPosition;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.AmmPositionCache;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Model for AMM Position events
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmmPositionEvent extends BaseEvent {
  private String identifier;
  private String poolPair;
  private String ownerAccountKey0;
  private String ownerAccountKey1;
  private BigDecimal slippage;
  private BigDecimal amount0Initial;
  private BigDecimal amount1Initial;

  private int tickLowerIndex;
  private int tickUpperIndex;

  protected AmmPositionCache getAmmPositionCache() {
    return AmmPositionCache.getInstance();
  }

  @Override
  public String getProducerEventId() {
    return this.identifier;
  }

  @Override
  public String getEventHandler() {
    return EventHandlerAction.AMM_POSITION_EVENT;
  }

  public Optional<AmmPosition> fetchAmmPosition(boolean raiseException) {
    Optional<AmmPosition> position = getAmmPositionCache().getAmmPosition(identifier);
    if (raiseException && !position.isPresent()) {
      throw new IllegalStateException("AmmPosition " + identifier + " not found");
    }
    return position;
  }

  public AmmPosition toAmmPosition(boolean raiseException) {
    AmmPosition position = fetchAmmPosition(raiseException).orElseGet(() -> {
      AmmPosition newPosition = new AmmPosition(
          identifier,
          poolPair,
          ownerAccountKey0,
          ownerAccountKey1,
          tickLowerIndex,
          tickUpperIndex,
          slippage,
          amount0Initial,
          amount1Initial);

      return newPosition;
    });
    return position;
  }

  public AmmPositionEvent parserData(JsonNode messageJson) {
    String eventId = messageJson.path("eventId").asText();
    ActionType tmpActionType = ActionType.fromValue(messageJson.path("actionType").asText());
    String tmpActionId = messageJson.path("actionId").asText();
    OperationType tmpOperationType = OperationType.fromValue(messageJson.path("operationType").asText());

    String tmpIdentifier = messageJson.path("identifier").asText();
    String tmpPoolPair = messageJson.path("poolPair").asText();
    String tmpOwnerAccountKey0 = messageJson.path("ownerAccountKey0").asText();
    String tmpOwnerAccountKey1 = messageJson.path("ownerAccountKey1").asText();
    BigDecimal tmpSlippage = BigDecimal.valueOf(messageJson.path("slippage").asDouble(0.0));
    BigDecimal tmpAmount0Initial = new BigDecimal(messageJson.path("amount0Initial").asText("0"));
    BigDecimal tmpAmount1Initial = new BigDecimal(messageJson.path("amount1Initial").asText("0"));

    int tmpTickLowerIndex = messageJson.path("tickLowerIndex").asInt();
    int tmpTickUpperIndex = messageJson.path("tickUpperIndex").asInt();

    setEventId(eventId);
    setActionType(tmpActionType);
    setActionId(tmpActionId);
    setOperationType(tmpOperationType);
    setIdentifier(tmpIdentifier);
    setPoolPair(tmpPoolPair);
    setOwnerAccountKey0(tmpOwnerAccountKey0);
    setOwnerAccountKey1(tmpOwnerAccountKey1);
    setSlippage(tmpSlippage);
    setAmount0Initial(tmpAmount0Initial);
    setAmount1Initial(tmpAmount1Initial);
    setTickLowerIndex(tmpTickLowerIndex);
    setTickUpperIndex(tmpTickUpperIndex);

    return this;
  }

  public void validate() {
    List<String> objectErrors = super.validateRequiredFields();

    if (identifier == null || identifier.isEmpty()) {
      objectErrors.add("Position identifier is required");
    }

    if (objectErrors.size() == 0) {
      if (!ActionType.AMM_POSITION.isEqualTo(getActionType().getValue())) {
        objectErrors.add("Action type not matched: expected AmmPosition, got " + getActionType().getValue());
      }

      if (!OperationType.AMM_POSITION_OPERATIONS.contains(getOperationType())) {
        objectErrors
            .add("Operation type is not supported in list: [" + OperationType.getSupportedAmmPositionValues() + "]");
      } else if (OperationType.AMM_POSITION_CREATE.isEqualTo(getOperationType().getValue())) {
        AmmPosition position = toAmmPosition(false);
        objectErrors.addAll(position.validateRequiredFields());
        objectErrors.addAll(position.validateResourcesExist());
      } else {
        fetchAmmPosition(true).ifPresent(position -> {
          objectErrors.addAll(position.validateResourcesExist());
        });
      }
    }

    if (objectErrors.size() > 0) {
      throw new IllegalArgumentException("validate AmmPositionEvent: " + String.join(", ", objectErrors));
    }
  }

  @Override
  public Map<String, Object> toOperationObjectMessageJson() {
    Map<String, Object> messageJson = super.toOperationObjectMessageJson();
    messageJson.put("object", fetchAmmPosition(true).map(AmmPosition::toMessageJson).orElse(null));
    return messageJson;
  }
}
