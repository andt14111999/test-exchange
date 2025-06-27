package com.exchangeengine.factory.event;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.Trade;
import com.exchangeengine.model.event.TradeEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.instancio.Instancio;
import org.instancio.Model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.instancio.Select.field;

/**
 * Factory class để tạo TradeEvent objects cho mục đích testing
 */
public class TradeEventFactory {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final String DEFAULT_EVENT_ID = "evt-" + UUID.randomUUID().toString().substring(0, 8);
  private static final ActionType DEFAULT_ACTION_TYPE = ActionType.TRADE;
  private static final String DEFAULT_ACTION_ID = "act-" + UUID.randomUUID().toString().substring(0, 8);
  private static final OperationType DEFAULT_OPERATION_TYPE = OperationType.TRADE_CREATE;
  private static final String DEFAULT_IDENTIFIER = "trade-" + UUID.randomUUID().toString().substring(0, 8);
  private static final String DEFAULT_OFFER_KEY = "offer-" + UUID.randomUUID().toString().substring(0, 8);
  private static final String DEFAULT_BUYER_ACCOUNT_KEY = "buyer-" + UUID.randomUUID().toString().substring(0, 8);
  private static final String DEFAULT_SELLER_ACCOUNT_KEY = "seller-" + UUID.randomUUID().toString().substring(0, 8);
  private static final String DEFAULT_SYMBOL = "BTC:USD";
  private static final BigDecimal DEFAULT_PRICE = new BigDecimal("10000");
  private static final BigDecimal DEFAULT_COIN_AMOUNT = new BigDecimal("1");
  private static final String DEFAULT_TAKER_SIDE = Trade.TAKER_SIDE_BUY;
  private static final String DEFAULT_STATUS = Trade.TradeStatus.UNPAID.name();

  /**
   * Tạo model cơ bản cho TradeEvent với các giá trị mặc định
   *
   * @return model cho TradeEvent
   */
  public static Model<TradeEvent> model() {
    return Instancio.of(TradeEvent.class)
        .set(field(TradeEvent::getEventId), DEFAULT_EVENT_ID)
        .set(field(TradeEvent::getActionType), DEFAULT_ACTION_TYPE)
        .set(field(TradeEvent::getActionId), DEFAULT_ACTION_ID)
        .set(field(TradeEvent::getOperationType), DEFAULT_OPERATION_TYPE)
        .set(field(TradeEvent::getIdentifier), DEFAULT_IDENTIFIER)
        .set(field(TradeEvent::getOfferKey), DEFAULT_OFFER_KEY)
        .set(field(TradeEvent::getBuyerAccountKey), DEFAULT_BUYER_ACCOUNT_KEY)
        .set(field(TradeEvent::getSellerAccountKey), DEFAULT_SELLER_ACCOUNT_KEY)
        .set(field(TradeEvent::getSymbol), DEFAULT_SYMBOL)
        .set(field(TradeEvent::getPrice), DEFAULT_PRICE)
        .set(field(TradeEvent::getCoinAmount), DEFAULT_COIN_AMOUNT)
        .set(field(TradeEvent::getTakerSide), DEFAULT_TAKER_SIDE)
        .set(field(TradeEvent::getStatus), DEFAULT_STATUS)
        .set(field(TradeEvent::getCreatedAt), Instant.now())
        .set(field(TradeEvent::getUpdatedAt), Instant.now())
        .toModel();
  }

  /**
   * Tạo TradeEvent với các giá trị mặc định
   *
   * @return một TradeEvent hợp lệ
   */
  public static TradeEvent create() {
    return Instancio.create(model());
  }

  /**
   * Tạo TradeEvent với operation type cụ thể
   *
   * @param operationType operation type cần thiết lập
   * @return TradeEvent với operation type đã chỉ định
   */
  public static TradeEvent withOperationType(OperationType operationType) {
    return Instancio.of(model())
        .set(field(TradeEvent::getOperationType), operationType)
        .create();
  }

  /**
   * Tạo TradeEvent với action type cụ thể
   *
   * @param actionType action type cần thiết lập
   * @return TradeEvent với action type đã chỉ định
   */
  public static TradeEvent withActionType(ActionType actionType) {
    return Instancio.of(model())
        .set(field(TradeEvent::getActionType), actionType)
        .create();
  }

  /**
   * Tạo TradeEvent với identifier cụ thể
   *
   * @param identifier identifier cần thiết lập
   * @return TradeEvent với identifier đã chỉ định
   */
  public static TradeEvent withIdentifier(String identifier) {
    return Instancio.of(model())
        .set(field(TradeEvent::getIdentifier), identifier)
        .create();
  }

