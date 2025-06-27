package com.exchangeengine.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BaseTransactionTest {

  private static class TestTransaction extends BaseTransaction {
    // Concrete implementation for testing abstract class
  }

  @Test
  @DisplayName("Constructor should initialize timestamps")
  void constructor_ShouldInitializeTimestamps() {
    TestTransaction transaction = new TestTransaction();
    assertTrue(transaction.getCreatedAt() > 0);
    assertEquals(transaction.getCreatedAt(), transaction.getUpdatedAt());
  }

  @Test
  @DisplayName("setStatus should update the status and updatedAt timestamp")
  void setStatus_ShouldUpdateStatusAndTimestamp() {
    TestTransaction transaction = new TestTransaction();
    long initialTimestamp = transaction.getUpdatedAt();

    // Wait a bit to ensure timestamp changes
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
    }

    transaction.setStatus("completed");

    assertEquals("completed", transaction.getStatus());
    assertTrue(transaction.getUpdatedAt() > initialTimestamp);
  }

  @Test
  @DisplayName("updateStatus should update status and timestamp")
  void updateStatus_ShouldUpdateStatusAndTimestamp() {
    TestTransaction transaction = new TestTransaction();
    long initialTimestamp = transaction.getUpdatedAt();

    // Wait a bit to ensure timestamp changes
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
    }

    transaction.updateStatus("failed");

    assertEquals("failed", transaction.getStatus());
    assertTrue(transaction.getUpdatedAt() > initialTimestamp);
  }

  @Test
  @DisplayName("validateRequiredFields should return errors for missing required fields")
  void validateRequiredFields_ShouldReturnErrorsForMissingFields() {
    TestTransaction transaction = new TestTransaction();
    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Identifier is required"));
    assertTrue(errors.contains("Action type is required"));
    assertTrue(errors.contains("Action ID is required"));
    assertTrue(errors.contains("Account key is required"));
    assertTrue(errors.contains("Status is required"));
    assertEquals(5, errors.size());
  }

  @Test
  @DisplayName("validateRequiredFields should return error when actionId is null")
  void validateRequiredFields_ShouldReturnError_WhenActionIdIsNull() {
    TestTransaction transaction = new TestTransaction();
    transaction.setIdentifier("txn123");
    transaction.setActionType(ActionType.COIN_TRANSACTION);
    // actionId intentionally left null
    transaction.setAccountKey("btc:user123");
    transaction.setStatus("pending");

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Action ID is required"));
    assertEquals(1, errors.size());
  }

  @Test
  @DisplayName("validateRequiredFields should return error when actionId is empty")
  void validateRequiredFields_ShouldReturnError_WhenActionIdIsEmpty() {
    TestTransaction transaction = new TestTransaction();
    transaction.setIdentifier("txn123");
    transaction.setActionType(ActionType.COIN_TRANSACTION);
    transaction.setActionId("   "); // Empty after trim
    transaction.setAccountKey("btc:user123");
    transaction.setStatus("pending");

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Action ID is required"));
    assertEquals(1, errors.size());
  }

  @Test
  @DisplayName("validateRequiredFields should return error when status is null")
  void validateRequiredFields_ShouldReturnError_WhenStatusIsNull() {
    TestTransaction transaction = new TestTransaction();
    transaction.setIdentifier("txn123");
    transaction.setActionType(ActionType.COIN_TRANSACTION);
    transaction.setActionId("act-123");
    transaction.setAccountKey("btc:user123");
    // status intentionally left null

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Status is required"));
    assertEquals(1, errors.size());
  }

  @Test
  @DisplayName("validateRequiredFields should return error when status is empty")
  void validateRequiredFields_ShouldReturnError_WhenStatusIsEmpty() {
    TestTransaction transaction = new TestTransaction();
    transaction.setIdentifier("txn123");
    transaction.setActionType(ActionType.COIN_TRANSACTION);
    transaction.setActionId("act-123");
    transaction.setAccountKey("btc:user123");
    transaction.setStatus("  "); // Empty after trim

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Status is required"));
    assertEquals(1, errors.size());
  }

  @Test
  @DisplayName("validateRequiredFields should return no errors when all required fields are provided")
  void validateRequiredFields_ShouldReturnNoErrorsWhenAllFieldsPresent() {
    TestTransaction transaction = new TestTransaction();
    transaction.setIdentifier("txn123");
    transaction.setActionType(ActionType.COIN_TRANSACTION);
    transaction.setActionId("act-123");
    transaction.setAccountKey("btc:user123");
    transaction.setStatus("pending");

    List<String> errors = transaction.validateRequiredFields();
    assertTrue(errors.isEmpty());
  }

  @Test
  @DisplayName("getIdentifier should return lowercase identifier")
  void getIdentifier_ShouldReturnLowercaseIdentifier() {
    TestTransaction transaction = new TestTransaction();
    transaction.setIdentifier("TXN123");
    assertEquals("txn123", transaction.getIdentifier());
  }

  @Test
  @DisplayName("setIdentifier should store lowercase identifier")
  void setIdentifier_ShouldStoreLowercaseIdentifier() {
    TestTransaction transaction = new TestTransaction();
    transaction.setIdentifier("TXN123");
    assertEquals("txn123", transaction.getIdentifier());
  }

  @Test
  @DisplayName("getActionType should return value from enum")
  void getActionType_ShouldReturnValueFromEnum() {
    TestTransaction transaction = new TestTransaction();
    transaction.setActionType(ActionType.COIN_TRANSACTION);
    assertEquals(ActionType.COIN_TRANSACTION.getValue(), transaction.getActionType());
  }

  @Test
  @DisplayName("setActionType with string should set correct enum")
  void setActionType_WithString_ShouldSetCorrectEnum() {
    TestTransaction transaction = new TestTransaction();
    transaction.setActionType("CoinTransaction");
    assertEquals(ActionType.COIN_TRANSACTION.getValue(), transaction.getActionType());
  }

  @Test
  @DisplayName("toString should include all relevant fields")
  void toString_ShouldIncludeAllRelevantFields() {
    TestTransaction transaction = new TestTransaction();
    transaction.setIdentifier("txn123");
    transaction.setActionType(ActionType.COIN_TRANSACTION);
    transaction.setActionId("act-123");
    transaction.setAccountKey("btc:user123");
    transaction.setStatus("pending");

    String toString = transaction.toString();

    assertTrue(toString.contains("actionType=" + ActionType.COIN_TRANSACTION));
    assertTrue(toString.contains("actionId='act-123'"));
    assertTrue(toString.contains("accountKey='btc:user123'"));
    assertTrue(toString.contains("identifier='txn123'"));
    assertTrue(toString.contains("status='pending'"));
  }

  @Test
  @DisplayName("setters and getters should work properly")
  void settersAndGetters_ShouldWorkProperly() {
    TestTransaction transaction = new TestTransaction();

    transaction.setActionType(ActionType.COIN_TRANSACTION);
    transaction.setActionId("act-123");
    transaction.setAccountKey("btc:user123");
    transaction.setIdentifier("txn123");
    transaction.setStatus("pending");
    transaction.setStatusExplanation("Processing");

    long createdAt = Instant.now().toEpochMilli();
    long updatedAt = createdAt + 1000;
    transaction.setCreatedAt(createdAt);
    transaction.setUpdatedAt(updatedAt);

    assertEquals(ActionType.COIN_TRANSACTION.getValue(), transaction.getActionType());
    assertEquals("act-123", transaction.getActionId());
    assertEquals("btc:user123", transaction.getAccountKey());
    assertEquals("txn123", transaction.getIdentifier());
    assertEquals("pending", transaction.getStatus());
    assertEquals("Processing", transaction.getStatusExplanation());
    assertEquals(createdAt, transaction.getCreatedAt());
    assertEquals(updatedAt, transaction.getUpdatedAt());
  }

  @Test
  @DisplayName("validateRequiredFields should return error when accountKey is null")
  void validateRequiredFields_ShouldReturnError_WhenAccountKeyIsNull() {
    TestTransaction transaction = new TestTransaction();
    transaction.setIdentifier("txn123");
    transaction.setActionType(ActionType.COIN_TRANSACTION);
    transaction.setActionId("act-123");
    // accountKey intentionally left null
    transaction.setStatus("pending");

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Account key is required"));
    assertEquals(1, errors.size());
  }

  @Test
  @DisplayName("validateRequiredFields should return error when accountKey is empty")
  void validateRequiredFields_ShouldReturnError_WhenAccountKeyIsEmpty() {
    TestTransaction transaction = new TestTransaction();
    transaction.setIdentifier("txn123");
    transaction.setActionType(ActionType.COIN_TRANSACTION);
    transaction.setActionId("act-123");
    transaction.setAccountKey("   "); // Empty after trim
    transaction.setStatus("pending");

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Account key is required"));
    assertEquals(1, errors.size());
  }

  @Test
  @DisplayName("validateRequiredFields should return error when identifier is null")
  void validateRequiredFields_ShouldReturnError_WhenIdentifierIsNull() {
    TestTransaction transaction = new TestTransaction();
    // identifier intentionally left null
    transaction.setActionType(ActionType.COIN_TRANSACTION);
    transaction.setActionId("act-123");
    transaction.setAccountKey("btc:user123");
    transaction.setStatus("pending");

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Identifier is required"));
    assertEquals(1, errors.size());
  }

  @Test
  @DisplayName("validateRequiredFields should return error when identifier is empty")
  void validateRequiredFields_ShouldReturnError_WhenIdentifierIsEmpty() {
    TestTransaction transaction = new TestTransaction();
    transaction.setIdentifier("   "); // Empty after trim
    transaction.setActionType(ActionType.COIN_TRANSACTION);
    transaction.setActionId("act-123");
    transaction.setAccountKey("btc:user123");
    transaction.setStatus("pending");

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Identifier is required"));
    assertEquals(1, errors.size());
  }

  @Test
  @DisplayName("validateRequiredFields should return error when actionType is null")
  void validateRequiredFields_ShouldReturnError_WhenActionTypeIsNull() {
    TestTransaction transaction = new TestTransaction();
    transaction.setIdentifier("txn123");
    // actionType intentionally left null
    transaction.setActionId("act-123");
    transaction.setAccountKey("btc:user123");
    transaction.setStatus("pending");

    List<String> errors = transaction.validateRequiredFields();

    assertTrue(errors.contains("Action type is required"));
    assertEquals(1, errors.size());
  }

  @Test
  @DisplayName("setIdentifier should handle null value")
  void setIdentifier_ShouldHandleNullValue() {
    TestTransaction transaction = new TestTransaction();
    transaction.setIdentifier(null);
    assertNull(transaction.getIdentifier());
  }

  @Test
  @DisplayName("setStatusExplanation should update explanation")
  void setStatusExplanation_ShouldUpdateExplanation() {
    TestTransaction transaction = new TestTransaction();
    transaction.setStatusExplanation("Test explanation");
    assertEquals("Test explanation", transaction.getStatusExplanation());
  }
}
