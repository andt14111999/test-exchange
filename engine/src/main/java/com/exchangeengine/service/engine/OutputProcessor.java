package com.exchangeengine.service.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.messaging.producer.KafkaProducerService;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.storage.StorageService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is responsible for processing output after the Business Logic
 * Processor has calculated. It includes:
 * 1. Sending events to Kafka
 * 2. Saving to storage
 *
 * Use thread pool to process these tasks asynchronously.
 */
public class OutputProcessor {
  private static final Logger logger = LoggerFactory.getLogger(OutputProcessor.class);

  // Singleton instance
  private static volatile OutputProcessor instance;

  private final StorageService storageService;
  private final KafkaProducerService kafkaProducerService;
  private final ExecutorService storageExecutor;
  private final ExecutorService kafkaExecutor;

  // Number of threads for each type of processing
  private static final int STORAGE_THREADS = 2;
  private static final int KAFKA_THREADS = 3;

  /**
   * Lấy instance của OutputProcessor.
   *
   * @return Instance của OutputProcessor
   */
  public static synchronized OutputProcessor getInstance() {
    if (instance == null) {
      instance = new OutputProcessor(KafkaProducerService.getInstance());
    }
    return instance;
  }

  /**
   * Thiết lập instance kiểm thử (chỉ sử dụng cho testing)
   *
   * @param testInstance Instance kiểm thử cần thiết lập
   */
  public static void setTestInstance(OutputProcessor testInstance) {
    instance = testInstance;
  }

  /**
   * Reset instance về null (chỉ sử dụng cho testing)
   */
  public static void resetInstance() {
    instance = null;
  }

