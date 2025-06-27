package com.exchangeengine.model.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.util.Arrays;

import com.exchangeengine.factory.event.DisruptorEventFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exchangeengine.extension.CombinedTestExtension;
import com.exchangeengine.factory.event.AccountEventFactory;
import com.exchangeengine.factory.event.AmmOrderEventFactory;
import com.exchangeengine.factory.event.AmmPoolEventFactory;
import com.exchangeengine.factory.event.AmmPositionEventFactory;
import com.exchangeengine.factory.event.CoinDepositEventFactory;
import com.exchangeengine.factory.event.CoinWithdrawalEventFactory;
import com.exchangeengine.factory.event.MerchantEscrowEventFactory;
import com.exchangeengine.factory.event.BalancesLockEventFactory;

@ExtendWith({ MockitoExtension.class, CombinedTestExtension.class })
class DisruptorEventTest {

  @Mock
  private AccountEvent mockAccountEvent;

  @Mock
  private CoinDepositEvent mockCoinDepositEvent;

  @Mock
  private CoinWithdrawalEvent mockCoinWithdrawalEvent;

  @Mock
  private AmmPoolEvent mockAmmPoolEvent;

  @Mock
  private MerchantEscrowEvent mockMerchantEscrowEvent;

  @Mock
  private AmmPositionEvent mockAmmPositionEvent;

  @Mock
  private AmmOrderEvent mockAmmOrderEvent;

  @Mock
  private TradeEvent mockTradeEvent;

  @Mock
  private OfferEvent mockOfferEvent;

  @Mock
  private BalancesLockEvent mockBalancesLockEvent;

  @Test
  @DisplayName("Tạo DisruptorEvent với constructor mặc định")
  void testCreateDisruptorEvent() {
    // When
    DisruptorEvent event = new DisruptorEvent();

    // Then
    assertTrue(event.isSuccess());
    assertNull(event.getErrorMessage());
    assertNotNull(event.getTimestamp());
    assertNull(event.getAccountEvent());
    assertNull(event.getCoinDepositEvent());
    assertNull(event.getCoinWithdrawalEvent());
    assertNull(event.getAmmPoolEvent());
    assertNull(event.getMerchantEscrowEvent());
    assertNull(event.getAmmPositionEvent());
    assertNull(event.getTradeEvent());
    assertNull(event.getOfferEvent());
  }

  @Test
  @DisplayName("Tạo DisruptorEvent từ factory")
  void testCreateDisruptorEventFromFactory() {
    // When
    DisruptorEvent event = DisruptorEventFactory.create();

    // Then
    assertTrue(event.isSuccess());
    assertNull(event.getErrorMessage());
    assertNotNull(event.getTimestamp());
  }

  @Test
  @DisplayName("Gọi setErrorMessage sẽ đặt isSuccess thành false")
  void testSetErrorMessage() {
    // Given
    DisruptorEvent event = DisruptorEventFactory.create();
    assertTrue(event.isSuccess());

    // When
    String errorMessage = "Error happened";
    event.setErrorMessage(errorMessage);

    // Then
    assertFalse(event.isSuccess());
    assertEquals(errorMessage, event.getErrorMessage());
  }

  @Test
  @DisplayName("getEvent trả về AccountEvent khi có AccountEvent")
  void testGetEventWithAccountEvent() {
    // Given
    DisruptorEvent event = DisruptorEventFactory.withAccountEvent();

    // When
    BaseEvent result = event.getEvent();

    // Then
    assertNotNull(result);
    assertTrue(result instanceof AccountEvent);
    assertEquals(event.getAccountEvent(), result);
  }

  @Test
  @DisplayName("getEvent trả về CoinDepositEvent khi có CoinDepositEvent")
  void testGetEventWithCoinDepositEvent() {
    // Given
    DisruptorEvent event = DisruptorEventFactory.withCoinDepositEvent();

    // When
    BaseEvent result = event.getEvent();

    // Then
    assertNotNull(result);
    assertTrue(result instanceof CoinDepositEvent);
    assertEquals(event.getCoinDepositEvent(), result);
  }

  @Test
  @DisplayName("getEvent trả về CoinWithdrawalEvent khi có CoinWithdrawalEvent")
  void testGetEventWithCoinWithdrawalEvent() {
    // Given
    DisruptorEvent event = DisruptorEventFactory.withCoinWithdrawalEvent();

    // When
    BaseEvent result = event.getEvent();

    // Then
    assertNotNull(result);
    assertTrue(result instanceof CoinWithdrawalEvent);
    assertEquals(event.getCoinWithdrawalEvent(), result);
  }

  @Test
  @DisplayName("getEvent trả về AmmPoolEvent khi có AmmPoolEvent")
  void testGetEventWithAmmPoolEvent() {
    // Given
    DisruptorEvent event = DisruptorEventFactory.withAmmPoolEvent();

    // When
    BaseEvent result = event.getEvent();

    // Then
    assertNotNull(result);
    assertTrue(result instanceof AmmPoolEvent);
    assertEquals(event.getAmmPoolEvent(), result);
  }

