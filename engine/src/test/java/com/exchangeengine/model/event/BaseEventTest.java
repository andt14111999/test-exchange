package com.exchangeengine.model.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.exchangeengine.factory.event.BaseEventFactory;
import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.OperationType;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class BaseEventTest {

  @Test
  @DisplayName("Test create valid BaseEvent")
  public void testCreateValidBaseEvent() {
    // Given
    BaseEvent baseEvent = BaseEventFactory.create();

    // Then
    assertNotNull(baseEvent);
    assertNotNull(baseEvent.getEventId());
    assertNotNull(baseEvent.getActionType());
    assertNotNull(baseEvent.getActionId());
    assertNotNull(baseEvent.getOperationType());
  }

  @Test
  @DisplayName("Test create BaseEvent with specific operation type")
  public void testCreateBaseEventWithSpecificOperationType() {
    // Given
    BaseEvent baseEvent = BaseEventFactory.withOperationType(OperationType.COIN_WITHDRAWAL_CREATE);

    // Then
    assertEquals(OperationType.COIN_WITHDRAWAL_CREATE, baseEvent.getOperationType());
  }

  @Test
  @DisplayName("Test create BaseEvent with specific action type")
  public void testCreateBaseEventWithSpecificActionType() {
    // Given
    BaseEvent baseEvent = BaseEventFactory.withActionType(ActionType.AMM_POOL);

    // Then
    assertEquals(ActionType.AMM_POOL, baseEvent.getActionType());
  }

  @Test
  @DisplayName("Test create BaseEvent with specific event ID")
  public void testCreateBaseEventWithSpecificEventId() {
    // Given
    String eventId = "custom-event-id";
    BaseEvent baseEvent = BaseEventFactory.withEventId(eventId);

    // Then
    assertEquals(eventId, baseEvent.getEventId());
  }

  @Test
  @DisplayName("Test validate required fields with all fields present")
  public void testValidateRequiredFieldsAllFieldsPresent() {
    // Given
    BaseEvent baseEvent = BaseEventFactory.create();

    // When
    List<String> errors = baseEvent.validateRequiredFields();

    // Then
    assertTrue(errors.isEmpty(), "Validation should pass with all required fields");
  }

  @Test
  @DisplayName("Test validate required fields with missing event ID")
  public void testValidateRequiredFieldsMissingEventId() {
    // Given
    BaseEvent baseEvent = BaseEventFactory.withMissingEventId();

    // When
    List<String> errors = baseEvent.validateRequiredFields();

    // Then
    assertFalse(errors.isEmpty(), "Validation should fail with missing event ID");
    assertTrue(errors.stream().anyMatch(error -> error.contains("Event ID")),
        "Error message should mention missing event ID");
  }

  @Test
  @DisplayName("Test validate required fields with empty event ID")
  public void testValidateRequiredFieldsEmptyEventId() {
    // Given
    BaseEvent baseEvent = BaseEventFactory.create();
    baseEvent.setEventId("  "); // Just whitespace characters

    // When
    List<String> errors = baseEvent.validateRequiredFields();

    // Then
    assertFalse(errors.isEmpty(), "Validation should fail with empty event ID");
    assertTrue(errors.stream().anyMatch(error -> error.contains("Event ID")),
        "Error message should mention empty event ID");
  }

  @Test
  @DisplayName("Test validate required fields with unsupported operation type value")
  public void testValidateRequiredFieldsUnsupportedOperationType() {
    // Given
    BaseEvent baseEvent = BaseEventFactory.create();

    // Using reflection to set an invalid operation type with an unsupported value
    // This is a bit hacky but allows us to test the branch where operationType !=
    // null
    // but operationType.getValue() is not supported
    OperationType mockOperationType = mock(OperationType.class);
    when(mockOperationType.getValue()).thenReturn("invalid_operation_type");
    baseEvent.setOperationType(mockOperationType);

    // When
    List<String> errors = baseEvent.validateRequiredFields();

    // Then
    assertFalse(errors.isEmpty(), "Validation should fail with unsupported operation type");
    assertTrue(errors.stream().anyMatch(error -> error.contains("Operation type")),
        "Error message should mention operation type");
  }

  @Test
  @DisplayName("Test validate required fields with null operation type")
  public void testValidateRequiredFieldsNullOperationType() {
    // Given
    BaseEvent baseEvent = BaseEventFactory.create();
    baseEvent.setOperationType(null);

    // When
    List<String> errors = baseEvent.validateRequiredFields();

    // Then
    assertFalse(errors.isEmpty(), "Validation should fail with null operation type");
    assertTrue(errors.stream().anyMatch(error -> error.contains("Operation type")),
        "Error message should mention operation type");
  }

  @Test
  @DisplayName("Test validate required fields with unsupported action type value")
  public void testValidateRequiredFieldsUnsupportedActionType() {
    // Given
    BaseEvent baseEvent = BaseEventFactory.create();

    // Using reflection to set an invalid action type with an unsupported value
    ActionType mockActionType = mock(ActionType.class);
    when(mockActionType.getValue()).thenReturn("invalid_action_type");
    baseEvent.setActionType(mockActionType);

    // When
    List<String> errors = baseEvent.validateRequiredFields();

    // Then
    assertFalse(errors.isEmpty(), "Validation should fail with unsupported action type");
    assertTrue(errors.stream().anyMatch(error -> error.contains("Action type")),
        "Error message should mention action type");
  }

  @Test
  @DisplayName("Test validate required fields with null action type")
  public void testValidateRequiredFieldsNullActionType() {
    // Given
    BaseEvent baseEvent = BaseEventFactory.create();
    baseEvent.setActionType(null);

    // When
    List<String> errors = baseEvent.validateRequiredFields();

    // Then
    assertFalse(errors.isEmpty(), "Validation should fail with null action type");
    assertTrue(errors.stream().anyMatch(error -> error.contains("Action type")),
        "Error message should mention action type");
  }

  @Test
  @DisplayName("Test validate required fields with empty action ID")
  public void testValidateRequiredFieldsEmptyActionId() {
    // Given
    BaseEvent baseEvent = BaseEventFactory.create();
    baseEvent.setActionId("");

    // When
    List<String> errors = baseEvent.validateRequiredFields();

    // Then
    assertFalse(errors.isEmpty(), "Validation should fail with empty action ID");
    assertTrue(errors.stream().anyMatch(error -> error.contains("Action ID")),
        "Error message should mention action ID");
  }

  @Test
  @DisplayName("Test validate required fields with null action ID")
  public void testValidateRequiredFieldsNullActionId() {
    // Given
    BaseEvent baseEvent = BaseEventFactory.create();
    baseEvent.setActionId(null);

    // When
    List<String> errors = baseEvent.validateRequiredFields();

    // Then
    assertFalse(errors.isEmpty(), "Validation should fail with null action ID");
    assertTrue(errors.stream().anyMatch(error -> error.contains("Action ID")),
        "Error message should mention action ID");
  }

  @Test
  @DisplayName("Test getProducerEventId returns a UUID string")
  public void testGetProducerEventId() {
    // Given
    BaseEvent baseEvent = BaseEventFactory.create();

    // When
    String producerEventId = baseEvent.getProducerEventId();

    // Then
    assertNotNull(producerEventId);
    // Attempt to parse as UUID to confirm format
    UUID uuid = UUID.fromString(producerEventId);
    assertNotNull(uuid);
  }

  @Test
  @DisplayName("Test getEventHandler returns UNKNOWN_EVENT")
  public void testGetEventHandler() {
    // Given
    BaseEvent baseEvent = BaseEventFactory.create();

    // When
    String eventHandler = baseEvent.getEventHandler();

    // Then
    assertEquals(EventHandlerAction.UNKNOWN_EVENT, eventHandler);
  }

  @Test
  @DisplayName("Test toOperationObjectMessageJson includes eventId")
  public void testToOperationObjectMessageJson() {
    // Given
    BaseEvent baseEvent = BaseEventFactory.create();
    String eventId = baseEvent.getEventId();

    // When
    Map<String, Object> json = baseEvent.toOperationObjectMessageJson();

    // Then
    assertNotNull(json);
    assertEquals(eventId, json.get("eventId"));
  }

  @Test
  @DisplayName("Test conversion to JsonNode")
  public void testToJsonNode() {
    // Given
    BaseEvent baseEvent = BaseEventFactory.create();

    // When
    JsonNode jsonNode = BaseEventFactory.toJsonNode(baseEvent);

    // Then
    assertNotNull(jsonNode);
    assertEquals(baseEvent.getEventId(), jsonNode.get("eventId").asText());
    assertEquals(baseEvent.getActionType().getValue(), jsonNode.get("actionType").asText());
    assertEquals(baseEvent.getActionId(), jsonNode.get("actionId").asText());
    assertEquals(baseEvent.getOperationType().getValue(), jsonNode.get("operationType").asText());
  }
}
