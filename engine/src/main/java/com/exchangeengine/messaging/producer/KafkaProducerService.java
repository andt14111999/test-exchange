package com.exchangeengine.messaging.producer;

import com.exchangeengine.messaging.common.KafkaConfig;
import com.exchangeengine.model.Account;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.KafkaTopics;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.model.Tick;
import com.exchangeengine.storage.StorageService;
import com.exchangeengine.util.EnvManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Service to send events to Kafka.
 * Use shared producer from KafkaConfig.
 * Send event callback to KafkaProducerService for Rails BE receive.
 */
public class KafkaProducerService {
  private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

  // Thay đổi từ static final thành instance field để có thể thay thế trong
  // testing
  private EnvManager envManager;

  // Singleton instance
  private static volatile KafkaProducerService instance;

  private final KafkaProducer<String, String> producer;
  private final StorageService storageService;
  private final ObjectMapper objectMapper;

  /**
   * Lấy instance của KafkaProducerService.
   *
   * @return Instance của KafkaProducerService
   */
  public static synchronized KafkaProducerService getInstance() {
    if (instance == null) {
      instance = new KafkaProducerService(KafkaConfig.getInstance().getProducer());
    }
    return instance;
  }

  /**
   * Thiết lập instance kiểm thử (chỉ sử dụng cho testing)
   *
   * @param testInstance Instance kiểm thử cần thiết lập
   */
  public static void setTestInstance(KafkaProducerService testInstance) {
    instance = testInstance;
  }

  /**
   * Reset instance về null (chỉ sử dụng cho testing)
   */
  public static void resetInstance() {
    instance = null;
  }

  /**
   * Constructor chỉ với KafkaProducer.
   * Private để đảm bảo Singleton pattern.
   *
   * @param producer KafkaProducer được truyền vào
   */
  private KafkaProducerService(KafkaProducer<String, String> producer) {
    this.producer = producer;
    this.storageService = StorageService.getInstance();
    this.objectMapper = new ObjectMapper();
    this.envManager = EnvManager.getInstance();
    logger.info("KafkaProducerService initialized with producer from KafkaConfig");
  }

  // Getter và setter cho envManager để hỗ trợ testing
  public EnvManager getEnvManager() {
    return envManager;
  }

  public void setEnvManager(EnvManager envManager) {
    this.envManager = envManager;
  }

  public void sendCoinAccountBalance(String accountKey) {
    Map<String, Object> message = generateBalanceUpdateMessageJson(accountKey);
    String kafkaKey = "coin-account-" + accountKey;
    sendEventToKafka(KafkaTopics.COIN_ACCOUNT_UPDATE_TOPIC, kafkaKey, message);
  }

  public void sendCoinAccountUpdate(String inputEventId, Account account) {
    Map<String, Object> message = account.toMessageJson();
    message.put("inputEventId", inputEventId + "-" + account.getKey());
    String kafkaKey = "coin-account-" + account.getKey();
    sendEventToKafka(KafkaTopics.COIN_ACCOUNT_UPDATE_TOPIC, kafkaKey, message);
  }

  public void sendAmmPoolUpdate(ProcessResult result) {
    String kafkaKey = "amm-pool-" + result.getAmmPool().get().getPair();
    Map<String, Object> message = result.toAmmPoolObjectMessageJson();
    sendEventToKafka(KafkaTopics.AMM_POOL_UPDATE_TOPIC, kafkaKey, message);
  }

  /**
   * Send merchant escrow update to Kafka
   *
   * @param result Process result
   */
  public void sendMerchantEscrowUpdate(ProcessResult result) {
    try {
      Map<String, Object> message = result.toMerchantEscrowObjectMessageJson();
      String kafkaKey = "merchant-escrow-" + result.getMerchantEscrow().get().getIdentifier();
      sendEventToKafka(KafkaTopics.MERCHANT_ESCROW_UPDATE_TOPIC, kafkaKey, message);
    } catch (Exception e) {
      logger.error("Error generating merchant escrow update message: {}", e.getMessage(), e);
    }
  }

