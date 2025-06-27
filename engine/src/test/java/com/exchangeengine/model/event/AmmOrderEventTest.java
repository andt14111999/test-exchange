package com.exchangeengine.model.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.spy;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exchangeengine.extension.CombinedTestExtension;
import com.exchangeengine.factory.event.AmmOrderEventFactory;
import com.exchangeengine.model.Account;
import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.AmmOrder;
import com.exchangeengine.model.AmmPool;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.AmmOrderCache;
import com.exchangeengine.storage.cache.AmmPoolCache;
import com.fasterxml.jackson.databind.JsonNode;

@ExtendWith({ MockitoExtension.class, CombinedTestExtension.class })
public class AmmOrderEventTest {

  @Mock
  private AmmOrderCache ammOrderCache;

  @Mock
  private AccountCache accountCache;

  @Mock
  private AmmPoolCache ammPoolCache;

  @Mock
  private AmmPool ammPool;

  @Mock
  private Account account;

  private AmmOrderEvent ammOrderEvent;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Thiết lập AmmOrderCache mock
    AmmOrderCache.setTestInstance(ammOrderCache);
    lenient().when(ammOrderCache.ammOrderExists(anyString())).thenReturn(false);

    // Thiết lập AccountCache mock
    AccountCache.setTestInstance(accountCache);
    lenient().when(accountCache.getAccount(anyString())).thenReturn(Optional.of(account));

    // Thiết lập AmmPoolCache mock
    AmmPoolCache.setTestInstance(ammPoolCache);
    lenient().when(ammPoolCache.getAmmPool(anyString())).thenReturn(Optional.of(ammPool));

