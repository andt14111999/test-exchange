package com.exchangeengine.util;

import com.exchangeengine.factory.event.AccountEventFactory;
import com.exchangeengine.factory.event.CoinDepositEventFactory;
import com.exchangeengine.messaging.producer.KafkaProducerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMessageUtilsTest {

  @Mock
  private KafkaProducerService producerService;

  private ObjectMapper objectMapper;
  private JsonNode messageJson;
  private String identifier;
  private String eventId;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    identifier = "txn-" + UUID.randomUUID();
    eventId = "evt-" + UUID.randomUUID();

    // Tạo message JSON mẫu
    ObjectNode node = objectMapper.createObjectNode();
    node.put("identifier", identifier);
    node.put("eventId", eventId);
    node.put("amount", 100);
    node.put("coin", "usdt");
    messageJson = node;
  }

  @Test
  @DisplayName("KafkaMessageUtils should have private constructor and should not be instantiated")
  void constructor_ShouldBePrivateAndThrowException() throws Exception {
    // Kiểm tra xem constructor có phải là private không
    Constructor<KafkaMessageUtils> constructor = KafkaMessageUtils.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");

    // Thử tạo instance và kiểm tra ngoại lệ
    constructor.setAccessible(true);
    Exception exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
    assertTrue(exception.getCause() instanceof UnsupportedOperationException,
        "Should throw UnsupportedOperationException");
    assertEquals("Utility class should not be instantiated",
        exception.getCause().getMessage(), "Exception message should match");
  }

  @Test
  @DisplayName("processWithErrorHandling should execute processor without exception")
  void processWithErrorHandling_ShouldExecuteProcessor_WithoutException() {
    // Arrange
    boolean[] processorCalled = { false };
    KafkaMessageUtils.ProcessorFunction processor = () -> {
      processorCalled[0] = true;
    };

    // Act
    KafkaMessageUtils.processWithErrorHandling(messageJson, processor, producerService, "TestContext");

    // Assert
    assertTrue(processorCalled[0], "Processor should be called");
    verifyNoInteractions(producerService);
  }

  @Test
  @DisplayName("processWithErrorHandling should handle processor exception and send error message")
  void processWithErrorHandling_ShouldHandleProcessorException_AndSendErrorMessage() {
    // Arrange
    String errorMessage = "Test error message";
    KafkaMessageUtils.ProcessorFunction processor = () -> {
      throw new RuntimeException(errorMessage);
    };

    // Act
    KafkaMessageUtils.processWithErrorHandling(messageJson, processor, producerService, "TestContext");

    // Assert
    verify(producerService).sendTransactionResultNotProcessed(any(Map.class));
  }

  @Test
  @DisplayName("processWithErrorHandling should handle exception when sending error notification")
  void processWithErrorHandling_ShouldHandleException_WhenSendingErrorNotification() {
    // Arrange
    String errorMessage = "Test error message";
    KafkaMessageUtils.ProcessorFunction processor = () -> {
      throw new RuntimeException(errorMessage);
    };

    // Throw exception when trying to send transaction result
    doThrow(new RuntimeException("Failed to send")).when(producerService)
        .sendTransactionResultNotProcessed(any(Map.class));

    // Act
    KafkaMessageUtils.processWithErrorHandling(messageJson, processor, producerService, "TestContext");

    // Assert
    verify(producerService).sendTransactionResultNotProcessed(any(Map.class));
    // Không có cách để kiểm tra internal log, chỉ đảm bảo code không throw
    // exception
  }

  @Test
  @DisplayName("generateErrorMessageJson should create error message with original data")
  void generateErrorMessageJson_ShouldCreateErrorMessage_WithOriginalData() {
    // Arrange
    String errorMessage = "Something went wrong";

    // Act
    Map<String, Object> result = KafkaMessageUtils.generateErrorMessageJson(messageJson, errorMessage);

    // Assert
    assertNotNull(result);
    assertEquals(false, result.get("isSuccess"));
    assertEquals(errorMessage, result.get("errorMessage"));
    assertEquals(identifier, result.get("identifier"));
    assertEquals(eventId, result.get("eventId"));
    assertEquals(100, result.get("amount"));
    assertEquals("usdt", result.get("coin"));
  }

  @Test
  @DisplayName("processWithErrorHandling should handle different message types")
  void processWithErrorHandling_ShouldHandleDifferentMessageTypes() {
    // Sử dụng CoinDepositEventFactory để tạo JsonNode
    JsonNode depositNode = CoinDepositEventFactory.createJsonNode();
    KafkaMessageUtils.processWithErrorHandling(depositNode, () -> {
    }, producerService, "TestContext");

    // Sử dụng AccountEventFactory để tạo JsonNode
    JsonNode accountNode = AccountEventFactory.createJsonNode();
    KafkaMessageUtils.processWithErrorHandling(accountNode, () -> {
    }, producerService, "TestContext");

    // Không verify interactions nào khác ngoài việc kiểm tra không có exception nào
    // được ném ra
    verifyNoInteractions(producerService);
  }

  @Test
  @DisplayName("processWithErrorHandling should handle error with factory-created messages")
  void processWithErrorHandling_ShouldHandleErrorWithFactoryMessages() {
    // Arrange - Sử dụng CoinDepositEventFactory để tạo JsonNode
    JsonNode depositNode = CoinDepositEventFactory.createJsonNode();
    String errorMessage = "Test error with factory message";

    KafkaMessageUtils.ProcessorFunction processor = () -> {
      throw new RuntimeException(errorMessage);
    };

    // Act
    KafkaMessageUtils.processWithErrorHandling(depositNode, processor, producerService, "TestContext");

    // Assert
    verify(producerService).sendTransactionResultNotProcessed(any(Map.class));
  }
}
