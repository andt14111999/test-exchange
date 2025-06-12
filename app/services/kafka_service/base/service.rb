# frozen_string_literal: true

module KafkaService
  module Base
    class Service
      def initialize
         @logger = Rails.env.production? ? Rails.logger : Logger.new("log/#{service_name}.log")
        initialize_producer
      end

      private

      def initialize_producer
        @producer = Producer.new
      rescue StandardError => e
        @logger.error("Failed to initialize Kafka producer: #{e.message}")
        raise
      end

      attr_reader :producer

      def service_name
        self.class.name.demodulize.underscore
      end

      def send_event(topic:, key:, data:)
        @logger.info("Sending event to #{topic}")
        producer.send_message(
          topic: topic,
          key: key,
          payload: data.merge(default_event_data)
        )
      rescue StandardError => e
        @logger.error("#{service_name} error: #{e.message}")
        raise
      end

      def default_event_data
        {
          eventId: SecureRandom.uuid,
          timestamp: Time.current.to_i * 1000
        }
      end
    end
  end
end
