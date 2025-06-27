package com.exchangeengine.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.AmmPoolCache;
import com.exchangeengine.extension.CombinedTestExtension;
import com.exchangeengine.factory.AmmOrderFactory;

@ExtendWith({ MockitoExtension.class, CombinedTestExtension.class })
public class AmmOrderTest {

  @Mock
  private AccountCache mockAccountCache;

  @Mock
  private AmmPoolCache mockAmmPoolCache;

  @Mock
  private Account mockAccount;

  @Mock
  private AmmPool mockPool;

  private AmmOrder order;

  @BeforeEach
  public void setUp() {
    // Thiết lập mocks
    AccountCache.setTestInstance(mockAccountCache);
    AmmPoolCache.setTestInstance(mockAmmPoolCache);

    // Tạo order test sử dụng Factory
    order = AmmOrderFactory.createCustomAmmOrder(Map.of(
        "identifier", "test-order-id",
        "poolPair", "USDT-VND",
        "ownerAccountKey0", "account0",
        "ownerAccountKey1", "account1",
        "zeroForOne", true,
        "amountSpecified", new BigDecimal("1000"),
        "status", AmmOrder.STATUS_PROCESSING,
        "slippage", new BigDecimal("0.05")));
  }

  @Test
  @DisplayName("Constructor với đầy đủ tham số nên khởi tạo AmmOrder đúng")
  public void testFullParameterConstructor() {
    // Chuẩn bị
    String identifier = "order-123";
    String poolPair = "BTC-USD";
    String ownerAccountKey0 = "account-abc";
    String ownerAccountKey1 = "account-xyz";
    Boolean zeroForOne = false;
    BigDecimal amountSpecified = new BigDecimal("500");
    BigDecimal slippage = new BigDecimal("0.01");
    String status = AmmOrder.STATUS_PROCESSING;

    // Thực hiện
    AmmOrder order = new AmmOrder(
        identifier,
        poolPair,
        ownerAccountKey0,
        ownerAccountKey1,
        zeroForOne,
        amountSpecified,
        slippage,
        status);

    // Kiểm tra
    assertEquals(identifier, order.getIdentifier(), "Identifier phải được khởi tạo đúng");
    assertEquals(poolPair, order.getPoolPair(), "PoolPair phải được khởi tạo đúng");
    assertEquals(ownerAccountKey0, order.getOwnerAccountKey0(), "OwnerAccountKey0 phải được khởi tạo đúng");
    assertEquals(ownerAccountKey1, order.getOwnerAccountKey1(), "OwnerAccountKey1 phải được khởi tạo đúng");
    assertEquals(zeroForOne, order.getZeroForOne(), "ZeroForOne phải được khởi tạo đúng");
    assertEquals(amountSpecified, order.getAmountSpecified(), "AmountSpecified phải được khởi tạo đúng");
    assertEquals(slippage, order.getSlippage(), "Slippage phải được khởi tạo đúng");
    assertEquals(status, order.getStatus(), "Status phải được khởi tạo đúng");
    assertTrue(order.getCreatedAt() > 0, "CreatedAt phải được thiết lập");
    assertTrue(order.getUpdatedAt() > 0, "UpdatedAt phải được thiết lập");
  }

  @Test
  @DisplayName("validateRequiredFields should return errors for missing required fields")
  public void testValidateRequiredFields_MissingFields() {
    // Chuẩn bị
    AmmOrder invalidOrder = new AmmOrder();

    // Thực hiện
    List<String> errors = invalidOrder.validateRequiredFields();

    // Kiểm tra
    assertFalse(errors.isEmpty(), "Phải có lỗi cho các trường thiếu");
    assertTrue(errors.stream().anyMatch(err -> err.contains("identifier is required")), "Phải có lỗi về identifier");
    assertTrue(errors.stream().anyMatch(err -> err.contains("Pool pair is required")), "Phải có lỗi về pool pair");
  }

  @Test
  @DisplayName("validateRequiredFields should return errors for invalid slippage")
  public void testValidateRequiredFields_InvalidSlippage() {
    // Chuẩn bị
    order.setSlippage(new BigDecimal("0.000001")); // Quá nhỏ

    // Thực hiện
    List<String> errors = order.validateRequiredFields();

    // Kiểm tra
    assertFalse(errors.isEmpty(), "Phải có lỗi cho slippage không hợp lệ");
    assertTrue(errors.stream().anyMatch(err -> err.contains("Slippage")), "Phải có lỗi về slippage");
  }

