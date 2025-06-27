package com.exchangeengine.bootstrap;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.InOrder;
import org.apache.kafka.clients.producer.KafkaProducer;

import com.exchangeengine.extension.CombinedTestExtension;
import com.exchangeengine.messaging.common.KafkaConfig;
import com.exchangeengine.messaging.consumer.KafkaConsumerService;
import com.exchangeengine.messaging.producer.KafkaProducerService;
import com.exchangeengine.service.engine.EngineDisruptorService;
import com.exchangeengine.service.engine.EngineHandler;
import com.exchangeengine.service.engine.OutputProcessor;
import com.exchangeengine.storage.rocksdb.RocksDBService;
import com.exchangeengine.storage.StorageService;

@ExtendWith({ MockitoExtension.class, CombinedTestExtension.class })
@MockitoSettings(strictness = Strictness.LENIENT)
class ServiceInitializerTest {

  @Mock
  private RocksDBService mockRocksDBService;

  @Mock
  private StorageService mockStorageService;

  @Mock
  private KafkaConfig mockKafkaConfig;

  @Mock
  private KafkaProducerService mockKafkaProducerService;

  @Mock
  private OutputProcessor mockOutputProcessor;

  @Mock
  private EngineHandler mockEngineHandler;

  @Mock
  private KafkaConsumerService mockKafkaConsumerService;

  @Mock
  private EngineDisruptorService mockEngineDisruptorService;

  @Mock
  private KafkaProducer<String, String> mockKafkaProducer;

  @BeforeEach
  void setUp() throws Exception {
    // CombinedTestExtension đã tự động reset tất cả các singleton

    // Thiết lập các mock
    setupMocks();

    // Khởi tạo biến môi trường
    resetInitializedFlag();
  }

  /**
   * Thiết lập các mock cần thiết cho test
   */
  private void setupMocks() {
    // Thiết lập tất cả các mock
    RocksDBService.setTestInstance(mockRocksDBService);
    StorageService.setTestInstance(mockStorageService);
    KafkaConfig.setTestInstance(mockKafkaConfig);
    KafkaProducerService.setTestInstance(mockKafkaProducerService);
    OutputProcessor.setTestInstance(mockOutputProcessor);
    EngineHandler.setTestInstance(mockEngineHandler);
    KafkaConsumerService.setTestInstance(mockKafkaConsumerService);
    EngineDisruptorService.setTestInstance(mockEngineDisruptorService);

    // Thiết lập mockKafkaConfig để trả về mockKafkaProducer
    when(mockKafkaConfig.getProducer()).thenReturn(mockKafkaProducer);
  }

  /**
   * Reset initialized flag
   */
  private void resetInitializedFlag() throws Exception {
    Field initializedField = ServiceInitializer.class.getDeclaredField("initialized");
    initializedField.setAccessible(true);
    initializedField.set(null, false);
  }

  @Test
  @DisplayName("ServiceInitializer should have a private constructor to prevent instantiation")
  void constructorShouldBePrivate() throws Exception {
    Constructor<ServiceInitializer> constructor = ServiceInitializer.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");

    // Kiểm tra có thể truy cập vào constructor private
    constructor.setAccessible(true);
    Exception exception = assertThrows(Exception.class, constructor::newInstance);

    // Kiểm tra xem exception có phải là do constructor private không cho phép tạo
    // instance
    assertTrue(exception.getCause() instanceof IllegalStateException
        || exception.getCause() instanceof UnsupportedOperationException,
        "Constructor should throw exception to prevent instantiation");
  }

  @Test
  @DisplayName("Utility class should have all methods and fields static")
  void utilityClass_ShouldHaveAllStaticMethodsAndFields() {
    // Kiểm tra tất cả các phương thức công khai đều là static
    assertTrue(java.util.Arrays.stream(ServiceInitializer.class.getDeclaredMethods())
        .filter(m -> Modifier.isPublic(m.getModifiers()))
        .allMatch(m -> Modifier.isStatic(m.getModifiers())),
        "All public methods should be static in a utility class");

    // Kiểm tra tất cả các trường đều là static
    assertTrue(java.util.Arrays.stream(ServiceInitializer.class.getDeclaredFields())
        .filter(f -> !f.isSynthetic()) // Loại bỏ các trường tổng hợp
        .allMatch(f -> Modifier.isStatic(f.getModifiers())),
        "All fields should be static in a utility class");
  }

