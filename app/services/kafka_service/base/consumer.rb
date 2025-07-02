# frozen_string_literal: true

module KafkaService
  module Base
    class Consumer
      def initialize(group_id:, topics:)
        @logger = Rails.env.production? ? Rails.logger : Logger.new('log/kafka_consumer.log')
        @group_id = group_id
        @topics = Array(topics)
        @consumer = initialize_consumer
        @running = false
        @logger.info("RdKafka Consumer initialized for group: #{group_id}, topics: #{@topics.join(', ')}")
      end

      def start(&)
        @running = true
        @logger.info('Starting to consume messages...')

        subscribe_to_topics
        consume_messages(&)
      rescue StandardError => e
        @logger.error("Consumer error: #{e.message}")
        @logger.error(e.backtrace.join("\n"))
        stop
        raise
      end

      def stop
        @running = false
        @logger.info('Stopping RdKafka Consumer...')

        begin
          @consumer&.close
          @logger.info('RdKafka Consumer stopped successfully')
        rescue StandardError => e
          @logger.error("Error closing consumer: #{e.message}")
        end
      end

      private

      def rdkafka_config
        config = {
          'bootstrap.servers': KafkaService::Config::Brokers::BROKERS.join(','),
          'group.id': @group_id,
          'client.id': "#{Rails.env}_#{@group_id}",
          'auto.offset.reset': 'latest',
          'enable.auto.commit': true,
          'auto.commit.interval.ms': 5000,
          'session.timeout.ms': 30000,
          'heartbeat.interval.ms': 10000,
          'max.poll.interval.ms': 300000,
          'fetch.min.bytes': 1,
          # Production performance settings
          'fetch.message.max.bytes': 1048576,  # 1MB max message size
          'queued.min.messages': 100000,       # Local message queue size
          'queued.max.messages.kbytes': 65536, # Max queue size in KB
          'socket.timeout.ms': 60000,          # Socket timeout
          'reconnect.backoff.ms': 100,         # Reconnection backoff
          'reconnect.backoff.max.ms': 10000    # Max reconnection backoff
        }

        if ssl_enabled?
          config.merge!(ssl_config)
        end

        config
      end

      def ssl_enabled?
        ENV.fetch('KAFKA_SSL_ENABLED', 'false') == 'true'
      end

      def ssl_config
        {
          'security.protocol': 'SSL',
          'ssl.ca.location': '/etc/ssl/certs/ca-certificates.crt',
          'ssl.verify.hostname': 'false'
        }
      end

      def initialize_consumer
        Rdkafka::Config.new(rdkafka_config).consumer
      end

      def subscribe_to_topics
        @consumer.subscribe(*@topics)
        @logger.info("Subscribed to topics: #{@topics.join(', ')}")
      end

      def consume_messages(&block)
        @consumer.each do |message|
          break unless @running

          begin
            process_message(message, &block)
          rescue StandardError => e
            @logger.error("Error processing individual message: #{e.message}")
            @logger.error(e.backtrace.join("\n"))
            # Continue processing other messages
          end
        end
      rescue StandardError => e
        @logger.error("Error in consume_messages loop: #{e.message}")
        @logger.error(e.backtrace.join("\n"))
        raise
      end

      def process_message(message)
        return unless @running

        begin
          payload = JSON.parse(message.payload)
          yield(message.topic, payload)
        rescue JSON::ParserError => e
          @logger.error("Failed to parse message: #{e.message}")
          @logger.error("Raw message: #{message.payload}")
        rescue StandardError => e
          @logger.error("Error processing message: #{e.message}")
          @logger.error(e.backtrace.join("\n"))
          raise
        end
      end
    end
  end
end
