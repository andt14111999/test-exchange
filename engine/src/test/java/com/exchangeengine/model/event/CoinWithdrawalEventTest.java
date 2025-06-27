package com.exchangeengine.model.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.exchangeengine.extension.CombinedTestExtension;
import com.exchangeengine.factory.event.CoinWithdrawalEventFactory;
import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.CoinWithdrawal;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.WithdrawalCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@ExtendWith({ MockitoExtension.class, CombinedTestExtension.class })
public class CoinWithdrawalEventTest {

  @Mock(lenient = true)
  private CoinWithdrawal coinWithdrawal;

  @Mock(lenient = true)
  private WithdrawalCache withdrawalCache;

  @BeforeEach
  public void setUp() {
    // Sử dụng setTestInstance trực tiếp trên WithdrawalCache
    WithdrawalCache.setTestInstance(withdrawalCache);
  }

  @AfterEach
  public void tearDown() {
    // Reset test instance
    WithdrawalCache.setTestInstance(null);
  }

  @Test
  @DisplayName("Test create valid CoinWithdrawalEvent")
  public void testCreateValidCoinWithdrawalEvent() {
    // Given
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.create();

    // Then
    assertNotNull(withdrawalEvent);
    assertNotNull(withdrawalEvent.getEventId());
    assertNotNull(withdrawalEvent.getIdentifier());
    assertNotNull(withdrawalEvent.getAccountKey());
    assertEquals(ActionType.COIN_TRANSACTION, withdrawalEvent.getActionType());
    assertEquals(OperationType.COIN_WITHDRAWAL_CREATE, withdrawalEvent.getOperationType());
    assertEquals("usdt", withdrawalEvent.getCoin().toLowerCase());
    assertNotNull(withdrawalEvent.getTxHash());
    assertEquals("layer1", withdrawalEvent.getLayer());
    assertNotNull(withdrawalEvent.getDestinationAddress());
    assertEquals("verified", withdrawalEvent.getStatus());
    assertNotNull(withdrawalEvent.getStatusExplanation());
    assertEquals(0, new BigDecimal("1.23").compareTo(withdrawalEvent.getAmount()));
    assertEquals(0, new BigDecimal("0.01").compareTo(withdrawalEvent.getFee()));
  }

  @Test
  @DisplayName("Test get producer event ID")
  public void testGetProducerEventId() {
    // Given
    String identifier = "test-withdrawal-id";
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.withIdentifier(identifier);

    // When
    String producerEventId = withdrawalEvent.getProducerEventId();

    // Then
    assertEquals(identifier, producerEventId);
  }

  @Test
  @DisplayName("Test get event handler")
  public void testGetEventHandler() {
    // Given
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.create();

    // When
    String eventHandler = withdrawalEvent.getEventHandler();

    // Then
    assertEquals(EventHandlerAction.WITHDRAWAL_EVENT, eventHandler);
  }

  @Test
  @DisplayName("Test fetch coin withdrawal when exists")
  public void testFetchCoinWithdrawalWhenExists() {
    // Given
    String identifier = "test-withdrawal-id";
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.withIdentifier(identifier);

    // Configure mock specifically for this test
    when(withdrawalCache.getWithdrawal(identifier)).thenReturn(Optional.of(coinWithdrawal));

    // When
    Optional<CoinWithdrawal> result = withdrawalEvent.fetchCoinWithdrawal(false);

    // Then
    assertTrue(result.isPresent());
    assertEquals(coinWithdrawal, result.get());
    verify(withdrawalCache).getWithdrawal(identifier);
  }

  @Test
  @DisplayName("Test fetch coin withdrawal when not exists and no exception required")
  public void testFetchCoinWithdrawalWhenNotExistsNoException() {
    // Given
    String identifier = "test-withdrawal-id";
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.withIdentifier(identifier);

    // Configure mock specifically for this test
    when(withdrawalCache.getWithdrawal(identifier)).thenReturn(Optional.empty());

    // When
    Optional<CoinWithdrawal> result = withdrawalEvent.fetchCoinWithdrawal(false);

    // Then
    assertFalse(result.isPresent());
    verify(withdrawalCache).getWithdrawal(identifier);
  }

