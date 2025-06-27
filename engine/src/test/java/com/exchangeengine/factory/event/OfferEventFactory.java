package com.exchangeengine.factory.event;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.Offer;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.event.OfferEvent;
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
 * Factory class để tạo OfferEvent objects cho mục đích testing
 */
public class OfferEventFactory {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final String DEFAULT_EVENT_ID = "evt-" + UUID.randomUUID().toString().substring(0, 8);
  private static final ActionType DEFAULT_ACTION_TYPE = ActionType.OFFER;
  private static final String DEFAULT_ACTION_ID = "act-" + UUID.randomUUID().toString().substring(0, 8);
  private static final OperationType DEFAULT_OPERATION_TYPE = OperationType.OFFER_CREATE;
  private static final String DEFAULT_IDENTIFIER = "offer-" + UUID.randomUUID().toString().substring(0, 8);
  private static final String DEFAULT_USER_ID = "user-" + UUID.randomUUID().toString().substring(0, 8);
  private static final String DEFAULT_OFFER_TYPE = Offer.OfferType.BUY.name();
  private static final String DEFAULT_COIN_CURRENCY = "btc";
  private static final String DEFAULT_CURRENCY = "usd";
  private static final BigDecimal DEFAULT_PRICE = new BigDecimal("10000");
  private static final BigDecimal DEFAULT_MIN_AMOUNT = new BigDecimal("0.01");
  private static final BigDecimal DEFAULT_MAX_AMOUNT = new BigDecimal("1");
  private static final BigDecimal DEFAULT_TOTAL_AMOUNT = new BigDecimal("1");
  private static final BigDecimal DEFAULT_AVAILABLE_AMOUNT = new BigDecimal("1");
  private static final String DEFAULT_PAYMENT_METHOD_ID = "pm-" + UUID.randomUUID().toString().substring(0, 8);
  private static final Integer DEFAULT_PAYMENT_TIME = 30;
  private static final String DEFAULT_COUNTRY_CODE = "US";
  private static final Boolean DEFAULT_DISABLED = false;
  private static final Boolean DEFAULT_DELETED = false;
  private static final Boolean DEFAULT_AUTOMATIC = true;
  private static final Boolean DEFAULT_ONLINE = true;
  private static final BigDecimal DEFAULT_MARGIN = new BigDecimal("1.5");

  /**
   * Tạo model cơ bản cho OfferEvent với các giá trị mặc định
   *
   * @return model cho OfferEvent
   */
  public static Model<OfferEvent> model() {
    return Instancio.of(OfferEvent.class)
        .set(field(OfferEvent::getEventId), DEFAULT_EVENT_ID)
        .set(field(OfferEvent::getActionType), DEFAULT_ACTION_TYPE)
        .set(field(OfferEvent::getActionId), DEFAULT_ACTION_ID)
        .set(field(OfferEvent::getOperationType), DEFAULT_OPERATION_TYPE)
        .set(field(OfferEvent::getIdentifier), DEFAULT_IDENTIFIER)
        .set(field(OfferEvent::getUserId), DEFAULT_USER_ID)
        .set(field(OfferEvent::getOfferType), DEFAULT_OFFER_TYPE)
        .set(field(OfferEvent::getCoinCurrency), DEFAULT_COIN_CURRENCY)
        .set(field(OfferEvent::getCurrency), DEFAULT_CURRENCY)
        .set(field(OfferEvent::getPrice), DEFAULT_PRICE)
        .set(field(OfferEvent::getMinAmount), DEFAULT_MIN_AMOUNT)
        .set(field(OfferEvent::getMaxAmount), DEFAULT_MAX_AMOUNT)
        .set(field(OfferEvent::getTotalAmount), DEFAULT_TOTAL_AMOUNT)
        .set(field(OfferEvent::getAvailableAmount), DEFAULT_AVAILABLE_AMOUNT)
        .set(field(OfferEvent::getPaymentMethodId), DEFAULT_PAYMENT_METHOD_ID)
        .set(field(OfferEvent::getPaymentTime), DEFAULT_PAYMENT_TIME)
        .set(field(OfferEvent::getCountryCode), DEFAULT_COUNTRY_CODE)
        .set(field(OfferEvent::getDisabled), DEFAULT_DISABLED)
        .set(field(OfferEvent::getDeleted), DEFAULT_DELETED)
        .set(field(OfferEvent::getAutomatic), DEFAULT_AUTOMATIC)
        .set(field(OfferEvent::getOnline), DEFAULT_ONLINE)
        .set(field(OfferEvent::getMargin), DEFAULT_MARGIN)
        .set(field(OfferEvent::getCreatedAt), Instant.now())
        .set(field(OfferEvent::getUpdatedAt), Instant.now())
        .toModel();
  }

