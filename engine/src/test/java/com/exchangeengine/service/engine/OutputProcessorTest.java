package com.exchangeengine.service.engine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.exchangeengine.factory.AccountFactory;
import com.exchangeengine.factory.AmmPoolFactory;
import com.exchangeengine.factory.AmmPositionFactory;
import com.exchangeengine.factory.AccountHistoryFactory;
import com.exchangeengine.factory.CoinDepositFactory;
import com.exchangeengine.factory.CoinWithdrawalFactory;
import com.exchangeengine.factory.event.DisruptorEventFactory;
import com.exchangeengine.messaging.producer.KafkaProducerService;
import com.exchangeengine.model.Account;
import com.exchangeengine.model.AccountHistory;
import com.exchangeengine.model.AmmOrder;
import com.exchangeengine.model.AmmPool;
import com.exchangeengine.model.AmmPosition;
import com.exchangeengine.model.CoinDeposit;
import com.exchangeengine.model.CoinWithdrawal;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.event.AccountEvent;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.storage.StorageService;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.AccountHistoryCache;
import com.exchangeengine.storage.cache.AmmOrderCache;
import com.exchangeengine.storage.cache.AmmPoolCache;
import com.exchangeengine.storage.cache.AmmPositionCache;
import com.exchangeengine.storage.cache.DepositCache;
import com.exchangeengine.storage.cache.WithdrawalCache;
import com.exchangeengine.extension.SingletonResetExtension;
import com.exchangeengine.extension.MockitoStaticCleanupExtension;
import com.exchangeengine.factory.MerchantEscrowFactory;
import com.exchangeengine.model.MerchantEscrow;
import com.exchangeengine.storage.cache.MerchantEscrowCache;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.rocksdb.RocksDBService;
import com.exchangeengine.extension.CombinedTestExtension;
import com.exchangeengine.factory.OfferFactory;
import com.exchangeengine.factory.TickFactory;
import com.exchangeengine.factory.TradeFactory;
import com.exchangeengine.model.Offer;
import com.exchangeengine.model.Tick;
import com.exchangeengine.model.Trade;
import com.exchangeengine.storage.cache.OfferCache;
import com.exchangeengine.storage.cache.TradeCache;
import com.exchangeengine.factory.BalanceLockFactory;
import com.exchangeengine.model.BalanceLock;
import com.exchangeengine.storage.cache.BalanceLockCache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;

@ExtendWith({ MockitoExtension.class, CombinedTestExtension.class })
class OutputProcessorTest {

  @Mock
  private KafkaProducerService kafkaProducerService;

  @Mock
  private StorageService storageService;

  @Mock
  private AccountCache accountCache;

  @Mock
  private DepositCache depositCache;

  @Mock
  private WithdrawalCache withdrawalCache;

  @Mock
  private AccountHistoryCache accountHistoryCache;

  @Mock
  private AmmPoolCache ammPoolCache;

  @Mock
  private AmmPositionCache ammPositionCache;

  @Mock
  private AmmOrderCache ammOrderCache;

  @Mock
  private OfferCache offerCache;

  @Mock
  private TradeCache tradeCache;

  @Mock
  private BalanceLockCache balanceLockCache;

  @Mock
  private RocksDBService mockRocksDBService;

  @Mock
  private ExecutorService mockExecutorService;

  private OutputProcessor outputProcessor;

  // Test constants
  private static final String ACCOUNT_KEY = "btc:user123";
  private static final String EVENT_ID = "test-event-id";
  private static final String IDENTIFIER = "tx123";
  private static final BigDecimal AMOUNT = new BigDecimal("1.0");

  // Khai báo static mocks ở cấp độ class để có thể đóng trong afterEach
  private static MockedStatic<RocksDBService> mockedRocksDB;
  private static MockedStatic<StorageService> mockedStorage;
  private static MockedStatic<KafkaProducerService> mockedKafka;

  @BeforeEach
  void setUp() throws Exception {
    // Reset all singleton instances
    SingletonResetExtension.resetAll();

    // Setup Mocks
    setupMocks();

    // Reset the singleton instance for testing
    Field instanceField = OutputProcessor.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);

    // Set up mocks for cache objects with lenient() to avoid
    // UnnecessaryStubbingException
    lenient().when(storageService.getAccountCache()).thenReturn(accountCache);
    lenient().when(storageService.getDepositCache()).thenReturn(depositCache);
    lenient().when(storageService.getWithdrawalCache()).thenReturn(withdrawalCache);
    lenient().when(storageService.getAccountHistoryCache()).thenReturn(accountHistoryCache);
    lenient().when(storageService.getAmmPoolCache()).thenReturn(ammPoolCache);
    lenient().when(storageService.getAmmPositionCache()).thenReturn(ammPositionCache);
    lenient().when(storageService.getBalanceLockCache()).thenReturn(balanceLockCache);

    // Use reflection to create instance with private constructor
    Constructor<OutputProcessor> constructor = OutputProcessor.class.getDeclaredConstructor(KafkaProducerService.class);
    constructor.setAccessible(true);
    outputProcessor = constructor.newInstance(kafkaProducerService);

    // Set singleton instance
    instanceField.set(null, outputProcessor);

    // Set storageService via reflection
    Field storageServiceField = OutputProcessor.class.getDeclaredField("storageService");
    storageServiceField.setAccessible(true);
    storageServiceField.set(outputProcessor, storageService);

    // Replace the TestUtils code with direct reflection
    try {
      Field kafkaExecutorField = OutputProcessor.class.getDeclaredField("kafkaExecutor");
      kafkaExecutorField.setAccessible(true);
      kafkaExecutorField.set(outputProcessor, mockExecutorService);

      Field storageExecutorField = OutputProcessor.class.getDeclaredField("storageExecutor");
      storageExecutorField.setAccessible(true);
      storageExecutorField.set(outputProcessor, mockExecutorService);
    } catch (Exception e) {
      throw new RuntimeException("Failed to inject mock executors", e);
    }

