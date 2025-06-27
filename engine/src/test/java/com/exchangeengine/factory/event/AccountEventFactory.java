package com.exchangeengine.factory.event;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.event.AccountEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.instancio.Instancio;
import org.instancio.Model;

import java.util.UUID;

import static org.instancio.Select.field;

/**
 * Factory class to create AccountEvent objects for testing purposes
 */
public class AccountEventFactory {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Create a model for AccountEvent with default values
   *
   * @return a model for AccountEvent
   */
  public static Model<AccountEvent> model() {
    return Instancio.of(AccountEvent.class)
        .set(field(AccountEvent::getEventId), UUID.randomUUID().toString())
        .set(field(AccountEvent::getAccountKey), "account-" + UUID.randomUUID().toString().substring(0, 8))
        .set(field(AccountEvent::getActionType), ActionType.COIN_ACCOUNT)
        .set(field(AccountEvent::getActionId), UUID.randomUUID().toString())
        .set(field(AccountEvent::getOperationType), OperationType.COIN_ACCOUNT_CREATE)
        .toModel();
  }

  /**
   * Create a valid AccountEvent with default values
   *
   * @return a valid AccountEvent
   */
  public static AccountEvent create() {
    return Instancio.create(model());
  }

  /**
   * Create an AccountEvent with a custom model modification
   *
   * @param customizer function to customize the model
   * @return customized AccountEvent
   */
  public static AccountEvent withCustomValues(String accountKey) {
    return Instancio.of(model())
        .set(field(AccountEvent::getAccountKey), accountKey)
        .create();
  }

  /**
   * Create a JsonNode representation of an AccountEvent
   *
   * @param accountEvent the account event to convert to JsonNode
   * @return a JsonNode representation
   */
  public static JsonNode toJsonNode(AccountEvent accountEvent) {
    ObjectNode jsonNode = objectMapper.createObjectNode();
    jsonNode.put("eventId", accountEvent.getEventId());
    jsonNode.put("accountKey", accountEvent.getAccountKey());
    jsonNode.put("actionType", accountEvent.getActionType().getValue());
    jsonNode.put("actionId", accountEvent.getActionId());
    jsonNode.put("operationType", accountEvent.getOperationType().getValue());
    return jsonNode;
  }

  /**
   * Create a JsonNode representation of a valid AccountEvent with default values
   *
   * @return a JsonNode representation of a valid AccountEvent
   */
  public static JsonNode createJsonNode() {
    return toJsonNode(create());
  }
}
