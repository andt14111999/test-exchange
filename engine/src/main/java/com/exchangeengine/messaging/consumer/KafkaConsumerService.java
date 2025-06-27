package com.exchangeengine.messaging.consumer;

import com.exchangeengine.messaging.producer.KafkaProducerService;
import com.exchangeengine.model.event.AccountEvent;
import com.exchangeengine.model.event.AmmPoolEvent;
import com.exchangeengine.model.event.CoinDepositEvent;
import com.exchangeengine.model.event.CoinWithdrawalEvent;
import com.exchangeengine.model.event.MerchantEscrowEvent;
import com.exchangeengine.model.event.AmmPositionEvent;
import com.exchangeengine.model.event.AmmOrderEvent;
import com.exchangeengine.model.event.BalancesLockEvent;
import com.exchangeengine.model.KafkaTopics;
import com.exchangeengine.service.engine.EngineHandler;
import com.exchangeengine.util.KafkaMessageUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service xử lý các sự kiện logic chính từ Kafka như tạo tài khoản, gửi tiền,
 * rút tiền.
 * Các sự kiện này sẽ được xử lý tuần tự và đưa vào LMAX Disruptor.
 */
public class KafkaConsumerService implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

  public static final String ENGINE_LOGIC_SERVICE_GROUP = "engine-logic-service-group";

  // Singleton instance
  private static volatile KafkaConsumerService instance;

  private final KafkaConsumer<String, String> consumer;
  private final EngineHandler engineHandler;
  private final KafkaProducerService producerService;
  private final ObjectMapper objectMapper;

  private final AtomicBoolean running = new AtomicBoolean(true);

  /**
   * Lấy instance của KafkaConsumerService.
   *
   * @return Instance của KafkaConsumerService
   */
  public static synchronized KafkaConsumerService getInstance() {
    if (instance == null) {
      instance = new KafkaConsumerService(ENGINE_LOGIC_SERVICE_GROUP);
    }
    return instance;
  }

  /**
   * Reset instance tiện cho việc kiểm thử.
   * CẢNH BÁO: Chỉ sử dụng cho mục đích kiểm thử.
   */
  public static void resetInstance() {
    if (instance != null) {
      instance = null;
    }
  }

  /**
   * Thiết lập instance kiểm thử (chỉ sử dụng cho testing)
   *
   * @param testInstance Instance kiểm thử cần thiết lập
   */
  public static void setTestInstance(KafkaConsumerService testInstance) {
    instance = testInstance;
  }

  /**
   * Constructor, create a new consumer for this service.
   * Private để đảm bảo Singleton pattern.
   *
   * @param consumerGroup Consumer group ID
   */
  private KafkaConsumerService(String consumerGroup) {
    // Sử dụng cấu hình chung với readFromBeginning = false để chỉ đọc tin nhắn mới
    // Logic service nên xử lý tất cả tin nhắn từ đầu, nên có thể cấu hình qua biến
    // môi trường
    Properties props = KafkaConsumerConfig.createConsumerConfig(consumerGroup, false, true);

    this.consumer = new KafkaConsumer<>(props);
    this.engineHandler = EngineHandler.getInstance();
    this.producerService = KafkaProducerService.getInstance();
    this.objectMapper = new ObjectMapper();

    // Subscribe vào các topic xử lý logic
    consumer.subscribe(Arrays.asList(KafkaTopics.LOGIC_TOPICS));
    logger.info("KafkaConsumerService initialized and subscribed to logic topics: {}",
        Arrays.toString(KafkaTopics.LOGIC_TOPICS));
  }

  @Override
  public void run() {
    try {
      while (running.get()) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, String> record : records) {
          try {
            processRecord(record);
            consumer.commitSync();
          } catch (Exception e) {
            logger.error("Error processing record: {}", e.getMessage(), e);
          }
        }
      }
    } catch (WakeupException e) {
      // Ignore exception if closing
      if (running.get()) {
        throw e;
      }
    } catch (Exception e) {
      logger.error("Lỗi trong consumer loop: {}", e.getMessage(), e);
    } finally {
      consumer.close();
      logger.info("Consumer closed");
    }
  }

  /**
   * Process a record from Kafka.
   *
   * @param record Record to process
   */
  private void processRecord(ConsumerRecord<String, String> record) {
    try {
      JsonNode messageJson = objectMapper.readTree(record.value());

      logger.debug("Received message: topic={}, partition={}, offset={}, key={}, value={}",
          record.topic(), record.partition(), record.offset(), record.key(), messageJson);

      switch (record.topic()) {
        case KafkaTopics.COIN_ACCOUNT_TOPIC:
          processCreateCoinAccountRequest(messageJson);
          break;
        case KafkaTopics.COIN_DEPOSIT_TOPIC:
          processDepositRequest(messageJson);
          break;
        case KafkaTopics.COIN_WITHDRAWAL_TOPIC:
          processWithdrawRequest(messageJson);
          break;
        case KafkaTopics.AMM_POOL_TOPIC:
          processAmmPoolRequest(messageJson);
          break;
        case KafkaTopics.MERCHANT_ESCROW_TOPIC:
          processMerchantEscrowRequest(messageJson);
          break;
        case KafkaTopics.AMM_POSITION_TOPIC:
          processAmmPositionRequest(messageJson);
          break;
        case KafkaTopics.AMM_ORDER_TOPIC:
          processAmmOrderRequest(messageJson);
          break;
        case KafkaTopics.TRADE_TOPIC:
          processTradeRequest(messageJson);
          break;
        case KafkaTopics.OFFER_TOPIC:
          processOfferRequest(messageJson);
          break;
        case KafkaTopics.BALANCES_LOCK_TOPIC:
          processBalancesLockRequest(messageJson);
          break;
        default:
          logger.warn("Cannot process topic: {}", record.topic());
      }
    } catch (Exception e) {
      logger.error("Error processing message: {}", e.getMessage(), e);
    }
  }

  /**
   * Process create coin account request.
   *
   * @param messageJson Content of request
   */
  private void processCreateCoinAccountRequest(JsonNode messageJson) {
    KafkaMessageUtils.processWithErrorHandling(messageJson, () -> {
      AccountEvent accountEvent = new AccountEvent();
      accountEvent.parserData(messageJson);
      accountEvent.validate();

      engineHandler.createCoinAccount(accountEvent);
    }, producerService, "LogicConsumerCreateCoinAccount");
  }

  /**
   * Process deposit request.
   *
   * @param messageJson Content of request
   */
  private void processDepositRequest(JsonNode messageJson) {
    KafkaMessageUtils.processWithErrorHandling(messageJson, () -> {
      CoinDepositEvent depositEvent = new CoinDepositEvent();
      depositEvent.parserData(messageJson);
      depositEvent.validate();

      engineHandler.deposit(depositEvent);
    }, producerService, "LogicConsumerDeposit");
  }

  /**
   * Process withdraw request.
   *
   * @param messageJson Content of request
   */
  private void processWithdrawRequest(JsonNode messageJson) {
    KafkaMessageUtils.processWithErrorHandling(messageJson, () -> {
      CoinWithdrawalEvent withdrawalEvent = new CoinWithdrawalEvent();
      withdrawalEvent.parserData(messageJson);
      withdrawalEvent.validate();

      engineHandler.withdraw(withdrawalEvent);
    }, producerService, "LogicConsumerWithdraw");
  }

  /**
   * Process amm pool request.
   *
   * @param messageJson Content of request
   */
  private void processAmmPoolRequest(JsonNode messageJson) {
    KafkaMessageUtils.processWithErrorHandling(messageJson, () -> {
      AmmPoolEvent ammPoolEvent = new AmmPoolEvent();
      ammPoolEvent.parserData(messageJson);
      ammPoolEvent.validate();

      engineHandler.ammPool(ammPoolEvent);
    }, producerService, "LogicConsumerAmmPool");
  }

  /**
   * Process merchant escrow request.
   *
   * @param messageJson Content of request
   */
  private void processMerchantEscrowRequest(JsonNode messageJson) {
    KafkaMessageUtils.processWithErrorHandling(messageJson, () -> {
      MerchantEscrowEvent merchantEscrowEvent = new MerchantEscrowEvent();
      merchantEscrowEvent.parserData(messageJson);
      merchantEscrowEvent.validate();

      engineHandler.merchantEscrow(merchantEscrowEvent);
    }, producerService, "LogicConsumerMerchantEscrow");
  }

  /**
   * Process amm position request.
   *
   * @param messageJson Content of request
   */
  private void processAmmPositionRequest(JsonNode messageJson) {
    KafkaMessageUtils.processWithErrorHandling(messageJson, () -> {
      AmmPositionEvent ammPositionEvent = new AmmPositionEvent();
      ammPositionEvent.parserData(messageJson);
      ammPositionEvent.validate();

      engineHandler.ammPosition(ammPositionEvent);
    }, producerService, "LogicConsumerAmmPosition");
  }

  /**
   * Process amm order request.
   *
   * @param messageJson Content of request
   */
  private void processAmmOrderRequest(JsonNode messageJson) {
    KafkaMessageUtils.processWithErrorHandling(messageJson, () -> {
      AmmOrderEvent ammOrderEvent = new AmmOrderEvent();
      ammOrderEvent.parserData(messageJson);
      ammOrderEvent.validate();

      engineHandler.ammOrder(ammOrderEvent);
    }, producerService, "LogicConsumerAmmOrder");
  }

  /**
   * Process trade request.
   *
   * @param messageJson Content of request
   */
  private void processTradeRequest(JsonNode messageJson) {
    KafkaMessageUtils.processWithErrorHandling(messageJson, () -> {
      com.exchangeengine.model.event.TradeEvent tradeEvent = new com.exchangeengine.model.event.TradeEvent();
      tradeEvent.parserData(messageJson);
      tradeEvent.validate();

      engineHandler.processTrade(tradeEvent);
    }, producerService, "LogicConsumerTrade");
  }

  /**
   * Process offer request.
   *
   * @param messageJson Content of request
   */
  private void processOfferRequest(JsonNode messageJson) {
    KafkaMessageUtils.processWithErrorHandling(messageJson, () -> {
      com.exchangeengine.model.event.OfferEvent offerEvent = new com.exchangeengine.model.event.OfferEvent();
      offerEvent.parserData(messageJson);
      offerEvent.validate();

      engineHandler.processOffer(offerEvent);
    }, producerService, "LogicConsumerOffer");
  }

  /**
   * Process balances lock request.
   *
   * @param messageJson Content of request
   */
  private void processBalancesLockRequest(JsonNode messageJson) {
    KafkaMessageUtils.processWithErrorHandling(messageJson, () -> {
      BalancesLockEvent balancesLockEvent = new BalancesLockEvent();
      balancesLockEvent.parserData(messageJson);
      balancesLockEvent.validate();

      engineHandler.balancesLock(balancesLockEvent);
    }, producerService, "LogicConsumerBalancesLock");
  }

  /**
   * Dừng consumer.
   */
  public void shutdown() {
    running.set(false);
    consumer.wakeup();
    logger.info("Đã gửi tín hiệu dừng đến consumer");
  }
}
