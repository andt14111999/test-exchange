# frozen_string_literal: true

module KafkaService
  class DeadLetterQueue
    def self.publish(topic:, payload:, error:)
      producer = KafkaService::Base::Producer.new
      producer.send_message(
        topic: "#{topic}.dlq",
        key: payload['identifier'] || SecureRandom.uuid,
        payload: {
          original_message: payload,
          error: {
            message: error.message,
            backtrace: error.backtrace&.first(5),
            timestamp: Time.current
          }
        }
      )
    ensure
      producer&.close
    end
  end
end
