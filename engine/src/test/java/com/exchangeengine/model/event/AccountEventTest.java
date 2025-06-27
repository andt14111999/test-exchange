package com.exchangeengine.model.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.exchangeengine.factory.event.AccountEventFactory;
import com.exchangeengine.model.Account;
import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.AccountCache;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class AccountEventTest {

  @Mock
  private Account account;

  private static AccountCache accountCacheInstance;

  @BeforeAll
  public static void setUpClass() {
    accountCacheInstance = mock(AccountCache.class);
    AccountCache.setTestInstance(accountCacheInstance);
  }

  @AfterAll
  public static void tearDownClass() {
    AccountCache.setTestInstance(null);
  }

  @Test
  @DisplayName("Test create valid AccountEvent")
  public void testCreateValidAccountEvent() {
    // Given
    AccountEvent accountEvent = AccountEventFactory.create();

    // Then
    assertNotNull(accountEvent);
    assertNotNull(accountEvent.getEventId());
    assertNotNull(accountEvent.getAccountKey());
    assertEquals(ActionType.COIN_ACCOUNT, accountEvent.getActionType());
    assertTrue(OperationType.COIN_ACCOUNT_OPERATIONS.contains(accountEvent.getOperationType()));
  }

  @Test
  @DisplayName("Test get producer event ID")
  public void testGetProducerEventId() {
    // Given
    AccountEvent accountEvent = AccountEventFactory.create();
    String accountKey = "test-account-key";
    accountEvent.setAccountKey(accountKey);

    // When
    String producerEventId = accountEvent.getProducerEventId();

    // Then
    assertEquals(accountKey, producerEventId);
  }

  @Test
  @DisplayName("Test get event handler")
  public void testGetEventHandler() {
    // Given
    AccountEvent accountEvent = AccountEventFactory.create();

    // When
    String eventHandler = accountEvent.getEventHandler();

    // Then
    assertEquals(EventHandlerAction.ACCOUNT_EVENT, eventHandler);
  }

  @Test
  @DisplayName("Test to account")
  public void testToAccount() {
    // Given
    AccountEvent accountEvent = AccountEventFactory.create();
    String accountKey = "test-account-key";
    accountEvent.setAccountKey(accountKey);

    // When
    when(accountCacheInstance.getOrCreateAccount(accountKey)).thenReturn(account);
    Account result = accountEvent.toAccount();

    // Then
    assertEquals(account, result);
    // Not verifying getOrCreateAccount calls as it might be called multiple times
  }

  @Test
  @DisplayName("Test parse data from JsonNode")
  public void testParserData() {
    // Given
    String eventId = UUID.randomUUID().toString();
    String accountKey = "test-account-key";
    ActionType actionType = ActionType.COIN_ACCOUNT;
    String actionId = UUID.randomUUID().toString();
    OperationType operationType = OperationType.COIN_ACCOUNT_CREATE;

    AccountEvent sourceEvent = new AccountEvent();
    sourceEvent.setEventId(eventId);
    sourceEvent.setAccountKey(accountKey);
    sourceEvent.setActionType(actionType);
    sourceEvent.setActionId(actionId);
    sourceEvent.setOperationType(operationType);
    JsonNode jsonNode = AccountEventFactory.toJsonNode(sourceEvent);

    // When
    AccountEvent accountEvent = new AccountEvent();
    accountEvent.parserData(jsonNode);

    // Then
    assertEquals(eventId, accountEvent.getEventId());
    assertEquals(accountKey, accountEvent.getAccountKey());
    assertEquals(actionType, accountEvent.getActionType());
    assertEquals(actionId, accountEvent.getActionId());
    assertEquals(operationType, accountEvent.getOperationType());
  }

  @Test
  @DisplayName("Test validate with valid AccountEvent")
  public void testValidateValidAccountEvent() {
    // Given
    AccountEvent accountEvent = AccountEventFactory.create();
    String accountKey = "test-account-key";
    accountEvent.setAccountKey(accountKey);

    // When
    when(accountCacheInstance.getOrCreateAccount(accountKey)).thenReturn(account);
    when(account.validateRequiredFields()).thenReturn(java.util.Collections.emptyList());

    // Then
    assertDoesNotThrow(() -> accountEvent.validate());
  }

  @Test
  @DisplayName("Test validate with invalid operation type")
  public void testValidateInvalidOperationType() {
    // Given
    AccountEvent accountEvent = AccountEventFactory.create();
    accountEvent.setOperationType(OperationType.COIN_DEPOSIT_CREATE); // Invalid operation type for account event

    // Then
    Exception exception = assertThrows(IllegalArgumentException.class, () -> accountEvent.validate());
    assertTrue(exception.getMessage().contains("Operation type is not supported"));
  }

  @Test
  @DisplayName("Test validate with invalid action type")
  public void testValidateInvalidActionType() {
    // Given
    AccountEvent accountEvent = AccountEventFactory.create();
    accountEvent.setActionType(ActionType.COIN_TRANSACTION); // Invalid action type for account event

    // Then
    Exception exception = assertThrows(IllegalArgumentException.class, () -> accountEvent.validate());
    assertTrue(exception.getMessage().contains("Action type not matched"));
  }

  @Test
  @DisplayName("Test to operation object message JSON")
  public void testToOperationObjectMessageJson() {
    // Given
    AccountEvent accountEvent = AccountEventFactory.create();
    String accountKey = "test-account-key";
    accountEvent.setAccountKey(accountKey);

    // When
    when(accountCacheInstance.getOrCreateAccount(accountKey)).thenReturn(account);
    Map<String, Object> objectJson = Map.of("key", "value");
    when(account.toMessageJson()).thenReturn(objectJson);

    // When
    Map<String, Object> result = accountEvent.toOperationObjectMessageJson();

    // Then
    assertTrue(result.containsKey("object"));
    assertEquals(objectJson, result.get("object"));
  }
}
