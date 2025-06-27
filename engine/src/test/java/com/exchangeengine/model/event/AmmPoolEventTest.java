package com.exchangeengine.model.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.exchangeengine.factory.event.AmmPoolEventFactory;
import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.AmmPool;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.AmmPoolCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AmmPoolEventTest {

  @Mock(lenient = true)
  private AmmPool ammPool;

  @Mock(lenient = true)
  private AmmPoolCache ammPoolCache;

  @BeforeEach
  void setUp() {
    // Sử dụng setTestInstance thay vì MockedStatic
    AmmPoolCache.setTestInstance(ammPoolCache);
    when(ammPool.getTotalValueLockedToken0()).thenReturn(BigDecimal.ZERO);
    when(ammPool.getTotalValueLockedToken1()).thenReturn(BigDecimal.ZERO);
  }

  @AfterEach
  void tearDown() {
    // Reset test instance
    AmmPoolCache.setTestInstance(null);
  }

  @Test
  @DisplayName("Test create valid AmmPoolEvent")
  void testCreateValidAmmPoolEvent() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.create();

    // Then
    assertNotNull(event);
    assertEquals(ActionType.AMM_POOL, event.getActionType());
    assertTrue(OperationType.AMM_POOL_OPERATIONS.contains(event.getOperationType()));
    assertNotNull(event.getPair());
    assertNotNull(event.getToken0());
    assertNotNull(event.getToken1());
    assertTrue(event.getFeePercentage() >= 0);
    assertTrue(event.getFeeProtocolPercentage() >= 0);
  }

  @Test
  @DisplayName("Test getProducerEventId returns pair")
  void testGetProducerEventId() {
    // Given
    String pair = "USDT/VND";
    AmmPoolEvent event = AmmPoolEventFactory.withPair(pair);

    // When
    String producerEventId = event.getProducerEventId();

    // Then
    assertEquals(pair, producerEventId);
  }

  @Test
  @DisplayName("Test getEventHandler returns AMM_POOL_EVENT")
  void testGetEventHandler() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.create();

    // When
    String handler = event.getEventHandler();

    // Then
    assertEquals(EventHandlerAction.AMM_POOL_EVENT, handler);
  }

  @Test
  @DisplayName("Test fetchAmmPool when pool exists")
  void testFetchAmmPoolWhenExists() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.create();
    String pair = event.getPair();

    // Configure mocks
    when(ammPoolCache.getAmmPool(pair)).thenReturn(Optional.of(ammPool));

    // When
    Optional<AmmPool> result = event.fetchAmmPool(false);

    // Then
    assertTrue(result.isPresent());
    assertEquals(ammPool, result.get());
    verify(ammPoolCache).getAmmPool(pair);
  }

  @Test
  @DisplayName("Test fetchAmmPool when pool does not exist and no exception")
  void testFetchAmmPoolWhenNotExistsNoException() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.create();
    String pair = event.getPair();

    // Configure mocks
    when(ammPoolCache.getAmmPool(pair)).thenReturn(Optional.empty());

    // When
    Optional<AmmPool> result = event.fetchAmmPool(false);

    // Then
    assertFalse(result.isPresent());
    verify(ammPoolCache).getAmmPool(pair);
  }

  @Test
  @DisplayName("Test fetchAmmPool when pool does not exist and raise exception")
  void testFetchAmmPoolWhenNotExistsRaiseException() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.create();
    String pair = event.getPair();

    // Configure mocks
    when(ammPoolCache.getAmmPool(pair)).thenReturn(Optional.empty());

    // When/Then
    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
      event.fetchAmmPool(true);
    });
    assertTrue(exception.getMessage().contains(pair));
    assertTrue(exception.getMessage().contains("not found"));
    verify(ammPoolCache).getAmmPool(pair);
  }

  @Test
  @DisplayName("Test toAmmPool creates new pool when not exists")
  void testToAmmPoolCreatesNewPool() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.create();
    String pair = event.getPair();
    String token0 = event.getToken0();
    String token1 = event.getToken1();
    double feePercentage = event.getFeePercentage();
    double feeProtocolPercentage = event.getFeeProtocolPercentage();
    boolean isActive = event.isActive();

    // Configure mocks
    when(ammPoolCache.getAmmPool(pair)).thenReturn(Optional.empty());

    // When
    AmmPool result = event.toAmmPool(false);

    // Then
    assertNotNull(result);
    assertEquals(pair, result.getPair());
    assertEquals(token0, result.getToken0());
    assertEquals(token1, result.getToken1());
    assertEquals(feePercentage, result.getFeePercentage());
    assertEquals(feeProtocolPercentage, result.getFeeProtocolPercentage());
    assertEquals(isActive, result.isActive());
    verify(ammPoolCache).getAmmPool(pair);
  }

  @Test
  @DisplayName("Test toAmmPool returns existing pool")
  void testToAmmPoolReturnsExistingPool() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.create();
    String pair = event.getPair();

    // Configure mocks
    when(ammPoolCache.getAmmPool(pair)).thenReturn(Optional.of(ammPool));

    // When
    AmmPool result = event.toAmmPool(true);

    // Then
    assertNotNull(result);
    assertEquals(ammPool, result);
    verify(ammPoolCache).getAmmPool(pair);
  }

  @Test
  @DisplayName("Test validate with valid AMM_POOL_CREATE event")
  void testValidateValidCreateEvent() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.create();
    event.setToken0("USDT");
    event.setToken1("VND");

    // Use spy without unnecessary mocks
    AmmPoolEvent spyEvent = spy(event);
    // Force toAmmPool to return a valid pool
    AmmPool mockPool = mock(AmmPool.class);
    lenient().when(mockPool.validateRequiredFields()).thenReturn(new ArrayList<>());
    doReturn(mockPool).when(spyEvent).toAmmPool(anyBoolean());

    // When/Then
    assertDoesNotThrow(() -> spyEvent.validate());
  }

  @Test
  @DisplayName("Test validate with invalid action type")
  void testValidateInvalidActionType() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.create();
    event.setActionType(ActionType.COIN_TRANSACTION);

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      event.validate();
    });
    assertTrue(exception.getMessage().contains("Action type not matched"));
  }

  @Test
  @DisplayName("Test validate with unsupported operation type")
  void testValidateUnsupportedOperationType() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.create();
    event.setOperationType(OperationType.COIN_DEPOSIT_CREATE);

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      event.validate();
    });
    assertTrue(exception.getMessage().contains("Operation type is not supported in list"));
  }

  @Test
  @DisplayName("Test validate with valid update event")
  void testValidateValidUpdateEvent() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.forUpdate();
    event.setPair("USDT/VND");

    // Configure mock để trả về pool khi getAmmPool được gọi
    when(ammPoolCache.getAmmPool(event.getPair())).thenReturn(Optional.of(ammPool));
    when(ammPool.hasUpdateField(event.isActive(), event.getFeePercentage(), event.getFeeProtocolPercentage(),
        event.getInitPrice()))
        .thenReturn(true);

    // Then
    assertDoesNotThrow(() -> event.validate());
  }

  @Test
  @DisplayName("Test validate with update event but no fields to update")
  void testValidateUpdateEventNoUpdateFields() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.create();
    event.setOperationType(OperationType.AMM_POOL_UPDATE);

    // Configure mock để trả về pool khi getAmmPool được gọi
    when(ammPoolCache.getAmmPool(event.getPair())).thenReturn(Optional.of(ammPool));
    when(ammPool.hasUpdateField(event.isActive(), event.getFeePercentage(), event.getFeeProtocolPercentage(),
        event.getInitPrice()))
        .thenReturn(false);

    // Then
    assertThrows(IllegalArgumentException.class, () -> event.validate());
  }

  @Test
  @DisplayName("Test validate with operation type outside AMM_POOL_CREATE or AMM_POOL_UPDATE")
  void testValidateNonCreateOrUpdateOperationType() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.create();
    event.setOperationType(OperationType.COIN_DEPOSIT_CREATE);

    // Using spy to verify toAmmPool is not called
    AmmPoolEvent spyEvent = spy(event);

    // When/Then
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      spyEvent.validate();
    });

    // Verify toAmmPool methods are not called
    verify(spyEvent, never()).toAmmPool(anyBoolean());
    assertTrue(exception.getMessage().contains("Operation type is not supported"));
  }

  @Test
  @DisplayName("Test validate with AMM_POOL_UPDATE event but pool not found")
  void testValidateUpdateEventPoolNotFound() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.forUpdate();

    // Configure mocks
    when(ammPoolCache.getAmmPool(event.getPair())).thenReturn(Optional.empty());

    // When/Then
    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
      event.validate();
    });
    assertTrue(exception.getMessage().contains("not found"));
    verify(ammPoolCache).getAmmPool(event.getPair());
  }

  @Test
  @DisplayName("Test validate throws exception when there are validation errors")
  void testValidateThrowsExceptionWithErrors() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.create();

    // Cần mock cho BaseEvent.validateRequiredFields() - sử dụng spy
    AmmPoolEvent spyEvent = spy(event);
    List<String> errors = new ArrayList<>();
    errors.add("Test error message");
    doReturn(errors).when(spyEvent).validateRequiredFields();

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      spyEvent.validate();
    });
    assertTrue(exception.getMessage().contains("validate AmmPoolEvent"));
    assertTrue(exception.getMessage().contains("Test error message"));
  }

  @Test
  @DisplayName("Test toOperationObjectMessageJson when pool exists")
  void testToOperationObjectMessageJsonPoolExists() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.create();

    // Configure mocks
    when(ammPoolCache.getAmmPool(event.getPair())).thenReturn(Optional.of(ammPool));
    when(ammPool.toMessageJson()).thenReturn(mock(java.util.Map.class));

    // When
    Map<String, Object> result = event.toOperationObjectMessageJson();

    // Then
    assertNotNull(result);
    assertNotNull(result.get("object"));
    verify(ammPoolCache).getAmmPool(event.getPair());
    verify(ammPool).toMessageJson();
  }

  @Test
  @DisplayName("Test toOperationObjectMessageJson when pool does not exist")
  void testToOperationObjectMessageJsonPoolNotExists() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.create();

    // Configure mocks
    when(ammPoolCache.getAmmPool(event.getPair())).thenReturn(Optional.empty());

    // When/Then
    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
      event.toOperationObjectMessageJson();
    });
    assertTrue(exception.getMessage().contains(event.getPair()));
    assertTrue(exception.getMessage().contains("not found"));
    verify(ammPoolCache).getAmmPool(event.getPair());
  }

  @Test
  @DisplayName("Test parserData from JsonNode")
  void testParserData() {
    // Given
    // Dùng supported coin values từ Coin.java
    AmmPoolEvent originalEvent = AmmPoolEventFactory.create();
    originalEvent.setToken0("USDT");
    originalEvent.setToken1("VND");

    JsonNode jsonNode = AmmPoolEventFactory.toJsonNode(originalEvent);
    AmmPoolEvent parsedEvent = new AmmPoolEvent();

    // When
    parsedEvent.parserData(jsonNode);

    // Then
    assertEquals(originalEvent.getEventId(), parsedEvent.getEventId());
    assertEquals(originalEvent.getActionType(), parsedEvent.getActionType());
    assertEquals(originalEvent.getActionId(), parsedEvent.getActionId());
    assertEquals(originalEvent.getOperationType(), parsedEvent.getOperationType());
    assertEquals(originalEvent.getPair(), parsedEvent.getPair());
    assertEquals(originalEvent.getToken0(), parsedEvent.getToken0());
    assertEquals(originalEvent.getToken1(), parsedEvent.getToken1());
    assertEquals(originalEvent.getFeePercentage(), parsedEvent.getFeePercentage());
    assertEquals(originalEvent.getFeeProtocolPercentage(), parsedEvent.getFeeProtocolPercentage());
    assertEquals(originalEvent.isActive(), parsedEvent.isActive());
  }

  @Test
  @DisplayName("Test parserData parses all fields correctly")
  void testParserDataParsesAllFields() {
    // Given
    AmmPoolEvent event = new AmmPoolEvent();
    JsonNode messageJson = AmmPoolEventFactory.createJsonNode();

    // When
    event.parserData(messageJson);

    // Then
    assertEquals(messageJson.path("eventId").asText(), event.getEventId());
    assertEquals(ActionType.fromValue(messageJson.path("actionType").asText()), event.getActionType());
    assertEquals(messageJson.path("actionId").asText(), event.getActionId());
    assertEquals(OperationType.fromValue(messageJson.path("operationType").asText()), event.getOperationType());
    assertEquals(messageJson.path("pair").asText(), event.getPair());
    assertEquals(messageJson.path("token0").asText(), event.getToken0());
    assertEquals(messageJson.path("token1").asText(), event.getToken1());
    assertEquals(messageJson.path("feePercentage").asDouble(), event.getFeePercentage());
    assertEquals(messageJson.path("feeProtocolPercentage").asDouble(), event.getFeeProtocolPercentage());
    assertEquals(messageJson.path("isActive").asBoolean(), event.isActive());
    assertEquals(messageJson.path("tickSpacing").asInt(), event.getTickSpacing());
  }

  @Test
  @DisplayName("Test parserData parses initPrice field")
  void testParserDataParsesInitPrice() {
    // Given
    AmmPoolEvent event = new AmmPoolEvent();
    JsonNode messageJson = AmmPoolEventFactory.createJsonNodeWithInitPrice(BigDecimal.valueOf(1.5));

    // When
    event.parserData(messageJson);

    // Then
    assertEquals(BigDecimal.valueOf(1.5), event.getInitPrice());
  }

  @Test
  @DisplayName("Test toAmmPool sets initPrice when creating new pool")
  void testToAmmPoolSetsInitPriceWhenNew() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.create();
    String pair = event.getPair();
    BigDecimal initPrice = BigDecimal.valueOf(1.25);
    event.setInitPrice(initPrice);

    // Configure mocks
    when(ammPoolCache.getAmmPool(pair)).thenReturn(Optional.empty());

    // When
    AmmPool result = event.toAmmPool(false);

    // Then
    assertNotNull(result);
    assertEquals(initPrice, result.getInitPrice());
    verify(ammPoolCache).getAmmPool(pair);
  }

  /**
   * Helper method to create a JsonNode with initPrice
   */
  private JsonNode createJsonNodeWithInitPrice(BigDecimal initPrice) {
    ObjectNode jsonNode = JsonNodeFactory.instance.objectNode();
    jsonNode.put("eventId", "test-event-id");
    jsonNode.put("actionType", ActionType.AMM_POOL.getValue());
    jsonNode.put("actionId", "test-action-id");
    jsonNode.put("operationType", OperationType.AMM_POOL_CREATE.getValue());
    jsonNode.put("pair", "USDT/VND");
    jsonNode.put("token0", "USDT");
    jsonNode.put("token1", "VND");
    jsonNode.put("feePercentage", 0.003);
    jsonNode.put("feeProtocolPercentage", 0.05);
    jsonNode.put("tickSpacing", 10);
    jsonNode.put("isActive", true);
    jsonNode.put("initPrice", initPrice.doubleValue());
    return jsonNode;
  }

  @Test
  @DisplayName("Test validate initPrice in update event when pool is active")
  void testValidateInitPriceWhenPoolIsActive() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.forUpdate();
    event.setInitPrice(BigDecimal.valueOf(2.0));

    AmmPool activePool = new AmmPool(event.getPair());
    activePool.setActive(true);
    activePool.setTotalValueLockedToken0(BigDecimal.ZERO);
    activePool.setTotalValueLockedToken1(BigDecimal.ZERO);

    // Configure mocks
    when(ammPoolCache.getAmmPool(event.getPair())).thenReturn(Optional.of(activePool));

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
    assertTrue(exception.getMessage().contains("Cannot modify initPrice on active pool"));
  }

  @Test
  @DisplayName("Test validate initPrice in update event when pool has liquidity")
  void testValidateInitPriceWhenPoolHasLiquidity() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.forUpdate();
    event.setInitPrice(BigDecimal.valueOf(2.0));

    AmmPool poolWithLiquidity = new AmmPool(event.getPair());
    poolWithLiquidity.setActive(false);
    poolWithLiquidity.setTotalValueLockedToken0(BigDecimal.valueOf(100));
    poolWithLiquidity.setTotalValueLockedToken1(BigDecimal.ZERO);

    // Configure mocks
    when(ammPoolCache.getAmmPool(event.getPair())).thenReturn(Optional.of(poolWithLiquidity));

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
    assertTrue(exception.getMessage().contains("Cannot modify initPrice on pool with liquidity"));
  }

  @Test
  @DisplayName("Test validate initPrice in update event with negative price")
  void testValidateInitPriceWithNegativeValue() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.forUpdate();
    event.setInitPrice(BigDecimal.valueOf(-1.0));

    AmmPool validPool = new AmmPool(event.getPair());
    validPool.setActive(false);
    validPool.setTotalValueLockedToken0(BigDecimal.ZERO);
    validPool.setTotalValueLockedToken1(BigDecimal.ZERO);

    // Configure mocks
    when(ammPoolCache.getAmmPool(event.getPair())).thenReturn(Optional.of(validPool));

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
    assertTrue(exception.getMessage().contains("Initial price must be positive"));
  }

  @Test
  @DisplayName("Test successful update of initPrice")
  void testSuccessfulUpdateOfInitPrice() {
    // Given
    AmmPoolEvent event = AmmPoolEventFactory.forUpdate();
    event.setInitPrice(BigDecimal.valueOf(2.0));

    AmmPool validPool = new AmmPool(event.getPair());
    validPool.setActive(false);
    validPool.setTotalValueLockedToken0(BigDecimal.ZERO);
    validPool.setTotalValueLockedToken1(BigDecimal.ZERO);
    validPool.setFeePercentage(0.003);
    validPool.setFeeProtocolPercentage(0.1);

    // Configure mocks
    when(ammPoolCache.getAmmPool(event.getPair())).thenReturn(Optional.of(validPool));

    // When/Then
    assertDoesNotThrow(() -> event.validate());
  }
}
