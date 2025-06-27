package com.exchangeengine.messaging.consumer;

import com.exchangeengine.extension.CombinedTestExtension;
import com.exchangeengine.util.EnvManager;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({ MockitoExtension.class, CombinedTestExtension.class })
@MockitoSettings(strictness = Strictness.LENIENT)
class KafkaConsumerConfigTest {

  @Mock
  private EnvManager mockEnvManager;

  private MockedStatic<EnvManager> mockedEnvManagerStatic;

  @BeforeEach
  void setUp() throws Exception {
    // Thiết lập môi trường test
    EnvManager.setTestEnvironment();

    // Setup các mock cần thiết
    mockEnvManager = mock(EnvManager.class);
    mockedEnvManagerStatic = mockStatic(EnvManager.class);
    mockedEnvManagerStatic.when(EnvManager::getInstance).thenReturn(mockEnvManager);

    // QUAN TRỌNG: Sử dụng reflection để set trực tiếp biến static envManager trong
    // KafkaConsumerConfig
    Field envManagerField = KafkaConsumerConfig.class.getDeclaredField("envManager");
    envManagerField.setAccessible(true);
    envManagerField.set(null, mockEnvManager);

    // Cấu hình các giá trị mặc định cho EnvManager
    when(mockEnvManager.get("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")).thenReturn("localhost:9092");
    when(mockEnvManager.get("KAFKA_AUTO_OFFSET_RESET", "latest")).thenReturn("latest");
    when(mockEnvManager.get("KAFKA_FETCH_MIN_BYTES", "1024")).thenReturn("1024");
    when(mockEnvManager.get("KAFKA_FETCH_MAX_WAIT_MS", "500")).thenReturn("500");
    when(mockEnvManager.get("KAFKA_HEARTBEAT_INTERVAL_MS", "3000")).thenReturn("3000");
    when(mockEnvManager.get("KAFKA_SESSION_TIMEOUT_MS", "10000")).thenReturn("10000");
    when(mockEnvManager.get("KAFKA_MAX_POLL_INTERVAL_MS", "300000")).thenReturn("300000");
    when(mockEnvManager.get("KAFKA_GROUP_ID", "engine-service-group")).thenReturn("test-group");
  }

  @AfterEach
  void tearDown() {
    if (mockedEnvManagerStatic != null) {
      mockedEnvManagerStatic.close();
    }
  }

  @Test
  @DisplayName("createConsumerConfig with readFromBeginning=true should set auto.offset.reset to earliest")
  void createConsumerConfig_WithReadFromBeginningTrue_ShouldSetAutoOffsetResetToEarliest() {
    // Act
    Properties result = KafkaConsumerConfig.createConsumerConfig("test-group", true, true);

    // Assert
    assertEquals("earliest", result.getProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG),
        "auto.offset.reset should be set to earliest");
  }

  @Test
  @DisplayName("createConsumerConfig with autoCommit=true should set enable.auto.commit to true")
  void createConsumerConfig_WithAutoCommitTrue_ShouldSetEnableAutoCommitToTrue() {
    // Act
    Properties result = KafkaConsumerConfig.createConsumerConfig("test-group", true, false);

    // Assert
    assertEquals("true", result.getProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG),
        "enable.auto.commit should be set to true");
  }

  @Test
  @DisplayName("createConsumerConfig with autoCommit=false should set enable.auto.commit to false")
  void createConsumerConfig_WithAutoCommitFalse_ShouldSetEnableAutoCommitToFalse() {
    // Act
    Properties result = KafkaConsumerConfig.createConsumerConfig("test-group", false, false);

    // Assert
    assertEquals("false", result.getProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG),
        "enable.auto.commit should be set to false");
  }

  @Test
  @DisplayName("createConsumerConfig with autoIgnoreTopic=true should configure additional properties")
  void createConsumerConfig_WithAutoIgnoreTopicTrue_ShouldConfigureAdditionalProperties() {
    // Act
    Properties result = KafkaConsumerConfig.createConsumerConfig("test-group", true, false, true);

    // Assert
    assertEquals("1024", result.getProperty(ConsumerConfig.FETCH_MIN_BYTES_CONFIG),
        "fetch.min.bytes should be set");
    assertEquals("500", result.getProperty(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG),
        "fetch.max.wait.ms should be set");
    assertEquals("3000", result.getProperty(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG),
        "heartbeat.interval.ms should be set");
    assertEquals("10000", result.getProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG),
        "session.timeout.ms should be set");
    assertEquals("300000", result.getProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG),
        "max.poll.interval.ms should be set");
  }

  @Test
  @DisplayName("createConsumerConfig with autoIgnoreTopic=false should not configure additional properties")
  void createConsumerConfig_WithAutoIgnoreTopicFalse_ShouldNotConfigureAdditionalProperties() {
    // Act
    Properties result = KafkaConsumerConfig.createConsumerConfig("test-group", true, false, false);

    // Assert
    assertNull(result.getProperty(ConsumerConfig.FETCH_MIN_BYTES_CONFIG),
        "fetch.min.bytes should not be set");
    assertNull(result.getProperty(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG),
        "fetch.max.wait.ms should not be set");
    assertNull(result.getProperty(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG),
        "heartbeat.interval.ms should not be set");
    assertNull(result.getProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG),
        "session.timeout.ms should not be set");
    assertNull(result.getProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG),
        "max.poll.interval.ms should not be set");
  }

  @Test
  @DisplayName("createConsumerConfig with readFromBeginning=true and autoIgnoreTopic=true")
  void createConsumerConfig_WithReadFromBeginningTrueAndAutoIgnoreTopicTrue_ShouldConfigureAdditionalProperties() {
    // Act
    Properties result = KafkaConsumerConfig.createConsumerConfig("test-group", true, true, true);

    // Assert
    assertEquals("earliest", result.getProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG),
        "auto.offset.reset should be set to earliest");
    assertEquals("1024", result.getProperty(ConsumerConfig.FETCH_MIN_BYTES_CONFIG),
        "fetch.min.bytes should be set");
    assertEquals("500", result.getProperty(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG),
        "fetch.max.wait.ms should be set");
    assertEquals("3000", result.getProperty(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG),
        "heartbeat.interval.ms should be set");
    assertEquals("10000", result.getProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG),
        "session.timeout.ms should be set");
    assertEquals("300000", result.getProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG),
        "max.poll.interval.ms should be set");
  }

  @Test
  @DisplayName("createConsumerConfig khi ở môi trường production sẽ set cấu hình SSL")
  void createConsumerConfig_WhenProductionEnv_ShouldSetSSLProperties() {
    // Arrange: Giả lập môi trường production
    when(mockEnvManager.isProduction()).thenReturn(true);

    // Act: Tạo config
    Properties result = KafkaConsumerConfig.createConsumerConfig("test-group", true, false);

    // Assert: Kiểm tra các property SSL
    assertEquals("SSL", result.getProperty("security.protocol"), "security.protocol phải là SSL ở production");
    assertEquals("https", result.getProperty("ssl.endpoint.identification.algorithm"), "ssl.endpoint.identification.algorithm phải là https ở production");
  }
}