  @Test
  @DisplayName("validateRequiredFields should return errors for non-positive amount")
  public void testValidateRequiredFields_NonPositiveAmount() {
    // Chuẩn bị
    order.setAmountSpecified(BigDecimal.ZERO);

    // Thực hiện
    List<String> errors = order.validateRequiredFields();

    // Kiểm tra
    assertFalse(errors.isEmpty(), "Phải có lỗi cho amount không hợp lệ");
    assertTrue(errors.stream().anyMatch(err -> err.contains("Amount specified")), "Phải có lỗi về amount");
  }

  @Test
  @DisplayName("validateResourcesExist should return errors when pool does not exist")
  public void testValidateResourcesExist_PoolNotExist() {
    // Chuẩn bị
    when(mockAmmPoolCache.getAmmPool(anyString())).thenReturn(Optional.empty());

    // Thực hiện
    List<String> errors = order.validateResourcesExist();

    // Kiểm tra
    assertFalse(errors.isEmpty(), "Phải có lỗi khi pool không tồn tại");
    assertTrue(errors.stream().anyMatch(err -> err.contains("Pool not found")), "Phải có lỗi về pool không tồn tại");
  }

  @Test
  @DisplayName("validateResourcesExist should return errors when pool is not active")
  public void testValidateResourcesExist_PoolNotActive() {
    // Chuẩn bị
    when(mockAmmPoolCache.getAmmPool(anyString())).thenReturn(Optional.of(mockPool));
    when(mockPool.isActive()).thenReturn(false);

    // Thực hiện
    List<String> errors = order.validateResourcesExist();

    // Kiểm tra
    assertFalse(errors.isEmpty(), "Phải có lỗi khi pool không active");
    assertTrue(errors.stream().anyMatch(err -> err.contains("Pool is not active")), "Phải có lỗi về pool không active");
  }

  @Test
  @DisplayName("validateResourcesExist should return errors when account0 does not exist")
  public void testValidateResourcesExist_Account0NotExist() {
    // Chuẩn bị
    when(mockAmmPoolCache.getAmmPool(anyString())).thenReturn(Optional.of(mockPool));
    when(mockPool.isActive()).thenReturn(true);

    // Account0 không tồn tại
    when(mockAccountCache.getAccount("account0")).thenReturn(Optional.empty());
    // Account1 tồn tại
    when(mockAccountCache.getAccount("account1")).thenReturn(Optional.of(mockAccount));

    // Thực hiện
    List<String> errors = order.validateResourcesExist();

    // Kiểm tra
    assertFalse(errors.isEmpty(), "Phải có lỗi khi account0 không tồn tại");
    assertTrue(errors.stream().anyMatch(err -> err.contains("Account not found: account0")),
        "Phải có lỗi về account0 không tồn tại");
  }

  @Test
  @DisplayName("validateResourcesExist should return errors when account1 does not exist")
  public void testValidateResourcesExist_Account1NotExist() {
    // Chuẩn bị
    when(mockAmmPoolCache.getAmmPool(anyString())).thenReturn(Optional.of(mockPool));
    when(mockPool.isActive()).thenReturn(true);

    // Account0 tồn tại
    when(mockAccountCache.getAccount("account0")).thenReturn(Optional.of(mockAccount));
    // Account1 không tồn tại
    when(mockAccountCache.getAccount("account1")).thenReturn(Optional.empty());

    // Thực hiện
    List<String> errors = order.validateResourcesExist();

    // Kiểm tra
    assertFalse(errors.isEmpty(), "Phải có lỗi khi account1 không tồn tại");
    assertTrue(errors.stream().anyMatch(err -> err.contains("Account not found: account1")),
        "Phải có lỗi về account1 không tồn tại");
  }

  @Test
  @DisplayName("validateResourcesExist should return errors when both accounts do not exist")
  public void testValidateResourcesExist_AccountsNotExist() {
    // Chuẩn bị
    when(mockAmmPoolCache.getAmmPool(anyString())).thenReturn(Optional.of(mockPool));
    when(mockPool.isActive()).thenReturn(true);
    when(mockAccountCache.getAccount(anyString())).thenReturn(Optional.empty());

    // Thực hiện
    List<String> errors = order.validateResourcesExist();

    // Kiểm tra
    assertFalse(errors.isEmpty(), "Phải có lỗi khi accounts không tồn tại");
    assertTrue(errors.stream().anyMatch(err -> err.contains("Account not found")),
        "Phải có lỗi về account không tồn tại");
  }

