package com.exchangeengine.model.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.exchangeengine.factory.event.AmmPositionEventFactory;
import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.AmmPosition;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.AmmPositionCache;
import com.fasterxml.jackson.databind.JsonNode;
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
public class AmmPositionEventTest {

  @Mock(lenient = true)
  private AmmPosition ammPosition;

  @Mock(lenient = true)
  private AmmPositionCache ammPositionCache;

  @BeforeEach
  void setUp() {
    // Sử dụng setTestInstance thay vì MockedStatic
    AmmPositionCache.setTestInstance(ammPositionCache);
  }

  @AfterEach
  void tearDown() {
    // Reset test instance
    AmmPositionCache.setTestInstance(null);
  }

  @Test
  @DisplayName("Test create valid AmmPositionEvent")
  void testCreateValidAmmPositionEvent() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.create();

    // Then
    assertNotNull(event);
    assertEquals(ActionType.AMM_POSITION, event.getActionType());
    assertTrue(OperationType.AMM_POSITION_OPERATIONS.contains(event.getOperationType()));
    assertNotNull(event.getIdentifier());
    assertNotNull(event.getPoolPair());
    assertNotNull(event.getOwnerAccountKey0());
    assertNotNull(event.getOwnerAccountKey1());
    assertNotNull(event.getSlippage());
    assertNotNull(event.getAmount0Initial());
    assertNotNull(event.getAmount1Initial());
  }

  @Test
  @DisplayName("Test getProducerEventId returns identifier")
  void testGetProducerEventId() {
    // Given
    String identifier = "position-123";
    AmmPositionEvent event = AmmPositionEventFactory.customize(Map.of("identifier", identifier));

    // When
    String producerEventId = event.getProducerEventId();

    // Then
    assertEquals(identifier, producerEventId);
  }

  @Test
  @DisplayName("Test getEventHandler returns AMM_POSITION_EVENT")
  void testGetEventHandler() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.create();

    // When
    String handler = event.getEventHandler();

    // Then
    assertEquals(EventHandlerAction.AMM_POSITION_EVENT, handler);
  }

  @Test
  @DisplayName("Test fetchAmmPosition when position exists")
  void testFetchAmmPositionWhenExists() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.create();
    String identifier = event.getIdentifier();

    // Configure mocks
    when(ammPositionCache.getAmmPosition(identifier)).thenReturn(Optional.of(ammPosition));

    // When
    Optional<AmmPosition> result = event.fetchAmmPosition(false);

    // Then
    assertTrue(result.isPresent());
    assertEquals(ammPosition, result.get());
    verify(ammPositionCache).getAmmPosition(identifier);
  }

  @Test
  @DisplayName("Test fetchAmmPosition when position does not exist and no exception")
  void testFetchAmmPositionWhenNotExistsNoException() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.create();
    String identifier = event.getIdentifier();

    // Configure mocks
    when(ammPositionCache.getAmmPosition(identifier)).thenReturn(Optional.empty());

    // When
    Optional<AmmPosition> result = event.fetchAmmPosition(false);

    // Then
    assertFalse(result.isPresent());
    verify(ammPositionCache).getAmmPosition(identifier);
  }

  @Test
  @DisplayName("Test fetchAmmPosition when position does not exist and raise exception")
  void testFetchAmmPositionWhenNotExistsRaiseException() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.create();
    String identifier = event.getIdentifier();

    // Configure mocks
    when(ammPositionCache.getAmmPosition(identifier)).thenReturn(Optional.empty());

    // When/Then
    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
      event.fetchAmmPosition(true);
    });
    assertTrue(exception.getMessage().contains(identifier));
    assertTrue(exception.getMessage().contains("not found"));
    verify(ammPositionCache).getAmmPosition(identifier);
  }

  @Test
  @DisplayName("Test toAmmPosition creates new position when not exists")
  void testToAmmPositionCreatesNewPosition() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.create();
    String identifier = event.getIdentifier();
    String poolPair = event.getPoolPair();
    String ownerAccountKey0 = event.getOwnerAccountKey0();
    String ownerAccountKey1 = event.getOwnerAccountKey1();
    BigDecimal slippage = event.getSlippage();
    BigDecimal amount0Initial = event.getAmount0Initial();
    BigDecimal amount1Initial = event.getAmount1Initial();

    // Configure mocks
    when(ammPositionCache.getAmmPosition(identifier)).thenReturn(Optional.empty());

    // When
    AmmPosition result = event.toAmmPosition(false);

    // Then
    assertNotNull(result);
    assertEquals(identifier, result.getIdentifier());
    assertEquals(poolPair, result.getPoolPair());
    assertEquals(ownerAccountKey0, result.getOwnerAccountKey0());
    assertEquals(ownerAccountKey1, result.getOwnerAccountKey1());
    assertEquals(slippage, result.getSlippage());
    assertEquals(amount0Initial, result.getAmount0Initial());
    assertEquals(amount1Initial, result.getAmount1Initial());
    verify(ammPositionCache).getAmmPosition(identifier);
  }

  @Test
  @DisplayName("Test toAmmPosition returns existing position")
  void testToAmmPositionReturnsExistingPosition() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.create();
    String identifier = event.getIdentifier();

    // Configure mocks
    when(ammPositionCache.getAmmPosition(identifier)).thenReturn(Optional.of(ammPosition));

    // When
    AmmPosition result = event.toAmmPosition(true);

    // Then
    assertNotNull(result);
    assertEquals(ammPosition, result);
    verify(ammPositionCache).getAmmPosition(identifier);
  }

  @Test
  @DisplayName("Test validate with valid AMM_POSITION_CREATE event")
  void testValidateValidCreateEvent() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.create();

    // Use spy without unnecessary mocks
    AmmPositionEvent spyEvent = spy(event);

    // Force toAmmPosition to return a valid position
    AmmPosition mockPosition = mock(AmmPosition.class);
    lenient().when(mockPosition.validateRequiredFields()).thenReturn(new ArrayList<>());
    lenient().when(mockPosition.validateResourcesExist()).thenReturn(new ArrayList<>());
    doReturn(mockPosition).when(spyEvent).toAmmPosition(anyBoolean());

    // When/Then
    assertDoesNotThrow(() -> spyEvent.validate());

    // Verify both validation methods are called
    verify(mockPosition).validateRequiredFields();
    verify(mockPosition).validateResourcesExist();
  }

  @Test
  @DisplayName("Test validate with invalid action type")
  void testValidateInvalidActionType() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.create();
    event.setActionType(ActionType.COIN_TRANSACTION);

    // Tạo spy để mock các phương thức khác và tránh RocksDB exception
    AmmPositionEvent spyEvent = spy(event);

    // Mock toAmmPosition để không gọi đến RocksDB
    AmmPosition mockPosition = mock(AmmPosition.class);
    doReturn(mockPosition).when(spyEvent).toAmmPosition(anyBoolean());

    // Thiết lập các mock cần thiết
    lenient().when(mockPosition.validateRequiredFields()).thenReturn(new ArrayList<>());
    lenient().when(mockPosition.validateResourcesExist()).thenReturn(new ArrayList<>());

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      spyEvent.validate();
    });
    assertTrue(exception.getMessage().contains("Action type not matched"));
    assertTrue(exception.getMessage().contains("expected AmmPosition"));
  }

  @Test
  @DisplayName("Test validate with missing identifier")
  void testValidateWithMissingIdentifier() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.create();
    event.setIdentifier("");

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      event.validate();
    });
    assertTrue(exception.getMessage().contains("Position identifier is required"));
  }

  @Test
  @DisplayName("Test validate with null identifier")
  void testValidateWithNullIdentifier() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.create();
    event.setIdentifier(null);

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      event.validate();
    });
    assertTrue(exception.getMessage().contains("Position identifier is required"));
  }

  @Test
  @DisplayName("Test validate with unsupported operation type")
  void testValidateUnsupportedOperationType() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.create();
    event.setOperationType(OperationType.COIN_DEPOSIT_CREATE);

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      event.validate();
    });
    assertTrue(exception.getMessage().contains("Operation type is not supported in list"));
  }

  @Test
  @DisplayName("Test validate with operation not in AMM_POSITION_OPERATIONS")
  void testValidateOperationNotInPositionOperations() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.create();
    // Thiết lập một operation type không thuộc AMM_POSITION_OPERATIONS
    event.setOperationType(OperationType.AMM_POOL_CREATE);

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      event.validate();
    });
    assertTrue(exception.getMessage().contains("Operation type is not supported in list"));
    assertTrue(exception.getMessage().contains(OperationType.getSupportedAmmPositionValues()));
  }

  @Test
  @DisplayName("Test validate with valid collect fee event")
  void testValidateValidCollectFeeEvent() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.customize(Map.of(
        "operationType", OperationType.AMM_POSITION_COLLECT_FEE));

    // Configure mocks
    when(ammPositionCache.getAmmPosition(event.getIdentifier())).thenReturn(Optional.of(ammPosition));

    // When/Then
    assertDoesNotThrow(() -> event.validate());
    verify(ammPositionCache).getAmmPosition(event.getIdentifier());
  }

  @Test
  @DisplayName("Test validate with valid close position event")
  void testValidateValidCloseEvent() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.customize(Map.of(
        "operationType", OperationType.AMM_POSITION_CLOSE));

    // Configure mocks
    when(ammPositionCache.getAmmPosition(event.getIdentifier())).thenReturn(Optional.of(ammPosition));

    // When/Then
    assertDoesNotThrow(() -> event.validate());
    verify(ammPositionCache).getAmmPosition(event.getIdentifier());
  }

  @Test
  @DisplayName("Test toOperationObjectMessageJson when position exists")
  void testToOperationObjectMessageJsonPositionExists() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.create();

    // Configure mocks
    when(ammPositionCache.getAmmPosition(event.getIdentifier())).thenReturn(Optional.of(ammPosition));
    when(ammPosition.toMessageJson()).thenReturn(Map.of("key", "value"));

    // When
    Map<String, Object> result = event.toOperationObjectMessageJson();

    // Then
    assertNotNull(result);
    assertEquals(event.getEventId(), result.get("eventId"));
    assertEquals(Map.of("key", "value"), result.get("object"));
    verify(ammPositionCache).getAmmPosition(event.getIdentifier());
    verify(ammPosition).toMessageJson();
  }

  @Test
  @DisplayName("Test parserData from JsonNode")
  void testParserData() {
    // Given
    JsonNode jsonNode = AmmPositionEventFactory.createJsonNode();

    // When
    AmmPositionEvent result = new AmmPositionEvent().parserData(jsonNode);

    // Then
    assertNotNull(result);
    assertEquals(jsonNode.path("eventId").asText(), result.getEventId());
    assertEquals(ActionType.fromValue(jsonNode.path("actionType").asText()), result.getActionType());
    assertEquals(jsonNode.path("actionId").asText(), result.getActionId());
    assertEquals(OperationType.fromValue(jsonNode.path("operationType").asText()), result.getOperationType());
    assertEquals(jsonNode.path("identifier").asText(), result.getIdentifier());
    assertEquals(jsonNode.path("poolPair").asText(), result.getPoolPair());
    assertEquals(jsonNode.path("ownerAccountKey0").asText(), result.getOwnerAccountKey0());
    assertEquals(jsonNode.path("ownerAccountKey1").asText(), result.getOwnerAccountKey1());
    assertEquals(BigDecimal.valueOf(jsonNode.path("slippage").asDouble()), result.getSlippage());
    assertEquals(new BigDecimal(jsonNode.path("amount0Initial").asText("0")), result.getAmount0Initial());
    assertEquals(new BigDecimal(jsonNode.path("amount1Initial").asText("0")), result.getAmount1Initial());
    assertEquals(jsonNode.path("tickLowerIndex").asInt(), result.getTickLowerIndex());
    assertEquals(jsonNode.path("tickUpperIndex").asInt(), result.getTickUpperIndex());
  }

  @Test
  @DisplayName("Test parserData parses all fields correctly for collect fee")
  void testParserDataParsesAllFieldsForCollectFee() {
    // Bỏ test này vì AmmPositionEvent không còn các trường fee và token
  }

  @Test
  @DisplayName("Test parserData parses all fields correctly for close position")
  void testParserDataParsesAllFieldsForClose() {
    // Bỏ test này vì AmmPositionEvent không còn các trường fee và token
  }

  @Test
  @DisplayName("Test validate position fetch when validate collect fee")
  void testValidatePositionFetch() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.customize(Map.of(
        "operationType", OperationType.AMM_POSITION_COLLECT_FEE));

    // Configure mocks for position exists
    when(ammPositionCache.getAmmPosition(event.getIdentifier())).thenReturn(Optional.of(ammPosition));

    // When/Then
    assertDoesNotThrow(() -> {
      event.validate();
    });
    // Kiểm tra rằng fetchAmmPosition đã được gọi
    verify(ammPositionCache).getAmmPosition(event.getIdentifier());
  }

  @Test
  @DisplayName("Test validate with invalid model fields")
  void testValidateInvalidModelFields() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.create();

    // Tạo spy và chuẩn bị mock cho toAmmPosition
    AmmPositionEvent spyEvent = spy(event);
    AmmPosition mockPosition = mock(AmmPosition.class);

    // Position sẽ trả về danh sách lỗi từ validateRequiredFields
    List<String> validationErrors = new ArrayList<>();
    validationErrors.add("Invalid field error");
    when(mockPosition.validateRequiredFields()).thenReturn(validationErrors);
    when(mockPosition.validateResourcesExist()).thenReturn(new ArrayList<>()); // No resource errors
    doReturn(mockPosition).when(spyEvent).toAmmPosition(false);

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      spyEvent.validate();
    });
    assertTrue(exception.getMessage().contains("validate AmmPositionEvent"));
    assertTrue(exception.getMessage().contains("Invalid field error"));
  }

  @Test
  @DisplayName("Test validate with invalid resources")
  void testValidateInvalidResources() {
    // Given
    AmmPositionEvent event = AmmPositionEventFactory.create();

    // Tạo spy và chuẩn bị mock cho toAmmPosition
    AmmPositionEvent spyEvent = spy(event);
    AmmPosition mockPosition = mock(AmmPosition.class);

    // Position sẽ trả về danh sách lỗi từ validateResourcesExist
    List<String> resourceErrors = new ArrayList<>();
    resourceErrors.add("Resource not found error");
    when(mockPosition.validateRequiredFields()).thenReturn(new ArrayList<>()); // No field errors
    when(mockPosition.validateResourcesExist()).thenReturn(resourceErrors);
    doReturn(mockPosition).when(spyEvent).toAmmPosition(false);

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      spyEvent.validate();
    });
    assertTrue(exception.getMessage().contains("validate AmmPositionEvent"));
    assertTrue(exception.getMessage().contains("Resource not found error"));
  }

  @Test
  @DisplayName("Test validate calls fetchAmmPosition for non-CREATE operations")
  void testValidateCallsFetchAmmPositionForNonCreateOperations() {
    // Create a test subclass to track fetchAmmPosition calls
    class TestAmmPositionEvent extends AmmPositionEvent {
      @Override
      public Optional<AmmPosition> fetchAmmPosition(boolean raiseException) {
        if (raiseException) {
          return Optional.of(ammPosition);
        }
        return Optional.empty();
      }
    }

    // Given
    TestAmmPositionEvent testEvent = spy(new TestAmmPositionEvent());
    testEvent.setEventId("test-event-id");
    testEvent.setActionId("test-action-id");
    testEvent.setActionType(ActionType.AMM_POSITION);
    testEvent.setOperationType(OperationType.AMM_POSITION_CLOSE);
    testEvent.setIdentifier("test-position-id");

    // When
    testEvent.validate();

    // Then
    verify(testEvent).fetchAmmPosition(true);
  }
}
