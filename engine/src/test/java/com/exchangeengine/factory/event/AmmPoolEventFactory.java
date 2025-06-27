package com.exchangeengine.factory.event;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.event.AmmPoolEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.instancio.Instancio;
import org.instancio.Model;

import java.math.BigDecimal;
import java.util.UUID;

import static org.instancio.Select.field;

/**
 * Factory class to create AmmPoolEvent objects for testing purposes
 */
public class AmmPoolEventFactory {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final String DEFAULT_EVENT_ID = "evt-" + UUID.randomUUID().toString().substring(0, 8);
  private static final ActionType DEFAULT_ACTION_TYPE = ActionType.AMM_POOL;
  private static final String DEFAULT_ACTION_ID = "act-" + UUID.randomUUID().toString().substring(0, 8);
  private static final OperationType DEFAULT_OPERATION_TYPE = OperationType.AMM_POOL_CREATE;
  private static final String DEFAULT_PAIR = "USDT/VND";
  private static final String DEFAULT_TOKEN0 = "USDT";
  private static final String DEFAULT_TOKEN1 = "VND";
  private static final double DEFAULT_FEE_PERCENTAGE = 0.003;
  private static final double DEFAULT_FEE_PROTOCOL_PERCENTAGE = 0.1;
  private static final boolean DEFAULT_IS_ACTIVE = true;
  private static final int DEFAULT_TICK_SPACING = 10;
  private static final BigDecimal DEFAULT_INIT_PRICE = BigDecimal.valueOf(1.5);

  /**
   * Create a base model for AmmPoolEvent with default values
   *
   * @return a model for AmmPoolEvent
   */
  public static Model<AmmPoolEvent> model() {
    return Instancio.of(AmmPoolEvent.class)
        .set(field(AmmPoolEvent::getEventId), DEFAULT_EVENT_ID)
        .set(field(AmmPoolEvent::getActionType), DEFAULT_ACTION_TYPE)
        .set(field(AmmPoolEvent::getActionId), DEFAULT_ACTION_ID)
        .set(field(AmmPoolEvent::getOperationType), DEFAULT_OPERATION_TYPE)
        .set(field(AmmPoolEvent::getPair), DEFAULT_PAIR)
        .set(field(AmmPoolEvent::getToken0), DEFAULT_TOKEN0)
        .set(field(AmmPoolEvent::getToken1), DEFAULT_TOKEN1)
        .set(field(AmmPoolEvent::getFeePercentage), DEFAULT_FEE_PERCENTAGE)
        .set(field(AmmPoolEvent::getFeeProtocolPercentage), DEFAULT_FEE_PROTOCOL_PERCENTAGE)
        .set(field(AmmPoolEvent::getTickSpacing), DEFAULT_TICK_SPACING)
        .set(field(AmmPoolEvent::isActive), DEFAULT_IS_ACTIVE)
        .toModel();
  }

  /**
   * Create an AmmPoolEvent with default values
   *
   * @return a valid AmmPoolEvent
   */
  public static AmmPoolEvent create() {
    return Instancio.create(model());
  }

  /**
   * Create an AmmPoolEvent with a specific pair
   *
   * @param pair the pair to set
   * @return an AmmPoolEvent with the specified pair
   */
  public static AmmPoolEvent withPair(String pair) {
    return Instancio.of(model())
        .set(field(AmmPoolEvent::getPair), pair)
        .create();
  }

  /**
   * Create an AmmPoolEvent with AMM_POOL_UPDATE operation type
   *
   * @return an AmmPoolEvent with AMM_POOL_UPDATE operation type
   */
  public static AmmPoolEvent forUpdate() {
    return Instancio.of(model())
        .set(field(AmmPoolEvent::getOperationType), OperationType.AMM_POOL_UPDATE)
        .create();
  }

  /**
   * Create an AmmPoolEvent with specific tokens
   *
   * @param token0 the first token
   * @param token1 the second token
   * @return an AmmPoolEvent with the specified tokens
   */
  public static AmmPoolEvent withTokens(String token0, String token1) {
    return Instancio.of(model())
        .set(field(AmmPoolEvent::getToken0), token0)
        .set(field(AmmPoolEvent::getToken1), token1)
        .set(field(AmmPoolEvent::getPair), token0 + "/" + token1)
        .create();
  }

  /**
   * Create an AmmPoolEvent with specific initPrice
   *
   * @param initPrice the init price
   * @return an AmmPoolEvent with the specified initPrice
   */
  public static AmmPoolEvent withInitPrice(BigDecimal initPrice) {
    return Instancio.of(model())
        .set(field(AmmPoolEvent::getInitPrice), initPrice)
        .create();
  }

  /**
   * Create a JsonNode representation of an AmmPoolEvent
   *
   * @param ammPoolEvent the AMM pool event to convert to JsonNode
   * @return a JsonNode representation
   */
  public static JsonNode toJsonNode(AmmPoolEvent ammPoolEvent) {
    ObjectNode jsonNode = objectMapper.createObjectNode();
    jsonNode.put("eventId", ammPoolEvent.getEventId());
    jsonNode.put("actionType", ammPoolEvent.getActionType().getValue());
    jsonNode.put("actionId", ammPoolEvent.getActionId());
    jsonNode.put("operationType", ammPoolEvent.getOperationType().getValue());
    jsonNode.put("pair", ammPoolEvent.getPair());
    jsonNode.put("token0", ammPoolEvent.getToken0());
    jsonNode.put("token1", ammPoolEvent.getToken1());
    jsonNode.put("feePercentage", ammPoolEvent.getFeePercentage());
    jsonNode.put("feeProtocolPercentage", ammPoolEvent.getFeeProtocolPercentage());
    jsonNode.put("tickSpacing", ammPoolEvent.getTickSpacing());
    jsonNode.put("isActive", ammPoolEvent.isActive());

    // Add initPrice as a feature we'll implement
    jsonNode.put("initPrice", DEFAULT_INIT_PRICE.doubleValue());

    return jsonNode;
  }

  /**
   * Create a JsonNode representation of a valid AmmPoolEvent with default values
   *
   * @return a JsonNode representation of a valid AmmPoolEvent
   */
  public static JsonNode createJsonNode() {
    return toJsonNode(create());
  }

  /**
   * Create a JsonNode representation with a specific initPrice value
   *
   * @param initPrice the init price to include
   * @return a JsonNode with initPrice set
   */
  public static JsonNode createJsonNodeWithInitPrice(BigDecimal initPrice) {
    ObjectNode jsonNode = (ObjectNode) createJsonNode();
    jsonNode.put("initPrice", initPrice.doubleValue());
    return jsonNode;
  }
}
