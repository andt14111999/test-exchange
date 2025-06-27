package com.exchangeengine.model.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.exchangeengine.factory.event.CoinDepositEventFactory;
import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.CoinDeposit;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.DepositCache;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class CoinDepositEventTest {

  @Mock
  private CoinDeposit coinDeposit;

  private static MockedStatic<DepositCache> mockedDepositCache;
  private static DepositCache depositCacheInstance;

  @BeforeAll
  public static void setUpClass() {
    depositCacheInstance = mock(DepositCache.class);
    mockedDepositCache = mockStatic(DepositCache.class);
    mockedDepositCache.when(DepositCache::getInstance).thenReturn(depositCacheInstance);
  }

  @Test
  @DisplayName("Test create valid CoinDepositEvent")
  public void testCreateValidCoinDepositEvent() {
    // Given
    CoinDepositEvent depositEvent = CoinDepositEventFactory.create();

    // Then
    assertNotNull(depositEvent);
    assertNotNull(depositEvent.getEventId());
    assertNotNull(depositEvent.getIdentifier());
    assertNotNull(depositEvent.getAccountKey());
    assertEquals(ActionType.COIN_TRANSACTION, depositEvent.getActionType());
    assertEquals(OperationType.COIN_DEPOSIT_CREATE, depositEvent.getOperationType());
    assertEquals("usdt", depositEvent.getCoin().toLowerCase());
    assertNotNull(depositEvent.getTxHash());
    assertEquals("layer1", depositEvent.getLayer());
    assertNotNull(depositEvent.getDepositAddress());
    assertEquals("pending", depositEvent.getStatus());
    assertEquals("Deposit is being processed", depositEvent.getStatusExplanation());
    assertEquals(0, new BigDecimal("1.23").compareTo(depositEvent.getAmount()));
  }

  @Test
  @DisplayName("Test get producer event ID")
  public void testGetProducerEventId() {
    // Given
    String identifier = "test-deposit-id";
    CoinDepositEvent depositEvent = CoinDepositEventFactory.withIdentifier(identifier);

    // When
    String producerEventId = depositEvent.getProducerEventId();

    // Then
    assertEquals(identifier, producerEventId);
  }

  @Test
  @DisplayName("Test get event handler")
  public void testGetEventHandler() {
    // Given
    CoinDepositEvent depositEvent = CoinDepositEventFactory.create();

    // When
    String eventHandler = depositEvent.getEventHandler();

    // Then
    assertEquals(EventHandlerAction.DEPOSIT_EVENT, eventHandler);
  }

  @Test
  @DisplayName("Test fetch coin deposit when exists")
  public void testFetchCoinDepositWhenExists() {
    // Given
    String identifier = "test-deposit-id";
    CoinDepositEvent depositEvent = CoinDepositEventFactory.withIdentifier(identifier);

    // When
    when(depositCacheInstance.getDeposit(identifier)).thenReturn(Optional.of(coinDeposit));
    Optional<CoinDeposit> result = depositEvent.fetchCoinDeposit(false);

    // Then
    assertTrue(result.isPresent());
    assertEquals(coinDeposit, result.get());
  }

  @Test
  @DisplayName("Test fetch coin deposit when not exists and no exception required")
  public void testFetchCoinDepositWhenNotExistsNoException() {
    // Given
    String identifier = "test-deposit-id";
    CoinDepositEvent depositEvent = CoinDepositEventFactory.withIdentifier(identifier);

    // When
    when(depositCacheInstance.getDeposit(identifier)).thenReturn(Optional.empty());
    Optional<CoinDeposit> result = depositEvent.fetchCoinDeposit(false);

    // Then
    assertFalse(result.isPresent());
  }

  @Test
  @DisplayName("Test fetch coin deposit when not exists and exception required")
  public void testFetchCoinDepositWhenNotExistsWithException() {
    // Given
    String identifier = "test-deposit-id";
    CoinDepositEvent depositEvent = CoinDepositEventFactory.withIdentifier(identifier);

    // When
    when(depositCacheInstance.getDeposit(identifier)).thenReturn(Optional.empty());

    // Then
    Exception exception = assertThrows(IllegalStateException.class, () -> depositEvent.fetchCoinDeposit(true));
    assertTrue(exception.getMessage().contains("CoinDeposit not found identifier"));
  }

  @Test
  @DisplayName("Test to coin deposit when exists")
  public void testToCoinDepositWhenExists() {
    // Given
    String identifier = "test-deposit-id";
    CoinDepositEvent depositEvent = CoinDepositEventFactory.withIdentifier(identifier);

    // When
    when(depositCacheInstance.getDeposit(identifier)).thenReturn(Optional.of(coinDeposit));
    CoinDeposit result = depositEvent.toCoinDeposit(true);

    // Then
    assertEquals(coinDeposit, result);
  }

  @Test
  @DisplayName("Test to coin deposit when not exists and creates new")
  public void testToCoinDepositWhenNotExistsCreatesNew() {
    // Given
    String identifier = "test-deposit-id";
    String accountKey = "test-account-key";
    BigDecimal amount = new BigDecimal("1.23");
    String coin = "USDT";

    CoinDepositEvent depositEvent = CoinDepositEventFactory.withIdentifier(identifier);
    depositEvent.setAccountKey(accountKey);
    depositEvent.setAmount(amount);
    depositEvent.setCoin(coin);

    // When
    when(depositCacheInstance.getDeposit(identifier)).thenReturn(Optional.empty());
    CoinDeposit result = depositEvent.toCoinDeposit(false);

    // Then
    assertNotNull(result);
    assertEquals(identifier, result.getIdentifier());
    assertEquals(accountKey, result.getAccountKey());
    assertEquals(0, amount.compareTo(result.getAmount()));
    assertEquals("usdt", result.getCoin());
    assertNotNull(result.getTxHash());
    assertNotNull(result.getLayer());
    assertNotNull(result.getDepositAddress());
    assertNotNull(result.getStatus());
    assertNotNull(result.getActionType());
    assertNotNull(result.getActionId());
  }

  @Test
  @DisplayName("Test parse data from JsonNode")
  public void testParserData() {
    // Given
    String identifier = "test-deposit-id";
    String accountKey = "test-account-key";

    CoinDepositEvent sourceEvent = CoinDepositEventFactory.withIdentifier(identifier);
    sourceEvent.setAccountKey(accountKey);
    JsonNode jsonNode = CoinDepositEventFactory.toJsonNode(sourceEvent);

    // When
    CoinDepositEvent depositEvent = new CoinDepositEvent();
    depositEvent.parserData(jsonNode);

    // Then
    assertNotNull(depositEvent.getEventId());
    assertEquals(identifier, depositEvent.getIdentifier());
    assertEquals(accountKey, depositEvent.getAccountKey());
    assertNotNull(depositEvent.getAmount());
    assertEquals("usdt", depositEvent.getCoin());
    assertNotNull(depositEvent.getTxHash());
    assertEquals("layer1", depositEvent.getLayer());
    assertNotNull(depositEvent.getDepositAddress());
    assertEquals("pending", depositEvent.getStatus());
    assertEquals("Deposit is being processed", depositEvent.getStatusExplanation());
    assertEquals(ActionType.COIN_TRANSACTION, depositEvent.getActionType());
    assertNotNull(depositEvent.getActionId());
    assertEquals(OperationType.COIN_DEPOSIT_CREATE, depositEvent.getOperationType());
  }

  @Test
  @DisplayName("Test validate with valid CoinDepositEvent")
  public void testValidateValidCoinDepositEvent() {
    // Given
    String identifier = "test-deposit-id";
    CoinDepositEvent depositEvent = CoinDepositEventFactory.withIdentifier(identifier);

    // Mock necessary behaviors for validation
    when(depositCacheInstance.getDeposit(identifier)).thenReturn(Optional.empty());

    // Create a spy on the depositEvent to verify the method call
    CoinDepositEvent spyDepositEvent = spy(depositEvent);
    CoinDeposit mockCoinDeposit = mock(CoinDeposit.class);

    // Configure the spy to return our mock when toCoinDeposit is called
    doReturn(mockCoinDeposit).when(spyDepositEvent).toCoinDeposit(false);
    when(mockCoinDeposit.coinDepositValidates(anyString())).thenReturn(java.util.Collections.emptyList());

    // Then
    assertDoesNotThrow(() -> spyDepositEvent.validate());

    // Verify toCoinDeposit and coinDepositValidates were called
    verify(spyDepositEvent).toCoinDeposit(false);
    verify(mockCoinDeposit).coinDepositValidates(spyDepositEvent.getStatus());
  }

  @Test
  @DisplayName("Test validate with non-deposit create operation type")
  public void testValidateNonDepositCreateOperationType() {
    // Given
    String identifier = "test-deposit-id";
    CoinDepositEvent depositEvent = CoinDepositEventFactory.withIdentifier(identifier);

    // Change operation type to something other than COIN_DEPOSIT_CREATE
    depositEvent.setOperationType(OperationType.BALANCE_QUERY);

    // Create a spy on the depositEvent to verify the method call
    CoinDepositEvent spyDepositEvent = spy(depositEvent);

    // Then
    assertDoesNotThrow(() -> spyDepositEvent.validate());

    // Verify toCoinDeposit was not called since operation type is not
    // COIN_DEPOSIT_CREATE
    verify(spyDepositEvent, never()).toCoinDeposit(false);
  }

  @Test
  @DisplayName("Test validate with invalid CoinDepositEvent")
  public void testValidateInvalidCoinDepositEvent() {
    // Given
    String identifier = "test-deposit-id";
    CoinDepositEvent depositEvent = CoinDepositEventFactory.withIdentifier(identifier);
    depositEvent.setActionType(null); // Invalid: ActionType is null

    // Then
    Exception exception = assertThrows(IllegalArgumentException.class, () -> depositEvent.validate());
    assertTrue(exception.getMessage().contains("validate CoinDepositEvent"));
  }

  @Test
  @DisplayName("Test to operation object message JSON")
  public void testToOperationObjectMessageJson() {
    // Given
    String identifier = "test-deposit-id";
    CoinDepositEvent depositEvent = CoinDepositEventFactory.withIdentifier(identifier);

    // When
    when(depositCacheInstance.getDeposit(identifier)).thenReturn(Optional.of(coinDeposit));
    Map<String, Object> objectJson = Map.of("key", "value");
    when(coinDeposit.toMessageJson()).thenReturn(objectJson);

    // When
    Map<String, Object> result = depositEvent.toOperationObjectMessageJson();

    // Then
    assertTrue(result.containsKey("object"));
    assertEquals(objectJson, result.get("object"));
  }

  @Test
  @DisplayName("Test to operation object message JSON with exception")
  public void testToOperationObjectMessageJsonWithException() {
    // Given
    String identifier = "test-deposit-id";
    CoinDepositEvent depositEvent = CoinDepositEventFactory.withIdentifier(identifier);

    // When
    when(depositCacheInstance.getDeposit(identifier)).thenReturn(Optional.empty());

    // Then
    Exception exception = assertThrows(IllegalStateException.class,
        () -> depositEvent.toOperationObjectMessageJson());
    assertTrue(exception.getMessage().contains("CoinDeposit not found identifier"));
  }
}
