package com.exchangeengine.service.engine;

import com.exchangeengine.factory.event.DisruptorEventFactory;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.storage.StorageService;
import com.exchangeengine.storage.cache.EventCache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.ArgumentCaptor;
import com.exchangeengine.extension.SingletonResetExtension;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

@ExtendWith({ MockitoExtension.class, SingletonResetExtension.class })
@MockitoSettings(strictness = Strictness.LENIENT)
class DisruptorEventHandlerTest {

  @Mock
  private StorageService mockStorageService;

  @Mock
  private OutputProcessor mockOutputProcessor;

  @Mock
  private EventCache mockEventCache;

  private DisruptorEventHandler eventHandler;

  @BeforeEach
  void setUp() throws Exception {
    // Reset all singleton instances
    SingletonResetExtension.resetAll();

    // Set mock StorageService instance
    StorageService.setTestInstance(mockStorageService);

    // Set mock OutputProcessor instance
    OutputProcessor.setTestInstance(mockOutputProcessor);

    // Mock EventCache và đảm bảo mockStorageService.getEventCache() trả về mock này
    when(mockStorageService.getEventCache()).thenReturn(mockEventCache);

    // Đảm bảo tất cả các phương thức được thiết lập trên mockEventCache
    Mockito.lenient().when(mockEventCache.isEventProcessed(anyString())).thenReturn(false);
    Mockito.lenient().doNothing().when(mockEventCache).updateEvent(anyString());

    // Tạo instance của DisruptorEventHandler
    eventHandler = new DisruptorEventHandler();

    // Sử dụng reflection để tiêm mockStorageService vào field storageService của
    // DisruptorEventHandler
    try {
      Field storageServiceField = DisruptorEventHandler.class.getDeclaredField("storageService");
      storageServiceField.setAccessible(true);
      storageServiceField.set(eventHandler, mockStorageService);

      // Tiêm mockOutputProcessor vào field outputProcessor của DisruptorEventHandler
      Field outputProcessorField = DisruptorEventHandler.class.getDeclaredField("outputProcessor");
      outputProcessorField.setAccessible(true);
      outputProcessorField.set(eventHandler, mockOutputProcessor);
    } catch (Exception e) {
      throw new RuntimeException("Failed to inject mocks using reflection", e);
    }
  }

  @AfterEach
  void tearDown() {
    // Không cần dọn dẹp, SingletonResetExtension sẽ làm việc này
  }