  @Test
  @DisplayName("getEvent trả về MerchantEscrowEvent khi có MerchantEscrowEvent")
  void testGetEventWithMerchantEscrowEvent() {
    // Given
    DisruptorEvent event = DisruptorEventFactory.withMerchantEscrowEvent();

    // When
    BaseEvent result = event.getEvent();

    // Then
    assertNotNull(result);
    assertTrue(result instanceof MerchantEscrowEvent);
    assertEquals(event.getMerchantEscrowEvent(), result);
  }

  @Test
  @DisplayName("getEvent trả về AmmPositionEvent khi có AmmPositionEvent")
  void testGetEventWithAmmPositionEvent() {
    // Given
    DisruptorEvent event = DisruptorEventFactory.withAmmPositionEvent();

    // When
    BaseEvent result = event.getEvent();

    // Then
    assertNotNull(result);
    assertTrue(result instanceof AmmPositionEvent);
    assertEquals(event.getAmmPositionEvent(), result);
  }

  @Test
  @DisplayName("getEvent trả về TradeEvent khi có TradeEvent")
  void testGetEventWithTradeEvent() {
    // Given
    DisruptorEvent event = DisruptorEventFactory.withTradeEvent();

    // When
    BaseEvent result = event.getEvent();

    // Then
    assertNotNull(result);
    assertTrue(result instanceof TradeEvent);
    assertEquals(event.getTradeEvent(), result);
  }

  @Test
  @DisplayName("getEvent trả về OfferEvent khi có OfferEvent")
  void testGetEventWithOfferEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    event.setOfferEvent(mockOfferEvent);

    // When
    BaseEvent result = event.getEvent();

