package com.exchangeengine.messaging.common;

import com.exchangeengine.model.KafkaTopics;
import com.exchangeengine.util.EnvManager;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.Properties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test cho KafkaConfig
 * Kiểm tra khởi tạo Singleton, cấu hình SSL khi production, và shutdown resource
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KafkaConfigTest {

  @Mock
  private EnvManager mockEnvManager;

  @Mock
  private ListTopicsResult mockListTopicsResult;

  @Mock
  private KafkaProducer<String, String> mockProducer;

  private MockedStatic<EnvManager> mockedEnvManagerStatic;
  private MockedStatic<AdminClient> mockedAdminClientStatic;
  private MockedConstruction<KafkaProducer> mockedProducerConstruction;

  private AdminClient mockAdminClient;
  private KafkaConfig kafkaConfig;

  @BeforeEach
  void setUp() throws Exception {
    // Thiết lập môi trường test
    EnvManager.setTestEnvironment();

    // Mock EnvManager
    mockedEnvManagerStatic = mockStatic(EnvManager.class);
    mockedEnvManagerStatic.when(EnvManager::getInstance).thenReturn(mockEnvManager);

    // Configure EnvManager mock with test values
    when(mockEnvManager.get("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")).thenReturn("test-server:9092");
    when(mockEnvManager.get("KAFKA_SECURITY_PROTOCOL", "")).thenReturn("SSL");
    when(mockEnvManager.get("KAFKA_SSL_ENDPOINT", "")).thenReturn("https");
    when(mockEnvManager.getInt("KAFKA_TOPIC_PARTITIONS", 3)).thenReturn(3);
    when(mockEnvManager.getInt("KAFKA_TOPIC_REPLICATION_FACTOR", 1)).thenReturn(1);
    when(mockEnvManager.get("APP_ENV", "development")).thenReturn("test");

    // Setup AdminClient
    mockAdminClient = mock(AdminClient.class);
    mockedAdminClientStatic = mockStatic(AdminClient.class);
    mockedAdminClientStatic.when(() -> AdminClient.create(any(Properties.class))).thenReturn(mockAdminClient);

    // Mock ListTopicsResult
    when(mockAdminClient.listTopics()).thenReturn(mockListTopicsResult);

    // Cấu hình ListTopicsResult
    Set<String> existingTopics = new HashSet<>();
    existingTopics.add(KafkaTopics.COIN_ACCOUNT_TOPIC);
    KafkaFuture<Set<String>> future = KafkaFuture.completedFuture(existingTopics);
    when(mockListTopicsResult.names()).thenReturn(future);

    // Mock createTopics to return a CreateTopicsResult
    when(mockAdminClient.createTopics(any())).thenReturn(mock(org.apache.kafka.clients.admin.CreateTopicsResult.class));

    // Reset singleton instance
    Field instanceField = KafkaConfig.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);

    // Setup mocked KafkaProducer construction
    mockedProducerConstruction = mockConstruction(KafkaProducer.class);

    // Khởi tạo KafkaConfig sau khi đã setup các mock
    kafkaConfig = KafkaConfig.getInstance();

    // Sau khi khởi tạo, cập nhật các mock objects vào instance
    updateMocksInKafkaConfig();
  }

  /**
   * Cập nhật các mock objects vào KafkaConfig instance
   */
  private void updateMocksInKafkaConfig() throws Exception {
    // Cập nhật mockAdminClient vào kafkaConfig
    Field adminClientField = KafkaConfig.class.getDeclaredField("kafkaAdminClient");
    adminClientField.setAccessible(true);
    adminClientField.set(kafkaConfig, mockAdminClient);

    // KafkaProducer đã được tạo trong constructor, chúng ta sẽ sử dụng
    // producer từ mockedProducerConstruction nếu cần
  }

  @AfterEach
  void tearDown() {
    if (mockedEnvManagerStatic != null) {
      mockedEnvManagerStatic.close();
    }
    if (mockedAdminClientStatic != null) {
      mockedAdminClientStatic.close();
    }
    if (mockedProducerConstruction != null) {
      mockedProducerConstruction.close();
    }
    KafkaConfig.setTestInstance(null); // Reset singleton
  }

  @Test
  @DisplayName("Kiểm tra Singleton instance")
  void testSingletonInstance() {
    KafkaConfig instance1 = KafkaConfig.getInstance();
    KafkaConfig instance2 = KafkaConfig.getInstance();
    assertSame(instance1, instance2, "Phải trả về cùng một instance");
  }

  @Test
  @DisplayName("Khởi tạo Producer không SSL khi không phải production")
  void testProducerConfigNonProduction() {
    KafkaConfig config = KafkaConfig.getInstance();
    KafkaProducer<String, String> producer = config.getProducer();
    assertNotNull(producer, "Producer phải được khởi tạo");
    // Không kiểm tra SSL vì envManagerMock.isProduction() trả về false
  }

  @Test
  @DisplayName("Khởi tạo Producer với SSL khi production")
  void testProducerConfigProduction() {
    // Đặt lại mock để trả về true cho production
    when(mockEnvManager.isProduction()).thenReturn(true);
    KafkaConfig.setTestInstance(null); // Reset singleton
    KafkaConfig config = KafkaConfig.getInstance();
    KafkaProducer<String, String> producer = config.getProducer();
    assertNotNull(producer, "Producer phải được khởi tạo");
    // Không thể kiểm tra trực tiếp properties, nhưng có thể kiểm tra log hoặc cấu hình qua integration test
  }

  @Test
  @DisplayName("Đóng tài nguyên không lỗi")
  void testShutdown() {
    KafkaConfig config = KafkaConfig.getInstance();
    assertDoesNotThrow(config::shutdown, "Đóng tài nguyên không được throw exception");
  }

  @Test
  @DisplayName("constructor should initialize AdminClient and Producer")
  void constructor_ShouldInitializeAdminClientAndProducer() throws Exception {
    // Verify that a KafkaProducer was constructed during initialization
    assertTrue(mockedProducerConstruction.constructed().size() > 0,
        "A KafkaProducer should be constructed during initialization");

    // Verify that the AdminClient and KafkaProducer fields in KafkaConfig are not
    // null
    // Phản ánh việc các thành phần này đã được khởi tạo trong constructor
    Field adminClientField = KafkaConfig.class.getDeclaredField("kafkaAdminClient");
    adminClientField.setAccessible(true);
    assertNotNull(adminClientField.get(kafkaConfig), "kafkaAdminClient should be initialized");

    Field producerField = KafkaConfig.class.getDeclaredField("sharedKafkaProducer");
    producerField.setAccessible(true);
    assertNotNull(producerField.get(kafkaConfig), "sharedKafkaProducer should be initialized");
  }

  @Test
  @DisplayName("constructor should initialize AdminClient with SSL properties in production")
  void constructor_ShouldInitializeAdminClientWithSslProperties() {
    try {
      // Đóng các static mocks
      if (mockedEnvManagerStatic != null) {
        mockedEnvManagerStatic.close();
        mockedEnvManagerStatic = null;
      }
      if (mockedAdminClientStatic != null) {
        mockedAdminClientStatic.close();
        mockedAdminClientStatic = null;
      }

      // Reset Singleton
      Field instanceField = KafkaConfig.class.getDeclaredField("instance");
      instanceField.setAccessible(true);
      instanceField.set(null, null);

      // Tạo mock EnvManager cho môi trường production
      EnvManager mockEnvManagerProd = mock(EnvManager.class);
      when(mockEnvManagerProd.isProduction()).thenReturn(true);
      when(mockEnvManagerProd.get("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")).thenReturn("test-server:9092");

      // Đặt mockEnvManagerProd trực tiếp vào biến static envManager
      Field envManagerField = KafkaConfig.class.getDeclaredField("envManager");
      envManagerField.setAccessible(true);
      envManagerField.set(null, mockEnvManagerProd);

      // Capture Properties đã được truyền vào AdminClient.create
      ArgumentCaptor<Properties> propertiesCaptor = ArgumentCaptor.forClass(Properties.class);

      // Thiết lập AdminClient static mock
      mockedAdminClientStatic = mockStatic(AdminClient.class);
      mockedAdminClientStatic.when(() -> AdminClient.create(propertiesCaptor.capture()))
          .thenReturn(mock(AdminClient.class));

      // Tạo KafkaConfig instance
      KafkaConfig instance = KafkaConfig.getInstance();

      // Lấy properties đã được capture
      Properties capturedProps = propertiesCaptor.getValue();

      // In ra các thuộc tính để debug
      System.out.println("Captured Properties:");
      capturedProps.forEach((key, value) -> System.out.println(key + " = " + value));

      // Kiểm tra SSL properties đã được đặt
      assertEquals("SSL", capturedProps.get(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG),
          "Thuộc tính security.protocol phải là SSL");
      assertEquals("https", capturedProps.get(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG),
          "Thuộc tính ssl.endpoint.identification.algorithm phải là https");

      // Gán instance để các test khác có thể sử dụng
      kafkaConfig = instance;
    } catch (Exception e) {
      e.printStackTrace();
      fail("Test failed with exception: " + e.getMessage());
    } finally {
      // Reset lại EnvManager về mock ban đầu cho các test tiếp theo
      try {
        Field envManagerField = KafkaConfig.class.getDeclaredField("envManager");
        envManagerField.setAccessible(true);
        envManagerField.set(null, mockEnvManager);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  @DisplayName("getProducer should return existing producer")
  void getProducer_ShouldReturnExistingProducer() throws Exception {
    // Setup
    Field producerField = KafkaConfig.class.getDeclaredField("sharedKafkaProducer");
    producerField.setAccessible(true);
    producerField.set(kafkaConfig, mockProducer);

    // Act
    KafkaProducer<String, String> producer = kafkaConfig.getProducer();

    // Assert
    assertSame(mockProducer, producer, "Should return the existing producer");
  }

  @Test
  @DisplayName("getBootstrapServers should return correct server")
  void getBootstrapServers_ShouldReturnCorrectServer() {
    // Act
    String bootstrapServers = kafkaConfig.getBootstrapServers();

    // Assert
    assertEquals("test-server:9092", bootstrapServers, "Bootstrap servers should match");
  }

  @Test
  @DisplayName("createTopics should handle exceptions when creating topics")
  void createTopics_ShouldHandleExceptions_WhenCreatingTopics() throws Exception {
    // Setup adminClient to throw exception when createTopics is called
    Exception testException = new RuntimeException("Test exception");
    when(mockAdminClient.createTopics(any())).thenThrow(testException);

    // Act - no exception should be thrown
    kafkaConfig.createTopics();

    // Assert - no assertion needed as we're just checking that the code handles
    // exceptions
    // If the test passes, it means the exception was handled by the try-catch block
  }

  @Test
  @DisplayName("topicExists should return empty set when exception occurs")
  void topicExists_ShouldReturnEmptySet_WhenExceptionOccurs() throws Exception {
    // Setup adminClient to throw exception when listTopics is called
    when(mockAdminClient.listTopics()).thenThrow(new RuntimeException("Test exception"));

    // Act
    Set<String> topics = kafkaConfig.topicExists();

    // Assert
    assertTrue(topics.isEmpty(), "Should return empty set when exception occurs");
  }

  @Test
  @DisplayName("topicExists should return existing topics")
  void topicExists_ShouldReturnExistingTopics() throws Exception {
    // Act
    Set<String> topics = kafkaConfig.topicExists();

    // Assert
    assertTrue(topics.contains(KafkaTopics.COIN_ACCOUNT_TOPIC), "Should contain the COIN_ACCOUNT_TOPIC");
  }

  @Test
  @DisplayName("createTopics should create non-existing topics")
  void createTopics_ShouldCreateNonExistingTopics() throws Exception {
    // Act
    kafkaConfig.createTopics();

    // Assert
    verify(mockAdminClient, atLeastOnce()).createTopics(any());
  }

  @Test
  @DisplayName("shutdown should handle exceptions when closing AdminClient")
  void shutdown_ShouldHandleExceptions_WhenClosingAdminClient() throws Exception {
    // Setup adminClient to throw exception when close is called
    doThrow(new RuntimeException("Test AdminClient close exception")).when(mockAdminClient).close();

    // Act - no exception should be thrown
    kafkaConfig.shutdown();

    // Assert
    verify(mockAdminClient).close();
    // If the test passes, it means the exception was handled by the try-catch block
  }

  @Test
  @DisplayName("shutdown should handle null AdminClient")
  void shutdown_ShouldHandleNullAdminClient() throws Exception {
    // Setup kafkaAdminClient = null
    Field adminClientField = KafkaConfig.class.getDeclaredField("kafkaAdminClient");
    adminClientField.setAccessible(true);
    adminClientField.set(kafkaConfig, null);

    // Setup mockProducer in kafkaConfig to ensure the rest of the shutdown method
    // runs
    Field producerField = KafkaConfig.class.getDeclaredField("sharedKafkaProducer");
    producerField.setAccessible(true);
    producerField.set(kafkaConfig, mockProducer);

    // Act - no exception should be thrown
    kafkaConfig.shutdown();

    // Assert - no call to adminClient.close() because it's null
    verify(mockProducer).close(); // Check that the rest of the method still runs
  }

  @Test
  @DisplayName("shutdown should handle exceptions when closing Producer")
  void shutdown_ShouldHandleExceptions_WhenClosingProducer() throws Exception {
    // Setup mockProducer in kafkaConfig
    Field producerField = KafkaConfig.class.getDeclaredField("sharedKafkaProducer");
    producerField.setAccessible(true);
    producerField.set(kafkaConfig, mockProducer);

    // Setup producer to throw exception when close is called
    doThrow(new RuntimeException("Test Producer close exception")).when(mockProducer).close();

    // Act - no exception should be thrown
    kafkaConfig.shutdown();

    // Assert
    verify(mockProducer).close();
    // If the test passes, it means the exception was handled by the try-catch block
  }

  @Test
  @DisplayName("shutdown should handle null Producer")
  void shutdown_ShouldHandleNullProducer() throws Exception {
    // Setup sharedKafkaProducer = null
    Field producerField = KafkaConfig.class.getDeclaredField("sharedKafkaProducer");
    producerField.setAccessible(true);
    producerField.set(kafkaConfig, null);

    // Act - no exception should be thrown
    kafkaConfig.shutdown();

    // Assert - only adminClient.close() is called
    verify(mockAdminClient).close();
    // No verify for producer.close() because it's null
  }

  @Test
  @DisplayName("shutdown should close resources")
  void shutdown_ShouldCloseResources() throws Exception {
    // Setup mockProducer in kafkaConfig
    Field producerField = KafkaConfig.class.getDeclaredField("sharedKafkaProducer");
    producerField.setAccessible(true);
    producerField.set(kafkaConfig, mockProducer);

    // Act
    kafkaConfig.shutdown();

    // Assert
    verify(mockAdminClient).close();
    verify(mockProducer).close();
  }
}
