package com.exchangeengine.service.engine;

import com.exchangeengine.factory.event.CoinDepositEventFactory;
import com.exchangeengine.factory.event.CoinWithdrawalEventFactory;
import com.exchangeengine.factory.event.AccountEventFactory;
import com.exchangeengine.factory.event.AmmPoolEventFactory;
import com.exchangeengine.factory.event.MerchantEscrowEventFactory;
import com.exchangeengine.model.event.CoinDepositEvent;
import com.exchangeengine.model.event.CoinWithdrawalEvent;
import com.exchangeengine.model.event.AccountEvent;
import com.exchangeengine.model.event.AmmPoolEvent;
import com.exchangeengine.model.event.MerchantEscrowEvent;
import com.exchangeengine.model.event.AmmPositionEvent;
import com.exchangeengine.factory.event.AmmPositionEventFactory;
import com.exchangeengine.model.event.AmmOrderEvent;
import com.exchangeengine.factory.event.AmmOrderEventFactory;
import com.exchangeengine.model.event.TradeEvent;
import com.exchangeengine.factory.event.TradeEventFactory;
import com.exchangeengine.model.event.OfferEvent;
import com.exchangeengine.factory.event.OfferEventFactory;
import com.exchangeengine.factory.event.BalancesLockEventFactory;
import com.exchangeengine.model.event.BalancesLockEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EngineHandlerTest {

  @Mock
  private EngineDisruptorService mockDisruptorService;

  private EngineHandler engineHandler;
  private EngineHandler originalInstance;

  @BeforeEach
  void setUp() throws Exception {
    // Lưu trữ instance gốc trước khi thay đổi
    Field instanceField = EngineHandler.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    originalInstance = (EngineHandler) instanceField.get(null);

    // Tạo một instance mới của EngineHandler với mockDisruptorService
    engineHandler = createEngineHandlerInstance(mockDisruptorService);

    // Thiết lập instance mới vào singleton
    instanceField.set(null, engineHandler);
  }

  @AfterEach
  void tearDown() throws Exception {
    // Khôi phục lại instance gốc
    Field instanceField = EngineHandler.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, originalInstance);
  }

  private EngineHandler createEngineHandlerInstance(EngineDisruptorService disruptorService) throws Exception {
    // Sử dụng reflection để gọi constructor private
    Constructor<EngineHandler> constructor = EngineHandler.class.getDeclaredConstructor(EngineDisruptorService.class);
    constructor.setAccessible(true);
    return constructor.newInstance(disruptorService);
  }

  @Test
  @DisplayName("Phương thức deposit nên gọi disruptorService.deposit với tham số đúng")
  void deposit_ShouldCallDisruptorServiceDeposit() {
    // Arrange
    CoinDepositEvent depositEvent = CoinDepositEventFactory.create();

    // Act
    engineHandler.deposit(depositEvent);

    // Assert
    verify(mockDisruptorService).deposit(depositEvent);
  }

  @Test
  @DisplayName("Phương thức withdraw nên gọi disruptorService.withdraw với tham số đúng")
  void withdraw_ShouldCallDisruptorServiceWithdraw() {
    // Arrange
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.create();

    // Act
    engineHandler.withdraw(withdrawalEvent);

    // Assert
    verify(mockDisruptorService).withdraw(withdrawalEvent);
  }

  @Test
  @DisplayName("Phương thức createCoinAccount nên gọi disruptorService.createCoinAccount với tham số đúng")
  void createCoinAccount_ShouldCallDisruptorServiceCreateCoinAccount() {
    // Arrange
    AccountEvent accountEvent = AccountEventFactory.create();

    // Act
    engineHandler.createCoinAccount(accountEvent);

    // Assert
    verify(mockDisruptorService).createCoinAccount(accountEvent);
  }

  @Test
  @DisplayName("Phương thức ammPool nên gọi disruptorService.ammPool với tham số đúng")
  void ammPool_ShouldCallDisruptorServiceAmmPool() {
    // Arrange
    AmmPoolEvent ammPoolEvent = AmmPoolEventFactory.create();

    // Act
    engineHandler.ammPool(ammPoolEvent);

    // Assert
    verify(mockDisruptorService).ammPool(ammPoolEvent);
  }

  @Test
  @DisplayName("Phương thức merchantEscrow nên gọi disruptorService.merchantEscrow với tham số đúng")
  void merchantEscrow_ShouldCallDisruptorServiceMerchantEscrow() {
    // Arrange
    MerchantEscrowEvent merchantEscrowEvent = MerchantEscrowEventFactory.create();

    // Act
    engineHandler.merchantEscrow(merchantEscrowEvent);

    // Assert
    verify(mockDisruptorService).merchantEscrow(merchantEscrowEvent);
  }

  @Test
  @DisplayName("Phương thức ammPosition nên gọi disruptorService.ammPosition với tham số đúng")
  void ammPosition_ShouldCallDisruptorServiceAmmPosition() {
    // Arrange
    AmmPositionEvent positionEvent = AmmPositionEventFactory.create();

    // Act
    engineHandler.ammPosition(positionEvent);

    // Assert
    verify(mockDisruptorService).ammPosition(positionEvent);
  }

  @Test
  @DisplayName("Phương thức ammOrder nên gọi disruptorService.ammOrder với tham số đúng")
  void ammOrder_ShouldCallDisruptorServiceAmmOrder() {
    // Arrange
    AmmOrderEvent orderEvent = AmmOrderEventFactory.create();

    // Act
    engineHandler.ammOrder(orderEvent);

    // Assert
    verify(mockDisruptorService).ammOrder(orderEvent);
  }

  @Test
  @DisplayName("Phương thức processTrade nên gọi disruptorService.trade với tham số đúng")
  void processTrade_ShouldCallDisruptorServiceTrade() {
    // Arrange
    TradeEvent tradeEvent = TradeEventFactory.create();

    // Act
    engineHandler.processTrade(tradeEvent);

    // Assert
    verify(mockDisruptorService).trade(tradeEvent);
  }

  @Test
  @DisplayName("Phương thức processOffer nên gọi disruptorService.offer với tham số đúng")
  void processOffer_ShouldCallDisruptorServiceOffer() {
    // Arrange
    OfferEvent offerEvent = OfferEventFactory.create();

    // Act
    engineHandler.processOffer(offerEvent);

    // Assert
    verify(mockDisruptorService).offer(offerEvent);
  }

  @Test
  @DisplayName("Phương thức getInstance nên trả về cùng một instance")
  void getInstance_ShouldReturnSameInstance() {
    // Act
    EngineHandler instance1 = EngineHandler.getInstance();
    EngineHandler instance2 = EngineHandler.getInstance();

    // Assert
    assertSame(instance1, instance2);
  }

  @Test
  @DisplayName("Phương thức getInstance nên tạo instance mới khi instance là null")
  void getInstance_ShouldCreateNewInstanceWhenNull() throws Exception {
    // Arrange
    Field instanceField = EngineHandler.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);

    // Act
    EngineDisruptorService mockService = mock(EngineDisruptorService.class);

    try (MockedStatic<EngineDisruptorService> mockedStatic = mockStatic(EngineDisruptorService.class)) {
      mockedStatic.when(EngineDisruptorService::getInstance).thenReturn(mockService);

      EngineHandler instance = EngineHandler.getInstance();

      // Assert
      assertNotNull(instance);

      // Verify EngineDisruptorService.getInstance() was called
      mockedStatic.verify(EngineDisruptorService::getInstance);

      // Verify disruptorService field was set correctly
      Field disruptorServiceField = EngineHandler.class.getDeclaredField("disruptorService");
      disruptorServiceField.setAccessible(true);
      EngineDisruptorService actualDisruptorService = (EngineDisruptorService) disruptorServiceField.get(instance);
      assertSame(mockService, actualDisruptorService);
    }

    // Restore original instance for other tests
    instanceField.set(null, engineHandler);
  }

  @Test
  @DisplayName("Constructor mặc định nên khởi tạo disruptorService từ getInstance")
  void defaultConstructor_ShouldInitializeDisruptorServiceFromGetInstance() throws Exception {
    // Arrange
    Constructor<EngineHandler> constructor = EngineHandler.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    // Chuẩn bị mock static method EngineDisruptorService.getInstance()
    EngineDisruptorService expectedDisruptorService = mock(EngineDisruptorService.class);

    try (MockedStatic<EngineDisruptorService> mockedStatic = mockStatic(EngineDisruptorService.class)) {
      mockedStatic.when(EngineDisruptorService::getInstance).thenReturn(expectedDisruptorService);

      // Act
      EngineHandler handler = constructor.newInstance();

      // Assert - Verify disruptorService field was set correctly
      Field disruptorServiceField = EngineHandler.class.getDeclaredField("disruptorService");
      disruptorServiceField.setAccessible(true);
      EngineDisruptorService actualDisruptorService = (EngineDisruptorService) disruptorServiceField.get(handler);

      assertSame(expectedDisruptorService, actualDisruptorService);
    }
  }

  @Test
  @DisplayName("Phương thức setTestInstance nên thiết lập instance test")
  void setTestInstance_ShouldSetTheTestInstance() throws Exception {
    // Arrange
    EngineHandler mockHandler = mock(EngineHandler.class);

    // Act
    EngineHandler.setTestInstance(mockHandler);

    // Assert
    EngineHandler instance = EngineHandler.getInstance();
    assertSame(mockHandler, instance);
  }

  @Test
  @DisplayName("Phương thức balancesLock nên gọi disruptorService.balancesLock với tham số đúng")
  void balancesLock_ShouldCallDisruptorServiceBalancesLock() {
    // Arrange
    BalancesLockEvent balancesLockEvent = BalancesLockEventFactory.create();

    // Act
    engineHandler.balancesLock(balancesLockEvent);

    // Assert
    verify(mockDisruptorService).balancesLock(balancesLockEvent);
  }
}
