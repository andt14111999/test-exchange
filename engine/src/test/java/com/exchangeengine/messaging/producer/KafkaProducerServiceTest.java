package com.exchangeengine.messaging.producer;

import com.exchangeengine.messaging.common.KafkaConfig;
import com.exchangeengine.model.*;
import com.exchangeengine.model.event.AccountEvent;
import com.exchangeengine.model.event.CoinWithdrawalEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.storage.StorageService;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.DepositCache;
import com.exchangeengine.storage.rocksdb.RocksDBService;
import com.exchangeengine.util.EnvManager;
import com.exchangeengine.factory.AmmPoolFactory;
import com.exchangeengine.factory.AmmPositionFactory;
import com.exchangeengine.factory.BalanceLockFactory;
import com.exchangeengine.factory.CoinWithdrawalFactory;
import com.exchangeengine.factory.ProcessResultFactory;
import com.exchangeengine.extension.CombinedTestExtension;
import com.exchangeengine.service.engine.OutputProcessor;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith({ MockitoExtension.class, CombinedTestExtension.class })
@MockitoSettings(strictness = Strictness.LENIENT)
class KafkaProducerServiceTest {
  @Mock
  private KafkaProducer<String, String> mockProducer;

  @Mock
  private KafkaConfig mockKafkaConfig;

  @Mock
  private StorageService mockStorageService;

  @Mock
  private EnvManager mockEnvManager;

  @Mock
  private RecordMetadata mockRecordMetadata;

  @Mock
  private AccountCache mockAccountCache;

  @Mock
  private DepositCache mockDepositCache;

  @Mock
  private RocksDBService mockRocksDBService;

  private KafkaProducerService kafkaProducerService;

  private Object[] staticMocks;

  @BeforeEach
  void setUp() throws Exception {
    // Mock environment and RocksDB before proceeding with anything else
    EnvManager.setTestEnvironment();

    // Mock RocksDBService trước khi nó được sử dụng
    RocksDBService.setTestInstance(mockRocksDBService);

    // Cấu hình mocks
    when(mockKafkaConfig.getProducer()).thenReturn(mockProducer);
    when(mockRecordMetadata.partition()).thenReturn(0);
    when(mockRecordMetadata.offset()).thenReturn(123L);

    // Mock StorageService caches
    when(mockStorageService.getAccountCache()).thenReturn(mockAccountCache);
    when(mockStorageService.getDepositCache()).thenReturn(mockDepositCache);

    // Set up KafkaProducer to return CompletableFuture with RecordMetadata
    CompletableFuture<RecordMetadata> future = CompletableFuture.completedFuture(mockRecordMetadata);
    when(mockProducer.send(any(ProducerRecord.class), any())).thenAnswer(invocation -> future);

    // Create KafkaProducerService instance via reflection
    Constructor<KafkaProducerService> constructor = KafkaProducerService.class
        .getDeclaredConstructor(KafkaProducer.class);
    constructor.setAccessible(true);
    kafkaProducerService = constructor.newInstance(mockProducer);

    // Set test instance as singleton to avoid real initialization
    KafkaProducerService.setTestInstance(kafkaProducerService);

    // Set dependencies via reflection
    Field storageServiceField = KafkaProducerService.class.getDeclaredField("storageService");
    storageServiceField.setAccessible(true);
    storageServiceField.set(kafkaProducerService, mockStorageService);

    // Use setter to replace EnvManager
    kafkaProducerService.setEnvManager(mockEnvManager);
  }

  @AfterEach
  void tearDown() {
    // Đảm bảo cleanup tài nguyên và các singleton
    CombinedTestExtension.resetAllInOrder();
  }

  @Test
  @DisplayName("getInstance should return a singleton instance")
  void getInstance_ShouldReturnSingletonInstance() {
    KafkaProducerService instance1 = KafkaProducerService.getInstance();
    KafkaProducerService instance2 = KafkaProducerService.getInstance();

    assertSame(instance1, instance2, "getInstance should always return the same instance");
  }

  @Test
  @DisplayName("sendCoinAccountUpdate should send correct message")
  void sendCoinAccountUpdate_ShouldSendCorrectMessage() {
    // Arrange
    String accountKey = "btc:user123";
    Account account = new Account(accountKey);
    account.setAvailableBalance(new BigDecimal("100.0"));
    account.setFrozenBalance(new BigDecimal("50.0"));

    // Act
    kafkaProducerService.sendCoinAccountUpdate("test-input-event-id", account);

    // Assert
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(mockProducer).send(recordCaptor.capture(), any());

    ProducerRecord<String, String> capturedRecord = recordCaptor.getValue();
    assertEquals(KafkaTopics.COIN_ACCOUNT_UPDATE_TOPIC, capturedRecord.topic(), "Topic should match");
    assertEquals("coin-account-" + accountKey, capturedRecord.key(), "Key should match account key");
    assertTrue(capturedRecord.value().contains("\"key\":\"" + accountKey + "\""), "Value should contain account key");
    assertTrue(capturedRecord.value().contains("\"availableBalance\":"), "Value should contain available balance");
    assertTrue(capturedRecord.value().contains("\"frozenBalance\":"), "Value should contain frozen balance");
  }

  @Test
  @DisplayName("sendCoinAccountBalance should send correct message")
  void sendCoinAccountBalance_ShouldSendCorrectMessage() {
    // Arrange
    String accountKey = "btc:user123";
    Account account = new Account(accountKey);
    account.setAvailableBalance(new BigDecimal("100.0"));
    account.setFrozenBalance(new BigDecimal("50.0"));

    when(mockStorageService.getAccountCache().getAccount(accountKey)).thenReturn(Optional.of(account));

    // Act
    kafkaProducerService.sendCoinAccountBalance(accountKey);

    // Assert
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(mockProducer).send(recordCaptor.capture(), any());

    ProducerRecord<String, String> capturedRecord = recordCaptor.getValue();
    assertEquals(KafkaTopics.COIN_ACCOUNT_UPDATE_TOPIC, capturedRecord.topic(), "Topic should match");
    assertEquals("coin-account-" + accountKey, capturedRecord.key(), "Key should match account key");
    assertTrue(capturedRecord.value().contains("\"key\":\"" + accountKey + "\""), "Value should contain account key");
    assertTrue(capturedRecord.value().contains("\"availableBalance\":"), "Value should contain available balance");
    assertTrue(capturedRecord.value().contains("\"frozenBalance\":"), "Value should contain frozen balance");
  }