  @Test
  @DisplayName("updateAfterExecution should update order information")
  public void testUpdateAfterExecution() {
    // Chuẩn bị
    BigDecimal amountActual = new BigDecimal("950");
    BigDecimal amountReceived = new BigDecimal("940");
    int beforeTickIndex = 100;
    int afterTickIndex = 110;
    Map<String, BigDecimal> fees = Map.of("fee", new BigDecimal("10"));

    // Thực hiện
    boolean result = order.updateAfterExecution(amountActual, amountReceived, beforeTickIndex, afterTickIndex, fees);

    // Kiểm tra
    assertTrue(result, "Cập nhật phải thành công");
    assertEquals(amountActual, order.getAmountActual(), "amountActual phải được cập nhật");
    assertEquals(amountReceived, order.getAmountReceived(), "amountReceived phải được cập nhật");
    assertEquals(beforeTickIndex, order.getBeforeTickIndex(), "beforeTickIndex phải được cập nhật");
    assertEquals(afterTickIndex, order.getAfterTickIndex(), "afterTickIndex phải được cập nhật");
    assertEquals(fees, order.getFees(), "fees phải được cập nhật");
  }

  @Test
  @DisplayName("updateAfterExecution should not update if order is not processing")
  public void testUpdateAfterExecution_NotProcessing() {
    // Chuẩn bị
    AmmOrder successOrder = AmmOrderFactory.createCustomAmmOrder(Map.of(
        "identifier", "test-order-id",
        "poolPair", "USDT-VND",
        "ownerAccountKey0", "account0",
        "ownerAccountKey1", "account1",
        "zeroForOne", true,
        "amountSpecified", new BigDecimal("1000"),
        "status", AmmOrder.STATUS_SUCCESS));

    BigDecimal originalAmount = successOrder.getAmountActual();

    // Thực hiện
    boolean result = successOrder.updateAfterExecution(
        new BigDecimal("950"),
        new BigDecimal("940"),
        100,
        110,
        new HashMap<>());

    // Kiểm tra
    assertFalse(result, "Cập nhật phải thất bại khi status không phải processing");
    assertEquals(originalAmount, successOrder.getAmountActual(), "amountActual không được cập nhật");
  }

  @Test
  @DisplayName("markSuccess should update status to success")
  public void testMarkSuccess() {
    // Thực hiện
    boolean result = order.markSuccess();

    // Kiểm tra
    assertTrue(result, "Cập nhật phải thành công");
    assertEquals(AmmOrder.STATUS_SUCCESS, order.getStatus(), "Status phải là success");
    assertTrue(order.getCompletedAt() > 0, "CompletedAt phải được thiết lập");
    assertEquals(order.getCompletedAt(), order.getUpdatedAt(), "UpdatedAt phải được cập nhật");
  }

  @Test
  @DisplayName("markSuccess should not update if order is not processing")
  public void testMarkSuccess_NotProcessing() {
    // Chuẩn bị
    AmmOrder errorOrder = AmmOrderFactory.createCustomAmmOrder(Map.of(
        "identifier", "test-order-id",
        "poolPair", "USDT-VND",
        "ownerAccountKey0", "account0",
        "ownerAccountKey1", "account1",
        "zeroForOne", true,
        "amountSpecified", new BigDecimal("1000"),
        "status", AmmOrder.STATUS_ERROR));

    // Thực hiện
    boolean result = errorOrder.markSuccess();

    // Kiểm tra
    assertFalse(result, "Cập nhật phải thất bại khi status không phải processing");
    assertEquals(AmmOrder.STATUS_ERROR, errorOrder.getStatus(), "Status không được thay đổi");
  }

  @Test
  @DisplayName("markError should update status to error")
  public void testMarkError() {
    // Chuẩn bị
    String errorMessage = "Test error";

    // Thực hiện
    boolean result = order.markError(errorMessage);

    // Kiểm tra
    assertTrue(result, "Cập nhật phải thành công");
    assertEquals(AmmOrder.STATUS_ERROR, order.getStatus(), "Status phải là error");
    assertEquals(errorMessage, order.getErrorMessage(), "ErrorMessage phải được thiết lập");
    assertTrue(order.getCompletedAt() > 0, "CompletedAt phải được thiết lập");
    assertEquals(order.getCompletedAt(), order.getUpdatedAt(), "UpdatedAt phải được cập nhật");
  }

  @Test
  @DisplayName("markError should not update if order is not processing")
  public void testMarkError_NotProcessing() {
    // Chuẩn bị
    AmmOrder successOrder = AmmOrderFactory.createCustomAmmOrder(Map.of(
        "identifier", "test-order-id",
        "poolPair", "USDT-VND",
        "ownerAccountKey0", "account0",
        "ownerAccountKey1", "account1",
        "zeroForOne", true,
        "amountSpecified", new BigDecimal("1000"),
        "status", AmmOrder.STATUS_SUCCESS));

    String originalError = successOrder.getErrorMessage();

    // Thực hiện
    boolean result = successOrder.markError("Test error");

    // Kiểm tra
    assertFalse(result, "Cập nhật phải thất bại khi status không phải processing");
    assertEquals(AmmOrder.STATUS_SUCCESS, successOrder.getStatus(), "Status không được thay đổi");
    assertEquals(originalError, successOrder.getErrorMessage(), "ErrorMessage không được thay đổi");
  }

