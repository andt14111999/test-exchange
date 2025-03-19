# frozen_string_literal: true

module KafkaService
  module Base
    class Consumer
      def initialize(group_id:, topics:)
        @logger = Logger.new('log/kafka_consumer.log')
        @kafka = ::Kafka.new(
          seed_brokers: KafkaService::Config::Brokers::BROKERS,
          client_id: "#{Rails.env}_#{group_id}",
          logger: @logger,
          socket_timeout: 20,
          connect_timeout: 20
        )
        @consumer = initialize_consumer(group_id)
        subscribe_to_topics(topics)
        @running = false
      end

      def start(&)
        @running = true
        @logger.info('Starting to consume messages...')

        consume_messages(&)
      rescue StandardError => e
        @logger.error("Consumer error: #{e.message}")
        stop
        raise
      end

      def stop
        @running = false
        @consumer.stop
        @logger.info('Kafka Consumer stopped')
      end

      private

      def initialize_consumer(group_id)
        @kafka.consumer(
          group_id: group_id,
          offset_commit_interval: 5,
          offset_commit_threshold: 100,
          offset_retention_time: 7_200, # 2 hours
          fetcher_max_queue_size: 100,
          session_timeout: 30,
          heartbeat_interval: 10
        )
      end

      def subscribe_to_topics(topics)
        Array(topics).each do |topic|
          @consumer.subscribe(topic, start_from_beginning: false)
        end
        @logger.info("Subscribed to topics: #{topics.join(', ')}")
      end

      def consume_messages(&block)
        @consumer.each_message do |message|
          break unless @running

          process_message(message, &block)
        end
      end

      def process_message(message)
        payload = JSON.parse(message.value)
        yield(message.topic, payload)
      rescue JSON::ParserError => e
        @logger.error("Failed to parse message: #{e.message}")
      rescue StandardError => e
        @logger.error("Error processing message: #{e.message}")
      end
    end
  end
end