  /**
   * Initialize OutputProcessor với KafkaProducerService.
   * Private để đảm bảo Singleton pattern.
   *
   * @param kafkaProducerService Service for sending Kafka
   */
  private OutputProcessor(KafkaProducerService kafkaProducerService) {
    this.storageService = StorageService.getInstance();
    this.kafkaProducerService = kafkaProducerService;

    // Initialize thread pool for kafka with clear thread names
    this.kafkaExecutor = Executors.newFixedThreadPool(KAFKA_THREADS, new ThreadFactory() {
      private final AtomicInteger counter = new AtomicInteger(1);

      @Override
      public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, "kafka-output-processor-" + counter.getAndIncrement());
        thread.setDaemon(true);
        return thread;
      }
    });

    // Initialize thread pool for storage with clear thread names
    this.storageExecutor = Executors.newFixedThreadPool(STORAGE_THREADS, new ThreadFactory() {
      private final AtomicInteger counter = new AtomicInteger(1);

      @Override
      public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, "storage-output-processor-" + counter.getAndIncrement());
        thread.setDaemon(true);
        return thread;
      }
    });

    logger.info("OutputProcessor initialized with {} storage threads, {} kafka threads",
        STORAGE_THREADS, KAFKA_THREADS);
  }

  /**
   * Process output after calculation
   * This method will send event to executors to process asynchronously
   *
   * @param result     ProcessResult chứa kết quả xử lý
   * @param endOfBatch Flag for end of batch
   */
  public void processOutput(final ProcessResult result, final boolean endOfBatch) {
    // For faster testing, process Kafka directly when account and recipient account
    // are both present
    if (result.getAccount().isPresent() && result.getRecipientAccount().isPresent()) {
      // Process immediately to ensure account updates are sent in the expected order
      sendEventToKafka(result);
    } else {
      // Process sending Kafka asynchronously for other cases
      kafkaExecutor.submit(() -> {
        sendEventToKafka(result);
      });
    }

    // Process storage asynchronously
    storageExecutor.submit(() -> {
      processStorageAsynchronously(result, endOfBatch);
    });
  }

  /**
   * send event to kafka.
   *
   * @param result ProcessResult chứa kết quả xử lý
   */
  private void sendEventToKafka(ProcessResult result) {
    try {
      String inputEventId = result.getEvent().getEventId();
      // Send all account updates first
      if (result.getAccount().isPresent()) {
        kafkaProducerService.sendCoinAccountUpdate(inputEventId, result.getAccount().get());
      }

      if (result.getRecipientAccount().isPresent()) {
        kafkaProducerService.sendCoinAccountUpdate(inputEventId, result.getRecipientAccount().get());
      }

      if (result.getFiatAccount().isPresent()) {
        kafkaProducerService.sendCoinAccountUpdate(inputEventId, result.getFiatAccount().get());
      }

      if (result.getCoinAccount().isPresent()) {
        kafkaProducerService.sendCoinAccountUpdate(inputEventId, result.getCoinAccount().get());
      }

      if (result.getBuyerAccount().isPresent()) {
        kafkaProducerService.sendCoinAccountUpdate(inputEventId, result.getBuyerAccount().get());
      }

      if (result.getSellerAccount().isPresent()) {
        kafkaProducerService.sendCoinAccountUpdate(inputEventId, result.getSellerAccount().get());
      }

      if (!result.getAccounts().isEmpty()) {
        result.getAccounts().values().forEach(account -> {
          kafkaProducerService.sendCoinAccountUpdate(inputEventId, account);
        });
      }

      if (result.getBalanceLock().isPresent()) {
        kafkaProducerService.sendBalanceLockUpdate(result);
      }

      if (result.getWithdrawal().isPresent()) {
        kafkaProducerService.sendCoinWithdrawalUpdate(result);
      }

      if (result.getAmmOrder().isPresent()) {
        kafkaProducerService.sendAmmOrderUpdate(result);
      }

      if (result.getAmmPool().isPresent()) {
        kafkaProducerService.sendAmmPoolUpdate(result);
      }

      if (result.getMerchantEscrow().isPresent()) {
        kafkaProducerService.sendMerchantEscrowUpdate(result);
      }

      if (result.getAmmPosition().isPresent()) {
        kafkaProducerService.sendAmmPositionUpdate(result);
      }

      if (result.getOffer().isPresent()) {
        kafkaProducerService.sendOfferUpdate(result);
      }

      if (result.getTrade().isPresent()) {
        kafkaProducerService.sendTradeUpdate(result);
      }

      if (!result.getTicks().isEmpty()) {
        result.getTicks().forEach(tick -> {
          kafkaProducerService.sendTickUpdate(tick);
        });
      }

      if (result.getAmmPool().isEmpty() &&
          result.getAmmPosition().isEmpty() &&
          result.getAmmOrder().isEmpty()) {
        kafkaProducerService.sendTransactionResult(result.getEvent());
      }

      logger.debug("Event sent to Kafka: {}", result.getEvent().getEventId());
    } catch (Exception e) {
      logger.error("OutputProcessor: Error sending event to Kafka: {}, result={}", e.getMessage(), result);
    }
  }

  /**
   * Process storage asynchronously
   *
   * @param result     ProcessResult chứa kết quả xử lý
   * @param endOfBatch Flag for end of batch
   */
  private void processStorageAsynchronously(ProcessResult result, boolean endOfBatch) {
    try {
      // Xử lý dữ liệu từ ProcessResult
      processResultData(result);

      // Kiểm tra và flush nếu cần
      if (endOfBatch || storageService.shouldFlush()) {
        storageService.flushToDisk();
        logger.debug("Data flushed to disk after processing event: {}", result.getEvent().getEventId());
      }
    } catch (Exception e) {
      logger.error("Error processing storage for event {}: {}", result.getEvent().getEventId(),
          e.getMessage(), e);
    }
  }

  /**
   * Xử lý dữ liệu từ ProcessResult và cập nhật vào batch
   *
   * @param result ProcessResult chứa dữ liệu cần xử lý
   */
  private void processResultData(ProcessResult result) {
    try {
      // Xử lý dữ liệu Account nếu có
      result.getAccount().ifPresent(account -> {
        storageService.getAccountCache().addAccountToBatch(account);
      });

      // Xử lý dữ liệu Recipient Account nếu có
      result.getRecipientAccount().ifPresent(recipientAccount -> {
        storageService.getAccountCache().addAccountToBatch(recipientAccount);
      });

      // Xử lý dữ liệu Fiat Account nếu có
      result.getFiatAccount().ifPresent(fiatAccount -> {
        storageService.getAccountCache().addAccountToBatch(fiatAccount);
      });

      // Xử lý dữ liệu Coin Account nếu có
      result.getCoinAccount().ifPresent(coinAccount -> {
        storageService.getAccountCache().addAccountToBatch(coinAccount);
      });

      // Xử lý tập hợp accounts nếu có
      if (!result.getAccounts().isEmpty()) {
        result.getAccounts().values().forEach(account -> {
          storageService.getAccountCache().addAccountToBatch(account);
        });
      }

      // Xử lý dữ liệu CoinDeposit nếu có
      result.getDeposit().ifPresent(deposit -> {
        storageService.getDepositCache().addDepositToBatch(deposit);
      });

      // Xử lý dữ liệu CoinWithdrawal nếu có
      result.getWithdrawal().ifPresent(withdrawal -> {
        storageService.getWithdrawalCache().addWithdrawalToBatch(withdrawal);
      });

      // Cập nhật event cache
      result.getAccountHistory().ifPresent(history -> {
        storageService.getAccountHistoryCache().addHistoryToBatch(history);
      });

      // Cập nhật Recipient Account History cache
      result.getRecipientAccountHistory().ifPresent(history -> {
        storageService.getAccountHistoryCache().addHistoryToBatch(history);
      });

      // Cập nhật Fiat Account History cache
      result.getFiatAccountHistory().ifPresent(history -> {
        storageService.getAccountHistoryCache().addHistoryToBatch(history);
      });

      // Cập nhật Coin Account History cache
      result.getCoinAccountHistory().ifPresent(history -> {
        storageService.getAccountHistoryCache().addHistoryToBatch(history);
      });

      // Xử lý tập hợp accountHistories nếu có
      if (!result.getAccountHistories().isEmpty()) {
        result.getAccountHistories().forEach(history -> {
          storageService.getAccountHistoryCache().addHistoryToBatch(history);
        });
      }

      // Cập nhật AmmPool cache
      result.getAmmPool().ifPresent(ammPool -> {
        storageService.getAmmPoolCache().addAmmPoolToBatch(ammPool);
      });

      // Cập nhật MerchantEscrow cache
      result.getMerchantEscrow().ifPresent(merchantEscrow -> {
        storageService.getMerchantEscrowCache().addMerchantEscrowToBatch(merchantEscrow);
      });

      // Cập nhật AmmPosition cache
      result.getAmmPosition().ifPresent(ammPosition -> {
        storageService.getAmmPositionCache().addAmmPositionToBatch(ammPosition);
      });

      // Cập nhật Offer cache
      result.getOffer().ifPresent(offer -> {
        storageService.getOfferCache().addOfferToBatch(offer);
      });

      // Cập nhật Trade cache
      result.getTrade().ifPresent(trade -> {
        storageService.getTradeCache().addTradeToBatch(trade);
      });

      // Cập nhật Buyer/Seller accounts nếu có
      result.getBuyerAccount().ifPresent(buyerAccount -> {
        storageService.getAccountCache().addAccountToBatch(buyerAccount);
      });

      result.getSellerAccount().ifPresent(sellerAccount -> {
        storageService.getAccountCache().addAccountToBatch(sellerAccount);
      });

      // Cập nhật AmmOrder cache
      result.getAmmOrder().ifPresent(ammOrder -> {
        storageService.getAmmOrderCache().addAmmOrderToBatch(ammOrder);
      });

      // Cập nhật BalanceLock cache
      result.getBalanceLock().ifPresent(balanceLock -> {
        storageService.getBalanceLockCache().addBalanceLockToBatch(balanceLock);
      });

    } catch (Exception e) {
      logger.error("Error processing result data for event {}: {}",
          result.getEvent().getEventId(), e.getMessage(), e);
    }
  }

  /**
   * Đóng processor và giải phóng tài nguyên
   */
  public void shutdown() {
    logger.info("Shutting down OutputProcessor...");

    // Đóng các executor
    storageExecutor.shutdown();
    kafkaExecutor.shutdown();

    // Đảm bảo dữ liệu được lưu trữ trước khi thoát
    try {
      storageService.flushToDisk();
      logger.info("Final flush completed during shutdown");
    } catch (Exception e) {
      logger.error("Error during final flush: {}", e.getMessage(), e);
    }

    logger.info("OutputProcessor shutdown completed");
  }
}