    // Set up executor service to run tasks immediately
    lenient().when(mockExecutorService.submit(any(Runnable.class))).thenAnswer(invocation -> {
      Runnable task = invocation.getArgument(0);
      task.run();
      return null;
    });
  }

  @AfterEach
  void tearDown() {
    // Đảm bảo đóng tất cả các static mocks sau mỗi test
    if (mockedRocksDB != null) {
      mockedRocksDB.close();
      mockedRocksDB = null;
    }

    if (mockedStorage != null) {
      mockedStorage.close();
      mockedStorage = null;
    }

    if (mockedKafka != null) {
      mockedKafka.close();
      mockedKafka = null;
    }

    // Ensure cleanup của static mocks
    MockitoStaticCleanupExtension.forceCleanupStaticMocks();
  }

  /**
   * Setup all mocks including static mocks
   */
  private void setupMocks() {
    try {
      // First, ensure any existing static mocks are cleaned up
      MockitoStaticCleanupExtension.forceCleanupStaticMocks();

      // Close any existing mocks to avoid duplication
      if (mockedRocksDB != null) {
        mockedRocksDB.close();
        mockedRocksDB = null;
      }

      if (mockedStorage != null) {
        mockedStorage.close();
        mockedStorage = null;
      }

      if (mockedKafka != null) {
        mockedKafka.close();
        mockedKafka = null;
      }

      // Then mock the RocksDBService
      mockedRocksDB = mockStatic(RocksDBService.class);
      mockedRocksDB.when(RocksDBService::getInstance).thenReturn(mockRocksDBService);

      // Then mock the StorageService to return our mocked storageService
      mockedStorage = mockStatic(StorageService.class);
      mockedStorage.when(StorageService::getInstance).thenReturn(storageService);

      // Mock KafkaProducerService
      mockedKafka = mockStatic(KafkaProducerService.class);
      mockedKafka.when(KafkaProducerService::getInstance).thenReturn(kafkaProducerService);

      // Setup other necessary mocks for caches
      MerchantEscrowCache merchantEscrowCache = mock(MerchantEscrowCache.class);
      lenient().when(storageService.getMerchantEscrowCache()).thenReturn(merchantEscrowCache);

      // Thêm mock cho OfferCache và TradeCache
      lenient().when(storageService.getOfferCache()).thenReturn(offerCache);
      lenient().when(storageService.getTradeCache()).thenReturn(tradeCache);
      lenient().when(storageService.getAmmOrderCache()).thenReturn(ammOrderCache);

    } catch (Exception e) {
      // Log the error but continue with the test
      System.err.println("Error setting up mocks: " + e.getMessage());
      e.printStackTrace(); // Thêm stack trace để debug
    }
  }

  /**
   * Helper method to create a test event and set its eventId via a BaseEvent
   *
   * @param eventId the eventId to set
   * @return DisruptorEvent with the eventId set
   */
  private DisruptorEvent createTestEventWithId(String eventId) {
    DisruptorEvent event = DisruptorEventFactory.create();
    // Cần tạo và thiết lập một BaseEvent để có thể thiết lập eventId
    AccountEvent accountEvent = new AccountEvent();
    accountEvent.setEventId(eventId);
    accountEvent.setAccountKey(ACCOUNT_KEY);
    event.setAccountEvent(accountEvent);
    return event;
  }

  @Test
  @DisplayName("Test cho việc gửi event thành công")
  void processOutput_ShouldSendEventToKafka_WhenEventIsSuccessful() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);
    Account account = AccountFactory.create(ACCOUNT_KEY);
    AmmPool ammPool = AmmPoolFactory.createDefaultAmmPool();
    ProcessResult result = ProcessResult.success(event);
    result.setAccount(account);
    result.setAmmPool(ammPool);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then - Không kiểm tra sendTransactionResult vì đã thay đổi logic
    // verify(kafkaProducerService).sendTransactionResult(event);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, account);
    verify(kafkaProducerService).sendAmmPoolUpdate(result);
    verify(accountCache).addAccountToBatch(account);
    verify(ammPoolCache).addAmmPoolToBatch(ammPool);
  }

  @Test
  @DisplayName("Test cho việc gửi event thành công khi không có AmmPool")
  void processOutput_ShouldSendTransactionResult_WhenNoAmmPool() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);
    Account account = AccountFactory.create(ACCOUNT_KEY);
    ProcessResult result = ProcessResult.success(event);
    result.setAccount(account);
    // Không có AmmPool

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then - Kiểm tra sendTransactionResult được gọi khi không có AmmPool
    verify(kafkaProducerService).sendTransactionResult(event);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, account);
    verify(accountCache).addAccountToBatch(account);
  }

  @Test
  @DisplayName("Test cho việc flush to disk khi endOfBatch là true")
  void processOutput_ShouldFlushToDisk_WhenEndOfBatchIsTrue() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);
    ProcessResult result = ProcessResult.success(event);

    // When
    outputProcessor.processOutput(result, true);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(storageService).flushToDisk();
  }

  @Test
  @DisplayName("Test cho việc shutdown thành công")
  void shutdown_ShouldFlushToDisk() throws Exception {
    // When
    outputProcessor.shutdown();

    // Then
    verify(storageService).flushToDisk();
  }

  @Test
  @DisplayName("Test cho việc flush to disk khi StorageService says it should flush")
  void processOutput_ShouldFlushToDisk_WhenStorageServiceSaysItShouldFlush() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);
    ProcessResult result = ProcessResult.success(event);

    // StorageService says it should flush
    when(storageService.shouldFlush()).thenReturn(true);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(storageService).shouldFlush();
    verify(storageService).flushToDisk();
  }

  @Test
  @DisplayName("Test cho việc process deposit khi result chứa deposit")
  void processOutput_ShouldProcessDeposit_WhenResultContainsDeposit() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Sử dụng CoinDepositFactory để tạo CoinDeposit
    CoinDeposit deposit = CoinDepositFactory.create(ACCOUNT_KEY, IDENTIFIER, AMOUNT);

    ProcessResult result = ProcessResult.success(event);
    result.setDeposit(deposit);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(depositCache).addDepositToBatch(deposit);
  }

  @Test
  @DisplayName("Test cho việc process withdrawal khi result chứa withdrawal")
  void processOutput_ShouldProcessWithdrawal_WhenResultContainsWithdrawal() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Sử dụng CoinWithdrawalFactory để tạo CoinWithdrawal
    CoinWithdrawal withdrawal = CoinWithdrawalFactory.create(ACCOUNT_KEY, IDENTIFIER, AMOUNT);

    ProcessResult result = ProcessResult.success(event);
    result.setWithdrawal(withdrawal);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(withdrawalCache).addWithdrawalToBatch(withdrawal);
  }

  @Test
  @DisplayName("Test cho việc process account history khi result chứa history")
  void processOutput_ShouldProcessAccountHistory_WhenResultContainsHistory() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Sử dụng AccountHistoryFactory để tạo AccountHistory
    AccountHistory history = AccountHistoryFactory.createForDeposit(ACCOUNT_KEY, IDENTIFIER);

    ProcessResult result = ProcessResult.success(event);
    result.setAccountHistory(history);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(accountHistoryCache).addHistoryToBatch(history);
  }

  @Test
  @DisplayName("Test cho việc handle Kafka exception và tiếp tục xử lý")
  void processOutput_ShouldHandleKafkaException_AndContinueProcessing() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    Account account = AccountFactory.create(ACCOUNT_KEY);
    ProcessResult result = ProcessResult.success(event);
    result.setAccount(account);

    // Setup Kafka to throw an exception
    doThrow(new RuntimeException("Kafka connection error")).when(kafkaProducerService).sendTransactionResult(any());

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then - Storage processing should still happen despite Kafka error
    verify(kafkaProducerService).sendTransactionResult(event);
    verify(accountCache).addAccountToBatch(account);
  }

  @Test
  @DisplayName("Test cho việc handle Storage exception và tiếp tục xử lý")
  void processOutput_ShouldHandleStorageException_AndContinueProcessing() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    Account account = AccountFactory.create(ACCOUNT_KEY);
    ProcessResult result = ProcessResult.success(event);
    result.setAccount(account);

    // Setup Storage to throw an exception
    doThrow(new RuntimeException("Storage error")).when(accountCache).addAccountToBatch(any());

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then - Kafka processing should still happen despite Storage error
    verify(kafkaProducerService).sendTransactionResult(event);
    verify(accountCache).addAccountToBatch(account);
  }

  @Test
  @DisplayName("Test cho việc processStorageAsynchronously nên bắt lỗi và log lỗi")
  void processStorageAsynchronously_ShouldCatchAndLogException_DirectMethod() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    Account account = AccountFactory.create(ACCOUNT_KEY);
    ProcessResult result = ProcessResult.success(event);
    result.setAccount(account);

    // Setup exceptions using lenient to avoid UnnecessaryStubbingException
    lenient().doThrow(new RuntimeException("Critical storage error")).when(accountCache).addAccountToBatch(any());
    lenient().doThrow(new RuntimeException("Critical storage error")).when(storageService).shouldFlush();
    lenient().doThrow(new RuntimeException("Critical storage error")).when(storageService).flushToDisk();

    // Get access to the private method using reflection
    java.lang.reflect.Method processStorageMethod = OutputProcessor.class.getDeclaredMethod(
        "processStorageAsynchronously", ProcessResult.class, boolean.class);
    processStorageMethod.setAccessible(true);

    // When - Should not throw exception
    processStorageMethod.invoke(outputProcessor, result, false);

    // Sleep briefly to allow async tasks to complete if needed
    Thread.sleep(100);

    // Then - verify method was called but exception was caught
    verify(accountCache, atLeastOnce()).addAccountToBatch(account);
  }

  @Test
  @DisplayName("Test cho việc shutdown nên xử lý lỗi khi flushToDisk")
  void shutdown_ShouldHandleExceptionDuringFlush() throws Exception {
    // Given
    doThrow(new RuntimeException("Critical flush error")).when(storageService).flushToDisk();

    // When - Should not throw exception
    outputProcessor.shutdown();

    // Then
    verify(storageService).flushToDisk();
  }

  @Test
  @DisplayName("Test cho việc processOutput nên gửi event thành công khi result chứa ammPool")
  void processOutput_ShouldProcessAmmPool_WhenResultContainsAmmPool() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Sử dụng AmmPoolFactory để tạo AmmPool
    AmmPool ammPool = AmmPoolFactory.createDefaultAmmPool();

    ProcessResult result = ProcessResult.success(event);
    result.setAmmPool(ammPool);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(ammPoolCache).addAmmPoolToBatch(ammPool);
  }

  @Test
  @DisplayName("Test cho việc processOutput nên xử lý null fields")
  void processOutput_ShouldHandleNullFieldsGracefully() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Create result with null values
    ProcessResult result = ProcessResult.success(event);
    // No fields set in result - all fields are null

    // When - Should not throw exception
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then - Verify it still processed basic data
    verify(kafkaProducerService).sendTransactionResult(event);
  }

  @Test
  @DisplayName("Test cho việc processOutput nên xử lý khi có nhiều request đồng thời")
  void processOutput_ShouldHandleConcurrentRequests() throws Exception {
    // Create a list of threads and results
    final int threadCount = 5;
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch endLatch = new CountDownLatch(threadCount);

    List<Thread> threads = new ArrayList<>();

    // Create multiple threads to call processOutput concurrently
    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      Thread thread = new Thread(() -> {
        try {
          startLatch.await(); // Wait for the signal to start

          // Create different result objects for each thread
          DisruptorEvent event = createTestEventWithId("event-" + index);
          Account account = AccountFactory.create("account-" + index);
          ProcessResult result = ProcessResult.success(event);
          result.setAccount(account);

          // Process output
          outputProcessor.processOutput(result, index == threadCount - 1);
        } catch (Exception e) {
          fail("Exception should not be thrown: " + e.getMessage());
        } finally {
          endLatch.countDown();
        }
      });

      threads.add(thread);
      thread.start();
    }

    // Signal all threads to start processing
    startLatch.countDown();

    // Wait for all threads to finish
    endLatch.await(5, TimeUnit.SECONDS);

    // Sleep to allow async tasks to complete
    Thread.sleep(200);

    // Verify
    verify(kafkaProducerService, times(threadCount)).sendTransactionResult(any());
    verify(kafkaProducerService, times(threadCount)).sendCoinAccountUpdate(any(String.class), any(Account.class));
    verify(accountCache, times(threadCount)).addAccountToBatch(any());

    // The last thread should have triggered a flush
    verify(storageService, atLeastOnce()).flushToDisk();
  }

  @Test
  @DisplayName("Test cho việc processOutput nên xử lý khi tất cả các trường được đặt")
  void processOutput_ShouldHandleResultWithAllFieldsSet() throws Exception {
    // Create a result with all fields set
    DisruptorEvent event = createTestEventWithId(EVENT_ID);
    Account account = AccountFactory.create(ACCOUNT_KEY);
    CoinDeposit deposit = CoinDepositFactory.create(ACCOUNT_KEY, IDENTIFIER, AMOUNT);
    CoinWithdrawal withdrawal = CoinWithdrawalFactory.create(ACCOUNT_KEY, IDENTIFIER, AMOUNT);
    AccountHistory history = AccountHistoryFactory.createForDeposit(ACCOUNT_KEY, IDENTIFIER);
    AmmPool ammPool = AmmPoolFactory.createDefaultAmmPool();

    ProcessResult result = ProcessResult.success(event);
    result.setAccount(account)
        .setDeposit(deposit)
        .setWithdrawal(withdrawal)
        .setAccountHistory(history)
        .setAmmPool(ammPool);

    // Process output
    outputProcessor.processOutput(result, true);

    // Sleep to allow async tasks to complete
    Thread.sleep(200);

    // Verify that all fields were processed
    // Không kiểm tra sendTransactionResult vì đã thay đổi logic
    // verify(kafkaProducerService).sendTransactionResult(event);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, account);
    verify(accountCache).addAccountToBatch(account);
    verify(depositCache).addDepositToBatch(deposit);
    verify(withdrawalCache).addWithdrawalToBatch(withdrawal);
    verify(accountHistoryCache).addHistoryToBatch(history);
    verify(ammPoolCache).addAmmPoolToBatch(ammPool);
    verify(storageService).flushToDisk();
  }

  @Test
  @DisplayName("Test cho việc resetInstance nên reset instance")
  void resetInstance_ShouldResetInstance() throws Exception {
    // Kiểm tra trước khi reset - lấy instance (sẽ tạo mới nếu null)
    OutputProcessor instanceBeforeReset = OutputProcessor.getInstance();
    assertNotNull(instanceBeforeReset, "Instance phải tồn tại trước khi reset");

    // Thực hiện reset instance
    OutputProcessor.resetInstance();

    // Sau khi reset - getInstance() sẽ tạo mới instance
    OutputProcessor newInstance = OutputProcessor.getInstance();

    // Kiểm tra instance mới khác với instance cũ
    assertNotEquals(instanceBeforeReset, newInstance, "Instance mới phải khác với instance cũ");

    // Reset lại để không ảnh hưởng đến các test khác
    OutputProcessor.resetInstance();
  }

  @Test
  @DisplayName("Test for processing fiat account history")
  void processOutput_ShouldProcessFiatAccountHistory_WhenResultContainsFiatAccountHistory() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Create FiatAccountHistory
    AccountHistory fiatAccountHistory = AccountHistoryFactory.create(ACCOUNT_KEY, "fiat-history-123",
        OperationType.COIN_DEPOSIT_CREATE.getValue());

    ProcessResult result = ProcessResult.success(event);
    result.setFiatAccountHistory(fiatAccountHistory);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(accountHistoryCache).addHistoryToBatch(fiatAccountHistory);
  }

  @Test
  @DisplayName("Test for processing coin account history")
  void processOutput_ShouldProcessCoinAccountHistory_WhenResultContainsCoinAccountHistory() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Create CoinAccountHistory
    AccountHistory coinAccountHistory = AccountHistoryFactory.create(ACCOUNT_KEY, "coin-history-123",
        OperationType.COIN_WITHDRAWAL_CREATE.getValue());

    ProcessResult result = ProcessResult.success(event);
    result.setCoinAccountHistory(coinAccountHistory);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(accountHistoryCache).addHistoryToBatch(coinAccountHistory);
  }

  @Test
  @DisplayName("Test for processing fiat account")
  void processOutput_ShouldProcessFiatAccount_WhenResultContainsFiatAccount() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Create FiatAccount
    Account fiatAccount = AccountFactory.create(ACCOUNT_KEY);

    ProcessResult result = ProcessResult.success(event);
    result.setFiatAccount(fiatAccount);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(accountCache).addAccountToBatch(fiatAccount);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, fiatAccount);
  }

  @Test
  @DisplayName("Test for processing both account and fiat account")
  void processOutput_ShouldProcessBothAccountAndFiatAccount_WhenResultContainsBoth() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Create Accounts
    Account account = AccountFactory.create(ACCOUNT_KEY);
    Account fiatAccount = AccountFactory.create("fiat:" + ACCOUNT_KEY);

    ProcessResult result = ProcessResult.success(event);
    result.setAccount(account);
    result.setFiatAccount(fiatAccount);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(accountCache).addAccountToBatch(account);
    verify(accountCache).addAccountToBatch(fiatAccount);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, account);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, fiatAccount);
  }

  @Test
  @DisplayName("Test for processing merchantEscrow")
  void processOutput_ShouldProcessMerchantEscrow_WhenResultContainsMerchantEscrow() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Set up MerchantEscrowCache mock
    MerchantEscrowCache merchantEscrowCache = mock(MerchantEscrowCache.class);
    when(storageService.getMerchantEscrowCache()).thenReturn(merchantEscrowCache);

    // Create MerchantEscrow
    MerchantEscrow merchantEscrow = MerchantEscrowFactory.createDefault();

    ProcessResult result = ProcessResult.success(event);
    result.setMerchantEscrow(merchantEscrow);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(merchantEscrowCache).addMerchantEscrowToBatch(merchantEscrow);
    verify(kafkaProducerService).sendMerchantEscrowUpdate(result);
  }

  @Test
  @DisplayName("Test for exception in processResultData")
  void processResultData_ShouldHandleExceptionGracefully() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);
    Account account = AccountFactory.create(ACCOUNT_KEY);
    ProcessResult result = ProcessResult.success(event);
    result.setAccount(account);

    // Simulate exception when adding account to batch
    doThrow(new RuntimeException("Test exception")).when(accountCache).addAccountToBatch(account);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then - should not throw exception up, it should be caught and logged
    // No assertions needed as we're testing that the exception doesn't propagate
  }

  @Test
  @DisplayName("Test for getInstance creates new instance when null")
  void getInstance_ShouldCreateNewInstance_WhenInstanceIsNull() throws Exception {
    // Given
    // Reset the singleton instance first
    OutputProcessor.resetInstance();

    // When - sử dụng mock đã được thiết lập trong setup
    // Act
    OutputProcessor instance = OutputProcessor.getInstance();

    // Then
    assertNotNull(instance);
    // Verify rằng KafkaProducerService.getInstance() đã được gọi
    mockedKafka.verify(KafkaProducerService::getInstance);
  }

  @Test
  @DisplayName("Test for getInstance returns existing instance")
  void getInstance_ShouldReturnExistingInstance_WhenInstanceIsNotNull() throws Exception {
    // Given
    // Đặt instance thông qua reflection
    Field instanceField = OutputProcessor.class.getDeclaredField("instance");
    instanceField.setAccessible(true);

    OutputProcessor mockProcessor = mock(OutputProcessor.class);
    instanceField.set(null, mockProcessor);

    // When
    OutputProcessor instance = OutputProcessor.getInstance();

    // Then
    assertSame(mockProcessor, instance);

    // Reset lại để không ảnh hưởng đến các test khác
    instanceField.set(null, outputProcessor);
  }

  @Test
  @DisplayName("Test for complete coverage of sendEventToKafka method with coin account")
  void sendEventToKafka_ShouldSendCoinAccountUpdate_WhenResultContainsCoinAccount() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Create Account and CoinAccount
    Account coinAccount = AccountFactory.create("coin:" + ACCOUNT_KEY);

    ProcessResult result = ProcessResult.success(event);
    result.setCoinAccount(coinAccount);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, coinAccount);
    verify(accountCache).addAccountToBatch(coinAccount);
  }

  @Test
  @DisplayName("Test for complete coverage with all types of history")
  void processOutput_ShouldProcessAllTypesOfHistory_WhenResultContainsAllHistories() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Create all history types
    AccountHistory accountHistory = AccountHistoryFactory.create(ACCOUNT_KEY, "history-123",
        OperationType.COIN_DEPOSIT_CREATE.getValue());
    AccountHistory fiatAccountHistory = AccountHistoryFactory.create("fiat:" + ACCOUNT_KEY, "fiat-history-123",
        OperationType.COIN_DEPOSIT_CREATE.getValue());
    AccountHistory coinAccountHistory = AccountHistoryFactory.create("coin:" + ACCOUNT_KEY, "coin-history-123",
        OperationType.COIN_WITHDRAWAL_CREATE.getValue());

    ProcessResult result = ProcessResult.success(event);
    result.setAccountHistory(accountHistory);
    result.setFiatAccountHistory(fiatAccountHistory);
    result.setCoinAccountHistory(coinAccountHistory);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(accountHistoryCache).addHistoryToBatch(accountHistory);
    verify(accountHistoryCache).addHistoryToBatch(fiatAccountHistory);
    verify(accountHistoryCache).addHistoryToBatch(coinAccountHistory);
  }

  @Test
  @DisplayName("Test cho việc processOutput nên gửi event thành công khi result chứa ammPosition")
  void processOutput_ShouldProcessAmmPosition_WhenResultContainsAmmPosition() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Sử dụng AmmPositionFactory để tạo AmmPosition
    AmmPosition ammPosition = AmmPositionFactory.createDefaultAmmPosition();

    ProcessResult result = ProcessResult.success(event);
    result.setAmmPosition(ammPosition);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(ammPositionCache).addAmmPositionToBatch(ammPosition);
    verify(kafkaProducerService).sendAmmPositionUpdate(result);
  }

  @Test
  @DisplayName("Test cho việc processOutput nên xử lý exception khi sendAmmPositionUpdate gặp lỗi")
  void processOutput_ShouldHandleException_WhenSendAmmPositionUpdateFails() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);
    AmmPosition ammPosition = AmmPositionFactory.createDefaultAmmPosition();
    ProcessResult result = ProcessResult.success(event);
    result.setAmmPosition(ammPosition);

    // Setup Kafka để ném exception khi gọi sendAmmPositionUpdate
    doThrow(new RuntimeException("Kafka error when sending AMM position update"))
        .when(kafkaProducerService).sendAmmPositionUpdate(any(ProcessResult.class));

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    // Verify kafka method was called despite exception
    verify(kafkaProducerService).sendAmmPositionUpdate(result);

    // Verify storage processing still happened
    verify(ammPositionCache).addAmmPositionToBatch(ammPosition);
  }

  @Test
  @DisplayName("Test cho việc processOutput nên xử lý đồng thời AmmPool và AmmPosition")
  void processOutput_ShouldProcessBothAmmPoolAndAmmPosition() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);
    AmmPool ammPool = AmmPoolFactory.createDefaultAmmPool();
    AmmPosition ammPosition = AmmPositionFactory.createDefaultAmmPosition();

    ProcessResult result = ProcessResult.success(event);
    result.setAmmPool(ammPool);
    result.setAmmPosition(ammPosition);

    // When
    outputProcessor.processOutput(result, true);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(ammPoolCache).addAmmPoolToBatch(ammPool);
    verify(ammPositionCache).addAmmPositionToBatch(ammPosition);
    verify(kafkaProducerService).sendAmmPoolUpdate(result);
    verify(kafkaProducerService).sendAmmPositionUpdate(result);
    // TransactionResult should not be sent when AmmPool or AmmPosition is present
    verify(kafkaProducerService, never()).sendTransactionResult(any());
    // FlushToDisk should be called because endOfBatch is true
    verify(storageService).flushToDisk();
  }

  @Test
  @DisplayName("Test cho việc processOutput xử lý tập hợp accounts")
  void processOutput_ShouldProcessMultipleAccounts_WhenResultContainsAccountsCollection() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Tạo nhiều account với các key khác nhau
    Account account1 = AccountFactory.create("btc:user1");
    Account account2 = AccountFactory.create("eth:user2");
    Account account3 = AccountFactory.create("usdt:user3");

    ProcessResult result = ProcessResult.success(event);

    // Thêm tài khoản vào tập hợp accounts
    result.addAccount(account1);
    result.addAccount(account2);
    result.addAccount(account3);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(accountCache).addAccountToBatch(account1);
    verify(accountCache).addAccountToBatch(account2);
    verify(accountCache).addAccountToBatch(account3);

    // Verify Kafka messages were sent for each account
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, account1);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, account2);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, account3);
  }

  @Test
  @DisplayName("Test cho việc processOutput xử lý tập hợp accountHistories")
  void processOutput_ShouldProcessAccountHistoriesCollection() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Tạo nhiều accountHistory với các nội dung khác nhau
    AccountHistory history1 = AccountHistoryFactory.createForDeposit("btc:user1", "tx1");
    AccountHistory history2 = AccountHistoryFactory.createForDeposit("eth:user2", "tx2");
    AccountHistory history3 = AccountHistoryFactory.createForDeposit("usdt:user3", "tx3");

    ProcessResult result = ProcessResult.success(event);

    // Thêm các accountHistory vào tập hợp
    result.addAccountHistory(history1);
    result.addAccountHistory(history2);
    result.addAccountHistory(history3);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(accountHistoryCache).addHistoryToBatch(history1);
    verify(accountHistoryCache).addHistoryToBatch(history2);
    verify(accountHistoryCache).addHistoryToBatch(history3);
  }

  @Test
  @DisplayName("Test cho việc processOutput xử lý kết hợp cả đối tượng đơn lẻ và tập hợp")
  void processOutput_ShouldProcessBothSingleObjectAndCollections() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Tạo đối tượng đơn lẻ
    Account singleAccount = AccountFactory.create("btc:main");
    AccountHistory singleHistory = AccountHistoryFactory.createForDeposit("btc:main", "tx-main");

    // Tạo tập hợp đối tượng
    Account collectionAccount1 = AccountFactory.create("eth:user1");
    Account collectionAccount2 = AccountFactory.create("usdt:user2");

    AccountHistory collectionHistory1 = AccountHistoryFactory.createForDeposit("eth:user1", "tx1");
    AccountHistory collectionHistory2 = AccountHistoryFactory.createForDeposit("usdt:user2", "tx2");

    ProcessResult result = ProcessResult.success(event);

    // Thiết lập cả đối tượng đơn lẻ và tập hợp
    result.setAccount(singleAccount);
    result.setAccountHistory(singleHistory);

    result.addAccount(collectionAccount1);
    result.addAccount(collectionAccount2);

    result.addAccountHistory(collectionHistory1);
    result.addAccountHistory(collectionHistory2);

    // When
    outputProcessor.processOutput(result, true);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then - Kiểm tra cả đối tượng đơn lẻ và tập hợp được xử lý

    // Kiểm tra accounts
    verify(accountCache).addAccountToBatch(singleAccount);
    verify(accountCache).addAccountToBatch(collectionAccount1);
    verify(accountCache).addAccountToBatch(collectionAccount2);

    // Kiểm tra account histories
    verify(accountHistoryCache).addHistoryToBatch(singleHistory);
    verify(accountHistoryCache).addHistoryToBatch(collectionHistory1);
    verify(accountHistoryCache).addHistoryToBatch(collectionHistory2);

    // Kiểm tra Kafka
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, singleAccount);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, collectionAccount1);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, collectionAccount2);

    // Kiểm tra flush khi endOfBatch là true
    verify(storageService).flushToDisk();
  }

  @Test
  @DisplayName("Test cho việc processOutput xử lý ngoại lệ khi xử lý tập hợp accounts")
  void processOutput_ShouldHandleExceptions_WhenProcessingCollections() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Tạo tập hợp đối tượng account
    Account account1 = AccountFactory.create("btc:user1");
    Account account2 = AccountFactory.create("eth:user2");

    ProcessResult result = ProcessResult.success(event);

    // Thêm account vào tập hợp
    result.addAccount(account1);
    result.addAccount(account2);

    // Setup ngoại lệ cho account1 cụ thể để account2 vẫn được xử lý
    doThrow(new RuntimeException("Error processing account")).when(accountCache)
        .addAccountToBatch(account1);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then - Dù có ngoại lệ, xử lý vẫn tiếp tục với việc gửi Kafka message

    // Verify rằng tất cả account đều được gửi qua Kafka
    verify(kafkaProducerService, times(1)).sendCoinAccountUpdate(EVENT_ID, account1);
    verify(kafkaProducerService, times(1)).sendCoinAccountUpdate(EVENT_ID, account2);

    // Verify rằng account2 vẫn được lưu vào cache mặc dù account1 gây ra exception
    verify(accountCache, times(1)).addAccountToBatch(account2);
  }

  @Test
  @DisplayName("Test cho việc processOutput xử lý tập hợp rỗng")
  void processOutput_ShouldHandleEmptyCollections() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Tạo đối tượng đơn lẻ
    Account singleAccount = AccountFactory.create("btc:main");

    ProcessResult result = ProcessResult.success(event);

    // Thiết lập đối tượng đơn lẻ, nhưng tập hợp sẽ là rỗng
    result.setAccount(singleAccount);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    // Kiểm tra rằng chỉ đối tượng đơn lẻ được xử lý
    verify(accountCache).addAccountToBatch(singleAccount);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, singleAccount);

    // Kiểm tra rằng không có đối tượng phụ nào được xử lý
    // Không thể kiểm tra verify(never()) vì không có cách để biết chính xác phương
    // thức nào không được gọi
    // Thay vào đó, chúng ta kiểm tra tương tác phương thức đơn lẻ được gọi đúng số
    // lần
    verify(accountCache, times(1)).addAccountToBatch(any(Account.class));
    verify(kafkaProducerService, times(1)).sendCoinAccountUpdate(any(String.class), any(Account.class));
    verify(accountHistoryCache, never()).addHistoryToBatch(any(AccountHistory.class));
  }

  @Test
  @DisplayName("Test cho việc processOutput xử lý try-catch khi xử lý tập hợp accountHistories")
  void processOutput_ShouldHandleExceptions_WhenProcessingAccountHistories() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Tạo các AccountHistory
    AccountHistory history1 = AccountHistoryFactory.createForDeposit("btc:user1", "tx1");
    AccountHistory history2 = AccountHistoryFactory.createForDeposit("eth:user2", "tx2");

    ProcessResult result = ProcessResult.success(event);

    // Thêm AccountHistory vào tập hợp - history1 sẽ được xử lý trước
    result.addAccountHistory(history1);
    result.addAccountHistory(history2);

    // Setup ngoại lệ cho history1 - điều này sẽ dừng vòng lặp forEach và history2
    // không được xử lý
    doThrow(new RuntimeException("Error processing history")).when(accountHistoryCache)
        .addHistoryToBatch(history1);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    // Verify rằng history1 được gọi (gây ra ngoại lệ)
    verify(accountHistoryCache, times(1)).addHistoryToBatch(history1);

    // Verify rằng history2 KHÔNG được gọi vì vòng lặp forEach đã bị gián đoạn bởi
    // ngoại lệ
    verify(accountHistoryCache, never()).addHistoryToBatch(history2);

    // Verify rằng quá trình xử lý vẫn tiếp tục mặc dù có ngoại lệ
    // (tức là ngoại lệ đã được bắt bởi khối try-catch)
    verify(storageService, never()).flushToDisk(); // endOfBatch là false
  }

  @Test
  @DisplayName("Test processing multiple accounts from accounts map")
  void processOutput_ShouldProcessMultipleAccounts_WhenResultContainsAccountsMap() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Create multiple accounts
    Account account1 = AccountFactory.create("btc:user123");
    Account account2 = AccountFactory.create("eth:user123");
    Account account3 = AccountFactory.create("usdt:user123");

    ProcessResult result = ProcessResult.success(event);

    // Add accounts to the accounts map
    result.addAccount(account1);
    result.addAccount(account2);
    result.addAccount(account3);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then - Verify all accounts are added to account cache
    verify(accountCache).addAccountToBatch(account1);
    verify(accountCache).addAccountToBatch(account2);
    verify(accountCache).addAccountToBatch(account3);

    // And - Verify Kafka messages are sent for all accounts
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, account1);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, account2);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, account3);
  }

  @Test
  @DisplayName("Test cho việc processOutput nên gửi event thành công khi result chứa ammOrder")
  void processOutput_ShouldProcessAmmOrder_WhenResultContainsAmmOrder() throws Exception {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.withAmmOrderEvent();
    AmmOrder ammOrder = mock(AmmOrder.class);
    lenient().when(ammOrder.getIdentifier()).thenReturn(IDENTIFIER);

    ProcessResult result = new ProcessResult(event);
    result.setAmmOrder(ammOrder);

    // Mock AmmOrderCache
    AmmOrderCache ammOrderCache = mock(AmmOrderCache.class);
    lenient().when(storageService.getAmmOrderCache()).thenReturn(ammOrderCache);

    // Act
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Assert
    verify(kafkaProducerService).sendAmmOrderUpdate(result); // Check that order update is sent to Kafka
    verify(kafkaProducerService, never()).sendTransactionResult(any(DisruptorEvent.class)); // Transaction result
                                                                                            // shouldn't be sent for
                                                                                            // AmmOrder
  }

  @Test
  @DisplayName("Test cho việc processOutput nên gửi event thành công khi result chứa offer")
  void processOutput_ShouldProcessOffer_WhenResultContainsOffer() throws Exception {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.create();
    Offer offer = OfferFactory.create();

    ProcessResult result = new ProcessResult(event);
    result.setOffer(offer);

    // Act
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Assert
    verify(kafkaProducerService).sendOfferUpdate(result);
    verify(offerCache).addOfferToBatch(offer);
  }

  @Test
  @DisplayName("Test cho việc processOutput nên gửi event thành công khi result chứa trade")
  void processOutput_ShouldProcessTrade_WhenResultContainsTrade() throws Exception {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.create();
    Trade trade = TradeFactory.create();

    ProcessResult result = new ProcessResult(event);
    result.setTrade(trade);

    // Act
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Assert
    verify(kafkaProducerService).sendTradeUpdate(result);
    verify(tradeCache).addTradeToBatch(trade);
  }

  @Test
  @DisplayName("Test cho việc processOutput nên gửi event thành công khi result chứa cả seller và buyer account")
  void processOutput_ShouldProcessBuyerAndSellerAccounts_WhenResultContainsBoth() throws Exception {
    // Arrange
    DisruptorEvent event = createTestEventWithId(EVENT_ID);
    Account buyerAccount = AccountFactory.create("btc:buyer123");
    Account sellerAccount = AccountFactory.create("btc:seller123");

    ProcessResult result = new ProcessResult(event);
    result.setBuyerAccount(buyerAccount);
    result.setSellerAccount(sellerAccount);

    // Act
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Assert
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, buyerAccount);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, sellerAccount);
    verify(accountCache).addAccountToBatch(buyerAccount);
    verify(accountCache).addAccountToBatch(sellerAccount);
  }

  @Test
  @DisplayName("Test cho việc processOutput xử lý exception khi sendOfferUpdate gặp lỗi")
  void processOutput_ShouldHandleException_WhenSendOfferUpdateFails() throws Exception {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.create();
    Offer offer = OfferFactory.create();

    ProcessResult result = new ProcessResult(event);
    result.setOffer(offer);

    // Giả lập lỗi khi gửi Kafka message
    doThrow(new RuntimeException("Kafka error")).when(kafkaProducerService).sendOfferUpdate(any(ProcessResult.class));

    // Act
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Assert
    // Verify rằng ngoại lệ được xử lý và luồng storage vẫn hoạt động
    verify(offerCache).addOfferToBatch(offer);
  }

  @Test
  @DisplayName("Test cho việc processOutput xử lý exception khi sendTradeUpdate gặp lỗi")
  void processOutput_ShouldHandleException_WhenSendTradeUpdateFails() throws Exception {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.create();
    Trade trade = TradeFactory.create();

    ProcessResult result = new ProcessResult(event);
    result.setTrade(trade);

    // Giả lập lỗi khi gửi Kafka message
    doThrow(new RuntimeException("Kafka error")).when(kafkaProducerService).sendTradeUpdate(any(ProcessResult.class));

    // Act
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Assert
    // Verify rằng ngoại lệ được xử lý và luồng storage vẫn hoạt động
    verify(tradeCache).addTradeToBatch(trade);
  }

  @Test
  @DisplayName("Test cho việc processOutput nên gửi tick updates khi result chứa ticks")
  void processOutput_ShouldSendTickUpdates_WhenResultContainsTicks() throws Exception {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.create();

    // Create ticks
    Tick tick1 = new TickFactory().createTick("BTC-USDT", 1000);
    Tick tick2 = new TickFactory().createTick("ETH-USDT", 2000);

    ProcessResult result = new ProcessResult(event);
    result.addTick(tick1);
    result.addTick(tick2);

    // Act
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Assert
    verify(kafkaProducerService).sendTickUpdate(tick1);
    verify(kafkaProducerService).sendTickUpdate(tick2);
  }

  @Test
  @DisplayName("Test cho việc processOutput xử lý exception khi sendTickUpdate gặp lỗi")
  void processOutput_ShouldHandleException_WhenSendTickUpdateFails() throws Exception {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.create();
    Tick tick = new TickFactory().createTick("BTC-USDT", 1000);

    ProcessResult result = new ProcessResult(event);
    result.addTick(tick);

    // Giả lập lỗi khi gửi Kafka message
    doThrow(new RuntimeException("Kafka error")).when(kafkaProducerService).sendTickUpdate(any(Tick.class));

    // Act
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Assert
    // Verify rằng ngoại lệ được xử lý và không ảnh hưởng đến luồng xử lý
    verify(kafkaProducerService).sendTickUpdate(tick);
  }

  @Test
  @DisplayName("Test cho việc processOutput xử lý exception khi addOfferToBatch gặp lỗi")
  void processOutput_ShouldHandleException_WhenAddOfferToBatchFails() throws Exception {
    // Arrange
    DisruptorEvent event = DisruptorEventFactory.create();
    Offer offer = OfferFactory.create();

    ProcessResult result = new ProcessResult(event);
    result.setOffer(offer);

    // Giả lập lỗi khi thêm vào batch
    doThrow(new RuntimeException("Storage error")).when(offerCache).addOfferToBatch(any(Offer.class));

    // Act
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Assert
    // Verify rằng Kafka message vẫn được gửi mặc dù có lỗi storage
    verify(kafkaProducerService).sendOfferUpdate(result);
  }

  @Test
  @DisplayName("Test cho việc processOutput xử lý đầy đủ tất cả các loại đối tượng")
  void processOutput_ShouldProcessAllTypesOfObjects() throws Exception {
    // Arrange
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Tạo tất cả các loại đối tượng
    Account account = AccountFactory.create("btc:main");
    Account fiatAccount = AccountFactory.create("usd:main");
    Account coinAccount = AccountFactory.create("eth:main");
    Account buyerAccount = AccountFactory.create("btc:buyer");
    Account sellerAccount = AccountFactory.create("btc:seller");

    AccountHistory accountHistory = AccountHistoryFactory.createForDeposit("btc:main", "tx1");
    AccountHistory fiatAccountHistory = AccountHistoryFactory.createForDeposit("usd:main", "tx2");
    AccountHistory coinAccountHistory = AccountHistoryFactory.createForDeposit("eth:main", "tx3");

    CoinDeposit deposit = CoinDepositFactory.createDefaultCoinDeposit();
    CoinWithdrawal withdrawal = CoinWithdrawalFactory.createDefaultCoinWithdrawal();

    AmmPool ammPool = AmmPoolFactory.createDefaultAmmPool();
    MerchantEscrow merchantEscrow = MerchantEscrowFactory.createDefault();
    AmmPosition ammPosition = AmmPositionFactory.createDefaultAmmPosition();
    AmmOrder ammOrder = mock(AmmOrder.class);
    lenient().when(ammOrder.getIdentifier()).thenReturn("order123");

    Offer offer = OfferFactory.create();
    Trade trade = TradeFactory.create();

    // Thêm accounts và histories vào collections
    Account collectionAccount1 = AccountFactory.create("xrp:user1");
    Account collectionAccount2 = AccountFactory.create("ltc:user2");

    AccountHistory collectionHistory1 = AccountHistoryFactory.createForDeposit("xrp:user1", "txc1");
    AccountHistory collectionHistory2 = AccountHistoryFactory.createForDeposit("ltc:user2", "txc2");

    // Tạo ProcessResult với tất cả các đối tượng
    ProcessResult result = ProcessResult.success(event);
    result.setAccount(account)
        .setFiatAccount(fiatAccount)
        .setCoinAccount(coinAccount)
        .setBuyerAccount(buyerAccount)
        .setSellerAccount(sellerAccount)
        .setAccountHistory(accountHistory)
        .setFiatAccountHistory(fiatAccountHistory)
        .setCoinAccountHistory(coinAccountHistory)
        .setDeposit(deposit)
        .setWithdrawal(withdrawal)
        .setAmmPool(ammPool)
        .setMerchantEscrow(merchantEscrow)
        .setAmmPosition(ammPosition)
        .setAmmOrder(ammOrder)
        .setOffer(offer)
        .setTrade(trade)
        .addAccount(collectionAccount1)
        .addAccount(collectionAccount2)
        .addAccountHistory(collectionHistory1)
        .addAccountHistory(collectionHistory2);

    // Act
    outputProcessor.processOutput(result, true);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Assert - Verify rằng tất cả các loại xử lý đều được thực hiện

    // Verify storageService
    verify(storageService).flushToDisk(); // endOfBatch == true

    // Verify account cache
    verify(accountCache).addAccountToBatch(account);
    verify(accountCache).addAccountToBatch(fiatAccount);
    verify(accountCache).addAccountToBatch(coinAccount);
    verify(accountCache).addAccountToBatch(buyerAccount);
    verify(accountCache).addAccountToBatch(sellerAccount);
    verify(accountCache).addAccountToBatch(collectionAccount1);
    verify(accountCache).addAccountToBatch(collectionAccount2);

    // Verify history cache
    verify(accountHistoryCache).addHistoryToBatch(accountHistory);
    verify(accountHistoryCache).addHistoryToBatch(fiatAccountHistory);
    verify(accountHistoryCache).addHistoryToBatch(coinAccountHistory);
    verify(accountHistoryCache).addHistoryToBatch(collectionHistory1);
    verify(accountHistoryCache).addHistoryToBatch(collectionHistory2);

    // Verify other caches
    verify(depositCache).addDepositToBatch(deposit);
    verify(withdrawalCache).addWithdrawalToBatch(withdrawal);
    verify(ammPoolCache).addAmmPoolToBatch(ammPool);
    verify(ammPositionCache).addAmmPositionToBatch(ammPosition);
    verify(ammOrderCache).addAmmOrderToBatch(ammOrder);
    verify(offerCache).addOfferToBatch(offer);
    verify(tradeCache).addTradeToBatch(trade);

    // Verify Kafka messages
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, account);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, fiatAccount);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, coinAccount);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, buyerAccount);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, sellerAccount);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, collectionAccount1);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, collectionAccount2);
    verify(kafkaProducerService).sendAmmPoolUpdate(result);
    verify(kafkaProducerService).sendMerchantEscrowUpdate(result);
    verify(kafkaProducerService).sendAmmPositionUpdate(result);
    verify(kafkaProducerService).sendAmmOrderUpdate(result);
    verify(kafkaProducerService).sendOfferUpdate(result);
    verify(kafkaProducerService).sendTradeUpdate(result);
  }

  @Test
  @DisplayName("Should send recipient account update to Kafka")
  void sendRecipientAccountUpdateToKafka() {
    // Arrange
    Account senderAccount = AccountFactory.create("btc:user123");
    Account recipientAccount = AccountFactory.create("btc:user456");

    AccountHistory senderHistory = new AccountHistory(
        senderAccount.getKey(), "tx123", "coin_withdrawal_releasing");
    AccountHistory recipientHistory = new AccountHistory(
        recipientAccount.getKey(), "tx123", "recipient_coin_withdrawal_releasing");

    CoinWithdrawal withdrawal = mock(CoinWithdrawal.class);
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    ProcessResult result = new ProcessResult(event);
    result.setAccount(senderAccount)
        .setAccountHistory(senderHistory)
        .setRecipientAccount(recipientAccount)
        .setRecipientAccountHistory(recipientHistory)
        .setWithdrawal(withdrawal);

    // Act
    outputProcessor.processOutput(result, true);

    // Assert
    // Verify that both sender and recipient account updates were sent to Kafka
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, senderAccount);
    verify(kafkaProducerService).sendCoinAccountUpdate(EVENT_ID, recipientAccount);

    // Verify that both accounts were stored
    verify(accountCache).addAccountToBatch(senderAccount);
    verify(accountCache).addAccountToBatch(recipientAccount);

    // Verify that transaction result is sent
    verify(kafkaProducerService).sendTransactionResult(event);
  }

  @Test
  @DisplayName("Test sendEventToKafka sends updates for both accounts when recipient account is present")
  void sendEventToKafka_ShouldSendCoinAccountUpdateForBothAccounts_WhenRecipientAccountIsPresent() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Create a new mock KafkaProducerService specifically for this test
    KafkaProducerService mockKafka = mock(KafkaProducerService.class, Mockito.RETURNS_DEFAULTS);

    // Create a new OutputProcessor with the mock
    Field kafkaProducerServiceField = OutputProcessor.class.getDeclaredField("kafkaProducerService");
    kafkaProducerServiceField.setAccessible(true);
    kafkaProducerServiceField.set(outputProcessor, mockKafka);

    // Create sender and recipient accounts
    Account senderAccount = AccountFactory.create("btc:sender:123");
    Account recipientAccount = AccountFactory.create("btc:recipient:456");

    ProcessResult result = ProcessResult.success(event);
    result.setAccount(senderAccount);
    result.setRecipientAccount(recipientAccount);

    // Make sendEventToKafka method accessible
    Method sendEventToKafkaMethod = OutputProcessor.class.getDeclaredMethod("sendEventToKafka", ProcessResult.class);
    sendEventToKafkaMethod.setAccessible(true);

    // When
    sendEventToKafkaMethod.invoke(outputProcessor, result);

    // Then
    verify(mockKafka).sendCoinAccountUpdate(EVENT_ID, senderAccount);
    verify(mockKafka).sendCoinAccountUpdate(EVENT_ID, recipientAccount);
    verify(mockKafka).sendTransactionResult(event);
  }

  @Test
  @DisplayName("Test for processing balance lock")
  void processOutput_ShouldProcessBalanceLock_WhenResultContainsBalanceLock() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Create BalanceLock
    BalanceLock balanceLock = BalanceLockFactory.create();

    ProcessResult result = ProcessResult.success(event);
    result.setBalanceLock(balanceLock);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

         // Then
     verify(balanceLockCache).addBalanceLockToBatch(balanceLock);
     verify(kafkaProducerService).sendBalanceLockUpdate(result);
  }

  @Test
  @DisplayName("Test for processing coin withdrawal")
  void processOutput_ShouldProcessCoinWithdrawal_WhenResultContainsCoinWithdrawal() throws Exception {
    // Given
    DisruptorEvent event = createTestEventWithId(EVENT_ID);

    // Create CoinWithdrawal
    CoinWithdrawal withdrawal = CoinWithdrawalFactory.create("btc:user123", "withdrawal-123", new BigDecimal("1.0"));

    ProcessResult result = ProcessResult.success(event);
    result.setWithdrawal(withdrawal);

    // When
    outputProcessor.processOutput(result, false);

    // Sleep briefly to allow async tasks to complete
    Thread.sleep(100);

    // Then
    verify(withdrawalCache).addWithdrawalToBatch(withdrawal);
    verify(kafkaProducerService).sendCoinWithdrawalUpdate(result);
  }
}
