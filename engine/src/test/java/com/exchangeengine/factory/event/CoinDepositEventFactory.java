package com.exchangeengine.factory.event;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.event.CoinDepositEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.instancio.Instancio;
import org.instancio.Model;

import java.math.BigDecimal;
import java.util.UUID;

import static org.instancio.Select.field;

/**
 * Factory class to create CoinDepositEvent objects for testing purposes
 */
public class CoinDepositEventFactory {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final String DEFAULT_EVENT_ID = "evt-" + UUID.randomUUID().toString().substring(0, 8);
  private static final String DEFAULT_IDENTIFIER = "deposit-" + UUID.randomUUID().toString().substring(0, 8);
  private static final String DEFAULT_ACCOUNT_KEY = "account-" + UUID.randomUUID().toString().substring(0, 8);
  private static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("1.23");
  private static final String DEFAULT_COIN = "USDT";
  private static final String DEFAULT_TX_HASH = "0x" + UUID.randomUUID().toString().replace("-", "");
  private static final String DEFAULT_LAYER = "layer1";
  private static final String DEFAULT_DEPOSIT_ADDRESS = "bc1" + UUID.randomUUID().toString().substring(0, 20);
  private static final String DEFAULT_STATUS = "pending";
  private static final String DEFAULT_STATUS_EXPLANATION = "Deposit is being processed";
  private static final ActionType DEFAULT_ACTION_TYPE = ActionType.COIN_TRANSACTION;
  private static final String DEFAULT_ACTION_ID = "act-" + UUID.randomUUID().toString().substring(0, 8);
  private static final OperationType DEFAULT_OPERATION_TYPE = OperationType.COIN_DEPOSIT_CREATE;

  /**
   * Create a base model for CoinDepositEvent with default values
   *
   * @return a model for CoinDepositEvent
   */
  public static Model<CoinDepositEvent> model() {
    return Instancio.of(CoinDepositEvent.class)
        .set(field(CoinDepositEvent::getEventId), DEFAULT_EVENT_ID)
        .set(field(CoinDepositEvent::getIdentifier), DEFAULT_IDENTIFIER)
        .set(field(CoinDepositEvent::getAccountKey), DEFAULT_ACCOUNT_KEY)
        .set(field(CoinDepositEvent::getAmount), DEFAULT_AMOUNT)
        .set(field(CoinDepositEvent::getCoin), DEFAULT_COIN)
        .set(field(CoinDepositEvent::getTxHash), DEFAULT_TX_HASH)
        .set(field(CoinDepositEvent::getLayer), DEFAULT_LAYER)
        .set(field(CoinDepositEvent::getDepositAddress), DEFAULT_DEPOSIT_ADDRESS)
        .set(field(CoinDepositEvent::getStatus), DEFAULT_STATUS)
        .set(field(CoinDepositEvent::getStatusExplanation), DEFAULT_STATUS_EXPLANATION)
        .set(field(CoinDepositEvent::getActionType), DEFAULT_ACTION_TYPE)
        .set(field(CoinDepositEvent::getActionId), DEFAULT_ACTION_ID)
        .set(field(CoinDepositEvent::getOperationType), DEFAULT_OPERATION_TYPE)
        .toModel();
  }

  /**
   * Create a CoinDepositEvent with default values
   *
   * @return a valid CoinDepositEvent
   */
  public static CoinDepositEvent create() {
    return Instancio.create(model());
  }

  /**
   * Create a CoinDepositEvent with custom identifier
   *
   * @param identifier the unique identifier of the deposit
   * @return a CoinDepositEvent with the specified identifier
   */
  public static CoinDepositEvent withIdentifier(String identifier) {
    return Instancio.of(model())
        .set(field(CoinDepositEvent::getIdentifier), identifier)
        .create();
  }

  /**
   * Create a CoinDepositEvent with custom account key
   *
   * @param accountKey the account key
   * @return a CoinDepositEvent with the specified account key
   */
  public static CoinDepositEvent withAccountKey(String accountKey) {
    return Instancio.of(model())
        .set(field(CoinDepositEvent::getAccountKey), accountKey)
        .create();
  }

  /**
   * Create a JsonNode representation of a CoinDepositEvent
   *
   * @param depositEvent the event to convert to JsonNode
   * @return a JsonNode representation
   */
  public static JsonNode toJsonNode(CoinDepositEvent depositEvent) {
    ObjectNode jsonNode = objectMapper.createObjectNode();
    jsonNode.put("eventId", depositEvent.getEventId());
    jsonNode.put("identifier", depositEvent.getIdentifier());
    jsonNode.put("accountKey", depositEvent.getAccountKey());
    jsonNode.put("amount", depositEvent.getAmount().doubleValue());
    jsonNode.put("coin", depositEvent.getCoin());
    jsonNode.put("txHash", depositEvent.getTxHash());
    jsonNode.put("layer", depositEvent.getLayer());
    jsonNode.put("depositAddress", depositEvent.getDepositAddress());
    jsonNode.put("status", depositEvent.getStatus());
    jsonNode.put("statusExplanation", depositEvent.getStatusExplanation());
    jsonNode.put("actionType", depositEvent.getActionType().getValue());
    jsonNode.put("actionId", depositEvent.getActionId());
    jsonNode.put("operationType", depositEvent.getOperationType().getValue());
    return jsonNode;
  }

  /**
   * Create a JsonNode representation of a default CoinDepositEvent
   *
   * @return a JsonNode representation
   */
  public static JsonNode createJsonNode() {
    return toJsonNode(create());
  }
}