  @Test
  @DisplayName("Test fetch coin withdrawal when not exists and exception required")
  public void testFetchCoinWithdrawalWhenNotExistsWithException() {
    // Given
    String identifier = "test-withdrawal-id";
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.withIdentifier(identifier);

    // Configure mock specifically for this test
    when(withdrawalCache.getWithdrawal(identifier)).thenReturn(Optional.empty());

    // Then
    Exception exception = assertThrows(IllegalStateException.class, () -> withdrawalEvent.fetchCoinWithdrawal(true));
    assertTrue(exception.getMessage().contains("CoinWithdrawal not found identifier"));
    verify(withdrawalCache).getWithdrawal(identifier);
  }

  @Test
  @DisplayName("Test to coin withdrawal when exists")
  public void testToCoinWithdrawalWhenExists() {
    // Given
    String identifier = "test-withdrawal-id";
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.withIdentifier(identifier);

    // Configure mock specifically for this test
    when(withdrawalCache.getWithdrawal(identifier)).thenReturn(Optional.of(coinWithdrawal));

    // When
    CoinWithdrawal result = withdrawalEvent.toCoinWithdrawal(true);

    // Then
    assertEquals(coinWithdrawal, result);
    verify(withdrawalCache).getWithdrawal(identifier);
  }

  @Test
  @DisplayName("Test to coin withdrawal when not exists and creates new")
  public void testToCoinWithdrawalWhenNotExistsCreatesNew() {
    // Given
    String identifier = "test-withdrawal-id";
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.withIdentifier(identifier);

    // Explicitly set coin to lowercase for the test
    withdrawalEvent.setCoin(withdrawalEvent.getCoin().toLowerCase());

    // Configure mock specifically for this test
    when(withdrawalCache.getWithdrawal(identifier)).thenReturn(Optional.empty());

    // When
    CoinWithdrawal result = withdrawalEvent.toCoinWithdrawal(false);

    // Then
    assertNotNull(result);
    assertEquals(identifier, result.getIdentifier());
    assertEquals(withdrawalEvent.getAccountKey(), result.getAccountKey());
    assertEquals(0, withdrawalEvent.getAmount().compareTo(result.getAmount()));
    // Coin is set directly from the event without case conversion in
    // toCoinWithdrawal
    assertEquals("usdt", result.getCoin().toLowerCase());
    assertEquals(withdrawalEvent.getTxHash(), result.getTxHash());
    assertEquals(withdrawalEvent.getLayer(), result.getLayer());
    assertEquals(withdrawalEvent.getDestinationAddress(), result.getDestinationAddress());
    assertEquals(withdrawalEvent.getStatus(), result.getStatus());
    // ActionType không có phương thức isEqualTo, nhưng CoinTransaction có thể có
    // cách khác để lưu trữ ActionType
    assertEquals(ActionType.COIN_TRANSACTION.getValue(), result.getActionType().toString());
    assertEquals(withdrawalEvent.getActionId(), result.getActionId());
    assertEquals(0, withdrawalEvent.getFee().compareTo(result.getFee()));
    verify(withdrawalCache).getWithdrawal(identifier);
  }