  @Test
  @DisplayName("Initialize method should initialize all components")
  void initialize_ShouldInitializeAllComponents() {
    // Thu thập các mock singleton trước khi gọi initialize()
    StorageService preInitMockStorageService = StorageService.getInstance();
    RocksDBService preInitMockRocksDBService = RocksDBService.getInstance();
    EngineDisruptorService preInitMockEngineDisruptorService = EngineDisruptorService.getInstance();

    // Act
    ServiceInitializer.initialize();

    // Assert - Đảm bảo rằng getInstance vẫn trả về các mock object
    assertSame(preInitMockRocksDBService, RocksDBService.getInstance(),
        "RocksDBService should be mockRocksDBService");
    assertSame(preInitMockStorageService, StorageService.getInstance(),
        "StorageService should be mockStorageService");
    assertSame(preInitMockEngineDisruptorService, EngineDisruptorService.getInstance(),
        "EngineDisruptorService should be mockEngineDisruptorService");
  }

  @Test
  @DisplayName("Initialize method should not initialize components twice")
  void initialize_ShouldNotInitializeTwice() {
    // Khởi tạo lần đầu tiên
    ServiceInitializer.initialize();

    // Reset counters trong mocks
    clearInvocations(mockKafkaConfig);

    // Khởi tạo lần thứ hai
    ServiceInitializer.initialize();

    // Verify rằng KafkaConfig.getProducer() không được gọi lần thứ hai
    // do chúng ta đã đánh dấu initialized = true
    verify(mockKafkaConfig, times(0)).getProducer();
  }

  @Test
  @DisplayName("Initialize method should throw exception and reset flag when initialization fails")
  void initialize_ShouldThrowExceptionAndResetFlag_WhenInitializationFails() throws Exception {
    // Given
    // Create a fresh mock of EngineHandler for this specific test
    EngineHandler mockEngineHandlerForTest = mock(EngineHandler.class);

    // Use MockedStatic to mock the getInstance method of EngineHandler
    try (MockedStatic<EngineHandler> engineHandlerMocked = mockStatic(EngineHandler.class)) {
      // Configure the mock to throw an exception when getInstance is called
      engineHandlerMocked.when(EngineHandler::getInstance)
          .thenThrow(new RuntimeException("Test exception"));

      // When - Then
      Exception exception = assertThrows(RuntimeException.class, () -> {
        ServiceInitializer.initialize();
      });

      // Kiểm tra ngoại lệ
      assertEquals("Không thể khởi tạo các thành phần dùng chung", exception.getMessage());
      assertTrue(exception.getCause() instanceof RuntimeException);
      assertEquals("Test exception", exception.getCause().getMessage());

      // Kiểm tra flag initialized đã được reset
      Field initializedField = ServiceInitializer.class.getDeclaredField("initialized");
      initializedField.setAccessible(true);
      boolean initialized = (boolean) initializedField.get(null);
      assertFalse(initialized, "initialized flag should be reset to false when initialization fails");
    }
  }

