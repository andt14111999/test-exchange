//Remove validate tests
package com.exchangeengine.messaging.consumer;

import com.exchangeengine.factory.event.AccountEventFactory;
import com.exchangeengine.factory.event.AmmPoolEventFactory;
import com.exchangeengine.factory.event.CoinDepositEventFactory;
import com.exchangeengine.factory.event.CoinWithdrawalEventFactory;
import com.exchangeengine.factory.event.AmmPositionEventFactory;
import com.exchangeengine.factory.event.BalancesLockEventFactory;
import com.exchangeengine.messaging.producer.KafkaProducerService;
import com.exchangeengine.model.KafkaTopics;
import com.exchangeengine.model.event.AmmPositionEvent;
import com.exchangeengine.model.event.AmmOrderEvent;
import com.exchangeengine.model.event.BalancesLockEvent;
import com.exchangeengine.service.engine.EngineHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.exchangeengine.model.event.AccountEvent;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KafkaConsumerServiceTest {

  @Mock
  private EngineHandler mockEngineHandler;

  @Mock
  private KafkaProducerService mockProducerService;

  @Mock
  private ConsumerRecords<String, String> mockConsumerRecords;

  private MockedStatic<EngineHandler> mockedEngineHandlerStatic;
  private MockedStatic<KafkaProducerService> mockedProducerServiceStatic;
  private MockedStatic<KafkaConsumerConfig> mockedKafkaConsumerConfigStatic;

  @BeforeEach
  void setUp() {
    // Reset singleton instance
    KafkaConsumerService.resetInstance();

    // Mock static methods
    mockedEngineHandlerStatic = mockStatic(EngineHandler.class);
    mockedProducerServiceStatic = mockStatic(KafkaProducerService.class);
    mockedKafkaConsumerConfigStatic = mockStatic(KafkaConsumerConfig.class);

    // Setup default behavior
    mockedEngineHandlerStatic.when(EngineHandler::getInstance).thenReturn(mockEngineHandler);
    mockedProducerServiceStatic.when(KafkaProducerService::getInstance).thenReturn(mockProducerService);

    // Mock KafkaConsumerConfig
    Properties mockProps = new Properties();
    mockedKafkaConsumerConfigStatic.when(() -> KafkaConsumerConfig.createConsumerConfig(
        anyString(), anyBoolean(), anyBoolean())).thenReturn(mockProps);
  }

  @AfterEach
  void tearDown() {
    // Close all static mocks
    if (mockedEngineHandlerStatic != null) {
      mockedEngineHandlerStatic.close();
    }
    if (mockedProducerServiceStatic != null) {
      mockedProducerServiceStatic.close();
    }
    if (mockedKafkaConsumerConfigStatic != null) {
      mockedKafkaConsumerConfigStatic.close();
    }
  }

  @Test
  @DisplayName("getInstance should return singleton instance")
  void getInstance_ShouldReturnSingletonInstance() {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Act
      KafkaConsumerService instance1 = KafkaConsumerService.getInstance();
      KafkaConsumerService instance2 = KafkaConsumerService.getInstance();

      // Assert
      assertNotNull(instance1, "Instance should not be null");
      assertSame(instance1, instance2, "getInstance should always return the same instance");
      assertEquals(1, mockedConsumerConstruction.constructed().size(),
          "KafkaConsumer should be constructed only once");
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("run should poll consumer and process records")
  void run_ShouldPollConsumerAndProcessRecords() throws Exception {
    // Arrange
    List<ConsumerRecord<String, String>> records = new ArrayList<>();

    String accountEventJson = AccountEventFactory.createJsonNode().toPrettyString();
    String depositEventJson = CoinDepositEventFactory.createJsonNode().toPrettyString();
    String withdrawalEventJson = CoinWithdrawalEventFactory.createJsonNode().toPrettyString();
    String ammPoolEventJson = AmmPoolEventFactory.createJsonNode().toPrettyString();
    String ammPositionEventJson = AmmPositionEventFactory.createJsonNode().toPrettyString();

    records.add(new ConsumerRecord<>(KafkaTopics.COIN_ACCOUNT_TOPIC, 0, 0, "key1", accountEventJson));
    records.add(new ConsumerRecord<>(KafkaTopics.COIN_DEPOSIT_TOPIC, 0, 1, "key2", depositEventJson));
    records.add(new ConsumerRecord<>(KafkaTopics.COIN_WITHDRAWAL_TOPIC, 0, 2, "key3", withdrawalEventJson));
    records.add(new ConsumerRecord<>(KafkaTopics.AMM_POOL_TOPIC, 0, 3, "key4", ammPoolEventJson));
    records.add(new ConsumerRecord<>(KafkaTopics.AMM_POSITION_TOPIC, 0, 4, "key5", ammPositionEventJson));

    when(mockConsumerRecords.iterator()).thenReturn(records.iterator());

    // Mock KafkaConsumer to return records, then empty records to exit the loop
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class,
        (mock, context) -> {
          when(mock.poll(any(Duration.class)))
              .thenReturn(mockConsumerRecords)
              .thenReturn(ConsumerRecords.empty());
        });

    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Set running field via reflection
      Field runningField = KafkaConsumerService.class.getDeclaredField("running");
      runningField.setAccessible(true);
      AtomicBoolean running = (AtomicBoolean) runningField.get(service);

      // Create thread to run service
      Thread thread = new Thread(service);

      // Act
      thread.start();

      // Wait for service to process records
      Thread.sleep(200);

      // Set running to false to exit the loop
      running.set(false);

      // Wait for thread to finish
      thread.join(500);

      // Assert
      assertFalse(thread.isAlive(), "Thread should have stopped");

      // Verify consumer.poll() was called at least once
      KafkaConsumer<String, String> consumer = mockedConsumerConstruction.constructed().get(0);
      verify(consumer, atLeastOnce()).poll(any(Duration.class));

      // Verify consumer.commitSync() was called after processing records
      verify(consumer, atLeastOnce()).commitSync();
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("shutdown should set running to false and wake up consumer")
  void shutdown_ShouldSetRunningToFalseAndWakeUpConsumer() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Act
      KafkaConsumerService service = KafkaConsumerService.getInstance();
      service.shutdown();

      // Assert
      // Verify consumer.wakeup() was called
      KafkaConsumer<String, String> consumer = mockedConsumerConstruction.constructed().get(0);
      verify(consumer).wakeup();

      // Verify running was set to false
      Field runningField = KafkaConsumerService.class.getDeclaredField("running");
      runningField.setAccessible(true);
      AtomicBoolean running = (AtomicBoolean) runningField.get(service);
      assertFalse(running.get(), "running should be set to false");
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("run should handle WakeupException when running is false")
  void run_ShouldHandleWakeupExceptionWhenRunningIsFalse() throws Exception {
    // Arrange
    // Mock KafkaConsumer to throw WakeupException when poll() is called
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class,
        (mock, context) -> {
          doThrow(new WakeupException())
              .when(mock).poll(any(Duration.class));
        });
    
    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();
      
      // Get reference to the actual consumer created via MockedConstruction
      KafkaConsumer<String, String> consumer = mockedConsumerConstruction.constructed().get(0);
      
      // Set running field to false
      Field runningField = KafkaConsumerService.class.getDeclaredField("running");
      runningField.setAccessible(true);
      runningField.set(service, new AtomicBoolean(false));
      
      // Act - Call run() directly instead of through a thread
      service.run();
      
      // Assert
      // Verify consumer.close() was called in the finally block
      verify(consumer).close();
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("run should handle general exceptions")
  void run_ShouldHandleGeneralExceptions() throws Exception {
    // Arrange
    // Mock KafkaConsumer to throw RuntimeException when poll() is called
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class,
        (mock, context) -> {
          when(mock.poll(any(Duration.class)))
              .thenThrow(new RuntimeException("Test exception"));
        });

    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Set running field to false after first poll
      Field runningField = KafkaConsumerService.class.getDeclaredField("running");
      runningField.setAccessible(true);
      AtomicBoolean running = new AtomicBoolean(true);
      runningField.set(service, running);

      // Create thread to run service
      Thread thread = new Thread(() -> {
        service.run();
      });

      // Act
      thread.start();

      // Wait for service to encounter exception
      Thread.sleep(200);

      // Stop thread if it's still running
      running.set(false);
      thread.join(500);

      // Assert
      assertFalse(thread.isAlive(), "Thread should have stopped");

      // Verify consumer.poll() was called
      KafkaConsumer<String, String> consumer = mockedConsumerConstruction.constructed().get(0);
      verify(consumer).poll(any(Duration.class));

      // Verify consumer.close() was called in the finally block
      verify(consumer).close();
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processRecord should handle exceptions when processing a record")
  void processRecord_ShouldHandleExceptionsWhenProcessingRecord() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    // Create invalid JSON that will cause an exception
    String invalidJson = "{invalid_json";
    List<ConsumerRecord<String, String>> records = new ArrayList<>();
    records.add(new ConsumerRecord<>(KafkaTopics.COIN_ACCOUNT_TOPIC, 0, 0, "key1", invalidJson));

    // Create ConsumerRecords with one invalid record
    when(mockConsumerRecords.iterator()).thenReturn(records.iterator());

    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Mock KafkaConsumer to return mockConsumerRecords then empty records
      KafkaConsumer<String, String> consumer = mockedConsumerConstruction.constructed().get(0);
      when(consumer.poll(any(Duration.class)))
          .thenReturn(mockConsumerRecords)
          .thenReturn(ConsumerRecords.empty());

      // Set running field to false after first poll
      Field runningField = KafkaConsumerService.class.getDeclaredField("running");
      runningField.setAccessible(true);
      AtomicBoolean running = new AtomicBoolean(true);
      runningField.set(service, running);

      // Create thread to run service
      Thread thread = new Thread(service);

      // Act
      thread.start();

      // Wait for service to process the invalid record
      Thread.sleep(200);

      // Stop the thread
      running.set(false);
      thread.join(500);

      // Assert
      assertFalse(thread.isAlive(), "Thread should have stopped");

      // Verify consumer.poll() was called
      verify(consumer, atLeastOnce()).poll(any(Duration.class));

      // In actual implementation, commitSync() is still called even when
      // processRecord throws an exception
      // because the try-catch block in the run method catches the exception and
      // allows the loop to continue
      verify(consumer, atLeastOnce()).commitSync();

    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("run should handle exceptions when processing records")
  void run_ShouldHandleProcessingExceptions() throws Exception {
    // Arrange
    // Setup mocked consumer records
    ConsumerRecord<String, String> mockRecord = mock(ConsumerRecord.class);
    when(mockRecord.topic()).thenReturn(KafkaTopics.COIN_ACCOUNT_TOPIC);
    when(mockRecord.value()).thenReturn("valid_json");

    List<ConsumerRecord<String, String>> recordsList = new ArrayList<>();
    recordsList.add(mockRecord);

    when(mockConsumerRecords.iterator()).thenReturn(recordsList.iterator());

    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(
        KafkaConsumer.class,
        (mock, context) -> {
          when(mock.poll(any(Duration.class))).thenReturn(mockConsumerRecords);
        });

    try {
      // Mock ObjectMapper to throw exception when readTree is called
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Use reflection to replace the objectMapper with a mock
      ObjectMapper mockMapper = mock(ObjectMapper.class);
      when(mockMapper.readTree(anyString())).thenThrow(new RuntimeException("Forced test exception"));

      Field objectMapperField = KafkaConsumerService.class.getDeclaredField("objectMapper");
      objectMapperField.setAccessible(true);
      objectMapperField.set(service, mockMapper);

      // Set running to be set to false after processing
      Field runningField = KafkaConsumerService.class.getDeclaredField("running");
      runningField.setAccessible(true);
      AtomicBoolean running = new AtomicBoolean(true);
      runningField.set(service, running);

      // Create a thread to run the service
      Thread thread = new Thread(() -> {
        service.run();
      });

      // Act
      thread.start();

      // Give some time for the thread to process records
      Thread.sleep(200);

      // Stop the thread
      running.set(false);
      thread.join(500);

      // Assert
      assertFalse(thread.isAlive(), "Thread should have stopped");

      // Verify interactions
      KafkaConsumer<String, String> consumer = mockedConsumerConstruction.constructed().get(0);
      verify(consumer, atLeastOnce()).poll(any(Duration.class));
      verify(consumer, atLeastOnce()).commitSync(); // Should be called even if processRecord throws

    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("run should catch exception in inner try-catch for record processing")
  void run_ShouldCatchExceptionInInnerTryCatch() throws Exception {
    // Arrange
    // Setup mocked consumer record
    ConsumerRecord<String, String> mockRecord = mock(ConsumerRecord.class);
    when(mockRecord.topic()).thenReturn(KafkaTopics.COIN_ACCOUNT_TOPIC);
    when(mockRecord.value()).thenReturn("{\"value\":\"test\"}");

    List<ConsumerRecord<String, String>> recordsList = new ArrayList<>();
    recordsList.add(mockRecord);

    when(mockConsumerRecords.iterator()).thenReturn(recordsList.iterator());

    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(
        KafkaConsumer.class,
        (mock, context) -> {
          when(mock.poll(any(Duration.class)))
              .thenReturn(mockConsumerRecords)
              .thenReturn(ConsumerRecords.empty());

          // Force exception in commitSync to trigger inner catch block
          doThrow(new RuntimeException("Test exception")).when(mock).commitSync();
        });

    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Set running to stop after processing
      Field runningField = KafkaConsumerService.class.getDeclaredField("running");
      runningField.setAccessible(true);
      AtomicBoolean running = new AtomicBoolean(true);
      runningField.set(service, running);

      // Create a thread to run the service
      Thread thread = new Thread(() -> {
        service.run();
      });

      // Act
      thread.start();

      // Wait for the service to process
      Thread.sleep(200);

      // Stop the thread
      running.set(false);
      thread.join(500);

      // Assert
      assertFalse(thread.isAlive(), "Thread should have stopped");

      // Verify interactions
      KafkaConsumer<String, String> consumer = mockedConsumerConstruction.constructed().get(0);
      verify(consumer, atLeastOnce()).poll(any(Duration.class));
      verify(consumer, atLeastOnce()).commitSync(); // Should be called

    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processMerchantEscrowRequest should process merchant escrow event correctly")
  void processMerchantEscrowRequest_ShouldProcessMerchantEscrowEventCorrectly() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Get access to the private method using reflection
      Method processMerchantEscrowRequestMethod = KafkaConsumerService.class.getDeclaredMethod(
          "processMerchantEscrowRequest", JsonNode.class);
      processMerchantEscrowRequestMethod.setAccessible(true);

      // Create a valid JSON node for merchant escrow event
      ObjectMapper objectMapper = new ObjectMapper();
      String merchantEscrowJson = "{" +
          "\"eventId\":\"test-event-id\"," +
          "\"actionType\":\"MERCHANT_ESCROW\"," +
          "\"actionId\":\"test-action-id\"," +
          "\"operationType\":\"MERCHANT_ESCROW_MINT\"," +
          "\"identifier\":\"test-escrow-1\"," +
          "\"usdtAccountKey\":\"usdt-account-1\"," +
          "\"fiatAccountKey\":\"fiat-account-1\"," +
          "\"usdtAmount\":\"100.00\"," +
          "\"fiatAmount\":\"1000000.00\"," +
          "\"fiatCurrency\":\"VND\"," +
          "\"userId\":\"test-user-1\"," +
          "\"merchantEscrowOperationId\":\"merchant-escrow-op-1\"" +
          "}";
      JsonNode messageJson = objectMapper.readTree(merchantEscrowJson);

      // Act
      processMerchantEscrowRequestMethod.invoke(service, messageJson);

      // Assert
      verify(mockEngineHandler).merchantEscrow(any());
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processMerchantEscrowRequest should handle MERCHANT_ESCROW_BURN operation")
  void processMerchantEscrowRequest_ShouldHandleMerchantEscrowBurnOperation() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Get access to the private method using reflection
      Method processMerchantEscrowRequestMethod = KafkaConsumerService.class.getDeclaredMethod(
          "processMerchantEscrowRequest", JsonNode.class);
      processMerchantEscrowRequestMethod.setAccessible(true);

      // Create a valid JSON node for merchant escrow event with BURN operation
      ObjectMapper objectMapper = new ObjectMapper();
      String merchantEscrowJson = "{" +
          "\"eventId\":\"test-event-id\"," +
          "\"actionType\":\"MERCHANT_ESCROW\"," +
          "\"actionId\":\"test-action-id\"," +
          "\"operationType\":\"MERCHANT_ESCROW_BURN\"," +
          "\"identifier\":\"test-escrow-1\"," +
          "\"usdtAccountKey\":\"usdt-account-1\"," +
          "\"fiatAccountKey\":\"fiat-account-1\"," +
          "\"usdtAmount\":\"100.00\"," +
          "\"fiatAmount\":\"1000000.00\"," +
          "\"fiatCurrency\":\"VND\"," +
          "\"userId\":\"test-user-1\"," +
          "\"merchantEscrowOperationId\":\"merchant-escrow-op-1\"" +
          "}";
      JsonNode messageJson = objectMapper.readTree(merchantEscrowJson);

      // Act
      processMerchantEscrowRequestMethod.invoke(service, messageJson);

      // Assert
      verify(mockEngineHandler).merchantEscrow(any());
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processMerchantEscrowRequest should handle validation exceptions")
  void processMerchantEscrowRequest_ShouldHandleValidationExceptions() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Get access to the private method using reflection
      Method processMerchantEscrowRequestMethod = KafkaConsumerService.class.getDeclaredMethod(
          "processMerchantEscrowRequest", JsonNode.class);
      processMerchantEscrowRequestMethod.setAccessible(true);

      // Create an invalid JSON node for merchant escrow event (missing required
      // fields)
      ObjectMapper objectMapper = new ObjectMapper();
      String invalidMerchantEscrowJson = "{" +
          "\"eventId\":\"test-event-id\"," +
          "\"actionType\":\"MERCHANT_ESCROW\"," +
          "\"actionId\":\"test-action-id\"" +
          "}";
      JsonNode messageJson = objectMapper.readTree(invalidMerchantEscrowJson);

      // Act
      processMerchantEscrowRequestMethod.invoke(service, messageJson);

      // Assert
      // Verify that KafkaProducerService was called to handle error
      verify(mockProducerService).sendTransactionResultNotProcessed(any(Map.class));

      // Verify that engineHandler.merchantEscrow was not called due to validation
      // error
      verify(mockEngineHandler, never()).merchantEscrow(any());
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processRecord should handle MERCHANT_ESCROW_TOPIC records")
  void processRecord_ShouldHandleMerchantEscrowTopicRecords() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Get access to the private processRecord method using reflection
      Method processRecordMethod = KafkaConsumerService.class.getDeclaredMethod(
          "processRecord", ConsumerRecord.class);
      processRecordMethod.setAccessible(true);

      // Create a valid merchant escrow JSON record
      String merchantEscrowJson = "{" +
          "\"eventId\":\"test-event-id\"," +
          "\"actionType\":\"MERCHANT_ESCROW\"," +
          "\"actionId\":\"test-action-id\"," +
          "\"operationType\":\"MERCHANT_ESCROW_MINT\"," +
          "\"identifier\":\"test-escrow-1\"," +
          "\"usdtAccountKey\":\"usdt-account-1\"," +
          "\"fiatAccountKey\":\"fiat-account-1\"," +
          "\"usdtAmount\":\"100.00\"," +
          "\"fiatAmount\":\"1000000.00\"," +
          "\"fiatCurrency\":\"VND\"," +
          "\"userId\":\"test-user-1\"," +
          "\"merchantEscrowOperationId\":\"merchant-escrow-op-1\"" +
          "}";
      ConsumerRecord<String, String> record = new ConsumerRecord<>(
          KafkaTopics.MERCHANT_ESCROW_TOPIC, 0, 0, "key", merchantEscrowJson);

      // Act
      processRecordMethod.invoke(service, record);

      // Assert
      verify(mockEngineHandler).merchantEscrow(any());
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("setTestInstance should set the singleton instance")
  void setTestInstance_ShouldSetSingletonInstance() {
    // Arrange
    KafkaConsumerService.resetInstance();
    KafkaConsumerService mockService = mock(KafkaConsumerService.class);

    // Act
    KafkaConsumerService.setTestInstance(mockService);
    KafkaConsumerService instance = KafkaConsumerService.getInstance();

    // Assert
    assertSame(mockService, instance, "setTestInstance should set the singleton instance");
  }

  @Test
  @DisplayName("processRecord should handle AMM position events correctly")
  void processRecord_ShouldHandleAmmPositionEventCorrectly() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Get access to the private processRecord method using reflection
      Method processRecordMethod = KafkaConsumerService.class.getDeclaredMethod(
          "processRecord", ConsumerRecord.class);
      processRecordMethod.setAccessible(true);

      // Create a valid AMM position JSON record
      String ammPositionJson = "{" +
          "\"eventId\":\"test-event-id\"," +
          "\"actionType\":\"AMM_POOL\"," +
          "\"actionId\":\"test-action-id\"," +
          "\"operationType\":\"AMM_POSITION_CREATE\"," +
          "\"identifier\":\"test-position-1\"," +
          "\"poolPair\":\"BTC-ETH\"," +
          "\"ownerAccountKey0\":\"btc-account-1\"," +
          "\"ownerAccountKey1\":\"eth-account-1\"," +
          "\"slippage\":\"0.01\"," +
          "\"amount0Initial\":\"1.5\"," +
          "\"amount1Initial\":\"20.0\"," +
          "\"tickLowerIndex\":\"-100\"," +
          "\"tickUpperIndex\":\"100\"" +
          "}";

      // Create a consumer record for AMM_POSITION_TOPIC
      ConsumerRecord<String, String> record = new ConsumerRecord<>(
          KafkaTopics.AMM_POSITION_TOPIC, 0, 0, "key", ammPositionJson);

      // Act & Assert
      // This should not throw exception
      processRecordMethod.invoke(service, record);
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processAmmPositionRequest should execute processWithErrorHandling correctly")
  void processAmmPositionRequest_ShouldExecuteProcessWithErrorHandlingCorrectly() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);
    MockedConstruction<AmmPositionEvent> mockedAmmPositionEventConstruction = Mockito.mockConstruction(
        AmmPositionEvent.class, (mock, context) -> {
          // Không cần làm gì khi AmmPositionEvent được tạo
        });

    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Get access to the method via reflection
      Method processAmmPositionRequestMethod = KafkaConsumerService.class.getDeclaredMethod(
          "processAmmPositionRequest", JsonNode.class);
      processAmmPositionRequestMethod.setAccessible(true);

      // Create valid AmmPosition JSON
      String ammPositionJson = "{" +
          "\"eventId\":\"test-event-id\"," +
          "\"actionType\":\"AMM_POOL\"," +
          "\"actionId\":\"test-action-id\"," +
          "\"operationType\":\"AMM_POSITION_CREATE\"," +
          "\"identifier\":\"test-position-1\"," +
          "\"poolPair\":\"BTC-ETH\"," +
          "\"ownerAccountKey0\":\"btc-account-1\"," +
          "\"ownerAccountKey1\":\"eth-account-1\"," +
          "\"slippage\":\"0.01\"," +
          "\"amount0Initial\":\"1.5\"," +
          "\"amount1Initial\":\"20.0\"," +
          "\"tickLowerIndex\":\"-100\"," +
          "\"tickUpperIndex\":\"100\"" +
          "}";
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode messageJson = objectMapper.readTree(ammPositionJson);

      // Act
      processAmmPositionRequestMethod.invoke(service, messageJson);

      // Assert
      // Verify AmmPositionEvent was created
      assertFalse(mockedAmmPositionEventConstruction.constructed().isEmpty(),
          "AmmPositionEvent should have been created");

      // Get the created AmmPositionEvent instance
      if (!mockedAmmPositionEventConstruction.constructed().isEmpty()) {
        AmmPositionEvent createdEvent = mockedAmmPositionEventConstruction.constructed().get(0);

        // Verify methods were called on the AmmPositionEvent
        verify(createdEvent).parserData(any(JsonNode.class));
        verify(createdEvent).validate();

        // Verify engineHandler was called with the event
        verify(mockEngineHandler).ammPosition(createdEvent);
      }
    } finally {
      mockedAmmPositionEventConstruction.close();
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processAmmOrderRequest should execute processWithErrorHandling correctly")
  void processAmmOrderRequest_ShouldExecuteProcessWithErrorHandlingCorrectly() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);
    MockedConstruction<AmmOrderEvent> mockedAmmOrderEventConstruction = Mockito.mockConstruction(
        AmmOrderEvent.class, (mock, context) -> {
          // Không cần làm gì khi AmmOrderEvent được tạo
        });

    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Get access to the method via reflection
      Method processAmmOrderRequestMethod = KafkaConsumerService.class.getDeclaredMethod(
          "processAmmOrderRequest", JsonNode.class);
      processAmmOrderRequestMethod.setAccessible(true);

      // Create valid AmmOrder JSON
      String ammOrderJson = "{" +
          "\"eventId\":\"test-event-id\"," +
          "\"actionType\":\"AMM_ORDER\"," +
          "\"actionId\":\"test-action-id\"," +
          "\"operationType\":\"AMM_ORDER_CREATE\"," +
          "\"identifier\":\"test-order-1\"," +
          "\"poolPair\":\"BTC-ETH\"," +
          "\"ownerAccountKey\":\"btc-account-1\"," +
          "\"amount\":\"1.5\"," +
          "\"price\":\"20000.0\"" +
          "}";
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode messageJson = objectMapper.readTree(ammOrderJson);

      // Act
      processAmmOrderRequestMethod.invoke(service, messageJson);

      // Assert
      // Verify AmmOrderEvent was created
      assertFalse(mockedAmmOrderEventConstruction.constructed().isEmpty(),
          "AmmOrderEvent should have been created");

      // Get the created AmmOrderEvent instance
      if (!mockedAmmOrderEventConstruction.constructed().isEmpty()) {
        AmmOrderEvent createdEvent = mockedAmmOrderEventConstruction.constructed().get(0);

        // Verify methods were called on the AmmOrderEvent
        verify(createdEvent).parserData(any(JsonNode.class));
        verify(createdEvent).validate();

        // Verify engineHandler was called with the event
        verify(mockEngineHandler).ammOrder(createdEvent);
      }
    } finally {
      mockedAmmOrderEventConstruction.close();
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processRecord should handle AMM order events correctly")
  void processRecord_ShouldHandleAmmOrderEventCorrectly() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Get access to the private processRecord method using reflection
      Method processRecordMethod = KafkaConsumerService.class.getDeclaredMethod(
          "processRecord", ConsumerRecord.class);
      processRecordMethod.setAccessible(true);

      // Create a valid AMM order JSON record
      String ammOrderJson = "{" +
          "\"eventId\":\"test-event-id\"," +
          "\"actionType\":\"AMM_ORDER\"," +
          "\"actionId\":\"test-action-id\"," +
          "\"operationType\":\"AMM_ORDER_CREATE\"," +
          "\"identifier\":\"test-order-1\"," +
          "\"poolPair\":\"BTC-ETH\"," +
          "\"ownerAccountKey\":\"btc-account-1\"," +
          "\"amount\":\"1.5\"," +
          "\"price\":\"20000.0\"" +
          "}";

      // Create a consumer record for AMM_ORDER_TOPIC
      ConsumerRecord<String, String> record = new ConsumerRecord<>(
          KafkaTopics.AMM_ORDER_TOPIC, 0, 0, "key", ammOrderJson);

      // Act & Assert
      // This should not throw exception
      processRecordMethod.invoke(service, record);
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processTradeRequest should process trade event correctly")
  void processTradeRequest_ShouldProcessTradeEventCorrectly() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);
    MockedConstruction<com.exchangeengine.model.event.TradeEvent> mockedTradeEventConstruction = Mockito.mockConstruction(
        com.exchangeengine.model.event.TradeEvent.class, (mock, context) -> {
          // Không cần làm gì khi TradeEvent được tạo
        });

    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Get access to the method via reflection
      Method processTradeRequestMethod = KafkaConsumerService.class.getDeclaredMethod(
          "processTradeRequest", JsonNode.class);
      processTradeRequestMethod.setAccessible(true);

      // Create valid Trade JSON
      String tradeJson = "{" +
          "\"eventId\":\"test-event-id\"," +
          "\"actionType\":\"TRADE\"," +
          "\"actionId\":\"test-action-id\"," +
          "\"operationType\":\"TRADE_CREATE\"," +
          "\"identifier\":\"test-trade-1\"," +
          "\"offerId\":\"test-offer-1\"," +
          "\"takerSide\":\"BUY\"," +
          "\"coinAmount\":\"1.0\"," +
          "\"price\":\"20000.0\"" +
          "}";
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode messageJson = objectMapper.readTree(tradeJson);

      // Act
      processTradeRequestMethod.invoke(service, messageJson);

      // Assert
      // Verify TradeEvent was created
      assertFalse(mockedTradeEventConstruction.constructed().isEmpty(),
          "TradeEvent should have been created");

      // Get the created TradeEvent instance
      if (!mockedTradeEventConstruction.constructed().isEmpty()) {
        com.exchangeengine.model.event.TradeEvent createdEvent = mockedTradeEventConstruction.constructed().get(0);

        // Verify methods were called on the TradeEvent
        verify(createdEvent).parserData(any(JsonNode.class));
        verify(createdEvent).validate();

        // Verify engineHandler was called with the event
        verify(mockEngineHandler).processTrade(createdEvent);
      }
    } finally {
      mockedTradeEventConstruction.close();
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processRecord should handle TRADE_TOPIC records")
  void processRecord_ShouldHandleTradeTopicRecords() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Get access to the private processRecord method using reflection
      Method processRecordMethod = KafkaConsumerService.class.getDeclaredMethod(
          "processRecord", ConsumerRecord.class);
      processRecordMethod.setAccessible(true);

      // Create a valid trade JSON record
      String tradeJson = "{" +
          "\"eventId\":\"test-event-id\"," +
          "\"actionType\":\"TRADE\"," +
          "\"actionId\":\"test-action-id\"," +
          "\"operationType\":\"TRADE_CREATE\"," +
          "\"identifier\":\"test-trade-1\"," +
          "\"offerId\":\"test-offer-1\"," +
          "\"takerSide\":\"BUY\"," +
          "\"coinAmount\":\"1.0\"," +
          "\"price\":\"20000.0\"" +
          "}";

      // Create a consumer record for TRADE_TOPIC
      ConsumerRecord<String, String> record = new ConsumerRecord<>(
          KafkaTopics.TRADE_TOPIC, 0, 0, "key", tradeJson);

      // Act & Assert
      // This should not throw exception
      processRecordMethod.invoke(service, record);
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processOfferRequest should process offer event correctly")
  void processOfferRequest_ShouldProcessOfferEventCorrectly() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);
    MockedConstruction<com.exchangeengine.model.event.OfferEvent> mockedOfferEventConstruction = Mockito.mockConstruction(
        com.exchangeengine.model.event.OfferEvent.class, (mock, context) -> {
          // Không cần làm gì khi OfferEvent được tạo
        });

    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Get access to the method via reflection
      Method processOfferRequestMethod = KafkaConsumerService.class.getDeclaredMethod(
          "processOfferRequest", JsonNode.class);
      processOfferRequestMethod.setAccessible(true);

      // Create valid Offer JSON
      String offerJson = "{" +
          "\"eventId\":\"test-event-id\"," +
          "\"actionType\":\"OFFER\"," +
          "\"actionId\":\"test-action-id\"," +
          "\"operationType\":\"OFFER_CREATE\"," +
          "\"identifier\":\"test-offer-1\"," +
          "\"userId\":\"user-id-1\"," +
          "\"offerType\":\"BUY\"," +
          "\"coinCurrency\":\"BTC\"," +
          "\"currency\":\"USD\"," +
          "\"price\":\"20000.0\"," +
          "\"minAmount\":\"0.01\"," +
          "\"maxAmount\":\"1.0\"," +
          "\"totalAmount\":\"1.0\"," +
          "\"paymentMethodId\":\"pm-123\"," +
          "\"paymentTime\":30," +
          "\"countryCode\":\"US\"," +
          "\"automatic\":true," +
          "\"online\":true," +
          "\"margin\":\"1.0\"" +
          "}";
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode messageJson = objectMapper.readTree(offerJson);

      // Act
      processOfferRequestMethod.invoke(service, messageJson);

      // Assert
      // Verify OfferEvent was created
      assertFalse(mockedOfferEventConstruction.constructed().isEmpty(),
          "OfferEvent should have been created");

      // Get the created OfferEvent instance
      if (!mockedOfferEventConstruction.constructed().isEmpty()) {
        com.exchangeengine.model.event.OfferEvent createdEvent = mockedOfferEventConstruction.constructed().get(0);

        // Verify methods were called on the OfferEvent
        verify(createdEvent).parserData(any(JsonNode.class));
        verify(createdEvent).validate();

        // Verify engineHandler was called with the event
        verify(mockEngineHandler).processOffer(createdEvent);
      }
    } finally {
      mockedOfferEventConstruction.close();
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processRecord should handle OFFER_TOPIC records")
  void processRecord_ShouldHandleOfferTopicRecords() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Get access to the private processRecord method using reflection
      Method processRecordMethod = KafkaConsumerService.class.getDeclaredMethod(
          "processRecord", ConsumerRecord.class);
      processRecordMethod.setAccessible(true);

      // Create a valid offer JSON record
      String offerJson = "{" +
          "\"eventId\":\"test-event-id\"," +
          "\"actionType\":\"OFFER\"," +
          "\"actionId\":\"test-action-id\"," +
          "\"operationType\":\"OFFER_CREATE\"," +
          "\"identifier\":\"test-offer-1\"," +
          "\"userId\":\"user-id-1\"," +
          "\"offerType\":\"BUY\"," +
          "\"coinCurrency\":\"BTC\"," +
          "\"currency\":\"USD\"," +
          "\"price\":\"20000.0\"," +
          "\"minAmount\":\"0.01\"," +
          "\"maxAmount\":\"1.0\"," +
          "\"totalAmount\":\"1.0\"," +
          "\"paymentMethodId\":\"pm-123\"," +
          "\"paymentTime\":30," +
          "\"countryCode\":\"US\"," +
          "\"automatic\":true," +
          "\"online\":true," +
          "\"margin\":\"1.0\"" +
          "}";

      // Create a consumer record for OFFER_TOPIC
      ConsumerRecord<String, String> record = new ConsumerRecord<>(
          KafkaTopics.OFFER_TOPIC, 0, 0, "key", offerJson);

      // Act & Assert
      // This should not throw exception
      processRecordMethod.invoke(service, record);
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processRecord should handle unsupported topic with warning")
  void processRecord_ShouldHandleUnsupportedTopicWithWarning() throws Exception {
    // Arrange
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = Mockito.mockConstruction(KafkaConsumer.class);

    try {
      // Initialize service
      KafkaConsumerService service = KafkaConsumerService.getInstance();

      // Get access to the private processRecord method using reflection
      Method processRecordMethod = KafkaConsumerService.class.getDeclaredMethod(
          "processRecord", ConsumerRecord.class);
      processRecordMethod.setAccessible(true);

      // Create an unsupported topic record
      String someJson = "{\"eventId\":\"test-event-id\"}";
      ConsumerRecord<String, String> record = new ConsumerRecord<>(
          "UNSUPPORTED_TOPIC", 0, 0, "key", someJson);

      // Act & Assert
      // This should not throw exception, but log a warning
      processRecordMethod.invoke(service, record);
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processBalancesLockRequest should process balances lock event correctly")
  void processBalancesLockRequest_ShouldProcessBalancesLockEventCorrectly() throws Exception {
    // Arrange
    JsonNode messageJson = BalancesLockEventFactory.createJsonNode();
    
    // Access private method via reflection
    Method processBalancesLockRequestMethod = KafkaConsumerService.class.getDeclaredMethod(
        "processBalancesLockRequest", JsonNode.class);
    processBalancesLockRequestMethod.setAccessible(true);
    
    // Create the service with the mocked dependencies
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = mockConstruction(KafkaConsumer.class);
    try {
      KafkaConsumerService service = KafkaConsumerService.getInstance();
      
      // Act
      processBalancesLockRequestMethod.invoke(service, messageJson);
      
      // Assert
      // Verify engineHandler.balancesLock was called with a BalancesLockEvent
      ArgumentCaptor<BalancesLockEvent> eventCaptor = ArgumentCaptor.forClass(BalancesLockEvent.class);
      verify(mockEngineHandler).balancesLock(eventCaptor.capture());
      
      BalancesLockEvent capturedEvent = eventCaptor.getValue();
      assertNotNull(capturedEvent);
      // Verify event fields were set correctly
      assertEquals(messageJson.get("eventId").asText(), capturedEvent.getEventId());
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processRecord should handle BALANCES_LOCK_TOPIC records")
  void processRecord_ShouldHandleBalancesLockTopicRecords() throws Exception {
    // Arrange
    JsonNode messageJson = BalancesLockEventFactory.createJsonNode();
    String messageString = messageJson.toString();
    
    // Create ConsumerRecord for BALANCES_LOCK_TOPIC
    ConsumerRecord<String, String> record = new ConsumerRecord<>(
        KafkaTopics.BALANCES_LOCK_TOPIC, 0, 0, "key", messageString);
    
    // Access processRecord method via reflection
    Method processRecordMethod = KafkaConsumerService.class.getDeclaredMethod(
        "processRecord", ConsumerRecord.class);
    processRecordMethod.setAccessible(true);
    
    // Create the service with mocked dependencies
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = mockConstruction(KafkaConsumer.class);
    try {
      KafkaConsumerService service = KafkaConsumerService.getInstance();
      
      // Act
      processRecordMethod.invoke(service, record);
      
      // Assert
      // Verify that the balancesLock method was called
      verify(mockEngineHandler).balancesLock(any(BalancesLockEvent.class));
    } finally {
      mockedConsumerConstruction.close();
    }
  }

  @Test
  @DisplayName("processBalancesLockRequest should handle validation exceptions")
  void processBalancesLockRequest_ShouldHandleValidationExceptions() throws Exception {
    // Arrange - Create a message that will trigger validation failure
    JsonNode messageJson = BalancesLockEventFactory.createJsonNode();
    
    // Access the private method via reflection
    Method processBalancesLockRequestMethod = KafkaConsumerService.class.getDeclaredMethod(
        "processBalancesLockRequest", JsonNode.class);
    processBalancesLockRequestMethod.setAccessible(true);
    
    // Use mocked construction for both KafkaConsumer and BalancesLockEvent
    try (MockedConstruction<BalancesLockEvent> mockedEventConstruction = 
         mockConstruction(BalancesLockEvent.class, (mock, context) -> {
           // Set up the mock to throw an exception during validation
           doThrow(new IllegalArgumentException("Validation failed")).when(mock).validate();
         });
         MockedConstruction<KafkaConsumer> mockedConsumerConstruction = 
         mockConstruction(KafkaConsumer.class)) {
      
      // Create the service
      KafkaConsumerService service = KafkaConsumerService.getInstance();
      
      // Act
      processBalancesLockRequestMethod.invoke(service, messageJson);
      
      // Assert
      // Verify that an error message was sent
      verify(mockProducerService).sendTransactionResultNotProcessed(any(Map.class));
      
      // Verify that the engineHandler method was never called due to validation exception
      verify(mockEngineHandler, never()).balancesLock(any(BalancesLockEvent.class));
    }
  }

  @Test
  @DisplayName("processCreateCoinAccountRequest should process account events correctly")
  void processCreateCoinAccountRequest_ShouldProcessAccountEventsCorrectly() throws Exception {
    // Arrange
    JsonNode messageJson = mock(JsonNode.class);
    
    // Set up a factory for AccountEvent mocks
    MockedConstruction<AccountEvent> mockedAccountEventConstruction = 
        mockConstruction(AccountEvent.class);
    
    // Create the service with mocked dependencies
    MockedConstruction<KafkaConsumer> mockedConsumerConstruction = 
        mockConstruction(KafkaConsumer.class);
    
    try {
      // Create the service
      KafkaConsumerService service = KafkaConsumerService.getInstance();
      
      // Access the private method via reflection
      Method processCreateCoinAccountRequestMethod = KafkaConsumerService.class.getDeclaredMethod(
          "processCreateCoinAccountRequest", JsonNode.class);
      processCreateCoinAccountRequestMethod.setAccessible(true);
      
      // Act
      processCreateCoinAccountRequestMethod.invoke(service, messageJson);
      
      // Assert
      // Verify AccountEvent was created and methods were called
      assertFalse(mockedAccountEventConstruction.constructed().isEmpty(),
          "AccountEvent should have been created");
      
      // Get the created AccountEvent instance
      AccountEvent createdEvent = mockedAccountEventConstruction.constructed().get(0);
      
      // Verify methods were called on the AccountEvent
      verify(createdEvent).parserData(messageJson);
      verify(createdEvent).validate();
      
      // Verify engineHandler was called with the event
      verify(mockEngineHandler).createCoinAccount(createdEvent);
    } finally {
      mockedAccountEventConstruction.close();
      mockedConsumerConstruction.close();
    }
  }
}
