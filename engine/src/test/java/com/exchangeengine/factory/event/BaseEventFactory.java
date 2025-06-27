package com.exchangeengine.factory.event;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.event.BaseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.instancio.Instancio;
import org.instancio.Model;

import java.util.UUID;

import static org.instancio.Select.field;

/**
 * Factory class to create BaseEvent objects for testing purposes
 */
public class BaseEventFactory {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final String DEFAULT_EVENT_ID = "evt-" + UUID.randomUUID().toString().substring(0, 8);
  private static final ActionType DEFAULT_ACTION_TYPE = ActionType.COIN_TRANSACTION;
  private static final String DEFAULT_ACTION_ID = "act-" + UUID.randomUUID().toString().substring(0, 8);
  private static final OperationType DEFAULT_OPERATION_TYPE = OperationType.COIN_DEPOSIT_CREATE;

  /**
   * Create a base model for BaseEvent with default values
   *
   * @return a model for BaseEvent
   */
  public static Model<BaseEvent> model() {
    return Instancio.of(BaseEvent.class)
        .set(field(BaseEvent::getEventId), DEFAULT_EVENT_ID)
        .set(field(BaseEvent::getActionType), DEFAULT_ACTION_TYPE)
        .set(field(BaseEvent::getActionId), DEFAULT_ACTION_ID)
        .set(field(BaseEvent::getOperationType), DEFAULT_OPERATION_TYPE)
        .toModel();
  }

  /**
   * Create a BaseEvent with default values
   *
   * @return a valid BaseEvent
   */
  public static BaseEvent create() {
    return Instancio.create(model());
  }

  /**
   * Create a BaseEvent with a specific operation type
   *
   * @param operationType the operation type to set
   * @return a BaseEvent with the specified operation type
   */
  public static BaseEvent withOperationType(OperationType operationType) {
    return Instancio.of(model())
        .set(field(BaseEvent::getOperationType), operationType)
        .create();
  }

  /**
   * Create a BaseEvent with a specific action type
   *
   * @param actionType the action type to set
   * @return a BaseEvent with the specified action type
   */
  public static BaseEvent withActionType(ActionType actionType) {
    return Instancio.of(model())
        .set(field(BaseEvent::getActionType), actionType)
        .create();
  }

  /**
   * Create a BaseEvent with a specific event ID
   *
   * @param eventId the event ID to set
   * @return a BaseEvent with the specified event ID
   */
  public static BaseEvent withEventId(String eventId) {
    return Instancio.of(model())
        .set(field(BaseEvent::getEventId), eventId)
        .create();
  }

  /**
   * Create a BaseEvent with a missing event ID (for validation testing)
   *
   * @return a BaseEvent with a null event ID
   */
  public static BaseEvent withMissingEventId() {
    return Instancio.of(model())
        .set(field(BaseEvent::getEventId), null)
        .create();
  }

  /**
   * Create a JsonNode representation of a BaseEvent
   *
   * @param baseEvent the base event to convert to JsonNode
   * @return a JsonNode representation
   */
  public static JsonNode toJsonNode(BaseEvent baseEvent) {
    ObjectNode jsonNode = objectMapper.createObjectNode();
    jsonNode.put("eventId", baseEvent.getEventId());
    jsonNode.put("actionType", baseEvent.getActionType().getValue());
    jsonNode.put("actionId", baseEvent.getActionId());
    jsonNode.put("operationType", baseEvent.getOperationType().getValue());
    return jsonNode;
  }

  /**
   * Create a JsonNode representation of a valid BaseEvent with default values
   *
   * @return a JsonNode representation of a valid BaseEvent
   */
  public static JsonNode createJsonNode() {
    return toJsonNode(create());
  }
}
