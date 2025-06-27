package com.exchangeengine.bootstrap;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.exchangeengine.messaging.consumer.KafkaConsumerQueryService;
import com.exchangeengine.messaging.consumer.KafkaConsumerService;
import com.exchangeengine.messaging.producer.KafkaProducerService;
import com.exchangeengine.service.engine.EngineHandler;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EngineServiceMainTest {

  @Mock
  private EngineHandler mockEngineHandler;

  @Mock
  private KafkaProducerService mockProducerService;

  @Mock
  private KafkaConsumerService mockConsumerService;

  @Mock
  private KafkaConsumerQueryService mockQueryConsumerService;

  @Mock
  private ExecutorService mockLogicExecutorService;

  @Mock
  private ExecutorService mockQueryExecutorService;

  @BeforeEach
  void setUp() throws Exception {
    // Reset isRunning flag
    Field isRunningField = EngineServiceMain.class.getDeclaredField("isRunning");
    isRunningField.setAccessible(true);
    isRunningField.set(null, false);

    // Reset các service
    resetServiceFields();
  }

  private void resetServiceFields() throws Exception {
    Field engineHandlerField = EngineServiceMain.class.getDeclaredField("engineHandler");
    engineHandlerField.setAccessible(true);
    engineHandlerField.set(null, null);

    Field producerServiceField = EngineServiceMain.class.getDeclaredField("producerService");
    producerServiceField.setAccessible(true);
    producerServiceField.set(null, null);

    Field consumerServiceField = EngineServiceMain.class.getDeclaredField("consumerService");
    consumerServiceField.setAccessible(true);
    consumerServiceField.set(null, null);

    Field queryConsumerServiceField = EngineServiceMain.class.getDeclaredField("queryConsumerService");
    queryConsumerServiceField.setAccessible(true);
    queryConsumerServiceField.set(null, null);

    Field logicExecutorServiceField = EngineServiceMain.class.getDeclaredField("logicExecutorService");
    logicExecutorServiceField.setAccessible(true);
    logicExecutorServiceField.set(null, null);

    Field queryExecutorServiceField = EngineServiceMain.class.getDeclaredField("queryExecutorService");
    queryExecutorServiceField.setAccessible(true);
    queryExecutorServiceField.set(null, null);
  }

  @Test
  @DisplayName("Start method should initialize all components successfully")
  void start_ShouldInitializeAllComponents() throws Exception {
    try (
        MockedStatic<EngineHandler> engineHandlerMocked = mockStatic(EngineHandler.class);
        MockedStatic<KafkaProducerService> producerServiceMocked = mockStatic(KafkaProducerService.class);
        MockedStatic<KafkaConsumerService> consumerServiceMocked = mockStatic(KafkaConsumerService.class);
        MockedStatic<KafkaConsumerQueryService> queryConsumerServiceMocked = mockStatic(
            KafkaConsumerQueryService.class);
        MockedStatic<ServiceInitializer> serviceInitializerMocked = mockStatic(ServiceInitializer.class);
        MockedStatic<java.util.concurrent.Executors> executorsMocked = mockStatic(
            java.util.concurrent.Executors.class)) {
      // Mock các dependency
      engineHandlerMocked.when(EngineHandler::getInstance).thenReturn(mockEngineHandler);
      producerServiceMocked.when(KafkaProducerService::getInstance).thenReturn(mockProducerService);
      consumerServiceMocked.when(KafkaConsumerService::getInstance).thenReturn(mockConsumerService);
      queryConsumerServiceMocked.when(KafkaConsumerQueryService::getInstance).thenReturn(mockQueryConsumerService);

      // Mock thread pools
      executorsMocked.when(() -> java.util.concurrent.Executors.newSingleThreadExecutor())
          .thenReturn(mockLogicExecutorService)
          .thenReturn(mockQueryExecutorService);

      // Mock query threads count
      when(mockQueryConsumerService.getQueryThreadsCount()).thenReturn(2);

      // Act
      EngineServiceMain.start();

      // Kiểm tra ServiceInitializer.createTopics được gọi
      serviceInitializerMocked.verify(() -> ServiceInitializer.createTopics());

      // Verify executors được submit
      verify(mockLogicExecutorService).submit(mockConsumerService);
      verify(mockQueryExecutorService).submit(mockQueryConsumerService);

      // Kiểm tra isRunning flag
      Field isRunningField = EngineServiceMain.class.getDeclaredField("isRunning");
      isRunningField.setAccessible(true);
      boolean isRunning = (boolean) isRunningField.get(null);
      assertTrue(isRunning);
    }
  }

  @Test
  @DisplayName("Start method should not initialize if service is already running")
  void start_ShouldNotInitializeIfAlreadyRunning() throws Exception {
    try (
        MockedStatic<ServiceInitializer> serviceInitializerMocked = mockStatic(ServiceInitializer.class)) {
      // Thiết lập isRunning = true
      Field isRunningField = EngineServiceMain.class.getDeclaredField("isRunning");
      isRunningField.setAccessible(true);
      isRunningField.set(null, true);

      // Act
      EngineServiceMain.start();

      // Verify createTopics không được gọi
      serviceInitializerMocked.verify(() -> ServiceInitializer.createTopics(), never());
    }
  }

  @Test
  @DisplayName("Start method should handle exceptions during initialization")
  void start_ShouldHandleExceptions() throws Exception {
    try (
        MockedStatic<EngineHandler> engineHandlerMocked = mockStatic(EngineHandler.class);
        MockedStatic<KafkaProducerService> producerServiceMocked = mockStatic(KafkaProducerService.class);
        MockedStatic<KafkaConsumerService> consumerServiceMocked = mockStatic(KafkaConsumerService.class);
        MockedStatic<KafkaConsumerQueryService> queryConsumerServiceMocked = mockStatic(
            KafkaConsumerQueryService.class);
        MockedStatic<ServiceInitializer> serviceInitializerMocked = mockStatic(ServiceInitializer.class);
        MockedStatic<java.util.concurrent.Executors> executorsMocked = mockStatic(
            java.util.concurrent.Executors.class)) {
      // Mock các dependency
      engineHandlerMocked.when(EngineHandler::getInstance).thenReturn(mockEngineHandler);
      producerServiceMocked.when(KafkaProducerService::getInstance).thenReturn(mockProducerService);
      consumerServiceMocked.when(KafkaConsumerService::getInstance).thenReturn(mockConsumerService);
      queryConsumerServiceMocked.when(KafkaConsumerQueryService::getInstance).thenReturn(mockQueryConsumerService);

      // Mock thread pools
      executorsMocked.when(() -> java.util.concurrent.Executors.newSingleThreadExecutor())
          .thenReturn(mockLogicExecutorService)
          .thenReturn(mockQueryExecutorService);

      // Mock query threads count
      when(mockQueryConsumerService.getQueryThreadsCount()).thenReturn(2);

      // Mock ServiceInitializer.createTopics để ném ngoại lệ
      serviceInitializerMocked.when(() -> ServiceInitializer.createTopics())
          .thenThrow(new RuntimeException("Failed to create topics"));

      // Act & Assert
      RuntimeException exception = assertThrows(RuntimeException.class, () -> {
        EngineServiceMain.start();
      });

      assertEquals("Cannot start EngineService", exception.getMessage());

      // Verify isRunning flag không được set
      Field isRunningField = EngineServiceMain.class.getDeclaredField("isRunning");
      isRunningField.setAccessible(true);
      boolean isRunning = (boolean) isRunningField.get(null);
      assertFalse(isRunning);
    }
  }

  @Test
  @DisplayName("GetEngineHandler should return the current EngineHandler instance")
  void getEngineHandler_ShouldReturnEngineHandler() throws Exception {
    // Thiết lập engineHandler field
    Field engineHandlerField = EngineServiceMain.class.getDeclaredField("engineHandler");
    engineHandlerField.setAccessible(true);
    engineHandlerField.set(null, mockEngineHandler);

    // Act
    EngineHandler result = EngineServiceMain.getEngineHandler();

    // Assert
    assertEquals(mockEngineHandler, result);
  }

  @Test
  @DisplayName("GetProducerService should return the current KafkaProducerService instance")
  void getProducerService_ShouldReturnProducerService() throws Exception {
    // Thiết lập producerService field
    Field producerServiceField = EngineServiceMain.class.getDeclaredField("producerService");
    producerServiceField.setAccessible(true);
    producerServiceField.set(null, mockProducerService);

    // Act
    KafkaProducerService result = EngineServiceMain.getProducerService();

    // Assert
    assertEquals(mockProducerService, result);
  }

  @Test
  @DisplayName("Stop method should shutdown all components gracefully")
  void stop_ShouldShutdownAllComponents() throws Exception {
    // Thiết lập các field
    Field isRunningField = EngineServiceMain.class.getDeclaredField("isRunning");
    isRunningField.setAccessible(true);
    isRunningField.set(null, true);

    Field consumerServiceField = EngineServiceMain.class.getDeclaredField("consumerService");
    consumerServiceField.setAccessible(true);
    consumerServiceField.set(null, mockConsumerService);

    Field queryConsumerServiceField = EngineServiceMain.class.getDeclaredField("queryConsumerService");
    queryConsumerServiceField.setAccessible(true);
    queryConsumerServiceField.set(null, mockQueryConsumerService);

    Field producerServiceField = EngineServiceMain.class.getDeclaredField("producerService");
    producerServiceField.setAccessible(true);
    producerServiceField.set(null, mockProducerService);

    Field logicExecutorServiceField = EngineServiceMain.class.getDeclaredField("logicExecutorService");
    logicExecutorServiceField.setAccessible(true);
    logicExecutorServiceField.set(null, mockLogicExecutorService);

    Field queryExecutorServiceField = EngineServiceMain.class.getDeclaredField("queryExecutorService");
    queryExecutorServiceField.setAccessible(true);
    queryExecutorServiceField.set(null, mockQueryExecutorService);

    // Mock await termination
    when(mockLogicExecutorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
    when(mockQueryExecutorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);

    // Act
    EngineServiceMain.stop();

    // Verify
    verify(mockConsumerService).shutdown();
    verify(mockQueryConsumerService).shutdown();
    verify(mockLogicExecutorService).shutdown();
    verify(mockQueryExecutorService).shutdown();
    verify(mockProducerService).close();

    // Verify không gọi shutdownNow vì await termination trả về true
    verify(mockLogicExecutorService, never()).shutdownNow();
    verify(mockQueryExecutorService, never()).shutdownNow();

    // Kiểm tra isRunning flag
    boolean isRunning = (boolean) isRunningField.get(null);
    assertFalse(isRunning);
  }

  @Test
  @DisplayName("Stop method should force shutdown when executors timeout")
  void stop_ShouldForceShutdownWhenExecutorsTimeout() throws Exception {
    // Thiết lập các field
    Field isRunningField = EngineServiceMain.class.getDeclaredField("isRunning");
    isRunningField.setAccessible(true);
    isRunningField.set(null, true);

    Field logicExecutorServiceField = EngineServiceMain.class.getDeclaredField("logicExecutorService");
    logicExecutorServiceField.setAccessible(true);
    logicExecutorServiceField.set(null, mockLogicExecutorService);

    Field queryExecutorServiceField = EngineServiceMain.class.getDeclaredField("queryExecutorService");
    queryExecutorServiceField.setAccessible(true);
    queryExecutorServiceField.set(null, mockQueryExecutorService);

    // Mock await termination trả về false để buộc shutdown now
    when(mockLogicExecutorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(false);
    when(mockQueryExecutorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(false);

    // Act
    EngineServiceMain.stop();

    // Verify
    verify(mockLogicExecutorService).shutdownNow();
    verify(mockQueryExecutorService).shutdownNow();
  }

  @Test
  @DisplayName("Stop method should handle null fields gracefully")
  void stop_ShouldHandleNullFields() throws Exception {
    // Thiết lập isRunning = true nhưng các service = null
    Field isRunningField = EngineServiceMain.class.getDeclaredField("isRunning");
    isRunningField.setAccessible(true);
    isRunningField.set(null, true);

    // Act
    EngineServiceMain.stop();

    // Không có exception được throw
    boolean isRunning = (boolean) isRunningField.get(null);
    assertFalse(isRunning);
  }

  @Test
  @DisplayName("Stop method should not stop if service is not running")
  void stop_ShouldNotStopIfNotRunning() throws Exception {
    try (
        MockedStatic<KafkaConsumerService> consumerServiceMocked = mockStatic(KafkaConsumerService.class)) {
      // Thiết lập isRunning = false
      Field isRunningField = EngineServiceMain.class.getDeclaredField("isRunning");
      isRunningField.setAccessible(true);
      isRunningField.set(null, false);

      // Act
      EngineServiceMain.stop();

      // Verify không gọi getInstance
      consumerServiceMocked.verify(() -> KafkaConsumerService.getInstance(), never());
    }
  }

  @Test
  @DisplayName("Stop method should handle exceptions during shutdown")
  void stop_ShouldHandleExceptions() throws Exception {
    // Thiết lập các field
    Field isRunningField = EngineServiceMain.class.getDeclaredField("isRunning");
    isRunningField.setAccessible(true);
    isRunningField.set(null, true);

    Field consumerServiceField = EngineServiceMain.class.getDeclaredField("consumerService");
    consumerServiceField.setAccessible(true);
    consumerServiceField.set(null, mockConsumerService);

    // Mock shutdown để ném ngoại lệ
    doThrow(new RuntimeException("Failed to shutdown")).when(mockConsumerService).shutdown();

    // Act
    EngineServiceMain.stop();

    // Verify
    verify(mockConsumerService).shutdown();

    // Verify isRunning flag được reset
    boolean isRunning = (boolean) isRunningField.get(null);
    assertFalse(isRunning);
  }
}
