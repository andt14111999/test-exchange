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
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service xử lý riêng các truy vấn (query) từ Kafka.
 * Sử dụng ThreadPool riêng để không ảnh hưởng đến luồng xử lý chính.
 */
public class KafkaConsumerQueryService implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerQueryService.class);
  private static final EnvManager envManager = EnvManager.getInstance();
  private static final String LOGGER_CONTEXT = "QueryConsumer";
  private static final boolean AUTO_COMMIT = true;

  // Singleton instance
  private static volatile KafkaConsumerQueryService instance;

  private final KafkaConsumer<String, String> consumer;
  private final KafkaProducerService producerService;
  private final ObjectMapper objectMapper;
  private final ExecutorService queryExecutor;
  private final int queryThreadsCount;

  private final AtomicBoolean running = new AtomicBoolean(true);

  /**
   * Lấy instance của KafkaConsumerQueryService.
   *
   * @return Instance của KafkaConsumerQueryService
   */
  public static synchronized KafkaConsumerQueryService getInstance() {
    if (instance == null) {
      int threadCount = Integer.parseInt(envManager.get("KAFKA_QUERY_THREADS", "3"));
      instance = new KafkaConsumerQueryService("engine-query-group", threadCount);
    }
    return instance;
  }

  /**
   * Constructor, tạo consumer mới cho service này.
   * Private để đảm bảo Singleton pattern.
   *
   * @param consumerGroup     Consumer group ID
   * @param queryThreadsCount Số lượng thread xử lý query
   */
  private KafkaConsumerQueryService(String consumerGroup, int queryThreadsCount) {
    this.queryThreadsCount = queryThreadsCount;

    // Tạo ThreadPool cho xử lý query
    logger.info("Initializing query executor with {} threads", queryThreadsCount);
    this.queryExecutor = Executors.newFixedThreadPool(queryThreadsCount);

    // Sử dụng cấu hình chung, đặt readFromBeginning = false để chỉ đọc tin nhắn mới
    // Bật auto commit để tự động commit offset
    Properties props = KafkaConsumerConfig.createConsumerConfig(consumerGroup, AUTO_COMMIT, false);

    this.consumer = new KafkaConsumer<>(props);
    this.producerService = KafkaProducerService.getInstance();
    this.objectMapper = new ObjectMapper();

    // Subscribe vào các topic query
    consumer.subscribe(Arrays.asList(KafkaTopics.QUERY_TOPICS));
    logger.info("KafkaConsumerQueryService initialized and subscribed to query topics: {}",
        Arrays.toString(KafkaTopics.QUERY_TOPICS));
  }

  @Override
  public void run() {
    try {
      while (running.get()) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, String> record : records) {
          // Xử lý tất cả các record trong QUERY_TOPICS bằng ThreadPool
          final ConsumerRecord<String, String> finalRecord = record;
          queryExecutor.submit(() -> {
            try {
              processQueryRecord(finalRecord);
              // Xóa consumer.commitSync() ở đây
            } catch (Exception e) {
              logger.error("Error processing query record: {}", e.getMessage(), e);
            }
          });
        }
        // Commit offsets trong luồng chính sau khi đã gửi tất cả các tác vụ
        if (!AUTO_COMMIT) {
          if (!records.isEmpty()) {
            consumer.commitSync();
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
      logger.info("Query Consumer closed");
    }
  }

  /**
   * Xử lý record query từ Kafka.
   *
   * @param record Record cần xử lý
   */
  private void processQueryRecord(ConsumerRecord<String, String> record) {
    try {
      JsonNode messageJson = objectMapper.readTree(record.value());
      logger.debug("Processing query: topic={}, partition={}, offset={}, key={}, value={}",
          record.topic(), record.partition(), record.offset(), record.key(), messageJson);

      switch (record.topic()) {
        case KafkaTopics.COIN_ACCOUNT_QUERY_TOPIC:
          processCoinAccountQuery(messageJson);
          break;
        case KafkaTopics.RESET_BALANCE_TOPIC:
          processResetBalanceRequest(messageJson);
          break;
        case KafkaTopics.TICK_QUERY_TOPIC:
          processTickQuery(messageJson);
          break;
        default:
          logger.warn("Cannot process topic: {}", record.topic());
      }
    } catch (Exception e) {
      logger.error("Error processing query message: {}", e.getMessage(), e);
    }
  }

  /**
   * Xử lý truy vấn tài khoản coin.
   *
   * @param messageJson Nội dung truy vấn
   */
  private void processCoinAccountQuery(JsonNode messageJson) {
    KafkaMessageUtils.processWithErrorHandling(messageJson, () -> {
      AccountEvent accountEvent = new AccountEvent().parserData(messageJson);
      accountEvent.validate();

      String accountKey = accountEvent.getAccountKey();
      logger.debug("Processing query for account: {}", accountKey);

      producerService.sendCoinAccountBalance(accountKey);
    }, producerService, LOGGER_CONTEXT);
  }

  /**
   * Xử lý yêu cầu reset số dư.
   *
   * @param messageJson Nội dung yêu cầu
   */
  private void processResetBalanceRequest(JsonNode messageJson) {
    KafkaMessageUtils.processWithErrorHandling(messageJson, () -> {
      String accountKey = messageJson.path("accountKey").asText();

      if (accountKey == null || accountKey.isEmpty()) {
        throw new IllegalArgumentException("accountKey is required");
      }

      logger.info("Processing reset balance request: accountKey={}", accountKey);

      // perform reset balance
      producerService.resetCoinAccount(accountKey);
    }, producerService, LOGGER_CONTEXT);
  }

  /**
   * Process tick query request
   *
   * @param messageJson Query content
   */
  private void processTickQuery(JsonNode messageJson) {
    KafkaMessageUtils.processWithErrorHandling(messageJson, () -> {
      TickEvent tickEvent = new TickEvent();
      tickEvent.parserData(messageJson);
      tickEvent.validate();

      String poolPair = tickEvent.getPoolPair();
      logger.debug("Processing query for ticks in pool: {}", poolPair);

      // Get ticks from bitmap
      List<Tick> ticks = tickEvent.fetchTicksFromBitmap();

      // Send each tick to Kafka
      for (Tick tick : ticks) {
        producerService.sendTickUpdate(tick);
      }

    }, producerService, LOGGER_CONTEXT);
  }

  /**
   * Dừng consumer và executor.
   */
  public void shutdown() {
    running.set(false);
    consumer.wakeup();

    if (queryExecutor != null) {
      queryExecutor.shutdown();
      try {
        // Đợi các query đang xử lý hoàn thành trong thời gian giới hạn
        if (!queryExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
          queryExecutor.shutdownNow();
        }
      } catch (InterruptedException e) {
        queryExecutor.shutdownNow();
        Thread.currentThread().interrupt();
      }
      logger.info("Query executor shutdown completed");
    }

    logger.info("Đã gửi tín hiệu dừng đến query consumer");
  }

  /**
   * Lấy số lượng thread đang được sử dụng cho xử lý query
   *
   * @return Số lượng thread
   */
  public int getQueryThreadsCount() {
    return queryThreadsCount;
  }
}
