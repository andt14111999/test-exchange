package com.exchangeengine.util;

import com.exchangeengine.messaging.producer.KafkaProducerService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Tiện ích xử lý các message từ Kafka, giúp tái sử dụng code giữa các consumer
 */
public class KafkaMessageUtils {
  private static final Logger logger = LoggerFactory.getLogger(KafkaMessageUtils.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Private constructor để ngăn việc khởi tạo instance của utility class.
   */
  private KafkaMessageUtils() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  /**
   * Interface định nghĩa một hàm xử lý có thể ném ra ngoại lệ
   */
  @FunctionalInterface
  public interface ProcessorFunction {
    void process() throws Exception;
  }

  /**
   * Xử lý một message từ Kafka với cơ chế bắt lỗi và thông báo
   *
   * @param messageJson     Message dạng JSON từ Kafka
   * @param processor       Hàm xử lý chính
   * @param producerService Service gửi kết quả
   * @param loggerContext   Context để ghi log (thường là tên class)
   * @return Khóa Kafka được sử dụng (identifier hoặc eventId hoặc random UUID)
   */
  public static void processWithErrorHandling(
      JsonNode messageJson,
      ProcessorFunction processor,
      KafkaProducerService producerService,
      String loggerContext) {

    try {
      // Gọi hàm xử lý logic chính
      processor.process();
    } catch (Exception e) {
      logger.error("[{}] Error processing message: {}", loggerContext, e.getMessage(), e);
      try {
        Map<String, Object> responseMessageJson = generateErrorMessageJson(
            messageJson, e.getMessage());
        responseMessageJson.put("inputEventId", messageJson.get("eventId").asText());

        producerService.sendTransactionResultNotProcessed(responseMessageJson);
      } catch (Exception ex) {
        logger.error("[{}] Error sending error notification: {}", loggerContext, ex.getMessage(), ex);
      }
    }
  }

  /**
   * Tạo thông báo lỗi từ message gốc
   *
   * @param messageJson  Message gốc
   * @param errorMessage Thông báo lỗi
   * @return Map chứa thông tin lỗi
   */
  public static Map<String, Object> generateErrorMessageJson(JsonNode messageJson, String errorMessage) {
    ObjectNode newMessageJson = (ObjectNode) messageJson.deepCopy();
    newMessageJson.put("isSuccess", false);
    newMessageJson.put("errorMessage", errorMessage);

    return objectMapper.convertValue(newMessageJson, new TypeReference<Map<String, Object>>() {
    });
  }
}
