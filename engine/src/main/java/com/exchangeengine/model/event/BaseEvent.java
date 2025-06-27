package com.exchangeengine.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.OperationType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model for Disruptor events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseEvent {
  private String eventId;
  private OperationType operationType;
  private ActionType actionType;
  private String actionId;

  public String getProducerEventId() {
    return UUID.randomUUID().toString();
  }

  public List<String> validateRequiredFields() {
    List<String> errors = new ArrayList<>();
    if (eventId == null || eventId.trim().isEmpty()) {
      errors.add("Event ID is required");
    }

    if (operationType == null || !OperationType.isSupported(operationType.getValue())) {
      errors.add("Operation type not supported in list: [" + OperationType.getSupportedValues() + "]");
    }

    if (actionType == null || !ActionType.isSupported(actionType.getValue())) {
      errors.add("Action type not supported in list: [" + ActionType.getSupportedValues() + "]");
    }

    if (actionId == null || actionId.trim().isEmpty()) {
      errors.add("Action ID is required");
    }

    return errors;
  }

  public Map<String, Object> toOperationObjectMessageJson() {
    Map<String, Object> messageJson = new HashMap<>();
    messageJson.put("eventId", getEventId());
    return messageJson;
  }

  public Map<String, Object> toBaseMessageJson() {
    Map<String, Object> messageJson = new HashMap<>();
    messageJson.put("eventId", getEventId());
    messageJson.put("operationType", operationType.getValue());
    messageJson.put("actionType", actionType.getValue());
    messageJson.put("actionId", actionId);
    return messageJson;
  }

  public String getEventHandler() {
    return EventHandlerAction.UNKNOWN_EVENT;
  }
}