  @Test
  @DisplayName("Test parse data from JsonNode")
  public void testParserData() {
    // Given
    String identifier = "test-withdrawal-id";
    CoinWithdrawalEvent sourceEvent = CoinWithdrawalEventFactory.create();
    // Đặt lại chính xác lượng
    sourceEvent.setIdentifier(identifier);

    JsonNode jsonNode = CoinWithdrawalEventFactory.toJsonNode(sourceEvent);

    // When
    CoinWithdrawalEvent withdrawalEvent = new CoinWithdrawalEvent();
    withdrawalEvent.parserData(jsonNode);

    // Then
    assertNotNull(withdrawalEvent.getEventId());
    assertEquals(identifier, withdrawalEvent.getIdentifier());
    assertEquals(sourceEvent.getAccountKey(), withdrawalEvent.getAccountKey());

    // Trong parserData, số được lưu dưới dạng double trong JsonNode, sau đó được
    // chuyển đổi trở lại thành BigDecimal
    // Điều này có thể gây ra sự khác biệt nhỏ, chỉ kiểm tra rằng chúng gần bằng
    // nhau
    assertTrue(Math.abs(sourceEvent.getAmount().doubleValue() - withdrawalEvent.getAmount().doubleValue()) < 0.0001);

    // parserData explicitly converts to lowercase
    assertEquals("usdt", withdrawalEvent.getCoin());
    assertEquals(sourceEvent.getTxHash(), withdrawalEvent.getTxHash());
    assertEquals(sourceEvent.getLayer(), withdrawalEvent.getLayer());
    assertEquals(sourceEvent.getDestinationAddress(), withdrawalEvent.getDestinationAddress());
    assertEquals(sourceEvent.getStatus(), withdrawalEvent.getStatus());
    assertEquals(sourceEvent.getStatusExplanation(), withdrawalEvent.getStatusExplanation());
    assertEquals(sourceEvent.getActionType(), withdrawalEvent.getActionType());
    assertEquals(sourceEvent.getActionId(), withdrawalEvent.getActionId());
    assertEquals(sourceEvent.getOperationType(), withdrawalEvent.getOperationType());

    // Tương tự cho fee, kiểm tra gần bằng nhau thay vì bằng chính xác
    assertTrue(Math.abs(sourceEvent.getFee().doubleValue() - withdrawalEvent.getFee().doubleValue()) < 0.0001);
  }

  @Test
  @DisplayName("Test validate with COIN_WITHDRAWAL_CREATE operation type")
  public void testValidateWithCoinWithdrawalCreateOperationType() {
    // Given
    String identifier = "test-withdrawal-id";
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.withIdentifierAndOperationType(
        identifier, OperationType.COIN_WITHDRAWAL_CREATE);

    // Create a spy on the withdrawalEvent to verify the method call
    CoinWithdrawalEvent spyWithdrawalEvent = spy(withdrawalEvent);
    CoinWithdrawal mockCoinWithdrawal = mock(CoinWithdrawal.class);

    // Configure the spy to return our mock when toCoinWithdrawal is called
    doReturn(mockCoinWithdrawal).when(spyWithdrawalEvent).toCoinWithdrawal(false);
    when(mockCoinWithdrawal.coinWithdrawalValidates(anyString())).thenReturn(java.util.Collections.emptyList());

    // Then
    assertDoesNotThrow(() -> spyWithdrawalEvent.validate());

    // Verify toCoinWithdrawal and coinWithdrawalValidates were called
    verify(spyWithdrawalEvent).toCoinWithdrawal(false);
    verify(mockCoinWithdrawal).coinWithdrawalValidates(spyWithdrawalEvent.getStatus());
  }

  @Test
  @DisplayName("Test validate with COIN_WITHDRAWAL_RELEASING operation type")
  public void testValidateWithCoinWithdrawalReleasingOperationType() {
    // Given
    String identifier = "test-withdrawal-id";
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.withIdentifierAndOperationType(
        identifier, OperationType.COIN_WITHDRAWAL_RELEASING);

    // Configure mock specifically for this test
    when(withdrawalCache.getWithdrawal(identifier)).thenReturn(Optional.of(coinWithdrawal));
    when(coinWithdrawal.coinWithdrawalReleasingValidates(anyString())).thenReturn(java.util.Collections.emptyList());

    // Create a spy for the withdrawal event
    CoinWithdrawalEvent spyWithdrawalEvent = spy(withdrawalEvent);
    doReturn(Optional.of(coinWithdrawal)).when(spyWithdrawalEvent).fetchCoinWithdrawal(true);

    // Then
    assertDoesNotThrow(() -> spyWithdrawalEvent.validate());

    // Verify the appropriate methods were called
    verify(spyWithdrawalEvent).fetchCoinWithdrawal(true);
    verify(coinWithdrawal).coinWithdrawalReleasingValidates(spyWithdrawalEvent.getOperationType().getValue());
  }