  /**
   * Tạo OfferEvent với các giá trị mặc định
   *
   * @return một OfferEvent hợp lệ
   */
  public static OfferEvent create() {
    return Instancio.create(model());
  }

  /**
   * Tạo OfferEvent với operation type cụ thể
   *
   * @param operationType operation type cần thiết lập
   * @return OfferEvent với operation type đã chỉ định
   */
  public static OfferEvent withOperationType(OperationType operationType) {
    return Instancio.of(model())
        .set(field(OfferEvent::getOperationType), operationType)
        .create();
  }

  /**
   * Tạo OfferEvent với action type cụ thể
   *
   * @param actionType action type cần thiết lập
   * @return OfferEvent với action type đã chỉ định
   */
  public static OfferEvent withActionType(ActionType actionType) {
    return Instancio.of(model())
        .set(field(OfferEvent::getActionType), actionType)
        .create();
  }

  /**
   * Tạo OfferEvent với identifier cụ thể
   *
   * @param identifier identifier cần thiết lập
   * @return OfferEvent với identifier đã chỉ định
   */
  public static OfferEvent withIdentifier(String identifier) {
    return Instancio.of(model())
        .set(field(OfferEvent::getIdentifier), identifier)
        .create();
  }

  /**
   * Tạo OfferEvent với userId cụ thể
   *
   * @param userId userId cần thiết lập
   * @return OfferEvent với userId đã chỉ định
   */
  public static OfferEvent withUserId(String userId) {
    return Instancio.of(model())
        .set(field(OfferEvent::getUserId), userId)
        .create();
  }

  /**
   * Tạo OfferEvent với giá cụ thể
   *
   * @param price giá cần thiết lập
   * @return OfferEvent với giá đã chỉ định
   */
  public static OfferEvent withPrice(BigDecimal price) {
    return Instancio.of(model())
        .set(field(OfferEvent::getPrice), price)
        .create();
  }

  /**
   * Tạo OfferEvent với các giá trị timestamp cụ thể
   *
   * @param created thời gian tạo
   * @param updated thời gian cập nhật
   * @return OfferEvent với các timestamps đã chỉ định
   */
  public static OfferEvent withTimestamps(Instant created, Instant updated) {
    return Instancio.of(model())
        .set(field(OfferEvent::getCreatedAt), created)
        .set(field(OfferEvent::getUpdatedAt), updated)
        .create();
  }

  /**
   * Tạo JsonNode từ OfferEvent
   *
   * @param offerEvent offer event cần chuyển đổi thành JsonNode
   * @return JsonNode tương ứng
   */
  public static JsonNode toJsonNode(OfferEvent offerEvent) {
    ObjectNode jsonNode = objectMapper.createObjectNode();
    jsonNode.put("eventId", offerEvent.getEventId());
    jsonNode.put("actionType", offerEvent.getActionType().getValue());
    jsonNode.put("actionId", offerEvent.getActionId());
    jsonNode.put("operationType", offerEvent.getOperationType().getValue());
    jsonNode.put("identifier", offerEvent.getIdentifier());
    jsonNode.put("userId", offerEvent.getUserId());
    jsonNode.put("offerType", offerEvent.getOfferType());
    jsonNode.put("coinCurrency", offerEvent.getCoinCurrency());
    jsonNode.put("currency", offerEvent.getCurrency());
    
    if (offerEvent.getPrice() != null) {
      jsonNode.put("price", offerEvent.getPrice());
    }
    
    if (offerEvent.getMinAmount() != null) {
      jsonNode.put("minAmount", offerEvent.getMinAmount());
    }
    
    if (offerEvent.getMaxAmount() != null) {
      jsonNode.put("maxAmount", offerEvent.getMaxAmount());
    }
    
    if (offerEvent.getTotalAmount() != null) {
      jsonNode.put("totalAmount", offerEvent.getTotalAmount());
    }
    
    if (offerEvent.getAvailableAmount() != null) {
      jsonNode.put("availableAmount", offerEvent.getAvailableAmount());
    }
    
    if (offerEvent.getMargin() != null) {
      jsonNode.put("margin", offerEvent.getMargin());
    }
    
    jsonNode.put("paymentMethodId", offerEvent.getPaymentMethodId());
    jsonNode.put("paymentTime", offerEvent.getPaymentTime());
    jsonNode.put("countryCode", offerEvent.getCountryCode());
    jsonNode.put("disabled", offerEvent.getDisabled());
    jsonNode.put("deleted", offerEvent.getDeleted());
    jsonNode.put("automatic", offerEvent.getAutomatic());
    jsonNode.put("online", offerEvent.getOnline());
    
    if (offerEvent.getCreatedAt() != null) {
      jsonNode.put("createdAt", offerEvent.getCreatedAt().toString());
    }
    
    if (offerEvent.getUpdatedAt() != null) {
      jsonNode.put("updatedAt", offerEvent.getUpdatedAt().toString());
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