  @Test
  @DisplayName("InitializeDisruptor method should throw RuntimeException when EngineDisruptorService throws exception")
  void initializeDisruptor_ShouldThrowRuntimeException_WhenEngineDisruptorServiceThrowsException() throws Exception {
    try (MockedStatic<EngineDisruptorService> engineDisruptorServiceMocked = mockStatic(EngineDisruptorService.class)) {
      // Thiết lập EngineDisruptorService.getInstance() để ném ngoại lệ
      engineDisruptorServiceMocked.when(EngineDisruptorService::getInstance)
          .thenThrow(new RuntimeException("Disruptor initialization failed"));

      // Gọi private method initializeDisruptor thông qua reflection
      Method method = ServiceInitializer.class.getDeclaredMethod("initializeDisruptor");
      method.setAccessible(true);

      // Act và Assert
      RuntimeException exception = assertThrows(RuntimeException.class, () -> {
        try {
          method.invoke(null);
        } catch (InvocationTargetException e) {
          if (e.getCause() instanceof RuntimeException) {
            throw (RuntimeException) e.getCause();
          }
          throw new RuntimeException(e);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });

      // Verify exception message
      assertEquals("Cannot initialize Disruptor", exception.getMessage());
    }
  }

  @Test
  @DisplayName("CreateTopics method should call KafkaConfig.createTopics")
  void createTopics_ShouldCallKafkaConfigCreateTopics() {
    // KafkaConfig.getInstance() đã được mock trong setUp()

    // Act
    ServiceInitializer.createTopics();

    // Verify
    verify(mockKafkaConfig).createTopics();
  }

  @Test
  @DisplayName("shutdownDisruptor should shutdown Disruptor components in correct order")
  void shutdownDisruptor_ShouldShutdownComponents() throws Exception {
    // Thiết lập phương thức private shutdownDisruptor qua reflection
    Method shutdownDisruptorMethod = ServiceInitializer.class.getDeclaredMethod("shutdownDisruptor");
    shutdownDisruptorMethod.setAccessible(true);

    // Act - Gọi shutdownDisruptor trực tiếp
    shutdownDisruptorMethod.invoke(null);

    // Verify thứ tự gọi các phương thức
    InOrder inOrder = inOrder(mockEngineDisruptorService, mockOutputProcessor);
    inOrder.verify(mockEngineDisruptorService).shutdown();
    inOrder.verify(mockOutputProcessor).shutdown();
  }

  @Test
  @DisplayName("shutdownDisruptor should handle exception from EngineDisruptorService")
  void shutdownDisruptor_ShouldHandleException() throws Exception {
    // Thiết lập EngineDisruptorService.shutdown() ném exception
    doThrow(new RuntimeException("Test shutdown exception")).when(mockEngineDisruptorService).shutdown();

    // Thiết lập phương thức private shutdownDisruptor qua reflection
    Method shutdownDisruptorMethod = ServiceInitializer.class.getDeclaredMethod("shutdownDisruptor");
    shutdownDisruptorMethod.setAccessible(true);

    // Act - Gọi shutdownDisruptor trực tiếp
    shutdownDisruptorMethod.invoke(null);

    // Bởi vì trong shutdownDisruptor(), hai lệnh gọi nằm trong cùng một khối
    // try-catch,
    // nên khi mockEngineDisruptorService.shutdown() phát sinh exception, khối catch
    // sẽ bắt exception
    // và OutputProcessor.shutdown() sẽ không được gọi
    // Vì vậy ta chỉ cần verify mockEngineDisruptorService.shutdown() đã được gọi
    verify(mockEngineDisruptorService).shutdown();

    // Và verify rằng OutputProcessor.shutdown() KHÔNG được gọi
    verify(mockOutputProcessor, never()).shutdown();
  }

  @Test
  @DisplayName("shutdownKafka should shutdown Kafka components in correct order")
  void shutdownKafka_ShouldShutdownComponents() throws Exception {
    // Thiết lập phương thức private shutdownKafka qua reflection
    Method shutdownKafkaMethod = ServiceInitializer.class.getDeclaredMethod("shutdownKafka");
    shutdownKafkaMethod.setAccessible(true);

    // Act - Gọi shutdownKafka trực tiếp
    shutdownKafkaMethod.invoke(null);

    // Verify thứ tự gọi các phương thức
    InOrder inOrder = inOrder(mockKafkaConsumerService, mockKafkaProducerService, mockKafkaConfig);
    inOrder.verify(mockKafkaConsumerService).shutdown();
    inOrder.verify(mockKafkaProducerService).close();
    inOrder.verify(mockKafkaConfig).shutdown();
  }

  @Test
  @DisplayName("shutdownKafka should handle exception in KafkaConsumerService")
  void shutdownKafka_ShouldHandleExceptionInConsumerService() throws Exception {
    // Thiết lập KafkaConsumerService.shutdown() ném exception
    doThrow(new RuntimeException("Consumer shutdown failed")).when(mockKafkaConsumerService).shutdown();

    // Thiết lập phương thức private shutdownKafka qua reflection
    Method shutdownKafkaMethod = ServiceInitializer.class.getDeclaredMethod("shutdownKafka");
    shutdownKafkaMethod.setAccessible(true);

    // Act - Gọi shutdownKafka trực tiếp
    shutdownKafkaMethod.invoke(null);

    // Verify các phương thức khác vẫn được gọi
    verify(mockKafkaProducerService).close();
    verify(mockKafkaConfig).shutdown();
  }
}