    // Khởi tạo AmmOrderEvent sử dụng factory
    ammOrderEvent = AmmOrderEventFactory.withIdentifier("order-id");
  }

  @Test
  void testGetProducerEventId() {
    assertEquals("order-id", ammOrderEvent.getProducerEventId());
  }

  @Test
  void testGetEventHandler() {
    assertEquals(EventHandlerAction.AMM_ORDER_EVENT, ammOrderEvent.getEventHandler());
  }

  @Test
  void testCheckAmmOrderExists_False() {
    assertFalse(ammOrderEvent.checkAmmOrderExists());
  }

  @Test
  void testCheckAmmOrderExists_True() {
    when(ammOrderCache.ammOrderExists("order-id")).thenReturn(true);
    assertTrue(ammOrderEvent.checkAmmOrderExists());
  }

  @Test
  void testToAmmOrder_NotFound() {
    AmmOrder result = ammOrderEvent.toAmmOrder(false);

    assertNotNull(result);
    assertEquals("order-id", result.getIdentifier());
    assertEquals("BTC/USDT", result.getPoolPair());
  }

  @Test
  void testParserData() throws Exception {
    // Sử dụng Factory để tạo JsonNode
    JsonNode jsonNode = AmmOrderEventFactory.createJsonNodeWithIdentifier("order-id");

    // Tạo AmmOrderEvent mới và parse
    AmmOrderEvent event = new AmmOrderEvent();
    event.parserData(jsonNode);

    // Kiểm tra kết quả
    assertEquals(jsonNode.path("eventId").asText(), event.getEventId());
    assertEquals(ActionType.AMM_ORDER, event.getActionType());
    assertEquals(jsonNode.path("actionId").asText(), event.getActionId());
    assertEquals(OperationType.AMM_ORDER_SWAP, event.getOperationType());
    assertEquals("order-id", event.getIdentifier());
    assertEquals(jsonNode.path("poolPair").asText(), event.getPoolPair());
    assertEquals(jsonNode.path("ownerAccountKey0").asText(), event.getOwnerAccountKey0());
    assertEquals(jsonNode.path("ownerAccountKey1").asText(), event.getOwnerAccountKey1());
    assertEquals(jsonNode.path("status").asText(), event.getStatus());
    assertEquals(jsonNode.path("zeroForOne").asBoolean(), event.getZeroForOne());
    assertEquals(new BigDecimal(jsonNode.path("amountSpecified").asText()), event.getAmountSpecified());
    assertEquals(BigDecimal.valueOf(jsonNode.path("slippage").asDouble()), event.getSlippage());
  }

  @Test
  void testInvalidActionType() {
    ammOrderEvent.setActionType(ActionType.COIN_ACCOUNT);
    assertEquals(ActionType.COIN_ACCOUNT, ammOrderEvent.getActionType());
  }

  @Test
  void testInvalidOperationType() {
    ammOrderEvent.setOperationType(OperationType.COIN_DEPOSIT_CREATE);
    assertEquals(OperationType.COIN_DEPOSIT_CREATE, ammOrderEvent.getOperationType());
  }

  @Test
  @DisplayName("Kiểm tra validate với event hợp lệ")
  void testValidateWithValidEvent() {
    // Tạo một event hợp lệ
    AmmOrderEvent event = AmmOrderEventFactory.create();

    // Thiết lập mockAmmPool trả về active=true
    when(ammPoolCache.getAmmPool(anyString())).thenReturn(Optional.of(ammPool));
    when(ammPool.isActive()).thenReturn(true);
    when(accountCache.getAccount(anyString())).thenReturn(Optional.of(account));

    // Thực thi
    assertDoesNotThrow(() -> event.validate(), "Validate không được ném ra exception với event hợp lệ");
  }

  @Test
  @DisplayName("Kiểm tra validate với identifier null")
  void testValidateWithNullIdentifier() {
    // Tạo event với identifier null
    AmmOrderEvent event = AmmOrderEventFactory.create();
    event.setIdentifier(null);

    // Thực thi và kiểm tra
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
    assertTrue(exception.getMessage().contains("Order identifier is required"),
        "Exception phải chứa thông báo về identifier bị thiếu");
  }

  @Test
  @DisplayName("Kiểm tra validate với identifier rỗng")
  void testValidateWithEmptyIdentifier() {
    // Tạo event với identifier rỗng
    AmmOrderEvent event = AmmOrderEventFactory.create();
    event.setIdentifier("");

    // Thực thi và kiểm tra
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
    assertTrue(exception.getMessage().contains("Order identifier is required"),
        "Exception phải chứa thông báo về identifier rỗng");
  }

  @Test
  @DisplayName("Kiểm tra validate với action type không phù hợp")
  void testValidateWithIncorrectActionType() {
    // Tạo event với action type không phải AMM_ORDER
    AmmOrderEvent event = AmmOrderEventFactory.create();
    event.setActionType(ActionType.COIN_ACCOUNT);

    // Thực thi và kiểm tra
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
    assertTrue(exception.getMessage().contains("Action type not matched"),
        "Exception phải chứa thông báo về action type không phù hợp");
  }

  @Test
  @DisplayName("Kiểm tra validate với operation type không được hỗ trợ")
  void testValidateWithUnsupportedOperationType() {
    // Tạo event với operation type không được hỗ trợ
    AmmOrderEvent event = AmmOrderEventFactory.create();
    event.setOperationType(OperationType.COIN_DEPOSIT_CREATE);

    // Thực thi và kiểm tra
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
    assertTrue(exception.getMessage().contains("Operation type is not supported"),
        "Exception phải chứa thông báo về operation type không được hỗ trợ");
  }

  @Test
  @DisplayName("Kiểm tra validate khi order đã tồn tại")
  void testValidateWhenOrderAlreadyExists() {
    // Cài đặt mock để trả về true khi kiểm tra tồn tại
    when(ammOrderCache.ammOrderExists(anyString())).thenReturn(true);

    // Tạo event với order đã tồn tại
    AmmOrderEvent event = AmmOrderEventFactory.create();

    // Thực thi và kiểm tra
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
    assertTrue(exception.getMessage().contains("already exists"),
        "Exception phải chứa thông báo về order đã tồn tại");
  }

  @Test
  @DisplayName("Kiểm tra checkAmmOrderExists")
  void testCheckAmmOrderExists() {
    // Cài đặt mock
    when(ammOrderCache.ammOrderExists("existing-order")).thenReturn(true);
    when(ammOrderCache.ammOrderExists("new-order")).thenReturn(false);

    // Tạo events
    AmmOrderEvent existingEvent = AmmOrderEventFactory.withIdentifier("existing-order");
    AmmOrderEvent newEvent = AmmOrderEventFactory.withIdentifier("new-order");

    // Kiểm tra
    assertTrue(existingEvent.checkAmmOrderExists(), "Phải trả về true cho order đã tồn tại");
    assertFalse(newEvent.checkAmmOrderExists(), "Phải trả về false cho order chưa tồn tại");
  }

  @Test
  @DisplayName("Kiểm tra toAmmOrder không ném exception khi raiseException=false")
  void testToAmmOrderWithoutRaisingException() {
    // Cài đặt mock để trả về true khi kiểm tra tồn tại
    lenient().when(ammOrderCache.ammOrderExists(anyString())).thenReturn(true);

    // Tạo event với order đã tồn tại
    AmmOrderEvent event = AmmOrderEventFactory.create();

    // Thực thi - không nên ném exception khi raiseException=false
    assertDoesNotThrow(() -> event.toAmmOrder(false),
        "toAmmOrder không được ném ra exception khi raiseException=false");
  }

  @Test
  @DisplayName("Kiểm tra toAmmOrder ném exception khi raiseException=true và order đã tồn tại")
  void testToAmmOrderWithRaisingException() {
    // Cài đặt mock để trả về true khi kiểm tra tồn tại
    when(ammOrderCache.ammOrderExists(anyString())).thenReturn(true);

    // Tạo event với order đã tồn tại
    AmmOrderEvent event = AmmOrderEventFactory.create();

    // Thực thi - nên ném exception khi raiseException=true
    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> event.toAmmOrder(true));
    assertTrue(exception.getMessage().contains("already exists"),
        "Exception phải chứa thông báo về order đã tồn tại");
  }

  @Test
  @DisplayName("Kiểm tra toOperationObjectMessageJson trả về messageJson đúng định dạng")
  void testToOperationObjectMessageJson() {
    // Chuẩn bị
    AmmOrderEvent event = AmmOrderEventFactory.create();
    AmmOrder mockOrder = mock(AmmOrder.class);
    Map<String, Object> mockOrderJson = new HashMap<>();
    mockOrderJson.put("id", "test-id");
    mockOrderJson.put("poolPair", "BTC/USDT");

    // Mock để tránh exception khi gọi toAmmOrder(true), sử dụng lenient()
    lenient().when(ammOrderCache.ammOrderExists(anyString())).thenReturn(false);
    lenient().when(mockOrder.toMessageJson()).thenReturn(mockOrderJson);

    // Sử dụng spy để mock chỉ phương thức toAmmOrder
    AmmOrderEvent spyEvent = spy(event);
    doReturn(mockOrder).when(spyEvent).toAmmOrder(true);

    // Thực thi
    Map<String, Object> result = spyEvent.toOperationObjectMessageJson();

    // Kiểm tra
    assertNotNull(result);
    // Kiểm tra phần object
    assertNotNull(result.get("object"));
  }

  @Test
  @DisplayName("Kiểm tra toAmmOrder với raiseException=true nhưng order không tồn tại")
  void testToAmmOrderWithRaiseExceptionButOrderNotExists() {
    // Cài đặt mock để trả về false khi kiểm tra tồn tại
    when(ammOrderCache.ammOrderExists(anyString())).thenReturn(false);

    // Tạo event
    AmmOrderEvent event = AmmOrderEventFactory.create();

    // Thực thi - không nên ném exception khi raiseException=true nhưng order không
    // tồn tại
    AmmOrder result = assertDoesNotThrow(() -> event.toAmmOrder(true),
        "toAmmOrder không nên ném exception khi raiseException=true nhưng order không tồn tại");

    // Kiểm tra
    assertNotNull(result);
    assertEquals(event.getIdentifier(), result.getIdentifier());
  }
}