    // Then
    assertNotNull(result);
    assertSame(mockOfferEvent, result);
    assertEquals(event.getOfferEvent(), result);
  }

  @Test
  @DisplayName("getEvent trả về BalancesLockEvent khi có BalancesLockEvent")
  void testGetEventWithBalancesLockEvent() {
    // Given
    DisruptorEvent event = DisruptorEventFactory.withBalancesLockEvent();

    // When
    BaseEvent result = event.getEvent();

    // Then
    assertNotNull(result);
    assertTrue(result instanceof BalancesLockEvent);
    assertEquals(event.getBalancesLockEvent(), result);
  }

  @Test
  @DisplayName("getEvent trả về BaseEvent trống khi không có event nào")
  void testGetEventWithNoEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    // Đảm bảo tất cả các trường event đều null
    event.setAccountEvent(null);
    event.setCoinDepositEvent(null);
    event.setCoinWithdrawalEvent(null);
    event.setAmmPoolEvent(null);
    event.setMerchantEscrowEvent(null);
    event.setAmmPositionEvent(null);
    event.setTradeEvent(null);
    event.setOfferEvent(null);

    // When
    BaseEvent result = event.getEvent();

    // Then
    assertNotNull(result);
    assertTrue(result instanceof BaseEvent);
    // Kiểm tra đây là một BaseEvent mới được tạo (không phải các loại event khác)
    assertFalse(result instanceof AccountEvent);
    assertFalse(result instanceof CoinDepositEvent);
    assertFalse(result instanceof CoinWithdrawalEvent);
    assertFalse(result instanceof AmmPoolEvent);
    assertFalse(result instanceof MerchantEscrowEvent);
    assertFalse(result instanceof AmmPositionEvent);
    assertFalse(result instanceof TradeEvent);
    assertFalse(result instanceof OfferEvent);
  }

  @Test
  @DisplayName("getEventId trả về ID của event")
  void testGetEventId() {
    // Given
    String eventId = UUID.randomUUID().toString();
    when(mockAccountEvent.getEventId()).thenReturn(eventId);

    DisruptorEvent event = new DisruptorEvent();
    event.setAccountEvent(mockAccountEvent);

    // When
    String result = event.getEventId();

    // Then
    assertEquals(eventId, result);
    verify(mockAccountEvent).getEventId();
  }

  @Test
  @DisplayName("getEventId trả về eventId của BaseEvent trống khi không có event cụ thể")
  void testGetEventIdWithNoEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    event.setAccountEvent(null);
    event.setCoinDepositEvent(null);
    event.setCoinWithdrawalEvent(null);
    event.setAmmPoolEvent(null);
    event.setMerchantEscrowEvent(null);
    event.setAmmPositionEvent(null);
    event.setTradeEvent(null);
    event.setOfferEvent(null);

    // When
    String result = event.getEventId();

    // Then
    assertNull(result, "Phải trả về null khi không có event cụ thể nào");
  }

  @Test
  @DisplayName("getProducerKey trả về producerEventId của event")
  void testGetProducerKey() {
    // Given
    String producerEventId = "producer-event-id";
    when(mockAccountEvent.getProducerEventId()).thenReturn(producerEventId);

    DisruptorEvent event = new DisruptorEvent();
    event.setAccountEvent(mockAccountEvent);

    // When
    String result = event.getProducerKey();

    // Then
    assertEquals(producerEventId, result);
    verify(mockAccountEvent).getProducerEventId();
  }

  @Test
  @DisplayName("getProducerKey trả về producerEventId khi không có event cụ thể")
  void testGetProducerKeyWithNoEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    event.setAccountEvent(null);
    event.setCoinDepositEvent(null);
    event.setCoinWithdrawalEvent(null);
    event.setAmmPoolEvent(null);
    event.setMerchantEscrowEvent(null);
    event.setAmmPositionEvent(null);
    event.setTradeEvent(null);
    event.setOfferEvent(null);

    // When
    String result = event.getProducerKey();

    // Then
    assertNotNull(result);
    assertTrue(result.length() > 5, "Phải trả về một chuỗi có độ dài hợp lý");
  }

  @Test
  @DisplayName("getProducerKey xử lý khi event là null")
  void testGetProducerKeyWithNullEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    DisruptorEvent spyEvent = spy(event);
    doReturn(null).when(spyEvent).getEvent();

    // When
    String result = spyEvent.getProducerKey();

    // Then
    assertNotNull(result);
    // Kiểm tra UUID format
    assertTrue(result.length() > 30);
  }

  @Test
  @DisplayName("toOperationObjectMessageJson trả về MessageJson đúng format")
  void testToOperationObjectMessageJson() {
    // Given
    Map<String, Object> objectMap = new HashMap<>();
    objectMap.put("object", "test-object");

    Map<String, Object> messageJson = new HashMap<>();
    messageJson.put("object", "test-object");

    when(mockAccountEvent.toOperationObjectMessageJson()).thenReturn(objectMap);
    String eventId = "event-123";
    when(mockAccountEvent.getEventId()).thenReturn(eventId);

    DisruptorEvent event = new DisruptorEvent();
    event.setAccountEvent(mockAccountEvent);
    event.setErrorMessage("test-error");
    event.setSuccess(false);
    long timestamp = event.getTimestamp();

    // When
    Map<String, Object> result = event.toOperationObjectMessageJson();

    // Then
    assertNotNull(result);
    assertEquals(false, result.get("isSuccess"));
    assertEquals("test-error", result.get("errorMessage"));
    assertEquals(timestamp, result.get("timestamp"));
    assertEquals(eventId, result.get("inputEventId"));
    assertEquals("test-object", result.get("object"));

    verify(mockAccountEvent).toOperationObjectMessageJson();
    verify(mockAccountEvent).getEventId();
  }

  @Test
  @DisplayName("toOperationObjectMessageJson trả về MessageJson đúng format với AmmPositionEvent")
  void testToOperationObjectMessageJsonWithAmmPositionEvent() {
    // Given
    Map<String, Object> objectMap = new HashMap<>();
    objectMap.put("object", "test-amm-position");

    DisruptorEvent event = new DisruptorEvent();
    event.setAmmPositionEvent(mockAmmPositionEvent);

    // Mock timestamp để test có tính ổn định
    event.setTimestamp(1000L);

    when(mockAmmPositionEvent.toOperationObjectMessageJson()).thenReturn(objectMap);
    when(mockAmmPositionEvent.getEventId()).thenReturn(null);

    // When
    Map<String, Object> result = event.toOperationObjectMessageJson();

    // Then
    assertNotNull(result);
    assertEquals(true, result.get("isSuccess"));
    assertNull(result.get("errorMessage"));
    assertEquals(1000L, result.get("timestamp"));
    assertNull(result.get("inputEventId"));
    assertEquals("test-amm-position", result.get("object"));
    verify(mockAmmPositionEvent).toOperationObjectMessageJson();
  }

  @Test
  @DisplayName("toOperationObjectMessageJson trả về MessageJson đúng format khi không có event cụ thể")
  void testToOperationObjectMessageJsonNoEvent() {
    // Given
    // Tạo DisruptorEvent rỗng để đảm bảo không có event thực
    DisruptorEvent event = new DisruptorEvent();
    DisruptorEvent spyEvent = spy(event);

    // Mock để trả về BaseEvent đơn giản thay vì event thực
    BaseEvent mockBaseEvent = mock(BaseEvent.class);
    Map<String, Object> mockMap = new HashMap<>();
    when(mockBaseEvent.toOperationObjectMessageJson()).thenReturn(mockMap);
    when(mockBaseEvent.getEventId()).thenReturn("mock-id");
    doReturn(mockBaseEvent).when(spyEvent).getEvent();

    spyEvent.setErrorMessage("no-event-error");
    spyEvent.setSuccess(false);
    long timestamp = spyEvent.getTimestamp();

    // When
    Map<String, Object> result = spyEvent.toOperationObjectMessageJson();

    // Then
    assertNotNull(result);
    assertEquals(false, result.get("isSuccess"));
    assertEquals("no-event-error", result.get("errorMessage"));
    assertEquals(timestamp, result.get("timestamp"));
    assertEquals("mock-id", result.get("inputEventId"));
    assertNull(result.get("object"));
  }

  @Test
  @DisplayName("toOperationObjectMessageJson xử lý khi event là null")
  void testToOperationObjectMessageJsonWithNullEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    DisruptorEvent spyEvent = spy(event);
    doReturn(null).when(spyEvent).getEvent();

    spyEvent.setErrorMessage("test-error");
    spyEvent.setSuccess(false);
    long timestamp = spyEvent.getTimestamp();

    // When
    Map<String, Object> result = spyEvent.toOperationObjectMessageJson();

    // Then
    assertNotNull(result);
    assertEquals(false, result.get("isSuccess"));
    assertEquals("test-error", result.get("errorMessage"));
    assertEquals(timestamp, result.get("timestamp"));
    assertNull(result.get("inputEventId"));
    assertNull(result.get("object"));
  }

  @Test
  @DisplayName("validate throws exception khi không có event")
  void testValidateWithNoEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    event.setAccountEvent(null);
    event.setCoinDepositEvent(null);
    event.setCoinWithdrawalEvent(null);
    event.setAmmPoolEvent(null);
    event.setMerchantEscrowEvent(null);
    event.setAmmPositionEvent(null);
    event.setTradeEvent(null);
    event.setOfferEvent(null);

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      event.validate();
    });
    assertTrue(exception.getMessage().contains("No event specified"));
  }

  @Test
  @DisplayName("validate gọi validate của AccountEvent")
  void testValidateWithAccountEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    event.setAccountEvent(mockAccountEvent);

    // When
    event.validate();

    // Then
    verify(mockAccountEvent).validate();
  }

  @Test
  @DisplayName("validate gọi validate của CoinDepositEvent")
  void testValidateWithCoinDepositEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    event.setCoinDepositEvent(mockCoinDepositEvent);

    // When
    event.validate();

    // Then
    verify(mockCoinDepositEvent).validate();
  }

  @Test
  @DisplayName("validate gọi validate của CoinWithdrawalEvent")
  void testValidateWithCoinWithdrawalEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    event.setCoinWithdrawalEvent(mockCoinWithdrawalEvent);

    // When
    event.validate();

    // Then
    verify(mockCoinWithdrawalEvent).validate();
  }

  @Test
  @DisplayName("validate gọi validate của AmmPoolEvent")
  void testValidateWithAmmPoolEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    event.setAmmPoolEvent(mockAmmPoolEvent);

    // When
    event.validate();

    // Then
    verify(mockAmmPoolEvent).validate();
  }

  @Test
  @DisplayName("validate gọi validate của MerchantEscrowEvent")
  void testValidateWithMerchantEscrowEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    event.setMerchantEscrowEvent(mockMerchantEscrowEvent);

    // When
    event.validate();

    // Then
    verify(mockMerchantEscrowEvent).validate();
  }

  @Test
  @DisplayName("validate gọi validate của AmmPositionEvent")
  void testValidateWithAmmPositionEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    event.setAmmPositionEvent(mockAmmPositionEvent);

    // When
    event.validate();

    // Then
    verify(mockAmmPositionEvent).validate();
  }

  @Test
  @DisplayName("validate gọi validate của TradeEvent")
  void testValidateWithTradeEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    event.setTradeEvent(mockTradeEvent);

    // When
    event.validate();

    // Then
    verify(mockTradeEvent).validate();
  }

  @Test
  @DisplayName("validate gọi validate của OfferEvent")
  void testValidateWithOfferEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    event.setOfferEvent(mockOfferEvent);

    // When
    event.validate();

    // Then
    verify(mockOfferEvent).validate();
  }

  @Test
  @DisplayName("validate throws exception khi event không thuộc loại đã biết")
  void testValidateWithUnknownEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();

    // Tạo một implementation đặc biệt của BaseEvent không phải là các event đã biết
    // và không phải là BaseEvent mặc định
    BaseEvent customEvent = new BaseEvent() {
      @Override
      public String getEventId() {
        return "custom-event-id";
      }
    };

    // Do DisruptorEvent.validate đã có điều kiện isDefaultBaseEvent,
    // ta cần sử dụng khác những class đã biết và khác BaseEvent mặc định
    DisruptorEvent spyEvent = spy(event);
    doReturn(customEvent).when(spyEvent).getEvent();

    // Đảm bảo rằng customEvent không được coi là một trong những event đã biết
    assertFalse(customEvent instanceof AccountEvent);
    assertFalse(customEvent instanceof CoinDepositEvent);
    assertFalse(customEvent instanceof CoinWithdrawalEvent);
    assertFalse(customEvent instanceof AmmPoolEvent);

    // Khi
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      spyEvent.validate();
    });

    // Thì
    assertEquals("Unknown event type: " + customEvent.getClass().getSimpleName(), exception.getMessage());
  }

  @Test
  @DisplayName("copyFrom sao chép tất cả dữ liệu từ DisruptorEvent nguồn")
  void testCopyFromAll() {
    // Given
    DisruptorEvent source = new DisruptorEvent();
    source.setAccountEvent(mockAccountEvent);
    source.setCoinDepositEvent(mockCoinDepositEvent);
    source.setCoinWithdrawalEvent(mockCoinWithdrawalEvent);
    source.setAmmPoolEvent(mockAmmPoolEvent);
    source.setMerchantEscrowEvent(mockMerchantEscrowEvent);
    source.setAmmPositionEvent(mockAmmPositionEvent);
    source.setTradeEvent(mockTradeEvent);
    source.setOfferEvent(mockOfferEvent);
    source.setErrorMessage("Test error");
    source.setSuccess(false);
    long timestamp = Instant.now().toEpochMilli();
    source.setTimestamp(timestamp);

    DisruptorEvent target = new DisruptorEvent();

    // When
    target.copyFrom(source);

    // Then
    assertEquals(mockAccountEvent, target.getAccountEvent());
    assertEquals(mockCoinDepositEvent, target.getCoinDepositEvent());
    assertEquals(mockCoinWithdrawalEvent, target.getCoinWithdrawalEvent());
    assertEquals(mockAmmPoolEvent, target.getAmmPoolEvent());
    assertEquals(mockMerchantEscrowEvent, target.getMerchantEscrowEvent());
    assertEquals(mockAmmPositionEvent, target.getAmmPositionEvent());
    assertEquals(mockTradeEvent, target.getTradeEvent());
    assertEquals(mockOfferEvent, target.getOfferEvent());

    assertEquals(timestamp, target.getTimestamp());
  }

  @Test
  @DisplayName("copyFrom sao chép dữ liệu từ DisruptorEvent nguồn")
  void testCopyFrom() {
    // Given
    DisruptorEvent source = new DisruptorEvent();
    source.setAccountEvent(mockAccountEvent);
    source.setCoinDepositEvent(mockCoinDepositEvent);
    source.setCoinWithdrawalEvent(mockCoinWithdrawalEvent);
    source.setAmmPoolEvent(mockAmmPoolEvent);

    DisruptorEvent target = new DisruptorEvent();

    // When
    target.copyFrom(source);

    // Then
    assertEquals(mockAccountEvent, target.getAccountEvent());
    assertEquals(mockCoinDepositEvent, target.getCoinDepositEvent());
    assertEquals(mockCoinWithdrawalEvent, target.getCoinWithdrawalEvent());
    assertEquals(mockAmmPoolEvent, target.getAmmPoolEvent());
  }

  @Test
  @DisplayName("toOperationObjectMessageJson trả về MessageJson đúng format với TradeEvent")
  void testToOperationObjectMessageJsonWithTradeEvent() {
    // Given
    Map<String, Object> objectMap = new HashMap<>();
    objectMap.put("object", "test-trade");

    DisruptorEvent event = new DisruptorEvent();
    event.setTradeEvent(mockTradeEvent);

    when(mockTradeEvent.toOperationObjectMessageJson()).thenReturn(objectMap);
    when(mockTradeEvent.getEventId()).thenReturn("trade-123");

    // When
    Map<String, Object> result = event.toOperationObjectMessageJson();

    // Then
    assertNotNull(result);
    assertEquals("test-trade", result.get("object"));
    assertEquals("trade-123", result.get("inputEventId"));
    verify(mockTradeEvent).toOperationObjectMessageJson();
    verify(mockTradeEvent).getEventId();
  }

  @Test
  @DisplayName("toOperationObjectMessageJson trả về MessageJson đúng format với OfferEvent")
  void testToOperationObjectMessageJsonWithOfferEvent() {
    // Given
    Map<String, Object> objectMap = new HashMap<>();
    objectMap.put("object", "test-offer");

    DisruptorEvent event = new DisruptorEvent();
    event.setOfferEvent(mockOfferEvent);

    when(mockOfferEvent.toOperationObjectMessageJson()).thenReturn(objectMap);
    when(mockOfferEvent.getEventId()).thenReturn("offer-123");

    // When
    Map<String, Object> result = event.toOperationObjectMessageJson();

    // Then
    assertNotNull(result);
    assertEquals("test-offer", result.get("object"));
    assertEquals("offer-123", result.get("inputEventId"));
    verify(mockOfferEvent).toOperationObjectMessageJson();
    verify(mockOfferEvent).getEventId();
  }

  @Test
  @DisplayName("copyFrom trả về instance hiện tại khi nguồn là null")
  void testCopyFromNull() {
    // Given
    DisruptorEvent target = new DisruptorEvent();

    // When
    DisruptorEvent result = target.copyFrom(null);

    // Then
    assertEquals(target, result);
  }

  @Test
  @DisplayName("validate throws exception khi event là null")
  void testValidateWithEventNull() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    DisruptorEvent spyEvent = spy(event);
    doReturn(null).when(spyEvent).getEvent();

    // When/Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      spyEvent.validate();
    });
    assertTrue(exception.getMessage().contains("No event specified"));
  }

  @Test
  @DisplayName("successes() đặt isSuccess thành true và errorMessage thành null")
  void testSuccesses() {
    // Given
    DisruptorEvent event = DisruptorEventFactory.create();
    event.setErrorMessage("Some error");
    assertFalse(event.isSuccess());
    assertNotNull(event.getErrorMessage());

    // When
    event.successes();

    // Then
    assertTrue(event.isSuccess());
    assertNull(event.getErrorMessage());
  }

  @Test
  @DisplayName("toDisruptorMessageJson trả về MessageJson đúng format")
  void testToDisruptorMessageJson() {
    // Given
    Map<String, Object> baseMessageJson = new HashMap<>();
    baseMessageJson.put("operationType", "OPERATION");
    baseMessageJson.put("actionType", "ACTION");
    baseMessageJson.put("actionId", "action-123");

    String eventId = "event-123";
    when(mockAccountEvent.getEventId()).thenReturn(eventId);
    when(mockAccountEvent.toBaseMessageJson()).thenReturn(baseMessageJson);

    DisruptorEvent event = new DisruptorEvent();
    event.setAccountEvent(mockAccountEvent);

    // When
    Map<String, Object> result = event.toDisruptorMessageJson();

    // Then
    assertNotNull(result);
    assertEquals(eventId, result.get("eventId"));
    assertTrue((Boolean) result.get("isSuccess"));
    assertNull(result.get("errorMessage"));
    assertNotNull(result.get("timestamp"));
    assertEquals("OPERATION", result.get("operationType"));
    assertEquals("ACTION", result.get("actionType"));
    assertEquals("action-123", result.get("actionId"));

    verify(mockAccountEvent).toBaseMessageJson();
    verify(mockAccountEvent).getEventId();
  }

  @Test
  @DisplayName("toDisruptorMessageJson trả về MessageJson với isSuccess=false khi có error")
  void testToDisruptorMessageJsonWithError() {
    // Given
    Map<String, Object> baseMessageJson = new HashMap<>();
    baseMessageJson.put("operationType", "OPERATION");
    baseMessageJson.put("actionType", "ACTION");
    baseMessageJson.put("actionId", "action-123");

    String eventId = "event-123";
    String errorMessage = "Some error occurred";
    when(mockAccountEvent.getEventId()).thenReturn(eventId);
    when(mockAccountEvent.toBaseMessageJson()).thenReturn(baseMessageJson);

    DisruptorEvent event = new DisruptorEvent();
    event.setAccountEvent(mockAccountEvent);
    event.setErrorMessage(errorMessage);

    // When
    Map<String, Object> result = event.toDisruptorMessageJson();

    // Then
    assertNotNull(result);
    assertEquals(eventId, result.get("eventId"));
    assertFalse((Boolean) result.get("isSuccess"));
    assertEquals(errorMessage, result.get("errorMessage"));
    assertNotNull(result.get("timestamp"));
    assertEquals("OPERATION", result.get("operationType"));
    assertEquals("ACTION", result.get("actionType"));
    assertEquals("action-123", result.get("actionId"));
  }

  @Test
  @DisplayName("toDisruptorMessageJson handles null event gracefully")
  void testToDisruptorMessageJsonWithNullEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    DisruptorEvent spyEvent = spy(event);
    BaseEvent mockBaseEvent = mock(BaseEvent.class);

    doReturn(mockBaseEvent).when(spyEvent).getEvent();
    Map<String, Object> baseMessageJson = new HashMap<>();
    when(mockBaseEvent.toBaseMessageJson()).thenReturn(baseMessageJson);

    // When
    Map<String, Object> result = spyEvent.toDisruptorMessageJson();

    // Then
    assertNotNull(result);
    assertTrue(result.containsKey("isSuccess"));
    assertTrue(result.containsKey("errorMessage"));
    assertTrue(result.containsKey("timestamp"));
    assertTrue(result.containsKey("eventId"));
  }

  @Test
  @DisplayName("toDisruptorMessageJson trả về MessageJson đúng format với TradeEvent")
  void testToDisruptorMessageJsonWithTradeEvent() {
    // Given
    Map<String, Object> baseMessageJson = new HashMap<>();
    baseMessageJson.put("operationType", "TRADE");
    baseMessageJson.put("actionType", "TRADE");
    baseMessageJson.put("actionId", "trade-123");

    String eventId = "event-123";
    when(mockTradeEvent.getEventId()).thenReturn(eventId);
    when(mockTradeEvent.toBaseMessageJson()).thenReturn(baseMessageJson);

    DisruptorEvent event = new DisruptorEvent();
    event.setTradeEvent(mockTradeEvent);

    // When
    Map<String, Object> result = event.toDisruptorMessageJson();

    // Then
    assertNotNull(result);
    assertEquals(eventId, result.get("eventId"));
    assertTrue((Boolean) result.get("isSuccess"));
    assertNull(result.get("errorMessage"));
    assertNotNull(result.get("timestamp"));
    assertEquals("TRADE", result.get("operationType"));
    assertEquals("TRADE", result.get("actionType"));
    assertEquals("trade-123", result.get("actionId"));

    verify(mockTradeEvent).toBaseMessageJson();
    verify(mockTradeEvent).getEventId();
  }

  @Test
  @DisplayName("toDisruptorMessageJson trả về MessageJson đúng format với OfferEvent")
  void testToDisruptorMessageJsonWithOfferEvent() {
    // Given
    Map<String, Object> baseMessageJson = new HashMap<>();
    baseMessageJson.put("operationType", "OFFER");
    baseMessageJson.put("actionType", "OFFER");
    baseMessageJson.put("actionId", "offer-123");

    String eventId = "event-123";
    when(mockOfferEvent.getEventId()).thenReturn(eventId);
    when(mockOfferEvent.toBaseMessageJson()).thenReturn(baseMessageJson);

    DisruptorEvent event = new DisruptorEvent();
    event.setOfferEvent(mockOfferEvent);

    // When
    Map<String, Object> result = event.toDisruptorMessageJson();

    // Then
    assertNotNull(result);
    assertEquals(eventId, result.get("eventId"));
    assertTrue((Boolean) result.get("isSuccess"));
    assertNull(result.get("errorMessage"));
    assertNotNull(result.get("timestamp"));
    assertEquals("OFFER", result.get("operationType"));
    assertEquals("OFFER", result.get("actionType"));
    assertEquals("offer-123", result.get("actionId"));

    verify(mockOfferEvent).toBaseMessageJson();
    verify(mockOfferEvent).getEventId();
  }

  @Test
  @DisplayName("toOperationObjectMessageJson handles null object entry")
  void testToOperationObjectMessageJsonWithNullObject() {
    // Given
    Map<String, Object> objectMessageJson = new HashMap<>();
    // Deliberately not setting "object" key

    BaseEvent mockBaseEvent = mock(BaseEvent.class);
    when(mockBaseEvent.toOperationObjectMessageJson()).thenReturn(objectMessageJson);
    when(mockBaseEvent.getEventId()).thenReturn("test-id");

    DisruptorEvent event = new DisruptorEvent();
    DisruptorEvent spyEvent = spy(event);
    doReturn(mockBaseEvent).when(spyEvent).getEvent();

    // When
    Map<String, Object> result = spyEvent.toOperationObjectMessageJson();

    // Then
    assertNotNull(result);
    assertEquals("test-id", result.get("inputEventId"));
    assertNull(result.get("object"));
  }

  @Test
  @DisplayName("Kiểm tra validate ném exception khi event là null")
  void testValidateWithNullEvent() {
    // Tạo DisruptorEvent với tất cả các event đều là null
    DisruptorEvent disruptorEvent = DisruptorEventFactory.create();

    // Thực thi và kiểm tra
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> disruptorEvent.validate());
    assertEquals("No event specified in DisruptorEvent", exception.getMessage());
  }

  @Test
  @DisplayName("Kiểm tra validate ném exception khi chỉ có BaseEvent mặc định")
  void testValidateWithDefaultBaseEvent() {
    // Tạo DisruptorEvent chỉ có BaseEvent
    DisruptorEvent disruptorEvent = DisruptorEventFactory.create();
    // Không cần set trực tiếp BaseEvent vì DisruptorEvent.getEvent() đã trả về
    // BaseEvent mặc định
    // khi không có event nào khác được thiết lập

    // Thực thi và kiểm tra
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> disruptorEvent.validate());
    assertEquals("No event specified in DisruptorEvent", exception.getMessage());
  }

  @Test
  @DisplayName("Kiểm tra validate với AmmOrderEvent")
  void testValidateWithAmmOrderEvent() {
    // Chuẩn bị - sử dụng mock thay vì factory
    DisruptorEvent disruptorEvent = new DisruptorEvent();
    AmmOrderEvent mockAmmOrderEvent = mock(AmmOrderEvent.class);
    disruptorEvent.setAmmOrderEvent(mockAmmOrderEvent);

    // Thiết lập mock để không throw exception
    doNothing().when(mockAmmOrderEvent).validate();

    // Thực thi
    disruptorEvent.validate();

    // Kiểm tra
    verify(mockAmmOrderEvent).validate();
  }

  @Test
  @DisplayName("Kiểm tra validate ném exception với loại event không xác định")
  void testValidateWithUnknownEventType() {
    // Giống với test validateWithUnknownEvent đã có sẵn và hoạt động đúng
    // Given
    DisruptorEvent event = new DisruptorEvent();

    // Tạo một implementation đặc biệt của BaseEvent không phải là các event đã biết
    // và không phải là BaseEvent mặc định
    BaseEvent customEvent = new BaseEvent() {
      @Override
      public String getEventId() {
        return "custom-event-id";
      }

      @Override
      public Map<String, Object> toBaseMessageJson() {
        return new HashMap<>();
      }
    };

    // Do DisruptorEvent.validate đã có điều kiện isDefaultBaseEvent,
    // ta cần sử dụng khác những class đã biết và khác BaseEvent mặc định
    DisruptorEvent spyEvent = spy(event);
    doReturn(customEvent).when(spyEvent).getEvent();

    // Đảm bảo rằng customEvent không được coi là một trong những event đã biết
    assertFalse(customEvent instanceof AccountEvent);
    assertFalse(customEvent instanceof CoinDepositEvent);
    assertFalse(customEvent instanceof CoinWithdrawalEvent);
    assertFalse(customEvent instanceof AmmPoolEvent);

    // Khi
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      spyEvent.validate();
    });

    // Thì
    assertTrue(exception.getMessage().contains("Unknown event type"));
  }

  @Test
  @DisplayName("Kiểm tra copyFrom với nguồn null")
  void testCopyFromWithNullSource() {
    // Chuẩn bị
    DisruptorEvent disruptorEvent = DisruptorEventFactory.create();

    // Thực thi
    DisruptorEvent result = disruptorEvent.copyFrom(null);

    // Kiểm tra
    assertSame(disruptorEvent, result);
  }

  @Test
  @DisplayName("Kiểm tra copyFrom với nguồn có đầy đủ các event")
  void testCopyFromWithFullSource() {
    // Chuẩn bị
    DisruptorEvent source = new DisruptorEvent();
    source.setAccountEvent(mockAccountEvent);
    source.setCoinDepositEvent(mockCoinDepositEvent);
    source.setCoinWithdrawalEvent(mockCoinWithdrawalEvent);
    source.setAmmPoolEvent(mockAmmPoolEvent);
    source.setMerchantEscrowEvent(mockMerchantEscrowEvent);
    source.setAmmPositionEvent(mockAmmPositionEvent);
    source.setAmmOrderEvent(mockAmmOrderEvent);

    DisruptorEvent target = new DisruptorEvent();

    // Thực thi
    DisruptorEvent result = target.copyFrom(source);

    // Kiểm tra
    assertSame(target, result);
    assertSame(mockAccountEvent, result.getAccountEvent());
    assertSame(mockCoinDepositEvent, result.getCoinDepositEvent());
    assertSame(mockCoinWithdrawalEvent, result.getCoinWithdrawalEvent());
    assertSame(mockAmmPoolEvent, result.getAmmPoolEvent());
    assertSame(mockMerchantEscrowEvent, result.getMerchantEscrowEvent());
    assertSame(mockAmmPositionEvent, result.getAmmPositionEvent());
    assertSame(mockAmmOrderEvent, result.getAmmOrderEvent());
  }

  @Test
  @DisplayName("validate gọi validate của BalancesLockEvent")
  void testValidateWithBalancesLockEvent() {
    // Given
    DisruptorEvent event = new DisruptorEvent();
    event.setBalancesLockEvent(mockBalancesLockEvent);

    // When
    event.validate();

    // Then
    verify(mockBalancesLockEvent).validate();
  }

  @Test
  @DisplayName("toOperationObjectMessageJson trả về MessageJson đúng format với BalancesLockEvent")
  void testToOperationObjectMessageJsonWithBalancesLockEvent() {
    // Given
    String testEventId = "test-balances-lock-event-id";
    Map<String, Object> objectJson = new HashMap<>();
    objectJson.put("lockId", "test-lock-id");
    objectJson.put("status", "LOCKED");

    Map<String, Object> messageJson = new HashMap<>();
    messageJson.put("object", objectJson);

    when(mockBalancesLockEvent.toOperationObjectMessageJson()).thenReturn(messageJson);
    when(mockBalancesLockEvent.getEventId()).thenReturn(testEventId);

    DisruptorEvent event = new DisruptorEvent();
    event.setBalancesLockEvent(mockBalancesLockEvent);

    // When
    Map<String, Object> result = event.toOperationObjectMessageJson();

    // Then
    assertNotNull(result);
    assertEquals(testEventId, result.get("inputEventId"));
    assertTrue(result.containsKey("isSuccess"));
    assertTrue(result.containsKey("errorMessage"));
    assertTrue(result.containsKey("timestamp"));

    assertEquals(objectJson, result.get("object"));
  }

  @Test
  @DisplayName("toDisruptorMessageJson trả về MessageJson đúng format với BalancesLockEvent")
  void testToDisruptorMessageJsonWithBalancesLockEvent() {
    // Given
    String testEventId = "test-balances-lock-event-id";
    Map<String, Object> baseMessageJson = new HashMap<>();
    baseMessageJson.put("operationType", "balances_lock_create");
    baseMessageJson.put("lockId", "test-lock-id");
    baseMessageJson.put("accountKeys", Arrays.asList("account1", "account2"));

    when(mockBalancesLockEvent.toBaseMessageJson()).thenReturn(baseMessageJson);
    when(mockBalancesLockEvent.getEventId()).thenReturn(testEventId);

    DisruptorEvent event = new DisruptorEvent();
    event.setBalancesLockEvent(mockBalancesLockEvent);

    // When
    Map<String, Object> result = event.toDisruptorMessageJson();

    // Then
    assertNotNull(result);
    assertEquals(testEventId, result.get("eventId"));
    assertTrue(result.containsKey("isSuccess"));
    assertTrue(result.containsKey("errorMessage"));
    assertTrue(result.containsKey("timestamp"));

    assertEquals("balances_lock_create", result.get("operationType"));
    assertEquals("test-lock-id", result.get("lockId"));
    assertEquals(Arrays.asList("account1", "account2"), result.get("accountKeys"));
  }
}