  /**
   * Gửi thông tin cập nhật về vị thế AMM đến Kafka
   *
   * @param result Kết quả xử lý chứa thông tin về vị thế AMM
   */
  public void sendAmmPositionUpdate(ProcessResult result) {
    String kafkaKey = "amm-position-" + result.getAmmPosition().get().getIdentifier();
    Map<String, Object> message = result.toAmmPositionObjectMessageJson();
    sendEventToKafka(KafkaTopics.AMM_POSITION_UPDATE_TOPIC, kafkaKey, message);
  }

  /**
   * Gửi thông tin cập nhật về lệnh AMM đến Kafka
   *
   * @param result Kết quả xử lý chứa thông tin về lệnh AMM
   */
  public void sendAmmOrderUpdate(ProcessResult result) {
    String kafkaKey = "amm-order-" + result.getAmmOrder().get().getIdentifier();
    Map<String, Object> message = result.toAmmOrderObjectMessageJson();
    sendEventToKafka(KafkaTopics.AMM_ORDER_UPDATE_TOPIC, kafkaKey, message);
  }

  /**
   * Send offer update to Kafka
   *
   * @param result Process result containing offer information
   */
  public void sendOfferUpdate(ProcessResult result) {
    try {
      if (!result.getOffer().isPresent()) {
        logger.warn("Cannot send offer update: Offer is not present in result");
        return;
      }

      String kafkaKey = "offer-" + result.getOffer().get().getIdentifier();
      Map<String, Object> message = result.toOfferObjectMessageJson();

      sendEventToKafka(KafkaTopics.OFFER_UPDATE_TOPIC, kafkaKey, message);
    } catch (Exception e) {
      logger.error("Error generating offer update message: {}", e.getMessage(), e);
    }
  }

  /**
   * Send trade update to Kafka
   *
   * @param result Process result containing trade information
   */
  public void sendTradeUpdate(ProcessResult result) {
    try {
      if (!result.getTrade().isPresent()) {
        logger.warn("Cannot send trade update: Trade is not present in result");
        return;
      }

      String kafkaKey = "trade-" + result.getTrade().get().getIdentifier();
      Map<String, Object> message = result.toTradeObjectMessageJson();

      sendEventToKafka(KafkaTopics.TRADE_UPDATE_TOPIC, kafkaKey, message);
    } catch (Exception e) {
      logger.error("Error generating trade update message: {}", e.getMessage(), e);
    }
  }

  /**
   * send transaction result.
   *
   * @param event event processed by business logic processor
   */
  public void sendTransactionResult(DisruptorEvent event) {
    if (event == null) {
      logger.warn("Cannot send transaction result for null event");
      return;
    }

    Map<String, Object> message = generateTransactionResultMessageJson(event);
    String kafkaKey = "transaction-result-" + event.getProducerKey();
    sendEventToKafka(KafkaTopics.TRANSACTION_RESPONSE_TOPIC, kafkaKey, message);
  }

  /**
   * Send transaction result not processed.
   *
   * @param message message
   */
  public void sendTransactionResultNotProcessed(Map<String, Object> message) {
    String kafkaKey = "error-" + UUID.randomUUID().toString();
    sendEventToKafka(KafkaTopics.TRANSACTION_RESPONSE_TOPIC, kafkaKey, message);
  }

  /**
   * Send tick update to Kafka
   *
   * @param tick Tick to send
   */
  public void sendTickUpdate(Tick tick) {
    if (tick == null) {
      logger.debug("No tick to send");
      return;
    }

    String kafkaKey = "tick-update-" + tick.getTickKey();
    Map<String, Object> message = tick.toMessageJson();
    sendEventToKafka(KafkaTopics.TICK_UPDATE_TOPIC, kafkaKey, message);
  }

  /**
   * Reset balance for development and test environment only.
   *
   * @param accountKey accountKey
   */
  public void resetCoinAccount(String accountKey) {
    String appEnv = envManager.get("APP_ENV", "development");
    logger.info("Checking if can reset balance in environment '{}' for accountKey={}", appEnv, accountKey);

    if ("development".equals(appEnv) || "test".equals(appEnv)) {
      logger.info("Resetting account balance for accountKey={} in {} environment", accountKey, appEnv);
      storageService.getAccountCache().resetAccount(accountKey);
    } else {
      logger.info("Cannot reset balance in '{}' environment: accountKey={}", appEnv, accountKey);
    }
  }

