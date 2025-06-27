package com.exchangeengine.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.AmmPool;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.AmmPoolCache;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Model for AMM Pool events
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmmPoolEvent extends BaseEvent {
  private String pair;
  private String token0;
  private String token1;
  private int tickSpacing;

  private double feePercentage;
  private double feeProtocolPercentage;
  private BigDecimal initPrice;

  private boolean isActive;

  protected AmmPoolCache getAmmPoolCache() {
    return AmmPoolCache.getInstance();
  }

  @Override
  public String getProducerEventId() {
    return this.pair;
  }

  @Override
  public String getEventHandler() {
    return EventHandlerAction.AMM_POOL_EVENT;
  }

  public Optional<AmmPool> fetchAmmPool(boolean raiseException) {
    Optional<AmmPool> pool = getAmmPoolCache().getAmmPool(pair);
    if (raiseException && !pool.isPresent()) {
      throw new IllegalStateException("AmmPool " + pair + " not found");
    }
    return pool;
  }

  public AmmPool toAmmPool(boolean raiseException) {
    AmmPool pool = fetchAmmPool(raiseException).orElseGet(() -> {
      AmmPool newPool = new AmmPool(pair);
      newPool.setToken0(token0);
      newPool.setToken1(token1);
      newPool.setFeePercentage(feePercentage);
      newPool.setFeeProtocolPercentage(feeProtocolPercentage);
      newPool.setTickSpacing(tickSpacing);
      newPool.setActive(isActive);
      newPool.setInitPrice(initPrice);
      return newPool;
    });
    return pool;
  }

  public AmmPoolEvent parserData(JsonNode messageJson) {
    String eventId = messageJson.path("eventId").asText();
    ActionType tmpActionType = ActionType.fromValue(messageJson.path("actionType").asText());
    String tmpActionId = messageJson.path("actionId").asText();
    OperationType tmpOperationType = OperationType.fromValue(messageJson.path("operationType").asText());
    String tmpPair = messageJson.path("pair").asText();
    String tmpToken0 = messageJson.path("token0").asText();
    String tmpToken1 = messageJson.path("token1").asText();
    double tmpFeePercentage = messageJson.path("feePercentage").asDouble();
    double tmpFeeProtocolPercentage = messageJson.path("feeProtocolPercentage").asDouble();
    boolean tmpIsActive = messageJson.path("isActive").asBoolean();
    int tmpTickSpacing = messageJson.path("tickSpacing").asInt();
    BigDecimal tmpInitPrice = null;
    if (!messageJson.path("initPrice").isMissingNode() && !messageJson.path("initPrice").isNull()) {
      tmpInitPrice = BigDecimal.valueOf(messageJson.path("initPrice").asDouble());
    }

    setEventId(eventId);
    setActionType(tmpActionType);
    setActionId(tmpActionId);
    setOperationType(tmpOperationType);
    setPair(tmpPair);
    setToken0(tmpToken0);
    setToken1(tmpToken1);
    setFeePercentage(tmpFeePercentage);
    setFeeProtocolPercentage(tmpFeeProtocolPercentage);
    setTickSpacing(tmpTickSpacing);
    setActive(tmpIsActive);
    setInitPrice(tmpInitPrice);

    return this;
  }

  public void validate() {
    List<String> objectErrors = super.validateRequiredFields();

    if (!ActionType.AMM_POOL.isEqualTo(getActionType().getValue())) {
      objectErrors.add("Action type not matched: expected AmmPool, got " + getActionType().getValue());
    }

    if (objectErrors.size() == 0) {
      String operationTypeValue = getOperationType().getValue();
      if (OperationType.AMM_POOL_CREATE.isEqualTo(operationTypeValue)) {
        objectErrors.addAll(toAmmPool(false).validateRequiredFields());
      } else if (OperationType.AMM_POOL_UPDATE.isEqualTo(operationTypeValue)) {
        AmmPool pool = toAmmPool(true);
        if (!pool.hasUpdateField(isActive, feePercentage, feeProtocolPercentage, initPrice)) {
          objectErrors.add(
              "Update pool missing required fields(isActive, feePercentage, feeProtocolPercentage, initPrice) or not changed");
        }

        if (initPrice != null) {
          if (initPrice.compareTo(BigDecimal.ZERO) <= 0) {
            objectErrors.add("Initial price must be positive");
          }

          if (pool.getTotalValueLockedToken0().compareTo(BigDecimal.ZERO) > 0 ||
              pool.getTotalValueLockedToken1().compareTo(BigDecimal.ZERO) > 0) {
            objectErrors.add("Cannot modify initPrice on pool with liquidity");
          }

          if (pool.isActive()) {
            objectErrors.add("Cannot modify initPrice on active pool");
          }
        }
      } else {
        objectErrors
            .add("Operation type is not supported in list: [" + OperationType.getSupportedAmmPoolValues() + "]");
      }
    }

    if (objectErrors.size() > 0) {
      throw new IllegalArgumentException("validate AmmPoolEvent: " + String.join(", ", objectErrors));
    }
  }

  @Override
  public Map<String, Object> toOperationObjectMessageJson() {
    Map<String, Object> messageJson = super.toOperationObjectMessageJson();
    messageJson.put("object", fetchAmmPool(true).map(AmmPool::toMessageJson).orElse(null));

    return messageJson;
  }
}