  @Test
  @DisplayName("Test validate with COIN_WITHDRAWAL_FAILED operation type")
  public void testValidateWithCoinWithdrawalFailedOperationType() {
    // Given
    String identifier = "test-withdrawal-id";
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.withIdentifierAndOperationType(
        identifier, OperationType.COIN_WITHDRAWAL_FAILED);

    // Configure mock specifically for this test
    when(withdrawalCache.getWithdrawal(identifier)).thenReturn(Optional.of(coinWithdrawal));
    when(coinWithdrawal.coinWithdrawalFailedValidates(anyString())).thenReturn(java.util.Collections.emptyList());

    // Create a spy for the withdrawal event
    CoinWithdrawalEvent spyWithdrawalEvent = spy(withdrawalEvent);
    doReturn(Optional.of(coinWithdrawal)).when(spyWithdrawalEvent).fetchCoinWithdrawal(true);

    // Then
    assertDoesNotThrow(() -> spyWithdrawalEvent.validate());

    // Verify the appropriate methods were called
    verify(spyWithdrawalEvent).fetchCoinWithdrawal(true);
    verify(coinWithdrawal).coinWithdrawalFailedValidates(spyWithdrawalEvent.getOperationType().getValue());
  }

  @Test
  @DisplayName("Test validate with COIN_WITHDRAWAL_CANCELLED operation type")
  public void testValidateWithCoinWithdrawalCancelledOperationType() {
    // Given
    String identifier = "test-withdrawal-id";
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.withIdentifierAndOperationType(
        identifier, OperationType.COIN_WITHDRAWAL_CANCELLED);

    // Configure mock specifically for this test
    when(withdrawalCache.getWithdrawal(identifier)).thenReturn(Optional.of(coinWithdrawal));
    when(coinWithdrawal.coinWithdrawalCancelledValidates(anyString())).thenReturn(java.util.Collections.emptyList());

    // Create a spy for the withdrawal event
    CoinWithdrawalEvent spyWithdrawalEvent = spy(withdrawalEvent);
    doReturn(Optional.of(coinWithdrawal)).when(spyWithdrawalEvent).fetchCoinWithdrawal(true);

    // Then
    assertDoesNotThrow(() -> spyWithdrawalEvent.validate());

    // Verify the appropriate methods were called
    verify(spyWithdrawalEvent).fetchCoinWithdrawal(true);
    verify(coinWithdrawal).coinWithdrawalCancelledValidates(spyWithdrawalEvent.getOperationType().getValue());
  }

  @Test
  @DisplayName("Test validate with invalid CoinWithdrawalEvent")
  public void testValidateInvalidCoinWithdrawalEvent() {
    // Given
    String identifier = "test-withdrawal-id";
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.withIdentifier(identifier);
    withdrawalEvent.setActionType(null); // Invalid: ActionType is null

    // Then
    Exception exception = assertThrows(IllegalArgumentException.class, () -> withdrawalEvent.validate());
    assertTrue(exception.getMessage().contains("validate CoinWithdrawalEvent"));
  }

  @Test
  @DisplayName("Test to operation object message JSON")
  public void testToOperationObjectMessageJson() {
    // Given
    String identifier = "test-withdrawal-id";
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.withIdentifier(identifier);

    // Configure mock specifically for this test
    when(withdrawalCache.getWithdrawal(identifier)).thenReturn(Optional.of(coinWithdrawal));
    Map<String, Object> objectJson = Map.of("key", "value");
    when(coinWithdrawal.toMessageJson()).thenReturn(objectJson);

    // Create a spy for the withdrawal event
    CoinWithdrawalEvent spyWithdrawalEvent = spy(withdrawalEvent);
    doReturn(Optional.of(coinWithdrawal)).when(spyWithdrawalEvent).fetchCoinWithdrawal(true);

    // When
    Map<String, Object> result = spyWithdrawalEvent.toOperationObjectMessageJson();

    // Then
    assertTrue(result.containsKey("object"));
    assertEquals(objectJson, result.get("object"));
    verify(spyWithdrawalEvent).fetchCoinWithdrawal(true);
    verify(coinWithdrawal).toMessageJson();
  }

