package com.exchangeengine.factory.event;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.event.CoinWithdrawalEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.instancio.Instancio;
import org.instancio.Model;

import java.math.BigDecimal;
import java.util.UUID;

import static org.instancio.Select.field;

/**
 * Factory class to create CoinWithdrawalEvent objects for testing purposes
 */
public class CoinWithdrawalEventFactory {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final String DEFAULT_EVENT_ID = "evt-" + UUID.randomUUID().toString().substring(0, 8);
  private static final String DEFAULT_IDENTIFIER = "withdrawal-" + UUID.randomUUID().toString().substring(0, 8);
  private static final String DEFAULT_ACCOUNT_KEY = "account-" + UUID.randomUUID().toString().substring(0, 8);
  private static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("1.23");
  private static final String DEFAULT_COIN = "USDT";
  private static final String DEFAULT_TX_HASH = "0x" + UUID.randomUUID().toString().replace("-", "");
  private static final String DEFAULT_LAYER = "layer1";
  private static final String DEFAULT_DESTINATION_ADDRESS = "0x" + UUID.randomUUID().toString().substring(0, 20);
  private static final BigDecimal DEFAULT_FEE = new BigDecimal("0.01");
  private static final String DEFAULT_STATUS = "verified";
  private static final String DEFAULT_STATUS_EXPLANATION = "Withdrawal is being processed";
  private static final ActionType DEFAULT_ACTION_TYPE = ActionType.COIN_TRANSACTION;
  private static final String DEFAULT_ACTION_ID = "act-" + UUID.randomUUID().toString().substring(0, 8);
  private static final OperationType DEFAULT_OPERATION_TYPE = OperationType.COIN_WITHDRAWAL_CREATE;

  /**
   * Create a base model for CoinWithdrawalEvent with default values
   *
   * @return a model for CoinWithdrawalEvent
   */
  public static Model<CoinWithdrawalEvent> model() {
    return Instancio.of(CoinWithdrawalEvent.class)
        .set(field(CoinWithdrawalEvent::getEventId), DEFAULT_EVENT_ID)
        .set(field(CoinWithdrawalEvent::getIdentifier), DEFAULT_IDENTIFIER)
        .set(field(CoinWithdrawalEvent::getAccountKey), DEFAULT_ACCOUNT_KEY)
        .set(field(CoinWithdrawalEvent::getAmount), DEFAULT_AMOUNT)
        .set(field(CoinWithdrawalEvent::getCoin), DEFAULT_COIN)
        .set(field(CoinWithdrawalEvent::getTxHash), DEFAULT_TX_HASH)
        .set(field(CoinWithdrawalEvent::getLayer), DEFAULT_LAYER)
        .set(field(CoinWithdrawalEvent::getDestinationAddress), DEFAULT_DESTINATION_ADDRESS)
        .set(field(CoinWithdrawalEvent::getFee), DEFAULT_FEE)
        .set(field(CoinWithdrawalEvent::getStatus), DEFAULT_STATUS)
        .set(field(CoinWithdrawalEvent::getStatusExplanation), DEFAULT_STATUS_EXPLANATION)
        .set(field(CoinWithdrawalEvent::getActionType), DEFAULT_ACTION_TYPE)
        .set(field(CoinWithdrawalEvent::getActionId), DEFAULT_ACTION_ID)
        .set(field(CoinWithdrawalEvent::getOperationType), DEFAULT_OPERATION_TYPE)
        .toModel();
  }

  /**
   * Create a CoinWithdrawalEvent with default values
   *
   * @return a valid CoinWithdrawalEvent
   */
  public static CoinWithdrawalEvent create() {
    return Instancio.create(model());
  }

  /**
   * Create a CoinWithdrawalEvent with custom identifier
   *
   * @param identifier the unique identifier of the withdrawal
   * @return a CoinWithdrawalEvent with the specified identifier
   */
  public static CoinWithdrawalEvent withIdentifier(String identifier) {
    return Instancio.of(model())
        .set(field(CoinWithdrawalEvent::getIdentifier), identifier)
        .create();
  }

  /**
   * Create a CoinWithdrawalEvent with a specific operation type
   * Automatically sets status based on operation type:
   * - COIN_WITHDRAWAL_CREATE: "verified"
   * - Others: "processing"
   *
   * @param operationType the operation type to set
   * @return a CoinWithdrawalEvent with the specified operation type
   */
  public static CoinWithdrawalEvent withOperationType(OperationType operationType) {
    String status = operationType == OperationType.COIN_WITHDRAWAL_CREATE ? "verified" : "processing";
    return Instancio.of(model())
        .set(field(CoinWithdrawalEvent::getOperationType), operationType)
        .set(field(CoinWithdrawalEvent::getStatus), status)
        .create();
  }

  /**
   * Create a CoinWithdrawalEvent with custom identifier and operation type
   *
   * @param identifier    the unique identifier of the withdrawal
   * @param operationType the operation type to set
   * @return a CoinWithdrawalEvent with the specified identifier and operation
   *         type
   */
  public static CoinWithdrawalEvent withIdentifierAndOperationType(String identifier, OperationType operationType) {
    String status = operationType == OperationType.COIN_WITHDRAWAL_CREATE ? "verified" : "processing";
    return Instancio.of(model())
        .set(field(CoinWithdrawalEvent::getIdentifier), identifier)
        .set(field(CoinWithdrawalEvent::getOperationType), operationType)
        .set(field(CoinWithdrawalEvent::getStatus), status)
        .create();
  }

  /**
   * Create a JsonNode representation of a CoinWithdrawalEvent
   *
   * @param withdrawalEvent the event to convert to JsonNode
   * @return a JsonNode representation
   */
  public static JsonNode toJsonNode(CoinWithdrawalEvent withdrawalEvent) {
    ObjectNode jsonNode = objectMapper.createObjectNode();
    jsonNode.put("eventId", withdrawalEvent.getEventId());
    jsonNode.put("identifier", withdrawalEvent.getIdentifier());
    jsonNode.put("accountKey", withdrawalEvent.getAccountKey());
    jsonNode.put("amount", withdrawalEvent.getAmount().doubleValue());
    jsonNode.put("coin", withdrawalEvent.getCoin());
    jsonNode.put("txHash", withdrawalEvent.getTxHash());
    jsonNode.put("layer", withdrawalEvent.getLayer());
    jsonNode.put("destinationAddress", withdrawalEvent.getDestinationAddress());
    jsonNode.put("fee", withdrawalEvent.getFee().doubleValue());
    jsonNode.put("status", withdrawalEvent.getStatus());
    jsonNode.put("statusExplanation", withdrawalEvent.getStatusExplanation());
    jsonNode.put("actionType", withdrawalEvent.getActionType().getValue());
    jsonNode.put("actionId", withdrawalEvent.getActionId());
    jsonNode.put("operationType", withdrawalEvent.getOperationType().getValue());
    return jsonNode;
  }

  /**
   * Create a JsonNode representation of a default CoinWithdrawalEvent
   *
   * @return a JsonNode representation
   */
  public static JsonNode createJsonNode() {
    return toJsonNode(create());
  }
}