  /**
   * Send a balance lock update to Kafka
   *
   * @param result ProcessResult containing balance lock data
   */
  public void sendBalanceLockUpdate(ProcessResult result) {
    if (!result.getBalanceLock().isPresent()) {
      logger.warn("Cannot send balance lock update: BalanceLock is not present in result");
      return;
    }

    try {
      Map<String, Object> messageJson = result.toBalanceLockObjectMessageJson();
      String kafkaKey = "balance-lock-" + result.getBalanceLock().get().getLockId();
      sendEventToKafka(KafkaTopics.BALANCES_LOCK_UPDATE_TOPIC, kafkaKey, messageJson);

      logger.info("Balance lock update sent: lockId={}, status={}",
          result.getBalanceLock().get().getLockId(),
          result.getBalanceLock().get().getStatus());
    } catch (Exception e) {
      logger.error("Error sending balance lock update to Kafka: {}", e.getMessage(), e);
    }
  }

  /**
   * Send a coin withdrawal update to Kafka
   *
   * @param result ProcessResult containing coin withdrawal data
   */
  public void sendCoinWithdrawalUpdate(ProcessResult result) {
    if (!result.getWithdrawal().isPresent()) {
      logger.warn("Cannot send coin withdrawal update: CoinWithdrawal is not present in result");
      return;
    }

    try {
      Map<String, Object> messageJson = result.toCoinWithdrawalObjectMessageJson();
      String kafkaKey = "coin-withdrawal-" + result.getWithdrawal().get().getIdentifier();
      sendEventToKafka(KafkaTopics.COIN_WITHDRAWAL_UPDATE_TOPIC, kafkaKey, messageJson);

      logger.info("Coin withdrawal update sent: identifier={}, status={}",
          result.getWithdrawal().get().getIdentifier(),
          result.getWithdrawal().get().getStatus());
    } catch (Exception e) {
      logger.error("Error sending coin withdrawal update to Kafka: {}", e.getMessage(), e);
    }
  }

  // private methods

  /**
   * send event to kafka.
   *
   * @param topic   topic
   * @param key     key
   * @param message message
   */
  private void sendEventToKafka(String topic, String key, Map<String, Object> message) {
    if (message == null) {
      return;
    }

    try {
      if (message.get("messageId") == null) {
        message.put("messageId", UUID.randomUUID().toString());
      }
      String newMessageJson = objectMapper.writeValueAsString(message);

      ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, newMessageJson);

      Future<RecordMetadata> future = producer.send(record, (metadata, exception) -> {
        if (exception != null) {
          logger.error("Error sending event to Kafka: {}, metadata: {}", exception.getMessage(), metadata);
          for (int i = 0; i < 2; i++) {
            producer.send(record, (metadata1, exception1) -> {
              if (exception1 == null) {
                return;
              }
              logger.error("Retry error sending event to Kafka: {}, metadata: {}", exception1.getMessage(),
                  metadata1);
            });
          }
        }
      });

      RecordMetadata metadata = future.get();
      logger.info("Send event {} to Kafka: message={}, partition={}, offset={}", topic, newMessageJson,
          metadata.partition(), metadata.offset());
    } catch (Exception e) {
      logger.error("Error sending event to Kafka: {}, message: {}", e.getMessage(), message);
    }
  }

  /**
   * generate message json.
   *
   * @param event event
   * @return message json
   */
  private Map<String, Object> generateTransactionResultMessageJson(DisruptorEvent event) {
    try {
      Map<String, Object> message = event.toOperationObjectMessageJson();
      return message;
    } catch (Exception e) {
      logger.error("Error generating message JSON: {}", e.getMessage(), e);
      return null;
    }
  }

  /**
   * Generate balance update message json.
   *
   * @param accountKey accountKey
   * @return message json
   */
  private Map<String, Object> generateBalanceUpdateMessageJson(String accountKey) {
    Optional<Account> account = storageService.getAccountCache().getAccount(accountKey);
    if (account.isPresent()) {
      return account.get().toMessageJson();
    } else {
      throw new IllegalStateException("Account not found: " + accountKey);
    }
  }

  /**
   * close producer.
   * do not close producer because it is shared.
   */
  public void close() {
    // do not close producer because it is shared
    logger.info("KafkaProducerService closed (shared producer still active)");
  }
}