  @Test
  @DisplayName("Test to operation object message JSON with exception")
  public void testToOperationObjectMessageJsonWithException() {
    // Given
    String identifier = "test-withdrawal-id";
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.withIdentifier(identifier);

    // Configure mock specifically for this test
    when(withdrawalCache.getWithdrawal(identifier)).thenReturn(Optional.empty());

    // When/Then
    Exception exception = assertThrows(IllegalStateException.class,
        () -> withdrawalEvent.toOperationObjectMessageJson());
    assertTrue(exception.getMessage().contains("CoinWithdrawal not found identifier"));
    verify(withdrawalCache).getWithdrawal(identifier);
  }

  @Test
  @DisplayName("Test validate with invalid operation type")
  public void testValidateWithInvalidOperationType() {
    // Given
    String identifier = "test-withdrawal-id";
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.withIdentifier(identifier);

    // Set an operation type that's valid in the system but not valid for coin
    // withdrawals
    withdrawalEvent.setOperationType(OperationType.COIN_ACCOUNT_CREATE);

    // Then - không cần mock vì validate sẽ phát hiện operation type không hợp lệ
    // trước khi gọi đến withdrawal cache
    Exception exception = assertThrows(IllegalArgumentException.class, () -> withdrawalEvent.validate());
    assertTrue(exception.getMessage().contains("Unsupported operation type"));
    assertTrue(exception.getMessage().contains(OperationType.COIN_ACCOUNT_CREATE.getValue()));
  }

  @Test
  @DisplayName("Test parse data with null recipientAccountKey")
  public void testParserDataWithNullRecipientAccountKey() {
    // Given
    String identifier = "test-withdrawal-id";
    CoinWithdrawalEvent sourceEvent = CoinWithdrawalEventFactory.create();
    sourceEvent.setIdentifier(identifier);
    sourceEvent.setRecipientAccountKey(null); // Set to null

    JsonNode jsonNode = CoinWithdrawalEventFactory.toJsonNode(sourceEvent);

    // When
    CoinWithdrawalEvent withdrawalEvent = new CoinWithdrawalEvent();
    withdrawalEvent.parserData(jsonNode);

    // Then
    assertNull(withdrawalEvent.getRecipientAccountKey());
    assertEquals(identifier, withdrawalEvent.getIdentifier());
  }

  @Test
  @DisplayName("Test parse data with missing recipientAccountKey field")
  public void testParserDataWithMissingRecipientAccountKey() {
    // Given
    String jsonString = "{"
        + "\"eventId\":\"test-event-id\","
        + "\"identifier\":\"test-withdrawal-id\","
        + "\"actionType\":\"COIN_TRANSACTION\","
        + "\"actionId\":\"test-action-id\","
        + "\"operationType\":\"coin_withdrawal_create\","
        + "\"accountKey\":\"btc:user123\","
        + "\"amount\":1.0,"
        + "\"coin\":\"BTC\","
        + "\"txHash\":\"tx123\","
        + "\"layer\":\"L1\","
        + "\"destinationAddress\":\"addr123\","
        + "\"fee\":0.1,"
        + "\"status\":\"pending\","
        + "\"statusExplanation\":\"Processing\""
        + "}";

    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode;
    try {
      jsonNode = mapper.readTree(jsonString);
    } catch (Exception e) {
      fail("Failed to parse JSON: " + e.getMessage());
      return;
    }

    // When
    CoinWithdrawalEvent withdrawalEvent = new CoinWithdrawalEvent();
    withdrawalEvent.parserData(jsonNode);

    // Then
    assertNull(withdrawalEvent.getRecipientAccountKey());
    assertEquals("test-withdrawal-id", withdrawalEvent.getIdentifier());
    assertEquals("btc", withdrawalEvent.getCoin()); // Should be converted to lowercase
  }
}
