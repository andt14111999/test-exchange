package com.exchangeengine.messaging.common;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;

import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.KafkaTopics;
import com.exchangeengine.util.EnvManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class KafkaConfig {
  private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);
  private static EnvManager envManager = EnvManager.getInstance();

  // Singleton instance
  private static volatile KafkaConfig instance;

  private final String bootstrapServers;
  private KafkaProducer<String, String> sharedKafkaProducer;
  private AdminClient kafkaAdminClient;

  /**
   * Lấy instance của KafkaConfig.
   *
   * @return Instance của KafkaConfig
   */
  public static synchronized KafkaConfig getInstance() {
    if (instance == null) {
      instance = new KafkaConfig();
    }
    return instance;
  }

  /**
   * Thiết lập instance kiểm thử (chỉ sử dụng cho testing)
   *
   * @param testInstance Instance kiểm thử cần thiết lập
   */
  public static void setTestInstance(KafkaConfig testInstance) {
    instance = testInstance;
  }

  /**
   * Constructor riêng để đảm bảo Singleton pattern.
   */
  private KafkaConfig() {
    this.bootstrapServers = envManager.get("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
    initializeAdminClient();
    initializeProducer();
    logger.info("KafkaConfig initialized with bootstrap servers: {}", bootstrapServers);
  }

  /**
   * Khởi tạo Kafka AdminClient.
   */
  private void initializeAdminClient() {
    Properties adminProps = new Properties();
    adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    adminProps.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
    adminProps.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 30000);

    // Thêm SSL settings khi ở môi trường production
    if (envManager.isProduction()) {
      adminProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
      adminProps.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "https");
      logger.info("Production environment detected, SSL settings enabled");
    }

    kafkaAdminClient = AdminClient.create(adminProps);
    logger.info("Kafka AdminClient initialized with SSL, truststore={}, timeout={}ms",
        adminProps.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG),
        adminProps.get(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG));
  }

  /**
   * Khởi tạo Kafka Producer.
   */
  private void initializeProducer() {
    Properties producerProps = new Properties();
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

    // Thêm SSL settings khi ở môi trường production
    if (envManager.isProduction()) {
      producerProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
      producerProps.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "https");
      logger.info("Production environment detected, SSL settings enabled");
    }

    // Add other configurations from EnvManager if needed
    sharedKafkaProducer = new KafkaProducer<>(producerProps);
    logger.info("Kafka Producer initialized");
  }

  /**
   * Lấy Kafka Producer đã được khởi tạo.
   *
   * @return KafkaProducer instance
   */
  public KafkaProducer<String, String> getProducer() {
    return sharedKafkaProducer;
  }

  /**
   * Tạo các topic Kafka nếu chưa tồn tại.
   */
  public void createTopics() {
    int partitions = envManager.getInt("KAFKA_TOPIC_PARTITIONS", 3);
    short replicationFactor = (short) envManager.getInt("KAFKA_TOPIC_REPLICATION_FACTOR", 1);

    logger.info("Creating topics with {} partitions and replication factor {}", partitions, replicationFactor);

    Set<String> topics = topicExists();
    logger.info("Topics: {}", topics);
    logger.info("KafkaTopics.TOPICS: {}", Arrays.toString(KafkaTopics.TOPICS));

    for (String topic : KafkaTopics.TOPICS) {
      try {
        if (topics.contains(topic)) {
          logger.info("Topic {} already exists", topic);
          continue;
        }

        NewTopic newTopic = new NewTopic(topic, partitions, replicationFactor);
        kafkaAdminClient.createTopics(java.util.Collections.singleton(newTopic));

        logger.info("Created topic {}", topic);
      } catch (Exception e) {
        logger.warn("Cannot create topic {}: {}", topic, e.getMessage());
      }
    }
  }

  /**
   * Kiểm tra xem topic đã tồn tại chưa.
   *
   * @return Tập hợp các topic đã tồn tại
   */
  public Set<String> topicExists() {
    try {
      logger.info("Checking for existing topics with timeout: {}ms", 30000);
      return kafkaAdminClient.listTopics().names().get(30000, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      logger.error("Error checking topics", e);
      return Collections.emptySet();
    }
  }

  /**
   * Lấy địa chỉ bootstrap servers.
   *
   * @return Địa chỉ bootstrap servers
   */
  public String getBootstrapServers() {
    return bootstrapServers;
  }

  /**
   * Đóng các tài nguyên Kafka.
   */
  public void shutdown() {
    if (kafkaAdminClient != null) {
      try {
        kafkaAdminClient.close();
        logger.info("Kafka AdminClient closed");
      } catch (Exception e) {
        logger.error("Error closing Kafka AdminClient: {}", e.getMessage());
      }
    }

    if (sharedKafkaProducer != null) {
      try {
        sharedKafkaProducer.close();
        logger.info("Kafka Producer closed");
      } catch (Exception e) {
        logger.error("Error closing Kafka Producer: {}", e.getMessage());
      }
    }
  }
}
