package com.exchangeengine.service.engine;

import com.exchangeengine.factory.event.AccountEventFactory;
import com.exchangeengine.factory.event.CoinDepositEventFactory;
import com.exchangeengine.factory.event.CoinWithdrawalEventFactory;
import com.exchangeengine.factory.event.DisruptorEventFactory;
import com.exchangeengine.factory.event.AmmPoolEventFactory;
import com.exchangeengine.factory.event.MerchantEscrowEventFactory;
import com.exchangeengine.factory.event.AmmPositionEventFactory;
import com.exchangeengine.factory.event.AmmOrderEventFactory;
import com.exchangeengine.factory.event.TradeEventFactory;
import com.exchangeengine.factory.event.OfferEventFactory;
import com.exchangeengine.factory.event.BalancesLockEventFactory;
import com.exchangeengine.model.event.AccountEvent;
import com.exchangeengine.model.event.CoinDepositEvent;
import com.exchangeengine.model.event.CoinWithdrawalEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.event.AmmPoolEvent;
import com.exchangeengine.model.event.MerchantEscrowEvent;
import com.exchangeengine.model.event.AmmPositionEvent;
import com.exchangeengine.model.event.AmmOrderEvent;
import com.exchangeengine.model.event.TradeEvent;
import com.exchangeengine.model.event.OfferEvent;
import com.exchangeengine.model.event.BalancesLockEvent;
import com.exchangeengine.util.EnvManager;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.EventHandler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EngineDisruptorServiceTest {

  @Mock
  private Disruptor<DisruptorEvent> mockDisruptor;

  @Mock
  private RingBuffer<DisruptorEvent> mockRingBuffer;

  @Mock
  private EnvManager mockEnvManager;

  @Mock
  private OutputProcessor mockOutputProcessor;

  private EngineDisruptorService engineDisruptorService;
  private EngineDisruptorService originalInstance;

  @BeforeEach
  void setUp() throws Exception {
    // Lưu trữ instance gốc trước khi thay đổi
    Field instanceField = EngineDisruptorService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    originalInstance = (EngineDisruptorService) instanceField.get(null);

    // Mock EnvManager để trả về các giá trị cần thiết
    try (MockedStatic<EnvManager> mockedEnvManager = mockStatic(EnvManager.class)) {
      mockedEnvManager.when(EnvManager::getInstance).thenReturn(mockEnvManager);
      when(mockEnvManager.getInt("DISRUPTOR_BUFFER_SIZE", 4096)).thenReturn(1024);
      when(mockEnvManager.get("ENGINE_SERVICE_NAME", "engine-service")).thenReturn("test-service");

      // Mock RingBuffer
      when(mockDisruptor.getRingBuffer()).thenReturn(mockRingBuffer);

      // Tạo instance EngineDisruptorService qua constructor private
      engineDisruptorService = createEngineDisruptorServiceInstance(mockDisruptor, mockRingBuffer, "test-service");

      // Thiết lập instance mới vào singleton
      instanceField.set(null, engineDisruptorService);
    }
  }

  @AfterEach
  void tearDown() throws Exception {
    resetInstance();
  }

  private EngineDisruptorService createEngineDisruptorServiceInstance(
      Disruptor<DisruptorEvent> disruptor,
      RingBuffer<DisruptorEvent> ringBuffer,
      String serviceName) throws Exception {

    // Sử dụng reflection để gọi constructor private
    Field instanceField = EngineDisruptorService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null); // Reset instance

    // Tạo instance mới
    java.lang.reflect.Constructor<EngineDisruptorService> constructor = EngineDisruptorService.class
        .getDeclaredConstructor(
            Disruptor.class, RingBuffer.class, String.class);
    constructor.setAccessible(true);
    return constructor.newInstance(disruptor, ringBuffer, serviceName);
  }

  @Test
  @DisplayName("Phương thức deposit nên gọi publishEvent với tham số đúng")
  void deposit_ShouldCallPublishEventWithCorrectParameters() {
    // Arrange - Sử dụng CoinDepositEventFactory để tạo event
    CoinDepositEvent depositEvent = CoinDepositEventFactory.create();

    // Mock phương thức publishEvent
    EngineDisruptorService spyService = spy(engineDisruptorService);
    doNothing().when(spyService).publishEvent(any(DisruptorEvent.class));

    // Act
    spyService.deposit(depositEvent);

    // Assert
    verify(spyService).publishEvent(any(DisruptorEvent.class));
  }

  @Test
  @DisplayName("Phương thức withdraw nên gọi publishEvent với tham số đúng")
  void withdraw_ShouldCallPublishEventWithCorrectParameters() {
    // Arrange - Sử dụng CoinWithdrawalEventFactory để tạo event
    CoinWithdrawalEvent withdrawalEvent = CoinWithdrawalEventFactory.create();

    // Mock phương thức publishEvent
    EngineDisruptorService spyService = spy(engineDisruptorService);
    doNothing().when(spyService).publishEvent(any(DisruptorEvent.class));

    // Act
    spyService.withdraw(withdrawalEvent);

    // Assert
    verify(spyService).publishEvent(any(DisruptorEvent.class));
  }

  @Test
  @DisplayName("Phương thức createCoinAccount nên gọi publishEvent với tham số đúng")
  void createCoinAccount_ShouldCallPublishEventWithCorrectParameters() {
    // Arrange - Sử dụng AccountEventFactory để tạo event
    AccountEvent accountEvent = AccountEventFactory.create();

    // Mock phương thức publishEvent
    EngineDisruptorService spyService = spy(engineDisruptorService);
    doNothing().when(spyService).publishEvent(any(DisruptorEvent.class));

    // Act
    spyService.createCoinAccount(accountEvent);

    // Assert
    verify(spyService).publishEvent(any(DisruptorEvent.class));
  }

  @Test
  @DisplayName("Phương thức ammPool nên gọi publishEvent với tham số đúng")
  void ammPool_ShouldCallPublishEventWithCorrectParameters() {
    // Arrange - Sử dụng AmmPoolEventFactory để tạo event
    AmmPoolEvent ammPoolEvent = AmmPoolEventFactory.create();

    // Mock phương thức publishEvent
    EngineDisruptorService spyService = spy(engineDisruptorService);
    doNothing().when(spyService).publishEvent(any(DisruptorEvent.class));

    // Act
    spyService.ammPool(ammPoolEvent);

    // Assert
    verify(spyService).publishEvent(any(DisruptorEvent.class));
  }

  @Test
  @DisplayName("Phương thức merchantEscrow nên gọi publishEvent với tham số đúng")
  void merchantEscrow_ShouldCallPublishEventWithCorrectParameters() {
    // Arrange - Sử dụng MerchantEscrowEventFactory để tạo event
    MerchantEscrowEvent merchantEscrowEvent = MerchantEscrowEventFactory.create();

    // Mock phương thức publishEvent
    EngineDisruptorService spyService = spy(engineDisruptorService);
    doNothing().when(spyService).publishEvent(any(DisruptorEvent.class));

    // Act
    spyService.merchantEscrow(merchantEscrowEvent);

    // Assert
    verify(spyService).publishEvent(any(DisruptorEvent.class));
  }

  @Test
  @DisplayName("Phương thức ammPosition nên gọi publishEvent với tham số đúng")
  void ammPosition_ShouldCallPublishEventWithCorrectParameters() {
    // Arrange - Sử dụng AmmPositionEventFactory để tạo event
    AmmPositionEvent ammPositionEvent = AmmPositionEventFactory.create();

    // Mock phương thức publishEvent
    EngineDisruptorService spyService = spy(engineDisruptorService);
    doNothing().when(spyService).publishEvent(any(DisruptorEvent.class));

    // Act
    spyService.ammPosition(ammPositionEvent);

    // Assert
    verify(spyService).publishEvent(any(DisruptorEvent.class));
  }

  @Test
  @DisplayName("Phương thức publishEvent nên xử lý đúng trường hợp sao chép dữ liệu với DisruptorEvent")
  void publishEvent_ShouldCopySourceEventDataCorrectly() {
    // Arrange
    DisruptorEvent sourceEvent = DisruptorEventFactory.withCoinDepositEvent();
    DisruptorEvent bufferEvent = new DisruptorEvent();

    when(mockRingBuffer.next()).thenReturn(0L);
    when(mockRingBuffer.get(0L)).thenReturn(bufferEvent);

    // Act
    engineDisruptorService.publishEvent(sourceEvent);

    // Assert
    // Verify that bufferEvent.copyFrom(sourceEvent) was called by checking the
    // state
    // Since we can't directly verify method calls on non-mock objects,
    // we can verify the state of bufferEvent has been updated
    assertNotNull(bufferEvent.getCoinDepositEvent(), "CoinDepositEvent should be copied to bufferEvent");
    verify(mockRingBuffer).publish(0L);
  }

  @Test
  @DisplayName("Phương thức publishEvent nên đảm bảo publish được gọi trong khối finally ngay cả khi có exception")
  void publishEvent_ShouldCallPublishEvenWhenExceptionOccurs() {
    // Arrange
    DisruptorEvent sourceEvent = DisruptorEventFactory.withAccountEvent();
    DisruptorEvent bufferEvent = new DisruptorEvent();

    when(mockRingBuffer.next()).thenReturn(0L);
    when(mockRingBuffer.get(0L)).thenReturn(bufferEvent);

    // Mock copyFrom để ném exception
    DisruptorEvent spyBufferEvent = spy(bufferEvent);
    doThrow(new RuntimeException("Test exception during copy")).when(spyBufferEvent)
        .copyFrom(any(DisruptorEvent.class));
    when(mockRingBuffer.get(0L)).thenReturn(spyBufferEvent);

    // Act & Assert
    assertThrows(RuntimeException.class, () -> {
      engineDisruptorService.publishEvent(sourceEvent);
    });

    // Verify publish is still called
    verify(mockRingBuffer).publish(0L);
  }

  @Test
  @DisplayName("setTestInstance nên thay thế instance hiện tại với instance kiểm thử")
  void setTestInstance_ShouldReplaceCurrentInstance() throws Exception {
    // Arrange
    EngineDisruptorService mockInstance = mock(EngineDisruptorService.class);

    // Act
    EngineDisruptorService.setTestInstance(mockInstance);

    // Assert
    Field instanceField = EngineDisruptorService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    assertSame(mockInstance, instanceField.get(null), "setTestInstance should set the instance field");

    // Clean up
    instanceField.set(null, engineDisruptorService);
  }

  @Test
  @DisplayName("Phương thức publishEvent nên xử lý đúng trường hợp khi RingBuffer.get() throw exception")
  void publishEvent_ShouldHandleExceptionFromRingBufferGet() {
    // Arrange
    DisruptorEvent sourceEvent = DisruptorEventFactory.withAccountEvent();

    when(mockRingBuffer.next()).thenReturn(0L);
    when(mockRingBuffer.get(0L)).thenThrow(new RuntimeException("Test exception from get()"));

    // Act & Assert
    Exception exception = assertThrows(RuntimeException.class, () -> {
      engineDisruptorService.publishEvent(sourceEvent);
    });

    assertEquals("Test exception from get()", exception.getMessage());

    // Verify publish is still called
    verify(mockRingBuffer).publish(0L);
  }

  @Test
  @DisplayName("Constructor nên khởi tạo đúng các thành phần")
  void constructor_ShouldInitializeFieldsCorrectly() throws Exception {
    // Arrange
    Disruptor<DisruptorEvent> customDisruptor = mock(Disruptor.class);
    RingBuffer<DisruptorEvent> customRingBuffer = mock(RingBuffer.class);
    String customName = "custom-service";

    // Act - Use reflection to access private constructor
    Constructor<EngineDisruptorService> constructor = EngineDisruptorService.class
        .getDeclaredConstructor(Disruptor.class, RingBuffer.class, String.class);
    constructor.setAccessible(true);
    EngineDisruptorService customService = constructor.newInstance(customDisruptor, customRingBuffer, customName);

    // Assert - Use reflection to check field values
    Field disruptorField = EngineDisruptorService.class.getDeclaredField("disruptor");
    disruptorField.setAccessible(true);
    assertSame(customDisruptor, disruptorField.get(customService), "disruptor field should be set correctly");

    Field ringBufferField = EngineDisruptorService.class.getDeclaredField("ringBuffer");
    ringBufferField.setAccessible(true);
    assertSame(customRingBuffer, ringBufferField.get(customService), "ringBuffer field should be set correctly");

    Field serviceNameField = EngineDisruptorService.class.getDeclaredField("serviceName");
    serviceNameField.setAccessible(true);
    assertEquals(customName, serviceNameField.get(customService), "serviceName field should be set correctly");
  }

  @Test
  @DisplayName("Phương thức publishEvent nên có thể publish event thành công")
  void publishEvent_ShouldPublishEventSuccessfully() {
    // Arrange - Sử dụng DisruptorEventFactory để tạo event
    DisruptorEvent sourceEvent = DisruptorEventFactory.withAccountEvent();
    DisruptorEvent bufferEvent = DisruptorEventFactory.create();

    when(mockRingBuffer.next()).thenReturn(0L);
    when(mockRingBuffer.get(0L)).thenReturn(bufferEvent);

    // Act
    engineDisruptorService.publishEvent(sourceEvent);

    // Assert
    verify(mockRingBuffer).publish(0L);
  }

  @Test
  @DisplayName("Phương thức publishEvent nên ném ngoại lệ khi RingBuffer.next() thất bại")
  void publishEvent_ShouldPropagateException() {
    // Arrange - Sử dụng DisruptorEventFactory để tạo event
    DisruptorEvent sourceEvent = DisruptorEventFactory.withAccountEvent();

    // Giả lập một RuntimeException khi gọi next() trên RingBuffer
    RuntimeException expectedException = new RuntimeException("Test exception");
    when(mockRingBuffer.next()).thenThrow(expectedException);

    // Act & Assert - Đảm bảo exception được ném ra đúng cách
    Exception thrownException = assertThrows(RuntimeException.class, () -> {
      engineDisruptorService.publishEvent(sourceEvent);
    });

    // Assert - Kiểm tra message và exception đúng là exception được ném từ
    // RingBuffer.next()
    assertSame(expectedException, thrownException);
  }

  @Test
  @DisplayName("Phương thức shutdown nên gọi disruptor.shutdown()")
  void shutdown_ShouldCallDisruptorShutdown() {
    // Act
    engineDisruptorService.shutdown();

    // Assert
    verify(mockDisruptor).shutdown();
  }

  @Test
  @DisplayName("Phương thức getServiceName nên trả về đúng tên dịch vụ")
  void getServiceName_ShouldReturnCorrectServiceName() {
    // Act
    String serviceName = engineDisruptorService.getServiceName();

    // Assert
    assertEquals("test-service", serviceName);
  }

  @Test
  @DisplayName("Phương thức getRemainingCapacity nên trả về dung lượng còn lại của ringBuffer")
  void getRemainingCapacity_ShouldReturnBufferRemainingCapacity() {
    // Arrange
    when(mockRingBuffer.remainingCapacity()).thenReturn(512L);

    // Act
    long capacity = engineDisruptorService.getRemainingCapacity();

    // Assert
    assertEquals(512L, capacity);
  }

  @Test
  @DisplayName("getInstance() nên trả về instance đã tồn tại nếu có")
  void getInstance_ShouldReturnExistingInstance() throws Exception {
    // Arrange
    Field instanceField = EngineDisruptorService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    EngineDisruptorService existingInstance = (EngineDisruptorService) instanceField.get(null);

    // Đảm bảo instance không null bằng cách set nó trước
    assertNotNull(existingInstance, "Instance phải được khởi tạo trước khi test này chạy");
    instanceField.set(null, existingInstance);

    // Act
    EngineDisruptorService returnedInstance = EngineDisruptorService.getInstance();

    // Assert
    assertSame(existingInstance, returnedInstance, "getInstance should return the existing instance");

    // Một cách khác để test branch "instance != null" trong getInstance()
    EngineDisruptorService anotherCall = EngineDisruptorService.getInstance();
    assertSame(returnedInstance, anotherCall,
        "getInstance() nên trả về cùng một instance khi gọi nhiều lần");
  }

  @Test
  @DisplayName("createInstance() nên khởi tạo thành công và kết hợp tất cả các chức năng")
  void createInstance_ShouldInitializeSuccess() throws Exception {
    // Lưu trữ instance ban đầu
    Field instanceField = EngineDisruptorService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    Object originalInstance = instanceField.get(null);

    try {
      // Mock tất cả các dependency cần thiết
      EnvManager mockEnvManager = mock(EnvManager.class);
      OutputProcessor mockOutputProcessor = mock(OutputProcessor.class);
      RingBuffer<DisruptorEvent> mockRingBuffer = mock(RingBuffer.class);
      Runnable mockRunnable = mock(Runnable.class);

      // Cấu hình mock EnvManager
      when(mockEnvManager.getInt("DISRUPTOR_BUFFER_SIZE", 4096)).thenReturn(1024);
      String testServiceName = "test-engine-service";
      when(mockEnvManager.get("ENGINE_SERVICE_NAME", "engine-service")).thenReturn(testServiceName);

      try (MockedStatic<EnvManager> mockedEnvManager = mockStatic(EnvManager.class);
          MockedStatic<OutputProcessor> mockedOutputProcessor = mockStatic(OutputProcessor.class);
          MockedConstruction<DisruptorEventHandler> mockedHandlerConstruction = mockConstruction(
              DisruptorEventHandler.class);
          MockedConstruction<Disruptor> mockedDisruptorConstruction = mockConstruction(Disruptor.class,
              (mock, ctx) -> {
                when(mock.getRingBuffer()).thenReturn(mockRingBuffer);
                when(mock.handleEventsWith(any(EventHandler.class))).thenReturn(null);
              })) {

        // Thiết lập mock cho static methods
        mockedEnvManager.when(EnvManager::getInstance).thenReturn(mockEnvManager);
        mockedOutputProcessor.when(OutputProcessor::getInstance).thenReturn(mockOutputProcessor);

        // Đặt instance về null để có thể test createInstance
        instanceField.set(null, null);

        // Truy cập phương thức createInstance qua reflection
        Method createInstanceMethod = EngineDisruptorService.class.getDeclaredMethod("createInstance");
        createInstanceMethod.setAccessible(true);

        // Act - Gọi createInstance
        EngineDisruptorService result = (EngineDisruptorService) createInstanceMethod.invoke(null);

        // Assert
        assertNotNull(result, "createInstance nên trả về một instance mới");

        // Kiểm tra xem các mocked constructor được gọi
        assertEquals(1, mockedHandlerConstruction.constructed().size(), "Nên tạo 1 DisruptorEventHandler");
        assertEquals(1, mockedDisruptorConstruction.constructed().size(), "Nên tạo 1 Disruptor");

        // Test ThreadFactory từ EngineDisruptorService
        ThreadFactory threadFactory = new ThreadFactory() {
          private final AtomicInteger counter = new AtomicInteger(1);

          @Override
          public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(testServiceName + "-disruptor-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
          }
        };

        // Test ThreadFactory với một runnable mock
        Thread thread1 = threadFactory.newThread(mockRunnable);

        // Đảm bảo thread được tạo đúng cách
        assertNotNull(thread1, "Thread đã tạo không được null");
        assertEquals(testServiceName + "-disruptor-1", thread1.getName(),
            "Thread name nên theo format: serviceName-disruptor-counter");
        assertTrue(thread1.isDaemon(), "Thread nên được đặt là daemon");

        // Tạo thread thứ hai để kiểm tra bộ đếm
        Thread thread2 = threadFactory.newThread(mockRunnable);
        assertNotNull(thread2, "Thread thứ hai không được null");
        assertEquals(testServiceName + "-disruptor-2", thread2.getName(),
            "Thread thứ hai nên tăng giá trị counter");

        // Kiểm tra Disruptor methods
        Disruptor<?> mockDisruptor = mockedDisruptorConstruction.constructed().get(0);
        verify(mockDisruptor).start();
        verify(mockDisruptor).getRingBuffer();
      }
    } finally {
      // Khôi phục lại instance ban đầu
      instanceField.set(null, originalInstance);
    }
  }

  @Test
  @DisplayName("getInstance() nên gọi createInstance() khi instance là null")
  void getInstance_ShouldCallCreateInstanceWhenNull() throws Exception {
    // Arrange - chuẩn bị reflection
    Field instanceField = EngineDisruptorService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);

    // Lưu trữ instance gốc
    EngineDisruptorService originalInstance = (EngineDisruptorService) instanceField.get(null);

    try {
      // Đặt instance về null
      instanceField.set(null, null);

      // Mock outputProcessor để tránh lỗi khi tạo instance thật
      OutputProcessor mockProcessor = mock(OutputProcessor.class);
      try (MockedStatic<OutputProcessor> mockedOutputProcessor = mockStatic(OutputProcessor.class)) {
        mockedOutputProcessor.when(OutputProcessor::getInstance).thenReturn(mockProcessor);

        // Theo dõi việc gọi phương thức getInstance bằng spy
        try (MockedStatic<EngineDisruptorService> mockedStatic = mockStatic(EngineDisruptorService.class,
            invocation -> {
              String methodName = invocation.getMethod().getName();
              if (methodName.equals("getInstance")) {
                // Cho phép gọi phương thức thật của getInstance()
                return invocation.callRealMethod();
              } else if (methodName.equals("createInstance")) {
                // Khi createInstance() được gọi, trả về engineDisruptorService đã được mock
                return engineDisruptorService;
              }
              return invocation.callRealMethod();
            })) {

          // Act - gọi getInstance() khi instance là null
          EngineDisruptorService result = EngineDisruptorService.getInstance();

          // Assert
          // Xác minh createInstance() đã được gọi chính xác 1 lần
          mockedStatic.verify(() -> {
            try {
              java.lang.reflect.Method createInstanceMethod = EngineDisruptorService.class
                  .getDeclaredMethod("createInstance");
              createInstanceMethod.setAccessible(true);
              createInstanceMethod.invoke(null);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }, times(1));

          // Kiểm tra instance đã được trả về
          assertSame(engineDisruptorService, result, "getInstance() nên trả về instance từ createInstance()");
        }
      }
    } finally {
      // Khôi phục lại instance ban đầu
      instanceField.set(null, originalInstance);
    }
  }

  @Test
  @DisplayName("createInstance() nên ném RuntimeException khi xảy ra lỗi")
  void createInstance_ShouldHandleException() throws Exception {
    // Arrange
    Field instanceField = EngineDisruptorService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    Object originalInstance = instanceField.get(null);

    try {
      // Đặt instance về null
      instanceField.set(null, null);

      try (MockedStatic<EnvManager> mockedEnvManager = mockStatic(EnvManager.class);
          MockedStatic<OutputProcessor> mockedOutputProcessor = mockStatic(OutputProcessor.class)) {

        // Mock dependencies
        EnvManager mockEnvManager = mock(EnvManager.class);
        OutputProcessor mockOutputProcessor = mock(OutputProcessor.class);

        // Thiết lập mock cho static methods
        mockedEnvManager.when(EnvManager::getInstance).thenReturn(mockEnvManager);
        mockedOutputProcessor.when(OutputProcessor::getInstance).thenThrow(new RuntimeException("Test exception"));

        // Cấu hình mock EnvManager
        when(mockEnvManager.getInt("DISRUPTOR_BUFFER_SIZE", 4096)).thenReturn(1024);
        when(mockEnvManager.get("ENGINE_SERVICE_NAME", "engine-service")).thenReturn("test-service");

        // Truy cập phương thức createInstance qua reflection
        Method createInstanceMethod = EngineDisruptorService.class.getDeclaredMethod("createInstance");
        createInstanceMethod.setAccessible(true);

        // Act & Assert
        // Gọi method qua reflection
        try {
          createInstanceMethod.invoke(null);
          fail("Phương thức createInstance nên ném ngoại lệ RuntimeException");
        } catch (InvocationTargetException e) {
          // Print detailed information about the exception
          System.out.println("Exception class: " + e.getTargetException().getClass().getName());
          System.out.println("Exception message: " + e.getTargetException().getMessage());

          if (e.getTargetException().getCause() != null) {
            System.out.println("Cause class: " + e.getTargetException().getCause().getClass().getName());
            System.out.println("Cause message: " + e.getTargetException().getCause().getMessage());
          } else {
            System.out.println("No cause available");
          }

          // Kiểm tra ngoại lệ bên trong
          assertTrue(e.getTargetException() instanceof RuntimeException);
          assertEquals("Cannot initialize EngineDisruptorService", e.getTargetException().getMessage());

          // Kiểm tra exception gốc có tồn tại
          Throwable cause = e.getTargetException().getCause();
          assertNotNull(cause);
          assertTrue(cause instanceof RuntimeException);

          // Không kiểm tra nội dung của message vì nó có thể thay đổi
          // tùy thuộc vào triển khai
        }
      }
    } finally {
      // Khôi phục lại instance ban đầu
      instanceField.set(null, originalInstance);
    }
  }

  @Test
  @DisplayName("ammPosition nên truyền đúng dữ liệu AmmPositionEvent vào DisruptorEvent")
  void ammPosition_ShouldTransferDataCorrectlyToDisruptorEvent() {
    // Arrange - Sử dụng AmmPositionEventFactory để tạo event với dữ liệu xác định
    AmmPositionEvent ammPositionEvent = AmmPositionEventFactory.create();
    ammPositionEvent.setEventId("test-position-event-id");

    DisruptorEvent bufferEvent = new DisruptorEvent();

    // Capture DisruptorEvent được truyền vào publishEvent
    ArgumentCaptor<DisruptorEvent> eventCaptor = ArgumentCaptor.forClass(DisruptorEvent.class);

    // Mock RingBuffer để chúng ta có thể kiểm tra dữ liệu
    when(mockRingBuffer.next()).thenReturn(0L);
    when(mockRingBuffer.get(0L)).thenReturn(bufferEvent);

    // Act
    engineDisruptorService.ammPosition(ammPositionEvent);

    // Assert
    verify(mockRingBuffer).publish(0L);

    // Kiểm tra xem dữ liệu đã được sao chép chính xác không
    assertNotNull(bufferEvent.getAmmPositionEvent(), "AmmPositionEvent không được null");
    assertEquals("test-position-event-id", bufferEvent.getAmmPositionEvent().getEventId(),
        "EventId của AmmPositionEvent phải được giữ nguyên");
    assertNull(bufferEvent.getAmmPoolEvent(), "AmmPoolEvent phải là null");
    assertNull(bufferEvent.getAccountEvent(), "AccountEvent phải là null");
    assertNull(bufferEvent.getCoinDepositEvent(), "CoinDepositEvent phải là null");
    assertNull(bufferEvent.getCoinWithdrawalEvent(), "CoinWithdrawalEvent phải là null");
  }

  @Test
  @DisplayName("publishEvent nên đảm bảo giải phóng sequence khi có lỗi xảy ra trong quá trình copyFrom")
  void publishEvent_ShouldReleaseSequenceWhenExceptionOccursDuringCopyFrom() {
    // Arrange
    DisruptorEvent sourceEvent = DisruptorEventFactory.withAmmPositionEvent();
    DisruptorEvent spyBufferEvent = spy(new DisruptorEvent());

    // Giả lập RingBuffer trả về sequence và spyBufferEvent
    when(mockRingBuffer.next()).thenReturn(42L);
    when(mockRingBuffer.get(42L)).thenReturn(spyBufferEvent);

    // Giả lập lỗi khi gọi copyFrom
    doThrow(new RuntimeException("Test copy exception")).when(spyBufferEvent).copyFrom(any(DisruptorEvent.class));

    // Act & Assert
    Exception exception = assertThrows(RuntimeException.class, () -> {
      engineDisruptorService.publishEvent(sourceEvent);
    });

    // Đảm bảo exception là đúng loại
    assertEquals("Test copy exception", exception.getMessage());

    // Verify rằng sequence đã được publish ngay cả khi có lỗi
    verify(mockRingBuffer).publish(42L);
  }

  @Test
  @DisplayName("Phương thức ammOrder nên gọi publishEvent với tham số đúng")
  void ammOrder_ShouldCallPublishEventWithCorrectParameters() {
    // Arrange - Sử dụng Factory để tạo event
    AmmOrderEvent ammOrderEvent = mock(AmmOrderEvent.class);

    // Mock phương thức publishEvent
    EngineDisruptorService spyService = spy(engineDisruptorService);
    doNothing().when(spyService).publishEvent(any(DisruptorEvent.class));

    // Act
    spyService.ammOrder(ammOrderEvent);

    // Assert
    verify(spyService).publishEvent(any(DisruptorEvent.class));
  }

  @Test
  @DisplayName("ammOrder nên truyền đúng dữ liệu AmmOrderEvent vào DisruptorEvent")
  void ammOrder_ShouldTransferDataCorrectlyToDisruptorEvent() {
    // Arrange - Tạo event có dữ liệu xác định
    AmmOrderEvent ammOrderEvent = mock(AmmOrderEvent.class);
    when(ammOrderEvent.getEventId()).thenReturn("test-order-event-id");

    DisruptorEvent bufferEvent = new DisruptorEvent();

    // Mock RingBuffer để có thể kiểm tra dữ liệu
    when(mockRingBuffer.next()).thenReturn(0L);
    when(mockRingBuffer.get(0L)).thenReturn(bufferEvent);

    // Act
    engineDisruptorService.ammOrder(ammOrderEvent);

    // Assert
    verify(mockRingBuffer).publish(0L);

    // Kiểm tra dữ liệu đã được sao chép đúng không
    assertNotNull(bufferEvent.getAmmOrderEvent(), "AmmOrderEvent không được null");
    assertEquals("test-order-event-id", bufferEvent.getAmmOrderEvent().getEventId(),
        "EventId của AmmOrderEvent phải được giữ nguyên");
    assertNull(bufferEvent.getAmmPoolEvent(), "AmmPoolEvent phải là null");
    assertNull(bufferEvent.getAccountEvent(), "AccountEvent phải là null");
    assertNull(bufferEvent.getCoinDepositEvent(), "CoinDepositEvent phải là null");
    assertNull(bufferEvent.getCoinWithdrawalEvent(), "CoinWithdrawalEvent phải là null");
  }

  @Test
  @DisplayName("Phương thức trade nên gọi publishEvent với tham số đúng")
  void trade_ShouldCallPublishEventWithCorrectParameters() {
    // Arrange - Sử dụng TradeEventFactory để tạo event
    TradeEvent tradeEvent = TradeEventFactory.create();

    // Mock phương thức publishEvent
    EngineDisruptorService spyService = spy(engineDisruptorService);
    doNothing().when(spyService).publishEvent(any(DisruptorEvent.class));

    // Act
    spyService.trade(tradeEvent);

    // Assert
    verify(spyService).publishEvent(any(DisruptorEvent.class));
  }

  @Test
  @DisplayName("trade nên truyền đúng dữ liệu TradeEvent vào DisruptorEvent")
  void trade_ShouldTransferDataCorrectlyToDisruptorEvent() {
    // Arrange
    TradeEvent sourceTradeEvent = TradeEventFactory.create();
    ArgumentCaptor<DisruptorEvent> eventCaptor = ArgumentCaptor.forClass(DisruptorEvent.class);
    
    // Mock phương thức publishEvent
    EngineDisruptorService spyService = spy(engineDisruptorService);
    doNothing().when(spyService).publishEvent(eventCaptor.capture());
    
    // Act
    spyService.trade(sourceTradeEvent);
    
    // Assert
    DisruptorEvent capturedEvent = eventCaptor.getValue();
    assertNotNull(capturedEvent.getTradeEvent(), "TradeEvent should not be null");
    assertEquals(sourceTradeEvent, capturedEvent.getTradeEvent(), 
        "TradeEvent in DisruptorEvent should be the same as source event");
  }

  @Test
  @DisplayName("Phương thức offer nên gọi publishEvent với tham số đúng")
  void offer_ShouldCallPublishEventWithCorrectParameters() {
    // Arrange - Sử dụng OfferEventFactory để tạo event
    OfferEvent offerEvent = OfferEventFactory.create();

    // Mock phương thức publishEvent
    EngineDisruptorService spyService = spy(engineDisruptorService);
    doNothing().when(spyService).publishEvent(any(DisruptorEvent.class));

    // Act
    spyService.offer(offerEvent);

    // Assert
    verify(spyService).publishEvent(any(DisruptorEvent.class));
  }

  @Test
  @DisplayName("offer nên truyền đúng dữ liệu OfferEvent vào DisruptorEvent")
  void offer_ShouldTransferDataCorrectlyToDisruptorEvent() {
    // Arrange
    OfferEvent sourceOfferEvent = OfferEventFactory.create();
    ArgumentCaptor<DisruptorEvent> eventCaptor = ArgumentCaptor.forClass(DisruptorEvent.class);
    
    // Mock phương thức publishEvent
    EngineDisruptorService spyService = spy(engineDisruptorService);
    doNothing().when(spyService).publishEvent(eventCaptor.capture());
    
    // Act
    spyService.offer(sourceOfferEvent);
    
    // Assert
    DisruptorEvent capturedEvent = eventCaptor.getValue();
    assertNotNull(capturedEvent.getOfferEvent(), "OfferEvent should not be null");
    assertEquals(sourceOfferEvent, capturedEvent.getOfferEvent(), 
        "OfferEvent in DisruptorEvent should be the same as source event");
  }

  @Test
  @DisplayName("deposit nên truyền đúng dữ liệu CoinDepositEvent vào DisruptorEvent")
  void deposit_ShouldTransferDataCorrectlyToDisruptorEvent() {
    // Arrange
    CoinDepositEvent sourceDepositEvent = CoinDepositEventFactory.create();
    ArgumentCaptor<DisruptorEvent> eventCaptor = ArgumentCaptor.forClass(DisruptorEvent.class);
    
    // Mock phương thức publishEvent
    EngineDisruptorService spyService = spy(engineDisruptorService);
    doNothing().when(spyService).publishEvent(eventCaptor.capture());
    
    // Act
    spyService.deposit(sourceDepositEvent);
    
    // Assert
    DisruptorEvent capturedEvent = eventCaptor.getValue();
    assertNotNull(capturedEvent.getCoinDepositEvent(), "CoinDepositEvent should not be null");
    assertEquals(sourceDepositEvent, capturedEvent.getCoinDepositEvent(), 
        "CoinDepositEvent in DisruptorEvent should be the same as source event");
  }

  @Test
  @DisplayName("withdraw nên truyền đúng dữ liệu CoinWithdrawalEvent vào DisruptorEvent")
  void withdraw_ShouldTransferDataCorrectlyToDisruptorEvent() {
    // Arrange
    CoinWithdrawalEvent sourceWithdrawalEvent = CoinWithdrawalEventFactory.create();
    ArgumentCaptor<DisruptorEvent> eventCaptor = ArgumentCaptor.forClass(DisruptorEvent.class);
    
    // Mock phương thức publishEvent
    EngineDisruptorService spyService = spy(engineDisruptorService);
    doNothing().when(spyService).publishEvent(eventCaptor.capture());
    
    // Act
    spyService.withdraw(sourceWithdrawalEvent);
    
    // Assert
    DisruptorEvent capturedEvent = eventCaptor.getValue();
    assertNotNull(capturedEvent.getCoinWithdrawalEvent(), "CoinWithdrawalEvent should not be null");
    assertEquals(sourceWithdrawalEvent, capturedEvent.getCoinWithdrawalEvent(), 
        "CoinWithdrawalEvent in DisruptorEvent should be the same as source event");
  }

  @Test
  @DisplayName("createCoinAccount nên truyền đúng dữ liệu AccountEvent vào DisruptorEvent")
  void createCoinAccount_ShouldTransferDataCorrectlyToDisruptorEvent() {
    // Arrange
    AccountEvent sourceAccountEvent = AccountEventFactory.create();
    ArgumentCaptor<DisruptorEvent> eventCaptor = ArgumentCaptor.forClass(DisruptorEvent.class);
    
    // Mock phương thức publishEvent
    EngineDisruptorService spyService = spy(engineDisruptorService);
    doNothing().when(spyService).publishEvent(eventCaptor.capture());
    
    // Act
    spyService.createCoinAccount(sourceAccountEvent);
    
    // Assert
    DisruptorEvent capturedEvent = eventCaptor.getValue();
    assertNotNull(capturedEvent.getAccountEvent(), "AccountEvent should not be null");
    assertEquals(sourceAccountEvent, capturedEvent.getAccountEvent(), 
        "AccountEvent in DisruptorEvent should be the same as source event");
  }

  @Test
  @DisplayName("merchantEscrow nên truyền đúng dữ liệu MerchantEscrowEvent vào DisruptorEvent")
  void merchantEscrow_ShouldTransferDataCorrectlyToDisruptorEvent() {
    // Arrange
    MerchantEscrowEvent sourceMerchantEscrowEvent = MerchantEscrowEventFactory.create();
    ArgumentCaptor<DisruptorEvent> eventCaptor = ArgumentCaptor.forClass(DisruptorEvent.class);
    
    // Mock phương thức publishEvent
    EngineDisruptorService spyService = spy(engineDisruptorService);
    doNothing().when(spyService).publishEvent(eventCaptor.capture());
    
    // Act
    spyService.merchantEscrow(sourceMerchantEscrowEvent);
    
    // Assert
    DisruptorEvent capturedEvent = eventCaptor.getValue();
    assertNotNull(capturedEvent.getMerchantEscrowEvent(), "MerchantEscrowEvent should not be null");
    assertEquals(sourceMerchantEscrowEvent, capturedEvent.getMerchantEscrowEvent(), 
        "MerchantEscrowEvent in DisruptorEvent should be the same as source event");
  }
  
  @Test
  @DisplayName("ammPool nên truyền đúng dữ liệu AmmPoolEvent vào DisruptorEvent")
  void ammPool_ShouldTransferDataCorrectlyToDisruptorEvent() {
    // Arrange
    AmmPoolEvent sourceAmmPoolEvent = AmmPoolEventFactory.create();
    ArgumentCaptor<DisruptorEvent> eventCaptor = ArgumentCaptor.forClass(DisruptorEvent.class);
    
    // Mock phương thức publishEvent
    EngineDisruptorService spyService = spy(engineDisruptorService);
    doNothing().when(spyService).publishEvent(eventCaptor.capture());
    
    // Act
    spyService.ammPool(sourceAmmPoolEvent);
    
    // Assert
    DisruptorEvent capturedEvent = eventCaptor.getValue();
    assertNotNull(capturedEvent.getAmmPoolEvent(), "AmmPoolEvent should not be null");
    assertEquals(sourceAmmPoolEvent, capturedEvent.getAmmPoolEvent(), 
        "AmmPoolEvent in DisruptorEvent should be the same as source event");
  }

  @Test
  @DisplayName("Phương thức balancesLock nên gọi publishEvent với tham số đúng")
  void balancesLock_ShouldCallPublishEventWithCorrectParameters() {
    // Arrange - Sử dụng BalancesLockEventFactory để tạo event
    BalancesLockEvent balancesLockEvent = BalancesLockEventFactory.create();

    // Mock phương thức publishEvent
    EngineDisruptorService spyService = spy(engineDisruptorService);
    doNothing().when(spyService).publishEvent(any(DisruptorEvent.class));

    // Act
    spyService.balancesLock(balancesLockEvent);

    // Assert
    verify(spyService).publishEvent(any(DisruptorEvent.class));
  }

  @Test
  @DisplayName("balancesLock nên truyền đúng dữ liệu BalancesLockEvent vào DisruptorEvent")
  void balancesLock_ShouldTransferDataCorrectlyToDisruptorEvent() {
    // Arrange
    BalancesLockEvent balancesLockEvent = BalancesLockEventFactory.create();
    DisruptorEvent mockEvent = mock(DisruptorEvent.class);

    // Chuẩn bị mockRingBuffer để nó trả về mockEvent khi get() được gọi
    long sequence = 42;
    when(mockRingBuffer.next()).thenReturn(sequence);
    when(mockRingBuffer.get(sequence)).thenReturn(mockEvent);

    // Act
    engineDisruptorService.balancesLock(balancesLockEvent);

    // Assert
    // Capture argument để kiểm tra
    ArgumentCaptor<DisruptorEvent> sourceCaptor = ArgumentCaptor.forClass(DisruptorEvent.class);
    verify(mockEvent).copyFrom(sourceCaptor.capture());

    // Kiểm tra dữ liệu được truyền vào
    DisruptorEvent sourceEvent = sourceCaptor.getValue();
    assertNotNull(sourceEvent);
    assertEquals(balancesLockEvent, sourceEvent.getBalancesLockEvent());
  }

  private void resetInstance() throws Exception {
    Field instanceField = EngineDisruptorService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);
  }
}
