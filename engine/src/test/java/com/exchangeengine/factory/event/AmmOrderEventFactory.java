package com.exchangeengine.factory.event;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.AmmOrder;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.event.AmmOrderEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.instancio.Instancio;
import org.instancio.Model;

import java.math.BigDecimal;
import java.util.UUID;

import static org.instancio.Select.field;

/**
 * Factory class để tạo AmmOrderEvent objects cho mục đích testing
 */
public class AmmOrderEventFactory {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final String DEFAULT_EVENT_ID = "evt-" + UUID.randomUUID().toString().substring(0, 8);
  private static final ActionType DEFAULT_ACTION_TYPE = ActionType.AMM_ORDER;
  private static final String DEFAULT_ACTION_ID = "act-" + UUID.randomUUID().toString().substring(0, 8);
  private static final OperationType DEFAULT_OPERATION_TYPE = OperationType.AMM_ORDER_SWAP;
  private static final String DEFAULT_IDENTIFIER = "order-" + UUID.randomUUID().toString().substring(0, 8);
  private static final String DEFAULT_POOL_PAIR = "BTC/USDT";
  private static final String DEFAULT_ACCOUNT_KEY_0 = "account0";
  private static final String DEFAULT_ACCOUNT_KEY_1 = "account1";
  private static final String DEFAULT_STATUS = AmmOrder.STATUS_PROCESSING;
  private static final boolean DEFAULT_ZERO_FOR_ONE = true;
  private static final BigDecimal DEFAULT_AMOUNT_SPECIFIED = BigDecimal.TEN;
  private static final BigDecimal DEFAULT_SLIPPAGE = BigDecimal.valueOf(0.01);

  /**
   * Tạo model cơ bản cho AmmOrderEvent với các giá trị mặc định
   *
   * @return Một model cho AmmOrderEvent
   */
  public static Model<AmmOrderEvent> model() {
    return Instancio.of(AmmOrderEvent.class)
        .set(field(AmmOrderEvent::getEventId), DEFAULT_EVENT_ID)
        .set(field(AmmOrderEvent::getActionType), DEFAULT_ACTION_TYPE)
        .set(field(AmmOrderEvent::getActionId), DEFAULT_ACTION_ID)
        .set(field(AmmOrderEvent::getOperationType), DEFAULT_OPERATION_TYPE)
        .set(field(AmmOrderEvent::getIdentifier), DEFAULT_IDENTIFIER)
        .set(field(AmmOrderEvent::getPoolPair), DEFAULT_POOL_PAIR)
        .set(field(AmmOrderEvent::getOwnerAccountKey0), DEFAULT_ACCOUNT_KEY_0)
        .set(field(AmmOrderEvent::getOwnerAccountKey1), DEFAULT_ACCOUNT_KEY_1)
        .set(field(AmmOrderEvent::getStatus), DEFAULT_STATUS)
        .set(field(AmmOrderEvent::getZeroForOne), DEFAULT_ZERO_FOR_ONE)
        .set(field(AmmOrderEvent::getAmountSpecified), DEFAULT_AMOUNT_SPECIFIED)
        .set(field(AmmOrderEvent::getSlippage), DEFAULT_SLIPPAGE)
        .toModel();
  }

  /**
   * Tạo AmmOrderEvent với các giá trị mặc định
   *
   * @return AmmOrderEvent hợp lệ
   */
  public static AmmOrderEvent create() {
    return Instancio.create(model());
  }

  /**
   * Tạo AmmOrderEvent với identifier cụ thể
   *
   * @param identifier Order identifier
   * @return AmmOrderEvent với identifier đã chỉ định
   */
  public static AmmOrderEvent withIdentifier(String identifier) {
    return Instancio.of(model())
        .set(field(AmmOrderEvent::getIdentifier), identifier)
        .create();
  }

  /**
   * Tạo AmmOrderEvent với pool pair cụ thể
   *
   * @param poolPair Pool pair
   * @return AmmOrderEvent với pool pair đã chỉ định
   */
  public static AmmOrderEvent withPoolPair(String poolPair) {
    return Instancio.of(model())
        .set(field(AmmOrderEvent::getPoolPair), poolPair)
        .create();
  }

  /**
   * Tạo AmmOrderEvent với tài khoản chủ sở hữu cụ thể
   *
   * @param accountKey0 Account key 0
   * @param accountKey1 Account key 1
   * @return AmmOrderEvent với thông tin tài khoản đã chỉ định
   */
  public static AmmOrderEvent withOwnerAccounts(String accountKey0, String accountKey1) {
    return Instancio.of(model())
        .set(field(AmmOrderEvent::getOwnerAccountKey0), accountKey0)
        .set(field(AmmOrderEvent::getOwnerAccountKey1), accountKey1)
        .create();
  }

  /**
   * Tạo JsonNode biểu diễn AmmOrderEvent
   *
   * @param ammOrderEvent AmmOrderEvent cần chuyển thành JsonNode
   * @return JsonNode biểu diễn
   */
  public static JsonNode toJsonNode(AmmOrderEvent ammOrderEvent) {
    ObjectNode jsonNode = objectMapper.createObjectNode();
    jsonNode.put("eventId", ammOrderEvent.getEventId());
    jsonNode.put("actionType", ammOrderEvent.getActionType().getValue());
    jsonNode.put("actionId", ammOrderEvent.getActionId());
    jsonNode.put("operationType", ammOrderEvent.getOperationType().getValue());
    jsonNode.put("identifier", ammOrderEvent.getIdentifier());
    jsonNode.put("poolPair", ammOrderEvent.getPoolPair());
    jsonNode.put("ownerAccountKey0", ammOrderEvent.getOwnerAccountKey0());
    jsonNode.put("ownerAccountKey1", ammOrderEvent.getOwnerAccountKey1());
    jsonNode.put("status", ammOrderEvent.getStatus());
    jsonNode.put("zeroForOne", ammOrderEvent.getZeroForOne());
    jsonNode.put("amountSpecified", ammOrderEvent.getAmountSpecified().toString());
    jsonNode.put("slippage", ammOrderEvent.getSlippage().doubleValue());

    return jsonNode;
  }

  /**
   * Tạo JsonNode biểu diễn AmmOrderEvent hợp lệ với giá trị mặc định
   *
   * @return JsonNode biểu diễn AmmOrderEvent hợp lệ
   */
  public static JsonNode createJsonNode() {
    return toJsonNode(create());
  }

  /**
   * Tạo JsonNode với identifier cụ thể
   *
   * @param identifier Order identifier
   * @return JsonNode với identifier đã chỉ định
   */
  public static JsonNode createJsonNodeWithIdentifier(String identifier) {
    ObjectNode jsonNode = (ObjectNode) createJsonNode();
    jsonNode.put("identifier", identifier);
    return jsonNode;
  }
}