  @Test
  @DisplayName("isProcessing should return true for processing status")
  public void testIsProcessing() {
    // Chuẩn bị sử dụng order từ setUp (đã là processing)

    // Thực hiện & Kiểm tra
    assertTrue(order.isProcessing(), "isProcessing phải trả về true cho status processing");
  }

  @Test
  @DisplayName("isSuccess should return true for success status")
  public void testIsSuccess() {
    // Chuẩn bị
    AmmOrder successOrder = AmmOrderFactory.createCustomAmmOrder(Map.of(
        "status", AmmOrder.STATUS_SUCCESS));

    // Thực hiện & Kiểm tra
    assertTrue(successOrder.isSuccess(), "isSuccess phải trả về true cho status success");
  }

  @Test
  @DisplayName("isError should return true for error status")
  public void testIsError() {
    // Chuẩn bị
    AmmOrder errorOrder = AmmOrderFactory.createCustomAmmOrder(Map.of(
        "status", AmmOrder.STATUS_ERROR));

    // Thực hiện & Kiểm tra
    assertTrue(errorOrder.isError(), "isError phải trả về true cho status error");
  }

  @Test
  @DisplayName("getAccount0 should return the account")
  public void testGetAccount0() {
    // Chuẩn bị
    when(mockAccountCache.getAccount("account0")).thenReturn(Optional.of(mockAccount));

    // Thực hiện
    Account result = order.getAccount0();

    // Kiểm tra
    assertNotNull(result, "Account không được null");
    assertSame(mockAccount, result, "Phải trả về account từ cache");
  }

  @Test
  @DisplayName("getAccount0 should throw exception when account not found")
  public void testGetAccount0_NotFound() {
    // Chuẩn bị
    when(mockAccountCache.getAccount("account0")).thenReturn(Optional.empty());

    // Thực hiện & Kiểm tra
    assertThrows(IllegalStateException.class, () -> order.getAccount0(),
        "Phải throw exception khi account không tồn tại");
  }

  @Test
  @DisplayName("getAccount1 should return the account")
  public void testGetAccount1() {
    // Chuẩn bị
    when(mockAccountCache.getAccount("account1")).thenReturn(Optional.of(mockAccount));

    // Thực hiện
    Account result = order.getAccount1();

    // Kiểm tra
    assertNotNull(result, "Account không được null");
    assertSame(mockAccount, result, "Phải trả về account từ cache");
  }

  @Test
  @DisplayName("getAccount1 should throw exception when account not found")
  public void testGetAccount1_NotFound() {
    // Chuẩn bị
    when(mockAccountCache.getAccount("account1")).thenReturn(Optional.empty());

    // Thực hiện & Kiểm tra
    assertThrows(IllegalStateException.class, () -> order.getAccount1(),
        "Phải throw exception khi account không tồn tại");
  }

  @Test
  @DisplayName("getPool should return the pool")
  public void testGetPool() {
    // Chuẩn bị
    when(mockAmmPoolCache.getAmmPool("USDT-VND")).thenReturn(Optional.of(mockPool));

    // Thực hiện
    Optional<AmmPool> result = order.getPool();

    // Kiểm tra
    assertTrue(result.isPresent(), "Pool phải tồn tại");
    assertSame(mockPool, result.get(), "Phải trả về pool từ cache");
  }

  @Test
  @DisplayName("toMessageJson should return map representation")
  public void testToMessageJson() {
    // Chuẩn bị
    BigDecimal amountReceived = new BigDecimal("800");
    order.setAmountReceived(amountReceived);

    // Thực hiện
    Map<String, Object> result = order.toMessageJson();

    // Kiểm tra
    assertNotNull(result, "Map không được null");
    assertEquals(order.getIdentifier(), result.get("identifier"), "identifier phải khớp");
    assertEquals(order.getPoolPair(), result.get("poolPair"), "poolPair phải khớp");
    assertEquals(order.getAmountSpecified().toString(), result.get("amountSpecified").toString(),
        "amountSpecified phải khớp");
    assertEquals(order.getAmountReceived().toString(), result.get("amountReceived").toString(),
        "amountReceived phải khớp");
  }
}
