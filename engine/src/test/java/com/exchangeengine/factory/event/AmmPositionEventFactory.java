package com.exchangeengine.factory.event;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.event.AmmPositionEvent;
import com.exchangeengine.util.TestModelFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.instancio.Instancio;
import org.instancio.Model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.instancio.Select.field;

/**
 * Factory class to create AmmPositionEvent objects for testing purposes
 */
public class AmmPositionEventFactory {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final String DEFAULT_EVENT_ID = "evt-" + UUID.randomUUID().toString().substring(0, 8);
  private static final ActionType DEFAULT_ACTION_TYPE = ActionType.AMM_POSITION;
  private static final String DEFAULT_ACTION_ID = "act-" + UUID.randomUUID().toString().substring(0, 8);
  private static final OperationType DEFAULT_OPERATION_TYPE = OperationType.AMM_POSITION_CREATE;
  private static final String DEFAULT_IDENTIFIER = "pos-" + UUID.randomUUID().toString().substring(0, 8);
  private static final String DEFAULT_POOL_PAIR = "USDT/VND";
  private static final String DEFAULT_OWNER_ACCOUNT_KEY0 = "USDT:user123";
  private static final String DEFAULT_OWNER_ACCOUNT_KEY1 = "VND:user123";
  private static final BigDecimal DEFAULT_SLIPPAGE = BigDecimal.valueOf(0.01);
  private static final BigDecimal DEFAULT_AMOUNT0_INITIAL = new BigDecimal("100000000");
  private static final BigDecimal DEFAULT_AMOUNT1_INITIAL = new BigDecimal("150000000");
  private static final int DEFAULT_TICK_LOWER_INDEX = -100;
  private static final int DEFAULT_TICK_UPPER_INDEX = 100;

  /**
   * Create a base model for AmmPositionEvent with default values
   *
   * @return a model for AmmPositionEvent
   */
  public static Model<AmmPositionEvent> model() {
    return Instancio.of(AmmPositionEvent.class)
        .set(field(AmmPositionEvent::getEventId), DEFAULT_EVENT_ID)
        .set(field(AmmPositionEvent::getActionType), DEFAULT_ACTION_TYPE)
        .set(field(AmmPositionEvent::getActionId), DEFAULT_ACTION_ID)
        .set(field(AmmPositionEvent::getOperationType), DEFAULT_OPERATION_TYPE)
        .set(field(AmmPositionEvent::getIdentifier), DEFAULT_IDENTIFIER)
        .set(field(AmmPositionEvent::getPoolPair), DEFAULT_POOL_PAIR)
        .set(field(AmmPositionEvent::getOwnerAccountKey0), DEFAULT_OWNER_ACCOUNT_KEY0)
        .set(field(AmmPositionEvent::getOwnerAccountKey1), DEFAULT_OWNER_ACCOUNT_KEY1)
        .set(field(AmmPositionEvent::getSlippage), DEFAULT_SLIPPAGE)
        .set(field(AmmPositionEvent::getAmount0Initial), DEFAULT_AMOUNT0_INITIAL)
        .set(field(AmmPositionEvent::getAmount1Initial), DEFAULT_AMOUNT1_INITIAL)
        .set(field(AmmPositionEvent::getTickLowerIndex), DEFAULT_TICK_LOWER_INDEX)
        .set(field(AmmPositionEvent::getTickUpperIndex), DEFAULT_TICK_UPPER_INDEX)
        .toModel();
  }

  /**
   * Create an AmmPositionEvent with default values
   *
   * @return a valid AmmPositionEvent
   */
  public static AmmPositionEvent create() {
    return Instancio.create(model());
  }

  /**
   * Create an AmmPositionEvent with custom values
   *
   * @param customFields Map of fields and values to customize
   * @return a customized AmmPositionEvent
   */
  public static AmmPositionEvent customize(Map<String, Object> customFields) {
    return TestModelFactory.customize(create(), customFields);
  }

  /**
   * Create a JsonNode representation of an AmmPositionEvent
   *
   * @param ammPositionEvent the AMM position event to convert to JsonNode
   * @return a JsonNode representation
   */
  public static JsonNode toJsonNode(AmmPositionEvent ammPositionEvent) {
    ObjectNode jsonNode = objectMapper.createObjectNode();

    // Thêm các trường cơ bản
    jsonNode.put("eventId", ammPositionEvent.getEventId());
    jsonNode.put("actionType", ammPositionEvent.getActionType().getValue());
    jsonNode.put("actionId", ammPositionEvent.getActionId());
    jsonNode.put("operationType", ammPositionEvent.getOperationType().getValue());
    jsonNode.put("identifier", ammPositionEvent.getIdentifier());
    jsonNode.put("poolPair", ammPositionEvent.getPoolPair());
    jsonNode.put("ownerAccountKey0", ammPositionEvent.getOwnerAccountKey0());
    jsonNode.put("ownerAccountKey1", ammPositionEvent.getOwnerAccountKey1());
    jsonNode.put("tickLowerIndex", ammPositionEvent.getTickLowerIndex());
    jsonNode.put("tickUpperIndex", ammPositionEvent.getTickUpperIndex());

    // Thêm các trường số với xử lý null
    if (ammPositionEvent.getSlippage() != null) {
      jsonNode.put("slippage", ammPositionEvent.getSlippage().doubleValue());
    } else {
      jsonNode.putNull("slippage");
    }

    if (ammPositionEvent.getAmount0Initial() != null) {
      jsonNode.put("amount0Initial", ammPositionEvent.getAmount0Initial().toString());
    } else {
      jsonNode.putNull("amount0Initial");
    }

    if (ammPositionEvent.getAmount1Initial() != null) {
      jsonNode.put("amount1Initial", ammPositionEvent.getAmount1Initial().toString());
    } else {
      jsonNode.putNull("amount1Initial");
    }

    return jsonNode;
  }

  /**
   * Create a JsonNode representation of a valid AmmPositionEvent with default
   * values
   *
   * @return a JsonNode representation of a valid AmmPositionEvent
   */
  public static JsonNode createJsonNode() {
    return toJsonNode(create());
  }

  /**
   * Create a JsonNode for an event with collect fee operation
   */
  public static JsonNode createJsonNodeForCollectFee() {
    AmmPositionEvent event = customize(Map.of("operationType", OperationType.AMM_POSITION_COLLECT_FEE));
    return toJsonNode(event);
  }

  /**
   * Create a JsonNode for an event with close position operation
   */
  public static JsonNode createJsonNodeForClose() {
    AmmPositionEvent event = customize(Map.of("operationType", OperationType.AMM_POSITION_CLOSE));
    return toJsonNode(event);
  }
}
