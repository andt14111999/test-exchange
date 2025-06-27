package com.exchangeengine.messaging.consumer;

import com.exchangeengine.util.EnvManager;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Cấu hình chung cho Kafka Consumer, được tái sử dụng bởi nhiều service
 */
public class KafkaConsumerConfig {
  private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerConfig.class);
  private static EnvManager envManager = EnvManager.getInstance();

  /**
   * Constructor mặc định
   */
  private KafkaConsumerConfig() {
    // Constructor rỗng
  }

  /**
   * Tạo cấu hình cơ bản cho một Kafka Consumer với tùy chọn readFromBeginning
   *
   * @param consumerGroup     Tên nhóm consumer
   * @param autoCommit        Có tự động commit offset hay không
   * @param readFromBeginning Đọc từ đầu (true) hoặc chỉ đọc tin nhắn mới (false)
   * @return Properties chứa cấu hình
   */
  public static Properties createConsumerConfig(String consumerGroup, boolean autoCommit, boolean readFromBeginning) {
    // Mặc định không tự động bỏ qua topic không hoạt động
    return createConsumerConfig(consumerGroup, autoCommit, readFromBeginning, false);
  }

  /**
   * Tạo cấu hình cơ bản cho một Kafka Consumer với tùy chọn readFromBeginning
   *
   * @param consumerGroup     Tên nhóm consumer
   * @param autoCommit        Có tự động commit offset hay không
   * @param readFromBeginning Đọc từ đầu (true) hoặc chỉ đọc tin nhắn mới (false)
   * @return Properties chứa cấu hình
   */
  public static Properties createConsumerConfig(String consumerGroup, boolean autoCommit, boolean readFromBeginning,
      boolean autoIgnoreTopic) {
    String bootstrapServers = envManager.get("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");

    // Lấy giá trị auto.offset.reset từ biến môi trường hoặc tham số
    String autoOffsetReset;
    if (readFromBeginning) {
      autoOffsetReset = "earliest";
    } else {
      // Cho phép cấu hình từ biến môi trường, mặc định là latest
      autoOffsetReset = envManager.get("KAFKA_AUTO_OFFSET_RESET", "latest");
    }

    logger.info("Initializing Kafka consumer with offset reset: {}", autoOffsetReset);

    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);

    // Thêm cấu hình SSL cho môi trường production
    if (envManager.isProduction()) {
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "https");
        logger.info("Production environment detected, using default JVM SSL settings for consumer");
    }

    // Cấu hình auto commit
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(autoCommit));
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

    if (autoIgnoreTopic) {
      // Cấu hình giúp consumer group tự loại bỏ topic không còn hoạt động
      // Số lượng byte tối thiểu mà consumer mong đợi khi gọi poll()
      // Nếu không đủ dữ liệu, consumer sẽ đợi tối đa fetch.max.wait.ms
      // Giá trị lớn giúp tăng hiệu suất và giảm tải broker
      props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG,
          envManager.get("KAFKA_FETCH_MIN_BYTES", "1024")); // 1KB

      // Thời gian tối đa consumer đợi đạt đủ fetch.min.bytes
      // Giá trị thấp hơn giúp phản hồi nhanh, giá trị cao hơn giúp giảm tải broker
      props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG,
          envManager.get("KAFKA_FETCH_MAX_WAIT_MS", "500")); // 500ms

      // Khoảng thời gian client Kafka gửi heartbeat để duy trì kết nối tới broker
      // Nên thấp hơn session.timeout.ms
      props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,
          envManager.get("KAFKA_HEARTBEAT_INTERVAL_MS", "3000")); // 3 giây

      // Nếu broker không nhận được heartbeat trong khoảng thời gian này
      // Consumer sẽ bị coi là chết và broker sẽ trigger rebalance
      props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,
          envManager.get("KAFKA_SESSION_TIMEOUT_MS", "10000")); // 10 giây

      // Thời gian tối đa giữa các lần gọi poll() trước khi consumer bị coi là chết
      // Nên lớn hơn session.timeout.ms và bao gồm thời gian xử lý message
      props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
          envManager.get("KAFKA_MAX_POLL_INTERVAL_MS", "300000")); // 5 phút
    }

    return props;
  }
}