  @Test
  @DisplayName("onEvent nên xử lý sự kiện COIN_DEPOSIT_CREATE thành công")
  void onEvent_ShouldProcessDepositEventSuccessfully() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withCoinDepositEvent();
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Sử dụng try-with-resources để tạo mock cho DepositProcessor
    try (
        MockedConstruction<com.exchangeengine.service.engine.deposit.DepositProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.deposit.DepositProcessor.class,
            (mock, context) -> {
              // Đảm bảo mock trả về ProcessResult thành công
              ProcessResult mockResult = ProcessResult.success(event);
              when(mock.process()).thenReturn(mockResult);
            })) {

      // Act
      eventHandler.onEvent(event, 0, false);

      // Assert
      // Kiểm tra xem DepositProcessor có được tạo không
      assertEquals(1, mockedProcessor.constructed().size(), "DepositProcessor should be constructed");
      // Kiểm tra xem process() đã được gọi chưa
      verify(mockedProcessor.constructed().get(0)).process();
      // Kiểm tra EventCache.updateEvent và OutputProcessor.processOutput được gọi
      verify(mockEventCache).updateEvent(event.getEventId());
      verify(mockOutputProcessor).processOutput(any(ProcessResult.class), eq(false));
    }
  }

  @Test
  @DisplayName("onEvent nên xử lý sự kiện COIN_WITHDRAWAL_CREATE thành công")
  void onEvent_ShouldProcessWithdrawalEventSuccessfully() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withCoinWithdrawalEvent();
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Sử dụng try-with-resources để tạo mock cho WithdrawalProcessor
    try (
        MockedConstruction<com.exchangeengine.service.engine.withdrawal.WithdrawalProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.withdrawal.WithdrawalProcessor.class,
            (mock, context) -> {
              // Đảm bảo mock trả về ProcessResult thành công
              ProcessResult mockResult = ProcessResult.success(event);
              when(mock.process()).thenReturn(mockResult);
            })) {

      // Act
      eventHandler.onEvent(event, 0, false);

      // Assert
      // Kiểm tra xem WithdrawalProcessor có được tạo không
      assertEquals(1, mockedProcessor.constructed().size(), "WithdrawalProcessor should be constructed");
      // Kiểm tra xem process() đã được gọi chưa
      verify(mockedProcessor.constructed().get(0)).process();
      // Kiểm tra EventCache.updateEvent và OutputProcessor.processOutput được gọi
      verify(mockEventCache).updateEvent(event.getEventId());
      verify(mockOutputProcessor).processOutput(any(ProcessResult.class), eq(false));
    }
  }

  @Test
  @DisplayName("onEvent nên xử lý sự kiện COIN_WITHDRAWAL_RELEASING thành công")
  void onEvent_ShouldProcessWithdrawalReleasingEventSuccessfully() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withCoinWithdrawalEvent();
    event.getCoinWithdrawalEvent().setOperationType(OperationType.COIN_WITHDRAWAL_RELEASING);
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Sử dụng try-with-resources để tạo mock cho WithdrawalProcessor
    try (
        MockedConstruction<com.exchangeengine.service.engine.withdrawal.WithdrawalProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.withdrawal.WithdrawalProcessor.class,
            (mock, context) -> {
              // Đảm bảo mock trả về ProcessResult thành công
              ProcessResult mockResult = ProcessResult.success(event);
              when(mock.process()).thenReturn(mockResult);
            })) {

      // Act
      eventHandler.onEvent(event, 0, false);

      // Assert
      // Kiểm tra xem WithdrawalProcessor có được tạo không
      assertEquals(1, mockedProcessor.constructed().size(), "WithdrawalProcessor should be constructed");
      // Kiểm tra xem process() đã được gọi chưa
      verify(mockedProcessor.constructed().get(0)).process();
      // Kiểm tra EventCache.updateEvent và OutputProcessor.processOutput được gọi
      verify(mockEventCache).updateEvent(event.getEventId());
      verify(mockOutputProcessor).processOutput(any(ProcessResult.class), eq(false));
    }
  }

  @Test
  @DisplayName("onEvent nên xử lý sự kiện COIN_WITHDRAWAL_FAILED thành công")
  void onEvent_ShouldProcessWithdrawalFailedEventSuccessfully() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withCoinWithdrawalEvent();
    event.getCoinWithdrawalEvent().setOperationType(OperationType.COIN_WITHDRAWAL_FAILED);
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Sử dụng try-with-resources để tạo mock cho WithdrawalProcessor
    try (
        MockedConstruction<com.exchangeengine.service.engine.withdrawal.WithdrawalProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.withdrawal.WithdrawalProcessor.class,
            (mock, context) -> {
              // Đảm bảo mock trả về ProcessResult thành công
              ProcessResult mockResult = ProcessResult.success(event);
              when(mock.process()).thenReturn(mockResult);
            })) {

      // Act
      eventHandler.onEvent(event, 0, false);

      // Assert
      // Kiểm tra xem WithdrawalProcessor có được tạo không
      assertEquals(1, mockedProcessor.constructed().size(), "WithdrawalProcessor should be constructed");
      // Kiểm tra xem process() đã được gọi chưa
      verify(mockedProcessor.constructed().get(0)).process();
      // Kiểm tra EventCache.updateEvent và OutputProcessor.processOutput được gọi
      verify(mockEventCache).updateEvent(event.getEventId());
      verify(mockOutputProcessor).processOutput(any(ProcessResult.class), eq(false));
    }
  }

  @Test
  @DisplayName("onEvent nên xử lý sự kiện COIN_ACCOUNT_CREATE thành công")
  void onEvent_ShouldProcessCoinAccountEventSuccessfully() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withAccountEvent();
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Act
    eventHandler.onEvent(event, 0, false);

    // Assert
    verify(mockEventCache).updateEvent(event.getEventId());
    verify(mockOutputProcessor).processOutput(any(ProcessResult.class), eq(false));
  }

  @Test
  @DisplayName("onEvent nên bỏ qua sự kiện đã được xử lý trước đó")
  void onEvent_ShouldSkipAlreadyProcessedEvent() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withCoinDepositEvent();
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(true);

    // Act
    eventHandler.onEvent(event, 0, false);

    // Assert
    verify(mockEventCache).updateEvent(event.getEventId());
    verify(mockOutputProcessor).processOutput(any(ProcessResult.class), eq(false));
  }

  @Test
  @DisplayName("onEvent nên ghi nhận lỗi khi xử lý sự kiện không hợp lệ")
  void onEvent_ShouldHandleInvalidEventType() {
    // Arrange
    DisruptorEvent event = new DisruptorEvent(); // Sự kiện không có event nào được thiết lập
    when(mockEventCache.isEventProcessed(any())).thenReturn(false);

    // Act
    eventHandler.onEvent(event, 0, false);

    // Assert
    verify(mockEventCache).updateEvent(any());
    verify(mockOutputProcessor).processOutput(any(ProcessResult.class), eq(false));
  }

  @Test
  @DisplayName("onEvent nên xử lý nhiều sự kiện liên tiếp")
  void onEvent_ShouldProcessMultipleEvents() {
    // Arrange
    DisruptorEvent event1 = DisruptorEventFactory.withCoinDepositEvent();
    DisruptorEvent event2 = DisruptorEventFactory.withCoinWithdrawalEvent();
    DisruptorEvent event3 = DisruptorEventFactory.withAccountEvent();

    when(mockEventCache.isEventProcessed(any())).thenReturn(false);

    // Act
    eventHandler.onEvent(event1, 0, false);
    eventHandler.onEvent(event2, 1, false);
    eventHandler.onEvent(event3, 2, true);

    // Assert
    verify(mockEventCache, times(3)).updateEvent(any());
    verify(mockOutputProcessor, times(3)).processOutput(any(ProcessResult.class), anyBoolean());
    verify(mockOutputProcessor).processOutput(any(ProcessResult.class), eq(true));
  }

  @Test
  @DisplayName("shutdown nên gọi đúng các phương thức shutdown")
  void shutdown_ShouldShutdownCorrectly() {
    // Act
    eventHandler.shutdown();

    // Assert: No Exception
  }

  @Test
  @DisplayName("onEvent nên xử lý ngoại lệ trong try-catch khi không có event cụ thể")
  void onEvent_ShouldHandleExceptionInTryCatch() {
    // Arrange - Tạo DisruptorEvent mà không có event cụ thể nào
    DisruptorEvent event = new DisruptorEvent();
    // Đảm bảo event có eventId để tránh NullPointerException
    when(mockEventCache.isEventProcessed(any())).thenReturn(false);

    // Prepare a ProcessResult captor
    ArgumentCaptor<ProcessResult> resultCaptor = ArgumentCaptor.forClass(ProcessResult.class);

    // Act
    eventHandler.onEvent(event, 0, false);

    // Assert
    // Kiểm tra ProcessResult đã được gửi tới OutputProcessor
    verify(mockOutputProcessor).processOutput(resultCaptor.capture(), eq(false));

    // Kiểm tra ProcessResult đã được ghi nhận đúng
    ProcessResult capturedResult = resultCaptor.getValue();
    assertFalse(capturedResult.getEvent().isSuccess(), "ProcessResult should be unsuccessful when handling exception");
    assertTrue(capturedResult.getEvent().getErrorMessage() != null, "Error message should not be null");
  }

  @Test
  @DisplayName("onEvent nên xử lý sự kiện AMM_POOL_CREATE thành công")
  void onEvent_ShouldProcessAmmPoolEventSuccessfully() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withAmmPoolEvent();
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Sử dụng try-with-resources để tạo mock cho AmmPoolProcessor
    try (
        MockedConstruction<com.exchangeengine.service.engine.amm_pool.AmmPoolProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.amm_pool.AmmPoolProcessor.class,
            (mock, context) -> {
              // Đảm bảo mock trả về ProcessResult thành công
              ProcessResult mockResult = ProcessResult.success(event);
              when(mock.process()).thenReturn(mockResult);
            })) {

      // Act
      eventHandler.onEvent(event, 0, false);

      // Assert
      // Kiểm tra xem AmmPoolProcessor có được tạo không
      assertEquals(1, mockedProcessor.constructed().size(), "AmmPoolProcessor should be constructed");
      // Kiểm tra xem process() đã được gọi chưa
      verify(mockedProcessor.constructed().get(0)).process();
      // Kiểm tra EventCache.updateEvent và OutputProcessor.processOutput được gọi
      verify(mockEventCache).updateEvent(event.getEventId());
      verify(mockOutputProcessor).processOutput(any(ProcessResult.class), eq(false));
    }
  }

  @Test
  @DisplayName("onEvent nên xử lý sự kiện BALANCE_RESET thành công")
  void onEvent_ShouldProcessBalanceResetEventSuccessfully() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withAccountEvent();
    event.getAccountEvent().setOperationType(OperationType.BALANCE_RESET);
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Sử dụng try-with-resources để tạo mock cho CoinAccountProcessor
    try (
        MockedConstruction<com.exchangeengine.service.engine.coin_account.CoinAccountProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.coin_account.CoinAccountProcessor.class,
            (mock, context) -> {
              // Đảm bảo mock trả về ProcessResult thành công
              ProcessResult mockResult = ProcessResult.success(event);
              when(mock.process()).thenReturn(mockResult);
            })) {

      // Act
      eventHandler.onEvent(event, 0, false);

      // Assert
      // Kiểm tra xem CoinAccountProcessor có được tạo không
      assertEquals(1, mockedProcessor.constructed().size(), "CoinAccountProcessor should be constructed");
      // Kiểm tra xem process() đã được gọi chưa
      verify(mockedProcessor.constructed().get(0)).process();
      // Kiểm tra EventCache.updateEvent và OutputProcessor.processOutput được gọi
      verify(mockEventCache).updateEvent(event.getEventId());
      verify(mockOutputProcessor).processOutput(any(ProcessResult.class), eq(false));
    }
  }

  @Test
  @DisplayName("onEvent should process MERCHANT_ESCROW event successfully")
  void onEvent_ShouldProcessMerchantEscrowEventSuccessfully() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withMerchantEscrowEvent();
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Use try-with-resources to mock MerchantEscrowProcessor
    try (
        MockedConstruction<com.exchangeengine.service.engine.merchant_escrow.MerchantEscrowProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.merchant_escrow.MerchantEscrowProcessor.class,
            (mock, context) -> {
              // Ensure mock returns successful ProcessResult
              ProcessResult mockResult = ProcessResult.success(event);
              when(mock.process()).thenReturn(mockResult);
            })) {

      // Act
      eventHandler.onEvent(event, 0, false);

      // Assert
      // Check if MerchantEscrowProcessor was constructed
      assertEquals(1, mockedProcessor.constructed().size(), "MerchantEscrowProcessor should be constructed");
      // Check if process() was called
      verify(mockedProcessor.constructed().get(0)).process();
      // Check if EventCache.updateEvent and OutputProcessor.processOutput were called
      verify(mockEventCache).updateEvent(event.getEventId());
      verify(mockOutputProcessor).processOutput(any(ProcessResult.class), eq(false));
    }
  }

  @Test
  @DisplayName("onEvent nên xử lý sự kiện AMM_POSITION_CREATE thành công")
  void onEvent_ShouldProcessAmmPositionEventSuccessfully() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withAmmPositionEvent();
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Sử dụng try-with-resources để tạo mock cho AmmPositionProcessor
    try (
        MockedConstruction<com.exchangeengine.service.engine.amm_position.AmmPositionProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.amm_position.AmmPositionProcessor.class,
            (mock, context) -> {
              // Đảm bảo mock trả về ProcessResult thành công
              ProcessResult mockResult = ProcessResult.success(event);
              when(mock.process()).thenReturn(mockResult);
            })) {

      // Act
      eventHandler.onEvent(event, 0, false);

      // Assert
      // Kiểm tra xem AmmPositionProcessor có được tạo không
      assertEquals(1, mockedProcessor.constructed().size(), "AmmPositionProcessor should be constructed");
      // Kiểm tra xem process() đã được gọi chưa
      verify(mockedProcessor.constructed().get(0)).process();
      // Kiểm tra EventCache.updateEvent và OutputProcessor.processOutput được gọi
      verify(mockEventCache).updateEvent(event.getEventId());
      verify(mockOutputProcessor).processOutput(any(ProcessResult.class), eq(false));
    }
  }

  @Test
  @DisplayName("Constructor should initialize with proper instances")
  void constructor_ShouldInitializeWithProperInstances() throws Exception {
    // Arrange - already done in setUp()

    // Act - create a new instance without mocking the dependencies
    DisruptorEventHandler newHandler = new DisruptorEventHandler();

    // Assert - use reflection to verify the handler got the singleton instances
    Field storageServiceField = DisruptorEventHandler.class.getDeclaredField("storageService");
    storageServiceField.setAccessible(true);
    StorageService actualStorageService = (StorageService) storageServiceField.get(newHandler);

    Field outputProcessorField = DisruptorEventHandler.class.getDeclaredField("outputProcessor");
    outputProcessorField.setAccessible(true);
    OutputProcessor actualOutputProcessor = (OutputProcessor) outputProcessorField.get(newHandler);

    // Verify correct instances were obtained (singletons)
    assertEquals(StorageService.getInstance(), actualStorageService, "StorageService should be properly initialized");
    assertEquals(OutputProcessor.getInstance(), actualOutputProcessor,
        "OutputProcessor should be properly initialized");
  }

  @Test
  @DisplayName("onEvent should handle processor exceptions and create error result")
  void onEvent_ShouldHandleProcessorExceptions() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withCoinDepositEvent();
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    String errorMessage = "Test processor exception";

    // Mock DepositProcessor to throw exception
    try (
        MockedConstruction<com.exchangeengine.service.engine.deposit.DepositProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.deposit.DepositProcessor.class,
            (mock, context) -> {
              // Make process() throw exception
              when(mock.process()).thenThrow(new RuntimeException(errorMessage));
            })) {

      // Act
      eventHandler.onEvent(event, 0, true); // Using endOfBatch=true to test that path

      // Assert
      // Check if process() was called and exception was handled
      verify(mockedProcessor.constructed().get(0)).process();

      // Capture the ProcessResult sent to outputProcessor
      ArgumentCaptor<ProcessResult> resultCaptor = ArgumentCaptor.forClass(ProcessResult.class);
      verify(mockOutputProcessor).processOutput(resultCaptor.capture(), eq(true));

      // Verify the error was properly set
      ProcessResult capturedResult = resultCaptor.getValue();
      assertFalse(capturedResult.getEvent().isSuccess(), "Event should be marked as failed");
      assertEquals(errorMessage, capturedResult.getEvent().getErrorMessage(), "Error message should be set");

      // Verify EventCache was still updated
      verify(mockEventCache).updateEvent(event.getEventId());
    }
  }

  @Test
  @DisplayName("onEvent should handle null result gracefully")
  void onEvent_ShouldHandleNullResult() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withCoinDepositEvent();
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Mock DepositProcessor to return null result (abnormal but possible)
    try (
        MockedConstruction<com.exchangeengine.service.engine.deposit.DepositProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.deposit.DepositProcessor.class,
            (mock, context) -> {
              when(mock.process()).thenReturn(null);
            })) {

      // Act - this would normally throw NPE in finally block if not handled
      eventHandler.onEvent(event, 0, false);

      // Assert
      verify(mockedProcessor.constructed().get(0)).process();
      verify(mockEventCache).updateEvent(event.getEventId());
      verify(mockOutputProcessor).processOutput(isNull(), eq(false));
    }
  }

  @Test
  @DisplayName("onEvent nên xử lý ngoại lệ khi AmmPositionProcessor.process() ném lỗi")
  void onEvent_ShouldHandleExceptionFromAmmPositionProcessor() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withAmmPositionEvent();
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Chuẩn bị một ArgumentCaptor để bắt ProcessResult
    ArgumentCaptor<ProcessResult> resultCaptor = ArgumentCaptor.forClass(ProcessResult.class);

    // Sử dụng try-with-resources để tạo mock cho AmmPositionProcessor
    try (
        MockedConstruction<com.exchangeengine.service.engine.amm_position.AmmPositionProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.amm_position.AmmPositionProcessor.class,
            (mock, context) -> {
              // Đảm bảo mock ném ra ngoại lệ khi process() được gọi
              when(mock.process()).thenThrow(new RuntimeException("Test exception"));
            })) {

      // Act
      eventHandler.onEvent(event, 0, false);

      // Assert
      // Kiểm tra xem process() đã được gọi chưa
      verify(mockedProcessor.constructed().get(0)).process();
      // Kiểm tra ProcessResult đã được gửi tới OutputProcessor
      verify(mockOutputProcessor).processOutput(resultCaptor.capture(), eq(false));
      // Kiểm tra ProcessResult đã được ghi nhận đúng
      ProcessResult capturedResult = resultCaptor.getValue();
      assertFalse(capturedResult.getEvent().isSuccess(),
          "ProcessResult should be unsuccessful when handling exception");
      assertTrue(capturedResult.getEvent().getErrorMessage().contains("Test exception"),
          "Error message should contain the exception message");
    }
  }

  @Test
  @DisplayName("onEvent nên xử lý sự kiện AMM_ORDER_SWAP thành công")
  void onEvent_ShouldProcessAmmOrderEventSuccessfully() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withAmmOrderEvent();
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Sử dụng try-with-resources để tạo mock cho AmmOrderProcessor
    try (
        MockedConstruction<com.exchangeengine.service.engine.amm_order.AmmOrderProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.amm_order.AmmOrderProcessor.class,
            (mock, context) -> {
              // Đảm bảo mock trả về ProcessResult thành công
              ProcessResult mockResult = ProcessResult.success(event);
              when(mock.process()).thenReturn(mockResult);
            })) {

      // Act
      eventHandler.onEvent(event, 0, false);

      // Assert
      // Kiểm tra xem AmmOrderProcessor có được tạo không
      assertEquals(1, mockedProcessor.constructed().size(), "AmmOrderProcessor should be constructed");
      // Kiểm tra xem process() đã được gọi chưa
      verify(mockedProcessor.constructed().get(0)).process();
      // Kiểm tra EventCache.updateEvent và OutputProcessor.processOutput được gọi
      verify(mockEventCache).updateEvent(event.getEventId());
      verify(mockOutputProcessor).processOutput(any(ProcessResult.class), eq(false));
    }
  }

  @Test
  @DisplayName("onEvent nên xử lý ngoại lệ khi AmmOrderProcessor.process() ném lỗi")
  void onEvent_ShouldHandleExceptionFromAmmOrderProcessor() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withAmmOrderEvent();
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Sử dụng try-with-resources để tạo mock cho AmmOrderProcessor
    try (
        MockedConstruction<com.exchangeengine.service.engine.amm_order.AmmOrderProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.amm_order.AmmOrderProcessor.class,
            (mock, context) -> {
              // Đảm bảo mock ném lỗi khi gọi process()
              when(mock.process()).thenThrow(new RuntimeException("Test exception"));
            })) {

      // Act
      eventHandler.onEvent(event, 0, false);

      // Assert
      // Kiểm tra xem AmmOrderProcessor có được tạo không
      assertEquals(1, mockedProcessor.constructed().size(), "AmmOrderProcessor should be constructed");
      // Kiểm tra xem process() đã được gọi chưa
      verify(mockedProcessor.constructed().get(0)).process();

      // Kiểm tra rằng event đã được đặt thành trạng thái lỗi
      ArgumentCaptor<ProcessResult> resultCaptor = ArgumentCaptor.forClass(ProcessResult.class);
      verify(mockOutputProcessor).processOutput(resultCaptor.capture(), eq(false));

      ProcessResult capturedResult = resultCaptor.getValue();
      assertFalse(capturedResult.getEvent().isSuccess());
      assertEquals("Test exception", capturedResult.getEvent().getErrorMessage());

      // Kiểm tra EventCache.updateEvent được gọi
      verify(mockEventCache).updateEvent(event.getEventId());
    }
  }

  @Test
  @DisplayName("onEvent nên xử lý sự kiện TRADE_EVENT thành công")
  void onEvent_ShouldProcessTradeEventSuccessfully() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withTradeEvent();
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Sử dụng try-with-resources để tạo mock cho TradeProcessor
    try (
        MockedConstruction<com.exchangeengine.service.engine.trade.TradeProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.trade.TradeProcessor.class,
            (mock, context) -> {
              // Đảm bảo mock trả về ProcessResult thành công
              ProcessResult mockResult = ProcessResult.success(event);
              when(mock.process()).thenReturn(mockResult);
            })) {

      // Act
      eventHandler.onEvent(event, 0, false);

      // Assert
      // Kiểm tra xem TradeProcessor có được tạo không
      assertEquals(1, mockedProcessor.constructed().size(), "TradeProcessor should be constructed");
      // Kiểm tra xem process() đã được gọi chưa
      verify(mockedProcessor.constructed().get(0)).process();
      // Kiểm tra EventCache.updateEvent và OutputProcessor.processOutput được gọi
      verify(mockEventCache).updateEvent(event.getEventId());
      verify(mockOutputProcessor).processOutput(any(ProcessResult.class), eq(false));
    }
  }

  @Test
  @DisplayName("onEvent nên xử lý sự kiện OFFER_EVENT thành công")
  void onEvent_ShouldProcessOfferEventSuccessfully() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withOfferEvent();
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Sử dụng try-with-resources để tạo mock cho OfferProcessor
    try (
        MockedConstruction<com.exchangeengine.service.engine.offer.OfferProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.offer.OfferProcessor.class,
            (mock, context) -> {
              // Đảm bảo mock trả về ProcessResult thành công
              ProcessResult mockResult = ProcessResult.success(event);
              when(mock.process()).thenReturn(mockResult);
            })) {

      // Act
      eventHandler.onEvent(event, 0, false);

      // Assert
      // Kiểm tra xem OfferProcessor có được tạo không
      assertEquals(1, mockedProcessor.constructed().size(), "OfferProcessor should be constructed");
      // Kiểm tra xem process() đã được gọi chưa
      verify(mockedProcessor.constructed().get(0)).process();
      // Kiểm tra EventCache.updateEvent và OutputProcessor.processOutput được gọi
      verify(mockEventCache).updateEvent(event.getEventId());
      verify(mockOutputProcessor).processOutput(any(ProcessResult.class), eq(false));
    }
  }

  @Test
  @DisplayName("onEvent nên xử lý sự kiện BALANCES_LOCK_CREATE thành công")
  void onEvent_ShouldProcessBalancesLockCreateEventSuccessfully() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withBalancesLockEvent();
    event.getBalancesLockEvent().setOperationType(OperationType.BALANCES_LOCK_CREATE);
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Sử dụng try-with-resources để tạo mock cho BalancesLockProcessor
    try (
        MockedConstruction<com.exchangeengine.service.engine.balances_lock.BalancesLockProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.balances_lock.BalancesLockProcessor.class,
            (mock, context) -> {
              // Đảm bảo mock trả về ProcessResult thành công
              ProcessResult mockResult = ProcessResult.success(event);
              when(mock.process()).thenReturn(mockResult);
            })) {

      // Act
      eventHandler.onEvent(event, 0, false);

      // Assert
      // Kiểm tra xem BalancesLockProcessor có được tạo không
      assertEquals(1, mockedProcessor.constructed().size(), "BalancesLockProcessor should be constructed");
      // Kiểm tra xem process() đã được gọi chưa
      verify(mockedProcessor.constructed().get(0)).process();
      // Kiểm tra EventCache.updateEvent và OutputProcessor.processOutput được gọi
      verify(mockEventCache).updateEvent(event.getEventId());
      verify(mockOutputProcessor).processOutput(any(ProcessResult.class), eq(false));
    }
  }

  @Test
  @DisplayName("onEvent nên xử lý sự kiện BALANCES_LOCK_RELEASE thành công")
  void onEvent_ShouldProcessBalancesLockReleaseEventSuccessfully() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withBalancesLockEvent();
    event.getBalancesLockEvent().setOperationType(OperationType.BALANCES_LOCK_RELEASE);
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Sử dụng try-with-resources để tạo mock cho BalancesLockProcessor
    try (
        MockedConstruction<com.exchangeengine.service.engine.balances_lock.BalancesLockProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.balances_lock.BalancesLockProcessor.class,
            (mock, context) -> {
              // Đảm bảo mock trả về ProcessResult thành công
              ProcessResult mockResult = ProcessResult.success(event);
              when(mock.process()).thenReturn(mockResult);
            })) {

      // Act
      eventHandler.onEvent(event, 0, false);

      // Assert
      // Kiểm tra xem BalancesLockProcessor có được tạo không
      assertEquals(1, mockedProcessor.constructed().size(), "BalancesLockProcessor should be constructed");
      // Kiểm tra xem process() đã được gọi chưa
      verify(mockedProcessor.constructed().get(0)).process();
      // Kiểm tra EventCache.updateEvent và OutputProcessor.processOutput được gọi
      verify(mockEventCache).updateEvent(event.getEventId());
      verify(mockOutputProcessor).processOutput(any(ProcessResult.class), eq(false));
    }
  }

  @Test
  @DisplayName("onEvent nên xử lý ngoại lệ khi BalancesLockProcessor.process() ném lỗi")
  void onEvent_ShouldHandleExceptionFromBalancesLockProcessor() {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withBalancesLockEvent();
    when(mockEventCache.isEventProcessed(event.getEventId())).thenReturn(false);

    // Sử dụng try-with-resources để tạo mock cho BalancesLockProcessor với kịch bản ném lỗi
    try (
        MockedConstruction<com.exchangeengine.service.engine.balances_lock.BalancesLockProcessor> mockedProcessor = mockConstruction(
            com.exchangeengine.service.engine.balances_lock.BalancesLockProcessor.class,
            (mock, context) -> {
              // Đảm bảo mock ném lỗi khi gọi process()
              when(mock.process()).thenThrow(new RuntimeException("Test exception"));
            })) {

      // Act
      eventHandler.onEvent(event, 0, false);

      // Assert
      // Kiểm tra xem BalancesLockProcessor có được tạo không
      assertEquals(1, mockedProcessor.constructed().size(), "BalancesLockProcessor should be constructed");
      // Kiểm tra xem process() đã được gọi chưa
      verify(mockedProcessor.constructed().get(0)).process();
      // Kiểm tra xem EventCache.updateEvent được gọi
      verify(mockEventCache).updateEvent(event.getEventId());
      // Kiểm tra OutputProcessor.processOutput được gọi với result có isSuccess = false
      ArgumentCaptor<ProcessResult> resultCaptor = ArgumentCaptor.forClass(ProcessResult.class);
      verify(mockOutputProcessor).processOutput(resultCaptor.capture(), eq(false));
      assertFalse(resultCaptor.getValue().getEvent().getErrorMessage() == null);
    }
  }
}