  /**
   * Tạo TradeEvent với takerSide cụ thể
   *
   * @param takerSide takerSide cần thiết lập
   * @return TradeEvent với takerSide đã chỉ định
   */
  public static TradeEvent withTakerSide(String takerSide) {
    return Instancio.of(model())
        .set(field(TradeEvent::getTakerSide), takerSide)
        .create();
  }

  /**
   * Tạo TradeEvent với các giá trị timestamp cụ thể
   *
   * @param created thời gian tạo
   * @param updated thời gian cập nhật
   * @param completed thời gian hoàn thành
   * @param cancelled thời gian hủy
   * @return TradeEvent với các timestamps đã chỉ định
   */
  public static TradeEvent withTimestamps(Instant created, Instant updated, Instant completed, Instant cancelled) {
    return Instancio.of(model())
        .set(field(TradeEvent::getCreatedAt), created)
        .set(field(TradeEvent::getUpdatedAt), updated)
        .set(field(TradeEvent::getCompletedAt), completed)
        .set(field(TradeEvent::getCancelledAt), cancelled)
        .create();
  }

  /**
   * Tạo JsonNode từ TradeEvent
   *
   * @param tradeEvent trade event cần chuyển đổi thành JsonNode
   * @return JsonNode tương ứng
   */
  public static JsonNode toJsonNode(TradeEvent tradeEvent) {
    ObjectNode jsonNode = objectMapper.createObjectNode();
    jsonNode.put("eventId", tradeEvent.getEventId());
    jsonNode.put("actionType", tradeEvent.getActionType().getValue());
    jsonNode.put("actionId", tradeEvent.getActionId());
    jsonNode.put("operationType", tradeEvent.getOperationType().getValue());
    jsonNode.put("identifier", tradeEvent.getIdentifier());
    jsonNode.put("offerKey", tradeEvent.getOfferKey());
    jsonNode.put("buyerAccountKey", tradeEvent.getBuyerAccountKey());
    jsonNode.put("sellerAccountKey", tradeEvent.getSellerAccountKey());
    
    // Handle symbol or extract coin/fiat currencies
    if (tradeEvent.getSymbol() != null) {
      jsonNode.put("symbol", tradeEvent.getSymbol());
      String[] parts = tradeEvent.getSymbol().split(":");
      if (parts.length == 2) {
        jsonNode.put("coinCurrency", parts[0]);
        jsonNode.put("fiatCurrency", parts[1]);
      }
    }
    
    if (tradeEvent.getPrice() != null) {
      jsonNode.put("price", tradeEvent.getPrice());
    }
    
    if (tradeEvent.getCoinAmount() != null) {
      jsonNode.put("coinAmount", tradeEvent.getCoinAmount());
    }
    
    jsonNode.put("takerSide", tradeEvent.getTakerSide());
    jsonNode.put("status", tradeEvent.getStatus());
    
    if (tradeEvent.getCreatedAt() != null) {
      jsonNode.put("createdAt", tradeEvent.getCreatedAt().toString());
    }
    
    if (tradeEvent.getUpdatedAt() != null) {
      jsonNode.put("updatedAt", tradeEvent.getUpdatedAt().toString());
    }
    
    if (tradeEvent.getCompletedAt() != null) {
      jsonNode.put("releasedAt", tradeEvent.getCompletedAt().toString());
    }
    
    if (tradeEvent.getCancelledAt() != null) {
      jsonNode.put("cancelledAt", tradeEvent.getCancelledAt().toString());
    }
    
    return jsonNode;
  }

  /**
   * Tạo JsonNode với các giá trị mặc định
   *
   * @return JsonNode tương ứng
   */
  public static JsonNode createJsonNode() {
    return toJsonNode(create());
  }

  /**
   * Tạo JsonNode với timestamps theo định dạng Unix epoch
   *
   * @return JsonNode với timestamps dạng epoch
   */
  public static JsonNode createJsonNodeWithEpochTimestamps() {
    ObjectNode jsonNode = (ObjectNode) createJsonNode();
    Instant now = Instant.now();
    
    jsonNode.put("createdAt", now.getEpochSecond());
    jsonNode.put("updatedAt", now.getEpochSecond());
    jsonNode.remove("releasedAt");
    jsonNode.remove("cancelledAt");
    
    return jsonNode;
  }

  /**
   * Tạo JsonNode thiếu trường bắt buộc
   *
   * @param fieldToRemove tên trường cần loại bỏ
   * @return JsonNode thiếu trường chỉ định
   */
  public static JsonNode createJsonNodeWithMissingField(String fieldToRemove) {
    ObjectNode jsonNode = (ObjectNode) createJsonNode();
    jsonNode.remove(fieldToRemove);
    return jsonNode;
  }
}