  @Test
  @DisplayName("sendCoinAccountBalance should throw exception when account not found")
  void sendCoinAccountBalance_ShouldThrowException_WhenAccountNotFound() {
    // Arrange
    String accountKey = "btc:nonexistent";
    when(mockStorageService.getAccountCache().getAccount(accountKey)).thenReturn(Optional.empty());

    // Act & Assert
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      kafkaProducerService.sendCoinAccountBalance(accountKey);
    });
    assertTrue(exception.getMessage().contains("Account not found"),
        "Exception message should mention account not found");
  }

  @Test
  @DisplayName("sendTransactionResult should send correct message")
  void sendTransactionResult_ShouldSendCorrectMessage() {
    // Arrange
    String accountKey = "btc:user123";
    DisruptorEvent event = mock(DisruptorEvent.class);
    Map<String, Object> eventMessage = new HashMap<>();
    eventMessage.put("accountKey", accountKey);
    eventMessage.put("eventId", "evt123");

    // Mock the event behavior
    when(event.getProducerKey()).thenReturn(accountKey);
    when(event.toOperationObjectMessageJson()).thenReturn(eventMessage);

    // Act
    kafkaProducerService.sendTransactionResult(event);

    // Assert
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(mockProducer).send(recordCaptor.capture(), any());

    ProducerRecord<String, String> capturedRecord = recordCaptor.getValue();
    assertEquals(KafkaTopics.TRANSACTION_RESPONSE_TOPIC, capturedRecord.topic(), "Topic should match");
    assertEquals("transaction-result-" + accountKey, capturedRecord.key(),
        "Key should match account key");
  }

  @Test
  @DisplayName("resetCoinAccount should reset account in development environment")
  void resetCoinAccount_ShouldResetAccount_InDevelopmentEnvironment() {
    // Arrange
    String accountKey = "btc:user123";

    // Đặt môi trường thành "development" cho test case này
    when(mockEnvManager.get(eq("APP_ENV"), anyString())).thenReturn("development");

    // Act
    kafkaProducerService.resetCoinAccount(accountKey);

    // Assert
    verify(mockStorageService.getAccountCache()).resetAccount(accountKey);
  }

  @Test
  @DisplayName("resetCoinAccount should reset account in test environment")
  void resetCoinAccount_ShouldResetAccount_InTestEnvironment() {
    // Arrange
    String accountKey = "btc:user123";

    // Đặt môi trường thành "test" cho test case này
    when(mockEnvManager.get(eq("APP_ENV"), anyString())).thenReturn("test");

    // Act
    kafkaProducerService.resetCoinAccount(accountKey);

    // Assert
    verify(mockStorageService.getAccountCache()).resetAccount(accountKey);
  }

  @Test
  @DisplayName("resetCoinAccount should correctly handle production environment")
  void resetCoinAccount_ShouldCorrectlyHandle_ProductionEnvironment() {
    // Arrange - production environment
    String accountKey = "btc:user123";
    when(mockEnvManager.get(eq("APP_ENV"), anyString())).thenReturn("production");
    when(mockStorageService.getAccountCache()).thenReturn(mockAccountCache);

    // Act
    kafkaProducerService.resetCoinAccount(accountKey);

    // Assert - should not call resetAccount in production
    verify(mockAccountCache, never()).resetAccount(accountKey);
  }

  @Test
  @DisplayName("resetCoinAccount should correctly handle staging environment")
  void resetCoinAccount_ShouldCorrectlyHandle_StagingEnvironment() {
    // Arrange - staging environment
    String accountKey = "btc:user123";
    when(mockEnvManager.get(eq("APP_ENV"), anyString())).thenReturn("staging");
    when(mockStorageService.getAccountCache()).thenReturn(mockAccountCache);

    // Act
    kafkaProducerService.resetCoinAccount(accountKey);

    // Assert - should not call resetAccount in staging
    verify(mockAccountCache, never()).resetAccount(accountKey);
  }

  @Test
  @DisplayName("resetCoinAccount should properly handle different environment conditions")
  void resetCoinAccount_ShouldHandle_DifferentEnvironmentConditions() {
    // Test case with development environment
    String accountKey = "btc:user123";
    reset(mockEnvManager, mockStorageService, mockAccountCache);

    // Important: use anyString() for the first parameter and match exactly
    // "development" for the second
    when(mockEnvManager.get(eq("APP_ENV"), anyString())).thenReturn("development");
    when(mockStorageService.getAccountCache()).thenReturn(mockAccountCache);

    kafkaProducerService.resetCoinAccount(accountKey);

    verify(mockAccountCache).resetAccount(accountKey);
  }

  @Test
  @DisplayName("resetCoinAccount should handle different non-development environments")
  void resetCoinAccount_ShouldHandle_NonDevelopmentEnvironments() {
    // Arrange - set up non-development environments
    String[] nonDevEnvironments = { "production", "staging", "other" };

    for (String env : nonDevEnvironments) {
      // Reset mocks for each environment
      reset(mockEnvManager, mockStorageService, mockAccountCache);

      // Configure environment
      when(mockEnvManager.get(eq("APP_ENV"), anyString())).thenReturn(env);
      when(mockEnvManager.get(eq("APP_MODE"), anyString())).thenReturn(env);
      when(mockStorageService.getAccountCache()).thenReturn(mockAccountCache);

      // Act
      String accountKey = "btc:user123";
      kafkaProducerService.resetCoinAccount(accountKey);

      // Assert - for non-dev environments, resetAccount should not be called
      verify(mockAccountCache, never()).resetAccount(accountKey);
    }

    // Also test edge cases: null or empty environment
    String[] edgeCases = { null, "" };
    for (String env : edgeCases) {
      // Reset mocks for each environment
      reset(mockEnvManager, mockStorageService, mockAccountCache);

      // Configure environment
      when(mockEnvManager.get(eq("APP_ENV"), anyString())).thenReturn(env);
      when(mockEnvManager.get(eq("APP_MODE"), anyString())).thenReturn(env);
      when(mockStorageService.getAccountCache()).thenReturn(mockAccountCache);

      // Act
      String accountKey = "btc:user123";
      kafkaProducerService.resetCoinAccount(accountKey);

      // Assert - for null or empty env, resetAccount should not be called
      verify(mockAccountCache, never()).resetAccount(accountKey);
    }
  }

  @Test
  @DisplayName("sendTransactionResultNotProcessed should send correct message")
  void sendTransactionResultNotProcessed_ShouldSendCorrectMessage() {
    // Arrange
    Map<String, Object> message = new HashMap<>();
    message.put("isSuccess", false);
    message.put("errorMessage", "Error processing transaction");
    message.put("messageId", "msg123");

    // Act
    kafkaProducerService.sendTransactionResultNotProcessed(message);

    // Assert
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(mockProducer).send(recordCaptor.capture(), any());

    ProducerRecord<String, String> capturedRecord = recordCaptor.getValue();
    assertEquals(KafkaTopics.TRANSACTION_RESPONSE_TOPIC, capturedRecord.topic(), "Topic should match");
    assertTrue(capturedRecord.key().startsWith("error-"), "Key should start with error-");
    assertTrue(capturedRecord.value().contains("\"isSuccess\":false"), "Value should indicate failure");
    assertTrue(capturedRecord.value().contains("\"errorMessage\":\"Error processing transaction\""),
        "Value should contain error message");
  }

  @Test
  @DisplayName("sendEventToKafka should add messageId if not present")
  void sendEventToKafka_ShouldAddMessageId_IfNotPresent() {
    // Arrange
    Map<String, Object> message = new HashMap<>();
    message.put("key", "value");
    // Cần thiết messageId để chắc chắn nó không có trước khi gọi

    // Act
    kafkaProducerService.sendTransactionResultNotProcessed(message);

    // Assert
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(mockProducer).send(recordCaptor.capture(), any());

    // Kiểm tra messageId được thêm vào message
    String capturedValue = recordCaptor.getValue().value();
    assertTrue(capturedValue.contains("\"messageId\":"), "MessageId should be added to message");
  }

  @Test
  @DisplayName("sendEventToKafka should handle exception in Kafka callback")
  void sendEventToKafka_ShouldHandleException_InKafkaCallback() {
    // Arrange
    Map<String, Object> message = new HashMap<>();
    message.put("key", "value");

    // Setup mock để execute callback với exception
    CompletableFuture<RecordMetadata> future = CompletableFuture.completedFuture(mockRecordMetadata);
    when(mockProducer.send(any(ProducerRecord.class), any())).thenAnswer(invocation -> {
      Callback callback = invocation.getArgument(1);
      Exception testException = new RuntimeException("Kafka send error");
      callback.onCompletion(null, testException);
      return future;
    });

    // Act - sử dụng sendTransactionResultNotProcessed để kích hoạt sendEventToKafka
    // bên trong
    kafkaProducerService.sendTransactionResultNotProcessed(message);

    // Assert - producer.send được gọi 3 lần (1 lần ban đầu + 2 lần retry)
    verify(mockProducer, times(3)).send(any(ProducerRecord.class), any());
  }

  @Test
  @DisplayName("close should not close shared producer")
  void close_ShouldNotCloseSharedProducer() {
    // Act
    kafkaProducerService.close();

    // Assert - verify that close was never called on the producer since it's shared
    verify(mockProducer, never()).close();
  }

  @Test
  @DisplayName("sendTickUpdate should send tick to Kafka")
  void sendTickUpdate_ShouldSendTickToKafka() {
    // Arrange
    Tick tick = new Tick("BTC-USDT", 1000);
    tick.setLiquidityGross(new BigDecimal("100"));
    tick.setLiquidityNet(new BigDecimal("50"));

    // Act
    kafkaProducerService.sendTickUpdate(tick);

    // Assert
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(mockProducer).send(recordCaptor.capture(), any());

    ProducerRecord<String, String> capturedRecord = recordCaptor.getValue();
    assertEquals(KafkaTopics.TICK_UPDATE_TOPIC, capturedRecord.topic(), "Topic should match");
    assertEquals("tick-update-" + tick.getTickKey(), capturedRecord.key(), "Key should match tick key");
    assertTrue(capturedRecord.value().contains("\"poolPair\":\"BTC-USDT\""), "Value should contain poolPair");
    assertTrue(capturedRecord.value().contains("\"tickIndex\":1000"), "Value should contain tickIndex");
  }

  @Test
  @DisplayName("sendTickUpdate should not send when tick is null")
  void sendTickUpdate_ShouldNotSend_WhenTickIsNull() {
    // Act
    kafkaProducerService.sendTickUpdate(null);

    // Assert
    verify(mockProducer, never()).send(any(), any());
  }

  @Test
  @DisplayName("sendEventToKafka should handle callback when exception is null")
  void sendEventToKafka_ShouldHandleCallback_WhenExceptionIsNull() {
    // Arrange
    Map<String, Object> message = new HashMap<>();
    message.put("key", "value");

    // Setup mock để execute callback với exception = null (thành công)
    CompletableFuture<RecordMetadata> future = CompletableFuture.completedFuture(mockRecordMetadata);
    when(mockProducer.send(any(ProducerRecord.class), any())).thenAnswer(invocation -> {
      Callback callback = invocation.getArgument(1);
      // Gọi callback với exception = null (thành công)
      callback.onCompletion(mockRecordMetadata, null);
      return future;
    });

    // Act - sử dụng public API để gián tiếp gọi sendEventToKafka
    kafkaProducerService.sendTransactionResultNotProcessed(message);

    // Assert - chỉ gọi send một lần vì không có retry
    verify(mockProducer, times(1)).send(any(ProducerRecord.class), any());
  }

  @Test
  @DisplayName("resetCoinAccount should handle all environment values")
  void resetCoinAccount_ShouldHandleAll_EnvironmentValues() {
    // Create a map of test scenarios: environment -> should call resetAccount
    Map<String, Boolean> testCases = new HashMap<>();
    testCases.put("development", true); // Should call resetAccount
    testCases.put("test", true); // Should call resetAccount
    testCases.put("production", false); // Should NOT call resetAccount
    testCases.put("staging", false); // Should NOT call resetAccount
    testCases.put(null, false); // Should NOT call resetAccount (edge case)
    testCases.put("", false); // Should NOT call resetAccount (edge case)

    String accountKey = "btc:user123";

    for (Map.Entry<String, Boolean> testCase : testCases.entrySet()) {
      // Reset mocks for each iteration
      reset(mockStorageService, mockAccountCache);

      // Configure the environment
      String environment = testCase.getKey();
      boolean shouldCallResetAccount = testCase.getValue();

      // Mock EnvManager
      when(mockEnvManager.get(eq("APP_ENV"), anyString())).thenReturn(environment);
      when(mockStorageService.getAccountCache()).thenReturn(mockAccountCache);

      // Act
      kafkaProducerService.resetCoinAccount(accountKey);

      // Assert - verify correct behavior based on environment
      if (shouldCallResetAccount) {
        verify(mockAccountCache, times(1)).resetAccount(accountKey);
      } else {
        verify(mockAccountCache, never()).resetAccount(accountKey);
      }
    }
  }

  @Test
  @DisplayName("sendTransactionResult should handle null event gracefully")
  void sendTransactionResult_ShouldNotSendMessage_WhenEventIsNull() {
    // Act
    kafkaProducerService.sendTransactionResult(null);

    // Assert - should not call producer.send for null event
    verify(mockProducer, never()).send(any(ProducerRecord.class), any());
  }

  @Test
  @DisplayName("sendTransactionResult should send message even with empty event")
  void sendTransactionResult_ShouldSendMessage_WithEmptyEvent() {
    // Arrange
    DisruptorEvent event = new DisruptorEvent();
    // No events set

    // Act
    kafkaProducerService.sendTransactionResult(event);

    // Assert - producer.send should be called even with empty event
    verify(mockProducer).send(any(ProducerRecord.class), any());

    // Capture the record to verify its content
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(mockProducer).send(recordCaptor.capture(), any());

    ProducerRecord<String, String> capturedRecord = recordCaptor.getValue();
    assertEquals(KafkaTopics.TRANSACTION_RESPONSE_TOPIC, capturedRecord.topic(), "Topic should match");
    assertTrue(capturedRecord.key().startsWith("transaction-result-"), "Key should start with transaction-result-");
  }

  @Test
  @DisplayName("getEnvManager should return the envManager instance")
  void getEnvManager_ShouldReturnEnvManagerInstance() {
    // Act
    EnvManager result = kafkaProducerService.getEnvManager();

    // Assert
    assertSame(mockEnvManager, result, "getEnvManager should return the correct EnvManager instance");
  }

  @Test
  @DisplayName("sendEventToKafka should handle successful retry after exception")
  void sendEventToKafka_ShouldHandleSuccessfulRetry_AfterException() {
    // Arrange
    Map<String, Object> message = new HashMap<>();
    message.put("key", "value");

    // Track callback invocations
    AtomicInteger callCount = new AtomicInteger(0);

    // Setup mock để simulate exception in first send, then success in retry
    CompletableFuture<RecordMetadata> future = CompletableFuture.completedFuture(mockRecordMetadata);
    when(mockProducer.send(any(ProducerRecord.class), any())).thenAnswer(invocation -> {
      Callback callback = invocation.getArgument(1);
      int count = callCount.getAndIncrement();

      if (count == 0) {
        // First call - simulate error
        callback.onCompletion(null, new RuntimeException("Kafka send error"));
      } else if (count == 1) {
        // Second call (first retry) - simulate success
        callback.onCompletion(mockRecordMetadata, null);
      } else {
        // Ở đây chúng ta không gọi thêm callback nếu đã vượt quá số lần dự kiến
        // Do producer.send() có thể được gọi nhiều lần trong retry loop
      }
      return future;
    });

    // Act - use public API to indirectly call sendEventToKafka
    kafkaProducerService.sendTransactionResultNotProcessed(message);

    // Assert - send should be called at least twice (original + at least 1 retry)
    verify(mockProducer, atLeast(2)).send(any(ProducerRecord.class), any());
  }

  @Test
  @DisplayName("sendEventToKafka should handle exceptions in future.get()")
  void sendEventToKafka_ShouldHandleExceptions_InFutureGet() {
    // Arrange
    Map<String, Object> message = new HashMap<>();
    message.put("key", "value");

    // Setup mock để throw exception from future.get()
    CompletableFuture<RecordMetadata> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new ExecutionException(new RuntimeException("Future.get error")));

    when(mockProducer.send(any(ProducerRecord.class), any())).thenReturn(failedFuture);

    // Act - use public API to indirectly call sendEventToKafka
    kafkaProducerService.sendTransactionResultNotProcessed(message);

    // Assert - method should complete without throwing exception
    // and the exception should be logged (we can't verify logging directly in this
    // test)
    verify(mockProducer).send(any(ProducerRecord.class), any());
  }

  @Test
  @DisplayName("sendAmmPoolUpdate should send correct message")
  void sendAmmPoolUpdate_ShouldSendCorrectMessage() {
    // Arrange
    ProcessResult result = ProcessResultFactory.createFullResult();
    result.setAmmPool(AmmPoolFactory.createDefaultAmmPool());

    // Act
    kafkaProducerService.sendAmmPoolUpdate(result);

    // Assert
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(mockProducer).send(recordCaptor.capture(), any());

    ProducerRecord<String, String> capturedRecord = recordCaptor.getValue();
    assertEquals(KafkaTopics.AMM_POOL_UPDATE_TOPIC, capturedRecord.topic(), "Topic should match");
    assertEquals("amm-pool-USDT/VND", capturedRecord.key(), "Key should match pool pair");
  }

  @Test
  @DisplayName("sendAmmPositionUpdate should send correct message")
  void sendAmmPositionUpdate_ShouldSendCorrectMessage() {
    // Arrange
    ProcessResult result = ProcessResultFactory.createFullResult();
    AmmPosition ammPosition = AmmPositionFactory.createCustomAmmPosition(Map.of("identifier", "test-position-id"));
    result.setAmmPosition(ammPosition);

    // Act
    kafkaProducerService.sendAmmPositionUpdate(result);

    // Assert
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(mockProducer).send(recordCaptor.capture(), any());

    ProducerRecord<String, String> capturedRecord = recordCaptor.getValue();
    assertEquals(KafkaTopics.AMM_POSITION_UPDATE_TOPIC, capturedRecord.topic(), "Topic should match");
    assertEquals("amm-position-test-position-id", capturedRecord.key(), "Key should match position identifier");
  }

  @Test
  @DisplayName("sendEventToKafka should handle null message")
  void sendEventToKafka_ShouldHandleNullMessage() throws Exception {
    // Sử dụng reflection để truy cập phương thức private sendEventToKafka
    Method sendEventToKafkaMethod = KafkaProducerService.class.getDeclaredMethod(
        "sendEventToKafka", String.class, String.class, Map.class);
    sendEventToKafkaMethod.setAccessible(true);

    // Act
    sendEventToKafkaMethod.invoke(kafkaProducerService, "test-topic", "test-key", null);

    // Assert - producer.send should not be called with null message
    verify(mockProducer, never()).send(any(ProducerRecord.class), any());
  }

  @Test
  @DisplayName("generateTransactionResultMessageJson should handle exception")
  void generateTransactionResultMessageJson_ShouldHandleException() throws Exception {
    // Arrange
    DisruptorEvent event = mock(DisruptorEvent.class);
    when(event.toOperationObjectMessageJson()).thenThrow(new RuntimeException("Test exception"));

    // Use reflection to access private method
    Method generateTransactionResultMessageJsonMethod = KafkaProducerService.class
        .getDeclaredMethod("generateTransactionResultMessageJson", DisruptorEvent.class);
    generateTransactionResultMessageJsonMethod.setAccessible(true);

    // Act
    Map<String, Object> result = (Map<String, Object>) generateTransactionResultMessageJsonMethod
        .invoke(kafkaProducerService, event);

    // Assert
    assertNull(result, "Method should return null when exception occurs");
  }

  /**
   * Test for sendMerchantEscrowUpdate method
   */
  @Test
  @DisplayName("sendMerchantEscrowUpdate should send correct message")
  void sendMerchantEscrowUpdate_ShouldSendCorrectMessage() {
    // Arrange
    String identifier = "merchant-escrow-123";
    ProcessResult result = mock(ProcessResult.class);
    MerchantEscrow merchantEscrow = mock(MerchantEscrow.class);
    Map<String, Object> messageJson = new HashMap<>();
    messageJson.put("identifier", identifier);
    messageJson.put("messageId", "msg123");

    when(merchantEscrow.getIdentifier()).thenReturn(identifier);
    when(result.getMerchantEscrow()).thenReturn(Optional.of(merchantEscrow));
    when(result.toMerchantEscrowObjectMessageJson()).thenReturn(messageJson);

    // Act
    kafkaProducerService.sendMerchantEscrowUpdate(result);

    // Assert
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(mockProducer).send(recordCaptor.capture(), any());

    ProducerRecord<String, String> capturedRecord = recordCaptor.getValue();
    assertEquals(KafkaTopics.MERCHANT_ESCROW_UPDATE_TOPIC, capturedRecord.topic(), "Topic should match");
    assertEquals("merchant-escrow-" + identifier, capturedRecord.key(), "Key should match identifier");
    assertTrue(capturedRecord.value().contains("\"identifier\":\"" + identifier + "\""),
        "Value should contain identifier");
  }

  /**
   * Test for sendMerchantEscrowUpdate with exception
   */
  @Test
  @DisplayName("sendMerchantEscrowUpdate should handle exception gracefully")
  void sendMerchantEscrowUpdate_ShouldHandleExceptionGracefully() {
    // Arrange
    ProcessResult result = mock(ProcessResult.class);
    MerchantEscrow merchantEscrow = mock(MerchantEscrow.class);

    when(result.getMerchantEscrow()).thenReturn(Optional.of(merchantEscrow));
    when(result.toMerchantEscrowObjectMessageJson()).thenThrow(new RuntimeException("Test exception"));

    // Act - Should not throw exception
    kafkaProducerService.sendMerchantEscrowUpdate(result);

    // Assert - No exception thrown and no message sent
    verify(mockProducer, never()).send(any(), any());
  }

  /**
   * Test for setTestInstance static method
   */
  @Test
  @DisplayName("setTestInstance should set the singleton instance")
  void setTestInstance_ShouldSetSingletonInstance() {
    // Arrange
    KafkaProducerService testInstance = mock(KafkaProducerService.class);

    // Act
    KafkaProducerService.setTestInstance(testInstance);
    KafkaProducerService instance = KafkaProducerService.getInstance();

    // Assert
    assertSame(testInstance, instance, "getInstance should return the test instance");
  }

  /**
   * Test for resetInstance static method
   */
  @Test
  @DisplayName("resetInstance should reset the singleton instance")
  void resetInstance_ShouldResetSingletonInstance() throws Exception {
    // Arrange - Create mock instances
    KafkaProducerService testInstance1 = mock(KafkaProducerService.class);
    KafkaConfig mockConfig = mock(KafkaConfig.class);
    KafkaProducer<String, String> mockKafkaProducer = mock(KafkaProducer.class);

    // Use static mocking for KafkaConfig
    try (MockedStatic<KafkaConfig> mockedKafkaConfig = mockStatic(KafkaConfig.class)) {
      // Configure mock
      mockedKafkaConfig.when(KafkaConfig::getInstance).thenReturn(mockConfig);
      when(mockConfig.getProducer()).thenReturn(mockKafkaProducer);

      // Set the test instance
      KafkaProducerService.setTestInstance(testInstance1);

      // Act - Reset the instance
      KafkaProducerService.resetInstance();

      // Get a new instance - this should create a new one
      KafkaProducerService instance = KafkaProducerService.getInstance();

      // Assert
      assertNotSame(testInstance1, instance, "getInstance should create a new instance after reset");
    }
  }

  /**
   * Test for exception in sendEventToKafka when serializing message
   */
  @Test
  @DisplayName("sendEventToKafka should handle exception when serializing message")
  void sendEventToKafka_ShouldHandleException_WhenSerializingMessage() throws Exception {
    // Arrange
    String topic = "test-topic";
    String key = "test-key";
    Map<String, Object> message = mock(Map.class);

    // Create custom ObjectMapper that throws exception when writeValueAsString is
    // called
    Field objectMapperField = KafkaProducerService.class.getDeclaredField("objectMapper");
    objectMapperField.setAccessible(true);

    // Store original object mapper
    Object originalObjectMapper = objectMapperField.get(kafkaProducerService);

    try {
      // Replace with mocked object mapper
      com.fasterxml.jackson.databind.ObjectMapper mockObjectMapper = mock(
          com.fasterxml.jackson.databind.ObjectMapper.class);
      when(mockObjectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization error"));
      objectMapperField.set(kafkaProducerService, mockObjectMapper);

      // Invoke private method using reflection
      Method sendEventToKafkaMethod = KafkaProducerService.class.getDeclaredMethod(
          "sendEventToKafka", String.class, String.class, Map.class);
      sendEventToKafkaMethod.setAccessible(true);

      // Act
      sendEventToKafkaMethod.invoke(kafkaProducerService, topic, key, message);

      // Assert - No exception thrown
      verify(mockProducer, never()).send(any(), any());

    } finally {
      // Restore original object mapper
      objectMapperField.set(kafkaProducerService, originalObjectMapper);
    }
  }

  /**
   * Test for getInstance when KafkaConfig throws an exception
   */
  @Test
  @DisplayName("getInstance should handle KafkaConfig exception")
  void getInstance_ShouldHandleKafkaConfigException() {
    // Arrange - First reset the instance
    KafkaProducerService.resetInstance();

    // Use static mocking for KafkaConfig
    try (MockedStatic<KafkaConfig> mockedKafkaConfig = mockStatic(KafkaConfig.class)) {
      // Configure mock to throw exception
      mockedKafkaConfig.when(KafkaConfig::getInstance).thenThrow(new RuntimeException("KafkaConfig error"));

      // Act & Assert
      assertThrows(RuntimeException.class, () -> {
        KafkaProducerService.getInstance();
      });
    }
  }

  /**
   * Test for generateBalanceUpdateMessageJson when account not found
   */
  @Test
  @DisplayName("generateBalanceUpdateMessageJson should throw exception when account not found")
  void generateBalanceUpdateMessageJson_ShouldThrowException_WhenAccountNotFound() throws Exception {
    // Arrange
    String nonExistentAccountKey = "non-existent-account";
    AccountCache mockAccountCache = mock(AccountCache.class);
    when(mockAccountCache.getAccount(nonExistentAccountKey)).thenReturn(Optional.empty());

    StorageService mockStorageService = mock(StorageService.class);
    when(mockStorageService.getAccountCache()).thenReturn(mockAccountCache);

    // Use reflection to set the private storageService field
    Field storageServiceField = KafkaProducerService.class.getDeclaredField("storageService");
    storageServiceField.setAccessible(true);
    storageServiceField.set(kafkaProducerService, mockStorageService);

    Method generateBalanceUpdateMessageJsonMethod = KafkaProducerService.class.getDeclaredMethod(
        "generateBalanceUpdateMessageJson", String.class);
    generateBalanceUpdateMessageJsonMethod.setAccessible(true);

    // Act & Assert
    InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> {
      generateBalanceUpdateMessageJsonMethod.invoke(kafkaProducerService, nonExistentAccountKey);
    });

    // Verify the cause is an IllegalStateException with the correct message
    assertTrue(exception.getCause() instanceof IllegalStateException,
        "Underlying exception should be IllegalStateException");
    assertTrue(exception.getCause().getMessage().contains("Account not found"),
        "Exception message should contain 'Account not found'");
  }

  /**
   * Test for constructor with private access
   */
  @Test
  @DisplayName("Constructor should initialize all required fields")
  void constructor_ShouldInitializeAllRequiredFields() throws Exception {
    // Arrange
    KafkaProducer<String, String> mockKafkaProducer = mock(KafkaProducer.class);

    // Use reflection to create instance with private constructor
    Constructor<KafkaProducerService> constructor = KafkaProducerService.class
        .getDeclaredConstructor(KafkaProducer.class);
    constructor.setAccessible(true);

    // Act
    KafkaProducerService service = constructor.newInstance(mockKafkaProducer);

    // Assert
    Field producerField = KafkaProducerService.class.getDeclaredField("producer");
    producerField.setAccessible(true);
    assertSame(mockKafkaProducer, producerField.get(service), "Producer should be initialized with passed instance");

    Field storageServiceField = KafkaProducerService.class.getDeclaredField("storageService");
    storageServiceField.setAccessible(true);
    assertNotNull(storageServiceField.get(service), "StorageService should be initialized");

    Field objectMapperField = KafkaProducerService.class.getDeclaredField("objectMapper");
    objectMapperField.setAccessible(true);
    assertNotNull(objectMapperField.get(service), "ObjectMapper should be initialized");

    Field envManagerField = KafkaProducerService.class.getDeclaredField("envManager");
    envManagerField.setAccessible(true);
    assertNotNull(envManagerField.get(service), "EnvManager should be initialized");
  }

  /**
   * Test for sendEventToKafka handling InterruptedException in future.get()
   */
  @Test
  @DisplayName("sendEventToKafka should handle InterruptedException in future.get()")
  void sendEventToKafka_ShouldHandleInterruptedException_InFutureGet() throws Exception {
    // Arrange
    Map<String, Object> message = new HashMap<>();
    message.put("key", "value");
    message.put("messageId", "test-message-id");

    // Create a CompletableFuture that will throw InterruptedException
    CompletableFuture<RecordMetadata> interruptedFuture = new CompletableFuture<>();
    interruptedFuture.completeExceptionally(new InterruptedException("Thread interrupted during Kafka operation"));

    // Configure the producer to return the interrupted future
    when(mockProducer.send(any(ProducerRecord.class), any())).thenReturn(interruptedFuture);

    // Act - use the public API to test the private sendEventToKafka method
    kafkaProducerService.sendTransactionResultNotProcessed(message);

    // Assert - method should complete without throwing exception
    // The exception should be logged but we can't verify logging directly
    verify(mockProducer).send(any(ProducerRecord.class), any());
  }

  /**
   * Test for multiple consecutive callback failures in sendEventToKafka
   */
  @Test
  @DisplayName("sendEventToKafka should attempt exactly two retries when Kafka callbacks consistently fail")
  void sendEventToKafka_ShouldAttemptExactlyTwoRetries_WhenKafkaCallbacksConsistentlyFail() {
    // Arrange
    Map<String, Object> message = new HashMap<>();
    message.put("key", "value");
    message.put("messageId", "test-message-id");

    // Count all callback invocations
    AtomicInteger callbackCount = new AtomicInteger(0);

    // Setup mock to execute callback with exception for all attempts
    CompletableFuture<RecordMetadata> future = CompletableFuture.completedFuture(mockRecordMetadata);
    when(mockProducer.send(any(ProducerRecord.class), any())).thenAnswer(invocation -> {
      Callback callback = invocation.getArgument(1);
      // Every callback execution will fail with an exception
      callback.onCompletion(null, new RuntimeException("Kafka send error #" + callbackCount.incrementAndGet()));
      return future;
    });

    // Act
    kafkaProducerService.sendTransactionResultNotProcessed(message);

    // Assert - initial attempt + 2 retries = 3 total calls
    verify(mockProducer, times(3)).send(any(ProducerRecord.class), any());
    assertEquals(3, callbackCount.get(), "Callback should be executed exactly 3 times (initial + 2 retries)");
  }

  /**
   * Test for handling of circular references that would cause serialization
   * errors
   */
  @Test
  @DisplayName("sendEventToKafka should handle circular references in message objects")
  void sendEventToKafka_ShouldHandleCircularReferences_InMessageObjects() throws Exception {
    // Arrange - Create a message with a circular reference that would cause Jackson
    // to fail
    Map<String, Object> circularMessage = new HashMap<>();
    circularMessage.put("messageId", "circular-reference-test");
    circularMessage.put("self", circularMessage); // Create circular reference

    // Create a custom ObjectMapper mock that will throw an exception during
    // serialization
    com.fasterxml.jackson.databind.ObjectMapper mockObjectMapper = mock(
        com.fasterxml.jackson.databind.ObjectMapper.class);
    when(mockObjectMapper.writeValueAsString(any())).thenThrow(
        new com.fasterxml.jackson.core.JsonProcessingException("Circular reference detected") {
        });

    // Replace the real ObjectMapper with our mock using reflection
    Field objectMapperField = KafkaProducerService.class.getDeclaredField("objectMapper");
    objectMapperField.setAccessible(true);

    // Store original object mapper
    Object originalObjectMapper = objectMapperField.get(kafkaProducerService);

    try {
      // Replace with mocked ObjectMapper that throws exception
      objectMapperField.set(kafkaProducerService, mockObjectMapper);

      // Act - This should not throw despite the serialization error
      kafkaProducerService.sendTransactionResultNotProcessed(circularMessage);

      // Assert - producer.send should never be called when serialization fails
      verify(mockProducer, never()).send(any(ProducerRecord.class), any());

    } finally {
      // Restore original ObjectMapper
      objectMapperField.set(kafkaProducerService, originalObjectMapper);
    }
  }

  @Test
  @DisplayName("sendEventToKafka in OutputProcessor should handle multiple accounts from ProcessResult")
  void sendEventToKafka_ShouldSendUpdatesForMultipleAccounts() throws Exception {
    // Arrange - Create accounts
    Account account1 = new Account("btc:user123");
    account1.setAvailableBalance(new BigDecimal("100.0"));

    Account account2 = new Account("eth:user123");
    account2.setAvailableBalance(new BigDecimal("5.0"));

    Account account3 = new Account("usdt:user123");
    account3.setAvailableBalance(new BigDecimal("1000.0"));

    // Create ProcessResult with multiple accounts
    DisruptorEvent event = new DisruptorEvent();
    ProcessResult result = new ProcessResult(event);

    // Add all accounts to the map
    result.addAccount(account1);
    result.addAccount(account2);
    result.addAccount(account3);

    // Create OutputProcessor instance via reflection
    Constructor<OutputProcessor> constructor = OutputProcessor.class.getDeclaredConstructor(KafkaProducerService.class);
    constructor.setAccessible(true);
    OutputProcessor outputProcessor = constructor.newInstance(kafkaProducerService);

    // Get access to private sendEventToKafka method
    Method sendEventToKafkaMethod = OutputProcessor.class.getDeclaredMethod("sendEventToKafka", ProcessResult.class);
    sendEventToKafkaMethod.setAccessible(true);

    // Act - Call the private method via reflection
    sendEventToKafkaMethod.invoke(outputProcessor, result);

    // Assert - Each account should have been sent to Kafka
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(mockProducer, times(4)).send(recordCaptor.capture(), any());

    // Extract all captured records
    List<ProducerRecord<String, String>> capturedRecords = recordCaptor.getAllValues();

    // Verify that at least 3 messages were sent to the COIN_ACCOUNT_UPDATE_TOPIC
    int coinAccountUpdateCount = 0;
    for (ProducerRecord<String, String> record : capturedRecords) {
      if (KafkaTopics.COIN_ACCOUNT_UPDATE_TOPIC.equals(record.topic())) {
        coinAccountUpdateCount++;
        assertTrue(record.key().startsWith("coin-account-"), "Key should start with coin-account-");
        assertTrue(record.value().contains("\"availableBalance\":"), "Value should contain available balance");
      }
    }
    assertEquals(3, coinAccountUpdateCount, "Should send 3 messages to COIN_ACCOUNT_UPDATE_TOPIC");

    // Verify that each account's key is included in at least one message
    assertTrue(capturedRecords.stream()
        .filter(r -> KafkaTopics.COIN_ACCOUNT_UPDATE_TOPIC.equals(r.topic()))
        .anyMatch(r -> r.key().contains("btc:user123")),
        "Should send update for btc account");
    assertTrue(capturedRecords.stream()
        .filter(r -> KafkaTopics.COIN_ACCOUNT_UPDATE_TOPIC.equals(r.topic()))
        .anyMatch(r -> r.key().contains("eth:user123")),
        "Should send update for eth account");
    assertTrue(capturedRecords.stream()
        .filter(r -> KafkaTopics.COIN_ACCOUNT_UPDATE_TOPIC.equals(r.topic()))
        .anyMatch(r -> r.key().contains("usdt:user123")),
        "Should send update for usdt account");
  }

  @Test
  @DisplayName("sendAmmOrderUpdate should send correct message")
  void sendAmmOrderUpdate_ShouldSendCorrectMessage() {
    // Arrange
    String identifier = "order123";
    AmmOrder ammOrder = mock(AmmOrder.class);
    when(ammOrder.getIdentifier()).thenReturn(identifier);
    when(ammOrder.toMessageJson()).thenReturn(Map.of("identifier", identifier, "status", "completed"));

    DisruptorEvent event = mock(DisruptorEvent.class);
    when(event.toOperationObjectMessageJson()).thenReturn(new HashMap<>());

    ProcessResult result = new ProcessResult(event);
    result.setAmmOrder(ammOrder);

    // Act
    kafkaProducerService.sendAmmOrderUpdate(result);

    // Assert
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(mockProducer).send(recordCaptor.capture(), any());

    ProducerRecord<String, String> capturedRecord = recordCaptor.getValue();
    assertEquals(KafkaTopics.AMM_ORDER_UPDATE_TOPIC, capturedRecord.topic(), "Topic should match");
    assertEquals("amm-order-" + identifier, capturedRecord.key(), "Key should match order identifier");
    assertTrue(capturedRecord.value().contains("\"messageId\""), "Value should contain messageId");
  }

  @Test
  @DisplayName("sendOfferUpdate should send correct message")
  void sendOfferUpdate_ShouldSendCorrectMessage() {
    // Arrange
    String identifier = "offer123";
    Offer offer = mock(Offer.class);
    when(offer.getIdentifier()).thenReturn(identifier);
    Map<String, Object> offerMessageJson = new HashMap<>();
    offerMessageJson.put("identifier", identifier);
    offerMessageJson.put("status", "PENDING");
    when(offer.toMessageJson()).thenReturn(offerMessageJson);

    DisruptorEvent event = mock(DisruptorEvent.class);
    Map<String, Object> eventMessageJson = new HashMap<>();
    eventMessageJson.put("type", "OFFER_UPDATE");
    when(event.toDisruptorMessageJson()).thenReturn(eventMessageJson);

    ProcessResult result = mock(ProcessResult.class);
    when(result.getOffer()).thenReturn(Optional.of(offer));
    when(result.getEvent()).thenReturn(event);

    // Mock toOfferObjectMessageJson to return proper structure with object field
    Map<String, Object> fullMessageJson = new HashMap<>();
    fullMessageJson.put("type", "OFFER_UPDATE");
    fullMessageJson.put("object", offerMessageJson);
    fullMessageJson.put("inputEventId", "test-event-id-" + identifier);
    when(result.toOfferObjectMessageJson()).thenReturn(fullMessageJson);

    // Act
    kafkaProducerService.sendOfferUpdate(result);

    // Assert
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(mockProducer).send(recordCaptor.capture(), any());

    ProducerRecord<String, String> capturedRecord = recordCaptor.getValue();
    assertEquals(KafkaTopics.OFFER_UPDATE_TOPIC, capturedRecord.topic(), "Topic should match");
    assertEquals("offer-" + identifier, capturedRecord.key(), "Key should match offer identifier");
    assertTrue(capturedRecord.value().contains("\"object\""), "Value should contain object field");
    assertTrue(capturedRecord.value().contains("\"identifier\":\"" + identifier + "\""),
        "Value should contain identifier");
  }

  @Test
  @DisplayName("sendOfferUpdate should handle missing offer gracefully")
  void sendOfferUpdate_ShouldHandleMissingOffer() {
    // Arrange
    ProcessResult result = mock(ProcessResult.class);
    when(result.getOffer()).thenReturn(Optional.empty());

    // Act
    kafkaProducerService.sendOfferUpdate(result);

    // Assert - No message should be sent
    verify(mockProducer, never()).send(any(ProducerRecord.class), any());
  }

  @Test
  @DisplayName("sendOfferUpdate should handle exception gracefully")
  void sendOfferUpdate_ShouldHandleExceptionGracefully() {
    // Arrange
    String identifier = "offer123";
    Offer offer = mock(Offer.class);
    when(offer.getIdentifier()).thenReturn(identifier);

    DisruptorEvent event = mock(DisruptorEvent.class);

    ProcessResult result = mock(ProcessResult.class);
    when(result.getOffer()).thenReturn(Optional.of(offer));
    when(result.getEvent()).thenReturn(event);
    when(result.toOfferObjectMessageJson()).thenThrow(new RuntimeException("Test exception"));

    // Act - Should not throw exception
    kafkaProducerService.sendOfferUpdate(result);

    // Assert - No exception thrown and no message sent
    verify(mockProducer, never()).send(any(), any());
  }

  @Test
  @DisplayName("sendTradeUpdate should send correct message")
  void sendTradeUpdate_ShouldSendCorrectMessage() {
    // Arrange
    String identifier = "trade123";
    Trade trade = mock(Trade.class);
    when(trade.getIdentifier()).thenReturn(identifier);
    Map<String, Object> tradeMessageJson = new HashMap<>();
    tradeMessageJson.put("identifier", identifier);
    tradeMessageJson.put("status", "COMPLETED");
    when(trade.toMessageJson()).thenReturn(tradeMessageJson);

    DisruptorEvent event = mock(DisruptorEvent.class);
    Map<String, Object> eventMessageJson = new HashMap<>();
    eventMessageJson.put("type", "TRADE_UPDATE");
    when(event.toDisruptorMessageJson()).thenReturn(eventMessageJson);

    ProcessResult result = mock(ProcessResult.class);
    when(result.getTrade()).thenReturn(Optional.of(trade));
    when(result.getEvent()).thenReturn(event);

    // Mock toTradeObjectMessageJson to return proper structure with object field
    Map<String, Object> fullMessageJson = new HashMap<>();
    fullMessageJson.put("type", "TRADE_UPDATE");
    fullMessageJson.put("object", tradeMessageJson);
    fullMessageJson.put("inputEventId", "test-event-id-" + identifier);
    when(result.toTradeObjectMessageJson()).thenReturn(fullMessageJson);

    // Act
    kafkaProducerService.sendTradeUpdate(result);

    // Assert
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(mockProducer).send(recordCaptor.capture(), any());

    ProducerRecord<String, String> capturedRecord = recordCaptor.getValue();
    assertEquals(KafkaTopics.TRADE_UPDATE_TOPIC, capturedRecord.topic(), "Topic should match");
    assertEquals("trade-" + identifier, capturedRecord.key(), "Key should match trade identifier");
    assertTrue(capturedRecord.value().contains("\"object\""), "Value should contain object field");
    assertTrue(capturedRecord.value().contains("\"identifier\":\"" + identifier + "\""),
        "Value should contain identifier");
  }

  @Test
  @DisplayName("sendTradeUpdate should handle missing trade gracefully")
  void sendTradeUpdate_ShouldHandleMissingTrade() {
    // Arrange
    ProcessResult result = mock(ProcessResult.class);
    when(result.getTrade()).thenReturn(Optional.empty());

    // Act
    kafkaProducerService.sendTradeUpdate(result);

    // Assert - No message should be sent
    verify(mockProducer, never()).send(any(ProducerRecord.class), any());
  }

  @Test
  @DisplayName("sendTradeUpdate should handle exception gracefully")
  void sendTradeUpdate_ShouldHandleExceptionGracefully() {
    // Arrange
    String identifier = "trade123";
    Trade trade = mock(Trade.class);
    when(trade.getIdentifier()).thenReturn(identifier);

    DisruptorEvent event = mock(DisruptorEvent.class);

    ProcessResult result = mock(ProcessResult.class);
    when(result.getTrade()).thenReturn(Optional.of(trade));
    when(result.getEvent()).thenReturn(event);
    when(result.toTradeObjectMessageJson()).thenThrow(new RuntimeException("Test exception"));

    // Act - Should not throw exception
    kafkaProducerService.sendTradeUpdate(result);

    // Assert - No exception thrown and no message sent
    verify(mockProducer, never()).send(any(), any());
  }

  @Test
  @DisplayName("sendBalanceLockUpdate should send correct message")
  void sendBalanceLockUpdate_ShouldSendCorrectMessage() {
    // Arrange
    BalanceLock balanceLock = BalanceLockFactory.create();
    balanceLock.setStatus("LOCKED");

    DisruptorEvent event = new DisruptorEvent();
    AccountEvent accountEvent = new AccountEvent();
    accountEvent.setEventId("test-event-id");
    event.setAccountEvent(accountEvent);

    ProcessResult result = ProcessResult.success(event);
    result.setBalanceLock(balanceLock);

    Map<String, Object> messageJson = new HashMap<>();
    messageJson.put("object", balanceLock.toMessageJson());
    messageJson.put("inputEventId", "test-event-id-" + balanceLock.getIdentifier());

    ProcessResult spyResult = spy(result);
    doReturn(messageJson).when(spyResult).toBalanceLockObjectMessageJson();

    // Act
    kafkaProducerService.sendBalanceLockUpdate(spyResult);

    // Assert
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(mockProducer).send(recordCaptor.capture(), any());

    ProducerRecord<String, String> capturedRecord = recordCaptor.getValue();
    assertEquals(KafkaTopics.BALANCES_LOCK_UPDATE_TOPIC, capturedRecord.topic());
    assertEquals("balance-lock-" + balanceLock.getLockId(), capturedRecord.key());
    assertTrue(capturedRecord.value().contains("\"object\":"));
  }

  @Test
  @DisplayName("sendBalanceLockUpdate should handle missing balance lock gracefully")
  void sendBalanceLockUpdate_ShouldHandleMissingBalanceLock() {
    // Arrange
    ProcessResult result = mock(ProcessResult.class);
    when(result.getBalanceLock()).thenReturn(Optional.empty());

    // Act
    kafkaProducerService.sendBalanceLockUpdate(result);

    // Assert - No message should be sent
    verify(mockProducer, never()).send(any(ProducerRecord.class), any());
  }

  @Test
  @DisplayName("sendBalanceLockUpdate should handle exception gracefully")
  void sendBalanceLockUpdate_ShouldHandleExceptionGracefully() {
    // Arrange
    String lockId = "lock123";
    BalanceLock balanceLock = mock(BalanceLock.class);
    when(balanceLock.getLockId()).thenReturn(lockId);

    DisruptorEvent event = mock(DisruptorEvent.class);

    ProcessResult result = mock(ProcessResult.class);
    when(result.getBalanceLock()).thenReturn(Optional.of(balanceLock));
    when(result.getEvent()).thenReturn(event);
    when(result.toBalanceLockObjectMessageJson()).thenThrow(new RuntimeException("Test exception"));

    // Act - Should not throw exception
    kafkaProducerService.sendBalanceLockUpdate(result);

    // Assert - No exception thrown and no message sent
    verify(mockProducer, never()).send(any(), any());
  }

  @Test
  @DisplayName("sendCoinWithdrawalUpdate should send correct message")
  void sendCoinWithdrawalUpdate_ShouldSendCorrectMessage() {
    // Arrange
    CoinWithdrawal withdrawal = CoinWithdrawalFactory.create("btc:user123", "withdrawal-123", new BigDecimal("1.0"));
    withdrawal.setStatus("completed");

    DisruptorEvent event = new DisruptorEvent();
    CoinWithdrawalEvent withdrawalEvent = new CoinWithdrawalEvent();
    withdrawalEvent.setEventId("test-event-id");
    event.setCoinWithdrawalEvent(withdrawalEvent);

    ProcessResult result = ProcessResult.success(event);
    result.setWithdrawal(withdrawal);

    Map<String, Object> messageJson = new HashMap<>();
    messageJson.put("object", withdrawal.toMessageJson());
    messageJson.put("inputEventId", "test-event-id-" + withdrawal.getIdentifier());

    ProcessResult spyResult = spy(result);
    doReturn(messageJson).when(spyResult).toCoinWithdrawalObjectMessageJson();

    // Act
    kafkaProducerService.sendCoinWithdrawalUpdate(spyResult);

    // Assert
    ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
    verify(mockProducer).send(recordCaptor.capture(), any());

    ProducerRecord<String, String> capturedRecord = recordCaptor.getValue();
    assertEquals(KafkaTopics.COIN_WITHDRAWAL_UPDATE_TOPIC, capturedRecord.topic());
    assertEquals("coin-withdrawal-" + withdrawal.getIdentifier(), capturedRecord.key());
    assertTrue(capturedRecord.value().contains("\"object\":"));
  }

  @Test
  @DisplayName("sendCoinWithdrawalUpdate should handle missing withdrawal gracefully")
  void sendCoinWithdrawalUpdate_ShouldHandleMissingWithdrawal() {
    // Arrange
    ProcessResult result = mock(ProcessResult.class);
    when(result.getWithdrawal()).thenReturn(Optional.empty());

    // Act
    kafkaProducerService.sendCoinWithdrawalUpdate(result);

    // Assert - No message should be sent
    verify(mockProducer, never()).send(any(ProducerRecord.class), any());
  }

  @Test
  @DisplayName("sendCoinWithdrawalUpdate should handle exception gracefully")
  void sendCoinWithdrawalUpdate_ShouldHandleExceptionGracefully() {
    // Arrange
    String identifier = "withdrawal123";
    CoinWithdrawal withdrawal = mock(CoinWithdrawal.class);
    when(withdrawal.getIdentifier()).thenReturn(identifier);

    DisruptorEvent event = mock(DisruptorEvent.class);

    ProcessResult result = mock(ProcessResult.class);
    when(result.getWithdrawal()).thenReturn(Optional.of(withdrawal));
    when(result.getEvent()).thenReturn(event);
    when(result.toCoinWithdrawalObjectMessageJson()).thenThrow(new RuntimeException("Test exception"));

    // Act - Should not throw exception
    kafkaProducerService.sendCoinWithdrawalUpdate(result);

    // Assert - No exception thrown and no message sent
    verify(mockProducer, never()).send(any(), any());
  }
}
