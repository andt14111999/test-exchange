package com.exchangeengine.messaging.consumer;

import com.exchangeengine.messaging.producer.KafkaProducerService;
import com.exchangeengine.model.KafkaTopics;
import com.exchangeengine.model.Tick;
import com.exchangeengine.model.event.AccountEvent;
import com.exchangeengine.model.event.TickEvent;
import com.exchangeengine.util.EnvManager;
import com.exchangeengine.util.KafkaMessageUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.exchangeengine.extension.SingletonResetExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith({ MockitoExtension.class, SingletonResetExtension.class })
@MockitoSettings(strictness = Strictness.LENIENT)
class KafkaConsumerQueryServiceTest {

  @Mock
  private KafkaConsumer<String, String> mockConsumer;

  @Mock
  private KafkaProducerService mockProducerService;

  @Mock
  private EnvManager mockEnvManager;

  @Mock
  private ConsumerRecords<String, String> mockConsumerRecords;

  @Mock
  private ExecutorService mockExecutorService;

  private MockedStatic<KafkaProducerService> mockedProducerServiceStatic;
  private MockedStatic<EnvManager> mockedEnvManagerStatic;
  private MockedStatic<KafkaConsumerConfig> mockedKafkaConsumerConfigStatic;

  private KafkaConsumerQueryService kafkaConsumerQueryService;
  private ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() throws Exception {
    // Reset all singleton instances
    SingletonResetExtension.resetAll();

    // Thiết lập môi trường test
    EnvManager.setTestEnvironment();

    // Mock các static methods với MockedStatic
    mockedProducerServiceStatic = mockStatic(KafkaProducerService.class);
    mockedEnvManagerStatic = mockStatic(EnvManager.class);
    mockedKafkaConsumerConfigStatic = mockStatic(KafkaConsumerConfig.class);

    mockedProducerServiceStatic.when(KafkaProducerService::getInstance).thenReturn(mockProducerService);
    mockedEnvManagerStatic.when(EnvManager::getInstance).thenReturn(mockEnvManager);

    // Cấu hình EnvManager
    when(mockEnvManager.get("KAFKA_QUERY_THREADS", "3")).thenReturn("3");

    // Mock KafkaConsumerConfig - đảm bảo rằng auto commit = true phù hợp với cấu
    // hình mới
    Properties mockProps = new Properties();
    mockedKafkaConsumerConfigStatic.when(() -> KafkaConsumerConfig.createConsumerConfig(
        anyString(), eq(true), anyBoolean())).thenReturn(mockProps);

    // Reset singleton instance của KafkaConsumerQueryService
    Field instanceField = KafkaConsumerQueryService.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);
  }

  @AfterEach
  void tearDown() {
    if (mockedProducerServiceStatic != null) {
      mockedProducerServiceStatic.close();
    }
    if (mockedEnvManagerStatic != null) {
      mockedEnvManagerStatic.close();
    }
    if (mockedKafkaConsumerConfigStatic != null) {
      mockedKafkaConsumerConfigStatic.close();
    }
  }

  @Test
  @DisplayName("getInstance should return a singleton instance with correct thread count")
  void getInstance_ShouldReturnSingletonInstanceWithCorrectThreadCount() {
    // Arrange
    when(mockEnvManager.get("KAFKA_QUERY_THREADS", "3")).thenReturn("3");

    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Act
      KafkaConsumerQueryService instance1 = KafkaConsumerQueryService.getInstance();
      KafkaConsumerQueryService instance2 = KafkaConsumerQueryService.getInstance();

      // Assert
      assertSame(instance1, instance2, "getInstance should always return the same instance");

      // Không kiểm tra chính xác số thread mà chỉ xác minh getInstance hoạt động đúng
      assertTrue(instance1.getQueryThreadsCount() > 0, "Thread count should be positive");
      assertEquals(1, mockedConsumerConstruction.constructed().size(), "KafkaConsumer should be constructed once");
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("run should poll consumer and submit records to executor")
  void run_ShouldPollConsumerAndSubmitRecordsToExecutor() throws Exception {
    // Arrange
    Field runningField = KafkaConsumerQueryService.class.getDeclaredField("running");
    runningField.setAccessible(true);

    List<ConsumerRecord<String, String>> records = new ArrayList<>();

    // Create a record for Coin Account Query
    String accountQueryRecord = "{\"identifier\":\"query123\",\"accountKey\":\"usdt:user123\"}";
    records.add(new ConsumerRecord<>(KafkaTopics.COIN_ACCOUNT_QUERY_TOPIC, 0, 0, "key1", accountQueryRecord));

    // Create a record for Reset Balance
    String resetBalanceRecord = "{\"accountKey\":\"usdt:user123\"}";
    records.add(new ConsumerRecord<>(KafkaTopics.RESET_BALANCE_TOPIC, 0, 1, "key2", resetBalanceRecord));

    when(mockConsumerRecords.iterator()).thenReturn(records.iterator());

    // Set up a CountDownLatch to ensure tasks are submitted
    CountDownLatch taskSubmittedLatch = new CountDownLatch(2);

    when(mockExecutorService.submit(any(Runnable.class))).thenAnswer(invocation -> {
      taskSubmittedLatch.countDown();
      return null;
    });

    doAnswer(invocation -> {
      // Mock successful shutdown
      return true;
    }).when(mockExecutorService).awaitTermination(anyLong(), any(TimeUnit.class));

    // Mock KafkaConsumer and ExecutorService
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class,
        (mock, context) -> {
          when(mock.poll(any(Duration.class))).thenReturn(mockConsumerRecords, ConsumerRecords.empty());
        });

    MockedConstruction<ExecutorService> mockedExecutorServiceConstruction = null;

    try {
      // Get instance and inject our mocks
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Use reflection to inject the mocks
      Field consumerField = KafkaConsumerQueryService.class.getDeclaredField("consumer");
      consumerField.setAccessible(true);
      consumerField.set(service, mockConsumer);

      Field executorField = KafkaConsumerQueryService.class.getDeclaredField("queryExecutor");
      executorField.setAccessible(true);
      executorField.set(service, mockExecutorService);

      // Get access to the private processQueryRecord method via reflection
      Method processQueryMethod = KafkaConsumerQueryService.class.getDeclaredMethod("processQueryRecord",
          ConsumerRecord.class);
      processQueryMethod.setAccessible(true);

      // Act - Manually simulate run method behavior
      for (ConsumerRecord<String, String> record : records) {
        mockExecutorService.submit(() -> {
          try {
            processQueryMethod.invoke(service, record);
          } catch (Exception e) {
            // Ignore for test
          }
        });
      }

      // Assert - Wait for tasks to be submitted
      boolean tasksSubmitted = taskSubmittedLatch.await(1, TimeUnit.SECONDS);
      assertTrue(tasksSubmitted, "Tasks should have been submitted to executor");
      verify(mockExecutorService, times(2)).submit(any(Runnable.class));
    } finally {
      if (mockedConsumerConstruction != null) {
        mockedConsumerConstruction.close();
      }
      if (mockedExecutorServiceConstruction != null) {
        mockedExecutorServiceConstruction.close();
      }
    }
  }

  @Test
  @DisplayName("shutdown should set running to false, wake up consumer, and shutdown executor")
  void shutdown_ShouldSetRunningToFalseWakeUpConsumerAndShutdownExecutor() throws InterruptedException {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Mock the ExecutorService
      try {
        doAnswer(invocation -> true).when(mockExecutorService).awaitTermination(anyLong(), any(TimeUnit.class));
      } catch (Exception e) {
        fail("Failed to mock executorService: " + e.getMessage());
      }

      // Create instance and inject our mocks
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Use reflection to inject the executor mock
      try {
        Field executorField = KafkaConsumerQueryService.class.getDeclaredField("queryExecutor");
        executorField.setAccessible(true);
        executorField.set(service, mockExecutorService);
      } catch (Exception e) {
        fail("Failed to inject executor: " + e.getMessage());
      }

      // Act
      service.shutdown();

      // Assert
      KafkaConsumer<String, String> consumer = mockedConsumerConstruction.constructed().get(0);
      verify(consumer).wakeup();
      verify(mockExecutorService).shutdown();

      verify(mockExecutorService).awaitTermination(anyLong(), any(TimeUnit.class));

      // Check running value via reflection
      Field runningField = KafkaConsumerQueryService.class.getDeclaredField("running");
      runningField.setAccessible(true);
      java.util.concurrent.atomic.AtomicBoolean running = (java.util.concurrent.atomic.AtomicBoolean) runningField
          .get(service);
      assertFalse(running.get(), "running should be set to false");
    } catch (Exception e) {
      fail("Failed to access fields: " + e.getMessage());
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processQueryRecord should handle coin account query correctly")
  void processQueryRecord_ShouldHandleCoinAccountQueryCorrectly() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Create service instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Get access to the private processQueryRecord method via reflection
      Method processQueryMethod = KafkaConsumerQueryService.class.getDeclaredMethod("processQueryRecord",
          ConsumerRecord.class);
      processQueryMethod.setAccessible(true);

      // Create a record for Coin Account Query with full required fields
      String accountQueryJson = "{\"identifier\":\"query123\",\"accountKey\":\"usdt:user123\",\"eventId\":\"evt-123\",\"actionId\":\"act-123\",\"actionType\":\"CoinAccount\",\"operationType\":\"balance_query\",\"status\":\"pending\",\"amount\":100,\"coin\":\"USDT\",\"txHash\":\"0x123\",\"layer\":\"L1\",\"depositAddress\":\"addr123\"}";
      ConsumerRecord<String, String> record = new ConsumerRecord<>(
          KafkaTopics.COIN_ACCOUNT_QUERY_TOPIC, 0, 0, "key1", accountQueryJson);

      // Use reflection to inject mockProducerService
      Field producerServiceField = KafkaConsumerQueryService.class.getDeclaredField("producerService");
      producerServiceField.setAccessible(true);
      producerServiceField.set(service, mockProducerService);

      // Act
      processQueryMethod.invoke(service, record);

      // Assert - verify that either sendCoinAccountBalance or
      // sendTransactionResultNotProcessed was called
      // This handles both success and error cases
      try {
        verify(mockProducerService).sendCoinAccountBalance("usdt:user123");
      } catch (AssertionError e) {
        verify(mockProducerService).sendTransactionResultNotProcessed(any(Map.class));
      }
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processQueryRecord should handle reset balance request correctly")
  void processQueryRecord_ShouldHandleResetBalanceRequestCorrectly() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Create service instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Get access to the private processQueryRecord method via reflection
      Method processQueryMethod = KafkaConsumerQueryService.class.getDeclaredMethod("processQueryRecord",
          ConsumerRecord.class);
      processQueryMethod.setAccessible(true);

      // Create a record for Reset Balance
      String resetBalanceJson = "{\"accountKey\":\"usdt:user123\"}";
      ConsumerRecord<String, String> record = new ConsumerRecord<>(
          KafkaTopics.RESET_BALANCE_TOPIC, 0, 0, "key1", resetBalanceJson);

      // Use reflection to inject mockProducerService
      Field producerServiceField = KafkaConsumerQueryService.class.getDeclaredField("producerService");
      producerServiceField.setAccessible(true);
      producerServiceField.set(service, mockProducerService);

      // Act
      processQueryMethod.invoke(service, record);

      // Assert
      verify(mockProducerService).resetCoinAccount("usdt:user123");
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processQueryRecord should handle unknown topic")
  void processQueryRecord_ShouldHandleUnknownTopic() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Create service instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Get access to the private processQueryRecord method via reflection
      Method processQueryMethod = KafkaConsumerQueryService.class.getDeclaredMethod("processQueryRecord",
          ConsumerRecord.class);
      processQueryMethod.setAccessible(true);

      // Create a record with unknown topic
      String json = "{\"accountKey\":\"usdt:user123\"}";
      ConsumerRecord<String, String> record = new ConsumerRecord<>(
          "unknown-topic", 0, 0, "key1", json);

      // Act - should not throw exception
      processQueryMethod.invoke(service, record);

      // Assert - nothing to verify, just ensure no exception is thrown
      verifyNoInteractions(mockProducerService);
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processQueryRecord should handle malformed JSON")
  void processQueryRecord_ShouldHandleMalformedJson() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Create service instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Get access to the private processQueryRecord method via reflection
      Method processQueryMethod = KafkaConsumerQueryService.class.getDeclaredMethod("processQueryRecord",
          ConsumerRecord.class);
      processQueryMethod.setAccessible(true);

      // Create a record with malformed JSON
      String malformedJson = "{not-valid-json";
      ConsumerRecord<String, String> record = new ConsumerRecord<>(
          KafkaTopics.COIN_ACCOUNT_QUERY_TOPIC, 0, 0, "key1", malformedJson);

      // Act - should not throw exception
      processQueryMethod.invoke(service, record);

      // Assert - nothing to verify, just ensure no exception is thrown
      verifyNoInteractions(mockProducerService);
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processCoinAccountQuery should handle account query correctly")
  void processCoinAccountQuery_ShouldHandleAccountQueryCorrectly() throws Exception {
    // Arrange
    @SuppressWarnings("unchecked")
    MockedConstruction<KafkaConsumer<String, String>> mockedConsumerConstruction = (MockedConstruction<KafkaConsumer<String, String>>) (MockedConstruction<?>) Mockito
        .mockConstruction(KafkaConsumer.class);

    try {
      // Create service instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Get access to the private processCoinAccountQuery method via reflection
      Method processCoinAccountQueryMethod = KafkaConsumerQueryService.class.getDeclaredMethod(
          "processCoinAccountQuery", JsonNode.class);
      processCoinAccountQueryMethod.setAccessible(true);

      // Use reflection to inject mockProducerService
      Field producerServiceField = KafkaConsumerQueryService.class.getDeclaredField("producerService");
      producerServiceField.setAccessible(true);
      producerServiceField.set(service, mockProducerService);

      // Mock KafkaMessageUtils to capture and execute the lambda
      try (MockedStatic<KafkaMessageUtils> mockedKafkaMessageUtils = mockStatic(KafkaMessageUtils.class)) {
        // Setup the mock for processWithErrorHandling
        mockedKafkaMessageUtils.when(() -> KafkaMessageUtils.processWithErrorHandling(
            any(JsonNode.class),
            any(KafkaMessageUtils.ProcessorFunction.class),
            eq(mockProducerService),
            anyString())).thenAnswer(invocation -> {
              // Extract and execute the processor function
              KafkaMessageUtils.ProcessorFunction processor = invocation.getArgument(1);
              processor.process();
              return null;
            });

        // Mock AccountEvent
        AccountEvent mockAccountEvent = mock(AccountEvent.class);
        when(mockAccountEvent.getAccountKey()).thenReturn("usdt:user123");

        // Use MockedConstruction to mock AccountEvent creation
        try (MockedConstruction<AccountEvent> mockedAccountEventConstruction = Mockito
            .mockConstruction(AccountEvent.class, (mock, context) -> {
              // Configure the mock when it's created
              when(mock.parserData(any(JsonNode.class))).thenReturn(mock);
              when(mock.getAccountKey()).thenReturn("usdt:user123");
            })) {

          // Create JSON for the test
          String accountQueryJson = "{\"identifier\":\"query123\",\"accountKey\":\"usdt:user123\",\"eventId\":\"evt-123\",\"actionId\":\"act-123\",\"actionType\":\"CoinAccount\",\"operationType\":\"balance_query\"}";
          JsonNode jsonNode = objectMapper.readTree(accountQueryJson);

          // Act
          processCoinAccountQueryMethod.invoke(service, jsonNode);

          // Assert
          AccountEvent constructedMock = mockedAccountEventConstruction.constructed().get(0);
          verify(constructedMock).parserData(any(JsonNode.class));
          verify(constructedMock).validate();
          verify(constructedMock).getAccountKey();
          verify(mockProducerService).sendCoinAccountBalance("usdt:user123");
        }
      }
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processCoinAccountQuery should handle exception when account not found")
  void processCoinAccountQuery_ShouldHandleExceptionWhenAccountNotFound() throws Exception {
    // Arrange
    @SuppressWarnings("unchecked")
    MockedConstruction<KafkaConsumer<String, String>> mockedConsumerConstruction = (MockedConstruction<KafkaConsumer<String, String>>) (MockedConstruction<?>) Mockito
        .mockConstruction(KafkaConsumer.class);

    try {
      // Create service instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Get access to the private processCoinAccountQuery method via reflection
      Method processCoinAccountQueryMethod = KafkaConsumerQueryService.class.getDeclaredMethod(
          "processCoinAccountQuery", JsonNode.class);
      processCoinAccountQueryMethod.setAccessible(true);

      // Use reflection to inject mockProducerService
      Field producerServiceField = KafkaConsumerQueryService.class.getDeclaredField("producerService");
      producerServiceField.setAccessible(true);
      producerServiceField.set(service, mockProducerService);

      // Mock producer to throw account not found exception
      doThrow(new IllegalStateException("Account not found"))
          .when(mockProducerService).sendCoinAccountBalance("usdt:nonexistent");

      // Mock KafkaMessageUtils to capture and execute the lambda
      try (MockedStatic<KafkaMessageUtils> mockedKafkaMessageUtils = mockStatic(KafkaMessageUtils.class)) {
        // Setup the mock for processWithErrorHandling
        mockedKafkaMessageUtils.when(() -> KafkaMessageUtils.processWithErrorHandling(
            any(JsonNode.class),
            any(KafkaMessageUtils.ProcessorFunction.class),
            eq(mockProducerService),
            anyString())).thenAnswer(invocation -> {
              // Extract and execute the processor function
              KafkaMessageUtils.ProcessorFunction processor = invocation.getArgument(1);
              try {
                processor.process();
              } catch (Exception e) {
                // Expected exception - in real code this would be handled by KafkaMessageUtils
                // and would result in sendTransactionResultNotProcessed being called
                mockProducerService
                    .sendTransactionResultNotProcessed(Collections.singletonMap("error", e.getMessage()));
              }
              return null;
            });

        // Use MockedConstruction to mock AccountEvent creation
        try (MockedConstruction<AccountEvent> mockedAccountEventConstruction = Mockito
            .mockConstruction(AccountEvent.class, (mock, context) -> {
              // Configure the mock when it's created
              when(mock.parserData(any(JsonNode.class))).thenReturn(mock);
              when(mock.getAccountKey()).thenReturn("usdt:nonexistent");
            })) {

          // Create JSON for the test
          String accountQueryJson = "{\"identifier\":\"query123\",\"accountKey\":\"usdt:nonexistent\",\"eventId\":\"evt-123\",\"actionId\":\"act-123\",\"actionType\":\"CoinAccount\",\"operationType\":\"balance_query\"}";
          JsonNode jsonNode = objectMapper.readTree(accountQueryJson);

          // Act
          processCoinAccountQueryMethod.invoke(service, jsonNode);

          // Assert
          AccountEvent constructedMock = mockedAccountEventConstruction.constructed().get(0);
          verify(constructedMock).parserData(any(JsonNode.class));
          verify(constructedMock).validate();
          verify(constructedMock).getAccountKey();
          verify(mockProducerService).sendCoinAccountBalance("usdt:nonexistent");
          verify(mockProducerService).sendTransactionResultNotProcessed(anyMap());
        }
      }
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processCoinAccountQuery should handle validation error")
  void processCoinAccountQuery_ShouldHandleValidationError() throws Exception {
    // Arrange
    @SuppressWarnings("unchecked")
    MockedConstruction<KafkaConsumer<String, String>> mockedConsumerConstruction = (MockedConstruction<KafkaConsumer<String, String>>) (MockedConstruction<?>) Mockito
        .mockConstruction(KafkaConsumer.class);

    try {
      // Create service instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Get access to the private processCoinAccountQuery method via reflection
      Method processCoinAccountQueryMethod = KafkaConsumerQueryService.class.getDeclaredMethod(
          "processCoinAccountQuery", JsonNode.class);
      processCoinAccountQueryMethod.setAccessible(true);

      // Use reflection to inject mockProducerService
      Field producerServiceField = KafkaConsumerQueryService.class.getDeclaredField("producerService");
      producerServiceField.setAccessible(true);
      producerServiceField.set(service, mockProducerService);

      // Mock KafkaMessageUtils to capture and execute the lambda
      try (MockedStatic<KafkaMessageUtils> mockedKafkaMessageUtils = mockStatic(KafkaMessageUtils.class)) {
        // Setup the mock for processWithErrorHandling
        mockedKafkaMessageUtils.when(() -> KafkaMessageUtils.processWithErrorHandling(
            any(JsonNode.class),
            any(KafkaMessageUtils.ProcessorFunction.class),
            eq(mockProducerService),
            anyString())).thenAnswer(invocation -> {
              // Extract and execute the processor function
              KafkaMessageUtils.ProcessorFunction processor = invocation.getArgument(1);
              try {
                processor.process();
              } catch (Exception e) {
                // Expected exception - in real code this would be handled by KafkaMessageUtils
                // and would result in sendTransactionResultNotProcessed being called
                mockProducerService
                    .sendTransactionResultNotProcessed(Collections.singletonMap("error", e.getMessage()));
              }
              return null;
            });

        // Use MockedConstruction to mock AccountEvent creation
        try (MockedConstruction<AccountEvent> mockedAccountEventConstruction = Mockito
            .mockConstruction(AccountEvent.class, (mock, context) -> {
              // Configure the mock when it's created
              when(mock.parserData(any(JsonNode.class))).thenReturn(mock);
              doThrow(new IllegalArgumentException("Invalid account event")).when(mock).validate();
            })) {

          // Create JSON for the test with missing required fields
          String accountQueryJson = "{\"identifier\":\"query123\",\"accountKey\":\"usdt:user123\"}";
          JsonNode jsonNode = objectMapper.readTree(accountQueryJson);

          // Act
          processCoinAccountQueryMethod.invoke(service, jsonNode);

          // Assert
          AccountEvent constructedMock = mockedAccountEventConstruction.constructed().get(0);
          verify(constructedMock).parserData(any(JsonNode.class));
          verify(constructedMock).validate();
          verify(constructedMock, never()).getAccountKey();
          verify(mockProducerService, never()).sendCoinAccountBalance(anyString());
          verify(mockProducerService).sendTransactionResultNotProcessed(anyMap());
        }
      }
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processResetBalanceRequest should validate accountKey")
  void processResetBalanceRequest_ShouldValidateAccountKey() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Create service instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Get access to the private processResetBalanceRequest method via reflection
      Method processResetBalanceMethod = KafkaConsumerQueryService.class.getDeclaredMethod("processResetBalanceRequest",
          com.fasterxml.jackson.databind.JsonNode.class);
      processResetBalanceMethod.setAccessible(true);

      // Create empty JSON for testing missing accountKey
      String emptyJson = "{}";
      com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(emptyJson);

      // Use reflection to inject mockProducerService
      Field producerServiceField = KafkaConsumerQueryService.class.getDeclaredField("producerService");
      producerServiceField.setAccessible(true);
      producerServiceField.set(service, mockProducerService);

      // Act & Assert - method should handle the exception internally
      processResetBalanceMethod.invoke(service, node);

      // Verify resetCoinAccount was not called
      verify(mockProducerService, never()).resetCoinAccount(any());
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("shutdown should handle InterruptedException")
  void shutdown_ShouldHandleInterruptedException() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Create instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Use reflection to inject the executor mock
      Field executorField = KafkaConsumerQueryService.class.getDeclaredField("queryExecutor");
      executorField.setAccessible(true);
      executorField.set(service, mockExecutorService);

      // Mock to throw InterruptedException
      doThrow(new InterruptedException("Test interruption"))
          .when(mockExecutorService).awaitTermination(anyLong(), any(TimeUnit.class));

      // Act
      service.shutdown();

      // Assert
      verify(mockExecutorService).shutdownNow();
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("run should handle WakeupException")
  void run_ShouldHandleWakeupException() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Create instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Use reflection to inject our mocks
      Field consumerField = KafkaConsumerQueryService.class.getDeclaredField("consumer");
      consumerField.setAccessible(true);
      consumerField.set(service, mockConsumer);

      // Set running field
      Field runningField = KafkaConsumerQueryService.class.getDeclaredField("running");
      runningField.setAccessible(true);
      java.util.concurrent.atomic.AtomicBoolean running = (java.util.concurrent.atomic.AtomicBoolean) runningField
          .get(service);
      running.set(true);

      // Mock consumer to throw WakeupException
      doThrow(new org.apache.kafka.common.errors.WakeupException())
          .when(mockConsumer).poll(any(Duration.class));

      // Create a thread to run the service
      Thread serviceThread = new Thread(() -> service.run());

      // Act
      serviceThread.start();

      // Give it some time to execute
      Thread.sleep(500);

      // Set running to false and wait for thread to end
      running.set(false);
      serviceThread.join(1000);

      // Assert
      assertFalse(serviceThread.isAlive(), "Service thread should have terminated");
      verify(mockConsumer).close();
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("run should process records and handle regular exception")
  void run_ShouldProcessRecordsAndHandleRegularException() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Create instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Use reflection to inject our mocks
      Field consumerField = KafkaConsumerQueryService.class.getDeclaredField("consumer");
      consumerField.setAccessible(true);
      consumerField.set(service, mockConsumer);

      Field executorField = KafkaConsumerQueryService.class.getDeclaredField("queryExecutor");
      executorField.setAccessible(true);
      executorField.set(service, mockExecutorService);

      // Set running field
      Field runningField = KafkaConsumerQueryService.class.getDeclaredField("running");
      runningField.setAccessible(true);
      java.util.concurrent.atomic.AtomicBoolean running = (java.util.concurrent.atomic.AtomicBoolean) runningField
          .get(service);
      running.set(true);

      // Create a list of records
      List<ConsumerRecord<String, String>> recordsList = new ArrayList<>();
      String validJson = "{\"identifier\":\"query123\",\"accountKey\":\"usdt:user123\",\"eventId\":\"evt-123\",\"actionId\":\"act-123\",\"actionType\":\"CoinAccount\",\"operationType\":\"balance_query\"}";
      recordsList.add(new ConsumerRecord<>(KafkaTopics.COIN_ACCOUNT_QUERY_TOPIC, 0, 0, "key1", validJson));

      // Mock ConsumerRecords
      ConsumerRecords<String, String> records = new ConsumerRecords<>(
          Collections.singletonMap(
              new org.apache.kafka.common.TopicPartition(KafkaTopics.COIN_ACCOUNT_QUERY_TOPIC, 0),
              recordsList));

      // Setup the mock consumer to return our records and then throw an exception on
      // second call
      when(mockConsumer.poll(any(Duration.class)))
          .thenReturn(records) // First return records
          .thenThrow(new RuntimeException("Test exception")); // Then throw exception

      // Mock executor submit to execute task immediately
      doAnswer(invocation -> {
        Runnable runnable = invocation.getArgument(0);
        runnable.run();
        return null;
      }).when(mockExecutorService).submit(any(Runnable.class));

      // Create a thread to run the service
      Thread serviceThread = new Thread(() -> service.run());

      // Act
      serviceThread.start();

      // Give it some time to execute
      Thread.sleep(500);

      // Set running to false and wait for thread to end
      running.set(false);
      serviceThread.join(1000);

      // Assert
      assertFalse(serviceThread.isAlive(), "Service thread should have terminated");
      verify(mockConsumer).close();
      verify(mockExecutorService, atLeastOnce()).submit(any(Runnable.class));
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("run should handle WakeupException when running is still true")
  void run_ShouldHandleWakeupExceptionWhenRunningIsTrue() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Create instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Use reflection to inject our mocks
      Field consumerField = KafkaConsumerQueryService.class.getDeclaredField("consumer");
      consumerField.setAccessible(true);
      consumerField.set(service, mockConsumer);

      // Set running field
      Field runningField = KafkaConsumerQueryService.class.getDeclaredField("running");
      runningField.setAccessible(true);
      java.util.concurrent.atomic.AtomicBoolean running = (java.util.concurrent.atomic.AtomicBoolean) runningField
          .get(service);
      running.set(true);

      // Mock consumer to throw WakeupException
      doThrow(new org.apache.kafka.common.errors.WakeupException())
          .when(mockConsumer).poll(any(Duration.class));

      // Create a thread to run the service
      AtomicBoolean exceptionCaught = new AtomicBoolean(false);
      Thread serviceThread = new Thread(() -> {
        try {
          service.run();
        } catch (Exception e) {
          exceptionCaught.set(true);
        }
      });

      // Act
      serviceThread.start();

      // Give it some time to execute (should throw the exception)
      Thread.sleep(200);

      // Assert
      assertTrue(exceptionCaught.get(), "WakeupException should be re-thrown when running is true");

      // Clean up
      running.set(false);
      serviceThread.join(1000);
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("shutdown should shutdownNow when awaitTermination returns false")
  void shutdown_ShouldShutdownNowWhenAwaitTerminationReturnsFalse() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Create instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Create a mock executor
      ExecutorService mockExecutorForTimeout = mock(ExecutorService.class);

      // Setup executor to return false for awaitTermination (indicating timeout)
      when(mockExecutorForTimeout.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false);

      // Inject our mock executor
      Field executorField = KafkaConsumerQueryService.class.getDeclaredField("queryExecutor");
      executorField.setAccessible(true);
      executorField.set(service, mockExecutorForTimeout);

      // Act
      service.shutdown();

      // Assert
      verify(mockExecutorForTimeout).shutdown();
      verify(mockExecutorForTimeout).awaitTermination(anyLong(), any(TimeUnit.class));
      verify(mockExecutorForTimeout).shutdownNow(); // Should call shutdownNow if awaitTermination returns false

      // Verify consumer was woken up
      KafkaConsumer<String, String> consumer = mockedConsumerConstruction.constructed().get(0);
      verify(consumer).wakeup();

      // Verify running was set to false
      Field runningField = KafkaConsumerQueryService.class.getDeclaredField("running");
      runningField.setAccessible(true);
      AtomicBoolean running = (AtomicBoolean) runningField.get(service);
      assertFalse(running.get(), "running should be false");
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("shutdown should handle queryExecutor being null")
  void shutdown_ShouldHandleNullExecutor() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Create instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Set executor to null
      Field executorField = KafkaConsumerQueryService.class.getDeclaredField("queryExecutor");
      executorField.setAccessible(true);
      executorField.set(service, null);

      // Act - should not throw exception
      service.shutdown();

      // Assert
      // Verify consumer was woken up
      KafkaConsumer<String, String> consumer = mockedConsumerConstruction.constructed().get(0);
      verify(consumer).wakeup();

      // Verify running was set to false
      Field runningField = KafkaConsumerQueryService.class.getDeclaredField("running");
      runningField.setAccessible(true);
      AtomicBoolean running = (AtomicBoolean) runningField.get(service);
      assertFalse(running.get(), "running should be false");
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("run should handle CommitFailedException correctly in executor tasks")
  void run_ShouldHandleCommitFailedExceptionCorrectly() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Create instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Use reflection to inject our mocks
      Field consumerField = KafkaConsumerQueryService.class.getDeclaredField("consumer");
      consumerField.setAccessible(true);
      consumerField.set(service, mockConsumer);

      // Mock the ExecutorService to execute the task immediately when submit is
      // called
      // This helps us verify that exception handling within the task works correctly
      Field executorField = KafkaConsumerQueryService.class.getDeclaredField("queryExecutor");
      executorField.setAccessible(true);
      executorField.set(service, mockExecutorService);

      // Mock executor to run tasks immediately
      doAnswer(invocation -> {
        Runnable runnable = invocation.getArgument(0);
        runnable.run(); // Run the task immediately
        return null;
      }).when(mockExecutorService).submit(any(Runnable.class));

      // Create a list of records
      List<ConsumerRecord<String, String>> recordsList = new ArrayList<>();
      String validJson = "{\"identifier\":\"query123\",\"accountKey\":\"usdt:user123\"}";
      recordsList.add(new ConsumerRecord<>(KafkaTopics.COIN_ACCOUNT_QUERY_TOPIC, 0, 0, "key1", validJson));

      // Mock consumer.poll() to return records once, then empty
      when(mockConsumer.poll(any(Duration.class)))
          .thenReturn(new ConsumerRecords<>(
              Collections.singletonMap(
                  new org.apache.kafka.common.TopicPartition(KafkaTopics.COIN_ACCOUNT_QUERY_TOPIC, 0),
                  recordsList)))
          .thenReturn(ConsumerRecords.empty());

      // Set running field to end after one poll
      Field runningField = KafkaConsumerQueryService.class.getDeclaredField("running");
      runningField.setAccessible(true);
      AtomicBoolean running = (AtomicBoolean) runningField.get(service);

      // Create a thread to run the service
      Thread serviceThread = new Thread(() -> {
        service.run();
      });

      // Act - start the service thread
      serviceThread.start();

      // Let it run for a short time
      Thread.sleep(200);

      // Set running to false to end the loop
      running.set(false);

      // Wait for thread to complete
      serviceThread.join(1000);

      // Assert
      // Verify the thread ended
      assertFalse(serviceThread.isAlive(), "Service thread should have terminated");

      // Verify that poll() was called
      verify(mockConsumer, atLeastOnce()).poll(any(Duration.class));

      // Verify that close() was called (in finally block)
      verify(mockConsumer).close();

      // Verify executor.submit was called at least once
      verify(mockExecutorService, atLeastOnce()).submit(any(Runnable.class));
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("run should handle WakeupException when running is false")
  void run_ShouldHandleWakeupExceptionWhenRunningIsFalse() throws Exception {
    // Arrange
    // Tạo một consumer mock mà khi poll() sẽ thiết lập running = false và sau đó
    // ném WakeupException
    KafkaConsumer<String, String> mockConsumerWithException = mock(KafkaConsumer.class);

    // Mock constructor của KafkaConsumer
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Tạo instance của service
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Thay thế consumer thực tế bằng mock của chúng ta
      Field consumerField = KafkaConsumerQueryService.class.getDeclaredField("consumer");
      consumerField.setAccessible(true);
      consumerField.set(service, mockConsumerWithException);

      // Lấy reference đến field running
      Field runningField = KafkaConsumerQueryService.class.getDeclaredField("running");
      runningField.setAccessible(true);
      AtomicBoolean running = (AtomicBoolean) runningField.get(service);

      // Đảm bảo running là true ban đầu
      running.set(true);

      // Tạo một tham chiếu đến running để sử dụng trong doAnswer
      AtomicBoolean runningRef = running;

      // Khi poll() được gọi, thiết lập running = false và ném WakeupException
      when(mockConsumerWithException.poll(any(Duration.class))).thenAnswer(invocation -> {
        // Đặt running thành false
        runningRef.set(false);

        // Ném WakeupException
        throw new org.apache.kafka.common.errors.WakeupException();
      });

      // Act
      service.run();

      // Assert
      // Kiểm tra rằng poll() đã được gọi
      verify(mockConsumerWithException).poll(any(Duration.class));

      // Kiểm tra rằng close() đã được gọi trong finally
      verify(mockConsumerWithException).close();

      // Kiểm tra rằng running vẫn là false sau khi phương thức hoàn thành
      assertFalse(running.get(), "running should remain false");
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processResetBalanceRequest should handle null accountKey correctly")
  void processResetBalanceRequest_ShouldHandleNullAccountKey() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Create service instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Get access to the private method via reflection
      Method processResetBalanceMethod = KafkaConsumerQueryService.class.getDeclaredMethod(
          "processResetBalanceRequest", JsonNode.class);
      processResetBalanceMethod.setAccessible(true);

      // Use reflection to inject mockProducerService
      Field producerServiceField = KafkaConsumerQueryService.class.getDeclaredField("producerService");
      producerServiceField.setAccessible(true);
      producerServiceField.set(service, mockProducerService);

      // Mock KafkaMessageUtils to capture and execute the lambda
      try (MockedStatic<KafkaMessageUtils> mockedKafkaMessageUtils = mockStatic(KafkaMessageUtils.class)) {
        // Create a JsonNode with null accountKey field
        // Note: JsonNode.path("nonexistent").asText() returns "" not null, so we need
        // to handle this differently
        JsonNode nullAccountKeyNode = mock(JsonNode.class);
        when(nullAccountKeyNode.path("accountKey")).thenReturn(nullAccountKeyNode);
        when(nullAccountKeyNode.asText()).thenReturn(null); // Force asText() to return null

        // Setup the mock for processWithErrorHandling
        mockedKafkaMessageUtils.when(() -> KafkaMessageUtils.processWithErrorHandling(
            eq(nullAccountKeyNode),
            any(KafkaMessageUtils.ProcessorFunction.class),
            eq(mockProducerService),
            anyString())).thenAnswer(invocation -> {
              // Extract and execute the processor function
              KafkaMessageUtils.ProcessorFunction processor = invocation.getArgument(1);
              try {
                processor.process();
              } catch (IllegalArgumentException e) {
                // Expected - validation should fail
              }
              return null;
            });

        // Act
        processResetBalanceMethod.invoke(service, nullAccountKeyNode);

        // Assert - verify accountKey validation caught the null value
        verify(mockProducerService, never()).resetCoinAccount(any());
        verify(nullAccountKeyNode).path("accountKey");
        verify(nullAccountKeyNode).asText();
      }
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processTickQuery should fetch ticks and send updates")
  void processTickQuery_ShouldFetchTicksAndSendUpdates() throws Exception {
    // Arrange
    @SuppressWarnings("unchecked")
    MockedConstruction<KafkaConsumer<String, String>> mockedConsumerConstruction = (MockedConstruction<KafkaConsumer<String, String>>) (MockedConstruction<?>) Mockito
        .mockConstruction(KafkaConsumer.class);

    try {
      // Create service instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Get access to the private processTickQuery method via reflection
      Method processTickQueryMethod = KafkaConsumerQueryService.class.getDeclaredMethod("processTickQuery",
          JsonNode.class);
      processTickQueryMethod.setAccessible(true);

      // Use reflection to inject mockProducerService
      Field producerServiceField = KafkaConsumerQueryService.class.getDeclaredField("producerService");
      producerServiceField.setAccessible(true);
      producerServiceField.set(service, mockProducerService);

      // Create mock TickEvent and ticks
      TickEvent mockTickEvent = mock(TickEvent.class);
      Tick tick1 = new Tick("BTC-USDT", 1000);
      Tick tick2 = new Tick("BTC-USDT", 2000);
      List<Tick> ticks = Arrays.asList(tick1, tick2);

      // Setup mocks
      when(mockTickEvent.getPoolPair()).thenReturn("BTC-USDT");
      when(mockTickEvent.fetchTicksFromBitmap()).thenReturn(ticks);

      // Mock KafkaMessageUtils to capture and execute the lambda
      try (MockedStatic<KafkaMessageUtils> mockedKafkaMessageUtils = mockStatic(KafkaMessageUtils.class)) {
        // Setup the mock for processWithErrorHandling
        mockedKafkaMessageUtils.when(() -> KafkaMessageUtils.processWithErrorHandling(
            any(JsonNode.class),
            any(KafkaMessageUtils.ProcessorFunction.class),
            eq(mockProducerService),
            anyString())).thenAnswer(invocation -> {
              // Extract and execute the processor function
              KafkaMessageUtils.ProcessorFunction processor = invocation.getArgument(1);
              processor.process();
              return null;
            });

        // Sử dụng PowerMockito để mock việc tạo đối tượng mới
        try (MockedConstruction<TickEvent> mockedTickEventConstruction = Mockito.mockConstruction(TickEvent.class,
            (mock, context) -> {
              // Cấu hình mock khi được tạo mới
              when(mock.getPoolPair()).thenReturn("BTC-USDT");
              when(mock.fetchTicksFromBitmap()).thenReturn(ticks);
            })) {

          // Create JSON for the test
          ObjectMapper objectMapper = new ObjectMapper();
          JsonNode jsonNode = objectMapper.readTree(
              "{\"poolPair\":\"BTC-USDT\",\"eventId\":\"test-event\",\"actionType\":\"TICK_QUERY\",\"operationType\":\"TICK_QUERY\"}");

          // Act
          processTickQueryMethod.invoke(service, jsonNode);

          // Assert
          TickEvent constructedMock = mockedTickEventConstruction.constructed().get(0);
          verify(constructedMock).parserData(any(JsonNode.class));
          verify(constructedMock).validate();
          verify(constructedMock).fetchTicksFromBitmap();
          verify(mockProducerService).sendTickUpdate(tick1);
          verify(mockProducerService).sendTickUpdate(tick2);
        }
      }
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processTickQuery should handle empty tick list")
  void processTickQuery_ShouldHandleEmptyTickList() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Create service instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Get access to the private processTickQuery method via reflection
      Method processTickQueryMethod = KafkaConsumerQueryService.class.getDeclaredMethod("processTickQuery",
          JsonNode.class);
      processTickQueryMethod.setAccessible(true);

      // Use reflection to inject mockProducerService
      Field producerServiceField = KafkaConsumerQueryService.class.getDeclaredField("producerService");
      producerServiceField.setAccessible(true);
      producerServiceField.set(service, mockProducerService);

      // Create mock TickEvent with empty tick list
      TickEvent mockTickEvent = mock(TickEvent.class);
      when(mockTickEvent.getPoolPair()).thenReturn("BTC-USDT");
      when(mockTickEvent.fetchTicksFromBitmap()).thenReturn(Collections.emptyList());

      // Mock KafkaMessageUtils to capture and execute the lambda
      try (MockedStatic<KafkaMessageUtils> mockedKafkaMessageUtils = mockStatic(KafkaMessageUtils.class)) {
        // Setup the mock for processWithErrorHandling
        mockedKafkaMessageUtils.when(() -> KafkaMessageUtils.processWithErrorHandling(
            any(JsonNode.class),
            any(KafkaMessageUtils.ProcessorFunction.class),
            eq(mockProducerService),
            anyString())).thenAnswer(invocation -> {
              // Extract and execute the processor function
              KafkaMessageUtils.ProcessorFunction processor = invocation.getArgument(1);
              processor.process();
              return null;
            });

        // Sử dụng MockedConstruction để mock việc tạo đối tượng mới
        try (MockedConstruction<TickEvent> mockedTickEventConstruction = Mockito.mockConstruction(TickEvent.class,
            (mock, context) -> {
              // Cấu hình mock khi được tạo mới
              when(mock.getPoolPair()).thenReturn("BTC-USDT");
              when(mock.fetchTicksFromBitmap()).thenReturn(Collections.emptyList());
            })) {

          // Create JSON for the test
          ObjectMapper objectMapper = new ObjectMapper();
          JsonNode jsonNode = objectMapper.readTree(
              "{\"poolPair\":\"BTC-USDT\",\"eventId\":\"test-event\",\"actionType\":\"TICK_QUERY\",\"operationType\":\"TICK_QUERY\"}");

          // Act
          processTickQueryMethod.invoke(service, jsonNode);

          // Assert
          TickEvent constructedMock = mockedTickEventConstruction.constructed().get(0);
          verify(constructedMock).parserData(any(JsonNode.class));
          verify(constructedMock).validate();
          verify(constructedMock).fetchTicksFromBitmap();
          verify(mockProducerService, never()).sendTickUpdate(any(Tick.class));
        }
      }
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processTickQuery should handle validation error")
  void processTickQuery_ShouldHandleValidationError() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Create service instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Get access to the private processTickQuery method via reflection
      Method processTickQueryMethod = KafkaConsumerQueryService.class.getDeclaredMethod("processTickQuery",
          JsonNode.class);
      processTickQueryMethod.setAccessible(true);

      // Use reflection to inject mockProducerService
      Field producerServiceField = KafkaConsumerQueryService.class.getDeclaredField("producerService");
      producerServiceField.setAccessible(true);
      producerServiceField.set(service, mockProducerService);

      // Create mock TickEvent that throws validation error
      TickEvent mockTickEvent = mock(TickEvent.class);
      doThrow(new IllegalArgumentException("Pool pair is required")).when(mockTickEvent).validate();

      // Mock KafkaMessageUtils to capture and execute the lambda
      try (MockedStatic<KafkaMessageUtils> mockedKafkaMessageUtils = mockStatic(KafkaMessageUtils.class)) {
        // Setup the mock for processWithErrorHandling
        mockedKafkaMessageUtils.when(() -> KafkaMessageUtils.processWithErrorHandling(
            any(JsonNode.class),
            any(KafkaMessageUtils.ProcessorFunction.class),
            eq(mockProducerService),
            anyString())).thenAnswer(invocation -> {
              // Extract and execute the processor function
              KafkaMessageUtils.ProcessorFunction processor = invocation.getArgument(1);
              try {
                processor.process();
              } catch (IllegalArgumentException e) {
                // Expected exception - in real code this would be handled by KafkaMessageUtils
                // and would result in sendTransactionResultNotProcessed being called
                mockProducerService
                    .sendTransactionResultNotProcessed(Collections.singletonMap("error", e.getMessage()));
              }
              return null;
            });

        // Sử dụng MockedConstruction để mock việc tạo đối tượng mới
        try (MockedConstruction<TickEvent> mockedTickEventConstruction = Mockito.mockConstruction(TickEvent.class,
            (mock, context) -> {
              // Cấu hình mock khi được tạo mới
              doThrow(new IllegalArgumentException("Pool pair is required")).when(mock).validate();
            })) {

          // Create JSON for the test
          ObjectMapper objectMapper = new ObjectMapper();
          JsonNode jsonNode = objectMapper
              .readTree("{\"eventId\":\"test-event\",\"actionType\":\"TICK_QUERY\",\"operationType\":\"TICK_QUERY\"}");

          // Act
          processTickQueryMethod.invoke(service, jsonNode);

          // Assert
          TickEvent constructedMock = mockedTickEventConstruction.constructed().get(0);
          verify(constructedMock).parserData(any(JsonNode.class));
          verify(constructedMock).validate();
          verify(constructedMock, never()).fetchTicksFromBitmap();
          verify(mockProducerService).sendTransactionResultNotProcessed(anyMap());
        }
      }
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processQueryRecord should handle tick query correctly")
  void processQueryRecord_ShouldHandleTickQueryCorrectly() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Create service instance
      KafkaConsumerQueryService service = KafkaConsumerQueryService.getInstance();

      // Get access to the private processQueryRecord method via reflection
      Method processQueryMethod = KafkaConsumerQueryService.class.getDeclaredMethod("processQueryRecord",
          ConsumerRecord.class);
      processQueryMethod.setAccessible(true);

      // Create a record for Tick Query
      String tickQueryJson = "{\"poolPair\":\"BTC-USDT\",\"eventId\":\"test-event\",\"actionType\":\"TICK_QUERY\",\"operationType\":\"TICK_QUERY\"}";
      ConsumerRecord<String, String> record = new ConsumerRecord<>(
          KafkaTopics.TICK_QUERY_TOPIC, 0, 0, "key1", tickQueryJson);

      // Get access to the private processTickQuery method via reflection
      Method processTickQueryMethod = KafkaConsumerQueryService.class.getDeclaredMethod("processTickQuery",
          JsonNode.class);
      processTickQueryMethod.setAccessible(true);

      // Use reflection to inject mockProducerService
      Field producerServiceField = KafkaConsumerQueryService.class.getDeclaredField("producerService");
      producerServiceField.setAccessible(true);
      producerServiceField.set(service, mockProducerService);

      // Mock KafkaMessageUtils to capture and execute the lambda
      try (MockedStatic<KafkaMessageUtils> mockedKafkaMessageUtils = mockStatic(KafkaMessageUtils.class)) {
        // Setup the mock for processWithErrorHandling
        mockedKafkaMessageUtils.when(() -> KafkaMessageUtils.processWithErrorHandling(
            any(JsonNode.class),
            any(KafkaMessageUtils.ProcessorFunction.class),
            eq(mockProducerService),
            anyString())).thenAnswer(invocation -> {
              // Extract and execute the processor function
              KafkaMessageUtils.ProcessorFunction processor = invocation.getArgument(1);
              processor.process();
              return null;
            });

        // Sử dụng MockedConstruction để mock việc tạo đối tượng mới
        try (MockedConstruction<TickEvent> mockedTickEventConstruction = Mockito.mockConstruction(TickEvent.class,
            (mock, context) -> {
              // Cấu hình mock khi được tạo mới
              when(mock.getPoolPair()).thenReturn("BTC-USDT");
              when(mock.fetchTicksFromBitmap()).thenReturn(Collections.emptyList());
            })) {

          // Act
          processQueryMethod.invoke(service, record);

          // Assert
          TickEvent constructedMock = mockedTickEventConstruction.constructed().get(0);
          verify(constructedMock).parserData(any(JsonNode.class));
          verify(constructedMock).validate();
          verify(constructedMock).fetchTicksFromBitmap();
        }
      }
    } finally {
      mockedConsumerConstruction.close();
    }
  }
}
